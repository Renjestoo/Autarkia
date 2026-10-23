"""Copy the built Autarkia jar into the CurseForge instance mods folder and remove older Autarkia jars there.

Only jars that this repo built (present in build/libs) are removed; any other autarkia-*.jar in the mods folder
stops the script before anything changes, because it belongs to someone else.

Usage: install_jar.py
"""

import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODS = Path(r"C:\Users\renat\curseforge\minecraft\Instances\Claude Mod\mods")


def main():
    props = dict(l.split("=", 1) for l in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines() if "=" in l)
    jar = ROOT / "build" / "libs" / f"autarkia-{props['mod_version']}.jar"
    if not jar.exists():
        raise SystemExit(f"jar {jar} missing; run tools/gradle.py build")
    olds = [p for p in MODS.glob("autarkia-*.jar") if p.name != jar.name]
    foreign = [p.name for p in olds if not (jar.parent / p.name).exists()]
    if foreign:
        raise SystemExit(f"not built by this repo, left untouched: {foreign}; remove them yourself, then run again")
    for old in olds:
        old.unlink()
        print(f"removed {old.name}")
    shutil.copy2(jar, MODS / jar.name)
    present = sorted(p.name for p in MODS.glob("autarkia-*.jar"))
    print(f"installed {jar.name}; autarkia jars in instance: {present}")
    if present != [jar.name]:
        raise SystemExit("instance must hold exactly one autarkia jar")


if __name__ == "__main__":
    main()
