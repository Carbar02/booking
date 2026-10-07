package it.eng.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OspiteRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 100) String cognome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String telefono) {
}