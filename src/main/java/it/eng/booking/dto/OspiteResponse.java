package it.eng.booking.dto;

import it.eng.booking.model.Ospite;

public record OspiteResponse(Long id, String nome, String cognome, String email, String telefono) {
    public static OspiteResponse from(Ospite ospite) {
        return new OspiteResponse(ospite.getId(), ospite.getNome(), ospite.getCognome(),
                ospite.getEmail(), ospite.getTelefono());
    }
}