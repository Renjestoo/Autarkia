"""Dump the Render and Server thread stacks of running JVMs (jps + jstack from the JDK 25)."""

import subprocess

JDK = r"F:\jdks\jdk-25\bin"


def main():
    out = subprocess.run(["tasklist", "/FI", "IMAGENAME eq java.exe", "/FO", "CSV", "/NH"], capture_output=True, text=True).stdout
    print("phase: java processes")
    print(out)
    pids = [l.split('","')[1] for l in out.strip().splitlines() if l.startswith('"java.exe"')]
    if not pids:
        raise SystemExit("no java.exe process found")
    for pid in pids:
        main_class = ""
        dump = subprocess.run([JDK + r"\jstack.exe", "-J-Djava.io.tmpdir=F:/tmp/autarkia", pid], capture_output=True, text=True).stdout
        for b in dump.split("\n\n"):
            if '"Render thread"' in b or '"Server thread"' in b:
                print(f"--- pid {pid} {main_class}")
                print(b[:5000])


if __name__ == "__main__":
    main()
