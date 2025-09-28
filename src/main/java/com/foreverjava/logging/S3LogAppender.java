package com.foreverjava.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A Logback appender that mirrors application logs to a single object in Amazon S3.
 * <p>
 * Small log files are downloaded into memory for fast appends while larger files are extended
 * using S3 multipart copy, avoiding unbounded memory growth on the application node. This design is
 * best-effort and optimised for operational visibility rather than extreme throughput.
 */
public class S3LogAppender extends AppenderBase<ILoggingEvent> {

    private static final int DEFAULT_FLUSH_THRESHOLD = 10;
    private static final long MULTIPART_MIN_PART_SIZE_BYTES = 5L * 1024L * 1024L; // 5 MiB

    private final List<String> buffer = new ArrayList<>();
    private final Object bufferLock = new Object();

    private Layout<ILoggingEvent> layout;
    private AmazonS3 s3Client;
    private String bucketName;
    private String key;
    private String region;
    private boolean enabled = true;
    private int flushThreshold = DEFAULT_FLUSH_THRESHOLD;
    private long maxStartupDownloadBytes = 1024L * 1024L; // 1 MiB
    private long smallObjectDownloadThresholdBytes = MULTIPART_MIN_PART_SIZE_BYTES;
    private byte[] aggregatedWarmCache = new byte[0];

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFlushThreshold(int flushThreshold) {
        if (flushThreshold <= 0) {
            addWarn("Flush threshold must be greater than zero. Falling back to default.");
            return;
        }
        this.flushThreshold = flushThreshold;
    }

    public void setMaxStartupDownloadBytes(long maxStartupDownloadBytes) {
        if (maxStartupDownloadBytes < 0) {
            addWarn("Maximum startup download bytes cannot be negative. Keeping existing value.");
            return;
        }
        this.maxStartupDownloadBytes = maxStartupDownloadBytes;
    }

    public void setSmallObjectDownloadThresholdBytes(long smallObjectDownloadThresholdBytes) {
        if (smallObjectDownloadThresholdBytes < 0) {
            addWarn("Small object download threshold cannot be negative. Keeping existing value.");
            return;
        }
        this.smallObjectDownloadThresholdBytes = smallObjectDownloadThresholdBytes;
    }

    @Override
    public void start() {
        if (!enabled) {
            addInfo("S3LogAppender disabled via configuration. Skipping initialization.");
            return;
        }
        if (layout == null) {
            addError("No layout configured for S3LogAppender. Cannot start appender.");
            return;
        }
        if (bucketName == null || bucketName.isBlank()) {
            addError("Bucket name must be configured for S3LogAppender.");
            return;
        }
        if (key == null || key.isBlank()) {
            addError("Object key must be configured for S3LogAppender.");
            return;
        }

        layout.start();
        s3Client = buildClient();
        preloadExistingLogs();
        super.start();
    }

