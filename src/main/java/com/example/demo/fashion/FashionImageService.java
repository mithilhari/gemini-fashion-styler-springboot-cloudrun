package com.example.demo.fashion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class FashionImageService {

    private static final String PROJECT_ID =
            System.getenv("GOOGLE_CLOUD_PROJECT");

    private static final String LOCATION = "us-central1";

    private static final String IMAGEN_ENDPOINT =
        "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/imagen-2.0-edit-001:predict";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StyledImageResult styleImage(
            MultipartFile image,
            String fashionText,
            String gender,
            String season,
            String occasion,
            String weather
    ) throws Exception {

        String prompt = buildPrompt(
                fashionText, gender, season, occasion, weather
        );

        String base64Image =
                Base64.getEncoder().encodeToString(image.getBytes());

        String requestBody = """
        {
          "instances": [{
            "prompt": "%s",
            "image": {
              "bytesBase64Encoded": "%s"
            }
          }],
          "parameters": {
            "sampleCount": 1
          }
        }
        """.formatted(escape(prompt), base64Image);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                IMAGEN_ENDPOINT.formatted(PROJECT_ID, LOCATION)
                        ))
                        .header("Authorization", "Bearer " + fetchAccessToken())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("IMAGEN STATUS: " + response.statusCode());
        System.out.println("IMAGEN RESPONSE BODY:");
        System.out.println(response.body());

        JsonNode root = mapper.readTree(response.body());

        if (!root.has("predictions")) {
            throw new RuntimeException(
                "Imagen error response: " + response.body()
            );
        }

        String styledImageBase64 =
            root.at("/predictions/0/bytesBase64Encoded").asText();


        return new StyledImageResult(styledImageBase64, fashionText);
    }

    private String buildPrompt(
            String fashionText,
            String gender,
            String season,
            String occasion,
            String weather
    ) {
        return """
        Keep the same person, face, body type, and pose.
        Modify ONLY the clothing and accessories.

        Fashion guidance:
        %s

        Context:
        Gender: %s
        Season: %s
        Occasion: %s
        Weather: %s

        Requirements:
        - Photorealistic
        - Editorial fashion quality
        - Realistic fabrics
        - Natural lighting
        - No face or body changes
        """.formatted(
                fashionText, gender, season, occasion, weather
        );
    }

    private String fetchAccessToken() throws Exception {
        Process process = new ProcessBuilder(
                "gcloud", "auth", "print-access-token"
        ).start();

        return new String(process.getInputStream().readAllBytes()).trim();
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }

    // DTO returned to controller
    public record StyledImageResult(
            String imageBase64,
            String fashionText
    ) {}
}

