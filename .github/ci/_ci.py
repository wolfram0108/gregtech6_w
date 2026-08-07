"""Общий центр вывода CI-гейтов: одна функция печати на все скрипты .github/ci/.

Печатает и в лог задания, и в сводку прогона GitHub Actions ($GITHUB_STEP_SUMMARY, markdown).
Консоль Windows по умолчанию cp1251 и на эмодзи в сводке падает UnicodeEncodeError — гейт обязан
судить код, а не кодировку терминала, поэтому stdout переводится в UTF-8 с заменой непечатаемого.
"""
from __future__ import annotations

import os
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")


def summary(text: str) -> None:
    """Строка в лог задания + в сводку прогона (если мы внутри GitHub Actions)."""
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as fh:
            fh.write(text + "\n")
    print(text)
