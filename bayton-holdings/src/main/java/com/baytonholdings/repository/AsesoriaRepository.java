package com.baytonholdings.repository;

import com.baytonholdings.domain.Asesoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsesoriaRepository extends JpaRepository<Asesoria, Long> {

    /**
     * Asesorías de un asesor (vista "Mi perfil" del asesor y detalle en la grilla).
     * JOIN FETCH de asesor, empresa y área: el DTO muestra el nombre de los
     * tres, y las tres relaciones son LAZY. Sin el fetch habría hasta tres
     * consultas extra por fila (N+1), o un LazyInitializationException si el
     * mapeo ocurriera fuera de la transacción. Son todas N:1, así que no hay
     * producto cartesiano.
     */
    @Query("""
           select a from Asesoria a
           join fetch a.asesor
           join fetch a.empresa
           join fetch a.area
           where a.asesor.id = :asesorId
           order by a.fechaInicio desc
           """)
    List<Asesoria> findByAsesorId(@Param("asesorId") Long asesorId);

    /** Asesorías de una empresa; mismo criterio de JOIN FETCH que findByAsesorId. */
    @Query("""
           select a from Asesoria a
           join fetch a.asesor
           join fetch a.empresa
           join fetch a.area
           where a.empresa.id = :empresaId
           order by a.fechaInicio desc
           """)
    List<Asesoria> findByEmpresaId(@Param("empresaId") Long empresaId);

    /** Listado completo para la grilla del administrador. */
    @Query("""
           select a from Asesoria a
           join fetch a.asesor
           join fetch a.empresa
           join fetch a.area
           order by a.fechaInicio desc
           """)
    List<Asesoria> findAllConDetalle();

    /**
     * Una asesoría con asesor, empresa y área ya cargados, para el formulario
     * de edición y para validar cambios en el servicio. Mismo criterio de
     * JOIN FETCH que los listados (todas N:1, sin producto cartesiano).
     */
    @Query("""
           select a from Asesoria a
           join fetch a.asesor
           join fetch a.empresa
           join fetch a.area
           where a.id = :id
           """)
    Optional<Asesoria> findConDetalleById(@Param("id") Long id);

    /**
     * Asesorías que referencian a un área. Sin fetch a propósito: solo la usa
     * el servicio para borrarlas al eliminar el área, dentro de la transacción.
     */
    List<Asesoria> findByAreaId(Long areaId);

    /** Para rechazar la terna (asesor, empresa, área) repetida con un mensaje claro, antes de que falle la restricción única. */
    boolean existsByAsesorIdAndEmpresaIdAndAreaId(Long asesorId, Long empresaId, Long areaId);

    /** Conteos para armar el mensaje de confirmación de baja (cantidad de dependientes). */
    long countByAsesorId(Long asesorId);

    long countByEmpresaId(Long empresaId);

    long countByAreaId(Long areaId);
}
