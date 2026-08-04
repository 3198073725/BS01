#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend-springboot"
WEB_DIR="$SCRIPT_DIR/web-client"

echo "=== 启动 Spring Boot 后端 (端口 8000) ==="
cd "$BACKEND_DIR"
mvn -o spring-boot:run -Dspring-boot.run.profiles=dev &
BACKEND_PID=$!

echo "=== 等待后端就绪 ==="
for i in $(seq 1 30); do
    if curl -s http://localhost:8000/api/users/ping/ > /dev/null 2>&1; then
        echo "后端已就绪"
        break
    fi
    sleep 2
done

echo "=== 启动 web-client 前端 (端口 8080) ==="
cd "$WEB_DIR"
npm run serve &
WEB_PID=$!

trap "kill $BACKEND_PID $WEB_PID 2>/dev/null; exit" EXIT INT TERM

echo "=== 服务已启动 ==="
echo "  前端: http://localhost:8080"
echo "  后端: http://localhost:8000"
echo ""

wait
