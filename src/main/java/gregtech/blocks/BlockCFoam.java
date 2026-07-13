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

package gregtech.blocks;

import static gregapi.data.CS.*;

import gregapi.block.IBlockFoamable;
import gregapi.block.metatype.BlockColored;
import gregapi.block.metatype.BlockMetaType;
import gregapi.block.metatype.ItemBlockMetaType;
import gregapi.data.MT;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.BlockTextureCopied;
import gregapi.render.IIconContainer;
import gregapi.block.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;

public class BlockCFoam extends BlockColored implements IBlockFoamable {
	public BlockCFoam(String aUnlocalised) {
		super(ItemBlockMetaType.class, Material.rock, SoundType.STONE, aUnlocalised, "C-Foam", MT.ConstructionFoam, 4.0F, 1.5F, 1, Textures.BlockIcons.CFOAMS);
		MT.ConstructionFoam.mTextureSolid = BlockTextureCopied.get(this, SIDE_TOP, DYE_INDEX_LightGray);
	}
	
	@Override
	protected BlockMetaType makeSlab(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		return new BlockCFoam(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
	}
	
	protected BlockCFoam(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		super(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
	}
	
	@Override
	public boolean applyFoam(Level aWorld, int aX, int aY, int aZ, byte aSide, short[] aCFoamRGB, byte aVanillaColor) {
		return F;
	}
	
	@Override
	public boolean dryFoam(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		return F;
	}
	
	@Override
	public boolean removeFoam(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		return WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
	}
	
	@Override
	public boolean hasFoam(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		return T;
	}
	
	@Override
	public boolean driedFoam(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		return T;
	}
}
