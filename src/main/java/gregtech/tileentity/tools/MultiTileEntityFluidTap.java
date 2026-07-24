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

import gregapi.data.FL;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.data.LH.Chat;
import gregapi.data.MD;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityTapAccessible;
import gregapi.tileentity.ITileEntityTapFillable;
import gregapi.tileentity.base.TileEntityBase11AttachmentSmall;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.util.ST;
import gregapi.util.UT;
import gregtech.tileentity.food.MultiTileEntitySandwich;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import openblocks.common.LiquidXpUtils;
import openmods.utils.EnchantmentUtils;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityFluidTap extends TileEntityBase11AttachmentSmall {
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
			DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(mFacing);
			if (tDelegator.mTileEntity instanceof ITileEntityTapAccessible) {
				ItemStack aStack = ST.n(aPlayer.getMainHandItem()); // F15-граница: движок EMPTY -> GT6 null (тело 1:1 рассуждает null-семантикой)
				if (ItemsGT.VOIDING_ITEMS.contains(aStack, F)) {
					UT.Sounds.send(SFX.IC_SPRAY, 1.0F, 2.0F, this, F);
					GarbageGT.trash(((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, Integer.MAX_VALUE, T));
					return T;
				}
				FluidStack aFluid = ((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, Integer.MAX_VALUE, F);
				if (!FL.gas(aFluid, T) && aFluid.getAmount() > 0 && (mAcidProof || !FL.acid(aFluid))) {
					if (aStack == null) {
						DelegatorTileEntity<BlockEntity> tDelegator2 = getAdjacentTileEntity(SIDE_BOTTOM);
						if (tDelegator2.mTileEntity == null) {
							if (tDelegator2.getBlock() instanceof CauldronBlock) {
								byte tMeta = tDelegator2.getMetaData();
								if (tMeta < 3 && FL.water(aFluid) && aFluid.getAmount() >= 334) {
									if (aFluid.getAmount() >= 1000 && tMeta <= 0) {
										((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, 1000, T);
										tDelegator2.setMetaData((byte)(tMeta + 3));
									} else if (aFluid.getAmount() >= 667 && tMeta <= 1) {
										((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity,  667, T);
										tDelegator2.setMetaData((byte)(tMeta + 2));
									} else if (tMeta <= 2) {
										((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity,  334, T);
										tDelegator2.setMetaData((byte)(tMeta + 1));
									}
									UT.Sounds.send(SFX.IC_SPRAY, 1.0F, 2.0F, this, F);
									UT.Sounds.send(SFX.MC_LIQUID_WATER, this, F);
								}
								return T;
							}
						} else if (tDelegator2.mTileEntity instanceof ITileEntityTapFillable) {
							OreDictMaterialStack tMaterial = OreDictMaterial.FLUID_MAP.get(FL.regName(aFluid.getFluid()));
							aFluid = aFluid.copy();
							aFluid.setAmount(Math.min(aFluid.getAmount(), FL.lava(aFluid) ? 1000 : !FL.water(aFluid) && tMaterial != null && tMaterial.mAmount > 0 ? UT.Code.bindInt(tMaterial.mAmount) : 250));
							if (FL.nonzero(((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, UT.Code.bindInt(((ITileEntityTapFillable)tDelegator2.mTileEntity).tapFill(tDelegator2.mSideOfTileEntity, aFluid, T)), T))) {
								UT.Sounds.send(SFX.IC_SPRAY, 1.0F, 2.0F, this, F);
								UT.Sounds.send(SFX.MC_LIQUID_WATER, this, F);
							}
							return T;
						} else if (tDelegator2.mTileEntity instanceof MultiTileEntitySandwich) {
							ItemStack tStack = FL.fill(aFluid, IL.Bottle_Empty.get(1), F, F);
							if (ST.valid(tStack)) {
								FluidStack tFluid = FL.mul(FL.getFluid(tStack, T), ((MultiTileEntitySandwich)tDelegator2.mTileEntity).getIngredientCount(), 4, T);
								if (tFluid != null && tFluid.getAmount() <= aFluid.getAmount() && ((MultiTileEntitySandwich)tDelegator2.mTileEntity).addIngredient(tStack) > 0) {
									((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, tFluid.getAmount(), T);
									UT.Sounds.send(SFX.IC_SPRAY, 1.0F, 2.0F, this, F);
									return T;
								}
							}
							return T;
						}
						// Drop XP in case the Fluid is labeled as a Liquid
						if (FL.XP.is(aFluid)) {
							if (MD.OB.mLoaded) {
								try {
									int tXP = Math.min(LiquidXpUtils.liquidToXpRatio(aFluid.getAmount()), Math.max(10, UT.Code.roundUp(EnchantmentUtils.getExperienceForLevel(aPlayer.experienceLevel+1) - (EnchantmentUtils.getExperienceForLevel(aPlayer.experienceLevel) + (aPlayer.experienceProgress * aPlayer.getXpNeededForNextLevel())))));
									int tDrain = LiquidXpUtils.xpToLiquidRatio(tXP);
									if (tDrain > 0 && tXP > 0) {
										((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, tDrain, T);
										level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.5, getBlockPos().getY()+0.2, getBlockPos().getZ()+0.5, tXP));
									}
								} catch(Throwable e) {e.printStackTrace(ERR);}
								return T;
							}
							// Even if OpenBlocks is not installed, in case Liquid XP exists somewhere, just turn it into regular XP at the default Rate, with one bucket of XP per click.
							int tXP = Math.min(50, aFluid.getAmount()/20);
							if (tXP > 0) {
								((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, tXP*20, T);
								level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.5, getBlockPos().getY()+0.2, getBlockPos().getZ()+0.5, tXP));
							}
							return T;
						}
						// Act like Liquid XP too.
						if (FL.Mob.is(aFluid)) {
							int tXP = Math.min(50, (aFluid.getAmount()*3)/200);
							if (tXP > 0) {
								((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, (tXP*200)/3, T);
								level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.5, getBlockPos().getY()+0.2, getBlockPos().getZ()+0.5, tXP));
							}
							return T;
						}
						// Nothing left to check for empty Hands
						return T;
					}
					FluidStack tNewFluid = aFluid.copy();
					ItemStack tStack = FL.fill(tNewFluid, ST.amount(1, aStack), T, T, T, T);
					if (aFluid.getAmount() > tNewFluid.getAmount() && ((ITileEntityTapAccessible)tDelegator.mTileEntity).tapDrain(tDelegator.mSideOfTileEntity, aFluid.getAmount() - tNewFluid.getAmount(), T) != null) {
						UT.Sounds.send(SFX.IC_SPRAY, 1.0F, 2.0F, this, F);
						aStack.setCount(aStack.getCount()-1);
						ST.give(aPlayer, tStack, T);
						return T;
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
			case SIDE_Z_NEG: box(aBlock, PX_P[ 6], PX_P[ 6], PX_P[ 2], PX_N[ 6], PX_N[ 9], PX_N[12]); return T;
			default        : box(aBlock, PX_P[ 6], PX_P[ 6], PX_P[12], PX_N[ 6], PX_N[ 9], PX_N[ 2]); return T;
			case SIDE_X_NEG: box(aBlock, PX_P[ 2], PX_P[ 6], PX_P[ 6], PX_N[12], PX_N[ 9], PX_N[ 6]); return T;
			case SIDE_X_POS: box(aBlock, PX_P[12], PX_P[ 6], PX_P[ 6], PX_N[ 2], PX_N[ 9], PX_N[ 6]); return T;
			}
		case 1:
			switch(mFacing) {
			case SIDE_Z_NEG: box(aBlock, PX_P[ 7], PX_P[ 4], PX_P[ 0], PX_N[ 7], PX_N[10], PX_N[12]); return T;
			default        : box(aBlock, PX_P[ 7], PX_P[ 4], PX_P[12], PX_N[ 7], PX_N[10], PX_N[ 0]); return T;
			case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 4], PX_P[ 7], PX_N[12], PX_N[10], PX_N[ 7]); return T;
			case SIDE_X_POS: box(aBlock, PX_P[12], PX_P[ 4], PX_P[ 7], PX_N[ 0], PX_N[10], PX_N[ 7]); return T;
			}
		case 2:
			switch(mFacing) {
			case SIDE_Z_NEG: box(aBlock, PX_P[ 7], PX_P[ 3], PX_P[ 4], PX_N[ 7], PX_N[10], PX_N[10]); return T;
			default        : box(aBlock, PX_P[ 7], PX_P[ 3], PX_P[10], PX_N[ 7], PX_N[10], PX_N[ 4]); return T;
			case SIDE_X_NEG: box(aBlock, PX_P[ 4], PX_P[ 3], PX_P[ 7], PX_N[10], PX_N[10], PX_N[ 7]); return T;
			case SIDE_X_POS: box(aBlock, PX_P[10], PX_P[ 3], PX_P[ 7], PX_N[ 4], PX_N[10], PX_N[ 7]); return T;
			}
		}
		return T;
	}
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		return aRenderPass == 1 && (aSide == mFacing ? !aShouldSideBeRendered[aSide] : aSide == OPOS[mFacing]) ? null : BlockTextureMulti.get(BlockTextureDefault.get(sColoreds[FACES_TBS[aSide]], mRGBa), BlockTextureDefault.get(sOverlays[FACES_TBS[aSide]]));
	}
	
	// Icons
	public static IIconContainer sColoreds[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/tools/tap/colored/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/tools/tap/colored/top"),
		new Textures.BlockIcons.CustomIcon("machines/tools/tap/colored/side"),
	}, sOverlays[] = new IIconContainer[] {
		new Textures.BlockIcons.CustomIcon("machines/tools/tap/overlay/bottom"),
		new Textures.BlockIcons.CustomIcon("machines/tools/tap/overlay/top"),
		new Textures.BlockIcons.CustomIcon("machines/tools/tap/overlay/side"),
	};
	
	@Override
	public AABB getSelectedBoundingBoxFromPool() {
		switch(mFacing) {
		case SIDE_Z_NEG: return box(PX_P[ 6], PX_P[ 3], PX_P[ 0], PX_N[ 6], PX_N[ 9], PX_N[10]);
		default        : return box(PX_P[ 6], PX_P[ 3], PX_P[10], PX_N[ 6], PX_N[ 9], PX_N[ 0]);
		case SIDE_X_NEG: return box(PX_P[ 0], PX_P[ 3], PX_P[ 6], PX_N[10], PX_N[ 9], PX_N[ 6]);
		case SIDE_X_POS: return box(PX_P[10], PX_P[ 3], PX_P[ 6], PX_N[ 0], PX_N[ 9], PX_N[ 6]);
		}
	}
	
	@Override
	public void setBlockBoundsBasedOnState(Block aBlock) {
		switch(mFacing) {
		case SIDE_Z_NEG: box(aBlock, PX_P[ 6], PX_P[ 3], PX_P[ 0], PX_N[ 6], PX_N[ 9], PX_N[10]); break;
		default        : box(aBlock, PX_P[ 6], PX_P[ 3], PX_P[10], PX_N[ 6], PX_N[ 9], PX_N[ 0]); break;
		case SIDE_X_NEG: box(aBlock, PX_P[ 0], PX_P[ 3], PX_P[ 6], PX_N[10], PX_N[ 9], PX_N[ 6]); break;
		case SIDE_X_POS: box(aBlock, PX_P[10], PX_P[ 3], PX_P[ 6], PX_N[ 0], PX_N[ 9], PX_N[ 6]); break;
		}
	}
	
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.tap";}
}
