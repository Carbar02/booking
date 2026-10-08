package it.eng.booking.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Appartamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String indirizzo;
    private String descrizione;

    @OneToMany(mappedBy = "appartamento")
    private List<Camera> camere = new ArrayList<>();

    protected Appartamento() {
    }

    public Appartamento(String nome, String indirizzo, String descrizione) {
        this.nome = nome;
        this.indirizzo = indirizzo;
        this.descrizione = descrizione;
    }

    public void aggiorna(String nome, String indirizzo, String descrizione) {
        this.nome = nome;
        this.indirizzo = indirizzo;
        this.descrizione = descrizione;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public List<Camera> getCamere() {
        return List.copyOf(camere);
    }
}