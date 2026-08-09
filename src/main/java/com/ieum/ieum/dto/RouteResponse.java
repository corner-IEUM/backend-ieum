package com.ieum.ieum.dto;

public record RouteResponse(
        Long navigationId,
        int distance,      // m
        int duration,      // s
        String routeData   // 카카오 원본 경로 JSON (앱에서 파싱해 단계별 안내에 사용)
) {}