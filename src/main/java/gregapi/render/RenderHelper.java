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

package gregapi.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static gregapi.data.CS.*;

/**
 * F3-render: 1.7.10 immediate-mode GUI/overlay-помощники. Инвентарный рендер предмета в neo — через baked item-модель
 * ({@link gregapi.render.GT6ItemModel}) + {@code GuiGraphics.renderItem} на call-site; {@code renderItemIntoGUI} здесь мёртв
 * (0 вызывателей, суперседирован). {@code mRenderBlocks} — нейтральный held-объект ("передать дальше"). Единственное живое —
 * {@code drawWrenchOverlay} (косметический overlay соединений труб/проводов при наведении с гаечным ключом).
 */
public class RenderHelper {
	/** F3-render: было {@code RenderBlocks} (тип удалён); нейтральный held-объект, тело не трогается. */
	public static Object mRenderBlocks = null;

	/** F3-render: было immediate-mode RenderItem/GL11; в neo инвентарь-рендер — baked GT6ItemModel + GuiGraphics.renderItem. Мёртв (0 вызывателей). */
	public static void renderItemIntoGUI(Font aFontRenderer, TextureManager aTextureManager, ItemStack aStack, int aX, int aY, boolean aEffect) {
		//
	}

	/** F3-render, ЖИВОЙ wrench-overlay (сетка зон клика инструментом по грани). 1.7.10: сырой GL11 в кадре
	 *  DrawBlockHighlightEvent — translate в центр блока + CCL {@code Rotation.sideRotations[aSide]} + рисование
	 *  в локальной плоскости y=-0.5025 (RenderHelper:98-449 оригинала).
	 *  <p>Ветка 1.20.1: носитель — {@code RenderHighlightEvent.Block} ({@code RenderHighlightEvent.java:107}), и он
	 *  НЕМЕДЛЕННЫЙ: даёт готовые {@code PoseStack} и {@code MultiBufferSource} прямо в кадре отрисовки выделения —
	 *  то есть та же семантика, что у 1.7.10 (нарисовал тут же), без отложенного {@code CustomBlockOutlineRenderer}
	 *  26.x. Линии — канон {@code LevelRenderer.renderShape} ({@code RenderType.lines()}, vertex+color+normal).
	 *  Базисы граней — 1:1 из байткода CCL Rotation.sideRotations (Matrix4-инициализаторы); геометрия
	 *  (112 отрезков) — дословная транскрипция glVertex3d-пар оригинала. Дыхание цвета — CLIENT_TIME, как было. */
	private static final double[][][] WRENCH_SIDE_BASIS = { // [aSide][локальная ось X/Y/Z][мировой вектор x,y,z]
		{{ 1, 0, 0}, { 0, 1, 0}, { 0, 0, 1}}, // DOWN: identity
		{{ 1, 0, 0}, { 0,-1, 0}, { 0, 0,-1}}, // UP: diag(1,-1,-1)
		{{ 1, 0, 0}, { 0, 0, 1}, { 0,-1, 0}}, // NORTH: world=(lx,-lz,ly)
		{{ 1, 0, 0}, { 0, 0,-1}, { 0, 1, 0}}, // SOUTH: world=(lx,lz,-ly)
		{{ 0,-1, 0}, { 1, 0, 0}, { 0, 0, 1}}, // WEST: world=(ly,-lx,lz)
		{{ 0, 1, 0}, {-1, 0, 0}, { 0, 0, 1}}, // EAST: world=(-ly,lx,lz)
	};
	private interface WrenchLine {void line(double aX1, double aZ1, double aX2, double aZ2);}

	public static void drawWrenchOverlay(net.minecraftforge.client.event.RenderHighlightEvent.Block aEvent, byte aConnections, byte aSide) {
		if (gregapi.data.CS.SIDES_INVALID[aSide]) return;
		net.minecraft.core.BlockPos tPos = aEvent.getTarget().getBlockPos();
		try {
			drawWrenchOverlayNow(aEvent.getMultiBufferSource(), aEvent.getPoseStack(), aEvent.getCamera().getPosition(),
				tPos.getX(), tPos.getY(), tPos.getZ(), aConnections & 255, aSide);
		} catch (Throwable e) {/* косметика не смеет ронять кадр */}
	}

