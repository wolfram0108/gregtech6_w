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

package gregapi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Explosion;

import gregapi.data.LH;
import gregapi.data.OP;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.IPlantable;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBase extends Block implements IBlockBase {
	public final String mNameInternal;
	/** The last bounds set; 1.7.10 mutated Block.mBoundingBox directly.
	 *  Real render use is deferred to a later client-side pass. */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	/** 1.7.10 was single-threaded, so mutating shared Block fields during render was safe.
	 *  neo meshes chunks on several threads sharing one Block, which raced on that field; fixed with a thread-local copy. */
	public static final ThreadLocal<boolean[]> RENDER_BOUNDS_CTX = ThreadLocal.withInitial(() -> new boolean[1]);
	private final ThreadLocal<float[]> mRenderBoundsTL = ThreadLocal.withInitial(() -> mRenderBounds.clone());
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		float[] tBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
		mRenderBoundsTL.set(tBounds);
		if (!RENDER_BOUNDS_CTX.get()[0]) mRenderBounds = tBounds;
	}
	/** Current render bounds for GT6BlockModel; reads the thread-local copy so each render thread
	 *  sees only its own pass's values, not another thread's. */
	public float[] getRenderBounds() {return mRenderBoundsTL.get();}
	// The 1.7.10 collision surface (addCollisionBoxesToList/getCollisionBoundingBoxFromPool) is gone from the engine,
	// so its defaults live here at the root and the subclass override chain keeps working as in the original.
	/** A 1:1 port of vanilla's default getCollisionBoundingBoxFromPool, using the static bounds plus position. */
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {
		float[] tB = mRenderBounds;
		return new AABB(aX+tB[0], aY+tB[1], aZ+tB[2], aX+tB[3], aY+tB[4], aZ+tB[5]);
	}
	/** A 1:1 port of vanilla's default addCollisionBoxesToList. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addCollisionBoxesToList(Level aWorld, int aX, int aY, int aZ, AABB aAABB, List aList, Entity aEntity) {
		AABB tBox = getCollisionBoundingBoxFromPool(aWorld, aX, aY, aZ);
		if (tBox != null && aAABB.intersects(tBox)) aList.add(tBox);
	}
	/** A 1:1 port of vanilla's default, a no-op since bounds are static here; subclasses override it. */
	public void setBlockBoundsBasedOnState(BlockGetter aWorld, int aX, int aY, int aZ) {/**/}
	/** Lets a block family whose shape depends on substate (bars, spikes) return it straight from
	 *  BlockState, since neo builds its shape cache once against an empty world with no real position to read meta from.
	 *  @param aCollision true for the collision shape, false for the outline/selection box.
	 *  @return the local-space shape, or null if this family has no state-based shape. */
	protected net.minecraft.world.phys.shapes.VoxelShape shapeFromState(BlockState aState, boolean aCollision) {return null;}

	// Collision bridge: a null pool means a passable block, noCollission blocks must stay passable, and the
	// state-cache path (no world) reads the state shape first, then the static bounds, as 1.7.10 read mBoundingBox.
	@Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		if (!hasCollision) return net.minecraft.world.phys.shapes.Shapes.empty();
		net.minecraft.world.phys.shapes.VoxelShape tFromState = shapeFromState(aState, T);
		if (tFromState != null) return tFromState;
		if (aWorld instanceof Level tLevel) {
			List<AABB> tList = new java.util.ArrayList<>();
			addCollisionBoxesToList(tLevel, aPos.getX(), aPos.getY(), aPos.getZ(), new AABB(aPos.getX()-1, aPos.getY()-1, aPos.getZ()-1, aPos.getX()+2, aPos.getY()+2, aPos.getZ()+2), tList, aContext instanceof net.minecraft.world.phys.shapes.EntityCollisionContext tEntityContext ? tEntityContext.getEntity() : null);
			net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.empty();
			for (AABB tBox : tList) if (tBox != null) rShape = net.minecraft.world.phys.shapes.Shapes.or(rShape, net.minecraft.world.phys.shapes.Shapes.create(tBox.move(-aPos.getX(), -aPos.getY(), -aPos.getZ())));
			return rShape;
		}
		float[] tB = mRenderBounds;
		return tB[0] <= 0 && tB[1] <= 0 && tB[2] <= 0 && tB[3] >= 1 && tB[4] >= 1 && tB[5] >= 1 ? super.getCollisionShape(aState, aWorld, aPos, aContext) : net.minecraft.world.phys.shapes.Shapes.create(new AABB(tB[0], tB[1], tB[2], tB[3], tB[4], tB[5]));
	}
	// Outline bridge in the 1.7.10 collisionRayTrace order: state bounds first, static bounds second; an empty
	// result (render-thread bounds race) becomes a full cube, because an empty outline makes the block untargetable.
	@Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {
		// The state shape is the only path valid both in the state cache (no world) and in the live world;
		// families needing world logic (bars: same block in hand gives a full cube) handle it inside their hook.
		net.minecraft.world.phys.shapes.VoxelShape tFromState = shapeFromState(aState, F);
		if (tFromState != null) return tFromState.isEmpty() ? net.minecraft.world.phys.shapes.Shapes.block() : tFromState;
		try { setBlockBoundsBasedOnState(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()); } catch (Throwable e) {/* foreign BlockGetter or a race; fall through to the static bounds below */}
		float[] tB = mRenderBounds;
		if (tB[0] <= 0 && tB[1] <= 0 && tB[2] <= 0 && tB[3] >= 1 && tB[4] >= 1 && tB[5] >= 1) return super.getShape(aState, aWorld, aPos, aContext);
		net.minecraft.world.phys.shapes.VoxelShape rShape = net.minecraft.world.phys.shapes.Shapes.create(new AABB(tB[0], tB[1], tB[2], tB[3], tB[4], tB[5]));
		return rShape.isEmpty() ? net.minecraft.world.phys.shapes.Shapes.block() : rShape;
	}
	/** 1.7.10's render dispatcher used getRenderType()==0 for a standard cube and a PILLAR value for
	 *  log/beam/bale blocks, which GT6BlockModel still reads to rotate UVs along the stacking axis. */
	public int getRenderType() {return 0;}
	/** 1.7.10 mutated light emission directly; neo's lightLevel function is set at construction but
	 *  evaluated lazily after registration, by which point a subclass's setLightLevel has already run. */
	protected float mLightLevel = 0.0F;
	public void setLightLevel(float aLightLevel) {mLightLevel = aLightLevel;}
	/** Bridges to neo's lightLevel function, reading the instance's mLightLevel via state.getBlock() at initCache time. */
	private static int lightOf(net.minecraft.world.level.block.state.BlockState aState) {return aState.getBlock() instanceof BlockBase b ? (int)(15.0F * b.mLightLevel) : 0;}

	/** Stored on the block itself because neo removed WD.getMaterial(Block). */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}
	// Whether a tool is required for the drop is decided by the material, as in 1.7.10 canHarvestBlock:
	// rock needs a pickaxe, wood/cloth/dirt drop by hand; without the gate the whole family dropped by hand.
	private static net.minecraft.world.level.block.state.BlockBehaviour.Properties mkProps(String aNameInternal, Material aMaterial, SoundType aSoundType) {
		net.minecraft.world.level.block.state.BlockBehaviour.Properties p = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().sound(aSoundType).lightLevel(BlockBase::lightOf);
		if (aMaterial != null && !aMaterial.isToolNotRequired()) p = p.requiresCorrectToolForDrops();
		p = mapColorOf(p, aMaterial);
		return p;
	}

	/** 1.7.10 read map color straight from the block's material; neo defaults to skipping the block
	 *  entirely, erasing GT6's ores and fluids from maps; restored here via the same bridge MTE blocks already use. */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties mapColorOf(net.minecraft.world.level.block.state.BlockBehaviour.Properties aProps, Material aMaterial) {
		if (aMaterial == null) return aProps;
		gregapi.block.MapColor tColor = aMaterial.getMaterialMapColor();
		return tColor == null ? aProps : aProps.mapColor(tColor.toNeo());
	}
	public BlockBase(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType) {
		// The engine block is immutable: sound, light and id go into Properties before super; per-meta hardness
		// and map color vary, so they stay dynamic overrides, and the name comes from the registry (ST.register).
		super(mkProps(aNameInternal, aMaterial, aSoundType));
		mMaterial = aMaterial;
		mNameInternal = aNameInternal;
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.BLOCK); // matches 1.7.10's setCreativeTab(tabBlock); last write wins, so a subclass can override it
		// Registers only the BlockItem here, through a supplier, once the ITEMS registry event is open;
		// the block itself is registered lazily elsewhere.
		final Class<? extends BlockItem> tItemClass = aItemClass==null?gregapi.block.ItemBlockBase.class:aItemClass;
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> (BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, T, this));
		LH.add(mNameInternal+"."+W, "Any Sub-Block of this one");
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public String getHarvestTool(int aMeta) {return TOOL_pickaxe;}
	public int getHarvestLevel(int aMeta) {return 0;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return getHarvestTool(aMeta).equals(aType);}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return T;}
	public boolean renderAsNormalBlock() {return T;}
	public boolean isOpaqueCube() {return T;}
	public boolean func_149730_j() {return isOpaqueCube();}
	// 1.7.10's isOpaqueCube() channel became orphaned because neo instead culls neighbor faces via an
	// occlusion shape defaulting to a full cube, hiding whatever sits behind non-opaque blocks; this bridges the two.
	@Override public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos) {
		return isOpaqueCube() ? super.getOcclusionShape(aState, aWorld, aPos) : net.minecraft.world.phys.shapes.Shapes.empty();
	}
	@Override public boolean propagatesSkylightDown(BlockState aState, net.minecraft.world.level.BlockGetter aWorld, BlockPos aPos) {
		return !isOpaqueCube() || super.propagatesSkylightDown(aState, aWorld, aPos);
	}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return isSideSolid(WD.meta(aWorld, aX, aY, aZ), UT.Code.side(aDirection));}
	// neo's skipRendering has inverted semantics from 1.7.10's shouldSideBeRendered and drops the
	// world/position arguments; the position-independent branch ports with the inversion, the rest uses vanilla's default.
	/** neo asks face visibility through a pair of BlockState with no world or coordinates, so
	 *  subclasses that kept the 1.7.10 signature had no caller; placed at the hierarchy root so every descendant inherits it. */
	public boolean shouldSideBeRendered(BlockState aState, BlockState aNeighbor, byte aSide) {return T;}

	@Override public boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {if (!shouldSideBeRendered(aState, aNeighbor, UT.Code.side(aDir))) return T; return isOpaqueCube() ? WD.visOpq(aNeighbor.getBlock()) : super.skipRendering(aState, aNeighbor, aDir);}
	public int damageDropped(int aMeta) {return aMeta;}
	public int quantityDropped(int aMeta, int aFortune, Random aRandom) {return 1;}
	public ItemStack createStackedBlock(int aMeta) {return ST.make(this, 1, damageDropped(aMeta));}

	// neo drives everything shown about a block (tooltip, icon, middle-click) through getCloneItemStack,
	// whose default ignores metadata; this wires it to the already-ported subtype-aware body, once at the hierarchy root.
	@Override public ItemStack getCloneItemStack(BlockState aState, net.minecraft.world.phys.HitResult aTarget, net.minecraft.world.level.BlockGetter aLevel, net.minecraft.core.BlockPos aPos, Player aPlayer) {
		ItemStack rStack = createStackedBlock(WD.meta(aLevel, aPos.getX(), aPos.getY(), aPos.getZ()));
		return rStack == null || rStack.isEmpty() ? super.getCloneItemStack(aState, aTarget, aLevel, aPos, aPlayer) : rStack;
	}

	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public int getLightOpacity() {return LIGHT_OPACITY_MAX;}

	// Root of the BlockBase light-opacity bridge (shared by sealable/meta/tree/metatype/glass/leaves/etc.): 1.7.10 asked
	// the block for getLightOpacity(), but neo reads it from BlockState instead, losing GT6's values without this bridge.
	@Override public int getLightBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.lightDampening(getLightOpacity());}

	// neo asks the same ambient-occlusion value through a different underlying flag than 1.7.10's
	// isBlockNormalCube, which diverges on GT6's full-collision non-normal-render blocks like glass; this bridges the flags.
	@Override public float getShadeBrightness(BlockState aState, BlockGetter aWorld, BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** 1.7.10's flag for shading neighbors, ported body-for-body; renderAsNormalBlock() stays virtual
	 *  so subclass overrides (glass, leaves, paths, slabs) are honored exactly as in the original. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}
	public Item getItemDropped(int aMeta, Random aRandom, int aFortune) {return Item.byBlock(this);}

	// Blocks without a loot table got no drops under neo's default getDrops(loot), so this bridges
	// to GT6's own getDrops hook, reading metadata from the state snapshot since the block is already air by this point.
	@Override public List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		net.minecraft.server.level.ServerLevel tLevel = aParams.getLevel();
		net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
		if (tOrigin == null) return super.getDrops(aState, aParams);
		// Explosion-drop gating goes through the single WD.explosionDropDenied center; no duplicate copies remain.
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList();
		int tX = net.minecraft.util.Mth.floor(tOrigin.x), tY = net.minecraft.util.Mth.floor(tOrigin.y), tZ = net.minecraft.util.Mth.floor(tOrigin.z);
		net.minecraft.world.entity.Entity tEntity = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY);
		int tFortune = WD.lootFortune(aParams);
		// Hay-bale variants used to drop the wrong variant because reading metadata from the world
		// returned 0 for an already-air block; this reads it from the state snapshot instead, the same fix as above.
		ArrayList<ItemStack> rDrops = getDrops(tLevel, tX, tY, tZ, WD.meta(aState), tFortune);
		if (rDrops == null) return java.util.Collections.emptyList();
		// 1.20.1: GT6's own blocks have no loot tables, so the global GT6BlockDropsModifier can't see them --
		// drop handling is called from the same center directly instead, one rule for both paths.
		gregapi.GT_API_Proxy.processBlockDrops(rDrops, tLevel, new BlockPos(tX, tY, tZ), aState, tEntity);
		return rDrops;
	}
	// 1.7.10's default drop behavior: quantityDropped copies of getItemDropped/damageDropped, so a
	// block with no override simply drops itself.
	public ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) {
		ArrayList<ItemStack> rDrops = ST.arraylist();
		Item tItem = getItemDropped(aMeta, RNGSUS, aFortune);
		if (tItem != null) for (int i = 0, j = quantityDropped(aMeta, aFortune, RNGSUS); i < j; i++) rDrops.add(ST.make(tItem, 1, damageDropped(aMeta)));
		return rDrops;
	}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(Object aIconRegister) {/**/}
	public boolean canSustainPlant(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide, IPlantable aPlant) {return F;}
	// Bridges the Forge soil hook into the 1.7.10-shaped override above: F for the whole family 1:1 with the
	// original (:90), Grass/Diggable/Stones refine it virtually. 1.20.1 keeps the 1.7.10 hook shape (IPlantable,
	// boolean — IForgeBlock.java:354) and vanilla plants implement IPlantable, so no plant adapter is needed.
	@Override public boolean canSustainPlant(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos, Direction aSide, IPlantable aPlant) {
		return canSustainPlant(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aSide, aPlant);
	}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {byte aMeta = WD.meta(aWorld, aX, aY, aZ); return canCreatureSpawn(aMeta) && isSideSolid(aMeta, SIDE_TOP);}
	public boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {return isFireSource(WD.meta(aWorld, aX, aY, aZ));}
	public boolean isFlammable(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return isFlammable(WD.meta(aWorld, aX, aY, aZ));}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFlammability(WD.meta(aWorld, aX, aY, aZ));}
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return getFireSpreadSpeed(WD.meta(aWorld, aX, aY, aZ));}
	// neo asks explosion resistance through IBlockExtension.getExplosionResistance instead of the old Entity/World/position
	// signature.
	// (BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]
	@Override public float getExplosionResistance(BlockState aState, BlockGetter aWorld, BlockPos aPos, Explosion aExplosion) {return getExplosionResistance(WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()));}
	public float getExplosionResistance(Entity aEntity) {return getExplosionResistance((byte)0);}
	// getDestroySpeed only returns the baked, immutable destroy time, but getDestroyProgress is
	// still an overridable hook, so GT6's own getBlockHardness is wired in there instead, by the vanilla formula.
	@Override public float getDestroyProgress(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {
		if (!(aWorld instanceof Level tLevel)) return super.getDestroyProgress(aState, aPlayer, aWorld, aPos);
		return WD.destroyProgress(getBlockHardness(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()), aPlayer, aState, aWorld, aPos); // the vanilla formula lives in the single center WD.destroyProgress
	}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 1;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return 1;}
	public final void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {if (useGravity(WD.meta(aWorld, aX, aY, aZ))) aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 2); onNeighborBlockChange2(aWorld, aX, aY, aZ, aBlock);}
	// neo signals neighbor changes through BlockBehaviour.neighborChanged instead of the old
	// onNeighborBlockChange, bridged the same way as BlockFluidBaseGT.
	@Override public void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, BlockPos aFromPos, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}
	// neo calls onPlace(BlockState, Level, BlockPos, BlockState, boolean) instead of the old onBlockAdded.
	@Override public final void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {if (useGravity(WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()))) aWorld.scheduleTick(aPos, this, 2); onBlockAdded2(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());}
	public ResourceLocation getIcon(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {return getIcon(aSide, WD.meta(aWorld, aX, aY, aZ));}
	// 1.7.10's inherited vanilla Block.getIcon(int,int) is gone entirely in 26.1.2; restored locally here
	// (same trick as BlockBaseMeta.getIcon) so this call site and subclass overrides share one contract.
	/** BlockBase itself never had a body here; 1.7.10's version came from vanilla Block's own blockIcon field,
	 *  which neo has none of, so null means "no icon channel wired" and the caller falls back to the baked path. */
	public ResourceLocation getIcon(int aSide, int aMeta) {return null;}
	
	@Override public String name(byte aMeta) {return aMeta == W ? mNameInternal : mNameInternal + "." + aMeta;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return isSideSolid(aMeta, aSide);}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {/**/}
	@Override public float getExplosionResistance(byte aMeta) {return 10.0F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public int getItemStackLimit(ItemStack aStack) {return UT.Code.bindStack(OP.block.mDefaultStackSize);}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	public boolean checkNoEntityCollision(Level aWorld, int aX, int aY, int aZ, byte aMeta, Entity aExceptThisOne) {return WD.noEntityCollision(aWorld, new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1), aExceptThisOne);}

	// Forge's canReplace/onBlockPlaced hooks were removed since neo rebuilt placement around
	// BlockPlaceContext, so their old defaults are reproduced here as plain GT6 helpers instead.
	public boolean canReplace(Level aWorld, int aX, int aY, int aZ, int aSide, ItemStack aStack) {return T;}
	public byte onBlockPlaced(Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, byte aMeta) {return aMeta;}
	public boolean isSideSolid(int aMeta, byte aSide) {return T;}
	public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {/**/}
	public void onNeighborBlockChange2(Level aWorld, int aX, int aY, int aZ, Block aBlock) {/**/}
	public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {/**/}
	
	// updateTick was an orphan under its old 1.7.10 signature, so scheduled ticks (leaf decay, sapling
	// growth, gravity, moss) never actually ran; this wires it to neo's tick() instead.
	@Override public void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), UT.Code.random(aRandom)); // the RandomSource-to-Random converter lives in the single center UT.Code.random
	}
	// neo split the single 1.7.10 updateTick into separate scheduled and random ticks, and the random
	// one defaulted to empty, so saplings never grew; merging them back into one restores the original behavior.
	@Override public void randomTick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		tick(aState, aWorld, aPos, aRandom);
	}
	public final void updateTick(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (aWorld.isClientSide() || checkGravity(aWorld, aX, aY, aZ)) return;
		updateTick2(aWorld, aX, aY, aZ, aRandom);
	}
	
	public boolean checkGravity(Level aWorld, int aX, int aY, int aZ) {
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		if (aY > WD.minY(aWorld) && useGravity(aMeta) && FallingBlock.isFree(WD.block(aWorld, aX, aY - 1, aZ).defaultBlockState())) { // the world floor is getMinY() here, not 0, since negative Y sections exist now
			// The old fallInstantly static field no longer exists on the engine, so it defaults to true here;
			// checkChunksExist is replaced by isAreaLoaded, the same technique already used elsewhere.
			if (T && aWorld.isAreaLoaded(new BlockPos(aX, aY, aZ), 32)) {
				// FallingBlockEntity's constructor is private now, so this uses FallingBlockEntity.fall(...), the
				// only public neo spawn path, which replaces the block itself before spawning the entity.
				if (!aWorld.isClientSide()) FallingBlockEntity.fall(aWorld, new BlockPos(aX, aY, aZ), aWorld.getBlockState(new BlockPos(aX, aY, aZ)));
			} else {
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				while (FallingBlock.isFree(WD.block(aWorld, aX, aY-1, aZ).defaultBlockState()) && aY > 0) --aY;
				if (aY > 0) WD.set(aWorld, aX, aY, aZ, this, aMeta, 2);
			}
			return T;
		}
		return F;
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		if (!WD.replaceable(WD.block(aWorld, aX, aY, aZ), aWorld, aX, aY, aZ)) return F;
		if (!canReplace(aWorld, aX, aY, aZ, aSide, aStack)) return F;
		byte aMeta = UT.Code.bind4(aItem.getMetadata(ST.meta(aStack)));
		if (!checkNoEntityCollision(aWorld, aX, aY, aZ, aMeta, null)) return F;
		// canPlaceEntityOnSide is restored through the single center WD.canPlaceEntityOnSide, since the
		// Forge hook itself was removed but the ability it provided still needs a home.
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == WD.maxY(aWorld) && getMaterial().isSolid()) /* the world ceiling now comes from the single Y-scale center, not a hardcoded 255 */ || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		// Placement finishes through the overridable aItem.placeBlockAt channel; calling WD.set directly
		// here used to bypass it, so a slab's variant-selection override never ran and it always placed facing down.
		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aMeta))) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
	
	public final int quantityDropped(Random aRandom) {return quantityDropped(0, 0, aRandom);}

	/** Harvest permission is judged by the center WD.canHarvestBlock; this is only the call site.
	 *  The rule moved here from PlayerEvent.HarvestCheck, which in 1.20.1 carries neither world nor position. */
	@Override public boolean canHarvestBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer) {
		return gregapi.util.WD.canHarvestBlock(aState, aWorld, aPos, aPlayer);
	}

}
