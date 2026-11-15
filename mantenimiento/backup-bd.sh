#!/usr/bin/env bash
set -euo pipefail

# Respaldo lógico preventivo de PostgreSQL / Supabase (solo lectura, formato custom).
# Uso:
#   ./mantenimiento/backup-bd.sh            # Ejecuta respaldo real
#   ./mantenimiento/backup-bd.sh --dry-run  # Valida parámetros sin escribir archivo

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DRY_RUN="false"
BACKUP_DIR="${BACKUP_DIR:-${SCRIPT_DIR}/backups}"

if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN="true"
fi

if [[ -f "${PROJECT_ROOT}/.env" ]]; then
  while IFS='=' read -r key val; do
    [[ -z "${key}" || "${key}" == \#* ]] && continue
    key="${key// /}"
    if [[ -z "${!key:-}" ]]; then
      export "${key}=${val}"
    fi
  done < "${PROJECT_ROOT}/.env"
fi

DB_URL_VALUE="${DB_URL:-jdbc:postgresql://localhost:5432/maricielojoyas}"
PGUSER_VALUE="${DB_USER:-postgres}"

if [[ ! "${DB_URL_VALUE}" =~ ^jdbc:postgresql://([^:/?]+)(:([0-9]+))?/([^?]+) ]]; then
  echo "ERROR: DB_URL no cumple el formato jdbc:postgresql://host:puerto/base" >&2
  exit 1
fi

PGHOST_VALUE="${BASH_REMATCH[1]}"
PGPORT_VALUE="${BASH_REMATCH[3]:-5432}"
PGDATABASE_VALUE="${BASH_REMATCH[4]}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_FILE="${BACKUP_DIR}/cielojoyas-${TIMESTAMP}.dump"

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "[DryRun] Archivo destino: ${OUTPUT_FILE}"
  echo "[DryRun] pg_dump --format=custom --no-owner --no-privileges --host=${PGHOST_VALUE} --port=${PGPORT_VALUE} --username=${PGUSER_VALUE} --dbname=${PGDATABASE_VALUE} --file=${OUTPUT_FILE}"
  exit 0
fi

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "ERROR: pg_dump no está instalado o no figura en PATH." >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}"
cleanup() {
  unset PGPASSWORD || true
}
trap cleanup EXIT

if [[ -n "${DB_PASSWORD:-}" ]]; then
  export PGPASSWORD="${DB_PASSWORD}"
fi

pg_dump \
  --format=custom \
  --no-owner \
  --no-privileges \
  --host="${PGHOST_VALUE}" \
  --port="${PGPORT_VALUE}" \
  --username="${PGUSER_VALUE}" \
  --dbname="${PGDATABASE_VALUE}" \
  --file="${OUTPUT_FILE}"

echo "Respaldo generado correctamente en: ${OUTPUT_FILE}"
