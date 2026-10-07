package it.eng.booking.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Table;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"appartamento_id", "numero"}))
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCamera tipo;
    private int capienzaMassima;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzoPerNotte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appartamento_id", nullable = false)
    private Appartamento appartamento;
    private boolean attiva;

    protected Camera() {
    }

    public Camera(String numero, TipoCamera tipo, int capienzaMassima,
            BigDecimal prezzoPerNotte, Appartamento appartamento, boolean attiva) {
        this.numero = numero;
        this.tipo = tipo;
        this.capienzaMassima = capienzaMassima;
        this.prezzoPerNotte = prezzoPerNotte;
        this.appartamento = appartamento;
        this.attiva = attiva;
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public TipoCamera getTipo() {
        return tipo;
    }

    public int getCapienzaMassima() {
        return capienzaMassima;
    }

    public BigDecimal getPrezzoPerNotte() {
        return prezzoPerNotte;
    }

    public Appartamento getAppartamento() {
        return appartamento;
    }

    public boolean isAttiva() {
        return attiva;
    }
}