	private static void drawWrenchOverlayNow(net.minecraft.client.renderer.MultiBufferSource aBuffer, com.mojang.blaze3d.vertex.PoseStack aPose,
			net.minecraft.world.phys.Vec3 tCam, int aX, int aY, int aZ, int aConnections, byte aSide) {
		final double tCX = aX + 0.5 - tCam.x, tCY = aY + 0.5 - tCam.y, tCZ = aZ + 0.5 - tCam.z;
		final double[][] tM = WRENCH_SIDE_BASIS[aSide]; final double tLY = -0.5025; // локальная плоскость чуть снаружи грани, как glTranslated(0,-0.5025,0)
		double tColorD = (CLIENT_TIME % 42 < 21 ? 0.25 + ((CLIENT_TIME % 21) / 40.0) : 0.75 - ((CLIENT_TIME % 21) / 40.0));
		int tGray = (int)(tColorD * 255.0);
		final int tColor = net.minecraft.util.FastColor.ARGB32.color(127, tGray, tGray, tGray); // glColor4d(t,t,t,0.5)
		final com.mojang.blaze3d.vertex.VertexConsumer tVC = aBuffer.getBuffer(net.minecraft.client.renderer.RenderType.lines());
		final com.mojang.blaze3d.vertex.PoseStack.Pose tPose = aPose.last();
		WrenchLine tLine = (aX1, aZ1, aX2, aZ2) -> {
			double x1 = tCX + tM[0][0]*aX1 + tM[1][0]*tLY + tM[2][0]*aZ1, y1 = tCY + tM[0][1]*aX1 + tM[1][1]*tLY + tM[2][1]*aZ1, z1 = tCZ + tM[0][2]*aX1 + tM[1][2]*tLY + tM[2][2]*aZ1;
			double x2 = tCX + tM[0][0]*aX2 + tM[1][0]*tLY + tM[2][0]*aZ2, y2 = tCY + tM[0][1]*aX2 + tM[1][1]*tLY + tM[2][1]*aZ2, z2 = tCZ + tM[0][2]*aX2 + tM[1][2]*tLY + tM[2][2]*aZ2;
			org.joml.Vector3f tNormal = new org.joml.Vector3f((float)(x2-x1), (float)(y2-y1), (float)(z2-z1)).normalize();
			tVC.vertex(tPose.pose(), (float)x1, (float)y1, (float)z1).color(tColor).normal(tPose.normal(), tNormal.x(), tNormal.y(), tNormal.z()).endVertex();
			tVC.vertex(tPose.pose(), (float)x2, (float)y2, (float)z2).color(tColor).normal(tPose.normal(), tNormal.x(), tNormal.y(), tNormal.z()).endVertex();
		};
		// ===== дословная транскрипция вершин оригинала (пары glVertex3d → отрезки), локальные (x,z) =====
		tLine.line(0.50, -0.25, -0.50, -0.25);
		tLine.line(0.50, 0.25, -0.50, 0.25);
		tLine.line(0.25, -0.50, 0.25, 0.50);
		tLine.line(-0.25, -0.50, -0.25, 0.50);
		switch(aSide) {
		case SIDE_DOWN:
			if (FACE_CONNECTED[SIDE_DOWN][aConnections]) {
				tLine.line(-0.25, -0.25, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_UP][aConnections]) {
				tLine.line(-0.50, -0.50, -0.25, -0.25);
				tLine.line(-0.50, -0.25, -0.25, -0.50);
				tLine.line(+0.50, +0.50, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, +0.50);
				tLine.line(+0.50, -0.50, +0.25, -0.25);
				tLine.line(+0.50, -0.25, +0.25, -0.50);
				tLine.line(-0.50, +0.50, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_NORTH][aConnections]) {
				tLine.line(-0.25, -0.50, +0.25, -0.25);
				tLine.line(-0.25, -0.25, +0.25, -0.50);
			}
			if (FACE_CONNECTED[SIDE_SOUTH][aConnections]) {
				tLine.line(-0.25, +0.50, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_WEST][aConnections]) {
				tLine.line(-0.50, -0.25, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_EAST][aConnections]) {
				tLine.line(+0.50, -0.25, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, -0.25);
			}
			break;
		case SIDE_UP:
			if (FACE_CONNECTED[SIDE_DOWN][aConnections]) {
				tLine.line(-0.50, -0.50, -0.25, -0.25);
				tLine.line(-0.50, -0.25, -0.25, -0.50);
				tLine.line(+0.50, +0.50, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, +0.50);
				tLine.line(+0.50, -0.50, +0.25, -0.25);
				tLine.line(+0.50, -0.25, +0.25, -0.50);
				tLine.line(-0.50, +0.50, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_UP][aConnections]) {
				tLine.line(-0.25, -0.25, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_NORTH][aConnections]) {
				tLine.line(-0.25, +0.50, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_SOUTH][aConnections]) {
				tLine.line(-0.25, -0.50, +0.25, -0.25);
				tLine.line(-0.25, -0.25, +0.25, -0.50);
			}
			if (FACE_CONNECTED[SIDE_WEST][aConnections]) {
				tLine.line(-0.50, -0.25, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_EAST][aConnections]) {
				tLine.line(+0.50, -0.25, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, -0.25);
			}
			break;
		case SIDE_NORTH:
			if (FACE_CONNECTED[SIDE_DOWN][aConnections]) {
				tLine.line(-0.25, +0.50, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_UP][aConnections]) {
				tLine.line(-0.25, -0.50, +0.25, -0.25);
				tLine.line(-0.25, -0.25, +0.25, -0.50);
			}
			if (FACE_CONNECTED[SIDE_NORTH][aConnections]) {
				tLine.line(-0.25, -0.25, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_SOUTH][aConnections]) {
				tLine.line(-0.50, -0.50, -0.25, -0.25);
				tLine.line(-0.50, -0.25, -0.25, -0.50);
				tLine.line(+0.50, +0.50, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, +0.50);
				tLine.line(+0.50, -0.50, +0.25, -0.25);
				tLine.line(+0.50, -0.25, +0.25, -0.50);
				tLine.line(-0.50, +0.50, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_WEST][aConnections]) {
				tLine.line(-0.50, -0.25, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_EAST][aConnections]) {
				tLine.line(+0.50, -0.25, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, -0.25);
			}
			break;
		case SIDE_SOUTH:
			if (FACE_CONNECTED[SIDE_DOWN][aConnections]) {
				tLine.line(-0.25, -0.50, +0.25, -0.25);
				tLine.line(-0.25, -0.25, +0.25, -0.50);
			}
			if (FACE_CONNECTED[SIDE_UP][aConnections]) {
				tLine.line(-0.25, +0.50, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_NORTH][aConnections]) {
				tLine.line(-0.50, -0.50, -0.25, -0.25);
				tLine.line(-0.50, -0.25, -0.25, -0.50);
				tLine.line(+0.50, +0.50, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, +0.50);
				tLine.line(+0.50, -0.50, +0.25, -0.25);
				tLine.line(+0.50, -0.25, +0.25, -0.50);
				tLine.line(-0.50, +0.50, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_SOUTH][aConnections]) {
				tLine.line(-0.25, -0.25, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_WEST][aConnections]) {
				tLine.line(-0.50, -0.25, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_EAST][aConnections]) {
				tLine.line(+0.50, -0.25, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, -0.25);
			}
			break;
		case SIDE_WEST:
			if (FACE_CONNECTED[SIDE_DOWN][aConnections]) {
				tLine.line(+0.50, -0.25, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_UP][aConnections]) {
				tLine.line(-0.50, -0.25, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_NORTH][aConnections]) {
				tLine.line(-0.25, -0.50, +0.25, -0.25);
				tLine.line(-0.25, -0.25, +0.25, -0.50);
			}
			if (FACE_CONNECTED[SIDE_SOUTH][aConnections]) {
				tLine.line(-0.25, +0.50, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_WEST][aConnections]) {
				tLine.line(-0.25, -0.25, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_EAST][aConnections]) {
				tLine.line(-0.50, -0.50, -0.25, -0.25);
				tLine.line(-0.50, -0.25, -0.25, -0.50);
				tLine.line(+0.50, +0.50, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, +0.50);
				tLine.line(+0.50, -0.50, +0.25, -0.25);
				tLine.line(+0.50, -0.25, +0.25, -0.50);
				tLine.line(-0.50, +0.50, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, +0.50);
			}
			break;
		case SIDE_EAST:
			if (FACE_CONNECTED[SIDE_DOWN][aConnections]) {
				tLine.line(-0.50, -0.25, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_UP][aConnections]) {
				tLine.line(+0.50, -0.25, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, -0.25);
			}
			if (FACE_CONNECTED[SIDE_NORTH][aConnections]) {
				tLine.line(-0.25, -0.50, +0.25, -0.25);
				tLine.line(-0.25, -0.25, +0.25, -0.50);
			}
			if (FACE_CONNECTED[SIDE_SOUTH][aConnections]) {
				tLine.line(-0.25, +0.50, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_WEST][aConnections]) {
				tLine.line(-0.50, -0.50, -0.25, -0.25);
				tLine.line(-0.50, -0.25, -0.25, -0.50);
				tLine.line(+0.50, +0.50, +0.25, +0.25);
				tLine.line(+0.50, +0.25, +0.25, +0.50);
				tLine.line(+0.50, -0.50, +0.25, -0.25);
				tLine.line(+0.50, -0.25, +0.25, -0.50);
				tLine.line(-0.50, +0.50, -0.25, +0.25);
				tLine.line(-0.50, +0.25, -0.25, +0.50);
			}
			if (FACE_CONNECTED[SIDE_EAST][aConnections]) {
				tLine.line(-0.25, -0.25, +0.25, +0.25);
				tLine.line(-0.25, +0.25, +0.25, -0.25);
			}
			break;
		}
	}
}
