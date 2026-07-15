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

import net.minecraft.core.BlockPos;

import gregapi.api.Optional;
import gregapi.block.IBlockBase;
import gregapi.block.ItemBlockBase;
import gregapi.block.Material;
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
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.resources.Identifier;
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
	/** F9: было super(Material.plants) — BlockFlower(1.7.10, recompSrc Block.java:26) — переходник не
	 *  распространён на классы вне BlockBase (F9 4-bis, тот же приём переиспользован: собственное mMaterial/
	 *  getMaterial(), не новая абстракция). */
	protected final Material mMaterial = Material.plants;
	public Material getMaterial() {return mMaterial;}

	/** @param aSpeed is usually 0.4F */
	public BlockBaseFlower(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, long aMaxMeta, IIconContainer[] aIcons) {
		// F16/F9 форс движка: 1.7.10 BlockFlower(int) отбирал группу суб-типов (не эффект) - концепт исчез; neo
		// FlowerBlock(SuspiciousStewEffects,Properties) [FlowerBlock.java:36] требует эффект похлёбки - GT6-цветы
		// декоративные (без спец-эффекта) -> SuspiciousStewEffects.EMPTY [SuspiciousStewEffects.java:25], тот же
		// Properties.of()-дефолт, что и остальные BlockBase-наследники (F9-мост твёрдости отложен туда же).
		super(net.minecraft.world.item.component.SuspiciousStewEffects.EMPTY, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
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
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {/* IBlock-хук; F-shape отложена core-wide, FlowerBlock несёт свой neo SHAPE */}
	// neo BonemealableBlock: GT6-цветы декоративны — костная мука неприменима (как ванильные одиночные цветы).
	@Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader aWorld, BlockPos aPos, BlockState aState) {return F;}
	@Override public boolean isBonemealSuccess(net.minecraft.world.level.Level aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {return F;}
	@Override public void performBonemeal(net.minecraft.server.level.ServerLevel aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {/**/}
	@Override public String name(byte aMeta) {return mNameInternal + "." + aMeta;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 0;}
	// было getExplosionResistance(Entity,World,x,y,z,eX,eY,eZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]; исходное тело игнорировало все параметры (константа 0).
	@Override public float getExplosionResistance(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.level.Explosion aExplosion) {return 0;}
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
	public Identifier getIcon(int aSide, int aMeta) {return mIcons[aMeta % mIcons.length].getIcon(0);}
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
	// было Block.canSustainPlant(IBlockAccess,x,y,z,side,IPlantable) (1.7.10) -> IBlockExtension.canSustainPlant(BlockState,
	// BlockGetter,BlockPos,Direction,BlockState) [IBlockExtension.java:424], TriState вместо boolean; вызов на generic
	// Block-объекте (не IMTE-дispatch, простая soil-проверка) - toBoolean(T) как дефолт для TriState.DEFAULT ("плант решает
	// сам"), тот же fallback, что в MultiTileEntityBlock.canSustainPlant, для соответствия старому boolean-контракту метода.
	public boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {BlockPos tBelow = new BlockPos(aX, aY-1, aZ); return WD.oxygen(aWorld, aX, aY, aZ) && WD.block(aWorld, aX, aY - 1, aZ).canSustainPlant(aWorld.getBlockState(tBelow), aWorld, tBelow, Direction.UP, Blocks.DANDELION.defaultBlockState()).toBoolean(T);}
	public boolean func_149851_a(Level aWorld, int aX, int aY, int aZ, boolean aIsRemote) {return T;}
	public boolean func_149852_a(Level aWorld, Random aRandom, int aX, int aY, int aZ) {return T;}
	public void func_149853_b(Level aWorld, Random aRandom, int aX, int aY, int aZ) {ST.drop(aWorld, aX+0.5, aY+0.5, aZ+0.5, this, 1, WD.meta(aWorld, aX, aY, aZ));}
	// было Block.onBlockPlaced(World,x,y,z,side,hitX,hitY,hitZ,meta) (1.7.10 vanilla override-точка, дефолт identity
	// return meta [recompSrc Block.java:1067-1069]) - удалено из neo целиком; GT6-own reintroduced generic-hook (тот
	// же приём, что BlockBaseSpike/BlockBaseLog/BlockBaseBeam уже переопределяют), дефолт-идентичность как в оригинале.
	public int onBlockPlaced(Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, int aMeta) {return aMeta;}
	
	// @Override
	public void checkAndDropBlock(Level aWorld, int aX, int aY, int aZ) {
		if (canBlockStay(aWorld, aX, aY, aZ)) return;
		WD.dropBlockAsItem(aWorld, aX, aY, aZ, WD.meta(aWorld, aX, aY, aZ), 0);
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
				// было TileEntity.markDirty() -> BlockEntity.setChanged() [BlockEntity.java:219]
				tTileEntity.setChanged();
				// было World.markBlockForUpdate(x,y,z) -> Level.setBlocksDirty(BlockPos,BlockState,BlockState)
				// [Level.java:335], тот же приём, что уже принят в BlockBaseRail.func_150054_a (old==new, GT6 не
				// отслеживает раздельно old/new BlockState в meta-модели).
				if (!WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), ST.meta(aStack), 2, F)) {BlockPos tPos = new BlockPos(aX, aY, aZ); BlockState tState = aWorld.getBlockState(tPos); aWorld.setBlocksDirty(tPos, tState, tState);}
				if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
			}
			return T;
		}

		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		// было tBlock != Blocks.tallgrass (1.7.10 единый BlockTallGrass, meta grass/fern) -> neo раздвоил на
		// Blocks.SHORT_GRASS/Blocks.FERN, оба instanceof TallGrassBlock [TallGrassBlock.java:15, Blocks.java:707-732] -
		// instanceof как 1:1-эквивалент identity-проверки единого класса (второй tBlock!=DEAD_BUSH дубль-баг порта устранён).
		} else if (tBlock != Blocks.VINE && !(tBlock instanceof net.minecraft.world.level.block.TallGrassBlock) && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		// World.canPlaceEntityOnSide восстановлен 1:1 через ЦЕНТР WD.canPlaceEntityOnSide (Forge-хук удалён по ИМЕНИ,
		// способность есть — коллизия формы с исключением размещающего + заменяемость цели; централизован в WD.java).
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == 255 && getMaterial().isSolid()) || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aItem.getMetadata(aStack.getDamageValue())))) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
