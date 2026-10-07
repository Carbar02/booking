package it.eng.booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.eng.booking.model.Camera;
import jakarta.persistence.LockModeType;

public interface CameraRepository extends JpaRepository<Camera, Long> {
    List<Camera> findAllByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select camera from Camera camera where camera.id = :id")
    Optional<Camera> findByIdForUpdate(@Param("id") Long id);
}