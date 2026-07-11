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
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.AddFeaturesBiomeModifier;
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
 * <p>PORT-TODO(F6, WorldGenRegion): {@link #place} бриджится на существующую {@code Level}-типизированную
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

	/**
	 * Датаген-набор: CONFIGURED_FEATURE -> PLACED_FEATURE -> BIOME_MODIFIERS, дословно по паттерну
	 * {@code BiomeModifierTest.java:87-117} (RegistrySetBuilder.add + BootstrapContext.register/lookup).
	 * PORT-TODO(F6, теги измерений): подключены только 3 ВАНИЛЬНЫХ биом-тега ({@link BiomeTags#IS_OVERWORLD}/
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
		});

	public GT6WorldgenFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	/**
	 * Диспетчер-Feature (`decisions/F6-worldgen.md` §4): тело 1:1 воспроизводит прежний вызов
	 * {@code GT6WorldGenerator.generate(aWorld, aChunkX<<4, aChunkZ<<4, F)} из {@code GT_API_Proxy.generate}
	 * (`GT_API_Proxy.java:1456-1457` до этого перехода) — сам {@link GT6WorldGenerator} не переписан, только
	 * точка вызова. {@code F} (не GalactiCraft) — как в оригинале, дословно, не выдумано.
	 */
	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel tLevel = context.level();
		ServerLevel tServerLevel = tLevel.getLevel();
		int tChunkX = (context.origin().getX() >> 4) << 4;
		int tChunkZ = (context.origin().getZ() >> 4) << 4;
		GT6WorldGenerator.generate(tServerLevel, tChunkX, tChunkZ, false);
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
	}

	private static void onGatherDataStatic(GatherDataEvent.Client aEvent) {
		GT6_WORLDGEN.get().onGatherData(aEvent);
	}
}
