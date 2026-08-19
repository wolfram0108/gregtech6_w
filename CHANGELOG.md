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

**Two game versions, one repository.** This branch (`main`) targets Minecraft 26.1.2 and is versioned
`6.0.0-alpha.N`. The `1.20.1` branch is a backport to that older game version and carries it in the
middle of its own version — `6.0.0-1.20.1-alpha.N` — so the two release series never collide.

## [Unreleased]

## [6.0.0-alpha.5] — the two versions become one set of fixes, and a log that never wrote a line

### Fixed

- **An item placed on an anvil was not drawn.** Machines whose look depends on what is inside them —
  anvils, crates, bookshelves, juicers, mixing bowls, crucibles and seven more — only refreshed that
  look while their chunk was being ticked. A block can be visible without being ticked on this
  engine, so a machine you were standing next to could keep showing an older picture. The look is
  now recomputed when the snapshot is sent, whether the block ticks or not, and the mod no longer
  depends on a signal that another mod can take away by replacing the renderer.
- **The recipe viewer offered recipes the original never showed.** GregTech deliberately hides some
  of its own recipes from the viewer; that rule came across but was not applied, so the list held
  entries the 2014 mod kept out of sight. Recipes that do not fit a three-by-three grid are also laid
  out again the way they were meant to be.
- **Replacing one machine with another kept the first one's contents.** Swapping a machine in place
  left the previous machine's stored data attached to the spot.
- **A corner rail was drawn rotated by half a turn** on two of its four shapes. The track worked; the
  picture lied.
- **An item lying on the ground blocked building.** After a machine was dismantled, its dropped item
  made the square unbuildable until the item was picked up. In the original the item was pushed aside.
- **Geothermal water had no visible flow direction**, so a stream looked like a still pool.
- **Glass slabs showed a seam from inside**, where full glass blocks correctly hide it.
- **A dismantled block could leave its data behind** on the edge of a loaded area.
- **Five fixes found while working on the 1.20.1 backport were brought over**: a burning animal dying
  in lava could crash the server; bars and fluid displays refused to be placed at all; rails and
  flowers could be placed on nothing; crafting with a bottle or bucket could swallow the container;
  and a removal path could delete machines it had not proven absent.
- **The player activity log never wrote a single line on this version.** The mod records who
  interacted with which block; that recording failed to start on every launch, and the failure was
  swallowed silently, so the file existed and stayed empty. Failures to start a subsystem are now
  reported in the mod's log instead of vanishing.
- **A dedicated server did not exit after `stop`.** The world was saved and the ports were released,
  but the process stayed alive and had to be killed by hand, which breaks any restart script. The
  mod now closes what it started when the server shuts down, finishing the log entry it was writing
  instead of losing it.

### Changed

- **Ore map updates are sent in one batch per chunk** instead of one per entry, and the cached
  geometry of a machine is invalidated in constant time rather than by walking the chunk.
- **The build now refuses to publish** if client-only engine types appear in classes that a
  dedicated server loads — the failure that this guards against had already happened once.

### Known issues

- Oil and gas puddles are not named in the Jade tooltip. The mechanism behind it was rejected as
  unsound and will be rebuilt as a property of the block itself, the same way for both versions.
- Vanilla mud duplicates GregTech's own dirt; GregTech fluids cannot be waterlogged; stripping and
  felling a tree the way the original did are not implemented yet.


## [6.0.0-alpha.4] — Applied Energistics 2 fits in, and three old bugs stop coming back

### Added

- **Applied Energistics 2 works alongside GregTech 6 as one progression, not two.** GT6 stays the
  industrial layer; AE2 keeps what only it does — the network, storage, autocrafting and spatial
  storage. Machines of AE2 that merely repeat a GregTech machine no longer have their own recipes,
  so there is one way to make a thing, not two.
- **Energy crosses the border both ways.** GregTech power feeds AE2 directly at 1 EU = 4 FE = 2 AE,
  and AE2 devices charge from it, so no converter block is needed. In configs carried over from an
  earlier build the key `Emit_EU_as_RF_from_Blocks` stays `false` — set it to `true` by hand to
  enable the bridge.
- **One wrench for everything.** Gregorius' wrench turns, dismantles and configures AE2 blocks the
  same way it does GregTech ones; AE2's own tools are no longer needed for that.
