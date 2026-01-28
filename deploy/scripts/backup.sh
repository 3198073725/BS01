#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TS="$(date +%F_%H%M%S)"
BACKUP_BASE="${BACKUP_DIR:-/root/BS01-backups}"
DST_DIR="${BACKUP_BASE}/${TS}"

SKIP_DB=0
SKIP_MEDIA=0
SKIP_SYSTEMD=0
SKIP_CODE=0
WITH_BUNDLE=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dir) BACKUP_BASE="$2"; shift 2;;
    --no-db) SKIP_DB=1; shift;;
    --no-media) SKIP_MEDIA=1; shift;;
    --no-systemd) SKIP_SYSTEMD=1; shift;;
    --no-code) SKIP_CODE=1; shift;;
    --no-bundle) WITH_BUNDLE=0; shift;;
    *) echo "unknown arg: $1" >&2; exit 2;;
  esac
done

mkdir -p "$DST_DIR"

# env file
if [[ -f "$ROOT/backend/.env" ]]; then
  install -m 600 "$ROOT/backend/.env" "$DST_DIR/.env"
fi

# code snapshot (exclude heavy caches)
if [[ $SKIP_CODE -eq 0 ]]; then
  PARENT="$(dirname "$ROOT")"; NAME="$(basename "$ROOT")"
  tar -czf "$DST_DIR/code.tgz" \
    --exclude="${NAME}/.git" \
    --exclude="${NAME}/.venv" \
    --exclude="${NAME}/**/node_modules" \
    --exclude="${NAME}/backend/staticfiles" \
    -C "$PARENT" "$NAME"
  if [[ -d "$ROOT/.git" && $WITH_BUNDLE -eq 1 ]]; then
    git -C "$ROOT" bundle create "$DST_DIR/repo.bundle" --all || true
  fi
fi

# media files
if [[ $SKIP_MEDIA -eq 0 && -d "$ROOT/backend/media" ]]; then
  tar -czf "$DST_DIR/media.tgz" -C "$ROOT/backend" media
fi

# systemd units
if [[ $SKIP_SYSTEMD -eq 0 ]]; then
  SYS_DIR="/etc/systemd/system"
  to_pack=()
  for u in bs01-gunicorn.service bs01-web.service bs01-admin.service bs01-mobile.service bs01-celery.service bs01-celery-beat.service; do
    [[ -f "$SYS_DIR/$u" ]] && to_pack+=("$u") || true
  done
  if [[ ${#to_pack[@]} -gt 0 ]]; then
    tar -czf "$DST_DIR/systemd.tgz" -C "$SYS_DIR" "${to_pack[@]}"
  fi
fi

# database dump (PostgreSQL)
if [[ $SKIP_DB -eq 0 ]]; then
  if [[ -f "$DST_DIR/.env" ]]; then
    set +u
    DB_ENGINE=$(grep -E '^DB_ENGINE=' "$DST_DIR/.env" | tail -n1 | cut -d= -f2- || true)
    DB_NAME=$(grep -E '^DB_NAME=' "$DST_DIR/.env" | tail -n1 | cut -d= -f2- || true)
    DB_USER=$(grep -E '^DB_USER=' "$DST_DIR/.env" | tail -n1 | cut -d= -f2- || true)
    DB_PASSWORD=$(grep -E '^DB_PASSWORD=' "$DST_DIR/.env" | tail -n1 | cut -d= -f2- || true)
    DB_HOST=$(grep -E '^DB_HOST=' "$DST_DIR/.env" | tail -n1 | cut -d= -f2- || echo 127.0.0.1)
    DB_PORT=$(grep -E '^DB_PORT=' "$DST_DIR/.env" | tail -n1 | cut -d= -f2- || echo 5432)
    set -u
    if [[ "${DB_ENGINE:-django.db.backends.postgresql}" == "django.db.backends.postgresql" && -n "${DB_NAME:-}" && -n "${DB_USER:-}" ]]; then
      export PGPASSWORD="${DB_PASSWORD:-}"
      pg_dump -Fc -U "$DB_USER" -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" "$DB_NAME" > "$DST_DIR/db.dump" || {
        echo "warn: pg_dump failed" >&2
        rm -f "$DST_DIR/db.dump" || true
      }
      unset PGPASSWORD || true
    fi
  fi
fi

# checksums
(
  cd "$DST_DIR"
  shopt -s nullglob
  sha256sum * > SHA256SUMS || true
)

ln -sfn "$DST_DIR" "${BACKUP_BASE}/latest"

cat <<EOF
backup done:
  dir: $DST_DIR
  files:
    $(ls -1 "$DST_DIR" | sed 's/^/    - /')
EOF
