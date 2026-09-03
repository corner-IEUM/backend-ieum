package com.ieum.ieum.dto;

public record CrosswalkHazard(
        int priority,   // GuidanceMessage와 동일한 스케일 (1=즉각위험, 2=결정적순간, 3=일반정보)
        String message
) {}
