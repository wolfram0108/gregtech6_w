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
	 * ВТОРОЙ НОСИТЕЛЬ ПЕРЕВОДА (MODCOMPAT-014) — сторона решает, есть ли он вообще.
	 *
	 * <p>На клиенте таблиц переводов ДВЕ: глобальная {@code Language.getInstance()}, куда центр локализации
	 * ставит надстройку, и собственный указатель {@code I18n} ({@code I18n.java:11}), который движок ставит
	 * только в {@code LanguageManager.apply:66-68}. Через второй спрашивают сторонние моды, поэтому после
	 * КАЖДОГО долива их надо сводить к одной надстройке — иначе имена GT6 видит лишь первый.
	 * На выделенном сервере второго носителя не существует: здесь пусто, и клиентский тип сюда не тянется
	 * (класс дефекта BUG-092 — клиентский тип в общем классе убивал дедикейт).
	 */
	public void syncClientI18n() {/* сервер: второго носителя перевода нет */}

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

	/** Мост оплаты маяка — возрождение Forge-хука 1.7.10 {@code Item.isBeaconPayment(ItemStack)} (Forge Item.java:1482).
	 *  В 1.7.10 слот маяка спрашивал сам предмет (TileEntityBeacon.isItemValidForSlot:409); в neo оплата — тег
	 *  {@code ItemTags.BEACON_PAYMENT_ITEMS} на Item (BeaconMenu:33,165), материал в данных стека тегу не виден.
	 *  Центральный предикат: ванильный тег ИЛИ пер-стековый ответ носителя контракта {@link IItemBeaconPayment}
	 *  (сейчас — PrefixItem, тело 1:1 с оригиналом). Тег GT6-предметами НЕ заполняется — иначе маяк принимал бы
	 *  и неценные материалы, шире оригинала (решение пользователя 2026-07-30). */
	public static boolean isBeaconPayment(ItemStack aStack) {
		return aStack.is(net.minecraft.tags.ItemTags.BEACON_PAYMENT_ITEMS) || (aStack.getItem() instanceof IItemBeaconPayment tItem && tItem.isBeaconPayment(aStack));
	}

	/** Плечо моста: подмена слота 0 ванильного {@code BeaconMenu} на слот с центральным предикатом. Все пути
	 *  клика идут через {@code slots.get(index).mayPlace} (AbstractContainerMenu:356,382,452,485,494,693), поле
	 *  {@code BeaconMenu.paymentSlot} продолжает работать (тот же Container, возврат предмета в {@code removed()}
	 *  не задет). Клиентское плечо — {@code GT_API_Proxy_Client.onScreenOpening} (клиент строит СВОЙ экземпляр
	 *  меню по сети, серверная подмена его не достигает). */
	public static void wrapBeaconPaymentSlot(net.minecraft.world.inventory.AbstractContainerMenu aMenu) {
		if (!(aMenu instanceof net.minecraft.world.inventory.BeaconMenu)) return;
		net.minecraft.world.inventory.Slot tOld = aMenu.slots.get(0);
		net.minecraft.world.inventory.Slot tNew = new net.minecraft.world.inventory.Slot(tOld.container, tOld.getContainerSlot(), tOld.x, tOld.y) {
			@Override public boolean mayPlace(ItemStack aStack) {return isBeaconPayment(aStack);}
			@Override public int getMaxStackSize() {return 1;} // как у PaymentSlot (BeaconMenu:170)
		};
		tNew.index = tOld.index;
		aMenu.slots.set(0, tNew);
	}

	/** Серверное плечо: {@code PlayerContainerEvent.Open} летит после сборки меню (ServerPlayer.java:1458). */
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
		// Сторож паспорта ролей жидкостей (BUG-120): роль «своя жидкость в теге среды» обещана данными
		// (tags/fluid/water.json) — рассинхрон кода и файла обязан кричать в лог, а не молча убивать плавание.
		gregapi.block.fluid.BlockFluidBaseGT.validateEngineRoles();
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

	/**
	 * BUG-056: открыть игроку экран «все рецепты этой машины». В 1.7.10 это делал сам мод NEI
	 * ({@code GuiCraftingRecipe.openRecipeGui(mNameNEI)}, вызывалось из {@code RecipeMap.openNEI}); в 26.1.2
	 * его роль занял JEI, и открытие экрана — сугубо КЛИЕНТСКОЕ действие. Общий код (RecipeMap) не должен
	 * видеть client-only классы JEI, поэтому вызов идёт через прокси — тем же приёмом, что
	 * {@link #registerClientModels}. Сервер: no-op, как и раньше возвращаем false.
	 */
	public boolean openRecipeGui(String aNameNEI) {return false;}
	
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
				gt6ChunkFinishTick();
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
					} else if (!WD.blockTicking(tTileEntity)) {
						// №2в (2026-08-09): чанк не тикает блоками (пограничный/выгружается) — техника замирает
						// вместе с миром, из списка НЕ удаляется (оттает при повышении уровня чанка).
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
						// №2в: см. SERVER_TICK_PRE выше.
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
						// №2в: сущность мертва — апдейт потерял хозяина, выбрасываем; чанк заморожен — переносим
						// в активную очередь до оттаивания (в замороженном мире соседям нечего пересчитывать).
						if (tTileEntity instanceof ITileEntityUnloadable && ((ITileEntityUnloadable)tTileEntity).isDead()) continue;
						if (!WD.blockTicking(tTileEntity)) {DELAYED_BLOCK_UPDATES.add(tTileEntity); continue;}
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
							// №2в: было isLoaded — пограничный чанк проходил гейт, и распад листвы шёл там, где движок
							// случайные тики уже выключил. Теперь тот же закон, что у движка: не тикает — переносим на
							// +8 тиков БЕЗ сжигания попытки (чанк загружен, но заморожен); чанк выгружен — бросаем,
							// как бросал прежний isLoaded-гейт.
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
							// №2в: чанк заморожен — перенос в активную очередь до оттаивания.
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
			
			if (aEvent instanceof ServerTickEvent.Post) { // было aEvent.phase == ServerTickEvent.END
				for (int i = 0; i < SERVER_TICK_POST.size(); i++) {
					ITileEntityServerTickPost tTileEntity = SERVER_TICK_POST.get(i);
					if (tTileEntity.isDead()) {
						SERVER_TICK_POST.remove(i--);
						tTileEntity.onUnregisterPost();
					} else if (!WD.blockTicking(tTileEntity)) {
						// №2в: см. SERVER_TICK_PRE выше.
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
						// №2в: см. SERVER_TICK_PRE выше.
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
				if (++sFlightTick % 600 == 0) flightSample(aEvent.getServer()); // самописец: срез раз в 30 секунд
				
				if (SERVER_TIME % 1200 == 0) checkSaveLocation(aEvent.getServer().getWorldPath(LevelResource.ROOT).toFile(), T);
				
				if (TICK_LOCK.isHeldByCurrentThread()) TICK_LOCK.unlock();
			}
		}
	}

	// [BUG-047] F-hook-removed → центральный мост: 1.7.10 Forge-хуки BlockRailBase.onMinecartPass/getRailMaxSpeed
	// ВЫРЕЗАНЫ из NeoForge 26.1.2 (extensions-каталог: только IBaseRailBlockExtension — isFlexibleRail/canMakeSlopes/
	// getRailDirection/isValidRailShape; буст движок читает ТОЛЬКО с instanceof PoweredRailBlock —
	// OldMinecartBehavior:115-116). Мост: EntityTickEvent.Post = раз в тик на сущность ПОСЛЕ движения — та же фаза,
	// что 1.7.10 хвост EntityMinecart.func_145821_a (вызывал onMinecartPass после moveAlongTrack); позиция рельса —
	// getCurrentBlockPosOrRailBelow (канал самого движка). Кламп СКОРОСТИ здесь — только ЗАПАСНОЕ плечо (вниз):
	// основной канал — GT6MinecartBehavior (см. onMinecartJoinBridge ниже), который отвечает движку per-rail
	// величиной ДО движения; у подменённых минкартов пост-кламп не дублируется.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onMinecartPassBridge(EntityTickEvent.Post aEvent) {
		if (!(aEvent.getEntity() instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart tCart) || tCart.level().isClientSide()) return;
		BlockPos tRailPos = tCart.getCurrentBlockPosOrRailBelow();
		if (!(WD.block(tCart.level(), tRailPos.getX(), tRailPos.getY(), tRailPos.getZ()) instanceof gregapi.block.misc.BlockBaseRail tRail)) return;
		tRail.onMinecartPass(tCart.level(), tCart, tRailPos.getX(), tRailPos.getY(), tRailPos.getZ());
		if (minecartBehavior(tCart) instanceof gregapi.block.misc.BlockBaseRail.GT6MinecartBehavior) return; // движок уже клампит per-rail
		float tMax = tRail.getRailMaxSpeed(tCart.level(), tCart, tRailPos.getX(), tRailPos.getY(), tRailPos.getZ());
		net.minecraft.world.phys.Vec3 tCartMotion = tCart.getDeltaMovement();
		if (Math.abs(tCartMotion.x) > tMax || Math.abs(tCartMotion.z) > tMax)
			tCart.setDeltaMovement(net.minecraft.util.Mth.clamp(tCartMotion.x, -tMax, tMax), tCartMotion.y, net.minecraft.util.Mth.clamp(tCartMotion.z, -tMax, tMax));
	}

	// [BUG-047, метка отложенности F-hook-removed СНЯТА 2026-08-06] Скорости рельсов ВЫШЕ движковых 0.4 (Ti 1.2 и далее):
	// кламп смещения захардкожен ВНУТРИ OldMinecartBehavior.moveAlongTrack:208-211 через getMaxSpeed:410-411 —
	// пост-событием не поднимается. Единственная точка per-cart — поле AbstractMinecart.behavior (private final,
	// AbstractMinecart:62, назначается конструктором); события/расширения на выбор поведения в 26.1.2 нет.
	// Подмена рефлексией на входе минкарта в мир — приём прецедентен (IItemProjectile → AbstractArrow.baseDamage,
	// единственное место чтения на весь мод); подкласс — BlockBaseRail.GT6MinecartBehavior (1:1-формула
	// min(rail, капа-минкарта-1.2) из EntityMinecart:373-374, там же цитаты). Обе стороны: клиентское плечо кроет
	// getKnownMovement (производные системы). Experimental-физика (NewMinecartBehavior) НЕ подменяется: её модель
	// скоростей — своя (канона 1.7.10 у неё нет), там остаётся запасной пост-кламп из onMinecartPassBridge.
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

	// BUG-090: поведение GT6-зельев-эффектов, жившее в 1.7.10 в обработчиках Immersive Engineering
	// (EventHandler.java:387-408, декомпил-референс ImmersiveEngineering-1.7.10/ в дереве проекта) — сами
	// эффекты теперь регистрирует GT6 (gregapi/potion/MobEffectsGT, «функция, не авторство»), обработчики
	// продублированы 1:1 в этом же едином центре подписки. LivingHurtEvent (1.7.10) в neo не существует —
	// модифицируемая величина урона до брони = LivingIncomingDamageEvent.setAmount (сверено,
	// neoforge-decompiled/.../LivingIncomingDamageEvent.java); приоритет LOWEST — как у IE-оригинала.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLivingHurtPotionsGT(LivingIncomingDamageEvent aEvent) {
		MobEffectInstance tEffect;
		// 1:1 IE EventHandler.java:390-395: урон огнём × (1.5 + amp²·0.5) при эффекте flammable.
		if (aEvent.getSource().is(DamageTypeTags.IS_FIRE) && (tEffect = aEvent.getEntity().getEffect(gregapi.potion.MobEffectsGT.FLAMMABLE)) != null) {
			int tAmp = tEffect.getAmplifier();
			aEvent.setAmount(aEvent.getAmount() * (1.5F + tAmp*tAmp*0.5F));
		}
		// 1:1 IE EventHandler.java:396-401: урон типа "flux" (IE-электричество) × тот же множитель при
		// conductive. В сборке без IE-машин источника "flux"-урона нет — как и в 1.7.10 (см. MobEffectsGT).
		if ("flux".equals(aEvent.getSource().getMsgId()) && (tEffect = aEvent.getEntity().getEffect(gregapi.potion.MobEffectsGT.CONDUCTIVE)) != null) {
			int tAmp = tEffect.getAmplifier();
			aEvent.setAmount(aEvent.getAmount() * (1.5F + tAmp*tAmp*0.5F));
		}
	}

	// 1:1 IE EventHandler.java:403-408: sticky ослабляет прыжок — motionY -= (amp+1)·0.3.
	@SubscribeEvent
	public void onLivingJumpPotionsGT(LivingEvent.LivingJumpEvent aEvent) {
		MobEffectInstance tEffect = aEvent.getEntity().getEffect(gregapi.potion.MobEffectsGT.STICKY);
		if (tEffect != null) {
			net.minecraft.world.phys.Vec3 tMotion = aEvent.getEntity().getDeltaMovement();
			aEvent.getEntity().setDeltaMovement(tMotion.x, tMotion.y - (tEffect.getAmplifier()+1)*0.3F, tMotion.z);
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
			// BUG-103 (класс «состав сущностей меняется во время обхода»): удалять ПРЯМО В ЦИКЛЕ нельзя. По коду
			// движка discard() → Callback.onRemove → stopTracking → onTrackingEnd → ChunkMap.removeEntity (правит
			// entityMap) И visibleEntityStorage.remove — то есть структурно меняет и карту трекеров, и ТУ САМУЮ
			// коллекцию, которую перебирает getAll() (EntityLookup.byId, Int2ObjectLinkedOpenHashMap; getAllEntities
			// отдаёт её живую обёртку). В 1.7.10 setDead() только ставил флаг, и обход был безопасен. Копим и
			// удаляем ПОСЛЕ цикла — наблюдаемое поведение то же, движковые карты не трогаются во время обхода.
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
							((ItemEntity)aEntity).setItem(NI);
							// BUG-103: не discard() здесь — мы внутри обхода сущностей мира (см. tToDiscard выше)
							if (tToDiscard == null) tToDiscard = new ArrayListNoNulls<>(16);
							tToDiscard.add((ItemEntity)aEntity);
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

			// BUG-103: обход завершён — теперь удаление безопасно (движковые карты правятся вне итерации)
			if (tToDiscard != null) for (ItemEntity tDead : tToDiscard) if (!tDead.isRemoved()) tDead.discard();

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
	
	// Правка №1 (BUG-106): МИГРАЦИЯ старых миров — сущности руды/породы (материал в mMetaData) переливаются в
	// карту чанка (PrefixBlockOreMap) и снимаются НАВСЕГДА (чанк помечен на сохранение — при записи уйдёт уже
	// без них). Сущности с mItemNBT (канал №8 аудита) остаются жить, но материал дублируется в карту, чтобы
	// воронка чтения была единой. Тип сущности остаётся зарегистрированным вечно — он и есть читатель легаси.
	// ================================================================================================================
	// ПОЛЁТНЫЙ САМОПИСЕЦ (требование пользователя 2026-08-09: «любая многочасовая игра фиксирует всё — потом
	// разбирается любой баг без наигрывания»). Мод-половина чёрного ящика (внешняя — watchdog: JFR/GC/куча/дамп):
	// раз в 30 секунд строка CSV в logs/gt6-flight.csv — время, средний тик, куча, по каждому измерению чанки/
	// сущности/игроки/позиция. Команда /gt6mark <текст> — пометить МОМЕНТ бага в самописце и логе: при разборе
	// метка связывает слова пользователя с телеметрией и JFR-стеками по времени. Стоимость среза — микросекунды.
	// ================================================================================================================
	private static long sFlightTick = 0;
	private static boolean sFlightBroken = false;
	private static final java.time.format.DateTimeFormatter FLIGHT_TS = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static void flightWrite(net.minecraft.server.MinecraftServer aServer, String aLine) {
		if (sFlightBroken) return;
		try {
			java.nio.file.Path tDir = aServer.getServerDirectory().resolve("logs");
			java.nio.file.Files.createDirectories(tDir);
			java.nio.file.Path tFile = tDir.resolve("gt6-flight.csv");
			if (java.nio.file.Files.exists(tFile) && java.nio.file.Files.size(tFile) > 64L << 20) // ротация 64МБ
				java.nio.file.Files.move(tFile, tDir.resolve("gt6-flight-" + System.currentTimeMillis() + ".csv"));
			if (!java.nio.file.Files.exists(tFile))
				java.nio.file.Files.writeString(tFile, "время;тип;тик_мс;куча_МБ;куча_макс_МБ;измерение;чанков;сущностей;игроков;позиция;текст\n", java.nio.file.StandardOpenOption.CREATE);
			java.nio.file.Files.writeString(tFile, aLine, java.nio.file.StandardOpenOption.APPEND);
		} catch (Throwable e) {sFlightBroken = true; ERR.println("[GT6-FLIGHT] самописец отключён: " + e);}
	}

	private static void flightSample(net.minecraft.server.MinecraftServer aServer) {
		try {
			String tNow = java.time.LocalDateTime.now().format(FLIGHT_TS);
			double tTickMs = aServer.getAverageTickTimeNanos() / 1.0e6;
			Runtime tRt = Runtime.getRuntime();
			long tUsed = (tRt.totalMemory() - tRt.freeMemory()) >> 20, tMax = tRt.maxMemory() >> 20;
			StringBuilder tOut = new StringBuilder();
			for (net.minecraft.server.level.ServerLevel tLevel : aServer.getAllLevels()) {
				int tEntities = 0; for (@SuppressWarnings("unused") Object tE : tLevel.getAllEntities()) tEntities++;
				net.minecraft.server.level.ServerPlayer tFirst = tLevel.players().isEmpty() ? null : tLevel.players().get(0);
				tOut.append(tNow).append(";срез;").append(String.format(java.util.Locale.ROOT, "%.1f", tTickMs)).append(';').append(tUsed).append(';').append(tMax)
					.append(';').append(tLevel.dimension().identifier()).append(';').append(tLevel.getChunkSource().getLoadedChunksCount())
					.append(';').append(tEntities).append(';').append(tLevel.players().size())
					.append(';').append(tFirst == null ? "-" : tFirst.blockPosition().toShortString().replace(',', ' ')).append(";-\n");
			}
			flightWrite(aServer, tOut.toString());
		} catch (Throwable e) {/* срез не смеет ронять тик */}
	}

	/** Метка момента: пишется и в самописец, и в gregtech.log — при разборе связывает слова пользователя со стеками/телеметрией по времени. */
	public static void flightMark(net.minecraft.server.MinecraftServer aServer, String aText) {
		String tNow = java.time.LocalDateTime.now().format(FLIGHT_TS);
		String tSafe = aText == null ? "-" : aText.replace(';', ',').replace('\n', ' ');
		OUT.println("[GT6-MARK " + tNow + "] " + tSafe);
		flightWrite(aServer, tNow + ";МЕТКА;-;-;-;-;-;-;-;-;" + tSafe + "\n");
		flightSample(aServer); // срез в момент метки — состояние ровно тогда, когда пользователь это увидел
	}

	@SubscribeEvent
	public void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent aEvent) {
		aEvent.getDispatcher().register(net.minecraft.commands.Commands.literal("gt6mark")
			.executes(tCtx -> {flightMark(tCtx.getSource().getServer(), "метка без текста"); tCtx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("[GT6] метка записана в самописец"), false); return 1;})
			.then(net.minecraft.commands.Commands.argument("текст", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
				.executes(tCtx -> {flightMark(tCtx.getSource().getServer(), com.mojang.brigadier.arguments.StringArgumentType.getString(tCtx, "текст")); tCtx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("[GT6] метка записана в самописец"), false); return 1;})));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onChunkLoadMigrateOres(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		if (aEvent.getLevel() == null || aEvent.getLevel().isClientSide() || !(aEvent.getChunk() instanceof LevelChunk tChunk)) return;
		gregapi.block.prefixblock.PrefixBlock.migrateChunkOres(tChunk); // логика — в центре у данных (PrefixBlock)
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
				// Правка №1 (BUG-106): сущности у руды больше нет — бедрок-руда узнаётся по самому блоку (пара та же).
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
		if (BLAST_RESISTANT_MOB_SPAWNERS) aEvent.getAffectedBlocks().removeIf(p -> gregapi.util.WD.state(aEvent.getLevel(), p).getBlock() == net.minecraft.world.level.block.Blocks.SPAWNER);
	}
	
	// BUG-071 ПАРНАЯ ПОЛОВИНА МОСТА ДОБЫЧИ (к onBlockBreakSpeedEvent ниже): ПРАВО на дроп.
	// Дословный перенос Forge 1.7.10 ForgeHooks.canHarvestBlock (recompSrc ForgeHooks.java:95-116) — тот самый метод,
	// которым 1.7.10 и решал вопрос: материал без требования → можно; нет стека/типа → ванильный вердикт; уровень
	// инструмента < 0 (класс предмету чужой) → ванильный вердикт; иначе сравнение УРОВНЕЙ.
	// Почему это вообще понадобилось: в neo правило считается БЕЗ позиции (Item.isCorrectToolForDrops(stack,state)),
	// а у GT6 подтип блока живёт в BlockEntity — на этом пути мета вырождается в 0, и требуемый уровень становился
	// нулевым для ВСЕХ руд и машин (BUG-071, замер gt6harvestprobe). Событие HarvestCheck позицию несёт (PlayerEvent
	// .HarvestCheck:getPos), и движок ходит именно через него: ServerPlayerGameMode:291 → BlockState.canHarvestBlock
	// (level,pos,player) → IBlockExtension:216 → EventHooks.doPlayerHarvestCheck. Одна точка на весь мод — как и
	// соседний BreakSpeed-мост, который Грегориус завёл ровно для такой же цели (скорость).
	// Трогаем ТОЛЬКО блоки GT6 (контракт IBlock): чужие блоки судит движок, как и раньше.
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
			if (ST.invalid(tStack) || !UT.Code.stringValid(tTool)) return; // :102-107 — ванильный вердикт как есть
			int tToolLevel = WD.toolLevel(tStack, tTool);
			if (tToolLevel < 0) return;                                    // :109-113 — класс чужой → ванильный вердикт
			aEvent.setCanHarvest(tToolLevel >= WD.harvestLevel(tWorld, tPos.getX(), tPos.getY(), tPos.getZ())); // :115
		} catch (Throwable e) {/* право на дроп не должно ронять разрушение блока */}
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
				// ⛔ КРАШ-КЛАСС (лог04, 2026-08-07): здесь стоял discard() — дословный перенос 1.7.10 setDead(),
				// и в neo он ЛОМАЕТ ДВИЖОК. Событие постится ВНУТРИ добавления сущности в мир
				// (PersistentEntitySectionManager.addEntity:80), ДО setLevelCallback: discard() в этот момент
				// уводит сущность в removed при ЕЩЁ пустом callback'е (EntityInLevelCallback.NULL) — то есть
				// «удаление» никого не уведомляет. Движок продолжает добавление как ни в чём не бывало
				// (addEntityWithoutEvent: секция → callback → startTracking) и заносит УЖЕ УДАЛЁННУЮ сущность
				// в ChunkMap.entityMap. Дальше эта запись рвёт обход карты трекеров: NPE в fastutil-итераторе
				// ChunkMap.tick:1206 (падает тот, кто обходит, а не тот, кто испортил — в стеке нас нет).
				// Штатный neo-путь «сущность не появляется» — отмена события: движок делает return false ДО
				// добавления (addEntity:80), в мир она не попадает вовсе. Наблюдаемое поведение то же, что у
				// setDead в 1.7.10, где ни entityMap, ни callback-механизма не существовало.
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
		int aX = UT.Code.roundDown(aEvent.getX()), aY = (int)UT.Code.bind(WD.minY(aWorld), WD.topY(aWorld), UT.Code.roundDown(aEvent.getY())), aZ = UT.Code.roundDown(aEvent.getZ()); // BUG-089: было bind(0, getHeight()) — спавн на Y<0 кламплся к нулю, проверки судили чужую позицию

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

			// F-arrow-enchants: 1:1 оригинала (gregtech6/src/main/java/gregapi/GT_API_Proxy.java:1563-1569) — Power/Punch/Flame
			// применяются к снаряду ВРУЧНУЮ здесь, как делал автор, а не отдаются движку. Движковый путь (протащить лук как
			// AbstractArrow.firedFromWeapon, тогда EnchantmentHelper.modifyDamage/doKnockback отработают сами) снарядам GT6
			// недоступен: поле private, задаётся только конструктором Arrow(Level,…,weapon), а тот хардкодит EntityType.ARROW
			// (neo-decompiled Arrow.java:34), тогда как у снарядов GT6 свой EntityType (EntitiesGT.ARROW_*).
			// Эквиваленты сверены по ПОВЕДЕНИЮ, а не по имени:
			//   Power  1.7.10 setDamage(getDamage()+lvl*0.5+0.5)  -> setBaseDamage(getBaseDamageGT()+…), AbstractArrow.java:671;
			//   Punch  1.7.10 setKnockbackStrength(lvl)           -> центр EntityProjectile (величина применяется при попадании
			//                                                        тем же расчётом, что в 1.7.10 — EntityArrow_Material:250-253);
			//   Flame  1.7.10 setFire(lvl*100) в СЕКУНДАХ         -> igniteForSeconds(lvl*100), Entity.java:630 — ставит огонь
			//                                                        только если дольше текущего, семантика setFire сохранена.
			int tLevel = UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.POWER, aEvent.getBow());
			if (tLevel > 0) tArrowEntity.setBaseDamage(tArrowEntity.getBaseDamageGT() + tLevel * 0.5D + 0.5D);
			tLevel = UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.PUNCH, aEvent.getBow());
			if (tLevel > 0) tArrowEntity.setKnockbackStrength(tLevel);
			tLevel = UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FLAME, aEvent.getBow());
			if (tLevel > 0) tArrowEntity.igniteForSeconds(tLevel * 100);

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

	// ==========================================================================================================
	// F6-worldgen: ЕДИНЫЙ ДИСПЕТЧЕР ОТЛОЖЕННОЙ ДОРАБОТКИ СВЕЖЕГО ЧАНКА.
	//
	// Общий корень у всех задач здесь один: `WorldGenRegion` не рассылает соседям НИКАКИХ оповещений
	// (neo-decompiled/server/level/WorldGenRegion.java:257-262 — состояние пишется прямо в чанк), тогда как в
	// 1.7.10 populate шёл по живому `World.setBlock` с флагами 3, и движок доводил мир сам. Всё, что раньше
	// доделывал движок, приходится доделывать явно — и обязательно ПОСЛЕ генерации, на первой загрузке готового
	// чанка: в момент самой генерации соседние чанки могут быть не готовы, а часть ванильных фич ещё не отработала.
	//
	// Задачи различаются флагом, но очередь, отбор чанка и защита «чанк ещё не FULL» у них общие — поэтому
	// механизм ОДИН, а не по копии на задачу:
	//   RECHUNK_REDSTONE — данжи #39: разбудить редстоун-цепь (в 1.7.10 её будили flags=3 факелов при populate);
	//   RECHUNK_PLANTS   — уронить осиротевшую растительность (GT6 вытесняет нижнюю половину двублочных растений,
	//                      верхняя без оповещения повисала в воздухе — жалоба «высокая трава над камнем»).
	// PRODUCTION-механизм (не проба).
	// ==========================================================================================================
	public static final int RECHUNK_REDSTONE = 1, RECHUNK_PLANTS = 2;
	private static final java.util.concurrent.ConcurrentLinkedQueue<Object[]> sChunkFinishQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

	@net.neoforged.bus.api.SubscribeEvent
	public void onChunkLoadFinishWorldgen(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		if (!(aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel tLevel)) return;
		if (!aEvent.isNewChunk()) return; // только свежесгенерённые: в старых мирах уже стоящее не трогаем
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		int tTasks = RECHUNK_PLANTS; // растительность проверяется в каждом свежем чанке
		// редстоун — только в данж-области, она детерминирована якорной формулой
		if (gregapi.worldgen.dungeon.WorldgenDungeonGT.isDungeonAreaChunk(tLevel, tPos.x(), tPos.z())) tTasks |= RECHUNK_REDSTONE;
		sChunkFinishQueue.add(new Object[] {tLevel, tPos, tTasks});
	}

	private static void gt6ChunkFinishTick() {
		for (int n = 0; n < 4; n++) {
			Object[] tJob = sChunkFinishQueue.poll();
			if (tJob == null) return;
			net.minecraft.server.level.ServerLevel tLevel = (net.minecraft.server.level.ServerLevel)tJob[0];
			net.minecraft.world.level.ChunkPos tPos = (net.minecraft.world.level.ChunkPos)tJob[1];
			int tTasks = (Integer)tJob[2];
			net.minecraft.world.level.chunk.LevelChunk tChunk = tLevel.getChunkSource().getChunkNow(tPos.x(), tPos.z());
			if (tChunk == null) {sChunkFinishQueue.add(tJob); return;} // ещё не FULL — попробуем следующим тиком
			if ((tTasks & RECHUNK_PLANTS) != 0) {
				try {gregapi.util.WD.dropUnsupportedPlants(tLevel, tChunk);} catch (Throwable e) {e.printStackTrace(ERR);}
			}
			if ((tTasks & RECHUNK_REDSTONE) != 0) {
				try {
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
				} catch (Throwable e) {e.printStackTrace(ERR);}
			}
		}
	}


}
