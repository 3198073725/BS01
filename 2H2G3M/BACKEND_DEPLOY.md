# BS01 后端（仅后端）服务器部署文档（Ubuntu 22.04 / 单机）

本文档仅覆盖 **后端 API** 在服务器上的部署。

- **后端代码目录**：`/root/BS01/backend`
- **Gunicorn 监听**：`127.0.0.1:8000`
- **对外访问**：推荐通过 Nginx 反代到 `:8000`（以及可选静态直出 `/media/`）
- **异步任务**：Celery（建议也部署，否则转码/生成缩略图会不工作）

> 说明：本仓库是主仓库 + submodule 结构。后端是 submodule：`/root/BS01/backend`。

---

## 0. 你需要准备的信息

- **服务器公网 IP**（假设为 `<SERVER_IP>`）
- 你希望 API 使用的域名：
  - 推荐：`api.bs01.local`（用于 hosts 虚拟域名）或你的真实域名（如 `api.example.com`）
- PostgreSQL 连接信息：
  - 数据库名（如 `bs01`）
  - 用户名（如 `bs01`）
  - 密码
- Redis 连接信息（单机可用 `redis://127.0.0.1:6379/0`）

---

## 1. 安装系统依赖（服务器执行）

```bash
sudo apt update
sudo apt install -y \
  python3-venv python3-dev build-essential \
  postgresql postgresql-contrib \
  redis-server \
  nginx \
  ffmpeg \
  ca-certificates curl git
```

### 1.1 安装 Python 3.12（Django 6 需要）

```bash
sudo apt install -y software-properties-common
sudo add-apt-repository -y ppa:deadsnakes/ppa
sudo apt update
sudo apt install -y python3.12 python3.12-venv python3.12-dev
python3.12 --version
```

---

## 2. 拉取代码（含 submodule）

首次部署：

```bash
sudo git clone --recurse-submodules git@github.com:3198073725/BS01.git /root/BS01
```

如果你已经 clone 过：

```bash
cd /root/BS01
sudo git pull
sudo git submodule update --init --recursive
```

---

## 3. 配置数据库（PostgreSQL）

### 3.1 创建数据库与用户（服务器执行）

```bash
sudo -u postgres psql
```

在 `psql` 中执行（示例，可按需改名/改密码）：

```sql
CREATE DATABASE bs01;
CREATE USER bs01 WITH PASSWORD 'REPLACE_WITH_STRONG_PASSWORD';
ALTER ROLE bs01 SET client_encoding TO 'utf8';
ALTER ROLE bs01 SET default_transaction_isolation TO 'read committed';
ALTER ROLE bs01 SET timezone TO 'Asia/Shanghai';
GRANT ALL PRIVILEGES ON DATABASE bs01 TO bs01;
\q
```

---

## 4. 创建 Python 虚拟环境并安装依赖

后端目录：`/root/BS01/backend`

```bash
cd /root/BS01/backend
python3.12 -m venv .venv
source .venv/bin/activate
pip install -U pip
pip install -r requirements.txt
```

---

## 5. 配置后端环境变量（强烈推荐使用 `.env`）

后端会读取：`/root/BS01/backend/.env`

你可以以示例文件为模板：

- 示例文件：`/root/BS01/2H2G3M/env/backend.env.production.example`

### 5.1 创建 `.env`

```bash
cd /root/BS01/backend
cp /root/BS01/2H2G3M/env/backend.env.production.example .env
```

### 5.2 必改项（最少）

编辑 `/root/BS01/backend/.env`，重点检查并填写：

- `SECRET_KEY=...`
- `DEBUG=false`
- `ALLOWED_HOSTS=...`
- `DATABASE_URL=postgres://...` 或对应的 DB 配置（以你文件里的字段为准）
- `REDIS_URL=redis://127.0.0.1:6379/0`

### 5.3 重要：媒体目录（解决 /media 404 的关键）

如果你希望媒体文件放在 **服务器的 `/root/BS01/media`**（而不是默认的 `/root/BS01/backend/media`），请在 `.env` 里设置：

```bash
MEDIA_ROOT=/root/BS01/media
MEDIA_URL=/media/
```

并确保目录存在：

```bash
sudo mkdir -p /root/BS01/media/videos
sudo mkdir -p /root/BS01/media/videos/thumbs
sudo mkdir -p /root/BS01/media/videos/low
sudo mkdir -p /root/BS01/media/videos/hls
sudo chown -R root:root /root/BS01/media
sudo chmod -R 755 /root/BS01/media
```

