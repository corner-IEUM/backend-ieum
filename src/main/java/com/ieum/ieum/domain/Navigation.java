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
}