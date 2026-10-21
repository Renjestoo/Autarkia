"""Print entry names (and optionally contents) of a jar under a path prefix.

Usage: jar_cat.py JAR PREFIX [--show]
"""

import sys
import zipfile


def main():
    jar, prefix = sys.argv[1], sys.argv[2]
    show = "--show" in sys.argv
    with zipfile.ZipFile(jar) as z:
        names = [n for n in z.namelist() if n.startswith(prefix) and not n.endswith("/")]
        print(f"{len(names)} entries under {prefix}")
        for n in names:
            print("==", n)
            if show:
                print(z.read(n).decode("utf-8", "replace")[:1500])


if __name__ == "__main__":
    main()
