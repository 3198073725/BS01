#!/usr/bin/env bash
set -euo pipefail

ROOT="/root/BS01"
UNIT_SRC="$ROOT/2H2G3M/systemd"
UNIT_DST="/etc/systemd/system"

if [[ ${EUID:-0} -ne 0 ]]; then
  echo "require root" >&2
  exit 1
fi

for f in bs01-gunicorn.service bs01-celery.service bs01-celery-transcode.service; do
  if [[ ! -f "$UNIT_SRC/$f" ]]; then
    echo "missing unit: $UNIT_SRC/$f" >&2
    exit 2
  fi
  cp "$UNIT_SRC/$f" "$UNIT_DST/$f"
done

systemctl daemon-reload

echo "[ok] units installed. You can run: systemctl enable --now bs01-gunicorn bs01-celery"
