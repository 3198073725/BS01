#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BS01 项目运维脚本（Python 版）

用途：统一管理后端与前端的运行、日志、依赖、迁移、测试与体检。
- 生产服务：Django + Gunicorn + Celery（systemd）
- 开发辅助：Web/Admin/Mobile 前端 dev server（可选 systemd）

示例：
  python bs01ctl.py status                 # 查看全部服务状态
  python bs01ctl.py restart all            # 重启全部服务
  python bs01ctl.py logs backend -f -n 200 # 实时查看后端日志
  python bs01ctl.py install                # 安装后端与前端依赖
  python bs01ctl.py migrate                # 执行数据库迁移（PostgreSQL）
  python bs01ctl.py test apps --keepdb     # 运行测试（使用 pg 测试库）
  python bs01ctl.py doctor                 # 体检：默认检查生产服务
  python bs01ctl.py doctor --include-frontend-dev  # 附加检查前端开发服务

注意：本脚本默认以当前仓库根目录为项目根；涉及 systemd 的操作通常需要 root 权限，若非 root 将尝试使用 sudo。
"""

import argparse
import os
import sys
import subprocess
import shlex
import socket
import shutil
import http.client
import re
from urllib.parse import urlparse
import pwd
import grp
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
VENV_PY = BASE_DIR / '.venv' / 'bin' / 'python'
VENV_PIP = BASE_DIR / '.venv' / 'bin' / 'pip'
CELERY_BIN = BASE_DIR / '.venv' / 'bin' / 'celery'
MANAGE_PY = BASE_DIR / 'backend' / 'manage.py'
ENV_FILE = BASE_DIR / 'backend' / '.env'
SYSTEMD_DIR = Path('/etc/systemd/system')
SERVICE_DIR = BASE_DIR / 'deploy' / 'systemd'
DEV_SERVICE_DIR = BASE_DIR / 'deploy' / 'systemd-dev'

SERVICES = {
    'backend': 'bs01-gunicorn.service',
    'web': 'bs01-web.service',
    'admin': 'bs01-admin.service',
    'mobile': 'bs01-mobile.service',
    'celery': 'bs01-celery.service',
    'celery-transcode': 'bs01-celery-transcode.service',
    'celery-beat': 'bs01-celery-beat.service',
}
PRODUCTION_TARGETS = ['backend', 'celery', 'celery-transcode', 'celery-beat']
DEV_TARGETS = ['web', 'admin', 'mobile']
SERVICE_FILES = {
    'backend': SERVICE_DIR / 'bs01-gunicorn.service',
    'celery': SERVICE_DIR / 'bs01-celery.service',
    'celery-transcode': SERVICE_DIR / 'bs01-celery-transcode.service',
    'celery-beat': SERVICE_DIR / 'bs01-celery-beat.service',
    'web': DEV_SERVICE_DIR / 'bs01-web.service',
    'admin': DEV_SERVICE_DIR / 'bs01-admin.service',
    'mobile': DEV_SERVICE_DIR / 'bs01-mobile.service',
}
UNIAPP_ROOT = BASE_DIR / 'mobile_uniapp'
UNIAPP_DEV_PID = Path('/tmp/bs01_uniapp_dev.pid')
UNIAPP_DEV_LOG = Path('/tmp/bs01_uniapp_dev.log')
DEFAULT_TARGETS = PRODUCTION_TARGETS
DEPLOY_SCRIPTS_DIR = BASE_DIR / 'deploy' / 'scripts'
WEB_DIST_DIR = BASE_DIR / 'web-client' / 'dist'
ADMIN_DIST_DIR = BASE_DIR / 'admin-console' / 'dist'
WEB_STATIC_DST = Path('/var/www/bs01/web')
ADMIN_STATIC_DST = Path('/var/www/bs01/admin')
LEGACY_NGINX_CONF = BASE_DIR / '2H2G3M' / 'nginx' / 'bs01.conf'
NGINX_AVAIL = Path('/etc/nginx/sites-available/bs01.conf')
NGINX_ENABLED = Path('/etc/nginx/sites-enabled/bs01.conf')

# ------------------------- 工具函数 -------------------------

def is_root() -> bool:
    return os.geteuid() == 0 if hasattr(os, 'geteuid') else True


def run(cmd: str, cwd: Path | None = None, check: bool = True) -> int:
    """运行命令，输出直传控制台。"""
    print(f"$ {cmd}")
    proc = subprocess.run(cmd, shell=True, cwd=str(cwd) if cwd else None)
    if check and proc.returncode != 0:
        raise SystemExit(proc.returncode)
    return proc.returncode


def run_capture(cmd: str, cwd: Path | None = None) -> tuple[int, str]:
    """运行命令并捕获输出。"""
    print(f"$ {cmd}")
    proc = subprocess.run(
        cmd,
        shell=True,
        cwd=str(cwd) if cwd else None,
        text=True,
        capture_output=True,
    )
    output = ((proc.stdout or "") + (proc.stderr or "")).strip()
    return proc.returncode, output


def systemctl(args: str, check: bool = True) -> int:
    cmd = f"systemctl {args}"
    if not is_root():
        cmd = f"sudo {cmd}"
    return run(cmd, check=check)


def journalctl(unit: str, lines: int = 200, follow: bool = False) -> int:
    opt_f = "-f" if follow else ""
    cmd = f"journalctl -u {shlex.quote(unit)} -n {int(lines)} {opt_f} --no-pager"
    if not is_root():
        cmd = f"sudo {cmd}"
    return run(cmd)


def ensure_paths():
    if not VENV_PY.exists():
        print("[错误] 未找到虚拟环境：", VENV_PY)
        print("请先创建：python3 -m venv .venv && ./.venv/bin/pip install -r requirements.txt")
        raise SystemExit(1)
    if not MANAGE_PY.exists():
        print("[错误] 未找到 manage.py：", MANAGE_PY)
        raise SystemExit(1)
    if not ENV_FILE.exists():
        print("[警告] 未找到环境文件 .env：", ENV_FILE)


def ensure_script(path: Path) -> Path:
    if not path.exists():
        print("[错误] 未找到脚本：", path)
        raise SystemExit(1)
    return path


def parse_env_file(path: Path) -> dict[str, str]:
    env: dict[str, str] = {}
    if not path.exists():
        return env
    for raw in path.read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        env[key.strip()] = value.strip().strip('"').strip("'")
    return env


def default_service_user() -> str:
    try:
        return pwd.getpwuid(BASE_DIR.stat().st_uid).pw_name
    except Exception:
        return 'bs01'


def default_service_group() -> str:
    try:
        return grp.getgrgid(BASE_DIR.stat().st_gid).gr_name
    except Exception:
        return default_service_user()


def default_npm_bin() -> str:
    return shutil.which('npm') or '/usr/bin/npm'


def build_service_context(args=None, *, project_root: Path | None = None, service_user: str | None = None,
                          service_group: str | None = None, npm_bin: str | None = None) -> dict[str, str]:
    project_root = Path(project_root or getattr(args, 'project_root', BASE_DIR)).resolve()
    service_user = service_user or getattr(args, 'service_user', None) or default_service_user()
    service_group = service_group or getattr(args, 'service_group', None) or default_service_group()
    npm_bin = npm_bin or getattr(args, 'npm_bin', None) or default_npm_bin()
    return {
        'project_root': str(project_root),
        'service_user': service_user,
        'service_group': service_group,
        'npm_bin': npm_bin,
    }


def render_service_template(template_path: Path, context: dict[str, str], include_metadata: bool = False) -> str:
    template_path = template_path.resolve()
    text = template_path.read_text(encoding='utf-8')
    replacements = {
        '__PROJECT_ROOT__': context['project_root'],
        '__SERVICE_USER__': context['service_user'],
        '__SERVICE_GROUP__': context['service_group'],
        '__NPM_BIN__': context['npm_bin'],
    }
    for src, dst in replacements.items():
        text = text.replace(src, dst)
    if not include_metadata:
        return text

    try:
        rel = template_path.relative_to(BASE_DIR)
    except ValueError:
        rel = template_path.name
    header = [
        f"# Managed by bs01ctl.py from {rel}",
        f"# ProjectRoot={context['project_root']}",
        f"# ServiceUser={context['service_user']}",
        f"# ServiceGroup={context['service_group']}",
        f"# NpmBin={context['npm_bin']}",
        "",
    ]
    return "\n".join(header) + text


def read_installed_service_context(systemd_path: Path) -> dict[str, str] | None:
    try:
        text = systemd_path.read_text(encoding='utf-8')
    except Exception:
        return None

    meta = {}
    patterns = {
        'project_root': r'^# ProjectRoot=(.+)$',
        'service_user': r'^# ServiceUser=(.+)$',
        'service_group': r'^# ServiceGroup=(.+)$',
        'npm_bin': r'^# NpmBin=(.+)$',
    }
    for key, pattern in patterns.items():
        m = re.search(pattern, text, re.MULTILINE)
        if m:
            meta[key] = m.group(1).strip()
    if len(meta) != len(patterns):
        return None
    return meta


# ------------------------- 交互辅助 -------------------------

def _prompt(msg: str, default: str | None = None) -> str:
    tip = f" [{default}]" if default is not None else ""
    s = input(f"{msg}{tip}: ").strip()
    return s or (default or "")


def _yesno(msg: str, default: str = 'n') -> bool:
    s = _prompt(f"{msg} (y/n)", default).lower()
    return s in ('y', 'yes')


def _choice(msg: str, choices: list[str], default: str | None = None) -> str:
    while True:
        s = _prompt(f"{msg} 可选：{', '.join(choices)}", default)
        if s in choices:
            return s
        print("[提示] 输入无效，请重试。")


def list_units(targets: list[str]) -> list[str]:
    units = []
    for t in targets:
        if t == 'all':
            for k in DEFAULT_TARGETS:
                units.append(SERVICES[k])
        elif t in SERVICES:
            units.append(SERVICES[t])
        else:
            print(f"[警告] 未识别的目标：{t}，已跳过")
    # 去重并保持顺序
    seen = set()
    result = []
    for u in units:
        if u not in seen:
            seen.add(u)
            result.append(u)
    return result


# ------------------------- 子命令实现 -------------------------

def unit_installed(unit: str) -> bool:
    return (SYSTEMD_DIR / unit).exists()

def cmd_status(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        if unit_installed(u):
            systemctl(f"status {u} --no-pager", check=False)
        else:
            print(f"[跳过] 未安装单元：{u}（可执行 'python3 bs01ctl.py setup-services' 安装生产单元；前端开发单元需加 --include-frontend-dev）")


def cmd_start(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        if unit_installed(u):
            systemctl(f"enable --now {u}", check=False)
        else:
            print(f"[跳过] 未安装单元：{u}（可执行 'python3 bs01ctl.py setup-services' 安装生产单元；前端开发单元需加 --include-frontend-dev）")


def cmd_stop(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        if unit_installed(u):
            systemctl(f"stop {u}", check=False)
        else:
            print(f"[跳过] 未安装单元：{u}")


def cmd_restart(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        if unit_installed(u):
            systemctl(f"restart {u}", check=False)
        else:
            print(f"[跳过] 未安装单元：{u}")


def cmd_reload(args):
    systemctl("daemon-reload")


def cmd_enable(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        if unit_installed(u):
            systemctl(f"enable {u}", check=False)
        else:
            print(f"[跳过] 未安装单元：{u}")


def cmd_disable(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        if unit_installed(u):
            systemctl(f"disable {u}", check=False)
        else:
            print(f"[跳过] 未安装单元：{u}")


def _read_pid(pid_path: Path) -> int | None:
    try:
        s = pid_path.read_text(encoding='utf-8').strip()
        if not s:
            return None
        return int(s)
    except Exception:
        return None


def _pid_alive(pid: int) -> bool:
    try:
        os.kill(pid, 0)
        return True
    except Exception:
        return False


def cmd_uniapp_build_h5(args):
    """构建 uni-app H5 生产包。"""
    if not UNIAPP_ROOT.exists():
        print(f"[错误] 未找到 uni-app 源码目录：{UNIAPP_ROOT}")
        return
    print("[信息] 正在构建 uni-app H5...")
    run("npm run build:h5", cwd=UNIAPP_ROOT)
    print("[成功] uni-app H5 构建完成。产物在 mobile_uniapp/dist/build/h5")


def cmd_uniapp_clean(args):
    """清理 uni-app 构建缓存。"""
    dist = UNIAPP_ROOT / 'dist'
    if dist.exists():
        print(f"[信息] 正在清理 {dist}...")
        shutil.rmtree(dist)
        print("[成功] uni-app 缓存已清理。")
    else:
        print("[信息] 无需清理，dist 目录不存在。")


def cmd_uniapp_dev_start(args):
    """启动 uni-app H5 开发服务 (后台运行)。"""
    if not UNIAPP_ROOT.exists():
        print(f"[错误] 未找到 uni-app 源码目录：{UNIAPP_ROOT}")
        return
    
    print("[信息] 正在停止旧的 uni-app 开发服务...")
    cmd_uniapp_dev_stop(None)
    
    print("[信息] 正在启动 uni-app H5 开发服务...")
    # 使用 nohup 后台运行 npm run dev:h5
    # 注意：npm 可能会启动子进程，这里尝试记录 pid
    cmd = f"nohup npm run dev:h5 >{UNIAPP_DEV_LOG} 2>&1 & echo $! > {UNIAPP_DEV_PID}"
    run(cmd, cwd=UNIAPP_ROOT)
    
    print("[成功] 开发服务已在后台启动。")
    print(f"[提示] 日志文件：{UNIAPP_DEV_LOG}")
    print(f"[提示] 默认端口通常为 5173，请查看日志确认。")


def cmd_uniapp_dev_stop(args):
    """停止 uni-app 开发服务。"""
    pid = _read_pid(UNIAPP_DEV_PID)
    if not pid:
        print("[信息] 未发现 uni-app 开发服务 PID 文件。")
        return
    
    if _pid_alive(pid):
        print(f"[信息] 正在停止 uni-app 开发服务 (pid={pid})...")
        # 尝试杀掉进程组，因为 npm 会启动子进程
        run(f"pkill -P {pid} || kill {pid}", check=False)
        print("[成功] 已发送停止信号。")
    else:
        print(f"[信息] 服务进程 (pid={pid}) 已不存在。")
    
    try:
        UNIAPP_DEV_PID.unlink(missing_ok=True)
    except:
        pass


def cmd_uniapp_dev_status(args):
    """查看 uni-app 开发服务状态。"""
    pid = _read_pid(UNIAPP_DEV_PID)
    if not pid:
        print("[状态] uni-app 开发服务未运行 (无 PID 文件)。")
        return
    
    if _pid_alive(pid):
        print(f"[状态] uni-app 开发服务正在运行 (pid={pid})。")
        print(f"[信息] 最近 10 行日志：")
        run(f"tail -n 10 {UNIAPP_DEV_LOG}", check=False)
    else:
        print(f"[状态] PID 文件存在但进程已退出 (pid={pid})。")


def cmd_logs(args):
    unit = SERVICES.get(args.target)
    if not unit:
        print("[错误] 目标应为 backend/web/admin/mobile/celery/celery-transcode/celery-beat 之一")
        raise SystemExit(2)
    journalctl(unit, lines=args.lines, follow=args.follow)


def cmd_install(args):
    """安装依赖（后端 + 前端）。"""
    # 后端 Python 依赖
    if not VENV_PY.exists():
        # 自动创建虚拟环境并升级 pip
        run("python3 -m venv .venv", cwd=BASE_DIR)
        run(f"{VENV_PY} -m pip install -U pip", cwd=BASE_DIR)
    run(f"{VENV_PIP} install -r requirements.txt", cwd=BASE_DIR)
    # 前端依赖
    if not args.skip_frontend:
        if (BASE_DIR / 'web-client' / 'package.json').exists():
            run("npm i --no-audit --no-fund", cwd=BASE_DIR / 'web-client')
        if (BASE_DIR / 'admin-console' / 'package.json').exists():
            run("npm ci --no-audit --no-fund", cwd=BASE_DIR / 'admin-console')
        if UNIAPP_ROOT.exists():
            print("[信息] 正在安装 uni-app 依赖...")
            run("npm i --no-audit --no-fund", cwd=UNIAPP_ROOT)


def cmd_install_os_deps(args):
    script = ensure_script(DEPLOY_SCRIPTS_DIR / 'install_os_deps.sh')
    extra = " --db-from-env" if args.db_from_env else ""
    run(f"bash {shlex.quote(str(script))}{extra}", cwd=BASE_DIR)


def cmd_build_frontends(args):
    if (BASE_DIR / 'web-client' / 'package.json').exists():
        run("npm ci --no-audit --no-fund || npm i --no-audit --no-fund", cwd=BASE_DIR / 'web-client')
        run("npm run build", cwd=BASE_DIR / 'web-client')
    if (BASE_DIR / 'admin-console' / 'package.json').exists():
        run("npm ci --no-audit --no-fund || npm i --no-audit --no-fund", cwd=BASE_DIR / 'admin-console')
        run("npm run build", cwd=BASE_DIR / 'admin-console')


def cmd_deploy_frontend_static(args):
    if not WEB_DIST_DIR.exists():
        print(f"[错误] 未找到前台构建产物：{WEB_DIST_DIR}")
        raise SystemExit(1)
    if not ADMIN_DIST_DIR.exists():
        print(f"[错误] 未找到管理端构建产物：{ADMIN_DIST_DIR}")
        raise SystemExit(1)
    if not is_root():
        print("[错误] 静态发布需要 root 权限。")
        raise SystemExit(1)
    WEB_STATIC_DST.mkdir(parents=True, exist_ok=True)
    ADMIN_STATIC_DST.mkdir(parents=True, exist_ok=True)
    run(f"rm -rf {shlex.quote(str(WEB_STATIC_DST))}/*")
    run(f"rm -rf {shlex.quote(str(ADMIN_STATIC_DST))}/*")
    run(f"cp -r {shlex.quote(str(WEB_DIST_DIR))}/. {shlex.quote(str(WEB_STATIC_DST))}/")
    run(f"cp -r {shlex.quote(str(ADMIN_DIST_DIR))}/. {shlex.quote(str(ADMIN_STATIC_DST))}/")


def cmd_install_nginx_conf(args):
    src = LEGACY_NGINX_CONF
    if not src.exists():
        print(f"[错误] 未找到 Nginx 配置模板：{src}")
        raise SystemExit(1)
    if not is_root():
        print("[错误] 安装 Nginx 配置需要 root 权限。")
        raise SystemExit(1)
    run(f"cp {shlex.quote(str(src))} {shlex.quote(str(NGINX_AVAIL))}")
    run(f"ln -sf {shlex.quote(str(NGINX_AVAIL))} {shlex.quote(str(NGINX_ENABLED))}")
    run("rm -f /etc/nginx/sites-enabled/default", check=False)
    run("nginx -t")
    if args.reload:
        run("systemctl reload nginx")


def cmd_setup_services(args):
    """安装/更新 systemd 单元并重载。"""
    context = build_service_context(args)
    targets = list(PRODUCTION_TARGETS)
    if args.include_frontend_dev:
        targets.extend(DEV_TARGETS)
    files = [SERVICE_FILES[t] for t in targets]
    for f in files:
        if not f.exists():
            print("[警告] 未找到服务文件：", f)
            continue
        dst = SYSTEMD_DIR / f.name
        rendered = render_service_template(f, context, include_metadata=True)
        tmp = BASE_DIR / '.tmp' / f.name
        tmp.parent.mkdir(parents=True, exist_ok=True)
        tmp.write_text(rendered, encoding='utf-8')
        try:
            cmd = f"install -m 0644 {shlex.quote(str(tmp))} {shlex.quote(str(dst))}"
            if not is_root():
                cmd = f"sudo {cmd}"
            run(cmd)
        finally:
            try:
                tmp.unlink()
            except FileNotFoundError:
                pass
    systemctl("daemon-reload")
    if args.enable:
        cmd_start(argparse.Namespace(targets=['all']))
        if args.include_frontend_dev:
            cmd_start(argparse.Namespace(targets=DEV_TARGETS))


def cmd_backup(args):
    script = ensure_script(DEPLOY_SCRIPTS_DIR / 'backup.sh')
    parts = ["bash", shlex.quote(str(script))]
    if args.dir:
        parts.extend(["--dir", shlex.quote(args.dir)])
    if args.no_db:
        parts.append("--no-db")
    if args.no_media:
        parts.append("--no-media")
    if args.no_systemd:
        parts.append("--no-systemd")
    if args.no_code:
        parts.append("--no-code")
    if args.no_bundle:
        parts.append("--no-bundle")
    run(" ".join(parts), cwd=BASE_DIR)


def cmd_restore(args):
    script = ensure_script(DEPLOY_SCRIPTS_DIR / 'restore.sh')
    parts = ["bash", shlex.quote(str(script))]
    if args.src:
        parts.extend(["--src", shlex.quote(args.src)])
    if args.with_db:
        parts.append("--with-db")
    if args.wipe_media:
        parts.append("--wipe-media")
    if args.reuse_systemd_render:
        parts.append("--reuse-systemd-render")
    if args.include_frontend_dev:
        parts.append("--include-frontend-dev")
    if args.service_user:
        parts.extend(["--service-user", shlex.quote(args.service_user)])
    if args.service_group:
        parts.extend(["--service-group", shlex.quote(args.service_group)])
    if args.project_root:
        parts.extend(["--project-root", shlex.quote(args.project_root)])
    if args.npm_bin:
        parts.extend(["--npm-bin", shlex.quote(args.npm_bin)])
    run(" ".join(parts), cwd=BASE_DIR)


def cmd_migrate(args):
    ensure_paths()
    run(f"{VENV_PY} {MANAGE_PY} migrate", cwd=BASE_DIR)


def cmd_check(args):
    ensure_paths()
    run(f"{VENV_PY} {MANAGE_PY} check", cwd=BASE_DIR)


def cmd_collectstatic(args):
    ensure_paths()
    run(f"{VENV_PY} {MANAGE_PY} collectstatic --noinput", cwd=BASE_DIR)


def cmd_test(args):
    ensure_paths()
    label = args.label or ''
    keep = ' --keepdb' if args.keepdb else ''
    run(f"{VENV_PY} {MANAGE_PY} test {label} -v 2{keep}", cwd=BASE_DIR)


def cmd_doctor(args):
    """基本体检：默认检查生产服务，可选附加前端开发服务。"""
    include_frontend_dev = bool(getattr(args, 'include_frontend_dev', False))
    env_map = parse_env_file(ENV_FILE)
    status_targets = ['all']
    if include_frontend_dev:
        status_targets = ['all'] + DEV_TARGETS
    print("[信息] 检查服务状态...")
    cmd_status(argparse.Namespace(targets=status_targets))
    print("\n[信息] 检查关键文件...")
    for p in [VENV_PY, MANAGE_PY, ENV_FILE]:
        print("  ✅ 存在" if p.exists() else "  ❌ 不存在", "-", p)
    print("\n[信息] 检查端口连通性...")
    def _probe_tcp(host: str, port: int) -> tuple[str, str]:
        try:
            with socket.create_connection((host, port), timeout=1.5):
                return "ok", f"{host}:{port} 可连接"
        except PermissionError as e:
            return "skip", f"{host}:{port} 检查受限：{e}"
        except OSError as e:
            return "err", f"{host}:{port} 不可用：{e}"

    targets = [
        ("后端", "127.0.0.1", 8000),
    ]
    if include_frontend_dev:
        targets.extend([
            ("Web", "127.0.0.1", 8080),
            ("Admin", "127.0.0.1", 8082),
            ("移动端", "127.0.0.1", 5173),
        ])
    for name, host, port in targets:
        status, msg = _probe_tcp(host, port)
        icon = "✅" if status == "ok" else ("⚠️" if status == "skip" else "❌")
        print(f"  {icon} {name} {msg}")
    if not include_frontend_dev:
        print("  ℹ️ 前端开发端口检查默认跳过；如需检查 8080/8082/5173，请附加 --include-frontend-dev")

    # Show who occupies gunicorn bind port (from config)
    try:
        bind_port = 8000
        cfg = (BASE_DIR / 'backend' / 'gunicorn.conf.py').read_text(encoding='utf-8')
        for line in cfg.splitlines():
            s = line.strip()
            if s.startswith('bind') and ':' in s:
                # e.g. bind = "0.0.0.0:8000"
                colon = s.rfind(':')
                if colon > 0:
                    tail = s[colon+1:].strip().strip('"\'')
                    try:
                        bind_port = int(''.join(ch for ch in tail if ch.isdigit()))
                    except Exception:
                        bind_port = 8000
                break
        print(f"\n[信息] 端口占用检查（Gunicorn bind: :{bind_port}）...")
        run(f"ss -ltnp | grep -E ':{bind_port}\\b' || true", check=False)
    except Exception:
        pass
    print("\n[信息] 检查 ffmpeg/ffprobe 可用性...")
    for bin_name in ('ffmpeg', 'ffprobe'):
        path = shutil.which(bin_name)
        if not path:
            print(f"  ❌ 未找到 {bin_name}，建议安装：apt install -y ffmpeg")
        else:
            rc = run(f"{bin_name} -version > /dev/null 2>&1", check=False)
            print(f"  {'✅' if rc == 0 else '❌'} {bin_name} 可执行：{path}")
    print("\n[信息] 检查 Celery 可用性...")
    if CELERY_BIN.exists():
        # 显式指定工作目录与 PYTHONPATH，避免导入 backend 失败导致误报
        rc_ver, out_ver = run_capture(f"{CELERY_BIN} --version", cwd=BASE_DIR)
        if rc_ver == 0:
            version_line = out_ver.splitlines()[0] if out_ver else "unknown"
            print(f"  ✅ Celery CLI 可执行：{version_line}")
        else:
            brief = out_ver.splitlines()[-1] if out_ver else "未知错误"
            print(f"  ❌ Celery CLI 执行失败：{brief}")

        # 使用 -A backend（Celery 会在 backend/celery.py 中自动发现 app）
        py_path = shlex.quote(str(BASE_DIR / 'backend'))
        rc, out = run_capture(
            f"PYTHONPATH={py_path} DJANGO_SETTINGS_MODULE=backend.settings "
            f"{CELERY_BIN} -A backend.celery:app inspect ping -t 2",
            cwd=BASE_DIR,
        )
        if rc == 0:
            print("  ✅ Celery worker 可响应 ping")
        else:
            detail = out.splitlines()[-1] if out else "无输出"
            print(f"  ❌ Celery worker 未响应 ping：{detail}")
    else:
        print("  ❌ 未找到 Celery 可执行文件：", CELERY_BIN)

    print("\n[信息] CORS 预检自检 (OPTIONS /api/)...")
    def _cors_preflight(origin: str) -> None:
        try:
            conn = http.client.HTTPConnection('127.0.0.1', 8000, timeout=2)
            headers = {
                'Origin': origin,
                'Access-Control-Request-Method': 'GET',
                'Access-Control-Request-Headers': 'Authorization, Content-Type',
            }
            conn.request('OPTIONS', '/api/admin/users/', headers=headers)
            resp = conn.getresponse()
            status = resp.status
            hdrs = {k.lower(): v for k, v in resp.getheaders()}
        except Exception as e:
            print(f"  ❌ 预检请求失败（{origin}）：{e}")
            try:
                conn.close()
            except Exception:
                pass
            return
        finally:
            try:
                conn.close()
            except Exception:
                pass

        if status in (301, 302, 307, 308):
            loc = hdrs.get('location', '')
            print(f"  ❌ 发生重定向（HTTP {status} -> {loc}）。可能因 SECURE_SSL_REDIRECT=true 导致 http->https 跳转，影响预检。")
            return
        if status >= 400:
            print(f"  ❌ 非预期状态码（HTTP {status}）。")
            return

        acao = hdrs.get('access-control-allow-origin')
        acam = hdrs.get('access-control-allow-methods')
        acah = hdrs.get('access-control-allow-headers')
        if not acao:
            print("  ❌ 缺少 Access-Control-Allow-Origin 响应头。")
            return
        if acao != '*' and acao != origin:
            print(f"  ⚠️ Access-Control-Allow-Origin 值异常：{acao}（期望 {origin} 或 *）。")
        else:
            print(f"  ✅ 预检通过（Origin={origin}，ACAO={acao}，Methods={acam or '-'}，Headers={acah or '-'}）")

    def _read_unit_logs(unit: str, lines: int = 200) -> str:
        cmd = ["journalctl", "-u", unit, "-n", str(lines), "--no-pager"]
        if not is_root():
            cmd = ["sudo"] + cmd
        try:
            proc = subprocess.run(cmd, capture_output=True, text=True)
            return (proc.stdout or "") + (proc.stderr or "")
        except Exception as e:
            print(f"  ⚠️ 读取 {unit} 日志失败：{e}")
            return ""

    def _extract_http_origins(logs: str) -> list[str]:
        origins: list[str] = []
        for line in logs.splitlines():
            for match in re.findall(r"http://[A-Za-z0-9\.-]+:\d+", line):
                if match not in origins:
                    origins.append(match)
        return origins

    def _report_frontend_logs(label: str, unit_key: str, success_markers: list[str]) -> list[str]:
        print(f"\n[信息] {label} 前端编译/启动状态检测（读取 journalctl）...")
        unit = SERVICES.get(unit_key)
        if not unit or not unit_installed(unit):
            print(f"  ⚠️ 未安装或未识别 systemd 单元：{unit or unit_key}，跳过状态检测。")
            return []
        logs = _read_unit_logs(unit)
        low = logs.lower()
        if not logs.strip():
            print("  ⚠️ 无日志输出，可能服务未启动或日志被轮转。")
        elif "failed to compile" in low or "module not found" in low or re.search(r"error in \\S+", low):
            print("  ❌ 检测到编译错误（包含 'Failed to compile'/'Module not found' 等关键字）。")
        elif "compiled with warnings" in low:
            print("  ⚠️ 已编译但存在警告（Compiled with warnings）。")
        elif any(marker in low for marker in success_markers):
            print("  ✅ 已检测到成功启动/编译标记。")
        else:
            print("  ℹ️ 未检测到显著的启动或编译状态关键字。")
        return _extract_http_origins(logs)

    if include_frontend_dev:
        print("\n[信息] 前端开发 CORS 预检...")
        frontend_origins = [
            "http://127.0.0.1:8080",
            "http://localhost:8080",
            "http://127.0.0.1:8082",
            "http://localhost:8082",
            "http://127.0.0.1:5173",
            "http://localhost:5173",
        ]
        for og in frontend_origins:
            _cors_preflight(og)

        discovered_origins: list[str] = []
        discovered_origins.extend(_report_frontend_logs("Web", "web", ["compiled successfully"]))
        discovered_origins.extend(_report_frontend_logs("Admin", "admin", ["compiled successfully"]))
        discovered_origins.extend(_report_frontend_logs("移动端", "mobile", ["ready in", "local:", "network:"]))
        for og in discovered_origins:
            if og not in frontend_origins:
                print(f"  尝试对日志中的 Origin 进行预检：{og}")
                _cors_preflight(og)
    else:
        print("\n[信息] 已跳过前端开发自检（CORS 预检、Admin 编译状态）。如需检查，请附加 --include-frontend-dev。")

    print("\n[信息] 检查 PostgreSQL 配置与连通性...")
    db_engine = env_map.get('DB_ENGINE', 'django.db.backends.postgresql')
    db_host = env_map.get('DB_HOST', '127.0.0.1') or '127.0.0.1'
    db_port = env_map.get('DB_PORT', '5432') or '5432'
    db_name = env_map.get('DB_NAME', '')
    db_user = env_map.get('DB_USER', '')
    if 'postgresql' not in db_engine:
        print(f"  ℹ️ 当前 DB_ENGINE={db_engine}，跳过 PostgreSQL 专项检查。")
    else:
        if db_name and db_user:
            print(f"  ℹ️ 目标数据库：{db_user}@{db_host}:{db_port}/{db_name}")
        else:
            print("  ⚠️ backend/.env 缺少 DB_NAME 或 DB_USER。")
        status, msg = _probe_tcp(db_host, int(db_port))
        icon = "✅" if status == "ok" else ("⚠️" if status == "skip" else "❌")
        print(f"  {icon} PostgreSQL {msg}")

    print("\n[信息] 检查 Redis 配置与连通性...")
    redis_url = env_map.get('REDIS_URL') or env_map.get('CELERY_BROKER_URL') or env_map.get('CELERY_RESULT_BACKEND', '')
    if redis_url:
        parsed = urlparse(redis_url)
        redis_host = parsed.hostname or env_map.get('REDIS_HOST', '127.0.0.1')
        redis_port = parsed.port or int(env_map.get('REDIS_PORT', '6379') or '6379')
    else:
        redis_host = env_map.get('REDIS_HOST', '127.0.0.1') or '127.0.0.1'
        redis_port = int(env_map.get('REDIS_PORT', '6379') or '6379')
    status, msg = _probe_tcp(redis_host, int(redis_port))
    icon = "✅" if status == "ok" else ("⚠️" if status == "skip" else "❌")
    print(f"  {icon} Redis {msg}")
    redis_cli = shutil.which('redis-cli')
    if redis_cli:
        rc, out = run_capture(f"{redis_cli} -h {shlex.quote(str(redis_host))} -p {int(redis_port)} ping")
        if rc == 0:
            print(f"  ✅ redis-cli ping 成功：{(out.splitlines()[-1] if out else 'PONG')}")
        else:
            detail = out.splitlines()[-1] if out else "无输出"
            print(f"  ⚠️ redis-cli ping 失败：{detail}")
    else:
        print("  ℹ️ 未找到 redis-cli，跳过 PING 检查。")

    print("\n[信息] 检查 Nginx 状态与静态目录...")
    nginx_bin = shutil.which('nginx')
    if not nginx_bin:
        print("  ⚠️ 未找到 nginx 可执行文件。")
    else:
        rc, out = run_capture("nginx -t")
        if rc == 0:
            print("  ✅ nginx -t 通过")
        else:
            detail = out.splitlines()[-1] if out else "无输出"
            print(f"  ❌ nginx -t 失败：{detail}")
        rc_active, out_active = run_capture("systemctl is-active nginx")
        state = out_active.splitlines()[-1] if out_active else "unknown"
        icon = "✅" if rc_active == 0 and state == "active" else "⚠️"
        print(f"  {icon} nginx 服务状态：{state}")
    for label, path in (("前台静态目录", WEB_STATIC_DST), ("管理端静态目录", ADMIN_STATIC_DST)):
        if path.exists():
            try:
                count = sum(1 for _ in path.iterdir())
            except Exception:
                count = -1
            suffix = f"（{count} 项）" if count >= 0 else ""
            print(f"  ✅ {label}存在：{path}{suffix}")
        else:
            print(f"  ⚠️ {label}不存在：{path}")
    for label, path in (("Nginx enabled 配置", NGINX_ENABLED), ("Nginx available 配置", NGINX_AVAIL)):
        print(f"  {'✅' if path.exists() else '⚠️'} {label}：{path}")

    # Compare systemd units with deploy service templates
    print("\n[信息] 校验 systemd 单元与部署模板是否一致...")
    def _read(p: Path) -> str:
        try:
            return p.read_text(encoding='utf-8')
        except Exception:
            return ''
    compare_prefixes = ('User=', 'Group=', 'WorkingDirectory=', 'EnvironmentFile=', 'ExecStart=')
    def _relevant_lines(txt: str) -> list[str]:
        lines = []
        for line in txt.splitlines():
            s = line.strip()
            if any(s.startswith(prefix) for prefix in compare_prefixes):
                lines.append(s)
        return lines
    check_targets = list(PRODUCTION_TARGETS)
    if include_frontend_dev:
        check_targets.extend(DEV_TARGETS)
    for key in check_targets:
        unit = SERVICES[key]
        sys_path = SYSTEMD_DIR / unit
        dep_path = SERVICE_FILES[key]
        if not sys_path.exists():
            print(f"  ❌ 未安装到 systemd: {unit} (缺少 {sys_path})")
            continue
        if not dep_path.exists():
            print(f"  ❌ 部署目录缺少：{dep_path}")
            continue
        context = read_installed_service_context(sys_path) or build_service_context()
        sys_exec = _relevant_lines(_read(sys_path))
        dep_exec = _relevant_lines(render_service_template(dep_path, context))
        if sys_exec != dep_exec:
            print(f"  ⚠️ 单元不一致: {unit}")
            print(f"     systemd: {sys_exec or ['<无>']}")
            print(f"     deploy : {dep_exec or ['<无>']}")
        else:
            print(f"  ✅ 单元一致: {unit}")


def interactive_menu():
    while True:
        print("\n====== BS01 运维菜单 ======")
        print("1) 查看服务状态")
        print("2) 启动服务")
        print("3) 停止服务")
        print("4) 重启服务")
        print("5) 查看日志")
        print("6) 安装依赖")
        print("7) 安装/更新 systemd 服务单元")
        print("8) 数据库迁移")
        print("9) Django 健康检查")
        print("10) 收集静态文件")
        print("11) 运行测试")
        print("12) 体检")
        print("13) 构建 uni-app H5 生产包")
        print("14) 清理 uni-app 缓存")
        print("15) 启动 uni-app 开发服务 (H5)")
        print("16) 停止 uni-app 开发服务")
        print("17) 查看 uni-app 开发服务状态")
        print("18) 安装系统依赖")
        print("19) 执行备份")
        print("20) 执行恢复")
        print("21) 构建前端生产包")
        print("22) 发布前端静态文件")
        print("23) 安装 Nginx 配置")
        print("0) 退出")
        choice = _prompt("请选择编号", "1")

        try:
            num = int(choice)
        except ValueError:
            print("[提示] 请输入数字编号。")
            continue

        if num == 0:
            break
        elif num == 1:
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile', 'celery', 'celery-transcode', 'celery-beat'], 'all')
            cmd_status(argparse.Namespace(targets=[tgt]))
        elif num == 2:
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile', 'celery', 'celery-transcode', 'celery-beat'], 'all')
            cmd_start(argparse.Namespace(targets=[tgt]))
        elif num == 3:
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile', 'celery', 'celery-transcode', 'celery-beat'], 'all')
            cmd_stop(argparse.Namespace(targets=[tgt]))
        elif num == 4:
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile', 'celery', 'celery-transcode', 'celery-beat'], 'all')
            cmd_restart(argparse.Namespace(targets=[tgt]))
        elif num == 5:
            tgt = _choice("目标服务", ['backend', 'web', 'admin', 'mobile', 'celery', 'celery-transcode', 'celery-beat'], 'backend')
            lines = _prompt("显示日志行数", "200")
            try:
                n = int(lines)
            except ValueError:
                n = 200
            follow = _yesno("是否持续跟随?")
            cmd_logs(argparse.Namespace(target=tgt, lines=n, follow=follow))
        elif num == 6:
            skip_fe = not _yesno("安装前端依赖?", 'y')
            cmd_install(argparse.Namespace(skip_frontend=skip_fe))
        elif num == 7:
            include_dev = _yesno("是否同时安装前端开发用 systemd 单元?", 'n')
            en = _yesno("安装后是否启用并启动默认生产服务?", 'n')
            cmd_setup_services(argparse.Namespace(enable=en, include_frontend_dev=include_dev))
        elif num == 8:
            cmd_migrate(argparse.Namespace())
        elif num == 9:
            cmd_check(argparse.Namespace())
        elif num == 10:
            cmd_collectstatic(argparse.Namespace())
        elif num == 11:
            label = _prompt("测试标签(回车默认 apps)", "apps")
            keep = _yesno("是否保留测试数据库?", 'y')
            cmd_test(argparse.Namespace(label=label, keepdb=keep))
        elif num == 12:
            include_dev = _yesno("是否附加检查前端开发服务?", 'n')
            cmd_doctor(argparse.Namespace(include_frontend_dev=include_dev))
        elif num == 13:
            cmd_uniapp_build_h5(argparse.Namespace())
        elif num == 14:
            cmd_uniapp_clean(argparse.Namespace())
        elif num == 15:
            cmd_uniapp_dev_start(argparse.Namespace())
        elif num == 16:
            cmd_uniapp_dev_stop(argparse.Namespace())
        elif num == 17:
            cmd_uniapp_dev_status(argparse.Namespace())
        elif num == 18:
            db_from_env = _yesno("是否按 backend/.env 初始化 PostgreSQL 用户和数据库?", 'n')
            cmd_install_os_deps(argparse.Namespace(db_from_env=db_from_env))
        elif num == 19:
            cmd_backup(argparse.Namespace(
                dir=_prompt("备份目录(留空使用默认)", "") or None,
                no_db=not _yesno("包含数据库备份?", 'y'),
                no_media=not _yesno("包含 media 备份?", 'y'),
                no_systemd=not _yesno("包含 systemd 备份?", 'y'),
                no_code=not _yesno("包含代码快照?", 'y'),
                no_bundle=not _yesno("包含 git bundle?", 'y'),
            ))
        elif num == 20:
            cmd_restore(argparse.Namespace(
                src=_prompt("恢复来源目录(留空使用默认 latest)", "") or None,
                with_db=_yesno("是否恢复数据库?", 'n'),
                wipe_media=_yesno("恢复前是否清空 media?", 'n'),
                reuse_systemd_render=_yesno("是否复用备份中的 systemd 渲染参数?", 'n'),
                include_frontend_dev=_yesno("是否同时恢复前端开发 systemd 单元?", 'n'),
                service_user=None,
                service_group=None,
                project_root=None,
                npm_bin=None,
            ))
        elif num == 21:
            cmd_build_frontends(argparse.Namespace())
        elif num == 22:
            cmd_deploy_frontend_static(argparse.Namespace())
        elif num == 23:
            cmd_install_nginx_conf(argparse.Namespace(reload=_yesno("安装后是否重载 nginx?", 'y')))
        else:
            print("[提示] 无效编号，请重试。")


# ------------------------- 参数解析 -------------------------

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="BS01 项目运维脚本（中文）")
    sub = p.add_subparsers(dest='cmd', required=True)

    # 服务控制
    for name in ('status', 'start', 'stop', 'restart', 'enable', 'disable'):
        sp = sub.add_parser(name, help=f"{name} 服务：backend/web/admin/mobile/celery/celery-transcode/celery-beat/all")
        sp.add_argument('targets', nargs='*', help="目标服务，默认 all")
        sp.set_defaults(func=globals()[f"cmd_{name}"])

    sp = sub.add_parser('reload', help='systemd daemon-reload')
    sp.set_defaults(func=cmd_reload)

    sp = sub.add_parser('logs', help='查看服务日志（使用 journalctl）')
    sp.add_argument('target', choices=['backend', 'web', 'admin', 'mobile', 'celery', 'celery-transcode', 'celery-beat'], help='目标服务')
    sp.add_argument('-n', '--lines', type=int, default=200, help='显示行数，默认 200')
    sp.add_argument('-f', '--follow', action='store_true', help='持续跟随')
    sp.set_defaults(func=cmd_logs)

    # 依赖 / 部署
    sp = sub.add_parser('install', help='安装依赖（后端 + 前端）')
    sp.add_argument('--skip-frontend', action='store_true', help='跳过前端依赖安装')
    sp.set_defaults(func=cmd_install)

    sp = sub.add_parser('install-os-deps', help='安装系统依赖（包装 deploy/scripts/install_os_deps.sh）')
    sp.add_argument('--db-from-env', action='store_true', help='按 backend/.env 初始化 PostgreSQL 用户和数据库')
    sp.set_defaults(func=cmd_install_os_deps)

    sp = sub.add_parser('build-frontends', help='构建前台与管理端生产包')
    sp.set_defaults(func=cmd_build_frontends)

    sp = sub.add_parser('deploy-frontend-static', help='发布前台与管理端静态文件到 /var/www/bs01')
    sp.set_defaults(func=cmd_deploy_frontend_static)

    sp = sub.add_parser('install-nginx-conf', help='安装 Nginx 站点配置')
    sp.add_argument('--no-reload', dest='reload', action='store_false', help='安装后不重载 nginx')
    sp.set_defaults(func=cmd_install_nginx_conf, reload=True)

    sp = sub.add_parser('setup-services', help='安装/更新 systemd 服务单元并重载（默认仅生产服务）')
    sp.add_argument('--enable', action='store_true', help='完成后立即启用并启动默认生产服务')
    sp.add_argument('--include-frontend-dev', action='store_true', help='额外安装前端开发用 systemd 单元')
    sp.add_argument('--project-root', default=str(BASE_DIR), help='渲染 systemd 模板时使用的项目根目录')
    sp.add_argument('--service-user', default=default_service_user(), help='渲染 systemd 模板时使用的运行用户')
    sp.add_argument('--service-group', default=default_service_group(), help='渲染 systemd 模板时使用的运行组')
    sp.add_argument('--npm-bin', default=default_npm_bin(), help='前端开发服务使用的 npm 可执行路径')
    sp.set_defaults(func=cmd_setup_services)

    sp = sub.add_parser('backup', help='执行备份（包装 deploy/scripts/backup.sh）')
    sp.add_argument('--dir', help='备份目录')
    sp.add_argument('--no-db', action='store_true', help='跳过数据库备份')
    sp.add_argument('--no-media', action='store_true', help='跳过 media 备份')
    sp.add_argument('--no-systemd', action='store_true', help='跳过 systemd 单元备份')
    sp.add_argument('--no-code', action='store_true', help='跳过代码快照')
    sp.add_argument('--no-bundle', action='store_true', help='跳过 git bundle')
    sp.set_defaults(func=cmd_backup)

    sp = sub.add_parser('restore', help='执行恢复（包装 deploy/scripts/restore.sh）')
    sp.add_argument('--src', help='恢复来源目录')
    sp.add_argument('--with-db', action='store_true', help='恢复数据库')
    sp.add_argument('--wipe-media', action='store_true', help='恢复前清空 media')
    sp.add_argument('--reuse-systemd-render', action='store_true', help='复用备份中的 systemd 渲染参数')
    sp.add_argument('--include-frontend-dev', action='store_true', help='同时恢复前端开发 systemd 单元')
    sp.add_argument('--service-user', help='覆盖恢复时使用的 service user')
    sp.add_argument('--service-group', help='覆盖恢复时使用的 service group')
    sp.add_argument('--project-root', help='覆盖恢复时使用的 project root')
    sp.add_argument('--npm-bin', help='覆盖恢复时使用的 npm 路径')
    sp.set_defaults(func=cmd_restore)

    # Django 常用命令
    sp = sub.add_parser('migrate', help='数据库迁移')
    sp.set_defaults(func=cmd_migrate)

    sp = sub.add_parser('check', help='Django 健康检查')
    sp.set_defaults(func=cmd_check)

    sp = sub.add_parser('collectstatic', help='收集静态文件（生产）')
    sp.set_defaults(func=cmd_collectstatic)

    sp = sub.add_parser('test', help='运行测试（默认标签为空，可指定 apps）')
    sp.add_argument('label', nargs='?', help='测试标签，例如 apps 或 apps.users')
    sp.add_argument('--keepdb', action='store_true', help='保留测试数据库')
    sp.set_defaults(func=cmd_test)

    sp = sub.add_parser('doctor', help='体检（默认生产服务，可选附加前端开发服务）')
    sp.add_argument('--include-frontend-dev', action='store_true', help='附加检查前端开发端口、CORS 预检与编译状态')
    sp.set_defaults(func=cmd_doctor)

    sp = sub.add_parser('uniapp-build-h5', help='构建 uni-app H5 生产包')
    sp.set_defaults(func=cmd_uniapp_build_h5)

    sp = sub.add_parser('uniapp-clean', help='清理 uni-app 构建产物')
    sp.set_defaults(func=cmd_uniapp_clean)

    sp = sub.add_parser('uniapp-dev-start', help='启动 uni-app 开发服务 (后台)')
    sp.set_defaults(func=cmd_uniapp_dev_start)

    sp = sub.add_parser('uniapp-dev-stop', help='停止 uni-app 开发服务')
    sp.set_defaults(func=cmd_uniapp_dev_stop)

    sp = sub.add_parser('uniapp-dev-status', help='查看 uni-app 开发服务状态')
    sp.set_defaults(func=cmd_uniapp_dev_status)

    return p


def main():
    if len(sys.argv) == 1:
        try:
            interactive_menu()
        except KeyboardInterrupt:
            print("\n[中断] 已取消")
            raise SystemExit(130)
        return

    parser = build_parser()
    args = parser.parse_args()
    try:
        args.func(args)
    except KeyboardInterrupt:
        print("\n[中断] 已取消")
        raise SystemExit(130)


if __name__ == '__main__':
    main()
