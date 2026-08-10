package com.ieum.ieum.service;

import com.ieum.ieum.client.KakaoDirectionsClient;
import com.ieum.ieum.domain.Navigation;
import com.ieum.ieum.dto.KakaoWalkResponse;
import com.ieum.ieum.dto.NavigationCompleteRequest;
import com.ieum.ieum.dto.RouteRequest;
import com.ieum.ieum.dto.RouteResponse;
import com.ieum.ieum.repository.NavigationRepository;
import tools.jackson.databind.ObjectMapper;   // ★ Jackson 3 경로 (아래 주의 참고)
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ieum.ieum.dto.Crosswalk;
import java.util.List;

@Service
public class NavigationService {

    private final KakaoDirectionsClient kakaoClient;
    private final NavigationRepository navigationRepository;
    private final ObjectMapper objectMapper;
    private final RouteParser routeParser;

    public NavigationService(KakaoDirectionsClient kakaoClient,
                             NavigationRepository navigationRepository,
                             ObjectMapper objectMapper,
                             RouteParser routeParser) {
        this.kakaoClient = kakaoClient;
        this.navigationRepository = navigationRepository;
        this.objectMapper = objectMapper;
        this.routeParser = routeParser;
    }

    @Transactional
    public RouteResponse createRoute(Long userId, RouteRequest req) {
        String raw = kakaoClient.walk(req.startLng(), req.startLat(), req.endLng(), req.endLat());

        KakaoWalkResponse parsed;
        try {
            parsed = objectMapper.readValue(raw, KakaoWalkResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("카카오 경로 응답을 해석하지 못했습니다.", e);
        }

        if (parsed.route() == null || !"OK".equals(parsed.status())) {
            throw new IllegalArgumentException("경로를 찾을 수 없습니다. (status: " + parsed.status() + ")");
        }

        Navigation nav = Navigation.builder()
                .userId(userId)
                .startLng(req.startLng()).startLat(req.startLat())
                .endLng(req.endLng()).endLat(req.endLat())
                .routeData(raw)
                .distance(parsed.route().properties().totalDistance())
                .duration(parsed.route().properties().totalTime())
                .build();

        navigationRepository.save(nav);
        List<Crosswalk> crosswalks = routeParser.extractCrosswalks(raw);
        return new RouteResponse(
                nav.getId(),
                nav.getDistance(),
                nav.getDuration(),
                crosswalks,
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