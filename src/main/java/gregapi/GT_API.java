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
import net.neoforged.api.distmarker.OnlyIn;
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
 * F12 (жизненный цикл, decisions/F12-registration-lifecycle.md): три родных FML-мода Грегориуса
 * (GAPI/GAPI_POST/GT) переносятся как три отдельных neo-{@code @Mod} на своих местах — этот класс
 * остаётся точкой входа мода GAPI. Оригинальная FML-строка {@code dependencies=} несла как
 * структурный порядок (GAPI грузится перед GAPI_POST — обязателен для 3-модовой связки GT6), так и
 * ~150 мягких order-хинтов для внешних совместимых модов (compat-mirror, зона F10). Внешние
 * order-хинты в {@code depends()} не перенесены — они не влияют на компиляцию/жизненный цикл самого
 * GT6 и относятся к F10 (compat-mirror), когда те моды реально появятся в дереве как neo-цели.
 * PORT-TODO(F12-depends, при подключении compat-mirror-модов возможно потребуется добавить их сюда
 * как мягкие order-хинты).
 *
 * УЛИКА R7 (исправлено): {@code depends()} ждёт СЫРОЙ {@code String[]} modId, без парсера префиксов
 * старого FML (fml-decompiled {@code net/neoforged/fml/common/Mod.java:16},
 * {@code FMLJavaModLanguageProvider.java:33,67-70} — строка вида {@code "required-before:"+modId} не
 * находится в загруженном списке модов и приводит к тому, что весь entrypoint-класс отфильтровывается
 * из загрузки). Передан чистый {@code ModIDs.GAPI_POST}.
 *
 * УЛИКА R8 (доработка): {@code depends()} здесь фильтрует entrypoint только по НАЛИЧИЮ modId
 * (fml-decompiled {@code FMLJavaModLanguageProvider.java:33}) — он НЕ задаёт порядок загрузки
 * (структурный факт "GAPI перед GAPI_POST" в {@code depends()} НЕ выражается). Реальный порядок
 * задан {@code ModSorter} (fml-decompiled {@code net/neoforged/fml/loading/ModSorter.java:194-208})
 * из графа {@code [[dependencies.gregapi]]}/{@code [[dependencies.gregapi_post]]} с полем
 * {@code ordering="BEFORE"/"AFTER"} в {@code src/main/templates/META-INF/neoforge.mods.toml} —
 * см. комментарий там же. {@code depends()} здесь остаётся как НЕЗАВИСИМЫЙ REQUIRED-гейт
 * (не грузить entrypoint, если GAPI_POST отсутствует в списке модов), а не как источник порядка.
 */
@Mod(value = ModIDs.GAPI, depends = {ModIDs.GAPI_POST})
public class GT_API extends Abstract_Mod {
	/**
	 * Замена {@code @SidedProxy}: neo не имеет annotation-диспетчера сторон, поэтому сторона выбирается
	 * напрямую по {@link FMLEnvironment#getDist()} (сверено: {@code DistExecutor} в этой версии neo не
	 * существует — decisions/F12-registration-lifecycle.md §7).
	 */
	public static GT_API_Proxy api_proxy = FMLEnvironment.getDist().isClient() ? new GT_API_Proxy_Client() : new GT_API_Proxy_Server();

	public static final Collection<Map<ItemStackContainer, ?>> STACKMAPS = new ArrayListNoNulls<>();

	/** Used to register Icons. It is not necessary to make those into Lists */
	public static Set<Runnable> sBlockIconload = new HashSetNoNulls<>(), sItemIconload = new HashSetNoNulls<>();
	/** The Icon Registers from Blocks and Items. They will get set right before the corresponding Icon Load Phase as executed in the Runnable List above. */
	// PORT-TODO(F3, baked-рендер клиента): 1.7.10 net.minecraft.client.renderer.texture.IIconRegister
	// удалён из движка целиком (атлас-стежка теперь baked-модели, не immediate-mode Icon-регистрация).
	// Тот же класс проблемы, что gregapi/render/TextureSet.java registerIcons(Object) (уже переведено) —
	// поле типизировано как Object (та же деградация), консьюмеры (BI/Textures.java) уже переведены на Identifier.
	@OnlyIn(Dist.CLIENT)
	public static Object sBlockIcons, sItemIcons;

