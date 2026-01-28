#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID:-0} -ne 0 ]]; then
  echo "require root" >&2
  exit 1
fi

NODE_VERSION="${NODE_VERSION:-v18.20.8}"

if command -v apt-get >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y curl ca-certificates gnupg lsb-release software-properties-common
  apt-get install -y python3-venv python3-dev build-essential git ffmpeg redis-server postgresql postgresql-contrib libpq-dev iproute2 net-tools tar xz-utils
  if ! command -v node >/dev/null 2>&1 || [[ "$(node -v 2>/dev/null || true)" != "$NODE_VERSION" ]]; then
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y nodejs
  fi
  if [[ "$(node -v 2>/dev/null || true)" != "$NODE_VERSION" ]]; then
    tmpdir="$(mktemp -d)"
    cd "$tmpdir"
    curl -fsSLO "https://nodejs.org/dist/${NODE_VERSION}/node-${NODE_VERSION}-linux-x64.tar.xz"
    tar -xJf "node-${NODE_VERSION}-linux-x64.tar.xz"
    install -m 0755 "node-${NODE_VERSION}-linux-x64/bin/node" /usr/local/bin/node
    install -m 0755 "node-${NODE_VERSION}-linux-x64/bin/npm" /usr/local/bin/npm
    install -m 0755 "node-${NODE_VERSION}-linux-x64/bin/npx" /usr/local/bin/npx
    ln -sf /usr/local/bin/node /usr/bin/node || true
    ln -sf /usr/local/bin/npm /usr/bin/npm || true
    ln -sf /usr/local/bin/npx /usr/bin/npx || true
    cd /
    rm -rf "$tmpdir"
  fi
  systemctl enable --now redis-server || true
  systemctl enable --now postgresql || true
else
  echo "unsupported distro" >&2
  exit 1
fi

if [[ "${1:-}" == "--db-from-env" ]]; then
  ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
  if [[ -f "$ROOT/backend/.env" ]]; then
    set +u
    DB_ENGINE="$(grep -E '^DB_ENGINE=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)"
    DB_NAME="$(grep -E '^DB_NAME=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)"
    DB_USER="$(grep -E '^DB_USER=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)"
    DB_PASSWORD="$(grep -E '^DB_PASSWORD=' "$ROOT/backend/.env" | tail -n1 | cut -d= -f2- || true)"
    set -u
    if [[ "${DB_ENGINE:-django.db.backends.postgresql}" == "django.db.backends.postgresql" ]] && [[ -n "${DB_NAME:-}" && -n "${DB_USER:-}" ]]; then
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
    fi
  fi
fi

echo "node: $(node -v 2>/dev/null || echo not-installed)"
echo "npm: $(npm -v 2>/dev/null || echo not-installed)"
echo "redis: $(redis-server --version 2>/dev/null || echo not-installed)"
echo "psql: $(psql --version 2>/dev/null || echo not-installed)"
