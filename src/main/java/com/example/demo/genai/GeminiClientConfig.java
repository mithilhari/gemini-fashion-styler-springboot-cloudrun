package com.example.demo.genai;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {
  @Bean
  Client genAiClient() {
    // Uses Gemini Developer API by default and reads GOOGLE_API_KEY from env
    return new Client();
  }
}
