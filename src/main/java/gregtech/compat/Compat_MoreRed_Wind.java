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

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;

import gregapi.data.MD;
import gregapi.data.TD;
import gregapi.tileentity.connectors.MultiTileEntityAxle;
import gregapi.util.UT;

/**
 * Feeds GregTech 6 axles from More Red windcatchers: the mod keeps the windmill, GT6 keeps the power.
 * A stack of windcatchers raises the power packets, not the speed — the speed is the wind level the
 * mod itself computed, capped at eight, and the packet count is the height of the stack.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = "gregtech")
public final class Compat_MoreRed_Wind {
	private Compat_MoreRed_Wind() {}

	private static final Identifier WINDCATCHER_SUFFIX_OWNER = Identifier.fromNamespaceAndPath("morered", "oak_windcatcher");
	private static final Set<BlockPos> WINDCATCHERS = new HashSet<>();
	private static boolean sActive = false;

	/** Chunks bring the windmills in and out; the set only ever holds what is loaded. */
	@net.neoforged.bus.api.SubscribeEvent
	public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load aEvent) {
		if (!MD.MR.mLoaded || !(aEvent.getLevel() instanceof ServerLevel) || !(aEvent.getChunk() instanceof LevelChunk tChunk)) return;
		for (BlockEntity tBE : tChunk.getBlockEntities().values()) if (isWindcatcher(tBE.getBlockState())) {
			WINDCATCHERS.add(tBE.getBlockPos().immutable());
			sActive = true;
		}
	}

	@net.neoforged.bus.api.SubscribeEvent
	public static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload aEvent) {
		if (!sActive || !(aEvent.getChunk() instanceof LevelChunk tChunk)) return;
		long tChunkKey = ChunkPos.pack(tChunk.getPos().x(), tChunk.getPos().z());
		WINDCATCHERS.removeIf(p -> ChunkPos.pack(p) == tChunkKey);
	}

	/** Either half of the pair may be placed first, so both cases register the windmill. */
	@net.neoforged.bus.api.SubscribeEvent
	public static void onPlace(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent aEvent) {
		if (!MD.MR.mLoaded) return;
		BlockPos tPos = aEvent.getPos().immutable();
		if (isWindcatcher(aEvent.getPlacedBlock())) {WINDCATCHERS.add(tPos); sActive = true; return;}
		if (!(aEvent.getLevel() instanceof ServerLevel tLevel)) return;
		for (Direction tDir : Direction.values()) {
			BlockPos tSide = tPos.relative(tDir);
			if (isWindcatcher(tLevel.getBlockState(tSide))) {WINDCATCHERS.add(tSide.immutable()); sActive = true;}
		}
	}

	/** Every tick each stack of windmills pushes its rotation into whatever GT6 axle touches it. */
	@net.neoforged.bus.api.SubscribeEvent
	public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post aEvent) {
		if (!sActive) return;
		for (net.minecraft.server.level.ServerLevel tLevel : aEvent.getServer().getAllLevels()) {
			for (BlockPos tPos : WINDCATCHERS.toArray(new BlockPos[0])) {
				if (!tLevel.isLoaded(tPos)) continue;
				BlockState tState = tLevel.getBlockState(tPos);
				if (!isWindcatcher(tState)) {WINDCATCHERS.remove(tPos); continue;}
				// Only the bottom of a stack drives the axle: the ones above it add power, not speed.
				if (isWindcatcher(tLevel.getBlockState(tPos.below()))) continue;
				long tSpeed = wind(tState);
				if (tSpeed <= 0) continue;
				long tPower = 1;
				for (BlockPos tUp = tPos.above(); isWindcatcher(tLevel.getBlockState(tUp)); tUp = tUp.above()) tPower++;
				drive(tLevel, tPos, tSpeed, tPower);
			}
		}
	}

	private static void drive(ServerLevel aLevel, BlockPos aBottom, long aSpeed, long aPower) {
		for (BlockPos tPos = aBottom; isWindcatcher(aLevel.getBlockState(tPos)); tPos = tPos.above()) {
			for (Direction tDir : Direction.values()) {
				BlockEntity tBE = aLevel.getBlockEntity(tPos.relative(tDir));
				if (!(tBE instanceof MultiTileEntityAxle tAxle)) continue;
				byte tSide = UT.Code.side(tDir.getOpposite());
				if (tAxle.isEnergyAcceptingFrom(TD.Energy.RU, tSide, F)) {
					tAxle.doEnergyInjection(TD.Energy.RU, tSide, aSpeed, aPower, T);
					return;
				}
			}
		}
	}

	private static boolean isWindcatcher(BlockState aState) {
		Identifier tKey = BuiltInRegistries.BLOCK.getKey(aState.getBlock());
		return tKey != null && WINDCATCHER_SUFFIX_OWNER.getNamespace().equals(tKey.getNamespace())
			&& tKey.getPath().endsWith("windcatcher");
	}

	/** The wind level is the mod's own reading of height and open sky; GT6 takes it as the speed. */
	private static long wind(BlockState aState) {
		for (Property<?> tProperty : aState.getProperties()) {
			if ("wind".equals(tProperty.getName()) && tProperty instanceof IntegerProperty tInt) return aState.getValue(tInt);
		}
		return 0;
	}
}
