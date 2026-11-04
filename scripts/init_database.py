"""Crea la base local sin borrar datos existentes. Ejecutar desde cualquier carpeta."""
from pathlib import Path
import os
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / 'src/main/resources/academia'
VERSION = 4


def initialize(connection):
    if connection.in_transaction:
        raise RuntimeError('Inicializa SQLite fuera de una transacción existente')
    connection.execute('PRAGMA busy_timeout = 5000')
    connection.execute('PRAGMA foreign_keys = OFF')
    try:
        connection.execute('BEGIN IMMEDIATE')
        version = connection.execute('PRAGMA user_version').fetchone()[0]
        if version > VERSION:
            raise RuntimeError('La base requiere una versión más reciente de la aplicación')
        if version < 3:
            columns = {row[1] for row in connection.execute('PRAGMA table_info(alumnos)')}
            if 'codigo_qr' in columns:
                run_script(connection, 'migration-v2.sql', ';')
            run_script(connection, 'schema.sql', ';')
            run_script(connection, 'backfill-alumnos.sql', ';')
            run_script(connection, 'migration-v3.sql', '-- siguiente sentencia')
        if version < VERSION:
            run_script(connection, 'migration-v4.sql', ';')
            connection.execute(f'PRAGMA user_version = {VERSION}')
        if connection.execute('PRAGMA foreign_key_check').fetchone():
            raise RuntimeError('Relaciones inválidas en la base')
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.execute('PRAGMA foreign_keys = ON')


def run_script(connection, name, separator):
    for statement in (SQL / name).read_text(encoding='utf-8').split(separator):
        if statement.strip():
            connection.execute(statement)


def private_data_directory(directory):
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    if directory.is_symlink() or not directory.is_dir():
        raise ValueError('La carpeta de datos debe ser un directorio local')
    if os.name == 'posix':
        directory.chmod(0o700)


if __name__ == '__main__':
    path = ROOT / 'data/academia.db'
    private_data_directory(path.parent)
    if path.is_symlink() or (path.exists() and not path.is_file()):
        raise ValueError('La base de datos debe ser un archivo regular')
    if os.name == 'posix':
        fd = os.open(path, os.O_CREAT | os.O_WRONLY | os.O_NOFOLLOW, 0o600)
        try:
            os.fchmod(fd, 0o600)
        finally:
            os.close(fd)
    with sqlite3.connect(path) as connection:
        initialize(connection)
        assert connection.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
        assert not connection.execute('PRAGMA foreign_key_check').fetchall()
        tables = connection.execute("SELECT count(*) FROM sqlite_master WHERE type='table'").fetchone()[0]
    print(f'Base creada: {path} ({tables} tablas)')
