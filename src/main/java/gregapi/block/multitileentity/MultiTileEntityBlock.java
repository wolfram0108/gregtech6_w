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

	/** F-bounds (the same approach as BlockBase.java): last set bounds (1.7.10 mutated Block.mBoundingBox);
	 *  neo bounds are immutable -> stored ourselves, render usage deferred to the F3 client pass. IBlock-mandatory method. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	@Override public float[] getRenderBounds() {return mRenderBounds;}

	/** F9 follow-up: gregapi Material is stored by the MTE block (the same pattern as BlockBase); neo removed the vanilla Block.getMaterial()/blockMaterial. */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}

	public static String getName(String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		// F9/sound: was aSoundType.soundName (1.7.10 String sound category) in the MTE registration key. neo SoundType has no
		// name — reproducing 1.7.10 soundName 1:1 (values verified FACTUALLY against the golden dump: iron/machine(METAL)→"stone",
		// rock(STONE)→"stone", cloth/redstonelight(WOOL)→"cloth", leaves/tnt(GRASS)→"grass", wood(WOOD)→"wood").
		return "gt.block.multitileentity." + aNameOfVanillaMaterialField + "." + soundName(aSoundType) + "." + aTool + "." + aHarvestLevelOffset + "." + aHarvestLevelMinimum + "." + aHarvestLevelMaximum + "." + aOpaque + "." + aNormalCube;
	}

	/** 1.7.10 SoundType.soundName equivalent for the MTE registration key (verified against the golden dump). Other SoundTypes
	 *  don't occur in getOrCreate — falls back to the break-sound Identifier (shows up in the engine dump as a diff, not silently wrong). */
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
	// F16/F13: Properties at ctor — sound (step sound) + noOcclusion for non-opaque (otherwise render is solid + light is blocked). setId is mandatory.
	private static net.minecraft.world.level.block.state.BlockBehaviour.Properties mkProps(SoundType aSoundType, String aRegName, boolean aOpaque, String aTool, Material aVanillaMaterial) {
		// F-shape: dynamicShape() is MANDATORY — otherwise neo caches getCollisionShape (builds it once with EmptyBlockGetter/
		// BlockPos.ZERO, BlockBehaviour:916) → the per-BE shape (getCollisionShape bridge below, MTE-Rock/pipes) is ignored,
		// snow/collision/isFaceSturdy come from the static cache = full cube. dynamicShape → cache isn't built → the bridge lives.
		// F-shape SIDE EFFECT (pipe disappeared underwater): dynamicShape() => the shape cache isn't built => calculateSolid()
		// (BlockBehaviour:472-478) returns false when cache==null => legacySolid=false => blocksMotion()=false =>
		// vanilla water considers the block permeable (FlowingFluid.canHoldFluid) and DESTROYS it when flooded.
		// 1.7.10: MTE had a solid Material (machine/rock) — water flowed around it. forceSolidOn() (BlockBehaviour:473) => 1:1.
		net.minecraft.world.level.block.state.BlockBehaviour.Properties p = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().dynamicShape().forceSolidOn().sound(aSoundType).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aRegName))));
		if (!aOpaque) p = p.noOcclusion();
		// BUG-064 (Jade stayed silent about the tool on machines): in 1.7.10 block hardness was queried POSITIONALLY —
		// getBlockHardness(World,x,y,z) (original :299), and GT6 returned it from the TE, with the default when
		// IMTE_GetBlockHardness was absent being 1.0F. neo has no positional channel: BlockState.getDestroySpeed is final and
		// returns the static Properties.destroyTime (BlockBehaviour:636-638), which wasn't set here —
		// meaning FROM THE OUTSIDE every GT6 machine looked like it "breaks instantly". That's exactly what made Jade stay
		// silent: its vanilla handler skips the block if `!requiresCorrectToolForDrops && getDestroySpeed == 0`
		// (Jade 26.1-neoforge sources, SimpleToolHandler:45-47). We set the SAME default GT6 value (1.0F);
		// the exact per-TE hardness still lives in getDestroyProgress below and doesn't depend on this field —
		// it computes progress itself (WD.destroyProgress from TE hardness) and doesn't call super.
		p = p.destroyTime(1.0F);
		// F-harvest-tool (1:1 GT6, FIXED per a player report "ropes/anvils should break by hand"):
		// in 1.7.10 the gate "does harvesting need a tool" was decided by the MATERIAL (EntityPlayer.canHarvestBlock →
		// Material.isToolNotRequired), NOT the getHarvestTool string — that only set the EFFECTIVE tool.
		// The aUtilStone/Wood/Wool sets (anvils/ropes/scaffolding/pebbles) on Material.redstoneLight (tool NOT
		// required) → hand broke them (/30) AND got drops; only materials with setRequiresTool require a tool
		// (MaterialMachines/rock/iron/anvil). The previous gate "mTool non-empty" hung the flag on EVERYTHING — hands lost
		// both the drop and the /30 speed. neo equivalent of the 1.7.10 semantics: requiresCorrectToolForDrops ⟺ the material
		// requires a tool (and a tool is assigned).
		if (aTool != null && !aTool.isEmpty() && aVanillaMaterial != null && !aVanillaMaterial.isToolNotRequired()) p = p.requiresCorrectToolForDrops();
		return p;
	}
	protected MultiTileEntityBlock(String aModID, String aNameOfVanillaMaterialField, Material aVanillaMaterial, SoundType aSoundType, String aTool, int aHarvestLevelOffset, int aHarvestLevelMinimum, int aHarvestLevelMaximum, boolean aOpaque, boolean aNormalCube) {
		// F12 follow-up (block-split, MTE): setId in Properties (neo Block requires an id); namespace=GT (gt.multitileentity — GT6
		// content, golden = gregtech:; matches the registry ST.register→registerBlock below). The name is computed with the same getName(...) as mNameInternal (line below) → the key matches.
		// Construction happens on RegisterEvent via GT_API.deferBlockInit (call site getOrCreate/Loader_Others).
		// F16: sound(aSoundType) (step sound). F13/F16: non-opaque → .noOcclusion() (otherwise render is solid + light is blocked). mkProps below.
		super(mkProps(aSoundType, getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube), aOpaque, aTool, aVanillaMaterial));
		mMaterial = aVanillaMaterial;
		if (GAPI.mStartedInit) throw new IllegalStateException("Blocks can only be initialised within preInit!");
		
		mNameInternal = getName(aNameOfVanillaMaterialField, aVanillaMaterial, aSoundType, aTool, aHarvestLevelOffset, aHarvestLevelMinimum, aHarvestLevelMaximum, aOpaque, aNormalCube);
		ST.register(this, mNameInternal, BlockItem.class);
		
		MULTITILEENTITYBLOCKMAP.put(aModID + ":" + mNameInternal, this);
		
		// F16: setStepSound IS WIRED — the sound is set in mkProps above (.sound(aSoundType) at ctor). Not a stub.
		mOpaque = aOpaque;
		mNormalCube = aNormalCube;
		
		mTool = aTool.toLowerCase();
		mHarvestLevelOffset = aHarvestLevelOffset;
		mHarvestLevelMinimum = Math.max(0, aHarvestLevelMinimum);
		mHarvestLevelMaximum = Math.max(aHarvestLevelMinimum, aHarvestLevelMaximum);
		
		// F13/F16: opaque IS WIRED in Properties at ctor (mkProps above: non-opaque → .noOcclusion() → neo render/light are correct).
		// The own isOpaqueCube()/getLightOpacity() read mOpaque for GT6-internal logic. Not a stub.

		if (MD.Mek.mLoaded) try {MekanismAPI.addBoxBlacklist(this, W);} catch(Throwable e) {e.printStackTrace(ERR);}
		// F12 follow-up (block-split): ST.hide → ST.make (ItemStack) → server-start → deferItemInit.
		gregapi.GT_API.deferItemInit(() -> ST.hide(this));
	}
	
	// @Override
	// TAKEN APART (wave 2A consolidation, orchestrator decision). This 6-arg method is the 1.7.10 signature
	// Block.breakBlock(World,x,y,z,Block,meta); the engine no longer calls it (the "// @Override" comment above is
	// not an annotation, plain text). No caller exists: grep "\.breakBlock\(aWorld\|\.breakBlock\(tWorld\|\.breakBlock\(level"
	// over the whole tree - 0 matches (every real `.breakBlock(...)` call in the tree is the 0-arg form,
	// either via `super.breakBlock()` or a line inside this very method's body below).
	// The line below carries the IMTE_BreakBlock veto: "return true to prevent the TileEntity from being removed"
	// (contract declared by the original, gregtech6/.../IMultiTileEntity.java:84). Analysis of both trees:
	// the 1.7.10 original - 18 implementations (`grep -rn "public boolean breakBlock()" src/main/java/` = 18),
	// NOT A SINGLE ONE returns `T`; the only `return T` line in the whole tree sits in
	// example/MultiTileEntityChest.java inside a COMMENTED-OUT method. The port - 15 implementations,
	// NOT A SINGLE ONE returns `T` (same formula, same result).
	// Conclusion: the channel is REDUNDANT, not broken. The veto never fired even in the original — it's an
	// author's design provision left unused; the port's behavior is identical to the original without any bridge. The
	// bridge is NOT wired (wiring it would change behavior on zero blocks) and the method is NOT deleted (that would
	// erase the author's contract, which the port reproduces rather than reinterprets). Wire it if an
	// implementation of IMTE_BreakBlock.breakBlock() returning T appears.
	public final void breakBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMetaData) {
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity);
		// was aTileEntity.shouldRefresh(...) (1.7.10 TileEntity.shouldRefresh, REMOVED entirely from neo BlockEntity,
		// not found in any of the 3 roots) - GT6 implements it itself on TileEntityBase01Root (no longer a vanilla
		// override, just a regular method), so we route through a cast; a non-GT6 TE (shouldn't happen for MTE)
		// is treated as "refresh=true" (default of TileEntityBase01Root.mShouldRefresh), so as not to lose the trigger.
		boolean tShouldRefresh = !(aTileEntity instanceof gregapi.tileentity.base.TileEntityBase01Root) || ((gregapi.tileentity.base.TileEntityBase01Root)aTileEntity).shouldRefresh(this, aBlock, aMetaData, aMetaData, aWorld, aX, aY, aZ);
		if (aTileEntity == null || !tShouldRefresh) return;
		if (aTileEntity instanceof IMTE_BreakBlock && ((IMTE_BreakBlock)aTileEntity).breakBlock()) return;
		if (aTileEntity instanceof IMTE_HasMultiBlockMachineRelevantData && ((IMTE_HasMultiBlockMachineRelevantData)aTileEntity).hasMultiBlockMachineRelevantData()) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(aWorld, aX, aY, aZ, this, (byte)aMetaData, T);
		aWorld.removeBlockEntity(new BlockPos(aX, aY, aZ)); // was aWorld.removeTileEntity(x,y,z) (1.7.10 World), neo Level.removeBlockEntity(BlockPos) [Level.java:688]
	}

	// Wiring the "block removed — touch neighbors" channel (the same approach PrefixBlock.affectNeighborsAfterRemoval and
	// BlockBaseTree.affectNeighborsAfterRemoval already apply for this name): 1.7.10 breakBlock(World,x,y,z,Block,meta)
	// -> neo BlockBehaviour.affectNeighborsAfterRemoval(BlockState,ServerLevel,BlockPos,boolean) [BlockBehaviour.java:170],
	// the engine calls it as the last line of LevelChunk.setBlockState (LevelChunk.java:318-322), right after the regular
	// removeBlockEntity (:315). Only the leftover-BE sweep below hangs here - the GT6-specific content of the channel
	// (6-arg breakBlock/IMTE_BreakBlock veto) is deliberately NOT wired here: it has a separate, already-occupied channel
	// (TileEntityBase05Inventories.preRemoveSideEffects -> 0-arg breakBlock()), and the decision on the 6-arg method is
	// outside the scope of this fix.
	@Override protected void affectNeighborsAfterRemoval(BlockState aState, ServerLevel aWorld, BlockPos aPos, boolean aMovedByPiston) {
		sweepBlockEntityRemains(aWorld, aPos);
		super.affectNeighborsAfterRemoval(aState, aWorld, aPos, aMovedByPiston);
	}

	/** LEFTOVER BE SWEEP (an engine hole; verified against {@code reference/engine/neo-decompiled} - 26.1.2 repeats
	 *  1.20.1 1:1). The regular BlockEntity removal - {@code LevelChunk.setBlockState} calls {@code this.removeBlockEntity(pos)}
	 *  DIRECTLY (LevelChunk.java:315), bypassing any Block hook - and misses the BlockEntity in TWO
	 *  chunk states, both legitimate and both occurring in a live world:
	 *
	 *  <p><b>1. The record is still "packed".</b> {@code LevelChunk.removeBlockEntity} removes only from {@code blockEntities}
	 *  and does NOT touch {@code pendingBlockEntities} (LevelChunk.java:482-497). Unpacking (draining {@code pendingBlockEntities})
	 *  happens only in {@code LevelChunk.postProcessGeneration} (LevelChunk.java:573-607), and its sole caller is
	 *  {@code ChunkMap.prepareTickingChunk} (ChunkMap.java:679-683), i.e. the chunk must reach TICKING. A chunk at the
	 *  edge of the radius (loaded but not ticking) never reaches unpacking: the block removal happens silently, the
	 *  pending entry remains and is written to disk as an orphan with its full tags - a "ghost" with no collision, on whose
	 *  spot nothing can be placed.
	 *
	 *  <p><b>2. The chunk isn't yet declared loaded.</b> The body of {@code LevelChunk.removeBlockEntity} is entirely gated
	 *  by {@code isInLevel()} = {@code loaded || isClientSide} (LevelChunk.java:413-415, 483), and {@code loaded}
	 *  is set only on promotion to FULL, after the chunk constructor already ran. A removal that falls into this window
	 *  the engine doesn't perform at all.
	 *
	 *  <p><b>The technique used is existing, not new.</b> The call {@code chunk.getBlockEntity(pos)} unpacks the pending
	 *  entry itself ({@code pendingBlockEntities.remove} + promotion) - the ore-meta writing funnel uses this same
	 *  technique to remove the pending entry
	 *  ({@link gregapi.block.prefixblock.PrefixBlock#setOreMeta}). Beyond that the leftover is removed the regular way, and for
	 *  window #2 (the regular path is a no-op) the entity marks itself removed.
	 *
	 *  <p><b>There's no "new state" argument here</b> (unlike the old {@code onRemove}, from which the sweep
	 *  is carried over) - we read the current cell state directly: the section is already rewritten before this hook is
	 *  called (LevelChunk.java:280, before :321), so {@code aWorld.getBlockState(aPos)} gives the same answer the
	 *  new-state parameter would have given.
	 *
	 *  <p><b>MTE-to-MTE replacement is unaffected:</b> when {@code aWorld.getBlockState(aPos).hasBlockEntity()} the leftover
	 *  already belongs to the NEW block (the engine creates it further down the same {@code setBlockState}, :331-347) -
	 *  it must not be touched, and we don't touch it. This condition applies to a change between DIFFERENT Java Block
	 *  classes (blockChanged=true). MTE-to-MTE replacement by the SAME physical Block class (the typical case,
	 *  {@code MultiTileEntityBlockInternal.placeBlock} - GT6 keeps the machine's identity in meta/BE, not in the Block
	 *  type) gives {@code blockChanged=false}, and this hook isn't called at all - for that case the pending entry is
	 *  removed centrally in {@code WD.te} (item 5), with the same {@code chunk.getBlockEntity(pos)} technique as here.
	 *
	 *  <p><b>1.7.10.</b> There, TE removal belonged to the body of {@code breakBlock} (original
	 *  {@code MultiTileEntityBlock:145-152} - {@code aWorld.removeTileEntity(x,y,z)} as the last line), and the
	 *  split between "live entity / packed pending entry" in the chunk didn't exist at all: {@code Chunk.chunkTileEntityMap}
	 *  was a single map. The sweep restores the same outcome - "after a block is removed there's no entity in the cell" -
	 *  rather than introducing new behavior. */
	private static void sweepBlockEntityRemains(Level aWorld, BlockPos aPos) {
		if (aWorld == null || aWorld.getBlockState(aPos).hasBlockEntity()) return;
		try {
			net.minecraft.world.level.chunk.LevelChunk tChunk = aWorld.getChunkAt(aPos);
			if (tChunk == null) return;
			BlockEntity tRemains = tChunk.getBlockEntity(aPos); // unpacks the pending entry (see the analysis above)
			if (tRemains == null) return;
			tChunk.removeBlockEntity(aPos);
			if (!tRemains.isRemoved()) tRemains.setRemoved(); // "chunk not yet loaded" window: the regular removal is a no-op
		} catch (Throwable e) {e.printStackTrace(ERR);}
	}

	// was @Override Block.getMapColor(int) (1.7.10) - removed in neo; the own byte-meta dispatcher
	// remains a regular GT6 method (not an engine override). super.getMapColor(aMeta) (vanilla default = the material)
	// is replaced with mMaterial.getMaterialMapColor() - the same default source, 1:1.
	public MapColor getMapColor(int aMeta) {
		return mMapColor == null ? mMaterial.getMaterialMapColor() : mMapColor;
	}
	public MultiTileEntityBlock setMapColor(MapColor aMapColor) {
		mMapColor = aMapColor;
		return this;
	}
	// F13: getMapColor(int) → IBlockExtension.getMapColor(BlockState,BlockGetter,BlockPos,MapColor). The bridge from gregapi.block.MapColor
	// to the engine one is centralized in MapColor.toNeo() (F9 bridge, indices 0-63 match). Return the GT6 block color 1:1; null → default.
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
	/** F17 item 12 (decisions/F17-chunk-be-sync.md, "the hole follows the action"): the 1.7.10 sync contract — a packet
	 *  UPDATES the existing client TE, creating a new one only when the TE is absent/the wrong one. The port recreated the BE
	 *  UNCONDITIONALLY (the path was written for a missing BE) → a mature client BE was discarded on every full sync
	 *  (a neighbor update) → at the moment of the immediate mesh rebuild the BE is "raw" → notick MTE (walls) are transparent
	 *  at the action's location until the next rebuild. Reuse before creation — central for ALL MTE and all packet types. */
	private static BlockEntity reuseOrCreateClientTE(Level aWorld, int aX, int aY, int aZ, short aID1, short aID2, INetworkHandler aNetworkHandler, boolean aSnapshotHasCovers) {
		BlockEntity tExisting = WD.te(aWorld, aX, aY, aZ, T);
		if (tExisting instanceof IMultiTileEntity tMTE && !tExisting.isRemoved()
		 && tMTE.getMultiTileEntityRegistryID() == aID1 && tMTE.getMultiTileEntityID() == aID2) {
			// BUG-114: the packet with IDs is a FULL SNAPSHOT of client state, and in 1.7.10 it closed even what's
			// not in it: receiving it ALWAYS recreated the TE (original MultiTileEntityBlock:172 tRegistry.getNewTileEntity),
			// and everything unsynced was born as default. The port reuses the BE (F17 item 12 above), so we must
			// clear the CONDITIONAL part of the snapshot ourselves. There's one conditional part in the tree — the cover block: the
			// sender only sends it when hasCovers() (TileEntityBase06Covers:176, notick/TileEntityBase04Covers:217), everything
			// else (paint, visual, direction) is always sent. Removing the LAST cover produced a snapshot with no cover data —
			// and old covers lived on the client until the section reloaded. The clearing channel is the same receive method
			// (receiveDataCovers(null, ...) → mCovers = null, TileEntityBase06Covers:101-103), we don't create our own.
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
	// was super.addCollisionBoxesToList(...) (1.7.10 Block, REMOVED entirely from neo - box-list collision
	// replaced by the engine with the VoxelShape system). Default inline port 1:1 instead of the super call (Block.java:661-669 recompSrc):
	// getCollisionBoundingBoxFromPool + intersects check, the same algorithm the vanilla default had.
	@SuppressWarnings("unchecked") public final void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, @SuppressWarnings("rawtypes") List aList, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_AddCollisionBoxesToList) ((IMTE_AddCollisionBoxesToList)aTileEntity).addCollisionBoxesToList(aAABB, aList, aEntity); else if (aTileEntity != null) {AABB tBox = getCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ); if (tBox != null && aAABB.intersects(tBox)) aList.add(tBox);}}
	public final AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetCollisionBoundingBoxFromPool ? ((IMTE_GetCollisionBoundingBoxFromPool)aTileEntity).getCollisionBoundingBoxFromPool() : aTileEntity == null ? null : new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	// F-shape (the "engine channel moved" class, like F-tick/useOn): 1.7.10 getCollisionBoundingBoxFromPool/getSelectedBoundingBox
	// (AABB) removed — neo does block shape via VoxelShape (getCollisionShape=collision/snow, getShape=outline). WITHOUT the bridge MTE blocks
	// gave the default FULL CUBE → snow would settle on pebbles (SnowLayerBlock.canSurvive→isFaceFull(getCollisionShape,UP)),
	// collision/outline were full (a pebble was impassable). Dispatch order = 1:1 with the 1.7.10 chain (World.getCollidingBoundingBoxes →
	// addCollisionBoxesToList): FIRST IMTE_AddCollisionBoxesToList (a list of sub-boxes; scaffolding/rope = a passable frame,
	// pipes = sleeves by mConnections, machines via the TE04/TE06 base = default full box), fallback — broad-phase
	// getCollisionBoundingBoxFromPool. The entity (shift checks, Scaffold design 1) comes from EntityCollisionContext (null for the
	// Empty context = 1:1 with aEntity==null). BE AABBs are absolute (box()=pos+bounds) → a relative VoxelShape (move(-pos));
	// an empty list/null → Shapes.empty (passable, snow won't settle). Centralized across the whole MTE layer.
	// ⛔ WITHOUT an instanceof Level gate: the engine also calls the shape with a CHUNK BlockGetter — BlockCollisions.computeNext:90
	// isSuffocating(chunk,pos) → isCollisionShapeFullBlock → (dynamicShape, no cache) a live getCollisionShape(chunk).
	// The gate returned super=FULL CUBE → "block suffocates" → LocalPlayer.moveTowardsClosestSpace pushed the
	// player out of passable MTE (scaffolding/rope) every tick — climbing broke off (judged by the gt6climbprobe stand:
	// onClimbable=true, the player was climbing, but suffocateCell=true pushed them out). The TE lookup doesn't need Level —
	// BlockGetter.getBlockEntity is enough.
	// Fix #4 (BUG-106): a shape VOXELIZATION cache. MTE boxes are discrete (fractions of 1/16 from mConnections/bounds) —
	// identical box sets occur across thousands of blocks, yet Shapes.create/or (an expensive voxelization) was called on
	// EVERY collision query every tick (~2% of all allocations per JFR). The key is the exact box coordinates in the
	// block's local frame: identical boxes → the same (immutable, shared) shape. The boxes themselves are still
	// collected by a live TE call (the dependency on the entity/neighbors is preserved 1:1) — only the "boxes → voxel shape"
	// transform is cached, since it's a pure function.
	private static final class ShapeKey {
		private final double[] mCoords; private final int mHash;
		ShapeKey(double[] aCoords) {mCoords = aCoords; mHash = java.util.Arrays.hashCode(aCoords);}
		@Override public int hashCode() {return mHash;}
		@Override public boolean equals(Object aOther) {return aOther instanceof ShapeKey tKey && java.util.Arrays.equals(mCoords, tKey.mCoords);}
	}
	private static final java.util.concurrent.ConcurrentHashMap<ShapeKey, net.minecraft.world.phys.shapes.VoxelShape> SHAPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
	private static net.minecraft.world.phys.shapes.VoxelShape cachedShape(List<AABB> aBoxes, BlockPos aPos) {
		if (SHAPE_CACHE.size() > 8192) SHAPE_CACHE.clear(); // safety valve (shapes are discrete — the real size is in the hundreds)
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

	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld == null) return super.getCollisionShape(aState, aWorld, aPos, aContext);
		BlockEntity tTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (tTileEntity instanceof IMTE_AddCollisionBoxesToList tMulti) {
			List<AABB> tList = new ArrayList<>();
			tMulti.addCollisionBoxesToList(new AABB(aPos.getX()-1, aPos.getY()-1, aPos.getZ()-1, aPos.getX()+2, aPos.getY()+2, aPos.getZ()+2), tList, aContext instanceof net.minecraft.world.phys.shapes.EntityCollisionContext tEntityContext ? tEntityContext.getEntity() : null);
			if (tList.isEmpty()) return net.minecraft.world.phys.shapes.Shapes.empty();
			return cachedShape(tList, aPos); // fix #4: voxelization from the cache
		}
		if (tTileEntity instanceof IMTE_GetCollisionBoundingBoxFromPool tPool) {
			AABB tBox = tPool.getCollisionBoundingBoxFromPool();
			return tBox == null ? net.minecraft.world.phys.shapes.Shapes.empty() : cachedShape(java.util.List.of(tBox), aPos); // fix #4
		}
		return super.getCollisionShape(aState, aWorld, aPos, aContext); // TE null/no interfaces → full cube (1:1 pool fallback)
	}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (aWorld instanceof Level tLevel) {
			BlockEntity tTileEntity = WD.te(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
			if (tTileEntity instanceof IMTE_GetSelectedBoundingBoxFromPool tSel) {
				AABB tBox = tSel.getSelectedBoundingBoxFromPool();
				if (tBox != null) return cachedShape(java.util.List.of(tBox), aPos); // fix #4: voxelization from the cache
			}
		}
		return super.getShape(aState, aWorld, aPos, aContext); // regular MTE (machines/chests) — full cube (as before)
	}
	public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_UpdateTick) ((IMTE_UpdateTick)aTileEntity).updateTick(aRandom);}
	public final void onBlockDestroyedByPlayer(Level aWorld, int aX, int aY, int aZ, int aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnBlockDestroyedByPlayer) ((IMTE_OnBlockDestroyedByPlayer)aTileEntity).onBlockDestroyedByPlayer(aRandom);}
	// was onBlockAdded(World,x,y,z) -> BlockBehaviour.onPlace(BlockState,Level,BlockPos,BlockState,boolean) [BlockBehaviour.java:167]
	@Override protected final void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnBlockAdded) ((IMTE_OnBlockAdded)aTileEntity).onBlockAdded();}
	// was super.dropXpOnBlockBreak(...) (1.7.10 Block, REMOVED entirely from neo). Default inline port 1:1 instead of the
	// super call (Block.java:843-854 recompSrc): the EntityXPOrb spawn loop -> neo ExperienceOrb.award(ServerLevel,Vec3,int)
	// (ExperienceOrb.java:190, the same split algorithm inside award/awardWithDirection).
	public final void dropXpOnBlockBreak(Level aWorld, int aX, int aY, int aZ, int aXP) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_DropXpOnBlockBreak) ((IMTE_DropXpOnBlockBreak)aTileEntity).dropXpOnBlockBreak(aXP); else if (!aWorld.isClientSide() && aWorld instanceof ServerLevel aServerWorld) ExperienceOrb.award(aServerWorld, new Vec3(aX+0.5, aY+0.5, aZ+0.5), aXP);}
	// F13: 1.7.10 Block.collisionRayTrace removed — neo's collision raytrace is a generic on top of VoxelShape/getShape (not a per-Block override).
	// The TE interface IMTE_CollisionRayTrace has no implementors (0, verified) → a dead compile surface, not a stub (nothing to lose).
	public final HitResult collisionRayTrace(Level aWorld, int aX, int aY, int aZ, Vec3 aVectorA, Vec3 aVectorB) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CollisionRayTrace ? ((IMTE_CollisionRayTrace)aTileEntity).collisionRayTrace(aVectorA, aVectorB) : null;}
	// was aPlayer.getHeldItem() (1.7.10 EntityPlayer no-arg, default hand) -> neo Player.getMainHandItem() (Player.java:2257)
	// U4 activation BRIDGE (player reports: a stack of ingots/coins doesn't grow on click — a NEW block is placed on
	// top instead; the battery box GUI doesn't open): GT6's onBlockActivated below is a 1:1 port, but it was ORPHANED
	// ("the channel moved"): neo calls BlockBehaviour.useItemOn (with the item, BEFORE placement by the item — like
	// 1.7.10's activateBlockOrUseItem called Block.onBlockActivated before Item.onItemUse) and useWithoutItem (empty
	// hand). We bridge BOTH into the GT6 channel: true → SUCCESS/SUCCESS_SERVER (click consumed, placement doesn't
	// happen — the ingot goes INTO the stack).
	@Override protected net.minecraft.world.InteractionResult useItemOn(ItemStack aStack, BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer, net.minecraft.world.InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
		if (aHand == net.minecraft.world.InteractionHand.MAIN_HAND && bridgeBlockActivated(aWorld, aPos, aPlayer, aHit))
			return aWorld.isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.SUCCESS_SERVER;
		return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
	}
	@Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		if (bridgeBlockActivated(aWorld, aPos, aPlayer, aHit))
			return aWorld.isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.SUCCESS_SERVER;
		return net.minecraft.world.InteractionResult.PASS;
	}
	private boolean bridgeBlockActivated(Level aWorld, BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
		net.minecraft.world.phys.Vec3 tHitVec = aHit.getLocation();
		return onBlockActivated(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aPlayer, aHit.getDirection().get3DDataValue(),
			(float)(tHitVec.x - aPos.getX()), (float)(tHitVec.y - aPos.getY()), (float)(tHitVec.z - aPos.getZ()));
	}

	public final boolean onBlockActivated(Level aWorld, int aX, int aY, int aZ, Player aPlayer, int aSide, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aPlayer != null && IL.TC_Thaumometer.equal(aPlayer.getMainHandItem(), T, T) && (!(aTileEntity instanceof ITileEntityBookShelf) || !((ITileEntityBookShelf)aTileEntity).isShelfFace(UT.Code.side(aSide)))) return F; return aTileEntity instanceof IMTE_OnBlockActivated && ((IMTE_OnBlockActivated)aTileEntity).onBlockActivated(aPlayer, UT.Code.side(aSide), aHitX, aHitY, aHitZ);}
	public final void onEntityWalking(Level aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnEntityWalking) ((IMTE_OnEntityWalking)aTileEntity).onEntityWalking(aEntity);}
	// was onBlockClicked(World,x,y,z,EntityPlayer) -> BlockBehaviour.attack(BlockState,Level,BlockPos,Player) [BlockBehaviour.java:353]
	@Override protected final void attack(BlockState aState, Level aWorld, BlockPos aPos, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnBlockClicked) ((IMTE_OnBlockClicked)aTileEntity).onBlockClicked(aPlayer); else super.attack(aState, aWorld, aPos, aPlayer);}
	// F13: 1.7.10 Block.velocityToAddToEntity removed (the vanilla default was empty; only BlockPistonMoving/Portal had an effect) —
	// no neo hook. The TE interface IMTE_VelocityToAddToEntity has no implementors (0, verified) → a dead surface, not a stub.
	public final void velocityToAddToEntity(Level aWorld, int aX, int aY, int aZ, Entity aEntity, Vec3 aVector) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_VelocityToAddToEntity) ((IMTE_VelocityToAddToEntity)aTileEntity).velocityToAddToEntity(aEntity, aVector);}
	// was isProvidingWeakPower(IBlockAccess,x,y,z,side) -> BlockBehaviour.getSignal(BlockState,BlockGetter,BlockPos,Direction) [BlockBehaviour.java:356]
	@Override protected final int getSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(UT.Code.side(aSide)) : super.getSignal(aState, aWorld, aPos, aSide);}
	// was onEntityCollidedWithBlock(World,x,y,z,Entity) -> BlockBehaviour.entityInside(BlockState,Level,BlockPos,Entity,InsideBlockEffectApplier,boolean) [BlockBehaviour.java:360];
	// the new parameters effectApplier/isPrecise (batched damage effects, an F16 concept with no 1.7.10 counterpart) are unused - GT6 didn't apply them before either.
	@Override protected final void entityInside(BlockState aState, Level aWorld, BlockPos aPos, Entity aEntity, net.minecraft.world.entity.InsideBlockEffectApplier aEffectApplier, boolean aIsPrecise) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnEntityCollidedWithBlock) ((IMTE_OnEntityCollidedWithBlock)aTileEntity).onEntityCollidedWithBlock(aEntity); else super.entityInside(aState, aWorld, aPos, aEntity, aEffectApplier, aIsPrecise);}
	// was isProvidingStrongPower(IBlockAccess,x,y,z,side) -> BlockBehaviour.getDirectSignal(BlockState,BlockGetter,BlockPos,Direction) [BlockBehaviour.java:363]
	@Override protected final int getDirectSignal(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_IsProvidingStrongPower ? ((IMTE_IsProvidingStrongPower)aTileEntity).isProvidingStrongPower(UT.Code.side(aSide)) : super.getDirectSignal(aState, aWorld, aPos, aSide);}
	public final boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_CanBlockStay) || ((IMTE_CanBlockStay)aTileEntity).canBlockStay();}
	// was onFallenUpon(World,x,y,z,Entity,dist) -> Block.fallOn(Level,BlockState,BlockPos,Entity,double) [Block.java:484] - fallDistance is now double, not float.
	@Override public final void fallOn(Level aWorld, BlockState aState, BlockPos aPos, Entity aEntity, double aFallDistance) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_OnFallenUpon) ((IMTE_OnFallenUpon)aTileEntity).onFallenUpon(aEntity, (float)aFallDistance); else super.fallOn(aWorld, aState, aPos, aEntity, aFallDistance);}
	// F13: 1.7.10 Block.onBlockHarvested/onBlockPreDestroy removed — neo's destroy pipeline calls playerWillDestroy
	// (Level,BlockPos,BlockState,Player) before the block is removed. The neo hook below dispatches both TE hooks 1:1. GT6 methods preserved.
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
	// The neo hook below dispatches IMTE_FillWithRain on RAIN (fillWithRain was rain-specific), 1:1. GT6 method preserved.
	@Override public void handlePrecipitation(BlockState aState, Level aWorld, BlockPos aPos, net.minecraft.world.level.biome.Biome.Precipitation aPrecipitation) {
		BlockEntity tTE = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		if (aPrecipitation == net.minecraft.world.level.biome.Biome.Precipitation.RAIN && tTE instanceof IMTE_FillWithRain) ((IMTE_FillWithRain)tTE).fillWithRain();
		else super.handlePrecipitation(aState, aWorld, aPos, aPrecipitation);
	}
	public final void fillWithRain(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_FillWithRain) ((IMTE_FillWithRain)aTileEntity).fillWithRain();}
	// was hasComparatorInputOverride()/getComparatorInputOverride(World,x,y,z,side) -> BlockBehaviour.hasAnalogOutputSignal(BlockState)
	// [BlockBehaviour.java:226] / BlockBehaviour.getAnalogOutputSignal(BlockState,Level,BlockPos,Direction) [BlockBehaviour.java:310];
	// the default (without an IMTE hook) = 0, the same default the engine has (BlockBehaviour.java:311).
	@Override protected final boolean hasAnalogOutputSignal(BlockState aState) {return T;}
	@Override protected final int getAnalogOutputSignal(BlockState aState, Level aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_GetComparatorInputOverride ? ((IMTE_GetComparatorInputOverride)aTileEntity).getComparatorInputOverride(UT.Code.side(aSide)) : aTileEntity instanceof IMTE_IsProvidingWeakPower ? ((IMTE_IsProvidingWeakPower)aTileEntity).isProvidingWeakPower(OPOS[UT.Code.side(aSide)]) : 0;}
	// was getLightValue(IBlockAccess,x,y,z) -> IBlockExtension.hasDynamicLightEmission(BlockState) [IBlockExtension.java:121] +
	// IBlockExtension.getLightEmission(BlockState,BlockGetter,BlockPos) [IBlockExtension.java:152]; the default (without an IMTE hook) =
	// aState.getLightEmission() (the value baked into Properties), the same default super.getLightValue would have returned.
	@Override public final boolean hasDynamicLightEmission(BlockState aState) {return T;}
	// F6 worldgen CRITICAL (deadlock): the light engine calls getLightEmission DURING chunk generation (hasDynamicLightEmission=T) →
	// a regular WD.te forces getChunk.join of the chunk being generated → the light thread waits on itself → the world entry hangs forever.
	// Fetch the BE NON-blockingly (WD.teNonForcing: only from an already-FULL chunk, otherwise null → the baked default; BE light is recomputed after gen).
	@Override public final int getLightEmission(BlockState aState, BlockGetter aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.teNonForcing(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()); return aTileEntity instanceof IMTE_GetLightValue ? UT.Code.bind4(((IMTE_GetLightValue)aTileEntity).getLightValue()) : aState.getLightEmission();}
	public final boolean isLadder(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsLadder && ((IMTE_IsLadder)aTileEntity).isLadder(aEntity);}
	// was isLadder(IBlockAccess,x,y,z,EntityLivingBase) -> IBlockExtension.isLadder(BlockState,LevelReader,BlockPos,LivingEntity) [IBlockExtension.java:178].
	// NOT the static BlockTags.CLIMBABLE (that's the interface default, for blocks with no override) - a per-position dynamic hook: LivingEntity.onClimbable()
	// calls it directly via CommonHooks.isLivingOnLadder(state,level,pos,entity) [LivingEntity.java:1755, CommonHooks.java:404-428], not through the tag -
	// the per-BE logic (Scaffold.mDesign!=3) carries over 1:1, no architectural gap. LevelReader extends BlockGetter (verified) - a direct delegate.
	@Override public final boolean isLadder(BlockState aState, LevelReader aWorld, BlockPos aPos, LivingEntity aEntity) {return isLadder(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aEntity);}
	public final boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsNormalCube ? ((IMTE_IsNormalCube)aTileEntity).isNormalCube() : mNormalCube;}
	public final boolean isReplaceable(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsReplaceable ? ((IMTE_IsReplaceable)aTileEntity).isReplaceable() : getMaterial().isReplaceable();}
	public final boolean isBurning(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBurning && ((IMTE_IsBurning)aTileEntity).isBurning();}
	public final boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {if (aWorld == null) return F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsAir && ((IMTE_IsAir)aTileEntity).isAir();}
	// was removedByPlayer(World,EntityPlayer,x,y,z,willHarvest) -> IBlockExtension.onDestroyedByPlayer
	// (BlockState,Level,BlockPos,Player,ItemStack,boolean,FluidState) [IBlockExtension.java:238], returns
	// a boolean "was the block actually destroyed" - the same contract as the old removedByPlayer.
	@Override public final boolean onDestroyedByPlayer(BlockState aBlockState, Level aWorld, BlockPos aPos, Player aPlayer, ItemStack aToolStack, boolean aWillHarvest, FluidState aFluid) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); return aTileEntity instanceof IMTE_RemovedByPlayer ? ((IMTE_RemovedByPlayer)aTileEntity).removedByPlayer(aWorld, aPlayer, aWillHarvest) : super.onDestroyedByPlayer(aBlockState, aWorld, aPos, aPlayer, aToolStack, aWillHarvest, aFluid);}
	public final boolean canCreatureSpawn(MobCategory aType, BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanCreatureSpawn && ((IMTE_CanCreatureSpawn)aTileEntity).canCreatureSpawn(aType);}
	public final boolean isBed(BlockGetter aWorld, int aX, int aY, int aZ, LivingEntity aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBed && ((IMTE_IsBed)aTileEntity).isBed(aPlayer);}
	// Bugfix: was instanceof IMTE_VelocityToAddToEntity (the wrong interface) when casting to IMTE_GetBedSpawnPosition - fixed to the correct instanceof.
	public final BlockPos getBedSpawnPosition(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBedSpawnPosition ? ((IMTE_GetBedSpawnPosition)aTileEntity).getBedSpawnPosition(aPlayer) : null;}
	public final void setBedOccupied(BlockGetter aWorld, int aX, int aY, int aZ, Player aPlayer, boolean aOccupied) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_SetBedOccupied) ((IMTE_SetBedOccupied)aTileEntity).setBedOccupied(aPlayer, aOccupied);}
	public final int getBedDirection(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBedDirection ? ((IMTE_GetBedDirection)aTileEntity).getBedDirection() : 0;}
	public final boolean isBedFoot(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBedFoot && ((IMTE_IsBedFoot)aTileEntity).isBedFoot();}
	// F13: 1.7.10 Forge Block.beginLeavesDecay has no neo counterpart (the leaf-decay system differs). IMTE_BeginLeavesDecay has no
	// implementors (0, verified) → a dead compile surface, not a stub. The default was an empty body
	// (Block.java:1956 recompSrc) - not calling super is 1:1 equivalent to "do nothing".
	public final void beginLeavesDecay(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_BeginLeavesDecay) ((IMTE_BeginLeavesDecay)aTileEntity).beginLeavesDecay();}
	// F13: 1.7.10 Forge Block.canSustainLeaves has no neo counterpart. IMTE_CanSustainLeaves has no implementors (0) → a dead surface.
	// The default was false (Block.java:1967-1970 recompSrc) - substituted directly instead of super. Not a stub.
	public final boolean canSustainLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanSustainLeaves ? ((IMTE_CanSustainLeaves)aTileEntity).canSustainLeaves() : F;}
	public final boolean isLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsLeaves && ((IMTE_IsLeaves)aTileEntity).isLeaves();}
	public final boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanBeReplacedByLeaves && ((IMTE_CanBeReplacedByLeaves)aTileEntity).canBeReplacedByLeaves();}
	public final boolean isWood(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsWood ? ((IMTE_IsWood)aTileEntity).isWood() : F;}// was super.isWood (Forge 1.7.10 Block.isWood default = false; neo's Block has no such method)
	public final boolean isReplaceableOreGen(net.minecraft.world.level.LevelAccessor aWorld, int aX, int aY, int aZ, Block aTarget) {if (GAPI.mStartedServerStarted < 1) return F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsReplaceableOreGen ? ((IMTE_IsReplaceableOreGen)aTileEntity).isReplaceableOreGen(aTarget) : (aTarget == this);}// was super.isReplaceableOreGen (Forge 1.7.10 Block.isReplaceableOreGen default = identity this==target)
	// was canConnectRedstone(IBlockAccess,x,y,z,side) -> IBlockExtension.canConnectRedstone(BlockState,BlockGetter,BlockPos,Direction) [IBlockExtension.java:904]
	@Override public final boolean canConnectRedstone(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_CanConnectRedstone ? ((IMTE_CanConnectRedstone)aTileEntity).canConnectRedstone(UT.Code.side(aSide)) : super.canConnectRedstone(aState, aWorld, aPos, aSide);}
	public final boolean canPlaceTorchOnTop(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_CanPlaceTorchOnTop ? ((IMTE_CanPlaceTorchOnTop)aTileEntity).canPlaceTorchOnTop() : isSideSolid(aWorld, aX, aY, aZ, FORGE_DIR[SIDE_TOP]);}
	// F13: 1.7.10 Forge Block.isFoliage has no neo counterpart. IMTE_IsFoliage has no implementors (0) → a dead surface.
	// The default was false (Block.java:2156-2159 recompSrc) - substituted directly instead of super. Not a stub.
	public final boolean isFoliage(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFoliage ? ((IMTE_IsFoliage)aTileEntity).isFoliage() : F;}
	// was canSustainPlant(IBlockAccess,x,y,z,side,IPlantable) -> IBlockExtension.canSustainPlant(BlockState,BlockGetter,
	// BlockPos,Direction,BlockState) [IBlockExtension.java:424], IPlantable(1.7.10 parameter)->BlockState(neo), TriState instead of boolean.
	// Plant adapted via the WD.plantable center: in 1.7.10 every vanilla plant was IPlantable (Forge patch), so the
	// IMTE hook must answer for ANY plant, not just GT6 ones — a bare instanceof left dungeon Plant Pots barren.
	@Override public final net.minecraft.util.TriState canSustainPlant(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide, BlockState aPlant) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (!(aTileEntity instanceof IMTE_CanSustainPlant)) return net.minecraft.util.TriState.DEFAULT; return net.minecraft.util.TriState.from(((IMTE_CanSustainPlant)aTileEntity).canSustainPlant(UT.Code.side(aSide), WD.plantable(aPlant)));}
	// F13: 1.7.10 Block.onPlantGrow removed from neo (no hook). IMTE_OnPlantGrow has no implementors (0) → a dead surface.
	// The vanilla default was empty → not calling super is 1:1 equivalent to "do nothing". Not a stub.
	public final void onPlantGrow(Level aWorld, int aX, int aY, int aZ, int sX, int sY, int sZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnPlantGrow) ((IMTE_OnPlantGrow)aTileEntity).onPlantGrow(sX, sY, sZ);}
	public final boolean isFertile(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFertile && ((IMTE_IsFertile)aTileEntity).isFertile();}
	public final boolean rotateBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RotateBlock && ((IMTE_RotateBlock)aTileEntity).rotateBlock(UT.Code.side(aSide));}
	public final Direction[] getValidRotations(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetValidRotations ? ((IMTE_GetValidRotations)aTileEntity).getValidRotations() : ZL_FORGEDIRECTION;}
	public final float getEnchantPowerBonus(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetEnchantPowerBonus ? ((IMTE_GetEnchantPowerBonus)aTileEntity).getEnchantPowerBonus() : 0;}
	// was getEnchantPowerBonus(World,x,y,z) -> IBlockExtension.getEnchantPowerBonus(BlockState,BlockGetter,BlockPos) [IBlockExtension.java:520].
	// aWorld here is only a BlockGetter (weaker than the old World) - the existing 1.7.10 method above is typed to Level; we delegate only
	// when it's really a Level (like getCollisionShape/receiveData in this file), otherwise a default of 0 (the same default the IMTE dispatcher gives without a TE).
	@Override public final float getEnchantPowerBonus(BlockState aState, BlockGetter aWorld, BlockPos aPos) {return aWorld instanceof Level tLevel ? getEnchantPowerBonus(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()) : 0;}
	public final boolean recolourBlock(Level aWorld, int aX, int aY, int aZ, Direction aSide, int aColor) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RecolourBlock && ((IMTE_RecolourBlock)aTileEntity).recolourBlock(UT.Code.side(aSide), (byte)aColor);}
	public final boolean shouldCheckWeakPower(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_ShouldCheckWeakPower ? ((IMTE_ShouldCheckWeakPower)aTileEntity).shouldCheckWeakPower(UT.Code.side(aSide)) : isNormalCube(aWorld, aX, aY, aZ);}
	// was shouldCheckWeakPower(IBlockAccess,x,y,z,side) -> IBlockExtension.shouldCheckWeakPower(BlockState,SignalGetter,BlockPos,Direction) [IBlockExtension.java:544].
	// SignalGetter extends BlockGetter (verified, SignalGetter.java:10) - a direct delegate; Direction->int via the CENTER UT.Code.side(Direction) (the same approach as every other side bridge in this file), the delegate takes the original int overload.
	@Override public final boolean shouldCheckWeakPower(BlockState aState, net.minecraft.world.level.SignalGetter aWorld, BlockPos aPos, Direction aSide) {return shouldCheckWeakPower(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), UT.Code.side(aSide));}
	// was getWeakChanges(IBlockAccess,x,y,z) -> IBlockExtension.getWeakChanges(BlockState,LevelReader,BlockPos)
	// [IBlockExtension.java:557], default false (matches the old vanilla default).
	@Override public final boolean getWeakChanges(BlockState aState, LevelReader aWorld, BlockPos aPos) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity instanceof IMTE_GetWeakChanges ? ((IMTE_GetWeakChanges)aTileEntity).getWeakChanges() : F;}
	public final boolean addHitEffects(Level aWorld, HitResult aTarget, ParticleEngine aRenderer) {BlockEntity aTileEntity = WD.te(aWorld, ((BlockHitResult)aTarget).getBlockPos().getX(), ((BlockHitResult)aTarget).getBlockPos().getY(), ((BlockHitResult)aTarget).getBlockPos().getZ(), T); return aTileEntity instanceof IMTE_AddHitEffects && ((IMTE_AddHitEffects)aTileEntity).addHitEffects(aWorld, aTarget, aRenderer);}
	public final boolean addDestroyEffects(Level aWorld, int aX, int aY, int aZ, int aMetaData, ParticleEngine aRenderer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_AddDestroyEffects && ((IMTE_AddDestroyEffects)aTileEntity).addDestroyEffects(aMetaData, aRenderer);}
	// was shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], the semantics are INVERTED (shouldRender -> skipRendering) AND the new signature doesn't
	// pass World/BlockPos at all - it's impossible to determine the neighboring block's TileEntity (IMTE_ShouldSideBeRendered),
	// the way the 1.7.10 original did via aWorld.getTileEntity(aX-OFFX[side],...). F3 functional-adapted (the neo skipRendering signature lost World/BlockPos → per-TE culling is unreachable; the vanilla default super.skipRendering is used, 1:1 in consequence):
	// custom per-TE dispatch is unreachable without a position; we use the vanilla default (the same fallback
	// as the old super.shouldSideBeRendered branch, just under the new name/polarity).
	@Override protected final boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {return super.skipRendering(aState, aNeighbor, aDir);}
	public final void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_SetBlockBoundsBasedOnState) ((IMTE_SetBlockBoundsBasedOnState)aTileEntity).setBlockBoundsBasedOnState(this); else if (aTileEntity == null) setBlockBounds(-999, -999, -999, -998, -998, -998); else setBlockBounds(0, 0, 0, 1, 1, 1);}
	public final AABB getSelectedBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity == null ? new AABB(-999, -999, -999, -998, -998, -998) : aTileEntity instanceof IMTE_GetSelectedBoundingBoxFromPool ? ((IMTE_GetSelectedBoundingBoxFromPool)aTileEntity).getSelectedBoundingBoxFromPool() : new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1);}
	// was randomDisplayTick(World,x,y,z,Random) -> Block.animateTick(BlockState,Level,BlockPos,RandomSource) [Block.java:355]
	@Override public final void animateTick(BlockState aState, Level aWorld, BlockPos aPos, RandomSource aRandom) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (aTileEntity instanceof IMTE_RandomDisplayTick) ((IMTE_RandomDisplayTick)aTileEntity).randomDisplayTick(aRandom); else super.animateTick(aState, aWorld, aPos, aRandom);}
	public final void onBlockExploded(Level aWorld, int aX, int aY, int aZ, Explosion aExplosion) {if (aWorld.isClientSide()) return; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity != null) LAST_BROKEN_TILEENTITY.set(aTileEntity); if (aTileEntity instanceof IMTE_OnBlockExploded) ((IMTE_OnBlockExploded)aTileEntity).onExploded(aExplosion); else WD.set(aWorld, aX, aY, aZ, NB, 0, 3);}
	// was onBlockExploded(World,x,y,z,Explosion) -> IBlockExtension.onBlockExploded(BlockState,ServerLevel,BlockPos,Explosion)/Block.wasExploded(ServerLevel,BlockPos,Explosion)
	// [IBlockExtension.java:775, Block.java:457]; called from BlockBehaviour.onExplosionHit(...,ServerLevel,...) [BlockBehaviour.java:173-193] (already server-only by parameter type).
	// The GT6 method above ALREADY sets air itself in the fallback branch (WD.set(...,NB,0,3)) - equivalent to the default neo body (level.setBlock(pos,AIR,3)+wasExploded no-op),
	// so a direct delegate without calling super/wasExploded reproduces the original 1:1 (there GT6 also fully replaced it, not calling super).
	@Override public final void onBlockExploded(BlockState aState, ServerLevel aWorld, BlockPos aPos, Explosion aExplosion) {onBlockExploded(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aExplosion);}
	// F13: 1.7.10 Block.getPickBlock(HitResult,World,x,y,z,Player) removed — neo's middle-click goes through
	// IBlockExtension.getCloneItemStack(LevelReader,BlockPos,BlockState,boolean,Player). Below, this neo hook
	// delegates to GT6's getPickBlock (the TE dispatcher IMTE_GetPickBlock), restoring behavior 1:1. GT6 methods preserved.
	@Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState, boolean aIncludeData, Player aPlayer) {
		BlockEntity tTE = WD.te(aLevel, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		ItemStack r = tTE instanceof IMTE_GetPickBlock ? ((IMTE_GetPickBlock)tTE).getPickBlock(null) : null;
		return ST.valid(r) ? r : super.getCloneItemStack(aLevel, aPos, aState, aIncludeData, aPlayer);
	}
	public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ, Player aPlayer) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	public final ItemStack getPickBlock(HitResult aTarget, Level aWorld, int aX, int aY, int aZ                      ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetPickBlock?((IMTE_GetPickBlock)aTileEntity).getPickBlock(aTarget):null;}
	@Override public final ItemStack getItemStackFromBlock(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetStackFromBlock?((IMTE_GetStackFromBlock)aTileEntity).getStackFromBlock(aSide):null;}
	public final int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetFlammability ? ((IMTE_GetFlammability)aTileEntity).getFlammability(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	// was getFlammability(IBlockAccess,x,y,z,side) -> IBlockExtension.getFlammability(BlockState,BlockGetter,BlockPos,Direction) [IBlockExtension.java:677]. A direct delegate (types match 1:1).
	@Override public final int getFlammability(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return getFlammability(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aSide);}
	public final int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetFireSpreadSpeed ? ((IMTE_GetFireSpreadSpeed)aTileEntity).getFireSpreadSpeed(UT.Code.side(aSide), getMaterial().getCanBurn()) : getMaterial().getCanBurn() ? 150 : 0;}
	// was getFireSpreadSpeed(IBlockAccess,x,y,z,side) -> IBlockExtension.getFireSpreadSpeed(BlockState,BlockGetter,BlockPos,Direction) [IBlockExtension.java:721]. A direct delegate (types match 1:1).
	@Override public final int getFireSpreadSpeed(BlockState aState, BlockGetter aWorld, BlockPos aPos, Direction aSide) {return getFireSpreadSpeed(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aSide);}
	// F13: 1.7.10 isFireSource(World,x,y,z,side) -> IBlockExtension.isFireSource(BlockState,LevelReader,BlockPos,Direction) [IBlockExtension.java:736] EXISTS in neo,
	// but IMTE_IsFireSource has no implementors anywhere in the tree (verified with grep) -> a dead compile surface, no bridge added (nothing to wire).
	public final boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsFireSource && ((IMTE_IsFireSource)aTileEntity).isFireSource(UT.Code.side(aSide));}
	public final boolean canEntityDestroy(BlockGetter aWorld, int aX, int aY, int aZ, Entity aEntity) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return !(aTileEntity instanceof IMTE_CanEntityDestroy) || ((IMTE_CanEntityDestroy)aTileEntity).canEntityDestroy(aEntity);}
	// was canEntityDestroy(IBlockAccess,x,y,z,Entity) -> IBlockExtension.canEntityDestroy(BlockState,BlockGetter,BlockPos,Entity) [IBlockExtension.java:748]. A direct delegate (types match 1:1).
	@Override public final boolean canEntityDestroy(BlockState aState, BlockGetter aWorld, BlockPos aPos, Entity aEntity) {return canEntityDestroy(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aEntity);}
	@Override public final long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_OnToolClick ? ((IMTE_OnToolClick)aTileEntity).onToolClick(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ) : 0;}
	@Override public final OreDictMaterialStack getMaterialAtSide(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetMaterialAtSide?((IMTE_GetMaterialAtSide)aTileEntity).getMaterialAtSide(aSide):null;}
	@Override public final boolean removeMaterialFromSide(Level aWorld, int aX, int aY, int aZ, byte aSide, OreDictMaterialStack aMaterial) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_RemoveMaterialFromSide && ((IMTE_RemoveMaterialFromSide)aTileEntity).removeMaterialFromSide(aSide, aMaterial);}
	public final void dropBlockAsItemWithChance(Level aWorld, int aX, int aY, int aZ, int aMeta, float aChance, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, F, LAST_HARVESTING_PLAYER.get()); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	// was EnchantmentHelper.getSilkTouchModifier(Player)/getFortuneModifier(Player) (1.7.10) - removed in neo;
	// the real neo way: EnchantmentHelper.getEnchantmentLevel(Holder<Enchantment>,LivingEntity) via a Holder from RegistryAccess
	// (verified, EnchantmentHelper.java:292 + Enchantments.SILK_TOUCH/FORTUNE), the same approach already accepted and
	// approved in review in GT_API_Proxy.onBlockHarvestingEvent (GT_API_Proxy.java:1450-1451).
	public final void harvestBlock(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {if (aPlayer == null) aPlayer = LAST_HARVESTING_PLAYER.get(); aPlayer.awardStat(Stats.BLOCK_MINED.get(this), 1); /* was Stats.mineBlockStatArray[getIdFromBlock(this)] (1.7.10 int ID) -> Stats.BLOCK_MINED.get(Block) [Stats.java:12] + Player.awardStat [Player.java:1413] */ UT.Entities.exhaust(aPlayer, 0.025F); Holder<Enchantment> tSilkTouchHolder = aWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH); Holder<Enchantment> tFortuneHolder = aWorld.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE); boolean aSilkTouch = EnchantmentHelper.getEnchantmentLevel(tSilkTouchHolder, aPlayer) > 0; int aFortune = EnchantmentHelper.getEnchantmentLevel(tFortuneHolder, aPlayer); float aChance = 1.0F; BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) {ArrayListNoNulls<ItemStack> tList = ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, aSilkTouch); aChance = WD.fireBlockHarvesting(tList, aWorld, this, aX, aY, aZ, aMeta, aFortune, aChance, aSilkTouch, aPlayer); for (ItemStack tStack : tList) if (RNGSUS.nextFloat() <= aChance) WD.dropBlockAsItem(aWorld, aX, aY, aZ, tStack);}}
	public final ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aUnusableMetaData, int aFortune) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_GetDrops) return ((IMTE_GetDrops)aTileEntity).getDrops(aFortune, F); return ST.arraylist();}
	// F-loot (the IMTE_GetDrops channel, LIVE-DEFECTS.md audit): 1.7.10 harvestBlock(World,EntityPlayer,x,y,z,meta) WAS a real Forge/vanilla override
	// (oracle: @Override, gregtech6/.../MultiTileEntityBlock.java:261); neo has no method with this name/signature at all (0 hits across all 3
	// reference roots). The real drop hook when a player destroys a block is now Block.playerDestroy(Level,Player,BlockPos,BlockState,BlockEntity,
	// ItemStack) [Block.java:467], called from ServerPlayerGameMode.destroyBlock after a successful removeBlock (ServerPlayerGameMode.java:296) -
	// the sole real caller of drops on player block destruction. WITHOUT the bridge MTE blocks (IMTE_GetDrops implementors: the TE03/TE04 bases and
	// others) dropped NOTHING: the vanilla playerDestroy default calls dropResources via the loot table (BlockBehaviour.Properties.drops defaults to
	// "gregtech:blocks/<regname>", BlockBehaviour.java:982-984), and there's no such JSON for procedural MTE registrations (no datagen/loot_table) ->
	// an empty LootTable -> 0 items. aMeta below is unused by harvestBlock's body (same as in the 1.7.10 oracle) - passed as 0, the signature is kept.
	@Override public void playerDestroy(Level aWorld, Player aPlayer, BlockPos aPos, BlockState aState, BlockEntity aBlockEntity, ItemStack aDestroyedWith) {harvestBlock(aWorld, aPlayer, aPos.getX(), aPos.getY(), aPos.getZ(), 0);}
	// was aPlayer.level().getTileEntity(x,y,z) (1.7.10 World) -> the WD.te(...) center (the same approach as every other
	// TE lookup in this file), not a direct engine call.
	@Override public final ArrayList<String> getDebugInfo(Player aPlayer, int aX, int aY, int aZ, int aScanLevel) {BlockEntity aTileEntity = WD.te(aPlayer.level(), aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetDebugInfo ? ((IMTE_GetDebugInfo)aTileEntity).getDebugInfo(aScanLevel) : null;}
	// DIAGNOSIS (2026-07-21, not implemented - no clean 1:1 channel): 1.7.10 isSideSolid(IBlockAccess,x,y,z,side) WAS a direct
	// Forge override (a per-side boolean). Checked across all 3 roots: IBlockExtension.java does NOT contain isSideSolid at all (0 hits) -
	// neo computes "face solidity" purely from the VoxelShape (BlockBehaviour.isFaceSturdy(dir,SupportType), BlockBehaviour.java:876-881,
	// via SupportType.isSupporting -> state.getBlockSupportShape(level,pos) [BlockBehaviour.java:282-284], protected and overridable, BUT
	// this method's default = this.getCollisionShape(...) - the very same VoxelShape our getCollisionShape bridge above already returns (line ~295,
	// IMTE_GetCollisionBoundingBoxFromPool) - i.e. the channel is ALREADY INDIRECTLY covered by the block's OVERALL shape, not an independent per-side boolean.
	// IMTE_IsSideSolid gives an INDEPENDENT boolean per each of the 6 faces (7 implementors: Covers/Placeable/Sandwich/Stick/Rock/FluidSpring) -
	// synthesizing a single VoxelShape with per-face solidity that varies (merging full-face boxes on the "solid" faces) is NEW geometric
	// logic, not a 1:1 unpack+dispatch, so it's NOT implemented here (out of scope for this task, left to the orchestrator/ADR).
	public final boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsSideSolid?((IMTE_IsSideSolid)aTileEntity).isSideSolid(UT.Code.side(aSide)):mOpaque;}
	public final boolean isBeaconBase(BlockGetter aWorld, int aX, int aY, int aZ, int aBeaconX, int aBeaconY, int aBeaconZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_IsBeaconBase && ((IMTE_IsBeaconBase)aTileEntity).isBeaconBase(aBeaconX, aBeaconY, aBeaconZ);}
	public final int getLightOpacity(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetLightOpacity?((IMTE_GetLightOpacity)aTileEntity).getLightOpacity():mOpaque?LIGHT_OPACITY_MAX:LIGHT_OPACITY_NONE;}
	public final boolean isOpaqueCube() {return mOpaque;}
	public final boolean func_149730_j() {return mOpaque;}
	public final boolean renderAsNormalBlock() {return mOpaque || mNormalCube;}

	// F3 shade BRIDGE (the MTE hierarchy is separate from BlockBase, extends Block directly; the channel's analysis lives in BlockBase).
	// Machines/pipes/covers without mOpaque and without mNormalCube didn't shade neighbors in 1.7.10, but neo's default judges by
	// collision and would darken everything around them down to 0.2.
	@Override protected float getShadeBrightness(BlockState aState, BlockGetter aWorld, BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** 1.7.10 {@code Block.isBlockNormalCube()} ({@code Block.java:502-504}) — body 1:1, see {@code BlockBase}. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}
	public final boolean isNormalCube()  {return mNormalCube;}
	// was canProvidePower() -> BlockBehaviour.isSignalSource(BlockState) [BlockBehaviour.java:218]
	@Override protected final boolean isSignalSource(BlockState aState) {return !mNormalCube;}
	@Override public final Block getBlock() {return this;}
	public final String getUnlocalizedName() {return mNameInternal;}
	// LOCALIZATION (IMPLEMENTED): 1.7.10 vanilla Block.getLocalizedName() @Override removed from neo (no such method) → @Override dropped,
	// the method is functional via the localization CENTER LH.get(key) (like FluidGT/BlockBaseFluid.getLocalizedName). All adaptation to the engine
	// lives in one place (gregapi.lang.LanguageHandler). Not a stub. localization.csv=100% confirms it.
	public final String getLocalizedName() {return LH.get(mNameInternal);}
	public final String getHarvestTool(int aMeta) {return mTool;}
	public final boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public final int getHarvestLevel(int aMeta) {return (int)UT.Code.bind_(mHarvestLevelMinimum, mHarvestLevelMaximum, mHarvestLevelOffset + aMeta);}
	/** BUG-071: the value 1.7.10 held in THIS block's meta is the {@code mBlockMetaData} of the MTE class
	 *  (set at registration from {@code material.mToolQuality}: Loader_MultiTileEntities:895 onward).
	 *  In the port block meta isn't expressed by the state (IBlockExtendedMetaData:49 → 0), so we take it from the
	 *  class standing AT THIS POSITION: the BE knows its own registry and ID. The level formula stays SINGULAR (method above). */
	@Override public int getHarvestLevel(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getHarvestLevel(blockMetaDataAt(aWorld, aX, aY, aZ));
	}
	/** The 1.7.10 meta of the MTE block at a position: {@code mBlockMetaData} of its class (0 if there's no BE yet — like empty meta). */
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
	// was canHarvestBlock(EntityPlayer,meta) -> IBlockExtension.canHarvestBlock(BlockState,BlockGetter,BlockPos,Player)
	// [IBlockExtension.java:215], default EventHooks.doPlayerHarvestCheck(...) (itself the interface's own default) - the same
	// approach previously called through super (zero GT6-specific logic in this method, a pure pass-through point).
	@Override public final boolean canHarvestBlock(BlockState aState, BlockGetter aWorld, BlockPos aPos, Player aPlayer) {return EventHooks.doPlayerHarvestCheck(aPlayer, aState, aWorld, aPos);}
	public final boolean hasTileEntity(int aMeta) {return T;}
	public final boolean canSilkHarvest() {return F;}
	public final int getRenderBlockPass() {return ITexture.Util.MC_ALPHA_BLENDING?1:0;}
	public final BlockEntity createNewTileEntity(Level aWorld, int aMeta) {return null;}
	public final BlockEntity createTileEntity(Level aWorld, int aMeta) {return null;}
	// was EntityBlock.newBlockEntity(BlockPos,BlockState) (neo, EntityBlock.java:14, mandatory because the class implements
	// EntityBlock) - GT6's TE creation for MultiTileEntityBlock does NOT go through this (verified against the original: createNewTileEntity/
	// createTileEntity ALREADY return null in the 1.7.10 oracle too, MultiTileEntityBlock.java:282-283), but through
	// MultiTileEntityRegistry.getNewTileEntity (see receiveData above) - the network/explicit path.
	// BUG-117, a second carrier of the class "EntityBlock answers null to a DUMMY stub": null here is REACHABLE only
	// for a position WITHOUT a live entity (promotePendingBlockEntity, LevelChunk:373-381; with a live entity/stub the
	// stub isn't asked for) — and then the engine printed WARN:627, and the block stayed a ghost with no entity.
	// We return TileEntityLoaderStub — exactly what the MTE_TYPE factory returns for this same block on the LOAD path
	// (TileEntityBase01Root.createType:178): the contract is honored, WARN is impossible, and a stub with no captured NBT
	// degrades gracefully per the BUG-057 canon (reconstruction: no reg/id keys → air, no leftovers remain).
	@Override public final BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {return new gregapi.tileentity.base.TileEntityLoaderStub(aPos, aState);}
	// F-tick (the "BE ticks" seam, centered for the whole MTE class): in 1.7.10 the World itself ticked TileEntities with canUpdate()==true
	// (updateEntity every tick, BOTH sides — recompSrc World.updateEntities); in neo, BE ticks go ONLY through
	// EntityBlock.getTicker → BlockEntityTicker (Level.tickBlockEntities). Without this seam the whole TE03+ mechanism is dead:
	// onTick*, sendClientData sync to client (the place path), animations (mLidAngle), doBlockUpdate. canUpdate is 1:1 (TE01:572).
	// BUG-138: THE ENGINE DOES THE SELECTION, not our lambda after the selection. In 1.7.10 the TE itself carried the flag (the Forge hook canUpdate(),
	// original TileEntityBase01Root:440), and the author moved the non-ticking half of the hierarchy into the notick package; neo doesn't ask the question and puts into the list everyone this
	// method handed a ticker to (LevelChunk.updateBlockEntityTicker: ticker==null → removeBlockEntityTicker). While the ticker
	// was handed to everyone, the engine walked the world's list of 43,500 block entities every tick and asked shouldTickBlocksAt —
	// 8.46% of a live client's profile went into the selection, another 4.94% into calling the lambda, which immediately returned.
	// The tick flag is now declared by the block entity's type (the only discriminator the engine gives in this hook: the block and
	// the state are shared across all MTE) — see TileEntityBase01Root.MTE_TYPE_NOTICK.
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
	public final Identifier getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	public final Identifier getIcon(int aSide, int aMetaData) {return Textures.BlockIcons.CFOAM_HARDENED.getIcon(0);}
	// F3 render (deferred phase): 1.7.10 Block.getRenderType()/super.getRenderType() removed from neo (rendering is
	// data-driven via models/getRenderShape; getRenderType is never called in the neo pipeline — its callers are gone,
	// see the ToolCompat instanceof migration). super.getRenderType() -> -1 ("no custom render ID"); with a live GT6
	// client renderer it returns its mRenderID. The method is kept for the client F3 phase, the engine doesn't call it.
	public final int getRenderType() {return RendererBlockTextured.INSTANCE==null?-1:RendererBlockTextured.INSTANCE.mRenderID;}
	@Override public final IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T); return tTileEntity instanceof IRenderedBlockObject ? (IRenderedBlockObject)tTileEntity : null;}
	// was onBlockEventReceived(World,x,y,z,id,data) -> BlockBehaviour.triggerEvent(BlockState,Level,BlockPos,int,int)
	// [BlockBehaviour.java:206]; TileEntity.receiveClientEvent(id,data) -> BlockEntity.triggerEvent(int,int) [BlockEntity.java:270]
	@Override protected final boolean triggerEvent(BlockState aState, Level aWorld, BlockPos aPos, int aID, int aParam) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); return aTileEntity == null || aTileEntity.triggerEvent(aID, aParam);}
	// was getPlayerRelativeBlockHardness(EntityPlayer,World,x,y,z) -> BlockBehaviour.getDestroyProgress
	// (BlockState,Player,BlockGetter,BlockPos) [BlockBehaviour.java:340]
	// F-hardness (found from a live report "machines break by hand"): WAS — a TE gate on top of super.getDestroyProgress,
	// but super takes state.getDestroySpeed = Properties.destroyTime, which mkProps does NOT set (default 0) → division
	// by 0 → Infinity → ANY MTE broke INSTANTLY with anything. The 1.7.10 breaking chain: getPlayerRelativeBlockHardness
	// (original :298) on top of ForgeHooks.blockStrength with PER-TE hardness (getBlockHardness :299 → IMTE, mHardness from
	// NBT_HARDNESS registration). NOW 1:1: blockStrength from TE hardness (h<0 → 0-indestructible; digSpeed/h/(canHarvest
	// ?30:100) — hand on a machine-with-a-tool = /100 and full hardness, as in 1.7.10) → then the TE gate
	// IMTE_GetPlayerRelativeBlockHardness (TileEntityBase01Root:1108 allowInteraction → max(v,1e-4) | 0).
	private static long sLastDigZeroDiag = 0;
	@Override protected final float getDestroyProgress(BlockState aState, Player aPlayer, BlockGetter aWorld, BlockPos aPos) {
		BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T);
		float tHardness = aTileEntity instanceof IMTE_GetBlockHardness ? ((IMTE_GetBlockHardness)aTileEntity).getBlockHardness() : 1.0F;
		float tOriginal = WD.destroyProgress(tHardness, aPlayer, aState, aWorld, aPos); // the vanilla formula — the WD.destroyProgress CENTER
		float rResult = aTileEntity instanceof IMTE_GetPlayerRelativeBlockHardness ? ((IMTE_GetPlayerRelativeBlockHardness)aTileEntity).getPlayerRelativeBlockHardness(aPlayer, tOriginal) : tOriginal;
		// [GT6-DIG-ZERO] live-world diagnostics (always active, 1s throttle, server only): zero progress on a
		// NON-indestructible block = an anomaly in the chain (stats-null/unusable/quality<level/isMinable) — print the components
		// so a player's log names the cause. Doesn't fire in normal play (a hand gives >0).
		if (rResult <= 0.0F && tHardness >= 0 && aWorld instanceof Level tLevel && !tLevel.isClientSide() && System.currentTimeMillis() - sLastDigZeroDiag > 1000) {
			sLastDigZeroDiag = System.currentTimeMillis();
			ItemStack tHand = aPlayer.getMainHandItem();
			StringBuilder tSB = new StringBuilder("[GT6-DIG-ZERO] progress=0: block=").append(aTileEntity == null ? "BE-null" : aTileEntity.getClass().getSimpleName())
				.append(" hardness=").append(tHardness)
				.append(" playerSpeed=").append(aPlayer.getDestroySpeed(aState, aPos))
				.append(" harvestOK=").append(net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(aPlayer, aState, aWorld, aPos))
				.append(" hand=").append(tHand.isEmpty() ? "EMPTY" : String.valueOf(tHand.getItem()) + "#" + gregapi.util.ST.meta_(tHand));
			if (gregapi.util.ST.item_(tHand) instanceof gregapi.item.multiitem.MultiItemTool tTool) {
				gregapi.item.multiitem.tools.IToolStats tStats = tTool.getToolStats(tHand);
				tSB.append(" | tool: stats=").append(tStats == null ? "NULL(meta lookup!)" : tStats.getClass().getSimpleName())
					.append(" usable=").append(tTool.isItemStackUsable(tHand))
					.append(" digSpeed=").append(tTool.getDigSpeed(tHand, aState.getBlock(), 0))
					.append(" quality=").append(tStats == null ? "-" : String.valueOf(tStats.getBaseQuality() + tTool.getPrimaryMaterial(tHand).mToolQuality))
					// BUG-071: the level is taken from the POSITIONAL center. The old call with meta 0, after restoring
					// per-material-ness, would print zero for any machine — the diagnostics would mislead
					// exactly where it's read ("why doesn't it break").
					.append(" needed-level=").append(WD.harvestLevel(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()))
					.append(" needed-tool=").append(WD.harvestTool(aState.getBlock(), 0));
			}
			gregapi.data.CS.OUT.println(tSB.toString());
		}
		return rResult;
	}
	// F13: the engine-facing dynamic hardness (a player's mining speed) IS WIRED above via getDestroyProgress
	// (line ~469, the TE dispatcher IMTE_GetPlayerRelativeBlockHardness). This getBlockHardness is an internal GT6 helper
	// (TE hardness for its own logic), not a stub; the engine doesn't call it (it has getDestroyProgress).
	public final float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); return aTileEntity instanceof IMTE_GetBlockHardness?((IMTE_GetBlockHardness)aTileEntity).getBlockHardness():1.0F;}
	// was getExplosionResistance(Entity,World,x,y,z,expX,expY,expZ) -> IBlockExtension.getExplosionResistance
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]; Explosion.getDirectSourceEntity()/center() replace the lost parameters
	@Override public final float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); Vec3 aCenter = aExplosion.center(); return aTileEntity instanceof IMTE_GetExplosionResistance?((IMTE_GetExplosionResistance)aTileEntity).getExplosionResistance(aExplosion.getDirectSourceEntity(), aCenter.x, aCenter.y, aCenter.z):1.0F;}
	// was onNeighborChange(IBlockAccess,x,y,z,tileX,Y,Z) -> IBlockExtension.onNeighborChange(BlockState,LevelReader,BlockPos,BlockPos) [IBlockExtension.java:534]
	@Override public final void onNeighborChange(BlockState aState, LevelReader aWorld, BlockPos aPos, BlockPos aNeighbor) {BlockEntity aTileEntity = WD.te(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), T); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aNeighbor.getX(), aNeighbor.getY(), aNeighbor.getZ()); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborChange) ((IMTE_OnNeighborChange)aTileEntity).onNeighborChange(aWorld, aNeighbor.getX(), aNeighbor.getY(), aNeighbor.getZ());}
	// F17 item 13 "the hole follows the action": orphan sweeping (BE=null → remove the block) — SERVER ONLY. In 1.7.10 this
	// branch was dead on the client by the invariant "client TE exists in sync with the chunk" (the engine put the TE from chunk NBT);
	// in the neo port the client BE arrives ASYNCHRONOUSLY (GT6 packets, F17) → BE being absent on the client = a sync
	// state, not orphanhood. A neighbor update in this window removed the block LOCALLY on the client (the server still held
	// the block; the next server position update would bring it back) → a "wandering hole" in walls + sync packets dropping into client air.
	// ⚠️ The flag driving the orphan-sweep branch is an instance of the crash class "the flag switched carrier": WD.te == null in neo answers
	// THREE different states at once (the cell is really empty / the chunk isn't yet visible to the mod / the entity is still packed in pendingBlockEntities),
	// 1.7.10 knew no such distinction (the question and the removal went through one synchronous path). The flag was replaced with a provable one
	// (WD.teProvenAbsent: the chunk is visible AND there's really no BlockEntity in it); the branch itself, its side, and its action are unchanged.
	public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (!LOCK) {LOCK = T; if (aTileEntity instanceof ITileEntity) ((ITileEntity)aTileEntity).onAdjacentBlockChange(aX, aY, aZ); LOCK = F;} if (aTileEntity instanceof IMTE_OnNeighborBlockChange) ((IMTE_OnNeighborBlockChange)aTileEntity).onNeighborBlockChange(aWorld, aBlock); if (aTileEntity == null && !aWorld.isClientSide() && WD.teProvenAbsent(aWorld, aX, aY, aZ)) {traceOrphanSweep(aWorld, aX, aY, aZ); WD.set(aWorld, aX, aY, aZ, NB, 0, 3);}}

	/** The orphan-sweep branch fired LEGITIMATELY (the BE is provably absent). The event is rare and valuable for
	 *  diagnosing the source of orphans, so it's always visible: the first 20 and every 500th. Silence from the counter = no orphans are being born in the world. */
	private static final java.util.concurrent.atomic.AtomicLong sOrphanBlocksSwept = new java.util.concurrent.atomic.AtomicLong();
	private static void traceOrphanSweep(Level aWorld, int aX, int aY, int aZ) {
		long tN = sOrphanBlocksSwept.incrementAndGet();
		if (tN <= 20 || tN % 500 == 0) OUT.println("[GT6-MTEORPHAN] orphan block removed (BE provably absent) @" + aX + ", " + aY + ", " + aZ
			+ " chunk=[" + (aX >> 4) + ", " + (aZ >> 4) + "] tick=" + aWorld.getGameTime() + " total=" + tN);
	}
	// F-neighbor (the channel moved): 1.7.10 World.notifyBlocksOfNeighborChange called Block.onNeighborBlockChange; the neo entry point is
	// BlockBehaviour.neighborChanged. Bridge modeled on BlockFluidBaseGT:154; the GT6 channel (IMTE_OnNeighborBlockChange + orphan sweep) is intact.
	@Override protected void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, net.minecraft.world.level.redstone.Orientation aOrientation, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}
	@Override public final boolean usesRenderPass(int aRenderPass, ItemStack aStack) {return T;}
	@Override public final boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return T;}
	@Override public final void receiveBlockError(BlockGetter aWorld, int aX, int aY, int aZ, String aError) {BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (tTileEntity instanceof ITileEntity) {((ITileEntity)tTileEntity).setError(aError); WD.update(aWorld, aX, aY, aZ); UT.Sounds.play(SFX.GT_BEEP, 100, 1.0F, aX, aY, aZ);}}
	@Override public final void onWalkOver(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T); if (aTileEntity instanceof IMTE_OnWalkOver) ((IMTE_OnWalkOver)aTileEntity).onWalkOver(aEntity);}
}
