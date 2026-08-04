#!/usr/bin/env bash
# 停止 VidSprout 生产环境（保留数据卷）
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose down
