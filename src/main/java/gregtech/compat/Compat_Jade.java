/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregtech.compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;


import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.ToolHandler;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import gregapi.data.CS;
import gregapi.data.CS.ToolsGT;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.util.WD;

/**
 * MODCOMPAT-001 — ИНСТРУМЕНТЫ GT6 В ТУЛТИПЕ JADE («Effective Tool» / «Currently Harvestable»).
 *
 * <p><b>Зачем отдельный плагин, а не одни ванильные теги.</b> Jade определяет «чем добывать» перебором
 * зарегистрированных {@link ToolHandler} — свои он заводит только для ванильных инструментов
 * (кирка/топор/лопата/мотыга/меч/ножницы, {@code VanillaPlugin.registerClient}), и его
 * {@code SimpleToolHandler.test} смотрит на {@code Tool}-компонент предмета и теги блока. У GT6 инструменты
 * СВОИ, и по замеру реестра ими добывается заметная часть мира: гаечный ключ (ВСЕ машины), лом, кусачки,
 * совок, ножницы, меч. Ванильными тегами это невыразимо — ванильного «mineable/wrench» не существует.
 * Поэтому GT6 регистрирует СОБСТВЕННЫЕ {@link ToolHandler} — ровно тот путь, который Jade и предусмотрел
 * ({@code HarvestToolProvider.registerHandler}).
 *
 * <p><b>Источник истины — сам GT6, а не догадка.</b> Какой инструмент нужен блоку, спрашивается его же
 * методом {@code getHarvestTool} (тот самый, что был и в 1.7.10), а сами предметы-инструменты берутся из
 * реестра {@link ToolsGT#list} — того же, которым GT6 проверяет «является ли предмет ключом». Ничего не
 * перечисляется вручную: добавится новый инструмент в GT6 — он появится и в тултипе.
 *
 * <p><b>Мягкая зависимость.</b> Jade подключён только на compile-classpath; в jar игрока его нет. Этот класс
 * находит и грузит сам Jade по аннотации {@link WailaPlugin} — production-код GT6 на него не ссылается,
 * поэтому без установленного Jade он просто никогда не загружается.
 */
@WailaPlugin
public class Compat_Jade implements IWailaPlugin {
	/** GT6-типы инструментов, которыми реально добываются блоки (замер реестра: кроме ванильных
	 *  pickaxe/axe/shovel встречаются именно эти). Ванильные три оставляем Jade — он их уже показывает. */
	private static final String[] GT6_TOOL_TYPES = {
		  CS.TOOL_wrench      // все машины GT6
		, CS.TOOL_crowbar     // рельсы/каркасы
		, CS.TOOL_cutter      // провода
		, CS.TOOL_scoop       // ульи
		, CS.TOOL_shears      // листва/шерсть-подобное
		, CS.TOOL_sword       // паутина и прочее «режется мечом»
		, CS.TOOL_saw         // распиливаемое
		, CS.TOOL_knife       // срезаемое ножом
		, CS.TOOL_hoe         // грядки
		, CS.TOOL_screwdriver
		, CS.TOOL_hammer
		, CS.TOOL_softhammer
		, CS.TOOL_file
		, CS.TOOL_drill
		, CS.TOOL_chisel
		, CS.TOOL_plunger
	};

	/**
	 * BUG-070 п.1 — ВАНИЛЬНЫЕ типы инструментов, для которых GT6 обязан завести СВОИ обработчики.
	 *
	 * <p>Кирку/топор/лопату Jade показывает сам, но судит о них по ванильным тегам ({@code SimpleToolHandler:66}
	 * читает {@code Tool}-компонент и {@code state.is(rule.blocks())}). У GT6 шкала уровней 0..15, а ванильных
	 * ступеней три — поэтому блоки с уровнем выше 3 намеренно оставлены БЕЗ тега {@code mineable/*}
	 * ({@code GT6HarvestTags:80}: иначе ванильная кирка получила бы ложное право на дроп). Для них ванильный
	 * обработчик молчит, список инструментов остаётся пустым — и Jade выходит ДО отрисовки чего бы то ни было
	 * ({@code HarvestToolProvider.getText:129}). Отсюда симптом игрока: «на рудах, где уровня не хватает,
	 * не видно вообще ничего». Спрашивать надо у самого блока, а не у тегов, — это и делает {@link GT6ToolHandler}.
	 */
	private static final String[] VANILLA_TOOL_TYPES = {CS.TOOL_pickaxe, CS.TOOL_axe, CS.TOOL_shovel};

