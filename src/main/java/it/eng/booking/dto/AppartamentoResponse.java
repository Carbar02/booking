package it.eng.booking.dto;

import java.util.Comparator;
import java.util.List;

import it.eng.booking.model.Appartamento;
import it.eng.booking.model.Camera;

public record AppartamentoResponse(Long id, String nome, String indirizzo,
        String descrizione, List<CameraResponse> camere) {

    public static AppartamentoResponse from(Appartamento appartamento) {
        return new AppartamentoResponse(appartamento.getId(), appartamento.getNome(),
                appartamento.getIndirizzo(), appartamento.getDescrizione(),
                appartamento.getCamere().stream().sorted(Comparator.comparing(Camera::getId))
                        .map(CameraResponse::from).toList());
    }
}