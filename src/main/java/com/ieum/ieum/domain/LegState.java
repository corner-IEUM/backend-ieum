package com.ieum.ieum.domain;

/**
 * TRANSIT 경로에서 현재 구간(leg)이 진행 중인 단계.
 * 프론트엔드 상태 머신과 1:1로 맞춘 값이므로 이름을 임의로 바꾸지 말 것.
 *
 * WALK_TO_STOP(정류장으로 도보 이동) -> WAITING_AT_STOP(정류장 도착정보 대기)
 * -> BOARDING_CONFIRM(탑승 확인 대기) -> ON_BUS(버스 이동 중)
 * -> ALIGHTING_CONFIRM(하차 확인 대기) -> WALK_TO_DEST(목적지로 도보 이동)
 */
public enum LegState {
    WALK_TO_STOP, WAITING_AT_STOP, BOARDING_CONFIRM, ON_BUS, ALIGHTING_CONFIRM, WALK_TO_DEST
}
