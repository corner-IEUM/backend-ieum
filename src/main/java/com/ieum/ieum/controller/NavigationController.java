package com.ieum.ieum.controller;

import com.ieum.ieum.dto.NavigationCompleteRequest;
import com.ieum.ieum.dto.NavigationHistoryResponse;
import com.ieum.ieum.dto.NearbyHazardRequest;
import com.ieum.ieum.dto.RouteCandidate;
import com.ieum.ieum.dto.RouteRequest;
import com.ieum.ieum.dto.RouteResponse;
import com.ieum.ieum.dto.TransitCandidatesResponse;
import com.ieum.ieum.service.HazardAlertService;
import com.ieum.ieum.service.NavigationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final NavigationService navigationService;
    private final HazardAlertService hazardAlertService;

    public NavigationController(NavigationService navigationService, HazardAlertService hazardAlertService) {
        this.navigationService = navigationService;
        this.hazardAlertService = hazardAlertService;
    }

    // "내 경로" 탭에서 프론트가 호출하는 API — 인증된 사용자의 길찾기 기록을 최신순으로 반환
    // GET /api/navigation/history
    @GetMapping("/history")
    public ResponseEntity<List<NavigationHistoryResponse>> getHistory(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(navigationService.getHistory(userId));
    }

    @PostMapping("/route/candidates")
    public ResponseEntity<List<RouteCandidate>> searchRouteCandidates(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(navigationService.searchRouteCandidates(request));
    }

    @PostMapping("/route")
    public ResponseEntity<RouteResponse> createRoute(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(navigationService.createRoute(userId, request));
    }

    // 대중교통 경로 후보 조회 — DB에 저장하지 않으며, 대중교통 후보(transitOptions, 없으면 빈 배열)와
    // 비교용 도보 전용 후보(walkOption)를 항상 함께 내려준다.
    @GetMapping("/transit/candidates")
    public ResponseEntity<TransitCandidatesResponse> searchTransitCandidates(
            @AuthenticationPrincipal Long userId,
            @RequestParam Double startLng,
            @RequestParam Double startLat,
            @RequestParam Double endLng,
            @RequestParam Double endLat) {
        RouteRequest req = new RouteRequest(startLng, startLat, endLng, endLat, null);
        return ResponseEntity.ok(navigationService.searchTransitCandidates(req));
    }

    @PostMapping("/nearby-hazards")
    public ResponseEntity<Void> checkNearbyHazards(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NearbyHazardRequest request) {
        hazardAlertService.checkAndNotify(request.lng(), request.lat());
        return ResponseEntity.noContent().build();
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