package com.baytonholdings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * País en el que una empresa del holding puede tener su sede o desarrollar
 * actividad. Es referenciado por {@link Empresa} (N:1 como sede y N:M como
 * país de operación); no mantiene colecciones propias para evitar cargas
 * innecesarias.
 */
@Entity
@Table(name = "pais")
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    /** Producto interno bruto del país. */
    @Column(nullable = false)
    private Double pib;

    @Column(name = "num_habitantes", nullable = false)
    private Long numHabitantes;

    @Column(nullable = false, length = 100)
    private String capital;

    /** Constructor vacío requerido por JPA. */
    public Pais() {
    }

    public Pais(String nombre, Double pib, Long numHabitantes, String capital) {
        this.nombre = nombre;
        this.pib = pib;
        this.numHabitantes = numHabitantes;
        this.capital = capital;
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

    public Double getPib() {
        return pib;
    }

    public void setPib(Double pib) {
        this.pib = pib;
    }

    public Long getNumHabitantes() {
        return numHabitantes;
    }

    public void setNumHabitantes(Long numHabitantes) {
        this.numHabitantes = numHabitantes;
    }

    public String getCapital() {
        return capital;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pais otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Pais.class.hashCode();
    }

    @Override
    public String toString() {
        return "Pais{id=" + id + ", nombre=" + nombre + ", capital=" + capital + "}";
    }
}
