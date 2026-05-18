# Deployment Guide

这份文档描述的是 `deploy/` 目录下的通用部署模板，不是当前机器的唯一真实状态。

先看清这一点：

- `deploy/systemd/` 是通用模板
- 模板实际运行的是 `uvicorn backend.asgi:application`
- 它既可以直接监听 `:8000`，也可以放在 `nginx`、反向代理或其他入口层后面
- 如果你要“后端直接对外监听 `0.0.0.0:8000`，完全不依赖 nginx”，优先看 [../2H2G3M/BACKEND_DEPLOY.md](/root/BS01/2H2G3M/BACKEND_DEPLOY.md)

## 目录说明

- `env.example`
  后端 `.env` 示例。
- `systemd/`
  生产服务模板。
- `systemd-dev/`
  可选的前端开发服务模板。
- `scripts/`
  安装系统依赖、安装 systemd、备份、恢复脚本。

## 这套模板适合什么场景

适合：

- 你要部署到新机器
- 你需要可渲染的 systemd 模板
- 你可能会使用自定义项目路径、用户或组
- 你可能会把服务放到 `nginx` 或其他入口层后面

不适合直接照搬的场景：

- 你只想完全复刻当前 `/root/BS01` 这台机器的固定路径部署

## 关键环境变量

参考 [env.example](/root/BS01/deploy/env.example:1)。

核心变量：

- `SECRET_KEY`
- `DEBUG`
- `ALLOWED_HOSTS`
- `SITE_URL`
- `MEDIA_URL`
- `DB_ENGINE`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_HOST`
- `DB_PORT`
- `REDIS_URL`
- `CELERY_BROKER_URL`
- `CELERY_RESULT_BACKEND`
- `CORS_ALLOWED_ORIGINS`
- `CSRF_TRUSTED_ORIGINS`

如果前端域名和 API 域名不一致，`CORS_ALLOWED_ORIGINS` 与 `CSRF_TRUSTED_ORIGINS` 一定要配置正确。

## 通用最小部署流程

### 1. 安装系统依赖

Ubuntu 示例：

```bash
apt install -y python3-venv python3-dev build-essential nginx redis-server
```

如果机器还负责转码，额外安装：

```bash
apt install -y ffmpeg
```

### 2. 创建虚拟环境并安装依赖

```bash
PROJECT_ROOT=/srv/vidsprout
python3 -m venv "$PROJECT_ROOT/.venv"
"$PROJECT_ROOT/.venv/bin/pip" install -U pip
"$PROJECT_ROOT/.venv/bin/pip" install -r "$PROJECT_ROOT/requirements.txt"
```

### 3. 配置 `.env`

```bash
cp deploy/env.example backend/.env
```

然后修改：

- `SECRET_KEY`
- `DB_*`
- `REDIS_URL`
- `SITE_URL`
- `ALLOWED_HOSTS`
- `CORS_ALLOWED_ORIGINS`
- `CSRF_TRUSTED_ORIGINS`

### 4. 初始化数据库

```bash
cd "$PROJECT_ROOT/backend"
"$PROJECT_ROOT/.venv/bin/python" manage.py migrate
"$PROJECT_ROOT/.venv/bin/python" manage.py createsuperuser
```

### 5. 安装 systemd 单元

推荐用 `bs01ctl.py` 渲染安装：

```bash
python3 bs01ctl.py setup-services --enable
```

常见覆盖方式：

```bash
python3 bs01ctl.py setup-services --service-user bs01 --service-group bs01
python3 bs01ctl.py setup-services --project-root /srv/vidsprout
```

也可以使用脚本：

```bash
deploy/scripts/install_systemd.sh --service-user bs01 --service-group bs01
```

### 6. 启动服务

```bash
systemctl daemon-reload
systemctl enable bs01-gunicorn bs01-celery bs01-celery-transcode bs01-celery-beat
systemctl start bs01-gunicorn bs01-celery bs01-celery-transcode bs01-celery-beat
```

## systemd 模板说明

`deploy/systemd/` 中包含：

- `bs01-gunicorn.service`
- `bs01-celery.service`
- `bs01-celery-transcode.service`
- `bs01-celery-beat.service`

注意：

- `bs01-gunicorn.service` 这个名字是历史遗留
- 模板实际运行的是 `uvicorn backend.asgi:application`
- 这保证了 `/ws/system-events/` 可以工作

## nginx 说明

如果你选择反向代理模式，通常前面还要配 `nginx`：

- `/api/` 反代到 `127.0.0.1:8000`
- `/ws/` 反代到 `127.0.0.1:8000`
- `/media/` 由 nginx 直出或转到对象存储/CDN

WebSocket 代理必须带：

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

## WebSocket 说明

项目的系统配置同步使用：

- `/ws/system-events/`

因此：

- 后端必须跑 ASGI
- 不能退回 `backend.wsgi:application`
- 只要入口层正确转发，WebSocket 不要求必须使用 nginx，本质上要求的是 ASGI 链路可达

## 与当前机器的差异

当前仓库里的实际机器使用的是固定路径的 `2H2G3M/` 资料。

而这份 `deploy/README.md` 描述的是更通用的“可渲染模板部署”。不要把两者混为一谈。
