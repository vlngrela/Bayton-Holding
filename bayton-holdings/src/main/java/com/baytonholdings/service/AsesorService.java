package com.baytonholdings.service;

import com.baytonholdings.domain.Area;
import com.baytonholdings.domain.Asesor;
import com.baytonholdings.domain.Asesoria;
import com.baytonholdings.domain.Rol;
import com.baytonholdings.repository.AreaRepository;
import com.baytonholdings.repository.AsesorRepository;
import com.baytonholdings.service.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.baytonholdings.service.ServiceSupport.noExiste;
import static com.baytonholdings.service.ServiceSupport.sincronizar;
import static com.baytonholdings.service.ServiceSupport.textoObligatorio;

/**
 * Lógica de negocio de los asesores.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El código es único y no se puede modificar, porque es también el
 *       nombre de usuario de su cuenta.</li>
 *   <li>Al crear un asesor se crea su cuenta de acceso (usuario = código,
 *       contraseña inicial = código, guardada con BCrypt).</li>
 *   <li>No se puede quitar de un asesor un área en la que tiene una
 *       asesoría: el asesor debe cubrir todas las áreas de sus asesorías.</li>
 *   <li>Al eliminar un asesor se eliminan sus asesorías (cascada de la
 *       entidad) y su cuenta de acceso.</li>
 * </ul>
 * Las entidades devueltas traen las áreas cargadas. Las asesorías se
 * consultan con {@code AsesoriaService.findByAsesor}.</p>
 */
@Service
@Transactional(readOnly = true)
public class AsesorService {

    private static final int CODIGO_MAX = 30;

    private final AsesorRepository asesorRepository;
    private final AreaRepository areaRepository;
    private final UsuarioService usuarioService;

    public AsesorService(AsesorRepository asesorRepository,
                         AreaRepository areaRepository,
                         UsuarioService usuarioService) {
        this.asesorRepository = asesorRepository;
        this.areaRepository = areaRepository;
        this.usuarioService = usuarioService;
    }

    public List<Asesor> findAll() {
        return asesorRepository.findAllConAreas();
    }

    public Optional<Asesor> findById(Long id) {
        return asesorRepository.findConAreasById(id);
    }

    /** Cantidad de asesorías del asesor, para el mensaje de confirmación de baja. */
    public int contarAsesorias(Long id) {
        Asesor asesor = asesorRepository.findById(id).orElseThrow(() -> noExiste("el asesor", id));
        return asesor.getAsesorias().size();
    }

    /**
     * Alta (id nulo) o modificación (id presente). Las áreas del asesor
     * recibido reemplazan a las actuales.
     */
    @Transactional
    public Asesor save(Asesor asesor) {
        String codigo = textoObligatorio(asesor.getCodigo(), "código", CODIGO_MAX);
        String nombre = textoObligatorio(asesor.getNombre(), "nombre", 100);
        String direccion = textoObligatorio(asesor.getDireccion(), "dirección", 200);
        String titulacion = textoObligatorio(asesor.getTitulacion(), "titulación", 100);

        Set<Area> areas = new HashSet<>();
        for (Area area : asesor.getAreas()) {
            areas.add(cargarArea(area));
        }

        if (asesor.getId() == null) {
            if (asesorRepository.existsByCodigoIgnoreCase(codigo)) {
                throw new BusinessException("Ya existe un asesor con el código '" + codigo + "'.");
            }
            // El código será el usuario: debe estar libre también frente a vendedores y administrador.
            if (usuarioService.existsByUsername(codigo)) {
                throw new BusinessException("El código '" + codigo + "' ya está en uso como nombre de usuario.");
            }
            Asesor nuevo = new Asesor(codigo, nombre, direccion, titulacion);
            areas.forEach(nuevo::agregarArea);
            Asesor guardado = asesorRepository.save(nuevo);
            usuarioService.crearCuenta(codigo, codigo, Rol.ASESOR, guardado.getId());
            return guardado;
        }

        Asesor existente = asesorRepository.findConAreasById(asesor.getId())
                .orElseThrow(() -> noExiste("el asesor", asesor.getId()));
        if (!existente.getCodigo().equalsIgnoreCase(codigo)) {
            throw new BusinessException("El código de un asesor no se puede modificar: es su nombre de usuario.");
        }
        // No se puede dejar de cubrir un área en la que hay una asesoría.
        for (Asesoria asesoria : existente.getAsesorias()) {
            if (!areas.contains(asesoria.getArea())) {
                throw new BusinessException("No se puede quitar el área '" + asesoria.getArea().getNombre()
                        + "': el asesor tiene una asesoría en ella. Eliminá antes esa asesoría.");
            }
        }
        existente.setNombre(nombre);
        existente.setDireccion(direccion);
        existente.setTitulacion(titulacion);
        sincronizar(existente.getAreas(), areas, existente::agregarArea, existente::quitarArea);
        return asesorRepository.save(existente);
    }

    /**
     * Baja de un asesor: se elimina su cuenta de acceso y luego el asesor,
     * cuyas asesorías se borran en cascada. Las filas de la tabla
     * {@code asesor_area} las limpia Hibernate (el asesor es el lado propietario).
     */
    @Transactional
    public void delete(Long id) {
        Asesor asesor = asesorRepository.findById(id).orElseThrow(() -> noExiste("el asesor", id));
        usuarioService.eliminarPorReferencia(Rol.ASESOR, id);
        asesorRepository.delete(asesor);
    }

    private Area cargarArea(Area referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new BusinessException("Hay un área sin seleccionar.");
        }
        return areaRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("el área", referencia.getId()));
    }
}
