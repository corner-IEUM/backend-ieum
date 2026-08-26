package com.ieum.ieum.service;

import com.ieum.ieum.client.TmapDirectionsClient;
import com.ieum.ieum.domain.Navigation;
import com.ieum.ieum.dto.NavigationCompleteRequest;
import com.ieum.ieum.dto.RouteRequest;
import com.ieum.ieum.dto.RouteResponse;
import com.ieum.ieum.repository.NavigationRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;   // ★ Jackson 3 경로 (아래 주의 참고)
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ieum.ieum.dto.Crosswalk;
import com.ieum.ieum.dto.Stair;
import java.util.List;

@Service
public class NavigationService {

    private final TmapDirectionsClient tmapClient;
    private final NavigationRepository navigationRepository;
    private final ObjectMapper objectMapper;
    private final RouteParser routeParser;

    public NavigationService(TmapDirectionsClient tmapClient,
                             NavigationRepository navigationRepository,
                             ObjectMapper objectMapper,
                             RouteParser routeParser) {
        this.tmapClient = tmapClient;
        this.navigationRepository = navigationRepository;
        this.objectMapper = objectMapper;
        this.routeParser = routeParser;
    }

    @Transactional
    public RouteResponse createRoute(Long userId, RouteRequest req) {
        String raw = tmapClient.walk(req.startLng(), req.startLat(), req.endLng(), req.endLat());

        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("티맵 경로 응답을 해석하지 못했습니다.", e);
        }
        if (root.has("error")) {
            throw new IllegalArgumentException("경로를 찾을 수 없습니다. (" + root.path("error").path("message").asString() + ")");
        }

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
        return new RouteResponse(
                nav.getId(),
                nav.getDistance(),
                nav.getDuration(),
                crosswalks,
                stairs,
                raw
        );
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