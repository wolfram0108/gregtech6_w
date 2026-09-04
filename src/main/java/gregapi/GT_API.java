/**
 * Copyright (c) 2025 GregTech-6 Team
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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.api.distmarker.Dist;
import gregapi.api.Abstract_Mod;
import gregapi.api.Abstract_Proxy;
import gregapi.api.FMLInitializationEvent;
import gregapi.api.FMLModIdMappingEvent;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.api.FMLPreInitializationEvent;
import gregapi.block.ToolCompat;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_CanConnectRedstone;
import gregapi.block.prefixblock.PrefixBlockFallingEntity;
import gregapi.block.prefixblock.PrefixBlockTileEntity;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.code.ItemStackContainer;
import gregapi.compat.ICompat;
import gregapi.compat.buildcraft.ICompatBC;
import gregapi.compat.computercraft.ICompatCC;
import gregapi.compat.forestry.ICompatFR;
import gregapi.compat.galacticraft.ICompatGC;
import gregapi.compat.industrialcraft.ICompatIC2;
import gregapi.compat.industrialcraft.ICompatIC2EUItem;
import gregapi.compat.opencomputers.ICompatOC;
import gregapi.compat.thaumcraft.ICompatTC;
import gregapi.compat.warpdrive.ICompatWD;
import gregapi.config.Config;
import gregapi.config.ConfigCategories;
import gregapi.cover.CoverRegistry;
import gregapi.cover.ICover;
import gregapi.cover.covers.CoverRedstoneRepeater;
import gregapi.cover.covers.CoverRedstoneTorch;
import gregapi.data.*;
import gregapi.dummies.DummyWorld;
import gregapi.enchants.Enchantment_EnderDamage;
import gregapi.enchants.Enchantment_Radioactivity;
import gregapi.enchants.Enchantment_SlimeDamage;
import gregapi.enchants.Enchantment_WerewolfDamage;
import gregapi.item.ItemEmptySlot;
import gregapi.item.ItemFluidDisplay;
import gregapi.item.ItemIntegratedCircuit;
import gregapi.lang.LanguageHandler;
import gregapi.load.LoaderOreDictReRegistrations;
import gregapi.log.LogBuffer;
import gregapi.log.LoggerPlayerActivity;
import gregapi.network.NetworkHandler;
import gregapi.network.packets.*;
import gregapi.network.packets.covers.*;
import gregapi.network.packets.covervisuals.*;
import gregapi.network.packets.data.*;
import gregapi.network.packets.ids.*;
import gregapi.old.Textures;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.*;
import gregapi.render.IRenderedBlockObject.ErrorRenderer;
import gregapi.render.ITexture;
import gregapi.render.TextureSet;
import gregapi.tileentity.energy.EnergyCompat;
import gregapi.util.CR;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.worldgen.GT6WorldGenerator;
import net.minecraft.world.level.block.Block;
import gregapi.block.ItemBlockBase;
import gregapi.block.Material;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.AxeItem;
import gregapi.config.ModConfigSpec;
import gregapi.recipes.RecipeSorter;
import team.chisel.carving.Carving;
import thaumcraft.api.ThaumcraftApi;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * This loads before compatible Mods, except Micdoodlecore. GT_API_Post loads after all compatible Mods.
 *
 * F12 (lifecycle, decisions/F12-registration-lifecycle.md): Gregorius's three native FML mods
 * (GAPI/GAPI_POST/GT) are ported as three separate neo-{@code @Mod}s in their own places — this class
 * remains the entry point of the GAPI mod. The original FML {@code dependencies=} string carried both
 * structural order (GAPI loads before GAPI_POST — required for GT6's 3-mod bundle) and
 * ~150 soft order hints for external compatible mods (compat-mirror, F10 zone). The external
 * order hints in {@code depends()} were not ported — they do not affect compilation/lifecycle of
 * GT6 itself and belong to F10 (compat-mirror), for when those mods actually appear in the tree as neo targets.
 * F12-depends (a conditional note, not a stub): the depends() order hints to compat-mirror mods were not ported — they do not
 * affect GT6's compilation/lifecycle; IF those mods appear in the tree as neo targets, add the soft order hints here.
 *
 * EVIDENCE R7 (fixed): {@code depends()} expects a RAW {@code String[]} modId, without the old FML's
 * prefix parser (fml-decompiled {@code net/neoforged/fml/common/Mod.java:16},
 * {@code FMLJavaModLanguageProvider.java:33,67-70} — a string like {@code "required-before:"+modId} is not
 * found in the loaded mod list, causing the whole entrypoint class to be filtered
 * out of loading). Passed the plain {@code ModIDs.GAPI_POST}.
 *
 * EVIDENCE R8 (refinement): {@code depends()} here only filters the entrypoint by the PRESENCE of modId
 * (fml-decompiled {@code FMLJavaModLanguageProvider.java:33}) — it does NOT set load order
 * (the structural fact "GAPI before GAPI_POST" is NOT expressed in {@code depends()}). The actual order
 * is set by {@code ModSorter} (fml-decompiled {@code net/neoforged/fml/loading/ModSorter.java:194-208})
 * from the {@code [[dependencies.gregapi]]}/{@code [[dependencies.gregapi_post]]} graph via the
 * {@code ordering="BEFORE"/"AFTER"} field in {@code src/main/templates/META-INF/neoforge.mods.toml} —
 * see the comment there too. {@code depends()} here remains an INDEPENDENT REQUIRED gate
 * (don't load the entrypoint if GAPI_POST is absent from the mod list), not a source of order.
 */
@Mod(value = ModIDs.GAPI, depends = {ModIDs.GAPI_POST})
public class GT_API extends Abstract_Mod {
	/**
	 * Replacement for {@code @SidedProxy}: neo has no annotation-based side dispatcher, so the side is
	 * chosen directly via {@link FMLEnvironment#getDist()} (verified: {@code DistExecutor} does not
	 * exist in this neo version — decisions/F12-registration-lifecycle.md §7).
	 *
	 * Assigned in the constructor AFTER {@link MT#init()} (not inline in the field's static initializer):
	 * the client {@link GT_API_Proxy_Client} reads {@code MT.*.mRGBa} in its own constructor, and material
	 * construction goes through {@link #STACKMAPS}. Inline-initializing the api_proxy field would run in
	 * class-init order BEFORE STACKMAPS (declared below) → on the client, MT would be pulled prematurely
	 * and throw an NPE ("STACKMAPS is null"). The original {@code @SidedProxy} was injected by FML while
	 * constructing the mod (after class-init) — the same timing is reproduced by building the proxy in the constructor.
	 */
	public static GT_API_Proxy api_proxy;

	public static final Collection<Map<ItemStackContainer, ?>> STACKMAPS = new ArrayListNoNulls<>();

	/** Used to register Icons. It is not necessary to make those into Lists */
	public static Set<Runnable> sBlockIconload = new HashSetNoNulls<>(), sItemIconload = new HashSetNoNulls<>();
	/** The Icon Registers from Blocks and Items. They will get set right before the corresponding Icon Load Phase as executed in the Runnable List above. */
	// F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo calls): 1.7.10's net.minecraft.client.renderer.texture.IIconRegister
	// is removed from the engine entirely (atlas stitching is now baked models, not immediate-mode icon registration).
	// Same problem class as gregapi/render/TextureSet.java registerIcons(Object) (already ported) —
	// the field is typed as Object (the same degradation), consumers (BI/Textures.java) are already ported to Identifier.
	public static Object sBlockIcons, sItemIcons;

