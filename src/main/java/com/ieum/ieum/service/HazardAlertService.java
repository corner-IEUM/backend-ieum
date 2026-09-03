package com.ieum.ieum.service;

import com.ieum.ieum.dto.GuidanceMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 실시간 위치 근처 위험요소를 조회해 안내로 내보낸다. 소스별로 실패가 서로 전파되지 않게 격리한다. */
@Slf4j
@Service
public class HazardAlertService {

    private final CrosswalkAccessibilityService crosswalkAccessibilityService;
    private final GuidanceService guidanceService;

    public HazardAlertService(CrosswalkAccessibilityService crosswalkAccessibilityService,
                               GuidanceService guidanceService) {
        this.crosswalkAccessibilityService = crosswalkAccessibilityService;
        this.guidanceService = guidanceService;
    }

    public void checkAndNotify(double lng, double lat) {
        try {
            crosswalkAccessibilityService.findMostSevereNearby(lng, lat)
                    .ifPresent(hazard -> guidanceService.send(new GuidanceMessage(
                            "crosswalk_accessibility", hazard.priority(), hazard.message(),
                            Map.of("lng", lng, "lat", lat))));
        } catch (Exception e) {
            log.warn("[횡단보도 접근성 조회 실패] {}", e.getMessage());
        }
        // YOLO 근처 조회는 좌표로 즉시 조회 가능한 소스가 아직 없어 이번 범위 밖 — 생기면 이 자리에 병렬로 추가
    }
}
