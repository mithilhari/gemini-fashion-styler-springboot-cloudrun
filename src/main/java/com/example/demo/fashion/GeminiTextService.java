package com.example.demo.fashion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GeminiTextService {

    private static final String PROJECT_ID =
            System.getenv("GOOGLE_CLOUD_PROJECT");

    private static final String LOCATION = "us-central1";

    private static final String GEMINI_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1/models/gemini-1.0-pro:generateContent";


    private final ObjectMapper mapper = new ObjectMapper();

    public String generateFashionAdvice(
            String gender,
            String season,
            String occasion,
            String weather
    ) throws Exception {

        String prompt = """
        You are a professional fashion stylist.
        Provide concise outfit styling advice.

        Gender: %s
        Season: %s
        Occasion: %s
        Weather: %s
        """.formatted(
                gender, season, occasion, weather
        );

        String requestBody = """
        {
          "contents": [{
            "role": "user",
            "parts": [{ "text": "%s" }]
          }]
        }
        """.formatted(escape(prompt));

        String accessToken = fetchAccessToken();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                GEMINI_ENDPOINT.formatted(PROJECT_ID, LOCATION)
                        ))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", System.getenv("GEMINI_API_KEY"))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("GEMINI STATUS: " + response.statusCode());
        System.out.println("GEMINI RESPONSE BODY:");
        System.out.println(response.body());


        JsonNode root = mapper.readTree(response.body());

        return root.at("/candidates/0/content/parts/0/text").asText();
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
}

