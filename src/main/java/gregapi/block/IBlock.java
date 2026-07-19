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

package gregapi.block;

import net.minecraft.world.level.block.Block;


/**
 * @author Gregorius Techneticies
 */
public interface IBlock {
	public Block getBlock();
	/** F-bounds: 1.7.10 мутировал Block.mBoundingBox (рендер per-pass + коллизия). neo bounds immutable →
	 *  GT6-блок хранит последние заданные bounds сам; рендер-использование отложено на F3-клиент-проход. */
	public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ);
	/** F-bounds (чтение, пара к setBlockBounds): последние заданные render-bounds {minX,minY,minZ,maxX,maxY,maxZ}.
	 *  Единый контракт для ВСЕХ Block-иерархий GT6 (BlockBase/флюиды/MTE/rail/prefix — общего предка нет);
	 *  читает GT6BlockModel.applyBounds (1.7.10 RenderBlocks.setRenderBoundsFromBlock) — под-боксы render-пассов. */
	public float[] getRenderBounds();
}
