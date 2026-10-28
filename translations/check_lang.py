#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Checks a GregTech 6 translation file against its template. Python 3, standard library only.

    python check_lang.py ja_jp.json --template gregtech6.en.json
    python check_lang.py ja_jp.json --template gt6u.en.json --core-template gregtech6.en.json --missing ja_jp.missing.json
    python check_lang.py --selftest

What it proves: the file is valid, how much of the template is translated, and that nothing the game needs inside a text was lost.
What it does NOT prove: that the translation reads well, or that the game shows it. That is checked by selecting the language in the game.

ERRORS (exit code 1) are what breaks the game's text or the file: invalid JSON, a key given twice, a value that is not a string,
a book page of 256 characters or more (the game drops such a page).
WARNINGS are what usually is a mistake: a key of the main template in the addon's file, a changed «§» colour code, «%s» placeholder, «¶» line break or edge space; Windows line endings.
"""
import collections
import io
import json
import os
import re
import sys
import tempfile

FORMAT = re.compile(r"%(?:\d+\$)?[sdfxXc]|%%")  # a percent sign in a sentence («40% of») is not a placeholder
COLOUR = re.compile(r"§[0-9a-fk-orA-FK-OR]")
BOOK_PAGE = re.compile(r"(?:^|\.)written\.book\..+\.page\.\d+$")  # also «gt6u.rewrite.written.book...»
BOOK_PAGE_LIMIT = 256  # UT.Books.createWrittenBook drops a page of this many characters or more


class Report(object):
    def __init__(self):
        self.errors, self.warnings = [], []
        self.keys = self.template = self.translated = self.empty = self.same = self.unknown = 0
        self.missing = {}


def load_object(text):
    """JSON object with the keys given twice listed, since json.loads keeps only the last one."""
    twice = []
    def pairs(items):
        seen = {}
        for k, v in items:
            if k in seen: twice.append(k)
            seen[k] = v
        return seen
    return json.loads(text, object_pairs_hook=pairs), twice


def tokens(text):
    return collections.Counter(FORMAT.findall(text) + [c.lower() for c in COLOUR.findall(text)]
                               + ["¶"] * text.count("¶") + ["\\n"] * text.count("\n"))


def check(raw, template, core_keys=None, limit=8):
    """raw: the bytes of the translation file; template: {key: English}; core_keys: keys of the main template, for an addon's file."""
    r = Report()
    r.template = len(template)
    try:
        text = raw.decode("utf-8-sig")
    except UnicodeDecodeError as e:
        r.errors.append("the file is not UTF-8: %s" % e); return r
    if raw.startswith(b"\xef\xbb\xbf"): r.warnings.append("the file starts with a byte-order mark (BOM); save it as UTF-8 without it")
    if b"\r\n" in raw: r.warnings.append("the file has Windows line endings (CRLF); the project keeps LF")
    try:
        data, twice = load_object(text)
    except ValueError as e:
        r.errors.append("invalid JSON: %s" % e); return r
    if not isinstance(data, dict):
        r.errors.append("the file must be one JSON object {\"key\": \"text\", ...}"); return r
    r.keys = len(data)
    if twice: r.errors.append("%d keys are given twice (the last one wins, the first is lost): %s" % (len(set(twice)), ", ".join(sorted(set(twice))[:limit])))
    for key, value in data.items():
        if not isinstance(value, str): r.errors.append("the value of %r is not a string" % key)
    bad = {k: v for k, v in data.items() if isinstance(v, str)}

    misplaced = [k for k in bad if core_keys is not None and k in core_keys and k not in template]
    if misplaced: r.warnings.append("%d keys belong to the main template; from this file they apply only while the main file of the same language lacks them (the game merges the files of different mods in no fixed order): %s" % (len(misplaced), ", ".join(sorted(misplaced)[:limit])))

    long_pages = [k for k, v in bad.items() if k in template and BOOK_PAGE.search(k) and len(v) >= BOOK_PAGE_LIMIT]
    if long_pages: r.errors.append("%d book pages are %d characters or longer; the game drops such a page: %s" % (len(long_pages), BOOK_PAGE_LIMIT, ", ".join(sorted(long_pages)[:limit])))

    codes, formats, breaks, edges = [], [], [], []
    for key, english in template.items():
        value = bad.get(key)
        if value is None or not value.strip():
            if value is not None: r.empty += 1
            r.missing[key] = english
            continue
        r.translated += 1
        if value == english and re.search("[A-Za-z]{2}", english): r.same += 1
        want, have = tokens(english), tokens(value)
        if want != have:
            lost = want - have
            extra = have - want
            (codes if any(t.startswith("§") for t in list(lost) + list(extra)) else formats if any(t.startswith("%") for t in list(lost) + list(extra)) else breaks).append(key)
        if english[:1].isspace() != value[:1].isspace() or english[-1:].isspace() != value[-1:].isspace(): edges.append(key)
    if codes: r.warnings.append("%d texts have other colour codes («§x») than the English: %s" % (len(codes), ", ".join(codes[:limit])))
    if formats: r.warnings.append("%d texts have other placeholders («%%s», «%%d») than the English: %s" % (len(formats), ", ".join(formats[:limit])))
    if breaks: r.warnings.append("%d texts have other line breaks («¶») than the English: %s" % (len(breaks), ", ".join(breaks[:limit])))
    if edges: r.warnings.append("%d texts start or end with a space where the English does not (or the other way round); the game joins them with other text: %s" % (len(edges), ", ".join(edges[:limit])))
    r.unknown = len([k for k in bad if k not in template])
    return r


