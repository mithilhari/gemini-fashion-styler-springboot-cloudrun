package com.example.demo.fashion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/fashion")
public class FashionController {

    private final FashionImageService fashionImageService;

    public FashionController(FashionImageService fashionImageService) {
        this.fashionImageService = fashionImageService;
    }

    @PostMapping("/style")
    public ResponseEntity<FashionImageService.StyledImageResult> styleFashion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("occasion") String occasion,
            @RequestParam("style") String style,
            @RequestParam("color") String color,
            @RequestParam("season") String season,
            @RequestParam(value = "additionalPrompt", required = false) String additionalPrompt) {

        // Calls the service with all form parameters
        FashionImageService.StyledImageResult result = fashionImageService.styleImage(
                file, occasion, style, color, season, additionalPrompt
        );

        return ResponseEntity.ok(result);
    }
}
