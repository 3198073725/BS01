#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BS01 项目运维脚本（Python 版）

用途：统一管理后端、两个前端与移动端（Metro）的运行、日志、依赖、迁移、测试与体检。
- 后端：Django + Gunicorn（systemd 服务名：bs01-gunicorn）
- Web 客户端：Vue CLI（systemd 服务名：bs01-web）
- 管理端：Vue CLI（systemd 服务名：bs01-admin）
- 移动端：React Native Metro（systemd 服务名：bs01-mobile）

示例：
  python bs01ctl.py status                 # 查看全部服务状态
  python bs01ctl.py restart all            # 重启全部服务
  python bs01ctl.py logs backend -f -n 200 # 实时查看后端日志
  python bs01ctl.py install                # 安装后端与前端依赖
  python bs01ctl.py migrate                # 执行数据库迁移（PostgreSQL）
  python bs01ctl.py test apps --keepdb     # 运行测试（使用 pg 测试库）
  python bs01ctl.py doctor                 # 体检：服务与端口基本检查

注意：本脚本假定运行路径为 /root/BS01 且以 root 运行；若非 root，将尝试使用 sudo。
"""

import argparse
import os
import sys
import subprocess
import shlex
import socket
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
VENV_PY = BASE_DIR / 'venv' / 'bin' / 'python'
VENV_PIP = BASE_DIR / 'venv' / 'bin' / 'pip'
MANAGE_PY = BASE_DIR / 'backend' / 'manage.py'
ENV_FILE = BASE_DIR / 'backend' / '.env'
SYSTEMD_DIR = Path('/etc/systemd/system')
SERVICE_DIR = BASE_DIR / 'deploy' / 'systemd'

SERVICES = {
    'backend': 'bs01-gunicorn.service',
    'web': 'bs01-web.service',
    'admin': 'bs01-admin.service',
    'mobile': 'bs01-mobile.service',
}
DEFAULT_TARGETS = ['backend', 'web', 'admin']

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


def systemctl(args: str) -> int:
    cmd = f"systemctl {args}"
    if not is_root():
        cmd = f"sudo {cmd}"
    return run(cmd)


def journalctl(unit: str, lines: int = 200, follow: bool = False) -> int:
    opt_f = "-f" if follow else ""
    cmd = f"journalctl -u {shlex.quote(unit)} -n {int(lines)} {opt_f} --no-pager"
    if not is_root():
        cmd = f"sudo {cmd}"
    return run(cmd)


def ensure_paths():
    if not VENV_PY.exists():
        print("[错误] 未找到虚拟环境：", VENV_PY)
        print("请先创建：python3 -m venv venv && ./venv/bin/pip install -r requirements.txt")
        raise SystemExit(1)
    if not MANAGE_PY.exists():
        print("[错误] 未找到 manage.py：", MANAGE_PY)
        raise SystemExit(1)
    if not ENV_FILE.exists():
        print("[警告] 未找到环境文件 .env：", ENV_FILE)


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

def cmd_status(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        systemctl(f"status {u} --no-pager")


def cmd_start(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        systemctl(f"enable --now {u}")


def cmd_stop(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        systemctl(f"stop {u}")


def cmd_restart(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        systemctl(f"restart {u}")


def cmd_reload(args):
    systemctl("daemon-reload")


def cmd_enable(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        systemctl(f"enable {u}")


def cmd_disable(args):
    units = list_units(args.targets or ['all'])
    for u in units:
        systemctl(f"disable {u}")


def cmd_logs(args):
    unit = SERVICES.get(args.target)
    if not unit:
        print("[错误] 目标应为 backend/web/admin 之一")
        raise SystemExit(2)
    journalctl(unit, lines=args.lines, follow=args.follow)


def cmd_install(args):
    """安装依赖（后端 + 前端）。"""
    # 后端 Python 依赖
    run(f"{VENV_PIP} install -r requirements.txt", cwd=BASE_DIR)
    # 前端依赖
    if not args.skip_frontend:
        if (BASE_DIR / 'web-client' / 'package.json').exists():
            run("npm i --no-audit --no-fund", cwd=BASE_DIR / 'web-client')
        if (BASE_DIR / 'admin-console' / 'package.json').exists():
            run("npm ci --no-audit --no-fund", cwd=BASE_DIR / 'admin-console')


def cmd_setup_services(args):
    """安装/更新 systemd 单元并重载。"""
    files = [
        SERVICE_DIR / 'bs01-gunicorn.service',
        SERVICE_DIR / 'bs01-web.service',
        SERVICE_DIR / 'bs01-admin.service',
        SERVICE_DIR / 'bs01-mobile.service',  # 仅安装，不默认启用
    ]
    for f in files:
        if not f.exists():
            print("[警告] 未找到服务文件：", f)
            continue
        dst = SYSTEMD_DIR / f.name
        cmd = f"cp {shlex.quote(str(f))} {shlex.quote(str(dst))}"
        if not is_root():
            cmd = f"sudo {cmd}"
        run(cmd)
    systemctl("daemon-reload")
    if args.enable:
        cmd_start(argparse.Namespace(targets=['all']))


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
    """基本体检：服务状态 + 端口连通性 + 关键文件检查。"""
    print("[信息] 检查服务状态...")
    cmd_status(argparse.Namespace(targets=['all']))
    print("\n[信息] 检查关键文件...")
    for p in [VENV_PY, MANAGE_PY, ENV_FILE]:
        print("  ✅ 存在" if p.exists() else "  ❌ 不存在", "-", p)
    print("\n[信息] 检查端口连通性...")
    targets = [
        ("后端", "127.0.0.1", 8000),
        ("Web", "127.0.0.1", 8080),
        ("Admin", "127.0.0.1", 8082),
    ]
    for name, host, port in targets:
        try:
            with socket.create_connection((host, port), timeout=1.5):
                print(f"  ✅ {name} {host}:{port} 可连接")
        except OSError as e:
            print(f"  ❌ {name} {host}:{port} 不可用：{e}")


def interactive_menu():
    ensure_paths()
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
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile'], 'all')
            cmd_status(argparse.Namespace(targets=[tgt]))
        elif num == 2:
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile'], 'all')
            cmd_start(argparse.Namespace(targets=[tgt]))
        elif num == 3:
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile'], 'all')
            cmd_stop(argparse.Namespace(targets=[tgt]))
        elif num == 4:
            tgt = _choice("目标服务", ['all', 'backend', 'web', 'admin', 'mobile'], 'all')
            cmd_restart(argparse.Namespace(targets=[tgt]))
        elif num == 5:
            tgt = _choice("目标服务", ['backend', 'web', 'admin', 'mobile'], 'backend')
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
            en = _yesno("安装后是否启用并启动后端与两个前端? (mobile 不默认启用)", 'n')
            cmd_setup_services(argparse.Namespace(enable=en))
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
            cmd_doctor(argparse.Namespace())
        else:
            print("[提示] 无效编号，请重试。")


# ------------------------- 参数解析 -------------------------

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="BS01 项目运维脚本（中文）")
    sub = p.add_subparsers(dest='cmd', required=True)

    # 服务控制
    for name in ('status', 'start', 'stop', 'restart', 'enable', 'disable'):
        sp = sub.add_parser(name, help=f"{name} 服务：backend/web/admin/mobile/all")
        sp.add_argument('targets', nargs='*', help="目标服务，默认 all")
        sp.set_defaults(func=globals()[f"cmd_{name}"])

    sp = sub.add_parser('reload', help='systemd daemon-reload')
    sp.set_defaults(func=cmd_reload)

    sp = sub.add_parser('logs', help='查看服务日志（使用 journalctl）')
    sp.add_argument('target', choices=['backend', 'web', 'admin', 'mobile'], help='目标服务')
    sp.add_argument('-n', '--lines', type=int, default=200, help='显示行数，默认 200')
    sp.add_argument('-f', '--follow', action='store_true', help='持续跟随')
    sp.set_defaults(func=cmd_logs)

    # 依赖 / 部署
    sp = sub.add_parser('install', help='安装依赖（后端 + 前端）')
    sp.add_argument('--skip-frontend', action='store_true', help='跳过前端依赖安装')
    sp.set_defaults(func=cmd_install)

    sp = sub.add_parser('setup-services', help='安装/更新 systemd 服务单元并重载')
    sp.add_argument('--enable', action='store_true', help='完成后立即启用并启动全部服务')
    sp.set_defaults(func=cmd_setup_services)

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

    sp = sub.add_parser('doctor', help='体检（状态/端口/文件）')
    sp.set_defaults(func=cmd_doctor)

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
