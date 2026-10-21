"""Run Gradle tasks and dev runs for Autarkia with the machine rules applied.

Usage:
    gradle.py build
    gradle.py runServer [--rcon "cmd1;cmd2"] [--wait "TEXT"] [--timeout 900]
    gradle.py runClient [--world NAME] [--director FILE] [--wait TEXT] [--timeout 900] [--name client]
    gradle.py <any gradle task> [gradle args]

Gradle and every game process run below normal priority, on a private Win32 desktop,
muted, with temp and caches on F:. Full logs go to build-logs/. The run fails loudly on
crash reports, failed server starts and BUILD FAILED.
"""

import argparse
import ctypes
import os
import re
import shutil
import socket
import struct
import subprocess
import sys
import time
from ctypes import wintypes
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOGS = ROOT / "build-logs"
JAVA_HOME = Path(r"F:\jdks\jdk-25")
TMP = Path(r"F:\tmp\autarkia")
SERVER_DIR = ROOT / "run" / "server"
RCON_PORT = 25591
RCON_PASS = "autarkia-dev"
DESKTOP_NAME = "AutarkiaDesk"
BELOW_NORMAL = 0x00004000
CREATE_NO_WINDOW = 0x08000000
CRASH_MARKERS = ("---- Minecraft Crash Report ----", "#@!@# Game crashed!", "Failed to start the minecraft server",
                 "Exception in server tick loop", "Encountered an unexpected exception", "BUILD FAILED")


def env():
    e = dict(os.environ)
    TMP.mkdir(parents=True, exist_ok=True)
    e.update({
        "GRADLE_USER_HOME": r"F:\gradle-home",
        "JAVA_HOME": str(JAVA_HOME),
        "TEMP": str(TMP), "TMP": str(TMP),
        "GRADLE_OPTS": f"-Djava.io.tmpdir={TMP.as_posix()} -Dfile.encoding=UTF-8",
        "JAVA_TOOL_OPTIONS": f"-Djava.io.tmpdir={TMP.as_posix()}",
        "ALSOFT_DRIVERS": "null",
        "PATH": str(JAVA_HOME / "bin") + os.pathsep + e.get("PATH", ""),
    })
    return e


class STARTUPINFOW(ctypes.Structure):
    _fields_ = [("cb", wintypes.DWORD), ("lpReserved", wintypes.LPWSTR), ("lpDesktop", wintypes.LPWSTR),
                ("lpTitle", wintypes.LPWSTR), ("dwX", wintypes.DWORD), ("dwY", wintypes.DWORD),
                ("dwXSize", wintypes.DWORD), ("dwYSize", wintypes.DWORD), ("dwXCountChars", wintypes.DWORD),
                ("dwYCountChars", wintypes.DWORD), ("dwFillAttribute", wintypes.DWORD), ("dwFlags", wintypes.DWORD),
                ("wShowWindow", wintypes.WORD), ("cbReserved2", wintypes.WORD), ("lpReserved2", ctypes.c_void_p),
                ("hStdInput", wintypes.HANDLE), ("hStdOutput", wintypes.HANDLE), ("hStdError", wintypes.HANDLE)]


class PROCESS_INFORMATION(ctypes.Structure):
    _fields_ = [("hProcess", wintypes.HANDLE), ("hThread", wintypes.HANDLE),
                ("dwProcessId", wintypes.DWORD), ("dwThreadId", wintypes.DWORD)]


_desktop_handle = None


def private_desktop():
    """Create (or open) the private desktop and return its full name."""
    global _desktop_handle
    if _desktop_handle is None:
        user32 = ctypes.WinDLL("user32", use_last_error=True)
        user32.CreateDesktopW.restype = wintypes.HANDLE
        h = user32.CreateDesktopW(DESKTOP_NAME, None, None, 0, 0x10000000, None)
        if not h:
            raise OSError(f"CreateDesktopW failed: {ctypes.get_last_error()}")
        _desktop_handle = h
    return "WinSta0\\" + DESKTOP_NAME


