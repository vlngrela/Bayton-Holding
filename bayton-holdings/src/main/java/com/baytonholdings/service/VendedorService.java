package com.baytonholdings.service;

import com.baytonholdings.domain.Empresa;
import com.baytonholdings.domain.Rol;
import com.baytonholdings.domain.Vendedor;
import com.baytonholdings.repository.EmpresaRepository;
import com.baytonholdings.repository.VendedorRepository;
import com.baytonholdings.service.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.baytonholdings.service.ServiceSupport.noExiste;
import static com.baytonholdings.service.ServiceSupport.textoObligatorio;

/**
 * Lógica de negocio de los vendedores y de la estructura piramidal de captación.
 *
 * <p>Reglas:
 * <ul>
 *   <li><b>REGLA 1:</b> un vendedor solo puede captar a otro de su misma
 *       empresa y la captación registra su fecha. También se rechaza la
 *       autocaptación y los ciclos. Las comprobaciones viven en
 *       {@code Vendedor.captar}; este servicio las aplica en todos los
 *       caminos (alta, modificación y {@link #captar}) y traduce sus errores
 *       a {@link BusinessException}.</li>
 *   <li><b>REGLA 3:</b> al crear un vendedor sin captador, su
 *       {@code fechaCaptacion} es la fecha actual (salvo que se informe otra).</li>
 *   <li>El código es único y no se puede modificar, porque es también el
 *       nombre de usuario de su cuenta.</li>
 *   <li>Al crear un vendedor se crea su cuenta de acceso (usuario = código,
 *       contraseña inicial = código, guardada con BCrypt).</li>
 *   <li>No se puede cambiar de empresa a un vendedor que tiene captados.</li>
 *   <li>Al eliminar un vendedor, sus captados quedan sin captador (no se
 *       borran, conservan su fecha) y se elimina su cuenta.</li>
 * </ul>
 * Las entidades devueltas traen empresa y captador cargados;
 * {@link #findById} trae además los captados.</p>
 */
@Service
@Transactional(readOnly = true)
public class VendedorService {

    private static final int CODIGO_MAX = 30;

    private final VendedorRepository vendedorRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioService usuarioService;

    public VendedorService(VendedorRepository vendedorRepository,
                           EmpresaRepository empresaRepository,
                           UsuarioService usuarioService) {
        this.vendedorRepository = vendedorRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioService = usuarioService;
    }

    public List<Vendedor> findAll() {
        return vendedorRepository.findAllConDetalle();
    }

    public Optional<Vendedor> findById(Long id) {
        return vendedorRepository.findConDetalleById(id);
    }

    public List<Vendedor> findByEmpresa(Long empresaId) {
        return vendedorRepository.findByEmpresaId(empresaId);
    }

    /** Vendedores captados directamente por el vendedor indicado. */
    public List<Vendedor> findCaptados(Long captadorId) {
        return vendedorRepository.findByCaptadorId(captadorId);
    }

    /**
     * Vendedores que se pueden ofrecer como captador (para el combo de la
     * vista): los de la empresa indicada, sin el propio vendedor ni sus
     * descendientes en la pirámide, que formarían un ciclo.
     *
     * @param excluirVendedorId vendedor que se está editando; null si es un alta
     */
    public List<Vendedor> findCandidatosCaptador(Long empresaId, Long excluirVendedorId) {
        List<Vendedor> deLaEmpresa = vendedorRepository.findByEmpresaId(empresaId);
        if (excluirVendedorId == null) {
            return deLaEmpresa;
        }
        Set<Long> excluidos = new HashSet<>();
        excluidos.add(excluirVendedorId);
        boolean agregoAlguno = true;
        while (agregoAlguno) { // recorre los descendientes hasta que no aparezcan más
            agregoAlguno = false;
            for (Vendedor vendedor : deLaEmpresa) {
                Vendedor captador = vendedor.getCaptador();
                if (captador != null && excluidos.contains(captador.getId()) && excluidos.add(vendedor.getId())) {
                    agregoAlguno = true;
                }
            }
        }
        return deLaEmpresa.stream().filter(v -> !excluidos.contains(v.getId())).toList();
    }

    /** Alta (id nulo) o modificación (id presente). */
    @Transactional
    public Vendedor save(Vendedor vendedor) {
        return vendedor.getId() == null ? crear(vendedor) : actualizar(vendedor);
    }

    /**
     * REGLA 1: registra que {@code captadorId} captó a {@code captadoId}.
     * Falla con BusinessException si no son de la misma empresa, si es el
     * mismo vendedor o si se formaría un ciclo. Si la fecha es nula se usa la actual.
     */
    @Transactional
    public Vendedor captar(Long captadorId, Long captadoId, LocalDate fecha) {
        Vendedor captador = vendedorRepository.findById(captadorId)
                .orElseThrow(() -> noExiste("el vendedor captador", captadorId));
        Vendedor captado = vendedorRepository.findById(captadoId)
                .orElseThrow(() -> noExiste("el vendedor captado", captadoId));
        LocalDate fechaCaptacion = fecha != null ? fecha : LocalDate.now();
        aplicarRegla(() -> captador.captar(captado, fechaCaptacion));
        return vendedorRepository.save(captado);
    }

    /** Desvincula al vendedor de su captador (conserva la fecha de captación). */
    @Transactional
    public Vendedor quitarCaptador(Long vendedorId) {
        Vendedor vendedor = vendedorRepository.findById(vendedorId)
                .orElseThrow(() -> noExiste("el vendedor", vendedorId));
        vendedor.quitarCaptador();
        return vendedorRepository.save(vendedor);
    }

