/**
 * Copyright (c) 2026 GregTech-6 Team
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

import cofh.lib.util.ComparableItem;
// net.neoforged.fml.Logging (was imported, .severe(String) was called) — not a logger, a container of log4j Marker constants
// (checked, fml-decompiled/net/neoforged/fml/Logging.java) — .severe(...) does not exist there; replaced with the already
// centralized ERR.println(...) (gregapi.data.CS), used nearby with the same text.
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import ganymedes01.etfuturum.entities.EntityHusk;
import ganymedes01.etfuturum.entities.EntityStray;
import ganymedes01.etfuturum.entities.EntityZombieVillager;
import ganymedes01.etfuturum.recipes.BlastFurnaceRecipes;
import ganymedes01.etfuturum.recipes.SmokerRecipes;
import gregapi.api.Abstract_Mod;
import gregapi.api.Abstract_Proxy;
import gregapi.block.*;
import gregapi.block.metatype.BlockBasePlanks;
import gregapi.block.misc.BlockBaseBale;
import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.block.prefixblock.PrefixBlockTileEntity;
import gregapi.block.tree.BlockBaseBeam;
import gregapi.block.tree.BlockBaseLog;
import gregapi.block.tree.BlockBaseSapling;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.code.ItemNBT;
import gregapi.code.ItemStackContainer;
import gregapi.data.*;
import gregapi.enchants.Enchantment_WerewolfDamage;
import gregapi.item.IItemBeaconPayment;
import gregapi.item.IItemNoGTOverride;
import gregapi.item.IItemProjectile;
import gregapi.item.IItemProjectile.EntityProjectile;
import gregapi.item.IItemRottable.RottingUtil;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.MultiItemRandom;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.IBehavior;
import gregapi.item.multiitem.tools.IToolStats;
import gregapi.network.packets.PacketConfig;
import gregapi.network.packets.PacketDeathPoint;
import gregapi.network.packets.PacketPrefix;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.listeners.IOreDictListenerItem;
import gregapi.player.EntityFoodTracker;
import gregapi.random.IHasWorldAndCoords;
import gregapi.recipes.ICraftingRecipeGT;
import gregapi.tileentity.*;
import gregapi.tileentity.inventories.ITileEntityBookShelf;
import gregapi.util.*;
import gregapi.wooddict.BeamEntry;
import gregapi.wooddict.WoodDictionary;
import gregapi.wooddict.WoodEntry;
import gregapi.worldgen.GT6WorldGenerator;
import gregtech.items.behaviors.Behavior_Gun;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.BaseRailBlock;
import gregapi.block.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent.EntityConstructing;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.ArrowNockEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import gregapi.recipes.ShapedOreRecipe;
import gregapi.recipes.ShapelessOreRecipe;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import twilightforest.entity.boss.EntityTFMinoshroom;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F12 (decisions/F12-registration-lifecycle.md, revision R3): the constructor used to carry two invented
 * APIs — {@code DeferredRegister.registerFuelHandler(this)} and {@code DeferredRegister.registerWorldGenerator(this, weight)}
 * — no such methods exist on the NeoForge DeferredRegister (checked, neoforge-decompiled). The class also
 * illegally "implements" two concrete classes, {@code FurnaceFuelBurnTimeEvent} and {@code Feature}
 * (1.7.10 {@code IFuelHandler}/{@code IWorldGenerator} were mechanically renamed by the type dictionary to
 * engine events/classes — that cannot compile as-is). Both 1.7.10 mechanisms are not a "registry
 * registration" but a subscription to an interface dispatcher; their neo equivalent is a plain {@code @SubscribeEvent}
 * on this same class, which is ALREADY registered on {@code NeoForge.EVENT_BUS} below (a single subscription
 * center, not scattered across the codebase). The body of {@link #getBurnTime(net.minecraft.world.item.ItemStack)}
 * is untouched (1:1) — only wired through {@link #onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent)}.
 * WorldGen part: F6 is implemented ({@code decisions/F6-worldgen.md}) — the old stub method {@code generate}
 * (was the body of {@code IWorldGenerator.generate}, called through the invented
 * {@code DeferredRegister.registerWorldGenerator}) was removed together with the commented-out bridge sketch
 * to {@code PopulateChunkEvent} below (which also does not exist in neo as such) — the real entry point is now
 * {@link gregapi.worldgen.GT6WorldgenFeature#place}, registered centrally through
 * {@link gregapi.worldgen.GT6WorldgenFeature#register} (called from the {@code GT_API} constructor).
 * The dispatcher itself, {@link gregapi.worldgen.GT6WorldGenerator#generate(net.minecraft.world.level.Level,int,int,boolean)},
 * is not rewritten — only the call site is.
 *
 * F-GUI (the "GUI/menu" seam, revised): the constructor illegally "implements" {@code IContainerFactory}
 * ({@code net.neoforged.neoforge.network.IContainerFactory<T extends AbstractContainerMenu>} —
 * a generic factory for ONE container type, {@code create(int,Inventory,RegistryFriendlyByteBuf):T}, whose signature
 * does NOT MATCH {@code getServerGuiElement(int,Player,Level,int,int,int):Object}; it was a leftover stub that
 * never actually implemented the interface) — removed. The former 1.7.10 {@code implements IGuiHandler}
 * (Forge network registry, auto-dispatcher {@code player.openGui(mod,id,...)}) does not exist in neo at all —
 * the {@code id → getGUIServer} route was moved into the SINGLE center {@link gregapi.gui.GT6MenuProvider} (server-side
 * opening) + {@link gregapi.gui.ContainerCommon#createFromNetwork} (client-side container reconstruction);
 * {@code getServerGuiElement} was removed here (not duplicated — it was exactly the same line {@code WD.te+getGUIServer},
 * now living in one place). {@link #getClientGuiElement} is left as-is (not {@code @Override} — there is no target)
 * — an anchor for the client-render phase (F14 gui-client-screen — CLIENT, server-side GUI logic works; the client
 * screen is client-render, not yet headless-verified); the GUI logic itself is not touched.
 */
public abstract class GT_API_Proxy extends Abstract_Proxy {
	public GT_API_Proxy() {
		// F7 (contract seam, invisible to the compiler): neo EventBus.register(this) is FORBIDDEN — rule "the supertype of
		// the registered object must not carry @SubscribeEvent" (fml EventBus.java:117-126), while the base class holds
		// all handlers centrally (philosophy "one place"). The mechanism lives in Abstract_Proxy.registerSubscribeEvents() —
		// one per-method addListener for the whole mod (the same trick gregtech.GT_Proxy uses).
		registerSubscribeEvents();
	}

	/**
	 * SECOND TRANSLATION CARRIER (MODCOMPAT-014) — the side decides whether it even exists.
	 *
	 * <p>On the client there are TWO translation tables: the global {@code Language.getInstance()}, where the
	 * localization center installs its overlay, and a separate pointer {@code I18n} ({@code I18n.java:11}), which
	 * the engine sets only in {@code LanguageManager.apply:66-68}. Third-party mods ask the second one, so after
	 * EVERY refill they must be reconciled to a single overlay — otherwise only the first one sees GT6 names.
	 * On a dedicated server the second carrier does not exist: this stays empty here, and the client-only type is
	 * not pulled in (defect class BUG-092 — a client-only type in a common class kills the dedicated server).
	 */
	public void syncClientI18n() {/* server: no second translation carrier */}

	/** Language the player picked in the game options; null on a dedicated server, which has no player language. */
	public String selectedLanguage() {return null;}

	/**
	 * F12/R3 bridge: replaces the invented {@code DeferredRegister.registerFuelHandler(this)}. The event
	 * {@link FurnaceFuelBurnTimeEvent} is posted on {@code NeoForge.EVENT_BUS} (checked, event class javadoc)
	 * — that same bus already listens to {@code this} (see constructor), so a plain
	 * {@code @SubscribeEvent} is enough, no separate registration needed.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent aEvent) {
		int tBurnTime = getBurnTime(aEvent.getItemStack());
		if (tBurnTime > 0) aEvent.setBurnTime(tBurnTime);
	}

	/** Beacon payment bridge — revival of the 1.7.10 Forge hook {@code Item.isBeaconPayment(ItemStack)} (Forge Item.java:1482).
	 *  In 1.7.10 the beacon slot asked the item itself (TileEntityBeacon.isItemValidForSlot:409); in neo payment is a tag
	 *  {@code ItemTags.BEACON_PAYMENT_ITEMS} on the Item (BeaconMenu:33,165), the material in the stack data is invisible to the tag.
	 *  Central predicate: the vanilla tag OR the per-stack answer of the contract carrier {@link IItemBeaconPayment}
	 *  (currently PrefixItem, body 1:1 with the original). GT6 items do NOT populate the tag — otherwise the beacon would
	 *  accept low-value materials too, wider than the original (user decision 2026-07-30). */
	public static boolean isBeaconPayment(ItemStack aStack) {
		return aStack.is(net.minecraft.tags.ItemTags.BEACON_PAYMENT_ITEMS) || (aStack.getItem() instanceof IItemBeaconPayment tItem && tItem.isBeaconPayment(aStack));
	}

	/** NAMED, NOT ANONYMOUS: an anonymous subclass forces javac to COPY the parent constructor's
	 *  parameter names from the engine artifact into its own. If the artifact carries
	 *  obfuscated names (and it does when unpacked on a clean machine — caught by the
	 *  2026-08-20 release build: "variable o is already defined in constructor"), two parameters
	 *  end up with the same name and compilation fails. A named class declares its own parameters
	 *  and does not depend on the artifact's names. Behavior unchanged. */
	private static final class BeaconPaymentSlot extends net.minecraft.world.inventory.Slot {
		BeaconPaymentSlot(net.minecraft.world.Container aContainer, int aSlot, int aX, int aY) {super(aContainer, aSlot, aX, aY);}
		@Override public boolean mayPlace(ItemStack aStack) {return isBeaconPayment(aStack);}
		@Override public int getMaxStackSize() {return 1;} // same as PaymentSlot (BeaconMenu:170)
	}

	/** Bridge arm: replaces slot 0 of the vanilla {@code BeaconMenu} with a slot carrying the central predicate. All
	 *  click paths go through {@code slots.get(index).mayPlace} (AbstractContainerMenu:356,382,452,485,494,693), the field
	 *  {@code BeaconMenu.paymentSlot} keeps working (same Container, item return in {@code removed()}
	 *  untouched). Client-side arm — {@code GT_API_Proxy_Client.onScreenOpening} (the client builds its OWN
	 *  menu instance over the network, the server-side swap does not reach it). */
	public static void wrapBeaconPaymentSlot(net.minecraft.world.inventory.AbstractContainerMenu aMenu) {
		if (!(aMenu instanceof net.minecraft.world.inventory.BeaconMenu)) return;
		net.minecraft.world.inventory.Slot tOld = aMenu.slots.get(0);
		net.minecraft.world.inventory.Slot tNew = new BeaconPaymentSlot(tOld.container, tOld.getContainerSlot(), tOld.x, tOld.y);
		tNew.index = tOld.index;
		aMenu.slots.set(0, tNew);
	}

	/** Server-side arm: {@code PlayerContainerEvent.Open} fires after the menu is assembled (ServerPlayer.java:1458). */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onContainerOpen(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open aEvent) {
		wrapBeaconPaymentSlot(aEvent.getContainer());
	}

	public int addArmor(String aPrefix) {
		return 0;
	}
	
	public Player getThePlayer() {
		return null;
	}

	/** S6: the client-only {@code Minecraft.getInstance().isSingleplayer()} cannot be called from common code (the
	 *  {@code Minecraft} class does not exist on dedicated). Center of side-splitting (same trick as {@link #getThePlayer()}): server = F. */
	public boolean isSingleplayer() {
		return F;
	}

	/** S6: reading an assets resource (icon PNG for average color) goes through the client {@code Minecraft.getResourceManager()};
	 *  not callable from common code (dedicated has no Minecraft/assets). Center: server = null (item assets do not exist server-side). */
	public java.io.InputStream getResourceStream(net.minecraft.resources.Identifier aRL) {
		return null;
	}

	public boolean sendUseItemPacket(Player aPlayer, Level aWorld, ItemStack aStack) {
		return F;
	}
	
	/** F-GUI: client-render anchor (F14 gui-client-screen — CLIENT client-render phase) — not {@code @Override}, no target
	 *  (see class javadoc); the server-side twin {@code getServerGuiElement} was removed — center is in
	 *  {@link gregapi.gui.GT6MenuProvider}/{@link gregapi.gui.ContainerCommon#createFromNetwork}. */
	public Object getClientGuiElement(int aGUIID, Player aPlayer, Level aWorld, int aX, int aY, int aZ) {
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		return tTileEntity instanceof ITileEntityGUI ? ((ITileEntityGUI)tTileEntity).getGUIClient(aGUIID, aPlayer) : null;
	}
	
	private File mSaveLocation = null;
	
	/**
	 * saves Data whenever Save File Location changes or if aForceSave is passed, usually by the minutely Autosave.
	 */
	public boolean checkSaveLocation(File aSaveLocation, boolean aForceSave) {
		boolean tSave = (aForceSave || aSaveLocation == null), tLoad = (mSaveLocation == null);
		// Did Save Files swap secretly? Can happen in Singleplayer with the popular Forge Monopoly Bug: "Go directly to the Main Menu. Do not enter your World. Do not collect 200 Blocks."
		if (CODE_CLIENT && aSaveLocation != null && !aSaveLocation.equals(mSaveLocation)) tSave = tLoad = T;
		
		if (tSave && mSaveLocation != null) {
			// Only print this if it is not the minutely Autosave.
			if (aSaveLocation == null) OUT.println("Saving  World! " + mSaveLocation.getName());// else DEB.println("Autosave!      " + mSaveLocation.getName());
			// Make the Folder to drop the Save Files into.
			new File(mSaveLocation, "gregtech").mkdirs();
			// Call the Save Function in all the things that need it.
			GarbageGT.onServerSave(mSaveLocation);
			MultiTileEntityRegistry.onServerSave(mSaveLocation);
		}
		mSaveLocation = aSaveLocation;
		if (tLoad && mSaveLocation != null) {
			OUT.println("Loading World! " + mSaveLocation.getName());
			// Make the Folder to uhh wait why is that needed? Probably helps preventing Issues though, so why not.
			new File(mSaveLocation, "gregtech").mkdirs();
			// Call the Load Function in all the things that need it.
			GarbageGT.onServerLoad(mSaveLocation);
			MultiTileEntityRegistry.onServerLoad(mSaveLocation);
		}
		return tSave || tLoad;
	}
	
	@Override
	public void onProxyBeforeServerStarted(Abstract_Mod aMod, ServerStartedEvent aEvent) {
		SERVER_TIME = 0;
		MultiTileEntityRegistry.onServerStart();
		// Fluid role passport sentinel (BUG-120): the "own fluid in the environment tag" role is promised by data
		// (tags/fluid/water.json) — a code/file desync must scream into the log, not silently kill swimming.
		gregapi.block.fluid.BlockFluidBaseGT.validateEngineRoles();
	}
	// [GT6-STACKPROBE] removed (§9, BUG-041 cleanup — reproducing the cleanup of a parallel agent whose staged variant
	// carried the tick-machine 045 regression and was not taken; the cleanup itself is his work, only repeated here).

	
	@Override
	public void onProxyAfterServerStopping(Abstract_Mod aMod, ServerStoppingEvent aEvent) {
		checkSaveLocation(null, T);
		MultiTileEntityRegistry.onServerStop();
	}

	/**
	 * F3-render (client): a single subscription point for client model types on the mod-bus. No-op on the server
	 * (common code does not load client-only classes). The client proxy registers {@code GT6BlockModel.Unbaked}
	 * through {@code RegisterBlockStateModels} (decisions/F3-render.md §2.1). Centralization 1:1 — one type for the whole mod.
	 */
	public void registerClientModels(net.neoforged.bus.api.IEventBus aModBus) {/* server: no-op */}

	/**
	 * BUG-056: open the "all recipes of this machine" screen for the player. In 1.7.10 the NEI mod itself did this
	 * ({@code GuiCraftingRecipe.openRecipeGui(mNameNEI)}, called from {@code RecipeMap.openNEI}); in 26.1.2
	 * JEI took over its role, and opening the screen is a purely CLIENT-SIDE action. Common code (RecipeMap) must not
	 * see client-only JEI classes, so the call goes through the proxy — the same trick as
	 * {@link #registerClientModels}. Server: no-op, still returns false as before.
	 */
	public boolean openRecipeGui(String aNameNEI) {return false;}

	/** 1.7.10 EntityPlayer.displayGUIBook: the CLIENT override opened a book screen with the PASSED stack
	 *  (EntityPlayerSP:379-391), the server side was an empty body (EntityPlayer:1259). Neo openItemGui reads
	 *  only book components and the held item, so GT6 NBT books never opened. Same proxy seam as openRecipeGui. */
	public void displayBook(net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.item.ItemStack aStack, boolean aWritable) {/* server: no-op, 1:1 */}
	
	// DimensionManager (1.7.10 Forge) has no neo equivalent (not found in neo-decompiled, neoforge-decompiled, or fml-decompiled) —
	// the real neo path to the current save root: ServerLevel.getServer().getWorldPath(LevelResource.ROOT) (checked, MinecraftServer.java:2058 + LevelResource.java:16).
	@SubscribeEvent(priority = EventPriority.LOWEST) public void onWorldLoad  (LevelEvent.Load   aEvent) {if (aEvent.getLevel() instanceof ServerLevel tLevel) checkSaveLocation(tLevel.getServer().getWorldPath(LevelResource.ROOT).toFile(), F);}
	//@SubscribeEvent(priority = EventPriority.LOWEST) public void onWorldUnload(WorldEvent.Unload aEvent) {checkSaveLocation(DimensionManager.getCurrentSaveRootDirectory(), F);}
	//@SubscribeEvent(priority = EventPriority.LOWEST) public void onWorldSave  (WorldEvent.Save   aEvent) {checkSaveLocation(DimensionManager.getCurrentSaveRootDirectory(), F);}
	
