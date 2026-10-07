package it.eng.booking.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"prenotazione_id", "camera_id"}))
public class CameraPrenotata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prenotazione_id", nullable = false)
    private Prenotazione prenotazione;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camera_id", nullable = false)
    private Camera camera;

    @Column(nullable = false)
    private int numeroOspiti;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoPerNotteConcordato;

    protected CameraPrenotata() {
    }

    public CameraPrenotata(Prenotazione prenotazione, Camera camera,
            int numeroOspiti, BigDecimal prezzoPerNotteConcordato) {
        this.prenotazione = prenotazione;
        this.camera = camera;
        this.numeroOspiti = numeroOspiti;
        this.prezzoPerNotteConcordato = prezzoPerNotteConcordato;
    }

    public Long getId() {
        return id;
    }

    public Prenotazione getPrenotazione() {
        return prenotazione;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getNumeroOspiti() {
        return numeroOspiti;
    }

    public BigDecimal getPrezzoPerNotteConcordato() {
        return prezzoPerNotteConcordato;
    }
}