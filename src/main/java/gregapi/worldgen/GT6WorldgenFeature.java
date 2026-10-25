/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregapi.worldgen;

import java.util.List;
import java.util.Set;

import gregapi.data.MD;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.RemoveFeaturesBiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * F6 central adapter — the ONLY place in the mod that registers GT6 worldgen (veins/layers/small
 * ores) in neo. `IWorldGenerator`/`GameRegistry.registerWorldGenerator` were removed by the engine (it used to be
 * an imperative "generate whatever you want in this chunk"); the replacement is the data-driven chain `Feature`&lt;C&gt; -&gt;
 * `ConfiguredFeature` -&gt; `PlacedFeature` -&gt; `BiomeModifier` (`decisions/F6-worldgen.md` §1,3).
 *
 * <p>Per the decision `decisions/F6-worldgen.md` §3.1,§4, GT6's vein/layer/small-ore algorithms are NOT broken up
 * into vanilla `OreFeature`-like primitives (they cannot express GT6's 4-material vein semantics) — instead
 * ONE dispatcher {@code Feature} is used, whose body reproduces the old
 * {@code IWorldGenerator.generate} call 1:1 (it used to be {@code GT_API_Proxy.generate}, `GT_API_Proxy.java:1456` before
 * this transition) and hands control to the already-existing {@link GT6WorldGenerator#generate(Level,int,int,boolean)}
 * — the same dimension dispatcher/weighted vein choice as before (the algorithm is untouched, only the entry point changed).
 *
 * <p>Signature reference (NOT invented):
 * <ul>
 * <li>{@code Feature<FC>}, {@code place(FeaturePlaceContext)} — {@code neo-decompiled/.../feature/Feature.java:58,183}.</li>
 * <li>{@code FeaturePlaceContext.level()->WorldGenLevel/origin()->BlockPos} — {@code .../feature/FeaturePlaceContext.java:10-51}.</li>
 * <li>{@code ConfiguredFeature}/{@code PlacedFeature} records — {@code .../feature/ConfiguredFeature.java:17}, {@code .../placement/PlacedFeature.java:22}.</li>
 * <li>{@code RegistrySetBuilder}+{@code BootstrapContext}+{@code AddFeaturesBiomeModifier}+{@code DatapackBuiltinEntriesProvider} —
 *     the exact pattern from {@code NeoForge/tests/.../oldtest/world/BiomeModifierTest.java:63-160} (local).</li>
 * <li>{@code WorldGenLevel.getSeed()}/{@code ServerLevelAccessor.getLevel():ServerLevel} —
 *     {@code .../world/level/WorldGenLevel.java:8}, {@code .../world/level/ServerLevelAccessor.java:9}.</li>
 * </ul>
 *
 * <p>F6 functional-adapted (worldgen works — dumps 100%; place is bridged via context.level().getLevel(), the region wrapper is a caveat): {@link #place} bridges into the existing {@code Level}-typed
 * chain {@link GT6WorldGenerator}/{@link WorldgenObject} (which expects a full mutable
 * {@code Level}, just like the original 1.7.10 post-populate hook) via {@code context.level().getLevel()}.
 * At real runtime features are called with a {@code WorldGenRegion} (not always a full {@code ServerLevel}) —
 * `.getLevel()` returns the true {@code ServerLevel}, but bypassing the region wrapper means no
 * thread-safety guarantees at chunk borders under the modern engine's parallel generation. A full
 * migration of the whole chain to {@code WorldGenLevel} would require retyping signatures across ALL ~50
 * {@code WorldgenObject} subclasses (gregapi/worldgen + gregtech/worldgen/*) — outside the scope of this transition
 * (see the checkpoint report); the same applies to preserving the previous values/algorithms inside them.
 */
public class GT6WorldgenFeature extends Feature<NoneFeatureConfiguration> {

	/** The central DeferredRegister — the ONLY place where GT6 registers Feature types in neo. */
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MD.GAPI.mID);

	public static final DeferredHolder<Feature<?>, GT6WorldgenFeature> GT6_WORLDGEN =
		FEATURES.register("gt6_worldgen", GT6WorldgenFeature::new);

	private static final ResourceKey<ConfiguredFeature<?, ?>> GT6_WORLDGEN_CF =
		ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "gt6_worldgen"));
	private static final ResourceKey<PlacedFeature> GT6_WORLDGEN_PF =
		ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "gt6_worldgen"));

	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_OVERWORLD =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "add_gt6_worldgen_overworld"));
	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_NETHER =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "add_gt6_worldgen_nether"));
	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_END =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "add_gt6_worldgen_end"));
	// F6 §4.2.2: disable vanilla MC26 ores (GT6 replaces them with its own — bedrock ores + stone-layer overriding REPLACEABLE_BLOCKS).
	private static final ResourceKey<BiomeModifier> REMOVE_VANILLA_ORES_OVERWORLD =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "remove_vanilla_ores_overworld"));
	private static final ResourceKey<BiomeModifier> REMOVE_VANILLA_ORES_NETHER =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "remove_vanilla_ores_nether"));

	/**
	 * Datagen set: CONFIGURED_FEATURE -> PLACED_FEATURE -> BIOME_MODIFIERS, verbatim per the pattern
	 * {@code BiomeModifierTest.java:87-117} (RegistrySetBuilder.add + BootstrapContext.register/lookup).
	 * F6 functional (dimension/biome routing happens inside place; biome tags are only the input filter): only 3 VANILLA biome tags are wired up ({@link BiomeTags#IS_OVERWORLD}/
	 * {@code IS_NETHER}/{@code IS_END}, real ones — BiomeTags.java:19-21) — GT6WorldgenFeature#place itself
	 * routes by dimension/biome inside {@link GT6WorldGenerator#generate}, just like the original
	 * {@code IWorldGenerator}, which was called unconditionally for every chunk of every dimension; modded dimensions
	 * (Aether/Twilight/Erebus/...) have no biome tags of their own here without knowing their namespace — F10.
	 */
	private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
		.add(Registries.CONFIGURED_FEATURE, ctx -> ctx.register(GT6_WORLDGEN_CF,
			new ConfiguredFeature<>(GT6_WORLDGEN.get(), NoneFeatureConfiguration.INSTANCE)))
		.add(Registries.PLACED_FEATURE, ctx -> ctx.register(GT6_WORLDGEN_PF,
			new PlacedFeature(ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(GT6_WORLDGEN_CF),
				List.of(BiomeFilter.biome()))))
		// ENCHANT: the same datapack point (DatapackBuiltinEntriesProvider below) registers the 4 GT6 enchantments —
		// the center is gregapi/enchants/EnchantsGT6.java (a seam wired up by the integrator).
		.add(Registries.ENCHANTMENT, gregapi.enchants.EnchantsGT6::bootstrap)
		// BUG-004: DAMAGE_TYPE — the same datapack point registers the 13 GT6 damage types (exploded/spike/heat/frost/...).
		// DamageSources.bootstrap used to be written but NOT wired up -> the types were absent from the registry -> a codec crash on damage.
		.add(Registries.DAMAGE_TYPE, gregapi.damage.DamageSources::bootstrap)
		.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
			HolderSet<PlacedFeature> tPlaced = HolderSet.direct(ctx.lookup(Registries.PLACED_FEATURE).getOrThrow(GT6_WORLDGEN_PF));
			// ADAPT-009/flora: the step UNDERGROUND_ORES -> TOP_LAYER_MODIFICATION RESTORES the 1.7.10 order
			// (Forge's IWorldGenerator was called AFTER the WHOLE vanilla populate phase). At UNDERGROUND_ORES GT6 water
			// used to replace Blocks.WATER BEFORE the vanilla VEGETAL_DECORATION -> the kelp/seagrass/coral features (all
			// require is(Blocks.WATER): KelpFeature:26, SeagrassFeature:30, CoralFeature:39 in the reference) never
			// generated AT ALL. Now flora is placed into vanilla water first, then GT6 replaces the water AROUND it
			// (the scan passes through non-opaque plants). WARNING: the generated json is kept in sync with this Java source
			// (src/generated/resources/data/gregapi/neoforge/biome_modifier/add_gt6_worldgen_*.json) — the runtime reads THAT.
			ctx.register(ADD_GT6_WORLDGEN_OVERWORLD, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD), tPlaced, Decoration.TOP_LAYER_MODIFICATION));
			ctx.register(ADD_GT6_WORLDGEN_NETHER, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_NETHER), tPlaced, Decoration.TOP_LAYER_MODIFICATION));
			ctx.register(ADD_GT6_WORLDGEN_END, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_END), tPlaced, Decoration.TOP_LAYER_MODIFICATION));
			// F6 §4.2.2: remove vanilla MC26 ores (allSteps — the authoritative signature per javap RemoveFeaturesBiomeModifier). Keys are OrePlacements (one feature covers both the stone and deepslate ore variant).
			var tPF = ctx.lookup(Registries.PLACED_FEATURE);
			ctx.register(REMOVE_VANILLA_ORES_OVERWORLD, RemoveFeaturesBiomeModifier.allSteps(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
				HolderSet.direct(
					tPF.getOrThrow(OrePlacements.ORE_COAL_UPPER), tPF.getOrThrow(OrePlacements.ORE_COAL_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_IRON_UPPER), tPF.getOrThrow(OrePlacements.ORE_IRON_MIDDLE), tPF.getOrThrow(OrePlacements.ORE_IRON_SMALL),
					tPF.getOrThrow(OrePlacements.ORE_GOLD), tPF.getOrThrow(OrePlacements.ORE_GOLD_LOWER), tPF.getOrThrow(OrePlacements.ORE_GOLD_EXTRA),
					tPF.getOrThrow(OrePlacements.ORE_REDSTONE), tPF.getOrThrow(OrePlacements.ORE_REDSTONE_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_DIAMOND), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_MEDIUM), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_LARGE), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_BURIED),
					tPF.getOrThrow(OrePlacements.ORE_LAPIS), tPF.getOrThrow(OrePlacements.ORE_LAPIS_BURIED),
					tPF.getOrThrow(OrePlacements.ORE_COPPER), tPF.getOrThrow(OrePlacements.ORE_COPPER_LARGE),
					tPF.getOrThrow(OrePlacements.ORE_EMERALD),
					// BUG-033 fix #1 (F6 §4.2.1): the vanilla MC26 STONE blobs — granite/diorite/andesite/tuff. GT6 treats
					// them as "stone" and replaces them with its own layers (they are in StoneLayer.REPLACEABLE_BLOCKS), BUT
					// the vanilla features of these blobs ran in the same underground_ores step and survived the GT6 pass -> the "unswitched rock types" from the report.
					tPF.getOrThrow(OrePlacements.ORE_GRANITE_UPPER), tPF.getOrThrow(OrePlacements.ORE_GRANITE_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_DIORITE_UPPER), tPF.getOrThrow(OrePlacements.ORE_DIORITE_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_ANDESITE_UPPER), tPF.getOrThrow(OrePlacements.ORE_ANDESITE_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_TUFF))));
			ctx.register(REMOVE_VANILLA_ORES_NETHER, RemoveFeaturesBiomeModifier.allSteps(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_NETHER),
				HolderSet.direct(
					tPF.getOrThrow(OrePlacements.ORE_QUARTZ_NETHER), tPF.getOrThrow(OrePlacements.ORE_QUARTZ_DELTAS),
					tPF.getOrThrow(OrePlacements.ORE_GOLD_NETHER),
					tPF.getOrThrow(OrePlacements.ORE_ANCIENT_DEBRIS_LARGE), tPF.getOrThrow(OrePlacements.ORE_ANCIENT_DEBRIS_SMALL))));
		});

	public GT6WorldgenFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	/**
	 * Dispatcher Feature (`decisions/F6-worldgen.md` §4) — CLEAN ARCHITECTURE (2026-07-17).
	 *
	 * <p>{@link GT6WorldGenerator} and the whole chain of {@link WorldgenObject} subclasses were moved from {@code Level} to
	 * {@code WorldGenLevel}/{@code LevelAccessor} (centralized through the god class {@code WD} — one place,
	 * same as Gregorius did it). Thanks to this, generation now runs DIRECTLY in the FEATURES stage via {@code context.level()}
	 * ({@code WorldGenRegion}: access to the center chunk + already-loaded neighbors, {@code getChunk}/{@code setBlock}
	 * WITHOUT forced generation/{@code CompletableFuture.join}).
	 *
	 * <p>The old DEADLOCK is removed AT THE ROOT: the server-tick sweep ({@code onServerTick}->{@code ServerLevel.getChunk}
	 * of the chunk CURRENTLY being generated -> {@code join} -> the chunk waits on itself -> a permanent world-entry hang) NO
	 * LONGER EXISTS — generating in the engine's proper slot (Feature.place on a region) creates no reentrancy. The entry point is 1:1
	 * with the 1.7.10 post-populate hook: {@code generate(world, blockX, blockZ)}.
	 */
	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		net.minecraft.core.BlockPos tOrigin = context.origin();
		GT6WorldGenerator.generate(context.level(), tOrigin.getX(), tOrigin.getZ(), false);
		return true;
	}

	private void onGatherData(GatherDataEvent.Client aEvent) {
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<DatapackBuiltinEntriesProvider>) aOutput ->
			new DatapackBuiltinEntriesProvider(aOutput, aEvent.getLookupProvider(), BUILDER, Set.of(MD.GAPI.mID)));
		// F12-harvest: porting GT6's harvest data (getHarvestTool/getHarvestLevel — the same methods as in 1.7.10)
		// into vanilla tags — the only channel that both the engine and tooltip mods listen to in neo.
		// Same datagen point, no separate event is set up (gregapi/data/GT6HarvestTags.java).
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6HarvestTags>) aOutput ->
			new gregapi.data.GT6HarvestTags(aOutput, aEvent.getLookupProvider()));
		// F12-ammo: porting GT6's ammo data (the IItemProjectile contract — the same one an item is asked via when
		// firing) into the vanilla minecraft:arrows tag. In neo a bow selects ammo ONLY by tag
		// (ProjectileWeaponItem.java:20) and without it does not even post the fire event — gregapi/data/GT6ItemTags.java.
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6ItemTags>) aOutput ->
			new gregapi.data.GT6ItemTags(aOutput, aEvent.getLookupProvider()));
		// F1-b tag bridge, OUTGOING side: GT6 items/blocks into the material-agnostic convention tags c:
		// (c:ingots, c:dusts, c:gems, c:nuggets, c:rods, c:ores, c:storage_blocks, c:raw_materials) — the only
		// thing expressible on the registry entry under the F1-B model. Same datagen point, no separate event is set up
		// (gregapi/data/GT6ConventionTags.java, the name table is gregapi/oredict/OreDictTags.java).
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6ConventionTags>) aOutput ->
			new gregapi.data.GT6ConventionTags(aOutput, aEvent.getLookupProvider()));
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6ConventionTags.Blocks>) aOutput ->
			new gregapi.data.GT6ConventionTags.Blocks(aOutput, aEvent.getLookupProvider()));
	}

	/** F6: the central subscription point, called ONCE from the {@code GT_API} constructor (the same mod bus
	 *  that {@code ITEMS}/{@code BLOCKS} are already subscribed on — F12, `GT_API.java`). */
	public static void register(IEventBus aModBus) {
		FEATURES.register(aModBus);
		aModBus.addListener(GT6WorldgenFeature::onGatherDataStatic);
		aModBus.addListener(GT6WorldgenFeature::onRegisterSpawnPlacements);
		// F6-worldgen: the actual GENERATION of ores/layers/trees now lives in Feature.place (the FEATURES stage, WorldGenLevel) —
		// the server-tick sweep is GONE. Only the load-time reconstruction of MTE stubs remains on the game bus
		// (a separate mechanism, F-tileentity-construction): ChunkEvent.Load catches the stubs -> server-tick replaces them with real MTEs (a FULL chunk, setBlockEntity is safe outside the save cycle).
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkLoad);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkUnload);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkWatch);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onServerTick);
		// CRITICAL (a second world hangs): the static worldgen queues (STUB_QUEUE/CLIENT_STUB_QUEUE/WORLDGEN_MTE/PENDING_SYNC) hold
		// a ChunkReq referencing the LEVEL and BlockEntity of the FIRST world. They used to NOT be cleared on exit -> in the
		// second world: drainStubs processes a stale ChunkReq with a dead level -> getChunk on it hangs -> a freeze after ~9 chunks.
		// We clear them on server stop (between singleplayer worlds). Also reset the BlockRiver static (PLACEMENT_ALLOWED — must not carry over into a new world).
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent aEvent) -> {
			STUB_QUEUE.clear();
			CLIENT_STUB_QUEUE.clear();
			WORLDGEN_MTE.clear();
			PENDING_SYNC.clear();
			gregapi.block.prefixblock.PrefixBlock.clearOreMapSync(); // the same stale-level class: the map holds a ServerLevel
			gregtech.blocks.fluids.BlockRiver.PLACEMENT_ALLOWED = false;
		});
		// Re-entry deadlock (jstack: world-2's Server thread stuck in getChunk->join): world-1's chunk unload runs AFTER
		// ServerStopping -> requests get added AFTER the clear above and outlive the server. A second clear happens on FULL
		// stop (ServerStopped), plus a stale-request filter in drainStubs (do not rely on clear timing alone).
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent aEvent) -> {
			STUB_QUEUE.clear();
			CLIENT_STUB_QUEUE.clear();
			WORLDGEN_MTE.clear();
			PENDING_SYNC.clear();
			gregapi.block.prefixblock.PrefixBlock.clearOreMapSync(); // the same stale-level class: the map holds a ServerLevel
		});
	}

	// R1-fauna: GT6 water (River/Ocean/Swamp) replaced Blocks.WATER with its own block -> vanilla water spawning stays
	// silent, because the selection rules hardcode block IDENTITY: getBlockState(...).is(Blocks.WATER). The 1.7.10 GT6
	// original had no such problem (spawning went by Material.water; fish as entities did not exist there at all — they
	// arrived in MC 1.13, the only water creature before that was the squid). The canonical neo bridge:
	// RegisterSpawnPlacementsEvent with Operation.OR gives the entity type a SECOND predicate, which passes wherever
	// the vanilla one tripped over the mod's water.
	// WARNING: OR extends the entity type's rule AS A WHOLE, and it also applies in vanilla water — so the added
	// predicate must be the rule of THIS EXACT entity type: a verbatim port of its vanilla rule with ONLY the broken
	// link (gt6WaterBlockAt) swapped out. A shared predicate for everyone = someone else's rule for each (the glow
	// squid used to get the surface-fish window and spawn at the surface in broad daylight — BP-BUG-030).
	// The engine has FOUR rules for eight species — the same count here, one-to-one and with the same targets:
	//   WaterAnimal.checkSurfaceWaterAnimalSpawnRules:70-78                -> gt6SurfaceWaterAnimalSpawnRules (COD/SALMON/PUFFERFISH)
	//   AgeableWaterCreature.checkSurfaceAgeableWaterCreatureSpawnRules:66-75 — the SAME text, rule for SQUID/DOLPHIN -> the same method
	//   TropicalFish.checkTropicalFishSpawnRules:261-269                   -> gt6TropicalFishSpawnRules
	//   GlowSquid.checkGlowSquidSpawnRules:110-113                         -> gt6GlowSquidSpawnRules
	//   AbstractNautilus.checkNautilusSpawnRules:134-143                   -> gt6NautilusSpawnRules (its own window seaLevel-25..seaLevel-5)
	// The species list was obtained by a full sweep of SpawnPlacements.java + grepping check*SpawnRules for is(Blocks.WATER) — this is ALL
	// of them whose rule asks about water-block identity. Drowned/Guardian/Axolotl need NO bridge: their rules are entirely
	// tag-based (Drowned.java:136-139, Guardian.java:302-308, Axolotl.java:559-562).
	public static void onRegisterSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent aEvent) {
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.COD          , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.SALMON       , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.PUFFERFISH   , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.SQUID        , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		// ADAPT-009/fauna: the dolphin (warm/lukewarm oceans 1.13+, part of BIOMES_OCEAN) is judged by the same
		// surface-water rule as fish (SpawnPlacements.java:94-99).
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.DOLPHIN      , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.TROPICAL_FISH, GT6WorldgenFeature::gt6TropicalFishSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.GLOW_SQUID   , GT6WorldgenFeature::gt6GlowSquidSpawnRules);
		// The nautilus is a 26.1.2 species (absent in 1.20.1), with vanilla spawns in all nine ocean biomes
		// (data/minecraft/worldgen/biome/*ocean*.json). Its rule hardcodes above-Blocks.WATER just like the fish rule,
		// but its depth window is its OWN — hence its own predicate.
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.NAUTILUS     , GT6WorldgenFeature::gt6NautilusSpawnRules);
	}

	private static <T extends net.minecraft.world.entity.Entity> void addGT6WaterSpawn(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent aEvent, net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.entity.SpawnPlacements.SpawnPredicate<T> aRule) {
		aEvent.register(aType, aRule, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.OR);
	}

	/** The ONLY adapted bridge link. The engine asks "is the block in this cell {@code Blocks.WATER}?"
	 *  ({@code getBlockState(pos).is(Blocks.WATER)}), while GT6's ocean/river/swamp water carries ITS OWN block
	 *  ({@code BlockWaterlike extends BlockFluidBaseGT extends LiquidBlock}, the fluid is genuine {@code Fluids.WATER}:
	 *  {@code BlockWaterlike.java:91} -> {@code BlockFluidBaseGT.java:223}). We ask the same thing in the engine's terms:
	 *  "a full-block liquid, and it is water". In VANILLA water the answer matches the engine's exactly (vanilla's only
	 *  {@code LiquidBlock}s are {@code Blocks.WATER} and {@code Blocks.LAVA}, {@code Blocks.java:294-310}; waterlogged
	 *  cells are not {@code LiquidBlock}s — exactly as with the engine), and it becomes true in GT6 water. */
	private static boolean gt6WaterBlockAt(net.minecraft.world.level.LevelAccessor aLevel, net.minecraft.core.BlockPos aPos) {
		net.minecraft.world.level.block.state.BlockState tState = aLevel.getBlockState(aPos);
		return tState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
			&& tState.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
	}

	/** {@code WaterAnimal.checkSurfaceWaterAnimalSpawnRules} ({@code WaterAnimal.java:70-78}) verbatim, with only the
	 *  {@code getBlockState(above).is(Blocks.WATER)} link swapped. The rule for COD/SALMON/PUFFERFISH and — the same
	 *  text in {@code AgeableWaterCreature:66-75} — for SQUID/DOLPHIN. */
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6SurfaceWaterAnimalSpawnRules(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.EntitySpawnReason aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		int tSea = aLevel.getSeaLevel();
		int tMin = tSea - 13;
		return aPos.getY() >= tMin && aPos.getY() <= tSea
			&& aLevel.getFluidState(aPos.below()).is(net.minecraft.tags.FluidTags.WATER)
			&& gt6WaterBlockAt(aLevel, aPos.above());
	}

	/** {@code TropicalFish.checkTropicalFishSpawnRules} ({@code TropicalFish.java:261-269}) verbatim: the branch
	 *  "a biome tagged ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT — at any depth" is kept too. */
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6TropicalFishSpawnRules(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.EntitySpawnReason aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		return aLevel.getFluidState(aPos.below()).is(net.minecraft.tags.FluidTags.WATER)
			&& gt6WaterBlockAt(aLevel, aPos.above())
			&& (aLevel.getBiome(aPos).is(net.minecraft.tags.BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT)
			 || gt6SurfaceWaterAnimalSpawnRules(aType, aLevel, aReason, aPos, aRandom));
	}

	/** {@code GlowSquid.checkGlowSquidSpawnRules} ({@code GlowSquid.java:110-113}) verbatim: the depth
	 *  {@code y <= seaLevel-33} and total darkness are kept — only the water-block identity check is swapped. */
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6GlowSquidSpawnRules(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.EntitySpawnReason aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		return aPos.getY() <= aLevel.getSeaLevel() - 33
			&& aLevel.getRawBrightness(aPos, 0) == 0
			&& gt6WaterBlockAt(aLevel, aPos);
	}

	/** {@code AbstractNautilus.checkNautilusSpawnRules} ({@code AbstractNautilus.java:134-143}) verbatim: its own
	 *  depth window {@code seaLevel-25 .. seaLevel-5} is kept — only the water-block identity check above it is swapped. */
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6NautilusSpawnRules(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.EntitySpawnReason aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		int tSea = aLevel.getSeaLevel();
		int tMin = tSea - 25;
		return aPos.getY() >= tMin && aPos.getY() <= tSea - 5
			&& aLevel.getFluidState(aPos.below()).is(net.minecraft.tags.FluidTags.WATER)
			&& gt6WaterBlockAt(aLevel, aPos.above());
	}

	private static void onGatherDataStatic(GatherDataEvent.Client aEvent) {
		GT6_WORLDGEN.get().onGatherData(aEvent);
	}

	// F-tileentity-construction (load-time MTE stub reconstruction): a chunk queue for replacing TileEntityLoaderStub with a real MTE.
	// Stubs appear on ANY load of a chunk with an MTE (neo replaces a non-PrefixBlock GT6 MTE with the shared MTE_TYPE -> TileEntityLoaderStub when
	// reading NBT). TWO levels: server-side (STUB_QUEUE, drained on server-tick) AND CLIENT-side (CLIENT_STUB_QUEUE, drained on client-tick).
	// Without client-side reconstruction the MTE BE stays a stub on the client -> not an IRenderedBlockObject -> passRenderingToObject=null ->
	// getRenderPasses=0 -> the block is NOT drawn (invisible: rocks/sticks/fluid sources/machines). Reconstruction is unified (reconstructChunkMTEs).
	private record ChunkReq(net.minecraft.world.level.Level level, int blockX, int blockZ) {}
	private static final java.util.Queue<ChunkReq> STUB_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();
	public  static final java.util.Queue<ChunkReq> CLIENT_STUB_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();

	// F6-worldgen CROSS-CHUNK BE PERSISTENCE: worldgen also places MTEs into NEIGHBORING chunks of the region (tree
	// leaves, bedrock spring features). Under neo's Feature.place model, a BE write into an already-finalized neighbor
	// LevelChunk does NOT persist (only the center-chunk ProtoChunk persists). Diagnostics: srvBE=null persistently for
	// leaves@surface / rock@Y-64 / springs. Fix: on placeBlock (worldgen) we register (registry,pos,id,nbt) keyed by the
	// MTE's OWN CHUNK; when THAT chunk itself finalizes (ChunkEvent.Load), we re-attach the BE to the real LevelChunk — that way the binding sticks and syncs to the client.
	private static final java.util.Map<Long, java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity>> WORLDGEN_MTE = new java.util.concurrent.ConcurrentHashMap<>();
	// PENDING_SYNC: freshly-written worldgen MTEs, for IMMEDIATE sync on server-tick to anyone ALREADY tracking the chunk. Catches
	// cross-chunk MTEs (redstonelight decor) added into a chunk ALREADY sent to a player (onChunkWatch has already run for it).
	private static final java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity> PENDING_SYNC = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static long chunkKey(int aCX, int aCZ) {return ((long)aCX << 32) | (aCZ & 0xFFFFFFFFL);}

	/** Called from {@code WD.te} whenever ANY MTE BE is attached during worldgen (aWorld is not a Level — a WorldGenRegion).
	 *  WD.te is the single central MTE-BE attachment point (placeBlock AND the direct paths both go through it) -> it
	 *  captures EVERYTHING. Keying the record by CHUNK is needed so that on {@link #onChunkWatch sending the chunk to a player}
	 *  we can sync these worldgen MTEs to them (they do not auto-sync: getUpdatePacket=null, and GT6's getClientDataPacket
	 *  is only called during gameplay) — otherwise the client never sees them. */
	public static void recordWorldgenMTE(net.minecraft.world.level.block.entity.BlockEntity aTileEntity) {
		if (aTileEntity == null) return;
		net.minecraft.core.BlockPos tPos = aTileEntity.getBlockPos();
		WORLDGEN_MTE.computeIfAbsent(chunkKey(tPos.getX() >> 4, tPos.getZ() >> 4), k -> new java.util.concurrent.ConcurrentLinkedQueue<>()).add(aTileEntity);
		PENDING_SYNC.add(aTileEntity);
	}

	/** Immediate sync of freshly-written worldgen MTEs (server-tick): if THEIR chunk is loaded — re-attach-if-lost +
	 *  sendClientData to everyone in range. For a chunk not yet sent to any player this is a no-op (nobody in range) — it
	 *  will be picked up by onChunkWatch when it is sent. Specifically catches cross-chunk MTEs in an ALREADY-sent chunk. Drained once per pass, with a quota. */
	private static void drainPendingSync(net.minecraft.server.MinecraftServer aServer) {
		net.minecraft.world.level.block.entity.BlockEntity tBE; int tN = 0;
		while (tN < 256 && (tBE = PENDING_SYNC.poll()) != null) { tN++;
			try {
				net.minecraft.core.BlockPos tPos = tBE.getBlockPos();
				int tCX = tPos.getX() >> 4, tCZ = tPos.getZ() >> 4;
				for (net.minecraft.server.level.ServerLevel tL : aServer.getAllLevels()) {
					net.minecraft.world.level.chunk.LevelChunk tC = tL.getChunkSource().getChunkNow(tCX, tCZ);
					if (tC == null) continue;
					if (!(tC.getBlockState(tPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) break;
					net.minecraft.world.level.block.entity.BlockEntity tCur = tC.getBlockEntity(tPos);
					if (tCur == null) { tBE.clearRemoved(); tL.setBlockEntity(tBE); tCur = tBE; }
					if (tCur instanceof gregapi.tileentity.base.TileEntityBase03TicksAndSync tSync) tSync.sendClientData(true, null);
					break;
				}
			} catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	private static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		if (aEvent.getLevel() instanceof ServerLevel tLevel) {
			// DEFERRED to server-tick — swapping the stub (setBlockEntity) DURING ChunkEvent.Load (which also fires on
			// save/shutdown) caused a reentrancy hang; server-tick.Post does not run during save. Stubs appear on EVERY load of a non-PrefixBlock MTE -> sweep every load.
			STUB_QUEUE.add(new ChunkReq(tLevel, tPos.getMinBlockX(), tPos.getMinBlockZ()));
		} else if (aEvent.getLevel() instanceof net.minecraft.world.level.Level tLevel && tLevel.isClientSide()) {
			// CLIENT: the same mechanism — stub->real MTE, but on client-tick (otherwise MTEs are invisible). We do not touch
			// the ClientLevel class in common code (gated by isClientSide); drained by GT_API_Proxy_Client.onClientMTEReconstruct.
			CLIENT_STUB_QUEUE.add(new ChunkReq(tLevel, tPos.getMinBlockX(), tPos.getMinBlockZ()));
		}
	}

	private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post aEvent) {
		drainStubs(STUB_QUEUE, 16);
		drainPendingSync(aEvent.getServer());
	}

	/** CLIENT SYNC of worldgen MTEs. ROOT of the invisibility: the server HAS the worldgen MTE's BE (source/rock/redstonelight),
	 *  but the client does NOT receive it (the chunk packet carries no MTE BE — getUpdatePacket=null; and GT6's sync
	 *  getClientDataPacket is only called on gameplay placement/update, not on worldgen). Here we catch the MOMENT the
	 *  chunk is sent to a player (ChunkWatchEvent.Sent) and sync all of that chunk's worldgen MTEs to them (+ re-attach
	 *  the BE if it was lost cross-chunk). Precise timing, no per-tick overhead. Note: sweepWorldgenMTE was removed — it was a no-op (everything was skip-hasBE). */
	private static void onChunkWatch(net.neoforged.neoforge.event.level.ChunkWatchEvent.Sent aEvent) {
		net.minecraft.world.level.ChunkPos tCP = aEvent.getPos();
		int tCX = tCP.getMinBlockX() >> 4, tCZ = tCP.getMinBlockZ() >> 4;
		java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity> tQueue = WORLDGEN_MTE.get(chunkKey(tCX, tCZ));
		if (tQueue == null || tQueue.isEmpty()) return;
		net.minecraft.server.level.ServerLevel tLevel = aEvent.getLevel();
		net.minecraft.server.level.ServerPlayer tPlayer = aEvent.getPlayer();
		net.minecraft.world.level.chunk.LevelChunk tChunk = tLevel.getChunkSource().getChunkNow(tCX, tCZ);
		if (tChunk == null) return;
		for (net.minecraft.world.level.block.entity.BlockEntity tBE : tQueue) {
			try {
				net.minecraft.core.BlockPos tPos = tBE.getBlockPos();
				if (!(tChunk.getBlockState(tPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) continue;
				net.minecraft.world.level.block.entity.BlockEntity tCur = tChunk.getBlockEntity(tPos);
				if (tCur == null) { tBE.clearRemoved(); tLevel.setBlockEntity(tBE); tCur = tBE; } // BE was lost cross-chunk after all — re-attach it
				// SYNC to the player who received the chunk: worldgen MTEs do not auto-sync (getUpdatePacket=null) -> send a GT6 data packet (PacketSyncDataIDs -> the client creates the BE).
				// N5 invisibility: only TileEntityBase03TicksAndSync (TICKING ones) used to be handled -> non-ticking worldgen MTEs (Rock pebbles/sticks —
				// TileEntityBase03MultiTileEntities, getUpdateTag=0) never synced to the client -> invisible. Both roots implement the shared
				// ITileEntitySynchronising.sendUpdateToPlayer (Base02Sync:99 and TicksAndSync:101 -> sendClientData) — we sync THROUGH it,
				// targeting the player who received the chunk (rather than a broadcast). Covers both MTE branches with one centralized call.
				if (tCur instanceof gregapi.tileentity.ITileEntitySynchronising tSync) tSync.sendUpdateToPlayer(tPlayer);
			} catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	/** Chunk unload -> drop its records (on reload the MTE arrives as a stub, which stub reconstruction will pick up). */
	private static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload aEvent) {
		if (!(aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel)) return;
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		WORLDGEN_MTE.remove(chunkKey(tPos.getMinBlockX() >> 4, tPos.getMinBlockZ() >> 4));
	}

	/** Client-side drain (called from GT_API_Proxy_Client on ClientTickEvent) — reconstructs MTE stubs on the ClientLevel. */
	public static void drainClientStubs() {drainStubs(CLIENT_STUB_QUEUE, 32);}

	private static void drainStubs(java.util.Queue<ChunkReq> aQueue, int aQuota) {
		ChunkReq tReq; int tM = 0;
		while (tM < aQuota && (tReq = aQueue.poll()) != null) {
			// stale gate (re-entry deadlock): a request whose level belongs to a STOPPED server -> getChunk goes into a permanent
			// CompletableFuture.join (mainThreadProcessor is dead, ServerChunkCache.getChunk:147-148). Skip it instead of draining.
			if (tReq.level() instanceof net.minecraft.server.level.ServerLevel tSL
			 && tSL.getServer() != net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer()) continue;
			try { reconstructChunkMTEs(tReq.level(), tReq.blockX() >> 4, tReq.blockZ() >> 4); tM++; }
			catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	/** F-tileentity-construction (load-time reconstruction): sweep the BEs of a loaded chunk, replacing every {@link gregapi.tileentity.base.TileEntityLoaderStub}
	 *  (the placeholder neo substituted for a GT6 MTE while reading NBT) with a real MTE via the registry. Deferred to server-tick (the chunk is FULL, setBlockEntity is safe). */
	public static void reconstructChunkMTEs(net.minecraft.world.level.Level aLevel, int aChunkX, int aChunkZ) {
		// getChunk(cx,cz,FULL,false) is non-blocking on the main thread (both server-tick AND client-tick), and works for ClientLevel too
		// (unlike the server-only getChunkSource().getChunkNow). null/not-FULL -> skip (we reconstruct on the next load).
		net.minecraft.world.level.chunk.ChunkAccess tCA = aLevel.getChunk(aChunkX, aChunkZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
		if (!(tCA instanceof net.minecraft.world.level.chunk.LevelChunk tChunk)) return;
		java.util.List<net.minecraft.world.level.block.entity.BlockEntity> tStubs = null;
		for (net.minecraft.world.level.block.entity.BlockEntity tBE : tChunk.getBlockEntities().values())
			if (tBE instanceof gregapi.tileentity.base.TileEntityLoaderStub) {(tStubs == null ? tStubs = new java.util.ArrayList<>() : tStubs).add(tBE);}
		if (tStubs != null) for (net.minecraft.world.level.block.entity.BlockEntity tBE : tStubs) reconstructMTE(aLevel, (gregapi.tileentity.base.TileEntityLoaderStub)tBE);
		// (cross-chunk BE persistence is re-attached by the server-tick sweepWorldgenMTE sweep — this method only does stub reconstruction)
	}

	/** Assemble a real MTE from the NBT (reg/id) captured by the stub, and replace the stub with it. The pos channel of getNewTileEntityContainer takes the position from the stub's pos. Level (both server AND client). */
	public static void reconstructMTE(net.minecraft.world.level.Level aLevel, gregapi.tileentity.base.TileEntityLoaderStub aStub) {
		net.minecraft.nbt.CompoundTag tNBT = aStub.mLoadedNBT;
		if (tNBT == null) return;
		// BUG-057 tail (player decision 2026-08-06): self-cleanup of "husks" from old worlds. The earlier save defect
		// (before the TileEntityLoaderStub.saveAdditional fix) wrote only id/x/y/z to disk — the block's identity is lost
		// FOREVER. Exact tell: the NBT is read, but it carries NEITHER the NBT_MTE_REG NOR the NBT_MTE_ID key at all (a
		// legitimate stub carries both keys even when the value is 0). Per the "never invent state" philosophy there is
		// nothing to reconstruct — the ghost block and the stub are simply removed (NO default MTE is produced: pools like aUtilStone are ambiguous).
		if (!tNBT.contains(gregapi.data.CS.NBT_MTE_REG) || !tNBT.contains(gregapi.data.CS.NBT_MTE_ID)) {
			net.minecraft.core.BlockPos tHuskPos = aStub.getBlockPos();
			if (aLevel.getBlockState(tHuskPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)
				aLevel.setBlock(tHuskPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			else aLevel.removeBlockEntity(tHuskPos);
			long tN = sHusksCleaned.incrementAndGet();
			if (tN <= 20 || tN % 500 == 0) gregapi.data.CS.OUT.println("[GT6-WG] BUG-057 husk cleaned up @" + tHuskPos.toShortString() + ", total=" + tN);
			return;
		}
		short tID  = tNBT.getShort(gregapi.data.CS.NBT_MTE_ID ).orElse((short)0);
		// F6-dedicated: the registry is resolved by the CENTER (name -> number -> the single candidate), not by a raw
		// number — a block-item's numeric item id in neo is local to the JVM, and a client on a dedicated server does not know it.
		gregapi.block.multitileentity.MultiTileEntityRegistry tRegistry = gregapi.block.multitileentity.MultiTileEntityRegistry.resolve(tNBT);
		// A stub with a lost/zero reg has nothing to reconstruct from — it stays as-is (BUG-057).
		if (tRegistry == null) return;
		net.minecraft.core.BlockPos tPos = aStub.getBlockPos();
		// block gate (root of the mismatch flood): an orphaned BE (the block at the position is not an MTE — overwritten/air)
		// is NOT reconstructed, the stub is removed -> the world self-cleans orphans instead of an endless "Block state mismatch ... != air" on every load.
		if (!(aLevel.getBlockState(tPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) {
			aLevel.removeBlockEntity(tPos);
			long tN = sOrphansCleaned.incrementAndGet();
			if (tN <= 20 || tN % 500 == 0) gregapi.data.CS.OUT.println("[GT6-WG] orphan BE cleaned up @" + tPos.toShortString() + " (block=" + aLevel.getBlockState(tPos).getBlock() + "), total=" + tN);
			return;
		}
		gregapi.block.multitileentity.MultiTileEntityContainer tContainer = tRegistry.getNewTileEntityContainer(aLevel, tPos.getX(), tPos.getY(), tPos.getZ(), tID, tNBT);
		if (tContainer == null || tContainer.mTileEntity == null) return;
		aLevel.setBlockEntity(tContainer.mTileEntity); // the pos channel -> a real pos -> attaches it in place, replacing the stub
	}
	private static final java.util.concurrent.atomic.AtomicLong sOrphansCleaned = new java.util.concurrent.atomic.AtomicLong();
	private static final java.util.concurrent.atomic.AtomicLong sHusksCleaned = new java.util.concurrent.atomic.AtomicLong();
}
