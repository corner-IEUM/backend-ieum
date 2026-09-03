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
        return restClient.get()
                .uri(uri -> {
                    uri.path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("size", size);
                    if (lng != null && lat != null) {
                        uri.queryParam("x", lng)          // x = 경도(lng)
                                .queryParam("y", lat)          // y = 위도(lat)
                                .queryParam("sort", "distance"); // 현재 위치서 가까운 순
                    }
                    return uri.build();
                })
                .retrieve()
                .body(String.class);
    }
}