> 注意：如果你用非 root 用户运行 gunicorn/celery，需要把目录权限改成对应用户。

---

## 6. 初始化数据库（迁移 + 超级管理员）

```bash
cd /root/BS01/backend
source .venv/bin/activate
python manage.py migrate
python manage.py createsuperuser
```

---

## 7. 本地验证（不启 systemd，先跑通一次）

```bash
cd /root/BS01/backend
source .venv/bin/activate
python manage.py runserver 0.0.0.0:8000
```

在你电脑上访问：

- `http://<SERVER_IP>:8000/api/health/`

能返回 `{"status":"ok"}` 说明 API 起得来。

按 `Ctrl+C` 退出。

---

## 8. 使用 systemd 启动 Gunicorn（生产推荐）

仓库已提供 systemd unit 模板：

- `2H2G3M/systemd/bs01-gunicorn.service`
- `2H2G3M/systemd/bs01-celery.service`
- `2H2G3M/systemd/bs01-celery-transcode.service`

### 8.1 安装 unit 文件

```bash
cd /root/BS01
sudo bash 2H2G3M/scripts/install_systemd_units.sh
```

### 8.2 启动并设置开机自启

```bash
sudo systemctl daemon-reload
sudo systemctl enable bs01-gunicorn
sudo systemctl start bs01-gunicorn
sudo systemctl status bs01-gunicorn --no-pager
```

### 8.3 查看日志

```bash
sudo journalctl -u bs01-gunicorn -n 200 --no-pager
sudo journalctl -u bs01-gunicorn -f
```

---

## 9.（强烈推荐）部署 Celery（否则转码/缩略图等不会异步执行）

```bash
sudo systemctl enable bs01-celery
sudo systemctl start bs01-celery
sudo systemctl status bs01-celery --no-pager

# 如果你把转码任务单独拆成一个 worker（视你 unit 配置而定）
sudo systemctl enable bs01-celery-transcode
sudo systemctl start bs01-celery-transcode
sudo systemctl status bs01-celery-transcode --no-pager
```

查看日志：

```bash
sudo journalctl -u bs01-celery -n 200 --no-pager
sudo journalctl -u bs01-celery-transcode -n 200 --no-pager
```

---

## 10. Nginx（可选，但推荐）

如果你希望从 80 端口访问 API：

- 让 Nginx 监听 `:80`
- 反向代理 `/api/` 到 `http://127.0.0.1:8000`
- （可选）直接从磁盘直出 `/media/`（性能更好）

本仓库提供示例：`2H2G3M/nginx/bs01.conf`。

安装示例配置：

```bash
cd /root/BS01
sudo bash 2H2G3M/scripts/install_nginx_conf.sh
sudo nginx -t
sudo systemctl reload nginx
```

---

## 11. 常见问题排查

### 11.1 `/media/...` 访问 404

优先检查：

- Django `MEDIA_ROOT` 是否指向你真实存储目录
  - 如果你期望是 `/root/BS01/media`，则 `.env` 必须有 `MEDIA_ROOT=/root/BS01/media`
- 该文件是否真实存在：

```bash
ls -la /root/BS01/media/videos/
```

- Gunicorn 是否读取了正确的环境变量（systemd 环境）
  - `systemctl cat bs01-gunicorn` 查看 unit 的 `Environment=` 或 `EnvironmentFile=`
  - 修改 unit 后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl restart bs01-gunicorn
```

### 11.2 上传成功但 Celery 不转码

检查：

- Redis 是否运行：`systemctl status redis-server`
- worker 是否运行：`systemctl status bs01-celery`
- Celery 日志里是否能看到 `tasks.transcode_video_to_hls received`

### 11.3 Gunicorn 启动失败

看日志：

```bash
sudo journalctl -u bs01-gunicorn -n 200 --no-pager
```

常见原因：

- `.env` 缺少 `SECRET_KEY`
- 数据库连不上（`DATABASE_URL` 错、pg 未启动、密码错）
- 依赖未安装（requirements）

---

## 12. 升级流程（以后更新代码）

```bash
cd /root/BS01
sudo git pull
sudo git submodule update --init --recursive

cd /root/BS01/backend
source .venv/bin/activate
pip install -r requirements.txt
python manage.py migrate

sudo systemctl restart bs01-gunicorn
sudo systemctl restart bs01-celery
sudo systemctl restart bs01-celery-transcode
```
