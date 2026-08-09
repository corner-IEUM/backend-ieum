package com.ieum.ieum.controller;

import com.ieum.ieum.dto.PlaceSearchResponse;
import com.ieum.ieum.service.PlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/search")
    public ResponseEntity<PlaceSearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double lat) {
        return ResponseEntity.ok(placeService.search(query, lng, lat));
    }
}