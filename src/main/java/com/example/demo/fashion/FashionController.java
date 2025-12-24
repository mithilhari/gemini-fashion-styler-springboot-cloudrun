package com.example.demo.fashion;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fashion")
public class FashionController {

  private final FashionService service;

  public FashionController(FashionService service) {
    this.service = service;
  }

  @PostMapping(
      path = "/style",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public FashionResponse style(
      @RequestPart("image") MultipartFile image,
      @RequestPart(required = false) String occasion,
      @RequestPart(required = false) String vibe,
      @RequestPart(required = false) String notes
  ) throws Exception {
    return service.style(image, occasion, vibe, notes);
  }

  @PostMapping(
      path = "/generate-image",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ImageGenResponse generateImage(@RequestBody ImageGenRequest req)
      throws Exception {
    return service.generateImageWithGeminiFlashImage(req.getPrompt());
  }
}