- **Sky stone is made, not only found.** It can be smelted from dust and cast into blocks through
  the ordinary GregTech chain. World generation gives you the choice: the AE2 meteorite, or an
  ordinary underground vein of sky stone — one or the other, never both.

### Fixed

- **A vanilla furnace could be crafted from plain cobblestone.** GregTech deliberately gates the
  furnace behind a Fire Starter in the middle of the grid, and the recipe book showed exactly that —
  but the crafting table still accepted a bare ring of cobblestone. The mod was resurrecting the
  vanilla recipe itself, under its own name, after removing it. Twenty-eight recipes were affected
  the same way, among them the saddle, the anvil, the bucket, the pistons, all four rails, the
  dispenser, the redstone lamp, the repeater and the enchanting table.
- **Vanilla blocks stopped stacking after re-entering a world.** Dirt, logs, cobblestone walls and
  seventy-five other blocks would suddenly stack one to a slot — the first visit to a world was
  fine, the second was not, and deleting the config only appeared to help because it meant
  restarting the game. The stack limits are now applied correctly however many times the game
  reloads its data. The config file was never at fault.
- **On a dedicated server, GregTech blocks were invisible.** Bushes, rocks, sticks and machines
  arrived at the client as empty space with a collision box and an unreadable name. The client was
  told which block it was by a number that means something different in every process, so it could
  not recognise it. Blocks now carry a name that reads the same everywhere. Worlds saved before this
  release are repaired on load, not left broken.

## [6.0.0-alpha.3] — biomes decide again, tools are heard and held right, machines cost less to draw

### Fixed

- **Ores that belong to a biome were not generated anywhere.** Every worldgen entry restricted to a
  biome — ruby, cinnabar, topaz, the jaspers, emerald, gold in badlands, copper in savanna, 51 entries
  in all — asked "is this biome in my list?" and always got "no", because the biome arrived stripped of
  its identity. Four materials existed in no world at all: niter, pink diamond, alexandrite and
  dominican amber. The same loss silenced eight other things quietly: hay bales never took the
  humidity of their biome, bees always hatched with day genes instead of desert night ones, and a rock
  could not tell where it lay. Worlds generated before this release keep the ground they already have;
  newly generated chunks carry the ores.
- **Tool sounds were silent.** The wrench, the screwdriver and the beep failed in three different
  ways at once — the sounds were never registered (1.7.10 needed no registration), the list of them
  could not be read from inside a packaged jar, and the mining sound was played by a client-only call
  from code that now runs on the server. Sounds of an action are now born on the server and addressed
  to whoever caused them, exactly as they were heard in 1.7.10. A lost sound no longer disappears in
  silence: it names itself, its reason and its place in the log.
- **Tools and swords were held wrong in hand.** In 1.7.10 an item had exactly two hand poses and a
  flag chose between them; that flag had lost its meaning here, so every flat item was held the same
  way. Both poses are taken from the engine's own models now.
- **Covers stayed visible after being removed.** The packet that carries a machine's full state was
  applied to a reused block entity, and the part that was absent from the packet survived it.
- **Other mods could not see any GregTech name.** Item tooltips, waila-style overlays and mod config
  screens fall back to a second translation table on the client, which the mod never filled — so a
  neighbour mod asking for a GregTech name got the raw key. Both tables are filled now, from the one
  place the mod installs its names.
- **Saplings and flowers blocked the world generator and would not grow on GregTech soil.** The sign
  that means "a plant, easily displaced" covered a single block type here instead of every plant, so
  58 of them — saplings of every wood, dandelions, torchflower — were treated as solid obstacles.
- **Bottled potions and four loot entries were lost.** A fluid could not be poured into the container
  declared for it, because the pairing was never registered: 76 of the 96 fluids that name a container
  had none. Potions in bottles also carried no vanilla potion data — they were nameless, colourless
  and had no effect when drunk. Both are restored, and the loot table for bottles is complete again.
- **Swamp and ocean claimed land beyond their own biome.** In 1.7.10 large water *was* a biome, so
  the original's biome list was enough to hold them; in this version the spill lies in the biome of
  the shore, and the guard did not recognise it.

### Changed

- **The client draws machines far more cheaply.** Machine geometry was rebuilt every single frame;
  it is now kept and rebuilt only when the engine itself marks that part of the world as changed —
  the same signal vanilla uses to rebuild a chunk's mesh, and the same condition under which 1.7.10
  rebuilt it. Reading the world no longer forces chunks to load, ore material is stored per chunk
  instead of per block entity, and a flight recorder writes a line every 30 seconds so a slowdown can
  be examined after the fact rather than reproduced.
