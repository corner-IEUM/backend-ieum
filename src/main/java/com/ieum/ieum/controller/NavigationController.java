package com.ieum.ieum.controller;

import com.ieum.ieum.dto.NavigationCompleteRequest;
import com.ieum.ieum.dto.RouteRequest;
import com.ieum.ieum.dto.RouteResponse;
import com.ieum.ieum.service.NavigationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final NavigationService navigationService;

    public NavigationController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @PostMapping("/route")
    public ResponseEntity<RouteResponse> createRoute(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(navigationService.createRoute(userId, request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> complete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody NavigationCompleteRequest request) {
        navigationService.complete(userId, id, request);
        return ResponseEntity.noContent().build();
    }
}