package com.ieum.ieum.dto;

/** 도보 leg 안의 세부 이동 단계 (티맵 보행자 경로의 안내 지점과 유사한 역할) */
public record WalkStep(String streetName, int distance, String description, String linestring) {}
