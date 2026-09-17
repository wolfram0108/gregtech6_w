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

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.api.distmarker.Dist;
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

/** The three original FML mods become three separate neo @Mod entry points; depends() here only gates
 *  presence (a raw modId, not old FML's "required-before:" prefix, which would drop the entrypoint). */
@Mod(ModIDs.GAPI)
public class GT_API extends Abstract_Mod {
	/** Replaces @SidedProxy: neo has no side-dispatch annotation, so the side is chosen via FMLEnvironment#getDist().
	 *  Assigned after MT#init(), not inline in the static initializer, or class-init would run before STACKMAPS exists. */
	public static GT_API_Proxy api_proxy;

	public static final Collection<Map<ItemStackContainer, ?>> STACKMAPS = new ArrayListNoNulls<>();

	/** Used to register Icons. It is not necessary to make those into Lists */
	public static Set<Runnable> sBlockIconload = new HashSetNoNulls<>(), sItemIconload = new HashSetNoNulls<>();
	/** The Icon Registers from Blocks and Items. They will get set right before the corresponding Icon Load Phase as executed in the Runnable List above. */
	// 1.7.10's IIconRegister is gone entirely (atlas stitching is now baked models); same class of problem as
	// TextureSet.registerIcons(Object) — typed as Object, with consumers already moved to ResourceLocation.
	public static Object sBlockIcons, sItemIcons;

