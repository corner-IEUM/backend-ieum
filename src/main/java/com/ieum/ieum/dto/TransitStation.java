package com.ieum.ieum.dto;

/**
 * 대중교통 leg가 지나는 정류장/역 (passStopList.stations). index 0이 탑승 정류장,
 * 마지막 index가 하차 정류장이고 그 사이는 경유만 하는 중간 정류장이다.
 * GPS 기반 하차 안내("N정류장 전")가 이 목록을 그대로 활용한다.
 */
public record TransitStation(int index, String stationName, String stationId, double lng, double lat) {}
