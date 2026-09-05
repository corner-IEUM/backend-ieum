package com.ieum.ieum.dto;

import com.ieum.ieum.domain.Navigation;
import java.time.LocalDateTime;

// 길찾기(Navigation) 히스토리 조회 응답 — "내 경로" 탭에서 유저별 길찾기 기록을 최신순으로 보여줄 때 사용
// WalkLogResponse와 동일한 패턴(record + fromEntity)을 따름
public record NavigationHistoryResponse(
    Long id,
    Double startLng,
    Double startLat,
    Double endLng,
    Double endLat,
    Integer distance,   // m
    Integer duration,   // s
    Integer obstacleCnt,
    Boolean completed,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String routeData    // 티맵 원본 경로 JSON (앱에서 다시 단계별 안내를 그려볼 수 있도록 포함)
) {

    // Entity -> DTO 변환 메서드
    public static NavigationHistoryResponse fromEntity(Navigation nav) {
        return new NavigationHistoryResponse(
                nav.getId(),
                nav.getStartLng(),
                nav.getStartLat(),
                nav.getEndLng(),
                nav.getEndLat(),
                nav.getDistance(),
                nav.getDuration(),
                nav.getObstacleCnt(),
                nav.getCompleted(),
                nav.getStartTime(),
                nav.getEndTime(),
                nav.getRouteData()
        );
    }
}
