#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID:-0} -ne 0 ]]; then
  echo "require root" >&2
  exit 1
fi

# dev servers not recommended in production
systemctl disable --now bs01-web bs01-admin bs01-mobile 2>/dev/null || true

echo "[ok] dev services disabled"
