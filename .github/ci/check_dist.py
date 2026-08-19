#!/usr/bin/env python3
"""Гейт дистрибутивной чистоты: клиентский тип не должен появиться в классе, который грузится на сервере.

Класс дефекта (BP-BUG-022 на ветке 1.20.1, BUG-084 на main): общий класс мода упоминает тип из
`net.minecraft.client.**` / `com.mojang.blaze3d.**`, выделенный сервер пытается его загрузить и падает
(`RuntimeDistCleaner`: «Attempted to load class … for invalid dist DEDICATED_SERVER»). Дефект не виден
ни в одиночной игре, ни в компиляции — только на дедикейте, поэтому нужен машинный сторож.

ЧТО СУДИТ. Констант-пул СКОМПИЛИРОВАННЫХ классов продукта (`build/classes/java/main`): имена из
CONSTANT_Class и дескрипторы из CONSTANT_NameAndType — то есть ровно те имена, которые JVM разрешает
при линковке и при обращении к члену. Классы, у которых нет исходника в `src/main/java` (оснастка
проверки, подключаемая под `-Pgt6probes`), не судятся: у них своя сторона и свои правила.

ЧЕГО НЕ СУДИТ. (1) Дескрипторы объявлений, которые никто не вызывает (абстрактные методы, поля) — их
JVM не разрешает сама по себе. (2) Разницу «имя разрешается при линковке» и «лениво, при исполнении»:
констант-пул её не хранит, поэтому носитель — это риск, а не доказанное падение; падение судит только
живой прогон дедикейта. (3) Косвенные цепочки через свои же классы.

ВЕРДИКТ. Красный, если появился носитель вне двух поимённых списков в `.github/ci/baseline.json`:
`dist_client` — классы, живущие ТОЛЬКО на клиенте (рендер, модели, экраны: клиентские типы им положены),
`dist_latent` — реестр общих классов, у которых клиентское имя лежит под ленивым разрешением; список
не должен расти. Оба списка — «имя → причина», причина обязательна.

ЗАПУСК: `python3 .github/ci/check_dist.py` из корня репозитория (в CI — шаг Guard build.yml);
`--list` дополнительно печатает весь состав носителей с их клиентскими именами.

Выход: 0 — носителей вне списков нет; 1 — есть (или битый baseline); 2 — судить нечего (классы не
собраны либо сканер ослеп), вердикта НЕТ.
"""
from __future__ import annotations

import json
import os
import pathlib
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _ci import summary  # noqa: E402  — общий центр вывода гейтов

PREFIXES = ("net/minecraft/client/", "com/mojang/blaze3d/", "snownee/jade/overlay/", "snownee/jade/impl/")
CLASSES_ROOT = pathlib.Path("build/classes/java/main")
SRC_ROOT = pathlib.Path("src/main/java")
BASELINE = pathlib.Path(".github/ci/baseline.json")


def parse_pool(data: bytes):
    """(имя класса, имена из CONSTANT_Class, дескрипторы из CONSTANT_NameAndType) или None."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        return None
    count = int.from_bytes(data[8:10], "big")
    pos, utf, klass, nat, i = 10, {}, {}, [], 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            ln = int.from_bytes(data[pos:pos + 2], "big")
            pos += 2
            utf[i] = data[pos:pos + ln].decode("utf-8", "replace")
            pos += ln
        elif tag in (7, 8, 16, 19, 20):
            if tag == 7:
                klass[i] = int.from_bytes(data[pos:pos + 2], "big")
            pos += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            if tag == 12:
                nat.append(int.from_bytes(data[pos + 2:pos + 4], "big"))
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1                       # long/double занимают две ячейки
        elif tag == 15:
            pos += 3
        else:
            return None                  # неизвестный тег — класс не наш, пропускаем
        i += 1
    this_class = int.from_bytes(data[pos + 2:pos + 4], "big")
    names = {utf[v] for v in klass.values() if v in utf}
    descs = {utf[v] for v in nat if v in utf}
    return utf.get(klass.get(this_class, -1), "?"), names, descs


def hits(strings) -> list[str]:
    return sorted({s for s in strings for p in PREFIXES if p in s})


def listed(name: str, table: dict) -> str | None:
    for key in table:
        if name.startswith(key):
            return key
    return None


def main() -> int:
    if not CLASSES_ROOT.is_dir():
        summary(f"### ❌ Dist purity\n\n`{CLASSES_ROOT}` not found — compile the product first.")
        return 2
    try:
        base = json.loads(BASELINE.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        summary(f"### ❌ Dist purity\n\nCannot read `{BASELINE}`: {exc}")
        return 1
    client_ok = base.get("dist_client", {})
    latent_ok = base.get("dist_latent", {})

    files = sorted(CLASSES_ROOT.rglob("*.class"))
    if not files:
        summary(f"### ❌ Dist purity\n\nNo class files under `{CLASSES_ROOT}` — nothing to judge.")
        return 2

    carriers, foreign = [], 0
    for path in files:
        parsed = parse_pool(path.read_bytes())
        if parsed is None:
            continue
        name, names, descs = parsed
        if not (SRC_ROOT / (name.split("$")[0] + ".java")).exists():
            foreign += 1                 # класс оснастки, не продукт
            continue
        found = hits(names) + hits(descs)
        if found:
            carriers.append((name, found))

    client, latent, viol = [], [], []
    for name, found in carriers:
        if listed(name, client_ok):
            client.append(name)
        elif listed(name, latent_ok):
            latent.append(name)
        else:
            viol.append((name, found))

    # ПОЗИТИВНЫЙ КОНТРОЛЬ: списки не пусты, значит носители обязаны находиться. Ноль найденных при
    # непустом реестре означает ослепший сканер (не тот каталог, чужой формат), а не чистое дерево.
    if (client_ok or latent_ok) and not carriers:
        summary("### ❌ Dist purity\n\nScanner found no carriers at all while the registry is not "
                "empty — the scan is blind, verdict withheld.")
        return 2

    if "--list" in sys.argv:                      # локальный разбор: весь состав носителей с именами
        for name, found in carriers:
            mark = "CLIENT" if listed(name, client_ok) else ("LATENT" if listed(name, latent_ok) else "!! UNLISTED")
            print("%-11s %s" % (mark, name))
            for item in found:
                print("            " + item)

    summary("### Dist purity (client types in compiled product classes)\n")
    summary("| group | count |")
    summary("|---|---:|")
    summary(f"| classes scanned | {len(files) - foreign} |")
    summary(f"| client-only carriers (allowed) | {len(client)} |")
    summary(f"| latent carriers (registered) | {len(latent)} |")
    summary(f"| **carriers outside both lists** | **{len(viol)}** |")

    if viol:
        summary("")
        summary(f"<details><summary>Unlisted carriers ({len(viol)})</summary>\n")
        summary("```")
        for name, found in viol:
            summary(name)
            for item in found:
                summary("      " + item)
        summary("```")
        summary("</details>")
        summary("")
        summary("A client type in a class the dedicated server loads breaks the server (RuntimeDistCleaner). "
                "Either use the common equivalent, or — if the type is dictated by an engine contract — add the "
                "class to `dist_client` / `dist_latent` in `.github/ci/baseline.json` **in the same commit**, "
                "with the reason spelled out.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
