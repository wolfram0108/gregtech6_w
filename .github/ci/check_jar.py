#!/usr/bin/env python3
"""Гейт чистоты production-jar.

Зачем: compat-mirror (F10) компилирует crutch-классы в пакеты `net.minecraft.*` и `cpw.*` —
ими в рантайме ВЛАДЕЮТ реальные модули (minecraft / FML). Когда они попадали в jar,
модульный загрузчик отказывался грузить мод (package-split ResolutionException).
Исключение стоит в build.gradle (`tasks.named('jar') { exclude … }`), но регресс уже случался
один раз из-за порядка задач — поэтому проверяем механикой, а не доверием.

Прочие зеркала (appeng/ic2/buildcraft/…) НЕ запрещены: ими никакой модуль не владеет,
а рантайм GT6 их shim'ы может звать. Их снятие — работа F10, не этого гейта.

Выход: 0 — jar чист; 1 — найдены запрещённые записи или jar не найден.
"""
from __future__ import annotations

import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _ci import summary  # noqa: E402  — общий центр вывода гейтов

FORBIDDEN_PREFIXES = ("net/minecraft/", "cpw/")


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
        bad = [n for n in names if n.startswith(FORBIDDEN_PREFIXES)]
        size_mb = os.path.getsize(jar) / (1024 * 1024)
        mark = "0 ✅" if not bad else f"{len(bad)} ❌"
        summary(f"| `{os.path.basename(jar)}` | {size_mb:.1f} MB | {len(names)} | {mark} |")
        if bad:
            failed = True
            summary("")
            summary(f"Forbidden entries in `{os.path.basename(jar)}` (first 20):")
            summary("```")
            for n in bad[:20]:
                summary(n)
            if len(bad) > 20:
                summary(f"… and {len(bad) - 20} more")
            summary("```")
            summary(
                "These packages are owned by the `minecraft` / FML modules at runtime. "
                "Shipping them causes a package-split resolution failure when the mod loads. "
                "See the `jar` task exclusions in `build.gradle`."
            )

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
