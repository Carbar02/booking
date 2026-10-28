package it.eng.booking.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 1000) String question,
        @Size(max = 8) List<@Valid ChatMessage> history) {

    public ChatRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
