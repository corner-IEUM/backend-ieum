package com.ieum.ieum.service;

import com.ieum.ieum.dto.LegPoint;
import com.ieum.ieum.dto.TransitItinerary;
import com.ieum.ieum.dto.TransitLeg;
import com.ieum.ieum.dto.TransitRideLeg;
import com.ieum.ieum.dto.TransitStation;
import com.ieum.ieum.dto.WalkLeg;
import com.ieum.ieum.dto.WalkStep;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** 티맵 대중교통 길찾기(POST /transit/routes) 응답을 itinerary/leg 구조로 파싱한다. */
@Component
public class TransitRouteParser {

    private final ObjectMapper objectMapper;

    public TransitRouteParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 응답에 담긴 경로 후보 전체를 순서대로 반환 (후보 없음 = 빈 리스트, 상위에서 도보 전용 폴백 판단에 사용) */
    public List<TransitItinerary> parseItineraries(String raw) {
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("티맵 대중교통 응답을 해석하지 못했습니다.", e);
        }
        if (root.has("error")) {
            throw new IllegalArgumentException("대중교통 경로를 찾을 수 없습니다. (" + root.path("error").path("message").asString() + ")");
        }

        List<TransitItinerary> itineraries = new ArrayList<>();
        for (JsonNode node : root.path("metaData").path("plan").path("itineraries")) {
            itineraries.add(parseItinerary(node));
        }
        return itineraries;
    }

    private TransitItinerary parseItinerary(JsonNode node) {
        List<TransitLeg> legs = new ArrayList<>();
        for (JsonNode legNode : node.path("legs")) {
            legs.add(parseLeg(legNode));
        }
        return new TransitItinerary(
                node.path("totalTime").asInt(),
                node.path("totalDistance").asInt(),
                node.path("totalWalkTime").asInt(),
                node.path("totalWalkDistance").asInt(),
                node.path("transferCount").asInt(),
                node.path("fare").path("regular").path("totalFare").asInt(),
                legs
        );
    }

    private TransitLeg parseLeg(JsonNode legNode) {
        String mode = legNode.path("mode").asString();
        LegPoint start = parsePoint(legNode.path("start"));
        LegPoint end = parsePoint(legNode.path("end"));
        int distance = legNode.path("distance").asInt();
        int sectionTime = legNode.path("sectionTime").asInt();

        if ("WALK".equals(mode)) {
            return new WalkLeg(mode, distance, sectionTime, start, end, parseSteps(legNode.path("steps")));
        }
        return new TransitRideLeg(
                mode,
                distance,
                sectionTime,
                start,
                end,
                legNode.path("route").asString(),
                legNode.path("routeId").asString(),
                legNode.path("routeColor").asString(),
                legNode.path("type").asInt(),
                legNode.path("service").asInt() == 1,
                parseStations(legNode.path("passStopList").path("stations")),
                legNode.path("passShape").path("linestring").asString(null)
        );
    }

    private LegPoint parsePoint(JsonNode pointNode) {
        return new LegPoint(pointNode.path("name").asString(), pointNode.path("lon").asDouble(), pointNode.path("lat").asDouble());
    }

    private List<WalkStep> parseSteps(JsonNode stepsNode) {
        List<WalkStep> steps = new ArrayList<>();
        for (JsonNode stepNode : stepsNode) {
            steps.add(new WalkStep(
                    stepNode.path("streetName").asString(),
                    stepNode.path("distance").asInt(),
                    stepNode.path("description").asString(),
                    stepNode.path("linestring").asString()
            ));
        }
        return steps;
    }

    // 정류장 lon/lat은 티맵이 문자열("127.103864")로 내려주지만 start/end(숫자)와 동일하게 asDouble()로 처리 가능
    private List<TransitStation> parseStations(JsonNode stationsNode) {
        List<TransitStation> stations = new ArrayList<>();
        for (JsonNode stationNode : stationsNode) {
            stations.add(new TransitStation(
                    stationNode.path("index").asInt(),
                    stationNode.path("stationName").asString(),
                    stationNode.path("stationID").asString(),
                    stationNode.path("lon").asDouble(),
                    stationNode.path("lat").asDouble()
            ));
        }
        return stations;
    }
}