	public  static final List<ITileEntityServerTickPre    > SERVER_TICK_PRE                = new ArrayListNoNulls<>(), SERVER_TICK_PR2  = new ArrayListNoNulls<>();
	public  static final List<ITileEntityServerTickPost   > SERVER_TICK_POST               = new ArrayListNoNulls<>(), SERVER_TICK_PO2T = new ArrayListNoNulls<>();
	public  static final List<ITileEntityMobSpawnInhibitor> MOB_SPAWN_INHIBITORS           = new ArrayListNoNulls<>();
	public  static       List<IHasWorldAndCoords>           DELAYED_BLOCK_UPDATES          = new ArrayListNoNulls<>();
	private static       List<IHasWorldAndCoords>           DELAYED_BLOCK_UPDATES_2        = new ArrayListNoNulls<>();
	/** F-tree (BUG-005): forced decay of VANILLA leaves for WD.leafdecay — neo split the 1.7.10 updateTick channel
	 *  (scheduled tick = DISTANCE recompute, decay = randomTick); entries {ServerLevel, BlockPos, Long deadline(SERVER_TIME), Integer attempt}. */
	public  static final List<Object[]>                     DELAYED_LEAF_DECAYS            = new ArrayListNoNulls<>();
	public  static       List<ITileEntityScheduledUpdate>   SCHEDULED_TILEENTITY_UPDATES   = new ArrayListNoNulls<>();
	private static       List<ITileEntityScheduledUpdate>   SCHEDULED_TILEENTITY_UPDATES_2 = new ArrayListNoNulls<>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onServerTick(ServerTickEvent aEvent) {
		TOOL_SOUNDS = TOOL_SOUNDS_SETTING;
		
		// Fixing a Thaumcraft Bug in its Loot Bags.
		ST.fixBookStacks();
		
		{ // ServerTickEvent is always server-side (checked, javadoc net.neoforged.neoforge.event.tick.ServerTickEvent) — replaces the dead aEvent.side.isServer()
			// Try acquiring the Lock within 10 Milliseconds. Otherwise fuck anyone who locks it up for too long, or any other faulty reason MC doesn't work.
			try {TICK_LOCK.tryLock(10, TimeUnit.MILLISECONDS);} catch (Throwable e) {e.printStackTrace(ERR);} finally {if (TICK_LOCK.isHeldByCurrentThread()) TICK_LOCK.unlock();}

			// Making sure it is being free'd up in order to prevent exploits or Garbage Collection mishaps.
			LAST_BROKEN_TILEENTITY.set(null);

			if (aEvent instanceof ServerTickEvent.Pre) { // was aEvent.phase == ServerTickEvent.START — neo splits START/END into Pre/Post (checked, ServerTickEvent.java)
				gt6ChunkFinishTick();
				SYNC_SECOND = (SERVER_TIME % 20 == 0);

				if (SERVER_TIME++ == 0) {
					// Initial Save Data check
					// DimensionManager has no neo equivalent (see onWorldLoad above) — the real path is through ServerTickEvent itself.
					checkSaveLocation(aEvent.getServer().getWorldPath(LevelResource.ROOT).toFile(), F);
					
					// Unification Stuff
					HashSetNoNulls<ItemStack> tStacks = new HashSetNoNulls<>(10000);
					
					if (MD.IC2.mLoaded) try {
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.cannerBottle        .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.centrifuge          .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.compressor          .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.extractor           .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.macerator           .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.metalformerCutting  .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.metalformerExtruding.getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.metalformerRolling  .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.matterAmplifier     .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					for (ic2.api.recipe.RecipeOutput tRecipe : ic2.api.recipe.Recipes.oreWashing          .getRecipes().values()) for (ItemStack tStack : tRecipe.items) tStacks.add(tStack);
					} catch(Throwable e) {e.printStackTrace(ERR);}
					
					if (MD.RC.mLoaded) {
					try {for (Object  tRecipe : mods.railcraft.api.crafting.RailcraftCraftingManager.blastFurnace  .getRecipes   ()) tStacks.add((ItemStack)UT.Reflection.getFieldContent(tRecipe, "output"));} catch(Throwable e) {e.printStackTrace(ERR);}
					try {for (Object  tRecipe : mods.railcraft.api.crafting.RailcraftCraftingManager.cokeOven      .getRecipes   ()) tStacks.add((ItemStack)UT.Reflection.getFieldContent(tRecipe, "output"));} catch(Throwable e) {e.printStackTrace(ERR);}
					try {for (Object  tRecipe : mods.railcraft.api.crafting.RailcraftCraftingManager.rockCrusher   .getRecipes   ()) for (Map.Entry<ItemStack, Float> tEntry : (List<Map.Entry<ItemStack, Float>>)UT.Reflection.getFieldContent(tRecipe, "outputs")) tStacks.add(tEntry.getKey());} catch(Throwable e) {e.printStackTrace(ERR);}
					try {for (net.minecraft.world.item.crafting.Recipe tRecipe : mods.railcraft.api.crafting.RailcraftCraftingManager.rollingMachine.getRecipeList()) if (tRecipe != null) tStacks.add((ItemStack)UT.Reflection.getFieldContent(tRecipe, "output"));} catch(Throwable e) {e.printStackTrace(ERR);}
					}
					
					if (MD.TE.mLoaded && ALWAYS_FALSE) {
						List<Map> tMaps = new ArrayListNoNulls<>();
						List<Set> tSets = new ArrayListNoNulls<>();
						
						for (String tClassName : new String[] {"cofh.thermalexpansion.util.crafting.InsolatorManager", "cofh.thermalexpansion.util.crafting.ChargerManager", "cofh.thermalexpansion.util.crafting.ExtruderManager", "cofh.thermalexpansion.util.crafting.PrecipitatorManager", "cofh.thermalexpansion.util.crafting.TransposerManager", "cofh.thermalexpansion.util.crafting.CrucibleManager", "cofh.thermalexpansion.util.crafting.SmelterManager", "cofh.thermalexpansion.util.crafting.SawmillManager", "cofh.thermalexpansion.util.crafting.PulverizerManager", "cofh.thermalexpansion.util.crafting.FurnaceManager"}) {try {
							Class tClass = Class.forName(tClassName);
							Object
							tObject = UT.Reflection.getFieldContent(tClass, "recipeMap", T, F);
							if (tObject instanceof Map) tMaps.add((Map)tObject);
							tObject = UT.Reflection.getFieldContent(tClass, "recipeMapFill", T, F);
							if (tObject instanceof Map) tMaps.add((Map)tObject);
							tObject = UT.Reflection.getFieldContent(tClass, "recipeMapExtraction", T, F);
							if (tObject instanceof Map) tMaps.add((Map)tObject);
							tObject = UT.Reflection.getFieldContent(tClass, "validationSet", T, F);
							if (tObject instanceof Set) tSets.add((Set)tObject);
							tObject = UT.Reflection.getFieldContent(tClass, "lockSet", T, F);
							if (tObject instanceof Set) tSets.add((Set)tObject);
						} catch(Throwable e) {e.printStackTrace(ERR);}}
						
						for (Map tMap : tMaps) {
							try {for (Object tCompStack : tMap.keySet()) if (tCompStack instanceof ComparableItem) {
								ItemStack tStack = OM.get(ST.make(((ComparableItem)tCompStack).item, 1, ((ComparableItem)tCompStack).metadata));
								if (ST.valid(tStack)) {
									((ComparableItem)tCompStack).item     = ST.item_(tStack);
									((ComparableItem)tCompStack).metadata = ST.meta_(tStack);
								}
							}} catch(Throwable e) {e.printStackTrace(ERR);}
							UT.Code.reMap(tMap);
						}
						
						for (Set tSet : tSets) {
							try {for (Object tCompStack : tSet) if (tCompStack instanceof ComparableItem) {
								ItemStack tStack = OM.get(ST.make(((ComparableItem)tCompStack).item, 1, ((ComparableItem)tCompStack).metadata));
								if (ST.valid(tStack)) {
									((ComparableItem)tCompStack).item     = ST.item_(tStack);
									((ComparableItem)tCompStack).metadata = ST.meta_(tStack);
								}
							}} catch(Throwable e) {e.printStackTrace(ERR);}
							UT.Code.reMap(tSet);
						}
					}
					
					// EVENTS impossible-1:1 (neo model): ChestGenHooks (1.7.10 Forge runtime chest-content registry) removed — vanilla loot
					// is fully data-driven (JSON LootTable), no runtime "GenHooks" exists. OreDict unification of vanilla chest loot in neo is not
					// a registry mutation but a GlobalLootModifier (a separate data-driven subsystem); the IE part is foreign (absent). Correctly
					// disabled, no surrogate. GT6's own loot (task F-loot) is already on the neo LootTable.
					// for (String tLootList : ST.LOOT_TABLES) for (WeightedRandomChestContent tContent : ChestGenHooks.getInfo(tLootList).getItems(RNGSUS)) tStacks.add(tContent.theItemId);
					//
					// if (MD.IE.mLoaded) try {
					// 	for (WeightedRandomChestContent tContent : ((ChestGenHooks)UT.Reflection.getFieldContent("blusunrize.immersiveengineering.common.world.VillageEngineersHouse", "crateContents")).getItems(RNGSUS)) {
					// 		if (OM.is("ingotAluminium", tContent.theItemId)) {
					// 			ST.set(tContent.theItemId, OP.ingot.mat(MT.Constantan, 1));
					// 		} else {
					// 			tStacks.add(tContent.theItemId);
					// 		}
					// 	}
					// } catch(Throwable e) {
					// 	e.printStackTrace(ERR);
					// }

					// EVENTS impossible-1:1 (neo model): 1.7.10 mutated a flat Map<ItemStack,ItemStack> of smelting recipes to unify outputs.
					// neo's RecipeManager is typed (RecipeType.SMELTING, RecipeHolder<SmeltingRecipe>) and recipes are IMMUTABLE —
					// the output of an existing recipe cannot be mutated at runtime (enumerable via getAllRecipesFor, not changeable).
					// Unifying smelting outputs in neo means replacing the recipe/datagen, not mutation — out of scope here. Correctly disabled.
					// for (Object tStack : RecipeManager.smelting().getSmeltingList().values()) tStacks.add((ItemStack)tStack);
					
					if (MD.EtFu.mLoaded) {
						boolean tSuccess = F;
						
						if (!tSuccess) try {
							Map
							tMap = ((Map)UT.Reflection.getFieldContent(SmokerRecipes.smelting(), "smeltingList", T, D1));
							if (tMap != null) {for (Object tStack : tMap.values()) tSuccess |= tStacks.add((ItemStack)tStack);}
							tMap = ((Map)UT.Reflection.getFieldContent(BlastFurnaceRecipes.smelting(), "smeltingList", T, D1));
							if (tMap != null) {for (Object tStack : tMap.values()) tSuccess |= tStacks.add((ItemStack)tStack);}
						} catch(Throwable e) {if (D1) e.printStackTrace(ERR);}
						
						if (!tSuccess) ERR.println("Et Futurum Requiem needs to be updated!");
					}
					
					for (ICraftingRecipeGT tRecipe : CR.list()) if (tRecipe != null) tStacks.add(tRecipe.getRecipeOutput());
					
					for (ItemStack tOutput : tStacks) {
						if (OreDictManager.INSTANCE.isOreDictItem(tOutput)) {
							ERR.println("GT-ERR-01: @ " + ST.item_(tOutput).getDescriptionId() + "   " + tOutput.getDisplayName());
							ERR.println("GT-ERR-01: @ " + ST.item_(tOutput).getDescriptionId() + "   " + tOutput.getDisplayName());
							if (CS.CODE_CLIENT) {
								ERR.println("A Recipe used an OreDict Item as Output directly, without copying it before!!! This is a typical CallByReference/CallByValue Error");
								ERR.println("Said Item will be renamed to make the invalid Recipe visible, so that you can report it properly.");
								ERR.println("Please check all Recipes outputting this Item, and report the Recipes to their Owner.");
								ERR.println("The Owner of the ==>RECIPE<==, NOT the Owner of the Item, which has been mentioned above!!!");
								ERR.println("And ONLY Recipes which are ==>OUTPUTTING<== the Item, sorry but I don't want failed Bug Reports.");
								ERR.println("GregTech just reports this Error to you, so you can report it to the Mod causing the Problem.");
								ERR.println("Even though I make that Bug visible, I can not and will not fix that for you, that's for the causing Mod to fix.");
								ERR.println("And speaking of failed Reports:");
								ERR.println("Both IC2 and GregTech CANNOT be the CAUSE of this Problem, so don't report it to either of them.");
								ERR.println("I REPEAT, BOTH, IC2 and GregTech CANNOT be the source of THIS BUG. NO MATTER WHAT.");
								ERR.println("Asking in the IC2 Forums, which Mod is causing that, won't help anyone, since it is not possible to determine, which Mod it is.");
								ERR.println("If it would be possible, then I would have had added the Mod which is causing it to the Message already. But it is not possible.");
								ERR.println("Sorry, but this Error is serious enough to justify this Wall-O-Text and the partially allcapsed Language.");
								ERR.println("Also it is a Ban Reason on the IC2-Forums to seriously post this Text. We all know about its existence.");
								
								tOutput.set(DataComponents.CUSTOM_NAME, Component.literal("ERROR!")); // was setStackDisplayName (1.7.10) — neo: DataComponents.CUSTOM_NAME (checked, ItemStack.java:819)
								UT.NBT.set(tOutput, UT.NBT.setBoolean(UT.NBT.getNBT(tOutput), "gt.err.oredict.output", T));
							}
						} else {
							OM.set(tOutput);
						}
					}
					
					// Cleaning up Recipes with Empty OreDict Lists, since they are never craftable.
					List<ICraftingRecipeGT> tList = CR.list();
					for (int i = 0; i < tList.size(); i++) {
						Object tRecipe = tList.get(i);
						if (tRecipe instanceof ShapedOreRecipe) {
							Object[] tInput = ((ShapedOreRecipe)tRecipe).getInput();
							for (int j = 0; j < tInput.length; j++) {
								if (tInput[j] instanceof List && ((List<?>)tInput[j]).isEmpty()) {
//                                  DEB.println("Removed Recipe for " + ((ShapedOreRecipe)tRecipe).getRecipeOutput().getDisplayName() + " because Ingredient Nr. " + j + " is missing");
									tList.remove(i--);
									break;
								}
							}
						} else if (tRecipe instanceof ShapelessOreRecipe) {
							List<Object> tInput = ((ShapelessOreRecipe)tRecipe).getInput();
							for (int j = 0; j < tInput.size(); j++) {
								if (tInput.get(j) instanceof List && ((List<?>)tInput.get(j)).isEmpty()) {
//                                  DEB.println("Removed Recipe for " + ((ShapelessOreRecipe)tRecipe).getRecipeOutput().getDisplayName() + " because Ingredient Nr. " + j + " is missing");
									tList.remove(i--);
									break;
								}
							}
						}
					}
					
					OreDictManager.INSTANCE.fixStacksizes();
				}
				
				for (int i = 0; i < SERVER_TICK_PRE.size(); i++) {
					ITileEntityServerTickPre tTileEntity = SERVER_TICK_PRE.get(i);
					if (tTileEntity.isDead()) {
						SERVER_TICK_PRE.remove(i--);
						tTileEntity.onUnregisterPre();
					} else if (!WD.blockTicking(tTileEntity)) {
						// #2c (2026-08-09): the chunk does not block-tick (border/unloading) — the machine freezes
						// together with the world, is NOT removed from the list (thaws once the chunk level rises).
					} else {
						try {
							tTileEntity.onServerTickPre(T);
						} catch(Throwable e) {
							SERVER_TICK_PRE.remove(i--);
							tTileEntity.setError("Server Tick Pre 1 - " + e);
							e.printStackTrace(ERR);
						}
					}
				}
				for (int i = 0; i < SERVER_TICK_PR2.size(); i++) {
					ITileEntityServerTickPre tTileEntity = SERVER_TICK_PR2.get(i);
					if (tTileEntity.isDead()) {
						SERVER_TICK_PR2.remove(i--);
						tTileEntity.onUnregisterPre();
					} else if (!WD.blockTicking(tTileEntity)) {
						// #2c: see SERVER_TICK_PRE above.
					} else {
						try {
							tTileEntity.onServerTickPre(F);
						} catch(Throwable e) {
							SERVER_TICK_PR2.remove(i--);
							tTileEntity.setError("Server Tick Pre 2 - " + e);
							e.printStackTrace(ERR);
						}
					}
				}
				
				DELAYED_BLOCK_UPDATES_2.clear();
				List tList = DELAYED_BLOCK_UPDATES_2;
				DELAYED_BLOCK_UPDATES_2 = DELAYED_BLOCK_UPDATES;
				DELAYED_BLOCK_UPDATES = tList;
				for (IHasWorldAndCoords tTileEntity : DELAYED_BLOCK_UPDATES_2) {
					try {
						// #2c: the entity is dead — the update lost its owner, discard it; the chunk is frozen — move it
						// into the active queue until it thaws (in a frozen world neighbors have nothing to recompute).
						if (tTileEntity instanceof ITileEntityUnloadable && ((ITileEntityUnloadable)tTileEntity).isDead()) continue;
						if (!WD.blockTicking(tTileEntity)) {DELAYED_BLOCK_UPDATES.add(tTileEntity); continue;}
						BlockPos tUpdatePos = new BlockPos(tTileEntity.getX(), tTileEntity.getY(), tTileEntity.getZ());
						tTileEntity.getWorld().updateNeighborsAt(tUpdatePos, tTileEntity.getBlock(tTileEntity.getCoords()), null);
						// This queue bypasses doBlockUpdate, so foreign signal graphs would keep a stale value here.
						gregapi.compat.SignalGraphBridge.notifyChanged(tTileEntity.getWorld(), tUpdatePos);
					} catch(Throwable e) {
						if (tTileEntity instanceof ITileEntityErrorable) ((ITileEntityErrorable)tTileEntity).setError("Delayed Block Update - " + e);
						e.printStackTrace(ERR);
					}
				}

				// F-tree (BUG-005): matured forced decays of VANILLA leaves (queued by WD.leafdecay) — executed through the
				// ENGINE'S OWN channels: state.tick (DISTANCE recompute, LeavesBlock.tick:79-81) + re-read + state.randomTick
				// (decaying-leaf decay, LeavesBlock.randomTick:67-72). No custom decay logic — only forcing what
				// the engine would call itself on random ticks (1.7.10: both channels were ONE updateTick).
				// If the leaf survives (the DISTANCE cascade from felled logs has not arrived yet — it travels in delay-1
				// waves) — retry in 8 ticks, up to 40 attempts: a real support (a neighboring tree's log) will exhaust the limit and drop.
				for (int i = 0; i < DELAYED_LEAF_DECAYS.size(); i++) {
					Object[] tEntry = DELAYED_LEAF_DECAYS.get(i);
					if (SERVER_TIME >= (Long)tEntry[2]) {
						DELAYED_LEAF_DECAYS.remove(i--);
						try {
							ServerLevel tLevel = (ServerLevel)tEntry[0];
							BlockPos tPos = (BlockPos)tEntry[1];
							// #2c: was isLoaded — a border chunk passed the gate, and leaf decay ran where the engine
							// had already turned random ticks off. Now the same law as the engine: not ticking — reschedule
							// +8 ticks WITHOUT burning an attempt (chunk loaded but frozen); chunk unloaded — drop it,
							// same as the former isLoaded gate did.
							if (!WD.blockTicking(tLevel, tPos)) {
								if (WD.chunkNow(tLevel, tPos.getX() >> 4, tPos.getZ() >> 4) != null)
									DELAYED_LEAF_DECAYS.add(new Object[] {tLevel, tPos, SERVER_TIME + 8, tEntry[3]});
							} else if (tLevel.isLoaded(tPos)) {
								net.minecraft.world.level.block.state.BlockState tState = tLevel.getBlockState(tPos);
								if (tState.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) {
									tState.tick(tLevel, tPos, tLevel.getRandom());
									tState = tLevel.getBlockState(tPos);
									if (tState.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) {
										tState.randomTick(tLevel, tPos, tLevel.getRandom());
										tState = tLevel.getBlockState(tPos);
										if (tState.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock
										 && !tState.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)
										 && (Integer)tEntry[3] < 40)
											DELAYED_LEAF_DECAYS.add(new Object[] {tLevel, tPos, SERVER_TIME + 8, ((Integer)tEntry[3]) + 1});
									}
								}
							}
						} catch(Throwable e) {e.printStackTrace(ERR);}
					}
				}
				
				if (SERVER_TIME > 10) {
					for (ITileEntityScheduledUpdate tTileEntity : SCHEDULED_TILEENTITY_UPDATES_2) if (!tTileEntity.isDead()) {
						try {
							// #2c: the chunk is frozen — move it into the active queue until it thaws.
							if (!WD.blockTicking(tTileEntity)) {SCHEDULED_TILEENTITY_UPDATES.add(tTileEntity); continue;}
							tTileEntity.onScheduledUpdate();
						} catch(Throwable e) {
							if (tTileEntity instanceof ITileEntityErrorable) ((ITileEntityErrorable)tTileEntity).setError("Scheduled TileEntity Update - " + e);
							e.printStackTrace(ERR);
						}
					}
					SCHEDULED_TILEENTITY_UPDATES_2.clear();
					tList = SCHEDULED_TILEENTITY_UPDATES_2;
					SCHEDULED_TILEENTITY_UPDATES_2 = SCHEDULED_TILEENTITY_UPDATES;
					SCHEDULED_TILEENTITY_UPDATES = tList;
					
					while (!mNewPlayers.isEmpty()) {
						ServerPlayer tPlayer = mNewPlayers.remove(0);
						NW_API.sendToPlayer(new PacketConfig(), tPlayer);
						for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) if (!tPrefix.contains(TD.Prefix.PREFIX_UNUSED)) NW_API.sendToPlayer(new PacketPrefix(tPrefix), tPlayer);
					}
				}
			}
			
