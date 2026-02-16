#!/usr/bin/env bash
set -euo pipefail

ROOT="/root/BS01"

echo "[web-client] build"
cd "$ROOT/web-client"
npm ci --no-audit --no-fund || npm i --no-audit --no-fund
npm run build

echo "[admin-console] build"
cd "$ROOT/admin-console"
npm ci --no-audit --no-fund || npm i --no-audit --no-fund
npm run build

echo "[ok] frontends built"