    private AmazonS3 buildClient() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance());
        if (region != null && !region.isBlank()) {
            try {
                builder = builder.withRegion(Regions.fromName(region));
            } catch (IllegalArgumentException ex) {
                addWarn("Invalid AWS region provided for S3LogAppender: " + region + ". Using default region.");
            }
        }
        return builder.build();
    }

    private void preloadExistingLogs() {
        try {
            if (!s3Client.doesObjectExist(bucketName, key)) {
                return;
            }
        } catch (AmazonServiceException serviceException) {
            addWarn("Unable to determine if log object exists in S3. Logs will be uploaded from scratch.", serviceException);
            return;
        }

        try (S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));
             InputStream objectStream = object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] bufferBytes = new byte[8192];
            long remaining = maxStartupDownloadBytes <= 0 ? Long.MAX_VALUE : maxStartupDownloadBytes;
            int read;
            while ((read = objectStream.read(bufferBytes, 0, (int) Math.min(bufferBytes.length, remaining))) != -1) {
                outputStream.write(bufferBytes, 0, read);
                remaining -= read;
                if (remaining <= 0) {
                    addInfo("Existing S3 log larger than configured startup preload. Truncating preload content.");
                    break;
                }
            }
            // Retain only a small warm cache to avoid unbounded memory usage. The
            // appender will stream larger appends directly via multipart uploads.
            aggregatedWarmCache = outputStream.toByteArray();
        } catch (AmazonClientException e) {
            addWarn("Failed to load existing log object from S3. A new object will be created when logs are uploaded.", e);
            aggregatedWarmCache = new byte[0];
        } catch (IOException e) {
            addWarn("Error while reading existing log data from S3. Proceeding with empty cache.", e);
            aggregatedWarmCache = new byte[0];
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }
        String message = layout.doLayout(eventObject);
        synchronized (bufferLock) {
            buffer.add(message);
            if (buffer.size() >= flushThreshold || eventObject.getLevel().isGreaterOrEqual(Level.ERROR)) {
                flushBuffer();
            }
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) {
            return;
        }
        synchronized (bufferLock) {
            flushBuffer();
        }
        if (layout != null) {
            layout.stop();
        }
        super.stop();
    }

    private void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }
        String payload = mergeBuffer();
        try {
            uploadToS3(payload);
            buffer.clear();
        } catch (RuntimeException ex) {
            addError("Failed to upload log batch to S3. Retaining batch for retry.", ex);
        }
    }

    private String mergeBuffer() {
        StringBuilder builder = new StringBuilder();
        for (String entry : buffer) {
            builder.append(entry);
        }
        return builder.toString();
    }

    private void uploadToS3(String payload) {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        if (data.length == 0) {
            return;
        }

        try {
            if (!s3Client.doesObjectExist(bucketName, key)) {
                putNewObject(data);
                aggregatedWarmCache = data;
                return;
            }

            long existingSize = fetchExistingObjectSize();
            if (existingSize < smallObjectDownloadThresholdBytes) {
                appendByDownloading(existingSize, data);
            } else {
                appendByMultipartCopy(existingSize, data);
            }
        } catch (AmazonClientException e) {
            throw new RuntimeException("Unable to upload log data to S3", e);
        }
    }

    private void putNewObject(byte[] data) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain; charset=utf-8");
        metadata.setContentLength(data.length);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        s3Client.putObject(bucketName, key, inputStream, metadata);
    }

    private long fetchExistingObjectSize() {
        ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key);
        return metadata.getContentLength();
    }

    private void appendByDownloading(long existingSize, byte[] newData) {
        long limit = maxStartupDownloadBytes <= 0 ? Long.MAX_VALUE : maxStartupDownloadBytes;
        if (existingSize > limit) {
            addWarn("Existing S3 log larger than configured in-memory append limit. Falling back to multipart copy.");
            appendByMultipartCopy(existingSize, newData);
            return;
        }

        byte[] existingData = loadExistingObject((int) existingSize);
        byte[] combined = new byte[existingData.length + newData.length];
        System.arraycopy(existingData, 0, combined, 0, existingData.length);
        System.arraycopy(newData, 0, combined, existingData.length, newData.length);
        putNewObject(combined);
        aggregatedWarmCache = combined;
    }

    private byte[] loadExistingObject(int expectedLength) {
        if (expectedLength == 0 && aggregatedWarmCache.length == 0) {
            return new byte[0];
        }
        if (aggregatedWarmCache.length == expectedLength) {
            return aggregatedWarmCache;
        }

        try (S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, key));
             InputStream objectStream = object.getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream(expectedLength > 0 ? expectedLength : 1024)) {
            byte[] bufferBytes = new byte[8192];
            int read;
            while ((read = objectStream.read(bufferBytes)) != -1) {
                outputStream.write(bufferBytes, 0, read);
            }
            byte[] bytes = outputStream.toByteArray();
            aggregatedWarmCache = bytes;
            return bytes;
        } catch (AmazonClientException | IOException e) {
            throw new RuntimeException("Unable to load existing log object from S3", e);
        }
    }

    private void appendByMultipartCopy(long existingSize, byte[] newData) {
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, key);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("text/plain; charset=utf-8");
        initRequest.setObjectMetadata(objectMetadata);

        InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);
        String uploadId = initResponse.getUploadId();
        List<PartETag> partETags = new ArrayList<>();

        int nextPartNumber = 1;
        try {
            if (existingSize > 0) {
                CopyPartRequest copyRequest = new CopyPartRequest()
                        .withSourceBucketName(bucketName)
                        .withSourceKey(key)
                        .withDestinationBucketName(bucketName)
                        .withDestinationKey(key)
                        .withUploadId(uploadId)
                        .withFirstByte(0L)
                        .withLastByte(existingSize - 1)
                        .withPartNumber(nextPartNumber);
                CopyPartResult copyResult = s3Client.copyPart(copyRequest);
                partETags.add(copyResult.getPartETag());
                nextPartNumber++;
            }

            UploadPartRequest uploadRequest = new UploadPartRequest()
                    .withBucketName(bucketName)
                    .withKey(key)
                    .withUploadId(uploadId)
                    .withPartNumber(nextPartNumber)
                    .withInputStream(new ByteArrayInputStream(newData))
                    .withPartSize(newData.length);
            UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
            partETags.add(uploadResult.getPartETag());

            s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags));
        } catch (AmazonClientException ex) {
            s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, key, uploadId));
            throw ex;
        }
        aggregatedWarmCache = newData.length < maxStartupDownloadBytes ? newData : new byte[0];
    }
}
