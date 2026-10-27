package com.ieum.ieum.dto;

public record ArrivalResponse(
        String routeName,
        int arrivalSec,
        Integer nextArrivalSec
) {}