def spawn(cmdline, cwd, log_path, hidden_desktop=True):
    """Start cmd.exe /c cmdline with output redirected to log_path; return (process handle, pid)."""
    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
    si = STARTUPINFOW()
    si.cb = ctypes.sizeof(si)
    if hidden_desktop:
        si.lpDesktop = private_desktop()
    pi = PROCESS_INFORMATION()
    full = f'cmd.exe /c "{cmdline} > "{log_path}" 2>&1"'
    e = env()
    block = "".join(f"{k}={v}\0" for k, v in e.items()) + "\0"
    ok = kernel32.CreateProcessW(None, ctypes.create_unicode_buffer(full), None, None, False,
                                 BELOW_NORMAL | CREATE_NO_WINDOW | 0x00000400, ctypes.create_unicode_buffer(block),
                                 str(cwd), ctypes.byref(si), ctypes.byref(pi))
    if not ok:
        raise OSError(f"CreateProcessW failed: {ctypes.get_last_error()}")
    kernel32.CloseHandle(pi.hThread)
    return pi.hProcess, pi.dwProcessId


def exit_code(handle):
    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
    code = wintypes.DWORD()
    kernel32.GetExitCodeProcess(handle, ctypes.byref(code))
    return None if code.value == 259 else code.value


def kill_tree(pid):
    subprocess.run(["taskkill", "/T", "/F", "/PID", str(pid)], capture_output=True)


def rcon(commands, host="127.0.0.1", port=RCON_PORT):
    """Send commands over RCON and return the responses."""
    out = []
    with socket.create_connection((host, port), timeout=30) as s:
        def packet(rid, kind, body):
            data = struct.pack("<ii", rid, kind) + body.encode("utf-8") + b"\0\0"
            s.sendall(struct.pack("<i", len(data)) + data)
            size = struct.unpack("<i", recv_exact(s, 4))[0]
            payload = recv_exact(s, size)
            return payload[8:-2].decode("utf-8", "replace")
        packet(1, 3, RCON_PASS)
        for i, c in enumerate(commands):
            print(f"  rcon> {c}", flush=True)
            out.append(packet(10 + i, 2, c))
    return out


def recv_exact(s, n):
    buf = b""
    while len(buf) < n:
        chunk = s.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("RCON closed")
        buf += chunk
    return buf


