package com.foreverjava.Controller;

import com.foreverjava.Reader.JavaReaderService;
import com.foreverjava.Reader.XmlReaderService;
import com.foreverjava.Util.Creds;
import com.foreverjava.Util.FJCryptoUtil;
import com.foreverjava.Util.FileConfigUtil;
import com.foreverjava.Writer.FileWriterClass;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@CrossOrigin
@RestController
public class Server_Controller {

	private static final Logger logger = LoggerFactory.getLogger(Server_Controller.class);
	private final XmlReaderService xmlReaderService;
	private final FJCryptoUtil encryptionUtils;
	private final JavaReaderService javaReaderService;
	private final FileConfigUtil fileConfig;
	private final ExecutorService executorService = Executors.newFixedThreadPool(10);
	private final AtomicInteger apiRequestCount = new AtomicInteger(0);

	@Autowired
	public Server_Controller(XmlReaderService xmlReaderService, FJCryptoUtil encryptionUtils,
							JavaReaderService javaReaderService, FileConfigUtil fileConfig) {
		this.xmlReaderService = xmlReaderService;
		this.encryptionUtils = encryptionUtils;
		this.javaReaderService = javaReaderService;
		this.fileConfig = fileConfig;
	}

	@GetMapping("/read-xml")
	public Creds readXml(@RequestParam String filePath) {
		logInfo("Received request to read XML file from path: " + filePath);
		return xmlReaderService.readCredsFromXml(filePath);
	}

	@GetMapping("/list-java-files")
	public List<String> listJavaFiles() {
		logInfo("Received request to list Java files");
		return javaReaderService.listJavaFiles();
	}

	@GetMapping("/get-file-contents")
	public String getFileContents(@RequestParam String fileName) {
		logInfo("Received request to get file contents for file: " + fileName);
		try {
			return javaReaderService.readFileContents(fileName);
		} catch (IOException e) {
			logError("Error reading file contents for file: " + fileName, e);
			return "Error: " + e.getMessage();
		}
	}

	@GetMapping("/Hello")
	public String welcome() {
		logInfo("Received request for Hello endpoint");
		Date date = new Date();
		return date + " API is working as expected - Gaurav";
	}

	@GetMapping("/Error")
	public String error() {
		logInfo("Received request for Error endpoint");
		return "error 1";
	}

	@RequestMapping("/java")
	public StringBuilder codeExecutor(@RequestBody String request) throws IOException {
		JSONObject jsonObject = new JSONObject(request);
		String code = jsonObject.getString("code");
		String input = jsonObject.getString("input");
		logInfo("Received request to execute Java code with input: " + input);

		StringBuilder result = new StringBuilder();
		try {
			Calendar calendar = Calendar.getInstance();
			String timestamp = String.valueOf(calendar.getTime().getTime());

			result = executorService.submit(() -> {
				FileWriterClass fileWriter = new FileWriterClass(code, input, timestamp, fileConfig);
				return fileWriter.write();
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			logError("Error executing Java code", e);
		}
		return result;
	}

	private void logInfo(String message) {
		int requestCount = apiRequestCount.incrementAndGet();
		logger.info("[{}] [{}] [API Request #{}] - {}", new Date(), Thread.currentThread().getName(), requestCount, message);
	}

	private void logError(String message, Throwable throwable) {
		int requestCount = apiRequestCount.incrementAndGet();
		logger.error("[{}] [{}] [API Request #{}] - {}", new Date(), Thread.currentThread().getName(), requestCount, message, throwable);
	}
}
