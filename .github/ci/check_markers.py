#!/usr/bin/env python3
"""Гейт отложенности: счётчики меток PORT-TODO и SILENT-STUB не должны расти.

PORT-TODO — единица работы, отложенная при порте (привязана к шву F#).
SILENT-STUB — заглушка, которая МОЛЧА роняет данные (худший сорт: судья зелёный, данных нет).

Правило проекта: отложенность держится видимой и УБЫВАЮЩЕЙ. Порог — `.github/ci/baseline.json`.
Счётчик ниже порога — гейт зелёный и предлагает опустить порог (порог не правится автоматически).

Считается только `src/main/java` — единственный код продукта в репозитории. Оснастка проверки
(стенды, машинерия сверки) живёт вне репозитория и под счёт не попадает вовсе.

Выход: 0 — счётчики в пределах порога; 1 — превышение или битый baseline.
"""
from __future__ import annotations

import json
import os
import pathlib
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _ci import summary  # noqa: E402  — общий центр вывода гейтов

MARKERS = ("PORT-TODO", "SILENT-STUB")
SCAN_ROOT = pathlib.Path("src/main/java")
BASELINE = pathlib.Path(".github/ci/baseline.json")


def count_markers() -> dict[str, list[str]]:
    hits: dict[str, list[str]] = {m: [] for m in MARKERS}
    for path in SCAN_ROOT.rglob("*.java"):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        for lineno, line in enumerate(text.splitlines(), 1):
            for marker in MARKERS:
                if marker in line:
                    hits[marker].append(f"{path.as_posix()}:{lineno}: {line.strip()[:160]}")
    return hits


def main() -> int:
    if not SCAN_ROOT.is_dir():
        summary(f"### ❌ Deferral counters\n\n`{SCAN_ROOT}` not found — wrong working directory?")
        return 1
    try:
        limits = json.loads(BASELINE.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        summary(f"### ❌ Deferral counters\n\nCannot read `{BASELINE}`: {exc}")
        return 1

    hits = count_markers()
    failed = False

    summary("### Deferral counters (`src/main/java`)\n")
    summary("| marker | found | allowed | verdict |")
    summary("|---|---:|---:|---|")
    for marker in MARKERS:
        found = len(hits[marker])
        allowed = int(limits.get(marker, 0))
        if found > allowed:
            verdict, failed = "❌ grew", True
        elif found < allowed:
            verdict = "⬇️ shrank — tighten the baseline"
        else:
            verdict = "✅"
        summary(f"| `{marker}` | {found} | {allowed} | {verdict} |")

    for marker in MARKERS:
        found = len(hits[marker])
        if found > int(limits.get(marker, 0)):
            summary("")
            summary(f"<details><summary>{marker} sites ({found})</summary>\n")
            summary("```")
            for line in hits[marker][:100]:
                summary(line)
            if found > 100:
                summary(f"… and {found - 100} more")
            summary("```")
            summary("</details>")

    if failed:
        summary("")
        summary(
            "Deferred work must shrink, not grow. Either close the marker, or raise the limit in "
            "`.github/ci/baseline.json` **in the same commit** with a reason in the commit message."
        )
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
