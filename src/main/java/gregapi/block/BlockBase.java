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

package gregapi.block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;

import gregapi.data.LH;
import gregapi.data.OP;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import gregapi.block.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.IIcon;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBase extends Block implements IBlockBase {
	public final String mNameInternal;
	/** F-bounds: последние заданные bounds (1.7.10 мутировал Block.mBoundingBox); рендер-использование
	 *  отложено на F3-клиент-проход. Хранит форму {minX,minY,minZ,maxX,maxY,maxZ}. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}

	/** F9: gregapi Material (портированная 1.7.10-модель) хранится блоком — neo `WD.getMaterial(Block)` удалён. */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}
	public BlockBase(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType) {
		// F16/F9 форс движка: neo `Block` immutable (данные в Properties ДО super). setStepSound встроен в Properties.sound;
		// setBlockName удалён (имя через реестр — ST.register ниже); setCreativeTab удалён (creative-tab через
		// BuildCreativeModeTabContentsEvent-датаген) → PORT-TODO(F16). Твёрдость/light/mapColor из Material — мост F9, дефолт пока.
		super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().sound(aSoundType));
		mMaterial = aMaterial;
		mNameInternal = aNameInternal;
		ST.register(this, mNameInternal, aItemClass);
		LH.add(mNameInternal+"."+W, "Any Sub-Block of this one");
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	public String getLocalizedName() {return I18n.translateToLocal(mNameInternal);}
	public String getHarvestTool(int aMeta) {return TOOL_pickaxe;}
	public int getHarvestLevel(int aMeta) {return 0;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return T;}
	public boolean renderAsNormalBlock() {return T;}
	public boolean isOpaqueCube() {return T;}
	public boolean func_149730_j() {return isOpaqueCube();}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return isSideSolid(WD.meta(aWorld, aX, aY, aZ), UT.Code.side(aDirection));}
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return isOpaqueCube() ? !WD.visOpq(WD.block(aWorld, aX, aY, aZ)) : super.shouldSideBeRendered(aWorld, aX, aY, aZ, aSide);}
	public int damageDropped(int aMeta) {return aMeta;}
	public int quantityDropped(int aMeta, int aFortune, Random aRandom) {return 1;}
	public ItemStack createStackedBlock(int aMeta) {return ST.make(this, 1, damageDropped(aMeta));}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public int getLightOpacity() {return LIGHT_OPACITY_MAX;}
	public Item getItemDropped(int aMeta, Random aRandom, int aFortune) {return Item.byBlock(this);}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public boolean canSustainPlant(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide, IPlantable aPlant) {return F;}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {byte aMeta = WD.meta(aWorld, aX, aY, aZ); return canCreatureSpawn(aMeta) && isSideSolid(aMeta, SIDE_TOP);}
	public boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {return isFireSource(WD.meta(aWorld, aX, aY, aZ));}
	public boolean isFlammable(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return isFlammable(WD.meta(aWorld, aX, aY, aZ));}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFlammability(WD.meta(aWorld, aX, aY, aZ));}
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFireSpreadSpeed(WD.meta(aWorld, aX, aY, aZ));}
	public float getExplosionResistance(Entity aEntity, Level aWorld, int aX, int aY, int aZ, double eX, double eY, double eZ) {return getExplosionResistance(WD.meta(aWorld, aX, aY, aZ));}
	public float getExplosionResistance(Entity aEntity) {return getExplosionResistance((byte)0);}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 1;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return 1;}
	public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {if (useGravity(WD.meta(aWorld, aX, aY, aZ))) aWorld.scheduleBlockUpdate(aX, aY, aZ, this, 2); onNeighborBlockChange2(aWorld, aX, aY, aZ, aBlock);}
	public final void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {if (useGravity(WD.meta(aWorld, aX, aY, aZ))) aWorld.scheduleBlockUpdate(aX, aY, aZ, this, 2); onBlockAdded2(aWorld, aX, aY, aZ);}
	public IIcon getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return getIcon(aSide, WD.meta(aWorld, aX, aY, aZ));}
	
	@Override public String name(byte aMeta) {return aMeta == W ? mNameInternal : mNameInternal + "." + aMeta;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return isSideSolid(aMeta, aSide);}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {/**/}
	@Override public float getExplosionResistance(byte aMeta) {return 10.0F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public int getItemStackLimit(ItemStack aStack) {return UT.Code.bindStack(OP.block.mDefaultStackSize);}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	public boolean checkNoEntityCollision(Level aWorld, int aX, int aY, int aZ, byte aMeta, Entity aExceptThisOne) {return aWorld.checkNoEntityCollision(new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1), aExceptThisOne);}
	public boolean isSideSolid(int aMeta, byte aSide) {return T;}
	public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {/**/}
	public void onNeighborBlockChange2(Level aWorld, int aX, int aY, int aZ, Block aBlock) {/**/}
	public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {/**/}
	
	// @Override
	public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (aWorld.isClientSide() || checkGravity(aWorld, aX, aY, aZ)) return;
		updateTick2(aWorld, aX, aY, aZ, aRandom);
	}
	
	public boolean checkGravity(Level aWorld, int aX, int aY, int aZ) {
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		if (aY > 0 && useGravity(aMeta) && FallingBlock.func_149831_e(aWorld, aX, aY - 1, aZ)) {
			if (!FallingBlock.fallInstantly && aWorld.checkChunksExist(aX-32, aY-32, aZ-32, aX+32, aY+32, aZ+32)) {
				if (!aWorld.isClientSide()) aWorld.addFreshEntity(new FallingBlockEntity(aWorld, aX+0.5, aY+0.5, aZ+0.5, this, aMeta));
			} else {
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				while (FallingBlock.func_149831_e(aWorld, aX, aY-1, aZ) && aY > 0) --aY;
				if (aY > 0) WD.set(aWorld, aX, aY, aZ, this, aMeta, 2);
			}
			return T;
		}
		return F;
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && tBlock != Blocks.DEAD_BUSH && !tBlock.isReplaceable(aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}
		
		if (!WD.block(aWorld, aX, aY, aZ).isReplaceable(aWorld, aX, aY, aZ)) return F;
		if (!canReplace(aWorld, aX, aY, aZ, aSide, aStack)) return F;
		byte aMeta = UT.Code.bind4(aItem.getMetadata(ST.meta(aStack)));
		if (!checkNoEntityCollision(aWorld, aX, aY, aZ, aMeta, null)) return F;
		if (!WD.mayEdit(aPlayer, aX, aY, aZ, aSide, aStack) || (aY == 255 && getMaterial().isSolid()) || !aWorld.canPlaceEntityOnSide(this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;
		
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aMeta))) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
	
	public final int quantityDropped(Random aRandom) {return quantityDropped(0, 0, aRandom);}
}