	/** Префикс имени наших обработчиков в карте Jade (ключ карты — String,
	 *  HarvestToolProvider.java:47): отличает свои записи от ванильных при обходе. */
	private static final String NAME_PREFIX = MD.GT.mID + ":";

	@Override
	public void registerClient(IWailaClientRegistration aRegistration) {
		// GT6-типы: ванильного обработчика для них не существует в принципе (нет ни тега, ни предмета)
		for (String tToolType : GT6_TOOL_TYPES   ) HarvestToolProvider.registerHandler(new GT6ToolHandler(tToolType, null));
		// Ванильные типы: подстраховываем ТОЛЬКО там, где обработчик самого Jade промолчал — иначе значок задвоится
		for (String tToolType : VANILLA_TOOL_TYPES) HarvestToolProvider.registerHandler(new GT6ToolHandler(tToolType, tToolType));
		// BUG-070 п.2/п.3 — строка «какой уровень нужен» и «что в руке»: у Jade такой строки нет ни для кого
		aRegistration.registerBlockComponent(GT6HarvestLevelProvider.INSTANCE, Block.class);
		registerFluidCellRestore(aRegistration);
		registerFluidCellReachable(aRegistration);
	}

	/**
	 * КЛЕТКА НЕФТИ/ГАЗА ПОД ПРИЦЕЛОМ (BP-BUG-020: «нефть не показывается в Jade, то же с газом»).
	 *
	 * <p><b>Корень.</b> Витрина показывает то, во что попал ЛУЧ, а луч отбирает клетки-жидкости по
	 * {@code FluidState} ({@code ClipContext.Fluid.canPick}; форма самого блока пуста —
	 * {@code LiquidBlock.getShape:105-107}). Роль {@code NO_ENGINE_FLUID} обязана держать {@code FluidState}
	 * пустым (BP-BUG-003: иначе клетка рисуется дважды), поэтому 4 нефти и газ невидимы ЛЮБОМУ лучу: он
	 * проходит сквозь них и приносит дно лужи — или не приносит ничего. Воды GT6 показываются как раз потому,
	 * что их роль объявляет движку непустой {@code FluidState}.
	 *
	 * <p><b>Что было в 1.7.10.</b> Там отбор делал сам блок: {@code BlockFluidFinite.canCollideCheck(meta,
	 * fullHit)} = {@code fullHit && meta == quantaPerBlock-1} (Forge 1.7.10, {@code BlockFluidFinite.java:50-53}).
	 * Прицел игрока ({@code fullHit=false}) нефть не видел, а луч подсказок ({@code fullHit=true}) видел — и
	 * только ПОЛНУЮ клетку. Канал у блока отобран движком; ответ роли восстановлен
	 * ({@code BlockFluidBaseGT.isFullFluidCell}), а спросить его некому — луч чужой.
	 *
	 * <p><b>Чиним витриной, а не механикой.</b> Дать нефти непустой {@code FluidState} значило бы вернуть
	 * BP-BUG-003 (две геометрии), а непустую {@code getShape} — сделать её кликабельной для ВСЕХ лучей, чего
	 * в 1.7.10 не было. Поэтому досматриваем клетку тем же каналом, которым уже пользуется соседний дефект
	 * витрины, — {@code addRayTraceCallback}: пускаем свой луч движковым обходом
	 * ({@code BlockGetter.traverseBlocks}), берём ПЕРВУЮ клетку-жидкость GT6, невидимую движковому лучу, и
	 * ставим её целью витрины, только если она БЛИЖЕ уже найденной цели (иначе луч «простреливал» бы стены).
	 *
	 * <p><b>Режим жидкостей — не наш, а игрока.</b> Отбор берётся у самого Jade
	 * ({@code IWailaConfig.get().getGeneral().getDisplayFluids()}): {@code NONE} — не вмешиваемся вовсе;
	 * {@code SOURCE_ONLY} — только полные клетки (ровно канон 1.7.10); {@code ANY} — любые;
	 * {@code FALLBACK} — только когда цели нет вовсе (так Jade обращается и с настоящими жидкостями,
	 * {@code RayTracing.java:157-161,189-196}).
	 *
	 * <p><b>Чего не покрывает.</b> Если луч не встретил вообще ничего в пределах досягаемости, Jade выходит
	 * ДО колбэков ({@code WailaTickHandler.java:100-103}) — витрина остаётся пустой; воспроизводится только
	 * на озере, у которого в пределах руки нет ни дна, ни берега.
	 */
	private static void registerFluidCellReachable(IWailaClientRegistration aRegistration) {
		aRegistration.addRayTraceCallback((aHit, aAccessor, aOriginal) -> {
			try {
				net.minecraft.client.Minecraft tMC = net.minecraft.client.Minecraft.getInstance();
				net.minecraft.world.entity.Entity tCamera = tMC.getCameraEntity();
				if (tCamera == null || tMC.level == null || tMC.player == null) return aAccessor;

				snownee.jade.api.config.IWailaConfig.FluidMode tMode = snownee.jade.api.config.IWailaConfig.get().getGeneral().getDisplayFluids();
				if (tMode == snownee.jade.api.config.IWailaConfig.FluidMode.NONE) return aAccessor;
				// FALLBACK — жидкость только тогда, когда цели нет: так Jade поступает и с настоящими жидкостями
				if (tMode == snownee.jade.api.config.IWailaConfig.FluidMode.FALLBACK && aAccessor != null) return aAccessor;
				boolean tSourceOnly = (tMode == snownee.jade.api.config.IWailaConfig.FluidMode.SOURCE_ONLY);

				net.minecraft.world.phys.Vec3 tFrom = tCamera.getEyePosition(tMC.getFrameTime());
				double tJadeReach = jadeReach(tMC);
				if (tJadeReach <= 0) return aAccessor;
				double tReach = aHit != null && aHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS
					? Math.sqrt(aHit.getLocation().distanceToSqr(tFrom)) : tJadeReach;
				if (tReach <= 0) return aAccessor;
				net.minecraft.world.phys.Vec3 tTo = tFrom.add(tCamera.getViewVector(tMC.getFrameTime()).scale(Math.min(tReach, tJadeReach)));

				net.minecraft.world.phys.BlockHitResult tOurHit = clipOwnFluid(tMC.level, tFrom, tTo, tSourceOnly);
				if (tOurHit == null) return aAccessor;
				// стена ближе клетки — витрину не перехватываем (луч не простреливает препятствия)
				if (aHit != null && aHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS
					&& aHit.getLocation().distanceToSqr(tFrom) <= tOurHit.getLocation().distanceToSqr(tFrom)) return aAccessor;

				BlockState tState = tMC.level.getBlockState(tOurHit.getBlockPos());
				return aRegistration.blockAccessor()
					.level(tMC.level).player(tMC.player)
					.blockState(tState).blockEntity((net.minecraft.world.level.block.entity.BlockEntity)null)
					.hit(tOurHit).requireVerification().build();
			} catch (Throwable e) {/* мира/клетки уже нет — оставляем витрину как есть */}
			return aAccessor;
		});
	}

