package com.baytonholdings.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Empresa perteneciente al holding. Tiene un único país sede, puede operar en
 * varios países (no necesariamente la sede), cubrir varias áreas de mercado y
 * contar con varios vendedores.
 */
@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "fecha_entrada_holding", nullable = false)
    private LocalDate fechaEntradaHolding;

    @Column(name = "facturacion_anual", nullable = false, precision = 19, scale = 2)
    private BigDecimal facturacionAnual;

    @Column(name = "ciudad_sede", nullable = false, length = 100)
    private String ciudadSede;

    /** N:1 obligatorio. Muchas empresas pueden tener el mismo país como sede. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pais_sede_id", nullable = false)
    private Pais paisSede;

    /**
     * N:M (lado propietario). Países donde la empresa desarrolla actividad;
     * tabla intermedia {@code empresa_pais_operacion}.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "empresa_pais_operacion",
            joinColumns = @JoinColumn(name = "empresa_id"),
            inverseJoinColumns = @JoinColumn(name = "pais_id"))
    private Set<Pais> paisesOperacion = new HashSet<>();

    /**
     * N:M (lado propietario). Áreas de mercado que cubre la empresa; tabla
     * intermedia {@code empresa_area}.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "empresa_area",
            joinColumns = @JoinColumn(name = "empresa_id"),
            inverseJoinColumns = @JoinColumn(name = "area_id"))
    private Set<Area> areas = new HashSet<>();

    /**
     * 1:N inverso de {@link Vendedor#getEmpresa()}. Una empresa tiene muchos
     * vendedores. Solo cascada de borrado: un vendedor no existe sin empresa,
     * pero no se usa orphanRemoval porque un vendedor puede pasar a otra
     * empresa sin que eso lo elimine. Antes de borrar la empresa, el servicio
     * debe limpiar los vínculos de captación (auto-FK entre vendedores).
     */
    @OneToMany(mappedBy = "empresa", cascade = CascadeType.REMOVE)
    private Set<Vendedor> vendedores = new HashSet<>();

    /** Constructor vacío requerido por JPA. */
    public Empresa() {
    }

    public Empresa(String nombre, LocalDate fechaEntradaHolding, BigDecimal facturacionAnual,
                   String ciudadSede, Pais paisSede) {
        this.nombre = nombre;
        this.fechaEntradaHolding = fechaEntradaHolding;
        this.facturacionAnual = facturacionAnual;
        this.ciudadSede = ciudadSede;
        this.paisSede = paisSede;
    }

    public void agregarPaisOperacion(Pais pais) {
        this.paisesOperacion.add(pais);
    }

    public void quitarPaisOperacion(Pais pais) {
        this.paisesOperacion.remove(pais);
    }

    public void agregarArea(Area area) {
        this.areas.add(area);
    }

    public void quitarArea(Area area) {
        this.areas.remove(area);
    }

    /** Agrega el vendedor a la empresa y fija la empresa en el vendedor (ambos lados). */
    public void agregarVendedor(Vendedor vendedor) {
        this.vendedores.add(vendedor);
        vendedor.setEmpresa(this);
    }

    /**
     * Cantidad de vendedores, calculada a partir de la colección (no es una
     * columna, para que no pueda quedar desincronizada). Carga la colección
     * perezosa: usar dentro de una transacción.
     */
    public int getNumVendedores() {
        return vendedores.size();
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

    public LocalDate getFechaEntradaHolding() {
        return fechaEntradaHolding;
    }

    public void setFechaEntradaHolding(LocalDate fechaEntradaHolding) {
        this.fechaEntradaHolding = fechaEntradaHolding;
    }

    public BigDecimal getFacturacionAnual() {
        return facturacionAnual;
    }

    public void setFacturacionAnual(BigDecimal facturacionAnual) {
        this.facturacionAnual = facturacionAnual;
    }

    public String getCiudadSede() {
        return ciudadSede;
    }

    public void setCiudadSede(String ciudadSede) {
        this.ciudadSede = ciudadSede;
    }

    public Pais getPaisSede() {
        return paisSede;
    }

    public void setPaisSede(Pais paisSede) {
        this.paisSede = paisSede;
    }

    /** Vista de solo lectura; para modificar usar agregarPaisOperacion / quitarPaisOperacion. */
    public Set<Pais> getPaisesOperacion() {
        return Collections.unmodifiableSet(paisesOperacion);
    }

    /** Vista de solo lectura; para modificar usar agregarArea / quitarArea. */
    public Set<Area> getAreas() {
        return Collections.unmodifiableSet(areas);
    }

    /** Vista de solo lectura; para modificar usar agregarVendedor. */
    public Set<Vendedor> getVendedores() {
        return Collections.unmodifiableSet(vendedores);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Empresa otra)) {
            return false;
        }
        return id != null && id.equals(otra.getId());
    }

    @Override
    public int hashCode() {
        return Empresa.class.hashCode();
    }

    @Override
    public String toString() {
        return "Empresa{id=" + id + ", nombre=" + nombre + ", ciudadSede=" + ciudadSede + "}";
    }
}
