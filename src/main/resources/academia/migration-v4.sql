CREATE TABLE usuarios (
    id INTEGER PRIMARY KEY,
    nombre TEXT NOT NULL UNIQUE COLLATE NOCASE,
    sal TEXT NOT NULL,
    clave_hash TEXT NOT NULL,
    rol TEXT NOT NULL CHECK (rol IN ('ADMIN', 'OPERADOR', 'CONSULTA')),
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);
CREATE TABLE auditoria (
    id INTEGER PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    operacion TEXT NOT NULL,
    entidad_id INTEGER,
    fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE solicitudes_cobro (
    clave TEXT PRIMARY KEY,
    pago_id INTEGER NOT NULL UNIQUE REFERENCES pagos(id)
);
