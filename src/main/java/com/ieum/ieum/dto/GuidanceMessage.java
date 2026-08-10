package com.ieum.ieum.dto;

import java.util.Map;

public record GuidanceMessage(
        String kind,               // "obstacle" | "crosswalk"
        int priority,              // 1=즉각위험, 2=결정적순간, 3=일반정보
        String message,            // 앱이 그대로 읽을 문구
        Map<String, Object> meta   // 소스별 부가정보 (방향/근접도/거리 등)
) {}