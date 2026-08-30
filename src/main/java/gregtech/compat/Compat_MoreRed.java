/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregtech.compat;

import static gregapi.data.CS.*;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import gregapi.api.Abstract_Mod;
import gregapi.api.FMLInitializationEvent;
import gregapi.block.multitileentity.MultiTileEntityBlock;
import gregapi.code.ModData;
import gregapi.compat.CompatMods;
import gregapi.data.TD;
import gregapi.tileentity.connectors.ITileEntityConnector;
import gregapi.util.UT;

import commoble.morered.api.MoreRedAPI;

/**
 * Tells More Red which faces of a GregTech 6 block its wires may bond to.
 * Its default connector accepts a face only if the face shape covers the wire node, which is true for
 * machines but never for a GT6 connector: a GT6 seam is decided by the connection mask in the tile
 * entity, not by shape, so redstone from the mod stopped at every GT6 wire.
 */
public class Compat_MoreRed extends CompatMods {
	public Compat_MoreRed(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}

	@Override public void onLoad(FMLInitializationEvent aEvent) {
		int tCount = 0;
		for (Block tBlock : BuiltInRegistries.BLOCK) if (tBlock instanceof MultiTileEntityBlock) {
			MoreRedAPI.getWireConnectabilityRegistry().put(tBlock, Compat_MoreRed::canConnect);
			tCount++;
		}
		OUT.println("GT_Mod: More Red wire connectability registered for " + tCount + " MultiTileEntity blocks.");
	}

	/** Answers the mod with what GT6 answers vanilla: a connector opens a seam, everything else keeps
	 *  the mod's own rule. */
	private static boolean canConnect(net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos,
		net.minecraft.world.level.block.state.BlockState aState, net.minecraft.core.BlockPos aWirePos,
		net.minecraft.world.level.block.state.BlockState aWireState, Direction aWireFace, Direction aToWire) {
		BlockEntity tTileEntity = aWorld.getBlockEntity(aPos);
		if (tTileEntity instanceof ITileEntityConnector tConnector) {
			byte tSide = UT.Code.side(aToWire);
			return tConnector.connected(tSide) && tConnector.getConnectorTypes(tSide).contains(TD.Connectors.WIRE_REDSTONE);
		}
		return MoreRedAPI.getDefaultWireConnector().canConnectToAdjacentWire(aWorld, aPos, aState, aWirePos, aWireState, aWireFace, aToWire);
	}
}
