# Mod compatibility

GregTech 6 was built in an era when industrial mods interlocked deeply: it knew about **211
other mods** and adapted its own content to what they added — their ores and materials, their
machines, their recipes, their world generation. That code came across with the rest of the port and
sits behind the same central registry the original used, but almost none of it is connected to
anything today, because most of those mods do not exist on this engine.

This document says plainly what works, what could work, and what will not.

## Working today

| Mod | Verified against | What is integrated |
|---|---|---|
| **JEI** | `15.48.0.183` | GT6 registers its own recipe categories and item variants; verified in a live client |
| **Jade** | `11.13.3+forge` | GT6 registers its own harvest tools — wrench, crowbar, cutters and the rest — which vanilla tags cannot express, so the "can I mine this" tooltip is correct |
| **Applied Energistics 2** | `15.4.10` | GT6 stays the industrial layer and AE2 keeps the network, storage, autocrafting and spatial storage; AE2 machines that merely repeat a GregTech one lose their own recipes, energy crosses the border both ways, and the GregTech wrench turns and dismantles AE2 blocks |
| **The vanilla map** | — | GT6 blocks and fluids resolve to the correct map colour |

> **JourneyMap** is not part of this branch's build and has not been checked here; on the `main`
> branch it is wired up and verified. The map-colour work itself is shared code and is present on
> both branches.


## What the original integrated with

The table below is generated from the mod's own registry of foreign mods and from a count of how
many places in the code reference each one. The count is a fair measure of how deep the integration
went: a mod with hundreds of references had its materials, ores or machines woven into GT6's own
chains; a mod with a handful had a recipe or two.

**About the status column.** It is filled in only where the answer is known for certain. Whether a
2014-era mod has a modern counterpart is not something this project measured, and guessing would be
worse than silence — so most rows say `not assessed` rather than pretending.

* `⏳ modern version exists` — the mod is alive on modern versions; the integration is not wired up
* `— obsolete` — the integration lost its purpose
* `◻ not assessed` — no claim made

**Nothing marked ⏳ or ◻ is planned work.** These integrations are dormant by design: the port's goal
was GregTech 6 itself, and cross-mod work is a separate undertaking that has not begun.

