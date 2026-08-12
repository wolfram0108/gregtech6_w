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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.tileentity.tools;

import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityFunnelAccessible;
import gregapi.tileentity.base.TileEntityBase11AttachmentSmall;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityFluidFunnel extends TileEntityBase11AttachmentSmall {
	public boolean mAcidProof = F;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_ACIDPROOF)) mAcidProof = aNBT.getBooleanOr(NBT_ACIDPROOF, false);
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(Chat.ORANGE + LH.get(LH.NO_GUI_CLICK_TO_TANK));
		aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_LIQUIDPROOF));
		if (mAcidProof) aList.add(Chat.ORANGE + LH.get(LH.TOOLTIP_ACIDPROOF));
	}
	
	@Override
	public boolean onBlockActivated3(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isServerSide()) {
			ItemStack aStack = ST.n(aPlayer.getMainHandItem()); // F15-граница: движок EMPTY -> GT6 null (тело 1:1 рассуждает null-семантикой)
			if (aStack != null) {
				FluidStack tFluid = FL.getFluid(ST.amount(1, aStack), T);
				if (!FL.gas(tFluid, T) && tFluid.getAmount() > 0 && (mAcidProof || !FL.acid(tFluid))) {
					DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(mFacing);
					if (tDelegator.mTileEntity instanceof ITileEntityFunnelAccessible) {
						int tAmount = ((ITileEntityFunnelAccessible)tDelegator.mTileEntity).funnelFill(tDelegator.mSideOfTileEntity, tFluid, F);
						if (tAmount >= tFluid.getAmount() && ((ITileEntityFunnelAccessible)tDelegator.mTileEntity).funnelFill(tDelegator.mSideOfTileEntity, tFluid, T) > 0) {
							UT.Sounds.send(SFX.MC_LIQUID_WATER, this, F);
							aStack.setCount(aStack.getCount()-1);
							ST.give(aPlayer, ST.container(ST.amount(1, aStack), T), T);
							return T;
						}
						// F5/BUG-045 (1:1): восстановленный IFluidContainerItem.drain(ItemStack,...) (compat-mirror; оригинал :83-87);
						// прежний FluidUtil.getFluidHandler-путь был мёртв (capability для GT6-предметов не регистрируется).
						if (aStack.getItem() instanceof IFluidContainerItem && aStack.getCount() == 1) {
							UT.Sounds.send(SFX.MC_LIQUID_WATER, this, F);
							((IFluidContainerItem)aStack.getItem()).drain(aStack, ((ITileEntityFunnelAccessible)tDelegator.mTileEntity).funnelFill(tDelegator.mSideOfTileEntity, tFluid, T), T);
							return T;
						}
					}
				}
			}
		}
		return T;
	}
	
	@Override public int getRenderPasses2(Block aBlock, boolean[] aShouldSideBeRendered) {return 3;}
	@Override public boolean usesRenderPass2(int aRenderPass, boolean[] aShouldSideBeRendered) {return T;}
	
	@Override
	public boolean setBlockBounds2(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		switch(aRenderPass) {
		case 0:
			switch(mFacing) {
			case SIDE_Z_NEG: box(aBlock, PX_P[ 5], PX_P[ 9], PX_P[ 0], PX_N[ 5], PX_N[ 6], PX_N[10]); return T;
			case SIDE_Z_POS: box(aBlock, PX_P[ 5], PX_P[ 9], PX_P[10], PX_N[ 5], PX_N[ 6], PX_N[ 0]); return T;
			case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 9], PX_P[ 5], PX_N[10], PX_N[ 6], PX_N[ 5]); return T;
			case SIDE_X_POS: box(aBlock, PX_P[10], PX_P[ 9], PX_P[ 5], PX_N[ 0], PX_N[ 6], PX_N[ 5]); return T;
			default: box(aBlock, PX_P[ 5], PX_P[ 2], PX_P[ 5], PX_N[ 5], PX_N[13], PX_N[ 5]); return T;
			}
		case 1:
			switch(mFacing) {
			case SIDE_Z_NEG: box(aBlock, PX_P[ 6], PX_P[ 8], PX_P[ 0], PX_N[ 6], PX_N[ 7], PX_N[12]); return T;
			case SIDE_Z_POS: box(aBlock, PX_P[ 6], PX_P[ 8], PX_P[12], PX_N[ 6], PX_N[ 7], PX_N[ 0]); return T;
			case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 8], PX_P[ 6], PX_N[12], PX_N[ 7], PX_N[ 6]); return T;
			case SIDE_X_POS: box(aBlock, PX_P[12], PX_P[ 8], PX_P[ 6], PX_N[ 0], PX_N[ 7], PX_N[ 6]); return T;
			default: box(aBlock, PX_P[ 6], PX_P[ 1], PX_P[ 6], PX_N[ 6], PX_N[14], PX_N[ 6]); return T;
			}
		case 2:
			switch(mFacing) {
			case SIDE_Z_NEG: box(aBlock, PX_P[ 7], PX_P[ 7], PX_P[ 0], PX_N[ 7], PX_N[ 8], PX_N[14]); return T;
			case SIDE_Z_POS: box(aBlock, PX_P[ 7], PX_P[ 7], PX_P[14], PX_N[ 7], PX_N[ 8], PX_N[ 0]); return T;
			case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 7], PX_P[ 7], PX_N[14], PX_N[ 8], PX_N[ 7]); return T;
			case SIDE_X_POS: box(aBlock, PX_P[14], PX_P[ 7], PX_P[ 7], PX_N[ 0], PX_N[ 8], PX_N[ 7]); return T;
			default: box(aBlock, PX_P[ 7], PX_P[ 0], PX_P[ 7], PX_N[ 7], PX_N[15], PX_N[ 7]); return T;
			}
		}
		return T;
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		return BlockTextureMulti.get(BlockTextureDefault.get(sColoreds[FACES_TBS[aSide]], mRGBa), BlockTextureDefault.get(sOverlays[FACES_TBS[aSide]]));
	}
	
	// Icons
	public static IIconContainer sColoreds[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/tools/funnel/colored/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/tools/funnel/colored/top"),
		new Textures.BlockIcons.CustomIcon("machines/tools/funnel/colored/side"),
	}, sOverlays[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/tools/funnel/overlay/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/tools/funnel/overlay/top"),
		new Textures.BlockIcons.CustomIcon("machines/tools/funnel/overlay/side"),
	};
	
	@Override
	public AABB getSelectedBoundingBoxFromPool() {
		switch(mFacing) {
		case SIDE_Z_NEG: return box(PX_P[ 5], PX_P[ 7], PX_P[ 0], PX_N[ 5], PX_N[ 6], PX_N[10]);
		case SIDE_Z_POS: return box(PX_P[ 5], PX_P[ 7], PX_P[10], PX_N[ 5], PX_N[ 6], PX_N[ 0]);
		case SIDE_X_NEG: return box(PX_P[ 0], PX_P[ 7], PX_P[ 5], PX_N[10], PX_N[ 6], PX_N[ 5]);
		case SIDE_X_POS: return box(PX_P[10], PX_P[ 7], PX_P[ 5], PX_N[ 0], PX_N[ 6], PX_N[ 5]);
		default        : return box(PX_P[ 5], PX_P[ 0], PX_P[ 5], PX_N[ 5], PX_N[13], PX_N[ 5]);
		}
	}
	
	@Override
	public void setBlockBoundsBasedOnState(Block aBlock) {
		switch(mFacing) {
		case SIDE_Z_NEG: box(aBlock, PX_P[ 5], PX_P[ 7], PX_P[ 0], PX_N[ 5], PX_N[ 6], PX_N[10]); break;
		case SIDE_Z_POS: box(aBlock, PX_P[ 5], PX_P[ 7], PX_P[10], PX_N[ 5], PX_N[ 6], PX_N[ 0]); break;
		case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 7], PX_P[ 5], PX_N[10], PX_N[ 6], PX_N[ 5]); break;
		case SIDE_X_POS: box(aBlock, PX_P[10], PX_P[ 7], PX_P[ 5], PX_N[ 0], PX_N[ 6], PX_N[ 5]); break;
		default        : box(aBlock, PX_P[ 5], PX_P[ 0], PX_P[ 5], PX_N[ 5], PX_N[13], PX_N[ 5]); break;
		}
	}
	
	@Override public byte getDefaultSide() {return SIDE_BOTTOM;}
	@Override public boolean[] getValidSides() {return SIDES_BOTTOM_HORIZONTAL;}
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.funnel";}
}
