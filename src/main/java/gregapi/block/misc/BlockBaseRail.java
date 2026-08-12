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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
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
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
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
public class BlockBaseRail extends BaseRailBlock implements IBlockBase, IBlockSealable, IBlockToolable, gregapi.block.IBlockExtendedMetaData {
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
	@Override public float[] getRenderBounds() {return mRenderBounds;}

	// BUG-047 МОСТ МЕТЫ (мета ↔ BlockState): GT6-код рельса весь ходит через WD.meta/WD.set (getIcon/onToolClick/
	// детектор/скорость/буст/RailRenderer), а neo BaseRailBlock — через BlockState-Property (getShapeProperty,
	// BaseRailBlock.java:152). Носителем меты делается САМ state: SHAPE (форма; порядок RailShape.java:6-15 ТОЧНО
	// равен числовой мете 1.7.10: 0=NS,1=EW,2-5=подъёмы,6-9=углы) + POWERED (бит 8: питание booster/detector либо
	// вариант разметки BlockRailRoad) — мост IBlockExtendedMetaData ниже. createBlockStateDefinition вызывается
	// ВНУТРИ Block-конструктора [Block.java:235-239] ДО инициализации полей mPowerRail/mDetectorRail (порядок
	// super->createBlockStateDefinition->поля подкласса) - per-instance выбор STRAIGHT(6-знач., PoweredRailBlock.SHAPE)/
	// CURVED(10-знач., RailBlock.SHAPE) недостижим на этом этапе. Используем безусловно более широкий RailBlock.SHAPE
	// [RailBlock.java:16] (10 значений, надмножество) для ВСЕХ вариантов — у straight-рельсов лишние углы недостижимы
	// выравниванием (RailState гейтит !isStraight, RailState.java:163-179,247-263), но ПРЕДСТАВИМЫ (1:1 с квирком
	// оригинала: крошбар straight-рельса циклит мету %10, включая «мусорные» 6/7). POWERED регистрируется у ВСЕХ
	// вариантов по той же причине (у flexible-рельсов вестигиален, всегда false). WATERLOGGED [BaseRailBlock.java:28]
	// обязателен (getFluidState/updateShape читают его из state).
	private static final Property<RailShape> SHAPE_PROPERTY = RailBlock.SHAPE;
	/** Канон DetectorRailBlock.java:32 (BlockStateProperties.POWERED — тот же property, что у vanilla powered/detector). */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;
	@Override public Property<RailShape> getShapeProperty() {return SHAPE_PROPERTY;}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {builder.add(SHAPE_PROPERTY, POWERED, WATERLOGGED);} // тройка — канон DetectorRailBlock.java:190
	// F16 impossible-1:1 (1.7.10 не имел codec-регистрации; neo codec — не data-driven для этого класса): 1.7.10 не имел codec-based регистрации (класс отсутствовал как
	// override-точка) - neo Block.codec() [Block.java:126-129] переабстрагирован BaseRailBlock.codec()
	// [BaseRailBlock.java:47], требует MapCodec<? extends BaseRailBlock>; GT6 регистрирует блоки процедурно
	// (ST.register, много-аргументный конструктор), несовместимо с simpleCodec(Function<Properties,B>)
	// [BlockBehaviour.java:127] (однопараметрический). MapCodec.unit(...) - тот же приём, что F16-decision
	// (decisions/F16-block-codec.md) уже утвердил для процедурно регистрируемых GT6-блоков (не участвует в
	// реальной (де)сериализации), живой neo-пример использования — MapCodec.unit(Supplier) [EmptyModel.java:33].
	@Override public MapCodec<? extends BaseRailBlock> codec() {return MapCodec.unit(() -> this);}

