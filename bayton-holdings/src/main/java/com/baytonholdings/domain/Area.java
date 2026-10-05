package com.baytonholdings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Área de mercado que puede ser cubierta por una o más empresas del holding y
 * en la que uno o más asesores pueden brindar soporte.
 *
 * <p>Las dos colecciones son el lado inverso (no propietario) de las
 * relaciones N:M: son de solo lectura y reflejan lo que está en base. Para
 * agregar o quitar un área hay que operar sobre {@link Empresa} o
 * {@link Asesor}, que son los lados propietarios. Al eliminar un área, el
 * servicio debe quitarla primero de esas colecciones y borrar las
 * {@link Asesoria} que la referencian.</p>
 */
@Entity
@Table(name = "area")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** N:M inverso de {@link Empresa#getAreas()}. Un área puede ser cubierta por muchas empresas. */
    @ManyToMany(mappedBy = "areas", fetch = FetchType.LAZY)
    private Set<Empresa> empresas = new HashSet<>();

    /** N:M inverso de {@link Asesor#getAreas()}. Un área puede ser cubierta por muchos asesores. */
    @ManyToMany(mappedBy = "areas", fetch = FetchType.LAZY)
    private Set<Asesor> asesores = new HashSet<>();

    /** Constructor vacío requerido por JPA. */
    public Area() {
    }

    public Area(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /** Vista de solo lectura (lado inverso). */
    public Set<Empresa> getEmpresas() {
        return Collections.unmodifiableSet(empresas);
    }

    /** Vista de solo lectura (lado inverso). */
    public Set<Asesor> getAsesores() {
        return Collections.unmodifiableSet(asesores);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Area otra)) {
            return false;
        }
        return id != null && id.equals(otra.getId());
    }

    @Override
    public int hashCode() {
        return Area.class.hashCode();
    }

    @Override
    public String toString() {
        return "Area{id=" + id + ", nombre=" + nombre + "}";
    }
}
