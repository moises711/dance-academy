"""Prueba aislada de persistencia, restricciones y consultas del esquema."""
import sqlite3
from init_database import initialize

c = sqlite3.connect(':memory:')
initialize(c)
c.execute("INSERT INTO estudiantes(id,nombre) VALUES(1,'Ana')")
c.execute("INSERT INTO alumnos(estudiante_id,dni) VALUES(1,'12345678')")
c.execute("INSERT INTO planes VALUES(1,'Mensual','mensualidad',30,NULL,12000,1)")
c.execute("INSERT INTO matriculas(id,alumno_id,plan_id,fecha_inicio,fecha_vencimiento,precio_centimos) VALUES(1,1,1,'2026-11-01','2026-11-30',12000)")
c.execute("INSERT INTO congelaciones(matricula_id,fecha_inicio,motivo) VALUES(1,'2026-11-04','Viaje')")
c.execute("INSERT INTO pagos(id,matricula_id,concepto,monto_centimos,metodo) VALUES(1,1,'mensualidad',12000,'efectivo')")
c.execute("INSERT INTO cajas(id,responsable) VALUES(1,'Admin')")
c.execute("INSERT INTO movimientos_caja(caja_id,tipo,concepto,monto_centimos,metodo,pago_id) VALUES(1,'ingreso','Mensualidad',12000,'efectivo',1)")
c.execute("INSERT INTO movimientos_caja(caja_id,tipo,concepto,monto_centimos,metodo) VALUES(1,'egreso','Limpieza',2000,'efectivo')")
assert c.execute('SELECT saldo_pendiente_centimos FROM v_matriculas').fetchone()[0] == 0
assert c.execute('SELECT efectivo_esperado_centimos FROM v_balance_cajas').fetchone()[0] == 10000
for sql in [
    "INSERT INTO alumnos(estudiante_id) VALUES(999)",
    "INSERT INTO congelaciones(matricula_id,fecha_inicio,motivo) VALUES(1,'2026-11-05','Duplicada')",
    "INSERT INTO pagos(matricula_id,concepto,monto_centimos,metodo) VALUES(1,'mensualidad',-1,'efectivo')",
    "INSERT INTO ganadores_sorteo VALUES(999,1,1,CURRENT_TIMESTAMP)",
]:
    try:
        c.execute(sql)
    except sqlite3.IntegrityError:
        pass
    else:
        raise AssertionError(f'Restriccion no aplicada: {sql}')
c.commit()
initialize(c)
assert c.execute('SELECT nombre FROM estudiantes').fetchone()[0] == 'Ana'
for (name,) in c.execute("SELECT name FROM sqlite_master WHERE type='view'").fetchall():
    c.execute(f'SELECT * FROM {name}').fetchall()
assert c.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
assert not c.execute('PRAGMA foreign_key_check').fetchall()
print('OK: relaciones, importes, congelaciones, caja, vistas y reinicializacion sin perdida de datos')

# El codigo conserva ceros iniciales y rechaza formatos invalidos y duplicados.
c.execute("UPDATE alumnos SET codigo='0007' WHERE estudiante_id=1")
for code in ['123', '12345', '12a4', ' 123', '１２３４']:
    try:
        c.execute('UPDATE alumnos SET codigo=? WHERE estudiante_id=1', (code,))
    except sqlite3.IntegrityError:
        pass
    else:
        raise AssertionError(f'Codigo invalido aceptado: {code}')
c.execute("INSERT INTO estudiantes(id,nombre) VALUES(2,'Luis')")
try:
    c.execute("INSERT INTO alumnos(estudiante_id,codigo) VALUES(2,'0007')")
except sqlite3.IntegrityError:
    pass
else:
    raise AssertionError('Codigo duplicado aceptado')
assert c.execute('SELECT codigo FROM alumnos WHERE estudiante_id=1').fetchone()[0] == '0007'
# Simula una base anterior con QR y relaciones existentes.
c.commit()
c.execute('PRAGMA foreign_keys=OFF')
c.execute('ALTER TABLE alumnos RENAME COLUMN codigo TO codigo_qr')
c.commit()
initialize(c)
assert 'codigo_qr' not in {r[1] for r in c.execute('PRAGMA table_info(alumnos)')}
assert c.execute('SELECT codigo FROM alumnos WHERE estudiante_id=1').fetchone()[0] == '0000'
assert c.execute('SELECT alumno_id FROM matriculas').fetchone()[0] == 1
assert not c.execute('PRAGMA foreign_key_check').fetchall()
print('OK: codigos de cuatro digitos, unicidad y migracion de QR con relaciones existentes')
