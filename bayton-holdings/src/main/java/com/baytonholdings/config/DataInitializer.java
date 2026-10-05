package com.baytonholdings.config;

import com.baytonholdings.domain.Asesor;
import com.baytonholdings.domain.Empresa;
import com.baytonholdings.domain.Pais;
import com.baytonholdings.domain.Rol;
import com.baytonholdings.domain.Usuario;
import com.baytonholdings.domain.Vendedor;
import com.baytonholdings.repository.AsesorRepository;
import com.baytonholdings.repository.EmpresaRepository;
import com.baytonholdings.repository.PaisRepository;
import com.baytonholdings.repository.UsuarioRepository;
import com.baytonholdings.repository.VendedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga inicial: crea los tres usuarios de prueba cuando la tabla
 * {@code usuario} está vacía. Se ejecuta una vez al arrancar la aplicación y
 * es idempotente: si ya hay usuarios, no toca nada.
 *
 * <ul>
 *   <li>{@code admin / admin123}: ADMINISTRADOR (sin persona asociada).</li>
 *   <li>{@code vendedor / vendedor123}: VENDEDOR, asociado al vendedor {@code V001}.</li>
 *   <li>{@code asesor / asesor123}: ASESOR, asociado al asesor {@code A001}.</li>
 * </ul>
 * Las contraseñas se guardan con BCrypt (el {@link PasswordEncoder} lo define
 * {@code PasswordEncoderConfig}).
 *
 * <p><b>Solo para desarrollo:</b> son credenciales conocidas y débiles. Cambiarlas
 * o desactivar este componente con {@code bayton.datos-iniciales.habilitado=false}
 * antes de cualquier uso real.</p>
 *
 * <p>Un usuario VENDEDOR o ASESOR debe apuntar a una persona real. Por eso, si
 * todavía no existen, se crean el vendedor {@code V001} y el asesor {@code A001}
 * junto con el país y la empresa mínimos que el vendedor necesita. Son los mismos
 * registros que carga {@code db/data.sql} (se identifican por nombre o código, no
 * por id), de modo que ese script puede ejecutarse antes o después sin duplicar
 * nada. Si hay que cambiar estos datos, hay que cambiarlos también en {@code data.sql}.</p>
 *
 * <p>Usa los repositorios directamente (y no los servicios) a propósito: es una
 * carga de infraestructura, y {@code VendedorService}/{@code AsesorService} crean
 * por su cuenta una cuenta con el código como usuario, que acá no se quiere.</p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PAIS = "Argentina";
    private static final String EMPRESA = "Bayton Tech S.A.";
    private static final String CODIGO_VENDEDOR = "V001";
    private static final String CODIGO_ASESOR = "A001";

    private final PaisRepository paisRepository;
    private final EmpresaRepository empresaRepository;
    private final VendedorRepository vendedorRepository;
    private final AsesorRepository asesorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean habilitado;

    public DataInitializer(PaisRepository paisRepository,
                           EmpresaRepository empresaRepository,
                           VendedorRepository vendedorRepository,
                           AsesorRepository asesorRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${bayton.datos-iniciales.habilitado:true}") boolean habilitado) {
        this.paisRepository = paisRepository;
        this.empresaRepository = empresaRepository;
        this.vendedorRepository = vendedorRepository;
        this.asesorRepository = asesorRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.habilitado = habilitado;
    }

    /** Todo ocurre en una sola transacción: o se crea el conjunto completo o no se crea nada. */
    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado) {
            log.info("DataInitializer desactivado (bayton.datos-iniciales.habilitado=false).");
            return;
        }
        if (usuarioRepository.count() > 0) {
            log.info("La tabla usuario ya tiene datos: no se crean los usuarios iniciales.");
            return;
        }

        Vendedor vendedor = obtenerOCrearVendedor();
        Asesor asesor = obtenerOCrearAsesor();

        crearUsuario("admin", "admin123", Rol.ADMINISTRADOR, null);
        crearUsuario("vendedor", "vendedor123", Rol.VENDEDOR, vendedor.getId());
        crearUsuario("asesor", "asesor123", Rol.ASESOR, asesor.getId());

        log.info("Usuarios iniciales creados: admin (ADMINISTRADOR), vendedor (VENDEDOR -> {}), asesor (ASESOR -> {}).",
                CODIGO_VENDEDOR, CODIGO_ASESOR);
    }

    private void crearUsuario(String username, String passwordPlano, Rol rol, Long referenciaId) {
        usuarioRepository.save(new Usuario(username, passwordEncoder.encode(passwordPlano), rol, referenciaId));
    }

    /** Vendedor V001 (el que ya exista, o uno nuevo con su empresa y país). */
    private Vendedor obtenerOCrearVendedor() {
        return vendedorRepository.findAll().stream()
                .filter(v -> CODIGO_VENDEDOR.equalsIgnoreCase(v.getCodigo()))
                .findFirst()
                .orElseGet(() -> {
                    Vendedor nuevo = new Vendedor(CODIGO_VENDEDOR, "Carlos Méndez",
                            "Av. Corrientes 1234, Buenos Aires", obtenerOCrearEmpresa());
                    nuevo.setFechaCaptacion(LocalDate.of(2024, 1, 15));
                    return vendedorRepository.save(nuevo);
                });
    }

    private Empresa obtenerOCrearEmpresa() {
        return empresaRepository.findAll().stream()
                .filter(e -> EMPRESA.equalsIgnoreCase(e.getNombre()))
                .findFirst()
                .orElseGet(() -> empresaRepository.save(new Empresa(EMPRESA, LocalDate.of(2019, 3, 15),
                        new BigDecimal("12500000.00"), "Buenos Aires", obtenerOCrearPais())));
    }

    private Pais obtenerOCrearPais() {
        return paisRepository.findAll().stream()
                .filter(p -> PAIS.equalsIgnoreCase(p.getNombre()))
                .findFirst()
                .orElseGet(() -> paisRepository.save(new Pais(PAIS, 640.0, 46_000_000L, "Buenos Aires")));
    }

    /** Asesor A001 (el que ya exista, o uno nuevo; sus áreas y asesoría las completa data.sql). */
    private Asesor obtenerOCrearAsesor() {
        return asesorRepository.findAll().stream()
                .filter(a -> CODIGO_ASESOR.equalsIgnoreCase(a.getCodigo()))
                .findFirst()
                .orElseGet(() -> asesorRepository.save(new Asesor(CODIGO_ASESOR, "Lucía Fernández",
                        "Calle Florida 567, Buenos Aires", "Ingeniera en Sistemas")));
    }
}
