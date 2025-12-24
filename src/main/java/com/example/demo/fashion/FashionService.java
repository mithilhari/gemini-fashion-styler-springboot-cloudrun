package com.example.demo.fashion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@Service
public class FashionService {

  private final Client client;
  private final ObjectMapper mapper = new ObjectMapper();

  public FashionService(Client client) {
    this.client = client;
  }

  /**
   * STEP 1 + 2
   * Analyze the uploaded image and return observations, quick fixes,
   * and a high-quality image generation prompt.
   */
  public FashionResponse style(
      MultipartFile image,
      String occasion,
      String vibe,
      String notes
  ) throws Exception {

    byte[] imageBytes = image.getBytes();
    String mimeType = image.getContentType();

    String prompt = """
Return ONLY valid JSON:
{
  "observations": "string",
  "quickFixes": ["string"],
  "imagePrompt": "string"
}

Rules:
- No markdown
- No ellipses
- Describe only visible clothing
- No identity inference

Occasion: %s
Vibe: %s
Notes: %s
""".formatted(
        safe(occasion),
        safe(vibe),
        safe(notes)
    );

    Content content = Content.fromParts(
        Part.fromBytes(imageBytes, mimeType),
        Part.fromText(prompt)
    );

    GenerateContentResponse response =
        client.models.generateContent("gemini-2.5-flash", content, null);

    FashionAdvice advice =
        mapper.readValue(clean(response.text()), FashionAdvice.class);

    return new FashionResponse(
        advice.getObservations(),
        advice.getQuickFixes().toArray(String[]::new),
        advice.getImagePrompt()
    );
  }

  /**
   * STEP 4
   * Generate an image from the previously generated prompt.
   */
  public ImageGenResponse generateImage(String prompt) {

    Content content = Content.fromParts(
        Part.fromText(prompt)
    );

    GenerateContentResponse response =
        client.models.generateContent("gemini-2.5-flash", content, null);

    var candidatesOpt = response.candidates();
    if (candidatesOpt.isEmpty() || candidatesOpt.get().isEmpty()) {
      throw new RuntimeException("No candidates returned by Gemini");
    }

    var candidate = candidatesOpt.get().get(0);

    var contentOpt = candidate.content();
    if (contentOpt.isEmpty()) {
      throw new RuntimeException("No content in Gemini response");
    }

    var partsOpt = contentOpt.get().parts();
    if (partsOpt.isEmpty()) {
      throw new RuntimeException("No parts in Gemini content");
    }

    byte[] outBytes = null;
    String outMime = "image/png";

    for (Part p : partsOpt.get()) {
      var inlineOpt = p.inlineData();
      if (inlineOpt.isPresent()) {
        var blob = inlineOpt.get();
        outBytes = blob.data().orElse(null);
        outMime = blob.mimeType().orElse("image/png");
        break;
      }
    }

    if (outBytes == null) {
      throw new RuntimeException("Image generation failed: no image bytes");
    }

    String base64 = Base64.getEncoder().encodeToString(outBytes);

    return new ImageGenResponse(outMime, base64);
  }

  // -----------------------------
  // Helpers
  // -----------------------------

  private String clean(String raw) {
    raw = raw.replace("```json", "").replace("```", "").trim();
    int a = raw.indexOf('{');
    int b = raw.lastIndexOf('}');
    return raw.substring(a, b + 1);
  }

  private String safe(String s) {
    return s == null ? "" : s;
  }
}

