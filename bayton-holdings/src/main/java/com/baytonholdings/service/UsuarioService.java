package com.baytonholdings.service;

import com.baytonholdings.domain.Rol;
import com.baytonholdings.domain.Usuario;
import com.baytonholdings.repository.AsesorRepository;
import com.baytonholdings.repository.UsuarioRepository;
import com.baytonholdings.repository.VendedorRepository;
import com.baytonholdings.service.exception.BusinessException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.baytonholdings.service.ServiceSupport.noExiste;
import static com.baytonholdings.service.ServiceSupport.obligatorio;
import static com.baytonholdings.service.ServiceSupport.textoObligatorio;

/**
 * Lógica de negocio de las cuentas de acceso.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El username es único.</li>
 *   <li>La contraseña se guarda siempre con BCrypt; nunca en texto plano. Al
 *       crear una cuenta, {@code Usuario.password} se interpreta como
 *       <b>texto plano</b> y se encripta. Al modificar, {@link #save} NO toca
 *       la contraseña: se cambia solo con {@link #cambiarPassword}.</li>
 *   <li>ADMINISTRADOR no se asocia a nadie (referenciaId nulo). VENDEDOR y
 *       ASESOR deben apuntar a un vendedor o asesor existente.</li>
 *   <li>Un vendedor o asesor tiene, como máximo, una cuenta.</li>
 * </ul>
 * Las cuentas de vendedores y asesores las crea y elimina automáticamente
 * {@code VendedorService}/{@code AsesorService} mediante {@link #crearCuenta}
 * y {@link #eliminarPorReferencia}.</p>
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private static final int PASSWORD_MAX = 72; // límite de BCrypt

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final VendedorRepository vendedorRepository;
    private final AsesorRepository asesorRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          VendedorRepository vendedorRepository,
                          AsesorRepository asesorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.vendedorRepository = vendedorRepository;
        this.asesorRepository = asesorRepository;
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll(Sort.by("username"));
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    /** Cuenta del vendedor o asesor con ese id (el rol es parte de la clave porque los ids pueden coincidir). */
    public Optional<Usuario> findByReferencia(Rol rol, Long referenciaId) {
        return usuarioRepository.findByRolAndReferenciaId(rol, referenciaId);
    }

    /**
     * Alta (id nulo; el password va en texto plano y se encripta) o
     * modificación de username, rol y referencia (id presente; el password
     * no se modifica).
     */
    @Transactional
    public Usuario save(Usuario usuario) {
        String username = textoObligatorio(usuario.getUsername(), "usuario", 50);
        Rol rol = obligatorio(usuario.getRol(), "rol");
        validarReferencia(rol, usuario.getReferenciaId());
        Long referenciaId = usuario.getReferenciaId();

        if (usuario.getId() == null) {
            String clave = claveValida(usuario.getPassword());
            if (usuarioRepository.existsByUsername(username)) {
                throw new BusinessException("Ya existe un usuario llamado '" + username + "'.");
            }
            if (referenciaId != null && usuarioRepository.findByRolAndReferenciaId(rol, referenciaId).isPresent()) {
                throw new BusinessException("Esa persona ya tiene una cuenta de acceso.");
            }
            return usuarioRepository.save(new Usuario(username, passwordEncoder.encode(clave), rol, referenciaId));
        }

        Usuario existente = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> noExiste("el usuario", usuario.getId()));
        if (!existente.getUsername().equalsIgnoreCase(username) && usuarioRepository.existsByUsername(username)) {
            throw new BusinessException("Ya existe un usuario llamado '" + username + "'.");
        }
        if (referenciaId != null) {
            boolean ocupada = usuarioRepository.findByRolAndReferenciaId(rol, referenciaId)
                    .filter(otro -> !otro.getId().equals(existente.getId()))
                    .isPresent();
            if (ocupada) {
                throw new BusinessException("Esa persona ya tiene una cuenta de acceso.");
            }
        }
        existente.setUsername(username);
        existente.setRol(rol);
        existente.setReferenciaId(referenciaId);
        return usuarioRepository.save(existente);
    }

    /** Atajo para crear una cuenta con contraseña en texto plano (se encripta acá). */
    @Transactional
    public Usuario crearCuenta(String username, String passwordPlano, Rol rol, Long referenciaId) {
        return save(new Usuario(username, passwordPlano, rol, referenciaId));
    }

    /** Cambia la contraseña de una cuenta; recibe texto plano y la guarda encriptada. */
    @Transactional
    public void cambiarPassword(Long id, String passwordPlano) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> noExiste("el usuario", id));
        usuario.setPassword(passwordEncoder.encode(claveValida(passwordPlano)));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void delete(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> noExiste("el usuario", id));
        usuarioRepository.delete(usuario);
    }

    /**
     * Elimina la cuenta de un vendedor o asesor, si la tiene. Lo usan
     * VendedorService y AsesorService al dar de baja a la persona, porque
     * {@code referenciaId} no tiene clave foránea que lo haga solo.
     */
    @Transactional
    public void eliminarPorReferencia(Rol rol, Long referenciaId) {
        usuarioRepository.findByRolAndReferenciaId(rol, referenciaId).ifPresent(usuarioRepository::delete);
    }

    private void validarReferencia(Rol rol, Long referenciaId) {
        if (rol == Rol.ADMINISTRADOR) {
            if (referenciaId != null) {
                throw new BusinessException("El administrador no se asocia a un vendedor ni a un asesor.");
            }
            return;
        }
        String quien = rol == Rol.VENDEDOR ? "vendedor" : "asesor";
        if (referenciaId == null) {
            throw new BusinessException("Un usuario con rol " + rol + " debe estar asociado a un " + quien + ".");
        }
        boolean existe = rol == Rol.VENDEDOR
                ? vendedorRepository.existsById(referenciaId)
                : asesorRepository.existsById(referenciaId);
        if (!existe) {
            throw new BusinessException("No existe el " + quien + " con id " + referenciaId + ".");
        }
    }

    /** La contraseña no se recorta (los espacios son válidos); solo se exige que no esté vacía ni supere el límite de BCrypt. */
    private String claveValida(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new BusinessException("La contraseña es obligatoria.");
        }
        if (clave.length() > PASSWORD_MAX) {
            throw new BusinessException("La contraseña no puede superar los " + PASSWORD_MAX + " caracteres.");
        }
        return clave;
    }
}
