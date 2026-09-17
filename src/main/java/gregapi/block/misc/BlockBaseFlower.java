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

import gregapi.api.Optional;
import gregapi.block.IBlockBase;
import gregapi.block.ItemBlockBase;
import gregapi.block.Material;
import gregapi.compat.galacticraft.IBlockSealable;
import gregapi.data.MD;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock;
import mods.railcraft.common.carts.EntityTunnelBore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.PlantType;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
	@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock", modid = ModIDs.GC)
})
public abstract class BlockBaseFlower extends FlowerBlock implements IBlockBase, IBlockSealable, IOxygenReliantBlock, BonemealableBlock, gregapi.render.IRenderedCross, gregapi.block.IBlockExtendedMetaData {
	public final String mNameInternal;
	public IIconContainer[] mIcons;
	/** For Creative Subsets, not actually important. */
	private final byte mMaxMeta;

	/** The flower's ore-indicator variant lives in a synced BlockState property rather than a tile
	 *  entity, routed through WD.set/WD.meta, and GT6BlockModel reads it to draw the cross model. */
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty META = net.minecraft.world.level.block.state.properties.IntegerProperty.create("meta", 0, 15);
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> aBuilder) {super.createBlockStateDefinition(aBuilder); aBuilder.add(META);}
	/** The Material bridge isn't extended to classes outside BlockBase, so this keeps its own
	 *  mMaterial/getMaterial() instead of a new shared abstraction. */
	protected final Material mMaterial = Material.plants;
	public Material getMaterial() {return mMaterial;}

	/** @param aSpeed is usually 0.4F */
	public BlockBaseFlower(Class<? extends ItemBlockBase> aItemClass, String aNameInternal, long aMaxMeta, IIconContainer[] aIcons) {
		// FlowerBlock's constructor now requires a stew effect, so decorative GT6 flowers pass EMPTY; noCollision() is also
		// required, or occlusion treats the flower as solid and kills the grass underneath it, as happened before this fix.
		super(net.minecraft.world.effect.MobEffects.SATURATION, 0, gregapi.block.BlockBase.mapColorOf(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission().sound(net.minecraft.world.level.block.SoundType.GRASS), gregapi.block.Material.plants));
		registerDefaultState(getStateDefinition().any().setValue(META, 0)); // defaults to META=0
		mMaxMeta = (byte)(UT.Code.bind4(aMaxMeta-1)+1);
		mIcons = aIcons;
		mNameInternal = aNameInternal;
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.DECORATIONS);
		// Only the BlockItem is registered here; the block itself registers lazily at its call site.
		final Class<? extends net.minecraft.world.item.BlockItem> tItemClass = aItemClass==null?gregapi.block.ItemBlockBase.class:aItemClass;
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, mNameInternal, () -> (net.minecraft.world.item.BlockItem)gregapi.util.UT.Reflection.callConstructor(tItemClass, 0, null, gregapi.data.CS.T, this));
		if (MD.RC.mLoaded) try {EntityTunnelBore.addMineableBlock(this);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (COMPAT_FR != null) gregapi.GT_API.deferItemInit(() -> COMPAT_FR.addToBackpacks("forester", ST.make(this, 1, W)));
	}
	
	public final String getUnlocalizedName() {return mNameInternal;}
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {/* shape is deferred core-wide; FlowerBlock carries its own neo shape instead */}
	@Override public float[] getRenderBounds() {return null;/* flowers use cross rendering and store no bounds */}
	// GT6 flowers are decorative, so bonemeal has no effect, the same as vanilla's single flowers.
	@Override public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader aWorld, BlockPos aPos, BlockState aState, boolean aIsClient) {return F;}
	@Override public boolean isBonemealSuccess(net.minecraft.world.level.Level aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {return F;}
	@Override public void performBonemeal(net.minecraft.server.level.ServerLevel aWorld, net.minecraft.util.RandomSource aRandom, BlockPos aPos, BlockState aState) {/**/}
	@Override public String name(byte aMeta) {return mNameInternal + "." + aMeta;}
	public String getLocalizedName() {return gregapi.lang.LanguageHandler.get(mNameInternal);}
	public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return 0;}
	// neo asks explosion resistance through IBlockExtension instead of the old Entity-based overload; the original body
	// ignored every parameter anyway, returning a constant 0.
	@Override public float getExplosionResistance(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, BlockPos aPos, net.minecraft.world.level.Explosion aExplosion) {return 0;}
	public float getExplosionResistance(Entity aEntity) {return 0;}
	public String getHarvestTool(int aMeta) {return TOOL_sword;}
	public int getHarvestLevel(int aMeta) {return 0;}
	public boolean canSilkHarvest() {return canSilkHarvest((byte)0);}
	public boolean canSilkHarvest(Level aWorld, Player aPlayer, int aX, int aY, int aZ, int aMeta) {return canSilkHarvest(UT.Code.bind4(aMeta));}
	public boolean isToolEffective(String aType, int aMeta) {return T;}
	public boolean canBeReplacedByLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return T;}
	public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	public int damageDropped(int aMeta) {return aMeta;}
	public int quantityDropped(Random par1Random) {return 1;}
	public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ);}
	public int getLightOpacity() {return LIGHT_OPACITY_NONE;}

	// Its own copy of the light-opacity bridge, since flowers extend vanilla FlowerBlock, not BlockBase.
	@Override public int getLightBlock(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.lightDampening(getLightOpacity());}

	// Its own copy of the shade bridge, for the same reason: flowers extend vanilla FlowerBlock, not BlockBase.
	@Override public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aWorld, net.minecraft.core.BlockPos aPos) {return gregapi.data.CS.shadeBrightness(isBlockNormalCube());}

	/** Body 1:1 with 1.7.10's Block.isBlockNormalCube; see BlockBase for the same method. */
	public boolean isBlockNormalCube() {return mMaterial.blocksMovement() && renderAsNormalBlock();}

	/** 1.7.10 got this false from vanilla BlockBush itself; neo's ancestor has no such method,
	 *  so it is set explicitly here instead, 1:1. */
	public boolean renderAsNormalBlock() {return F;}
	public Item getItemDropped(int par1, Random aRandom, int par3) {return Item.byBlock(this);}
	public Item getItem(Level aWorld, int aX, int aY, int aZ) {return Item.byBlock(this);}
	public void registerBlockIcons(Object aIconRegister) {/**/}
	public boolean canCreatureSpawn(MobCategory type, BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {for (int i = 0; i < maxMeta(); i++) aList.add(ST.make(aItem, 1, i));}
	public boolean isSealed(Level aWorld, int aX, int aY, int aZ, Direction aDirection) {return F;}
	@Override public Block getBlock() {return this;}
	@Override public byte maxMeta() {return mMaxMeta;}
	public ResourceLocation getIcon(int aSide, int aMeta) {return mIcons[aMeta % mIcons.length].getIcon(0);}

	// Uses the shared IBlockExtendedMetaData defaults instead of a Level-gated local setter, since the default correctly
	// writes both the live region and worldgen's ChunkAccess; aWorld==null means an item render, aX carrying the stack's meta.
	@Override public ResourceLocation getCrossIcon(BlockGetter aWorld, int aX, int aY, int aZ) {
		if (mIcons == null || mIcons.length == 0) return null;
		IIconContainer tIcon = mIcons[UT.Code.bind4(aWorld == null ? aX : WD.meta(aWorld, aX, aY, aZ)) % mIcons.length];
		return tIcon == null ? null : tIcon.getIcon(0);
	}
	public void onOxygenAdded(Level aWorld, int aX, int aY, int aZ) {/**/}
	public void onOxygenRemoved(Level aWorld, int aX, int aY, int aZ) {if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, NB, 0, 3); return;}}
	
	@Override public void addInformation(ItemStack aStack, byte aMeta, Player aPlayer, List<String> aList, boolean aF3_H) {/**/}
	@Override public float getExplosionResistance(byte aMeta) {return 0;}
	@Override public boolean useGravity(byte aMeta) {return F;}
	@Override public boolean doesWalkSpeed(byte aMeta) {return F;}
	@Override public boolean doesPistonPush(byte aMeta) {return F;}
	@Override public boolean canSilkHarvest(byte aMeta) {return T;}
	@Override public boolean canCreatureSpawn(byte aMeta) {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public boolean isFlammable(byte aMeta) {return getFlammability(aMeta) > 0;}
	@Override public boolean isFireSource(byte aMeta) {return F;}
	@Override public int getFlammability(byte aMeta) {return 0;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 0;}
	@Override public int getItemStackLimit(ItemStack aStack) {return 64;}
	@Override public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	
	// The real IPlantable signature is (BlockGetter,BlockPos), not the old (BlockGetter,x,y,z) shim,
	// which was never actually an override and silently never fired. getPlantMetadata is removed: the real interface has none.
	@Override public PlantType getPlantType(BlockGetter aWorld, BlockPos aPos) {return PlantType.PLAINS;}
	@Override public BlockState getPlant(BlockGetter aWorld, BlockPos aPos) {BlockState tState = aWorld.getBlockState(aPos); return tState.getBlock() != this ? defaultBlockState() : tState;}
	// Goes through the shared WD.canSustainPlant center; an earlier copy collapsed its tri-state
	// result to always true, so flowers stood on stone or even air and never got removed for lacking support.
	public boolean canBlockStay(Level aWorld, int aX, int aY, int aZ) {return WD.oxygen(aWorld, aX, aY, aZ) && WD.canSustainPlant(aWorld, aX, aY - 1, aZ, Direction.UP, Blocks.DANDELION);}
	public boolean func_149851_a(Level aWorld, int aX, int aY, int aZ, boolean aIsRemote) {return T;}
	public boolean func_149852_a(Level aWorld, Random aRandom, int aX, int aY, int aZ) {return T;}
	public void func_149853_b(Level aWorld, Random aRandom, int aX, int aY, int aZ) {ST.drop(aWorld, aX+0.5, aY+0.5, aZ+0.5, this, 1, WD.meta(aWorld, aX, aY, aZ));}
	// Reintroduced as a plain generic hook, the same technique as its siblings, since neo removed
	// this vanilla override point entirely; the default still just returns the meta unchanged, as before.
	public int onBlockPlaced(Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, int aMeta) {return aMeta;}
	
	// Bridged separately here since this hierarchy sits on vanilla FlowerBlock and has none of BlockBase's drop bridges;
	// without it, flowers removed by decay or lost support dropped nothing at all, since GT6 has no loot tables.
	@Override public java.util.List<ItemStack> getDrops(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aParams) {
		net.minecraft.world.phys.Vec3 tOrigin = aParams.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
		if (tOrigin == null) return super.getDrops(aState, aParams);
		if (WD.explosionDropDenied(aParams)) return java.util.Collections.emptyList();
		java.util.ArrayList<ItemStack> rDrops = ST.arraylist();
		int tMeta = WD.meta(aState);
		Item tItem = getItemDropped(tMeta, RNGSUS, 0);
		if (tItem != null) for (int i = 0, j = quantityDropped(RNGSUS); i < j; i++) rDrops.add(ST.make(tItem, 1, damageDropped(tMeta)));
		return rDrops;
	}

	// Wires two neo hooks to the same 1.7.10 removal rule: canSurvive (called on neighbor updates, reading canBlockStay) and
	// randomTick (catching oxygen changes with no neighbor update at all), matching where the original was attached.
	@Override public boolean canSurvive(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.LevelReader aWorld, BlockPos aPos) {
		return aWorld instanceof Level tLevel ? canBlockStay(tLevel, aPos.getX(), aPos.getY(), aPos.getZ()) : super.canSurvive(aState, aWorld, aPos);
	}
	@Override public boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState aState) {return T;}
	@Override public void randomTick(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		checkAndDropBlock(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
	}
	public void checkAndDropBlock(Level aWorld, int aX, int aY, int aZ) {
		if (canBlockStay(aWorld, aX, aY, aZ)) return;
		WD.dropBlockAsItem(aWorld, aX, aY, aZ, WD.meta(aWorld, aX, aY, aZ), 0);
		WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
	}
	
	@Override public boolean onItemUseFirst(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return F;}
	
	@Override
	public boolean onItemUse(ItemBlockBase aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		
		// Potting a GT6 flower is a no-op, a deliberate degradation: neo represents a filled pot as its own separate block per
		// plant, and registering that many custom potted blocks for a decorative feature was judged disproportionate.
		if (tBlock == Blocks.FLOWER_POT) return F;

		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		// Was tBlock != Blocks.tallgrass (one 1.7.10 class for grass/fern); neo split it into GRASS/FERN, both
		// instanceof TallGrassBlock -- the same identity check, expressed by type instead of by a single constant.
		} else if (tBlock != Blocks.VINE && !(tBlock instanceof net.minecraft.world.level.block.TallGrassBlock) && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}

		// Restored through the same shared center WD.canPlaceEntityOnSide as the rest of the block hierarchies.
		if (!(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack) || (aY == WD.maxY(aWorld) && getMaterial().isSolid()) /* the world ceiling now comes from the single Y-scale center, not a hardcoded 255 */ || !WD.canPlaceEntityOnSide(aWorld, this, aX, aY, aZ, F, aSide, aPlayer, aStack)) return F;

		if (aItem.placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, onBlockPlaced(aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, aItem.getMetadata(aStack.getDamageValue())))) {
			WD.playStepSound(aWorld, aX+0.5F, aY+0.5F, aZ+0.5F, this);
			if (!UT.Entities.hasInfiniteItems(aPlayer)) aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}
}
