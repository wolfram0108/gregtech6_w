# Contributing

This is a **port**, not a mod of its own. That single fact decides almost every rule below: the goal
is that players get GregTech 6 as Gregorius Techneticies wrote it, running on a modern engine. A
change that makes the code nicer but the behaviour different is a regression here, even when it looks
like an improvement.

Please read [how the port was executed](README.md#how-the-port-was-executed) before your first
change — it explains why the code looks the way it does.

## The two rules that matter most

**1. Transcribe, do not redesign.** Change only what the compiler or the engine physically rejects,
and change as little as possible. Control flow, conditions, ordering, structure and names stay as
they are — including the parts that look suboptimal. In a system this interconnected, an "obvious
improvement" is usually a load-order assumption held by something three modules away.

**2. Adapt in one place, never file by file.** Where the modern engine forces a change, the
replacement belongs where the mod already keeps that conversation — one adapter used by the whole
mod — not a local patch in every file that broke. If you find yourself writing the same workaround
twice, the second one is a bug.

A corollary worth stating: **where the original has one thing, the port has one thing.** Duplicating
a mechanism is treated as a defect, not as a shortcut.

## Before you open a pull request

1. **It builds:** `./gradlew build`
2. **The tests pass:** `./gradlew test` — 13 tests, no failures, no skips. If your change makes a
   test fail, the fix is the code or the test's premise, never deleting the test.
3. **If you touched data generation** — materials, prefixes, recipes, ore dictionary, worldgen — say
   so in the pull request. There is a comparison against the original for exactly this
   (`./gradlew test -Pgt6.oracle=<path>`); it needs dumps that are not in the repository, so a
   maintainer runs it. Flagging the change is enough.
4. **Nothing new is deferred silently:** CI counts unfinished-work markers in the source and fails if
   they grow.

## Claims and evidence

Any statement that something works is expected to come with the output that shows it — a test run, a
comparison against the original, a log from the game. "Should be fine" and "looks correct" are not
accepted for behaviour claims, because this project has repeatedly been wrong that way: reading the
1.7.10 source produced confident and incorrect answers often enough that the original is now treated
as a program to run and measure, not as documentation to read.

If you cannot prove a change, say plainly what is unverified. That is respected; an overstated claim
is not.

## Reporting behaviour differences

If you are not sending code but noticed the port behaving differently from GregTech 6 on 1.7.10, that
is one of the most valuable reports this project can get — open an issue with the bug template.

## Style

- **Code comments and internal documentation are written in Russian**, because that is the working
  language of the port. Keep to it when editing existing code so a file does not become bilingual.
- **The project's showcase is written in English**: README, changelog, issue and pull request text,
  release notes.
- **Commit messages are written in Russian**, like the rest of the history. The log is a record of
  how the port was reasoned about, and it is kept in one language rather than split in half.
- Match the surrounding code: same naming, same formatting, same comment density. The codebase has a
  strong existing idiom inherited from upstream; follow it rather than your own.
- Do not reformat code you are not changing. Whitespace-only diffs hide real changes.

## Commits and pull requests

- One logical change per pull request. A fix plus an unrelated cleanup is two pull requests.
- Explain **what was broken and how you know it is fixed**, not just what you edited.
- Reference the issue if there is one.
- Do not commit build output, logs, dumps, IDE files or anything else the build can regenerate.

## Licensing of contributions

GregTech 6 is licensed under the **GNU Lesser General Public License v3 or later**, and this port
inherits it. By contributing you agree that your contribution is licensed under the same terms.
Do not paste code from sources whose licence forbids it — including decompiled Minecraft code, which
must never enter this repository.
