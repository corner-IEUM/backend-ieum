package com.ieum.ieum.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransitRouteSaveResponse {
    private Long navigationId;
    private String routeType;
    private int currentLegIndex;
    private String currentLegState;
}