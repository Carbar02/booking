package it.eng.booking.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.eng.booking.dto.CameraPrenotazioneRequest;
import it.eng.booking.dto.CreaPrenotazioneRequest;
import it.eng.booking.dto.OspiteRequest;
import it.eng.booking.dto.PrenotazioneResponse;
import it.eng.booking.exception.CameraNonDisponibileException;
import it.eng.booking.exception.RichiestaNonValidaException;
import it.eng.booking.exception.RisorsaNonTrovataException;
import it.eng.booking.model.Camera;
import it.eng.booking.model.Ospite;
import it.eng.booking.model.Prenotazione;
import it.eng.booking.model.StatoPrenotazione;
import it.eng.booking.repository.CameraRepository;
import it.eng.booking.repository.OspiteRepository;
import it.eng.booking.repository.PrenotazioneRepository;

@Service
@Transactional(readOnly = true)
public class PrenotazioneService {

    private final CameraRepository cameraRepository;
    private final OspiteRepository ospiteRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    public PrenotazioneService(CameraRepository cameraRepository, OspiteRepository ospiteRepository,
            PrenotazioneRepository prenotazioneRepository) {
        this.cameraRepository = cameraRepository;
        this.ospiteRepository = ospiteRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    public List<PrenotazioneResponse> getPrenotazioni() {
        return prenotazioneRepository.findAllByOrderByIdAsc().stream()
                .map(PrenotazioneResponse::from).toList();
    }

    public PrenotazioneResponse getPrenotazione(Long id) {
        return PrenotazioneResponse.from(prenotazioneRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Prenotazione non trovata: " + id)));
    }

    @Transactional
    public PrenotazioneResponse crea(CreaPrenotazioneRequest request) {
        if (request.dataArrivo().isBefore(LocalDate.now())
                || !request.dataPartenza().isAfter(request.dataArrivo())) {
            throw new RichiestaNonValidaException(
                    "L'arrivo non puo essere nel passato e la partenza deve essere successiva all'arrivo.");
        }
        Set<Long> cameraIds = new HashSet<>();
        for (CameraPrenotazioneRequest dettaglio : request.camere()) {
            if (!cameraIds.add(dettaglio.cameraId())) {
                throw new RichiestaNonValidaException("Una camera non puo essere inserita due volte.");
            }
        }
        List<CameraPrenotazioneRequest> dettagli = request.camere().stream()
                .sorted(Comparator.comparing(CameraPrenotazioneRequest::cameraId)).toList();
        List<Camera> camere = new ArrayList<>();
        for (CameraPrenotazioneRequest dettaglio : dettagli) {
            Camera camera = cameraRepository.findByIdForUpdate(dettaglio.cameraId())
                    .orElseThrow(() -> new RisorsaNonTrovataException(
                            "Camera non trovata: " + dettaglio.cameraId()));
            if (dettaglio.numeroOspiti() <= 0 || dettaglio.numeroOspiti() > camera.getCapienzaMassima()) {
                throw new RichiestaNonValidaException("Numero ospiti non valido per la camera " + camera.getNumero());
            }
            if (!camera.isAttiva() || prenotazioneRepository.countSovrapposizioni(camera.getId(),
                    request.dataArrivo(), request.dataPartenza(),
                    List.of(StatoPrenotazione.IN_ATTESA, StatoPrenotazione.CONFERMATA)) > 0) {
                throw new CameraNonDisponibileException("Camera non disponibile: " + camera.getNumero());
            }
            camere.add(camera);
        }
        OspiteRequest datiOspite = request.ospite();
        Ospite ospite = ospiteRepository.save(new Ospite(datiOspite.nome(), datiOspite.cognome(),
                datiOspite.email(), datiOspite.telefono()));
        Prenotazione prenotazione = new Prenotazione(ospite, request.dataArrivo(), request.dataPartenza());
        for (int indice = 0; indice < camere.size(); indice++) {
            prenotazione.aggiungiCamera(camere.get(indice), dettagli.get(indice).numeroOspiti());
        }
        return PrenotazioneResponse.from(prenotazioneRepository.save(prenotazione));
    }

    @Transactional
    public PrenotazioneResponse annulla(Long id) {
        Prenotazione prenotazione = prenotazioneRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Prenotazione non trovata: " + id));
        if (prenotazione.getStato() == StatoPrenotazione.COMPLETATA) {
            throw new RichiestaNonValidaException("Una prenotazione completata non puo essere annullata.");
        }
        prenotazione.annulla();
        return PrenotazioneResponse.from(prenotazione);
    }
}