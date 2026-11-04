DROP VIEW IF EXISTS v_matriculas;
DROP VIEW IF EXISTS v_balance_cajas;
DROP VIEW IF EXISTS v_finanzas_diarias;
DROP VIEW IF EXISTS v_alertas_vencimientos;
DROP VIEW IF EXISTS v_control_peso;
DROP VIEW IF EXISTS v_elegibles_sorteo;
DROP VIEW IF EXISTS v_dashboard;
CREATE TABLE alumnos_nuevos (
 estudiante_id INTEGER PRIMARY KEY REFERENCES estudiantes(id),
 dni TEXT UNIQUE CHECK(dni IS NULL OR (length(dni)=8 AND dni NOT GLOB '*[^0-9]*')),
 fecha_nacimiento TEXT, direccion TEXT, contacto_emergencia TEXT, telefono_emergencia TEXT,
 estado TEXT NOT NULL DEFAULT 'activo' CHECK(estado IN ('activo','inactivo','retirado')),
 codigo TEXT NOT NULL UNIQUE DEFAULT (printf('%04d', abs(random() % 10000))) CHECK(length(codigo)=4 AND codigo NOT GLOB '*[^0-9]*'),
 observaciones TEXT, creado_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO alumnos_nuevos(estudiante_id,dni,fecha_nacimiento,direccion,contacto_emergencia,telefono_emergencia,estado,codigo,observaciones,creado_en)
SELECT estudiante_id,dni,fecha_nacimiento,direccion,contacto_emergencia,telefono_emergencia,estado,
printf('%04d',row_number() OVER (ORDER BY estudiante_id)-1),observaciones,creado_en FROM alumnos;
DROP TABLE alumnos;
ALTER TABLE alumnos_nuevos RENAME TO alumnos;
