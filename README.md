# VidSprout Monorepo

VidSprout 是一个视频平台 monorepo，包含双引擎后端（Django / Spring Boot）、Web 前台、管理后台、移动端和部署脚本。

仓库里仍然保留了部分历史命名 `BS01`。当前可以按下面理解：

- 产品名：`VidSprout`
- 仓库历史名/脚本名/服务名：`BS01`
- `bs01-gunicorn.service` 这个名字是历史遗留，实际运行进程已经是 `uvicorn`（ASGI），不是 WSGI Gunicorn

## 文档入口

- 总览：当前文件
- 变更记录：[CHANGELOG.md](/root/BS01/CHANGELOG.md)
- 通用部署说明：[deploy/README.md](/root/BS01/deploy/README.md)
- 当前单机直连 ASGI 部署：[2H2G3M/BACKEND_DEPLOY.md](/root/BS01/2H2G3M/BACKEND_DEPLOY.md)
- 2C2G/3M 部署资料说明：[2H2G3M/README.md](/root/BS01/2H2G3M/README.md)
- 后端模块：[backend/README.md](/root/BS01/backend/README.md)
- Web 前台：[web-client/README.md](/root/BS01/web-client/README.md)
- 管理后台：[admin-console/README.md](/root/BS01/admin-console/README.md)
- 移动端：[mobile_uniapp/README.md](/root/BS01/mobile_uniapp/README.md)

## 目录结构

- `backend/`
  Django + DRF + ASGI，提供 API、认证、通知、视频、WebSocket。
- `backend-springboot/`
  Spring Boot 3.2 + JPA + Spring Security，双引擎实现，API 契约与 Django 版完全一致。
- `web-client/`
  Web 前台。
- `admin-console/`
  管理后台。
- `mobile_uniapp/`
  UniApp 移动端。
- `deploy/`
  通用部署模板与脚本。
- `2H2G3M/`
  当前单机生产资料，带固定路径 `/root/BS01` 的服务模板和脚本。
- `bs01ctl.py`
  统一运维脚本。

## 最近更新

- 新增 Spring Boot 后端引擎，实现与 Django 版完全一致的 API 契约（JWT 鉴权、分片上传、ItemCF 推荐），基于 JVM 并发模型提供更高吞吐量。
- 新增 AI 审核链路，管理端增加审核视图，后端补充自动审核规则与测试。
- Web 与移动端的系统配置同步从"仅轮询"升级为"启动拉取 + 系统事件推送 + 前台恢复补拉"。
- 移动端新增维护模式页，`maintenance_mode` 现已对 H5 与打包 App 生效。
- 根仓库新增 [CHANGELOG.md](/root/BS01/CHANGELOG.md) 记录跨子项目变更。

## 当前建议先这样理解

这个仓库现在有两条部署路线，之前混乱主要就是因为它们被混写了。

### 1. 当前机器实际运行方式

这是你现在这台机器的实际状态，也是当前最直接的一种跑法：

- 后端：`uvicorn backend.asgi:application`
- 监听：`0.0.0.0:8000`
- WebSocket：`/ws/system-events/`
- `nginx`：不是必须，当前可以完全不依赖它
- 前端默认 API 基址：优先显式配置，否则按当前访问域名推导 `api.*`

如果你只是想继续维护这台机器，优先看：

- [2H2G3M/BACKEND_DEPLOY.md](/root/BS01/2H2G3M/BACKEND_DEPLOY.md)

### 2. 通用模板部署方式

`deploy/` 目录里的模板更偏“通用、可渲染、适合别的机器复用”：

- `deploy/systemd/` 是可渲染模板，不绑定固定项目路径或固定用户
- 模板实际运行的是 `uvicorn backend.asgi:application`
- 它既可以直连暴露 `:8000`，也可以放在 `nginx` / 反向代理后面

如果你准备在另一台机器重新部署，优先看：

