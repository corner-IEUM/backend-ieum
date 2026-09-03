package com.ieum.ieum.dto;

import jakarta.validation.constraints.NotNull;

public record NearbyHazardRequest(
        @NotNull Double lng,
        @NotNull Double lat
) {}