	/**
	 * ДАЛЬНОСТЬ ЛУЧА ВИТРИНЫ — НЕ НАШЕ ЧИСЛО, А ЕГО СОБСТВЕННОЕ. Считается ровно тем выражением, которым
	 * Jade считает её себе: {@code mc.gameMode.getPickRange() + config.getReachDistance()}
	 * ({@code RayTracing.java:117}, дальше эта величина уходит в {@code rayTrace}). Первое слагаемое —
	 * движковая дальность прицела ({@code MultiPlayerGameMode.getPickRange}: 5.0 в креативе, 4.5 иначе),
	 * второе — надбавка из настроек самого Jade (по умолчанию 0, {@code WailaConfig.java:90,185}).
	 *
	 * <p>До этого здесь стояла собственная константа {@code 8.0} — выдуманное число на месте движковой
	 * величины: свой луч уходил дальше, чем смотрит сам Jade, и правило «не дальше руки» держалось на
	 * совпадении. Источник у дальности теперь один, и он чужой.
	 */
	private static double jadeReach(net.minecraft.client.Minecraft aMC) {
		double tPick = aMC.gameMode == null ? 0 : aMC.gameMode.getPickRange();
		return tPick + snownee.jade.api.config.IWailaConfig.get().getGeneral().getReachDistance();
	}

