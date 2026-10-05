package com.baytonholdings.service;

import com.baytonholdings.domain.Area;
import com.baytonholdings.domain.Asesor;
import com.baytonholdings.domain.Asesoria;
import com.baytonholdings.domain.Empresa;
import com.baytonholdings.repository.AreaRepository;
import com.baytonholdings.repository.AsesorRepository;
import com.baytonholdings.repository.AsesoriaRepository;
import com.baytonholdings.repository.EmpresaRepository;
import com.baytonholdings.service.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.baytonholdings.service.ServiceSupport.noExiste;
import static com.baytonholdings.service.ServiceSupport.obligatorio;

/**
 * Lógica de negocio de las asesorías (Asesor - Empresa - Área).
 *
 * <p>Reglas:
 * <ul>
 *   <li><b>REGLA 2:</b> la combinación (asesor, empresa, área) es única. Se
 *       valida acá con un mensaje claro; la restricción única de la tabla
 *       queda como respaldo ante accesos concurrentes.</li>
 *   <li>La fecha de inicio es obligatoria.</li>
 *   <li>Al crear una asesoría, el área se suma a las áreas que cubre el
 *       asesor (lo hace {@code Asesor.agregarAsesoria}).</li>
 *   <li>El asesor de una asesoría no se puede cambiar: se elimina y se crea otra.</li>
 * </ul>
 * Las entidades devueltas traen asesor, empresa y área ya cargados.</p>
 */
@Service
@Transactional(readOnly = true)
public class AsesoriaService {

    private final AsesoriaRepository asesoriaRepository;
    private final AsesorRepository asesorRepository;
    private final EmpresaRepository empresaRepository;
    private final AreaRepository areaRepository;

    public AsesoriaService(AsesoriaRepository asesoriaRepository,
                           AsesorRepository asesorRepository,
                           EmpresaRepository empresaRepository,
                           AreaRepository areaRepository) {
        this.asesoriaRepository = asesoriaRepository;
        this.asesorRepository = asesorRepository;
        this.empresaRepository = empresaRepository;
        this.areaRepository = areaRepository;
    }

    public List<Asesoria> findAll() {
        return asesoriaRepository.findAllConDetalle();
    }

    public Optional<Asesoria> findById(Long id) {
        return asesoriaRepository.findConDetalleById(id);
    }

    public List<Asesoria> findByAsesor(Long asesorId) {
        return asesoriaRepository.findByAsesorId(asesorId);
    }

    public List<Asesoria> findByEmpresa(Long empresaId) {
        return asesoriaRepository.findByEmpresaId(empresaId);
    }

    public boolean existeCombinacion(Long asesorId, Long empresaId, Long areaId) {
        return asesoriaRepository.existsByAsesorIdAndEmpresaIdAndAreaId(asesorId, empresaId, areaId);
    }

    /** Alta (id nulo) o modificación (id presente) de una asesoría. */
    @Transactional
    public Asesoria save(Asesoria asesoria) {
        LocalDate fechaInicio = obligatorio(asesoria.getFechaInicio(), "fecha de inicio");
        Asesor asesor = cargarAsesor(asesoria.getAsesor());
        Empresa empresa = cargarEmpresa(asesoria.getEmpresa());
        Area area = cargarArea(asesoria.getArea());

        if (asesoria.getId() == null) {
            // REGLA 2: una sola asesoría por (asesor, empresa, área).
            if (asesoriaRepository.existsByAsesorIdAndEmpresaIdAndAreaId(
                    asesor.getId(), empresa.getId(), area.getId())) {
                throw combinacionDuplicada(asesor, empresa, area);
            }
            Asesoria nueva = new Asesoria(asesor, empresa, area, fechaInicio);
            asesor.agregarAsesoria(nueva); // sincroniza ambos lados y suma el área al asesor
            return asesoriaRepository.save(nueva);
        }

        Asesoria existente = asesoriaRepository.findConDetalleById(asesoria.getId())
                .orElseThrow(() -> noExiste("la asesoría", asesoria.getId()));
        if (!existente.getAsesor().equals(asesor)) {
            throw new BusinessException(
                    "No se puede cambiar el asesor de una asesoría: eliminala y creá una nueva.");
        }
        // REGLA 2 también al modificar: solo se revalida si la combinación cambió.
        boolean cambioCombinacion = !existente.getEmpresa().equals(empresa) || !existente.getArea().equals(area);
        if (cambioCombinacion && asesoriaRepository.existsByAsesorIdAndEmpresaIdAndAreaId(
                asesor.getId(), empresa.getId(), area.getId())) {
            throw combinacionDuplicada(asesor, empresa, area);
        }
        existente.setEmpresa(empresa);
        existente.setArea(area);
        existente.setFechaInicio(fechaInicio);
        asesor.agregarArea(area); // el asesor cubre el área de todas sus asesorías
        return asesoriaRepository.save(existente);
    }

    @Transactional
    public void delete(Long id) {
        Asesoria asesoria = asesoriaRepository.findConDetalleById(id)
                .orElseThrow(() -> noExiste("la asesoría", id));
        eliminar(asesoria);
    }

    /** Elimina todas las asesorías de una empresa. Lo usa EmpresaService al dar de baja la empresa. */
    @Transactional
    public void eliminarPorEmpresa(Long empresaId) {
        asesoriaRepository.findByEmpresaId(empresaId).forEach(this::eliminar);
    }

    /** Elimina todas las asesorías de un área. Lo usa AreaService al dar de baja el área. */
    @Transactional
    public void eliminarPorArea(Long areaId) {
        asesoriaRepository.findByAreaId(areaId).forEach(this::eliminar);
    }

    /**
     * La quita de la colección del asesor (orphanRemoval) y además la borra
     * explícitamente, para no depender solo de orphanRemoval y para que la
     * colección en memoria no quede con una asesoría eliminada.
     */
    private void eliminar(Asesoria asesoria) {
        asesoria.getAsesor().quitarAsesoria(asesoria);
        asesoriaRepository.delete(asesoria);
    }

    private BusinessException combinacionDuplicada(Asesor asesor, Empresa empresa, Area area) {
        return new BusinessException("El asesor '" + asesor.getNombre() + "' ya tiene una asesoría con la empresa '"
                + empresa.getNombre() + "' en el área '" + area.getNombre() + "'.");
    }

    private Asesor cargarAsesor(Asesor referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new BusinessException("El asesor es obligatorio.");
        }
        return asesorRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("el asesor", referencia.getId()));
    }

    private Empresa cargarEmpresa(Empresa referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new BusinessException("La empresa es obligatoria.");
        }
        return empresaRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("la empresa", referencia.getId()));
    }

    private Area cargarArea(Area referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new BusinessException("El área es obligatoria.");
        }
        return areaRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("el área", referencia.getId()));
    }
}
