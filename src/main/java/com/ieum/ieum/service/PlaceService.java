package com.ieum.ieum.service;

import com.ieum.ieum.client.KakaoDirectionsClient;
import com.ieum.ieum.dto.PlaceSearchResponse;
import tools.jackson.databind.JsonNode;        // Jackson 3 (navigation에서 쓴 그 패키지)
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaceService {

    private final KakaoDirectionsClient kakaoClient;
    private final ObjectMapper objectMapper;

    public PlaceService(KakaoDirectionsClient kakaoClient, ObjectMapper objectMapper) {
        this.kakaoClient = kakaoClient;
        this.objectMapper = objectMapper;
    }

    public PlaceSearchResponse search(String query, Double lng, Double lat) {
        String raw = kakaoClient.searchKeyword(query, lng, lat, 5);  // 음성 UX 고려해 5개로 제한

        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("장소 검색 응답을 해석하지 못했습니다.", e);
        }

        List<PlaceSearchResponse.Place> places = new ArrayList<>();
        for (JsonNode doc : root.path("documents")) {
            String name = doc.path("place_name").asString();
            String road = doc.path("road_address_name").asString();
            if (road.isBlank()) {
                road = doc.path("address_name").asString();  // 도로명 없으면 지번주소로 대체
            }

            // ★ 카카오는 x/y/distance를 '문자열'로 줍니다 → 숫자로 변환 필요
            double x = Double.parseDouble(doc.path("x").asString());  // 경도
            double y = Double.parseDouble(doc.path("y").asString());  // 위도

            String distStr = doc.path("distance").asString();
            Integer distance = distStr.isBlank() ? null : Integer.valueOf(distStr);

            places.add(new PlaceSearchResponse.Place(name, road, x, y, distance));
        }

        return new PlaceSearchResponse(places);
    }
}