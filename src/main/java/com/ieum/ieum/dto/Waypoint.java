package com.ieum.ieum.dto;

public record Waypoint(
        double lng,             // 경도(x)
        double lat,             // 위도(y)
        String guidance,        // 티맵이 준 안내문구 그대로
        WaypointCategory category
) {}
