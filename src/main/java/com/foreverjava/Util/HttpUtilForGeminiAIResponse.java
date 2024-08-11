package com.foreverjava.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtilForGeminiAIResponse {

    public static String sendPostRequest(String requestUrl, String payload) throws IOException {
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set up the connection properties
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Send the JSON payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read the response
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String responseCode = extractPartsTextFromResponse(response.toString());
            String finishReason = "";
            if(responseCode == null || !responseCode.matches("\\S+")){
                finishReason = extractFinishReason(response.toString());
                return finishReason;
            }
            return responseCode;
        } catch (IOException e) {
            // Handle the error response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return "Error response: " + response.toString();
            }
        } finally {
            connection.disconnect();
        }
    }

    public static String extractFinishReason(String jsonResponse) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonResponse);

        // Directly access "finishReason" using path
        JsonNode finishReasonNode = rootNode.at("/candidates/0/finishReason");

        // Check if the node exists and is a text node
        if (finishReasonNode.isTextual()) {
            return finishReasonNode.asText();
        } else {
            // Handle the case where the node is missing or not textual (e.g., null)
            return null; // Or throw an exception if you prefer
        }
    }

    private static String extractPartsTextFromResponse(String jsonResponse) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonResponse);
        JsonNode partsNode = rootNode.at("/candidates/0/content/parts");

        StringBuilder partsText = new StringBuilder();
        if (partsNode.isArray()) {
            for (JsonNode part : partsNode) {
                JsonNode textNode = part.get("text");
                if (textNode != null) {
                    String text = textNode.asText();
                    // Remove the "```java" and "```" markers
                    text = text.replace("```java", "").replace("```", "");
                    partsText.append(text);
                }
            }
        }

        return partsText.toString().trim();
    }
}