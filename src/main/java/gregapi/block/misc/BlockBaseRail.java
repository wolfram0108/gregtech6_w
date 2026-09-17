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
	/** The Material bridge isn't extended to classes outside BlockBase, so this keeps its own
	 *  mMaterial/getMaterial() instead of a new shared abstraction. */
	protected final Material mMaterial = Material.circuits;
	public Material getMaterial() {return mMaterial;}
	/** BlockBaseRail doesn't inherit BlockBase, but the IBlock contract still requires setBlockBounds,
	 *  so the same technique is reused locally here instead of sharing BlockBase's implementation. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	@Override public float[] getRenderBounds() {return mRenderBounds;}

	// GT6's rail code goes entirely through WD.meta/WD.set, but neo stores rail shape and power in BlockState properties
	// instead; SHAPE's order matches 1.7.10's meta exactly, and the wider property covers every variant from the start.
	private static final Property<RailShape> SHAPE_PROPERTY = RailBlock.SHAPE;
	/** The same POWERED property vanilla's own powered and detector rails use. */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;
	@Override public Property<RailShape> getShapeProperty() {return SHAPE_PROPERTY;}
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {builder.add(SHAPE_PROPERTY, POWERED, WATERLOGGED);} // matches vanilla DetectorRailBlock's own codec pattern
	// 1.20.1: BlockBehaviour has no codec() method yet, so there's nothing to override here.

	// ------------------------------------------------------------------------------------------------------------
	// Meta and SHAPE+POWERED map 1:1 with 1.7.10, including its junk crowbar-cycled corner values on straight rails;
	// flexible-rail metas 10-15, unrepresentable in the neo enum, clamp to 9, matching the original's own quirk.
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
	// Mirrors the same technique as BlockBaseMeta.setExtendedMetaData, for callers outside WD.set's atomic path.
	@Override public void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockState tState = aWorld.getBlockState(tPos);
		if (tState.getBlock() != this) return;
		BlockState tNew = getStateForExtendedMetaData(tState, aMetaData);
		if (aWorld instanceof net.minecraft.world.level.LevelAccessor tLA) tLA.setBlock(tPos, tNew, 3);
		else if (aWorld instanceof net.minecraft.world.level.chunk.ChunkAccess tChunk) tChunk.setBlockState(tPos, tNew, F);
	}

	/** @param aSpeed is usually 0.4F */
	public BlockBaseRail(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, String aLocalName, boolean aPowerRail, boolean aDetectorRail, float aSpeed, float aExplosionResistance, int aHarvestLevel, IIconContainer aIconPrimary, IIconContainer aIconSecondary) {
		// Uses the same Properties.of() default BlockBase already relies on, plus noCollision() to match both vanilla rails and
		// 1.7.10's own null collision box; without it, the ported rail collided with carts and players as a solid plate.
		super(aPowerRail || aDetectorRail, net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission());
		mNameInternal = aNameInternal;
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.TRANSPORT);
		// Only the BlockItem is registered here, through a supplier; the block itself registers lazily elsewhere.
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
		// Matches vanilla's own default state exactly; without setting it explicitly, a boolean
		// property's raw default of true would place every new rail already waterlogged and powered.
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
	// Actually wired to neo through BlockBase's centralized getDestroyProgress override, not a stub;
	// the rail carries vanilla rail hardness 1:1.
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.RAIL, aWorld, aX, aY, aZ);}
	// Ported losslessly through IBlockExtension's newer signature; the original body ignored every
	// parameter except this anyway, always returning the same constant.
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
	// Same occlusion bridge as BlockBase, applied here since rails sit outside that hierarchy: non-opaque means an empty
	// occlusion shape and light passing through.
	@Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos) {
		return net.minecraft.world.phys.shapes.Shapes.empty();
	}
	@Override public boolean propagatesSkylightDown(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos) {return true;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	public int damageDropped(int aMeta) {return 0;}
	public int quantityDropped(Random par1Random) {return 1;}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return 0;}
	public int getLightOpacity() {return LIGHT_OPACITY_NONE;}

	// Its own copy of the light-opacity bridge, since rails extend vanilla BaseRailBlock, not BlockBase.
	@Override public int getLightBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.lightDampening(getLightOpacity());}

	// Its own copy of the shade bridge, for the same reason: rails extend vanilla BaseRailBlock, not BlockBase.
	@Override public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** Body 1:1 with 1.7.10's Block.isBlockNormalCube; see BlockBase for the same method. */
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
				; // matches the original exactly: flag 0 suppresses the client packet, the same effect 1.7.10 got by toggling isRemote
				boolean tResult = WD.set(aWorld, aX, aY, aZ, this, WD.meta(aWorld, aX, aY, aZ) ^ 8, 0);
				;
				return tResult?10000:0;
			}
			if (aTool.equals(TOOL_crowbar)) {
				byte aMeta = WD.meta(aWorld, aX, aY, aZ);
				; // matches the original exactly: flag 0 suppresses the client packet, the same effect 1.7.10 got by toggling isRemote
				// neo's BaseRailBlock doesn't store this flag itself, so GregTech6's own mPowerRail/mDetectorRail fields carry the same
				// value instead.
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
				// neo's hasNeighborSignal replaces the old isBlockIndirectlyGettingPowered.
				if (aWorld.hasNeighborSignal(new BlockPos(aX, aY, aZ))) return T;
				return func_150058_a(aWorld, aX, aY, aZ, j1, p_150057_5_, p_150057_6_ + 1);
			}
		}
		return F;
	}
	
	// @Override
	protected void func_150048_a(Level aWorld, int aX, int aY, int aZ, int aMeta, int aData, Block aBlock) {
		if (mPowerRail) {
			// neo's hasNeighborSignal replaces the old isBlockIndirectlyGettingPowered.
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
	// neo asks this through isSignalSource instead of the old canProvidePower.
	@Override public boolean isSignalSource(BlockState aState) {return mDetectorRail;}

	// Was onEntityCollidedWithBlock(World,x,y,z,Entity) -> BlockBehaviour.entityInside(BlockState,Level,BlockPos,Entity).
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
	
	// neo asks weak redstone power through BlockBehaviour.getSignal instead of the old isProvidingWeakPower.
	@Override public int getSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return mDetectorRail ? (WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) & 8) != 0 ? 15 : 0 : 0;}
	// neo asks strong redstone power through getDirectSignal instead of the old isProvidingStrongPower.
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
			// neo's setBlocksDirty replaces the old render-update call; GregTech6 passes the same state
			// for old and new, the same technique already used in WD.update, since it doesn't track them separately.
			{BlockPos tPos = new BlockPos(aX, aY, aZ); BlockState tState = aWorld.getBlockState(tPos); aWorld.setBlocksDirty(tPos, tState, tState);}
		}
		if (!flag1 && flag) {
			WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aMetaData & 7, 3, F);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this);
			aWorld.updateNeighborsAt(new BlockPos(aX, aY - 1, aZ), this);
			{BlockPos tPos = new BlockPos(aX, aY, aZ); BlockState tState = aWorld.getBlockState(tPos); aWorld.setBlocksDirty(tPos, tState, tState);}
		}
		if (flag1) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate(aWorld));
		// Was World.func_147453_f(x,y,z,Block) -> Level.updateNeighborsAt(BlockPos,Block), the same trick
		// already used above in this file.
		aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this);
	}
	
	// Restores 1.7.10's onBlockAdded behavior (shape alignment plus initial power calc for straight rails), which the port had
	// dropped; it now works because the meta bridge makes SHAPE the real carrier neo's own updateState/updateDir already read.
	@Override public void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {
		if (!aOldState.is(aState.getBlock())) updateState(aState, aWorld, aPos, aMovedByPiston);
		if (mDetectorRail) func_150054_a(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));
	}

	// neo's updateState hook is the real call point for 1.7.10's power-bit recalculation, which the
	// port had left dead with no caller; this bridge restores it, with arguments matching vanilla exactly.
	@Override protected void updateState(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock) {
		int tMeta = WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
		func_150048_a(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), tMeta, (mPowerRail || mDetectorRail) ? tMeta & 7 : tMeta, aBlock);
	}

	// Same scheduled-tick bridge as BlockBase: without it, updateTick was an orphan and the detector rail never turned back
	// off.
	@Override public void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), UT.Code.random(aRandom)); // the RandomSource-to-Random converter lives in the single center UT.Code.random
	}

	// neo's analog-output-signal methods replace the old comparator-override pair; the Direction
	// parameter they add is ignored here, matching vanilla's own detector rail.
	@Override public boolean hasAnalogOutputSignal(BlockState aState) {return hasComparatorInputOverride();}
	@Override public int getAnalogOutputSignal(BlockState aState, Level aWorld, BlockPos aPos) {return getComparatorInputOverride(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), 0);}

	// Bridged locally since the rail sits outside the BlockBase hierarchy; without it, Properties'
	// baked destroy time was 0 and the rail broke instantly by hand.
	@Override public float getDestroyProgress(BlockState aState, Player aPlayer, BlockGetter aWorld, BlockPos aPos) {
		if (!(aWorld instanceof Level tLevel)) return super.getDestroyProgress(aState, aPlayer, aWorld, aPos);
		return WD.destroyProgress(getBlockHardness(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()), aPlayer, aState, aWorld, aPos);
	}

	// Bridged separately since the rail has no loot table and sits outside BlockBase's drop bridge;
	// without it, losing support silently dropped nothing at all, matching the same defect class as ordinary blocks.
	@Override public List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList(); // explosion-drop gating goes through the shared center
		java.util.List<ItemStack> rDrops = new java.util.ArrayList<>();
		rDrops.add(ST.make(this, 1, 0));
		net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
		if (tOrigin == null) return rDrops;
		// 1.20.1: GT6's own blocks have no loot tables, so the global GT6BlockDropsModifier can't see them --
		// drop handling is called from the same center directly instead, one rule for both paths.
		gregapi.GT_API_Proxy.processBlockDrops(rDrops, aParams.getLevel(), net.minecraft.core.BlockPos.containing(tOrigin), aState, aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY));
		return rDrops;
	}
	
	public boolean hasComparatorInputOverride() {return mDetectorRail;}
	
	// @Override
	public int getComparatorInputOverride(Level aWorld, int aX, int aY, int aZ, int aSide) {
		if (mDetectorRail && (WD.meta(aWorld, aX, aY, aZ) & 8) > 0) {
			@SuppressWarnings("unchecked")
			List<MinecartCommandBlock> list = aWorld.getEntitiesOfClass(MinecartCommandBlock.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875));
			// neo's getCommandBlock() replaces the old SRG-named methods.
			// [MinecartCommandBlock.java:77] + BaseCommandBlock.getSuccessCount() [BaseCommandBlock.java:34]
			if (list.size() > 0) return list.get(0).getCommandBlock().getSuccessCount();
			@SuppressWarnings("unchecked")
			// neo's getEntitiesOfClass with a Container predicate replaces the old selector-based entity
			// query, keeping the same selection meaning (IInventory became Container).
			List<AbstractMinecart> list1 = aWorld.getEntitiesOfClass(AbstractMinecart.class, new AABB(aX + 0.125, aY, aZ + 0.125, aX + 0.875, aY + 0.875, aZ + 0.875), aEntity -> aEntity instanceof Container);
			// neo's getRedstoneSignalFromContainer replaces the old calcRedstoneFromInventory.
			if (list1.size() > 0) return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)list1.get(0));
		}
		return 0;
	}
	
	// neo's isAreaLoaded replaces the old doChunksNearChunkExist.
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
	
	/** Per-rail max speed now comes through the engine's own channel, the same way as 1.7.10: the minecart
	 *  asks the rail block directly, so no cart-behavior subclass or field substitution is needed here at all. */
	@Override public float getRailMaxSpeed(net.minecraft.world.level.block.state.BlockState aState, Level aWorld, BlockPos aPos, AbstractMinecart aCart) {
		return getRailMaxSpeed(aWorld, aCart, aPos.getX(), aPos.getY(), aPos.getZ());
	}

	/** Minecart pass-through is the same 1.7.10 engine channel verbatim: IForgeBaseRailBlock.onMinecartPass. */
	@Override public void onMinecartPass(net.minecraft.world.level.block.state.BlockState aState, Level aWorld, BlockPos aPos, AbstractMinecart aCart) {
		onMinecartPass(aWorld, aCart, aPos.getX(), aPos.getY(), aPos.getZ());
	}

	// GT6's own 1.7.10 form, body verbatim; the mutable motionX/Y/Z fields are replaced by the center
	// WD.setMotionX/Y/Z, as everywhere else in the port.
	public void onMinecartPass(Level aWorld, AbstractMinecart aCart, int aX, int aY, int aZ) {
		if (mPowerRail) {
			byte tRailMeta = WD.meta(aWorld, aX, aY, aZ);
			double tMotion = Math.sqrt(WD.motionX(aCart)*WD.motionX(aCart) + WD.motionZ(aCart)*WD.motionZ(aCart));
			if ((tRailMeta & 8) != 0) {
				if (tMotion > 0.01) {
					WD.setMotionX(aCart, WD.motionX(aCart) * 2);
					WD.setMotionZ(aCart, WD.motionZ(aCart) * 2);
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
					WD.setMotionX(aCart, WD.motionX(aCart) / 2);
					WD.setMotionY(aCart, 0);
					WD.setMotionZ(aCart, WD.motionZ(aCart) / 2);
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
		// Was tBlock != Blocks.tallgrass (one 1.7.10 class for grass/fern); neo split it into GRASS/FERN, both
		// instanceof TallGrassBlock -- the same identity check, expressed by type instead of by a single constant.
		} else if (tBlock != Blocks.VINE && !(tBlock instanceof TallGrassBlock) && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		// Restored through the same shared center WD.canPlaceEntityOnSide as the rest of the block hierarchies.
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == WD.maxY(aWorld) && getMaterial().isSolid()) /* the world ceiling now comes from the single Y-scale center, not a hardcoded 255 */ || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		// The water being replaced is snapshotted before placement happens, since the placement itself
		// would overwrite it, matching vanilla's own getStateForPlacement semantics and the same technique used for slabs.
		BlockPos tPlacePos = new BlockPos(aX, aY, aZ);
		boolean tWater = aWorld.getFluidState(tPlacePos).getType() == net.minecraft.world.level.material.Fluids.WATER;
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, SIDES_AXIS_X[UT.Code.getHorizontalForPlayerPlacing(aPlayer)] ? 1 : 0)) {
			if (tWater) WD.waterlog(aWorld, aX, aY, aZ); // the same shared WD.waterlog center used for slabs
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
