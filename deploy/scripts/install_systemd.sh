#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

if [[ ${EUID:-0} -ne 0 ]]; then
  echo "require root" >&2
  exit 1
fi

if [[ ! -f "$ROOT/backend/.env" ]]; then
  echo "missing $ROOT/backend/.env" >&2
  exit 2
fi

if [[ ! -d "$ROOT/.venv" ]]; then
  python3 -m venv "$ROOT/.venv"
fi
"$ROOT/.venv/bin/python" -m pip install -U pip
"$ROOT/.venv/bin/pip" install -r "$ROOT/requirements.txt"

# 前端依赖安装（如不需要可传 --skip-frontend 给本脚本）
SKIP_FE=0
for a in "$@"; do
  [[ "$a" == "--skip-frontend" ]] && SKIP_FE=1 || true
done
if [[ $SKIP_FE -eq 0 ]]; then
  if [[ -f "$ROOT/web-client/package.json" ]]; then
    npm --prefix "$ROOT/web-client" ci --no-audit --no-fund || npm --prefix "$ROOT/web-client" i --no-audit --no-fund
  fi
  if [[ -f "$ROOT/admin-console/package.json" ]]; then
    npm --prefix "$ROOT/admin-console" ci --no-audit --no-fund || true
  fi
fi

# 数据库迁移
"$ROOT/.venv/bin/python" "$ROOT/backend/manage.py" migrate

# 安装并启用 systemd 单元
python3 "$ROOT/bs01ctl.py" setup-services --enable

# 展示状态
python3 "$ROOT/bs01ctl.py" status

# 基本体检
python3 "$ROOT/bs01ctl.py" doctor || true

echo "done"
