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

package gregapi.block.prefixblock;
import gregapi.util.WD;

import static gregapi.data.CS.*;

import com.mojang.serialization.MapCodec;

import gregapi.block.IBlockPlacable;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * @author Gregorius Techneticies
 */
public class PrefixBlockFallingEntity extends FallingBlockEntity {
	protected IBlockPlacable mBlock;
	protected ItemStack mStack;
	protected CompoundTag mBlockNBT;

	/**
	 * F12-entity: РЕАЛЬНЫЙ блок, который падает — то, что 1.7.10 держал в приватном поле базы
	 * {@code EntityFallingBlock.field_145811_e} (ставилось конструктором {@code super(aWorld,aX,aY,aZ,(Block)aBlock,0)}).
	 * В neo одноимённое поле {@code FallingBlockEntity.blockState} тоже приватно, но конструктор, который его
	 * задаёт, закрыт (FallingBlockEntity.java:64,79) — поэтому своё поле берёт на себя ту же роль.
	 * <p>Оригинал обращался к нему через {@code super.func_145805_f()} (PrefixBlockFallingEntity.java:87,98,116-118),
	 * а ПУБЛИЧНЫЙ геттер переопределял на гравий (:120-122) — то есть движку и рендеру намеренно показывался
	 * не сам блок. Здесь разделение то же: логика читает это поле, движок — {@link #getBlockState()}.
	 */
	protected net.minecraft.world.level.block.state.BlockState mBlockState = null;

	/** Реальный падающий блок для собственной логики — точный аналог {@code super.func_145805_f()} оригинала. */
	protected Block fallingBlock() {return mBlockState != null ? mBlockState.getBlock() : super.getBlockState().getBlock();}

	/** Фабрика движка ({@code EntityType.EntityFactory}): вызывается при спавне на клиенте и при загрузке с диска. */
	public PrefixBlockFallingEntity(EntityType<? extends PrefixBlockFallingEntity> aType, Level aWorld) {
		super(aType, aWorld);
	}

	public PrefixBlockFallingEntity(Level aWorld) {
		super(gregapi.GT_API.METABLOCK_FALLING.get(), aWorld);
	}

	public PrefixBlockFallingEntity(Level aWorld, double aX, double aY, double aZ, IBlockPlacable aBlock, ItemStack aStack) {
		super(gregapi.GT_API.METABLOCK_FALLING.get(), aWorld);
		setPos(aX, aY, aZ);
		setStartPos(blockPosition()); // 1:1 приватного neo-конструктора (FallingBlockEntity.java:88) — иначе точка старта нулевая
		mBlock = aBlock;
		mStack = aStack;
		mBlockNBT = ItemNBT.get(aStack);
		mBlockState = ((Block)aBlock).defaultBlockState(); // 1:1 оригинала: (Block)aBlock, мета 0
	}

	/**
	 * 1:1 оригинала (:120-122): {@code public Block func_145805_f() {return Blocks.gravel;}} — публичный геттер
	 * блока, единственный, которым падающую сущность видят движок и рендер. Мета-блок GT6 рисуется своей моделью
	 * по данным из TileEntity, которых у сущности нет, поэтому автор подменил визуал гравием; собственная логика
	 * от подмены не страдает — она читает {@link #fallingBlock()}. В neo этот геттер обслуживает и спавн-пакет
	 * ({@code Block.getId(this.getBlockState())}, FallingBlockEntity.java:363), то есть гравий увидит и клиент —
	 * ровно как в 1.7.10, где приватное поле базы на клиент вообще не синхронизировалось.
	 */
	@Override
	public net.minecraft.world.level.block.state.BlockState getBlockState() {
		return Blocks.GRAVEL.defaultBlockState();
	}

