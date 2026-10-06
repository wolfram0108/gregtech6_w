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

package gregapi.compat.buildcraft;
import gregapi.util.WD;

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.core.BuildCraftAPI;
import buildcraft.api.core.IWorldProperty;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import gregapi.api.FMLPostInitializationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import gregapi.code.TagData;
import gregapi.compat.CompatBase;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.util.ST;
import gregapi.wooddict.WoodDictionary;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import gregapi.tileentity.base.TileEntityBase01Root;
import gregapi.tileentity.energy.GT6EnergyCapability;

import static gregapi.data.CS.*;


public class CompatBC extends CompatBase implements ICompatBC {
	public CompatBC() {
		TriggerBC_Energy_Capacity_Empty.class.getCanonicalName();
		TriggerBC_Energy_Capacity_Partial.class.getCanonicalName();
		TriggerBC_Energy_Capacity_NotFull.class.getCanonicalName();
		TriggerBC_Energy_Capacity_Full.class.getCanonicalName();
		IWorldProperty.class.getCanonicalName();
		BuildCraftAPI.class.getCanonicalName();
	}
	
	@Override
	public void onPostLoad(FMLPostInitializationEvent aEvent) {
		for (TagData tEnergyType : TD.Energy.ALL) {
			new TriggerBC_Energy_Capacity_Empty(tEnergyType);
			new TriggerBC_Energy_Capacity_Partial(tEnergyType);
			new TriggerBC_Energy_Capacity_NotFull(tEnergyType);
			new TriggerBC_Energy_Capacity_Full(tEnergyType);
		}
	}
	
	@Override
	public void onServerStarting(ServerStartingEvent aEvent) {
		BuildCraftAPI.registerWorldProperty("wood", new WorldPropertyIsLog());
	}
	
	@Override
	public boolean rotateBlock(Level aWorld, BlockPos aPos, BlockState aState, Direction aSide) {
		return aState.getBlock() instanceof ICustomRotationHandler && ((ICustomRotationHandler)aState.getBlock()).attemptRotation(aWorld, aPos, aState, aSide).consumesAction();
	}
	
	// In 1.7.10 BuildCraft engines pushed RF into GT6's RF blocks and GT6 generators pushed RF into BuildCraft; BuildCraft 26.1
	// carries MJ instead, so the same blocks meet it in MJ at the rate BuildCraft itself converts MJ and RF with.
	@Override
	public void registerCapabilities(RegisterCapabilitiesEvent aEvent, Block[] aBlocks) {
		aEvent.registerBlock(MjAPI.CAP_RECEIVER , CompatBC::mjAt, aBlocks);
		aEvent.registerBlock(MjAPI.CAP_CONNECTOR, CompatBC::mjAt, aBlocks);
	}
	
	private static FluxMjReceiver mjAt(Level aLevel, BlockPos aPos, BlockState aState, BlockEntity aBlockEntity, Direction aSide) {
		TileEntityBase01Root tRoot = GT6EnergyCapability.flux(aBlockEntity, aSide);
		return tRoot == null ? null : new FluxMjReceiver(tRoot, aSide);
	}
	
	@Override
	public long insertRF(BlockEntity aReceiver, byte aSide, long aRF) {
		if (aReceiver == null || aReceiver.getLevel() == null) return -1;
		IMjReceiver tMJ;
		try {tMJ = MjAPI.CAP_RECEIVER.getCapability(aReceiver.getLevel(), aReceiver.getBlockPos(), null, aReceiver, FORGE_DIR[aSide]);} catch (Throwable e) {return -1;}
		if (tMJ == null) return -1;
		long tPerRF = MjAPI.getRfConversion().mjPerRf, tOffer = Math.min(aRF * tPerRF, tMJ.getPowerRequested());
		if (!tMJ.canReceive() || tOffer <= 0) return 0;
		return (tOffer - tMJ.receivePower(tOffer, F)) / tPerRF;
	}
	
	/** A GT6 RF block as an MJ receiver: MJ in, converted at BuildCraft's rate, goes to receiveEnergy like 1.7.10 RF did. */
	private static final class FluxMjReceiver implements IMjReceiver {
		private final TileEntityBase01Root mRoot;
		private final Direction mSide;
		FluxMjReceiver(TileEntityBase01Root aRoot, Direction aSide) {mRoot = aRoot; mSide = aSide;}
		@Override public boolean canConnect(IMjConnector aOther) {return T;}
		@Override public long getPowerRequested() {return (long)mRoot.receiveEnergy(mSide, Integer.MAX_VALUE, T) * MjAPI.getRfConversion().mjPerRf;}
		@Override public long receivePower(long aMicroJoules, boolean aSimulate) {
			long tPerRF = MjAPI.getRfConversion().mjPerRf;
			int tRF = (int)Math.min(Integer.MAX_VALUE, aMicroJoules / tPerRF);
			return aMicroJoules - (long)mRoot.receiveEnergy(mSide, tRF, aSimulate) * tPerRF;
		}
	}
	
	// BuildCraft 26.1 pipes hand out their item injector per flow (PipeApi.CAP_INJECTABLE); 7.1.23 pipe tiles implemented
	// IInjectable themselves. ST keeps the 1.7.10 flow and asks these three, so no BuildCraft type sits in an always-loaded class.
	private static IInjectable injectable(Object aTileEntity, Direction aSide) {
		if (!(aTileEntity instanceof IPipeHolder)) return null;
		IPipe tPipe = ((IPipeHolder)aTileEntity).getPipe();
		return tPipe == null || tPipe.getFlow() == null ? null : tPipe.getFlow().getCapability(PipeApi.CAP_INJECTABLE, aSide);
	}
	public static boolean isInjectable(Object aTileEntity, Direction aSide) {return injectable(aTileEntity, aSide) != null;}
	public static boolean canInject(Object aTileEntity, Direction aSide) {IInjectable tPipe = injectable(aTileEntity, aSide); return tPipe != null && tPipe.canInjectItems(aSide);}
	/** Items the pipe takes, as 7.1.23 injectItem returned; 26.1 returns the leftover, and colour and speed take BuildCraft's own defaults. */
	public static int inject(Object aTileEntity, ItemStack aStack, boolean aDoAdd, Direction aSide) {
		IInjectable tPipe = injectable(aTileEntity, aSide);
		if (tPipe == null || ST.invalid(aStack)) return 0;
		ItemStack tLeft = tPipe.injectItem(aStack, aDoAdd, aSide, null, 0);
		return aStack.getCount() - (tLeft == null ? 0 : tLeft.getCount());
	}
	
	// BuildCraft 26.1 asks a world property by position; 7.1.23 resolved block and meta first (WorldProperty.get) and GT6
	// answered from them, so this resolves the same pair and keeps the 1.7.10 predicate. Nothing is cached, so clear() is empty.
	public static class WorldPropertyIsLog implements IWorldProperty {
		@Override
		public boolean get(Level aWorld, BlockPos aPos) {
			int aX = aPos.getX(), aY = aPos.getY(), aZ = aPos.getZ();
			Block aBlock = WD.block(aWorld, aX, aY, aZ);
			byte aMeta = WD.meta(aWorld, aX, aY, aZ);
			return aBlock instanceof HugeMushroomBlock || WD.wood(aBlock, aWorld, aX, aY, aZ) || OP.log.contains(ST.make(aBlock, 1, aMeta)) || WoodDictionary.WOODS.containsKey(aBlock, aMeta, T);
		}
		
		@Override
		public void clear() {/**/}
	}
}
