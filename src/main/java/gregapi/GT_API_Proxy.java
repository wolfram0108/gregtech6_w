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
 * нет) — якорь client-render-фазы (F14 gui-client-screen —  CLIENT, серверная GUI-логика работает; клиент-экран = client-render, headless-неверифиц), сама GUI-логика
 * не трогается.
 */
public abstract class GT_API_Proxy extends Abstract_Proxy {
	public GT_API_Proxy() {
		// F7 (контракт-шов, компилятор слеп): neo EventBus.register(this) ЗАПРЕЩЁН — правило «супертип регистрируемого
		// объекта не смеет нести @SubscribeEvent» (fml EventBus.java:117-126), а base-класс держит все обработчики
		// централизованно (философия «одно место»). Механизм вынесен в Abstract_Proxy.registerSubscribeEvents() —
		// один per-method-addListener на весь мод (тот же приём применяет gregtech.GT_Proxy).
		registerSubscribeEvents();
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

	/** S6: client-only {@code Minecraft.getInstance().isSingleplayer()} нельзя звать из общего кода (на dedicated
	 *  класса {@code Minecraft} нет). Центр side-разделения (тот же приём, что {@link #getThePlayer()}): сервер = F. */
	public boolean isSingleplayer() {
		return F;
	}

	/** S6: чтение assets-ресурса (PNG иконки для среднего цвета) идёт через client {@code Minecraft.getResourceManager()};
	 *  из общего кода нельзя (на dedicated нет Minecraft/assets). Центр: сервер = null (assets предметов на сервере нет). */
	public java.io.InputStream getResourceStream(net.minecraft.resources.Identifier aRL) {
		return null;
	}

	public boolean sendUseItemPacket(Player aPlayer, Level aWorld, ItemStack aStack) {
		return F;
	}
	
	/** F-GUI: client-render якорь (F14 gui-client-screen —  CLIENT client-render-фаза) — не {@code @Override}, цели нет
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
	// [GT6-STACKPROBE] снята (§9, уборка BUG-041 — воспроизведение уборки параллельного агента, чей staged-вариант
	// содержал регресс тик-машины 045 и не был взят; сама уборка — его работа, здесь только повторена).

	
	@Override
	public void onProxyAfterServerStopping(Abstract_Mod aMod, ServerStoppingEvent aEvent) {
		checkSaveLocation(null, T);
		MultiTileEntityRegistry.onServerStop();
	}

	/**
	 * F3-render (client): единая точка подписки клиентских модель-типов на mod-bus. На сервере no-op
	 * (общий код не грузит client-only классы). Клиент-прокси регистрирует {@code GT6BlockModel.Unbaked}
	 * через {@code RegisterBlockStateModels} (decisions/F3-render.md §2.1). Централизация 1:1 — один тип на весь мод.
	 */
	public void registerClientModels(net.neoforged.bus.api.IEventBus aModBus) {/* server: no-op */}
	
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
	/** F-tree (BUG-005): форс-распад ВАНИЛЬНОЙ листвы для WD.leafdecay — neo расщепил канал 1.7.10 updateTick
	 *  (scheduled tick = пересчёт DISTANCE, распад = randomTick); записи {ServerLevel, BlockPos, Long срок(SERVER_TIME), Integer попытка}. */
	public  static final List<Object[]>                     DELAYED_LEAF_DECAYS            = new ArrayListNoNulls<>();
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
				// [GT6-MTEAUDIT] BUG-057 — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6mteauditprobe.flag")) gt6MTEAuditProbeTick(aEvent.getServer());
				// [GT6-WIREPROBE] верификационный стенд «Связка №1 — электрические провода EU» (Ф3.1) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6wireprobe.flag")) gt6WireProbeTick(aEvent.getServer());
				// [GT6-FLUIDPIPEPROBE] верификационный стенд «Связка №2 — жидкостные трубы» (Ф3.1) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6fluidpipeprobe.flag")) gt6FluidPipeProbeTick(aEvent.getServer());
				// [GT6-ITEMPIPEPROBE] верификационный стенд «Связка №3 — предметные трубы» (Ф3.1, на каркасе GT6ProbeStand) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6itempipeprobe.flag")) gt6ItemPipeProbeTick(aEvent.getServer());
				gt6DungeonRedstoneWakeTick();
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
					
					// EVENTS impossible-1:1 (neo-модель): ChestGenHooks (1.7.10 Forge runtime chest-content-реестр) удалён — vanilla-лут
					// полностью data-driven (JSON LootTable), рантайм-"GenHooks" нет. OreDict-унификация лута vanilla-сундуков в neo = не
					// мутация реестра, а GlobalLootModifier (отдельная data-driven подсистема); IE-часть — форейн (отсутствует). Отключено верно,
					// без суррогата. Собственный GT6-лут (task F-loot) уже на neo LootTable.
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

					// EVENTS impossible-1:1 (neo-модель): 1.7.10 менял плоскую Map<ItemStack,ItemStack> смелтинга для унификации выходов.
					// neo RecipeManager типизирован (RecipeType.SMELTING, RecipeHolder<SmeltingRecipe>) и рецепты ИММУТАБЕЛЬНЫ —
					// выход существующего рецепта в рантайме не мутируется (перечислить можно getAllRecipesFor, изменить — нет).
					// Унификация smelting-выходов в neo = замена рецепта/датаген, не мутация — вне этого пути. Отключено верно.
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

				// F-tree (BUG-005): созревшие форс-распады ванильной листвы (кладёт WD.leafdecay) — исполняем ДВИЖКОВЫМИ
				// каналами: state.tick (пересчёт DISTANCE, LeavesBlock.tick:79-81) + повторное чтение + state.randomTick
				// (распад decaying-листа, LeavesBlock.randomTick:67-72). Никакой своей логики распада — только форс вызова
				// того, что движок вызвал бы сам по случайным тикам (1.7.10: оба канала были ОДНИМ updateTick).
				// Если лист уцелел (каскад DISTANCE от снесённых брёвен ещё не дошёл — он идёт волнами delay-1) — повтор
				// через 8 тиков, максимум 40 попыток: настоящая опора (бревно соседнего дерева) исчерпает лимит и выпадет.
				for (int i = 0; i < DELAYED_LEAF_DECAYS.size(); i++) {
					Object[] tEntry = DELAYED_LEAF_DECAYS.get(i);
					if (SERVER_TIME >= (Long)tEntry[2]) {
						DELAYED_LEAF_DECAYS.remove(i--);
						try {
							ServerLevel tLevel = (ServerLevel)tEntry[0];
							BlockPos tPos = (BlockPos)tEntry[1];
							if (tLevel.isLoaded(tPos)) {
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

	// [BUG-047] F-hook-removed → центральный мост: 1.7.10 Forge-хуки BlockRailBase.onMinecartPass/getRailMaxSpeed
	// ВЫРЕЗАНЫ из NeoForge 26.1.2 (extensions-каталог: только IBaseRailBlockExtension — isFlexibleRail/canMakeSlopes/
	// getRailDirection/isValidRailShape; клапан скорости захардкожен OldMinecartBehavior.getMaxSpeed:410-411 = 0.4,
	// буст движок читает ТОЛЬКО с instanceof PoweredRailBlock — OldMinecartBehavior:115-116). Мост: EntityTickEvent.Post =
	// раз в тик на сущность ПОСЛЕ движения — та же фаза, что 1.7.10 хвост EntityMinecart.func_145821_a (вызывал
	// onMinecartPass после moveAlongTrack); позиция рельса — getCurrentBlockPosOrRailBelow (канал самого движка).
	// getRailMaxSpeed-кламп восстанавливает per-rail скорости (медленные Al 0.2/Bronze 0.3 — 1:1).
	// PORT-TODO(F-hook-removed): скорости рельсов ВЫШЕ движковых 0.4 (Ti 1.2 … Adamantium 4.0) недостижимы —
	// кламп СМЕЩЕНИЯ захардкожен внутри OldMinecartBehavior.moveAlongTrack:208-211 (getMaxSpeed:410 = 0.4),
	// per-rail переопределения в 26.1.2 нет; поднять при появлении хука/решении о патче. Реестр: DEFERRED-LEDGER.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onMinecartPassBridge(EntityTickEvent.Post aEvent) {
		if (!(aEvent.getEntity() instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart tCart) || tCart.level().isClientSide()) return;
		BlockPos tRailPos = tCart.getCurrentBlockPosOrRailBelow();
		if (!(WD.block(tCart.level(), tRailPos.getX(), tRailPos.getY(), tRailPos.getZ()) instanceof gregapi.block.misc.BlockBaseRail tRail)) return;
		tRail.onMinecartPass(tCart.level(), tCart, tRailPos.getX(), tRailPos.getY(), tRailPos.getZ());
		float tMax = tRail.getRailMaxSpeed(tCart.level(), tCart, tRailPos.getX(), tRailPos.getY(), tRailPos.getZ());
		net.minecraft.world.phys.Vec3 tCartMotion = tCart.getDeltaMovement();
		if (Math.abs(tCartMotion.x) > tMax || Math.abs(tCartMotion.z) > tMax)
			tCart.setDeltaMovement(net.minecraft.util.Mth.clamp(tCartMotion.x, -tMax, tMax), tCartMotion.y, net.minecraft.util.Mth.clamp(tCartMotion.z, -tMax, tMax));
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
							// EVENTS: golden ставил "health" И "field_70291_e" — это ОДНО поле (field_70291_e = SRG-имя health в 1.7.10
							// EntityItem; дублирование деобф+SRG). health=250 выше уже покрывает оба → второй set был избыточен, не потеря.
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
					// EVENTS impossible-1:1: neo ExperienceOrb.age приватно, без public-сеттера — перенос возраста при слиянии
					// орбов (1.7.10 xpOrbAge public) не выразим; слияние значения работает, возраст сохраняет выживший орб (age
					// влияет лишь на despawn-таймер, ~5 мин) → шаг пропущен, слияние XP функционально.
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
				// EVENTS model-shift (не core-data-loss): 1.7.10 sweep по World.loadedTileEntityList раз в 20 тиков помечал
				// ITileEntityNeedsSaving-TE dirty (crash-resilience: между авто-сейвами). neo хранит BlockEntity per-chunk
				// (LevelChunk.getBlockEntities()) — плоского world-списка нет. Персистентность НЕ теряется: neo пишет ВСЕ BE
				// при выгрузке чанка + периодическом авто-сейве грязных чанков. Оставшийся аспект — только crash-resilience
				// (пометка dirty между сейвами); neo-идиома — TE зовёт setChanged() при мутации (распределённо), а не централь-
				// ный sweep. ITileEntityNeedsSaving реализует лишь TileEntityBase02AdjacentTEBuffer; выгрузка-сейв покрывает.
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
								// F5/BUG-045 (1:1): блок восстановлен на живом compat-mirror IFluidContainerItem (оригинал :797-805);
								// foreign-gated (Betweenlands в 26.1.2 отсутствует — ветка tBetweenlands мертва, но контракт 1:1).
								} else if (tStack.getItem() instanceof IFluidContainerItem) {
									FluidStack tFluid = ((IFluidContainerItem)tStack.getItem()).getFluid(tStack);
									if (tFluid != null && !FL.Potion_Tainted.is(tFluid) && FluidsGT.POTION.contains(FL.regName(tFluid.getFluid()))) {
										((IFluidContainerItem)tStack.getItem()).drain(tStack, Integer.MAX_VALUE, T);
										((IFluidContainerItem)tStack.getItem()).fill(tStack, FL.Potion_Tainted.make(tFluid.getAmount()), T);
									}
								}
								ItemStack tRotten = RottingUtil.rotting(tStack, aPlayer.level(), UT.Code.roundDown(aPlayer.getX()), UT.Code.roundDown(aPlayer.getY()), UT.Code.roundDown(aPlayer.getZ()));
								if (ST.invalid(tRotten)) {tStack.setCount(0); aPlayer.getInventory().setItem(i, ST.nn(NI)); continue;} // F15-граница: setItem(null) на NonNullList кидает NPE
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
									// EVENTS: 1.7.10 Potion.moveSlowdown → neo MobEffects.SLOWNESS (Holder, существует — ренейм, не удаление).
									// getActivePotionEffect→getEffect. Восстановлено 1:1 (applyPotion(Entity,Holder,...) уже поддержан, UT.java:3036).
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
							// EVENTS: Potion.poison → neo MobEffects.POISON (Holder, существует). Восстановлено 1:1.
							UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.POISON, 1200, tCraponite, T);
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
	// tChunk.isTerrainPopulated (1.7.10 генерация-флаг) в neo не существует (impossible-1:1) — просматриваемые чанки ВСЕГДА
	// FULL-статуса, проверка не нужна (опущена верно); chunkTileEntityMap → getBlockEntities().
	// ⚠ КАНОН neo (ChunkWatchEvent.java:64-65): Watch = чанк лишь ПОСТАВЛЕН В ОЧЕРЕДЬ — «must NOT be used to send
	// additional chunk-related data to the client as the client will not be aware of the chunk yet»; для данных — Sent.
	// На Watch GT6-пакеты BE прилетали РАНЬШЕ чанка → клиент дропал их (блока ещё нет) → клиент-BE worldgen-MTE
	// (камешки/палки) не создавался при ПОВТОРНОМ входе в мир → BER рисовать нечего (репорт игрока 2026-07-19).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onChunkWatchEvent(ChunkWatchEvent.Sent aEvent) {
		LevelChunk tChunk = aEvent.getChunk();
		if (tChunk == null) return;
		// F17: отправка игроку может идти В ТОМ ЖЕ тике, что загрузка чанка, а очередь реконструкции стабов
		// (STUB_QUEUE) дренируется тиками — ПОСЛЕ отправки. При загрузке чанка MTE-BE рождается как
		// TileEntityLoaderStub (MTE_TYPE-фабрика, класс из sub-ID недоступен), стаб не ITileEntitySynchronising →
		// рассылка проходила мимо; тикающие MTE дозревали от своих тиков, notick (стены) — никогда. Синхронная
		// реконструкция стабов ЭТОГО чанка существующим единым механизмом ДО рассылки: порядок «BE реальны →
		// синк» гарантирован по построению для ВСЕХ MTE.
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
				// EVENTS: Potion.weakness/digSlowdown → neo MobEffects.WEAKNESS/MINING_FATIGUE (Holder, существуют). Восстановлено 1:1.
				// ADAPT-002: Mining Fatigue при поломке инструмента ослаблен III→I (amplifier 2→0) по запросу игрока. Weakness 1:1 (III).
				UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.WEAKNESS      ,  300, 2, F);
				UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.MINING_FATIGUE, 1200, 0, F);
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
				// EVENTS/F18-redundant: вызов кормил ТОЛЬКО ST.check→vanilla-достижение (F18, neo авто-выдаёт advancement) — moot.
				// 1.7.10 metadata Block-API (getItemDropped/getDamageValue) удалено в пользу LootTable+BlockState (impossible-1:1),
				// но здесь не нужно: ST.check и так no-op. Отключено верно.
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
						// F10 external-compat (foreign-gated): TF Transformation Powder (TF отсутствует в сборке → эта ветка мертва).
						// 1.7.10 BaseSpawner String-API (getEntityNameToSpawn/setEntityName) → neo EntityType-модель (setEntityId(
						// EntityType,Level,RandomSource,BlockPos), BaseSpawner.java:55); neo-путь при наличии TF = BuiltInRegistries.
						// ENTITY_TYPE.get(Identifier) для String→EntityType-моста над TRANSFORMATION_POWDER_SPAWNER_MAP. Гейт F (TF absent).
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
	// getItemAbility()==ItemAbilities.HOE_TILL. F6 (1:1): 1.7.10 «Blocks.dirt && metadata!=0» (coarse=1/podzol=2) → в neo это
	// ОТДЕЛЬНЫЕ Block-типы Blocks.COARSE_DIRT/Blocks.PODZOL (не метадата dirt) — точный набор, не переизобретение. Восстановлено.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onUseHoeEvent(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent aEvent) {
		if (aEvent.getItemAbility() == net.neoforged.neoforge.common.ItemAbilities.HOE_TILL && (aEvent.getState().getBlock() == Blocks.COARSE_DIRT || aEvent.getState().getBlock() == Blocks.PODZOL)) aEvent.setCanceled(T);
	}

