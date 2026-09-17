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

package gregapi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LevelRenderer;

import gregapi.render.MultiTileEntityBER;

/** Resets the BER quad cache on the same signal the engine uses to mark render sections dirty, giving the cache
 *  1.7.10's mesh-level granularity: stale rendering is possible only where 1.7.10's own mesh would be stale too. */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

	@Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
	private void gt6$invalidateQuadCaches(int aSectionX, int aSectionY, int aSectionZ, boolean aPlayerChanged, CallbackInfo aCI) {
		MultiTileEntityBER.onSectionDirty(aSectionX, aSectionY, aSectionZ);
	}

	@Inject(method = "allChanged()V", at = @At("HEAD"))
	private void gt6$invalidateAllQuadCaches(CallbackInfo aCI) {
		MultiTileEntityBER.onRenderAllChanged();
	}
}
