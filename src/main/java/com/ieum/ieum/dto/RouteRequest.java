package com.ieum.ieum.dto;

import jakarta.validation.constraints.NotNull;

public record RouteRequest(
        @NotNull Double startLng,
        @NotNull Double startLat,
        @NotNull Double endLng,
        @NotNull Double endLat
) {}