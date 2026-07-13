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

package gregapi.block.fluid;

import gregapi.block.IBlock;
import gregapi.block.Material;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.HashMap;
import java.util.Map;

import static gregapi.data.CS.*;

/**
 * F5 форс движка (decisions/F5-fluids.md §5): в 1.7.10 {@link BlockWaterlike} и {@link BlockBaseFluid} делил
 * ОДИН общий предок — Forge {@code net.minecraftforge.fluids.BlockFluidBase} (quanta-текучесть: quantaPerBlock/
 * density/densityDir/tickRate/displacements-поля + canDisplace/displaceIfPossible/getDensity/
 * getQuantaValueBelow-методы). Класс удалён в neo (ни в одном из 3 корней референса) — GT6 сама этот класс
 * никогда не писала (сторонняя Forge-библиотека), поэтому предок воспроизведён здесь ОДИН раз (централизация
 * §3, F5-доклад §5 "кастомный Block-базовый класс"), тела 1:1 из Forge 1.7.10
 * {@code BlockFluidBase}/{@code BlockFluidClassic}/{@code BlockFluidFinite} (только API-свод под
 * BlockGetter/Level/BlockPos вместо IBlockAccess/World/int-тройки; {@code Material.func_149688_o()} ->
 * {@link WD#getMaterial(Block)}, {@code material.func_76230_c()} -> {@code Material.blocksMovement()},
 * {@code Material.field_151567_E} -> {@code Material.portal} — сверено `methods.csv`/`fields.csv` MCP 1.7.10).
 * Реально используемая GT6-логика quanta-потока (Ocean/River/Swamp updateTick, BlockBaseFluid.updateTick) —
 * СОБСТВЕННАЯ, не отсюда; сюда попало только то, что реально вызывается через unqualified/{@code super.}-имя
 * из {@link BlockWaterlike}/{@link BlockBaseFluid} (canDisplace/displaceIfPossible/getQuantaValueBelow/
 * getDensity) — мёртвый в GT6 {@code BlockFluidClassic}-tick-хвост (getOptimalFlowDirections/
 * calculateFlowCost/flowIntoBlock/canFlowInto/isFlowingVertically — никогда не вызывается, GT6 переопределяет
 * тик целиком в Ocean/River/Swamp и никогда не зовёт {@code super.updateTick}) не портирован — не выдумываем
 * мёртвый код.
 */
public abstract class BlockFluidBaseGT extends Block implements IBlock {
	/** было Forge {@code BlockFluidBase.displacements} + статический {@code defaultDisplacements}
	 *  (wooden_door/iron_door/standing_sign/wall_sign/reeds -> false). PORT-TODO(F5, fluid-door-sign-defaults):
	 *  1.7.10 знал ОДИН блок на дверь/вывеску; neo расщепил на блок-на-древесину (нет 1:1 отображения без
	 *  угадывания полного списка — REMAP-RULES «не выдумывать»), карта оставлена пустой (безопасный дефолт:
	 *  двери/вывески в material.blocksMovement()-ветке и так возвращают false). */
	protected Map<Block, Boolean> displacements = new HashMap<>();

	protected int quantaPerBlock = 8;
	protected float quantaPerBlockFloat = 8F;
	protected int density = 1;
	protected int densityDir = -1;
	protected int tickRate = 20;

