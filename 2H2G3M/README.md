# BS01（VidSprout）2核2G/3M 单机生产部署（Ubuntu 22.04，HTTP，hosts 虚拟域名）

本文档按“从一台全新 Ubuntu 22.04 服务器开始”的方式编写，**一个步骤一个步骤、一个命令一个命令**执行。

部署目标（生产形态）：

- 后端：Gunicorn（systemd）监听 `127.0.0.1:8000`
- 异步：Celery（systemd），2C2G 调优（默认并发 `-c 1`）
- 前端：Web/管理端 **构建为静态文件**，由 Nginx 托管
- 媒体：本机磁盘 `/root/BS01/backend/media/`，由 Nginx 直出 `/media/`
- 域名：无真实域名，使用 hosts 虚拟域名
  - `web.bs01.local`
  - `admin.bs01.local`
  - `api.bs01.local`
- HTTPS：不强制（HTTP 80）

---

## 0) 约定与准备

- 项目根目录固定为：`/root/BS01`
- 你需要一台 Ubuntu 22.04 服务器，并能使用 `root` 或 `sudo` 权限

0.1 确认系统版本（在服务器上执行）：

```bash
cat /etc/os-release
uname -a
```

预期：显示 Ubuntu 22.04（Jammy）。

0.2 确认你具备 sudo 权限（在服务器上执行）：

```bash
whoami
sudo -n true 2>/dev/null && echo "sudo ok" || echo "sudo needs password"
```

如果显示 `sudo needs password`，后续所有 `sudo` 命令会要求你输入一次密码，这是正常的。

---

## 1) 在你的电脑上配置 hosts（否则虚拟域名无法访问）

1.1 获取服务器公网 IP（在服务器上执行）：

```bash
curl -4 ifconfig.me || true
```

1.2 在你自己的电脑（浏览器所在机器）编辑 hosts 文件，添加三行：

```text
<服务器公网IP>  web.bs01.local
<服务器公网IP>  admin.bs01.local
<服务器公网IP>  api.bs01.local
```

1.3 验证解析是否生效（在你自己的电脑上执行）：

```bash
ping -c 1 web.bs01.local
ping -c 1 admin.bs01.local
ping -c 1 api.bs01.local
```

预期：三条都能解析到你的服务器公网 IP。

---

## 2) 服务器基础环境初始化（Ubuntu 22.04）

2.1 更新系统索引：

```bash
sudo apt update
```

2.1.1（可选）升级系统已安装的软件包：

```bash
sudo apt -y upgrade
```

2.2 安装基础依赖（后端/数据库/缓存/Nginx/转码）：

```bash
sudo apt install -y \
  python3-venv python3-dev build-essential \
  nginx \
  redis-server \
  postgresql postgresql-contrib \
  ffmpeg \
  ca-certificates curl git
```

2.2.1 验证关键命令存在：

```bash
python3 --version
git --version
nginx -v
psql --version
redis-server --version
ffmpeg -version | head -n 1
```

2.3 验证服务是否存在：

```bash
systemctl status nginx --no-pager
systemctl status redis-server --no-pager
systemctl status postgresql --no-pager
```

预期：至少显示 `Active: active (running)`（如果不是，先修复系统服务再继续）。

2.4（推荐）开启 UFW 防火墙并放行必要端口（在服务器执行）：

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw --force enable
sudo ufw status verbose
```

预期：
- 22/tcp（OpenSSH）允许
- 80/tcp 允许

2.5（强烈推荐，2G 内存场景）创建 Swap（可选但建议）：

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
free -h
```

如果你希望重启后仍生效：