	/** ПЕРВАЯ по лучу клетка-жидкость GT6, невидимая движковому лучу. Обход — движковый
	 *  ({@code BlockGetter.traverseBlocks}, тот же, которым идёт {@code Level.clip}); отбор — у роли
	 *  ({@code BlockFluidBaseGT}), а не по списку блоков, поэтому новая жидкость попадёт под правило сама. */
	private static net.minecraft.world.phys.BlockHitResult clipOwnFluid(Level aWorld, net.minecraft.world.phys.Vec3 aFrom, net.minecraft.world.phys.Vec3 aTo, boolean aSourceOnly) {
		return net.minecraft.world.level.BlockGetter.traverseBlocks(aFrom, aTo, (Void)null, (aCtx, aPos) -> {
			BlockState tState = aWorld.getBlockState(aPos);
			if (!(tState.getBlock() instanceof gregapi.block.fluid.BlockFluidBaseGT tFluid)) return null;
			if (!tFluid.isInvisibleToFluidClip(tState)) return null;      // движковый луч её и так видит — не наше дело
			if (aSourceOnly && !tFluid.isFullFluidCell(tState)) return null; // канон 1.7.10: луч видел только полную клетку
			return net.minecraft.world.phys.shapes.Shapes.block().clip(aFrom, aTo, aPos);
		}, (aCtx) -> null);
	}

	/**
	 * ИМЯ КЛЕТКИ-ЖИДКОСТИ GT6 (регресс ветки 1.20.1: Jade писал над водой GT6 «Water» вместо типа воды).
	 *
	 * <p><b>Почему это вообще понадобилось на 1.20.1 и не нужно на 26.1.</b> Jade 11.13.3 строит аксессор
	 * не из того блока, что стоит в клетке: {@code RayTracing.wrapBlock} БЕЗУСЛОВНО подменяет состояние на
	 * «легаси-блок его жидкости», если у блока непустой {@code FluidState} и пустой {@code getShape}
	 * ({@code RayTracing.java:57-59}), и делает это ДО плагин-колбэков ({@code WailaTickHandler.java:107}
	 * против {@code :135-137}). Блоки-жидкости GT6 наследуют {@code LiquidBlock}, у которого
	 * {@code getShape} пуст ({@code LiquidBlock.java:104-106}), а мировые воды обязаны объявлять движку
	 * ВАНИЛЬНУЮ воду — иначе мертвы waterlogging и заморозка ({@code BlockWaterlike.getFluidState}).
	 * Оба условия выполнены — и клетка теряла своё имя, получая {@code Blocks.WATER}. В Jade 26.1.8 такой
	 * подмены в трассировке нет вовсе: она живёт только внутри {@code CorePlugin.hideBlocks} и срабатывает
	 * лишь для блоков, помеченных {@code shouldHide}, — GT6 таких пометок не ставит.
	 *
	 * <p><b>Чиним витриной, а не механикой.</b> Отдать воде свою жидкость или непустую форму значило бы
	 * сломать плавание, клип и заморозку ради надписи. Поэтому берём канал, который Jade для этого и
	 * завёл, — {@code addRayTraceCallback} ({@code IWailaClientRegistration.java:136-140}), выполняемый
	 * ПОСЛЕ подмены и умеющий пересобрать аксессор ({@code BlockAccessor.Builder.from/blockState}).
	 *
	 * <p><b>Правило, а не перечень.</b> Спрашиваем сам мир: если клетка под прицелом занята блоком-жидкостью
	 * GT6, а аксессор несёт другой блок — возвращаем настоящее состояние клетки. Поэтому оно покрывает все
	 * блоки-жидкости GT6 разом (океан, река, болото, продвинутая река, геотермальная вода) и любые будущие,
	 * ничего не перечисляя. Нефти и газ подменой не задеты: их {@code FluidState} — собственная жидкость GT6,
	 * а её {@code createLegacyBlock} отдаёт СВОЙ же блок ({@code FluidGT:290-293}) — правило их просто не
	 * касается, условие «аксессор несёт другой блок» у них ложно.
	 *
	 * <p>Приоритет — умолчание (0): встроенные колбэки Jade стоят на −10 и 5000
	 * ({@code VanillaPlugin.java:194-195}), в их порядок не вмешиваемся.
	 */
	private static void registerFluidCellRestore(IWailaClientRegistration aRegistration) {
		aRegistration.addRayTraceCallback((aHit, aAccessor, aOriginal) -> {
			if (!(aAccessor instanceof BlockAccessor tAccessor)) return aAccessor;
			try {
				Level tWorld = tAccessor.getLevel();
				if (tWorld == null) return aAccessor;
				BlockState tReal = tWorld.getBlockState(tAccessor.getPosition());
				if (tReal.getBlock() == tAccessor.getBlock()) return aAccessor;                    // подмены не было — не трогаем
				if (!(tReal.getBlock() instanceof gregapi.block.fluid.BlockFluidBaseGT)) return aAccessor; // клетка не наша — чужая витрина
				return aRegistration.blockAccessor().from(tAccessor).blockState(tReal).build();
			} catch (Throwable e) {/* мира/клетки уже нет — оставляем витрину как есть */}
			return aAccessor;
		});
	}

