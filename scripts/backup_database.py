"""Genera una copia local verificada con permisos privados."""
import argparse
from contextlib import closing
from datetime import datetime, timezone
from pathlib import Path
import sqlite3
import tempfile

from init_database import ROOT


def backup(source: Path, directory: Path) -> Path:
    timestamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    with closing(sqlite3.connect(source.resolve().as_uri() + '?mode=ro', uri=True)) as original:
        with tempfile.NamedTemporaryFile(prefix=f'academia-{timestamp}-', suffix='.db',
                                         dir=directory, delete=False) as file:
            target = Path(file.name)
        try:
            with closing(sqlite3.connect(target)) as copy:
                original.backup(copy)
                if copy.execute('PRAGMA integrity_check').fetchall() != [('ok',)]:
                    raise RuntimeError('La copia no supera la comprobación de integridad')
                if copy.execute('PRAGMA foreign_key_check').fetchone():
                    raise RuntimeError('La copia contiene relaciones inválidas')
        except BaseException:
            target.unlink(missing_ok=True)
            raise
    return target


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--restaurar', type=Path, metavar='RESPALDO',
                        help='valida y prepara una copia de recuperación sin sobrescribir la base activa')
    args = parser.parse_args()
    data = ROOT / 'data'
    source = data / 'academia.db' if args.restaurar is None else args.restaurar
    print(backup(source, data))
