package com.ieum.ieum.service;

import com.ieum.ieum.domain.CrosswalkFacility;
import com.ieum.ieum.dto.CrosswalkHazard;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosswalkAccessibilityServiceTest {

    // toHazard()는 리포지토리를 쓰지 않으므로 repository는 null로 둬도 된다.
    private final CrosswalkAccessibilityService service = new CrosswalkAccessibilityService(null);

    @Test
    void missingPedestrianSignal_isMostSevere() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .hasPedestrianSignal(false)
                .hasSoundSignal(false)
                .hasCurbCut(false)
                .hasBrailleBlock(false)
                .build();

        CrosswalkHazard hazard = service.toHazard(facility).orElseThrow();

        assertEquals(1, hazard.priority());
        assertTrue(hazard.message().contains("보행자 신호등"));
    }

    @Test
    void missingSoundSignal_winsOverCurbCutAndBrailleBlock() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .hasPedestrianSignal(true)
                .hasSoundSignal(false)
                .hasCurbCut(false)
                .hasBrailleBlock(false)
                .build();

        CrosswalkHazard hazard = service.toHazard(facility).orElseThrow();

        assertTrue(hazard.message().contains("음향 신호기"));
    }

    @Test
    void tightSignalTiming_isLowestPriority() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .hasPedestrianSignal(true)
                .hasSoundSignal(true)
                .hasCurbCut(true)
                .hasBrailleBlock(true)
                .crossingLengthM(20.0)
                .greenSignalSec(15.0)
                .build();

        CrosswalkHazard hazard = service.toHazard(facility).orElseThrow();

        assertEquals(3, hazard.priority());
        assertTrue(hazard.message().contains("고정된 특성"));
    }

    @Test
    void noIssue_returnsEmpty() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .hasPedestrianSignal(true)
                .hasSoundSignal(true)
                .hasCurbCut(true)
                .hasBrailleBlock(true)
                .build();

        Optional<CrosswalkHazard> hazard = service.toHazard(facility);

        assertTrue(hazard.isEmpty());
    }
}
