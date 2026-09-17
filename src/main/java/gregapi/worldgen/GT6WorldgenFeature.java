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
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers.AddFeaturesBiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers.RemoveFeaturesBiomeModifier;
import net.minecraftforge.data.event.GatherDataEvent;

import net.minecraftforge.registries.DeferredRegister;

/** GT6's vein/layer/ore algorithms are kept as one dispatcher Feature that reproduces the old generate() call
 *  1:1, bridging via context.level().getLevel(); a full WorldGenLevel migration would retype ~50 subclasses, out of scope. */
public class GT6WorldgenFeature extends Feature<NoneFeatureConfiguration> {

	/** The only place in the mod that registers Feature types with neo. */
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MD.GAPI.mID);

	public static final net.minecraftforge.registries.RegistryObject<GT6WorldgenFeature> GT6_WORLDGEN =
		FEATURES.register("gt6_worldgen", GT6WorldgenFeature::new);

	private static final ResourceKey<ConfiguredFeature<?, ?>> GT6_WORLDGEN_CF =
		ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(MD.GAPI.mID, "gt6_worldgen"));
	private static final ResourceKey<PlacedFeature> GT6_WORLDGEN_PF =
		ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(MD.GAPI.mID, "gt6_worldgen"));

	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_OVERWORLD =
		ResourceKey.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, new ResourceLocation(MD.GAPI.mID, "add_gt6_worldgen_overworld"));
	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_NETHER =
		ResourceKey.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, new ResourceLocation(MD.GAPI.mID, "add_gt6_worldgen_nether"));
	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_END =
		ResourceKey.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, new ResourceLocation(MD.GAPI.mID, "add_gt6_worldgen_end"));
	// Disables vanilla MC26 ore generation; GT6 supplies its own veins plus a stone-layer overlay instead.
	private static final ResourceKey<BiomeModifier> REMOVE_VANILLA_ORES_OVERWORLD =
		ResourceKey.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, new ResourceLocation(MD.GAPI.mID, "remove_vanilla_ores_overworld"));
	private static final ResourceKey<BiomeModifier> REMOVE_VANILLA_ORES_NETHER =
		ResourceKey.create(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, new ResourceLocation(MD.GAPI.mID, "remove_vanilla_ores_nether"));

	/** Only the three vanilla dimension biome tags are wired here.
	 *  The dispatcher Feature itself routes by dimension/biome internally, as the old generator always did. */
	private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
		.add(Registries.CONFIGURED_FEATURE, ctx -> ctx.register(GT6_WORLDGEN_CF,
			new ConfiguredFeature<>(GT6_WORLDGEN.get(), NoneFeatureConfiguration.INSTANCE)))
		.add(Registries.PLACED_FEATURE, ctx -> ctx.register(GT6_WORLDGEN_PF,
			new PlacedFeature(ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(GT6_WORLDGEN_CF),
				List.of(BiomeFilter.biome()))))
		// The same datapack registration point also wires GT6's own enchantments and its own damage types.
		// The damage-type bootstrap existed but was never actually hooked in, crashing on any GT6 damage codec lookup.
		.add(Registries.DAMAGE_TYPE, gregapi.damage.DamageSources::bootstrap)
		.add(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
			HolderSet<PlacedFeature> tPlaced = HolderSet.direct(ctx.lookup(Registries.PLACED_FEATURE).getOrThrow(GT6_WORLDGEN_PF));
			// Restores 1.7.10's original order: GT6's worldgen used to run after all vanilla population, but the port ran too
			// early and replaced vanilla water before kelp/seagrass/coral could generate in it at all.
			ctx.register(ADD_GT6_WORLDGEN_OVERWORLD, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD), tPlaced, Decoration.TOP_LAYER_MODIFICATION));
			ctx.register(ADD_GT6_WORLDGEN_NETHER, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_NETHER), tPlaced, Decoration.TOP_LAYER_MODIFICATION));
			ctx.register(ADD_GT6_WORLDGEN_END, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_END), tPlaced, Decoration.TOP_LAYER_MODIFICATION));
			// Removes vanilla MC26 ore generation; one placed feature covers both the stone and deepslate variant of each ore.
			var tPF = ctx.lookup(Registries.PLACED_FEATURE);
			ctx.register(REMOVE_VANILLA_ORES_OVERWORLD, RemoveFeaturesBiomeModifier.allSteps(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
				HolderSet.direct(
					tPF.getOrThrow(OrePlacements.ORE_COAL_UPPER), tPF.getOrThrow(OrePlacements.ORE_COAL_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_IRON_UPPER), tPF.getOrThrow(OrePlacements.ORE_IRON_MIDDLE), tPF.getOrThrow(OrePlacements.ORE_IRON_SMALL),
					tPF.getOrThrow(OrePlacements.ORE_GOLD), tPF.getOrThrow(OrePlacements.ORE_GOLD_LOWER), tPF.getOrThrow(OrePlacements.ORE_GOLD_EXTRA),
					tPF.getOrThrow(OrePlacements.ORE_REDSTONE), tPF.getOrThrow(OrePlacements.ORE_REDSTONE_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_DIAMOND), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_LARGE), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_BURIED),
					tPF.getOrThrow(OrePlacements.ORE_LAPIS), tPF.getOrThrow(OrePlacements.ORE_LAPIS_BURIED),
					tPF.getOrThrow(OrePlacements.ORE_COPPER), tPF.getOrThrow(OrePlacements.ORE_COPPER_LARGE),
					tPF.getOrThrow(OrePlacements.ORE_EMERALD),
					// Vanilla stone-blob features (granite/diorite/etc.) ran in the same step GT6 replaces stone in, and survived it,
					// leaving vanilla rock GT6 never intended.
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

	/** The whole WorldgenObject chain now runs on WorldGenLevel/LevelAccessor, generating directly in the FEATURES
	 *  stage; the old server-tick deadlock (a chunk waiting on itself) can't happen inside the engine's own generation slot. */
	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		net.minecraft.core.BlockPos tOrigin = context.origin();
		GT6WorldGenerator.generate(context.level(), tOrigin.getX(), tOrigin.getZ(), false);
		return true;
	}

	private void onGatherData(GatherDataEvent aEvent) {
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<DatapackBuiltinEntriesProvider>) aOutput ->
			new DatapackBuiltinEntriesProvider(aOutput, aEvent.getLookupProvider(), BUILDER, Set.of(MD.GAPI.mID)));
		// Carries GT6's harvest-tool/level data into vanilla tags, the one channel both the engine and tooltip mods read.
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6HarvestTags>) aOutput ->
			new gregapi.data.GT6HarvestTags(aOutput, aEvent.getLookupProvider()));
		// Carries the projectile-ammo contract into vanilla's arrows tag.
		// The bow checks only that tag and won't even fire the shoot event without it.
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6ItemTags>) aOutput ->
			new gregapi.data.GT6ItemTags(aOutput, aEvent.getLookupProvider()));
		// Bridges material-agnostic GT6 groups (ingots/dusts/gems/...) outward into the conventional forge: tags.
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6ConventionTags>) aOutput ->
			new gregapi.data.GT6ConventionTags(aOutput, aEvent.getLookupProvider()));
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<gregapi.data.GT6ConventionTags.Blocks>) aOutput ->
			new gregapi.data.GT6ConventionTags.Blocks(aOutput, aEvent.getLookupProvider()));
	}

	/** Central subscription point, called once from the same mod bus that item/block registration already uses. */
	public static void register(IEventBus aModBus) {
		FEATURES.register(aModBus);
		aModBus.addListener(GT6WorldgenFeature::onGatherDataStatic);
		aModBus.addListener(GT6WorldgenFeature::onRegisterSpawnPlacements);
		// Actual ore/layer/tree generation now runs inside Feature.place.
		// The game bus keeps only the separate MTE-stub reconstruction on chunk load.
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkLoad);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkUnload);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkWatch);
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(GT6WorldgenFeature::onServerTick);
		// Static worldgen queues used to keep a stale first-world level reference, so a second world's drain hung on a
		// dead getChunk and froze after a few chunks; they're now cleared when the server stops between worlds.
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppingEvent aEvent) -> {
			STUB_QUEUE.clear();
			CLIENT_STUB_QUEUE.clear();
			WORLDGEN_MTE.clear();
			PENDING_SYNC.clear();
			gregapi.block.prefixblock.PrefixBlock.clearOreMapSync(); // Same stale-level issue: this map also holds a ServerLevel reference.
			gregtech.blocks.fluids.BlockRiver.PLACEMENT_ALLOWED = false;
		});
		// World-1 chunk unloading happens after the first cleanup, so requests added afterward can survive the server.
		// A second clear on full stop, plus a stale-request filter in the drain, don't rely on timing alone.
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent aEvent) -> {
			STUB_QUEUE.clear();
			CLIENT_STUB_QUEUE.clear();
			WORLDGEN_MTE.clear();
			PENDING_SYNC.clear();
			gregapi.block.prefixblock.PrefixBlock.clearOreMapSync(); // Same stale-level issue: this map also holds a ServerLevel reference.
		});
	}

	// GT6's own water block breaks vanilla's identity-based spawn checks for water mobs, unlike 1.7.10 which tested
	// Material.water; each affected mob gets its own OR'd predicate mirroring vanilla's rule, not one shared check.
	// (Drowned.java:95-96, Guardian.java:296-297, Axolotl.java:444-445).
	public static void onRegisterSpawnPlacements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent aEvent) {
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.COD          , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.SALMON       , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.PUFFERFISH   , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.SQUID        , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		// The dolphin, added with warm/lukewarm oceans, is judged by the same surface-water spawn rule as the fish.
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.DOLPHIN      , GT6WorldgenFeature::gt6SurfaceWaterAnimalSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.TROPICAL_FISH, GT6WorldgenFeature::gt6TropicalFishSpawnRules);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.GLOW_SQUID   , GT6WorldgenFeature::gt6GlowSquidSpawnRules);
	}

	private static <T extends net.minecraft.world.entity.Entity> void addGT6WaterSpawn(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent aEvent, net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.entity.SpawnPlacements.SpawnPredicate<T> aRule) {
		aEvent.register(aType, aRule, net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.OR);
	}

	/** The engine tests for block identity Blocks.WATER; this asks the same question in fluid terms instead (a
	 *  full-block liquid that is water), which agrees with vanilla and only changes the answer inside GT6's own water. */
	private static boolean gt6WaterBlockAt(net.minecraft.world.level.LevelAccessor aLevel, net.minecraft.core.BlockPos aPos) {
		net.minecraft.world.level.block.state.BlockState tState = aLevel.getBlockState(aPos);
		return tState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
			&& tState.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
	}

	/** Vanilla's surface water-animal spawn rule verbatim, with only the block-identity check swapped. */
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6SurfaceWaterAnimalSpawnRules(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.MobSpawnType aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		int tSea = aLevel.getSeaLevel();
		int tMin = tSea - 13;
		return aPos.getY() >= tMin && aPos.getY() <= tSea
			&& aLevel.getFluidState(aPos.below()).is(net.minecraft.tags.FluidTags.WATER)
			&& gt6WaterBlockAt(aLevel, aPos.above());
	}

	/** Vanilla's tropical-fish spawn rule verbatim, keeping the any-depth biome-tag exception. */
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6TropicalFishSpawnRules(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.MobSpawnType aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		return aLevel.getFluidState(aPos.below()).is(net.minecraft.tags.FluidTags.WATER)
			&& gt6WaterBlockAt(aLevel, aPos.above())
			&& (aLevel.getBiome(aPos).is(net.minecraft.tags.BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT)
			 || gt6SurfaceWaterAnimalSpawnRules(aType, aLevel, aReason, aPos, aRandom));
	}

	/** Vanilla's glow-squid spawn rule verbatim (depth and darkness), with only the water-block identity swapped. */
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6GlowSquidSpawnRules(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.MobSpawnType aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		return aPos.getY() <= aLevel.getSeaLevel() - 33
			&& aLevel.getRawBrightness(aPos, 0) == 0
			&& gt6WaterBlockAt(aLevel, aPos);
	}

	private static void onGatherDataStatic(GatherDataEvent aEvent) {
		GT6_WORLDGEN.get().onGatherData(aEvent);
	}

	// Every chunk load can hand back a TileEntityLoaderStub instead of a real MTE, on both server and client, so each
	// side drains its own queue on its own tick; without the client one, the block stays invisible there.
	private record ChunkReq(net.minecraft.world.level.Level level, int blockX, int blockZ) {}
	private static final java.util.Queue<ChunkReq> STUB_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();
	public  static final java.util.Queue<ChunkReq> CLIENT_STUB_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();

	// Worldgen can place an MTE in an already-finalized neighbor chunk, where a BE write during Feature.place
	// doesn't persist; this re-attaches it once that chunk itself finalizes on load.
	private static final java.util.Map<Long, java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity>> WORLDGEN_MTE = new java.util.concurrent.ConcurrentHashMap<>();
	// Records worldgen MTEs by their own chunk, so a real LevelChunk finalizing later can re-attach and sync them.
	private static final java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity> PENDING_SYNC = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static long chunkKey(int aCX, int aCZ) {return ((long)aCX << 32) | (aCZ & 0xFFFFFFFFL);}

	/** WD.te is the single central point that binds any MTE-BE.
	 *  Recording here from it captures every worldgen MTE without a second hook. */
	public static void recordWorldgenMTE(net.minecraft.world.level.block.entity.BlockEntity aTileEntity) {
		if (aTileEntity == null) return;
		net.minecraft.core.BlockPos tPos = aTileEntity.getBlockPos();
		WORLDGEN_MTE.computeIfAbsent(chunkKey(tPos.getX() >> 4, tPos.getZ() >> 4), k -> new java.util.concurrent.ConcurrentLinkedQueue<>()).add(aTileEntity);
		PENDING_SYNC.add(aTileEntity);
	}

	/** Syncs freshly-recorded worldgen MTEs right away if their chunk is already being watched.
	 *  Otherwise onChunkWatch picks them up later. */
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

	private static void onChunkLoad(net.minecraftforge.event.level.ChunkEvent.Load aEvent) {
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		if (aEvent.getLevel() instanceof ServerLevel tLevel) {
			// Deferred to server tick: swapping the stub during ChunkEvent.Load itself caused a re-entrant hang.
			// That same event also fires on save, when a swap would be unsafe.
			STUB_QUEUE.add(new ChunkReq(tLevel, tPos.getMinBlockX(), tPos.getMinBlockZ()));
		} else if (aEvent.getLevel() instanceof net.minecraft.world.level.Level tLevel && tLevel.isClientSide()) {
			// Same stub-to-real-MTE mechanism runs on client tick, since the client would otherwise see invisible MTEs.
			CLIENT_STUB_QUEUE.add(new ChunkReq(tLevel, tPos.getMinBlockX(), tPos.getMinBlockZ()));
		}
	}

	private static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent aEvent) {
		if (aEvent.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
		drainStubs(STUB_QUEUE, 16);
		drainPendingSync(aEvent.getServer());
	}

	/** The chunk packet never carries a worldgen MTE's BE to the client, and GT6's own sync only fires on gameplay
	 *  changes, not worldgen; this catches the exact moment the chunk is sent to a player and syncs it then. */
	private static void onChunkWatch(net.minecraftforge.event.level.ChunkWatchEvent.Watch aEvent) {
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
				if (tCur == null) { tBE.clearRemoved(); tLevel.setBlockEntity(tBE); tCur = tBE; } // The BE got lost cross-chunk after all, so it's re-attached here.
				// Worldgen MTEs never auto-sync (getUpdatePacket=null), so a GT6 data packet is sent explicitly here.
				// Both MTE hierarchies (ticking and non-ticking) share one interface for this, so one call covers both.
				if (tCur instanceof gregapi.tileentity.ITileEntitySynchronising tSync) tSync.sendUpdateToPlayer(tPlayer);
			} catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	/** Unloading a chunk drops its recorded entries; a reload hands back a stub, which the reconstruction path picks up again. */
	private static void onChunkUnload(net.minecraftforge.event.level.ChunkEvent.Unload aEvent) {
		if (!(aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel)) return;
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		WORLDGEN_MTE.remove(chunkKey(tPos.getMinBlockX() >> 4, tPos.getMinBlockZ() >> 4));
	}

	/** Client-side drain, called on the client tick, reconstructing MTE stubs on the ClientLevel. */
	public static void drainClientStubs() {drainStubs(CLIENT_STUB_QUEUE, 32);}

	private static void drainStubs(java.util.Queue<ChunkReq> aQueue, int aQuota) {
		ChunkReq tReq; int tM = 0;
		while (tM < aQuota && (tReq = aQueue.poll()) != null) {
			// A request whose level belongs to an already-stopped server would hang forever in getChunk.
			// It's skipped instead of drained.
			if (tReq.level() instanceof net.minecraft.server.level.ServerLevel tSL
			 && tSL.getServer() != net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()) continue;
			try { reconstructChunkMTEs(tReq.level(), tReq.blockX() >> 4, tReq.blockZ() >> 4); tM++; }
			catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	/** Replaces each loader stub in a loaded chunk with its real MTE via the registry.
	 *  Deferred to server tick, where the chunk is FULL and setBlockEntity is safe. */
	public static void reconstructChunkMTEs(net.minecraft.world.level.Level aLevel, int aChunkX, int aChunkZ) {
		// getChunk(...,FULL,false) never blocks the main thread on either tick, unlike the server-only getChunkNow.
		// A miss just retries on the next load.
		net.minecraft.world.level.chunk.ChunkAccess tCA = aLevel.getChunk(aChunkX, aChunkZ, net.minecraft.world.level.chunk.ChunkStatus.FULL, false);
		if (!(tCA instanceof net.minecraft.world.level.chunk.LevelChunk tChunk)) return;
		java.util.List<net.minecraft.world.level.block.entity.BlockEntity> tStubs = null;
		for (net.minecraft.world.level.block.entity.BlockEntity tBE : tChunk.getBlockEntities().values())
			if (tBE instanceof gregapi.tileentity.base.TileEntityLoaderStub) {(tStubs == null ? tStubs = new java.util.ArrayList<>() : tStubs).add(tBE);}
		if (tStubs != null) for (net.minecraft.world.level.block.entity.BlockEntity tBE : tStubs) reconstructMTE(aLevel, (gregapi.tileentity.base.TileEntityLoaderStub)tBE);
		// Cross-chunk BE re-attachment happens in the server-tick sweep; this only reconstructs the stub.
	}

	/** Builds the real MTE from the stub's captured reg/id and swaps it in, using the stub's own position.
	 *  Works on both server and client. */
	public static void reconstructMTE(net.minecraft.world.level.Level aLevel, gregapi.tileentity.base.TileEntityLoaderStub aStub) {
		net.minecraft.nbt.CompoundTag tNBT = aStub.mLoadedNBT;
		if (tNBT == null) return;
		// A past save defect wrote only position, permanently erasing block identity; the tell is that both identity keys
		// are entirely absent (a legitimate stub always carries them, even as 0), so the ghost is removed, not guessed.
		if (!tNBT.contains(gregapi.data.CS.NBT_MTE_REG) || !tNBT.contains(gregapi.data.CS.NBT_MTE_ID)) {
			net.minecraft.core.BlockPos tHuskPos = aStub.getBlockPos();
			if (aLevel.getBlockState(tHuskPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)
				aLevel.setBlock(tHuskPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			else aLevel.removeBlockEntity(tHuskPos);
			long tN = sHusksCleaned.incrementAndGet();
			if (tN <= 20 || tN % 500 == 0) gregapi.data.CS.OUT.println("[GT6-WG] шелуха BUG-057 вычищена @" + tHuskPos.toShortString() + ", всего=" + tN);
			return;
		}
		short tReg = tNBT.getShort(gregapi.data.CS.NBT_MTE_REG);
		short tID  = tNBT.getShort(gregapi.data.CS.NBT_MTE_ID );
		gregapi.block.multitileentity.MultiTileEntityRegistry tRegistry = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry(tReg);
		// A stub with a lost or zero registry has nothing to reconstruct from and is left as it is.
		if (tRegistry == null) return;
		net.minecraft.core.BlockPos tPos = aStub.getBlockPos();
		// An orphaned BE, where the block here is no longer an MTE, is never reconstructed.
		// Removing the stub lets the world self-clean instead of logging a mismatch warning forever.
		if (!(aLevel.getBlockState(tPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) {
			aLevel.removeBlockEntity(tPos);
			long tN = sOrphansCleaned.incrementAndGet();
			if (tN <= 20 || tN % 500 == 0) gregapi.data.CS.OUT.println("[GT6-WG] BE-сирота вычищен @" + tPos.toShortString() + " (блок=" + aLevel.getBlockState(tPos).getBlock() + "), всего=" + tN);
			return;
		}
		gregapi.block.multitileentity.MultiTileEntityContainer tContainer = tRegistry.getNewTileEntityContainer(aLevel, tPos.getX(), tPos.getY(), tPos.getZ(), tID, tNBT);
		if (tContainer == null || tContainer.mTileEntity == null) return;
		aLevel.setBlockEntity(tContainer.mTileEntity); // The position channel gives the real position, where this attaches, replacing the stub.
	}
	private static final java.util.concurrent.atomic.AtomicLong sOrphansCleaned = new java.util.concurrent.atomic.AtomicLong();
	private static final java.util.concurrent.atomic.AtomicLong sHusksCleaned = new java.util.concurrent.atomic.AtomicLong();
}