			if (aEvent instanceof ServerTickEvent.Post) { // was aEvent.phase == ServerTickEvent.END
				// Coalesced ore-map broadcast (consolidation wave 3, item 1): dirty chunks piled up over the tick —
				// send each ONE time (the packet carries the whole chunk, see PrefixBlock.flushOreMapSync).
				gregapi.block.prefixblock.PrefixBlock.flushOreMapSync();
				for (int i = 0; i < SERVER_TICK_POST.size(); i++) {
					ITileEntityServerTickPost tTileEntity = SERVER_TICK_POST.get(i);
					if (tTileEntity.isDead()) {
						SERVER_TICK_POST.remove(i--);
						tTileEntity.onUnregisterPost();
					} else if (!WD.blockTicking(tTileEntity)) {
						// #2c: see SERVER_TICK_PRE above.
					} else {
						try {
							tTileEntity.onServerTickPost(T);
						} catch(Throwable e) {
							SERVER_TICK_POST.remove(i--);
							tTileEntity.setError("Server Tick Post 1 - " + e);
							e.printStackTrace(ERR);
						}
					}
				}
				
				for (int i = 0; i < SERVER_TICK_PO2T.size(); i++) {
					ITileEntityServerTickPost tTileEntity = SERVER_TICK_PO2T.get(i);
					if (tTileEntity.isDead()) {
						SERVER_TICK_PO2T.remove(i--);
						tTileEntity.onUnregisterPost();
					} else if (!WD.blockTicking(tTileEntity)) {
						// #2c: see SERVER_TICK_PRE above.
					} else {
						try {
							tTileEntity.onServerTickPost(F);
						} catch(Throwable e) {
							SERVER_TICK_PO2T.remove(i--);
							tTileEntity.setError("Server Tick Post 2 - " + e);
							e.printStackTrace(ERR);
						}
					}
				}
				
				EntityFoodTracker.tick();
				
				if (SERVER_TIME % 1200 == 0) checkSaveLocation(aEvent.getServer().getWorldPath(LevelResource.ROOT).toFile(), T);
				
				if (TICK_LOCK.isHeldByCurrentThread()) TICK_LOCK.unlock();
			}
		}
	}

	// [BUG-047] F-hook-removed → central bridge: 1.7.10 Forge hooks BlockRailBase.onMinecartPass/getRailMaxSpeed
	// were CUT from NeoForge 26.1.2 (extensions folder only has IBaseRailBlockExtension — isFlexibleRail/canMakeSlopes/
	// getRailDirection/isValidRailShape; the boost engine reads ONLY via instanceof PoweredRailBlock —
	// OldMinecartBehavior:115-116). Bridge: EntityTickEvent.Post = once per tick per entity AFTER movement — the same phase
	// as the 1.7.10 tail of EntityMinecart.func_145821_a (called onMinecartPass after moveAlongTrack); rail position —
	// getCurrentBlockPosOrRailBelow (the engine's own channel). The SPEED clamp here is only a FALLBACK arm (downward):
	// the primary channel is GT6MinecartBehavior (see onMinecartJoinBridge below), which answers the engine per-rail
	// with the value BEFORE movement; substituted minecarts do not get the post-clamp duplicated.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onMinecartPassBridge(EntityTickEvent.Post aEvent) {
		if (!(aEvent.getEntity() instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart tCart) || tCart.level().isClientSide()) return;
		BlockPos tRailPos = tCart.getCurrentBlockPosOrRailBelow();
		if (!(WD.block(tCart.level(), tRailPos.getX(), tRailPos.getY(), tRailPos.getZ()) instanceof gregapi.block.misc.BlockBaseRail tRail)) return;
		tRail.onMinecartPass(tCart.level(), tCart, tRailPos.getX(), tRailPos.getY(), tRailPos.getZ());
		if (minecartBehavior(tCart) instanceof gregapi.block.misc.BlockBaseRail.GT6MinecartBehavior) return; // the engine already clamps per-rail
		float tMax = tRail.getRailMaxSpeed(tCart.level(), tCart, tRailPos.getX(), tRailPos.getY(), tRailPos.getZ());
		net.minecraft.world.phys.Vec3 tCartMotion = tCart.getDeltaMovement();
		if (Math.abs(tCartMotion.x) > tMax || Math.abs(tCartMotion.z) > tMax)
			tCart.setDeltaMovement(net.minecraft.util.Mth.clamp(tCartMotion.x, -tMax, tMax), tCartMotion.y, net.minecraft.util.Mth.clamp(tCartMotion.z, -tMax, tMax));
	}

	// [BUG-047, deferred-work marker F-hook-removed REMOVED 2026-08-06] Rail speeds ABOVE the engine's 0.4 (Ti 1.2 and beyond):
	// the displacement clamp is HARDCODED INSIDE OldMinecartBehavior.moveAlongTrack:208-211 via getMaxSpeed:410-411 —
	// a post-event cannot raise it. The only per-cart point is the field AbstractMinecart.behavior (private final,
	// AbstractMinecart:62, assigned by the constructor); there is no event/extension to choose behavior in 26.1.2.
	// Reflection substitution on minecart world-join is a precedented trick (IItemProjectile → AbstractArrow.baseDamage,
	// the sole read site for the whole mod); the subclass is BlockBaseRail.GT6MinecartBehavior (1:1 formula
	// min(rail, cart-cap-1.2) from EntityMinecart:373-374, same citations there). Both sides: the client-side arm covers
	// getKnownMovement (derived systems). Experimental physics (NewMinecartBehavior) is NOT substituted: its speed
	// model is its own (no 1.7.10 canon for it), it keeps the fallback post-clamp from onMinecartPassBridge.
	private static java.lang.reflect.Field sMinecartBehaviorField = null;
	private static Object minecartBehavior(net.minecraft.world.entity.vehicle.minecart.AbstractMinecart aCart) {
		try {
			if (sMinecartBehaviorField == null) {
				sMinecartBehaviorField = net.minecraft.world.entity.vehicle.minecart.AbstractMinecart.class.getDeclaredField("behavior");
				sMinecartBehaviorField.setAccessible(true);
			}
			return sMinecartBehaviorField.get(aCart);
		} catch (Throwable e) {return null;}
	}
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onMinecartJoinBridge(EntityJoinLevelEvent aEvent) {
		if (!(aEvent.getEntity() instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart tCart)) return;
		if (net.minecraft.world.entity.vehicle.minecart.AbstractMinecart.useExperimentalMovement(tCart.level())) return;
		try {
			Object tBehavior = minecartBehavior(tCart);
			if (tBehavior == null || tBehavior instanceof gregapi.block.misc.BlockBaseRail.GT6MinecartBehavior) return;
			sMinecartBehaviorField.set(tCart, new gregapi.block.misc.BlockBaseRail.GT6MinecartBehavior(tCart));
		} catch (Throwable e) {e.printStackTrace(ERR);}
	}

	// BUG-090: behavior of GT6 potion effects that used to live in 1.7.10 inside Immersive Engineering handlers
	// (EventHandler.java:387-408, decompiled reference ImmersiveEngineering-1.7.10/ in the project tree) — the
	// effects themselves are now registered by GT6 (gregapi/potion/MobEffectsGT, "function, not authorship"), the
	// handlers are duplicated 1:1 into this same single subscription center. LivingHurtEvent (1.7.10) does not exist in neo —
	// the mutable pre-armor damage amount is LivingIncomingDamageEvent.setAmount (checked,
	// neoforge-decompiled/.../LivingIncomingDamageEvent.java); priority LOWEST — same as the IE original.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingHurtPotionsGT(LivingIncomingDamageEvent aEvent) {
		MobEffectInstance tEffect;
		// 1:1 IE EventHandler.java:390-395: fire damage × (1.5 + amp²·0.5) under the flammable effect.
		if (aEvent.getSource().is(DamageTypeTags.IS_FIRE) && (tEffect = aEvent.getEntity().getEffect(gregapi.potion.MobEffectsGT.FLAMMABLE)) != null) {
			int tAmp = tEffect.getAmplifier();
			aEvent.setAmount(aEvent.getAmount() * (1.5F + tAmp*tAmp*0.5F));
		}
		// 1:1 IE EventHandler.java:396-401: "flux"-type damage (IE electricity) × the same multiplier under
		// conductive. In a build without IE machines there is no "flux" damage source — same as in 1.7.10 (see MobEffectsGT).
		if ("flux".equals(aEvent.getSource().getMsgId()) && (tEffect = aEvent.getEntity().getEffect(gregapi.potion.MobEffectsGT.CONDUCTIVE)) != null) {
			int tAmp = tEffect.getAmplifier();
			aEvent.setAmount(aEvent.getAmount() * (1.5F + tAmp*tAmp*0.5F));
		}
	}

	// 1:1 IE EventHandler.java:403-408: sticky weakens the jump — motionY -= (amp+1)·0.3.
	@SubscribeEvent
	public void onLivingJumpPotionsGT(LivingEvent.LivingJumpEvent aEvent) {
		MobEffectInstance tEffect = aEvent.getEntity().getEffect(gregapi.potion.MobEffectsGT.STICKY);
		if (tEffect != null) {
			net.minecraft.world.phys.Vec3 tMotion = aEvent.getEntity().getDeltaMovement();
			aEvent.getEntity().setDeltaMovement(tMotion.x, tMotion.y - (tEffect.getAmplifier()+1)*0.3F, tMotion.z);
		}
	}

	// Was @SubscribeEvent onLivingUpdate(LivingUpdateEvent) — LivingUpdateEvent (net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent,
	// 1.7.10) does not exist in neo (checked: net.neoforged.neoforge.event.entity.living.LivingEvent.java only contains LivingJumpEvent/
	// LivingVisibilityEvent). The real per-tick hook for any Entity (including LivingEntity) is EntityTickEvent.Post, "fired once per game tick,
	// per entity, after the entity performs work" (checked, net.neoforged.neoforge.event.tick.EntityTickEvent.java) — called from the tail of
	// Entity#tick() (not just LivingEntity), so an explicit instanceof check was added (the dispatcher got wider, the handler body is 1:1).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingUpdate(EntityTickEvent.Post aEvent) {
		if (!(aEvent.getEntity() instanceof LivingEntity)) return;
		LivingEntity aEntityLiving = (LivingEntity)aEvent.getEntity();

		int
		tX = UT.Code.roundDown(aEntityLiving.getX()),
		tY = UT.Code.roundDown(aEntityLiving.getY() + aEntityLiving.getEyeHeight()),
		tZ = UT.Code.roundDown(aEntityLiving.getZ());

		Block tBlock = WD.block(aEntityLiving.level(), tX, tY, tZ);
		if (tBlock instanceof IBlockOnHeadInside) ((IBlockOnHeadInside)tBlock).onHeadInside(aEntityLiving, aEntityLiving.level(), tX, tY, tZ);

		tY = UT.Code.roundDown(aEntityLiving.getBoundingBox().minY-0.001F);

		if (aEntityLiving instanceof Player) {
			if (BlocksGT.Paths != null && !aEntityLiving.level().isClientSide()) {
				Block tPath = IL.EtFu_Path.block();
				if (ST.valid(tPath)) for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
					if (tPath == WD.block(aEntityLiving.level(), tX+i, tY+k, tZ+j)) WD.replaceAll(aEntityLiving.level(), tX+i, tY+k, tZ+j, tPath, W, BlocksGT.Paths, 0);
				}
			}
		}

		if (aEntityLiving.onGround()) {
			tBlock = WD.block(aEntityLiving.level(), tX, tY, tZ);
			if (!WD.hasCollide(aEntityLiving.level(), tX, tY, tZ, tBlock)) {
				int tAddX = (aEntityLiving.getX() >= tX + 0.5 ? +1 : -1), tAddZ = (aEntityLiving.getZ() >= tZ + 0.5 ? +1 : -1);
				tBlock = WD.block(aEntityLiving.level(), tX+tAddX, tY, tZ);
				if (WD.hasCollide(aEntityLiving.level(), tX+tAddX, tY, tZ, tBlock)) {
					tX += tAddX;
				} else {
					tBlock = WD.block(aEntityLiving.level(), tX, tY, tZ+tAddZ);
					if (WD.hasCollide(aEntityLiving.level(), tX, tY, tZ+tAddZ, tBlock)) {
						tZ += tAddZ;
					} else {
						tBlock = WD.block(aEntityLiving.level(), tX+tAddX, tY, tZ+tAddZ);
						if (WD.hasCollide(aEntityLiving.level(), tX+tAddX, tY, tZ+tAddZ, tBlock)) {
							tX += tAddX;
							tZ += tAddZ;
						} else {
							tBlock = NB;
						}
					}
				}
			}

			// walk over special Blocks.
			if (tBlock instanceof IBlockOnWalkOver) ((IBlockOnWalkOver)tBlock).onWalkOver(aEntityLiving, aEntityLiving.level(), tX, tY, tZ);
			// Only Serverside for this Stuff.
			if (!aEntityLiving.level().isClientSide()) {
				// Zombies trample Farmland.
				if (tBlock == Blocks.FARMLAND && aEntityLiving instanceof Zombie) {
					WD.set(aEntityLiving.level(), tX, tY, tZ, Blocks.DIRT, 0, 3);
					UT.Sounds.send(SFX.MC_DIG_GRAVEL, aEntityLiving.level(), tX, tY, tZ);
				}
				// Big Animals break regular tall Grass, but not super tall Grass.
				if (aEntityLiving instanceof Pig || aEntityLiving instanceof Sheep || aEntityLiving instanceof Cow || aEntityLiving instanceof Horse) {
					if (WD.block(aEntityLiving.level(), tX, tY+1, tZ) == Blocks.DEAD_BUSH) {
						WD.set(aEntityLiving.level(), tX, tY+1, tZ, NB, 0, 3);
						UT.Sounds.send(SFX.MC_DIG_GRASS, 0.5F, 0.5F, aEntityLiving.level(), tX, tY, tZ);
					}
				}
				// Area of Effect Block Destruction Ability of certain Mobs.
				if (aEntityLiving.invulnerableTime > 0) {
					// Minoshroom
					if (MD.TF.mLoaded && aEntityLiving instanceof EntityTFMinoshroom) {
						// Once damaged, the Minoshroom will not stay bound to its Room! (was detachHome() — 1.7.10 EntityCreature;
						// neo PathfinderMob/Mob equivalent for releasing the home tether — setHomeTo(BlockPos.ZERO, -1) (checked, Mob.java: homeRadius==-1 ⇒ isWithinHome() always true).
						((PathfinderMob)aEntityLiving).setHomeTo(BlockPos.ZERO, -1);
						// Minoshroom surprise charge through the Fenced Gateways!
						for (int iX = tX-15, eX = tX+15; iX <= eX; iX++) for (int iZ = tZ-15, eZ = tZ+15; iZ <= eZ; iZ++) for (int iY = tY+1, eY = tY+3; iY <= eY; iY++) {
							if (WD.block(aEntityLiving.level(), iX, iY, iZ) == Blocks.OAK_FENCE) {
								WD.set(aEntityLiving.level(), iX, iY, iZ, NB, 0, 3);
								ST.drop(aEntityLiving.level(), iX, iY, iZ, IL.Stick.get(1));
								UT.Sounds.send(SFX.MC_DIG_WOOD, aEntityLiving.level(), iX, iY, iZ);
							}
						}
					}
				}
			}
		}
	}
	
	// Was aEvent.side/aEvent.phase (1.7.10 TickEvent.WorldTickEvent+Phase) — neo LevelTickEvent only carries getLevel()/hasTime(),
	// and Pre/Post are separate subclasses (checked, net.neoforged.neoforge.event.tick.LevelTickEvent.java); "world.loadedEntityList"/
	// "loadedTileEntityList" (flat ArrayLists) are gone — the real neo path: Level.getEntities().getAll() (Iterable, not indexable).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onWorldTick(LevelTickEvent aEvent) {
		TOOL_SOUNDS = TOOL_SOUNDS_SETTING;

		if (aEvent.getLevel() instanceof ServerLevel aServerLevel && aEvent instanceof LevelTickEvent.Post) { // getEntities() with no arguments is declared on ServerLevel, not Level (checked, ServerLevel.java:1753)
			ArrayListNoNulls<ExperienceOrb> tOrbs = (XP_ORB_COMBINING && SERVER_TIME % 40 == 31 ? new ArrayListNoNulls<ExperienceOrb>(128) : null);
			// BUG-103 (defect class "the entity set changes during iteration"): removing DIRECTLY IN THE LOOP is not allowed. By
			// engine code, discard() → Callback.onRemove → stopTracking → onTrackingEnd → ChunkMap.removeEntity (mutates
			// entityMap) AND visibleEntityStorage.remove — i.e. it structurally mutates both the tracker map and the VERY
			// collection that getAll() iterates (EntityLookup.byId, Int2ObjectLinkedOpenHashMap; getAllEntities
			// returns its live wrapper). In 1.7.10 setDead() only set a flag, and iteration was safe. We collect and
			// remove AFTER the loop — the observed behavior is the same, engine maps are not touched during iteration.
			ArrayListNoNulls<ItemEntity> tToDiscard = null;

			for (Entity aEntity : aServerLevel.getEntities().getAll()) {
				if (aEntity == null || aEntity.isRemoved()) continue;
				if (aEntity instanceof ExperienceOrb) {
					if (tOrbs != null) tOrbs.add((ExperienceOrb)aEntity);
				} else if (aEntity instanceof ItemEntity) {
					ItemStack aStack = ((ItemEntity)aEntity).getItem();
					if (ST.valid(aStack)) {
						ItemStack rStack = ST.copy(aStack);
						boolean tBreak = F, tFireProof = F;

						// TODO make a case for Armor too whenever I decide to even add Armor.
						if (rStack.getItem() instanceof MultiItemTool) {
							if (MultiItemTool.getPrimaryMaterial  (aStack).contains(TD.Properties.UNBURNABLE)) tFireProof = T;
							if (MultiItemTool.getSecondaryMaterial(aStack).contains(TD.Properties.UNBURNABLE)) tFireProof = T;
						}

						OreDictItemData aData = OM.anydata_(rStack);
						if (aData != null) {
							if (aData.validPrefix()) for (IOreDictListenerItem tListener : aData.mPrefix.mListenersItem) {
								rStack = tListener.onTickWorld(aData.mPrefix, aData.mMaterial.mMaterial, rStack, (ItemEntity)aEntity);
								if (!ST.equal(rStack, aStack) || rStack.getCount() != aStack.getCount()) {tBreak = T; break;}
							}
							if (!tBreak && aData.validMaterial()) for (OreDictMaterialStack tMaterial : aData.getAllMaterialStacks()) {
								if (tBreak) break;
								if (tMaterial.mMaterial.contains(TD.Properties.UNBURNABLE)) tFireProof = T;
								for (IOreDictListenerItem tListener : tMaterial.mMaterial.mListenersItem) {
									rStack = tListener.onTickWorld(aData.mPrefix, tMaterial.mMaterial, rStack, (ItemEntity)aEntity);
									if (!ST.equal(rStack, aStack) || rStack.getCount() != aStack.getCount()) {tBreak = T; break;}
								}
							}
						}

						if (rStack == null || rStack.getCount() <= 0) {
							// ItemEntity reads its own stack (fireImmune -> getItem().canBeHurtBy), so null crashes the engine.
							((ItemEntity)aEntity).setItem(ST.nn(NI));
							// BUG-103: no discard() here — we are inside the world entity iteration (see tToDiscard above)
							if (tToDiscard == null) tToDiscard = new ArrayListNoNulls<>(16);
							tToDiscard.add((ItemEntity)aEntity);
							// Removal is deferred, so isRemoved() stays false: the entity must be skipped explicitly.
							continue;
						} else if (!ST.equal(rStack, aStack) || rStack.getCount() != aStack.getCount()) {
							((ItemEntity)aEntity).setItem(rStack);
							UT.Reflection.setField(ItemEntity.class, aEntity, "pickupDelay", 40, F); // was delayBeforeCanPickup (1.7.10) — neo field name: pickupDelay, private (checked, ItemEntity.java:49)
						}

						if (!aEntity.isRemoved() && aEntity.isOnFire() && (tBreak || (tFireProof && !MD.MC.owns(rStack)))) {
							UT.Reflection.setField(ItemEntity.class, aEntity, "health", 250, F);
							// EVENTS: golden set both "health" AND "field_70291_e" — that is ONE field (field_70291_e = the SRG name of health in the
							// 1.7.10 EntityItem; a deobf+SRG duplication). health=250 above already covers both → the second set was redundant, not a loss.
							aEntity.extinguishFire();
						}
					}
				} else if (aEntity instanceof LivingEntity) {
					if (ENTITY_CRAMMING > 0 && SERVER_TIME % 50 == 0 && !(aEntity instanceof Player) && ((LivingEntity)aEntity).isPushable() && ((LivingEntity)aEntity).getHealth() > 0) { // was canBePushed() (1.7.10) — neo: isPushable() (checked, LivingEntity.java:3391)
						List<Entity> tList = aEntity.level().getEntities(aEntity, aEntity.getBoundingBox().inflate(0.2, 0.0, 0.2));
						Class<? extends Entity> tClass = aEntity.getClass();
						int aEntityCount = 1;
						if (tList != null) for (int j = 0; j < tList.size(); j++) if (tList.get(j) != null && tList.get(j).getClass() == tClass) aEntityCount++;
						if (aEntityCount > ENTITY_CRAMMING) aEntity.hurt(aEntity.level().damageSources().inWall(), (aEntityCount - ENTITY_CRAMMING) * TFC_DAMAGE_MULTIPLIER);
					}
				}
			}

			// BUG-103: iteration finished — removal is now safe (engine maps are mutated outside iteration)
			if (tToDiscard != null) for (ItemEntity tDead : tToDiscard) if (!tDead.isRemoved()) tDead.discard();

			if (tOrbs != null && tOrbs.size() > 32) for (ExperienceOrb aOrb : tOrbs) {
				if (aOrb.getValue() >= Short.MAX_VALUE) continue;
				if (aOrb.getValue() <= 0) {aOrb.setValue(0); aOrb.discard(); continue;}
				for (ExperienceOrb tOrb : tOrbs) if (aOrb != tOrb && !tOrb.isRemoved() && tOrb.getValue() > 0 && tOrb.getValue() < Short.MAX_VALUE && aOrb.distanceToSqr(tOrb) <= 3) {
					// EVENTS impossible-1:1: neo ExperienceOrb.age is private, no public setter — carrying over age when merging
					// orbs (1.7.10 public xpOrbAge) is not expressible; value merging works, the surviving orb keeps its own age (age
					// only affects the despawn timer, ~5 min) → the step is skipped, XP merging is still functional.
					if (aOrb.getValue() + tOrb.getValue() > Short.MAX_VALUE) {
						tOrb.setValue(tOrb.getValue() - (Short.MAX_VALUE - aOrb.getValue()));
						aOrb.setValue(Short.MAX_VALUE);
						break;
					}
					aOrb.setValue(aOrb.getValue() + tOrb.getValue());
					tOrb.setValue(0);
					tOrb.discard();
					break;
				}
			}

			if (SERVER_TIME % 20 == 1) {
				// EVENTS model-shift (not core-data-loss): the 1.7.10 sweep over World.loadedTileEntityList every 20 ticks marked
				// ITileEntityNeedsSaving TEs dirty (crash-resilience: between auto-saves). neo stores BlockEntity per-chunk
				// (LevelChunk.getBlockEntities()) — there is no flat world list. Persistence is NOT lost: neo writes ALL BEs
				// on chunk unload + periodic auto-save of dirty chunks. The remaining aspect is only crash-resilience
				// (marking dirty between saves); the neo idiom is the TE calling setChanged() on mutation (distributed), not a central
				// sweep. ITileEntityNeedsSaving is implemented only by TileEntityBase02AdjacentTEBuffer; unload-save covers it.
			}
		}
	}
	
	// Was cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent (1.7.10) — does not exist in neo. The real neo equivalent
	// for "a player successfully picked up an item" is ItemEntityPickupEvent.Post (checked, net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerItemPickupEvent(ItemEntityPickupEvent.Post aEvent) {
		ST.check(aEvent.getPlayer(), aEvent.getItemEntity().getItem());
	}

	private int BEAR_INVENTORY_COOL_DOWN = 5;

	// Was aEvent.phase == Phase.END (1.7.10 TickEvent) — neo PlayerTickEvent splits Pre/Post into subclasses, Post is already "after the tick" (checked, PlayerTickEvent.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerTickEvent(PlayerTickEvent.Post aEvent) {
		Player aPlayer = aEvent.getEntity();
		if (!aPlayer.isRemoved()) {

		////if (aPlayer.worldObj.provider instanceof WorldProviderTwilightForest) {
		////    Object tChunkProvider = ((WorldProviderTwilightForest)aPlayer.worldObj.provider).getChunkProvider();
		////    if (tChunkProvider != null) {
		////        DEB.println("TRYING TO REMOVE SPAMMED TWILIGHT STRUCTURES");
		////        Iterator<Map.Entry<Long, Object>> titerator = ((MapGenTFMajorFeature)UT.Reflection.getFieldContent(tChunkProvider, "majorFeatureGenerator")).structureMap.entrySet().iterator();
		////        while (titerator.hasNext()) {
		////            Map.Entry<Long, Object> tEntry = titerator.next();
		////            ChunkCoordIntPair tCoords = new ChunkCoordIntPair((int)((tEntry.getKey()) & 4294967295L), (int)((tEntry.getKey() >> 32) & 4294967295L));
		////            if (Math.abs(tCoords.chunkXPos) > 666 || Math.abs(tCoords.chunkZPos) > 666) {
		////                DEB.println("REMOVED A SUPERFLUOUS STRUCTURE AT: " + tCoords.chunkXPos*16 + "; " + tCoords.chunkZPos*16);
		////                titerator.remove();
		////            }
		////        }
		////    }
		////}
			
			for (Object tPotion : aPlayer.getActiveEffects()) { // was getActivePotionEffects() (1.7.10) — neo: getActiveEffects() (checked, LivingEntity.java:994)
				if (tPotion instanceof MobEffectInstance && ((MobEffectInstance)tPotion).getDuration() <= 0) {
					aPlayer.removeEffect(((MobEffectInstance)tPotion).getEffect()); // was removePotionEffect(int)/getPotionID() — neo: removeEffect(Holder<MobEffect>)/getEffect() (checked, LivingEntity.java:1079 + MobEffectInstance.java:192)
					break;
				}
			}
			
			if (!aPlayer.level().isClientSide()) { // was aEvent.side.isServer() — PlayerTickEvent.Post fires on both sides (checked, javadoc PlayerTickEvent.java)
				/** This cannot work the way I hoped it would, would despawn way too few mobs...
				if (SERVER_TIME % 100 == 0) {
					DEB.println("==========");
					DEB.println("TEST START");
					DEB.println("==========");
					Iterator<EntityLiving>
					tIterator = mMobsToFastDespawn.iterator();
					while (tIterator.hasNext()) {
						EntityLiving tEntity = tIterator.next();
						if (tEntity.isRemoved()) {
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.tickCount + "     DEAD");
							tIterator.remove();
						} else if (tEntity.isNoDespawnRequired()) {
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.tickCount + "     PERSISTENT");
							tIterator.remove();
						} else if (tEntity.tickCount != tEntity.getAge()) {
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.tickCount + "     GOT CLOSE TO PLAYER");
							tIterator.remove();
						} else {
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.tickCount);
						}
					}
					DEB.println("====01====");
					DEB.println("List Changed: " + mMobsToFastDespawn.removeAll(aPlayer.worldObj.getEntities(aPlayer, AxisAlignedBB.getBoundingBox(aPlayer.posX-32, aPlayer.posY-32, aPlayer.posZ-32, aPlayer.posX+32, aPlayer.posY+32, aPlayer.posZ+32))));
					DEB.println("====02====");
					tIterator = mMobsToFastDespawn.iterator();
					while (tIterator.hasNext()) {
						EntityLiving tEntity = tIterator.next();
						DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.tickCount);
					}
					DEB.println("==========");
					DEB.println("TEST END");
					DEB.println("==========");
				}
				*/
				if (SURVIVAL_INTO_ADVENTURE_MODE && aPlayer.tickCount%200==0 && aPlayer.getAbilities().mayBuild && !UT.Entities.isCreative(aPlayer) && aPlayer instanceof ServerPlayer aServerPlayer) {
					aServerPlayer.setGameMode(GameType.ADVENTURE); // was setGameType(WorldSettings.GameType...) — neo: ServerPlayer.setGameMode(GameType) (checked, ServerPlayer.java:1849)
					aPlayer.getAbilities().mayBuild = F;
					if (ADVENTURE_MODE_KIT) {
						if (MD.GT.mLoaded) {
							UT.Entities.sendchat(aPlayer, CHAT_GREG + LH.tt("Thank you for choosing the GregTech-6 Adventure Mode Starter Kit."));
							
							MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
							ST.drop(aPlayer, tRegistry == null ? IL.Bottle_Purple_Drink.get(6) : tRegistry.getItem(8762, 1, UT.NBT.make(NBT_INV_LIST, UT.NBT.makeInv(IL.Bottle_Purple_Drink.get(1), IL.Bottle_Empty.get(1), IL.Bottle_Purple_Drink.get(1), IL.Bottle_Purple_Drink.get(1), IL.Bottle_Empty.get(1), IL.Bottle_Purple_Drink.get(1), IL.Bottle_Purple_Drink.get(1), IL.Bottle_Purple_Drink.get(1), IL.Bottle_Empty.get(1)))));
							ST.drop(aPlayer, IL.Grass_Dry.get(9));
							ST.drop(aPlayer, IL.Stick.get(16));
							ST.drop(aPlayer, Items.FLINT, 12, 0);
							ST.drop(aPlayer, Blocks.DIRT, 16, 0);
							ST.drop(aPlayer, Blocks.OAK_SAPLING, 4, 0);
							switch (RNGSUS.nextInt(4)) {
							case 0: ST.drop(aPlayer, IL.Food_Large_Sandwich_Veggie.get(1)); break;
							case 1: ST.drop(aPlayer, IL.Food_Large_Sandwich_Cheese.get(1)); break;
							case 2: ST.drop(aPlayer, IL.Food_Large_Sandwich_Steak .get(1)); break;
							case 3: ST.drop(aPlayer, IL.Food_Large_Sandwich_Bacon .get(1)); break;
							}
						} else {
							UT.Entities.sendchat(aPlayer, CHAT_GREG + LH.tt("It's dangerous to go alone! Take this."));
							ST.drop(aPlayer, Items.STONE_AXE, 1, 0);
						}
					}
				}
				
				
				final boolean tHungerEffect = (HUNGER_BY_INVENTORY_WEIGHT && aPlayer.tickCount % 2400 == 1200), tBetweenlands = WD.dimBTL(aPlayer.level());//, tCrazyJ1984 = "CrazyJ1984".equalsIgnoreCase(aPlayer.getScoreboardName());
				if (aPlayer.tickCount % 120 == 0) {
					ItemStack tStack;
					int tCount = 64, tEmptySlots = 36, tCraponite = 0;
					for (int i = 0; i < 36; i++) {
						if (ST.valid(tStack = aPlayer.getInventory().getItem(i))) {
							tEmptySlots--;
							if (tBetweenlands) {
								if (tStack.getItem() == Items.POTION) {
									ST.set(tStack, IL.BTL_Tainted_Potion.get(1), F, F);
								// F5/BUG-045 (1:1): the block is restored on the live compat-mirror IFluidContainerItem (original :797-805);
								// foreign-gated (Betweenlands is absent in 26.1.2 — the tBetweenlands branch is dead, but the contract stays 1:1).
								} else if (tStack.getItem() instanceof IFluidContainerItem) {
									FluidStack tFluid = ((IFluidContainerItem)tStack.getItem()).getFluid(tStack);
									if (tFluid != null && !FL.Potion_Tainted.is(tFluid) && FluidsGT.POTION.contains(FL.regName(tFluid.getFluid()))) {
										((IFluidContainerItem)tStack.getItem()).drain(tStack, Integer.MAX_VALUE, T);
										((IFluidContainerItem)tStack.getItem()).fill(tStack, FL.Potion_Tainted.make(tFluid.getAmount()), T);
									}
								}
								ItemStack tRotten = RottingUtil.rotting(tStack, aPlayer.level(), UT.Code.roundDown(aPlayer.getX()), UT.Code.roundDown(aPlayer.getY()), UT.Code.roundDown(aPlayer.getZ()));
								if (ST.invalid(tRotten)) {tStack.setCount(0); aPlayer.getInventory().setItem(i, ST.nn(NI)); continue;} // F15 boundary: setItem(null) on a NonNullList throws NPE
								if (tStack != tRotten) ST.set(tStack, tRotten);
							}
							// You can't detect properly when you pick things up out of a Chest, so part of the Inventory scan it is!
							if (IL.TF_Trophy_Urghast.equal(tStack, T, T)) {
								ST.check(aPlayer, tStack);
							}
							// Radiation and Heat Damage.
							if (!UT.Entities.isInvincible(aPlayer)) {
								UT.Entities.applyRadioactivity(aPlayer, UT.Entities.getRadioactivityLevel(tStack), tStack.getCount());
								float tHeat = UT.Entities.getHeatDamageFromItem(tStack);
								if (tHeat != 0.0F) if (tHeat > 0) UT.Entities.applyHeatDamage(aPlayer, tHeat); else UT.Entities.applyFrostDamage(aPlayer, -tHeat);
							}
							// Data based checks.
							OreDictItemData tData = OM.anydata_(tStack);
							if (tData != null && tData.validMaterial()) {
								if ((tData.mMaterial.mMaterial == MT.Bedrockium || tData.mMaterial.mMaterial == MT.Neutronium) && (tData.validPrefix() || tData.mByProducts.length <= 0)) {
									// EVENTS: 1.7.10 Potion.moveSlowdown → neo MobEffects.SLOWNESS (Holder, exists — a rename, not a removal).
									// getActivePotionEffect→getEffect. Restored 1:1 (applyPotion(Entity,Holder,...) already supported, UT.java:3036).
									net.minecraft.world.effect.MobEffectInstance tEffect = null;
									UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.SLOWNESS, Math.max(140, ((tEffect = aPlayer.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS))==null?0:tEffect.getDuration())), 3, F);
								}
								if (tData.mMaterial.mMaterial == MT.Craponite) {
									tCraponite++;
								}
								if (tData.mMaterial.mMaterial == MT.Firestone && tData.validPrefix() && !MD.RC.owns(tStack)) for (int j = (int)UT.Code.divup(tData.mMaterial.mAmount * tStack.getCount(), U); j > 0; j--) {
									WD.fire(aPlayer.level(), UT.Code.roundDown(aPlayer.getX())-5+RNGSUS.nextInt(11), UT.Code.roundDown(aPlayer.getY())-5+RNGSUS.nextInt(11), UT.Code.roundDown(aPlayer.getZ())-5+RNGSUS.nextInt(11), RNGSUS.nextInt(8) != 0);
								}
							}
							if (tHungerEffect) tCount+=(tStack.getCount() * 64) / Math.max(1, tStack.getMaxStackSize());
							if (INVENTORY_UNIFICATION) OM.set_(tStack);
							ST.update(tStack, aPlayer);
							if (ItemNBT.has(tStack) && ItemNBT.get(tStack).isEmpty()) ItemNBT.set(tStack, null);
						}
					}
					
					// This Code is to tell Bear and all the people around him that he should clean up his always cluttered Inventory.
					if ("Bear989Sr".equalsIgnoreCase(aPlayer.getScoreboardName())) {
						if (tCraponite > 0) {
							// Crazy started to give Bear her Craponite Arrows, lets not let him have those.
							// EVENTS: Potion.poison → neo MobEffects.POISON (Holder, exists). Restored 1:1.
							UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.POISON, 1200, tCraponite, T);
						}
						if (--BEAR_INVENTORY_COOL_DOWN < 0 && tEmptySlots < 4 && aPlayer.level() instanceof ServerLevel aServerLevel) {
							BEAR_INVENTORY_COOL_DOWN = 100;
							UT.Sounds.send(SFX.MC_HMM, aPlayer);
							for (int i = 0; i < aServerLevel.players().size(); i++) { // was level().playerEntities (1.7.10) — neo: ServerLevel.players() (checked, ServerLevel.java:1530)
								Player tPlayer = aServerLevel.players().get(i);
								if (tPlayer == null) continue;
								if ("Bear989Sr".equalsIgnoreCase(tPlayer.getScoreboardName())) {
									if (tPlayer.getY() < 30) {
										UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Stop making Holes in the Ground, Bear!"));
									} else {
										// Bear does not like being called these names, so lets annoy him. XD
										switch(tEmptySlots) {
										case 0: UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Alright Buttercup, your Inventory is full, time to go home.")); break;
										case 1: UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Your Inventory is starting to get full, Buttercup")); break;
										case 2: UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Your Inventory is starting to get full, Bean989Sr")); break;
										case 3: UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Your Inventory is starting to get full, Mr. Bear")); break;
										}
									}
								} else if ("Bear989jr".equalsIgnoreCase(tPlayer.getScoreboardName())) {
									// ENCHANT: Enchantment_WerewolfDamage.INSTANCE (1.7.10 Java object of the enchant) replaced with
									// ResourceKey<Enchantment> KEY (see gregapi/enchants/Enchantment_WerewolfDamage.java) —
									// resolved through the server's live RegistryAccess (same trick as SILK_TOUCH/FORTUNE above in this file).
									ST.give(tPlayer, UT.NBT.addEnchantment(ST.make(Items.COOKIE, 1, 0, "Jr. Cookie"), Enchantment_WerewolfDamage.KEY, 1), F);
									UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Have a Jr. Cookie. Please tell Fatass to clean his Inventory, or smack him with it."));
								} else if ("CrazyJ1984".equalsIgnoreCase(tPlayer.getScoreboardName())) {
									ItemStack tArrow = ST.update(OP.arrowGtWood.mat(MT.Craponite, 1), aPlayer);
									if (ST.valid(tArrow)) {
										ST.give(tPlayer, tArrow, F);
										UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "I'm not trying to tell you what to do, but please don't hurt Bear with this."));
									} else {
										UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "I'm not trying to tell you what to do, but please don't hurt Bear."));
									}
								} else if ("TooShyShy78".equalsIgnoreCase(tPlayer.getScoreboardName())) {
									ItemStack tArrow = ST.update(OP.arrowGtWood.mat(MT.Craponite, 1), aPlayer);
									if (ST.valid(tArrow)) {
										ST.give(tPlayer, tArrow, F);
										UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "People around Bear always seem to suffer a severe case of Craponite Arrow in Inventory, I don't know why."));
									} else {
										UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Aaaaand Bears Inventory is full again isn't it..."));
									}
								} else if ("Ilirith".equalsIgnoreCase(tPlayer.getScoreboardName())) {
									UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Could you tell Bear989Sr very gently, that his Inventory is a fucking mess again?"));
								} else if ("Shadowkn1ght18".equalsIgnoreCase(tPlayer.getScoreboardName())) {
									UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "Here is your special Message to make you tell Bear989Sr to clean his Inventory."));
								} else if ("e99999".equalsIgnoreCase(tPlayer.getScoreboardName())) {
									UT.Entities.chat(tPlayer, Component.literal(LH.Chat.DGRAY + "You get the sneaking suspicion that Bears Inventory may or may not be full right now."));
								} else {
									UT.Entities.chat(tPlayer, Component.literal(CHAT_GREG + "There is this fella called Bear-Nine-Eight-Nine, needing be reminded of his Inventory being a major Pine."));
								}
							}
						}
					}
					
					// was inventory.armorInventory[0..3] (1.7.10, boots/leggings/chest/helmet) — armor is no longer stored in Inventory in neo
					// (a 36-slot array), but in EntityEquipment; the order FEET/LEGS/CHEST/HEAD matches the old 0..3 (checked, EquipmentSlot.java).
					for (EquipmentSlot tSlot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) if (ST.valid(tStack = aPlayer.getItemBySlot(tSlot))) {
						// The Better Storage Backpack would dupe Items when destroyed while worn, so this will prevent that.
						// A Backpack already is hindrance enough if you want full Armor, so Durability should not matter here anyways.
						// I also like this Backpack implementation, so I cant just leave the dupe exploit easy to pull off.
						if (MD.BTRS.mLoaded && (IL.BTRS_Backpack.equal(tStack, T, T) || IL.BTRS_Thaumpack.equal(tStack, T, T) || IL.BTRS_Enderpack.equal(tStack, T, T))) {
							ST.meta(tStack, 0);
						}
						
						if (!UT.Entities.isInvincible(aPlayer)) {
							UT.Entities.applyRadioactivity(aPlayer, UT.Entities.getRadioactivityLevel(tStack), tStack.getCount());
							float tHeat = UT.Entities.getHeatDamageFromItem(tStack);
							if (tHeat != 0.0F) if (tHeat > 0) UT.Entities.applyHeatDamage(aPlayer, tHeat); else UT.Entities.applyFrostDamage(aPlayer, -tHeat);
						}
						if (tHungerEffect) tCount+=256;
					}
					if (tHungerEffect) UT.Entities.exhaust(aPlayer, Math.max(1.0F, tCount/666F));
				}
			}
		}
	}
	
	// ChunkWatchEvent.Watch.player/.chunk (1.7.10) — private fields in neo (checked, net.neoforged.neoforge.event.level.ChunkWatchEvent.java) —
	// getPlayer()/getChunk(); getChunk() returns the LevelChunk directly, the repeated getChunkFromChunkCoords(...) by x/z is no longer needed.
	// tChunk.isTerrainPopulated (1.7.10 generation flag) does not exist in neo (impossible-1:1) — watched chunks are ALWAYS
	// FULL status, the check is unneeded (correctly omitted); chunkTileEntityMap → getBlockEntities().
	// ⚠ neo CANON (ChunkWatchEvent.java:64-65): Watch = the chunk is merely QUEUED — "must NOT be used to send
	// additional chunk-related data to the client as the client will not be aware of the chunk yet"; for data use Sent.
	// On Watch, GT6 BE packets arrived EARLIER than the chunk → the client dropped them (no block yet) → the client-side worldgen-MTE BE
	// (pebbles/sticks) was not created on a REPEAT world join → nothing for the BER to render (player report 2026-07-19).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onChunkWatchEvent(ChunkWatchEvent.Sent aEvent) {
		LevelChunk tChunk = aEvent.getChunk();
		if (tChunk == null) return;
		// F17: sending to the player can happen in the SAME tick as the chunk load, while the stub reconstruction queue
		// (STUB_QUEUE) drains over ticks — AFTER the send. On chunk load an MTE-BE is born as a
		// TileEntityLoaderStub (MTE_TYPE factory, the class from sub-ID unavailable), the stub is not ITileEntitySynchronising →
		// the broadcast passed it by; ticking MTEs matured from their own ticks, notick ones (walls) — never. Synchronous
		// reconstruction of THIS chunk's stubs via the existing single mechanism BEFORE the broadcast: the order "BE real →
		// sync" is guaranteed by construction for ALL MTEs.
		gregapi.worldgen.GT6WorldgenFeature.reconstructChunkMTEs(tChunk.getLevel(), tChunk.getPos().x(), tChunk.getPos().z());
		if (tChunk.getBlockEntities() != null && tChunk.getBlockEntities().size() > 0) {
			byte tIterations = 8;
			HashSetNoNulls<Object> tSet = new HashSetNoNulls<>();
			while (tIterations-->0) try {
				for (Object tTileEntity : tChunk.getBlockEntities().values()) if (tTileEntity instanceof ITileEntitySynchronising) if (tSet.add(tTileEntity)) {((ITileEntitySynchronising)tTileEntity).sendUpdateToPlayer(aEvent.getPlayer());}
				tIterations = 0;
			} catch(ConcurrentModificationException e) {
				if (tIterations <= 0) ERR.println("Failed to Iterate 8 times. Giving up on sending Data to Client!");
			} catch(Throwable e) {
				// FastUtils throws a NullPointer instead of a CME...
				if (tIterations <= 0) e.printStackTrace(ERR);
			}
		}
	}
	
	// Edit #1 (BUG-106): old-world MIGRATION — ore/rock entities (material in mMetaData) are poured into
	// the chunk map (PrefixBlockOreMap) and removed FOREVER (the chunk is marked for saving — on write they will already be
	// gone). Entities with mItemNBT (audit channel #8) keep living, but the material is duplicated into the map so that
	// the read funnel stays unified. The entity type remains registered forever — it is the legacy reader.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onChunkLoadMigrateOres(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		if (aEvent.getLevel() == null || aEvent.getLevel().isClientSide() || !(aEvent.getChunk() instanceof LevelChunk tChunk)) return;
		gregapi.block.prefixblock.PrefixBlock.migrateChunkOres(tChunk); // logic lives in the data's own center (PrefixBlock)
		gregtech.blocks.BlockDiggable.migrateChunkMud(tChunk); // ADAPT-015: same trick — logic lives with the mud data
	}

	// PlayerDestroyItemEvent.original/.entityPlayer (1.7.10) — private fields in neo, getOriginal()/getEntity() (checked,
	// net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent.java). ItemSword/ItemTool (1.7.10 classes) do not exist in neo
	// (no SwordItem/PickaxeItem/DiggerItem under net.minecraft.world.item — checked) — real equivalent: ItemTags.SWORDS/AXES/PICKAXES/
	// SHOVELS/HOES tag checks on ItemStack. inventory.mainInventory (flat mutable array) removed — Inventory.getItem(i)/setItem(i,x).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerDestroyItem(PlayerDestroyItemEvent aEvent) {
		ItemStack aOriginal = aEvent.getOriginal();
		Player aPlayer = aEvent.getEntity();
		// Uhh, why is this null? Must be a Bug somewhere else.
		if (aOriginal == null) return;
		// Only for real Players!
		if (!UT.Entities.isPlayer(aPlayer)) return;
		// No Creative Mode Refill!
		if (UT.Entities.hasInfiniteItems(aPlayer)) return;
		// Tool Break Fatique.
		if (TOOL_BREAK_FATIQUE) {
			if (ST.item_(aOriginal) instanceof MultiItemTool) {
				IToolStats tStats = ((MultiItemTool)ST.item_(aOriginal)).getToolStats(aOriginal);
				if (tStats != null) tStats.afterBreaking(aOriginal, aPlayer);
			} else
			if (!ItemsGT.NO_TOOL_FATIQUE.contains(aOriginal, T) && (aOriginal.is(ItemTags.SWORDS) || aOriginal.is(ItemTags.AXES) || aOriginal.is(ItemTags.PICKAXES) || aOriginal.is(ItemTags.SHOVELS) || aOriginal.is(ItemTags.HOES))) {
				// If you work so hard that your Tool breaks, you should probably take a break yourself. :P
				// EVENTS: Potion.weakness/digSlowdown → neo MobEffects.WEAKNESS/MINING_FATIGUE (Holder, exist). Restored 1:1.
				// ADAPT-002: Mining Fatigue on tool breakage weakened III→I (amplifier 2→0) per player request. Weakness stays 1:1 (III).
				UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.WEAKNESS      ,  300, 2, F);
				UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.MINING_FATIGUE, 1200, 0, F);
			}
		}
		//
		net.minecraft.world.entity.player.Inventory tInv = aPlayer.getInventory();
		// Only work on Vanilla-Sized Player Inventories!
		// 1.7.10 measured mainInventory; getContainerSize() also counts the seven equipment slots.
		if (tInv.getNonEquipmentItems().size() != 36) return;
		//
		int tSlot = tInv.getSelectedSlot();
		// There cant be any Inventory Row above this one.
		if (tSlot >= 27) return;
		// Refill, but only if the Slot in the Hotbar is Empty.
		if (tInv.getItem(tSlot) != null && tInv.getItem(tSlot).getCount() > 0) return;
		// Do not refill Foods!
		if (ST.food(aOriginal) > 0) return;
		// Do not refill Edibles!
		if (aOriginal.getUseAnimation() == ItemUseAnimation.EAT) return;
		// Do not refill Drinkables!
		if (aOriginal.getUseAnimation() == ItemUseAnimation.DRINK) return;
		// Move into First Row.
		if (tSlot < 9) {
			if (ST.equal(aOriginal, tInv.getItem(tSlot+27), T)) {
			if (ST.equal(aOriginal, tInv.getItem(tSlot+18), T)) {
			if (ST.equal(aOriginal, tInv.getItem(tSlot+ 9), T)) {
			tInv.setItem(tSlot, tInv.getItem(tSlot+ 9)); tInv.setItem(tSlot+ 9, ST.nn(NI)); ST.update(aPlayer); return;}
			tInv.setItem(tSlot, tInv.getItem(tSlot+18)); tInv.setItem(tSlot+18, ST.nn(NI)); ST.update(aPlayer); return;}
			tInv.setItem(tSlot, tInv.getItem(tSlot+27)); tInv.setItem(tSlot+27, ST.nn(NI)); ST.update(aPlayer); return;}
			return;
		}
		// Move into Second Row. Usually only with the Double Hotbars Mod.
		if (tSlot < 18) {
			if (ST.equal(aOriginal, tInv.getItem(tSlot+18), T)) {
			if (ST.equal(aOriginal, tInv.getItem(tSlot+ 9), T)) {
			tInv.setItem(tSlot, tInv.getItem(tSlot+ 9)); tInv.setItem(tSlot+ 9, ST.nn(NI)); ST.update(aPlayer); return;}
			tInv.setItem(tSlot, tInv.getItem(tSlot+18)); tInv.setItem(tSlot+18, ST.nn(NI)); ST.update(aPlayer); return;}
			return;
		}
		// Move into Third Row. Unsure if a Triple Hotbar Mod exists, but if it does, well then it is supported.
		if (ST.equal(aOriginal, tInv.getItem(tSlot+ 9), T)) {
		tInv.setItem(tSlot, tInv.getItem(tSlot+ 9)); tInv.setItem(tSlot+ 9, ST.nn(NI)); ST.update(aPlayer); return;}
		return;
	}
	
	// Was cpw.mods-none, and PlayerUseItemEvent.Finish (1.7.10) — does not exist in neo (no "PlayerUseItemEvent" package). The real
	// neo equivalent — LivingEntityUseItemEvent.Finish (checked, net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.java) —
	// fires for ANY LivingEntity (not just Player), an explicit instanceof check was added (the dispatcher got wider).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onItemUseFinish(LivingEntityUseItemEvent.Finish aEvent) {
		if (!(aEvent.getEntity() instanceof Player)) return;
		Player aPlayer = (Player)aEvent.getEntity();

		int[] tStats = FoodsGT.get(aEvent.getItem());
		if (tStats != null) {
			EntityFoodTracker tTracker = EntityFoodTracker.get(aPlayer);
			if (tTracker != null) {
				if (tStats.length > 0 && tStats[0] != 0) tTracker.changeAlcohol    (tStats[0]);
				if (tStats.length > 1 && tStats[1] != 0) tTracker.changeCaffeine   (tStats[1]);
				if (tStats.length > 2 && tStats[2] != 0) tTracker.changeDehydration(tStats[2]);
				if (tStats.length > 3 && tStats[3] != 0) tTracker.changeSugar      (tStats[3]);
				if (tStats.length > 4 && tStats[4] != 0) tTracker.changeFat        (tStats[4]);
				if (tStats.length > 5 && tStats[5] != 0) tTracker.changeRadiation  (tStats[5]);
			}
		}

		CompoundTag tNBT = ItemNBT.get(aEvent.getItem());
		if (tNBT != null && tNBT.contains(NBT_EFFECTS)) { // was hasKey/getCompoundTag/getInteger (1.7.10) — neo CompoundTag: contains/getCompoundOrEmpty/getInt(Optional<Integer>) (checked, CompoundTag.java)
			tNBT = tNBT.getCompoundOrEmpty(NBT_EFFECTS);
			if (RNGSUS.nextInt(100) < tNBT.getInt("chance").orElse(0)) UT.Entities.applyPotion(aPlayer, tNBT.getInt("id").orElse(0), tNBT.getInt("time").orElse(0), tNBT.getInt("lvl").orElse(0), F);
		}

		if (aEvent.getItem().getItem() == Items.APPLE) {
			if (IL.GrC_Applecore.exists()) {
				if (ST.invalid(aEvent.getResultStack())) aEvent.setResultStack(IL.GrC_Applecore.get(1)); else ST.give(aPlayer, IL.GrC_Applecore.get(1), F);
			} else if (IL.Food_Apple_Red_Core.exists()) {
				if (ST.invalid(aEvent.getResultStack())) aEvent.setResultStack(IL.Food_Apple_Red_Core.get(1)); else ST.give(aPlayer, IL.Food_Apple_Red_Core.get(1), F);
			}
		}
	}
	
	// Was PlayerInteractEvent+Action-enum (1.7.10, a single class with fields x/y/z/face/entityPlayer/world) — in neo PlayerInteractEvent is abstract,
	// actions are split across subclasses RightClickBlock/RightClickItem/RightClickEmpty/LeftClickBlock/LeftClickEmpty/EntityInteract(Specific)
	// (checked, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.java); subscribing to the abstract base class still catches
	// all subclasses (ListenerList checked, the bus climbs recursively via parent — fml-decompiled/net/neoforged/bus/ListenerList.java) —
	// inside, the body dispatches via instanceof instead of aEvent.action as before. x/y/z → getPos(), face → getFace() (Direction), world → getLevel().
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerInteraction(PlayerInteractEvent aEvent) {
		Player aPlayer = aEvent.getEntity();
		Level aWorld = aEvent.getLevel();
		int aX = aEvent.getPos().getX(), aY = aEvent.getPos().getY(), aZ = aEvent.getPos().getZ();
		byte aFace = (byte)(aEvent.getFace() == null ? -1 : aEvent.getFace().get3DDataValue());
		if (aPlayer == null || aPlayer.level() == null || aWorld == null) return;

		PLAYER_LAST_CLICKED.put(aPlayer, aEvent.getPos());

		// If a Player rightclicks something, then that Chunk gotta be marked as modified, even if nothing happens.
		// There has been plenty of Bugs in various Mods, because of forgetting to mark things.
		WD.mark(aWorld, aX, aZ);

		ItemStack aStack = aPlayer.getInventory().getSelectedItem();
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		BlockEntity aTileEntity = aWorld.getBlockEntity(aEvent.getPos());

		if (aEvent instanceof PlayerInteractEvent.RightClickBlock aRightClickBlock) { // pattern binding, since setCanceled is only declared on concrete ICancellableEvent subclasses, not on the abstract PlayerInteractEvent (checked, PlayerInteractEvent.java)
			// Fixing a Vanilla Dupe Bug with stacked Music Discs and the Jukebox.
			if (aTileEntity instanceof JukeboxBlockEntity) {
				ItemStack tStack = ((JukeboxBlockEntity)aTileEntity).getTheItem(); // was func_145856_a() (1.7.10 SRG) — neo: getTheItem() (checked, JukeboxBlockEntity.java)
				if (tStack != null) tStack.setCount(1);
				return;
			}
			// You can easily recycle most things in GT6 anyways, so this should not be needed.
			if (IL.TF_Uncrafting.equal(aBlock)) {
				UT.Entities.chat(aPlayer, CHAT_GREG + LH.tt("No cheating! ;)"));
				aRightClickBlock.setCanceled(T);
				return;
			}
			// Just rightclick the Trophy to get the Achievement/Progress.
			if (IL.TF_Trophy.equal(aBlock)) {
				// EVENTS/F18-redundant: the call only fed ST.check→vanilla achievement (F18, neo auto-grants the advancement) — moot.
				// 1.7.10 metadata Block-API (getItemDropped/getDamageValue) removed in favor of LootTable+BlockState (impossible-1:1),
				// but it is not needed here: ST.check is already a no-op. Correctly disabled.
				// ST.check(aPlayer, ST.make(aBlock.getItemDropped(0, RNGSUS, 0), 1, aBlock.getDamageValue(aWorld, aX, aY, aZ)));
				return;
			}
			// Some Clientside Only Stuff.
			if (aPlayer.level().isClientSide() && !aPlayer.isShiftKeyDown()) {
				// Edit #1 (BUG-106): the ore entity no longer exists — bedrock ore is recognized by the block itself (same pair).
				{
					// Show uses for Bedrock Ore when clicking it.
					if (aBlock == BlocksGT.oreBedrock || aBlock == BlocksGT.oreSmallBedrock) {
						RM.BedrockOreList.openNEI();
					}
				}
			}
			if (ST.valid(aStack)) {
				// Preventing a Railcraft Crash with Fluid Container Items.
				if (aStack.getItem() instanceof IFluidContainerItem && !aPlayer.isShiftKeyDown() && aTileEntity != null && aTileEntity.getClass().getName().startsWith("mods.railcraft.common")) {
					aRightClickBlock.setCanceled(T);
					return;
				}
				/* I think this was for fixing some Adventure Mode related thing. Probably placing Scaffolds with Leftclick was broken, but I ended up fixing it another way.
				if (MD.IC2.mLoaded && SIDES_HORIZONTAL[aFace] && !aPlayer.isShiftKeyDown() && !aPlayer.getAbilities().mayBuild && !aWorld.canPlaceEntityOnSide(aBlock, aX+OFFSETS_X[aFace], aY, aZ+OFFSETS_Z[aFace], F, aFace, aPlayer, aStack)) {
					if (IL.IC2_Scaffold.equal(aBlock) && IL.IC2_Scaffold.equal(aStack, F, T)) {
						aBlock.onBlockClicked(aWorld, aX, aY, aZ, aPlayer);
						aPlayer.swingItem();
						aRightClickBlock.setCanceled(T);
						return;
					}
					if (IL.IC2_Scaffold_Iron.equal(aBlock) && IL.IC2_Scaffold_Iron.equal(aStack, F, T)) {
						aBlock.onBlockClicked(aWorld, aX, aY, aZ, aPlayer);
						aPlayer.swingItem();
						aRightClickBlock.setCanceled(T);
						return;
					}
				}*/
				if (!(aStack.getItem() instanceof IItemNoGTOverride)) {
					// Dollies won't work on GT6 TileEntities, so to prevent a Crash and deleted Resources, I just disable the interaction.
					if (IL.JABBA_Dolly.equal(aStack, T, T) || IL.JABBA_Dolly_Diamond.equal(aStack, T, T)) {
						if (aTileEntity instanceof ITileEntitySpecificPlacementBehavior) {
							UT.Entities.chat(aPlayer, CHAT_GREG + LH.tt("The Dolly Code is sadly not smart enough to move this TileEntity."), CHAT_GREG + LH.tt("It would crash if it actually did, so be glad I prevented your mistake."), CHAT_GREG + LH.tt("Would be great if it did work though..."));
							aRightClickBlock.setCanceled(T);
						}
						return;
					}
					// Instant breaking for those Wrenches.
					if (IL.BC_Wrench.equal(aStack, T, T) || IL.FR_Wrench.equal(aStack, T, T) || IL.SC2_Wrench.equal(aStack, T, T) || IL.AE_Wrench_Certus.equal(aStack, T, T) || IL.AE_Wrench_Quartz.equal(aStack, T, T) || IL.TE_Wrench.equal(aStack, T, T) || IL.TE_Wrench_Battle.equal(aStack, T, T)) {
						List<String> tChatReturn = new ArrayListNoNulls<>();
						long tDamage = IBlockToolable.Util.onToolClickWithoutCompat(TOOL_wrench, Long.MAX_VALUE, 3, aPlayer, tChatReturn, aPlayer.getInventory(), aPlayer.isShiftKeyDown(), aStack, aPlayer.level(), aFace, aX, aY, aZ, 0.5F, 0.5F, 0.5F);
						UT.Entities.sendchat(aPlayer, tChatReturn, F);
						if (tDamage > 0) {
							ST.use(aPlayer, aStack);
							UT.Sounds.send(SFX.MC_BREAK, aWorld, aX, aY, aZ);
							aRightClickBlock.setCanceled(T);
						}
						return;
					}
					// Instant breaking for those Soft Hammers.
					if (IL.MFR_Hammer.equal(aStack, T, T)) {
						List<String> tChatReturn = new ArrayListNoNulls<>();
						long tDamage = IBlockToolable.Util.onToolClickWithoutCompat(TOOL_softhammer, Long.MAX_VALUE, 3, aPlayer, tChatReturn, aPlayer.getInventory(), aPlayer.isShiftKeyDown(), aStack, aPlayer.level(), aFace, aX, aY, aZ, 0.5F, 0.5F, 0.5F);
						UT.Entities.sendchat(aPlayer, tChatReturn, F);
						if (tDamage > 0) {
							ST.use(aPlayer, aStack);
							UT.Sounds.send(SFX.MC_BREAK, aWorld, aX, aY, aZ);
							aRightClickBlock.setCanceled(T);
						}
						return;
					}
					// Instant breaking for those Hard Hammers.
					if (IL.IE_Hammer.equal(aStack, F, T) || IL.A97_Hammer.equal(aStack, T, T) || IL.SC2_Hammer.equal(aStack, T, T) || IL.SC2_Hammer_Gilded.equal(aStack, T, T)) {
						List<String> tChatReturn = new ArrayListNoNulls<>();
						long tDamage = IBlockToolable.Util.onToolClickWithoutCompat(TOOL_hammer, Long.MAX_VALUE, 3, aPlayer, tChatReturn, aPlayer.getInventory(), aPlayer.isShiftKeyDown(), aStack, aPlayer.level(), aFace, aX, aY, aZ, 0.5F, 0.5F, 0.5F);
						UT.Entities.sendchat(aPlayer, tChatReturn, F);
						if (tDamage > 0) {
							ST.use(aPlayer, aStack);
							UT.Sounds.send(SFX.MC_BREAK, aWorld, aX, aY, aZ);
							aRightClickBlock.setCanceled(T);
						}
						return;
					}
					// Make Railcrafts Crowbars work on GT6 Stuff.
					if (IL.RC_Crowbar_Iron.equal(aStack, T, T) || IL.RC_Crowbar_Steel.equal(aStack, T, T) || IL.RC_Crowbar_Thaumium.equal(aStack, T, T) || IL.RC_Crowbar_Voidmetal.equal(aStack, T, T)) {
						List<String> tChatReturn = new ArrayListNoNulls<>();
						long tDamage = IBlockToolable.Util.onToolClickWithoutCompat(TOOL_crowbar, Long.MAX_VALUE, 2, aPlayer, tChatReturn, aPlayer.getInventory(), aPlayer.isShiftKeyDown(), aStack, aPlayer.level(), aFace, aX, aY, aZ, 0.5F, 0.5F, 0.5F);
						UT.Entities.sendchat(aPlayer, tChatReturn, F);
						if (tDamage > 0) {
							aStack.hurtAndBreak((int)UT.Code.units(tDamage, 10000, 1, T), aPlayer, InteractionHand.MAIN_HAND); // was damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (checked, ItemStack.java:524)
							if (aStack.getDamageValue() >= aStack.getMaxDamage()) ST.use(aPlayer, aStack);
							aRightClickBlock.setCanceled(T);
						}
						return;
					}
					// Make Forestry Scoops work on GT6 Stuff.
					if (IL.FR_Scoop.equal(aStack, T, T)) {
						List<String> tChatReturn = new ArrayListNoNulls<>();
						long tDamage = IBlockToolable.Util.onToolClickWithoutCompat(TOOL_scoop, Long.MAX_VALUE, 0, aPlayer, tChatReturn, aPlayer.getInventory(), aPlayer.isShiftKeyDown(), aStack, aWorld, aFace, aX, aY, aZ, 0.5F, 0.5F, 0.5F);
						UT.Entities.sendchat(aPlayer, tChatReturn, F);
						if (tDamage > 0) {
							aStack.hurtAndBreak((int)UT.Code.units(tDamage, 10000, 1, T), aPlayer, InteractionHand.MAIN_HAND); // was damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (checked, ItemStack.java:524)
							if (aStack.getDamageValue() >= aStack.getMaxDamage()) ST.use(aPlayer, aStack);
							aRightClickBlock.setCanceled(T);
						}
						return;
					}
					// Make Railcrafts Firestone work as Flint and Steel on TNT and GT6 Machines
					if (IL.RC_Firestone_Refined.equal(aStack, T, T) || IL.RC_Firestone_Cracked.equal(aStack, T, T)) {
						List<String> tChatReturn = new ArrayListNoNulls<>();
						long tDamage = IBlockToolable.Util.onToolClickWithoutCompat(TOOL_igniter, Long.MAX_VALUE, Long.MAX_VALUE, aPlayer, tChatReturn, aPlayer.getInventory(), aPlayer.isShiftKeyDown(), aStack, aWorld, aFace, aX, aY, aZ, 0.5F, 0.5F, 0.5F);
						UT.Entities.sendchat(aPlayer, tChatReturn, F);
						if (tDamage > 0) {
							aStack.hurtAndBreak((int)UT.Code.units(tDamage, 10000, 1, T), aPlayer, InteractionHand.MAIN_HAND); // was damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (checked, ItemStack.java:524)
							if (aStack.getDamageValue() >= aStack.getMaxDamage()) ST.use(aPlayer, aStack);
							UT.Sounds.send(SFX.MC_IGNITE, aWorld, aX, aY, aZ);
							aRightClickBlock.setCanceled(T);
						}
						return;
					}
					// Make Twilight Forests Lamp of Cinders work as infinite Flint and Steel on TNT and GT6 Machines. Should be a good reward for getting to it.
					if (IL.TF_Lamp_of_Cinders.equal(aStack, T, T)) {
						List<String> tChatReturn = new ArrayListNoNulls<>();
						long tDamage = IBlockToolable.Util.onToolClick(TOOL_igniter, Long.MAX_VALUE, Long.MAX_VALUE, aPlayer, tChatReturn, aPlayer.getInventory(), aPlayer.isShiftKeyDown(), aStack, aWorld, aFace, aX, aY, aZ, 0.5F, 0.5F, 0.5F);
						UT.Entities.sendchat(aPlayer, tChatReturn, F);
						if (tDamage > 0) aRightClickBlock.setCanceled(T);
						return;
					}
					if (IL.TF_Transformation_Powder.equal(aStack, T, T)) {
						// Make Twilight Forests Transformation Powder work on Mob Spawners
						// F10 external-compat (foreign-gated): TF Transformation Powder (TF is absent from the build → this branch is dead).
						// 1.7.10 BaseSpawner String-API (getEntityNameToSpawn/setEntityName) → neo EntityType model (setEntityId(
						// EntityType,Level,RandomSource,BlockPos), BaseSpawner.java:55); the neo path when TF is present = BuiltInRegistries.
						// ENTITY_TYPE.get(Identifier) for a String→EntityType bridge over TRANSFORMATION_POWDER_SPAWNER_MAP. Gated F (TF absent).
						if (F && aTileEntity instanceof SpawnerBlockEntity) {
							if (aWorld.isClientSide()) return;
							BaseSpawner tSpawner = ((SpawnerBlockEntity)aTileEntity).getSpawner(); // was func_145881_a() (1.7.10 SRG) — neo: getSpawner() (checked, SpawnerBlockEntity.java:93)
							if (ST.use(aPlayer, aStack, 16)) {
								// I hope this works sync the new Mob Data over.
								aWorld.sendBlockUpdated(aEvent.getPos(), aWorld.getBlockState(aEvent.getPos()), aWorld.getBlockState(aEvent.getPos()), 3); // was markBlockForUpdate(x,y,z) (1.7.10) — neo: Level.sendBlockUpdated(pos,old,new,flags) (checked, Level.java:333)
							} else {
								UT.Entities.sendchat(aPlayer, LH.tt("You need 16 Bags of Transformation Powder to convert this!"));
							}
							aRightClickBlock.setCanceled(T);
							return;
						}
					}
				}
			}
		}

		if (aEvent instanceof PlayerInteractEvent.RightClickBlock || aEvent instanceof PlayerInteractEvent.RightClickItem) { // was aEvent.action==RIGHT_CLICK_BLOCK||RIGHT_CLICK_AIR
			if (ST.valid(aStack)) {
				// Make sure that shelvable Items don't do a Rightclick Action instead of being shelved.
				if (aEvent instanceof PlayerInteractEvent.RightClickBlock aRightClickBlock && aTileEntity instanceof ITileEntityBookShelf && ((ITileEntityBookShelf)aTileEntity).isShelfFace(aFace)) {
					aRightClickBlock.setUseBlock(TriState.TRUE);  // was aEvent.useBlock = Result.ALLOW
					if (BooksGT.BOOK_REGISTER.containsKey(aStack, T)) aRightClickBlock.setUseItem(TriState.FALSE); // was aEvent.useItem = Result.DENY
					return;
				}
				// Reload Guns with the potential Ammo in this Slot if applicable. Ugly Code, I know.
				if (!aPlayer.level().isClientSide()) {
					for (int i = 0; i < aPlayer.getInventory().getContainerSize(); i++) {
						ItemStack tStack = aPlayer.getInventory().getItem(i);
						if (ST.item(tStack) instanceof MultiItem) {
							List<IBehavior<MultiItem>> tList = ((MultiItem) ST.item_(tStack)).mItemBehaviors.get(ST.meta_(tStack));
							if (tList != null) for (IBehavior<MultiItem> tBehavior : tList) {
								if (tBehavior instanceof Behavior_Gun) {
									if (((Behavior_Gun) tBehavior).reloadGun(tStack, aPlayer, T)) {
										((ICancellableEvent)aEvent).setCanceled(T); // both RightClickBlock and RightClickItem implement ICancellableEvent (checked, PlayerInteractEvent.java)
										return;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	// UseHoeEvent (1.7.10, world/x/y/z/entityPlayer fields) does not exist in neo — replaced by the general BlockToolModificationEvent (ANY
	// ItemAbility, not just the hoe — checked, net.neoforged.neoforge.event.level.BlockEvent.java), so an explicit check was added:
	// getItemAbility()==ItemAbilities.HOE_TILL. F6 (1:1): 1.7.10 "Blocks.dirt && metadata!=0" (coarse=1/podzol=2) → in neo these are
	// SEPARATE Block types Blocks.COARSE_DIRT/Blocks.PODZOL (not dirt metadata) — an exact set, not a reinvention. Restored.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onUseHoeEvent(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent aEvent) {
		if (aEvent.getItemAbility() == net.neoforged.neoforge.common.ItemAbilities.HOE_TILL && (aEvent.getState().getBlock() == Blocks.COARSE_DIRT || aEvent.getState().getBlock() == Blocks.PODZOL)) aEvent.setCanceled(T);
	}

	// F12: blast-resistant-mob-spawners (golden mob_spawner.setResistance(6000000) = blast-immune). neo Properties immutable →
	// equivalent via ExplosionEvent.Detonate: strip SPAWNER positions from the affected blocks (the spawner survives the blast). Config cached.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onExplosionDetonate(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate aEvent) {
		if (BLAST_RESISTANT_MOB_SPAWNERS) aEvent.getAffectedBlocks().removeIf(p -> gregapi.util.WD.state(aEvent.getLevel(), p).getBlock() == net.minecraft.world.level.block.Blocks.SPAWNER);
	}
	
	// BUG-071 MATCHING HALF OF THE HARVEST BRIDGE (to onBlockBreakSpeedEvent below): the RIGHT to drop.
	// A literal port of Forge 1.7.10 ForgeHooks.canHarvestBlock (recompSrc ForgeHooks.java:95-116) — the very method
	// that 1.7.10 used to decide: material requires no tool → allowed; no stack/type → vanilla verdict; tool
	// level < 0 (the class is foreign to the item) → vanilla verdict; otherwise compare LEVELS.
	// Why this was needed at all: in neo the rule is evaluated WITHOUT a position (Item.isCorrectToolForDrops(stack,state)),
	// while GT6's block subtype lives in the BlockEntity — on that path the meta degenerates to 0, and the required level became
	// zero for ALL ores and machines (BUG-071, gt6harvestprobe measurement). The HarvestCheck event does carry a position (PlayerEvent
	// .HarvestCheck:getPos), and the engine actually routes through it: ServerPlayerGameMode:291 → BlockState.canHarvestBlock
	// (level,pos,player) → IBlockExtension:216 → EventHooks.doPlayerHarvestCheck. One point for the whole mod — same as the
	// neighboring BreakSpeed bridge, which Gregorius set up for exactly the same purpose (speed).
	// We touch ONLY GT6 blocks (IBlock contract): foreign blocks are judged by the engine, as before.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerHarvestCheckEvent(PlayerEvent.HarvestCheck aEvent) {
		try {
			net.minecraft.world.level.block.state.BlockState tState = aEvent.getTargetBlock();
			Block tBlock = tState.getBlock();
			if (!(tBlock instanceof gregapi.block.IBlock)) return;
			if (WD.getMaterial(tBlock).isToolNotRequired()) {aEvent.setCanHarvest(T); return;} // :97-100
			net.minecraft.core.BlockPos tPos = aEvent.getPos();
			net.minecraft.world.level.BlockGetter tWorld = aEvent.getLevel();
			ItemStack tStack = aEvent.getEntity().getMainHandItem();
			String tTool = WD.harvestTool(tBlock, WD.meta(tWorld, tPos.getX(), tPos.getY(), tPos.getZ()));
			if (ST.invalid(tStack) || !UT.Code.stringValid(tTool)) return; // :102-107 — vanilla verdict as-is
			int tToolLevel = WD.toolLevel(tStack, tTool);
			if (tToolLevel < 0) return;                                    // :109-113 — class is foreign → vanilla verdict
			aEvent.setCanHarvest(tToolLevel >= WD.harvestLevel(tWorld, tPos.getX(), tPos.getY(), tPos.getZ())); // :115
		} catch (Throwable e) {/* the right to drop must not crash block destruction */}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	@SuppressWarnings("unlikely-arg-type")
	// PlayerEvent.BreakSpeed (1.7.10: block/x/y/z/metadata/newSpeed fields) in neo only carries getState()/getOriginalSpeed()/
	// getNewSpeed()+setNewSpeed()/getPosition() (Optional<BlockPos>) (checked, PlayerEvent.java) — there is a real public setNewSpeed(),
	// so the reflection hack "Aether does something stupid" (working around field inaccessibility) is no longer needed — the plain setter is used.
	// BlockState carries no metadata — WD.meta(Level,x,y,z) is used at the event position (same trick as everywhere else in this file).
	public void onBlockBreakSpeedEvent(PlayerEvent.BreakSpeed aEvent) {
		if (aEvent.getNewSpeed() > 0) {
			Player aPlayer = aEvent.getEntity();
			BlockPos tPos = aEvent.getPosition().orElse(BlockPos.ZERO);
			int aX = tPos.getX(), aY = tPos.getY(), aZ = tPos.getZ();
			Block aBlock2 = aEvent.getState().getBlock();
			// F12: harder-mob-spawners (golden setHardness(500) vs vanilla's 5 → ×100 slower). neo Properties are immutable →
			// equivalent via BreakSpeed: speed × (5/500)=0.01 for a vanilla spawner. Config flag cached in GT_API.
			if (HARDER_MOB_SPAWNERS && aBlock2 == net.minecraft.world.level.block.Blocks.SPAWNER) aEvent.setNewSpeed(aEvent.getNewSpeed() * 0.01F);
			byte aMeta = WD.meta(aPlayer.level(), aX, aY, aZ);
			if (aPlayer != null) {
				ItemStack aStack = aPlayer.getMainHandItem();
				if (aStack != null && aStack.getItem() instanceof MultiItemTool) {
					aEvent.setNewSpeed(((MultiItemTool)aStack.getItem()).onBlockBreakSpeedEvent(aEvent.getNewSpeed(), aStack, aPlayer, aBlock2, aX, aY, aZ, aMeta, aEvent));
				}
			}

			ItemStackContainer tBlock = new ItemStackContainer(aBlock2, 1, aMeta);

			if (OM.prefixcontains(ST.make(aBlock2, 1, aMeta), TD.Prefix.ORE)) {
				aEvent.setNewSpeed((float)(aEvent.getNewSpeed() / HARDNESS_MULTIPLIER_ORES));
				return;
			}
			if (BlocksGT.stoneToBrokenOres.containsKey(tBlock) || BlocksGT.stoneToNormalOres.containsKey(tBlock) || BlocksGT.stoneToSmallOres.containsKey(tBlock)) {
				// F9 (1:1): SAND/ROCK multiplier by the material of the block below. WD.getMaterial(Block) IS IMPLEMENTED (Block→gregapi.Material
				// bridge, WD.java:474) — the stale "removed" tag is lifted.
				if (WD.getMaterial(aBlock2) == gregapi.block.Material.sand || WD.getMaterial(aBlock2) == gregapi.block.Material.clay || WD.getMaterial(aBlock2) == gregapi.block.Material.grass || WD.getMaterial(aBlock2) == gregapi.block.Material.ground) {
					aEvent.setNewSpeed((float)(aEvent.getNewSpeed() / HARDNESS_MULTIPLIER_SAND));
					return;
				}
				aEvent.setNewSpeed((float)(aEvent.getNewSpeed() / HARDNESS_MULTIPLIER_ROCK));
				return;
			}
			if (aBlock2 instanceof IBlockPlacable) {
				if (BlocksGT.stoneToBrokenOres.containsValue(aBlock2) || BlocksGT.stoneToNormalOres.containsValue(aBlock2) || BlocksGT.stoneToSmallOres.containsValue(aBlock2)) {
					aEvent.setNewSpeed((float)(aEvent.getNewSpeed() / HARDNESS_MULTIPLIER_ORES));
					return;
				}
			}
		}
	}

	// BlockEvent.BreakEvent (1.7.10) does not exist in neo (checked, net.neoforged.neoforge.event.level.BlockEvent.java — no nested
	// BreakEvent) — split into BreakBlockEvent (level.block, cancel semantics only, WITHOUT setExpToDrop) and BlockDropsEvent
	// (level, carries getDroppedExperience()/setDroppedExperience(int) — a direct neo equivalent of the old setExpToDrop). EnchantmentHelper.
	// getSilkTouchModifier(Player) (1.7.10) removed — the real neo way: EnchantmentHelper.getItemEnchantmentLevel(Holder<Enchantment>,LivingEntity)
	// via the Silk Touch Holder from RegistryAccess (checked, EnchantmentHelper.java:292 + Enchantments.SILK_TOUCH).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onBlockBreakingEvent(BlockDropsEvent aEvent) {
		if (aEvent.getState().getBlock() instanceof IPrefixBlock) {
			Holder<Enchantment> tSilkTouch = aEvent.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
			if (aEvent.getBreaker() instanceof LivingEntity aBreaker && EnchantmentHelper.getEnchantmentLevel(tSilkTouch, aBreaker) > 0) aEvent.setDroppedExperience(0);
		}
	}

	// BlockEvent.HarvestDropsEvent (1.7.10: List<ItemStack> drops, block/blockMetadata/world/x/y/z/harvester/isSilkTouching/fortuneLevel)
	// does not exist in neo — replaced by BlockDropsEvent (checked, net.neoforged.neoforge.event.level.BlockDropsEvent.java), carrying
	// List<ItemEntity> (not ItemStack) and without separate isSilkTouching/fortuneLevel/blockMetadata fields. gregapi.item.multiitem.MultiItemTool
	// (not my file) is ALREADY ported for this case — onHarvestBlockEvent(ArrayList<ItemStack>,...,Player,...,BlockDropsEvent) and
	// canCollectDropsDirectly(ItemStack,Block,byte) keep the old shape exactly as ArrayList<ItemStack> (checked, MultiItemTool.java) —
	// so a local ArrayList<ItemStack> bridge is built here over the ItemEntity drops, all 1.7.10 logic runs on it 1:1,
	// then the ItemEntity list is synced back. isSilkTouching/fortuneLevel are computed via EnchantmentHelper on the tool.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onBlockHarvestingEvent(BlockDropsEvent aEvent) {
		ArrayListNoNulls<ItemStack> aDropStacks = new ArrayListNoNulls<>();
		for (ItemEntity tDropEntity : aEvent.getDrops()) aDropStacks.add(tDropEntity.getItem());

		Level aWorld = aEvent.getLevel();
		int aX = aEvent.getPos().getX(), aY = aEvent.getPos().getY(), aZ = aEvent.getPos().getZ();
		// F13 contract (BUG-016): 1.7.10 HarvestDropsEvent.blockMetadata = meta of the DESTROYED block; in neo the block is already
		// removed from the world by this point (meta(aWorld,...)=0 always) — the meta is taken from the event's state snapshot.
		byte aBlockMeta = WD.meta(aEvent.getState());
		Entity aHarvesterEntity = aEvent.getBreaker();
		Holder<Enchantment> tSilkTouchHolder = aWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		Holder<Enchantment> tFortuneHolder = aWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);

		Iterator<ItemStack> aDrops = aDropStacks.iterator();
		Block aBlock = (aEvent.getState().getBlock() == Blocks.REDSTONE_ORE ? Blocks.REDSTONE_ORE : aEvent.getState().getBlock() == Blocks.REDSTONE_LAMP ? Blocks.REDSTONE_LAMP : aEvent.getState().getBlock() == BlocksGT.EtFu_Deepslate_Lit_Redstone_Ore ? BlocksGT.EtFu_Deepslate_Redstone_Ore : aEvent.getState().getBlock());

		while (aDrops.hasNext()) {
			ItemStack aDrop = aDrops.next();
			if (ST.invalid(aDrop) || ItemsGT.ILLEGAL_DROPS.contains(aDrop, T)) {aDrops.remove(); continue;}
			if (ST.item_(aDrop) == Items.GOLD_NUGGET) ST.meta_(aDrop, 0);
			if (FORCE_GRAVEL_NO_FLINT && aBlock == Blocks.GRAVEL && ST.item_(aDrop) == Items.FLINT) ST.set(aDrop, ST.make(Blocks.GRAVEL, 1, 0), T, F);
		}

		if (aBlock == null) {aEvent.getDrops().clear(); for (ItemStack tStack : aDropStacks) if (ST.valid(tStack)) aEvent.getDrops().add(new ItemEntity(aWorld, aX+0.5, aY+0.5, aZ+0.5, tStack)); return;}

		if (aBlock == Blocks.COARSE_DIRT) for (int i = 0, j = aDropStacks.size(); i < j; i++) if (ST.block(aDropStacks.get(0)) == Blocks.DIRT) {
			aDropStacks.set(i, ST.make(Blocks.COARSE_DIRT, aDropStacks.get(i).getCount(), 0));
		}

		boolean aIsSilkTouching = aHarvesterEntity instanceof LivingEntity aHarvesterLiving0 && EnchantmentHelper.getEnchantmentLevel(tSilkTouchHolder, aHarvesterLiving0) > 0;
		int aFortuneLevel = aHarvesterEntity instanceof LivingEntity aHarvesterLiving1 ? EnchantmentHelper.getEnchantmentLevel(tFortuneHolder, aHarvesterLiving1) : 0;

		if (IL.TF_Mushgloom_Huge.equal(aBlock)) {
			aDropStacks.clear();
			aDropStacks.add(aIsSilkTouching ? IL.TF_Mushgloom_Huge.get(1) : IL.TF_Mushgloom.get(UT.Code.bind(1, 4, RNGSUS.nextInt(3) + RNGSUS.nextInt(1+aFortuneLevel))));
		}

		if (aHarvesterEntity != null && aHarvesterEntity instanceof Player aHarvester) {
			if (FAST_LEAF_DECAY) WD.leafdecay(aWorld, aX, aY, aZ, aBlock, F, F);
			ItemStack aTool = aHarvester.getMainHandItem();
			if (aTool != null) {
				boolean
				tFireAspect = (UT.NBT.getEnchantmentLevel(Enchantments.FIRE_ASPECT, aTool) >= 3),
				tCanCollect = (ST.item_(aTool) instanceof MultiItemTool && ((MultiItemTool)ST.item_(aTool)).canCollectDropsDirectly(aTool, aBlock, aBlockMeta));

				if (ST.item_(aTool) instanceof MultiItemTool) {
					((MultiItemTool)ST.item_(aTool)).onHarvestBlockEvent(aDropStacks, aTool, aHarvester, aBlock, aX, aY, aZ, aBlockMeta, aFortuneLevel, aIsSilkTouching, aEvent);
				}

				for (ItemStack tDrop : aDropStacks) {
					ItemStack tTarget = (aIsSilkTouching ? BlocksGT.blockToSilk : BlocksGT.blockToDrop).get(tDrop);
					if (ST.valid(tTarget)) OM.set(ST.set(tDrop, tTarget, F, F)); else OM.set(tDrop);
				}

				if (tFireAspect) for (ItemStack tDrop : aDropStacks) {
					ItemStack tTarget = RM.get_smelting(tDrop);
					if (ST.valid(tTarget)) {
						tDrop.setCount(tDrop.getCount()*(tTarget.getCount()));
						OM.set(ST.set(tDrop, tTarget, F, T));
						tTarget = (aIsSilkTouching?BlocksGT.blockToSilk:BlocksGT.blockToDrop).get(tDrop);
						if (ST.valid(tTarget)) OM.set(ST.set(tDrop, tTarget, F, F));
					} else {
						WoodEntry tWoodEntry = WoodDictionary.WOODS.get(tDrop);
						if (tWoodEntry != null && tWoodEntry.mCharcoalCount > 0) {
							ST.set(tDrop, OP.gem.mat(MT.Charcoal, tWoodEntry.mCharcoalCount * tDrop.getCount()), T, F);
						} else {
							BeamEntry tBeamEntry = WoodDictionary.BEAMS.get(tDrop);
							if (tBeamEntry != null && tBeamEntry.mCharcoalCount > 0) {
								ST.set(tDrop, OP.gem.mat(MT.Charcoal, tBeamEntry.mCharcoalCount * tDrop.getCount()), T, F);
							}
						}
					}
				}

				// BUG-040: restored 1:1 the 1.7.10 mechanism "the tool collects a harvested drop straight into the inventory"
				// (original onHarvestDrops:1342-1369). The former "F &&" stub is removed — its justification ("ST.entity(Entity,
				// ItemStack) is missing") was a grep error: the method DOES EXIST (ST.java:614, type ItemEntity, not the old
				// EntityItem — hence the grep miss). A synthetic ItemEntity (ST.entity_ does NOT spawn into the world —
				// ST.java:615, no addFreshEntity → no dupe) is posted as ItemEntityPickupEvent.Pre — a 1:1 analog of the 1.7.10
				// EntityItemPickupEvent ("ask other mods whether they intercept the pickup", checked ItemEntityPickupEvent.java).
				// Engine mapping (F-adaptation at the engine's level): Result.ALLOW → canPickup()==TriState.TRUE; isDead → isRemoved().
				// The original lines isDead=F/=T are omitted: the ST.entity synthetic is by default not removed and not added to the world
				// (ephemeral, GC'd) — behavior is identical. Nobody intercepted → ST.add puts it into the player's inventory (+sound).
				if (tCanCollect && !aDropStacks.isEmpty()) {
					boolean aCollectSound = T;
					aDrops = aDropStacks.iterator();
					while (aDrops.hasNext()) {
						ItemStack aDrop = aDrops.next();
						if (ST.valid(aDrop)) {
							aDrop = ST.update(aDrop, aWorld, aX, aY, aZ);
							ItemEntity tEntity = ST.entity(aHarvester, aDrop);
							if (tEntity != null) {
								ItemEntityPickupEvent.Pre tEvent = new ItemEntityPickupEvent.Pre(aHarvester, tEntity);
								ST.set(aDrop, tEvent.getItemEntity().getItem(), T, T);
								NeoForge.EVENT_BUS.post(tEvent);
								if (tEvent.canPickup() == TriState.TRUE || tEntity.isRemoved() || aDrop.getCount() <= 0 || ST.invalid(aDrop)) {
									aDrops.remove();
								} else if (ST.add(aHarvester, aDrop)) {
									aDrops.remove();
									if (aCollectSound) {
										UT.Sounds.send(SFX.MC_COLLECT, 0.2F, ((RNGSUS.nextFloat()-RNGSUS.nextFloat())*0.7F+1.0F)*2.0F, aHarvester);
										aCollectSound = F;
									}
								}
							}
						}
					}
				}
			}
			ST.denull(aHarvester);
		}

		aEvent.getDrops().clear();
		for (ItemStack tStack : aDropStacks) if (ST.valid(tStack)) aEvent.getDrops().add(new ItemEntity(aWorld, aX+0.5, aY+0.5, aZ+0.5, tStack));
	}
	
	// EntityJoinLevelEvent.entity (1.7.10 via type-dictionary renaming) — a private field in neo, getEntity() (checked, EntityEvent.java).
	// ItemEntity.getEntityItem/setEntityItemStack/isDead/setDead (1.7.10) — neo: getItem/setItem/isRemoved/discard (checked, ItemEntity.java/Entity.java).
	// World.isClientSide() → Level.isClientSide(). World.findNearestEntityWithinAABB(Class,AABB,Entity) removed — the real neo path:
	// Level.getEntities(Entity,AABB,Predicate) + an isInstance check by class (checked, EntityGetter.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntitySpawningEvent(EntityJoinLevelEvent aEvent) {
		if (aEvent.getEntity() instanceof ItemEntity && !aEvent.getEntity().level().isClientSide()) {
			ItemStack aStack = ST.update(OM.get(((ItemEntity)aEvent.getEntity()).getItem()), aEvent.getEntity());
			if (ST.valid(aStack) && aStack.getCount() > 0) {
				Item aItem = ST.item_(aStack);
				if (ST.meta_(aStack) == W || aItem == Items.GOLD_NUGGET) ST.meta(aStack, 0);
				if (ST.meta_(aStack) == 0 && aItem == IL.TF_Mushgloom.item()) ST.meta(aStack, 9);
				// Check if this is likely a badly implemented Mob Drop from a Mo'Creatures Mob.
				try {
					Class<?> tMoCClass = Class.forName("drzhark.mocreatures.entity.IMoCEntity");
					if (!aEvent.getEntity().level().getEntities(aEvent.getEntity(), aEvent.getEntity().getBoundingBox().inflate(0.5,1.0,0.5), tMoCClass::isInstance).isEmpty()) {
					// Replace stupid Wooden and Stone Tools that clutter up Mob Farms for no reason, but only if nonplayerkill.
					if (aItem == Items.WOODEN_SWORD || aItem == Items.WOODEN_PICKAXE || aItem == Items.WOODEN_SHOVEL || aItem == Items.WOODEN_AXE || aItem == Items.WOODEN_HOE) {
						ST.set(aStack, IL.Stick.get(1));
					} else if (aItem == Items.STONE_SWORD || aItem == Items.STONE_PICKAXE || aItem == Items.STONE_SHOVEL || aItem == Items.STONE_AXE || aItem == Items.STONE_HOE) {
						ST.set(aStack, IL.Stick.get(2));
					}
				}} catch(Throwable e) {/** Do Nothing */}
				// Life Span Stuff
				if (((ItemEntity)aEvent.getEntity()).lifespan > 1200) {
					if (ST.item_(aStack) == Items.EGG || ST.item_(aStack) == Items.FEATHER || ST.item_(aStack) == Items.APPLE) {
						((ItemEntity)aEvent.getEntity()).lifespan = 1200;
					} else {
						if (((ItemEntity)aEvent.getEntity()).lifespan == 6000) {
							((ItemEntity)aEvent.getEntity()).lifespan = ITEM_DESPAWN_TIME;
						}
					}
				}
				// Result was valid so set the ItemStack.
				((ItemEntity)aEvent.getEntity()).setItem(aStack);
			} else {
				// Result was invalid therefore kill the Stack.
				// ⛔ CRASH CLASS (log04, 2026-08-07): discard() used to sit here — a literal port of 1.7.10 setDead(),
				// and in neo it BREAKS THE ENGINE. The event is posted INSIDE the entity-add-to-world sequence
				// (PersistentEntitySectionManager.addEntity:80), BEFORE setLevelCallback: discard() at this moment
				// pushes the entity into removed while the callback is STILL empty (EntityInLevelCallback.NULL) — i.e.
				// the "removal" notifies nobody. The engine keeps adding as if nothing happened
				// (addEntityWithoutEvent: section → callback → startTracking) and files an ALREADY-REMOVED entity
				// into ChunkMap.entityMap. Later that entry breaks the tracker-map traversal: NPE in the fastutil iterator
				// at ChunkMap.tick:1206 (whoever iterates crashes, not whoever broke it — we are not in that stack).
				// The proper neo way for "the entity does not appear" is cancelling the event: the engine returns false BEFORE
				// the add (addEntity:80), it never enters the world at all. The observed behavior is the same as
				// setDead in 1.7.10, where neither entityMap nor the callback mechanism existed.
				aEvent.setCanceled(true);
				return;
			}
		}
	}
	
	public static List<ServerPlayer> mNewPlayers = new ArrayListNoNulls<>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingDeath(LivingDeathEvent aEvent) {
		if (aEvent.getEntity() instanceof ServerPlayer) NW_API.sendToPlayer(new PacketDeathPoint(UT.Code.roundDown(aEvent.getEntity().getX()), UT.Code.roundDown(aEvent.getEntity().getY()), UT.Code.roundDown(aEvent.getEntity().getZ())), (ServerPlayer)aEvent.getEntity());
	}

	// Was cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent (1.7.10) — does not exist in neo. The real neo equivalent —
	// net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent (checked, PlayerEvent.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLoginEvent(PlayerEvent.PlayerLoggedInEvent aEvent) {
		if (DISABLE_ALL_IC2_COMPRESSOR_RECIPES) ic2.api.recipe.Recipes.compressor.getRecipes().clear();
		if (DISABLE_ALL_IC2_EXTRACTOR_RECIPES ) ic2.api.recipe.Recipes.extractor .getRecipes().clear();
		if (DISABLE_ALL_IC2_MACERATOR_RECIPES ) ic2.api.recipe.Recipes.macerator .getRecipes().clear();
		if (DISABLE_ALL_IC2_OREWASHER_RECIPES ) ic2.api.recipe.Recipes.oreWashing.getRecipes().clear();
		if (DISABLE_ALL_IC2_CENTRIFUGE_RECIPES) ic2.api.recipe.Recipes.centrifuge.getRecipes().clear();

		if (aEvent.getEntity().level().isClientSide()) return;
		if (aEvent.getEntity() instanceof ServerPlayer) mNewPlayers.add((ServerPlayer)aEvent.getEntity());
	}
	
	// F6: the old IWorldGenerator.generate(Random,int,int,Level,IChunkProvider,IChunkProvider) and the
	// commented-out bridge sketch to PopulateChunkEvent were removed — neither makes sense in neo
	// (IWorldGenerator and PopulateChunkEvent do not exist; IChunkProvider is a 1.7.10-only type). The real
	// entry point: gregapi.worldgen.GT6WorldgenFeature.place(FeaturePlaceContext) — a custom Feature,
	// registered centrally through PlacedFeature+BiomeModifier (see the class javadoc above and
	// decisions/F6-worldgen.md). The dispatcher GT6WorldGenerator.generate(Level,int,int,boolean) is untouched.

	// ItemExpireEvent.entity/.entityItem/.extraLife (1.7.10) — private fields in neo: getEntity()/getExtraLife()+setExtraLife(int)/
	// addExtraLife(int) (checked, net.neoforged.neoforge.event.entity.item.ItemExpireEvent.java) — there is no separate getItemEntity(),
	// ItemEvent.getEntity() is covariantly overridden and ALREADY returns ItemEntity (checked, ItemEvent.java). The event is NO LONGER
	// cancellable (no ICancellableEvent) — the engine itself decides whether to extend life based on the final getExtraLife() and calls discard()
	// if the age is still >= lifespan (checked, EventHooks.onItemExpire + ItemEntity.java:186-191) — all aEvent.setCanceled(T) calls were removed,
	// while the direct ItemEntity discard() stays where it forcibly killed the stack IMMEDIATELY before, ahead of the engine's natural path.
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemExpireEvent(ItemExpireEvent aEvent) {
		if (aEvent.getEntity().level().isClientSide()) return;
		ItemStack aStack = aEvent.getEntity().getItem();
		int aX = UT.Code.roundDown(aEvent.getEntity().getX()), aY = UT.Code.roundDown(aEvent.getEntity().getY()), aZ = UT.Code.roundDown(aEvent.getEntity().getZ());
		if (ST.valid(aStack)) {
			if (ST.item_(aStack) instanceof MultiTileEntityItemInternal) {
				long tExtraLife = ((MultiTileEntityItemInternal)ST.item_(aStack)).onDespawn(aEvent.getEntity(), aStack);
				if (aStack.getCount() <= 0) {
					aEvent.setExtraLife(0);
					aEvent.getEntity().discard();
					return;
				}
				aEvent.getEntity().setItem(aStack);
				if (tExtraLife > 0) {
					aEvent.setExtraLife(UT.Code.bindInt(aEvent.getExtraLife() + tExtraLife));
					return;
				}
			}
			MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
			if (tRegistry != null) {
				OreDictItemData tData = OM.anydata(aStack);
				if (tData != null) {
					if (tData.mPrefix == OP.rockGt || tData.mPrefix == OP.oreRaw) for (byte[] tOff : CUBE_3) if (WD.irrelevant(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2]) && tRegistry.mBlock.placeBlock(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2], SIDE_TOP, (short)32074, ST.save(NBT_VALUE, aStack), T, F)) {aStack.setCount(0); aEvent.setExtraLife(0); aEvent.getEntity().discard(); return;}
					if (tData.mPrefix == OP.ingot                               ) for (byte[] tOff : CUBE_3) if (WD.irrelevant(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2]) && tRegistry.mBlock.placeBlock(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2], SIDE_TOP, (short)32084, ST.save(NBT_VALUE, aStack), T, F)) {aStack.setCount(0); aEvent.setExtraLife(0); aEvent.getEntity().discard(); return;}
					if (tData.mPrefix == OP.plate                               ) for (byte[] tOff : CUBE_3) if (WD.irrelevant(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2]) && tRegistry.mBlock.placeBlock(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2], SIDE_TOP, (short)32085, ST.save(NBT_VALUE, aStack), T, F)) {aStack.setCount(0); aEvent.setExtraLife(0); aEvent.getEntity().discard(); return;}
					if (tData.mPrefix == OP.plateGem                            ) for (byte[] tOff : CUBE_3) if (WD.irrelevant(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2]) && tRegistry.mBlock.placeBlock(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2], SIDE_TOP, (short)32086, ST.save(NBT_VALUE, aStack), T, F)) {aStack.setCount(0); aEvent.setExtraLife(0); aEvent.getEntity().discard(); return;}
					if (tData.mPrefix == OP.scrapGt                             ) for (byte[] tOff : CUBE_3) if (WD.irrelevant(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2]) && tRegistry.mBlock.placeBlock(aEvent.getEntity().level(), aX+tOff[0], aY+tOff[1], aZ+tOff[2], SIDE_TOP, (short)32103, ST.save(NBT_VALUE, aStack), T, F)) {aStack.setCount(0); aEvent.setExtraLife(0); aEvent.getEntity().discard(); return;}
				}
			}
			GarbageGT.trash(aStack);
			aStack.setCount(0);
			aEvent.setExtraLife(0);
			aEvent.getEntity().setItem(aStack);
			aEvent.getEntity().discard();
			return;
		}
	}
	
	// LivingSpawnEvent.CheckSpawn (1.7.10) does not exist in neo — the closest real analog of "check the spawn position AFTER the mob is created" is
	// MobSpawnEvent.PositionCheck, with its own nested Result{SUCCEED,DEFAULT,FAIL} (NOT the general bus Result — checked,
	// net.neoforged.neoforge.event.entity.living.MobSpawnEvent.java); DENY→FAIL. .entityLiving/.world/.x/.y/.z — getEntity()(Mob)/
	// getEntity().level()/getX()/getY()/getZ() (double, via the entity — getLevel() only returns ServerLevelAccessor, no Level API).
	// WD.dimensionId(World)==0 (1.7.10) → Level.dimension()==Level.OVERWORLD (checked, Level.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onCheckSpawnEvent(MobSpawnEvent.PositionCheck aEvent) {
		if (aEvent.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL) return;
		// F6 neo-async-chunkgen (impossible-1:1; NEUTRALIZED WORLD-CREATE DEADLOCK, caught via jstack): when mobs spawn
		// DURING chunk generation (EntitySpawnReason.CHUNK_GENERATION) neo passes a WorldGenRegion, but the GT6 spawn guards
		// read blocks/light through aEvent.getEntity().level() = the REAL ServerLevel → getBlockState forces
		// ServerChunkCache.getChunk(...).join() on a chunk that is STILL GENERATING → the chunk waits on itself → an eternal
		// deadlock of the worldgen thread → "Loading terrain…" hangs forever (stack: NaturalSpawner.spawnMobsForChunkGeneration →
		// onCheckSpawnEvent → WD.opq → Level.getBlockState → getChunk.join). The GT6 spawn guards are GAMEPLAY-level (the gen phase
		// only places starting passive mobs); the handler is skipped in the gen context, normal spawn (ServerLevel) stays 1:1.
		if (aEvent.getSpawnType() == net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION) return;
		Class<? extends LivingEntity> aMobClass = aEvent.getEntity().getClass();
		Level aWorld = aEvent.getEntity().level();
		int aX = UT.Code.roundDown(aEvent.getX()), aY = (int)UT.Code.bind(WD.minY(aWorld), WD.topY(aWorld), UT.Code.roundDown(aEvent.getY())), aZ = UT.Code.roundDown(aEvent.getZ()); // BUG-089: was bind(0, getHeight()) — spawns at Y<0 got clamped to zero, checks judged the wrong position

		if (SPAWN_NO_BATS && aMobClass == Bat.class && WD.block(aWorld, aX, aY-2, aZ) != Blocks.STONE && WD.block(aWorld, aX, aY+2, aZ) != Blocks.STONE) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}

		if (SPAWN_HOSTILES_ONLY_IN_DARKNESS && WD.dimOverworldLike(aWorld)) try {
			// F-light: 1.7.10 Chunk.getBlockStorageArray()[section].getExtBlocklightValue(...) (per-section block light)
			// is gone — light in neo goes through LevelLightEngine; block light at a point = getBrightness(LightLayer.BLOCK,pos)
			// (LevelReader.java:174 uses that same getBrightness).
			if (aWorld.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new BlockPos(aX, aY, aZ)) > 0) {
				// Vanilla Mobs only, just in case.
				if (aMobClass == Creeper.class || aMobClass == EnderMan.class || aMobClass == Skeleton.class || aMobClass == Zombie.class || aMobClass == Spider.class || aMobClass == Witch.class || aMobClass == Bat.class) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
				// Well, that Zombie is kindof like Vanilla, so it counts.
				if (MD.TC.mLoaded) if (aEvent.getEntity() instanceof EntityBrainyZombie) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
				// TODO Add Drowned and other Et Futurum Requiem Mobs once they are released.
				// EVENTS foreign-gated (Et Futurum Requiem not ported to neo; the instanceof classes do not exist): ganymedes01.etfuturum.entities.{EntityHusk,EntityStray,EntityZombieVillager}
				// (a 1.7.10-era library, not ported to neo) do not extend the modern net.minecraft.world.entity.Mob —
				// the instanceof checks are unconvertible (hard compile error), not merely runtime-false; requires an update of the EtFu library itself.
				// if (MD.EtFu.mLoaded) if (aEvent.getEntity() instanceof EntityZombieVillager || aEvent.getEntity() instanceof EntityStray || aEvent.getEntity() instanceof EntityHusk) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
			}
		} catch(Throwable e) {e.printStackTrace(ERR);}

		if (aWorld.dimension() == Level.OVERWORLD && aY >= WD.waterLevel(aWorld) - 16) {
			if (GENERATE_BIOMES) {
				if (UT.Code.inside(-96,  95, aX) && UT.Code.inside(-96,  95, aZ)) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
			} else if (GENERATE_NEXUS) {
				if (UT.Code.inside(  0,  48, aX) && UT.Code.inside(-64, -16, aZ)) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
			}
			if (GENERATE_STREETS && (UT.Code.inside(-48, 48, aX) || UT.Code.inside(-48, 48, aZ))) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
			// EVENTS: 1.7.10 World.getWorldInfo().getSpawnX()/getSpawnZ() → neo Level.getLevelData().getRespawnData().globalPos().pos()
			// (was mislabeled: "no such methods" — actually renamed to RespawnData). SPAWN_ZONE_MOB_PROTECTION restored 1:1.
			net.minecraft.core.BlockPos tSpawn = aWorld.getLevelData().getRespawnData().globalPos().pos();
			if (SPAWN_ZONE_MOB_PROTECTION && UT.Code.inside(-144, 144, aX-tSpawn.getX()) && UT.Code.inside(-144, 144, aZ-tSpawn.getZ()) && WD.opq(aWorld, aX, 0, aZ, F, F)) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
		}

		//if (aEvent.entity instanceof EntityMob && !(aEvent.entity instanceof IBossDisplayData) && ((EntityMob)aEvent.entity).getCanSpawnHere()) mMobsToFastDespawn.add((EntityLiving)aEvent.entityLiving);

		for (int i = 0; i < MOB_SPAWN_INHIBITORS.size(); i++) {
			ITileEntityMobSpawnInhibitor tTileEntity = MOB_SPAWN_INHIBITORS.get(i);
			if (tTileEntity.isDead()) {
				MOB_SPAWN_INHIBITORS.remove(i--);
				tTileEntity.onUnregisterInhibitor();
			} else try {
				// SEAM PARTIALLY CLOSED (integrator): the interface ITileEntityMobSpawnInhibitor.inhibitMobSpawn(...)
				// was ported onto MobSpawnEvent.PositionCheck (gregapi/tileentity/ITileEntityMobSpawnInhibitor.java).
				// The implementer gregtech/tileentity/multiblocks/MultiTileEntityVonDaGraagg.java was DELIBERATELY rolled back
				// to HEAD by the integrator (71c8179) — that content file carries other unclosed seams (WD raw-coord
				// block API), out of scope for this centralization pass (task #18); the implementer currently
				// does NOT implement the updated interface (a separate, known class of problem).
				if (tTileEntity.inhibitMobSpawn(aEvent, aWorld, aX, aY, aZ)) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
			} catch(Throwable e) {
				MOB_SPAWN_INHIBITORS.remove(i--);
				tTileEntity.setError("Spawn Inhibitor - " + e);
				e.printStackTrace(ERR);
			}
		}
	}
	
	//public static List<EntityLiving> mMobsToFastDespawn = new ArrayListNoNulls<>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityConstructingEvent(EntityConstructing aEvent) {
		if (Abstract_Mod.sFinalized >= Abstract_Mod.sModCountUsingGTAPI && aEvent.getEntity() instanceof Player) EntityFoodTracker.add((Player)aEvent.getEntity());
	}

	// ArrowNockEvent.result (1.7.10 — an ItemStack override for "which item is actually being nocked") does not exist in neo — the modern
	// ArrowNockEvent only carries getBow()/getHand()/getLevel()/hasAmmo()/getAction(InteractionResult) (checked,
	// net.neoforged.neoforge.event.entity.player.ArrowNockEvent.java) — there is no direct way to swap the "nocked" item.
	// EVENTS impossible-1:1 (neo ArrowNockEvent has no result field): no 1:1 replacement for the result field — the body is temporarily inert.
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onArrowNockEvent(ArrowNockEvent aEvent) {
		Player aPlayer = aEvent.getEntity();
		if (F && !aEvent.isCanceled() && aEvent.hasAmmo() && ST.projectile(aPlayer.getInventory(), TD.Projectiles.ARROW) != null) {
			aEvent.setCanceled(T);
		}
	}

	// ArrowLooseEvent.bow/.charge (1.7.10) — getBow()/getCharge() (checked, ArrowLooseEvent.java). ItemBow → BowItem (class
	// rename, checked net.minecraft.world.item.BowItem). World.playSoundAtEntity → the centralized UT.Sounds.send (already used
	// throughout this file). World.spawnEntityInWorld → ServerLevel.addFreshEntity (checked, ServerLevel.java:976).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onArrowLooseEvent(ArrowLooseEvent aEvent) {
		Player aPlayer = aEvent.getEntity();
		ItemStack aArrow = ST.projectile(aPlayer, TD.Projectiles.ARROW);
		if (!aEvent.isCanceled() && ST.valid(aEvent.getBow()) && aArrow != null && aEvent.getBow().getItem() instanceof BowItem) {
			float tSpeed = aEvent.getCharge() / 20.0F;
			tSpeed = (tSpeed * tSpeed + tSpeed * 2.0F) / 3.0F;

			if (tSpeed < 0.1) return;
			if (tSpeed > 1.0) tSpeed = 1.0F;

			EntityProjectile tArrowEntity = ((IItemProjectile)aArrow.getItem()).getProjectile(TD.Projectiles.ARROW, aArrow, aPlayer.level(), aPlayer, tSpeed * 2.0F);

			if (tSpeed >= 1.0F) tArrowEntity.setCritArrow(T); // was setIsCritical(boolean) (1.7.10) — neo AbstractArrow: setCritArrow(boolean) (checked, AbstractArrow.java:540)

			// F-arrow-enchants: 1:1 of the original (gregtech6/src/main/java/gregapi/GT_API_Proxy.java:1563-1569) — Power/Punch/Flame
			// are applied to the projectile MANUALLY here, as the author did, rather than left to the engine. The engine path (carry the bow as
			// AbstractArrow.firedFromWeapon, so EnchantmentHelper.modifyDamage/doKnockback apply themselves) is unreachable for GT6
			// projectiles: the field is private, set only by the constructor Arrow(Level,…,weapon), and that one hardcodes EntityType.ARROW
			// (neo-decompiled Arrow.java:34), whereas GT6 projectiles have their own EntityType (EntitiesGT.ARROW_*).
			// Equivalents were checked by BEHAVIOR, not by name:
			//   Power  1.7.10 setDamage(getDamage()+lvl*0.5+0.5)  -> setBaseDamage(getBaseDamageGT()+…), AbstractArrow.java:671;
			//   Punch  1.7.10 setKnockbackStrength(lvl)           -> center EntityProjectile (the value is applied on hit
			//                                                        by the same formula as in 1.7.10 — EntityArrow_Material:250-253);
			//   Flame  1.7.10 setFire(lvl*100) in SECONDS          -> igniteForSeconds(lvl*100), Entity.java:630 — sets fire
			//                                                        only if longer than the current one, setFire semantics preserved.
			int tLevel = UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.POWER, aEvent.getBow());
			if (tLevel > 0) tArrowEntity.setBaseDamage(tArrowEntity.getBaseDamageGT() + tLevel * 0.5D + 0.5D);
			tLevel = UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.PUNCH, aEvent.getBow());
			if (tLevel > 0) tArrowEntity.setKnockbackStrength(tLevel);
			tLevel = UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FLAME, aEvent.getBow());
			if (tLevel > 0) tArrowEntity.igniteForSeconds(tLevel * 100);

			aEvent.getBow().hurtAndBreak(1, aPlayer, InteractionHand.MAIN_HAND); // was damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (checked, ItemStack.java:524)
			aEvent.getBow().getItem();
			UT.Sounds.send("random.bow", 1.0F, 1.0F / (RNGSUS.nextFloat() * 0.4F + 1.2F) + tSpeed * 0.5F, aPlayer);

			tArrowEntity.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED; // was canBePickedUp=1 (1.7.10 int) — neo: public field pickup of type AbstractArrow.Pickup (checked, AbstractArrow.java:72)

			if (!UT.Entities.hasInfiniteItems(aPlayer)) aArrow.setCount(aArrow.getCount()-1);
			if (aArrow.getCount() == 0) ST.denull(aPlayer);

			if (!aPlayer.level().isClientSide() && aPlayer.level() instanceof ServerLevel aServerLevel) aServerLevel.addFreshEntity(tArrowEntity);

			aEvent.setCanceled(T);
		}
	}
	
	// Body is 1:1, untouched — used to be an "@Override" from the invented IFuelHandler-as-event, now simply
	// called from onFurnaceFuelBurnTime(...) above (F12/R3 bridge).
	public int getBurnTime(ItemStack aFuel) {
		if (ST.invalid(aFuel) || FL.getFluid(aFuel, T) != null) return 0;
		Block aBlock = ST.block(aFuel);
		if (aBlock instanceof BaseRailBlock                                  ) return 0; // Needed so Railcrafts Tunnel Bore works properly and doesn't try to burn its Rails while laying them.
		if (aBlock instanceof HugeMushroomBlock                              ) return (3 * TICKS_PER_SMELT) / 2;
		if (aBlock == BlocksGT.BalesGrass                                    ) return (9 * TICKS_PER_SMELT) / ((ST.meta_(aFuel) & 3) == 1 ? 2 : 4);
		if (aBlock instanceof BlockBaseBale                                  ) return (9 * TICKS_PER_SMELT) / 4;
		if (aBlock instanceof BlockBasePlanks                                ) return (3 * TICKS_PER_SMELT) / 2;
		if (aBlock instanceof BlockBaseSapling                               ) return      TICKS_PER_SMELT  / 2;
		if (aBlock instanceof BlockBaseBeam || aBlock instanceof BlockBaseLog) return  6 * TICKS_PER_SMELT     ;
		long rFuelValue = UT.NBT.getNBT(aFuel).getLong(NBT_FUEL_VALUE).orElse(0L); // was a direct comparison with long (1.7.10) — neo CompoundTag.getLong returns Optional<Long> (checked, CompoundTag.java:331)
		if (aFuel.getItem() instanceof MultiItemRandom) {
			Short tFuelValue = ((MultiItemRandom)aFuel.getItem()).mBurnValues.get(ST.meta_(aFuel));
			if (tFuelValue != null) rFuelValue = Math.max(rFuelValue, tFuelValue);
		} else {
			if (OD.plankAnyWood.is_(aFuel      )) return 3 * TICKS_PER_SMELT / 2;
			if (OD.logWood     .is_(aFuel      )) return 6 * TICKS_PER_SMELT    ;
			if (OD.itemResin   .is_(aFuel      )) return     TICKS_PER_SMELT / 2;
			if (IL.TF_Sapling.equal(aFuel, T, T)) return     TICKS_PER_SMELT / 2;
		}
		
		OreDictItemData tData = OM.anydata_(aFuel);
		if (tData != null && (tData.mFurnaceFuel || rFuelValue != 0)) {
			long tBurnTime = 0;
			if (tData.mPrefix == null) {
				for (OreDictMaterialStack tMaterial : tData.getAllMaterialStacks()) tBurnTime += UT.Code.units(tMaterial.mMaterial.mFurnaceBurnTime, U, tMaterial.mAmount, F);
			} else if (tData.mPrefix == OP.oreRaw) {
				tBurnTime = tData.mMaterial.mMaterial.mFurnaceBurnTime;
			} else if (tData.mPrefix == OP.blockRaw) {
				tBurnTime = tData.mMaterial.mMaterial.mFurnaceBurnTime * 10;
			} else if (tData.mPrefix.contains(TD.Prefix.BURNABLE)) {
				for (OreDictMaterialStack tMaterial : tData.getAllMaterialStacks()) tBurnTime += UT.Code.units(tMaterial.mMaterial.mFurnaceBurnTime, U, tMaterial.mAmount, F);
				if (tData.mPrefix == OP.stick          && ANY.Wood.mToThis.contains(tData.mMaterial.mMaterial)) return (int)UT.Code.bind(0, 32000, Math.max( TICKS_PER_SMELT      /2, tBurnTime));
				if (tData.mPrefix == OP.stickLong      && ANY.Wood.mToThis.contains(tData.mMaterial.mMaterial)) return (int)UT.Code.bind(0, 32000, Math.max( TICKS_PER_SMELT        , tBurnTime));
				if (tData.mPrefix == OP.blockPlate     && ANY.Wood.mToThis.contains(tData.mMaterial.mMaterial)) return (int)UT.Code.bind(0, 32000, Math.max((TICKS_PER_SMELT* 27L)/2, tBurnTime));
				if (tData.mPrefix == OP.crateGtPlate   && ANY.Wood.mToThis.contains(tData.mMaterial.mMaterial)) return (int)UT.Code.bind(0, 32000, Math.max((TICKS_PER_SMELT* 51L)/2, tBurnTime));
				if (tData.mPrefix == OP.crateGt64Plate && ANY.Wood.mToThis.contains(tData.mMaterial.mMaterial)) return (int)UT.Code.bind(0, 32000, Math.max((TICKS_PER_SMELT*195L)/2, tBurnTime));
			}
			rFuelValue = Math.max(rFuelValue, tBurnTime);
		}
		// return at most 160 Smelts, without any fraction smelts.
		return (int)UT.Code.bind(0, 32000, rFuelValue);
	}

	// ==========================================================================================================
	// F6-worldgen: THE SINGLE DISPATCHER FOR DEFERRED FRESH-CHUNK FOLLOW-UP WORK.
	//
	// The common root of every task here is one and the same: `WorldGenRegion` broadcasts NO notifications at all to neighbors
	// (neo-decompiled/server/level/WorldGenRegion.java:257-262 — state is written straight into the chunk), whereas in
	// 1.7.10 populate ran over a live `World.setBlock` with flags 3, and the engine finished the job itself. Everything the
	// engine used to finish now has to be finished explicitly — and necessarily AFTER generation, on the first load of the
	// finished chunk: at the moment of generation itself neighboring chunks may not be ready, and some vanilla features haven't run yet.
	//
	// The tasks differ by flag, but the queue, chunk selection and "chunk not yet FULL" guard are shared — hence
	// ONE mechanism, not one copy per task:
	//   RECHUNK_REDSTONE — dungeons #39: wake up the redstone circuit (in 1.7.10 populate-time torches woke it via flags=3);
	//   RECHUNK_PLANTS   — drop orphaned vegetation (GT6 displaces the bottom half of two-block plants,
	//                      the top half hung in the air without notification — the "tall grass floating over stone" complaint).
	// PRODUCTION mechanism (not a probe).
	// ==========================================================================================================
	public static final int RECHUNK_REDSTONE = 1, RECHUNK_PLANTS = 2;
	private static final int RECHUNK_TRIES_PER_TICK = 64, RECHUNK_JOBS_PER_TICK = 4, RECHUNK_MAX_WAIT = 600;
	private static final java.util.concurrent.ConcurrentLinkedQueue<Object[]> sChunkFinishQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

	@net.neoforged.bus.api.SubscribeEvent
	public void onChunkLoadFinishWorldgen(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		if (!(aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel tLevel)) return;
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		// vegetation is dropped only in a fresh chunk: in an old world whatever is standing belongs to the player.
		int tTasks = aEvent.isNewChunk() ? RECHUNK_PLANTS : 0;
		// redstone — in a dungeon-area chunk on ANY load: the update broadcast is idempotent, and old-world dungeons
		// would otherwise stay with a dead circuit forever (doors "open", report 2026-08-31).
		if (gregapi.worldgen.dungeon.WorldgenDungeonGT.isDungeonAreaChunk(tLevel, tPos.x(), tPos.z())) tTasks |= RECHUNK_REDSTONE;
		if (tTasks != 0) sChunkFinishQueue.add(new Object[] {tLevel, tPos, tTasks, SERVER_TIME});
	}

	// At the time of ChunkEvent.Load the chunk is not yet returned by getChunkNow, so the task is almost always deferred;
	// the previous early-exit on the first not-ready task starved the WHOLE dispatcher tick, and nothing got done across an
	// entire run (2026-08-31 measurement: 8182 accepted, 0 done). A not-ready task no longer stalls the sweep, and a
	// chunk's task that never became ticking within RECHUNK_MAX_WAIT is dropped — otherwise the queue grows forever.
	private static void gt6ChunkFinishTick() {
		java.util.List<Object[]> tWaiting = null;
		int tDone = 0;
		for (int n = 0; n < RECHUNK_TRIES_PER_TICK && tDone < RECHUNK_JOBS_PER_TICK; n++) {
			Object[] tJob = sChunkFinishQueue.poll();
			if (tJob == null) break;
			net.minecraft.server.level.ServerLevel tLevel = (net.minecraft.server.level.ServerLevel)tJob[0];
			net.minecraft.world.level.ChunkPos tPos = (net.minecraft.world.level.ChunkPos)tJob[1];
			int tTasks = (Integer)tJob[2];
			net.minecraft.world.level.chunk.LevelChunk tChunk = tLevel.getChunkSource().getChunkNow(tPos.x(), tPos.z());
			if (tChunk == null) {
				if (SERVER_TIME - (Long)tJob[3] < RECHUNK_MAX_WAIT) {
					if (tWaiting == null) tWaiting = new java.util.ArrayList<>();
					tWaiting.add(tJob);
				}
				continue;
			}
			tDone++;
			if ((tTasks & RECHUNK_PLANTS) != 0) {
				try {gregapi.util.WD.dropUnsupportedPlants(tLevel, tChunk);} catch (Throwable e) {e.printStackTrace(ERR);}
			}
			if ((tTasks & RECHUNK_REDSTONE) != 0) {
				try {gregapi.worldgen.dungeon.WorldgenDungeonGT.wakeRedstone(tLevel, tChunk);} catch (Throwable e) {e.printStackTrace(ERR);}
			}
		}
		if (tWaiting != null) sChunkFinishQueue.addAll(tWaiting);
	}


}
