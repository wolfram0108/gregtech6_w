**English** · [Русский](README.ru.md)

# GregTech 6 for NeoForge

[![CI](https://github.com/wolfram0108/gregtech6_w/actions/workflows/build.yml/badge.svg)](https://github.com/wolfram0108/gregtech6_w/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/wolfram0108/gregtech6_w?include_prereleases&sort=semver)](https://github.com/wolfram0108/gregtech6_w/releases)
[![License](https://img.shields.io/badge/license-LGPL--3.0--or--later-blue)](LICENSE)

**A port of [GregTech 6](https://github.com/GregTech6/gregtech6) — Gregorius Techneticies' mod for
Minecraft 1.7.10 — to Minecraft 26.1.2 / NeoForge.**

| | |
|---|---|
| Minecraft | `26.1.2` |
| NeoForge | `26.1.2.77` |
| Java | 25 |
| Upstream | GregTech 6 `v6.17.06` (Minecraft 1.7.10, Forge 10.13.4) |
| License | LGPL-3.0-or-later, inherited from upstream |

> ### ⚠️ Alpha
>
> The mod builds, boots on client and dedicated server, generates its world and is playable end to
> end. It is **not** finished. Expect bugs, expect saves to break between builds, and do not use a
> world you care about. What is known to be unfinished is listed in
> [Current state](#current-state) rather than left for you to discover.

---

## Contents

- [What GregTech 6 is](#what-gregtech-6-is)
- [Why porting it is hard](#why-porting-it-is-hard)
- [How the port is done](#how-the-port-is-done)
- [Current state](#current-state)
- [Building](#building)
- [Running](#running)
- [Tests](#tests)
- [Development tooling](#development-tooling)
- [Continuous integration](#continuous-integration)
- [Mod compatibility](#mod-compatibility)
- [Reporting problems](#reporting-problems)
- [License and credits](#license-and-credits)

---

## What GregTech 6 is

GregTech 6 turns Minecraft into an industrial process game. Ore is not "iron ore that smelts into
iron" — it is a material with a chemical composition, a hardness, a melting point, a set of
by-products and a place in a processing chain that runs from a hand-cranked hammer up through
furnaces, crushers, centrifuges, boilers, engines, multiblock reactors and pressurized fluid
logistics. Wood, stone, rubber, alloys and dozens of fluids each have their own chains, and the
vanilla ones are replaced rather than sitting beside GT6's.

The technically unusual part is that almost none of this is written out by hand. GT6 is built on a
**material × prefix matrix**: it knows several hundred materials and several hundred item shapes
("ingot", "dust", "small gear", "double plate"…) and *generates* the resulting items, blocks, ore
dictionary entries, textures, names, recipes and machine behaviour at load time from the rules for
each combination. The mod is, in essence, one large generator plus the data it consumes.

For a port this matters more than any single feature. Porting the generator's **output** would be
pointless — the thing that has to survive the move is the **generator itself**, together with the
exact order in which it runs, because everything else in the mod is derived from it.

Scale, for context: about 200 000 lines of Java in ~1 230 classes, plus roughly 20 000 asset files.

## Why porting it is hard

GregTech 6 was written for Minecraft 1.7.10 (2014). Between that version and 26.1.2 essentially
every engine subsystem the mod depends on was removed or replaced — not deprecated, removed:

| What GT6 uses in 1.7.10 | What happened to it |
|---|---|
| Item metadata (`ItemStack` = item + damage value) | gone; item identity is now item + data components |
| Block metadata (0–15 per position) | gone; a block is a single block state |
| The block `Material` system (stone / wood / metal…) | removed as a concept |
| Immediate-mode rendering (`IIcon`, `Tessellator`, GL calls, TESRs) | removed; everything is baked models now |
| Forge's `OreDictionary` | removed |
| `IWorldGenerator` (imperative world generation) | replaced by data-driven features and biome modifiers |
| The fluid system (`Fluid`, `BlockFluidClassic`) | split apart and rebuilt |
| The networking layer (`SimpleNetworkWrapper`) | replaced |
| Vanilla crafting registration from code | removed; recipes are datapack JSON |
| The coremod / bytecode-transformer entry point | removed |
| GUI handling (`IGuiHandler`, `Container`, `GuiScreen`) | replaced |
| Achievements | replaced by advancements, with different side effects |

A mod that merely *uses* Minecraft can absorb that kind of change subsystem by subsystem. GT6
cannot, for a structural reason: it is a **monolith**. Its defining property — and the reason it
scales to hundreds of materials without collapsing — is that the conversation with the engine
happens in *one* place. One module talks to the world, one to item stacks, one to NBT, one to the
ore dictionary, and the entire mod goes through them, always. A dependency graph of the source shows
272 of its classes in a single strongly connected component: a knot that cannot be cut into
independently portable slices.

So the port has no "start with a small feature and grow" path available. Either the foundation
stands up as a whole, or nothing runs.

## How the port is done

### 1. Port it, do not rewrite it

The mod you play should be GregTech 6, not something inspired by it. The default is **literal
transcription**: only the symbol the compiler or the engine physically rejects gets changed, and it
gets changed as little as possible.

| Allowed | Not allowed |
|---|---|
| replacing an API call that no longer exists | restructuring control flow |
| adapting a signature the engine demands | merging, splitting or reordering methods |
| a deliberately designed replacement for a removed subsystem | "improving" a design while passing through it |
| recording an unavoidable deviation as a decision | inventing behaviour or constants not in the source |

This includes reproducing choices that look suboptimal. In a system this interconnected, an
"obvious improvement" is usually a load-order assumption held by something three modules away, and
the port is not the place to find that out.

### 2. Adapt centrally, never file by file

Where the engine forces a change, the change is designed **once** and placed where GT6 already keeps
that conversation — then the whole mod goes through it, exactly as it did before.

The alternative is what usually happens to large ports: each file gets patched where it broke, the
patches drift apart, and the mod's central abstractions quietly turn into a thousand local
translations that nobody can keep consistent. Concretely: block position metadata no longer exists,
but GT6's world-access module keeps its original 1.7.10 method signatures and translates internally
— so the ~1 200 call sites across the mod remain byte-for-byte what Gregorius wrote, and the entire
adaptation is one file you can read in an afternoon.

Rule of thumb throughout: **where the original has one thing, the port has one thing.**

### 3. The process

```mermaid
graph TD
  A["Copy the source verbatim<br/>(all files, no edits)"] --> B["Compile<br/>≈19 000 distinct errors —<br/>a precise map of what the engine removed"]
  B --> C["Identify the seams<br/>where 1:1 is physically impossible<br/>and design one central replacement for each"]
  C --> D["Close the errors, cheapest first:<br/>mechanical renames → local fixes →<br/>extending a central seam"]
  D --> E["Data correctness:<br/>does the generator still produce<br/>what the original produced?"]
  E --> F["Runtime correctness:<br/>does it still behave the same<br/>in a running game?"]
```

The unusual step is the first one. Copying 200 000 lines onto an engine that cannot compile them
sounds like a strange way to start, but the resulting error list is the most honest inventory of the
work that exists: every place the two engines disagree, enumerated by a compiler rather than by
someone's judgement about what probably needs attention. The seams in step 3 were chosen from that
inventory and from the dependency graph — not from a feature list.

### 4. The engine seams

Eighteen incompatibilities were found where the original construct simply has no counterpart. Each
one is a single, central replacement rather than a family of local workarounds:

| Removed in the modern engine | Replacement |
|---|---|
| item metadata | one item per shape + a material data component |
| coremod bytecode transformation | mixins over the same methods the original patched, plus access transformers |
| immediate-mode rendering | baked models driven by GT6's own texture layer; connection-dependent shapes via model data |
| Forge's ore dictionary | GT6's own registry (it already maintained a richer layer above Forge's) |
| the fluid system | GT6 fluid data kept as is; world fluids re-parented onto the engine's liquid block so vanilla interactions (freezing, sponges, buckets, lava meeting water) work again, while GT6's own finite-quantum flow is preserved |
| imperative world generation | the original vein/layer algorithms wrapped as engine features placed by biome modifiers |
| the networking layer | the same packets over the modern payload API |
| raw NBT as the data channel | a bridge that keeps the original keys and semantics, including its quirks |
| the block material system | the 1.7.10 system copied verbatim, plus one bridge into modern block properties |
| `@Optional` cross-mod compatibility | GT6's mod-presence registry retained; foreign APIs mirrored at compile time only |
| code-registered crafting recipes | procedural generation into GT6's own recipe buffer, exposed to the engine through a single dispatcher |
| the FML mod lifecycle | the mod's own lifecycle center untouched; only the outermost entry points are modern |
| block position metadata | central world-access module keeps the old signatures; own blocks carry extended metadata |
| the GUI stack | one menu provider, one menu type, original routing preserved |
| `null` as "empty item" | GT6 keeps its own model internally; conversion happens at an enumerated set of engine boundaries |
| mandatory block codecs | one central stub — GT6 blocks are procedural, never data-driven |
| the NBT read API | read helpers centralized, preserving 1.7.10's wrong-type behaviour |
| achievements | a no-op, because the modern award carries side effects the original never had |

### 5. How correctness is judged

The port cannot be verified by reading it. 200 000 lines of procedurally interlinked code offer
endless opportunity to be confidently wrong, and reading the 1.7.10 source produced confident wrong
answers repeatedly. So the original is not treated as documentation — it is treated as a **reference
implementation you can run**.

The 1.7.10 mod is kept as a live, instrumented build. It is asked what it does; the port is asked
the same question the same way; the answers are compared.

| Level | Question it answers |
|---|---|
| Compiler | does it exist and link |
| Data comparison against the original | does the generator still produce the same materials, items, ore dictionary entries, recipes, worldgen definitions and names |
| In-engine probes | does the real engine path behave the same — what block actually gets placed, what item actually ends up in the slot, what the machine actually consumes and outputs |
| Side-by-side measurement | for behaviour that only exists while running (fluid spreading, machine timing), the same instrument is compiled into *both* versions and the same scenario is run in each |
| Playing the game | everything the above cannot see |

Two things this discipline forces, both learned the hard way:

* **A comparison is only as good as its scope.** Matching data dumps prove the generator's *output*
  matches. They say nothing about whether the result reaches the player — the item can be correct in
  every registry and still be invisible, unobtainable or wrongly rendered. Each level above only
  closes its own question.
* **A check that cannot fail is not a check.** Every comparison is run once in a state where it is
  known to be broken, to confirm it can actually see the difference.

## Current state

**Working and exercised in play**

* Client and dedicated server boot; worlds generate and are playable.
* The material/prefix generator, ore processing chains, ore dictionary and recipe generation.
* World generation: ores, veins, rock layers, trees and plants, fluid springs, dungeons.
* Machines, energy (electric, steam, kinetic), pipes and wires, multiblocks, item and fluid logistics.
* Crafting, both the vanilla workbench and GT6's own crafting behaviour.
* Rendering, creative tabs, JEI integration, tooltips, item and block models, fluid rendering.
* Vanilla replacement behaviour: GT6 water, tool rules, harvest tiers, block hardness.

* Covers — filters, detectors, shutters, texture covers, pumps.

**Known differences from the original**

* Recipes that depend on empty slots in the crafting grid — the modern grid is trimmed before the
  recipe sees it, and the semantics here are unresolved.
* A small number of workbench recipes differ from the original in ore-list composition.

**Not started**

* Integration with other mods of the 1.7.10 era (IC2, Forestry, Railcraft, BuildCraft, Thaumcraft
  and ~45 more). GT6's compatibility code is ported and centralized, but nothing modern is wired up
  to it, so those integrations are inert.

**Alpha warnings**

* Worlds do not reliably survive updates. Some fixes apply only to newly generated chunks — data
  that an earlier build destroyed cannot be reconstructed.
* Dedicated-server play is the newest and least exercised part of the project.

## Building

You need **JDK 25**, about **8 GB of free RAM** and **10 GB of disk**. The first build downloads
NeoForge and decompiles Minecraft, which is what the memory is for (the decompiler inherits the
Gradle JVM heap, set to 6 GB in `gradle.properties`) — and it takes a while. Later builds are fast.

```bash
./gradlew build       # compile, jar, tests
./gradlew assemble    # just the jar -> build/libs/gregtech6-<version>.jar
./gradlew compileJava # just compile
```

## Running

```bash
./gradlew runClient   # development client
./gradlew runServer   # dedicated server (own directory: ./runserver)
./gradlew runData     # regenerate generated data files
```

Client and server deliberately use separate game directories, so both can run at once.

There is one opt-in build flag, `-Pgt6probes`, which adds the in-engine verification probes
(`src/probes/java`) to the build. Without it they are not compiled at all and cannot end up in a
player's jar; with it, `runClient`/`runServer` can run automated in-game checks.

## Tests

```bash
./gradlew test    # 13 tests, no external data required
```

The tests boot the mod inside a headless server and then exercise it: the material and prefix
generator, recipe matching, a machine running a full processing cycle tick by tick, energy transfer,
content registration, and the round trip of items through save and load.

## Development tooling

Two things in this repository are **tools from the porting effort**, not part of the product. They
are off by default, cost nothing when unused, and exist because a port occasionally has to ask
questions an ordinary mod never does.

| Tool | What it answers | How to run |
|---|---|---|
| **Comparison against the original** | "does the mod still generate exactly what GregTech 6 on 1.7.10 generated?" — materials, items, ore dictionary, recipes, worldgen, names | `./gradlew test -Pgt6.oracle=<path>` |
| **In-engine probes** | "what does the real engine path actually do here?" — drives real interactions and judges object identity, not pixels | `./gradlew runClient -Pgt6probes` |

The comparison needs dumps taken from a running 1.7.10 installation — roughly 200 MB, produced by a
separate dumper, and deliberately **not** part of this repository. Without the path, those checks
simply do not run: the port phase is finished, so the question is asked while investigating a
difference, not on every build.

## Continuous integration

Every push and pull request runs three jobs:

| Job | Checks |
|---|---|
| **Build** | produces the mod jar, then verifies the jar contains no compile-only stand-in classes for packages the engine owns — shipping those makes the mod fail to load, and it happened once — and that unfinished-work markers left in the source are not accumulating |
| **Stands** | compiles the in-engine probes, which the normal build does not touch and would otherwise silently rot |
| **Tests** | runs the test suite and compares the set of failures against a recorded baseline: an unknown failure fails the build, and a suite that did not run at all also fails (a test runner that quietly starts nothing must not read as success) |

A nightly workflow runs the slower checks: the full data comparison against the original, a check
that generated data files still match their generators, and a dedicated-server boot.

Tagging a release (`v<version>`) builds the jar, verifies the tag matches the version in
`gradle.properties`, and publishes a GitHub release — pre-release for alpha, beta and rc versions.

## Mod compatibility

| Mod | Status |
|---|---|
| **JEI** | supported — GT6's recipe categories and item variants are browsable |
| **Jade** | supported — GT6 registers its own tools (wrench, crowbar, cutters…), which vanilla tags cannot express, so harvest tooltips are correct |
| **JourneyMap** and the vanilla map | supported — GT6 blocks and fluids render correctly on both |
| 1.7.10-era industrial mods | not wired up, see [Current state](#current-state) |

## Reporting problems

The most useful report names the symptom and the conditions:

1. what you did — block, item, machine, exact interaction;
2. what happened, and what GregTech 6 on 1.7.10 does instead;
3. single-player or dedicated server, fresh world or an older one;
4. `logs/latest.log` and `gregtech.log`, plus the crash report if there is one.

"This behaves differently from the original" is the single most valuable kind of report for a port,
even when the current behaviour looks reasonable. Reports of that shape usually point at a missing
piece of the port rather than at the one thing you noticed — there is a separate issue template for
exactly this case.

Anything that lets a client crash a dedicated server, leak data or run code goes through private
reporting instead: [SECURITY.md](SECURITY.md).

Sending code rather than a report: [CONTRIBUTING.md](CONTRIBUTING.md) — it explains the two rules
that shape this codebase. What changed between versions: [CHANGELOG.md](CHANGELOG.md).

## License and credits

GregTech 6 is © Gregorius Techneticies and licensed under the **GNU Lesser General Public License,
version 3 or later** (`LICENSE`, `COPYING.LESSER`). This port is a derivative work and is
distributed under the same terms.

Assets are dedicated to the public domain under CC0 1.0 unless stated otherwise
(`LICENSE.assets`); assets containing the GregTech logo or derivatives of it are under
CC BY-NC 4.0 (`LICENSE.logos`).

* **Gregorius Techneticies** — author of GregTech 6. This project moves his work to a new engine;
  the design, the balance and the ideas are his.
* **NeoForged** — the mod loader and its documentation.
* **Applied Energistics 2** and NeoForge's own test mods — read-only references for how things are
  done on the modern engine.