	/** Один тип GT6-инструмента: «подходит ли он этому блоку» решает сам блок своим {@code getHarvestTool}. */
	private static class GT6ToolHandler implements ToolHandler {
		private final String mToolType;
		private final String mName;
		private final String mVanillaName; // не null только у ванильных типов — чей это обработчик у самого Jade
		private List<ItemStack> mTools; // лениво: реестр инструментов наполняется позже загрузки плагина

		GT6ToolHandler(String aToolType, String aVanillaName) {
			mToolType = aToolType;
			mVanillaName = aVanillaName;
			mName = NAME_PREFIX + aToolType;
		}

		@Override
		public ItemStack test(BlockState aState, Level aWorld, BlockPos aPos) {
			// ⭐ СПРАШИВАЕМ КАЖДЫЙ ИНСТРУМЕНТ, А НЕ БЛОК. Прежняя редакция сравнивала тип из getHarvestTool блока —
			// и врала: у блока строка ОДНА, а в GT6 решает каждый инструмент сам своим isMinableBlock. Ключ берёт
			// поршни, решётки, хопперы и всё, что подходит по его правилу (GT_Tool_Wrench.isMinableBlock:73-81) —
			// поэтому у бочки, объявляющей «axe», ключ тоже законно работает (проверено в живом оригинале 1.7.10).
			// Теперь иконок ровно столько, сколько инструментов реально берут блок, и они не зависят от руки.
			if (!anyToolOfTypeMines(mToolType, aState, aWorld, aPos)) return ItemStack.EMPTY;
			// ЧУЖОЙ БЛОК — ЧУЖАЯ ВИТРИНА. На не-GT6 блоках вмешиваемся, только если Jade сам ничего не показал:
			// совок/лопата GT6 формально берут и ванильную траву, но дописывать свои значки в витрину, которую
			// Jade уже нарисовал, — вторжение в чужую шкалу (поймано позитивным контролем судьи на Grass Block).
			if (!(aState.getBlock() instanceof gregapi.block.IBlock) && vanillaJadeAlreadyShowed(aState, aWorld, aPos)) return ItemStack.EMPTY;
			// СПРАШИВАЕМ САМ JADE, а не гадаем по тегу: если его собственный обработчик этого типа уже ответил,
			// второй такой же значок был бы дублем — молчим. Прежняя проверка «есть ли ванильный тег» была КОПИЕЙ
			// его политики и на ней же ошиблась: у мелкой руды тег есть, но Jade намеренно пропускает блоки,
			// ломающиеся мгновенно (SimpleToolHandler:44), — и тултип оставался пустым, хотя инструмент известен.
			if (mVanillaName != null) {
				ToolHandler tVanilla = HarvestToolProvider.TOOL_HANDLERS.get(mVanillaName);
				if (tVanilla != null && !tVanilla.test(aState, aWorld, aPos).isEmpty()) return ItemStack.EMPTY;
			}
			List<ItemStack> tTools = getTools();
			return tTools.isEmpty() ? ItemStack.EMPTY : tTools.get(0);
		}

