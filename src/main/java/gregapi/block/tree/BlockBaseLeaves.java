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

package gregapi.block.tree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.api.Optional;
import net.minecraftforge.api.distmarker.Dist;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.MD;
import gregapi.data.OP;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.IForgeShearable;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
  @Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock", modid = ModIDs.GC)
})
public abstract class BlockBaseLeaves extends BlockBaseTree implements IForgeShearable, IOxygenReliantBlock {
	public final Block mSaplings;
	public final Block[] mLogs;
	public final byte[] mLogMetas;
	
	public BlockBaseLeaves(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons, Block aSaplings, Block[] aLogs, byte[] aLogMetas) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType, Math.min(8, aMaxMeta), aIcons);
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.DECORATIONS);
		mSaplings = aSaplings;
		mLogMetas = aLogMetas;
		mLogs = aLogs;
		// F12-hardness: 1.7.10 setHardness(0.2F) (runtime мутатор) заменён getBlockHardness ниже (OAK_LEAVES) →
		// подключён к neo через BlockBase.getDestroyProgress (централизованно, 1:1). Runtime-мутатор не нужен.
	}
	
	@Override public boolean isFireSource(Level aWorld, int aX, int aY, int aZ, Direction aSide) {return F;}
	@Override public int getFlammability(byte aMeta) {return 30;}
	@Override public int getFireSpreadSpeed(byte aMeta) {return 60;}
	@Override public String getHarvestTool(int aMeta) {return TOOL_sword;}
	@Override public int damageDropped(int aMeta) {return aMeta & 7;}
	@Override public int getDamageValue(Level aWorld, int aX, int aY, int aZ) {return WD.meta(aWorld, aX, aY, aZ) & 7;}
	@Override public Item getItemDropped(int aMeta, Random aRandom, int aFortune) {return Item.byBlock(mSaplings);}
	@Override public float getBlockHardness(Level aWorld, int aX, int aY, int aZ) {return WD.hardness(Blocks.OAK_LEAVES, aWorld, aX, aY, aZ);}
	@Override public float getExplosionResistance(byte aMeta) {return Blocks.OAK_LEAVES.getExplosionResistance();}
	@Override public boolean renderAsNormalBlock() {return F;}
	@Override public boolean isNormalCube(BlockGetter aWorld, int aX, int aY, int aZ)  {return F;}
	@Override public boolean isOpaqueCube() {return F;}
	@Override public boolean isSealable(byte aMeta, byte aSide) {return F;}
	@Override public boolean isSideSolid(int aMeta, byte aSide) {return F;}
	public boolean isLeaves(BlockGetter aWorld, int aX, int aY, int aZ) {return T;}
	// F10: IShearable-зеркало снято — настоящий net.minecraftforge.common.IForgeShearable (сигнатура
	// isShearable(ItemStack,Level,BlockPos)/onSheared(Player,ItemStack,Level,BlockPos,int):List<ItemStack>,
	// оба метода default в интерфейсе — override сохраняет 1.7.10-тело как есть).
	@Override public boolean isShearable(ItemStack aItem, Level aWorld, BlockPos aPos) {return T;}
	@Override public int getLightOpacity() {return LIGHT_OPACITY_LEAVES;}
	@Override public int getItemStackLimit(ItemStack aStack) {return UT.Code.bindStack(OP.treeLeaves.mDefaultStackSize);}
	// 1:1 (:91 оригинала): выбор fancy/fast-варианта иконки по признаку ванильной листвы; семантика isOpaqueCube
	// = WD.visOpq (WD.opaque=canOcclude тут врал — см. skipRendering ниже). В neo-ванили листва всегда fancy.
	@Override public ResourceLocation getIcon(int aSide, int aMeta) {return mIcons[(aMeta&7)|(WD.visOpq(Blocks.OAK_LEAVES)?8:0)].getIcon(0);}
	@Override public List<ItemStack> onSheared(Player aPlayer, ItemStack aItem, Level aWorld, BlockPos aPos, int aFortune) {return ST.arraylist(ST.make(this, 1, WD.meta(aWorld, aPos.getX(), aPos.getY(), aPos.getZ()) & 7));}
	public AABB getCollisionBoundingBoxFromPool(Level aWorld, int aX, int aY, int aZ) {return MD.TFC.mLoaded || MD.TFCP.mLoaded ? null : WD.collisionBox(aWorld, aX, aY, aZ, this);}
	public void onOxygenAdded(Level aWorld, int aX, int aY, int aZ) {/**/}
	public void onOxygenRemoved(Level aWorld, int aX, int aY, int aZ) {if (!aWorld.isClientSide()) {aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 201+RNGSUS.nextInt(100)); return;}}
	
	@Override
	public void onBlockAdded2(Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !WD.oxygen(aWorld, aX, aY, aZ)) {aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 201+RNGSUS.nextInt(100)); return;}
	}
	
	// было shouldSideBeRendered(IBlockAccess,x,y,z,side) -> BlockBehaviour.skipRendering(BlockState,BlockState,Direction)
	// [BlockBehaviour.java:160], семантика ИНВЕРТИРОВАНА (shouldRender -> skipRendering). Позиция(aX,aY,aZ) в исходнике
	// была позицией СОСЕДА (стандартная 1.7.10-семантика shouldSideBeRendered) -> aNeighbor.getBlock() эквивалентен
	// WD.block(aWorld,aX,aY,aZ) без потерь.
	// ⛔ БЫЛО WD.opaque (canOcclude) — а canOcclude у всей BlockBase-семьи дефолтно TRUE, поэтому сосед-листва
	// считался «непрозрачным кубом» и грань скрывалась ВСЕГДА: дерево выглядело полым «стеклом» без внутренних
	// граней (приёмка 2026-07-30). Семантика 1.7.10 здесь — isOpaqueCube (:106 оригинала), её neo-канон —
	// isSolidRender = центр WD.visOpq (тот же класс дефекта, что F3-render «грань против слаба», WD:1368-1371).
	// visOpq(OAK_LEAVES) при noOcclusion ванильной листвы = false всегда — постоянный fancy, 1:1 с neo-ванилью
	// (динамический fast-режим листвы 1.7.10 в движке отсутствует).
	@Override
	public boolean skipRendering(BlockState aState, BlockState aNeighbor, Direction aDir) {
		Block aBlock = aNeighbor.getBlock();
		return WD.visOpq(aBlock) || (WD.visOpq(Blocks.OAK_LEAVES) && aBlock instanceof BlockBaseLeaves);
	}
	
	// @Override
	public void beginLeavesDecay(Level aWorld, int aX, int aY, int aZ) {
		if (aWorld.isClientSide()) return;
		if (!WD.oxygen(aWorld, aX, aY, aZ)) {aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 201+RNGSUS.nextInt(100)); return;}
		if (WD.meta(aWorld, aX, aY, aZ) < 8) return;
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, 10+RNGSUS.nextInt(SLOW_LEAF_DECAY ? 6400 : 100));
	}
	
	@Override
	public void updateTick2(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		if (aWorld.isClientSide()) return;
		if (!WD.oxygen(aWorld, aX, aY, aZ)) {WD.set(aWorld, aX, aY, aZ, NB, 0, 3); return;}
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		if (aMeta < 8) return;
		int tRangeSide = getLeavesRangeSide(aMeta), tRangeYNeg = getLeavesRangeYNeg(aMeta), tRangeYPos = getLeavesRangeYPos(aMeta);
		for (int i = -tRangeSide; i <= tRangeSide; ++i) for (int j = -tRangeYNeg; j <= tRangeYPos; ++j) for (int k = -tRangeSide; k <= tRangeSide; ++k) {
			if (mLogs    [aMeta & 7] != WD.block(aWorld, aX + i, aY + j, aZ + k)) continue;
			if (mLogMetas[aMeta & 7] != (WD.meta(aWorld, aX + i, aY + j, aZ + k) & 3)) continue;
			return;
		}
		if (!(MD.TFC.mLoaded || MD.TFCP.mLoaded) || aRandom.nextInt(4) == 0) WD.dropBlockAsItem(aWorld, aX, aY, aZ, aMeta, 0);
		WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
	}
	
	// @Override
	public ArrayList<ItemStack> getDrops(Level aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) {
		ArrayListNoNulls<ItemStack> rDrops = ST.arraylist();
		int tChance = 50;
		if (aFortune > 0) {
			tChance -= 5 << aFortune;
			if (tChance < 5) tChance = 5;
		}
		if (RNGSUS.nextInt(tChance) == 0) rDrops.add(ST.make(getItemDropped(aMeta, RNGSUS, aFortune), 1, damageDropped(aMeta)));
		return rDrops;
	}
	
	// было ColorizerFoliage.getFoliageColor(temp,rain) (1.7.10, тип удалён) -> FoliageColor.get(double,double)
	// [neo-decompiled/net/minecraft/world/level/FoliageColor.java:14], идентичная формула/буфер пикселей.
	public int getBlockColor() {return FoliageColor.get(0.5, 1.0);}
	// было ColorizerFoliage.getFoliageColorBasic() (константа 4764952, тип удалён) -> FoliageColor.getDefaultColor()
	// [neo-decompiled/net/minecraft/world/level/FoliageColor.java:6] (=0xFF48B518, младшие 24 бита совпадают: 0x48B518=4764952).
	public int getRenderColor(int p_149741_1_) {return FoliageColor.getDefaultColor();}
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {
		// было aWorld.getBiomeGenForCoords(x,z) (2D, IBlockAccess несла getBiome сама) — BlockGetter самого getBiome
		// не несёт (LevelReader.getBiome(BlockPos)); рендер вызывает colorMultiplier всегда с реальным Level (тот же
		// cast-guard приём, что уже принят в BlockRiver.colorMultiplier, BlockRiver.java:114-115), F3-safe дефолт
		// иначе; биом — через центр WD.biome(Level,x,z) (WD.java:551). Biome.getBiomeFoliageColor(x,y,z) (1.7.10
		// позиционный) удалён - neo Biome.getFoliageColor() [Biome.java:227] беспозиционный, ближайший 1:1.
		if (!(aWorld instanceof Level)) return 0x00ffffff;
		Level aLevel = (Level)aWorld;
		int l = 0, i1 = 0, j1 = 0;
		for (int k1 = -1; k1 <= 1; ++k1) for (int l1 = -1; l1 <= 1; ++l1) {
			int i2 = WD.biome(aLevel, aX + l1, aZ + k1).getFoliageColor();
			l += (i2 & 16711680) >> 16;
			i1 += (i2 & 65280) >> 8;
			j1 += i2 & 255;
		}
		return (l / 9 & 255) << 16 | (i1 / 9 & 255) << 8 | j1 / 9 & 255;
	}
}
