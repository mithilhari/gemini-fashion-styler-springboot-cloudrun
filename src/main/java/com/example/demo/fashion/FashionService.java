package com.example.demo.fashion;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FashionService {

    private static final int MAX_PROMPT_CHARS = 100_000;

    private final Client geminiClient;

    public FashionService(Client geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String style(
            MultipartFile image,
            String gender,
            String occasion,
            String season,
            String vibe
    ) {
        // ❌ DO NOT READ IMAGE BYTES INTO PROMPT
        // ❌ DO NOT BASE64 ENCODE IMAGE

        String prompt = """
        You are a professional fashion stylist.

        Based on the following user preferences, suggest a complete outfit.

        Gender: %s
        Occasion: %s
        Season: %s
        Vibe: %s

        Respond with:
        1. Outfit description
        2. Colors
        3. Accessories
        4. Footwear
        """.formatted(
                safe(gender),
                safe(occasion),
                safe(season),
                safe(vibe)
        );

        prompt = clampPrompt(prompt);

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        "gemini-2.0-flash",
                        prompt,
                        null
                );

        String text = response.text();

        if (text == null || text.isBlank()) {
            throw new RuntimeException("Gemini returned no text output");
        }

        return text;
    }

    private String clampPrompt(String prompt) {
        if (prompt.length() > MAX_PROMPT_CHARS) {
            return prompt.substring(0, MAX_PROMPT_CHARS);
        }
        return prompt;
    }

    private String safe(String value) {
        return value == null ? "unspecified" : value.trim();
    }
}

