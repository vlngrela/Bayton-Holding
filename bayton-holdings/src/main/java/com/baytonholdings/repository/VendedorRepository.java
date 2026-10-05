package com.baytonholdings.repository;

import com.baytonholdings.domain.Vendedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor, Long> {

    /**
     * Vendedores de una empresa.
     * JOIN FETCH de empresa y captador: el DTO de vendedor muestra el nombre
     * de ambos; sin el fetch habría una consulta extra por fila (N+1). El
     * captador va con LEFT JOIN porque es opcional.
     */
    @Query("""
           select v from Vendedor v
           join fetch v.empresa
           left join fetch v.captador
           where v.empresa.id = :empresaId
           order by v.nombre
           """)
    List<Vendedor> findByEmpresaId(@Param("empresaId") Long empresaId);

    /**
     * Vendedores captados directamente por el vendedor dado.
     * Es INNER JOIN en captador porque, por la condición, siempre existe.
     */
    @Query("""
           select v from Vendedor v
           join fetch v.empresa
           join fetch v.captador
           where v.captador.id = :captadorId
           order by v.nombre
           """)
    List<Vendedor> findByCaptadorId(@Param("captadorId") Long captadorId);

    /** Listado completo para la grilla del administrador, con empresa y captador ya cargados. */
    @Query("""
           select v from Vendedor v
           join fetch v.empresa
           left join fetch v.captador
           order by v.nombre
           """)
    List<Vendedor> findAllConDetalle();

    /**
     * Un vendedor con empresa, captador y captados, para el formulario de
     * edición y la vista "Mi perfil" del vendedor. Es una sola colección
     * (captados), así que no hay riesgo de MultipleBagFetchException.
     */
    @Query("""
           select v from Vendedor v
           join fetch v.empresa
           left join fetch v.captador
           left join fetch v.captados
           where v.id = :id
           """)
    Optional<Vendedor> findConDetalleById(@Param("id") Long id);

    /** Para validar unicidad del código antes de dar de alta. */
    boolean existsByCodigoIgnoreCase(String codigo);
}