```bash
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## 3) 安装 Node.js 18（用于构建前端静态文件）

> 你只需要在服务器上构建一次前端即可；生产环境不跑 dev server。

3.1 添加 NodeSource 仓库（Node 18）：

```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
```

3.2 安装 Node.js：

```bash
sudo apt install -y nodejs
```

3.3 验证版本：

```bash
node -v
npm -v
```

预期：Node 版本为 `v18.*`。

---

## 4) 拉取项目代码（含 submodule）

4.1 拉取：

```bash
sudo git clone --recurse-submodules git@github.com:3198073725/BS01.git /root/BS01
```

如果你已经 clone 过，只需要更新：

```bash
sudo git -C /root/BS01 pull
sudo git -C /root/BS01 submodule update --init --recursive
```

4.2 验证目录：

```bash
ls -la /root/BS01
```

预期：存在 `backend/`、`web-client/`、`admin-console/`、`deploy/`、`2H2G3M/` 等。

---

## 5) 初始化 PostgreSQL（创建用户与数据库）

5.1 进入 psql：

```bash
sudo -u postgres psql
```

5.2 在 psql 里执行（把密码改成强密码）：

```sql
CREATE USER bs01 WITH PASSWORD 'YourStrongPasswordHere';
CREATE DATABASE bs01 OWNER bs01;
GRANT ALL PRIVILEGES ON DATABASE bs01 TO bs01;
\q
```

5.3 验证能否连接（把密码替换成你的真实密码）：

```bash
PGPASSWORD='YourStrongPasswordHere' psql -h 127.0.0.1 -U bs01 -d bs01 -c 'SELECT 1;'
```

预期：输出一行 `?column?` 并显示 `1`。

---

## 6) 配置后端环境变量文件 `.env`

6.1 复制生产模板（本目录提供了更贴近本次部署的模板）：

```bash
sudo cp /root/BS01/2H2G3M/env/backend.env.production.example /root/BS01/backend/.env
```

6.2 编辑 `/root/BS01/backend/.env`，至少修改这些项：

- `SECRET_KEY=...`（必须）
- `DB_PASSWORD=...`（必须）
- `ALLOWED_HOSTS=api.bs01.local,127.0.0.1,localhost,117.72.192.70`（服务器公网 IP 已替换）

6.3 立刻检查（确保 SECRET_KEY 没漏）：

```bash
grep -E '^(SECRET_KEY|DEBUG|ALLOWED_HOSTS|DB_NAME|DB_USER|DB_PASSWORD|DB_HOST|REDIS_URL)=' /root/BS01/backend/.env
```

预期：能看到这些 key；其中 `DEBUG=false`。

6.4（建议）检查数据库配置是否正确（DB_HOST/DB_PORT）：

```bash
grep -E '^DB_(ENGINE|NAME|USER|PASSWORD|HOST|PORT)=' /root/BS01/backend/.env
```

---

## 7) 安装 Python 依赖 + 数据库迁移 + 收集静态

7.1 创建 venv：

```bash
python3 -m venv /root/BS01/.venv
```

7.2 升级 pip：

```bash
/root/BS01/.venv/bin/pip install -U pip
```

7.3 安装依赖：

```bash
/root/BS01/.venv/bin/pip install -r /root/BS01/requirements.txt
```

7.4 迁移数据库：

```bash
cd /root/BS01/backend
/root/BS01/.venv/bin/python manage.py migrate
```

如果迁移报错，优先检查：

```bash
PGPASSWORD='YourStrongPasswordHere' psql -h 127.0.0.1 -U bs01 -d bs01 -c 'SELECT 1;'
```

7.5 收集静态文件（供 Django admin/DRF schema 等使用）：

```bash
/root/BS01/.venv/bin/python manage.py collectstatic --noinput
```

---

## 8) 构建并发布 Web/管理端静态文件（生产必须）

8.1 构建（会安装 npm 依赖并 build）：

```bash
bash /root/BS01/2H2G3M/scripts/build_frontends.sh
```

如果脚本提示权限问题，先赋予执行权限：

```bash
sudo chmod +x /root/BS01/2H2G3M/scripts/*.sh
```

8.2 验证 build 产物存在：

```bash
ls -la /root/BS01/web-client/dist | head
ls -la /root/BS01/admin-console/dist | head
```

8.3 发布到 Nginx 静态目录：

```bash
sudo bash /root/BS01/2H2G3M/scripts/deploy_frontend_static.sh
```

8.4 验证静态目录：

```bash
ls -la /var/www/bs01/web | head
ls -la /var/www/bs01/admin | head
```

---

## 9) 禁用开发期 dev server 服务（生产必须）

```bash
sudo bash /root/BS01/2H2G3M/scripts/disable_dev_services.sh
```

---

## 10) 安装 systemd 单元（2C2G 调优版）并启动后端/Celery

10.1 安装 unit 文件到 `/etc/systemd/system/`：

```bash
sudo bash /root/BS01/2H2G3M/scripts/install_systemd_units.sh
```

10.2 启动后端与 Celery：

```bash
sudo systemctl enable --now bs01-gunicorn bs01-celery
```

10.3 检查状态：

```bash
sudo systemctl status bs01-gunicorn --no-pager
sudo systemctl status bs01-celery --no-pager
```

10.4 验证后端端口只在本机监听（预期 127.0.0.1:8000）：

```bash
ss -ltnp | grep ':8000' || true
```

预期：输出中包含 `127.0.0.1:8000`，并且进程为 gunicorn。

10.5 直接从服务器本机验证 API：

```bash
curl -i http://127.0.0.1:8000/api/health/
```

预期：HTTP 200。

---

## 11) 安装 Nginx 配置（web/admin/api 虚拟域名）并重载

11.1 安装配置：

```bash
sudo bash /root/BS01/2H2G3M/scripts/install_nginx_conf.sh
```

11.2 重载 Nginx：

```bash
sudo systemctl reload nginx
```

11.2.1（推荐）禁用 Nginx 默认站点，避免抢占 server_name：

```bash
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

11.3 在服务器本机验证（无需 hosts）：

```bash
curl -I http://127.0.0.1/ 2>/dev/null || true
curl -I -H 'Host: web.bs01.local' http://127.0.0.1/ | head -n 5
curl -I -H 'Host: admin.bs01.local' http://127.0.0.1/ | head -n 5
curl -I -H 'Host: api.bs01.local' http://127.0.0.1/api/health/ | head -n 5
```

预期：
- web/admin 返回 `200` 或 `304`
- api 健康检查返回 `200`

---

## 12) 最终验收（在你自己的电脑上）

12.1 访问：

- `http://web.bs01.local/`
- `http://admin.bs01.local/`

12.2 验证 API：

```bash
curl -i http://api.bs01.local/api/health/
```

---

## 13) 日常运维（必须掌握）

13.1 查看状态：

```bash
sudo systemctl status bs01-gunicorn bs01-celery --no-pager
```

13.2 重启后端：

```bash
sudo systemctl restart bs01-gunicorn
```

13.3 看日志：

```bash
sudo journalctl -u bs01-gunicorn -n 200 --no-pager
sudo journalctl -u bs01-gunicorn -f
```

---

## 14) 2C2G 常见问题排查

14.1 访问很慢/频繁 502：
- 优先看 Gunicorn 日志：`journalctl -u bs01-gunicorn -n 200 --no-pager`
- 看系统内存：`free -h`

14.2 Celery 抢占资源：
- 2C2G 建议默认只启用 `bs01-celery`，并保持 `-c 1`
- 转码服务 `bs01-celery-transcode` 建议按需手动启用：

```bash
sudo systemctl enable --now bs01-celery-transcode
```

14.3 上传大文件失败：
- Nginx `client_max_body_size` 需大于你的上传限制（本配置为 600m）

---

## 15) 本目录文件清单

- `env/backend.env.production.example`：生产 `.env` 模板
- `nginx/bs01.conf`：Nginx 虚拟域名配置
- `systemd/*.service`：2C2G 调优版 systemd unit
- `scripts/*.sh`：构建/发布/安装脚本
