package com.example.demo.genai;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    @Bean
    public Client geminiClient() {
        return Client.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();
    }
}

