package com.ieum.ieum.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

//대중교통 경로 후보 안의 구간(leg) 하나. 티맵 응답의 mode 필드로 WalkLeg/TransitRideLeg 중
// 하나로 확정되므로, 상태 머신(leg 단위 전환)이 switch 패턴으로 분기할 때 누락 없이 처리하도록 sealed로 제한한다.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "mode", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = WalkLeg.class, names = "WALK"),
        @JsonSubTypes.Type(value = TransitRideLeg.class, names = {"BUS", "SUBWAY"})
})

// 대중교통 경로 후보 안의 구간(leg) 하나. 티맵 응답의 mode 필드로 WalkLeg/TransitRideLeg 중
// 하나로 확정되므로, 상태 머신(leg 단위 전환)이 switch 패턴으로 분기할 때 누락 없이 처리하도록 sealed로 제한한다.
public sealed interface TransitLeg permits WalkLeg, TransitRideLeg {
    String mode();        // WALK, BUS, SUBWAY 등 — 티맵 원본 그대로. 프론트가 모든 leg를 mode로 균일하게 분기할 수 있도록 WalkLeg도 명시
    int distance();      // m
    int sectionTime();    // s
    LegPoint start();
    LegPoint end();
}
