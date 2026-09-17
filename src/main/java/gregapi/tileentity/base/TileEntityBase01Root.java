/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregapi.tileentity.base;
import gregapi.code.ItemNBT;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import gregapi.fluid.FluidTankInfo;

import gregapi.api.Optional;
import net.minecraftforge.api.distmarker.Dist;
import gregapi.block.multitileentity.IMultiTileEntity;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetLightValue;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_IsProvidingStrongPower;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.TagData;
import gregapi.data.FL;
import gregapi.data.TD;
import gregapi.gui.ContainerCommon;
import gregapi.gui.GT6MenuProvider;
import gregapi.gui.Slot_Base;
import gregapi.network.packets.PacketBlockError;
import gregapi.network.packets.PacketBlockEvent;
import gregapi.random.ExplosionGT;
import gregapi.render.BlockTextureCopied;
import gregapi.render.IRenderedBlockObject;
import gregapi.render.IRenderedBlockObject.ErrorRenderer;
import gregapi.render.ITexture;
import gregapi.render.RenderHelper;
import gregapi.tileentity.ITileEntity;
import gregapi.tileentity.ITileEntityAdjacentInventoryUpdatable;
import gregapi.tileentity.ITileEntityGUI;
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityCanDelegate;
import gregapi.tileentity.delegate.ITileEntityDelegating;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.multiblocks.MultiTileEntityMultiBlockPart;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyTile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * The Functions all TileEntities should have.
 */
// AE2's IMovableTile interface doesn't exist on the 1.20.1 branch of AE2 (its package holds different classes now).
// Loading the real AE2 jar alongside a mirror package would put appeng.* in two class sources at once, so it's dropped.
public abstract class TileEntityBase01Root extends BlockEntity implements ITileEntity, ITileEntityGUI {
	/** If this TileEntity checks for the Chunk to be loaded before returning World based values. If this is set to T, this TileEntity will not cause worfin' Chunks, uhh I mean orphan Chunks. */
	public boolean mIgnoreUnloadedChunks = T;
	
	/** This Variable checks if this TileEntity is dead, because Minecraft is too stupid to have proper TileEntity unloading. */
	public boolean mIsDead = F;
	
	/** This Variable checks if this TileEntity should refresh when the Block is being set. That way you can turn this check off any time you need it. */
	public boolean mShouldRefresh = T;
	
	/** This Variable is for a buffered Block Update. */
	public boolean mDoesBlockUpdate = F;
	
	/** This Variable is for the IC2 E-net. */
	public boolean mIsAddedToEnet = F, mDoEnetCheck = T;
	
	/** This Variable is for forcing the Selection Box to be full. */
	public boolean FORCE_FULL_SELECTION_BOXES = F;

	/** Client-only clip box for the last built frame; 1.7.10 had no such question since MTE geometry lived in the
	 *  chunk mesh, clipped whole by its 16-cube section. Purely visual, not saved or synced. */
	public net.minecraft.world.phys.AABB mRenderAABB = null;

	/** Client-only quad cache for the BER; 1.7.10's MTE geometry lived in the section mesh and only rebuilt on
	 *  markBlockForUpdate, so this cache restores that same granularity instead of rebuilding every frame. */
	public java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> mQuadCache = null;
	public long mQuadCacheEpoch = Long.MIN_VALUE;
	/** Second half of cache validity, alongside the render epoch: setSectionDirty only stamps the section now.
	 *  Long.MIN_VALUE means no cache was ever built (a never-marked section's real stamp is 0). */
	public long mQuadCacheSectionStamp = Long.MIN_VALUE;

	/** The engine now asks the BE itself for its render clip box (getRenderBoundingBox), checked before drawing.
	 *  GT6 boxes are computed at runtime, so this uses last frame's actual geometry and stays unclipped until known. */
	@Override
	public net.minecraft.world.phys.AABB getRenderBoundingBox() {
		net.minecraft.world.phys.AABB rBox = mRenderAABB;
		return rBox == null ? INFINITE_EXTENT_AABB : rBox;
	}

	/** If this TileEntity is ticking at all */
	public final boolean mIsTicking;
	
	// neo's BlockPos is immutable, so the old ChunkCoordinates cache-holder is gone; getCoords() below
	// returns getBlockPos() directly, which already is the BE's position.

	// neo's BlockEntity has no no-arg constructor, but GT6's 32000 MTE variants share one dynamic type built by
	// reflection; this placeholder type only needs a non-null constructor argument, not per-variant identity.
	public static BlockEntityType<TileEntityBase01Root> MTE_TYPE;
	/** The engine only lets getTicker(level,state,type) decide ticking, with no BE instance and one shared BlockState
	 *  for all MTEs, so a second BlockEntityType, chosen by a final field at construction, is the only lever available. */
	public static BlockEntityType<TileEntityBase01Root> MTE_TYPE_NOTICK;
	// neo rejects an empty varargs of valid blocks; the placeholder has none on purpose, since its
	// canonical instances are built by reflection, not placed by the engine.
	@SuppressWarnings({"unchecked", "rawtypes"})
	// World-load reconstructs a TE from NBT through this same supplier; ore blocks get their TE class
	// straight from the block, other MTE need the stub below to await reconstruction.
	/** Named, not anonymous: an anonymous subclass copies the parent constructor's obfuscated parameter
	 *  names, which collide and fail to compile on a clean build. */
	private static final class MTEBlockEntityType extends BlockEntityType<TileEntityBase01Root> {
		MTEBlockEntityType(BlockEntityType.BlockEntitySupplier<TileEntityBase01Root> aSupplier, com.mojang.datafixers.types.Type<?> aType) {super(aSupplier, java.util.Set.<Block>of(), aType);}
		// The placeholder type has no valid blocks, so neo's default isValid would reject every state;
		// overriding it to true lets each TE decide its own validity instead.
		@Override public boolean isValid(net.minecraft.world.level.block.state.BlockState aState) {return true;}
	}
	// One factory serves both types, since they differ only in whether they tick, not in how the object is built.
	@SuppressWarnings("unchecked")
	private static final BlockEntityType.BlockEntitySupplier<TileEntityBase01Root> FACTORY =
		(BlockEntityType.BlockEntitySupplier<TileEntityBase01Root>)(aPos, aState) -> aState.getBlock() instanceof gregapi.block.prefixblock.PrefixBlock ? new gregapi.block.prefixblock.PrefixBlockTileEntity(aPos, aState) : new TileEntityLoaderStub(aPos, aState);
	public static BlockEntityType<TileEntityBase01Root> createType() {return MTE_TYPE = new MTEBlockEntityType(FACTORY, (com.mojang.datafixers.types.Type<?>)null);}
	/** Type for the non-ticking half of the hierarchy, registered alongside the main one. */
	public static BlockEntityType<TileEntityBase01Root> createTypeNoTick() {return MTE_TYPE_NOTICK = new MTEBlockEntityType(FACTORY, (com.mojang.datafixers.types.Type<?>)null);}

	// neo's BlockEntity position is immutable and set only by the super constructor, but the whole MTE hierarchy
	// inherits no-arg constructors; a thread-local carries the real position across the reflection-based construction instead.
	public static final ThreadLocal<BlockPos> PENDING_WORLD_POS = new ThreadLocal<>();
	private static BlockPos pendingPosOrZero() {BlockPos p = PENDING_WORLD_POS.get(); return p != null ? p : BlockPos.ZERO;}

	/** Single point choosing the block-entity type by its ticking flag. */
	private static BlockEntityType<TileEntityBase01Root> typeOf(boolean aIsTicking) {return aIsTicking ? MTE_TYPE : MTE_TYPE_NOTICK;}

	public TileEntityBase01Root(boolean aIsTicking) {
		super(typeOf(aIsTicking), pendingPosOrZero(), Blocks.AIR.defaultBlockState());
		mIsTicking = aIsTicking;
	}

	// Constructor with a real position, for manually-placed GT6 TE (worldgen ore); without it the TE
	// ended up cached at (0,0,0) and the engine rejected the placement.
	public TileEntityBase01Root(boolean aIsTicking, BlockPos aPos) {
		super(typeOf(aIsTicking), aPos, Blocks.AIR.defaultBlockState());
		mIsTicking = aIsTicking;
	}

	// Constructor with the real placed BlockState too, so the engine's cached state matches and doesn't
	// need to self-correct with a mismatch warning.
	public TileEntityBase01Root(boolean aIsTicking, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(typeOf(aIsTicking), aPos, aState);
		mIsTicking = aIsTicking;
	}

	// The placeholder type has no valid blocks, so a canonical instance built with an AIR state would
	// otherwise fail neo's state validation; GT6's dynamic type system overrides it.
	public boolean isValidBlockState(net.minecraft.world.level.block.state.BlockState aState) {return true;}
	
	@Override
	public void onTileEntityPlaced() {
		//
	}
	
	@Override
	public void onAdjacentBlockChange(int aTileX, int aTileY, int aTileZ) {
		//
	}
	
