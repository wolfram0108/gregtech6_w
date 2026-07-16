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
import gregapi.block.BlockBaseMeta;
import gregapi.block.IBlockOnWalkOver;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.old.Textures;
import gregapi.render.*;
import gregapi.util.ST;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static gregapi.data.CS.*;

public class BlockPath extends BlockBaseMeta implements IBlockOnWalkOver, IRenderedBlock {
	public BlockPath(String aUnlocalised) {
		super(null, aUnlocalised, Material.grass, SoundType.GRASS, 12, Textures.BlockIcons.DIRTS);
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.TRANSPORT);
		LH.add(getUnlocalizedName()+ ".0", "Path");
		LH.add(getUnlocalizedName()+ ".1", "Aether Path");
		LH.add(getUnlocalizedName()+ ".2", "Loamy Path");
		LH.add(getUnlocalizedName()+ ".3", "Sandy Path");
		LH.add(getUnlocalizedName()+ ".4", "Silty Path");
		LH.add(getUnlocalizedName()+ ".5", "Alfisol Path");
		LH.add(getUnlocalizedName()+ ".6", "Andisol Path");
		LH.add(getUnlocalizedName()+ ".7", "Gelisol Path");
		LH.add(getUnlocalizedName()+ ".8", "Histosol Path");
		LH.add(getUnlocalizedName()+ ".9", "Inceptisol Path");
		LH.add(getUnlocalizedName()+".10", "Mollisol Path");
		LH.add(getUnlocalizedName()+".11", "Oxisol Path");
		LH.add(getUnlocalizedName()+".12", "Path");
		LH.add(getUnlocalizedName()+".13", "Path");
		LH.add(getUnlocalizedName()+".14", "Path");
		LH.add(getUnlocalizedName()+".15", "Path");
		setBlockBounds(0, 0, 0, 1, PIXELS_NEG[1], 1);
		
