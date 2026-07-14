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
 */

package gregapi;

import cofh.lib.util.ComparableItem;
// net.neoforged.fml.Logging (был импорт, .severe(String) вызывался) — не логгер, а контейнер log4j Marker-констант
// (сверено, fml-decompiled/net/neoforged/fml/Logging.java) — .severe(...) там не существует; заменено на уже
// централизованный ERR.println(...) (gregapi.data.CS), используемый рядом с тем же текстом.
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
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
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
 * F12 (decisions/F12-registration-lifecycle.md, ревизия R3): конструктор раньше нёс два выдуманных
 * API — {@code DeferredRegister.registerFuelHandler(this)} и {@code DeferredRegister.registerWorldGenerator(this, weight)}
 * — таких методов у NeoForge DeferredRegister нет (сверено, neoforge-decompiled). Класс также
 * незаконно "implements" два конкретных класса, {@code FurnaceFuelBurnTimeEvent} и {@code Feature}
 * (1.7.10 {@code IFuelHandler}/{@code IWorldGenerator} механически переименованы словарём типов в
 * события/классы движка — компилироваться так не может). Оба механизма 1.7.10 — не "регистрация в
 * реестр", а подписка на диспетчер интерфейсов; их neo-эквивалент — обычные {@code @SubscribeEvent}
 * на этом же классе, который УЖЕ регистрируется на {@code NeoForge.EVENT_BUS} ниже (единый центр
 * подписки, не рассыпаны по местам). Тело {@link #getBurnTime(net.minecraft.world.item.ItemStack)}
 * не тронуто (1:1) — только подключено через {@link #onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent)}.
 * WorldGen-часть: F6 разработан ({@code decisions/F6-worldgen.md}) — старый метод-заглушка {@code generate}
 * (был телом {@code IWorldGenerator.generate}, вызывавшимся через выдуманный
 * {@code DeferredRegister.registerWorldGenerator}) удалён вместе с закомментированным ниже наброском моста
 * на {@code PopulateChunkEvent} (тоже не существующий в neo как таковой) — реальная точка входа теперь
 * {@link gregapi.worldgen.GT6WorldgenFeature#place}, регистрируемая централизованно через
 * {@link gregapi.worldgen.GT6WorldgenFeature#register} (вызывается из {@code GT_API}-конструктора).
 * Сам диспетчер {@link gregapi.worldgen.GT6WorldGenerator#generate(net.minecraft.world.level.Level,int,int,boolean)}
 * не переписан — только точка вызова.
 *
 * F-GUI (шов «GUI/меню», ревизия): конструктор незаконно "implements" {@code IContainerFactory}
 * ({@code net.neoforged.neoforge.network.IContainerFactory<T extends AbstractContainerMenu>} —
 * generic-фабрика ОДНОГО типа контейнера, {@code create(int,Inventory,RegistryFriendlyByteBuf):T}, сигнатура
 * НЕ СОВПАДАЕТ с {@code getServerGuiElement(int,Player,Level,int,int,int):Object}; было заглушкой прежнего
 * флаунда, реально не реализовывало интерфейс) — снят. Прежний 1.7.10 {@code implements IGuiHandler}
 * (Forge network registry, автодиспетчер {@code player.openGui(mod,id,...)}) не существует в neo вообще —
 * маршрут {@code id → getGUIServer} перенесён в ЕДИНЫЙ центр {@link gregapi.gui.GT6MenuProvider} (серверное
 * открытие) + {@link gregapi.gui.ContainerCommon#createFromNetwork} (клиентская реконструкция контейнера);
 * {@code getServerGuiElement} здесь удалён (не дублируем — было ровно этой же строкой {@code WD.te+getGUIServer},
 * теперь она в одном месте). {@link #getClientGuiElement} оставлен как есть (не {@code @Override} — цели
 * нет) — якорь для отдельного клиент-рендер прохода (PORT-TODO(F14, gui-client-screen)), сама GUI-логика
 * не трогается.
 */
public abstract class GT_API_Proxy extends Abstract_Proxy {
	public GT_API_Proxy() {
		NeoForge.EVENT_BUS.register(this);
	}

	/**
	 * F12/R3-мост: заменяет выдуманный {@code DeferredRegister.registerFuelHandler(this)}. Событие
	 * {@link FurnaceFuelBurnTimeEvent} летит на {@code NeoForge.EVENT_BUS} (сверено, javadoc класса
	 * события) — этот же bus уже слушает {@code this} (см. конструктор), поэтому достаточно
	 * {@code @SubscribeEvent}, без отдельной регистрации.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent aEvent) {
		int tBurnTime = getBurnTime(aEvent.getItemStack());
		if (tBurnTime > 0) aEvent.setBurnTime(tBurnTime);
	}
	
	public int addArmor(String aPrefix) {
		return 0;
	}
	
	public Player getThePlayer() {
		return null;
	}
	
	public boolean sendUseItemPacket(Player aPlayer, Level aWorld, ItemStack aStack) {
		return F;
	}
	
	/** F-GUI: клиент-рендер якорь (PORT-TODO(F14, gui-client-screen)) — не {@code @Override}, цели нет
	 *  (см. javadoc класса); серверный близнец {@code getServerGuiElement} удалён — центр в
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
	}
	
	@Override
	public void onProxyAfterServerStopping(Abstract_Mod aMod, ServerStoppingEvent aEvent) {
		checkSaveLocation(null, T);
		MultiTileEntityRegistry.onServerStop();
	}
	
	// DimensionManager (1.7.10 Forge) neo-эквивалента не имеет (не найден ни в neo-decompiled, ни в neoforge-decompiled, ни в fml-decompiled) —
	// реальный neo-путь к текущему save-root: ServerLevel.getServer().getWorldPath(LevelResource.ROOT) (сверено, MinecraftServer.java:2058 + LevelResource.java:16).
	@SubscribeEvent(priority = EventPriority.LOWEST) public void onWorldLoad  (LevelEvent.Load   aEvent) {if (aEvent.getLevel() instanceof ServerLevel tLevel) checkSaveLocation(tLevel.getServer().getWorldPath(LevelResource.ROOT).toFile(), F);}
	//@SubscribeEvent(priority = EventPriority.LOWEST) public void onWorldUnload(WorldEvent.Unload aEvent) {checkSaveLocation(DimensionManager.getCurrentSaveRootDirectory(), F);}
	//@SubscribeEvent(priority = EventPriority.LOWEST) public void onWorldSave  (WorldEvent.Save   aEvent) {checkSaveLocation(DimensionManager.getCurrentSaveRootDirectory(), F);}
	
	public  static final List<ITileEntityServerTickPre    > SERVER_TICK_PRE                = new ArrayListNoNulls<>(), SERVER_TICK_PR2  = new ArrayListNoNulls<>();
	public  static final List<ITileEntityServerTickPost   > SERVER_TICK_POST               = new ArrayListNoNulls<>(), SERVER_TICK_PO2T = new ArrayListNoNulls<>();
	public  static final List<ITileEntityMobSpawnInhibitor> MOB_SPAWN_INHIBITORS           = new ArrayListNoNulls<>();
	public  static       List<IHasWorldAndCoords>           DELAYED_BLOCK_UPDATES          = new ArrayListNoNulls<>();
	private static       List<IHasWorldAndCoords>           DELAYED_BLOCK_UPDATES_2        = new ArrayListNoNulls<>();
	public  static       List<ITileEntityScheduledUpdate>   SCHEDULED_TILEENTITY_UPDATES   = new ArrayListNoNulls<>();
	private static       List<ITileEntityScheduledUpdate>   SCHEDULED_TILEENTITY_UPDATES_2 = new ArrayListNoNulls<>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST) 
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onServerTick(ServerTickEvent aEvent) {
		TOOL_SOUNDS = TOOL_SOUNDS_SETTING;
		
		// Fixing a Thaumcraft Bug in its Loot Bags.
		ST.fixBookStacks();
		
		{ // ServerTickEvent неизменно server-side (сверено, javadoc net.neoforged.neoforge.event.tick.ServerTickEvent) — замена мёртвого aEvent.side.isServer()
			// Try acquiring the Lock within 10 Milliseconds. Otherwise fuck anyone who locks it up for too long, or any other faulty reason MC doesn't work.
			try {TICK_LOCK.tryLock(10, TimeUnit.MILLISECONDS);} catch (Throwable e) {e.printStackTrace(ERR);} finally {if (TICK_LOCK.isHeldByCurrentThread()) TICK_LOCK.unlock();}

			// Making sure it is being free'd up in order to prevent exploits or Garbage Collection mishaps.
			LAST_BROKEN_TILEENTITY.set(null);

			if (aEvent instanceof ServerTickEvent.Pre) { // было aEvent.phase == ServerTickEvent.START — neo раскладывает START/END на Pre/Post (сверено, ServerTickEvent.java)
				SYNC_SECOND = (SERVER_TIME % 20 == 0);

				if (SERVER_TIME++ == 0) {
					// Initial Save Data check
					// DimensionManager неo-эквивалента не имеет (см. onWorldLoad выше) — реальный путь через сам ServerTickEvent.
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
					try {for (net.minecraft.world.item.crafting.Recipe tRecipe : mods.railcraft.api.crafting.RailcraftCraftingManager.rollingMachine.getRecipeList()) if (tRecipe != null) tStacks.add(tRecipe.getRecipeOutput());} catch(Throwable e) {e.printStackTrace(ERR);}
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
					
					// PORT-TODO(EVENTS, ChestGenHooks-loot-unification): net.minecraftforge.common.ChestGenHooks (1.7.10 Forge) не найден
					// ни в neo-decompiled, ни в neoforge-decompiled, ни в fml-decompiled — loot-table-driven Chest-Content-API удалён
					// (современные Loot Table у ванили полностью data-driven, нет рантайм "GenHooks"-реестра). Оба нижних блока
					// (сканирование ST.LOOT_TABLES и рефлексия в IE VillageEngineersHouse.crateContents) требуют отдельного F#-решения
					// по порту loot-table-based OreDict-унификации — временно отключены, не выдумка суррогата.
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

					// PORT-TODO(EVENTS, RecipeManager-smelting-enumeration): 1.7.10 FurnaceRecipes.smelting().getSmeltingList() — плоская
					// Map<ItemStack,ItemStack> — в neo RecipeManager реестрово-типизирован (RecipeType.SMELTING, RecipeHolder<SmeltingRecipe>,
					// getResultItem(RegistryAccess)); прямого 1:1 flat-map API нет (сверено, net.minecraft.world.item.crafting.RecipeManager).
					// Перечисление ванильных smelting-выходов для unification требует отдельного F#-решения (смежно F11 crafting dispatcher).
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
								
								tOutput.set(DataComponents.CUSTOM_NAME, Component.literal("ERROR!")); // было setStackDisplayName (1.7.10) — neo: DataComponents.CUSTOM_NAME (сверено, ItemStack.java:819)
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
						tTileEntity.getWorld().updateNeighborsAt(new BlockPos(tTileEntity.getX(), tTileEntity.getY(), tTileEntity.getZ()), tTileEntity.getBlock(tTileEntity.getCoords()), null);
					} catch(Throwable e) {
						if (tTileEntity instanceof ITileEntityErrorable) ((ITileEntityErrorable)tTileEntity).setError("Delayed Block Update - " + e);
						e.printStackTrace(ERR);
					}
				}
				
				if (SERVER_TIME > 10) {
					for (ITileEntityScheduledUpdate tTileEntity : SCHEDULED_TILEENTITY_UPDATES_2) if (!tTileEntity.isDead()) {
						try {
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
			
			if (aEvent instanceof ServerTickEvent.Post) { // было aEvent.phase == ServerTickEvent.END
				for (int i = 0; i < SERVER_TICK_POST.size(); i++) {
					ITileEntityServerTickPost tTileEntity = SERVER_TICK_POST.get(i);
					if (tTileEntity.isDead()) {
						SERVER_TICK_POST.remove(i--);
						tTileEntity.onUnregisterPost();
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
	
	// Было @SubscribeEvent onLivingUpdate(LivingUpdateEvent) — LivingUpdateEvent (net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent,
	// 1.7.10) в neo не существует (сверено: net.neoforged.neoforge.event.entity.living.LivingEvent.java содержит только LivingJumpEvent/
	// LivingVisibilityEvent). Реальный per-tick хук для любой Entity (в т.ч. LivingEntity) — EntityTickEvent.Post, "fired once per game tick,
	// per entity, after the entity performs work" (сверено, net.neoforged.neoforge.event.tick.EntityTickEvent.java) — вызывается из хвоста
	// Entity#tick() (не только LivingEntity), поэтому добавлена explicit instanceof-проверка (диспетчер стал шире, тело обработчика — 1:1).
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
						// Once damaged, the Minoshroom will not stay bound to its Room! (было detachHome() — 1.7.10 EntityCreature;
						// neo PathfinderMob/Mob-эквивалент снятия домашней привязки — setHomeTo(BlockPos.ZERO, -1) (сверено, Mob.java: homeRadius==-1 ⇒ isWithinHome() всегда true).
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
	
	// Было aEvent.side/aEvent.phase (1.7.10 TickEvent.WorldTickEvent+Phase) — neo LevelTickEvent несёт только getLevel()/hasTime(),
	// а Pre/Post — раздельные подклассы (сверено, net.neoforged.neoforge.event.tick.LevelTickEvent.java); "world.loadedEntityList"/
	// "loadedTileEntityList" (плоские ArrayList) удалены — реальный neo-путь: Level.getEntities().getAll() (Iterable, не индексируемый).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onWorldTick(LevelTickEvent aEvent) {
		TOOL_SOUNDS = TOOL_SOUNDS_SETTING;

		if (aEvent.getLevel() instanceof ServerLevel aServerLevel && aEvent instanceof LevelTickEvent.Post) { // getEntities() без аргументов объявлен на ServerLevel, не Level (сверено, ServerLevel.java:1753)
			ArrayListNoNulls<ExperienceOrb> tOrbs = (XP_ORB_COMBINING && SERVER_TIME % 40 == 31 ? new ArrayListNoNulls<ExperienceOrb>(128) : null);

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
							((ItemEntity)aEntity).setItem(NI);
							((ItemEntity)aEntity).discard();
						} else if (!ST.equal(rStack, aStack) || rStack.getCount() != aStack.getCount()) {
							((ItemEntity)aEntity).setItem(rStack);
							UT.Reflection.setField(ItemEntity.class, aEntity, "pickupDelay", 40, F); // было delayBeforeCanPickup (1.7.10) — neo-имя поля: pickupDelay, приватное (сверено, ItemEntity.java:49)
						}

						if (!aEntity.isRemoved() && aEntity.isOnFire() && (tBreak || (tFireProof && !MD.MC.owns(rStack)))) {
							UT.Reflection.setField(ItemEntity.class, aEntity, "health", 250, F);
							// PORT-TODO(EVENTS, ItemEntity.field_70291_e): 1.7.10 SRG-имя без neo-соответствия (второе приватное поле рядом с "health"
							// в 1.7.10 ItemEntity больше не существует, сверено ItemEntity.java) — второй reflection-хак опущен.
							aEntity.extinguishFire();
						}
					}
				} else if (aEntity instanceof LivingEntity) {
					if (ENTITY_CRAMMING > 0 && SERVER_TIME % 50 == 0 && !(aEntity instanceof Player) && ((LivingEntity)aEntity).isPushable() && ((LivingEntity)aEntity).getHealth() > 0) { // было canBePushed() (1.7.10) — neo: isPushable() (сверено, LivingEntity.java:3391)
						List<Entity> tList = aEntity.level().getEntities(aEntity, aEntity.getBoundingBox().inflate(0.2, 0.0, 0.2));
						Class<? extends Entity> tClass = aEntity.getClass();
						int aEntityCount = 1;
						if (tList != null) for (int j = 0; j < tList.size(); j++) if (tList.get(j) != null && tList.get(j).getClass() == tClass) aEntityCount++;
						if (aEntityCount > ENTITY_CRAMMING) aEntity.hurt(aEntity.level().damageSources().inWall(), (aEntityCount - ENTITY_CRAMMING) * TFC_DAMAGE_MULTIPLIER);
					}
				}
			}

			if (tOrbs != null && tOrbs.size() > 32) for (ExperienceOrb aOrb : tOrbs) {
				if (aOrb.getValue() >= Short.MAX_VALUE) continue;
				if (aOrb.getValue() <= 0) {aOrb.setValue(0); aOrb.discard(); continue;}
				for (ExperienceOrb tOrb : tOrbs) if (aOrb != tOrb && !tOrb.isRemoved() && tOrb.getValue() > 0 && tOrb.getValue() < Short.MAX_VALUE && aOrb.distanceToSqr(tOrb) <= 3) {
					// PORT-TODO(EVENTS, ExperienceOrb.age-merge): neo ExperienceOrb.age приватно, без public-сеттера (сверено, ExperienceOrb.java) —
					// перенос возраста при слиянии орбов (1.7.10 xpOrbAge, public) прямого 1:1 не имеет, шаг пропущен.
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
				// PORT-TODO(EVENTS, world-tileentity-flat-list): 1.7.10 World.loadedTileEntityList (плоский ArrayList) удалён — neo хранит
				// BlockEntity per-chunk (LevelChunk.getBlockEntities()), нет прямого world-wide 1:1 (сверено, LevelChunk.java/ServerLevel.java).
				for (BlockEntity aTileEntity : java.util.Collections.<BlockEntity>emptyList()) {
					if (aTileEntity instanceof ITileEntityNeedsSaving) WD.mark(aTileEntity);
				}
			}
		}
	}
	
	// Было cpw.mods.fml.common.gameevent.PlayerEvent.ItemPickupEvent (1.7.10) — не существует в neo. Реальный neo-эквивалент
	// "игрок успешно подобрал предмет" — ItemEntityPickupEvent.Post (сверено, net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerItemPickupEvent(ItemEntityPickupEvent.Post aEvent) {
		ST.check(aEvent.getPlayer(), aEvent.getItemEntity().getItem());
	}

	private int BEAR_INVENTORY_COOL_DOWN = 5;

	// Было aEvent.phase == Phase.END (1.7.10 TickEvent) — neo PlayerTickEvent раскладывает Pre/Post на подклассы, Post уже "после тика" (сверено, PlayerTickEvent.java).
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
			
			for (Object tPotion : aPlayer.getActiveEffects()) { // было getActivePotionEffects() (1.7.10) — neo: getActiveEffects() (сверено, LivingEntity.java:994)
				if (tPotion instanceof MobEffectInstance && ((MobEffectInstance)tPotion).getDuration() <= 0) {
					aPlayer.removeEffect(((MobEffectInstance)tPotion).getEffect()); // было removePotionEffect(int)/getPotionID() — neo: removeEffect(Holder<MobEffect>)/getEffect() (сверено, LivingEntity.java:1079 + MobEffectInstance.java:192)
					break;
				}
			}
			
			if (!aPlayer.level().isClientSide()) { // было aEvent.side.isServer() — PlayerTickEvent.Post летит на обеих сторонах (сверено, javadoc PlayerTickEvent.java)
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
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.ticksExisted + "     DEAD");
							tIterator.remove();
						} else if (tEntity.isNoDespawnRequired()) {
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.ticksExisted + "     PERSISTENT");
							tIterator.remove();
						} else if (tEntity.ticksExisted != tEntity.getAge()) {
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.ticksExisted + "     GOT CLOSE TO PLAYER");
							tIterator.remove();
						} else {
							DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.ticksExisted);
						}
					}
					DEB.println("====01====");
					DEB.println("List Changed: " + mMobsToFastDespawn.removeAll(aPlayer.worldObj.getEntities(aPlayer, AxisAlignedBB.getBoundingBox(aPlayer.posX-32, aPlayer.posY-32, aPlayer.posZ-32, aPlayer.posX+32, aPlayer.posY+32, aPlayer.posZ+32))));
					DEB.println("====02====");
					tIterator = mMobsToFastDespawn.iterator();
					while (tIterator.hasNext()) {
						EntityLiving tEntity = tIterator.next();
						DEB.println(tEntity.getClass() + "     " + tEntity.getAge() + "     " + tEntity.ticksExisted);
					}
					DEB.println("==========");
					DEB.println("TEST END");
					DEB.println("==========");
				}
				*/
				if (SURVIVAL_INTO_ADVENTURE_MODE && aPlayer.tickCount%200==0 && aPlayer.getAbilities().mayBuild && !UT.Entities.isCreative(aPlayer) && aPlayer instanceof ServerPlayer aServerPlayer) {
					aServerPlayer.setGameMode(GameType.ADVENTURE); // было setGameType(WorldSettings.GameType...) — neo: ServerPlayer.setGameMode(GameType) (сверено, ServerPlayer.java:1849)
					aPlayer.getAbilities().mayBuild = F;
					if (ADVENTURE_MODE_KIT) {
						if (MD.GT.mLoaded) {
							UT.Entities.sendchat(aPlayer, CHAT_GREG + "Thank you for choosing the GregTech-6 Adventure Mode Starter Kit.");
							
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
							UT.Entities.sendchat(aPlayer, CHAT_GREG + "It's dangerous to go alone! Take this.");
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
								}
								// PORT-TODO(EVENTS, IFluidHandlerItem-capability): capability-система fluid-in-item полностью переработана в neo
								// (IFluidHandlerItem — @Deprecated(forRemoval), заменена ResourceHandler/ItemAccess; getFluid(ItemStack)/drain(..,bool)/
								// fill(..,bool) более не существуют — реальные IFluidHandler.drain/fill принимают FluidAction, не boolean, сверено
								// net.neoforged.neoforge.fluids.capability.IFluidHandler.java) — требует отдельного F#-решения, блок отключён.
								// else if (tStack.getItem() instanceof IFluidHandlerItem) {
								// 	FluidStack tFluid = ((IFluidHandlerItem)tStack.getItem()).getFluid(tStack);
								// 	if (tFluid != null && !FL.Potion_Tainted.is(tFluid) && FluidsGT.POTION.contains(FL.regName(tFluid.getFluid()))) {
								// 		((IFluidHandlerItem)tStack.getItem()).drain(tStack, Integer.MAX_VALUE, T);
								// 		((IFluidHandlerItem)tStack.getItem()).fill(tStack, FL.Potion_Tainted.make(tFluid.getAmount()), T);
								// 	}
								// }
								ItemStack tRotten = RottingUtil.rotting(tStack, aPlayer.level(), UT.Code.roundDown(aPlayer.getX()), UT.Code.roundDown(aPlayer.getY()), UT.Code.roundDown(aPlayer.getZ()));
								if (ST.invalid(tRotten)) {tStack.setCount(0); aPlayer.getInventory().setItem(i, NI); continue;}
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
									// PORT-TODO(EVENTS, MobEffect-legacy-static-ids): 1.7.10 Potion-константы (MobEffect.moveSlowdown и т.п.) как
									// static-поля с числовым .id удалены — современный MobEffect реестрово-Holder-based, без 1:1 (сверено,
									// net.minecraft.world.effect.MobEffect.java + Mob-эффекты теперь через MobEffects.* Holder). Требует
									// отдельного F#-решения по переносу PotionsGT/числовых id (смежно UT.Entities.applyPotion(int)).
									// MobEffectInstance tEffect = null;
									// UT.Entities.applyPotion(aPlayer, MobEffect.moveSlowdown.id, Math.max(140, ((tEffect = aPlayer.getActivePotionEffect(MobEffect.moveSlowdown))==null?0:tEffect.getDuration())), 3, F);
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
							// PORT-TODO(EVENTS, MobEffect-legacy-static-ids): MobEffect.poison (см. отметку выше по moveSlowdown) — без 1:1.
							// UT.Entities.applyPotion(aPlayer, MobEffect.poison, 1200, tCraponite, T);
						}
						if (--BEAR_INVENTORY_COOL_DOWN < 0 && tEmptySlots < 4 && aPlayer.level() instanceof ServerLevel aServerLevel) {
							BEAR_INVENTORY_COOL_DOWN = 100;
							UT.Sounds.send(SFX.MC_HMM, aPlayer);
							for (int i = 0; i < aServerLevel.players().size(); i++) { // было level().playerEntities (1.7.10) — neo: ServerLevel.players() (сверено, ServerLevel.java:1530)
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
									// ENCHANT: Enchantment_WerewolfDamage.INSTANCE (1.7.10 Java-объект чара) заменён на
									// ResourceKey<Enchantment> KEY (см. gregapi/enchants/Enchantment_WerewolfDamage.java) —
									// resolve через живой RegistryAccess сервера (тот же приём, что SILK_TOUCH/FORTUNE выше в этом файле).
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
					
					// было inventory.armorInventory[0..3] (1.7.10, boots/leggings/chest/helmet) — броня в neo больше не хранится в Inventory
					// (36-слотовый массив), а в EntityEquipment; порядок FEET/LEGS/CHEST/HEAD соответствует старому 0..3 (сверено, EquipmentSlot.java).
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
	
	// ChunkWatchEvent.Watch.player/.chunk (1.7.10) — приватные поля в neo (сверено, net.neoforged.neoforge.event.level.ChunkWatchEvent.java) —
	// getPlayer()/getChunk(); getChunk() отдаёт LevelChunk напрямую, повторный getChunkFromChunkCoords(...) по x/z больше не нужен.
	// tChunk.isTerrainPopulated (1.7.10 генерация-флаг) в neo не существует — просматриваемые чанки всегда FULL-статуса, PORT-TODO(EVENTS,
	// LevelChunk.isTerrainPopulated) — проверка опущена, chunkTileEntityMap → getBlockEntities().
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onChunkWatchEvent(ChunkWatchEvent.Watch aEvent) {
		LevelChunk tChunk = aEvent.getChunk();
		if (tChunk != null && tChunk.getBlockEntities() != null && tChunk.getBlockEntities().size() > 0) {
			byte tIterations = 8;
			HashSetNoNulls<Object> tSet = new HashSetNoNulls<>();
			while (tIterations-->0) try {
				for (Object tTileEntity : tChunk.getBlockEntities().values()) if (tTileEntity instanceof ITileEntitySynchronising) if (tSet.add(tTileEntity)) ((ITileEntitySynchronising)tTileEntity).sendUpdateToPlayer(aEvent.getPlayer());
				tIterations = 0;
			} catch(ConcurrentModificationException e) {
				if (tIterations <= 0) ERR.println("Failed to Iterate 8 times. Giving up on sending Data to Client!");
			} catch(Throwable e) {
				// FastUtils throws a NullPointer instead of a CME...
				if (tIterations <= 0) e.printStackTrace(ERR);
			}
		}
	}
	
	// PlayerDestroyItemEvent.original/.entityPlayer (1.7.10) — приватные поля в neo, getOriginal()/getEntity() (сверено,
	// net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent.java). ItemSword/ItemTool (1.7.10 классы) в neo не существуют
	// (нет ни SwordItem/PickaxeItem/DiggerItem под net.minecraft.world.item — сверено) — реальный аналог: ItemTags.SWORDS/AXES/PICKAXES/
	// SHOVELS/HOES теговые проверки на ItemStack. inventory.mainInventory (плоский изменяемый массив) удалён — Inventory.getItem(i)/setItem(i,x).
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
				// PORT-TODO(EVENTS, MobEffect-legacy-static-ids): MobEffect.weakness/.digSlowdown (см. отметку выше по moveSlowdown) — без 1:1.
				// UT.Entities.applyPotion(aPlayer, MobEffect.weakness   ,  300, 2, F);
				// UT.Entities.applyPotion(aPlayer, MobEffect.digSlowdown, 1200, 2, F);
			}
		}
		//
		net.minecraft.world.entity.player.Inventory tInv = aPlayer.getInventory();
		// Only work on Vanilla-Sized Player Inventories!
		if (tInv.getContainerSize() != 36) return;
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
			tInv.setItem(tSlot, tInv.getItem(tSlot+ 9)); tInv.setItem(tSlot+ 9, NI); ST.update(aPlayer); return;}
			tInv.setItem(tSlot, tInv.getItem(tSlot+18)); tInv.setItem(tSlot+18, NI); ST.update(aPlayer); return;}
			tInv.setItem(tSlot, tInv.getItem(tSlot+27)); tInv.setItem(tSlot+27, NI); ST.update(aPlayer); return;}
			return;
		}
		// Move into Second Row. Usually only with the Double Hotbars Mod.
		if (tSlot < 18) {
			if (ST.equal(aOriginal, tInv.getItem(tSlot+18), T)) {
			if (ST.equal(aOriginal, tInv.getItem(tSlot+ 9), T)) {
			tInv.setItem(tSlot, tInv.getItem(tSlot+ 9)); tInv.setItem(tSlot+ 9, NI); ST.update(aPlayer); return;}
			tInv.setItem(tSlot, tInv.getItem(tSlot+18)); tInv.setItem(tSlot+18, NI); ST.update(aPlayer); return;}
			return;
		}
		// Move into Third Row. Unsure if a Triple Hotbar Mod exists, but if it does, well then it is supported.
		if (ST.equal(aOriginal, tInv.getItem(tSlot+ 9), T)) {
		tInv.setItem(tSlot, tInv.getItem(tSlot+ 9)); tInv.setItem(tSlot+ 9, NI); ST.update(aPlayer); return;}
		return;
	}
	
	// Было cpw.mods-нет, а PlayerUseItemEvent.Finish (1.7.10) — не существует в neo (нет пакета "PlayerUseItemEvent"). Реальный
	// neo-эквивалент — LivingEntityUseItemEvent.Finish (сверено, net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.java) —
	// летит для ЛЮБОГО LivingEntity (не только Player), добавлена explicit instanceof-проверка (диспетчер стал шире).
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
		if (tNBT != null && tNBT.contains(NBT_EFFECTS)) { // было hasKey/getCompoundTag/getInteger (1.7.10) — neo CompoundTag: contains/getCompoundOrEmpty/getInt(Optional<Integer>) (сверено, CompoundTag.java)
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
	
	// Было PlayerInteractEvent+Action-enum (1.7.10, единый класс с полями x/y/z/face/entityPlayer/world) — в neo PlayerInteractEvent абстрактен,
	// действия расфасованы по подклассам RightClickBlock/RightClickItem/RightClickEmpty/LeftClickBlock/LeftClickEmpty/EntityInteract(Specific)
	// (сверено, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.java); подписка на абстрактный базовый класс по-прежнему ловит
	// все подклассы (ListenerList проверен, bus рекурсивно поднимается по parent — fml-decompiled/net/neoforged/bus/ListenerList.java) —
	// внутри тело диспетчеризуется через instanceof вместо aEvent.action, как раньше. x/y/z → getPos(), face → getFace() (Direction), world → getLevel().
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

		if (aEvent instanceof PlayerInteractEvent.RightClickBlock aRightClickBlock) { // связывание, т.к. setCanceled объявлен только на конкретных ICancellableEvent-подклассах, не на абстрактном PlayerInteractEvent (сверено, PlayerInteractEvent.java)
			// Fixing a Vanilla Dupe Bug with stacked Music Discs and the Jukebox.
			if (aTileEntity instanceof JukeboxBlockEntity) {
				ItemStack tStack = ((JukeboxBlockEntity)aTileEntity).getTheItem(); // было func_145856_a() (1.7.10 SRG) — neo: getTheItem() (сверено, JukeboxBlockEntity.java)
				if (tStack != null) tStack.setCount(1);
				return;
			}
			// You can easily recycle most things in GT6 anyways, so this should not be needed.
			if (IL.TF_Uncrafting.equal(aBlock)) {
				UT.Entities.chat(aPlayer, CHAT_GREG + "No cheating! ;)");
				aRightClickBlock.setCanceled(T);
				return;
			}
			// Just rightclick the Trophy to get the Achievement/Progress.
			if (IL.TF_Trophy.equal(aBlock)) {
				// PORT-TODO(EVENTS, Block.getItemDropped/getDamageValue): 1.7.10 metadata-based Block-API (getItemDropped(meta,Random,fortune),
				// getDamageValue(World,x,y,z)) удалено в пользу LootTable+BlockState (сверено, net.minecraft.world.level.block.Block.java —
				// методов нет); ST.check сам уже форс-no-op (см. gregapi.util.ST.check, PORT-TODO stats-loot) — вызов отключён.
				// ST.check(aPlayer, ST.make(aBlock.getItemDropped(0, RNGSUS, 0), 1, aBlock.getDamageValue(aWorld, aX, aY, aZ)));
				return;
			}
			// Some Clientside Only Stuff.
			if (aPlayer.level().isClientSide() && !aPlayer.isShiftKeyDown()) {
				if (aTileEntity instanceof PrefixBlockTileEntity) {
					// Show uses for Bedrock Ore when clicking it.
					if (aBlock == BlocksGT.oreBedrock || aBlock == BlocksGT.oreSmallBedrock) {
						RM.BedrockOreList.openNEI();
					//  RM.BedrockOreList.guiUsesNEI(ST.make((Block)BlocksGT.oreBedrock, 1, ((PrefixBlockTileEntity)aTileEntity).mMetaData));
					}
				}
			}
			if (ST.valid(aStack)) {
				// Preventing a Railcraft Crash with Fluid Container Items.
				if (aStack.getItem() instanceof IFluidHandlerItem && !aPlayer.isShiftKeyDown() && aTileEntity != null && aTileEntity.getClass().getName().startsWith("mods.railcraft.common")) {
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
							UT.Entities.chat(aPlayer, CHAT_GREG + "The Dolly Code is sadly not smart enough to move this TileEntity.", CHAT_GREG + "It would crash if it actually did, so be glad I prevented your mistake.", CHAT_GREG + "Would be great if it did work though...");
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
							aStack.hurtAndBreak((int)UT.Code.units(tDamage, 10000, 1, T), aPlayer, InteractionHand.MAIN_HAND); // было damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (сверено, ItemStack.java:524)
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
							aStack.hurtAndBreak((int)UT.Code.units(tDamage, 10000, 1, T), aPlayer, InteractionHand.MAIN_HAND); // было damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (сверено, ItemStack.java:524)
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
							aStack.hurtAndBreak((int)UT.Code.units(tDamage, 10000, 1, T), aPlayer, InteractionHand.MAIN_HAND); // было damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (сверено, ItemStack.java:524)
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
						// PORT-TODO(EVENTS, BaseSpawner-string-entity-api): 1.7.10 BaseSpawner.getEntityNameToSpawn()/setEntityName(String)
						// (плоский String-идентификатор) удалены — neo BaseSpawner работает через EntityType<?> (setEntityId(EntityType,
						// Level,RandomSource,BlockPos), сверено BaseSpawner.java:55) — нет прямого String↔EntityType 1:1 без реестрового моста;
						// TRANSFORMATION_POWDER_SPAWNER_MAP (String→String) требует отдельного решения по переносу на EntityType-ключи.
						if (F && aTileEntity instanceof SpawnerBlockEntity) {
							if (aWorld.isClientSide()) return;
							BaseSpawner tSpawner = ((SpawnerBlockEntity)aTileEntity).getSpawner(); // было func_145881_a() (1.7.10 SRG) — neo: getSpawner() (сверено, SpawnerBlockEntity.java:93)
							if (ST.use(aPlayer, aStack, 16)) {
								// I hope this works sync the new Mob Data over.
								aWorld.sendBlockUpdated(aEvent.getPos(), aWorld.getBlockState(aEvent.getPos()), aWorld.getBlockState(aEvent.getPos()), 3); // было markBlockForUpdate(x,y,z) (1.7.10) — neo: Level.sendBlockUpdated(pos,old,new,flags) (сверено, Level.java:333)
							} else {
								UT.Entities.sendchat(aPlayer, "You need 16 Bags of Transformation Powder to convert this!");
							}
							aRightClickBlock.setCanceled(T);
							return;
						}
					}
				}
			}
		}

		if (aEvent instanceof PlayerInteractEvent.RightClickBlock || aEvent instanceof PlayerInteractEvent.RightClickItem) { // было aEvent.action==RIGHT_CLICK_BLOCK||RIGHT_CLICK_AIR
			if (ST.valid(aStack)) {
				// Make sure that shelvable Items don't do a Rightclick Action instead of being shelved.
				if (aEvent instanceof PlayerInteractEvent.RightClickBlock aRightClickBlock && aTileEntity instanceof ITileEntityBookShelf && ((ITileEntityBookShelf)aTileEntity).isShelfFace(aFace)) {
					aRightClickBlock.setUseBlock(TriState.TRUE);  // было aEvent.useBlock = Result.ALLOW
					if (BooksGT.BOOK_REGISTER.containsKey(aStack, T)) aRightClickBlock.setUseItem(TriState.FALSE); // было aEvent.useItem = Result.DENY
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
										((ICancellableEvent)aEvent).setCanceled(T); // RightClickBlock и RightClickItem оба реализуют ICancellableEvent (сверено, PlayerInteractEvent.java)
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
	
	// UseHoeEvent (1.7.10, world/x/y/z/entityPlayer поля) не существует в neo — заменён общим BlockToolModificationEvent (ЛЮБАЯ
	// ItemAbility, не только мотыга — сверено, net.neoforged.neoforge.event.level.BlockEvent.java), поэтому добавлена явная проверка
	// getItemAbility()==ItemAbilities.HOE_TILL. blockMetadata (1.7.10 dirt-вариант: 1=coarse dirt, 2=podzol) удалён — в neo это отдельные
	// Block-типы (Blocks.COARSE_DIRT/Blocks.PODZOL), не метадата Blocks.DIRT (сверено, аналогично общей проблеме Blocks.* по файлу).
	// PORT-TODO(EVENTS, dirt-metadata-variant): нет 1:1 переноса проверки "dirt с ненулевой metadata" без переизобретения списка блоков.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onUseHoeEvent(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent aEvent) {
		// if (aEvent.getItemAbility() == net.neoforged.neoforge.common.ItemAbilities.HOE_TILL && aEvent.getState().getBlock() == Blocks.DIRT /* metadata!=0 PORT-TODO выше */) aEvent.setCanceled(T);
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST) 
	@SuppressWarnings("unlikely-arg-type")
	// PlayerEvent.BreakSpeed (1.7.10: block/x/y/z/metadata/newSpeed поля) в neo несёт только getState()/getOriginalSpeed()/
	// getNewSpeed()+setNewSpeed()/getPosition() (Optional<BlockPos>) (сверено, PlayerEvent.java) — есть настоящий public setNewSpeed(),
	// поэтому reflection-хак "Aether does something stupid" (обход недоступности поля) более не нужен — используется штатный сеттер.
	// Метадата у BlockState нет — используется WD.meta(Level,x,y,z) по позиции события (тот же приём, что и везде в файле).
	public void onBlockBreakSpeedEvent(PlayerEvent.BreakSpeed aEvent) {
		if (aEvent.getNewSpeed() > 0) {
			Player aPlayer = aEvent.getEntity();
			BlockPos tPos = aEvent.getPosition().orElse(BlockPos.ZERO);
			int aX = tPos.getX(), aY = tPos.getY(), aZ = tPos.getZ();
			Block aBlock2 = aEvent.getState().getBlock();
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
				// PORT-TODO(EVENTS, Block.getMaterial): vanilla WD.getMaterial(Block) (1.7.10) удалён — в neo классификация материала не
				// выражена простым Material-объектом на Block (сверено, net.minecraft.world.level.block.Block.java — метода нет); gregapi.block.Material
				// (свой класс, тот же импорт) не содержит обратного моста Block→Material. Различение SAND/ROCK-множителя отложено — консервативно ROCK.
				// if (WD.getMaterial(aBlock2) == Material.sand || WD.getMaterial(aBlock2) == Material.clay || WD.getMaterial(aBlock2) == Material.grass || WD.getMaterial(aBlock2) == Material.ground) {
				// 	aEvent.setNewSpeed((float)(aEvent.getNewSpeed() / HARDNESS_MULTIPLIER_SAND));
				// 	return;
				// }
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

	// BlockEvent.BreakEvent (1.7.10) не существует в neo (сверено, net.neoforged.neoforge.event.level.BlockEvent.java — нет вложенного
	// BreakEvent) — расщеплён на BreakBlockEvent (level.block, только cancel-семантика, БЕЗ setExpToDrop) и BlockDropsEvent
	// (level, несёт getDroppedExperience()/setDroppedExperience(int) — прямой neo-эквивалент старого setExpToDrop). EnchantmentHelper.
	// getSilkTouchModifier(Player) (1.7.10) удалён — реальный neo: EnchantmentHelper.getItemEnchantmentLevel(Holder<Enchantment>,LivingEntity)
	// по Holder силы прикосновения из RegistryAccess (сверено, EnchantmentHelper.java:292 + Enchantments.SILK_TOUCH).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onBlockBreakingEvent(BlockDropsEvent aEvent) {
		if (aEvent.getState().getBlock() instanceof IPrefixBlock) {
			Holder<Enchantment> tSilkTouch = aEvent.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
			if (aEvent.getBreaker() instanceof LivingEntity aBreaker && EnchantmentHelper.getEnchantmentLevel(tSilkTouch, aBreaker) > 0) aEvent.setDroppedExperience(0);
		}
	}

	// BlockEvent.HarvestDropsEvent (1.7.10: List<ItemStack> drops, block/blockMetadata/world/x/y/z/harvester/isSilkTouching/fortuneLevel)
	// не существует в neo — заменён BlockDropsEvent (сверено, net.neoforged.neoforge.event.level.BlockDropsEvent.java), несущим
	// List<ItemEntity> (не ItemStack) и без отдельных isSilkTouching/fortuneLevel/blockMetadata полей. gregapi.item.multiitem.MultiItemTool
	// (не мой файл) УЖЕ портирован на этот случай — onHarvestBlockEvent(ArrayList<ItemStack>,...,Player,...,BlockDropsEvent) и
	// canCollectDropsDirectly(ItemStack,Block,byte) сохраняют старую форму именно как ArrayList<ItemStack> (сверено, MultiItemTool.java) —
	// поэтому здесь строится локальный ArrayList<ItemStack>-мост поверх ItemEntity-дропов, вся 1.7.10-логика выполняется на нём 1:1,
	// затем список ItemEntity синхронизируется обратно. isSilkTouching/fortuneLevel считаются через EnchantmentHelper по инструменту.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onBlockHarvestingEvent(BlockDropsEvent aEvent) {
		ArrayListNoNulls<ItemStack> aDropStacks = new ArrayListNoNulls<>();
		for (ItemEntity tDropEntity : aEvent.getDrops()) aDropStacks.add(tDropEntity.getItem());

		Level aWorld = aEvent.getLevel();
		int aX = aEvent.getPos().getX(), aY = aEvent.getPos().getY(), aZ = aEvent.getPos().getZ();
		byte aBlockMeta = WD.meta(aWorld, aX, aY, aZ);
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

		if (aBlock == Blocks.DIRT && aBlockMeta == 1) for (int i = 0, j = aDropStacks.size(); i < j; i++) if (ST.block(aDropStacks.get(0)) == Blocks.DIRT) {
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

				// PORT-TODO(EVENTS, ST.entity-missing + EntityItemPickupEvent): gregapi.util.ST (не мой файл) не содержит метода
				// ST.entity(Entity,ItemStack) — уже отсутствует независимо от event-порта (грепнуто, gregapi/util/ST.java). Старый
				// cpw.mods.fml.common.gameevent.EntityItemPickupEvent синтетический трюк ("симулировать подбор, спросить другие моды")
				// в neo заменяется ItemEntityPickupEvent.Pre(Player,ItemEntity)+TriState canPickup() (сверено, ItemEntityPickupEvent.java),
				// но без ST.entity() как источника временного ItemEntity перенос недоделываем — требует отдельного решения по ST.java.
				if (F && tCanCollect && !aDropStacks.isEmpty()) {
					boolean aCollectSound = T;
					aDrops = aDropStacks.iterator();
					while (aDrops.hasNext()) {
						ItemStack aDrop = aDrops.next();
						if (ST.valid(aDrop)) {
							aDrop = ST.update(aDrop, aWorld, aX, aY, aZ);
							if (ST.add(aHarvester, aDrop)) {
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
			ST.denull(aHarvester);
		}

		aEvent.getDrops().clear();
		for (ItemStack tStack : aDropStacks) if (ST.valid(tStack)) aEvent.getDrops().add(new ItemEntity(aWorld, aX+0.5, aY+0.5, aZ+0.5, tStack));
	}
	
	// EntityJoinLevelEvent.entity (1.7.10 через словарь-переименование) — приватное поле в neo, getEntity() (сверено, EntityEvent.java).
	// ItemEntity.getEntityItem/setEntityItemStack/isDead/setDead (1.7.10) — neo: getItem/setItem/isRemoved/discard (сверено, ItemEntity.java/Entity.java).
	// World.isClientSide() → Level.isClientSide(). World.findNearestEntityWithinAABB(Class,AABB,Entity) удалён — реальный neo-путь:
	// Level.getEntities(Entity,AABB,Predicate) + isInstance-проверка по классу (сверено, EntityGetter.java).
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
				aEvent.getEntity().discard();
				return;
			}
		}
	}
	
	public static List<ServerPlayer> mNewPlayers = new ArrayListNoNulls<>();
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingDeath(LivingDeathEvent aEvent) {
		if (aEvent.getEntity() instanceof ServerPlayer) NW_API.sendToPlayer(new PacketDeathPoint(UT.Code.roundDown(aEvent.getEntity().getX()), UT.Code.roundDown(aEvent.getEntity().getY()), UT.Code.roundDown(aEvent.getEntity().getZ())), (ServerPlayer)aEvent.getEntity());
	}

	// Было cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent (1.7.10) — не существует в neo. Реальный neo-эквивалент —
	// net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent (сверено, PlayerEvent.java).
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
	
	// F6: старый IWorldGenerator.generate(Random,int,int,Level,IChunkProvider,IChunkProvider) и
	// закомментированный набросок моста на PopulateChunkEvent удалены — оба не имеют смысла в neo
	// (IWorldGenerator и PopulateChunkEvent не существуют; IChunkProvider — 1.7.10-only тип). Реальная
	// точка входа: gregapi.worldgen.GT6WorldgenFeature.place(FeaturePlaceContext) — кастомная Feature,
	// зарегистрированная через PlacedFeature+BiomeModifier (см. javadoc класса выше и
	// decisions/F6-worldgen.md). Диспетчер GT6WorldGenerator.generate(Level,int,int,boolean) не тронут.

	// ItemExpireEvent.entity/.entityItem/.extraLife (1.7.10) — приватные поля в neo: getEntity()/getExtraLife()+setExtraLife(int)/
	// addExtraLife(int) (сверено, net.neoforged.neoforge.event.entity.item.ItemExpireEvent.java) — отдельного getItemEntity() НЕТ,
	// ItemEvent.getEntity() ковариантно переопределён и УЖЕ возвращает ItemEntity (сверено, ItemEvent.java). Событие БОЛЬШЕ
	// НЕ cancellable (нет ICancellableEvent) — движок сам решает продлевать ли жизнь по итоговому getExtraLife() и делает discard()
	// если возраст всё ещё >= lifespan (сверено, EventHooks.onItemExpire + ItemEntity.java:186-191) — все aEvent.setCanceled(T) сняты,
	// а прямой discard() ItemEntity оставлен там, где он и раньше принудительно убивал стек НЕМЕДЛЕННО (до естественного пути движка).
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
	
	// LivingSpawnEvent.CheckSpawn (1.7.10) не существует в neo — ближайший реальный аналог "проверка позиции спавна ПОСЛЕ создания моба" —
	// MobSpawnEvent.PositionCheck, с собственным вложенным Result{SUCCEED,DEFAULT,FAIL} (НЕ общий bus Result — сверено,
	// net.neoforged.neoforge.event.entity.living.MobSpawnEvent.java); DENY→FAIL. .entityLiving/.world/.x/.y/.z — getEntity()(Mob)/
	// getEntity().level()/getX()/getY()/getZ() (double, через сущность — getLevel() отдаёт лишь ServerLevelAccessor, без Level-API).
	// WD.dimensionId(World)==0 (1.7.10) → Level.dimension()==Level.OVERWORLD (сверено, Level.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onCheckSpawnEvent(MobSpawnEvent.PositionCheck aEvent) {
		if (aEvent.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL) return;
		Class<? extends LivingEntity> aMobClass = aEvent.getEntity().getClass();
		Level aWorld = aEvent.getEntity().level();
		int aX = UT.Code.roundDown(aEvent.getX()), aY = (int)UT.Code.bind(0, aWorld.getHeight(), UT.Code.roundDown(aEvent.getY())), aZ = UT.Code.roundDown(aEvent.getZ());

		if (SPAWN_NO_BATS && aMobClass == Bat.class && WD.block(aWorld, aX, aY-2, aZ) != Blocks.STONE && WD.block(aWorld, aX, aY+2, aZ) != Blocks.STONE) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}

		if (SPAWN_HOSTILES_ONLY_IN_DARKNESS && WD.dimOverworldLike(aWorld)) try {
			// F-light: 1.7.10 Chunk.getBlockStorageArray()[section].getExtBlocklightValue(...) (per-section блок-свет)
			// удалён — свет в neo через LevelLightEngine; блок-свет в точке = getBrightness(LightLayer.BLOCK,pos)
			// (LevelReader.java:174 использует тот же getBrightness).
			if (aWorld.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, new BlockPos(aX, aY, aZ)) > 0) {
				// Vanilla Mobs only, just in case.
				if (aMobClass == Creeper.class || aMobClass == EnderMan.class || aMobClass == Skeleton.class || aMobClass == Zombie.class || aMobClass == Spider.class || aMobClass == Witch.class || aMobClass == Bat.class) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
				// Well, that Zombie is kindof like Vanilla, so it counts.
				if (MD.TC.mLoaded) if (aEvent.getEntity() instanceof EntityBrainyZombie) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
				// TODO Add Drowned and other Et Futurum Requiem Mobs once they are released.
				// PORT-TODO(EVENTS, EtFu-mob-hierarchy): ganymedes01.etfuturum.entities.{EntityHusk,EntityStray,EntityZombieVillager}
				// (1.7.10-era библиотека, не портирована на neo) не наследуются от современного net.minecraft.world.entity.Mob —
				// instanceof неконвертируемы (hard compile error), не просто раннтайм-false; требует апдейта самой EtFu-библиотеки.
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
			// PORT-TODO(EVENTS, World.getWorldInfo-spawn-pos): 1.7.10 World.getWorldInfo().getSpawnX()/getSpawnZ() удалены — в neo мировая
			// точка спавна не выражена простым x/z на LevelData/ServerLevelData/WritableLevelData (сверено — методов нет ни в одном из трёх);
			// требует отдельного исследования (возможно ServerLevel вне зоны видимости getLevel()) — проверка отключена.
			// if (SPAWN_ZONE_MOB_PROTECTION && UT.Code.inside(-144, 144, aX-aWorld.getWorldInfo().getSpawnX()) && UT.Code.inside(-144, 144, aZ-aWorld.getWorldInfo().getSpawnZ()) && WD.opq(aWorld, aX, 0, aZ, F, F)) {aEvent.setResult(MobSpawnEvent.PositionCheck.Result.FAIL); return;}
		}

		//if (aEvent.entity instanceof EntityMob && !(aEvent.entity instanceof IBossDisplayData) && ((EntityMob)aEvent.entity).getCanSpawnHere()) mMobsToFastDespawn.add((EntityLiving)aEvent.entityLiving);

		for (int i = 0; i < MOB_SPAWN_INHIBITORS.size(); i++) {
			ITileEntityMobSpawnInhibitor tTileEntity = MOB_SPAWN_INHIBITORS.get(i);
			if (tTileEntity.isDead()) {
				MOB_SPAWN_INHIBITORS.remove(i--);
				tTileEntity.onUnregisterInhibitor();
			} else try {
				// СТЫК ЧАСТИЧНО ЗАКРЫТ (интегратор): интерфейс ITileEntityMobSpawnInhibitor.inhibitMobSpawn(...)
				// переведён на MobSpawnEvent.PositionCheck (gregapi/tileentity/ITileEntityMobSpawnInhibitor.java).
				// Implementer gregtech/tileentity/multiblocks/MultiTileEntityVonDaGraagg.java НАРОЧНО откачен
				// интегратором к HEAD (71c8179) — контент-файл несёт другие незакрытые жилы (WD raw-coord
				// block-API), распространение вне этого захода центров (задача #18); implementer сейчас
				// НЕ реализует обновлённый интерфейс (другой класс проблем, известен).
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

	// ArrowNockEvent.result (1.7.10 — ItemStack-override "какой предмет реально натягивается") в neo не существует — современный
	// ArrowNockEvent несёт только getBow()/getHand()/getLevel()/hasAmmo()/getAction(InteractionResult) (сверено,
	// net.neoforged.neoforge.event.entity.player.ArrowNockEvent.java) — прямого способа подменить "натягиваемый" предмет нет.
	// PORT-TODO(EVENTS, ArrowNockEvent-result-override): нет 1:1 замены полю result — тело временно не выполняется.
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onArrowNockEvent(ArrowNockEvent aEvent) {
		Player aPlayer = aEvent.getEntity();
		if (F && !aEvent.isCanceled() && aEvent.hasAmmo() && ST.projectile(aPlayer.getInventory(), TD.Projectiles.ARROW) != null) {
			aEvent.setCanceled(T);
		}
	}

	// ArrowLooseEvent.bow/.charge (1.7.10) — getBow()/getCharge() (сверено, ArrowLooseEvent.java). ItemBow → BowItem (переименование
	// класса, сверено net.minecraft.world.item.BowItem). World.playSoundAtEntity → централизованный UT.Sounds.send (уже используется
	// в этом файле повсеместно). World.spawnEntityInWorld → ServerLevel.addFreshEntity (сверено, ServerLevel.java:976).
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

			if (tSpeed >= 1.0F) tArrowEntity.setCritArrow(T); // было setIsCritical(boolean) (1.7.10) — neo AbstractArrow: setCritArrow(boolean) (сверено, AbstractArrow.java:540)

			// PORT-TODO(EVENTS, AbstractArrow-enchant-bonus-api): Enchantments.POWER/.punch/.flame (1.7.10 static enum-like поля) удалены
			// (та же проблема класса, что и Blocks.*/Items.* по файлу) — плюс AbstractArrow больше не несёт getDamage()/setKnockbackStrength(int)/
			// setFire(int) в старом виде (сверено, AbstractArrow.java — только setBaseDamage(double), нет getter; knockback/fire — другие пути:
			// Entity.setRemainingFireTicks(int) вместо setFire(int), knockback вообще не найден как метод). Требует отдельного F#-решения.
			// int
			// tLevel = UT.NBT.getEnchantmentLevel(Enchantments.POWER, aEvent.getBow());
			// if (tLevel > 0) tArrowEntity.setDamage(tArrowEntity.getDamage() + tLevel * 0.5D + 0.5D);
			// tLevel = UT.NBT.getEnchantmentLevel(Enchantments.PUNCH, aEvent.getBow());
			// if (tLevel > 0) tArrowEntity.setKnockbackStrength(tLevel);
			// tLevel = UT.NBT.getEnchantmentLevel(Enchantments.FLAME, aEvent.getBow());
			// if (tLevel > 0) tArrowEntity.setFire(tLevel * 100);

			aEvent.getBow().hurtAndBreak(1, aPlayer, InteractionHand.MAIN_HAND); // было damageItem(int,EntityLivingBase) (1.7.10) — neo: hurtAndBreak(int,LivingEntity,InteractionHand) (сверено, ItemStack.java:524)
			aEvent.getBow().getItem();
			UT.Sounds.send("random.bow", 1.0F, 1.0F / (RNGSUS.nextFloat() * 0.4F + 1.2F) + tSpeed * 0.5F, aPlayer);

			tArrowEntity.pickup = net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup.ALLOWED; // было canBePickedUp=1 (1.7.10 int) — neo: public поле pickup типа AbstractArrow.Pickup (сверено, AbstractArrow.java:72)

			if (!UT.Entities.hasInfiniteItems(aPlayer)) aArrow.setCount(aArrow.getCount()-1);
			if (aArrow.getCount() == 0) ST.denull(aPlayer);

			if (!aPlayer.level().isClientSide() && aPlayer.level() instanceof ServerLevel aServerLevel) aServerLevel.addFreshEntity(tArrowEntity);

			aEvent.setCanceled(T);
		}
	}
	
	// Тело 1:1, не тронуто — раньше "@Override" от выдуманного IFuelHandler-как-события, теперь просто
	// вызывается из onFurnaceFuelBurnTime(...) выше (F12/R3-мост).
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
		long rFuelValue = UT.NBT.getNBT(aFuel).getLong(NBT_FUEL_VALUE).orElse(0L); // было прямое сравнение с long (1.7.10) — neo CompoundTag.getLong возвращает Optional<Long> (сверено, CompoundTag.java:331)
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
}
