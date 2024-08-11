
/**
 *
 */
package com.foreverjava.Controller;

import java.io.IOException;

import com.foreverjava.Util.Creds;
import com.foreverjava.Util.FJCryptoUtil;
import com.foreverjava.Util.FileConfigUtil;
import com.foreverjava.Reader.JavaReaderService;
import com.foreverjava.Reader.XmlReaderService;
import com.foreverjava.Reader.GeminiService;
import com.foreverjava.Writer.FileWriterClass;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
import java.io.IOException;

/**
 * @author GAURAV
 *
 */
@CrossOrigin
@RestController
public class Server_Controller {

	@Autowired
	private XmlReaderService xmlReaderService;

	@Autowired
	private JavaReaderService javaReaderService;

	@Autowired
    	private GeminiService geminiService;

	private final FileConfigUtil fileConfig;

	@Autowired
	public Server_Controller(FileConfigUtil fileConfig) {
		this.fileConfig = fileConfig;
	}

	@GetMapping("/read-xml")
	public Creds readXml(){
		return xmlReaderService.readCredsFromXml(fileConfig);
	}

	@GetMapping("/list-java-files")
	public List<String> listJavaFiles() {
		return javaReaderService.listJavaFiles();
	}

	@GetMapping("/get-file-contents")
	public String getFileContents(@RequestParam String fileName) {
		try {
			return javaReaderService.readFileContents(fileName);
		} catch (IOException e) {
			return "Error: " + e.getMessage();
		}
	}

	@GetMapping("/Hello")
	public String welcome() {
		System.out.println("..................getting a request....");
		java.util.Date date = new java.util.Date();
		return new String(date + " APi is working as expected - Gaurav");
	}

	@GetMapping("/Error")
	public String error() {
		return "error 1";
	}

	@RequestMapping("/java")
	public StringBuilder CodeExecutor(@RequestBody String s) throws IOException {
		JSONObject j = new JSONObject(s);
		System.out.println(j.getString("code") + " : " +j.getString("input"));
		Executor executor = Executors.newFixedThreadPool(10);
		StringBuilder result = new StringBuilder();
		try {
			Calendar c1 = Calendar.getInstance();
			Date dateOne = c1.getTime();
			long time = dateOne.getTime();
			String timestamp = String.valueOf(time);

			result = ((ExecutorService) executor).submit(() -> {
				FileWriterClass f = new FileWriterClass(j.getString("code"),j.getString("input"), timestamp, fileConfig);
				return f.write();
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new StringBuilder(result);
	}

    @GetMapping("/FJ-AI")
    public String getResponse(String prompt) throws IOException {
        String suffix = "Write only the Java code for the following: ";
        Creds creds = xmlReaderService.readCredsFromXml(fileConfig);
        String ai_api = creds.getAI_API();
        return geminiService.callApi(suffix + prompt, ai_api);
    }

	@GetMapping("Vertex-AI")
	public String getResponsefromVertexAI(String prompt) throws IOException{
		String suffix = "Write only the Java code for the following: ";
		String projectId = "gen-lang-client-0300339255";
		String location = "us-central1";
		String modelName = "gemini-1.5-flash-001";

//		String output = textInput(projectId, location, modelName, textPrompt);
//		System.out.println(output);

		try (VertexAI vertexAI = new VertexAI(projectId, location)) {
			GenerativeModel model = new GenerativeModel(modelName, vertexAI);

			GenerateContentResponse response = model.generateContent(suffix + prompt);
			String output = ResponseHandler.getText(response);
			output = output.replace("```java", "").replace("```", "");
			System.out.println(output);
			return output;
		}
	}
}