		if (COMPAT_FR  != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("digger", ST.make(this, 1, W)));
	}
	
	public ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) { // GT6-хук (не neo @Override); мост дропа зовёт его как у core-блоков
		switch(aMeta) {
		case  1: return ST.arraylist(IL.AETHER_Dirt.get(1));
		case  2: return ST.arraylist(IL.BoP_Dirt_Loamy.get(1));
		case  3: return ST.arraylist(IL.BoP_Dirt_Sandy.get(1));
		case  4: return ST.arraylist(IL.BoP_Dirt_Silty.get(1));
		case  5: return ST.arraylist(IL.EB_Dirt_Alfisol.get(1));
		case  6: return ST.arraylist(IL.EB_Dirt_Andisol.get(1));
		case  7: return ST.arraylist(IL.EB_Dirt_Gelisol.get(1));
		case  8: return ST.arraylist(IL.EB_Dirt_Histosol.get(1));
		case  9: return ST.arraylist(IL.EB_Dirt_Inceptisol.get(1));
		case 10: return ST.arraylist(IL.EB_Dirt_Mollisol.get(1));
		case 11: return ST.arraylist(IL.EB_Dirt_Oxisol.get(1));
		default: return ST.arraylist(ST.make(Blocks.DIRT, 1, 0));
		}
	}
	
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) { // GT6 render-хук (мост neo=skipRendering в BlockBase); F-shape/render отложены core-wide
		if (SIDES_TOP[aSide]) return T;
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		return tBlock != Blocks.FARMLAND && !WD.visOpq(tBlock);
	}
	
	public int getRenderType() {return RendererBlockTextured.INSTANCE==null?0:RendererBlockTextured.INSTANCE.mRenderID;}
	
	@Override
	public ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {
		if (SIDES_TOP[aSide]) return BlockTextureDefault.get(Textures.BlockIcons.PATH_TOP);
		ITexture tDirt = BlockTextureDefault.get(mIcons[ST.meta_(aStack) % 16]);
		return SIDES_BOTTOM[aSide]?tDirt:BlockTextureMulti.get(tDirt, BlockTextureDefault.get(Textures.BlockIcons.PATH_SIDE));
	}
	
	@Override
	public ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (SIDES_BOTTOM[aSide]) return BlockTextureDefault.get(mIcons[WD.meta(aWorld, aX, aY, aZ) % 16]);
		if (SIDES_TOP   [aSide]) return BlockTextureDefault.get(Textures.BlockIcons.PATH_TOP);
		return BlockTextureMulti.get(   BlockTextureDefault.get(mIcons[WD.meta(aWorld, aX, aY, aZ) % 16]), BlockTextureDefault.get(isHalfBlock(aWorld, aX, aY, aZ) ? Textures.BlockIcons.PATH_SLAB : Textures.BlockIcons.PATH_SIDE));
	}
	
	public boolean isHalfBlock(BlockGetter aWorld, int aX, int aY, int aZ) {
		return WD.block(aWorld, aX+1, aY-1, aZ) == this || WD.block(aWorld, aX, aY-1, aZ+1) == this || WD.block(aWorld, aX-1, aY-1, aZ) == this || WD.block(aWorld, aX, aY-1, aZ-1) == this;
	}
	
	@Override
	public void onWalkOver(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if ((WD.motionX(aEntity) != 0 || WD.motionZ(aEntity) != 0) && !aEntity.isInWater() && !aEntity.isShiftKeyDown()) {
			double tSpeed = (WD.block(aWorld, aX, aY-1, aZ).getFriction() >= 0.8 && isHalfBlock(aWorld, aX, aY, aZ) ? 1.05 : 1.1);
			WD.setMotionX(aEntity, WD.motionX(aEntity)*tSpeed); WD.setMotionZ(aEntity, WD.motionZ(aEntity)*tSpeed);
		}
		// Convert Et Futurum Grass Paths to this when adjacent.
		if (IL.EtFu_Path.exists()) for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (IL.EtFu_Path.equal(WD.block(aWorld, aX+i, aY+j, aZ+k))) {
				WD.set(aWorld, aX+i, aY+j, aZ+k, this, 0, 2);
			}
		}
	}
	
	@Override public boolean usesRenderPass(int aRenderPass, ItemStack aStack                                                                     ) {return T;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered         ) {return T;}
	@Override public boolean setBlockBounds(int aRenderPass, ItemStack aStack                                                                     ) {setBlockBounds(0, 0, 0, 1,                                          PX_N[1] , 1); return T;}
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered         ) {setBlockBounds(0, 0, 0, 1, (isHalfBlock(aWorld, aX, aY, aZ)?PX_N[9]:PX_N[1]), 1); return T;}
	@Override public int getRenderPasses(ItemStack aStack                                                                                         ) {return 1;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered                             ) {return 1;}
	@Override public IRenderedBlockObject passRenderingToObject(ItemStack aStack                                                                  ) {return null;}
	@Override public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ                                       ) {return null;}
	@Override public Identifier getIcon(int aSide, int aMeta) {return (SIDES_TOP[aSide]?Textures.BlockIcons.PATH_TOP:Textures.BlockIcons.DIRTS[aMeta % 16]).getIcon(0);}
	
	
	@SuppressWarnings({"unchecked"})
	public void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, List aList, Entity aEntity) {
		AABB
		tAABB = new AABB(aX, aY, aZ, aX+1, aY+0.5  , aZ+1); if (tAABB.intersects(aAABB)) aList.add(tAABB);
		if (isHalfBlock(aWorld, aX, aY, aZ)) return;
		tAABB = new AABB(aX, aY, aZ, aX+1, aY+0.875, aZ+1); if (tAABB.intersects(aAABB)) aList.add(tAABB);
		tAABB = new AABB(aX, aY, aZ, aX+1, aY+1    , aZ+1); if (tAABB.intersects(aAABB)) aList.add(tAABB);
	}
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return new AABB(aX, aY, aZ, aX+1, aY+(isHalfBlock(aWorld, aX, aY, aZ)?0.5:1), aZ+1);}
	public AABB getSelectedBoundingBoxFromPool (Level aWorld, int aX, int aY, int aZ) {return new AABB(aX, aY, aZ, aX+1, aY+(isHalfBlock(aWorld, aX, aY, aZ)?0.5:1), aZ+1);}
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {setBlockBounds(0, 0, 0, 1, (isHalfBlock(aWorld, aX, aY, aZ)?0.5F:1), 1);}
	@Override public boolean doesWalkSpeed(byte aMeta) {return T;}
	@Override public boolean doesPistonPush(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest() {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public int getLightOpacity() {return LIGHT_OPACITY_WATER;}
	@Override public String getHarvestTool(int aMeta) {return TOOL_shovel;}
	@Override public int getHarvestLevel(int aMeta) {return 0;}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.GRASS_BLOCK, aWorld, aX, aY, aZ) * 2;}
	@Override public float getExplosionResistance(byte aMeta) {return Blocks.GRASS_BLOCK.getExplosionResistance() * 1.5F;}
	@Override public boolean isSideSolid(int aMeta, byte aSide) {return SIDES_BOTTOM_HORIZONTAL[aSide];}
	@Override public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	public boolean isNormalCube() {return F;}
	@Override public boolean isOpaqueCube() {return F;}
	@Override public boolean renderAsNormalBlock() {return F;}
}
