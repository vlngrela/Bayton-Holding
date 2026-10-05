package com.baytonholdings.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Define el encriptador de contraseñas (BCrypt) como bean.
 *
 * <p>Está en una clase aparte, y no dentro de la configuración principal de
 * seguridad, a propósito: {@code UsuarioService} necesita el encoder y la
 * configuración de seguridad necesitará servicios de usuarios. Si el bean
 * viviera en la misma clase que el resto de la configuración, Spring
 * detectaría una dependencia circular al arrancar.</p>
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
