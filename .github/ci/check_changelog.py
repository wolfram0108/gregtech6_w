#!/usr/bin/env python3
"""CHANGELOG gate: one release section must follow the approved template.

Pure text parsing: no network, no git, standard library only. Every violation is printed as
`Cn line L: message`; the last output line is `CHANGELOG OK <VER>` or `CHANGELOG FAIL <VER>: k violation(s)`.

Template of a section (heading uses U+2014 with single spaces around it):

    ## [6.0.0-alpha.17] <U+2014> topic of at most eight words

    ### Added / ### Changed / ### Fixed / ### Removed     (this order, each at most once, none empty)
    - One sentence. ([#31](https://github.com/<repo>/issues/31))

    Thanks to [@a](https://github.com/a), [@b](https://github.com/b), and [@c](https://github.com/c) for the reports and contributions.

Rules:
    C1 heading and topic            C6 forbidden content (ticket ids, hedges, code identifiers, quotes)
    C2 sections: set, order, count  C7 markup (emphasis, tables, code blocks, other headings)
    C3 item: one line, shape, size  C8 optional thanks line: last line, exact wording
    C4 issue links: trailing only   C9 any other unrecognized line
    C5 English only (no Cyrillic)

Sections older than the template are skipped (TEMPLATE_SINCE); `--force` checks them anyway.

Usage:
    python3 .github/ci/check_changelog.py [PATH] [--version VER] [--force]
    python3 .github/ci/check_changelog.py --selftest

Exit: 0 - conforms (or legacy and skipped); 1 - violations; 2 - nothing to judge (file or version unreadable).
"""
from __future__ import annotations

import argparse
import os
import pathlib
import re
import sys

# A gate must judge the text, not the terminal encoding (Windows consoles default to cp1251).
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

DEFAULT_REPO = "wolfram0108/gregtech6_w"
# First alpha number written in the template; older sections stay as they were published.
TEMPLATE_SINCE = {"main": 17, "b1201": 13}
SECTIONS = ("Added", "Changed", "Fixed", "Removed")
MAX_TOPIC_WORDS = 8
MAX_ITEM_LEN = 180

ALPHA_RE = re.compile(r"-alpha\.(\d+)$")
SECTION_REF_RE = re.compile(r"^## \[([^\]]*)\]")
HEADER_RE = re.compile(r"^## \[([^\]]*)\] \u2014 (\S.*)$")
HEADING_RE = re.compile(r"^(#{1,6})\s+(.*)$")
NESTED_RE = re.compile(r"^\s+(?:[-*+]|\d+[.)])\s")
FOREIGN_MARKER_RE = re.compile(r"^(?:[*+]|\d+[.)])\s")

# Trailing group of one or more [#N](url) links; the URL itself is judged separately for a precise message.
TAIL_RE = re.compile(r" \((\[#\d+\]\([^)]*\)(?:, \[#\d+\]\([^)]*\))*)\)$")
ISSUE_LINK_RE = re.compile(r"\[#(\d+)\]\(([^)]*)\)")
LINK_ANY_RE = re.compile(r"\[[^\]]*\]\([^)]*\)")

_AUTHOR = r"\[@[A-Za-z0-9][A-Za-z0-9-]*\]\(https://github\.com/[A-Za-z0-9][A-Za-z0-9-]*\)"
THANKS_START_RE = re.compile(r"^thanks\b", re.IGNORECASE)
THANKS_RE = re.compile(
    rf"^Thanks to ({_AUTHOR}|{_AUTHOR} and {_AUTHOR}|{_AUTHOR}(?:, {_AUTHOR})+, and {_AUTHOR})"
    r" for the reports and contributions\.$"
)
AUTHOR_RE = re.compile(r"\[@([^\]]+)\]\(https://github\.com/([^)]+)\)")

CYRILLIC_RE = re.compile(r"[\u0400-\u04ff]")
TICKET_RE = re.compile(r"\b(?:BUG|ADAPT|BP-BUG|BP-ADAPT|MODCOMPAT)-\d+|\bM-\d{3}")
HEDGE_RE = re.compile(r"known issues?|not yet|partially|\bTODO\b|\bWIP\b|work in progress", re.IGNORECASE)
CODE_SPAN_RE = re.compile(r"`([^`\n]+)`")
CAMEL_RE = re.compile(r"[A-Za-z]*[a-z][A-Z][A-Za-z]*")
CALL_RE = re.compile(r"\w+\(\)")