	// F12: blast-resistant-mob-spawners (golden mob_spawner.setResistance(6000000) = blast-immune). neo Properties immutable →
	// эквивалент через ExplosionEvent.Detonate: убираем SPAWNER-позиции из разрушаемых (спавнер переживает взрыв). Config кэширован.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onExplosionDetonate(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate aEvent) {
		if (BLAST_RESISTANT_MOB_SPAWNERS) aEvent.getAffectedBlocks().removeIf(p -> aEvent.getLevel().getBlockState(p).getBlock() == net.minecraft.world.level.block.Blocks.SPAWNER);
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
			// F12: harder-mob-spawners (golden setHardness(500) vs ванильные 5 → ×100 медленнее). neo Properties immutable →
			// эквивалент через BreakSpeed: speed × (5/500)=0.01 для vanilla-спавнера. Config-флаг кэширован в GT_API.
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
				// F9 (1:1): SAND/ROCK-множитель по материалу нижнего блока. WD.getMaterial(Block) РЕАЛИЗОВАН (Block→gregapi.Material
				// мост, WD.java:474) — стух-тег «удалён» снят.
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
		// F13-контракт (BUG-016): 1.7.10 HarvestDropsEvent.blockMetadata = мета РАЗРУШЕННОГО блока; в neo блок к этому
		// моменту уже удалён из мира (meta(aWorld,...)=0 всегда) — мета берётся из снимка состояния события.
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

				// BUG-040: восстановлен 1:1 механизм 1.7.10 «инструмент собирает срезанный дроп сразу в инвентарь»
				// (оригинал onHarvestDrops:1342-1369). Прежняя заглушка «F &&» снята — её обоснование («ST.entity(Entity,
				// ItemStack) отсутствует») было ошибкой грепа: метод ЕСТЬ (ST.java:614, тип ItemEntity, не старый
				// EntityItem — потому и промахнулся греп). Синтетический ItemEntity (ST.entity_ НЕ спавнит в мир —
				// ST.java:615, без addFreshEntity → дюпа нет) постится в ItemEntityPickupEvent.Pre — 1:1-аналог 1.7.10
				// EntityItemPickupEvent («спросить другие моды, не перехватят ли подбор», сверено ItemEntityPickupEvent.java).
				// Маппинг движка (F-адаптация на его уровне): Result.ALLOW → canPickup()==TriState.TRUE; isDead → isRemoved().
				// Строки оригинала isDead=F/=T опущены: ST.entity-синтетик по дефолту не removed и в мир не добавлен
				// (эфемерен, GC) — поведение тождественно. Не перехватил никто → ST.add кладёт в инвентарь игрока (+звук).
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
		// F6 neo-async-chunkgen (impossible-1:1; ОБЕЗВРЕЖЕН DEADLOCK создания мира, пойман jstack'ом): при спавне мобов
		// ВО ВРЕМЯ ГЕНЕРАЦИИ чанка (EntitySpawnReason.CHUNK_GENERATION) neo передаёт WorldGenRegion, но GT6-защиты спавна
		// читают блоки/свет через aEvent.getEntity().level() = ИСТИННЫЙ ServerLevel → getBlockState форсит
		// ServerChunkCache.getChunk(...).join() на ЕЩЁ ГЕНЕРИРУЕМОМ чанке → чанк ждёт сам себя → вечный deadlock
		// worldgen-потока → "Loading terrain…" висит навсегда (стек: NaturalSpawner.spawnMobsForChunkGeneration →
		// onCheckSpawnEvent → WD.opq → Level.getBlockState → getChunk.join). GT6-защиты спавна — ГЕЙМПЛЕЙНЫЕ (ген-фаза
		// ставит лишь стартовых пассивных мобов); в ген-контексте хендлер пропускаем, обычный спавн (ServerLevel) — 1:1.
		if (aEvent.getSpawnType() == net.minecraft.world.entity.EntitySpawnReason.CHUNK_GENERATION) return;
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
				// EVENTS foreign-gated (Et Futurum Requiem не портирован на neo; instanceq-классы не существуют): ganymedes01.etfuturum.entities.{EntityHusk,EntityStray,EntityZombieVillager}
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
			// EVENTS: 1.7.10 World.getWorldInfo().getSpawnX()/getSpawnZ() → neo Level.getLevelData().getRespawnData().globalPos().pos()
			// (мислейбл был: «методов нет» — на деле переименовано в RespawnData). SPAWN_ZONE_MOB_PROTECTION восстановлено 1:1.
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
	// EVENTS impossible-1:1 (neo ArrowNockEvent без result-поля): нет 1:1 замены полю result — тело временно не выполняется.
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

			// PORT-TODO(F-entity-construction, arrow-weapon-enchants): neo сменил МОДЕЛЬ — POWER/PUNCH/FLAME больше не применяются
			// вручную (getDamage/setKnockbackStrength/setFire сняты; baseDamage private без getter, knockback — не публичный сеттер).
			// Движок применяет их АВТОМАТИЧЕСКИ из firedFromWeapon (AbstractArrow.java:98/422/511: EnchantmentHelper.modifyDamage +
			// doKnockback по firedFromWeapon). neo-путь 1:1 = протащить лук как firedFromWeapon в ctor снаряда — но EntityProjectile
			// строится с ItemStack.EMPTY (см. IItemProjectile:EntityProjectile), а его EntityType — плейсхолдер ARROW: обе завязки —
			// на отложенную F-entity-construction (реальная регистрация EntityType + проброс лука в конструкцию снаряда). Enchant'ы
			// (Enchantments.POWER/PUNCH/FLAME) в neo = ResourceKey, НЕ удалены — блокирует именно конструкция снаряда, не сами чары.

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

