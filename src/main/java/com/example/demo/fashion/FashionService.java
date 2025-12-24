package com.example.demo.fashion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ImageConfig;
import com.google.genai.types.Part;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FashionService {

  private static final Set<String> ALLOWED_MIME =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final long MAX_BYTES = 8L * 1024 * 1024;

  private final Client client;
  private final ObjectMapper om = new ObjectMapper();

  public FashionService(Client client) {
    this.client = client;
  }

  public FashionResponse suggestAndGenerate(
      MultipartFile file,
      String occasion,
      String vibe,
      String notes,
      String aspectRatio
  ) {
    validate(file);

    final byte[] imageBytes;
    final String mimeType;
    try {
      imageBytes = file.getBytes();
      mimeType = file.getContentType();
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded image.");
    }

    FashionAdvice advice = generateAdvice(imageBytes, mimeType, occasion, vibe, notes);

    GeneratedImage generated = generateEditedImage(imageBytes, mimeType, advice.imagePrompt(), aspectRatio);

    return new FashionResponse(
        advice.observations(),
        advice.quickFixes(),
        advice.imagePrompt(),
        generated.mimeType(),
        generated.base64()
    );
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload an image.");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image too large. Max 8MB.");
    }
    String ct = file.getContentType();
    if (ct == null || !ALLOWED_MIME.contains(ct)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Unsupported file type. Use JPG, PNG, or WEBP.");
    }
  }

  private FashionAdvice generateAdvice(
      byte[] imageBytes,
      String mimeType,
      String occasion,
      String vibe,
      String notes
  ) {
    String occ = (occasion == null || occasion.isBlank()) ? "everyday" : occasion.trim();
    String vb = (vibe == null || vibe.isBlank()) ? "clean, modern" : vibe.trim();
    String extra = (notes == null) ? "" : notes.trim();

    String instruction = """
You are a fashion stylist. Analyze the person’s outfit in the uploaded photo and give SIMPLE, practical improvements.

Return STRICT JSON only with these keys:
- observations: string (what they are wearing + what stands out; no guessing identity or sensitive traits)
- quickFixes: array of 6-10 short strings (easy changes: fit, layering, color harmony, accessories, shoes, grooming)
- imagePrompt: string (ONE prompt for an image editor model to update the OUTFIT while keeping the same person, pose, and background)

Constraints for imagePrompt:
- Photorealistic
- Keep same person identity, face, pose, and background
- Only change clothing + accessories + styling details
- No logos, no brand marks, no text overlays, no watermark
Target occasion: %s
Target vibe: %s
User notes: %s
""".formatted(occ, vb, extra);

    Content content = Content.fromParts(
        Part.fromBytes(imageBytes, mimeType),
        Part.fromText(instruction)
    );

    GenerateContentResponse resp =
        client.models.generateContent("gemini-2.5-flash", content, null);

    String text = resp.text();
    try {
      return om.readValue(text, FashionAdvice.class);
    } catch (Exception parseErr) {
      return new FashionAdvice(
          "Could not parse structured observations. Raw: " + safeTrim(text),
          List.of("Try again with a clearer full-body photo", "Add occasion/vibe notes"),
          "Photorealistic fashion edit of the input photo: refine the outfit for " + occ +
              " with a " + vb + " vibe. Keep the same person, face, pose, and background. " +
              "Only adjust clothing, fit, colors, and accessories. No logos, no text."
      );
    }
  }

  private GeneratedImage generateEditedImage(
      byte[] imageBytes,
      String mimeType,
      String prompt,
      String aspectRatio
  ) {
    GenerateContentConfig.Builder cfg = GenerateContentConfig.builder()
        .responseModalities("IMAGE");

    if (aspectRatio != null && !aspectRatio.isBlank()) {
      cfg.imageConfig(ImageConfig.builder().aspectRatio(aspectRatio.trim()).build());
    }

    Content content = Content.fromParts(
        Part.fromBytes(imageBytes, mimeType),
        Part.fromText(prompt + "\nDo not change the input aspect ratio.")
    );

    GenerateContentResponse response =
        client.models.generateContent("gemini-2.5-flash-image", content, cfg.build());

    for (Part part : response.parts()) {
      if (part.inlineData().isPresent()
          && part.inlineData().get().data().isPresent()) {
        byte[] out = part.inlineData().get().data().get();
        String b64 = Base64.getEncoder().encodeToString(out);
        return new GeneratedImage("image/png", b64);
      }
    }

    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No image returned by Gemini.");
  }

  private String safeTrim(String s) {
    if (s == null) return "";
    return s.length() > 800 ? s.substring(0, 800) + "..." : s;
  }

  public record FashionAdvice(String observations, List<String> quickFixes, String imagePrompt) {}

  public record FashionResponse(
      String observations,
      List<String> quickFixes,
      String promptUsed,
      String mimeType,
      String base64
  ) {}

  private record GeneratedImage(String mimeType, String base64) {}
}
