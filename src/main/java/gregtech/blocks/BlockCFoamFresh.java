/**
 * Copyright (c) 2023 GregTech-6 Team
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

import gregapi.block.IBlockFoamable;
import gregapi.block.metatype.BlockColored;
import gregapi.block.metatype.BlockMetaType;
import gregapi.block.metatype.ItemBlockMetaType;
import gregapi.data.CS.*;
import gregapi.data.MT;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.WD;
import gregapi.block.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Random;

import static gregapi.data.CS.*;

public class BlockCFoamFresh extends BlockColored implements IBlockFoamable {
	public BlockCFoamFresh(String aUnlocalised) {
		super(ItemBlockMetaType.class, Material.sponge, soundTypeCloth, aUnlocalised, "Fresh C-Foam", MT.ConstructionFoam, 1.0F, 0.0F, 0, Textures.BlockIcons.CFOAMS_FRESH);
	}
	
	@Override
	protected BlockMetaType makeSlab(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		return new BlockCFoamFresh(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
	}
	
	protected BlockCFoamFresh(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		super(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
	}
	
	@Override
	public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide()) aWorld.scheduleBlockUpdate(aX, aY, aZ, this, 100+RNGSUS.nextInt(5900));
	}
	
	@Override
	public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (!aWorld.isClientSide()) dryFoam(aWorld, aX, aY, aZ, SIDE_ANY);
	}
	
	@Override
	public ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) {
		return ST.arraylist();
	}
	
	@Override
	public boolean isOpaqueCube() {
		return F;
	}
	
	@Override
	public boolean isSideSolid(int aMeta, byte aSide) {
		return F;
	}
	
	@Override
	public boolean isSealable(byte aMeta, byte aSide) {
		return F;
	}
	
	@Override
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ) {
		return T;
	}
	
	@Override
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {
		return null;
	}
	
	public boolean isBlockSolid(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		return F;
	}
	
	@Override
	public boolean applyFoam(Level aWorld, int aX, int aY, int aZ, byte aSide, short[] aCFoamRGB, byte aVanillaColor) {
		return F;
	}
	
	@Override
	public boolean dryFoam(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		return WD.set(aWorld, aX, aY, aZ, SIDES_VALID[mSide]?((BlockMetaType)BlocksGT.CFoam).mSlabs[mSide]:BlocksGT.CFoam, WD.meta(aWorld, aX, aY, aZ), 3);
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
		return F;
	}
}
