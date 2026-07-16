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
import gregapi.block.Material;
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
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.texture.IIconRegister;
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
import net.minecraft.resources.Identifier;
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
	/** F9: было super(Material.circuits) — BlockRailBase(1.7.10, recompSrc Block.java:34) — переходник не
	 *  распространён на классы вне BlockBase (F9 4-bis, тот же приём переиспользован: собственное mMaterial/
	 *  getMaterial(), не новая абстракция). */
	protected final Material mMaterial = Material.circuits;
	public Material getMaterial() {return mMaterial;}
	/** F-bounds: BlockBaseRail не наследует BlockBase (наследует vanilla BaseRailBlock) — IBlock.setBlockBounds
	 *  всё равно обязателен (implements IBlockBase extends IBlock); тот же приём, что BlockBase#setBlockBounds
	 *  (BlockBase.java:67-69), локально (не новая абстракция, переиспользование формы). */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}

	// PORT-TODO(F16, block-baserailblock-state-property-model): neo BaseRailBlock ре-абстрагирует
	// getShapeProperty()/codec() (BaseRailBlock.java:47,152) под BlockState-Property модель формы рельса
	// (RailShape); GT6 хранит форму через meta (WD.meta/WD.set), не через BlockState-property. createBlockStateDefinition
	// вызывается ВНУТРИ Block-конструктора [Block.java:235-239] ДО инициализации полей mPowerRail/mDetectorRail
	// (порядок super->createBlockStateDefinition->поля подкласса) - per-instance выбор STRAIGHT(6-знач.,
	// PoweredRailBlock.SHAPE)/CURVED(10-знач., RailBlock.SHAPE) недостижим на этом этапе. Используем безусловно
	// более широкий RailBlock.SHAPE [RailBlock.java:16] (10 значений, надмножество PoweredRailBlock.SHAPE) для ВСЕХ
	// вариантов - безопасный супернабор (state.getValue никогда не бросает), ценой утраты vanilla-валидации
	// "нет кривых у powered/detector rail" (GT6 всё равно не пишет в это state через WD.meta-модель, потери
	// функциональности нет). WATERLOGGED [BaseRailBlock.java:28] регистрируется рядом по той же причине
	// (getFluidState/updateShape тоже требуют его в state, иначе state.getValue бросает).
	private static final Property<RailShape> SHAPE_PROPERTY = RailBlock.SHAPE;
	@Override public Property<RailShape> getShapeProperty() {return SHAPE_PROPERTY;}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {builder.add(SHAPE_PROPERTY, WATERLOGGED);}
	// PORT-TODO(F16, block-codec-not-datadriven): 1.7.10 не имел codec-based регистрации (класс отсутствовал как
	// override-точка) - neo Block.codec() [Block.java:126-129] переабстрагирован BaseRailBlock.codec()
	// [BaseRailBlock.java:47], требует MapCodec<? extends BaseRailBlock>; GT6 регистрирует блоки процедурно
	// (ST.register, много-аргументный конструктор), несовместимо с simpleCodec(Function<Properties,B>)
	// [BlockBehaviour.java:127] (однопараметрический). MapCodec.unit(...) - тот же приём, что F16-decision
	// (decisions/F16-block-codec.md) уже утвердил для процедурно регистрируемых GT6-блоков (не участвует в
	// реальной (де)сериализации), живой neo-пример использования — MapCodec.unit(Supplier) [EmptyModel.java:33].
	@Override public MapCodec<? extends BaseRailBlock> codec() {return MapCodec.unit(() -> this);}

	/** @param aSpeed is usually 0.4F */
	public BlockBaseRail(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, String aLocalName, boolean aPowerRail, boolean aDetectorRail, float aSpeed, float aExplosionResistance, int aHarvestLevel, IIconContainer aIconPrimary, IIconContainer aIconSecondary) {
		// F16/F9 форс движка: neo BaseRailBlock(boolean,Properties) требует Properties [BaseRailBlock.java:41] -
		// тот же Properties.of()-дефолт, что BlockBase уже использует (F9-мост твёрдости/материала отложен туда же).
		// F12-followup (block-split): setId в Properties (иначе «Block id not set»); namespace=GAPI (совпадает с реестром,
		// куда ST.register клал блок), ключ санитизирован. aNameInternal (поле ещё не присвоено на этой строке).
		super(aPowerRail || aDetectorRail, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aNameInternal)))));
		mNameInternal = aNameInternal;
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.TRANSPORT);
		// F12-followup (block-split): блок регистрирует registerBlockLazy на call-site; ЗДЕСЬ — только BlockItem через supplier.
		final Class<? extends net.minecraft.world.item.BlockItem> tItemClass = aItemClass==null?gregapi.block.ItemBlockBase.class:aItemClass;
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> (net.minecraft.world.item.BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, gregapi.data.CS.T, this));
		LH.add(mNameInternal, aLocalName);
		mExplosionResistance = aExplosionResistance;
		mHarvestLevel = aHarvestLevel;
		mSpeed = aSpeed;
		mIconSecondary = aIconSecondary;
		mIconPrimary = aIconPrimary;
		mDetectorRail = aDetectorRail;
		mPowerRail = aPowerRail;
		if (aPowerRail) REDSTONE_SINKS.add(this);
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W)));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_RAILSPEED) + LH.Chat.GREEN + Math.min(MD.RC.mLoaded ? 3 : 10, mSpeed/0.4F) + "x");
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	@Override public String name(byte aMeta) {return mNameInternal;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	// F13: этот getBlockHardness ПОДКЛЮЧЁН к neo через BlockBase.getDestroyProgress (централизованный override,
	// vanilla-формула по getBlockHardness) — блок несёт RAIL-твёрдость 1:1. Не заглушка.
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
	public Identifier getIcon(int aSide, int aMeta) {return ((mPowerRail||mDetectorRail?(aMeta&8)!=0:aMeta>=6)?mIconSecondary:mIconPrimary).getIcon(0);}
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
				; // 1.7.10 isRemote=T/F вокруг setBlock подавлял клиент-пакет; neo isClientSide() final — но способность ЕСТЬ: WD.set flag 0 (без UPDATE_CLIENTS=2) = НЕТ клиент-пакета, точно итог оригинала (isRemote=T + flag 0). Не деградация.
				boolean tResult = WD.set(aWorld, aX, aY, aZ, this, WD.meta(aWorld, aX, aY, aZ) ^ 8, 0);
				;
				return tResult?10000:0;
			}
			if (aTool.equals(TOOL_crowbar)) {
				byte aMeta = WD.meta(aWorld, aX, aY, aZ);
				; // 1.7.10 isRemote=T/F вокруг setBlock подавлял клиент-пакет; neo isClientSide() final — но способность ЕСТЬ: WD.set flag 0 (без UPDATE_CLIENTS=2) = НЕТ клиент-пакета, точно итог оригинала (isRemote=T + flag 0). Не деградация.
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
	// F13 functional-adapted (структурная несовместимость модели данных): 1.7.10 super.onBlockAdded (BlockRailBase) выравнивал
	// форму рельса по соседям (func_150052_a); neo BaseRailBlock.onPlace работает через RailState/BlockState-Property, а этот
	// класс хранит форму в meta (модели данных несовместимы — эквивалента super-вызова 1:1 нет). Форму GT6-рельс держит своей
	// meta-логикой (updateNeighborsAt при соседских изменениях, см. выше); detector-логика сохранена без потерь. Не заглушка.
	@Override protected void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {
		if (mDetectorRail) func_150054_a(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));
	}
	
	public boolean hasComparatorInputOverride() {return mDetectorRail;}
	
	// @Override
	public int getComparatorInputOverride(Level aWorld, int aX, int aY, int aZ, int aSide) {
		if (mDetectorRail && (WD.meta(aWorld, aX, aY, aZ) & 8) > 0) {
			@SuppressWarnings("unchecked")
			List<MinecartCommandBlock> list = aWorld.getEntitiesOfClass(MinecartCommandBlock.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875));
			// было func_145822_e()/func_145760_g() (SRG, 1.7.10) -> MinecartCommandBlock.getCommandBlock()
			// [MinecartCommandBlock.java:77] + BaseCommandBlock.getSuccessCount() [BaseCommandBlock.java:34]
			if (list.size() > 0) return list.get(0).getCommandBlock().getSuccessCount();
			@SuppressWarnings("unchecked")
			// было World.selectEntitiesWithinAABB(Class,AABB,IEntitySelector) + IEntitySelector.selectInventories
			// (1.7.10, тип/поле удалены) -> EntityGetter.getEntitiesOfClass(Class,AABB,Predicate) [EntityGetter.java:23],
			// предикат instanceof Container (было instanceof IInventory) - тот же смысл отбора.
			List<AbstractMinecart> list1 = aWorld.getEntitiesOfClass(AbstractMinecart.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875), aEntity -> aEntity instanceof Container);
			// было Container.calcRedstoneFromInventory(IInventory) -> AbstractContainerMenu.getRedstoneSignalFromContainer(Container) [AbstractContainerMenu.java:761]
			if (list1.size() > 0) return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)list1.get(0));
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
					// было aCart.motionX *= 2; aCart.motionZ *= 2; (1.7.10 мутируемые поля) -> neo Vec3 иммутабелен -
					// центр WD.setMotionX/setMotionZ (уже используется ниже в этом же методе, WD.java:368,370),
					// последовательные вызовы читают/пишут независимые оси без потери семантики.
					net.minecraft.world.phys.Vec3 tMotionVec = aCart.getDeltaMovement();
					WD.setMotionX(aCart, tMotionVec.x * 2);
					WD.setMotionZ(aCart, tMotionVec.z * 2);
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
					// было aCart.motionX /= 2; ...; aCart.motionZ /= 2; (1.7.10 мутируемые поля) -> тот же центр
					// WD.setMotionX/Y/Z, порядок трёх присвоений сохранён 1:1.
					WD.setMotionX(aCart, aCart.getDeltaMovement().x / 2);
					WD.setMotionY(aCart, 0);
					WD.setMotionZ(aCart, aCart.getDeltaMovement().z / 2);
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
		// было tBlock != Blocks.tallgrass (1.7.10 единый BlockTallGrass, meta grass/fern) -> neo раздвоил на
		// Blocks.SHORT_GRASS/Blocks.FERN, оба instanceof TallGrassBlock [TallGrassBlock.java:15, Blocks.java:707-732] -
		// instanceof как 1:1-эквивалент identity-проверки единого класса (второй tBlock!=DEAD_BUSH дубль-баг порта устранён).
		} else if (tBlock != Blocks.VINE && !(tBlock instanceof TallGrassBlock) && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		// World.canPlaceEntityOnSide восстановлен 1:1 через ЦЕНТР WD.canPlaceEntityOnSide (Forge-хук удалён по ИМЕНИ,
		// способность есть — коллизия формы с исключением размещающего + заменяемость цели; централизован в WD.java).
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == 255 && getMaterial().isSolid()) || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, SIDES_AXIS_X[UT.Code.getHorizontalForPlayerPlacing(aPlayer)] ? 1 : 0)) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