	// F6-worldgen redstone-wake (данжи #39, живой тест «дверь срабатывает со второго раза»): в 1.7.10 редстоун-цепи
	// данжа оживлял flags=3 факелов при populate (нотификации соседей); WorldGenRegion апдейтов не шлёт ВООБЩЕ →
	// провода рождаются POWER=0 и цепь двери мертва до первого пинка (движковый postprocess обновляет только ФОРМУ —
	// LevelChunk.postProcessGeneration:590 updateFromNeighbourShapes, setBlock без бита UPDATE_NEIGHBORS). Эквивалент
	// 1:1: при ПЕРВОЙ загрузке (генерации) чанка данж-области будим цепь — нотификация соседей каждой редстоун-позиции
	// Y-окна данжа. Данж-чанк детерминирован якорной формулой (WorldgenDungeonGT.isDungeonAreaChunk); очередь на
	// серверный тик (в момент ChunkEvent.Load соседние чанки могут быть не готовы). PRODUCTION-механизм (не проба).
	private static final java.util.concurrent.ConcurrentLinkedQueue<Object[]> sDgRedstoneWake = new java.util.concurrent.ConcurrentLinkedQueue<>();
	@net.neoforged.bus.api.SubscribeEvent
	public void onChunkLoadDungeonRedstoneWake(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		if (!(aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel tLevel)) return;
		if (!aEvent.isNewChunk()) return; // только свежесгенерённые (у загруженных состояние уже пересчитано прошлой сессией)
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		if (!gregapi.worldgen.dungeon.WorldgenDungeonGT.isDungeonAreaChunk(tLevel, tPos.x(), tPos.z())) return;
		sDgRedstoneWake.add(new Object[] {tLevel, tPos});
	}
	private static void gt6DungeonRedstoneWakeTick() {
		for (int n = 0; n < 4; n++) {
			Object[] tWake = sDgRedstoneWake.poll();
			if (tWake == null) return;
			net.minecraft.server.level.ServerLevel tLevel = (net.minecraft.server.level.ServerLevel)tWake[0];
			net.minecraft.world.level.ChunkPos tPos = (net.minecraft.world.level.ChunkPos)tWake[1];
			net.minecraft.world.level.chunk.LevelChunk tChunk = tLevel.getChunkSource().getChunkNow(tPos.x(), tPos.z());
			if (tChunk == null) {sDgRedstoneWake.add(tWake); return;} // ещё не FULL — попробуем следующим тиком
			int tY0 = gregapi.util.WD.remapY(tLevel, 20);
			for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = tY0-12; y <= tY0+14; y++) {
				BlockPos tBP = new BlockPos((tPos.x() << 4) + x, y, (tPos.z() << 4) + z);
				net.minecraft.world.level.block.Block tBlock = tChunk.getBlockState(tBP).getBlock();
				if (tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_WIRE || tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_WALL_TORCH
				 || tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_TORCH || tBlock == net.minecraft.world.level.block.Blocks.STICKY_PISTON
				 || tBlock == net.minecraft.world.level.block.Blocks.PISTON || tBlock == net.minecraft.world.level.block.Blocks.REDSTONE_LAMP) {
					tLevel.updateNeighborsAt(tBP, tBlock, null);
				}
			}
		}
	}

	// [GT6-MTEAUDIT] BUG-057 (MTE-блоки со временем прозрачны): живой аудит BE — снять при уборке фазы.
	// Гейт §2.1 (-Pgt6probes + run/gt6mteauditprobe.flag). Фазы: C=аудит зоны игрока (сломанные блоки репро-мира),
	// A=телепорт в свежие чанки + аудит (ожидание: здоровые BE), затем saveAllChunks + relog (двухмировой приём BUG-002),
	// B=аудит той же свежей зоны после перезахода. A(real>0) -> B(NULL/stub) = детерминированная репродукция «со временем».
	private static int sMTEAuditTick = -1, sMTEAuditPhase = 0, sMTEAuditWait = 0, sMTEAuditServerHash = 0, sMTEAuditSession = 0;
	private static net.minecraft.core.BlockPos sMTEAuditFreshPos = null;
	private static int[] sMTEAuditCountsA = null;
	public static volatile int sMTEAuditClientCmd = 0; // 0=нет, 1=клиент-скан, 2=relog
	public static volatile String sMTEAuditScanLabel = "";
	@SuppressWarnings("resource")
	public static void gt6MTEAuditProbeTick(net.minecraft.server.MinecraftServer aServer) {
		java.io.PrintStream O = OUT;
		int tHash = System.identityHashCode(aServer);
		if (sMTEAuditServerHash == 0) sMTEAuditServerHash = tHash;
		else if (sMTEAuditServerHash != tHash) {
			sMTEAuditServerHash = tHash; sMTEAuditSession++; sMTEAuditTick = -1; sMTEAuditWait = 0;
			O.println("[GT6-MTEAUDIT] НОВАЯ СЕССИЯ СЕРВЕРА #" + sMTEAuditSession + " (relog состоялся), фаза " + sMTEAuditPhase + " -> 7");
			if (sMTEAuditPhase == 6) sMTEAuditPhase = 7;
		}
		sMTEAuditTick++;
		try {
			if (sMTEAuditPhase < 10 && sMTEAuditTick > 9000) {O.println("[GT6-MTEAUDIT] EXC timeout: фаза " + sMTEAuditPhase + " не завершилась за 9000 тиков сессии"); sMTEAuditPhase = 10; return;}
			if (aServer.getPlayerList().getPlayers().isEmpty()) return;
			net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
			net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
			if (sMTEAuditPhase == 0 && sMTEAuditTick >= 200) {
				O.println("========== [GT6-MTEAUDIT] BUG-057: аудит BE MTE-семьи (мир=" + tLevel.getServer().getWorldData().getLevelName() + ") ==========");
				gt6MTEAuditScan("PHASE-C(зона игрока)", tLevel, tPlayer.blockPosition());
				sMTEAuditScanLabel = "PHASE-C"; sMTEAuditClientCmd = 1;
				sMTEAuditPhase = 1; sMTEAuditWait = 0;
			} else if (sMTEAuditPhase == 1) {
				if (sMTEAuditClientCmd == 0 && ++sMTEAuditWait > 60) {
					int tFX = tPlayer.blockPosition().getX() + 4096, tFZ = tPlayer.blockPosition().getZ();
					sMTEAuditFreshPos = new net.minecraft.core.BlockPos(tFX, 250, tFZ);
					tPlayer.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
					tPlayer.teleportTo(tLevel, tFX + 0.5, 250, tFZ + 0.5, java.util.Set.of(), 0, 0, true);
					O.println("[GT6-MTEAUDIT] телепорт в свежую зону " + sMTEAuditFreshPos.toShortString() + ", жду генерацию чанков...");
					sMTEAuditPhase = 2; sMTEAuditWait = 0;
				}
			} else if (sMTEAuditPhase == 2) {
				if (tLevel.getChunkSource().getChunkNow(sMTEAuditFreshPos.getX() >> 4, sMTEAuditFreshPos.getZ() >> 4) != null) {
					int tY = tLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sMTEAuditFreshPos.getX(), sMTEAuditFreshPos.getZ());
					sMTEAuditFreshPos = new net.minecraft.core.BlockPos(sMTEAuditFreshPos.getX(), tY, sMTEAuditFreshPos.getZ());
					tPlayer.teleportTo(tLevel, sMTEAuditFreshPos.getX() + 0.5, tY + 1, sMTEAuditFreshPos.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
					O.println("[GT6-MTEAUDIT] свежая зона готова, поверхность y=" + tY + "; прогрев 300 тиков (отложка вордгена)...");
					sMTEAuditPhase = 3; sMTEAuditWait = 0;
				} else if (++sMTEAuditWait > 1200) {O.println("[GT6-MTEAUDIT] EXC чанк свежей зоны не сгенерировался за 1200 тиков"); sMTEAuditPhase = 10;}
			} else if (sMTEAuditPhase == 3) {
				if (++sMTEAuditWait >= 300) {
					sMTEAuditCountsA = gt6MTEAuditScan("PHASE-A(свежие чанки)", tLevel, sMTEAuditFreshPos);
					sMTEAuditScanLabel = "PHASE-A"; sMTEAuditClientCmd = 1;
					sMTEAuditPhase = 4; sMTEAuditWait = 0;
				}
			} else if (sMTEAuditPhase == 4) {
				if (sMTEAuditClientCmd == 0 && ++sMTEAuditWait > 40) {
					boolean tRC = aServer.saveAllChunks(false, true, true);
					O.println("[GT6-MTEAUDIT] saveAllChunks(false,true,true) => " + tRC + "; relog через 40 тиков");
					sMTEAuditPhase = 5; sMTEAuditWait = 0;
				}
			} else if (sMTEAuditPhase == 5) {
				if (++sMTEAuditWait > 40) {
					O.println("[GT6-MTEAUDIT] сигнал клиенту: relog в тот же мир");
					sMTEAuditClientCmd = 2;
					sMTEAuditPhase = 6; sMTEAuditWait = 0;
				}
			} else if (sMTEAuditPhase == 6) {
				if (++sMTEAuditWait > 2400) {O.println("[GT6-MTEAUDIT] EXC relog не состоялся за 2400 тиков"); sMTEAuditPhase = 10;}
			} else if (sMTEAuditPhase == 7) {
				if (++sMTEAuditWait > 100) {
					tPlayer.teleportTo(tLevel, sMTEAuditFreshPos.getX() + 0.5, 250, sMTEAuditFreshPos.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
					O.println("[GT6-MTEAUDIT] сессия-2: телепорт назад в свежую зону " + sMTEAuditFreshPos.toShortString());
					sMTEAuditPhase = 8; sMTEAuditWait = 0;
				}
			} else if (sMTEAuditPhase == 8) {
				boolean tReady = tLevel.getChunkSource().getChunkNow(sMTEAuditFreshPos.getX() >> 4, sMTEAuditFreshPos.getZ() >> 4) != null;
				if (tReady && ++sMTEAuditWait > 200) {
					int tY = tLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sMTEAuditFreshPos.getX(), sMTEAuditFreshPos.getZ());
					tPlayer.teleportTo(tLevel, sMTEAuditFreshPos.getX() + 0.5, tY + 1, sMTEAuditFreshPos.getZ() + 0.5, java.util.Set.of(), 0, 0, true);
					int[] tB = gt6MTEAuditScan("PHASE-B(та же зона после save+relog)", tLevel, sMTEAuditFreshPos);
					int[] tA = sMTEAuditCountsA;
					if (tA != null) {
						boolean tRepro = tA[1] > 0 && (tB[3] > 0 || tB[2] > 0 || tB[1] < tA[1]);
						O.println("[GT6-MTEAUDIT] ВЕРДИКТ save+relog: A(blocks=" + tA[0] + " real=" + tA[1] + " stub=" + tA[2] + " NULL=" + tA[3] + ") -> B(blocks=" + tB[0] + " real=" + tB[1] + " stub=" + tB[2] + " NULL=" + tB[3] + ") => " + (tRepro ? "РЕПРО: BE потеряны/застряли после save+relog" : "потери не видно"));
					}
					sMTEAuditScanLabel = "PHASE-B"; sMTEAuditClientCmd = 1;
					sMTEAuditPhase = 9; sMTEAuditWait = 0;
				} else if (!tReady && ++sMTEAuditWait > 1200) {O.println("[GT6-MTEAUDIT] EXC чанк свежей зоны не загрузился после relog"); sMTEAuditPhase = 10;}
			} else if (sMTEAuditPhase == 9) {
				if (sMTEAuditClientCmd == 0 && ++sMTEAuditWait > 20) {
					O.println("========== [GT6-MTEAUDIT] DONE ==========");
					sMTEAuditPhase = 10; sMTEAuditWait = 0;
				}
			} else if (sMTEAuditPhase == 10 && sMTEAuditTick % 200 == 0 && sMTEAuditWait++ < 10) {
				O.println("[GT6-MTEAUDIT] heartbeat: сервер жив, тик " + sMTEAuditTick);
			}
		} catch (Throwable e) {O.println("[GT6-MTEAUDIT] EXC " + e); e.printStackTrace(O); sMTEAuditPhase = 10;}
	}

	/** [GT6-MTEAUDIT] скан ±32 блока по горизонтали (вся высота) вокруг центра: каждый MTE-блок классифицируется по BE
	 *  (real IMultiTileEntity / TileEntityLoaderStub / NULL / other). Работает на ServerLevel И ClientLevel (Level-обобщён,
	 *  BE берутся из map чанка НАПРЯМУЮ — без ленивого создания через Level.getBlockEntity). Снять при уборке фазы. */
	public static int[] gt6MTEAuditScan(String aLabel, net.minecraft.world.level.Level aLevel, net.minecraft.core.BlockPos aCenter) {
		java.io.PrintStream O = OUT;
		int tR = 32, tBlocks = 0, tReal = 0, tStub = 0, tNull = 0, tOther = 0, tMissChunks = 0;
		int tSampleN = 0, tSampleS = 0, tSampleR = 0;
		java.util.Map<String, int[]> tPerBlock = new java.util.TreeMap<>();
		StringBuilder tSamples = new StringBuilder();
		for (int tCX = (aCenter.getX() - tR) >> 4; tCX <= (aCenter.getX() + tR) >> 4; tCX++)
		for (int tCZ = (aCenter.getZ() - tR) >> 4; tCZ <= (aCenter.getZ() + tR) >> 4; tCZ++) {
			net.minecraft.world.level.chunk.ChunkAccess tCA = aLevel.getChunk(tCX, tCZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
			if (!(tCA instanceof net.minecraft.world.level.chunk.LevelChunk tChunk)) {tMissChunks++; continue;}
			java.util.Map<net.minecraft.core.BlockPos, net.minecraft.world.level.block.entity.BlockEntity> tBEs = tChunk.getBlockEntities();
			net.minecraft.world.level.chunk.LevelChunkSection[] tSecs = tChunk.getSections();
			for (int tSI = 0; tSI < tSecs.length; tSI++) {
				if (tSecs[tSI].hasOnlyAir()) continue;
				int tSY = tChunk.getSectionYFromSectionIndex(tSI) << 4;
				for (int tY = 0; tY < 16; tY++) for (int tZ = 0; tZ < 16; tZ++) for (int tX = 0; tX < 16; tX++) {
					net.minecraft.world.level.block.state.BlockState tState = tSecs[tSI].getBlockState(tX, tY, tZ);
					if (!(tState.getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) continue;
					tBlocks++;
					net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos((tCX << 4) + tX, tSY + tY, (tCZ << 4) + tZ);
					String tName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(tState.getBlock()).getPath();
					String tShort = tName.length() > 30 ? tName.substring(24) : tName; // gt.block.multitileentity.XXX -> XXX...
					int[] tCnt = tPerBlock.computeIfAbsent(tShort, k -> new int[4]);
					tCnt[0]++;
					net.minecraft.world.level.block.entity.BlockEntity tBE = tBEs.get(tPos);
					if (tBE == null) {
						tNull++; tCnt[1]++;
						if (tSampleN++ < 6) tSamples.append("[GT6-MTEAUDIT] ").append(aLabel).append(" SAMPLE BE=NULL @").append(tPos.toShortString()).append(" блок=").append(tShort).append('\n');
					} else if (tBE instanceof gregapi.tileentity.base.TileEntityLoaderStub tStubBE) {
						tStub++; tCnt[2]++;
						if (tSampleS++ < 6) {
							net.minecraft.nbt.CompoundTag tNBT = tStubBE.mLoadedNBT;
							tSamples.append("[GT6-MTEAUDIT] ").append(aLabel).append(" SAMPLE BE=STUB @").append(tPos.toShortString()).append(" блок=").append(tShort)
								.append(tNBT == null ? " mLoadedNBT=null" : " ключи=" + tNBT.keySet() + " reg=" + tNBT.getShort(NBT_MTE_REG).orElse((short)-1) + " id=" + tNBT.getShort(NBT_MTE_ID).orElse((short)-1)).append('\n');
						}
					} else if (tBE instanceof gregapi.block.multitileentity.IMultiTileEntity tMTE) {
						tReal++; tCnt[3]++;
						if (tSampleR++ < 4) tSamples.append("[GT6-MTEAUDIT] ").append(aLabel).append(" SAMPLE BE=REAL @").append(tPos.toShortString()).append(' ').append(tBE.getClass().getSimpleName()).append(" reg=").append(tMTE.getMultiTileEntityRegistryID()).append(" id=").append(tMTE.getMultiTileEntityID()).append('\n');
					} else tOther++;
				}
			}
		}
		O.println("[GT6-MTEAUDIT] " + aLabel + " центр=" + aCenter.toShortString() + " r=" + tR + " чанков-мимо=" + tMissChunks);
		O.println("[GT6-MTEAUDIT] " + aLabel + " ИТОГ: MTE-блоков=" + tBlocks + " BE: real=" + tReal + " stub=" + tStub + " NULL=" + tNull + " other=" + tOther);
		int tLines = 0;
		for (java.util.Map.Entry<String, int[]> tE : tPerBlock.entrySet()) {
			if (tLines++ >= 14) {O.println("[GT6-MTEAUDIT]   ... (ещё " + (tPerBlock.size() - 14) + " типов блоков)"); break;}
			int[] tC = tE.getValue();
			O.println("[GT6-MTEAUDIT]   " + tE.getKey() + ": блоков=" + tC[0] + " (NULL=" + tC[1] + " stub=" + tC[2] + " real=" + tC[3] + ")");
		}
		O.print(tSamples);
		return new int[]{tBlocks, tReal, tStub, tNull, tOther};
	}

	// ========== [GT6-WIREPROBE] ВРЕМЕННАЯ проба «Связка №1 — электрические провода EU» (Ф3.1, гейт run/gt6wireprobe.flag + -Pgt6probes) ==========
	// МИГРИРОВАНА на каркас gregapi.probe.GT6ProbeStand (это его приёмка: те же 4 кейса и те же числа, что
	// прогон до миграции — эталон в STATE/коммите e66850f4). Судимый канал прежний и ПОЛНОСТЬЮ реальный:
	// engine тикает BatteryBox.onTick2 (getTicker) -> ITileEntityEnergy.Util.emitEnergyToNetwork ->
	// WireElectric.doEnergyInjection -> transferElectricity -> addToEnergyTransferred -> BatBox.doInject;
	// из пробы ни один из этих методов не вызывается — только реальные тики. Сетап-поля источников
	// (mEnergy/mBatteryCount/mOutput/mStopped) выставляются напрямую КАЖДЫЙ тик ДО реального тика (Pre-фаза) —
	// обход ТОЛЬКО инвентарной бухгалтерии батарей в слотах, не передачи энергии (манифест §4 «дать как
	// скрафченный»). Три линии со свежими позициями: NORM (32В/1A), OVERVOLT (форс 64В > mVoltage),
	// OVERAMP (форс mBatteryCount=2 > mAmperage). Снять при уборке фазы.
	private static final int WIRE_L = 6;
	private static final int WIRE_ID = 28050;   // 1x Tin Wire — Loader_MultiTileEntities.java:1918 (V=32, A=1, loss=2)
	private static final int BATBOX_ID = 10081; // Battery Box (LV) — Loader_MultiTileEntities.java:895 (in=out=32)
	private static final String WIRE_M = "GT6-WIREPROBE";
	private static int sWireProbeTick = -1;
	private static gregapi.probe.GT6ProbeStand.Seq sWireSeq;
	private static net.minecraft.server.level.ServerPlayer sWirePlayer;
	private static gregapi.tileentity.energy.TileEntityBase10EnergyBatBox sWireSrcNorm, sWireSinkNorm, sWireSrcOver, sWireSinkOver, sWireSrcAmp, sWireSinkAmp;
	private static gregapi.tileentity.connectors.MultiTileEntityWireElectric[] sWireChainNorm, sWireChainOver, sWireChainAmp;
	private static net.minecraft.core.BlockPos[] sWireChainOverPos, sWireChainAmpPos;
	private static long sWireNormE0 = -1;
	private static long sWireMLoss, sWireMVoltage, sWireMAmperage; // прочитано из живого BE, НЕ предположено

	/** Одна линия стенда на каркасе: анкер -> BatBox(источник) -> WIRE_L проводов -> BatBox(приёмник).
	 *  Постройка/анкер/свежие стеки/верификация классов — каркас {@link gregapi.probe.GT6ProbeStand#place}/
	 *  {@link gregapi.probe.GT6ProbeStand#line}; здесь остаётся только СХЕМА и топология (facing/connect). */
	private static Object[] gt6WireProbeRow(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aAnchor,
			net.minecraft.core.BlockPos[] aPosOut) {
		net.minecraft.core.Direction tEast = net.minecraft.core.Direction.EAST;
		gregapi.probe.GT6ProbeStand.solidPad(aLevel, aAnchor.below(), WIRE_L + 3, 1); // пол — гигиена, не судимый канал
		gregapi.tileentity.energy.TileEntityBase10EnergyBatBox tSrc = gregapi.probe.GT6ProbeStand.place(
			aLevel, sWirePlayer, aAnchor, tEast, gregapi.probe.GT6ProbeStand.mteStack(BATBOX_ID),
			gregapi.tileentity.energy.TileEntityBase10EnergyBatBox.class, WIRE_M, "источник");
		if (tSrc == null) return new Object[]{null, null, null};
		tSrc.setPrimaryFacing(SIDE_EAST); // эмиссия — на восток, в линию
		net.minecraft.core.BlockPos tSrcPos = aAnchor.relative(tEast);
		gregapi.tileentity.connectors.MultiTileEntityWireElectric[] tChain = gregapi.probe.GT6ProbeStand.line(
			aLevel, sWirePlayer, tSrcPos, tEast, WIRE_L, WIRE_ID,
			gregapi.tileentity.connectors.MultiTileEntityWireElectric.class, WIRE_M);
		if (tChain[WIRE_L-1] == null) return new Object[]{tSrc, null, tChain};
		if (aPosOut != null) for (int i = 0; i < WIRE_L; i++) aPosOut[i] = tSrcPos.relative(tEast, i + 1);
		gregapi.tileentity.energy.TileEntityBase10EnergyBatBox tSink = gregapi.probe.GT6ProbeStand.place(
			aLevel, sWirePlayer, tSrcPos.relative(tEast, WIRE_L), tEast, gregapi.probe.GT6ProbeStand.mteStack(BATBOX_ID),
			gregapi.tileentity.energy.TileEntityBase10EnergyBatBox.class, WIRE_M, "приёмник");
		if (tSink == null) return new Object[]{tSrc, null, tChain};
		tSink.setPrimaryFacing(SIDE_EAST); // isInput = aSide != mFacing -> принимает с запада (от провода)
		// принудительная связность концов реальным API connect() (как гайковёрт) — топология, не обход передачи
		tChain[0].connect(SIDE_WEST, T);
		tChain[WIRE_L-1].connect(SIDE_EAST, T);
		return new Object[]{tSrc, tSink, tChain};
	}

	/** Сетап-поля КАЖДЫЙ тик ДО реального onTick2 (наш диспатч — Pre-фаза): только «батареи вставлены», не передача. */
	private static void gt6WireProbeApplyFields() {
		if (sWireSinkAmp == null) return;
		sWireSrcNorm.mEnergy = 1_000_000_000L; sWireSrcNorm.mBatteryCount = 1; sWireSrcNorm.mChargeableCount = 0; sWireSrcNorm.mStopped = F; sWireSrcNorm.mMode = 0;
		sWireSinkNorm.mChargeableCount = 1000; sWireSinkNorm.mBatteryCount = 0; sWireSinkNorm.mStopped = F;
		sWireSrcOver.mEnergy = 1_000_000_000L; sWireSrcOver.mOutput = 64; sWireSrcOver.mBatteryCount = 1; sWireSrcOver.mChargeableCount = 0; sWireSrcOver.mStopped = F; sWireSrcOver.mMode = 0;
		sWireSinkOver.mChargeableCount = 1000; sWireSinkOver.mBatteryCount = 0; sWireSinkOver.mStopped = F;
		sWireSrcAmp.mEnergy = 1_000_000_000L; sWireSrcAmp.mBatteryCount = 2; sWireSrcAmp.mChargeableCount = 0; sWireSrcAmp.mStopped = F; sWireSrcAmp.mMode = 0;
		sWireSinkAmp.mChargeableCount = 1000; sWireSinkAmp.mBatteryCount = 0; sWireSinkAmp.mStopped = F;
	}

	private static void gt6WireProbePrintBurn(String aLabel, net.minecraft.server.level.ServerLevel aLevel, gregapi.tileentity.connectors.MultiTileEntityWireElectric[] aChain, net.minecraft.core.BlockPos[] aPos) {
		StringBuilder tLine = new StringBuilder();
		for (int i = 0; i < aChain.length; i++) tLine.append(aLevel.getBlockState(aPos[i]).is(Blocks.FIRE) ? "FIRE" : String.valueOf(aChain[i].mBurnCounter)).append(' ');
		gregapi.data.CS.OUT.println("[" + WIRE_M + "] " + aLabel + " тик " + sWireProbeTick + " mBurnCounter/FIRE по проводам: " + tLine);
	}

	/** Тик 200: постройка трёх линий + чтение живых параметров провода. Любой обрыв -> RuntimeException -> Seq печатает EXC. */
	private static void gt6WireProbeBuild() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = sWirePlayer.level();
		O.println("========== [" + WIRE_M + "] Связка №1 — электрические провода EU (Ф3.1, на каркасе GT6ProbeStand) ==========");
		MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		if (tReg == null || tReg.getClassContainer(WIRE_ID) == null || tReg.getClassContainer(BATBOX_ID) == null) throw new RuntimeException("реестр/ID не найдены (провод=" + WIRE_ID + " batbox=" + BATBOX_ID + ")");
		O.println("[" + WIRE_M + "] ID подтверждены: провод=" + tReg.getClassContainer(WIRE_ID).mClass.getSimpleName() + "(" + WIRE_ID + ") batbox=" + tReg.getClassContainer(BATBOX_ID).mClass.getSimpleName() + "(" + BATBOX_ID + ")");
		net.minecraft.core.BlockPos tBase = sWirePlayer.blockPosition().offset(4, 0, 4);
		sWireChainOverPos = new net.minecraft.core.BlockPos[WIRE_L]; sWireChainAmpPos = new net.minecraft.core.BlockPos[WIRE_L];
		Object[] tNorm = gt6WireProbeRow(tLevel, tBase,                 null);
		Object[] tOver = gt6WireProbeRow(tLevel, tBase.offset(0, 0, 3), sWireChainOverPos);
		Object[] tAmp  = gt6WireProbeRow(tLevel, tBase.offset(0, 0, 6), sWireChainAmpPos);
		sWireSrcNorm = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tNorm[0]; sWireSinkNorm = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tNorm[1]; sWireChainNorm = (gregapi.tileentity.connectors.MultiTileEntityWireElectric[]) tNorm[2];
		sWireSrcOver = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tOver[0]; sWireSinkOver = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tOver[1]; sWireChainOver = (gregapi.tileentity.connectors.MultiTileEntityWireElectric[]) tOver[2];
		sWireSrcAmp  = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tAmp [0]; sWireSinkAmp  = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tAmp [1]; sWireChainAmp  = (gregapi.tileentity.connectors.MultiTileEntityWireElectric[]) tAmp [2];
		if (sWireSinkNorm == null || sWireSinkOver == null || sWireSinkAmp == null) {sWireSinkAmp = null; throw new RuntimeException("постройка линии не удалась (null в цепочке)");}
		sWireMLoss = sWireChainNorm[0].mLoss; sWireMVoltage = sWireChainNorm[0].mVoltage; sWireMAmperage = sWireChainNorm[0].mAmperage;
		O.println("[" + WIRE_M + "] живые параметры провода (из BE, не предположены): mLoss=" + sWireMLoss + " mVoltage=" + sWireMVoltage + " mAmperage=" + sWireMAmperage);
		O.println("[" + WIRE_M + "] NORM: src.mOutput=" + sWireSrcNorm.mOutput + " src.mFacing=" + sWireSrcNorm.mFacing + " sink.mFacing=" + sWireSinkNorm.mFacing);
		O.println("[" + WIRE_M + "] OVERVOLT: src.mOutput(будет форсирован)=64 (> mVoltage=" + sWireMVoltage + " провода)");
		O.println("[" + WIRE_M + "] OVERAMP: src.mOutput=32, mBatteryCount(будет форсирован)=2 (> mAmperage=" + sWireMAmperage + " провода)");
		gt6WireProbeApplyFields(); // применить в ЭТОМ же тике — реальный onTick2 источников идёт после Pre-хука
	}

	/** Тик 280: все 4 судьи (формат вердиктов — каркас, числа и подписи 1:1 с прогоном до миграции). */
	private static void gt6WireProbeJudge() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = sWirePlayer.level();
		long tDeltaTicks = 280 - 220;
		long tExpectedVoltageAtSink = sWireSrcNorm.mOutput - sWireMLoss * WIRE_L;
		long tExpectedDelta = tExpectedVoltageAtSink * 1 * tDeltaTicks;
		long tActualDelta = sWireSinkNorm.mEnergy - sWireNormE0;
		O.println("[" + WIRE_M + "] ===== КЕЙС 1 NORM =====");
		O.println("[" + WIRE_M + "] NORM E1 (тик 280): sink.mEnergy=" + sWireSinkNorm.mEnergy + "; дельта за " + tDeltaTicks + " тиков=" + tActualDelta + " (ожидание=(" + sWireSrcNorm.mOutput + "-" + sWireMLoss + "×" + WIRE_L + ")×1×" + tDeltaTicks + "=" + tExpectedDelta + ")");
		sWireSeq.judge("NORM дельта энергии", tActualDelta == tExpectedDelta, tExpectedDelta, tActualDelta);
		long tNormBurnSum = 0; StringBuilder tBurnLine = new StringBuilder();
		for (int i = 0; i < WIRE_L; i++) {tBurnLine.append(sWireChainNorm[i].mBurnCounter).append(' '); tNormBurnSum += sWireChainNorm[i].mBurnCounter;}
		O.println("[" + WIRE_M + "] NORM mBurnCounter по проводам: " + tBurnLine + "(ожидание все 0)");
		sWireSeq.judge("NORM burn", tNormBurnSum == 0, 0, tNormBurnSum);
		long tActualWattageLast = sWireChainNorm[WIRE_L-1].mWattageLast;
		O.println("[" + WIRE_M + "] NORM провод[последний].mWattageLast=" + tActualWattageLast + " (ожидание=напряжение-после-потерь×амперы=" + tExpectedVoltageAtSink + ")");
		sWireSeq.judge("NORM mWattageLast", tActualWattageLast == tExpectedVoltageAtSink, tExpectedVoltageAtSink, tActualWattageLast);

		O.println("[" + WIRE_M + "] ===== КЕЙС 2 OVERVOLT =====");
		gt6WireProbePrintBurn("OVERVOLT состояние проводов (тик 280):", tLevel, sWireChainOver, sWireChainOverPos);
		sWireSeq.judge("OVERVOLT возгорание (mBurnCounter>=16 либо FIRE)", gt6WireProbeBurned(tLevel, sWireChainOver, sWireChainOverPos), "перегорел/загорелся", "цел");

		O.println("[" + WIRE_M + "] ===== КЕЙС 3 OVERAMP =====");
		gt6WireProbePrintBurn("OVERAMP состояние проводов (тик 280):", tLevel, sWireChainAmp, sWireChainAmpPos);
		sWireSeq.judge("OVERAMP возгорание (mBurnCounter>=16 либо FIRE)", gt6WireProbeBurned(tLevel, sWireChainAmp, sWireChainAmpPos), "перегорел/загорелся", "цел");

		O.println("[" + WIRE_M + "] ===== КЕЙС 4 CONTROL-NEG =====");
		long tNormBurnSum2 = 0;
		for (int i = 0; i < WIRE_L; i++) tNormBurnSum2 += sWireChainNorm[i].mBurnCounter;
		O.println("[" + WIRE_M + "] CONTROL-NEG (линия NORM после того, как OVERVOLT/OVERAMP отгорели): сумма mBurnCounter=" + tNormBurnSum2 + " (ожидание 0 — перелив возгорания на соседнюю линию не должен случиться)");
		sWireSeq.judge("CONTROL-NEG", tNormBurnSum2 == 0, 0, tNormBurnSum2);
		sWireSeq.done();
	}

	private static boolean gt6WireProbeBurned(net.minecraft.server.level.ServerLevel aLevel, gregapi.tileentity.connectors.MultiTileEntityWireElectric[] aChain, net.minecraft.core.BlockPos[] aPos) {
		for (int i = 0; i < WIRE_L; i++) if (aLevel.getBlockState(aPos[i]).is(Blocks.FIRE) || aChain[i].mBurnCounter >= 16) return T;
		return F;
	}

	public static void gt6WireProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sWireProbeTick++;
		if (aServer.getPlayerList().getPlayers().isEmpty()) return;
		sWirePlayer = aServer.getPlayerList().getPlayers().get(0);
		if (sWireSeq == null) {
			sWireSeq = new gregapi.probe.GT6ProbeStand.Seq(WIRE_M)
				.at(200, GT_API_Proxy::gt6WireProbeBuild)
				.at(220, () -> {sWireNormE0 = sWireSinkNorm.mEnergy; gregapi.data.CS.OUT.println("[" + WIRE_M + "] NORM E0 (тик 220): sink.mEnergy=" + sWireNormE0);})
				.at(280, GT_API_Proxy::gt6WireProbeJudge);
			for (int t = 210; t <= 270; t += 10) sWireSeq.at(t, () -> {
				net.minecraft.server.level.ServerLevel tLevel = sWirePlayer.level();
				gt6WireProbePrintBurn("OVERVOLT", tLevel, sWireChainOver, sWireChainOverPos);
				gt6WireProbePrintBurn("OVERAMP",  tLevel, sWireChainAmp,  sWireChainAmpPos);
			});
		}
		gt6WireProbeApplyFields(); // сетап КАЖДЫЙ тик ДО реального тика
		sWireSeq.tick(sWireProbeTick);
	}

	// ========== [GT6-FLUIDPIPEPROBE] ВРЕМЕННАЯ проба «Связка №2 — жидкостные трубы» (Ф3.1, гейт run/gt6fluidpipeprobe.flag + -Pgt6probes) ==========
	// Верификационный стенд: судит ВНУТРЕННИЕ процессы трубы (mTemperature/mTransferredAmount/утечки/перегрев) против
	// семантики оригинала MultiTileEntityPipeFluid.onServerTickPre/distribute (1.7.10, перенесено дословно — сверено
	// построчно, расхождений в control-flow НЕТ, только engine-swap типов + уже принятые ADR: BUG-025 cauldron-split,
	// F5 IFluidHandler.fill(FluidStack,FluidAction) sideless-shim, F5 getTankInfo->getTanks()>0). Источник/приёмник —
	// реальные MTE «Bronze Drum» (gregapi.tileentity.tank.TileEntityBase08Barrel, Loader_MultiTileEntities.java:2155):
	// их внутренние поля mTank/mMode выставляются НАПРЯМУЮ как СЕТАП (аналог «дать инструмент как скрафченный»,
	// LIVE-PROBE-MANUAL.md §4) — это обходит ТОЛЬКО инвентарную бухгалтерию бочки (реально игрок наливал бы бочку
	// ведром/краном), НЕ судимый канал. Судимый канал остаётся ПОЛНОСТЬЮ реальным: бочка эмитит жидкость через
	// РЕАЛЬНЫЙ TileEntityBase08Barrel.onTick2 (mMode бит0 — тот же бит, что переключает TOOL_wrench :137-143) ->
	// FL.move(mTank, getAdjacentTank(tSide)) -> труба.fill(...) -> труба сама переносит дальше через РЕАЛЬНЫЙ
	// getTicker/onServerTickPre -> distribute (:255-338, :340-436 оригинала) — ни один из ЭТИХ методов не вызывается
	// пробой напрямую, только реальные тики решают.
	// ВАЖНО (диф против первого предположения горизонтальной линии): TileEntityBase08Barrel.onTick2 — при бите0
	// эмиссия идёт СТРОГО ПО ГРАВИТАЦИИ (aFluid газ -> ALL_SIDES_VERTICAL {UP,DOWN}; легче воды -> ALL_SIDES_TOP;
	// иначе (вода/лава) -> ALL_SIDES_BOTTOM {DOWN} — TileEntityBase08Barrel.java:215-219), НЕ горизонтально — поэтому
	// линии здесь ВЕРТИКАЛЬНЫЕ (снизу вверх: анкер-STONE, приёмник-бочка, FP_L труб, источник-бочка НАВЕРХУ), в отличие
	// от горизонтальных линий WIREPROBE (провод не завязан на гравитацию).
	// 4 линии (свежие позиции, разнесены по Z на 5 блоков — упреждает fire-spread между деревянными GAS/HOT трубами):
	// NORM (Cu-труба id26102, gasProof=T — вода, чистый перенос), GAS (Wood-труба id26002, gasProof=F — природный газ,
	// ожидается утечка FL.gas :296), GAS-CONTROL (Cu-труба gasProof=T, тот же газ — утечки быть не должно),
	// HOT (Wood-труба id26002, mMaxTemperature=340K — лава, ожидается перегрев :320-327).
	// ACID (поправка игрока 2026-07-25 — прежний SKIP был ЛОЖНЫМ ГРЕПОМ: статический список FluidsGT.ACID это чужие
	// моды, но FL.java:1312 (ориг. :1118) ДИНАМИЧЕСКИ добавляет туда жидкость КАЖДОГО материала с TD.Properties.ACID
	// при её создании — H2SO4/HNO3/HCl/AquaRegia суть СОБСТВЕННЫЕ кислоты Грега): колонна Cu-труб (mAcidProof=F) БЕЗ
	// бочек — серная кислота заливается прямо в трубу[0], дальше только реальный onServerTickPre: разъедание
	// GarbageGT.trash(tTank,16) по 16mb/тик + 1%/тик setToAir (ориг. :307-317) => детерминированный исход «кислота
	// в системе = 0». Контроль — само-обнаруженная в реестре acidProof-труба (скан канонических TE 26000..26399):
	// кислота в ней НЕ убывает; нет такой трубы в реестре => честный SKIP контроля с печатью. Снять при уборке фазы.
	private static int sFPProbeTick = -1;
	private static final int FP_L = 6;
	private static final int PIPE_NORM_ID = 26102; // Medium Copper Fluid Pipe (gasProof=T,acidProof=F) — Loader_MultiTileEntities.java:1855 (aID=26100 +2=medium)
	private static final int PIPE_WOOD_ID = 26002; // Medium Wood   Fluid Pipe (gasProof=F,acidProof=F,maxTemp=340) — Loader_MultiTileEntities.java:1850 (aID=26000 +2=medium)
	private static final int BARREL_ID    = 32102; // Bronze Drum (gasProof=T,acidProof=F,capacity=64000) — Loader_MultiTileEntities.java:2155
	private static final long FP_WATER = 4000, FP_GAS = 3000, FP_LAVA = 500, FP_ACID = 2000;
	private static boolean sFPSetupOk = F;
	private static gregapi.tileentity.tank.TileEntityBase08Barrel sFPSrcNorm, sFPSinkNorm, sFPSrcGas, sFPSinkGas, sFPSrcGasCtl, sFPSinkGasCtl, sFPSrcHot, sFPSinkHot;
	private static final gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] sFPNorm   = new gregapi.tileentity.connectors.MultiTileEntityPipeFluid[FP_L];
	private static final gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] sFPGas    = new gregapi.tileentity.connectors.MultiTileEntityPipeFluid[FP_L];
	private static final gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] sFPGasCtl = new gregapi.tileentity.connectors.MultiTileEntityPipeFluid[FP_L];
	private static final gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] sFPHot    = new gregapi.tileentity.connectors.MultiTileEntityPipeFluid[FP_L];
	private static final net.minecraft.core.BlockPos[] sFPHotPos = new net.minecraft.core.BlockPos[FP_L];
	// [GT6-FLUIDPIPEPROBE] ACID-кейс (поправка игрока): колонны строятся каркасом GT6ProbeStand.line
	private static gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] sFPAcid, sFPAcidCtl; // ctl==null => контроль SKIP
	private static net.minecraft.core.BlockPos[] sFPAcidPos;
	private static int sFPAcidCtlId = -1, sFPAcidZeroTick = -1;
	private static boolean sFPAcidClassOk = F, sFPAcidWaterNegOk = F, sFPAcidEverAir = F;
	private static long sFPCapNorm, sFPMaxTempNorm, sFPCapWood, sFPMaxTempWood; // живые параметры труб (прочитаны из BE, НЕ предположены)
	private static long sFPTotal0Norm, sFPTotal0Gas, sFPTotal0GasCtl; // начальная консервация по цепи
	private static long sFPAccumTransferredNorm = 0;
	// HOT: FIRE — эффект setOnFire() кратковременный (WD.burn ставит блок FIRE, ванильный scheduled-tick FireBlock
	// гасит его через рандомный интервал, если не может выжить — плавающая колонна в воздухе без опоры/горючего
	// соседа) и НЕ гарантированно виден в ЕДИНСТВЕННОМ замере на конкретном тике (урок §7 манифеста «один прогон при
	// недетерминизме») — копим «видели ли хоть раз» по ВСЕМУ окну, не только в конце.
	private static boolean sFPHotEverSelfFire = F, sFPHotEverNeighborFire = F;
	private static int sFPHotFireTicksSeen = 0;

	/** Установка одного MTE-блока реальным каналом игрока (шаблон gt6storprobe/gt6wireprobe: item.useOn(UseOnContext)). */
	private static net.minecraft.world.level.block.entity.BlockEntity gt6FluidPipeProbePlace(net.minecraft.server.level.ServerPlayer aPlayer, net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aClickedPos, net.minecraft.core.Direction aFace, net.minecraft.world.item.ItemStack aItem) {
		aPlayer.getInventory().setItem(0, aItem); aPlayer.getInventory().setSelectedSlot(0);
		net.minecraft.world.phys.Vec3 tHit = net.minecraft.world.phys.Vec3.atCenterOf(aClickedPos).add(aFace.getStepX()*0.5, aFace.getStepY()*0.5, aFace.getStepZ()*0.5);
		aPlayer.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(aPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(tHit, aFace, aClickedPos, false)));
		return aLevel.getBlockEntity(aClickedPos.relative(aFace));
	}

	/** Строит одну ВЕРТИКАЛЬНУЮ колонну снизу вверх: анкер(STONE) -> приёмник(бочка) -> FP_L труб -> источник(бочка).
	 *  Обязательно вертикально — TileEntityBase08Barrel.onTick2 бит0 эмитирует НЕгазовую жидкость только ВНИЗ
	 *  (ALL_SIDES_BOTTOM), см. комментарий блока выше. aChainPosOut может быть null (позиции не нужны, линия не горит). */
	private static Object[] gt6FluidPipeProbeBuildColumn(net.minecraft.server.level.ServerPlayer aPlayer, net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aBase,
			net.minecraft.world.item.ItemStack aSrcItem, net.minecraft.world.item.ItemStack aSinkItem, net.minecraft.world.item.ItemStack aPipeItem,
			gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] aChainOut, net.minecraft.core.BlockPos[] aChainPosOut) {
		net.minecraft.core.Direction tUp = net.minecraft.core.Direction.UP;
		// расчистка колонны: aBase (i=0) — STONE (клик-цель для установки приёмника на i=1), i=1..FP_L+2 (приёмник+трубы+источник) — AIR.
		for (int i = 0; i <= FP_L + 2; i++) aLevel.setBlock(aBase.above(i), i == 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.world.level.block.entity.BlockEntity tSinkBE = gt6FluidPipeProbePlace(aPlayer, aLevel, aBase, tUp, aSinkItem);
		if (!(tSinkBE instanceof gregapi.tileentity.tank.TileEntityBase08Barrel tSink)) {
			O.println("[GT6-FLUIDPIPEPROBE] DIAG приёмник не встал @" + aBase.above() + " BE=" + (tSinkBE == null ? "null" : tSinkBE.getClass().getSimpleName()) + " блок=" + aLevel.getBlockState(aBase.above()).getBlock());
			return new Object[]{null, null};
		}
		net.minecraft.core.BlockPos tCursor = aBase.above();
		for (int i = 0; i < FP_L; i++) {
			net.minecraft.world.level.block.entity.BlockEntity tPipeBE = gt6FluidPipeProbePlace(aPlayer, aLevel, tCursor, tUp, aPipeItem);
			if (!(tPipeBE instanceof gregapi.tileentity.connectors.MultiTileEntityPipeFluid tPipe)) {
				O.println("[GT6-FLUIDPIPEPROBE] DIAG труба[" + i + "] не встала @" + tCursor.above() + " BE=" + (tPipeBE == null ? "null" : tPipeBE.getClass().getSimpleName()) + " блок=" + aLevel.getBlockState(tCursor.above()).getBlock() + " стек-остаток=" + aPipeItem.getCount());
				return new Object[]{null, tSink};
			}
			aChainOut[i] = tPipe;
			tCursor = tCursor.above();
			if (aChainPosOut != null) aChainPosOut[i] = tCursor;
		}
		net.minecraft.world.level.block.entity.BlockEntity tSrcBE = gt6FluidPipeProbePlace(aPlayer, aLevel, tCursor, tUp, aSrcItem);
		if (!(tSrcBE instanceof gregapi.tileentity.tank.TileEntityBase08Barrel tSrc)) {
			O.println("[GT6-FLUIDPIPEPROBE] DIAG источник не встал @" + tCursor.above() + " BE=" + (tSrcBE == null ? "null" : tSrcBE.getClass().getSimpleName()) + " блок=" + aLevel.getBlockState(tCursor.above()).getBlock());
			return new Object[]{null, tSink};
		}
		// принудительная связность обоих концов реальным API connect() (тем же методом, что дёргает гайковёрт/авто-разводка) — сеттинг топологии, НЕ обход переливания
		aChainOut[0].connect(SIDE_DOWN, T);
		aChainOut[FP_L-1].connect(SIDE_UP, T);
		return new Object[]{tSrc, tSink};
	}

	/** Сумма mb по всей цепи (обе бочки + все mTanks труб) — судья консервации (в). */
	private static long gt6FluidPipeProbeSum(gregapi.tileentity.tank.TileEntityBase08Barrel aSrc, gregapi.tileentity.tank.TileEntityBase08Barrel aSink, gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] aChain) {
		long rSum = aSrc.mTank.amount() + aSink.mTank.amount();
		for (gregapi.tileentity.connectors.MultiTileEntityPipeFluid tPipe : aChain) for (gregapi.fluid.FluidTankGT tTank : tPipe.mTanks) rSum += tTank.amount();
		return rSum;
	}

	public static void gt6FluidPipeProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sFPProbeTick++;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		try {
			if (aServer.getPlayerList().getPlayers().isEmpty()) return;
			net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
			net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();

			if (sFPProbeTick == 200) {
				O.println("========== [GT6-FLUIDPIPEPROBE] Связка №2 — жидкостные трубы (Ф3.1) ==========");
				MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
				if (tReg == null || tReg.getClassContainer(PIPE_NORM_ID) == null || tReg.getClassContainer(PIPE_WOOD_ID) == null || tReg.getClassContainer(BARREL_ID) == null) {
					O.println("[GT6-FLUIDPIPEPROBE] EXC: реестр/ID не найдены (Cu-труба=" + PIPE_NORM_ID + " Wood-труба=" + PIPE_WOOD_ID + " бочка=" + BARREL_ID + ") => FAIL"); sFPProbeTick = 999999; return;
				}
				O.println("[GT6-FLUIDPIPEPROBE] ID подтверждены: Cu-труба=" + tReg.getClassContainer(PIPE_NORM_ID).mClass.getSimpleName() + "(" + PIPE_NORM_ID + ") Wood-труба=" + tReg.getClassContainer(PIPE_WOOD_ID).mClass.getSimpleName() + "(" + PIPE_WOOD_ID + ") бочка=" + tReg.getClassContainer(BARREL_ID).mClass.getSimpleName() + "(" + BARREL_ID + ")");

				net.minecraft.core.BlockPos tBase = tPlayer.blockPosition().offset(4, -1, 4);

				Object[] tRowNorm = gt6FluidPipeProbeBuildColumn(tPlayer, tLevel, tBase,                tReg.getItem(BARREL_ID), tReg.getItem(BARREL_ID), tReg.getItem(PIPE_NORM_ID, FP_L + 2), sFPNorm,   null);
				sFPSrcNorm  = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowNorm[0]; sFPSinkNorm  = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowNorm[1];

				Object[] tRowGas = gt6FluidPipeProbeBuildColumn(tPlayer, tLevel, tBase.offset(0, 0, 5),  tReg.getItem(BARREL_ID), tReg.getItem(BARREL_ID), tReg.getItem(PIPE_WOOD_ID, FP_L + 2), sFPGas,    null);
				sFPSrcGas   = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowGas[0];  sFPSinkGas   = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowGas[1];

				Object[] tRowGasCtl = gt6FluidPipeProbeBuildColumn(tPlayer, tLevel, tBase.offset(0, 0, 10), tReg.getItem(BARREL_ID), tReg.getItem(BARREL_ID), tReg.getItem(PIPE_NORM_ID, FP_L + 2), sFPGasCtl, null);
				sFPSrcGasCtl = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowGasCtl[0]; sFPSinkGasCtl = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowGasCtl[1];

				Object[] tRowHot = gt6FluidPipeProbeBuildColumn(tPlayer, tLevel, tBase.offset(0, 0, 15), tReg.getItem(BARREL_ID), tReg.getItem(BARREL_ID), tReg.getItem(PIPE_WOOD_ID, FP_L + 2), sFPHot,    sFPHotPos);
				sFPSrcHot   = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowHot[0];  sFPSinkHot   = (gregapi.tileentity.tank.TileEntityBase08Barrel) tRowHot[1];

				if (sFPSrcNorm == null || sFPSinkNorm == null || sFPNorm[FP_L-1] == null
				 || sFPSrcGas == null || sFPSinkGas == null || sFPGas[FP_L-1] == null
				 || sFPSrcGasCtl == null || sFPSinkGasCtl == null || sFPGasCtl[FP_L-1] == null
				 || sFPSrcHot == null || sFPSinkHot == null || sFPHot[FP_L-1] == null) {
					O.println("[GT6-FLUIDPIPEPROBE] EXC: постройка колонны не удалась (null в цепочке) => FAIL"); sFPProbeTick = 999999; return;
				}

				sFPCapNorm = sFPNorm[0].mCapacity; sFPMaxTempNorm = sFPNorm[0].mMaxTemperature;
				sFPCapWood = sFPGas[0].mCapacity;  sFPMaxTempWood = sFPGas[0].mMaxTemperature;
				long tWaterTemp = FL.temperature(FL.Water.make(1));
				long tGasTemp   = FL.temperature(FL.Gas_Natural.make(1));
				long tLavaTemp  = FL.temperature(FL.Lava.make(1));
				O.println("[GT6-FLUIDPIPEPROBE] живые параметры (из BE, не предположены): Cu-труба mCapacity=" + sFPCapNorm + " mMaxTemperature=" + sFPMaxTempNorm + " mGasProof=" + sFPNorm[0].mGasProof + " mAcidProof=" + sFPNorm[0].mAcidProof);
				O.println("[GT6-FLUIDPIPEPROBE] живые параметры: Wood-труба mCapacity=" + sFPCapWood + " mMaxTemperature=" + sFPMaxTempWood + " mGasProof=" + sFPGas[0].mGasProof + " mAcidProof=" + sFPGas[0].mAcidProof);
				O.println("[GT6-FLUIDPIPEPROBE] живая температура жидкостей: вода=" + tWaterTemp + "K природный_газ=" + tGasTemp + "K лава=" + tLavaTemp + "K");
				O.println("[GT6-FLUIDPIPEPROBE] Bronze Drum: capacity=" + sFPSrcNorm.mTank.capacity() + " mGasProof=" + sFPSrcNorm.mGasProof + " mAcidProof=" + sFPSrcNorm.mAcidProof);

				// NORM: вода в источник, авто-выход бит0 (TileEntityBase08Barrel.java:137-143 — тот же бит, что переключает TOOL_wrench)
				sFPSrcNorm.mTank.setFluid(FL.Water.make(FP_WATER)); sFPSrcNorm.mMode |= B[0];
				// GAS / GAS-CONTROL: природный газ, тот же авто-выход (для газа onTick2 шлёт ALL_SIDES_VERTICAL — тоже вниз входит)
				sFPSrcGas.mTank.setFluid(FL.Gas_Natural.make(FP_GAS)); sFPSrcGas.mMode |= B[0];
				sFPSrcGasCtl.mTank.setFluid(FL.Gas_Natural.make(FP_GAS)); sFPSrcGasCtl.mMode |= B[0];
				// HOT: источник НЕ используется как эмиттер (риск преждевременного meltdown бочки от лавы, TileEntityBase08Barrel.java:168) — лава заливается ПРЯМО в трубу[0] на след. тике

				// ACID: колонна Cu-труб БЕЗ бочек (каркас GT6ProbeStand.line; анкер-STONE каркас ставит сам)
				sFPAcidPos = new net.minecraft.core.BlockPos[FP_L];
				sFPAcid = gregapi.probe.GT6ProbeStand.line(tLevel, tPlayer, tBase.offset(0, 0, 20), net.minecraft.core.Direction.UP, FP_L, PIPE_NORM_ID,
					gregapi.tileentity.connectors.MultiTileEntityPipeFluid.class, "GT6-FLUIDPIPEPROBE");
				if (sFPAcid[FP_L-1] == null) {O.println("[GT6-FLUIDPIPEPROBE] EXC: постройка ACID-колонны не удалась => FAIL"); sFPProbeTick = 999999; return;}
				for (int i = 0; i < FP_L; i++) sFPAcidPos[i] = tBase.offset(0, 0, 20).above(i + 1);
				// ACID-CONTROL: само-обнаружение acidProof-трубы сканом канонических TE реестра (не выдумывать ID)
				for (int tId = 26000; tId < 26400 && sFPAcidCtlId < 0; tId++)
					if (tReg.getClassContainer(tId) != null
					 && gregapi.block.multitileentity.MultiTileEntityRegistry.getCanonicalTileEntity("gt.multitileentity", tId) instanceof gregapi.tileentity.connectors.MultiTileEntityPipeFluid tP
					 && tP.mAcidProof) sFPAcidCtlId = tId;
				if (sFPAcidCtlId >= 0) {
					sFPAcidCtl = gregapi.probe.GT6ProbeStand.line(tLevel, tPlayer, tBase.offset(0, 0, 25), net.minecraft.core.Direction.UP, FP_L, sFPAcidCtlId,
						gregapi.tileentity.connectors.MultiTileEntityPipeFluid.class, "GT6-FLUIDPIPEPROBE");
					if (sFPAcidCtl[FP_L-1] == null) sFPAcidCtl = null;
					O.println("[GT6-FLUIDPIPEPROBE] ACID-CONTROL: найдена acidProof-труба id=" + sFPAcidCtlId + (sFPAcidCtl == null ? " (постройка не удалась — контроль SKIP)" : " (" + sFPAcidCtl[0].getClass().getSimpleName() + ", mAcidProof=" + sFPAcidCtl[0].mAcidProof + ")"));
				} else O.println("[GT6-FLUIDPIPEPROBE] ACID-CONTROL: в реестре 26000..26399 не найдено ни одной acidProof-трубы — контроль SKIP (честно)");

				sFPTotal0Norm   = gt6FluidPipeProbeSum(sFPSrcNorm,   sFPSinkNorm,   sFPNorm);
				sFPTotal0Gas    = gt6FluidPipeProbeSum(sFPSrcGas,    sFPSinkGas,    sFPGas);
				sFPTotal0GasCtl = gt6FluidPipeProbeSum(sFPSrcGasCtl, sFPSinkGasCtl, sFPGasCtl);
				O.println("[GT6-FLUIDPIPEPROBE] начальная консервация: NORM=" + sFPTotal0Norm + "mb GAS=" + sFPTotal0Gas + "mb GAS-CONTROL=" + sFPTotal0GasCtl + "mb");

				sFPSetupOk = T;
			} else if (sFPSetupOk) {
				if (sFPProbeTick == 210) {
					// HOT: сетап-заливка лавы НАПРЯМУЮ в трубу[0] линии (аналог «дать инструмент как скрафченный», §4 манифеста) —
					// обходит риск преждевременного meltdown бочки-источника от лавы, судимый канал остаётся полностью реальным:
					// труба реагирует САМА через свой реальный onServerTickPre (оригинал :320-327).
					sFPHot[0].mTanks[0].setFluid(FL.Lava.make(FP_LAVA));
					O.println("[GT6-FLUIDPIPEPROBE] HOT: залито " + FP_LAVA + "mb лавы в трубу[0] линии HOT напрямую (temperature обновится на ближайшем реальном onServerTickPre трубы)");
					// ACID: сетап-заливка серной кислоты Грега (MT.H2SO4, TD.Properties.ACID -> FluidsGT.ACID динамически, FL.java:1312)
					net.neoforged.neoforge.fluids.FluidStack tAcid = gregapi.data.MT.H2SO4.liquid(gregapi.data.CS.U, T);
					tAcid.setAmount((int) FP_ACID);
					sFPAcidClassOk = FL.acid(tAcid);
					sFPAcidWaterNegOk = !FL.acid(FL.Water.make(1));
					sFPAcid[0].mTanks[0].setFluid(tAcid);
					O.println("[GT6-FLUIDPIPEPROBE] ACID: залито " + FP_ACID + "mb '" + FL.name(tAcid, F) + "' в трубу[0] ACID-колонны; FL.acid(H2SO4)=" + sFPAcidClassOk + " FL.acid(вода)=" + !sFPAcidWaterNegOk);
					if (sFPAcidCtl != null) {
						net.neoforged.neoforge.fluids.FluidStack tAcid2 = gregapi.data.MT.H2SO4.liquid(gregapi.data.CS.U, T);
						tAcid2.setAmount((int) FP_ACID);
						sFPAcidCtl[0].mTanks[0].setFluid(tAcid2);
						O.println("[GT6-FLUIDPIPEPROBE] ACID-CONTROL: залито " + FP_ACID + "mb в acidProof-трубу[0] контрольной колонны");
					}
				}
				// накопление СРАЗУ после setup (тик 200, когда auto-output бита0 уже включён нашим сетапом в ТОМ ЖЕ тике) —
				// не с 211: перенос по цепочке может начаться уже на тиках 201-210 (недетерминировано rng-порядком целей
				// в distribute() — сверено 2 прогона, окно 211 давало то +80, то -243 к приросту приёмника; ловилось
				// именно смещённым окном, не багом — расширение окна устраняет false-negative замера).
				if (sFPProbeTick >= 201 && sFPProbeTick <= 900) sFPAccumTransferredNorm += sFPNorm[FP_L-1].mTransferredAmount;
				// ACID: наблюдение каждый тик — момент полного разъедания (сумма=0) + «труба когда-либо стала AIR» (1%/тик setToAir)
				if (sFPProbeTick >= 211 && sFPProbeTick <= 900) {
					long tAcidSum = 0;
					for (gregapi.tileentity.connectors.MultiTileEntityPipeFluid tPipe : sFPAcid) if (tPipe != null) for (gregapi.fluid.FluidTankGT tTank : tPipe.mTanks) tAcidSum += tTank.amount();
					if (tAcidSum == 0 && sFPAcidZeroTick < 0) sFPAcidZeroTick = sFPProbeTick;
					for (net.minecraft.core.BlockPos tPos : sFPAcidPos) if (tLevel.getBlockState(tPos).isAir()) sFPAcidEverAir = T;
				}
				// HOT: непрерывное наблюдение FIRE КАЖДЫЙ тик окна (не разовый замер — эффект setOnFire() кратковременный, §7 манифеста)
				if (sFPProbeTick >= 201 && sFPProbeTick <= 900) {
					boolean tTickSelfFire = F, tTickNeighborFire = F;
					for (int i = 0; i < FP_L; i++) {
						net.minecraft.core.BlockPos tPos = sFPHotPos[i];
						if (tLevel.getBlockState(tPos).is(Blocks.FIRE)) tTickSelfFire = T;
						for (byte tSide : ALL_SIDES_VALID) if (tLevel.getBlockState(tPos.relative(net.minecraft.core.Direction.from3DDataValue(tSide))).is(Blocks.FIRE)) tTickNeighborFire = T;
					}
					if (tTickSelfFire) sFPHotEverSelfFire = T;
					if (tTickNeighborFire) sFPHotEverNeighborFire = T;
					if (tTickSelfFire || tTickNeighborFire) sFPHotFireTicksSeen++;
				}
				if (sFPProbeTick >= 210 && sFPProbeTick % 60 == 0 && sFPProbeTick <= 900) {
					long tNowNorm = gt6FluidPipeProbeSum(sFPSrcNorm, sFPSinkNorm, sFPNorm);
					long tNowGas  = gt6FluidPipeProbeSum(sFPSrcGas,  sFPSinkGas,  sFPGas);
					long tNowGasCtl = gt6FluidPipeProbeSum(sFPSrcGasCtl, sFPSinkGasCtl, sFPGasCtl);
					O.println("[GT6-FLUIDPIPEPROBE] тик " + sFPProbeTick + " консервация NORM=" + tNowNorm + "(ожид." + sFPTotal0Norm + ") GAS=" + tNowGas + " GAS-CONTROL=" + tNowGasCtl + "(ожид." + sFPTotal0GasCtl + ") sink.NORM=" + sFPSinkNorm.mTank.amount() + " HOT труба[0].temp=" + sFPHot[0].mTemperature + "(max=" + sFPHot[0].mMaxTemperature + ") HOT-fire-видели-пока=" + sFPHotFireTicksSeen + "тик(ов) (self=" + sFPHotEverSelfFire + " сосед=" + sFPHotEverNeighborFire + ")");
				}

				if (sFPProbeTick == 900) {
					O.println("[GT6-FLUIDPIPEPROBE] ===== КЕЙС 1 NORM =====");
					long tSinkAmount = sFPSinkNorm.mTank.amount();
					long tTotalNow = gt6FluidPipeProbeSum(sFPSrcNorm, sFPSinkNorm, sFPNorm);
					long tTicks = 900 - 200;
					long tRate = tSinkAmount / tTicks; // средний темп за весь замер (честно помечено — не пиковый)
					O.println("[GT6-FLUIDPIPEPROBE] NORM: приёмник получил=" + tSinkAmount + "mb за " + tTicks + " тиков, средний темп=" + tRate + "mb/t (потолок mCapacity/2=" + (sFPCapNorm/2) + "mb/t)");
					O.println("[GT6-FLUIDPIPEPROBE] NORM (а) жидкость дошла: " + (tSinkAmount > 0 ? "=> PASS" : "=> FAIL (ожидалось >0, получено 0)"));
					O.println("[GT6-FLUIDPIPEPROBE] NORM (б) средний темп<=потолок capacity/2: " + (tRate <= sFPCapNorm/2 ? "=> PASS" : "=> FAIL (ожидалось <=" + (sFPCapNorm/2) + ", получено " + tRate + ")"));
					O.println("[GT6-FLUIDPIPEPROBE] NORM (в) консервация: сейчас=" + tTotalNow + " начально=" + sFPTotal0Norm + " " + (tTotalNow == sFPTotal0Norm ? "=> PASS" : "=> FAIL (ожидалось " + sFPTotal0Norm + ", получено " + tTotalNow + ")"));
					// (г) критерий — ОДНОСТОРОННЕЕ неравенство «накоплено >= перенесено», НЕ равенство и НЕ узкий допуск:
					// каждый ФИЗИЧЕСКИЙ приход жидкости в приёмник учитывается РОВНО один раз в mTransferredAmount
					// трубы[последней] (при окне, покрывающем ВЕСЬ период с 201 — сверено: узкое окно 211 иногда давало
					// накоплено<прирост, т.к. упускало ранний transfer до 211 — false-negative замера, не баг, устранено
					// расширением окна) — значит накопленный transferred НЕ МОЖЕТ быть меньше факта переноса. Избыток
					// («churn») — легитимный «холостой» рециркулирующий трафик труба[последняя]<->труба[предпоследняя]
					// на границе давления (anti-backflow FACE_CONNECTED снимается КАЖДЫЙ тик, оригинал :330, алгоритм
					// «давления» :426-428) — сверено 3 прогона: churn менялся (80, -243→устранено окном, 2457), но ПОСЛЕ
					// расширения окна знак всегда >=0. Разрыв «дюп/потеря» проверяется (в) консервацией отдельно (PASS).
					long tChurn = sFPAccumTransferredNorm - tSinkAmount;
					O.println("[GT6-FLUIDPIPEPROBE] NORM (г) mTransferredAmount трубы[последней] накоплено=" + sFPAccumTransferredNorm + " vs прирост приёмника=" + tSinkAmount + " churn(холостой перелив)=" + tChurn + " " + (tChurn >= 0 ? "=> PASS" : "=> FAIL (ожидалось накоплено>=прирост, получена нехватка " + tChurn + ")"));

					O.println("[GT6-FLUIDPIPEPROBE] ===== КЕЙС 2 GAS =====");
					long tGasNow = gt6FluidPipeProbeSum(sFPSrcGas, sFPSinkGas, sFPGas);
					long tGasLeaked = sFPTotal0Gas - tGasNow;
					O.println("[GT6-FLUIDPIPEPROBE] GAS: начально=" + sFPTotal0Gas + "mb сейчас_в_системе=" + tGasNow + "mb утекло=" + tGasLeaked + "mb (утечка не-gasProof трубы — MultiTileEntityPipeFluid.java:296 GarbageGT.trash(tTank,8), по 8mb за тик пока в НЕ-gasProof трубе есть газ)");
					O.println("[GT6-FLUIDPIPEPROBE] GAS утечка произошла: " + (tGasLeaked > 0 ? "=> PASS" : "=> FAIL (ожидалась утечка >0, получено 0)"));

					long tGasCtlNow = gt6FluidPipeProbeSum(sFPSrcGasCtl, sFPSinkGasCtl, sFPGasCtl);
					O.println("[GT6-FLUIDPIPEPROBE] GAS-CONTROL (gasProof=T труба): начально=" + sFPTotal0GasCtl + " сейчас=" + tGasCtlNow + " " + (tGasCtlNow == sFPTotal0GasCtl ? "=> PASS (утечки нет)" : "=> FAIL (ожидалось " + sFPTotal0GasCtl + ", получено " + tGasCtlNow + ")"));

					O.println("[GT6-FLUIDPIPEPROBE] ===== КЕЙС 3 HOT =====");
					StringBuilder tHotLine = new StringBuilder();
					for (int i = 0; i < FP_L; i++) {
						boolean tSelfFireNow = tLevel.getBlockState(sFPHotPos[i]).is(Blocks.FIRE);
						tHotLine.append(tSelfFireNow ? "FIRE" : String.valueOf(sFPHot[i].mTemperature)).append(' ');
					}
					O.println("[GT6-FLUIDPIPEPROBE] HOT состояние труб (снимок тика 900): " + tHotLine + " (mMaxTemperature=" + sFPMaxTempWood + ")");
					O.println("[GT6-FLUIDPIPEPROBE] HOT: FIRE виден на " + sFPHotFireTicksSeen + " тиках из " + (900-201+1) + " замеренных (self-когда-либо=" + sFPHotEverSelfFire + " сосед-когда-либо=" + sFPHotEverNeighborFire + ") — накоплено НЕПРЕРЫВНЫМ наблюдением каждый тик, не разовым замером (setOnFire()/WD.burn() кратковременно: ванильный scheduled-tick FireBlock гасит плавающий в воздухе огонь без опоры, переставляется на следующем тике заново, пока mTemperature>mMaxTemperature — TileEntityBase01Root.java:1021)");
					O.println("[GT6-FLUIDPIPEPROBE] HOT реакция (setOnFire=поджиг соседа каждый тик, пока mTemperature>mMaxTemperature, ИЛИ setToFire=разрушение трубы MultiTileEntityPipeFluid.java:320-326 rng(100)==0 1%/тик): " + ((sFPHotEverSelfFire || sFPHotEverNeighborFire) ? "=> PASS (перегрев/поджиг сработал)" : "=> FAIL (ожидался подожжённый сосед либо труба=FIRE хотя бы раз за окно 211..900)"));

					O.println("[GT6-FLUIDPIPEPROBE] ===== КЕЙС 4 CONTROL-NEG =====");
					O.println("[GT6-FLUIDPIPEPROBE] CONTROL-NEG (линия NORM после кейсов GAS/HOT на соседних линиях): сумма=" + tTotalNow + " (ожидание=" + sFPTotal0Norm + " — перелив воздействия GAS/HOT на соседнюю линию не должен случиться)");
					O.println("[GT6-FLUIDPIPEPROBE] CONTROL-NEG: " + (tTotalNow == sFPTotal0Norm ? "=> PASS" : "=> FAIL (ожидалось " + sFPTotal0Norm + ", получено " + tTotalNow + ")"));

					O.println("[GT6-FLUIDPIPEPROBE] ===== КЕЙС 5 ACID (поправка игрока: кислоты Грега, не чужих модов) =====");
					long tAcidFinal = 0;
					for (gregapi.tileentity.connectors.MultiTileEntityPipeFluid tPipe : sFPAcid) if (tPipe != null) for (gregapi.fluid.FluidTankGT tTank : tPipe.mTanks) tAcidFinal += tTank.amount();
					O.println("[GT6-FLUIDPIPEPROBE] ACID (д1) классификация FL.acid(H2SO4) через ДИНАМИЧЕСКОЕ наполнение FluidsGT.ACID (FL.java:1312): " + (sFPAcidClassOk ? "=> PASS" : "=> FAIL (ожидалось true — жидкость материала с TD.Properties.ACID обязана классифицироваться кислотой)"));
					O.println("[GT6-FLUIDPIPEPROBE] ACID (д2) контроль классификации FL.acid(вода)=false: " + (sFPAcidWaterNegOk ? "=> PASS" : "=> FAIL (вода классифицирована кислотой — классификация слишком широка)"));
					O.println("[GT6-FLUIDPIPEPROBE] ACID (д3) разъедание не-acidProof трубы (ориг. :307-317 trash 16mb/тик + 1%/тик setToAir): залито=" + FP_ACID + "mb осталось=" + tAcidFinal + "mb; сумма достигла 0 на тике " + sFPAcidZeroTick + "; труба растворялась в AIR хотя бы раз=" + sFPAcidEverAir + " " + (tAcidFinal == 0 ? "=> PASS (кислота полностью разъедена)" : "=> FAIL (ожидалось 0 к тику 900 — 2000mb/16mb за тик = ~125 тиков, окно 690)"));
					if (sFPAcidCtl != null) {
						long tAcidCtlFinal = 0;
						for (gregapi.tileentity.connectors.MultiTileEntityPipeFluid tPipe : sFPAcidCtl) if (tPipe != null) for (gregapi.fluid.FluidTankGT tTank : tPipe.mTanks) tAcidCtlFinal += tTank.amount();
						O.println("[GT6-FLUIDPIPEPROBE] ACID (д4) CONTROL acidProof-труба id=" + sFPAcidCtlId + " держит кислоту без потерь: залито=" + FP_ACID + " осталось=" + tAcidCtlFinal + " " + (tAcidCtlFinal == FP_ACID ? "=> PASS" : "=> FAIL (ожидалось " + FP_ACID + ", получено " + tAcidCtlFinal + ")"));
					} else O.println("[GT6-FLUIDPIPEPROBE] ACID (д4) CONTROL: SKIP — acidProof-труба в реестре не найдена/не встала (см. печать постройки)");

					O.println("========== [GT6-FLUIDPIPEPROBE] DONE ==========");
				}
			}
			if (sFPProbeTick > 900 && sFPProbeTick % 200 == 0 && sFPProbeTick <= 2500) O.println("[GT6-FLUIDPIPEPROBE] heartbeat: сервер жив, тик " + sFPProbeTick);
		} catch (Throwable e) {O.println("[GT6-FLUIDPIPEPROBE] EXC " + e); e.printStackTrace(O); sFPProbeTick = 999999;}
	}

	// ========== [GT6-ITEMPIPEPROBE] ВРЕМЕННАЯ проба «Связка №3 — предметные трубы» (Ф3.1, гейт run/gt6itempipeprobe.flag + -Pgt6probes, на каркасе GT6ProbeStand) ==========
	// Судимый канал ПОЛНОСТЬЮ реальный: труба тикает через РЕАЛЬНЫЙ MultiTileEntityPipeItem.onServerTickPre
	// (SERVER_TICK_PRE/PR2, сверено построчно с оригиналом gregtech6/.../MultiTileEntityPipeItem.java — расхождений
	// в control-flow НЕТ, только engine-swap типов: TileEntity->BlockEntity, IInventory/ISidedInventory->
	// Container/WorldlyContainer, NBTTagCompound->CompoundTag с getX().orElse(...), TileEntityHopper/Dispenser->
	// HopperBlockEntity/DispenserBlockEntity, ISidedInventory.getAccessibleSlotsFromSide->WorldlyContainer.getSlotsForFace(FORGE_DIR[...]))
	// -> scanPipes/sortByValuesAcending (:201, выбор БЛИЖАЙШЕЙ цели по возрастанию суммы mStepSize пути) ->
	// sendItemStack -> insertItemStackIntoTileEntity (:224-241) -> ST.move; ни один из этих методов пробой не
	// вызывается напрямую — только реальные тики решают. Сетап-закладка стека в слот[0] трубы (аналог «дать
	// инструмент как скрафченный», §4 манифеста) НЕ трогает mLastReceivedFrom (TileEntityBase05Inventories.java:93
	// slot(i,stack) — чистый сеттер mInventory[i]=stack, без побочных эффектов) — остаётся SIDE_UNDEFINED ==
	// oLastReceivedFrom, гейт :195 равенства выполняется, перенос идёт (документированный в задаче допустимый сетап).
	// Три ГОРИЗОНТАЛЬНЫЕ линии (свежие позиции, разнос по Z=6 >=5 — упреждает CONTROL-NEG-перелив): NEAREST
	// (ближний сундук — ветка на север от p[1], дальний — продолжение линии на восток от p[последний]; сортировка
	// scanPipes по возрастанию суммы mStepSize пути отдаёт предпочтение БЛИЖНЕМУ), FILTER (та же топология +
	// CoverFilterItem на северной стороне p[1]; эталон фильтра — сетап напрямую в CoverData.mNBTs, тем же приёмом,
	// что использует сам CoverFilterItem.onCoverClickedRight, §4 манифеста; судится ФИЛЬТРАЦИЯ транспорта
	// insertItemStackIntoTileEntity:231-233, не постановка кавера — сама постановка кавера идёт через реальный
	// публичный API ITileEntityCoverable.setCoverItem), DISABLED-SIDE (mDisabledOutputs на северной стороне p[1]
	// выставлен напрямую — тот же эффект, что даёт обезьяний ключ в onToolClick2:136-146).
	// ID НЕ выдуманы, оба грепом источника: труба — "Brass Item Pipe" medium id=25002 (Loader_MultiTileEntities.java:1827
	// addItemPipes(25000,...) + MultiTileEntityPipeItem.addItemPipes аID+2=medium, :77) — поправка: в этой сборке
	// GT6 НЕТ отдельного медного (Cu) яруса предметных труб (только сплавы: Brass/Constantan/CobaltBrass/Ge/
	// ArsenicCopper/...), задание ошибочно предполагало "медную" (по аналогии с Cu-трубой FLUIDPIPEPROBE) — взят
	// САМЫЙ ПЕРВЫЙ зарегистрированный ярус (Brass), функционально идентичный любому другому; сундук — "Mossy Stone
	// Chest" id=32745 (Loader_MultiTileEntities.java:152, aRegistry.add("Mossy Stone Chest","Chests",32745,...,
	// MultiTileEntityChest.class,...)). Оба ID подтверждены В РАНТАЙМЕ через getClassContainer()!=null (см. build).
	// Снять при уборке фазы.
	private static final int ITEMPIPE_L = 6, ITEMPIPE_NEAR_IDX = 1;
	private static final int PIPE_ID  = 25002; // Brass Item Pipe (medium) — см. комментарий блока выше
	private static final int CHEST_ID = 32745; // Mossy Stone Chest — Loader_MultiTileEntities.java:152
	private static final String IP_M = "GT6-ITEMPIPEPROBE";
	private static int sIPProbeTick = -1;
	private static net.minecraft.server.level.ServerPlayer sIPPlayer;
	private static gregapi.probe.GT6ProbeStand.Seq sIPSeq;
	private static gregapi.tileentity.connectors.MultiTileEntityPipeItem[] sIPNearChain, sIPFilterChain, sIPDisChain;
	private static gregapi.block.multitileentity.example.MultiTileEntityChest sIPNearNear, sIPNearFar, sIPFilterNear, sIPFilterFar, sIPDisNear, sIPDisFar;
	private static long sIPStepSize, sIPCapacity;                       // живые параметры трубы (из BE, не предположены)
	private static long sIPNear0, sIPFilter0, sIPDis0;                  // консервация: заложенное количество предметов по линии (константа всё время пробы)
	private static long sIPNear400Near, sIPNear400Far, sIPDis400Near, sIPDis400Far; // снимок на тике 400 для CONTROL-NEG (сверка на тике 650)
	private static int  sIPArrivalTick = -1;
	private static long sIPArrivalServerTime = -1;                      // RATE: проба-тик и живой SERVER_TIME первого появления в ближнем сундуке NEAREST
	private static int  sIPConserveSamples = 0, sIPConserveFails = 0;   // CASE4: жёсткая консервация на каждом тике окна 210..650, по всем 3 линиям

	/** Сумма предметов во всех слотах сундука (консервация; каркас {@link gregapi.probe.GT6ProbeStand#slotCount}). */
	private static long gt6ItemPipeProbeChestSum(gregapi.block.multitileentity.example.MultiTileEntityChest aChest) {
		if (aChest == null) return 0;
		long rSum = 0;
		for (int i = 0; i < aChest.invsize(); i++) rSum += gregapi.probe.GT6ProbeStand.slotCount(aChest, i);
		return rSum;
	}
	/** Сумма предметов во всех слотах всех труб цепи (консервация). */
	private static long gt6ItemPipeProbeChainSum(gregapi.tileentity.connectors.MultiTileEntityPipeItem[] aChain) {
		long rSum = 0;
		for (gregapi.tileentity.connectors.MultiTileEntityPipeItem tP : aChain) if (tP != null) for (int i = 0; i < tP.invsize(); i++) rSum += gregapi.probe.GT6ProbeStand.slotCount(tP, i);
		return rSum;
	}
	/** Полная очистка сундука (F15: только ItemStack.EMPTY, никогда null — см. §7 манифеста «чистка слотов»). */
	private static void gt6ItemPipeProbeClearChest(gregapi.block.multitileentity.example.MultiTileEntityChest aChest) {
		for (int i = 0; i < aChest.invsize(); i++) gregapi.probe.GT6ProbeStand.slotSet(aChest, i, ItemStack.EMPTY);
	}
	/** Первый непустой стек в сундуке (проверка ТИПА предмета, не только количества — R8 «молчаливая потеря семантики»). */
	private static ItemStack gt6ItemPipeProbeFirstStack(gregapi.block.multitileentity.example.MultiTileEntityChest aChest) {
		for (int i = 0; i < aChest.invsize(); i++) {ItemStack tStack = aChest.slot(i); if (tStack != null && !tStack.isEmpty()) return tStack;}
		return ItemStack.EMPTY;
	}

	/** Одна горизонтальная линия на каркасе: анкер -> ITEMPIPE_L труб (восток); ближний сундук — ветка на север от
	 *  p[ITEMPIPE_NEAR_IDX]; дальний сундук — продолжение линии на восток от p[последний]. Топология/финальная
	 *  конфигурация (disabled-bit, cover-фильтр) — здесь; постройка/анкер/свежие стеки/верификация класса — каркас
	 *  {@link gregapi.probe.GT6ProbeStand#line}/{@link gregapi.probe.GT6ProbeStand#place}. */
	private static Object[] gt6ItemPipeProbeRow(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aBase, boolean aDisableNearOutput, boolean aInstallFilter) {
		net.minecraft.core.Direction tEast = net.minecraft.core.Direction.EAST;
		gregapi.probe.GT6ProbeStand.solidPad(aLevel, aBase.below(), ITEMPIPE_L + 3, 1); // пол — гигиена, не судимый канал
		gregapi.tileentity.connectors.MultiTileEntityPipeItem[] tChain = gregapi.probe.GT6ProbeStand.line(
			aLevel, sIPPlayer, aBase, tEast, ITEMPIPE_L, PIPE_ID, gregapi.tileentity.connectors.MultiTileEntityPipeItem.class, IP_M);
		if (tChain[ITEMPIPE_L-1] == null) return new Object[]{tChain, null, null};
		net.minecraft.core.BlockPos tNearAnchor = aBase.relative(tEast, ITEMPIPE_NEAR_IDX + 1); // позиция p[ITEMPIPE_NEAR_IDX]
		net.minecraft.core.BlockPos tFarAnchor  = aBase.relative(tEast, ITEMPIPE_L);             // позиция p[последний]
		gregapi.block.multitileentity.example.MultiTileEntityChest tNear = gregapi.probe.GT6ProbeStand.place(
			aLevel, sIPPlayer, tNearAnchor, net.minecraft.core.Direction.NORTH, gregapi.probe.GT6ProbeStand.mteStack(CHEST_ID),
			gregapi.block.multitileentity.example.MultiTileEntityChest.class, IP_M, "ближний сундук");
		gregapi.block.multitileentity.example.MultiTileEntityChest tFar = gregapi.probe.GT6ProbeStand.place(
			aLevel, sIPPlayer, tFarAnchor, tEast, gregapi.probe.GT6ProbeStand.mteStack(CHEST_ID),
			gregapi.block.multitileentity.example.MultiTileEntityChest.class, IP_M, "дальний сундук");
		if (tNear == null || tFar == null) return new Object[]{tChain, tNear, tFar};
		// принудительная связность реальным API connect() (тем же методом, что дёргает гайковёрт/авто-разводка) — сеттинг топологии, НЕ обход переноса
		tChain[ITEMPIPE_NEAR_IDX].connect(SIDE_NORTH, T);
		tChain[ITEMPIPE_L-1].connect(SIDE_EAST, T);
		if (aDisableNearOutput) tChain[ITEMPIPE_NEAR_IDX].mDisabledOutputs ^= B[SIDE_NORTH]; // эффект обезьяньего ключа, оригинал onToolClick2:136-146
		if (aInstallFilter) tChain[ITEMPIPE_NEAR_IDX].setCoverItem(SIDE_NORTH, IL.Cover_Filter_Item.get(1), null, T, T); // реальный публичный API постановки кавера (ITileEntityCoverable)
		return new Object[]{tChain, tNear, tFar};
	}

	/** Тик 200: постройка трёх линий + чтение живых параметров трубы. Любой обрыв -> RuntimeException -> Seq печатает EXC. */
	private static void gt6ItemPipeProbeBuild() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = sIPPlayer.level();
		O.println("========== [" + IP_M + "] Связка №3 — предметные трубы (Ф3.1, на каркасе GT6ProbeStand) ==========");
		MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		if (tReg == null || tReg.getClassContainer(PIPE_ID) == null || tReg.getClassContainer(CHEST_ID) == null) throw new RuntimeException("реестр/ID не найдены (труба=" + PIPE_ID + " сундук=" + CHEST_ID + ")");
		O.println("[" + IP_M + "] ID подтверждены: труба=" + tReg.getClassContainer(PIPE_ID).mClass.getSimpleName() + "(" + PIPE_ID + ") сундук=" + tReg.getClassContainer(CHEST_ID).mClass.getSimpleName() + "(" + CHEST_ID + ")");

		net.minecraft.core.BlockPos tBaseNear   = sIPPlayer.blockPosition().offset(4, 0, 4);
		net.minecraft.core.BlockPos tBaseFilter = tBaseNear.offset(0, 0, 6);
		net.minecraft.core.BlockPos tBaseDis    = tBaseNear.offset(0, 0, 12);

		Object[] tRowNear   = gt6ItemPipeProbeRow(tLevel, tBaseNear,   F, F);
		Object[] tRowFilter = gt6ItemPipeProbeRow(tLevel, tBaseFilter, F, T);
		Object[] tRowDis    = gt6ItemPipeProbeRow(tLevel, tBaseDis,    T, F);
		sIPNearChain   = (gregapi.tileentity.connectors.MultiTileEntityPipeItem[]) tRowNear[0];   sIPNearNear   = (gregapi.block.multitileentity.example.MultiTileEntityChest) tRowNear[1];   sIPNearFar   = (gregapi.block.multitileentity.example.MultiTileEntityChest) tRowNear[2];
		sIPFilterChain = (gregapi.tileentity.connectors.MultiTileEntityPipeItem[]) tRowFilter[0]; sIPFilterNear = (gregapi.block.multitileentity.example.MultiTileEntityChest) tRowFilter[1]; sIPFilterFar = (gregapi.block.multitileentity.example.MultiTileEntityChest) tRowFilter[2];
		sIPDisChain    = (gregapi.tileentity.connectors.MultiTileEntityPipeItem[]) tRowDis[0];    sIPDisNear    = (gregapi.block.multitileentity.example.MultiTileEntityChest) tRowDis[1];    sIPDisFar    = (gregapi.block.multitileentity.example.MultiTileEntityChest) tRowDis[2];
		if (sIPNearNear == null || sIPNearFar == null || sIPFilterNear == null || sIPFilterFar == null || sIPDisNear == null || sIPDisFar == null)
			throw new RuntimeException("постройка линии не удалась (null сундук в цепочке)");

		sIPStepSize = sIPNearChain[0].mStepSize; sIPCapacity = sIPNearChain[0].invsize();
		O.println("[" + IP_M + "] живые параметры трубы (из BE, не предположены): mStepSize=" + sIPStepSize + " invsize(capacity)=" + sIPCapacity + " (один и тот же ярус на всех 3 линиях)");
		O.println("[" + IP_M + "] сундук invsize=" + sIPNearNear.invsize());

		gregapi.cover.CoverData tFilterCovers = sIPFilterChain[ITEMPIPE_NEAR_IDX].getCoverData();
		tFilterCovers.mNBTs[SIDE_NORTH] = ST.save("gt.filter.item", Blocks.COBBLESTONE);
		O.println("[" + IP_M + "] FILTER: кавер CoverFilterItem установлен на p[" + ITEMPIPE_NEAR_IDX + "] сторона NORTH, эталон=Cobblestone (mVisuals=" + tFilterCovers.mVisuals[SIDE_NORTH] + "=whitelist, insertItemStackIntoTileEntity:231-233)");
		O.println("[" + IP_M + "] DISABLED-SIDE: p[" + ITEMPIPE_NEAR_IDX + "].mDisabledOutputs=" + sIPDisChain[ITEMPIPE_NEAR_IDX].mDisabledOutputs + " (бит NORTH=" + B[SIDE_NORTH] + " выставлен)");
	}

	/** Тик 210: закладка стека A=16×Cobblestone в слот[0] трубы p[0] каждой линии (сетап-канал, §4 манифеста — TileEntityBase05Inventories.slot(i,stack) чистый сеттер, mLastReceivedFrom не трогает). */
	private static void gt6ItemPipeProbeLoad() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		gregapi.probe.GT6ProbeStand.slotSet(sIPNearChain[0],   0, ST.make(Blocks.COBBLESTONE, 16, 0));
		gregapi.probe.GT6ProbeStand.slotSet(sIPFilterChain[0], 0, ST.make(Blocks.COBBLESTONE, 16, 0));
		gregapi.probe.GT6ProbeStand.slotSet(sIPDisChain[0],    0, ST.make(Blocks.COBBLESTONE, 16, 0));
		sIPNear0   = gt6ItemPipeProbeChestSum(sIPNearNear)   + gt6ItemPipeProbeChestSum(sIPNearFar)   + gt6ItemPipeProbeChainSum(sIPNearChain);
		sIPFilter0 = gt6ItemPipeProbeChestSum(sIPFilterNear) + gt6ItemPipeProbeChestSum(sIPFilterFar) + gt6ItemPipeProbeChainSum(sIPFilterChain);
		sIPDis0    = gt6ItemPipeProbeChestSum(sIPDisNear)    + gt6ItemPipeProbeChestSum(sIPDisFar)    + gt6ItemPipeProbeChainSum(sIPDisChain);
		O.println("[" + IP_M + "] заложено (тик 210, слот[0] трубы p[0]): NEAREST=" + sIPNear0 + " FILTER=" + sIPFilter0 + " DISABLED-SIDE=" + sIPDis0 + " (по 16×Cobblestone)");
	}

	/** Окно 210..650, КАЖДЫЙ тик: (а) жёсткая консервация по всем 3 линиям (CASE4); (б) первое появление предмета
	 *  в ближнем сундуке NEAREST — RATE, мягкий судья (только печать тика+SERVER_TIME%10, оригинал :194). */
	private static void gt6ItemPipeProbeConserveCheck() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		long tNear   = gt6ItemPipeProbeChestSum(sIPNearNear)   + gt6ItemPipeProbeChestSum(sIPNearFar)   + gt6ItemPipeProbeChainSum(sIPNearChain);
		long tFilter = gt6ItemPipeProbeChestSum(sIPFilterNear) + gt6ItemPipeProbeChestSum(sIPFilterFar) + gt6ItemPipeProbeChainSum(sIPFilterChain);
		long tDis    = gt6ItemPipeProbeChestSum(sIPDisNear)    + gt6ItemPipeProbeChestSum(sIPDisFar)    + gt6ItemPipeProbeChainSum(sIPDisChain);
		sIPConserveSamples++;
		if (tNear != sIPNear0 || tFilter != sIPFilter0 || tDis != sIPDis0) {
			sIPConserveFails++;
			O.println("[" + IP_M + "] DIAG консервация нарушена на тике " + sIPProbeTick + ": NEAREST=" + tNear + "(ожид." + sIPNear0 + ") FILTER=" + tFilter + "(ожид." + sIPFilter0 + ") DISABLED-SIDE=" + tDis + "(ожид." + sIPDis0 + ")");
		}
		if (sIPArrivalTick < 0 && gt6ItemPipeProbeChestSum(sIPNearNear) > 0) {
			sIPArrivalTick = sIPProbeTick; sIPArrivalServerTime = SERVER_TIME;
			O.println("[" + IP_M + "] RATE: первое появление предмета в ближнем сундуке NEAREST — проба-тик=" + sIPArrivalTick + " SERVER_TIME=" + sIPArrivalServerTime + " SERVER_TIME%10=" + (sIPArrivalServerTime % 10) + " (перенос только на кратных 10 тиках, оригинал onServerTickPre:194)");
		}
	}

	/** Тик 400: КЕЙС 1 NEAREST, КЕЙС 3 DISABLED-SIDE, КЕЙС 2(а) FILTER — эталон A прошёл. */
	private static void gt6ItemPipeProbeJudge400() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + IP_M + "] ===== КЕЙС 1 NEAREST =====");
		long tNearNearSum = gt6ItemPipeProbeChestSum(sIPNearNear), tNearFarSum = gt6ItemPipeProbeChestSum(sIPNearFar), tNearChainSum = gt6ItemPipeProbeChainSum(sIPNearChain);
		ItemStack tNearFirst = gt6ItemPipeProbeFirstStack(sIPNearNear);
		O.println("[" + IP_M + "] NEAREST: ближний=" + tNearNearSum + "(" + (tNearFirst.isEmpty()?"пусто":tNearFirst.getItem()) + ") дальний=" + tNearFarSum + " в трубах=" + tNearChainSum + " (заложено=" + sIPNear0 + ")");
		sIPSeq.judge("NEAREST (а) ближний сундук получил весь стек", tNearNearSum == sIPNear0, sIPNear0, tNearNearSum);
		sIPSeq.judge("NEAREST (б) тип предмета в ближнем = Cobblestone", !tNearFirst.isEmpty() && tNearFirst.is(Blocks.COBBLESTONE.asItem()), "cobblestone", tNearFirst.isEmpty()?"пусто":tNearFirst.getItem());
		sIPSeq.judge("NEAREST (в) дальний сундук пуст (сортировка scanPipes :201 предпочла ближний)", tNearFarSum == 0, 0, tNearFarSum);
		sIPSeq.conserve("NEAREST (г) консервация", sIPNear0, () -> tNearNearSum + tNearFarSum + tNearChainSum);
		sIPNear400Near = tNearNearSum; sIPNear400Far = tNearFarSum;

		O.println("[" + IP_M + "] ===== КЕЙС 3 DISABLED-SIDE =====");
		long tDisNearSum = gt6ItemPipeProbeChestSum(sIPDisNear), tDisFarSum = gt6ItemPipeProbeChestSum(sIPDisFar), tDisChainSum = gt6ItemPipeProbeChainSum(sIPDisChain);
		ItemStack tDisFarFirst = gt6ItemPipeProbeFirstStack(sIPDisFar);
		O.println("[" + IP_M + "] DISABLED-SIDE: ближний(вывод отключён)=" + tDisNearSum + " дальний=" + tDisFarSum + "(" + (tDisFarFirst.isEmpty()?"пусто":tDisFarFirst.getItem()) + ") в трубах=" + tDisChainSum + " (заложено=" + sIPDis0 + ")");
		sIPSeq.judge("DISABLED-SIDE (а) ближний сундук пуст (эмиссия в его сторону отключена, гейт insertItemStackIntoTileEntity:225)", tDisNearSum == 0, 0, tDisNearSum);
		sIPSeq.judge("DISABLED-SIDE (б) дальний сундук получил весь стек", tDisFarSum == sIPDis0, sIPDis0, tDisFarSum);
		sIPSeq.conserve("DISABLED-SIDE (в) консервация", sIPDis0, () -> tDisNearSum + tDisFarSum + tDisChainSum);
		sIPDis400Near = tDisNearSum; sIPDis400Far = tDisFarSum;

		O.println("[" + IP_M + "] ===== КЕЙС 2(а) FILTER — эталон A (Cobblestone, совпадает с фильтром) =====");
		long tFilterNearSum1 = gt6ItemPipeProbeChestSum(sIPFilterNear), tFilterFarSum1 = gt6ItemPipeProbeChestSum(sIPFilterFar);
		ItemStack tFilterNearFirst = gt6ItemPipeProbeFirstStack(sIPFilterNear);
		O.println("[" + IP_M + "] FILTER фаза A: ближний(фильтрованный)=" + tFilterNearSum1 + "(" + (tFilterNearFirst.isEmpty()?"пусто":tFilterNearFirst.getItem()) + ") дальний=" + tFilterFarSum1 + " (заложено=" + sIPFilter0 + ")");
		sIPSeq.judge("FILTER (а1) A прошёл whitelist в ближний фильтрованный сундук", tFilterNearSum1 == sIPFilter0, sIPFilter0, tFilterNearSum1);
		sIPSeq.judge("FILTER (а2) дальний пуст (A не ушёл мимо фильтра)", tFilterFarSum1 == 0, 0, tFilterFarSum1);

		O.println("[" + IP_M + "] RATE (мягкий судья, только печать): mStepSize=" + sIPStepSize + " invsize=" + sIPCapacity + " первое появление в ближнем NEAREST на проба-тике=" + sIPArrivalTick + " SERVER_TIME=" + sIPArrivalServerTime + " SERVER_TIME%10=" + (sIPArrivalServerTime < 0 ? "?(не зафиксировано)" : String.valueOf(sIPArrivalServerTime % 10)) + " (ожидание 0 — оригинал onServerTickPre:194)");
	}

	/** Тик 410: очистка сундуков FILTER-линии + закладка B=16×Dirt (не совпадает с эталоном фильтра). */
	private static void gt6ItemPipeProbeSetupFilterB() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		gt6ItemPipeProbeClearChest(sIPFilterNear);
		gt6ItemPipeProbeClearChest(sIPFilterFar);
		gregapi.probe.GT6ProbeStand.slotSet(sIPFilterChain[0], 0, ST.make(Blocks.DIRT, 16, 0));
		O.println("[" + IP_M + "] FILTER фаза B: сундуки очищены, заложен B=16×Dirt в p[0] (эталон фильтра — Cobblestone, не совпадает)");
	}

	/** Тик 600: КЕЙС 2(б) FILTER — эталон B не проходит whitelist в фильтрованный сундук. */
	private static void gt6ItemPipeProbeJudge600() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + IP_M + "] ===== КЕЙС 2(б) FILTER — эталон B (Dirt, НЕ совпадает с фильтром) =====");
		long tFilterNearSum2 = gt6ItemPipeProbeChestSum(sIPFilterNear), tFilterFarSum2 = gt6ItemPipeProbeChestSum(sIPFilterFar), tFilterChainSum2 = gt6ItemPipeProbeChainSum(sIPFilterChain);
		ItemStack tFilterFarFirst = gt6ItemPipeProbeFirstStack(sIPFilterFar);
		O.println("[" + IP_M + "] FILTER фаза B: ближний(фильтрованный)=" + tFilterNearSum2 + " дальний=" + tFilterFarSum2 + "(" + (tFilterFarFirst.isEmpty()?"пусто":tFilterFarFirst.getItem()) + ") в трубах=" + tFilterChainSum2 + " (заложено=" + sIPFilter0 + ")");
		sIPSeq.judge("FILTER (б1) B НЕ попал в фильтрованный ближний сундук", tFilterNearSum2 == 0, 0, tFilterNearSum2);
		sIPSeq.judge("FILTER (б2) B ушёл дальше — в дальний сундук (не застрял, консервация линии цела)", tFilterFarSum2 == sIPFilter0, sIPFilter0, tFilterFarSum2);
		sIPSeq.judge("FILTER (б3) тип предмета в дальнем = Dirt", !tFilterFarFirst.isEmpty() && tFilterFarFirst.is(Blocks.DIRT.asItem()), "dirt", tFilterFarFirst.isEmpty()?"пусто":tFilterFarFirst.getItem());
	}

	/** Тик 650: КЕЙС 5 CONTROL-NEG (линии не повлияли друг на друга за время работы FILTER-фазы Б) + итог CASE4 CONSERVE + DONE. */
	private static void gt6ItemPipeProbeJudge650() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + IP_M + "] ===== КЕЙС 5 CONTROL-NEG =====");
		long tNearNearNow = gt6ItemPipeProbeChestSum(sIPNearNear), tNearFarNow = gt6ItemPipeProbeChestSum(sIPNearFar);
		long tDisNearNow  = gt6ItemPipeProbeChestSum(sIPDisNear),  tDisFarNow  = gt6ItemPipeProbeChestSum(sIPDisFar);
		O.println("[" + IP_M + "] CONTROL-NEG NEAREST снимок-тик400=(" + sIPNear400Near + "," + sIPNear400Far + ") сейчас=(" + tNearNearNow + "," + tNearFarNow + ")");
		sIPSeq.judge("CONTROL-NEG NEAREST не изменилась за время работы FILTER-фазы Б", tNearNearNow == sIPNear400Near && tNearFarNow == sIPNear400Far, sIPNear400Near + "/" + sIPNear400Far, tNearNearNow + "/" + tNearFarNow);
		O.println("[" + IP_M + "] CONTROL-NEG DISABLED-SIDE снимок-тик400=(" + sIPDis400Near + "," + sIPDis400Far + ") сейчас=(" + tDisNearNow + "," + tDisFarNow + ")");
		sIPSeq.judge("CONTROL-NEG DISABLED-SIDE не изменилась", tDisNearNow == sIPDis400Near && tDisFarNow == sIPDis400Far, sIPDis400Near + "/" + sIPDis400Far, tDisNearNow + "/" + tDisFarNow);

		O.println("[" + IP_M + "] ===== КЕЙС 4 CONSERVE (жёсткий судья) =====");
		O.println("[" + IP_M + "] консервация держалась на " + (sIPConserveSamples - sIPConserveFails) + "/" + sIPConserveSamples + " замерах (тики 210..650, каждый тик, все 3 линии одновременно)");
		sIPSeq.judge("CONSERVE консервация держалась на каждом замере", sIPConserveFails == 0, 0, sIPConserveFails);

		sIPSeq.done();
	}

	public static void gt6ItemPipeProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sIPProbeTick++;
		if (aServer.getPlayerList().getPlayers().isEmpty()) return;
		sIPPlayer = aServer.getPlayerList().getPlayers().get(0);
		if (sIPSeq == null) {
			sIPSeq = new gregapi.probe.GT6ProbeStand.Seq(IP_M)
				.at(200, GT_API_Proxy::gt6ItemPipeProbeBuild)
				.at(210, GT_API_Proxy::gt6ItemPipeProbeLoad)
				.window(210, 650, GT_API_Proxy::gt6ItemPipeProbeConserveCheck)
				.at(400, GT_API_Proxy::gt6ItemPipeProbeJudge400)
				.at(410, GT_API_Proxy::gt6ItemPipeProbeSetupFilterB)
				.at(600, GT_API_Proxy::gt6ItemPipeProbeJudge600)
				.at(650, GT_API_Proxy::gt6ItemPipeProbeJudge650);
		}
		sIPSeq.tick(sIPProbeTick);
	}

}
