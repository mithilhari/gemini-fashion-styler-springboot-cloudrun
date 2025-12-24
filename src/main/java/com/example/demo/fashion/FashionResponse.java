package com.example.demo.fashion;

public class FashionResponse {

  private String observations;
  private String[] quickFixes;
  private String imagePrompt;

  public FashionResponse(
      String observations,
      String[] quickFixes,
      String imagePrompt
  ) {
    this.observations = observations;
    this.quickFixes = quickFixes;
    this.imagePrompt = imagePrompt;
  }

  public String getObservations() {
    return observations;
  }

  public String[] getQuickFixes() {
    return quickFixes;
  }

  public String getImagePrompt() {
    return imagePrompt;
  }
}

