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
import net.minecraft.resources.Identifier;

/**
 * @author Gregorius Techneticies
 */
public abstract class BlockBaseMeta extends BlockBaseSealable implements gregapi.render.IRenderedBlock, gregapi.block.IBlockExtendedMetaData {
	public IIconContainer[] mIcons;
	/** For Creative Subsets, not actually important. */
	private final byte mMaxMeta;

	// F13 (BUG-006/009/010): семья BlockBaseMeta (логи/камни/листва/слэбы) не хранила мету → placed-блок всегда meta 0
	// (WD.set:848 сохранял мету ТОЛЬКО для IBlockExtendedMetaData). Восстановлено ДОСЛОВНЫМ зеркалом уже работающего
	// приёма BlockBaseFlower:75-76,145-152 / BlockFluidBaseGT: мета в BlockState-свойстве (4 бита = 0-15; WD.meta через
	// Code.bind4). WD.set/WD.meta уже маршрутизируют в get/setExtendedMetaData; GT6BlockModel читает getExtendedMetaData.
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty META =
		net.minecraft.world.level.block.state.properties.IntegerProperty.create("meta", 0, 15);
	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> aBuilder) {super.createBlockStateDefinition(aBuilder); aBuilder.add(META);}

	public BlockBaseMeta(Class<? extends BlockItem> aItemClass, String aNameInternal, Material aMaterial, SoundType aSoundType, long aMaxMeta, IIconContainer[] aIcons) {
		super(aItemClass, aNameInternal, aMaterial, aSoundType);
		mMaxMeta = (byte)UT.Code.bind(1, 16, aMaxMeta);
		mIcons = aIcons;
		registerDefaultState(getStateDefinition().any().setValue(META, 0)); // порядок: после super() (createBlockStateDefinition уже отработал в Block-конструкторе)
	}

	// Хранилище меты семьи — BlockState-свойство META. Читает любой BlockGetter; пишет Level/LevelAccessor (интерактив)
	// ИЛИ ChunkAccess (ворлдген — логи/руды кладутся WD.set(ChunkAccess):872 → setExtendedMetaData(aChunk,...)).
	// Консолидация (BUG-047-ревизия): раскладка меты по свойствам живёт в ОДНОМ месте — каналах
	// get/getStateForExtendedMetaData(BlockState) интерфейса (дефолт = META); геттер/сеттер только маршрутизируют.
	@Override public short getExtendedMetaData(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {
		net.minecraft.world.level.block.state.BlockState tState = aWorld.getBlockState(new net.minecraft.core.BlockPos(aX, aY, aZ));
		return tState.getBlock() == this ? getExtendedMetaData(tState) : 0;
	}
	@Override public void setExtendedMetaData(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		net.minecraft.core.BlockPos tPos = new net.minecraft.core.BlockPos(aX, aY, aZ);
		net.minecraft.world.level.block.state.BlockState tState = aWorld.getBlockState(tPos);
		if (tState.getBlock() != this) return;
		net.minecraft.world.level.block.state.BlockState tNew = getStateForExtendedMetaData(tState, aMetaData);
		if (tNew == null) return;
		if (aWorld instanceof net.minecraft.world.level.LevelAccessor tLA) tLA.setBlock(tPos, tNew, 3);
		else if (aWorld instanceof net.minecraft.world.level.chunk.ChunkAccess tChunk) tChunk.setBlockState(tPos, tNew, net.minecraft.world.level.block.Block.UPDATE_ALL);
	}

	@Override public byte maxMeta() {return mMaxMeta;}
	public Identifier getIcon(int aSide, int aMeta) {return mIcons[aMeta % mIcons.length].getIcon(0);}
	@SuppressWarnings("unchecked") public void getSubBlocks(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {for (int i = 0; i < maxMeta(); i++) aList.add(ST.make(aItem, 1, i));}

	// F3-render (централизация): BlockBaseMeta-контент (asphalt/concrete/cfoam/glass/…) рендерится единой GT6BlockModel
	// через IRenderedBlock — текстура берётся из уже существующих mIcons (per-meta IIconContainer), обёрнутых в ITexture
	// (BlockTextureDefault.get, client-safe → null на сервере). Один render-pass, полный блок, все стороны = иконка меты.
	// Убирает «Missing model for variant Block{gregtech:gt.block.*}» (было: не-IRenderedBlock → плейсхолдер). Иконка per-side
	// не различается (getIcon(side,meta) сам игнорирует side у этих блоков — 1:1 с оригиналом).
	// per-side выбор — через САМ getIcon(side,meta) (наследники — BlockBaseBeam/Bale — переопределяют его
	// per-side логикой top/side; прежний texOf(meta) игнорировал side → item/мир-формы теряли боковые грани,
	// пойман block-item golden-компаратором: порт rye_top vs golden rye_side+rye_top).
	/** 1.7.10 vanilla-рендер применял Block.getRenderColor(meta) (BlockColored: DYES_INT — бетон/асфальт/cfoam
	 *  тёмные 0x202020 при мете 0); дефолт белый 1:1 vanilla Block. Пойман block-golden (тинт ffffff vs 202020). */
	public int getRenderColor(int aMeta) {return 0xFFFFFF;}

	private gregapi.render.ITexture texOf(byte aSide, int aMeta) {
		if (mIcons == null || mIcons.length == 0) return null;
		final int tColor = getRenderColor(aMeta);
		final short[] tRGBa = tColor == 0xFFFFFF ? null : gregapi.util.UT.Code.getRGBaArray(tColor);
		final net.minecraft.resources.Identifier tIcon = getIcon(aSide, aMeta);
		final gregapi.render.IIconContainer tBase = mIcons[aMeta % mIcons.length];
		if (tIcon == null || tIcon.equals(tBase.getIcon(0))) return tRGBa == null ? gregapi.render.BlockTextureDefault.get(tBase) : gregapi.render.BlockTextureDefault.get(tBase, tRGBa);
		return wrap(tIcon, tBase, tRGBa);
	}
	private gregapi.render.ITexture wrap(final net.minecraft.resources.Identifier tIcon, final gregapi.render.IIconContainer tBase, short[] aRGBa) {
		gregapi.render.IIconContainer tCont = new gregapi.render.IIconContainer() {
			@Override public net.minecraft.resources.Identifier getIcon(int aRenderPass) {return tIcon;}
			@Override public boolean isUsingColorModulation(int aRenderPass) {return tBase.isUsingColorModulation(aRenderPass);}
			@Override public short[] getIconColor(int aRenderPass) {return tBase.getIconColor(aRenderPass);}
			@Override public int getIconPasses() {return tBase.getIconPasses();}
			@Override public net.minecraft.resources.Identifier getTextureFile() {return tBase.getTextureFile();}
			@Override public void registerIcons(Object aIconRegister) {/* атлас-стежка мертва (F3) */}
		};
		return aRGBa == null ? gregapi.render.BlockTextureDefault.get(tCont) : gregapi.render.BlockTextureDefault.get(tCont, aRGBa);
	}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, net.minecraft.world.item.ItemStack aStack) {return texOf(aSide, gregapi.util.ST.meta_(aStack));}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {return texOf(aSide, gregapi.util.WD.meta(aWorld, aX, aY, aZ));}
	@Override public boolean usesRenderPass(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return aRenderPass == 0;}
	@Override public boolean usesRenderPass(int aRenderPass, net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return true;}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return true;}
	@Override public int getRenderPasses(net.minecraft.world.item.ItemStack aStack) {return 1;}
	@Override public int getRenderPasses(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(net.minecraft.world.item.ItemStack aStack) {return null;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(net.minecraft.world.level.BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
}
