package com.ieum.ieum.service;

import com.ieum.ieum.dto.Crosswalk;
import com.ieum.ieum.dto.Stair;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Component
public class RouteParser {

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

    /** "횡단보도" 지점(Point feature)의 좌표 목록 */
    public List<Crosswalk> extractCrosswalks(String raw) {
        return extractPointsContaining(raw, List.of("횡단보도"), (coord, desc) -> new Crosswalk(coord[0], coord[1], desc));
    }

    /**
     * 계단이 있을 가능성이 높은 지점(Point feature) 목록.
     * 티맵은 "계단"이라는 단어를 거의 안 쓰고 "지하보도"/"육교" 진입 안내로 표현하는 경우가 대부분이라
     * 세 키워드를 함께 매칭한다.
     */
    public List<Stair> extractStairs(String raw) {
        return extractPointsContaining(raw, List.of("계단", "지하보도", "육교"), (coord, desc) -> new Stair(coord[0], coord[1], desc));
    }

    /** description에 keyword 중 하나라도 포함된 Point feature들을 좌표+안내문구로 추출 */
    private <T> List<T> extractPointsContaining(String raw, List<String> keywords, BiFunction<double[], String, T> factory) {
        List<T> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(raw);
            for (JsonNode feature : root.path("features")) {
                if (!"Point".equals(feature.path("geometry").path("type").asString())) {
                    continue;
                }
                String description = feature.path("properties").path("description").asString();
                if (description != null && keywords.stream().anyMatch(description::contains)) {
                    JsonNode coord = feature.path("geometry").path("coordinates");
                    result.add(factory.apply(new double[]{ coord.get(0).asDouble(), coord.get(1).asDouble() }, description));
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return result;
    }
}
