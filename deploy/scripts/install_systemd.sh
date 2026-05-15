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
INCLUDE_FRONTEND_DEV=0
SERVICE_USER=""
SERVICE_GROUP=""
PROJECT_ROOT_OVERRIDE=""
NPM_BIN_OVERRIDE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-frontend)
      SKIP_FE=1
      shift
      ;;
    --include-frontend-dev)
      INCLUDE_FRONTEND_DEV=1
      shift
      ;;
    --service-user)
      SERVICE_USER="${2:-}"
      shift 2
      ;;
    --service-group)
      SERVICE_GROUP="${2:-}"
      shift 2
      ;;
    --project-root)
      PROJECT_ROOT_OVERRIDE="${2:-}"
      shift 2
      ;;
    --npm-bin)
      NPM_BIN_OVERRIDE="${2:-}"
      shift 2
      ;;
    *)
      echo "unknown arg: $1" >&2
      exit 2
      ;;
  esac
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

# 安装并启用生产 systemd 单元（如需前端开发服务，额外传 --include-frontend-dev）
SETUP_ARGS=(setup-services --enable)
if [[ $INCLUDE_FRONTEND_DEV -eq 1 ]]; then
  SETUP_ARGS+=(--include-frontend-dev)
fi
if [[ -n "$SERVICE_USER" ]]; then
  SETUP_ARGS+=(--service-user "$SERVICE_USER")
fi
if [[ -n "$SERVICE_GROUP" ]]; then
  SETUP_ARGS+=(--service-group "$SERVICE_GROUP")
fi
if [[ -n "$PROJECT_ROOT_OVERRIDE" ]]; then
  SETUP_ARGS+=(--project-root "$PROJECT_ROOT_OVERRIDE")
fi
if [[ -n "$NPM_BIN_OVERRIDE" ]]; then
  SETUP_ARGS+=(--npm-bin "$NPM_BIN_OVERRIDE")
fi
python3 "$ROOT/bs01ctl.py" "${SETUP_ARGS[@]}"

# 展示状态
python3 "$ROOT/bs01ctl.py" status
if [[ $INCLUDE_FRONTEND_DEV -eq 1 ]]; then
  python3 "$ROOT/bs01ctl.py" status web admin mobile
fi

# 基本体检
DOCTOR_ARGS=(doctor)
if [[ $INCLUDE_FRONTEND_DEV -eq 1 ]]; then
  DOCTOR_ARGS+=(--include-frontend-dev)
fi
python3 "$ROOT/bs01ctl.py" "${DOCTOR_ARGS[@]}" || true

echo "done"
