package it.eng.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.eng.booking.model.Ospite;

public interface OspiteRepository extends JpaRepository<Ospite, Long> {
}