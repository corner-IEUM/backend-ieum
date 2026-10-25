package com.ieum.ieum.dto;

import java.util.List;

/**
 * 대중교통 탑승 구간(mode=BUS/SUBWAY 등). 정류장 대기/탑승 확인/하차 확인 상태 머신이
 * 다루는 leg. 같은 정류장에서 탈 수 있는 다른 노선 목록(Lane)은 현재 범위에서는 쓰지
 * 않아 의도적으로 파싱하지 않는다 — 필요해지면 그때 추가.
 */
public record TransitRideLeg(
        String mode,          // BUS, SUBWAY, EXPRESSBUS 등
        int distance,
        int sectionTime,
        LegPoint start,
        LegPoint end,
        String route,         // 노선명 (예: "간선:370", "수도권5호선")
        String routeId,
        String routeColor,
        int routeTypeCode,    // 티맵 "type" — 이동수단별 노선코드
        boolean inService,    // 티맵 "service": 1=운행중, 0=운행종료
        List<TransitStation> stations,
        String passShape      // 경로 좌표 linestring 원본 문자열 ("lon,lat lon,lat ...")
) implements TransitLeg {}
