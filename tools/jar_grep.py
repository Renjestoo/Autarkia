"""Search every jar (and nested jar) in a folder for a byte string; print jar and entry names that contain it.

Usage: jar_grep.py FOLDER TEXT
"""

import io
import sys
import zipfile
from pathlib import Path

from tqdm import tqdm


def scan(zf, label, needle, hits):
    for name in zf.namelist():
        data = zf.read(name)
        if name.endswith(".jar"):
            try:
                scan(zipfile.ZipFile(io.BytesIO(data)), f"{label} > {name}", needle, hits)
            except zipfile.BadZipFile:
                pass
        elif needle in data:
            hits.append(f"{label} :: {name}")


def main():
    folder, text = Path(sys.argv[1]), sys.argv[2].encode("utf-8")
    hits = []
    for jar in tqdm(sorted(folder.glob("*.jar")), desc="jars", mininterval=2):
        try:
            scan(zipfile.ZipFile(jar), jar.name, text, hits)
        except zipfile.BadZipFile:
            print("bad zip", jar)
    print(f"{len(hits)} hits")
    for h in hits[:50]:
        print(" ", h)


if __name__ == "__main__":
    main()
