package com.ieum.ieum.service;

import com.ieum.ieum.client.SeoulBusClient;
import com.ieum.ieum.dto.ArrivalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TransitArrivalService {

    private static final double MAX_MATCH_DISTANCE_M = 50;   // 이보다 먼 정류소는 같은 곳으로 보지 않음

    private final SeoulBusClient seoulBusClient;
    private final ObjectMapper objectMapper;

    public TransitArrivalService(SeoulBusClient seoulBusClient, ObjectMapper objectMapper) {
        this.seoulBusClient = seoulBusClient;
        this.objectMapper = objectMapper;
    }

    /** 정류소명+좌표로 서울시 정류소를 찾고, 해당 노선의 도착 예정 정보를 반환한다. routeName이 비면 전체 노선. */
    public List<ArrivalResponse> getArrivals(String stopName, double lat, double lng, String routeName) {
        try {
            String arsId = findNearestArsId(stopName, lat, lng);
            log.info("[도착정보] 매칭된 arsId={}", arsId);
            return parseArrivals(seoulBusClient.arrivalsByArsId(arsId), routeName);
        } catch (ArrivalUnavailableException e) {
            log.warn("[도착정보 실패] {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("[도착정보 조회 실패]", e);
            throw new ArrivalUnavailableException("버스 도착 정보를 불러올 수 없어요", e);
        }
    }

    private String findNearestArsId(String stopName, double lat, double lng) {
        String raw = seoulBusClient.searchStations(stopName);
        JsonNode root = objectMapper.readTree(raw);
        String bestArs = null;
        double bestDist = Double.MAX_VALUE;
        for (JsonNode st : items(root)) {
            double d = distanceM(lat, lng,
                    Double.parseDouble(st.path("tmY").asString()),
                    Double.parseDouble(st.path("tmX").asString()));
            if (d < bestDist) {
                bestDist = d;
                bestArs = st.path("arsId").asString();
            }
        }
        log.info("[도착정보] 정류소 검색 결과 최근접 arsId={}, 거리={}m", bestArs, bestDist);
        if (bestArs == null || bestDist > MAX_MATCH_DISTANCE_M) {
            // 반대 방향 정류소를 안내하면 위험하므로 애매하면 실패 처리
            throw new ArrivalUnavailableException("일치하는 정류소를 찾지 못했어요");
        }
        return bestArs;
    }

    private List<ArrivalResponse> parseArrivals(String raw, String routeName) {
        JsonNode root = objectMapper.readTree(raw);
        String code = root.path("msgHeader").path("headerCd").asString();
        if ("4".equals(code)) return List.of();                 // 결과 없음
        if (!"0".equals(code)) throw new ArrivalUnavailableException("도착정보 응답 오류: " + code);

        String wanted = normalizeRoute(routeName);
        List<ArrivalResponse> result = new ArrayList<>();
        for (JsonNode it : items(root)) {
            String rt = it.path("rtNm").asString();
            if (!wanted.isEmpty() && !wanted.equals(normalizeRoute(rt))) continue;

            String msg1 = it.path("arrmsg1").asString();
            int t1 = toInt(it.path("traTime1").asString());
            boolean arriving = "1".equals(it.path("isArrive1").asString());
            if (msg1.contains("운행종료") || (t1 <= 0 && !arriving)) continue;   // 안 오는 버스를 기다리게 하지 않음

            int t2 = toInt(it.path("traTime2").asString());
            result.add(new ArrivalResponse(rt, Math.max(t1, 0), t2 > 0 ? t2 : null));
        }
        return result;
    }

    /** msgBody.itemList가 배열이든 단일 객체든 리스트로 */
    private static List<JsonNode> items(JsonNode root) {
        JsonNode list = root.path("msgBody").path("itemList");
        List<JsonNode> out = new ArrayList<>();
        if (list.isArray()) list.forEach(out::add);
        else if (list.isObject()) out.add(list);
        return out;
    }

    /** "간선:8146" → "8146" */
    private static String normalizeRoute(String r) {
        if (r == null) return "";
        int i = r.indexOf(':');
        return (i >= 0 ? r.substring(i + 1) : r).trim();
    }

    private static int toInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static double distanceM(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}