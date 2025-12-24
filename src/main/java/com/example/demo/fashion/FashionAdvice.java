package com.example.demo.fashion;

import java.util.List;

public class FashionAdvice {

  private String observations;
  private List<String> quickFixes;
  private String imagePrompt;

  public FashionAdvice() {}

  public FashionAdvice(String observations, List<String> quickFixes, String imagePrompt) {
    this.observations = observations;
    this.quickFixes = quickFixes;
    this.imagePrompt = imagePrompt;
  }

  public String getObservations() {
    return observations;
  }

  public List<String> getQuickFixes() {
    return quickFixes;
  }

  public String getImagePrompt() {
    return imagePrompt;
  }
}

