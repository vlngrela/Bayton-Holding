package com.baytonholdings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Base abstracta de las personas que se relacionan con el holding
 * ({@link Vendedor} y {@link Asesor}).
 *
 * <p>No es una entidad por sí misma: al ser {@code @MappedSuperclass}, sus
 * columnas se replican en la tabla de cada subclase (cada una con su propia
 * secuencia de ids). Por eso no se puede consultar "todas las Personas" de
 * forma polimórfica; el polimorfismo se aprovecha en memoria, vía
 * {@link #getDescripcionRol()}.</p>
 */
@MappedSuperclass
public abstract class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código de negocio, único dentro de la tabla de cada subclase. */
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String direccion;

    /** Constructor vacío requerido por JPA. */
    protected Persona() {
    }

    protected Persona(String codigo, String nombre, String direccion) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
    }

    /**
     * Descripción legible del rol que cumple esta persona en el holding.
     * Cada subclase concreta la implementa (polimorfismo).
     */
    public abstract String getDescripcionRol();

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
