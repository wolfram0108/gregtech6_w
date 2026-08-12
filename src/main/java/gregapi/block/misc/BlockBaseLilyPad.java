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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.block.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;

import static gregapi.data.CS.*;
import static net.minecraftforge.common.EnumPlantType.*;

import java.util.List;
import java.util.Random;

import gregapi.block.BlockBaseMeta;
import gregapi.block.ItemBlockBase;
import gregapi.data.MD;
import gregapi.data.RM;
import gregapi.render.BlockTextureDefault;
import gregapi.render.IIconContainer;
import gregapi.render.IRenderedBlock;
import gregapi.render.IRenderedBlockObject;
import gregapi.render.ITexture;
import gregapi.render.RendererBlockTextured;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import mods.railcraft.common.carts.EntityTunnelBore;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

public class BlockBaseLilyPad extends BlockBaseMeta implements IPlantable, IRenderedBlock {
	public BlockBaseLilyPad(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons) {
		super(ItemBlockBase.class, aNameInternal, Material.plants, SoundType.GRASS, aMaxMeta, aIcons);
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.015625F, 1.0F);
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.DECORATIONS);
		// F12-followup (block-split): RM.chisel/COMPAT_FR используют ST.make → server-start → deferItemInit.
		gregapi.GT_API.deferItemInit(() -> {
		RM.chisel(aNameInternal, ST.make(this, 1, W));
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W)));
		});
	}
	
	@Override public String getHarvestTool(int aMeta) {return TOOL_sword;}
	@Override public int getHarvestLevel(int aMeta) {return 0;}
	// было super.addCollisionBoxesToList(...) (1.7.10 Block, УДАЛЁН из neo целиком). Дефолт inline-порт 1:1 вместо
	// super-вызова (Block.java:661-669 recompSrc), тот же приём, что уже принят в MultiTileEntityBlock.
	public void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, @SuppressWarnings("rawtypes") List aList, Entity aEntity) {if (!(aEntity instanceof Boat)) {AABB tBox = getCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ); if (tBox != null && aAABB.intersects(tBox)) aList.add(tBox);}}
	public boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {return aY >= WD.minY(aWorld) && aY < WD.topY(aWorld) && WD.getMaterial(WD.block(aWorld, aX, aY - 1, aZ)) == Material.water && WD.meta(aWorld, aX, aY - 1, aZ) == 0;} // BUG-089: было aY >= 0 && aY < 256 — границы мира через центр F6-Y-scale
	// было Block.canPlaceBlockAt(World,x,y,z) (1.7.10, дефолт world.getBlock(x,y,z).isReplaceable(...), Block.java:1046-1049)
	// удалён из neo целиком - inline-порт вместо super через уже-существующий центр WD.replaceable (тот же приём, что
	// BlockBase.onItemUse уже использует для идентичной проверки).
	public boolean canPlaceBlockAt(Level aWorld, int aX, int aY, int aZ) {return WD.replaceable(WD.block(aWorld, aX, aY, aZ), aWorld, aX, aY, aZ) && canBlockStay(aWorld, aX, aY, aZ);}
	@Override public boolean checkNoEntityCollision(Level aWorld, int aX, int aY, int aZ, byte aMeta, Entity aExceptThisOne) {return T;}
	@Override public void onNeighborBlockChange2(Level aWorld, int aX, int aY, int aZ, Block aBlock) {checkAndDropBlock(aWorld, aX, aY, aZ);}
	@Override public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {checkAndDropBlock(aWorld, aX, aY, aZ);}
	@Override public boolean isOpaqueCube() {return F;}
	@Override public boolean renderAsNormalBlock() {return F;}
	@Override public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	@Override public boolean isSideSolid(int aMeta, byte aSide) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	public EnumPlantType getPlantType(BlockGetter aWorld, int aX, int aY, int aZ) {return Water;}
	public Block getPlant(BlockGetter aWorld, int aX, int aY, int aZ) {return this;}
	public int getPlantMetadata(BlockGetter aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.LILY_PAD, aWorld, aX, aY, aZ);}
	@Override public float getExplosionResistance(byte aMeta) {return Blocks.LILY_PAD.getExplosionResistance();}
	@Override public int getItemStackLimit(ItemStack aStack) {return 64;}
	
	public void checkAndDropBlock(Level aWorld, int aX, int aY, int aZ) {
		if (!canBlockStay(aWorld, aX, aY, aZ)) {
			WD.dropBlockAsItem(aWorld, aX, aY, aZ, WD.meta(aWorld, aX, aY, aZ), 0);
			// было Block.getBlockById(0) (1.7.10 static-реестр по числовому ID, 0=воздух) -> centre-константа NB (=Blocks.AIR, CS.java:876)
			WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
		}
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {
		HitResult tPos = WD.getMOP(aWorld, aPlayer, T);
		if (tPos == null || tPos.getType() != HitResult.Type.BLOCK) return aStack;
		int aX = ((BlockHitResult)tPos).getBlockPos().getX(), aY = ((BlockHitResult)tPos).getBlockPos().getY(), aZ = ((BlockHitResult)tPos).getBlockPos().getZ();
		// было World.canMineBlock(EntityPlayer,x,y,z) -> Level.mayInteract(Entity,BlockPos) [Level.java:887], тот же смысл (spawn-protection дефолт true).
		if (!aWorld.mayInteract(aPlayer, new BlockPos(aX, aY, aZ)) || !(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), ((BlockHitResult)tPos).getDirection(), aStack)) return aStack;
		// было World.isAirBlock(x,y,z) -> LevelReader.isEmptyBlock(BlockPos) [LevelReader.java:84-86]
		if (WD.getMaterial(WD.block(aWorld, aX, aY, aZ)) == Material.water && WD.meta(aWorld, aX, aY, aZ) == 0 && aWorld.isEmptyBlock(new BlockPos(aX, aY+1, aZ))) {
			WD.set(aWorld, aX, aY+1, aZ, this, ST.meta_(aStack), 3);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) {aStack.setCount(aStack.getCount()-1);}
		}
		return aStack;
	}
	
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return new AABB(aX, aY, aZ, aX+1, aY+0.015625F, aZ+1);}
	public AABB getSelectedBoundingBoxFromPool (Level aWorld, int aX, int aY, int aZ) {return new AABB(aX, aY, aZ, aX+1, aY+0.015625F, aZ+1);}
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {setBlockBounds(0, 0, 0, 1, 0.015625F, 1);}
	
	@Override public ResourceLocation getIcon(int aSide, int aMeta) {return mIcons[aMeta % mIcons.length].getIcon(0);}
	public int getRenderType() {return RendererBlockTextured.INSTANCE==null?23:RendererBlockTextured.INSTANCE.mRenderID;}
	@Override public ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return SIDES_VERTICAL[aSide] ? BlockTextureDefault.get(mIcons[ST.meta_(aStack) % mIcons.length]) : null;}
	@Override public ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return SIDES_VERTICAL[aSide] ? BlockTextureDefault.get(mIcons[WD.meta(aWorld, aX, aY, aZ) % mIcons.length]) : null;}
	@Override public boolean setBlockBounds(int aRenderPass, ItemStack aStack) {setBlockBounds(0, 0, 0, 1, 0.015625F, 1); return T;}
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {setBlockBounds(0, 0, 0, 1, 0.015625F, 1); return T;}
	@Override public int getRenderPasses(ItemStack aStack) {return 1;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override public IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return null;}
	@Override public boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return T;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return T;}
}