	// ------------------------------------------------------------------------------------------------------------
	// BUG-047 мост IBlockExtendedMetaData: мета 1.7.10 ↔ SHAPE+POWERED. Раскладка 1:1 с оригиналом:
	// straight (mPowerRail||mDetectorRail, вкл. BlockRailRoad): биты 0-2 = форма (RailShape.ordinal 0-7 — включая
	//   «мусорные» углы 6/7 из крошбар-цикла %10, представимо), бит 8 = POWERED (питание либо вариант разметки рода).
	//   Биекция на всех метах 0-15.
	// flexible (обычный рельс): мета = форма 0-9 (RailShape.ordinal), POWERED вестигиален (false). Меты 10-15
	//   в neo непредставимы (RailShape кончается на 9) → кламп к 9; у Грега это была мусор-мета от крошбара
	//   на угле 9 (формула даёт 10) — деградация той же степени, но без выхода за enum.
	// ------------------------------------------------------------------------------------------------------------
	@Override public short getExtendedMetaData(BlockState aState) {
		int tShape = aState.getValue(SHAPE_PROPERTY).ordinal();
		if (mPowerRail || mDetectorRail) return (short)((tShape & 7) | (aState.getValue(POWERED) ? 8 : 0));
		return (short)tShape;
	}
	@Override public BlockState getStateForExtendedMetaData(BlockState aBase, short aMetaData) {
		int tMeta = aMetaData & 15;
		if (mPowerRail || mDetectorRail) return aBase.setValue(SHAPE_PROPERTY, RailShape.values()[tMeta & 7]).setValue(POWERED, (tMeta & 8) != 0);
		return aBase.setValue(SHAPE_PROPERTY, RailShape.values()[Math.min(tMeta, 9)]).setValue(POWERED, false);
	}
	@Override public short getExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockState tState = aWorld.getBlockState(new BlockPos(aX, aY, aZ));
		return tState.getBlock() == this ? getExtendedMetaData(tState) : 0;
	}
	// Зеркало приёма BlockBaseMeta.setExtendedMetaData:64-71 (прямые вызыватели вне WD.set; WD.set идёт атомарным путём).
	@Override public void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockState tState = aWorld.getBlockState(tPos);
		if (tState.getBlock() != this) return;
		BlockState tNew = getStateForExtendedMetaData(tState, aMetaData);
		if (aWorld instanceof net.minecraft.world.level.LevelAccessor tLA) tLA.setBlock(tPos, tNew, 3);
		else if (aWorld instanceof net.minecraft.world.level.chunk.ChunkAccess tChunk) tChunk.setBlockState(tPos, tNew, Block.UPDATE_ALL);
	}

	/** @param aSpeed is usually 0.4F */
	public BlockBaseRail(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, String aLocalName, boolean aPowerRail, boolean aDetectorRail, float aSpeed, float aExplosionResistance, int aHarvestLevel, IIconContainer aIconPrimary, IIconContainer aIconSecondary) {
		// F16/F9 форс движка: neo BaseRailBlock(boolean,Properties) требует Properties [BaseRailBlock.java:41] -
		// тот же Properties.of()-дефолт, что BlockBase уже использует (F9-мост твёрдости/материала отложен туда же).
		// F12-followup (block-split): setId в Properties (иначе «Block id not set»); namespace=GAPI (совпадает с реестром,
		// куда ST.register клал блок), ключ санитизирован. aNameInternal (поле ещё не присвоено на этой строке).
		// BUG-047: noCollision() — 1:1 vanilla-рельс (Blocks.java:1548) и 1.7.10 BlockRailBase.getCollisionBoundingBoxFromPool=null;
		// без него рельс порта был твёрдой 2px-плитой (коллизил вагонетку/игрока).
		super(aPowerRail || aDetectorRail, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollision());
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
		// 1:1 vanilla RailBlock (RailBlock.java:25) / DetectorRailBlock.java:46: дефолт NS + не-powered + сухой
		// (сырой дефолт boolean-property = true — без registerDefaultState рельс ставился waterlogged/powered).
		// Вода при УСТАНОВКЕ В воду ставится отдельно в onItemUse (BUG-047, прецедент слэбов BUG-010).
		registerDefaultState(this.stateDefinition.any().setValue(SHAPE_PROPERTY, RailShape.NORTH_SOUTH).setValue(POWERED, false).setValue(WATERLOGGED, false));
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
	// F-occlusion мост (тот же приём, что BlockBase — рельс вне той иерархии, extends BaseRailBlock):
	// не-opaque → occlusion-форма пуста (сосед не вырезается) + свет проходит.
	@Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(net.minecraft.world.level.block.state.BlockState aState) {
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}
	@Override public boolean propagatesSkylightDown(net.minecraft.world.level.block.state.BlockState aState) {return true;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	public int damageDropped(int aMeta) {return 0;}
	public int quantityDropped(Random par1Random) {return 1;}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return 0;}
	public int getLightOpacity() {return LIGHT_OPACITY_NONE;}

	// F3 light-opacity МОСТ (рельсы наследуют ванильный BaseRailBlock, а не BlockBase — свой мост, см. разбор там).
	@Override protected int getLightBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.lightDampening(getLightOpacity());}

	// F3 shade МОСТ (рельсы наследуют ванильный BaseRailBlock, а не BlockBase — свой мост, см. разбор там).
	@Override public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** 1.7.10 {@code Block.isBlockNormalCube()} ({@code Block.java:502-504}) — тело 1:1, см. {@code BlockBase}. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}
	public Item getItemDropped(int par1, Random par2Random, int par3) {return Item.byBlock(this);}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(Object aIconRegister) {/**/}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {return canCreatureSpawn(WD.meta(aWorld, aX, aY, aZ));}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab par2CreativeTabs, @SuppressWarnings("rawtypes") List aList) {aList.add(ST.make(aItem, 1, 0));}
	public ResourceLocation getIcon(int aSide, int aMeta) {return ((mPowerRail||mDetectorRail?(aMeta&8)!=0:aMeta>=6)?mIconSecondary:mIconPrimary).getIcon(0);}
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
				aWorld.updateNeighborsAt(new BlockPos(aX, aY - 1, aZ), this);
				if (aData == 2 || aData == 3 || aData == 4 || aData == 5) {
					aWorld.updateNeighborsAt(new BlockPos(aX, aY + 1, aZ), this);
				}
			}
		}
	}
	
	public int tickRate(Level aWorld) {return 20;}
	// было canProvidePower() -> BlockBehaviour.isSignalSource(BlockState) [BlockBehaviour.java:218]
	@Override public boolean isSignalSource(BlockState aState) {return mDetectorRail;}

	// было onEntityCollidedWithBlock(World,x,y,z,Entity) -> BlockBehaviour.entityInside(BlockState,Level,BlockPos,Entity) [BlockBehaviour.java:393]
	@Override public void entityInside(BlockState aState, Level aWorld, BlockPos aPos, Entity aEntity) {
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
	@Override public int getSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return mDetectorRail ? (WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) & 8) != 0 ? 15 : 0 : 0;}
	// было isProvidingStrongPower(IBlockAccess,x,y,z,side) -> BlockBehaviour.getDirectSignal(BlockState,BlockGetter,BlockPos,Direction) [BlockBehaviour.java:363]
	@Override public int getDirectSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return mDetectorRail ? (WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) & 8) == 0 ? 0 : (aSide == Direction.UP ? 15 : 0) : 0;}
	
	private void func_150054_a(Level aWorld, int aX, int aY, int aZ, int aMetaData) {
		boolean flag = (aMetaData & 8) != 0;
		boolean flag1 = F;
		@SuppressWarnings("unchecked")
		List<AbstractMinecart> list = aWorld.getEntitiesOfClass(AbstractMinecart.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875));
		
		if (!list.isEmpty()) flag1 = T;
		if (flag1 && !flag) {
			WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMetaData | 8, 3, F);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY - 1, aZ), this);
			// было World.markBlockRangeForRenderUpdate(x0,y0,z0,x1,y1,z1) -> Level.setBlocksDirty(BlockPos,BlockState,BlockState)
			// [Level.java:335, реальный neo-приём для detector-rail - см. DetectorRailBlock.checkPressed]; GT6 не отслеживает
			// раздельно old/new BlockState (meta не проецирована на реальный BlockState, F13-модель меты) - тот же приём,
			// что уже принят в WD.update (old==new).
			{BlockPos tPos = new BlockPos(aX, aY, aZ); BlockState tState = aWorld.getBlockState(tPos); aWorld.setBlocksDirty(tPos, tState, tState);}
		}
		if (!flag1 && flag) {
			WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMetaData & 7, 3, F);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY - 1, aZ), this);
			{BlockPos tPos = new BlockPos(aX, aY, aZ); BlockState tState = aWorld.getBlockState(tPos); aWorld.setBlocksDirty(tPos, tState, tState);}
		}
		if (flag1) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate(aWorld));
		// было World.func_147453_f(x,y,z,Block) -> Level.updateNeighborsAt(BlockPos, Block) [Level.java:338];
		// тот же приём, что уже используется в этом файле выше (updateNeighborsAt(..., this)).
		aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this);
	}
	
	// было onBlockAdded(World,x,y,z) -> BlockBehaviour.onPlace(BlockState,Level,BlockPos,BlockState,boolean) [BlockBehaviour.java:167].
	// BUG-047: 1:1 с оригиналом (BlockBaseRail 1.7.10:257-260) — super.onBlockAdded (выравнивание формы по соседям +
	// для straight стартовый расчёт питания) БЫЛ ВЫБРОШЕН портом; с мостом меты (SHAPE = носитель) neo-эквивалент
	// работает: updateState → updateDir(RailState, first=true) + для isStraight neighborChanged на себя
	// (BaseRailBlock.java:64-77 ≡ 1.7.10 onBlockAdded: func_150052_a + if(field_150053_a) onNeighborBlockChange).
	// Гард !oldState.is(...) — vanilla (мета-запись того же блока выравнивание НЕ перезапускает, 1:1 Chunk-семантика).
	// Детектор-надстройка — 1:1 хвост оригинала. BlockRailRoad переопределяет NO-OP (разметка не выравнивается).
	@Override public void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {
		if (!aOldState.is(aState.getBlock())) updateState(aState, aWorld, aPos, aMovedByPiston);
		if (mDetectorRail) func_150054_a(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));
	}

	// BUG-047: neo-хук BaseRailBlock.updateState(BlockState,Level,BlockPos,Block) [BaseRailBlock.java:111-112, пустой в базе]
	// = ТОЧКА вызова 1.7.10 func_150048_a из onNeighborBlockChange (расчёт бита питания power-рельса от редстоуна/цепочки).
	// Порт нёс func_150048_a мёртвым (без вызывателя) — мост восстанавливает канал. Аргументы 1:1 vanilla 1.7.10:
	// aMeta = полная мета, aData = field_150053_a(straight) ? meta&7 : meta.
	@Override protected void updateState(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock) {
		int tMeta = WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
		func_150048_a(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), tMeta, (mPowerRail || mDetectorRail) ? tMeta & 7 : tMeta, aBlock);
	}

	// BUG-047: neo scheduled-tick канал = tick(BlockState,ServerLevel,BlockPos,RandomSource) — приём BlockBase:310-312
	// (updateTick без моста был сиротой → детектор никогда не гас: scheduleTick из func_150054_a бил в неперекрытый tick()).
	@Override public void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), UT.Code.random(aRandom)); // конвертер — ЦЕНТР UT.Code.random
	}

	// BUG-047: было hasComparatorInputOverride/getComparatorInputOverride (1.7.10) → neo hasAnalogOutputSignal/
	// getAnalogOutputSignal (канон DetectorRailBlock.java:142,147; сигнатура с Direction — тело GT6 сторону игнорирует, 1:1).
	@Override public boolean hasAnalogOutputSignal(BlockState aState) {return hasComparatorInputOverride();}
	@Override public int getAnalogOutputSignal(BlockState aState, Level aWorld, BlockPos aPos) {return getComparatorInputOverride(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), 0);}

	// BUG-047 F12/F9-hardness: рельс ВНЕ BlockBase-иерархии → мост getDestroyProgress локально (зеркало
	// MultiTileEntityBlockInternal:172-176; формула — ЦЕНТР WD.destroyProgress). Без моста Properties.destroyTime=0 →
	// мгновенный слом рукой (getBlockHardness был мёртв).
	@Override public float getDestroyProgress(BlockState aState, Player aPlayer, BlockGetter aWorld, BlockPos aPos) {
		if (!(aWorld instanceof Level tLevel)) return super.getDestroyProgress(aState, aPlayer, aWorld, aPos);
		return WD.destroyProgress(getBlockHardness(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()), aPlayer, aState, aWorld, aPos);
	}

	// BUG-047 дроп-мост (класс BUG-006, зеркало BlockBase.getDrops:215-231 — рельс вне той иерархии, loot-таблицы нет →
	// neo-дефолт дропал ПУСТО, в т.ч. при потере опоры через vanilla neighborChanged→dropResources). 1.7.10-семантика
	// рельса: quantityDropped=1 × getItemDropped=сам блок × damageDropped=0. Гейт взрыва — тот же шов BUG-024.
	@Override public List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList(); // гейт взрыва — ЦЕНТР (BUG-024)
		return java.util.Collections.singletonList(ST.make(this, 1, 0));
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
	
	/** [BUG-047, метка отложенности F-hook-removed СНЯТА 2026-08-06] Per-rail максимум скорости В КАНАЛЕ ДВИЖКА.
	 *  1.7.10: {@code EntityMinecart:373-374} — {@code maxSpeed = min(rail.getRailMaxSpeed(...),
	 *  getCurrentCartSpeedCapOnRail())}, капа минкарта {@code getMaxCartSpeedOnRail() = 1.2f}
	 *  ({@code EntityMinecart:1335}, инициализация {@code currentSpeedRail} — {@code :61}); водной ветки не было
	 *  (вода-физика минкартов — движок 1.13+). neo 26.1.2: величина захардкожена
	 *  {@code OldMinecartBehavior.getMaxSpeed:410-411} (вода 0.2 / суша 0.4) и читается клампом смещения
	 *  {@code moveAlongTrack:208-211} ДО любого события — потому пост-мост {@code GT_API_Proxy.onMinecartPassBridge}
	 *  мог только резать вниз (медленные Al 0.2/Bronze 0.3), а Ti 1.2 был недостижим. Восстановление 1:1 —
	 *  подкласс поведения отвечает движку per-rail величиной в ЕГО ЖЕ канале (второй хардкод того же рода —
	 *  {@code getKnownMovement:403-407}, кламп ±0.4 для производных систем: снаряды/поводок/частицы — перекрыт
	 *  той же величиной). Подмену поля {@code AbstractMinecart.behavior} держит
	 *  {@code GT_API_Proxy.onMinecartJoinBridge} — единственная точка на весь мод. */
	public static final class GT6MinecartBehavior extends net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior {
		/** Капа минкарта 1.7.10: {@code EntityMinecart.getMaxCartSpeedOnRail() = 1.2f} ({@code :1335}) —
		 *  непреодолима и в оригинале: Adamantium-рельс 4.0 давал минкарту максимум 1.2. */
		private static final double CART_SPEED_CAP = 1.2;
		public GT6MinecartBehavior(AbstractMinecart aCart) {super(aCart);}
		/** GT6-рельс под минкартом → его {@link BlockBaseRail#getRailMaxSpeed}; иначе −1 (не наш случай). */
		private float railMax() {
			BlockPos tPos = minecart.getCurrentBlockPosOrRailBelow();
			if (WD.block(minecart.level(), tPos.getX(), tPos.getY(), tPos.getZ()) instanceof BlockBaseRail tRail)
				return tRail.getRailMaxSpeed(minecart.level(), minecart, tPos.getX(), tPos.getY(), tPos.getZ());
			return -1;
		}
		@Override public double getMaxSpeed(net.minecraft.server.level.ServerLevel aLevel) {
			float tRailMax = railMax();
			return tRailMax < 0 ? super.getMaxSpeed(aLevel) : Math.min(tRailMax, CART_SPEED_CAP);
		}
		@Override public net.minecraft.world.phys.Vec3 getKnownMovement(net.minecraft.world.phys.Vec3 aMovement) {
			float tRailMax = railMax();
			if (tRailMax < 0) return super.getKnownMovement(aMovement);
			double tMax = Math.min(tRailMax, CART_SPEED_CAP);
			// 1:1 к NaN-гарду движка (OldMinecartBehavior.getKnownMovement:403-407), предел — per-rail.
			return !Double.isNaN(aMovement.x) && !Double.isNaN(aMovement.y) && !Double.isNaN(aMovement.z)
				? new net.minecraft.world.phys.Vec3(net.minecraft.util.Mth.clamp(aMovement.x, -tMax, tMax), aMovement.y, net.minecraft.util.Mth.clamp(aMovement.z, -tMax, tMax))
				: net.minecraft.world.phys.Vec3.ZERO;
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
		// Blocks.GRASS/Blocks.FERN, оба instanceof TallGrassBlock [TallGrassBlock.java:15, Blocks.java:707-732] -
		// instanceof как 1:1-эквивалент identity-проверки единого класса (второй tBlock!=DEAD_BUSH дубль-баг порта устранён).
		} else if (tBlock != Blocks.VINE && !(tBlock instanceof TallGrassBlock) && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		// World.canPlaceEntityOnSide восстановлен 1:1 через ЦЕНТР WD.canPlaceEntityOnSide (Forge-хук удалён по ИМЕНИ,
		// способность есть — коллизия формы с исключением размещающего + заменяемость цели; централизован в WD.java).
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == WD.maxY(aWorld) && getMaterial().isSolid()) /* BUG-089: было aY == 255 — верх мира через центр F6-Y-scale */ || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		// BUG-047 waterlog: замещаемая вода-источник запоминается ДО установки (сам сет её затирает) — семантика
		// vanilla getStateForPlacement (BaseRailBlock.java:138-144), приём — прецедент слэбов BUG-010 (ItemBlockMetaType:49-57).
		BlockPos tPlacePos = new BlockPos(aX, aY, aZ);
		boolean tWater = aWorld.getFluidState(tPlacePos).getType() == net.minecraft.world.level.material.Fluids.WATER;
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, SIDES_AXIS_X[UT.Code.getHorizontalForPlayerPlacing(aPlayer)] ? 1 : 0)) {
			if (tWater) WD.waterlog(aWorld, aX, aY, aZ); // приём — ЦЕНТР WD.waterlog (BUG-010/BUG-047)
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
