package com.baytonholdings.service;

import com.baytonholdings.service.exception.BusinessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Utilidades de validación y sincronización compartidas por los servicios.
 * Es privada del paquete: las vistas no la ven ni la necesitan.
 */
final class ServiceSupport {

    private ServiceSupport() {
    }

    /** Texto obligatorio: no nulo, no vacío y de hasta {@code maximo} caracteres. Devuelve el texto sin espacios sobrantes. */
    static String textoObligatorio(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("El campo '" + campo + "' es obligatorio.");
        }
        return controlarLargo(valor.trim(), campo, maximo);
    }

    /** Texto opcional: devuelve null si viene vacío; si no, lo devuelve limpio y con el largo controlado. */
    static String textoOpcional(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return controlarLargo(valor.trim(), campo, maximo);
    }

    private static String controlarLargo(String limpio, String campo, int maximo) {
        if (limpio.length() > maximo) {
            throw new BusinessException(
                    "El campo '" + campo + "' no puede superar los " + maximo + " caracteres.");
        }
        return limpio;
    }

    static <T> T obligatorio(T valor, String campo) {
        if (valor == null) {
            throw new BusinessException("El campo '" + campo + "' es obligatorio.");
        }
        return valor;
    }

    /** Rechaza números negativos (y NaN). Acepta BigDecimal, Double, Long, etc. */
    static void noNegativo(Number valor, String campo) {
        boolean invalido;
        if (valor instanceof BigDecimal decimal) {
            invalido = decimal.signum() < 0;
        } else {
            invalido = valor.doubleValue() < 0 || Double.isNaN(valor.doubleValue());
        }
        if (invalido) {
            throw new BusinessException("El campo '" + campo + "' no puede ser negativo.");
        }
    }

    static BusinessException noExiste(String descripcion, Object id) {
        return new BusinessException("No existe " + descripcion + " con id " + id + ".");
    }

    /**
     * Deja la colección {@code actuales} igual a {@code deseados}, quitando
     * lo que sobra y agregando lo que falta a través de los métodos de la
     * entidad (que mantienen sincronizados ambos lados de la relación).
     * {@code actuales} puede ser una vista de solo lectura de la colección viva.
     */
    static <T> void sincronizar(Set<T> actuales, Set<T> deseados, Consumer<T> agregar, Consumer<T> quitar) {
        for (T elemento : new ArrayList<>(actuales)) {
            if (!deseados.contains(elemento)) {
                quitar.accept(elemento);
            }
        }
        for (T elemento : deseados) {
            if (!actuales.contains(elemento)) {
                agregar.accept(elemento);
            }
        }
    }
}
