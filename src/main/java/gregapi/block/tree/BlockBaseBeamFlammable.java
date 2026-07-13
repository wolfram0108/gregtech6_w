/**
 * Copyright (c) 2021 GregTech-6 Team
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

package gregapi.block.tree;
import net.minecraft.world.level.block.Block.SoundType;

import static gregapi.data.CS.*;

import gregapi.render.IIconContainer;
import gregapi.block.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBaseBeamFlammable extends BlockBaseBeam {
	public BlockBaseBeamFlammable(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType, Math.min(4, aMaxMeta), aIcons);
	}
	
	@Override public boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {return F;}
	@Override public int getFlammability(byte aMeta) {return 5;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 5;}
}
