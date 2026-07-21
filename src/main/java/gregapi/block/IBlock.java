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
	/** F-tool (читают ЦЕНТРЫ WD.harvestTool/WD.harvestLevel): 1.7.10 Forge держал getHarvestTool(int)/getHarvestLevel(int)
	 *  на САМОМ Block — каждая GT6-иерархия отвечала своими полями (BlockBase.mTool, MTE-Block.mTool, PrefixBlock.mTool,
	 *  Rail=crowbar). neo эту точку удалил; контракт восстанавливает её на едином IBlock (общего Block-предка у иерархий
	 *  нет — та же причина, что getRenderBounds выше). Носители перекрывают дефолт автоматически одноимёнными
	 *  существующими методами; не-носители (жидкости, Internal — как в 1.7.10 без override) — дефолт «без инструмента». */
	default String getHarvestTool(int aMeta) {return "";}
	default int getHarvestLevel(int aMeta) {return 0;}
	/** F-hardness (читает ЦЕНТР WD.hardness): 1.7.10 Block.getBlockHardness(World,x,y,z) — Forge-точка per-position
	 *  твёрдости; GT6-иерархии несут свои значения (BlockBase-подклассы per-meta, PrefixBlock per-material,
	 *  MTE-блоки per-TE mHardness из NBT_HARDNESS) — носители перекрывают дефолт автоматически одноимёнными
	 *  методами. Дефолт = vanilla: destroyTime реального state на позиции. Потребитель — износ инструмента
	 *  (MultiItemTool.onBlockDestroyed: урон × твёрдость, 1:1 оригинал). */
	default float getBlockHardness(net.minecraft.world.level.Level aWorld, int aX, int aY, int aZ) {
		net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos(aX, aY, aZ);
		return aWorld.getBlockState(tPos).getDestroySpeed(aWorld, tPos);
	}
}
