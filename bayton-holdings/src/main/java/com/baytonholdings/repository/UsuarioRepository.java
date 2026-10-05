package com.baytonholdings.repository;

import com.baytonholdings.domain.Rol;
import com.baytonholdings.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado por la capa de seguridad para autenticar. */
    Optional<Usuario> findByUsername(String username);

    /** Para validar que el username no esté tomado al crear una cuenta. */
    boolean existsByUsername(String username);

    /**
     * Cuenta de un vendedor o asesor. Como {@code referenciaId} no tiene clave
     * foránea y los ids de vendedor y asesor pueden coincidir, siempre se
     * consulta junto con el rol. Lo usa el servicio para borrar la cuenta al
     * eliminar a la persona.
     */
    Optional<Usuario> findByRolAndReferenciaId(Rol rol, Long referenciaId);
}
