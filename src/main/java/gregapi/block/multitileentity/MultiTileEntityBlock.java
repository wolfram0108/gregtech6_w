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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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

	/** F-bounds (тот же приём, что BlockBase.java): последние заданные bounds (1.7.10 мутировал Block.mBoundingBox);
	 *  neo bounds immutable -> храним сами, рендер-использование отложено на F3-клиент-проход. IBlock-обязательный метод. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	@Override public float[] getRenderBounds() {return mRenderBounds;}

	/** F9-хвост: gregapi Material хранится MTE-блоком (тот же паттерн, что BlockBase); neo убрал ванильный Block.getMaterial()/blockMaterial. */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}

	public static String getName(String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		// F9/sound: было aSoundType.soundName (1.7.10 String-категория звука) в рег-ключе MTE. neo SoundType без имени —
		// воспроизводим 1.7.10-soundName 1:1 (значения сверены ФАКТИЧЕСКИ по golden-дампу: iron/machine(METAL)→"stone",
		// rock(STONE)→"stone", cloth/redstonelight(WOOL)→"cloth", leaves/tnt(GRASS)→"grass", wood(WOOD)→"wood").
		return "gt.block.multitileentity." + aNameOfVanillaMaterialField + "." + soundName(aSoundType) + "." + aTool + "." + aHarvestLevelOffset + "." + aHarvestLevelMinimum + "." + aHarvestLevelMaximum + "." + aOpaque + "." + aNormalCube;
	}

	/** 1.7.10 SoundType.soundName-эквивалент для рег-ключа MTE (сверено по golden-дампу). Прочие SoundType в getOrCreate не
	 *  встречаются — fallback на break-sound Identifier (проявится в engine-дампе как diff, а не тихо-неверно). */
	private static String soundName(SoundType aSoundType) {
		if (aSoundType == SoundType.WOOL)  return "cloth";
		if (aSoundType == SoundType.METAL) return "stone";
		if (aSoundType == SoundType.STONE) return "stone";
		if (aSoundType == SoundType.WOOD)  return "wood";
		if (aSoundType == SoundType.GRASS) return "grass";
		return aSoundType.getBreakSound().location().toString();
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
	// F16/F13: Properties при ctor — sound(step-звук) + noOcclusion для non-opaque (иначе рендер solid + свет блокируется). setId обязателен.
	private static net.minecraft.world.level.block.state.BlockBehaviour.Properties mkProps(SoundType aSoundType, String aRegName, boolean aOpaque) {
		// F-shape: dynamicShape() ОБЯЗАТЕЛЕН — иначе neo кэширует getCollisionShape (строит его раз с EmptyBlockGetter/
		// BlockPos.ZERO, BlockBehaviour:916) → per-BE форма (getCollisionShape-мост ниже, MTE-Rock/трубы) игнорируется,
		// снег/коллизия/isFaceSturdy берутся из статического кэша = полный куб. dynamicShape → кэш не строится → мост живёт.
		net.minecraft.world.level.block.state.BlockBehaviour.Properties p = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().dynamicShape().sound(aSoundType).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aRegName))));
		if (!aOpaque) p = p.noOcclusion();
		return p;
	}
	protected MultiTileEntityBlock(String aModID, String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		// F12-followup (block-split, MTE): setId в Properties (neo Block требует id); namespace=GT (gt.multitileentity — контент
		// GT6, golden = gregtech:; совпадает с реестром ST.register→registerBlock ниже). Имя вычисляется тем же getName(...), что и mNameInternal (стр. ниже) → ключ совпадает.
		// Конструкция идёт на RegisterEvent через GT_API.deferBlockInit (call-site getOrCreate/Loader_Others).
		// F16: sound(aSoundType) (step-звук). F13/F16: non-opaque → .noOcclusion() (иначе рендер solid + свет блокируется). mkProps ниже.
		super(mkProps(aSoundType, getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube), aOpaque));
		mMaterial = aVanillaMaterial;
		if (GAPI.mStartedInit) throw new IllegalStateException("Blocks can only be initialised within preInit!");
		
		mNameInternal = getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube);
		ST.register(this, mNameInternal, BlockItem.class);
		
		MULTITILEENTITYBLOCKMAP.put(aModID + ":" + mNameInternal, this);
		
		// F16: setStepSound ПОДКЛЮЧЕН — звук выставлен в mkProps выше (.sound(aSoundType) при ctor). Не заглушка.
		mOpaque = aOpaque;
		mNormalCube = aNormalCube;
		
		mTool = aTool.toLowerCase();
		mHarvestLevelOffset = aHarvestLevelOffset;
		mHarvestLevelMinimum = Math.max(0, aHarvestLevelMinimum);
		mHarvestLevelMaximum = Math.max(aHarvestLevelMinimum, aHarvestLevelMaximum);
		
		// F13/F16: opaque ПОДКЛЮЧЕН в Properties при ctor (mkProps выше: non-opaque → .noOcclusion() → neo рендер/свет корректны).
		// Собственные isOpaqueCube()/getLightOpacity() читают mOpaque для GT6-внутренней логики. Не заглушка.

		if (MD.Mek.mLoaded) try {MekanismAPI.addBoxBlacklist(this, W);} catch(Throwable e) {e.printStackTrace(ERR);}
		// F12-followup (block-split): ST.hide → ST.make (ItemStack) → server-start → deferItemInit.
		gregapi.GT_API.deferItemInit(() -> ST.hide(this));
	}
	
	// @Override
	public final void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMetaData) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		// было aTileEntity.shouldRefresh(...) (1.7.10 TileEntity.shouldRefresh, УДАЛЁН в neo BlockEntity целиком,
		// не найден ни в одном из 3 корней) - GT6 сам реализует его на TileEntityBase01Root (не vanilla-override
		// больше, обычный метод), поэтому маршрутизируем через cast; не-GT6-TE (не должно происходить для MTE)
		// трактуем как "refresh=true" (дефолт TileEntityBase01Root.mShouldRefresh), чтобы не потерять срабатывание.
		boolean tShouldRefresh = !(aTileEntity instanceof gregapi.tileentity.base.TileEntityBase01Root) || ((gregapi.tileentity.base.TileEntityBase01Root)aTileEntity).shouldRefresh(this, aBlock, aMetaData, aMetaData, aWorld, aX, aY, aZ);
		if (aTileEntity == null || !tShouldRefresh) return;
		if (aTileEntity instanceof IMTE_BreakBlock && ((IMTE_BreakBlock)aTileEntity).breakBlock()) return;
		if (aTileEntity instanceof IMTE_HasMultiBlockMachineRelevantData && ((IMTE_HasMultiBlockMachineRelevantData)aTileEntity).hasMultiBlockMachineRelevantData()) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(aWorld, aX, aY, aZ, this, (byte)aMetaData, T);
		aWorld.removeBlockEntity(new BlockPos(aX, aY, aZ)); // было aWorld.removeTileEntity(x,y,z) (1.7.10 World), neo Level.removeBlockEntity(BlockPos) [Level.java:688]
	}
	
	// было @Override Block.getMapColor(int) (1.7.10) - удалён в neo; собственный byte-meta dispatcher
	// остаётся обычным GT6-методом (не движковый override). super.getMapColor(aMeta) (vanilla-дефолт = материал)
	// заменён на mMaterial.getMaterialMapColor() - тот же источник дефолта, 1:1.
	public MapColor getMapColor(int aMeta) {
		return mMapColor == null ? mMaterial.getMaterialMapColor() : mMapColor;
	}
	public MultiTileEntityBlock setMapColor(MapColor aMapColor) {
		mMapColor = aMapColor;
		return this;
	}
	// F13: getMapColor(int) → IBlockExtension.getMapColor(BlockState,BlockGetter,BlockPos,MapColor). Мост gregapi.block.MapColor→
	// движковый централизован в MapColor.toNeo() (F9-bridge, индексы 0-63 совпадают). Возвращаем GT6-цвет блока 1:1; null → дефолт.
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
	@Override public final void receiveData         (BlockGetter aWorld, int aX, int aY, int aZ              , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte aData  , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByte     ) ((IMTE_SyncDataByte     )aTileEntity).receiveDataByte     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short aData , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataShort    ) ((IMTE_SyncDataShort    )aTileEntity).receiveDataShort    (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int aData   , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataInteger  ) ((IMTE_SyncDataInteger  )aTileEntity).receiveDataInteger  (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long aData  , INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataLong     ) ((IMTE_SyncDataLong     )aTileEntity).receiveDataLong     (aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	@Override public final void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler, short[] aCoverVisuals, boolean[] aVisualsToSync)                                       {if (!(aWorld instanceof Level)) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity == null) return; WD.te((Level)aWorld, aX, aY, aZ, aTileEntity, F); if (aTileEntity instanceof ITileEntitySynchronising) ((ITileEntitySynchronising)aTileEntity).processPacket(aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataCovers) ((IMTE_SyncDataCovers)aTileEntity).receiveDataCovers(aCoverVisuals, aVisualsToSync, aNetworkHandler); if (aTileEntity instanceof IMTE_SyncDataByteArray) ((IMTE_SyncDataByteArray)aTileEntity).receiveDataByteArray(aData, aNetworkHandler); WD.update(aWorld, aX, aY, aZ);}
	public final boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_GetBlocksMovement) || ((IMTE_GetBlocksMovement)aTileEntity).getBlocksMovement();}
	// было super.addCollisionBoxesToList(...) (1.7.10 Block, УДАЛЁН из neo целиком - box-list-коллизия
	// заменена движком на VoxelShape-систему). Дефолт inline-порт 1:1 вместо super-вызова (Block.java:661-669 recompSrc):
	// getCollisionBoundingBoxFromPool + intersects-проверка, тот же алгоритм, что был у vanilla-дефолта.
	@SuppressWarnings("unchecked") public final void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, @SuppressWarnings("rawtypes") List aList, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_AddCollisionBoxesToList) ((IMTE_AddCollisionBoxesToList)aTileEntity).addCollisionBoxesToList(aAABB, aList, aEntity); else if (aTileEntity != null) {AABB tBox = getCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ); if (tBox != null && aAABB.intersects(tBox)) aList.add(tBox);}}
	public final AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetCollisionBoundingBoxFromPool ? ((IMTE_GetCollisionBoundingBoxFromPool)aTileEntity).getCollisionBoundingBoxFromPool() : aTileEntity == null ? null : new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	// F-shape (класс «канал движка сместился», как F-tick/useOn): 1.7.10 getCollisionBoundingBoxFromPool/getSelectedBoundingBox
	// (AABB) удалены — neo форма блока через VoxelShape (getCollisionShape=коллизия/снег, getShape=outline). БЕЗ моста MTE-блоки
	// давали дефолтный ПОЛНЫЙ КУБ → снег ложился на камешки (SnowLayerBlock.canSurvive→isFaceFull(getCollisionShape,UP)),
	// коллизия/outline полные (камешек непроходим). Мост: BE-AABB (абсолютная, box()=pos+bounds) → относительный VoxelShape
	// (move(-pos)); null коллизия (напр. MTE-Rock) → Shapes.empty (проходим, снег не ляжет). Централизованно на весь MTE-слой.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (!(aWorld instanceof Level tLevel)) return super.getCollisionShape(aState, aWorld, aPos, aContext);
		AABB tBox = getCollisionBoundingBoxFromPool(tLevel, aPos.getX(), aPos.getY(), aPos.getZ());
		return tBox == null ? net.minecraft.world.phys.shapes.Shapes.empty() : net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ()));
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld instanceof Level tLevel) {
			BlockEntity tTileEntity = WD.te(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			if (tTileEntity instanceof IMTE_GetSelectedBoundingBoxFromPool tSel) {
				AABB tBox = tSel.getSelectedBoundingBoxFromPool();
				if (tBox != null) return net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ()));
			}
		}
		return super.getShape(aState, aWorld, aPos, aContext); // обычные MTE (машины/сундуки) — полный куб (как было)
	}
	public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_UpdateTick) ((IMTE_UpdateTick)aTileEntity).updateTick(aRandom);}
	public final void onBlockDestroyedByPlayer(Level aWorld, int aX, int aY, int aZ, int aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockDestroyedByPlayer) ((IMTE_OnBlockDestroyedByPlayer)aTileEntity).onBlockDestroyedByPlayer(aRandom);}
	// было onBlockAdded(World,x,y,z) -> BlockBehaviour.onPlace(BlockState,Level,BlockPos,BlockState,boolean) [BlockBehaviour.java:167]
	@Override protected final void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnBlockAdded) ((IMTE_OnBlockAdded)aTileEntity).onBlockAdded();}
	// было super.dropXpOnBlockBreak(...) (1.7.10 Block, УДАЛЁН из neo целиком). Дефолт inline-порт 1:1 вместо
	// super-вызова (Block.java:843-854 recompSrc): цикл EntityXPOrb-спавна -> neo ExperienceOrb.award(ServerLevel,Vec3,int)
	// (ExperienceOrb.java:190, тот же split-алгоритм внутри award/awardWithDirection).
	public final void dropXpOnBlockBreak(Level aWorld, int aX, int aY, int aZ, int aXP) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_DropXpOnBlockBreak) ((IMTE_DropXpOnBlockBreak)aTileEntity).dropXpOnBlockBreak(aXP); else if (!aWorld.isClientSide() && aWorld instanceof ServerLevel aServerWorld) ExperienceOrb.award(aServerWorld, new Vec3(aX+0.5, aY+0.5, aZ+0.5), aXP);}
	// F13: 1.7.10 Block.collisionRayTrace удалён — neo коллизия-raytrace генерик поверх VoxelShape/getShape (не per-Block-override).
	// TE-интерфейс IMTE_CollisionRayTrace без implementor'ов (0, сверено) → мёртвая compile-поверхность, не заглушка (терять нечего).
	public final HitResult collisionRayTrace(Level aWorld, int aX, int aY, int aZ, Vec3 aVectorA, Vec3 aVectorB) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CollisionRayTrace ? ((IMTE_CollisionRayTrace)aTileEntity).collisionRayTrace(aVectorA, aVectorB) : null;}
	// было aPlayer.getHeldItem() (1.7.10 EntityPlayer no-arg, дефолтная рука) -> neo Player.getMainHandItem() (Player.java:2257)
	public final boolean onBlockActivated(Level aWorld, int aX, int aY, int aZ, Player aPlayer, int aSide, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aPlayer != null && IL.TC_Thaumometer.equal(aPlayer.getMainHandItem(), T, T) && (!(aTileEntity instanceof ITileEntityBookShelf) || !((ITileEntityBookShelf)aTileEntity).isShelfFace(UT.Code.side(aSide)))) return F; return aTileEntity instanceof IMTE_OnBlockActivated && ((IMTE_OnBlockActivated)aTileEntity).onBlockActivated(aPlayer, UT.Code.side(aSide), aHitX, aHitY, aHitZ);}
	public final void onEntityWalking(Level aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnEntityWalking) ((IMTE_OnEntityWalking)aTileEntity).onEntityWalking(aEntity);}
	// было onBlockClicked(World,x,y,z,EntityPlayer) -> BlockBehaviour.attack(BlockState,Level,BlockPos,Player) [BlockBehaviour.java:353]
	@Override protected final void attack(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnBlockClicked) ((IMTE_OnBlockClicked)aTileEntity).onBlockClicked(aPlayer); else super.attack(aState, aWorld, aPos, aPlayer);}
	// F13: 1.7.10 Block.velocityToAddToEntity удалён (ванильный дефолт был пуст; эффект имели лишь BlockPistonMoving/Portal) —
	// нет neo-хука. TE-интерфейс IMTE_VelocityToAddToEntity без implementor'ов (0, сверено) → мёртвая поверхность, не заглушка.
	public final void velocityToAddToEntity(Level aWorld, int aX, int aY, int aZ, Entity aEntity, Vec3 aVector) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_VelocityToAddToEntity) ((IMTE_VelocityToAddToEntity)aTileEntity).velocityToAddToEntity(aEntity, aVector);}
	// было isProvidingWeakPower(IBlockAccess,x,y,z,side) -> BlockBehaviour.getSignal(BlockState,BlockGetter,BlockPos,Direction) [BlockBehaviour.java:356]
	@Override protected final int getSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(UT.Code.side(aSide)) : super.getSignal(aState, aWorld, aPos, aSide);}
	// было onEntityCollidedWithBlock(World,x,y,z,Entity) -> BlockBehaviour.entityInside(BlockState,Level,BlockPos,Entity,InsideBlockEffectApplier,boolean) [BlockBehaviour.java:360];
	// новые параметры effectApplier/isPrecise (батч damage-эффектов, F16-концепция без 1.7.10-аналога) не используются - GT6 их и раньше не применял.
	@Override protected final void entityInside(BlockState aState, Level aWorld, BlockPos aPos, Entity aEntity, net.minecraft.world.entity.InsideBlockEffectApplier aEffectApplier, boolean aIsPrecise) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnEntityCollidedWithBlock) ((IMTE_OnEntityCollidedWithBlock)aTileEntity).onEntityCollidedWithBlock(aEntity); else super.entityInside(aState, aWorld, aPos, aEntity, aEffectApplier, aIsPrecise);}
	// было isProvidingStrongPower(IBlockAccess,x,y,z,side) -> BlockBehaviour.getDirectSignal(BlockState,BlockGetter,BlockPos,Direction) [BlockBehaviour.java:363]
	@Override protected final int getDirectSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_IsProvidingStrongPower ? ((IMTE_IsProvidingStrongPower)aTileEntity).isProvidingStrongPower(UT.Code.side(aSide)) : super.getDirectSignal(aState, aWorld, aPos, aSide);}
	public final boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_CanBlockStay) || ((IMTE_CanBlockStay)aTileEntity).canBlockStay();}
	// было onFallenUpon(World,x,y,z,Entity,dist) -> Block.fallOn(Level,BlockState,BlockPos,Entity,double) [Block.java:484] - fallDistance теперь double, не float.
	@Override public final void fallOn(Level aWorld, BlockState aState, BlockPos aPos, Entity aEntity, double aFallDistance) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnFallenUpon) ((IMTE_OnFallenUpon)aTileEntity).onFallenUpon(aEntity, (float)aFallDistance); else super.fallOn(aWorld, aState, aPos, aEntity, aFallDistance);}
	// F13: 1.7.10 Block.onBlockHarvested/onBlockPreDestroy удалены — neo destroy-пайплайн зовёт playerWillDestroy
	// (Level,BlockPos,BlockState,Player) перед снятием блока. Ниже neo-хук диспетчит оба TE-хука 1:1. GT6-методы сохранены.
	@Override public BlockState playerWillDestroy(Level aWorld, BlockPos aPos, BlockState aState, Player aPlayer) {
		BlockEntity tTE = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		int tMeta = WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (tTE instanceof IMTE_OnBlockHarvested) ((IMTE_OnBlockHarvested)tTE).onBlockHarvested(tMeta, aPlayer);
		if (tTE instanceof IMTE_OnBlockPreDestroy) ((IMTE_OnBlockPreDestroy)tTE).onBlockPreDestroy(tMeta);
		return super.playerWillDestroy(aWorld, aPos, aState, aPlayer);
	}
	public final void onBlockHarvested(Level aWorld, int aX, int aY, int aZ, int aMetaData, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockHarvested) ((IMTE_OnBlockHarvested)aTileEntity).onBlockHarvested(aMetaData, aPlayer);}
	public final void onBlockPreDestroy(Level aWorld, int aX, int aY, int aZ, int aMetaData) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockPreDestroy) ((IMTE_OnBlockPreDestroy)aTileEntity).onBlockPreDestroy(aMetaData);}
	// F13: 1.7.10 Block.fillWithRain(World,x,y,z) → neo Block.handlePrecipitation(state,level,pos,precipitation) (overridable).
	// Ниже neo-хук диспетчит IMTE_FillWithRain при ДОЖДЕ (fillWithRain был rain-specific), 1:1. GT6-метод сохранён.
	@Override public void handlePrecipitation(BlockState aState, Level aWorld, BlockPos aPos, net.minecraft.world.level.biome.Biome.Precipitation aPrecipitation) {
		BlockEntity tTE = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (aPrecipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN && tTE instanceof IMTE_FillWithRain) ((IMTE_FillWithRain)tTE).fillWithRain();
		else super.handlePrecipitation(aState, aWorld, aPos, aPrecipitation);
	}
	public final void fillWithRain(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_FillWithRain) ((IMTE_FillWithRain)aTileEntity).fillWithRain();}
	// было hasComparatorInputOverride()/getComparatorInputOverride(World,x,y,z,side) -> BlockBehaviour.hasAnalogOutputSignal(BlockState)
	// [BlockBehaviour.java:226] / BlockBehaviour.getAnalogOutputSignal(BlockState,Level,BlockPos,Direction) [BlockBehaviour.java:310];
	// дефолт (без IMTE-хука) = 0, тот же дефолт, что и у движка (BlockBehaviour.java:311).
	@Override protected final boolean hasAnalogOutputSignal(BlockState aState) {return T;}
	@Override protected final int getAnalogOutputSignal(BlockState aState, Level aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_GetComparatorInputOverride ? ((IMTE_GetComparatorInputOverride)aTileEntity).getComparatorInputOverride(UT.Code.side(aSide)) : aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(OPOS[UT.Code.side(aSide)]) : 0;}
	// было getLightValue(IBlockAccess,x,y,z) -> IBlockExtension.hasDynamicLightEmission(BlockState) [IBlockExtension.java:121] +
	// IBlockExtension.getLightEmission(BlockState,BlockGetter,BlockPos) [IBlockExtension.java:152]; дефолт (без IMTE-хука) =
	// aState.getLightEmission() (запечённое в Properties значение), тот же дефолт, что вернул бы super.getLightValue.
	@Override public final boolean hasDynamicLightEmission(BlockState aState) {return T;}
	// F6-worldgen КРИТ (дедлок): свет-движок зовёт getLightEmission ВО ВРЕМЯ генерации чанка (hasDynamicLightEmission=T) →
	// обычный WD.te форсит getChunk.join генерируемого чанка → light-поток ждёт сам себя → вечное зависание входа в мир.
	// Берём BE НЕблокирующе (WD.teNonForcing: только из уже-FULL чанка, иначе null → запечённый дефолт; BE-свет пересчитается после gen).
	@Override public final int getLightEmission(BlockState aState, BlockGetter aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.teNonForcing(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()); return aTileEntity instanceof IMTE_GetLightValue ? UT.Code.bind4(((IMTE_GetLightValue)aTileEntity).getLightValue()) : aState.getLightEmission();}
	public final boolean isLadder(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsLadder && ((IMTE_IsLadder)aTileEntity).isLadder(aEntity);}
	public final boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsNormalCube ? ((IMTE_IsNormalCube)aTileEntity).isNormalCube() : mNormalCube;}
	public final boolean isReplaceable(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsReplaceable ? ((IMTE_IsReplaceable)aTileEntity).isReplaceable() : getMaterial().isReplaceable();}
	public final boolean isBurning(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBurning && ((IMTE_IsBurning)aTileEntity).isBurning();}
	public final boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {if (aWorld == null) return F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsAir && ((IMTE_IsAir)aTileEntity).isAir();}
	// было removedByPlayer(World,EntityPlayer,x,y,z,willHarvest) -> IBlockExtension.onDestroyedByPlayer
	// (BlockState,Level,BlockPos,Player,ItemStack,boolean,FluidState) [IBlockExtension.java:238], возвращает
	// boolean "блок реально уничтожен" - тот же контракт, что и старый removedByPlayer.
	@Override public final boolean onDestroyedByPlayer(BlockState aBlockState, Level aWorld, BlockPos aPos, Player aPlayer, ItemStack aToolStack, boolean aWillHarvest, FluidState aFluid) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); return aTileEntity instanceof IMTE_RemovedByPlayer ? ((IMTE_RemovedByPlayer)aTileEntity).removedByPlayer(aWorld, aPlayer, aWillHarvest) : super.onDestroyedByPlayer(aBlockState, aWorld, aPos, aPlayer, aToolStack, aWillHarvest, aFluid);}
	public final boolean canCreatureSpawn(MobCategory aType, BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanCreatureSpawn && ((IMTE_CanCreatureSpawn)aTileEntity).canCreatureSpawn(aType);}
	public final boolean isBed(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBed && ((IMTE_IsBed)aTileEntity).isBed(aPlayer);}
	public final BlockPos getBedSpawnPosition(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_VelocityToAddToEntity ? ((IMTE_GetBedSpawnPosition)aTileEntity).getBedSpawnPosition(aPlayer) : null;}
	public final void setBedOccupied(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer, boolean aOccupied) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_SetBedOccupied) ((IMTE_SetBedOccupied)aTileEntity).setBedOccupied(aPlayer, aOccupied);}
	public final int getBedDirection(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBedDirection ? ((IMTE_GetBedDirection)aTileEntity).getBedDirection() : 0;}
	public final boolean isBedFoot(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBedFoot && ((IMTE_IsBedFoot)aTileEntity).isBedFoot();}
	// F13: 1.7.10 Forge Block.beginLeavesDecay нет в neo (leaf-decay-система иная). IMTE_BeginLeavesDecay без implementor'ов (0,
	// сверено) → мёртвая compile-поверхность, не заглушка. Дефолт был пустым телом
	// (Block.java:1956 recompSrc) - отсутствие super-вызова 1:1 эквивалентно "ничего не делать".
	public final void beginLeavesDecay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_BeginLeavesDecay) ((IMTE_BeginLeavesDecay)aTileEntity).beginLeavesDecay();}
	// F13: 1.7.10 Forge Block.canSustainLeaves нет в neo. IMTE_CanSustainLeaves без implementor'ов (0) → мёртвая поверхность.
	// Дефолт был false (Block.java:1967-1970 recompSrc) - подставлен напрямую вместо super. Не заглушка.
	public final boolean canSustainLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanSustainLeaves ? ((IMTE_CanSustainLeaves)aTileEntity).canSustainLeaves() : F;}
	public final boolean isLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsLeaves && ((IMTE_IsLeaves)aTileEntity).isLeaves();}
	public final boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanBeReplacedByLeaves && ((IMTE_CanBeReplacedByLeaves)aTileEntity).canBeReplacedByLeaves();}
	public final boolean isWood(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsWood ? ((IMTE_IsWood)aTileEntity).isWood() : F;}// было super.isWood (Forge 1.7.10 Block.isWood дефолт = false; neo Block метода не имеет)
	public final boolean isReplaceableOreGen(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, Block aTarget) {if (GAPI.mStartedServerStarted < 1) return F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsReplaceableOreGen ? ((IMTE_IsReplaceableOreGen)aTileEntity).isReplaceableOreGen(aTarget) : (aTarget == this);}// было super.isReplaceableOreGen (Forge 1.7.10 Block.isReplaceableOreGen дефолт = identity this==target)
	// было canConnectRedstone(IBlockAccess,x,y,z,side) -> IBlockExtension.canConnectRedstone(BlockState,BlockGetter,BlockPos,Direction) [IBlockExtension.java:904]
	@Override public final boolean canConnectRedstone(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_CanConnectRedstone ? ((IMTE_CanConnectRedstone)aTileEntity).canConnectRedstone(UT.Code.side(aSide)) : super.canConnectRedstone(aState, aWorld, aPos, aSide);}
	public final boolean canPlaceTorchOnTop(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanPlaceTorchOnTop ? ((IMTE_CanPlaceTorchOnTop)aTileEntity).canPlaceTorchOnTop() : isSideSolid(aWorld, aX, aY, aZ, FORGE_DIR[SIDE_TOP]);}
	// F13: 1.7.10 Forge Block.isFoliage нет в neo. IMTE_IsFoliage без implementor'ов (0) → мёртвая поверхность.
	// Дефолт был false (Block.java:2156-2159 recompSrc) - подставлен напрямую вместо super. Не заглушка.
	public final boolean isFoliage(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFoliage ? ((IMTE_IsFoliage)aTileEntity).isFoliage() : F;}
	// было canSustainPlant(IBlockAccess,x,y,z,side,IPlantable) -> IBlockExtension.canSustainPlant(BlockState,BlockGetter,
	// BlockPos,Direction,BlockState) [IBlockExtension.java:424], IPlantable(1.7.10-параметр)->BlockState(neo), TriState вместо boolean.
	// net.minecraftforge.common.IPlantable (F10 compile-only shim) остаётся для IMTE-хука; мост BlockState->IPlantable через
	// instanceof (GT6-плант-блоки реализуют IPlantable напрямую, см. compat-mirror/.../IPlantable.java). Дефолт (не-IPlantable
	// плант, либо нет IMTE-хука) = TriState.DEFAULT ("плант решает сам") - 1:1 с доком IBlockExtension.canSustainPlant,
	// заменяет прежнюю материал-зависимую vanilla-логику (та же семантика "движок решит").
	@Override public final net.minecraft.util.TriState canSustainPlant(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide, BlockState aPlant) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (!(aTileEntity instanceof IMTE_CanSustainPlant) || !(aPlant.getBlock() instanceof IPlantable aPlantable)) return net.minecraft.util.TriState.DEFAULT; return net.minecraft.util.TriState.from(((IMTE_CanSustainPlant)aTileEntity).canSustainPlant(UT.Code.side(aSide), aPlantable));}
	// F13: 1.7.10 Block.onPlantGrow удалён из neo (нет хука). IMTE_OnPlantGrow без implementor'ов (0) → мёртвая поверхность.
	// Ванильный дефолт был пуст → отсутствие super-вызова 1:1 эквивалентно "ничего не делать". Не заглушка.
	public final void onPlantGrow(Level aWorld, int aX, int aY, int aZ, int sX, int sY, int sZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnPlantGrow) ((IMTE_OnPlantGrow)aTileEntity).onPlantGrow(sX, sY, sZ);}
	public final boolean isFertile(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFertile && ((IMTE_IsFertile)aTileEntity).isFertile();}
	public final boolean rotateBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RotateBlock && ((IMTE_RotateBlock)aTileEntity).rotateBlock(UT.Code.side(aSide));}
	public final Direction[] getValidRotations(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetValidRotations ? ((IMTE_GetValidRotations)aTileEntity).getValidRotations() : ZL_FORGEDIRECTION;}
	public final float getEnchantPowerBonus(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetEnchantPowerBonus ? ((IMTE_GetEnchantPowerBonus)aTileEntity).getEnchantPowerBonus() : 0;}
	public final boolean recolourBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide, int aColor) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RecolourBlock && ((IMTE_RecolourBlock)aTileEntity).recolourBlock(UT.Code.side(aSide), (byte)aColor);}
	public final boolean shouldCheckWeakPower(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_ShouldCheckWeakPower ? ((IMTE_ShouldCheckWeakPower)aTileEntity).shouldCheckWeakPower(UT.Code.side(aSide)) : isNormalCube(aWorld, aX, aY, aZ);}
	// было getWeakChanges(IBlockAccess,x,y,z) -> IBlockExtension.getWeakChanges(BlockState,LevelReader,BlockPos)
	// [IBlockExtension.java:557], дефолт false (совпадает с прежним vanilla-дефолтом).
	@Override public final boolean getWeakChanges(BlockState aState, LevelReader aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_GetWeakChanges ? ((IMTE_GetWeakChanges)aTileEntity).getWeakChanges() : F;}
	public final boolean addHitEffects(Level aWorld, HitResult aTarget, ParticleEngine aRenderer) {BlockEntity aTileEntity = WD.te(aWorld, ((BlockHitResult)aTarget).getBlockPos().getX(), ((BlockHitResult)aTarget).getBlockPos().getY(), ((BlockHitResult)aTarget).getBlockPos().getZ(), T); return aTileEntity instanceof IMTE_AddHitEffects && ((IMTE_AddHitEffects)aTileEntity).addHitEffects(aWorld, aTarget, aRenderer);}
	public final boolean addDestroyEffects(Level aWorld, int aX, int aY, int aZ, int aMetaData, ParticleEngine aRenderer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_AddDestroyEffects && ((IMTE_AddDestroyEffects)aTileEntity).addDestroyEffects(aMetaData, aRenderer);}
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], семантика ИНВЕРТИРОВАНА (shouldRender -> skipRendering) И новая сигнатура не
	// передаёт World/BlockPos вовсе - невозможно определить TileEntity соседнего блока (IMTE_ShouldSideBeRendered),
	// как это делал 1.7.10-оригинал через aWorld.getTileEntity(aX-OFFX[side],...). F3 functional-adapted (neo skipRendering сигнатура потеряла World/BlockPos → per-TE culling недостижим; используется vanilla-дефолт super.skipRendering, 1:1 по следствию):
	// custom per-TE диспетчеризация недостижима без позиции; используем ванильный дефолт (тот же fallback,
	// что и в старой ветке super.shouldSideBeRendered, просто под новым именем/полярностью).
	@Override protected final boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {return super.skipRendering(aState, aNeighbor, aDir);}
	public final void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_SetBlockBoundsBasedOnState) ((IMTE_SetBlockBoundsBasedOnState)aTileEntity).setBlockBoundsBasedOnState(this); else if (aTileEntity == null) setBlockBounds(-999, -999, -999, -998, -998, -998); else setBlockBounds(0, 0, 0, 1, 1, 1);}
	public final AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity == null ? new AABB(-999, -999, -999, -998, -998, -998) : aTileEntity instanceof IMTE_GetSelectedBoundingBoxFromPool ? ((IMTE_GetSelectedBoundingBoxFromPool)aTileEntity).getSelectedBoundingBoxFromPool() : new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	// было randomDisplayTick(World,x,y,z,Random) -> Block.animateTick(BlockState,Level,BlockPos,RandomSource) [Block.java:355]
	@Override public final void animateTick(BlockState aState, Level aWorld, BlockPos aPos, RandomSource aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_RandomDisplayTick) ((IMTE_RandomDisplayTick)aTileEntity).randomDisplayTick(aRandom); else super.animateTick(aState, aWorld, aPos, aRandom);}
	public final void onBlockExploded(Level aWorld, int aX, int aY, int aZ, Explosion aExplosion) {if (aWorld.isClientSide()) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); if (aTileEntity instanceof IMTE_OnBlockExploded) ((IMTE_OnBlockExploded)aTileEntity).onExploded(aExplosion); else WD.set(aWorld, aX, aY, aZ, NB, 0, 3);}
	// F13: 1.7.10 Block.getPickBlock(HitResult,World,x,y,z,Player) удалён — neo middle-click идёт через
	// IBlockExtension.getCloneItemStack(LevelReader,BlockPos,BlockState,boolean,Player). Ниже — этот neo-хук
	// делегирует в GT6-getPickBlock (TE-диспетчер IMTE_GetPickBlock), восстанавливая поведение 1:1. GT6-методы сохранены.
	@Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, boolean aIncludeData, Player aPlayer) {
		BlockEntity tTE = WD.te(aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		ItemStack r = tTE instanceof IMTE_GetPickBlock ? ((IMTE_GetPickBlock)tTE).getPickBlock(null) : null;
		return ST.valid(r) ? r : super.getCloneItemStack(aLevel, aPos, aState, aIncludeData, aPlayer);
	}
	public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ                      ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	@Override public final ItemStack getItemStackFromBlock(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetStackFromBlock?((IMTE_GetStackFromBlock)aTileEntity).getStackFromBlock(aSide):null;}
	public final int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetFlammability ? ((IMTE_GetFlammability)aTileEntity).getFlammability(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	public final int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetFireSpreadSpeed ? ((IMTE_GetFireSpreadSpeed)aTileEntity).getFireSpreadSpeed(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	public final boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFireSource && ((IMTE_IsFireSource)aTileEntity).isFireSource(UT.Code.side(aSide));}
	public final boolean canEntityDestroy(BlockGetter aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_CanEntityDestroy) || ((IMTE_CanEntityDestroy)aTileEntity).canEntityDestroy(aEntity);}
	@Override public final long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_OnToolClick ? ((IMTE_OnToolClick)aTileEntity).onToolClick(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ) : 0;}
	@Override public final OreDictMaterialStack getMaterialAtSide(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetMaterialAtSide?((IMTE_GetMaterialAtSide)aTileEntity).getMaterialAtSide(aSide):null;}
	@Override public final boolean removeMaterialFromSide(Level aWorld, int aX, int aY, int aZ, byte aSide, OreDictMaterialStack aMaterial) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RemoveMaterialFromSide && ((IMTE_RemoveMaterialFromSide)aTileEntity).removeMaterialFromSide(aSide, aMaterial);}
	public final void dropBlockAsItemWithChance(Level aWorld, int aX, int aY, int aZ, int aMeta, float aChance, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, F, LAST_HARVESTING_PLAYER.get()); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	// было EnchantmentHelper.getSilkTouchModifier(Player)/getFortuneModifier(Player) (1.7.10) - удалены в neo;
	// реальный neo: EnchantmentHelper.getEnchantmentLevel(Holder<Enchantment>,LivingEntity) по Holder из RegistryAccess
	// (сверено, EnchantmentHelper.java:292 + Enchantments.SILK_TOUCH/FORTUNE), тот же приём, что уже принят и
	// одобрен ревизией в GT_API_Proxy.onBlockHarvestingEvent (GT_API_Proxy.java:1450-1451).
	public final void harvestBlock(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {if (aPlayer == null) aPlayer = LAST_HARVESTING_PLAYER.get(); aPlayer.awardStat(Stats.BLOCK_MINED.get(this), 1); /* было Stats.mineBlockStatArray[getIdFromBlock(this)] (1.7.10 int-ID) -> Stats.BLOCK_MINED.get(Block) [Stats.java:12] + Player.awardStat [Player.java:1413] */ UT.Entities.exhaust(aPlayer, 0.025F); Holder<Enchantment> tSilkTouchHolder = aWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH); Holder<Enchantment> tFortuneHolder = aWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE); boolean aSilkTouch = EnchantmentHelper.getEnchantmentLevel(tSilkTouchHolder, aPlayer) > 0; int aFortune = EnchantmentHelper.getEnchantmentLevel(tFortuneHolder, aPlayer); float aChance = 1.0F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, aSilkTouch); aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, aSilkTouch, aPlayer); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	public final ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aUnusableMetaData, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) return ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); return ST.arraylist();}
	// было aPlayer.level().getTileEntity(x,y,z) (1.7.10 World) -> центр WD.te(...) (тот же приём, что и все остальные
	// TE-lookup в этом файле), не прямой движковый вызов.
	@Override public final ArrayList<String> getDebugInfo(Player aPlayer, int aX, int aY, int aZ, int aScanLevel) {BlockEntity aTileEntity = WD.te(aPlayer.level(), aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetDebugInfo ? ((IMTE_GetDebugInfo)aTileEntity).getDebugInfo(aScanLevel) : null;}
	public final boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsSideSolid?((IMTE_IsSideSolid)aTileEntity).isSideSolid(UT.Code.side(aSide)):mOpaque;}
	public final boolean isBeaconBase(BlockGetter aWorld, int aX, int aY, int aZ, int aBeaconX, int aBeaconY, int aBeaconZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBeaconBase && ((IMTE_IsBeaconBase)aTileEntity).isBeaconBase(aBeaconX, aBeaconY, aBeaconZ);}
	public final int getLightOpacity(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetLightOpacity?((IMTE_GetLightOpacity)aTileEntity).getLightOpacity():mOpaque?LIGHT_OPACITY_MAX:LIGHT_OPACITY_NONE;}
	public final boolean isOpaqueCube() {return mOpaque;}
	public final boolean func_149730_j() {return mOpaque;}
	public final boolean renderAsNormalBlock() {return mOpaque || mNormalCube;}
	public final boolean isNormalCube()  {return mNormalCube;}
	// было canProvidePower() -> BlockBehaviour.isSignalSource(BlockState) [BlockBehaviour.java:218]
	@Override protected final boolean isSignalSource(BlockState aState) {return !mNormalCube;}
	@Override public final Block getBlock() {return this;}
	public final String getUnlocalizedName() {return mNameInternal;}
	// LOCALIZATION (РЕАЛИЗОВАНО): 1.7.10 vanilla Block.getLocalizedName() @Override удалён из neo (нет метода) → @Override снят,
	// метод функционален через ЦЕНТР локализации LH.get(key) (как FluidGT/BlockBaseFluid.getLocalizedName). Вся адаптация к движку —
	// в одном месте (gregapi.lang.LanguageHandler). Не заглушка. localization.csv=100% подтверждает.
	public final String getLocalizedName() {return LH.get(mNameInternal);}
	public final String getHarvestTool(int aMeta) {return mTool;}
	public final boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public final int getHarvestLevel(int aMeta) {return (int)UT.Code.bind_(mHarvestLevelMinimum, mHarvestLevelMaximum, mHarvestLevelOffset + aMeta);}
	// было canHarvestBlock(EntityPlayer,meta) -> IBlockExtension.canHarvestBlock(BlockState,BlockGetter,BlockPos,Player)
	// [IBlockExtension.java:215], дефолт EventHooks.doPlayerHarvestCheck(...) (сам же дефолт интерфейса) - тот же
	// приём, что вызывался через super раньше (ноль GT6-specific логики в этом методе, чистая passthrough-точка).
	@Override public final boolean canHarvestBlock(BlockState aState, BlockGetter aWorld, BlockPos aPos, Player aPlayer) {return EventHooks.doPlayerHarvestCheck(aPlayer, aState, aWorld, aPos);}
	public final boolean hasTileEntity(int aMeta) {return T;}
	public final boolean canSilkHarvest() {return F;}
	public final int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	public final BlockEntity createNewTileEntity(Level aWorld, int aMeta) {return null;}
	public final BlockEntity createTileEntity(Level aWorld, int aMeta) {return null;}
	// было EntityBlock.newBlockEntity(BlockPos,BlockState) (neo, EntityBlock.java:14, обязательный т.к. класс implements
	// EntityBlock) - GT6 TE-создание для MultiTileEntityBlock идёт НЕ через это (сверено оригиналом: createNewTileEntity/
	// createTileEntity УЖЕ возвращают null и в 1.7.10-оракуле, MultiTileEntityBlock.java:282-283), а через
	// MultiTileEntityRegistry.getNewTileEntity (см. receiveData выше) - сетевой/явный путь. null здесь 1:1 с оракулом,
	// не деградация.
	@Override public final BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {return null;}
	// F-tick (шов «тики BE», центр на весь MTE-класс): 1.7.10 World сам тикал TileEntity с canUpdate()==true
	// (updateEntity каждый тик, ОБЕ стороны — recompSrc World.updateEntities); в neo тики BE идут ТОЛЬКО через
	// EntityBlock.getTicker → BlockEntityTicker (Level.tickBlockEntities). Без шва весь механизм TE03+ мёртв:
	// onTick*, sendClientData-синк клиенту (place-путь), анимации (mLidAngle), doBlockUpdate. canUpdate 1:1 (TE01:572).
	@Override public final <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, net.minecraft.world.level.block.entity.BlockEntityType<T> aType) {
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
	public final void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public final Identifier getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	public final Identifier getIcon(int aSide, int aMetaData) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	// F3-render (отложенная фаза): 1.7.10 Block.getRenderType()/super.getRenderType() удалён из neo (рендер
	// data-driven через модели/getRenderShape; getRenderType в neo-пайплайне не вызывается — вызыватели ушли,
	// см. ToolCompat instanceof-миграцию). super.getRenderType() -> -1 («нет кастом-render-ID»); при живом GT6-
	// клиент-рендерере возвращает его mRenderID. Метод сохранён для клиентской F3-фазы, движком не зовётся.
	public final int getRenderType() {return RendererBlockTextured.INSTANCE==null?-1:RendererBlockTextured.INSTANCE.mRenderID;}
	@Override public final IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T); return tTileEntity instanceof IRenderedBlockObject ? (IRenderedBlockObject)tTileEntity : null;}
	// было onBlockEventReceived(World,x,y,z,id,data) -> BlockBehaviour.triggerEvent(BlockState,Level,BlockPos,int,int)
	// [BlockBehaviour.java:206]; TileEntity.receiveClientEvent(id,data) -> BlockEntity.triggerEvent(int,int) [BlockEntity.java:270]
	@Override protected final boolean triggerEvent(BlockState aState, Level aWorld, BlockPos aPos, int aID, int aParam) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity == null || aTileEntity.triggerEvent(aID, aParam);}
	// было getPlayerRelativeBlockHardness(EntityPlayer,World,x,y,z) -> BlockBehaviour.getDestroyProgress
	// (BlockState,Player,BlockGetter,BlockPos) [BlockBehaviour.java:340]
	@Override protected final float getDestroyProgress(BlockState aState, Player aPlayer, BlockGetter aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_GetPlayerRelativeBlockHardness ? ((IMTE_GetPlayerRelativeBlockHardness)aTileEntity).getPlayerRelativeBlockHardness(aPlayer, super.getDestroyProgress(aState, aPlayer, aWorld, aPos)) : super.getDestroyProgress(aState, aPlayer, aWorld, aPos);}
	// F13: движок-facing динамическая твёрдость (скорость добычи игроком) ПОДКЛЮЧЕНА выше через getDestroyProgress
	// (стр. ~469, TE-диспетчер IMTE_GetPlayerRelativeBlockHardness). Этот getBlockHardness — внутренний GT6-хелпер
	// (TE-твёрдость для собственной логики), не стаб; движок его не зовёт (у него getDestroyProgress).
	public final float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBlockHardness?((IMTE_GetBlockHardness)aTileEntity).getBlockHardness():1.0F;}
	// было getExplosionResistance(Entity,World,x,y,z,expX,expY,expZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]; Explosion.getDirectSourceEntity()/center() заменяют потерянные параметры
	@Override public final float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); Vec3 aCenter = aExplosion.center(); return aTileEntity instanceof IMTE_GetExplosionResistance?((IMTE_GetExplosionResistance)aTileEntity).getExplosionResistance(aExplosion.getDirectSourceEntity(), aCenter.x, aCenter.y, aCenter.z):1.0F;}
	// было onNeighborChange(IBlockAccess,x,y,z,tileX,Y,Z) -> IBlockExtension.onNeighborChange(BlockState,LevelReader,BlockPos,BlockPos) [IBlockExtension.java:534]
	@Override public final void onNeighborChange(BlockState aState, LevelReader aWorld, BlockPos aPos, BlockPos aNeighbor) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aNeighbor.getX(), aNeighbor.getY(), aNeighbor.getZ()); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborChange) ((IMTE_OnNeighborChange)aTileEntity).onNeighborChange(aWorld, aNeighbor.getX(), aNeighbor.getY(), aNeighbor.getZ());}
	public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aX, aY, aZ); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborBlockChange) ((IMTE_OnNeighborBlockChange)aTileEntity).onNeighborBlockChange(aWorld, aBlock); if (aTileEntity == null) WD.set(aWorld, aX, aY, aZ, NB, 0, 3);}
	@Override public final boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return T;}
	@Override public final boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return T;}
	@Override public final void receiveBlockError(BlockGetter aWorld, int aX, int aY, int aZ, String aError) {BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (tTileEntity instanceof ITileEntity) {((ITileEntity)tTileEntity).setError(aError); WD.update(aWorld, aX, aY, aZ); UT.Sounds.play(SFX.GT_BEEP, 100, 1.0F, aX, aY, aZ);}}
	@Override public final void onWalkOver(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnWalkOver) ((IMTE_OnWalkOver)aTileEntity).onWalkOver(aEntity);}
}
