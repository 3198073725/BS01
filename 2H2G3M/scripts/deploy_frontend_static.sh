#!/usr/bin/env bash
set -euo pipefail

ROOT="/root/BS01"
WEB_DST="/var/www/bs01/web"
ADMIN_DST="/var/www/bs01/admin"

if [[ ${EUID:-0} -ne 0 ]]; then
  echo "require root" >&2
  exit 1
fi

mkdir -p "$WEB_DST" "$ADMIN_DST"
rm -rf "${WEB_DST:?}"/*
rm -rf "${ADMIN_DST:?}"/*

# web-client
if [[ ! -d "$ROOT/web-client/dist" ]]; then
  echo "missing $ROOT/web-client/dist, run build_frontends.sh first" >&2
  exit 2
fi
cp -r "$ROOT/web-client/dist"/* "$WEB_DST"/

# admin-console
if [[ ! -d "$ROOT/admin-console/dist" ]]; then
  echo "missing $ROOT/admin-console/dist, run build_frontends.sh first" >&2
  exit 3
fi
cp -r "$ROOT/admin-console/dist"/* "$ADMIN_DST"/

echo "[ok] static deployed to $WEB_DST and $ADMIN_DST"
