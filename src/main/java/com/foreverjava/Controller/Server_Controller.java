package com.foreverjava.Controller;

import com.foreverjava.Dto.ApiResponseDTO;
import com.foreverjava.Dto.CodeExecutionRequest;
import com.foreverjava.Dto.CodeExecutionResponse;
import com.foreverjava.Util.Creds;
import com.foreverjava.Util.FileConfigUtil;
import com.foreverjava.Reader.ApiRequestService;
import com.foreverjava.Reader.GeminiService;
import com.foreverjava.Reader.JavaReaderService;
import com.foreverjava.Reader.XmlReaderService;
import com.foreverjava.Util.Hibernate.Table1;
import com.foreverjava.Util.Hibernate.Table1Service;
import com.foreverjava.service.CodeExecutionService;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author GAURAV
 */
@CrossOrigin
@RestController
@Validated
public class Server_Controller {

        private static final Logger LOGGER = LoggerFactory.getLogger(Server_Controller.class);

        private final XmlReaderService xmlReaderService;
        private final JavaReaderService javaReaderService;
        private final GeminiService geminiService;
        private final Table1Service table1Service;
        private final FileConfigUtil fileConfig;
        private final ApiRequestService apiRequestService;
        private final CodeExecutionService codeExecutionService;

        @Autowired
        public Server_Controller(XmlReaderService xmlReaderService,
                                                         JavaReaderService javaReaderService,
                                                         GeminiService geminiService,
                                                         Table1Service table1Service,
                                                         FileConfigUtil fileConfig,
                                                         ApiRequestService apiRequestService,
                                                         CodeExecutionService codeExecutionService) {
                this.xmlReaderService = xmlReaderService;
                this.javaReaderService = javaReaderService;
                this.geminiService = geminiService;
                this.table1Service = table1Service;
                this.fileConfig = fileConfig;
                this.apiRequestService = apiRequestService;
                this.codeExecutionService = codeExecutionService;
        }

        // Endpoint to read credentials from an XML file
        @GetMapping("/read-xml")
        public Creds readXml() {
                LOGGER.info("Request received to read XML credentials");
                return xmlReaderService.readCredsFromXml(fileConfig);
        }

        // Endpoint to list all Java files
        @GetMapping("/list-java-files")
        public List<String> listJavaFiles() {
                LOGGER.info("Request received to list Java files");
                return javaReaderService.listJavaFiles();
        }

        // Endpoint to get the contents of a specific file
        @GetMapping("/get-file-contents")
        public String getFileContents(@RequestParam String fileName) {
                LOGGER.info("Request received to get file contents for: {}", fileName);
                try {
                        return javaReaderService.readFileContents(fileName);
                } catch (IOException e) {
                        LOGGER.error("Error reading file contents for {}", fileName, e);
                        return "Error: " + e.getMessage();
                }
        }

        // Simple endpoint to check if the API is working
        @GetMapping("/Hello")
        public String welcome() {
                LOGGER.info("Health check requested");
                return Instant.now() + " API is working as expected - Gaurav";
        }

        // Placeholder endpoint for error handling
        @GetMapping("/Error")
        public String error() {
                LOGGER.warn("Error endpoint triggered");
                return "Error 1";
        }

        // Endpoint to execute Java code provided in the request body
        @PostMapping("/java")
        public CompletableFuture<ResponseEntity<CodeExecutionResponse>> codeExecutor(@Valid @RequestBody CodeExecutionRequest request) {
                LOGGER.info("Request received to execute Java code using {}", request.getJavaVersion().getDisplayName());
                return codeExecutionService.executeAsync(request)
                                .thenApply(ResponseEntity::ok);
        }

        // Endpoint to get a response from an AI model using GeminiService
        @GetMapping("/fj-ai")
        public String getResponse(@RequestParam String prompt) throws IOException {
                LOGGER.info("Request received to generate AI response using FJ-AI");
                String suffix = "Write only the Java code for the following: ";
                Creds creds = xmlReaderService.readCredsFromXml(fileConfig);
                String aiApi = creds.getAI_API();
                return geminiService.callApi(suffix + prompt, aiApi);
        }

        // Endpoint to get a response from Google's Vertex AI
        @GetMapping("/vertex-ai")
        public String getResponseFromVertexAI(@RequestParam String prompt) throws IOException {
                LOGGER.info("Request received to generate AI response using Vertex-AI");
                String suffix = "Write only the Java code for the following: ";
                String projectId = "gen-lang-client-0300339255";
                String location = "us-central1";
                String modelName = "gemini-1.5-flash-001";

                try (VertexAI vertexAI = new VertexAI(projectId, location)) {
                        GenerativeModel model = new GenerativeModel(modelName, vertexAI);
                        GenerateContentResponse response = model.generateContent(suffix + prompt);
                        String output = ResponseHandler.getText(response);
                        output = output.replace("```java", "").replace("```", "");
                        LOGGER.debug("Vertex AI response generated");
                        return output;
                }
        }

        // Endpoint to retrieve all records from Table1
        @GetMapping("/all")
        public List<Table1> getAllRecords() {
                LOGGER.info("Request received to fetch all records from Table1");
                return table1Service.getAllRecords();
        }

        // Endpoint to validate API request and generate AI response
        @PostMapping("/request-validate-generate-ai")
        public ApiResponseDTO handleRequest(@RequestParam("ipv6") String ipv6Value, @RequestParam("prompt") String prompt) throws IOException {
                LOGGER.info("Received request to validate and generate code using AI for IPv6 {}", ipv6Value);
                return apiRequestService.handleApiRequest(ipv6Value, prompt);
        }
}
