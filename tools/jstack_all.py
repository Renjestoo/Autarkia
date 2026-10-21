"""Write a full thread dump of a JVM to build-logs/jstack_<pid>.txt and print threads that block on network or futures.

Usage: jstack_all.py PID
"""

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main():
    pid = sys.argv[1]
    dump = subprocess.run([r"F:\jdks\jdk-25\bin\jstack.exe", pid], capture_output=True, text=True).stdout
    out = ROOT / "build-logs" / f"jstack_{pid}.txt"
    out.write_text(dump, encoding="utf-8")
    print(f"phase: wrote {len(dump)} chars to {out}")
    keys = ("Socket", "http", "HttpClient", "CompletableFuture.get", "join(", "realms", "Realms", "authlib", "Yggdrasil", "ProfileKey")
    for block in dump.split("\n\n"):
        if any(k in block for k in keys):
            print(block[:2500])
            print("----")


if __name__ == "__main__":
    main()