def read_mod_version(props_text):
    for raw in props_text.split("\n"):
        m = re.match(r"^\s*mod_version\s*=\s*(\S+)\s*$", raw)
        if m:
            return m.group(1)
    return None


def skip_threshold(ver, force):
    """Template start (alpha number) when `ver` is a legacy section to skip, else None."""
    if force:
        return None
    m = ALPHA_RE.search(ver)
    if not m:
        return None
    since = TEMPLATE_SINCE["b1201" if "1.20.1" in ver else "main"]
    return since if int(m.group(1)) < since else None


def find_section(lines, ver):
    """(start, end) indices of the section `## [ver]`: up to the next `## [` line or end of file."""
    for start, line in enumerate(lines):
        m = SECTION_REF_RE.match(line)
        if m and m.group(1) == ver:
            for end in range(start + 1, len(lines)):
                if lines[end].startswith("## ["):
                    return start, end
            return start, len(lines)
    return None


def thanks_ok(line):
    m = THANKS_RE.match(line)
    if not m:
        return False
    return all(name == url for name, url in AUTHOR_RE.findall(m.group(1)))


def header_problem(line, ver):
    m = HEADER_RE.match(line)
    if not m:
        return "header must be '## [VER] <U+2014> topic' with single spaces around the dash and a non-empty topic"
    if m.group(1) != ver:
        return f"header version [{m.group(1)}] differs from {ver}"
    words = len(m.group(2).split())
    if words > MAX_TOPIC_WORDS:
        return f"topic has {words} words (max {MAX_TOPIC_WORDS})"
    return None


def scan_line(line):
    """Content rules that hold on any line of the section: C5, C6 and inline C7."""
    found = []
    m = CYRILLIC_RE.search(line)
    if m:
        found.append(("C5", f"Cyrillic character U+{ord(m.group()):04X}"))
    tickets = sorted(set(TICKET_RE.findall(line)))
    if tickets:
        found.append(("C6", "ticket id: " + ", ".join(tickets)))
    hedges = sorted({h.lower() for h in HEDGE_RE.findall(line)})
    if hedges:
        found.append(("C6", "hedge wording: " + ", ".join(hedges)))
    idents = []
    for span in CODE_SPAN_RE.findall(line):
        idents += [m.group() for m in CAMEL_RE.finditer(span)]
        idents += CALL_RE.findall(span)
    if idents:
        found.append(("C6", "code identifier in backticks: " + ", ".join(sorted(set(idents)))))
    if line.lstrip().startswith(">"):
        found.append(("C6", "quoted line"))
    if "\u26a0" in line:
        found.append(("C6", "warning sign"))
    # Code spans may legitimately hold globs such as config/**/x, so emphasis is judged outside them.
    outside_code = CODE_SPAN_RE.sub("", line)
    if "**" in outside_code or "__" in outside_code:
        found.append(("C7", "emphasis markup (** or __)"))
    return found


def check_item(text, repo, report):
    """C3 and C4 for one item; `report(rule, message)` is bound to the item's line by the caller."""
    body = text
    tail = TAIL_RE.search(text)
    if tail:
        body = text[: tail.start()]
        links = ISSUE_LINK_RE.findall(tail.group(1))
        if len(links) > 2:
            report("C4", f"{len(links)} issue links in one item (max 2)")
        url_re = re.compile(rf"https://github\.com/{re.escape(repo)}/(?:issues|pull)/(\d+)")
        for num, url in links:
            m = url_re.fullmatch(url)
            if not m:
                report("C4", f"issue URL must be https://github.com/{repo}/issues/N or /pull/N: {url}")
            elif m.group(1) != num:
                report("C4", f"link text #{num} differs from URL number {m.group(1)}")
    if LINK_ANY_RE.search(body):
        report("C4", "link in the middle of the item (only a trailing issue link is allowed)")
    if not body.strip():
        report("C3", "empty item")
        return
    if not re.match(r"[A-Z0-9]", body):
        report("C3", "item text must start with an uppercase Latin letter or a digit")
    if not body.endswith("."):
        report("C3", "item text must end with a period")
    if len(body) > MAX_ITEM_LEN:
        report("C3", f"item text is {len(body)} characters (max {MAX_ITEM_LEN})")


