package com.ieum.ieum.service;

import com.ieum.ieum.client.TmapDirectionsClient;
import com.ieum.ieum.client.TmapTransitClient;
import com.ieum.ieum.domain.Navigation;
import com.ieum.ieum.dto.NavigationCompleteRequest;
import com.ieum.ieum.dto.NavigationHistoryResponse;
import com.ieum.ieum.dto.RouteCandidate;
import com.ieum.ieum.dto.RouteRequest;
import com.ieum.ieum.dto.RouteResponse;
import com.ieum.ieum.dto.TransitCandidatesResponse;
import com.ieum.ieum.dto.TransitItinerary;
import com.ieum.ieum.repository.NavigationRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;   // ★ Jackson 3 경로 (아래 주의 참고)
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ieum.ieum.dto.Crosswalk;
import com.ieum.ieum.dto.CrosswalkAccessibilitySummary;
import com.ieum.ieum.dto.Stair;
import java.util.List;

@Slf4j
@Service
public class NavigationService {

    private static final int RECOMMENDED_OPTION = 0;
    private static final int NO_STAIRS_OPTION = 30;

    private final TmapDirectionsClient tmapClient;
    private final TmapTransitClient tmapTransitClient;
    private final NavigationRepository navigationRepository;
    private final ObjectMapper objectMapper;
    private final RouteParser routeParser;
    private final TransitRouteParser transitRouteParser;
    private final CrosswalkAccessibilityService crosswalkAccessibilityService;

    public NavigationService(TmapDirectionsClient tmapClient,
                             TmapTransitClient tmapTransitClient,
                             NavigationRepository navigationRepository,
                             ObjectMapper objectMapper,
                             RouteParser routeParser,
                             TransitRouteParser transitRouteParser,
                             CrosswalkAccessibilityService crosswalkAccessibilityService) {
        this.tmapClient = tmapClient;
        this.tmapTransitClient = tmapTransitClient;
        this.navigationRepository = navigationRepository;
        this.objectMapper = objectMapper;
        this.routeParser = routeParser;
        this.transitRouteParser = transitRouteParser;
        this.crosswalkAccessibilityService = crosswalkAccessibilityService;
    }

    /**
     * 출발/도착 좌표에 대해 "추천 경로"와 "계단 없는 경로" 두 후보를 미리 보여준다.
     * DB에 저장하지 않으며, 사용자가 이 중 하나를 고르면 그 searchOption으로 createRoute를 호출해 확정한다.
     */
    public List<RouteCandidate> searchRouteCandidates(RouteRequest req) {
        RouteCandidate recommended = buildCandidate(req, RECOMMENDED_OPTION, "추천 경로");
        RouteCandidate noStairs = buildCandidate(req, NO_STAIRS_OPTION, "계단 없는 경로");
        return List.of(recommended, noStairs);
    }

    /**
     * 대중교통 경로 후보 + 비교용 도보 전용 후보를 함께 조회한다.
     * 대중교통 API 호출/파싱이 실패해도(서비스 장애, 상품 미신청 등) 도보 비교는 보여줄 수 있어야
     * 하므로 예외를 격리해서 빈 리스트로 내려준다 (HazardAlertService와 동일한 방식).
     */
    public TransitCandidatesResponse searchTransitCandidates(RouteRequest req) {
        List<TransitItinerary> transitOptions;
        try {
            String raw = tmapTransitClient.search(req.startLng(), req.startLat(), req.endLng(), req.endLat());
            transitOptions = transitRouteParser.parseItineraries(raw);
        } catch (Exception e) {
            log.warn("[대중교통 경로 조회 실패] {}", e.getMessage());
            transitOptions = List.of();
        }
        RouteCandidate walkOption = buildCandidate(req, RECOMMENDED_OPTION, "도보 전용");
        return new TransitCandidatesResponse(transitOptions, walkOption);
    }

    private RouteCandidate buildCandidate(RouteRequest req, int searchOption, String label) {
        String raw = fetchRoute(req, searchOption);
        RouteParser.RouteTotals totals = routeParser.extractTotals(raw);
        List<Crosswalk> crosswalks = routeParser.extractCrosswalks(raw);
        return new RouteCandidate(
                searchOption,
                label,
                totals.distance(),
                totals.duration(),
                crosswalks,
                routeParser.extractStairs(raw),
                crosswalkAccessibilityService.summarizeForRoute(crosswalks)
        );
    }

    @Transactional
    public RouteResponse createRoute(Long userId, RouteRequest req) {
        int searchOption = req.searchOption() != null ? req.searchOption() : RECOMMENDED_OPTION;
        String raw = fetchRoute(req, searchOption);
        RouteParser.RouteTotals totals = routeParser.extractTotals(raw);

        Navigation nav = Navigation.builder()
                .userId(userId)
                .startLng(req.startLng()).startLat(req.startLat())
                .endLng(req.endLng()).endLat(req.endLat())
                .routeData(raw)
                .distance(totals.distance())
                .duration(totals.duration())
                .build();

        navigationRepository.save(nav);
        List<Crosswalk> crosswalks = routeParser.extractCrosswalks(raw);
        List<Stair> stairs = routeParser.extractStairs(raw);
        CrosswalkAccessibilitySummary accessibilitySummary = crosswalkAccessibilityService.summarizeForRoute(crosswalks);
        return new RouteResponse(
                nav.getId(),
                nav.getDistance(),
                nav.getDuration(),
                crosswalks,
                stairs,
                accessibilitySummary,
                raw
        );
    }

    private String fetchRoute(RouteRequest req, int searchOption) {
        String raw = tmapClient.walk(req.startLng(), req.startLat(), req.endLng(), req.endLat(), searchOption);

        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("티맵 경로 응답을 해석하지 못했습니다.", e);
        }
        if (root.has("error")) {
            throw new IllegalArgumentException("경로를 찾을 수 없습니다. (" + root.path("error").path("message").asString() + ")");
        }
        return raw;
    }

    // "내 경로" 탭에서 유저별 길찾기 기록을 최신순으로 조회 (Repository 메서드는 기존에 이미 준비돼 있었음)
    public List<NavigationHistoryResponse> getHistory(Long userId) {
        return navigationRepository.findByUserIdOrderByStartTimeDesc(userId).stream()
                .map(NavigationHistoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void complete(Long userId, Long navigationId, NavigationCompleteRequest req) {
        Navigation nav = navigationRepository.findById(navigationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길찾기 기록입니다."));
        if (!nav.getUserId().equals(userId)) {   // 남의 기록은 못 건드림
            throw new IllegalArgumentException("본인의 길찾기 기록만 수정할 수 있습니다.");
        }
        nav.complete(req.obstacleCnt(), req.completed());
    }
}