def prepare_server():
    SERVER_DIR.mkdir(parents=True, exist_ok=True)
    (SERVER_DIR / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    props = SERVER_DIR / "server.properties"
    wanted = {"server-port": "25590", "enable-rcon": "true", "rcon.port": str(RCON_PORT), "rcon.password": RCON_PASS,
              "online-mode": "false", "spawn-protection": "0", "level-name": "world", "motd": "Autarkia dev",
              "pause-when-empty-seconds": "0", "view-distance": "8", "simulation-distance": "8",
              "level-seed": "autarkia", "allow-flight": "true", "enable-command-block": "true"}
    cur = {}
    if props.exists():
        for line in props.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.startswith("#"):
                k, v = line.split("=", 1)
                cur[k] = v
    cur.update(wanted)
    props.write_text("".join(f"{k}={v}\n" for k, v in cur.items()), encoding="utf-8")


def crash_files(dirs):
    found = set()
    for d in dirs:
        cr = d / "crash-reports"
        if cr.exists():
            found |= {p for p in cr.glob("*.txt")}
    return found


def tail(path, pos):
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            f.seek(pos)
            data = f.read()
            return data, f.tell()
    except FileNotFoundError:
        return "", pos


def summarize(log_path):
    text = Path(log_path).read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    errs = []
    seen = set()
    for i, l in enumerate(lines):
        if re.search(r"\.java:\d+: (error|warning):", l):
            if l.strip() not in seen:
                seen.add(l.strip())
                errs.extend(lines[i:i + 3])
        elif re.search(r"ERROR\]|\[ERROR|Exception|WARN.*autarkia|autarkia.*WARN", l):
            errs.append(l)
    print(f"--- {len(lines)} log lines in {log_path}; {len(errs)} problem lines (first 150):")
    for l in errs[:150]:
        print("  ", l[:400])
    print("--- last 15 lines:")
    for l in lines[-15:]:
        print("  ", l[:400])


def run_task(task, extra, args):
    LOGS.mkdir(exist_ok=True)
    log = LOGS / f"{args.name or task}.log"
    if log.exists():
        log.unlink()
    game_dirs = [ROOT / "run" / "server", ROOT / "run" / "client", ROOT / "run" / "client2"]
    before = crash_files(game_dirs)
    if task == "runServer":
        prepare_server()
    cmd = f'"{ROOT / "gradlew.bat"}" {task} {" ".join(extra)} --console=plain'
    print(f"[gradle.py] start {cmd} (log {log})", flush=True)
    handle, pid = spawn(cmd, ROOT, log, hidden_desktop=True)
    pos = 0
    started = time.time()
    done_seen = False
    sent = False
    waited = args.wait is None
    stop_sent = False
    failure = None
    printed = 0
    while True:
        chunk, pos = tail(log, pos)
        if chunk:
            for line in chunk.splitlines():
                if any(m in line for m in CRASH_MARKERS) and failure is None:
                    failure = line.strip()
                if "[TEST" in line or "RESULT" in line or "PASS" in line or "FAIL" in line or "autarkia" in line.lower() and ("ERROR" in line or "WARN" in line):
                    if printed < 400:
                        print("  |", line[:300], flush=True)
                        printed += 1
                if task == "runServer" and "Done (" in line and "For help" in line:
                    done_seen = True
                if args.wait and args.wait in line:
                    waited = True
        code = exit_code(handle)
        if code is not None:
            print(f"[gradle.py] process exited with {code} after {time.time() - started:.0f}s", flush=True)
            break
        if failure:
            print(f"[gradle.py] failure marker: {failure}", flush=True)
            time.sleep(5)
            kill_tree(pid)
            break
        if task == "runServer" and done_seen:
            if not sent:
                sent = True
                cmds = [c.strip() for c in (args.rcon or "").split(";") if c.strip()]
                stop_sent = "stop" in cmds
                if cmds:
                    for r in rcon(cmds):
                        if r:
                            print("  rcon<", r[:500], flush=True)
            if waited and not stop_sent:
                time.sleep(2)
                rcon(["stop"])
                stop_sent = True
        if task.startswith("runClient") and waited and args.wait:
            print("[gradle.py] client wait text seen, waiting for clean exit", flush=True)
            for _ in range(90):
                if exit_code(handle) is not None:
                    break
                time.sleep(1)
            if exit_code(handle) is None:
                kill_tree(pid)
            stop_sent = True
            break
        if time.time() - started > args.timeout:
            failure = f"timeout after {args.timeout}s"
            print(f"[gradle.py] {failure}", flush=True)
            if task == "runServer" and done_seen and not stop_sent:
                try:
                    rcon(["stop"])
                    time.sleep(20)
                except OSError:
                    pass
            kill_tree(pid)
            break
        time.sleep(1)
    new_crashes = crash_files(game_dirs) - before
    for c in sorted(new_crashes):
        print(f"[gradle.py] NEW CRASH REPORT: {c}")
        failure = failure or f"crash report {c.name}"
    summarize(log)
    code = exit_code(handle)
    if failure or (code not in (0, None) and not (task.startswith("run") and stop_sent)):
        print(f"[gradle.py] FAILED: {failure or f'exit {code}'}")
        return 1
    print("[gradle.py] OK")
    return 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("task")
    p.add_argument("--rcon", default=os.environ.get("AUTARKIA_RCON"))
    p.add_argument("--wait", default=os.environ.get("AUTARKIA_WAIT"))
    p.add_argument("--timeout", type=int, default=1800)
    p.add_argument("--name", default=None)
    args, extra = p.parse_known_args()
    sys.exit(run_task(args.task, extra, args))


if __name__ == "__main__":
    main()
