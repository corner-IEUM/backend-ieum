package com.ieum.ieum.dto;

public record CrosswalkAccessibilitySummary(
        int noCurbCutCount,        // 턱 안낮춤 횡단보도 개수
        int noBrailleBlockCount,   // 점자블록 없는 횡단보도 개수
        int noSoundSignalCount,    // 음향신호기 없는 횡단보도 개수
        int noPedestrianSignalCount // 보행자신호등 없는 횡단보도 개수
) {}
