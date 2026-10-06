package com.pulsepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetStreamingUrlDto(@NotBlank @Size(max = 500) String streamingUrl) {
}
