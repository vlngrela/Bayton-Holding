package com.baytonholdings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Vendedor de una empresa del holding. Puede haber sido captado por otro
 * vendedor de la misma empresa, lo que forma la estructura piramidal de
 * captación (auto-relación).
 */
@Entity
@Table(name = "vendedor")
public class Vendedor extends Persona {

    /**
     * Fecha de captación. Si el vendedor tiene captador, es la fecha en que fue
     * captado; si no, es la fecha de alta en la estructura (la asigna el servicio
     * al crearlo). Se conserva aunque el vendedor pierda a su captador.
     */
    @Column(name = "fecha_captacion")
    private LocalDate fechaCaptacion;

    /** N:1 obligatorio. Muchos vendedores trabajan para una única empresa. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** N:1 opcional (auto-relación). Muchos captados tienen, como máximo, un captador. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captador_id")
    private Vendedor captador;

    /**
     * 1:N inverso de {@link #captador}. Un captador tiene muchos captados.
     * Sin cascada: al eliminar a un captador, sus captados NO se borran;
     * quedan sin captador (el servicio debe limpiar el vínculo antes de borrar).
     */
    @OneToMany(mappedBy = "captador")
    private Set<Vendedor> captados = new HashSet<>();

    /** Constructor vacío requerido por JPA. */
    public Vendedor() {
        super();
    }

    public Vendedor(String codigo, String nombre, String direccion, Empresa empresa) {
        super(codigo, nombre, direccion);
        this.empresa = empresa;
    }

    @Override
    public String getDescripcionRol() {
        return "Vendedor";
    }

    /**
     * Registra que este vendedor captó a {@code captado}, actualizando ambos
     * lados de la relación. Si el captado ya tenía otro captador, se lo
     * desvincula primero.
     *
     * @throws IllegalArgumentException si se intenta captar a sí mismo, si los
     *         vendedores son de empresas distintas o si se generaría un ciclo
     *         (el captado ya es ancestro de este vendedor).
     * @throws IllegalStateException si este vendedor no tiene empresa asignada.
     */
    public void captar(Vendedor captado, LocalDate fecha) {
        Objects.requireNonNull(captado, "El vendedor captado es obligatorio.");
        Objects.requireNonNull(fecha, "La fecha de captación es obligatoria.");
        if (this.empresa == null) {
            throw new IllegalStateException("El captador no tiene empresa asignada.");
        }
        if (this.equals(captado)) {
            throw new IllegalArgumentException("Un vendedor no puede captarse a sí mismo.");
        }
        if (!this.empresa.equals(captado.getEmpresa())) {
            throw new IllegalArgumentException(
                    "El captador y el captado deben pertenecer a la misma empresa.");
        }
        for (Vendedor ancestro = this.getCaptador(); ancestro != null; ancestro = ancestro.getCaptador()) {
            if (ancestro.equals(captado)) {
                throw new IllegalArgumentException(
                        "La captación generaría un ciclo en la estructura piramidal.");
            }
        }

        Vendedor anterior = captado.getCaptador();
        if (anterior != null) {
            anterior.removerCaptado(captado);
        }
        captado.setCaptador(this);
        captado.setFechaCaptacion(fecha);
        this.captados.add(captado);
    }

    /**
     * Desvincula a este vendedor de su captador (si lo tiene), limpiando ambos
     * lados. Conserva la fecha de captación: el vendedor sigue en la estructura.
     */
    public void quitarCaptador() {
        if (captador != null) {
            captador.removerCaptado(this);
        }
        this.captador = null;
    }

    /** Uso interno de la relación bidireccional; preferir {@link #captar} y {@link #quitarCaptador}. */
    public void removerCaptado(Vendedor captado) {
        this.captados.remove(captado);
    }

    public LocalDate getFechaCaptacion() {
        return fechaCaptacion;
    }

    public void setFechaCaptacion(LocalDate fechaCaptacion) {
        this.fechaCaptacion = fechaCaptacion;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Vendedor getCaptador() {
        return captador;
    }

    public void setCaptador(Vendedor captador) {
        this.captador = captador;
    }

    /** Vista de solo lectura; para modificar usar {@link #captar} / {@link #quitarCaptador}. */
    public Set<Vendedor> getCaptados() {
        return Collections.unmodifiableSet(captados);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vendedor otro)) {
            return false;
        }
        return getId() != null && getId().equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Vendedor.class.hashCode();
    }

    @Override
    public String toString() {
        return "Vendedor{id=" + getId() + ", codigo=" + getCodigo() + ", nombre=" + getNombre() + "}";
    }
}
