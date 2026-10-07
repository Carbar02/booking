package it.eng.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.eng.booking.model.Appartamento;

public interface AppartamentoRepository extends JpaRepository<Appartamento, Long> {
    List<Appartamento> findAllByOrderByIdAsc();
}