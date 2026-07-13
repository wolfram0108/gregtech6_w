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
	public static final Enchantment[] LOOTING_ENCHANTMENT = new Enchantment[0];

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

	// PORT-TODO(F9, block-material 1.7.10 grass/tallgrass/double_plant identity): тройное растение-семейство
	// "tallgrass"/"double_plant" 1.7.10 расщеплено на отдельные Blocks-константы в современном ванильном
	// дереве (флэттенинг блоков), 1:1 без риска "выдуманной константы" не установить в рамках этого захода
	// (вне зоны item-базовых-классов) — деградация до "нет спец-дропа" (F), структура методов сохранена.
	public boolean harvestGrass(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
		return F;
	}

	// PORT-TODO(F9, block-material 1.7.10 grass/tallgrass/double_plant identity): см. harvestGrass выше, тот же класс.
	public boolean harvestStick(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableDurability, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
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
	public Enchantment[] getEnchantments(ItemStack aStack, OreDictMaterial aMaterial) {
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

	// PORT-TODO(item-base, achievements→advancements): AchievementList (1.7.10) удалён целиком — 26.1.2
	// использует data-driven Advancements/CriteriaTriggers, нет прямого API-триггера по имени из мод-кода
	// без собственных CriteriaTrigger-регистраций (отдельный шов, не item-base). Деградация до no-op.
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
		// PORT-TODO(F8, enchant-registry): EnchantmentHelper.getKnockbackModifier(Player,LivingEntity) (1.7.10
		// static lookup) удалён — knockback-зачарования теперь через ServerLevel+Holder-визитор
		// (EnchantmentHelper.runIterationOnEquipment), недоступный в этом статическом контексте. Спринт-компонент сохранён 1:1.
		int tKnockback = (aPlayer.isSprinting()?1:0);
		if (tKnockback > 0) {
			aEntity.push(-Mth.sin((float)(aPlayer.getYRot() * Math.PI / 180)) * tKnockback * 0.5, 0.1, Mth.cos((float)(aPlayer.getYRot() * Math.PI / 180)) * tKnockback * 0.5);
			Vec3 tMotion = aPlayer.getDeltaMovement();
			aPlayer.setDeltaMovement(tMotion.x * 0.6, tMotion.y, tMotion.z * 0.6);
			aPlayer.setSprinting(F);
		}
		// PORT-TODO(item-base, critical-hit hook): Player.onCriticalHit/onEnchantmentCritical (1.7.10 client
		// feedback hooks) удалены — критический удар полностью внутренний server-side расчёт в 26.1.2, нет
		// public override-точки для мод-кода. Деградация до no-op.
		if (aEntity instanceof LivingEntity) Enchantments.applyBullshitA((LivingEntity)aEntity, aPlayer, aStack);
		Enchantments.applyBullshitB(aPlayer, aEntity, aStack);
		if (aEntity instanceof LivingEntity) aPlayer.awardStat(Stats.DAMAGE_DEALT, Math.round((aNormalDamage+aMagicDamage) * 10));
		// PORT-TODO(item-base, hurtResistantTime): Entity.hurtResistantTime (1.7.10 hit-invulnerability-frames
		// поле) отсутствует в 26.1.2 (боевая модель урона переработана целиком) — нет 1:1-держателя в 3 корнях референса.
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
