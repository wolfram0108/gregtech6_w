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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port (https://github.com/wolfram0108/gregtech6_w):
 * ported from GregTech6-Unofficial for Minecraft 1.7.10 / Forge to an addon for Minecraft 26.1.2 / NeoForge.
 */

package gt6u.tileentity.misc;

import gregapi.util.WD;

import gt6u.data.BI_GT6U;

import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.data.FL;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureFluid;
import gregapi.render.BlockTextureMulti;
import gregapi.render.ITexture;
import gregapi.tileentity.base.TileEntityBase04MultiTileEntities;
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import gregapi.block.fluid.BlockFluidBaseGT;
import gregapi.util.ST;
import net.minecraftforge.fluids.FluidStack;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityFluidWell extends TileEntityBase04MultiTileEntities implements IMTE_OnRegistration, ITileEntitySurface, IMTE_IsSideSolid, IMTE_GetExplosionResistance, IMTE_GetBlockHardness, IMTE_GetLightOpacity, IMTE_SyncDataShort {
	public FluidStack mFluid = FL.Water.make(1);
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains("gt.well")) mFluid = FL.load(aNBT, "gt.well");
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		FL.save(aNBT, "gt.well", mFluid);
	}
	
	@Override
	public final CompoundTag writeItemNBT(CompoundTag aNBT) {
		aNBT = super.writeItemNBT(aNBT);
		FL.save(aNBT, "gt.well", mFluid);
		return aNBT;
	}
	
	public static MultiTileEntityRegistry MTE_REGISTRY = null;
	public static MultiTileEntityFluidWell INSTANCE;
	
	@Override
	public IPacket getClientDataPacket(boolean aSendAll) {
		return getClientDataPacketShort(aSendAll, (short)FL.id_(mFluid.getFluid()));
	}
	
	@Override
	public boolean receiveDataShort(short aData, INetworkHandler aNetworkHandler) {
		mFluid = FL.make(FL.fluid(aData), 600);
		return T;
	}
	
	@Override
	public void onRegistration(MultiTileEntityRegistry aRegistry, short aID) {
		INSTANCE = this;
		MTE_REGISTRY = aRegistry;
	}
	
	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (aIsServerSide) {
			if (rng(mFluid.getAmount()) == 0) {
				// The fluid's block as MultiTileEntityFluidSpring finds it: GT6's own block first, else the engine's.
				Block tBlock = FL.BLOCKS.get(FL.regName(mFluid.getFluid())), tAbove = getBlockAtSide(SIDE_UP);
				if (ST.invalid(tBlock)) tBlock = mFluid.getFluid().defaultFluidState().createLegacyBlock().getBlock();
				if (WD.liquid_finite(tBlock)) {
					if (tAbove == tBlock) {
						WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, UT.Code.bind4(getMetaDataAtSide(SIDE_UP)+8), 3);
						((BlockFluidBaseGT)tBlock).onBlockAdded(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ()); // finite fluids start ticking here, as in MultiTileEntityFluidSpring
					} else if (WD.air(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tAbove)) {
						WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, 7, 3);
						((BlockFluidBaseGT)tBlock).onBlockAdded(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ());
					}
				} else {
					if (tAbove == tBlock) {
						if (getMetaDataAtSide(SIDE_UP) == 0) {
							for (byte tSide : ALL_SIDES_HORIZONTAL) {
								tAbove = getBlock(getBlockPos().getX()+OFFSETS_X[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFSETS_Z[tSide]);
								if (tAbove == tBlock) {
									if (0 != getMetaData(getBlockPos().getX()+OFFSETS_X[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFSETS_Z[tSide])) {
										WD.set(level, getBlockPos().getX()+OFFSETS_X[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFSETS_Z[tSide], tBlock, 0, 3);
										break;
									}
								} else if (WD.air(level, getBlockPos().getX()+OFFSETS_X[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFSETS_Z[tSide], tAbove)) {
									WD.set(level, getBlockPos().getX()+OFFSETS_X[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFSETS_Z[tSide], tBlock, 0, 3);
									break;
								}
							}
						} else {
							WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, 0, 3);
						}
					} else if (WD.air(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tAbove)) {
						WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, 0, 3);
					}
				}
			}
		}
	}
	
	@Override public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return aShouldSideBeRendered[aSide] ? BlockTextureMulti.get(BlockTextureFluid.get(mFluid), BlockTextureDefault.get(BI_GT6U.FLUID_WELL)) : null;}
	
	@Override public int getLightOpacity() {return LIGHT_OPACITY_MAX;}
	
	@Override public float getExplosionResistance2() {return Blocks.STONE.getExplosionResistance();}
	@Override public float getBlockHardness() {return 30;}
	
	@Override public boolean isSurfaceSolid         (byte aSide) {return T;}
	@Override public boolean isSurfaceOpaque        (byte aSide) {return T;}
	@Override public boolean isSideSolid            (byte aSide) {return T;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.fluid.well";}
}