def check_section(sec, first, ver, repo):
    """Violations (rule, file line, message) of one section; `sec[0]` is the heading at line `first`."""
    out = []

    def add(rule, idx, msg):
        out.append((rule, first + idx, msg))

    def close(cur):
        if cur is not None and cur[2] == 0:
            add("C2", cur[1], f"section '{cur[0]}' has no items")

    lines = [s.rstrip() for s in sec]
    for idx, line in enumerate(lines):
        for rule, msg in scan_line(line):
            add(rule, idx, msg)
    problem = header_problem(lines[0], ver)
    if problem:
        add("C1", 0, problem)

    seen = []
    last_rank = -1
    n_sections = 0
    cur = None  # [name, heading index, item count]
    in_fence = False
    prev = None  # "item" while the previous line was an item or its continuation
    thanks_at = []
    for idx in range(1, len(lines)):
        line = lines[idx]
        stripped = line.lstrip()
        if not stripped:
            prev = None
            continue
        if in_fence:
            if stripped.startswith("```"):
                in_fence = False
            continue
        if stripped.startswith("```"):
            add("C7", idx, "code block")
            in_fence = stripped.count("```") % 2 == 1
            prev = None
            continue
        m = HEADING_RE.match(line)
        if m:
            prev = None
            level, name = len(m.group(1)), m.group(2).strip()
            if level != 3:
                add("C7", idx, f"heading level {level} is not allowed")
                continue
            close(cur)
            n_sections += 1
            cur = [name, idx, 0]
            if name not in SECTIONS:
                add("C2", idx, f"unknown section '{name}' (allowed: {', '.join(SECTIONS)})")
                continue
            rank = SECTIONS.index(name)
            if name in seen:
                add("C2", idx, f"duplicate section '{name}'")
            elif rank < last_rank:
                add("C2", idx, f"section '{name}' is out of order (expected {', '.join(SECTIONS)})")
            else:
                seen.append(name)
            last_rank = max(last_rank, rank)
            continue
        if stripped.startswith("|"):
            add("C7", idx, "table")
            prev = None
            continue
        if stripped.startswith(">"):
            prev = None  # already reported as C6 by scan_line
            continue
        if THANKS_START_RE.match(line):
            thanks_at.append(idx)
            if not thanks_ok(line):
                add("C8", idx, "thanks line does not match the template")
            prev = None
            continue
        if NESTED_RE.match(line):
            add("C3", idx, "nested list or indented item")
            prev = None
            continue
        if FOREIGN_MARKER_RE.match(line):
            add("C3", idx, "item must start with '- '")
            prev = None
            continue
        if line == "-" or line.startswith("- "):
            if cur is None:
                add("C2", idx, "item outside of any ### section")
            else:
                cur[2] += 1
            check_item(line[2:], repo, lambda rule, msg, i=idx: add(rule, i, msg))
            prev = "item"
            continue
        if prev is not None:
            add("C3", idx, "continuation line: an item must be a single line")
            continue
        add("C9", idx, "unrecognized line: " + line.strip()[:60])
        prev = None
    close(cur)
    if n_sections == 0:
        add("C2", 0, "no ### sections")
    last = max((i for i, s in enumerate(lines) if s.strip()), default=0)
    for idx in thanks_at:
        if idx != last:
            add("C8", idx, "thanks line must be the last non-blank line of the section")
    return out


def collect(text, ver, repo):
    lines = text.split("\n")
    span = find_section(lines, ver)
    if span is None:
        return [("C1", 0, f"section not found: ## [{ver}]")]
    start, end = span
    return check_section(lines[start:end], start + 1, ver, repo)


def evaluate(text, ver, force, repo):
    """(exit code, output lines) for one version of one changelog text."""
    since = skip_threshold(ver, force)
    if since is not None:
        return 0, [f"CHANGELOG SKIP {ver} (legacy section, template starts at alpha.{since})"]
    violations = sorted(collect(text, ver, repo), key=lambda v: (v[1], v[0]))
    out = [f"{rule} line {line}: {msg}" for rule, line, msg in violations]
    if violations:
        out.append(f"CHANGELOG FAIL {ver}: {len(violations)} violation(s)")
        return 1, out
    out.append(f"CHANGELOG OK {ver}")
    return 0, out


# ---------------------------------------------------------------------------------------------
# Selftest: the gate must show both colors - green on good sections, the exact rule on bad ones.
# ---------------------------------------------------------------------------------------------

