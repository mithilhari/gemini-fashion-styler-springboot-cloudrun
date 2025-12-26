package com.example.demo.fashion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class FashionImageService {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    private final String location = "us-central1";

    // Data structure for the response
    public record StyledImageResult(String base64Image, String description) {}

    public StyledImageResult styleImage(MultipartFile file, String occasion, String style, String color, String season, String additionalPrompt) {
        
        if (projectId == null || projectId.isEmpty() || projectId.contains("your-project-id")) {
            throw new RuntimeException("Project ID is not configured in application.properties!");
        }

        try {
            // 1. Convert MultipartFile to Base64 string
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 2. Build the AI Generation Prompt
            String fullPrompt = String.format(
                "A professional fashion photo of a person wearing an outfit styled for a %s occasion. " +
                "The style should be %s, with a %s color palette, suitable for %s weather. %s",
                occasion, style, color, season, (additionalPrompt != null ? additionalPrompt : "")
            );

            // 3. Prepare the REST Request for Vertex AI (Imagen)
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/imagegeneration@006:predict",
                location, projectId, location
            );

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> instance = new HashMap<>();
            instance.put("prompt", fullPrompt);
            
            Map<String, String> imageInput = new HashMap<>();
            imageInput.put("bytesBase64Encoded", base64Image);
            instance.put("image", imageInput);

            requestBody.put("instances", Collections.singletonList(instance));
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sampleCount", 1);
            requestBody.put("parameters", parameters);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Note: Ensure 'gcloud auth application-default login' was run locally
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 4. Execute API Call
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // 5. Parse Response
            List<Map<String, String>> predictions = (List<Map<String, String>>) response.getBody().get("predictions");
            String outputBase64 = predictions.get(0).get("bytesBase64Encoded");

            return new StyledImageResult(outputBase64, "Successfully styled for " + occasion);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process fashion styling: " + e.getMessage(), e);
        }
    }
}
