/**
 * Copyright (c) 2026 wolfram0108
 *
 * COMPILE-TIME STAND-IN — NOT THIRD-PARTY CODE.
 *
 * This declaration was written from scratch for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). It contains no code from the project
 * that owns this package name, and no part of it was copied or decompiled from that
 * project: it declares only the members GregTech 6 itself implements or calls, so that
 * the port compiles while integration with that mod stays deferred.
 *
 * The original package name is kept deliberately, because GregTech 6 implements these
 * types verbatim and the port does not alter the code Gregorius Techneticies wrote.
 * Removing these classes from the build is not possible: 66 classes of the mod extend
 * or implement them, and the JVM requires the type to load the implementing class.
 *
 * All names, trademarks and rights in the project this package belongs to remain with
 * its authors. See src/compat-mirror/README.md and NOTICE.
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

package thaumcraft.common.tiles;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** F10 ЗЕРКАЛО (compile-only) чужого API Thaumcraft. Только объявления, используемые GregTech6
 *  (ContainerArcaneWorkbenchFixed — @Deprecated путь, не исполняется без реального TC). Реализует
 *  {@link Container} — ContainerArcaneWorkbenchFixed.onCraftMatrixChanged(mTileEntity) требует эту
 *  совместимость (реальный TileArcaneWorkbench у Thaumcraft — тоже инвентарь). См. compat-mirror/README.md. */
public class TileArcaneWorkbench implements Container {
	public ItemStack[] stackList = new ItemStack[0];

	public Level getLevel() {return null;}
	public ItemStack getStackInSlot(int aSlot) {return ItemStack.EMPTY;}
	public void setInventorySlotContentsSoftly(int aSlot, ItemStack aStack) {}

	@Override public int getContainerSize() {return 0;}
	@Override public boolean isEmpty() {return true;}
	@Override public ItemStack getItem(int aSlot) {return ItemStack.EMPTY;}
	@Override public ItemStack removeItem(int aSlot, int aAmount) {return ItemStack.EMPTY;}
	@Override public ItemStack removeItemNoUpdate(int aSlot) {return ItemStack.EMPTY;}
	@Override public void setItem(int aSlot, ItemStack aStack) {}
	@Override public void setChanged() {}
	@Override public boolean stillValid(Player aPlayer) {return false;}
	@Override public void clearContent() {}
}