_VER = "6.0.0-alpha.17"
_HEAD = f"## [{_VER}] \u2014 Smelting and pipes behave as in 1.7.10"
# Neighbours carry violations of their own; they must never be reported (section boundaries).
_PRELUDE = "\n".join([
    "# Changelog", "", "## [Unreleased]", "",
    "## [6.0.0-alpha.18] \u2014 Not under test", "", "### Added", "- **BUG-1** \u0416 lowercase", "",
])
_AFTER = "\n".join([
    "## [6.0.0-alpha.16] \u2014 Old style", "", "### Fixed", "- **M-179** \u0416 known issue", "",
])


def _link(n, kind="issues", repo=DEFAULT_REPO):
    return f"[#{n}](https://github.com/{repo}/{kind}/{n})"


def _text(n):
    return "A" + "b" * (n - 2) + "."


_T1 = "Thanks to [@user1](https://github.com/user1) for the reports and contributions."
_T2 = "Thanks to [@user1](https://github.com/user1) and [@user2](https://github.com/user2) for the reports and contributions."
_T3 = ("Thanks to [@user1](https://github.com/user1), [@user2](https://github.com/user2), and "
       "[@user3](https://github.com/user3) for the reports and contributions.")
_T4 = ("Thanks to [@u1](https://github.com/u1), [@u2](https://github.com/u2), [@u3](https://github.com/u3), and "
       "[@u4](https://github.com/u4) for the reports and contributions.")
_A = "[@a](https://github.com/a)"
_B = "[@b](https://github.com/b)"
_C = "[@c](https://github.com/c)"
_OK = "- Pipes hold fluid now."

_GOOD = "\n".join([
    _HEAD, "",
    "### Added",
    f"- Players can now craft the shiny machine. ({_link(31)})",
    "- The `config/gregtech/gregtech.cfg` file gains a `machine_speed` key.",
    "",
    "### Changed",
    f"- Ingots of `minecraft:copper_ingot` smelt faster. ({_link(32, 'pull')}, {_link(33)})",
    "- Pipes (bronze and steel) carry more fluid.",
    "",
    "### Fixed",
    f"- Bronze pipes no longer leak and now hold fluid. ({_link(35)})",
    "- Run `/gt6 dump` to list all registered materials.",
    "",
    _T3,
    "",
])


def _body(*lines, head=_HEAD):
    return "\n".join([head, ""] + list(lines)) + "\n"


def _doc(section, tail=True):
    return _PRELUDE + section + ("\n" + _AFTER if tail else "")


def _line_of(doc, needle, nth=1):
    for number, line in enumerate(doc.split("\n"), 1):
        if needle in line:
            nth -= 1
            if nth == 0:
                return number
    raise ValueError(f"needle not in document: {needle!r}")


