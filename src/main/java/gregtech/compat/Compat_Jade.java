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
import net.minecraft.resources.Identifier;
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

	@Override
	public void registerClient(IWailaClientRegistration aRegistration) {
		// GT6-типы: ванильного обработчика для них не существует в принципе (нет ни тега, ни предмета)
		for (String tToolType : GT6_TOOL_TYPES   ) HarvestToolProvider.registerHandler(() -> new GT6ToolHandler(tToolType, null));
		// Ванильные типы: подстраховываем ТОЛЬКО там, где обработчик самого Jade промолчал — иначе значок задвоится
		for (String tToolType : VANILLA_TOOL_TYPES) HarvestToolProvider.registerHandler(() -> new GT6ToolHandler(tToolType, snownee.jade.api.JadeIds.JADE(tToolType)));
		// BUG-070 п.2/п.3 — строка «какой уровень нужен» и «что в руке»: у Jade такой строки нет ни для кого
		aRegistration.registerBlockComponent(GT6HarvestLevelProvider.INSTANCE, Block.class);
	}

	/** Один тип GT6-инструмента: «подходит ли он этому блоку» решает сам блок своим {@code getHarvestTool}. */
	private static class GT6ToolHandler implements ToolHandler {
		private final String mToolType;
		private final Identifier mUID;
		private final Identifier mVanillaUID; // не null только у ванильных типов — чей это обработчик у самого Jade
		private List<ItemStack> mTools; // лениво: реестр инструментов наполняется позже загрузки плагина

		GT6ToolHandler(String aToolType, Identifier aVanillaUID) {
			mToolType = aToolType;
			mVanillaUID = aVanillaUID;
			mUID = Identifier.fromNamespaceAndPath(MD.GT.mID, "tool/" + aToolType);
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
			if (mVanillaUID != null) {
				ToolHandler tVanilla = HarvestToolProvider.TOOL_HANDLERS.get(mVanillaUID);
				if (tVanilla != null && !tVanilla.test(aState, aWorld, aPos).isEmpty()) return ItemStack.EMPTY;
			}
			List<ItemStack> tTools = getTools();
			return tTools.isEmpty() ? ItemStack.EMPTY : tTools.getFirst();
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
		public Identifier getUid() {return mUID;}
	}

	/** Нарисовал ли Jade витрину этого блока СВОИМИ силами. Спрашиваем только ЕГО обработчики (перебрать все
	 *  нельзя — среди них мы сами, вышла бы рекурсия). */
	private static boolean vanillaJadeAlreadyShowed(BlockState aState, Level aWorld, BlockPos aPos) {
		for (java.util.Map.Entry<Identifier, ToolHandler> tE : HarvestToolProvider.TOOL_HANDLERS.entrySet()) {
			if (tE.getKey().getNamespace().equals(MD.GT.mID)) continue; // наш — пропускаем
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
	 *     ({@code HarvestToolProvider:114} — {@code tooltip.append(0, …)}); текстом это был бы дубль;</li>
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

		private final Identifier mUID = Identifier.fromNamespaceAndPath(MD.GT.mID, "harvest_level");

		@Override
		public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
			Block tBlock = aAccessor.getBlockState().getBlock();
			Level tWorld = aAccessor.getLevel();
			BlockPos tPos = aAccessor.getPosition();
			// ── ИКОНКИ ИНСТРУМЕНТОВ ТАМ, ГДЕ JADE ИХ ПРЯЧЕТ ──────────────────────────────────────────────
			// Jade выходит ДО отрисовки, когда скорость разрушения равна нулю (HarvestToolProvider:93), а у GT6
			// неподходящий инструмент даёт РОВНО ноль — и это КАНОН 1.7.10, подтверждённый парным замером
			// (1360 пар, нулей 1176 в обеих версиях). Механику трогать нельзя; чиним витрину витриной.
			// Список инструментов спрашиваем У САМОГО JADE (его же getText), а не собираем свой — иначе это была
			// бы копия чужой политики, на которой заход BUG-070 уже обжигался. Рисуем ТОЛЬКО в дыре: он знает
			// инструменты, но показать их не может. При ненулевом прогрессе молчим — там рисует он сам, и наш
			// значок был бы дублем.
			try {
				if (aAccessor.getBlockState().getDestroyProgress(aAccessor.getPlayer(), tWorld, tPos) <= 0) {
					java.util.List<snownee.jade.api.ui.Element> tTools = HarvestToolProvider.INSTANCE.getText(aAccessor, aConfig);
					if (!tTools.isEmpty()) aTooltip.add(tTools);
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
		public Identifier getUid() {return mUID;}
	}
}
