package com.ieum.ieum.dto;

public record Crosswalk(
        double lng,        // 경도(x)
        double lat,        // 위도(y)
        String guidance    // 안내문구 (예: "판교역사거리에서 횡단보도 이용")
) {}