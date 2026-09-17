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

package gregapi.block.multitileentity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.server.level.ServerLevel;

import gregapi.block.*;
import gregapi.block.IBlockSyncData.IBlockSyncDataAndCoversAndIDs;
import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.IL;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.item.IItemGT;
import gregapi.network.INetworkHandler;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.render.IRenderedBlock;
import gregapi.render.IRenderedBlockObject;
import gregapi.render.ITexture;
import gregapi.render.RendererBlockTextured;
import gregapi.tileentity.ITileEntity;
import gregapi.tileentity.ITileEntityMachineBlockUpdateable;
import gregapi.tileentity.ITileEntitySynchronising;
import gregapi.tileentity.inventories.ITileEntityBookShelf;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import mekanism.api.MekanismAPI;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import gregapi.block.MapColor;
import gregapi.block.Material;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;

import java.util.*;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@SuppressWarnings("deprecation")
public class MultiTileEntityBlock extends Block implements IBlock, IItemGT, IBlockDebugable, IBlockErrorable, IBlockOnWalkOver, IBlockSyncDataAndCoversAndIDs, IRenderedBlock, EntityBlock, IBlockToolable, IBlockRetrievable, IBlockMaterial {
	private static final Map<String, MultiTileEntityBlock> MULTITILEENTITYBLOCKMAP = new HashMap<>();
	
	private final int mHarvestLevelOffset, mHarvestLevelMinimum, mHarvestLevelMaximum;
	private final String mNameInternal, mTool;
	private final boolean mOpaque, mNormalCube;
	
	public MapColor mMapColor = null;

