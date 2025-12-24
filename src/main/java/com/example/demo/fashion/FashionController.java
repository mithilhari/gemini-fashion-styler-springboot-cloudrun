package com.example.demo.fashion;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fashion")
public class FashionController {

  private final FashionService fashionService;

  public FashionController(FashionService fashionService) {
    this.fashionService = fashionService;
  }

  @PostMapping(
      value = "/style",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public FashionService.FashionResponse style(
      @RequestPart("image") MultipartFile image,
      @RequestParam(value = "occasion", required = false) String occasion,
      @RequestParam(value = "vibe", required = false) String vibe,
      @RequestParam(value = "notes", required = false) String notes,
      @RequestParam(value = "aspectRatio", required = false) String aspectRatio
  ) {
    return fashionService.suggestAndGenerate(image, occasion, vibe, notes, aspectRatio);
  }
}
