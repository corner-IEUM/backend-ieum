package com.ieum.ieum.service;

import com.ieum.ieum.client.TmapDirectionsClient;
import com.ieum.ieum.domain.Navigation;
import com.ieum.ieum.dto.NavigationCompleteRequest;
import com.ieum.ieum.dto.RouteCandidate;
import com.ieum.ieum.dto.RouteRequest;
import com.ieum.ieum.dto.RouteResponse;
import com.ieum.ieum.repository.NavigationRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;   // ★ Jackson 3 경로 (아래 주의 참고)
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ieum.ieum.dto.Crosswalk;
import com.ieum.ieum.dto.CrosswalkAccessibilitySummary;
import com.ieum.ieum.dto.Stair;
import java.util.List;

@Service
public class NavigationService {

    private static final int RECOMMENDED_OPTION = 0;
    private static final int NO_STAIRS_OPTION = 30;

    private final TmapDirectionsClient tmapClient;
    private final NavigationRepository navigationRepository;
    private final ObjectMapper objectMapper;
    private final RouteParser routeParser;
    private final CrosswalkAccessibilityService crosswalkAccessibilityService;

    public NavigationService(TmapDirectionsClient tmapClient,
                             NavigationRepository navigationRepository,
                             ObjectMapper objectMapper,
                             RouteParser routeParser,
                             CrosswalkAccessibilityService crosswalkAccessibilityService) {
        this.tmapClient = tmapClient;
        this.navigationRepository = navigationRepository;
        this.objectMapper = objectMapper;
        this.routeParser = routeParser;
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
