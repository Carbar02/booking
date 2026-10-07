package it.eng.booking.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import it.eng.booking.model.Appartamento;
import it.eng.booking.model.Camera;
import it.eng.booking.model.TipoCamera;
import it.eng.booking.repository.AppartamentoRepository;
import it.eng.booking.repository.CameraRepository;

@Component
public class DatiIniziali implements ApplicationRunner {

    private final AppartamentoRepository appartamentoRepository;
    private final CameraRepository cameraRepository;

    public DatiIniziali(AppartamentoRepository appartamentoRepository,
            CameraRepository cameraRepository) {
        this.appartamentoRepository = appartamentoRepository;
        this.cameraRepository = cameraRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (appartamentoRepository.count() != 0) {
            return;
        }
        Appartamento aurora = appartamentoRepository.save(new Appartamento(
                "Aurora", "Via Roma 10, Firenze", "Appartamento vicino al centro"));
        Appartamento giardino = appartamentoRepository.save(new Appartamento(
                "Giardino", "Via dei Tigli 5, Firenze", "Appartamento con giardino"));
        cameraRepository.saveAll(List.of(
                new Camera("101", TipoCamera.SINGOLA, 1, new BigDecimal("55.00"), aurora, true),
                new Camera("102", TipoCamera.DOPPIA, 2, new BigDecimal("85.00"), aurora, true),
                new Camera("103", TipoCamera.SINGOLA, 1, new BigDecimal("55.00"), aurora, false),
                new Camera("201", TipoCamera.DOPPIA, 2, new BigDecimal("90.00"), giardino, true),
                new Camera("202", TipoCamera.FAMILIARE, 4, new BigDecimal("140.00"), giardino, true)));
    }
}