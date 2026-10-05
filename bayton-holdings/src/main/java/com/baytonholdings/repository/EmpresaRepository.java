package com.baytonholdings.repository;

import com.baytonholdings.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    /**
     * Empresas cuya sede está en el país dado.
     * JOIN FETCH de paisSede: el DTO de empresa muestra el nombre del país, y
     * sin el fetch cada fila dispararía una consulta extra (N+1) o un
     * LazyInitializationException si se mapea fuera de la transacción.
     */
    @Query("""
           select e from Empresa e
           join fetch e.paisSede
           where e.paisSede.id = :paisId
           order by e.nombre
           """)
    List<Empresa> findByPaisSedeId(@Param("paisId") Long paisId);

    /** Listado completo para la grilla del administrador, con el país sede ya cargado. */
    @Query("select e from Empresa e join fetch e.paisSede order by e.nombre")
    List<Empresa> findAllConPaisSede();

    /**
     * Una empresa con todo lo que necesita el formulario de edición y la vista
     * de detalle: país sede, países de operación y áreas.
     * Se traen dos colecciones a la vez; es válido porque son Set (con List
     * Hibernate lanzaría MultipleBagFetchException). Los vendedores NO se
     * traen: serían un tercer producto cartesiano, y getNumVendedores() los
     * carga con una consulta aparte dentro de la transacción.
     * Hibernate 6+ elimina solo los duplicados del padre; no hace falta DISTINCT.
     */
    @Query("""
           select e from Empresa e
           join fetch e.paisSede
           left join fetch e.paisesOperacion
           left join fetch e.areas
           where e.id = :id
           """)
    Optional<Empresa> findConDetalleById(@Param("id") Long id);

    /** Para validar unicidad del nombre antes de dar de alta. */
    boolean existsByNombreIgnoreCase(String nombre);

    /** ¿Alguna empresa tiene a este país como sede? (para bloquear la baja del país). */
    boolean existsByPaisSedeId(Long paisId);

    /** ¿Alguna empresa opera en este país? (para bloquear la baja del país). */
    boolean existsByPaisesOperacionId(Long paisId);
}