	@Override
	public void tick() {
		xo = getX();
		yo = getY();
		zo = getZ();
		++time;
		setDeltaMovement(getDeltaMovement().add(0.0D, -0.03999999910593033D, 0.0D));
		move(MoverType.SELF, getDeltaMovement());
		setDeltaMovement(getDeltaMovement().scale(0.9800000190734863D));
		if (!level().isClientSide()) {
			int aX = UT.Code.roundDown(getX());
			int aY = UT.Code.roundDown(getY());
			int aZ = UT.Code.roundDown(getZ());
			if (time == 1) {
				if (WD.block(level(), aX, aY, aZ) != fallingBlock()) { // 1:1 оригинала :87 — сверка с РЕАЛЬНЫМ блоком (super.func_145805_f()), не с визуалом
					discard();
					return;
				}
				WD.set(level(), aX, aY, aZ, Blocks.AIR, 0, 3);
			}
			if (onGround()) {
				Vec3 v = getDeltaMovement();
				setDeltaMovement(v.x * 0.699999988079071D, v.y * -0.5D, v.z * 0.699999988079071D);
				if (WD.block(level(), aX, aY, aZ) != Blocks.MOVING_PISTON) {
					discard();
					// 1:1 оригинала :98 — «нельзя поставить блок в эту клетку» через ЦЕНТР WD.canPlaceEntityOnSide
					// (тот же, к которому подключены BlockBase/BlockBaseFlower/BlockBaseRail/BlockRailRoad/MultiTileEntityCoin).
					// Прежде здесь стоял WD.hasCollide(…, падающий блок), а он первым же термом спрашивает opaque(САМОГО блока):
					// для любой сплошной руды это безусловное «занято» → приземление всегда сваливалось в дроп предметом,
					// placeBlock не вызывался ни разу (короткое замыкание ||). fallingBlock() — РЕАЛЬНЫЙ блок, не визуал.
					// mBlock == null — страховка чтения старого сейва (см. readAdditionalSaveData): уходим в ветку дропа предметом.
					if (!WD.canPlaceEntityOnSide(level(), fallingBlock(), aX, aY, aZ, T, 1, null, mStack) || FallingBlock.isFree(WD.block(level(), aX, aY - 1, aZ).defaultBlockState()) || mBlock == null || !mBlock.placeBlock(level(), aX, aY, aZ, (byte)1, ST.meta_(mStack), ItemNBT.get(mStack), T, T)) {
						if (dropItem) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, tStack);}} else {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, mStack);}
					}
				}
			} else if (time > 100 && !level().isClientSide() && (aY < 1 || aY > 256) || time > 600) {
				if (dropItem) if (mBlock instanceof PrefixBlock) {for (ItemStack tStack : ((PrefixBlock)mBlock).mDrops.getDrops((PrefixBlock)mBlock, level(), aX, aY, aZ, ST.meta_(mStack), null, 0, F)) {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, tStack);}} else {if (level() instanceof ServerLevel tServerLevel) spawnAtLocation(tServerLevel, mStack);}
				discard();
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean causeFallDamage(double aFallDistance, float aDamageModifier, DamageSource aDamageSource) {
		int i = Mth.ceil(aFallDistance - 1.0D);
		if (i > 0) for (Entity tEntity : new ArrayListNoNulls<Entity>(level().getEntities(this, getBoundingBox()))) {
			if (tEntity instanceof LivingEntity) tEntity.hurt(damageSources().fallingBlock(this), TFC_DAMAGE_MULTIPLIER * Math.min(Mth.floor((float)i * 2), 40));// было DamageSource.fallingBlock (1.7.10 статик удалён) -> neo damageSources().fallingBlock(Entity=падающий блок=this)
		}
		return false;
	}

	/**
	 * F8 (шов «NBT-персистенс Entity», тот же приём моста CompoundTag<->ValueIO, что и F8-TE в
	 * {@code TileEntityBase01Root.saveAdditional/loadAdditional}): neo зовёт
	 * {@code addAdditionalSaveData(ValueOutput)}/{@code readAdditionalSaveData(ValueInput)}
	 * (`neo-decompiled/net/minecraft/world/entity/Entity.java:2121,2123`), а не GT6/1.7.10-модель
	 * {@code writeEntityToNBT}/{@code readEntityFromNBT}(NBTTagCompound). super.addAdditionalSaveData
	 * вызывается первым, чтобы сохранить neo-собственные данные FallingBlockEntity (Time/DropItem/
	 * BlockState/TileEntityData/…, см. FallingBlockEntity.java:290-302).
	 */
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		CompoundTag aNBT = UT.NBT.make();
		aNBT.putShort("MetaData", ST.meta_(mStack));
		if (mBlockNBT != null) aNBT.put("TileEntityData", mBlockNBT);
		output.store(aNBT);
		// F12-entity: РЕАЛЬНЫЙ падающий блок. 1.7.10 его сохранял базовый writeEntityToNBT (поле field_145811_e),
		// а neo-база пишет своё приватное поле — у нас там визуал-гравий (см. getBlockState). Поэтому настоящий
		// блок кладём своим ключом, иначе после перезахода сущность не знает, что именно она несёт.
		if (mBlockState != null) output.store("gt.fallingblock", net.minecraft.world.level.block.state.BlockState.CODEC, mBlockState);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		CompoundTag aNBT = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(UT.NBT::make);
		mBlockState = input.read("gt.fallingblock", net.minecraft.world.level.block.state.BlockState.CODEC).orElse(null);
		// 1:1 оригинала (:115-118): блок и стек восстанавливаются из РЕАЛЬНОГО блока (там — super.func_145805_f()).
		// Страховка на не-GT6 блок (сейв, записанный до появления ключа выше): mBlock остаётся null, и сущность
		// при приземлении уходит в ветку дропа предметом вместо постановки — вместо ClassCastException на загрузке.
		mBlock = fallingBlock() instanceof IBlockPlacable tPlacable ? tPlacable : null;
		mStack = ST.make(fallingBlock(), 1, aNBT.getShortOr("MetaData", (short)0));
		mBlockNBT = aNBT.getCompound("TileEntityData").orElse(null);
		ItemNBT.set(mStack, mBlockNBT);
	}
}
