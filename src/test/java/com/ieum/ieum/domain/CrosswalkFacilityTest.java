package com.ieum.ieum.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosswalkFacilityTest {

    @Test
    void curbCutIssue_whenNotLoweredAndNotRaised() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .hasCurbCut(false)
                .raisedCrosswalk(false)
                .build();

        assertTrue(facility.hasCurbCutIssue());
    }

    @Test
    void noCurbCutIssue_whenRaisedCrosswalkCompensates() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .hasCurbCut(false)
                .raisedCrosswalk(true)
                .build();

        assertFalse(facility.hasCurbCutIssue());
    }

    @Test
    void noCurbCutIssue_whenAlreadyLowered() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .hasCurbCut(true)
                .raisedCrosswalk(false)
                .build();

        assertFalse(facility.hasCurbCutIssue());
    }

    @Test
    void tightSignalTiming_whenRequiredSpeedExceedsThreshold() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .crossingLengthM(20.0)
                .greenSignalSec(15.0) // 요구 속도 ~1.33 m/s > 1.0 m/s
                .build();

        assertTrue(facility.hasTightSignalTiming());
    }

    @Test
    void noTightSignalTiming_whenWithinThreshold() {
        CrosswalkFacility facility = CrosswalkFacility.builder()
                .crossingLengthM(10.0)
                .greenSignalSec(15.0) // 요구 속도 ~0.67 m/s <= 1.0 m/s
                .build();

        assertFalse(facility.hasTightSignalTiming());
    }

    @Test
    void noTightSignalTiming_whenDataMissing() {
        CrosswalkFacility facility = CrosswalkFacility.builder().build();

        assertFalse(facility.hasTightSignalTiming());
    }
}
