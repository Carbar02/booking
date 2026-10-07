package it.eng.booking.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.eng.booking.model.Prenotazione;
import it.eng.booking.model.StatoPrenotazione;
import jakarta.persistence.LockModeType;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {

    List<Prenotazione> findAllByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select prenotazione from Prenotazione prenotazione where prenotazione.id = :id")
    Optional<Prenotazione> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select distinct dettaglio.camera.id from CameraPrenotata dettaglio
            where dettaglio.prenotazione.stato in :stati
              and dettaglio.prenotazione.dataArrivo < :partenza
              and dettaglio.prenotazione.dataPartenza > :arrivo
            """)
    List<Long> findCameraIdsOccupate(@Param("arrivo") LocalDate arrivo,
            @Param("partenza") LocalDate partenza,
            @Param("stati") List<StatoPrenotazione> stati);

    @Query("""
            select count(dettaglio) from CameraPrenotata dettaglio
            where dettaglio.camera.id = :cameraId
              and dettaglio.prenotazione.stato in :stati
              and dettaglio.prenotazione.dataArrivo < :partenza
              and dettaglio.prenotazione.dataPartenza > :arrivo
            """)
    long countSovrapposizioni(@Param("cameraId") Long cameraId,
            @Param("arrivo") LocalDate arrivo, @Param("partenza") LocalDate partenza,
            @Param("stati") List<StatoPrenotazione> stati);
}