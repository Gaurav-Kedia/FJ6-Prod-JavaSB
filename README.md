# FJ6-Prod-JavaSB
Java Spring Boot application for ForeverJava in FJ6 EC2 instance in Production Environment


commit 21 with update yml and sh file fin 1

## Logging to Amazon S3

The application ships with a custom Logback appender that can mirror live console logs to a
single object in Amazon S3. Configure the following properties (for example via environment
variables or your Spring configuration management) to enable it:

```
logging.s3.enabled=true
logging.s3.bucket=<your-log-bucket-name>
logging.s3.key=logs/application.log
logging.s3.region=<aws-region>
logging.s3.flush-threshold=10
logging.s3.max-startup-bytes=1048576
logging.s3.small-object-threshold-bytes=5242880
```

The appender uses the default AWS credential provider chain so it works with instance profiles,
container roles, or explicit access keys. Small log objects are downloaded into memory for fast
appends up to the configured thresholds, while larger objects are extended via S3 multipart copy so
logs continue to grow without exhausting server memory.
