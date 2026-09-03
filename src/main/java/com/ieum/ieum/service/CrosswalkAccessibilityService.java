package com.ieum.ieum.service;

import com.ieum.ieum.domain.CrosswalkFacility;
import com.ieum.ieum.dto.Crosswalk;
import com.ieum.ieum.dto.CrosswalkAccessibilitySummary;
import com.ieum.ieum.dto.CrosswalkHazard;
import com.ieum.ieum.repository.CrosswalkFacilityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** 좌표(들) 근처의 접근성 문제가 있는 횡단보도 조회 — 경로 요약(A)과 실시간 알림(B)이 공유 */
@Service
public class CrosswalkAccessibilityService {

    private static final double ROUTE_MATCH_RADIUS_M = 15.0;
    private static final double LIVE_MATCH_RADIUS_M = 30.0;

    private final CrosswalkFacilityRepository repository;

    public CrosswalkAccessibilityService(CrosswalkFacilityRepository repository) {
        this.repository = repository;
    }

    /** 경로상 Tmap 횡단보도 지점들 각각에 대해 가까운 실제 횡단보도의 접근성 이슈를 항목별로 집계 */
    public CrosswalkAccessibilitySummary summarizeForRoute(List<Crosswalk> tmapCrosswalks) {
        int noCurbCut = 0, noBrailleBlock = 0, noSoundSignal = 0, noPedestrianSignal = 0;

        for (Crosswalk cw : tmapCrosswalks) {
            CrosswalkFacility facility = repository.findNearest(cw.lng(), cw.lat(), ROUTE_MATCH_RADIUS_M)
                    .orElse(null);
            if (facility == null) continue;

            if (facility.hasCurbCutIssue()) noCurbCut++;
            if (facility.hasBrailleBlockIssue()) noBrailleBlock++;
            if (facility.hasSoundSignalIssue()) noSoundSignal++;
            if (facility.hasPedestrianSignalIssue()) noPedestrianSignal++;
        }

        return new CrosswalkAccessibilitySummary(noCurbCut, noBrailleBlock, noSoundSignal, noPedestrianSignal);
    }

    /** 현재 위치 근처 가장 심각한 접근성 문제 1건 (음성 안내는 한 번에 하나만 가능하므로) */
    public Optional<CrosswalkHazard> findMostSevereNearby(double lng, double lat) {
        return repository.findNearest(lng, lat, LIVE_MATCH_RADIUS_M)
                .flatMap(this::toHazard);
    }

    Optional<CrosswalkHazard> toHazard(CrosswalkFacility facility) {
        if (facility.hasPedestrianSignalIssue()) {
            return Optional.of(new CrosswalkHazard(1, "이 횡단보도는 보행자 신호등이 없습니다. 차량 흐름을 직접 확인하세요."));
        }
        if (facility.hasSoundSignalIssue()) {
            return Optional.of(new CrosswalkHazard(2, "이 횡단보도는 음향 신호기가 없습니다."));
        }
        if (facility.hasCurbCutIssue()) {
            return Optional.of(new CrosswalkHazard(2, "이 횡단보도는 보도턱이 낮춰져 있지 않습니다."));
        }
        if (facility.hasBrailleBlockIssue()) {
            return Optional.of(new CrosswalkHazard(3, "이 횡단보도는 점자블록이 없습니다."));
        }
        if (facility.hasTightSignalTiming()) {
            return Optional.of(new CrosswalkHazard(3, "이 횡단보도는 신호 시간이 짧게 설계되어 있습니다. (실시간 잔여 시간이 아닌 이 지점의 고정된 특성입니다)"));
        }
        return Optional.empty();
    }
}
