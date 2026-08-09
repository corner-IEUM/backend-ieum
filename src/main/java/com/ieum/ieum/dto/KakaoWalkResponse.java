package com.ieum.ieum.dto;

public record KakaoWalkResponse(String status, Route route) {
    public record Route(Properties properties) {}
    public record Properties(Integer totalDistance, Integer totalTime) {}
}