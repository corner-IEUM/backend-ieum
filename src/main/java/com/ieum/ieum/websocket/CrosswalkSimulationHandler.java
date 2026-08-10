package com.ieum.ieum.websocket;

import com.ieum.ieum.domain.Navigation;
import com.ieum.ieum.dto.Crosswalk;
import com.ieum.ieum.dto.GuidanceMessage;
import com.ieum.ieum.repository.NavigationRepository;
import com.ieum.ieum.service.GuidanceService;
import com.ieum.ieum.service.RouteParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CrosswalkSimulationHandler extends TextWebSocketHandler {

    private static final double APPROACH_M  = 30.0;  // 접근 안내 거리(m)
    private static final double ARRIVE_M     = 8.0;   // 도착 안내 거리(m)
    // private static final long   INTERVAL_MS  = 250;   // 좌표 재생 간격(ms) — 데모 속도 조절
    private static final double WALK_SPEED_MPS = 1.4;  // 보행 속도(m/s) — 사람 평균
    private static final double TIME_SCALE = 0.8;     // 데모 배속 (10 = 실제보다 10배 빠름)

    private final NavigationRepository navigationRepository;
    private final RouteParser routeParser;
    private final ObjectMapper objectMapper;
    private final GuidanceService guidanceService;

    // 데모용 가상 장애물: 경로 진행률(0.0~1.0) 지점에 배치
//    private record DemoObstacle(double at, int priority, String message) {}
//
//    private static final List<DemoObstacle> DEMO_OBSTACLES = List.of(
//            new DemoObstacle(0.20, 3, "오른쪽에 자전거"),
//            new DemoObstacle(0.45, 1, "정면에 사람"),        // 근접 → 즉각 위험(p1)
//            new DemoObstacle(0.70, 3, "왼쪽에 볼라드"),
//            new DemoObstacle(0.88, 3, "오른쪽에 킥보드")
//    );

    public CrosswalkSimulationHandler(NavigationRepository navigationRepository,
                                      RouteParser routeParser,
                                      ObjectMapper objectMapper,
                                      GuidanceService guidanceService) {
        this.navigationRepository = navigationRepository;
        this.routeParser = routeParser;
        this.objectMapper = objectMapper;
        this.guidanceService = guidanceService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode req = objectMapper.readTree(message.getPayload());
        long navigationId = req.path("navigationId").asLong();

        Navigation nav = navigationRepository.findById(navigationId).orElse(null);
        if (nav == null) {
            send(session, Map.of("type", "error", "message", "존재하지 않는 길찾기 기록입니다."));
            return;
        }

        String raw = nav.getRouteData();
        List<double[]> path = routeParser.extractPath(raw);
        List<Crosswalk> crosswalks = routeParser.extractCrosswalks(raw);

        if (path.isEmpty()) {
            send(session, Map.of("type", "error", "message", "경로 좌표를 찾을 수 없습니다."));
            return;
        }

        // 재생은 별도 스레드에서 (핸들러는 즉시 반환)
        new Thread(() -> replay(session, path, crosswalks)).start();
    }

    private void replay(WebSocketSession session, List<double[]> path, List<Crosswalk> crosswalks) {
        try {
            // 1) 전체 경로 + 횡단보도 위치를 먼저 전송 (지도 그리기용)
            send(session, Map.of("type", "init", "path", path, "crosswalks", crosswalks));

            boolean[] approached = new boolean[crosswalks.size()];
            boolean[] arrived = new boolean[crosswalks.size()];
            // boolean[] obstacleFired = new boolean[DEMO_OBSTACLES.size()];

            // 2) 좌표를 순서대로 재생 (GPS 흉내)
            for (int i = 0; i < path.size(); i++) {
                if (!session.isOpen()) return;
                double[] cur = path.get(i);

                send(session, Map.of("type", "position",
                        "lng", cur[0], "lat", cur[1], "index", i, "total", path.size()));

                // 3) 각 횡단보도와의 거리 확인 → 접근/도착 안내
                for (int c = 0; c < crosswalks.size(); c++) {
                    Crosswalk cw = crosswalks.get(c);
                    double d = haversine(cur[1], cur[0], cw.lat(), cw.lng());

                    if (!approached[c] && d <= APPROACH_M) {
                        approached[c] = true;
                        send(session, Map.of("type", "guidance", "event", "approach",
                                "guidance", cw.guidance(), "distance", Math.round(d),
                                "message", "잠시 후 횡단보도입니다. " + cw.guidance()));
                        // ↓ 통합 허브에도 전송 (priority 3 = 일반 정보)
                        guidanceService.send(new com.ieum.ieum.dto.GuidanceMessage(
                                "crosswalk", 3, "잠시 후 횡단보도입니다. " + cw.guidance(),
                                java.util.Map.of("event", "approach", "distance", Math.round(d))));
                    }
                    if (!arrived[c] && d <= ARRIVE_M) {
                        arrived[c] = true;
                        send(session, Map.of("type", "guidance", "event", "arrive",
                                "guidance", cw.guidance(), "distance", Math.round(d),
                                "message", "횡단보도에 도착했습니다. 신호를 확인하세요."));
                        // ↓ 통합 허브에도 전송 (priority 2 = 결정적 순간)
                        guidanceService.send(new com.ieum.ieum.dto.GuidanceMessage(
                                "crosswalk", 2, "횡단보도에 도착했습니다. 신호를 확인하세요.",
                                java.util.Map.of("event", "arrive", "guidance", cw.guidance())));
                    }
                }

                // 데모 장애물: 현재 진행률이 배치 지점을 지나면 발생
//                double progress = (double) i / (path.size() - 1);
//                for (int o = 0; o < DEMO_OBSTACLES.size(); o++) {
//                    DemoObstacle ob = DEMO_OBSTACLES.get(o);
//                    if (!obstacleFired[o] && progress >= ob.at()) {
//                        obstacleFired[o] = true;
//                        guidanceService.send(new GuidanceMessage(
//                                "obstacle", ob.priority(), ob.message(),
//                                Map.of("demo", true, "at", ob.at())));
//                    }
//                }

                // 다음 좌표까지의 거리만큼, 보행 속도로 이동하는 시간을 계산해 대기
                if (i + 1 < path.size()) {
                    double[] next = path.get(i + 1);
                    double segment = haversine(cur[1], cur[0], next[1], next[0]);  // m
                    long waitMs = (long) (segment / WALK_SPEED_MPS / TIME_SCALE * 1000);
                    if (waitMs > 0) Thread.sleep(waitMs);
                }
            }
            send(session, Map.of("type", "done", "message", "목적지에 도착했습니다."));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // 연결 끊김 — 조용히 종료
        }
    }

    private void send(WebSocketSession session, Map<String, Object> payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    /** 두 좌표 사이 거리(m) — Haversine */
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}