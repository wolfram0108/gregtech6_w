/**
 * Copyright (c) 2026 GregTech-6 Team
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
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
 */

package gregapi.compat;

import gregapi.data.CS;
import gregapi.data.MD;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Tells foreign signal graphs that a GregTech block changed the redstone it emits.
 * Such graphs cache power per node and watch block states, while GregTech keeps its
 * redstone in the block entity, so without this notice they keep a stale value forever.
 * Spoken in vanilla terms (a game event id), so nothing here depends on the other mod.
 */
public final class SignalGraphBridge {
	private SignalGraphBridge() {}

	private static final ResourceKey<GameEvent> SIGNAL_GRAPH_UPDATE = ResourceKey.create(
		Registries.GAME_EVENT, Identifier.fromNamespaceAndPath(CS.ModIDs.EXM, "signal_graph_update"));

	private static Holder<GameEvent> sEvent = null;
	private static boolean sResolved = false;

	public static void notifyChanged(LevelAccessor aLevel, BlockPos aPos) {
		if (aLevel == null || aPos == null) return;
		if (!sResolved) {
			sEvent = MD.EXM.mLoaded
				? BuiltInRegistries.GAME_EVENT.get(SIGNAL_GRAPH_UPDATE.identifier()).map(h -> (Holder<GameEvent>) h).orElse(null)
				: null;
			sResolved = true;
			CS.OUT.println("[GT6-SIGNALGRAPH] мост обновления чужого графа: мод загружен = " + MD.EXM.mLoaded
				+ ", событие " + SIGNAL_GRAPH_UPDATE.identifier() + " = " + (sEvent == null ? "не найдено" : "найдено"));
		}
		if (sEvent != null) aLevel.gameEvent(sEvent, aPos, GameEvent.Context.of(null, null));
	}
}
