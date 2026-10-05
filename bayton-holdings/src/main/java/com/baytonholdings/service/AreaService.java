package com.baytonholdings.service;

import com.baytonholdings.domain.Area;
import com.baytonholdings.domain.Asesor;
import com.baytonholdings.domain.Empresa;
import com.baytonholdings.repository.AreaRepository;
import com.baytonholdings.repository.AsesoriaRepository;
import com.baytonholdings.service.exception.BusinessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.baytonholdings.service.ServiceSupport.noExiste;
import static com.baytonholdings.service.ServiceSupport.textoObligatorio;
import static com.baytonholdings.service.ServiceSupport.textoOpcional;

/**
 * Lógica de negocio de las áreas de mercado.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El nombre es único (sin distinguir mayúsculas); la descripción es opcional.</li>
 *   <li>Al eliminar un área se la quita de las empresas y asesores que la
 *       usan y se eliminan las asesorías que la referencian (misma cascada
 *       que el sistema anterior). Para mostrar la confirmación con la
 *       cantidad de afectados, la vista usa {@link #contarDependientes}.</li>
 * </ul>
 * Las entidades devueltas no tienen asociaciones cargadas: las colecciones
 * {@code empresas} y {@code asesores} de Area son perezosas y no deben usarse
 * fuera del servicio.</p>
 */
@Service
@Transactional(readOnly = true)
public class AreaService {

    private final AreaRepository areaRepository;
    private final AsesoriaRepository asesoriaRepository;
    private final AsesoriaService asesoriaService;

    public AreaService(AreaRepository areaRepository,
                       AsesoriaRepository asesoriaRepository,
                       AsesoriaService asesoriaService) {
        this.areaRepository = areaRepository;
        this.asesoriaRepository = asesoriaRepository;
        this.asesoriaService = asesoriaService;
    }

    public List<Area> findAll() {
        return areaRepository.findAll(Sort.by("nombre"));
    }

    public Optional<Area> findById(Long id) {
        return areaRepository.findById(id);
    }

    /** Alta (id nulo) o modificación (id presente). */
    @Transactional
    public Area save(Area area) {
        String nombre = textoObligatorio(area.getNombre(), "nombre", 100);
        String descripcion = textoOpcional(area.getDescripcion(), "descripción", 500);

        if (area.getId() == null) {
            if (areaRepository.existsByNombreIgnoreCase(nombre)) {
                throw new BusinessException("Ya existe un área llamada '" + nombre + "'.");
            }
            return areaRepository.save(new Area(nombre, descripcion));
        }

        Area existente = areaRepository.findById(area.getId())
                .orElseThrow(() -> noExiste("el área", area.getId()));
        if (!existente.getNombre().equalsIgnoreCase(nombre) && areaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un área llamada '" + nombre + "'.");
        }
        existente.setNombre(nombre);
        existente.setDescripcion(descripcion);
        return areaRepository.save(existente);
    }

    /**
     * Cantidad de elementos que se verán afectados al eliminar el área:
     * empresas que la cubren + asesores que la cubren + asesorías en ella.
     * Sirve para armar el mensaje de confirmación en la vista.
     */
    public long contarDependientes(Long id) {
        Area area = areaRepository.findById(id).orElseThrow(() -> noExiste("el área", id));
        return area.getEmpresas().size() + area.getAsesores().size() + asesoriaRepository.countByAreaId(id);
    }

    /**
     * Baja en cascada: quita el área de empresas y asesores (lados
     * propietarios de las relaciones N:M), elimina sus asesorías y recién
     * entonces borra el área.
     */
    @Transactional
    public void delete(Long id) {
        Area area = areaRepository.findById(id).orElseThrow(() -> noExiste("el área", id));
        for (Empresa empresa : new ArrayList<>(area.getEmpresas())) {
            empresa.quitarArea(area);
        }
        for (Asesor asesor : new ArrayList<>(area.getAsesores())) {
            asesor.quitarArea(area);
        }
        asesoriaService.eliminarPorArea(id);
        areaRepository.delete(area);
    }
}
