/**
 * Copyright (c) 2020 GregTech-6 Team
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

package gregapi.block.multitileentity;

import static gregapi.data.CS.*;

import gregapi.oredict.OreDictMaterial;
import gregapi.util.UT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityClassContainer {
	public final Class<? extends BlockEntity> mClass;
	public final MultiTileEntityBlock mBlock;
	public final BlockEntity mCanonicalTileEntity;
	public final CompoundTag mParameters;
	public final byte mBlockMetaData, mStackSize;
	public final short mID, mCreativeTabID;
	public final boolean mHidden;
	
	public MultiTileEntityClassContainer(int aID, int aCreativeTabID, Class<? extends BlockEntity> aClass, int aBlockMetaData, int aStackSize, MultiTileEntityBlock aBlock, CompoundTag aParameters) {
		if (!IMultiTileEntity.class.isAssignableFrom(aClass)) throw new IllegalArgumentException("MultiTileEntities must implement the Interface IMultiTileEntity!");
		mBlockMetaData = (byte)aBlockMetaData;
		mStackSize = (byte)aStackSize;
		mParameters = aParameters==null?UT.NBT.make():aParameters;
		mHidden = mParameters.getBoolean(NBT_HIDDEN);
		mID = (short)aID;
		mCreativeTabID = (short)aCreativeTabID;
		mBlock = aBlock;
		mClass = aClass;
		if (mParameters.contains(NBT_MATERIAL) && !mParameters.contains(NBT_COLOR)) mParameters.putInt(NBT_COLOR, UT.Code.getRGBInt(OreDictMaterial.get(mParameters.getString(NBT_MATERIAL)).fRGBaSolid));
		try {mCanonicalTileEntity = aClass.newInstance();} catch (Throwable e) {throw new IllegalArgumentException(e);}
		if (mCanonicalTileEntity instanceof IMultiTileEntity) ((IMultiTileEntity)mCanonicalTileEntity).initFromNBT(mParameters, mID, (short)-1);
	}
}
