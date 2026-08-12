#!/bin/sh
# Start script for VidSprout showcase container
set -e

cd /app
mkdir -p /app/logs

echo "[startup] fixing permissions..."
chown -R postgres:postgres /var/lib/postgresql/data /run/postgresql 2>/dev/null || true

echo "[startup] starting PostgreSQL..."
su - postgres -c "pg_ctl -D /var/lib/postgresql/data -o '-c shared_buffers=32MB -c max_connections=10 -c listen_addresses=127.0.0.1' -l /dev/null start" >/dev/null 2>&1
for i in $(seq 1 20); do
    su - postgres -c "pg_isready -q" && break
    sleep 1
done
echo "[startup] PostgreSQL ready"

echo "[startup] starting Redis..."
redis-server /etc/redis.conf --port 6379 --bind 127.0.0.1 --daemonize no &
sleep 2
echo "[startup] Redis ready"

echo "[startup] starting Spring Boot..."
# JWT secret: prefer injected env; otherwise generate a fresh random one per container
# (the container is stateless, so tokens are invalidated on rebuild anyway).
JWT_SECRET="${JWT_SECRET:-$(head -c 48 /dev/urandom | base64 | tr -d '[:space:]')}"
DB_USER="${DB_USER:-bs01}"
DB_PASSWORD="${DB_PASSWORD:-bs01}"
JWT_SECRET="$JWT_SECRET" \
SPRING_PROFILES_ACTIVE=prod \
DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/bs01}" \
DB_USER="$DB_USER" \
DB_PASSWORD="$DB_PASSWORD" \
REDIS_HOST=127.0.0.1 \
REDIS_PASSWORD="${REDIS_PASSWORD:-}" \
java -Xms64m -Xmx128m -XX:MaxRAMPercentage=50.0 -jar /app/app.jar > /app/logs/backend.log 2>&1 &
APP_PID=$!

echo "[startup] waiting for backend to be ready..."
READY=0
for i in $(seq 1 60); do
    if curl -fsS http://127.0.0.1:8000/api/health/ >/dev/null 2>&1; then
        READY=1
        break
    fi
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        echo "[startup] backend process exited unexpectedly"
        cat /app/logs/backend.log 2>/dev/null | tail -50 || true
        exit 1
    fi
    sleep 1
done

if [ "$READY" != "1" ]; then
    echo "[startup] backend failed to become ready in time"
    tail -100 /app/logs/backend.log 2>/dev/null || true
    exit 1
fi
echo "[startup] backend ready"

echo "[startup] starting nginx..."
mkdir -p /run/nginx
nginx &
sleep 1
echo "[startup] nginx ready"

wait "$APP_PID"
