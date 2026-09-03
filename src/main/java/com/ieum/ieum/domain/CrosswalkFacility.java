package com.ieum.ieum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 전국횡단보도표준데이터. location/raw_data는 조회 쿼리(ST_DWithin)에서만 쓰고
 * 여기서는 매핑하지 않는다 (ddl-auto=validate 환경이라 매핑 안 된 컬럼은 검증 대상이 아님).
 */
@Entity
@Table(name = "crosswalks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrosswalkFacility {

    private static final double REQUIRED_WALK_SPEED_MPS = 1.0; // 논의 필요 값, 상수로 분리

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "management_no")
    private String managementNo;

    @Column(name = "has_curb_cut")
    private Boolean hasCurbCut;

    @Column(name = "has_braille_block")
    private Boolean hasBrailleBlock;

    @Column(name = "has_sound_signal")
    private Boolean hasSoundSignal;

    @Column(name = "has_pedestrian_signal")
    private Boolean hasPedestrianSignal;

    @Column(name = "width_m")
    private Double widthM;

    @Column(name = "raised_crosswalk")
    private Boolean raisedCrosswalk;

    @Column(name = "crossing_length_m")
    private Double crossingLengthM;

    @Column(name = "green_signal_sec")
    private Double greenSignalSec;

    @Column(name = "source_updated_at")
    private LocalDate sourceUpdatedAt;

    @Builder
    public CrosswalkFacility(String managementNo, Boolean hasCurbCut, Boolean hasBrailleBlock,
                              Boolean hasSoundSignal, Boolean hasPedestrianSignal, Double widthM,
                              Boolean raisedCrosswalk, Double crossingLengthM, Double greenSignalSec,
                              LocalDate sourceUpdatedAt) {
        this.managementNo = managementNo;
        this.hasCurbCut = hasCurbCut;
        this.hasBrailleBlock = hasBrailleBlock;
        this.hasSoundSignal = hasSoundSignal;
        this.hasPedestrianSignal = hasPedestrianSignal;
        this.widthM = widthM;
        this.raisedCrosswalk = raisedCrosswalk;
        this.crossingLengthM = crossingLengthM;
        this.greenSignalSec = greenSignalSec;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public boolean hasCurbCutIssue() {
        return Boolean.FALSE.equals(hasCurbCut) && !Boolean.TRUE.equals(raisedCrosswalk);
    }

    public boolean hasBrailleBlockIssue() {
        return Boolean.FALSE.equals(hasBrailleBlock);
    }

    public boolean hasSoundSignalIssue() {
        return Boolean.FALSE.equals(hasSoundSignal);
    }

    public boolean hasPedestrianSignalIssue() {
        return Boolean.FALSE.equals(hasPedestrianSignal);
    }

    /** 실시간 신호 잔여시간이 아니라, 이 지점의 고정된 설계 특성이다. */
    public boolean hasTightSignalTiming() {
        if (crossingLengthM == null || greenSignalSec == null || greenSignalSec <= 0) {
            return false;
        }
        return crossingLengthM / greenSignalSec > REQUIRED_WALK_SPEED_MPS;
    }
}
