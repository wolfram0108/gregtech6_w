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

import gregapi.block.IBlockBase;
import gregapi.block.IBlockToolable;
import gregapi.block.ItemBlockBase;
import gregapi.block.ToolCompat;
import gregapi.compat.galacticraft.IBlockSealable;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.command.IEntitySelector;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.IIcon;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class BlockBaseRail extends BaseRailBlock implements IBlockBase, IBlockSealable, IBlockToolable {
	public final String mNameInternal;
	public final float mSpeed, mExplosionResistance;
	public final IIconContainer mIconPrimary, mIconSecondary;
	public final int mHarvestLevel;
	public final boolean mPowerRail, mDetectorRail;
	
	/** @param aSpeed is usually 0.4F */
	public BlockBaseRail(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, String aLocalName, boolean aPowerRail, boolean aDetectorRail, float aSpeed, float aExplosionResistance, int aHarvestLevel, IIconContainer aIconPrimary, IIconContainer aIconSecondary) {
		super(aPowerRail || aDetectorRail);
		mNameInternal = aNameInternal;
		/* PORT-TODO(F16) setCreativeTab */;
		ST.register(this, mNameInternal, aItemClass);
		LH.add(mNameInternal, aLocalName);
		mExplosionResistance = aExplosionResistance;
		mHarvestLevel = aHarvestLevel;
		mSpeed = aSpeed;
		mIconSecondary = aIconSecondary;
		mIconPrimary = aIconPrimary;
		mDetectorRail = aDetectorRail;
		mPowerRail = aPowerRail;
		if (aPowerRail) REDSTONE_SINKS.add(this);
		if (COMPAT_FR != null) COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_RAILSPEED) + LH.Chat.GREEN + Math.min(MD.RC.mLoaded ? 3 : 10, mSpeed/0.4F) + "x");
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	@Override public String name(byte aMeta) {return mNameInternal;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	// PORT-TODO(F13/F16, block-getBlockHardness-removed): 1.7.10 vanilla Block.getBlockHardness(World,x,y,z) не имеет
	// override-точки в neo - BlockBehaviour.BlockStateBase.getDestroySpeed(BlockGetter,BlockPos) [BlockBehaviour.java:636]
	// лишь возвращает запечённое в BlockState значение (не вызывает Block, не переопределяем). Метод остаётся обычным.
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.RAIL, aWorld, aX, aY, aZ);}
	// было getExplosionResistance(Entity,World,x,y,z,eX,eY,eZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]; исходное тело игнорировало все
	// параметры кроме this (константа mExplosionResistance) - переносится без потерь.
	@Override public float getExplosionResistance(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.level.Explosion aExplosion) {return mExplosionResistance;}
	public float getExplosionResistance(Entity aEntity) {return mExplosionResistance;}
	public String getHarvestTool(int aMeta) {return TOOL_crowbar;}
	public int getHarvestLevel(int aMeta) {return mHarvestLevel;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	public boolean renderAsNormalBlock() {return F;}
	public boolean isOpaqueCube() {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	public int damageDropped(int aMeta) {return 0;}
	public int quantityDropped(Random par1Random) {return 1;}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return 0;}
	public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	public Item getItemDropped(int par1, Random par2Random, int par3) {return Item.byBlock(this);}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {return canCreatureSpawn(WD.meta(aWorld, aX, aY, aZ));}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab par2CreativeTabs, @SuppressWarnings("rawtypes") List aList) {aList.add(ST.make(aItem, 1, 0));}
	public IIcon getIcon(int aSide, int aMeta) {return ((mPowerRail||mDetectorRail?(aMeta&8)!=0:aMeta>=6)?mIconSecondary:mIconPrimary).getIcon(0);}
	public boolean isSealed(Level aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return 1;}
	
	@Override public float getExplosionResistance(byte aMeta) {return mExplosionResistance;}
	@Override public int getItemStackLimit(ItemStack aStack) {return 64;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return T;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {
		if (!aWorld.isClientSide()) {
			if (aTool.equals(TOOL_softhammer) && mPowerRail) {
				; // PORT-TODO(isRemote-toggle недостижим: neo isClientSide final; клиент-подавление снято, WD.set flag 0 = минимальное обновление)
				boolean tResult = WD.set(aWorld, aX, aY, aZ, this, WD.meta(aWorld, aX, aY, aZ) ^ 8, 0);
				;
				return tResult?10000:0;
			}
			if (aTool.equals(TOOL_crowbar)) {
				byte aMeta = WD.meta(aWorld, aX, aY, aZ);
				; // PORT-TODO(isRemote-toggle недостижим: neo isClientSide final; клиент-подавление снято, WD.set flag 0 = минимальное обновление)
				// было isPowered() (BlockRailBase.field_150053_a, 1.7.10) - neo BaseRailBlock не хранит этот флаг;
				// GT6-own mPowerRail/mDetectorRail уже несут то же значение (super(aPowerRail||aDetectorRail) в конструкторе).
				boolean tResult = WD.set(aWorld, aX, aY, aZ, this, (mPowerRail || mDetectorRail) ? (aMeta+1) % 10 : ((aMeta/8) * 8) + (((aMeta%8)+1) % 6), 0);
				;
				return tResult?2000:0;
			}
		}
		return ToolCompat.onToolClick(this, aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aWorld, aSide, aX, aY, aZ, aHitX, aHitY, aHitZ);
	}
	
	protected boolean func_150058_a(Level aWorld, int aX, int aY, int aZ, int p_150058_5_, boolean p_150058_6_, int p_150058_7_) {
		if (p_150058_7_ >= 8) return F;
		int j1 = p_150058_5_ & 7;
		boolean flag1 = T;
		switch (j1) {
		case 0: if (p_150058_6_) ++aZ; else --aZ; break;
		case 1: if (p_150058_6_) --aX; else ++aX; break;
		case 2: if (p_150058_6_) --aX; else {++aX; ++aY; flag1 = F;} j1 = 1; break;
		case 3: if (p_150058_6_) {--aX; ++aY; flag1 = F;} else ++aX; j1 = 1; break;
		case 4: if (p_150058_6_) ++aZ; else {--aZ; ++aY; flag1 = F;} j1 = 0; break;
		case 5: if (p_150058_6_) {++aZ; ++aY; flag1 = F;} else --aZ; j1 = 0; break;
		}
		return func_150057_a(aWorld, aX, aY, aZ, p_150058_6_, p_150058_7_, j1) || (flag1 && func_150057_a(aWorld, aX, aY - 1, aZ, p_150058_6_, p_150058_7_, j1));
	}
	
	protected boolean func_150057_a(Level aWorld, int aX, int aY, int aZ, boolean p_150057_5_, int p_150057_6_, int p_150057_7_) {
		if (WD.block(aWorld, aX, aY, aZ) == this) {
			int j1 = WD.meta(aWorld, aX, aY, aZ);
			int k1 = j1 & 7;
			
			if (p_150057_7_ == 1 && (k1 == 0 || k1 == 4 || k1 == 5)) return F;
			if (p_150057_7_ == 0 && (k1 == 1 || k1 == 2 || k1 == 3)) return F;
			
			if ((j1 & 8) != 0) {
				// было World.isBlockIndirectlyGettingPowered(x,y,z) -> SignalGetter.hasNeighborSignal(BlockPos) [SignalGetter.java:71]
				if (aWorld.hasNeighborSignal(new BlockPos(aX, aY, aZ))) return T;
				return func_150058_a(aWorld, aX, aY, aZ, j1, p_150057_5_, p_150057_6_ + 1);
			}
		}
		return F;
	}
	
	// @Override
	protected void func_150048_a(Level aWorld, int aX, int aY, int aZ, int aMeta, int aData, Block aBlock) {
		if (mPowerRail) {
			// было World.isBlockIndirectlyGettingPowered(x,y,z) -> SignalGetter.hasNeighborSignal(BlockPos) [SignalGetter.java:71]
			boolean flag = aWorld.hasNeighborSignal(new BlockPos(aX, aY, aZ));
			flag = flag || func_150058_a(aWorld, aX, aY, aZ, aMeta, T, 0) || func_150058_a(aWorld, aX, aY, aZ, aMeta, F, 0);
			boolean flag1 = F;
			if (flag && (aMeta & 8) == 0) {
				WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aData | 8, 3, F);
				flag1 = T;
			} else if (!flag && (aMeta & 8) != 0) {
				WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aData, 3, F);
				flag1 = T;
			}
			if (flag1) {
				aWorld.updateNeighborsAt(new BlockPos(aX, aY - 1, aZ), this, null);
				if (aData == 2 || aData == 3 || aData == 4 || aData == 5) {
					aWorld.updateNeighborsAt(new BlockPos(aX, aY + 1, aZ), this, null);
				}
			}
		}
	}
	
	public int tickRate(Level aWorld) {return 20;}
	// было canProvidePower() -> BlockBehaviour.isSignalSource(BlockState) [BlockBehaviour.java:218]
	@Override protected boolean isSignalSource(BlockState aState) {return mDetectorRail;}

	// было onEntityCollidedWithBlock(World,x,y,z,Entity) -> BlockBehaviour.entityInside(BlockState,Level,BlockPos,Entity,InsideBlockEffectApplier,boolean) [BlockBehaviour.java:360]
	@Override protected void entityInside(BlockState aState, Level aWorld, BlockPos aPos, Entity aEntity, net.minecraft.world.entity.InsideBlockEffectApplier aEffectApplier, boolean aIsPrecise) {
		if (mDetectorRail && !aWorld.isClientSide()) {
			int l = WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
			if ((l & 8) == 0) func_150054_a(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), l);
		}
	}
	
	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (mDetectorRail && !aWorld.isClientSide()) {
			int l = WD.meta(aWorld, aX, aY, aZ);
			if ((l & 8) != 0) func_150054_a(aWorld, aX, aY, aZ, l);
		}
	}
	
	// было isProvidingWeakPower(IBlockAccess,x,y,z,side) -> BlockBehaviour.getSignal(BlockState,BlockGetter,BlockPos,Direction) [BlockBehaviour.java:356]
	@Override protected int getSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return mDetectorRail ? (WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) & 8) != 0 ? 15 : 0 : 0;}
	// было isProvidingStrongPower(IBlockAccess,x,y,z,side) -> BlockBehaviour.getDirectSignal(BlockState,BlockGetter,BlockPos,Direction) [BlockBehaviour.java:363]
	@Override protected int getDirectSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return mDetectorRail ? (WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) & 8) == 0 ? 0 : (aSide == Direction.UP ? 15 : 0) : 0;}
	
	private void func_150054_a(Level aWorld, int aX, int aY, int aZ, int aMetaData) {
		boolean flag = (aMetaData & 8) != 0;
		boolean flag1 = F;
		@SuppressWarnings("unchecked")
		List<AbstractMinecart> list = aWorld.getEntitiesOfClass(AbstractMinecart.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875));
		
		if (!list.isEmpty()) flag1 = T;
		if (flag1 && !flag) {
			WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMetaData | 8, 3, F);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this, null);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY - 1, aZ), this, null);
			// было World.markBlockRangeForRenderUpdate(x0,y0,z0,x1,y1,z1) -> Level.setBlocksDirty(BlockPos,BlockState,BlockState)
			// [Level.java:335, реальный neo-приём для detector-rail - см. DetectorRailBlock.checkPressed]; GT6 не отслеживает
			// раздельно old/new BlockState (meta не проецирована на реальный BlockState, F13-модель меты) - тот же приём,
			// что уже принят в WD.update (old==new).
			{BlockPos tPos = new BlockPos(aX, aY, aZ); BlockState tState = aWorld.getBlockState(tPos); aWorld.setBlocksDirty(tPos, tState, tState);}
		}
		if (!flag1 && flag) {
			WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMetaData & 7, 3, F);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this, null);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY - 1, aZ), this, null);
			{BlockPos tPos = new BlockPos(aX, aY, aZ); BlockState tState = aWorld.getBlockState(tPos); aWorld.setBlocksDirty(tPos, tState, tState);}
		}
		if (flag1) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate(aWorld));
		// было World.func_147453_f(x,y,z,Block) -> Level.updateNeighborsAt(BlockPos,Block,Orientation) [Level.java:338];
		// тот же приём, что уже используется в этом файле выше (updateNeighborsAt(...,this,null)).
		aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this, null);
	}
	
	// было onBlockAdded(World,x,y,z) -> BlockBehaviour.onPlace(BlockState,Level,BlockPos,BlockState,boolean) [BlockBehaviour.java:167].
	// PORT-TODO(F13/F16, block-onBlockAdded-railbase-super-removed): 1.7.10 super.onBlockAdded (BlockRailBase) выравнивал
	// форму рельса по соседям (func_150052_a) и для powered-рельсов (field_150053_a) сразу дёргал onNeighborBlockChange;
	// neo BaseRailBlock.onPlace работает через RailState/BlockState-Property модель, несовместимую с meta-хранением
	// этого класса (структурная жила BaseRailBlock-конструктора/getShapeProperty вне этой задачи, свой F-шов) -
	// super-вызов не переносится (нет эквивалента 1:1 на этой модели данных). Собственная detector-логика сохранена без потерь.
	@Override protected void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {
		if (mDetectorRail) func_150054_a(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));
	}
	
	public boolean hasComparatorInputOverride() {return mDetectorRail;}
	
	// @Override
	public int getComparatorInputOverride(Level aWorld, int aX, int aY, int aZ, int aSide) {
		if (mDetectorRail && (WD.meta(aWorld, aX, aY, aZ) & 8) > 0) {
			@SuppressWarnings("unchecked")
			List<MinecartCommandBlock> list = aWorld.getEntitiesOfClass(MinecartCommandBlock.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875));
			if (list.size() > 0) return list.get(0).func_145822_e().func_145760_g();
			@SuppressWarnings("unchecked")
			List<AbstractMinecart> list1 = aWorld.selectEntitiesWithinAABB(AbstractMinecart.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875), IEntitySelector.selectInventories);
			if (list1.size() > 0) return AbstractContainerMenu.calcRedstoneFromInventory((Container)list1.get(0));
		}
		return 0;
	}
	
	// было World.doChunksNearChunkExist(x,y,z,radius) -> ILevelReaderExtension.isAreaLoaded(BlockPos,int) [ILevelReaderExtension.java:19]
	// @Override
	public float getRailMaxSpeed(Level aWorld, AbstractMinecart aCart, int aX, int aY, int aZ) {
		switch(WD.meta(aWorld, aX, aY, aZ) & 7) {
		case  0:
			if (WD.block(aWorld, aX  , aY, aZ+1) instanceof BlockBaseRail && (WD.meta(aWorld, aX  , aY, aZ+1) & 7) == 0
			&&  WD.block(aWorld, aX  , aY, aZ-1) instanceof BlockBaseRail && (WD.meta(aWorld, aX  , aY, aZ-1) & 7) == 0) return aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), 17) ? mSpeed : Math.min(mSpeed, 1.0F);
		case  1:
			if (WD.block(aWorld, aX+1, aY, aZ  ) instanceof BlockBaseRail && (WD.meta(aWorld, aX+1, aY, aZ  ) & 7) == 1
			&&  WD.block(aWorld, aX-1, aY, aZ  ) instanceof BlockBaseRail && (WD.meta(aWorld, aX-1, aY, aZ  ) & 7) == 1) return aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), 17) ? mSpeed : Math.min(mSpeed, 1.0F);
		default:
			return Math.min(mSpeed, 0.4F);
		}
	}
	
	// @Override
	public void onMinecartPass(Level aWorld, AbstractMinecart aCart, int aX, int aY, int aZ) {
		if (mPowerRail) {
			byte tRailMeta = WD.meta(aWorld, aX, aY, aZ);
			double tMotion = Math.sqrt(aCart.getDeltaMovement().x*aCart.getDeltaMovement().x + aCart.getDeltaMovement().z*aCart.getDeltaMovement().z);
			if ((tRailMeta & 8) != 0) {
				if (tMotion > 0.01) {
					aCart.getDeltaMovement().x *= 2;
					aCart.getDeltaMovement().z *= 2;
				} else {
					tRailMeta &= 7;
					if (tRailMeta == 1) {
							 if (WD.normalCube(WD.block(aWorld, aX-1, aY, aZ), aWorld, aX-1, aY, aZ)) WD.setMotionX(aCart, +0.02);
						else if (WD.normalCube(WD.block(aWorld, aX+1, aY, aZ), aWorld, aX+1, aY, aZ)) WD.setMotionX(aCart, -0.02);
					} else if (tRailMeta == 0) {
							 if (WD.normalCube(WD.block(aWorld, aX, aY, aZ-1), aWorld, aX, aY, aZ-1)) WD.setMotionZ(aCart, +0.02);
						else if (WD.normalCube(WD.block(aWorld, aX, aY, aZ+1), aWorld, aX, aY, aZ+1)) WD.setMotionZ(aCart, -0.02);
					}
				}
			} else {
				if (tMotion < 0.03) {
					WD.setMotionX(aCart, 0);
					WD.setMotionY(aCart, 0);
					WD.setMotionZ(aCart, 0);
				} else {
					aCart.getDeltaMovement().x /= 2;
					WD.setMotionY(aCart, 0);
					aCart.getDeltaMovement().z /= 2;
				}
			}
		}
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}
		
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == 255 && getMaterial().isSolid()) || !aWorld.canPlaceEntityOnSide(this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;
		
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, SIDES_AXIS_X[UT.Code.getHorizontalForPlayerPlacing(aPlayer)] ? 1 : 0)) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
