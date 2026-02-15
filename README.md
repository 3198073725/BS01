# VidSprout | 开源视频平台（Monorepo）

VidSprout 是一个现代化、可二次开发的视频平台项目。本仓库为 **主仓库（Monorepo）**，用于统一管理后端、Web 前台、管理后台、移动端以及部署脚本。

> 说明：历史仓库名/域名中可能仍存在 `BS01`（例如 `.gitmodules`、本地域名 `bs01.local` 等），但产品对外品牌已统一为 **VidSprout**。

---

## 目录结构

- `backend/`
  - Django + DRF 后端 API（认证、视频、互动、通知、转码任务等）
- `web-client/`
  - Web 前台（桌面端网页）
- `admin-console/`
  - 管理后台（运营/审核/内容治理）
- `mobile_uniapp/`
  - UniApp 移动端（H5/App/小程序）
- `deploy/`
  - 生产部署参考（systemd、环境变量模板、Nginx 要点等）
- `deploy/systemd/`
  - systemd 服务单元文件（后端 Gunicorn / Celery / Web / Admin）
- `deploy/scripts/`
  - 部署/运维脚本（安装系统依赖、安装 systemd、备份与恢复等）
- `bento4/`
  - Bento4 相关工具/依赖（用于 DASH 打包等能力）
- `bs01ctl.py`
  - 项目运维脚本（统一管理后端/前端/systemd/Celery/日志/依赖/迁移等）
- `requirements.txt`
  - 主仓库 Python 依赖（主要用于后端部署/运行）

---

## 架构概览（推荐部署拓扑）

```text
            ┌──────────────────────────┐
            │         Web Client        │
            │   web-client (Vue)        │
            └────────────┬─────────────┘
                         │ HTTPS
                         │
┌────────────────────────▼────────────────────────┐
│                    Backend                       │
│   backend (Django + DRF + SimpleJWT)             │
│   - API / Auth / Interactions / Notifications    │
│   - Media: upload / transcode / pack             │
└───────────────┬──────────────────────┬──────────┘
                │                      │
                │ Celery tasks          │ Cache/Broker
                │                      │
      ┌─────────▼─────────┐   ┌───────▼────────┐
      │   Celery Worker    │   │     Redis      │
      │  (transcode/pack)  │   │ broker/cache   │
      └─────────┬─────────┘   └────────────────┘
                │
                │ DB
                │
        ┌───────▼────────┐
        │   PostgreSQL/   │
        │     MySQL       │
        └────────────────┘

同时：
- admin-console 通过后端管理 API 管理内容与用户
- mobile_uniapp 通过后端 API 完成移动端功能
```

---

## 子仓库说明（入口文档）

- **后端**：`backend/README.md`
- **管理端**：`admin-console/README.md`
- **Web 前台**：`web-client/README.md`
- **移动端**：`mobile_uniapp/README.md`

建议先阅读各子仓库 README，再按本文档的「本地开发快速开始」统一启动。

---

## 依赖与环境准备

### 必备软件

- **Python**：3.9+
- **Node.js**：
  - admin-console：Node.js 16+（文档按 16+ 编写）
  - web-client：建议 18+
- **数据库**：PostgreSQL（推荐）或 MySQL
- **Redis**：用于缓存、Celery broker、结果存储

### 多媒体工具链（强烈建议）

- **FFmpeg**：用于转码、封面抽帧等
- **Bento4**：用于 DASH 打包

> `deploy/README.md` 中也对生产部署依赖做了简要清单。

---

## 代码获取与子模块初始化

本仓库包含多个 Git submodule（见 `.gitmodules`）。首次拉取后需要初始化子模块：

```bash
git submodule update --init --recursive
```

如果你是首次部署或遇到子仓库为空、缺文件，优先检查 submodule 是否初始化。

---

## 本地开发快速开始（全栈）

下面给出一套“能跑起来”的开发顺序：**后端 → Worker → Web/管理端 → 移动端**。

### 1) 后端（backend）

