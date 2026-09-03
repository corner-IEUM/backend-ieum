package com.ieum.ieum.dto;

import java.util.List;

public record RouteResponse(
        Long navigationId,
        int distance,      // m
        int duration,      // s
        List<Crosswalk> crosswalks, // 경로상 횡단보도 지점 목록
        List<Stair> stairs, // 경로상 계단(지하보도/육교 포함) 지점 목록
        CrosswalkAccessibilitySummary accessibilitySummary, // 경로상 횡단보도의 접근성 문제 항목별 개수
        String routeData   // 티맵 원본 경로 JSON (앱에서 파싱해 단계별 안내에 사용)
) {}