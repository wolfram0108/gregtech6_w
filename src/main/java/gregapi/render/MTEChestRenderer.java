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

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import gregapi.block.multitileentity.example.MultiTileEntityChest;
import gregapi.data.MD;
import gregapi.util.UT;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

import static gregapi.data.CS.*;

/** The chest's special renderer moved out of the common MultiTileEntityChest class, since client-only types
 *  embedded in a common class broke class linking on a dedicated server and aborted MTE registration entirely. */
public class MTEChestRenderer implements BlockEntityRenderer<MultiTileEntityChest> {

	private static MTEChestRenderer RENDERER;

	/** Bridge for the client-only onRegistrationFirstClient call, replacing ClientRegistry.bindTileEntitySpecialRenderer
	 *  with the unified GT6 BER's class dispatch. */
	public static void bindFirst(Class<?> aClass) {
		MultiTileEntityBER.bindSpecialRenderer(aClass, RENDERER = new MTEChestRenderer());
	}

	/** Bridge for onRegistrationClient: registers the .colored/.plain texture pair. */
	public static void bindTexture(String aTextureName, String aRegistryNameInternal) {
		RENDERER.mResources.put(aTextureName, new ResourceLocation[] {new ResourceLocation(MD.GT.mID, TEX_DIR_MODEL + aRegistryNameInternal + "/" + aTextureName + ".colored.png"), new ResourceLocation(MD.GT.mID, TEX_DIR_MODEL + aRegistryNameInternal + "/" + aTextureName + ".plain.png")});
	}

	private static final MultiTileEntityModelChest sModel = new MultiTileEntityModelChest();
	public final Map<String, ResourceLocation[]> mResources = new HashMap<>();

	/** On 1.20.1 the BER is single-parameter with no render-state object.
	 *  Values are gathered and drawn in one call, like 1.7.10's renderTileEntityAt. */
	@Override
	public void render(MultiTileEntityChest aChest, float aPartialTick, PoseStack aPoseStack, MultiBufferSource aBuffer, int aLight, int aOverlay) {
		ResourceLocation[] tLocation = mResources.get(aChest.mTextureName);
		if (tLocation == null || tLocation.length < 2) return;
		// Lid-angle interpolation and cubic easing curve, verbatim from 1.7.10's renderTileEntityAt.
		double tLidAngle = 1 - (aChest.oLidAngle + (aChest.mLidAngle - aChest.oLidAngle) * aPartialTick); tLidAngle = -(((1 - tLidAngle*tLidAngle*tLidAngle) * Math.PI) / 2);
		// For the item form, facing is already applied by the central applyItemFacing (value from getItemFacing).
		byte tFacing = aChest.mFacing;
		// Matrices are 1:1 with 1.7.10, since the model and textures were authored in its upside-down coordinate system.
		aPoseStack.pushPose();
		aPoseStack.translate(0, 1, 1);
		aPoseStack.scale(1, -1, -1);
		aPoseStack.translate(0.5f, 0.5f, 0.5f);
		aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(COMPASS_FROM_SIDE[tFacing] * 90 - 180));
		aPoseStack.translate(-0.5f, -0.5f, -0.5f);
		short[] tRGBa = UT.Code.getRGBaArray(aChest.mRGBa);
		// Pass 1 draws .colored.png tinted by mRGBa; pass 2 draws .plain.png untinted, as neo's entityCutout.
		sModel.render(aBuffer, aPoseStack, tLocation[0], (float)tLidAngle, aLight, aOverlay, tRGBa[0] / 255F, tRGBa[1] / 255F, tRGBa[2] / 255F);
		sModel.render(aBuffer, aPoseStack, tLocation[1], (float)tLidAngle, aLight, aOverlay, 1F, 1F, 1F);
		aPoseStack.popPose();
	}

	/** Chest model 1:1 (boxes/rotationPoints/texOffs literal from the 1.7.10 ModelBase version; ModelPart holds ModelRenderer). */
	public static class MultiTileEntityModelChest {
		private final net.minecraft.client.model.geom.ModelPart mRoot, mLid, mKnob;

		public MultiTileEntityModelChest() {
			net.minecraft.client.model.geom.builders.MeshDefinition tMesh = new net.minecraft.client.model.geom.builders.MeshDefinition();
			net.minecraft.client.model.geom.builders.PartDefinition tRoot = tMesh.getRoot();
			tRoot.addOrReplaceChild("lid",    net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox( 0, -5, -14, 14,  5, 14), net.minecraft.client.model.geom.PartPose.offset(1, 7, 15));
			tRoot.addOrReplaceChild("knob",   net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox(-1, -2, -15,  2,  4,  1), net.minecraft.client.model.geom.PartPose.offset(8, 7, 15));
			tRoot.addOrReplaceChild("bottom", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 19).addBox( 0,  0,   0, 14, 10, 14), net.minecraft.client.model.geom.PartPose.offset(1, 6, 1));
			mRoot = net.minecraft.client.model.geom.builders.LayerDefinition.create(tMesh, 64, 64).bakeRoot();
			mLid  = mRoot.getChild("lid");
			mKnob = mRoot.getChild("knob");
		}

		public void render(MultiBufferSource aBuffer, PoseStack aPoseStack, ResourceLocation aTexture, float aLidAngle, int aLight, int aOverlay, float aR, float aG, float aB) {
			// Lid angle is set right before drawing it; one static model serves every chest, and 1.20.1 draws immediately too
			// (the BER writes to the buffer here), so the old immediate-mode semantics carry over unchanged.
			mKnob.xRot = mLid.xRot = aLidAngle;
			mRoot.render(aPoseStack, aBuffer.getBuffer(RenderType.entityCutout(aTexture)), aLight, aOverlay, aR, aG, aB, 1F);
		}
	}
}