	/** Same trick as BlockBase: stores the last-set bounds itself, since neo bounds are immutable;
	 *  render use is deferred to a later client pass. Required by the IBlock contract. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	@Override public float[] getRenderBounds() {return mRenderBounds;}

	/** gregapi Material is stored by the MTE block itself (same pattern as BlockBase); neo removed
	 *  vanilla's own Block.getMaterial()/blockMaterial. */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}

	public static String getName(String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		// Was aSoundType.soundName (a 1.7.10 String category) in the MTE registration key; neo's
		// SoundType carries no name, so the 1.7.10 name is reproduced 1:1, values checked against the golden dump.
		// rock(STONE)→"stone", cloth/redstonelight(WOOL)→"cloth", leaves/tnt(GRASS)→"grass", wood(WOOD)→"wood").
		return "gt.block.multitileentity." + aNameOfVanillaMaterialField + "." + soundName(aSoundType) + "." + aTool + "." + aHarvestLevelOffset + "." + aHarvestLevelMinimum + "." + aHarvestLevelMaximum + "." + aOpaque + "." + aNormalCube;
	}

	/** 1.7.10 SoundType.soundName equivalent for the MTE key (checked against the golden dump); any other
	 *  SoundType falls back to its break-sound path, which shows up as a diff, not a silent wrong answer. */
	private static String soundName(SoundType aSoundType) {
		if (aSoundType == SoundType.WOOL)  return "cloth";
		if (aSoundType == SoundType.METAL) return "stone";
		if (aSoundType == SoundType.STONE) return "stone";
		if (aSoundType == SoundType.WOOD)  return "wood";
		if (aSoundType == SoundType.GRASS) return "grass";
		return aSoundType.getBreakSound().getLocation().toString();
	}
	
	/**
	 * @param aNameOfVanillaMaterialField the Name of the vanilla Material Field. In case this is not a vanilla Material, insert the Name you want to give your own Material instead.
	 * @param aVanillaMaterial the Material used to determine the Block.
	 * @param aSoundType the Sound Type of the Block.
	 * @param aTool the Tool used to harvest this Block.
	 * @param aHarvestLevelOffset
	 * @param aHarvestLevelMinimum
	 * @param aHarvestLevelMaximum
	 * @param aOpaque if this Block is Opaque.
	 * @param aNormalCube if this Block is a normal Cube (for Redstone Stuff).
	 */
	public static MultiTileEntityBlock getOrCreate(String aModID, String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		MultiTileEntityBlock rBlock = MULTITILEENTITYBLOCKMAP.get(aModID + ":" + getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool = aTool.toLowerCase(), aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube));
		return rBlock == null ? MultiTileEntityBlockWithCompat.create(aModID, aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube) : rBlock;
	}
	
	/**
	 * @param aNameOfVanillaMaterialField the Name of the vanilla Material Field. In case this is not a vanilla Material, insert the Name you want to give your own Material instead.
	 * @param aVanillaMaterial the Material used to determine the Block.
	 * @param aSoundType the Sound Type of the Block.
	 * @param aTool the Tool used to harvest this Block.
	 * @param aHarvestLevelOffset
	 * @param aHarvestLevelMinimum
	 * @param aHarvestLevelMaximum
	 * @param aOpaque if this Block is Opaque.
	 * @param aNormalCube if this Block is a normal Cube (for Redstone Stuff).
	 */
	// Properties at construction set the step sound plus noOcclusion for non-opaque blocks
	// (otherwise render treats them as solid and blocks light); setId is mandatory.
	private static net.minecraft.world.level.block.state.BlockBehaviour.Properties mkProps(SoundType aSoundType, String aRegName, boolean aOpaque, String aTool, Material aVanillaMaterial) {
		// dynamicShape() is mandatory: without it neo caches getCollisionShape once and ignores the per-BE
		// shape bridge below, so snow/collision/isFaceSturdy all read a cached full cube.
		net.minecraft.world.level.block.state.BlockBehaviour.Properties p = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().dynamicShape().forceSolidOn().sound(aSoundType);
		if (!aOpaque) p = p.noOcclusion();
		// 1.7.10 asked block hardness positionally and GT6 answered from the TE, defaulting to 1.0F when absent;
		// neo's default (0) made every GT6 machine look instantly breakable, which is also why Jade stayed silent about its tool.
		p = p.destroyTime(1.0F);
		// 1.7.10 decided "does this need a tool" by MATERIAL (isToolNotRequired), not by the harvest-tool
		// string; the old gate wrongly required a tool for everything, including ropes that should break by hand.
		if (aTool != null && !aTool.isEmpty() && aVanillaMaterial != null && !aVanillaMaterial.isToolNotRequired()) p = p.requiresCorrectToolForDrops();
		return p;
	}
	protected MultiTileEntityBlock(String aModID, String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		// setId in Properties (neo requires it); namespace=GT for gt.multitileentity content.
		// Construction happens at RegisterEvent via GT_API.deferBlockInit. Step sound and noOcclusion come from mkProps below.
		super(mkProps(aSoundType, getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube), aOpaque, aTool, aVanillaMaterial));
		mMaterial = aVanillaMaterial;
		if (GAPI.mStartedInit) throw new IllegalStateException("Blocks can only be initialised within preInit!");
		
		mNameInternal = getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube);
		ST.register(this, mNameInternal, BlockItem.class);
		
		MULTITILEENTITYBLOCKMAP.put(aModID + ":" + mNameInternal, this);
		
		// The step sound is wired through mkProps above (.sound() at construction). Not a stub.
		mOpaque = aOpaque;
		mNormalCube = aNormalCube;
		
		mTool = aTool.toLowerCase();
		mHarvestLevelOffset = aHarvestLevelOffset;
		mHarvestLevelMinimum = Math.max(0, aHarvestLevelMinimum);
		mHarvestLevelMaximum = Math.max(aHarvestLevelMinimum, aHarvestLevelMaximum);
		
		// Opaque is wired into Properties at construction (mkProps: non-opaque -> noOcclusion, correct
		// render/light); GT6's own isOpaqueCube/getLightOpacity still read mOpaque for internal logic. Not a stub.

		if (MD.Mek.mLoaded) try {MekanismAPI.addBoxBlacklist(this, W);} catch(Throwable e) {e.printStackTrace(ERR);}
		// F12-followup (block-split): ST.hide → ST.make (ItemStack) → server-start → deferItemInit.
		gregapi.GT_API.deferItemInit(() -> ST.hide(this));
	}
	
	// @Override
	/** true means the BlockEntity must NOT be removed (the IMTE_BreakBlock veto fired); every other outcome
	 *  is false, and the caller must let the engine remove it via super.onRemove (see there). */
	public final boolean breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMetaData) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		// Was aTileEntity.shouldRefresh(...) (removed from neo's BlockEntity entirely); GT6 implements it itself
		// on TileEntityBase01Root as an ordinary method now, routed through a cast; a non-GT6 TE defaults to refresh=true.
		boolean tShouldRefresh = !(aTileEntity instanceof gregapi.tileentity.base.TileEntityBase01Root) || ((gregapi.tileentity.base.TileEntityBase01Root)aTileEntity).shouldRefresh(this, aBlock, aMetaData, aMetaData, aWorld, aX, aY, aZ);
		// Early exits here don't mean "leave the BE": the only "leave it" case is the interface veto below;
		// everything else lets the engine remove it via the caller's super.onRemove, matching 1.7.10's own safety net.
		if (aTileEntity == null || !tShouldRefresh) return F;
		if (aTileEntity instanceof IMTE_BreakBlock && ((IMTE_BreakBlock)aTileEntity).breakBlock()) return T; // contract: return true to prevent the TileEntity from being removed
		if (aTileEntity instanceof IMTE_HasMultiBlockMachineRelevantData && ((IMTE_HasMultiBlockMachineRelevantData)aTileEntity).hasMultiBlockMachineRelevantData()) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(aWorld, aX, aY, aZ, this, (byte)aMetaData, T);
		aWorld.removeBlockEntity(new BlockPos(aX, aY, aZ)); // Was aWorld.removeTileEntity(x,y,z) (1.7.10 World); neo is Level.removeBlockEntity(BlockPos).
		return F;
	}

	/** This body had no caller at all: the engine's own removal hook (BlockBehaviour.onRemove) never called
	 *  it, so drop-on-break for MTE containers and multiblock update notification were both silently dead. */
	/** Regression fix: the first bridge called only breakBlock, not super, taking over the engine's own
	 *  unconditional duty to remove the BlockEntity, leaving a ghost BE on early exits; super now runs after, unless vetoed. */
	@Override public void onRemove(BlockState aState, Level aWorld, BlockPos aPos, BlockState aNewState, boolean aMovedByPiston) {
		boolean tKeepBlockEntity = breakBlock(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aState.getBlock(), blockMetaDataAt(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));
		if (!tKeepBlockEntity) {super.onRemove(aState, aWorld, aPos, aNewState, aMovedByPiston); sweepBlockEntityRemains(aWorld, aPos, aNewState);}
	}

	/** super.onRemove's own removal misses a BlockEntity in two legitimate chunk states: one where the entry
	 *  is still an unpacked pending record, or the chunk isn't loaded yet; the sweep unpacks it so nothing saves as a ghost. */
	private static void sweepBlockEntityRemains(Level aWorld, BlockPos aPos, BlockState aNewState) {
		if (aWorld == null || aNewState.hasBlockEntity()) return;
		try {
			net.minecraft.world.level.chunk.LevelChunk tChunk = aWorld.getChunkAt(aPos);
			if (tChunk == null) return;
			BlockEntity tRemains = tChunk.getBlockEntity(aPos); // unpacks the pending record (see the analysis above)
			if (tRemains == null) return;
			tChunk.removeBlockEntity(aPos);
			if (!tRemains.isRemoved()) tRemains.setRemoved(); // window where the chunk isn't loaded yet: the normal removal path is a no-op here
		} catch (Throwable e) {e.printStackTrace(ERR);}
	}

	// Was @Override Block.getMapColor(int) (removed in neo); kept as an ordinary GT6 method, not an engine
	// override. super.getMapColor(meta) becomes mMaterial.getMaterialMapColor(), the same default source, 1:1.
	public MapColor getMapColor(int aMeta) {
		return mMapColor == null ? mMaterial.getMaterialMapColor() : mMapColor;
	}
	public MultiTileEntityBlock setMapColor(MapColor aMapColor) {
		mMapColor = aMapColor;
		return this;
	}
	// Bridges the int-meta getMapColor above onto IBlockExtension's engine hook, through the shared
	// MapColor.toNeo() center; returns this block's own color, or the default on null.
	@Override public final net.minecraft.world.level.material.MapColor getMapColor(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.level.material.MapColor aDefaultColor) {
		MapColor tGT = getMapColor(0);
		return tGT != null ? tGT.toNeo() : aDefaultColor;
	}
	
	private static boolean LOCK = F;
	
	// You want to override one of those Functions with your TileEntity? Just implement the Interfaces of MultiTileEntityInterfaces on your TileEntity and you are done.
	@Override public final void receiveDataName     (BlockGetter aWorld, int aX, int aY, int aZ, String aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMultiTileEntity) ((IMultiTileEntity)aTileEntity).setCustomName(aData);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte   aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByte       ) if (((IMTE_SyncDataByte       )aTileEntity).receiveDataByte       (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short  aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataShort      ) if (((IMTE_SyncDataShort      )aTileEntity).receiveDataShort      (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int    aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataInteger    ) if (((IMTE_SyncDataInteger    )aTileEntity).receiveDataInteger    (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long   aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataLong       ) if (((IMTE_SyncDataLong       )aTileEntity).receiveDataLong       (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByteArray  ) if (((IMTE_SyncDataByteArray  )aTileEntity).receiveDataByteArray  (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	/** 1.7.10's sync contract updates an EXISTING client TE and only creates a new one when there isn't one;
	 *  the port recreated it unconditionally on every sync, throwing away a mature client BE. Reuse is centralized here. */
	private static BlockEntity reuseOrCreateClientTE(Level aWorld, int aX, int aY, int aZ, short aID1, short aID2, INetworkHandler aNetworkHandler, boolean aSnapshotHasCovers) {
		BlockEntity tExisting = WD.te(aWorld, aX, aY, aZ, T);
		if (tExisting instanceof IMultiTileEntity tMTE && !tExisting.isRemoved()
		 && tMTE.getMultiTileEntityRegistryID() == aID1 && tMTE.getMultiTileEntityID() == aID2) {
			// A packet's IDs are a FULL client-state snapshot, and 1.7.10 closed even what wasn't in it by always
			// recreating the TE; reuse means the one conditional part, covers, must now be actively cleared when absent.
			if (!aSnapshotHasCovers && tExisting instanceof IMTE_SyncDataCovers tCovers) tCovers.receiveDataCovers((short[])null, (short[])null, aNetworkHandler);
			return tExisting;
		}
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1);
		if (tRegistry == null) return null;
		BlockEntity aTileEntity = tRegistry.getNewTileEntity(aWorld, aX, aY, aZ, aID2);
		if (aTileEntity == null) return null;
		WD.te(aWorld, aX, aY, aZ, aTileEntity, F);
		return aTileEntity;
	}

	@Override public final void receiveData         (BlockGetter aWorld, int aX, int aY, int aZ              , INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, F); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte   aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, F); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByte     ) ((IMTE_SyncDataByte     )aTileEntity).receiveDataByte     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short  aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, F); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataShort    ) ((IMTE_SyncDataShort    )aTileEntity).receiveDataShort    (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int    aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, F); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataInteger  ) ((IMTE_SyncDataInteger  )aTileEntity).receiveDataInteger  (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long   aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, F); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataLong     ) ((IMTE_SyncDataLong     )aTileEntity).receiveDataLong     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, F); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByteArray) ((IMTE_SyncDataByteArray)aTileEntity).receiveDataByteArray(aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveData         (BlockGetter aWorld, int aX, int aY, int aZ              , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, T); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte aData  , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, T); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataByte     ) ((IMTE_SyncDataByte     )aTileEntity).receiveDataByte     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short aData , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, T); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataShort    ) ((IMTE_SyncDataShort    )aTileEntity).receiveDataShort    (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int aData   , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, T); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataInteger  ) ((IMTE_SyncDataInteger  )aTileEntity).receiveDataInteger  (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long aData  , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, T); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataLong     ) ((IMTE_SyncDataLong     )aTileEntity).receiveDataLong     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = reuseOrCreateClientTE((Level)aWorld, aX, aY, aZ, aID1, aID2, aNetworkHandler, T); if (aTileEntity == null) return; if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataByteArray) ((IMTE_SyncDataByteArray)aTileEntity).receiveDataByteArray(aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveData         (BlockGetter aWorld, int aX, int aY, int aZ              , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte aData  , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByte     ) ((IMTE_SyncDataByte     )aTileEntity).receiveDataByte     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short aData , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataShort    ) ((IMTE_SyncDataShort    )aTileEntity).receiveDataShort    (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int aData   , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataInteger  ) ((IMTE_SyncDataInteger  )aTileEntity).receiveDataInteger  (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long aData  , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataLong     ) ((IMTE_SyncDataLong     )aTileEntity).receiveDataLong     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByteArray) ((IMTE_SyncDataByteArray)aTileEntity).receiveDataByteArray(aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	public final boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_GetBlocksMovement) || ((IMTE_GetBlocksMovement)aTileEntity).getBlocksMovement();}
	// Was super.addCollisionBoxesToList (removed from neo entirely, box-list collision replaced by
	// VoxelShape); the vanilla default's own algorithm is inlined here 1:1 instead of a super call.
	@SuppressWarnings("unchecked") public final void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, @SuppressWarnings("rawtypes") List aList, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_AddCollisionBoxesToList) ((IMTE_AddCollisionBoxesToList)aTileEntity).addCollisionBoxesToList(aAABB, aList, aEntity); else if (aTileEntity != null) {AABB tBox = getCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ); if (tBox != null && aAABB.intersects(tBox)) aList.add(tBox);}}
	public final AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetCollisionBoundingBoxFromPool ? ((IMTE_GetCollisionBoundingBoxFromPool)aTileEntity).getCollisionBoundingBoxFromPool() : aTileEntity == null ? null : new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	// 1.7.10's box-list collision has no VoxelShape equivalent on Block, so without this bridge MTE blocks
	// defaulted to a full cube, breaking snow placement and letting players suffocate inside walkable MTEs.
	private static final class ShapeKey {
		private final double[] mCoords; private final int mHash;
		ShapeKey(double[] aCoords) {mCoords = aCoords; mHash = java.util.Arrays.hashCode(aCoords);}
		@Override public int hashCode() {return mHash;}
		@Override public boolean equals(Object aOther) {return aOther instanceof ShapeKey tKey && java.util.Arrays.equals(mCoords, tKey.mCoords);}
	}
	private static final java.util.concurrent.ConcurrentHashMap<ShapeKey, net.minecraft.world.phys.shapes.VoxelShape> SHAPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static net.minecraft.world.phys.shapes.VoxelShape cachedShape(List<AABB> aBoxes, BlockPos aPos) {
		if (SHAPE_CACHE.size() > 8192) SHAPE_CACHE.clear(); // safety margin (shapes are discrete -- the real size is in the hundreds)
		double[] tCoords = new double[aBoxes.size() * 6];
		int i = 0;
		for (AABB tBox : aBoxes) {
			tCoords[i++] = tBox.minX - aPos.getX(); tCoords[i++] = tBox.minY - aPos.getY(); tCoords[i++] = tBox.minZ - aPos.getZ();
			tCoords[i++] = tBox.maxX - aPos.getX(); tCoords[i++] = tBox.maxY - aPos.getY(); tCoords[i++] = tBox.maxZ - aPos.getZ();
		}
		return SHAPE_CACHE.computeIfAbsent(new ShapeKey(tCoords), aKey -> {
			net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.empty();
			for (int j = 0; j < aKey.mCoords.length; j += 6)
				rShape = net.minecraft.world.phys.shapes.Shapes.or(rShape, net.minecraft.world.phys.shapes.Shapes.create(new AABB(aKey.mCoords[j], aKey.mCoords[j+1], aKey.mCoords[j+2], aKey.mCoords[j+3], aKey.mCoords[j+4], aKey.mCoords[j+5])));
			return rShape;
		});
	}

	@Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld == null) return super.getCollisionShape(aState, aWorld, aPos, aContext);
		BlockEntity tTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (tTileEntity instanceof IMTE_AddCollisionBoxesToList tMulti) {
			List<AABB> tList = new ArrayList<>();
			tMulti.addCollisionBoxesToList(new AABB(aPos.getX()-1, aPos.getY()-1, aPos.getZ()-1, aPos.getX()+2, aPos.getY()+2, aPos.getZ()+2), tList, aContext instanceof net.minecraft.world.phys.shapes.EntityCollisionContext tEntityContext ? tEntityContext.getEntity() : null);
			if (tList.isEmpty()) return net.minecraft.world.phys.shapes.Shapes.empty();
			return cachedShape(tList, aPos); // cached voxelization
		}
		if (tTileEntity instanceof IMTE_GetCollisionBoundingBoxFromPool tPool) {
			AABB tBox = tPool.getCollisionBoundingBoxFromPool();
			return tBox == null ? net.minecraft.world.phys.shapes.Shapes.empty() : cachedShape(java.util.List.of(tBox), aPos); // same fix
		}
		return super.getCollisionShape(aState, aWorld, aPos, aContext); // no TE or no matching interface -> full cube (1:1 with the old pool fallback)
	}
	@Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld instanceof Level tLevel) {
			BlockEntity tTileEntity = WD.te(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			if (tTileEntity instanceof IMTE_GetSelectedBoundingBoxFromPool tSel) {
				AABB tBox = tSel.getSelectedBoundingBoxFromPool();
				if (tBox != null) return cachedShape(java.util.List.of(tBox), aPos); // cached voxelization
			}
		}
		return super.getShape(aState, aWorld, aPos, aContext); // ordinary MTE (machines/chests) -> full cube, as before
	}
	public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_UpdateTick) ((IMTE_UpdateTick)aTileEntity).updateTick(aRandom);}
	public final void onBlockDestroyedByPlayer(Level aWorld, int aX, int aY, int aZ, int aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockDestroyedByPlayer) ((IMTE_OnBlockDestroyedByPlayer)aTileEntity).onBlockDestroyedByPlayer(aRandom);}
	// neo calls onPlace(BlockState, Level, BlockPos, BlockState, boolean) instead of the old onBlockAdded.
	@Override public final void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnBlockAdded) ((IMTE_OnBlockAdded)aTileEntity).onBlockAdded();}
	// Was super.dropXpOnBlockBreak (removed from neo entirely); the vanilla default's own XP-orb spawn loop
	// is inlined here 1:1 via ExperienceOrb.award, the same split algorithm as before.
	public final void dropXpOnBlockBreak(Level aWorld, int aX, int aY, int aZ, int aXP) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_DropXpOnBlockBreak) ((IMTE_DropXpOnBlockBreak)aTileEntity).dropXpOnBlockBreak(aXP); else if (!aWorld.isClientSide() && aWorld instanceof ServerLevel aServerWorld) ExperienceOrb.award(aServerWorld, new Vec3(aX+0.5, aY+0.5, aZ+0.5), aXP);}
	// 1.7.10's collisionRayTrace has no neo hook (raytracing is generic over VoxelShape/getShape now);
	// the TE interface has zero implementors -- a dead compile surface, not a stub, since there's nothing to lose.
	public final HitResult collisionRayTrace(Level aWorld, int aX, int aY, int aZ, Vec3 aVectorA, Vec3 aVectorB) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CollisionRayTrace ? ((IMTE_CollisionRayTrace)aTileEntity).collisionRayTrace(aVectorA, aVectorB) : null;}
	// 1.20.1's click channel is one public method again, use(), matching 1.7.10's onBlockActivated exactly;
	// without this bridge stacking items by click and opening machine GUIs both silently stopped working.
	@Override public net.minecraft.world.InteractionResult use(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer, net.minecraft.world.InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
		if (aHand == net.minecraft.world.InteractionHand.MAIN_HAND && bridgeBlockActivated(aWorld, aPos, aPlayer, aHit))
			return net.minecraft.world.InteractionResult.SUCCESS;
		return net.minecraft.world.InteractionResult.PASS;
	}
	private boolean bridgeBlockActivated(Level aWorld, BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		net.minecraft.world.phys.Vec3 tHitVec = aHit.getLocation();
		return onBlockActivated(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aPlayer, aHit.getDirection().get3DDataValue(),
			(float)(tHitVec.x - aPos.getX()), (float)(tHitVec.y - aPos.getY()), (float)(tHitVec.z - aPos.getZ()));
	}

	public final boolean onBlockActivated(Level aWorld, int aX, int aY, int aZ, Player aPlayer, int aSide, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aPlayer != null && IL.TC_Thaumometer.equal(aPlayer.getMainHandItem(), T, T) && (!(aTileEntity instanceof ITileEntityBookShelf) || !((ITileEntityBookShelf)aTileEntity).isShelfFace(UT.Code.side(aSide)))) return F; return aTileEntity instanceof IMTE_OnBlockActivated && ((IMTE_OnBlockActivated)aTileEntity).onBlockActivated(aPlayer, UT.Code.side(aSide), aHitX, aHitY, aHitZ);}
	public final void onEntityWalking(Level aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnEntityWalking) ((IMTE_OnEntityWalking)aTileEntity).onEntityWalking(aEntity);}
	// Was onBlockClicked(World,x,y,z,EntityPlayer) -> BlockBehaviour.attack(BlockState,Level,BlockPos,Player).
	@Override public final void attack(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnBlockClicked) ((IMTE_OnBlockClicked)aTileEntity).onBlockClicked(aPlayer); else super.attack(aState, aWorld, aPos, aPlayer);}
	// 1.7.10's Block.velocityToAddToEntity is gone (its vanilla default was empty anyway); the TE
	// interface has zero implementors -- a dead surface, not a stub.
	public final void velocityToAddToEntity(Level aWorld, int aX, int aY, int aZ, Entity aEntity, Vec3 aVector) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_VelocityToAddToEntity) ((IMTE_VelocityToAddToEntity)aTileEntity).velocityToAddToEntity(aEntity, aVector);}
	// neo asks weak redstone power through BlockBehaviour.getSignal instead of the old isProvidingWeakPower.
	@Override public final int getSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(UT.Code.side(aSide)) : super.getSignal(aState, aWorld, aPos, aSide);}
	// Was onEntityCollidedWithBlock(World,x,y,z,Entity); neo's entityInside gained effectApplier/isPrecise
	// params (no 1.7.10 analog) that GT6 doesn't use, since it never used them before either.
	@Override public final void entityInside(BlockState aState, Level aWorld, BlockPos aPos, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnEntityCollidedWithBlock) ((IMTE_OnEntityCollidedWithBlock)aTileEntity).onEntityCollidedWithBlock(aEntity); else super.entityInside(aState, aWorld, aPos, aEntity);}
	// neo asks strong redstone power through getDirectSignal instead of the old isProvidingStrongPower.
	@Override public final int getDirectSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_IsProvidingStrongPower ? ((IMTE_IsProvidingStrongPower)aTileEntity).isProvidingStrongPower(UT.Code.side(aSide)) : super.getDirectSignal(aState, aWorld, aPos, aSide);}
	public final boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_CanBlockStay) || ((IMTE_CanBlockStay)aTileEntity).canBlockStay();}
	// Was onFallenUpon(World,x,y,z,Entity,dist) -> Block.fallOn(Level,BlockState,BlockPos,Entity,double);
	// fall distance is now double, not float.
	@Override public final void fallOn(Level aWorld, BlockState aState, BlockPos aPos, Entity aEntity, float aFallDistance) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnFallenUpon) ((IMTE_OnFallenUpon)aTileEntity).onFallenUpon(aEntity, aFallDistance); else super.fallOn(aWorld, aState, aPos, aEntity, aFallDistance);}
	// 1.7.10's onBlockHarvested/onBlockPreDestroy are both gone; neo's playerWillDestroy fires before
	// block removal, and this hook dispatches both original TE hooks from it 1:1.
	@Override public void playerWillDestroy(Level aWorld, BlockPos aPos, BlockState aState, Player aPlayer) {
		BlockEntity tTE = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		int tMeta = WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (tTE instanceof IMTE_OnBlockHarvested) ((IMTE_OnBlockHarvested)tTE).onBlockHarvested(tMeta, aPlayer);
		if (tTE instanceof IMTE_OnBlockPreDestroy) ((IMTE_OnBlockPreDestroy)tTE).onBlockPreDestroy(tMeta);
		super.playerWillDestroy(aWorld, aPos, aState, aPlayer);
	}
	public final void onBlockHarvested(Level aWorld, int aX, int aY, int aZ, int aMetaData, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockHarvested) ((IMTE_OnBlockHarvested)aTileEntity).onBlockHarvested(aMetaData, aPlayer);}
	public final void onBlockPreDestroy(Level aWorld, int aX, int aY, int aZ, int aMetaData) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockPreDestroy) ((IMTE_OnBlockPreDestroy)aTileEntity).onBlockPreDestroy(aMetaData);}
	// F13: 1.7.10 Block.fillWithRain(World,x,y,z) → neo Block.handlePrecipitation(state,level,pos,precipitation) (overridable).
	// Dispatches IMTE_FillWithRain on rain (was rain-specific fillWithRain), 1:1; the GT6 method itself is unchanged.
	@Override public void handlePrecipitation(BlockState aState, Level aWorld, BlockPos aPos, net.minecraft.world.level.biome.Biome.Precipitation aPrecipitation) {
		BlockEntity tTE = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (aPrecipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN && tTE instanceof IMTE_FillWithRain) ((IMTE_FillWithRain)tTE).fillWithRain();
		else super.handlePrecipitation(aState, aWorld, aPos, aPrecipitation);
	}
	public final void fillWithRain(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_FillWithRain) ((IMTE_FillWithRain)aTileEntity).fillWithRain();}
	// Was hasComparatorInputOverride()/getComparatorInputOverride -> hasAnalogOutputSignal/getAnalogOutputSignal;
	// default (no IMTE hook) is 0, matching the engine's own default.
	@Override public final boolean hasAnalogOutputSignal(BlockState aState) {return T;}
	// 1.20.1's getAnalogOutputSignal carries no side, same as 1.7.10's version; SIDE_UNKNOWN is passed,
	// the same input GT6 gives every other sideless query.
	@Override public final int getAnalogOutputSignal(BlockState aState, Level aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_GetComparatorInputOverride ? ((IMTE_GetComparatorInputOverride)aTileEntity).getComparatorInputOverride(SIDE_UNKNOWN) : aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(SIDE_UNKNOWN) : 0;}
	// The light engine calls getLightEmission DURING chunk generation, and a normal WD.te lookup forces the
	// generating chunk to finish first, deadlocking entry; a non-forcing lookup avoids this and defaults with no BE yet.
	@Override public final int getLightEmission(BlockState aState, BlockGetter aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.teNonForcing(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()); return aTileEntity instanceof IMTE_GetLightValue ? UT.Code.bind4(((IMTE_GetLightValue)aTileEntity).getLightValue()) : aState.getLightEmission();}
	public final boolean isLadder(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsLadder && ((IMTE_IsLadder)aTileEntity).isLadder(aEntity);}
	// Was isLadder(IBlockAccess,x,y,z,EntityLivingBase) -> IBlockExtension.isLadder; not the static
	// CLIMBABLE tag default, since LivingEntity calls this per-position hook directly, not through a tag.
	@Override public final boolean isLadder(BlockState aState, LevelReader aWorld, BlockPos aPos, LivingEntity aEntity) {return isLadder(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aEntity);}
	public final boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsNormalCube ? ((IMTE_IsNormalCube)aTileEntity).isNormalCube() : mNormalCube;}
	public final boolean isReplaceable(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsReplaceable ? ((IMTE_IsReplaceable)aTileEntity).isReplaceable() : getMaterial().isReplaceable();}
	public final boolean isBurning(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBurning && ((IMTE_IsBurning)aTileEntity).isBurning();}
	public final boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {if (aWorld == null) return F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsAir && ((IMTE_IsAir)aTileEntity).isAir();}
	// Was removedByPlayer(...) -> IBlockExtension.onDestroyedByPlayer.
	// Same contract: returns whether the block was actually destroyed.
	@Override public final boolean onDestroyedByPlayer(BlockState aBlockState, Level aWorld, BlockPos aPos, Player aPlayer, boolean aWillHarvest, FluidState aFluid) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); return aTileEntity instanceof IMTE_RemovedByPlayer ? ((IMTE_RemovedByPlayer)aTileEntity).removedByPlayer(aWorld, aPlayer, aWillHarvest) : super.onDestroyedByPlayer(aBlockState, aWorld, aPos, aPlayer, aWillHarvest, aFluid);}
	public final boolean canCreatureSpawn(MobCategory aType, BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanCreatureSpawn && ((IMTE_CanCreatureSpawn)aTileEntity).canCreatureSpawn(aType);}
	public final boolean isBed(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBed && ((IMTE_IsBed)aTileEntity).isBed(aPlayer);}
	// Bug fix: this cast used the wrong interface (IMTE_VelocityToAddToEntity instead of
	// IMTE_GetBedSpawnPosition); corrected.
	public final BlockPos getBedSpawnPosition(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBedSpawnPosition ? ((IMTE_GetBedSpawnPosition)aTileEntity).getBedSpawnPosition(aPlayer) : null;}
	public final void setBedOccupied(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer, boolean aOccupied) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_SetBedOccupied) ((IMTE_SetBedOccupied)aTileEntity).setBedOccupied(aPlayer, aOccupied);}
	public final int getBedDirection(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBedDirection ? ((IMTE_GetBedDirection)aTileEntity).getBedDirection() : 0;}
	public final boolean isBedFoot(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBedFoot && ((IMTE_IsBedFoot)aTileEntity).isBedFoot();}
	// Forge's Block.beginLeavesDecay has no neo hook. Zero implementors -- dead surface. Vanilla
	// default was an empty body, so no super call is 1:1 with doing nothing.
	public final void beginLeavesDecay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_BeginLeavesDecay) ((IMTE_BeginLeavesDecay)aTileEntity).beginLeavesDecay();}
	// Forge's Block.canSustainLeaves has no neo hook. Zero implementors -- dead surface. Vanilla
	// default was false, substituted directly instead of a super call. Not a stub.
	public final boolean canSustainLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanSustainLeaves ? ((IMTE_CanSustainLeaves)aTileEntity).canSustainLeaves() : F;}
	public final boolean isLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsLeaves && ((IMTE_IsLeaves)aTileEntity).isLeaves();}
	public final boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanBeReplacedByLeaves && ((IMTE_CanBeReplacedByLeaves)aTileEntity).canBeReplacedByLeaves();}
	public final boolean isWood(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsWood ? ((IMTE_IsWood)aTileEntity).isWood() : F;}// was super.isWood (Forge 1.7.10 default = false; neo's Block has no such method)
	public final boolean isReplaceableOreGen(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, Block aTarget) {if (GAPI.mStartedServerStarted < 1) return F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsReplaceableOreGen ? ((IMTE_IsReplaceableOreGen)aTileEntity).isReplaceableOreGen(aTarget) : (aTarget == this);}// was super.isReplaceableOreGen (Forge 1.7.10 default = identity, this==target)
	// Was canConnectRedstone(IBlockAccess,x,y,z,side).
	// -> IBlockExtension.canConnectRedstone(BlockState,BlockGetter,BlockPos,Direction).
	@Override public final boolean canConnectRedstone(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_CanConnectRedstone ? ((IMTE_CanConnectRedstone)aTileEntity).canConnectRedstone(UT.Code.side(aSide)) : super.canConnectRedstone(aState, aWorld, aPos, aSide);}
	public final boolean canPlaceTorchOnTop(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanPlaceTorchOnTop ? ((IMTE_CanPlaceTorchOnTop)aTileEntity).canPlaceTorchOnTop() : isSideSolid(aWorld, aX, aY, aZ, FORGE_DIR[SIDE_TOP]);}
	// Forge's Block.isFoliage has no neo hook. Zero implementors -- dead surface. Vanilla default
	// was false, substituted directly. Not a stub.
	public final boolean isFoliage(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFoliage ? ((IMTE_IsFoliage)aTileEntity).isFoliage() : F;}
	// 1.20.1's signature matches 1.7.10 verbatim again: canSustainPlant(...,IPlantable), with the real
	// IPlantable type, no TriState involved; default (no IMTE hook) is vanilla's own rule, same as before.
	@Override public final boolean canSustainPlant(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide, IPlantable aPlantable) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (!(aTileEntity instanceof IMTE_CanSustainPlant)) return super.canSustainPlant(aState, aWorld, aPos, aSide, aPlantable); return ((IMTE_CanSustainPlant)aTileEntity).canSustainPlant(UT.Code.side(aSide), aPlantable);}
	// 1.7.10's Block.onPlantGrow is gone from neo (no hook). Zero implementors -- dead surface.
	// Vanilla default was empty, so no super call is 1:1 with doing nothing. Not a stub.
	public final void onPlantGrow(Level aWorld, int aX, int aY, int aZ, int sX, int sY, int sZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnPlantGrow) ((IMTE_OnPlantGrow)aTileEntity).onPlantGrow(sX, sY, sZ);}
	public final boolean isFertile(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFertile && ((IMTE_IsFertile)aTileEntity).isFertile();}
	public final boolean rotateBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RotateBlock && ((IMTE_RotateBlock)aTileEntity).rotateBlock(UT.Code.side(aSide));}
	public final Direction[] getValidRotations(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetValidRotations ? ((IMTE_GetValidRotations)aTileEntity).getValidRotations() : ZL_FORGEDIRECTION;}
	public final float getEnchantPowerBonus(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetEnchantPowerBonus ? ((IMTE_GetEnchantPowerBonus)aTileEntity).getEnchantPowerBonus() : 0;}
	// Was getEnchantPowerBonus(World,...) -> IBlockExtension's version, which only guarantees a BlockGetter
	// (weaker than the old Level); delegates only when it really is a Level, else the same 0 default as without a TE.
	@Override public final float getEnchantPowerBonus(BlockState aState, net.minecraft.world.level.LevelReader aWorld, BlockPos aPos) {return aWorld instanceof Level tLevel ? getEnchantPowerBonus(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()) : 0;}
	public final boolean recolourBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide, int aColor) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RecolourBlock && ((IMTE_RecolourBlock)aTileEntity).recolourBlock(UT.Code.side(aSide), (byte)aColor);}
	public final boolean shouldCheckWeakPower(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_ShouldCheckWeakPower ? ((IMTE_ShouldCheckWeakPower)aTileEntity).shouldCheckWeakPower(UT.Code.side(aSide)) : isNormalCube(aWorld, aX, aY, aZ);}
	// Was shouldCheckWeakPower(IBlockAccess,...) -> IBlockExtension's version on SignalGetter (extends
	// BlockGetter, a direct delegate); Direction converts to int through the center UT.Code.side.
	@Override public final boolean shouldCheckWeakPower(BlockState aState, net.minecraft.world.level.SignalGetter aWorld, BlockPos aPos, Direction aSide) {return shouldCheckWeakPower(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), UT.Code.side(aSide));}
	// Was getWeakChanges(IBlockAccess,x,y,z) -> IBlockExtension.getWeakChanges; default false matches the old vanilla default.
	@Override public final boolean getWeakChanges(BlockState aState, LevelReader aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_GetWeakChanges ? ((IMTE_GetWeakChanges)aTileEntity).getWeakChanges() : F;}
	public final boolean addHitEffects(Level aWorld, HitResult aTarget, ParticleEngine aRenderer) {BlockEntity aTileEntity = WD.te(aWorld, ((BlockHitResult)aTarget).getBlockPos().getX(), ((BlockHitResult)aTarget).getBlockPos().getY(), ((BlockHitResult)aTarget).getBlockPos().getZ(), T); return aTileEntity instanceof IMTE_AddHitEffects && ((IMTE_AddHitEffects)aTileEntity).addHitEffects(aWorld, aTarget, aRenderer);}
	public final boolean addDestroyEffects(Level aWorld, int aX, int aY, int aZ, int aMetaData, ParticleEngine aRenderer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_AddDestroyEffects && ((IMTE_AddDestroyEffects)aTileEntity).addDestroyEffects(aMetaData, aRenderer);}
	// Was shouldSideBeRendered(...) -> BlockBehaviour.skipRendering, with INVERTED semantics and a signature
	// that dropped World/BlockPos entirely, making the old per-TE lookup impossible; falls back to the vanilla default.
	@Override public final boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {return super.skipRendering(aState, aNeighbor, aDir);}
	public final void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_SetBlockBoundsBasedOnState) ((IMTE_SetBlockBoundsBasedOnState)aTileEntity).setBlockBoundsBasedOnState(this); else if (aTileEntity == null) setBlockBounds(-999, -999, -999, -998, -998, -998); else setBlockBounds(0, 0, 0, 1, 1, 1);}
	public final AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity == null ? new AABB(-999, -999, -999, -998, -998, -998) : aTileEntity instanceof IMTE_GetSelectedBoundingBoxFromPool ? ((IMTE_GetSelectedBoundingBoxFromPool)aTileEntity).getSelectedBoundingBoxFromPool() : new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	// Was randomDisplayTick(World,x,y,z,Random) -> Block.animateTick(BlockState,Level,BlockPos,RandomSource).
	@Override public final void animateTick(BlockState aState, Level aWorld, BlockPos aPos, RandomSource aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_RandomDisplayTick) ((IMTE_RandomDisplayTick)aTileEntity).randomDisplayTick(aRandom); else super.animateTick(aState, aWorld, aPos, aRandom);}
	public final void onBlockExploded(Level aWorld, int aX, int aY, int aZ, Explosion aExplosion) {if (aWorld.isClientSide()) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); if (aTileEntity instanceof IMTE_OnBlockExploded) ((IMTE_OnBlockExploded)aTileEntity).onExploded(aExplosion); else WD.set(aWorld, aX, aY, aZ, NB, 0, 3);}
	// Was onBlockExploded(...) -> IBlockExtension.onBlockExploded/Block.wasExploded. GT6's method already
	// sets air in its fallback branch, matching neo's own default body, so a direct delegate without super/wasExploded is 1:1.
	@Override public final void onBlockExploded(BlockState aState, Level aWorld, BlockPos aPos, Explosion aExplosion) {onBlockExploded(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aExplosion);}
	// 1.7.10's getPickBlock is gone; neo's middle-click goes through IBlockExtension.getCloneItemStack
	// instead, which this hook delegates into GT6's own getPickBlock dispatcher, restoring the behavior 1:1.
	@Override public ItemStack getCloneItemStack(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.phys.HitResult aTarget, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, Player aPlayer) {
		BlockEntity tTE = WD.te(aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		ItemStack r = tTE instanceof IMTE_GetPickBlock ? ((IMTE_GetPickBlock)tTE).getPickBlock(null) : null;
		return ST.valid(r) ? r : super.getCloneItemStack(aState, aTarget, aLevel, aPos, aPlayer);
	}
	public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ                      ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	@Override public final ItemStack getItemStackFromBlock(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetStackFromBlock?((IMTE_GetStackFromBlock)aTileEntity).getStackFromBlock(aSide):null;}
	public final int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetFlammability ? ((IMTE_GetFlammability)aTileEntity).getFlammability(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	// Was getFlammability(IBlockAccess,...) -> IBlockExtension.getFlammability, a direct delegate (types match 1:1).
	@Override public final int getFlammability(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return getFlammability(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aSide);}
	public final int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetFireSpreadSpeed ? ((IMTE_GetFireSpreadSpeed)aTileEntity).getFireSpreadSpeed(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	// Was getFireSpreadSpeed(IBlockAccess,...) -> IBlockExtension.getFireSpreadSpeed, a direct delegate (types match 1:1).
	@Override public final int getFireSpreadSpeed(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return getFireSpreadSpeed(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aSide);}
	// 1.7.10's isFireSource DOES exist as a neo hook, but the TE interface has zero implementors
	// anywhere in the tree -- nothing to wire a bridge to.
	public final boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFireSource && ((IMTE_IsFireSource)aTileEntity).isFireSource(UT.Code.side(aSide));}
	public final boolean canEntityDestroy(BlockGetter aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_CanEntityDestroy) || ((IMTE_CanEntityDestroy)aTileEntity).canEntityDestroy(aEntity);}
	// Was canEntityDestroy(IBlockAccess,...) -> IBlockExtension.canEntityDestroy, a direct delegate (types match 1:1).
	@Override public final boolean canEntityDestroy(BlockState aState, BlockGetter aWorld, BlockPos aPos, Entity aEntity) {return canEntityDestroy(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aEntity);}
	@Override public final long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_OnToolClick ? ((IMTE_OnToolClick)aTileEntity).onToolClick(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ) : 0;}
	@Override public final OreDictMaterialStack getMaterialAtSide(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetMaterialAtSide?((IMTE_GetMaterialAtSide)aTileEntity).getMaterialAtSide(aSide):null;}
	@Override public final boolean removeMaterialFromSide(Level aWorld, int aX, int aY, int aZ, byte aSide, OreDictMaterialStack aMaterial) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RemoveMaterialFromSide && ((IMTE_RemoveMaterialFromSide)aTileEntity).removeMaterialFromSide(aSide, aMaterial);}
	public final void dropBlockAsItemWithChance(Level aWorld, int aX, int aY, int aZ, int aMeta, float aChance, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, F, LAST_HARVESTING_PLAYER.get()); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	// Was EnchantmentHelper.getSilkTouchModifier/getFortuneModifier (both removed from neo); the real
	// replacement is getEnchantmentLevel by Holder from RegistryAccess, already reviewed and accepted in GT_API_Proxy.
	public final void harvestBlock(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {if (aPlayer == null) aPlayer = LAST_HARVESTING_PLAYER.get(); aPlayer.awardStat(Stats.BLOCK_MINED.get(this), 1); /* was Stats.mineBlockStatArray[int id] (1.7.10) -> Stats.BLOCK_MINED.get(Block) + Player.awardStat */ UT.Entities.exhaust(aPlayer, 0.025F); Enchantment tSilkTouchHolder = Enchantments.SILK_TOUCH; Enchantment tFortuneHolder = Enchantments.BLOCK_FORTUNE; boolean aSilkTouch = EnchantmentHelper.getEnchantmentLevel(tSilkTouchHolder, aPlayer) > 0; int aFortune = EnchantmentHelper.getEnchantmentLevel(tFortuneHolder, aPlayer); float aChance = 1.0F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, aSilkTouch); aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, aSilkTouch, aPlayer); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	public final ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aUnusableMetaData, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) return ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); return ST.arraylist();}
	// 1.7.10's harvestBlock was a real engine override; neo has no method by that name or signature at all.
	// The real drop hook is now playerDestroy; without this bridge every MTE dropped nothing, having no loot table.
	@Override public void playerDestroy(Level aWorld, Player aPlayer, BlockPos aPos, BlockState aState, BlockEntity aBlockEntity, ItemStack aDestroyedWith) {harvestBlock(aWorld, aPlayer, aPos.getX(), aPos.getY(), aPos.getZ(), 0);}
	// The MTE family has no loot table: every engine drop path except playerDestroy (explosions, pistons, destroyBlock)
	// dropped nothing, while 1.7.10 served them all through getDrops(fortune) via LAST_BROKEN_TILEENTITY.
	@Override public java.util.List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList();
		BlockEntity tBE = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
		if (tBE == null) {
			net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
			if (tOrigin == null) return super.getDrops(aState, aParams);
			tBE = WD.te(aParams.getLevel(), net.minecraft.util.Mth.floor(tOrigin.x), net.minecraft.util.Mth.floor(tOrigin.y), net.minecraft.util.Mth.floor(tOrigin.z), T);
		}
		if (!(tBE instanceof IMTE_GetDrops tDrops)) return java.util.Collections.emptyList();
		ArrayListNoNulls<ItemStack> rList = tDrops.getDrops(WD.lootFortune(aParams), WD.lootSilkTouch(aParams));
		if (rList == null) return java.util.Collections.emptyList();
		// Branch 1.20.1: the global loot modifier does not see blocks without loot tables, so drop processing is called from here.
		gregapi.GT_API_Proxy.processBlockDrops(rList, aParams.getLevel(), tBE.getBlockPos(), aState, aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY));
		return rList;
	}
	// Was aPlayer.level().getTileEntity(x,y,z) (1.7.10 World) -> the center WD.te(...), the same trick as
	// every other TE lookup in this file, not a direct engine call.
	@Override public final ArrayList<String> getDebugInfo(Player aPlayer, int aX, int aY, int aZ, int aScanLevel) {BlockEntity aTileEntity = WD.te(aPlayer.level(), aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetDebugInfo ? ((IMTE_GetDebugInfo)aTileEntity).getDebugInfo(aScanLevel) : null;}
	// Not implemented: 1.7.10's isSideSolid was a real per-side Forge override, but the real neo interface
	// has no such method -- face solidity comes purely from VoxelShape; a true per-face answer is new geometry, deferred.
	public final boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsSideSolid?((IMTE_IsSideSolid)aTileEntity).isSideSolid(UT.Code.side(aSide)):mOpaque;}
	public final boolean isBeaconBase(BlockGetter aWorld, int aX, int aY, int aZ, int aBeaconX, int aBeaconY, int aBeaconZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBeaconBase && ((IMTE_IsBeaconBase)aTileEntity).isBeaconBase(aBeaconX, aBeaconY, aBeaconZ);}
	public final int getLightOpacity(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetLightOpacity?((IMTE_GetLightOpacity)aTileEntity).getLightOpacity():mOpaque?LIGHT_OPACITY_MAX:LIGHT_OPACITY_NONE;}
	public final boolean isOpaqueCube() {return mOpaque;}
	public final boolean func_149730_j() {return mOpaque;}
	public final boolean renderAsNormalBlock() {return mOpaque || mNormalCube;}

	// Shade bridge (MTE is a separate hierarchy from BlockBase, inheriting Block directly): machines/
	// pipes/covers without mOpaque/mNormalCube never darkened neighbors in 1.7.10; neo's default darkens all around them.
	@Override public float getShadeBrightness(BlockState aState, BlockGetter aWorld, BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** Body 1:1 with 1.7.10's Block.isBlockNormalCube; see BlockBase for the same method. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}
	public final boolean isNormalCube()  {return mNormalCube;}
	// neo asks this through isSignalSource instead of the old canProvidePower.
	@Override public final boolean isSignalSource(BlockState aState) {return !mNormalCube;}
	@Override public final Block getBlock() {return this;}
	public final String getUnlocalizedName() {return mNameInternal;}
	// 1.7.10's vanilla @Override Block.getLocalizedName is gone from neo (no such method), so @Override is
	// dropped, but the method is functional through the localization center LH.get. Not a stub.
	public final String getLocalizedName() {return LH.get(mNameInternal);}
	public final String getHarvestTool(int aMeta) {return mTool;}
	public final boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public final int getHarvestLevel(int aMeta) {return (int)UT.Code.bind_(mHarvestLevelMinimum, mHarvestLevelMaximum, mHarvestLevelOffset + aMeta);}
	/** 1.7.10 kept this value in the MTE class's own mBlockMetaData (set from the material's mToolQuality
	 *  at registration); block meta can't be a BlockState property here, so it's read from the class standing at this position. */
	@Override public int getHarvestLevel(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getHarvestLevel(blockMetaDataAt(aWorld, aX, aY, aZ));
	}
	/** The 1.7.10-style MTE block meta at a position: the class's own mBlockMetaData.
	 *  0 if there's no BE yet, matching an empty meta. */
	public static int blockMetaDataAt(BlockGetter aWorld, int aX, int aY, int aZ) {
		try {
			BlockEntity tTileEntity = aWorld.getBlockEntity(new BlockPos(aX, aY, aZ));
			if (!(tTileEntity instanceof IMultiTileEntity tMTE)) return 0;
			MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(tMTE.getMultiTileEntityRegistryID());
			if (tRegistry == null) return 0;
			MultiTileEntityClassContainer tClass = tRegistry.getClassContainer(tMTE.getMultiTileEntityID());
			return tClass == null ? 0 : tClass.mBlockMetaData;
		} catch (Throwable e) {return 0;}
	}
	// On 1.20.1, was canHarvestBlock(EntityPlayer,meta) -> IForgeBlock.canHarvestBlock; the old
	// passthrough judged the rule without a position, so the required level always came out 0; now calls WD.canHarvestBlock.
	@Override public final boolean canHarvestBlock(BlockState aState, BlockGetter aWorld, BlockPos aPos, Player aPlayer) {return gregapi.util.WD.canHarvestBlock(aState, aWorld, aPos, aPlayer);}
	public final boolean hasTileEntity(int aMeta) {return T;}
	public final boolean canSilkHarvest() {return F;}
	public final int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	public final BlockEntity createNewTileEntity(Level aWorld, int aMeta) {return null;}
	public final BlockEntity createTileEntity(Level aWorld, int aMeta) {return null;}
	// GT6's own TE creation for this block doesn't go through EntityBlock.newBlockEntity at all (it always
	// returned null even in 1.7.10); returning a real stub here satisfies the contract so the engine's WARN never fires.
	@Override public final BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {return new gregapi.tileentity.base.TileEntityLoaderStub(aPos, aState);}
	// 1.7.10's World ticked every TileEntity with canUpdate()==true itself; neo only ticks a BlockEntity
	// that getTicker returns a ticker for. The ENGINE filters now, by block-entity TYPE, not a per-instance lambda.
	@Override public final <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, net.minecraft.world.level.block.entity.BlockEntityType<T> aType) {
		if (aType != gregapi.tileentity.base.TileEntityBase01Root.MTE_TYPE) return null;
		return (tLevel, tPos, tState, tBE) -> {
			if (tBE instanceof gregapi.tileentity.base.TileEntityBase01Root tTE && tTE.canUpdate()) tTE.updateEntity();
		};
	}
	public final void getSubBlocks(Item aItem, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {/**/}
	@Override public final ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override public final boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public final int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 0;}
	@Override public final ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return null;}
	@Override public final boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return F;}
	@Override public final int getRenderPasses(ItemStack aStack) {return 0;}
	@Override public final IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return null;}
	public final void registerBlockIcons(Object aIconRegister) {/**/}
	public final ResourceLocation getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	public final ResourceLocation getIcon(int aSide, int aMetaData) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	// 1.7.10's Block.getRenderType is gone from neo's own render pipeline and isn't called by it; kept
	// only for a later client-rendering phase, where a live GT6 renderer still reads it.
	public final int getRenderType() {return RendererBlockTextured.INSTANCE==null?-1:RendererBlockTextured.INSTANCE.mRenderID;}
	@Override public final IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T); return tTileEntity instanceof IRenderedBlockObject ? (IRenderedBlockObject)tTileEntity : null;}
	// Was onBlockEventReceived(World,x,y,z,id,data) -> BlockBehaviour.triggerEvent(BlockState,Level,BlockPos,int,int).
	// [BlockBehaviour.java:206]; TileEntity.receiveClientEvent(id,data) -> BlockEntity.triggerEvent(int,int) [BlockEntity.java:270]
	@Override public final boolean triggerEvent(BlockState aState, Level aWorld, BlockPos aPos, int aID, int aParam) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity == null || aTileEntity.triggerEvent(aID, aParam);}
	// The old TE gate sat on top of super.getDestroyProgress, whose destroySpeed defaulted to 0, dividing
	// by zero, breaking instantly; fixed to compute progress from the same per-TE hardness the 1.7.10 chain used.
	// IMTE_GetPlayerRelativeBlockHardness (TileEntityBase01Root:1108 allowInteraction → max(v,1e-4) | 0).
	private static long sLastDigZeroDiag = 0;
	@Override public final float getDestroyProgress(BlockState aState, Player aPlayer, BlockGetter aWorld, BlockPos aPos) {
		BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		float tHardness = aTileEntity instanceof IMTE_GetBlockHardness ? ((IMTE_GetBlockHardness)aTileEntity).getBlockHardness() : 1.0F;
		float tOriginal = WD.destroyProgress(tHardness, aPlayer, aState, aWorld, aPos); // the vanilla formula lives in the single center WD.destroyProgress
		float rResult = aTileEntity instanceof IMTE_GetPlayerRelativeBlockHardness ? ((IMTE_GetPlayerRelativeBlockHardness)aTileEntity).getPlayerRelativeBlockHardness(aPlayer, tOriginal) : tOriginal;
		// Always-on live diagnostic (server-only, throttled to 1/s): zero progress on a destructible block is
		// an anomaly in the chain, so its components are logged to name the cause; never fires in ordinary play.
		if (rResult <= 0.0F && tHardness >= 0 && aWorld instanceof Level tLevel && !tLevel.isClientSide() && System.currentTimeMillis() - sLastDigZeroDiag > 1000) {
			sLastDigZeroDiag = System.currentTimeMillis();
			ItemStack tHand = aPlayer.getMainHandItem();
			StringBuilder tSB = new StringBuilder("[GT6-DIG-ZERO] прогресс=0: блок=").append(aTileEntity == null ? "BE-null" : aTileEntity.getClass().getSimpleName())
				.append(" hardness=").append(tHardness)
				.append(" playerSpeed=").append(aPlayer.getDestroySpeed(aState))
				.append(" harvestOK=").append(net.minecraftforge.common.ForgeHooks.isCorrectToolForDrops(aState, aPlayer))
				.append(" рука=").append(tHand.isEmpty() ? "ПУСТО" : String.valueOf(tHand.getItem()) + "#" + gregapi.util.ST.meta_(tHand));
			if (gregapi.util.ST.item_(tHand) instanceof gregapi.item.multiitem.MultiItemTool tTool) {
				gregapi.item.multiitem.tools.IToolStats tStats = tTool.getToolStats(tHand);
				tSB.append(" | инструмент: stats=").append(tStats == null ? "NULL(мета-лукап!)" : tStats.getClass().getSimpleName())
					.append(" usable=").append(tTool.isItemStackUsable(tHand))
					.append(" digSpeed=").append(tTool.getDigSpeed(tHand, aState.getBlock(), 0))
					.append(" quality=").append(tStats == null ? "-" : String.valueOf(tStats.getBaseQuality() + tTool.getPrimaryMaterial(tHand).mToolQuality))
					// The level comes from the positional center now; the old by-meta-0 call would print zero
					// for any machine after per-material restoration, misleading exactly the diagnostic meant to explain the cause.
					.append(" нужен-уровень=").append(WD.harvestLevel(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()))
					.append(" нужен-tool=").append(WD.harvestTool(aState.getBlock(), 0));
			}
			gregapi.data.CS.OUT.println(tSB.toString());
		}
		return rResult;
	}
	// The engine-facing dynamic hardness is already wired above through getDestroyProgress; this
	// getBlockHardness is an internal GT6 helper for its own logic, not a stub -- the engine never calls it.
	public final float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBlockHardness?((IMTE_GetBlockHardness)aTileEntity).getBlockHardness():1.0F;}
	// Was getExplosionResistance(Entity,World,...) -> IBlockExtension's version; Explosion.getDirectSourceEntity()/
	// center() replace the lost parameters.
	@Override public final float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); Vec3 aCenter = aExplosion.getPosition(); return aTileEntity instanceof IMTE_GetExplosionResistance?((IMTE_GetExplosionResistance)aTileEntity).getExplosionResistance(aExplosion.getDirectSourceEntity(), aCenter.x, aCenter.y, aCenter.z):1.0F;}
	// Was onNeighborChange(IBlockAccess,x,y,z,tileX,Y,Z).
	// -> IBlockExtension.onNeighborChange(BlockState,LevelReader,BlockPos,BlockPos).
	@Override public final void onNeighborChange(BlockState aState, LevelReader aWorld, BlockPos aPos, BlockPos aNeighbor) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aNeighbor.getX(), aNeighbor.getY(), aNeighbor.getZ()); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborChange) ((IMTE_OnNeighborChange)aTileEntity).onNeighborChange(aWorld, aNeighbor.getX(), aNeighbor.getY(), aNeighbor.getZ());}
	// Orphan cleanup (BE==null -> remove block) is server-only: in 1.7.10 the client branch was dead by
	// invariant; the port's client BE now arrives asynchronously, so a missing one there means syncing, not orphaned.
	/** Root cause of players losing builds: the orphan-cleanup branch itself is 1.7.10 canon and
	 *  stays; the defect was its TRIGGER -- WD.te==null on 1.20.1 doesn't only mean orphaned; a chunk race could trip it too. */
	public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aX, aY, aZ); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborBlockChange) ((IMTE_OnNeighborBlockChange)aTileEntity).onNeighborBlockChange(aWorld, aBlock); if (aTileEntity == null && !aWorld.isClientSide() && WD.teProvenAbsent(aWorld, aX, aY, aZ)) {traceOrphanSweep(aWorld, aX, aY, aZ); WD.set(aWorld, aX, aY, aZ, NB, 0, 3);}}

	/** A legitimate sweep (BE provably gone). Rare and worth hunting for its own root cause, so it's
	 *  always logged: first 20, then every 500th, same convention as other leak counters. */
	private static final java.util.concurrent.atomic.AtomicLong sOrphanBlocksSwept = new java.util.concurrent.atomic.AtomicLong();
	private static void traceOrphanSweep(Level aWorld, int aX, int aY, int aZ) {
		long tN = sOrphanBlocksSwept.incrementAndGet();
		if (tN <= 20 || tN % 500 == 0) OUT.println("[GT6-MTEORPHAN] блок-сирота снят (BE доказуемо нет) @" + aX + ", " + aY + ", " + aZ
			+ " чанк=[" + (aX >> 4) + ", " + (aZ >> 4) + "] тик=" + aWorld.getGameTime() + " всего=" + tN);
	}
	// 1.7.10's World.notifyBlocksOfNeighborChange called Block.onNeighborBlockChange; neo's
	// entry point is BlockBehaviour.neighborChanged. Bridged the same way as BlockFluidBaseGT.
	@Override public void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, BlockPos aFromPos, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}
	@Override public final boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return T;}
	@Override public final boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return T;}
	@Override public final void receiveBlockError(BlockGetter aWorld, int aX, int aY, int aZ, String aError) {BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (tTileEntity instanceof ITileEntity) {((ITileEntity)tTileEntity).setError(aError); WD.update(aWorld, aX, aY, aZ); UT.Sounds.play(SFX.GT_BEEP, 100, 1.0F, aX, aY, aZ);}}
	@Override public final void onWalkOver(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnWalkOver) ((IMTE_OnWalkOver)aTileEntity).onWalkOver(aEntity);}
}
