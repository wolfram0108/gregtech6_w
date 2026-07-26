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
				// [GT6-ENERGYCHAINPROBE] верификационный стенд «Связка №4 — энерго-лестница» (Ф3.1, на каркасе GT6ProbeStand) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6energychainprobe.flag")) gt6EnergyChainProbeTick(aEvent.getServer());
				// [GT6-CRUCIBLEPROBE] верификационный стенд «Связка №5 — тигельный цикл» (Ф3.1, на каркасе GT6ProbeStand) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6crucibleprobe.flag")) gt6CrucibleProbeTick(aEvent.getServer());
				// [GT6-AUTOOUTPROBE] верификационный стенд «Связка №6 — авто-вывод машин + каверы в работе» (Ф3.1, на каркасе GT6ProbeStand) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6autooutprobe.flag")) gt6AutoOutProbeTick(aEvent.getServer());
				// [GT6-CHEMPROBE] верификационный стенд «Связка №7 — химический процесс multi-fluid» (Ф3.1, на каркасе GT6ProbeStand) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6chemprobe.flag")) gt6ChemProbeTick(aEvent.getServer());
				// [GT6-STEAMFARMPROBE] верификационный стенд «Связка №8 — паровая ферма N бойлеров → 1 турбина» (Ф3.1, на каркасе GT6ProbeStand) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6steamfarmprobe.flag")) gt6SteamFarmProbeTick(aEvent.getServer());
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

	// ========== [GT6-ENERGYCHAINPROBE] ВРЕМЕННАЯ проба «Связка №4 — энерго-лестница» (Ф3.1, гейт run/gt6energychainprobe.flag + -Pgt6probes, на каркасе GT6ProbeStand) ==========
	// Цель: доказать ЧИСЛОМ реальную конверсию топливо→HU→Steam→RU→EU→RU, КАЖДОЕ звено — реальный конвертор
	// GT6, тикающий штатным onTick2 (никакой судимый метод пробой не вызывается напрямую). Судимые каналы:
	// MultiTileEntityGeneratorSolid.onTick2 (:100-176, HU из рецепта FM.Furnace) -> ITileEntityEnergy.Util.
	// emitEnergyToNetwork (ITileEntityEnergy.java:229) -> MultiTileEntityBoilerTank.doInject/onTick2 (:112-152,
	// Steam из HU+воды) -> FL.move(getAdjacentTank(SIDE_UP)) -> MultiTileEntityTurbineSteam.doConversion (:87-109,
	// RU из Steam) -> TE_Behavior_Energy_Converter.doConversion (:61-94, эмиссия) -> MultiTileEntityDynamoElectric
	// (наследует TileEntityBase10EnergyConverter, EU из RU) -> TileEntityBase10EnergyBatBox.doInject (:185-199).
	// Топология (этап А, разведка кода, ВЫЧИТАНА, не угадана): генератор эмитит HU ТОЛЬКО вверх
	// (MultiTileEntityGeneratorSolid.java:270 isEnergyEmittingTo: SIDES_TOP[aSide]); бойлер принимает HU с ЛЮБОЙ
	// стороны (:250 isEnergyAcceptingFrom = isEnergyType, side-гейта нет) и эмитит пар ТОЛЬКО вверх (:143
	// getAdjacentTank(SIDE_UP), хардкод, не по mFacing); турбина принимает Steam на OPOS[mFacing] и эмитит RU на
	// mFacing (:129-130) — при mFacing=UP принимает снизу и эмитит вверх; динамо симметрично: RU на OPOS[mFacing],
	// EU на mFacing (:35-36) — при mFacing=UP тот же вертикальный проход. Итог: ВЕРТИКАЛЬНЫЙ СТОЛБ генератор->
	// бойлер->турбина(mFacing=UP)->динамо(mFacing=UP)->батарея(приёмник), facing турбины/динамо выставлен ПОСЛЕ
	// установки реальным API setPrimaryFacing (тот же метод, что дёргает гайковёрт, TileEntityBase09FacingSingle.
	// java:88) — топология, не обход передачи (манифест §4). Подбор тиров (этап А, чтобы избежать "чёрной дыры"
	// потери энергии на несовпадении размеров пакетов): TE_Behavior_Energy_Stats.doInject (:56-66) отвергает пакет
	// РАЗМЕРОМ МЕНЬШЕ receiver.mMin, но эмиттер (TE_Behavior_Energy_Converter.java:79-90) считает это "успехом"
	// (расходует mStorage), даже если receiver ничего не получил — ПРОВЕРЕНО по коду: mEnergyOUT.mMin для Invar-
	// турбины (id1518, NBT_OUTPUT=32→min=16, Loader_MultiTileEntities.java:797) РОВНО = mEnergyIN.mMin динамо T1
	// (id10111, NBT_INPUT=32→min=16, :950); динамо-T1 mEnergyOUT.mMin=11 (NBT_OUTPUT=22→11) << mInput/2=4 батареи
	// ULV (id10080, mInput=V[0]=8, TileEntityBase10EnergyBatBox.java:724 getEnergySizeInputMin=recommended/2) — все
	// пороги цепи согласованы конструктором GT6, потерь на границах не будет. Бойлер (Pb, id1200, NBT_OUTPUT_SU=
	// 16×STEAM_PER_EU=32) эмитит пар соседу только когда mTanks[1].amount()>capacity/2 (:139, capacity=mOutput×
	// 10000=320000 — рассчитано на промышленный приток HU, недостижимо с нуля за окно пробы одним генератором) —
	// сетап-обход ТОЛЬКО начальной точки резервуара (пред-заряд пара чуть ниже порога излучения, аналог "вода в
	// танк" из задания), сама эмиссия/конверсия ниже по цепи — реальные тики. Формула бойлера (:117-124): пар =
	// вода_расход×mEfficiency×160/10000, mEfficiency∈[5000,10000] (калcификация :119-122) ⇒ пар∈[вода×80,вода×160].
	// Формула турбины (:95): RU=Steam/STEAM_PER_EU (точно, без округления вниз кроме целочисленного пола). Формула
	// генератора (:104,156): HU=min(mRate,mEnergy)/тик эмиссия; накопление=рецепт.getAbsoluteTotalPower()×
	// mEfficiency/10000 (Recipe.java:723-724, UT.Code.units). Дифф порт/оригинал всех 5 классов задания — построчно
	// идентичен (только engine-swap типов TileEntity->BlockEntity, NBTTagCompound->CompoundTag+getXOr, World->Level,
	// IFluidHandler forge->neoforge; см. параллельное чтение файлов в отчёте агента) — расхождений в control-flow
	// НЕТ. Снять при уборке фазы.
	private static final int ECP_GEN_ID         = 1199;  // Brick Burning Box (Solid) — Loader_MultiTileEntities.java:520, mEfficiency=2500 mRate=16 HU/т
	private static final int ECP_BOILER_ID      = 1200;  // Steam Boiler Tank (Pb) — :554, NBT_OUTPUT_SU=16×STEAM_PER_EU=32 (mOutput)
	private static final int ECP_TURBINE_ID     = 1518;  // Steam Turbine (Invar) — :797, NBT_INPUT=48×STEAM_PER_EU=96 NBT_OUTPUT=32 (тир согласован с динамо T1)
	private static final int ECP_DYNAMO_ID      = 10111; // Electric Dynamo (T1) — :950, NBT_INPUT=32 NBT_OUTPUT=22
	private static final int ECP_BATBOX_RECV_ID = 10080; // Battery Box (ULV) — :894 i=0, mInput=8, окно приёма [min=4..max=16].
	// РАЗБОР ФИНАЛЬНЫЙ (§6.3-принты доказали): живая цепь даёт пакет РОВНО 14 EU (турбина: steam-storage 64 →
	// tOutput=units(64,96,32)=21 RU; динамо: units(21,32,22)=14 EU; DIAG-DOCONV ×1018 storage=21 tOutput=14).
	// Root.doEnergyInjection:886 (ориг. :717, посимвольно 1:1): пакет < getEnergySizeInputMin приёмника
	// «съедается» БЕЗ зачисления (return aAmount мимо doInject) — семантика НЕДОНАПРЯЖЕНИЯ GT6. LV-батарея
	// (min=16) глотала пакет-14 впустую каждый тик — потому «эмиссия есть, приёмник пуст». ULV (min=4≤14≤max=16)
	// принимает. Промежуточная замена ULV→LV была ошибкой ревизии оркестратора (страх oversize 44>16 —
	// нереализуем: живой поток фиксирован на 14, ибо один Pb-бойлер физически не разгоняет турбину до
	// рекомендованных 96 steam-storage; «раскормить» цепь = связка №8 «N бойлеров на турбину»).
	private static final int ECP_BATBOX_SRC_ID  = 10081; // Battery Box (LV) — :894 i=1, тот же ID, что в WIREPROBE (mInput=mOutput=32)
	private static final int ECP_MOTOR_ID       = 10021; // Electric Motor (T1) — :850, NBT_INPUT=32 NBT_OUTPUT=16
	private static final int ECP_AXLE_ID        = 24800; // Small Wooden Axle — Loader_MultiTileEntities.java:1667, mSpeed=VMAX[0] mPower=1, коннектор без порога размера
	private static final String ECP_M = "GT6-ENERGYCHAINPROBE";
	private static int sECPProbeTick = -1;
	private static net.minecraft.server.level.ServerPlayer sECPPlayer;
	private static gregapi.probe.GT6ProbeStand.Seq sECPSeq;

	// ISO (фаза 4a — генератор+бойлер изолированно, без турбины)
	private static gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick sECPIsoGen;
	private static gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank sECPIsoBoiler;
	private static long sECPIsoWater0, sECPIsoFuel0;
	// COLD (control-neg — тот же генератор+бойлер, НИКОГДА не разжигается/не кормится)
	private static gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick sECPColdGen;
	private static gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank sECPColdBoiler;
	private static long sECPColdWater0;
	// CHAIN (фаза 4b — полная цепь генератор+бойлер+турбина+динамо+батарея-приёмник)
	private static gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick sECPChainGen;
	private static gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank sECPChainBoiler;
	private static gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam sECPChainTurbine;
	private static gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric sECPChainDynamo;
	private static gregapi.tileentity.energy.TileEntityBase10EnergyBatBox sECPChainBatBox;
	private static long sECPChainEu0, sECPChainSteamCounter0;
	// MOTOR (фаза 4c — батарея-источник+мотор+вал, независимая горизонтальная линия)
	private static gregapi.tileentity.energy.TileEntityBase10EnergyBatBox sECPMotorSrc;
	private static gregtech.tileentity.energy.converters.MultiTileEntityMotorElectric sECPMotor;
	private static gregapi.tileentity.connectors.MultiTileEntityAxle sECPAxle;
	private static net.minecraft.core.BlockPos sECPMotorPos; // DIAG: для сверки захваченной ссылки sECPMotor с реально тикающим BE по координатам (§6.1) — снять при уборке фазы

	/** Прямой доступ к mTanks[aIndex] бойлера (caraCass fill()/tankAmount() бьют только в индекс 0 через singular
	 *  mTank — у бойлера ЕГО НЕТ, только массив mTanks[вода=0,пар=1]; читаем массив через caraCass fld()). */
	private static gregapi.fluid.FluidTankGT gt6EnergyChainProbeBoilerTank(Object aBoiler, int aIndex) {
		Object tArr = gregapi.probe.GT6ProbeStand.fld(aBoiler, "mTanks");
		return (tArr instanceof gregapi.fluid.FluidTankGT[] tTanks && aIndex < tTanks.length) ? tTanks[aIndex] : null;
	}
	private static long gt6EnergyChainProbeBoilerAmount(Object aBoiler, int aIndex) {
		gregapi.fluid.FluidTankGT t = gt6EnergyChainProbeBoilerTank(aBoiler, aIndex);
		return t == null ? 0 : t.amount();
	}
	private static void gt6EnergyChainProbeBoilerFill(Object aBoiler, int aIndex, String aFluidName, long aMB) {
		gregapi.fluid.FluidTankGT t = gt6EnergyChainProbeBoilerTank(aBoiler, aIndex);
		if (t != null) t.setFluid(gregapi.data.FL.make(aFluidName, aMB));
	}

	/** Один "генератор+бойлер" (общий кирпич ISO/COLD/CHAIN): анкер -> генератор (face UP) -> бойлер (face UP,
	 *  прямо на генераторе). Перед-грань генератора (реальный гейт onTick2 :113-114 hasCollide/oxygen) расчищена
	 *  в AIR по ЖИВОМУ mFacing (публичное поле TileEntityBase09FacingSingle.java:45) — не судимый канал, топология. */
	private static Object[] gt6EnergyChainProbeBuildGenBoiler(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aGround, String aLabel) {
		gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick tGen = gregapi.probe.GT6ProbeStand.place(
			aLevel, sECPPlayer, aGround, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(ECP_GEN_ID),
			gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick.class, ECP_M, aLabel + "-генератор");
		if (tGen == null) return new Object[]{null, null};
		net.minecraft.core.BlockPos tGenPos = aGround.above();
		net.minecraft.core.Direction tFront = FORGE_DIR[tGen.mFacing];
		aLevel.setBlock(tGenPos.relative(tFront), Blocks.AIR.defaultBlockState(), 3);
		gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank tBoiler = gregapi.probe.GT6ProbeStand.place(
			aLevel, sECPPlayer, tGenPos, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(ECP_BOILER_ID),
			gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank.class, ECP_M, aLabel + "-бойлер");
		return new Object[]{tGen, tBoiler};
	}

	/** Тик 200: постройка ISO/COLD/CHAIN (вертикальные столбы) + MOTOR (горизонтальная линия). Любой обрыв ->
	 *  RuntimeException -> Seq печатает EXC. */
	private static void gt6EnergyChainProbeBuild() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = sECPPlayer.level();
		O.println("========== [" + ECP_M + "] Связка №4 — энерго-лестница (Ф3.1, на каркасе GT6ProbeStand) ==========");
		MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		int[] tIds = {ECP_GEN_ID, ECP_BOILER_ID, ECP_TURBINE_ID, ECP_DYNAMO_ID, ECP_BATBOX_RECV_ID, ECP_BATBOX_SRC_ID, ECP_MOTOR_ID, ECP_AXLE_ID};
		for (int tId : tIds) if (tReg == null || tReg.getClassContainer(tId) == null) throw new RuntimeException("реестр/ID не найден: " + tId);
		O.println("[" + ECP_M + "] ID подтверждены: генератор=" + tReg.getClassContainer(ECP_GEN_ID).mClass.getSimpleName() + "(" + ECP_GEN_ID + ") бойлер=" + tReg.getClassContainer(ECP_BOILER_ID).mClass.getSimpleName() + "(" + ECP_BOILER_ID + ") турбина=" + tReg.getClassContainer(ECP_TURBINE_ID).mClass.getSimpleName() + "(" + ECP_TURBINE_ID + ") динамо=" + tReg.getClassContainer(ECP_DYNAMO_ID).mClass.getSimpleName() + "(" + ECP_DYNAMO_ID + ") батарея-ULV=" + tReg.getClassContainer(ECP_BATBOX_RECV_ID).mClass.getSimpleName() + "(" + ECP_BATBOX_RECV_ID + ") батарея-LV=" + tReg.getClassContainer(ECP_BATBOX_SRC_ID).mClass.getSimpleName() + "(" + ECP_BATBOX_SRC_ID + ") мотор=" + tReg.getClassContainer(ECP_MOTOR_ID).mClass.getSimpleName() + "(" + ECP_MOTOR_ID + ") вал=" + tReg.getClassContainer(ECP_AXLE_ID).mClass.getSimpleName() + "(" + ECP_AXLE_ID + ")");

		net.minecraft.core.BlockPos tBaseIso   = sECPPlayer.blockPosition().offset(4, 0,  4);
		net.minecraft.core.BlockPos tBaseCold  = sECPPlayer.blockPosition().offset(4, 0, 10);
		net.minecraft.core.BlockPos tBaseChain = sECPPlayer.blockPosition().offset(4, 0, 16);
		net.minecraft.core.BlockPos tBaseMotor = sECPPlayer.blockPosition().offset(4, 0, 22);

		Object[] tIso  = gt6EnergyChainProbeBuildGenBoiler(tLevel, tBaseIso,   "ISO");
		Object[] tCold = gt6EnergyChainProbeBuildGenBoiler(tLevel, tBaseCold,  "COLD");
		Object[] tChn  = gt6EnergyChainProbeBuildGenBoiler(tLevel, tBaseChain, "CHAIN");
		sECPIsoGen   = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick) tIso[0];  sECPIsoBoiler   = (gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank) tIso[1];
		sECPColdGen  = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick) tCold[0]; sECPColdBoiler  = (gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank) tCold[1];
		sECPChainGen = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick) tChn[0];  sECPChainBoiler = (gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank) tChn[1];
		if (sECPIsoBoiler == null || sECPColdBoiler == null || sECPChainBoiler == null) throw new RuntimeException("постройка генератор+бойлер не удалась (null в цепочке)");

		net.minecraft.core.BlockPos tChainBoilerPos = tBaseChain.above().above();
		sECPChainTurbine = gregapi.probe.GT6ProbeStand.place(tLevel, sECPPlayer, tChainBoilerPos, net.minecraft.core.Direction.UP,
			gregapi.probe.GT6ProbeStand.mteStack(ECP_TURBINE_ID), gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam.class, ECP_M, "CHAIN-турбина");
		if (sECPChainTurbine == null) throw new RuntimeException("турбина не встала");
		sECPChainTurbine.setPrimaryFacing(SIDE_UP); // приём Steam снизу (OPOS[UP]=DOWN, от бойлера), эмиссия RU вверх — TurbineSteam.java:129-130
		net.minecraft.core.BlockPos tTurbinePos = tChainBoilerPos.above();
		sECPChainDynamo = gregapi.probe.GT6ProbeStand.place(tLevel, sECPPlayer, tTurbinePos, net.minecraft.core.Direction.UP,
			gregapi.probe.GT6ProbeStand.mteStack(ECP_DYNAMO_ID), gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric.class, ECP_M, "CHAIN-динамо");
		if (sECPChainDynamo == null) throw new RuntimeException("динамо не встало");
		sECPChainDynamo.setPrimaryFacing(SIDE_UP); // приём RU снизу, эмиссия EU вверх — DynamoElectric.java:35-36
		net.minecraft.core.BlockPos tDynamoPos = tTurbinePos.above();
		sECPChainBatBox = gregapi.probe.GT6ProbeStand.place(tLevel, sECPPlayer, tDynamoPos, net.minecraft.core.Direction.UP,
			gregapi.probe.GT6ProbeStand.mteStack(ECP_BATBOX_RECV_ID), gregapi.tileentity.energy.TileEntityBase10EnergyBatBox.class, ECP_M, "CHAIN-батарея-приёмник");
		if (sECPChainBatBox == null) throw new RuntimeException("батарея-приёмник не встала");
		sECPChainBatBox.setPrimaryFacing(SIDE_NORTH); // isInput=aSide!=mFacing (:232) -> принимает снизу (DOWN!=NORTH)

		sECPMotorSrc = gregapi.probe.GT6ProbeStand.place(tLevel, sECPPlayer, tBaseMotor, net.minecraft.core.Direction.EAST,
			gregapi.probe.GT6ProbeStand.mteStack(ECP_BATBOX_SRC_ID), gregapi.tileentity.energy.TileEntityBase10EnergyBatBox.class, ECP_M, "MOTOR-батарея-источник");
		if (sECPMotorSrc == null) throw new RuntimeException("батарея-источник (MOTOR) не встала");
		sECPMotorSrc.setPrimaryFacing(SIDE_EAST); // эмиссия EU на восток, к мотору
		net.minecraft.core.BlockPos tSrcPos = tBaseMotor.relative(net.minecraft.core.Direction.EAST);
		sECPMotor = gregapi.probe.GT6ProbeStand.place(tLevel, sECPPlayer, tSrcPos, net.minecraft.core.Direction.EAST,
			gregapi.probe.GT6ProbeStand.mteStack(ECP_MOTOR_ID), gregtech.tileentity.energy.converters.MultiTileEntityMotorElectric.class, ECP_M, "MOTOR-мотор");
		if (sECPMotor == null) throw new RuntimeException("мотор не встал");
		sECPMotor.setPrimaryFacing(SIDE_EAST); // приём EU с запада (isInput=aSide!=mFacing), эмиссия RU на восток к валу
		net.minecraft.core.BlockPos tMotorPos = tSrcPos.relative(net.minecraft.core.Direction.EAST);
		sECPMotorPos = tMotorPos;
		sECPAxle = gregapi.probe.GT6ProbeStand.place(tLevel, sECPPlayer, tMotorPos, net.minecraft.core.Direction.EAST,
			gregapi.probe.GT6ProbeStand.mteStack(ECP_AXLE_ID), gregapi.tileentity.connectors.MultiTileEntityAxle.class, ECP_M, "MOTOR-вал");
		if (sECPAxle == null) throw new RuntimeException("вал не встал");
		boolean tAxleConnected = sECPAxle.connect(SIDE_WEST, T); // принять RU с запада (от мотора) — реальный API connect(), как WIREPROBE/ITEMPIPEPROBE (топология, не обход передачи)
		O.println("[" + ECP_M + "] DIAG-MOTOR: вал.connect(WEST,true) вернул=" + tAxleConnected + "; вал.isEnergyAcceptingFrom(RU,WEST,F)=" + sECPAxle.isEnergyAcceptingFrom(TD.Energy.RU, SIDE_WEST, F) + " мотор.isEnergyEmittingTo(RU,EAST,F)=" + sECPMotor.isEnergyEmittingTo(TD.Energy.RU, SIDE_EAST, F));

		O.println("[" + ECP_M + "] живые параметры (из BE, не предположены): генератор mRate=" + gregapi.probe.GT6ProbeStand.fldLong(sECPIsoGen, "mRate") + " mEfficiency=" + gregapi.probe.GT6ProbeStand.fldLong(sECPIsoGen, "mEfficiency") + "; бойлер mOutput=" + gregapi.probe.GT6ProbeStand.fldLong(sECPIsoBoiler, "mOutput") + " mCapacity=" + gregapi.probe.GT6ProbeStand.fldLong(sECPIsoBoiler, "mCapacity"));
		O.println("[" + ECP_M + "] турбина(CHAIN) mEnergyIN(min/rec/max)=" + sECPChainTurbine.mConverter.mEnergyIN.mMin + "/" + sECPChainTurbine.mConverter.mEnergyIN.mRec + "/" + sECPChainTurbine.mConverter.mEnergyIN.mMax + " mEnergyOUT(min/rec/max)=" + sECPChainTurbine.mConverter.mEnergyOUT.mMin + "/" + sECPChainTurbine.mConverter.mEnergyOUT.mRec + "/" + sECPChainTurbine.mConverter.mEnergyOUT.mMax);
		O.println("[" + ECP_M + "] динамо(CHAIN) mEnergyIN.mMin=" + sECPChainDynamo.mConverter.mEnergyIN.mMin + " mEnergyOUT(min/rec/max)=" + sECPChainDynamo.mConverter.mEnergyOUT.mMin + "/" + sECPChainDynamo.mConverter.mEnergyOUT.mRec + "/" + sECPChainDynamo.mConverter.mEnergyOUT.mMax + "; батарея-приёмник mInput=" + sECPChainBatBox.mInput);
		O.println("[" + ECP_M + "] мотор(MOTOR) mEnergyIN.mMin=" + sECPMotor.mConverter.mEnergyIN.mMin + " mEnergyOUT(min/rec/max)=" + sECPMotor.mConverter.mEnergyOUT.mMin + "/" + sECPMotor.mConverter.mEnergyOUT.mRec + "/" + sECPMotor.mConverter.mEnergyOUT.mMax + "; батарея-источник mOutput=" + sECPMotorSrc.mOutput + "; вал mSpeed/mPower=" + sECPAxle.mSpeed + "/" + sECPAxle.mPower);
	}

	/** Тик 210: разжечь ISO+CHAIN (топливо+вода), НЕ трогать COLD (кроме воды — отличие ТОЛЬКО в горении), пред-
	 *  зарядить паровой буфер CHAIN чуть ниже порога излучения соседу (см. комментарий блока выше — сетап резервуара,
	 *  не обход конверсии/эмиссии). */
	private static void gt6EnergyChainProbeLoad() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		gregapi.probe.GT6ProbeStand.fldSet(sECPIsoGen, "mBurning", T);
		gregapi.probe.GT6ProbeStand.slotSet(sECPIsoGen, 0, ST.make(Items.COAL, 32, 0));
		gt6EnergyChainProbeBoilerFill(sECPIsoBoiler, 0, "water", 4000);
		sECPIsoWater0 = gt6EnergyChainProbeBoilerAmount(sECPIsoBoiler, 0);
		sECPIsoFuel0  = gregapi.probe.GT6ProbeStand.slotCount(sECPIsoGen, 0);

		gt6EnergyChainProbeBoilerFill(sECPColdBoiler, 0, "water", 4000); // воду даём тоже — единственное отличие COLD от ISO есть mBurning/топливо
		sECPColdWater0 = gt6EnergyChainProbeBoilerAmount(sECPColdBoiler, 0);

		gregapi.probe.GT6ProbeStand.fldSet(sECPChainGen, "mBurning", T);
		gregapi.probe.GT6ProbeStand.slotSet(sECPChainGen, 0, ST.make(Items.COAL, 32, 0));
		gt6EnergyChainProbeBoilerFill(sECPChainBoiler, 0, "water", 4000);
		long tChainCap = gregapi.probe.GT6ProbeStand.fldLong(sECPChainBoiler, "mCapacity"); // живое поле этого экземпляра = mOutput×10000 (:79)
		// предзаряд ВЫШЕ порога эмиссии (:139-140 бойлер отдаёт вверх только излишек сверх cap/2): +100000 даёт
		// устойчивый поток пара с первого тика — производство бойлера судится фазой 4a, 4b судит КОНВЕРСИЮ цепи
		// (прежний "чуть ниже порога" оставлял турбине капельный приток — артефакт замера, не дефект порта)
		long tPrecharge = tChainCap / 2 + 100000;
		gt6EnergyChainProbeBoilerFill(sECPChainBoiler, 1, "steam", tPrecharge);
		sECPChainEu0 = sECPChainBatBox.mEnergy;
		sECPChainSteamCounter0 = sECPChainTurbine.mSteamCounter;

		O.println("[" + ECP_M + "] тик 210 загрузка: ISO вода0=" + sECPIsoWater0 + " топливо0=" + sECPIsoFuel0 + "; COLD вода0=" + sECPColdWater0 + " (НЕ разожжён, топлива нет); CHAIN cap=" + tChainCap + " пред-заряд_пара=" + tPrecharge + " (порог излучения :139 = cap/2=" + (tChainCap / 2) + ") eu0=" + sECPChainEu0 + " steamCounter0=" + sECPChainSteamCounter0);
	}

	/** Каждый тик (окно 200..1300): держим батарею-источник MOTOR заряженной (сетап-обход ТОЛЬКО инвентарной
	 *  бухгалтерии батарей, как в WIREPROBE gt6WireProbeApplyFields — не передачи). */
	private static void gt6EnergyChainProbeApplyMotorSrcFields() {
		if (sECPMotorSrc == null) return;
		sECPMotorSrc.mEnergy = 1_000_000_000L; sECPMotorSrc.mBatteryCount = 1; sECPMotorSrc.mChargeableCount = 0; sECPMotorSrc.mStopped = F; sECPMotorSrc.mMode = 0;
		// CHAIN-приёмник: mReceivablePower строится из mChargeableCount (:153), без форса = 0 → doInject молча
		// возвращает 0 (:179) → динамо не изливается → ПОДПОР всей цепи (турбина съела 103mb и встала — корень
		// FAIL 4b прогона run6/final1). Тот же сетап-обход «батареи вставлены», что у WIREPROBE-приёмника.
		if (sECPChainBatBox != null) {sECPChainBatBox.mChargeableCount = 1000; sECPChainBatBox.mBatteryCount = 0; sECPChainBatBox.mStopped = F;}
	}

	/** DIAG (§6.1, лестница хопов): каждый стык прогоняется ВРУЧНУЮ из пробы на живых BE (форс накопителя →
	 *  реальный публичный Util.emitEnergyToNetwork → чтение приёмника) — изолирует мёртвый хоп. Приём DIAG-MOTOR
	 *  («РЕАЛЬНЫЙ emitEnergyToNetwork вызванный ИЗ ПРОБЫ»); не судимый канал, только диагностика. Снять при уборке фазы. */
	private static void gt6EnergyChainProbeDiagChainHops() {
		if (sECPChainTurbine == null || sECPChainDynamo == null || sECPChainBatBox == null) return;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		// ХОП 1: турбина -RU-> динамо
		sECPChainTurbine.mStorage.mEnergy = 96;
		long tP1 = gregapi.tileentity.energy.ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.RU, 32, 1, sECPChainTurbine);
		O.println("[" + ECP_M + "] DIAG-CHAIN-HOP1 турбина(форс storage=96)-RU(32×1)->сеть: пакетов=" + tP1 + " динамо.mStorage ПОСЛЕ=" + sECPChainDynamo.mStorage.mEnergy);
		// ХОП 2: динамо -EU-> батарея
		sECPChainDynamo.mStorage.mEnergy = 64;
		long tEu0 = sECPChainBatBox.mEnergy;
		long tP2 = gregapi.tileentity.energy.ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, 22, 1, sECPChainDynamo);
		O.println("[" + ECP_M + "] DIAG-CHAIN-HOP2 динамо(форс storage=64)-EU(22×1)->сеть: пакетов=" + tP2 + " батарея.mEnergy " + tEu0 + "->" + sECPChainBatBox.mEnergy + " (receivable=" + sECPChainBatBox.mReceivablePower + ")");
		sECPChainTurbine.mStorage.mEnergy = 0; sECPChainDynamo.mStorage.mEnergy = 0; // вернуть как было — дальше живой цикл
		// §6.1 identity-проверка ТРОЙКИ CHAIN (класс «протухшая ссылка», память gt6-mismatch-flood-not-orphans):
		// пересозданный движком BE стартует с mEnergy=0/receivable=0 — форсы и ручные хопы уходят в мёртвую копию
		net.minecraft.world.level.block.entity.BlockEntity tFreshT = sECPPlayer.level().getBlockEntity(sECPChainTurbine.getBlockPos());
		net.minecraft.world.level.block.entity.BlockEntity tFreshD = sECPPlayer.level().getBlockEntity(sECPChainDynamo.getBlockPos());
		net.minecraft.world.level.block.entity.BlockEntity tFreshB = sECPPlayer.level().getBlockEntity(sECPChainBatBox.getBlockPos());
		O.println("[" + ECP_M + "] DIAG-CHAIN-IDENTITY: турбина СОВПАДАЕТ=" + (tFreshT == sECPChainTurbine) + " динамо СОВПАДАЕТ=" + (tFreshD == sECPChainDynamo) + " батарея СОВПАДАЕТ=" + (tFreshB == sECPChainBatBox)
			+ (tFreshB instanceof gregapi.tileentity.energy.TileEntityBase10EnergyBatBox tFB ? " СВЕЖАЯ батарея.mEnergy=" + tFB.mEnergy + " .mReceivablePower=" + tFB.mReceivablePower + " .mChargeableCount=" + tFB.mChargeableCount : " свежая батарея НЕ BatBox: " + (tFreshB == null ? "null" : tFreshB.getClass().getSimpleName())));
	}

	/** DIAG (§6.1): по-тиковая трасса стыка CHAIN турбина→динамо→батарея (тики 211..240) + полные IN/OUT-тройки
	 *  обоих конверторов на первом тике — ищем, где именно глохнет RU/EU (гипотеза: реальный пакет турбины >
	 *  входного максимума динамо, симуляция size=16 этот случай не кроет). Снять при уборке фазы. */
	private static void gt6EnergyChainProbeDiagChainTrace() {
		if (sECPChainTurbine == null || sECPChainDynamo == null || sECPChainBatBox == null) return;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		if (sECPProbeTick == 211) {
			O.println("[" + ECP_M + "] DIAG-CHAIN-TRACE тройки: турбина IN(steam)=" + sECPChainTurbine.mConverter.mEnergyIN.mMin + "/" + sECPChainTurbine.mConverter.mEnergyIN.mRec + "/" + sECPChainTurbine.mConverter.mEnergyIN.mMax
				+ " OUT(RU)=" + sECPChainTurbine.mConverter.mEnergyOUT.mMin + "/" + sECPChainTurbine.mConverter.mEnergyOUT.mRec + "/" + sECPChainTurbine.mConverter.mEnergyOUT.mMax
				+ "; динамо IN(RU)=" + sECPChainDynamo.mConverter.mEnergyIN.mMin + "/" + sECPChainDynamo.mConverter.mEnergyIN.mRec + "/" + sECPChainDynamo.mConverter.mEnergyIN.mMax
				+ " OUT(EU)=" + sECPChainDynamo.mConverter.mEnergyOUT.mMin + "/" + sECPChainDynamo.mConverter.mEnergyOUT.mRec + "/" + sECPChainDynamo.mConverter.mEnergyOUT.mMax);
		}
		O.println("[" + ECP_M + "] DIAG-CHAIN-TRACE тик " + sECPProbeTick + ": турбина.mTank=" + sECPChainTurbine.mTank.amount() + " турбина.mStorage=" + sECPChainTurbine.mStorage.mEnergy + " турбина.canEmit=" + sECPChainTurbine.mConverter.mCanEmitEnergy
			+ " динамо.mStorage=" + sECPChainDynamo.mStorage.mEnergy + " динамо.canEmit=" + sECPChainDynamo.mConverter.mCanEmitEnergy
			+ " батарея.mChargeableCount=" + sECPChainBatBox.mChargeableCount + " батарея.mReceivablePower=" + sECPChainBatBox.mReceivablePower + " батарея.mEnergy=" + sECPChainBatBox.mEnergy
			+ " ТИХИЙ-ПЕРЕГРУЗ: турбина.mExplosionPrevention=" + gregapi.probe.GT6ProbeStand.fldLong(sECPChainTurbine, "mExplosionPrevention") + " динамо.mExplosionPrevention=" + gregapi.probe.GT6ProbeStand.fldLong(sECPChainDynamo, "mExplosionPrevention")
			+ " турбина.emits=" + sECPChainTurbine.mConverter.mEmitsEnergy + " динамо.emits=" + sECPChainDynamo.mConverter.mEmitsEnergy);
	}

	/** DIAG (§6.1): по-тиковая трассировка 211..225 — стабильно ли держится src.mBatteryCount ПОСЛЕ форсирования
	 *  через реальный BE-тик (наш Pre-хук форсирует ДО тика, здесь читаем ПОСЛЕ). Снять при уборке фазы. */
	private static void gt6EnergyChainProbeDiagMotorTrace() {
		if (sECPMotorSrc == null || sECPMotor == null || sECPAxle == null) return;
		gregapi.data.CS.OUT.println("[" + ECP_M + "] DIAG-MOTOR-TRACE тик " + sECPProbeTick + ": src.mBatteryCount=" + sECPMotorSrc.mBatteryCount + " src.mChargeableCount=" + sECPMotorSrc.mChargeableCount + " src.mEmitsEnergy=" + sECPMotorSrc.mEmitsEnergy + " src.mEnergy=" + sECPMotorSrc.mEnergy + " мотор.mStorage.mEnergy=" + sECPMotor.mStorage.mEnergy + " мотор.mConverter.mCanEmitEnergy=" + sECPMotor.mConverter.mCanEmitEnergy + " мотор.mConverter.mEmitsEnergy=" + sECPMotor.mConverter.mEmitsEnergy + " вал.mTransferredLast=" + sECPAxle.mTransferredLast + " вал.mTransferredEnergy=" + sECPAxle.mTransferredEnergy + " вал.mTimer=" + gregapi.probe.GT6ProbeStand.fldLong(sECPAxle, "mTimer") + " мотор.mTimer=" + gregapi.probe.GT6ProbeStand.fldLong(sECPMotor, "mTimer") + " src.mTimer=" + gregapi.probe.GT6ProbeStand.fldLong(sECPMotorSrc, "mTimer"));
	}

	/** DIAG (§6.1 локализация): почему EU из батареи-источника не доходит до mStorage мотора — печать живых
	 *  булевых гейтов ОБОИХ концов + прямой вызов реальных публичных isEnergy* методов (не судимый канал, только
	 *  диагностика; сам перенос энергии этими вызовами НЕ подменяется). Снять при уборке фазы. */
	private static void gt6EnergyChainProbeDiagMotor() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + ECP_M + "] DIAG-MOTOR: src.mActive=" + sECPMotorSrc.mActive + " src.mBatteryCount=" + sECPMotorSrc.mBatteryCount + " src.mChargeableCount=" + sECPMotorSrc.mChargeableCount + " src.mStopped=" + sECPMotorSrc.mStopped + " src.mFacing=" + sECPMotorSrc.mFacing + " src.mEmitsEnergy=" + sECPMotorSrc.mEmitsEnergy + " src.mEnergyTypeOut=" + sECPMotorSrc.mEnergyTypeOut);
		O.println("[" + ECP_M + "] DIAG-MOTOR: src.isEnergyEmittingTo(EU,EAST,F)=" + sECPMotorSrc.isEnergyEmittingTo(TD.Energy.EU, SIDE_EAST, F) + " src.isEnergyEmittingTo(EU,EAST,T)=" + sECPMotorSrc.isEnergyEmittingTo(TD.Energy.EU, SIDE_EAST, T));
		O.println("[" + ECP_M + "] DIAG-MOTOR: motor.mFacing=" + sECPMotor.mFacing + " motor.mStopped=" + gregapi.probe.GT6ProbeStand.fldBool(sECPMotor, "mStopped") + " motor.mConverter.mEnergyIN.mType=" + sECPMotor.mConverter.mEnergyIN.mType + " motor.mConverter.mWasteEnergy=" + sECPMotor.mConverter.mWasteEnergy + " motor.mStorage.mEnergy=" + sECPMotor.mStorage.mEnergy + " motor.mStorage.mCapacity=" + sECPMotor.mStorage.mCapacity);
		O.println("[" + ECP_M + "] DIAG-MOTOR: motor.isEnergyAcceptingFrom(EU,WEST,F)=" + sECPMotor.isEnergyAcceptingFrom(TD.Energy.EU, SIDE_WEST, F) + " motor.isEnergyType(EU,WEST,F)=" + sECPMotor.isEnergyType(TD.Energy.EU, SIDE_WEST, F) + " motor.getEnergySizeInputMin(EU,WEST)=" + sECPMotor.getEnergySizeInputMin(TD.Energy.EU, SIDE_WEST));
		long tSim = sECPMotor.doEnergyInjection(TD.Energy.EU, SIDE_WEST, 32, 1, F); // симуляция (aDoInject=F) — не меняет состояние, только проверка гейта
		O.println("[" + ECP_M + "] DIAG-MOTOR: СИМУЛЯЦИЯ motor.doEnergyInjection(EU,WEST,size=32,amount=1,doInject=F)=" + tSim + " (ожидание >0, если гейт открыт)");
		gregapi.tileentity.delegate.DelegatorTileEntity<net.minecraft.world.level.block.entity.BlockEntity> tAdjSrc = sECPMotorSrc.getAdjacentTileEntity(SIDE_EAST);
		gregapi.tileentity.delegate.DelegatorTileEntity<net.minecraft.world.level.block.entity.BlockEntity> tAdjMotor = sECPMotor.getAdjacentTileEntity(SIDE_WEST);
		O.println("[" + ECP_M + "] DIAG-MOTOR: src.getAdjacentTileEntity(EAST).mTileEntity=" + (tAdjSrc.mTileEntity == null ? "null" : tAdjSrc.mTileEntity.getClass().getSimpleName()) + " (ожидание MultiTileEntityMotorElectric); motor.getAdjacentTileEntity(WEST).mTileEntity=" + (tAdjMotor.mTileEntity == null ? "null" : tAdjMotor.mTileEntity.getClass().getSimpleName()) + " (ожидание TileEntityBase10EnergyBatBox-наследник)");
		long tReal = gregapi.tileentity.energy.ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, sECPMotorSrc.mOutput, 1, sECPMotorSrc);
		O.println("[" + ECP_M + "] DIAG-MOTOR: РЕАЛЬНЫЙ emitEnergyToNetwork(EU,size=" + sECPMotorSrc.mOutput + ",amount=1) вызванный ИЗ ПРОБЫ (не БЕ-тик) вернул=" + tReal + "; мотор.mStorage.mEnergy ПОСЛЕ=" + sECPMotor.mStorage.mEnergy);
		// §6.1: сверка "протухшей ссылки" — тот ли объект BE тикает по факту, что мы захватили при постройке (память gt6-mismatch-flood-not-orphans/gt6-eye-report-n1-n6)
		net.minecraft.world.level.block.entity.BlockEntity tFreshBE = sECPPlayer.level().getBlockEntity(sECPMotorPos);
		O.println("[" + ECP_M + "] DIAG-MOTOR: свежий getBlockEntity(motorPos)=" + (tFreshBE == null ? "null" : tFreshBE.getClass().getSimpleName() + "@" + System.identityHashCode(tFreshBE)) + " захваченный sECPMotor@" + System.identityHashCode(sECPMotor) + " СОВПАДАЕТ=" + (tFreshBE == sECPMotor) + (tFreshBE instanceof gregtech.tileentity.energy.converters.MultiTileEntityMotorElectric tFreshMotor ? " свежий.mStorage.mEnergy=" + tFreshMotor.mStorage.mEnergy : ""));
	}

	/** Тик 260: ФАЗА 4c — батарея-источник -> мотор -> вал (EU->RU), независимая от прогрева котла линия.
	 *  ВАЖНО (урок §7 манифеста «кратковременные эффекты — Seq.watch»): MultiTileEntityAxle.transferRotations
	 *  (:105-117) обнуляет mTransferredEnergy обратно в семантике "реле" — реальный ненулевой перенос
	 *  регистрируется только на mTimer<1/oRotationDir==0 первом вызове ЛИБО когда canEmitEnergyTo(противоположная
	 *  сторона)==true (эстафета ДАЛЬШЕ); тупиковый вал (ничего не подключено на дальней стороне) после первого
	 *  тика КАЖДЫЙ следующий вызов передаёт aPower=0 в addToEnergyTransferred (:114,116) — однократный снимок в
	 *  конце окна лжёт (тот же класс дефекта, что HOT в FLUIDPIPEPROBE). Читаем через Seq.watch(окно 205..259),
	 *  проба видит mTransferredEnergy>0 ИЛИ mRotationDir!=0 (последнее выставляется БЕЗУСЛОВНО на каждом реальном
	 *  вызове :110, до гейта эстафеты) — если проба видела ХОТЯ БЫ РАЗ, реальная передача была. */
	private static void gt6EnergyChainProbeJudge4c() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + ECP_M + "] ===== ФАЗА 4c: батарея-источник → мотор → вал (EU→RU) =====");
		long tRu = sECPAxle.mTransferredLast;
		long tMotorOutMax = sECPMotor.mConverter.mEnergyOUT.mMax;
		boolean tEverTransferred = sECPSeq.everSeen("4c-ru");
		O.println("[" + ECP_M + "] 4c: вал.mTransferredLast(снимок сейчас)=" + tRu + " вал.mRotationDir(сейчас)=" + sECPAxle.mRotationDir + " everSeen(4c-ru, окно 205..259)=" + tEverTransferred + " (мотор mEnergyOUT min/rec/max=" + sECPMotor.mConverter.mEnergyOUT.mMin + "/" + sECPMotor.mConverter.mEnergyOUT.mRec + "/" + tMotorOutMax + "; мотор.mStorage.mEnergy=" + sECPMotor.mStorage.mEnergy + " mConverter.mEmitsEnergy=" + sECPMotor.mConverter.mEmitsEnergy + ")");
		sECPSeq.judge("4c мотор эмитирует RU на вал (реальная эмиссия дошла хотя бы раз в окне)", tEverTransferred, T, tEverTransferred);
		sECPSeq.judge("4c RU-эмиссия в пределах mEnergyOUT.mMax мотора (снимок не превышает потолок)", tRu <= tMotorOutMax, "<=" + tMotorOutMax, tRu);
	}

	/** Тик 500: ФАЗА 4a — горелка+топливо → бойлер с водой, ИЗОЛИРОВАННО (без турбины сверху). */
	private static void gt6EnergyChainProbeJudge4a() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + ECP_M + "] ===== ФАЗА 4a: горелка+топливо → бойлер с водой (HU→Steam, изолировано) =====");
		long tWaterNow = gt6EnergyChainProbeBoilerAmount(sECPIsoBoiler, 0);
		long tSteamNow = gt6EnergyChainProbeBoilerAmount(sECPIsoBoiler, 1);
		long tFuelNow  = gregapi.probe.GT6ProbeStand.slotCount(sECPIsoGen, 0);
		long tWaterConsumed = sECPIsoWater0 - tWaterNow;
		long tFuelConsumed  = sECPIsoFuel0 - tFuelNow;
		boolean tBurning = gregapi.probe.GT6ProbeStand.fldBool(sECPIsoGen, "mBurning");
		long tGenEnergy = gregapi.probe.GT6ProbeStand.fldLong(sECPIsoGen, "mEnergy");
		long tBoilerEnergy = gregapi.probe.GT6ProbeStand.fldLong(sECPIsoBoiler, "mEnergy");
		Object tLastRecipe = gregapi.probe.GT6ProbeStand.fld(sECPIsoGen, "mLastRecipe");
		O.println("[" + ECP_M + "] 4a: топливо0=" + sECPIsoFuel0 + " сейчас=" + tFuelNow + " (расход=" + tFuelConsumed + "); вода0=" + sECPIsoWater0 + " сейчас=" + tWaterNow + " (расход=" + tWaterConsumed + "); пар=" + tSteamNow + "; mBurning=" + tBurning + " генератор.mEnergy=" + tGenEnergy + " бойлер.mEnergy=" + tBoilerEnergy + " mLastRecipe=" + tLastRecipe);
		sECPSeq.judge("4a топливо расходуется (реальное горение)", tFuelConsumed > 0, ">0", tFuelConsumed);
		sECPSeq.judge("4a вода расходуется (реальная конверсия)", tWaterConsumed > 0, ">0", tWaterConsumed);
		sECPSeq.judge("4a пар произведён", tSteamNow > 0, ">0", tSteamNow);
		if (tWaterConsumed > 0) {
			long tExpMin = tWaterConsumed * 80;  // MultiTileEntityBoilerTank.java:120-123 — mEfficiency∈[5000,10000] (калcификация), пар=вода×mEfficiency×160/10000 ⇒ [вода×80..вода×160]
			long tExpMax = tWaterConsumed * 160;
			sECPSeq.judge("4a пар в формульных пределах [вода×80..160] (калcификация :120-123)", tSteamNow >= tExpMin && tSteamNow <= tExpMax, "[" + tExpMin + ".." + tExpMax + "]", tSteamNow);
		}
	}

	/** Тик 1200: CONTROL-NEG (COLD — без розжига/топлива, ничего не тикает без входа) + ФАЗА 4b (полная цепь
	 *  Steam→RU→EU) + DONE. */
	/** DIAG (§6.1): состояние CHAIN на середине окна — почему EU в батарее-приёмнике не растёт, хотя турбина
	 *  обрабатывает пар. Снять при уборке фазы. */
	private static void gt6EnergyChainProbeDiagChain() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		long tSteamNow = gt6EnergyChainProbeBoilerAmount(sECPChainBoiler, 1);
		O.println("[" + ECP_M + "] DIAG-CHAIN: бойлер.tank1(пар)=" + tSteamNow + " (порог :139=" + (gregapi.probe.GT6ProbeStand.fldLong(sECPChainBoiler, "mCapacity") / 2) + ") турбина.mTank(вход)=" + sECPChainTurbine.mTank.amount() + " турбина.mSteamCounter=" + sECPChainTurbine.mSteamCounter + " турбина.mStorage.mEnergy=" + sECPChainTurbine.mStorage.mEnergy + " турбина.mConverter.mCanEmitEnergy=" + sECPChainTurbine.mConverter.mCanEmitEnergy + " турбина.mConverter.mEmitsEnergy=" + sECPChainTurbine.mConverter.mEmitsEnergy + " турбина.mFacing=" + sECPChainTurbine.mFacing);
		O.println("[" + ECP_M + "] DIAG-CHAIN: динамо.mFacing=" + sECPChainDynamo.mFacing + " динамо.mStorage.mEnergy=" + sECPChainDynamo.mStorage.mEnergy + " динамо.mConverter.mCanEmitEnergy=" + sECPChainDynamo.mConverter.mCanEmitEnergy + " динамо.mConverter.mEmitsEnergy=" + sECPChainDynamo.mConverter.mEmitsEnergy + " батарея-приёмник.mEnergy=" + sECPChainBatBox.mEnergy + " батарея-приёмник.mFacing=" + sECPChainBatBox.mFacing);
		long tSim = sECPChainDynamo.doEnergyInjection(TD.Energy.RU, SIDE_DOWN, 16, 1, F);
		O.println("[" + ECP_M + "] DIAG-CHAIN: СИМУЛЯЦИЯ динамо.doEnergyInjection(RU,DOWN,size=16,amount=1,doInject=F)=" + tSim + " (ожидание >0, если гейт открыт)");
	}

	private static void gt6EnergyChainProbeJudgeFinal() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + ECP_M + "] ===== CONTROL-NEG: COLD (без розжига/топлива) =====");
		long tColdWaterNow = gt6EnergyChainProbeBoilerAmount(sECPColdBoiler, 0);
		long tColdSteamNow = gt6EnergyChainProbeBoilerAmount(sECPColdBoiler, 1);
		boolean tColdBurning = gregapi.probe.GT6ProbeStand.fldBool(sECPColdGen, "mBurning");
		O.println("[" + ECP_M + "] COLD (тик 1200): mBurning=" + tColdBurning + " вода0=" + sECPColdWater0 + " сейчас=" + tColdWaterNow + " пар=" + tColdSteamNow);
		sECPSeq.judge("COLD не горит (mBurning=false, розжига не было)", !tColdBurning, F, tColdBurning);
		sECPSeq.judge("COLD вода не тронута (нет входа — ничего не тикает)", tColdWaterNow == sECPColdWater0, sECPColdWater0, tColdWaterNow);
		sECPSeq.judge("COLD пар не произведён", tColdSteamNow == 0, 0, tColdSteamNow);

		O.println("[" + ECP_M + "] ===== ФАЗА 4b: бойлер → турбина → динамо → батарея (Steam→RU→EU, полная цепь) =====");
		long tEuNow = sECPChainBatBox.mEnergy;
		long tSteamCounterNow = sECPChainTurbine.mSteamCounter;
		long tEuDelta = tEuNow - sECPChainEu0;
		long tSteamDelta = tSteamCounterNow - sECPChainSteamCounter0; // ВНИМАНИЕ: mSteamCounter %= STEAM_PER_WATER(200) при переливе (:98-106 дистиллят) — дельта занижена при переполнении за окно, судья по ней — мягкий, не точный расход
		boolean tEuEverSeen = sECPSeq.everSeen("4b-eu");
		boolean tDynStorageEverSeen = sECPSeq.everSeen("4b-dynamo-storage");
		boolean tTurbineStorageEverSeen = sECPSeq.everSeen("4b-turbine-storage");
		O.println("[" + ECP_M + "] 4b: батарея-приёмник.mEnergy: было=" + sECPChainEu0 + " стало=" + tEuNow + " (прирост=" + tEuDelta + ", everSeen>0 за окно=" + tEuEverSeen + "); турбина.mSteamCounter: было=" + sECPChainSteamCounter0 + " стало=" + tSteamCounterNow + " (обработано пара=" + tSteamDelta + "мб, возможен перелив-обёртка :105); DIAG everSeen динамо.mStorage>0=" + tDynStorageEverSeen + " турбина.mStorage>0=" + tTurbineStorageEverSeen);
		sECPSeq.judge("4b EU в приёмнике выросло хотя бы раз в окне (Seq.watch, урок §7 манифеста)", tEuEverSeen, T, tEuEverSeen);
		sECPSeq.judge("4b турбина реально обработала пар (mSteamCounter вырос)", tSteamDelta > 0, ">0", tSteamDelta);
		if (tSteamDelta > 0) {
			// МЕРА ПАРА ИСПРАВЛЕНА: mSteamCounter-дельта живёт ПО МОДУЛЮ STEAM_PER_WATER (:105, обёртка была
			// помечена в судье с первого прогона) — реального пара в ~50 раз больше, прежний «потолок 63» был
			// сломанной линейкой (ложный FAIL при честном приросте 10242). Честная верхняя граница считается
			// из СЕТАПА (доступный пар системы = предзаряд cap/2+100000 + макс-производство вода×160), а не из
			// обёрнутого счётчика; дельта counter остаётся справочной печатью.
			long tSteamAvail = gregapi.probe.GT6ProbeStand.fldLong(sECPChainBoiler, "mCapacity") / 2 + 100000 + 4000 * 160;
			long tRuMax = tSteamAvail / STEAM_PER_EU; // MultiTileEntityTurbineSteam.java:95 — RU не больше, чем весь пар мог дать
			long tDynIn = sECPChainDynamo.mConverter.mEnergyIN.mRec, tDynOut = sECPChainDynamo.mConverter.mEnergyOUT.mRec;
			long tEuCeil = tRuMax * tDynOut / tDynIn; // TE_Behavior_Energy_Converter.java:62 — верхняя граница КПД динамо (tOutput=mStorage×mEnergyOUT.mRec/mEnergyIN.mRec)
			O.println("[" + ECP_M + "] 4b теоретический потолок (из сетапа, не из модуло-счётчика): пар_доступный=" + tSteamAvail + " RU_max=" + tRuMax + " EU_потолок=RU_max×" + tDynOut + "/" + tDynIn + "=" + tEuCeil);
			sECPSeq.judge("4b EU-прирост <= теоретический потолок цепи (не создаёт энергию из ничего)", tEuDelta <= tEuCeil, "<=" + tEuCeil, tEuDelta);
		}
		sECPSeq.done();
	}

	public static void gt6EnergyChainProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sECPProbeTick++;
		if (aServer.getPlayerList().getPlayers().isEmpty()) return;
		sECPPlayer = aServer.getPlayerList().getPlayers().get(0);
		if (sECPSeq == null) {
			sECPSeq = new gregapi.probe.GT6ProbeStand.Seq(ECP_M)
				.at(200, GT_API_Proxy::gt6EnergyChainProbeBuild)
				.at(210, GT_API_Proxy::gt6EnergyChainProbeLoad)
				.window(211, 225, GT_API_Proxy::gt6EnergyChainProbeDiagMotorTrace)
				.window(211, 240, GT_API_Proxy::gt6EnergyChainProbeDiagChainTrace) // [GT6-ENERGYCHAINPROBE] §6.1-трасса стыка турбина→динамо→батарея — снять при уборке фазы
				// хоп-лестница СНЯТА с таймлайна: её ручные инъекции ЗАГРЯЗНЯЛИ watch «4b-eu» (ложный PASS от
				// собственного вброса 22 EU) — метод gt6EnergyChainProbeDiagChainHops остаётся в арсенале, не зарегистрирован
				.window(200, 1300, GT_API_Proxy::gt6EnergyChainProbeApplyMotorSrcFields)
				.watch("4c-ru", 205, 259, () -> sECPAxle != null && (sECPAxle.mTransferredEnergy > 0 || sECPAxle.mRotationDir != 0))
				.watch("4b-eu", 210, 1199, () -> sECPChainBatBox != null && sECPChainBatBox.mEnergy > 0)
				.watch("4b-dynamo-storage", 210, 1199, () -> sECPChainDynamo != null && sECPChainDynamo.mStorage.mEnergy > 0)
				.watch("4b-turbine-storage", 210, 1199, () -> sECPChainTurbine != null && sECPChainTurbine.mStorage.mEnergy > 0)
				.at(230, GT_API_Proxy::gt6EnergyChainProbeDiagMotor)
				.at(260, GT_API_Proxy::gt6EnergyChainProbeJudge4c)
				.at(500, GT_API_Proxy::gt6EnergyChainProbeJudge4a)
				.at(700, GT_API_Proxy::gt6EnergyChainProbeDiagChain)
				.at(1200, GT_API_Proxy::gt6EnergyChainProbeJudgeFinal);
		}
		sECPSeq.tick(sECPProbeTick);
	}

	// ========== [GT6-CRUCIBLEPROBE] ВРЕМЕННАЯ проба «Связка №5 — тигельный цикл» (Ф3.1, гейт run/gt6crucibleprobe.flag + -Pgt6probes, на каркасе GT6ProbeStand) ==========
	// Цель: доказать ЧИСЛОМ реальную цепь топливо->HU->тигель(нагрев)->плавка->заливка в форму->слиток, КАЖДОЕ
	// звено — реальный GT6-конвертор, тикающий штатно (ни один судимый метод пробой не вызывается напрямую).
	// Этап А (разведка кода, ВЫЧИТАНА, не угадана — оба класса, порт=оригинал посимвольно 1:1, только engine-swap
	// TileEntity->BlockEntity / World->Level / NBTTagCompound->CompoundTag+getXOr):
	// (1) HU-приём: MultiTileEntityCrucible implements ITileEntityEnergy (гл. набор HU/KU/CU/VIS_IGNIS,
	//     ENERGYTYPES:699/717(порт)); isEnergyAcceptingFrom всегда T (без side-гейта) :703/721; getEnergySizeInputMin=1
	//     :706/724 — НЕДОНАПРЯЖЕНИЯ (урок связки №4) тут НЕТ, любой пакет размером >=1 проходит Root.doEnergyInjection
	//     (TileEntityBase01Root.java:886 ориг./:717, посимвольно 1:1). Канал прихода — НЕ прямая грань контроллера,
	//     а СТЕНА MultiTileEntityMultiBlockPart(mode=ONLY_ENERGY_IN): TileEntityBase01Root:729-747(ориг.) даёт
	//     ЛЮБОМУ TileEntity общий проброс doEnergyInjection(aPart,...)->doEnergyInjection(...) (aPart игнорируется),
	//     а сама стена (MultiTileEntityMultiBlockPart.java:542-547) гейтует по mMode ПЕРЕД проброс в контроллер —
	//     только стены нижнего кольца (Y=0 относительно контроллера, mode=ONLY_ENERGY_IN, тултип
	//     "Energy IN from Bottom Layer") пропускают HU; средний слой (ONLY_CRUCIBLE) и верхний (ONLY_ITEM_FLUID)
	//     блокируют (mMode содержит бит NO_ENERGY_IN). doInject (не doEnergyInjection!) — конкретная реализация
	//     контроллера :704/722: HU -> mEnergy += size*amount.
	// (2) Температурная модель: onServerTickPost :184-384(ориг.)/202-402(порт) — tRequiredEnergy=1+weight/100
	//     (KG_PER_ENERGY=100, weight=mMaterial.getWeight(U*100)+содержимое), tConversions=mEnergy/tRequiredEnergy;
	//     если !=0: mEnergy-=conversions*tRequiredEnergy, mTemperature+=conversions (каждая "конверсия"=+1K).
	//     Остывание к envTemp — только через mCooldown<=0 раз в 10 тиков ±1K (:363/381); разрушение (лава+урон)
	//     при mTemperature>getTemperatureMax(SIDE_INSIDE)=mMaterial.mMeltingPoint*1.10 (HEAT_RESISTANCE_BONUS=1.10).
	// (3) Вместимость: MAX_AMOUNT=16*3*3*3*U (:79/97); закладка — addMaterialStacks(List<OreDictMaterialStack>,
	//     aTemperature) :386-408(ориг.)/404-426(порт), правит-но реальный канал закладки — предмет через слот(0)
	//     (WD.suck из 2x3x2 объёма над центром, :204/222) ИЛИ клик/жидкость по верхнему слою (onBlockActivated3);
	//     пробой сетап-обходом (units напрямую, разрешено заданием) вызывается addMaterialStacks САМ (тот же метод,
	//     каким пользуется вся реальная логика — не синтетика внутренностей, а официальный публичный API закладки).
	// (4) Плавка: mTemperature>=tMaterial.mMeltingPoint -> при пересечении порога конверсия в mTargetSmelting (для
	//     чистого металла Sn по умолчанию = сам Sn, OreDictMaterial.java:289 mTargetSmelting=OM.stack(this,U)) —
	//     видимый эффект чистого металла: mDisplayedFluid=mMaterial.mID при T>=melt (:350/368), иначе -1.
	// (5) Заливка в Mold: MultiTileEntityMold.onBlockActivated3 (:267-294) — реальный клик ПУСТОЙ РУКОЙ по ВЕРХНЕЙ
	//     грани формы; клик у центра грани -> getSideWrenching(UP,0.5,_,0.5)=UP=SIDES_VERTICAL -> цикл по всем 4
	//     горизонтальным соседям, у первого ITileEntityCrucible вызывается fillMoldAtSide(this,...) — стена среднего
	//     слоя (mode=ONLY_CRUCIBLE, тултип "Molds usable at second Layer of Walls") пробрасывает в контроллер
	//     (wall :700-706 гейт NO_CRUCIBLE), контроллер (:565-573 ориг./547-555... фактически :565/583(порт)
	//     fillMoldAtSide) находит расплавленный (T>=melt, mTargetSmelting.mMaterial==себе) компонент и зовёт
	//     aMold.fillMold(...) (Mold.java:246-264) — тот пишет mContent формы и её mTemperature=T контроллера.
	//     Слиток рождается в Mold.onServerTickPost (:178-204(порт)) когда mTemperature формы падает НИЖЕ mMeltingPoint
	//     (естественное остывание -5K/тик, :160(порт)) — tPrefix.mat(material, amount/tPrefix.mAmount) в slot(0);
	//     повторный клик пустой рукой по форме -> pickUpItem (:296-322(порт)) кладёт слиток в руку игрока — РЕАЛЬНЫЙ
	//     человеческий канал получения предмета, тот же метод, что вызывает физический клик игрока.
	// (6) Счётчики для судей: mTemperature/mEnergy контроллера (reflection), mContent (List<OreDictMaterialStack>,
	//     ищем запись материала Sn по .mMaterial==MT.Sn), mDisplayedFluid (короткий ID материала или -1).
	// Тир крышки: "Large Titanium Crucible" (id 17306, Loader_MultiTileEntities.java:1273 ориг./1277 порт,
	// NBT_DESIGN=18006 -> mWalls=18006 "Titanium Wall" :1149/1153) выбран НАРОЧНО вместо StainlessSteel/Steel —
	// Ti.mGramPerCubicCentimeter=4.54 (MT.java titanium():411) вдвое легче стали (~7.9), тигель прогревается
	// быстрее при равном притоке HU — конструктивный выбор тира, не изменение механики (крышка/тигель — тот же
	// класс MultiTileEntityCrucible для ЛЮБОГО зарегистрированного тира). Плавимый металл — MT.Sn (Tin, MT.java:126
	// tin(): mMeltingPoint=505K) — низкая точка плавления, задание явно предложило "олово/свинец"; Pb (лежит выше
	// по melt) не проверялся, Sn взят как более быстрый кейс. Горелка — Brick Burning Box id=1199 (тот же класс и
	// параметры, что верифицированы в ENERGYCHAINPROBE, mRate=16 HU/тик, mEfficiency=2500); ВОСЕМЬ штук ставятся под
	// ВСЕМИ 8 кольцевыми ячейками нижнего слоя (полное использование "Energy IN from Bottom Layer", не хак) ради
	// реалистичного времени прогрева (1 горелка нагревала бы тигель до 505K по формуле ~11-12 тыс. тиков — не хак,
	// а недооснащённая печь; 8 горелок = ~130 HU/тик суммарно, прогрев до 505K ожидается за ~800-1000 тиков).
	// Форма — Mold (Stone) id=1050; mShape пробой выставляется в bitmask формы OP.ingot (i=0 паттерн из статического
	// блока MultiTileEntityMold.java:687-695, скопирован дословно) — сетап-обход "форма уже прорезана долотом",
	// РЕАЛЬНЫЙ канал (клик по верхней грани) остаётся судимым. Снять при уборке фазы.
	private static final int CRP_CRUCIBLE_ID = 17306; // Large Titanium Crucible — :1273(ориг.)/1277(порт), NBT_DESIGN=18006
	private static final int CRP_WALL_ID     = 18006; // Titanium Wall — :1149(ориг.)/1153(порт)
	private static final int CRP_GEN_ID      = 1199;  // Brick Burning Box (Solid) — тот же генератор, что ENERGYCHAINPROBE
	private static final int CRP_MOLD_ID     = 1050;  // Mold (Stone) — :347(ориг.)/348(порт)
	private static final String CRP_M = "GT6-CRUCIBLEPROBE";
	// 8 кольцевых XZ-смещений 3x3 вокруг центра (1,1) — checkStructure2 i,j∈[-1,1] исключая (0,0), ориг./порт :112-131
	private static final int[] CRP_RING_DX = {0,1,2,0,2,0,1,2};
	private static final int[] CRP_RING_DZ = {0,0,0,1,1,2,2,2};
	// Для каждой горелки — направление НАРУЖУ от кольца 3x3 (в свободную, расчищенную зону footprint), чтобы
	// front-face гейт (mFacing, MultiTileEntityGeneratorSolid.java:111-114) НЕ упирался в соседнюю горелку/центр.
	private static final byte[] CRP_RING_OUTWARD = {SIDE_WEST, SIDE_NORTH, SIDE_EAST, SIDE_WEST, SIDE_EAST, SIDE_WEST, SIDE_SOUTH, SIDE_EAST};
	private static final long CRP_SEED_SN = 2 * U; // 2 единицы Sn — больше, чем требует форма слитка (U), остаток проверяет CONSERVE
	private static final int CRP_INGOT_SHAPE =
		  (1<<0)|(1<<1)|(1<<2)
		| (1<<5)|(1<<6)|(1<<7)
		| (1<<10)|(1<<11)|(1<<12)
		| (1<<15)|(1<<16)|(1<<17)
		| (1<<20)|(1<<21)|(1<<22); // паттерн i=0 из MultiTileEntityMold.java:687-695 (OP.ingot), скопирован дословно

	private static int sCRPProbeTick = -1;
	private static ServerPlayer sCRPPlayer;
	private static gregapi.probe.GT6ProbeStand.Seq sCRPSeq;

	private static gregtech.tileentity.multiblocks.MultiTileEntityCrucible sCRPHotCrucible, sCRPColdCrucible;
	private static gregtech.tileentity.tools.MultiTileEntityMold sCRPHotMold;
	private static final gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[] sCRPHotGens = new gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[CRP_RING_DX.length];
	private static BlockPos sCRPHotBase, sCRPColdBase;
	private static long sCRPHotTemp0, sCRPHotEnergy0, sCRPColdTemp0;
	private static long sCRPSnBeforePour, sCRPSnAfterPour;

	/** Расчищает объём постройки в AIR (не судимый канал, гигиена как solidPad в других стендах). */
	private static void gt6CrucibleProbeClearFootprint(ServerLevel aLevel, BlockPos aBase) {
		for (int x = -1; x <= 4; x++) for (int y = -3; y <= 3; y++) for (int z = -1; z <= 3; z++)
			aLevel.setBlock(aBase.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
	}

	/** Сумма количества материала aMat в mContent контроллера (reflection, читает публичные поля OreDictMaterialStack). */
	@SuppressWarnings("unchecked")
	private static long gt6CrucibleProbeContentAmount(Object aCrucible, Object aMat) {
		Object tRaw = gregapi.probe.GT6ProbeStand.fld(aCrucible, "mContent");
		if (!(tRaw instanceof List)) return 0;
		long rSum = 0;
		for (OreDictMaterialStack tStack : (List<OreDictMaterialStack>) tRaw) if (tStack != null && tStack.mMaterial == aMat) rSum += tStack.mAmount;
		return rSum;
	}

	/** Строит один тигель-риг: 3x3x3 стен + контроллер в центре нижнего слоя ("hollow of walls with opening on
	 *  top", MultiTileEntityCrucible.java:112-131 checkStructure2, 1:1 ориг./порт). aBase — угол (min X,min Z) на
	 *  высоте контроллера (pattern-y=0). aPlaceGens — поставить горелки под ВСЕМИ 8 кольцевыми ячейками нижнего
	 *  слоя (Energy IN); aPlaceMold — поставить форму у восточной кольцевой ячейки СРЕДНЕГО слоя (Y+1, "Molds
	 *  usable at second Layer of Walls"). */
	private static Object[] gt6CrucibleProbeBuildRig(ServerLevel aLevel, BlockPos aBase, String aLabel, boolean aPlaceGens, boolean aPlaceMold) {
		gt6CrucibleProbeClearFootprint(aLevel, aBase);
		Map<Character, Object> tLegend = new HashMap<>();
		tLegend.put('W', CRP_WALL_ID);
		tLegend.put('C', CRP_CRUCIBLE_ID);
		String[] tLayers = {"WWW\nWCW\nWWW", "WWW\nW.W\nWWW", "WWW\nW.W\nWWW"};
		Map<Character, List<BlockEntity>> tBuilt = gregapi.probe.GT6ProbeStand.pattern(aLevel, sCRPPlayer, aBase, tLayers, tLegend, CRP_M);
		List<BlockEntity> tControllers = tBuilt.get('C');
		if (tControllers == null || tControllers.isEmpty() || !(tControllers.get(0) instanceof gregtech.tileentity.multiblocks.MultiTileEntityCrucible)) throw new RuntimeException(aLabel + ": контроллер тигля не встал");
		gregtech.tileentity.multiblocks.MultiTileEntityCrucible tController = (gregtech.tileentity.multiblocks.MultiTileEntityCrucible) tControllers.get(0);

		gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[] tGens = new gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[CRP_RING_DX.length];
		if (aPlaceGens) for (int i = 0; i < CRP_RING_DX.length; i++) {
			BlockPos tGenAnchor = aBase.offset(CRP_RING_DX[i], -2, CRP_RING_DZ[i]);
			gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick tGen = gregapi.probe.GT6ProbeStand.place(aLevel, sCRPPlayer, tGenAnchor, net.minecraft.core.Direction.UP,
				gregapi.probe.GT6ProbeStand.mteStack(CRP_GEN_ID), gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick.class, CRP_M, aLabel + "-горелка[" + i + "]");
			if (tGen == null) throw new RuntimeException(aLabel + ": горелка[" + i + "] не встала");
			// Front-face гейт горелки (MultiTileEntityGeneratorSolid.java:111-114 — refuel-цикл требует !hasCollide&&oxygen
			// ПЕРЕД mFacing, тот же приём, что ECP gt6EnergyChainProbeBuildGenBoiler): реальным API setPrimaryFacing
			// (TileEntityBase09FacingSingle.java:90, тот же метод, что дёргает гайковёрт) разворачиваем НАРУЖУ от кольца
			// (не в соседнюю горелку/анкер центра), затем расчищаем эту клетку в AIR — иначе горелки в плотной 8-упаковке
			// гаснут после первого заряда mEnergy (сосед/анкер блокирует refuel).
			tGen.setPrimaryFacing(CRP_RING_OUTWARD[i]);
			net.minecraft.core.BlockPos tGenPos = tGenAnchor.above();
			net.minecraft.core.Direction tGenFront = FORGE_DIR[tGen.mFacing];
			aLevel.setBlock(tGenPos.relative(tGenFront), Blocks.AIR.defaultBlockState(), 3);
			tGens[i] = tGen;
		}

		gregtech.tileentity.tools.MultiTileEntityMold tMold = null;
		if (aPlaceMold) {
			BlockPos tMoldAnchor = aBase.offset(3, 0, 1); // восток от кольцевой стены (2,1,1) среднего слоя
			tMold = gregapi.probe.GT6ProbeStand.place(aLevel, sCRPPlayer, tMoldAnchor, net.minecraft.core.Direction.UP,
				gregapi.probe.GT6ProbeStand.mteStack(CRP_MOLD_ID), gregtech.tileentity.tools.MultiTileEntityMold.class, CRP_M, aLabel + "-форма");
			if (tMold == null) throw new RuntimeException(aLabel + ": форма не встала");
		}
		return new Object[]{tController, tGens, tMold};
	}

	/** Тик 200: построить HOT (с горелками+формой) и COLD (голые стены, без горелок — "тигель без горелки"). */
	private static void gt6CrucibleProbeBuild() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [" + CRP_M + "] Связка №5 — тигельный цикл (Ф3.1, на каркасе GT6ProbeStand) ==========");
		ServerLevel tLevel = sCRPPlayer.level();
		sCRPHotBase  = sCRPPlayer.blockPosition().offset(4, 0, 4);
		sCRPColdBase = sCRPPlayer.blockPosition().offset(4, 0, 20);

		Object[] tHot = gt6CrucibleProbeBuildRig(tLevel, sCRPHotBase, "HOT", T, T);
		sCRPHotCrucible = (gregtech.tileentity.multiblocks.MultiTileEntityCrucible) tHot[0];
		gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[] tHotGens = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[]) tHot[1];
		System.arraycopy(tHotGens, 0, sCRPHotGens, 0, tHotGens.length);
		sCRPHotMold = (gregtech.tileentity.tools.MultiTileEntityMold) tHot[2];

		Object[] tCold = gt6CrucibleProbeBuildRig(tLevel, sCRPColdBase, "COLD", F, F);
		sCRPColdCrucible = (gregtech.tileentity.multiblocks.MultiTileEntityCrucible) tCold[0];

		O.println("[" + CRP_M + "] построено: HOT контроллер=" + sCRPHotCrucible.getClass().getSimpleName() + "@" + sCRPHotBase.offset(1,0,1) + " (8 горелок + форма) ; COLD контроллер=" + sCRPColdCrucible.getClass().getSimpleName() + "@" + sCRPColdBase.offset(1,0,1) + " (без горелок)");
		O.println("[" + CRP_M + "] живые параметры (из BE): горелка.mRate=" + gregapi.probe.GT6ProbeStand.fldLong(sCRPHotGens[0], "mRate") + " горелка.mEfficiency=" + gregapi.probe.GT6ProbeStand.fldLong(sCRPHotGens[0], "mEfficiency") + "; тигель.getEnergySizeInputMin(HU)=" + sCRPHotCrucible.getEnergySizeInputMin(TD.Energy.HU, SIDE_ANY) + " тигель.getTemperatureMax=" + sCRPHotCrucible.getTemperatureMax(SIDE_ANY) + "K (безопасно выше Sn.melt=" + MT.Sn.mMeltingPoint + "K)");
	}

	/** Тик 210: разжечь ВСЕ 8 горелок HOT (топливо), засеять HOT тигель оловом (сетап-обход "units напрямую",
	 *  разрешён заданием — судимый канал остаётся реальный тик onServerTickPost), выставить форме bitmask слитка
	 *  (сетап-обход "долото уже применено"). COLD НЕ трогается вовсе ("тигель без горелки"). */
	private static void gt6CrucibleProbeLoad() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		for (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick tGen : sCRPHotGens) {
			gregapi.probe.GT6ProbeStand.fldSet(tGen, "mBurning", T);
			gregapi.probe.GT6ProbeStand.slotSet(tGen, 0, ST.make(Items.COAL, 32, 0));
		}
		long tEnvTemp = WD.envTemp(sCRPPlayer.level(), sCRPHotBase.getX()+1, sCRPHotBase.getY(), sCRPHotBase.getZ()+1);
		boolean tAdded = sCRPHotCrucible.addMaterialStacks(Arrays.asList(OM.stack(MT.Sn, CRP_SEED_SN)), tEnvTemp);
		gregapi.probe.GT6ProbeStand.fldSet(sCRPHotMold, "mShape", CRP_INGOT_SHAPE);
		sCRPHotTemp0 = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mTemperature");
		sCRPHotEnergy0 = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mEnergy");
		O.println("[" + CRP_M + "] тик 210 загрузка: 8 горелок HOT разожжены (32 угля каждая), Sn засеяно=" + tAdded + " (" + CRP_SEED_SN + " единиц, envTemp=" + tEnvTemp + "K), форма.mShape=форма-слитка; HOT temp0=" + sCRPHotTemp0 + "K energy0=" + sCRPHotEnergy0);
	}

	/** Тик 220: снимок ДО нагрева (HOT) и базовая точка COLD. */
	private static void gt6CrucibleProbeSampleEarly() {
		sCRPHotTemp0 = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mTemperature");
		sCRPHotEnergy0 = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mEnergy");
		sCRPColdTemp0 = gregapi.probe.GT6ProbeStand.fldLong(sCRPColdCrucible, "mTemperature");
		gregapi.data.CS.OUT.println("[" + CRP_M + "] тик 220 ранний снимок: HOT temp0=" + sCRPHotTemp0 + "K energy0=" + sCRPHotEnergy0 + "; COLD temp0=" + sCRPColdTemp0 + "K");
	}

	/** Тик 900: ФАЗА HEAT — тигель греется, скорость сверена с формулой tRequiredEnergy=1+weight/100 (:353 ориг./371
	 *  порт) и теоретическим потолком притока HU (8 горелок × mRate за окно), теоретический потолок — тот же приём,
	 *  что в ENERGYCHAINPROBE 4b (не точное предсказание тика-в-тик, а верхняя граница "не из ничего"). */
	@SuppressWarnings("unchecked")
	private static void gt6CrucibleProbeJudgeHeat() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		long tTemp1 = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mTemperature");
		long tEnergy1 = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mEnergy");
		long tDeltaT = tTemp1 - sCRPHotTemp0;
		Object tMat = gregapi.probe.GT6ProbeStand.fld(sCRPHotCrucible, "mMaterial");
		double tWeight = ((gregapi.oredict.OreDictMaterial) tMat).getWeight(U * 100) + OM.weight((List<OreDictMaterialStack>) gregapi.probe.GT6ProbeStand.fld(sCRPHotCrucible, "mContent"));
		long tRequiredEnergy = 1 + (long) (tWeight / gregtech.tileentity.multiblocks.MultiTileEntityCrucible.KG_PER_ENERGY);
		long tRate = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotGens[0], "mRate");
		int tTicksElapsed = 900 - 220;
		long tMaxHU = tRate * sCRPHotGens.length * tTicksElapsed;
		long tCeilConversions = tMaxHU / tRequiredEnergy;
		O.println("[" + CRP_M + "] ===== ФАЗА HEAT (тик 900, окно 220..900) =====");
		O.println("[" + CRP_M + "] HEAT: temp0=" + sCRPHotTemp0 + "K temp1=" + tTemp1 + "K (дельта=" + tDeltaT + "K); energy0=" + sCRPHotEnergy0 + " energy1=" + tEnergy1 + "; weight(живой)=" + tWeight + "кг tRequiredEnergy(живой)=" + tRequiredEnergy + " HU/K; потолок притока=" + tRate + "×" + sCRPHotGens.length + "×" + tTicksElapsed + "=" + tMaxHU + " HU => потолок конверсий=" + tCeilConversions + "K");
		sCRPSeq.judge("HEAT температура тигля растёт (реальный нагрев от горелок)", tDeltaT > 0, ">0", tDeltaT);
		sCRPSeq.judge("HEAT скорость нагрева в пределах теоретического потолка притока HU (формула :353/371, не из ничего)", tDeltaT <= tCeilConversions, "<=" + tCeilConversions, tDeltaT);
	}

	/** Тик 3200 (большой запас после ФИКСА front-face горелок — см. CRP_RING_OUTWARD): ФАЗА MELT — при
	 *  T>=Sn.mMeltingPoint контроллер показывает расплав (mDisplayedFluid=Sn.mID, :350/368). */
	private static void gt6CrucibleProbeJudgeMelt() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		long tTemp = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mTemperature");
		int tDisplayedFluid = gregapi.probe.GT6ProbeStand.fldInt(sCRPHotCrucible, "mDisplayedFluid");
		sCRPSnBeforePour = gt6CrucibleProbeContentAmount(sCRPHotCrucible, MT.Sn);
		O.println("[" + CRP_M + "] ===== ФАЗА MELT (тик 3200) =====");
		O.println("[" + CRP_M + "] MELT: тигель.mTemperature=" + tTemp + "K (порог Sn.mMeltingPoint=" + MT.Sn.mMeltingPoint + "K); mDisplayedFluid=" + tDisplayedFluid + " (ожидание Sn.mID=" + MT.Sn.mID + "); Sn в содержимом=" + sCRPSnBeforePour + " (засеяно=" + CRP_SEED_SN + ")");
		sCRPSeq.judge("MELT температура достигла точки плавления Sn", tTemp >= MT.Sn.mMeltingPoint, ">=" + MT.Sn.mMeltingPoint, tTemp);
		sCRPSeq.judge("MELT тигель показывает расплав нужного материала (mDisplayedFluid==Sn.mID)", tDisplayedFluid == MT.Sn.mID, MT.Sn.mID, tDisplayedFluid);
		sCRPSeq.judge("MELT единицы Sn сохранены в содержимом (units, не потеряны при конверсии self->self)", sCRPSnBeforePour == CRP_SEED_SN, CRP_SEED_SN, sCRPSnBeforePour);
	}

	/** Тик 1350: ПОКМ пустой рукой по верхней грани формы (реальный канал Mold.onBlockActivated3, клик у центра
	 *  грани -> цикл по горизонтальным соседям -> находит стену среднего слоя -> fillMoldAtSide). */
	private static void gt6CrucibleProbePour() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		sCRPPlayer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		sCRPPlayer.getInventory().setItem(0, ItemStack.EMPTY);
		sCRPPlayer.getInventory().setSelectedSlot(0);
		BlockPos tMoldPos = sCRPHotBase.offset(3, 1, 1);
		gregapi.probe.GT6ProbeStand.teleportLook(sCRPPlayer, tMoldPos.getX() + 0.5, tMoldPos.getY() + 1.0, tMoldPos.getZ() + 0.5, 0F, 90F);
		net.minecraft.world.InteractionResult tResult = gregapi.probe.GT6ProbeStand.clickBlock(sCRPPlayer, tMoldPos, net.minecraft.core.Direction.UP);
		Object tMoldContent = gregapi.probe.GT6ProbeStand.fld(sCRPHotMold, "mContent");
		O.println("[" + CRP_M + "] ===== ФАЗА POUR (тик 3250, клик пустой рукой по верху формы @" + tMoldPos + ") =====");
		O.println("[" + CRP_M + "] POUR: клик вернул=" + tResult + "; форма.mContent=" + (tMoldContent == null ? "null (заливка не удалась)" : tMoldContent) + " форма.mTemperature=" + gregapi.probe.GT6ProbeStand.fldLong(sCRPHotMold, "mTemperature") + "K");
		sCRPSeq.judge("POUR форма приняла расплав (mContent != null сразу после клика)", tMoldContent != null, "!= null", tMoldContent);
	}

	/** Тик 3400 (150 тиков после POUR — запас над расчётным временем остывания формы ниже Sn.mMeltingPoint,
	 *  естественное охлаждение -5K/тик, Mold.java:160 порт; форма после заливки ~1000K, нужно ~100 тиков): второй
	 *  клик пустой рукой по форме — форма уже остыла, в slot(0) уже слиток -> pickUpItem кладёт его в руку игрока
	 *  (реальный канал получения предмета). */
	private static void gt6CrucibleProbePickup() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		sCRPSnAfterPour = gt6CrucibleProbeContentAmount(sCRPHotCrucible, MT.Sn);
		BlockPos tMoldPos = sCRPHotBase.offset(3, 1, 1);
		net.minecraft.world.InteractionResult tResult = gregapi.probe.GT6ProbeStand.clickBlock(sCRPPlayer, tMoldPos, net.minecraft.core.Direction.UP);
		gregapi.data.CS.OUT.println("[" + CRP_M + "] тик 3400 PICKUP: форма.mTemperature=" + gregapi.probe.GT6ProbeStand.fldLong(sCRPHotMold, "mTemperature") + "K клик вернул=" + tResult + "; Sn в тигле после заливки=" + sCRPSnAfterPour + " (расход=" + (sCRPSnBeforePour - sCRPSnAfterPour) + ", ожидание=" + gregtech.tileentity.multiblocks.MultiTileEntityCrucible.KG_PER_ENERGY + "..U)");
	}

	/** Тик 3450: финальные судьи — POUR (слиток в руке игрока), CONSERVE (единицы по цепи закладка->слиток,
	 *  включая промежуточное состояние формы — расплав ЕЩЁ в форме, если PICKUP почему-то опередил остывание),
	 *  COLD (без горелки — не греется/не плавит), CONTROL-NEG (COLD не задет HOT-нагревом) + DONE. */
	@SuppressWarnings("unchecked")
	private static void gt6CrucibleProbeJudgeFinal() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + CRP_M + "] ===== ФИНАЛ (тик 3450) =====");

		ItemStack tHandStack = sCRPPlayer.getInventory().getItem(0);
		OreDictItemData tHandData = OM.anydata_(tHandStack);
		boolean tGotIngot = tHandData != null && tHandData.mPrefix == OP.ingot && tHandData.mMaterial != null && tHandData.mMaterial.mMaterial == MT.Sn;
		O.println("[" + CRP_M + "] POUR-финал: рука игрока=" + tHandStack + " (ожидание: 1x Sn Ingot)");
		sCRPSeq.judge("POUR слиток Sn получен игроком реальным каналом (клик->pickUpItem)", tGotIngot, "Sn Ingot x>=1", tHandStack);

		// CONSERVE считает ВЕСЬ путь закладка->расплав->слиток: остаток в тигле + слиток в руке игрока + то, что
		// ЕЩЁ сидит в форме (расплав mContent ИЛИ уже остывший слиток в slot(0), не подобранный) — ничего не должно
		// потеряться на любом промежуточном шаге цепи, независимо от точного момента PICKUP.
		long tSnRemaining = gt6CrucibleProbeContentAmount(sCRPHotCrucible, MT.Sn);
		long tIngotUnits = tGotIngot ? tHandStack.getCount() * OP.ingot.mAmount : 0;
		long tMoldUnits = 0;
		Object tMoldContent = gregapi.probe.GT6ProbeStand.fld(sCRPHotMold, "mContent");
		if (tMoldContent instanceof OreDictMaterialStack tMoldStack && tMoldStack.mMaterial == MT.Sn) tMoldUnits += tMoldStack.mAmount;
		ItemStack tMoldSlotStack = sCRPHotMold.slot(0);
		// F15-гейт судьи: slot(0) отдаёт null-able 1:1-инвентарь — OM.anydata_ на null падает NPE (был EXC прогона 3)
		OreDictItemData tMoldSlotData = ST.valid(tMoldSlotStack) ? OM.anydata_(tMoldSlotStack) : null;
		if (tMoldSlotData != null && tMoldSlotData.mPrefix == OP.ingot && tMoldSlotData.mMaterial != null && tMoldSlotData.mMaterial.mMaterial == MT.Sn) tMoldUnits += tMoldSlotStack.getCount() * OP.ingot.mAmount;
		long tConserveTotal = tSnRemaining + tIngotUnits + tMoldUnits;
		O.println("[" + CRP_M + "] CONSERVE: засеяно=" + CRP_SEED_SN + " units; осталось в тигле=" + tSnRemaining + " + слиток в руке(" + (tGotIngot ? tHandStack.getCount() : 0) + "×" + OP.ingot.mAmount + ")=" + tIngotUnits + " + ещё в форме=" + tMoldUnits + " (mContent=" + tMoldContent + " slot0=" + tMoldSlotStack + ") => сумма=" + tConserveTotal);
		sCRPSeq.judge("CONSERVE единицы материала сохранены по всей цепи закладка->расплав->слиток", tConserveTotal == CRP_SEED_SN, CRP_SEED_SN, tConserveTotal);

		sCRPPlayer.setGameMode(net.minecraft.world.level.GameType.CREATIVE); // вернуть режим (§9 гигиена, как в др. пробах)

		long tColdTempFinal = gregapi.probe.GT6ProbeStand.fldLong(sCRPColdCrucible, "mTemperature");
		int tColdDisplayedFluid = gregapi.probe.GT6ProbeStand.fldInt(sCRPColdCrucible, "mDisplayedFluid");
		long tColdSn = gt6CrucibleProbeContentAmount(sCRPColdCrucible, MT.Sn);
		long tColdDeltaT = tColdTempFinal - sCRPColdTemp0;
		O.println("[" + CRP_M + "] COLD: temp0=" + sCRPColdTemp0 + "K tempFinal=" + tColdTempFinal + "K (дельта=" + tColdDeltaT + "K, HOT дельта была намного больше); mDisplayedFluid=" + tColdDisplayedFluid + " Sn-содержимое=" + tColdSn);
		sCRPSeq.judge("COLD тигель без горелки НЕ нагрелся (дельта в узкой полосе, в отличие от HOT)", Math.abs(tColdDeltaT) <= 5, "<=5", tColdDeltaT);
		sCRPSeq.judge("COLD тигель без горелки НЕ расплавил металл (mDisplayedFluid=-1, металла не было)", tColdDisplayedFluid == -1 && tColdSn == 0, "-1 и 0", tColdDisplayedFluid + "/" + tColdSn);

		long tHotTempFinal = gregapi.probe.GT6ProbeStand.fldLong(sCRPHotCrucible, "mTemperature");
		O.println("[" + CRP_M + "] CONTROL-NEG: HOT (реальный приток HU) tempFinal=" + tHotTempFinal + "K против COLD (без притока) tempFinal=" + tColdTempFinal + "K — разница подтверждает, что нагрев причинно связан с горелками, а не с общим тиком/окружением");
		sCRPSeq.judge("CONTROL-NEG HOT нагрелся значительно сильнее COLD (соседние структуры не задеты общим эффектом)", tHotTempFinal - tColdTempFinal > 50, ">50", tHotTempFinal - tColdTempFinal);

		sCRPSeq.done();
	}

	public static void gt6CrucibleProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sCRPProbeTick++;
		if (aServer.getPlayerList().getPlayers().isEmpty()) return;
		sCRPPlayer = aServer.getPlayerList().getPlayers().get(0);
		if (sCRPSeq == null) {
			sCRPSeq = new gregapi.probe.GT6ProbeStand.Seq(CRP_M)
				.at(200, GT_API_Proxy::gt6CrucibleProbeBuild)
				.at(210, GT_API_Proxy::gt6CrucibleProbeLoad)
				.at(220, GT_API_Proxy::gt6CrucibleProbeSampleEarly)
				.at(900, GT_API_Proxy::gt6CrucibleProbeJudgeHeat)
				.at(3200, GT_API_Proxy::gt6CrucibleProbeJudgeMelt)
				.at(3250, GT_API_Proxy::gt6CrucibleProbePour)
				.at(3400, GT_API_Proxy::gt6CrucibleProbePickup)
				.at(3450, GT_API_Proxy::gt6CrucibleProbeJudgeFinal);
		}
		sCRPSeq.tick(sCRPProbeTick);
	}

	// ========== [GT6-AUTOOUTPROBE] ВРЕМЕННАЯ проба «Связка №6 — авто-вывод машин + каверы в работе» (Ф3.1, гейт run/gt6autooutprobe.flag + -Pgt6probes, на каркасе GT6ProbeStand) ==========
	// ЭТАП А (разведка кода, ВЫЧИТАНА, не угадана — все сборки судятся РЕАЛЬНЫМ тиком, ни один судимый метод пробой
	// не вызывается напрямую):
	// (а) БОЧКА, бит mMode B[0] "auto-fill vertically adjacent Tanks depending on Gravity": onToolClick2 переключает
	//     это обезьяним ключом (TileEntityBase08Barrel.java:137-143); onTick2 эмиссия — внутри блока :162-224, сама
	//     ветка :214-219: не запечатанная бочка (mMode&B[1]==0) с !mMagicProof/!mAcidProof/!mPlasmaProof/!mGasProof/
	//     allowFluid-жидкостью и mMode&B[0]!=0 -> tSides = gas?ALL_SIDES_VERTICAL:lighter?ALL_SIDES_TOP:ALL_SIDES_BOTTOM
	//     (CS.java:722-724; вода не gas и не "lighter" -> BOTTOM={SIDE_DOWN=0}) -> FL.move(mTank,getAdjacentTank(tSide))
	//     БЕЗ явного лимита (2-арг. overload FL.move(IFluidTank,DelegatorTileEntity), FL.java:972,974 -> aMaxMoved=
	//     Long.MAX_VALUE) -> переносит ВЕСЬ доступный объём за ОДИН тик (ограничено только capacity приёмника) —
	//     инстант-заливка, НЕ метрируемый темп. Уже освоено стендом №2 (FLUIDPIPEPROBE NORM: `sFPSrcNorm.mTank.
	//     setFluid(...); sFPSrcNorm.mMode |= B[0];`, GT_API_Proxy.java:2470) — здесь короткий прямой регресс
	//     бочка-над-бочкой (без трубы-посредника, приёмник — соседняя бочка напрямую).
	// (б) БАЗОВАЯ ЭЛЕКТРО-МАШИНА, авто-вывод предметов: MultiTileEntityBasicMachine.java поля mItemAutoOutput/
	//     mItemOutputs/mDisabledItemOutput (публичные, :99); onTick2 (:456-473) вызывает doOutputFluids БЕЗУСЛОВНО
	//     каждый тик (:464), но НЕ doOutputItems напрямую — вывод предметов идёт через doWork(:785-798)->doActive
	//     (:800-892): блок авто-вывода предметов (:872-889) стоит ВНЕ секции "mMaxProgress>0" -> выполняется КАЖДЫЙ
	//     тик, пока энергии хватает (mEnergy>=mInputMin&&mEnergy>=mMinEnergy, :786), НЕЗАВИСИМО от того, обрабатывается
	//     ли рецепт. Триггер (:878): output-слоты НЕ пусты И (mIgnited>0||mInventoryChanged||!mRunning||mOutputBlocked
	//     ==1||aTimer%200==5); "!mRunning" читает СТАРОЕ (пред-тиковое) значение — doWork выставляет mRunning=T ПОСЛЕ
	//     возврата doActive (:787-788) -> на ПЕРВЫЙ тик с достаточной энергией триггер срабатывает гарантированно.
	//     doOutputItems (:993-997) -> ST.moveAll(delegator(tAutoOutput),getItemOutputTarget) с aMaxMove=64 (ST.java:
	//     661) -> ПЕРЕНОС ВСЕГО СТЕКА ЗА ОДИН ТИК (в отличие от кавера — не метрируемый темп). Обезьяний ключ
	//     переключает mDisabledItemOutput тем же публичным полем (:390-401 monkeywrench), здесь выставляется напрямую
	//     (топология, не судимый канал). Тир: "Electrolyzer" (ULV) id=20091 — MultiTileEntityBasicMachineElectric,
	//     Loader_MultiTileEntities.java:1340, NBT_ENERGY_ACCEPTED=EU, NBT_RECIPEMAP=RM.Electrolyzer (RM.java:123:
	//     input/output/min items=2/6/1), NBT_INV_SIDE_AUTO_OUT=SIDE_RIGHT, NBT_INV_SIDE_OUT=SBIT_R|SBIT_L — авто-
	//     вывод ВКЛЮЧЁН по умолчанию у реальной зарегистрированной машины (NBT_INV_DISABLED_OUT не задан -> false) —
	//     живая машина как она есть, не выдуманная конфигурация; мировая сторона вывода вычисляется ЖИВЫМ кодом
	//     (FACING_TO_SIDE[mFacing][mItemAutoOutput], :570/995), не предполагается.
	// (в) КАВЕР-НАСОС CoverPump (gregapi/cover/covers/CoverPump.java): onTickPre (:67-76) СЕРВЕР-СТОРОНА, раз в 20
	//     тиков (SERVER_TIME%20==5, "L/sec" из тултипа :81), tThroughput=mThroughput (:45,47-49, public final,
	//     задаётся конструктором предмета-кавера); mVisuals[aSide]==0 — умолчание для ТАНКА (не трубы: onCoverPlaced
	//     :52-55 ставит visual=1(IN) ТОЛЬКО для MultiTileEntityPipeFluid) -> режим OUT: FL.move(delegator(aSide)
	//     [ЭТОТ танк] -> getAdjacentTank(aSide) [сосед], tThroughput) — 3-арг. overload С явным лимитом (FL.java:967)
	//     -> МЕТРИРУЕМЫЙ перенос (в отличие от бочки-гравитации выше). Диспетчер кавер-тиков — TileEntityBase06Covers.
	//     java:202 (mCovers.tickPre внутри final onTick, тот же центр, что уже верифицирован CoverFilterItem в
	//     ITEMPIPEPROBE FILTER-кейсе). Предмет насоса: IL.PUMPS[0] "Compact Electric Pump (ULV)", id=12020,
	//     MultiItemTechnological.java:51, mThroughput=250<<(2*0)=250 (250mb за цикл/20 тиков). Установлен НА БОЧКЕ
	//     (не на трубе) реальным публичным API setCoverItem (ITileEntityCoverable.java:35, тот же метод, что
	//     ITEMPIPEPROBE FILTER-кейс).
	// (г) Судьи — каркас (slotCount/tankAmount/conserve): CONSERVE — сумма источник+приёмник неизменна на каждом
	//     замере; COLD-контроли — те же постройки БЕЗ включённого бита/кавера/авто-вывода, ничего не движется.
	// Дифф порт/оригинал задействованных методов (TileEntityBase08Barrel.onTick2, MultiTileEntityBasicMachine.
	// doActive/doOutputItems, CoverPump.onTickPre) — построчно 1:1 с оригиналом gregtech6/.../*.java (engine-swap
	// TileEntity->BlockEntity, IFluidHandler forge->neoforge, World->Level, NBTTagCompound->CompoundTag+getXOr;
	// расхождений в control-flow нет). Снять при уборке фазы.
	private static final int AOP_ELECTRO_ID = 20091; // Electrolyzer (ULV), MultiTileEntityBasicMachineElectric — Loader_MultiTileEntities.java:1340
	private static final int AOP_CHEST_ID   = 32745; // Mossy Stone Chest — тот же ID, что ITEMPIPEPROBE (Loader_MultiTileEntities.java:152)
	private static final int AOP_BARREL_ID  = 32102; // Bronze Drum — тот же ID, что FLUIDPIPEPROBE (capacity=64000, gasProof=T)
	private static final long AOP_WATER_TOP     = 20000; // засев верхней бочки BARREL-OUT (< capacity 64000)
	private static final long AOP_WATER_PUMPSRC = 20000; // засев бочки-источника COVER-PUMP
	private static final String AOP_M = "GT6-AUTOOUTPROBE";
	private static int sAOPProbeTick = -1;
	private static ServerPlayer sAOPPlayer;
	private static gregapi.probe.GT6ProbeStand.Seq sAOPSeq;

	// MACHINE-OUT
	private static gregapi.tileentity.machines.MultiTileEntityBasicMachineElectric sAOPMachineHot, sAOPMachineCold;
	private static gregapi.block.multitileentity.example.MultiTileEntityChest sAOPMachineHotChest, sAOPMachineColdChest;
	private static int sAOPMachineHotOutSlot, sAOPMachineColdOutSlot;
	private static long sAOPMachineHotSum0, sAOPMachineColdSum0;

	// COVER-PUMP
	private static gregapi.tileentity.tank.TileEntityBase08Barrel sAOPPumpSrcHot, sAOPPumpTgtHot, sAOPPumpSrcCold, sAOPPumpTgtCold;
	private static long sAOPPumpHotSum0, sAOPPumpColdSum0;
	private static int sAOPPumpThroughput;
	private static long sAOPPumpLoadServerTime;

	// BARREL-OUT
	private static gregapi.tileentity.tank.TileEntityBase08Barrel sAOPBarrelTopHot, sAOPBarrelBotHot, sAOPBarrelTopCold, sAOPBarrelBotCold;
	private static long sAOPBarrelHotSum0, sAOPBarrelColdSum0;

	/** Сумма предметов во всех слотах сундука (консервация; каркас {@link gregapi.probe.GT6ProbeStand#slotCount}). */
	private static long gt6AutoOutProbeChestSum(gregapi.block.multitileentity.example.MultiTileEntityChest aChest) {
		if (aChest == null) return 0;
		long rSum = 0;
		for (int i = 0; i < aChest.invsize(); i++) rSum += gregapi.probe.GT6ProbeStand.slotCount(aChest, i);
		return rSum;
	}

	/** Один "машина+сундук": анкер -> машина (face UP) -> фикс. ориентация SOUTH (реальный API setPrimaryFacing, тот
	 *  же, что дёргает гайковёрт) -> живой расчёт мировой стороны авто-вывода (FACING_TO_SIDE[mFacing][mItemAutoOutput],
	 *  MultiTileEntityBasicMachine.java:570/995) -> сундук на этой стороне. aEnableOutput=F -> COLD-контроль
	 *  (mDisabledItemOutput=T явно, тот же публичный флаг, что переключает обезьяний ключ, :390-401). */
	private static Object[] gt6AutoOutProbeBuildMachine(ServerLevel aLevel, BlockPos aGround, boolean aEnableOutput, String aLabel) {
		gregapi.tileentity.machines.MultiTileEntityBasicMachineElectric tMachine = gregapi.probe.GT6ProbeStand.place(
			aLevel, sAOPPlayer, aGround, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(AOP_ELECTRO_ID),
			gregapi.tileentity.machines.MultiTileEntityBasicMachineElectric.class, AOP_M, aLabel + "-машина");
		if (tMachine == null) throw new RuntimeException(aLabel + ": машина не встала");
		tMachine.setPrimaryFacing(SIDE_SOUTH);
		tMachine.mDisabledItemOutput = !aEnableOutput;
		byte tOutSide = FACING_TO_SIDE[tMachine.mFacing][tMachine.mItemAutoOutput];
		BlockPos tMachinePos = aGround.above();
		gregapi.block.multitileentity.example.MultiTileEntityChest tChest = gregapi.probe.GT6ProbeStand.place(
			aLevel, sAOPPlayer, tMachinePos, FORGE_DIR[tOutSide], gregapi.probe.GT6ProbeStand.mteStack(AOP_CHEST_ID),
			gregapi.block.multitileentity.example.MultiTileEntityChest.class, AOP_M, aLabel + "-сундук");
		if (tChest == null) throw new RuntimeException(aLabel + ": сундук не встал (сторона авто-вывода=" + tOutSide + ")");
		return new Object[]{tMachine, tChest, tOutSide};
	}

	/** Пара бочек для COVER-PUMP: источник(anchor) -> приёмник(East от источника). aInstallCover=T -> реальный
	 *  публичный setCoverItem(EAST, IL.PUMPS[0]="Compact Electric Pump (ULV)", mThroughput=250) на источнике. */
	private static Object[] gt6AutoOutProbeBuildPumpPair(ServerLevel aLevel, BlockPos aGround, boolean aInstallCover, String aLabel) {
		gregapi.tileentity.tank.TileEntityBase08Barrel tSrc = gregapi.probe.GT6ProbeStand.place(
			aLevel, sAOPPlayer, aGround, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(AOP_BARREL_ID),
			gregapi.tileentity.tank.TileEntityBase08Barrel.class, AOP_M, aLabel + "-источник");
		if (tSrc == null) throw new RuntimeException(aLabel + ": источник-бочка не встала");
		BlockPos tSrcPos = aGround.above();
		gregapi.tileentity.tank.TileEntityBase08Barrel tTgt = gregapi.probe.GT6ProbeStand.place(
			aLevel, sAOPPlayer, tSrcPos, net.minecraft.core.Direction.EAST, gregapi.probe.GT6ProbeStand.mteStack(AOP_BARREL_ID),
			gregapi.tileentity.tank.TileEntityBase08Barrel.class, AOP_M, aLabel + "-приёмник");
		if (tTgt == null) throw new RuntimeException(aLabel + ": приёмник-бочка не встала");
		if (aInstallCover) tSrc.setCoverItem(SIDE_EAST, IL.PUMPS[0].get(1), null, T, T);
		return new Object[]{tSrc, tTgt};
	}

	/** Столб бочек для BARREL-OUT: нижняя(приёмник, на анкере) -> верхняя(источник, над нижней). aEnableBit=T ->
	 *  верхняя.mMode|=B[0] (тот же бит, что верифицирован FLUIDPIPEPROBE NORM, GT_API_Proxy.java:2470). */
	private static Object[] gt6AutoOutProbeBuildBarrelColumn(ServerLevel aLevel, BlockPos aGround, boolean aEnableBit, String aLabel) {
		gregapi.tileentity.tank.TileEntityBase08Barrel tBottom = gregapi.probe.GT6ProbeStand.place(
			aLevel, sAOPPlayer, aGround, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(AOP_BARREL_ID),
			gregapi.tileentity.tank.TileEntityBase08Barrel.class, AOP_M, aLabel + "-нижняя(приёмник)");
		if (tBottom == null) throw new RuntimeException(aLabel + ": нижняя бочка не встала");
		BlockPos tBottomPos = aGround.above();
		gregapi.tileentity.tank.TileEntityBase08Barrel tTop = gregapi.probe.GT6ProbeStand.place(
			aLevel, sAOPPlayer, tBottomPos, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(AOP_BARREL_ID),
			gregapi.tileentity.tank.TileEntityBase08Barrel.class, AOP_M, aLabel + "-верхняя(источник)");
		if (tTop == null) throw new RuntimeException(aLabel + ": верхняя бочка не встала");
		if (aEnableBit) tTop.mMode |= B[0];
		return new Object[]{tTop, tBottom};
	}

	/** Тик 200: постройка HOT+COLD троек (MACHINE-OUT, COVER-PUMP, BARREL-OUT) + чтение живых параметров.
	 *  Любой обрыв -> RuntimeException -> Seq печатает EXC. */
	private static void gt6AutoOutProbeBuild() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		ServerLevel tLevel = sAOPPlayer.level();
		O.println("========== [" + AOP_M + "] Связка №6 — авто-вывод машин + каверы в работе (Ф3.1, на каркасе GT6ProbeStand) ==========");
		MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		int[] tIds = {AOP_ELECTRO_ID, AOP_CHEST_ID, AOP_BARREL_ID};
		for (int tId : tIds) if (tReg == null || tReg.getClassContainer(tId) == null) throw new RuntimeException("реестр/ID не найден: " + tId);
		O.println("[" + AOP_M + "] ID подтверждены: машина=" + tReg.getClassContainer(AOP_ELECTRO_ID).mClass.getSimpleName() + "(" + AOP_ELECTRO_ID + ") сундук=" + tReg.getClassContainer(AOP_CHEST_ID).mClass.getSimpleName() + "(" + AOP_CHEST_ID + ") бочка=" + tReg.getClassContainer(AOP_BARREL_ID).mClass.getSimpleName() + "(" + AOP_BARREL_ID + ")");

		BlockPos tBaseMachineHot  = sAOPPlayer.blockPosition().offset(4, 0, 4);
		BlockPos tBaseMachineCold = sAOPPlayer.blockPosition().offset(4, 0, 10);
		BlockPos tBasePumpHot     = sAOPPlayer.blockPosition().offset(4, 0, 16);
		BlockPos tBasePumpCold    = sAOPPlayer.blockPosition().offset(4, 0, 22);
		BlockPos tBaseBarrelHot   = sAOPPlayer.blockPosition().offset(4, 0, 28);
		BlockPos tBaseBarrelCold  = sAOPPlayer.blockPosition().offset(4, 0, 34);

		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBaseMachineHot,  4, 1); // пол — гигиена, не судимый канал
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBaseMachineCold, 4, 1);
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBasePumpHot,     4, 1);
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBasePumpCold,    4, 1);
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBaseBarrelHot,   2, 1);
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBaseBarrelCold,  2, 1);

		Object[] tMachineHot  = gt6AutoOutProbeBuildMachine(tLevel, tBaseMachineHot,  T, "MACHINE-OUT HOT");
		Object[] tMachineCold = gt6AutoOutProbeBuildMachine(tLevel, tBaseMachineCold, F, "MACHINE-OUT COLD");
		sAOPMachineHot  = (gregapi.tileentity.machines.MultiTileEntityBasicMachineElectric) tMachineHot[0];  sAOPMachineHotChest  = (gregapi.block.multitileentity.example.MultiTileEntityChest) tMachineHot[1];
		sAOPMachineCold = (gregapi.tileentity.machines.MultiTileEntityBasicMachineElectric) tMachineCold[0]; sAOPMachineColdChest = (gregapi.block.multitileentity.example.MultiTileEntityChest) tMachineCold[1];

		Object[] tPumpHot  = gt6AutoOutProbeBuildPumpPair(tLevel, tBasePumpHot,  T, "COVER-PUMP HOT");
		Object[] tPumpCold = gt6AutoOutProbeBuildPumpPair(tLevel, tBasePumpCold, F, "COVER-PUMP COLD");
		sAOPPumpSrcHot  = (gregapi.tileentity.tank.TileEntityBase08Barrel) tPumpHot[0];  sAOPPumpTgtHot  = (gregapi.tileentity.tank.TileEntityBase08Barrel) tPumpHot[1];
		sAOPPumpSrcCold = (gregapi.tileentity.tank.TileEntityBase08Barrel) tPumpCold[0]; sAOPPumpTgtCold = (gregapi.tileentity.tank.TileEntityBase08Barrel) tPumpCold[1];

		Object[] tBarrelHot  = gt6AutoOutProbeBuildBarrelColumn(tLevel, tBaseBarrelHot,  T, "BARREL-OUT HOT");
		Object[] tBarrelCold = gt6AutoOutProbeBuildBarrelColumn(tLevel, tBaseBarrelCold, F, "BARREL-OUT COLD");
		sAOPBarrelTopHot  = (gregapi.tileentity.tank.TileEntityBase08Barrel) tBarrelHot[0];  sAOPBarrelBotHot  = (gregapi.tileentity.tank.TileEntityBase08Barrel) tBarrelHot[1];
		sAOPBarrelTopCold = (gregapi.tileentity.tank.TileEntityBase08Barrel) tBarrelCold[0]; sAOPBarrelBotCold = (gregapi.tileentity.tank.TileEntityBase08Barrel) tBarrelCold[1];

		if (sAOPMachineHotChest == null || sAOPMachineColdChest == null) throw new RuntimeException("MACHINE-OUT: постройка не удалась (сундук null)");
		if (sAOPPumpTgtHot == null || sAOPPumpTgtCold == null) throw new RuntimeException("COVER-PUMP: постройка не удалась (бочка null)");
		if (sAOPBarrelBotHot == null || sAOPBarrelBotCold == null) throw new RuntimeException("BARREL-OUT: постройка не удалась (бочка null)");

		gregapi.cover.covers.CoverPump tPumpBehavior = (gregapi.cover.covers.CoverPump) sAOPPumpSrcHot.getCoverData().mBehaviours[SIDE_EAST];
		if (tPumpBehavior == null) throw new RuntimeException("COVER-PUMP HOT: кавер не встал на источнике (mBehaviours[EAST]==null)");
		sAOPPumpThroughput = tPumpBehavior.mThroughput;

		sAOPMachineHotOutSlot  = sAOPMachineHot.mRecipes.mInputItemsCount;
		sAOPMachineColdOutSlot = sAOPMachineCold.mRecipes.mInputItemsCount;
		O.println("[" + AOP_M + "] живые параметры (из BE, не предположены): машина.mFacing=" + sAOPMachineHot.mFacing + " машина.mItemAutoOutput=" + sAOPMachineHot.mItemAutoOutput
			+ " (мировая сторона=" + FACING_TO_SIDE[sAOPMachineHot.mFacing][sAOPMachineHot.mItemAutoOutput] + ") outSlot=" + sAOPMachineHotOutSlot + " mRecipes.mOutputItemsCount=" + sAOPMachineHot.mRecipes.mOutputItemsCount
			+ "; кавер-насос.mThroughput=" + sAOPPumpThroughput + "; бочка.mTank.capacity()=" + sAOPBarrelTopHot.mTank.capacity());
	}

	/** Тик 210: сетап-закладка (готовый результат в выходном слоте машины напрямую, жидкость в бочках-источниках) —
	 *  судимый канал остаётся реальный: доставка идёт только штатными тиками ниже по цепи. */
	private static void gt6AutoOutProbeLoad() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		gregapi.probe.GT6ProbeStand.slotSet(sAOPMachineHot,  sAOPMachineHotOutSlot,  ST.make(Blocks.COBBLESTONE, 16, 0));
		gregapi.probe.GT6ProbeStand.slotSet(sAOPMachineCold, sAOPMachineColdOutSlot, ST.make(Blocks.COBBLESTONE, 16, 0));
		sAOPMachineHot.mEnergy  = 1_000_000_000L; // сетап-обход бухгалтерии энергии (тот же приём, что ECP sECPMotorSrc.mEnergy) — судимый канал doActive()/doOutputItems() остаётся реальным
		sAOPMachineCold.mEnergy = 1_000_000_000L;
		sAOPMachineHotSum0  = gregapi.probe.GT6ProbeStand.slotCount(sAOPMachineHot,  sAOPMachineHotOutSlot)  + gt6AutoOutProbeChestSum(sAOPMachineHotChest);
		sAOPMachineColdSum0 = gregapi.probe.GT6ProbeStand.slotCount(sAOPMachineCold, sAOPMachineColdOutSlot) + gt6AutoOutProbeChestSum(sAOPMachineColdChest);

		gregapi.probe.GT6ProbeStand.fill(sAOPPumpSrcHot,  "water", AOP_WATER_PUMPSRC);
		gregapi.probe.GT6ProbeStand.fill(sAOPPumpSrcCold, "water", AOP_WATER_PUMPSRC);
		sAOPPumpHotSum0  = sAOPPumpSrcHot.mTank.amount()  + sAOPPumpTgtHot.mTank.amount();
		sAOPPumpColdSum0 = sAOPPumpSrcCold.mTank.amount() + sAOPPumpTgtCold.mTank.amount();
		sAOPPumpLoadServerTime = SERVER_TIME;

		gregapi.probe.GT6ProbeStand.fill(sAOPBarrelTopHot,  "water", AOP_WATER_TOP);
		gregapi.probe.GT6ProbeStand.fill(sAOPBarrelTopCold, "water", AOP_WATER_TOP);
		sAOPBarrelHotSum0  = sAOPBarrelTopHot.mTank.amount()  + sAOPBarrelBotHot.mTank.amount();
		sAOPBarrelColdSum0 = sAOPBarrelTopCold.mTank.amount() + sAOPBarrelBotCold.mTank.amount();

		O.println("[" + AOP_M + "] тик 210 загрузка: MACHINE-OUT HOT/COLD слот=" + sAOPMachineHotOutSlot + "/" + sAOPMachineColdOutSlot + " (по 16×Cobblestone); "
			+ "COVER-PUMP HOT/COLD source=" + AOP_WATER_PUMPSRC + "mb (sum0=" + sAOPPumpHotSum0 + "/" + sAOPPumpColdSum0 + "), SERVER_TIME=" + sAOPPumpLoadServerTime + "; "
			+ "BARREL-OUT HOT/COLD верхняя=" + AOP_WATER_TOP + "mb (sum0=" + sAOPBarrelHotSum0 + "/" + sAOPBarrelColdSum0 + ")");
	}

	/** Тик 260 (50 тиков после загрузки — с запасом на первый триггерящий тик doActive()): MACHINE-OUT + BARREL-OUT,
	 *  оба HOT+COLD (оба мгновенные one-shot переносы по коду, см. комментарий блока выше). */
	private static void gt6AutoOutProbeJudgeMachineAndBarrel() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + AOP_M + "] ===== MACHINE-OUT (тик 260) =====");
		long tHotSlot = gregapi.probe.GT6ProbeStand.slotCount(sAOPMachineHot, sAOPMachineHotOutSlot);
		long tHotChest = gt6AutoOutProbeChestSum(sAOPMachineHotChest);
		long tColdSlot = gregapi.probe.GT6ProbeStand.slotCount(sAOPMachineCold, sAOPMachineColdOutSlot);
		long tColdChest = gt6AutoOutProbeChestSum(sAOPMachineColdChest);
		O.println("[" + AOP_M + "] MACHINE-OUT HOT: слот=" + tHotSlot + " сундук=" + tHotChest + " (заложено=" + sAOPMachineHotSum0 + "); COLD: слот=" + tColdSlot + " сундук=" + tColdChest + " (заложено=" + sAOPMachineColdSum0 + ", авто-вывод выключен явно)");
		sAOPSeq.judge("MACHINE-OUT HOT: результат уехал из машины БЕЗ кликов (слот опустел)", tHotSlot == 0, 0, tHotSlot);
		sAOPSeq.judge("MACHINE-OUT HOT: результат пришёл в приёмник целиком", tHotChest == sAOPMachineHotSum0, sAOPMachineHotSum0, tHotChest);
		sAOPSeq.conserve("MACHINE-OUT HOT: консервация", sAOPMachineHotSum0, () -> tHotSlot + tHotChest);
		sAOPSeq.judge("MACHINE-OUT COLD: авто-вывод выключен -> слот НЕ опустел", tColdSlot == sAOPMachineColdSum0, sAOPMachineColdSum0, tColdSlot);
		sAOPSeq.judge("MACHINE-OUT COLD: приёмник пуст (ничего не двигалось)", tColdChest == 0, 0, tColdChest);

		O.println("[" + AOP_M + "] ===== BARREL-OUT (тик 260) =====");
		long tBarrelHotTop = sAOPBarrelTopHot.mTank.amount(), tBarrelHotBot = sAOPBarrelBotHot.mTank.amount();
		long tBarrelColdTop = sAOPBarrelTopCold.mTank.amount(), tBarrelColdBot = sAOPBarrelBotCold.mTank.amount();
		O.println("[" + AOP_M + "] BARREL-OUT HOT: верхняя=" + tBarrelHotTop + " нижняя=" + tBarrelHotBot + " (сумма0=" + sAOPBarrelHotSum0 + "); COLD: верхняя=" + tBarrelColdTop + " нижняя=" + tBarrelColdBot + " (сумма0=" + sAOPBarrelColdSum0 + ", бит НЕ выставлен)");
		sAOPSeq.judge("BARREL-OUT HOT: верхняя бочка стекла вниз (гравитация, TileEntityBase08Barrel.java:214-219)", tBarrelHotTop == 0 && tBarrelHotBot == AOP_WATER_TOP, "верх=0 низ=" + AOP_WATER_TOP, "верх=" + tBarrelHotTop + " низ=" + tBarrelHotBot);
		sAOPSeq.conserve("BARREL-OUT HOT: консервация mb", sAOPBarrelHotSum0, () -> tBarrelHotTop + tBarrelHotBot);
		sAOPSeq.judge("BARREL-OUT COLD: бит не выставлен -> ничего не стекло", tBarrelColdTop == AOP_WATER_TOP && tBarrelColdBot == 0, "верх=" + AOP_WATER_TOP + " низ=0", "верх=" + tBarrelColdTop + " низ=" + tBarrelColdBot);
	}

	/** Тик 400 (190 тиков после загрузки — до 9-10 циклов кавера каждые 20 тиков): COVER-PUMP HOT+COLD + финальный
	 *  CONSERVE + DONE. */
	private static void gt6AutoOutProbeJudgePumpAndFinal() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + AOP_M + "] ===== COVER-PUMP (тик 400) =====");
		long tElapsed = SERVER_TIME - sAOPPumpLoadServerTime;
		long tCeilEvents = tElapsed / 20 + 2; // запас против модуло-выравнивания SERVER_TIME%20==5 (CoverPump.java:68)
		long tCeiling = tCeilEvents * sAOPPumpThroughput;
		long tHotSrc = sAOPPumpSrcHot.mTank.amount(), tHotTgt = sAOPPumpTgtHot.mTank.amount();
		long tColdSrc = sAOPPumpSrcCold.mTank.amount(), tColdTgt = sAOPPumpTgtCold.mTank.amount();
		O.println("[" + AOP_M + "] COVER-PUMP HOT: источник=" + tHotSrc + " приёмник=" + tHotTgt + " (сумма0=" + sAOPPumpHotSum0 + "); прошло=" + tElapsed + " тиков => потолок циклов=" + tCeilEvents + " x mThroughput(" + sAOPPumpThroughput + ")=" + tCeiling);
		O.println("[" + AOP_M + "] COVER-PUMP COLD (без кавера): источник=" + tColdSrc + " приёмник=" + tColdTgt + " (сумма0=" + sAOPPumpColdSum0 + ")");
		sAOPSeq.judge("COVER-PUMP HOT: жидкость перекачивается сама (приёмник > 0)", tHotTgt > 0, ">0", tHotTgt);
		sAOPSeq.judge("COVER-PUMP HOT: темп в пределах формулы кавера (<= потолок циклов x mThroughput)", tHotTgt <= tCeiling, "<=" + tCeiling, tHotTgt);
		sAOPSeq.conserve("COVER-PUMP HOT: консервация mb", sAOPPumpHotSum0, () -> tHotSrc + tHotTgt);
		sAOPSeq.judge("COVER-PUMP COLD: без кавера -> ничего не перекачано", tColdTgt == 0 && tColdSrc == AOP_WATER_PUMPSRC, "источник=" + AOP_WATER_PUMPSRC + " приёмник=0", "источник=" + tColdSrc + " приёмник=" + tColdTgt);

		sAOPSeq.done();
	}

	public static void gt6AutoOutProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sAOPProbeTick++;
		if (aServer.getPlayerList().getPlayers().isEmpty()) return;
		sAOPPlayer = aServer.getPlayerList().getPlayers().get(0);
		if (sAOPSeq == null) {
			sAOPSeq = new gregapi.probe.GT6ProbeStand.Seq(AOP_M)
				.at(200, GT_API_Proxy::gt6AutoOutProbeBuild)
				.at(210, GT_API_Proxy::gt6AutoOutProbeLoad)
				.at(260, GT_API_Proxy::gt6AutoOutProbeJudgeMachineAndBarrel)
				.at(400, GT_API_Proxy::gt6AutoOutProbeJudgePumpAndFinal);
		}
		sAOPSeq.tick(sAOPProbeTick);
	}

	// ========== [GT6-CHEMPROBE] ВРЕМЕННАЯ проба «Связка №7 — химический процесс multi-fluid» (Ф3.1, гейт run/gt6chemprobe.flag + -Pgt6probes, на каркасе GT6ProbeStand) ==========
	// ЭТАП А (разведка кода, ВЫЧИТАНА, не угадана — судимый канал ТОЛЬКО checkRecipe()/doActive() реальными тиками, ни один
	// судимый метод пробой не вызывается напрямую):
	// (а) Базовая машина ест жидкости рецепта через MultiTileEntityBasicMachine.java: mTanksInput/mTanksOutput — публичные
	//     массивы FluidTankGT (:106), размер = mRecipes.mInputFluidCount/mOutputFluidCount (константа RecipeMap, не рецепта).
	//     checkRecipe() (:688-783) матчит рецепт ДВУХФАЗНО: (1) mRecipes.findRecipe(...,mTanksInput,tInputs) (:717) ищет
	//     кандидат БЕЗ проверки количеств (Recipe.java findRecipeInternal:489, isRecipeInputEqual(F,T,...) внутри —
	//     aDontCheckStackSizes=T, матчит только по ТИПУ жидкости/предмета); (2) строгая проверка isRecipeInputEqual
	//     (aApplyRecipe,F,mTanksInput,tInputs) (:743, IFluidTank[]-оверлоад Recipe.java:822-840) — количества проверяются
	//     СТРОГО (aDontCheckStackSizes=F); если хоть одной жидкости не хватает — возврат FALSE ДО единого drain()
	//     (проверочный цикл проходит ПОЛНОСТЬЮ раньше первого списания, Recipe.java:826-830) => FOUND_RECIPE_BUT_DID_
	//     NOT_MEET_REQUIREMENTS, НИЧЕГО не списано (all-or-nothing, не частичное списание). При успехе — drain() КАЖДОЙ
	//     жидкости РОВНО на tFluid.getAmount() (:835), выходы кладутся в первый подходящий/пустой mTanksOutput[j]
	//     (MultiTileEntityBasicMachine.java:822-840). mParallel=4 (Mixer ULV, NBT_PARALLEL) не искажает тест: первая же
	//     consume-попытка (:743) списывает РОВНО 1×рецепт (aDecreaseStacksizeBySuccess=aApplyRecipe), вторая попытка
	//     добрать ещё 3× (:749, isRecipeInputEqual(int,...) Recipe.java:842-863) находит уже пустые танки (мы даём РОВНО
	//     1× объём) => tMaxProcessCount=1+0=1 — итог точно 1× выход, несмотря на mParallel=4.
	// (б) РЕАЛЬНЫЙ рецепт (RM.Mixer, 2 жидкости на входе + предмет-катализатор, жидкость на выходе) — Loader_Recipes_Chem.
	//     java:53: RM.Mixer.addRecipe1(T, 16, 112, OM.dust(MT.Ca), FL.array(MT.CO2.gas(U*3,T), FL.mul(tWater,3)),
	//     MT.H.gas(U*2,F), OM.dust(MT.CaCO3,U*5)) (tWater = первая итерация FL.waters(1000), :37 = FL.Water — обычная
	//     вода). ДОСЛОВНО: вход = 1×Calcium Dust + CO2(газ, U*3 материал-единиц) + Water(3000mb = 1000mb×3); выход =
	//     H2(газ, U*2 материал-единиц) + 5×Calcite Dust (CaCO3); EUt=16; duration=112 тиков. Точные mb читаются ЖИВЫМ
	//     сканом RM.Mixer.mRecipeList (gt6ChemProbeFindRecipe()), НЕ пересчитываются вручную (U-конверсия зависит от
	//     mGasUnit/mLiquidUnit конкретного материала, OreDictMaterial.java:1333-1348) — константы из памяти запрещены.
	// (в) Машина этого RM — "Mixer (ULV)" id=20181, класс MultiTileEntityBasicMachine (НЕ ...Electric!) — Loader_
	//     MultiTileEntities.java:1396, NBT_ENERGY_ACCEPTED=TD.Energy.RU (кинетическая, не EU), NBT_RECIPEMAP=RM.Mixer,
	//     NBT_INPUT=32 (mInputMin=16 mInputMax=64). Энергия — сетап-обход бухгалтерии (тот же приём, что ECP/AOP:
	//     mEnergy — ПУБЛИЧНОЕ поле самого базового класса, MultiTileEntityBasicMachine.java:103) — судимый канал
	//     checkRecipe()/doActive() остаётся реальным, обходится только доставка RU по сети (топология, не рецептный шов).
	// (г) Дифф порт/оригинал MultiTileEntityBasicMachine.checkRecipe/doActive, Recipe.isRecipeInputEqual(IFluidTank[]) —
	//     построчно 1:1 с gregtech6/ (engine-swap TileEntity->BlockEntity, IFluidHandler forge->neoforge, FluidStack.
	//     getAmount()/shrink вместо прямого .amount, drain(int,boolean)->drain(int,FluidAction)); расхождений в
	//     control-flow нет (см. отчёт агента). Снять при уборке фазы.
	private static final int CHEM_MIXER_ID = 20181; // Mixer (ULV) — Loader_MultiTileEntities.java:1396, класс MultiTileEntityBasicMachine
	private static final String CHEM_M = "GT6-CHEMPROBE";
	private static int sChemProbeTick = -1;
	private static ServerPlayer sChemPlayer;
	private static gregapi.probe.GT6ProbeStand.Seq sChemSeq;

	private static gregapi.tileentity.machines.MultiTileEntityBasicMachine sChemRun, sChemNeg, sChemCold;
	private static ItemStack sChemItemIn, sChemItemOut;
	private static FluidStack sChemFluidCO2, sChemFluidWater, sChemFluidH2;
	private static long sChemRecipeDuration, sChemRecipeEUt;
	private static int sChemOutSlot;

	private static long sChemNegCO2_0, sChemNegWater0, sChemNegItemIn0;
	private static long sChemColdCO2_0, sChemColdWater0, sChemColdItemIn0;

	private static boolean sChemRunSeenActive = F;
	private static int sChemRunDoneTick = -1;

	/** Живой скан RM.Mixer.mRecipeList — находит РОВНО тот рецепт из Loader_Recipes_Chem.java:53 (Ca-дуст + CO2-газ +
	 *  вода -> H2-газ + CaCO3-дуст), по ТИПАМ жидкостей/предмета (не по mb — те читаются ПОСЛЕ, живыми полями рецепта). */
	private static gregapi.recipes.Recipe gt6ChemProbeFindRecipe() {
		// F-идентификация по FL.regName(Fluid) (та же строковая связка, что mRecipeFluidMap.get(FL.regName(...)) в
		// findRecipeInternal:523 — доказанно рабочий канал движка), а НЕ по типу предмета-катализатора: item-вход
		// проходит OreDictManager.setStackArray_ (унификацию) внутри конструктора Recipe (aUnificate=T во всех
		// addRecipeN-обёртках) — унифицированный экземпляр может отличаться identity/типом от свежего OM.dust(MT.Ca)
		// в пробе (другой мод с тем же OreDict-тегом может быть "предпочтён"); предмет читается ИЗ найденного рецепта
		// (живое поле), не сверяется заранее.
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.world.level.material.Fluid tCO2Fluid = MT.CO2.gas(1, T).getFluid();
		net.minecraft.world.level.material.Fluid tWaterFluid = FL.Water.make(1).getFluid();
		net.minecraft.world.level.material.Fluid tH2Fluid = MT.H.gas(1, T).getFluid();
		String tCO2Name = FL.regName(tCO2Fluid), tWaterName = FL.regName(tWaterFluid), tH2Name = FL.regName(tH2Fluid);
		gregapi.recipes.Recipe rFound = null;
		int tCandidates = 0;
		for (gregapi.recipes.Recipe tR : RM.Mixer.mRecipeList) {
			if (tR.mFluidInputs.length != 2) continue;
			boolean tHasCO2 = F, tHasWater = F;
			for (FluidStack tF : tR.mFluidInputs) if (tF != null) {
				if (tCO2Name.equals(FL.regName(tF.getFluid()))) tHasCO2 = T;
				if (tWaterName.equals(FL.regName(tF.getFluid()))) tHasWater = T;
			}
			if (!tHasCO2 || !tHasWater) continue;
			tCandidates++;
			if (tCandidates <= 5) O.println("[" + CHEM_M + "] DIAG кандидат #" + tCandidates + ": item_in=" + (tR.mInputs.length > 0 ? tR.mInputs[0] : "(нет)") + " item_out=" + (tR.mOutputs.length > 0 ? tR.mOutputs[0] : "(нет)") + " fluid_out.length=" + tR.mFluidOutputs.length + " fluid_out0=" + (tR.mFluidOutputs.length > 0 ? tR.mFluidOutputs[0] : "(нет)"));
			if (rFound == null && tR.mFluidOutputs.length == 1 && tR.mFluidOutputs[0] != null && tH2Name.equals(FL.regName(tR.mFluidOutputs[0].getFluid()))) rFound = tR;
		}
		O.println("[" + CHEM_M + "] живой скан RM.Mixer.mRecipeList: всего=" + RM.Mixer.mRecipeList.size() + " CO2(" + tCO2Name + ")+Water(" + tWaterName + ")-кандидатов=" + tCandidates + " найден(H2=" + tH2Name + ")=" + (rFound != null));
		return rFound;
	}

	private static gregapi.tileentity.machines.MultiTileEntityBasicMachine gt6ChemProbeBuildMixer(ServerLevel aLevel, BlockPos aGround, String aLabel) {
		gregapi.tileentity.machines.MultiTileEntityBasicMachine tM = gregapi.probe.GT6ProbeStand.place(
			aLevel, sChemPlayer, aGround, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(CHEM_MIXER_ID),
			gregapi.tileentity.machines.MultiTileEntityBasicMachine.class, CHEM_M, aLabel + "-микшер");
		if (tM == null) throw new RuntimeException(aLabel + ": микшер не встал");
		return tM;
	}

	/** Тик 200: постройка RUN/NEG/COLD (одинаковая схема, разное наполнение в load()) + живой скан рецепта. */
	private static void gt6ChemProbeBuild() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		ServerLevel tLevel = sChemPlayer.level();
		O.println("========== [" + CHEM_M + "] Связка №7 — химический процесс multi-fluid (Ф3.1, на каркасе GT6ProbeStand) ==========");
		MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		if (tReg == null || tReg.getClassContainer(CHEM_MIXER_ID) == null) throw new RuntimeException("реестр/ID не найден: " + CHEM_MIXER_ID);
		O.println("[" + CHEM_M + "] ID подтверждён: микшер=" + tReg.getClassContainer(CHEM_MIXER_ID).mClass.getSimpleName() + "(" + CHEM_MIXER_ID + ")");

		gregapi.recipes.Recipe tRecipe = gt6ChemProbeFindRecipe();
		if (tRecipe == null) throw new RuntimeException("рецепт RM.Mixer (Ca+CO2+H2O->CaCO3+H2, Loader_Recipes_Chem.java:53) не найден живым сканом");
		if (tRecipe.mInputs.length != 1 || tRecipe.mOutputs.length != 1) throw new RuntimeException("найденный рецепт имеет неожиданную форму item_in.length=" + tRecipe.mInputs.length + " item_out.length=" + tRecipe.mOutputs.length + " (ожидалось 1/1)");
		sChemItemIn  = ST.copy(tRecipe.mInputs[0]);
		sChemItemOut = ST.copy(tRecipe.mOutputs[0]);
		net.minecraft.world.level.material.Fluid tCO2Fluid = MT.CO2.gas(1, T).getFluid();
		for (FluidStack tF : tRecipe.mFluidInputs) if (tF != null) {
			if (tF.getFluid() == tCO2Fluid) sChemFluidCO2 = tF.copy(); else sChemFluidWater = tF.copy();
		}
		sChemFluidH2 = tRecipe.mFluidOutputs[0].copy();
		sChemRecipeDuration = tRecipe.mDuration;
		sChemRecipeEUt = tRecipe.mEUt;
		sChemOutSlot = RM.Mixer.mInputItemsCount;
		O.println("[" + CHEM_M + "] рецепт ДОСЛОВНО (живой скан RM.Mixer.mRecipeList, Loader_Recipes_Chem.java:53): item_in=" + sChemItemIn
			+ " fluid_in=[" + sChemFluidCO2 + " (" + sChemFluidCO2.getAmount() + "mb), " + sChemFluidWater + " (" + sChemFluidWater.getAmount() + "mb)]"
			+ " -> fluid_out=" + sChemFluidH2 + " (" + sChemFluidH2.getAmount() + "mb) item_out=" + sChemItemOut
			+ " EUt=" + sChemRecipeEUt + " duration=" + sChemRecipeDuration + " outSlot=" + sChemOutSlot);

		BlockPos tBaseRun  = sChemPlayer.blockPosition().offset(4, 0, 4);
		BlockPos tBaseNeg  = sChemPlayer.blockPosition().offset(4, 0, 10);
		BlockPos tBaseCold = sChemPlayer.blockPosition().offset(4, 0, 16);
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBaseRun,  4, 1);
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBaseNeg,  4, 1);
		gregapi.probe.GT6ProbeStand.solidPad(tLevel, tBaseCold, 4, 1);

		sChemRun  = gt6ChemProbeBuildMixer(tLevel, tBaseRun,  "RUN");
		sChemNeg  = gt6ChemProbeBuildMixer(tLevel, tBaseNeg,  "NEG");
		sChemCold = gt6ChemProbeBuildMixer(tLevel, tBaseCold, "COLD");

		O.println("[" + CHEM_M + "] живые параметры машины (RUN): mInput=" + sChemRun.mInput + " mInputMin=" + sChemRun.mInputMin + " mInputMax=" + sChemRun.mInputMax
			+ " mTanksInput.length=" + sChemRun.mTanksInput.length + " mTanksOutput.length=" + sChemRun.mTanksOutput.length + " mEnergyTypeAccepted=" + sChemRun.mEnergyTypeAccepted + " mParallel=" + sChemRun.mParallel);
	}

	/** Тик 210: сетап-закладка ДОСЛОВНАЯ по рецепту (RUN — точно; NEG — CO2 наполовину; COLD — точно, но без энергии). */
	private static void gt6ChemProbeLoad() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		gregapi.probe.GT6ProbeStand.slotSet(sChemRun, 0, ST.copy(sChemItemIn));
		sChemRun.mTanksInput[0].setFluid(sChemFluidCO2.copy());
		sChemRun.mTanksInput[1].setFluid(sChemFluidWater.copy());
		sChemRun.mEnergy = 1_000_000_000L; // сетап-обход бухгалтерии RU (тот же приём, что ECP/AOP) — судимый канал checkRecipe()/doActive() реальный

		FluidStack tHalfCO2 = sChemFluidCO2.copyWithAmount(sChemFluidCO2.getAmount() / 2);
		gregapi.probe.GT6ProbeStand.slotSet(sChemNeg, 0, ST.copy(sChemItemIn));
		sChemNeg.mTanksInput[0].setFluid(tHalfCO2);
		sChemNeg.mTanksInput[1].setFluid(sChemFluidWater.copy());
		sChemNeg.mEnergy = 1_000_000_000L; // энергии хватает — недостача ТОЛЬКО в жидкости (PARTIAL-NEG)

		gregapi.probe.GT6ProbeStand.slotSet(sChemCold, 0, ST.copy(sChemItemIn));
		sChemCold.mTanksInput[0].setFluid(sChemFluidCO2.copy());
		sChemCold.mTanksInput[1].setFluid(sChemFluidWater.copy());
		// sChemCold.mEnergy остаётся 0 по умолчанию — COLD, судимый канал doWork()/doInactive()

		sChemNegCO2_0   = sChemNeg.mTanksInput[0].amount();  sChemNegWater0   = sChemNeg.mTanksInput[1].amount();  sChemNegItemIn0  = gregapi.probe.GT6ProbeStand.slotCount(sChemNeg,  0);
		sChemColdCO2_0  = sChemCold.mTanksInput[0].amount(); sChemColdWater0  = sChemCold.mTanksInput[1].amount(); sChemColdItemIn0 = gregapi.probe.GT6ProbeStand.slotCount(sChemCold, 0);

		O.println("[" + CHEM_M + "] тик " + sChemProbeTick + " загрузка: RUN CO2=" + sChemRun.mTanksInput[0].amount() + " Water=" + sChemRun.mTanksInput[1].amount() + " item=" + gregapi.probe.GT6ProbeStand.slotCount(sChemRun, 0) + " mEnergy=" + sChemRun.mEnergy
			+ "; NEG CO2=" + sChemNegCO2_0 + " (половина от " + sChemFluidCO2.getAmount() + ") Water=" + sChemNegWater0 + " item=" + sChemNegItemIn0
			+ "; COLD CO2=" + sChemColdCO2_0 + " Water=" + sChemColdWater0 + " item=" + sChemColdItemIn0 + " mEnergy=" + sChemCold.mEnergy);
	}

	/** Тик 224 (14 тиков после загрузки): RECIPE-RUN (1) — рецепт распознан и реально ПРОГРЕССИРУЕТ (не мгновенно). */
	private static void gt6ChemProbeJudgeStarted() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("[" + CHEM_M + "] ===== RECIPE-RUN (1) старт+прогресс (тик " + sChemProbeTick + ") =====");
		O.println("[" + CHEM_M + "] RUN mMaxProgress=" + sChemRun.mMaxProgress + " mProgress=" + sChemRun.mProgress + " mCurrentRecipe!=null=" + (sChemRun.mCurrentRecipe != null) + " mRunning=" + sChemRun.mRunning);
		sChemSeq.judge("RUN: рецепт распознан и запущен (mMaxProgress>0)", sChemRun.mMaxProgress > 0, ">0", sChemRun.mMaxProgress);
		sChemSeq.judge("RUN: прогресс идёт реальными тиками (0<mProgress<=mMaxProgress, не мгновенно)", sChemRun.mProgress > 0 && sChemRun.mProgress <= sChemRun.mMaxProgress, "(0.." + sChemRun.mMaxProgress + "]", sChemRun.mProgress);
	}

	/** Окно 211..299: следит за переходом RUN "активна -> простаивает" (первый тик ПОСЛЕ активной фазы) — факт. длительность
	 *  (урок §7 манифеста: однократный снимок в конце лжёт, копим первый переход через непрерывное наблюдение). */
	private static void gt6ChemProbeTrackRun() {
		if (sChemRun == null) return;
		if (sChemRun.mMaxProgress > 0) sChemRunSeenActive = T;
		else if (sChemRunSeenActive && sChemRunDoneTick < 0) sChemRunDoneTick = sChemProbeTick;
	}

	/** Тик 300: RECIPE-RUN (2,3,4) + CONSERVE + PARTIAL-NEG + COLD + DONE. */
	private static void gt6ChemProbeJudgeFinal() {
		java.io.PrintStream O = gregapi.data.CS.OUT;

		O.println("[" + CHEM_M + "] ===== RECIPE-RUN (2,3,4): списание/выход/длительность (тик " + sChemProbeTick + ") =====");
		long tRunItemIn  = gregapi.probe.GT6ProbeStand.slotCount(sChemRun, 0);
		long tRunItemOut = gregapi.probe.GT6ProbeStand.slotCount(sChemRun, sChemOutSlot);
		long tRunCO2     = sChemRun.mTanksInput[0].amount();
		long tRunWater   = sChemRun.mTanksInput[1].amount();
		long tRunH2Out   = 0; for (gregapi.fluid.FluidTankGT tT : sChemRun.mTanksOutput) tRunH2Out += tT.amount();
		long tFactualTicks = sChemRunDoneTick < 0 ? -1 : (sChemRunDoneTick - 210);
		O.println("[" + CHEM_M + "] RUN финал: itemIn(слот0)=" + tRunItemIn + " itemOut(слот" + sChemOutSlot + ")=" + tRunItemOut + " CO2=" + tRunCO2 + " Water=" + tRunWater + " H2out(сумма танков)=" + tRunH2Out
			+ " mMaxProgress=" + sChemRun.mMaxProgress + " mProgress=" + sChemRun.mProgress + " факт._тиков=" + tFactualTicks + " (рецепт duration=" + sChemRecipeDuration + " EUt=" + sChemRecipeEUt + ")");
		sChemSeq.judge("RUN (2а) предмет-катализатор Ca списан РОВНО", tRunItemIn == 0, 0, tRunItemIn);
		sChemSeq.judge("RUN (2б) CO2 списан РОВНО (mb-в-mb)", tRunCO2 == 0, 0, tRunCO2);
		sChemSeq.judge("RUN (2в) Water списан РОВНО (mb-в-mb)", tRunWater == 0, 0, tRunWater);
		sChemSeq.judge("RUN (3а) H2 выход РОВНО по рецепту", tRunH2Out == sChemFluidH2.getAmount(), sChemFluidH2.getAmount(), tRunH2Out);
		sChemSeq.judge("RUN (3б) CaCO3 выход РОВНО по рецепту", tRunItemOut == sChemItemOut.getCount(), sChemItemOut.getCount(), tRunItemOut);
		sChemSeq.judge("RUN (4) длительность в пределах рецептной (0<факт<=duration=" + sChemRecipeDuration + ")", tFactualTicks > 0 && tFactualTicks <= sChemRecipeDuration, "(0.." + sChemRecipeDuration + "]", tFactualTicks);
		sChemSeq.judge("CONSERVE: баланс до/после точно по рецепту (ничего не исчезло/не задвоилось сверх преобразования)",
			tRunItemIn == 0 && tRunCO2 == 0 && tRunWater == 0 && tRunH2Out == sChemFluidH2.getAmount() && tRunItemOut == sChemItemOut.getCount(),
			"вход(0,0,0)->выход(" + sChemFluidH2.getAmount() + "," + sChemItemOut.getCount() + ")",
			"вход(" + tRunItemIn + "," + tRunCO2 + "," + tRunWater + ")->выход(" + tRunH2Out + "," + tRunItemOut + ")");

		O.println("[" + CHEM_M + "] ===== PARTIAL-NEG (тик " + sChemProbeTick + ") =====");
		long tNegItemIn  = gregapi.probe.GT6ProbeStand.slotCount(sChemNeg, 0);
		long tNegItemOut = gregapi.probe.GT6ProbeStand.slotCount(sChemNeg, sChemOutSlot);
		long tNegCO2     = sChemNeg.mTanksInput[0].amount();
		long tNegWater   = sChemNeg.mTanksInput[1].amount();
		O.println("[" + CHEM_M + "] NEG: CO2=" + tNegCO2 + " (было " + sChemNegCO2_0 + ", половина от " + sChemFluidCO2.getAmount() + ") Water=" + tNegWater + " (было " + sChemNegWater0 + ") item=" + tNegItemIn + " (было " + sChemNegItemIn0 + ") mMaxProgress=" + sChemNeg.mMaxProgress);
		sChemSeq.judge("NEG: рецепт НЕ стартовал (mMaxProgress==0, CO2 вдвое меньше нужного)", sChemNeg.mMaxProgress == 0, 0, sChemNeg.mMaxProgress);
		sChemSeq.judge("NEG: CO2 не списан (недостающая жидкость цела)", tNegCO2 == sChemNegCO2_0, sChemNegCO2_0, tNegCO2);
		sChemSeq.judge("NEG: Water не списан (полная жидкость тоже цела — all-or-nothing)", tNegWater == sChemNegWater0, sChemNegWater0, tNegWater);
		sChemSeq.judge("NEG: предмет-катализатор цел", tNegItemIn == sChemNegItemIn0, sChemNegItemIn0, tNegItemIn);
		sChemSeq.judge("NEG: выход пуст (ничего не произведено)", tNegItemOut == 0, 0, tNegItemOut);

		O.println("[" + CHEM_M + "] ===== COLD (тик " + sChemProbeTick + ") =====");
		long tColdItemIn  = gregapi.probe.GT6ProbeStand.slotCount(sChemCold, 0);
		long tColdItemOut = gregapi.probe.GT6ProbeStand.slotCount(sChemCold, sChemOutSlot);
		long tColdCO2     = sChemCold.mTanksInput[0].amount();
		long tColdWater   = sChemCold.mTanksInput[1].amount();
		O.println("[" + CHEM_M + "] COLD: mEnergy=" + sChemCold.mEnergy + " CO2=" + tColdCO2 + " (было " + sChemColdCO2_0 + ") Water=" + tColdWater + " (было " + sChemColdWater0 + ") item=" + tColdItemIn + " (было " + sChemColdItemIn0 + ") mMaxProgress=" + sChemCold.mMaxProgress);
		sChemSeq.judge("COLD: без энергии рецепт НЕ стартовал", sChemCold.mMaxProgress == 0, 0, sChemCold.mMaxProgress);
		sChemSeq.judge("COLD: CO2 цел", tColdCO2 == sChemColdCO2_0, sChemColdCO2_0, tColdCO2);
		sChemSeq.judge("COLD: Water цел", tColdWater == sChemColdWater0, sChemColdWater0, tColdWater);
		sChemSeq.judge("COLD: предмет-катализатор цел", tColdItemIn == sChemColdItemIn0, sChemColdItemIn0, tColdItemIn);
		sChemSeq.judge("COLD: выход пуст", tColdItemOut == 0, 0, tColdItemOut);

		sChemSeq.done();
	}

	public static void gt6ChemProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sChemProbeTick++;
		if (aServer.getPlayerList().getPlayers().isEmpty()) return;
		sChemPlayer = aServer.getPlayerList().getPlayers().get(0);
		if (sChemSeq == null) {
			sChemSeq = new gregapi.probe.GT6ProbeStand.Seq(CHEM_M)
				.at(200, GT_API_Proxy::gt6ChemProbeBuild)
				.at(210, GT_API_Proxy::gt6ChemProbeLoad)
				.at(224, GT_API_Proxy::gt6ChemProbeJudgeStarted)
				.window(211, 299, GT_API_Proxy::gt6ChemProbeTrackRun)
				.at(300, GT_API_Proxy::gt6ChemProbeJudgeFinal);
		}
		sChemSeq.tick(sChemProbeTick);
	}

	// ========== [GT6-STEAMFARMPROBE] ВРЕМЕННАЯ проба «Связка №8 — паровая ферма N бойлеров → 1 турбина» (Ф3.1, гейт run/gt6steamfarmprobe.flag + -Pgt6probes, на каркасе GT6ProbeStand) ==========
	// Запрос игрока: "паровые схемы, где несколько бойлеров крутят одну турбину" + живой тест ВСКРЫТОЙ связкой №4
	// семантики недонапряжения (Root.doEnergyInjection:886): 1 Pb-бойлер разгоняет цепь лишь до пакета динамо <16
	// (LV min) — батарея пуста; N бойлеров обязаны поднять пакет выше порога.
	// ЭТАП А v2 (ПЕРВАЯ схема — паровой манифольд трубами к ОДНОЙ турбине — ОПРОВЕРГНУТА ЖИВЫМ ПРОГОНОМ, см. ниже
	// "ЭТАП А v1 [ОПРОВЕРГНУТО]"; это ФИНАЛЬНАЯ рабочая схема): турбина физически НЕ может стоять за трубой —
	// агрегация переносится на РОТАЦИОННУЮ (RU) сторону через MultiTileEntityGearBox, который РЕАЛЬНО СКЛАДЫВАЕТ
	// несколько ОДНОТИКОВЫХ входов (MultiTileEntityGearBox.java:380-395: "There already has been at least one Input
	// during this Tick. Add more Power." — mCurrentSpeed=Math.min(tSpeed,mCurrentSpeed) [держит скорость по
	// САМОМУ СЛАБОМУ], mCurrentPower+=aPower [СКЛАДЫВАЕТ мощность/количество] — это ЕДИНСТВЕННЫЙ уже существующий
	// в коде центр, который АГРЕГИРУЕТ несколько независимых источников в БОЛЬШИЙ единый пакет; ни труба (см. v1),
	// ни провод (см. ниже "почему НЕ провод"), ни вал (чистый прямой транзит OPOS[aSide], MultiTileEntityAxle.java:
	// 105-117, БЕЗ сложения) этого не делают). Схема: КАЖДАЯ из N колонн генератор->бойлер->турбина — ПРЯМОЙ стек
	// (турбина СТОИТ НА бойлере, БЕЗ трубы — тот же путь getAdjacentTank(SIDE_UP)->FL.move->fillSided->fill(Direction,
	// ...), что уже доказанно работает в ENERGYCHAINPROBE), турбина эмитит RU СТРОГО ВВЕРХ (mFacing=UP гейт, см. v1)
	// — НАД КАЖДОЙ турбиной стоит СВОЯ шестерня (никаких сложных гейтов: gearbox читает соседей КАЖДЫЙ тик заново
	// через getAdjacentTileEntity, никакого connect()/ITileEntityConnector нет вовсе — MultiTileEntityGearBox extends
	// TileEntityBase07Paintable, НЕ коннектор). Шестерни выстроены В ЛИНИЮ (та же горизонтальная геометрия, что и
	// манифольд v1, но БЕЗ падежа "сторона не резолвится" — здесь ВСЯ маршрутизация энергии идёт через ГТ6-родной
	// ITileEntityEnergy.doEnergyInjection с РЕАЛЬНОЙ стороной на каждом хопе, а не через neo-сайдлес IFluidHandler):
	// каждая шестерня[i] mAxleGear (публичное поле, бит-маска "где смонтирована шестерня", без axle — верхние 2 бита
	// =0) = SBIT_D(вход от СВОЕЙ турбины) | (SBIT_U ТОЛЬКО у шестерни[0], выход к динамо) | (SBIT_W если i>0, к
	// шестерне[i-1]) | (SBIT_E если i<N-1, от шестерни[i+1]) — валидность проверена ВРУЧНУЮ по MultiTileEntityGearBox.
	// checkGears():271-310 (case 2 "corner" :284-293 и case 3/4 "triangle" :294-306, ни у одной из наших масок не
	// задействованы ВСЕ 3 оси разом → mGearsWork=true); mGearsWork — ПУБЛИЧНОЕ поле, НЕ пересчитывается автоматически
	// при прямой записи mAxleGear рефлексией НЕ нужной (все поля gearbox — public), поэтому пробa вызывает checkGears()
	// САМА и пишет mGearsWork явно — тот же публичный метод, каким реально пользуется readFromNBT2:70/onToolClick2.
	// Шестерня[0] (агрегатор) относит СУММУ на динамо(mFacing=UP)->батарея-LV(mFacing=NORTH), как раньше. Почему НЕ
	// провод (EU): проверено по коду — MultiTileEntityWireElectric.transferElectricity (:170-189) релеит КАЖДЫЙ
	// входящий пакет СВОИМ РАЗМЕРОМ (aVoltage только УМЕНЬШАЕТСЯ на mLoss, никогда не растёт от параллельных
	// источников) — параллельное подключение N динамо к ОДНОЙ батарее через провод не поднимает size(=voltage)
	// ни на йоту (только amperage/count), а порог §4 (getEnergySizeInputMin) — это ИМЕННО ограничение по size;
	// подниматься способен только САМ пакет ОДНОГО эмиттера, а его пакет = f(накопленный mStorage.mEnergy) —
	// вот почему нужно копить БОЛЬШЕ RU в ОДНОМ dynamo.mStorage (через gearbox), а не параллелить много dynamo.
	// ЭТАП А v1 [ОПРОВЕРГНУТО, см. ↑]: ПЕРВАЯ гипотеза (жидкостные трубы Cu id26102, манифольд над бойлерами,
	// турбина на манифольде) СТРОИЛАСЬ и ЖИВОЙ ПРОГОН (2 прогона, идентичный результат) показал: бойлер->манифольд
	// работал (mTanks[0].amount() рос 0->600=mCapacity потолок пропускной трубы), но манифольд->турбина НИКОГДА
	// не заполнялся (турбина.mTank=0 все 900 тиков, DIAG-CONNECT подтвердил connected(DOWN)=connected(UP)=true —
	// связи были верны). Корень (§6.1, найден ЖИВОЙ трассировкой ДО перестройки, не угадан): MultiTileEntityPipeFluid.
	// distribute() (:410,:428) зовёт adjacent-tank цель через ГЕНЕРИЧЕСКИЙ neo-интерфейс IFluidHandler.fill(FluidStack,
	// FluidAction) БЕЗ стороны — TileEntityBase01Root.java:815 "return fill((Direction)null, aFluid, aAction.execute());"
	// — это резолвится в UT.Code.side(null)=SIDE_ANY(6) (:807 "sideless neo-вызов = сторона null -> SIDE_ANY(6),
	// родная GT6-конвенция «любая сторона»"). Турбина же требует ТОЧНОЕ совпадение стороны: getFluidTankFillable2
	// (MultiTileEntityTurbineSteam.java:116) "return isInput(aSide) && ...", isInput(aSide){return aSide==OPOS[mFacing];}
	// (:129) — SIDE_ANY(6) НИКОГДА не равен ни одному реальному 0-5, значит null. Контраст: бочка (стенд №2) принимает
	// БЕЗ гейта по стороне (TileEntityBase08Barrel.java:298 "return (mMode&B[1])!=0?null:mTank;" — не проверяет aSide
	// вовсе), поэтому FLUIDPIPEPROBE (труба->бочка) прошёл, а труба->турбина — структурно не может (не факт постройки,
	// а СВОЙСТВО КОНТРАКТА neo IFluidHandler у ЛЮБОЙ трубы-в-любую-турбину, не только в этом стенде). Прямой хоп
	// бойлер->турбина (БЕЗ трубы, как в ENERGYCHAINPROBE) РАБОТАЕТ, потому что там сторона идёт через ДРУГОЙ путь:
	// FL.move->fill_(DelegatorTileEntity,...)->fillSided (FL.java) — "return aFluidHandler instanceof TileEntityBase01Root
	// tGT ? tGT.fill(FORGE_DIR[aSide], aFluid, aDoFill) : ..." — РЕАЛЬНАЯ сторона от делегатора, НЕ sideless. Задание
	// прямо предусматривало этот исход: "Если агрегация по коду физически невозможна — перестрой схему (например
	// турбины на КАЖДЫЙ бойлер и агрегация RU валами?)" — перестроено на agregацию ШЕСТЕРНЯМИ (валы транзитны и не
	// складывают, см. ↑). Фронт горелки — НАРУЖУ ОТ РЯДА (перпендикулярно оси, приём 8-горелочного кольца
	// CRUCIBLEPROBE, MultiTileEntityGeneratorSolid.java:111-114 гейт !hasCollide&&oxygen) — ряд можно ставить
	// ВПЛОТНУЮ (dx=1), клиренс горелки на ПЕРПЕНДИКУЛЯРНОЙ оси, соседнюю колонну не заденет. Снять при уборке фазы.
	// ИТОГ v3 [ЖИВОЙ ПРОГОН ВСКРЫЛ ВТОРОЙ БАРЬЕР, ЧЕСТНЫЙ FAIL ТОПОЛОГИИ АГРЕГАЦИИ]: схема v2 (шестерни-цепочка)
	// ПОСТРОИЛАСЬ и ЗАПУСТИЛАСЬ (checkGears()=true у всех троих, mAxleGear=[35,49,17] по формуле выше), НО живой
	// прогон (DIAG-JAM, тики 211-900) показал: шестерня[0] (биты D+U+E=35, "агрегатор") и шестерня[1] (биты
	// D+W+E=49, "средняя") получили mJammed=TRUE начиная с тика 213 НАВСЕГДА (шестерня[2], биты D+W=17, чистый
	// "угол" — 2 грани — НЕ заклинила, mJammed=false все 900 тиков). Корень (§6.1, вручную пересчитан ДО и
	// подтверждён живым mRotationData/mJammed ПОСЛЕ) — MultiTileEntityGearBox.getRotations():227-268: при получении
	// ВТОРОГО входа за тот же тик с ДРУГОЙ, не совпадающей по паритету грани, вычисленный tRotationData СРАВНИВАЕТСЯ
	// с уже установленным mRotationData (:381-389 "There already has been at least one Input during this Tick...
	// if (tRotationData != mRotationData) { ... mJammed = T; }") — для 3-гранного узла D+U+E (или D+W+E) вход С ОСИ
	// D/U (вертикаль) и вход С ГРАНИ E/W (горизонталь) дают РАЗНЫЕ tRotationData (пересчитано вручную по коду:
	// getRotations(D,false) на маске 35 = 96, getRotations(E,false) на ТОЙ ЖЕ маске 35 = 67, 96≠67 → джем; для маски
	// 49: getRotations(D,false)=112 против getRotations(E или W,false), аналогично конфликт) — это РЕАЛЬНАЯ
	// механика сцепления зубьев (перпендикулярные шестерни ДОЛЖНЫ вращаться в согласованных направлениях; "вертикаль
	// как вход" и "горизонталь как вход" в ОДНОМ 3-гранном узле физически несовместимы для ОДНОВРЕМЕННОГО приёма
	// с двух источников), НЕ баг постройки пробы — тот же класс ограничения, что тултип "Gears are interlocked
	// wrongly!" (MultiTileEntityGearBox.java:87,94-95) описывает игроку. После джема isEnergyAcceptingFrom (:413,
	// "(aTheoretical||!mJammed)&&...") НАВСЕГДА отвергает ВСЕ дальнейшие входы на ЭТОЙ шестерне — batch с турбины[1]
	// и весь хвост цепи от шестерни[2] цепи после первого столкновения не проходят НИКОГДА (что и наблюдается:
	// шестерня[0].mCurrentPower/mTransferredLast=0 с тика 213 до конца окна). Вывод: из ДВУХ опробованных схем
	// агрегации (v1 труба, v2 шестерни-цепочка) ОБЕ физически заблокированы кодом GT6 по РАЗНЫМ причинам (v1 —
	// программный контракт neo IFluidHandler, v2 — механика паритета вращения шестерён) — задание САМО допускало
	// этот исход ("сдай обоснованный FAIL топологии"). BASELINE (прямой стек, без агрегации) и рост ПРОИЗВОДСТВА
	// пара ×N (судья "суммарный пар... кратно числу бойлеров" — PASS, коэффициент РОВНО 3.0) остаются ДОКАЗАННЫМИ;
	// судьи "LV-батарея начала принимать"/"пакет вырос" у FARM закономерно FAIL — заносится в судьи ЧЕСТНО, не
	// подгоняется. Снять при уборке фазы.
	private static final int STF_GEN_ID     = 1199;  // Brick Burning Box (Solid) — тот же генератор, что ENERGYCHAINPROBE/CRUCIBLEPROBE
	private static final int STF_BOILER_ID  = 1200;  // Steam Boiler Tank (Pb) — тот же ECP_BOILER_ID, mOutput=32
	private static final int STF_TURBINE_ID = 1518;  // Steam Turbine (Invar) — тот же ECP_TURBINE_ID
	private static final int STF_DYNAMO_ID  = 10111; // Electric Dynamo (T1) — тот же ECP_DYNAMO_ID
	private static final int STF_BATBOX_ID  = 10081; // Battery Box (LV) — окно приёма [16..64] (mInput=32, TileEntityBase01Root.java:893-894 min/max=rec/2, rec*2)
	private static final int STF_GEARBOX_ID = 24819; // Custom Bronze Gearbox — Loader_MultiTileEntities.java:1682, NBT_INPUT=VMAX[1]=64 (VMAX CS.java:154) — больше турбинного RU-пакета (mEnergyOUT.mRec=32)
	private static final int STF_N          = 3;     // число бойлеров фермы (задание: "возьми 3-4")
	private static final String STF_M = "GT6-STEAMFARMPROBE";
	private static int sSTFProbeTick = -1;
	private static net.minecraft.server.level.ServerPlayer sSTFPlayer;
	private static gregapi.probe.GT6ProbeStand.Seq sSTFSeq;
	private static long sSTFBaseMaxPkt = 0, sSTFFarmMaxPkt = 0;

	// BASELINE-1 — ПРЯМОЙ стек (без шестерни, 1 колонна не нуждается в агрегации): та же семантика недонапряжения,
	// что ENERGYCHAINPROBE CHAIN, приёмник — LV (id10081)
	private static gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick sSTFBaseGen;
	private static gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank sSTFBaseBoiler;
	private static gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam sSTFBaseTurbine;
	private static gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric sSTFBaseDynamo;
	private static gregapi.tileentity.energy.TileEntityBase10EnergyBatBox sSTFBaseBattery;

	// FARM-N (n=STF_N) — N колонн, КАЖДАЯ турбина->СВОЯ шестерня, шестерни в цепь суммируют RU в ОДНО динамо->батарею
	private static gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[] sSTFFarmGens;
	private static gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[] sSTFFarmBoilers;
	private static gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam[] sSTFFarmTurbines;
	private static gregtech.tileentity.energy.transformers.MultiTileEntityGearBox[] sSTFFarmGears;
	private static gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric sSTFFarmDynamo;
	private static gregapi.tileentity.energy.TileEntityBase10EnergyBatBox sSTFFarmBattery;
	private static long sSTFFarmEu0;

	// COLD (n=STF_N) — тот же аппарат (с шестернями), НИКОГДА не зажигается/не заряжается (контроль)
	private static gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[] sSTFColdGens;
	private static gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[] sSTFColdBoilers;
	private static gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam[] sSTFColdTurbines;
	private static gregtech.tileentity.energy.transformers.MultiTileEntityGearBox[] sSTFColdGears;
	private static gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric sSTFColdDynamo;
	private static gregapi.tileentity.energy.TileEntityBase10EnergyBatBox sSTFColdBattery;

	/** Строит ОДНУ колонну генератор->бойлер->турбина, турбина ПРЯМО НА бойлере (БЕЗ трубы — доказанный рабочий
	 *  путь ENERGYCHAINPROBE: getAdjacentTank(SIDE_UP)->FL.move->fill_(DelegatorTileEntity)->fillSided с РЕАЛЬНОЙ
	 *  стороной, см. комментарий блока выше). Генератор развёрнут НАРУЖУ от ряда (SIDE_SOUTH, перпендикулярно оси
	 *  ряда EAST/+X — тот же приём CRUCIBLEPROBE). aBase — позиция земли ПОД генератором. */
	private static Object[] gt6SteamFarmProbeBuildColumn(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aBase, String aLabel) {
		gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick tGen = gregapi.probe.GT6ProbeStand.place(
			aLevel, sSTFPlayer, aBase, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_GEN_ID),
			gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick.class, STF_M, aLabel + "-генератор");
		if (tGen == null) throw new RuntimeException(aLabel + ": генератор не встал");
		tGen.setPrimaryFacing(SIDE_SOUTH); // перпендикулярно оси ряда — клиренс не заденет соседнюю колонну
		net.minecraft.core.BlockPos tGenPos = aBase.above();
		aLevel.setBlock(tGenPos.relative(FORGE_DIR[tGen.mFacing]), Blocks.AIR.defaultBlockState(), 3);
		gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank tBoiler = gregapi.probe.GT6ProbeStand.place(
			aLevel, sSTFPlayer, tGenPos, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_BOILER_ID),
			gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank.class, STF_M, aLabel + "-бойлер");
		if (tBoiler == null) throw new RuntimeException(aLabel + ": бойлер не встал");
		net.minecraft.core.BlockPos tBoilerPos = tGenPos.above();
		gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam tTurbine = gregapi.probe.GT6ProbeStand.place(
			aLevel, sSTFPlayer, tBoilerPos, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_TURBINE_ID),
			gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam.class, STF_M, aLabel + "-турбина");
		if (tTurbine == null) throw new RuntimeException(aLabel + ": турбина не встала");
		tTurbine.setPrimaryFacing(SIDE_UP); // приём пара СНИЗУ (от бойлера, ПРЯМОЙ IFluidHandler-хоп, side-aware), эмиссия RU вверх — TurbineSteam.java:129-130
		return new Object[]{tGen, tBoiler, tTurbine};
	}

	/** Тик 200 (BASELINE): построить 1 колонну + динамо+батарея НАПРЯМУЮ на турбине (как ENERGYCHAINPROBE CHAIN). */
	private static void gt6SteamFarmProbeBuildBaseline(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aGround) {
		Object[] c = gt6SteamFarmProbeBuildColumn(aLevel, aGround, "BASELINE");
		sSTFBaseGen = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick) c[0];
		sSTFBaseBoiler = (gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank) c[1];
		sSTFBaseTurbine = (gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam) c[2];
		gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric tDynamo = gregapi.probe.GT6ProbeStand.place(
			aLevel, sSTFPlayer, sSTFBaseTurbine.getBlockPos(), net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_DYNAMO_ID),
			gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric.class, STF_M, "BASELINE-динамо");
		if (tDynamo == null) throw new RuntimeException("BASELINE: динамо не встало");
		tDynamo.setPrimaryFacing(SIDE_UP);
		sSTFBaseDynamo = tDynamo;
		gregapi.tileentity.energy.TileEntityBase10EnergyBatBox tBattery = gregapi.probe.GT6ProbeStand.place(
			aLevel, sSTFPlayer, tDynamo.getBlockPos(), net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_BATBOX_ID),
			gregapi.tileentity.energy.TileEntityBase10EnergyBatBox.class, STF_M, "BASELINE-батарея");
		if (tBattery == null) throw new RuntimeException("BASELINE: батарея не встала");
		tBattery.setPrimaryFacing(SIDE_NORTH); // isInput=aSide!=mFacing -> принимает снизу (DOWN!=NORTH)
		sSTFBaseBattery = tBattery;
		gregapi.data.CS.OUT.println("[" + STF_M + "] BASELINE построен: 1 колонна (прямой стек, без шестерни), турбина@" + sSTFBaseTurbine.getBlockPos() + " динамо@" + tDynamo.getBlockPos() + " батарея-LV@" + tBattery.getBlockPos());
	}

	/** Тик 200 (FARM/COLD): построить aN колонн + aN шестерён в цепь (см. комментарий блока — mAxleGear-маска на
	 *  колонну) + динамо+батарея НАД шестернёй[0] (агрегатор). aRowBase — земля ПОД колонной[0] (колонна[i] на
	 *  aRowBase.offset(i,0,0), ряд вдоль EAST/+X). */
	private static Object[] gt6SteamFarmProbeBuildFarm(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aRowBase, String aLabel) {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		int n = STF_N;
		gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[] rGens = new gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[n];
		gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[] rBoilers = new gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[n];
		gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam[] rTurbines = new gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam[n];
		gregtech.tileentity.energy.transformers.MultiTileEntityGearBox[] rGears = new gregtech.tileentity.energy.transformers.MultiTileEntityGearBox[n];
		for (int i = 0; i < n; i++) {
			net.minecraft.core.BlockPos tBase = aRowBase.offset(i, 0, 0);
			Object[] c = gt6SteamFarmProbeBuildColumn(aLevel, tBase, aLabel + "[" + i + "]");
			rGens[i] = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick) c[0];
			rBoilers[i] = (gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank) c[1];
			rTurbines[i] = (gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam) c[2];
			gregtech.tileentity.energy.transformers.MultiTileEntityGearBox tGear = gregapi.probe.GT6ProbeStand.place(
				aLevel, sSTFPlayer, rTurbines[i].getBlockPos(), net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_GEARBOX_ID),
				gregtech.tileentity.energy.transformers.MultiTileEntityGearBox.class, STF_M, aLabel + "-шестерня[" + i + "]");
			if (tGear == null) throw new RuntimeException(aLabel + ": шестерня[" + i + "] не встала");
			int tBits = SBIT_D; // вход СНИЗУ от своей турбины — всегда
			if (i == 0) tBits |= SBIT_U;        // агрегатор: выход ВВЕРХ к динамо
			if (i > 0) tBits |= SBIT_W;          // связь к предыдущей (индекс-1, к агрегатору)
			if (i < n-1) tBits |= SBIT_E;        // связь к следующей (индекс+1)
			tGear.mAxleGear = (short) tBits;     // публичное поле — "где смонтирована шестерня", БЕЗ axle (верхние 2 бита=0)
			tGear.mGearsWork = tGear.checkGears(); // ПУБЛИЧНЫЙ метод (MultiTileEntityGearBox.java:271), тот же вызов, что readFromNBT2:70/onToolClick2 — не пересчитывается автоматически при прямой записи mAxleGear
			rGears[i] = tGear;
		}
		gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric tDynamo = gregapi.probe.GT6ProbeStand.place(
			aLevel, sSTFPlayer, rGears[0].getBlockPos(), net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_DYNAMO_ID),
			gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric.class, STF_M, aLabel + "-динамо");
		if (tDynamo == null) throw new RuntimeException(aLabel + ": динамо не встало");
		tDynamo.setPrimaryFacing(SIDE_UP);
		gregapi.tileentity.energy.TileEntityBase10EnergyBatBox tBattery = gregapi.probe.GT6ProbeStand.place(
			aLevel, sSTFPlayer, tDynamo.getBlockPos(), net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_BATBOX_ID),
			gregapi.tileentity.energy.TileEntityBase10EnergyBatBox.class, STF_M, aLabel + "-батарея");
		if (tBattery == null) throw new RuntimeException(aLabel + ": батарея не встала");
		tBattery.setPrimaryFacing(SIDE_NORTH);
		O.println("[" + STF_M + "] " + aLabel + " построен: N=" + n + " колонн, шестерни в цепь (биты=" + java.util.Arrays.toString(new int[]{rGears[0].mAxleGear, n>1?rGears[1].mAxleGear:-1, n>2?rGears[n-1].mAxleGear:-1}) + "), mGearsWork=" + java.util.Arrays.asList(rGears).stream().map(g -> g.mGearsWork).toList() + ", динамо@" + tDynamo.getBlockPos() + " батарея-LV@" + tBattery.getBlockPos());
		return new Object[]{rGens, rBoilers, rTurbines, rGears, tDynamo, tBattery};
	}

	/** Тик 200: построить BASELINE(1 колонна)/FARM(N колонн+шестерни)/COLD(N колонн+шестерни) (свежая зона
	 *  Z=42/50/60, за пределами всех прежних стендов — макс. Z был 34 у AUTOOUTPROBE). */
	private static void gt6SteamFarmProbeBuild() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = sSTFPlayer.level();
		O.println("========== [" + STF_M + "] Связка №8 — паровая ферма N бойлеров → 1 турбина (Ф3.1, на каркасе GT6ProbeStand) ==========");
		MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		int[] tIds = {STF_GEN_ID, STF_BOILER_ID, STF_TURBINE_ID, STF_DYNAMO_ID, STF_BATBOX_ID, STF_GEARBOX_ID};
		for (int tId : tIds) if (tReg == null || tReg.getClassContainer(tId) == null) throw new RuntimeException("реестр/ID не найден: " + tId);
		O.println("[" + STF_M + "] ID подтверждены: генератор=" + tReg.getClassContainer(STF_GEN_ID).mClass.getSimpleName() + "(" + STF_GEN_ID + ") бойлер=" + tReg.getClassContainer(STF_BOILER_ID).mClass.getSimpleName() + "(" + STF_BOILER_ID + ") турбина=" + tReg.getClassContainer(STF_TURBINE_ID).mClass.getSimpleName() + "(" + STF_TURBINE_ID + ") шестерня=" + tReg.getClassContainer(STF_GEARBOX_ID).mClass.getSimpleName() + "(" + STF_GEARBOX_ID + ") динамо=" + tReg.getClassContainer(STF_DYNAMO_ID).mClass.getSimpleName() + "(" + STF_DYNAMO_ID + ") батарея-LV=" + tReg.getClassContainer(STF_BATBOX_ID).mClass.getSimpleName() + "(" + STF_BATBOX_ID + ")");

		net.minecraft.core.BlockPos tRowBase = sSTFPlayer.blockPosition().offset(4, 0, 42);
		net.minecraft.core.BlockPos tRowFarm = sSTFPlayer.blockPosition().offset(4, 0, 50);
		net.minecraft.core.BlockPos tRowCold = sSTFPlayer.blockPosition().offset(4, 0, 60);

		gt6SteamFarmProbeBuildBaseline(tLevel, tRowBase);

		Object[] tFarm = gt6SteamFarmProbeBuildFarm(tLevel, tRowFarm, "FARM");
		sSTFFarmGens     = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[]) tFarm[0];
		sSTFFarmBoilers  = (gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[]) tFarm[1];
		sSTFFarmTurbines = (gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam[]) tFarm[2];
		sSTFFarmGears    = (gregtech.tileentity.energy.transformers.MultiTileEntityGearBox[]) tFarm[3];
		sSTFFarmDynamo   = (gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric) tFarm[4];
		sSTFFarmBattery  = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tFarm[5];

		Object[] tCold = gt6SteamFarmProbeBuildFarm(tLevel, tRowCold, "COLD");
		sSTFColdGens     = (gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[]) tCold[0];
		sSTFColdBoilers  = (gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[]) tCold[1];
		sSTFColdTurbines = (gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam[]) tCold[2];
		sSTFColdGears    = (gregtech.tileentity.energy.transformers.MultiTileEntityGearBox[]) tCold[3];
		sSTFColdDynamo   = (gregtech.tileentity.energy.converters.MultiTileEntityDynamoElectric) tCold[4];
		sSTFColdBattery  = (gregapi.tileentity.energy.TileEntityBase10EnergyBatBox) tCold[5];

		O.println("[" + STF_M + "] живые параметры: бойлер mOutput=" + gregapi.probe.GT6ProbeStand.fldLong(sSTFFarmBoilers[0], "mOutput") + " mCapacity=" + gregapi.probe.GT6ProbeStand.fldLong(sSTFFarmBoilers[0], "mCapacity") + "; шестерня[0].mMaxThroughPut=" + sSTFFarmGears[0].mMaxThroughPut);
		O.println("[" + STF_M + "] турбина mEnergyIN(min/rec/max)=" + sSTFFarmTurbines[0].mEnergyIN.mMin + "/" + sSTFFarmTurbines[0].mEnergyIN.mRec + "/" + sSTFFarmTurbines[0].mEnergyIN.mMax + " mEnergyOUT(min/rec/max)=" + sSTFFarmTurbines[0].mEnergyOUT.mMin + "/" + sSTFFarmTurbines[0].mEnergyOUT.mRec + "/" + sSTFFarmTurbines[0].mEnergyOUT.mMax);
		O.println("[" + STF_M + "] динамо mEnergyIN(min/rec/max)=" + sSTFFarmDynamo.mEnergyIN.mMin + "/" + sSTFFarmDynamo.mEnergyIN.mRec + "/" + sSTFFarmDynamo.mEnergyIN.mMax + " mEnergyOUT(min/rec/max)=" + sSTFFarmDynamo.mEnergyOUT.mMin + "/" + sSTFFarmDynamo.mEnergyOUT.mRec + "/" + sSTFFarmDynamo.mEnergyOUT.mMax + " mStorage.mCapacity=" + sSTFFarmDynamo.mStorage.mCapacity + "; батарея-LV mInput=" + sSTFFarmBattery.mInput + " min=" + sSTFFarmBattery.getEnergySizeInputMin(TD.Energy.EU, SIDE_DOWN) + " max=" + sSTFFarmBattery.getEnergySizeInputMax(TD.Energy.EU, SIDE_DOWN));
	}

	/** Разжечь+предзарядить ОДИН массив колонн: КАЖДЫЙ бойлер получает СВОЙ горящий генератор (32 угля) и СВОЙ
	 *  пред-заряд пара cap/2+100000 (выше порога эмиссии :139-140 — тот же приём ENERGYCHAINPROBE, обходит ТОЛЬКО
	 *  начальную точку резервуара; сама конверсия/эмиссия/агрегация — реальные тики). Переиспользует ГОТОВЫЙ
	 *  хелпер gt6EnergyChainProbeBoilerFill (тот же класс GT_API_Proxy, ECP-секция выше) — не дублирует рефлексию. */
	private static void gt6SteamFarmProbeLoadRow(gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[] aGens, gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[] aBoilers) {
		for (int i = 0; i < aGens.length; i++) {
			gregapi.probe.GT6ProbeStand.fldSet(aGens[i], "mBurning", T);
			gregapi.probe.GT6ProbeStand.slotSet(aGens[i], 0, ST.make(Items.COAL, 32, 0));
			long tCap = gregapi.probe.GT6ProbeStand.fldLong(aBoilers[i], "mCapacity");
			gt6EnergyChainProbeBoilerFill(aBoilers[i], 1, "steam", tCap / 2 + 100000);
			gt6EnergyChainProbeBoilerFill(aBoilers[i], 0, "water", 4000);
		}
	}

	/** Тик 210: разжечь BASELINE и FARM (COLD НЕ трогается вовсе — контроль "ферма без предзаряда/горения"). */
	private static void gt6SteamFarmProbeLoad() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		gt6SteamFarmProbeLoadRow(new gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[]{sSTFBaseGen}, new gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[]{sSTFBaseBoiler});
		gt6SteamFarmProbeLoadRow(sSTFFarmGens, sSTFFarmBoilers);
		O.println("[" + STF_M + "] тик 210 загрузка: BASELINE(1 бойлер) и FARM(" + STF_N + " бойлеров) разожжены+предзаряжены (precharge=cap/2+100000 на КАЖДЫЙ бойлер); COLD НЕ трогается (контроль)");
	}

	/** Окно 211..900: живой пакет динамо (формула TE_Behavior_Energy_Converter.java:62, tOutput=units(mStorage.
	 *  mEnergy,mEnergyIN.mRec,mEnergyOUT.mRec,F) — читаем ТЕ ЖЕ публичные поля, что читает production-код, ничего
	 *  не подменяем) — копим МАКСИМУМ за окно (пакет колеблется тик-в-тик, урок §7 манифеста «однократный снимок
	 *  лжёт»), печать BASELINE и FARM РЯДОМ каждые 50 тиков (+первые тики после загрузки — локализация каскада
	 *  через шестерни). */
	private static void gt6SteamFarmProbeTrace() {
		if (sSTFBaseDynamo == null || sSTFFarmDynamo == null) return;
		long tBasePkt = UT.Code.units(sSTFBaseDynamo.mStorage.mEnergy, sSTFBaseDynamo.mEnergyIN.mRec, sSTFBaseDynamo.mEnergyOUT.mRec, F);
		long tFarmPkt = UT.Code.units(sSTFFarmDynamo.mStorage.mEnergy, sSTFFarmDynamo.mEnergyIN.mRec, sSTFFarmDynamo.mEnergyOUT.mRec, F);
		if (tBasePkt > sSTFBaseMaxPkt) sSTFBaseMaxPkt = tBasePkt;
		if (tFarmPkt > sSTFFarmMaxPkt) sSTFFarmMaxPkt = tFarmPkt;
		if (sSTFProbeTick % 50 == 0 || sSTFProbeTick <= 216) {
			java.io.PrintStream O = gregapi.data.CS.OUT;
			long tG0Power = sSTFFarmGears[0].mCurrentPower, tG0Speed = sSTFFarmGears[0].mCurrentSpeed, tG0Last = sSTFFarmGears[0].mTransferredLast;
			O.println("[" + STF_M + "] DIAG-TRACE тик " + sSTFProbeTick + ": BASELINE турбина.mSteamCounter=" + sSTFBaseTurbine.mSteamCounter + " динамо.mStorage=" + sSTFBaseDynamo.mStorage.mEnergy + " pkt=" + tBasePkt + " батарея.mEnergy=" + sSTFBaseBattery.mEnergy
				+ " || FARM(N=" + STF_N + ") турбина[0].mSteamCounter=" + sSTFFarmTurbines[0].mSteamCounter + " шестерня[0].(power/speed/last)=" + tG0Power + "/" + tG0Speed + "/" + tG0Last + " динамо.mStorage=" + sSTFFarmDynamo.mStorage.mEnergy + " pkt=" + tFarmPkt + " батарея.mEnergy=" + sSTFFarmBattery.mEnergy);
			// [GT6-STEAMFARMPROBE] DIAG §6.1 — гипотеза "заклинивание по рассинхрону направления вращения": при
			// ОДНОВРЕМЕННОМ приёме на РАЗНЫХ осях (D от своей турбины + E от цепочки) getRotations() может дать
			// НЕСОВПАДАЮЩИЙ mRotationData -> MultiTileEntityGearBox.java:381-389 "Gears are jamming!" mJammed=true,
			// после чего isEnergyAcceptingFrom (:413) НАВСЕГДА возвращает false для ВСЕХ шестерён цепи. Снять при уборке фазы.
			O.println("[" + STF_M + "] DIAG-JAM тик " + sSTFProbeTick + ": шестерня[0].mJammed=" + sSTFFarmGears[0].mJammed + " mRotationData=" + sSTFFarmGears[0].mRotationData + " mInputtedSides=" + sSTFFarmGears[0].mInputtedSides
				+ " || шестерня[1].mJammed=" + sSTFFarmGears[1].mJammed + " mRotationData=" + sSTFFarmGears[1].mRotationData
				+ " || шестерня[2].mJammed=" + sSTFFarmGears[2].mJammed + " mRotationData=" + sSTFFarmGears[2].mRotationData);
			// [GT6-STEAMFARMPROBE] DIAG §6.1 — локализация хопа турбина->динамо (BASELINE, прямой стек, идентичный
			// ENERGYCHAINPROBE): турбина.mStorage(RU накоплено) / mConverter.mCanEmitEnergy,mEmitsEnergy (реальные
			// публичные поля, читаем БЕЗ вызова судимого метода) + isEnergyEmittingTo/isEnergyAcceptingFrom на РЕАЛЬНЫХ
			// сторонах (UP от турбины, DOWN у динамо) — не симуляция значения, просто чтение состояния. Снять при уборке фазы.
			O.println("[" + STF_M + "] DIAG-HOP тик " + sSTFProbeTick + ": BASELINE турбина.mStorage=" + sSTFBaseTurbine.mStorage.mEnergy + " mCanEmitEnergy=" + sSTFBaseTurbine.mConverter.mCanEmitEnergy + " mEmitsEnergy=" + sSTFBaseTurbine.mConverter.mEmitsEnergy
				+ " турбина.isEnergyEmittingTo(RU,UP,F)=" + sSTFBaseTurbine.isEnergyEmittingTo(TD.Energy.RU, SIDE_UP, F) + " динамо.isEnergyAcceptingFrom(RU,DOWN,F)=" + sSTFBaseDynamo.isEnergyAcceptingFrom(TD.Energy.RU, SIDE_DOWN, F)
				+ " турбина.mFacing=" + sSTFBaseTurbine.mFacing + " динамо.mFacing=" + sSTFBaseDynamo.mFacing + " турбина.getBlockPos()=" + sSTFBaseTurbine.getBlockPos() + " динамо.getBlockPos()=" + sSTFBaseDynamo.getBlockPos());
		}
	}

	// ================================================================================================================
	// [GT6-STEAMFARMPROBE] BUG-062 ЖИВОЙ СУДЬЯ v1 (восстановленная топология по заданию — прежде ОПРОВЕРГНУТА
	// живым прогоном, см. комментарий блока "ЭТАП А v1 [ОПРОВЕРГНУТО]" выше ~:4143-4164): манифольд бойлер ->
	// STF_PIPE_L труб (Cu, PIPE_NORM_ID=26102, переиспользован из FLUIDPIPEPROBE, GT_API_Proxy.java:2350) -> турбина.
	// Прежний прогон: турбина.mTank=0 все 900 тиков — корень MultiTileEntityPipeFluid.distribute() кандидат-чек
	// (:411, сейчас уже сторононесущий) звал приёмник СAЙДЛЕС-вызовом (BUG-062), а турбина отвергает пакет без
	// точной стороны (isInput(aSide)=aSide==OPOS[mFacing], MultiTileEntityTurbineSteam.java:129). Фикс 39c668c8
	// вернул сторону через FL.fill (fillSided) — этот кейс ЖИВЬЁМ доказывает/опровергает результат фикса числами.
	// ЕДИНСТВЕННЫЙ судья (объявлен ДО прогона, порог не двигать): турбина.mTank.amount()>0 хотя бы раз за окно
	// 211..900 (Seq.watch — транзиент короток: doConversion:88-108 опустошает mTank ЦЕЛИКОМ в ТОТ ЖЕ тик, когда
	// накопленное превышает getEnergySizeInputMin()*2, однократный замер в конце лжёт, манифест §7). Труба[последняя]
	// .mTanks[0] наполнялась — печатается для протокола (доказывает, что бойлер сварил пар И труба его понесла),
	// НЕ судит вердикт. Турбина смотрит входом ВНИЗ (mFacing=UP -> OPOS[UP]=DOWN, тот же приём, что BASELINE/FARM
	// прямой стек выше) — манифольд физически СВЕРХУ бойлера (боилер эмитит СТРОГО SIDE_UP, MultiTileEntityBoilerTank.
	// java:143, не зависит от mFacing) и СНИЗУ турбины. Свежая зона Z=70 (макс. Z был 60 у COLD). Снять при уборке фазы.
	// ================================================================================================================
	private static final int STF_PIPE_L = 3; // число сегментов трубы в манифольде (задание: "труба", не 1 блок)
	private static gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick sSTFPipeV1Gen;
	private static gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank sSTFPipeV1Boiler;
	private static final gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] sSTFPipeV1Pipes = new gregapi.tileentity.connectors.MultiTileEntityPipeFluid[STF_PIPE_L];
	private static gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam sSTFPipeV1Turbine;
	private static long sSTFPipeV1MaxTurbineTank = 0, sSTFPipeV1MaxPipeTank = 0;
	private static int sSTFPipeV1FirstNonZeroTick = -1;

	/** Тик 200: построить PIPEV1 — бойлер -> STF_PIPE_L труб Cu -> турбина (см. комментарий блока выше). Тот же
	 *  приём постройки колонны, что gt6SteamFarmProbeBuildColumn (генератор перпендикулярно оси, расчистка фронта
	 *  горелки), но турбина НЕ на бойлере напрямую — манифольд {@link gregapi.probe.GT6ProbeStand#line} между ними. */
	private static void gt6SteamFarmProbeBuildPipeV1() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = sSTFPlayer.level();
		net.minecraft.core.BlockPos tBase = sSTFPlayer.blockPosition().offset(4, 0, 70);
		gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick tGen = gregapi.probe.GT6ProbeStand.place(
			tLevel, sSTFPlayer, tBase, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_GEN_ID),
			gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick.class, STF_M, "PIPEV1-генератор");
		if (tGen == null) throw new RuntimeException("PIPEV1: генератор не встал");
		tGen.setPrimaryFacing(SIDE_SOUTH); // перпендикулярно оси ряда — тот же приём, что gt6SteamFarmProbeBuildColumn
		net.minecraft.core.BlockPos tGenPos = tBase.above();
		tLevel.setBlock(tGenPos.relative(FORGE_DIR[tGen.mFacing]), Blocks.AIR.defaultBlockState(), 3);
		gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank tBoiler = gregapi.probe.GT6ProbeStand.place(
			tLevel, sSTFPlayer, tGenPos, net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_BOILER_ID),
			gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank.class, STF_M, "PIPEV1-бойлер");
		if (tBoiler == null) throw new RuntimeException("PIPEV1: бойлер не встал");
		net.minecraft.core.BlockPos tBoilerPos = tGenPos.above();
		gregapi.tileentity.connectors.MultiTileEntityPipeFluid[] tPipes = gregapi.probe.GT6ProbeStand.line(
			tLevel, sSTFPlayer, tBoilerPos, net.minecraft.core.Direction.UP, STF_PIPE_L, PIPE_NORM_ID,
			gregapi.tileentity.connectors.MultiTileEntityPipeFluid.class, STF_M);
		for (int i = 0; i < STF_PIPE_L; i++) {
			if (tPipes[i] == null) throw new RuntimeException("PIPEV1: труба[" + i + "] не встала");
			sSTFPipeV1Pipes[i] = tPipes[i];
		}
		gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam tTurbine = gregapi.probe.GT6ProbeStand.place(
			tLevel, sSTFPlayer, sSTFPipeV1Pipes[STF_PIPE_L-1].getBlockPos(), net.minecraft.core.Direction.UP, gregapi.probe.GT6ProbeStand.mteStack(STF_TURBINE_ID),
			gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam.class, STF_M, "PIPEV1-турбина");
		if (tTurbine == null) throw new RuntimeException("PIPEV1: турбина не встала");
		tTurbine.setPrimaryFacing(SIDE_UP); // вход СНИЗУ (OPOS[UP]=DOWN, MultiTileEntityTurbineSteam.java:129) — со стороны манифольда
		// явное восстановление концевых связей (тот же приём FLUIDPIPEPROBE :2416-2417): нижний конец обычно уже
		// авто-связан onPlaced() (боилер существовал на момент клика трубы[0]), верхний — НЕТ (турбина появилась
		// ПОСЛЕ трубы[последняя], а турбина не ITileEntityConnector — её onPlaced на связность трубы не влияет).
		sSTFPipeV1Pipes[0].connect(SIDE_DOWN, T);
		sSTFPipeV1Pipes[STF_PIPE_L-1].connect(SIDE_UP, T);
		sSTFPipeV1Gen = tGen; sSTFPipeV1Boiler = tBoiler; sSTFPipeV1Turbine = tTurbine;
		O.println("[" + STF_M + "] PIPEV1 построен (BUG-062 живой судья): бойлер@" + tBoilerPos + " труб=" + STF_PIPE_L + "(Cu id=" + PIPE_NORM_ID + ") турбина@" + tTurbine.getBlockPos()
			+ " connected(труба[0].DOWN)=" + sSTFPipeV1Pipes[0].connected(SIDE_DOWN) + " connected(труба[посл].UP)=" + sSTFPipeV1Pipes[STF_PIPE_L-1].connected(SIDE_UP));
	}

	/** Тик 210: разжечь+предзарядить PIPEV1 (переиспользован gt6SteamFarmProbeLoadRow — тот же центр, что BASELINE/FARM). */
	private static void gt6SteamFarmProbePipeV1Load() {
		gt6SteamFarmProbeLoadRow(new gregtech.tileentity.energy.generators.MultiTileEntityGeneratorBrick[]{sSTFPipeV1Gen}, new gregtech.tileentity.energy.converters.MultiTileEntityBoilerTank[]{sSTFPipeV1Boiler});
		gregapi.data.CS.OUT.println("[" + STF_M + "] PIPEV1 тик 210 загрузка: генератор разожжён+бойлер предзаряжен (precharge=cap/2+100000)");
	}

	/** Окно 211..900: живые числа PIPEV1 каждые 50 тиков (+первые тики, +каждые 10 тиков пока турбина.mTank>0 —
	 *  транзиент короток, doConversion:88-108 опустошает mTank в тот же тик выше порога) — max-накопление обоих
	 *  танков (урок §7 манифеста «однократный замер лжёт», тот же приём sSTFBaseMaxPkt/sSTFFarmMaxPkt выше). */
	private static void gt6SteamFarmProbePipeV1Trace() {
		if (sSTFPipeV1Turbine == null) return;
		long tTurbineTank = sSTFPipeV1Turbine.mTank.amount();
		long tPipeTank = sSTFPipeV1Pipes[STF_PIPE_L-1].mTanks[0].amount();
		if (tTurbineTank > sSTFPipeV1MaxTurbineTank) sSTFPipeV1MaxTurbineTank = tTurbineTank;
		if (tPipeTank > sSTFPipeV1MaxPipeTank) sSTFPipeV1MaxPipeTank = tPipeTank;
		if (tTurbineTank > 0 && sSTFPipeV1FirstNonZeroTick < 0) sSTFPipeV1FirstNonZeroTick = sSTFProbeTick;
		if (sSTFProbeTick % 50 == 0 || sSTFProbeTick <= 216 || (tTurbineTank > 0 && sSTFProbeTick % 10 == 0)) {
			gregapi.data.CS.OUT.println("[" + STF_M + "] DIAG-PIPEV1 тик " + sSTFProbeTick + ": труба[посл].mTanks[0]=" + tPipeTank + " турбина.mTank=" + tTurbineTank + " турбина.mSteamCounter=" + sSTFPipeV1Turbine.mSteamCounter);
		}
	}

	/** Тик 900: BASELINE (недонапряжение подтверждено) vs FARM (масштабирование числом через шестерни) vs COLD
	 *  (нули) + DONE. */
	private static void gt6SteamFarmProbeJudgeFinal() {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		long tBaseMin = sSTFBaseBattery.getEnergySizeInputMin(TD.Energy.EU, SIDE_DOWN);
		long tFarmMin = sSTFFarmBattery.getEnergySizeInputMin(TD.Energy.EU, SIDE_DOWN);

		O.println("[" + STF_M + "] ===== BASELINE-1 (1 бойлер, прямой стек, тик " + sSTFProbeTick + ") =====");
		O.println("[" + STF_M + "] BASELINE: динамо.mStorage=" + sSTFBaseDynamo.mStorage.mEnergy + " pkt_max_за_окно=" + sSTFBaseMaxPkt + " (LV.mInput=" + sSTFBaseBattery.mInput + " min=" + tBaseMin + ") батарея.mEnergy=" + sSTFBaseBattery.mEnergy + " турбина.mSteamCounter=" + sSTFBaseTurbine.mSteamCounter);
		sSTFSeq.judge("BASELINE: пакет динамо < min LV-батареи (недонапряжение по семантике связки №4)", sSTFBaseMaxPkt < tBaseMin, "<" + tBaseMin, sSTFBaseMaxPkt);
		sSTFSeq.judge("BASELINE: LV-батарея ПУСТА (пакет отвергнут целиком, Root.doEnergyInjection:886)", sSTFBaseBattery.mEnergy == 0, 0, sSTFBaseBattery.mEnergy);

		O.println("[" + STF_M + "] ===== FARM-N (N=" + STF_N + " бойлеров, шестерни агрегируют RU, тик " + sSTFProbeTick + ") =====");
		O.println("[" + STF_M + "] FARM: динамо.mStorage=" + sSTFFarmDynamo.mStorage.mEnergy + " (cap=" + sSTFFarmDynamo.mStorage.mCapacity + ") pkt_max_за_окно=" + sSTFFarmMaxPkt + " (LV min=" + tFarmMin + ") батарея.mEnergy=" + sSTFFarmBattery.mEnergy);
		boolean tEverGrew = sSTFSeq.everSeen("farm-eu-grew");
		sSTFSeq.judge("FARM: пакет динамо ВЫРОС против BASELINE (" + sSTFFarmMaxPkt + " vs " + sSTFBaseMaxPkt + ")", sSTFFarmMaxPkt > sSTFBaseMaxPkt, ">" + sSTFBaseMaxPkt, sSTFFarmMaxPkt);
		sSTFSeq.judge("FARM: пакет динамо перевалил за min LV", sSTFFarmMaxPkt >= tFarmMin, ">=" + tFarmMin, sSTFFarmMaxPkt);
		sSTFSeq.judge("FARM: LV-батарея НАЧАЛА принимать (mEnergy>0 хотя бы раз за окно, Seq.watch — урок §7 манифеста)", tEverGrew, T, tEverGrew);
		sSTFSeq.judge("FARM: LV-батарея.mEnergy сейчас > 0", sSTFFarmBattery.mEnergy > 0, ">0", sSTFFarmBattery.mEnergy);
		// ДИАГНОСТИЧЕСКИЙ судья (§6.1, НЕ маскирует провал агрегации выше — фиксирует ДИАГНОЗ КОРНЯ машиной, не
		// комментарием): 3-гранные шестерни (агрегатор[0], средняя[1]) заклинивают при первом же одновременном
		// приёме с разных осей (getRotations() рассинхрон, MultiTileEntityGearBox.java:381-389) — печать
		// ПОДТВЕРЖДАЕТ ИЛИ ОПРОВЕРГАЕТ гипотезу живыми полями, не мнением.
		O.println("[" + STF_M + "] ДИАГНОЗ КОРНЯ (§6.1): шестерня[0].mJammed=" + sSTFFarmGears[0].mJammed + " шестерня[1].mJammed=" + sSTFFarmGears[1].mJammed + " шестерня[2](чистый угол,2 грани).mJammed=" + sSTFFarmGears[2].mJammed);
		sSTFSeq.judge("FARM: ДИАГНОЗ подтверждён — 3-гранные шестерни[0]/[1] заклинили (getRotations-рассинхрон), угловая шестерня[2] цела", sSTFFarmGears[0].mJammed && sSTFFarmGears[1].mJammed && !sSTFFarmGears[2].mJammed, "true,true,false", sSTFFarmGears[0].mJammed + "," + sSTFFarmGears[1].mJammed + "," + sSTFFarmGears[2].mJammed);

		long tBaseSteam = sSTFBaseTurbine.mSteamCounter;
		long tFarmSteamSum = 0; for (gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam t : sSTFFarmTurbines) tFarmSteamSum += t.mSteamCounter;
		double tRatio = tBaseSteam > 0 ? (double) tFarmSteamSum / tBaseSteam : -1;
		O.println("[" + STF_M + "] СРАВНЕНИЕ пара: BASELINE.mSteamCounter=" + tBaseSteam + " FARM.СУММА(N турбин).mSteamCounter=" + tFarmSteamSum + " отношение=" + tRatio + " (ожидание ~N=" + STF_N + "; тот же счётчик, что MultiTileEntityTurbineSteam.java:94, живёт ПО МОДУЛЮ STEAM_PER_WATER=200 :98 — справочно, не точный расход, урок №2 манифеста)");
		sSTFSeq.judge("FARM: суммарный пар (по всем N турбинам) обработан кратно числу бойлеров (±50%, тот же допуск, что ENERGYCHAINPROBE 4b)", tRatio >= STF_N * 0.5 && tRatio <= STF_N * 1.5, "[" + (STF_N * 0.5) + ".." + (STF_N * 1.5) + "]", tRatio);

		// Потолок ИЗ СЕТАПА (не из модуло-счётчика, урок №2 манифеста) — тот же приём ENERGYCHAINPROBE 4b, ×N бойлеров
		long tCapPerBoiler = gregapi.probe.GT6ProbeStand.fldLong(sSTFFarmBoilers[0], "mCapacity");
		long tSteamAvailTotal = (tCapPerBoiler / 2 + 100000 + 4000 * 160) * STF_N; // (precharge + вода×160 макс.калcификация :120-123) × N боилеров
		long tRuMax = tSteamAvailTotal / STEAM_PER_EU; // MultiTileEntityTurbineSteam.java:95
		long tDynIn = sSTFFarmDynamo.mEnergyIN.mRec, tDynOut = sSTFFarmDynamo.mEnergyOUT.mRec;
		long tEuCeil = tRuMax * tDynOut / tDynIn; // TE_Behavior_Energy_Converter.java:62 — верхняя граница КПД динамо
		long tEuDelta = sSTFFarmBattery.mEnergy - sSTFFarmEu0;
		O.println("[" + STF_M + "] FARM теоретический потолок (из сетапа ×" + STF_N + "): пар_доступный=" + tSteamAvailTotal + " RU_max=" + tRuMax + " EU_потолок=" + tEuCeil + " EU_прирост_факт=" + tEuDelta);
		sSTFSeq.judge("FARM: EU-прирост <= теоретический потолок цепи (не создаёт энергию из ничего)", tEuDelta <= tEuCeil, "<=" + tEuCeil, tEuDelta);

		O.println("[" + STF_M + "] ===== COLD (N=" + STF_N + " бойлеров, никогда не зажжена/не заряжена, тик " + sSTFProbeTick + ") =====");
		boolean tColdBurning = gregapi.probe.GT6ProbeStand.fldBool(sSTFColdGens[0], "mBurning");
		long tColdSteamSum = 0; for (gregtech.tileentity.energy.converters.MultiTileEntityTurbineSteam t : sSTFColdTurbines) tColdSteamSum += t.mSteamCounter;
		O.println("[" + STF_M + "] COLD: генератор[0].mBurning=" + tColdBurning + " СУММА(N турбин).mSteamCounter=" + tColdSteamSum + " динамо.mStorage=" + sSTFColdDynamo.mStorage.mEnergy + " батарея.mEnergy=" + sSTFColdBattery.mEnergy);
		sSTFSeq.judge("COLD: генератор НЕ горит", !tColdBurning, F, tColdBurning);
		sSTFSeq.judge("COLD: турбины не обработали пар", tColdSteamSum == 0, 0, tColdSteamSum);
		sSTFSeq.judge("COLD: динамо.mStorage пуст", sSTFColdDynamo.mStorage.mEnergy == 0, 0, sSTFColdDynamo.mStorage.mEnergy);
		sSTFSeq.judge("COLD: батарея пуста", sSTFColdBattery.mEnergy == 0, 0, sSTFColdBattery.mEnergy);

		O.println("[" + STF_M + "] ===== PIPEV1 (BUG-062 живой судья — восстановленная топология v1: бойлер->" + STF_PIPE_L + " труб(Cu)->турбина, тик " + sSTFProbeTick + ") =====");
		boolean tPipeV1TurbineFilled = sSTFSeq.everSeen("pipev1-turbine-filled");
		boolean tPipeV1PipeFilled = sSTFSeq.everSeen("pipev1-pipe-filled");
		O.println("[" + STF_M + "] PIPEV1: турбина.mTank max_за_окно=" + sSTFPipeV1MaxTurbineTank + " (видели>0 хотя бы раз=" + tPipeV1TurbineFilled + ", первый ненулевой тик=" + sSTFPipeV1FirstNonZeroTick + ") труба[последняя].mTanks[0] max_за_окно=" + sSTFPipeV1MaxPipeTank + " (видели>0=" + tPipeV1PipeFilled + ") турбина.mSteamCounter=" + sSTFPipeV1Turbine.mSteamCounter);
		sSTFSeq.judge("PIPEV1 (BUG-062 ЕДИНСТВЕННЫЙ судья): турбина.mTank>0 хотя бы раз за окно 211..900 — пар дошёл трубой до турбины", tPipeV1TurbineFilled, T, tPipeV1TurbineFilled);
		O.println("[" + STF_M + "] PIPEV1 протокол (справочно, НЕ судит вердикт): труба[последняя] наполнялась=" + tPipeV1PipeFilled + " (доказывает, что бойлер сварил пар И труба его понесла, а не только 'бойлер не сварил')");

		sSTFSeq.done();
	}

	public static void gt6SteamFarmProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sSTFProbeTick++;
		if (aServer.getPlayerList().getPlayers().isEmpty()) return;
		sSTFPlayer = aServer.getPlayerList().getPlayers().get(0);
		if (sSTFSeq == null) {
			sSTFSeq = new gregapi.probe.GT6ProbeStand.Seq(STF_M)
				.at(200, GT_API_Proxy::gt6SteamFarmProbeBuild)
				.at(200, GT_API_Proxy::gt6SteamFarmProbeBuildPipeV1) // BUG-062 живой судья v1 (см. комментарий блока)
				.at(210, () -> {gt6SteamFarmProbeLoad(); sSTFFarmEu0 = sSTFFarmBattery.mEnergy;})
				.at(210, GT_API_Proxy::gt6SteamFarmProbePipeV1Load)
				.window(211, 900, GT_API_Proxy::gt6SteamFarmProbeTrace)
				.window(211, 900, GT_API_Proxy::gt6SteamFarmProbePipeV1Trace)
				.watch("farm-eu-grew", 210, 900, () -> sSTFFarmBattery != null && sSTFFarmBattery.mEnergy > 0)
				.watch("pipev1-turbine-filled", 211, 900, () -> sSTFPipeV1Turbine != null && sSTFPipeV1Turbine.mTank.amount() > 0)
				.watch("pipev1-pipe-filled", 211, 900, () -> sSTFPipeV1Pipes[STF_PIPE_L-1] != null && sSTFPipeV1Pipes[STF_PIPE_L-1].mTanks[0].amount() > 0)
				.at(900, GT_API_Proxy::gt6SteamFarmProbeJudgeFinal);
		}
		sSTFSeq.tick(sSTFProbeTick);
	}

}
