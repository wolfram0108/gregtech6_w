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

package gregapi.block.prefixblock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block.SoundType;

import gregapi.GT_API_Proxy;
import gregapi.block.IBlockSyncData;
import gregapi.block.IBlockToolable;
import gregapi.block.IPrefixBlock;
import gregapi.block.ToolCompat;
import gregapi.block.behaviors.Drops;
import gregapi.code.ModData;
import gregapi.data.*;
import gregapi.lang.LanguageHandler;
import gregapi.network.INetworkHandler;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.random.ExplosionGT;
import gregapi.render.*;
import gregapi.tileentity.ITileEntity;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import mekanism.api.MekanismAPI;
import mods.railcraft.common.carts.EntityTunnelBore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.EntityBlock;
import gregapi.block.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.IIcon;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class PrefixBlock extends Block implements Runnable, EntityBlock, IBlockSyncData, IRenderedBlock, IBlockToolable, IPrefixBlock {
	public Drops mDrops;
	public boolean mRegisterToOreDict = T, mHidden = F;
	
	public final float mMinX, mMinY, mMinZ, mMaxX, mMaxY, mMaxZ;
	public final int mHarvestLevelOffset, mHarvestLevelMinimum, mHarvestLevelMaximum;
	public final ITexture mTexture;
	public final String mNameInternal, mTool, mModIDOwner;
	public final OreDictPrefix mPrefix;
	public final OreDictMaterialStack mHullMaterial;
	public final OreDictMaterial[] mMaterialList;
	public final float mBaseHardness, mBaseResistance;
	public final boolean mGravity, mBeaconBase, mEnderDragonProof, mWitherProof, mSpawnProof, mOpaque, mNormalCube, mPlacementChecksTemperature, mPlacementChecksAntimatter, mCanBurn, mCanExplode, mRenderOverlayInWorld, mCanGlow, mCanLight;
	
	@Deprecated
	public PrefixBlock(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, OreDictMaterialStack aHullMaterial, Class<? extends PrefixBlockItem> aItemClass, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aGravity, boolean aBeaconBase, boolean aEnderDragonProof, boolean aWitherProof, boolean aOpaque, boolean aNormalCube, boolean aPlacementChecksTemperature, boolean aPlacementChecksAntimatter, boolean aCanBurn, boolean aCanExplode, boolean aRenderOverlayInWorld, boolean aCanGlow, boolean aCanLight, boolean aSpawnProof) {
		this(aModIDOwner, aModIDTextures, aNameInternal, aPrefix, aHullMaterial, aItemClass, aDrops, aTexture, aVanillaMaterial, aSoundType, aTool, aBaseHardness, aBaseResistance, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, 0, 0, 0, 1, 1, 1, aGravity, aBeaconBase, aEnderDragonProof, aWitherProof, aOpaque, aNormalCube, aPlacementChecksTemperature, aPlacementChecksAntimatter, aCanBurn, aCanExplode, aRenderOverlayInWorld, aCanGlow, aCanLight, aSpawnProof);
	}
	
	@Deprecated
	public PrefixBlock(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, OreDictMaterialStack aHullMaterial, Class<? extends PrefixBlockItem> aItemClass, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ, boolean aGravity, boolean aBeaconBase, boolean aEnderDragonProof, boolean aWitherProof, boolean aOpaque, boolean aNormalCube, boolean aPlacementChecksTemperature, boolean aPlacementChecksAntimatter, boolean aCanBurn, boolean aCanExplode, boolean aRenderOverlayInWorld, boolean aCanGlow, boolean aCanLight, boolean aSpawnProof) {
		this(aModIDOwner, aModIDTextures, aNameInternal, aPrefix, aHullMaterial, aItemClass, aDrops, aTexture, aVanillaMaterial, aSoundType, aTool, aBaseHardness, aBaseResistance, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ, aGravity, aBeaconBase, aEnderDragonProof, aWitherProof, aOpaque, aNormalCube, aPlacementChecksTemperature, aPlacementChecksAntimatter, aCanBurn, aCanExplode, aRenderOverlayInWorld, aCanGlow, aCanLight, aSpawnProof, OreDictMaterial.MATERIAL_ARRAY);
	}
	
	/**
	 * Specific for Ore Block creation
	 */
	public PrefixBlock(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, boolean aGravity, boolean aEnderDragonProof, OreDictMaterial... aMaterialList) {
		this(aModIDOwner, aModIDTextures, aNameInternal, aPrefix, null, null, aDrops, aTexture, aVanillaMaterial, aSoundType, aTool, aBaseHardness, aBaseResistance, aHarvestLevelOffset, aHarvestLevelMinimum, 999, 0, 0, 0, 1, 1, 1, aGravity, F, aEnderDragonProof, F, T, T, F, F, T, T, T, T, T, F, aMaterialList);
	}
	
	/**
	 * Specific for Ore Block creation
	 * Only saves on one Parameter by using 1 instead of 2 Mod IDs.
	 */
	public PrefixBlock(ModData aMod, String aNameInternal, OreDictPrefix aPrefix, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, boolean aGravity, boolean aEnderDragonProof, OreDictMaterial... aMaterialList) {
		this(aMod.mID, aMod.mID, aNameInternal, aPrefix, null, null, aDrops, aTexture, aVanillaMaterial, aSoundType, aTool, aBaseHardness, aBaseResistance, aHarvestLevelOffset, aHarvestLevelMinimum, 999, 0, 0, 0, 1, 1, 1, aGravity, F, aEnderDragonProof, F, T, T, F, F, T, T, T, T, T, F, aMaterialList);
	}
	
	/**
	 * Only saves on one Parameter by using 1 instead of 2 Mod IDs.
	 */
	public PrefixBlock(ModData aMod, String aNameInternal, OreDictPrefix aPrefix, OreDictMaterialStack aHullMaterial, Class<? extends PrefixBlockItem> aItemClass, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ, boolean aGravity, boolean aBeaconBase, boolean aEnderDragonProof, boolean aWitherProof, boolean aOpaque, boolean aNormalCube, boolean aPlacementChecksTemperature, boolean aPlacementChecksAntimatter, boolean aCanBurn, boolean aCanExplode, boolean aRenderOverlayInWorld, boolean aCanGlow, boolean aCanLight, boolean aSpawnProof, OreDictMaterial... aMaterialList) {
		this(aMod.mID, aMod.mID, aNameInternal, aPrefix, aHullMaterial, aItemClass, aDrops, aTexture, aVanillaMaterial, aSoundType, aTool, aBaseHardness, aBaseResistance, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ, aGravity, aBeaconBase, aEnderDragonProof, aWitherProof, aOpaque, aNormalCube, aPlacementChecksTemperature, aPlacementChecksAntimatter, aCanBurn, aCanExplode, aRenderOverlayInWorld, aCanGlow, aCanLight, aSpawnProof, aMaterialList);
	}
	
	/**
	 * Just create one instance of this Block and everything else is getting registered automatically.
	 * 
	 * @param aModIDOwner the ID of the owning Mod. DO NOT INSERT ANY GREGTECH MODID!!!
	 * @param aModIDTextures the ID of the Texture providing Mod (for the "ModID:TextureName" thing)
	 * @param aNameInternal the internal Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 * @param aPrefix the OreDictPrefix corresponding to this Item.
	 * @param aHullMaterial the Material the Hull consists of. Can be null.
	 * @param aItemClass the Class of the ItemBlock to be used. If you pass null it will default to the regular MetaBlockItem Class.
	 * @param aTexture the Texture underlay for this Block. Used for Ores and Crates. Can be null to use normal Rendering.
	 * @param aVanillaMaterial the Material used to determine the Block.
	 * @param aSoundType the Sound Type of the Block.
	 * @param aTool the Tool used to harvest this Block.
	 * @param aBaseHardness if smaller than zero, then this Block is indestructible.
	 * @param aBaseResistance
	 * @param aHarvestLevelOffset
	 * @param aHarvestLevelMinimum
	 * @param aHarvestLevelMaximum
	 * @param aMinX
	 * @param aMinY
	 * @param aMinZ
	 * @param aMaxX
	 * @param aMaxY
	 * @param aMaxZ
	 * @param aGravity if this Block falls like Gravel.
	 * @param aSpawnProof if this Block cannot spawn Mobs.
	 * @param aBeaconBase if this Block can be used as Beacon Base.
	 * @param aEnderDragonProof if this Block cannot be destroyed by an Ender Dragon (used for the End Ores).
	 * @param aWitherProof if this Block cannot be destroyed by a Wither.
	 * @param aOpaque if this Block is Opaque.
	 * @param aNormalCube if this Block is a normal Cube (for Redstone Stuff).
	 * @param aPlacementChecksTemperature if this Block checks for Temperature to be proper before placing.
	 * @param aPlacementChecksAntimatter if this Block checks for being Antimatter before placing.
	 * @param aCanBurn if this Block can burn if the Material it is made of can burn.
	 * @param aCanExplode if this Block can explode if the Material it is made of can explode.
	 * @param aRenderOverlayInWorld if the Icon Overlay is to be rendered InWorld. Used for Crates and Ores.
	 */
	public PrefixBlock(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, OreDictMaterialStack aHullMaterial, Class<? extends PrefixBlockItem> aItemClass, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ, boolean aGravity, boolean aBeaconBase, boolean aEnderDragonProof, boolean aWitherProof, boolean aOpaque, boolean aNormalCube, boolean aPlacementChecksTemperature, boolean aPlacementChecksAntimatter, boolean aCanBurn, boolean aCanExplode, boolean aRenderOverlayInWorld, boolean aCanGlow, boolean aCanLight, boolean aSpawnProof, OreDictMaterial... aMaterialList) {
		super(aVanillaMaterial);
		mPrefix = aPrefix;
		mNameInternal = aNameInternal;
		mMaterialList = (aMaterialList.length > 0 ? aMaterialList : OreDictMaterial.MATERIAL_ARRAY);
		if (mMaterialList[0] != MT.Empty) throw new IllegalArgumentException("The first element of the custom Material List has to be MT.Empty for technical reasons!");
		
		mMinX = (float)aMinX; mMinY = (float)aMinY; mMinZ = (float)aMinZ; mMaxX = (float)aMaxX; mMaxY = (float)aMaxY; mMaxZ = (float)aMaxZ;
		
		/* PORT-TODO(F16) setStepSound */;
		mOpaque = aOpaque;
		mGravity = aGravity;
		mCanBurn = aCanBurn;
		mCanGlow = aCanGlow;
		mCanLight = aCanLight;
		mCanExplode = aCanExplode;
		mNormalCube = aNormalCube;
		mBeaconBase = aBeaconBase;
		mSpawnProof = aSpawnProof;
		mWitherProof = aWitherProof;
		mEnderDragonProof = aEnderDragonProof;
		mRenderOverlayInWorld = aRenderOverlayInWorld;
		mPlacementChecksAntimatter = aPlacementChecksAntimatter;
		mPlacementChecksTemperature = aPlacementChecksTemperature;
		
		mTool = aTool.toLowerCase();
		mTexture = aTexture;
		mModIDOwner = aModIDOwner;
		mHullMaterial = aHullMaterial;
		mBaseHardness = aBaseHardness;
		mBaseResistance = aBaseResistance;
		mHarvestLevelOffset = aHarvestLevelOffset;
		mHarvestLevelMinimum = Math.max(0, aHarvestLevelMinimum);
		mHarvestLevelMaximum = Math.max(aHarvestLevelMinimum, aHarvestLevelMaximum);
		mPrefix.addTextureSet(aModIDTextures, F);
		LH.add("oredict." + mPrefix.dat(MT.Empty).toString(), getLocalName(mPrefix, MT.Empty));
		LH.add(mNameInternal+"."+W, "Any Sub-Block of this one"); // Local Name for the WildcardItem Variant.
		
		opaque = mOpaque;
		lightOpacity = mOpaque ? 255 : 0;
		
		ST.register(this, mNameInternal, aItemClass==null?PrefixBlockItem.class:aItemClass);
		
		mPrefix.mRegisteredItems.add(this); // this optimizes some processes by decreasing the size of the Set.
		
		if (mPrefix.contains(TD.Prefix.ORE)) {
			if (COMPAT_FR  != null) COMPAT_FR.addToBackpacks("miner", ST.make(this, 1, W));
			if (COMPAT_IC2 != null && mBaseHardness >= 0) {
				for (byte i = 0; i < 16; i++) COMPAT_IC2.valuable(this, i, 3);
			}
		} else if (mPrefix.containsAny(TD.Prefix.DUST_BASED, TD.Prefix.INGOT_BASED, TD.Prefix.GEM_BASED)) {
			if (COMPAT_FR  != null) COMPAT_FR.addToBackpacks("miner", ST.make(this, 1, W));
		} else {
			if (COMPAT_FR  != null) COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W));
		}
		
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (MD.Mek.mLoaded) try {MekanismAPI.addBoxBlacklist(this, W);} catch(Throwable e) {e.printStackTrace(ERR);}
		
		if (mOpaque) VISUALLY_OPAQUE_BLOCKS.add(this);
		mDrops = aDrops==null?new Drops(this, this, this, this, F, F, 0, 0):aDrops;
		
		if (CODE_CLIENT) MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(this), RendererBlockTextured.INSTANCE);
		
		// Execute before all the other things. This is to ensure that PrefixBlocks are created before MultiItems.
		(GAPI.mBeforeInit==null?GAPI.mBeforePostInit:GAPI.mBeforeInit).add(0, this);
	}
	
	/** This ensures, that all Materials are registered at the time this Item registers to the OreDictionary. */
	@Override
	public void run() {
		for (short i = 0; i < mMaterialList.length; i++) if (mPrefix.isGeneratingItem(mMaterialList[i])) {
			LH.add("oredict." + mPrefix.dat(mMaterialList[i]).toString(), getLocalName(mPrefix, mMaterialList[i]));
		}
		if (mRegisterToOreDict) {
			boolean tUnificationAllowed = (mPrefix.contains(TD.Prefix.UNIFICATABLE) && !mPrefix.contains(TD.Prefix.UNIFICATABLE_RECIPES));
			for (short i = 0; i < mMaterialList.length; i++) if (mPrefix.isGeneratingItem(mMaterialList[i])) {
				ItemStack tStack = ST.update_(ST.make(this, 1, i));
				if (tUnificationAllowed) OreDictManager.INSTANCE.addTarget_(mPrefix, mMaterialList[i], tStack); else OreDictManager.INSTANCE.registerOre_(mPrefix, mMaterialList[i], tStack);
			}
		}
	}
	
	// @Override
	public void registerBlockIcons(IIconRegister aIconRegister) {/*
		if (mPrefix.mIconIndexBlock >= 0) {
			MT.NULL.mTextureSetsBlock.get(mPrefix.mIconIndexBlock).registerIcons(aIconRegister);
			HashSet<IIconContainer> tSet = new HashSet<IIconContainer>();
			for (int i = 0; i < mMaterialList.length; i++) if (mMaterialList[i] != null && mMaterialList[i].mTextureSetsBlock != null) {
				IIconContainer tIcon = mMaterialList[i].mTextureSetsBlock.get(mPrefix.mIconIndexBlock);
				if (tSet.add(tIcon)) tIcon.registerIcons(aIconRegister);
			}
		}*/
	}
	
	// @Override
	public IIcon getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		return getIcon(aSide, getMetaDataValue(aWorld, aX, aY, aZ));
	}
	
	// @Override
	public IIcon getIcon(int aSide, int aMetaData) {
		if (mPrefix.mIconIndexBlock >= 0) {
			OreDictMaterial aMaterial = getMetaMaterial(aMetaData);
			if (aMaterial != null && aMaterial.mTextureSetsBlock != null)
			return aMaterial    .mTextureSetsBlock.get(mPrefix.mIconIndexBlock).getIcon(0);
			return MT.NULL      .mTextureSetsBlock.get(mPrefix.mIconIndexBlock).getIcon(0);
		}
		return null;
	}
	
	// @Override
	public int getRenderColor(int aMetaData) {
		OreDictMaterial aMaterial = getMetaMaterial(aMetaData);
		return aMaterial == null ? super.getRenderColor(aMetaData) : UT.Code.getRGBInt(aMaterial.fRGBa[mPrefix.mState]);
	}
	
	public ITexture getTexture(short aMetaData, boolean aRendersInWorld) {
		if (!mRenderOverlayInWorld && aRendersInWorld) return mTexture;
		if (mPrefix.mIconIndexBlock >= 0) {
			OreDictMaterial aMaterial = getMetaMaterial(aMetaData);
			if (mTexture == null) {
				if (aMaterial != null && aMaterial.mTextureSetsBlock != null)
				return BlockTextureDefault.get(aMaterial, mPrefix, mCanGlow && aMaterial.contains(TD.Properties.GLOWING));
				return BlockTextureDefault.get(MT.NULL, mPrefix);
			}
			if (aMaterial != null && aMaterial.mTextureSetsBlock != null)
			return BlockTextureMulti.get(mTexture, BlockTextureDefault.get(aMaterial, mPrefix, mCanGlow && aMaterial.contains(TD.Properties.GLOWING)));
			return BlockTextureMulti.get(mTexture, BlockTextureDefault.get(MT.NULL, mPrefix));
		}
		return null;
	}
	
	@Override
	public ITexture getTexture(int aRenderPass, byte aSide, ItemStack aStack) {
		return getTexture(ST.meta_(aStack), F);
	}
	
	@Override
	public ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {
		return aShouldSideBeRendered[aSide] ? getTexture(getMetaDataValue(aWorld, aX, aY, aZ), T) : null;
	}
	
	@Override
	public boolean setBlockBounds(int aRenderPass, ItemStack aStack) {
		return F;
	}
	
	@Override
	public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {
		return F;
	}
	
	@Override
	public int getRenderPasses(ItemStack aStack) {
		return 1;
	}
	
	@Override
	public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {
		return 1;
	}
	
	@Override
	public IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockEntity tRenderParameterTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		return mRenderingObjectBlock != null ? mRenderingObjectBlock : tRenderParameterTileEntity instanceof IRenderedBlockObject ? (IRenderedBlockObject)tRenderParameterTileEntity : null;
	}
	
	@Override
	public IRenderedBlockObject passRenderingToObject(ItemStack aStack) {
		return mRenderingObjectStack;
	}
	
	public IRenderedBlockObject mRenderingObjectBlock = null, mRenderingObjectStack = null;
	
	public PrefixBlock setRenderingObject(IRenderedBlockObject aBlock, IRenderedBlockObject aStack) {
		mRenderingObjectBlock = aBlock;
		mRenderingObjectStack = aStack;
		return this;
	}
	
	private static boolean LOCK = F;
	
	// @Override
	public void onNeighborChange(BlockGetter aWorld, int aX, int aY, int aZ, int aTileX, int aTileY, int aTileZ) {
		if (!LOCK) {
			LOCK = T;
			BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
			if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aTileX, aTileY, aTileZ);
			LOCK = F;
		}
	}
	
	// @Override
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		BlockEntity aTileEntity = null;
		if (!LOCK) {
			LOCK = T;
			aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
			if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aX, aY, aZ);
			LOCK = F;
		}
		scheduleUpdateIfNeeded(aWorld, aX, aY, aZ, aTileEntity);
	}
	
	public boolean scheduleUpdateIfNeeded(Level aWorld, int aX, int aY, int aZ, BlockEntity aTileEntity) {
		if (mGravity && aY > 0 && FallingBlock.func_149831_e(aWorld, aX, aY - 1, aZ)) {
			aWorld.scheduleBlockUpdate(aX, aY, aZ, this, 2);
			return T;
		}
		if (aTileEntity == null) return F;
		if (!mCanBurn && !mCanExplode) return F;
		if (mPrefix.contains(TD.Prefix.DUST_BASED)) {
			aWorld.scheduleBlockUpdate(aX, aY, aZ, this, 2);
			return T;
		}
		OreDictMaterial aMaterial = getMetaMaterial(aTileEntity);
		if (aMaterial.containsAny(TD.Properties.FLAMMABLE, TD.Properties.EXPLOSIVE, TD.Atomic.ALKALI_METAL)) {
			aWorld.scheduleBlockUpdate(aX, aY, aZ, this, 2);
			return T;
		}
		return F;
	}
	
	// @Override
	public void onBlockExploded(Level aWorld, int aX, int aY, int aZ, Explosion aExplosion) {
		if (aWorld.isClientSide()) return;
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		OreDictMaterial aMaterial = getMetaMaterial(aTileEntity);
		WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
		if (aMaterial != null && ((mCanExplode && aMaterial.contains(TD.Properties.EXPLOSIVE)) || (mCanBurn && aMaterial.contains(TD.Properties.FLAMMABLE) && mPrefix.contains(TD.Prefix.DUST_BASED)))) try {ExplosionGT.explode(aWorld, null, aX+0.5, aY+0.5, aZ+0.5, ((mPrefix.mAmount>0?mPrefix.mAmount:U)*0.7F)/U, T, T);} catch(StackOverflowError e) {ERR.println("WARNING: StackOverflow during Explosion has been prevented at: " + aX +" ; "+ aY +" ; "+ aZ);}
	}
	
	// @Override
	public float getExplosionResistance(Entity par1Entity, Level aWorld, int aX, int aY, int aZ, double explosionX, double explosionY, double explosionZ)       {
		OreDictMaterial aMaterial = getMetaMaterial(aWorld, aX, aY, aZ);
		if (aMaterial != null && ((mCanExplode && aMaterial.contains(TD.Properties.EXPLOSIVE)) || (mCanBurn && aMaterial.contains(TD.Properties.FLAMMABLE) && mPrefix.contains(TD.Prefix.DUST_BASED)))) return 0;
		return mBaseResistance * (1+getHarvestLevel(WD.meta(aWorld, aX, aY, aZ)));
	}
	
	// @Override
	public boolean onBlockEventReceived(Level aWorld, int aX, int aY, int aZ, int aID, int aData) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		return aTileEntity == null || aTileEntity.receiveClientEvent(aID, aData);
	}
	
	// @Override
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {
		return getMetaDataValue(aWorld, aX, aY, aZ);
	}
	
	// @Override
	public ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {
		return getItemStackFromBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN);
	}
	
	// @Override
	public void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int par6) {
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity != null) LAST_BROKEN_TILEENTITY.set(tTileEntity);
		aWorld.removeTileEntity(aX, aY, aZ);
	}
	
	@Override
	public boolean placeBlock(Level aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		OreDictMaterial aMaterial = getMetaMaterial(aMetaData);
		if (aMaterial != null && (aForcePlacement || ((!mPlacementChecksAntimatter || !aMaterial.contains(TD.Atomic.ANTIMATTER)) && (!mPlacementChecksTemperature || aMaterial.mMeltingPoint > WD.temperature(aWorld, aX, aY, aZ)))) && WD.set(aWorld, aX, aY, aZ, this, UT.Code.bind4(aMaterial.mToolQuality), aCauseBlockUpdates?3:0)) {
			// This darn TileEntity update is ruining World generation Code (infinite Loops when placing TileEntities on Chunk Borders). I'm glad I finally found a way to disable it.
			BlockEntity tTileEntity = createTileEntity(aWorld, aX, aY, aZ, aSide, aMetaData, aNBT);
			WD.te(aWorld, aX, aY, aZ, tTileEntity, aCauseBlockUpdates);
			scheduleUpdateIfNeeded(aWorld, aX, aY, aZ, tTileEntity);
			if (!aWorld.isClientSide()) GT_API_Proxy.SCHEDULED_TILEENTITY_UPDATES.add((PrefixBlockTileEntity)tTileEntity);
			return T;
		}
		return F;
	}
	
	@Override
	public ItemStack getItemStackFromBlock(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		return ST.make(this, 1, getMetaDataValue(aTileEntity), aTileEntity instanceof PrefixBlockTileEntity ? ((PrefixBlockTileEntity)aTileEntity).mItemNBT : null);
	}
	
	// @Override
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {
		OreDictMaterialStack aMaterial = getMaterialAtSide(aWorld, aX, aY, aZ, UT.Code.side(aSide));
		return aMaterial == null || !mCanBurn || aMaterial.mMaterial.contains(TD.Properties.UNBURNABLE) ? 0 : (aMaterial.mMaterial.contains(TD.Properties.FLAMMABLE)?100:0) + (aMaterial.mMaterial.contains(TD.Properties.BURNING)?200:0);
	}
	
	// @Override
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {
		OreDictMaterialStack aMaterial = getMaterialAtSide(aWorld, aX, aY, aZ, UT.Code.side(aSide));
		return aMaterial == null || !mCanBurn || aMaterial.mMaterial.contains(TD.Properties.UNBURNABLE) ? 0 : (aMaterial.mMaterial.contains(TD.Properties.FLAMMABLE)?100:0) + (aMaterial.mMaterial.contains(TD.Properties.BURNING)?200:0);
	}
	
	// @Override
	public boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {
		OreDictMaterialStack aMaterial = getMaterialAtSide(aWorld, aX, aY, aZ, UT.Code.side(aSide));
		return aMaterial != null && mCanBurn && aMaterial.mMaterial.contains(TD.Properties.FLAMMABLE) && aMaterial.mMaterial.contains(TD.Properties.UNBURNABLE);
	}
	
	// @Override
	public boolean canEntityDestroy(BlockGetter aWorld, int aX, int aY, int aZ, Entity aEntity) {
		if (aEntity instanceof EnderDragon) {
			if (mEnderDragonProof) return F;
			OreDictMaterialStack aMaterial = getMaterialAtSide(aWorld, aX, aY, aZ, SIDE_ANY);
			return aMaterial == null || !aMaterial.mMaterial.contains(TD.Properties.ENDER_DRAGON_PROOF);
		}
		if (aEntity instanceof WitherBoss) {
			if (mWitherProof) return F;
			OreDictMaterialStack aMaterial = getMaterialAtSide(aWorld, aX, aY, aZ, SIDE_ANY);
			return aMaterial == null || !aMaterial.mMaterial.contains(TD.Properties.WITHER_PROOF);
		}
		return T;
	}
	
	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, AbstractContainerMenu aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {
		OreDictMaterial aMaterial = getMetaMaterial(aWorld, aX, aY, aZ);
		if (!aWorld.isClientSide() && aTool.equals(TOOL_magnifyingglass)) {
			if (aChatReturn != null) aChatReturn.add("This is " + getLocalName(mPrefix, aMaterial));
			return 1;
		}
		if (!aWorld.isClientSide() && aTool.equals(TOOL_prospector) && mPrefix.contains(TD.Prefix.ORE)) {
			if (aChatReturn != null) aChatReturn.add(getLocalName(OP.ore, aMaterial)+"!");
			return 100;
		}
		// Proceed with the regular onToolClick of the ToolCompat Class, because it has important Code in it.
		return ToolCompat.onToolClick(this, aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aWorld, aSide, aX, aY, aZ, aHitX, aHitY, aHitZ);
	}
	
	@Override
	public OreDictMaterialStack getMaterialAtSide(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		if (mHullMaterial != null) return mHullMaterial;
		OreDictMaterial aMaterial = getMetaMaterial(aWorld, aX, aY, aZ);
		return aMaterial == null ? null : OM.stack(mPrefix, aMaterial);
	}
	
	@Override
	public void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (aTileEntity == null && aWorld instanceof Level) aTileEntity = WD.te((Level)aWorld, aX, aY, aZ, createTileEntity((Level)aWorld, aX, aY, aZ, SIDE_ANY, aMetaData, null), F);
		if (aTileEntity instanceof PrefixBlockTileEntity) ((PrefixBlockTileEntity)aTileEntity).mMetaData = aMetaData;
		if (aWorld instanceof Level && ((Level)aWorld).isClientSide()) WD.update(aWorld, aX, aY, aZ);
	}
	
	@Override
	public short getExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getMetaDataValue(aWorld, aX, aY, aZ);
	}
	
	@Override
	public boolean removeMaterialFromSide(Level aWorld, int aX, int aY, int aZ, byte aSide, OreDictMaterialStack aMaterial) {
		OreDictMaterialStack tMaterial = getMaterialAtSide(aWorld, aX, aY, aZ, aSide);
		if (aMaterial.mMaterial == tMaterial.mMaterial && aMaterial.mAmount > 0 && aMaterial.mAmount <= tMaterial.mAmount) {
			ItemStack tStack = OM.dust(aMaterial.mMaterial, tMaterial.mAmount - aMaterial.mAmount);
			if (tStack != null) ST.drop(aWorld, aX+0.5, aY+0.5, aZ+0.5, tStack);
			WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
			return T;
		}
		return F;
	}
	
	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (aWorld.isClientSide() || checkGravity(aWorld, aX, aY, aZ)) return;
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		OreDictMaterial aMaterial = getMetaMaterial(aTileEntity);
		if (aMaterial != null) {
			if (mCanBurn && (mPrefix.contains(TD.Prefix.DUST_BASED) || (mCanExplode && aMaterial.contains(TD.Properties.EXPLOSIVE))) && aMaterial.contains(TD.Properties.FLAMMABLE) && WD.temperature(aWorld, aX, aY, aZ) > C + 100) {
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				try {ExplosionGT.explode(aWorld, null, aX+0.5, aY+0.5, aZ+0.5, (aMaterial.contains(TD.Properties.EXPLOSIVE)?(mPrefix.mAmount>0?mPrefix.mAmount:U)*0.5F:(mPrefix.mAmount>0?mPrefix.mAmount:U)*0.33F)/U, T, T);} catch(StackOverflowError e) {ERR.println("WARNING: StackOverflow during Explosion has been prevented at: " + aX +" ; "+ aY +" ; "+ aZ);}
				return;
			}
			if ((mCanBurn || mCanExplode) && aMaterial.contains(TD.Atomic.ALKALI_METAL)) {
				boolean tExplode = F;
				for (byte tSide : ALL_SIDES_VALID) {
					Block tBlock = WD.block(aWorld, aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]);
					if (tBlock == Blocks.water || tBlock == Blocks.flowing_water) {
						WD.set(aWorld, aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide], NB, 0, 3);
						tExplode = T;
					}
				}
				if (tExplode) {
					WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
					try {ExplosionGT.explode(aWorld, null, aX+0.5, aY+0.5, aZ+0.5, (aMaterial.contains(TD.Properties.EXPLOSIVE)?(mPrefix.mAmount>0?mPrefix.mAmount:U)*0.5F:(mPrefix.mAmount>0?mPrefix.mAmount:U)*0.33F)/U, T, T);} catch(StackOverflowError e) {ERR.println("WARNING: StackOverflow during Explosion has been prevented at: " + aX +" ; "+ aY +" ; "+ aZ);}
					return;
				}
			}
		}
	}
	
	// @Override
	public void dropBlockAsItemWithChance(Level aWorld, int aX, int aY, int aZ, int aMeta, float aChance, int aFortune) {
		ArrayList<ItemStack> tList = mDrops.getDrops(this, aWorld, aX, aY, aZ, aFortune, F);
		aChance = EventHooks.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, 0, aFortune, aChance, F, harvesters.get());
		for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) dropBlockAsItem(aWorld, aX, aY, aZ, tStack);
	}
	
	// @Override
	public void harvestBlock(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {
		aPlayer.addStat(Stats.mineBlockStatArray[getIdFromBlock(this)], 1);
		UT.Entities.exhaust(aPlayer, 0.025F);
		boolean aSilkTouch = EnchantmentHelper.getSilkTouchModifier(aPlayer);
		int aFortune = EnchantmentHelper.getFortuneModifier(aPlayer);
		ArrayList<ItemStack> tList = mDrops.getDrops(this, aWorld, aX, aY, aZ, aFortune, aSilkTouch);
		float aChance = EventHooks.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, 0, aFortune, 1.0F, aSilkTouch, aPlayer);
		for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) dropBlockAsItem(aWorld, aX, aY, aZ, tStack);
	}
	
	public final ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aUnusableMetaData, int aFortune) {return mDrops.getDrops(this, aWorld, aX, aY, aZ, aFortune, F);}
	public int getExpDrop(BlockGetter aWorld, int aMeta, int aFortune) {return mDrops.getExp(this);}
	public int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	public void getSubBlocks(Item aItem, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {aItem.getSubItems(aItem, aCreativeTab, aList);}
	/** Where I come from, we set the TileEntities ourselves instead of letting a Handler do it. */
	public final BlockEntity createNewTileEntity(Level aWorld, int aMeta) {return null;}
	/** Where I come from, we set the TileEntities ourselves instead of letting a Handler do it. */
	public final BlockEntity createTileEntity(Level aWorld, int aMeta) {return null;}
	@Override public String toString() {return mNameInternal;}
	public String getUnlocalizedName() {return mNameInternal;}
	public String getLocalizedName() {return I18n.translateToLocal(mNameInternal);}
	public String getHarvestTool(int aMaterialToolQuality) {return mTool;}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return AABB.getBoundingBox(aX + mMinX, aY + mMinY, aZ + mMinZ, aX + mMaxX, aY + mMaxY, aZ + mMaxZ);}
	public AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return AABB.getBoundingBox(aX + mMinX, aY + mMinY, aZ + mMinZ, aX + mMaxX, aY + mMaxY, aZ + mMaxZ);}
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {setBlockBounds(mMinX, mMinY, mMinZ, mMaxX, mMaxY, mMaxZ);}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return mBaseHardness < 0 ? -1 : mBaseHardness == 0 ? 0 : Math.max(1, mBaseHardness * (1+getHarvestLevel(WD.meta(aWorld, aX, aY, aZ))));}
	public int getRenderType() {return RendererBlockTextured.INSTANCE==null?super.getRenderType():RendererBlockTextured.INSTANCE.mRenderID;}
	public int getHarvestLevel(int aMaterialToolQuality) {return (int)UT.Code.bind_(mHarvestLevelMinimum, mHarvestLevelMaximum, mHarvestLevelOffset + aMaterialToolQuality);}
	public int tickRate(Level aWorld) {return 2;}
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return getRenderColor(getMetaDataValue(aWorld, aX, aY, aZ));}
	public int getLightOpacity() {return mOpaque?255:0;}
	public boolean isBeaconBase(BlockGetter aWorld, int aX, int aY, int aZ, int aBeaconX, int aBeaconY, int aBeaconZ) {return mBeaconBase;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return mOpaque;}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return mNormalCube;}
	public boolean hasTileEntity(int aMeta) {return T;}
	public boolean renderAsNormalBlock() {return T;}
	public final boolean isOpaqueCube() {return mOpaque;}
	public boolean canSilkHarvest() {return F;}
	public boolean func_149730_j() {return mOpaque;}
	public boolean canCreatureSpawn(MobCategory aType, BlockGetter aWorld, int aX, int aY, int aZ) {return !mSpawnProof;}
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {setBlockBoundsBasedOnState(aWorld, aX, aY, aZ); return super.shouldSideBeRendered(aWorld, aX, aY, aZ, aSide);}
	@Override public boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return T;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return T;}
	@Override public Block getBlock() {return this;}
	
	public PrefixBlock setHidden(boolean aHidden) {mHidden = aHidden; return this;}
	
	/** @return the Local Name for this Block depending on Prefix and Material. */
	public String getLocalName(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		return LanguageHandler.getLocalName(aPrefix, aMaterial);
	}
	
	public short getMetaDataValue(BlockEntity aTileEntity) {
		return aTileEntity instanceof PrefixBlockTileEntity?((PrefixBlockTileEntity)aTileEntity).mMetaData:0;
	}
	
	public short getMetaDataValue(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getMetaDataValue(WD.te(aWorld, aX, aY, aZ, T));
	}
	
	public OreDictMaterial getMetaMaterial(int aMetaData) {
		return UT.Code.exists(aMetaData, mMaterialList)?mMaterialList[aMetaData]:null;
	}
	
	public OreDictMaterial getMetaMaterial(BlockEntity aTileEntity) {
		return getMetaMaterial(aTileEntity instanceof PrefixBlockTileEntity?((PrefixBlockTileEntity)aTileEntity).mMetaData:0);
	}
	
	public OreDictMaterial getMetaMaterial(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getMetaMaterial(WD.te(aWorld, aX, aY, aZ, T));
	}
	
	public BlockEntity createTileEntity(Level aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT) {
		PrefixBlockTileEntity rTileEntity = new PrefixBlockTileEntity();
		if (aNBT != null) rTileEntity.readFromNBT(aNBT);
		rTileEntity.mMetaData = aMetaData;
		rTileEntity.mItemNBT = aNBT == null ? null : aNBT.contains("gt.nbt.drop") ? aNBT.getCompoundTag("gt.nbt.drop") : aNBT;
		return rTileEntity;
	}
	
	protected boolean checkGravity(Level aWorld, int aX, int aY, int aZ) {
		if (mGravity && aY > 0 && WD.te(aWorld, aX, aY, aZ, T) != null && FallingBlock.func_149831_e(aWorld, aX, aY - 1, aZ)) {
			if (!FallingBlock.fallInstantly && aWorld.checkChunksExist(aX-32, aY-32, aZ-32, aX+32, aY+32, aZ+32)) {
				if (!aWorld.isClientSide()) aWorld.spawnEntityInWorld(new PrefixBlockFallingEntity(aWorld, aX+0.5, aY+0.5, aZ+0.5, this, getItemStackFromBlock(aWorld, aX, aY, aZ, SIDE_UP)));
			} else {
				short tMetaData = getMetaDataValue(aWorld, aX, aY, aZ);
				if (tMetaData > 0) {
					WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
					while (FallingBlock.func_149831_e(aWorld, aX, aY-1, aZ) && aY > 0) --aY;
					if (aY > 0) placeBlock(aWorld, aX, aY, aZ, SIDE_UP, tMetaData, null, F, T);
				}
			}
			return T;
		}
		return F;
	}
	
	@Override public void receiveDataByte     (BlockGetter aWorld, int aX, int aY, int aZ, byte   aData, INetworkHandler aNetworkHandler) {/**/}
	@Override public void receiveDataShort    (BlockGetter aWorld, int aX, int aY, int aZ, short  aData, INetworkHandler aNetworkHandler) {setExtendedMetaData(aWorld, aX, aY, aZ, aData);}
	@Override public void receiveDataInteger  (BlockGetter aWorld, int aX, int aY, int aZ, int    aData, INetworkHandler aNetworkHandler) {/**/}
	@Override public void receiveDataLong     (BlockGetter aWorld, int aX, int aY, int aZ, long   aData, INetworkHandler aNetworkHandler) {/**/}
	@Override public void receiveDataByteArray(BlockGetter aWorld, int aX, int aY, int aZ, byte[] aData, INetworkHandler aNetworkHandler) {/**/}
	@Override public void receiveDataName     (BlockGetter aWorld, int aX, int aY, int aZ, String aData, INetworkHandler aNetworkHandler) {if (UT.Code.stringValid(aData)) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof PrefixBlockTileEntity) {if (((PrefixBlockTileEntity)aTileEntity).mItemNBT == null) ((PrefixBlockTileEntity)aTileEntity).mItemNBT = UT.NBT.make(); ((PrefixBlockTileEntity)aTileEntity).mItemNBT.put("display", UT.NBT.makeString(((PrefixBlockTileEntity)aTileEntity).mItemNBT.getCompoundTag("display"), "Name", aData));}}}
}
