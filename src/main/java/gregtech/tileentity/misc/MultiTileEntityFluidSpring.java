/**
 * Copyright (c) 2024 GregTech-6 Team
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

package gregtech.tileentity.misc;

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
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityFluidSpring extends TileEntityBase04MultiTileEntities implements IMTE_OnRegistration, ITileEntitySurface, IMTE_IsSideSolid, IMTE_GetExplosionResistance, IMTE_GetBlockHardness, IMTE_GetLightOpacity, IMTE_SyncDataShort {
	public FluidStack mFluid = FL.Water.make(1);
	public boolean mActive = F;
	// ADAPT-004 (нововведение по запросу игрока, ADAPTATIONS.md): конфиг-множитель темпа produce. Читается ОДИН раз
	// на mod-load в Loader_Worldgen (рядом с tInfiniteOil/Gas). Применяется при produce (не в worldgen-NBT) → живо
	// действует на ВСЕ родники (старые+новые). Дефолт 1.0 → делитель == amount → поведение строго 1:1 с оригиналом.
	public static double PRODUCTION_MULTIPLIER = 1.0;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains("gt.spring")) mFluid = FL.load(aNBT, "gt.spring");
		if (aNBT.contains(NBT_ACTIVE)) mActive = aNBT.getBooleanOr(NBT_ACTIVE, false);
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		FL.save(aNBT, "gt.spring", mFluid);
		UT.NBT.setBoolean(aNBT, NBT_ACTIVE, mActive);
	}
	
	@Override
	public final CompoundTag writeItemNBT(CompoundTag aNBT) {
		aNBT = super.writeItemNBT(aNBT);
		FL.save(aNBT, "gt.spring", mFluid);
		return aNBT;
	}
	
	public static MultiTileEntityRegistry MTE_REGISTRY = null;
	public static MultiTileEntityFluidSpring INSTANCE;
	
	public static boolean setBlock(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, FluidStack aSpring) {
		return MTE_REGISTRY.mBlock.placeBlock(aWorld, aX, aY, aZ, SIDE_UP, INSTANCE.getMultiTileEntityID(), UT.NBT.make("gt.spring", aSpring), T, F);
	}
	
	@Override
	public IPacket getClientDataPacket(boolean aSendAll) {
		return getClientDataPacketShort(aSendAll, (short)gregapi.data.FL.id_(mFluid.getFluid()));
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
		if (mFluid.getAmount() <= 0) mFluid.setAmount(600);
		if (aIsServerSide) {
			boolean tProduce = F;
			if (mActive) {
				// ADAPT-004: делитель шанса = amount/множитель → шанс = множитель/amount; кламп делителя ≥1 (шанс не выше 1/тик).
				tProduce = (rng((int) Math.max(1, Math.round(mFluid.getAmount() / PRODUCTION_MULTIPLIER))) == 0);
			} else if (SERVER_TIME % 20 == 1 && !WD.liquid(getBlockAtSide(SIDE_UP))) {
				tProduce = mActive = T;
			}
			if (tProduce) {
				Block tBlock = FL.BLOCKS.get(FL.regName(mFluid.getFluid())), tAbove = getBlockAtSide(SIDE_UP);
				if (ST.invalid(tBlock)) tBlock = mFluid.getFluid().defaultFluidState().createLegacyBlock().getBlock();
				if (ST.valid(tBlock)) {
					if (WD.liquid_finite(tBlock)) {
						if (tAbove == tBlock) {
							WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, UT.Code.bind4(getMetaDataAtSide(SIDE_UP)+8), 3);
							((gregapi.block.fluid.BlockFluidBaseGT)tBlock).onBlockAdded(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ()); // было tBlock.updateTick(...) → порт-транскрипция randomTick оказалась no-op (дефолт neo randomTick пуст, BlockBehaviour:334); течение — через ЕДИНЫЙ центральный планировщик тика блока (тот же, что зовёт постановка/сосед-апдейт)
						} else if (WD.liquid(tAbove) || WD.air(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tAbove)) {
							WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, 7, 3);
							((gregapi.block.fluid.BlockFluidBaseGT)tBlock).onBlockAdded(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ()); // было tBlock.updateTick(...) → порт-транскрипция randomTick оказалась no-op (дефолт neo randomTick пуст, BlockBehaviour:334); течение — через ЕДИНЫЙ центральный планировщик тика блока (тот же, что зовёт постановка/сосед-апдейт)
						}
					} else {
						if (tAbove == tBlock) {
							if (getMetaDataAtSide(SIDE_UP) == 0) {
								for (byte tSide : ALL_SIDES_HORIZONTAL) {
									tAbove = getBlock(getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFZ[tSide]);
									if (tAbove == tBlock) {
										if (0 != getMetaData(getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFZ[tSide])) {
											WD.set(level, getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFZ[tSide], tBlock, 0, 3);
											break;
										}
									} else if (WD.air(level, getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFZ[tSide], tAbove)) {
										WD.set(level, getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+1, getBlockPos().getZ()+OFFZ[tSide], tBlock, 0, 3);
										break;
									}
								}
							} else {
								WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, 0, 3);
							}
						} else if (WD.liquid(tAbove) || WD.air(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tAbove)) {
							WD.set(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), tBlock, 0, 3);
						}
					}
				}
			}
		}
	}
	
	@Override public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return aShouldSideBeRendered[aSide] ? BlockTextureMulti.get(BlockTextureFluid.get(mFluid), BlockTextureDefault.get(Textures.BlockIcons.FLUID_SPRING)) : null;}
	
	@Override public int getLightOpacity() {return LIGHT_OPACITY_MAX;}
	
	@Override public float getExplosionResistance2() {return Blocks.BEDROCK.getExplosionResistance();}
	@Override public float getBlockHardness() {return -1;}
	
	@Override public boolean isSurfaceSolid         (byte aSide) {return T;}
	@Override public boolean isSurfaceOpaque        (byte aSide) {return T;}
	@Override public boolean isSideSolid            (byte aSide) {return T;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.fluid.spring";}
}
