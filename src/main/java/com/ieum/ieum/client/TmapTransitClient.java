package com.ieum.ieum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 티맵 대중교통 길찾기(POST /transit/routes). 기존 TmapDirectionsClient(보행자 경로)와 같은
 * appKey를 쓰지만, 포털에서 "대중교통" 상품을 별도로 신청해야 호출 가능하다 (403 INVALID_API_KEY 참고).
 */
@Component
public class TmapTransitClient {

    private final RestClient restClient;

    public TmapTransitClient(@Value("${tmap.app-key}") String appKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://apis.openapi.sk.com")
                .defaultHeader("appKey", appKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String search(double startLng, double startLat, double endLng, double endLat) {
        return restClient.post()
                .uri("/transit/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "startX", String.valueOf(startLng),
                        "startY", String.valueOf(startLat),
                        "endX", String.valueOf(endLng),
                        "endY", String.valueOf(endLat)))
                .retrieve()
                .body(String.class);
    }
}
