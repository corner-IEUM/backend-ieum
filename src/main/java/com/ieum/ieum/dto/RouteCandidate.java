package com.ieum.ieum.dto;

import java.util.List;

public record RouteCandidate(
        int searchOption,   // 이 후보를 저장/시작할 때 /route 요청에 그대로 넣을 값
        String label,       // "추천 경로" / "계단 없는 경로" 등 화면 표시용
        int distance,       // m
        int duration,       // s
        List<Crosswalk> crosswalks,
        List<Stair> stairs,
        CrosswalkAccessibilitySummary accessibilitySummary
) {}
