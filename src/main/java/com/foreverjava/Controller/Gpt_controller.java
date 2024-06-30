package com.foreverjava.Controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.HttpURLConnection;
import java.net.URL;
import com.foreverjava.Util.HttpUtils;

@RestController
@RequestMapping("/api")
public class Gpt_controller {

    private String openaiApiKey="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";
    String model = "gpt-3.5-turbo-instruct";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/generate-code")
    public String generateCode(@RequestBody String userInput) {
        String prompt = "Generate Java code for the following request: " + userInput;
        String openaiApiUrl = "https://api.openai.com/v1/completions"; // Update endpoint to v1/completions

        try {
//            URL obj = new URL(openaiApiUrl);
//            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
//            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Authorization", "Bearer " + openaiApiKey);
//            connection.setRequestProperty("Content-Type", "application/json");
//
//            // The request body
//            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
//            connection.setDoOutput(true);
//            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
//            writer.write(body);
//            writer.flush();
//            writer.close();
//
//            // Response from ChatGPT
//            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            String line;
//
//            StringBuffer response = new StringBuffer();
//
//            while ((line = br.readLine()) != null) {
//                response.append(line);
//            }
//            br.close();
//
//            // calls the method to extract the message.
//            return extractMessageFromJSONResponse(response.toString());

            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";
            return HttpUtils.sendPostRequest(openaiApiUrl, body, openaiApiKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content")+ 11;

        int end = response.indexOf("\"", start);

        return response.substring(start, end);

    }
}