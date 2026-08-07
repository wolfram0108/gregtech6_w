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
	 * F12-entity: РЕАЛЬНЫЙ падающий блок для собственной логики — точный аналог {@code super.func_145805_f()}
	 * оригинала (PrefixBlockFallingEntity.java:87,98,116-118).
	 * <p>Хранилище то же, что у 1.7.10 — приватное поле базы ({@code FallingBlockEntity.blockState}); своей копии
	 * состояния не заводим. Задаётся оно в конструкторе через штатное чтение NBT базы (единственный путь: поле
	 * приватно, конструктор с ним закрыт, сеттера нет — FallingBlockEntity.java:64,79), поэтому и сохранение с
	 * загрузкой работают базовым механизмом, без собственного ключа.
	 * <p>ПУБЛИЧНЫЙ геттер при этом переопределён на гравий (:120-122 оригинала) — движку и рендеру автор намеренно
	 * показывал не сам блок. Разделение ровно то же: логика зовёт этот метод, движок — {@link #getBlockState()}.
	 */
	protected Block fallingBlock() {return super.getBlockState().getBlock();}

	/** Записать реальный падающий блок в приватное поле базы штатным путём — её же чтением NBT.
	 *  {@code readAdditionalSaveData} базы (FallingBlockEntity.java:305-314) — чистые присваивания с дефолтами:
	 *  Time=0, DropItem=true, остальное по умолчанию, побочных эффектов нет. */
	private void initFallingBlock(Level aWorld, net.minecraft.world.level.block.state.BlockState aState) {
		net.minecraft.util.ProblemReporter.Collector tRep = new net.minecraft.util.ProblemReporter.Collector();
		net.minecraft.world.level.storage.TagValueOutput tOut =
			net.minecraft.world.level.storage.TagValueOutput.createWithContext(tRep, aWorld.registryAccess());
		tOut.store("BlockState", net.minecraft.world.level.block.state.BlockState.CODEC, aState);
		super.readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(tRep, aWorld.registryAccess(), tOut.buildResult()));
	}

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
		initFallingBlock(aWorld, ((Block)aBlock).defaultBlockState()); // 1:1 оригинала: (Block)aBlock, мета 0
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
			} else if (time > 100 && !level().isClientSide() && (aY < WD.minY(level())+1 || aY > WD.topY(level())) || time > 600) { // BUG-089: было aY < 1 || aY > 256 — границы мира через центр F6-Y-scale
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
		// Реальный падающий блок сохраняет САМА база (пишет своё приватное поле, куда его положил конструктор
		// через initFallingBlock) — ровно как 1.7.10 сохранял его базовым writeEntityToNBT. Своего ключа не нужно.
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		CompoundTag aNBT = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(UT.NBT::make);
		// 1:1 оригинала (:115-118): блок и стек восстанавливаются из РЕАЛЬНОГО блока (там — super.func_145805_f()),
		// который база уже подняла из NBT строкой выше. Страховка на не-GT6 блок (чужой или повреждённый сейв):
		// mBlock остаётся null, и сущность при приземлении уходит в ветку дропа предметом — вместо краха приведения.
		mBlock = fallingBlock() instanceof IBlockPlacable tPlacable ? tPlacable : null;
		mStack = ST.make(fallingBlock(), 1, aNBT.getShortOr("MetaData", (short)0));
		mBlockNBT = aNBT.getCompound("TileEntityData").orElse(null);
		ItemNBT.set(mStack, mBlockNBT);
	}
}
