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

import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.ToolHandler;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import gregapi.data.CS;
import gregapi.data.CS.ToolsGT;
import gregapi.data.MD;

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

	@Override
	public void registerClient(IWailaClientRegistration aRegistration) {
		for (String tToolType : GT6_TOOL_TYPES) HarvestToolProvider.registerHandler(() -> new GT6ToolHandler(tToolType));
	}

	/** Один тип GT6-инструмента: «подходит ли он этому блоку» решает сам блок своим {@code getHarvestTool}. */
	private static class GT6ToolHandler implements ToolHandler {
		private final String mToolType;
		private final Identifier mUID;
		private List<ItemStack> mTools; // лениво: реестр инструментов наполняется позже загрузки плагина

		GT6ToolHandler(String aToolType) {
			mToolType = aToolType;
			mUID = Identifier.fromNamespaceAndPath(MD.GT.mID, "tool/" + aToolType);
		}

		@Override
		public ItemStack test(BlockState aState, Level aWorld, BlockPos aPos) {
			String tNeeded = harvestToolOf(aState, aWorld, aPos);
			if (tNeeded == null || !tNeeded.equals(mToolType)) return ItemStack.EMPTY;
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
	 * Какой инструмент нужен блоку — спрашиваем ЕГО ЖЕ 1.7.10-метод. Мета берётся из мира (у GT6 подтип блока
	 * живёт в BlockEntity/мете, а не в BlockState), поэтому передаём реальные координаты — иначе для
	 * prefix-блоков и MTE ответ был бы про мету 0.
	 */
	private static String harvestToolOf(BlockState aState, Level aWorld, BlockPos aPos) {
		try {
			net.minecraft.world.level.block.Block tBlock = aState.getBlock();
			int tMeta = gregapi.util.WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
			if (tBlock instanceof gregapi.block.prefixblock.PrefixBlock tP) return tP.getHarvestTool(tMeta);
			if (tBlock instanceof gregapi.block.BlockBase tB) return tB.getHarvestTool(tMeta);
			if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlock tM) return tM.getHarvestTool(tMeta);
		} catch (Throwable e) {/* не GT6-блок либо мира ещё нет */}
		return null;
	}
}
