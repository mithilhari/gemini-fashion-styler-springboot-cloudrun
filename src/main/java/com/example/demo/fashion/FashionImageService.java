package com.example.demo.fashion;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateImagesRequest;
import com.google.cloud.vertexai.api.Image;
import com.google.cloud.vertexai.api.ImagePrompt;
import com.google.cloud.vertexai.api.ImageSeed;
import com.google.cloud.vertexai.api.ImageSource;
import com.google.cloud.vertexai.api.PredictResponse;
import com.google.cloud.vertexai.generativeai.ImageGenerationModel;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@Service
public class FashionImageService {

    private static final String PROJECT_ID = System.getenv("GOOGLE_CLOUD_PROJECT");
    private static final String LOCATION = "us-central1";
    private static final String MODEL = "imagen-3.0-edit-001";

    public StyledImageResult styleImage(
            MultipartFile image,
            String fashionText,
            String gender,
            String season,
            String occasion,
            String weather
    ) throws Exception {

        // 1️⃣ Build image prompt
        String prompt = buildImagePrompt(
                fashionText, gender, season, occasion, weather
        );

        // 2️⃣ Prepare source image
        ImageSource sourceImage =
                ImageSource.newBuilder()
                        .setBytes(image.getBytes())
                        .build();

        ImagePrompt imagePrompt =
                ImagePrompt.newBuilder()
                        .setPrompt(prompt)
                        .addImage(sourceImage)
                        .build();

        // 3️⃣ Call Vertex AI Imagen
        try (VertexAI vertexAI = new VertexAI(PROJECT_ID, LOCATION)) {

            ImageGenerationModel model =
                    new ImageGenerationModel(MODEL, vertexAI);

            PredictResponse response =
                    model.generateImages(
                            GenerateImagesRequest.newBuilder()
                                    .addImagePrompts(imagePrompt)
                                    .setSampleCount(1)
                                    .build()
                    );

            Image generated =
                    response.getPredictions(0)
                            .getStructValue()
                            .getFieldsOrThrow("bytesBase64Encoded")
                            .getStringValue()
                            .isEmpty()
                            ? null
                            : null;

            // Imagen SDK returns base64 directly
            String base64Image =
                    response.getPredictions(0)
                            .getStructValue()
                            .getFieldsOrThrow("bytesBase64Encoded")
                            .getStringValue();

            return new StyledImageResult(base64Image, fashionText);
        }
    }

    private String buildImagePrompt(
            String fashionText,
            String gender,
            String season,
            String occasion,
            String weather
    ) {
        return """
        Keep the same person, face, body type, and pose.
        Update the outfit according to this fashion guidance:

        %s

        Context:
        Gender: %s
        Season: %s
        Occasion: %s
        Weather: %s

        Style rules:
        - Photorealistic
        - Fashion editorial quality
        - Realistic fabric textures
        - Natural lighting
        - Subtle, believable outfit changes
        """.formatted(
                fashionText, gender, season, occasion, weather
        );
    }

    // Simple DTO
    public record StyledImageResult(
            String imageBase64,
            String fashionText
    ) {}
}

