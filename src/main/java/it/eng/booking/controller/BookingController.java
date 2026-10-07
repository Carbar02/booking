package it.eng.booking.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.eng.booking.dto.AppartamentoResponse;
import it.eng.booking.dto.CameraResponse;
import it.eng.booking.dto.CreaPrenotazioneRequest;
import it.eng.booking.dto.PrenotazioneResponse;
import it.eng.booking.model.TipoCamera;
import it.eng.booking.service.CameraService;
import it.eng.booking.service.PrenotazioneService;
import jakarta.validation.Valid;

@RestController
public class BookingController {

    private final CameraService cameraService;
    private final PrenotazioneService prenotazioneService;

    public BookingController(CameraService cameraService, PrenotazioneService prenotazioneService) {
        this.cameraService = cameraService;
        this.prenotazioneService = prenotazioneService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Booking API funzionante!";
    }

    @GetMapping("/camere")
    public List<CameraResponse> getCamere(
            @RequestParam(required = false) Long appartamentoId,
            @RequestParam(required = false) TipoCamera tipo) {
        return cameraService.getCamere(appartamentoId, tipo);
    }

    @GetMapping("/appartamenti")
    public List<AppartamentoResponse> getAppartamenti() {
        return cameraService.getAppartamenti();
    }

    @GetMapping("/camere/disponibili")
    public List<CameraResponse> getCamereDisponibili(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataArrivo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPartenza,
            @RequestParam(defaultValue = "1") int numeroOspiti,
            @RequestParam(required = false) Long appartamentoId,
            @RequestParam(required = false) TipoCamera tipo) {
        return cameraService.getDisponibili(dataArrivo, dataPartenza, numeroOspiti, appartamentoId, tipo);
    }

    @GetMapping("/prenotazioni")
    public List<PrenotazioneResponse> getPrenotazioni() {
        return prenotazioneService.getPrenotazioni();
    }

    @GetMapping("/prenotazioni/{id}")
    public PrenotazioneResponse getPrenotazione(@PathVariable Long id) {
        return prenotazioneService.getPrenotazione(id);
    }

    @PostMapping("/prenotazioni")
    public ResponseEntity<PrenotazioneResponse> creaPrenotazione(
            @Valid @RequestBody CreaPrenotazioneRequest request) {
        PrenotazioneResponse prenotazione = prenotazioneService.crea(request);
        return ResponseEntity.created(URI.create("/prenotazioni/" + prenotazione.id())).body(prenotazione);
    }

    @PostMapping("/prenotazioni/{id}/annulla")
    public PrenotazioneResponse annullaPrenotazione(@PathVariable Long id) {
        return prenotazioneService.annulla(id);
    }
}
