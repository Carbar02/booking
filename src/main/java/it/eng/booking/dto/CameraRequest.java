package it.eng.booking.dto;

import java.math.BigDecimal;

import it.eng.booking.model.TipoCamera;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CameraRequest(
        @NotBlank @Size(max = 20) String numero,
        @NotNull TipoCamera tipo,
        @Positive int capienzaMassima,
        @NotNull @PositiveOrZero BigDecimal prezzoPerNotte,
        boolean attiva) {
}