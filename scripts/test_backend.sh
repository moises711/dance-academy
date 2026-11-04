#!/usr/bin/env bash
set -euo pipefail

cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
jdbc=${1:-target/backend-libs/sqlite-jdbc-3.50.3.0.jar}
if [[ ! -r "$jdbc" ]]; then
    printf 'No se encuentra SQLite JDBC. Uso: %s /ruta/sqlite-jdbc.jar\n' "$0" >&2
    exit 1
fi
jdbc=$(realpath -- "$jdbc")

tmp=$(mktemp -d)
trap 'rm -rf -- "$tmp"' EXIT
mkdir -p "$tmp/classes" "$tmp/run"

python3 -B scripts/test_database.py
javac --release 21 -encoding UTF-8 -d "$tmp/classes" \
    src/main/java/academia/{db,service}/*.java \
    src/test/java/academia/BackendIntegration.java

cd -- "$tmp/run"
java --enable-native-access=ALL-UNNAMED \
    -cp "$tmp/classes:$OLDPWD/src/main/resources:$jdbc" academia.BackendIntegration