	/** The single point through which the whole mod registers Item/Block in NeoForge's DeferredRegister,
	 *  replacing scattered direct DeferredRegister calls found across GT6_Main/GT_API_Proxy/ST. */
	public static final DeferredRegister<net.minecraft.world.item.Item>  ITEMS  = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, ModIDs.GAPI);
	public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCKS, ModIDs.GAPI);

	/** GT6 1.7.10 stores an item's subtype in the damage value; neo clamps setDamageValue to [0,maxDamage],
	 *  so a maxDamage=0 meta-item would collapse every subtype to 0, breaking unification/recipes/MTEs. */
	/** One placeholder BlockEntityType for the whole MTE hierarchy: creating one calls createIntrusiveHolder,
	 *  only valid during RegisterEvent<BlockEntityType>; the old lazy <clinit> creation hit a frozen registry. */
	public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, ModIDs.GAPI);
	public static final Object MTE_TYPE_HOLDER = BLOCK_ENTITIES.register("mte", gregapi.tileentity.base.TileEntityBase01Root::createType);
	/** Second type of the same hierarchy — the non-ticking half (super(F)): it exists only to answer the engine's
	 *  one pre-tick question, EntityBlock.getTicker(level, state, type). */
	public static final Object MTE_TYPE_NOTICK_HOLDER = BLOCK_ENTITIES.register("mte_notick", gregapi.tileentity.base.TileEntityBase01Root::createTypeNoTick);

	/** Central EntityType registry for gregapi content, the same pattern as ITEMS/BLOCKS/BLOCK_ENTITIES above.
	 *  Replaces the removed 1.7.10 EntityRegistry.registerModEntity. */
	public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ModIDs.GAPI);

	/** The falling meta-block; parameters match the original registerModEntity call: trackingRange 160, updateFrequency 1.
	 *  Hitbox matches vanilla's FALLING_BLOCK; the registry name is lowercased since ResourceLocation forbids uppercase. */
	public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<gregapi.block.prefixblock.PrefixBlockFallingEntity>> METABLOCK_FALLING =
		ENTITIES.register("gt_metablockfallingentity", () -> net.minecraft.world.entity.EntityType.Builder.<gregapi.block.prefixblock.PrefixBlockFallingEntity>of(gregapi.block.prefixblock.PrefixBlockFallingEntity::new, net.minecraft.world.entity.MobCategory.MISC)
			.sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(1)
			.build("gt_metablockfallingentity"));

	/** GT6's own sounds. In 1.7.10 a sound only needed a name in sounds.json; the engine looked it up by name.
	 *  neo requires a registered SoundEvent for that name, or the sound never plays; keys come from that same file. */
	public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, ModIDs.GAPI);
	static {
		for (String tKey : soundKeysFromAssets()) SOUND_EVENTS.register(tKey, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(ModIDs.GAPI, tKey)));
	}
	/** The only source of truth is the mod's own sounds.json. Reading it via the classloader fails in the shipped jar
	 *  (FML's modular loader doesn't serve mod resources that way), silently registering zero sounds. */
	private static java.util.List<String> soundKeysFromAssets() {
		String tPath = "assets/" + ModIDs.GAPI + "/sounds.json";
		java.util.List<String> rKeys = java.util.List.of();
		try (java.io.InputStream tIn = GT_API.class.getResourceAsStream("/" + tPath)) {
			if (tIn != null) rKeys = soundKeysFrom(tIn);
		} catch (Throwable e) {/* Fallback path below. */}
		// Fallback path: wherever the class itself physically lives — a directory in dev, a jar in the shipped build.
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
		} catch (Throwable e) {/* If empty, no sounds will register — visible in the line below. */}
		OUT.println("GT6 sounds: объявлено в " + tPath + " и зарегистрировано " + rKeys.size() + " звуков " + rKeys);
		return rKeys;
	}
	private static java.util.List<String> soundKeysFrom(java.io.InputStream aIn) throws java.io.IOException {
		try (java.io.InputStream tIn = aIn) {
			com.google.gson.JsonObject tJson = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(tIn, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			return java.util.List.copyOf(tJson.keySet());
		}
	}

	// On 1.20.1 the SUBTYPE/ZEROSIZE data components don't exist; subtype is back in damage (ST.meta_) and
	// the size-0-catalyst marker is back in stack NBT (ST.size_), the same channels as the original.

	/** GT6 items used to build OreDict data and recipes (ST.make) right in the constructor, but neo constructs
	 *  the item at RegisterEvent while stacks can only be built post-freeze (Holder.components bind later). */
	public static final List<Runnable> DEFERRED_ITEM_INIT = new ArrayListNoNulls<>();
	public static void deferItemInit(Runnable aInit) {if (aInit != null) DEFERRED_ITEM_INIT.add(aInit);}
	/** Window where the deferred stack-init runs at server start: in 1.7.10 the whole pipeline ran at Init/PreInit;
	 *  neo only binds Holder.components at server start, so the pipeline physically moved here. */
	public static boolean sDeferredItemInitRunning = false;
	/** True only after the queue has drained once: before that, god-item variants aren't added yet (still queued),
	 *  so judging a tab/item empty by getSubItems would be wrong (see CreativeTabsGT.isTabEmpty). */
	public static boolean sDeferredItemInitDone = false;

	/** Queue of foreign-recipe scans. In 1.7.10 the scan ran at PostInit against a finished CraftingManager;
	 *  its neo input (ore-versions, role-C) only appears at server start, so it runs after role-C, before finalize. */
	public static final List<Runnable> DEFERRED_RECIPE_SCAN = new ArrayListNoNulls<>();
	public static void deferRecipeScan(Runnable aScan) {if (aScan != null) DEFERRED_RECIPE_SCAN.add(aScan);}
	/** The server for the current recipe-scan window (non-null only while the queue runs), used by removeDatapackRecipes. */
	public static net.minecraft.server.MinecraftServer sCurrentServerForRecipeScan = null;

	/** Neo equivalent of 1.7.10's CraftingManager.getRecipeList().remove(...): neo's RecipeManager has no
	 *  runtime removal, so the map is rebuilt without suppressed entries and swapped in by reflection. */
	/** Every key ever suppressed, so it can be reapplied after /reload (the datapack's recipe map gets recreated). */
	public static final java.util.Set<net.minecraft.resources.ResourceLocation> SUPPRESSED_DATAPACK_RECIPES = new java.util.HashSet<>();

	public void onDatapackSyncReapplySuppression(net.minecraftforge.event.OnDatapackSyncEvent aEvent) {
		if (aEvent.getPlayer() != null) return; // Player join doesn't recreate the map; reapplication is only needed on /reload.
		removeDatapackRecipes(aEvent.getPlayerList().getServer(), new java.util.HashSet<>(SUPPRESSED_DATAPACK_RECIPES));
	}

	public static void removeDatapackRecipes(net.minecraft.server.MinecraftServer aServer, java.util.Set<net.minecraft.resources.ResourceLocation> aRemove) {
		if (aServer == null || aRemove == null || aRemove.isEmpty()) return;
		SUPPRESSED_DATAPACK_RECIPES.addAll(aRemove);
		try {
			// 1.20.1: rebuilding the recipe list uses the engine's public API, replaceRecipes(Iterable<Recipe<?>>);
			// the reflection needed on 26.x isn't needed here.
			net.minecraft.world.item.crafting.RecipeManager tRM = aServer.getRecipeManager();
			java.util.List<net.minecraft.world.item.crafting.Recipe<?>> tKeep = new java.util.ArrayList<>();
			int tBefore = 0;
			for (net.minecraft.world.item.crafting.Recipe<?> tRecipe : tRM.getRecipes()) {tBefore++; if (!aRemove.contains(tRecipe.getId())) tKeep.add(tRecipe);}
			tRM.replaceRecipes(tKeep);
			OUT.println("GT_API: datapack recipes suppressed (F11-recipe-scan): " + (tBefore - tKeep.size()) + " of " + aRemove.size() + " requested.");
		} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	// The drain loop: a callback can add a new deferItemInit itself (nested deferral, e.g. block -> slab).
	// Processed FIFO without ConcurrentModification, draining anything added mid-run too.
	public static void runDeferredItemInit() {
		sDeferredItemInitRunning = true;
		// Vanilla dictionary entries that Forge itself seeded before any mod loaded in 1.7.10; called first here,
		// since GT6's own stack-based content below needs to see an already-populated vanilla dictionary.
		try {gregapi.oredict.OreDictionary.initVanillaEntries();} catch(Throwable e) {e.printStackTrace(ERR);}
		// Incoming tag bridge: in 1.7.10 foreign mods called OreDictionary.registerOre themselves; here the common
		// language is forge: tags, read by the bridge and fed into the same dictionary entry point.
		try {gregapi.oredict.OreDictTags.importFromTags();} catch(Throwable e) {e.printStackTrace(ERR);}
		try {while (!DEFERRED_ITEM_INIT.isEmpty()) {Runnable tInit = DEFERRED_ITEM_INIT.remove(0); try {tInit.run();} catch(Throwable e) {e.printStackTrace(ERR);}}}
		finally {sDeferredItemInitRunning = false; sDeferredItemInitDone = true;}
	}

	/** Some GT6 subsystems build the neo Block outside any DeferredRegister supplier and outside preInit,
	 *  so they can't be expressed as a single registerBlockLazy; wrapped in deferBlockInit(Runnable) instead. */
	public static final List<Runnable> DEFERRED_BLOCK_INIT = new ArrayListNoNulls<>();
	public static void deferBlockInit(Runnable aInit) {if (aInit != null) DEFERRED_BLOCK_INIT.add(aInit);}
	/** Active RegisterEvent<Block> during the DEFERRED_BLOCK_INIT drain; when non-null, registerBlock registers
	 *  the block directly into this event's registry, since this phase's DeferredRegister may already be done. */
	public static net.minecraftforge.registries.RegisterEvent sBlockRegisterEvent = null;
	private static void runDeferredBlockInit(net.minecraftforge.registries.RegisterEvent aEvent) {
		sBlockRegisterEvent = aEvent;
		try {for (Runnable tInit : DEFERRED_BLOCK_INIT) try {tInit.run();} catch(Throwable e) {e.printStackTrace(ERR);} DEFERRED_BLOCK_INIT.clear();}
		finally {sBlockRegisterEvent = null;}
	}
	/** Opens the block-registry event before any DeferredRegister dispatcher, or a block built inside another
	 *  DR supplier would register out of order, changing set names and shifting some recipe inputs. */
	private static void onRegisterEventOpen(net.minecraftforge.registries.RegisterEvent aEvent) {
		if (aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK)) sBlockRegisterEvent = aEvent;
	}
	/** After every DeferredRegister has run: drain the deferred construction queue and close the event. */
	private static void onRegisterEvent(net.minecraftforge.registries.RegisterEvent aEvent) {
		if (aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK)) {
			try {runDeferredBlockInit(aEvent);} finally {sBlockRegisterEvent = null;}
		}
	}

	/** Registers only the block (caller wires its own BlockItem). This loader's registries forbid a direct
	 *  Registry.register call outside the active RegisterEvent's own registry; 26.1.2 has no such wrapper. */
	public static void registerBlockOnly(Block aBlock, String aRegistryName) {
		if (aBlock == null || aRegistryName == null) return;
		if (sBlockRegisterEvent == null) {deferBlockInit(() -> registerBlockOnly(aBlock, aRegistryName)); return;}
		sBlockRegisterEvent.register(net.minecraft.core.registries.Registries.BLOCK
			, new net.minecraft.resources.ResourceLocation(ModIDs.GT, sanitizeRegName(aRegistryName)), () -> aBlock);
	}

	/** Mod bus saved from the constructor, so lazily created sub-namespaces can subscribe to RegisterEvent later. */
	private static IEventBus sModBus = null;
	/** One DeferredRegister<Item> per namespace owner: GT6 lets addons create Items under a foreign modId,
	 *  but DeferredRegister is bound to one namespace, so the center keeps a namespace->registry map. */
	private static final Map<String, DeferredRegister<net.minecraft.world.item.Item>> ITEMS_BY_NS = new HashMap<>();
	static {ITEMS_BY_NS.put(ModIDs.GAPI, ITEMS);}

	private static DeferredRegister<net.minecraft.world.item.Item> itemsFor(String aNamespace) {
		DeferredRegister<net.minecraft.world.item.Item> rReg = ITEMS_BY_NS.get(aNamespace);
		if (rReg == null) {
			rReg = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, aNamespace);
			if (sModBus != null) rReg.register(sModBus);
			ITEMS_BY_NS.put(aNamespace, rReg);
		}
		return rReg;
	}

	/** Bridge for Item registration, called from ST.register(Item, String): registers under the GAPI namespace
	 *  (previously a made-up direct DeferredRegister.registerItem(...) call). */
	public static net.minecraftforge.registries.RegistryObject<Item> registerItem(Item aItem, String aRegistryName) {
		return registerItem(aItem, aRegistryName, ModIDs.GAPI);
	}

	/** Bridge: registers an Item under the owner namespace aModIDOwner (replaces the invented
	 *  3-arg DeferredRegister.registerItem, matching the original GameRegistry.registerItem(item, name, modId)). */
	public static net.minecraftforge.registries.RegistryObject<Item> registerItem(Item aItem, String aRegistryName, String aModIDOwner) {
		return itemsFor(aModIDOwner).register(aRegistryName, () -> aItem);
	}

	/** Lazy registration: the supplier constructs the item at RegisterEvent (registry unfrozen), not eagerly
	 *  in preInit (registry frozen); same trick as fluid-split's source-supplier. */
	public static net.minecraftforge.registries.RegistryObject<Item> registerItemLazy(String aModIDOwner, String aRegistryName, java.util.function.Supplier<? extends Item> aSupplier) {
		return itemsFor(aModIDOwner).register(sanitizeRegName(aRegistryName), aSupplier);
	}

	private static final Map<String, DeferredRegister<net.minecraft.world.level.block.Block>> BLOCKS_BY_NS = new HashMap<>();
	static {BLOCKS_BY_NS.put(ModIDs.GAPI, BLOCKS);}
	private static DeferredRegister<net.minecraft.world.level.block.Block> blocksFor(String aNamespace) {
		DeferredRegister<net.minecraft.world.level.block.Block> rReg = BLOCKS_BY_NS.get(aNamespace);
		if (rReg == null) {rReg = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCKS, aNamespace); if (sModBus != null) rReg.register(sModBus); BLOCKS_BY_NS.put(aNamespace, rReg);}
		return rReg;
	}

	/** Lazy block registration: the supplier constructs the block at RegisterEvent, when createIntrusiveHolder
	 *  and setId are valid; its BlockItem registers itself via registerItemLazy, same form as item/fluid split. */
	public static void registerBlockLazy(String aModIDOwner, String aRegistryName, java.util.function.Supplier<? extends Block> aBlockSupplier) {
		blocksFor(aModIDOwner).register(sanitizeRegName(aRegistryName), aBlockSupplier);
	}

	/** neo's ResourceLocation path only allows [a-z0-9/._-], but GT6 item names contain uppercase.
	 *  Only the registration KEY is sanitized (same trick as FluidGT.safeRegName); identity is by object, not key. */
	public static String sanitizeRegName(String aName) {
		String rName = aName.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
		return rName.isEmpty() ? "unnamed" : rName;
	}

	/** Bridge called from ST.register(Block, String, Class): Block and BlockItem register under
	 *  the same name, matching the original GameRegistry.registerBlock(Block, Class, String). */
	/** Central BlockItem assembly for a block. neo's BlockItem has no (Block)-only constructor, so it's built
	 *  directly with an id derived from the block's already-registered key. */
	public static BlockItem blockItemFor(Block aBlock, Class<? extends BlockItem> aItemClass) {
		if (aItemClass != null && aItemClass != BlockItem.class) {
			BlockItem rItem = (BlockItem)UT.Reflection.callConstructor(aItemClass, 0, null, T, aBlock);
			if (rItem != null) return rItem;
		}
		return new BlockItem(aBlock, new net.minecraft.world.item.Item.Properties());
	}

	public static net.minecraftforge.registries.RegistryObject<Block> registerBlock(Block aBlock, String aRegistryName, Class<? extends BlockItem> aItemClass) {
		if (sBlockRegisterEvent != null) {
			// Called from deferBlockInit during RegisterEvent<Block>: the block is already built, registered directly
			// into the event's own registry; its BlockItem goes into the ITEMS DeferredRegister, resolved later.
			sBlockRegisterEvent.register(net.minecraft.core.registries.Registries.BLOCK, new net.minecraft.resources.ResourceLocation(ModIDs.GT, sanitizeRegName(aRegistryName)), () -> aBlock);
			itemsFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> blockItemFor(aBlock, aItemClass));
			return null;
		}
		net.minecraftforge.registries.RegistryObject<Block> rBlock = blocksFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> aBlock);
		itemsFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> blockItemFor(aBlock, aItemClass));
		return rBlock;
	}

	private LoggerPlayerActivity mPlayerLogger;

	@SuppressWarnings("unchecked")
	// javafml 1.20.1 constructs the @Mod class with a no-arg constructor (the IEventBus form only appeared
	// in 26.x); the mod bus is fetched from the loading context instead, matching the 1.20.1 form.
	// FMLJavaModLoadingContext.getModEventBus()Lnet/minecraftforge/eventbus/api/IEventBus;.
	public GT_API() {
		IEventBus aModBus = FMLJavaModLoadingContext.get().getModEventBus();
		GAPI = this;
		
		if (!MD.ENCHIRIDION.mLoaded) MD.MaCu.mLoaded = F;
		
		// A bunch of Code that is there to statically initialize the Database in the right order and without crashes.
		MT.init();
		// Replaces @SidedProxy: the side-specific proxy is built here, after MT.init(), not inline in the static
		// initializer, which would run class-init before STACKMAPS exists and throw NPE.
		api_proxy = FMLEnvironment.dist.isClient() ? new GT_API_Proxy_Client() : new GT_API_Proxy_Server();
		BI.BAROMETER.toString();
		OP.ore.toString();
		
		// Make sure Icons are initialized.
		Textures.BlockIcons.VOID.toString();
		Textures.ItemIcons .VOID.toString();
		ErrorRenderer.INSTANCE.toString();
		
		// Guess what, I got a random Crash from one of those not being classloaded...
		UT.Entities.class.toString();
		IMTE_CanConnectRedstone.class.toString();
		
		
		// The world is no longer built here: Level.<init> needs the biome registry, which doesn't exist yet at
		// mod-construction time. The dummy world is created once the registry exists, at server start instead.
		
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
		
		// The vanilla-ore-target block (ST.make(Blocks.X) = ItemStack) moved to onLoad, since neo hasn't bound
		// item Holder.components yet during @Mod construction.
		
		// F12: "fixing missing container items" (1.7.10 setContainerItem: mushroom_stew->bowl, potion/experience_bottle->
		// Not needed in neo: vanilla already carries these craft remainders by default; a correct no-op, not a stub.
		// GT6's own items set craftRemainder on registration instead.
		
		// Fixing Max Stacksizes that don't make sense.
		ST.forceProperMaxStacksizes();
		
		// Fixing some Adventure Mode things.
		// The original mutated a foreign object here (bed/sponge -> "axe", tnt -> "pickaxe"); neo has no such
		// mutator. The mod asks its own center instead: all six values live in WD.vanillaPassport.

		try {
			// The Access Transformer should make this work
			Material.tnt.setAdventureModeExempt();
		} catch(Throwable e) {
			UT.Reflection.callMethod(Material.tnt, new String[] {"func_85158_p", "setAdventureModeExempt"}, T, F, F);
			e.printStackTrace(ERR);
		}

		// 1.7.10's reflection hack on a private static "effective tools" field has no neo equivalent: tool
		// effectiveness is data-driven there too, and no such mutable static field exists in the decompile.

		// The mod's own central DeferredRegisters register on the mod bus; the bus is remembered so lazily
		// created sub-namespaces (itemsFor) can subscribe to RegisterEvent in time too.
		sModBus = aModBus;
		ITEMS .register(aModBus);
		BLOCKS.register(aModBus);
		BLOCK_ENTITIES.register(aModBus); // Placeholder MTE_TYPE on RegisterEvent<BlockEntityType>, before the registry freezes.
		ENTITIES.register(aModBus); // EntityType of the falling meta-block, replacing EntityRegistry.registerModEntity.
		SOUND_EVENTS.register(aModBus); // GT6's own sounds: 1.7.10 needed only sounds.json, neo needs a registered SoundEvent.
		// Pair of listeners keeps the block registry open for the whole phase: HIGHEST opens it before any
		// DeferredRegister, LOWEST drains the deferred queue and closes it after them.
		aModBus.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST, GT_API::onRegisterEventOpen);
		aModBus.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, GT_API::onRegisterEvent);
		// Central worldgen adapter (Feature/PlacedFeature/BiomeModifier), on the same mod bus — one subscription point.
		gregapi.worldgen.GT6WorldgenFeature.register(aModBus);
		// Central adapter for custom enchantment effects, on the same mod bus — one subscription point.
		gregapi.enchants.EnchantsGT6.register(aModBus);
		// Chunk-level ore material map, on the same mod bus — one subscription point.
		// (gregapi/block/prefixblock/PrefixBlockOreMap.java).
		gregapi.block.prefixblock.PrefixBlockOreMap.register(aModBus);
		// Central DeferredRegister for GT6's own potion effects (flammable/slippery/conductive/sticky/insanity),
		// since neither IE nor EnviroMine exist for 26.1.2.
		gregapi.potion.MobEffectsGT.register(aModBus);
		// Central DeferredRegisters for fluids (FluidType+Fluid), on the same mod bus, closing a prior registration wiring gap.
		gregapi.fluid.FluidGT.FLUID_TYPES.register(aModBus);
		gregapi.fluid.FluidGT.FLUIDS.register(aModBus);
		// No mod-bus subscription for capabilities here anymore: on 1.20.1 a BlockEntity declares its own via
		// getCapability, with the single bridge at the common root and "what to expose" in GT6FluidCapability.
		// (gregapi/player/EntityFoodTracker.java).
		gregapi.player.EntityFoodTracker.register(aModBus);
		// BUG-145 mirror: the ITEM arm of the fluid view — stacks of live IFluidContainerItem items
		// expose FLUID_HANDLER_ITEM; one listener, declared next to the other capability seams.
		gregapi.fluid.GT6FluidCapability.registerItemCapabilities();
		// On 1.20.1 foreign-block drop handling is delivered via a global loot modifier (Forge 1.20.1 has no
		// drop-list event); the rule lives in GT_API_Proxy.processBlockDrops, only the codec registration is here.
		gregapi.loot.GT6BlockDropsModifier.register(aModBus);
		// Uses the same GLM-serializer registry that GT6BlockDropsModifier already subscribed above.
		// This line only guarantees the second entry (chest_loot) makes it in too.
		gregapi.loot.GT6ChestLootModifier.register();
		// Central crafting-bench dispatcher (CustomRecipe SERIALIZERS), on the same mod bus.
		GT6CraftingDispatcher.register(aModBus);
		// Central GUI MenuType (ContainerCommon), on the same mod bus.
		gregapi.gui.ContainerCommon.register(aModBus);
		// Single dynamic model type GT6BlockModel, registered on the mod bus (client-only): delegated to the
		// client proxy, no-op on server, so common code never loads client-only classes.
		api_proxy.registerClientModels(aModBus);
		// Single tab-filling handler (CreativeTabsGT), replacing scattered setCreativeTab calls; on the same mod bus.
		gregapi.item.CreativeTabsGT.register(aModBus);
		// No separate stack-size-override carrier here anymore: with the branch's Access Transformer,
		// ST.setMaxStackSize mutates the item field directly again, as in 1.7.10.

		// Replaces the @Mod.EventHandler dispatcher: each phase subscribes to the mod bus directly, keeping
		// GT6's original three-phase (Pre/Init/Post) contract over neo's own lifecycle events.
		// PreInit -> FMLConstructModEvent; Init -> FMLCommonSetupEvent; PostInit -> FMLLoadCompleteEvent
		// (decisions/F12-registration-lifecycle.md §4).
		aModBus.addListener(this::onPreLoad);
		aModBus.addListener(this::onLoad);
		aModBus.addListener(this::onPostLoad);
		// AE2 layer: the conditional built-in datapack ae2replacegen (meteorite-generation suppression), on the mod bus.
		aModBus.addListener(this::onAddPackFinders);

		// GT6's server phases (Abstract_Mod is already on neo's native events) subscribe on the game bus, not the mod bus.
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
		// Root cause note: deferred item-init must finish before spawn-area pre-generation runs (see below).
		MinecraftForge.EVENT_BUS.addListener(this::onLevelLoadEarlyItemInit);
		// Reload boundary: /reload recreates the datapack's RecipeMap, so Replace's suppression is reapplied
		// on OnDatapackSyncEvent (player==null means reload), before recipes are sent to the client.
		MinecraftForge.EVENT_BUS.addListener(this::onDatapackSyncReapplySuppression);
	}

	/** Single authoritative drain point for deferred item-init: the old ServerStartingEvent drain ran too late,
	 *  since neo's boot order pre-generates the spawn area before that event fires, leaving GEN_GT empty. */
	/** Water not regenerating on its own is GT6's normal behavior (confirmed live in 1.7.10), not a port artifact.
	 *  The only channel here is the vanilla game rule water_source_conversion, set once at overworld load. */
	private void applyWaterSourceConversionRule(net.minecraft.server.level.ServerLevel aLevel) {
		try {
			boolean tWanted = gregapi.data.CS.WATER_SOURCE_CONVERSION;
			net.minecraft.world.level.GameRules tRules = aLevel.getGameRules();
			if (tRules.getBoolean(net.minecraft.world.level.GameRules.RULE_WATER_SOURCE_CONVERSION) == tWanted) return;
			tRules.getRule(net.minecraft.world.level.GameRules.RULE_WATER_SOURCE_CONVERSION).set(tWanted, aLevel.getServer());
			OUT.println("[GT6] бесконечная вода: правило water_source_conversion = " + tWanted + (tWanted ? " (ванильное поведение по настройке)" : " (вода конечна, как в 1.7.10 с GT6)"));
		} catch (Throwable e) {e.printStackTrace(ERR);}
	}

	public void onLevelLoadEarlyItemInit(net.minecraftforge.event.level.LevelEvent.Load aEvent) {
		if (aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel tLevel && tLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
			applyWaterSourceConversionRule(tLevel);
			runDeferredItemInit();
			// The vanilla furnace's shift-click gate is built by the engine before this data-init runs, while
			// FurnaceRecipes is still empty; rebuilding the propertySet afterward fixes it, idempotently.
			net.minecraft.server.MinecraftServer tServer = tLevel.getServer();
			// Replaces vanilla crafting-bench recipes with ore-versions, the same thing Forge did in 1.7.10.
			// Right here: RecipeManager and the dictionary are both already fully populated; idempotent.
			gregapi.oredict.OreDictionary.initVanillaRecipeReplacements(tServer);
			// Foreign-recipe scans run after role-C (their input is its ore-versions, as in 1.7.10) and before
			// finalizeRecipeLoading below, so recipe-book propertySets/displays see the suppression.
			sCurrentServerForRecipeScan = tServer;
			try {for (Runnable tScan : DEFERRED_RECIPE_SCAN) try {tScan.run();} catch(Throwable e) {e.printStackTrace(ERR);} DEFERRED_RECIPE_SCAN.clear();}
			finally {sCurrentServerForRecipeScan = null;}
			// Datapack side of CR.remove: 1.7.10's remove(...) also stripped vanilla recipes from the live
			// CraftingManager; their neo descendants are suppressed here by the same judge, matches() against the grid.
			if (tServer != null) try {
				java.util.Set<net.minecraft.resources.ResourceLocation> tRemove = new java.util.HashSet<>();
				for (net.minecraft.world.item.ItemStack[] tGrid : gregapi.util.CR.DATAPACK_REMOVALS) {
					net.minecraft.world.inventory.CraftingContainer tInput = gregapi.util.CR.crafting(tGrid);
					for (net.minecraft.world.item.crafting.Recipe<?> tAny : tServer.getRecipeManager().getRecipes()) {
						if (!(tAny instanceof net.minecraft.world.item.crafting.CraftingRecipe tCraft)) continue;
						if (tAny instanceof gregapi.recipes.GT6CraftingDispatcher) continue;
						try {if (tCraft.matches(tInput, tServer.overworld())) tRemove.add(tAny.getId());} catch(Throwable e) {/* A foreign recipe failed matches() — not our judgment to make. */}
					}
				}
				// Second arm of the same class: removal by OUTPUT (CR.delate/CR.remout), matching 1.7.10's remout exactly.
				// Output comes from getResultItem(registryAccess), the descendant of 1.7.10's getRecipeOutput().
				for (net.minecraft.world.item.ItemStack tOut : gregapi.util.CR.DATAPACK_REMOVALS_OUT) {
					for (net.minecraft.world.item.crafting.Recipe<?> tAny : tServer.getRecipeManager().getRecipes()) {
						if (!(tAny instanceof net.minecraft.world.item.crafting.CraftingRecipe tCraft)) continue;
						if (tAny instanceof gregapi.recipes.GT6CraftingDispatcher) continue;
						try {
							net.minecraft.world.item.ItemStack tResult = tCraft.getResultItem(tServer.registryAccess());
							if (gregapi.util.ST.valid(tResult) && gregapi.util.ST.equal(tResult, tOut, T)) tRemove.add(tAny.getId());
						} catch(Throwable e) {/* A foreign recipe failed getResultItem — not our judgment to make. */}
					}
				}
				// Third arm of the same class: removal by recipe TYPE (CR.remoutType). Not drained: it describes a
				// machine, not a one-off request, and is reapplied on every world load.
				if (!gregapi.util.CR.DATAPACK_REMOVALS_TYPE.isEmpty()) {
					for (net.minecraft.world.item.crafting.Recipe<?> tAny : tServer.getRecipeManager().getRecipes()) {
						net.minecraft.resources.ResourceLocation tType = net.minecraftforge.registries.ForgeRegistries.RECIPE_TYPES.getKey(tAny.getType());
						if (tType != null && gregapi.util.CR.DATAPACK_REMOVALS_TYPE.contains(tType.toString())) tRemove.add(tAny.getId());
					}
				}
				gregapi.util.CR.DATAPACK_REMOVALS_OUT.clear();
				gregapi.util.CR.DATAPACK_REMOVALS.clear();
				// Relogging in singleplayer spins up a NEW MinecraftServer with a fresh datapack, but the scan queues
				// were already drained by the first start; the accumulated key set is reapplied here every load.
				tRemove.addAll(SUPPRESSED_DATAPACK_RECIPES);
				// GT6's own recipe suppression must also reach a recipe's ore-VERSION, not just its datapack original:
				// role-C builds that version AFTER loaders already ran their removals, missing it by construction.
				int tDroppedOre = 0;
				for (java.util.Iterator<gregapi.recipes.ICraftingRecipeGT> tIt = gregapi.util.CR.BUFFER.iterator(); tIt.hasNext();) {
					gregapi.recipes.ICraftingRecipeGT tRecipe = tIt.next();
					net.minecraft.resources.ResourceLocation tSource = null;
					if (tRecipe instanceof gregapi.recipes.ShapedOreRecipe tShaped && tShaped.mVanillaReplacement) tSource = tShaped.mSourceId;
					else if (tRecipe instanceof gregapi.recipes.ShapelessOreRecipe tShapeless && tShapeless.mVanillaReplacement) tSource = tShapeless.mSourceId;
					if (tSource != null && tRemove.contains(tSource)) {tIt.remove(); tDroppedOre++;}
				}
				OUT.println("GT_API: ore-версий роли-C снято вслед за подавленным оригиналом (BP-BUG-013): " + tDroppedOre);
				removeDatapackRecipes(tServer, tRemove);
			} catch(Throwable e) {e.printStackTrace(ERR);}
			// Nothing to rebuild on 1.20.1: the static RecipePropertySet finalizeRecipeLoading served doesn't exist here.
			// The old catch-up loot-pool injection is gone too; delivery now goes through the live IGlobalLootModifier.
		} else if (aEvent.getLevel() instanceof net.minecraft.world.level.Level tClientLevel && tClientLevel.isClientSide()) {
			// A remote client has no ServerLevel, so the drain above never ran there, leaving deferred item-init
			// empty on the client. Singleplayer masked it, since the integrated server drains it first.
			runDeferredItemInit();
		}
	}

	/** Replaces @Mod.EventHandler onPreLoad: subscribed in the constructor to FMLConstructModEvent (mod bus).
	 *  Builds the GT6 shim event and passes it to Abstract_Mod.onModPreInit, whose body stays the original. */
	/** Mounts a built-in datapack that empties AE2's ae2:has_meteorites tag, gated by the same key that
	 *  drives the meteoric-iron vein in Loader_Worldgen; AE2 15.4.10 itself has no generation toggle. */
	/** 1.20.1-specific engine seam: ModelBakery unconditionally warns "missing model" for every block/item
	 *  lacking a static model JSON, which GregTech never has (its models are procedural, injected after baking). */
	public void onAddPackFinders(net.minecraftforge.event.AddPackFindersEvent aEvent) {
		// CLIENT_RESOURCES is handled before the server-only return below: a dedicated server never receives
		// this event type at all, and it has nothing to do with the AE2 datapacks handled further down.
		if (aEvent.getPackType() == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) {addProceduralClientModelPack(aEvent); return;}
		if (aEvent.getPackType() != net.minecraft.server.packs.PackType.SERVER_DATA) return;
		if (!MD.AE.mLoaded) return;
		// First pack: suppresses meteorite generation, gated by the master key ReplaceMeteoriteGeneration.
		if (AE2_REPLACE_METEORITE_GENERATION) addBuiltInPack(aEvent, "ae2replacegen", "GT6: AE2 generation replaced by GregTech");
		// Second pack, own key: rewires the one AE2 recipe whose input GregTech took away (the ME Network Tool,
		// whose craft needs a quartz wrench we disabled); kept separate since the two config keys are independent.
		if (AE2_KILL_QUARTZ_TOOLS) addBuiltInPack(aEvent, "ae2gtrecipes", "GT6: AE2 recipes rewired to GregTech inputs");
	}

	/** Mounts one built-in mod datapack. One point for every pack in the port, since assembling a Pack
	 *  on 1.20.1 is non-trivial and repeating that form per pack would only invite drift. */
	private static void addBuiltInPack(net.minecraftforge.event.AddPackFindersEvent aEvent, String aDir, String aTitle) {
		aEvent.addRepositorySource(aConsumer -> {
			try {
				java.nio.file.Path tRoot = net.minecraftforge.fml.ModList.get().getModFileById(ModIDs.GAPI).getFile().findResource(aDir);
				net.minecraft.server.packs.repository.Pack tPack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
					  ModIDs.GAPI + ":" + aDir
					, net.minecraft.network.chat.Component.literal(aTitle)
					, T
					, aPackId -> new net.minecraftforge.resource.PathPackResources(aPackId, T, tRoot)
					, net.minecraft.server.packs.PackType.SERVER_DATA
					, net.minecraft.server.packs.repository.Pack.Position.TOP
					, net.minecraft.server.packs.repository.PackSource.BUILT_IN);
				if (tPack != null) {aConsumer.accept(tPack); OUT.println("GT_API: встроенный датапак " + aDir + " подключён.");}
				else ERR.println("GT_API: встроенный датапак " + aDir + " НЕ создан — Pack.readMetaAndCreate вернул null (нет pack.mcmeta?).");
			} catch(Throwable e) {ERR.println("GT_API: встроенный датапак " + aDir + " не подключён:"); e.printStackTrace(ERR);}
		});
	}

	/** Mounts the second form of the same center — the procedural client pack (see javadoc above).
	 *  Position.BOTTOM keeps the placeholder below any real assets, so a real model always wins. */
	private static void addProceduralClientModelPack(net.minecraftforge.event.AddPackFindersEvent aEvent) {
		aEvent.addRepositorySource(aConsumer -> {
			try {
				net.minecraft.server.packs.repository.Pack tPack = net.minecraft.server.packs.repository.Pack.readMetaAndCreate(
					  ModIDs.GAPI + ":modelplaceholder"
					, net.minecraft.network.chat.Component.literal("GT6: procedural block/item model placeholders")
					, T
					, aPackId -> new GT6ProceduralClientResourcePack(aPackId)
					, net.minecraft.server.packs.PackType.CLIENT_RESOURCES
					, net.minecraft.server.packs.repository.Pack.Position.BOTTOM
					, net.minecraft.server.packs.repository.PackSource.BUILT_IN);
				if (tPack != null) {aConsumer.accept(tPack); OUT.println("GT_API: процедурный пак заглушек модели подключён.");}
				else ERR.println("GT_API: процедурный пак заглушек модели НЕ создан — Pack.readMetaAndCreate вернул null.");
			} catch(Throwable e) {ERR.println("GT_API: процедурный пак заглушек модели не подключён:"); e.printStackTrace(ERR);}
		});
	}

	/** Backing store for the procedural client resource source. Nothing reads the registry in the constructor,
	 *  since the event fires before it's guaranteed full; getResource walks the registry on every call instead. */
	private static final class GT6ProceduralClientResourcePack extends net.minecraft.server.packs.AbstractPackResources {
		private static final byte[] PACK_META = "{\"pack\":{\"description\":\"GT6 procedural model placeholders (blockstates/air, models/item parent air)\",\"pack_format\":15}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		private static final byte[] BLOCKSTATE_JSON = "{\"variants\":{\"\":{\"model\":\"minecraft:block/air\"}}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		private static final byte[] ITEM_MODEL_JSON = "{\"parent\":\"minecraft:block/air\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		private static final String BLOCKSTATES_DIR = "blockstates/", ITEM_MODELS_DIR = "models/item/", JSON_EXT = ".json";

		GT6ProceduralClientResourcePack(String aPackId) {super(aPackId, T);}

		@Override
		public net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream> getRootResource(String... aPath) {
			if (aPath.length == 1 && aPath[0].equals("pack.mcmeta")) return () -> new java.io.ByteArrayInputStream(PACK_META);
			return null;
		}

		@Override
		public net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream> getResource(net.minecraft.server.packs.PackType aType, net.minecraft.resources.ResourceLocation aLoc) {
			if (aType != net.minecraft.server.packs.PackType.CLIENT_RESOURCES || !ModIDs.isGregNamespace(aLoc.getNamespace())) return null;
			String tPath = aLoc.getPath();
			if (tPath.startsWith(BLOCKSTATES_DIR) && tPath.endsWith(JSON_EXT)) {
				String tKey = tPath.substring(BLOCKSTATES_DIR.length(), tPath.length() - JSON_EXT.length());
				if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(new net.minecraft.resources.ResourceLocation(aLoc.getNamespace(), tKey))) return () -> new java.io.ByteArrayInputStream(BLOCKSTATE_JSON);
				return null;
			}
			if (tPath.startsWith(ITEM_MODELS_DIR) && tPath.endsWith(JSON_EXT)) {
				String tKey = tPath.substring(ITEM_MODELS_DIR.length(), tPath.length() - JSON_EXT.length());
				if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(new net.minecraft.resources.ResourceLocation(aLoc.getNamespace(), tKey))) return () -> new java.io.ByteArrayInputStream(ITEM_MODEL_JSON);
				return null;
			}
			return null;
		}

		@Override
		public void listResources(net.minecraft.server.packs.PackType aType, String aNamespace, String aPath, net.minecraft.server.packs.PackResources.ResourceOutput aOutput) {
			if (aType != net.minecraft.server.packs.PackType.CLIENT_RESOURCES || !ModIDs.isGregNamespace(aNamespace)) return;
			// Prefixes match what ModelBakery itself asks for: "blockstates" and "models" (item models under models/item/).
			if (aPath.equals("blockstates")) {
				for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
					net.minecraft.resources.ResourceLocation tKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tBlock);
					if (tKey == null || !tKey.getNamespace().equals(aNamespace)) continue;
					aOutput.accept(new net.minecraft.resources.ResourceLocation(aNamespace, BLOCKSTATES_DIR + tKey.getPath() + JSON_EXT), () -> new java.io.ByteArrayInputStream(BLOCKSTATE_JSON));
				}
			} else if (aPath.equals("models")) {
				for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
					net.minecraft.resources.ResourceLocation tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
					if (tKey == null || !tKey.getNamespace().equals(aNamespace)) continue;
					aOutput.accept(new net.minecraft.resources.ResourceLocation(aNamespace, ITEM_MODELS_DIR + tKey.getPath() + JSON_EXT), () -> new java.io.ByteArrayInputStream(ITEM_MODEL_JSON));
				}
			}
		}

		@Override
		public java.util.Set<String> getNamespaces(net.minecraft.server.packs.PackType aType) {
			return aType == net.minecraft.server.packs.PackType.CLIENT_RESOURCES ? java.util.Set.of(ModIDs.GT, ModIDs.GAPI) : java.util.Set.of();
		}

		@Override
		public void close() {/* Nothing to close here — no files or streams held. */}
	}

	public void onPreLoad(FMLConstructModEvent aModEvent) {runPhaseInModLoadOrder(aModEvent, this, this::onPreLoadPhase);}
	/** Body of the PreInit phase; run by the center Abstract_Mod#runPhaseInModLoadOrder, in mod load order. */
	private void onPreLoadPhase() {
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

	/** Replaces @Mod.EventHandler onLoad: subscribed in the constructor to FMLCommonSetupEvent (mod bus). */
	public void onLoad(FMLCommonSetupEvent aModEvent) {runPhaseInModLoadOrder(aModEvent, this, this::onLoadPhase);}
	/** Body of the Init phase; run by the center Abstract_Mod#runPhaseInModLoadOrder, in mod load order. */
	private void onLoadPhase() {
		// Ore-targets and recipes create an ItemStack, and onLoad still isn't post-bind (components bind at server start).
		// Wrapped in deferItemInit, so it runs at onModServerStarting2 (post-bind).
		deferItemInit(() -> {
		// REMAP-RULES block-flatten (data, not behavior): old snake_case Blocks fields are gone, replaced by
		// neo's real UPPER_SNAKE constants (e.g. RedSand -> RED_SAND).
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

		// Recipe fixes moved from onModPreInit2: they create an ItemStack, impossible in preInit since
		// Holder.components aren't bound yet; possible here, after registration and binding.
		CR.remove(ST.make(Blocks.OAK_PLANKS, 1, 0), ST.make(Blocks.SPRUCE_PLANKS, 1, 0), ST.make(Blocks.BIRCH_PLANKS, 1, 0));
		CR.shaped(ST.make(Blocks.OAK_SLAB, 6, 0), CR.NONE, "WWW", 'W', ST.make(Blocks.OAK_PLANKS, 1, 0));
		// Preventing a Water Dupe by registering this Recipe early so it won't be overridden
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.GLASS_BOTTLE, 1, 0), FL.Water.make(250), NF, ST.make(Items.POTION, 1, 0));
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.POTION, 1, 0), ST.make(Items.GLASS_BOTTLE, 1, 0));
		}); // End of the deferItemInit wrapper for ore-targets/recipes (runs at onModServerStarting2, post-bind).

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
		// runDeferredItemInit moved to onModServerStarting2: onLoad(CommonSetup) isn't post-bind
		// (Holder.components only bind at server start).
	}
	
	// PostInit: subscribed in the constructor to FMLLoadCompleteEvent (mod bus); neo's native event
	// replaces the old FML 1.7.10 complexity around loadComplete.
	public void onPostLoad(FMLLoadCompleteEvent aModEvent) {runPhaseInModLoadOrder(aModEvent, this, () -> onModPostInit(new FMLPostInitializationEvent()));}

	@Override public String getModID() {return MD.GAPI.mID;}
	@Override public String getModName() {return MD.GAPI.mName;}
	@Override public String getModNameForLog() {return "GT_API";}
	@Override public Abstract_Proxy getProxy() {return api_proxy;}

	// Server phases are subscribed in the constructor to MinecraftForge.EVENT_BUS (game bus), not the mod bus.
	public void onServerStarting  (ServerStartingEvent aEvent) {
		// Localization center, server arm. In 1.7.10 name injection lived in shared code, so server strings
		// were human-readable too; the client arm hooks resource loading, which a dedicated server lacks.
		int tInjected = gregapi.lang.LanguageHandler.injectIntoEngine();
		if (tInjected > 0) OUT.println("GT6 localization: имён GT6 дописано в таблицу движка (сервер): " + tInjected);
		onModServerStarting(aEvent);
	}
	public void onServerStarted   (ServerStartedEvent  aEvent) {onModServerStarted(aEvent);}
	public void onServerStopping  (ServerStoppingEvent aEvent) {onModServerStopping(aEvent);}
	public void onServerStopped   (ServerStoppedEvent  aEvent) {onModServerStopped(aEvent);}

	@Override
	@SuppressWarnings({ "resource", "deprecation" })
	public void onModPreInit2(FMLPreInitializationEvent aEvent) {
		// neo signature verified against fml-decompiled InterModComms.
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
		// A subsystem init failing SILENTLY is the same bug class that hid the player-activity log for a long time.
		// Reported through the mod's existing error channel (ERR); no new mechanism added.
		} catch (Throwable e) {ERR.println("GT_API: список материалов (materiallist.log) не открыт — файла не будет"); e.printStackTrace(ERR);}
		
		tFile = new File(DirectoriesGT.LOGS, "oredict.log");
		if (!tFile.exists()) {try {tFile.createNewFile();} catch (Throwable e) {/**/}}
		try {
			tList = ((LogBuffer)ORD).mBufferedLog;
			ORD = new PrintStream(tFile);
			ORD.println("**********************************************************************");
			ORD.println("* This is the complete Log of the GregTech OreDictionary Handler     *");
			ORD.println("**********************************************************************");
			for (String tString : tList) ORD.println(tString);
		} catch (Throwable e) {ERR.println("GT_API: журнал словаря руд (oredict.log) не открыт — записи словаря потеряны"); e.printStackTrace(ERR);}
		
		if (ConfigsGT.GREGTECH.get("general", "LoggingPlayerActivity", !CODE_CLIENT)) {
			tFile = new File(DirectoriesGT.LOGS, "playeractivity_"+(System.currentTimeMillis()/60000)+".log");
			if (!tFile.exists()) {try {tFile.createNewFile();} catch (Throwable e) {/**/}}
			// The actual carrier that surfaced this bug class: a bus-registration exception was silently swallowed,
			// mPlayerLogger stayed null, and the activity log never wrote a line, despite a live config.
			try {mPlayerLogger = new LoggerPlayerActivity(new PrintStream(tFile));} catch (Throwable e) {ERR.println("GT_API: журнал активности игрока не заведён — записей о действиях игроков не будет"); e.printStackTrace(ERR);}
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
		// AE2's own generators are suppressed elsewhere, leaving its network powered only through the engine's
		// FE bridge, closed to non-whitelisted mods by default; raised to true so AE2 has power out of the box.
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
		
		// AE2 layer, same trick as the ic2 flags above: the key is only read when AE2 is loaded, and defaults
		// to true without AE2.
		AE2_REPLACE_METEORITE_GENERATION        = !MD.AE.mLoaded || ConfigsGT.GREGTECH.get("ae2", "ReplaceMeteoriteGeneration", T);
		AE2_KILL_QUARTZ_TOOLS                   = !MD.AE.mLoaded || ConfigsGT.GREGTECH.get("ae2", "DisableAllQuartzToolRecipes", T);

		if (ConfigsGT.GREGTECH.get("general", "disable_STDOUT"             , F)) System.out.close();
		if (ConfigsGT.GREGTECH.get("general", "disable_STDERR"             , F)) System.err.close();
		// 1.7.10 mutated vanilla's mob-spawner block at runtime; neo's Properties are immutable, so config
		// flags are cached instead and applied via PlayerEvent.BreakSpeed / ExplosionEvent in GT_API_Proxy.
		HARDER_MOB_SPAWNERS          = ConfigsGT.GREGTECH.get("general", "hardermobspawners"          , T);
		BLAST_RESISTANT_MOB_SPAWNERS = ConfigsGT.GREGTECH.get("general", "blastresistantmobspawners"  , T);
		
		// Light level of burning fueled machines; 0 = off (strict 1:1), clamped 0-15.
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
		// PacketOreMap: on this branch a chunk capability has no autosync, unlike neo's attachment on branch main.
		// [+112 to +119] = PacketBlockEvent
		// [+104 to +111] = PacketBlockError
		// [+ 72 to + 79] = PacketDeathPoint
		// [-120 to + 71] = PacketSyncData
		// [-128 to -121] = PacketSound
		NW_API = new NetworkHandler(MD.GAPI.mID, "GAPI", new PacketConfig(), new PacketPrefix(), new PacketItemStackChat(), new gregapi.network.packets.PacketOreMap()
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
		// Nothing to call here: the original registered a class; neo registers a BlockEntityType instead,
		// already set up once for the whole MTE hierarchy (MTE_TYPE_HOLDER above).
		// Creating and loading the Lang File.
		if (CODE_CLIENT) {
			tFile = new File(DirectoriesGT.MINECRAFT, "GregTech.lang");
			if (!tFile.exists()) tFile = new File(DirectoriesGT.MINECRAFT, "gregtech.lang");
			LanguageHandler.sLangFile = new ModConfigSpec(tFile);
			LanguageHandler.sUseFile = LanguageHandler.sLangFile.get("EnableLangFile", "UseThisFileAsLanguageFile", F).getBoolean(F);
		}
		// Icon-load queues are cleared on both sides, not just the server: the driver that read them during
		// atlas stitching is dead (icon building is lazy now), but the queue kept being filled forever.
		if (sBlockIconload != null) {sBlockIconload.clear(); sBlockIconload = null;}
		if (sItemIconload  != null) {sItemIconload .clear(); sItemIconload  = null;}
		// Creating and loading the Unification Config.
		OreDictManager.INSTANCE.mUnificationConfig = new Config("Unification.cfg");
		// Initialising the Re-Registrations.
		new LoaderOreDictReRegistrations().run();
		// Register the Falling MetaBlock Entity.
		// Nothing to call here: 1.7.10 registered the entity class right at this point; neo needs the EntityType
		// already in the registry before this phase, so registration moved to the central block above.
		// Initialise Enchantments.
		new Enchantment_WerewolfDamage();
		new Enchantment_EnderDamage();
		new Enchantment_Radioactivity();
		new Enchantment_SlimeDamage();
		// Initialises the Fluid Display Item.
		// Item construction deferred into a DeferredRegister supplier, built on RegisterEvent when the registry
		// is open for the intrusive holder; IL holds the supplier, mStack materializes it lazily at runtime.
		IL.Display_Fluid.set(GT_API.ITEMS.register("gt.display.fluid", ItemFluidDisplay::new));
		// Initialises the Integrated Circuit Item.
		IL.Circuit_Selector.set(GT_API.ITEMS.register("gt.integrated_circuit", ItemIntegratedCircuit::new)); // F12-lazy: construct@RegisterEvent-supplier
		// Initialises the Empty Slot Marker Item.
		IL.Empty_Slot.set(GT_API.ITEMS.register("gt.empty_slot", ItemEmptySlot::new)); // F12-lazy: construct@RegisterEvent-supplier
		// Register the GUI Handler.
		// Recipe fixes (ST.make = ItemStack) moved to onLoad: stacks can't be created in preInit, since
		// Holder.components aren't bound yet.
		
		// The FML reflection hack that force-moved GAPI to the front of activeModList is gone (no neo equivalent).
		// Its job is now done declaratively by the engine's own dependency graph, via depends() above.
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
	public void onModPostInit2(FMLPostInitializationEvent aEvent) {deferItemInit(() -> onModPostInit2Deferred(aEvent));} // PostInit data-init (ST.make/static-init) is deferred to server start (post-bind); LoadComplete itself isn't post-bind.
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
		// The mod owners referenced above don't exist for 26.1.2; the five effects GT6 actually applies are
		// registered by the mod itself (gregapi/potion/MobEffectsGT), matching the IE/EnviroMine reference in behavior.
		if (PotionsGT.ID_FLAMMABLE  < 0) UT.Entities.bindPotionID(PotionsGT.ID_FLAMMABLE  = gregapi.potion.MobEffectsGT.ID_FLAMMABLE , gregapi.potion.MobEffectsGT.FLAMMABLE.get() );
		if (PotionsGT.ID_SLIPPERY   < 0) UT.Entities.bindPotionID(PotionsGT.ID_SLIPPERY   = gregapi.potion.MobEffectsGT.ID_SLIPPERY  , gregapi.potion.MobEffectsGT.SLIPPERY.get()  );
		if (PotionsGT.ID_CONDUCTIVE < 0) UT.Entities.bindPotionID(PotionsGT.ID_CONDUCTIVE = gregapi.potion.MobEffectsGT.ID_CONDUCTIVE, gregapi.potion.MobEffectsGT.CONDUCTIVE.get());
		if (PotionsGT.ID_STICKY     < 0) UT.Entities.bindPotionID(PotionsGT.ID_STICKY     = gregapi.potion.MobEffectsGT.ID_STICKY    , gregapi.potion.MobEffectsGT.STICKY.get()    );
		if (PotionsGT.ID_INSANITY   < 0) UT.Entities.bindPotionID(PotionsGT.ID_INSANITY   = gregapi.potion.MobEffectsGT.ID_INSANITY  , gregapi.potion.MobEffectsGT.INSANITY.get()  );

		EnergyCompat.checkAvailabilities();
		ToolCompat.checkAvailabilities();
		ST.checkAvailabilities();
		
		OUT.println(getModNameForLog() + ": If the Loading Bar somehow Freezes at this Point, then you definetly ran out of Memory or permgenspace, look at the other Logs to confirm it.");
		OreDictManager.INSTANCE.onPostLoad();
		
		ICover tCover = new CoverRedstoneTorch();
		// Block-flatten (data): 1.7.10's separate lit/unlit redstone torch blocks are one neo block with a
		// BlockState "lit" property; the second registration becomes a harmless duplicate key, not lost data.
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
		// Deferred stack-init for items: the single execution point moved to level LOAD, since spawn-area
		// pre-generation runs before ServerStartingEvent and consumes the worldgen registry.
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
		// Restores a contract the original never honored: the diary-writer thread was never stopped in 1.7.10
		// either, and a normal dedicated-server shutdown never calls System.exit, so it kept the JVM alive.
		if (mPlayerLogger != null) mPlayerLogger.stop();
	}
	
	// neo has no numeric block/item ids, so there's no neo analog of FMLModIdMappingEvent; this method
	// isn't wired to any bus, kept 1:1 and callable by hand if ever needed.
	// gregapi.api.FMLModIdMappingEvent).
	public void onIDChangingEvent(FMLModIdMappingEvent aEvent) {
		// Fixing missing Blocks caused by DragonAPI. The Issue is more complicated but it should fix some part of it.
		// This DragonAPI fix depends on 1.7.10 Forge's numeric Block.blockRegistry; NeoForge has no numeric
		// block ids at all, so there's no 1:1 form of this piece; the rest below is kept 1:1.

		OUT.println(getModNameForLog() + ": Remapping ItemStackMaps due to ID Map change. Those damn Items should have a consistent Hashcode, but noooo, ofcourse they break Basic Code Conventions! Thanks Forge and Mojang!");
		
		for (Map<ItemStackContainer, ?> tMap : STACKMAPS) UT.Code.reMap(tMap);
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onIDChanging(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
}
