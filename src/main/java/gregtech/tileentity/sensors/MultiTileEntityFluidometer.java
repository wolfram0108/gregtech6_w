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
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.tileentity.sensors;

import gregapi.data.FL;
import static gregapi.data.CS.*;

import gregapi.data.BI;
import gregapi.data.LH;
import gregapi.old.Textures;
import gregapi.render.IIconContainer;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.machines.MultiTileEntitySensorTE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import gregapi.fluid.FluidTankInfo;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityFluidometer extends MultiTileEntitySensorTE {
	static {LH.add("gt.tooltip.sensor.fluidometer", "Measures Fluids (In Liters)");}
	@Override public String getSensorDescription() {return LH.get("gt.tooltip.sensor.fluidometer");}
	
	@Override
	public long getCurrentValue(DelegatorTileEntity<BlockEntity> aDelegator) {
		if (aDelegator.mTileEntity instanceof IFluidHandler) {
			FluidTankInfo[] tInfo = FL.getTankInfo((IFluidHandler)aDelegator.mTileEntity, aDelegator.mSideOfTileEntity);
			if (tInfo != null) {
				long rFluid = 0;
				for (FluidTankInfo tTank : tInfo) if (tTank != null && tTank.fluid != null) rFluid += tTank.fluid.getAmount();
				return rFluid;
			}
		}
		// F5 §6.2: «сколько в клетке» — один вопрос центру (ванильные источники, воды GT6 и кванты нефтей),
		// вместо ветки «мета 0 = 1000» плюс отдельной ветки IFluidBlock.
		FluidStack tFluid = FL.drainable(aDelegator.mWorld, new net.minecraft.core.BlockPos(aDelegator.mX, aDelegator.mY, aDelegator.mZ));
		return tFluid == null ? 0 : tFluid.getAmount();
	}
	
	@Override
	public long getCurrentMax(DelegatorTileEntity<BlockEntity> aDelegator) {
		if (aDelegator.mTileEntity instanceof IFluidHandler) {
			FluidTankInfo[] tInfo = FL.getTankInfo((IFluidHandler)aDelegator.mTileEntity, aDelegator.mSideOfTileEntity);
			if (tInfo != null) {
				long rCapacity = 0;
				for (FluidTankInfo tTank : tInfo) if (tTank != null) rCapacity += tTank.capacity;
				return rCapacity;
			}
		}
		return 0;
	}
	
	@Override public short[] getSymbolColor() {return CA_BLUE_255;}
	@Override public IIconContainer getSymbolIcon() {return BI.CHAR_LITER;}
	@Override public IIconContainer getTextureFront() {return sTextureFront;}
	@Override public IIconContainer getTextureBack () {return sTextureBack;}
	@Override public IIconContainer getTextureSide () {return sTextureSide;}
	@Override public IIconContainer getOverlayFront() {return sOverlayFront;}
	@Override public IIconContainer getOverlayBack () {return sOverlayBack;}
	@Override public IIconContainer getOverlaySide () {return sOverlaySide;}
	
	public static IIconContainer
	sTextureFront   = new Textures.BlockIcons.CustomIcon("machines/redstone/sensors/fluidometer/colored/front"),
	sTextureBack    = new Textures.BlockIcons.CustomIcon("machines/redstone/sensors/fluidometer/colored/back"),
	sTextureSide    = new Textures.BlockIcons.CustomIcon("machines/redstone/sensors/fluidometer/colored/side"),
	sOverlayFront   = new Textures.BlockIcons.CustomIcon("machines/redstone/sensors/fluidometer/overlay/front"),
	sOverlayBack    = new Textures.BlockIcons.CustomIcon("machines/redstone/sensors/fluidometer/overlay/back"),
	sOverlaySide    = new Textures.BlockIcons.CustomIcon("machines/redstone/sensors/fluidometer/overlay/side");
	
	@Override public String getTileEntityName() {return "gt.multitileentity.redstone.sensors.fluidometer";}
}
