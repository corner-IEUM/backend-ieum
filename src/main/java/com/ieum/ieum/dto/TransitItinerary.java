package com.ieum.ieum.dto;

import java.util.List;

/** 대중교통 경로 후보 하나 (티맵 응답의 itinerary 1건) — "경로 후보 목록" 화면에 그대로 대응 */
public record TransitItinerary(
        int totalTime,          // s
        int totalDistance,      // m
        int totalWalkTime,      // s
        int totalWalkDistance,  // m
        int transferCount,
        int fare,               // 원
        List<TransitLeg> legs
) {}
