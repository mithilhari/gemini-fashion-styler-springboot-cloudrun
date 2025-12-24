package com.example.demo.fashion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FashionService {

  private final Client client;
  private final ObjectMapper mapper = new ObjectMapper();

  public FashionService(Client client) {
    this.client = client;
  }

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

