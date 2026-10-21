"""Extract vanilla 26.3 asset and data files into api/vanilla for format reference."""

import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAR = Path(r"C:\Users\renat\curseforge\minecraft\Install\versions\26.3\26.3.jar")
OUT = ROOT / "api" / "vanilla"


def main():
    with zipfile.ZipFile(JAR) as z:
        names = z.namelist()
        keep = [n for n in names if (n.startswith("assets/") or n.startswith("data/")) and not n.endswith((".png", ".ogg", ".nbt", "/"))]
        print(f"extracting {len(keep)} vanilla json/text files")
        for n in keep:
            out = OUT / n
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(z.read(n))
        pngs = [n for n in names if n.endswith(".png") and ("textures/item/" in n or "textures/block/" in n or "textures/gui/" in n or "textures/entity/equipment" in n)]
        print(f"extracting {len(pngs)} vanilla textures for palette reference")
        for n in pngs:
            out = OUT / n
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(z.read(n))
    print("done")


if __name__ == "__main__":
    main()
