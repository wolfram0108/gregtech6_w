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

package gregapi.render;

import com.mojang.blaze3d.vertex.PoseStack;

import gregapi.tileentity.inventories.MultiTileEntityMassStorage;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;

import static gregapi.data.CS.*;

/** Mass-storage's special renderer is pulled out for the same reason and technique as {@link MTEChestRenderer}. */
public class MTEMassStorageRenderer implements BlockEntityRenderer<MultiTileEntityMassStorage> {
	public static MTEMassStorageRenderer INSTANCE = new MTEMassStorageRenderer();

	/** Bridge for the client-only onRegistrationFirstClient call. */
	public static void bindFirst(Class<?> aClass) {
		MultiTileEntityBER.bindSpecialRenderer(aClass, INSTANCE);
	}

	/** On 1.20.1 the BER is single-parameter with no render-state object: gather values and draw in one call.
	 *  Contents render via the standard ItemRenderer.renderStatic in GUI context, i.e. the inventory-icon form. */
	@Override
	public void render(MultiTileEntityMassStorage aStorage, float aPartialTick, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		if (!aStorage.slotHas(1) || !aStorage.isFaceVisible()) return;
		// For the item form, facing is already applied by applyItemFacing; the detached TE gets a tunable display-item facing.
		byte tFacing = aStorage.mFacing;
		// 1.7.10's GUI item render draws from a corner, so its translate point was offset to center that old visual.
		// The engine's item model already renders centered, so this translates straight to the item's center instead.
		aPoseStack.pushPose();
		aPoseStack.translate(0.5 + OFFX[tFacing]*0.502, 0.375, 0.5 + OFFZ[tFacing]*0.502);
		// 1.7.10's rotZ(180) undid the old y-down GUI render; the engine's model is y-up, so a literal port flips it upside down.
		// The GUI context always faces the model to +Z, so the needed yaw is -toYRot() per direction, not one fixed angle.
		net.minecraft.core.Direction tDir = net.minecraft.core.Direction.from3DDataValue(tFacing);
		if (tDir.getAxis().isHorizontal()) {
			aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-tDir.toYRot()));
		} else {
			aPoseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90 * tDir.getAxisDirection().getStep()));
		}
		// The missing mirror half comes back as a Y-axis 180 rotation, not a negative scale: rotation keeps face culling
		// and chirality intact, and the block model needs that same 180 turn by the player's own measurement.
		if (MASSSTORAGE_DISPLAY_YAW != 0) aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(MASSSTORAGE_DISPLAY_YAW));
		aPoseStack.scale(0.5f, 0.5f, 0.0001f);
		net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(aStorage.slot(1), net.minecraft.world.item.ItemDisplayContext.GUI,
			0xF000F0 /* fullbright 240/240, matching the original setLightmapTextureCoords. */, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
			aPoseStack, aBuffer, aStorage.getLevel(), 0);
		aPoseStack.popPose();
	}
}