def show(r, name, template_name, limit=8):
    pct = 100.0 * r.translated / r.template if r.template else 0.0
    print("%s: %d keys; template %s: %d keys" % (name, r.keys, template_name, r.template))
    print("translated: %d of %d (%.1f %%); empty values: %d; the same as the English: %d (numbers and abbreviations may be right)" % (r.translated, r.template, pct, r.empty, r.same))
    print("missing: %d; keys the template does not have: %d (obsolete or misspelled)" % (len(r.missing), r.unknown))
    for e in r.errors: print("ERROR   " + e)
    for w in r.warnings: print("WARNING " + w)
    print("verdict: " + ("FAILED, %d errors" % len(r.errors) if r.errors else "no errors, %d warnings" % len(r.warnings)))


def selftest():
    """The checker judged on files that are wrong in a known way, and on one that is right: a checker that has never failed proves nothing."""
    template = {
        "a.name": "Copper Ingot", "a.tip": "Heat: %s K", "a.red": "§cDanger", "a.pad": "Produces ", "a.page": "Line one¶Line two",
        "written.book.X.page.1": "Short page", "b.same": "LP", "a.percent": "Hits for 40% of it", "gt6u.rewrite.written.book.Y.page.0": "A page",
    }
    good = {"a.name": "Slitek", "a.tip": "Hitze: %s K", "a.red": "§cGefahr", "a.pad": "Erzeugt ", "a.page": "Zeile¶Zeile", "written.book.X.page.1": "Kurz", "b.same": "LP", "a.percent": "Trifft mit 40% davon", "gt6u.rewrite.written.book.Y.page.0": "Seite"}
    def run(obj=None, raw=None, core=None):
        data = raw if raw is not None else json.dumps(obj, ensure_ascii=False).encode("utf-8")
        return check(data, template, core)
    cases = []
    r = run(good); cases.append(("positive control: a right file (a percent sign in a sentence is no placeholder)", not r.errors and not r.warnings and r.translated == 9))
    r = run(raw=b'{"a.name": "x", "a.name": "y"}'); cases.append(("a key given twice", any("given twice" in e for e in r.errors)))
    r = run(raw=b'{"a.name": '); cases.append(("invalid JSON", any("invalid JSON" in e for e in r.errors)))
    r = run({"a.name": 5}); cases.append(("a value that is not a string", any("not a string" in e for e in r.errors)))
    r = run(dict(good, **{"written.book.X.page.1": "x" * 256})); cases.append(("a book page that is too long", any("book pages" in e for e in r.errors)))
    r = run(dict(good, **{"core.only": "x"}), core={"core.only", "a.name"}); cases.append(("a key of the main template in an addon file", any("main template" in w for w in r.warnings) and not r.errors))
    r = run(dict(good, **{"gt6u.rewrite.written.book.Y.page.0": "x" * 256})); cases.append(("a long page under a rewrite key", any("book pages" in e for e in r.errors)))
    r = run(dict(good, **{"a.red": "Gefahr"})); cases.append(("a lost colour code", any("colour" in w for w in r.warnings)))
    r = run(dict(good, **{"a.tip": "Hitze: K"})); cases.append(("a lost placeholder", any("placeholders" in w for w in r.warnings)))
    r = run(dict(good, **{"a.page": "Zeile Zeile"})); cases.append(("a lost line break", any("line breaks" in w for w in r.warnings)))
    r = run(dict(good, **{"a.pad": "Erzeugt"})); cases.append(("a lost trailing space", any("space" in w for w in r.warnings)))
    r = run(raw=json.dumps(good).encode("utf-8").replace(b", ", b",\r\n")); cases.append(("Windows line endings", any("CRLF" in w for w in r.warnings)))
    r = run({"a.name": "Slitek"}); cases.append(("missing keys are counted and listed", len(r.missing) == 8 and r.translated == 1))
    r = run(dict(good, **{"a.name": ""})); cases.append(("an empty value is untranslated", r.empty == 1 and "a.name" in r.missing))
    failed = [name for name, ok in cases if not ok]
    for name, ok in cases: print(("ok      " if ok else "FAILED  ") + name)
    print("selftest: %d of %d" % (len(cases) - len(failed), len(cases)))
    return 1 if failed else 0


def main(argv):
    if "--selftest" in argv: return selftest()
    def option(name):
        return argv[argv.index(name) + 1] if name in argv else None
    paths = [a for i, a in enumerate(argv) if not a.startswith("--") and (i == 0 or not argv[i - 1].startswith("--"))]
    template_path = option("--template")
    if len(paths) != 1 or not template_path:
        print(__doc__); return 2
    def read_template(path):
        with io.open(path, encoding="utf-8") as f: return json.load(f)
    template = read_template(template_path)
    core = set(read_template(option("--core-template"))) if option("--core-template") else None
    with io.open(paths[0], "rb") as f: raw = f.read()
    r = check(raw, template, core)
    show(r, os.path.basename(paths[0]), os.path.basename(template_path))
    if option("--missing") and r.missing:
        with io.open(option("--missing"), "w", encoding="utf-8", newline="\n") as f: f.write(json.dumps(r.missing, ensure_ascii=False, indent=1, sort_keys=True))
        print("the %d missing keys, with their English, are in %s" % (len(r.missing), option("--missing")))
    return 1 if r.errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
