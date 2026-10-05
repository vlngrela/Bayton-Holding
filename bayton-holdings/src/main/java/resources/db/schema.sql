-- =============================================================================
-- Bayton Grupo Empresario - DDL completo (MySQL 8 / MariaDB 10.5+)
-- =============================================================================
-- USO MANUAL. La aplicacion NO ejecuta este archivo (esta en db/, no en la raiz
-- del classpath, y spring.sql.init.mode=never). Normalmente las tablas las crea
-- Hibernate con ddl-auto=update; este script sirve para crearlas a mano o para
-- documentar el modelo.
--
-- Es re-ejecutable (CREATE ... IF NOT EXISTS). Refleja las entidades JPA del
-- paquete com.baytonholdings.domain; si cambias una entidad, actualiza este archivo.
--
-- Cargar desde el cliente mysql:   SOURCE /ruta/completa/schema.sql;
-- =============================================================================

CREATE DATABASE IF NOT EXISTS bayton_holding
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE bayton_holding;

-- -----------------------------------------------------------------------------
-- Para reiniciar desde cero: descomentar (el orden respeta las claves foraneas).
-- -----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS asesoria;
-- DROP TABLE IF EXISTS asesor_area;
-- DROP TABLE IF EXISTS empresa_area;
-- DROP TABLE IF EXISTS empresa_pais_operacion;
-- DROP TABLE IF EXISTS vendedor;
-- DROP TABLE IF EXISTS asesor;
-- DROP TABLE IF EXISTS empresa;
-- DROP TABLE IF EXISTS usuario;
-- DROP TABLE IF EXISTS area;
-- DROP TABLE IF EXISTS pais;

-- =============================================================================
-- TABLAS BASE (sin dependencias)
-- =============================================================================