def _build_cases():
    cases = []  # (name, doc, ver, expected rule ids, [(rule, needle | (needle, nth) | line number)])

    def case(name, section, expect=(), at=(), ver=_VER, tail=True, crlf=False):
        doc = _doc(section if tail else section.rstrip("\n"), tail)
        cases.append((name, doc.replace("\n", "\r\n") if crlf else doc, ver, frozenset(expect), tuple(at)))

    # Good colors: nothing may be reported.
    case("good_full", _GOOD)
    case("good_crlf", _GOOD, crlf=True)
    case("good_eof_without_newline", _GOOD, tail=False)
    case("good_minimal_digit_start", _body("### Removed", "- 3 obsolete recipes are gone."))
    case("good_item_180_chars_plain", _body("### Fixed", f"- {_text(180)}"))
    case("good_item_180_chars_with_link", _body("### Fixed", f"- {_text(180)} ({_link(1)})"))
    case("good_topic_8_words", _body("### Fixed", _OK, head=f"## [{_VER}] \u2014 one two three four five six seven eight"))
    case("good_blank_after_section_heading", _body("### Fixed", "", _OK, ""))
    case("good_thanks_1_author", _body("### Fixed", _OK, "", _T1))
    case("good_thanks_2_authors", _body("### Fixed", _OK, "", _T2))
    case("good_thanks_4_authors", _body("### Fixed", _OK, "", _T4))
    case("good_glob_in_code_not_emphasis", _body("### Fixed", "- The pattern `config/**/*.cfg` is read first."))
    case("good_snake_case_in_code", _body("### Fixed", "- The `machine_speed` key is read."))
    case("good_camel_outside_code", _body("### Fixed", "- The getBlockPathType name is gone from player view."))
    case("good_not_a_ticket", _body("### Fixed", "- The ATM-123 pack loads now."))
    case("good_word_boundaries", _body("### Fixed", "- The wiper clears all todos."))
    case("good_cyrillic_supplement_out_of_range", _body("### Fixed", "- Pipes \u0500 hold fluid now."))

    # C1
    h = f"## [{_VER}]"
    case("c1_hyphen_separator", _body("### Fixed", _OK, head=f"{h} - Topic words"), {"C1"}, [("C1", h)])
    case("c1_dash_without_spaces", _body("### Fixed", _OK, head=f"{h}\u2014Topic words"), {"C1"}, [("C1", h)])
    case("c1_double_space_after_dash", _body("### Fixed", _OK, head=f"{h} \u2014  Topic words"), {"C1"}, [("C1", h)])
    case("c1_empty_topic", _body("### Fixed", _OK, head=f"{h} \u2014"), {"C1"}, [("C1", h)])
    case("c1_topic_9_words", _body("### Fixed", _OK, head=f"{h} \u2014 one two three four five six seven eight nine"),
         {"C1"}, [("C1", h)])
    case("c1_section_not_found", _GOOD, {"C1"}, [("C1", 0)], ver="6.0.0-alpha.99")
    case("c1_version_is_not_a_prefix", _body("### Fixed", _OK, head="## [6.0.0-alpha.170] \u2014 Topic words"), {"C1"}, [("C1", 0)])

    # C2
    case("c2_unknown_section", _body("### Fixed", _OK, "### Security", "- Holes closed."), {"C2"}, [("C2", "### Security")])
    case("c2_lowercase_section_name", _body("### fixed", _OK), {"C2"}, [("C2", "### fixed")])
    case("c2_order", _body("### Fixed", _OK, "### Changed", "- Pipes carry more."), {"C2"}, [("C2", "### Changed")])
    case("c2_duplicate", _body("### Added", _OK, "### Added", "- Pipes carry more."), {"C2"}, [("C2", ("### Added", 3))])  # occurrence 1 is in the prelude
    case("c2_no_sections", _body(), {"C2"}, [("C2", h)])
    case("c2_empty_middle_section", _body("### Changed", "### Fixed", _OK), {"C2"}, [("C2", "### Changed")])
    case("c2_empty_last_section", _body("### Fixed", _OK, "### Removed"), {"C2"}, [("C2", "### Removed")])
    case("c2_item_before_section", _body(_OK, "### Fixed", "- Pipes carry more."), {"C2"}, [("C2", "- Pipes hold fluid now.")])

    # C3
    case("c3_no_period", _body("### Fixed", "- Pipes hold fluid now"), {"C3"}, [("C3", "- Pipes hold fluid now")])
    case("c3_lowercase_start", _body("### Fixed", "- pipes hold fluid now."), {"C3"}, [("C3", "- pipes")])
    case("c3_181_chars", _body("### Fixed", f"- {_text(181)}"), {"C3"}, [("C3", "- Abbb")])
    case("c3_181_chars_before_link", _body("### Fixed", f"- {_text(181)} ({_link(1)})"), {"C3"}, [("C3", "- Abbb")])
    case("c3_nested_item", _body("### Fixed", _OK, "  - Sub item."), {"C3"}, [("C3", "  - Sub item.")])
    case("c3_continuation_indented", _body("### Fixed", _OK, "  and more text."), {"C3"}, [("C3", "and more text.")])
    case("c3_continuation_flush", _body("### Fixed", _OK, "More text here."), {"C3"}, [("C3", "More text here.")])
    case("c3_star_marker", _body("### Fixed", _OK, "* Second item."), {"C3"}, [("C3", "* Second item.")])
    case("c3_numbered_marker", _body("### Fixed", _OK, "1. Second item."), {"C3"}, [("C3", "1. Second item.")])
    case("c3_empty_item", _body("### Fixed", _OK, "-"), {"C3"})

    # C4
    link5 = _link(5)
    case("c4_link_in_the_middle", _body("### Fixed", f"- Pipes ({link5}) hold fluid now."), {"C4"}, [("C4", "- Pipes (")])
    case("c4_period_after_link", _body("### Fixed", f"- Pipes hold fluid now. ({link5})."), {"C4"}, [("C4", "- Pipes hold")])
    case("c4_number_mismatch",
         _body("### Fixed", f"- Pipes hold fluid now. ([#5](https://github.com/{DEFAULT_REPO}/issues/6))"), {"C4"}, [("C4", "- Pipes hold")])
    case("c4_foreign_repo", _body("### Fixed", f"- Pipes hold fluid now. ({_link(5, repo='someone/else')})"), {"C4"}, [("C4", "- Pipes hold")])
    case("c4_wrong_url_kind",
         _body("### Fixed", f"- Pipes hold fluid now. ([#5](https://github.com/{DEFAULT_REPO}/discussions/5))"), {"C4"}, [("C4", "- Pipes hold")])
    case("c4_three_links", _body("### Fixed", f"- Pipes hold fluid now. ({_link(1)}, {_link(2)}, {_link(3)})"), {"C4"}, [("C4", "- Pipes hold")])
    case("c4_foreign_link_in_text", _body("### Fixed", "- See [the docs](https://example.com/x) for pipes."), {"C4"}, [("C4", "- See")])

    # C5
    case("c5_cyrillic_in_item", _body("### Fixed", "- Pipes \u0416 hold fluid now."), {"C5"}, [("C5", "- Pipes")])
    case("c5_cyrillic_in_topic", _body("### Fixed", _OK, head=f"{h} \u2014 Topic \u0416 words"), {"C5"}, [("C5", h)])
    case("c5_last_cyrillic_code_point", _body("### Fixed", "- Pipes \u04ff hold fluid now."), {"C5"}, [("C5", "- Pipes")])

    # C6
    for ticket in ("BUG-12", "ADAPT-3", "BP-BUG-7", "BP-ADAPT-4", "MODCOMPAT-5", "M-179"):
        case(f"c6a_ticket_{ticket}", _body("### Fixed", f"- Ticket {ticket} is closed."), {"C6"}, [("C6", "- Ticket")])
    for phrase in ("known issue", "known issues", "Known Issue", "not yet", "partially", "TODO", "todo", "WIP", "work in progress"):
        case("c6b_hedge_" + phrase.replace(" ", "_"), _body("### Fixed", f"- This is {phrase} for pipes."), {"C6"}, [("C6", "- This is")])
    case("c6c_camel_case", _body("### Fixed", "- The `getBlockPathType` call is gone."), {"C6"}, [("C6", "- The")])
    case("c6c_pascal_case", _body("### Fixed", "- The `BlockPos` class is gone."), {"C6"}, [("C6", "- The")])
    case("c6c_camel_call", _body("### Fixed", "- The `doThing()` call is gone."), {"C6"}, [("C6", "- The")])
    case("c6c_plain_call", _body("### Fixed", "- The `run()` call is gone."), {"C6"}, [("C6", "- The")])
    case("c6d_quote_line", _body("### Fixed", _OK, "> Quoted text."), {"C6"}, [("C6", "> Quoted")])
    case("c6d_warning_sign", _body("### Fixed", "- Pipes hold fluid now \u26a0."), {"C6"}, [("C6", "- Pipes")])

    # C7
    case("c7_bold_stars", _body("### Fixed", "- Pipes **hold** fluid now."), {"C7"}, [("C7", "- Pipes")])
    case("c7_bold_underscores", _body("### Fixed", "- Pipes __hold__ fluid now."), {"C7"}, [("C7", "- Pipes")])
    case("c7_table", _body("### Fixed", _OK, "| a | b |"), {"C7"}, [("C7", "| a | b |")])
    case("c7_code_block_content_is_skipped", _body("### Fixed", _OK, "```", "### NotASection", "plain text", "```"), {"C7"}, [("C7", "```")])
    case("c7_heading_level_4", _body("### Fixed", _OK, "#### Deep"), {"C7"}, [("C7", "#### Deep")])
    case("c7_heading_level_2", _body("### Fixed", _OK, "## Title"), {"C7"}, [("C7", "## Title")])
    case("c7_heading_level_1", _body("### Fixed", _OK, "# Title"), {"C7"}, [("C7", "# Title")])

    # C8
    case("c8_thanks_not_last", _body("### Added", _OK, "", _T1, "", "### Fixed", "- Pipes carry more."), {"C8"}, [("C8", "Thanks to")])
    case("c8_two_thanks_lines", _body("### Fixed", _OK, "", _T1, _T2), {"C8"}, [("C8", _T1)])
    case("c8_thanks_then_dangling_text", _body("### Fixed", _OK, "", _T1, "", "Dangling text."), {"C8", "C9"}, [("C8", "Thanks to"), ("C9", "Dangling")])
    case("c8_three_authors_without_oxford_comma",
         _body("### Fixed", _OK, "", f"Thanks to {_A}, {_B} and {_C} for the reports and contributions."), {"C8"}, [("C8", "Thanks to")])
    case("c8_two_authors_with_comma",
         _body("### Fixed", _OK, "", f"Thanks to {_A}, and {_B} for the reports and contributions."), {"C8"}, [("C8", "Thanks to")])
    case("c8_name_differs_from_url",
         _body("### Fixed", _OK, "", "Thanks to [@a](https://github.com/b) for the reports and contributions."), {"C8"}, [("C8", "Thanks to")])
    case("c8_swapped_tail", _body("### Fixed", _OK, "", f"Thanks to {_A} for the contributions and reports."), {"C8"}, [("C8", "Thanks to")])
    case("c8_lowercase_thanks", _body("### Fixed", _OK, "", f"thanks to {_A} for the reports and contributions."), {"C8"}, [("C8", "thanks to")])
    case("c8_no_final_period", _body("### Fixed", _OK, "", f"Thanks to {_A} for the reports and contributions"), {"C8"}, [("C8", "Thanks to")])

    # C9
    case("c9_paragraph_after_heading", _body("This release is great.", "### Fixed", _OK), {"C9"}, [("C9", "This release")])
    case("c9_dangling_text_after_blank", _body("### Fixed", _OK, "", "Some trailing text."), {"C9"}, [("C9", "Some trailing")])
    case("c9_indented_text_after_blank", _body("### Fixed", _OK, "", "    indented text"), {"C9"}, [("C9", "indented text")])
    case("c9_horizontal_rule", _body("### Fixed", _OK, "", "---"), {"C9"}, [("C9", "---")])
    return cases


