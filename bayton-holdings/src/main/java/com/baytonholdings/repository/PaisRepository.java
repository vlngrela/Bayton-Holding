package com.baytonholdings.repository;

import com.baytonholdings.domain.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Long> {

    /** Para validar unicidad del nombre antes de dar de alta (el servicio lanza la excepción). */
    boolean existsByNombreIgnoreCase(String nombre);
}
