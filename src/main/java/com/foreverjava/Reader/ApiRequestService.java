package com.foreverjava.Reader;

import com.foreverjava.Dto.ApiResponseDTO;
import com.foreverjava.Dto.UserIPv6DTO;
import com.foreverjava.Util.XmlQueryLoader;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ApiRequestService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private XmlQueryLoader xmlQueryLoader;

    public ApiResponseDTO handleApiRequest(String ipv6Value, String prompt) {
        System.out.println("Processing API request for IPv6: " + ipv6Value);

        String message;
        String status;
        int requestCount = 0;

        // Check if the user exists
        String selectUserIPv6Query = xmlQueryLoader.getQuery("selectUserIPv6");
        UserIPv6DTO userIPv6DTO = null;
        try {
            userIPv6DTO = jdbcTemplate.queryForObject(
                    selectUserIPv6Query,
                    new Object[]{ipv6Value},
                    (rs, rowNum) -> {
                        UserIPv6DTO dto = new UserIPv6DTO();
                        dto.setIpv6Id(rs.getLong("ipv6_id"));
                        dto.setIpv6Value(rs.getString("ipv6_value"));
                        dto.setRoleId(rs.getInt("role_id"));
                        return dto;
                    }
            );
        } catch (Exception e) {
            System.out.println("User not found, creating a new entry for IPv6: " + ipv6Value);
        }

        if (userIPv6DTO == null) {
            // Fetch the role ID for customer (assumed default role for new users)
            String selectCustomerRoleQuery = xmlQueryLoader.getQuery("selectRoleByName");
            int customerRoleId = jdbcTemplate.queryForObject(selectCustomerRoleQuery, new Object[]{"customer"}, Integer.class);

            String insertUserIPv6Query = xmlQueryLoader.getQuery("insertUserIPv6");
            jdbcTemplate.update(insertUserIPv6Query, ipv6Value, customerRoleId);
            userIPv6DTO = new UserIPv6DTO();
            userIPv6DTO.setIpv6Value(ipv6Value);
            userIPv6DTO.setRoleId(customerRoleId);
            userIPv6DTO.setIpv6Id(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        }

        // Fetch the request limit for the user's role
        String selectRoleLimitQuery = xmlQueryLoader.getQuery("selectRoleLimitById");
        int requestLimit = jdbcTemplate.queryForObject(selectRoleLimitQuery, new Object[]{userIPv6DTO.getRoleId()}, Integer.class);

        // Get request count for today
        String requestCountQuery = xmlQueryLoader.getQuery("countRequestsForToday");
        try {
            requestCount = jdbcTemplate.queryForObject(
                    requestCountQuery,
                    new Object[]{userIPv6DTO.getIpv6Id(), LocalDate.now()},
                    Integer.class
            );
        } catch (Exception e) {
            System.out.println("No requests found for today for IPv6: " + ipv6Value);
        }

        // Determine if request is allowed
        if (requestCount >= requestLimit) {
            status = "denied";
            message = "Request limit exceeded";
        } else {
            // Log the request
            String insertApiRequestLogQuery = xmlQueryLoader.getQuery("insertApiRequestLog");
            jdbcTemplate.update(insertApiRequestLogQuery, userIPv6DTO.getIpv6Id(), LocalDateTime.now());

            // Trigger your logic
            message = getResponsefromVertexAI(prompt);
            status = "processed";
            requestCount += 1;
        }

        return new ApiResponseDTO(requestCount, status, message);
    }

    private String getResponsefromVertexAI(String prompt) {
        String suffix = "Write only the Java code with necessary imports for the following: ";
        String projectId = "gen-lang-client-0300339255";
        String location = "us-central1";
        String modelName = "gemini-1.5-flash-001";

        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            GenerativeModel model = new GenerativeModel(modelName, vertexAI);

            GenerateContentResponse response = model.generateContent(suffix + prompt);
            String output = ResponseHandler.getText(response);
            output = output.replace("```java", "").replace("```", "");
            System.out.println(output);
            return output;
        }
        catch (Exception e){
            return e.getMessage();
        }
    }
}