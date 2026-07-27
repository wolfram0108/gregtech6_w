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
	/** F-bounds (тот же приём, что BlockBase.java/MultiTileEntityBlock.java): последние заданные bounds (через
	 *  setBlockBoundsBasedOnState -> setBlockBounds), neo bounds immutable -> храним сами отдельно от mMinX..mMaxZ
	 *  (те final, интринсик-геометрия материала), рендер-использование отложено на F3-клиент-проход. */
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
	// F13/F16/F16: Properties при ctor — sound(step-звук) + noOcclusion для non-opaque (иначе рендер solid + свет блокируется). setId обязателен.
	private static net.minecraft.world.level.block.state.BlockBehaviour.Properties mkProps(String aModIDOwner, String aNameInternal, SoundType aSoundType, boolean aOpaque, String aTool, Material aVanillaMaterial) {
		net.minecraft.world.level.block.state.BlockBehaviour.Properties p = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().sound(aSoundType).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(aModIDOwner, gregapi.GT_API.sanitizeRegName(aNameInternal))));
		if (!aOpaque) p = p.noOcclusion();
		// F-harvest-tool (1:1 GT6, зеркало MultiTileEntityBlock.mkProps): гейт «нужен ли инструмент для дропа» решает
		// МАТЕРИАЛ (1.7.10 EntityPlayer.canHarvestBlock → Material.isToolNotRequired), не строка инструмента: руды на
		// Material.rock (требует) → только кирка; мягкие prefix-блоки (материалы без setRequiresTool) — рука дропает /30.
		if (aTool != null && !aTool.isEmpty() && aVanillaMaterial != null && !aVanillaMaterial.isToolNotRequired()) p = p.requiresCorrectToolForDrops();
		// MODCOMPAT-002: цвет на карте — тот же 1.7.10-дефолт «из материала», см. BlockBase.mapColorOf.
		p = gregapi.block.BlockBase.mapColorOf(p, aVanillaMaterial);
		return p;
	}
	public PrefixBlock(String aModIDOwner, String aModIDTextures, String aNameInternal, OreDictPrefix aPrefix, OreDictMaterialStack aHullMaterial, Class<? extends PrefixBlockItem> aItemClass, Drops aDrops, ITexture aTexture, Material aVanillaMaterial, SoundType aSoundType, String aTool, float aBaseHardness, float aBaseResistance, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, double aMinX, double aMinY, double aMinZ, double aMaxX, double aMaxY, double aMaxZ, boolean aGravity, boolean aBeaconBase, boolean aEnderDragonProof, boolean aWitherProof, boolean aOpaque, boolean aNormalCube, boolean aPlacementChecksTemperature, boolean aPlacementChecksAntimatter, boolean aCanBurn, boolean aCanExplode, boolean aRenderOverlayInWorld, boolean aCanGlow, boolean aCanLight, boolean aSpawnProof, OreDictMaterial... aMaterialList) {
		// F12-followup (block-split): setId в Properties (neo Block.<init> требует ID). F16: sound(aSoundType) (step-звук).
		// F13/F16: opaque/lightOpacity — 1.7.10 runtime-поля удалены; neo occlusion/свет из Properties → non-opaque блоки
		// получают .noOcclusion() при ctor (иначе рендерятся solid + блокируют свет). aOpaque — ctor-param. mkProps ниже.
		super(mkProps(aModIDOwner, aNameInternal, aSoundType, aOpaque, aTool, aVanillaMaterial));
		mPrefix = aPrefix;
		mNameInternal = aNameInternal;
		mMaterialList = (aMaterialList.length > 0 ? aMaterialList : OreDictMaterial.MATERIAL_ARRAY);
		if (mMaterialList[0] != MT.Empty) throw new IllegalArgumentException("The first element of the custom Material List has to be MT.Empty for technical reasons!");
		
		mMinX = (float)aMinX; mMinY = (float)aMinY; mMinZ = (float)aMinZ; mMaxX = (float)aMaxX; mMaxY = (float)aMaxY; mMaxZ = (float)aMaxZ;
		
		// F16: setStepSound ПОДКЛЮЧЕН — звук выставлен в mkProps выше (.sound(aSoundType) при ctor). Не заглушка.
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
		
		// F13/F16: opaque ПОДКЛЮЧЕН в Properties при ctor (mkProps выше: non-opaque → .noOcclusion() → neo рендер/свет
		// корректны). Собственные isOpaqueCube()/getLightOpacity() читают mOpaque для GT6-внутренней логики. Не заглушка.
		
		// F12-followup (block-split): блок регистрирует registerBlockLazy на call-site; ЗДЕСЬ (на RegisterEvent<Block>, ITEMS ещё
		// открыт) регистрируем ТОЛЬКО BlockItem через supplier (тот же приём, что item-split). Было: ST.register(this,...) — оно
		// регистрировало и блок (эагер→freeze) и BlockItem.
		final Class<? extends PrefixBlockItem> tItemClass = aItemClass==null?PrefixBlockItem.class:aItemClass;
		gregapi.GT_API.registerItemLazy(aModIDOwner, mNameInternal, () -> (net.minecraft.world.item.BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, T, this));
		
		// F12-followup (block-split, hashCode-стабильность): как в PrefixItem — id_(BlockItem) до регистрации = -1 →
		// запись в «мёртвом» бакете, дедуп не находит. Откладываем add на server-start (id_ стабилен) → wildcard-дедуп схлопывает.
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
		
		// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было MinecraftForgeClient.registerItemRenderer(...) (net.minecraftforge.client
		// удалён целиком в 26.1.2, RendererBlockTextured больше не implements IItemRenderer — decisions/F3-render.md §2.1/§3
		// "IItemRenderer"). Реальная регистрация item-модели — RegisterBlockStateModels/ModelEvent.RegisterStandalone (Фаза C).
		
		// Execute before all the other things. This is to ensure that PrefixBlocks are created before MultiItems.
		(GAPI.mBeforeInit==null?GAPI.mBeforePostInit:GAPI.mBeforeInit).add(0, this);
	}
	
	/** This ensures, that all Materials are registered at the time this Item registers to the OreDictionary. */
	// F12-followup (block-split): тело делает ST.make/OreDict-регистрацию (Holder.components привязаны только на server-start) →
	// отложено в runDeferredItemInit (тот же приём, что PrefixItem.run). run() вызывается на @Init (mBeforeInit) → defer добавлен
	// до postInit-дефферов; guard registerOre_ «Only @Init/@PreInit» подавлён в окне (см. GT_API.sDeferredItemInitRunning).
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
		// F3-render (tint): super.getRenderColor(int) удалён из neo (block-color data-driven, регистрируется
		// клиентски отдельной фазой). Дефолт при отсутствии материала = 0xFFFFFF (белый, без тонирования — ровно
		// прежний Block.getRenderColor-дефолт 1.7.10). Материал-RGB сохранён.
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
	
	// было onNeighborChange(IBlockAccess,x,y,z,tileX,Y,Z) -> IBlockExtension.onNeighborChange(BlockState,LevelReader,BlockPos,BlockPos) [IBlockExtension.java:534]
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
	// F-neighbor (канал сместился): 1.7.10 World.notifyBlocksOfNeighborChange звал Block.onNeighborBlockChange; neo-вход —
	// BlockBehaviour.neighborChanged. Мост по образцу BlockFluidBaseGT:154.
	@Override protected void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, net.minecraft.world.level.redstone.Orientation aOrientation, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}
	
	public boolean scheduleUpdateIfNeeded(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, BlockEntity aTileEntity) {
		if (mGravity && aY > 0 && FallingBlock.isFree(WD.block(aWorld, aX, aY - 1, aZ).defaultBlockState())) {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2);
			return T;
		}
		if (aTileEntity == null) return F;
		if (!mCanBurn && !mCanExplode) return F;
		if (mPrefix.contains(TD.Prefix.DUST_BASED)) {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2);
			return T;
		}
		OreDictMaterial aMaterial = getMetaMaterial(aTileEntity);
		if (aMaterial.containsAny(TD.Properties.FLAMMABLE, TD.Properties.EXPLOSIVE, TD.Atomic.ALKALI_METAL)) {
			aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2);
			return T;
		}
		return F;
	}
	
	// BUG-024 (улов BUG-020): 1.7.10-хук ниже был мёртв — при взрыве LAST_BROKEN не ставился, блок сносил neo-дефолт
	// (setBlock air + wasExploded), цепная детонация EXPLOSIVE/FLAMMABLE-руд не срабатывала. Мост тем же приёмом, что
	// MultiTileEntityBlock:502. Порядок vanilla (BlockBehaviour.onExplosionHit:173-193): дропы через loot-канал ДО этого
	// хука (BE ещё жив), затем удаление здесь. GT6-версия не звала super (1.7.10 onBlockDestroyedByExplosion) — 1:1.
	@Override public void onBlockExploded(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, Explosion aExplosion) {
		onBlockExploded(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aExplosion);
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
	
	// было getExplosionResistance(Entity,World,x,y,z,expX,expY,expZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]; explosionX/Y/Z были не использованы
	// исходным телом (только material-проверка по позиции) - без потери переносится напрямую.
	@Override
	public float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {
		OreDictMaterial aMaterial = getMetaMaterial(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
		if (aMaterial != null && ((mCanExplode && aMaterial.contains(TD.Properties.EXPLOSIVE)) || (mCanBurn && aMaterial.contains(TD.Properties.FLAMMABLE) && mPrefix.contains(TD.Prefix.DUST_BASED)))) return 0;
		// BUG-020: в 1.7.10 формула читала getBlockMetadata = bind4(mToolQuality материала) (placement :435 клал именно
		// его в мету чанка). В порте числовой меты нет, а WD.meta даёт bind4(ID материала) = мусор → quality берётся из
		// материала напрямую (мета чанка была его чистой производной — 1:1 по значению).
		return mBaseResistance * (1+getHarvestLevel(aMaterial == null ? 0 : UT.Code.bind4(aMaterial.mToolQuality)));
	}
	
	// было onBlockEventReceived(World,x,y,z,id,data) -> BlockBehaviour.triggerEvent(BlockState,Level,BlockPos,int,int)
	// [BlockBehaviour.java:206]; TileEntity.receiveClientEvent(id,data) -> BlockEntity.triggerEvent(int,int) [BlockEntity.java:270]
	@Override
	protected boolean triggerEvent(BlockState aState, Level aWorld, BlockPos aPos, int aID, int aParam) {
		BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		return aTileEntity == null || aTileEntity.triggerEvent(aID, aParam);
	}
	
	// @Override
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {
		return getMetaDataValue(aWorld, aX, aY, aZ);
	}
	
	// F13: 1.7.10 Block.getPickBlock удалён — neo middle-click через IBlockExtension.getCloneItemStack; ниже neo-хук
	// делегирует в GT6-getPickBlock (getItemStackFromBlock), восстанавливая поведение 1:1. GT6-метод сохранён.
	@Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, boolean aIncludeData, Player aPlayer) {
		ItemStack r = getItemStackFromBlock(aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), SIDE_UNKNOWN);
		return ST.valid(r) ? r : super.getCloneItemStack(aLevel, aPos, aState, aIncludeData, aPlayer);
	}
	public ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {
		return getItemStackFromBlock(aWorld, aX, aY, aZ, SIDE_UNKNOWN);
	}

	// @Override
	public void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int par6) {
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity != null) LAST_BROKEN_TILEENTITY.set(tTileEntity);
		aWorld.removeBlockEntity(new BlockPos(aX, aY, aZ)); // было aWorld.removeTileEntity(x,y,z) (1.7.10 World), neo Level.removeBlockEntity(BlockPos) [Level.java:688]
	}
	// BUG-020 (дроп руды): breakBlock выше — мёртвый 1.7.10-хук (никто не зовёт) → LAST_BROKEN_TILEENTITY не ставился →
	// Drops.getDrops (:67 WD.te) на loot-этапе (BE уже снят движком) не находил материал. Мост тем же приёмом, что
	// MultiTileEntityBlock.onDestroyedByPlayer:433 — LAST_BROKEN ставится ДО снятия блока, тик-конец его чистит (Proxy:911).
	@Override public boolean onDestroyedByPlayer(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer, ItemStack aToolStack, boolean aWillHarvest, net.minecraft.world.level.material.FluidState aFluid) {
		BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		return super.onDestroyedByPlayer(aState, aWorld, aPos, aPlayer, aToolStack, aWillHarvest, aFluid);
	}
	
	@Override
	public boolean placeBlock(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT, boolean aCauseBlockUpdates, boolean aForcePlacement) {
		OreDictMaterial aMaterial = getMetaMaterial(aMetaData);
		// F6-worldgen (флаг): без aCauseBlockUpdates ставим с UPDATE_KNOWN_SHAPE (16), чтобы neo пропустил neighbor-shape-update.
		// Иначе setBlock во время ворлдгена читает соседний (ещё не сгенерированный) чанк → синхронная его генерация → каскад
		// (тик >60с → watchdog-краш). 1:1 с 1.7.10: ворлдген клал флагом 2 (без neighbor-notify), тут UPDATE_KNOWN_SHAPE — эквивалент.
		if (aMaterial != null && (aForcePlacement || ((!mPlacementChecksAntimatter || !aMaterial.contains(TD.Atomic.ANTIMATTER)) && (!mPlacementChecksTemperature || aMaterial.mMeltingPoint > WD.temperature(aWorld, aX, aY, aZ)))) && WD.set(aWorld, aX, aY, aZ, this, UT.Code.bind4(aMaterial.mToolQuality), aCauseBlockUpdates?3:net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE)) {
			// This darn TileEntity update is ruining World generation Code (infinite Loops when placing TileEntities on Chunk Borders). I'm glad I finally found a way to disable it.
			BlockEntity tTileEntity = createTileEntity(aWorld, aX, aY, aZ, aSide, aMetaData, aNBT);
			WD.te(aWorld, aX, aY, aZ, tTileEntity, aCauseBlockUpdates);
			scheduleUpdateIfNeeded(aWorld, aX, aY, aZ, tTileEntity);
			// F6-worldgen: планировать occlusion-апдейт ТОЛЬКО при размещении в настоящем Level (gameplay). Во время worldgen
			// aWorld=WorldGenLevel/WorldGenRegion — BE ставится на ProtoChunk, его level ещё НЕ привязан (привяжется при
			// ProtoChunk→LevelChunk). Прежний безусловный add клал 96741 руд-BE с level=null → onScheduledUpdate→visOcc(null)→NPE
			// на каждом server-tick. Синк материала руды клиенту в worldgen не нужен (нет игроков) и всё равно идёт через
			// getUpdateTag на chunk-load. 1:1 с духом Грегориуса «This darn TileEntity update is ruining World generation Code».
			if (aWorld instanceof Level && !aWorld.isClientSide()) GT_API_Proxy.SCHEDULED_TILEENTITY_UPDATES.add((PrefixBlockTileEntity)tTileEntity);
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
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {
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
		if (aTileEntity instanceof PrefixBlockTileEntity) ((PrefixBlockTileEntity)aTileEntity).receiveMetaData(aMetaData); // F3-render #2: сбрасывает кэш mTexture вместе с mMetaData.
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
	
	// @Override
	public void harvestBlock(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {
		aPlayer.awardStat(Stats.BLOCK_MINED.get(this), 1); /* было Stats.mineBlockStatArray[getIdFromBlock(this)] (1.7.10 int-ID) -> Stats.BLOCK_MINED.get(Block) [Stats.java:12] + Player.awardStat [Player.java:1413] */
		UT.Entities.exhaust(aPlayer, 0.025F);
		// было EnchantmentHelper.getSilkTouchModifier(Player)/getFortuneModifier(Player) (1.7.10) - удалены в neo;
		// реальный neo: EnchantmentHelper.getEnchantmentLevel(Holder<Enchantment>,LivingEntity) по Holder из
		// RegistryAccess (сверено, EnchantmentHelper.java:292 + Enchantments.SILK_TOUCH/FORTUNE), тот же приём,
		// что уже принят и одобрен ревизией в GT_API_Proxy.onBlockHarvestingEvent (GT_API_Proxy.java:1450-1451)
		// и в MultiTileEntityBlock.harvestBlock (тот же класс проблемы).
		net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> tSilkTouchHolder = aWorld.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
		net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> tFortuneHolder = aWorld.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
		boolean aSilkTouch = EnchantmentHelper.getEnchantmentLevel(tSilkTouchHolder, aPlayer) > 0;
		int aFortune = EnchantmentHelper.getEnchantmentLevel(tFortuneHolder, aPlayer);
		ArrayList<ItemStack> tList = mDrops.getDrops(this, aWorld, aX, aY, aZ, aFortune, aSilkTouch);
		float aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, 0, aFortune, 1.0F, aSilkTouch, aPlayer);
		for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);
	}
	
	// BUG-020 (дроп руды): GT6-хуки дропа выше (harvestBlock/dropBlockAsItemWithChance/getDrops) — мёртвые 1.7.10-имена;
	// neo рождает дропы из loot-table, которой у PrefixBlock нет → дроп был ПУСТО (замер gt6oreprobe). Мост тем же приёмом,
	// что BlockBase.getDrops:214 (neo getDrops(state,params) → GT6 mDrops), + silk/fortune из THIS_ENTITY — 1:1 семантика
	// harvestBlock:648-650. Материал жив через LAST_BROKEN_TILEENTITY (onDestroyedByPlayer выше). dropResources дальше сам
	// поднимает BlockDropsEvent → onBlockHarvestingEvent (unification/blockToSilk) — конвейер 1.7.10 HarvestDropsEvent цел.
	@Override protected java.util.List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		net.minecraft.server.level.ServerLevel tLevel = aParams.getLevel();
		net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
		if (tOrigin == null) return super.getDrops(aState, aParams);
		// BUG-024: гейт дропа от взрыва — ЦЕНТР WD.explosionDropDenied (1.7.10 Explosion.doExplosionB / ExplosionGT:175; консолидация, копии искоренены).
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
	// F-creative: getSubItems — метод GT6-предмета (PrefixBlockItem:81), не член neo Item; предмет PrefixBlock всегда
	// PrefixBlockItem (ctor Class<? extends PrefixBlockItem>) — каст. GT6 зовёт getSubBlocks внутренне (BlockMetaType:200),
	// функционал сохранён (neo креатив-пайплайн его не зовёт — это отдельная F-creative event-фаза).
	public void getSubBlocks(Item aItem, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {if (aItem instanceof PrefixBlockItem tItem) tItem.getSubItems(aItem, aCreativeTab, aList);}
	/** Where I come from, we set the TileEntities ourselves instead of letting a Handler do it. */
	public final BlockEntity createNewTileEntity(Level aWorld, int aMeta) {return null;}
	/** Where I come from, we set the TileEntities ourselves instead of letting a Handler do it. */
	public final BlockEntity createTileEntity(Level aWorld, int aMeta) {return null;}
	// F3-render КОРЕНЬ «руды в прогрузке/серое вкрапление»: neo объявляет наличие BE у блока ЧЕРЕЗ newBlockEntity (это
	// ЕДИНСТВЕННЫЙ путь в neo — в отличие от 1.7.10, где TE ставились вручную). Прежний null → neo на КЛИЕНТЕ не создавал BE
	// для руды → синхронизированный сервером PrefixBlockTileEntity (mMetaData=материал) не удерживался (be=null у 203/203 руд,
	// GT6-ORE-PROBE) → материал недоступен → getMetaMaterial=NULL → серое вкрапление без цвета. Возвращаем свежий
	// PrefixBlockTileEntity (mMetaData дочитывается из синка readFromNBT/receiveMetaData; placeBlock всё равно ставит свой BE
	// с материалом на сервере). RE-APPLY 2026-07-17: безопасно после снятия серверно-тикового worldgen (генерация в Feature.place
	// на регионе, реентрантного getChunk текущего чанка больше нет — прежний дедлок с ore-BE устранён в корне).
	@Override public final BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {return new PrefixBlockTileEntity(aPos, aState);}
	@Override public String toString() {return mNameInternal;}
	public String getUnlocalizedName() {return mNameInternal;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public String getHarvestTool(int aMaterialToolQuality) {return mTool;}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return new AABB(aX + mMinX, aY + mMinY, aZ + mMinZ, aX + mMaxX, aY + mMaxY, aZ + mMaxZ);}
	public AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return new AABB(aX + mMinX, aY + mMinY, aZ + mMinZ, aX + mMaxX, aY + mMaxY, aZ + mMaxZ);}
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {setBlockBounds(mMinX, mMinY, mMinZ, mMaxX, mMaxY, mMaxZ);}
	// F-shape (зеркало корней BlockBase/MTE — третий Block-корень без общего предка): neo-коллизия/outline из тех же
	// статических bounds mMin*..mMax*, что 1.7.10-каналы выше (:678-680). Bounds финальны per-инстанс → позиция/мир не
	// нужны, одна ветка обслуживает и живой мир, и BlockState-кэш (EmptyBlockGetter: снег/isFaceSturdy). Полный куб
	// (руды/блоки 0..1) → super без изменений; неполные prefix-формы получают реальную коллизию и прицел-рамку.
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (!hasCollision) return net.minecraft.world.phys.shapes.Shapes.empty();
		if (mMinX <= 0 && mMinY <= 0 && mMinZ <= 0 && mMaxX >= 1 && mMaxY >= 1 && mMaxZ >= 1) return super.getCollisionShape(aState, aWorld, aPos, aContext);
		return net.minecraft.world.phys.shapes.Shapes.create(new AABB(mMinX, mMinY, mMinZ, mMaxX, mMaxY, mMaxZ));
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (mMinX <= 0 && mMinY <= 0 && mMinZ <= 0 && mMaxX >= 1 && mMaxY >= 1 && mMaxZ >= 1) return super.getShape(aState, aWorld, aPos, aContext);
		net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.create(new AABB(mMinX, mMinY, mMinZ, mMaxX, mMaxY, mMaxZ));
		return rShape.isEmpty() ? net.minecraft.world.phys.shapes.Shapes.block() : rShape;
	}
	// F12/F9-hardness (BUG-020): в 1.7.10 getBlockHardness был @Override реального Forge-хука — движок звал его сам.
	// В neo канал сместился в getDestroyProgress (Properties.destroyTime у PrefixBlock не задан = 0 → блок ломался
	// мгновенно, mBaseHardness руд не участвовал). Мост тем же приёмом, что BlockBase:248 (vanilla-формула, 1:1).
	@Override protected float getDestroyProgress(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.entity.player.Player aPlayer, BlockGetter aWorld, BlockPos aPos) {
		if (!(aWorld instanceof Level tLevel)) return super.getDestroyProgress(aState, aPlayer, aWorld, aPos);
		return WD.destroyProgress(getBlockHardness(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()), aPlayer, aState, aWorld, aPos); // vanilla-формула — ЦЕНТР WD.destroyProgress
	}
	// BUG-020 (второй операнд формулы): 1.7.10 getBlockMetadata = bind4(mToolQuality материала) — см. getExplosionResistance
	// выше; quality из материала TE (WD.te внутри страхуется LAST_BROKEN_TILEENTITY → и harvest-путь после removeBlock жив).
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {
		if (mBaseHardness < 0) return -1;
		if (mBaseHardness == 0) return 0;
		OreDictMaterial tMaterial = getMetaMaterial(aWorld, aX, aY, aZ);
		return Math.max(1, mBaseHardness * (1+getHarvestLevel(tMaterial == null ? 0 : UT.Code.bind4(tMaterial.mToolQuality))));
	}
	// F3-render (отложенная фаза): super.getRenderType() удалён из neo (рендер data-driven) -> -1; см. MultiTileEntityBlock:436.
	public int getRenderType() {return RendererBlockTextured.INSTANCE==null?-1:RendererBlockTextured.INSTANCE.mRenderID;}
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
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], семантика ИНВЕРТИРОВАНА (shouldRender -> skipRendering) И новая сигнатура не
	// передаёт World/BlockPos вовсе - невозможно вызвать setBlockBoundsBasedOnState(aWorld,x,y,z) как раньше.
	// F3 functional-adapted (neo skipRendering сигнатура потеряла World/BlockPos → per-TE culling недостижим; используется vanilla-дефолт super.skipRendering, 1:1 по следствию): побочный эффект setBlockBoundsBasedOnState
	// недостижим без позиции; используем ванильный дефолт (тот же fallback, что и в старой ветке
	// super.shouldSideBeRendered, просто под новым именем/полярностью).
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
	
	public BlockEntity createTileEntity(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, byte aSide, short aMetaData, CompoundTag aNBT) {
		// blockstate руды передаём в TE (= defaultBlockState, как ставит WD.set) → TE кэширует верный state сразу,
		// без «Block state mismatch … updating» на загрузке чанка (руды и так генерировались, это был лишь шум кэша).
		PrefixBlockTileEntity rTileEntity = new PrefixBlockTileEntity(new net.minecraft.core.BlockPos(aX, aY, aZ), defaultBlockState());
		if (aNBT != null) rTileEntity.readFromNBT(aNBT);
		rTileEntity.mMetaData = aMetaData;
		rTileEntity.mItemNBT = aNBT == null ? null : aNBT.contains("gt.nbt.drop") ? aNBT.getCompoundOrEmpty("gt.nbt.drop") : aNBT;
		return rTileEntity;
	}
	
	protected boolean checkGravity(Level aWorld, int aX, int aY, int aZ) {
		if (mGravity && aY > 0 && WD.te(aWorld, aX, aY, aZ, T) != null && FallingBlock.isFree(WD.block(aWorld, aX, aY - 1, aZ).defaultBlockState())) {
			// было BlockFalling.fallInstantly (1.7.10 static-поле, дефолт false, не найден ни в одном из 3 корней) ->
			// "T"; World.checkChunksExist(±32) -> ILevelReaderExtension.isAreaLoaded(BlockPos,int) [ILevelReaderExtension.java:19]
			// (тот же приём, что и BlockBase.checkGravity/decisions/DEFERRED-LEDGER.md §B2).
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
