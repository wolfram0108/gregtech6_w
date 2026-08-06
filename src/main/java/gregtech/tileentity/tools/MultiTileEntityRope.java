/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregtech.tileentity.tools;

import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ItemNBT;
import gregapi.data.CS.*;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityQuickObstructionCheck;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityRope extends TileEntityBase09FacingSingle implements ITileEntityQuickObstructionCheck, IMTE_IgnorePlayerCollisionWhenPlacing, IMTE_IsLadder, IMTE_OnBlockHarvested, IMTE_SetBlockBoundsBasedOnState, IMTE_GetCollisionBoundingBoxFromPool, IMTE_GetSelectedBoundingBoxFromPool {
	@Override
	public boolean onBlockActivated3(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		ItemStack aStack = aPlayer.getMainHandItem();
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(getMultiTileEntityRegistryID());
		if (tRegistry != null && ST.equal(aStack, toStack(), F)) {
			// BUG-089 (класс F6-Y-scale): было tY >= 0 — дно мира 1.7.10; в MC26 дно getMinY() (-64), протяжка
			// верёвки ниже нуля молча не работала. Граница — через центр WD.minY, как у жидкостей/данжей.
			if (isServerSide()) for (int tY = getBlockPos().getY()-1; tY >= WD.minY(level); tY--) {
				BlockEntity tTileEntity = getTileEntity(getBlockPos().getX(), tY, getBlockPos().getZ());
				if (tTileEntity instanceof MultiTileEntityRope) {
					if (((MultiTileEntityRope)tTileEntity).getMultiTileEntityRegistryID() != getMultiTileEntityRegistryID()) return T;
					if (((MultiTileEntityRope)tTileEntity).getMultiTileEntityID() != getMultiTileEntityID()) return T;
					if (((MultiTileEntityRope)tTileEntity).mFacing != mFacing) return T;
					continue;
				}
				if (WD.air(level, getBlockPos().getX(), tY, getBlockPos().getZ())) {
					tRegistry.mBlock.placeBlock(level, getBlockPos().getX(), tY, getBlockPos().getZ(), SIDE_ANY, getMultiTileEntityID(), UT.NBT.make(ItemNBT.has(aStack)?(CompoundTag)ItemNBT.get(aStack).copy():null, NBT_FACING, mFacing), T, F);
					if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
					UT.Sounds.send(SFX.MC_DIG_CLOTH, this, F);
				}
				return T;
			}
			return T;
		}
		return F;
	}
	
	@Override
	public void onBlockHarvested(int aMetaData, Player aPlayer) {
		if (isServerSide() && aPlayer != null) {
			BlockEntity tTileEntity = getTileEntityAtSideAndDistance(SIDE_UP, 1);
			if (!(tTileEntity instanceof MultiTileEntityRope)) for (int tY = getBlockPos().getY()-1; tY >= WD.minY(level); tY--) { // BUG-089: было tY >= 0, дно MC26 = getMinY()
				tTileEntity = getTileEntity(getBlockPos().getX(), tY, getBlockPos().getZ());
				if (tTileEntity instanceof MultiTileEntityRope && ((MultiTileEntityRope)tTileEntity).mFacing == mFacing && ((MultiTileEntityRope)tTileEntity).getMultiTileEntityRegistryID() == getMultiTileEntityRegistryID() && ((MultiTileEntityRope)tTileEntity).getMultiTileEntityID() == getMultiTileEntityID()) {
					((MultiTileEntityRope)tTileEntity).popOff(aPlayer);
					continue;
				}
				break;
			}
		}
	}
	
	@Override
	public void onTick2(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide && (mBlockUpdated || aTimer == 1) && !WD.opq(level, getOffsetX(mFacing), getOffsetY(mFacing), getOffsetZ(mFacing), F, T) && !WD.opq(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), F, T) && !(getTileEntityAtSideAndDistance(SIDE_UP, 1) instanceof MultiTileEntityRope)) popOff();
	}
	
	@Override public int getRenderPasses2(Block aBlock, boolean[] aShouldSideBeRendered) {return mFacing == SIDE_Y_NEG ? 2 : 1;}
	
	@Override
	public boolean setBlockBounds2(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		switch(aRenderPass) {
		case 0:
			switch(mFacing) {
			case SIDE_Z_POS: return box(aBlock, PX_P[ 6], PX_P[ 0], PX_P[12], PX_N[ 6], PX_N[ 0], PX_N[ 0]);
			case SIDE_Z_NEG: return box(aBlock, PX_P[ 6], PX_P[ 0], PX_P[ 0], PX_N[ 6], PX_N[ 0], PX_N[12]);
			case SIDE_X_POS: return box(aBlock, PX_P[12], PX_P[ 0], PX_P[ 6], PX_N[ 0], PX_N[ 0], PX_N[ 6]);
			case SIDE_X_NEG: return box(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 6], PX_N[12], PX_N[ 0], PX_N[ 6]);
			case SIDE_Y_POS: return box(aBlock, PX_P[ 6], PX_P[ 0], PX_P[ 6], PX_N[ 6], PX_N[ 0], PX_N[ 6]);
			case SIDE_Y_NEG: return box(aBlock, PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[12], PX_N[ 2]);
			default        : return box(aBlock, PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[12], PX_N[ 2]);
			}
		case 1:
			return box(aBlock, PX_P[ 4], PX_P[ 4], PX_P[ 4], PX_N[ 4], PX_N[ 8], PX_N[ 4]);
		default:
			return F;
		}
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		return BlockTextureMulti.get(BlockTextureDefault.get(sColored, mRGBa), BlockTextureDefault.get(sOverlay));
	}
	
	// Icons
	public static IIconContainer sColored = new Textures.BlockIcons.CustomIcon("machines/tools/rope/colored"), sOverlay = new Textures.BlockIcons.CustomIcon("machines/tools/rope/overlay");
	
	@Override
	public AABB getCollisionBoundingBoxFromPool() {
		switch(mFacing) {
		case SIDE_Z_POS: return box(PX_P[ 7], PX_P[ 0], PX_P[12], PX_N[ 7], PX_N[ 0], PX_N[ 2]);
		case SIDE_Z_NEG: return box(PX_P[ 7], PX_P[ 0], PX_P[ 2], PX_N[ 7], PX_N[ 0], PX_N[12]);
		case SIDE_X_POS: return box(PX_P[12], PX_P[ 0], PX_P[ 7], PX_N[ 2], PX_N[ 0], PX_N[ 7]);
		case SIDE_X_NEG: return box(PX_P[ 2], PX_P[ 0], PX_P[ 7], PX_N[12], PX_N[ 0], PX_N[ 7]);
		case SIDE_Y_POS: return box(PX_P[ 7], PX_P[ 0], PX_P[ 7], PX_N[ 7], PX_N[ 0], PX_N[ 7]);
		case SIDE_Y_NEG: return box(PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[ 8], PX_N[ 2]);
		default        : return box(PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[ 8], PX_N[ 2]);
		}
	}
	
	@Override
	public AABB getSelectedBoundingBoxFromPool() {
		switch(mFacing) {
		case SIDE_Z_POS: return box(PX_P[ 6], PX_P[ 0], PX_P[12], PX_N[ 6], PX_N[ 0], PX_N[ 0]);
		case SIDE_Z_NEG: return box(PX_P[ 6], PX_P[ 0], PX_P[ 0], PX_N[ 6], PX_N[ 0], PX_N[12]);
		case SIDE_X_POS: return box(PX_P[12], PX_P[ 0], PX_P[ 6], PX_N[ 0], PX_N[ 0], PX_N[ 6]);
		case SIDE_X_NEG: return box(PX_P[ 0], PX_P[ 0], PX_P[ 6], PX_N[12], PX_N[ 0], PX_N[ 6]);
		case SIDE_Y_POS: return box(PX_P[ 6], PX_P[ 0], PX_P[ 6], PX_N[ 6], PX_N[ 0], PX_N[ 6]);
		case SIDE_Y_NEG: return box(PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[ 8], PX_N[ 2]);
		default        : return box(PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[ 8], PX_N[ 2]);
		}
	}
	
	@Override
	public void setBlockBoundsBasedOnState(Block aBlock) {
		switch(mFacing) {
		case SIDE_Z_POS: box(aBlock, PX_P[ 6], PX_P[ 0], PX_P[12], PX_N[ 6], PX_N[ 0], PX_N[ 0]); return;
		case SIDE_Z_NEG: box(aBlock, PX_P[ 6], PX_P[ 0], PX_P[ 0], PX_N[ 6], PX_N[ 0], PX_N[12]); return;
		case SIDE_X_POS: box(aBlock, PX_P[12], PX_P[ 0], PX_P[ 6], PX_N[ 0], PX_N[ 0], PX_N[ 6]); return;
		case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 0], PX_P[ 6], PX_N[12], PX_N[ 0], PX_N[ 6]); return;
		case SIDE_Y_POS: box(aBlock, PX_P[ 6], PX_P[ 0], PX_P[ 6], PX_N[ 6], PX_N[ 0], PX_N[ 6]); return;
		case SIDE_Y_NEG: box(aBlock, PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[ 8], PX_N[ 2]); return;
		default        : box(aBlock, PX_P[ 2], PX_P[ 0], PX_P[ 2], PX_N[ 2], PX_N[ 8], PX_N[ 2]); return;
		}
	}
	
	@Override public float getSurfaceSize          (byte aSide) {return 0;}
	@Override public float getSurfaceSizeAttachable(byte aSide) {return 0;}
	@Override public float getSurfaceDistance      (byte aSide) {return 0;}
	@Override public boolean isSurfaceSolid        (byte aSide) {return F;}
	@Override public boolean isSurfaceOpaque2      (byte aSide) {return F;}
	@Override public boolean isSideSolid2          (byte aSide) {return F;}
	@Override public boolean allowCovers           (byte aSide) {return F;}
	@Override public boolean attachCoversFirst     (byte aSide) {return F;}
	@Override public boolean isObstructingBlockAt  (byte aSide) {return F;}
	@Override public boolean isLadder(LivingEntity aEntity) {return T;}
	@Override public boolean ignorePlayerCollisionWhenPlacing() {return T;}
	@Override public boolean useSidePlacementRotation        () {return T;}
	@Override public boolean useInversePlacementRotation     () {return T;}
	@Override public boolean checkObstruction(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	@Override public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	@Override public byte getDefaultSide() {return SIDE_Y_NEG;}
	@Override public boolean[] getValidSides() {return SIDES_VALID;}
	@Override public boolean isUsingWrenchingOverlay(ItemStack aStack, byte aSide) {return F;}
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	@Override public String getFacingTool() {return null;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.rope";}
}
