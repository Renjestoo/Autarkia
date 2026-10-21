"""Decompile one jar with vineflower into api/decomp/<jar stem> (used for loader and extra library jars).

Usage: decompile_one.py <jar path>
"""

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = Path(r"F:\gradle-home\caches\modules-2\files-2.1")


def main():
    jar = Path(sys.argv[1])
    vf = next(CACHE.glob("org.vineflower/vineflower/*/*/vineflower-*.jar"))
    out = ROOT / "api" / "decomp" / jar.stem
    out.mkdir(parents=True, exist_ok=True)
    print(f"decompiling {jar.name} -> {out}")
    r = subprocess.run([r"F:\jdks\jdk-25\bin\java.exe", "-Xmx3G", "-jar", str(vf), "-log=ERROR", str(jar), str(out)],
                       capture_output=True, text=True)
    print("exit", r.returncode, r.stderr[-2000:])


if __name__ == "__main__":
    main()
