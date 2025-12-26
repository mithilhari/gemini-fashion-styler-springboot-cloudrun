package com.example.demo.fashion;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.fashion.GeminiTextService;


@RestController
@RequestMapping("/fashion")
public class FashionController {

    private final FashionImageService fashionImageService;
    private final GeminiTextService geminiTextService;

    public FashionController(
            FashionImageService fashionImageService,
            GeminiTextService geminiTextService
    ) {
        this.fashionImageService = fashionImageService;
        this.geminiTextService = geminiTextService;
    }

    @PostMapping(
            value = "/style",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> styleFashion(
            @RequestPart("image") MultipartFile image,
            @RequestParam(defaultValue = "unisex") String gender,
            @RequestParam(defaultValue = "all") String season,
            @RequestParam(required = false) String occasion,
            @RequestParam(required = false) String weather
    ) throws Exception {

        String fashionText =
                geminiTextService.generateFashionAdvice(
                        gender, season, occasion, weather
                );

        FashionImageService.StyledImageResult result =
                fashionImageService.styleImage(
                        image, fashionText, gender, season, occasion, weather
                );

        return ResponseEntity.ok(result);
    }

}