| Mod | References in code | Status | Note |
|---|---:|---|---|
| HBM's Nuclear Tech Mod | 677 | ◻ not assessed |  |
| The Betweenlands | 543 | ◻ not assessed |  |
| Et Futurum | 471 | — obsolete | backported modern blocks into 1.7.10 — those blocks are vanilla here |
| HarvestCraft | 438 | ◻ not assessed |  |
| Forestry | 436 | ◻ not assessed |  |
| Biomes O' Plenty | 430 | ⏳ modern version exists | not wired up |
| RotaryCraft | 421 | ◻ not assessed |  |
| Netherlicious | 382 | ◻ not assessed |  |
| Erebus | 355 | ◻ not assessed |  |
| Twilight Forest | 340 | ⏳ modern version exists | not wired up |
| Extra Biomes XL | 251 | ◻ not assessed |  |
| Binnie's Extra Bees | 229 | ◻ not assessed |  |
| Botania | 219 | ⏳ modern version exists | not wired up |
| Thaumcraft | 216 | ◻ not assessed |  |
| HEXCraft | 213 | ◻ not assessed |  |
| Railcraft | 213 | ⏳ modern version exists | not wired up |
| The Aether | 212 | ◻ not assessed |  |
| Steamcraft 2 | 211 | ◻ not assessed |  |
| Atum | 195 | ◻ not assessed |  |
| Per Fabrica Ad Astra | 182 | ◻ not assessed |  |
| Applied Energistics | 176 | ✅ integrated | see **Working today** — the modern AE2 is wired up, the 1.7.10 integration is not what does it |
| Tech Guns | 168 | ◻ not assessed |  |
| The Aether | 167 | ◻ not assessed |  |
| Enhanced Biomes | 157 | ◻ not assessed |  |
| Extra Planets | 156 | ◻ not assessed |  |
| Galaxy Space | 153 | ◻ not assessed |  |
| Tropicraft | 148 | ◻ not assessed |  |
| Underground Biomes | 141 | ◻ not assessed |  |
| Binnie's Mods | 136 | ◻ not assessed |  |
| IndustrialCraft 2 | 135 | ◻ not assessed |  |
| Mariculture | 132 | ◻ not assessed |  |
| Plant Mega Pack | 128 | ◻ not assessed |  |
| Rockhounding | 127 | ◻ not assessed |  |
| Mo'Creatures | 121 | ◻ not assessed |  |
| Witchery | 117 | ◻ not assessed |  |
| Actually Additions | 115 | ◻ not assessed |  |
| Blue Power | 108 | ◻ not assessed |  |
| Highlands | 104 | ◻ not assessed |  |
| CandyCraft | 103 | ◻ not assessed |  |
| Voltz Engine | 99 | ◻ not assessed |  |
| MineFantasy II | 96 | ◻ not assessed |  |
| Chisel | 95 | ◻ not assessed |  |
| Ars Magica | 89 | ◻ not assessed |  |
| Thermal Expansion | 87 | ◻ not assessed |  |
| Immersive Engineering | 86 | ⏳ modern version exists | not wired up |
| Metallurgy | 82 | ◻ not assessed |  |
| Galacticraft | 80 | ◻ not assessed |  |
| Netherite Plus | 77 | ◻ not assessed |  |
| MineFactory Reloaded | 68 | ◻ not assessed |  |
| Extra Utilities | 66 | ◻ not assessed |  |
| Thermal Foundation | 66 | ◻ not assessed |  |
| ReactorCraft | 65 | ◻ not assessed |  |
| Binnie's Extra Trees | 58 | ◻ not assessed |  |
| Ender IO | 56 | ◻ not assessed |  |
| Galacticraft Planets | 56 | ◻ not assessed |  |
| AbyssalCraft | 55 | ◻ not assessed |  |
| Mineralogy | 55 | ◻ not assessed |  |
| Open Modular Turrets | 55 | ◻ not assessed |  |
| Divine RPG | 53 | ◻ not assessed |  |
| Flaxbeard's Steam Power | 51 | ◻ not assessed |  |
| Mekanism | 49 | ⏳ modern version exists | not wired up |
| Magic Bees | 44 | ◻ not assessed |  |
| BuildCraft Silicon | 41 | ◻ not assessed |  |
| ElectriCraft | 40 | ◻ not assessed |  |
| Exotic Birbs | 40 | ◻ not assessed |  |
| Ganys Surface | 39 | ◻ not assessed |  |
| Forbidden Magic | 37 | ◻ not assessed |  |
| Thermal Dynamics | 37 | ◻ not assessed |  |
| Balkon's Weapon Mod | 36 | ◻ not assessed |  |
| Cave World 2 | 36 | ◻ not assessed |  |
| Alfheim | 35 | ◻ not assessed |  |
| Factorization | 35 | ◻ not assessed |  |
| IHL | 35 | ◻ not assessed |  |
| BiblioCraft | 33 | ◻ not assessed |  |
| TerraFirmaCraft | 32 | ◻ not assessed |  |
| Project Red Exploration | 29 | ◻ not assessed |  |
| Tinkers Construct | 29 | ◻ not assessed |  |
| Mystcraft | 28 | ◻ not assessed |  |
| Salty Mod | 28 | ◻ not assessed |  |
| BuildCraft | 25 | ◻ not assessed |  |
| BuildCraft Transport | 25 | ◻ not assessed |  |
| Big Reactors | 23 | ◻ not assessed |  |
| Hardcore Ender Expansion | 23 | ◻ not assessed |  |
| Project Red | 23 | ◻ not assessed |  |
| Growthcraft Milk | 22 | ◻ not assessed |  |
| Matter Overdrive | 22 | ◻ not assessed |  |
| Fossils and Archeology | 21 | ◻ not assessed |  |
| ChocoCraft (Plus) | 20 | ◻ not assessed |  |
| Enviromine | 20 | ◻ not assessed |  |
| Extra Simple | 20 | ◻ not assessed |  |
| Falling Meteors | 20 | ◻ not assessed |  |
| Funky Locomotion | 20 | ◻ not assessed |  |
| Lib Vulpes | 18 | ◻ not assessed |  |
| TerraFirmaCraft Plus | 18 | ◻ not assessed |  |
| Storage Drawers | 17 | ◻ not assessed |  |
| Wild Mobs | 17 | ◻ not assessed |  |
| Advanced Rocketry | 16 | ◻ not assessed |  |
| Atomic Science | 16 | ◻ not assessed |  |
| Better Storage | 16 | ◻ not assessed |  |
| Binnie's Botany | 16 | ◻ not assessed |  |
| Draconic Evolution | 16 | ◻ not assessed |  |
| Bamboo Mod | 15 | ◻ not assessed |  |
| Electrical Age | 15 | ◻ not assessed |  |
| Ganys Nether | 15 | ◻ not assessed |  |
| IndustrialCraft 2 Classic | 15 | ◻ not assessed |  |
| Open Computers | 15 | ◻ not assessed |  |
| Redpower | 15 | ◻ not assessed |  |
| Project E | 14 | ◻ not assessed |  |
| Random Things | 14 | ◻ not assessed |  |
| Ganys End | 12 | ◻ not assessed |  |
| Growthcraft Bees | 12 | ◻ not assessed |  |
| Open Blocks | 12 | ◻ not assessed |  |
| ComputerCraft | 11 | ◻ not assessed |  |
| Avaritia | 10 | ◻ not assessed |  |
| ICBM | 10 | ◻ not assessed |  |
| Enderlicious | 9 | ◻ not assessed |  |
| Grimoire of Gaia | 9 | ◻ not assessed |  |
| Howling Moon | 9 | ◻ not assessed |  |
| Cooking for Blockheads | 8 | ◻ not assessed |  |
| Magneticraft | 8 | ◻ not assessed |  |
| ChromatiCraft | 7 | ◻ not assessed |  |
| RF Drills | 7 | ◻ not assessed |  |
| Warpbook | 7 | ◻ not assessed |  |
| Wireless Redstone Chickenbones Edition | 7 | ◻ not assessed |  |
| Aroma1997's Mining Dimension | 6 | ◻ not assessed |  |
| Dynamic Trees | 6 | ◻ not assessed |  |
| Enchiridion | 6 | ◻ not assessed |  |
| JABBA | 6 | ◻ not assessed |  |
| Lost Books | 6 | ◻ not assessed |  |
| AppleCore | 5 | ◻ not assessed |  |
| Better Records | 5 | ◻ not assessed |  |
| BuildCraft Builders | 5 | ◻ not assessed |  |
| Growthcraft | 5 | ◻ not assessed |  |
| Growthcraft Grapes | 5 | ◻ not assessed |  |
| Lootbags | 5 | ◻ not assessed |  |
| Lycanites Mobs (Demon) | 5 | ◻ not assessed |  |
| Better Beginnings | 4 | ◻ not assessed |  |
| Carpenters Blocks | 4 | ◻ not assessed |  |
| Forge Microblocks | 4 | ◻ not assessed |  |
| Growthcraft Bamboo | 4 | ◻ not assessed |  |
| Hardcore Questing Mode | 4 | ◻ not assessed |  |
| Lycanites Mobs (Arctic) | 4 | ◻ not assessed |  |
| Lycanites Mobs (Inferno) | 4 | ◻ not assessed |  |
| Mantle | 4 | ◻ not assessed |  |
| PneumaticCraft | 4 | ◻ not assessed |  |
| Binnie's Genetics | 3 | ◻ not assessed |  |
| BuildCraft Robotics | 3 | ◻ not assessed |  |
| Craft Guide | 3 | ◻ not assessed |  |
| Enchiridion | 3 | ◻ not assessed |  |
| Eureka | 3 | ◻ not assessed |  |
| Lycanites Mobs | 3 | ◻ not assessed |  |
| Magical Crops | 3 | ◻ not assessed |  |
| Project Red Expansion | 3 | ◻ not assessed |  |
| Simple Achievements | 3 | ◻ not assessed |  |
| Technomancy | 3 | ◻ not assessed |  |
| Ztones | 3 | ◻ not assessed |  |
| Binnie Patcher | 2 | ◻ not assessed |  |
| BuildCraft Factory | 2 | ◻ not assessed |  |
| Custom Ore Generation | 2 | ◻ not assessed |  |
| Growthcraft Apples | 2 | ◻ not assessed |  |
| Growthcraft Rice | 2 | ◻ not assessed |  |
| Lycanites Mobs (Mountain) | 2 | ◻ not assessed |  |
| Translocator | 2 | ◻ not assessed |  |
| Treecapitator | 2 | ◻ not assessed |  |
| Aroma1997 Core | 1 | ◻ not assessed |  |
| Battlegear 2 | 1 | ◻ not assessed |  |
| CoFH-Core | 1 | ◻ not assessed |  |
| Dragon API | 1 | ◻ not assessed |  |
| EssentialCraft | 1 | ◻ not assessed |  |
| Ganys Wood Stuff | 1 | ◻ not assessed |  |
| Growthcraft Cellar | 1 | ◻ not assessed |  |
| Growthcraft Fishtrap | 1 | ◻ not assessed |  |
| Growthcraft Hops | 1 | ◻ not assessed |  |
| Hunger Overhaul | 1 | ◻ not assessed |  |
| Lycanites Mobs (Desert) | 1 | ◻ not assessed |  |
| Lycanites Mobs (Shadow) | 1 | ◻ not assessed |  |
| Nuclear Control | 1 | ◻ not assessed |  |
| Streams | 1 | ◻ not assessed |  |
| Thaumcraft Extras | 1 | ◻ not assessed |  |
| Village Names | 1 | ◻ not assessed |  |
| Warp Drive | 1 | ◻ not assessed |  |
| Alternate Terrain Generation | 0 | ◻ not assessed |  |
| Baubles | 0 | ◻ not assessed |  |
| BuildCraft Energy | 0 | ◻ not assessed |  |
| CoFH-API | 0 | ◻ not assessed |  |
| CoFH-API Energy | 0 | ◻ not assessed |  |
| Lycanites Mobs (Forest) | 0 | ◻ not assessed |  |
| Lycanites Mobs (Freshwater) | 0 | ◻ not assessed |  |
| Lycanites Mobs (Jungle) | 0 | ◻ not assessed |  |
| Lycanites Mobs (Plains) | 0 | ◻ not assessed |  |
| Lycanites Mobs (Saltwater) | 0 | ◻ not assessed |  |
| Lycanites Mobs (Swamp) | 0 | ◻ not assessed |  |
| Mekanism Generators | 0 | ◻ not assessed |  |
| Mekanism Tools | 0 | ◻ not assessed |  |
| Micdoodle8 Core | 0 | ◻ not assessed |  |
| Modular Force Field System | 0 | ◻ not assessed |  |
| Natura | 0 | ◻ not assessed |  |
| Progressive Automation | 0 | ◻ not assessed |  |
| Project Red Compatibility | 0 | ◻ not assessed |  |
| Project Red Fabrication | 0 | ◻ not assessed |  |
| Project Red Illumination | 0 | ◻ not assessed |  |
| Project Red Integration | 0 | ◻ not assessed |  |
| Project Red Transmission | 0 | ◻ not assessed |  |
| Project Red Transport | 0 | ◻ not assessed |  |
| Psychedelicraft | 0 | ◻ not assessed |  |
| QwerTech | 0 | ◻ not assessed |  |
| Realistic Terrain Generation | 0 | ◻ not assessed |  |
| Realistic World Generation | 0 | ◻ not assessed |  |
| Soul Forest | 0 | ◻ not assessed |  |
| Wireless Redstone Chickenbones Edition | 0 | ◻ not assessed |  |
| Wireless Redstone Chickenbones Edition | 0 | ◻ not assessed |  |

## Why the code is still there

The compatibility layer was not stripped, for the same reason nothing else in this port was
rewritten: it is part of the mod's architecture. Foreign mods are asked about through one central
registry — "is this mod present" is the same question in ~120 places — and their APIs are mirrored at
compile time only, so nothing foreign ships inside the jar. When a modern counterpart is worth
integrating, the work is to connect one centre, not to reconstruct scattered glue.
