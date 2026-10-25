"""Launch the real NeoForge 26.3 client with every mod of the CurseForge instance plus the current Autarkia jar.

The run directory run-pack/ mirrors the instance mods (Autarkia jars excluded, stale files removed) and gets the
instance config, a test world (or none with --new-world) and quiet client options. --extra-mod adds a jar that only
this run uses (the playtest bridge). The game runs on a private desktop, or on the user's desktop with --visible,
muted, below normal priority, offline as "Dev". The log is copied to build-logs/packrun.log; a crash report or a
missing wait text fails the run.

Usage: packrun.py [--world NAME | --new-world] [--extra-mod JAR] [--visible] [--wait TEXT] [--timeout S] [--server HOST:PORT] [--name Dev]
"""

import argparse
import json
import re
import shutil
import sys
import time
import uuid
from pathlib import Path

from tqdm import tqdm

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gradle as g
import make_world

ROOT = Path(__file__).resolve().parent.parent
INSTALL = Path(r"C:\Users\renat\curseforge\minecraft\Install")
INSTANCE = Path(r"C:\Users\renat\curseforge\minecraft\Instances\Claude Mod")
RUN = ROOT / "run-pack"
JAVA = Path(r"F:\jdks\jdk-25\bin\java.exe")
PROBLEM = r"/WARN\]|/ERROR\]|Exception|Errors in|Failed to|Missing|Unable to load"
EXTERNAL_CACHE = re.compile(r"\[(jeiworldgen|jeitrades)/\]: Unable to read from existing cache file\.$")
CACHE_DECODE = "io.netty.handler.codec.DecoderException: Failed to decode JSON: Failed to parse either. First: Unknown registry key in ResourceKey[minecraft:root / minecraft:item]: "


def rules_ok(rules):
    if not rules:
        return True
    ok = False
    for r in rules:
        cond = True
        if "os" in r:
            cond = r["os"].get("name", "windows") == "windows" and "arch" not in r["os"]
        if "features" in r:
            cond = False
        if cond:
            ok = r["action"] == "allow"
    return ok


def libraries(version_json):
    out = []
    for lib in version_json.get("libraries", []):
        if not rules_ok(lib.get("rules")):
            continue
        art = (lib.get("downloads") or {}).get("artifact")
        if art and art.get("path"):
            out.append(INSTALL / "libraries" / art["path"])
    return out


def args_of(version_json, kind):
    res = []
    for a in (version_json.get("arguments") or {}).get(kind, []):
        if isinstance(a, str):
            res.append(a)
        elif rules_ok(a.get("rules")):
            v = a["value"]
            res.extend(v if isinstance(v, list) else [v])
    return res


def sync_dir(src, dst, skip=lambda p: False):
    dst.mkdir(parents=True, exist_ok=True)
    files = [p for p in src.rglob("*") if p.is_file() and not skip(p)]
    copied = 0
    for p in tqdm(files, desc=f"sync {src.name}", unit="f", mininterval=2):
        t = dst / p.relative_to(src)
        if not t.exists() or t.stat().st_size != p.stat().st_size or t.stat().st_mtime < p.stat().st_mtime:
            t.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(p, t)
            copied += 1
    return copied


def prepare(jar, world, new_world):
    print("phase 1: mods")
    mods = RUN / "mods"
    if mods.exists():
        for p in mods.glob("autarkia-*.jar"):
            p.unlink()
    n = sync_dir(INSTANCE / "mods", mods, skip=lambda p: p.name.lower().startswith("autarkia"))
    wanted = {p.relative_to(INSTANCE / "mods") for p in (INSTANCE / "mods").rglob("*") if p.is_file()}
    stale = [p for p in mods.rglob("*") if p.is_file() and p.relative_to(mods) not in wanted]
    for p in stale:
        print(f"  removed from run-pack (not in the instance): {p.relative_to(mods)}")
        p.unlink()
    shutil.copy2(jar, mods / jar.name)
    print(f"  {n} instance jars updated, {len(stale)} stale files removed, plus {jar.name}")
    print("phase 2: config")
    if not (RUN / "config").exists():
        sync_dir(INSTANCE / "config", RUN / "config", skip=lambda p: p.name.startswith("autarkia"))
    if (INSTANCE / "defaultconfigs").exists() and not (RUN / "defaultconfigs").exists():
        sync_dir(INSTANCE / "defaultconfigs", RUN / "defaultconfigs")
    print("phase 3: world and options")
    if new_world:
        old = RUN / "saves" / "Autarkia Playtest"
        if old.exists():
            shutil.rmtree(old)
        print("  no world copied: the playtest bot creates a new survival world")
    else:
        src = ROOT / "run" / "server" / "world"
        dst = RUN / "saves" / world
        if dst.exists():
            shutil.rmtree(dst)
        shutil.copytree(src, dst, ignore=shutil.ignore_patterns("session.lock"))
    make_world.set_options(RUN / "options.txt")