		@Override
		public List<ItemStack> getTools() {
			if (mTools == null || mTools.isEmpty()) {
				List<ItemStack> tList = new ArrayList<>();
				try {
					for (gregapi.code.ItemStackContainer tContainer : ToolsGT.list(mToolType)) {
						ItemStack tStack = tContainer.toStack();
						if (tStack != null && !tStack.isEmpty()) tList.add(tStack);
					}
				} catch (Throwable e) {/* реестр ещё не наполнен — попробуем в следующий раз */}
				mTools = tList;
			}
			return mTools;
		}

		@Override
		public String getName() {return mName;}
	}

	/** Нарисовал ли Jade витрину этого блока СВОИМИ силами. Спрашиваем только ЕГО обработчики (перебрать все
	 *  нельзя — среди них мы сами, вышла бы рекурсия). */
	private static boolean vanillaJadeAlreadyShowed(BlockState aState, Level aWorld, BlockPos aPos) {
		for (java.util.Map.Entry<String, ToolHandler> tE : HarvestToolProvider.TOOL_HANDLERS.entrySet()) {
			if (tE.getKey().startsWith(NAME_PREFIX)) continue; // наш — пропускаем
			try {if (!tE.getValue().test(aState, aWorld, aPos).isEmpty()) return true;} catch (Throwable e) {/* чужой упал — не наша беда */}
		}
		return false;
	}

	/**
	 * Берёт ли блок ХОТЬ ОДИН инструмент этого типа — спрашиваем сами инструменты их же методом
	 * {@code IToolStats.isMinableBlock}, тем самым, которым GT6 решает это в игре. Источник инструментов —
	 * реестр {@link ToolsGT#list}, поэтому перечислять вручную нечего: появится новый инструмент — попадёт сюда сам.
	 */
	private static boolean anyToolOfTypeMines(String aToolType, BlockState aState, Level aWorld, BlockPos aPos) {
		try {
			int tMeta = gregapi.util.WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
			for (gregapi.code.ItemStackContainer tContainer : ToolsGT.list(aToolType)) {
				ItemStack tStack = tContainer.toStack();
				if (tStack == null || tStack.isEmpty()) continue;
				// спрашиваем САМ ИНСТРУМЕНТ его же методом, а не лезем в его IToolStats снаружи
				if (tStack.getItem() instanceof gregapi.item.multiitem.MultiItemTool tTool
				 && tTool.isMinableBlock(tStack, aState.getBlock(), (byte)tMeta)) return true;
			}
		} catch (Throwable e) {/* реестр ещё не наполнен либо мира нет */}
		return false;
	}

	/**
	 * BUG-070 — СТРОКА ТРЕБУЕМОГО УРОВНЯ. Ровно одна вещь, которой у Jade нет ни для одного мода: какого тира
	 * инструмент нужен блоку по шкале GT6 0..15, а не по трём ванильным ступеням.
	 *
	 * <p><b>Что здесь СОЗНАТЕЛЬНО не показывается</b> (по разбору с игроком 2026-07-27):
	 * <ul>
	 * <li><b>название инструмента</b> — его несёт ИКОНКА, которую Jade кладёт в строку названия блока
	 *     ({@code HarvestToolProvider:139-141} — {@code align(RIGHT)} + {@code tooltip.append(0, …)}); текстом это был бы дубль;</li>
	 * <li><b>значок соответствия</b> — его ставит сам Jade по тому же вердикту, что решает судьбу дропа
	 *     ({@code CommonProxy:127} → {@code doPlayerHarvestCheck} → наш мост). Свой значок был бы вторым;</li>
	 * <li><b>ярус того, что в руке, числом</b> — «In Hand: 3» игроку ничего не говорит, а ✔/✕ от Jade говорит;</li>
	 * <li><b>раскраска строки</b> — раскрашивать по «будет ли дроп» вводило в заблуждение: у мягких блоков дроп
	 *     есть с чем угодно, включая руку, и зелёный читался как «инструмент подходит».</li>
	 * </ul>
	 *
	 * <p>Тир печатается СЛОВОМ через центр {@link LH#getHarvestLevelMaterial} — ту же таблицу, которой GT6
	 * подписывает уровень в тултипе предмета. Строки нет там, где требования нет: блок берётся рукой либо
	 * уровень не задан ({@code -1}). Блоки спрашиваются ОДИНАКОВО, свои и чужие — заказ игрока звучал
	 * «навожу на ЛЮБОЙ блок»; отбор по происхождению делал витрину половинчатой.
	 */
	public enum GT6HarvestLevelProvider implements IBlockComponentProvider {
		INSTANCE;

