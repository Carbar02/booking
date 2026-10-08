package it.eng.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.eng.booking.model.Ospite;

public interface OspiteRepository extends JpaRepository<Ospite, Long> {
	List<Ospite> findAllByOrderByCognomeAscNomeAsc();

	Optional<Ospite> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}