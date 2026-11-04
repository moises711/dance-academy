CREATE TRIGGER validar_estudiantes_insert
BEFORE INSERT ON estudiantes
WHEN length(trim(NEW.nombre)) NOT BETWEEN 1 AND 150
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en estudiantes');
END;
-- siguiente sentencia
CREATE TRIGGER validar_estudiantes_update
BEFORE UPDATE ON estudiantes
WHEN length(trim(NEW.nombre)) NOT BETWEEN 1 AND 150
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en estudiantes');
END;
-- siguiente sentencia
CREATE TRIGGER validar_alumnos_insert
BEFORE INSERT ON alumnos
WHEN (NEW.fecha_nacimiento IS NOT NULL AND (date(NEW.fecha_nacimiento, '+0 days') IS NOT NEW.fecha_nacimiento))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en alumnos');
END;
-- siguiente sentencia
CREATE TRIGGER validar_alumnos_update
BEFORE UPDATE ON alumnos
WHEN (NEW.fecha_nacimiento IS NOT NULL AND (date(NEW.fecha_nacimiento, '+0 days') IS NOT NEW.fecha_nacimiento))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en alumnos');
END;
-- siguiente sentencia
CREATE TRIGGER validar_planes_insert
BEFORE INSERT ON planes
WHEN (typeof(NEW.precio_centimos) <> 'integer')
  OR (typeof(NEW.duracion_dias) <> 'integer')
  OR (NEW.sesiones IS NOT NULL AND (typeof(NEW.sesiones) <> 'integer'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en planes');
END;
-- siguiente sentencia
CREATE TRIGGER validar_planes_update
BEFORE UPDATE ON planes
WHEN (typeof(NEW.precio_centimos) <> 'integer')
  OR (typeof(NEW.duracion_dias) <> 'integer')
  OR (NEW.sesiones IS NOT NULL AND (typeof(NEW.sesiones) <> 'integer'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en planes');
END;
-- siguiente sentencia
CREATE TRIGGER validar_matriculas_insert
BEFORE INSERT ON matriculas
WHEN (date(NEW.fecha_inicio, '+0 days') IS NOT NEW.fecha_inicio)
  OR (date(NEW.fecha_vencimiento, '+0 days') IS NOT NEW.fecha_vencimiento)
  OR (typeof(NEW.precio_centimos) <> 'integer')
  OR (NEW.sesiones_contratadas IS NOT NULL AND (typeof(NEW.sesiones_contratadas) <> 'integer'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en matriculas');
END;
-- siguiente sentencia
CREATE TRIGGER validar_matriculas_update
BEFORE UPDATE ON matriculas
WHEN (date(NEW.fecha_inicio, '+0 days') IS NOT NEW.fecha_inicio)
  OR (date(NEW.fecha_vencimiento, '+0 days') IS NOT NEW.fecha_vencimiento)
  OR (typeof(NEW.precio_centimos) <> 'integer')
  OR (NEW.sesiones_contratadas IS NOT NULL AND (typeof(NEW.sesiones_contratadas) <> 'integer'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en matriculas');
END;
-- siguiente sentencia
CREATE TRIGGER validar_congelaciones_insert
BEFORE INSERT ON congelaciones
WHEN (date(NEW.fecha_inicio, '+0 days') IS NOT NEW.fecha_inicio)
  OR (NEW.fecha_fin IS NOT NULL AND (date(NEW.fecha_fin, '+0 days') IS NOT NEW.fecha_fin))
  OR (typeof(NEW.dias_prorrogados) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en congelaciones');
END;
-- siguiente sentencia
CREATE TRIGGER validar_congelaciones_update
BEFORE UPDATE ON congelaciones
WHEN (date(NEW.fecha_inicio, '+0 days') IS NOT NEW.fecha_inicio)
  OR (NEW.fecha_fin IS NOT NULL AND (date(NEW.fecha_fin, '+0 days') IS NOT NEW.fecha_fin))
  OR (typeof(NEW.dias_prorrogados) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en congelaciones');
END;
-- siguiente sentencia
CREATE TRIGGER validar_asistencias_insert
BEFORE INSERT ON asistencias
WHEN (date(NEW.fecha, '+0 days') IS NOT NEW.fecha)
  OR (strftime('%H:%M:%S', NEW.hora_ingreso, '+0 seconds') IS NOT NEW.hora_ingreso OR substr(NEW.hora_ingreso, 1, 2) = '24')
  OR (NEW.hora_salida IS NOT NULL AND (strftime('%H:%M:%S', NEW.hora_salida, '+0 seconds') IS NOT NEW.hora_salida OR substr(NEW.hora_salida, 1, 2) = '24'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en asistencias');
END;
-- siguiente sentencia
CREATE TRIGGER validar_asistencias_update
BEFORE UPDATE ON asistencias
WHEN (date(NEW.fecha, '+0 days') IS NOT NEW.fecha)
  OR (strftime('%H:%M:%S', NEW.hora_ingreso, '+0 seconds') IS NOT NEW.hora_ingreso OR substr(NEW.hora_ingreso, 1, 2) = '24')
  OR (NEW.hora_salida IS NOT NULL AND (strftime('%H:%M:%S', NEW.hora_salida, '+0 seconds') IS NOT NEW.hora_salida OR substr(NEW.hora_salida, 1, 2) = '24'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en asistencias');
END;
-- siguiente sentencia
CREATE TRIGGER validar_asistencias_instructores_insert
BEFORE INSERT ON asistencias_instructores
WHEN (date(NEW.fecha, '+0 days') IS NOT NEW.fecha)
  OR (strftime('%H:%M:%S', NEW.hora_ingreso, '+0 seconds') IS NOT NEW.hora_ingreso OR substr(NEW.hora_ingreso, 1, 2) = '24')
  OR (NEW.hora_salida IS NOT NULL AND (strftime('%H:%M:%S', NEW.hora_salida, '+0 seconds') IS NOT NEW.hora_salida OR substr(NEW.hora_salida, 1, 2) = '24'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en asistencias_instructores');
END;
-- siguiente sentencia
CREATE TRIGGER validar_asistencias_instructores_update
BEFORE UPDATE ON asistencias_instructores
WHEN (date(NEW.fecha, '+0 days') IS NOT NEW.fecha)
  OR (strftime('%H:%M:%S', NEW.hora_ingreso, '+0 seconds') IS NOT NEW.hora_ingreso OR substr(NEW.hora_ingreso, 1, 2) = '24')
  OR (NEW.hora_salida IS NOT NULL AND (strftime('%H:%M:%S', NEW.hora_salida, '+0 seconds') IS NOT NEW.hora_salida OR substr(NEW.hora_salida, 1, 2) = '24'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en asistencias_instructores');
END;
-- siguiente sentencia
CREATE TRIGGER validar_horarios_insert
BEFORE INSERT ON horarios
WHEN (strftime('%H:%M:%S', NEW.hora_inicio, '+0 seconds') IS NOT NEW.hora_inicio OR substr(NEW.hora_inicio, 1, 2) = '24')
  OR (strftime('%H:%M:%S', NEW.hora_fin, '+0 seconds') IS NOT NEW.hora_fin OR substr(NEW.hora_fin, 1, 2) = '24')
  OR (NEW.capacidad IS NOT NULL AND (typeof(NEW.capacidad) <> 'integer'))
  OR (typeof(NEW.dia_semana) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en horarios');
END;
-- siguiente sentencia
CREATE TRIGGER validar_horarios_update
BEFORE UPDATE ON horarios
WHEN (strftime('%H:%M:%S', NEW.hora_inicio, '+0 seconds') IS NOT NEW.hora_inicio OR substr(NEW.hora_inicio, 1, 2) = '24')
  OR (strftime('%H:%M:%S', NEW.hora_fin, '+0 seconds') IS NOT NEW.hora_fin OR substr(NEW.hora_fin, 1, 2) = '24')
  OR (NEW.capacidad IS NOT NULL AND (typeof(NEW.capacidad) <> 'integer'))
  OR (typeof(NEW.dia_semana) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en horarios');
END;
-- siguiente sentencia
CREATE TRIGGER validar_instructores_insert
BEFORE INSERT ON instructores
WHEN (typeof(NEW.tarifa_hora_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en instructores');
END;
-- siguiente sentencia
CREATE TRIGGER validar_instructores_update
BEFORE UPDATE ON instructores
WHEN (typeof(NEW.tarifa_hora_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en instructores');
END;
-- siguiente sentencia
CREATE TRIGGER validar_mediciones_peso_insert
BEFORE INSERT ON mediciones_peso
WHEN (date(NEW.fecha, '+0 days') IS NOT NEW.fecha)
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en mediciones_peso');
END;
-- siguiente sentencia
CREATE TRIGGER validar_mediciones_peso_update
BEFORE UPDATE ON mediciones_peso
WHEN (date(NEW.fecha, '+0 days') IS NOT NEW.fecha)
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en mediciones_peso');
END;
-- siguiente sentencia
CREATE TRIGGER validar_remuneraciones_insert
BEFORE INSERT ON remuneraciones
WHEN (date(NEW.periodo_inicio, '+0 days') IS NOT NEW.periodo_inicio)
  OR (date(NEW.periodo_fin, '+0 days') IS NOT NEW.periodo_fin)
  OR (typeof(NEW.monto_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en remuneraciones');
END;
-- siguiente sentencia
CREATE TRIGGER validar_remuneraciones_update
BEFORE UPDATE ON remuneraciones
WHEN (date(NEW.periodo_inicio, '+0 days') IS NOT NEW.periodo_inicio)
  OR (date(NEW.periodo_fin, '+0 days') IS NOT NEW.periodo_fin)
  OR (typeof(NEW.monto_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en remuneraciones');
END;
-- siguiente sentencia
CREATE TRIGGER validar_cajas_insert
BEFORE INSERT ON cajas
WHEN (typeof(NEW.saldo_inicial_centimos) <> 'integer')
  OR (NEW.saldo_contado_centimos IS NOT NULL AND (typeof(NEW.saldo_contado_centimos) <> 'integer'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en cajas');
END;
-- siguiente sentencia
CREATE TRIGGER validar_cajas_update
BEFORE UPDATE ON cajas
WHEN (typeof(NEW.saldo_inicial_centimos) <> 'integer')
  OR (NEW.saldo_contado_centimos IS NOT NULL AND (typeof(NEW.saldo_contado_centimos) <> 'integer'))
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en cajas');
END;
-- siguiente sentencia
CREATE TRIGGER validar_pagos_insert
BEFORE INSERT ON pagos
WHEN (typeof(NEW.monto_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en pagos');
END;
-- siguiente sentencia
CREATE TRIGGER validar_pagos_update
BEFORE UPDATE ON pagos
WHEN (typeof(NEW.monto_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en pagos');
END;
-- siguiente sentencia
CREATE TRIGGER validar_comprobantes_insert
BEFORE INSERT ON comprobantes
WHEN (typeof(NEW.total_centimos) <> 'integer')
  OR (typeof(NEW.numero) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en comprobantes');
END;
-- siguiente sentencia
CREATE TRIGGER validar_comprobantes_update
BEFORE UPDATE ON comprobantes
WHEN (typeof(NEW.total_centimos) <> 'integer')
  OR (typeof(NEW.numero) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en comprobantes');
END;
-- siguiente sentencia
CREATE TRIGGER validar_movimientos_caja_insert
BEFORE INSERT ON movimientos_caja
WHEN (typeof(NEW.monto_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en movimientos_caja');
END;
-- siguiente sentencia
CREATE TRIGGER validar_movimientos_caja_update
BEFORE UPDATE ON movimientos_caja
WHEN (typeof(NEW.monto_centimos) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en movimientos_caja');
END;
-- siguiente sentencia
CREATE TRIGGER validar_sorteos_insert
BEFORE INSERT ON sorteos
WHEN (date(NEW.fecha_sorteo, '+0 days') IS NOT NEW.fecha_sorteo)
  OR (date(NEW.periodo_inicio, '+0 days') IS NOT NEW.periodo_inicio)
  OR (date(NEW.periodo_fin, '+0 days') IS NOT NEW.periodo_fin)
  OR (typeof(NEW.minimo_asistencias) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en sorteos');
END;
-- siguiente sentencia
CREATE TRIGGER validar_sorteos_update
BEFORE UPDATE ON sorteos
WHEN (date(NEW.fecha_sorteo, '+0 days') IS NOT NEW.fecha_sorteo)
  OR (date(NEW.periodo_inicio, '+0 days') IS NOT NEW.periodo_inicio)
  OR (date(NEW.periodo_fin, '+0 days') IS NOT NEW.periodo_fin)
  OR (typeof(NEW.minimo_asistencias) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en sorteos');
END;
-- siguiente sentencia
CREATE TRIGGER validar_ganadores_sorteo_insert
BEFORE INSERT ON ganadores_sorteo
WHEN (typeof(NEW.puesto) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en ganadores_sorteo');
END;
-- siguiente sentencia
CREATE TRIGGER validar_ganadores_sorteo_update
BEFORE UPDATE ON ganadores_sorteo
WHEN (typeof(NEW.puesto) <> 'integer')
BEGIN
    SELECT RAISE(ABORT, 'Datos invalidos en ganadores_sorteo');
END;
