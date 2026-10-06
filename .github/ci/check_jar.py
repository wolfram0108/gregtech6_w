#!/usr/bin/env python3
"""Production-jar purity gate. Two independent criteria; the jar must PASS BOTH.

1. DOES IT LOAD NEXT TO OTHER MODS. The mod loader turns every jar into a module and derives the module's
   packages from its .class entries; two modules holding one package make module resolution fail and the
   game does not start. GT6 compiles compile-only mirrors of foreign APIs (compat-mirror), so the jar may
   hold classes only under the roots GT6 owns: gregapi, gregtech, gregtech6 and gt6mirror (the mirrors,
   moved out of the foreign namespaces they imitate). Any other class root is a package some real mod or
   the engine can own, and is refused here before it reaches a player.

2. IS IT ACCEPTED BY DISTRIBUTION SITES. The original GT6 keeps the artist's personal tooling
   (copy_into_*.bat, copythings.bat, overwrite_all.bat) next to the textures; one executable extension
   inside the archive is enough for CurseForge/Modrinth to reject the whole release. Cut in build.gradle
   (`sourceSets.main.resources { exclude ... }`); guarded here for the same class.

Exit: 0 - the jar is clean by both criteria; 1 - forbidden entries found or no jar matched.
"""
from __future__ import annotations

import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _ci import summary  # noqa: E402 -- single shared sink for all gate output

# Class roots GT6 owns; a class anywhere else adds a package another module may also hold.
OWNED_CLASS_ROOTS = ("gregapi/", "gregtech/", "gregtech6/", "gt6mirror/")

# Extensions that make mod-hosting platforms reject the whole archive outright.
# CurseForge/Modrinth blacklist: executables and shell or batch scripts for any platform.
FORBIDDEN_SUFFIXES = (
    ".bat", ".cmd", ".com", ".exe", ".msi", ".scr",
    ".vbs", ".ps1", ".sh", ".bin", ".dll", ".so",
)


def main() -> int:
    pattern = sys.argv[1] if len(sys.argv) > 1 else "build/libs/*.jar"
    jars = [j for j in sorted(glob.glob(pattern)) if not j.endswith(("-sources.jar", "-javadoc.jar"))]
    if not jars:
        summary(f"### ❌ Jar purity\n\nNo jar matched `{pattern}`.")
        return 1

    failed = False
    summary("### Jar purity guard\n")
    summary("| jar | size | entries | forbidden |")
    summary("|---|---:|---:|---:|")

    for jar in jars:
        with zipfile.ZipFile(jar) as zf:
            names = zf.namelist()
        split = [n for n in names if n.endswith(".class") and not n.startswith(OWNED_CLASS_ROOTS)]
        exe = [n for n in names if n.lower().endswith(FORBIDDEN_SUFFIXES)]
        bad = split + exe
        size_mb = os.path.getsize(jar) / (1024 * 1024)
        mark = "0 ✅" if not bad else f"{len(bad)} ❌"
        summary(f"| `{os.path.basename(jar)}` | {size_mb:.1f} MB | {len(names)} | {mark} |")
        if bad:
            failed = True
        for entries, why in (
            (split, "Classes outside the roots GT6 owns put foreign packages into the GT6 module; the engine "
                    "or any mod holding the same package then fails module resolution and the game does not start. "
                    "Mirrors of foreign APIs belong under `gt6mirror/`."),
            (exe, "Executable/script extensions are blacklisted by mod distribution platforms, "
                  "which reject the whole archive — the jar cannot be published at all. "
                  "See the resource exclusions in `build.gradle`."),
        ):
            if not entries:
                continue
            summary("")
            summary(f"Forbidden entries in `{os.path.basename(jar)}` (first 20):")
            summary("```")
            for n in entries[:20]:
                summary(n)
            if len(entries) > 20:
                summary(f"… and {len(entries) - 20} more")
            summary("```")
            summary(why)

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
