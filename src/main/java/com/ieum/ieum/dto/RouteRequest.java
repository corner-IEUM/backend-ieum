package com.ieum.ieum.dto;

import jakarta.validation.constraints.NotNull;

public record RouteRequest(
        @NotNull Double startLng,
        @NotNull Double startLat,
        @NotNull Double endLng,
        @NotNull Double endLat,
        Integer searchOption   // 티맵 경로 탐색 옵션 (0=추천, 30=계단 제외 등). 생략 시 0(추천)
) {}