	// @Override
	public void readFromNBT(CompoundTag aNBT) {
		// load ID and Coords
		// neo's BlockEntity position is final, set by the engine before loadAdditional runs, unlike 1.7.10 reading it from NBT.
		// make sure Y is not negative because this causes crashes.
		if (WD.tileYInvalid(getLevel(), getBlockPos().getY())) WD.invalidateTileEntityWithNegativeYCoord(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), this); // the world floor is getMinY(), not 0, since bedrock legitimately sits below Y=0 in this engine
	}

	// @Override
	public void writeToNBT(CompoundTag aNBT) {
		// make sure Y is not negative because this causes crashes.
		if (WD.tileYInvalid(getLevel(), getBlockPos().getY())) WD.invalidateTileEntityWithNegativeYCoord(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), this); // the world floor is getMinY(), not 0, since bedrock legitimately sits below Y=0 in this engine
		// save ID and Coords
		aNBT.putString("id", getTileEntityName());
		aNBT.putInt("x", getBlockPos().getX());
		aNBT.putInt("y", getBlockPos().getY());
		aNBT.putInt("z", getBlockPos().getZ());
	}

	/** 1.20.1 still calls saveAdditional/load(CompoundTag), exactly like 1.7.10's writeToNBT/readFromNBT, so GT6's own
	 *  NBT chain plugs in directly; super runs first to preserve Forge's own data, matching AE2's base block entity. */
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		writeToNBT(aNBT);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		readFromNBT(aNBT);
	}

	// The base class returns an empty update tag, so the client's chunk packet carries no identity for the BE and it
	// loads as an untyped stub; this sends just the two identity fields, not machine state, avoiding a data leak.
	@Override public CompoundTag getUpdateTag() {
		CompoundTag rNBT = super.getUpdateTag();
		writeMTEIdentity(rNBT);
		return rNBT;
	}

	/** Only the two MTE hierarchies and the loader stub have an identity to write.
	 *  Everything else leaves the tag empty, as before. */
	protected void writeMTEIdentity(CompoundTag aNBT) {/* non-MTE subclasses carry no identity */}

	// saveCustomOnly was rejected: applying it to every BE, including machines, would bloat chunk-packet traffic
	// and leak server-only fields to the client.

	/** return the internal Name of this TileEntity to be registered. DO NOT START YOUR NAME WITH "gt."!!! */
	public abstract String getTileEntityName();
	
	@Override public void setChanged() {/* Oh no, I won't let this do anything anymore! It's only useful for Comparators and that didn't work properly anyways! */}
	@Override public Level getWorld() {return level;}
	@Override public int getX() {return getBlockPos().getX();}
	@Override public int getY() {return getBlockPos().getY();}
	@Override public int getZ() {return getBlockPos().getZ();}
	@Override public int getOffsetX (byte aSide) {return getBlockPos().getX() + OFFX[aSide];}
	@Override public int getOffsetY (byte aSide) {return getBlockPos().getY() + OFFY[aSide];}
	@Override public int getOffsetZ (byte aSide) {return getBlockPos().getZ() + OFFZ[aSide];}
	@Override public int getOffsetX (byte aSide, int aMultiplier) {return getBlockPos().getX() + OFFX[aSide] * aMultiplier;}
	@Override public int getOffsetY (byte aSide, int aMultiplier) {return getBlockPos().getY() + OFFY[aSide] * aMultiplier;}
	@Override public int getOffsetZ (byte aSide, int aMultiplier) {return getBlockPos().getZ() + OFFZ[aSide] * aMultiplier;}
	@Override public int getOffsetXN(byte aSide) {return getBlockPos().getX() - OFFX[aSide];}
	@Override public int getOffsetYN(byte aSide) {return getBlockPos().getY() - OFFY[aSide];}
	@Override public int getOffsetZN(byte aSide) {return getBlockPos().getZ() - OFFZ[aSide];}
	@Override public int getOffsetXN(byte aSide, int aMultiplier) {return getBlockPos().getX() - OFFX[aSide] * aMultiplier;}
	@Override public int getOffsetYN(byte aSide, int aMultiplier) {return getBlockPos().getY() - OFFY[aSide] * aMultiplier;}
	@Override public int getOffsetZN(byte aSide, int aMultiplier) {return getBlockPos().getZ() - OFFZ[aSide] * aMultiplier;}
	@Override public BlockPos getCoords() {return getBlockPos();}
	@Override public BlockPos getOffset (byte aSide, int aMultiplier) {return new BlockPos(getOffsetX (aSide, aMultiplier), getOffsetY (aSide, aMultiplier), getOffsetZ (aSide, aMultiplier));}
	@Override public BlockPos getOffsetN(byte aSide, int aMultiplier) {return new BlockPos(getOffsetXN(aSide, aMultiplier), getOffsetYN(aSide, aMultiplier), getOffsetZN(aSide, aMultiplier));}
	@Override public boolean isServerSide() {return level == null ? net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer() : !level.isClientSide();}
	@Override public boolean isClientSide() {return level == null ? net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient() :  level.isClientSide();}
	@Override public boolean openGUI(Player aPlayer) {return openGUI(aPlayer, 0);}
	/** The old Forge auto-dispatch for GUIs has no neo equivalent; the sole replacement is Player.openMenu(MenuProvider),
	 *  routed through {@link GT6MenuProvider} as the single MenuProvider implementation for the whole mod. */
	@Override public boolean openGUI(Player aPlayer, int aID) {
		if (aPlayer == null) return F;
		if (!(aPlayer instanceof net.minecraft.server.level.ServerPlayer tSP)) return F;
		net.minecraftforge.network.NetworkHooks.openScreen(tSP, new GT6MenuProvider(level, getBlockPos(), aID), aBuf -> {aBuf.writeBlockPos(getBlockPos()); aBuf.writeInt(aID);});
		return T;
	}
	@Override public int getRandomNumber(int aRange) {return RNGSUS.nextInt(aRange);}
	@Override public int rng(int aRange) {return RNGSUS.nextInt(aRange);}
	public boolean rng() {return RNGSUS.nextBoolean();}
	@Override public Biome getBiome(BlockPos aCoords) {return level==null?null:WD.biome(level, aCoords.getX(), aCoords.getZ());}
	@Override public Biome getBiome(int aX, int aZ) {return level==null?null:WD.biome(level, aX, aZ);}
	@Override public Biome getBiome() {return getBiome(getBlockPos().getX(), getBlockPos().getZ());}
	@Override public Block getBlockOffset(int aX, int aY, int aZ) {return getBlock(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public Block getBlockAtSide(byte aSide) {return getBlockAtSideAndDistance(aSide, 1);}
	@Override public Block getBlockAtSideAndDistance(byte aSide, int aDistance) {return getBlock(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public byte getMetaDataOffset(int aX, int aY, int aZ) {return getMetaData(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public byte getMetaDataAtSide(byte aSide) {return getMetaDataAtSideAndDistance(aSide, 1);}
	@Override public byte getMetaDataAtSideAndDistance(byte aSide, int aDistance) {return getMetaData(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public byte getLightLevelOffset(int aX, int aY, int aZ) {return getLightLevel(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public byte getLightLevelAtSide(byte aSide) {return getLightLevelAtSideAndDistance(aSide, 1);}
	@Override public byte getLightLevelAtSideAndDistance(byte aSide, int aDistance) {return getLightLevel(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getOpacityOffset(int aX, int aY, int aZ) {return getOpacity(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public boolean getOpacityAtSide(byte aSide) {return getOpacityAtSideAndDistance(aSide, 1);}
	@Override public boolean getOpacityAtSideAndDistance(byte aSide, int aDistance) {return getOpacity(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getRainOffset(int aX, int aY, int aZ) {return getRain(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public boolean getRainAtSide(byte aSide) {return getRainAtSideAndDistance(aSide, 1);}
	@Override public boolean getRainAtSideAndDistance(byte aSide, int aDistance) {return getRain(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getSkyOffset(int aX, int aY, int aZ) {return getSky(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public boolean getSkyAtSide(byte aSide) {return getSkyAtSideAndDistance(aSide, 1);}
	@Override public boolean getSkyAtSideAndDistance(byte aSide, int aDistance) {return getSky(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public boolean getAirOffset(int aX, int aY, int aZ) {return getAir(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public boolean getAirAtSide(byte aSide) {return getAirAtSideAndDistance(aSide, 1);}
	@Override public boolean getAirAtSideAndDistance(byte aSide, int aDistance) {return getAir(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public BlockEntity getTileEntityOffset(int aX, int aY, int aZ) {return getTileEntity(getBlockPos().getX()+aX, getBlockPos().getY()+aY, getBlockPos().getZ()+aZ);}
	@Override public BlockEntity getTileEntityAtSideAndDistance(byte aSide, int aDistance) {return getTileEntity(getOffsetX(aSide, aDistance), getOffsetY(aSide, aDistance), getOffsetZ(aSide, aDistance));}
	@Override public DelegatorTileEntity<BlockEntity         > getAdjacentTileEntity     (byte aSide) {return getAdjacentTileEntity(aSide, T, F);}
	@Override public DelegatorTileEntity<Container         > getAdjacentInventory      (byte aSide) {return getAdjacentInventory(aSide, T, F);}
	@Override public DelegatorTileEntity<WorldlyContainer    > getAdjacentSidedInventory (byte aSide) {return getAdjacentSidedInventory(aSide, T, F);}
	@Override public DelegatorTileEntity<IFluidHandler      > getAdjacentTank           (byte aSide) {return getAdjacentTank(aSide, T, F);}
	@Override public DelegatorTileEntity<Container         > getAdjacentInventory      (byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(aSide, aAllowDelegates, aNotConnectToDelegators); return new DelegatorTileEntity<>(tDelegator.mTileEntity instanceof Container      ?(Container        )tDelegator.mTileEntity:null, tDelegator);}
	@Override public DelegatorTileEntity<WorldlyContainer    > getAdjacentSidedInventory (byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(aSide, aAllowDelegates, aNotConnectToDelegators); return new DelegatorTileEntity<>(tDelegator.mTileEntity instanceof WorldlyContainer ?(WorldlyContainer   )tDelegator.mTileEntity:null, tDelegator);}
	@Override public DelegatorTileEntity<IFluidHandler      > getAdjacentTank           (byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(aSide, aAllowDelegates, aNotConnectToDelegators); return new DelegatorTileEntity<>(tDelegator.mTileEntity instanceof IFluidHandler   ?(IFluidHandler     )tDelegator.mTileEntity:null, tDelegator);}
	
	@Override
	public DelegatorTileEntity<BlockEntity> getAdjacentTileEntity(byte aSide, boolean aAllowDelegates, boolean aNotConnectToDelegators) {
		BlockEntity tTileEntity = getTileEntityAtSideAndDistance(aSide, 1);
		if (tTileEntity == null) return new DelegatorTileEntity<>(null, level, getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide), OPOS[aSide]);
		if (aNotConnectToDelegators && tTileEntity instanceof ITileEntityCanDelegate && ((ITileEntityCanDelegate)tTileEntity).isExtender(aSide)) return new DelegatorTileEntity<>(null, level, getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide), OPOS[aSide]);
		if (aAllowDelegates && tTileEntity instanceof ITileEntityDelegating) return ((ITileEntityDelegating)tTileEntity).getDelegateTileEntity(OPOS[aSide]);
		return new DelegatorTileEntity<>(tTileEntity, tTileEntity.getLevel(), tTileEntity.getBlockPos().getX(), tTileEntity.getBlockPos().getY(), tTileEntity.getBlockPos().getZ(), OPOS[aSide]);
	}
	
	public List<DelegatorTileEntity<BlockEntity>> allAdjacentTileEntities(boolean aAllowDelegates, boolean aNotConnectToDelegators) {
		ArrayListNoNulls<DelegatorTileEntity<BlockEntity>> rDelegates = new ArrayListNoNulls<>();
		for (byte tSide : ALL_SIDES_VALID) {
			DelegatorTileEntity<BlockEntity> tDelegate = getAdjacentTileEntity(tSide, aAllowDelegates, aNotConnectToDelegators);
			if (tDelegate.mTileEntity != null) rDelegates.add(tDelegate);
		}
		return rDelegates;
	}
	
	@Override
	public Block getBlock(int aX, int aY, int aZ) {
		if (level == null) return NB;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return NB;
		return WD.block(level, aX, aY, aZ);
	}
	
	@Override
	public byte getMetaData(int aX, int aY, int aZ) {
		if (level == null) return 0;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return 0;
		return UT.Code.bind4(WD.meta(level, aX, aY, aZ));
	}
	
	@Override
	public byte getLightLevel(int aX, int aY, int aZ) {
		if (level == null) return 14;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return 0;
		return UT.Code.bind4((long)WD.lightBrightness(level, aX, aY, aZ)*15);
	}
	
	@Override
	public boolean getSky(int aX, int aY, int aZ) {
		if (level == null) return T;
		if (!level.dimensionType().hasSkyLight()) return F;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return T;
		return WD.canSeeSky(level, aX, aY, aZ);
	}
	
	@Override
	public boolean getRain(int aX, int aY, int aZ) {
		if (level == null) return T;
		if (!level.dimensionType().hasSkyLight()) return F;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return T;
		return WD.precipitationHeight(level, aX, aZ) <= aY;
	}
	
	@Override
	public boolean getOpacity(int aX, int aY, int aZ) {
		if (level == null) return F;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return F;
		return WD.opaque(WD.block(level, aX, aY, aZ));
	}
	
	@Override
	public boolean getAir(int aX, int aY, int aZ) {
		if (level == null) return T;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return T;
		return WD.air(level, aX, aY, aZ);
	}
	
	@Override
	public BlockEntity getTileEntity(int aX, int aY, int aZ) {
		if (level == null) return null;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aX, aZ) && !WD.exists(level, aX, aY, aZ)) return null;
		return WD.te(level, aX, aY, aZ, T);
	}
	
	@Override
	public Block getBlock(BlockPos aCoords) {
		if (level == null) return NB;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return NB;
		return WD.block(level, aCoords.getX(), aCoords.getY(), aCoords.getZ());
	}
	
	@Override
	public byte getMetaData(BlockPos aCoords) {
		if (level == null) return 0;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return 0;
		return (byte)WD.meta(level, aCoords.getX(), aCoords.getY(), aCoords.getZ());
	}
	
	@Override
	public byte getLightLevel(BlockPos aCoords) {
		if (level == null) return 14;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return 0;
		return UT.Code.bind4((long)WD.lightBrightness(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())*15);
	}
	
	@Override
	public boolean getSky(BlockPos aCoords) {
		if (level == null) return T;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return T;
		return WD.canSeeSky(level, aCoords.getX(), aCoords.getY(), aCoords.getZ());
	}
	
	@Override
	public boolean getRain(BlockPos aCoords) {
		if (level == null) return T;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return T;
		return WD.precipitationHeight(level, aCoords.getX(), aCoords.getZ()) <= aCoords.getY();
	}
	
	@Override
	public boolean getOpacity(BlockPos aCoords) {
		if (level == null) return F;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return F;
		return WD.opaque(WD.block(level, aCoords.getX(), aCoords.getY(), aCoords.getZ()));
	}
	
	@Override
	public boolean getAir(BlockPos aCoords) {
		if (level == null) return T;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return T;
		return WD.air(level, aCoords.getX(), aCoords.getY(), aCoords.getZ());
	}
	
	@Override
	public BlockEntity getTileEntity(BlockPos aCoords) {
		if (level == null) return null;
		if (mIgnoreUnloadedChunks && crossedChunkBorder(aCoords) && !WD.exists(level, aCoords.getX(), aCoords.getY(), aCoords.getZ())) return null;
		return WD.te(level, aCoords, T);
	}
	
	public DelegatorTileEntity<BlockEntity> delegator(byte aSide) {
		return new DelegatorTileEntity<BlockEntity>(this, aSide);
	}
	
	@Override
	public void sendBlockEvent(byte aID, byte aValue) {
		NW_API.sendToAllPlayersInRange(new PacketBlockEvent(getCoords(), aID, aValue), level, getCoords());
	}
	
	@Override
	public boolean isDead() {
		return mIsDead;
	}
	
	// @Override
	public void setRemoved() {
		super.setRemoved();
		setDead();
	}
	
	// @Override
	public void clearRemoved() {
		super.clearRemoved();
		// Paired with invalidateCaps(): reviving the BE must also revive its capability.
		// Vanilla's clearRemoved leaves Forge caps untouched on its own.
		reviveCaps();
		mCapsValid = T;
		setAlive();
	}
	
	// @Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		setDead();
	}
	
	@Optional.Method(modid = ModIDs.IC2)
	private void loadIntoEnet() {
		if (this instanceof IEnergyTile && (isEnergyType(TD.Energy.EU, SIDE_ANY, T) || isEnergyType(TD.Energy.EU, SIDE_ANY, F))) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent((IEnergyTile)this));
			mIsAddedToEnet = T;
		} else {
			mDoEnetCheck = F;
		}
	}
	
	@Optional.Method(modid = ModIDs.IC2)
	private void unloadFromEnet() {
		MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent((IEnergyTile)this));
		mIsAddedToEnet = F;
	}
	
	protected void doEnetUpdate() {
		if (isServerSide() && mIsAddedToEnet && mDoEnetCheck) try {unloadFromEnet(); loadIntoEnet();} catch(Throwable e) {mDoEnetCheck = F;}
	}
	
	protected void setDead() {
		if (!mIsDead) {
			mIsDead = T;
			if (isServerSide() && mIsAddedToEnet) try {unloadFromEnet();} catch(Throwable e) {mDoEnetCheck = F;}
		}
	}
	
	protected void setAlive() {
		mIsDead = F;
	}
	
	// @Override
	public void updateEntity() {
		// Well, if the TileEntity gets ticked, it is alive.
		if (isDead()) setAlive();
		
		if (isServerSide() && !mIsAddedToEnet && mDoEnetCheck) try {loadIntoEnet();} catch(Throwable e) {mDoEnetCheck = F;}
		
		if (mExplosionStrength > 0) {
			setToAir();
			if (mExplosionStrength < 1) {
				UT.Sounds.send(SFX.MC_EXPLODE, this, F);
			} else {
				ExplosionGT.explode(level, null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), mExplosionStrength, F, T);
			}
			setDead();
			return;
		}
		
		if (mDoesBlockUpdate) doBlockUpdate();
	}
	
	@Override
	public long getTimer() {
		return 0;
	}
	
	// @Override
	public boolean canUpdate() {
		return mIsTicking && mShouldRefresh;
	}
	
	// neoforge has no equivalent hook at all; the engine itself decides whether to keep a block entity
	// across a state change, so there is nothing left to restore here.
	// @Override
	public boolean shouldRefresh(Block aOldBlock, Block aNewBlock, int aOldMeta, int aNewMeta, Level aWorld, int aX, int aY, int aZ) {
		return mShouldRefresh || aOldBlock != aNewBlock;
	}
	
	/** Simple Function to prevent Block Updates from happening multiple times within the same Tick. */
	public final void causeBlockUpdate() {
		if (mIsTicking) mDoesBlockUpdate = T; else doBlockUpdate();
	}
	
	public void doBlockUpdate() {
		Block tBlock = getBlock(getCoords());
		level.updateNeighborsAt(new BlockPos(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()), tBlock);
		if (this instanceof IMTE_IsProvidingStrongPower) for (byte tSide : ALL_SIDES_VALID) {
			if (WD.normalCube(getBlockAtSide(tSide), level, getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+OFFY[tSide], getBlockPos().getZ()+OFFZ[tSide])) {
				level.updateNeighborsAt(new BlockPos(getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+OFFY[tSide], getBlockPos().getZ()+OFFZ[tSide]), tBlock);
			}
		}
		mDoesBlockUpdate = F;
	}
	
	public final boolean crossedChunkBorder(int aX, int aZ) {
		return aX >> 4 != getBlockPos().getX() >> 4 || aZ >> 4 != getBlockPos().getZ() >> 4;
	}
	
	public final boolean crossedChunkBorder(BlockPos aCoords) {
		return aCoords.getX() >> 4 != getBlockPos().getX() >> 4 || aCoords.getZ() >> 4 != getBlockPos().getZ() >> 4;
	}
	
	public float mExplosionStrength = 0;
	
	public final void explode() {explode(!mIsTicking);}
	public final void explode(double aStrength) {explode(!mIsTicking, aStrength);}
	
	public void explode(boolean aInstant) {
		explode(aInstant, 4); // Seems to be a reasonable Default Explosion. This Function can of course be overridden.
	}
	public void explode(boolean aInstant, double aStrength) {
		mExplosionStrength = (float)Math.max(aStrength, mExplosionStrength);
		if (aInstant) {
			setToAir();
			mExplodeSpamCooldown = 0;
			if (mExplosionStrength < 1) {
				UT.Sounds.send(SFX.MC_EXPLODE, this, F);
			} else {
				ExplosionGT.explode(level, null, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), mExplosionStrength, F, T);
			}
		}
	}
	
	public void overcharge(long aVoltage, TagData aEnergyType) {
		// Only explode if allowed
		if (OVERCHARGE_EXPLOSIONS) {
			if (TD.Energy.ALL_EXPLODING.contains(aEnergyType)) {
				explode(UT.Code.tierMax(aVoltage));
			} else {
				explode(0.1);
			}
		} else if (OVERCHARGE_BREAKING) {
			explode(0.1);
		}
		// Yes, I will annoy people with that a lot, even when they disable Explosions.
		UT.Sounds.send(TD.Energy.ALL_ELECTRIC.contains(aEnergyType)?SFX.IC_MACHINE_OVERLOAD:TD.Energy.ALL_KINETIC.contains(aEnergyType)?SFX.IC_MACHINE_INTERRUPT:SFX.MC_EXPLODE, this, F);
		// The Noise should make the position obvious.
		DEB.println("Machine overcharged with: " + aVoltage + " " + aEnergyType.getLocalisedNameLong());
	}
	
	public float getExplosionResistance(Entity aExploder, double aExplosionX, double aExplosionY, double aExplosionZ) {return mExplosionStrength > 0 ? 0 : getExplosionResistance2(aExploder, aExplosionX, aExplosionY, aExplosionZ);}
	public float getExplosionResistance() {return mExplosionStrength > 0 ? 0 : getExplosionResistance2();}
	
	public float getExplosionResistance2(Entity aExploder, double aExplosionX, double aExplosionY, double aExplosionZ) {return getExplosionResistance2();}
	public float getExplosionResistance2() {return 0;}
	
	@Override public Object getGUIClient(int aGUIID, Player aPlayer) {return null;}
	@Override public Object getGUIServer(int aGUIID, Player aPlayer) {return null;}
	
	public boolean interceptClick(int aGUIID, Slot_Base aSlot, int aSlotIndex, int aInvSlot, Player aPlayer, boolean aShiftclick, boolean aRightclick, int aMouse, int aShift) {return F;}
	public ItemStack slotClick(int aGUIID, Slot_Base aSlot, int aSlotIndex, int aInvSlot, Player aPlayer, boolean aShiftclick, boolean aRightclick, int aMouse, int aShift) {return null;}
	public void killGUIs() {for (Object tPlayer : level.players()) if (tPlayer instanceof Player && ((Player)tPlayer).containerMenu instanceof ContainerCommon && ((ContainerCommon)((Player)tPlayer).containerMenu).mTileEntity == this) ((Player)tPlayer).closeContainer();}
	public void rebootGUIs(int aGUIID) {for (Object tPlayer : level.players()) if (tPlayer instanceof Player && ((Player)tPlayer).containerMenu instanceof ContainerCommon && ((ContainerCommon)((Player)tPlayer).containerMenu).mTileEntity == this) {((Player)tPlayer).closeContainer(); openGUI((Player)tPlayer, aGUIID);}}
	public long getOpenGUIs() {long rGUIs = 0; for (Object tPlayer : level.players()) if (tPlayer instanceof Player && ((Player)tPlayer).containerMenu instanceof ContainerCommon && ((ContainerCommon)((Player)tPlayer).containerMenu).mTileEntity == this) rGUIs++; return rGUIs;}
	public boolean needsToSyncEverything() {return F;}
	
	public boolean shouldSideBeRendered(byte aSide) {
		BlockEntity tTileEntity = getTileEntityAtSideAndDistance(aSide, 1);
		return tTileEntity instanceof ITileEntitySurface ? !((ITileEntitySurface)tTileEntity).isSurfaceOpaque(OPOS[aSide]) : !WD.visOpq(level, getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide), SIDES_VERTICAL[aSide] || WD.border(getBlockPos().getX(), getBlockPos().getZ(), getOffsetX(aSide), getOffsetZ(aSide)), F);
	}
	
	/* RenderBlocks was removed with no replacement type, so the parameter became a neutral Object,
	   matching the pattern used by the central IRenderedBlockObject interfaces. */
	public boolean renderItem(Block aBlock, Object aRenderer) {return F;}
	public boolean renderBlock(Block aBlock, Object aRenderer, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean usesRenderPass(int aRenderPass, boolean[] aShouldSideBeRendered) {return T;}
	public boolean renderFullBlockSide(Block aBlock, Object aRenderer, byte aSide) {return shouldSideBeRendered(aSide);}
	public final IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return ERROR_MESSAGE == null ? passRenderingToObject2(aStack) : ErrorRenderer.INSTANCE;}
	public final IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return ERROR_MESSAGE == null ? passRenderingToObject2(aWorld, aX, aY, aZ) : ErrorRenderer.INSTANCE;}
	public IRenderedBlockObject passRenderingToObject2(ItemStack aStack) {return (IRenderedBlockObject)this;}
	public IRenderedBlockObject passRenderingToObject2(BlockGetter aWorld, int aX, int aY, int aZ) {return (IRenderedBlockObject)this;}
	
	public void updateTanks() {/**/}
	public void updateInventory() {/**/}
	public void updateAdjacentInventories() {for (byte tSide : ALL_SIDES_VALID) {DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(tSide); if (tDelegator.mTileEntity instanceof ITileEntityAdjacentInventoryUpdatable) ((ITileEntityAdjacentInventoryUpdatable)tDelegator.mTileEntity).adjacentInventoryUpdated(tDelegator.mSideOfTileEntity, (Container)this);}}
	
	public void playClick() {UT.Sounds.send(SFX.MC_CLICK, this, F);}
	public void playCollect() {UT.Sounds.send(SFX.MC_COLLECT, 0.2F, this, F);}
	
	public void updateLightValue() {
		if (this instanceof IMTE_GetLightValue) {
			// neo has no direct light-value setter; a recalculation is triggered on the engine instead, which
			// reads the current value back through getLightEmission on demand.
			if (level != null) {
				level.getLightEngine().checkBlock(getBlockPos());
				for (byte tSide : ALL_SIDES_MIDDLE) level.getLightEngine().checkBlock(new BlockPos(getBlockPos().getX()+OFFX[tSide], getBlockPos().getY()+OFFY[tSide], getBlockPos().getZ()+OFFZ[tSide]));
			}
		}
	}
	
	public boolean shouldCheckWeakPower(byte aSide) {
		return F;
	}
	
	@Override
	public boolean hasRedstoneIncoming() {
		return hasRedstoneIncoming(ALL_SIDES_VALID);
	}
	public boolean hasRedstoneIncoming(byte[] aSides) {
		for (byte tSide : aSides) if (getRedstoneIncoming(tSide) > 0) return T;
		return F;
	}
	public boolean hasRedstoneIncomingFromNonRail() {
		return hasRedstoneIncomingFromNonRail(ALL_SIDES_VALID);
	}
	public boolean hasRedstoneIncomingFromNonRail(byte[] aSides) {
		for (byte tSide : aSides) if (!(getBlockAtSide(tSide) instanceof BaseRailBlock) && getRedstoneIncoming(tSide) > 0) return T;
		return F;
	}
	
	@Override
	public byte getRedstoneIncoming(byte aSide) {
		if (level == null) return 0;
		if (SIDES_INVALID[aSide]) {
			byte rRedstone = 0;
			for (byte tSide : ALL_SIDES_VALID) {
				rRedstone = (byte)Math.max(rRedstone, level.getSignal(new BlockPos(getOffsetX(tSide), getOffsetY(tSide), getOffsetZ(tSide)), FORGE_DIR[tSide]));
				if (rRedstone >= 15) return 15;
			}
			return rRedstone;
		}
		return UT.Code.bind4(level.getSignal(new BlockPos(getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide)), FORGE_DIR[aSide]));
	}
	
	@Override
	public byte getComparatorIncoming(byte aSide) {
		if (level == null) return 0;
		// Forge's comparator-override hooks on Block are gone in neo.
		// neo BlockState.hasAnalogOutputSignal()/getAnalogOutputSignal(Level,BlockPos,Direction) (BlockBehaviour:628/632).
		BlockPos tPos = new BlockPos(getOffsetX(aSide), getOffsetY(aSide), getOffsetZ(aSide));
		net.minecraft.world.level.block.state.BlockState tState = gregapi.util.WD.state(level, tPos);
		return tState.hasAnalogOutputSignal()?UT.Code.bind4(tState.getAnalogOutputSignal(level, tPos)):getRedstoneIncoming(aSide);
	}
	
	// A Default implementation of the Fluid Tank behaviour, so that every TileEntity can use this to simplify its Code.
	
	protected IFluidTank getFluidTankFillable(byte aSide, FluidStack aFluidToFill) {return null;}
	protected IFluidTank getFluidTankDrainable(byte aSide, FluidStack aFluidToDrain) {return null;}
	protected IFluidTank[] getFluidTanks(byte aSide) {return ZL_FT;}

	public int fill(Direction aDirection, FluidStack aFluid, boolean aDoFill) {
		if (aFluid == null || aFluid.getAmount() <= 0) return 0;
		IFluidTank tTank = getFluidTankFillable(UT.Code.side(aDirection), aFluid);
		if (tTank == null) return 0;
		int rFilledAmount = tTank.fill(aFluid, aDoFill ? FluidAction.EXECUTE : FluidAction.SIMULATE);
		if (rFilledAmount > 0 && aDoFill) updateTanks();
		return rFilledAmount;
	}
	
	public FluidStack drain(Direction aDirection, FluidStack aFluid, boolean aDoDrain) {
		if (aFluid == null || aFluid.getAmount() <= 0) return null;
		IFluidTank tTank = getFluidTankDrainable(UT.Code.side(aDirection), aFluid);
		if (tTank == null || FL.invalid(tTank.getFluid()) || tTank.getFluidAmount() == 0 || !FL.equal(tTank.getFluid(), aFluid)) return null;
		FluidStack rDrained = tTank.drain(aFluid.getAmount(), aDoDrain ? FluidAction.EXECUTE : FluidAction.SIMULATE);
		if (rDrained != null && aDoDrain) updateTanks();
		return rDrained;
	}
	
	public FluidStack drain(Direction aDirection, int aAmountToDrain, boolean aDoDrain) {
		if (aAmountToDrain <= 0) return null;
		IFluidTank tTank = getFluidTankDrainable(UT.Code.side(aDirection), null);
		if (tTank == null || FL.invalid(tTank.getFluid()) || tTank.getFluidAmount() == 0) return null;
		FluidStack rDrained = tTank.drain(aAmountToDrain, aDoDrain ? FluidAction.EXECUTE : FluidAction.SIMULATE);
		if (rDrained != null && aDoDrain) updateTanks();
		return rDrained;
	}
	
	public boolean canFill(Direction aDirection, Fluid aFluid) {
		if (aFluid == null) return F;
		IFluidTank tTank = getFluidTankFillable(UT.Code.side(aDirection), FL.make(aFluid, 0));
		return tTank != null && (FL.invalid(tTank.getFluid()) || tTank.getFluid().getFluid() == aFluid);
	}
	
	public boolean canDrain(Direction aDirection, Fluid aFluid) {
		if (aFluid == null) return F;
		IFluidTank tTank = getFluidTankDrainable(UT.Code.side(aDirection), FL.make(aFluid, 0));
		return tTank != null && (tTank.getFluid() != null && tTank.getFluid().getFluid() == aFluid);
	}
	
	public FluidTankInfo[] getTankInfo(Direction aDirection) {
		IFluidTank[] tTanks = getFluidTanks(UT.Code.side(aDirection));
		if (tTanks == null || tTanks.length <= 0) return ZL_FLUIDTANKINFO;
		FluidTankInfo[] rInfo = new FluidTankInfo[tTanks.length];
		for (int i = 0; i < tTanks.length; i++) rInfo[i] = new FluidTankInfo(tTanks[i]);
		return rInfo;
	}

	// Forge 1.20.1's IFluidHandler is sideless (seven methods); leaf TEs that declare it inherit these bridges from here,
	// closing the contract centrally; unknown side maps to SIDE_ANY, and EMPTY replaces null since drain must return a stack.
	public int getTanks() {FluidTankInfo[] t = getTankInfo((Direction)null); return t == null ? 0 : t.length;}
	public FluidStack getFluidInTank(int aTank) {FluidTankInfo[] t = getTankInfo((Direction)null); return t != null && aTank >= 0 && aTank < t.length && t[aTank] != null && t[aTank].fluid != null ? t[aTank].fluid : FluidStack.EMPTY;}
	public int getTankCapacity(int aTank) {FluidTankInfo[] t = getTankInfo((Direction)null); return t != null && aTank >= 0 && aTank < t.length && t[aTank] != null ? t[aTank].capacity : 0;}
	public boolean isFluidValid(int aTank, FluidStack aFluid) {return aFluid != null && !aFluid.isEmpty() && canFill((Direction)null, aFluid.getFluid());}
	public int fill(FluidStack aFluid, FluidAction aAction) {return fill((Direction)null, aFluid, aAction.execute());}
	public FluidStack drain(FluidStack aFluid, FluidAction aAction) {FluidStack r = drain((Direction)null, aFluid, aAction.execute()); return r == null ? FluidStack.EMPTY : r;}
	public FluidStack drain(int aMaxDrain, FluidAction aAction) {FluidStack r = drain((Direction)null, aMaxDrain, aAction.execute()); return r == null ? FluidStack.EMPTY : r;}

	// getCapability(Capability,Direction) on the BlockEntity replaces per-subclass interface declarations; the bridge
	// lives once at the hierarchy root, and side stays alive (bound to the handler) instead of collapsing to SIDE_ANY.
	private final net.minecraftforge.common.util.LazyOptional<?>[] mFluidCaps = new net.minecraftforge.common.util.LazyOptional<?>[7], mItemCaps = new net.minecraftforge.common.util.LazyOptional<?>[7];
	/** Mirrors CapabilityProvider's private valid flag: once caps are invalidated, none are handed out.
	 *  clearRemoved() is what revives them again. */
	private boolean mCapsValid = T;

	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, Direction aSide) {
		if (mCapsValid) {
			if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
				// No tanks at all is a different answer from having empty tanks; the capability itself is absent in the former case.
				if (!gregapi.fluid.GT6FluidCapability.hasTanks(this, aSide)) return net.minecraftforge.common.util.LazyOptional.empty();
				return cachedCap(mFluidCaps, aSide, () -> gregapi.fluid.GT6FluidCapability.handlerOf(this, aSide));
			}
			if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
				if (!gregapi.tileentity.GT6ItemCapability.hasInventory(this)) return net.minecraftforge.common.util.LazyOptional.empty();
				return cachedCap(mItemCaps, aSide, () -> gregapi.tileentity.GT6ItemCapability.handlerOf(this, aSide));
			}
		}
		return super.getCapability(aCapability, aSide);
	}

	/** Consumers may cache this LazyOptional and listen for its invalidation.
	 *  The same instance must be reused per side; handing out a fresh one each call would break that contract. */
	@SuppressWarnings("unchecked")
	private <T> net.minecraftforge.common.util.LazyOptional<T> cachedCap(net.minecraftforge.common.util.LazyOptional<?>[] aCache, Direction aSide, net.minecraftforge.common.util.NonNullSupplier<Object> aFactory) {
		int tIndex = UT.Code.side(aSide);
		net.minecraftforge.common.util.LazyOptional<?> rCap = aCache[tIndex];
		if (rCap == null) aCache[tIndex] = rCap = net.minecraftforge.common.util.LazyOptional.of(aFactory);
		return (net.minecraftforge.common.util.LazyOptional<T>)rCap;
	}

	/** A removed or unloaded BE must never hand out live capability handlers.
	 *  The engine calls this from setRemoved and onChunkUnloaded. */
	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mCapsValid = F;
		invalidateCapArray(mFluidCaps);
		invalidateCapArray(mItemCaps);
	}

	private static void invalidateCapArray(net.minecraftforge.common.util.LazyOptional<?>[] aCache) {
		for (int i = 0; i < aCache.length; i++) if (aCache[i] != null) {aCache[i].invalidate(); aCache[i] = null;}
	}

	// A Default implementation of the MultiBlock related Fluid Tank behaviour.
	
	protected IFluidTank getFluidTankFillable(MultiTileEntityMultiBlockPart aPart, byte aSide, FluidStack aFluidToFill) {return getFluidTankFillable(SIDE_ANY, aFluidToFill);}
	protected IFluidTank getFluidTankDrainable(MultiTileEntityMultiBlockPart aPart, byte aSide, FluidStack aFluidToDrain) {return getFluidTankDrainable(SIDE_ANY, aFluidToDrain);}
	/** public, not protected, since a multiblock wall exposes its controller's tanks through this method
	 *  to outside callers, who no longer see a raw IFluidHandler directly. */
	public IFluidTank[] getFluidTanks(MultiTileEntityMultiBlockPart aPart, byte aSide) {return getFluidTanks(SIDE_ANY);}
	
	public int fill(MultiTileEntityMultiBlockPart aPart, byte aDirection, FluidStack aFluid, boolean aDoFill) {
		if (aFluid == null || aFluid.getAmount() <= 0) return 0;
		IFluidTank tTank = getFluidTankFillable(aPart, SIDE_ANY, aFluid);
		if (tTank == null) return 0;
		int rFilledAmount = tTank.fill(aFluid, aDoFill ? FluidAction.EXECUTE : FluidAction.SIMULATE);
		if (rFilledAmount > 0 && aDoFill) updateTanks();
		return rFilledAmount;
	}
	
	public FluidStack drain(MultiTileEntityMultiBlockPart aPart, byte aDirection, FluidStack aFluid, boolean aDoDrain) {
		if (aFluid == null || aFluid.getAmount() <= 0) return null;
		IFluidTank tTank = getFluidTankDrainable(aPart, SIDE_ANY, aFluid);
		if (tTank == null || FL.invalid(tTank.getFluid()) || tTank.getFluidAmount() == 0 || !FL.equal(tTank.getFluid(), aFluid)) return null;
		FluidStack rDrained = tTank.drain(aFluid.getAmount(), aDoDrain ? FluidAction.EXECUTE : FluidAction.SIMULATE);
		if (rDrained != null && aDoDrain) updateTanks();
		return rDrained;
	}
	
	public FluidStack drain(MultiTileEntityMultiBlockPart aPart, byte aDirection, int aAmountToDrain, boolean aDoDrain) {
		if (aAmountToDrain <= 0) return null;
		IFluidTank tTank = getFluidTankDrainable(aPart, SIDE_ANY, null);
		if (tTank == null || FL.invalid(tTank.getFluid()) || tTank.getFluidAmount() == 0) return null;
		FluidStack rDrained = tTank.drain(aAmountToDrain, aDoDrain ? FluidAction.EXECUTE : FluidAction.SIMULATE);
		if (rDrained != null && aDoDrain) updateTanks();
		return rDrained;
	}
	
	public boolean canFill(MultiTileEntityMultiBlockPart aPart, byte aDirection, Fluid aFluid) {
		if (aFluid == null) return F;
		IFluidTank tTank = getFluidTankFillable(aPart, SIDE_ANY, FL.make(aFluid, 0));
		return tTank != null && (FL.invalid(tTank.getFluid()) || tTank.getFluid().getFluid() == aFluid);
	}
	
	public boolean canDrain(MultiTileEntityMultiBlockPart aPart, byte aDirection, Fluid aFluid) {
		if (aFluid == null) return F;
		IFluidTank tTank = getFluidTankDrainable(aPart, SIDE_ANY, FL.make(aFluid, 0));
		return tTank != null && (tTank.getFluid() != null && tTank.getFluid().getFluid() == aFluid);
	}
	
	public FluidTankInfo[] getTankInfo(MultiTileEntityMultiBlockPart aPart, byte aDirection) {
		IFluidTank[] tTanks = getFluidTanks(aPart, SIDE_ANY);
		if (tTanks == null || tTanks.length <= 0) return ZL_FLUIDTANKINFO;
		FluidTankInfo[] rInfo = new FluidTankInfo[tTanks.length];
		for (int i = 0; i < tTanks.length; i++) rInfo[i] = new FluidTankInfo(tTanks[i]);
		return rInfo;
	}
	
	// A Default implementation of the Energy behaviour.
	
	public long doInject (TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject ) {return 0;}
	public long doExtract(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return F;}
	public boolean isEnergyCapacitorType(TagData aEnergyType, byte aSide) {return F;}
	public long getEnergyStored(TagData aEnergyType, byte aSide) {return 0;}
	public long getEnergyCapacity(TagData aEnergyType, byte aSide) {return 0;}
	public Collection<TagData> getEnergyTypes(byte aSide) {return Collections.emptyList();}
	public Collection<TagData> getEnergyCapacitorTypes(byte aSide) {return Collections.emptyList();}
	
	public boolean isEnergyEmittingTo   (TagData aEnergyType, byte aSide, boolean aTheoretical) {return isEnergyType(aEnergyType, aSide, T) && getSurfaceSizeAttachable(aSide) > 0;}
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return isEnergyType(aEnergyType, aSide, F) && getSurfaceSizeAttachable(aSide) > 0;}
	public synchronized long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return aSize != 0 && isEnergyEmittingTo   (aEnergyType, aSide, F) ? TD.Energy.ALL_SIZE_IRRELEVANT.contains(aEnergyType) || Math.abs(aSize) >= getEnergySizeOutputMin(aEnergyType, aSide) ? doExtract(aEnergyType, aSide, aSize, aAmount, aDoExtract) :       0 : 0;}
	public synchronized long doEnergyInjection (TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject ) {return aSize != 0 && isEnergyAcceptingFrom(aEnergyType, aSide, F) ? TD.Energy.ALL_SIZE_IRRELEVANT.contains(aEnergyType) || Math.abs(aSize) >= getEnergySizeInputMin (aEnergyType, aSide) ? doInject (aEnergyType, aSide, aSize, aAmount, aDoInject ) : aAmount : 0;}
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {return 0;}
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return 0;}
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {return getEnergySizeOutputRecommended(aEnergyType, aSide) / 2;}
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {return getEnergySizeOutputRecommended(aEnergyType, aSide) * 2;}
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {return 0;}
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return 0;}
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return getEnergySizeInputRecommended(aEnergyType, aSide) / 2;}
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return getEnergySizeInputRecommended(aEnergyType, aSide) * 2;}
	
	// A Default implementation of the MultiBlock Energy behaviour.
	
	public boolean isEnergyType                         (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide, boolean aEmitting) {return isEnergyType(aEnergyType, aSide, aEmitting);}
	public boolean isEnergyCapacitorType                (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return isEnergyCapacitorType(aEnergyType, aSide);}
	public long getEnergyStored                         (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergyStored(aEnergyType, aSide);}
	public long getEnergyCapacity                       (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergyCapacity(aEnergyType, aSide);}
	public Collection<TagData> getEnergyTypes           (MultiTileEntityMultiBlockPart aPart, byte aSide) {return getEnergyTypes(aSide);}
	public Collection<TagData> getEnergyCapacitorTypes  (MultiTileEntityMultiBlockPart aPart, byte aSide) {return getEnergyCapacitorTypes(aSide);}
	
	public boolean isEnergyAcceptingFrom                (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide, boolean aTheoretical) {return isEnergyAcceptingFrom(aEnergyType, aSide, aTheoretical);}
	public boolean isEnergyEmittingTo                   (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide, boolean aTheoretical) {return isEnergyEmittingTo   (aEnergyType, aSide, aTheoretical);}
	public long doEnergyInjection                       (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject ) {return doEnergyInjection (aEnergyType, aSide, aSize, aAmount, aDoInject );}
	public long doEnergyExtraction                      (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return doEnergyExtraction(aEnergyType, aSide, aSize, aAmount, aDoExtract);}
	public long getEnergyOffered                        (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide, long aSize) {return getEnergyOffered(aEnergyType, aSide, aSize);}
	public long getEnergySizeOutputRecommended          (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergySizeOutputRecommended(aEnergyType, aSide);}
	public long getEnergySizeOutputMin                  (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergySizeOutputMin(aEnergyType, aSide);}
	public long getEnergySizeOutputMax                  (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergySizeOutputMax(aEnergyType, aSide);}
	public long getEnergyDemanded                       (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide, long aSize) {return getEnergyDemanded(aEnergyType, aSide, aSize);}
	public long getEnergySizeInputRecommended           (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergySizeInputRecommended(aEnergyType, aSide);}
	public long getEnergySizeInputMin                   (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergySizeInputMin(aEnergyType, aSide);}
	public long getEnergySizeInputMax                   (MultiTileEntityMultiBlockPart aPart, TagData aEnergyType, byte aSide) {return getEnergySizeInputMax(aEnergyType, aSide);}
	
	// A Default implementation for RF Stuff, so I don't have to implement those Interfaces manually every time I make an RF Emitter or Acceptor.
	
	public boolean canConnectEnergy  (Direction aDirection) {for (TagData tTag : TD.Energy.ALL_RF) if (isEnergyEmittingTo(tTag, UT.Code.side(aDirection), T)) return T; return isEnergyAcceptingFrom(TD.Energy.RF, UT.Code.side(aDirection), T);}
	public int     receiveEnergy     (Direction aDirection, int aSize, boolean aSimulate) {return (int)doEnergyInjection (TD.Energy.RF, UT.Code.side(aDirection), 1, aSize, !aSimulate);}
	public int     extractEnergy     (Direction aDirection, int aSize, boolean aSimulate) {return (int)doEnergyExtraction(TD.Energy.RF, UT.Code.side(aDirection), 1, aSize, !aSimulate);}
	public int     getEnergyStored   (Direction aDirection) {return UT.Code.bindInt(getEnergyCapacity(TD.Energy.RF, UT.Code.side(aDirection)));}
	public int     getMaxEnergyStored(Direction aDirection) {return UT.Code.bindInt(getEnergyCapacity(TD.Energy.RF, UT.Code.side(aDirection)));}
	
	// A Default implementation for EU Stuff, so I don't have to implement those Interfaces manually every time I make an EU Emitter or Acceptor.
	
	public int getSinkTier() {return UT.Code.tierMax(getEnergySizeInputRecommended(TD.Energy.EU, SIDE_ANY));}
	public int getSourceTier() {return UT.Code.tierMax(getEnergySizeOutputRecommended(TD.Energy.EU, SIDE_ANY));}
	public boolean acceptsEnergyFrom(BlockEntity aEmitter, Direction aDirection) {return !(aEmitter instanceof ITileEntityEnergy) && isEnergyAcceptingFrom(TD.Energy.EU, UT.Code.side(aDirection), T);}
	public boolean emitsEnergyTo(BlockEntity aReceiver, Direction aDirection) {return !(aReceiver instanceof ITileEntityEnergy) && isEnergyEmittingTo(TD.Energy.EU, UT.Code.side(aDirection), T);}
	public double getDemandedEnergy() {long tSize = getEnergySizeInputMax(TD.Energy.EU, SIDE_ANY); return tSize * doEnergyInjection(TD.Energy.EU, SIDE_ANY, tSize, 256, F);}
	
	public double injectEnergy(Direction aDirection, double aAmount, double aSize) {
		aSize = Math.min(aSize, aAmount);
		return aAmount - (aSize <= 0 ? aAmount * doEnergyInjection(TD.Energy.EU, UT.Code.side(aDirection), (long)aAmount, 1, T) : aSize * doEnergyInjection(TD.Energy.EU, UT.Code.side(aDirection), (long)aSize, (long)(aAmount / aSize), T));
	}
	
	private byte mExplodeSpamCooldown = 0;
	
	/**
	 * Structural checks that make sure that nothing is wrong with the environment around the Block. Should maybe only be checked every 600 ticks and only while the Machine is active.
	 * @return false if something went wrong.
	 */
	public boolean doDefaultStructuralChecks() {
		for (TagData tEnergyType : getEnergyTypes(SIDE_ANY)) {
			if (TD.Energy.ALL_WEAK_TO_FIRE.contains(tEnergyType)) for (byte tSide : ALL_SIDES_VALID) {
				if (!isFireProof(tSide) && getBlockAtSide(tSide) instanceof FireBlock && rng(10) == 0) {
					if (FIRE_EXPLOSIONS) explode(TD.Energy.ALL_EXPLODING.contains(tEnergyType) ? 4.0 : 0.1); else if (FIRE_BREAKING) explode(0.1);
					if (mExplodeSpamCooldown++ == 0) {
						UT.Sounds.send(TD.Energy.ALL_ELECTRIC.contains(tEnergyType)?SFX.IC_MACHINE_OVERLOAD:TD.Energy.ALL_KINETIC.contains(tEnergyType)?SFX.IC_MACHINE_INTERRUPT:SFX.MC_EXPLODE, this, F);
						DEB.println("Machine came into contact with Fire - Energy Type: " + tEnergyType.getLocalisedNameLong());
					}
					return F;
				}
			}
			if (TD.Energy.ALL_WEAK_TO_WATER.contains(tEnergyType)) for (byte tSide : ALL_SIDES_BUT_BOTTOM) {
				if (!isWaterProof(tSide) && WD.liquid(getBlockAtSide(tSide))) {
					if (WATER_EXPLOSIONS) explode(TD.Energy.ALL_EXPLODING.contains(tEnergyType) ? 4.0 : 0.1); else if (WATER_BREAKING) explode(0.1);
					if (mExplodeSpamCooldown++ == 0) {
						UT.Sounds.send(TD.Energy.ALL_ELECTRIC.contains(tEnergyType)?SFX.IC_MACHINE_OVERLOAD:TD.Energy.ALL_KINETIC.contains(tEnergyType)?SFX.IC_MACHINE_INTERRUPT:SFX.MC_EXPLODE, this, F);
						DEB.println("Machine came into contact with Water - Energy Type: " + tEnergyType.getLocalisedNameLong());
					}
					return F;
				}
				if (!isRainProof(tSide) && level.isRaining() && WD.rainfall(getBiome()) > 0 && rng(100) == 0 && getRainAtSide(tSide)) {
					if (RAIN_EXPLOSIONS) explode(TD.Energy.ALL_EXPLODING.contains(tEnergyType) ? 4.0 : 0.1); else if (RAIN_BREAKING) explode(0.1);
					if (mExplodeSpamCooldown++ == 0) {
						UT.Sounds.send(TD.Energy.ALL_ELECTRIC.contains(tEnergyType)?SFX.IC_MACHINE_OVERLOAD:TD.Energy.ALL_KINETIC.contains(tEnergyType)?SFX.IC_MACHINE_INTERRUPT:SFX.MC_EXPLODE, this, F);
						DEB.println("Machine came into contact with Rain - Energy Type: " + tEnergyType.getLocalisedNameLong());
					}
					return F;
				}
			}
			if (TD.Energy.ALL_WEAK_TO_THUNDER.contains(tEnergyType)) for (byte tSide : ALL_SIDES_BUT_BOTTOM) {
				if (!isThunderProof(tSide) && level.isThundering() && rng(1000) == 0 && getRainAtSide(tSide)) {
					if (THUNDER_EXPLOSIONS) explode(TD.Energy.ALL_EXPLODING.contains(tEnergyType) ? 4.0 : 0.1); else if (THUNDER_BREAKING) explode(0.1);
					if (mExplodeSpamCooldown++ == 0) {
						UT.Sounds.send(TD.Energy.ALL_ELECTRIC.contains(tEnergyType)?SFX.IC_MACHINE_OVERLOAD:TD.Energy.ALL_KINETIC.contains(tEnergyType)?SFX.IC_MACHINE_INTERRUPT:SFX.MC_EXPLODE, this, F);
						DEB.println("Machine came into contact with Thunder - Energy Type: " + tEnergyType.getLocalisedNameLong());
					}
					return F;
				}
			}
		}
		return T;
	}
	
	public boolean isFireProof              (byte aSide) {return F;}
	public boolean isRainProof              (byte aSide) {return F;}
	public boolean isWaterProof             (byte aSide) {return F;}
	public boolean isThunderProof           (byte aSide) {return F;}
	
	// A Default implementation of the Surface behavior.
	
	public float getSurfaceSize             (byte aSide) {return 1;}
	public float getSurfaceSizeAttachable   (byte aSide) {return getSurfaceSize(aSide);}
	public float getSurfaceDistance         (byte aSide) {return 0;}
	public boolean isSurfaceSolid           (byte aSide) {return isSurfaceOpaque(aSide);}
	public boolean isSurfaceOpaque          (byte aSide) {return T;}
	public boolean isSealable               (byte aSide) {return F;}
	public AABB getCollisionBoundingBoxFromPool() {return box();}
	public AABB getSelectedBoundingBoxFromPool () {if (FORCE_FULL_SELECTION_BOXES) return box(); return box(shrunkBox());}
	public void setBlockBoundsBasedOnState(Block aBlock) {if (FORCE_FULL_SELECTION_BOXES) box(aBlock); else box(aBlock, shrunkBox());}
	public boolean ignorePlayerCollisionWhenPlacing(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {return ignorePlayerCollisionWhenPlacing();}
	public boolean ignorePlayerCollisionWhenPlacing() {return F;}
	
	/** Old Coordinate containing Variant of onCoordinateChange, use only if you really need the Coordinates, as there is also a No-Parameter variant in use for some TileEntity Types! */
	public void onCoordinateChange(Level aWorld, int aOldX, int aOldY, int aOldZ) {onCoordinateChange();}
	public void onCoordinateChange() {/**/}
	
	// AE Stuff

	// The @Override is gone along with implements IMovableTile, but the method bodies stay.
	// Wiring to AE2's own move mechanic on 1.20.1 is out of scope here.
	public boolean prepareToMove() {return T;}
	public void doneMoving() {onCoordinateChange();}
	
	// Fire Stuff
	
	public int getFireSpreadSpeed(byte aSide, boolean aDefault) {return aDefault ? 150 : 0;}
	public int getFlammability   (byte aSide, boolean aDefault) {return aDefault ? 150 : 0;}
	public void setOnFire() {WD.burn(level, getCoords(), F, F);}
	public boolean setToFire() {return WD.set(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), Blocks.FIRE, 0, 3);}
	
	// Removal and Snow Layer Stuff
	
	public static final ITexture SNOW_TEXTURE = BlockTextureCopied.get(Blocks.SNOW); // very commonly used Texture.
	public boolean removedByPlayer(Level aWorld, Player aPlayer, boolean aWillHarvest) {return setToAir();}
	public boolean hasSnow() {for (int i : SCAN_NEG_1) for (int j : SCAN_NEG_1) if (getBlockOffset(i, 0, j) == Blocks.SNOW) return T; return F;}
	public boolean setToSnow() {return getOpacity(getBlockPos().getX(), getBlockPos().getY()-1, getBlockPos().getZ()) && hasSnow() && WD.set(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), Blocks.SNOW, 0, 3);}
	public boolean setToAir() {if (WD.set(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), Blocks.AIR, 0, 3)) {if (this instanceof IMultiTileEntity.IMTE_CanPlaceSnowLayerOnRemoval) setToSnow(); return T;} return F;}
	
	// Inventory Stuff
	
	public ItemStack slot(int aIndex, ItemStack aStack) {return NI;}
	public ItemStack slot(int aIndex) {return NI;}
	public ItemStack slotTake(int aIndex) {return NI;}
	public boolean slotTrash(int aIndex) {return GarbageGT.trash(slotTake(aIndex)) > 0;}
	public boolean slotNull(int aIndex) {if (slotHas(aIndex) && slot(aIndex).getCount() < 0) return slotKill(aIndex); return F;}
	public boolean slotKill(int aIndex) {slot(aIndex, NI); return T;}
	public boolean slotHas(int aIndex) {return F;}
	public boolean invempty() {return T;}
	public int invsize() {return 0;}
	public CompoundTag slotNBT(int aIndex) {return null;}
	
	// Connectable Inventories
	
	public int[] getAccessibleSlotsOfConnectedInventory() {return UT.Code.getAscendingArray(invsize());}
	
	public int addStackToConnectedInventory(byte aSide, ItemStack aStack, boolean aOnlyAddIfItAlreadyHasItemsOfThatTypeOrIsDedicated) {
		if (ST.invalid(aStack)) return 0;
		int rCount = 0, aCount = aStack.getCount();
		for (int tSlot : getAccessibleSlotsOfConnectedInventory()) if (ST.equal(slot(tSlot), aStack)) {
			aOnlyAddIfItAlreadyHasItemsOfThatTypeOrIsDedicated = F;
			int tChange = Math.min(aCount, slot(tSlot).getMaxStackSize() - slot(tSlot).getCount());
			slot(tSlot).setCount(slot(tSlot).getCount()+(tChange));
			rCount += tChange;
			aCount -= tChange;
			if (aCount <= 0) {
				updateInventory();
				return rCount;
			}
		}
		if (!aOnlyAddIfItAlreadyHasItemsOfThatTypeOrIsDedicated) for (int tSlot : getAccessibleSlotsOfConnectedInventory()) if (!slotHas(tSlot)) {
			slot(tSlot, ST.amount(aCount, aStack));
			rCount += aCount;
			aCount = 0;
			updateInventory();
			return rCount;
		}
		if (rCount > 0) updateInventory();
		return rCount;
	}
	
	public int removeStackFromConnectedInventory(byte aSide, ItemStack aStack, boolean aOnlyRemoveIfItCanRemoveAllAtOnce) {
		if (ST.invalid(aStack)) return 0;
		int rCount = 0;
		if (!aOnlyRemoveIfItCanRemoveAllAtOnce || getAmountOfItemsInConnectedInventory(aSide, aStack, aStack.getCount()) >= aStack.getCount()) {
			for (int tSlot : getAccessibleSlotsOfConnectedInventory()) if (ST.equal(slot(tSlot), aStack)) {
				int tChange = Math.min(aStack.getCount() - rCount, slot(tSlot).getCount());
				slot(tSlot).setCount(slot(tSlot).getCount()-(tChange));
				if (slot(tSlot).getCount() <= 0) slotKill(tSlot);
				rCount += tChange;
				if (rCount >= aStack.getCount()) {
					updateInventory();
					return rCount;
				}
			}
		}
		if (rCount > 0) updateInventory();
		return rCount;
	}
	
	public long getAmountOfItemsInConnectedInventory(byte aSide, ItemStack aStack, long aStopCountingAtThisNumber) {
		if (ST.invalid(aStack)) return 0;
		long rCount = 0;
		for (int tSlot : getAccessibleSlotsOfConnectedInventory()) if (ST.equal(slot(tSlot), aStack) && (rCount += slot(tSlot).getCount()) >= aStopCountingAtThisNumber) break;
		return rCount;
	}
	
	// Quick Fix for newly added Functionality
	
	public int funnelFill(byte aSide, FluidStack aFluid, boolean aDoFill) {return 0;}
	public int capnozzleFill(byte aSide, FluidStack aFluid, boolean aDoFill) {return funnelFill(aSide, aFluid, aDoFill);}
	public FluidStack tapDrain(byte aSide, int aMaxDrain, boolean aDoDrain) {return null;}
	public FluidStack nozzleDrain(byte aSide, int aMaxDrain, boolean aDoDrain) {return tapDrain(aSide, aMaxDrain, aDoDrain);}   
	
	// Useful to check if a Player or any other Entity is actually allowed to access or interact with this Block.
	
	@Override
	public boolean allowInteraction(Entity aEntity) {return T;}
	public boolean allowRightclick(Entity aEntity) {return allowInteraction(aEntity);}
	public float getPlayerRelativeBlockHardness(Player aPlayer, float aOriginal) {return allowInteraction(aPlayer) ? Math.max(aOriginal, 0.0001F) : 0;}
	
	// Regarding Multiblocks. If true it will always send a machineblock update whenever something relevant changes, such as facing or connectivity.
	
	public boolean hasMultiBlockMachineRelevantData() {return F;}
	
	// Makes a Bounding Box without having to constantly specify the Offset Coordinates.
	
	public AABB box(double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ) {return new AABB(getBlockPos().getX()+aMinX, getBlockPos().getY()+aMinY, getBlockPos().getZ()+aMinZ, getBlockPos().getX()+aMaxX, getBlockPos().getY()+aMaxY, getBlockPos().getZ()+aMaxZ);}
	public AABB box(double[] aBox) {return new AABB(getBlockPos().getX()+aBox[0], getBlockPos().getY()+aBox[1], getBlockPos().getZ()+aBox[2], getBlockPos().getX()+aBox[3], getBlockPos().getY()+aBox[4], getBlockPos().getZ()+aBox[5]);}
	public AABB box(float[] aBox) {return new AABB(getBlockPos().getX()+aBox[0], getBlockPos().getY()+aBox[1], getBlockPos().getZ()+aBox[2], getBlockPos().getX()+aBox[3], getBlockPos().getY()+aBox[4], getBlockPos().getZ()+aBox[5]);}
	public AABB box() {return new AABB(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getBlockPos().getX()+1, getBlockPos().getY()+1, getBlockPos().getZ()+1);}
	
	public boolean box(AABB aAABB, List<AABB> aList, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ) {
		AABB tBox = box(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aAABB, List<AABB> aList, double[] aBox) {
		AABB tBox = box(aBox[0], aBox[1], aBox[2], aBox[3], aBox[4], aBox[5]);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aAABB, List<AABB> aList, float[] aBox) {
		AABB tBox = box(aBox[0], aBox[1], aBox[2], aBox[3], aBox[4], aBox[5]);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aAABB, List<AABB> aList) {
		AABB tBox = box(PX_BOX);
		return tBox.intersects(aAABB) && aList.add(tBox);
	}
	public boolean box(AABB aBox, AABB aAABB, List<AABB> aList) {
		return aBox != null && aBox.intersects(aAABB) && aList.add(aBox);
	}
	
	public boolean box(Block aBlock) {WD.setBlockBounds(aBlock, 0,0,0,1,1,1); return T;}
	public boolean box(Block aBlock, double[] aBox) {WD.setBlockBounds(aBlock, (float)aBox[0], (float)aBox[1], (float)aBox[2], (float)aBox[3], (float)aBox[4], (float)aBox[5]); return T;}
	public boolean box(Block aBlock, float[] aBox) {WD.setBlockBounds(aBlock, aBox[0], aBox[1], aBox[2], aBox[3], aBox[4], aBox[5]); return T;}
	public boolean box(Block aBlock, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ) {WD.setBlockBounds(aBlock, (float)aMinX, (float)aMinY, (float)aMinZ, (float)aMaxX, (float)aMaxY, (float)aMaxZ); return T;}
	
	public float[] shrunkBox() {return PX_BOX;}
	
	// Default Overlay Code
	
	public boolean isUsingWrenchingOverlay(ItemStack aStack, byte aSide) {
		return F;
	}
	
	public boolean isConnectedWrenchingOverlay(ItemStack aStack, byte aSide) {
		return F;
	}
	
	/** Was DrawBlockHighlightEvent (type removed); see the interface's own javadoc for details. */
	public boolean onDrawBlockHighlight2(RenderHighlightEvent.Block aEvent) {return F;}

	/** The new RenderHighlightEvent.Block carries no player/current-item/partialTicks, unlike the 1.7.10 event it replaces.
	 *  Deciding the wrench overlay by held item is unreachable here before the BER path, so this stays a compiling stub. */
	public final boolean onDrawBlockHighlight(RenderHighlightEvent.Block aEvent) {
		FORCE_FULL_SELECTION_BOXES = F;
		// The new event has no player field, so it's fetched through the mod's own client/server split (GT_API_Proxy)
		// instead of calling the client-only Minecraft class directly, which would fail class verification on a dedicated server.
		byte tSide = (byte)aEvent.getTarget().getDirection().ordinal();
		if (!SIDES_VALID[tSide] || onDrawBlockHighlight2(aEvent)) return T;
		net.minecraft.world.entity.player.Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		ItemStack tHeld = tPlayer == null ? null : tPlayer.getMainHandItem();
		if (ST.valid(tHeld) && isUsingWrenchingOverlay(tHeld, tSide)) {
			// Holding a wrench forces the full selection box even on a thin connector, matching the original;
			// without it, an unconnected wire's frame becomes impossible to target.
			FORCE_FULL_SELECTION_BOXES = T;
			byte tConnections = 0;
			for (byte i = 0; i < 6; i++) if (isConnectedWrenchingOverlay(tHeld, i)) tConnections |= (byte)(1 << i);
			gregapi.render.RenderHelper.drawWrenchOverlay(aEvent, tConnections, tSide);
			return T;
		}
		return T;
	}
	
	// Error things
	
	public String ERROR_MESSAGE = null;
	@Override public void setError(String aError) {ERROR_MESSAGE = aError; if (isServerSide()) NW_API.sendToAllPlayersInRange(new PacketBlockError(getCoords(), ERROR_MESSAGE), level, getCoords());}
}
