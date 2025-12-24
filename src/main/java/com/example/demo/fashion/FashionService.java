package com.example.demo.fashion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class FashionService {

  private static final String REGION = "us-central1";
  private final ObjectMapper mapper = new ObjectMapper();

  // -------------------------------
  // STEP 1–3: ANALYSIS (Gemini Flash)
  // -------------------------------
  public FashionResponse style(
      MultipartFile image,
      String occasion,
      String vibe,
      String notes
  ) throws Exception {

    byte[] imageBytes = image.getBytes();
    String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes);

    String prompt = """
Return ONLY valid JSON:
{
  "observations": "string",
  "quickFixes": ["string"],
  "imagePrompt": "string"
}

Rules:
- No markdown
- No commentary
- No identity inference
- Describe visible clothing only

Occasion: %s
Vibe: %s
Notes: %s
""".formatted(
        safe(occasion),
        safe(vibe),
        safe(notes)
    );

    String body = """
{
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "inlineData": {
            "mimeType": "%s",
            "data": "%s"
          }
        },
        { "text": "%s" }
      ]
    }
  ]
}
""".formatted(
        image.getContentType(),
        imageBase64,
        escape(prompt)
    );

    JsonNode response =
        callVertex(
            "gemini-2.5-flash:generateContent",
            body
        );

    String text =
        response.at("/candidates/0/content/parts/0/text").asText();

    FashionAdvice advice =
        mapper.readValue(clean(text), FashionAdvice.class);

    return new FashionResponse(
        advice.getObservations(),
        advice.getQuickFixes().toArray(String[]::new),
        advice.getImagePrompt()
    );
  }

  // -----------------------------------------
  // STEP 4: IMAGE GENERATION (Flash Image 2.5)
  // -----------------------------------------
  public ImageGenResponse generateImageWithGeminiFlashImage(String prompt)
      throws Exception {

    String body = """
{
  "contents": [
    {
      "role": "user",
      "parts": [
        { "text": "%s" }
      ]
    }
  ]
}
""".formatted(escape(prompt));

    JsonNode response =
        callVertex(
            "gemini-2.5-flash-image:generateContent",
            body
        );

    JsonNode inline =
        response.at("/candidates/0/content/parts/0/inlineData");

    if (inline.isMissingNode()) {
      throw new RuntimeException("Gemini image generation failed");
    }

    return new ImageGenResponse(
        inline.get("mimeType").asText(),
        inline.get("data").asText()
    );
  }

  // -------------------------------
  // Vertex AI REST helper
  // -------------------------------
  private JsonNode callVertex(String method, String body)
      throws Exception {

    String token =
        GoogleCredentials.getApplicationDefault()
            .refreshAccessToken()
            .getTokenValue();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(
            "https://" + REGION +
            "-aiplatform.googleapis.com/v1/models/" + method))
        .header("Authorization", "Bearer " + token)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

    return mapper.readTree(response.body());
  }

  // -------------------------------
  // Helpers
  // -------------------------------
  private String clean(String raw) {
    raw = raw.replace("```json", "")
             .replace("```", "")
             .trim();
    int a = raw.indexOf('{');
    int b = raw.lastIndexOf('}');
    return raw.substring(a, b + 1);
  }

  private String escape(String s) {
    return s.replace("\"", "\\\"");
  }

  private String safe(String s) {
    return s == null ? "" : s;
  }
}

