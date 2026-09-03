package com.ieum.ieum.dto;

import jakarta.validation.constraints.NotNull;

//record : "값만 담음" -> DTO로 쓰기 적합, 불변객체, getter만 존재, equals/hashcode/toString 자동 생성
public record WalkCompleteRequest(
    @NotNull String routeData, //JSON 형태로 저장되는 경로 데이터

    @NotNull Double totalDistance, //보행에서 이동한 총 거리(단위 : 미터)

    @NotNull Long totalTime, //보행 걸린 총 시간(단위 : 초)

    @NotNull Integer obstacleCount //보행시 감지한 쟁애물 개수
) {}