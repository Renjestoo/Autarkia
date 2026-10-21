"""Build a local API reference for 26.3 in api/: decompiled Minecraft sources and library signatures.

Minecraft + NeoForge: extracted from the ModDevGradle sources artifact.
Libraries: sources jars from the Gradle cache when present, else `javap -p` signatures per class.
"""

import subprocess
import zipfile
from pathlib import Path

from tqdm import tqdm

ROOT = Path(__file__).resolve().parent.parent
API = ROOT / "api"
CACHE = Path(r"F:\gradle-home\caches\modules-2\files-2.1")
JAVAP = r"F:\jdks\jdk-25\bin\javap.exe"
LIBS = {
    "geckolib": "com.geckolib",
    "smartbrainlib": "net.tslat",
    "tinymultiblocklib": "net.nikdo53",
    "magicparticleslib": "com.klikli_dev",
    "guideme": "org.appliedenergistics",
    "frozenlib": "maven.modrinth/frozenlib",
    "jade": "maven.modrinth/jade",
    "playeranim": "com.zigythebird.playeranim",
    "jei": "mezz.jei",
    "yacl": "dev.isxander",
}


def extract(jar, dest, suffix):
    dest.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(jar) as z:
        names = [n for n in z.namelist() if n.endswith(suffix)]
        for n in tqdm(names, desc=f"extract {jar.name}", unit="f", mininterval=5):
            out = dest / n
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(z.read(n))
    return len(names)


def javap_dump(jar, out_file):
    with zipfile.ZipFile(jar) as z:
        classes = [n[:-6].replace("/", ".") for n in z.namelist() if n.endswith(".class") and "META-INF" not in n]
    out_file.parent.mkdir(parents=True, exist_ok=True)
    with open(out_file, "w", encoding="utf-8") as f:
        for i in tqdm(range(0, len(classes), 200), desc=f"javap {jar.name}", unit="batch", mininterval=5):
            batch = classes[i:i + 200]
            r = subprocess.run([JAVAP, "-p", "-cp", str(jar)] + batch, capture_output=True, text=True, encoding="utf-8", errors="replace")
            f.write(r.stdout)
    return len(classes)


def main():
    print("phase 1: minecraft sources")
    src = ROOT / "build" / "moddev" / "artifacts" / "minecraft-patched-26.3.0.95-sources.jar"
    n = extract(src, API / "mc", ".java")
    print(f"  {n} java files")
    print("phase 1b: neoforge sources")
    for s in (CACHE / "net.neoforged" / "neoforge").rglob("neoforge-*-sources.jar"):
        print(f"  {extract(s, API / 'mc', '.java')} java files from {s.name}")
    print("phase 2: libraries")
    for name, group in LIBS.items():
        base = CACHE / group
        jars = sorted(base.rglob("*.jar"))
        sources = [j for j in jars if j.name.endswith("-sources.jar")]
        binaries = [j for j in jars if not j.name.endswith("-sources.jar") and "javadoc" not in j.name]
        if sources:
            for s in sources:
                extract(s, API / name / "src", ".java")
        for b in binaries:
            javap_dump(b, API / name / f"{b.stem}.txt")
    anth = ROOT / "libs" / "anthology-1.0.0+26.3-neoforge.jar"
    javap_dump(anth, API / "anthology" / "anthology.txt")
    print("phase 3: decompile library jars with vineflower")
    vf = next(CACHE.glob("org.vineflower/vineflower/*/*/vineflower-*.jar"))
    targets = [anth] + [j for name, group in LIBS.items() for j in sorted((CACHE / group).rglob("*.jar"))
                        if not j.name.endswith(("-sources.jar", "-javadoc.jar")) and "common-31" not in j.name
                        and "gui-31" not in j.name and "lib-31" not in j.name and "neoforge-31" not in j.name]
    for j in tqdm(targets, desc="vineflower", unit="jar"):
        out = API / "decomp" / j.stem
        if out.exists():
            continue
        out.mkdir(parents=True)
        subprocess.run([r"F:\jdks\jdk-25\bin\java.exe", "-Xmx3G", "-jar", str(vf), "-log=ERROR", str(j), str(out)],
                       capture_output=True, text=True)
    print("done")


if __name__ == "__main__":
    main()