_SKIP_CASES = [
    # (version, force, expected threshold or None)
    ("6.0.0-alpha.16", False, 17),
    ("6.0.0-alpha.9", False, 17),
    ("6.0.0-alpha.17", False, None),
    ("6.0.0-alpha.100", False, None),
    ("6.0.0-1.20.1-alpha.12", False, 13),
    ("6.0.0-1.20.1-alpha.13", False, None),
    ("6.0.0-alpha.16", True, None),
    ("6.0.0-1.20.1-alpha.12", True, None),
    ("6.0.0-beta.1", False, None),
    ("6.0.0-rc.2", False, None),
    ("6.0.0", False, None),
    ("6.0.0-1.20.1-beta.1", False, None),
]


def _judge_case(doc, ver, expect, at):
    code, out = evaluate(doc, ver, False, DEFAULT_REPO)
    found = {v[0] for v in collect(doc, ver, DEFAULT_REPO)}
    if found != set(expect):
        return f"expected rules {sorted(expect)}, got {sorted(found)}: {out}"
    for rule, where in at:
        if isinstance(where, int):
            want = where
        elif isinstance(where, tuple):
            want = _line_of(doc, *where)
        else:
            want = _line_of(doc, where)
        if not any(v[0] == rule and v[1] == want for v in collect(doc, ver, DEFAULT_REPO)):
            return f"{rule} not reported at line {want}: {out}"
    if code != (1 if expect else 0):
        return f"exit code {code} for {len(expect)} expected rule(s)"
    if not out[-1].startswith("CHANGELOG FAIL " if expect else "CHANGELOG OK "):
        return f"wrong final line: {out[-1]!r}"
    return None


