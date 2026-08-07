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

package gregapi.compat.industrialcraft;

import gregapi.compat.ICompat;
import net.minecraft.world.item.ItemStack;

public interface ICompatIC2EUItem extends ICompat {
	public long charge(ItemStack aStack, long aAmount, boolean aDoCharge);
	public long charge(ItemStack aStack, long aAmount, int aTier, boolean aIgnoreLimit, boolean aDoCharge);
	public long decharge(ItemStack aStack, long aAmount, boolean aDoDecharge);
	public long decharge(ItemStack aStack, long aAmount, int aTier, boolean aIgnoreLimit, boolean aDoDecharge, boolean aIgnoreDischargability);
	public byte tier(ItemStack aStack);
	public boolean insidevolt(ItemStack aStack, long aMinVolt, long aMaxVolt);
	public boolean provider(ItemStack aStack);
	public boolean is(ItemStack aStack);
	public boolean is(ItemStack aStack, byte aTier);
	public long stored(ItemStack aStack);
	public long capacity(ItemStack aStack);
}
