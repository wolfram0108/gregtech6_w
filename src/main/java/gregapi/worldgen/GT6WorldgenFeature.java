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

package gregapi.worldgen;

import java.util.List;
import java.util.Set;

import gregapi.data.MD;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.RemoveFeaturesBiomeModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * F6 центральный переходник — ЕДИНСТВЕННОЕ место мода, которое регистрирует GT6-ворлдген (жилы/слои/малые
 * руды) в neo. `IWorldGenerator`/`GameRegistry.registerWorldGenerator` удалены движком (было императивное
 * "сгенерируй что хочешь в этом чанке"); замена — data-driven связка `Feature`&lt;C&gt; -&gt;
 * `ConfiguredFeature` -&gt; `PlacedFeature` -&gt; `BiomeModifier` (`decisions/F6-worldgen.md` §1,3).
 *
 * <p>По решению `decisions/F6-worldgen.md` §3.1,§4 GT6-алгоритмы жил/слоёв/малых руд НЕ дробятся на
 * ванильные `OreFeature`-подобные примитивы (они не выражают 4-материальную семантику жилы GT6) — вместо
 * этого используется ОДНА диспетчер-{@code Feature}, тело которой 1:1 воспроизводит прежний вызов
 * {@code IWorldGenerator.generate} (был {@code GT_API_Proxy.generate}, `GT_API_Proxy.java:1456` до этого
 * перехода) и передаёт управление уже существующему {@link GT6WorldGenerator#generate(Level,int,int,boolean)}
 * — тому же диспетчеру измерений/весовому выбору жилы, что и раньше (алгоритм не тронут, только точка входа).
 *
 * <p>Референс сигнатур (НЕ выдумано):
 * <ul>
 * <li>{@code Feature<FC>}, {@code place(FeaturePlaceContext)} — {@code neo-decompiled/.../feature/Feature.java:58,183}.</li>
 * <li>{@code FeaturePlaceContext.level()->WorldGenLevel/origin()->BlockPos} — {@code .../feature/FeaturePlaceContext.java:10-51}.</li>
 * <li>{@code ConfiguredFeature}/{@code PlacedFeature} record-и — {@code .../feature/ConfiguredFeature.java:17}, {@code .../placement/PlacedFeature.java:22}.</li>
 * <li>{@code RegistrySetBuilder}+{@code BootstrapContext}+{@code AddFeaturesBiomeModifier}+{@code DatapackBuiltinEntriesProvider} —
 *     дословный паттерн {@code NeoForge/tests/.../oldtest/world/BiomeModifierTest.java:63-160} (локально).</li>
 * <li>{@code WorldGenLevel.getSeed()}/{@code ServerLevelAccessor.getLevel():ServerLevel} —
 *     {@code .../world/level/WorldGenLevel.java:8}, {@code .../world/level/ServerLevelAccessor.java:9}.</li>
 * </ul>
 *
 * <p>F6 functional-adapted (worldgen работает — дампы 100%; place бриджится через context.level().getLevel(), region-обёртка — caveat): {@link #place} бриджится на существующую {@code Level}-типизированную
 * цепочку {@link GT6WorldGenerator}/{@link WorldgenObject} (которая ожидает полноценный мутабельный
 * {@code Level}, как и оригинальный 1.7.10 post-populate хук) через {@code context.level().getLevel()}.
 * В реальном рантайме фичи вызываются с {@code WorldGenRegion} (не всегда полным {@code ServerLevel}) —
 * `.getLevel()` возвращает истинный {@code ServerLevel}, но обход региона-обёртки означает отсутствие
 * гарантий потокобезопасности на границах чанков при параллельной генерации современного движка. Полная
 * миграция всей цепочки на {@code WorldGenLevel} потребовала бы ретайпа сигнатур во ВСЕХ ~50
 * {@code WorldgenObject}-подклассах (gregapi/worldgen + gregtech/worldgen/*) — вне границ этого перехода
 * (см. отчёт по чекпоинту), сюда же относится сохранение прежних значений/алгоритмов внутри них.
 */
public class GT6WorldgenFeature extends Feature<NoneFeatureConfiguration> {

	/** Центральный DeferredRegister — ЕДИНСТВЕННОЕ место, где GT6 регистрирует Feature-типы в neo. */
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MD.GAPI.mID);

	public static final DeferredHolder<Feature<?>, GT6WorldgenFeature> GT6_WORLDGEN =
		FEATURES.register("gt6_worldgen", GT6WorldgenFeature::new);

	private static final ResourceKey<ConfiguredFeature<?, ?>> GT6_WORLDGEN_CF =
		ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "gt6_worldgen"));
	private static final ResourceKey<PlacedFeature> GT6_WORLDGEN_PF =
		ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "gt6_worldgen"));

	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_OVERWORLD =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "add_gt6_worldgen_overworld"));
	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_NETHER =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "add_gt6_worldgen_nether"));
	private static final ResourceKey<BiomeModifier> ADD_GT6_WORLDGEN_END =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "add_gt6_worldgen_end"));
	// F6 §4.2.2: отключение ванильных руд MC26 (GT6 замещает их своими — bedrock-руды + stone-layer перекрытие REPLACEABLE_BLOCKS).
	private static final ResourceKey<BiomeModifier> REMOVE_VANILLA_ORES_OVERWORLD =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "remove_vanilla_ores_overworld"));
	private static final ResourceKey<BiomeModifier> REMOVE_VANILLA_ORES_NETHER =
		ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, Identifier.fromNamespaceAndPath(MD.GAPI.mID, "remove_vanilla_ores_nether"));

	/**
	 * Датаген-набор: CONFIGURED_FEATURE -> PLACED_FEATURE -> BIOME_MODIFIERS, дословно по паттерну
	 * {@code BiomeModifierTest.java:87-117} (RegistrySetBuilder.add + BootstrapContext.register/lookup).
	 * F6 functional (маршрутизация по измерению/биому внутри place; биом-теги = входной фильтр): подключены только 3 ВАНИЛЬНЫХ биом-тега ({@link BiomeTags#IS_OVERWORLD}/
	 * {@code IS_NETHER}/{@code IS_END}, реальные — BiomeTags.java:19-21) — GT6WorldgenFeature#place сам
	 * маршрутизирует по измерению/биому внутри {@link GT6WorldGenerator#generate}, как и оригинальный
	 * {@code IWorldGenerator}, вызывавшийся безусловно для каждого чанка каждого измерения; модовые измерения
	 * (Aether/Twilight/Erebus/...) не имеют здесь собственных биом-тегов без знания их namespace — F10.
	 */
	private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
		.add(Registries.CONFIGURED_FEATURE, ctx -> ctx.register(GT6_WORLDGEN_CF,
			new ConfiguredFeature<>(GT6_WORLDGEN.get(), NoneFeatureConfiguration.INSTANCE)))
		.add(Registries.PLACED_FEATURE, ctx -> ctx.register(GT6_WORLDGEN_PF,
			new PlacedFeature(ctx.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(GT6_WORLDGEN_CF),
				List.of(BiomeFilter.biome()))))
		// ENCHANT: та же датапак-точка (DatapackBuiltinEntriesProvider ниже) регистрирует 4 GT6-чара —
		// центр gregapi/enchants/EnchantsGT6.java (стык, подключён интегратором).
		.add(Registries.ENCHANTMENT, gregapi.enchants.EnchantsGT6::bootstrap)
		// BUG-004: DAMAGE_TYPE — та же датапак-точка регистрирует 13 GT6-типов урона (exploded/spike/heat/frost/...).
		// Раньше DamageSources.bootstrap был написан, но НЕ подключён → типы не в реестре → краш кодека при уроне.
		.add(Registries.DAMAGE_TYPE, gregapi.damage.DamageSources::bootstrap)
		.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
			HolderSet<PlacedFeature> tPlaced = HolderSet.direct(ctx.lookup(Registries.PLACED_FEATURE).getOrThrow(GT6_WORLDGEN_PF));
			ctx.register(ADD_GT6_WORLDGEN_OVERWORLD, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD), tPlaced, Decoration.UNDERGROUND_ORES));
			ctx.register(ADD_GT6_WORLDGEN_NETHER, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_NETHER), tPlaced, Decoration.UNDERGROUND_ORES));
			ctx.register(ADD_GT6_WORLDGEN_END, new AddFeaturesBiomeModifier(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_END), tPlaced, Decoration.UNDERGROUND_ORES));
			// F6 §4.2.2: убрать ванильные руды MC26 (allSteps — авторитетная сигнатура javap RemoveFeaturesBiomeModifier). Ключи — OrePlacements (одна фича покрывает stone+deepslate-вариант руды).
			var tPF = ctx.lookup(Registries.PLACED_FEATURE);
			ctx.register(REMOVE_VANILLA_ORES_OVERWORLD, RemoveFeaturesBiomeModifier.allSteps(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_OVERWORLD),
				HolderSet.direct(
					tPF.getOrThrow(OrePlacements.ORE_COAL_UPPER), tPF.getOrThrow(OrePlacements.ORE_COAL_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_IRON_UPPER), tPF.getOrThrow(OrePlacements.ORE_IRON_MIDDLE), tPF.getOrThrow(OrePlacements.ORE_IRON_SMALL),
					tPF.getOrThrow(OrePlacements.ORE_GOLD), tPF.getOrThrow(OrePlacements.ORE_GOLD_LOWER), tPF.getOrThrow(OrePlacements.ORE_GOLD_EXTRA),
					tPF.getOrThrow(OrePlacements.ORE_REDSTONE), tPF.getOrThrow(OrePlacements.ORE_REDSTONE_LOWER),
					tPF.getOrThrow(OrePlacements.ORE_DIAMOND), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_MEDIUM), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_LARGE), tPF.getOrThrow(OrePlacements.ORE_DIAMOND_BURIED),
					tPF.getOrThrow(OrePlacements.ORE_LAPIS), tPF.getOrThrow(OrePlacements.ORE_LAPIS_BURIED),
					tPF.getOrThrow(OrePlacements.ORE_COPPER), tPF.getOrThrow(OrePlacements.ORE_COPPER_LARGE),
					tPF.getOrThrow(OrePlacements.ORE_EMERALD))));
			ctx.register(REMOVE_VANILLA_ORES_NETHER, RemoveFeaturesBiomeModifier.allSteps(
				ctx.lookup(Registries.BIOME).getOrThrow(BiomeTags.IS_NETHER),
				HolderSet.direct(
					tPF.getOrThrow(OrePlacements.ORE_QUARTZ_NETHER), tPF.getOrThrow(OrePlacements.ORE_QUARTZ_DELTAS),
					tPF.getOrThrow(OrePlacements.ORE_GOLD_NETHER),
					tPF.getOrThrow(OrePlacements.ORE_ANCIENT_DEBRIS_LARGE), tPF.getOrThrow(OrePlacements.ORE_ANCIENT_DEBRIS_SMALL))));
		});

	public GT6WorldgenFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	/**
	 * Диспетчер-Feature (`decisions/F6-worldgen.md` §4) — ЧИСТАЯ АРХИТЕКТУРА (2026-07-17).
	 *
	 * <p>{@link GT6WorldGenerator} и вся цепочка {@link WorldgenObject}-подклассов переведены с {@code Level} на
	 * {@code WorldGenLevel}/{@code LevelAccessor} (централизованно через god-класс {@code WD} — как и у Грегориуса,
	 * в одном месте). Благодаря этому генерация идёт ПРЯМО в стадии FEATURES по {@code context.level()}
	 * ({@code WorldGenRegion}: доступ к центральному чанку + уже-загруженным соседям, {@code getChunk}/{@code setBlock}
	 * БЕЗ форс-генерации/{@code CompletableFuture.join}).
	 *
	 * <p>⛔ Прежний DEADLOCK снят В КОРНЕ: серверно-тиковый обход ({@code onServerTick}→{@code ServerLevel.getChunk}
	 * ТЕКУЩЕГО генерируемого чанка → {@code join} → чанк ждёт сам себя → вечное зависание входа в мир) БОЛЬШЕ НЕ
	 * СУЩЕСТВУЕТ — генерация в законном слоте движка (Feature.place на регионе) реентранси не создаёт. Точка входа 1:1
	 * с 1.7.10 post-populate: {@code generate(world, blockX, blockZ)}.
	 */
	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		net.minecraft.core.BlockPos tOrigin = context.origin();
		GT6WorldGenerator.generate(context.level(), tOrigin.getX(), tOrigin.getZ(), false);
		return true;
	}

	private void onGatherData(GatherDataEvent.Client aEvent) {
		aEvent.getGenerator().addProvider(true, (DataProvider.Factory<DatapackBuiltinEntriesProvider>) aOutput ->
			new DatapackBuiltinEntriesProvider(aOutput, aEvent.getLookupProvider(), BUILDER, Set.of(MD.GAPI.mID)));
	}

	/** F6: центральная точка подписки, вызывается ОДИН раз из {@code GT_API}-конструктора (тот же мод-бас,
	 *  на который уже подписаны {@code ITEMS}/{@code BLOCKS} — F12, `GT_API.java`). */
	public static void register(IEventBus aModBus) {
		FEATURES.register(aModBus);
		aModBus.addListener(GT6WorldgenFeature::onGatherDataStatic);
		aModBus.addListener(GT6WorldgenFeature::onRegisterSpawnPlacements);
		// F6-worldgen: сама ГЕНЕРАЦИЯ руд/слоёв/деревьев теперь в Feature.place (стадия FEATURES, WorldGenLevel) — серверно-тиковый
		// обход СНЯТ. На game-шине остаётся ТОЛЬКО load-реконструкция MTE-стабов (отдельный механизм, F-tileentity-construction):
		// ChunkEvent.Load ловит стабы → server-tick заменяет реальными MTE (FULL-чанк, setBlockEntity безопасен вне save-цикла).
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkLoad);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkUnload);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkWatch);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onServerTick);
		// КРИТ (второй мир виснет): static-очереди worldgen (STUB_QUEUE/CLIENT_STUB_QUEUE/WORLDGEN_MTE/PENDING_SYNC) держат
		// ChunkReq со ссылкой на LEVEL и BlockEntity ПЕРВОГО мира. При выходе они НЕ очищались → второй мир: drainStubs
		// обрабатывает stale-ChunkReq с мёртвым level → getChunk на нём виснет → freeze после ~9 чанков. Чистим на остановке
		// сервера (между мирами singleplayer). BlockRiver-статик тоже сбрасываем (PLACEMENT_ALLOWED — не переносить в новый мир).
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent aEvent) -> {
			STUB_QUEUE.clear();
			CLIENT_STUB_QUEUE.clear();
			WORLDGEN_MTE.clear();
			PENDING_SYNC.clear();
			gregtech.blocks.fluids.BlockRiver.PLACEMENT_ALLOWED = false;
		});
		registerWorldgenStressProbe();
	}

	// R1-живность: GT6-вода (River/Ocean/Swamp) заместила Blocks.WATER своим блоком → vanilla water-спаун молчит, т.к.
	// хардкодит is(Blocks.WATER) (WaterAnimal.checkSurfaceWaterAnimalSpawnRules:78). Оригинал GT6 1.7.10 не имел проблемы
	// (спаун шёл по Material.water). Каноничный neo-мост: RegisterSpawnPlacementsEvent c Operation.OR добавляет водным
	// мобам predicate, где верх/низ проверяются по ТЕГУ FluidTags.WATER (GT6-вода его несёт через getFluidState), а не по
	// конкретному Blocks.WATER. OR не ломает vanilla-спаун (в vanilla-воде работает исходный predicate) — только расширяет.
	public static void onRegisterSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent aEvent) {
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.COD);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.SALMON);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.PUFFERFISH);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.TROPICAL_FISH);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.SQUID);
		addGT6WaterSpawn(aEvent, net.minecraft.world.entity.EntityType.GLOW_SQUID);
	}

	private static <T extends net.minecraft.world.entity.Entity> void addGT6WaterSpawn(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent aEvent, net.minecraft.world.entity.EntityType<T> aType) {
		aEvent.register(aType, GT6WorldgenFeature::gt6WaterSpawnPredicate, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.OR);
	}

	// Копия vanilla WaterAnimal.checkSurfaceWaterAnimalSpawnRules (seaLevel-13..seaLevel), НО above по тегу FluidTags.WATER
	// (GT6-вода удовлетворяет) вместо getBlockState(above).is(Blocks.WATER). Below и так был по тегу — не трогаем.
	private static <T extends net.minecraft.world.entity.Entity> boolean gt6WaterSpawnPredicate(net.minecraft.world.entity.EntityType<T> aType, net.minecraft.world.level.ServerLevelAccessor aLevel, net.minecraft.world.entity.EntitySpawnReason aReason, net.minecraft.core.BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		int tSea = aLevel.getSeaLevel();
		return aPos.getY() >= tSea - 13 && aPos.getY() <= tSea
			&& aLevel.getFluidState(aPos.below()).is(net.minecraft.tags.FluidTags.WATER)
			&& aLevel.getFluidState(aPos.above()).is(net.minecraft.tags.FluidTags.WATER);
	}

	// ── F6-worldgen АВТОНОМНАЯ headless-приёмка (dev-диагностика; гейт: файл run/wgstress.flag, вне флага НЕ активна) ──
	// Форс-генерирует сетку чанков на dedicated-сервере → реально бежит GT6-Feature + свет-движок (та же цепочка, что вешала
	// клиент) → дампует руды/камни/флюиды/MTE + резолв материала руды (server-side BE mMetaData) в gregtech.log. Reproduce+verify
	// дедлока И проверка генерации/материала БЕЗ клиента и ручных миров. Дедлок жив → сервер виснет до TIMEOUT (jstack); снят → «DONE».
	private static final int STRESS_R = 8;
	private static int sStressTarget = 0, sStressTick = 0;
	private static void registerWorldgenStressProbe() {
		if (!gregapi.data.CS.probeFlag("wgstress.flag")) return;
		gregapi.data.CS.OUT.println("[GT6-WGSTRESS] флаг найден — headless worldgen-приёмка активна (R=" + STRESS_R + ")");
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent aEvent) -> {
			net.minecraft.server.level.ServerLevel tLvl = aEvent.getServer().overworld();
			for (int cx=-STRESS_R; cx<=STRESS_R; cx++) for (int cz=-STRESS_R; cz<=STRESS_R; cz++) tLvl.setChunkForced(cx, cz, true);
			sStressTarget = (2*STRESS_R+1)*(2*STRESS_R+1);
			gregapi.data.CS.OUT.println("[GT6-WGSTRESS] форс " + sStressTarget + " чанков (±" + STRESS_R + "), генерирую worldgen...");
		});
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post aEvent) -> {
			if (sStressTarget <= 0) return;
			if (++sStressTick % 20 != 0) return;
			net.minecraft.server.level.ServerLevel tLvl = aEvent.getServer().overworld();
			int tDone = 0;
			for (int cx=-STRESS_R; cx<=STRESS_R; cx++) for (int cz=-STRESS_R; cz<=STRESS_R; cz++) if (tLvl.getChunkSource().getChunkNow(cx, cz) != null) tDone++;
			gregapi.data.CS.OUT.println("[GT6-WGSTRESS] сгенерено " + tDone + "/" + sStressTarget);
			if (tDone >= sStressTarget) { dumpWorldgenStress(tLvl); sStressTarget = 0; aEvent.getServer().halt(false); } // halt: детерминированное завершение прогона (лог полный, без внешнего kill)
			else if (sStressTick > 20*150) { gregapi.data.CS.OUT.println("[GT6-WGSTRESS] TIMEOUT на " + tDone + "/" + sStressTarget + " — вероятен дедлок (снять jstack)"); sStressTarget = 0; aEvent.getServer().halt(false); }
		});
	}
	private static void dumpWorldgenStress(net.minecraft.server.level.ServerLevel aLvl) {
		int tOre=0, tOreMat=0, tOreNull=0, tStone=0, tFluid=0, tMTE=0, tBedrockOre=0;
		java.util.HashMap<String,Integer> tMats = new java.util.HashMap<>();
		java.util.HashMap<String,Integer> tMTEs = new java.util.HashMap<>(); // разбивка MTE по классам (флуид-спринги/rocks/resin-holes)
		int tMinY=aLvl.getMinY(), tMaxY=aLvl.getSeaLevel()+8;
		net.minecraft.core.BlockPos.MutableBlockPos tM = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (int cx=-STRESS_R; cx<=STRESS_R; cx++) for (int cz=-STRESS_R; cz<=STRESS_R; cz++)
			for (int lx=0; lx<16; lx++) for (int lz=0; lz<16; lz++) for (int y=tMinY; y<=tMaxY; y++) {
				tM.set((cx<<4)+lx, y, (cz<<4)+lz);
				net.minecraft.world.level.block.Block tB = aLvl.getBlockState(tM).getBlock();
				if (tB instanceof gregapi.block.prefixblock.PrefixBlock tPB) {
					tOre++;
					if (tB == gregapi.data.CS.BlocksGT.oreBedrock || tB == gregapi.data.CS.BlocksGT.oreSmallBedrock) tBedrockOre++;
					gregapi.oredict.OreDictMaterial tMat = tPB.getMetaMaterial(aLvl.getBlockEntity(tM));
					if (tMat==null) tOreNull++; else { tOreMat++; tMats.merge(tMat.mNameInternal,1,Integer::sum); }
				} else if (tB instanceof gregapi.block.metatype.BlockStones) tStone++;
				else if (tB instanceof gregapi.block.fluid.BlockBaseFluid) tFluid++;
				else if (tB instanceof gregapi.block.multitileentity.MultiTileEntityBlock) {
					tMTE++;
					net.minecraft.world.level.block.entity.BlockEntity tBE = aLvl.getBlockEntity(tM);
					String tName; try { tName = ((gregapi.block.multitileentity.MultiTileEntityBlockInternal)tB).getUnlocalizedName(); } catch (Throwable e) { tName = tB.getClass().getSimpleName(); }
					tMTEs.merge((tBE==null?"NULL-BE ":"OK ")+tName+(tBE==null?"":" ["+tBE.getClass().getSimpleName()+"]"), 1, Integer::sum);
				}
			}
		gregapi.data.CS.OUT.println("[GT6-WGSTRESS] === ДАМП (±"+STRESS_R+" чанков, Y["+tMinY+".."+tMaxY+"]) ===");
		gregapi.data.CS.OUT.println("[GT6-WGSTRESS] РУДЫ total="+tOre+" материал-резолв(сервер)="+tOreMat+" null="+tOreNull+" ИЗ НИХ бедрок-жилы="+tBedrockOre);
		gregapi.data.CS.OUT.println("[GT6-WGSTRESS] камень="+tStone+" флюид-блоки="+tFluid+" MTE="+tMTE);
		tMTEs.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue()).forEach(e-> gregapi.data.CS.OUT.println("[GT6-WGSTRESS]   MTE "+e.getKey()+"="+e.getValue()));
		tMats.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue()).limit(15).forEach(e-> gregapi.data.CS.OUT.println("[GT6-WGSTRESS]   мат "+e.getKey()+"="+e.getValue()));
		gregapi.data.CS.OUT.println("[GT6-WGSTRESS] DONE");
	}

	private static void onGatherDataStatic(GatherDataEvent.Client aEvent) {
		GT6_WORLDGEN.get().onGatherData(aEvent);
	}

	// F-tileentity-construction (load-реконструкция MTE-стабов): очередь чанков для замены TileEntityLoaderStub реальным MTE.
	// Стабы приходят на ЛЮБОЙ load чанка с MTE (neo подменяет не-PrefixBlock GT6-MTE общим MTE_TYPE → TileEntityLoaderStub при
	// чтении NBT). ДВА уровня: серверный (STUB_QUEUE, дренаж server-tick) И КЛИЕНТСКИЙ (CLIENT_STUB_QUEUE, дренаж client-tick).
	// Без клиентской реконструкции MTE-BE на клиенте остаётся стабом → не IRenderedBlockObject → passRenderingToObject=null →
	// getRenderPasses=0 → блок НЕ рисуется (прозрачный: камни/палки/флюид-источники/машины). Реконструкция ЕДИНА (reconstructChunkMTEs).
	private record ChunkReq(net.minecraft.world.level.Level level, int blockX, int blockZ) {}
	private static final java.util.Queue<ChunkReq> STUB_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();
	public  static final java.util.Queue<ChunkReq> CLIENT_STUB_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();

	// F6-worldgen КРОСС-ЧАНК BE-ПЕРСИСТ: worldgen кладёт MTE и в СОСЕДНИЕ чанки региона (листва деревьев, бедрок-фичи
	// спрингов). В модели neo Feature.place BE-запись в уже-финализированный сосед-LevelChunk НЕ персистит (центр-чанк
	// ProtoChunk персистит). Диагностика: srvBE=null постоянно для leaves@surface / rock@Y-64 / спрингов. Решение:
	// на placeBlock (worldgen) регистрируем (реестр,pos,id,nbt) по ЧАНКУ MTE; когда ЭТОТ чанк сам финализируется
	// (ChunkEvent.Load), переприкрепляем BE к настоящему LevelChunk — тогда привязка держится и синкается клиенту.
	private static final java.util.Map<Long, java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity>> WORLDGEN_MTE = new java.util.concurrent.ConcurrentHashMap<>();
	// PENDING_SYNC: свеже-записанные worldgen-MTE для НЕМЕДЛЕННОГО синка на server-tick тем, кто УЖЕ трекает чанк. Ловит
	// кросс-чанк MTE (redstonelight-декор), добавленные в УЖЕ-отправленный игроку чанк (onChunkWatch по нему уже прошёл).
	private static final java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity> PENDING_SYNC = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static long chunkKey(int aCX, int aCZ) {return ((long)aCX << 32) | (aCZ & 0xFFFFFFFFL);}

	/** Вызывается из {@code WD.te} при worldgen-привязке ЛЮБОГО MTE-BE (aWorld не Level — WorldGenRegion). WD.te —
	 *  единственная центральная точка привязки MTE-BE (placeBlock И прямые пути идут через неё) → captures ВСЁ. Запись
	 *  по ЧАНКУ нужна, чтобы на {@link #onChunkWatch отправке чанка игроку} синкнуть ему эти worldgen-MTE (сами они не
	 *  синкаются: getUpdatePacket=null, а GT6-getClientDataPacket зовётся лишь при gameplay) — иначе клиент их не видит. */
	public static void recordWorldgenMTE(net.minecraft.world.level.block.entity.BlockEntity aTileEntity) {
		if (aTileEntity == null) return;
		net.minecraft.core.BlockPos tPos = aTileEntity.getBlockPos();
		WORLDGEN_MTE.computeIfAbsent(chunkKey(tPos.getX() >> 4, tPos.getZ() >> 4), k -> new java.util.concurrent.ConcurrentLinkedQueue<>()).add(aTileEntity);
		PENDING_SYNC.add(aTileEntity);
	}

	/** Немедленный синк свеже-записанных worldgen-MTE (server-tick): если ИХ чанк загружен — переприкрепить-если-потерян +
	 *  sendClientData всем в радиусе. Для чанка, ещё не отправленного игроку, это no-op (некому в радиусе) — его подхватит
	 *  onChunkWatch при отправке. Ловит именно кросс-чанк MTE в УЖЕ-отправленном чанке. Обрабатываем один раз (drain), квота. */
	private static void drainPendingSync(net.minecraft.server.MinecraftServer aServer) {
		net.minecraft.world.level.block.entity.BlockEntity tBE; int tN = 0;
		while (tN < 256 && (tBE = PENDING_SYNC.poll()) != null) { tN++;
			try {
				net.minecraft.core.BlockPos tPos = tBE.getBlockPos();
				int tCX = tPos.getX() >> 4, tCZ = tPos.getZ() >> 4;
				for (net.minecraft.server.level.ServerLevel tL : aServer.getAllLevels()) {
					net.minecraft.world.level.chunk.LevelChunk tC = tL.getChunkSource().getChunkNow(tCX, tCZ);
					if (tC == null) continue;
					if (!(tC.getBlockState(tPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) break;
					net.minecraft.world.level.block.entity.BlockEntity tCur = tC.getBlockEntity(tPos);
					if (tCur == null) { tBE.clearRemoved(); tL.setBlockEntity(tBE); tCur = tBE; }
					if (tCur instanceof gregapi.tileentity.base.TileEntityBase03TicksAndSync tSync) tSync.sendClientData(true, null);
					break;
				}
			} catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	private static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		if (aEvent.getLevel() instanceof ServerLevel tLevel) {
			// ОТКЛАДЫВАЕМ на server-tick — подмена стаба (setBlockEntity) ВО ВРЕМЯ ChunkEvent.Load (идёт и при save/shutdown) давала
			// реентранси-зависание; server-tick.Post при save не выполняется. Стабы на КАЖДОЙ загрузке не-PrefixBlock MTE → sweep каждый load.
			STUB_QUEUE.add(new ChunkReq(tLevel, tPos.getMinBlockX(), tPos.getMinBlockZ()));
		} else if (aEvent.getLevel() instanceof net.minecraft.world.level.Level tLevel && tLevel.isClientSide()) {
			// КЛИЕНТ: тот же механизм — стаб→настоящий MTE, но на client-tick (иначе MTE прозрачны). ClientLevel-класс не трогаем
			// в common-коде (isClientSide-гейт); дренаж — GT_API_Proxy_Client.onClientMTEReconstruct.
			CLIENT_STUB_QUEUE.add(new ChunkReq(tLevel, tPos.getMinBlockX(), tPos.getMinBlockZ()));
		}
	}

	private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post aEvent) {
		drainStubs(STUB_QUEUE, 16);
		drainPendingSync(aEvent.getServer());
	}

	/** КЛИЕНТ-СИНК worldgen-MTE. КОРЕНЬ прозрачности: сервер ИМЕЕТ BE worldgen-MTE (source/rock/redstonelight), но клиент
	 *  НЕ получает (chunk-пакет не несёт MTE-BE — getUpdatePacket=null; а GT6-синк getClientDataPacket зовётся только при
	 *  gameplay-размещении/апдейте, не при worldgen). Здесь ловим МОМЕНТ отправки чанка игроку (ChunkWatchEvent.Sent) и
	 *  синкаем ему все worldgen-MTE этого чанка (+ переприкрепляем, если BE всё же потерялся кросс-чанк). Точный тайминг,
	 *  без per-tick overhead. См. sweepWorldgenMTE удалён — был пустой (всё skip-hasBE). */
	private static void onChunkWatch(net.neoforged.neoforge.event.level.ChunkWatchEvent.Sent aEvent) {
		net.minecraft.world.level.ChunkPos tCP = aEvent.getPos();
		int tCX = tCP.getMinBlockX() >> 4, tCZ = tCP.getMinBlockZ() >> 4;
		java.util.Queue<net.minecraft.world.level.block.entity.BlockEntity> tQueue = WORLDGEN_MTE.get(chunkKey(tCX, tCZ));
		if (tQueue == null || tQueue.isEmpty()) return;
		net.minecraft.server.level.ServerLevel tLevel = aEvent.getLevel();
		net.minecraft.server.level.ServerPlayer tPlayer = aEvent.getPlayer();
		net.minecraft.world.level.chunk.LevelChunk tChunk = tLevel.getChunkSource().getChunkNow(tCX, tCZ);
		if (tChunk == null) return;
		for (net.minecraft.world.level.block.entity.BlockEntity tBE : tQueue) {
			try {
				net.minecraft.core.BlockPos tPos = tBE.getBlockPos();
				if (!(tChunk.getBlockState(tPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) continue;
				net.minecraft.world.level.block.entity.BlockEntity tCur = tChunk.getBlockEntity(tPos);
				if (tCur == null) { tBE.clearRemoved(); tLevel.setBlockEntity(tBE); tCur = tBE; } // BE всё же потерялся кросс-чанк — переприкрепить
				// СИНК клиенту, получившему чанк: worldgen-MTE не синкаются авто (getUpdatePacket=null) → шлём GT6-пакет данных (PacketSyncDataIDs → клиент создаёт BE).
				// N5-прозрачность: было только TileEntityBase03TicksAndSync (ТИКУЮЩИЕ) → non-ticking worldgen-MTE (камешки Rock/палки —
				// TileEntityBase03MultiTileEntities, getUpdateTag=0) клиенту не синкались → прозрачны. Оба корня реализуют общий
				// ITileEntitySynchronising.sendUpdateToPlayer (Base02Sync:99 и TicksAndSync:101 → sendClientData) — синкаем ЧЕРЕЗ него
				// целевому игроку, получившему чанк (точнее broadcast). Покрывает обе ветки MTE одним централизованным вызовом.
				if (tCur instanceof gregapi.tileentity.ITileEntitySynchronising tSync) tSync.sendUpdateToPlayer(tPlayer);
			} catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	/** Выгрузка чанка → снять его записи (при перезагрузке MTE придёт стабом, его подхватит stub-реконструкция). */
	private static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload aEvent) {
		if (!(aEvent.getLevel() instanceof net.minecraft.server.level.ServerLevel)) return;
		net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
		WORLDGEN_MTE.remove(chunkKey(tPos.getMinBlockX() >> 4, tPos.getMinBlockZ() >> 4));
	}

	/** Клиентский дренаж (вызывается из GT_API_Proxy_Client на ClientTickEvent) — реконструкция MTE-стабов на ClientLevel. */
	public static void drainClientStubs() {drainStubs(CLIENT_STUB_QUEUE, 32);}

	private static void drainStubs(java.util.Queue<ChunkReq> aQueue, int aQuota) {
		ChunkReq tReq; int tM = 0;
		while (tM < aQuota && (tReq = aQueue.poll()) != null) {
			try { reconstructChunkMTEs(tReq.level(), tReq.blockX() >> 4, tReq.blockZ() >> 4); tM++; }
			catch (Throwable e) { e.printStackTrace(gregapi.data.CS.ERR); }
		}
	}

	/** F-tileentity-construction (load-реконструкция): пройти BE загруженного чанка, заменить каждый {@link gregapi.tileentity.base.TileEntityLoaderStub}
	 *  (пустышку, которой neo подменил GT6-MTE при чтении NBT) реальным MTE через реестр. Отложено на server-tick (чанк FULL, setBlockEntity безопасен). */
	public static void reconstructChunkMTEs(net.minecraft.world.level.Level aLevel, int aChunkX, int aChunkZ) {
		// getChunk(cx,cz,FULL,false) — неблокирующий на main-thread (server-tick И client-tick), работает и для ClientLevel
		// (в отличие от server-only getChunkSource().getChunkNow). null/не-FULL → пропуск (реконструируем при следующем load).
		net.minecraft.world.level.chunk.ChunkAccess tCA = aLevel.getChunk(aChunkX, aChunkZ, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
		if (!(tCA instanceof net.minecraft.world.level.chunk.LevelChunk tChunk)) return;
		java.util.List<net.minecraft.world.level.block.entity.BlockEntity> tStubs = null;
		for (net.minecraft.world.level.block.entity.BlockEntity tBE : tChunk.getBlockEntities().values())
			if (tBE instanceof gregapi.tileentity.base.TileEntityLoaderStub) {(tStubs == null ? tStubs = new java.util.ArrayList<>() : tStubs).add(tBE);}
		if (tStubs != null) for (net.minecraft.world.level.block.entity.BlockEntity tBE : tStubs) reconstructMTE(aLevel, (gregapi.tileentity.base.TileEntityLoaderStub)tBE);
		// (кросс-чанк BE-персист переприкрепляется server-tick sweep'ом sweepWorldgenMTE — здесь только stub-реконструкция)
	}

	/** Собрать реальный MTE из захваченного стабом NBT (reg/id) и заменить им стаб. pos-канал getNewTileEntityContainer даёт позицию из pos стаба. Level (сервер И клиент). */
	public static void reconstructMTE(net.minecraft.world.level.Level aLevel, gregapi.tileentity.base.TileEntityLoaderStub aStub) {
		net.minecraft.nbt.CompoundTag tNBT = aStub.mLoadedNBT;
		if (tNBT == null) return;
		short tReg = tNBT.getShort(gregapi.data.CS.NBT_MTE_REG).orElse((short)0);
		short tID  = tNBT.getShort(gregapi.data.CS.NBT_MTE_ID ).orElse((short)0);
		gregapi.block.multitileentity.MultiTileEntityRegistry tRegistry = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry(tReg);
		if (tRegistry == null) return;
		net.minecraft.core.BlockPos tPos = aStub.getBlockPos();
		// блок-гейт (корень mismatch-флуда): BE-сирота (блок в позиции не MTE — затёрт/air) НЕ реконструируется,
		// стаб снимается → мир самоочищается от сирот вместо вечного «Block state mismatch … != air» при каждой загрузке.
		if (!(aLevel.getBlockState(tPos).getBlock() instanceof gregapi.block.multitileentity.MultiTileEntityBlock)) {
			aLevel.removeBlockEntity(tPos);
			long tN = sOrphansCleaned.incrementAndGet();
			if (tN <= 20 || tN % 500 == 0) gregapi.data.CS.OUT.println("[GT6-WG] BE-сирота вычищен @" + tPos.toShortString() + " (блок=" + aLevel.getBlockState(tPos).getBlock() + "), всего=" + tN);
			return;
		}
		gregapi.block.multitileentity.MultiTileEntityContainer tContainer = tRegistry.getNewTileEntityContainer(aLevel, tPos.getX(), tPos.getY(), tPos.getZ(), tID, tNBT);
		if (tContainer == null || tContainer.mTileEntity == null) return;
		aLevel.setBlockEntity(tContainer.mTileEntity); // pos-канал → реальная pos → крепит на своё место, заменяя стаб
	}
	private static final java.util.concurrent.atomic.AtomicLong sOrphansCleaned = new java.util.concurrent.atomic.AtomicLong();
}
