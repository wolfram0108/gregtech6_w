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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.item.multiitem.tools;

import gregapi.item.multiitem.MultiItemTool;
import gregapi.oredict.OreDictMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * @author Gregorius Techneticies
 * 
 * The Stats for GT Tools. Not including any Material Modifiers.
 * 
 * And this is supposed to not have any ItemStack Parameters as these are generic Stats.
 */
public interface IToolStats {
	/**
	 * Called when aPlayer crafts this Tool
	 */
	public void onToolCrafted(ItemStack aStack, Player aPlayer);
	
	/**
	 * Called when this gets added to a Tool Item
	 */
	public void onStatsAddedToTool(MultiItemTool aItem, int aID);
	
	/**
	 * @return Damage the Tool receives when breaking a Block. 100 is one Damage Point (or 100 EU).
	 */
	public int getToolDamagePerBlockBreak();
	
	/**
	 * @return Damage the Tool receives when converting the drops of a Block. 100 is one Damage Point (or 100 EU).
	 */
	public int getToolDamagePerDropConversion();
	
	/**
	 * @return Damage the Tool receives when being used as Container Item. 100 is one use, however it is usually 8 times more than normal.
	 */
	public int getToolDamagePerContainerCraft();
	
	/**
	 * @return Damage the Tool receives when being used as Weapon, 200 is the normal Value, 100 for actual Weapons.
	 */
	public int getToolDamagePerEntityAttack();
	
	/**
	 * @return Basic Quality of the Tool, 0 is normal. If increased, it will increase the general quality of all Tools of this Type. Decreasing is also possible.
	 */
	public int getBaseQuality();
	
	/**
	 * @return The Damage Bonus for this Type of Tool against Mobs. 1.0F is normal punch.
	 */
	public float getBaseDamage();
	
	/**
	 * @return This gets the Hurt Resistance time for Entities getting hit. (always does 1 as minimum)
	 */
	public int getHurtResistanceTime(int aOriginalHurtResistance, Entity aEntity);
	
	/**
	 * @return The Exhaustion when using this as Weapon. 0.3F is Default.
	 */
	public float getExhaustionPerAttack(Entity aEntity);
	
	/**
	 * @return This is a multiplier for the Tool Speed. 1.0F = no special Speed.
	 */
	public float getSpeedMultiplier();
	
	/**
	 * @return This is a multiplier for the Tool Durability. 1.0F = no special Durability.
	 */
	public float getMaxDurabilityMultiplier();
	
	public DamageSource getDamageSource(LivingEntity aPlayer, Entity aEntity);
	
	public String getMiningSound();
	public String getCraftingSound();
	public String getEntityHitSound();
	public String getBreakingSound();
	
	public net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] getEnchantments(ItemStack aStack, OreDictMaterial aMaterial);
	public int[] getEnchantmentLevels(ItemStack aStack, OreDictMaterial aMaterial);
	
	/**
	 * @return If this Tool can instant collect Items.
	 */
	public boolean canCollect();
	
	/**
	 * @return If this Tool can be used for blocking Damage like a Sword.
	 */
	public boolean canBlock();
	
	/**
	 * @return If this Tool can be used as a RC Crowbar.
	 */
	public boolean isCrowbar();
	
	/**
	 * @return If this Tool can be used as a BC Wrench.
	 */
	public boolean isWrench();
	
	/**
	 * @return If this Tool can be used as a Forestry Grafter.
	 */
	public boolean isGrafter();
	
	/**
	 * @return If this Tool can be used as Weapon i.e. if that is the main purpose. (for enchants)
	 */
	public boolean isWeapon();
	
	/**
	 * @return If this Tool is a Ranged Weapon. Return false at isWeapon unless you have a Blade attached to your Bow/Gun or something
	 */
	public boolean isRangedWeapon();
	
	/**
	 * @return If this Tool can be used as Mining Tool i.e. if that is the main purpose. (for enchants)
	 */
	public boolean isMiningTool();
	
	/**
	 * @return If this Tools Damage does setDamageBypassesArmor() for its DamageSource.
	 */
	public boolean canPenetrate();
	
	/**
	 * @return If this Tool can make Mobs and Players drop Heads.
	 */
	public boolean canBehead();
	
	/**
	 * aBlock.getHarvestTool(aMetaData) can return the following Values for example.
	 * "chisel", "axe", "pickaxe", "sword", "shovel", "hoe", "grafter", "saw", "wrench", "monkeywrench", "crowbar", "file", "hammer", "plow", "plunger", "scoop", "screwdriver", "sense", "scythe", "softhammer", "cutter", "plasmatorch", "solderingtool"
	 * @return If this is a minable Block. Tool Quality checks (like Diamond Tier or something) are separate from this check.
	 */
	public boolean isMinableBlock(Block aBlock, byte aMetaData);
	
	/**
	 * aBlock.getHarvestTool(aMetaData) can return the following Values for example.
	 * "chisel", "axe", "pickaxe", "sword", "shovel", "hoe", "grafter", "saw", "wrench", "monkeywrench", "crowbar", "file", "hammer", "plow", "plunger", "scoop", "screwdriver", "sense", "scythe", "softhammer", "cutter", "plasmatorch", "solderingtool"
	 * @return Mining Speed for this Block from this Tool, 1.0 = Default Speed, 0.0 = cannot be mined. Tool Quality checks (like Diamond Tier or something) are separate from this check.
	 */
	public float getMiningSpeed(Block aBlock, byte aMetaData);
	
	/**
	 * @return Mining Speed for this Block from this Tool. Return aDefault if you don't want to override this.
	 */
	public float getMiningSpeed(Block aBlock, byte aMetaData, float aDefault, Player aPlayer, Level aWorld, int aX, int aY, int aZ);
	
	/**
	 * This lets you modify the Drop List, when this type of Tool has been used.
	 * @return the Amount of modified Items.
	 */
	public int convertBlockDrops(List<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, long aAvailableConversions, int aX, int aY, int aZ, byte aMetaData, int aFortune, boolean aSilkTouch);
	
	/**
	 * @return Returns a broken Version of the Item.
	 */
	public ItemStack getBrokenItem(ItemStack aStack);
	
	/**
	 * @return the Damage actually done to the Mob.
	 */
	public float getNormalDamageAgainstEntity(float aOriginalDamage, Entity aEntity, ItemStack aStack, Player aPlayer);
	
	/**
	 * @return the Damage actually done to the Mob.
	 */
	public float getMagicDamageAgainstEntity(float aOriginalDamage, Entity aEntity, ItemStack aStack, Player aPlayer);
	
	/**
	 * Gets called after successfully dealing Damage to a Mob.
	 */
	public void afterDealingDamage(float aNormalDamage, float aMagicDamage, int aFireAspect, boolean aCriticalHit, Entity aEntity, ItemStack aStack, Player aPlayer);
	
	/**
	 * Gets called right before the Tool gets removed from the Inventory.
	 */
	public void afterBreaking(ItemStack aStack, Player aPlayer);
	
	public int getRenderPasses();
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code IIcon getIcon(...)} (тип удалён в 26.1.2) — держатель ссылки на текстуру, тот же F3-канал что {@code gregapi.render.IIconContainer#getIcon(int)}. */
	public ResourceLocation getIcon(ItemStack aStack, int aRenderPass);
	public short[] getRGBa(ItemStack aStack, int aRenderPass);
}
