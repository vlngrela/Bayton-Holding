package com.baytonholdings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Credenciales de acceso al sistema. Es independiente de {@link Vendedor} y
 * {@link Asesor}: se vincula con ellos de forma lógica mediante
 * {@link #referenciaId} y {@link #rol}, sin clave foránea.
 *
 * <p>Como los ids de vendedor y de asesor viven en tablas distintas, el
 * {@code referenciaId} solo es interpretable junto con el rol: si el rol es
 * VENDEDOR apunta a un {@code Vendedor}; si es ASESOR, a un {@code Asesor};
 * si es ADMINISTRADOR, es {@code null}. Al no haber FK, el servicio debe
 * eliminar el usuario cuando elimine a la persona que referencia.</p>
 *
 * <p>No implementa {@code UserDetails} a propósito: esa adaptación pertenece
 * a la capa de seguridad, no al dominio.</p>
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash BCrypt (60 caracteres). Nunca se guarda la clave en texto plano. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    /** Id del Vendedor o Asesor al que corresponde la cuenta; null para el administrador. */
    @Column(name = "referencia_id")
    private Long referenciaId;

    /** Constructor vacío requerido por JPA. */
    public Usuario() {
    }

    public Usuario(String username, String password, Rol rol, Long referenciaId) {
        this.username = username;
        this.password = password;
        this.rol = rol;
        this.referenciaId = referenciaId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Long getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(Long referenciaId) {
        this.referenciaId = referenciaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }

    /** No incluye el password a propósito, para que no termine en logs. */
    @Override
    public String toString() {
        return "Usuario{id=" + id + ", username=" + username + ", rol=" + rol + "}";
    }
}