def command(args, jar):
    base = json.loads((INSTALL / "versions" / "26.3" / "26.3.json").read_text(encoding="utf-8"))
    neo = json.loads((INSTALL / "versions" / "neoforge-26.3.0.95" / "neoforge-26.3.0.95.json").read_text(encoding="utf-8"))
    cp = libraries(neo) + libraries(base) + [INSTALL / "versions" / "neoforge-26.3.0.95" / "neoforge-26.3.0.95.jar"]
    missing = [str(p) for p in cp if not p.exists()]
    if missing:
        raise SystemExit(f"missing libraries: {missing[:5]}")
    natives = Path(r"F:\tmp\autarkia\natives")
    natives.mkdir(parents=True, exist_ok=True)
    subst = {
        "${natives_directory}": str(natives), "${launcher_name}": "autarkia-packrun", "${launcher_version}": "1",
        "${classpath}": ";".join(str(p) for p in cp), "${classpath_separator}": ";", "${library_directory}": str(INSTALL / "libraries"),
        "${version_name}": "neoforge-26.3.0.95", "${auth_player_name}": args.name, "${game_directory}": str(RUN),
        "${assets_root}": str(INSTALL / "assets"), "${assets_index_name}": base["assetIndex"]["id"],
        "${auth_uuid}": str(uuid.uuid3(uuid.NAMESPACE_OID, "OfflinePlayer:" + args.name)).replace("-", ""),
        "${auth_access_token}": "0", "${clientid}": "0", "${auth_xuid}": "0", "${user_type}": "legacy", "${version_type}": "release",
        "${user_properties}": "{}",
    }

    def fill(a):
        for k, v in subst.items():
            a = a.replace(k, v)
        return a

    jvm = [fill(a) for a in args_of(neo, "jvm") + args_of(base, "jvm")]
    jvm += ["-Xmx6G", "-Djava.io.tmpdir=F:/tmp/autarkia", "-Dorg.lwjgl.system.SharedLibraryExtractPath=F:/tmp/autarkia/lwjgl",
            "-Dfile.encoding=UTF-8"]
    game = [fill(a) for a in args_of(base, "game") + args_of(neo, "game")]
    game += ["--width", "1280", "--height", "720", "--offlineDeveloperMode"]
    if args.server:
        game += ["--quickPlayMultiplayer", args.server]
    elif not args.new_world:
        game += ["--quickPlaySingleplayer", args.world]
    main_class = neo["mainClass"]
    argfile = RUN / "packrun_args.txt"
    lines = jvm + [main_class] + game
    argfile.write_text("\n".join(f'"{a}"' if " " in a else a for a in lines), encoding="utf-8")
    return f'"{JAVA}" @"{argfile}"'


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--world", default="AutarkiaTest")
    p.add_argument("--new-world", action="store_true", help="copy no world and open the title screen")
    p.add_argument("--extra-mod", help="a jar that only this run loads, such as the playtest bridge")
    p.add_argument("--wait", default="Stopping!")
    p.add_argument("--timeout", type=int, default=900)
    p.add_argument("--server")
    p.add_argument("--name", default="Dev")
    p.add_argument("--visible", action="store_true", help="show the game window on the user's desktop")
    args = p.parse_args()
    props = dict(l.split("=", 1) for l in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines() if "=" in l)
    jar = ROOT / "build" / "libs" / f"autarkia-{props['mod_version']}.jar"
    if not jar.exists():
        raise SystemExit(f"jar {jar} missing; run tools/gradle.py build")
    prepare(jar, args.world, args.new_world)
    if args.extra_mod:
        extra = Path(args.extra_mod)
        shutil.copy2(extra, RUN / "mods" / extra.name)
        print(f"  run-only mod: {extra.name}")
    cmd = command(args, jar)
    log = g.LOGS / "packrun.log"
    g.LOGS.mkdir(exist_ok=True)
    log.unlink(missing_ok=True)
    before = g.crash_files([RUN])
    print(f"phase 4: launch ({jar.name}{', visible' if args.visible else ''})", flush=True)
    handle, pid = g.spawn(cmd, RUN, log, hidden_desktop=not args.visible)
    start = time.time()
    seen = False
    pos = 0
    while time.time() - start < args.timeout:
        chunk, pos = g.tail(log, pos)
        for line in chunk.splitlines():
            if "Autarkia" in line and ("ERROR" in line or "WARN" in line) or "---- Minecraft Crash Report" in line:
                print("  |", line[:300], flush=True)
            if args.wait in line:
                seen = True
        if g.exit_code(handle) is not None:
            break
        if seen:
            for _ in range(90):
                if g.exit_code(handle) is not None:
                    break
                time.sleep(1)
            break
        time.sleep(1)
    if g.exit_code(handle) is None:
        g.kill_tree(pid)
    crashes = g.crash_files([RUN]) - before
    latest = RUN / "logs" / "latest.log"
    print(f"phase 5: done after {time.time() - start:.0f}s; wait text seen: {seen}; latest.log: {latest}")
    for c in crashes:
        print("NEW CRASH REPORT:", c)
    lines = latest.read_text(encoding="utf-8", errors="replace").splitlines() if latest.exists() else []
    bad, external = [], []
    for i, l in enumerate(lines):
        if "autarkia" in l.lower() and re.search(PROBLEM, l):
            (external if i and EXTERNAL_CACHE.search(lines[i - 1]) and l.startswith(CACHE_DECODE) else bad).append(l)
    if not latest.exists():
        bad.append("no latest.log")
    print(f"autarkia WARN/ERROR lines in latest.log: {len(bad)}")
    for l in bad[:40]:
        print("  ", l[:300])
    print(f"known external lines (JEI World Gen / JEI Trades cache read before registries, all mods): {len(external)}")
    for l in external[:5]:
        print("  ", l[:200])
    sys.exit(0 if seen and not crashes and not bad else 1)


if __name__ == "__main__":
    main()
