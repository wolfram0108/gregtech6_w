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

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.tiles.IHasWork;
import buildcraft.api.tiles.TilesAPI;
import buildcraft.api.transport.IInjectable;
import buildcraft.api.transport.pipe.PipeApi;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.code.TagData;
import gregapi.compat.CompatBase;
import gregapi.data.TD;
import gregapi.tileentity.base.TileEntityBase01Root;
import gregapi.tileentity.energy.GT6EnergyCapability;
import gregapi.tileentity.machines.MultiTileEntityBasicMachine;
import gregapi.util.ST;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.NonNullSupplier;

import static gregapi.data.CS.*;

/** BuildCraft: Community Edition 7.99 dropped the world properties GT6 1.7.10 answered "wood" through (BuildCraftAPI:30-39;
 *  its robots read #minecraft:logs), so that part has nothing to join here. */
public class CompatBC extends CompatBase implements ICompatBC {
	public CompatBC() {
		TriggerBC_Energy_Capacity_Empty.class.getCanonicalName();
		TriggerBC_Energy_Capacity_Partial.class.getCanonicalName();
		TriggerBC_Energy_Capacity_NotFull.class.getCanonicalName();
		TriggerBC_Energy_Capacity_Full.class.getCanonicalName();
		if (CODE_CLIENT) Client.register();
	}

	/** 1.7.10's «fuel» was one fluid of BuildCraft and GT6; BuildCraft 7.99 calls its successor fuel_light, so GT6 Fuel burns at
	 *  its rate, read from BuildCraft's recipes wherever they are loaded: at server start, and when a client receives them. */
	static void registerFuel(Level aLevel) {
		net.minecraftforge.fluids.FluidStack tLight = gregapi.data.FL.make(gregapi.compat.BuildCraftNames.FUEL, 1), tFuel = gregapi.data.FL.Fuel.make(1);
		buildcraft.api.fuels.IFuelManager tManager = buildcraft.api.fuels.BuildcraftFuelRegistry.fuel;
		buildcraft.api.fuels.IFuel tRate = aLevel == null || tLight == null || tFuel == null || tManager == null ? null : tManager.getFuel(aLevel, tLight);
		if (tRate != null && tManager.getFuel(aLevel, tFuel) == null) tManager.addUnregisteredFuel(new net.minecraft.resources.ResourceLocation(gregapi.data.MD.GAPI.mID, "fuel"), tFuel, tRate.getPowerPerCycle(), tRate.getTotalBurningTime());
	}

	/** Client types stay in here, so the class loads on a dedicated server. */
	private static final class Client {
		static void register() {net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOW, false, net.minecraftforge.client.event.RecipesUpdatedEvent.class, aEvent -> registerFuel(net.minecraft.client.Minecraft.getInstance().level));}
	}

	@Override
	public void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent aEvent) {
		registerFuel(aEvent.getServer().overworld());
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
	public boolean rotateBlock(Level aWorld, BlockPos aPos, BlockState aState, Direction aSide) {
		return aState.getBlock() instanceof ICustomRotationHandler && ((ICustomRotationHandler)aState.getBlock()).attemptRotation(aWorld, aPos, aState, aSide).consumesAction();
	}

	// In 1.7.10 BuildCraft engines pushed RF into GT6's RF blocks and gates read IHasWork off GT6 machines; BuildCraft 7.99
	// carries MJ and asks both through Forge capabilities, so the same blocks answer them there.
	@Override
	public NonNullSupplier<Object> capability(TileEntityBase01Root aRoot, Capability<?> aCapability, Direction aSide) {
		if (aCapability == MjAPI.CAP_RECEIVER || aCapability == MjAPI.CAP_CONNECTOR) {
			TileEntityBase01Root tRoot = GT6EnergyCapability.flux(aRoot, aSide);
			return tRoot == null ? null : () -> new FluxMjReceiver(tRoot, aSide);
		}
		if (aCapability == TilesAPI.CAP_HAS_WORK && aRoot instanceof MultiTileEntityBasicMachine tMachine) return () -> (IHasWork)tMachine::hasWork;
		return null;
	}

	@Override
	public long insertRF(BlockEntity aReceiver, byte aSide, long aRF) {
		if (aReceiver == null || aReceiver.getLevel() == null) return -1;
		IMjReceiver tMJ;
		try {tMJ = aReceiver.getCapability(MjAPI.CAP_RECEIVER, FORGE_DIR[aSide]).resolve().orElse(null);} catch (Throwable e) {return -1;}
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

	// BuildCraft 7.99 pipes hand out their item injector as a capability (PipeApi.CAP_INJECTABLE); 7.1.23 pipe tiles
	// implemented IInjectable themselves. ST keeps the 1.7.10 flow and asks these three, so no BuildCraft type sits in an always-loaded class.
	private static IInjectable injectable(Object aTileEntity, Direction aSide) {
		if (!(aTileEntity instanceof BlockEntity)) return null;
		try {return ((BlockEntity)aTileEntity).getCapability(PipeApi.CAP_INJECTABLE, aSide).resolve().orElse(null);} catch (Throwable e) {return null;}
	}
	public static boolean isInjectable(Object aTileEntity, Direction aSide) {return injectable(aTileEntity, aSide) != null;}
	public static boolean canInject(Object aTileEntity, Direction aSide) {IInjectable tPipe = injectable(aTileEntity, aSide); return tPipe != null && tPipe.canInjectItems(aSide);}
	/** Items the pipe takes, as 7.1.23 injectItem returned; 7.99 returns the leftover, and colour and speed take BuildCraft's own defaults. */
	public static int inject(Object aTileEntity, ItemStack aStack, boolean aDoAdd, Direction aSide) {
		IInjectable tPipe = injectable(aTileEntity, aSide);
		if (tPipe == null || ST.invalid(aStack)) return 0;
		ItemStack tLeft = tPipe.injectItem(aStack, aDoAdd, aSide, null, 0);
		return aStack.getCount() - (tLeft == null ? 0 : tLeft.getCount());
	}
}
