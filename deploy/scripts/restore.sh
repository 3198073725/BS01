#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID:-0} -ne 0 ]]; then
  echo "require root" >&2
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SRC_DIR="${SRC:-/root/BS01-backups/latest}"
WITH_DB=0
WIPE_MEDIA=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --src) SRC_DIR="$2"; shift 2;;
    --with-db) WITH_DB=1; shift;;
    --wipe-media) WIPE_MEDIA=1; shift;;
    *) echo "unknown arg: $1" >&2; exit 2;;
  esac
done

if [[ ! -d "$SRC_DIR" ]]; then
  echo "backup dir not found: $SRC_DIR" >&2
  exit 2
fi

stop_units=(bs01-gunicorn.service bs01-celery.service bs01-celery-beat.service bs01-web.service bs01-admin.service)
for u in "${stop_units[@]}"; do
  systemctl stop "$u" 2>/dev/null || true
done

# restore .env
if [[ -f "$SRC_DIR/.env" ]]; then
  install -m 600 "$SRC_DIR/.env" "$ROOT/backend/.env"
  echo "restored: backend/.env"
else
  echo "warn: $SRC_DIR/.env not found; keep existing .env if any" >&2
fi

# restore media
if [[ -f "$SRC_DIR/media.tgz" ]]; then
  if [[ $WIPE_MEDIA -eq 1 && -d "$ROOT/backend/media" ]]; then
    rm -rf "$ROOT/backend/media"
  fi
  mkdir -p "$ROOT/backend/media"
  tar -xzf "$SRC_DIR/media.tgz" -C "$ROOT/backend"
  echo "restored: media"
else
  echo "info: media.tgz not found; skip media"
fi

# optional: restore database (PostgreSQL custom format dump)
if [[ $WITH_DB -eq 1 ]]; then
  if [[ -f "$SRC_DIR/db.dump" ]]; then
    if [[ -f "$ROOT/backend/.env" ]]; then
      set +u
      DB_ENGINE=$(grep -E '^DB_ENGINE=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)
      DB_NAME=$(grep -E '^DB_NAME=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)
      DB_USER=$(grep -E '^DB_USER=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)
      DB_PASSWORD=$(grep -E '^DB_PASSWORD=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)
      DB_HOST=$(grep -E '^DB_HOST=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || echo 127.0.0.1)
      DB_PORT=$(grep -E '^DB_PORT=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || echo 5432)
      set -u
      if [[ "${DB_ENGINE:-django.db.backends.postgresql}" == "django.db.backends.postgresql" && -n "${DB_NAME:-}" && -n "${DB_USER:-}" ]]; then
        export PGPASSWORD="${DB_PASSWORD:-}"
        # ensure role/database exist
        sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
DO $$ BEGIN
IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${DB_USER}') THEN
  CREATE ROLE "${DB_USER}" LOGIN PASSWORD '${DB_PASSWORD}';
END IF;
END $$;
DO $$ BEGIN
IF NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DB_NAME}') THEN
  CREATE DATABASE "${DB_NAME}" OWNER "${DB_USER}";
END IF;
END $$;
ALTER DATABASE "${DB_NAME}" OWNER TO "${DB_USER}";
SQL
        pg_restore -c -U "$DB_USER" -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" "$SRC_DIR/db.dump"
        unset PGPASSWORD || true
        echo "restored: database ${DB_NAME}"
      else
        echo "warn: invalid DB_* in .env; skip db restore" >&2
      fi
    else
      echo "warn: backend/.env missing; skip db restore" >&2
    fi
  else
    echo "info: db.dump not found; skip db restore"
  fi
else
  echo "info: --with-db not set; skip db restore"
fi

# migrations
"$ROOT/.venv/bin/python" "$ROOT/backend/manage.py" migrate || true

# start units
systemctl daemon-reload || true
for u in "${stop_units[@]}"; do
  systemctl enable "$u" 2>/dev/null || true
  systemctl start "$u" 2>/dev/null || true
done

# basic doctor
python3 "$ROOT/bs01ctl.py" doctor || true

echo "restore done: $SRC_DIR"
