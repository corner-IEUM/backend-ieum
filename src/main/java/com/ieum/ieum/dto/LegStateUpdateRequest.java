package com.ieum.ieum.dto;

import com.ieum.ieum.domain.LegState;
import jakarta.validation.constraints.NotNull;

public record LegStateUpdateRequest(
        @NotNull Integer currentLegIndex,
        @NotNull LegState currentLegState
) {}