-- =============================================================================
-- Bayton Grupo Empresario - datos de prueba (MySQL 8 / MariaDB 10.5+)
-- =============================================================================
-- USO MANUAL (la aplicacion no lo ejecuta). Requiere que las tablas ya existan:
-- ejecutar antes la aplicacion una vez, o db/schema.sql.
--
--   mysql -u root -p --default-character-set=utf8mb4 bayton_holding
--   mysql> SOURCE /ruta/completa/data.sql;
--
-- IDEMPOTENTE: usa INSERT IGNORE y busca todo por nombre/codigo (nunca por id),
-- asi que se puede ejecutar varias veces y antes o despues de la primera
-- ejecucion de la aplicacion. DataInitializer crea un subconjunto de estos mismos
-- datos (pais Argentina, empresa Bayton Tech S.A., vendedor V001, asesor A001);
-- si ya existen, este script los respeta y completa el resto.
-- NO crea usuarios: de eso se ocupa DataInitializer (las claves van con BCrypt).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) PAISES  (PIB en miles de millones de USD; valores aproximados, solo de prueba)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO pais (nombre, pib, num_habitantes, capital) VALUES
    ('Argentina',  640.00,  46000000, 'Buenos Aires'),
    ('Brasil',    2170.00, 215000000, 'Brasilia'),
    ('Chile',      335.00,  19500000, 'Santiago');

-- -----------------------------------------------------------------------------
-- 2) AREAS
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO area (nombre, descripcion) VALUES
    ('Tecnología', 'Desarrollo de software, infraestructura y servicios digitales'),
    ('Salud',      'Servicios médicos, equipamiento y farmacéutica'),
    ('Educación',  'Formación, capacitación y plataformas educativas');

-- -----------------------------------------------------------------------------
-- 3) EMPRESAS con sede y operacion cruzadas
--    Bayton Tech S.A. : sede Argentina, opera en Brasil y Chile
--    Andes Salud S.A. : sede Chile,     opera en Argentina y Brasil
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO empresa (nombre, fecha_entrada_holding, facturacion_anual, ciudad_sede, pais_sede_id)
SELECT 'Bayton Tech S.A.', '2019-03-15', 12500000.00, 'Buenos Aires', p.id
FROM pais p WHERE p.nombre = 'Argentina';

INSERT IGNORE INTO empresa (nombre, fecha_entrada_holding, facturacion_anual, ciudad_sede, pais_sede_id)
SELECT 'Andes Salud S.A.', '2021-08-01', 8200000.00, 'Santiago', p.id
FROM pais p WHERE p.nombre = 'Chile';

-- Paises de operacion
INSERT IGNORE INTO empresa_pais_operacion (empresa_id, pais_id)
SELECT e.id, p.id FROM empresa e CROSS JOIN pais p
WHERE e.nombre = 'Bayton Tech S.A.' AND p.nombre IN ('Brasil', 'Chile');

INSERT IGNORE INTO empresa_pais_operacion (empresa_id, pais_id)
SELECT e.id, p.id FROM empresa e CROSS JOIN pais p
WHERE e.nombre = 'Andes Salud S.A.' AND p.nombre IN ('Argentina', 'Brasil');

-- Areas que cubre cada empresa
INSERT IGNORE INTO empresa_area (empresa_id, area_id)
SELECT e.id, a.id FROM empresa e CROSS JOIN area a
WHERE e.nombre = 'Bayton Tech S.A.' AND a.nombre IN ('Tecnología', 'Educación');

INSERT IGNORE INTO empresa_area (empresa_id, area_id)
SELECT e.id, a.id FROM empresa e CROSS JOIN area a
WHERE e.nombre = 'Andes Salud S.A.' AND a.nombre IN ('Salud', 'Tecnología');

-- -----------------------------------------------------------------------------
-- 4) VENDEDORES con jerarquia (todos en Bayton Tech S.A.: un captador y su captado
--    deben ser de la misma empresa)
--
--        V001 Carlos  (raiz: sin captador, fecha = fecha de alta)
--        ├── V002 Valeria
--        │     └── V004 Sofia
--        └── V003 Diego
--
--    Andes Salud S.A. queda sin vendedores: sirve para probar que SI se puede
--    eliminar, mientras que Bayton Tech S.A. no (regla: no se elimina una empresa
--    con vendedores).
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO vendedor (codigo, nombre, direccion, fecha_captacion, empresa_id, captador_id)
SELECT 'V001', 'Carlos Méndez', 'Av. Corrientes 1234, Buenos Aires', '2024-01-15', e.id, NULL
FROM empresa e WHERE e.nombre = 'Bayton Tech S.A.';

INSERT IGNORE INTO vendedor (codigo, nombre, direccion, fecha_captacion, empresa_id, captador_id)
SELECT 'V002', 'Valeria Ortiz', 'Av. Santa Fe 2200, Buenos Aires', '2024-03-10', e.id, c.id
FROM empresa e JOIN vendedor c ON c.codigo = 'V001'
WHERE e.nombre = 'Bayton Tech S.A.';

INSERT IGNORE INTO vendedor (codigo, nombre, direccion, fecha_captacion, empresa_id, captador_id)
SELECT 'V003', 'Diego Paredes', 'Calle Lavalle 850, Buenos Aires', '2024-04-22', e.id, c.id
FROM empresa e JOIN vendedor c ON c.codigo = 'V001'
WHERE e.nombre = 'Bayton Tech S.A.';

INSERT IGNORE INTO vendedor (codigo, nombre, direccion, fecha_captacion, empresa_id, captador_id)
SELECT 'V004', 'Sofía Benítez', 'Av. Cabildo 3100, Buenos Aires', '2024-07-05', e.id, c.id
FROM empresa e JOIN vendedor c ON c.codigo = 'V002'
WHERE e.nombre = 'Bayton Tech S.A.';

-- -----------------------------------------------------------------------------
-- 5) ASESORES con sus areas y una asesoria cada uno
--    (las areas del asesor incluyen siempre la de su asesoria)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO asesor (codigo, nombre, direccion, titulacion) VALUES
    ('A001', 'Lucía Fernández', 'Calle Florida 567, Buenos Aires', 'Ingeniera en Sistemas'),
    ('A002', 'Martín Rojas',    'Av. Providencia 1500, Santiago',   'Licenciado en Gestión de Salud');

INSERT IGNORE INTO asesor_area (asesor_id, area_id)
SELECT s.id, a.id FROM asesor s CROSS JOIN area a
WHERE s.codigo = 'A001' AND a.nombre IN ('Tecnología', 'Educación');

INSERT IGNORE INTO asesor_area (asesor_id, area_id)
SELECT s.id, a.id FROM asesor s CROSS JOIN area a
WHERE s.codigo = 'A002' AND a.nombre IN ('Salud', 'Tecnología');

-- Asesorias (la terna asesor-empresa-area es unica)
INSERT IGNORE INTO asesoria (fecha_inicio, asesor_id, empresa_id, area_id)
SELECT '2024-02-01', s.id, e.id, a.id
FROM asesor s CROSS JOIN empresa e CROSS JOIN area a
WHERE s.codigo = 'A001' AND e.nombre = 'Bayton Tech S.A.' AND a.nombre = 'Tecnología';

INSERT IGNORE INTO asesoria (fecha_inicio, asesor_id, empresa_id, area_id)
SELECT '2024-05-15', s.id, e.id, a.id
FROM asesor s CROSS JOIN empresa e CROSS JOIN area a
WHERE s.codigo = 'A002' AND e.nombre = 'Andes Salud S.A.' AND a.nombre = 'Salud';
