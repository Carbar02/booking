package it.eng.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatMessage(
        @NotNull ChatRole role,
        @NotBlank @Size(max = 2500) String content) {
}
