package com.ieum.ieum.service;

import com.ieum.ieum.dto.Crosswalk;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RouteParser {

    private final ObjectMapper objectMapper;

    public RouteParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 경로 전체 좌표를 순서대로 [lng, lat] 목록으로 (GPS 재생용) */
    public List<double[]> extractPath(String raw) {
        List<double[]> path = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(raw);
            for (JsonNode leg : root.path("route").path("legs")) {
                for (JsonNode step : leg.path("steps")) {
                    for (JsonNode pt : step.path("path").path("points")) {
                        path.add(new double[]{ pt.get(0).asDouble(), pt.get(1).asDouble() });
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return path;
    }

    /** "횡단보도" step의 좌표 목록 */
    public List<Crosswalk> extractCrosswalks(String raw) {
        List<Crosswalk> crosswalks = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(raw);
            for (JsonNode leg : root.path("route").path("legs")) {
                for (JsonNode step : leg.path("steps")) {
                    JsonNode props = step.path("properties");
                    String guidance = props.path("guidance").asString();
                    if (guidance.contains("횡단보도")) {
                        crosswalks.add(new Crosswalk(
                                props.path("x").asDouble(),
                                props.path("y").asDouble(),
                                guidance));
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return crosswalks;
    }
}