-- Entidad Pais. El nombre es unico (sin distinguir mayusculas, por la collation _ci).
CREATE TABLE IF NOT EXISTS pais (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(100)  NOT NULL,
    pib             DOUBLE        NOT NULL,   -- PIB; los datos de prueba lo expresan en miles de millones de USD
    num_habitantes  BIGINT        NOT NULL,
    capital         VARCHAR(100)  NOT NULL,
    CONSTRAINT pk_pais        PRIMARY KEY (id),
    CONSTRAINT uk_pais_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Entidad Area (area de mercado).
CREATE TABLE IF NOT EXISTS area (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(100)  NOT NULL,
    descripcion  VARCHAR(500)  NULL,
    CONSTRAINT pk_area        PRIMARY KEY (id),
    CONSTRAINT uk_area_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Entidad Usuario (credenciales de acceso). No tiene clave foranea hacia
-- vendedor/asesor: referencia_id se interpreta junto con rol (los ids de ambas
-- tablas pueden coincidir). El servicio elimina la cuenta al eliminar a la persona.
CREATE TABLE IF NOT EXISTS usuario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password       VARCHAR(100) NOT NULL,     -- hash BCrypt (60 caracteres); nunca texto plano
    rol            VARCHAR(20)  NOT NULL,     -- enum Rol guardado como texto
    referencia_id  BIGINT       NULL,         -- id del Vendedor o Asesor; NULL para el administrador
    CONSTRAINT pk_usuario          PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username),
    -- Una persona tiene, como maximo, una cuenta (respaldo de la regla de UsuarioService).
    -- Los NULL (administrador) no chocan entre si en un indice UNIQUE.
    CONSTRAINT uk_usuario_rol_referencia UNIQUE (rol, referencia_id),
    CONSTRAINT ck_usuario_rol      CHECK (rol IN ('ADMINISTRADOR', 'VENDEDOR', 'ASESOR')),
    -- misma regla que UsuarioService: el administrador no se asocia a nadie; los demas, si
    CONSTRAINT ck_usuario_referencia CHECK (
        (rol = 'ADMINISTRADOR' AND referencia_id IS NULL)
        OR (rol <> 'ADMINISTRADOR' AND referencia_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- EMPRESA y sus relaciones N:M
-- =============================================================================

-- Entidad Empresa. pais_sede_id: N:1 obligatorio (muchas empresas, un pais sede).
CREATE TABLE IF NOT EXISTS empresa (
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    nombre                 VARCHAR(100)   NOT NULL,
    fecha_entrada_holding  DATE           NOT NULL,
    facturacion_anual      DECIMAL(19,2)  NOT NULL,
    ciudad_sede            VARCHAR(100)   NOT NULL,
    pais_sede_id           BIGINT         NOT NULL,
    CONSTRAINT pk_empresa        PRIMARY KEY (id),
    CONSTRAINT uk_empresa_nombre UNIQUE (nombre),
    CONSTRAINT fk_empresa_pais_sede FOREIGN KEY (pais_sede_id) REFERENCES pais (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- (InnoDB crea solo el indice de pais_sede_id por la FK.)

-- N:M Empresa <-> Pais: paises donde la empresa desarrolla actividad (puede incluir o no la sede).
CREATE TABLE IF NOT EXISTS empresa_pais_operacion (
    empresa_id  BIGINT NOT NULL,
    pais_id     BIGINT NOT NULL,
    CONSTRAINT pk_empresa_pais_operacion PRIMARY KEY (empresa_id, pais_id),
    CONSTRAINT fk_epo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_epo_pais    FOREIGN KEY (pais_id)    REFERENCES pais (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- N:M Empresa <-> Area: areas de mercado que cubre la empresa.
CREATE TABLE IF NOT EXISTS empresa_area (
    empresa_id  BIGINT NOT NULL,
    area_id     BIGINT NOT NULL,
    CONSTRAINT pk_empresa_area PRIMARY KEY (empresa_id, area_id),
    CONSTRAINT fk_ea_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_ea_area    FOREIGN KEY (area_id)    REFERENCES area (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- VENDEDOR (piramide de captacion) y ASESOR
-- =============================================================================

-- Entidad Vendedor (hereda de Persona). Dos FKs:
--   empresa_id  : N:1 obligatorio, la empresa para la que trabaja.
--   captador_id : N:1 opcional y AUTO-RELACION, el vendedor que lo capto.
-- fecha_captacion: fecha en que fue captado o, si no tiene captador, fecha de alta.
-- Reglas de negocio que valida el servicio (Vendedor.captar), no la base: captador y captado
-- son de la misma empresa, nadie se capta a si mismo y no hay ciclos. (Una CHECK no puede
-- referirse a la columna AUTO_INCREMENT id, por eso la autocaptacion no se controla aqui.)
CREATE TABLE IF NOT EXISTS vendedor (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    codigo           VARCHAR(30)   NOT NULL,
    nombre           VARCHAR(100)  NOT NULL,
    direccion        VARCHAR(200)  NOT NULL,
    fecha_captacion  DATE          NULL,
    empresa_id       BIGINT        NOT NULL,
    captador_id      BIGINT        NULL,
    CONSTRAINT pk_vendedor        PRIMARY KEY (id),
    CONSTRAINT uk_vendedor_codigo UNIQUE (codigo),
    CONSTRAINT fk_vendedor_empresa  FOREIGN KEY (empresa_id)  REFERENCES empresa (id),
    CONSTRAINT fk_vendedor_captador FOREIGN KEY (captador_id) REFERENCES vendedor (id),
    -- los listados se ordenan por nombre
    INDEX idx_vendedor_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- (InnoDB crea solo los indices de empresa_id y captador_id por las FKs.)

-- Entidad Asesor (hereda de Persona).
CREATE TABLE IF NOT EXISTS asesor (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    codigo      VARCHAR(30)   NOT NULL,
    nombre      VARCHAR(100)  NOT NULL,
    direccion   VARCHAR(200)  NOT NULL,
    titulacion  VARCHAR(100)  NOT NULL,
    CONSTRAINT pk_asesor        PRIMARY KEY (id),
    CONSTRAINT uk_asesor_codigo UNIQUE (codigo),
    INDEX idx_asesor_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- N:M Asesor <-> Area: areas que cubre el asesor (debe incluir las areas de todas sus asesorias).
CREATE TABLE IF NOT EXISTS asesor_area (
    asesor_id  BIGINT NOT NULL,
    area_id    BIGINT NOT NULL,
    CONSTRAINT pk_asesor_area PRIMARY KEY (asesor_id, area_id),
    CONSTRAINT fk_aa_asesor FOREIGN KEY (asesor_id) REFERENCES asesor (id),
    CONSTRAINT fk_aa_area   FOREIGN KEY (area_id)   REFERENCES area (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- ASESORIA (asociacion Asesor - Empresa - Area con atributo propio)
-- =============================================================================

-- Tres N:1 obligatorias. REGLA 2: la terna (asesor, empresa, area) es unica.
CREATE TABLE IF NOT EXISTS asesoria (
    id            BIGINT  NOT NULL AUTO_INCREMENT,
    fecha_inicio  DATE    NOT NULL,
    asesor_id     BIGINT  NOT NULL,
    empresa_id    BIGINT  NOT NULL,
    area_id       BIGINT  NOT NULL,
    CONSTRAINT pk_asesoria PRIMARY KEY (id),
    CONSTRAINT uk_asesoria_asesor_empresa_area UNIQUE (asesor_id, empresa_id, area_id),
    CONSTRAINT fk_asesoria_asesor  FOREIGN KEY (asesor_id)  REFERENCES asesor (id),
    CONSTRAINT fk_asesoria_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id),
    CONSTRAINT fk_asesoria_area    FOREIGN KEY (area_id)    REFERENCES area (id),
    -- los listados se ordenan por fecha de inicio descendente
    INDEX idx_asesoria_fecha_inicio (fecha_inicio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- (La UNIQUE ya indexa asesor_id; InnoDB indexa empresa_id y area_id por las FKs.)