	/** F9: см. {@link gregapi.block.BlockBase#getMaterial()} — тот же приём (собственное поле вместо
	 *  удалённого neo {@code Material}-конструктора Block'а). */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}

	/** F-bounds: см. {@link gregapi.block.BlockBase#setBlockBounds} — тот же центр-приём, разделяемый ОБОИМИ
	 *  fluid-блоками (было Forge {@code Block.setBlockBounds} внутри {@code BlockFluidBase}-конструктора). */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}

	/** F16/F9 форс движка: было {@code BlockFluidBase(Fluid,Material)}, читавший density/temperature/
	 *  maxScaledLight/tickRate/densityDir ИЗ САМОГО Forge {@code Fluid}-объекта (data-holder-поля) — neo
	 *  {@code net.minecraft.world.level.material.Fluid} этих полей не несёт (данные расщеплены в
	 *  {@code FluidType}, F5-доклад §1/§3). PORT-TODO(F5, fluid-property-bridge): авто-вывод density/tickRate/
	 *  densityDir из жидкости отложен на этап 6 (FluidType.Properties мост, F5-доклад §8); поля остаются на
	 *  Forge-дефолтах (density=1, densityDir=-1, tickRate=20, quantaPerBlock=8) — вызыватели, которым нужно
	 *  другое, устанавливают явно (как уже делают {@link gregtech.blocks.fluids.BlockOcean}/River/Swamp,
	 *  переставляющие {@code tickRate} сразу после {@code super(...)}).
	 */
	public BlockFluidBaseGT(BlockBehaviour.Properties aProperties, Material aMaterial) {
		super(aProperties);
		mMaterial = aMaterial;
	}

	public abstract int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ);

	/** было Forge {@code BlockFluidBase.onNeighborBlockChange(World,x,y,z,Block)} (func_149695_a) — тело 1:1.
	 *  Нужен {@link gregtech.blocks.fluids.BlockOcean}/{@link gregtech.blocks.fluids.BlockRiver}, которые зовут
	 *  {@code super.onNeighborBlockChange(...)} после своей собственной логики. */
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
	}

	/** было Forge {@code BlockFluidBase.canDisplace(IBlockAccess,x,y,z)} — тело 1:1. */
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockPos aPos = new BlockPos(aX, aY, aZ);
		if (aWorld.getBlockState(aPos).isAir()) return T; // было block.isAir(world,x,y,z) — BlockState.isAir() (BlockBehaviour.java:575)
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == this) return F;
		if (displacements.containsKey(aBlock)) return displacements.get(aBlock);
		Material aBlockMaterial = WD.getMaterial(aBlock);
		if (aBlockMaterial.blocksMovement() || aBlockMaterial == Material.portal) return F;
		int tDensity = getDensity(aWorld, aX, aY, aZ);
		if (tDensity == Integer.MAX_VALUE) return T;
		return this.density > tDensity;
	}

	/** было Forge {@code BlockFluidBase.displaceIfPossible(World,x,y,z)} — тело 1:1 (drop-эффект вытесняемого
	 *  Blocks, было {@code block.dropBlockAsItem(...)}, 1.7.10-only метод удалён в neo — PORT-TODO(F5,
	 *  fluid-displace-drop): вытеснение само по себе (true/false) сохранено 1:1, побочный drop предмета из
	 *  вытесняемого блока отложен, компилируем без него, ничего кроме этого drop-эффекта не теряется). */
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {
		BlockPos aPos = new BlockPos(aX, aY, aZ);
		if (aWorld.getBlockState(aPos).isAir()) return T;
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == this) return F;
		if (displacements.containsKey(aBlock)) return displacements.get(aBlock);
		Material aBlockMaterial = WD.getMaterial(aBlock);
		if (aBlockMaterial.blocksMovement() || aBlockMaterial == Material.portal) return F;
		int tDensity = getDensity(aWorld, aX, aY, aZ);
		if (tDensity == Integer.MAX_VALUE) return T;
		return this.density > tDensity;
	}

	/** было Forge {@code BlockFluidBase.getDensity(IBlockAccess,x,y,z)} (static). */
	public static int getDensity(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (!(aBlock instanceof BlockFluidBaseGT)) return Integer.MAX_VALUE;
		return ((BlockFluidBaseGT)aBlock).density;
	}

	/** было Forge {@code BlockFluidBase.getQuantaValueBelow(IBlockAccess,x,y,z,belowThis)} (final) — тело 1:1. */
	public final int getQuantaValueBelow(BlockGetter aWorld, int aX, int aY, int aZ, int aBelowThis) {
		int tQuantaRemaining = getQuantaValue(aWorld, aX, aY, aZ);
		if (tQuantaRemaining >= aBelowThis) return -1;
		return tQuantaRemaining;
	}
}
