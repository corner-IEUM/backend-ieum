package com.ieum.ieum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class TmapDirectionsClient {

    private final RestClient restClient;

    public TmapDirectionsClient(@Value("${tmap.app-key}") String appKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://apis.openapi.sk.com")
                .defaultHeader("appKey", appKey)
                .build();
    }

    public String walk(double startLng, double startLat, double endLng, double endLat) {
        return restClient.post()
                .uri(uri -> uri.path("/tmap/routes/pedestrian")
                        .queryParam("version", 1)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "startX", String.valueOf(startLng),   // x = 경도(lng)
                        "startY", String.valueOf(startLat),   // y = 위도(lat)
                        "endX", String.valueOf(endLng),
                        "endY", String.valueOf(endLat),
                        "startName", "출발지",
                        "endName", "도착지",
                        "reqCoordType", "WGS84GEO",
                        "resCoordType", "WGS84GEO"))
                .retrieve()
                .body(String.class);
    }
}
