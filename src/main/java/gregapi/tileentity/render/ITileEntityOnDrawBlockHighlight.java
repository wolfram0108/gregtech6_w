/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.tileentity.render;

import gregapi.tileentity.ITileEntityUnloadable;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/**
 * @author Gregorius Techneticies
 *
 * PORT-TODO(F3, baked-рендер клиента): 1.7.10 {@code net.minecraftforge.client.event.DrawBlockHighlightEvent}
 * (immediate-mode, поля {@code player}/{@code target}/{@code currentItem}/{@code partialTicks}) удалён
 * целиком в 26.1.2 — событие пересобрано вокруг {@code BlockOutlineRenderState}
 * (`neoforge-decompiled/net/neoforged/neoforge/client/event/ExtractBlockOutlineRenderStateEvent.java:33-145`,
 * `getBlockPos/getBlockState/getHitResult/getCollisionContext`, БЕЗ прямого держателя игрока/предмета
 * в руке/partialTicks). Сигнатура метода вынужденно ретипирована на новый класс события (F, тип-шов);
 * реализации ниже по цепочке — компилируемая заглушка, реальная перерисовка wrench-overlay это
 * decisions/F3-render.md §2.7/BER-путь.
 */
public interface ITileEntityOnDrawBlockHighlight extends ITileEntityUnloadable {
	/** Gets called Client Side, when you mouse over this TileEntity. return true to prevent other things from rendering. */
	public boolean onDrawBlockHighlight(ExtractBlockOutlineRenderStateEvent aEvent);
}
