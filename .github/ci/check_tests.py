#!/usr/bin/env python3
"""Гейт регресса тестов по КАЛИБРОВАННОМУ базлайну падений.

Зачем не «просто gradlew test»: 4 из 15 тестов падают ПРЕДСУЩЕСТВУЮЩЕ — тест-раннер поднимает
мод без данных (карт рецептов 0, материалов валидных 0 из 60), это дефект оснастки, а не порта.
Красный гейт на каждом прогоне быстро перестают читать; зелёный гейт с выключенными тестами лжёт.
Поэтому судится МНОЖЕСТВО падений, а не их число:

  * падение вне списка известных            → гейт красный (новый регресс);
  * известное падение                       → гейт зелёный, строка в сводке;
  * известное падение начало проходить      → гейт зелёный + требование сузить список;
  * тестов не найдено вовсе                 → гейт красный (тихо не запустившийся раннер —
                                              ровно тот класс сбоя, из-за которого главный
                                              регресс-гейт проекта месяц не запускался).

Список — `.github/ci/known-failures.txt`, по строке `ИмяКласса#имяМетода`.
Строка `UNCALIBRATED` = список ещё не снят с реального прогона CI: гейт печатает найденное
и НЕ валит сборку. Убрать её и вписать реальные имена после первого зелёного прогона.

Выход: 0 — регресса нет; 1 — новые падения / тесты не запускались.
"""
from __future__ import annotations

import glob
import os
import pathlib
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _ci import summary  # noqa: E402  — общий центр вывода гейтов

RESULTS_GLOB = "build/test-results/test/*.xml"
KNOWN = pathlib.Path(".github/ci/known-failures.txt")
UNCALIBRATED = "UNCALIBRATED"


def read_known() -> tuple[set[str], bool]:
    if not KNOWN.exists():
        return set(), False
    names, uncalibrated = set(), False
    for raw in KNOWN.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        # Комментарий — только ЦЕЛАЯ строка, начинающаяся с '#': внутри имени теста '#' значащий
        # (`Класс#метод`), обрезать по нему нельзя.
        if not line or line.startswith("#"):
            continue
        if line == UNCALIBRATED:
            uncalibrated = True
            continue
        names.add(line)
    return names, uncalibrated


def collect() -> tuple[list[str], int, int]:
    """Вернуть (список падений `Класс#метод`, всего тестов, пропущено)."""
    failures, total, skipped = [], 0, 0
    for path in sorted(glob.glob(RESULTS_GLOB)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for suite in ([root] if root.tag == "testsuite" else root.iter("testsuite")):
            total += int(suite.get("tests", 0))
            skipped += int(suite.get("skipped", 0))
            for case in suite.iter("testcase"):
                if any(child.tag in ("failure", "error") for child in case):
                    failures.append(f"{case.get('classname')}#{case.get('name')}")
    return sorted(set(failures)), total, skipped


def main() -> int:
    known, uncalibrated = read_known()
    failures, total, skipped = collect()

    if total == 0:
        summary("### ❌ Test gate\n")
        summary(
            f"No test results under `{RESULTS_GLOB}`. The runner did not start — that is a failure, "
            "not a pass. Check the Gradle log for a JPMS/module resolution error before the first test."
        )
        return 1

    new = [f for f in failures if f not in known]
    fixed = sorted(known - set(failures))

    summary("### Test gate\n")
    summary("| tests | failed | skipped | known-failing | new failures |")
    summary("|---:|---:|---:|---:|---:|")
    summary(f"| {total} | {len(failures)} | {skipped} | {len(known)} | {len(new)} |")

    if failures:
        summary("")
        summary("<details><summary>Failing tests</summary>\n")
        summary("```")
        for f in failures:
            summary(("KNOWN " if f in known else "NEW   ") + f)
        summary("```")
        summary("</details>")

    if fixed:
        summary("")
        summary("⬇️ Listed as known-failing but passed now — remove from `.github/ci/known-failures.txt`:")
        summary("```")
        for f in fixed:
            summary(f)
        summary("```")

    if uncalibrated:
        summary("")
        summary(
            "⚠️ **Gate is UNCALIBRATED** — `.github/ci/known-failures.txt` still carries the "
            "`UNCALIBRATED` line, so new failures are reported but do not fail the build. "
            "Copy the list above into that file (minus the `UNCALIBRATED` line) to arm the gate."
        )
        return 0

    if new:
        summary("")
        summary("❌ Failures that are not in the known-failure baseline — this is a regression.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
