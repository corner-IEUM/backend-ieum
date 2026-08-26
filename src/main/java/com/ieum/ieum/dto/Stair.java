package com.ieum.ieum.dto;

public record Stair(
        double lng,        // 경도(x)
        double lat,        // 위도(y)
        String guidance    // 안내문구 (예: "강남역 11번출구에서 지하보도 진입 후 54m 이동")
) {}
