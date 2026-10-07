package it.eng.booking.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.eng.booking.dto.AppartamentoResponse;
import it.eng.booking.dto.CameraResponse;
import it.eng.booking.exception.RichiestaNonValidaException;
import it.eng.booking.model.StatoPrenotazione;
import it.eng.booking.model.TipoCamera;
import it.eng.booking.repository.AppartamentoRepository;
import it.eng.booking.repository.CameraRepository;
import it.eng.booking.repository.PrenotazioneRepository;

@Service
@Transactional(readOnly = true)
public class CameraService {

    private final CameraRepository cameraRepository;
    private final AppartamentoRepository appartamentoRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    public CameraService(CameraRepository cameraRepository,
            AppartamentoRepository appartamentoRepository,
            PrenotazioneRepository prenotazioneRepository) {
        this.cameraRepository = cameraRepository;
        this.appartamentoRepository = appartamentoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    public List<CameraResponse> getCamere(Long appartamentoId, TipoCamera tipo) {
        return cameraRepository.findAllByOrderByIdAsc().stream()
                .filter(camera -> appartamentoId == null
                        || camera.getAppartamento().getId().equals(appartamentoId))
                .filter(camera -> tipo == null || camera.getTipo() == tipo)
                .map(CameraResponse::from).toList();
    }

    public List<AppartamentoResponse> getAppartamenti() {
        return appartamentoRepository.findAllByOrderByIdAsc().stream()
                .map(AppartamentoResponse::from).toList();
    }

    public List<CameraResponse> getDisponibili(LocalDate dataArrivo, LocalDate dataPartenza,
            int numeroOspiti, Long appartamentoId, TipoCamera tipo) {
        if (dataArrivo.isBefore(LocalDate.now()) || !dataPartenza.isAfter(dataArrivo)) {
            throw new RichiestaNonValidaException(
                    "L'arrivo non puo essere nel passato e la partenza deve essere successiva all'arrivo.");
        }
        if (numeroOspiti <= 0) {
            throw new RichiestaNonValidaException("Il numero di ospiti deve essere positivo.");
        }
        Set<Long> occupate = new HashSet<>(prenotazioneRepository.findCameraIdsOccupate(
                dataArrivo, dataPartenza,
                List.of(StatoPrenotazione.IN_ATTESA, StatoPrenotazione.CONFERMATA)));
        return getCamere(appartamentoId, tipo).stream()
                .filter(camera -> camera.attiva() && camera.capienzaMassima() >= numeroOspiti)
                .filter(camera -> !occupate.contains(camera.id())).toList();
    }
}