package it.eng.booking.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.eng.booking.dto.AppartamentoResponse;
import it.eng.booking.dto.AppartamentoRequest;
import it.eng.booking.dto.CameraRequest;
import it.eng.booking.dto.CameraResponse;
import it.eng.booking.dto.CreaPrenotazioneRequest;
import it.eng.booking.dto.OspiteRequest;
import it.eng.booking.dto.OspiteResponse;
import it.eng.booking.dto.PrenotazioneResponse;
import it.eng.booking.model.TipoCamera;
import it.eng.booking.service.CameraService;
import it.eng.booking.service.PrenotazioneService;
import it.eng.booking.service.OspiteService;
import jakarta.validation.Valid;

@RestController
public class BookingController {

    private final CameraService cameraService;
    private final PrenotazioneService prenotazioneService;
    private final OspiteService ospiteService;

    public BookingController(CameraService cameraService, PrenotazioneService prenotazioneService,
            OspiteService ospiteService) {
        this.cameraService = cameraService;
        this.prenotazioneService = prenotazioneService;
        this.ospiteService = ospiteService;
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

    @PostMapping("/gestione/appartamenti")
    public ResponseEntity<AppartamentoResponse> creaAppartamento(@Valid @RequestBody AppartamentoRequest request) {
        AppartamentoResponse appartamento = cameraService.creaAppartamento(request);
        return ResponseEntity.created(URI.create("/appartamenti/" + appartamento.id())).body(appartamento);
    }

    @PutMapping("/gestione/appartamenti/{id}")
    public AppartamentoResponse aggiornaAppartamento(@PathVariable Long id,
            @Valid @RequestBody AppartamentoRequest request) {
        return cameraService.aggiornaAppartamento(id, request);
    }

    @PostMapping("/gestione/appartamenti/{id}/camere")
    public ResponseEntity<CameraResponse> creaCamera(@PathVariable Long id,
            @Valid @RequestBody CameraRequest request) {
        CameraResponse camera = cameraService.creaCamera(id, request);
        return ResponseEntity.created(URI.create("/camere/" + camera.id())).body(camera);
    }

    @PutMapping("/gestione/camere/{id}")
    public CameraResponse aggiornaCamera(@PathVariable Long id,
            @Valid @RequestBody CameraRequest request) {
        return cameraService.aggiornaCamera(id, request);
    }

    @GetMapping("/gestione/ospiti")
    public List<OspiteResponse> getOspiti() {
        return ospiteService.getOspiti();
    }

    @PostMapping("/gestione/ospiti")
    public ResponseEntity<OspiteResponse> creaOspite(@Valid @RequestBody OspiteRequest request) {
        OspiteResponse ospite = ospiteService.crea(request);
        return ResponseEntity.created(URI.create("/gestione/ospiti/" + ospite.id())).body(ospite);
    }

    @PutMapping("/gestione/ospiti/{id}")
    public OspiteResponse aggiornaOspite(@PathVariable Long id,
            @Valid @RequestBody OspiteRequest request) {
        return ospiteService.aggiorna(id, request);
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
