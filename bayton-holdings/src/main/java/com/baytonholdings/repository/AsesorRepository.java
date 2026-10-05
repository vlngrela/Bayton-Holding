package com.baytonholdings.repository;

import com.baytonholdings.domain.Asesor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsesorRepository extends JpaRepository<Asesor, Long> {

    /**
     * Listado para la grilla del administrador con las áreas ya cargadas: la
     * grilla muestra las áreas de cada asesor, y sin el fetch habría una
     * consulta extra por asesor (N+1). LEFT JOIN porque un asesor puede no
     * tener áreas. Las asesorías NO se traen acá (ver AsesoriaRepository).
     */
    @Query("select a from Asesor a left join fetch a.areas order by a.nombre")
    List<Asesor> findAllConAreas();

    /** Un asesor con sus áreas, para el formulario de edición y la vista "Mi perfil". */
    @Query("select a from Asesor a left join fetch a.areas where a.id = :id")
    Optional<Asesor> findConAreasById(@Param("id") Long id);

    /** Para validar unicidad del código antes de dar de alta. */
    boolean existsByCodigoIgnoreCase(String codigo);
}
