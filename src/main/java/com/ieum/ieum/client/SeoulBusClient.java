package com.ieum.ieum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 서울시 버스 정류소정보/도착정보 조회. 응답 JSON 문자열을 그대로 반환한다(파싱은 서비스에서). */
@Component
public class SeoulBusClient {

    private static final String BASE = "http://ws.bus.go.kr/api/rest/stationinfo";

    private final RestClient restClient;
    private final String arrivalKey;
    private final String stopKey;

    public SeoulBusClient(@Value("${seoul-bus.arrival-key}") String arrivalKey,
                          @Value("${seoul-bus.stop-key}") String stopKey) {
        this.arrivalKey = arrivalKey;
        this.stopKey = stopKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String searchStations(String stationName) {
        return get("/getStationByName", stopKey, "stSrch", stationName);
    }

    public String arrivalsByArsId(String arsId) {
        return get("/getStationByUid", arrivalKey, "arsId", arsId);
    }

    private String get(String path, String key, String paramName, String paramValue) {
        String url = BASE + path
                + "?serviceKey=" + enc(key)
                + "&" + paramName + "=" + enc(paramValue)
                + "&resultType=json";
        return restClient.get().uri(URI.create(url)).retrieve().body(String.class);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
