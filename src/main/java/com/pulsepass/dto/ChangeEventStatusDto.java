package com.pulsepass.dto;

import com.pulsepass.domain.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEventStatusDto(@NotNull EventStatus newStatus) {
}
