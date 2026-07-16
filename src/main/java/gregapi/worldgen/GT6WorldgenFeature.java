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
	 * Диспетчер-Feature (`decisions/F6-worldgen.md` §4).
	 *
	 * ⛔ F6-WORLDGEN DEADLOCK (обезврежено, ВРЕМЕННО no-op — 2026-07-16): {@link GT6WorldGenerator} — код 1.7.10,
	 * который во время placement фичи зовёт {@code Level.getChunk(cx,cz)} на {@code ServerLevel} (GT6WorldGenerator:66)
	 * для ТЕКУЩЕГО (ещё генерируемого) чанка. neo staged async chunk-gen: {@code ServerLevel.getChunk} форсирует статус
	 * FULL и делает {@code CompletableFuture.join} → чанк ждёт САМ СЕБЯ → вечный DEADLOCK worldgen-потока → Server thread
	 * виснет на {@code ServerChunkCache.getChunk} → вход в мир НАВСЕГДА зависает (пойман jstack'ом зависшего клиента).
	 * Датаген инжектит фичу в биомы (biome_modifier), поэтому она реально бежала и вешала мир.
	 *
	 * ПРАВИЛЬНЫЙ ФИКС (отдельный F6-порт): перевести {@link GT6WorldGenerator}+все {@code WorldgenObject} с {@code Level}
	 * на {@code WorldGenLevel}/{@code LevelAccessor} ({@code context.level()}) — тогда {@code getChunk}/{@code setBlock}
	 * идут через {@code WorldGenRegion} (кэш текущий+соседи, БЕЗ форс-генерации/join). До этого — no-op, чтобы мод был
	 * запускаем/тестируем (руды GT6 пока не спавнятся; контролируемая, видимая отложенность, не свалка).
	 */
	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		// F6-worldgen deadlock: GT6WorldGenerator.generate(ServerLevel) висит на getChunk текущего чанка (см. javadoc).
		// no-op до порта GT6WorldGenerator на WorldGenLevel. НЕ звать generate() на ServerLevel из placement.
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
		// F6-worldgen (реальный порт семантики 1.7.10 IWorldGenerator post-populate): подписка на game-шину.
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onChunkLoad);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(GT6WorldgenFeature::onServerTick);
	}

	private static void onGatherDataStatic(GatherDataEvent.Client aEvent) {
		GT6_WORLDGEN.get().onGatherData(aEvent);
	}

	// F6-worldgen (faithful, БЕЗ каскада): GT6WorldGenerator требует настоящий Level (TE-руды/саженцы/делегаты/
	// updateNeighbors), а neo Feature даёт WorldGenRegion → ServerLevel.getChunk(текущий генерируемый чанк) делает
	// CompletableFuture.join и виснет (deadlock). РЕШЕНИЕ 1:1 с 1.7.10 IWorldGenerator (post-populate): ловим
	// ChunkEvent.Load(isNewChunk) и ОТКЛАДЫВАЕМ на следующий server-tick (javadoc ChunkEvent.Load: "interactions with
	// the level must be delayed until the next game tick to prevent deadlocking") → к тику чанк ПОЛНОСТЬЮ сгенерирован
	// (getChunk=FULL мгновенно, нет deadlock), доступен настоящий ServerLevel. GT6WorldGenerator НЕ тронут (Level-цепь 1:1).
	private record ChunkReq(ServerLevel level, int blockX, int blockZ) {}
	private static final java.util.Queue<ChunkReq> CHUNK_QUEUE = new java.util.concurrent.ConcurrentLinkedQueue<>();

	private static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		if (aEvent.isNewChunk() && aEvent.getLevel() instanceof ServerLevel tLevel) {
			net.minecraft.world.level.ChunkPos tPos = aEvent.getChunk().getPos();
			CHUNK_QUEUE.add(new ChunkReq(tLevel, tPos.getMinBlockX(), tPos.getMinBlockZ()));
		}
	}

	private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post aEvent) {
		// Троттлинг: не более N чанков за тик (полный GT6-ворлдген чанка тяжёл; спавн грузит сотни чанков разом →
		// без лимита тик слишком долгий → watchdog). Очередь дренится за несколько тиков.
		ChunkReq tReq; int tN = 0;
		while (tN < 8 && (tReq = CHUNK_QUEUE.poll()) != null) {
			try {
				GT6WorldGenerator.generate(tReq.level(), tReq.blockX(), tReq.blockZ(), false);
				// F3-render #3 (стухший меш): post-populate пишет блоки через WD.set(chunk) БЕЗ клиент-нотификации → уже
				// отправленный клиенту чанк (спавн/быстрое движение) показывает довордген-ваниль. Помечаем чанк на повторную
				// отправку отслеживающим игрокам (для чанков впереди игрока список getPlayers пуст → ноль стоимости).
				resendChunk(tReq.level(), tReq.blockX() >> 4, tReq.blockZ() >> 4);
				tN++;
			} catch (Throwable e) {
				e.printStackTrace(gregapi.data.CS.ERR);
			}
		}
	}

	/** F3-render #3: заставить клиентов, уже отслеживающих этот чанк, перезагрузить его (после того как GT6-worldgen дописал блоки). */
	private static void resendChunk(ServerLevel aLevel, int aChunkX, int aChunkZ) {
		net.minecraft.world.level.chunk.LevelChunk tChunk = aLevel.getChunkSource().getChunkNow(aChunkX, aChunkZ);
		if (tChunk == null) return;
		for (net.minecraft.server.level.ServerPlayer tPlayer : aLevel.getChunkSource().chunkMap.getPlayers(tChunk.getPos(), false)) {
			tPlayer.connection.chunkSender.markChunkPendingToSend(tChunk);
		}
	}
}
