**English** · [Русский](README.ru.md)

# GregTech 6 for NeoForge

[![CI](https://github.com/wolfram0108/gregtech6_w/actions/workflows/build.yml/badge.svg?branch=1.20.1)](https://github.com/wolfram0108/gregtech6_w/actions/workflows/build.yml?query=branch%3A1.20.1)
[![License](https://img.shields.io/badge/license-LGPL--3.0--or--later-blue)](LICENSE)

**A port of [GregTech 6](https://github.com/GregTech6/gregtech6) — Gregorius Techneticies' mod for
Minecraft 1.7.10 — to Minecraft 1.20.1 / NeoForge.**

> [!IMPORTANT]
> **This is an unofficial port.** It is not affiliated with, endorsed by or supported by Gregorius
> Techneticies or the GregTech 6 team. Report problems with it *here*, never upstream — the bugs are
> the port's, not theirs. Where everything in a release comes from, component by component, is set
> out in [NOTICE](NOTICE).

| Your Minecraft | Branch | Loader | Java | Latest release |
|---|---|---|---|---|
| `26.1.2` | [`main`](https://github.com/wolfram0108/gregtech6_w/tree/main) | NeoForge `26.1.2.109` | 25 | [![Release](https://img.shields.io/github/v/release/wolfram0108/gregtech6_w?include_prereleases&sort=semver&label=release)](https://github.com/wolfram0108/gregtech6_w/releases) |
| `1.20.1` | [`1.20.1`](https://github.com/wolfram0108/gregtech6_w/tree/1.20.1) | NeoForge `1.20.1-47.1.106` | 17 | [![Release](https://img.shields.io/github/v/release/wolfram0108/gregtech6_w?include_prereleases&filter=*1.20.1*&label=release)](https://github.com/wolfram0108/gregtech6_w/releases?q=1.20.1) |

**You are reading the `1.20.1` branch — a backport.** It is the same mod and the same generator, built
against the older engine. The `main` branch targets Minecraft `26.1.2`.

What changed between versions — [CHANGELOG.md](CHANGELOG.md).

---

## What GregTech 6 is

The point of GregTech 6 is not the number of new machines and items: it is that its production chains
are modelled on real ones. Iron is not "dig up ore and throw it in a furnace"; it is actual casting with
additives, which needs materials that survive the heat and a mould for the ingot — the freshly cast
metal can burn you. Making steel means saturating the metal with oxygen and holding it within a narrow
temperature range. You cannot simply drop a resource into a furnace: the crucible is heated first and
the temperature watched, or the crucible melts along with its contents.

Everything else follows the same logic. Want steam? Work out how to boil water, or the boilers foul.
Want a crusher? Bring it rotational energy and add a motor that runs on steam or electricity. Want
automation? Learn to make aluminium: its chain has more than a dozen stages, chemistry and several
different processing methods. Electronics need microchips: monocrystals are grown into boules, oil is
split into fractions, ethane and butane are distilled in a catalytic reaction, and boards are stamped
and soldered in solder baths.

Chemistry is another branch of the same rigour. The acids produced in reactions eat through ordinary
tanks: storing and piping them requires acid-resistant materials — stainless steel or plastic. Without
those, there is no holding an acid, and no moving it through pipes.

The world follows the same logic. The random small deposits of iron and gold are gone: world generation
is entirely the mod's own, with rock layers, and ores sit in them by geological rules — some in specific
layers at specific heights, some requiring two layers side by side. Tools are tiered: rock of different
hardness cannot be mined with the same tool.

Behind this rigour is one system: every material has a composition, hardness, melting point and
by-products, and almost every process feeds the next. Vanilla chains do not sit beside GregTech's —
they are replaced by them: wood, stone, rubber, tools, water and mining all follow the same logic. The
mod is huge — hundreds of materials, tens of thousands of items and blocks, about two hundred fluids,
dozens of machine types and multiblocks. Progression is long: the first machines are built by hand,
and later production needs developed power, logistics and automation.

### GT6U addon

GT6 can be paired with **GT6U** (GregTech6-Unofficial), an optional addon by **YueSha** that continues
GT6 for 1.7.10. It expands the game considerably: new machines and multiblocks, a much deeper chemistry
branch, more ore processing and world generation. The addon does not replace GT6 — it extends it;
without it, the mod runs on its own. GT6 and GT6U versions must match.

### What is inside

| Subsystem | What it gives |
|---|---|
| Ores and world generation | veins, rock layers, trees and plants, fluid springs, dungeons |
| Ore processing | crushers, centrifuges, washing, furnaces, by-products |
| Energy | electricity, steam and kinetic; separate efficiency settings per machine group |
| Machines | dozens of machine types and thousands of their variants; multiblocks |
| Logistics | pipes, wires, items and fluids |
| Crafting | the vanilla workbench and GT6's own crafting behaviour; recipes in JEI |
| World and vanilla | GT6 water, tool rules, harvest tiers, block hardness |
| Covers | filters, detectors, shutters, texture covers, pumps |

### Getting started

Start with a hammer and a workbench: the first tools and machines in GT6 are made by hand. For
everything else, look at JEI — the mod shows its own recipe categories and item variants there.

## Mod compatibility

The table lists mods that have modern versions or ports: the compatibility layer was extended for
their modern releases.

| Mod | What works |
|---|---|
| **JEI** | GT6's recipe categories and item variants are browsable |
| **Jade** | block names, tank contents, tools and harvest levels; GT6 supplies the data through the standard channels Jade reads |
| **JourneyMap** and the vanilla map | GT6 blocks and fluids resolve to the correct map colour |
| **Applied Energistics 2** | GT6 stays the industrial layer while AE2 keeps the network, storage, autocrafting and spatial storage; energy crosses the border both ways, GT6 crafting encodes as crafting, and the GregTech wrench turns and dismantles AE2 blocks |
| **BuildCraft: Community Edition** | GT6 machines take BuildCraft's wrench, gates and triggers and connect to its pipes, engines and lasers; BuildCraft oil goes through GT6's processing and GT6 Fuel burns in BuildCraft engines |
| **More Red** | its redstone logic joins GT6's progression: the parts it keeps are built from GT6 components, its red alloy is GT6's Red Alloy, and its wires bond to GT6 wires and machines; its soldering recipes give way to GT6's crafting |
| **Railcraft Reborn** | its items are unified with GT6 materials: ores come from GT6 only, ingots and parts receive GregTech materials; the GT6 crowbar works as a Reborn crowbar, and the Reborn bore sees GT6 blocks |
| **FTB Quests** | GT6 items work as quest icons, tasks and rewards; this branch needs no separate fix |
| **Fluid tanks of other mods** | GT6 pipes, machines and sensors fill, drain and read the fluid storage of any mod that offers the standard fluid interface; a faulty one cannot break them |

The original GregTech 6 integrated with **211 mods** of the 1.7.10 era — their ores, materials, machines
and world generation. What is carried over, what works today and what remains dormant is set out in
[COMPATIBILITY.md](COMPATIBILITY.md).

## FAQ

### Why is the version `6.0.0` when the original is `6.17.06`?

`6.0.0` is the port's own version line, not a continuation of the original numbering. The upstream
version this port was made from is GregTech 6 `v6.17.06`.

### Why was porting it hard?

Between 1.7.10 and 1.20.1 the engine removed almost everything GT6 is built on: item and block metadata,
the block material system, immediate-mode rendering, the ore dictionary, imperative world generation,
the old fluid system, the networking layer, the coremod entry point and code-registered recipes. GT6 is
a monolith: 272 of its classes sit in a single strongly connected component, and it cannot be cut into
independent slices. So instead of file-by-file patches there are 18 central replacements — one per
removed subsystem. The full story is in [PORTING.md](PORTING.md).

### Is the port finished?

The port is complete: the mod builds, boots on client and dedicated server, generates its world and is
playable end to end. What remains is maintenance — fixing reported bugs and compatibility with other
mods.

### Can I use it in a modpack?

Yes. The port is distributed under LGPL-3.0-or-later; no separate permission is needed.

### Do I need the GT6U addon?

No. GT6U is an optional addon by YueSha carrying GregTech6-Unofficial content; without it, GT6 runs on
its own. If you install both, their versions must match.

## Contributing and translations

Sending code rather than a report — [CONTRIBUTING.md](CONTRIBUTING.md): it explains the two rules that
shape this codebase. Translations are welcome: the translator package and the workflow are in
[translations/README.md](translations/README.md). Community rules — [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

Report vulnerabilities that let a client crash a dedicated server, leak data or run code privately:
[SECURITY.md](SECURITY.md).

## Building

Building from source, running the client and server, offline builds and verifying downloaded files are
covered in [BUILDING.md](BUILDING.md).

## License and credits

GregTech 6 is © Gregorius Techneticies and licensed under the **GNU Lesser General Public License,
version 3 or later** (`LICENSE`, `COPYING.LESSER`). This port is a derivative work and is distributed
under the same terms. Files carried over from upstream keep their original notices and say that they
were modified for this port; files written for the port say so as well.

Assets are dedicated to the public domain under CC0 1.0 unless stated otherwise (`LICENSE.assets`);
assets containing the GregTech logo or derivatives of it are under CC BY-NC 4.0 (`LICENSE.logos`).
**No GregTech logo file is part of this distribution** — that licence text ships only so the terms
travel with the work if one is ever added.

[NOTICE](NOTICE) states the provenance of every component that ships: which assets came from upstream
unchanged, which were made here, and why the jar carries other mods' interfaces under `gt6mirror`
(they are stand-ins written from scratch, not those projects' code).

* **Gregorius Techneticies** — author of GregTech 6. This project moves his work to a new engine; the
  design, the balance and the ideas are his.
* **NeoForged** — the mod loader and its documentation.
