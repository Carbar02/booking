package it.eng.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CameraPrenotazioneRequest(
        @NotNull @Positive Long cameraId,
        @NotNull @Positive Integer numeroOspiti) {
}