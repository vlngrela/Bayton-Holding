package com.baytonholdings.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Asesor del holding. Puede cubrir varias áreas y prestar servicio a varias
 * empresas; el detalle de dónde presta servicio y desde cuándo se registra en
 * {@link Asesoria}.
 */
@Entity
@Table(name = "asesor")
public class Asesor extends Persona {

    @Column(nullable = false, length = 100)
    private String titulacion;

    /**
     * N:M (lado propietario). Áreas que cubre el asesor; tabla intermedia
     * {@code asesor_area}.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "asesor_area",
            joinColumns = @JoinColumn(name = "asesor_id"),
            inverseJoinColumns = @JoinColumn(name = "area_id"))
    private Set<Area> areas = new HashSet<>();

    /**
     * 1:N inverso de {@link Asesoria#getAsesor()}. Un asesor tiene muchas
     * asesorías. Cascada total y orphanRemoval: una asesoría no tiene sentido
     * sin su asesor, y quitarla de la colección la elimina.
     */
    @OneToMany(mappedBy = "asesor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Asesoria> asesorias = new HashSet<>();

    /** Constructor vacío requerido por JPA. */
    public Asesor() {
        super();
    }

    public Asesor(String codigo, String nombre, String direccion, String titulacion) {
        super(codigo, nombre, direccion);
        this.titulacion = titulacion;
    }

    @Override
    public String getDescripcionRol() {
        return "Asesor";
    }

    public void agregarArea(Area area) {
        this.areas.add(area);
    }

    public void quitarArea(Area area) {
        this.areas.remove(area);
    }

    /**
     * Registra una asesoría de este asesor y, como hacía el sistema anterior,
     * agrega su área a las áreas que cubre (un Set evita duplicados).
     */
    public void agregarAsesoria(Asesoria asesoria) {
        asesoria.setAsesor(this);
        this.asesorias.add(asesoria);
        if (asesoria.getArea() != null) {
            this.areas.add(asesoria.getArea());
        }
    }

    /** Quita la asesoría de la colección; por orphanRemoval se elimina de la base. */
    public void quitarAsesoria(Asesoria asesoria) {
        this.asesorias.remove(asesoria);
    }

    public String getTitulacion() {
        return titulacion;
    }

    public void setTitulacion(String titulacion) {
        this.titulacion = titulacion;
    }

    /** Vista de solo lectura; para modificar usar {@link #agregarArea} / {@link #quitarArea}. */
    public Set<Area> getAreas() {
        return Collections.unmodifiableSet(areas);
    }

    /** Vista de solo lectura; para modificar usar {@link #agregarAsesoria} / {@link #quitarAsesoria}. */
    public Set<Asesoria> getAsesorias() {
        return Collections.unmodifiableSet(asesorias);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Asesor otro)) {
            return false;
        }
        return getId() != null && getId().equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Asesor.class.hashCode();
    }

    @Override
    public String toString() {
        return "Asesor{id=" + getId() + ", codigo=" + getCodigo() + ", nombre=" + getNombre() + "}";
    }
}
