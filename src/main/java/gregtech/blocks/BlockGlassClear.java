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

import net.minecraft.world.level.block.SoundType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import gregapi.block.metatype.BlockColored;
import gregapi.block.metatype.BlockMetaType;
import gregapi.block.metatype.ItemBlockMetaType;
import gregapi.data.CS.*;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.old.Textures;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.IIconContainer;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

import static gregapi.data.CS.*;

public class BlockGlassClear extends BlockColored {
	public BlockGlassClear(String aUnlocalised) {
		super(ItemBlockMetaType.class, Material.glass, SoundType.GLASS, aUnlocalised, "Glass", MT.Glass, 0.5F, 0.5F, 0, Textures.BlockIcons.GLASSES_CLEAR);
		OM.data(ST.make(this, 1, W), new OreDictItemData(MT.Glass, U *9));
		BlocksGT.breakableGlass.add(this);
	}
	
	@Override
	protected BlockMetaType makeSlab(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		return new BlockGlassClear(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
	}
	
	protected BlockGlassClear(Class<? extends BlockItem> aItemClass, Material aVanillaMaterial, SoundType aVanillaSoundType, String aName, String aDefaultLocalised, OreDictMaterial aMaterial, float aResistanceMultiplier, float aHardnessMultiplier, int aHarvestLevel, int aCount, IIconContainer[] aIcons, byte aSlabType, BlockMetaType aBlock) {
		super(aItemClass, aVanillaMaterial, aVanillaSoundType, aName, aDefaultLocalised, aMaterial, aResistanceMultiplier, aHardnessMultiplier, aHarvestLevel, aCount, aIcons, aSlabType, aBlock);
		OM.data(ST.make(this, 1, W), new OreDictItemData(MT.Glass, U2*9));
		BlocksGT.breakableGlass.add(this);
	}
	
	@Override public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	@Override public int getRenderBlockPass() {return 1;}
	@Override public boolean isOpaqueCube() {return F;}
	@Override public boolean renderAsNormalBlock() {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return mBlock == this || mSide == aSide;}
	public boolean isBlockSolid(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return F;}
	@Override public ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) {return ST.arraylist(OP.scrapGt.mat(MT.Glass, mBlock == this ? 80 : 40));}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		if (aSide == OPOS[mSide]) return T;
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		return aBlock instanceof BlockMetaType && ((BlockMetaType)aBlock).mBlock == mBlock ? WD.meta(aWorld, aX, aY, aZ) != WD.meta(aWorld, aX - OFFX[aSide], aY - OFFY[aSide], aZ - OFFZ[aSide]) || ((((BlockMetaType)aBlock).mSide != mSide || aSide == mSide) && ((BlockMetaType)aBlock).mSide != OPOS[aSide] && ((BlockMetaType)aBlock).mSide != SIDE_ANY) : super.shouldSideBeRendered(aWorld, aX, aY, aZ, aSide);
	}
}
