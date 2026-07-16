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
 */

package gregapi.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * F3-render (client): «захватывающий рендерер» — объект {@code aRenderer}, который передаётся в GT6-цепочку
 * {@code ITexture.renderXPos(aRenderer,...) -> ITexture.Util.renderSide(side, Identifier, RGBa, ..., aRenderer, ...)}.
 * Вместо immediate-mode отрисовки (1.7.10 Tessellator, удалён) он АККУМУЛИРУЕТ per-side full-cube {@link BakedQuad}
 * для декларативной модели {@link GT6BlockModel}. Так GT6 per-side texture-логика (какая иконка/цвет на сторону —
 * решает сам ITexture/тайл) переиспользуется 1:1; переписан только «нарисуй сейчас» → «дай мне quad».
 * Спрайты резолвятся из атласа блоков в рантайме (текстуры GT6 динамические: материал×префикс, нельзя пре-bake).
 * Мост в {@link ITexture.Util}: если {@code aRenderer instanceof GT6QuadBuilder} → {@code putFace(side, icon, RGBa)}.
 * См. decisions/F3-render.md §2 (AE2 QuartzGlassModel/CubeBuilder-паттерн).
 */
@OnlyIn(Dist.CLIENT)
public final class GT6QuadBuilder {
	private final QuadCollection.Builder mQuads = new QuadCollection.Builder();
	private final List<BakedQuad> mAll = new ArrayList<>();
	/** Текущие render-bounds {minX,minY,minZ,maxX,maxY,maxZ} (1.7.10 RenderBlocks.setRenderBoundsFromBlock, обновляется per-pass). */
	private final float[] mBounds = {0, 0, 0, 1, 1, 1};
	private boolean mFullCube = true;

	/** F3-render: обновить текущие render-bounds перед проходом (GT6BlockModel читает {@code BlockBase.getRenderBounds()} после setBlockBounds). */
	public void setBounds(float[] aBounds) {
		if (aBounds == null || aBounds.length < 6) {System.arraycopy(new float[]{0,0,0,1,1,1}, 0, mBounds, 0, 6);}
		else System.arraycopy(aBounds, 0, mBounds, 0, 6);
		mFullCube = mBounds[0] <= 0 && mBounds[1] <= 0 && mBounds[2] <= 0 && mBounds[3] >= 1 && mBounds[4] >= 1 && mBounds[5] >= 1;
	}

	/** GT6 side-байт → neo Direction: SIDE_Y_NEG=0=DOWN, Y_POS=1=UP, Z_NEG=2=NORTH, Z_POS=3=SOUTH, X_NEG=4=WEST, X_POS=5=EAST. */
	public void putFace(byte aSide, Identifier aIcon, short[] aRGBa) {
		if (aIcon == null || aSide < 0 || aSide > 5) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		Direction tDir = Direction.from3DDataValue(aSide);
		BakedQuad tQuad = boundedFace(tDir, tSprite, aRGBa);
		if (tQuad == null) return;
		// full-cube грань — cull-aware (сосед скроет её); sub-cube (спайк/бар/провод) — всегда видима.
		if (mFullCube) mQuads.addCulledFace(tDir, tQuad); else mQuads.addUnculledFace(tQuad);
		mAll.add(tQuad);
	}

	public QuadCollection build() {return mQuads.build();}
	public List<BakedQuad> quads() {return mAll;}
	public boolean isEmpty() {return mAll.isEmpty();}

	private static TextureAtlasSprite sprite(Identifier aIcon) {
		try {return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(aIcon);} catch (Throwable e) {return null;}
	}

	/** Грань по текущим bounds (4 вершины) с UV из спрайта (клип по bounds) + tint из RGBa (0..255). AE2 QuartzGlassModel.createQuad/putVertex. */
	private BakedQuad boundedFace(Direction aDir, TextureAtlasSprite aSprite, short[] aRGBa) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		int a = aRGBa != null && aRGBa.length >= 4 ? (aRGBa[3] & 0xFF) : 255;
		float[][] c = corners(aDir, mBounds);
		net.minecraft.world.phys.Vec3 n = aDir.getUnitVec3();
		QuadBakingVertexConsumer tBuilder = new QuadBakingVertexConsumer();
		tBuilder.setSprite(new Material.Baked(aSprite, false));
		tBuilder.setDirection(aDir);
		for (int i = 0; i < 4; i++) {
			tBuilder.addVertex(c[i][0], c[i][1], c[i][2]);
			tBuilder.setColor(r, g, b, a);
			tBuilder.setNormal((float)n.x, (float)n.y, (float)n.z);
			tBuilder.setUv(aSprite.getU(c[i][3]), aSprite.getV(c[i][4]));
		}
		return tBuilder.bakeQuad();
	}

	/** 4 угла грани по bounds b={minX,minY,minZ,maxX,maxY,maxZ}, CCW относительно нормали; {x,y,z,u,v} (u,v в 0..16 → UV клипается по bounds).
	 *  При full-cube (0..1) сводится к прежнему поведению (u,v = 0..16). */
	private static float[][] corners(Direction aDir, float[] b) {
		float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
		float u0x = x0*16, u1x = x1*16, u0z = z0*16, u1z = z1*16, v0y = (1-y0)*16, v1y = (1-y1)*16;
		switch (aDir) {
		case DOWN:  return new float[][]{{x0,y0,z0, u0x,u0z},{x0,y0,z1, u0x,u1z},{x1,y0,z1, u1x,u1z},{x1,y0,z0, u1x,u0z}};
		case UP:    return new float[][]{{x0,y1,z1, u0x,u1z},{x0,y1,z0, u0x,u0z},{x1,y1,z0, u1x,u0z},{x1,y1,z1, u1x,u1z}};
		case NORTH: return new float[][]{{x1,y0,z0, u0x,v0y},{x1,y1,z0, u0x,v1y},{x0,y1,z0, u1x,v1y},{x0,y0,z0, u1x,v0y}};
		case SOUTH: return new float[][]{{x0,y0,z1, u0x,v0y},{x0,y1,z1, u0x,v1y},{x1,y1,z1, u1x,v1y},{x1,y0,z1, u1x,v0y}};
		case WEST:  return new float[][]{{x0,y0,z0, u0z,v0y},{x0,y1,z0, u0z,v1y},{x0,y1,z1, u1z,v1y},{x0,y0,z1, u1z,v0y}};
		case EAST:  return new float[][]{{x1,y0,z1, u0z,v0y},{x1,y1,z1, u0z,v1y},{x1,y1,z0, u1z,v1y},{x1,y0,z0, u1z,v0y}};
		default:    return new float[][]{{x0,y0,z0, u0x,u0z},{x0,y0,z1, u0x,u1z},{x1,y0,z1, u1x,u1z},{x1,y0,z0, u1x,u0z}};
		}
	}
}
