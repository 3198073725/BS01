#!/usr/bin/env bash
set -euo pipefail

ROOT="/root/BS01"
NG_SRC="$ROOT/2H2G3M/nginx/bs01.conf"
NG_AVAIL="/etc/nginx/sites-available/bs01.conf"
NG_ENABLED="/etc/nginx/sites-enabled/bs01.conf"

if [[ ${EUID:-0} -ne 0 ]]; then
  echo "require root" >&2
  exit 1
fi

if [[ ! -f "$NG_SRC" ]]; then
  echo "missing $NG_SRC" >&2
  exit 2
fi

cp "$NG_SRC" "$NG_AVAIL"
ln -sf "$NG_AVAIL" "$NG_ENABLED"

nginx -t

echo "[ok] nginx conf installed. run: systemctl reload nginx"
