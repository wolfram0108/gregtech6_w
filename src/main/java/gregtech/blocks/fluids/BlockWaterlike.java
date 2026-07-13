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

package gregtech.blocks.fluids;

import gregapi.block.IBlock;
import gregapi.block.IBlockOnHeadInside;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.item.IItemGT;
import gregapi.lang.LanguageHandler;
import gregapi.render.RendererBlockFluid;
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockWaterlike extends BlockFluidClassic implements IBlock, IItemGT, IBlockOnHeadInside {
	public static int WATER_UPDATE_FLAGS = 0;
	
	public final Fluid mFluid;
	
	public BlockWaterlike(String aName, Fluid aFluid, boolean aFlowsOut, boolean aHide) {
		super(aFluid, Material.water);
		mFluid = aFluid;
		quantaPerBlock = (aFlowsOut ? 8 : 3);
		quantaPerBlockFloat = quantaPerBlock;
		setResistance(30);
		setBlockName(aName);
		setLightOpacity(LIGHT_OPACITY_WATER);
		ST.register(this, aName, BlockItem.class);
		LH.add(getUnlocalizedName(), getLocalizedName());
		LanguageHandler.set(getLocalizedName(), getLocalizedName()); // WAILA is retarded...
		setFluidStack(FL.make(aFluid, 1000));
		if (aHide) ST.hide(this);
	}
	
	// @Override
	public FluidStack drain(Level aWorld, int aX, int aY, int aZ, boolean aDoDrain) {
		if (aDoDrain) WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
		return FL.make(getFluid(), 1000);
	}
	
	// @Override
	public boolean canDrain(Level aWorld, int aX, int aY, int aZ) {
		return WD.meta(aWorld, aX, aY, aZ) == 0;
	}
	
	public void updateFlow(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		int quantaRemaining = quantaPerBlock - WD.meta(aWorld, aX, aY, aZ);
		int expQuanta = -101;
		// check adjacent block levels if non-source
		if (quantaRemaining < quantaPerBlock) {
			if (WD.block(aWorld, aX  , aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX-1, aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX+1, aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX  , aY-densityDir, aZ-1) instanceof BlockWaterlike ||
				WD.block(aWorld, aX  , aY-densityDir, aZ+1) instanceof BlockWaterlike) {
				expQuanta = quantaPerBlock - 1;
			} else {
				int maxQuanta = -100;
				maxQuanta = getLargerQuanta(aWorld, aX-1, aY, aZ  , maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX+1, aY, aZ  , maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX  , aY, aZ-1, maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX  , aY, aZ+1, maxQuanta);
				expQuanta = maxQuanta - 1;
			}
			if (expQuanta != quantaRemaining) {
				quantaRemaining = expQuanta;
				if (expQuanta <= 0) {
					WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				} else {
					WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), quantaPerBlock - expQuanta, 3, F);
					aWorld.scheduleBlockUpdate(aX, aY, aZ, this, tickRate);
					aWorld.notifyBlocksOfNeighborChange(aX, aY, aZ, this);
				}
			}
		}
		// Here was an else Block that only caused huge amounts of Network Lag with no purpose. Forge, just what the fuck, setting Metadata from 0 to 0 and updating that "change" to Clients? There was no change that needed to be updated!
		
		
		if (canDisplace(aWorld, aX, aY+densityDir, aZ)) {
			if (displaceIfPossible(aWorld, aX, aY+densityDir, aZ)) WD.set(aWorld, aX, aY+densityDir, aZ, this, 1, WATER_UPDATE_FLAGS | 1);
			return;
		}
		
		int tFlowMeta  = (WD.block(aWorld, aX, aY-densityDir, aZ) instanceof BlockWaterlike ? 1 : quantaPerBlock - quantaRemaining + 1);
		if (tFlowMeta >= quantaPerBlock) return;
		
		if (WD.exists(aWorld, aX, aY, aZ-1) && displaceIfPossible(aWorld, aX  , aY, aZ-1)) WD.set(aWorld, aX  , aY, aZ-1, this, tFlowMeta, WATER_UPDATE_FLAGS | 1);
		if (WD.exists(aWorld, aX, aY, aZ+1) && displaceIfPossible(aWorld, aX  , aY, aZ+1)) WD.set(aWorld, aX  , aY, aZ+1, this, tFlowMeta, WATER_UPDATE_FLAGS | 1);
		if (WD.exists(aWorld, aX-1, aY, aZ) && displaceIfPossible(aWorld, aX-1, aY, aZ  )) WD.set(aWorld, aX-1, aY, aZ  , this, tFlowMeta, WATER_UPDATE_FLAGS | 1);
		if (WD.exists(aWorld, aX+1, aY, aZ) && displaceIfPossible(aWorld, aX+1, aY, aZ  )) WD.set(aWorld, aX+1, aY, aZ  , this, tFlowMeta, WATER_UPDATE_FLAGS | 1);
	}
	
	// @Override
	public Vec3 getFlowVector(BlockGetter aWorld, int aX, int aY, int aZ) {
		Vec3 rVector = Vec3.createVectorHelper(0, 0, 0);
		int tDecay = quantaPerBlock - getQuantaValue(aWorld, aX, aY, aZ);
		for (byte tSide : ALL_SIDES_HORIZONTAL) {
			int tX = aX+OFFX[tSide], tZ = aZ+OFFZ[tSide];
			int tOtherDecay = quantaPerBlock - getQuantaValue(aWorld, tX, aY, tZ);
			if (tOtherDecay >= quantaPerBlock) {
				if (!WD.getMaterial(WD.block(aWorld, tX, aY, tZ)).blocksMovement()) {
					tOtherDecay = quantaPerBlock - getQuantaValue(aWorld, tX, aY-1, tZ);
					if (tOtherDecay >= 0) {
						int tPower = tOtherDecay - (tDecay - quantaPerBlock);
						rVector = rVector.addVector((tX - aX) * tPower, 0, (tZ - aZ) * tPower);
					}
				}
			} else if (tOtherDecay >= 0) {
				int power = tOtherDecay - tDecay;
				rVector = rVector.addVector((tX - aX) * power, 0, (tZ - aZ) * power);
			}
		}
		if (WD.block(aWorld, aX, aY+1, aZ) instanceof BlockWaterlike && (
			isBlockSolid(aWorld, aX  , aY  , aZ-1, SIDE_Z_NEG) ||
			isBlockSolid(aWorld, aX  , aY  , aZ+1, SIDE_Z_POS) ||
			isBlockSolid(aWorld, aX-1, aY  , aZ  , SIDE_X_NEG) ||
			isBlockSolid(aWorld, aX+1, aY  , aZ  , SIDE_X_POS) ||
			isBlockSolid(aWorld, aX  , aY+1, aZ-1, SIDE_Z_NEG) ||
			isBlockSolid(aWorld, aX  , aY+1, aZ+1, SIDE_Z_POS) ||
			isBlockSolid(aWorld, aX-1, aY+1, aZ  , SIDE_X_NEG) ||
			isBlockSolid(aWorld, aX+1, aY+1, aZ  , SIDE_X_POS))) {
			rVector = rVector.normalize().addVector(0, -6, 0);
		}
		return rVector.normalize();
	}
	
	// @Override
	public int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return 0;
		if (aBlock == this) return quantaPerBlock - WD.meta(aWorld, aX, aY, aZ);
		if (aBlock instanceof BlockWaterlike) return 8-WD.meta(aWorld, aX, aY, aZ);
		if (aBlock == Blocks.WATER || aBlock == Blocks.WATER) return 8-WD.meta(aWorld, aX, aY, aZ);
		return -1;
	}
	
	// @Override
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return T;
		if (WD.getMaterial(aBlock) == Material.water || WD.visOpq(aBlock)) return F;
		if (aBlock.isAir(aWorld, aX, aY, aZ)) return T;
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity instanceof ITileEntitySurface) return !((ITileEntitySurface)tTileEntity).isSurfaceOpaque(OPOS[aSide]);
		return T;
	}
	
	public boolean isSourceBlock(BlockGetter aWorld, int aX, int aY, int aZ) {return WD.block(aWorld, aX, aY, aZ) instanceof BlockWaterlike && WD.meta(aWorld, aX, aY, aZ) == 0;}
	@Override public Block getBlock() {return this;}
	public final String getUnlocalizedName() {return FL.name(mFluid, F);}
	public String getLocalizedName() {return FL.name(mFluid, T);}
	public void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public int getRenderType() {return RendererBlockFluid.RENDER_ID;}
	public int getRenderBlockPass() {return 1;}
	public int getLightOpacity() {return LIGHT_OPACITY_WATER;}
	public IIcon getIcon(int aSide, int aMeta) {return Blocks.WATER.getIcon(aSide, aMeta);}
	public int getRenderColor(int aMeta) {return 0x00ffffff;}
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return 0x00ffffff;}
	
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return 0;}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return 0;}
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.canDisplace(aWorld, aX, aY, aZ);}
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.displaceIfPossible(aWorld, aX, aY, aZ);}
	public boolean canCollideCheck(int aMeta, boolean aFullHit) {return aFullHit && aMeta == 0;}
	public boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {return !mEffects.isEmpty();}
	public boolean isNormalCube() {return F;}
	public boolean isOpaqueCube() {return F;}
	public boolean func_149730_j() {return F;}
	public boolean getTickRandomly() {return F;}
	public boolean renderAsNormalBlock() {return F;}
	public boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return F;}
	
	public BlockWaterlike addEffect(int aEffectID, int aEffectDuration, int aEffectLevel) {
		mEffects.add(new int[] {aEffectID, aEffectDuration, aEffectLevel});
		return this;
	}
	
	public List<int[]> mEffects = new ArrayListNoNulls<>();
	
	@Override
	public void onHeadInside(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !mEffects.isEmpty() && (FL.gas(mFluid) ? !UT.Entities.isImmuneToBreathingGases(aEntity) : !UT.Entities.isWearingFullChemHazmat(aEntity))) {
			for (int[] tEffects : mEffects) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
			if (getMaterial() != Material.water && SERVER_TIME % 20 == 0) aEntity.attackEntityFrom(DamageSource.drown, 2.0F);
		}
	}
}
