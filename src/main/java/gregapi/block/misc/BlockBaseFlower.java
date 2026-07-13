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

package gregapi.block.misc;

import gregapi.api.Optional;
import gregapi.block.IBlockBase;
import gregapi.block.ItemBlockBase;
import gregapi.compat.galacticraft.IBlockSealable;
import gregapi.data.MD;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock;
import mods.railcraft.common.carts.EntityTunnelBore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.IIcon;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.EnumPlantType;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
	@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock", modid = ModIDs.GC)
})
public abstract class BlockBaseFlower extends FlowerBlock implements IBlockBase, IBlockSealable, IOxygenReliantBlock, BonemealableBlock {
	public final String mNameInternal;
	public IIconContainer[] mIcons;
	/** For Creative Subsets, not actually important. */
	private final byte mMaxMeta;
	
	/** @param aSpeed is usually 0.4F */
	public BlockBaseFlower(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, long aMaxMeta, IIconContainer[] aIcons) {
		super(0);
		mMaxMeta = (byte)(UT.Code.bind4(aMaxMeta-1)+1);
		mIcons = aIcons;
		/* PORT-TODO(F16) setStepSound */;
		mNameInternal = aNameInternal;
		/* PORT-TODO(F16) setCreativeTab */;
		ST.register(this, mNameInternal, aItemClass);
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W));
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	@Override public String name(byte aMeta) {return mNameInternal + "." + aMeta;}
	public String getLocalizedName() {return I18n.translateToLocal(mNameInternal);}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 0;}
	public float getExplosionResistance(Entity aEntity, Level aWorld, int aX, int aY, int aZ, double eX, double eY, double eZ) {return 0;}
	public float getExplosionResistance(Entity aEntity) {return 0;}
	public String getHarvestTool(int aMeta) {return TOOL_sword;}
	public int getHarvestLevel(int aMeta) {return 0;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return T;}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return T;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	public int damageDropped(int aMeta) {return aMeta;}
	public int quantityDropped(Random par1Random) {return 1;}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	public Item getItemDropped(int par1, Random aRandom, int par3) {return Item.byBlock(this);}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {for (int i = 0; i < maxMeta(); i++) aList.add(ST.make(aItem, 1, i));}
	public boolean isSealed(Level aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return mMaxMeta;}
	public IIcon getIcon(int aSide, int aMeta) {return mIcons[aMeta % mIcons.length].getIcon(0);}
	public void onOxygenAdded(Level aWorld, int aX, int aY, int aZ) {/**/}
	public void onOxygenRemoved(Level aWorld, int aX, int aY, int aZ) {if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, NB, 0, 3); return;}}
	
	@Override public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {/**/}
	@Override public float getExplosionResistance(byte aMeta) {return 0;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public int getItemStackLimit(ItemStack aStack) {return 64;}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	public EnumPlantType getPlantType(BlockGetter aWorld, int aX, int aY, int aZ) {return EnumPlantType.Plains;}
	public Block getPlant(BlockGetter aWorld, int aX, int aY, int aZ) {return this;}
	public int getPlantMetadata(BlockGetter aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {return WD.oxygen(aWorld, aX, aY, aZ) && WD.block(aWorld, aX, aY - 1, aZ).canSustainPlant(aWorld, aX, aY - 1, aZ, Direction.UP, Blocks.DANDELION);}
	public boolean func_149851_a(Level aWorld, int aX, int aY, int aZ, boolean aIsRemote) {return T;}
	public boolean func_149852_a(Level aWorld, Random aRandom, int aX, int aY, int aZ) {return T;}
	public void func_149853_b(Level aWorld, Random aRandom, int aX, int aY, int aZ) {ST.drop(aWorld, aX+0.5, aY+0.5, aZ+0.5, this, 1, WD.meta(aWorld, aX, aY, aZ));}
	
	// @Override
	public void checkAndDropBlock(Level aWorld, int aX, int aY, int aZ) {
		if (canBlockStay(aWorld, aX, aY, aZ)) return;
		dropBlockAsItem(aWorld, aX, aY, aZ, WD.meta(aWorld, aX, aY, aZ), 0);
		WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		
		if (tTileEntity instanceof TileEntityFlowerPot) {
			if (((TileEntityFlowerPot)tTileEntity).getFlowerPotItem() == null) {
				((TileEntityFlowerPot)tTileEntity).func_145964_a(aItem, ST.meta(aStack));
				tTileEntity.markDirty();
				if (!WD.setMeta(aWorld, aX, aY, aZ, ST.meta(aStack), 2)) aWorld.markBlockForUpdate(aX, aY, aZ);
				if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
			}
			return T;
		}
		
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && tBlock != Blocks.DEAD_BUSH && !tBlock.isReplaceable(aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}
		
		if (!aPlayer.canPlayerEdit(aX, aY, aZ, aSide, aStack) || (aY == 255 && getMaterial().isSolid()) || !aWorld.canPlaceEntityOnSide(this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;
		
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aItem.getMetadata(aStack.getDamageValue())))) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