1. 创建虚拟环境并安装依赖（主仓库根目录的 `requirements.txt` 已包含后端相关依赖）：

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -U pip
pip install -r requirements.txt
```

2. 配置环境变量：

- 生产/部署变量模板：`deploy/env.example`
- 按 `deploy/README.md` 的说明，将其复制为：`backend/.env`

```bash
cp deploy/env.example backend/.env
```

然后编辑 `backend/.env`，至少需要配置：

- `SECRET_KEY`
- `DEBUG`（本地可 true，生产必须 false）
- `DB_*` 或 `DATABASE_URL`
- `REDIS_URL`
- `CORS_ALLOWED_ORIGINS`、`CSRF_TRUSTED_ORIGINS`（联调前端必须）

3. 数据库迁移与管理账户：

```bash
cd backend
python manage.py migrate
python manage.py createsuperuser
```

4. 启动后端：

```bash
python manage.py runserver
```

> 如果你使用生产方式（Gunicorn + systemd），参考 `deploy/systemd/*` 与 `deploy/README.md`。

### 2) Celery Worker（可选但推荐）

视频转码/切片/打包通常通过 Celery 异步执行。

```bash
cd backend
celery -A core worker -l info
```

（如有 beat 定时任务，也可启动：`celery -A core beat -l info`，生产部署参考 `deploy/systemd`。）

### 3) Web 前台（web-client）

```bash
cd web-client
npm install
npm run dev
```

> 具体脚本以 `web-client/package.json` 为准。

### 4) 管理后台（admin-console）

```bash
cd admin-console
npm install
npm run serve
```

管理端页面标题已设置为「VidSprout 管理后台」（`admin-console/vue.config.js`）。

### 5) 移动端（mobile_uniapp）

移动端推荐使用 **HBuilderX** 打开 `mobile_uniapp/` 运行到 H5/App/小程序。

如果项目内包含 npm 依赖：

```bash
cd mobile_uniapp
npm install
```

> 移动端 H5 有全局滚动锁定策略；法律条款页等需要滚动的页面已实现动态解锁逻辑（见 `mobile_uniapp/src/App.vue` 与 `pages/legal/*`）。

---

## 默认端口与本地域名（建议约定）

项目中存在一些针对本地域名的便捷逻辑（例如 `admin-console/src/views/Videos.vue` 会将 `admin.` / `api.` 子域切换为 `web.` 并在本地映射到 8080）。

你可以使用类似以下约定（可按需调整）：

- Web 前台：`http://web.bs01.local:8080`
- 管理后台：`http://admin.bs01.local:8082`
- 移动端 H5：`http://mobile.bs01.local:5173`
- 后端 API：`http://api.bs01.local:8000`（或本地 `http://localhost:8000`）

> 重要：`web.bs01.local` 这种访问方式 **不是“开箱即用”**。
> 
> - 本地联调：需要在你的 **本机 DNS 或 `/etc/hosts`** 中配置解析，否则浏览器无法访问。
> - 生产环境：需要在你的 **域名 DNS** 中为 `web/admin/api/mobile` 等子域配置解析，并在 Nginx/网关中完成反代与 HTTPS。

### 本地 hosts 示例（推荐）

将以下内容追加到 `/etc/hosts`（Linux/macOS）或 `C:\\Windows\\System32\\drivers\\etc\\hosts`（Windows）：

```text
127.0.0.1  api.bs01.local
127.0.0.1  web.bs01.local
127.0.0.1  admin.bs01.local
127.0.0.1  mobile.bs01.local
```

如果你的服务跑在其他机器（例如局域网服务器 `192.168.1.10`），则把 `127.0.0.1` 改为对应服务器 IP。

---

## 部署（生产环境）

生产部署建议阅读：

- `deploy/README.md`

其中包含：
- 生产关键环境变量清单
- systemd + Gunicorn + Celery 的最小部署步骤
- Nginx 配置要点（API 反代、media 静态资源、HLS Range 请求、gzip 等）

---

## 运维脚本（强烈建议先看）

本仓库内置了可直接用于服务器运维的脚本，适合“单机部署 + systemd 托管”的场景。

### 1) `bs01ctl.py`（统一运维入口）

`bs01ctl.py` 是一个 Python 运维脚本，封装了常见操作：

- 服务：`status/start/stop/restart/logs`
- 部署：`install`（安装后端+前端依赖）、`setup-services`（安装/更新 systemd 单元）
- Django：`migrate/check/collectstatic/test`
- 体检：`doctor`（端口、服务、依赖、ffmpeg 等检查）
- 移动端：`uniapp-build-h5`、`uniapp-dev-start/stop/status`

常用示例：

```bash
python3 bs01ctl.py status
python3 bs01ctl.py restart all
python3 bs01ctl.py logs backend -f -n 200
python3 bs01ctl.py install
python3 bs01ctl.py migrate
python3 bs01ctl.py setup-services --enable
python3 bs01ctl.py doctor
```

> 注意：脚本内假定项目路径类似 `/root/BS01`，并且部分操作需要 root 权限（非 root 时会尝试使用 sudo）。

### 2) systemd 单元文件：`deploy/systemd/`

包含以下单元（以实际目录为准）：

- `bs01-gunicorn.service`
- `bs01-celery.service`
- `bs01-celery-transcode.service`
- `bs01-celery-beat.service`
- `bs01-web.service`
- `bs01-admin.service`

你可以使用：

```bash
python3 bs01ctl.py setup-services --enable
```

将单元安装到 `/etc/systemd/system/` 并启用启动。

### 3) 部署脚本：`deploy/scripts/`

该目录提供部署/运维辅助脚本（以实际文件为准）：

- `install_os_deps.sh`：安装系统依赖（Python 构建依赖、Nginx、Redis 等）
- `install_systemd.sh`：安装/更新 systemd 单元
- `backup.sh`：备份（通常包含数据库/关键目录）
- `restore.sh`：恢复备份

---

## 常见问题（FAQ）

### 1) 子仓库目录为空 / 找不到文件？
- 先执行：`git submodule update --init --recursive`

### 2) 前端跨域 / 登录态异常？
- 检查 `backend/.env`：
  - `CORS_ALLOWED_ORIGINS`
  - `CSRF_TRUSTED_ORIGINS`
  - `ALLOWED_HOSTS`
- 确保前端访问的 API base 与后端 `SITE_URL` 等配置一致

### 3) 视频转码不工作 / 没有生成播放文件？
- 确认已启动 Celery worker
- 检查 Redis 是否可用
- 检查服务器是否安装 FFmpeg/Bento4 且已加入 PATH

### 4) H5 页面无法滚动？
- 移动端 H5 为了防止页面抖动，可能会锁定 `html, body`。
- 需要滚动的页面请采用项目内已实现的“动态解锁”方案（法律条款页面已处理）。

---

## 贡献指南

欢迎提交 Issue / PR：
- 功能改动尽量拆分为小 PR
- 保持各子仓库 README 同步更新
- 涉及接口变更时，请同步更新前端调用与说明文档

---

## License

本仓库包含多个子模块，**不同模块可能存在不同许可**。请以各子仓库根目录的 `LICENSE` / `README` 声明为准。
