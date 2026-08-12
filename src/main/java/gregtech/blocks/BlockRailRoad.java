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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.blocks;

import net.minecraft.core.BlockPos;

import gregapi.block.ItemBlockBase;
import gregapi.block.ToolCompat;
import gregapi.block.misc.BlockBaseRail;
import gregapi.render.IIconContainer;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class BlockRailRoad extends BlockBaseRail {
	/** @param aSpeed is usually 0.4F */
	public BlockRailRoad(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, String aLocalName, float aSpeed, float aExplosionResistance, int aHarvestLevel, IIconContainer aIconPrimary, IIconContainer aIconSecondary) {
		super(aItemClass, aNameInternal, aLocalName, T, F, aSpeed, aExplosionResistance, aHarvestLevel, aIconPrimary, aIconSecondary);
	}
	
	@Override
	protected boolean func_150057_a(Level aWorld, int aX, int aY, int aZ, boolean p_150057_5_, int p_150057_6_, int p_150057_7_) {
		if (WD.block(aWorld, aX, aY, aZ) == this) {
			int tRailMeta = WD.meta(7, aWorld, aX, aY, aZ);
			
			if (p_150057_7_ == 1 && (tRailMeta == 0 || tRailMeta == 4 || tRailMeta == 5)) return F;
			if (p_150057_7_ == 0 && (tRailMeta == 1 || tRailMeta == 2 || tRailMeta == 3)) return F;
		}
		return F;
	}
	
	
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		if (!aWorld.isClientSide()) {
			if (!WD.floor(aWorld, aX, aY-1, aZ)) {
				WD.dropBlockAsItem(aWorld, aX, aY, aZ, 0, 0);
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
			}
		}
	}
	
	@Override
	protected void func_150048_a(Level aWorld, int aX, int aY, int aZ, int aMeta, int aData, Block aBlock) {
		// NO-OP
	}

	// BUG-047: 1:1 с 1.7.10 — onBlockAdded/func_150052_a у рода NO-OP (дорожная разметка НЕ выравнивается по соседям
	// и не имеет детектора) — гасим vanilla-выравнивание моста BlockBaseRail.onPlace. Бит 8 = вариант разметки, форма
	// ставится только placement'ом (мета 0/1/8/9) и резчиками (toggle ^8) — мост меты наследуется.
	@Override protected void onPlace(net.minecraft.world.level.block.state.BlockState aState, Level aWorld, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aOldState, boolean aMovedByPiston) {/* NO-OP */}
	
	
	protected void func_150052_a(Level aWorld, int aX, int aY, int aZ, boolean p_150052_5_) {
		// NO-OP
	}
	
	
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		// NO-OP
	}
	
	
	public void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta) {
		// NO-OP
	}
	
	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {
		if (!aWorld.isClientSide()) if (aTool.equals(TOOL_crowbar) || aTool.equals(TOOL_chisel) || aTool.equals(TOOL_shears) || aTool.equals(TOOL_scissors) || aTool.equals(TOOL_knife)) {
			return WD.set(aWorld, aX, aY, aZ, this, WD.meta(aWorld, aX, aY, aZ) ^ 8, 0)?1000:0;
		}
		return ToolCompat.onToolClick(this, aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aWorld, aSide, aX, aY, aZ, aHitX, aHitY, aHitZ);
	}
	
	@Override
	public void onMinecartPass(Level aWorld, AbstractMinecart aCart, int aX, int aY, int aZ) {
		double tMotion = Math.sqrt(WD.motionX(aCart)*WD.motionX(aCart) + WD.motionZ(aCart)*WD.motionZ(aCart));
		if (tMotion > 0.01) {
			WD.setMotionX(aCart, WD.motionX(aCart)*2); WD.setMotionZ(aCart, WD.motionZ(aCart)*2);
		} else {
			byte tRailMeta = WD.meta(7, aWorld, aX, aY, aZ);
			if (tRailMeta == 1) {
					 if (WD.normalCube(WD.block(aWorld, aX-1, aY, aZ), aWorld, aX-1, aY, aZ)) WD.setMotionX(aCart, +0.02);
				else if (WD.normalCube(WD.block(aWorld, aX+1, aY, aZ), aWorld, aX+1, aY, aZ)) WD.setMotionX(aCart, -0.02);
			} else if (tRailMeta == 0) {
					 if (WD.normalCube(WD.block(aWorld, aX, aY, aZ-1), aWorld, aX, aY, aZ-1)) WD.setMotionZ(aCart, +0.02);
				else if (WD.normalCube(WD.block(aWorld, aX, aY, aZ+1), aWorld, aX, aY, aZ+1)) WD.setMotionZ(aCart, -0.02);
			}
		}
	}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}
		
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == WD.maxY(aWorld) && getMaterial().isSolid()) /* BUG-089: было aY == 255 — верх мира через центр F6-Y-scale */ || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;
		
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, SIDES_AXIS_X[UT.Code.getHorizontalForPlayerPlacing(aPlayer)] ? aHitZ > 0.5 ? 9 : 1 : aHitX > 0.5 ? 8 : 0)) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
	
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.RAIL, aWorld, aX, aY, aZ) / 2;}
}
