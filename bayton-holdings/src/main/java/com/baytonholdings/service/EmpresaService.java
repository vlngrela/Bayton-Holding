package com.baytonholdings.service;

import com.baytonholdings.domain.Area;
import com.baytonholdings.domain.Empresa;
import com.baytonholdings.domain.Pais;
import com.baytonholdings.repository.AreaRepository;
import com.baytonholdings.repository.EmpresaRepository;
import com.baytonholdings.repository.PaisRepository;
import com.baytonholdings.service.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.baytonholdings.service.ServiceSupport.noExiste;
import static com.baytonholdings.service.ServiceSupport.noNegativo;
import static com.baytonholdings.service.ServiceSupport.obligatorio;
import static com.baytonholdings.service.ServiceSupport.sincronizar;
import static com.baytonholdings.service.ServiceSupport.textoObligatorio;

/**
 * Lógica de negocio de las empresas del holding.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El nombre es único (sin distinguir mayúsculas).</li>
 *   <li>Fecha de entrada, facturación (no negativa), ciudad sede y país sede
 *       son obligatorios. El país sede, los países de operación y las áreas
 *       deben existir.</li>
 *   <li><b>REGLA 4:</b> no se puede eliminar una empresa que tenga vendedores
 *       asociados. Las asesorías de la empresa sí se eliminan con ella (son
 *       solo vínculos, como en el sistema anterior).</li>
 * </ul>
 * {@link #findAll()} trae el país sede cargado; {@link #findById} trae además
 * países de operación y áreas. Los vendedores se obtienen con
 * {@code VendedorService.findByEmpresa}.</p>
 */
@Service
@Transactional(readOnly = true)
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final PaisRepository paisRepository;
    private final AreaRepository areaRepository;
    private final AsesoriaService asesoriaService;

    public EmpresaService(EmpresaRepository empresaRepository,
                          PaisRepository paisRepository,
                          AreaRepository areaRepository,
                          AsesoriaService asesoriaService) {
        this.empresaRepository = empresaRepository;
        this.paisRepository = paisRepository;
        this.areaRepository = areaRepository;
        this.asesoriaService = asesoriaService;
    }

    public List<Empresa> findAll() {
        return empresaRepository.findAllConPaisSede();
    }

    public Optional<Empresa> findById(Long id) {
        return empresaRepository.findConDetalleById(id);
    }

    public List<Empresa> findByPaisSede(Long paisId) {
        return empresaRepository.findByPaisSedeId(paisId);
    }

    /**
     * Alta (id nulo) o modificación (id presente). Los países de operación y
     * las áreas de la empresa recibida reemplazan a los actuales.
     */
    @Transactional
    public Empresa save(Empresa empresa) {
        String nombre = textoObligatorio(empresa.getNombre(), "nombre", 100);
        LocalDate fechaEntrada = obligatorio(empresa.getFechaEntradaHolding(), "fecha de entrada al holding");
        BigDecimal facturacion = obligatorio(empresa.getFacturacionAnual(), "facturación anual");
        noNegativo(facturacion, "facturación anual");
        facturacion = facturacion.setScale(2, RoundingMode.HALF_UP);
        String ciudadSede = textoObligatorio(empresa.getCiudadSede(), "ciudad sede", 100);

        Pais paisSede = cargarPais(empresa.getPaisSede(), "país sede");
        Set<Pais> paisesOperacion = new HashSet<>();
        for (Pais pais : empresa.getPaisesOperacion()) {
            paisesOperacion.add(cargarPais(pais, "país de operación"));
        }
        Set<Area> areas = new HashSet<>();
        for (Area area : empresa.getAreas()) {
            areas.add(cargarArea(area));
        }

        if (empresa.getId() == null) {
            if (empresaRepository.existsByNombreIgnoreCase(nombre)) {
                throw new BusinessException("Ya existe una empresa llamada '" + nombre + "'.");
            }
            Empresa nueva = new Empresa(nombre, fechaEntrada, facturacion, ciudadSede, paisSede);
            paisesOperacion.forEach(nueva::agregarPaisOperacion);
            areas.forEach(nueva::agregarArea);
            return empresaRepository.save(nueva);
        }

        Empresa existente = empresaRepository.findConDetalleById(empresa.getId())
                .orElseThrow(() -> noExiste("la empresa", empresa.getId()));
        if (!existente.getNombre().equalsIgnoreCase(nombre) && empresaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe una empresa llamada '" + nombre + "'.");
        }
        existente.setNombre(nombre);
        existente.setFechaEntradaHolding(fechaEntrada);
        existente.setFacturacionAnual(facturacion);
        existente.setCiudadSede(ciudadSede);
        existente.setPaisSede(paisSede);
        sincronizar(existente.getPaisesOperacion(), paisesOperacion,
                existente::agregarPaisOperacion, existente::quitarPaisOperacion);
        sincronizar(existente.getAreas(), areas, existente::agregarArea, existente::quitarArea);
        return empresaRepository.save(existente);
    }

    /**
     * REGLA 4: baja de una empresa. Se rechaza si tiene vendedores; para
     * eliminarla hay que dar de baja o reasignar antes a sus vendedores.
     * Sus asesorías se eliminan junto con ella.
     */
    @Transactional
    public void delete(Long id) {
        Empresa empresa = empresaRepository.findById(id).orElseThrow(() -> noExiste("la empresa", id));
        int vendedores = empresa.getNumVendedores();
        if (vendedores > 0) {
            throw new BusinessException("No se puede eliminar la empresa '" + empresa.getNombre() + "': tiene "
                    + vendedores + " vendedor(es) asociado(s). Eliminá o reasigná primero a sus vendedores.");
        }
        asesoriaService.eliminarPorEmpresa(id);
        empresaRepository.delete(empresa);
    }

    private Pais cargarPais(Pais referencia, String campo) {
        if (referencia == null || referencia.getId() == null) {
            throw new BusinessException("El campo '" + campo + "' es obligatorio.");
        }
        return paisRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("el país", referencia.getId()));
    }

    private Area cargarArea(Area referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new BusinessException("Hay un área sin seleccionar.");
        }
        return areaRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("el área", referencia.getId()));
    }
}
