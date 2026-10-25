package com.ieum.ieum.dto;

import java.util.List;

/**
 * GET /api/navigation/transit/candidates 응답.
 * transitOptions는 대중교통 경유 후보 목록(없으면 빈 리스트), walkOption은 도보 전용 후보로
 * 항상 포함한다 — 프론트의 "버스 이용 vs 도보만" 비교 화면과, 대중교통 후보가 0건일 때의
 * 폴백 화면을 이 응답 하나로 동시에 지원한다.
 */
public record TransitCandidatesResponse(
        List<TransitItinerary> transitOptions,
        RouteCandidate walkOption
) {}
