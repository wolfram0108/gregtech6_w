#!/usr/bin/env python3
"""Гейт чистоты production-jar. Два независимых критерия — jar обязан ПРОЙТИ ОБА.

1. ЗАГРУЖАЕТСЯ ЛИ. compat-mirror (F10) компилирует crutch-классы в пакеты `net.minecraft.*`
   и `cpw.*` — ими в рантайме ВЛАДЕЮТ реальные модули (minecraft / FML). Когда они попадали
   в jar, модульный загрузчик отказывался грузить мод (package-split ResolutionException).
   Исключение стоит в build.gradle (`tasks.named('jar') { exclude … }`), но регресс уже
   случался один раз из-за порядка задач — поэтому проверяем механикой, а не доверием.

   Прочие зеркала (appeng/ic2/buildcraft/…) НЕ запрещены: ими никакой модуль не владеет,
   а рантайм GT6 их shim'ы может звать. Их снятие — работа F10, не этого гейта.

2. ПРИНИМАЕТСЯ ЛИ ПЛОЩАДКОЙ РАЗДАЧИ. Прежде гейт знал только критерий 1 и был зелёным на
   jar, который CurseForge/Modrinth отклоняют ЦЕЛИКОМ: оригинал GT6 хранит рядом с текстурами
   личную оснастку художника (copy_into_*.bat, copythings.bat, overwrite_all.bat — 12 файлов),
   и она уезжала в поставку вместе с ассетами. Мод их не читает, но одного исполняемого
   расширения внутри архива достаточно, чтобы весь релиз стал непубликуемым. Отсекается в
   build.gradle (`sourceSets.main.resources { exclude … }`); здесь — сторож на тот же класс.

Выход: 0 — jar чист по обоим критериям; 1 — найдены запрещённые записи или jar не найден.
"""
from __future__ import annotations

import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _ci import summary  # noqa: E402  — общий центр вывода гейтов

FORBIDDEN_PREFIXES = ("net/minecraft/", "cpw/")

# Расширения, из-за которых площадки раздачи модов отклоняют архив целиком (чёрный список
# CurseForge/Modrinth: исполняемые файлы и командные скрипты любой платформы).
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
        split = [n for n in names if n.startswith(FORBIDDEN_PREFIXES)]
        exe = [n for n in names if n.lower().endswith(FORBIDDEN_SUFFIXES)]
        bad = split + exe
        size_mb = os.path.getsize(jar) / (1024 * 1024)
        mark = "0 ✅" if not bad else f"{len(bad)} ❌"
        summary(f"| `{os.path.basename(jar)}` | {size_mb:.1f} MB | {len(names)} | {mark} |")
        if bad:
            failed = True
        for entries, why in (
            (split, "These packages are owned by the `minecraft` / FML modules at runtime. "
                    "Shipping them causes a package-split resolution failure when the mod loads. "
                    "See the `jar` task exclusions in `build.gradle`."),
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
