package com.ieum.ieum.service;

import com.ieum.ieum.dto.Crosswalk;
import com.ieum.ieum.dto.Stair;
import com.ieum.ieum.dto.Waypoint;
import com.ieum.ieum.dto.WaypointCategory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RouteParser {

    /**
     * 계단이 있을 가능성이 높은 지점의 키워드.
     * 티맵은 "계단"이라는 단어를 거의 안 쓰고 "지하보도"/"육교" 진입 안내로 표현하는 경우가 대부분이라
     * 세 키워드를 함께 매칭한다.
     */
    private static final List<String> STAIR_KEYWORDS = List.of("계단", "지하보도", "육교");
    private static final String CROSSWALK_KEYWORD = "횡단보도";

    private final ObjectMapper objectMapper;

    public RouteParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 티맵 경로 응답(GeoJSON)의 총 거리(m)/시간(s) */
    public record RouteTotals(int distance, int duration) {}

    public RouteTotals extractTotals(String raw) {
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("티맵 경로 응답을 해석하지 못했습니다.", e);
        }
        for (JsonNode feature : root.path("features")) {
            JsonNode props = feature.path("properties");
            if (props.has("totalDistance")) {
                return new RouteTotals(props.path("totalDistance").asInt(), props.path("totalTime").asInt());
            }
        }
        throw new IllegalStateException("티맵 경로 응답에서 총 거리/시간 정보를 찾을 수 없습니다.");
    }

    /** 경로 전체 좌표를 순서대로 [lng, lat] 목록으로 (GPS 재생용) */
    public List<double[]> extractPath(String raw) {
        List<double[]> path = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(raw);
            for (JsonNode feature : root.path("features")) {
                if (!"LineString".equals(feature.path("geometry").path("type").asString())) {
                    continue;
                }
                for (JsonNode pt : feature.path("geometry").path("coordinates")) {
                    path.add(new double[]{ pt.get(0).asDouble(), pt.get(1).asDouble() });
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return path;
    }

    /**
     * 경로상의 모든 안내 지점(출발/도착 제외)을 순서대로 추출한다.
     * 계단/횡단보도/일반 회전 안내를 하나의 파이프라인으로 통합해, 같은 지점이
     * 두 카테고리에 동시에 걸려 중복 안내되는 걸 막는다. 문구는 항상 티맵이 준 그대로 사용한다.
     */
    public List<Waypoint> extractWaypoints(String raw) {
        List<Waypoint> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(raw);
            for (JsonNode feature : root.path("features")) {
                if (!"Point".equals(feature.path("geometry").path("type").asString())) {
                    continue;
                }
                JsonNode props = feature.path("properties");
                String pointType = props.path("pointType").asString();
                if ("SP".equals(pointType) || "EP".equals(pointType)) {
                    continue; // 출발/도착은 별도 이벤트(재생 시작/도착 완료)로 처리
                }
                String description = props.path("description").asString();
                if (description == null || description.isBlank()) {
                    continue;
                }
                JsonNode coord = feature.path("geometry").path("coordinates");
                result.add(new Waypoint(coord.get(0).asDouble(), coord.get(1).asDouble(), description, categorize(description)));
            }
        } catch (Exception e) {
            return List.of();
        }
        return result;
    }

    private WaypointCategory categorize(String description) {
        if (STAIR_KEYWORDS.stream().anyMatch(description::contains)) {
            return WaypointCategory.STAIR;
        }
        if (description.contains(CROSSWALK_KEYWORD)) {
            return WaypointCategory.CROSSWALK;
        }
        return WaypointCategory.TURN;
    }

    /** "횡단보도" 지점의 좌표 목록 (초기 경로 응답용) */
    public List<Crosswalk> extractCrosswalks(String raw) {
        return extractWaypoints(raw).stream()
                .filter(w -> w.category() == WaypointCategory.CROSSWALK)
                .map(w -> new Crosswalk(w.lng(), w.lat(), w.guidance()))
                .toList();
    }

    /** 계단(지하보도/육교 포함) 지점의 좌표 목록 (초기 경로 응답용) */
    public List<Stair> extractStairs(String raw) {
        return extractWaypoints(raw).stream()
                .filter(w -> w.category() == WaypointCategory.STAIR)
                .map(w -> new Stair(w.lng(), w.lat(), w.guidance()))
                .toList();
    }
}