    /**
     * Baja de un vendedor: sus captados quedan sin captador, se elimina su
     * cuenta de acceso y luego el vendedor. Los captados se actualizan antes
     * del borrado, así que no se viola la clave foránea de captación.
     */
    @Transactional
    public void delete(Long id) {
        Vendedor vendedor = vendedorRepository.findById(id).orElseThrow(() -> noExiste("el vendedor", id));
        for (Vendedor captado : new ArrayList<>(vendedor.getCaptados())) {
            captado.quitarCaptador();
        }
        usuarioService.eliminarPorReferencia(Rol.VENDEDOR, id);
        vendedorRepository.delete(vendedor);
    }

    // ------------------------------------------------------------------ alta

    private Vendedor crear(Vendedor datos) {
        String codigo = textoObligatorio(datos.getCodigo(), "código", CODIGO_MAX);
        String nombre = textoObligatorio(datos.getNombre(), "nombre", 100);
        String direccion = textoObligatorio(datos.getDireccion(), "dirección", 200);
        Empresa empresa = cargarEmpresa(datos.getEmpresa());
        Vendedor captador = cargarCaptador(datos.getCaptador());

        if (vendedorRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("Ya existe un vendedor con el código '" + codigo + "'.");
        }
        // El código será el usuario: debe estar libre también frente a asesores y administrador.
        if (usuarioService.existsByUsername(codigo)) {
            throw new BusinessException("El código '" + codigo + "' ya está en uso como nombre de usuario.");
        }

        Vendedor nuevo = new Vendedor(codigo, nombre, direccion, empresa);
        if (captador == null) {
            // REGLA 3: sin captador, la fecha de captación es la fecha actual.
            nuevo.setFechaCaptacion(datos.getFechaCaptacion() != null ? datos.getFechaCaptacion() : LocalDate.now());
        } else {
            // REGLA 1: se valida (misma empresa, sin ciclos) ANTES de guardar y se registra la fecha.
            LocalDate fecha = datos.getFechaCaptacion() != null ? datos.getFechaCaptacion() : LocalDate.now();
            aplicarRegla(() -> captador.captar(nuevo, fecha));
        }

        Vendedor guardado = vendedorRepository.save(nuevo);
        usuarioService.crearCuenta(codigo, codigo, Rol.VENDEDOR, guardado.getId());
        return guardado;
    }

    // ---------------------------------------------------------- modificación

    private Vendedor actualizar(Vendedor datos) {
        Vendedor vendedor = vendedorRepository.findConDetalleById(datos.getId())
                .orElseThrow(() -> noExiste("el vendedor", datos.getId()));

        String codigo = textoObligatorio(datos.getCodigo(), "código", CODIGO_MAX);
        if (!vendedor.getCodigo().equalsIgnoreCase(codigo)) {
            throw new BusinessException("El código de un vendedor no se puede modificar: es su nombre de usuario.");
        }
        vendedor.setNombre(textoObligatorio(datos.getNombre(), "nombre", 100));
        vendedor.setDireccion(textoObligatorio(datos.getDireccion(), "dirección", 200));

        Empresa empresa = cargarEmpresa(datos.getEmpresa());
        boolean cambioEmpresa = !empresa.equals(vendedor.getEmpresa());
        if (cambioEmpresa && !vendedor.getCaptados().isEmpty()) {
            throw new BusinessException("No se puede cambiar de empresa a un vendedor que tiene captados: "
                    + "sus captados deben pertenecer a la misma empresa. Quitales primero el captador.");
        }
        vendedor.setEmpresa(empresa);

        Vendedor captador = cargarCaptador(datos.getCaptador());
        if (captador == null) {
            if (vendedor.getCaptador() != null) {
                vendedor.quitarCaptador();
            }
            if (datos.getFechaCaptacion() != null) {
                vendedor.setFechaCaptacion(datos.getFechaCaptacion());
            }
        } else if (Objects.equals(captador, vendedor.getCaptador()) && !cambioEmpresa) {
            // Mismo captador y misma empresa: la relación no cambia, solo se admite corregir la fecha.
            if (datos.getFechaCaptacion() != null) {
                vendedor.setFechaCaptacion(datos.getFechaCaptacion());
            }
        } else {
            // REGLA 1: captador nuevo (o empresa nueva): se revalida misma empresa y ciclos.
            LocalDate fecha = datos.getFechaCaptacion() != null ? datos.getFechaCaptacion() : LocalDate.now();
            aplicarRegla(() -> captador.captar(vendedor, fecha));
        }
        return vendedorRepository.save(vendedor);
    }

    // --------------------------------------------------------------- helpers

    /** Ejecuta una regla del dominio y traduce sus IllegalArgument/IllegalState a BusinessException. */
    private void aplicarRegla(Runnable regla) {
        try {
            regla.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new BusinessException(e.getMessage(), e);
        }
    }

    private Empresa cargarEmpresa(Empresa referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new BusinessException("La empresa es obligatoria.");
        }
        return empresaRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("la empresa", referencia.getId()));
    }

    /** Devuelve el captador como entidad gestionada, o null si no se indicó ninguno. */
    private Vendedor cargarCaptador(Vendedor referencia) {
        if (referencia == null || referencia.getId() == null) {
            return null;
        }
        return vendedorRepository.findById(referencia.getId())
                .orElseThrow(() -> noExiste("el vendedor captador", referencia.getId()));
    }
}
