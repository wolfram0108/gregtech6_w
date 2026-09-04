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

package gregapi.block.prefixblock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;

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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
	/** F9 4-bis: same approach as in {@code BlockBaseRail}/{@code BlockBaseFlower}/{@code MultiTileEntityBlock} —
	 *  a dedicated field replacing the removed vanilla {@code Block.blockMaterial}, not a new abstraction. In 1.7.10
	 *  the material arrived via {@code super(aVanillaMaterial)} (original {@code PrefixBlock.java:168}) and took part in
	 *  block rules; here it is needed for the "normal cube" trait ({@link #isBlockNormalCube}). */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}
	/** F-bounds (same approach as BlockBase.java/MultiTileEntityBlock.java): the last-set bounds (via
	 *  setBlockBoundsBasedOnState -> setBlockBounds), neo bounds are immutable -> store them ourselves separately from mMinX..mMaxZ
	 *  (those are final, intrinsic material geometry), render usage is deferred to the F3 client pass. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	@Override public float[] getRenderBounds() {return mRenderBounds;}
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
	// F13/F16/F16: Properties at ctor — sound(step sound) + noOcclusion for non-opaque (otherwise renders solid + blocks light). setId is mandatory.
	private static net.minecraft.world.level.block.state.BlockBehaviour.Properties mkProps(String aModIDOwner, String aNameInternal, SoundType aSoundType, boolean aOpaque, String aTool, Material aVanillaMaterial) {
		net.minecraft.world.level.block.state.BlockBehaviour.Properties p = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().sound(aSoundType).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(aModIDOwner, gregapi.GT_API.sanitizeRegName(aNameInternal))))
			// Fix #1 (BUG-106): previously piston immovability relied on the fact "the block has a BE" (the engine does not
			// push blocks with a block entity); with the material moved into the chunk map there is no entity — declare the ban
			// explicitly, behavior 1:1 with before (and with 1.7.10, where TE blocks were unpushable).
			.pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK);
		if (!aOpaque) p = p.noOcclusion();
		// F-harvest-tool (1:1 GT6, mirrors MultiTileEntityBlock.mkProps): the gate "is a tool required for the drop" is decided by the
		// MATERIAL (1.7.10 EntityPlayer.canHarvestBlock → Material.isToolNotRequired), not the tool string: ores on
		// Material.rock (requires) → pickaxe only; soft prefix blocks (materials without setRequiresTool) — hand drops them too.
		if (aTool != null && !aTool.isEmpty() && aVanillaMaterial != null && !aVanillaMaterial.isToolNotRequired()) p = p.requiresCorrectToolForDrops();
		// MODCOMPAT-002: map color — the same 1.7.10 default "from the material", see BlockBase.mapColorOf.
		p = gregapi.block.BlockBase.mapColorOf(p, aVanillaMaterial);
		return p;
	}
	public PrefixBlock(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, OreDictMaterialStack aHullMaterial, Class<? extends PrefixBlockItem> aItemClass, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ, boolean aGravity, boolean aBeaconBase, boolean aEnderDragonProof, boolean aWitherProof, boolean aOpaque, boolean aNormalCube, boolean aPlacementChecksTemperature, boolean aPlacementChecksAntimatter, boolean aCanBurn, boolean aCanExplode, boolean aRenderOverlayInWorld, boolean aCanGlow, boolean aCanLight, boolean aSpawnProof, OreDictMaterial... aMaterialList) {
		// F12-followup (block-split): setId in Properties (neo Block.<init> requires an ID). F16: sound(aSoundType) (step sound).
		// F13/F16: opaque/lightOpacity — 1.7.10 runtime fields removed; neo occlusion/light comes from Properties → non-opaque blocks
		// get .noOcclusion() at ctor (otherwise they render solid + block light). aOpaque — ctor param. mkProps below.
		super(mkProps(aModIDOwner, aNameInternal, aSoundType, aOpaque, aTool, aVanillaMaterial));
		mPrefix = aPrefix;
		mMaterial = aVanillaMaterial;
		mNameInternal = aNameInternal;
		mMaterialList = (aMaterialList.length > 0 ? aMaterialList : OreDictMaterial.MATERIAL_ARRAY);
		if (mMaterialList[0] != MT.Empty) throw new IllegalArgumentException("The first element of the custom Material List has to be MT.Empty for technical reasons!");
		
		mMinX = (float)aMinX; mMinY = (float)aMinY; mMinZ = (float)aMinZ; mMaxX = (float)aMaxX; mMaxY = (float)aMaxY; mMaxZ = (float)aMaxZ;
		
		// F16: setStepSound IS WIRED UP — the sound is set in mkProps above (.sound(aSoundType) at ctor). Not a stub.
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
		
		// F13/F16: opaque IS WIRED UP in Properties at ctor (mkProps above: non-opaque → .noOcclusion() → neo render/light
		// are correct). Our own isOpaqueCube()/getLightOpacity() read mOpaque for GT6-internal logic. Not a stub.

		// F12-followup (block-split): the block registers via registerBlockLazy at the call site; HERE (on RegisterEvent<Block>, ITEMS is still
		// open) we register ONLY the BlockItem via a supplier (same approach as the item-split). It used to be: ST.register(this,...) — that
		// registered both the block (eager→freeze) and the BlockItem.
		final Class<? extends PrefixBlockItem> tItemClass = aItemClass==null?PrefixBlockItem.class:aItemClass;
		gregapi.GT_API.registerItemLazy(aModIDOwner, mNameInternal, () -> (net.minecraft.world.item.BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, T, this));
		
		// F12-followup (block-split, hashCode stability): same as in PrefixItem — id_(BlockItem) before registration = -1 →
		// the entry lands in a "dead" bucket, dedup does not find it. Defer the add to server-start (id_ is stable) → wildcard dedup collapses it.
		gregapi.GT_API.deferItemInit(() -> mPrefix.mRegisteredItems.add(this)); // this optimizes some processes by decreasing the size of the Set.

		if (mPrefix.contains(TD.Prefix.ORE)) {
			if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(this, 1, W)));
			if (COMPAT_IC2 != null && mBaseHardness >= 0) {
				for (byte i = 0; i < 16; i++) COMPAT_IC2.valuable(this, i, 3);
			}
		} else if (mPrefix.containsAny(TD.Prefix.DUST_BASED, TD.Prefix.INGOT_BASED, TD.Prefix.GEM_BASED)) {
			if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("miner", ST.make(this, 1, W)));
		} else {
			if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("builder", ST.make(this, 1, W)));
		}
		
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (MD.Mek.mLoaded) try {MekanismAPI.addBoxBlacklist(this, W);} catch(Throwable e) {e.printStackTrace(ERR);}
		
		if (mOpaque) VISUALLY_OPAQUE_BLOCKS.add(this);
		mDrops = aDrops==null?new Drops(this, this, this, this, F, F, 0, 0):aDrops;
		
		// F3 superseded-render (GT6BlockModel/ItemModel pipeline; the old getIcon/immediate-mode is dead, 0 neo calls): used to be MinecraftForgeClient.registerItemRenderer(...) (net.minecraftforge.client
		// was removed entirely in 26.1.2, RendererBlockTextured no longer implements IItemRenderer — decisions/F3-render.md §2.1/§3
		// "IItemRenderer"). The real item-model registration is RegisterBlockStateModels/ModelEvent.RegisterStandalone (Phase C).
		
		// Execute before all the other things. This is to ensure that PrefixBlocks are created before MultiItems.
		(GAPI.mBeforeInit==null?GAPI.mBeforePostInit:GAPI.mBeforeInit).add(0, this);
	}
	
	/** This ensures, that all Materials are registered at the time this Item registers to the OreDictionary. */
	// F12-followup (block-split): the body does ST.make/OreDict registration (Holder.components are only bound at server-start) →
	// deferred to runDeferredItemInit (same approach as PrefixItem.run). run() is called on @Init (mBeforeInit) → the defer is added
	// before the postInit deferrals; the registerOre_ guard "Only @Init/@PreInit" is suppressed in this window (see GT_API.sDeferredItemInitRunning).
	@Override
	public void run() {gregapi.GT_API.deferItemInit(this::runDeferred);}
	private void runDeferred() {
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
	public void registerBlockIcons(Object aIconRegister) {/*
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
	public Identifier getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		return getIcon(aSide, getMetaDataValue(aWorld, aX, aY, aZ));
	}

	// @Override
	public Identifier getIcon(int aSide, int aMetaData) {
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
		// F3-render (tint): super.getRenderColor(int) was removed from neo (block-color is data-driven, registered
		// client-side in a separate phase). Default when there's no material = 0xFFFFFF (white, no tint — exactly
		// the previous Block.getRenderColor default from 1.7.10). Material RGB is preserved.
		return aMaterial == null ? 0xFFFFFF : UT.Code.getRGBInt(aMaterial.fRGBa[mPrefix.mState]);
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
	
	// was onNeighborChange(IBlockAccess,x,y,z,tileX,Y,Z) -> IBlockExtension.onNeighborChange(BlockState,LevelReader,BlockPos,BlockPos) [IBlockExtension.java:534]
	@Override
	public void onNeighborChange(BlockState aState, LevelReader aWorld, BlockPos aPos, BlockPos aNeighbor) {
		if (!LOCK) {
			LOCK = T;
			BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aNeighbor.getX(), aNeighbor.getY(), aNeighbor.getZ());
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
	// F-neighbor (channel shifted): 1.7.10 World.notifyBlocksOfNeighborChange called Block.onNeighborBlockChange; the neo entry point is
	// BlockBehaviour.neighborChanged. Bridge modeled on BlockFluidBaseGT:154.
	@Override protected void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, net.minecraft.world.level.redstone.Orientation aOrientation, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}
	
	public boolean scheduleUpdateIfNeeded(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, BlockEntity aTileEntity) {
		if (mGravity && aY > WD.minY(aWorld) && FallingBlock.isFree(WD.block(aWorld, aX, aY - 1, aZ).defaultBlockState())) { // BUG-089: was aY > 0, MC26 floor = getMinY()
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2);
			return T;
		}
		if (!mCanBurn && !mCanExplode) return F;
		// Fix #1: material — from the passed entity (legacy/NBT) or from the funnel (chunk map); the previous gate
		// "TE == null → exit" meant "no data at this position" — now it is "material == null".
		OreDictMaterial aMaterial = aTileEntity != null ? getMetaMaterial(aTileEntity) : getMetaMaterial((BlockGetter)aWorld, aX, aY, aZ);
		// null guard (acceptance crash 2026-07-30): freshly loaded chunk data can arrive later than the block
		// (cascade from a PrefixBlockFallingEntity landing) → no material by meta. The original at :385 had no guard —
		// this window did not exist in 1.7.10; ALL neighboring branches in the file (:463, :472, :663) are guarded the same way.
		if (aMaterial == null) return F;
		if (mPrefix.contains(TD.Prefix.DUST_BASED)) {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2);
			return T;
		}
		if (aMaterial.containsAny(TD.Properties.FLAMMABLE, TD.Properties.EXPLOSIVE, TD.Atomic.ALKALI_METAL)) {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2);
			return T;
		}
		return F;
	}
	
	// BUG-024 (BUG-020 catch): the 1.7.10 hook below was dead — LAST_BROKEN was never set on explosion, the block was removed by neo's default
	// (setBlock air + wasExploded), the chain detonation of EXPLOSIVE/FLAMMABLE ores never triggered. Bridge using the same approach as
	// MultiTileEntityBlock:502. Vanilla order (BlockBehaviour.onExplosionHit:173-193): drops via the loot channel BEFORE this
	// hook (the BE is still alive), then removal here. The GT6 version did not call super (1.7.10 onBlockDestroyedByExplosion) — 1:1.
	@Override public void onBlockExploded(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, Explosion aExplosion) {
		onBlockExploded(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aExplosion);
	}
	// @Override
	public void onBlockExploded(Level aWorld, int aX, int aY, int aZ, Explosion aExplosion) {
		if (aWorld.isClientSide()) return;
		BlockEntity aTileEntity = teOrCarrier(aWorld, aX, aY, aZ); // fix #1: no entity — a carrier with the material from the map
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		OreDictMaterial aMaterial = getMetaMaterial(aTileEntity);
		WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
		if (aMaterial != null && ((mCanExplode && aMaterial.contains(TD.Properties.EXPLOSIVE)) || (mCanBurn && aMaterial.contains(TD.Properties.FLAMMABLE) && mPrefix.contains(TD.Prefix.DUST_BASED)))) try {ExplosionGT.explode(aWorld, null, aX+0.5, aY+0.5, aZ+0.5, ((mPrefix.mAmount>0?mPrefix.mAmount:U)*0.7F)/U, T, T);} catch(StackOverflowError e) {ERR.println("WARNING: StackOverflow during Explosion has been prevented at: " + aX +" ; "+ aY +" ; "+ aZ);}
	}
	
	// was getExplosionResistance(Entity,World,x,y,z,expX,expY,expZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]; explosionX/Y/Z were unused
	// by the original body (only a material check by position) - carried over directly without loss.
	@Override
	public float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {
		OreDictMaterial aMaterial = getMetaMaterial(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
		if (aMaterial != null && ((mCanExplode && aMaterial.contains(TD.Properties.EXPLOSIVE)) || (mCanBurn && aMaterial.contains(TD.Properties.FLAMMABLE) && mPrefix.contains(TD.Prefix.DUST_BASED)))) return 0;
		// BUG-020: in 1.7.10 the formula read getBlockMetadata = bind4(the material's mToolQuality) (placement :435 put exactly
		// that into the chunk meta). In the port there is no numeric meta, and WD.meta gives bind4(material ID) = garbage → quality is taken
		// from the material directly (the chunk meta was its pure derivative — 1:1 by value).
		return mBaseResistance * (1+getHarvestLevel(aMaterial == null ? 0 : UT.Code.bind4(aMaterial.mToolQuality)));
	}
	
	// was onBlockEventReceived(World,x,y,z,id,data) -> BlockBehaviour.triggerEvent(BlockState,Level,BlockPos,int,int)
	// [BlockBehaviour.java:206]; TileEntity.receiveClientEvent(id,data) -> BlockEntity.triggerEvent(int,int) [BlockEntity.java:270]
	@Override
	protected boolean triggerEvent(BlockState aState, Level aWorld, BlockPos aPos, int aID, int aParam) {
		BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		return aTileEntity == null || aTileEntity.triggerEvent(aID, aParam);
	}
	
	// ⚠️ CHANNEL IS REDUNDANT — in 1.7.10 getDamageValue answered "which subtype does this block's item have" and was called from
	// getPickBlock/createStackedBlock. In neo this role is carried entirely by getCloneItemStack (below, line 494):
	// it returns a ready stack with meta via getItemStackFromBlock. Kept as a comparison point with the original.
	// @Override
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {
		return getMetaDataValue(aWorld, aX, aY, aZ);
	}
	
	// F13: 1.7.10 Block.getPickBlock was removed — neo middle-click goes via IBlockExtension.getCloneItemStack; the neo hook below
	// delegates to the GT6 getPickBlock (getItemStackFromBlock), restoring the behavior 1:1. The GT6 method is kept.
	@Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, boolean aIncludeData, Player aPlayer) {
		ItemStack r = getItemStackFromBlock(aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), SIDE_UNKNOWN);
		return ST.valid(r) ? r : super.getCloneItemStack(aLevel, aPos, aState, aIncludeData, aPlayer);
	}
	public ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {
		return getItemStackFromBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN);
	}

	// ⚠️ CHANNEL IS REDUNDANT — analyzed during BUG-020 (analysis below, lines 508-510): the role "remember the
	// BlockEntity being removed before it's removed" is carried by the onDestroyedByPlayer bridge (line 511), set up with the same approach
	// as MultiTileEntityBlock.onDestroyedByPlayer:433. Kept as a comparison point with the original.
	// @Override
	public void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int par6) {
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity != null) LAST_BROKEN_TILEENTITY.set(tTileEntity);
		aWorld.removeBlockEntity(new BlockPos(aX, aY, aZ)); // was aWorld.removeTileEntity(x,y,z) (1.7.10 World), neo Level.removeBlockEntity(BlockPos) [Level.java:688]
	}
	// BUG-020 (ore drop): breakBlock above is a dead 1.7.10 hook (nobody calls it) → LAST_BROKEN_TILEENTITY was never set →
	// Drops.getDrops (:67 WD.te) at the loot stage (the BE is already removed by the engine) could not find the material. Bridge using the same approach as
	// MultiTileEntityBlock.onDestroyedByPlayer:433 — LAST_BROKEN is set BEFORE the block is removed, end-of-tick cleans it up (Proxy:911).
	@Override public boolean onDestroyedByPlayer(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer, ItemStack aToolStack, boolean aWillHarvest, net.minecraft.world.level.material.FluidState aFluid) {
		BlockEntity aTileEntity = teOrCarrier(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()); // fix #1: carrier from the map
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		return super.onDestroyedByPlayer(aState, aWorld, aPos, aPlayer, aToolStack, aWillHarvest, aFluid);
	}
	
	@Override
	public boolean placeBlock(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		OreDictMaterial aMaterial = getMetaMaterial(aMetaData);
		// F6-worldgen (flag): without aCauseBlockUpdates we place with UPDATE_KNOWN_SHAPE (16), so neo skips the neighbor shape update.
		// Otherwise setBlock during worldgen reads a neighboring (not-yet-generated) chunk → its synchronous generation → cascade
		// (tick >60s → watchdog crash). 1:1 with 1.7.10: worldgen placed with flag 2 (no neighbor-notify), UPDATE_KNOWN_SHAPE here is the equivalent.
		if (aMaterial != null && (aForcePlacement || ((!mPlacementChecksAntimatter || !aMaterial.contains(TD.Atomic.ANTIMATTER)) && (!mPlacementChecksTemperature || aMaterial.mMeltingPoint > WD.temperature(aWorld, aX, aY, aZ)))) && WD.set(aWorld, aX, aY, aZ, this, UT.Code.bind4(aMaterial.mToolQuality), aCauseBlockUpdates?3:net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE)) {
			// Fix #1 (BUG-106): material — into the chunk map (one entry instead of an entity per ore).
			// Gregorius's «This darn TileEntity update is ruining World generation Code» is solved here at the root:
			// writing to the map does not touch neighbors and does not wake up sync. The entity remains for the ONE
			// case that still needs it — a block with item NBT (mItemNBT, audit channel #8) — and is set manually, as the original did.
			setOreMeta(aWorld, aX, aY, aZ, aMetaData);
			if (aNBT != null) WD.te(aWorld, aX, aY, aZ, createTileEntity(aWorld, aX, aY, aZ, aSide, aMetaData, aNBT), aCauseBlockUpdates);
			scheduleUpdateIfNeeded(aWorld, aX, aY, aZ, null); // gravity/self-ignition on placement — as in the original
			return T;
		}
		return F;
	}
	
	@Override
	public ItemStack getItemStackFromBlock(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		// Fix #1: material — via the funnel (chunk map, entity fallback); NBT — from the rare entity, if present.
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		short tMeta = getMetaDataValue(aWorld, aX, aY, aZ);
		return ST.make(this, 1, tMeta, aTileEntity instanceof PrefixBlockTileEntity ? ((PrefixBlockTileEntity)aTileEntity).mItemNBT : null);
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

	// ===== WIRING 1.7.10 fire/destructibility channels to the engine =====================================
	// Same approach already adopted for the sibling MultiTileEntityBlock:526-535 — a direct delegate to the 1.7.10 method,
	// types match 1:1. For ores/crushed-ore the channels were declared (bodies 1:1 with the original PrefixBlock.java:453-476),
	// but had no callers: the engine asks them via IBlockExtension, not by 1.7.10 names. Without this,
	// flammability and dragon/wither protection were counted by vanilla defaults (the FireBlock table and the
	// DRAGON_IMMUNE tag), instead of by the block's MATERIAL, as intended in GT6.
	@Override public int getFlammability(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return getFlammability(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aSide);}
	@Override public int getFireSpreadSpeed(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return getFireSpreadSpeed(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aSide);}
	@Override public boolean canEntityDestroy(BlockState aState, BlockGetter aWorld, BlockPos aPos, Entity aEntity) {return canEntityDestroy(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aEntity);}
	// isFireSource: neo narrowed the type to LevelReader (IBlockExtension:736), while the 1.7.10 body wants a Level. The only
	// caller in the engine is FireBlock.tick(BlockState, ServerLevel, ...) (FireBlock.java:141,149), i.e. a ServerLevel
	// always arrives here; the cast is verified against the engine code, not assumed. The other case
	// (a LevelReader outside Level) does not occur in the engine — there we return the vanilla default, not a silent false.
	@Override public boolean isFireSource(BlockState aState, net.minecraft.world.level.LevelReader aWorld, BlockPos aPos, Direction aSide) {
		return aWorld instanceof Level tLevel ? isFireSource(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), aSide) : super.isFireSource(aState, aWorld, aPos, aSide);
	}

	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {
		OreDictMaterial aMaterial = getMetaMaterial(aWorld, aX, aY, aZ);
		if (!aWorld.isClientSide() && aTool.equals(TOOL_magnifyingglass)) {
			if (aChatReturn != null) aChatReturn.add(LH.tt("This is ") + getLocalName(mPrefix, aMaterial));
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
		// Fix #1: the write goes into the chunk map (write funnel); the legacy/NBT entity is updated ONLY in a live world
		// (texture cache reset, F3-render #2). Worldgen never has entities, and reading a BE from WorldGenRegion
		// prints an engine WARN on every call — a log flood of 100k+ lines (user report 2026-08-09).
		if (aWorld instanceof net.minecraft.world.level.LevelAccessor tAcc) setOreMeta(tAcc, aX, aY, aZ, aMetaData);
		if (aWorld instanceof Level) {
			BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
			if (aTileEntity instanceof PrefixBlockTileEntity) ((PrefixBlockTileEntity)aTileEntity).receiveMetaData(aMetaData);
			if (((Level)aWorld).isClientSide()) WD.update(aWorld, aX, aY, aZ);
		}
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
	
	// F12-tick (the same break and the same bridge already closed for the BlockBase family under BUG-005, BlockBase.java:368-373):
	// the neo scheduled-tick channel is tick(BlockState,ServerLevel,BlockPos,RandomSource); the 1.7.10 signature
	// updateTick overrides nothing (the "// @Override" below) and is not called by the engine. Without the bridge, three
	// scheduleUpdateIfNeeded branches (:423-438) hit a void: GRAVITY of meta-blocks, dust self-ignition, and
	// the explosion of flammable/alkali materials on heating. The RandomSource→java.util.Random converter is the UT.Code.random center.
	@Override protected void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {

		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), UT.Code.random(aRandom));
	}

	// Fix #1: clean up the map when the block is removed by any path (player/explosion/piston-impossible/WD.set) — otherwise
	// entries would pile up under other blocks. Drops are unaffected: by that point the material is already in the LAST_BROKEN carrier.
	@Override protected void affectNeighborsAfterRemoval(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, boolean aMovedByPiston) {
		setOreMeta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), (short)0);
		super.affectNeighborsAfterRemoval(aState, aWorld, aPos, aMovedByPiston);
	}

	// @Override
	public void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (aWorld.isClientSide() || checkGravity(aWorld, aX, aY, aZ)) return;
		OreDictMaterial aMaterial = getMetaMaterial(aWorld, aX, aY, aZ); // fix #1: via the funnel (map, entity fallback)
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
					if (tBlock == Blocks.WATER || tBlock == Blocks.WATER) {
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
		aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, 0, aFortune, aChance, F, LAST_HARVESTING_PLAYER.get());
		for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);
	}
	
	// ⚠️ CHANNEL IS REDUNDANT, no bridge NEEDED — analyzed by name (registry of dead channels, 2026-07-30).
	// The body's role is covered by three different paths, none of them lost:
	//  • drop — via getDrops(BlockState, LootParams.Builder) below (BUG-020), which also receives silk/fortune;
	//  • mining stats — the vanilla default Block.playerDestroy (Block.java:468) does the same awardStat;
    //  • exhaustion — same place (Block.java:469), and the value must NOT be taken from here: 0.025F in 1.7.10 was the VANILLA
    //    value (recompSrc Block.java:1195), GT6 simply repeated it, the mod had no rule of its own. Vanilla neo changed
    //    it to 0.005F, and 1:1 here means "like a vanilla block", not "the same number": otherwise GT6 ore would tire the player
    //    five times as much as stone, which was not the case in the original.
	// @Override
	public void harvestBlock(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {
		aPlayer.awardStat(Stats.BLOCK_MINED.get(this), 1); /* was Stats.mineBlockStatArray[getIdFromBlock(this)] (1.7.10 int-ID) -> Stats.BLOCK_MINED.get(Block) [Stats.java:12] + Player.awardStat [Player.java:1413] */
		UT.Entities.exhaust(aPlayer, 0.025F);
		// was EnchantmentHelper.getSilkTouchModifier(Player)/getFortuneModifier(Player) (1.7.10) - removed in neo;
		// the real neo way: EnchantmentHelper.getEnchantmentLevel(Holder<Enchantment>,LivingEntity) by a Holder from
		// RegistryAccess (verified, EnchantmentHelper.java:292 + Enchantments.SILK_TOUCH/FORTUNE), the same approach
		// already adopted and review-approved in GT_API_Proxy.onBlockHarvestingEvent (GT_API_Proxy.java:1450-1451)
		// and in MultiTileEntityBlock.harvestBlock (the same problem class).
		net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> tSilkTouchHolder = aWorld.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
		net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> tFortuneHolder = aWorld.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
		boolean aSilkTouch = EnchantmentHelper.getEnchantmentLevel(tSilkTouchHolder, aPlayer) > 0;
		int aFortune = EnchantmentHelper.getEnchantmentLevel(tFortuneHolder, aPlayer);
		ArrayList<ItemStack> tList = mDrops.getDrops(this, aWorld, aX, aY, aZ, aFortune, aSilkTouch);
		float aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, 0, aFortune, 1.0F, aSilkTouch, aPlayer);
		for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);
	}
	
	// BUG-020 (ore drop): the GT6 drop hooks above (harvestBlock/dropBlockAsItemWithChance/getDrops) are dead 1.7.10 names;
	// neo generates drops from a loot table, which PrefixBlock does not have → the drop was EMPTY (gt6oreprobe measurement). Bridge using the same approach
	// as BlockBase.getDrops:214 (neo getDrops(state,params) → GT6 mDrops), + silk/fortune from THIS_ENTITY — 1:1 semantics with
	// harvestBlock:648-650. The material survives via LAST_BROKEN_TILEENTITY (onDestroyedByPlayer above). dropResources further on
	// itself raises BlockDropsEvent → onBlockHarvestingEvent (unification/blockToSilk) — the 1.7.10 HarvestDropsEvent pipeline is intact.
	@Override protected java.util.List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		net.minecraft.server.level.ServerLevel tLevel = aParams.getLevel();
		net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
		if (tOrigin == null) return super.getDrops(aState, aParams);
		// BUG-024: the explosion-drop gate — CENTER WD.explosionDropDenied (1.7.10 Explosion.doExplosionB / ExplosionGT:175; consolidation, copies eradicated).
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList();
		int tX = net.minecraft.util.Mth.floor(tOrigin.x), tY = net.minecraft.util.Mth.floor(tOrigin.y), tZ = net.minecraft.util.Mth.floor(tOrigin.z);
		int tFortune = 0; boolean tSilkTouch = F;
		net.minecraft.world.entity.Entity tEntity = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
		if (tEntity instanceof net.minecraft.world.entity.LivingEntity tLiving) {
			tFortune = EnchantmentHelper.getEnchantmentLevel(tLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), tLiving);
			tSilkTouch = EnchantmentHelper.getEnchantmentLevel(tLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), tLiving) > 0;
		}
		ArrayList<ItemStack> rDrops = mDrops.getDrops(this, tLevel, tX, tY, tZ, tFortune, tSilkTouch);
		return rDrops == null ? java.util.Collections.emptyList() : rDrops;
	}
	public final ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aUnusableMetaData, int aFortune) {return mDrops.getDrops(this, aWorld, aX, aY, aZ, aFortune, F);}
	public int getExpDrop(BlockGetter aWorld, int aMeta, int aFortune) {return mDrops.getExp(this);}
	public int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	// F-creative: getSubItems is a GT6 item method (PrefixBlockItem:81), not a member of neo Item; a PrefixBlock's item is always
	// a PrefixBlockItem (ctor Class<? extends PrefixBlockItem>) — cast. GT6 calls getSubBlocks internally (BlockMetaType:200),
	// functionality preserved (the neo creative pipeline does not call it — that's a separate F-creative event phase).
	public void getSubBlocks(Item aItem, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {if (aItem instanceof PrefixBlockItem tItem) tItem.getSubItems(aItem, aCreativeTab, aList);}
	/** Where I come from, we set the TileEntities ourselves instead of letting a Handler do it. */
	public final BlockEntity createNewTileEntity(Level aWorld, int aMeta) {return null;}
	/** Where I come from, we set the TileEntities ourselves instead of letting a Handler do it. */
	public final BlockEntity createTileEntity(Level aWorld, int aMeta) {return null;}
	// F3-render ROOT CAUSE of "gray speck ore while loading": neo declares that a block has a BE THROUGH newBlockEntity (this is
	// the ONLY path in neo — unlike 1.7.10, where TEs were set manually). The previous null → neo did not create a BE on the CLIENT
	// for ore → the server-synced PrefixBlockTileEntity (mMetaData=material) was not retained (be=null for 203/203 ores,
	// GT6-ORE-PROBE) → material unavailable → getMetaMaterial=NULL → a gray speck with no color. We now return a fresh
	// PrefixBlockTileEntity (mMetaData is re-read from the sync readFromNBT/receiveMetaData; placeBlock still sets its own BE
	// with the material on the server). RE-APPLY 2026-07-17: safe after removing the server-tick worldgen (generation happens in Feature.place
	// on the region, there is no longer a reentrant getChunk of the current chunk — the former deadlock with ore BEs is fixed at the root).
	// Fix #1 (BUG-106): the material lives in the chunk map (PrefixBlockOreMap, synced by the standard AttachmentSync) —
	// the previous reason for the client entity ("material unavailable on render", RE-APPLY 2026-07-17 above) is removed at the root.
	// The block's EntityBlock nature is preserved: old entities from chunk NBT still load (type valid,
	// TileEntityBase01Root.createType) and migrate into the map (GT_API_Proxy.onChunkLoadMigrateOres), and blocks with
	// mItemNBT set their own entity manually (placeBlock) — "Where I come from, we set the TileEntities ourselves".
	// ⛔ returning null is FORBIDDEN (lived 2026-08-09..10): the neo contract "EntityBlock always creates" is strict —
	// worldgen writes a stub id="DUMMY" into the proto-chunk for EVERY block with hasBlockEntity (WorldGenRegion:276-281),
	// and the chunk's first full load calls this method (LevelChunk.promotePendingBlockEntity:612-614) and on null
	// prints WARN:627 for every ore — 343,394 lines in 3 minutes on an old world, ~1 million per run on a fresh one
	// (user report 2026-08-10). Returning an entity satisfies the stub, and the migration removes it in the SAME
	// load chain (ChunkStatusTasks:201-204: registerAllBlockEntitiesAfterLevelLoad → ChunkEvent.Load, one
	// thread, no reads in between) — a one-tick entity, no permanent objects appear on ores. Regular
	// entity reads do not spawn more: getBlockEntity goes through EntityCreationType.CHECK (read-only,
	// LevelChunk:372-395); IMMEDIATE is only called for entities from the chunk packet (:546-547) — regular ores
	// don't have any. The one-tick entity's material comes from the map (setLevel fallback in PrefixBlockTileEntity), "0 instead of ore" is impossible.
	@Override public final BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {return new PrefixBlockTileEntity(aPos, aState);}
	@Override public String toString() {return mNameInternal;}
	public String getUnlocalizedName() {return mNameInternal;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public String getHarvestTool(int aMaterialToolQuality) {return mTool;}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return new AABB(aX + mMinX, aY + mMinY, aZ + mMinZ, aX + mMaxX, aY + mMaxY, aZ + mMaxZ);}
	public AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return new AABB(aX + mMinX, aY + mMinY, aZ + mMinZ, aX + mMaxX, aY + mMaxY, aZ + mMaxZ);}
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {setBlockBounds(mMinX, mMinY, mMinZ, mMaxX, mMaxY, mMaxZ);}
	// F-shape (mirrors the BlockBase/MTE roots — a third Block root with no common ancestor): neo collision/outline from the same
	// static bounds mMin*..mMax* as the 1.7.10 channels above (:678-680). Bounds are final per-instance → position/world are not
	// needed, a single branch serves both the live world and the BlockState cache (EmptyBlockGetter: snow/isFaceSturdy). A full cube
	// (ores/blocks 0..1) → unchanged super; incomplete prefix shapes get real collision and an outline box.
	// Fix #4 (BUG-106): the mMin*..mMax* bounds are final per instance → the voxel shape is computed ONCE
	// (previously Shapes.create was allocated on every collision/outline request — ~2% of all allocations per JFR).
	private net.minecraft.world.phys.shapes.VoxelShape mShapeCache;
	private net.minecraft.world.phys.shapes.VoxelShape subCubeShape() {
		if (mShapeCache == null) mShapeCache = net.minecraft.world.phys.shapes.Shapes.create(new AABB(mMinX, mMinY, mMinZ, mMaxX, mMaxY, mMaxZ));
		return mShapeCache;
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (!hasCollision) return net.minecraft.world.phys.shapes.Shapes.empty();
		if (mMinX <= 0 && mMinY <= 0 && mMinZ <= 0 && mMaxX >= 1 && mMaxY >= 1 && mMaxZ >= 1) return super.getCollisionShape(aState, aWorld, aPos, aContext);
		return subCubeShape();
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (mMinX <= 0 && mMinY <= 0 && mMinZ <= 0 && mMaxX >= 1 && mMaxY >= 1 && mMaxZ >= 1) return super.getShape(aState, aWorld, aPos, aContext);
		net.minecraft.world.phys.shapes.VoxelShape rShape = subCubeShape();
		return rShape.isEmpty() ? net.minecraft.world.phys.shapes.Shapes.block() : rShape;
	}
	// F12/F9-hardness (BUG-020): in 1.7.10 getBlockHardness was an @Override of a real Forge hook — the engine called it itself.
	// In neo the channel shifted to getDestroyProgress (PrefixBlock's Properties.destroyTime is unset = 0 → the block broke
	// instantly, ore mBaseHardness played no part). Bridge using the same approach as BlockBase:248 (vanilla formula, 1:1).
	@Override protected float getDestroyProgress(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.entity.player.Player aPlayer, BlockGetter aWorld, BlockPos aPos) {
		if (!(aWorld instanceof Level tLevel)) return super.getDestroyProgress(aState, aPlayer, aWorld, aPos);
		return WD.destroyProgress(getBlockHardness(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()), aPlayer, aState, aWorld, aPos); // vanilla formula — CENTER WD.destroyProgress
	}
	// BUG-020 (second operand of the formula): 1.7.10 getBlockMetadata = bind4(material's mToolQuality) — see getExplosionResistance
	// above; quality from the TE's material (WD.te is backstopped internally by LAST_BROKEN_TILEENTITY → the harvest path after removeBlock stays alive too).
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {
		if (mBaseHardness < 0) return -1;
		if (mBaseHardness == 0) return 0;
		OreDictMaterial tMaterial = getMetaMaterial(aWorld, aX, aY, aZ);
		return Math.max(1, mBaseHardness * (1+getHarvestLevel(tMaterial == null ? 0 : UT.Code.bind4(tMaterial.mToolQuality))));
	}
	// F3-render (deferred phase): super.getRenderType() was removed from neo (render is data-driven) -> -1; see MultiTileEntityBlock:436.
	public int getRenderType() {return RendererBlockTextured.INSTANCE==null?-1:RendererBlockTextured.INSTANCE.mRenderID;}
	public int getHarvestLevel(int aMaterialToolQuality) {return (int)UT.Code.bind_(mHarvestLevelMinimum, mHarvestLevelMaximum, mHarvestLevelOffset + aMaterialToolQuality);}
	/** BUG-071: the value that 1.7.10 kept in THIS block's meta — {@code bind4(material.mToolQuality)}
	 *  (the original at :435 put it into meta on placement, the engine then called getHarvestLevel(meta)). In the port the prefix block's meta
	 *  is occupied by the material ID (PrefixBlockTileEntity.mMetaData), so we take the value from the material itself by position —
	 *  the result is the same as in 1.7.10, and there remains ONE level formula (the method above). Material from the BE: getMetaMaterial. */
	@Override public int getHarvestLevel(BlockGetter aWorld, int aX, int aY, int aZ) {
		OreDictMaterial tMaterial = getMetaMaterial(aWorld, aX, aY, aZ);
		return getHarvestLevel(tMaterial == null ? 0 : UT.Code.bind4(tMaterial.mToolQuality));
	}
	public int tickRate(Level aWorld) {return 2;}
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {return getRenderColor(getMetaDataValue(aWorld, aX, aY, aZ));}
	public int getLightOpacity() {return mOpaque?255:0;}

	// F3 light-opacity BRIDGE (the ore/crushed-ore hierarchy is separate from BlockBase, inherits Block directly):
	// the GT6 value is carried through to the engine's dampening channel, see the analysis in BlockBase. It is safe to read
	// the mOpaque field — initCache is called after block registration (neo-decompiled/.../Blocks.java:7221-7228).
	@Override protected int getLightDampening(net.minecraft.world.level.block.state.BlockState aState) {return gregapi.data.CS.lightDampening(getLightOpacity());}

	// F3 shade BRIDGE (the ore/crushed-ore hierarchy is separate from BlockBase, inherits Block directly; the channel analysis is
	// in BlockBase). In 1.7.10 this trait did not depend on the box size, so ore with a trimmed geometry
	// (mMinX..mMaxZ) shaded neighbors the same as a full cube, while the neo default-by-collision would no longer count it.
	@Override protected float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** 1.7.10 {@code Block.isBlockNormalCube()} ({@code Block.java:502-504}) — body 1:1, see {@code BlockBase}. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}
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
	// was shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], the semantics are INVERTED (shouldRender -> skipRendering) AND the new signature does not
	// pass World/BlockPos at all - impossible to call setBlockBoundsBasedOnState(aWorld,x,y,z) as before.
	// F3 functional-adapted (the neo skipRendering signature lost World/BlockPos → per-TE culling is unreachable; the vanilla default super.skipRendering is used, 1:1 in effect): the setBlockBoundsBasedOnState
	// side effect is unreachable without a position; we use the vanilla default (the same fallback as the old
	// super.shouldSideBeRendered branch, just under a new name/polarity).
	@Override protected boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {return super.skipRendering(aState, aNeighbor, aDir);}
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

	/** Fix #1 (BUG-106): READ-FUNNEL NECK. The material is taken from the chunk map (PrefixBlockOreMap);
	 *  the fallback to an entity is ONLY for chunks of old worlds not yet migrated (the entity stays alive until the first
	 *  ChunkEvent.Load) — after migration the branch is dead. All previous funnel callers are unchanged. */
	public short getMetaDataValue(BlockGetter aWorld, int aX, int aY, int aZ) {
		short rMeta = getOreMeta(aWorld, aX, aY, aZ);
		if (rMeta != 0) return rMeta;
		// Fallback to the entity — ONLY the live world (legacy of old worlds/NBT blocks). WorldGenRegion and other views
		// never have legacy entities by definition, and their getBlockEntity on a block "entitled to a BE but without one"
		// prints an engine WARN on every call — a log flood of 100k+ lines (user report 2026-08-09).
		if (!(aWorld instanceof Level)) return 0;
		return getMetaDataValue(WD.te(aWorld, aX, aY, aZ, T));
	}

	/** Reading the material map. Chunk resolution: worldgen region → the region's own chunk (ProtoChunk);
	 *  live world → WD.chunkNow (without loading, fix #2); client render view (RenderRegion et al. do not
	 *  carry a map) → the client world's chunk. */
	public static short getOreMeta(BlockGetter aWorld, int aX, int aY, int aZ) {
		net.minecraft.world.level.chunk.ChunkAccess tChunk = oreChunk(aWorld, aX, aZ);
		if (tChunk == null) return 0;
		PrefixBlockOreMap tMap = tChunk.getExistingDataOrNull(PrefixBlockOreMap.TYPE.get());
		return tMap == null ? 0 : tMap.get(aX, aY, aZ);
	}

	private static net.minecraft.world.level.chunk.ChunkAccess oreChunk(BlockGetter aWorld, int aX, int aZ) {
		if (aWorld instanceof Level tL) return WD.chunkNow(tL, aX >> 4, aZ >> 4);
		if (aWorld instanceof net.minecraft.world.level.WorldGenLevel tGen) return tGen.getChunk(aX >> 4, aZ >> 4);
		net.minecraft.world.entity.player.Player tPlayer = gregapi.GT_API.api_proxy == null ? null : gregapi.GT_API.api_proxy.getThePlayer();
		return tPlayer == null ? null : WD.chunkNow(tPlayer.level(), aX >> 4, aZ >> 4);
	}

	/** Fix #1: WRITE-FUNNEL NECK. Writes the material into the chunk map; in a live server world — marks it
	 *  for saving + the standard attachment sync to clients (AttachmentSync); on a worldgen ProtoChunk — just a
	 *  write (the map will travel into the LevelChunk on promotion and to the client when the chunk is sent). */
	public static void setOreMeta(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, short aMeta) {
		net.minecraft.world.level.chunk.ChunkAccess tChunk = oreChunk(aWorld, aX, aZ);
		if (tChunk == null) return;
		tChunk.getData(PrefixBlockOreMap.TYPE.get()).set(aX, aY, aZ, aMeta);
		tChunk.markUnsaved();
		// Tail of fix #1 (2026-08-10): once the material is written into the map, the entity/placeholder that the engine spawns on
		// setBlock ITSELF is removed right here — in the same write funnel. They are spawned by the contract "EntityBlock always
		// creates" (newBlockEntity must be non-null, see :788): the "worldgen into a live chunk" branch places a live
		// entity (WorldGenRegion:268-274 — the W judge caught 875 of them), the proto-chunk branch places a DUMMY placeholder (:276-281).
		// The item-NBT carrier is unaffected: it is set AFTER this write (placeBlock:548), and an already-standing one is
		// protected by the mItemNBT == null condition. On a proto-chunk, removeBlockEntity also removes a pending placeholder (:283-286).
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockEntity tBE = tChunk.getBlockEntity(tPos);
		if (tBE == null || (tBE instanceof PrefixBlockTileEntity tTE && tTE.mItemNBT == null)) tChunk.removeBlockEntity(tPos);
		if (tChunk instanceof net.minecraft.world.level.chunk.LevelChunk tLC && aWorld instanceof net.minecraft.server.level.ServerLevel tSL) markOreMapDirty(tSL, tLC);
	}

	// MAP-BROADCAST COALESCING (consolidation wave 3, item 1). The map packet carries the WHOLE chunk (AttachmentSync.syncUpdate
	// sends the full state to trackers RIGHT AWAY inside the call, with no queue of its own — neoforge-decompiled/.../AttachmentSync.java:84-109),
	// so N writes in a row to one chunk are N identical full broadcasts instead of one. We accumulate
	// dirty chunks and send once per server tick (GT_API_Proxy, Post phase) — the delivered state is the same
	// (we read the chunk's map at flush time, not a snapshot at write time), latency is less than a tick.
	private static final java.util.Map<net.minecraft.server.level.ServerLevel, java.util.Set<Long>> ORE_MAP_DIRTY = new java.util.concurrent.ConcurrentHashMap<>();

	/** Marks "this chunk's map has changed" — instead of an immediate broadcast (see {@link #flushOreMapSync}). */
	public static void markOreMapDirty(net.minecraft.server.level.ServerLevel aWorld, net.minecraft.world.level.chunk.LevelChunk aChunk) {
		ORE_MAP_DIRTY.computeIfAbsent(aWorld, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(aChunk.getPos().pack());
	}

	/** Diagnostic: count of actual chunk-map broadcasts to clients (batching judge gt6oremapprobe, S). */
	public static final java.util.concurrent.atomic.AtomicLong sOreMapSyncCalls = new java.util.concurrent.atomic.AtomicLong();

	/** Sends the accumulated batch — once per server tick (called by GT_API_Proxy.onServerTick, Post phase). We take the chunk
	 *  without loading it: an unloaded chunk has nobody to broadcast to, and the map will travel with the chunk on the next send. */
	public static void flushOreMapSync() {
		if (ORE_MAP_DIRTY.isEmpty()) return;
		for (java.util.Map.Entry<net.minecraft.server.level.ServerLevel, java.util.Set<Long>> tEntry : ORE_MAP_DIRTY.entrySet()) {
			net.minecraft.server.level.ServerLevel tLevel = tEntry.getKey();
			java.util.Iterator<Long> tIt = tEntry.getValue().iterator();
			while (tIt.hasNext()) {
				long tKey = tIt.next(); tIt.remove();
				net.minecraft.world.level.chunk.LevelChunk tChunk = tLevel.getChunkSource().getChunkNow(
					net.minecraft.world.level.ChunkPos.getX(tKey), net.minecraft.world.level.ChunkPos.getZ(tKey));
				if (tChunk != null) {tChunk.syncData(PrefixBlockOreMap.TYPE.get()); sOreMapSyncCalls.incrementAndGet();}
			}
		}
	}

	/** Reset between worlds: the map holds a reference to ServerLevel, and a stale level is a known defect class "second world
	 *  hangs" (see GT6WorldgenFeature: the same queues are cleared on ServerStopping/ServerStopped). */
	public static void clearOreMapSync() {ORE_MAP_DIRTY.clear();}

	/** Fix #1 (BUG-106): old-chunk MIGRATION — ore/rock entities are poured into the chunk map and
	 *  removed for good (the chunk is marked for saving — it will go to disk without them). Entities with mItemNBT
	 *  (audit channel #8) keep living, but the material is also duplicated into the map — the read funnel stays unified.
	 *  Called from GT_API_Proxy.onChunkLoadMigrateOres (ChunkEvent.Load, server) and directly by the stand. */
	public static void migrateChunkOres(net.minecraft.world.level.chunk.LevelChunk aChunk) {
		java.util.List<BlockPos> tLegacy = null;
		for (java.util.Map.Entry<BlockPos, BlockEntity> tEntry : aChunk.getBlockEntities().entrySet()) {
			if (!(tEntry.getValue() instanceof PrefixBlockTileEntity tTE)) continue;
			BlockPos tPos = tEntry.getKey();
			if (tTE.mMetaData > 0) aChunk.getData(PrefixBlockOreMap.TYPE.get()).set(tPos.getX(), tPos.getY(), tPos.getZ(), tTE.mMetaData);
			if (tTE.mItemNBT == null) {
				if (tLegacy == null) tLegacy = new java.util.ArrayList<>();
				tLegacy.add(tPos);
			}
		}
		if (tLegacy != null) {
			for (BlockPos tPos : tLegacy) aChunk.removeBlockEntity(tPos);
			aChunk.markUnsaved();
		}
	}

	/** Fix #1: LAST_BROKEN bridge for migrated blocks — there is no longer an entity at the position, but the drop
	 *  mechanism (Drops.getDrops via getMetaDataValue(TE)) reads "the last broken one". A one-shot carrier
	 *  with the material from the map, never placed into the world. */
	protected BlockEntity teOrCarrier(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockEntity rTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (rTileEntity != null) return rTileEntity;
		short tMeta = getOreMeta(aWorld, aX, aY, aZ);
		if (tMeta == 0) return null;
		PrefixBlockTileEntity rCarrier = new PrefixBlockTileEntity(new BlockPos(aX, aY, aZ), defaultBlockState());
		rCarrier.mMetaData = tMeta;
		return rCarrier;
	}

	public OreDictMaterial getMetaMaterial(int aMetaData) {
		return UT.Code.exists(aMetaData, mMaterialList)?mMaterialList[aMetaData]:null;
	}

	public OreDictMaterial getMetaMaterial(BlockEntity aTileEntity) {
		return getMetaMaterial(aTileEntity instanceof PrefixBlockTileEntity?((PrefixBlockTileEntity)aTileEntity).mMetaData:0);
	}
	
	public OreDictMaterial getMetaMaterial(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getMetaMaterial((int)getMetaDataValue(aWorld, aX, aY, aZ)); // fix #1: via the funnel (chunk map, entity fallback)
	}
	
	public BlockEntity createTileEntity(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT) {
		// pass the ore's blockstate into the TE (= defaultBlockState, as WD.set places it) → the TE caches the correct state right away,
		// without a "Block state mismatch … updating" on chunk load (ores were generating fine anyway, this was only cache noise).
		PrefixBlockTileEntity rTileEntity = new PrefixBlockTileEntity(new net.minecraft.core.BlockPos(aX, aY, aZ), defaultBlockState());
		if (aNBT != null) rTileEntity.readFromNBT(aNBT);
		rTileEntity.mMetaData = aMetaData;
		rTileEntity.mItemNBT = aNBT == null ? null : aNBT.contains("gt.nbt.drop") ? aNBT.getCompoundOrEmpty("gt.nbt.drop") : aNBT;
		return rTileEntity;
	}
	
	protected boolean checkGravity(Level aWorld, int aX, int aY, int aZ) {
		if (mGravity && aY > WD.minY(aWorld) && getMetaDataValue(aWorld, aX, aY, aZ) != 0 && FallingBlock.isFree(WD.block(aWorld, aX, aY - 1, aZ).defaultBlockState())) { // BUG-089: was aY > 0, MC26 floor = getMinY(); fix #1: "has an entity" → "has material in the funnel"
			// was BlockFalling.fallInstantly (1.7.10 static field, default false, not found in any of the 3 reference roots) ->
			// "T"; World.checkChunksExist(±32) -> ILevelReaderExtension.isAreaLoaded(BlockPos,int) [ILevelReaderExtension.java:19]
			// (the same approach as BlockBase.checkGravity/decisions/DEFERRED-LEDGER.md §B2).
			if (T && aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), 32)) {
				if (!aWorld.isClientSide()) aWorld.addFreshEntity(new PrefixBlockFallingEntity(aWorld, aX+0.5, aY+0.5, aZ+0.5, this, getItemStackFromBlock(aWorld, aX, aY, aZ, SIDE_UP)));
			} else {
				short tMetaData = getMetaDataValue(aWorld, aX, aY, aZ);
				if (tMetaData > 0) {
					WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
					while (FallingBlock.isFree(WD.block(aWorld, aX, aY-1, aZ).defaultBlockState()) && aY > 0) --aY;
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
	@Override public void receiveDataName     (BlockGetter aWorld, int aX, int aY, int aZ, String aData, INetworkHandler aNetworkHandler) {if (UT.Code.stringValid(aData)) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof PrefixBlockTileEntity) {if (((PrefixBlockTileEntity)aTileEntity).mItemNBT == null) ((PrefixBlockTileEntity)aTileEntity).mItemNBT = UT.NBT.make(); ((PrefixBlockTileEntity)aTileEntity).mItemNBT.put("display", UT.NBT.makeString(((PrefixBlockTileEntity)aTileEntity).mItemNBT.getCompoundOrEmpty("display"), "Name", aData));}}}
}
