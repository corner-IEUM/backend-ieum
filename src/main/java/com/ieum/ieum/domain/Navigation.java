package com.ieum.ieum.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "navigation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Navigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_lng", nullable = false)
    private Double startLng;

    @Column(name = "start_lat", nullable = false)
    private Double startLat;

    @Column(name = "end_lng", nullable = false)
    private Double endLng;

    @Column(name = "end_lat", nullable = false)
    private Double endLat;

    @JdbcTypeCode(SqlTypes.JSON)              // String을 jsonb로 저장
    @Column(name = "route_data", nullable = false)
    private String routeData;

    @Column(nullable = false)
    private Integer distance;

    @Column(nullable = false)
    private Integer duration;

    @CreationTimestamp
    @Column(name = "start_time", nullable = false, updatable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "obstacle_cnt", nullable = false)
    private Integer obstacleCnt = 0;

    @Column(nullable = false)
    private Boolean completed = false;

    // 경로 종류 — 새로 생성되는 경로는 기본적으로 도보 전용. 대중교통 경로는 startTransit()으로 전환
    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false, length = 20)
    private RouteType routeType = RouteType.WALK_ONLY;

    // leg(구간) 목록 — TRANSIT 경로에서만 값이 채워짐. 구조는 RouteParser처럼 서비스 레이어에서 파싱
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transit_legs")
    private String transitLegs;

    // transitLegs 배열에서 현재 진행 중인 구간 인덱스 — TRANSIT 경로에서만 사용
    @Column(name = "current_leg_index")
    private Integer currentLegIndex;

    // 현재 구간이 진행 중인 단계 — TRANSIT 경로에서만 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "current_leg_state", length = 20)
    private LegState currentLegState;

    @Builder
    public Navigation(Long userId, Double startLng, Double startLat, Double endLng, Double endLat,
                      String routeData, Integer distance, Integer duration) {
        this.userId = userId;
        this.startLng = startLng;
        this.startLat = startLat;
        this.endLng = endLng;
        this.endLat = endLat;
        this.routeData = routeData;
        this.distance = distance;
        this.duration = duration;
    }

    // 보행 종료 시 호출 (2단계)
    public void complete(int obstacleCnt, boolean completed) {
        this.obstacleCnt = obstacleCnt;
        this.completed = completed;
        this.endTime = LocalDateTime.now();
    }

    // 대중교통 경로로 확정 시 호출 — leg 목록을 저장하고 첫 구간(정류장행 도보)부터 시작
    public void startTransit(String transitLegs) {
        this.routeType = RouteType.TRANSIT;
        this.transitLegs = transitLegs;
        this.currentLegIndex = 0;
        this.currentLegState = LegState.WALK_TO_STOP;
    }

    // 사용자의 탑승/하차 확인 등으로 다음 구간·단계로 전환할 때 호출 (PATCH /api/navigation/{id}/leg-state)
    public void advanceLeg(int legIndex, LegState state) {
        this.currentLegIndex = legIndex;
        this.currentLegState = state;
    }
}