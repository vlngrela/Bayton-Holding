package com.baytonholdings.repository;

import com.baytonholdings.domain.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    /** Para validar unicidad del nombre antes de dar de alta. */
    boolean existsByNombreIgnoreCase(String nombre);
}
