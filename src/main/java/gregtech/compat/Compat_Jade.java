/**
 * Copyright (c) 2023 GregTech-6 Team
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.event.EventHooks;

import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.ToolHandler;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

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
			String tNeeded = harvestToolOf(aState, aWorld, aPos);
			if (tNeeded == null || !tNeeded.equals(mToolType)) return ItemStack.EMPTY;
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

	/**
	 * Какой инструмент нужен блоку — через ЦЕНТР {@code WD.harvestTool}, а не перебором {@code instanceof}:
	 * центр диспетчеризует по контракту {@code IBlock} и потому знает ВСЕ GT6-иерархии (прежний ручной перебор
	 * трёх классов был слеп к рельсам и внутренним блокам — тот же класс дефекта, что чинил BUG-071).
	 * Мета берётся из мира: у GT6 подтип блока живёт в BlockEntity, а не в BlockState.
	 */
	private static String harvestToolOf(BlockState aState, Level aWorld, BlockPos aPos) {
		try {
			return WD.harvestTool(aState.getBlock(), WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));
		} catch (Throwable e) {/* не GT6-блок либо мира ещё нет */}
		return null;
	}

	/**
	 * BUG-070 п.2/п.3 — СТРОКА УРОВНЯ ДОБЫЧИ. Показывает то, чего у Jade нет ни для одного мода: какой уровень
	 * инструмента блок требует (вся шкала GT6 0..15, а не три ванильные ступени) и что на этот счёт даёт
	 * предмет в руке.
	 *
	 * <p><b>Ни одной собственной формулировки.</b> Первая строка — тот же центр {@code LH.getToolTipHarvest},
	 * которым GT6 рисует эту же справку в тултипе предмета ({@code ItemBlockBase:123}): игрок видит один и тот
	 * же текст в обоих местах. Величины — существующие центры: {@code WD.harvestTool} (тип),
	 * {@code WD.harvestLevel(world,x,y,z)} (требуемый уровень В ЭТОЙ ПОЗИЦИИ), {@code WD.toolLevel} (ярус того,
	 * что в руке). Ни новых сущностей, ни второй копии шкалы.
	 *
	 * <p><b>Право судит движок, а не эта строка.</b> Значок ✔/✕ ставится по ответу
	 * {@code EventHooks.doPlayerHarvestCheck} — то есть по тому же событию, которым игра решает судьбу дропа
	 * (наш слушатель — {@code GT_API_Proxy.onPlayerHarvestCheckEvent}). Собственного правила здесь нет: витрина
	 * не имеет права разойтись с механикой.
	 */
	public enum GT6HarvestLevelProvider implements IBlockComponentProvider {
		INSTANCE;

		private static final String LANG_IN_HAND = "gt.lang.tooltip.harvest.inhand";
		private static final String LANG_LEVEL   = "gt.lang.tooltip.harvest.level";
		private final Identifier mUID = Identifier.fromNamespaceAndPath(MD.GT.mID, "harvest_level");

		@Override
		public void appendTooltip(ITooltip aTooltip, BlockAccessor aAccessor, IPluginConfig aConfig) {
			BlockState tState = aAccessor.getBlockState();
			Block tBlock = tState.getBlock();
			if (!(tBlock instanceof gregapi.block.IBlock)) return; // чужие блоки не наше дело: их шкала ванильная
			Level tWorld = aAccessor.getLevel();
			BlockPos tPos = aAccessor.getPosition();
			try {
				int tMeta = WD.meta(tWorld, tPos.getX(), tPos.getY(), tPos.getZ());
				String tTool = WD.harvestTool(tBlock, tMeta);
				int tNeeded = WD.harvestLevel(tWorld, tPos.getX(), tPos.getY(), tPos.getZ());
				aTooltip.add(Component.literal(LH.getToolTipHarvest(WD.getMaterial(tBlock), tTool, tNeeded)));

				// «что в руке» — только там, где инструмент вообще требуется (иначе строка ни о чём: копается рукой)
				if (WD.getMaterial(tBlock).isToolNotRequired()) return;
				Player tPlayer = aAccessor.getPlayer();
				if (tPlayer == null) return;
				int tHave = WD.toolLevel(tPlayer.getMainHandItem(), tTool);
				boolean tCan = EventHooks.doPlayerHarvestCheck(tPlayer, tState, tWorld, tPos);
				// ЧИСЛО ТРЕБУЕМОГО УРОВНЯ — здесь, а не в строке выше: оригинальный формат GT6 печатает его только
				// при уровне > 1 (LH.getToolTipHarvest:281 — ветка switch недостижима для 0/1, так и в 1.7.10).
				// Формат оригинала не трогаем (переносим механизм, не улучшаем), а недостающее число даём своей строкой.
				String tText = LH.get(LANG_LEVEL, "Level") + ": " + tNeeded + " · " + LH.get(LANG_IN_HAND, "In Hand")
					+ ": " + (tHave < 0 ? "-" : String.valueOf(tHave)) + " " + (tCan ? "✔" : "✘");
				IThemeHelper tTheme = IThemeHelper.get();
				aTooltip.add(tCan ? tTheme.success(tText) : tTheme.danger(tText));
			} catch (Throwable e) {/* мира/данных ещё нет — строки просто не будет */}
		}

		@Override
		public Identifier getUid() {return mUID;}
	}
}
