package com.example.demo.fashion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/fashion")
public class FashionController {

    private final FashionService fashionService;

    public FashionController(FashionService fashionService) {
        this.fashionService = fashionService;
    }

    @PostMapping("/style")
    public ResponseEntity<?> style(
            @RequestParam MultipartFile image,
            @RequestParam(value = "gender", required = false) String gender,
            @RequestParam String occasion,
            @RequestParam(value = "season", required = false) String season,
            @RequestParam String vibe
    ) {
        return ResponseEntity.ok(
                fashionService.style(image, gender, occasion, season, vibe)
        );
    }
}

