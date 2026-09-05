package com.ieum.ieum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoDirectionsClient {

    private final RestClient restClient;

    public KakaoDirectionsClient(@Value("${kakao.rest-api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }

    public String searchKeyword(String query, Double lng, Double lat, int size) {
        return searchKeyword(query, lng, lat, size, null);
    }

    // categoryGroupCode: 지하철역(SW8)·관광명소(AT4) 등으로 좁혀서, 상호명 우연 일치가 아니라
    // 실제 지역/장소 랜드마크를 우선 찾을 때 사용 (null이면 카테고리 제한 없는 일반 키워드 검색)
    public String searchKeyword(String query, Double lng, Double lat, int size, String categoryGroupCode) {
        return restClient.get()
                .uri(uri -> {
                    uri.path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("size", size);
                    if (categoryGroupCode != null) {
                        uri.queryParam("category_group_code", categoryGroupCode);
                    }
                    if (lng != null && lat != null) {
                        // x,y만 주면 카카오는 distance 필드를 계산해 내려주면서도 정렬은 기본값인
                        // accuracy(정확도)를 그대로 쓴다 — 실측 결과 이 조합이 "카페" 같은 일반
                        // 키워드는 현재 위치 근처를 자연스럽게 우선하면서도, "동대문역"·"군자" 같은
                        // 지역명/랜드마크 검색에서는 실제 그 장소(수백 km 밖이어도)를 정확히 찾아준다.
                        // 반대로 sort=distance를 강제하면 정확도와 무관하게 "현재 위치에서 가장 가까운"
                        // 결과가 우선되어, 사용자가 멀리 있는 목적지를 이름으로 찾을 때 엉뚱한 동명이인
                        // 상호명(예: "동대문엽기떡볶이" 지방 지점)이 진짜 목적지보다 앞에 나오는 문제가 있었다.
                        uri.queryParam("x", lng)   // x = 경도(lng)
                                .queryParam("y", lat);  // y = 위도(lat)
                    }
                    return uri.build();
                })
                .retrieve()
                .body(String.class);
    }
}