- [deploy/README.md](/root/BS01/deploy/README.md)

## 当前部署模式对照

| 模式 | 后端监听 | 是否依赖 nginx | 适用场景 |
|---|---|---:|---|
| 直连 ASGI | `0.0.0.0:8000` | 否 | 当前机器、内网调试、快速上线 |
| 反向代理 | 常见为 `0.0.0.0:8000` 或本机回环 | 是 | 域名、80/443、TLS、静态托管 |

重点结论：

- WebSocket 不依赖 `nginx`
- WebSocket 依赖 ASGI
- 如果页面走 `https://`，WebSocket 也必须走 `wss://`，这时通常还是需要反向代理或证书终止层

## 本地开发快速开始

### 1. 安装依赖

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -U pip
pip install -r requirements.txt
```

```bash
cd web-client && npm install
cd ../admin-console && npm install
cd ../mobile_uniapp && npm install
```

### 2. 配置环境变量

```bash
cp deploy/env.example backend/.env
```

至少确认这些变量：

- `SECRET_KEY`
- `DEBUG`
- `ALLOWED_HOSTS`
- `DB_*`
- `REDIS_URL`
- `CORS_ALLOWED_ORIGINS`
- `CSRF_TRUSTED_ORIGINS`

### 3. 初始化数据库

```bash
cd backend
../.venv/bin/python manage.py migrate
../.venv/bin/python manage.py createsuperuser
```

### 4. 启动后端

开发时可以直接：

```bash
cd backend
../.venv/bin/python manage.py runserver 0.0.0.0:8000
```

如果你要验证 WebSocket / 生产形态，更接近线上的是：

```bash
cd /root/BS01
./.venv/bin/uvicorn backend.asgi:application --host 0.0.0.0 --port 8000 --workers 1
```

### 5. 启动前端

```bash
cd web-client
npm run serve
```

```bash
cd admin-console
npm run serve
```

## 运维命令

常用入口是 `bs01ctl.py`：

```bash
python3 bs01ctl.py status
python3 bs01ctl.py restart all
python3 bs01ctl.py logs backend -f -n 200
python3 bs01ctl.py migrate
python3 bs01ctl.py build-frontends
python3 bs01ctl.py deploy-frontend-static
python3 bs01ctl.py doctor
```

说明：

- `setup-services` 默认使用 `deploy/systemd/` 里的通用模板
- `install-nginx-conf` 会安装 `2H2G3M/nginx/bs01.conf`
- 当前机器如果要保持“直连 8000，不依赖 nginx”，不要把 `deploy/systemd/bs01-gunicorn.service` 误当成当前线上状态

## 当前机器的访问方式

按当前仓库里的代码，前端默认会：

- 优先读取显式配置的 API 基址
- 否则根据当前访问域名推导 `api.*`
- WebSocket 会基于当前 API 基址自动换算成 `ws://` 或 `wss://`

需要检查的入口：

- [web-client/src/api.js](/root/BS01/web-client/src/api.js:1)
- [admin-console/src/lib/http.js](/root/BS01/admin-console/src/lib/http.js:1)
- [backend/.env](/root/BS01/backend/.env:1)

## FAQ

### 为什么有 `deploy/` 和 `2H2G3M/` 两套东西？

因为一套是通用模板，一套是这台单机的落地资料。之前两套文档混在一起写，才会显得混乱。

### 现在 WebSocket 到底依不依赖 nginx？

不依赖。依赖的是 ASGI。

### 现在服务名为什么还叫 `bs01-gunicorn`？

只是 unit 名字没改，实际进程已经是 `uvicorn backend.asgi:application`。

### 如果我要重新梳理部署，先看哪份？

- 维护当前机器：看 [2H2G3M/BACKEND_DEPLOY.md](/root/BS01/2H2G3M/BACKEND_DEPLOY.md)
- 部署到新机器：看 [deploy/README.md](/root/BS01/deploy/README.md)
