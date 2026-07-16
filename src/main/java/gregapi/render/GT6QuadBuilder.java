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

	/** GT6 side-байт → neo Direction: SIDE_Y_NEG=0=DOWN, Y_POS=1=UP, Z_NEG=2=NORTH, Z_POS=3=SOUTH, X_NEG=4=WEST, X_POS=5=EAST. */
	public void putFace(byte aSide, Identifier aIcon, short[] aRGBa) {
		if (aIcon == null || aSide < 0 || aSide > 5) return;
		TextureAtlasSprite tSprite = sprite(aIcon);
		if (tSprite == null) return;
		Direction tDir = Direction.from3DDataValue(aSide);
		BakedQuad tQuad = fullCubeFace(tDir, tSprite, aRGBa);
		if (tQuad != null) {mQuads.addCulledFace(tDir, tQuad); mAll.add(tQuad);}
	}

	public QuadCollection build() {return mQuads.build();}
	public List<BakedQuad> quads() {return mAll;}
	public boolean isEmpty() {return mAll.isEmpty();}

	private static TextureAtlasSprite sprite(Identifier aIcon) {
		try {return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(aIcon);} catch (Throwable e) {return null;}
	}

	/** Полная грань куба (4 вершины 0..1) с UV из спрайта + tint из RGBa (0..255). AE2 QuartzGlassModel.createQuad/putVertex. */
	private static BakedQuad fullCubeFace(Direction aDir, TextureAtlasSprite aSprite, short[] aRGBa) {
		int r = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[0] & 0xFF) : 255;
		int g = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[1] & 0xFF) : 255;
		int b = aRGBa != null && aRGBa.length >= 3 ? (aRGBa[2] & 0xFF) : 255;
		int a = aRGBa != null && aRGBa.length >= 4 ? (aRGBa[3] & 0xFF) : 255;
		float[][] c = corners(aDir);
		net.minecraft.world.phys.Vec3 n = aDir.getUnitVec3();
		QuadBakingVertexConsumer tBuilder = new QuadBakingVertexConsumer();
		tBuilder.setSprite(new Material.Baked(aSprite, false));
		tBuilder.setDirection(aDir);
		float[] u = {0, 0, 16, 16}, v = {0, 16, 16, 0};
		for (int i = 0; i < 4; i++) {
			tBuilder.addVertex(c[i][0], c[i][1], c[i][2]);
			tBuilder.setColor(r, g, b, a);
			tBuilder.setNormal((float)n.x, (float)n.y, (float)n.z);
			tBuilder.setUv(aSprite.getU(u[i] / 16f), aSprite.getV(v[i] / 16f));
		}
		return tBuilder.bakeQuad();
	}

	/** 4 угла полной грани куба (единичный блок 0..1), CCW относительно нормали. */
	private static float[][] corners(Direction aDir) {
		switch (aDir) {
		case DOWN:  return new float[][]{{0,0,0},{0,0,1},{1,0,1},{1,0,0}};
		case UP:    return new float[][]{{0,1,1},{0,1,0},{1,1,0},{1,1,1}};
		case NORTH: return new float[][]{{1,0,0},{1,1,0},{0,1,0},{0,0,0}};
		case SOUTH: return new float[][]{{0,0,1},{0,1,1},{1,1,1},{1,0,1}};
		case WEST:  return new float[][]{{0,0,0},{0,1,0},{0,1,1},{0,0,1}};
		case EAST:  return new float[][]{{1,0,1},{1,1,1},{1,1,0},{1,0,0}};
		default:    return new float[][]{{0,0,0},{0,0,1},{1,0,1},{1,0,0}};
		}
	}
}
