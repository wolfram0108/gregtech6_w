# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Versioning

This project carries the version of the mod it ports, not its own count:

```
6.0.0-alpha.1
│ │ │  └── iteration of the pre-release
│ │ └───── patch: fixes that change no behaviour deliberately
│ └─────── minor: ported subsystems reaching parity, new capabilities
└───────── major: tracks GregTech 6 itself
```

`alpha` means: playable end to end, verified against the original where a machine can decide it, and
**not** stable. Saves are not guaranteed to survive between alpha builds. `beta` will mean feature
complete with save compatibility maintained; a release without a suffix will mean the port is done.

Each release is tagged `v<version>`; the tag and the version in the build must agree, and CI refuses
to publish if they do not.

## [Unreleased]

## [6.0.0-alpha.1] — first public alpha

The first published build of the port. Minecraft 26.1.2, NeoForge 26.1.2.77, Java 25.

### Working

- Client and dedicated server boot; worlds generate and are playable end to end.
- The material × prefix generator: items, blocks, ore dictionary entries and recipes are produced
  procedurally as in the original.
- Ore processing chains, machines, multiblocks, item and fluid logistics.
- Energy in all its forms: electric, steam, kinetic.
- World generation: ores, veins, rock layers, trees and plants, fluid springs, dungeons.
- Crafting, both the vanilla workbench and GregTech's own crafting behaviour.
- Rendering, creative tabs, tooltips, item and block models, fluid rendering.
- GregTech's replacements for vanilla behaviour: water, tool rules, harvest tiers, block hardness.
- Covers: filters, detectors, shutters, texture covers, pumps.
- Integration with JEI, Jade and JourneyMap.

### Known limitations

- Recipes that depend on empty slots in the crafting grid behave differently: the modern crafting
  grid is trimmed before the recipe sees it.
- A small number of workbench recipes differ from the original in ore-list composition.
- Integrations with 1.7.10-era industrial mods (IC2, Forestry, Railcraft, BuildCraft, Thaumcraft and
  others) are not wired up: the compatibility code is ported, but nothing modern is connected to it.
- Worlds are not guaranteed to survive updates. Some fixes apply only to newly generated chunks.

[Unreleased]: https://github.com/wolfram0108/gregtech6_w/compare/v6.0.0-alpha.1...HEAD
[6.0.0-alpha.1]: https://github.com/wolfram0108/gregtech6_w/releases/tag/v6.0.0-alpha.1
