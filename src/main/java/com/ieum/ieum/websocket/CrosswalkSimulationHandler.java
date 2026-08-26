package com.ieum.ieum.websocket;

import com.ieum.ieum.domain.Navigation;
import com.ieum.ieum.dto.GuidanceMessage;
import com.ieum.ieum.dto.Waypoint;
import com.ieum.ieum.dto.WaypointCategory;
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

    private static final double CROSSWALK_APPROACH_M = 30.0; // 횡단보도 접근 안내 거리(m)
    private static final double CROSSWALK_ARRIVE_M    = 8.0;  // 횡단보도 도착 안내 거리(m)
    private static final double STAIR_APPROACH_M = 40.0; // 계단 접근 안내 거리(m) — 위험도가 높아 더 여유있게
    private static final double STAIR_ARRIVE_M   = 8.0;  // 계단 도착 안내 거리(m)
    private static final double TURN_ANNOUNCE_M  = 15.0; // 일반 회전/직진 안내 거리(m) — 1회만 안내
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
        List<Waypoint> waypoints = routeParser.extractWaypoints(raw);

        if (path.isEmpty()) {
            send(session, Map.of("type", "error", "message", "경로 좌표를 찾을 수 없습니다."));
            return;
        }

        // 재생은 별도 스레드에서 (핸들러는 즉시 반환)
        new Thread(() -> replay(session, path, waypoints)).start();
    }

    private void replay(WebSocketSession session, List<double[]> path, List<Waypoint> waypoints) {
        try {
            // 1) 전체 경로 + 횡단보도/계단 위치를 먼저 전송 (지도 그리기용, 일반 회전 지점은 마커로 그리지 않음)
            List<Waypoint> crosswalks = waypoints.stream().filter(w -> w.category() == WaypointCategory.CROSSWALK).toList();
            List<Waypoint> stairs = waypoints.stream().filter(w -> w.category() == WaypointCategory.STAIR).toList();
            send(session, Map.of("type", "init", "path", path, "crosswalks", crosswalks, "stairs", stairs));

            // 지점별 접근/도착 안내 여부 (일반 회전은 1회만 안내하므로 approached만 사용)
            boolean[] approached = new boolean[waypoints.size()];
            boolean[] arrived = new boolean[waypoints.size()];
            // boolean[] obstacleFired = new boolean[DEMO_OBSTACLES.size()];

            // 2) 좌표를 순서대로 재생 (GPS 흉내)
            for (int i = 0; i < path.size(); i++) {
                if (!session.isOpen()) return;
                double[] cur = path.get(i);

                send(session, Map.of("type", "position",
                        "lng", cur[0], "lat", cur[1], "index", i, "total", path.size()));

                // 3) 각 안내 지점과의 거리 확인 → 카테고리별로 접근/도착(또는 1회) 안내
                for (int w = 0; w < waypoints.size(); w++) {
                    Waypoint wp = waypoints.get(w);
                    double d = haversine(cur[1], cur[0], wp.lat(), wp.lng());
                    announce(session, wp, w, d, approached, arrived);
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

    private void announce(WebSocketSession session, Waypoint wp, int index, double distance,
                           boolean[] approached, boolean[] arrived) throws IOException {
        switch (wp.category()) {
            case STAIR -> {
                if (!approached[index] && distance <= STAIR_APPROACH_M) {
                    approached[index] = true;
                    emit(session, "stair", "approach", wp.guidance(), distance,
                            "잠시 후 계단입니다. " + wp.guidance(), 3);
                }
                if (!arrived[index] && distance <= STAIR_ARRIVE_M) {
                    arrived[index] = true;
                    emit(session, "stair", "arrive", wp.guidance(), distance,
                            "계단입니다. 손잡이와 발밑을 확인하세요.", 2);
                }
            }
            case CROSSWALK -> {
                if (!approached[index] && distance <= CROSSWALK_APPROACH_M) {
                    approached[index] = true;
                    emit(session, "crosswalk", "approach", wp.guidance(), distance,
                            "잠시 후 횡단보도입니다. " + wp.guidance(), 3);
                }
                if (!arrived[index] && distance <= CROSSWALK_ARRIVE_M) {
                    arrived[index] = true;
                    emit(session, "crosswalk", "arrive", wp.guidance(), distance,
                            "횡단보도에 도착했습니다. 신호를 확인하세요.", 2);
                }
            }
            case TURN -> {
                if (!approached[index] && distance <= TURN_ANNOUNCE_M) {
                    approached[index] = true;
                    emit(session, "turn", "guide", wp.guidance(), distance, wp.guidance(), 3);
                }
            }
        }
    }

    /** 시뮬레이션 소켓과 통합 안내 허브(GuidanceService) 양쪽으로 안내를 내보낸다. */
    private void emit(WebSocketSession session, String kind, String event, String guidance,
                       double distance, String message, int priority) throws IOException {
        send(session, Map.of("type", "guidance", "kind", kind, "event", event,
                "guidance", guidance, "distance", Math.round(distance), "message", message));
        guidanceService.send(new GuidanceMessage(kind, priority, message,
                Map.of("event", event, "guidance", guidance, "distance", Math.round(distance))));
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