def _judge_extras():
    """Cases about the pieces around the section rules: version choice, repo override, output lines."""
    extras = []
    for ver, force, want in _SKIP_CASES:
        got = skip_threshold(ver, force)
        extras.append((f"skip_{ver}{'_force' if force else ''}", None if got == want else f"expected {want}, got {got}"))

    good = _doc(_GOOD)
    code, out = evaluate(good, _VER, False, DEFAULT_REPO)
    extras.append(("evaluate_ok_line", None if (code, out) == (0, [f"CHANGELOG OK {_VER}"]) else f"{code} {out}"))

    code, out = evaluate(good, "6.0.0-alpha.16", False, DEFAULT_REPO)
    want = ["CHANGELOG SKIP 6.0.0-alpha.16 (legacy section, template starts at alpha.17)"]
    extras.append(("evaluate_skip_main_line", None if (code, out) == (0, want) else f"{code} {out}"))

    code, out = evaluate(good, "6.0.0-1.20.1-alpha.12", False, DEFAULT_REPO)
    want = ["CHANGELOG SKIP 6.0.0-1.20.1-alpha.12 (legacy section, template starts at alpha.13)"]
    extras.append(("evaluate_skip_b1201_line", None if (code, out) == (0, want) else f"{code} {out}"))

    # Forced check of a legacy section: the old-style neighbour must now fail, proving --force reaches the rules.
    code, out = evaluate(_doc(_GOOD), "6.0.0-alpha.16", True, DEFAULT_REPO)
    extras.append(("evaluate_force_checks_legacy", None if code == 1 and out[-1].startswith("CHANGELOG FAIL 6.0.0-alpha.16: ") else f"{code} {out}"))

    code, out = evaluate(_doc(_body("### Fixed", "- Pipes hold fluid now.", "- pipes hold fluid now.", "- fourth")), _VER, False, DEFAULT_REPO)
    extras.append(("evaluate_prints_every_violation", None if code == 1 and len(out) == 4 and out[-1] == f"CHANGELOG FAIL {_VER}: 3 violation(s)" else f"{code} {out}"))

    # The same text must pass for a fork and fail for the default repo: C4 follows GITHUB_REPOSITORY.
    forked = _doc(_body("### Fixed", f"- Pipes hold fluid now. ({_link(5, repo='fork/gt6')})"))
    ok_fork = evaluate(forked, _VER, False, "fork/gt6")[0] == 0
    bad_default = evaluate(forked, _VER, False, DEFAULT_REPO)[0] == 1
    extras.append(("repo_override", None if ok_fork and bad_default else f"fork ok={ok_fork}, default fails={bad_default}"))

    props = "# comment\nfoo=bar\nmod_version=6.0.0-alpha.16\nmod_id=gregtech\n"
    extras.append(("read_mod_version", None if read_mod_version(props) == "6.0.0-alpha.16" else read_mod_version(props)))
    extras.append(("read_mod_version_missing", None if read_mod_version("foo=bar\n") is None else "found a version"))
    return extras