		private final ResourceLocation mUID = new ResourceLocation(MD.GT.mID, "harvest_level");

		// MODCOMPAT-014: экран настроек Jade требует перевод у КАЖДОЙ опции плагина и роняет
		// AssertionError, если его нет — ключ строится как "config.jade.plugin_<namespace>.<path>" из UID
		// провайдера и проверяется через I18n.exists (JadeClient, ветка «Missing config translation: %s»).
		// Имя группы (без пути) Jade берёт тем же семейством ключей для заголовка раздела (его собственные —
		// config.jade.plugin_jade* в assets/jade/lang/en_us.json). Оба ключа отдаёт центр локализации мода,
		// как и все остальные имена GT6: LH.add → BACKUPMAP → Language.inject (LanguageHandler:87-97).
		// Место — здесь, рядом с UID: строка ключа выводится из него, и разъехаться им негде.
		static {
			LH.add("config.jade.plugin_" + MD.GT.mID, "GregTech");
			LH.add("config.jade.plugin_" + MD.GT.mID + ".harvest_level", "Harvest Level");
		}

		@Override
		public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
			Block tBlock = aAccessor.getBlockState().getBlock();
			Level tWorld = aAccessor.getLevel();
			BlockPos tPos = aAccessor.getPosition();
			// ── ИКОНКИ ИНСТРУМЕНТОВ ТАМ, ГДЕ JADE ИХ ПРЯЧЕТ ──────────────────────────────────────────────
			// Jade выходит ДО отрисовки, когда скорость разрушения равна нулю (HarvestToolProvider:117-127), а у GT6
			// неподходящий инструмент даёт РОВНО ноль — и это КАНОН 1.7.10, подтверждённый парным замером
			// (1360 пар, нулей 1176 в обеих версиях). Механику трогать нельзя; чиним витрину витриной.
			// Список инструментов спрашиваем У САМОГО JADE (его же getText), а не собираем свой — иначе это была
			// бы копия чужой политики, на которой заход BUG-070 уже обжигался. Рисуем ТОЛЬКО в дыре: он знает
			// инструменты, но показать их не может. При ненулевом прогрессе молчим — там рисует он сам, и наш
			// значок был бы дублем.
			try {
				if (aAccessor.getBlockState().getDestroyProgress(aAccessor.getPlayer(), tWorld, tPos) <= 0) {
					java.util.List<snownee.jade.api.ui.IElement> tTools = HarvestToolProvider.INSTANCE.getText(aAccessor, aConfig);
					if (!tTools.isEmpty()) appendHarvestIcons(aTooltip, tTools, aConfig);
				}
			} catch (Throwable e) {/* витрина Jade недоступна — строка тира ниже всё равно будет */}
			try {
				int tNeeded = WD.harvestLevel(tWorld, tPos.getX(), tPos.getY(), tPos.getZ());
				// Условие показа — «ИЗВЕСТЕН ли инструмент», а не «требуется ли он». Прежде выходили по
				// isToolNotRequired, и строка пропадала у песка, гравия, земли, досок и сундука: их материалы
				// (sand/ground/wood) инструмента не требуют, но класс инструмента у них ЗАДАН — по паспорту
				// 1.7.10 это shovel/axe, тир 0 (engine_block_passport.csv). Игрок 2026-08-06: «на stone и рудах
				// есть, а на гравии, песке, дереве, сундуке — нет». Молчим только там, где сказать нечего:
				// инструмент неизвестен И требования нет.
				String tTool = WD.harvestTool(tBlock, WD.meta(tWorld, tPos.getX(), tPos.getY(), tPos.getZ()));
				if ((tTool == null || tTool.isEmpty()) && WD.getMaterial(tBlock).isToolNotRequired()) return;
				// ⛔ БЕЗ ОТБОРА ПО ПРОИСХОЖДЕНИЮ БЛОКА. Прежде здесь стояло «не IBlock → выходим: чужая шкала
				// ванильная», и заказ игрока («навожу на ЛЮБОЙ блок — вижу тип, тир и соответствие») исполнялся
				// лишь наполовину: у блоков GT6 строка была, у ванильных — нет. Отбор был вынужденным: тира
				// ванильного блока порт тогда не знал вовсе. Теперь знает — центр WD отвечает по паспорту 1.7.10
				// (набор engine_block_passport.csv, 171 запись, снята с живого оригинала), поэтому спрашиваем
				// ОДИНАКОВО у всех. Строку по-прежнему не рисуем там, где требования нет: рука берёт блок
				// (проверка выше) или уровень не задан (-1 — дефолт 1.7.10, Block.java:2490).
				if (tNeeded < 0) return;
				String tName = LH.getHarvestLevelMaterial(tNeeded);
				aTooltip.add(Component.literal(LH.Chat.DGRAY + LH.get(LH.TOOL_HARVEST_TIER, "Requires Tier") + ": " + LH.Chat.WHITE
					+ tNeeded + (tName.isEmpty() ? "" : " (" + tName + ")")));
			} catch (Throwable e) {/* мира/данных ещё нет — строки просто не будет */}
		}

