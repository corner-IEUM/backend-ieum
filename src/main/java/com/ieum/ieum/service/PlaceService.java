package com.ieum.ieum.service;

import com.ieum.ieum.client.KakaoDirectionsClient;
import com.ieum.ieum.dto.PlaceSearchResponse;
import tools.jackson.databind.JsonNode;        // Jackson 3 (navigation에서 쓴 그 패키지)
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    private static final int RESULT_LIMIT = 5;  // 음성 UX 고려해 5개로 제한

    // 카카오 키워드 검색은 place_name'뿐 아니라 주소 텍스트까지 매칭 대상이라, "동대문" 같은
    // 지역명으로 검색하면 그 동네에 있는 아무 상호명(예: "동대문곱창")이 실제 랜드마크(동대문역 등)보다
    // 먼저 나올 수 있다. 지하철역(SW8)·관광명소(AT4) 카테고리로 먼저 찾아서 상단에 배치하면,
    // "지역/장소 이름"으로 검색했을 때 실제 그 장소 자체가 상호명 우연 일치보다 우선하게 된다.
    private static final List<String> LANDMARK_CATEGORY_CODES = List.of("SW8", "AT4");
    private static final int LANDMARK_RESULTS_PER_CATEGORY = 2;

    private final KakaoDirectionsClient kakaoClient;
    private final ObjectMapper objectMapper;

    public PlaceService(KakaoDirectionsClient kakaoClient, ObjectMapper objectMapper) {
        this.kakaoClient = kakaoClient;
        this.objectMapper = objectMapper;
    }

    public PlaceSearchResponse search(String query, Double lng, Double lat) {
        // 후보를 넉넉히 모아둔다: 카테고리 우선 검색(랜드마크) 결과를 먼저, 그다음 일반 검색 결과를
        // 이어붙이고 최종 5개로 자른다. 아래 최종 정리 단계에서 거리로 재정렬하지 않는 이유는
        // 그 아래 주석 참고.
        List<PlaceSearchResponse.Place> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>(); // "x,y"로 중복 제거 — 같은 장소가 카테고리 검색과 일반 검색에 둘 다 걸릴 수 있음

        for (String categoryCode : LANDMARK_CATEGORY_CODES) {
            try {
                String raw = kakaoClient.searchKeyword(query, lng, lat, LANDMARK_RESULTS_PER_CATEGORY, categoryCode);
                appendPlaces(raw, candidates, seen);
            } catch (Exception e) {
                // 랜드마크 우선 검색은 보조 수단 — 실패해도 아래 일반 검색으로 계속 진행한다
                log.warn("[장소검색] 카테고리({}) 우선 검색 실패, 일반 검색으로 계속 진행: {}", categoryCode, e.toString());
            }
        }

        try {
            String raw = kakaoClient.searchKeyword(query, lng, lat, RESULT_LIMIT, null);
            appendPlaces(raw, candidates, seen);
        } catch (Exception e) {
            log.error("[장소검색] 카카오 키워드 검색 실패 — query='{}', lng={}, lat={}: {}", query, lng, lat, e.toString());
            throw new IllegalStateException("장소 검색에 실패했습니다.", e);
        }

        // ★ 여기서 거리순으로 다시 정렬하면 안 된다 — 카테고리 우선 검색(SW8·AT4)으로 찾아낸
        // 진짜 랜드마크(예: "군자역", "동대문역")가 현재 위치에서 아무리 멀어도(수백 km) 정확히
        // 그 이름으로 찾아낸 결과인데, 거리로 재정렬하면 우연히 더 가까운 무관한 상호명(동명이인)에
        // 밀려 최종 5개에서 잘려나간다 (실측: 제주 기준 "동대문역" 검색 시 진짜 동대문역은 453km라
        // 정렬 후 순위 밖으로 밀리고, 267~310km 거리의 "동대문엽기떡볶이" 지방 지점들만 남았음).
        // 카테고리 우선 → 일반 검색 순서(둘 다 카카오 accuracy 정렬 결과) 그대로 두고,
        // distance는 화면에 "몇 km"로 보여주기 위한 표시용 정보로만 쓴다.
        List<PlaceSearchResponse.Place> places = candidates.size() > RESULT_LIMIT
                ? candidates.subList(0, RESULT_LIMIT)
                : candidates;
        return new PlaceSearchResponse(places);
    }

    private void appendPlaces(String raw, List<PlaceSearchResponse.Place> out, Set<String> seen) {
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("장소 검색 응답을 해석하지 못했습니다.", e);
        }

        for (JsonNode doc : root.path("documents")) {
            String name = doc.path("place_name").asString();
            String road = doc.path("road_address_name").asString();
            if (road.isBlank()) {
                road = doc.path("address_name").asString();  // 도로명 없으면 지번주소로 대체
            }

            // ★ 카카오는 x/y/distance를 '문자열'로 줍니다 → 숫자로 변환 필요
            double x = Double.parseDouble(doc.path("x").asString());  // 경도
            double y = Double.parseDouble(doc.path("y").asString());  // 위도

            String key = x + "," + y;
            if (!seen.add(key)) continue;  // 이미 추가된 장소(카테고리 우선 검색과 중복)면 건너뜀

            String distStr = doc.path("distance").asString();
            Integer distance = distStr.isBlank() ? null : Integer.valueOf(distStr);

            out.add(new PlaceSearchResponse.Place(name, road, x, y, distance));
        }
    }
}
