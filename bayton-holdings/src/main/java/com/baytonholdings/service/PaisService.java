package com.baytonholdings.service;

import com.baytonholdings.domain.Pais;
import com.baytonholdings.repository.EmpresaRepository;
import com.baytonholdings.repository.PaisRepository;
import com.baytonholdings.service.exception.BusinessException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.baytonholdings.service.ServiceSupport.noExiste;
import static com.baytonholdings.service.ServiceSupport.noNegativo;
import static com.baytonholdings.service.ServiceSupport.obligatorio;
import static com.baytonholdings.service.ServiceSupport.textoObligatorio;

/**
 * Lógica de negocio de los países.
 *
 * <p>Reglas:
 * <ul>
 *   <li>El nombre es único (sin distinguir mayúsculas).</li>
 *   <li>PIB y número de habitantes no pueden ser negativos.</li>
 *   <li>No se puede eliminar un país que sea sede de una empresa o donde
 *       alguna empresa opere: se bloquea la baja en lugar de borrar en
 *       cascada empresas, vendedores y asesorías.</li>
 * </ul>
 * Las entidades devueltas no tienen asociaciones cargadas (Pais no tiene colecciones).</p>
 */
@Service
@Transactional(readOnly = true)
public class PaisService {

    private final PaisRepository paisRepository;
    private final EmpresaRepository empresaRepository;

    public PaisService(PaisRepository paisRepository, EmpresaRepository empresaRepository) {
        this.paisRepository = paisRepository;
        this.empresaRepository = empresaRepository;
    }

    public List<Pais> findAll() {
        return paisRepository.findAll(Sort.by("nombre"));
    }

    public Optional<Pais> findById(Long id) {
        return paisRepository.findById(id);
    }

    /** Alta (id nulo) o modificación (id presente). */
    @Transactional
    public Pais save(Pais pais) {
        String nombre = textoObligatorio(pais.getNombre(), "nombre", 100);
        String capital = textoObligatorio(pais.getCapital(), "capital", 100);
        Double pib = obligatorio(pais.getPib(), "PIB");
        Long habitantes = obligatorio(pais.getNumHabitantes(), "número de habitantes");
        noNegativo(pib, "PIB");
        noNegativo(habitantes, "número de habitantes");

        if (pais.getId() == null) {
            if (paisRepository.existsByNombreIgnoreCase(nombre)) {
                throw new BusinessException("Ya existe un país llamado '" + nombre + "'.");
            }
            return paisRepository.save(new Pais(nombre, pib, habitantes, capital));
        }

        Pais existente = paisRepository.findById(pais.getId())
                .orElseThrow(() -> noExiste("el país", pais.getId()));
        // Solo se chequea duplicado si el nombre cambió más allá de mayúsculas/minúsculas.
        if (!existente.getNombre().equalsIgnoreCase(nombre) && paisRepository.existsByNombreIgnoreCase(nombre)) {
            throw new BusinessException("Ya existe un país llamado '" + nombre + "'.");
        }
        existente.setNombre(nombre);
        existente.setCapital(capital);
        existente.setPib(pib);
        existente.setNumHabitantes(habitantes);
        return paisRepository.save(existente);
    }

    /**
     * Baja de un país. Se rechaza si alguna empresa lo usa como sede o como
     * país de operación.
     */
    @Transactional
    public void delete(Long id) {
        Pais pais = paisRepository.findById(id).orElseThrow(() -> noExiste("el país", id));
        if (empresaRepository.existsByPaisSedeId(id)) {
            throw new BusinessException("No se puede eliminar el país '" + pais.getNombre()
                    + "': es la sede de al menos una empresa.");
        }
        if (empresaRepository.existsByPaisesOperacionId(id)) {
            throw new BusinessException("No se puede eliminar el país '" + pais.getNombre()
                    + "': hay empresas que operan en él.");
        }
        paisRepository.delete(pais);
    }
}
