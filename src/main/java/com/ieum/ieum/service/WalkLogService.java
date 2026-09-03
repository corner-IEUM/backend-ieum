package com.ieum.ieum.service;

import com.ieum.ieum.domain.WalkLog;
import com.ieum.ieum.dto.WalkCompleteRequest;
import com.ieum.ieum.dto.WalkLogResponse;
import com.ieum.ieum.repository.WalkLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class WalkLogService{
    // Repository를 주입받아 DB에 접근하는 서비스 클래스
    private final WalkLogRepository walkLogRepository;

    // 보행 완료시 호출되는 메서드
    // Entity를 생성하고 DB에 저장
    public WalkLogResponse saveWalkLog(Long userId, WalkCompleteRequest request){

        //DTO->Entity 변환 
        WalkLog walkLog = WalkLog.builder()
                .userId(userId)
                .routeData(request.routeData())
                .totalDistance(request.totalDistance())
                .totalTime(request.totalTime())
                .obstacleCount(request.obstacleCount())
                .build();
        //실제 DB 에 저장
        WalkLog savedWalkLog = walkLogRepository.save(walkLog);

        //Entity->DTO 변환
        return WalkLogResponse.fromEntity(savedWalkLog);


    }
    // " 내 경로" 탭에서 유저별 보행 기록을 최신순으로 조회 가능
    public List<WalkLogResponse> getWalkLogsByUserId(Long userId){
        //DB에서 userId를 기준으로 보행 기록을 조회
        List<WalkLog> walkLogs = walkLogRepository.findByUserIdOrderByCreatedAtDesc(userId);

        //Entity->DTO 변환
        return walkLogs.stream()
                .map(WalkLogResponse::fromEntity)
                .toList();
    }
}