	/**
	 * Централизованный мост регистрации F12: ЕДИНСТВЕННАЯ точка, через которую весь мод регистрирует
	 * Item/Block в NeoForge DeferredRegister (замена разрозненных прямых DeferredRegister-вызовов,
	 * найденных ревизией R3 в GT6_Main/GT_API_Proxy/ST — decisions/F12-registration-lifecycle.md).
	 * GT6 создаёт Item/Block ЗАРАНЕЕ ({@code new SomeItem()}), затем в оригинале регистрировал уже
	 * готовый экземпляр ({@code GameRegistry.registerItem(item, name)}). DeferredRegister ожидает
	 * Supplier; оборачиваем уже созданный экземпляр в Supplier, возвращающий его же — при однократной
	 * загрузке мода (без hot-reload реестров) это эквивалентно оригинальному поведению.
	 */
	public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems (ModIDs.GAPI);
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModIDs.GAPI);

	/**
	 * F12: мод-шина, сохранённая из конструктора, чтобы лениво созданные под-неймспейсы могли
	 * подписаться на {@code RegisterEvent} (см. {@link #itemsFor(String)}).
	 */
	private static IEventBus sModBus = null;
	/**
	 * F12: по одному {@code DeferredRegister.Items} на неймспейс-владелец. GT6 позволяет создавать
	 * Item под чужим modId (аддоны через {@code PrefixItem}), а {@code DeferredRegister} привязан к
	 * одному неймспейсу — поэтому центр держит карту неймспейс→реестр. Это по-прежнему ОДИН центр
	 * (весь мод сюда обращается), просто с учётом неймспейса, как было в {@code GameRegistry.registerItem(item,name,modId)}.
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

	/** F12/R3-мост, вызывается из {@code gregapi.util.ST.register(Item, String)}: регистрация под
	 *  неймспейсом GAPI (был прямой выдуманный {@code DeferredRegister.registerItem(...)}). */
	public static DeferredItem<Item> registerItem(Item aItem, String aRegistryName) {
		return registerItem(aItem, aRegistryName, ModIDs.GAPI);
	}

	/** F12/R3-мост: регистрация Item под неймспейсом владельца {@code aModIDOwner} (замена выдуманного
	 *  3-арг {@code DeferredRegister.registerItem(item, name, modId)} из {@code PrefixItem}/{@code ItemFluidDisplay};
	 *  соответствует оригиналу {@code GameRegistry.registerItem(item, name, modId)}). Централизовано —
	 *  весь мод регистрирует Item только через этот метод. */
	public static DeferredItem<Item> registerItem(Item aItem, String aRegistryName, String aModIDOwner) {
		return itemsFor(aModIDOwner).register(aRegistryName, () -> aItem);
	}

	/** F12/R3-мост, вызывается из {@code gregapi.util.ST.register(Block, String, Class)} (был прямой
	 *  выдуманный {@code DeferredRegister.registerBlock(...)}). Пара Block+BlockItem регистрируется под
	 *  одним и тем же именем — как было в оригинальном {@code GameRegistry.registerBlock(Block, Class, String)}. */
	public static DeferredBlock<Block> registerBlock(Block aBlock, String aRegistryName, Class<? extends BlockItem> aItemClass) {
		DeferredBlock<Block> rBlock = BLOCKS.register(aRegistryName, () -> aBlock);
		ITEMS.register(aRegistryName, () -> (BlockItem)UT.Reflection.callConstructor(aItemClass, 0, null, T, aBlock));
		return rBlock;
	}

	private LoggerPlayerActivity mPlayerLogger;

	@SuppressWarnings("unchecked")
	public GT_API(IEventBus aModBus) {
		GAPI = this;
		
		if (!MD.ENCHIRIDION.mLoaded) MD.MaCu.mLoaded = F;
		
		// A bunch of Code that is there to statically initialize the Database in the right order and without crashes.
		MT.init();
		BI.BAROMETER.toString();
		OP.ore.toString();
		
		// Make sure Icons are initialized.
		Textures.BlockIcons.VOID.toString();
		Textures.ItemIcons .VOID.toString();
		ErrorRenderer.INSTANCE.toString();
		
		// Guess what, I got a random Crash from one of those not being classloaded...
		UT.Entities.class.toString();
		IMTE_CanConnectRedstone.class.toString();
		
		
		try {
			DW = new DummyWorld();
		} catch(Throwable e) {
			ERR.println("======================================================================================================");
			ERR.println("WARNING, DUMMY WORLD COULD NOT BE CREATED, SOME RECIPE RELATED THINGS MAY NOT FUNCTION PROPERLY NOW!!!");
			ERR.println("======================================================================================================");
			e.printStackTrace(ERR);
			ERR.println("======================================================================================================");
		}
		
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
		
		// F12 boot-timing: блок vanilla-ore-target'ов (ST.make(Blocks.X) = ItemStack) ПЕРЕНЕСЁН в onLoad
		// (FMLCommonSetupEvent), т.к. в @Mod-конструкции neo ещё не привязал Holder.components предметов
		// (крах "Components not bound yet", Holder.java:273). Порядок «registered first» сохранён — блок в
		// САМОМ НАЧАЛЕ onLoad, до остального data-init. См. STATE.md «СИСТЕМНАЯ НАХОДКА F12» / decisions/F12.
		
		// Fixing missing Container Items.
		// PORT-TODO(F12, item-container-runtime-mutator): Item.setContainerItem(Item) (1.7.10 runtime
		// crafting-remainder mutator) удалён — neo's Item.craftingRemainingItem является private final,
		// задаётся ТОЛЬКО через Item.Properties.craftRemainder(...) на регистрации ванильного Item
		// (BuiltInRegistries), ретроактивная мутация уже зарегистрированных Items.MUSHROOM_STEW/POTION/
		// EXPERIENCE_BOTTLE недостижима из мод-кода. Не найдено ни в одном из 3 корней референса —
		// деградация до no-op. Оригинал: mushroom_stew->bowl, potionitem->glass_bottle, experience_bottle->glass_bottle.
		
		// Fixing Max Stacksizes that don't make sense.
		ST.forceProperMaxStacksizes();
		
		// Fixing some Adventure Mode things.
		// PORT-TODO(F12, adventure-mode-harvest): Block.setHarvestLevel(String,int) (1.7.10 runtime
		// harvest-tier mutator) удалён из движка целиком, не переименован — harvest-tier в neo задаётся
		// НЕИЗМЕНЯЕМО при регистрации блока (BlockBehaviour.Properties) и мешает через data-driven теги
		// (BlockTags.MINEABLE_WITH_AXE/MINEABLE_WITH_PICKAXE, датапак, не Java-runtime API). Не найдено
		// ни в одном из 3 корней референса — деградация до no-op (см. также reflection-хак ниже).

		try {
			// The Access Transformer should make this work
			Material.tnt.setAdventureModeExempt();
		} catch(Throwable e) {
			UT.Reflection.callMethod(Material.tnt, new String[] {"func_85158_p", "setAdventureModeExempt"}, T, F, F);
			e.printStackTrace(ERR);
		}

		// PORT-TODO(F12, adventure-mode-harvest): reflection-хак "AxeItem/ItemPickaxe.field_150917_c/
		// field_150915_c" (приватный статический Set<Block> "эффективных" блоков 1.7.10) не имеет 1:1
		// аналога — инструмент-эффективность в neo тоже data-driven (те же BlockTags.MINEABLE_WITH_*),
		// подобных изменяемых static-полей на Item-классах в декомпиле нет. Не найдено ни в одном из
		// 3 корней референса — деградация до no-op.

		// F12: центральные DeferredRegister этого мода — на мод-шину; шину запоминаем, чтобы лениво
		// созданные под-неймспейсы (itemsFor) тоже успели подписаться на RegisterEvent.
		sModBus = aModBus;
		ITEMS .register(aModBus);
		BLOCKS.register(aModBus);
		// F6: центральный ворлдген-переходник (Feature/PlacedFeature/BiomeModifier) — тот же мод-бас,
		// единая точка подписки (decisions/F6-worldgen.md, gregapi/worldgen/GT6WorldgenFeature.java).
		gregapi.worldgen.GT6WorldgenFeature.register(aModBus);
		// ENCHANT: центральный переходник кастомных чар-эффектов — тот же мод-бас, единая точка подписки
		// (gregapi/enchants/EnchantsGT6.java; закрывает стык F6↔ENCHANT wiring, метка `ENCHANT, регистрация`).
		gregapi.enchants.EnchantsGT6.register(aModBus);
		// F5: центральные DeferredRegister жидкостей (FluidType+Fluid) — тот же мод-бас, единая точка
		// подписки (decisions/F5-fluids.md §3, gregapi/fluid/FluidGT.java; закрывает прежний долг F12↔F5 wiring).
		gregapi.fluid.FluidGT.FLUID_TYPES.register(aModBus);
		gregapi.fluid.FluidGT.FLUIDS.register(aModBus);
		// F-attachment: центральный DeferredRegister Entity-attachment-типов (EntityFoodTracker) — тот же
		// мод-бас, единая точка подписки (gregapi/player/EntityFoodTracker.java; замена 1.7.10
		// IExtendedEntityProperties, ни один другой файл эту регистрацию не дублирует).
		gregapi.player.EntityFoodTracker.ATTACHMENTS.register(aModBus);
		// F11: центральный крафт-верстак-диспетчер (CustomRecipe SERIALIZERS) — тот же мод-бас, единая точка
		// подписки (decisions/F11-crafting-recipe.md §7, gregapi/recipes/GT6CraftingDispatcher.java; закрывает
		// прежний долг F12↔F11 wiring).
		GT6CraftingDispatcher.register(aModBus);
		// F14: центральный MenuType GUI (ContainerCommon) — тот же мод-бас, единая точка подписки (decisions/F14-gui-menu.md)
		gregapi.gui.ContainerCommon.register(aModBus);

		// F12: замена annotation-диспетчера @Mod.EventHandler — подписка фаз на мод-шину напрямую.
		// GT6-трёхфазный контракт (Pre/Init/Post) сохранён 1:1 поверх родных событий жизненного цикла neo:
		// PreInit -> FMLConstructModEvent; Init -> FMLCommonSetupEvent; PostInit -> FMLLoadCompleteEvent
		// (decisions/F12-registration-lifecycle.md §4).
		aModBus.addListener(this::onPreLoad);
		aModBus.addListener(this::onLoad);
		aModBus.addListener(this::onPostLoad);

		// Серверные фазы GT6 (Abstract_Mod уже на родных событиях neo) — на игровой шине, не на мод-шине.
		NeoForge.EVENT_BUS.addListener(this::onServerStarting);
		NeoForge.EVENT_BUS.addListener(this::onServerStarted);
		NeoForge.EVENT_BUS.addListener(this::onServerStopping);
		NeoForge.EVENT_BUS.addListener(this::onServerStopped);
	}

	/**
	 * PreInit. Замена {@code @Mod.EventHandler onPreLoad(FMLPreInitializationEvent)}: подписан в
	 * конструкторе на {@link FMLConstructModEvent} (мод-шина). Строит GT6-шим {@code FMLPreInitializationEvent}
	 * (носитель фазы, gregapi.api) и передаёт его в {@code Abstract_Mod.onModPreInit(...)} — тело фазы
	 * (onModPreInit2 и далее) остаётся байт-в-байт как в оригинале.
	 * PORT-TODO(F12-timing, decisions/F12-registration-lifecycle.md §7): не проверено, что регистрация
	 * контента внутри тела PreInit (до RegisterEvent) гарантированно не опаздывает относительно
	 * FMLConstructModEvent на всех сборках; сверить при первой реальной регистрации через ITEMS/BLOCKS.
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
	 * Init. Замена {@code @Mod.EventHandler onLoad(FMLInitializationEvent)}: подписан в конструкторе на
	 * {@link FMLCommonSetupEvent} (мод-шина).
	 */
	public void onLoad(FMLCommonSetupEvent aModEvent) {
		// F12 boot-timing (ПЕРЕНЕСЕНО из @Mod-конструктора): vanilla-ore-target'ы создают ItemStack (ST.make(Blocks.X)),
		// что невозможно в конструкции (Holder.components не привязаны) — здесь (FMLCommonSetupEvent, после регистрации
		// и привязки) можно. Порядок «registered first» цел: этот блок ПЕРВЫЙ в onLoad, до конфиг-цикла и onModInit.
		// It is VERY important that those are registered first. Otherwise GregTech would output its own Storage Blocks.
		// F12: REMAP-RULES.md §C/§C-bis блок-флэттен (данные, не поведение) — Blocks.<snake_case> удалены,
		// заменены реальными UPPER_SNAKE-константами neo; RedSand и "smooth double stone slab" (meta 8) → RED_SAND/SMOOTH_STONE.
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
	}
	
	// PostInit: подписан в конструкторе на FMLLoadCompleteEvent (мод-шина) — родное neo-событие
	// заменяет старую FML 1.7.10 сложность вокруг loadComplete (комментарий оракула выше снят вместе
	// с @Mod.EventHandler-диспетчером, который и был источником проблемы).
	public void onPostLoad(FMLLoadCompleteEvent aModEvent) {onModPostInit(new FMLPostInitializationEvent());}

	@Override public String getModID() {return MD.GAPI.mID;}
	@Override public String getModName() {return MD.GAPI.mName;}
	@Override public String getModNameForLog() {return "GT_API";}
	@Override public Abstract_Proxy getProxy() {return api_proxy;}

	// Серверные фазы — подписаны в конструкторе на NeoForge.EVENT_BUS (игровая шина), не на мод-шину.
	public void onServerStarting  (ServerStartingEvent aEvent) {onModServerStarting(aEvent);}
	public void onServerStarted   (ServerStartedEvent  aEvent) {onModServerStarted(aEvent);}
	public void onServerStopping  (ServerStoppingEvent aEvent) {onModServerStopping(aEvent);}
	public void onServerStopped   (ServerStoppedEvent  aEvent) {onModServerStopped(aEvent);}

	@Override
	@SuppressWarnings({ "resource", "deprecation" })
	public void onModPreInit2(FMLPreInitializationEvent aEvent) {
		// neo-сигнатура сверена с fml-decompiled/net/neoforged/fml/InterModComms.java:27
		// (decisions/F12-registration-lifecycle.md §7 — вопрос закрыт).
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
		} catch (Throwable e) {/**/}
		
		tFile = new File(DirectoriesGT.LOGS, "oredict.log");
		if (!tFile.exists()) {try {tFile.createNewFile();} catch (Throwable e) {/**/}}
		try {
			tList = ((LogBuffer)ORD).mBufferedLog;
			ORD = new PrintStream(tFile);
			ORD.println("**********************************************************************");
			ORD.println("* This is the complete Log of the GregTech OreDictionary Handler     *");
			ORD.println("**********************************************************************");
			for (String tString : tList) ORD.println(tString);
		} catch (Throwable e) {/**/}
		
		if (ConfigsGT.GREGTECH.get("general", "LoggingPlayerActivity", !CODE_CLIENT)) {
			tFile = new File(DirectoriesGT.LOGS, "playeractivity_"+(System.currentTimeMillis()/60000)+".log");
			if (!tFile.exists()) {try {tFile.createNewFile();} catch (Throwable e) {/**/}}
			try {mPlayerLogger = new LoggerPlayerActivity(new PrintStream(tFile));} catch (Throwable e) {/**/}
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
		EMIT_EU_AS_RF                           = ConfigsGT.GREGTECH.get("general", "Emit_EU_as_RF_from_Blocks"        , F);
		NERFED_WOOD                             = ConfigsGT.GREGTECH.get("general", "WoodNeedsSawForCrafting"          , T);
		FORCE_GRAVEL_NO_FLINT                   = ConfigsGT.GREGTECH.get("general", "GravelWontDropFlint"              , F);
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
		
		if (ConfigsGT.GREGTECH.get("general", "disable_STDOUT"             , F)) System.out.close();
		if (ConfigsGT.GREGTECH.get("general", "disable_STDERR"             , F)) System.err.close();
		// PORT-TODO(F12, block-property-runtime-mutator): Block.setHardness(float)/setResistance(float)
		// (1.7.10 runtime мутаторы) удалены — neo BlockBehaviour.Properties.strength(destroyTime,
		// explosionResistance) неизменяема, задаётся ТОЛЬКО при регистрации блока; Blocks.SPAWNER уже
		// зарегистрирован ванилью, ретроактивная мутация недостижима. Не найдено ни в одном из 3 корней
		// референса — деградация до no-op.
		if (ConfigsGT.GREGTECH.get("general", "hardermobspawners"          , T)) {/**/}
		if (ConfigsGT.GREGTECH.get("general", "blastresistantmobspawners"  , T)) {/**/} else {/**/}
		
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
		// PORT-TODO(F12-entity, decisions/F12-registration-lifecycle.md): нет ADR на TILEENTITY-TYPE
		// адаптер (BlockEntityType.Builder требует реальный BlockEntitySupplier + набор valid-блоков,
		// которых PrefixBlockTileEntity в текущем виде не предоставляет). Прежний вызов
		// (`DeferredRegister.registerTileEntity(Class, String)`) — выдуманный API, такого метода в
		// NeoForge DeferredRegister нет (сверено с neoforge-decompiled). Не выдумываю замену без ADR.
		// Creating and loading the Lang File.
		if (CODE_CLIENT) {
			tFile = new File(DirectoriesGT.MINECRAFT, "GregTech.lang");
			if (!tFile.exists()) tFile = new File(DirectoriesGT.MINECRAFT, "gregtech.lang");
			LanguageHandler.sLangFile = new ModConfigSpec(tFile);
			LanguageHandler.sUseFile = LanguageHandler.sLangFile.get("EnableLangFile", "UseThisFileAsLanguageFile", F).getBoolean(F);
		} else {
			sBlockIconload.clear();
			sBlockIconload = null;
			sItemIconload.clear();
			sItemIconload = null;
		}
		// Creating and loading the Unification Config.
		OreDictManager.INSTANCE.mUnificationConfig = new Config("Unification.cfg");
		// Initialising the Re-Registrations.
		new LoaderOreDictReRegistrations().run();
		// Register the Falling MetaBlock Entity.
		// PORT-TODO(F12-entity, decisions/F12-registration-lifecycle.md): та же граница, что и
		// registerTileEntity выше — ENTITY-TYPE адаптер (DeferredRegister.Entities.registerEntityType
		// требует EntityType.EntityFactory, совместимый с реальным конструктором PrefixBlockFallingEntity)
		// не разработан отдельным ADR. Прежний вызов (`DeferredRegister.registerModEntity(...)`) —
		// выдуманный API (1.7.10 GameRegistry.registerModEntity механически переименован в
		// DeferredRegister, которого там никогда не было).
		// Initialise Enchantments.
		new Enchantment_WerewolfDamage();
		new Enchantment_EnderDamage();
		new Enchantment_Radioactivity();
		new Enchantment_SlimeDamage();
		// Initialises the Fluid Display Item.
		// F12-lazy: конструкция предмета отложена в DeferredRegister-supplier (вызов на RegisterEvent — реестр открыт для
		// intrusive-holder); IL хранит supplier, mStack материализует лениво в рантайме. Было: IL.Display_Fluid.set(new ItemFluidDisplay()).
		IL.Display_Fluid.set(GT_API.ITEMS.register("gt.display.fluid", ItemFluidDisplay::new));
		// Initialises the Integrated Circuit Item.
		IL.Circuit_Selector.set(new ItemIntegratedCircuit());
		// Initialises the Empty Slot Marker Item.
		IL.Empty_Slot.set(new ItemEmptySlot());
		// Register the GUI Handler.
		// PORT-TODO(F7-gui, заменить старый Forge GUI-handler на реальный NeoForge menu/screen путь после сверки с референсом)
		// Fixing vanilla Oak Plank Slab Recipe.
		// F12: REMAP-RULES.md §C/§C-bis блок-флэттен (данные) — Blocks.OAK_PLANKS/wooden_slab (1.7.10 meta
		// 0/1/2=oak/spruce/birch) удалены, заменены реальными UPPER_SNAKE-константами neo per-species.
		CR.remove(ST.make(Blocks.OAK_PLANKS, 1, 0), ST.make(Blocks.SPRUCE_PLANKS, 1, 0), ST.make(Blocks.BIRCH_PLANKS, 1, 0));
		CR.shaped(ST.make(Blocks.OAK_SLAB, 6, 0), CR.NONE, "WWW", 'W', ST.make(Blocks.OAK_PLANKS, 1, 0));
		// Preventing a Water Dupe by registering this Recipe early so it won't be overridden
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.GLASS_BOTTLE, 1, 0), FL.Water.make(250), NF, ST.make(Items.POTION, 1, 0));
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.POTION, 1, 0), ST.make(Items.GLASS_BOTTLE, 1, 0));
		
		// F12: снят FML-хак принудительной перестановки GAPI в начало activeModList через reflection
		// (LoadController/ModList/ModContainer — внутренние классы FML 1.7.10, аналога в neo нет).
		// Его функцию ("GAPI грузится первым") теперь честно и декларативно даёт депенденси-граф
		// движка — @Mod(..., depends = {ModIDs.GAPI_POST}) выше в этом файле
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
	public void onModPostInit2(FMLPostInitializationEvent aEvent) {
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
		
		EnergyCompat.checkAvailabilities();
		ToolCompat.checkAvailabilities();
		ST.checkAvailabilities();
		
		OUT.println(getModNameForLog() + ": If the Loading Bar somehow Freezes at this Point, then you definetly ran out of Memory or permgenspace, look at the other Logs to confirm it.");
		OreDictManager.INSTANCE.onPostLoad();
		
		ICover tCover = new CoverRedstoneTorch();
		// F12: block-флэттен (данные) — Blocks.REDSTONE_TORCH/unlit_redstone_torch (1.7.10, два раздельных
		// блока lit/unlit) слиты в neo в ОДИН Blocks.REDSTONE_TORCH с BlockState-свойством "lit" (нет
		// отдельной unlit-константы); вторая регистрация становится тем же ключом — безвредный дубль,
		// не потеря данных (тот же tCover на тот же результирующий блок).
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
	}
	
	// В neo нет числовых ID блоков/предметов, поэтому нет и neo-аналога FMLModIdMappingEvent — метод
	// не подписан ни на одну шину (не вызывается автоматически, но остаётся 1:1 доступным вручную для
	// ICompat.onIDChanging(...), если понадобится на этапе рантайм-parity — см. javadoc
	// gregapi.api.FMLModIdMappingEvent).
	public void onIDChangingEvent(FMLModIdMappingEvent aEvent) {
		// Fixing missing Blocks caused by DragonAPI. The Issue is more complicated but it should fix some part of it.
		// PORT-TODO(F12, numeric-block-id-registry): DragonAPI-фикс завязан на числовой Block.blockRegistry
		// (getObjectById/addObject(int,...)) из Forge 1.7.10 — в NeoForge числовых ID блоков нет вовсе
		// (grep 3 корней референса: net.minecraft/net.neoforged — ни blockRegistry, ни int-based
		// addObject/getObjectById не существует), поэтому у этого куска нет и не может быть neo-1:1.
		// Метод сам по себе не подписан ни на одну шину (см. комментарий выше), это единственная живая
		// причина — остаток ниже (STACKMAPS-ремап + рассылка ICompat.onIDChanging) сохранён 1:1.

		OUT.println(getModNameForLog() + ": Remapping ItemStackMaps due to ID Map change. Those damn Items should have a consistent Hashcode, but noooo, ofcourse they break Basic Code Conventions! Thanks Forge and Mojang!");
		
		for (Map<ItemStackContainer, ?> tMap : STACKMAPS) UT.Code.reMap(tMap);
		for (ICompat tCompat : ICompat.COMPAT_CLASSES) try {tCompat.onIDChanging(aEvent);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
}
