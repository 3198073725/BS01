# 2H2G3M Profile

`2H2G3M/` 目录保存的是一套偏“当前单机生产环境”的资料，而不是通用部署模板。

## 这个目录是干什么的

它主要服务于这种场景：

- Ubuntu 单机
- 项目根目录固定为 `/root/BS01`
- 资源较紧张，按 2 核 2G 内存一类机器做参数收敛
- 需要一套能直接落地的 systemd / nginx / 前端发布脚本

## 包含内容

- `systemd/`
  固定路径的生产 unit 文件。
- `scripts/install_systemd_units.sh`
  安装这套 unit。
- `scripts/build_frontends.sh`
  构建 Web 和 Admin 前端。
- `scripts/deploy_frontend_static.sh`
  把前端构建结果发布到 `/var/www/bs01/`。
- `scripts/install_nginx_conf.sh`
  安装 nginx 站点配置。
- `env/backend.env.production.example`
  更贴近当前机器的后端环境变量示例。
- `nginx/bs01.conf`
  可选的 nginx 配置。

## 现在怎么理解这套资料

之前这里的文档默认写成“后端只监听 `127.0.0.1:8000`，必须走 nginx”。现在不再这么理解。

当前建议分开看：

- 后端直连 `0.0.0.0:8000`、不依赖 nginx：
  看 [BACKEND_DEPLOY.md](/root/BS01/2H2G3M/BACKEND_DEPLOY.md)
- 需要 nginx 统一 80/443、静态资源或 TLS：
  用本目录的 `nginx/` 和 `scripts/install_nginx_conf.sh`

## 与 `deploy/` 的区别

- `deploy/`
  通用模板，适合新机器、可渲染、默认更偏反向代理部署。
- `2H2G3M/`
  当前单机的固定路径资料，适合直接照当前机器落地。

如果你只想维护现在这台机器，优先看这里。
如果你要迁移到别的机器重新部署，优先看 [../deploy/README.md](/root/BS01/deploy/README.md)。
