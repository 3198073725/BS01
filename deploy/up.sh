#!/usr/bin/env bash
# 构建并启动 VidSprout 生产环境（docker compose）
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
    echo "缺少 .env 文件，请先执行: cp .env.example .env 并填写环境变量" >&2
    exit 1
fi

HTTP_PORT="${HTTP_PORT:-80}"

docker compose up -d --build

echo "等待服务健康（最长 5 分钟）..."
for i in $(seq 1 60); do
    if curl -fsS --max-time 3 "http://localhost:${HTTP_PORT}/api/health/" >/dev/null 2>&1; then
        echo "部署成功: http://localhost:${HTTP_PORT}"
        echo "  Web 界面: http://localhost:${HTTP_PORT}/"
        echo "  管理后台: http://localhost:${HTTP_PORT}/admin/"
        exit 0
    fi
    sleep 5
done

echo "服务启动超时，请检查日志: docker compose logs --tail=200 backend" >&2
exit 1
