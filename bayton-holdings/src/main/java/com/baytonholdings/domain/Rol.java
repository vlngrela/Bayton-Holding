package com.baytonholdings.domain;

/**
 * Roles posibles de un usuario del sistema.
 * <ul>
 *   <li>ADMINISTRADOR: CRUD total sobre todas las entidades.</li>
 *   <li>VENDEDOR: solo consulta sus propios datos.</li>
 *   <li>ASESOR: solo consulta sus propios datos.</li>
 * </ul>
 */
public enum Rol {
    ADMINISTRADOR,
    VENDEDOR,
    ASESOR
}
