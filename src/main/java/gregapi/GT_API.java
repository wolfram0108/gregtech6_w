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
 * F12-depends (условная заметка, не заглушка): order-хинты depends() к compat-mirror-модам не перенесены — они не
 * влияют на компиляцию/жизненный цикл GT6; ЕСЛИ те моды появятся в дереве как neo-цели, добавить сюда мягкие order-хинты.
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
@Mod(ModIDs.GAPI)
public class GT_API extends Abstract_Mod {
	/**
	 * Замена {@code @SidedProxy}: neo не имеет annotation-диспетчера сторон, поэтому сторона выбирается
	 * напрямую по {@link FMLEnvironment#getDist()} (сверено: {@code DistExecutor} в этой версии neo не
	 * существует — decisions/F12-registration-lifecycle.md §7).
	 *
	 * Присваивается в конструкторе ПОСЛЕ {@link MT#init()} (не инлайн в статик-инициализаторе поля):
	 * клиентский {@link GT_API_Proxy_Client} в своём конструкторе читает {@code MT.*.mRGBa}, а построение
	 * материалов идёт через {@link #STACKMAPS}. Инлайн-инициализация поля api_proxy шла бы в порядке
	 * class-init ДО STACKMAPS (объявлен ниже) → на клиенте MT тянулся раньше времени и падал NPE
	 * ("STACKMAPS is null"). Оригинальный {@code @SidedProxy} инъектировался FML при конструировании мода
	 * (после class-init) — тот же тайминг воспроизведён построением прокси в конструкторе.
	 */
	public static GT_API_Proxy api_proxy;

	public static final Collection<Map<ItemStackContainer, ?>> STACKMAPS = new ArrayListNoNulls<>();

