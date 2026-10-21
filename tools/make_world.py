"""Copy the dev server world into a client save so scripted client scenes load a fixed, repeatable world.

Usage: make_world.py [save name]   (default AutarkiaTest; source run/server/world)
"""

import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main():
    name = sys.argv[1] if len(sys.argv) > 1 else "AutarkiaTest"
    src = ROOT / "run" / "server" / "world"
    if not (src / "level.dat").exists():
        raise SystemExit(f"no server world at {src}; run tools/gradle.py runServer first")
    for client in ("client", "client2"):
        dst = ROOT / "run" / client / "saves" / name
        if dst.exists():
            shutil.rmtree(dst)
        shutil.copytree(src, dst, ignore=shutil.ignore_patterns("session.lock"))
        print(f"copied {src} -> {dst}")
        set_options(ROOT / "run" / client / "options.txt")


def set_options(path):
    """Write client options that keep scripted scenes free of tutorials, toasts and onboarding screens."""
    wanted = {"tutorialStep": "none", "realmsNotifications": "false", "joinedFirstServer": "true", "skipMultiplayerWarning": "true",
              "onboardAccessibility": "false", "pauseOnLostFocus": "false", "renderDistance": "12", "fov": "0.0", "guiScale": "2",
              "soundCategory_master": "0.0"}
    cur = {}
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if ":" in line:
                k, v = line.split(":", 1)
                cur[k] = v
    cur.update(wanted)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(f"{k}:{v}\n" for k, v in cur.items()), encoding="utf-8")
    print(f"options written: {path}")


if __name__ == "__main__":
    main()
