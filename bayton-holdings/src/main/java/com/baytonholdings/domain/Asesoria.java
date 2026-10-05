package com.baytonholdings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * Entidad intermedia con atributo propio: registra que un {@link Asesor}
 * presta servicio a una {@link Empresa} en un {@link Area} determinada, y
 * desde qué fecha. La combinación (asesor, empresa, área) no puede repetirse;
 * un mismo asesor sí puede tener varias asesorías con distintas empresas o
 * áreas.
 */
@Entity
@Table(
        name = "asesoria",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asesoria_asesor_empresa_area",
                columnNames = {"asesor_id", "empresa_id", "area_id"}))
public class Asesoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** N:1 obligatorio. Un asesor tiene muchas asesorías. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asesor_id", nullable = false)
    private Asesor asesor;

    /** N:1 obligatorio. Una empresa recibe muchas asesorías. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /** N:1 obligatorio. Un área tiene muchas asesorías. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    /** Constructor vacío requerido por JPA. */
    public Asesoria() {
    }

    public Asesoria(Asesor asesor, Empresa empresa, Area area, LocalDate fechaInicio) {
        this.asesor = asesor;
        this.empresa = empresa;
        this.area = area;
        this.fechaInicio = fechaInicio;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Asesor getAsesor() {
        return asesor;
    }

    public void setAsesor(Asesor asesor) {
        this.asesor = asesor;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Asesoria otra)) {
            return false;
        }
        return id != null && id.equals(otra.getId());
    }

    @Override
    public int hashCode() {
        return Asesoria.class.hashCode();
    }

    @Override
    public String toString() {
        return "Asesoria{id=" + id + ", fechaInicio=" + fechaInicio + "}";
    }
}