	/** Used to register Icons. It is not necessary to make those into Lists */
	public static Set<Runnable> sBlockIconload = new HashSetNoNulls<>(), sItemIconload = new HashSetNoNulls<>();
	/** The Icon Registers from Blocks and Items. They will get set right before the corresponding Icon Load Phase as executed in the Runnable List above. */
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): 1.7.10 net.minecraft.client.renderer.texture.IIconRegister
	// удалён из движка целиком (атлас-стежка теперь baked-модели, не immediate-mode Icon-регистрация).
	// Тот же класс проблемы, что gregapi/render/TextureSet.java registerIcons(Object) (уже переведено) —
	// поле типизировано как Object (та же деградация), консьюмеры (BI/Textures.java) уже переведены на ResourceLocation.
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
	public static final DeferredRegister<net.minecraft.world.item.Item>  ITEMS  = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, ModIDs.GAPI);
	public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCKS, ModIDs.GAPI);

	/** F12-followup (subtype-meta): GT6 1.7.10 хранит ПОДТИП предмета в damage-value (getItemDamage 0..32767) — meta-предметы
	 *  (PrefixItem/MultiItem, maxDamage=0) держат тысячи подтипов на одном Item через meta. neo клампит setDamageValue к
	 *  [0,maxDamage] (IItemExtension.setDamage) → у maxDamage=0 ВСЯ meta схлопывается в 0 (все материал-стеки становятся
	 *  идентичны → унификация/рецепты/MTE ломаются). Переиспользовать DAMAGE нельзя: он же durability реальных предметов
	 *  (maxDamage>0). Централизованная адаптация — ОТДЕЛЬНЫЙ компонент подтипа; {@code ST.meta_} get/set идёт через него,
	 *  минуя кламп. Persistent (NBT ItemStack.CODEC) + network-synced. Ставится только при meta!=0 (meta-0 = без компонента,
	 *  стекуется с ванилла). */
	/** F12-followup (MTE-type-timing): единый placeholder-BlockEntityType всей MTE-иерархии. Создание BlockEntityType зовёт
	 *  createIntrusiveHolder → только на RegisterEvent<BlockEntityType> (размороженный реестр). Регистрируем supplier'ом
	 *  {@code TileEntityBase01Root.createType} (он создаёт+кэширует MTE_TYPE); прежде <clinit> создавал его лениво на
	 *  server-start → «Registry is already frozen» → рушился весь Loader_MultiTileEntities (cables/wires/pipes = 0). */
	public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, ModIDs.GAPI);
	public static final Object MTE_TYPE_HOLDER = BLOCK_ENTITIES.register("mte", gregapi.tileentity.base.TileEntityBase01Root::createType);

	/** F12-entity: центральный реестр EntityType контента gregapi — тот же приём, что ITEMS/BLOCKS/BLOCK_ENTITIES выше
	 *  (и что gregtech.entities.EntitiesGT у своих стрел). Заменяет удалённый 1.7.10
	 *  {@code EntityRegistry.registerModEntity} (оригинал GT_API.java:722). */
	public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ModIDs.GAPI);

	/** F12-entity: падающий мета-блок. Параметры 1:1 из оригинала
	 *  {@code registerModEntity(PrefixBlockFallingEntity.class, "gt.MetaBlockFallingEntity", 0, this, 160, 1, T)}:
	 *  trackingRange 160 блоков = 10 чанков ({@code clientTrackingRange}), updateFrequency 1 ({@code updateInterval}).
	 *  Габарит — как у ванильного FALLING_BLOCK (0.98×0.98, {@code EntityType.java:492}), от которого 1.7.10-класс
	 *  наследовался. Имя реестра из «gt.MetaBlockFallingEntity» приведено к lowercase (neo ResourceLocation запрещает
	 *  заглавные) — тот же приём, что у {@code EntitiesGT}. */
	public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.entity.EntityType<gregapi.block.prefixblock.PrefixBlockFallingEntity>> METABLOCK_FALLING =
		ENTITIES.register("gt_metablockfallingentity", () -> net.minecraft.world.entity.EntityType.Builder.<gregapi.block.prefixblock.PrefixBlockFallingEntity>of(gregapi.block.prefixblock.PrefixBlockFallingEntity::new, net.minecraft.world.entity.MobCategory.MISC)
			.sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(1)
			.build("gt_metablockfallingentity"));

	/** BUG-113: свои звуки мода. В 1.7.10 звук адресовался ИМЕНЕМ, а объявлялся только в ассетах
	 *  ({@code assets/gregapi/sounds.json}) — никакой регистрации не требовалось, движок брал запись по имени.
	 *  В neo имя обязано иметь {@code SoundEvent} в реестре, иначе {@code Registry.getValue} отдаёт null и звук
	 *  молча не играется (путь проигрывания — {@code UT.Sounds.SoundWithLocation.play}). Ключи берём ИЗ ТОГО ЖЕ
	 *  ФАЙЛА, что и 1.7.10, — список звуков не дублируется в коде и переживает пополнение ассетов. */
	public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, ModIDs.GAPI);
	static {
		for (String tKey : soundKeysFromAssets()) SOUND_EVENTS.register(tKey, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ModIDs.GAPI, tKey)));
	}
	/** Имена звуков, объявленных модом в {@code assets/<namespace>/sounds.json} — единственный источник истины.
	 *  ⛔ ЧИТАТЬ ЧЕРЕЗ CLASSLOADER НЕЛЬЗЯ: в dev-среде ресурсы лежат в каталоге и {@code getResourceAsStream}
	 *  их отдаёт, а в собранном jar мод грузится модульным загрузчиком FML, ресурс не выдаётся, список
	 *  оказывается пустым — и звуки молча не регистрируются. Ровно этим отличался запуск из исходников
	 *  (звук был) от запуска с jar (звука не было). Штатный путь FML к файлам СВОЕГО мода — ModList/ModFile,
	 *  он одинаков в обеих средах; classloader остаётся запасным. */
	private static java.util.List<String> soundKeysFromAssets() {
		String tPath = "assets/" + ModIDs.GAPI + "/sounds.json";
		java.util.List<String> rKeys = java.util.List.of();
		try (java.io.InputStream tIn = GT_API.class.getResourceAsStream("/" + tPath)) {
			if (tIn != null) rKeys = soundKeysFrom(tIn);
		} catch (Throwable e) {/* запасной путь ниже */}
		// запасной путь — там, где физически лежит сам класс: каталог в dev-среде, jar в поставке
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
		} catch (Throwable e) {/* звуков не будет — это видно по строке ниже */}
		OUT.println("GT6 sounds: объявлено в " + tPath + " и зарегистрировано " + rKeys.size() + " звуков " + rKeys);
		return rKeys;
	}
	private static java.util.List<String> soundKeysFrom(java.io.InputStream aIn) throws java.io.IOException {
		try (java.io.InputStream tIn = aIn) {
			com.google.gson.JsonObject tJson = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(tIn, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			return java.util.List.copyOf(tJson.keySet());
		}
	}

	// F8/F1: компонентов SUBTYPE и ZEROSIZE (и самого реестра DATA_COMPONENT_TYPE) в 1.20.1 не существует.
	// Подтип предмета вернулся в damage — центр `ST.meta_` (damage-канал 1.7.10 здесь полностью жив,
	// IForgeItem.java:435-438,472-475); маркер size-0-катализатора вернулся в NBT стека — центр `ST.size_`
	// (ключ CS.NBT_ZEROSIZE). Оба канала — те же, что в оригинале, поэтому отдельных сущностей не нужно.

	/** F1/F12/F16 item-model сепарация: GT6-предметы делали OreDict-данные+рецепты (ST.make = стек себя) В КОНСТРУКТОРЕ, но
	 *  neo конструирует предмет @RegisterEvent (реестр открыт для intrusive-holder), а стеки можно только @пост-freeze
	 *  (Holder.components привязаны позже). Конструктор регистрирует свой stack-init сюда (Runnable, без стеков), а
	 *  {@link #runDeferredItemInit()} выполняет их в setup (пост-bind). См. decisions/F12-registration-lifecycle.md. */
	public static final List<Runnable> DEFERRED_ITEM_INIT = new ArrayListNoNulls<>();
	public static void deferItemInit(Runnable aInit) {if (aInit != null) DEFERRED_ITEM_INIT.add(aInit);}
	/** F12-followup (oredict-timing): окно исполнения отложенного stack-init на server-start. В 1.7.10 GT6 весь контент-пайплайн
	 *  (make-стеки → OreDict-регистрация → рецепты) шёл @Init/@PreInit; neo привязывает Holder.components только на server-start
	 *  (ReloadableServerResources) → тот же пайплайн физически сдвинут сюда. OreDictManager.registerOre_ имеет guard
	 *  «Only @Init/@PreInit» (sStartedPostInit>0 → throw) — во время ЭТОГО окна guard подавляется: это init GT6, сдвинутый во времени. */
	public static boolean sDeferredItemInitRunning = false;
	/** true после ПЕРВОГО осушения очереди: до этого момента варианты god-предметов ещё не насыпаны (addItems
	 *  в очереди), и судить наполненность вкладок/предметов по getSubItems нельзя — перечислитель пуст не потому,
	 *  что вариантов нет, а потому, что их время не пришло (см. CreativeTabsGT.isTabEmpty). */
	public static boolean sDeferredItemInitDone = false;

	/** F11-recipe-scan: очередь сканов ЧУЖИХ рецептов (Loader_Recipes_Replace). В 1.7.10 скан бежал на PostInit
	 *  по готовому CraftingManager; в neo его вход (ore-версии ванильных рецептов, F4 роль-C) появляется только
	 *  на server-start — очередь исполняется в {@link #onLevelLoadEarlyItemInit} ПОСЛЕ роли-C и ДО
	 *  {@code finalizeRecipeLoading} (чтобы пересборка propertySets/дисплеев увидела уже подавленные рецепты). */
	public static final List<Runnable> DEFERRED_RECIPE_SCAN = new ArrayListNoNulls<>();
	public static void deferRecipeScan(Runnable aScan) {if (aScan != null) DEFERRED_RECIPE_SCAN.add(aScan);}
	/** Сервер текущего окна recipe-scan (ненулевой только во время исполнения очереди) — для {@link #removeDatapackRecipes}. */
	public static net.minecraft.server.MinecraftServer sCurrentServerForRecipeScan = null;

	/** F11-recipe-scan: ПОДАВЛЕНИЕ датапак-рецептов — neo-эквивалент 1.7.10 {@code CraftingManager.getRecipeList().remove(...)}
	 *  (в 1.7.10 Replace удалял заменённый рецепт из живого списка; в neo {@code RecipeManager} рантайм-удаления не имеет,
	 *  {@code RecipeMap} immutable). Карта пересобирается публичной фабрикой {@code RecipeMap.create} без подавленных,
	 *  private-поле {@code RecipeManager.recipes} подменяется рефлексией — приём прецедентен (подмена
	 *  {@code AbstractMinecart.behavior}, JDK 25 пишет private instance-поля). Зовётся ДО finalizeRecipeLoading. */
	/** Все когда-либо подавленные ключи — для переприменения после /reload (карта датапака пересоздаётся). */
	public static final java.util.Set<net.minecraft.resources.ResourceLocation> SUPPRESSED_DATAPACK_RECIPES = new java.util.HashSet<>();

	public void onDatapackSyncReapplySuppression(net.minecraftforge.event.OnDatapackSyncEvent aEvent) {
		if (aEvent.getPlayer() != null) return; // вход игрока — карта не пересоздавалась; переприменение нужно только на /reload
		removeDatapackRecipes(aEvent.getPlayerList().getServer(), new java.util.HashSet<>(SUPPRESSED_DATAPACK_RECIPES));
	}

	public static void removeDatapackRecipes(net.minecraft.server.MinecraftServer aServer, java.util.Set<net.minecraft.resources.ResourceLocation> aRemove) {
		if (aServer == null || aRemove == null || aRemove.isEmpty()) return;
		SUPPRESSED_DATAPACK_RECIPES.addAll(aRemove);
		try {
			// 1.20.1: пересборка списка рецептов — ПУБЛИЧНЫЙ API движка, replaceRecipes(Iterable<Recipe<?>>)
			// (forge-1201-decompiled RecipeManager.java:173); рефлексия на приватное поле, нужная в 26.x, не нужна.
			// Обёртки «рецепт+id» нет — id носит сам рецепт (Recipe.getId(), Recipe.java:52).
			net.minecraft.world.item.crafting.RecipeManager tRM = aServer.getRecipeManager();
			java.util.List<net.minecraft.world.item.crafting.Recipe<?>> tKeep = new java.util.ArrayList<>();
			int tBefore = 0;
			for (net.minecraft.world.item.crafting.Recipe<?> tRecipe : tRM.getRecipes()) {tBefore++; if (!aRemove.contains(tRecipe.getId())) tKeep.add(tRecipe);}
			tRM.replaceRecipes(tKeep);
			OUT.println("GT_API: datapack recipes suppressed (F11-recipe-scan): " + (tBefore - tKeep.size()) + " of " + aRemove.size() + " requested.");
		} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	// drain-loop: коллбэк может добавить новый deferItemInit (вложенная отложка, напр. блок→слэб) — обрабатываем FIFO
	// без ConcurrentModification; список опустошается полностью, включая добавленное во время выполнения.
	public static void runDeferredItemInit() {
		sDeferredItemInitRunning = true;
		// F4 роль-B: ванильные записи словаря, которые в 1.7.10 заводил сам Forge ДО модов
		// (OreDictionary.initVanillaEntries — там же улики и границы переноса). Зовём в самом начале окна,
		// потому что весь stack-based контент GT6 регистрируется ниже по этой очереди и обязан видеть
		// уже наполненный ванильный словарь — ровно тот порядок, что был в 1.7.10.
		try {gregapi.oredict.OreDictionary.initVanillaEntries();} catch(Throwable e) {e.printStackTrace(ERR);}
		try {while (!DEFERRED_ITEM_INIT.isEmpty()) {Runnable tInit = DEFERRED_ITEM_INIT.remove(0); try {tInit.run();} catch(Throwable e) {e.printStackTrace(ERR);}}}
		finally {sDeferredItemInitRunning = false; sDeferredItemInitDone = true;}
	}

	/** F12-followup (block-split, MTE): некоторые GT6-подсистемы (MultiTileEntityRegistry/MultiTileEntityBlock) СТРОЯТ
	 *  neo-Block вне DeferredRegister-supplier И вне preInit (getOrCreate вызывается и на preInit, и на init, с дедупом и
	 *  setMapColor на возврате) — их нельзя выразить одним registerBlockLazy. Их конструирующий код оборачивается в
	 *  deferBlockInit(Runnable): очередь выполняется НА RegisterEvent&lt;Block&gt; (реестр разморожен → intrusive-holder ок),
	 *  а сама регистрация блока идёт через {@link #registerBlock} (ветка event.register, т.к. DeferredRegister уже мог быть
	 *  обработан). BlockItem регистрируется в ITEMS-DR (RegisterEvent&lt;Item&gt; позже). */
	public static final List<Runnable> DEFERRED_BLOCK_INIT = new ArrayListNoNulls<>();
	public static void deferBlockInit(Runnable aInit) {if (aInit != null) DEFERRED_BLOCK_INIT.add(aInit);}
	/** Активное RegisterEvent&lt;Block&gt; во время слива DEFERRED_BLOCK_INIT; ненулевой ⇒ {@link #registerBlock} регистрирует
	 *  блок напрямую в реестр этого события (DeferredRegister этой фазы уже обработан). */
	public static net.minecraftforge.registries.RegisterEvent sBlockRegisterEvent = null;
	private static void runDeferredBlockInit(net.minecraftforge.registries.RegisterEvent aEvent) {
		sBlockRegisterEvent = aEvent;
		try {for (Runnable tInit : DEFERRED_BLOCK_INIT) try {tInit.run();} catch(Throwable e) {e.printStackTrace(ERR);} DEFERRED_BLOCK_INIT.clear();}
		finally {sBlockRegisterEvent = null;}
	}
	private static void onRegisterEvent(net.minecraftforge.registries.RegisterEvent aEvent) {
		if (aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.BLOCK)) runDeferredBlockInit(aEvent);
	}

	/**
	 * F12: мод-шина, сохранённая из конструктора, чтобы лениво созданные под-неймспейсы могли
	 * подписаться на {@code RegisterEvent} (см. {@link #itemsFor(String)}).
	 */
	private static IEventBus sModBus = null;
	/**
	 * F12: по одному {@code DeferredRegister<net.minecraft.world.item.Item>} на неймспейс-владелец. GT6 позволяет создавать
	 * Item под чужим modId (аддоны через {@code PrefixItem}), а {@code DeferredRegister} привязан к
	 * одному неймспейсу — поэтому центр держит карту неймспейс→реестр. Это по-прежнему ОДИН центр
	 * (весь мод сюда обращается), просто с учётом неймспейса, как было в {@code GameRegistry.registerItem(item,name,modId)}.
	 */
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

	/** F12/R3-мост, вызывается из {@code gregapi.util.ST.register(Item, String)}: регистрация под
	 *  неймспейсом GAPI (был прямой выдуманный {@code DeferredRegister.registerItem(...)}). */
	public static net.minecraftforge.registries.RegistryObject<Item> registerItem(Item aItem, String aRegistryName) {
		return registerItem(aItem, aRegistryName, ModIDs.GAPI);
	}

	/** F12/R3-мост: регистрация Item под неймспейсом владельца {@code aModIDOwner} (замена выдуманного
	 *  3-арг {@code DeferredRegister.registerItem(item, name, modId)} из {@code PrefixItem}/{@code ItemFluidDisplay};
	 *  соответствует оригиналу {@code GameRegistry.registerItem(item, name, modId)}). Централизовано —
	 *  весь мод регистрирует Item только через этот метод. */
	public static net.minecraftforge.registries.RegistryObject<Item> registerItem(Item aItem, String aRegistryName, String aModIDOwner) {
		return itemsFor(aModIDOwner).register(aRegistryName, () -> aItem);
	}

	/** F12-followup (item-split): ленивая регистрация — supplier КОНСТРУИРУЕТ предмет на RegisterEvent (реестр разморожен →
	 *  {@code Item.<init>}→{@code createIntrusiveHolder} валиден), а не эагерно в preInit (реестр заморожен → freeze). Call-site:
	 *  {@code GT_API.registerItemLazy(modId, name, () -> Field = new ItemX(...))} — supplier строит предмет, присваивает поле и
	 *  возвращает его. Тот же приём, что fluid-split (FluidGT source-supplier). Заменяет эагер {@code new ItemX()} + self-register. */
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

	/** F12-followup (block-split): ленивая регистрация БЛОКА — supplier конструирует блок на RegisterEvent (реестр разморожен →
	 *  {@code Block.<init>}→{@code createIntrusiveHolder}+setId валидны). BlockItem регистрирует САМ конструктор блока через
	 *  {@link #registerItemLazy} (работает на RegisterEvent&lt;Block&gt;, т.к. RegisterEvent&lt;Item&gt; ещё не сработал). Call-site:
	 *  {@code GT_API.registerBlockLazy(modId, name, () -> Field = new BlockX(...))}. Тот же приём, что item/fluid-split. */
	public static void registerBlockLazy(String aModIDOwner, String aRegistryName, java.util.function.Supplier<? extends Block> aBlockSupplier) {
		blocksFor(aModIDOwner).register(sanitizeRegName(aRegistryName), aBlockSupplier);
	}

	/** neo {@link net.minecraft.resources.ResourceLocation}-путь допускает только [a-z0-9/._-]; GT6-имена предметов содержат
	 *  заглавные (напр. {@code gt.meta.dustSmall}) — санитизируем ТОЛЬКО ключ регистрации (тот же приём, что
	 *  {@code FluidGT.safeRegName}). Идентичность предмета для oredict/паритета — по объекту/{@code mNameInternal}, не по ключу. */
	public static String sanitizeRegName(String aName) {
		String rName = aName.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
		return rName.isEmpty() ? "unnamed" : rName;
	}

	/** F12/R3-мост, вызывается из {@code gregapi.util.ST.register(Block, String, Class)} (был прямой
	 *  выдуманный {@code DeferredRegister.registerBlock(...)}). Пара Block+BlockItem регистрируется под
	 *  одним и тем же именем — как было в оригинальном {@code GameRegistry.registerBlock(Block, Class, String)}. */
	/** F12-followup (item-split): центральная сборка BlockItem для блока. neo {@code BlockItem} НЕ имеет (Block)-конструктора
	 *  (только (Block,Properties)) → {@code callConstructor(BlockItem.class,...)} вернул бы null; строим напрямую с id,
	 *  производным из ключа уже-зарегистрированного блока (BlockItem делит id с блоком). Кастомный класс
	 *  (ItemBlockBase/PrefixBlockItem/ItemBlockMetaType/…) имеет (Block)-конструктор и сам ставит id из ключа блока. */
	public static BlockItem blockItemFor(Block aBlock, Class<? extends BlockItem> aItemClass) {
		if (aItemClass != null && aItemClass != BlockItem.class) {
			BlockItem rItem = (BlockItem)UT.Reflection.callConstructor(aItemClass, 0, null, T, aBlock);
			if (rItem != null) return rItem;
		}
		return new BlockItem(aBlock, new net.minecraft.world.item.Item.Properties());
	}

	public static net.minecraftforge.registries.RegistryObject<Block> registerBlock(Block aBlock, String aRegistryName, Class<? extends BlockItem> aItemClass) {
		if (sBlockRegisterEvent != null) {
			// F12-followup (block-split, MTE): вызвано из deferBlockInit во время RegisterEvent<Block> — блок УЖЕ построен
			// (реестр разморожен), регистрируем его напрямую в реестр события (ключ санитизирован, совпадает с setId блока);
			// BlockItem — в ITEMS-DR (обработается на RegisterEvent<Item> позже). DeferredRegister BLOCKS уже мог быть обработан.
			// F12-namespace (MTE): namespace=GT — gt.multitileentity контент GT6 (golden gregtech:), не gregapi. Единственные
			// вызыватели registerBlock — MTE (ST.register из MultiTileEntityRegistry/MultiTileEntityBlock). Ключ реестра/item-DR
			// совпадает с setId блока (ModIDs.GT) и ключом предмета (BuiltInRegistries.BLOCK.getKey(block)=GT). ~17k рецептов паритета.
			sBlockRegisterEvent.register(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ModIDs.GT, sanitizeRegName(aRegistryName)), () -> aBlock);
			itemsFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> blockItemFor(aBlock, aItemClass));
			return null;
		}
		net.minecraftforge.registries.RegistryObject<Block> rBlock = blocksFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> aBlock);
		itemsFor(ModIDs.GT).register(sanitizeRegName(aRegistryName), () -> blockItemFor(aBlock, aItemClass));
		return rBlock;
	}

	private LoggerPlayerActivity mPlayerLogger;

	@SuppressWarnings("unchecked")
	// javafml 1.20.1 конструирует @Mod-класс БЕЗАРГУМЕНТНЫМ конструктором
	// (FMLModContainer.constructMod → modClass.getDeclaredConstructor()); аргумент IEventBus появился
	// только в 26.x. Мод-шина берётся из контекста загрузки — форма 1.20.1, сверена с живым образцом
	// (reference/mods/Applied-Energistics-2-1.20.1/.../AppEngBase.java:125) и с членом
	// FMLJavaModLoadingContext.getModEventBus()Lnet/minecraftforge/eventbus/api/IEventBus;.
	public GT_API() {
		IEventBus aModBus = FMLJavaModLoadingContext.get().getModEventBus();
		GAPI = this;
		
		if (!MD.ENCHIRIDION.mLoaded) MD.MaCu.mLoaded = F;
		
		// A bunch of Code that is there to statically initialize the Database in the right order and without crashes.
		MT.init();
		// Замена @SidedProxy: строим сторонний прокси здесь, ПОСЛЕ MT.init() (клиентский прокси в ctor читает
		// MT.*.mRGBa), а не инлайн в статик-инициализаторе поля — иначе class-init тянул MT до STACKMAPS и падал NPE.
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
		
		
		// F6/26.1.2: здесь мир больше НЕ строится. Level.<init> требует реестр биомов
		// (Level.java:158 → PalettedContainerFactory:25 → lookupOrThrow(Registries.BIOME)), а на фазе
		// конструирования мода реестров ещё нет — попытка падала каждый запуск, и CS.DW оставался null.
		// Мир создаётся, когда реестр появляется: gregapi.dummies.DummyWorld.ensure(server.registryAccess()),
		// вызывается на старте сервера (Abstract_Mod.onModServerStarting) — раньше рецептов он не нужен,
		// его единственные потребители зовут recipe.matches(...) уже в игре.
		
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
		
		// F12: "fixing missing container items" (1.7.10 setContainerItem: mushroom_stew->bowl, potion/experience_bottle->
		// glass_bottle) — В NEO НЕ НУЖНО: ваниль УЖЕ несёт эти remainder по умолчанию (сверено референс:
		// Items.java MUSHROOM_STEW = ...usingConvertsTo(BOWL); potion/experience_bottle аналогично). Операция избыточна →
		// корректный no-op (ничего не «missing»), не заглушка. GT6-собственные предметы задают craftRemainder на регистрации.
		
		// Fixing Max Stacksizes that don't make sense.
		ST.forceProperMaxStacksizes();
		
		// Fixing some Adventure Mode things.
		// 1.7.10 правил здесь ЧУЖОЙ объект: Blocks.bed/sponge/hay_block -> "axe", tnt/monster_egg -> "pickaxe",
		// obsidian -> "pickaxe" 3 (оригинал GT_API.java:204-209). Мутатора чужого блока в neo нет
		// (harvest-tier задаётся неизменяемо при регистрации), НО мод спрашивает не движок, а свой центр:
		// все шесть значений лежат в паспорте WD.vanillaPassport, снятом оракулом с ЖИВОГО 1.7.10 — то есть
		// уже вместе с этой правкой, потому что дампер мерил мод в сборе. Функция на месте, мутация не нужна.

		try {
			// The Access Transformer should make this work
			Material.tnt.setAdventureModeExempt();
		} catch(Throwable e) {
			UT.Reflection.callMethod(Material.tnt, new String[] {"func_85158_p", "setAdventureModeExempt"}, T, F, F);
			e.printStackTrace(ERR);
		}

		// F12 impossible-1:1 (harvest-tier в neo immutable при ctor + data-driven BlockTags.MINEABLE_WITH_*, не runtime-мутатор): reflection-хак "AxeItem/ItemPickaxe.field_150917_c/
		// field_150915_c" (приватный статический Set<Block> "эффективных" блоков 1.7.10) не имеет 1:1
		// аналога — инструмент-эффективность в neo тоже data-driven (те же BlockTags.MINEABLE_WITH_*),
		// подобных изменяемых static-полей на Item-классах в декомпиле нет. Не найдено ни в одном из
		// 3 корней референса — деградация до no-op.

		// F12: центральные DeferredRegister этого мода — на мод-шину; шину запоминаем, чтобы лениво
		// созданные под-неймспейсы (itemsFor) тоже успели подписаться на RegisterEvent.
		sModBus = aModBus;
		ITEMS .register(aModBus);
		BLOCKS.register(aModBus);
		BLOCK_ENTITIES.register(aModBus); // F12-followup (MTE-type-timing): placeholder MTE_TYPE на RegisterEvent<BlockEntityType> (до freeze)
		ENTITIES.register(aModBus); // F12-entity: EntityType падающего мета-блока (замена EntityRegistry.registerModEntity, оригинал GT_API.java:722)
		SOUND_EVENTS.register(aModBus); // BUG-113: свои звуки мода (в 1.7.10 хватало sounds.json, в neo нужен SoundEvent в реестре)
		// F12-followup (block-split, MTE): слив DEFERRED_BLOCK_INIT на RegisterEvent<Block> (реестр разморожен) — единая
		// точка для подсистем, чьё конструирование блока нельзя выразить одним registerBlockLazy (см. deferBlockInit).
		aModBus.addListener(GT_API::onRegisterEvent);
		// F6: центральный ворлдген-переходник (Feature/PlacedFeature/BiomeModifier) — тот же мод-бас,
		// единая точка подписки (decisions/F6-worldgen.md, gregapi/worldgen/GT6WorldgenFeature.java).
		gregapi.worldgen.GT6WorldgenFeature.register(aModBus);
		// ENCHANT: центральный переходник кастомных чар-эффектов — тот же мод-бас, единая точка подписки
		// (gregapi/enchants/EnchantsGT6.java; закрывает стык F6↔ENCHANT wiring, метка `ENCHANT, регистрация`).
		gregapi.enchants.EnchantsGT6.register(aModBus);
		// Правка №1 (BUG-106): карта материалов руды на чанке — тот же мод-бас, единая точка подписки
		// (gregapi/block/prefixblock/PrefixBlockOreMap.java).
		gregapi.block.prefixblock.PrefixBlockOreMap.register(aModBus);
		// BUG-090: центральный DeferredRegister GT6-зельев-эффектов (flammable/slippery/conductive/sticky/
		// insanity — «функция, не авторство»: IE/EnviroMine для 26.1.2 нет) — тот же мод-бас, единая точка
		// подписки (gregapi/potion/MobEffectsGT.java; int-id встают в PotionsGT.ID_* на postInit ниже).
		gregapi.potion.MobEffectsGT.register(aModBus);
		// F5: центральные DeferredRegister жидкостей (FluidType+Fluid) — тот же мод-бас, единая точка
		// подписки (decisions/F5-fluids.md §3, gregapi/fluid/FluidGT.java; закрывает прежний долг F12↔F5 wiring).
		gregapi.fluid.FluidGT.FLUID_TYPES.register(aModBus);
		gregapi.fluid.FluidGT.FLUIDS.register(aModBus);
		// F5-capability: подписки на мод-шину здесь БОЛЬШЕ НЕТ. В 26.x капы объявлялись событием
		// RegisterCapabilitiesEvent (регистрация провайдеров по блокам); в 1.20.1 капу объявляет сам
		// BlockEntity методом getCapability(Capability, Direction) — единственный мост стоит в общем корне
		// иерархии (TileEntityBase01Root), а знание «что показать» живёт в gregapi/fluid/GT6FluidCapability.java
		// и gregapi/tileentity/GT6ItemCapability.java. Это форма 1.7.10: наружный контракт нёс сам TE.
		// Ветка 1.20.1: носитель трекера еды — capability на сущности (в 1.7.10 IExtendedEntityProperties,
		// в 26.x attachment). Единая точка подписки, ни один другой файл её не дублирует
		// (gregapi/player/EntityFoodTracker.java).
		gregapi.player.EntityFoodTracker.register(aModBus);
		// Ветка 1.20.1: доставка обработки дропа ЧУЖИХ блоков — глобальный модификатор лута (событий со списком
		// дропов у Forge 1.20.1 нет); правило живёт в GT_API_Proxy.processBlockDrops, здесь только реестр кодека.
		gregapi.loot.GT6BlockDropsModifier.register(aModBus);
		// F11: центральный крафт-верстак-диспетчер (CustomRecipe SERIALIZERS) — тот же мод-бас, единая точка
		// подписки (decisions/F11-crafting-recipe.md §7, gregapi/recipes/GT6CraftingDispatcher.java; закрывает
		// прежний долг F12↔F11 wiring).
		GT6CraftingDispatcher.register(aModBus);
		// F14: центральный MenuType GUI (ContainerCommon) — тот же мод-бас, единая точка подписки (decisions/F14-gui-menu.md)
		gregapi.gui.ContainerCommon.register(aModBus);
		// F3-render (client): единый динамический тип модели GT6BlockModel на mod-bus (RegisterBlockStateModels).
		// Только клиент — делегируем в клиент-прокси (server: no-op), общий код не грузит client-only классы.
		api_proxy.registerClientModels(aModBus);
		// F16-creative-tab: единый хендлер наполнения вкладок (замена россыпи setCreativeTab) — тот же мод-бас.
		gregapi.item.CreativeTabsGT.register(aModBus);
		// F16: отдельного носителя stack-size-override здесь больше нет. С Access Transformer'ом ветки
		// ST.setMaxStackSize снова мутирует поле предмета прямо (как 1.7.10), и вызовы стоят в 1:1-точках фаз
		// (ST.forceProperMaxStacksizes — GT_API:520 и :1402; OreDictPrefix.applyAllStackSizes — GT_API:1400).
		// GameTest'ы (проверка механик в РЕАЛЬНОМ мире) — ОСНАСТКА, а не поставка: живут в src/gametest/java,
		// подключаются флагом -Pgt6probes и подписываются на мод-шину сами (@EventBusSubscriber). Отсюда их
		// больше не зовут — production-код об оснастке не знает.

		// F12: замена annotation-диспетчера @Mod.EventHandler — подписка фаз на мод-шину напрямую.
		// GT6-трёхфазный контракт (Pre/Init/Post) сохранён 1:1 поверх родных событий жизненного цикла neo:
		// PreInit -> FMLConstructModEvent; Init -> FMLCommonSetupEvent; PostInit -> FMLLoadCompleteEvent
		// (decisions/F12-registration-lifecycle.md §4).
		aModBus.addListener(this::onPreLoad);
		aModBus.addListener(this::onLoad);
		aModBus.addListener(this::onPostLoad);

		// Серверные фазы GT6 (Abstract_Mod уже на родных событиях neo) — на игровой шине, не на мод-шине.
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
		// BUG-033 (КОРЕНЬ): отложенная item-init должна добежать ДО пре-генерации стартовой зоны — см. onLevelLoadEarlyItemInit.
		MinecraftForge.EVENT_BUS.addListener(this::onLevelLoadEarlyItemInit);
		// F11-recipe-scan (граница M-52): /reload пересоздаёт RecipeMap датапака — подавление Replace
		// переприменяется на OnDatapackSyncEvent (player==null = reload; стреляет ДО отправки рецептов клиенту).
		MinecraftForge.EVENT_BUS.addListener(this::onDatapackSyncReapplySuppression);
	}

	/** BUG-033 fix (КОРЕНЬ стартовой зоны) + F12 refinement. **ЕДИНАЯ авторитетная точка исполнения отложенной
	 *  item-init** ({@link #runDeferredItemInit}, наполняет в т.ч. worldgen-реестр {@code GEN_GT} через
	 *  {@code Loader_Worldgen}). Прежде F12 держал drain на {@code ServerStartingEvent}, но в порядке загрузки neo
	 *  пре-генерация стартовой зоны идёт РАНЬШЕ: {@code MinecraftServer.loadLevel()} = {@code createLevels()} [здесь
	 *  летит {@code LevelEvent.Load}] → {@code prepareLevels()} [«Preparing spawn area», спавн-worldgen] → и лишь ПОТОМ
	 *  {@code runServer()} шлёт {@code ServerStartingEvent} (сверено neo {@code MinecraftServer.java:403-411,733-739}).
	 *  Итог прежнего порядка: {@code GEN_GT} пуст на спавне → стартовая зона рождалась ЧИСТОЙ ВАНИЛЬЮ (deepslate/руды/
	 *  породы не замещались), чанки исследования (после ServerStarting) — нормальные GT6.
	 *  <p>Фикс: drain на загрузке overworld-уровня — это в {@code createLevels()} (реестры уже заморожены
	 *  {@code compositeAccess()} = post-bind, ST.make валиден), но ДО {@code prepareLevels()}. Тогда {@code GEN_GT}
	 *  готов к пре-гену спавна. **Это ЕДИНСТВЕННЫЙ drain** — точка ПОЗЖЕ (ServerStarting) убрана: очередь наполняется
	 *  только на mod-load (все {@code deferItemInit} в конструкторах/загрузчиках, ДО загрузки уровня), к
	 *  {@code LevelEvent.Load} она полна, drain её осушает целиком, после ничего не добавляется → ServerStarting-вызов
	 *  был доказанным no-op (живой полный тест игрока на версии с обоими вызовами это подтвердил: всё — рецепты/вкладки/
	 *  предметы/генерация — работает при drain'е на LevelEvent.Load, т.е. этот момент пост-bind для ВСЕХ отложек). */
	/** Вода не восстанавливается сама — это ШТАТНОЕ поведение GT6, а не нововведение порта (подтверждено
	 *  пользователем живой проверкой в 1.7.10: источник между двумя источниками там НЕ появляется). Порт это
	 *  поведение потерял, здесь оно ВОССТАНАВЛИВАЕТСЯ. Механизм 1.7.10 в исходнике не опознан (проверены события
	 *  Forge, ASM-патчи GT6, рефлексия по Blocks.water, конфиги, подмена блоков) — воспроизводится РЕЗУЛЬТАТ.
	 *  В движке 26.1.2 его даёт ЕДИНСТВЕННЫЙ канал — правило мира
	 *  {@code water_source_conversion} ({@code GameRules.java:92}, дефолт true), которое читает сама ванильная
	 *  вода ({@code WaterFluid.canConvertToSource:76-77}). Никакой иной точки у мода нет: жидкость ванильная,
	 *  её {@code FluidType} принадлежит движку. Поэтому правило выставляется ОДИН раз на загрузке overworld —
	 *  там же, где мод уже приводит мир в своё состояние. Лава не трогается (в 1.7.10 она и так конечна).
	 *
	 *  Настройка {@code general.WaterSourceConversion} возвращает ВАНИЛЬНОЕ поведение (дефолт F = вода конечна,
	 *  как в GT6). ⚠️ Правило пишется в сам мир (level.dat): после снятия мода оно останется выключенным, пока
	 *  игрок не вернёт его командой — побочный эффект единственного доступного канала. */
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
			// BUG-054: гейт shift-click ванильной печи (RecipePropertySet.FURNACE_INPUT → AbstractFurnaceMenu.canSmelt:142)
			// собирается движком на loadLevel ДО этой data-init (FurnaceRecipes ещё пуст → GT6SmeltingDispatcher.input()
			// отдаёт плейсхолдер BARRIER) → ванильная печь не признаёт GT6-обжигаемое, shift не кладёт его во входной слот.
			// Пересобираем propertySet ПОСЛЕ наполнения FurnaceRecipes: input() теперь непуст → forSingleInput(SMELTING)
			// (RecipeManager:257, свежий input(), не кэш) собирает GT6-входы → canSmelt(GT6)=true. ТОТ ЖЕ вызов, что движок
			// делает на reload (MinecraftServer.java:356,1588), идемпотентен. Топливо (isFuel) не затронуто — оно идёт через
			// FurnaceFuelBurnTimeEvent, независимо от propertySet. Сама плавка/ручная укладка работали и до фикса (matches live-lookup).
			net.minecraft.server.MinecraftServer tServer = tLevel.getServer();
			// F4 роль-C: замена ванильных верстак-рецептов ore-версиями (в 1.7.10 это делал сам Forge в
			// initVanillaEntries, вторая половина). Именно здесь: RecipeManager полон датапаком, словарь
			// полон ванилью (роль-B в начале drain'а выше) и GT6-стеками (сам drain). Идемпотентно.
			gregapi.oredict.OreDictionary.initVanillaRecipeReplacements(tServer);
			// F11-recipe-scan: сканы чужих рецептов (Loader_Recipes_Replace) — ПОСЛЕ роли-C (их вход — её
			// ore-версии, как в 1.7.10 входом были Forge-замены) и ДО finalizeRecipeLoading ниже (подавление
			// датапак-рецептов должно попасть в пересборку propertySets/дисплеев recipe book).
			sCurrentServerForRecipeScan = tServer;
			try {for (Runnable tScan : DEFERRED_RECIPE_SCAN) try {tScan.run();} catch(Throwable e) {e.printStackTrace(ERR);} DEFERRED_RECIPE_SCAN.clear();}
			finally {sCurrentServerForRecipeScan = null;}
			// BUG-091-хвост, датапак-плечо CR.remove (см. CR.DATAPACK_REMOVALS): в 1.7.10 remove(...) удалял и
			// ВАНИЛЬНЫЕ рецепты живого CraftingManager (бревно→4 доски и т.п.); их neo-наследники в датапаке
			// подавляются здесь ТЕМ ЖЕ судом, что 1.7.10 — matches() накопленной сеткой, — тем же центром
			// removeDatapackRecipes, что Replace. Собственный GT6CraftingDispatcher исключён (он матчится на
			// те же сетки — подавили бы сами себя).
			if (tServer != null) try {
				java.util.Set<net.minecraft.resources.ResourceLocation> tRemove = new java.util.HashSet<>();
				for (net.minecraft.world.item.ItemStack[] tGrid : gregapi.util.CR.DATAPACK_REMOVALS) {
					net.minecraft.world.inventory.CraftingContainer tInput = gregapi.util.CR.crafting(tGrid);
					for (net.minecraft.world.item.crafting.Recipe<?> tAny : tServer.getRecipeManager().getRecipes()) {
						if (!(tAny instanceof net.minecraft.world.item.crafting.CraftingRecipe tCraft)) continue;
						if (tAny instanceof gregapi.recipes.GT6CraftingDispatcher) continue;
						try {if (tCraft.matches(tInput, tServer.overworld())) tRemove.add(tAny.getId());} catch(Throwable e) {/*чужой рецепт упал на matches — не наш суд*/}
					}
				}
				// Второе плечо ТОГО ЖЕ класса — снятие по ВЫХОДУ (CR.delate/CR.remout, см. CR.DATAPACK_REMOVALS_OUT).
				// Суд ровно тот, что был у 1.7.10-remout: сравнение выхода рецепта с накопленным, NBT игнорируется.
				// Выход берётся getResultItem(registryAccess) — прямой наследник 1.7.10 IRecipe.getRecipeOutput()
				// (forge-1201-decompiled Recipe.java:19), тем же приёмом, что роль-C (OreDictionary.initVanillaRecipeReplacements).
				for (net.minecraft.world.item.ItemStack tOut : gregapi.util.CR.DATAPACK_REMOVALS_OUT) {
					for (net.minecraft.world.item.crafting.Recipe<?> tAny : tServer.getRecipeManager().getRecipes()) {
						if (!(tAny instanceof net.minecraft.world.item.crafting.CraftingRecipe tCraft)) continue;
						if (tAny instanceof gregapi.recipes.GT6CraftingDispatcher) continue;
						try {
							net.minecraft.world.item.ItemStack tResult = tCraft.getResultItem(tServer.registryAccess());
							if (gregapi.util.ST.valid(tResult) && gregapi.util.ST.equal(tResult, tOut, T)) tRemove.add(tAny.getId());
						} catch(Throwable e) {/*чужой рецепт упал на getResultItem — не наш суд*/}
					}
				}
				gregapi.util.CR.DATAPACK_REMOVALS_OUT.clear();
				gregapi.util.CR.DATAPACK_REMOVALS.clear();
				// Перезаход в одиночке = НОВЫЙ MinecraftServer со свежим (полным) датапаком, а очереди сканов
				// и DATAPACK_REMOVALS уже осушены первым стартом — накопленный набор ключей переприменяется
				// ЗДЕСЬ на каждом LevelEvent.Load (идемпотентно; /reload покрыт OnDatapackSyncEvent отдельно).
				// До этой строки подавление жило только в первом сервере сессии — релог возвращал ваниль
				// (симптом игрока: «бревно рукой снова даёт 4»; касалось и 21 инструмент-подавления Replace).
				tRemove.addAll(SUPPRESSED_DATAPACK_RECIPES);
				// BP-BUG-013 (на main — BUG-095-рецидив/BUG-124): снятие GT6 обязано доставать до ore-ВЕРСИИ
				// рецепта, а не только до его датапак-оригинала. В 1.7.10 замены Forge (ShapedOreRecipe) и
				// ванильные рецепты лежали в ОДНОМ CraftingManager, и remout(выход)/remove(сетка) резали их
				// одним проходом по одному списку (gt6-original CR.java:584 list(), 603-625 remout). Порт разнёс
				// их по двум спискам — датапак (RecipeManager) и собственный буфер GT6 (CR.BUFFER), — а роль-C
				// (OreDictionary.initVanillaRecipeReplacements, вызов выше по этому же методу) СТРОИТ ore-версию
				// ванильного рецепта ПОСЛЕ того, как загрузчики отработали свои снятия: в буфер ложится копия
				// рецепта, который GT6 только что снял. Симптом игрока: печь крафтилась из голого булыжника, хотя
				// оригинал даёт её только через OD.craftingFirestarter (Loader_Recipes_Vanilla:59-61,67 против
				// CR.delate:67). Оба цикла подавления выше до неё не достают ПО ПОСТРОЕНИЮ — они пропускают
				// GT6CraftingDispatcher, который эту ore-версию и подаёт в верстак.
				// Суд идёт по ИСТОЧНИКУ, а не по выходу: ore-версия помнит ключ своего датапак-оригинала
				// (mSourceId, OreDictionary:358,370). Оригинал подавлен -> подавлена и ore-версия; оригинал жив ->
				// ore-версия остаётся супермножеством живого рецепта, как и задумано ролью-C. Опора — tRemove, куда
				// уже влит персистентный SUPPRESSED_DATAPACK_RECIPES: сами регистры снятия осушаются выше, и проход
				// по ним был бы верен только на ПЕРВОЙ загрузке мира (тот же класс ошибки, что чинила строка выше).
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
			// Ветка 1.20.1: пересобирать нечего — статического RecipePropertySet, ради которого стоял
			// finalizeRecipeLoading, в этой версии не существует; shift-click-гейт печи живой
			// (AbstractFurnaceMenu.canSmelt:145-146 → getRecipeFor → matches). Класс дефекта BUG-054 отсутствует
			// (разбор — journal шов-работа №1, Б-3).
			// BUG-039 (F-loot, тот же класс тайминга): LootTableLoadEvent отстрелял при загрузке ресурсов ДО этой
			// data-init (буфер ChestGenHooks был пуст) → догоняющая инъекция GT-пулов в загруженные таблицы.
			// Идемпотентна (именованный pool); /reload и последующие загрузки покрывает сам LootTableLoadEvent.
			gt6mirror.minecraftforge.common.ChestGenHooks.injectAll(tServer);
		} else if (aEvent.getLevel() instanceof net.minecraft.world.level.Level tClientLevel && tClientLevel.isClientSide()) {
			// BUG-094 (дедикейт: камни/палки/машины прозрачны): у клиента, подключённого к ВЫДЕЛЕННОМУ серверу,
			// ServerLevel не существует → единственный drain выше НИКОГДА не бежал → вся отложенная item-init
			// (MTE-регистрации 4297, mDrops, oredict, вкладки…) на клиенте пуста; ни один GT6-пакет синка не мог
			// создать клиент-BE (getRegistry(id).mRegistry.size()==0, замер стенда gt6remoteprobe: NULL=228/228).
			// В 1.7.10 эта init жила в FML-фазах, бежавших НА ОБЕИХ сторонах, — клиентское плечо потерялось при
			// переносе на серверное событие. Одиночка маскирует: интегрированный сервер осушает очередь в той же
			// JVM ДО загрузки ClientLevel → здесь очередь уже пуста → no-op (идемпотентно по пустоте очереди).
			// Тайминг тот же, что у серверного плеча: ClientLevel.Load = post-bind (реестры/компоненты привязаны).
			// Серверные хвосты (роль-C, recipe-скан, propertySets, лут) остаются ТОЛЬКО в серверной ветви — у
			// удалённого клиента рецепты/луты приходят синком с сервера.
			runDeferredItemInit();
		}
	}

	/**
	 * PreInit. Замена {@code @Mod.EventHandler onPreLoad(FMLPreInitializationEvent)}: подписан в
	 * конструкторе на {@link FMLConstructModEvent} (мод-шина). Строит GT6-шим {@code FMLPreInitializationEvent}
	 * (носитель фазы, gregapi.api) и передаёт его в {@code Abstract_Mod.onModPreInit(...)} — тело фазы
	 * (onModPreInit2 и далее) остаётся байт-в-байт как в оригинале.
	 * F12-timing (boot-подтверждено: мод бутится, регистрация
	 * контента внутри PreInit работает); формально относительно
	 * FMLConstructModEvent на всех сборках; сверить при первой реальной регистрации через ITEMS/BLOCKS.
	 */
	public void onPreLoad(FMLConstructModEvent aModEvent) {runPhaseInModLoadOrder(aModEvent, this, this::onPreLoadPhase);}
	/** Тело фазы PreInit; запускается центром {@code Abstract_Mod#runPhaseInModLoadOrder} в порядке загрузки модов. */
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

	/**
	 * Init. Замена {@code @Mod.EventHandler onLoad(FMLInitializationEvent)}: подписан в конструкторе на
	 * {@link FMLCommonSetupEvent} (мод-шина).
	 */
	public void onLoad(FMLCommonSetupEvent aModEvent) {runPhaseInModLoadOrder(aModEvent, this, this::onLoadPhase);}
	/** Тело фазы Init; запускается центром {@code Abstract_Mod#runPhaseInModLoadOrder} в порядке загрузки модов. */
	private void onLoadPhase() {
		// F1/F12/F16 boot-timing: ore-target'ы + рецепты создают ItemStack (ST.make(Blocks/Items)) — onLoad(CommonSetup) НЕ
		// пост-bind (Holder.components привязывает ReloadableServerResources на server-start). Оборачиваем в deferItemInit →
		// выполнится в onModServerStarting2 (post-bind). НЕ в паритет-данных (ore-targets/recipes ≠ material/prefix scalar).
		deferItemInit(() -> {
		// vanilla-ore-target'ы. Порядок «registered first» сохранён (первый deferred).
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

		// F12 boot-timing (ПЕРЕНЕСЕНО из onModPreInit2): рецепт-фиксы создают ItemStack (ST.make) — невозможно в preInit
		// (Holder.components не привязаны); здесь (после регистрации+привязки) можно. Fixing vanilla Oak Plank Slab Recipe.
		CR.remove(ST.make(Blocks.OAK_PLANKS, 1, 0), ST.make(Blocks.SPRUCE_PLANKS, 1, 0), ST.make(Blocks.BIRCH_PLANKS, 1, 0));
		CR.shaped(ST.make(Blocks.OAK_SLAB, 6, 0), CR.NONE, "WWW", 'W', ST.make(Blocks.OAK_PLANKS, 1, 0));
		// Preventing a Water Dupe by registering this Recipe early so it won't be overridden
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.GLASS_BOTTLE, 1, 0), FL.Water.make(250), NF, ST.make(Items.POTION, 1, 0));
		RM.Canner.addRecipe1(T, 16, 16, ST.make(Items.POTION, 1, 0), ST.make(Items.GLASS_BOTTLE, 1, 0));
		}); // конец deferItemInit-обёртки ore-targets/recipes (выполнится @onModServerStarting2, post-bind)

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
		// F1/F12/F16 item-model: runDeferredItemInit ПЕРЕНЕСЁН в onModServerStarting2 — onLoad(CommonSetup) НЕ пост-bind
		// (верифицировано: Holder.components привязывает ReloadableServerResources на server-start, Holder.java:108).
	}
	
	// PostInit: подписан в конструкторе на FMLLoadCompleteEvent (мод-шина) — родное neo-событие
	// заменяет старую FML 1.7.10 сложность вокруг loadComplete (комментарий оракула выше снят вместе
	// с @Mod.EventHandler-диспетчером, который и был источником проблемы).
	public void onPostLoad(FMLLoadCompleteEvent aModEvent) {runPhaseInModLoadOrder(aModEvent, this, () -> onModPostInit(new FMLPostInitializationEvent()));}

	@Override public String getModID() {return MD.GAPI.mID;}
	@Override public String getModName() {return MD.GAPI.mName;}
	@Override public String getModNameForLog() {return "GT_API";}
	@Override public Abstract_Proxy getProxy() {return api_proxy;}

	// Серверные фазы — подписаны в конструкторе на MinecraftForge.EVENT_BUS (игровая шина), не на мод-шину.
	public void onServerStarting  (ServerStartingEvent aEvent) {
		// ЦЕНТР ЛОКАЛИЗАЦИИ (BUG-082), СЕРВЕРНОЕ ПЛЕЧО. В 1.7.10 впрыск имён жил в общем коде
		// (LanguageRegistry.injectLanguage — обе стороны), поэтому серверные строки тоже были человеческими.
		// Клиентское плечо висит на загрузке ресурсов (GT_API_Proxy_Client), которой на выделенном сервере нет —
		// здесь та же надстройка ставится над серверной таблицей. Подробности — LanguageHandler.injectIntoEngine().
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
		
		if (ConfigsGT.GREGTECH.get("general", "disable_STDOUT"             , F)) System.out.close();
		if (ConfigsGT.GREGTECH.get("general", "disable_STDERR"             , F)) System.err.close();
		// F12: 1.7.10 Blocks.mob_spawner.setHardness(500)/setResistance(6000000) — runtime-мутация vanilla-блока (neo Properties
		// immutable). Кэшируем config-флаги; hardness применяется через PlayerEvent.BreakSpeed (GT_API_Proxy.onBlockBreakSpeedEvent,
		// SPAWNER → speed×0.01 = 5/500), blast-resistance — через ExplosionEvent (GT_API_Proxy, SPAWNER исключается из разрушаемых).
		HARDER_MOB_SPAWNERS          = ConfigsGT.GREGTECH.get("general", "hardermobspawners"          , T);
		BLAST_RESISTANT_MOB_SPAWNERS = ConfigsGT.GREGTECH.get("general", "blastresistantmobspawners"  , T);
		
		// ADAPT-005 (нововведение, ADAPTATIONS.md): свет горящих топочных машин; 0 = выкл (строгое 1:1), кламп 0-15.
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
		// [        +124] = PacketOreMap (ветка 1.20.1: у капабилити чанка нет автосинка, который был у neo-attachment)
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
		// F12-entity (СДЕЛАНО ВЫШЕ, здесь звать нечего): оригинал регистрировал класс
		// (`GameRegistry.registerTileEntity(PrefixBlockTileEntity.class, "gt.MetaBlockTileEntity")`), neo регистрирует
		// BlockEntityType. Он заведён один на всю GT6-TE-иерархию — MTE_TYPE_HOLDER (:200), а его supplier сам отдаёт
		// PrefixBlockTileEntity для PrefixBlock-блоков (TileEntityBase01Root.createType:164) и isValid()→true, поэтому
		// отдельного типа под мета-блоки не нужно: реконструкция из NBT и постановка в мир уже идут через него.
		// Creating and loading the Lang File.
		if (CODE_CLIENT) {
			tFile = new File(DirectoriesGT.MINECRAFT, "GregTech.lang");
			if (!tFile.exists()) tFile = new File(DirectoriesGT.MINECRAFT, "gregtech.lang");
			LanguageHandler.sLangFile = new ModConfigSpec(tFile);
			LanguageHandler.sUseFile = LanguageHandler.sLangFile.get("EnableLangFile", "UseThisFileAsLanguageFile", F).getBoolean(F);
		}
		// BUG-106 (вторая утечка, замер живой игры 2026-08-09): очереди icon-load снимаются на ОБЕИХ сторонах, а не только
		// на сервере. В 1.7.10 их разбирал драйвер фазы загрузки иконок (ItemFluidDisplay.registerIcons -> обход
		// sBlockIconload при сшивке атласа); в порте этот драйвер МЁРТВ (IIconRegister удалён движком), а построение иконки
		// сделано ЛЕНИВЫМ (Textures.java:720, TextureSet.java:158, BI.java:176) — очередь больше никто не читает, но её
		// продолжали НАПОЛНЯТЬ. На клиенте она жила вечно: каждый CustomIcon, созданный уже в игре, вписывал себя в
		// статику навсегда. Горячий источник — MultiTileEntityMultiBlockPart.readFromNBT2:144 (иконки строятся на КАЖДОМ
		// чтении NBT части мультиблока, а оно идёт при загрузке чанков и реконструкции блок-сущностей).
		// Замер: 8 549 954 объекта CustomIcon, класс GT_API удерживал 1 924 485 544 байт = 47,26 % кучи (дамп MAT).
		// Снятие очереди отключает ВСЕ четыре точки записи разом (они все под гейтом `!= null`) — центр, а не россыпь.
		if (sBlockIconload != null) {sBlockIconload.clear(); sBlockIconload = null;}
		if (sItemIconload  != null) {sItemIconload .clear(); sItemIconload  = null;}
		// Creating and loading the Unification Config.
		OreDictManager.INSTANCE.mUnificationConfig = new Config("Unification.cfg");
		// Initialising the Re-Registrations.
		new LoaderOreDictReRegistrations().run();
		// Register the Falling MetaBlock Entity.
		// F12-entity (СДЕЛАНО ВЫШЕ, здесь звать нечего): 1.7.10 регистрировал класс сущности прямо в этой точке
		// (`EntityRegistry.registerModEntity`, оригинал :722), neo требует EntityType в реестре ДО этой фазы —
		// поэтому регистрация переехала в центральный ENTITIES/METABLOCK_FALLING (:203-214), параметры 1:1.
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
		IL.Circuit_Selector.set(GT_API.ITEMS.register("gt.integrated_circuit", ItemIntegratedCircuit::new)); // F12-lazy: construct@RegisterEvent-supplier
		// Initialises the Empty Slot Marker Item.
		IL.Empty_Slot.set(GT_API.ITEMS.register("gt.empty_slot", ItemEmptySlot::new)); // F12-lazy: construct@RegisterEvent-supplier
		// Register the GUI Handler.
		// F7-gui (GUI работает через GT6MenuProvider/ContainerCommon; старый Forge GUI-handler — документация)
		// F12 boot-timing: рецепт-фиксы (ST.make = ItemStack) ПЕРЕНЕСЕНЫ в onLoad (FMLCommonSetupEvent) — стеки нельзя
		// создавать в preInit (Holder.components не привязаны). См. onLoad. (Было: CR.remove/CR.shaped/RM.Canner.addRecipe1 тут.)
		
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
	public void onModPostInit2(FMLPostInitializationEvent aEvent) {deferItemInit(() -> onModPostInit2Deferred(aEvent));} // F1/F12/F16: PostInit-data-init (ST.make/static-init) отложен на server-start (post-bind); LoadComplete НЕ пост-bind
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
		// BUG-090: моды-владельцы выше для 26.1.2 не существуют (гейты мертвы) — пять эффектов, которые GT6
		// реально накладывает, регистрирует сам мод (gregapi/potion/MobEffectsGT, поведение 1:1 с декомпил-
		// референсами IE/EnviroMine в дереве проекта). Механизм — Грегов же «real IDs are to be set on API
		// postInit» (CS.java:1690): проставляем id и привязываем Holder в единую карту канала applyPotion(int).
		// Гейт `< 0` сохраняет приоритет чужого мода, если тот когда-либо оживёт. RADIATION/DEHYDRATION/
		// HYPOTHERMIA/HEATSTROKE/FROSTBITE остаются отрицательными намеренно — разбор в MobEffectsGT (javadoc).
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
		// F1/F12/F16 item-model: отложенный stack-init предметов (OreDict-данные+рецепты) — ЕДИНАЯ точка исполнения перенесена
		// на ЗАГРУЗКУ уровня ({@link #onLevelLoadEarlyItemInit}, LevelEvent.Load), т.к. пре-генерация стартовой зоны
		// (prepareLevels) идёт РАНЬШЕ ServerStartingEvent и потребляет worldgen-реестр (BUG-033). LevelEvent.Load — тоже
		// пост-bind (createLevels, compositeAccess заморожены), но ДО prepareLevels. К этому моменту очередь уже осушена.
		// F16-shell: генератор вкладок (MTE-загрузчик) отработал в drain выше по времени → фиксируем полный набор собственных
		// вкладок в конфиг-кэш; на СЛЕДУЮЩЕМ буте createShellsFromCache поднимет их до заморозки реестра CreativeModeTab.
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
	}
	
	// В neo нет числовых ID блоков/предметов, поэтому нет и neo-аналога FMLModIdMappingEvent — метод
	// не подписан ни на одну шину (не вызывается автоматически, но остаётся 1:1 доступным вручную для
	// ICompat.onIDChanging(...), если понадобится на этапе рантайм-parity — см. javadoc
	// gregapi.api.FMLModIdMappingEvent).
	public void onIDChangingEvent(FMLModIdMappingEvent aEvent) {
		// Fixing missing Blocks caused by DragonAPI. The Issue is more complicated but it should fix some part of it.
		// F12 impossible-1:1 (foreign DragonAPI-fix; neo не имеет числовых block-ID вовсе): DragonAPI-фикс завязан на числовой Block.blockRegistry
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
