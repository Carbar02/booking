package it.eng.booking.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreaPrenotazioneRequest(
        @NotNull @Valid OspiteRequest ospite,
        @NotNull @FutureOrPresent LocalDate dataArrivo,
        @NotNull LocalDate dataPartenza,
        @NotEmpty List<@NotNull @Valid CameraPrenotazioneRequest> camere) {
}