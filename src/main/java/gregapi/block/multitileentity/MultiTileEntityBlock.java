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
 */

package gregapi.block.multitileentity;

import gregapi.block.*;
import gregapi.block.IBlockSyncData.IBlockSyncDataAndCoversAndIDs;
import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.IL;
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
import net.minecraft.client.renderer.texture.IIconRegister;
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
import net.neoforged.neoforge.event.EventHooks;

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
	
	public static String getName(String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		return "gt.block.multitileentity." + aNameOfVanillaMaterialField + "." + aSoundType.soundName + "." + aTool + "." + aHarvestLevelOffset + "." + aHarvestLevelMinimum + "." + aHarvestLevelMaximum + "." + aOpaque + "." + aNormalCube;
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
	protected MultiTileEntityBlock(String aModID, String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		super(aVanillaMaterial);
		if (GAPI.mStartedInit) throw new IllegalStateException("Blocks can only be initialised within preInit!");
		
		mNameInternal = getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube);
		ST.register(this, mNameInternal, BlockItem.class);
		
		MULTITILEENTITYBLOCKMAP.put(aModID + ":" + mNameInternal, this);
		
		setStepSound(aSoundType);
		mOpaque = aOpaque;
		mNormalCube = aNormalCube;
		
		mTool = aTool.toLowerCase();
		mHarvestLevelOffset = aHarvestLevelOffset;
		mHarvestLevelMinimum = Math.max(0, aHarvestLevelMinimum);
		mHarvestLevelMaximum = Math.max(aHarvestLevelMinimum, aHarvestLevelMaximum);
		
		opaque = isOpaqueCube();
		lightOpacity = isOpaqueCube() ? 255 : 0;
		
		if (MD.Mek.mLoaded) try {MekanismAPI.addBoxBlacklist(this, W);} catch(Throwable e) {e.printStackTrace(ERR);}
		
		ST.hide(this);
	}
	
	@Override
	public final void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMetaData) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		if (aTileEntity == null || !aTileEntity.shouldRefresh(this, aBlock, aMetaData, aMetaData, aWorld, aX, aY, aZ)) return;
		if (aTileEntity instanceof IMTE_BreakBlock && ((IMTE_BreakBlock)aTileEntity).breakBlock()) return;
		if (aTileEntity instanceof IMTE_HasMultiBlockMachineRelevantData && ((IMTE_HasMultiBlockMachineRelevantData)aTileEntity).hasMultiBlockMachineRelevantData()) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(aWorld, aX, aY, aZ, this, (byte)aMetaData, T);
		aWorld.removeTileEntity(aX, aY, aZ);
	}
	
	@Override
	public MapColor getMapColor(int aMeta) {
		return mMapColor == null ? super.getMapColor(aMeta) : mMapColor;
	}
	public MultiTileEntityBlock setMapColor(MapColor aMapColor) {
		mMapColor = aMapColor;
		return this;
	}
	
	private static boolean LOCK = F;
	
	// You want to override one of those Functions with your TileEntity? Just implement the Interfaces of MultiTileEntityInterfaces on your TileEntity and you are done.
	@Override public final void receiveDataName     (BlockGetter aWorld, int aX, int aY, int aZ, String aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMultiTileEntity) ((IMultiTileEntity)aTileEntity).setCustomName(aData);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte   aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByte       ) if (((IMTE_SyncDataByte       )aTileEntity).receiveDataByte       (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short  aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataShort      ) if (((IMTE_SyncDataShort      )aTileEntity).receiveDataShort      (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int    aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataInteger    ) if (((IMTE_SyncDataInteger    )aTileEntity).receiveDataInteger    (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long   aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataLong       ) if (((IMTE_SyncDataLong       )aTileEntity).receiveDataLong       (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler)                                                                                        {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByteArray  ) if (((IMTE_SyncDataByteArray  )aTileEntity).receiveDataByteArray  (aData, aNetworkHandler)) WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveData         (BlockGetter aWorld, int aX, int aY, int aZ              , INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte   aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByte     ) ((IMTE_SyncDataByte     )aTileEntity).receiveDataByte     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short  aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataShort    ) ((IMTE_SyncDataShort    )aTileEntity).receiveDataShort    (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int    aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataInteger  ) ((IMTE_SyncDataInteger  )aTileEntity).receiveDataInteger  (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long   aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataLong     ) ((IMTE_SyncDataLong     )aTileEntity).receiveDataLong     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler, short aID1, short aID2)                                                                {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByteArray) ((IMTE_SyncDataByteArray)aTileEntity).receiveDataByteArray(aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveData         (BlockGetter aWorld, int aX, int aY, int aZ              , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte aData  , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataByte     ) ((IMTE_SyncDataByte     )aTileEntity).receiveDataByte     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short aData , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataShort    ) ((IMTE_SyncDataShort    )aTileEntity).receiveDataShort    (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int aData   , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataInteger  ) ((IMTE_SyncDataInteger  )aTileEntity).receiveDataInteger  (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long aData  , INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataLong     ) ((IMTE_SyncDataLong     )aTileEntity).receiveDataLong     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler, short aID1, short aID2, short[] aCoverIDs, short[] aCoverMetas, short[] aCoverVisuals) {if (!(aWorld instanceof Level)) return; MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(aID1); if (tRegistry == null) return; BlockEntity aTileEntity = tRegistry.getNewTileEntity((Level)aWorld, aX, aY, aZ, aID2); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers   ) {((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverIDs, aCoverMetas, aNetworkHandler); ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, TRUE_6, aNetworkHandler);} if (aTileEntity instanceof IMTE_SyncDataByteArray) ((IMTE_SyncDataByteArray)aTileEntity).receiveDataByteArray(aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveData         (BlockGetter aWorld, int aX, int aY, int aZ              , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte aData  , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByte     ) ((IMTE_SyncDataByte     )aTileEntity).receiveDataByte     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short aData , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataShort    ) ((IMTE_SyncDataShort    )aTileEntity).receiveDataShort    (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int aData   , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataInteger  ) ((IMTE_SyncDataInteger  )aTileEntity).receiveDataInteger  (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long aData  , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataLong     ) ((IMTE_SyncDataLong     )aTileEntity).receiveDataLong     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByteArray) ((IMTE_SyncDataByteArray)aTileEntity).receiveDataByteArray(aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return !(aTileEntity instanceof IMTE_GetBlocksMovement) || ((IMTE_GetBlocksMovement)aTileEntity).getBlocksMovement();}
	@Override @SuppressWarnings("unchecked") public final void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, @SuppressWarnings("rawtypes") List aList, Entity aEntity) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_AddCollisionBoxesToList) ((IMTE_AddCollisionBoxesToList)aTileEntity).addCollisionBoxesToList(aAABB, aList, aEntity); else if (aTileEntity != null) super.addCollisionBoxesToList(aWorld, aX, aY, aZ, aAABB, aList, aEntity);}
	@Override public final AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetCollisionBoundingBoxFromPool ? ((IMTE_GetCollisionBoundingBoxFromPool)aTileEntity).getCollisionBoundingBoxFromPool() : aTileEntity == null ? null : AABB.getBoundingBox(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	@Override public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_UpdateTick) ((IMTE_UpdateTick)aTileEntity).updateTick(aRandom);}
	@Override public final void onBlockDestroyedByPlayer(Level aWorld, int aX, int aY, int aZ, int aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockDestroyedByPlayer) ((IMTE_OnBlockDestroyedByPlayer)aTileEntity).onBlockDestroyedByPlayer(aRandom);}
	@Override public final void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnBlockAdded) ((IMTE_OnBlockAdded)aTileEntity).onBlockAdded();}
	@Override public final void dropXpOnBlockBreak(Level aWorld, int aX, int aY, int aZ, int aXP) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_DropXpOnBlockBreak) ((IMTE_DropXpOnBlockBreak)aTileEntity).dropXpOnBlockBreak(aXP); else super.dropXpOnBlockBreak(aWorld, aX, aY, aZ, aXP);}
	@Override public final HitResult collisionRayTrace(Level aWorld, int aX, int aY, int aZ, Vec3 aVectorA, Vec3 aVectorB) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_CollisionRayTrace ? ((IMTE_CollisionRayTrace)aTileEntity).collisionRayTrace(aVectorA, aVectorB) : super.collisionRayTrace(aWorld, aX, aY, aZ, aVectorA, aVectorB);}
	@Override public final boolean onBlockActivated(Level aWorld, int aX, int aY, int aZ, Player aPlayer, int aSide, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aPlayer != null && IL.TC_Thaumometer.equal(aPlayer.getHeldItem(), T, T) && (!(aTileEntity instanceof ITileEntityBookShelf) || !((ITileEntityBookShelf)aTileEntity).isShelfFace(UT.Code.side(aSide)))) return F; return aTileEntity instanceof IMTE_OnBlockActivated && ((IMTE_OnBlockActivated)aTileEntity).onBlockActivated(aPlayer, UT.Code.side(aSide), aHitX, aHitY, aHitZ);}
	@Override public final void onEntityWalking(Level aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnEntityWalking) ((IMTE_OnEntityWalking)aTileEntity).onEntityWalking(aEntity);}
	@Override public final void onBlockClicked(Level aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnBlockClicked) ((IMTE_OnBlockClicked)aTileEntity).onBlockClicked(aPlayer); else super.onBlockClicked(aWorld, aX, aY, aZ, aPlayer);}
	@Override public final void velocityToAddToEntity(Level aWorld, int aX, int aY, int aZ, Entity aEntity, Vec3 aVector) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_VelocityToAddToEntity) ((IMTE_VelocityToAddToEntity)aTileEntity).velocityToAddToEntity(aEntity, aVector); else super.velocityToAddToEntity(aWorld, aX, aY, aZ, aEntity, aVector);}
	@Override public final int isProvidingWeakPower(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(UT.Code.side(aSide)) : super.isProvidingWeakPower(aWorld, aX, aY, aZ, aSide);}
	@Override public final void onEntityCollidedWithBlock(Level aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnEntityCollidedWithBlock) ((IMTE_OnEntityCollidedWithBlock)aTileEntity).onEntityCollidedWithBlock(aEntity); else super.onEntityCollidedWithBlock(aWorld, aX, aY, aZ, aEntity);}
	@Override public final int isProvidingStrongPower(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsProvidingStrongPower ? ((IMTE_IsProvidingStrongPower)aTileEntity).isProvidingStrongPower(UT.Code.side(aSide)) : super.isProvidingStrongPower(aWorld, aX, aY, aZ, aSide);}
	@Override public final boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return !(aTileEntity instanceof IMTE_CanBlockStay) || ((IMTE_CanBlockStay)aTileEntity).canBlockStay();}
	@Override public final void onFallenUpon(Level aWorld, int aX, int aY, int aZ, Entity aEntity, float aFallDistance) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnFallenUpon) ((IMTE_OnFallenUpon)aTileEntity).onFallenUpon(aEntity, aFallDistance); else super.onFallenUpon(aWorld, aX, aY, aZ, aEntity, aFallDistance);}
	@Override public final void onBlockHarvested(Level aWorld, int aX, int aY, int aZ, int aMetaData, Player aPlayer) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnBlockHarvested) ((IMTE_OnBlockHarvested)aTileEntity).onBlockHarvested(aMetaData, aPlayer); else super.onBlockHarvested(aWorld, aX, aY, aZ, aMetaData, aPlayer);}
	@Override public final void onBlockPreDestroy(Level aWorld, int aX, int aY, int aZ, int aMetaData) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnBlockPreDestroy) ((IMTE_OnBlockPreDestroy)aTileEntity).onBlockPreDestroy(aMetaData); else super.onBlockPreDestroy(aWorld, aX, aY, aZ, aMetaData);}
	@Override public final void fillWithRain(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_FillWithRain) ((IMTE_FillWithRain)aTileEntity).fillWithRain(); else super.fillWithRain(aWorld, aX, aY, aZ);}
	@Override public final boolean hasComparatorInputOverride() {return T;}
	@Override public final int getComparatorInputOverride(Level aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetComparatorInputOverride ? ((IMTE_GetComparatorInputOverride)aTileEntity).getComparatorInputOverride(UT.Code.side(aSide)) : aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(OPOS[aSide]) : super.getComparatorInputOverride(aWorld, aX, aY, aZ, aSide);}
	@Override public final int getLightValue(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetLightValue ? UT.Code.bind4(((IMTE_GetLightValue)aTileEntity).getLightValue()) : super.getLightValue(aWorld, aX, aY, aZ);}
	@Override public final boolean isLadder(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aEntity) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsLadder && ((IMTE_IsLadder)aTileEntity).isLadder(aEntity);}
	@Override public final boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsNormalCube ? ((IMTE_IsNormalCube)aTileEntity).isNormalCube() : mNormalCube;}
	@Override public final boolean isReplaceable(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsReplaceable ? ((IMTE_IsReplaceable)aTileEntity).isReplaceable() : blockMaterial.isReplaceable();}
	@Override public final boolean isBurning(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsBurning && ((IMTE_IsBurning)aTileEntity).isBurning();}
	@Override public final boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {if (aWorld == null) return F; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsAir && ((IMTE_IsAir)aTileEntity).isAir();}
	@Override public final boolean removedByPlayer(Level aWorld, Player aPlayer, int aX, int aY, int aZ, boolean aWillHarvest) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); return aTileEntity instanceof IMTE_RemovedByPlayer ? ((IMTE_RemovedByPlayer)aTileEntity).removedByPlayer(aWorld, aPlayer, aWillHarvest) : super.removedByPlayer(aWorld, aPlayer, aX, aY, aZ, aWillHarvest);}
	@Override public final boolean canCreatureSpawn(MobCategory aType, BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_CanCreatureSpawn && ((IMTE_CanCreatureSpawn)aTileEntity).canCreatureSpawn(aType);}
	@Override public final boolean isBed(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aPlayer) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsBed && ((IMTE_IsBed)aTileEntity).isBed(aPlayer);}
	@Override public final BlockPos getBedSpawnPosition(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_VelocityToAddToEntity ? ((IMTE_GetBedSpawnPosition)aTileEntity).getBedSpawnPosition(aPlayer) : null;}
	@Override public final void setBedOccupied(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer, boolean aOccupied) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_SetBedOccupied) ((IMTE_SetBedOccupied)aTileEntity).setBedOccupied(aPlayer, aOccupied);}
	@Override public final int getBedDirection(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetBedDirection ? ((IMTE_GetBedDirection)aTileEntity).getBedDirection() : 0;}
	@Override public final boolean isBedFoot(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsBedFoot && ((IMTE_IsBedFoot)aTileEntity).isBedFoot();}
	@Override public final void beginLeavesDecay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_BeginLeavesDecay) ((IMTE_BeginLeavesDecay)aTileEntity).beginLeavesDecay(); else super.beginLeavesDecay(aWorld, aX, aY, aZ);}
	@Override public final boolean canSustainLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_CanSustainLeaves ? ((IMTE_CanSustainLeaves)aTileEntity).canSustainLeaves() : super.canSustainLeaves(aWorld, aX, aY, aZ);}
	@Override public final boolean isLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsLeaves && ((IMTE_IsLeaves)aTileEntity).isLeaves();}
	@Override public final boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_CanBeReplacedByLeaves && ((IMTE_CanBeReplacedByLeaves)aTileEntity).canBeReplacedByLeaves();}
	@Override public final boolean isWood(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsWood ? ((IMTE_IsWood)aTileEntity).isWood() : super.isWood(aWorld, aX, aY, aZ);}
	@Override public final boolean isReplaceableOreGen(Level aWorld, int aX, int aY, int aZ, Block aTarget) {if (GAPI.mStartedServerStarted < 1) return F; BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsReplaceableOreGen ? ((IMTE_IsReplaceableOreGen)aTileEntity).isReplaceableOreGen(aTarget) : super.isReplaceableOreGen(aWorld, aX, aY, aZ, aTarget);}
	@Override public final boolean canConnectRedstone(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_CanConnectRedstone ? ((IMTE_CanConnectRedstone)aTileEntity).canConnectRedstone(UT.Code.side(aSide)) : super.canConnectRedstone(aWorld, aX, aY, aZ, aSide);}
	@Override public final boolean canPlaceTorchOnTop(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_CanPlaceTorchOnTop ? ((IMTE_CanPlaceTorchOnTop)aTileEntity).canPlaceTorchOnTop() : isSideSolid(aWorld, aX, aY, aZ, FORGE_DIR[SIDE_TOP]);}
	@Override public final boolean isFoliage(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsFoliage ? ((IMTE_IsFoliage)aTileEntity).isFoliage() : super.isFoliage(aWorld, aX, aY, aZ);}
	@Override public final boolean canSustainPlant(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide, IPlantable aPlantable) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_CanSustainPlant ? ((IMTE_CanSustainPlant)aTileEntity).canSustainPlant(UT.Code.side(aSide), aPlantable) : super.canSustainPlant(aWorld, aX, aY, aZ, aSide, aPlantable);}
	@Override public final void onPlantGrow(Level aWorld, int aX, int aY, int aZ, int sX, int sY, int sZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnPlantGrow) ((IMTE_OnPlantGrow)aTileEntity).onPlantGrow(sX, sY, sZ); else super.onPlantGrow(aWorld, aX, aY, aZ, sX, sY, sZ);}
	@Override public final boolean isFertile(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsFertile && ((IMTE_IsFertile)aTileEntity).isFertile();}
	@Override public final boolean rotateBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_RotateBlock && ((IMTE_RotateBlock)aTileEntity).rotateBlock(UT.Code.side(aSide));}
	@Override public final Direction[] getValidRotations(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetValidRotations ? ((IMTE_GetValidRotations)aTileEntity).getValidRotations() : ZL_FORGEDIRECTION;}
	@Override public final float getEnchantPowerBonus(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetEnchantPowerBonus ? ((IMTE_GetEnchantPowerBonus)aTileEntity).getEnchantPowerBonus() : 0;}
	@Override public final boolean recolourBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide, int aColor) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_RecolourBlock && ((IMTE_RecolourBlock)aTileEntity).recolourBlock(UT.Code.side(aSide), (byte)aColor);}
	@Override public final boolean shouldCheckWeakPower(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_ShouldCheckWeakPower ? ((IMTE_ShouldCheckWeakPower)aTileEntity).shouldCheckWeakPower(UT.Code.side(aSide)) : isNormalCube(aWorld, aX, aY, aZ);}
	@Override public final boolean getWeakChanges(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetWeakChanges ? ((IMTE_GetWeakChanges)aTileEntity).getWeakChanges() : super.getWeakChanges(aWorld, aX, aY, aZ);}
	@Override public final boolean addHitEffects(Level aWorld, HitResult aTarget, ParticleEngine aRenderer) {BlockEntity aTileEntity = aWorld.getTileEntity(aTarget.blockX, aTarget.blockY, aTarget.blockZ); return aTileEntity instanceof IMTE_AddHitEffects && ((IMTE_AddHitEffects)aTileEntity).addHitEffects(aWorld, aTarget, aRenderer);}
	@Override public final boolean addDestroyEffects(Level aWorld, int aX, int aY, int aZ, int aMetaData, ParticleEngine aRenderer) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_AddDestroyEffects && ((IMTE_AddDestroyEffects)aTileEntity).addDestroyEffects(aMetaData, aRenderer);}
	@Override public final boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX-OFFX[aSide], aY-OFFY[aSide], aZ-OFFZ[aSide]); return aTileEntity instanceof IMTE_ShouldSideBeRendered ? ((IMTE_ShouldSideBeRendered)aTileEntity).shouldSideBeRendered(UT.Code.side(aSide)) : super.shouldSideBeRendered(aWorld, aX, aY, aZ, aSide);}
	@Override public final void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_SetBlockBoundsBasedOnState) ((IMTE_SetBlockBoundsBasedOnState)aTileEntity).setBlockBoundsBasedOnState(this); else if (aTileEntity == null) setBlockBounds(-999, -999, -999, -998, -998, -998); else setBlockBounds(0, 0, 0, 1, 1, 1);}
	@Override public final AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity == null ? AABB.getBoundingBox(-999, -999, -999, -998, -998, -998) : aTileEntity instanceof IMTE_GetSelectedBoundingBoxFromPool ? ((IMTE_GetSelectedBoundingBoxFromPool)aTileEntity).getSelectedBoundingBoxFromPool() : AABB.getBoundingBox(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	@Override public final void randomDisplayTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_RandomDisplayTick) ((IMTE_RandomDisplayTick)aTileEntity).randomDisplayTick(aRandom); else super.randomDisplayTick(aWorld, aX, aY, aZ, aRandom);}
	@Override public final void onBlockExploded(Level aWorld, int aX, int aY, int aZ, Explosion aExplosion) {if (aWorld.isRemote) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); if (aTileEntity instanceof IMTE_OnBlockExploded) ((IMTE_OnBlockExploded)aTileEntity).onExploded(aExplosion); else aWorld.setBlockToAir(aX, aY, aZ);}
	@Override public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	@Override public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ                      ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	@Override public final ItemStack getItemStackFromBlock(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetStackFromBlock?((IMTE_GetStackFromBlock)aTileEntity).getStackFromBlock(aSide):null;}
	@Override public final int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetFlammability ? ((IMTE_GetFlammability)aTileEntity).getFlammability(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	@Override public final int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetFireSpreadSpeed ? ((IMTE_GetFireSpreadSpeed)aTileEntity).getFireSpreadSpeed(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	@Override public final boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsFireSource && ((IMTE_IsFireSource)aTileEntity).isFireSource(UT.Code.side(aSide));}
	@Override public final boolean canEntityDestroy(BlockGetter aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return !(aTileEntity instanceof IMTE_CanEntityDestroy) || ((IMTE_CanEntityDestroy)aTileEntity).canEntityDestroy(aEntity);}
	@Override public final long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, AbstractContainerMenu aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_OnToolClick ? ((IMTE_OnToolClick)aTileEntity).onToolClick(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ) : 0;}
	@Override public final OreDictMaterialStack getMaterialAtSide(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetMaterialAtSide?((IMTE_GetMaterialAtSide)aTileEntity).getMaterialAtSide(aSide):null;}
	@Override public final boolean removeMaterialFromSide(Level aWorld, int aX, int aY, int aZ, byte aSide, OreDictMaterialStack aMaterial) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_RemoveMaterialFromSide && ((IMTE_RemoveMaterialFromSide)aTileEntity).removeMaterialFromSide(aSide, aMaterial);}
	@Override public final void dropBlockAsItemWithChance(Level aWorld, int aX, int aY, int aZ, int aMeta, float aChance, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); aChance = EventHooks.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, F, harvesters.get()); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	@Override public final void harvestBlock(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {if (aPlayer == null) aPlayer = harvesters.get(); aPlayer.addStat(Stats.mineBlockStatArray[getIdFromBlock(this)], 1); UT.Entities.exhaust(aPlayer, 0.025F); boolean aSilkTouch = EnchantmentHelper.getSilkTouchModifier(aPlayer); int aFortune = EnchantmentHelper.getFortuneModifier(aPlayer); float aChance = 1.0F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, aSilkTouch); aChance = EventHooks.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, aSilkTouch, aPlayer); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	@Override public final ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aUnusableMetaData, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) return ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); return ST.arraylist();}
	@Override public final ArrayList<String> getDebugInfo(Player aPlayer, int aX, int aY, int aZ, int aScanLevel) {BlockEntity aTileEntity = aPlayer.worldObj.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetDebugInfo ? ((IMTE_GetDebugInfo)aTileEntity).getDebugInfo(aScanLevel) : null;}
	@Override public final boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsSideSolid?((IMTE_IsSideSolid)aTileEntity).isSideSolid(UT.Code.side(aSide)):mOpaque;}
	@Override public final boolean isBeaconBase(BlockGetter aWorld, int aX, int aY, int aZ, int aBeaconX, int aBeaconY, int aBeaconZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_IsBeaconBase && ((IMTE_IsBeaconBase)aTileEntity).isBeaconBase(aBeaconX, aBeaconY, aBeaconZ);}
	@Override public final int getLightOpacity(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetLightOpacity?((IMTE_GetLightOpacity)aTileEntity).getLightOpacity():mOpaque?LIGHT_OPACITY_MAX:LIGHT_OPACITY_NONE;}
	@Override public final boolean isOpaqueCube() {return mOpaque;}
	@Override public final boolean func_149730_j() {return mOpaque;}
	@Override public final boolean renderAsNormalBlock() {return mOpaque || mNormalCube;}
	@Override public final boolean isNormalCube()  {return mNormalCube;}
	@Override public final boolean canProvidePower() {return !mNormalCube;}
	@Override public final Block getBlock() {return this;}
	@Override public final String getUnlocalizedName() {return mNameInternal;}
	@Override public final String getLocalizedName() {return StatCollector.translateToLocal(mNameInternal);}
	@Override public final String getHarvestTool(int aMeta) {return mTool;}
	@Override public final boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	@Override public final int getHarvestLevel(int aMeta) {return (int)UT.Code.bind_(mHarvestLevelMinimum, mHarvestLevelMaximum, mHarvestLevelOffset + aMeta);}
	@Override public final boolean canHarvestBlock(Player aPlayer, int aMeta) {return super.canHarvestBlock(aPlayer, aMeta);}
	@Override public final boolean hasTileEntity(int aMeta) {return T;}
	@Override public final boolean canSilkHarvest() {return F;}
	@Override public final int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	@Override public final BlockEntity createNewTileEntity(Level aWorld, int aMeta) {return null;}
	@Override public final BlockEntity createTileEntity(Level aWorld, int aMeta) {return null;}
	@Override public final void getSubBlocks(Item aItem, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {/**/}
	@Override public final ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
	@Override public final boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public final int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 0;}
	@Override public final ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {return null;}
	@Override public final boolean setBlockBounds(int aRenderPass, ItemStack aStack) {return F;}
	@Override public final int getRenderPasses(ItemStack aStack) {return 0;}
	@Override public final IRenderedBlockObject passRenderingToObject(ItemStack aStack) {return null;}
	@Override public final void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	@Override public final IIcon getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	@Override public final IIcon getIcon(int aSide, int aMetaData) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	@Override public final int getRenderType() {return RendererBlockTextured.INSTANCE==null?super.getRenderType():RendererBlockTextured.INSTANCE.mRenderID;}
	@Override public final IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity tTileEntity = aWorld.getTileEntity(aX, aY, aZ); return tTileEntity instanceof IRenderedBlockObject ? (IRenderedBlockObject)tTileEntity : null;}
	@Override public final boolean onBlockEventReceived(Level aWorld, int aX, int aY, int aZ, int aID, int aData) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity == null || aTileEntity.receiveClientEvent(aID, aData);}
	@Override public final float getPlayerRelativeBlockHardness(Player aPlayer, Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetPlayerRelativeBlockHardness ? ((IMTE_GetPlayerRelativeBlockHardness)aTileEntity).getPlayerRelativeBlockHardness(aPlayer, super.getPlayerRelativeBlockHardness(aPlayer, aWorld, aX, aY, aZ)) : super.getPlayerRelativeBlockHardness(aPlayer, aWorld, aX, aY, aZ);}
	@Override public final float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetBlockHardness?((IMTE_GetBlockHardness)aTileEntity).getBlockHardness():1.0F;}
	@Override public final float getExplosionResistance(Entity aExploder, Level aWorld, int aX, int aY, int aZ, double aExplosionX, double aExplosionY, double aExplosionZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); return aTileEntity instanceof IMTE_GetExplosionResistance?((IMTE_GetExplosionResistance)aTileEntity).getExplosionResistance(aExploder, aExplosionX, aExplosionY, aExplosionZ):1.0F;}
	@Override public final void onNeighborChange(BlockGetter aWorld, int aX, int aY, int aZ, int aTileX, int aTileY, int aTileZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aTileX, aTileY, aTileZ); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborChange) ((IMTE_OnNeighborChange)aTileEntity).onNeighborChange(aWorld, aTileX, aTileY, aTileZ);}
	@Override public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aX, aY, aZ); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborBlockChange) ((IMTE_OnNeighborBlockChange)aTileEntity).onNeighborBlockChange(aWorld, aBlock); if (aTileEntity == null) aWorld.setBlockToAir(aX, aY, aZ);}
	@Override public final boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return T;}
	@Override public final boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return T;}
	@Override public final void receiveBlockError(BlockGetter aWorld, int aX, int aY, int aZ, String aError) {BlockEntity tTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (tTileEntity instanceof ITileEntity) {((ITileEntity)tTileEntity).setError(aError); WD.update(aWorld, aX, aY, aZ); UT.Sounds.play(SFX.GT_BEEP, 100, 1.0F, aX, aY, aZ);}}
	@Override public final void onWalkOver(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = aWorld.getTileEntity(aX, aY, aZ); if (aTileEntity instanceof IMTE_OnWalkOver) ((IMTE_OnWalkOver)aTileEntity).onWalkOver(aEntity);}
}