	/**
	 * Centralized F12 registration bridge: the ONLY point through which the whole mod registers
	 * Item/Block in NeoForge's DeferredRegister (replaces the scattered direct DeferredRegister calls
	 * found by review R3 in GT6_Main/GT_API_Proxy/ST — decisions/F12-registration-lifecycle.md).
	 * GT6 creates Item/Block EARLY ({@code new SomeItem()}), and the original then registered the
	 * already-built instance ({@code GameRegistry.registerItem(item, name)}). DeferredRegister expects
	 * a Supplier; we wrap the already-created instance in a Supplier that returns that same instance —
	 * for a single mod load (no registry hot-reload) this is equivalent to the original behavior.
	 */
	public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems (ModIDs.GAPI);
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModIDs.GAPI);

	/** F12-followup (subtype-meta): GT6 1.7.10 stores the item's SUBTYPE in the damage value (getItemDamage 0..32767) — meta items
	 *  (PrefixItem/MultiItem, maxDamage=0) hold thousands of subtypes on one Item via meta. neo clamps setDamageValue to
	 *  [0,maxDamage] (IItemExtension.setDamage) → with maxDamage=0 ALL meta collapses to 0 (all material stacks become
	 *  identical → unification/recipes/MTE break). DAMAGE cannot be reused: it also serves as durability for real items
	 *  (maxDamage>0). The centralized adaptation is a SEPARATE subtype component; {@code ST.meta_} get/set goes through it,
	 *  bypassing the clamp. Persistent (NBT ItemStack.CODEC) + network-synced. Set only when meta!=0 (meta-0 = no component,
	 *  stacks with vanilla). */
	/** F12-followup (MTE-type-timing): a single placeholder BlockEntityType for the whole MTE hierarchy. Creating a BlockEntityType calls
	 *  createIntrusiveHolder → only valid on RegisterEvent<BlockEntityType> (registry unfrozen). We register it with the supplier
	 *  {@code TileEntityBase01Root.createType} (it creates+caches MTE_TYPE); previously <clinit> created it lazily on
	 *  server-start → "Registry is already frozen" → the whole Loader_MultiTileEntities crashed (cables/wires/pipes = 0). */
	public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, ModIDs.GAPI);
	public static final Object MTE_TYPE_HOLDER = BLOCK_ENTITIES.register("mte", gregapi.tileentity.base.TileEntityBase01Root::createType);
	/** BUG-138: a SECOND type of the same hierarchy — the non-ticking half ({@code gregapi.tileentity.notick}, {@code super(F)}).
	 *  It exists not for data but for the single question the engine asks about a block entity before ticking it:
	 *  {@code EntityBlock.getTicker(level, state, type)}. All MTEs share the same block and state, the type is the sole
	 *  distinguisher; details and measurement — {@code TileEntityBase01Root.MTE_TYPE_NOTICK}. */
	public static final Object MTE_TYPE_NOTICK_HOLDER = BLOCK_ENTITIES.register("mte_notick", gregapi.tileentity.base.TileEntityBase01Root::createTypeNoTick);

	/** F12-entity: central EntityType registry for gregapi content — the same approach as ITEMS/BLOCKS/BLOCK_ENTITIES above
	 *  (and gregtech.entities.EntitiesGT for its own arrows). Replaces the removed 1.7.10
	 *  {@code EntityRegistry.registerModEntity} (the 1.7.10 original, GT_API.java:722). */
	public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ModIDs.GAPI);

	/** F12-entity: falling meta-block. Parameters 1:1 from the original
	 *  {@code registerModEntity(PrefixBlockFallingEntity.class, "gt.MetaBlockFallingEntity", 0, this, 160, 1, T)}:
	 *  trackingRange 160 blocks = 10 chunks ({@code clientTrackingRange}), updateFrequency 1 ({@code updateInterval}).
	 *  Size — same as vanilla FALLING_BLOCK (0.98x0.98, {@code EntityType.java:492}), which the 1.7.10 class
	 *  extended. The registry name from "gt.MetaBlockFallingEntity" is lowercased (neo's Identifier forbids
	 *  uppercase) — the same approach as {@code EntitiesGT}. */
	public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<gregapi.block.prefixblock.PrefixBlockFallingEntity>> METABLOCK_FALLING =
		ENTITIES.register("gt_metablockfallingentity", rl -> net.minecraft.world.entity.EntityType.Builder.<gregapi.block.prefixblock.PrefixBlockFallingEntity>of(gregapi.block.prefixblock.PrefixBlockFallingEntity::new, net.minecraft.world.entity.MobCategory.MISC)
			.noLootTable().sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(1)
			.build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, rl)));

	/** BUG-113: the mod's own sounds. In 1.7.10 a sound was addressed by NAME and declared only in assets
	 *  ({@code assets/gregapi/sounds.json}) — no registration was needed, the engine looked up the entry by name.
	 *  In neo the name must have a registered {@code SoundEvent}, otherwise {@code Registry.getValue} returns null and
	 *  the sound silently doesn't play (the playback path is {@code UT.Sounds.SoundWithLocation.play}). Keys are read
	 *  FROM THE SAME FILE as 1.7.10 — the sound list is not duplicated in code and survives asset additions. */
	public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, ModIDs.GAPI);
	static {
		for (String tKey : soundKeysFromAssets()) SOUND_EVENTS.register(tKey, rl -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(rl));
	}
	/** Names of the sounds the mod declares in {@code assets/<namespace>/sounds.json} — the single source of truth.
	 *  DO NOT READ VIA CLASSLOADER: in a dev environment resources sit in a directory and {@code getResourceAsStream}
	 *  serves them, but in a built jar the mod is loaded by FML's module loader, the resource isn't served, the list
	 *  comes back empty — and sounds silently fail to register. That is exactly the difference between running from
	 *  sources (sound worked) and running from a jar (sound didn't). FML's standard path to ITS OWN mod's files is
	 *  ModList/ModFile, which is the same in both environments; the classloader remains a fallback. */
	private static java.util.List<String> soundKeysFromAssets() {
		String tPath = "assets/" + ModIDs.GAPI + "/sounds.json";
		java.util.List<String> rKeys = java.util.List.of();
		try (java.io.InputStream tIn = GT_API.class.getResourceAsStream("/" + tPath)) {
			if (tIn != null) rKeys = soundKeysFrom(tIn);
		} catch (Throwable e) {/* fallback below */}
		// fallback — wherever the class itself physically lives: a directory in dev, a jar in the shipped build
		if (rKeys.isEmpty()) try {
			java.net.URI tSelf = GT_API.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			java.io.File tRoot = new java.io.File(tSelf);
			if (tRoot.isDirectory()) {
				java.io.File tFile = new java.io.File(tRoot, tPath);
				if (tFile.isFile()) rKeys = soundKeysFrom(new java.io.FileInputStream(tFile));
			} else {
				try (java.util.zip.ZipFile tZip = new java.util.zip.ZipFile(tRoot)) {
					java.util.zip.ZipEntry tEntry = tZip.getEntry(tPath);
					if (tEntry != null) rKeys = soundKeysFrom(tZip.getInputStream(tEntry));
				}
			}
		} catch (Throwable e) {/* no sounds will play — visible from the line below */}
		OUT.println("GT6 sounds: declared in " + tPath + " and registered " + rKeys.size() + " sounds " + rKeys);
		return rKeys;
	}
	private static java.util.List<String> soundKeysFrom(java.io.InputStream aIn) throws java.io.IOException {
		try (java.io.InputStream tIn = aIn) {
			com.google.gson.JsonObject tJson = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(tIn, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			return java.util.List.copyOf(tJson.keySet());
		}
	}

	public static final DeferredRegister<net.minecraft.core.component.DataComponentType<?>> COMPONENTS = DeferredRegister.create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, ModIDs.GAPI);
	public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> SUBTYPE =
		COMPONENTS.register("subtype", () -> net.minecraft.core.component.DataComponentType.<Integer>builder()
			.persistent(com.mojang.serialization.Codec.INT)
			.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT)
			.build());

	/** F-size0-catalyst: GT6 uses a stack of size 0 as a catalyst ("input is required but not consumed": extruder-shape/mold).
	 *  1.7.10 held stackSize=0 with the item preserved; neo's count<=0 → isEmpty → getItem=AIR, copy→EMPTY (item is lost).
	 *  Adaptation (center ST.size_): a size-0 stack is stored as count=1 + this marker; {@link gregapi.util.ST#size(ItemStack)}
	 *  returns 0 for marked stacks → recipe-matching/consume/dump see the logical 0, identity is preserved. ST.equal
	 *  compares only item+meta+nbt (not arbitrary components) → the marker is transparent to comparisons. See decisions/F-size0-catalyst. */
	public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<net.minecraft.util.Unit>> ZEROSIZE =
		COMPONENTS.register("zerosize", () -> net.minecraft.core.component.DataComponentType.<net.minecraft.util.Unit>builder()
			.persistent(net.minecraft.util.Unit.CODEC)
			.networkSynchronized(net.minecraft.network.codec.StreamCodec.unit(net.minecraft.util.Unit.INSTANCE))
			.build());

	/** F1/F12/F16 item-model separation: GT6 items built OreDict data+recipes (ST.make = a stack of itself) IN THE CONSTRUCTOR, but
	 *  neo constructs an item @RegisterEvent (the registry is open for an intrusive holder), while stacks are only possible
	 *  @post-freeze (Holder.components are bound later). The constructor registers its stack-init here (a Runnable, no stacks), and
	 *  {@link #runDeferredItemInit()} runs them in setup (post-bind). See decisions/F12-registration-lifecycle.md. */
	public static final List<Runnable> DEFERRED_ITEM_INIT = new ArrayListNoNulls<>();
	public static void deferItemInit(Runnable aInit) {if (aInit != null) DEFERRED_ITEM_INIT.add(aInit);}
	/** F12-followup (oredict-timing): the execution window for the deferred stack-init on server-start. In 1.7.10 GT6's whole content
	 *  pipeline (make stacks → OreDict registration → recipes) ran @Init/@PreInit; neo only binds Holder.components on server-start
	 *  (ReloadableServerResources) → the same pipeline is physically shifted here. OreDictManager.registerOre_ has a guard
	 *  "Only @Init/@PreInit" (sStartedPostInit>0 → throw) — during THIS window the guard is suppressed: this is GT6's init, shifted in time. */
	public static boolean sDeferredItemInitRunning = false;

	/** F11-recipe-scan: queue of scans of FOREIGN recipes (Loader_Recipes_Replace). In 1.7.10 the scan ran on PostInit
	 *  over a fully-built CraftingManager; in neo its input (ore versions of vanilla recipes, F4 role-C) only appears
	 *  on server-start — the queue is executed in {@link #onLevelLoadEarlyItemInit} AFTER role-C and BEFORE
	 *  {@code finalizeRecipeLoading} (so the propertySet/display rebuild sees the already-suppressed recipes). */
	public static final List<Runnable> DEFERRED_RECIPE_SCAN = new ArrayListNoNulls<>();
	public static void deferRecipeScan(Runnable aScan) {if (aScan != null) DEFERRED_RECIPE_SCAN.add(aScan);}
	/** Server of the current recipe-scan window (non-null only while the queue runs) — for {@link #removeDatapackRecipes}. */
	public static net.minecraft.server.MinecraftServer sCurrentServerForRecipeScan = null;

	/** F11-recipe-scan: SUPPRESSING datapack recipes — the neo equivalent of 1.7.10's {@code CraftingManager.getRecipeList().remove(...)}
	 *  (in 1.7.10 Replace removed the replaced recipe from the live list; in neo {@code RecipeManager} has no runtime removal,
	 *  {@code RecipeMap} is immutable). The map is rebuilt via the public factory {@code RecipeMap.create} without the suppressed
	 *  ones, and the private field {@code RecipeManager.recipes} is swapped via reflection — a precedented approach (the same swap
	 *  used for {@code AbstractMinecart.behavior}, JDK 25 writes private instance fields). Called BEFORE finalizeRecipeLoading. */
	/** All keys ever suppressed — for reapplication after /reload (the datapack map is recreated). */
	public static final java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> SUPPRESSED_DATAPACK_RECIPES = new java.util.HashSet<>();

	public void onDatapackSyncReapplySuppression(net.neoforged.neoforge.event.OnDatapackSyncEvent aEvent) {
		if (aEvent.getPlayer() != null) return; // a player joining — the map wasn't recreated; reapplication is only needed on /reload
		removeDatapackRecipes(aEvent.getPlayerList().getServer(), new java.util.HashSet<>(SUPPRESSED_DATAPACK_RECIPES));
	}

	public static void removeDatapackRecipes(net.minecraft.server.MinecraftServer aServer, java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> aRemove) {
		if (aServer == null || aRemove == null || aRemove.isEmpty()) return;
		SUPPRESSED_DATAPACK_RECIPES.addAll(aRemove);
		try {
			net.minecraft.world.item.crafting.RecipeManager tRM = aServer.getRecipeManager();
			java.util.List<net.minecraft.world.item.crafting.RecipeHolder<?>> tKeep = new java.util.ArrayList<>();
			int tBefore = 0;
			for (net.minecraft.world.item.crafting.RecipeHolder<?> tHolder : tRM.recipeMap().values()) {tBefore++; if (!aRemove.contains(tHolder.id())) tKeep.add(tHolder);}
			java.lang.reflect.Field tField = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("recipes");
			tField.setAccessible(true);
			tField.set(tRM, net.minecraft.world.item.crafting.RecipeMap.create(tKeep));
			OUT.println("GT_API: datapack recipes suppressed (F11-recipe-scan): " + (tBefore - tKeep.size()) + " of " + aRemove.size() + " requested.");
		} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	// drain-loop: a callback may add a new deferItemInit (a nested deferral, e.g. block→slab) — handled FIFO
	// without ConcurrentModification; the list is drained fully, including entries added during execution.
	public static void runDeferredItemInit() {
		sDeferredItemInitRunning = true;
		// F4 role-B: vanilla ore-dictionary entries that Forge itself set up BEFORE mods in 1.7.10
		// (OreDictionary.initVanillaEntries — see there for evidence and porting boundaries). Called at the very
		// start of the window, because all of GT6's stack-based content is registered further down this queue and
		// must see an already-populated vanilla dictionary — exactly the same order as in 1.7.10.
		try {gregapi.oredict.OreDictionary.initVanillaEntries();} catch(Throwable e) {e.printStackTrace(ERR);}
		// F1-b tag bridge, INCOMING side (decisions/F4-oredictionary.md §4.4): in 1.7.10 foreign mods called
		// OreDictionary.registerOre themselves and GT6 caught it via an event; in neo the mods' common language is
		// c: tags, and the bridge reads the REAL registry, feeding its content into the same dictionary entry point.
		// The call site isn't arbitrary: setTarget_ with aOverwrite=F leaves the FIRST registered stack as the
		// unification target — so a foreign item must arrive before GT6's own stacks (the queue below), exactly as
		// mods that loaded before GT6 used to arrive first.
		try {gregapi.oredict.OreDictTags.importFromTags();} catch(Throwable e) {e.printStackTrace(ERR);}
		try {while (!DEFERRED_ITEM_INIT.isEmpty()) {Runnable tInit = DEFERRED_ITEM_INIT.remove(0); try {tInit.run();} catch(Throwable e) {e.printStackTrace(ERR);}}}
		finally {sDeferredItemInitRunning = false;}
	}

	/** F12-followup (block-split, MTE): some GT6 subsystems (MultiTileEntityRegistry/MultiTileEntityBlock) BUILD a
	 *  neo Block outside a DeferredRegister supplier AND outside preInit (getOrCreate is called both on preInit and
	 *  on init, with dedup and setMapColor on return) — they can't be expressed as a single registerBlockLazy. Their
	 *  constructing code is wrapped in deferBlockInit(Runnable): the queue runs ON RegisterEvent&lt;Block&gt; (registry
	 *  unfrozen → intrusive holder is fine), and the block itself is registered via {@link #registerBlock} (the
	 *  event.register branch, since this phase's DeferredRegister may already have been processed). BlockItem is
	 *  registered in the ITEMS DR (RegisterEvent&lt;Item&gt; fires later). */
	public static final List<Runnable> DEFERRED_BLOCK_INIT = new ArrayListNoNulls<>();
	public static void deferBlockInit(Runnable aInit) {if (aInit != null) DEFERRED_BLOCK_INIT.add(aInit);}
	/** The active RegisterEvent&lt;Block&gt; while DEFERRED_BLOCK_INIT is draining; non-null ⇒ {@link #registerBlock} registers
	 *  the block directly into this event's registry (this phase's DeferredRegister has already been processed). */
	public static net.neoforged.neoforge.registries.RegisterEvent sBlockRegisterEvent = null;
	private static void runDeferredBlockInit(net.neoforged.neoforge.registries.RegisterEvent aEvent) {
		sBlockRegisterEvent = aEvent;
		try {for (Runnable tInit : DEFERRED_BLOCK_INIT) try {tInit.run();} catch(Throwable e) {e.printStackTrace(ERR);} DEFERRED_BLOCK_INIT.clear();}
		finally {sBlockRegisterEvent = null;}
	}
	private static void onRegisterEvent(net.neoforged.neoforge.registries.RegisterEvent aEvent) {
		if (aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK)) runDeferredBlockInit(aEvent);
	}

	/**
	 * F12: the mod bus, saved from the constructor so lazily-created sub-namespaces can
	 * subscribe to {@code RegisterEvent} (see {@link #itemsFor(String)}).
	 */
	private static IEventBus sModBus = null;
	/**
	 * F12: one {@code DeferredRegister.Items} per owner namespace. GT6 allows creating an
	 * Item under a foreign modId (addons via {@code PrefixItem}), and {@code DeferredRegister} is bound to
	 * a single namespace — so the center keeps a namespace→registry map. This is still ONE center
	 * (the whole mod calls into it), just namespace-aware, as {@code GameRegistry.registerItem(item,name,modId)} was.
	 */
	private static final Map<String, DeferredRegister.Items> ITEMS_BY_NS = new HashMap<>();
	static {ITEMS_BY_NS.put(ModIDs.GAPI, ITEMS);}

	private static DeferredRegister.Items itemsFor(String aNamespace) {
		DeferredRegister.Items rReg = ITEMS_BY_NS.get(aNamespace);
		if (rReg == null) {
			rReg = DeferredRegister.createItems(aNamespace);
			if (sModBus != null) rReg.register(sModBus);
			ITEMS_BY_NS.put(aNamespace, rReg);
		}
		return rReg;
	}

	/** F12/R3 bridge, called from {@code gregapi.util.ST.register(Item, String)}: registration under
	 *  the GAPI namespace (previously a direct made-up {@code DeferredRegister.registerItem(...)}). */
	public static DeferredItem<Item> registerItem(Item aItem, String aRegistryName) {
		return registerItem(aItem, aRegistryName, ModIDs.GAPI);
	}

	/** F12/R3 bridge: registers an Item under the owner namespace {@code aModIDOwner} (replaces the made-up
	 *  3-arg {@code DeferredRegister.registerItem(item, name, modId)} from {@code PrefixItem}/{@code ItemFluidDisplay};
	 *  corresponds to the original {@code GameRegistry.registerItem(item, name, modId)}). Centralized —
	 *  the whole mod registers an Item only through this method. */
	public static DeferredItem<Item> registerItem(Item aItem, String aRegistryName, String aModIDOwner) {
		return itemsFor(aModIDOwner).register(aRegistryName, () -> aItem);
	}

	/** F12-followup (item-split): lazy registration — the supplier CONSTRUCTS the item on RegisterEvent (registry unfrozen →
	 *  {@code Item.<init>}→{@code createIntrusiveHolder} is valid), instead of eagerly in preInit (registry frozen). Call site:
	 *  {@code GT_API.registerItemLazy(modId, name, () -> Field = new ItemX(...))} — the supplier builds the item, assigns the field and
	 *  returns it. The same approach as fluid-split (FluidGT source supplier). Replaces the eager {@code new ItemX()} + self-register. */
	public static DeferredItem<Item> registerItemLazy(String aModIDOwner, String aRegistryName, java.util.function.Supplier<? extends Item> aSupplier) {
		return itemsFor(aModIDOwner).register(sanitizeRegName(aRegistryName), aSupplier);
	}

	private static final Map<String, DeferredRegister.Blocks> BLOCKS_BY_NS = new HashMap<>();
	static {BLOCKS_BY_NS.put(ModIDs.GAPI, BLOCKS);}
	private static DeferredRegister.Blocks blocksFor(String aNamespace) {
		DeferredRegister.Blocks rReg = BLOCKS_BY_NS.get(aNamespace);
		if (rReg == null) {rReg = DeferredRegister.createBlocks(aNamespace); if (sModBus != null) rReg.register(sModBus); BLOCKS_BY_NS.put(aNamespace, rReg);}
		return rReg;
	}

	/** F12-followup (block-split): lazy registration of a BLOCK — the supplier constructs the block on RegisterEvent (registry unfrozen →
	 *  {@code Block.<init>}→{@code createIntrusiveHolder}+setId are valid). BlockItem is registered by the block's OWN constructor via
	 *  {@link #registerItemLazy} (works on RegisterEvent&lt;Block&gt;, since RegisterEvent&lt;Item&gt; hasn't fired yet). Call site:
	 *  {@code GT_API.registerBlockLazy(modId, name, () -> Field = new BlockX(...))}. The same approach as item/fluid-split. */
	public static void registerBlockLazy(String aModIDOwner, String aRegistryName, java.util.function.Supplier<? extends Block> aBlockSupplier) {
		blocksFor(aModIDOwner).register(sanitizeRegName(aRegistryName), aBlockSupplier);
	}

	/** neo's {@link net.minecraft.resources.Identifier} path only allows [a-z0-9/._-]; GT6 item names contain
	 *  uppercase letters (e.g. {@code gt.meta.dustSmall}) — we sanitize ONLY the registration key (the same approach as
	 *  {@code FluidGT.safeRegName}). Item identity for oredict/parity is by object/{@code mNameInternal}, not by key. */
	public static String sanitizeRegName(String aName) {
		String rName = aName.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
		return rName.isEmpty() ? "unnamed" : rName;
	}

	/** F12/R3 bridge, called from {@code gregapi.util.ST.register(Block, String, Class)} (previously a direct
	 *  made-up {@code DeferredRegister.registerBlock(...)}). The Block+BlockItem pair is registered under
	 *  the same name — as in the original {@code GameRegistry.registerBlock(Block, Class, String)}. */
	/** F12-followup (item-split): the central assembly of a BlockItem for a block. neo's {@code BlockItem} has NO (Block) constructor
	 *  (only (Block,Properties)) → {@code callConstructor(BlockItem.class,...)} would return null; we build it directly with an id
	 *  derived from the already-registered block's key (BlockItem shares its id with the block). A custom class
	 *  (ItemBlockBase/PrefixBlockItem/ItemBlockMetaType/...) has a (Block) constructor and sets its own id from the block's key. */
	public static BlockItem blockItemFor(Block aBlock, Class<? extends BlockItem> aItemClass) {
		if (aItemClass != null && aItemClass != BlockItem.class) {
			BlockItem rItem = (BlockItem)UT.Reflection.callConstructor(aItemClass, 0, null, T, aBlock);
			if (rItem != null) return rItem;
		}
		return new BlockItem(aBlock, new net.minecraft.world.item.Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aBlock))));
	}

	public static DeferredBlock<Block> registerBlock(Block aBlock, String aRegistryName, Class<? extends BlockItem> aItemClass) {
		if (sBlockRegisterEvent != null) {
			// F12-followup (block-split, MTE): called from deferBlockInit during RegisterEvent<Block> — the block is ALREADY built
			// (registry unfrozen), so we register it directly into the event's registry (the key is sanitized, matching the block's setId);
			// BlockItem goes into the ITEMS DR (processed on RegisterEvent<Item> later). The BLOCKS DeferredRegister may already have been processed.
			// F12-namespace (MTE): namespace=GT — GT6's gt.multitileentity content (the golden gregtech:), not gregapi. The only
			// caller of registerBlock is MTE (ST.register from MultiTileEntityRegistry/MultiTileEntityBlock). The registry/item-DR key
			// matches the block's setId (ModIDs.GT) and the item key (BuiltInRegistries.BLOCK.getKey(block)=GT). ~17k parity recipes.
			sBlockRegisterEvent.register(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(ModIDs.GT, sanitizeRegName(aRegistryName)), () -> aBlock);
			itemsFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> blockItemFor(aBlock, aItemClass));
			return null;
		}
		DeferredBlock<Block> rBlock = blocksFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> aBlock);
		itemsFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> blockItemFor(aBlock, aItemClass));
		return rBlock;
	}

	private LoggerPlayerActivity mPlayerLogger;

	@SuppressWarnings("unchecked")
	public GT_API(IEventBus aModBus) {
		GAPI = this;
		
		if (!MD.ENCHIRIDION.mLoaded) MD.MaCu.mLoaded = F;
		
		// A bunch of Code that is there to statically initialize the Database in the right order and without crashes.
		MT.init();
		// Replacement for @SidedProxy: build the side proxy here, AFTER MT.init() (the client proxy reads
		// MT.*.mRGBa in its ctor), not inline in the field's static initializer — otherwise class-init would pull MT before STACKMAPS and throw an NPE.
		api_proxy = FMLEnvironment.getDist().isClient() ? new GT_API_Proxy_Client() : new GT_API_Proxy_Server();
		BI.BAROMETER.toString();
		OP.ore.toString();
		
		// Make sure Icons are initialized.
		Textures.BlockIcons.VOID.toString();
		Textures.ItemIcons .VOID.toString();
		ErrorRenderer.INSTANCE.toString();
		
		// Guess what, I got a random Crash from one of those not being classloaded...
		UT.Entities.class.toString();
		IMTE_CanConnectRedstone.class.toString();
		
		
		// F6/26.1.2: the world is no longer built here. Level.<init> requires a biome registry
		// (Level.java:158 → PalettedContainerFactory:25 → lookupOrThrow(Registries.BIOME)), and at the mod
		// construction phase the registries don't exist yet — the attempt crashed on every launch, and CS.DW stayed null.
		// The world is created once the registry exists: gregapi.dummies.DummyWorld.ensure(server.registryAccess()),
		// called on server start (Abstract_Mod.onModServerStarting) — it isn't needed before recipes,
		// its only consumers call recipe.matches(...) once already in-game.
		
		IconsGT.INDEX_BLOCK_GAS       = TextureSet.addToAll(MD.GT.mID, F, "gas");
		IconsGT.INDEX_BLOCK_PLASMA    = TextureSet.addToAll(MD.GT.mID, F, "plasma");
		IconsGT.INDEX_BLOCK_MOLTEN    = TextureSet.addToAll(MD.GT.mID, F, "molten");
		IconsGT.INDEX_BLOCK_PIPE_SIDE = TextureSet.addToAll(MD.GT.mID, F, "pipeSide");
		
		OP.ore              .addTextureSet(MD.GT, F);
		OP.oreGravel        .addTextureSet(MD.GT, F);
		OP.oreDense         .addTextureSet(MD.GT, F);
		OP.oreBedrock       .addTextureSet(MD.GT, F);
		
		OP.pipeTiny         .addTextureSet(MD.GT, F);
		OP.pipeSmall        .addTextureSet(MD.GT, F);
		OP.pipeMedium       .addTextureSet(MD.GT, F);
		OP.pipeLarge        .addTextureSet(MD.GT, F);
		OP.pipeHuge         .addTextureSet(MD.GT, F);
		OP.pipeQuadruple    .addTextureSet(MD.GT, F);
		OP.pipeNonuple      .addTextureSet(MD.GT, F);
		
		OP.wire             .addTextureSet(MD.GT, F);
		OP.foil             .addTextureSet(MD.GT, F);
		
		// F12 boot-timing: the block of vanilla ore-targets (ST.make(Blocks.X) = ItemStack) is MOVED to onLoad
		// (FMLCommonSetupEvent), since at @Mod construction time neo hasn't bound items' Holder.components yet
		// (crashes with "Components not bound yet", Holder.java:273). The "registered first" order is preserved — the block sits
		// at the VERY START of onLoad, before the rest of the data-init. See STATE.md "SYSTEMIC FINDING F12" / decisions/F12.

		// F12: "fixing missing container items" (1.7.10 setContainerItem: mushroom_stew->bowl, potion/experience_bottle->
		// glass_bottle) — NOT NEEDED IN NEO: vanilla ALREADY carries these remainders by default (verified against the reference:
		// Items.java MUSHROOM_STEW = ...usingConvertsTo(BOWL); same for potion/experience_bottle). The operation is redundant →
		// a correct no-op (nothing is "missing"), not a stub. GT6's own items set their craftRemainder on registration.
		
		// Fixing Max Stacksizes that don't make sense.
		ST.forceProperMaxStacksizes();
		
		// Fixing some Adventure Mode things.
		// 1.7.10 mutated a FOREIGN object here: Blocks.bed/sponge/hay_block -> "axe", tnt/monster_egg -> "pickaxe",
		// obsidian -> "pickaxe" 3 (the 1.7.10 original, GT_API.java:204-209). neo has no mutator for a foreign block
		// (harvest-tier is set immutably at registration), BUT the mod queries not the engine but its own center:
		// all six values live in the WD.vanillaPassport, captured by the oracle from the LIVE 1.7.10 — i.e.
		// already together with this fix, because the dumper measured the fully-assembled mod. The function is in place, no mutation needed.

		try {
			// The Access Transformer should make this work
			Material.tnt.setAdventureModeExempt();
		} catch(Throwable e) {
			UT.Reflection.callMethod(Material.tnt, new String[] {"func_85158_p", "setAdventureModeExempt"}, T, F, F);
			e.printStackTrace(ERR);
		}

		// F12 impossible-1:1 (harvest-tier in neo is immutable at ctor time + data-driven BlockTags.MINEABLE_WITH_*, not a runtime mutator): the reflection hack "AxeItem/ItemPickaxe.field_150917_c/
		// field_150915_c" (a private static Set<Block> of "effective" blocks in 1.7.10) has no 1:1
		// counterpart — tool effectiveness in neo is also data-driven (the same BlockTags.MINEABLE_WITH_*),
		// and no similar mutable static fields exist on Item classes in the decompile. Not found in any of the
		// 3 reference roots — degrades to a no-op.

		// F12: this mod's central DeferredRegisters go on the mod bus; the bus is saved so lazily
		// created sub-namespaces (itemsFor) also manage to subscribe to RegisterEvent in time.
		sModBus = aModBus;
		ITEMS .register(aModBus);
		BLOCKS.register(aModBus);
		COMPONENTS.register(aModBus); // F12-followup (subtype-meta): registers the subtype component on the mod bus (RegisterEvent<DataComponentType>)
		BLOCK_ENTITIES.register(aModBus); // F12-followup (MTE-type-timing): placeholder MTE_TYPE on RegisterEvent<BlockEntityType> (before freeze)
		ENTITIES.register(aModBus); // F12-entity: EntityType for the falling meta-block (replaces EntityRegistry.registerModEntity, the 1.7.10 original, GT_API.java:722)
		SOUND_EVENTS.register(aModBus); // BUG-113: the mod's own sounds (sounds.json alone was enough in 1.7.10, neo needs a registered SoundEvent)
		// F12-followup (block-split, MTE): drains DEFERRED_BLOCK_INIT on RegisterEvent<Block> (registry unfrozen) — the single
		// point for subsystems whose block construction can't be expressed as a single registerBlockLazy (see deferBlockInit).
		aModBus.addListener(GT_API::onRegisterEvent);
		// F6: the central worldgen adapter (Feature/PlacedFeature/BiomeModifier) — the same mod bus,
		// a single subscription point (decisions/F6-worldgen.md, gregapi/worldgen/GT6WorldgenFeature.java).
		gregapi.worldgen.GT6WorldgenFeature.register(aModBus);
		// ENCHANT: the central adapter for custom enchantment effects — the same mod bus, a single subscription point
		// (gregapi/enchants/EnchantsGT6.java; closes the F6↔ENCHANT wiring seam, tag `ENCHANT, registration`).
		gregapi.enchants.EnchantsGT6.register(aModBus);
		// Fix #1 (BUG-106): the per-chunk ore-material map — the same mod bus, a single subscription point
		// (gregapi/block/prefixblock/PrefixBlockOreMap.java).
		gregapi.block.prefixblock.PrefixBlockOreMap.register(aModBus);
		// BUG-090: the central DeferredRegister for GT6's potion effects (flammable/slippery/conductive/sticky/
		// insanity — "the feature, not its origin": IE/EnviroMine don't exist for 26.1.2) — the same mod bus, a single
		// subscription point (gregapi/potion/MobEffectsGT.java; int ids are set into PotionsGT.ID_* in postInit below).
		gregapi.potion.MobEffectsGT.register(aModBus);
		// F5: the central fluid DeferredRegisters (FluidType+Fluid) — the same mod bus, a single
		// subscription point (decisions/F5-fluids.md §3, gregapi/fluid/FluidGT.java; closes the earlier F12↔F5 wiring debt).
		gregapi.fluid.FluidGT.FLUID_TYPES.register(aModBus);
		gregapi.fluid.FluidGT.FLUIDS.register(aModBus);
		// F5-capability (MODCOMPAT-001 P2): registers Capabilities.Fluid.BLOCK for the WHOLE GT6 TE hierarchy —
		// restores the standard channel that in 1.7.10 was provided by `implements IFluidHandler` on the TEs themselves
		// (gregapi/fluid/GT6FluidCapability.java). Without it GT6's tanks don't exist from the outside: neither foreign
		// pumps/pipes nor tooltip mods can see them.
		gregapi.fluid.GT6FluidCapability.register(aModBus);
		// The same loss class for ITEMS: in 1.7.10 the base TEs declared IInventory/ISidedInventory, and that was
		// enough for a foreign hopper/pipe/tooltip mod; in neo only a registered
		// Capabilities.Item.BLOCK is visible from the outside, and it wasn't registered (grep = 0). The vanilla
		// Container/WorldlyContainer on the TEs are ported 1:1 — here they are only exposed outward via the
		// engine's standard wrappers (gregapi/tileentity/GT6ItemCapability.java).
		gregapi.tileentity.GT6ItemCapability.register(aModBus);
		// F-attachment: the central DeferredRegister for Entity attachment types (EntityFoodTracker) — the same
		// mod bus, a single subscription point (gregapi/player/EntityFoodTracker.java; replaces 1.7.10's
		// IExtendedEntityProperties, no other file duplicates this registration).
		gregapi.player.EntityFoodTracker.ATTACHMENTS.register(aModBus);
		// F11: the central crafting-bench dispatcher (CustomRecipe SERIALIZERS) — the same mod bus, a single
		// subscription point (decisions/F11-crafting-recipe.md §7, gregapi/recipes/GT6CraftingDispatcher.java; closes
		// the earlier F12↔F11 wiring debt).
		GT6CraftingDispatcher.register(aModBus);
		// F14: the central GUI MenuType (ContainerCommon) — the same mod bus, a single subscription point (decisions/F14-gui-menu.md)
		gregapi.gui.ContainerCommon.register(aModBus);
		// F3-render (client): the single dynamic GT6BlockModel model type on the mod bus (RegisterBlockStateModels).
		// Client-only — delegated to the client proxy (server: no-op), the common code doesn't load client-only classes.
		api_proxy.registerClientModels(aModBus);
		// F16-creative-tab: the single handler for filling tabs (replaces the scattered setCreativeTab calls) — the same mod bus.
		gregapi.item.CreativeTabsGT.register(aModBus);
		// F16/F10: applies the accumulated vanilla/foreign stack-size overrides (ST.setMaxStackSize) via ModifyDefaultComponentsEvent.
		aModBus.addListener(gregapi.util.ST::applyVanillaComponentOverrides);
		// GameTests (checking mechanics in a REAL world) are TEST RIGGING, not shipped content: they live in src/gametest/java,
		// are gated by the -Pgt6probes flag, and subscribe to the mod bus themselves (@EventBusSubscriber). They are no
		// longer called from here — production code doesn't know about the rigging.

		// F12: replacement for the @Mod.EventHandler annotation dispatcher — phases subscribe to the mod bus directly.
		// GT6's three-phase contract (Pre/Init/Post) is preserved 1:1 on top of neo's native lifecycle events:
		// PreInit -> FMLConstructModEvent; Init -> FMLCommonSetupEvent; PostInit -> FMLLoadCompleteEvent
		// (decisions/F12-registration-lifecycle.md §4).
		aModBus.addListener(this::onPreLoad);
		aModBus.addListener(this::onLoad);
		aModBus.addListener(this::onPostLoad);

		// GT6's server phases (Abstract_Mod is already on neo's native events) — on the game bus, not the mod bus.
		NeoForge.EVENT_BUS.addListener(this::onServerStarting);
		NeoForge.EVENT_BUS.addListener(this::onServerStarted);
		NeoForge.EVENT_BUS.addListener(this::onServerStopping);
		NeoForge.EVENT_BUS.addListener(this::onServerStopped);
		// BUG-033 (ROOT CAUSE): the deferred item-init must complete BEFORE the spawn area is pre-generated — see onLevelLoadEarlyItemInit.
		NeoForge.EVENT_BUS.addListener(this::onLevelLoadEarlyItemInit);
		// F11-recipe-scan (boundary M-52): /reload recreates the datapack RecipeMap — Replace suppression
		// is reapplied on OnDatapackSyncEvent (player==null = reload; fires BEFORE recipes are sent to the client).
		NeoForge.EVENT_BUS.addListener(this::onDatapackSyncReapplySuppression);
		// ADAPT-019: the conditional built-in datapack ae2replacegen (suppressing AE2 meteorites) — mod bus.
		aModBus.addListener(this::onAddPackFinders);
	}

	/**
	 * ADAPT-019 (AE2 layer): hooks in the built-in datapack {@code resources/ae2replacegen} — a remove-tag
	 * that empties {@code ae2:has_meteorites} (AE2 26.1's only generation). The pack is hooked in ONLY when
	 * {@code CS.AE2_REPLACE_METEORITE_GENERATION} is set (master key {@code ae2/ReplaceMeteoriteGeneration},
	 * read in {@link #onModPreInit2} following the ic2-flag pattern) — the other half of the same key sets up
	 * a bedrock meteoric-iron vein in {@code Loader_Worldgen}: meteorite present → no vein, no meteorite →
	 * vein present. AE2 26.1 itself no longer has a generation switch (rv3 had one —
	 * {@code AEFeature.MeteoriteWorldGen}, {@code Registration.java:705}; in 26.1 the structure registers
	 * unconditionally, {@code InitStructures.java:54}) — hence the lever is ours.
	 *
	 * <p><b>Timing:</b> the event fires when {@code PackRepository} is created
	 * ({@code ResourcePackLoader.populatePackRepository:76-81} ← patch {@code ServerPacksSource:71-80}) — i.e.
	 * on opening/creating a world or starting a dedicated server. Our preInit is {@code FMLConstructModEvent}
	 * ({@code GT6_Main:716}), the mod's earliest phase, while every creator of the SERVER_DATA repository is a
	 * world-open path ({@code WorldOpenFlows}, {@code CreateWorldScreen}, {@code Main:163}); the only early
	 * {@code createVanillaTrustedRepository} belongs to {@code KnownPacksManager} and is built when connecting to
	 * a server ({@code ClientConfigurationPacketListenerImpl:112}). The flag is always set by the time the event fires.
	 * Order: the engine first adds the mods' builtin packs ({@code ResourcePackLoader:79}) and only THEN sends the
	 * event ({@code :81}), and {@code Pack.Position.TOP} inserts the pack at the end of the list ({@code Pack:228-248}) =
	 * highest priority — the remove is applied after AE2's own datapack (checked by the {@code gt6ae2gen} probe: the tag is empty).
	 */
	public void onAddPackFinders(net.neoforged.neoforge.event.AddPackFindersEvent aEvent) {
		if (aEvent.getPackType() != net.minecraft.server.packs.PackType.SERVER_DATA) return;
		if (!MD.AE.mLoaded) return;
		if (AE2_REPLACE_METEORITE_GENERATION) aEvent.addPackFinders(
			net.minecraft.resources.Identifier.fromNamespaceAndPath(ModIDs.GAPI, "ae2replacegen"),
			net.minecraft.server.packs.PackType.SERVER_DATA,
			net.minecraft.network.chat.Component.literal("GT6: AE2 generation replaced by GregTech"),
			net.minecraft.server.packs.repository.PackSource.BUILT_IN,
			T, // alwaysActive: the pack isn't a player choice — it's driven by a config key
			net.minecraft.server.packs.repository.Pack.Position.TOP);
		// A SECOND ROOT, its own key: rewiring AE2 recipes whose input we took away. So far there's only one —
		// the Network Tool: suppressing both quartz WRENCH recipes (the DisableAllQuartzToolRecipes node) also killed
		// its crafting recipe (tools/network_tool.json requires #ae2:quartz_wrench), and it's an ME mechanic that must
		// keep working. The override swaps the input for GREG's wrench key via the neoforge:components component
		// ingredient (NeoForgeMod:367): item gregtech:gt.metatool.01 plus the component gregapi:subtype = 16 (ToolsGT.WRENCH, CS:2135).
		// The match is PARTIAL (strict defaults to false, DataComponentIngredient:43): the stack's other components aren't
		// compared, so a wrench of ANY material matches, while the sword (subtype 0) does not.
		// A separate pack, not a file merged into the first one: the keys are independent — turning off the
		// tool-recipe suppression returns both the quartz recipes and the native one, without touching the meteorites.
		if (AE2_KILL_QUARTZ_TOOLS) aEvent.addPackFinders(
			net.minecraft.resources.Identifier.fromNamespaceAndPath(ModIDs.GAPI, "ae2gtrecipes"),
			net.minecraft.server.packs.PackType.SERVER_DATA,
			net.minecraft.network.chat.Component.literal("GT6: AE2 recipes rewired to GregTech inputs"),
			net.minecraft.server.packs.repository.PackSource.BUILT_IN,
			T,
			net.minecraft.server.packs.repository.Pack.Position.TOP);
	}

	/** BUG-033 fix (ROOT CAUSE of the spawn-area bug) + F12 refinement. **THE SINGLE authoritative execution point of the
	 *  deferred item-init** ({@link #runDeferredItemInit}, which among other things fills the worldgen registry {@code GEN_GT} via
	 *  {@code Loader_Worldgen}). F12 previously kept the drain on {@code ServerStartingEvent}, but in neo's load order
	 *  the spawn-area pre-generation happens EARLIER: {@code MinecraftServer.loadLevel()} = {@code createLevels()} [where
	 *  {@code LevelEvent.Load} fires] → {@code prepareLevels()} ["Preparing spawn area", spawn worldgen] → and only THEN
	 *  does {@code runServer()} send {@code ServerStartingEvent} (verified against neo {@code MinecraftServer.java:403-411,733-739}).
	 *  Effect of the old order: {@code GEN_GT} was empty at spawn → the spawn area came out as PURE VANILLA (deepslate/ores/
	 *  rock were not substituted), while explored chunks (after ServerStarting) were normal GT6.
	 *  <p>Fix: drain on loading the overworld level — that's inside {@code createLevels()} (the registries are already frozen,
	 *  {@code compositeAccess()} = post-bind, ST.make is valid), but BEFORE {@code prepareLevels()}. By then {@code GEN_GT}
	 *  is ready for the spawn pre-gen. **This is the ONLY drain** — the later point (ServerStarting) was removed: the queue
	 *  is only filled at mod-load (all {@code deferItemInit} calls in constructors/loaders, BEFORE the level loads); by
	 *  {@code LevelEvent.Load} it is full, the drain empties it completely, and nothing is added afterward → the
	 *  ServerStarting call was a proven no-op (a full live player test on a build with both calls confirmed this: everything —
	 *  recipes/tabs/items/generation — works with the drain on LevelEvent.Load, i.e. this moment is post-bind for ALL deferrals). */
	/** Water doesn't regenerate on its own — this is GT6's STANDARD behavior, not a port novelty (confirmed
	 *  by the user's live check on 1.7.10: a source block does NOT appear between two source blocks there). The port
	 *  lost this behavior; here it is RESTORED. The 1.7.10 mechanism wasn't identified in the source (Forge events,
	 *  GT6 ASM patches, reflection over Blocks.water, configs, and block substitution were all checked) — the RESULT
	 *  is reproduced instead. On engine 26.1.2 the ONLY channel providing it is the world rule
	 *  {@code water_source_conversion} ({@code GameRules.java:92}, default true), which vanilla water itself reads
	 *  ({@code WaterFluid.canConvertToSource:76-77}). The mod has no other hook: the fluid is vanilla,
	 *  its {@code FluidType} belongs to the engine. So the rule is set ONCE on overworld load —
	 *  the same place the mod already brings the world into its own state. Lava isn't touched (it was already finite in 1.7.10 anyway).
	 *
	 *  The {@code general.WaterSourceConversion} setting restores VANILLA behavior (default F = water is finite,
	 *  as in GT6). Warning: the rule is written into the world itself (level.dat): after removing the mod it stays
	 *  off until a player restores it via command — a side effect of the only channel available. */
	private void applyWaterSourceConversionRule(net.minecraft.server.level.ServerLevel aLevel) {
		try {
			boolean tWanted = gregapi.data.CS.WATER_SOURCE_CONVERSION;
			net.minecraft.world.level.gamerules.GameRules tRules = aLevel.getGameRules();
			if (tRules.get(net.minecraft.world.level.gamerules.GameRules.WATER_SOURCE_CONVERSION) == tWanted) return;
			tRules.set(net.minecraft.world.level.gamerules.GameRules.WATER_SOURCE_CONVERSION, tWanted, aLevel.getServer());
			OUT.println("[GT6] infinite water: water_source_conversion rule = " + tWanted + (tWanted ? " (vanilla behaviour, per config)" : " (water is finite, as in 1.7.10 with GT6)"));
		} catch (Throwable e) {e.printStackTrace(ERR);}
	}

	public void onLevelLoadEarlyItemInit(net.neoforged.neoforge.event.level.LevelEvent.Load aEvent) {
		if (aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel tLevel && tLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
			applyWaterSourceConversionRule(tLevel);
			runDeferredItemInit();
			// BUG-054: the vanilla furnace's shift-click gate (RecipePropertySet.FURNACE_INPUT → AbstractFurnaceMenu.canSmelt:142)
			// is built by the engine on loadLevel BEFORE this data-init (FurnaceRecipes is still empty → GT6SmeltingDispatcher.input()
			// returns the BARRIER placeholder) → the vanilla furnace doesn't recognize GT6 smeltables, shift doesn't put them into the input slot.
			// We rebuild the propertySet AFTER FurnaceRecipes is filled: input() is now non-empty → forSingleInput(SMELTING)
			// (RecipeManager:257, a fresh input(), not the cache) collects GT6's inputs → canSmelt(GT6)=true. The SAME call the engine
			// makes on reload (MinecraftServer.java:356,1588), idempotent. Fuel (isFuel) is unaffected — it goes through
			// FurnaceFuelBurnTimeEvent, independent of the propertySet. Smelting/manual placement itself worked even before the fix (matches live-lookup).
			net.minecraft.server.MinecraftServer tServer = tLevel.getServer();
			// F4 role-C: replaces vanilla crafting-bench recipes with ore versions (in 1.7.10 Forge itself did this in
			// initVanillaEntries, the second half). Right here, because: the RecipeManager is full of the datapack, the
			// dictionary is full of vanilla (role-B at the start of the drain above) and GT6 stacks (the drain itself). Idempotent.
			gregapi.oredict.OreDictionary.initVanillaRecipeReplacements(tServer);
			// F11-recipe-scan: scans of foreign recipes (Loader_Recipes_Replace) — AFTER role-C (their input is
			// its ore versions, just as in 1.7.10 the input was Forge's replacements) and BEFORE finalizeRecipeLoading below
			// (suppressed datapack recipes must be reflected in the propertySet/recipe-book display rebuild).
			sCurrentServerForRecipeScan = tServer;
			try {for (Runnable tScan : DEFERRED_RECIPE_SCAN) try {tScan.run();} catch(Throwable e) {e.printStackTrace(ERR);} DEFERRED_RECIPE_SCAN.clear();}
			finally {sCurrentServerForRecipeScan = null;}
			// BUG-091 tail, the datapack arm of CR.remove (see CR.DATAPACK_REMOVALS): in 1.7.10 remove(...) also deleted
			// VANILLA recipes from the live CraftingManager (log→4 planks etc.); their neo descendants in the datapack
			// are suppressed here by the SAME judge as 1.7.10 — matches() against the accumulated grid — via the same
			// center removeDatapackRecipes as Replace. GT6's own GT6CraftingDispatcher is excluded (it matches on
			// the same grids — it would suppress itself).
			if (tServer != null) try {
				java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> tRemove = new java.util.HashSet<>();
				for (net.minecraft.world.item.ItemStack[] tGrid : gregapi.util.CR.DATAPACK_REMOVALS) {
					net.minecraft.world.item.crafting.CraftingInput tInput = gregapi.util.CR.crafting(tGrid);
					for (net.minecraft.world.item.crafting.RecipeHolder<?> tHolder : tServer.getRecipeManager().recipeMap().values()) {
						if (!(tHolder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe tCraft)) continue;
						if (tHolder.value() instanceof gregapi.recipes.GT6CraftingDispatcher) continue;
						try {if (tCraft.matches(tInput, tServer.overworld())) tRemove.add(tHolder.id());} catch(Throwable e) {/*a foreign recipe threw in matches — not our call*/}
					}
				}
				// The second arm of the SAME class — suppression by OUTPUT (CR.delate/CR.remout, see CR.DATAPACK_REMOVALS_OUT).
				// The judge is exactly the one 1.7.10's remout had: comparing the recipe's output against the accumulated set, NBT ignored.
				// The output is taken via assemble(EMPTY) — the same approach as role-C (OreDictionary.initVanillaRecipeReplacements).
				for (net.minecraft.world.item.ItemStack tOut : gregapi.util.CR.DATAPACK_REMOVALS_OUT) {
					for (net.minecraft.world.item.crafting.RecipeHolder<?> tHolder : tServer.getRecipeManager().recipeMap().values()) {
						if (!(tHolder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe tCraft)) continue;
						if (tHolder.value() instanceof gregapi.recipes.GT6CraftingDispatcher) continue;
						try {
							net.minecraft.world.item.ItemStack tResult = tCraft.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY);
							if (gregapi.util.ST.valid(tResult) && gregapi.util.ST.equal(tResult, tOut, T)) tRemove.add(tHolder.id());
						} catch(Throwable e) {/*a foreign recipe threw in assemble — not our call*/}
					}
				}
				// The third arm of the SAME class — suppression by recipe TYPE (CR.remoutType). The set isn't drained:
				// it describes a machine, not a one-off request, and is reapplied on every world load.
				if (!gregapi.util.CR.DATAPACK_REMOVALS_TYPE.isEmpty()) {
					for (net.minecraft.world.item.crafting.RecipeHolder<?> tHolder : tServer.getRecipeManager().recipeMap().values()) {
						net.minecraft.resources.Identifier tType = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.getKey(tHolder.value().getType());
						if (tType != null && gregapi.util.CR.DATAPACK_REMOVALS_TYPE.contains(tType.toString())) tRemove.add(tHolder.id());
					}
				}
				gregapi.util.CR.DATAPACK_REMOVALS_OUT.clear();
				gregapi.util.CR.DATAPACK_REMOVALS.clear();
				// Re-entering a singleplayer world = a NEW MinecraftServer with a fresh (full) datapack, while the scan
				// queues and DATAPACK_REMOVALS were already drained by the first start — the accumulated key set is reapplied
				// HERE on every LevelEvent.Load (idempotent; /reload is covered separately by OnDatapackSyncEvent).
				// Before this line, the suppression only lived in the session's first server — relogging returned vanilla
				// (player symptom: "log by hand gives 4 planks again"; this also affected 21 tool-recipe Replace suppressions).
				tRemove.addAll(SUPPRESSED_DATAPACK_RECIPES);
				// BUG-095 recurrence: GT6's suppression must also reach the ore VERSION of a recipe, not just its
				// datapack original. In 1.7.10 Forge's replacements (ShapedOreRecipe) and vanilla recipes lived in ONE
				// CraftingManager, and remout(output)/remove(grid) cut both in a single pass. The port split them into two
				// lists — the datapack (RecipeManager) and GT6's own buffer (CR.BUFFER) — and role-C
				// (OreDictionary.initVanillaRecipeReplacements, called above in this same method) BUILDS the ore version
				// of a vanilla recipe AFTER the loaders have already run their own suppressions: a copy of the recipe
				// GT6 just suppressed lands back in the buffer. Player symptom: the furnace crafted from a single cobblestone even though
				// the 1.7.10 original only gives it via OD.craftingFirestarter (Loader_Recipes_Vanilla:59-61,67). Both suppression
				// loops above cannot reach it BY CONSTRUCTION — they skip GT6CraftingDispatcher, which is the one
				// feeding this ore version into the crafting bench.
				// The judgment is by SOURCE, not by output: the ore version remembers the key of its datapack original
				// (mSourceId, OreDictionary:418,433). Original suppressed → ore version suppressed too; original alive →
				// the ore version remains a superset of the live recipe, exactly as role-C intends. It relies on tRemove, into which
				// the persistent SUPPRESSED_DATAPACK_RECIPES has already been merged: the suppression registries themselves are drained above, and iterating
				// over them would only be correct on the FIRST world load (the same error class the line above fixes).
				int tDroppedOre = 0;
				for (java.util.Iterator<gregapi.recipes.ICraftingRecipeGT> tIt = gregapi.util.CR.BUFFER.iterator(); tIt.hasNext();) {
					gregapi.recipes.ICraftingRecipeGT tRecipe = tIt.next();
					net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> tSource = null;
					if (tRecipe instanceof gregapi.recipes.ShapedOreRecipe tShaped && tShaped.mVanillaReplacement) tSource = tShaped.mSourceId;
					else if (tRecipe instanceof gregapi.recipes.ShapelessOreRecipe tShapeless && tShapeless.mVanillaReplacement) tSource = tShapeless.mSourceId;
					if (tSource != null && tRemove.contains(tSource)) {tIt.remove(); tDroppedOre++;}
				}
				OUT.println("GT_API: role-C ore variants dropped along with the suppressed original (BUG-095): " + tDroppedOre);
				removeDatapackRecipes(tServer, tRemove);
			} catch(Throwable e) {e.printStackTrace(ERR);}
			// BUG-054: rebuilds propertySets AFTER FurnaceRecipes is filled (the drain above) — and after the datapack-recipe
			// suppression scan (the same rebuild will collect displays without the suppressed ones). Idempotent.
			if (tServer != null) tServer.getRecipeManager().finalizeRecipeLoading(tLevel.enabledFeatures());
			// BUG-039 (F-loot, the same timing class): LootTableLoadEvent fired during resource loading BEFORE this
			// data-init (the ChestGenHooks buffer was empty) → a catch-up injection of GT pools into the already-loaded tables.
			// Idempotent (named pool); /reload and subsequent loads are covered by LootTableLoadEvent itself.
			net.minecraftforge.common.ChestGenHooks.injectAll(tServer);
		} else if (aEvent.getLevel() instanceof net.minecraft.world.level.Level tClientLevel && tClientLevel.isClientSide()) {
			// BUG-094 (dedicated server: stones/sticks/machines are transparent): for a client connected to a DEDICATED
			// server, no ServerLevel exists → the single drain above NEVER ran → all deferred item-init
			// (4297 MTE registrations, mDrops, oredict, tabs...) is empty on the client; no GT6 sync packet could
			// create a client-side BE (getRegistry(id).mRegistry.size()==0, measured by the gt6remoteprobe stand: NULL=228/228).
			// In 1.7.10 this init lived in FML phases that ran on BOTH sides — the client arm was lost when it was
			// moved to a server-only event. Singleplayer masks this: the integrated server drains the queue in the same
			// JVM BEFORE ClientLevel loads → here the queue is already empty → no-op (idempotent by an empty queue).
			// The timing is the same as the server arm: ClientLevel.Load = post-bind (registries/components bound).
			// The server-only tails (role-C, recipe-scan, propertySets, loot) stay ONLY in the server branch — a
			// remote client gets recipes/loot synced from the server.
			runDeferredItemInit();
		}
	}

	/**
	 * PreInit. Replacement for {@code @Mod.EventHandler onPreLoad(FMLPreInitializationEvent)}: subscribed in
	 * the constructor to {@link FMLConstructModEvent} (mod bus). Builds the GT6 shim {@code FMLPreInitializationEvent}
	 * (the phase carrier, gregapi.api) and passes it to {@code Abstract_Mod.onModPreInit(...)} — the phase body
	 * (onModPreInit2 and beyond) stays byte-for-byte as in the original.
	 * F12-timing (boot-confirmed: the mod boots, content registration
	 * inside PreInit works); formally relative to
	 * FMLConstructModEvent on all builds; verify on the first real registration via ITEMS/BLOCKS.
	 */
	public void onPreLoad(FMLConstructModEvent aModEvent) {
		FMLPreInitializationEvent aEvent = new FMLPreInitializationEvent(FMLPaths.CONFIGDIR.get().toFile());

		DirectoriesGT.CONFIG = aEvent.getModConfigurationDirectory();

		DirectoriesGT.CONFIG_GT = new File(DirectoriesGT.CONFIG, "GregTech");
		if (!DirectoriesGT.CONFIG_GT.exists()) DirectoriesGT.CONFIG_GT = new File(DirectoriesGT.CONFIG, "gregtech");

		DirectoriesGT.CONFIG_RECIPES = new File(DirectoriesGT.CONFIG, "Recipes");
		if (!DirectoriesGT.CONFIG_RECIPES.exists()) DirectoriesGT.CONFIG_RECIPES = new File(DirectoriesGT.CONFIG, "recipes");

		DirectoriesGT.MINECRAFT = DirectoriesGT.CONFIG.getParentFile();

		DirectoriesGT.LOGS = new File(DirectoriesGT.MINECRAFT, "logs");

		onModPreInit(aEvent);
	}

	/**
	 * Init. Replacement for {@code @Mod.EventHandler onLoad(FMLInitializationEvent)}: subscribed in the constructor to
	 * {@link FMLCommonSetupEvent} (mod bus).
	 */
	public void onLoad(FMLCommonSetupEvent aModEvent) {
		// F1/F12/F16 boot-timing: ore-targets + recipes create an ItemStack (ST.make(Blocks/Items)) — onLoad(CommonSetup) is NOT
		// post-bind (Holder.components is bound by ReloadableServerResources on server-start). Wrapped in deferItemInit →
		// runs in onModServerStarting2 (post-bind). NOT in parity data (ore-targets/recipes != material/prefix scalar).
		deferItemInit(() -> {
		// vanilla ore-targets. The "registered first" order is preserved (the first deferred entry).
		// It is VERY important that those are registered first. Otherwise GregTech would output its own Storage Blocks.
		// F12: REMAP-RULES.md §C/§C-bis block-flatten (data, not behavior) — Blocks.<snake_case> removed,
		// replaced with neo's real UPPER_SNAKE constants; RedSand and "smooth double stone slab" (meta 8) → RED_SAND/SMOOTH_STONE.
		OreDictManager.INSTANCE.setTarget_(OP.blockDust , MT.Stone     , ST.make(Blocks.GRAVEL           , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockDust , MT.SoulSand  , ST.make(Blocks.SOUL_SAND        , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockDust , MT.Sand      , ST.make(Blocks.SAND             , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockDust , MT.RedSand   , ST.make(Blocks.RED_SAND         , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockSolid, MT.Sand      , ST.make(Blocks.SANDSTONE        , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockSolid, MT.Glass     , ST.make(Blocks.GLASS            , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockSolid, MT.Stone     , ST.make(Blocks.SMOOTH_STONE     , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockSolid, MT.Netherrack, ST.make(Blocks.NETHERRACK       , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockSolid, MT.Endstone  , ST.make(Blocks.END_STONE        , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockSolid, MT.Obsidian  , ST.make(Blocks.OBSIDIAN         , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockIngot, MT.Fe        , ST.make(Blocks.IRON_BLOCK       , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockIngot, MT.Au        , ST.make(Blocks.GOLD_BLOCK       , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockGem  , MT.Diamond   , ST.make(Blocks.DIAMOND_BLOCK    , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockGem  , MT.Emerald   , ST.make(Blocks.EMERALD_BLOCK    , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockGem  , MT.Lapis     , ST.make(Blocks.LAPIS_BLOCK      , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockGem  , MT.Coal      , ST.make(Blocks.COAL_BLOCK       , 1, 0), T, F, T);
		OreDictManager.INSTANCE.setTarget_(OP.blockDust , MT.Redstone  , ST.make(Blocks.REDSTONE_BLOCK   , 1, 0), T, F, T);

		// F12 boot-timing (MOVED from onModPreInit2): recipe fixes create an ItemStack (ST.make) — impossible in preInit
		// (Holder.components not bound); possible here (after registration+binding). Fixing vanilla Oak Plank Slab Recipe.
		CR.remove(ST.make(Blocks.OAK_PLANKS, 1, 0), ST.make(Blocks.SPRUCE_PLANKS, 1, 0), ST.make(Blocks.BIRCH_PLANKS, 1, 0));
		CR.shaped(ST.make(Blocks.OAK_SLAB, 6, 0), CR.NONE, "WWW", 'W', ST.make(Blocks.OAK_PLANKS, 1, 0));
		// Preventing a Water Dupe by registering this Recipe early so it won't be overridden
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.GLASS_BOTTLE, 1, 0), FL.Water.make(250), NF, ST.make(Items.POTION, 1, 0));
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.POTION, 1, 0), ST.make(Items.GLASS_BOTTLE, 1, 0));
		}); // end of the deferItemInit wrapper for ore-targets/recipes (runs @onModServerStarting2, post-bind)

		for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_ARRAY) if (tMaterial != null && !tMaterial.contains(TD.Properties.INVALID_MATERIAL)) {
			tMaterial.mOreProcessingMultiplier = UT.Code.bindStack(ConfigsGT.OREPROCESSING.get(ConfigCategories.Materials.oreprocessingoutputmultiplier, tMaterial.mNameInternal, 1));
			tMaterial.mOreMultiplier = (byte)ConfigsGT.MATERIAL.get(tMaterial.mNameInternal, "MultiplierOre", tMaterial.mOreMultiplier);
			tMaterial.mToolQuality = (byte)ConfigsGT.MATERIAL.get(tMaterial.mNameInternal, "ToolQuality", tMaterial.mToolQuality);
			if (tMaterial.mToolTypes > 0) {
				tMaterial.mToolSpeed = (float)ConfigsGT.MATERIAL.get(tMaterial.mNameInternal, "ToolSpeed", tMaterial.mToolSpeed);
				tMaterial.mToolDurability = ConfigsGT.MATERIAL.get(tMaterial.mNameInternal, "ToolDurability", tMaterial.mToolDurability);
				tMaterial.mHandleMaterial = OreDictMaterial.get(ConfigsGT.MATERIAL.get(tMaterial.mNameInternal, "ToolHandle", tMaterial.mHandleMaterial.mNameInternal));
			}
		}
		onModInit(new FMLInitializationEvent());
		// F1/F12/F16 item-model: runDeferredItemInit is MOVED to onModServerStarting2 — onLoad(CommonSetup) is NOT post-bind
		// (verified: Holder.components is bound by ReloadableServerResources on server-start, Holder.java:108).
	}

	// PostInit: subscribed in the constructor to FMLLoadCompleteEvent (mod bus) — neo's native event
	// replaces the old FML 1.7.10 complexity around loadComplete (the oracle comment above was removed along
	// with the @Mod.EventHandler dispatcher, which was the source of the problem).
	public void onPostLoad(FMLLoadCompleteEvent aModEvent) {onModPostInit(new FMLPostInitializationEvent());}

	@Override public String getModID() {return MD.GAPI.mID;}
	@Override public String getModName() {return MD.GAPI.mName;}
	@Override public String getModNameForLog() {return "GT_API";}
	@Override public Abstract_Proxy getProxy() {return api_proxy;}

	// Server phases — subscribed in the constructor to NeoForge.EVENT_BUS (game bus), not the mod bus.
	public void onServerStarting  (ServerStartingEvent aEvent) {
		// LOCALIZATION CENTER (BUG-082), SERVER ARM. In 1.7.10 name injection lived in shared code
		// (LanguageRegistry.injectLanguage — both sides), so server-side strings were human-readable too.
		// The client arm hangs off resource loading (GT_API_Proxy_Client), which doesn't exist on a dedicated server —
		// here the same overlay is applied over the server table. Details — LanguageHandler.injectIntoEngine().
		int tInjected = gregapi.lang.LanguageHandler.injectIntoEngine();
		if (tInjected > 0) OUT.println("GT6 localization: GT6 names appended to the engine table (server): " + tInjected);
		onModServerStarting(aEvent);
	}
	public void onServerStarted   (ServerStartedEvent  aEvent) {onModServerStarted(aEvent);}
	public void onServerStopping  (ServerStoppingEvent aEvent) {onModServerStopping(aEvent);}
	public void onServerStopped   (ServerStoppedEvent  aEvent) {onModServerStopped(aEvent);}

	@Override
	@SuppressWarnings({ "resource", "deprecation" })
	public void onModPreInit2(FMLPreInitializationEvent aEvent) {
		// The neo signature was checked against fml-decompiled/net/neoforged/fml/InterModComms.java:27
		// (decisions/F12-registration-lifecycle.md §7 — question closed).
		InterModComms.sendTo(MD.GT.mID, "carbonconfig", "remapGui", () -> MD.GAPI.mID);

		File
		tFile = new File(DirectoriesGT.CONFIG_GT, "IDs.cfg");
		if (!tFile.exists()) tFile = new File(DirectoriesGT.CONFIG_GT, "ids.cfg");
		Config.sConfigFileIDs = new ModConfigSpec(tFile); Config.sConfigFileIDs.save();

		ConfigsGT.GREGTECH      = new Config("GregTech.cfg").setUseDefaultInNames(F);
		ConfigsGT.RECIPES       = new Config("Recipes.cfg");
		ConfigsGT.WORLDGEN      = new Config("WorldGenerationNew.cfg");
		ConfigsGT.WORLDGEN_GT5  = new Config("old_barely_used_gt5_style_garbage_worldgen.cfg");
		ConfigsGT.MATERIAL      = new Config("Materials.cfg");
		ConfigsGT.OREPROCESSING = new Config("OreProcessing.cfg");
		// Deprecated Config Files.
		ConfigsGT.OVERPOWERED = ConfigsGT.MACHINES = ConfigsGT.SPECIAL = ConfigsGT.GREGTECH;
		
		
		tFile = new File(DirectoriesGT.CONFIG_GT, "Stacksizes.cfg");
		if (!tFile.exists()) tFile = new File(DirectoriesGT.CONFIG_GT, "stacksizes.cfg");
		ModConfigSpec tStackConfig = new ModConfigSpec(tFile);

		tFile = new File(DirectoriesGT.LOGS, "gregtech.log");
		if (!tFile.exists()) try {tFile.createNewFile();} catch(Throwable e) {/**/}
		
		List<String>
		tList = ((LogBuffer)OUT).mBufferedLog;
		try {
			OUT = new PrintStream(tFile);
		} catch (Throwable e) {
			OUT = System.out;
		}
		
		for (String tString : tList) OUT.println(tString);
		
		if (ConfigsGT.GREGTECH.get("general", "LoggingErrors", T)) {
			tList = ((LogBuffer)ERR).mBufferedLog;
			ERR = OUT;
			for (String tString : tList) ERR.println(tString);
		} else {
			OUT.println("**********************************************************************");
			OUT.println("* WARNING: ERROR LOGGING HAS BEEN DISABLED FOR THIS LOG FILE         *");
			OUT.println("**********************************************************************");
		}
		
		tFile = new File(DirectoriesGT.CONFIG_GT, "materiallist.log");
		if (!tFile.exists()) {try {tFile.createNewFile();} catch (Throwable e) {/**/}}
		try {
			MAT_LOG = new PrintStream(tFile);
			MAT_LOG.println("**********************************************************************");
			MAT_LOG.println("* This is the complete List of usable GregTech Materials             *");
			MAT_LOG.println("**********************************************************************");
		// SILENCING REMOVED, SURGICALLY. Bug class: "a mod subsystem's init fails and nobody finds out" — this
		// exact class hid the dead player-activity log for two months (see below). Here the silence
		// hides undone work: MAT_LOG stays a buffer, and there will be no material list at all.
		// Reported through the mod's existing center (ERR — the same one the mod uses to report all its failures),
		// no dedicated mechanism added. The author's style is untouched: empty catches with a visible
		// consequence behind them (createNewFile — the PrintStream below will create the file anyway) are left as-is.
		} catch (Throwable e) {ERR.println("GT_API: material list (materiallist.log) not opened — the file will not appear"); e.printStackTrace(ERR);}
		
		tFile = new File(DirectoriesGT.LOGS, "oredict.log");
		if (!tFile.exists()) {try {tFile.createNewFile();} catch (Throwable e) {/**/}}
		try {
			tList = ((LogBuffer)ORD).mBufferedLog;
			ORD = new PrintStream(tFile);
			ORD.println("**********************************************************************");
			ORD.println("* This is the complete Log of the GregTech OreDictionary Handler     *");
			ORD.println("**********************************************************************");
			for (String tString : tList) ORD.println(tString);
		} catch (Throwable e) {ERR.println("GT_API: ore dictionary log (oredict.log) not opened — dictionary entries are lost"); e.printStackTrace(ERR);}
		
		if (ConfigsGT.GREGTECH.get("general", "LoggingPlayerActivity", !CODE_CLIENT)) {
			tFile = new File(DirectoriesGT.LOGS, "playeractivity_"+(System.currentTimeMillis()/60000)+".log");
			if (!tFile.exists()) {try {tFile.createNewFile();} catch (Throwable e) {/**/}}
			// THE INSTANCE THAT LED TO FINDING THE BUG CLASS: here a bus exception was silently swallowed
			// ("Cannot register listeners for abstract ..."), mPlayerLogger stayed null, and the player-activity
			// log never wrote a single line — with a live config and a created file.
			try {mPlayerLogger = new LoggerPlayerActivity(new PrintStream(tFile));} catch (Throwable e) {ERR.println("GT_API: player activity log not created — player actions will not be recorded"); e.printStackTrace(ERR);}
		}
		
		ConfigsGT.CLIENT = new Config(DirectoriesGT.MINECRAFT, "GregTech.cfg");
		
		D1                        = ConfigsGT.CLIENT.get(ConfigCategories.debug  , "logs"               , F);
		D2                        = ConfigsGT.CLIENT.get(ConfigCategories.debug  , "oredict"            , F);
		D3                        = ConfigsGT.CLIENT.get(ConfigCategories.debug  , "misc"               , F);
		EXPERIMENTS               = ConfigsGT.CLIENT.get(ConfigCategories.debug  , "experiments"        , F);
		CLIENT_BLOCKUPDATE_SOUNDS = ConfigsGT.CLIENT.get(ConfigCategories.debug  , "block_update_sounds", F);
		if ( ConfigsGT.CLIENT.get(ConfigCategories.debug, "april_fools"  , F)) APRIL_FOOLS = T;
		if ( ConfigsGT.CLIENT.get(ConfigCategories.debug, "xmas_july"    , F)) XMAS_IN_JULY = T;
		if ( ConfigsGT.CLIENT.get(ConfigCategories.debug, "xmas_december", F)) XMAS_IN_DECEMBER = T;
		
		if (APRIL_FOOLS) {
			MT.W.setLocal("Wolframium");
			MT.V.setLocal("Vandalium");
			MT.B.setLocal("Boring");
			MT.S.setLocal("Sulphur");
			MT.K.setLocal("Kalium");
			MT.Na.setLocal("Natrium");
			MT.Ar.setLocal("Aragon");
			MT.Al.setLocal("Aluminum");
			MT.Ni.setLocal("Ferrous Metal");
			MT.Pt.setLocal("Shiny Metal");
			MT.Mithril.setLocal("Mana Infused Metal");
			MT.Hg.setLocal("Quicksilver");
			MT.Mo.setLocal("Molly-B");
			MT.Sb.setLocal("Anti-Money");
			MT.Tc.setLocal("Gregorium");
			MT.Si.setLocal("Silicone");
			MT.Cr.setLocal("Firefox");
			MT.Cu.setLocal("Cooper");
			MT.AnnealedCopper.setLocal("Anilled Cooper");
			MT.Mg.setLocal("Manganesium");
			MT.Mn.setLocal("Animenese");
			MT.As.setLocal("Arse Nick");
			MT.Br.setLocal("Bro, that's mine");
			MT.Kr.setLocal("Kryptonite");
			MT.Bi.setLocal("Biffmiff");
			MT.Sg.setLocal("Resistance is Futile");
			MT.Zr.setLocal("Diamond");
			MT.Au.setLocal("Pyrite");
			MT.Pyrite.setLocal("Gold");
			MT.Fe.setLocal("Irun");
			MT.IronWood.setLocal("Irunwood");
			MT.ShadowIron.setLocal("Shade Irun");
			MT.DarkIron.setLocal("Dank Irun");
			MT.MeteoricIron.setLocal("Metaur Irun");
			MT.GildedIron.setLocal("Guild Irun");
			MT.WroughtIron.setLocal("Wrecked Irun");
			MT.Steel.setLocal("Style");
			MT.RedSteel.setLocal("Rad Style");
			MT.BlueSteel.setLocal("Blu Style");
			MT.BlackSteel.setLocal("Afro Style"); // the original Joke got cancelled, but since I got a big ball of Hair on my head as of the time of writing this (thanks pandemic), it is perfectly acceptable.
			MT.MeteoricSteel.setLocal("Metaur Style");
			MT.MeteoricRedSteel.setLocal("Metaur Rad Style");
			MT.MeteoricBlueSteel.setLocal("Metaur Blu Style");
			MT.MeteoricBlackSteel.setLocal("Metaur Afro Style"); // the original Joke got cancelled, but since I got a big ball of Hair on my head as of the time of writing this (thanks pandemic), it is perfectly acceptable.
			MT.DamascusSteel.setLocal("Dank Style");
			MT.VanadiumSteel.setLocal("Vandalium Style");
			MT.TungstenSteel.setLocal("Wolf Style");
			MT.ShadowSteel.setLocal("Shade Style");
			MT.Steeleaf.setLocal("Style Leave");
			MT.Fireleaf.setLocal("Burn Leave");
			MT.Knightmetal.setLocal("Night Metal");
			MT.FierySteel.setLocal("Fury Style");
			MT.SteelGalvanized.setLocal("Galvanized Square Steel");
			MT.Thaumium.setLocal("Thaumanominum");
			MT.DarkThaumium.setLocal("Dank Thaumanominum");
			MT.VoidMetal.setLocal("Warranty Void Metal");
			MT.Coal.setLocal("Cool");
			MT.Charcoal.setLocal("Charred Cole");
			MT.Lapis.setLocal("Le Piss");
			MT.Redstone.setLocal("Blingstone");
			MT.Glowstone.setLocal("Klostein");
			MT.Emerald.setLocal("Chaos Emerald");
			MT.Craponite.setLocal("Pink Diamond");
			MT.Diamond.setLocal("Sapphire");
			MT.DiamondPink.setLocal("Craponite");
			MT.Bedrock.setLocal("Sofarock");
			MT.Plastic.setLocal("LEGO");
			MT.Teflon.setLocal("Polytetrafluoroethylene");
			MT.Asbestos.setLocal("Bestos");
			MT.AncientDebris.setLocal("Cinnabun");
			MT.Cinnamon.setLocal("Ancient Debris");
			MT.Wheat.setLocal("Gluten");
			MT.Milk.setLocal("Lactose");
			MT.WOODS.Acacia.setLocal("A Cha Cha");
			MT.WOODS.DarkOak.setLocal("Dork Oak");
			MT.WOODS.Darkwood.setLocal("Dork Wood");
			MT.WOODS.Cinnamon.setLocal("Ancient Debris");
			MT.WOODS.Foxfire.setLocal("Chrome");
			MT.Rb.setLocal("Ruby");
			MT.Ruby.setLocal("Red Sapphire");
			MT.KCl.setLocal("Sylveonite");
			MT.KNO3.setLocal("Niter");
			MT.NaNO3.setLocal("Nitre");
			MT.Glyceryl.setLocal("Nitro");
			MT.Gunpowder.setLocal("Crossbow Powder");
			MT.Lubricant.setLocal("Lube");
			MT.H2SO4.setLocal("Sulphuric Acid");
			MT.H2S2O7.setLocal("Disulphuric Acid");
			MT.STONES.Greenschist.setLocal("Green Shit");
			MT.STONES.Blueschist.setLocal("Blue Shit");
			MT.Nikolite.setLocal("Bluestone");
			MT.PigIron.setLocal("Ferrobacon");
			MT.TinAlloy.setLocal("Tin*");
			MT.Bronze.setLocal("Tinkerers Alloy");
			MT.ArsenicCopper.setLocal("Arsenine Alloy");
			MT.ArsenicBronze.setLocal("Arsenine Tinkerers Alloy");
			MT.BismuthBronze.setLocal("Biffmiff Tinkerers Alloy");
			MT.BlackBronze.setLocal("Afro Tinkerers Alloy"); // the original Joke got cancelled, but since I got a big ball of Hair on my head as of the time of writing this (thanks pandemic), it is perfectly acceptable.
			MT.Constantan.setLocal("Cupronickel");
			MT.Ge.setLocal("Platosmium");
			MT.Amazonite.setLocal("Bezosite");
			MT.NetherQuartz.setLocal("Weather Quartz");
			MT.MilkyQuartz.setLocal("Milk Quartz");
			MT.CertusQuartz.setLocal("Citrus Quartz");
			MT.ChargedCertusQuartz.setLocal("Charged Citrus Quartz");
			MT.Firestone.setLocal("Hot Garbage");
			MT.UUMatter.setLocal("UwU-Matter");
			MT.UUAmplifier.setLocal("UwU-Amplifier");
			MT.OREMATS.Galena.setLocal("Silverlead");
			MT.OREMATS.Huebnerite.setLocal("Boobnerite");
			MT.OREMATS.Bromargyrite.setLocal("Bromagnerite");
			MT.OREMATS.Chalcopyrite.setLocal("Chackapackerite");
			
			for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_MAP.values()) if (tMaterial.mNameLocal.toLowerCase().contains("wood")) tMaterial.setLocal(tMaterial.mNameLocal + " >:] nice");
		}
		
		if (D1) {
			tList = ((LogBuffer)DEB).mBufferedLog;
			DEB = OUT;
			for (String tString : tList) DEB.println(tString);
		}
		
		
		for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) if (!tPrefix.contains(TD.Prefix.PREFIX_UNUSED)) {
			tPrefix.setConfigStacksize(tStackConfig.get("stacksizes", tPrefix.mNameInternal+"_"+tPrefix.mDefaultStackSize, tPrefix.mDefaultStackSize).getInt());
		}
		tStackConfig.save();

		SURVIVAL_INTO_ADVENTURE_MODE            = ConfigsGT.GREGTECH.get("general", "forceAdventureMode"               , F);
		ADVENTURE_MODE_KIT                      = ConfigsGT.GREGTECH.get("general", "AdventureModeStartingKit"         , !MD.GT.mLoaded);
		HUNGER_BY_INVENTORY_WEIGHT              = ConfigsGT.GREGTECH.get("general", "AFK_Hunger"                       ,  MD.GT.mLoaded);
		TOOL_BREAK_FATIQUE                      = ConfigsGT.GREGTECH.get("general", "ToolBreakFatique"                 , T);
		INVENTORY_UNIFICATION                   = ConfigsGT.GREGTECH.get("general", "InventoryUnification"             , T);
		XP_ORB_COMBINING                        = ConfigsGT.GREGTECH.get("general", "XP_Orb_Combining"                 , T);
		CONFIG_HARDNESS_MULTIPLIER_SAND         = ConfigsGT.GREGTECH.get("general", "HardnessMultiplier_Sand"          , 1);
		CONFIG_HARDNESS_MULTIPLIER_ROCK         = ConfigsGT.GREGTECH.get("general", "HardnessMultiplier_Rock"          , 1);
		CONFIG_HARDNESS_MULTIPLIER_ORES         = ConfigsGT.GREGTECH.get("general", "HardnessMultiplier_Ores"          , 1);
		ITEM_DESPAWN_TIME                       = ConfigsGT.GREGTECH.get("general", "ItemDespawnTime"                  ,6000);
		TREE_GROWTH_TIME                        = ConfigsGT.GREGTECH.get("general", "Tree_Growth_Time"                 , 1);
		ENTITY_CRAMMING                         = ConfigsGT.GREGTECH.get("general", "MaxEqualEntitiesAtOneSpot"        , 3);
		DRINKS_ALWAYS_DRINKABLE                 = ConfigsGT.GREGTECH.get("general", "drinks_always_drinkable"          , F);
		// E5: default raised F -> T ABOVE strict 1:1, by decision of the AE2 layer. The 1.7.10 original has F here
		// (gregtech6/src/main/java/gregapi/GT_API.java, the same key general/Emit_EU_as_RF_from_Blocks), and this
		// is deliberately not reproduced: AE2 generators are suppressed by default by the DisableAllEnergyGeneratorRecipes node,
		// meaning with F the AE2 network is left with no power source out of the box — a contradiction within the layer.
		// Default T opens the bridge "GT6 is the sole power source for the assembly". The switch is in place: an assembler
		// who wants the original behavior sets F and gets exactly that.
		EMIT_EU_AS_RF                           = ConfigsGT.GREGTECH.get("general", "Emit_EU_as_RF_from_Blocks"        , T);
		NERFED_WOOD                             = ConfigsGT.GREGTECH.get("general", "WoodNeedsSawForCrafting"          , T);
		FORCE_GRAVEL_NO_FLINT                   = ConfigsGT.GREGTECH.get("general", "GravelWontDropFlint"              , F);
		WATER_SOURCE_CONVERSION                 = ConfigsGT.GREGTECH.get("general", "WaterSourceConversion"            , F);
		SLOW_LEAF_DECAY                         = ConfigsGT.GREGTECH.get("general", "SlowLeafDecay"                    , F);
		FAST_LEAF_DECAY                         = ConfigsGT.GREGTECH.get("general", "FastLeafDecay"                    , T);
		CONSTANT_ENERGY                         = ConfigsGT.GREGTECH.get("general", "UninterruptedEnergyRequirement"   , T);
		FOOD_OVERDOSE_DEATH                     = ConfigsGT.GREGTECH.get("general", "DeathByOverdosingCertainFoods"    , T);
		NUTRITION_SYSTEM                        = ConfigsGT.GREGTECH.get("general", "NutritionSystem"                  , T);
		OBSTRUCTION_CHECKS                      = ConfigsGT.GREGTECH.get("general", "ObstructionChecks"                , T);
		OWNERSHIP_RESET                         = ConfigsGT.GREGTECH.get("general", "ResetPlayerOwnershipOfGT6Blocks"  , F);
		SPAWN_ZONE_MOB_PROTECTION               = ConfigsGT.GREGTECH.get("general", "PreventMobSpawnsCloseToSpawn"     , T);
		SPAWN_NO_BATS                           = ConfigsGT.GREGTECH.get("general", "PreventBatSpawnsOnNonVanillaStone", T);
		SPAWN_HOSTILES_ONLY_IN_DARKNESS         = ConfigsGT.GREGTECH.get("general", "PreventMobSpawnsAboveLightLevel0" , T);
		DISABLE_GT6_CRAFTING_RECIPES            = ConfigsGT.GREGTECH.get("general", "DisableGT6CraftingRecipesDEBUG"   , F);
		MOBS_DROP_LEAD                          = ConfigsGT.GREGTECH.get("general", "mobs_drop_lead_instead_of_iron"   , T);
		MOBS_DROP_MEAT                          = ConfigsGT.GREGTECH.get("general", "mobs_drop_variety_meats"          , T);
		MOBS_DROP_JUNK                          = ConfigsGT.GREGTECH.get("general", "mobs_drop_random_junk"            , T);
		MOBS_DROP_BOOK                          = ConfigsGT.GREGTECH.get("general", "mobs_drop_books_and_manuals"      , T);
		MOBS_DROP_NAME                          = ConfigsGT.GREGTECH.get("general", "mobs_drop_nametags_when_named"    , T);
		ZOMBIES_DIG_WITH_TOOLS                  = ConfigsGT.GREGTECH.get("general", "Zombies_Dig_With_Tools"           , F);
		ZOMBIES_DIG_TILEENTITIES                = ConfigsGT.GREGTECH.get("general", "Zombies_Dig_TileEntities"         , F);
		ZOMBIES_HOLD_PICKAXES                   = ConfigsGT.GREGTECH.get("general", "Zombies_Hold_Pickaxes"            , F);
		ZOMBIES_HOLD_TNT                        = ConfigsGT.GREGTECH.get("general", "Zombies_Hold_TNT"                 , F);
		ZOMBIES_IGNITE_HELD_TNT                 = ConfigsGT.GREGTECH.get("general", "Zombies_Ignite_Held_TNT"          , F);
		
		ENABLE_ADDING_IC2_MACERATOR_RECIPES     = ConfigsGT.GREGTECH.get("ic2", "EnableAddingMaceratorRecipes"         , T);
		ENABLE_ADDING_IC2_EXTRACTOR_RECIPES     = ConfigsGT.GREGTECH.get("ic2", "EnableAddingExtractorRecipes"         , T);
		ENABLE_ADDING_IC2_COMPRESSOR_RECIPES    = ConfigsGT.GREGTECH.get("ic2", "EnableAddingCompressorRecipes"        , T);
		ENABLE_ADDING_IC2_OREWASHER_RECIPES     = ConfigsGT.GREGTECH.get("ic2", "EnableAddingOreWasherRecipes"         , T);
		ENABLE_ADDING_IC2_CENTRIFUGE_RECIPES    = ConfigsGT.GREGTECH.get("ic2", "EnableAddingThermalCentrifugeRecipes" , T);
		
		if (!ConfigsGT.GREGTECH.get("general", "UseTFCAttackMultiplierWhenLoaded" , T) || TFC_DAMAGE_MULTIPLIER < 1 || (!MD.TFC.mLoaded && !MD.TFCP.mLoaded)) TFC_DAMAGE_MULTIPLIER = 1;
		
		if (MD.IC2C.mLoaded) {
		DISABLE_ALL_IC2_MACERATOR_RECIPES       = F;
		ENABLE_ADDING_IC2_MACERATOR_RECIPES     = T;
		DISABLE_ALL_IC2_EXTRACTOR_RECIPES       = F;
		ENABLE_ADDING_IC2_EXTRACTOR_RECIPES     = T;
		DISABLE_ALL_IC2_COMPRESSOR_RECIPES      = F;
		ENABLE_ADDING_IC2_COMPRESSOR_RECIPES    = T;
		DISABLE_ALL_IC2_OREWASHER_RECIPES       = F;
		ENABLE_ADDING_IC2_OREWASHER_RECIPES     = F;
		DISABLE_ALL_IC2_CENTRIFUGE_RECIPES      = F;
		ENABLE_ADDING_IC2_CENTRIFUGE_RECIPES    = F;
		} else if (MD.IC2.mLoaded) {
		DISABLE_ALL_IC2_MACERATOR_RECIPES       = ConfigsGT.GREGTECH.get("ic2", "DisableAllMaceratorRecipes"           , F);
		if (DISABLE_ALL_IC2_MACERATOR_RECIPES) ENABLE_ADDING_IC2_MACERATOR_RECIPES = F;
		DISABLE_ALL_IC2_EXTRACTOR_RECIPES       = ConfigsGT.GREGTECH.get("ic2", "DisableAllExtractorRecipes"           , F);
		if (DISABLE_ALL_IC2_EXTRACTOR_RECIPES) ENABLE_ADDING_IC2_EXTRACTOR_RECIPES = F;
		DISABLE_ALL_IC2_COMPRESSOR_RECIPES      = ConfigsGT.GREGTECH.get("ic2", "DisableAllCompressorRecipes"          , F);
		if (DISABLE_ALL_IC2_COMPRESSOR_RECIPES) ENABLE_ADDING_IC2_COMPRESSOR_RECIPES = F;
		DISABLE_ALL_IC2_OREWASHER_RECIPES       = ConfigsGT.GREGTECH.get("ic2", "DisableAllOreWasherRecipes"           , F);
		if (DISABLE_ALL_IC2_OREWASHER_RECIPES) ENABLE_ADDING_IC2_OREWASHER_RECIPES = F;
		DISABLE_ALL_IC2_CENTRIFUGE_RECIPES      = ConfigsGT.GREGTECH.get("ic2", "DisableAllThermalCentrifugeRecipes"   , F);
		if (DISABLE_ALL_IC2_CENTRIFUGE_RECIPES) ENABLE_ADDING_IC2_CENTRIFUGE_RECIPES = F;
		} else {
		DISABLE_ALL_IC2_MACERATOR_RECIPES       = F;
		ENABLE_ADDING_IC2_MACERATOR_RECIPES     = F;
		DISABLE_ALL_IC2_EXTRACTOR_RECIPES       = F;
		ENABLE_ADDING_IC2_EXTRACTOR_RECIPES     = F;
		DISABLE_ALL_IC2_COMPRESSOR_RECIPES      = F;
		ENABLE_ADDING_IC2_COMPRESSOR_RECIPES    = F;
		DISABLE_ALL_IC2_OREWASHER_RECIPES       = F;
		ENABLE_ADDING_IC2_OREWASHER_RECIPES     = F;
		DISABLE_ALL_IC2_CENTRIFUGE_RECIPES      = F;
		ENABLE_ADDING_IC2_CENTRIFUGE_RECIPES    = F;
		}

		// ADAPT-019, the same approach as the ic2 flags above: the key is read ONLY with AE2 live (without it, nothing
		// is written to the config), without AE2 the flag is always T — no meteorites, the meteoric-iron vein is needed (five MT alloys).
		// Consumers of the flag: GT_API.onAddPackFinders (the meteorite-suppression pack) and Loader_Worldgen (the vein).
		AE2_REPLACE_METEORITE_GENERATION        = !MD.AE.mLoaded || ConfigsGT.GREGTECH.get("ae2", "ReplaceMeteoriteGeneration", T);
		AE2_KILL_QUARTZ_TOOLS                   = !MD.AE.mLoaded || ConfigsGT.GREGTECH.get("ae2", "DisableAllQuartzToolRecipes", T);

		if (ConfigsGT.GREGTECH.get("general", "disable_STDOUT"             , F)) System.out.close();
		if (ConfigsGT.GREGTECH.get("general", "disable_STDERR"             , F)) System.err.close();
		// F12: 1.7.10's Blocks.mob_spawner.setHardness(500)/setResistance(6000000) — a runtime mutation of a vanilla block (neo's Properties
		// are immutable). We cache the config flags; hardness is applied via PlayerEvent.BreakSpeed (GT_API_Proxy.onBlockBreakSpeedEvent,
		// SPAWNER → speed×0.01 = 5/500), blast-resistance via ExplosionEvent (GT_API_Proxy, SPAWNER is excluded from destructible blocks).
		HARDER_MOB_SPAWNERS          = ConfigsGT.GREGTECH.get("general", "hardermobspawners"          , T);
		BLAST_RESISTANT_MOB_SPAWNERS = ConfigsGT.GREGTECH.get("general", "blastresistantmobspawners"  , T);

		// ADAPT-005 (a new addition, ADAPTATIONS.md): light level of burning furnace-type machines; 0 = off (strict 1:1), clamped 0-15.
		BURNING_BOX_LIGHT_VALUE             = UT.Code.bind4(ConfigsGT.GREGTECH.get("machines", "burning_box_light_value", 13));
		FIRE_EXPLOSIONS                     = ConfigsGT.GREGTECH.get("machines", "explode_by_fire"    , T);
		RAIN_EXPLOSIONS                     = ConfigsGT.GREGTECH.get("machines", "explode_by_rain"    , T);
		WATER_EXPLOSIONS                    = ConfigsGT.GREGTECH.get("machines", "explode_by_water"   , T);
		THUNDER_EXPLOSIONS                  = ConfigsGT.GREGTECH.get("machines", "explode_by_thunder" , T);
		OVERCHARGE_EXPLOSIONS               = ConfigsGT.GREGTECH.get("machines", "explode_by_overload", F);
		FIRE_BREAKING                       = ConfigsGT.GREGTECH.get("machines", "break_by_fire"      , T);
		RAIN_BREAKING                       = ConfigsGT.GREGTECH.get("machines", "break_by_rain"      , T);
		WATER_BREAKING                      = ConfigsGT.GREGTECH.get("machines", "break_by_water"     , T);
		THUNDER_BREAKING                    = ConfigsGT.GREGTECH.get("machines", "break_by_thunder"   , T);
		OVERCHARGE_BREAKING                 = ConfigsGT.GREGTECH.get("machines", "break_by_overload"  , F);
		
		if (FIRE_EXPLOSIONS      ) FIRE_BREAKING       = T;
		if (RAIN_EXPLOSIONS      ) RAIN_BREAKING       = T;
		if (WATER_EXPLOSIONS     ) WATER_BREAKING      = T;
		if (THUNDER_EXPLOSIONS   ) THUNDER_BREAKING    = T;
		if (OVERCHARGE_EXPLOSIONS) OVERCHARGE_BREAKING = T;
		
		if (CONFIG_HARDNESS_MULTIPLIER_SAND <= 0.0) CONFIG_HARDNESS_MULTIPLIER_SAND = 1.0;
		if (CONFIG_HARDNESS_MULTIPLIER_ROCK <= 0.0) CONFIG_HARDNESS_MULTIPLIER_ROCK = 1.0;
		if (CONFIG_HARDNESS_MULTIPLIER_ORES <= 0.0) CONFIG_HARDNESS_MULTIPLIER_ORES = 1.0;
		
		HARDNESS_MULTIPLIER_SAND = CONFIG_HARDNESS_MULTIPLIER_SAND;
		HARDNESS_MULTIPLIER_ROCK = CONFIG_HARDNESS_MULTIPLIER_ROCK;
		HARDNESS_MULTIPLIER_ORES = CONFIG_HARDNESS_MULTIPLIER_ORES;
		
		if (ConfigsGT.GREGTECH.get("compat", "IC2Classic"          , T)) ICompat.COMPAT_CLASSES.add(                   (ICompat          )UT.Reflection.callConstructor("gregapi.compat.industrialcraft.CompatIC2C"      , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "IC2EnergyItems"      , T)) ICompat.COMPAT_CLASSES.add(COMPAT_EU_ITEM   = (ICompatIC2EUItem )UT.Reflection.callConstructor("gregapi.compat.industrialcraft.CompatIC2EUItem" , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "IndustrialCraft2"    , T)) ICompat.COMPAT_CLASSES.add(COMPAT_IC2       = (ICompatIC2       )UT.Reflection.callConstructor("gregapi.compat.industrialcraft.CompatIC2"       , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "ThaumCraft"          , T)) ICompat.COMPAT_CLASSES.add(COMPAT_TC        = (ICompatTC        )UT.Reflection.callConstructor("gregapi.compat.thaumcraft.CompatTC"             , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "BuildCraft"          , T)) ICompat.COMPAT_CLASSES.add(COMPAT_BC        = (ICompatBC        )UT.Reflection.callConstructor("gregapi.compat.buildcraft.CompatBC"             , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "ComputerCraft"       , T)) ICompat.COMPAT_CLASSES.add(COMPAT_CC        = (ICompatCC        )UT.Reflection.callConstructor("gregapi.compat.computercraft.CompatCC"          , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "OpenComputers"       , T)) ICompat.COMPAT_CLASSES.add(COMPAT_OC        = (ICompatOC        )UT.Reflection.callConstructor("gregapi.compat.opencomputers.CompatOC"          , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "Forestry"            , T)) ICompat.COMPAT_CLASSES.add(COMPAT_FR        = (ICompatFR        )UT.Reflection.callConstructor("gregapi.compat.forestry.CompatFR"               , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "GalactiCraft"        , T)) ICompat.COMPAT_CLASSES.add(COMPAT_GC        = (ICompatGC        )UT.Reflection.callConstructor("gregapi.compat.galacticraft.CompatGC"           , 0, null, D2));
		if (ConfigsGT.GREGTECH.get("compat", "WarpDrive"           , T)) ICompat.COMPAT_CLASSES.add(COMPAT_WD        = (ICompatWD        )UT.Reflection.callConstructor("gregapi.compat.warpdrive.CompatWD"              , 0, null, D2));
		
		if (MD.TC.mLoaded) try {ThaumcraftApi.objectTags.isEmpty();} catch(NoSuchFieldError e) {throw new RuntimeException("Please uninstall ThaumicFixer, GregTech-6 itself by now fixes the Thaumometer Lag Issue in a far better and less 'Thaumcraft-Addons breaking' way than Thaumic Fixer.");}
		
		SHOW_HIDDEN_ITEMS                   = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "HiddenGTItems"           , F);
		SHOW_HIDDEN_MATERIALS               = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "HiddenGTMaterials"       , F);
		SHOW_HIDDEN_PREFIXES                = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "HiddenGTPrefixes"        , F);
		SHOW_MICROBLOCKS                    = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "MicroBlocks"             , F);
		SHOW_BUMBLEBEES                     = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "Bumblebees"              , F);
		SHOW_ORE_BLOCK_PREFIXES             = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "OreBlocks"               , F);
		SHOW_INTERNAL_NAMES                 = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "InternalNames"           , F);
		SHOW_CHEM_FORMULAS                  = ConfigsGT.CLIENT.get(ConfigCategories.visibility, "ChemTooltips"            , T);
		
		TOOL_SOUNDS_SETTING = TOOL_SOUNDS   = ConfigsGT.CLIENT.get(ConfigCategories.general, "sound_tools"             , TOOL_SOUNDS_SETTING);
		ITexture.Util.GT_ALPHA_BLENDING     = ConfigsGT.CLIENT.get(ConfigCategories.general, "useGTAlphaBlending"      , ITexture.Util.GT_ALPHA_BLENDING);
		ITexture.Util.MC_ALPHA_BLENDING     = ConfigsGT.CLIENT.get(ConfigCategories.general, "useMCAlphaBlending"      , ITexture.Util.MC_ALPHA_BLENDING);
		
		GT6WorldGenerator.PFAA = (ConfigsGT.WORLDGEN.get(ConfigCategories.general, "AutoDetectPFAA", T) && MD.PFAA.mLoaded && MD.COG.mLoaded);
		GT6WorldGenerator.TFC  = (ConfigsGT.WORLDGEN.get(ConfigCategories.general, "AutoDetectTFC" , T) && (MD.TFC.mLoaded || MD.TFCP.mLoaded));
		
		// Register Crafting Recipe Classes.
		RecipeSorter.register("gregtech:shaped"   , AdvancedCraftingShaped.class   , RecipeSorter.Category.SHAPED   , "after:minecraft:shaped before:minecraft:shapeless");
		RecipeSorter.register("gregtech:shapeless", AdvancedCraftingShapeless.class, RecipeSorter.Category.SHAPELESS, "after:gregtech:shaped after:minecraft:shapeless");
		RecipeSorter.register("gregtech:1ToY"     , AdvancedCrafting1ToY.class     , RecipeSorter.Category.SHAPELESS, "after:gregtech:shaped after:gregtech:shapeless");
		RecipeSorter.register("gregtech:XToY"     , AdvancedCraftingXToY.class     , RecipeSorter.Category.SHAPELESS, "after:gregtech:shaped after:gregtech:1ToY");
		RecipeSorter.register("gregtech:tool"     , AdvancedCraftingTool.class     , RecipeSorter.Category.SHAPELESS, "after:gregtech:shaped after:gregtech:XToY");
		
		// A Default Packet Handler for some of the already existing Code. Yes, all those Packets are generalised special cases in order to save on Bandwidth.
		// [        +127] = PacketConfig
		// [        +126] = PacketPrefix
		// [        +125] = PacketItemStackChat
		// [+112 to +119] = PacketBlockEvent
		// [+104 to +111] = PacketBlockError
		// [+ 72 to + 79] = PacketDeathPoint
		// [-120 to + 71] = PacketSyncData
		// [-128 to -121] = PacketSound
		NW_API = new NetworkHandler(MD.GAPI.mID, "GAPI", new PacketConfig(), new PacketPrefix(), new PacketItemStackChat()
		, new PacketBlockEvent                          ( 0), new PacketBlockEvent                          ( 1), new PacketBlockEvent                          ( 2), new PacketBlockEvent                          ( 3), new PacketBlockEvent                          ( 4), new PacketBlockEvent                          ( 5), new PacketBlockEvent                          ( 6), new PacketBlockEvent                          ( 7)
		, new PacketBlockError                          ( 0), new PacketBlockError                          ( 1), new PacketBlockError                          ( 2), new PacketBlockError                          ( 3), new PacketBlockError                          ( 4), new PacketBlockError                          ( 5), new PacketBlockError                          ( 6), new PacketBlockError                          ( 7)
		, new PacketDeathPoint                          ( 0), new PacketDeathPoint                          ( 1), new PacketDeathPoint                          ( 2), new PacketDeathPoint                          ( 3), new PacketDeathPoint                          ( 4), new PacketDeathPoint                          ( 5), new PacketDeathPoint                          ( 6), new PacketDeathPoint                          ( 7)
		, new PacketSound                               ( 0), new PacketSound                               ( 1), new PacketSound                               ( 2), new PacketSound                               ( 3), new PacketSound                               ( 4), new PacketSound                               ( 5), new PacketSound                               ( 6), new PacketSound                               ( 7)
		, new PacketSyncDataName                        ( 0), new PacketSyncDataName                        ( 1), new PacketSyncDataName                        ( 2), new PacketSyncDataName                        ( 3), new PacketSyncDataName                        ( 4), new PacketSyncDataName                        ( 5), new PacketSyncDataName                        ( 6), new PacketSyncDataName                        ( 7)
		, new PacketSyncDataByte                        ( 0), new PacketSyncDataByte                        ( 1), new PacketSyncDataByte                        ( 2), new PacketSyncDataByte                        ( 3), new PacketSyncDataByte                        ( 4), new PacketSyncDataByte                        ( 5), new PacketSyncDataByte                        ( 6), new PacketSyncDataByte                        ( 7)
		, new PacketSyncDataShort                       ( 0), new PacketSyncDataShort                       ( 1), new PacketSyncDataShort                       ( 2), new PacketSyncDataShort                       ( 3), new PacketSyncDataShort                       ( 4), new PacketSyncDataShort                       ( 5), new PacketSyncDataShort                       ( 6), new PacketSyncDataShort                       ( 7)
		, new PacketSyncDataInteger                     ( 0), new PacketSyncDataInteger                     ( 1), new PacketSyncDataInteger                     ( 2), new PacketSyncDataInteger                     ( 3), new PacketSyncDataInteger                     ( 4), new PacketSyncDataInteger                     ( 5), new PacketSyncDataInteger                     ( 6), new PacketSyncDataInteger                     ( 7)
		, new PacketSyncDataLong                        ( 0), new PacketSyncDataLong                        ( 1), new PacketSyncDataLong                        ( 2), new PacketSyncDataLong                        ( 3), new PacketSyncDataLong                        ( 4), new PacketSyncDataLong                        ( 5), new PacketSyncDataLong                        ( 6), new PacketSyncDataLong                        ( 7)
		, new PacketSyncDataByteArray                   ( 0), new PacketSyncDataByteArray                   ( 1), new PacketSyncDataByteArray                   ( 2), new PacketSyncDataByteArray                   ( 3), new PacketSyncDataByteArray                   ( 4), new PacketSyncDataByteArray                   ( 5), new PacketSyncDataByteArray                   ( 6), new PacketSyncDataByteArray                   ( 7)
		, new PacketSyncDataIDs                         ( 0), new PacketSyncDataIDs                         ( 1), new PacketSyncDataIDs                         ( 2), new PacketSyncDataIDs                         ( 3), new PacketSyncDataIDs                         ( 4), new PacketSyncDataIDs                         ( 5), new PacketSyncDataIDs                         ( 6), new PacketSyncDataIDs                         ( 7)
		, new PacketSyncDataByteAndIDs                  ( 0), new PacketSyncDataByteAndIDs                  ( 1), new PacketSyncDataByteAndIDs                  ( 2), new PacketSyncDataByteAndIDs                  ( 3), new PacketSyncDataByteAndIDs                  ( 4), new PacketSyncDataByteAndIDs                  ( 5), new PacketSyncDataByteAndIDs                  ( 6), new PacketSyncDataByteAndIDs                  ( 7)
		, new PacketSyncDataShortAndIDs                 ( 0), new PacketSyncDataShortAndIDs                 ( 1), new PacketSyncDataShortAndIDs                 ( 2), new PacketSyncDataShortAndIDs                 ( 3), new PacketSyncDataShortAndIDs                 ( 4), new PacketSyncDataShortAndIDs                 ( 5), new PacketSyncDataShortAndIDs                 ( 6), new PacketSyncDataShortAndIDs                 ( 7)
		, new PacketSyncDataIntegerAndIDs               ( 0), new PacketSyncDataIntegerAndIDs               ( 1), new PacketSyncDataIntegerAndIDs               ( 2), new PacketSyncDataIntegerAndIDs               ( 3), new PacketSyncDataIntegerAndIDs               ( 4), new PacketSyncDataIntegerAndIDs               ( 5), new PacketSyncDataIntegerAndIDs               ( 6), new PacketSyncDataIntegerAndIDs               ( 7)
		, new PacketSyncDataLongAndIDs                  ( 0), new PacketSyncDataLongAndIDs                  ( 1), new PacketSyncDataLongAndIDs                  ( 2), new PacketSyncDataLongAndIDs                  ( 3), new PacketSyncDataLongAndIDs                  ( 4), new PacketSyncDataLongAndIDs                  ( 5), new PacketSyncDataLongAndIDs                  ( 6), new PacketSyncDataLongAndIDs                  ( 7)
		, new PacketSyncDataByteArrayAndIDs             ( 0), new PacketSyncDataByteArrayAndIDs             ( 1), new PacketSyncDataByteArrayAndIDs             ( 2), new PacketSyncDataByteArrayAndIDs             ( 3), new PacketSyncDataByteArrayAndIDs             ( 4), new PacketSyncDataByteArrayAndIDs             ( 5), new PacketSyncDataByteArrayAndIDs             ( 6), new PacketSyncDataByteArrayAndIDs             ( 7)
		, new PacketSyncDataIDsAndCovers                ( 0), new PacketSyncDataIDsAndCovers                ( 1), new PacketSyncDataIDsAndCovers                ( 2), new PacketSyncDataIDsAndCovers                ( 3), new PacketSyncDataIDsAndCovers                ( 4), new PacketSyncDataIDsAndCovers                ( 5), new PacketSyncDataIDsAndCovers                ( 6), new PacketSyncDataIDsAndCovers                ( 7)
		, new PacketSyncDataByteAndIDsAndCovers         ( 0), new PacketSyncDataByteAndIDsAndCovers         ( 1), new PacketSyncDataByteAndIDsAndCovers         ( 2), new PacketSyncDataByteAndIDsAndCovers         ( 3), new PacketSyncDataByteAndIDsAndCovers         ( 4), new PacketSyncDataByteAndIDsAndCovers         ( 5), new PacketSyncDataByteAndIDsAndCovers         ( 6), new PacketSyncDataByteAndIDsAndCovers         ( 7)
		, new PacketSyncDataShortAndIDsAndCovers        ( 0), new PacketSyncDataShortAndIDsAndCovers        ( 1), new PacketSyncDataShortAndIDsAndCovers        ( 2), new PacketSyncDataShortAndIDsAndCovers        ( 3), new PacketSyncDataShortAndIDsAndCovers        ( 4), new PacketSyncDataShortAndIDsAndCovers        ( 5), new PacketSyncDataShortAndIDsAndCovers        ( 6), new PacketSyncDataShortAndIDsAndCovers        ( 7)
		, new PacketSyncDataIntegerAndIDsAndCovers      ( 0), new PacketSyncDataIntegerAndIDsAndCovers      ( 1), new PacketSyncDataIntegerAndIDsAndCovers      ( 2), new PacketSyncDataIntegerAndIDsAndCovers      ( 3), new PacketSyncDataIntegerAndIDsAndCovers      ( 4), new PacketSyncDataIntegerAndIDsAndCovers      ( 5), new PacketSyncDataIntegerAndIDsAndCovers      ( 6), new PacketSyncDataIntegerAndIDsAndCovers      ( 7)
		, new PacketSyncDataLongAndIDsAndCovers         ( 0), new PacketSyncDataLongAndIDsAndCovers         ( 1), new PacketSyncDataLongAndIDsAndCovers         ( 2), new PacketSyncDataLongAndIDsAndCovers         ( 3), new PacketSyncDataLongAndIDsAndCovers         ( 4), new PacketSyncDataLongAndIDsAndCovers         ( 5), new PacketSyncDataLongAndIDsAndCovers         ( 6), new PacketSyncDataLongAndIDsAndCovers         ( 7)
		, new PacketSyncDataByteArrayAndIDsAndCovers    ( 0), new PacketSyncDataByteArrayAndIDsAndCovers    ( 1), new PacketSyncDataByteArrayAndIDsAndCovers    ( 2), new PacketSyncDataByteArrayAndIDsAndCovers    ( 3), new PacketSyncDataByteArrayAndIDsAndCovers    ( 4), new PacketSyncDataByteArrayAndIDsAndCovers    ( 5), new PacketSyncDataByteArrayAndIDsAndCovers    ( 6), new PacketSyncDataByteArrayAndIDsAndCovers    ( 7)
		, new PacketSyncDataCoverVisuals                ( 0), new PacketSyncDataCoverVisuals                ( 1), new PacketSyncDataCoverVisuals                ( 2), new PacketSyncDataCoverVisuals                ( 3), new PacketSyncDataCoverVisuals                ( 4), new PacketSyncDataCoverVisuals                ( 5), new PacketSyncDataCoverVisuals                ( 6), new PacketSyncDataCoverVisuals                ( 7)
		, new PacketSyncDataByteAndCoverVisuals         ( 0), new PacketSyncDataByteAndCoverVisuals         ( 1), new PacketSyncDataByteAndCoverVisuals         ( 2), new PacketSyncDataByteAndCoverVisuals         ( 3), new PacketSyncDataByteAndCoverVisuals         ( 4), new PacketSyncDataByteAndCoverVisuals         ( 5), new PacketSyncDataByteAndCoverVisuals         ( 6), new PacketSyncDataByteAndCoverVisuals         ( 7)
		, new PacketSyncDataShortAndCoverVisuals        ( 0), new PacketSyncDataShortAndCoverVisuals        ( 1), new PacketSyncDataShortAndCoverVisuals        ( 2), new PacketSyncDataShortAndCoverVisuals        ( 3), new PacketSyncDataShortAndCoverVisuals        ( 4), new PacketSyncDataShortAndCoverVisuals        ( 5), new PacketSyncDataShortAndCoverVisuals        ( 6), new PacketSyncDataShortAndCoverVisuals        ( 7)
		, new PacketSyncDataIntegerAndCoverVisuals      ( 0), new PacketSyncDataIntegerAndCoverVisuals      ( 1), new PacketSyncDataIntegerAndCoverVisuals      ( 2), new PacketSyncDataIntegerAndCoverVisuals      ( 3), new PacketSyncDataIntegerAndCoverVisuals      ( 4), new PacketSyncDataIntegerAndCoverVisuals      ( 5), new PacketSyncDataIntegerAndCoverVisuals      ( 6), new PacketSyncDataIntegerAndCoverVisuals      ( 7)
		, new PacketSyncDataLongAndCoverVisuals         ( 0), new PacketSyncDataLongAndCoverVisuals         ( 1), new PacketSyncDataLongAndCoverVisuals         ( 2), new PacketSyncDataLongAndCoverVisuals         ( 3), new PacketSyncDataLongAndCoverVisuals         ( 4), new PacketSyncDataLongAndCoverVisuals         ( 5), new PacketSyncDataLongAndCoverVisuals         ( 6), new PacketSyncDataLongAndCoverVisuals         ( 7)
		, new PacketSyncDataByteArrayAndCoverVisuals    ( 0), new PacketSyncDataByteArrayAndCoverVisuals    ( 1), new PacketSyncDataByteArrayAndCoverVisuals    ( 2), new PacketSyncDataByteArrayAndCoverVisuals    ( 3), new PacketSyncDataByteArrayAndCoverVisuals    ( 4), new PacketSyncDataByteArrayAndCoverVisuals    ( 5), new PacketSyncDataByteArrayAndCoverVisuals    ( 6), new PacketSyncDataByteArrayAndCoverVisuals    ( 7)
		);
		NW_AP2 = new NetworkHandler(MD.GAPI.mID, "GAP2"
		, new PacketSyncDataByte                        ( 0), new PacketSyncDataByte                        ( 1), new PacketSyncDataByte                        ( 2), new PacketSyncDataByte                        ( 3), new PacketSyncDataByte                        ( 4), new PacketSyncDataByte                        ( 5), new PacketSyncDataByte                        ( 6), new PacketSyncDataByte                        ( 7)
		, new PacketSyncDataShort                       ( 0), new PacketSyncDataShort                       ( 1), new PacketSyncDataShort                       ( 2), new PacketSyncDataShort                       ( 3), new PacketSyncDataShort                       ( 4), new PacketSyncDataShort                       ( 5), new PacketSyncDataShort                       ( 6), new PacketSyncDataShort                       ( 7)
		, new PacketSyncDataInteger                     ( 0), new PacketSyncDataInteger                     ( 1), new PacketSyncDataInteger                     ( 2), new PacketSyncDataInteger                     ( 3), new PacketSyncDataInteger                     ( 4), new PacketSyncDataInteger                     ( 5), new PacketSyncDataInteger                     ( 6), new PacketSyncDataInteger                     ( 7)
		, new PacketSyncDataLong                        ( 0), new PacketSyncDataLong                        ( 1), new PacketSyncDataLong                        ( 2), new PacketSyncDataLong                        ( 3), new PacketSyncDataLong                        ( 4), new PacketSyncDataLong                        ( 5), new PacketSyncDataLong                        ( 6), new PacketSyncDataLong                        ( 7)
		, new PacketSyncDataByteArray                   ( 0), new PacketSyncDataByteArray                   ( 1), new PacketSyncDataByteArray                   ( 2), new PacketSyncDataByteArray                   ( 3), new PacketSyncDataByteArray                   ( 4), new PacketSyncDataByteArray                   ( 5), new PacketSyncDataByteArray                   ( 6), new PacketSyncDataByteArray                   ( 7)
		, new PacketSyncDataIDs                         ( 0), new PacketSyncDataIDs                         ( 1), new PacketSyncDataIDs                         ( 2), new PacketSyncDataIDs                         ( 3), new PacketSyncDataIDs                         ( 4), new PacketSyncDataIDs                         ( 5), new PacketSyncDataIDs                         ( 6), new PacketSyncDataIDs                         ( 7)
		, new PacketSyncDataByteAndIDs                  ( 0), new PacketSyncDataByteAndIDs                  ( 1), new PacketSyncDataByteAndIDs                  ( 2), new PacketSyncDataByteAndIDs                  ( 3), new PacketSyncDataByteAndIDs                  ( 4), new PacketSyncDataByteAndIDs                  ( 5), new PacketSyncDataByteAndIDs                  ( 6), new PacketSyncDataByteAndIDs                  ( 7)
		, new PacketSyncDataShortAndIDs                 ( 0), new PacketSyncDataShortAndIDs                 ( 1), new PacketSyncDataShortAndIDs                 ( 2), new PacketSyncDataShortAndIDs                 ( 3), new PacketSyncDataShortAndIDs                 ( 4), new PacketSyncDataShortAndIDs                 ( 5), new PacketSyncDataShortAndIDs                 ( 6), new PacketSyncDataShortAndIDs                 ( 7)
		, new PacketSyncDataIntegerAndIDs               ( 0), new PacketSyncDataIntegerAndIDs               ( 1), new PacketSyncDataIntegerAndIDs               ( 2), new PacketSyncDataIntegerAndIDs               ( 3), new PacketSyncDataIntegerAndIDs               ( 4), new PacketSyncDataIntegerAndIDs               ( 5), new PacketSyncDataIntegerAndIDs               ( 6), new PacketSyncDataIntegerAndIDs               ( 7)
		, new PacketSyncDataLongAndIDs                  ( 0), new PacketSyncDataLongAndIDs                  ( 1), new PacketSyncDataLongAndIDs                  ( 2), new PacketSyncDataLongAndIDs                  ( 3), new PacketSyncDataLongAndIDs                  ( 4), new PacketSyncDataLongAndIDs                  ( 5), new PacketSyncDataLongAndIDs                  ( 6), new PacketSyncDataLongAndIDs                  ( 7)
		, new PacketSyncDataByteArrayAndIDs             ( 0), new PacketSyncDataByteArrayAndIDs             ( 1), new PacketSyncDataByteArrayAndIDs             ( 2), new PacketSyncDataByteArrayAndIDs             ( 3), new PacketSyncDataByteArrayAndIDs             ( 4), new PacketSyncDataByteArrayAndIDs             ( 5), new PacketSyncDataByteArrayAndIDs             ( 6), new PacketSyncDataByteArrayAndIDs             ( 7)
		, new PacketSyncDataIDsAndCovers                ( 0), new PacketSyncDataIDsAndCovers                ( 1), new PacketSyncDataIDsAndCovers                ( 2), new PacketSyncDataIDsAndCovers                ( 3), new PacketSyncDataIDsAndCovers                ( 4), new PacketSyncDataIDsAndCovers                ( 5), new PacketSyncDataIDsAndCovers                ( 6), new PacketSyncDataIDsAndCovers                ( 7)
		, new PacketSyncDataByteAndIDsAndCovers         ( 0), new PacketSyncDataByteAndIDsAndCovers         ( 1), new PacketSyncDataByteAndIDsAndCovers         ( 2), new PacketSyncDataByteAndIDsAndCovers         ( 3), new PacketSyncDataByteAndIDsAndCovers         ( 4), new PacketSyncDataByteAndIDsAndCovers         ( 5), new PacketSyncDataByteAndIDsAndCovers         ( 6), new PacketSyncDataByteAndIDsAndCovers         ( 7)
		, new PacketSyncDataShortAndIDsAndCovers        ( 0), new PacketSyncDataShortAndIDsAndCovers        ( 1), new PacketSyncDataShortAndIDsAndCovers        ( 2), new PacketSyncDataShortAndIDsAndCovers        ( 3), new PacketSyncDataShortAndIDsAndCovers        ( 4), new PacketSyncDataShortAndIDsAndCovers        ( 5), new PacketSyncDataShortAndIDsAndCovers        ( 6), new PacketSyncDataShortAndIDsAndCovers        ( 7)
		, new PacketSyncDataIntegerAndIDsAndCovers      ( 0), new PacketSyncDataIntegerAndIDsAndCovers      ( 1), new PacketSyncDataIntegerAndIDsAndCovers      ( 2), new PacketSyncDataIntegerAndIDsAndCovers      ( 3), new PacketSyncDataIntegerAndIDsAndCovers      ( 4), new PacketSyncDataIntegerAndIDsAndCovers      ( 5), new PacketSyncDataIntegerAndIDsAndCovers      ( 6), new PacketSyncDataIntegerAndIDsAndCovers      ( 7)
		, new PacketSyncDataLongAndIDsAndCovers         ( 0), new PacketSyncDataLongAndIDsAndCovers         ( 1), new PacketSyncDataLongAndIDsAndCovers         ( 2), new PacketSyncDataLongAndIDsAndCovers         ( 3), new PacketSyncDataLongAndIDsAndCovers         ( 4), new PacketSyncDataLongAndIDsAndCovers         ( 5), new PacketSyncDataLongAndIDsAndCovers         ( 6), new PacketSyncDataLongAndIDsAndCovers         ( 7)
		, new PacketSyncDataByteArrayAndIDsAndCovers    ( 0), new PacketSyncDataByteArrayAndIDsAndCovers    ( 1), new PacketSyncDataByteArrayAndIDsAndCovers    ( 2), new PacketSyncDataByteArrayAndIDsAndCovers    ( 3), new PacketSyncDataByteArrayAndIDsAndCovers    ( 4), new PacketSyncDataByteArrayAndIDsAndCovers    ( 5), new PacketSyncDataByteArrayAndIDsAndCovers    ( 6), new PacketSyncDataByteArrayAndIDsAndCovers    ( 7)
		, new PacketSyncDataCoverVisuals                ( 0), new PacketSyncDataCoverVisuals                ( 1), new PacketSyncDataCoverVisuals                ( 2), new PacketSyncDataCoverVisuals                ( 3), new PacketSyncDataCoverVisuals                ( 4), new PacketSyncDataCoverVisuals                ( 5), new PacketSyncDataCoverVisuals                ( 6), new PacketSyncDataCoverVisuals                ( 7)
		, new PacketSyncDataByteAndCoverVisuals         ( 0), new PacketSyncDataByteAndCoverVisuals         ( 1), new PacketSyncDataByteAndCoverVisuals         ( 2), new PacketSyncDataByteAndCoverVisuals         ( 3), new PacketSyncDataByteAndCoverVisuals         ( 4), new PacketSyncDataByteAndCoverVisuals         ( 5), new PacketSyncDataByteAndCoverVisuals         ( 6), new PacketSyncDataByteAndCoverVisuals         ( 7)
		, new PacketSyncDataShortAndCoverVisuals        ( 0), new PacketSyncDataShortAndCoverVisuals        ( 1), new PacketSyncDataShortAndCoverVisuals        ( 2), new PacketSyncDataShortAndCoverVisuals        ( 3), new PacketSyncDataShortAndCoverVisuals        ( 4), new PacketSyncDataShortAndCoverVisuals        ( 5), new PacketSyncDataShortAndCoverVisuals        ( 6), new PacketSyncDataShortAndCoverVisuals        ( 7)
		, new PacketSyncDataIntegerAndCoverVisuals      ( 0), new PacketSyncDataIntegerAndCoverVisuals      ( 1), new PacketSyncDataIntegerAndCoverVisuals      ( 2), new PacketSyncDataIntegerAndCoverVisuals      ( 3), new PacketSyncDataIntegerAndCoverVisuals      ( 4), new PacketSyncDataIntegerAndCoverVisuals      ( 5), new PacketSyncDataIntegerAndCoverVisuals      ( 6), new PacketSyncDataIntegerAndCoverVisuals      ( 7)
		, new PacketSyncDataLongAndCoverVisuals         ( 0), new PacketSyncDataLongAndCoverVisuals         ( 1), new PacketSyncDataLongAndCoverVisuals         ( 2), new PacketSyncDataLongAndCoverVisuals         ( 3), new PacketSyncDataLongAndCoverVisuals         ( 4), new PacketSyncDataLongAndCoverVisuals         ( 5), new PacketSyncDataLongAndCoverVisuals         ( 6), new PacketSyncDataLongAndCoverVisuals         ( 7)
		, new PacketSyncDataByteArrayAndCoverVisuals    ( 0), new PacketSyncDataByteArrayAndCoverVisuals    ( 1), new PacketSyncDataByteArrayAndCoverVisuals    ( 2), new PacketSyncDataByteArrayAndCoverVisuals    ( 3), new PacketSyncDataByteArrayAndCoverVisuals    ( 4), new PacketSyncDataByteArrayAndCoverVisuals    ( 5), new PacketSyncDataByteArrayAndCoverVisuals    ( 6), new PacketSyncDataByteArrayAndCoverVisuals    ( 7)
		);
		// Registering the TileEntity used for Meta-Generated Blocks to store the 32000 variations.
		// F12-entity (ALREADY DONE ABOVE, nothing to call here): the original registered a class
		// (`GameRegistry.registerTileEntity(PrefixBlockTileEntity.class, "gt.MetaBlockTileEntity")`), neo registers a
		// BlockEntityType. It's set up once for the whole GT6 TE hierarchy — MTE_TYPE_HOLDER (:200), and its supplier itself
		// returns PrefixBlockTileEntity for PrefixBlock blocks (TileEntityBase01Root.createType:164) with isValid()→true, so
		// no separate type is needed for meta-blocks: reconstruction from NBT and placement into the world already go through it.
		// Creating and loading the Lang File.
		if (CODE_CLIENT) {
			tFile = new File(DirectoriesGT.MINECRAFT, "GregTech.lang");
			if (!tFile.exists()) tFile = new File(DirectoriesGT.MINECRAFT, "gregtech.lang");
			LanguageHandler.sLangFile = new ModConfigSpec(tFile);
			LanguageHandler.sUseFile = LanguageHandler.sLangFile.get("EnableLangFile", "UseThisFileAsLanguageFile", F).getBoolean(F);
		}
		// BUG-106 (a second leak, measured in a live game on 2026-08-09): the icon-load queues are cleared on BOTH sides, not just
		// the server. In 1.7.10 they were drained by the icon-loading-phase driver (ItemFluidDisplay.registerIcons -> iterating
		// sBlockIconload while stitching the atlas); in the port this driver is DEAD (IIconRegister was removed by the engine), and icon
		// construction is now LAZY (Textures.java:720, TextureSet.java:158, BI.java:176) — nothing reads the queue anymore, but it
		// kept being FILLED. On the client it lived forever: every CustomIcon created once already in-game registered itself in a
		// static field forever. The hot source is MultiTileEntityMultiBlockPart.readFromNBT2:144 (icons are built on EVERY
		// read of a multiblock part's NBT, and that happens on chunk load and block-entity reconstruction).
		// Measurement: 8,549,954 CustomIcon objects, the GT_API class retained 1,924,485,544 bytes = 47.26% of the heap (MAT dump).
		// Clearing the queue disables all four write sites at once (they're all behind a `!= null` gate) — a single center, not scattered code.
		if (sBlockIconload != null) {sBlockIconload.clear(); sBlockIconload = null;}
		if (sItemIconload  != null) {sItemIconload .clear(); sItemIconload  = null;}
		// Creating and loading the Unification Config.
		OreDictManager.INSTANCE.mUnificationConfig = new Config("Unification.cfg");
		// Initialising the Re-Registrations.
		new LoaderOreDictReRegistrations().run();
		// Register the Falling MetaBlock Entity.
		// F12-entity (ALREADY DONE ABOVE, nothing to call here): 1.7.10 registered the entity class right at this point
		// (`EntityRegistry.registerModEntity`, the 1.7.10 original :722), neo requires the EntityType in the registry BEFORE this phase —
		// so registration moved to the central ENTITIES/METABLOCK_FALLING (:203-214), parameters 1:1.
		// Initialise Enchantments.
		new Enchantment_WerewolfDamage();
		new Enchantment_EnderDamage();
		new Enchantment_Radioactivity();
		new Enchantment_SlimeDamage();
		// Initialises the Fluid Display Item.
		// F12-lazy: item construction is deferred into the DeferredRegister supplier (called on RegisterEvent — the registry is open for an
		// intrusive holder); IL holds the supplier, mStack materializes lazily at runtime. Was: IL.Display_Fluid.set(new ItemFluidDisplay()).
		IL.Display_Fluid.set(GT_API.ITEMS.register("gt.display.fluid", ItemFluidDisplay::new));
		// Initialises the Integrated Circuit Item.
		IL.Circuit_Selector.set(GT_API.ITEMS.register("gt.integrated_circuit", ItemIntegratedCircuit::new)); // F12-lazy: construct@RegisterEvent-supplier
		// Initialises the Empty Slot Marker Item.
		IL.Empty_Slot.set(GT_API.ITEMS.register("gt.empty_slot", ItemEmptySlot::new)); // F12-lazy: construct@RegisterEvent-supplier
		// Register the GUI Handler.
		// F7-gui (the GUI works via GT6MenuProvider/ContainerCommon; the old Forge GUI-handler is documentation only)
		// F12 boot-timing: recipe fixes (ST.make = ItemStack) are MOVED to onLoad (FMLCommonSetupEvent) — stacks can't
		// be created in preInit (Holder.components not bound). See onLoad. (Was: CR.remove/CR.shaped/RM.Canner.addRecipe1 here.)

		// F12: removed the FML hack that forced GAPI to the front of activeModList via reflection
		// (LoadController/ModList/ModContainer — internal classes of FML 1.7.10, no counterpart in neo).
		// Its function ("GAPI loads first") is now honestly and declaratively provided by the engine's
		// dependency graph — @Mod(..., depends = {ModIDs.GAPI_POST}) above in this file
		// (decisions/F12-registration-lifecycle.md §3-4).

		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onPreLoad(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	@Override
	public void onModInit2(FMLInitializationEvent aEvent) {
		if (MD.CHSL.mLoaded) try {
			Carving.chisel.getGroup("cobblestone").setOreName(null);
			Carving.chisel.getGroup("glowstone").setOreName(null);
		} catch(Throwable e) {e.printStackTrace(ERR);}
		
		OUT.println(getModNameForLog() + ": If the Loading Bar somehow Freezes at this Point, then you definetly ran out of Memory or permgenspace, look at the other Logs to confirm it.");
		OreDictManager.INSTANCE.enableRegistrations();
		
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onLoad(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	@Override
	public void onModPostInit2(FMLPostInitializationEvent aEvent) {deferItemInit(() -> onModPostInit2Deferred(aEvent));} // F1/F12/F16: PostInit data-init (ST.make/static-init) is deferred to server-start (post-bind); LoadComplete is NOT post-bind
	private void onModPostInit2Deferred(FMLPostInitializationEvent aEvent) {
		if (MD.IC2.mLoaded) {
			PotionsGT.ID_RADIATION    = ic2.api.info.Info.POTION_RADIATION.id;
		}
		if (MD.ENVM.mLoaded) {
			PotionsGT.ID_DEHYDRATION  = enviromine.EnviroPotion.dehydration.id;
			PotionsGT.ID_FROSTBITE    = enviromine.EnviroPotion.frostbite.id;
			PotionsGT.ID_HEATSTROKE   = enviromine.EnviroPotion.heatstroke.id;
			PotionsGT.ID_HYPOTHERMIA  = enviromine.EnviroPotion.hypothermia.id;
			PotionsGT.ID_INSANITY     = enviromine.EnviroPotion.insanity.id;
		}
		if (MD.IE.mLoaded) {
			PotionsGT.ID_FLAMMABLE    = blusunrize.immersiveengineering.common.util.IEPotions.flammable.id;
			PotionsGT.ID_SLIPPERY     = blusunrize.immersiveengineering.common.util.IEPotions.slippery.id;
			PotionsGT.ID_CONDUCTIVE   = blusunrize.immersiveengineering.common.util.IEPotions.conductive.id;
			PotionsGT.ID_STICKY       = blusunrize.immersiveengineering.common.util.IEPotions.sticky.id;
		}
		// BUG-090: the owner mods above don't exist for 26.1.2 (the gates are dead) — the five effects GT6
		// actually applies are registered by the mod itself (gregapi/potion/MobEffectsGT, behavior 1:1 with the decompile
		// references of IE/EnviroMine in the project tree). The mechanism is Gregorius's own "real IDs are to be set on API
		// postInit" (CS.java:1690): we set the id and bind the Holder into the single applyPotion(int) channel map.
		// The `< 0` gate preserves priority for a foreign mod if it ever comes back to life. RADIATION/DEHYDRATION/
		// HYPOTHERMIA/HEATSTROKE/FROSTBITE stay negative deliberately — see the breakdown in MobEffectsGT (javadoc).
		if (PotionsGT.ID_FLAMMABLE  < 0) UT.Entities.bindPotionID(PotionsGT.ID_FLAMMABLE  = gregapi.potion.MobEffectsGT.ID_FLAMMABLE , gregapi.potion.MobEffectsGT.FLAMMABLE );
		if (PotionsGT.ID_SLIPPERY   < 0) UT.Entities.bindPotionID(PotionsGT.ID_SLIPPERY   = gregapi.potion.MobEffectsGT.ID_SLIPPERY  , gregapi.potion.MobEffectsGT.SLIPPERY  );
		if (PotionsGT.ID_CONDUCTIVE < 0) UT.Entities.bindPotionID(PotionsGT.ID_CONDUCTIVE = gregapi.potion.MobEffectsGT.ID_CONDUCTIVE, gregapi.potion.MobEffectsGT.CONDUCTIVE);
		if (PotionsGT.ID_STICKY     < 0) UT.Entities.bindPotionID(PotionsGT.ID_STICKY     = gregapi.potion.MobEffectsGT.ID_STICKY    , gregapi.potion.MobEffectsGT.STICKY    );
		if (PotionsGT.ID_INSANITY   < 0) UT.Entities.bindPotionID(PotionsGT.ID_INSANITY   = gregapi.potion.MobEffectsGT.ID_INSANITY  , gregapi.potion.MobEffectsGT.INSANITY  );

		EnergyCompat.checkAvailabilities();
		ToolCompat.checkAvailabilities();
		ST.checkAvailabilities();
		
		OUT.println(getModNameForLog() + ": If the Loading Bar somehow Freezes at this Point, then you definetly ran out of Memory or permgenspace, look at the other Logs to confirm it.");
		OreDictManager.INSTANCE.onPostLoad();
		
		ICover tCover = new CoverRedstoneTorch();
		// F12: block-flatten (data) — Blocks.REDSTONE_TORCH/unlit_redstone_torch (1.7.10, two separate
		// lit/unlit blocks) are merged in neo into ONE Blocks.REDSTONE_TORCH with a BlockState "lit" property (there is
		// no separate unlit constant); the second registration resolves to the same key — a harmless duplicate,
		// not a data loss (the same tCover onto the same resulting block).
		CoverRegistry.put(ST.make(Blocks.REDSTONE_TORCH, 1, 0), tCover);
		CoverRegistry.put(ST.make(Blocks.REDSTONE_TORCH, 1, 0), tCover);
		CoverRegistry.put(ST.make(Items.REPEATER, 1, 0), new CoverRedstoneRepeater());
		
		OreDictPrefix.applyAllStackSizes();
		
		ST.forceProperMaxStacksizes();
		
//      Doesn't fucking work, the Chisel API is pure garbage...
//      if (MD.CHSL.mLoaded) {
//          if (MD.EtFu.mLoaded) {
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "granite|" +MD.EtFu.mID+":stone|1");
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "diorite|" +MD.EtFu.mID+":stone|3");
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "andesite|"+MD.EtFu.mID+":stone|5");
//          }
//          if (MD.BOTA.mLoaded) {
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "granite|" +MD.BOTA.mID+":stone|3");
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "diorite|" +MD.BOTA.mID+":stone|2");
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "andesite|"+MD.BOTA.mID+":stone|0");
//          }
//          if (MD.GT.mLoaded) {
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "granite|" +MD.GT.mID+":gt.stone.granite|0");
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "diorite|" +MD.GT.mID+":gt.stone.diorite|0");
//              FMLInterModComms.sendRuntimeMessage(GAPI, "ChiselAPI|Carving", "variation:add", "andesite|"+MD.GT.mID+":gt.stone.andesite|0");
//          }
//      }
		
		// Saving the Lang File.
		LanguageHandler.save();
		
		if (mPlayerLogger != null) new Thread(mPlayerLogger).start();
		
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onPostLoad(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
		
		for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_ARRAY) if (tMaterial != null && !tMaterial.contains(TD.Properties.INVALID_MATERIAL)) {
			if (tMaterial.mID < 10000) MAT_LOG.print(" ");
			if (tMaterial.mID <  1000) MAT_LOG.print(" ");
			if (tMaterial.mID <   100) MAT_LOG.print(" ");
			if (tMaterial.mID <    10) MAT_LOG.print(" ");
			MAT_LOG.print(tMaterial.mID);
			MAT_LOG.print(": ");
			MAT_LOG.print(tMaterial.mNameInternal);
			MAT_LOG.println();
		}
	}
	
	@Override
	public void onModServerStarting2(ServerStartingEvent aEvent) {
		// F1/F12/F16 item-model: the deferred item stack-init (OreDict data+recipes) — the SINGLE execution point moved
		// to LEVEL LOAD ({@link #onLevelLoadEarlyItemInit}, LevelEvent.Load), since the spawn-area pre-generation
		// (prepareLevels) happens BEFORE ServerStartingEvent and consumes the worldgen registry (BUG-033). LevelEvent.Load is also
		// post-bind (createLevels, compositeAccess frozen), but BEFORE prepareLevels. By this point the queue is already drained.
		// F16-shell: the tab generator (the MTE loader) has already run earlier in the drain → we snapshot the full set of
		// own tabs into the config cache; on the NEXT boot createShellsFromCache raises them before the CreativeModeTab registry freezes.
		gregapi.item.CreativeTabsGT.writeShellCache();
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onServerStarting(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	@Override
	public void onModServerStarted2(ServerStartedEvent aEvent) {
		for (Map<ItemStackContainer, ?> tMap : STACKMAPS) UT.Code.reMap(tMap);
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onServerStarted(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	@Override
	public void onModServerStopping2(ServerStoppingEvent aEvent) {
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onServerStopping(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	@Override
	public void onModServerStopped2(ServerStoppedEvent aEvent) {
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onServerStopped(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
		// RESTORING THE CONTRACT "what the mod started for the server's lifetime lives for the server's lifetime".
		// The 1.7.10 original never said goodbye to this thread at all (GT_API.java:830 — only starts it; there's no
		// shutdown anywhere in its whole tree), and the port inherited that 1:1. On the target engine this doesn't work:
		// a dedicated server's normal exit does NOT call System.exit — across the whole net.minecraft.server package it
		// occurs only in ServerWatchdog, i.e. on an emergency kill of a hung server. So the diary's infinite loop
		// (LoggerPlayerActivity, started 1:1 in postLoad) kept the JVM alive AFTER "stop": the world is
		// saved, ports are released, but the process doesn't exit — the dedicated-server owner gets a hanging restart.
		// We say goodbye here, in the mod's own farewell center, and specifically by letting the work finish: the diary
		// writes its final entry and closes the file itself (see LoggerPlayerActivity.stop()). The thread can't be marked
		// as a daemon — the engine would cut it off mid-write, losing the tail of the log.
		// Warning: whether the 1.7.10 engine needed this is NOT verified against the local reference — its sources
		// aren't in reference/. What is verified, and sufficient, is the opposite: the target engine has no such mechanism.
		if (mPlayerLogger != null) mPlayerLogger.stop();
	}
	
	// neo has no numeric block/item IDs, so there's no neo counterpart to FMLModIdMappingEvent — the method
	// isn't subscribed to any bus (not called automatically, but remains 1:1 manually available for
	// ICompat.onIDChanging(...), should it be needed during runtime-parity work — see javadoc
	// gregapi.api.FMLModIdMappingEvent).
	public void onIDChangingEvent(FMLModIdMappingEvent aEvent) {
		// Fixing missing Blocks caused by DragonAPI. The Issue is more complicated but it should fix some part of it.
		// F12 impossible-1:1 (foreign DragonAPI-fix; neo has no numeric block IDs at all): the DragonAPI fix is tied to the numeric Block.blockRegistry
		// (getObjectById/addObject(int,...)) from Forge 1.7.10 — in NeoForge numeric block IDs don't exist at all
		// (grep of all 3 reference roots: net.minecraft/net.neoforged — neither blockRegistry nor int-based
		// addObject/getObjectById exists), so this piece has no neo-1:1 and can't have one.
		// The method itself isn't subscribed to any bus (see the comment above), which is the only living
		// reason it's kept — the rest below (STACKMAPS remap + dispatching ICompat.onIDChanging) is preserved 1:1.

		OUT.println(getModNameForLog() + ": Remapping ItemStackMaps due to ID Map change. Those damn Items should have a consistent Hashcode, but noooo, ofcourse they break Basic Code Conventions! Thanks Forge and Mojang!");
		
		for (Map<ItemStackContainer, ?> tMap : STACKMAPS) UT.Code.reMap(tMap);
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onIDChanging(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
}