		@Override
		public ResourceLocation getUid() {return mUID;}

		/**
		 * КАК КЛАСТЬ ЧУЖИЕ ЭЛЕМЕНТЫ (наложение значков на строку тира, ветка 1.20.1).
		 *
		 * <p><b>Что было не так.</b> {@code HarvestToolProvider.getText} отдаёт элементы, ОТКАЛИБРОВАННЫЕ
		 * под способ вставки, который он же выберет дальше по своему конфигу
		 * ({@code Identifiers.MC_HARVEST_TOOL_NEW_LINE}, дефолт {@code false} — {@code VanillaPlugin:138}).
		 * В режиме «не новая строка» вся пачка НУЛЕВОЙ высоты: {@code spacer(5, 0)}, иконки
		 * {@code .size(10, 0)}, значок ✔/✕ — {@code Vec2.ZERO} ({@code HarvestToolProvider:49,161,169},
		 * {@code SubTextElement:21-23}). Она рассчитана на приклейку к уже существующей строке. Мы же
		 * клали её как ОТДЕЛЬНУЮ строку — а высота строки в 11.13.3 есть чистый максимум по элементам
		 * ({@code Tooltip:35-50}), и строки укладываются подряд без единого отступа
		 * ({@code TooltipRenderer:81-85} — {@code y += size.y}). Строка нулевой высоты не занимает места,
		 * следующая («Requires Tier») ложится на тот же y — отсюда наложение. Вдобавок без
		 * {@code Align.RIGHT} элементы уходят в левый список ({@code Element:17}) и накрывают именно текст.
		 *
		 * <p><b>Почему на 26.1 того же кода хватало.</b> Там у каждой строки есть собственный нижний отступ
		 * {@code marginBottom = 2} и flex-укладка ({@code Jade-src impl/Tooltip:252-253},
		 * {@code BoxElementImpl:88-96}) — она прощает строку нулевой высоты. Способ вставки был неверен и
		 * там, просто не был виден.
		 *
		 * <p><b>Правило.</b> Чужие готовые элементы кладём ТЕМ ЖЕ способом, каким их откалибровал их
		 * поставщик, а режим спрашиваем У НЕГО ЖЕ, а не угадываем — 1:1 с {@code HarvestToolProvider:131-142}.
		 * Это не копия чужой политики: «какие инструменты показывать» по-прежнему отвечает его {@code getText};
		 * здесь соблюдается лишь КОНТРАКТ ВОЗВРАТА — элементы годны ровно для одного способа вставки.
		 *
		 * <p>Побочно исчезает и развилка внешнего вида, которую видел игрок: значок встаёт на строку имени
		 * и с пустой рукой (там рисует сам Jade), и в дыре (рисуем мы) — одинаково.
		 */
		private static void appendHarvestIcons(ITooltip aTooltip, java.util.List<snownee.jade.api.ui.IElement> aElements, IPluginConfig aConfig) {
			if (aConfig.get(snownee.jade.api.Identifiers.MC_HARVEST_TOOL_NEW_LINE)) {
				aTooltip.add(aElements); // элементы откалиброваны под собственную строку (spacer высотой 10)
				return;
			}
			for (snownee.jade.api.ui.IElement tElement : aElements) tElement.align(snownee.jade.api.ui.IElement.Align.RIGHT);
			aTooltip.append(0, aElements); // приклейка к строке имени блока — как это делает сам Jade
		}
	}
}
