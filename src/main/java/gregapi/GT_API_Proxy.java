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
	
	// ========== [GT6-LEAFPROBE] ВРЕМЕННАЯ проба BUG-005 РЕАЛЬНЫМ путём игрока (гейт run/gt6leafprobe.flag + -Pgt6probes) ==========
	// Урок ложных PASS: судья обязан идти путём игрока, не синтетикой. Здесь: НАСТОЯЩИЙ дуб выращивается ДВИЖКОМ
	// (саженец + SaplingBlock.advanceTree — канал костной муки), рубится ДОСЛОВНО кодом топора-лесоруба
	// (GT_Tool_Axe.convertBlockDrops:113-123 — скан ствола, цикл destroyBlock + WD.leafdecay(level,x,y,z,null,T,T)),
	// затем считаются ОСТАВШИЕСЯ ванильные листья. PASS = 0 листьев через ~13с после рубки (форс-распад);
	// FAIL = листья висят (ванильная randomTick-скорость = минуты). Снять при уборке фазы.
	private static int sLeafProbeTick = -1;
	private static net.minecraft.core.BlockPos sLeafProbeBase = null;
	private static int sLeafProbeBefore = 0;
	public static void gt6LeafProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sLeafProbeTick++;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = aServer.overworld();
		try {
			if (sLeafProbeTick == 60) {
				O.println("========== [GT6-LEAFPROBE] PHASE A: выращивание НАСТОЯЩЕГО дуба движком ==========");
				net.minecraft.core.BlockPos tSpawn = tLevel.getRespawnData().pos(); // spawn мира (LevelData.RespawnData.pos(), LevelData.java:30)
				int tX = tSpawn.getX() + 24, tZ = tSpawn.getZ() + 24;
				int tY = tLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tX, tZ);
				// площадка (воздух 9×14×9 + земля 3×3) — подготовка МЕСТА; само дерево целиком порождает движок
				for (int j = 0; j <= 13; j++) for (int i = -4; i <= 4; i++) for (int k = -4; k <= 4; k++)
					tLevel.setBlock(new net.minecraft.core.BlockPos(tX+i, tY+j, tZ+k), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				for (int i = -1; i <= 1; i++) for (int k = -1; k <= 1; k++)
					tLevel.setBlock(new net.minecraft.core.BlockPos(tX+i, tY-1, tZ+k), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 3);
				sLeafProbeBase = new net.minecraft.core.BlockPos(tX, tY, tZ);
				tLevel.setBlock(sLeafProbeBase, net.minecraft.world.level.block.Blocks.OAK_SAPLING.defaultBlockState(), 3);
				net.minecraft.world.level.block.SaplingBlock tSapling = (net.minecraft.world.level.block.SaplingBlock)net.minecraft.world.level.block.Blocks.OAK_SAPLING;
				for (int t = 0; t < 200 && tLevel.getBlockState(sLeafProbeBase).getBlock() == tSapling; t++)
					tSapling.advanceTree(tLevel, sLeafProbeBase, tLevel.getBlockState(sLeafProbeBase), tLevel.getRandom());
				boolean tGrown = tLevel.getBlockState(sLeafProbeBase).is(net.minecraft.tags.BlockTags.LOGS);
				sLeafProbeBefore = gt6LeafProbeCount(tLevel);
				O.println("[GT6-LEAFPROBE] дерево выращено=" + tGrown + " база=" + sLeafProbeBase + " ванильных листьев вокруг=" + sLeafProbeBefore + (tGrown && sLeafProbeBefore > 0 ? "" : "  => FAIL (нет дерева/листьев — проба не валидна)"));
			} else if (sLeafProbeTick == 120) {
				if (sLeafProbeBase == null) return;
				O.println("========== [GT6-LEAFPROBE] PHASE B: рубка ДОСЛОВНО кодом топора (GT_Tool_Axe:113-123) ==========");
				int aX = sLeafProbeBase.getX(), aY = sLeafProbeBase.getY(), aZ = sLeafProbeBase.getZ(), tChopped = 0;
				net.minecraft.world.level.block.Block aBlock = tLevel.getBlockState(sLeafProbeBase).getBlock();
				int tY = aY, tH = tLevel.getHeight(), tCount = 0;
				// Checking... (скан ствола вверх, GT_Tool_Axe:113-118; без бюджета прочности — прочность в игре подтверждена)
				while (++tY < tH) {if (WD.block(tLevel, aX, tY, aZ) != aBlock) break; tCount++;}
				// Harvesting... (GT_Tool_Axe:120-123)
				while (--tY > aY && tCount-- > 0 && tLevel.destroyBlock(new net.minecraft.core.BlockPos(aX, tY, aZ), T)) {
					tChopped++;
					if (FAST_LEAF_DECAY) WD.leafdecay(tLevel, aX, tY, aZ, null, T, T);
				}
				if (tLevel.destroyBlock(new net.minecraft.core.BlockPos(aX, aY, aZ), T)) tChopped++; // нижний блок в игре ломает сам игрок
				if (FAST_LEAF_DECAY) WD.leafdecay(tLevel, aX, aY, aZ, null, T, T);
				O.println("[GT6-LEAFPROBE] FAST_LEAF_DECAY=" + FAST_LEAF_DECAY + " срублено=" + tChopped + " очередь форс-распада=" + DELAYED_LEAF_DECAYS.size() + " листьев сейчас=" + gt6LeafProbeCount(tLevel));
			} else if (sLeafProbeTick == 200 || sLeafProbeTick == 380) {
				if (sLeafProbeBase == null) return;
				int tLeft = gt6LeafProbeCount(tLevel);
				// диагностика: гистограмма DISTANCE оставшихся + разбивка «крона ствола (±4)» vs «дальше» (в ±8 могут
				// попадать листья СОСЕДНИХ деревьев с легитимной опорой — их висение не дефект)
				int[] tHist = new int[8]; int tNear = 0, tFar = 0; StringBuilder tSample = new StringBuilder();
				for (int j = -1; j <= 13; j++) for (int i = -8; i <= 8; i++) for (int k = -8; k <= 8; k++) {
					net.minecraft.world.level.block.state.BlockState tS = tLevel.getBlockState(sLeafProbeBase.offset(i, j, k));
					if (tS.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) {
						int d = tS.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE);
						boolean p = tS.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT);
						tHist[d]++;
						if (Math.abs(i) <= 4 && Math.abs(k) <= 4) tNear++; else tFar++;
						if (tSample.length() < 600) tSample.append(" [").append(i).append(",").append(j).append(",").append(k).append(" d=").append(d).append(p?" P":"").append("]");
					}
				}
				O.println("[GT6-LEAFPROBE] tick+" + (sLeafProbeTick-120) + " после рубки: листьев=" + tLeft + " (у ствола ±4: " + tNear + ", дальше: " + tFar + ") очередь=" + DELAYED_LEAF_DECAYS.size());
				O.println("[GT6-LEAFPROBE]   DISTANCE-гистограмма d1..d7: " + tHist[1] + "/" + tHist[2] + "/" + tHist[3] + "/" + tHist[4] + "/" + tHist[5] + "/" + tHist[6] + "/" + tHist[7] + "  образцы:" + tSample);
				if (sLeafProbeTick == 380) {
					// вердикт по кроне НАШЕГО дуба: листья у ствола обязаны исчезнуть; d=7 без PERSISTENT где угодно = пропущенный распад
					O.println("[GT6-LEAFPROBE] ВЕРДИКТ: " + (tNear == 0 && tHist[7] == 0 && sLeafProbeBefore > 0 ? "PASS (крона снесена, зависших d7 нет)" : "FAIL"));
					O.println("========== [GT6-LEAFPROBE] DONE ==========");
				}
			}
		} catch (Throwable e) {O.println("[GT6-LEAFPROBE] EXC " + e); e.printStackTrace(O);}
	}
	private static int gt6LeafProbeCount(net.minecraft.server.level.ServerLevel aLevel) {
		int r = 0;
		for (int j = -1; j <= 13; j++) for (int i = -8; i <= 8; i++) for (int k = -8; k <= 8; k++)
			if (aLevel.getBlockState(sLeafProbeBase.offset(i, j, k)).getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) r++;
		return r;
	}

	// ========== [GT6-CRAFTPROBE] ВРЕМЕННАЯ проба BUG-002 РЕАЛЬНЫМ путём игрока (гейт run/gt6craftprobe.flag + -Pgt6probes) ==========
	// Реальный триггер игрока: ПАЛКА+КРЕМЕНЬ в сетке 2×2 инвентаря -> вылет (EncoderException container_set_slot,
	// «Can't find id for ... fire_aspect» по ИДЕНТИЧНОСТИ в реестре кодека). Проба идёт путём игрока: кладёт палку и
	// кремень в НАСТОЯЩИЕ крафт-слоты inventoryMenu ЖИВОГО игрока (slotChangedCraftingGrid движка сам вычислит
	// результат и пошлёт клиенту ClientboundContainerSetSlotPacket — тот самый путь краша), затем ФОРС-кодирует пакет
	// результата тем же кодеком в RegistryFriendlyByteBuf(server.registryAccess()) и дампит идентичность holder'ов
	// энчантов результата против реестра сервера. Снять при уборке фазы.
	// Двухмировой сценарий (КЛЮЧЕВОЙ для BUG-002): data-init GT6 (рецепты + зачарование статических mOutput через
	// isItemStackUsable) выполняется ОДИН раз за запуск, на первом server-start -> Holder.Reference из реестра МИРА-1
	// заперт маркером "ench" в статическом mOutput. Перезаход (МИР-2, НОВЫЕ инстансы динреестров) -> крафт копирует
	// протухший holder -> кодек ищет ПО ИДЕНТИЧНОСТИ -> «Can't find id» -> дисконнект (краш игрока). Проба: крафт в
	// МИРЕ-1, программный перезаход (клиент-хук), тот же крафт в МИРЕ-2.
	private static int sCraftProbeTick = -1;
	public  static volatile int sCraftProbeStage = 0; // 0=мир-1; 1=мир-1 готов (сигнал клиенту на перезаход); -1=перезаход запущен; 2=мир-2; 3=всё
	private static int sCraftProbeServerHash = 0;
	public static void gt6CraftProbeTick(net.minecraft.server.MinecraftServer aServer) {
		int tServerHash = System.identityHashCode(aServer);
		if (sCraftProbeServerHash == 0) sCraftProbeServerHash = tServerHash;
		else if (sCraftProbeServerHash != tServerHash) {
			sCraftProbeServerHash = tServerHash;
			if (sCraftProbeStage == -1) {sCraftProbeStage = 2; sCraftProbeTick = -1;} // перезаход состоялся: счёт заново для мира-2
		}
		sCraftProbeTick++;
		if (sCraftProbeTick != 200) return;
		if (sCraftProbeStage == 0) {
			gt6CraftProbeCraft(aServer, "МИР-1 (реестры те же, что у data-init)");
			sCraftProbeStage = 1; // клиент-хук увидит и сделает перезаход
		} else if (sCraftProbeStage == 2) {
			gt6CraftProbeCraft(aServer, "МИР-2 (ПЕРЕЗАХОД: реестры новые, mOutput со старым holder)");
			sCraftProbeStage = 3;
			new java.io.File("wgautoworld.world").delete(); // не оставлять «вход в существующий мир» следующим запускам
			gregapi.data.CS.OUT.println("========== [GT6-CRAFTPROBE] DONE-ALL ==========");
		}
	}
	private static void gt6CraftProbeCraft(net.minecraft.server.MinecraftServer aServer, String aLabel) {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-CRAFTPROBE] BUG-002: палка+кремень в 2×2 РЕАЛЬНОГО inventoryMenu — " + aLabel + " ==========");
		try {
			if (aServer.getPlayerList().getPlayers().isEmpty()) {O.println("[GT6-CRAFTPROBE] нет игрока на сервере => пропуск"); return;}
			net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
			net.minecraft.world.inventory.InventoryMenu tMenu = tPlayer.inventoryMenu;
			// сетка 2×2 inventoryMenu: слот 0 = результат, 1..4 = крафт; рецепт ножа "SX" (Loader_Tools:258) — палка слева, кремень справа
			tMenu.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
			tMenu.getSlot(2).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT));
			net.minecraft.world.item.ItemStack tResult = tMenu.getSlot(0).getItem();
			O.println("[GT6-CRAFTPROBE] результат-слот: " + tResult + " компоненты: " + tResult.getComponentsPatch());
			// дамп идентичности holder'ов энчантов результата vs server-реестр
			net.minecraft.world.item.enchantment.ItemEnchantments tEnch = tResult.getOrDefault(net.minecraft.world.item.enchantment.EnchantmentHelper.getComponentType(tResult), net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
			net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> tReg = aServer.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
			for (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> tHolder : tEnch.keySet()) {
				int tIdInReg = tReg.getId(tHolder.value());
				O.println("[GT6-CRAFTPROBE] энчант " + tHolder.getRegisteredName() + " holder.value@" + System.identityHashCode(tHolder.value())
					+ " реестр-инстанс@" + System.identityHashCode(tReg.getValue(net.minecraft.resources.Identifier.parse(tHolder.getRegisteredName())))
					+ " getId(value)=" + tIdInReg + (tIdInReg < 0 ? "  => ИДЕНТИЧНОСТЬ НЕ НАЙДЕНА (крашнет кодек!)" : "  => ok"));
			}
			// форс-кодирование ТЕМ ЖЕ кодеком, что валил игрока
			net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket tPacket = new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(tMenu.containerId, tMenu.getStateId(), 0, tResult.copy());
			io.netty.buffer.ByteBuf tRaw = io.netty.buffer.Unpooled.buffer();
			try {
				net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket.STREAM_CODEC.encode(new net.minecraft.network.RegistryFriendlyByteBuf(tRaw, aServer.registryAccess()), tPacket);
				O.println("[GT6-CRAFTPROBE] форс-encode container_set_slot: OK, " + tRaw.readableBytes() + " байт  => PASS");
			} catch (Throwable e) {
				O.println("[GT6-CRAFTPROBE] форс-encode container_set_slot: КРАШ ВОСПРОИЗВЕДЁН  => FAIL");
				e.printStackTrace(O);
			} finally {tRaw.release();}
			// чистка: убрать ингредиенты из сетки (не оставлять игроку артефакты пробы)
			tMenu.getSlot(1).set(net.minecraft.world.item.ItemStack.EMPTY);
			tMenu.getSlot(2).set(net.minecraft.world.item.ItemStack.EMPTY);
		} catch (Throwable e) {O.println("[GT6-CRAFTPROBE] EXC " + e); e.printStackTrace(O);}
		O.println("========== [GT6-CRAFTPROBE] " + aLabel + " КОНЕЦ ==========");
	}

	// ========== [GT6-ATTACKPROBE] ВРЕМЕННАЯ проба BUG-003 РЕАЛЬНЫМ путём игрока (гейт run/gt6attackprobe.flag + -Pgt6probes) ==========
	// Реальный путь: ЛКМ-атака ГТ-инструментом по мобу. Сервер: нож крафтится РЕАЛЬНЫМ путём (палка+кремень в живых
	// крафт-слотах, как gt6craftprobe), кладётся в руку игрока, рядом спавнится овца. Клиент (хук в Proxy_Client):
	// НАСТОЯЩИЙ Minecraft.gameMode.attack(player, sheep) — полный путь клиент-предсказания (onLeftClickEntity GT6 на
	// ОБЕИХ сторонах) + серверный удар + doDamage (износ) + синк слота. PASS = овца ранена, нож изношен, соединение
	// живо, исключений нет. Снять при уборке фазы.
	private static int sAttackProbeTick = -1;
	private static int sAttackProbeSheepId = -1;
	public  static volatile int sAttackProbeEntityId = -1; // сигнал клиенту: id овцы для атаки (-1 = рано; -2 = клиент отработал)
	public static void gt6AttackProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sAttackProbeTick++;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		try {
			if (sAttackProbeTick == 200) {
				O.println("========== [GT6-ATTACKPROBE] BUG-003: атака ГТ-ножом по овце РЕАЛЬНЫМ путём ==========");
				if (aServer.getPlayerList().getPlayers().isEmpty()) {O.println("[GT6-ATTACKPROBE] нет игрока => пропуск"); return;}
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				// нож — реальным крафт-путём (та же сетка, что у игрока)
				net.minecraft.world.inventory.InventoryMenu tMenu = tPlayer.inventoryMenu;
				tMenu.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
				tMenu.getSlot(2).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT));
				net.minecraft.world.item.ItemStack tKnife = tMenu.getSlot(0).getItem().copy();
				tMenu.getSlot(1).set(net.minecraft.world.item.ItemStack.EMPTY);
				tMenu.getSlot(2).set(net.minecraft.world.item.ItemStack.EMPTY);
				if (tKnife.isEmpty()) {O.println("[GT6-ATTACKPROBE] крафт ножа НЕ дал результата => FAIL (проба не валидна)"); return;}
				tPlayer.getInventory().setItem(0, tKnife);
				tPlayer.getInventory().setSelectedSlot(0);
				O.println("[GT6-ATTACKPROBE] нож в руке: " + tKnife + " прочность-урон до атаки: " + gregapi.data.CS.ToolsGT.sMetaTool.getToolDamage(tKnife));
				net.minecraft.world.entity.animal.sheep.Sheep tSheep = net.minecraft.world.entity.EntityType.SHEEP.spawn(tPlayer.level(), tPlayer.blockPosition().offset(1, 0, 1), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
				if (tSheep == null) {O.println("[GT6-ATTACKPROBE] овца не заспавнилась => FAIL (проба не валидна)"); return;}
				tSheep.setNoAi(true); // не убежала до удара (подготовка МЕСТА; сам удар — целиком движковый путь)
				O.println("[GT6-ATTACKPROBE] овца id=" + tSheep.getId() + " hp=" + tSheep.getHealth() + " -> сигнал клиенту на атаку");
				sAttackProbeSheepId = tSheep.getId();
				sAttackProbeEntityId = tSheep.getId();
			} else if (sAttackProbeTick == 320) {
				if (sAttackProbeSheepId < 0) return;
				if (sAttackProbeEntityId != -2) {O.println("[GT6-ATTACKPROBE] клиент так и НЕ атаковал (нож не синкнулся?) => FAIL"); O.println("========== [GT6-ATTACKPROBE] DONE =========="); return;}
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().isEmpty() ? null : aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.world.entity.Entity tEntity = aServer.overworld().getEntity(sAttackProbeSheepId);
				float tHp = tEntity instanceof net.minecraft.world.entity.LivingEntity tL ? tL.getHealth() : -1;
				long tToolDamage = tPlayer == null ? -1 : gregapi.data.CS.ToolsGT.sMetaTool.getToolDamage(tPlayer.getInventory().getItem(0));
				boolean tConnected = tPlayer != null && tPlayer.connection.isAcceptingMessages();
				O.println("[GT6-ATTACKPROBE] итог: овца hp=" + tHp + " (было 8.0) износ ножа=" + tToolDamage + " соединение живо=" + tConnected
					+ ((tHp >= 0 && tHp < 8.0F) && tToolDamage > 0 && tConnected ? "  => PASS (урон прошёл, износ есть, дисконнекта нет)" : "  => FAIL"));
				O.println("========== [GT6-ATTACKPROBE] DONE ==========");
			}
		} catch (Throwable e) {O.println("[GT6-ATTACKPROBE] EXC " + e); e.printStackTrace(O);}
	}

	// ========== [GT6-FLOWERPROBE] ВРЕМЕННАЯ проба BUG-008 РЕАЛЬНЫМ путём игрока (гейт run/gt6flowerprobe.flag + -Pgt6probes) ==========
	// Реальный путь: сломать ванильный мак рукой в SURVIVAL -> должен выпасть предмет. Проба ломает НАСТОЯЩИМ каналом
	// (ServerPlayerGameMode.destroyBlock: полный пайплайн listener'ов, лута и BlockDropsEvent), считает ItemEntity,
	// и отдельно дампит каждый подозреваемый шов ИЗОЛИРОВАННО (лут-таблица / ILLEGAL_DROPS / blockToDrop / OM+ST.update),
	// чтобы при пропаже дропа пальцем указать на едока. Снять при уборке фазы.
	// Матрица кейсов: {пустая рука, ГТ-нож, ГТ-топор} × {мак, одуванчик} — игрок мог ломать С ИНСТРУМЕНТОМ в руке
	// (ветка MultiItemTool.onHarvestBlockEvent/convertBlockDrops переписывает дропы). Счёт = сущности + дельта инвентаря.
	private static int sFlowerProbeTick = -1, sFlowerProbeCase = 0;
	private static net.minecraft.core.BlockPos sFlowerProbePos = null;
	private static int sFlowerProbeInvBefore = 0;
	private static net.minecraft.world.item.ItemStack sFlowerProbeKnife = null, sFlowerProbeAxe = null;
	private static final String[] FLOWER_CASES = {"рука+мак", "рука+одуванчик", "нож+мак", "нож+одуванчик", "топор+мак", "топор+одуванчик", "рука+куст_роз", "топор+куст_роз"};
	public static void gt6FlowerProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sFlowerProbeTick++;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		try {
			if (sFlowerProbeTick == 160) {
				O.println("========== [GT6-FLOWERPROBE] BUG-008: слом цветов реальным путём (SURVIVAL), матрица 6 кейсов ==========");
				if (aServer.getPlayerList().getPlayers().isEmpty()) {O.println("[GT6-FLOWERPROBE] нет игрока => пропуск"); sFlowerProbeCase = 99; return;}
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				// инструменты — реальным крафт-путём (нож: палка+кремень; топор: кремень×3+палка "XX","XS")
				net.minecraft.world.inventory.InventoryMenu tMenu = tPlayer.inventoryMenu;
				tMenu.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
				tMenu.getSlot(2).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT));
				sFlowerProbeKnife = tMenu.getSlot(0).getItem().copy();
				tMenu.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT));
				tMenu.getSlot(2).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT));
				tMenu.getSlot(3).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FLINT));
				tMenu.getSlot(4).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
				sFlowerProbeAxe = tMenu.getSlot(0).getItem().copy();
				tMenu.getSlot(1).set(net.minecraft.world.item.ItemStack.EMPTY); tMenu.getSlot(2).set(net.minecraft.world.item.ItemStack.EMPTY);
				tMenu.getSlot(3).set(net.minecraft.world.item.ItemStack.EMPTY); tMenu.getSlot(4).set(net.minecraft.world.item.ItemStack.EMPTY);
				O.println("[GT6-FLOWERPROBE] инструменты: нож=" + sFlowerProbeKnife + " топор=" + sFlowerProbeAxe);
			} else if (sFlowerProbeTick >= 200 && sFlowerProbeCase < FLOWER_CASES.length) {
				int tPhase = (sFlowerProbeTick - 200) % 40;
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().isEmpty() ? null : aServer.getPlayerList().getPlayers().get(0);
				if (tPlayer == null) {sFlowerProbeCase = 99; return;}
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				boolean tIsRoseBush = sFlowerProbeCase >= 6;
				net.minecraft.world.level.block.Block tFlower = tIsRoseBush ? net.minecraft.world.level.block.Blocks.ROSE_BUSH : (sFlowerProbeCase % 2 == 0) ? net.minecraft.world.level.block.Blocks.POPPY : net.minecraft.world.level.block.Blocks.DANDELION;
				net.minecraft.world.item.Item tFlowerItem = tFlower.asItem();
				if (tPhase == 0) {
					// подготовка кейса: цветок + инструмент в руку + замер инвентаря
					net.minecraft.world.item.ItemStack tTool = (sFlowerProbeCase < 2 || sFlowerProbeCase == 6) ? net.minecraft.world.item.ItemStack.EMPTY : (sFlowerProbeCase < 4) ? sFlowerProbeKnife.copy() : sFlowerProbeAxe.copy();
					tPlayer.getInventory().setItem(0, tTool);
					tPlayer.getInventory().setSelectedSlot(0);
					sFlowerProbePos = tPlayer.blockPosition().offset(2, 0, sFlowerProbeCase); // свежая позиция на кейс
					tLevel.setBlock(sFlowerProbePos.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 3);
					if (tIsRoseBush) net.minecraft.world.level.block.DoublePlantBlock.placeAt(tLevel, tFlower.defaultBlockState(), sFlowerProbePos, 3);
					else tLevel.setBlock(sFlowerProbePos, tFlower.defaultBlockState(), 3);
					sFlowerProbeInvBefore = tPlayer.getInventory().countItem(tFlowerItem);
				} else if (tPhase == 10) {
					net.minecraft.world.level.GameType tOldMode = tPlayer.gameMode();
					tPlayer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
					tPlayer.gameMode.destroyBlock(sFlowerProbePos);
					tPlayer.setGameMode(tOldMode);
				} else if (tPhase == 30) {
					int tEntities = 0;
					for (net.minecraft.world.entity.item.ItemEntity tD : tLevel.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(sFlowerProbePos).inflate(5)))
						if (tD.getItem().getItem() == tFlowerItem) tEntities += tD.getItem().getCount();
					int tInvDelta = tPlayer.getInventory().countItem(tFlowerItem) - sFlowerProbeInvBefore;
					int tTotal = tEntities + tInvDelta;
					O.println("[GT6-FLOWERPROBE] кейс «" + FLOWER_CASES[sFlowerProbeCase] + "»: сущностей=" + tEntities + " в инвентарь=" + tInvDelta + " ИТОГО=" + tTotal + (tTotal > 0 ? "  => PASS" : "  => FAIL (дроп съеден)"));
					sFlowerProbeCase++;
					if (sFlowerProbeCase >= FLOWER_CASES.length) O.println("========== [GT6-FLOWERPROBE] DONE ==========");
				}
			}
		} catch (Throwable e) {O.println("[GT6-FLOWERPROBE] EXC " + e); e.printStackTrace(O); sFlowerProbeCase = 99;}
	}

	// ========== [GT6-SLABPROBE] ВРЕМЕННАЯ проба BUG-010 (гейт run/gt6slabprobe.flag + -Pgt6probes) ==========
	// Игрок: «слэбов в JEI в 5× больше оригинала; большинство выглядят как блоки; любые ставятся/выглядят полными».
	// Сервер ставит DOWN-слэб первого BlockMetaType, клиент (хук в Proxy_Client) меряет НАСТОЯЩИЕ квады клиентской
	// модели (min/max Y вершин — то, что реально видит игрок, без пикселей) + считает слэб-предметы в реестре.
	// Снять при уборке фазы.
	private static int sSlabProbeTick = -1;
	public  static volatile long sSlabProbePos = Long.MIN_VALUE; // сигнал клиенту: BlockPos.asLong установленного слэба
	public static void gt6SlabProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sSlabProbeTick++;
		if (sSlabProbeTick == 300) {gt6SlabProbePhaseB(aServer); return;}
		if (sSlabProbeTick != 200) return;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		try {
			O.println("========== [GT6-SLABPROBE] BUG-010: замер РЕАЛЬНОЙ клиентской модели слэба ==========");
			if (aServer.getPlayerList().getPlayers().isEmpty()) {O.println("[GT6-SLABPROBE] нет игрока => пропуск"); return;}
			net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
			net.minecraft.world.level.block.Block tStoneB = BlocksGT.stones[0];
			if (!(tStoneB instanceof gregapi.block.metatype.BlockMetaType tMT) || tMT.mSlabs == null) {O.println("[GT6-SLABPROBE] stones[0] без слэбов => SKIP"); return;}
			net.minecraft.core.BlockPos tPos = tPlayer.blockPosition().offset(3, 4, 3); // в воздухе — не зависеть от поверхности (снег и т.п.)
			for (int i = 0; i < 6; i++) if (tMT.mSlabs[i] != null)
				WD.set(tPlayer.level(), tPos.getX() + i, tPos.getY(), tPos.getZ(), tMT.mSlabs[i], 0, 3); // все 6 ориентаций подряд по X
			O.println("[GT6-SLABPROBE] сервер: 6 слэб-вариантов " + tMT.mSlabs[0] + ".. поставлены от " + tPos + " -> сигнал клиенту");
			sSlabProbePos = tPos.asLong();
		} catch (Throwable e) {O.println("[GT6-SLABPROBE] EXC " + e); e.printStackTrace(O);}
		if (sSlabProbeTick != -999) return; // (фаза B ниже переиспользует метод через отдельный тик)
	}
	// Фаза B (tick 300): УСТАНОВКА реальным путём item.useOn (полная цепь useOn->onItemUse->BlockBase->placeBlockAt):
	// (1) клик сверху -> DOWN-слэб; (2) клик в боковую грань -> БОКОВОЙ вариант; (3) клик под воду -> WATERLOGGED;
	// (4) коллизия поставленного = полкуба. Живой тест игрока показал: всегда DOWN + коллизия куба + вода исчезает.
	private static void gt6SlabProbePhaseB(net.minecraft.server.MinecraftServer aServer) {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		try {
			O.println("========== [GT6-SLABPROBE-B] установка/коллизия/вода реальным useOn ==========");
			if (aServer.getPlayerList().getPlayers().isEmpty()) {O.println("[GT6-SLABPROBE-B] нет игрока => пропуск"); return;}
			net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
			net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
			gregapi.block.metatype.BlockMetaType tMT = (gregapi.block.metatype.BlockMetaType)BlocksGT.stones[0];
			net.minecraft.world.level.block.Block tSlab0 = tMT.mSlabs[0];
			net.minecraft.core.BlockPos tB1 = tPlayer.blockPosition().offset(-4, 0, 0), tB2 = tB1.offset(0, 0, 3), tB3 = tB1.offset(0, 0, 6);
			for (net.minecraft.core.BlockPos p : new net.minecraft.core.BlockPos[] {tB1, tB2, tB3}) {
				tLevel.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
				tLevel.setBlock(p.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			}
			// (1) клик на ВЕРХ опоры, хит в центре
			tPlayer.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(tSlab0)); tPlayer.getInventory().setSelectedSlot(0);
			tPlayer.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(tPlayer, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tB1).add(0, 0.5, 0), net.minecraft.core.Direction.UP, tB1, false)));
			net.minecraft.world.level.block.Block tR1 = tLevel.getBlockState(tB1.above()).getBlock();
			O.println("[GT6-SLABPROBE-B] (1) клик сверху: встал " + tR1 + (tR1 == tSlab0 ? "  => PASS (DOWN-слэб)" : "  => FAIL"));
			// (4) коллизия поставленного
			net.minecraft.world.phys.AABB tCol = tLevel.getBlockState(tB1.above()).getCollisionShape(tLevel, tB1.above()).bounds();
			O.println("[GT6-SLABPROBE-B] (4) коллизия: Y=[" + (float)tCol.minY + ".." + (float)tCol.maxY + "]" + (tCol.maxY <= 0.55 ? "  => PASS (полкуба)" : "  => FAIL (куб)"));
			// (2) клик в СЕВЕРНУЮ грань опоры, хит в центре грани -> слэб в клетке севернее, боковой вариант
			net.minecraft.core.BlockPos tTarget2 = tB2.north();
			tLevel.setBlock(tTarget2, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			tPlayer.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(tSlab0));
			tPlayer.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(tPlayer, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(tB2.getX() + 0.5, tB2.getY() + 0.5, tB2.getZ()), net.minecraft.core.Direction.NORTH, tB2, false)));
			net.minecraft.world.level.block.Block tR2 = tLevel.getBlockState(tTarget2).getBlock();
			boolean tSideVariant = tR2 instanceof gregapi.block.metatype.BlockMetaType tR2MT && tR2MT.mIsSlab && tR2 != tSlab0;
			O.println("[GT6-SLABPROBE-B] (2) клик в бок: встал " + tR2 + (tSideVariant ? "  => PASS (боковой вариант)" : "  => FAIL (не боковой)"));
			// (3) вода: над опорой источник, клик на верх опоры -> слэб в воде, WATERLOGGED
			tLevel.setBlock(tB3.above(), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
			tPlayer.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(tSlab0));
			tPlayer.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(tPlayer, net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tB3).add(0, 0.5, 0), net.minecraft.core.Direction.UP, tB3, false)));
			net.minecraft.world.level.block.state.BlockState tS3 = tLevel.getBlockState(tB3.above());
			boolean tWL = tS3.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && tS3.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
			O.println("[GT6-SLABPROBE-B] (3) в воду: встал " + tS3.getBlock() + " WATERLOGGED=" + tWL + " fluid=" + tS3.getFluidState().getType()
				+ (tS3.getBlock() instanceof gregapi.block.metatype.BlockMetaType && tWL ? "  => PASS (вода осталась)" : "  => FAIL"));
			O.println("========== [GT6-SLABPROBE-B] DONE ==========");
		} catch (Throwable e) {O.println("[GT6-SLABPROBE-B] EXC " + e); e.printStackTrace(O);}
	}

	// ========== [GT6-ANVILPROBE] ВРЕМЕННАЯ проба BUG-011 РЕАЛЬНЫМ путём (гейт run/gt6anvilprobe.flag + -Pgt6probes) ==========
	// Репорт: «предмет не ложится в слот наковальни, а исчезает». Проба: НАСТОЯЩАЯ наковальня (item из MTE-реестра,
	// установка реальным useOn), затем ПКМ GT-слитком железа по верхней грани (реальный useOn -> onBlockActivated3).
	// Судья: слиток лёг в слот 0/1 наковальни, сумма предметов (рука+слоты) сохранена, ничего не исчезло. Снять при уборке.
	private static int sAnvilProbeTick = -1;
	private static net.minecraft.core.BlockPos sAnvilPos = null;
	public  static volatile long sAnvilProbeClientClick = Long.MIN_VALUE; // сигнал клиенту: кликнуть по верху наковальни (забор)
	public static void gt6AnvilProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sAnvilProbeTick++;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		try {
			if (sAnvilProbeTick == 200) {
				O.println("========== [GT6-ANVILPROBE] BUG-011: наковальня реальным путём ==========");
				if (aServer.getPlayerList().getPlayers().isEmpty()) {O.println("[GT6-ANVILPROBE] нет игрока => пропуск"); return;}
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
				net.minecraft.world.item.ItemStack tAnvil = tReg == null ? null : tReg.getItem(32025); // Stone Anvil (Loader_MultiTileEntities:2187)
				if (tAnvil == null || tAnvil.isEmpty()) {O.println("[GT6-ANVILPROBE] предмет наковальни (32025) не получен => FAIL (проба не валидна)"); return;}
				net.minecraft.core.BlockPos tBase = tPlayer.blockPosition().offset(-4, 0, -4);
				tLevel.setBlock(tBase, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
				tLevel.setBlock(tBase.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				tPlayer.getInventory().setItem(0, tAnvil); tPlayer.getInventory().setSelectedSlot(0);
				tPlayer.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(tPlayer, net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tBase).add(0, 0.5, 0), net.minecraft.core.Direction.UP, tBase, false)));
				sAnvilPos = tBase.above();
				O.println("[GT6-ANVILPROBE] установка: на " + sAnvilPos + " встал " + tLevel.getBlockState(sAnvilPos).getBlock() + " BE=" + tLevel.getBlockEntity(sAnvilPos));
			} else if (sAnvilProbeTick == 240) {
				if (sAnvilPos == null) return;
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				net.minecraft.world.item.ItemStack tIngot = gregapi.data.OP.ingot.mat(gregapi.data.MT.Fe, 4);
				net.minecraft.world.level.block.entity.BlockEntity tBE0 = tLevel.getBlockEntity(sAnvilPos);
				// DIAG рецепт-гейта размещения (условие onBlockActivated3:240)
				try {
					O.println("[GT6-ANVILPROBE] DIAG containsInput(ingot): Anvil=" + RM.Anvil.containsInput(tIngot, (gregapi.random.IHasWorldAndCoords)tBE0, NI)
						+ " BendSmall=" + RM.AnvilBendSmall.containsInput(tIngot, (gregapi.random.IHasWorldAndCoords)tBE0, NI)
						+ " BendBig=" + RM.AnvilBendBig.containsInput(tIngot, (gregapi.random.IHasWorldAndCoords)tBE0, NI));
				} catch (Throwable e) {O.println("[GT6-ANVILPROBE] DIAG containsInput EXC " + e);}
				O.println("[GT6-ANVILPROBE] в руке: " + tIngot + " -> ПКМ по верху наковальни (реальный канал gameMode.useItemOn)");
				tPlayer.getInventory().setItem(0, tIngot); tPlayer.getInventory().setSelectedSlot(0);
				tPlayer.gameMode.useItemOn(tPlayer, tLevel, tPlayer.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(sAnvilPos).add(0.2, 0.5, 0.2), net.minecraft.core.Direction.UP, sAnvilPos, false));
				net.minecraft.world.level.block.entity.BlockEntity tBE = tLevel.getBlockEntity(sAnvilPos);
				net.minecraft.world.item.ItemStack tHand = tPlayer.getInventory().getItem(0);
				String tSlots = "(BE не Container)"; int tInSlots = 0;
				if (tBE instanceof net.minecraft.world.Container tC) {
					tSlots = "slot0=" + tC.getItem(0) + " slot1=" + tC.getItem(1);
					for (int i = 0; i <= 1; i++) if (tC.getItem(i) != null && !tC.getItem(i).isEmpty()) tInSlots += tC.getItem(i).getCount();
				}
				int tTotal = tInSlots + (tHand == null || tHand.isEmpty() ? 0 : tHand.getCount());
				O.println("[GT6-ANVILPROBE] после клика: рука=" + tHand + " " + tSlots + " сумма=" + tTotal + "/4"
					+ (tInSlots > 0 && tTotal == 4 ? "  => PASS (лёг, ничего не исчезло)" : tTotal < 4 ? "  => FAIL (ПРОПАЖА подтверждена)" : "  => FAIL (не ложится, но цел)"));
			} else if (sAnvilProbeTick == 280) {
				// Фаза C (репорт игрока раунда 3: «слот руки после укладки „занят“, не забирается; свежий слот работает
				// один раз»): забор — рука КАК ОСТАЛАСЬ после укладки (НЕ чистим!), клик С КЛИЕНТА (настоящий путь с
				// предиктом). Дамп обеих рук — серверной и клиентской — ловит рассинк инвентаря после ST.move.
				if (sAnvilPos == null) return;
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.world.item.ItemStack tSrvHand = tPlayer.getInventory().getItem(0);
				O.println("[GT6-ANVILPROBE-C] серверная рука ПОСЛЕ укладки: " + tSrvHand + " isEmpty=" + tSrvHand.isEmpty() + " @" + System.identityHashCode(tSrvHand) + " -> сигнал клиенту на клик-забор");
				sAnvilProbeClientClick = sAnvilPos.asLong();
			} else if (sAnvilProbeTick == 340) {
				if (sAnvilPos == null) return;
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				net.minecraft.world.level.block.entity.BlockEntity tBE = tLevel.getBlockEntity(sAnvilPos);
				String tSlots2 = "(BE не Container)"; int tInSlots2 = 0;
				if (tBE instanceof net.minecraft.world.Container tC) {
					tSlots2 = "slot0=" + tC.getItem(0) + " slot1=" + tC.getItem(1);
					for (int i = 0; i <= 1; i++) if (tC.getItem(i) != null && !tC.getItem(i).isEmpty()) tInSlots2 += tC.getItem(i).getCount();
				}
				int tInv = tPlayer.getInventory().countItem(net.minecraft.world.item.Items.IRON_INGOT);
				O.println("[GT6-ANVILPROBE-C] итог забора: у игрока слитков=" + tInv + " " + tSlots2 + " сумма=" + (tInv + tInSlots2) + "/4"
					+ (tInv == 4 && tInSlots2 == 0 ? "  => PASS (забрал)" : (tInv + tInSlots2) < 4 ? "  => FAIL (ПРОПАЖА)" : "  => FAIL (НЕ ОТДАЁТ — репродукция репорта)"));
				O.println("========== [GT6-ANVILPROBE] DONE ==========");
			}
		} catch (Throwable e) {O.println("[GT6-ANVILPROBE] EXC " + e); e.printStackTrace(O);}
	}

	// ========== [GT6-BUGVERIFY] ВРЕМЕННАЯ механическая проба фиксов BUG-001..010 (гейт run/gt6bugverify.flag + -Pgt6probes) ==========
	// Прогоняет в РЕАЛЬНОМ серверном мире (overworld) ДО сдачи: F13 мета-round-trip, дроп BUG-006, реестр урона BUG-004
	// (тот самый getId(holder.value()), что падал в кодеке), распад листвы BUG-005 (tick-мост+мета), denull BUG-001.
	// Снять при уборке фазы (как wall-судьи 794f8a15). BUG-003 (клиент-предикшен атаки) серверной пробой не воспроизводим.
	private static int sBugVerifyTick = -1;
	private static final int BV_X = 2, BV_Y = 210, BV_Z = 2, BV_LX = 14, BV_LY = 208, BV_LZ = 14;
	public static void gt6BugVerifyTick(net.minecraft.server.MinecraftServer aServer) {
		sBugVerifyTick++;
		if (sBugVerifyTick == 40) gt6BugVerifyPhaseA(aServer);
		else if (sBugVerifyTick == 90) gt6BugVerifyPhaseB(aServer);
	}
	private static void gt6BugVerifyPhaseA(net.minecraft.server.MinecraftServer aServer) {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		O.println("========== [GT6-BUGVERIFY] PHASE A ==========");
		net.minecraft.server.level.ServerLevel tLevel = aServer.overworld();
		try { // F13 мета round-trip (BUG-009/010/006-type)
			net.minecraft.world.level.block.Block tLog = BlocksGT.Log1;
			WD.set(tLevel, BV_X, BV_Y, BV_Z, tLog, 5, 3);  int m1 = WD.meta(tLevel, BV_X, BV_Y, BV_Z);
			WD.set(tLevel, BV_X, BV_Y, BV_Z, tLog, 11, 3); int m2 = WD.meta(tLevel, BV_X, BV_Y, BV_Z);
			O.println("[GT6-BUGVERIFY] F13 meta round-trip Log1: set5->read" + m1 + " | set11->read" + m2 + "  => " + (m1==5 && m2==11 ? "PASS" : "FAIL"));
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] F13 meta EXC " + e); e.printStackTrace(O);}
		try { // BUG-006 дроп simple-блока
			net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(BV_X, BV_Y, BV_Z);
			java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(tLevel.getBlockState(p), tLevel, p, null);
			O.println("[GT6-BUGVERIFY] BUG-006 drops Log1: count=" + drops.size() + (drops.isEmpty()?"":" first="+drops.get(0)) + "  => " + (!drops.isEmpty() ? "PASS" : "FAIL"));
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] BUG-006 EXC " + e); e.printStackTrace(O);}
		try { // BUG-004 типы урона в реестре + getId(holder.value()) НЕ кидает (это и был краш кодека)
			net.minecraft.core.Registry<net.minecraft.world.damagesource.DamageType> reg = tLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
			gregapi.damage.DamageSources.GregTechDamageSource ds = gregapi.damage.DamageSources.getExplodingDamage();
			int id = reg.getId(ds.typeHolder().value());
			O.println("[GT6-BUGVERIFY] BUG-004 damage: getExplodingDamage holder getId=" + id + "  => " + (id >= 0 ? "PASS" : "FAIL"));
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] BUG-004 EXC (=был бы краш кодека!) " + e); e.printStackTrace(O);}
		try { // BUG-001 denull с count-0 стеком (старый код писал null в NonNullList -> NPE)
			net.minecraft.world.SimpleContainer inv = new net.minecraft.world.SimpleContainer(3);
			ItemStack zero = new ItemStack(net.minecraft.world.item.Items.STICK, 1); zero.setCount(0);
			inv.setItem(0, zero); ST.denull(inv);
			O.println("[GT6-BUGVERIFY] BUG-001 denull(count=0): no NPE, slot0empty=" + inv.getItem(0).isEmpty() + "  => PASS");
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] BUG-001 denull THREW (=баг жив!) " + e);}
		try { // BUG-005 листва: постановка decayable-меты БЕЗ опорных логов + scheduleTick (проверка в PHASE B)
			net.minecraft.world.level.block.Block tLeaf = BlocksGT.Leaves_AB;
			WD.set(tLevel, BV_LX, BV_LY, BV_LZ, tLeaf, 8, 3); int lm = WD.meta(tLevel, BV_LX, BV_LY, BV_LZ);
			tLevel.scheduleTick(new net.minecraft.core.BlockPos(BV_LX, BV_LY, BV_LZ), tLeaf, 2);
			O.println("[GT6-BUGVERIFY] BUG-005 leaf setup: Leaves_AB meta(read)=" + lm + " (нужно 8) scheduled decay tick -> проверка в PHASE B");
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] BUG-005 setup EXC " + e); e.printStackTrace(O);}
		try { // BUG-010 ФОРМА СЛЭБА (геометрия, не скриншот): getShape/renderBounds — полкуба или полный куб?
			net.minecraft.world.level.block.Block tStoneB = BlocksGT.stones[0];
			if (tStoneB instanceof gregapi.block.metatype.BlockMetaType tMT && tMT.mSlabs != null && tMT.mSlabs.length > gregapi.data.CS.SIDE_DOWN && tMT.mSlabs[gregapi.data.CS.SIDE_DOWN] != null) {
				net.minecraft.world.level.block.Block tSlab = tMT.mSlabs[gregapi.data.CS.SIDE_DOWN];
				net.minecraft.core.BlockPos ps = new net.minecraft.core.BlockPos(BV_X+4, BV_Y, BV_Z);
				WD.set(tLevel, BV_X+4, BV_Y, BV_Z, tSlab, 0, 3);
				net.minecraft.world.phys.AABB bb = tLevel.getBlockState(ps).getShape(tLevel, ps).bounds();
				boolean full = bb.minX<=0.01&&bb.minY<=0.01&&bb.minZ<=0.01&&bb.maxX>=0.99&&bb.maxY>=0.99&&bb.maxZ>=0.99;
				float[] rb = ((gregapi.block.BlockBase)tSlab).getRenderBounds();
				O.println("[GT6-BUGVERIFY] BUG-010 slab(DOWN) getShape Y=[" + (float)bb.minY + ".." + (float)bb.maxY + "] renderBounds=" + java.util.Arrays.toString(rb) + "  => " + (!full ? "PASS (НЕ полный куб — форма есть)" : "FAIL (полный куб)"));
			} else O.println("[GT6-BUGVERIFY] BUG-010 slab: stones[0] не BlockMetaType/нет слэбов => SKIP (проверить иначе)");
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] BUG-010 EXC " + e); e.printStackTrace(O);}
		try { // BUG-009 ОРИЕНТАЦИЯ ЛОГА (геометрия текстур, не скриншот): getIcon per side вертикаль(m0) vs горизонт(PILLAR_X)
			gregapi.block.BlockBaseMeta tLogM = (gregapi.block.BlockBaseMeta) BlocksGT.Log1;
			StringBuilder v = new StringBuilder(), h = new StringBuilder();
			for (int s = 0; s < 6; s++) {
				net.minecraft.resources.Identifier iv = tLogM.getIcon(s, 0), ih = tLogM.getIcon(s, gregapi.data.CS.PILLAR_X);
				v.append(iv==null?"null":iv.getPath()).append(s<5?" | ":"");
				h.append(ih==null?"null":ih.getPath()).append(s<5?" | ":"");
			}
			boolean differ = !v.toString().equals(h.toString());
			O.println("[GT6-BUGVERIFY] BUG-009 log getIcon вертикаль(m0):  " + v);
			O.println("[GT6-BUGVERIFY] BUG-009 log getIcon горизонт(m4):   " + h);
			O.println("[GT6-BUGVERIFY] BUG-009 orient (грань-текстуры меняются с ориентацией + getRenderType=PILLAR даёт UV-поворот) => " + (differ ? "PASS" : "FAIL"));
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] BUG-009 EXC " + e); e.printStackTrace(O);}
		O.println("========== [GT6-BUGVERIFY] PHASE A END ==========");
	}
	private static void gt6BugVerifyPhaseB(net.minecraft.server.MinecraftServer aServer) {
		java.io.PrintStream O = gregapi.data.CS.OUT;
		net.minecraft.server.level.ServerLevel tLevel = aServer.overworld();
		try { // BUG-005 tick-мост+мета: листва распалась?
			net.minecraft.world.level.block.state.BlockState st = tLevel.getBlockState(new net.minecraft.core.BlockPos(BV_LX, BV_LY, BV_LZ));
			O.println("[GT6-BUGVERIFY] BUG-005 leaf decay (tick-мост): after schedule -> block=" + st.getBlock() + " isAir=" + st.isAir() + "  => " + (st.isAir() ? "PASS" : "FAIL"));
		} catch (Throwable e) {O.println("[GT6-BUGVERIFY] BUG-005 check EXC " + e); e.printStackTrace(O);}
		O.println("========== [GT6-BUGVERIFY] DONE ==========");
	}

	// ========== [GT6-OREPROBE] ВРЕМЕННАЯ проба BUG-020 РЕАЛЬНЫМ путём игрока (гейт run/gt6oreprobe.flag + -Pgt6probes) ==========
	// Игрок: «инструментами не добывается ни одна руда — блок исчезает от одного удара». Корень: PrefixBlock (класс ВСЕХ руд)
	// не мостил getDestroyProgress (destroyTime=0 → мгновенный слом), mBaseHardness не участвовал. Судья: НАСТОЯЩАЯ руда
	// (BlocksGT.ore, материал MT.Fe) ставится реальным каналом placeBlock (как worldgen), замер state.getDestroyProgress
	// (тот же вызов, что у движка при добыче: кирка/рука/контроль-камень), затем реальный слом gameMode.destroyBlock
	// (SURVIVAL, полный пайплайн) — дроп + износ кирки (getToolDamage до/после, БЫЛО: износ от протухшей меты). Снять при уборке фазы.
	private static int sOreProbeTick = -1, sOreProbeCase = 0;
	private static net.minecraft.core.BlockPos sOreProbeOre = null, sOreProbeStone = null;
	private static long sOreProbeDamageBefore = -1;
	// матрица лута Drops 1:1 (оригинал Loader_Ores.java:77 = Drops(oreBroken, ore, oreRaw, 0, 1)):
	// без чар -> битая руда; silk touch -> сам блок руды; fortune -> oreRaw с множителем 1+rnd(f+1)
	private static final String[] ORE_CASES = {"кирка без чар -> битая руда", "silk-кирка -> сам блок руды", "fortuneIII-кирка -> oreRaw xN"};
	private static int sOreProbeExplOres = 0;
	private static final int[] OFFX_RING = {1,1,1,0,0,-1,-1,-1}, OFFZ_RING = {-1,0,1,-1,1,-1,0,1};
	public static void gt6OreProbeTick(net.minecraft.server.MinecraftServer aServer) {
		sOreProbeTick++;
		java.io.PrintStream O = gregapi.data.CS.OUT;
		try {
			if (sOreProbeTick == 200) {
				O.println("========== [GT6-OREPROBE] BUG-020: твёрдость/добыча руды PrefixBlock реальным путём ==========");
				if (aServer.getPlayerList().getPlayers().isEmpty()) {O.println("[GT6-OREPROBE] нет игрока => пропуск"); sOreProbeTick = 9999; return;}
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				// стальная кирка ГТ в руку (канал ToolsGT — как крафт игрока)
				ItemStack tPick = ToolsGT.sMetaTool.getToolWithStats(ToolsGT.PICKAXE, MT.Steel, MT.Steel);
				tPlayer.getInventory().setItem(0, tPick);
				tPlayer.getInventory().setSelectedSlot(0);
				O.println("[GT6-OREPROBE] кирка в руке: " + tPlayer.getMainHandItem());
				// руда железа реальным каналом установки руд (placeBlock — тот же, что worldgen) + контроль-камень
				sOreProbeOre   = tPlayer.blockPosition().offset(4, 1, 4);
				sOreProbeStone = tPlayer.blockPosition().offset(4, 1, 6);
				boolean tPlaced = ((gregapi.block.prefixblock.PrefixBlock)BlocksGT.ore).placeBlock(tLevel, sOreProbeOre.getX(), sOreProbeOre.getY(), sOreProbeOre.getZ(), SIDE_UNKNOWN, MT.Fe.mID, null, T, T);
				tLevel.setBlock(sOreProbeStone, Blocks.STONE.defaultBlockState(), 3);
				net.minecraft.world.level.block.state.BlockState tOreState = tLevel.getBlockState(sOreProbeOre);
				O.println("[GT6-OREPROBE] руда поставлена=" + tPlaced + " state=" + tOreState + " материал=" + ((gregapi.block.prefixblock.PrefixBlock)BlocksGT.ore).getMetaMaterial(tLevel, sOreProbeOre.getX(), sOreProbeOre.getY(), sOreProbeOre.getZ()));
				// изолированный шов — ровно тот вызов, что движок делает каждый тик добычи:
				float tProgPick  = tOreState.getDestroyProgress(tPlayer, tLevel, sOreProbeOre);
				tPlayer.getInventory().setSelectedSlot(1); // пустая рука
				float tProgHand  = tOreState.getDestroyProgress(tPlayer, tLevel, sOreProbeOre);
				float tProgStone = tLevel.getBlockState(sOreProbeStone).getDestroyProgress(tPlayer, tLevel, sOreProbeStone);
				tPlayer.getInventory().setSelectedSlot(0);
				int tTicksPick = tProgPick  > 0 ? (int)Math.ceil(1.0F/tProgPick) : -1;
				int tTicksHand = tProgHand  > 0 ? (int)Math.ceil(1.0F/tProgHand) : -1;
				O.println("[GT6-OREPROBE] progress/tick: кирка=" + tProgPick + " (тиков=" + tTicksPick + ")  рука=" + tProgHand + " (тиков=" + tTicksHand + ")  контроль-камень(рука)=" + tProgStone);
				O.println("[GT6-OREPROBE] мгновенный слом (>=1/tick)? кирка=" + (tProgPick >= 1) + " рука=" + (tProgHand >= 1) + "  => " + (tProgPick < 1 && tProgPick > 0 && tProgHand < 1 ? "PASS (руда не исчезает с одного удара)" : "FAIL"));
				sOreProbeDamageBefore = gregapi.item.multiitem.MultiItemTool.getToolDamage(tPlayer.getMainHandItem());
			} else if (sOreProbeTick >= 240 && sOreProbeCase < ORE_CASES.length) {
				int tPhase = (sOreProbeTick - 240) % 40;
				if (aServer.getPlayerList().getPlayers().isEmpty()) {sOreProbeTick = 9999; return;}
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				if (tPhase == 0) {
					sOreProbeOre = tPlayer.blockPosition().offset(4, 1, 8 + sOreProbeCase); // свежая позиция на кейс, вне радиуса подбора
					((gregapi.block.prefixblock.PrefixBlock)BlocksGT.ore).placeBlock(tLevel, sOreProbeOre.getX(), sOreProbeOre.getY(), sOreProbeOre.getZ(), SIDE_UNKNOWN, MT.Fe.mID, null, T, T);
					ItemStack tPick = ToolsGT.sMetaTool.getToolWithStats(ToolsGT.PICKAXE, MT.Steel, MT.Steel);
					if (sOreProbeCase == 1) tPick.enchant(tLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
					if (sOreProbeCase == 2) tPick.enchant(tLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
					tPlayer.getInventory().setItem(0, tPick);
					tPlayer.getInventory().setSelectedSlot(0);
					sOreProbeDamageBefore = gregapi.item.multiitem.MultiItemTool.getToolDamage(tPick);
					O.println("[GT6-OREPROBE] кейс «" + ORE_CASES[sOreProbeCase] + "»: руда " + sOreProbeOre + " = " + tLevel.getBlockState(sOreProbeOre) + " кирка=" + tPick);
				} else if (tPhase == 10) {
					net.minecraft.world.level.GameType tOldMode = tPlayer.gameMode();
					tPlayer.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
					tPlayer.gameMode.destroyBlock(sOreProbeOre);
					tPlayer.setGameMode(tOldMode);
				} else if (tPhase == 30) {
					StringBuilder tDrops = new StringBuilder();
					StringBuilder tJudge = new StringBuilder();
					for (net.minecraft.world.entity.item.ItemEntity tD : tLevel.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(sOreProbeOre).inflate(4))) {
						tDrops.append(tD.getItem()).append("; ");
						tJudge.append(ST.regName(tD.getItem())).append(" x").append(tD.getItem().getCount()).append(";");
						tD.discard();
					}
					String tJ = tJudge.toString();
					boolean tPass = sOreProbeCase == 0 ? tJ.contains("ore.broken") : sOreProbeCase == 1 ? tJ.contains("ore.normal") : tJ.toLowerCase().contains("raw");
					long tDamageAfter = gregapi.item.multiitem.MultiItemTool.getToolDamage(tPlayer.getMainHandItem());
					O.println("[GT6-OREPROBE] кейс «" + ORE_CASES[sOreProbeCase] + "»: блок после=" + tLevel.getBlockState(sOreProbeOre) + "  дроп=" + (tDrops.length() == 0 ? "ПУСТО" : tDrops) + "  (regNames=" + tJ + ")  износ=" + (tDamageAfter - sOreProbeDamageBefore) + "  => " + (tPass ? "PASS" : "FAIL"));
					sOreProbeCase++;
				}
			// [BUG-024] взрыв-кейсы: ванильный TNT (реальный ServerExplosion: onExplosionHit → loot-дроп с EXPLOSION_RADIUS → onBlockExploded-мост)
			} else if (sOreProbeTick == 400) {
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				// прогон 4 доказал: с ДЕФОЛТНЫМ tnt_explosion_drop_decay=false движок не кладёт EXPLOSION_RADIUS →
				// дроп 100% (как у ванильных блоков, 8/8) — корректно. Гейт 1/размер судим В decay-режиме:
				tLevel.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLOSION_DROP_DECAY, Boolean.TRUE, aServer);
				sOreProbeOre = tPlayer.blockPosition().offset(10, 1, 10);
				sOreProbeExplOres = 0;
				tLevel.setBlock(sOreProbeOre.below(), Blocks.STONE.defaultBlockState(), 3); // опора под TNT
				for (int i = 0; i < 8; i++) { // кольцо руд Fe вокруг будущего TNT
					net.minecraft.core.BlockPos tP = sOreProbeOre.offset(OFFX_RING[i], 0, OFFZ_RING[i]);
					if (((gregapi.block.prefixblock.PrefixBlock)BlocksGT.ore).placeBlock(tLevel, tP.getX(), tP.getY(), tP.getZ(), SIDE_UNKNOWN, MT.Fe.mID, null, T, T)) sOreProbeExplOres++;
				}
				net.minecraft.world.entity.item.PrimedTnt tTnt = new net.minecraft.world.entity.item.PrimedTnt(tLevel, sOreProbeOre.getX()+0.5, sOreProbeOre.getY(), sOreProbeOre.getZ()+0.5, null);
				tTnt.setFuse(20);
				tLevel.addFreshEntity(tTnt);
				O.println("[GT6-OREPROBE] кейс «TNT(decay) vs кольцо 8 руд Fe»: руд поставлено=" + sOreProbeExplOres + " TNT fuse=20 центр=" + sOreProbeOre);
			} else if (sOreProbeTick == 480) {
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				int tDestroyed = 0;
				for (int i = 0; i < 8; i++) if (tLevel.getBlockState(sOreProbeOre.offset(OFFX_RING[i], 0, OFFZ_RING[i])).isAir()) tDestroyed++;
				int tDropped = 0; StringBuilder tDrops = new StringBuilder();
				for (net.minecraft.world.entity.item.ItemEntity tD : tLevel.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(sOreProbeOre).inflate(8))) {
					String tR = ST.regName(tD.getItem());
					if (tR != null && tR.contains("ore.")) {tDropped += tD.getItem().getCount(); tDrops.append(tR).append(" x").append(tD.getItem().getCount()).append(";");}
					tD.discard();
				}
				// 1.7.10-шанс = 1/размер (TNT=4 → 25%) в decay-режиме; 100%-дроп = гейт EXPLOSION_RADIUS не работает
				tLevel.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.TNT_EXPLOSION_DROP_DECAY, Boolean.FALSE, aServer); // вернуть дефолт
				O.println("[GT6-OREPROBE] кейс «TNT(decay) vs руды»: снесено=" + tDestroyed + "/8  дроп-руд=" + tDropped + " (" + tDrops + ")  => " + (tDestroyed > 0 && tDropped < tDestroyed ? "PASS (снос есть, дроп с шансом ~1/4, не 100%)" : "FAIL"));
			} else if (sOreProbeTick == 500) {
				// цепная детонация: руда EXPLOSIVE-материала (Phosphorus, mCanExplode руды = T) + TNT вплотную.
				// Прогоны 4-5: взрыв руду не доставал (рельеф/вода на позиции) — строим арену: пол 3×3 + куб воздуха.
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				sOreProbeOre = tPlayer.blockPosition().offset(14, 1, 14);
				for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 1; dz++) {
					tLevel.setBlock(sOreProbeOre.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
					for (int dy = 0; dy <= 2; dy++) tLevel.setBlock(sOreProbeOre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
				}
				boolean tPlaced = ((gregapi.block.prefixblock.PrefixBlock)BlocksGT.ore).placeBlock(tLevel, sOreProbeOre.getX(), sOreProbeOre.getY(), sOreProbeOre.getZ(), SIDE_UNKNOWN, MT.Phosphorus.mID, null, T, T);
				net.minecraft.world.entity.item.PrimedTnt tTnt = new net.minecraft.world.entity.item.PrimedTnt(tLevel, sOreProbeOre.getX()+1.5, sOreProbeOre.getY(), sOreProbeOre.getZ()+0.5, null);
				tTnt.setFuse(20);
				tLevel.addFreshEntity(tTnt);
				// изолированный шов: взрывостойкость EXPLOSIVE-руды обязана быть 0 (гейт :464)
				O.println("[GT6-OREPROBE] кейс «TNT vs фосфорная руда (EXPLOSIVE)»: руда=" + tPlaced + " " + tLevel.getBlockState(sOreProbeOre)
					+ " resistance=" + ((gregapi.block.prefixblock.PrefixBlock)BlocksGT.ore).getExplosionResistance(tLevel.getBlockState(sOreProbeOre), tLevel, sOreProbeOre, null));
			} else if (sOreProbeTick == 525) {
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				int tTntAlive = tLevel.getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class, new net.minecraft.world.phys.AABB(sOreProbeOre).inflate(6)).size();
				O.println("[GT6-OREPROBE-DIAG] t+25 после спавна TNT (fuse 20): TNT-entity рядом=" + tTntAlive + " (0 = взорвался) руда=" + tLevel.getBlockState(sOreProbeOre) + " опора TNT=" + tLevel.getBlockState(sOreProbeOre.offset(1, -1, 0)));
			} else if (sOreProbeTick == 580) {
				net.minecraft.server.level.ServerPlayer tPlayer = aServer.getPlayerList().getPlayers().get(0);
				net.minecraft.server.level.ServerLevel tLevel = tPlayer.level();
				for (net.minecraft.world.entity.item.ItemEntity tD : tLevel.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(sOreProbeOre).inflate(8))) tD.discard();
				O.println("[GT6-OREPROBE] кейс «фосфорная руда»: блок после=" + tLevel.getBlockState(sOreProbeOre) + " (air => снесена; вызов моста см. DIAG выше)");
				O.println("========== [GT6-OREPROBE] DONE ==========");
			}
		} catch (Throwable e) {O.println("[GT6-OREPROBE] EXC " + e); e.printStackTrace(O); sOreProbeTick = 9999;}
	}

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
				if (gregapi.data.CS.probeFlag("gt6bugverify.flag")) gt6BugVerifyTick(aEvent.getServer()); // [GT6-BUGVERIFY] временная механическая проба фиксов BUG-001..010 — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6leafprobe.flag")) gt6LeafProbeTick(aEvent.getServer()); // [GT6-LEAFPROBE] временная проба BUG-005 РЕАЛЬНЫМ путём (дерево движком + рубка кодом топора) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6craftprobe.flag")) gt6CraftProbeTick(aEvent.getServer()); // [GT6-CRAFTPROBE] временная проба BUG-002 РЕАЛЬНЫМ путём (палка+кремень в живых крафт-слотах) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6attackprobe.flag")) gt6AttackProbeTick(aEvent.getServer()); // [GT6-ATTACKPROBE] временная проба BUG-003 РЕАЛЬНЫМ путём (клиентская атака ГТ-ножом по овце) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6flowerprobe.flag")) gt6FlowerProbeTick(aEvent.getServer()); // [GT6-FLOWERPROBE] временная проба BUG-008 РЕАЛЬНЫМ путём (слом мака SURVIVAL-каналом игрока) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6slabprobe.flag")) gt6SlabProbeTick(aEvent.getServer()); // [GT6-SLABPROBE] временная проба BUG-010 (клиентские квады модели слэба) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6anvilprobe.flag")) gt6AnvilProbeTick(aEvent.getServer()); // [GT6-ANVILPROBE] временная проба BUG-011 (наковальня реальным useOn) — снять при уборке фазы
				if (gregapi.data.CS.probeFlag("gt6oreprobe.flag")) gt6OreProbeTick(aEvent.getServer()); // [GT6-OREPROBE] временная проба BUG-020 (твёрдость/добыча руды PrefixBlock реальным путём) — снять при уборке фазы
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
								}
								// EVENTS foreign-gated+impossible-1:1 (Betweenlands отсутствует; IFluidHandlerItem , capability-система переработана): fluid-in-item в neo
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
				UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.WEAKNESS      ,  300, 2, F);
				UT.Entities.applyPotion(aPlayer, net.minecraft.world.effect.MobEffects.MINING_FATIGUE, 1200, 2, F);
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
		// [GT6-FLOWERPROBE-DIAG] временная диагностика BUG-008 (гейт как у пробы) — снять при уборке фазы
		boolean tFlowerDiag = gregapi.data.CS.probeFlag("gt6flowerprobe.flag") && aEvent.getState().getBlock() == Blocks.POPPY;
		if (tFlowerDiag) gregapi.data.CS.OUT.println("[GT6-FLOWERPROBE-DIAG] harvest ВХОД: дропов-сущностей=" + aEvent.getDrops().size() + " canceled=" + aEvent.isCanceled());
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

				// EVENTS functional-adapted (реальный подбор — через ItemEntityPickupEvent.Post handler; pre-симуляция «спросить моды» не нужна): gregapi.util.ST (не мой файл) не содержит метода
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
		if (tFlowerDiag) gregapi.data.CS.OUT.println("[GT6-FLOWERPROBE-DIAG] harvest ВЫХОД: стеков=" + aDropStacks + " дропов-сущностей=" + aEvent.getDrops().size());
	}
	
	// EntityJoinLevelEvent.entity (1.7.10 через словарь-переименование) — приватное поле в neo, getEntity() (сверено, EntityEvent.java).
	// ItemEntity.getEntityItem/setEntityItemStack/isDead/setDead (1.7.10) — neo: getItem/setItem/isRemoved/discard (сверено, ItemEntity.java/Entity.java).
	// World.isClientSide() → Level.isClientSide(). World.findNearestEntityWithinAABB(Class,AABB,Entity) удалён — реальный neo-путь:
	// Level.getEntities(Entity,AABB,Predicate) + isInstance-проверка по классу (сверено, EntityGetter.java).
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntitySpawningEvent(EntityJoinLevelEvent aEvent) {
		if (aEvent.getEntity() instanceof ItemEntity && !aEvent.getEntity().level().isClientSide()) {
			// [GT6-FLOWERPROBE-DIAG] временная диагностика BUG-008 — снять при уборке фазы
			boolean tFlowerDiag = gregapi.data.CS.probeFlag("gt6flowerprobe.flag") && ST.item_(((ItemEntity)aEvent.getEntity()).getItem()) == Items.POPPY.asItem();
			if (tFlowerDiag) gregapi.data.CS.OUT.println("[GT6-FLOWERPROBE-DIAG] entityJoin ВХОД: item=" + ((ItemEntity)aEvent.getEntity()).getItem() + " canceled=" + aEvent.isCanceled());
			ItemStack aStack = ST.update(OM.get(((ItemEntity)aEvent.getEntity()).getItem()), aEvent.getEntity());
			if (tFlowerDiag) gregapi.data.CS.OUT.println("[GT6-FLOWERPROBE-DIAG] entityJoin после OM/ST.update: aStack=" + aStack + " valid=" + ST.valid(aStack));
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
}
