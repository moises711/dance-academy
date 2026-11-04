"""Crea la base local sin borrar datos existentes. Ejecutar desde cualquier carpeta."""
from pathlib import Path
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = (ROOT / 'src/main/resources/academia/schema.sql').read_text(encoding='utf-8')

def initialize(connection):
    connection.execute('PRAGMA foreign_keys = OFF')
    columns = {row[1] for row in connection.execute('PRAGMA table_info(alumnos)')}
    migration = ''
    if 'codigo_qr' in columns:
        migration = (ROOT / 'src/main/resources/academia/migration-v2.sql').read_text(encoding='utf-8')
    try:
        connection.executescript('BEGIN IMMEDIATE;\n' + migration + '\n' + SCHEMA)
        if connection.execute('PRAGMA foreign_key_check').fetchall():
            raise RuntimeError('Relaciones invalidas en la migracion')
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.execute('PRAGMA foreign_keys = ON')

if __name__ == '__main__':
    path = ROOT / 'data/academia.db'
    path.parent.mkdir(parents=True, exist_ok=True)
    with sqlite3.connect(path) as connection:
        initialize(connection)
        assert connection.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
        assert not connection.execute('PRAGMA foreign_key_check').fetchall()
        tables = connection.execute("SELECT count(*) FROM sqlite_master WHERE type='table'").fetchone()[0]
    print(f'Base creada: {path} ({tables} tablas)')
