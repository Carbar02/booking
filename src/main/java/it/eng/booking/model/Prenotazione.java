package it.eng.booking.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Ospite ospite;

    @Column(nullable = false)
    private LocalDate dataArrivo;

    @Column(nullable = false)
    private LocalDate dataPartenza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoPrenotazione stato;

    @Column(nullable = false)
    private LocalDateTime dataCreazione;

    @OneToMany(mappedBy = "prenotazione", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CameraPrenotata> camere = new ArrayList<>();

    protected Prenotazione() {
    }

    public Prenotazione(Ospite ospite, LocalDate dataArrivo, LocalDate dataPartenza) {
        this.ospite = ospite;
        this.dataArrivo = dataArrivo;
        this.dataPartenza = dataPartenza;
        this.stato = StatoPrenotazione.CONFERMATA;
        this.dataCreazione = LocalDateTime.now();
    }

    public void aggiungiCamera(Camera camera, int numeroOspiti) {
        camere.add(new CameraPrenotata(this, camera, numeroOspiti, camera.getPrezzoPerNotte()));
    }

    public void annulla() {
        stato = StatoPrenotazione.ANNULLATA;
    }

    public Long getId() {
        return id;
    }

    public Ospite getOspite() {
        return ospite;
    }

    public LocalDate getDataArrivo() {
        return dataArrivo;
    }

    public LocalDate getDataPartenza() {
        return dataPartenza;
    }

    public StatoPrenotazione getStato() {
        return stato;
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public List<CameraPrenotata> getCamere() {
        return List.copyOf(camere);
    }
}