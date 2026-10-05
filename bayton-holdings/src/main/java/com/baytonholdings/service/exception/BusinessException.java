package com.baytonholdings.service.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio o recibe datos
 * inválidos (duplicados, referencias inexistentes, bajas no permitidas, etc.).
 *
 * <p>Es una excepción <b>no verificada</b> (RuntimeException) a propósito:
 * Spring revierte la transacción automáticamente cuando un método
 * {@code @Transactional} lanza una excepción de este tipo, así que una
 * operación que falla a mitad de camino no deja datos a medias.</p>
 *
 * <p>El mensaje está redactado para mostrarse tal cual al usuario en la
 * interfaz (por ejemplo, en una notificación de Vaadin).</p>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
