-- Esquema SQLite de la academia. Importes enteros en centimos de PEN.
CREATE TABLE IF NOT EXISTS estudiantes (
 id INTEGER PRIMARY KEY, nombre TEXT NOT NULL CHECK(length(trim(nombre))>0), email TEXT, telefono TEXT
);
CREATE TABLE IF NOT EXISTS alumnos (
 estudiante_id INTEGER PRIMARY KEY REFERENCES estudiantes(id),
 dni TEXT UNIQUE CHECK(dni IS NULL OR (length(dni)=8 AND dni NOT GLOB '*[^0-9]*')),
 fecha_nacimiento TEXT, direccion TEXT, contacto_emergencia TEXT, telefono_emergencia TEXT,
 estado TEXT NOT NULL DEFAULT 'activo' CHECK(estado IN ('activo','inactivo','retirado')),
 codigo TEXT NOT NULL UNIQUE DEFAULT (printf('%04d', abs(random() % 10000))) CHECK(length(codigo)=4 AND codigo NOT GLOB '*[^0-9]*'),
 observaciones TEXT, creado_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS historial_alumnos (
 id INTEGER PRIMARY KEY, alumno_id INTEGER NOT NULL REFERENCES alumnos(estudiante_id),
 evento TEXT NOT NULL, detalle TEXT, registrado_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS academia (
 id INTEGER PRIMARY KEY CHECK(id=1), nombre TEXT NOT NULL, ruc TEXT, direccion TEXT,
 telefono TEXT, email TEXT, moneda TEXT NOT NULL DEFAULT 'PEN', zona_horaria TEXT NOT NULL DEFAULT 'America/Lima'
);
INSERT OR IGNORE INTO academia(id,nombre) VALUES(1,'Academia de baile');
CREATE TABLE IF NOT EXISTS parametros (
 clave TEXT PRIMARY KEY, valor TEXT NOT NULL, descripcion TEXT
);
INSERT OR IGNORE INTO parametros VALUES('intervalo_peso_dias','15','Intervalo entre mediciones de peso');
INSERT OR IGNORE INTO parametros VALUES('alerta_vencimiento_dias','7','Dias de anticipacion para alertar vencimientos');
CREATE TABLE IF NOT EXISTS planes (
 id INTEGER PRIMARY KEY, nombre TEXT NOT NULL UNIQUE,
 modalidad TEXT NOT NULL CHECK(modalidad IN ('mensualidad','sesiones')),
 duracion_dias INTEGER NOT NULL CHECK(duracion_dias>0),
 sesiones INTEGER CHECK(sesiones>0), precio_centimos INTEGER NOT NULL CHECK(precio_centimos>=0),
 activo INTEGER NOT NULL DEFAULT 1 CHECK(activo IN (0,1)),
 CHECK(modalidad<>'sesiones' OR sesiones IS NOT NULL)
);
CREATE TABLE IF NOT EXISTS matriculas (
 id INTEGER PRIMARY KEY, alumno_id INTEGER NOT NULL REFERENCES alumnos(estudiante_id),
 plan_id INTEGER NOT NULL REFERENCES planes(id), fecha_inicio TEXT NOT NULL,
 fecha_vencimiento TEXT NOT NULL CHECK(fecha_vencimiento>=fecha_inicio),
 precio_centimos INTEGER NOT NULL CHECK(precio_centimos>=0), sesiones_contratadas INTEGER CHECK(sesiones_contratadas>0),
 estado TEXT NOT NULL DEFAULT 'activa' CHECK(estado IN ('activa','pausada','vencida','cancelada')),
 renovacion_de INTEGER UNIQUE REFERENCES matriculas(id), creado_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(renovacion_de IS NULL OR renovacion_de<>id)
);
CREATE TABLE IF NOT EXISTS congelaciones (
 id INTEGER PRIMARY KEY, matricula_id INTEGER NOT NULL REFERENCES matriculas(id),
 fecha_inicio TEXT NOT NULL, fecha_fin TEXT CHECK(fecha_fin>=fecha_inicio),
 motivo TEXT NOT NULL, dias_prorrogados INTEGER NOT NULL DEFAULT 0 CHECK(dias_prorrogados>=0),
 creado_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_congelacion_abierta ON congelaciones(matricula_id) WHERE fecha_fin IS NULL;
CREATE TABLE IF NOT EXISTS instructores (
 id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, dni TEXT UNIQUE, telefono TEXT, email TEXT,
 tarifa_hora_centimos INTEGER NOT NULL DEFAULT 0 CHECK(tarifa_hora_centimos>=0),
 activo INTEGER NOT NULL DEFAULT 1 CHECK(activo IN (0,1))
);
CREATE TABLE IF NOT EXISTS horarios (
 id INTEGER PRIMARY KEY, instructor_id INTEGER NOT NULL REFERENCES instructores(id),
 nombre_clase TEXT NOT NULL, dia_semana INTEGER NOT NULL CHECK(dia_semana BETWEEN 1 AND 7),
 hora_inicio TEXT NOT NULL, hora_fin TEXT NOT NULL CHECK(hora_fin>hora_inicio),
 capacidad INTEGER CHECK(capacidad>0), activo INTEGER NOT NULL DEFAULT 1 CHECK(activo IN (0,1))
);
CREATE TABLE IF NOT EXISTS asistencias (
 id INTEGER PRIMARY KEY, matricula_id INTEGER NOT NULL REFERENCES matriculas(id),
 horario_id INTEGER REFERENCES horarios(id), fecha TEXT NOT NULL DEFAULT (date('now','-5 hours')),
 hora_ingreso TEXT NOT NULL DEFAULT (time('now','-5 hours')), hora_salida TEXT CHECK(hora_salida>=hora_ingreso),
 metodo TEXT NOT NULL DEFAULT 'manual' CHECK(metodo IN ('qr','manual')),
 UNIQUE(matricula_id,fecha,hora_ingreso)
);
CREATE TABLE IF NOT EXISTS mediciones_peso (
 id INTEGER PRIMARY KEY, alumno_id INTEGER NOT NULL REFERENCES alumnos(estudiante_id),
 fecha TEXT NOT NULL, peso_kg REAL NOT NULL CHECK(peso_kg>0 AND peso_kg<700),
 observaciones TEXT, UNIQUE(alumno_id,fecha)
);
CREATE TABLE IF NOT EXISTS asistencias_instructores (
 id INTEGER PRIMARY KEY, instructor_id INTEGER NOT NULL REFERENCES instructores(id),
 fecha TEXT NOT NULL, hora_ingreso TEXT NOT NULL, hora_salida TEXT CHECK(hora_salida>=hora_ingreso),
 UNIQUE(instructor_id,fecha,hora_ingreso)
);
CREATE TABLE IF NOT EXISTS remuneraciones (
 id INTEGER PRIMARY KEY, instructor_id INTEGER NOT NULL REFERENCES instructores(id),
 periodo_inicio TEXT NOT NULL, periodo_fin TEXT NOT NULL CHECK(periodo_fin>=periodo_inicio),
 monto_centimos INTEGER NOT NULL CHECK(monto_centimos>0),
 estado TEXT NOT NULL DEFAULT 'pendiente' CHECK(estado IN ('pendiente','pagada','anulada')),
 UNIQUE(instructor_id,periodo_inicio,periodo_fin)
);
CREATE TABLE IF NOT EXISTS cajas (
 id INTEGER PRIMARY KEY, apertura TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, cierre TEXT CHECK(cierre>=apertura),
 saldo_inicial_centimos INTEGER NOT NULL DEFAULT 0 CHECK(saldo_inicial_centimos>=0),
 saldo_contado_centimos INTEGER CHECK(saldo_contado_centimos>=0), responsable TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_caja_abierta ON cajas((1)) WHERE cierre IS NULL;
CREATE TABLE IF NOT EXISTS pagos (
 id INTEGER PRIMARY KEY, matricula_id INTEGER NOT NULL REFERENCES matriculas(id),
 concepto TEXT NOT NULL CHECK(concepto IN ('mensualidad','sesiones','matricula','otro')),
 monto_centimos INTEGER NOT NULL CHECK(monto_centimos>0),
 metodo TEXT NOT NULL CHECK(metodo IN ('yape','efectivo')), referencia TEXT,
 estado TEXT NOT NULL DEFAULT 'confirmado' CHECK(estado IN ('pendiente','confirmado','anulado')),
 fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pago_yape_referencia ON pagos(referencia) WHERE metodo='yape' AND referencia IS NOT NULL;
CREATE TABLE IF NOT EXISTS comprobantes (
 id INTEGER PRIMARY KEY, pago_id INTEGER NOT NULL UNIQUE REFERENCES pagos(id),
 serie TEXT NOT NULL DEFAULT 'LOCAL', numero INTEGER NOT NULL CHECK(numero>0),
 tipo TEXT NOT NULL DEFAULT 'boleta_local' CHECK(tipo='boleta_local'),
 receptor_nombre TEXT NOT NULL, receptor_documento TEXT,
 total_centimos INTEGER NOT NULL CHECK(total_centimos>0), emitido_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 estado TEXT NOT NULL DEFAULT 'emitido' CHECK(estado IN ('emitido','anulado')), UNIQUE(serie,numero)
);
CREATE TABLE IF NOT EXISTS movimientos_caja (
 id INTEGER PRIMARY KEY, caja_id INTEGER NOT NULL REFERENCES cajas(id),
 tipo TEXT NOT NULL CHECK(tipo IN ('ingreso','egreso')), concepto TEXT NOT NULL,
 monto_centimos INTEGER NOT NULL CHECK(monto_centimos>0),
 metodo TEXT NOT NULL CHECK(metodo IN ('yape','efectivo')),
 pago_id INTEGER UNIQUE REFERENCES pagos(id), remuneracion_id INTEGER UNIQUE REFERENCES remuneraciones(id),
 fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(pago_id IS NULL OR (tipo='ingreso' AND remuneracion_id IS NULL)),
 CHECK(remuneracion_id IS NULL OR tipo='egreso')
);
CREATE TABLE IF NOT EXISTS sorteos (
 id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, premio TEXT NOT NULL, fecha_sorteo TEXT NOT NULL,
 minimo_asistencias INTEGER NOT NULL DEFAULT 0 CHECK(minimo_asistencias>=0),
 periodo_inicio TEXT NOT NULL, periodo_fin TEXT NOT NULL CHECK(periodo_fin>=periodo_inicio),
 requiere_matricula_vigente INTEGER NOT NULL DEFAULT 1 CHECK(requiere_matricula_vigente IN (0,1)),
 estado TEXT NOT NULL DEFAULT 'abierto' CHECK(estado IN ('abierto','realizado','cancelado'))
);
CREATE TABLE IF NOT EXISTS participantes_sorteo (
 sorteo_id INTEGER NOT NULL REFERENCES sorteos(id), alumno_id INTEGER NOT NULL REFERENCES alumnos(estudiante_id),
 registrado_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(sorteo_id,alumno_id)
);
CREATE TABLE IF NOT EXISTS ganadores_sorteo (
 sorteo_id INTEGER NOT NULL, alumno_id INTEGER NOT NULL, puesto INTEGER NOT NULL CHECK(puesto>0),
 elegido_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(sorteo_id,puesto), UNIQUE(sorteo_id,alumno_id),
 FOREIGN KEY(sorteo_id,alumno_id) REFERENCES participantes_sorteo(sorteo_id,alumno_id)
);
CREATE INDEX IF NOT EXISTS ix_matriculas_alumno ON matriculas(alumno_id,fecha_vencimiento);
CREATE INDEX IF NOT EXISTS ix_asistencias_fecha ON asistencias(fecha,matricula_id);
CREATE INDEX IF NOT EXISTS ix_peso_alumno ON mediciones_peso(alumno_id,fecha);
CREATE INDEX IF NOT EXISTS ix_pagos_matricula ON pagos(matricula_id,estado);
CREATE INDEX IF NOT EXISTS ix_movimientos_fecha ON movimientos_caja(fecha,caja_id);
CREATE VIEW IF NOT EXISTS v_matriculas AS
 SELECT m.*, e.nombre, p.nombre AS plan_nombre,
 CASE WHEN m.estado='activa' AND m.fecha_vencimiento<date('now','-5 hours') THEN 'vencida' ELSE m.estado END AS estado_actual,
 (SELECT count(*) FROM asistencias a WHERE a.matricula_id=m.id) AS sesiones_usadas,
 m.precio_centimos-COALESCE((SELECT sum(pa.monto_centimos) FROM pagos pa WHERE pa.matricula_id=m.id AND pa.estado='confirmado'),0) AS saldo_pendiente_centimos
 FROM matriculas m JOIN estudiantes e ON e.id=m.alumno_id JOIN planes p ON p.id=m.plan_id;
CREATE VIEW IF NOT EXISTS v_balance_cajas AS
 SELECT c.*, COALESCE(sum(CASE WHEN m.tipo='ingreso' THEN m.monto_centimos ELSE 0 END),0) AS ingresos_centimos,
 COALESCE(sum(CASE WHEN m.tipo='egreso' THEN m.monto_centimos ELSE 0 END),0) AS egresos_centimos,
 c.saldo_inicial_centimos+COALESCE(sum(CASE WHEN m.metodo='efectivo' THEN CASE WHEN m.tipo='ingreso' THEN m.monto_centimos ELSE -m.monto_centimos END ELSE 0 END),0) AS efectivo_esperado_centimos
 FROM cajas c LEFT JOIN movimientos_caja m ON m.caja_id=c.id GROUP BY c.id;
CREATE VIEW IF NOT EXISTS v_finanzas_diarias AS
 SELECT date(fecha,'-5 hours') AS fecha, metodo,
 sum(CASE WHEN tipo='ingreso' THEN monto_centimos ELSE 0 END) AS ingresos_centimos,
 sum(CASE WHEN tipo='egreso' THEN monto_centimos ELSE 0 END) AS egresos_centimos,
 sum(CASE WHEN tipo='ingreso' THEN monto_centimos ELSE -monto_centimos END) AS balance_centimos
 FROM movimientos_caja GROUP BY date(fecha,'-5 hours'),metodo;
CREATE VIEW IF NOT EXISTS v_alertas_vencimientos AS
 SELECT * FROM v_matriculas WHERE estado='activa' AND fecha_vencimiento<=date('now','-5 hours','+'||(SELECT valor FROM parametros WHERE clave='alerta_vencimiento_dias')||' days');
CREATE VIEW IF NOT EXISTS v_control_peso AS
 SELECT a.estudiante_id AS alumno_id,e.nombre,max(p.fecha) AS ultima_medicion,
 date(max(p.fecha),'+'||(SELECT valor FROM parametros WHERE clave='intervalo_peso_dias')||' days') AS proxima_medicion
 FROM alumnos a JOIN estudiantes e ON e.id=a.estudiante_id LEFT JOIN mediciones_peso p ON p.alumno_id=a.estudiante_id
 WHERE a.estado='activo' GROUP BY a.estudiante_id;
CREATE VIEW IF NOT EXISTS v_elegibles_sorteo AS
 SELECT s.id AS sorteo_id,a.estudiante_id AS alumno_id,e.nombre
 FROM sorteos s CROSS JOIN alumnos a JOIN estudiantes e ON e.id=a.estudiante_id
 WHERE a.estado='activo' AND s.estado='abierto'
 AND (SELECT count(*) FROM asistencias x JOIN matriculas m ON m.id=x.matricula_id WHERE m.alumno_id=a.estudiante_id AND x.fecha BETWEEN s.periodo_inicio AND s.periodo_fin)>=s.minimo_asistencias
 AND (s.requiere_matricula_vigente=0 OR EXISTS(SELECT 1 FROM matriculas m WHERE m.alumno_id=a.estudiante_id AND m.estado='activa' AND s.fecha_sorteo BETWEEN m.fecha_inicio AND m.fecha_vencimiento));
CREATE VIEW IF NOT EXISTS v_dashboard AS
 SELECT (SELECT count(*) FROM alumnos WHERE estado='activo') AS alumnos_activos,
 (SELECT count(*) FROM v_matriculas WHERE estado_actual='activa') AS matriculas_activas,
 (SELECT count(*) FROM asistencias WHERE fecha=date('now','-5 hours')) AS ingresos_hoy,
 (SELECT COALESCE(sum(monto_centimos),0) FROM pagos WHERE estado='confirmado' AND date(fecha,'-5 hours')=date('now','-5 hours')) AS cobros_hoy_centimos,
 (SELECT count(*) FROM v_alertas_vencimientos) AS alertas_vencimiento;