- **The log is quiet again.** Ore blocks answered the engine's block-entity contract in a way that
  made it warn on every one of them — hundreds of thousands of lines on an old world.
### Changed

- **Licensing and attribution were audited across the whole distribution and brought in order.**
  Thirteen files had lost the licence notice they were published with, and their copyright line had
  been rewritten — both are now restored verbatim from upstream. Every file carried over from
  GregTech 6 now also states that it was modified in 2026 for this port, as the licence requires,
  and the files written for the port carry their own authorship instead of upstream's.
- **`NOTICE` records where every shipped component comes from**: which assets are upstream's CC0
  work and which were made here, that no GregTech logo (the one non-free component upstream has) is
  part of this distribution, and that the classes sitting in other mods' package names are
  compile-time stand-ins written from scratch — not those projects' code. Each such file now says so
  in its own header as well.
- **README and the in-game mod list state plainly that this is an unofficial port**, not affiliated
  with or endorsed by Gregorius Techneticies, and that problems belong in this issue tracker rather
  than upstream's.

### Added

- **A release can now be verified without trusting the author.** Two independent checks: GitHub
  attests that the published jar was built by this repository's release workflow from the tagged
  commit (`gh attestation verify`), and the build itself is now reproducible — archive timestamps
  and file order are pinned, so building a tag on any machine yields a byte-identical jar whose
  checksum must match the published one. Measured rather than intended: a jar built on Windows and
  the one CI builds on Ubuntu from the same commit are byte-identical, and the digest GitHub attests
  is the one a local build produces. Two causes had to be removed — archive timestamps and ordering,
  then line endings, which Windows expands on checkout and which reach the jar as they sit on disk.
  The check was also run with the settings off, to prove it can fail: without them the same source
  produced two different jars. The jar is still not code-signed, deliberately — the loader ignores
  mod signatures entirely.

## [6.0.0-alpha.2] — crafting by grid position, leaf colour, world generation crash

### Fixed

- **Server crash while a world was generating.** World generation removed leftover item entities from
  the chunk-generation threads, which the game only allows from the server thread; this could corrupt
  its entity bookkeeping and bring the world down. All such removals now go through one place that
  defers them to the server thread. Two related cases were fixed as well: entities are no longer
  removed in the middle of iterating the world's entity list, and the GregTech arrow replacement no
  longer spawns its entity inside the game's own spawn call.
- **Blocks lost their position-dependent colour.** Leaves of GregTech trees took their colour from the
  inventory channel instead of the world one, so rainbow leaves rendered as flat patches that changed
  over time, and ordinary leaves ignored the biome. They are now tinted by position, as in the original.
- **Crafting recipes that depend on where the item sits in the grid.** Placing a single item in
  different cells of the workbench is meant to yield different products — a 16-wire splits into
  8-, 4- or 2-wires, crushed ore yields a flawed gem, dust yields quarters, an ingot yields chunks.
  The game hands recipes a grid cropped to the occupied cells, so the position never reached them and
  only the first variant of each was reachable. 27 recipes work again.
- **Chest loot overflowed its container.** The amount of loot is decided by the category counter, as in
  the original, instead of by the loot table: bookshelves and dungeon chests no longer ask for more
  items than fit, which the game used to drop silently.
- **Wrench, screwdriver and beep sounds were silent** — they were requested from the wrong namespace.

### Added

- **Recipe viewer now shows the grid-position recipes**, and the ones that convert several items of a
  prefix into another, with the cell to place the item in. Neither was ever displayed, in this port or
  in the original's viewer.

### Notes

- **Requires NeoForge 26.1.2.84 or newer** (the previous release was built against 26.1.2.77).
- This release adds a single, narrowly scoped patch into the game's code (a mixin) to recover the grid
  position a recipe needs. It is the only one in the project.
- Worlds generated by earlier builds keep whatever loot their chests already rolled; the change applies
  to newly generated ones.


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
[6.0.0-alpha.2]: https://github.com/wolfram0108/gregtech6_w/releases/tag/v6.0.0-alpha.2
[6.0.0-alpha.1]: https://github.com/wolfram0108/gregtech6_w/releases/tag/v6.0.0-alpha.1
