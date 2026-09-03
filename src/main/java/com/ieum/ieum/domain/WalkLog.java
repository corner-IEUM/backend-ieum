package com.ieum.ieum.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// 앱 실행시 DB에 자동적으로 테이블 만듦

@Entity
@Getter
@Table(name = "walk_log") // 테이블 이름 = walk_log
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class WalkLog{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본키 생성을 DB에 위임

    private Long id;

    //로그인 토큰에서 추출한 userId를 저장하는 컬럼
    @Column(name="user_id", nullable = false)
    private Long userId;

    //JSON 형태로 저장되는 경로 데이터, 길이 무제한
    //Navigation.routeData와 동일하게 String을 jsonb로 저장 (@Lob은 Postgres에서 oid로 매핑되어
    //autocommit 모드에서 조회가 안 되는 문제가 있어 사용하지 않음)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="route_data", nullable = false)
    private String routeData;

    //보행에서 이동한 총 거리(단위 : 미터)
    //프론트에서 좌표 간 거리를 다 더해서 계산->여기로 전달
    @Column(name="total_distance",nullable=false)
    private double totalDistance;

    //보행 걸린 총 시간(단위 : 초)
    @Column(name="total_time",nullable=false)
    private long totalTime;

    //보행시 감지한 장애물 개수
    @Column(name="obstacle_count",nullable=false)
    private int obstacleCount;

    //보행 기록 시간(언제 저장되었는지,최신순 정렬 기준)
    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt;

    @Builder
    public WalkLog(Long userId, String routeData, double totalDistance, long totalTime, int obstacleCount) {
        this.userId = userId;
        this.routeData = routeData;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.obstacleCount = obstacleCount;
    }

    //엔티티가 DB에 저장(insert)되기 직전에 자동 호출되어 저장 시점의 현재 시각을 기록
    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}