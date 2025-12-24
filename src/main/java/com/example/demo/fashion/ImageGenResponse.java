package com.example.demo.fashion;

public class ImageGenResponse {

  private String mimeType;
  private String base64;

  public ImageGenResponse(String mimeType, String base64) {
    this.mimeType = mimeType;
    this.base64 = base64;
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getBase64() {
    return base64;
  }
}

