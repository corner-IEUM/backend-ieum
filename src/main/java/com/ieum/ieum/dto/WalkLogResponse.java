package com.ieum.ieum.dto;

import com.ieum.ieum.domain.WalkLog;
import java.time.LocalDateTime;

//record : 값만 담음 
//필요한 필드만 골라서 클라이언트에 전달 가능 

public record WalkLogResponse(
    Long id, // 보행 기록 고유 ID
    String routeData, // JSON 형태로 저장되는 경로 데이터
    Double totalDistance, // 보행에서 이동한 총 거리(단위 : 미터)
    Long totalTime, // 보행 걸린 총 시간(단위 : 초) 
    Integer obstacleCount, // 보행시 감지한 장애물 개수
    LocalDateTime createdAt // 보행 기록 시간(언제 저장되었는지, 최신순 정렬 기준)
){
    
    //Entity -> DTO 변환 메서드
    public static WalkLogResponse fromEntity(WalkLog walkLog){
        return new WalkLogResponse(
                walkLog.getId(),
                walkLog.getRouteData(),
                walkLog.getTotalDistance(),
                walkLog.getTotalTime(),
                walkLog.getObstacleCount(),
                walkLog.getCreatedAt()
        );
    }
}