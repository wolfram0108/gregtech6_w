/**
 * Copyright (c) 2021 GregTech-6 Team
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SoundType;

import java.util.List;

import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.block.Material;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBaseMeta extends BlockBaseSealable implements gregapi.render.IRenderedBlock, gregapi.block.IBlockExtendedMetaData {
	public IIconContainer[] mIcons;
	/** For Creative Subsets, not actually important. */
	private final byte mMaxMeta;

	// This family never stored its metadata, so every placed block came back as meta 0; fixed by
	// mirroring the same BlockState-property technique already working for flowers and fluids.
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty META =
		net.minecraft.world.level.block.state.properties.IntegerProperty.create("meta", 0, 15);
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {super.createBlockStateDefinition(aBuilder); aBuilder.add(META);}

	public BlockBaseMeta(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType);
		mMaxMeta = (byte)UT.Code.bind(1, 16, aMaxMeta);
		mIcons = aIcons;
		registerDefaultState(getStateDefinition().any().setValue(META, 0)); // placed after super() because createBlockStateDefinition has already run inside the Block constructor
	}

	// Meta storage for this whole family is the META BlockState property; the get/set defaults come
	// from IBlockExtendedMetaData, one shared implementation instead of separate copies per carrier.

	@Override public byte maxMeta() {return mMaxMeta;}
	public ResourceLocation getIcon(int aSide, int aMeta) {return mIcons[aMeta % mIcons.length].getIcon(0);}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {for (int i = 0; i < maxMeta(); i++) aList.add(ST.make(aItem, 1, i));}

	// This whole family renders through one shared GT6BlockModel via IRenderedBlock, reusing the
	// existing per-meta icons; per-side variation still goes through getIcon itself, which subclasses override.
	/** Defaults to white, matching vanilla Block's own default; concrete/asphalt/cfoam override it
	 *  with their own darker tint, as 1.7.10 did. */
	public int getRenderColor(int aMeta) {return 0xFFFFFF;}

	/** 1.7.10 asked two different color methods depending on the render path (world vs inventory);
	 *  merging both paths into one model fed the inventory channel to both, so positional/biome tint never reached the world. */
	private gregapi.render.ITexture texOf(byte aSide, int aMeta) {return texOf(aSide, aMeta, getRenderColor(aMeta));}
	private gregapi.render.ITexture texOf(byte aSide, int aMeta, int aColor) {
		if (mIcons == null || mIcons.length == 0) return null;
		final int tColor = aColor;
		final short[] tRGBa = tColor == 0xFFFFFF ? null : gregapi.util.UT.Code.getRGBaArray(tColor);
		final net.minecraft.resources.ResourceLocation tIcon = getIcon(aSide, aMeta);
		final gregapi.render.IIconContainer tBase = mIcons[aMeta % mIcons.length];
		if (tIcon == null || tIcon.equals(tBase.getIcon(0))) return tRGBa == null ? gregapi.render.BlockTextureDefault.get(tBase) : gregapi.render.BlockTextureDefault.get(tBase, tRGBa);
		return wrap(tIcon, tBase, tRGBa);
	}
	private gregapi.render.ITexture wrap(final net.minecraft.resources.ResourceLocation tIcon, final gregapi.render.IIconContainer tBase, short[] aRGBa) {
		gregapi.render.IIconContainer tCont = new gregapi.render.IIconContainer() {
			@Override public net.minecraft.resources.ResourceLocation getIcon(int aRenderPass) {return tIcon;}
			@Override public boolean isUsingColorModulation(int aRenderPass) {return tBase.isUsingColorModulation(aRenderPass);}
			@Override public short[] getIconColor(int aRenderPass) {return tBase.getIconColor(aRenderPass);}
			@Override public int getIconPasses() {return tBase.getIconPasses();}
			@Override public net.minecraft.resources.ResourceLocation getTextureFile() {return tBase.getTextureFile();}
			@Override public void registerIcons(Object aIconRegister) {/* atlas stitching is dead here */}
		};
		return aRGBa == null ? gregapi.render.BlockTextureDefault.get(tCont) : gregapi.render.BlockTextureDefault.get(tCont, aRGBa);
	}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, net.minecraft.world.item.ItemStack aStack) {return texOf(aSide, gregapi.util.ST.meta_(aStack));}
	// The world path uses the world color channel; IBlock.colorMultiplier's default falls back to
	// getRenderColor(meta), keeping the same value as before for blocks with no positional tint.
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {return texOf(aSide, gregapi.util.WD.meta(aWorld, aX, aY, aZ), colorMultiplier(aWorld, aX, aY, aZ));}
	@Override public boolean usesRenderPass(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return aRenderPass == 0;}
	@Override public boolean usesRenderPass(int aRenderPass, net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return true;}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return true;}
	@Override public int getRenderPasses(net.minecraft.world.item.ItemStack aStack) {return 1;}
	@Override public int getRenderPasses(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(net.minecraft.world.item.ItemStack aStack) {return null;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
}
