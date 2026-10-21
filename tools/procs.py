"""List (and with --kill, terminate) java.exe processes started from this project or the pack run directory.

Usage: procs.py [--kill]
"""

import subprocess
import sys


def main():
    out = subprocess.run(["powershell", "-NoProfile", "-Command",
                          "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | ForEach-Object { \"$($_.ProcessId)`t$($_.CommandLine)\" }"],
                         capture_output=True, text=True).stdout
    rows = [l for l in out.splitlines() if l.strip()]
    print(f"{len(rows)} java processes")
    for r in rows:
        pid, _, cmd = r.partition("\t")
        ours = "autarkia" in cmd.lower() or "gradle" in cmd.lower()
        print(f"  {pid} ours={ours} {cmd[:160]}")
        if ours and "--kill" in sys.argv:
            subprocess.run(["taskkill", "/T", "/F", "/PID", pid], capture_output=True)
            print(f"  killed {pid}")


if __name__ == "__main__":
    main()
