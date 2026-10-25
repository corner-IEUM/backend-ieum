package com.ieum.ieum.dto;

import java.util.List;

/** 도보 구간(mode=WALK) — 정류장까지/하차 후 목적지까지 이동 시 기존 도보 안내 로직이 그대로 쓰는 leg */
public record WalkLeg(
        String mode,
        int distance,
        int sectionTime,
        LegPoint start,
        LegPoint end,
        List<WalkStep> steps
) implements TransitLeg {}
