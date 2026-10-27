package com.ieum.ieum.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransitRouteSaveRequest {
    private double startLat;
    private double startLng;
    private double destLat;
    private double destLng;
    private TransitItinerary itinerary; // 후보 중 사용자가 고른 경로 1개 

} 
