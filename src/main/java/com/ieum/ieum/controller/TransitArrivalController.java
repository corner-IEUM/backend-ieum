package com.ieum.ieum.controller;

import com.ieum.ieum.dto.ArrivalResponse;
import com.ieum.ieum.service.TransitArrivalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transit")
public class TransitArrivalController {

    private final TransitArrivalService transitArrivalService;

    public TransitArrivalController(TransitArrivalService transitArrivalService) {
        this.transitArrivalService = transitArrivalService;
    }

    @GetMapping("/arrivals")
    public ResponseEntity<List<ArrivalResponse>> getArrivals(
            @RequestParam String stopName,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) String routeName) {
        return ResponseEntity.ok(transitArrivalService.getArrivals(stopName, lat, lng, routeName));
    }
}