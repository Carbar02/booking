package it.eng.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppartamentoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 240) String indirizzo,
        @Size(max = 500) String descrizione) {
}