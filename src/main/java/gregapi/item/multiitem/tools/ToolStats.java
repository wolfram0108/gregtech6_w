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

package gregapi.item.multiitem.tools;

import gregapi.damage.DamageSources;
import gregapi.data.IL;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.IIconContainer;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.UT.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * PORT-TODO(F8, enchant-registry): {@code Enchantments.FORTUNE}/{@code Enchantments.LOOTING} (статические
 * инстансы 1.7.10) удалены — зачарования data-driven, {@code Holder<Enchantment>} требует живой
 * {@code RegistryAccess}, недоступный в статическом контексте (тот же класс проблемы, что
 * {@code UT.NBT.getEnchantmentLevelLootingFortune}) — деградация до пустого массива.
 */
public abstract class ToolStats implements IToolStats {
	public static final Enchantment[] FORTUNE_ENCHANTMENT = new Enchantment[0];
	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] LOOTING_ENCHANTMENT = new net.minecraft.resources.ResourceKey[0];

	@Override public int getToolDamagePerBlockBreak()                                       {return 100;}
	@Override public int getToolDamagePerDropConversion()                                   {return 100;}
	@Override public int getToolDamagePerContainerCraft()                                   {return 100;}
	@Override public int getToolDamagePerEntityAttack()                                     {return 100;}
	@Override public int getBaseQuality()                                                   {return   0;}
	@Override public int getHurtResistanceTime(int aOriginalHurtResistance, Entity aEntity) {return aOriginalHurtResistance;}
	@Override public float getBaseDamage()                                                  {return 1.0F;}
	@Override public float getSpeedMultiplier()                                             {return 1.0F;}
	@Override public float getMaxDurabilityMultiplier()                                     {return 1.0F;}
	@Override public float getExhaustionPerAttack(Entity aEntity)                           {return 0.3F;}
	@Override public String getMiningSound()                                                {return null;}
	@Override public String getCraftingSound()                                              {return null;}
	@Override public String getEntityHitSound()                                             {return null;}
	@Override public String getBreakingSound()                                              {return SFX.MC_BREAK;}
	@Override public boolean canCollect()                                                   {return F;}
	@Override public boolean canBlock()                                                     {return F;}
	@Override public boolean canPenetrate()                                                 {return F;}
	@Override public boolean canBehead()                                                    {return F;}
	@Override public boolean isWrench()                                                     {return F;}
	@Override public boolean isCrowbar()                                                    {return F;}
	@Override public boolean isGrafter()                                                    {return F;}
	@Override public boolean isWeapon()                                                     {return F;}
	@Override public boolean isRangedWeapon()                                               {return F;}
	@Override public boolean isMiningTool()                                                 {return T;}

	@Override
	public float getMiningSpeed(Block aBlock, byte aMetaData) {
		return isMinableBlock(aBlock, aMetaData) ? 1 : 0;
	}

	@Override
	public float getMiningSpeed(Block aBlock, byte aMetaData, float aDefault, Player aPlayer, Level aWorld, int aX, int aY, int aZ) {
		return aDefault;
	}

	@Override
	public DamageSource getDamageSource(LivingEntity aPlayer, Entity aEntity) {
		return DamageSources.getCombatDamage(aPlayer instanceof Player ? "player" : "mob", aPlayer, aEntity instanceof LivingEntity ? getDeathMessage(aPlayer, (LivingEntity)aEntity, aPlayer == null ? "Someone" : UT.Code.stringValidate(aPlayer.getScoreboardName(), "Someone"), UT.Code.stringValidate(aEntity.getScoreboardName(), "Someone")) : null, canBehead());
	}

	public Component getDeathMessage(LivingEntity aPlayer, LivingEntity aEntity, String aNamePlayer, String aNameEntity) {return DamageSources.getDeathMessage(aPlayer, aEntity, aNamePlayer, aNameEntity, getDeathMessage());}
	public String getDeathMessage() {return "Why is there no custom Death Message for this Tool?";}

	@Override
	public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
		return 0;
	}

	// F9-flatten: 1.7.10 meta-семейства расщеплены на отдельные neo-блоки (флэттенинг, константы verified javap). Порт 1:1
	// с оригиналом (ToolStats.java:111-145): tallgrass meta1(grass)/2(fern)->SHORT_GRASS/FERN; double_plant meta2/3->TALL_GRASS/
	// LARGE_FERN. Мод-ветки (TF/Aether/BoP) — F10 (вне scope CHARTER), сохранены 1:1, мёртвы без мода (IL.*.equal=F / MD.BoP.mLoaded=F).
	public boolean harvestGrass(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
		if (aBlock == Blocks.SHORT_GRASS || aBlock == Blocks.FERN) {
			aDrops.add(IL.Grass.get(1+RNGSUS.nextInt(1+aFortune))); return T;
		}
		if (aBlock == Blocks.TALL_GRASS || aBlock == Blocks.LARGE_FERN) {
			aDrops.add(IL.Grass.get(2+RNGSUS.nextInt(1+aFortune)+RNGSUS.nextInt(1+aFortune))); return T;
		}
		if (IL.TF_Tall_Grass.equal(aBlock)) {
			switch(aMetaData) {
			case 10: aDrops.add(IL.Grass.get(1+RNGSUS.nextInt(1+aFortune))); return T;
			}
			return F;
		}
		if (IL.AETHER_Tall_Grass.equal(aBlock)) {
			aDrops.add(IL.Grass.get(1+RNGSUS.nextInt(1+aFortune)));
			return T;
		}
		if (MD.BoP.mLoaded) {
			if (aBlock == ST.block(MD.BoP, "foliage")) {
				switch(aMetaData) {
				case  1: if (RNGSUS.nextInt(4) <= aFortune) aDrops.add(IL.Grass.get(1)); return T;
				case  2: if (RNGSUS.nextInt(2) <= aFortune) aDrops.add(IL.Grass.get(1)); return T;
				case 10: aDrops.add(IL.Grass.get(1+RNGSUS.nextInt(1+aFortune))); return T;
				case 11: aDrops.add(IL.Grass.get(1+RNGSUS.nextInt(1+aFortune))); return T;
				}
				return F;
			}
		}
		return F;
	}

	// F9-flatten: 1.7.10 tallgrass meta0(dead-shrub) + deadbush -> оба neo Blocks.DEAD_BUSH (оба роняли dead-stick, объединены 1:1).
	// TF/BoP-ветки — F10 (вне scope), сохранены 1:1, мёртвы без мода. Порт 1:1 с оригиналом (ToolStats.java:148-176).
	public boolean harvestStick(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
		if (aBlock == Blocks.DEAD_BUSH) {
			aDrops.add(OP.stick.mat(MT.WOODS.Dead, 1+RNGSUS.nextInt(2+aFortune)));
			return T;
		}
		if (IL.TF_Tall_Grass.equal(aBlock)) {
			switch(aMetaData) {
			case 11: aDrops.add(IL.Stick.get(1+RNGSUS.nextInt(2+aFortune))); return T;
			}
			return F;
		}
		if (MD.BoP.mLoaded) {
			if (aBlock == ST.block(MD.BoP, "foliage")) {
				switch(aMetaData) {
				case  4: aDrops.add(IL.Stick.get(1+RNGSUS.nextInt(2+aFortune))); return T;
				case  8: aDrops.add(IL.Stick.get(1+RNGSUS.nextInt(2+aFortune))); return T;
				case  9: aDrops.add(IL.Stick.get(1+RNGSUS.nextInt(2+aFortune))); return T;
				}
				return F;
			}
		}
		return F;
	}

	private long mMaterialAmount = 0;

	public ToolStats setMaterialAmount(long aMaterialAmount) {
		mMaterialAmount = aMaterialAmount;
		return this;
	}

	@Override
	public ItemStack getBrokenItem(ItemStack aStack) {
		return mMaterialAmount < U4 ? null : OP.scrapGt.mat(MultiItemTool.getPrimaryMaterial(aStack), 1+RNGSUS.nextInt(1+(int)(4*mMaterialAmount/U)));
	}

	@Override
	public net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] getEnchantments(ItemStack aStack, OreDictMaterial aMaterial) {
		return ZL_ENCHANTMENT;
	}

	@Override
	public int[] getEnchantmentLevels(ItemStack aStack, OreDictMaterial aMaterial) {
		return getEnchantmentLevels(aStack);
	}
	@Deprecated
	public int[] getEnchantmentLevels(ItemStack aStack) {
		return ZL_INTEGER;
	}

	// F18 achievements→advancements (подсистема, decisions/F18-achievements.md): AchievementList удалён; neo = data-driven
	// Advancements + CriteriaTriggers. GT6-достижение крафта инструмента = собственный CriteriaTrigger (advancement JSON +
	// регистрация триггера) — отдельная подсистема (task #26). no-op = текущее состояние до её постройки.
	@Override
	public void onToolCrafted(ItemStack aStack, Player aPlayer) {
		//
	}

	@Override
	public void onStatsAddedToTool(MultiItemTool aItem, int aID) {
		//
	}

	@Override
	public float getNormalDamageAgainstEntity(float aOriginalDamage, Entity aEntity, ItemStack aStack, Player aPlayer) {
		return aOriginalDamage;
	}

	@Override
	public float getMagicDamageAgainstEntity(float aOriginalDamage, Entity aEntity, ItemStack aStack, Player aPlayer) {
		return aOriginalDamage;
	}

	@Override
	public void afterDealingDamage(float aNormalDamage, float aMagicDamage, int aFireAspect, boolean aCriticalHit, Entity aEntity, ItemStack aStack, Player aPlayer) {
		if (aEntity instanceof LivingEntity && aFireAspect > 0) aEntity.igniteForSeconds(aFireAspect * 4);
		// F8 (1:1): 1.7.10 tKnockback = sprint + getKnockbackModifier(player,entity) (уровень Knockback-чары). neo-эквивалент —
		// EnchantmentHelper.modifyKnockback(sl,weapon,victim,source,base) [EnchantmentHelper.java:217]: base=0 → чистый вклад
		// Knockback-чары со стека. server-only (нужен ServerLevel). Было утеряно — восстановлено. Спринт-компонент 1:1.
		int tKnockback = (aPlayer.isSprinting()?1:0) + (aEntity instanceof LivingEntity && aEntity.level() instanceof net.minecraft.server.level.ServerLevel tSL ? (int)net.minecraft.world.item.enchantment.EnchantmentHelper.modifyKnockback(tSL, aStack, aEntity, tSL.damageSources().playerAttack(aPlayer), 0.0F) : 0);
		if (tKnockback > 0) {
			aEntity.push(-Mth.sin((float)(aPlayer.getYRot() * Math.PI / 180)) * tKnockback * 0.5, 0.1, Mth.cos((float)(aPlayer.getYRot() * Math.PI / 180)) * tKnockback * 0.5);
			Vec3 tMotion = aPlayer.getDeltaMovement();
			aPlayer.setDeltaMovement(tMotion.x * 0.6, tMotion.y, tMotion.z * 0.6);
			aPlayer.setSprinting(F);
		}
		// item-base impossible-1:1: Player.onCriticalHit/onEnchantmentCritical (1.7.10 КЛИЕНТ-feedback hooks) удалены —
		// крит-удар в 26.1.2 полностью внутренний server-расчёт, public override-точки для мод-кода нет. Крит-УРОН
		// применяется (tDamage*=1.5 в вызывателе); утрачен лишь клиент-визуал частиц → no-op ВЕРЕН.
		if (aEntity instanceof LivingEntity) Enchantments.applyBullshitA((LivingEntity)aEntity, aPlayer, aStack);
		Enchantments.applyBullshitB(aPlayer, aEntity, aStack);
		if (aEntity instanceof LivingEntity) aPlayer.awardStat(Stats.DAMAGE_DEALT, Math.round((aNormalDamage+aMagicDamage) * 10));
		// item-base: 1.7.10 Entity.hurtResistantTime → neo Entity.invulnerableTime (переименовано, поле ЕСТЬ) — контроль
		// частоты повторного удара инструментом. Восстановлено 1:1 (было ошибочно снято как «поле отсутствует»).
		aEntity.invulnerableTime = Math.max(1, getHurtResistanceTime(aEntity.invulnerableTime, aEntity));
		UT.Entities.exhaust(aPlayer, getExhaustionPerAttack(aEntity));
	}

	@Override
	public void afterBreaking(ItemStack aStack, Player aPlayer) {
		// If you work so hard that your Tool breaks, you should probably take a break yourself. :P
		UT.Entities.applyPotion(aPlayer, MobEffects.WEAKNESS.value()      ,  300, 2, F);
		UT.Entities.applyPotion(aPlayer, MobEffects.MINING_FATIGUE.value(), 1200, 2, F);
	}

	public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
		return Textures.ItemIcons.VOID;
	}

	public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
		return null;
	}

	@Override
	public int getRenderPasses() {
		return 4;
	}

	@Override
	public Identifier getIcon(ItemStack aStack, int aRenderPass) {
		switch(aRenderPass) {
		case 0: return getIcon(F, aStack).getIcon(0);
		case 1: return getIcon(F, aStack).getIcon(1);
		case 2: return getIcon(T, aStack).getIcon(0);
		case 3: return getIcon(T, aStack).getIcon(1);
		}
		return null;
	}

	@Override
	public short[] getRGBa(ItemStack aStack, int aRenderPass) {
		switch(aRenderPass) {
		case 0: return getRGBa(F, aStack);
		case 1: return UNCOLOURED;
		case 2: return getRGBa(T, aStack);
		case 3: return UNCOLOURED;
		}
		return UNCOLOURED;
	}
}
