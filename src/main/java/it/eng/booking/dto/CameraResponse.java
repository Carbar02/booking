package it.eng.booking.dto;

import java.math.BigDecimal;

import it.eng.booking.model.Camera;
import it.eng.booking.model.TipoCamera;

public record CameraResponse(Long id, String numero, TipoCamera tipo,
        int capienzaMassima, BigDecimal prezzoPerNotte, boolean attiva,
        Long appartamentoId, String appartamentoNome) {

    public static CameraResponse from(Camera camera) {
        return new CameraResponse(camera.getId(), camera.getNumero(), camera.getTipo(),
                camera.getCapienzaMassima(), camera.getPrezzoPerNotte(), camera.isAttiva(),
                camera.getAppartamento().getId(), camera.getAppartamento().getNome());
    }
}