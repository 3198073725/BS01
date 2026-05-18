# Current Backend Deployment

这份文档描述的是当前这台机器更接近真实状态的后端部署方式。

## 当前状态

当前仓库对应机器已经按下面方式运行：

- 项目根目录：`/root/BS01`
- 后端进程：`uvicorn backend.asgi:application`
- systemd 服务名：`bs01-gunicorn.service`
- 监听地址：`0.0.0.0:8000`
- HTTP 健康检查：`/api/health/`
- WebSocket：`/ws/system-events/`
- `nginx`：可选，不再是必需依赖

直接访问形式：

- HTTP: `http://<SERVER_IP>:8000`
- API: `http://<SERVER_IP>:8000/api/...`
- WebSocket: `ws://<SERVER_IP>:8000/ws/system-events/`

当前前端默认不会强制写死固定 IP，而是：

- Web 前台优先读取 `VUE_APP_API_BASE` 或 `localStorage.api_base`
- 管理后台优先读取 `localStorage.api_base` 或 `window.__API_BASE__`
- 如果都没有，再按当前访问域名推导 `api.*`

如果服务器域名、IP 或入口方式变化，要同步检查：

- [../web-client/src/api.js](/root/BS01/web-client/src/api.js:1)
- [../admin-console/src/lib/http.js](/root/BS01/admin-console/src/lib/http.js:1)
- [../backend/.env](/root/BS01/backend/.env:1)

## 适用场景

适合：

- 单机部署
- 内网或固定 IP 访问
- 不想依赖 `nginx`
- 需要 WebSocket 直接可用

不适合：

- 你准备直接上公网 `443` 和正式 TLS
- 你需要统一托管多个域名、多个前端入口

这些场景通常还是应该在前面放 `nginx` 或其他反向代理。

## 关键文件

- systemd 模板：
  [systemd/bs01-gunicorn.service](/root/BS01/2H2G3M/systemd/bs01-gunicorn.service:1)
- nginx 可选配置：
  [nginx/bs01.conf](/root/BS01/2H2G3M/nginx/bs01.conf:1)
- 前端静态发布脚本：
  [scripts/deploy_frontend_static.sh](/root/BS01/2H2G3M/scripts/deploy_frontend_static.sh:1)

## 最小部署步骤

### 1. 安装依赖

```bash
cd /root/BS01
python3 -m venv .venv
./.venv/bin/pip install -U pip
./.venv/bin/pip install -r requirements.txt
```

如果需要转码：

```bash
apt install -y ffmpeg redis-server postgresql
```

### 2. 配置环境变量

```bash
cp /root/BS01/2H2G3M/env/backend.env.production.example /root/BS01/backend/.env
```

至少修改这些变量：

- `SECRET_KEY`
- `DEBUG=false`
- `ALLOWED_HOSTS`
- `SITE_URL`
- `DB_*`
- `REDIS_URL`
- `CORS_ALLOWED_ORIGINS`
- `CSRF_TRUSTED_ORIGINS`

如果你继续使用“直连 IP:8000”的方式，`SITE_URL` 和前端允许来源也应该写成对应的 IP 地址与端口。

## 3. 初始化数据库

```bash
cd /root/BS01/backend
../.venv/bin/python manage.py migrate
../.venv/bin/python manage.py createsuperuser
```

## 4. 安装当前机器使用的 systemd 单元

当前这台机器不是用 `deploy/systemd/` 通用模板，而是直接用 `2H2G3M/systemd/` 里的固定路径模板。

安装方式：

```bash
cd /root/BS01
bash 2H2G3M/scripts/install_systemd_units.sh
systemctl daemon-reload
systemctl enable bs01-gunicorn bs01-celery bs01-celery-transcode bs01-celery-beat
systemctl restart bs01-gunicorn bs01-celery bs01-celery-transcode bs01-celery-beat
```

## 5. 验证服务

```bash
systemctl status bs01-gunicorn --no-pager
ss -ltnp | rg ':8000 '
curl -i http://127.0.0.1:8000/api/health/
```

预期：

- `bs01-gunicorn.service` 为 `active (running)`
- `ss` 显示 `0.0.0.0:8000`
- 健康检查返回 `200 OK`

## 6. 验证 WebSocket

只要后端跑的是 ASGI，这条链路就不依赖 `nginx`。

浏览器应连接：

- `ws://<SERVER_IP>:8000/ws/system-events/`

如果要本机验证，最简单的判断标准是后端日志会出现：

- WebSocket accepted / connection open

## 7. 防火墙

如果要让外部机器直接访问，别忘了放行 `8000/tcp`。

UFW 示例：

```bash
ufw allow 8000/tcp
ufw status
```

## 8. 前端说明

当前前端默认 API 基址不是固定 IP，而是“显式配置优先，否则按当前域名推导”。

这意味着：

- 如果你把 `web.` / `admin.` / `mobile.` 与 `api.` 放在同一套域名体系下，通常不用改代码默认值
- 如果你使用非常规入口，仍然可以通过环境变量、`window.__API_BASE__` 或浏览器存储覆盖

重新构建与发布：

```bash
cd /root/BS01/web-client && npm run build
cd /root/BS01/admin-console && npm run build
cd /root/BS01 && python3 bs01ctl.py deploy-frontend-static
```

## 9. 如果以后重新启用 nginx

这不是必须，但在下面场景会有价值：

- 需要 `80/443`
- 需要域名
- 需要 TLS/HTTPS
- 需要把静态文件和 API 统一到一个入口

这时可以使用：

- [nginx/bs01.conf](/root/BS01/2H2G3M/nginx/bs01.conf:1)

但要明确：`nginx` 只是入口层，不是 WebSocket 是否可用的前提。
