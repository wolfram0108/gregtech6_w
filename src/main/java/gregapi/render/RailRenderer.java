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

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * F3-render рельсов (client): воспроизведение vanilla {@code RenderBlocks.renderBlockRail} — плоский рельс-quad на
 * y=1/16 с иконкой рельса (straight/turned/active выбирает сам {@link gregapi.block.misc.BlockBaseRail#getIcon} по
 * мете), ориентация/наклон/угол — по мете. BUG-047: мета жива — мост IBlockExtendedMetaData рельса (мета ↔
 * SHAPE+POWERED в BlockState, выравнивание vanilla updateDir), {@code WD.meta} здесь читает реальную форму.
 * Рендер процедурный — тем же приёмом, что вся F3-render ветка (см. {@link GT6BlockModel}). BlockBaseRail наследует
 * vanilla {@code BaseRailBlock} и НЕ является IRenderedBlock, потому обрабатывается отдельной веткой, а не box-цепочкой.
 * <p>Мета рельса (1:1 vanilla): 0=плоский N-S, 1=плоский E-W, 2/3=подъём восток/запад (E-W трек), 4/5=подъём
 * север/юг (N-S трек), 6..9=углы SE/SW/NW/NE (turned-иконка). Power/detector-рельсы углов не имеют (биты 0-2 —
 * направление, бит 3 — питание; getIcon по биту питания даёт active-иконку).
 */
public final class RailRenderer {
	private RailRenderer() {}

	private static final float Y = 1.0F / 16.0F; // рельс на 1px над низом блока (vanilla 0.0625)

	/** Собрать quad'ы рельса в позиции (x,y,z) по его текущей мете. */
	public static void collectRailQuads(GT6QuadBuilder aQB, BlockAndTintGetter aLevel, int aX, int aY, int aZ, gregapi.block.misc.BlockBaseRail aRail) {
		int tMeta = gregapi.util.WD.meta(aLevel, aX, aY, aZ) & 0xFF;
		ResourceLocation tIcon = aRail.getIcon(0, tMeta); // primary/secondary (straight/turned либо active) выбирает сам блок
		if (tIcon == null) return;
		// Форма: power/detector — только направление (биты 0-2, углов нет); обычный рельс — мета 0..9.
		int tShape = (aRail.mPowerRail || aRail.mDetectorRail) ? (tMeta & 7) : (tMeta & 15);
		boolean tCorner = tShape >= 6 && tShape <= 9;
		boolean tEW = (tShape == 1 || tShape == 2 || tShape == 3); // E-W ориентация трека
		float[][] c = tCorner ? cornerQuad(tShape - 6) : flatQuad(tEW);
		if (tShape >= 2 && tShape <= 5) raise(c, tShape); // наклонные — поднять два угла со стороны подъёма
		// bothSides: рельс виден и сверху, и снизу (vanilla рисует up+down грань) — 1:1.
		aQB.fluidQuad(c, Direction.UP, tIcon, gregapi.data.CS.UNCOLOURED, true);
	}

	/** Плоский горизонтальный quad рельса; углы 0=(x0,z0) 1=(x1,z0) 2=(x1,z1) 3=(x0,z1). UV: для N-S трека V идёт вдоль
	 *  Z (рельсы север-юг), для E-W — V вдоль X (поворот текстуры на 90°). {x,y,z,u,v}, u,v в 0..16. */
	private static float[][] flatQuad(boolean aEW) {
		if (aEW) return new float[][]{{0,Y,0, 0,0},{1,Y,0, 0,16},{1,Y,1, 16,16},{0,Y,1, 16,0}};
		return new float[][]{{0,Y,0, 0,0},{1,Y,0, 16,0},{1,Y,1, 16,16},{0,Y,1, 0,16}};
	}

	/** Угловой quad (turned-иконка): базовая позиция + UV, повёрнутый на aRot*90° (1:1 vanilla поворот угла рельса). */
	private static float[][] cornerQuad(int aRot) {
		float[][] uv  = {{0,0},{16,0},{16,16},{0,16}};
		float[][] pos = {{0,Y,0},{1,Y,0},{1,Y,1},{0,Y,1}};
		float[][] c = new float[4][5];
		for (int i = 0; i < 4; i++) {
			c[i][0] = pos[i][0]; c[i][1] = pos[i][1]; c[i][2] = pos[i][2];
			float[] u = uv[(i + aRot) & 3];
			c[i][3] = u[0]; c[i][4] = u[1];
		}
		return c;
	}

	/** Наклон (ascending 2-5): поднять два угла со стороны подъёма на +1 по Y (низкий край на уровне рельса, высокий на +1). */
	private static void raise(float[][] c, int aShape) {
		int[] hi;
		switch (aShape) {
		case 2:  hi = new int[]{1,2}; break; // подъём на восток (x=1)
		case 3:  hi = new int[]{0,3}; break; // подъём на запад (x=0)
		case 4:  hi = new int[]{0,1}; break; // подъём на север (z=0)
		default: hi = new int[]{2,3}; break; // 5 — подъём на юг (z=1)
		}
		for (int i : hi) c[i][1] += 1.0F;
	}
}
