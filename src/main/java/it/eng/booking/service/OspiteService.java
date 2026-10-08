package it.eng.booking.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.eng.booking.dto.OspiteRequest;
import it.eng.booking.dto.OspiteResponse;
import it.eng.booking.exception.RichiestaNonValidaException;
import it.eng.booking.exception.RisorsaGiaEsistenteException;
import it.eng.booking.exception.RisorsaNonTrovataException;
import it.eng.booking.model.Ospite;
import it.eng.booking.repository.OspiteRepository;

@Service
@Transactional(readOnly = true)
public class OspiteService {

    private final OspiteRepository ospiteRepository;

    public OspiteService(OspiteRepository ospiteRepository) {
        this.ospiteRepository = ospiteRepository;
    }

    public List<OspiteResponse> getOspiti() {
        return ospiteRepository.findAllByOrderByCognomeAscNomeAsc().stream()
                .map(OspiteResponse::from).toList();
    }

    @Transactional
    public OspiteResponse crea(OspiteRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (ospiteRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(email).isPresent()) {
            throw new RisorsaGiaEsistenteException("Esiste gia un ospite con questa email.");
        }
        Ospite ospite = ospiteRepository.save(new Ospite(request.nome().trim(), request.cognome().trim(),
                email, request.telefono().trim()));
        return OspiteResponse.from(ospite);
    }

    @Transactional
    public OspiteResponse aggiorna(Long id, OspiteRequest request) {
        Ospite ospite = ospiteRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Ospite non trovato: " + id));
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (ospiteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new RichiestaNonValidaException("Esiste gia un ospite con questa email.");
        }
        ospite.aggiorna(request.nome().trim(), request.cognome().trim(), email, request.telefono().trim());
        return OspiteResponse.from(ospite);
    }
}