package com.foreverjava.Controller;

import com.foreverjava.Dto.ApiResponseDTO;
import com.foreverjava.Util.*;
import com.foreverjava.Reader.JavaReaderService;
import com.foreverjava.Reader.XmlReaderService;
import com.foreverjava.Reader.GeminiService;
import com.foreverjava.Reader.ApiRequestService;
import com.foreverjava.Util.Hibernate.Table1;
import com.foreverjava.Util.Hibernate.Table1Service;
import com.foreverjava.Writer.FileWriterClass;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

/**
 * @author GAURAV
 *
 */
@CrossOrigin
@RestController
public class Server_Controller {

	private final XmlReaderService xmlReaderService;
	private final JavaReaderService javaReaderService;
	private final GeminiService geminiService;
	private final Table1Service table1Service;
	private final FileConfigUtil fileConfig;
	private final ApiRequestService apiRequestService;
	private final ExecutorService executorService;

	@Autowired
	public Server_Controller(XmlReaderService xmlReaderService, JavaReaderService javaReaderService,
							 GeminiService geminiService, Table1Service table1Service,
							 FileConfigUtil fileConfig, ApiRequestService apiRequestService) {
		this.xmlReaderService = xmlReaderService;
		this.javaReaderService = javaReaderService;
		this.geminiService = geminiService;
		this.table1Service = table1Service;
		this.fileConfig = fileConfig;
		this.apiRequestService = apiRequestService;
		this.executorService = Executors.newFixedThreadPool(10);  // Thread pool executor
	}

	// Endpoint to read credentials from an XML file
	@GetMapping("/read-xml")
	public Creds readXml() {
		System.out.println("Request received to read XML credentials.");
		return xmlReaderService.readCredsFromXml(fileConfig);
	}

	// Endpoint to list all Java files
	@GetMapping("/list-java-files")
	public List<String> listJavaFiles() {
		System.out.println("Request received to list Java files.");
		return javaReaderService.listJavaFiles();
	}

	// Endpoint to get the contents of a specific file
	@GetMapping("/get-file-contents")
	public String getFileContents(@RequestParam String fileName) {
		System.out.println("Request received to get file contents for: " + fileName);
		try {
			return javaReaderService.readFileContents(fileName);
		} catch (IOException e) {
			System.err.println("Error reading file contents: " + e.getMessage());
			return "Error: " + e.getMessage();
		}
	}

	// Simple endpoint to check if the API is working
	@GetMapping("/Hello")
	public String welcome() {
		System.out.println("Request received to check API status.");
		return new Date() + " API is working as expected - Gaurav";
	}

	// Placeholder endpoint for error handling
	@GetMapping("/Error")
	public String error() {
		System.out.println("Error endpoint triggered.");
		return "Error 1";
	}

	// Endpoint to execute Java code provided in the request body
	@PostMapping("/java")
	public StringBuilder codeExecutor(@RequestBody String requestBody) throws IOException {
		System.out.println("Request received to execute Java code.");
		JSONObject requestJson = new JSONObject(requestBody);
		String code = requestJson.getString("code");
		String input = requestJson.getString("input");
		System.out.println("Code: " + code + " | Input: " + input);

		String timestamp = String.valueOf(Calendar.getInstance().getTimeInMillis());

		try {
			return executorService.submit(() -> {
				FileWriterClass fileWriter = new FileWriterClass(code, input, timestamp, fileConfig);
				return fileWriter.write();
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			System.err.println("Error executing Java code: " + e.getMessage());
			return new StringBuilder("Execution error: " + e.getMessage());
		}
//		JSONObject j = new JSONObject(s);
//		System.out.println(j.getString("code") + " : " +j.getString("input"));
//		Executor executor = Executors.newFixedThreadPool(10);
//		StringBuilder result = new StringBuilder();
//		try {
//			Calendar c1 = Calendar.getInstance();
//			Date dateOne = c1.getTime();
//			long time = dateOne.getTime();
//			String timestamp = String.valueOf(time);
//
//			result = ((ExecutorService) executor).submit(() -> {
//				FileWriterClass f = new FileWriterClass(j.getString("code"),j.getString("input"), timestamp, fileConfig);
//				return f.write();
//			}).get();
//		} catch (InterruptedException | ExecutionException e) {
//			e.printStackTrace();
//		}
//		return new StringBuilder(result);
	}

	// Endpoint to get a response from an AI model using GeminiService
	@GetMapping("/fj-ai")
	public String getResponse(@RequestParam String prompt) throws IOException {
		System.out.println("Request received to generate AI response using FJ-AI.");
		String suffix = "Write only the Java code for the following: ";
		Creds creds = xmlReaderService.readCredsFromXml(fileConfig);
		String aiApi = creds.getAI_API();
		return geminiService.callApi(suffix + prompt, aiApi);
	}

	// Endpoint to get a response from Google's Vertex AI
	@GetMapping("/vertex-ai")
	public String getResponseFromVertexAI(@RequestParam String prompt) throws IOException {
		System.out.println("Request received to generate AI response using Vertex-AI.");
		String suffix = "Write only the Java code for the following: ";
		String projectId = "gen-lang-client-0300339255";
		String location = "us-central1";
		String modelName = "gemini-1.5-flash-001";

		try (VertexAI vertexAI = new VertexAI(projectId, location)) {
			GenerativeModel model = new GenerativeModel(modelName, vertexAI);
			GenerateContentResponse response = model.generateContent(suffix + prompt);
			String output = ResponseHandler.getText(response);
			output = output.replace("```java", "").replace("```", "");
			System.out.println("Vertex AI response: " + output);
			return output;
		}
	}

	// Endpoint to retrieve all records from Table1
	@GetMapping("/all")
	public List<Table1> getAllRecords() {
		System.out.println("Request received to fetch all records from Table1.");
		return table1Service.getAllRecords();
	}

	// Endpoint to validate API request and generate AI response
	@PostMapping("/request-validate-generate-ai")
	public ApiResponseDTO handleRequest(@RequestParam("ipv6") String ipv6Value, @RequestParam("prompt") String prompt) throws IOException {
		System.out.println("Received request to validate and generate code using AI");
		System.out.println("Received API request for IPv6: " + ipv6Value + " ,For prompt: " + prompt);
		return apiRequestService.handleApiRequest(ipv6Value, prompt);
	}
}
