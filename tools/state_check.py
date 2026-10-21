"""Print a short state summary of the project: tools, generated asset counts, git status."""

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GEN = ROOT / "src" / "generated" / "resources" / "assets" / "autarkia"


def main():
    print("tools:", sorted(p.name for p in (ROOT / "tools").glob("*.py")))
    for sub in ["textures/block", "textures/item", "textures/entity", "textures/armor", "geckolib/models", "geckolib/animations"]:
        d = GEN / sub
        n = len(list(d.rglob("*"))) if d.exists() else 0
        print(f"{sub}: {n}")
    art = ROOT / "screenshots" / "art"
    print("contact sheets:", sorted(p.name for p in art.glob("*.png")) if art.exists() else [])
    print(subprocess.run(["git", "status", "--short"], cwd=ROOT, capture_output=True, text=True).stdout[:3000])
    print("java procs:", subprocess.run(["tasklist", "/FI", "IMAGENAME eq java.exe"], capture_output=True, text=True).stdout[-600:])


if __name__ == "__main__":
    main()