def selftest():
    results = []
    covered = set()
    for name, doc, ver, expect, at in _build_cases():
        covered |= expect
        try:
            results.append((name, _judge_case(doc, ver, expect, at)))
        except Exception as exc:  # noqa: BLE001 -- an instrument defect must be visible, not swallowed
            results.append((name, f"instrument raised {type(exc).__name__}: {exc}"))
    results.extend(_judge_extras())
    missing = sorted({f"C{i}" for i in range(1, 10)} - covered)
    results.append(("coverage_C1_to_C9", None if not missing else f"no bad case for {missing}"))

    for name, why in results:
        print(f"selftest {name}: ok" if why is None else f"selftest {name}: FAIL: {why}")
    failed = [name for name, why in results if why is not None]
    if failed:
        print(f"SELFTEST FAIL ({len(failed)} of {len(results)} cases)")
        return 1
    print(f"SELFTEST OK ({len(results)} cases)")
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description="Check one CHANGELOG section against the template.")
    parser.add_argument("path", nargs="?", help="changelog file (default: CHANGELOG.md in the repository root)")
    parser.add_argument("--version", help="section to check (default: mod_version from gradle.properties)")
    parser.add_argument("--force", action="store_true", help="check legacy sections too")
    parser.add_argument("--selftest", action="store_true", help="run the built-in good/bad samples and exit")
    args = parser.parse_args(argv)
    if args.selftest:
        return selftest()

    root = pathlib.Path(__file__).resolve().parents[2]
    path = pathlib.Path(args.path) if args.path else root / "CHANGELOG.md"
    ver = args.version
    try:
        if not ver:
            ver = read_mod_version((root / "gradle.properties").read_text(encoding="utf-8"))
        text = path.read_text(encoding="utf-8-sig")
    except (OSError, UnicodeDecodeError) as exc:
        print(f"CHANGELOG ERROR: cannot read input: {exc}")
        return 2
    if not ver:
        print(f"CHANGELOG ERROR: mod_version not found in {root / 'gradle.properties'}")
        return 2

    repo = os.environ.get("GITHUB_REPOSITORY") or DEFAULT_REPO
    code, out = evaluate(text, ver, args.force, repo)
    print("\n".join(out))
    return code


if __name__ == "__main__":
    sys.exit(main())
