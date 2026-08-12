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

package gregapi.network;

import java.util.UUID;

import gregapi.network.NetworkHandler.GT6Payload;
import gregapi.network.NetworkHandler.TargetPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;

/**
 * @author Gregorius Techneticies
 */
public interface INetworkHandler {
	/** It sends a Packet from Client to Server. */
	public void sendToServer(IPacket aPacket);
	/** It sends a Packet to the Player, who is mentioned inside the Parameter. */
	public void sendToPlayer(IPacket aPacket, ServerPlayer aPlayer);
	/** It sends a Packet to all Players, who are in the specified Range. */
	public void sendToAllAround(IPacket aPacket, TargetPoint aPosition);
	/** It sends a Packet to all Players, who watch the Chunk on these X/Z Coordinates. */
	public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, int aX, int aZ);
	/** It sends a Packet to all Players, who watch the Chunk on these X/Z Coordinates. */
	public void sendToAllPlayersInRange(IPacket aPacket, Level aWorld, BlockPos aCoords);
	/** It sends a Packet to all Players, who watch the Chunk on these X/Z Coordinates. */
	public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, int aX, int aZ);
	/** It sends a Packet to all Players, who watch the Chunk on these X/Z Coordinates. */
	public void sendToPlayerIfInRange(IPacket aPacket, UUID aPlayer, Level aWorld, BlockPos aCoords);
	/** It sends a Packet to all Players, who watch the Chunk on these X/Z Coordinates. */
	public void sendToAllPlayersInRangeExcept(IPacket aPacket, UUID aPlayer, Level aWorld, int aX, int aZ);
	/** It sends a Packet to all Players, who watch the Chunk on these X/Z Coordinates. */
	public void sendToAllPlayersInRangeExcept(IPacket aPacket, UUID aPlayer, Level aWorld, BlockPos aCoords);

	/** For very advanced usage only! */
	public CustomPacketPayload.Type<GT6Payload> getChannel(Dist aSide);
}
