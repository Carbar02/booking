package it.eng.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import it.eng.booking.model.Ospite;
import it.eng.booking.model.Prenotazione;
import it.eng.booking.model.StatoPrenotazione;

public record PrenotazioneResponse(Long id, OspiteResponse ospite,
        LocalDate dataArrivo, LocalDate dataPartenza, StatoPrenotazione stato,
        LocalDateTime dataCreazione, List<CameraPrenotataResponse> camere, BigDecimal totale) {

    public static PrenotazioneResponse from(Prenotazione prenotazione) {
        long notti = ChronoUnit.DAYS.between(prenotazione.getDataArrivo(), prenotazione.getDataPartenza());
        List<CameraPrenotataResponse> dettagli = prenotazione.getCamere().stream()
                .map(dettaglio -> new CameraPrenotataResponse(
                        CameraResponse.from(dettaglio.getCamera()), dettaglio.getNumeroOspiti(),
                        dettaglio.getPrezzoPerNotteConcordato(),
                        dettaglio.getPrezzoPerNotteConcordato().multiply(BigDecimal.valueOf(notti))))
                .toList();
        BigDecimal totale = dettagli.stream().map(CameraPrenotataResponse::totaleSoggiorno)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Ospite ospite = prenotazione.getOspite();
        return new PrenotazioneResponse(prenotazione.getId(),
                new OspiteResponse(ospite.getId(), ospite.getNome(), ospite.getCognome(),
                        ospite.getEmail(), ospite.getTelefono()),
                prenotazione.getDataArrivo(), prenotazione.getDataPartenza(), prenotazione.getStato(),
                prenotazione.getDataCreazione(), dettagli, totale);
    }

    public record OspiteResponse(Long id, String nome, String cognome, String email, String telefono) {
    }

    public record CameraPrenotataResponse(CameraResponse camera, int numeroOspiti,
            BigDecimal prezzoPerNotteConcordato, BigDecimal totaleSoggiorno) {
    }
}