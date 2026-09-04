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

package gregapi.item.multiitem;
import net.minecraft.resources.ResourceKey;

import net.minecraftforge.api.distmarker.Dist;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.code.ItemStackSet;
import gregapi.code.ObjectStack;
import gregapi.code.TagData;
import gregapi.data.*;
import gregapi.data.TC.TC_AspectStack;
import gregapi.item.IItemEnergy;
import gregapi.item.IItemGTContainerTool;
import gregapi.item.IItemGTHandTool;
import gregapi.item.multiitem.energy.EnergyStat;
import gregapi.item.multiitem.tools.IToolStats;
import gregapi.old.Textures;
import gregapi.oredict.OreDictMaterial;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * This is an example on how you can create a Tool ItemStack, in this case a Bismuth Wrench:
 * gregapi.data.CS.ToolsGT.sMetaTool.getToolWithStats(CS.ToolIDs.WRENCH, 1, MT.Bismuth, MT.Bismuth, null);
 */
public class MultiItemTool extends MultiItem implements IItemGTHandTool, IItemGTContainerTool {
	public final HashMap<Short, IToolStats> mToolStats = new HashMap<>();
	
	public static BlockPos LAST_TOOL_COORDS_BEFORE_DAMAGE = null;
	
	/**
	 * Creates the Item using these Parameters.
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public MultiItemTool(String aModID, String aUnlocalized) {
		super(aModID, aUnlocalized);
		// BUG-021: stack=1 does NOT follow from durability — GT6 tools track it via their own NBT system
		// (getToolDamage/doDamage), no durability component involved; engine never forced it, tools stacked to 64. Restored 1:1 (original :88); hard limit enforced via getItemStackLimit=1 (:~754) through the MultiItem.getMaxStackSize bridge.
		setMaxStackSize(1);
		/*
		if (MD.BG2.mLoaded) try {
			UT.Reflection.callPublicMethod(Class.forName("mods.battlegear2.api.weapons.WeaponRegistry"), "addTwoHanded", make(0));
			UT.Reflection.callPublicMethod(Class.forName("mods.battlegear2.api.weapons.WeaponRegistry"), "addTwoHanded", make(W));
			UT.Reflection.callPublicMethod(Class.forName("mods.battlegear2.api.weapons.WeaponRegistry"), "setWeapon", "MainHand", make(0));
			UT.Reflection.callPublicMethod(Class.forName("mods.battlegear2.api.weapons.WeaponRegistry"), "setWeapon", "MainHand", make(W));
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		*/
	}
	
	/**
	 * This adds a Custom Item to the ending Range.
	 * @param aID The Id of the assigned Tool Class [0 - 32765] (only even Numbers allowed! Uneven ID's are empty electric Items)
	 * @param aEnglish The Default Localized Name of the created Item
	 * @param aToolTip The Default ToolTip of the created Item, you can also insert null for having no ToolTip
	 * @param aToolStats The Food Value of this Item. Can be null as well.
	 * @param aRandomParameters The OreDict Names you want to give the Item. Also used to assign Thaumcraft Aspects.
	 * @return An ItemStack containing the newly created Item, but without specific Stats.
	 */
	public final ItemStack addTool(int aID, String aEnglish, String aToolTip, IToolStats aToolStats, Object... aRandomParameters) {
		if (aToolTip == null) aToolTip = "";
		if (aID >= 0 && aID < 32766 && isUsableMeta((short)aID)) {
			LH.add(getUnlocalizedName() + "." +  aID                , aEnglish);
			LH.add(getUnlocalizedName() + "." +  aID    + ".tooltip", aToolTip);
			LH.add(getUnlocalizedName() + "." + (aID+1)             , aEnglish + " (Empty)");
			LH.add(getUnlocalizedName() + "." + (aID+1) + ".tooltip", "You need to recharge it");
			mToolStats.put((short) aID   , aToolStats);
			mToolStats.put((short)(aID+1), aToolStats);
			aToolStats.onStatsAddedToTool(this, aID);
			ItemStack rStack = ST.make(this, 1, aID);
			List<TC_AspectStack> tAspects = new ArrayListNoNulls<>();
			for (Object tRandomParameter : aRandomParameters) {
				if (tRandomParameter instanceof TC_AspectStack)
					((TC_AspectStack)tRandomParameter).addToAspectList(tAspects);
				else if (tRandomParameter instanceof ItemStackSet)
					((ItemStackSet<?>)tRandomParameter).add(rStack.copy());
				else
					OM.reg(tRandomParameter, rStack);
			}
			if (COMPAT_TC != null) COMPAT_TC.registerThaumcraftAspectsToItem(rStack, tAspects, F);
			return rStack;
		}
		return null;
	}
	
	/**
	 * This Function gets an ItemStack Version of this Tool
	 * @param aToolID the ID of the Tool Class
	 * @param aPrimaryMaterial Primary Material of this Tool
	 * @param aSecondaryMaterial Secondary (Rod/Handle) Material of this Tool
	 */
	public final ItemStack getToolWithStats(int aToolID, OreDictMaterial aPrimaryMaterial, OreDictMaterial aSecondaryMaterial) {
		return getToolWithStats(aToolID, 1, aPrimaryMaterial, aSecondaryMaterial);
	}
	
	/**
	 * This Function gets an ItemStack Version of this Tool
	 * @param aToolID the ID of the Tool Class
	 * @param aAmount Amount of Items (well normally you only need 1)
	 * @param aPrimaryMaterial Primary Material of this Tool
	 * @param aSecondaryMaterial Secondary (Rod/Handle) Material of this Tool
	 */
	public final ItemStack getToolWithStats(int aToolID, int aAmount, OreDictMaterial aPrimaryMaterial, OreDictMaterial aSecondaryMaterial) {
		return getToolWithStats(aToolID, aAmount, aPrimaryMaterial, aSecondaryMaterial, 0, 0);
	}
	
	/**
	 * This Function gets an ItemStack Version of this Tool
	 * @param aToolID the ID of the Tool Class
	 * @param aAmount Amount of Items (well normally you only need 1)
	 * @param aPrimaryMaterial Primary Material of this Tool
	 * @param aSecondaryMaterial Secondary (Rod/Handle) Material of this Tool
	 */
	public final ItemStack getToolWithStats(int aToolID, int aAmount, OreDictMaterial aPrimaryMaterial, OreDictMaterial aSecondaryMaterial, long aMaxCharge, long aVoltage) {
		return getToolWithStats(aToolID, aAmount, aPrimaryMaterial, aSecondaryMaterial, aMaxCharge, aVoltage, 0);
	}
	
	/**
	 * This Function gets an ItemStack Version of this Tool
	 * @param aToolID the ID of the Tool Class
	 * @param aAmount Amount of Items (well normally you only need 1)
	 * @param aPrimaryMaterial Primary Material of this Tool
	 * @param aSecondaryMaterial Secondary (Rod/Handle) Material of this Tool
	 */
	public final ItemStack getToolWithStats(int aToolID, int aAmount, OreDictMaterial aPrimaryMaterial, OreDictMaterial aSecondaryMaterial, long aMaxCharge, long aVoltage, long aCharge) {
		ItemStack rStack = ST.make(this, aAmount, aToolID);
		IToolStats tToolStats = getToolStats(rStack);
		if (tToolStats != null) {
			CompoundTag tMainNBT = UT.NBT.make(), tToolNBT = UT.NBT.make();
			if (aPrimaryMaterial != null) {
				if (aPrimaryMaterial.mID > 0) tToolNBT.putShort("a", aPrimaryMaterial.mID); else tToolNBT.putString("b", aPrimaryMaterial.toString());
				UT.NBT.setNumber(tToolNBT, "j", (long)((aPrimaryMaterial.mToolDurability * 100L) * tToolStats.getMaxDurabilityMultiplier()));
			}
			if (aSecondaryMaterial != null) {
				if (aSecondaryMaterial.mID > 0) tToolNBT.putShort("c", aSecondaryMaterial.mID); else tToolNBT.putString("d", aSecondaryMaterial.toString());
			}
			if (aMaxCharge > 0) {
				tToolNBT.putBoolean("e", T);
				UT.NBT.setNumber(tToolNBT, "f", aMaxCharge);
				UT.NBT.setNumber(tToolNBT, "g", aVoltage);
			}
			tMainNBT.put("GT.ToolStats", tToolNBT);
			UT.NBT.set(rStack, tMainNBT);
			if (aCharge > 0 && aMaxCharge > 0) for (TagData tEnergyType : getEnergyTypes(rStack)) setEnergyStored(tEnergyType, rStack, Math.min(aCharge, aMaxCharge));
		}
		isItemStackUsable(rStack);
		return rStack;
	}
	
	/**
	 * Called by the Block Harvesting Event within the GT_Proxy
	 */
	public void onHarvestBlockEvent(ArrayList<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, int aX, int aY, int aZ, byte aMeta, int aFortune, boolean aSilkTouch) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null || ST.instaharvest(aBlock, aMeta) || !isItemStackUsable(aStack) || getDigSpeed(aStack, aBlock, aMeta) <= 0) {
			doDamage(aStack, 0, aPlayer, T);
			return;
		}
		long tDamage = tStats.convertBlockDrops(aDrops, aStack, aPlayer, aBlock, (getToolMaxDamage(aStack) - getToolDamage(aStack)) / tStats.getToolDamagePerDropConversion(), aX, aY, aZ, aMeta, aFortune, aSilkTouch);
		// 1:1 with 1.7.10 setBlockToAir: set AIR explicitly — neo removeBlock leaves the cell's fluid behind
		// (Level.java:296-298, fluidState.createLegacyBlock()); ice may already be water by this point (same bug class as CoverDrain).
		if (aBlock == Blocks.ICE && !aDrops.isEmpty()) aPlayer.level().setBlock(new BlockPos(aX, aY, aZ), Blocks.AIR.defaultBlockState(), 3);
		if (WD.dimBTL(aPlayer.level()) && !getPrimaryMaterial(aStack).contains(TD.Properties.BETWEENLANDS)) tDamage *= 4;
		doDamage(aStack, tDamage * tStats.getToolDamagePerDropConversion(), aPlayer, T);
	}
	
	public boolean canCollectDropsDirectly(ItemStack aStack) {
		IToolStats tStats = getToolStats(aStack);
		return (tStats.canCollect() || getPrimaryMaterial(aStack).contains(TD.Properties.AUTO_COLLECTING) || getSecondaryMaterial(aStack).contains(TD.Properties.AUTO_COLLECTING)) && isItemStackUsable(aStack);
	}
	public boolean canCollectDropsDirectly(ItemStack aStack, Block aBlock, byte aMeta) {
		if (ST.instaharvest(aBlock, aMeta)) return T;
		return canCollectDropsDirectly(aStack) && getDigSpeed(aStack, aBlock, aMeta) > 0;
	}
	
	public float onBlockBreakSpeedEvent(float aDefault, ItemStack aStack, Player aPlayer, Block aBlock, int aX, int aY, int aZ, byte aMeta, PlayerEvent.BreakSpeed aEvent) {
		// Yeah no Bedrock breaking with these Tools.
		if (aBlock == NB || WD.bedrock(aBlock)) return aDefault;
		// Things that are normally harvested instantly, like Torches for example.
		if (ST.instaharvest(aBlock, aMeta)) return Float.MAX_VALUE;
		// BUG-071 link 2 (link 1 = drop eligibility, GT_API_Proxy.onPlayerHarvestCheckEvent): insufficient level gave
		// ZERO speed in 1.7.10 (original getDigSpeed:482) — block didn't break at all, not "breaks without drop". Same check here, but level comes from the positional WD.harvestLevel center: getDestroySpeed(stack,state) has no position (prefix/MTE meta is repurposed), the BreakSpeed event does.
		IToolStats tStatsLevel = getToolStats(aStack);
		if (tStatsLevel == null || tStatsLevel.getBaseQuality() + getPrimaryMaterial(aStack).mToolQuality
			< UT.Code.bind4(WD.harvestLevel(aPlayer.level(), aX, aY, aZ))) return 0;
		// special case for Obsidian to be mined faster with higher Quality Pickaxes.
		if (OD.obsidian.is(ST.make(aBlock, 1, aMeta))) aDefault *= Math.max(1, getPrimaryMaterial(aStack).mToolQuality - 2);
		// and now the basic Tool Stats.
		IToolStats tStats = getToolStats(aStack);
		return tStats == null ? aDefault : tStats.getMiningSpeed(aBlock, aMeta, aDefault, aPlayer, aPlayer.level(), aX, aY, aZ);
	}
	
	@Override
	public boolean onLeftClickEntity(ItemStack aStack, Player aPlayer, Entity aEntity) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null || !isItemStackUsable(aStack)) return T;
		// play(String,int,float,Entity) vs play(String,int,float,BlockPos) is ambiguous to the compiler at this call
		// site (gregapi/util/UT.java, out of scope here) — use the coordinate overload instead, same effect since the Entity overload calls it internally anyway.
		if (TOOL_SOUNDS) UT.Sounds.forActor(tStats.getEntityHitSound(), 20, 1, aPlayer, UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ())); // BUG-113: hitEntity only runs server-side
		if (super.onLeftClickEntity(aStack, aPlayer, aEntity)) return T;
		// 1.7.10 Entity.canAttackWithItem() -> neo Entity.isAttackable() (whether the entity can be attacked). Capability present, 1:1.
		if (aEntity.isAttackable()) {
			int tImplosion = UT.NBT.getEnchantmentLevelImplosion(aStack);
			// F8 (1:1): 1.7.10 EnchantmentHelper.getFireAspectModifier(aPlayer) — Fire Aspect level on the weapon.
			// Enchantments.FIRE_ASPECT in neo is a ResourceKey (not removed); ported UT.NBT.getEnchantmentLevel reads it from the stack.
			int tFireAspect = UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, aStack);
			boolean tIgnitesFire = !aEntity.isOnFire() && tFireAspect > 0 && aEntity instanceof LivingEntity;
			if (tIgnitesFire) aEntity.setSecondsOnFire(1);
			if (aEntity.skipAttackInteraction(aPlayer)) {
				if (tIgnitesFire) aEntity.clearFire();
			} else {
				// F8 (1:1): 1st arg is the enchant damage bonus (Sharpness/Smite/Bane) vs the victim — 1.7.10
				// getEnchantmentModifierLiving(aPlayer,entity), ported as UT.Enchantments.getDamageBonusVsCreature (same center used by EntityArrow_Material/Behavior_Gun). Was 0 before.
				float tMagicDamage = tStats.getMagicDamageAgainstEntity(aEntity instanceof LivingEntity ? UT.Enchantments.getDamageBonusVsCreature(aStack, aEntity) : 0, aEntity, aStack, aPlayer), tDamage = tStats.getNormalDamageAgainstEntity((float)aPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE) + getToolCombatDamage(aStack), aEntity, aStack, aPlayer);
				// Also work on Ghasts and such. But no double dipping on Anti Creeper Damage!
				if (tImplosion > 0 && UT.Entities.isExplosiveCreature(aEntity) && !Creeper.class.isInstance(aEntity)) tMagicDamage += 1.5F * tImplosion;

				if (tDamage + tMagicDamage > 0) {
					// 1.7.10 Entity.hurtResistantTime -> neo Entity.invulnerableTime (same hit-invulnerability field, renamed). Capability present, 1:1.
					boolean tRealHit = (!aEntity.level().isClientSide() || aEntity.invulnerableTime <= 0);
					boolean tCriticalHit = aPlayer.fallDistance > 0 && !aPlayer.onGround() && !aPlayer.onClimbable() && !aPlayer.isInWater() && !aPlayer.hasEffect(MobEffects.BLINDNESS) && aPlayer.getVehicle() == null && aEntity instanceof LivingEntity;
					if (tCriticalHit && tDamage > 0) tDamage *= 1.5;
					float tFullDamage = (tDamage+tMagicDamage) * TFC_DAMAGE_MULTIPLIER;
					DamageSource tSource = tStats.getDamageSource(aPlayer, aEntity);
					if (tStats.canPenetrate() && tSource instanceof gregapi.damage.DamageSources.GregTechDamageSource) ((gregapi.damage.DamageSources.GregTechDamageSource)tSource).setDamageBypassesArmor();
					// Avoiding the Betweenlands Damage Cap of 40 in a fair way.
					// Only Betweenlands Materials will avoid it. And maybe some super Lategame Materials.
					// 1.7.10 attackEntityFrom ran on BOTH sides (client = prediction). neo equivalent hurtOrSimulate
					// dispatches itself (ServerLevel->hurtServer / ClientLevel->hurtClient, Entity.java:1835); a direct (ServerLevel) cast threw CCE client-side (BUG-003) — tRealHit above is true client-side too.
					if (tRealHit && MD.BTL.mLoaded && aEntity.getClass().getName().startsWith("thebetweenlands") && getPrimaryMaterial(aStack).contains(TD.Properties.BETWEENLANDS)) {
						float tDamageToDeal = tFullDamage;
						while (tDamageToDeal > 0 && aEntity.hurt(tSource, Math.min(tDamageToDeal, 12) / 0.3F)) {
							tDamageToDeal -= 12;
							if (tDamageToDeal > 0) aEntity.invulnerableTime = 0; // 1.7.10 hurtResistantTime=0 (dropped in the port) — reset invuln frames so the next 12-dmg hit lands (bypasses BTL's 40 cap); invulnerableTime is the renamed field
						}
						tRealHit &= (tDamageToDeal < tFullDamage);
					} else if (tRealHit) {
						tRealHit &= aEntity.hurt(tSource, tFullDamage);
					}
					// Only damage the Tool and perform its Specials, when you actually do hit the thing.
					// So Serverside always, and Clientside only if the Mob isn't in its invulnerability Frames.
					if (tRealHit) {
						tStats.afterDealingDamage(tDamage, tMagicDamage, tFireAspect, tCriticalHit, aEntity, aStack, aPlayer);
						doDamage(aStack, tStats.getToolDamagePerEntityAttack(), aPlayer, F);
					}
				}
			}
		}
		return T;
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {
		IToolStats tStats = getToolStats(aStack);
		// item-base: neo's use model is startUsingItem(InteractionHand) + duration from getUseDuration (below) —
		// no explicit setItemInUse(stack,72000) needed, it's declarative now. Not a stub.
		return super.onItemRightClick(aStack, aWorld, aPlayer);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack aStack) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && tStats.canBlock()) return UseAnim.BLOCK;
		return UseAnim.NONE;
	}
	@Override
	public int getUseDuration(ItemStack aStack) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && tStats.canBlock()) return 72000;
		return 0;
	}
	
	// F13/F16 creative-tab: getSubItems kept 1:1 as the variant enumerator; WIRED UP — CreativeTabsGT.populate
	// calls it reflectively from the GT tab's displayItems generator. Not a stub.
	@SuppressWarnings("unchecked")
	public final void getSubItems(Item var1, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {
		for (int i = 0; i < 32766; i+=2) if (getToolStats(ST.make(this, 1, i)) != null) {
			ItemStack tStack = ST.make(this, 1, i);
			isItemStackUsable(tStack);
			aList.add(tStack);
		}
	}
	
	@Override
	public void addAdditionalToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		long tMaxDamage = getToolMaxDamage(aStack), tDamage = getToolDamage(aStack);
		OreDictMaterial tMat1 = getPrimaryMaterial(aStack), tMat2 = getSecondaryMaterial(aStack);
		IToolStats tStats = getToolStats(aStack);
		if (tMaxDamage > 0 && tStats != null) {
			if (tMat1 == MT.NULL) {
				aList.add(LH.Chat.WHITE + LH.tt("Durability: x") + LH.Chat.GREEN + tStats.getMaxDurabilityMultiplier());
				aList.add(LH.Chat.WHITE + LH.tt("Level: +") + LH.Chat.YELLOW + tStats.getBaseQuality());
				float tCombat = getToolCombatDamage(aStack);
				aList.add(LH.Chat.WHITE + LH.tt("Melee Damage: +") + LH.Chat.BLUE + (tCombat * TFC_DAMAGE_MULTIPLIER) + LH.Chat.RED + " (= " + (TFC_DAMAGE_MULTIPLIER > 1 ? ((tCombat+1)*(TFC_DAMAGE_MULTIPLIER/2.0)) + ")" : ((tCombat+1)/2) + LH.tt(" Hearts)")));
				aList.add(LH.Chat.WHITE + LH.tt("Mining Speed: x") + LH.Chat.PINK + tStats.getSpeedMultiplier());
				if (tStats.canCollect()) aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_AUTOCOLLECT));
				if (tStats.canPenetrate()) aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_ARMOR_PENETRATING));
			} else {
				aList.add(LH.Chat.WHITE + LH.tt("Durability: ") + LH.Chat.GREEN + UT.Code.makeString(tMaxDamage - tDamage) + " / " + UT.Code.makeString(tMaxDamage));
				aList.add(LH.Chat.WHITE + tMat1.getLocal() + LH.Chat.YELLOW + LH.tt(" Level: ") + (tStats.getBaseQuality() + tMat1.mToolQuality));
				float tCombat = getToolCombatDamage(aStack);
				aList.add(LH.Chat.WHITE + LH.tt("Melee Damage: ") + LH.Chat.BLUE + "+" + (tCombat * TFC_DAMAGE_MULTIPLIER) + LH.Chat.RED + " (= " + (TFC_DAMAGE_MULTIPLIER > 1 ? ((tCombat+1)*(TFC_DAMAGE_MULTIPLIER/2.0)) + ")" : ((tCombat+1)/2) + LH.tt(" Hearts)")));
				aList.add(LH.Chat.WHITE + LH.tt("Mining Speed: ") + LH.Chat.PINK + Math.max(Float.MIN_NORMAL, tStats.getSpeedMultiplier() * tMat1.mToolSpeed));
				aList.add(LH.Chat.WHITE + LH.tt("Crafting Uses: ") + LH.Chat.GREEN + UT.Code.divup(getEnergyStats(aStack) == null ? tMaxDamage - tDamage : Math.min(getEnergyStored(TD.Energy.EU, aStack), getEnergyCapacity(TD.Energy.EU, aStack)), tStats.getToolDamagePerContainerCraft()));
				if (MD.BTL.mLoaded && tMat1.contains(TD.Properties.BETWEENLANDS)) aList.add(LH.Chat.GREEN + LH.get(LH.TOOLTIP_BETWEENLANDS_RESISTANCE));
				if (MD.TF .mLoaded && tMat1.contains(TD.Properties.MAZEBREAKER)) {
					if (canHarvestBlock(IL.TF_Mazestone.block(), aStack)) aList.add(LH.Chat.PINK + LH.get(LH.TOOLTIP_TWILIGHT_MAZE_STONE_BREAKING));
					if (canHarvestBlock(IL.TF_Mazehedge.block(), aStack)) aList.add(LH.Chat.PINK + LH.get(LH.TOOLTIP_TWILIGHT_MAZE_HEDGE_BREAKING));
					if (canHarvestBlock(IL.TF_Towerwood.block(), aStack)) aList.add(LH.Chat.PINK + LH.get(LH.TOOLTIP_TWILIGHT_TOWER_WOOD_BREAKING));
				}
				if (tMat1.contains(TD.Properties.UNBURNABLE) || tMat2.contains(TD.Properties.UNBURNABLE)) aList.add(LH.Chat.GREEN + LH.get(LH.TOOLTIP_UNBURNABLE));
				if (tStats.canCollect() || tMat1.contains(TD.Properties.AUTO_COLLECTING) || tMat2.contains(TD.Properties.AUTO_COLLECTING)) aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_AUTOCOLLECT));
				if (tStats.canPenetrate()) aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_ARMOR_PENETRATING));
			}
		}
	}
	
	public static final OreDictMaterial getPrimaryMaterial(ItemStack aStack) {return getPrimaryMaterial(aStack, MT.NULL);}
	public static final OreDictMaterial getPrimaryMaterial(ItemStack aStack, OreDictMaterial aDefault) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompound("GT.ToolStats");
			if (!aNBT.isEmpty()) {
				if (aNBT.contains("a")) return OreDictMaterial.get(aNBT.getShort("a"), aDefault);
				if (aNBT.contains("b")) return OreDictMaterial.get(aNBT.getString("b"), aDefault);
			}
		}
		return aDefault;
	}

	public static final OreDictMaterial getSecondaryMaterial(ItemStack aStack) {return getSecondaryMaterial(aStack, MT.NULL);}
	public static final OreDictMaterial getSecondaryMaterial(ItemStack aStack, OreDictMaterial aDefault) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompound("GT.ToolStats");
			if (!aNBT.isEmpty()) {
				if (aNBT.contains("c")) return OreDictMaterial.get(aNBT.getShort("c"), aDefault);
				if (aNBT.contains("d")) return OreDictMaterial.get(aNBT.getString("d"), aDefault);
			}
		}
		return aDefault;
	}

	@Override
	public IItemEnergy getEnergyStats(ItemStack aStack) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompound("GT.ToolStats");
			if (!aNBT.isEmpty()) {
				if (aNBT.getBoolean("e")) return EnergyStat.makeTool(TD.Energy.EU, aNBT.getLong("f"), aNBT.getLong("g"), 64, ST.make(this, 1, getUnusableMeta(aStack)), ST.make(this, 1, getUsableMeta(aStack)), ST.make(this, 1, getUsableMeta(aStack)));
			}
		}
		return null;
	}

	public float getToolCombatDamage(ItemStack aStack) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return 0;
		return tStats.getBaseDamage() + getPrimaryMaterial(aStack).mToolQuality;
	}

	public static final long getToolMaxDamage(ItemStack aStack) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompound("GT.ToolStats");
			if (aNBT.contains("j")) return Math.max(1, aNBT.getLong("j"));
			return Math.max(1, aNBT.getLong("MaxDamage"));
		}
		return 1;
	}
	public static final long getToolDamage(ItemStack aStack) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompound("GT.ToolStats");
			if (aNBT.contains("k")) return aNBT.getLong("k");
			return aNBT.getLong("Damage");
		}
		return 0;
	}
	// F8-nbt: the original mutated the stack's live tag (1.7.10 getTagCompound = reference). Under the ItemNBT bridge
	// (neo CustomData copies the tag on get()), the mutation must be written back: get(copy) -> mutate -> put sub-tag -> ItemNBT.set(aStack, ...), else it's a no-op and tools never wear (doDamage:464). See decisions/F8-nbt-data-components.md §7.
	public static final boolean setToolDamage(ItemStack aStack, long aDamage) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			CompoundTag tStats = aNBT.getCompound("GT.ToolStats");
			UT.NBT.setNumber(tStats, "k", aDamage);
			aNBT.put("GT.ToolStats", tStats);
			ItemNBT.set(aStack, aNBT);
			return T;
		}
		return F;
	}
	
	@Override
	public boolean destroyCheck(ItemStack aStack, Player aPlayer) {
		if (getToolDamage(aStack) >= getToolMaxDamage(aStack)) {
			doDamage(aStack, 0, aPlayer, T);
			return T;
		}
		return super.destroyCheck(aStack, aPlayer);
	}
	
	public boolean doDamage(ItemStack aStack, long aAmount) {return doDamage(aStack, aAmount, null, T);}
	public boolean doDamage(ItemStack aStack, long aAmount, LivingEntity aPlayer) {return doDamage(aStack, aAmount, aPlayer, T);}
	public boolean doDamage(ItemStack aStack, long aAmount, LivingEntity aPlayer, boolean aAllowBreaking) {
		if (UT.Entities.hasInfiniteItems(aPlayer)) return T;
		if (!isItemStackUsable(aStack)) return F;
		IItemEnergy tElectric = getEnergyStats(aStack);
		if (tElectric == null || RNGSUS.nextInt(Math.max(10, getPrimaryMaterial(aStack).mToolQuality * 20)) == 0) {
			long tNewDamage = getToolDamage(aStack) + aAmount;
			setToolDamage(aStack, tNewDamage);
			if (aAllowBreaking && tNewDamage >= getToolMaxDamage(aStack)) {
				IToolStats tStats = getToolStats(aStack);
				if (tStats == null) {
					ST.use(aPlayer, T, aStack);
				} else {
					if (TOOL_SOUNDS) {
						String tBreakSound = getPrimaryMaterial(aStack) == MT.NULL ? tStats.getCraftingSound() : tStats.getBreakingSound();
						// BUG-113: the "player present / no player" branch is no longer written inline — a central helper owns it
						if (aPlayer == null) UT.Sounds.play(tBreakSound, 100, 1, LAST_TOOL_COORDS_BEFORE_DAMAGE);
						else UT.Sounds.forActor(tBreakSound, 100, 1, aPlayer, UT.Code.roundDown(aPlayer.getX()), UT.Code.roundDown(aPlayer.getY()), UT.Code.roundDown(aPlayer.getZ()));
					}
					LAST_TOOL_COORDS_BEFORE_DAMAGE = null;
					ItemStack tBroken = tStats.getBrokenItem(aStack);
					if (ST.invalid(tBroken) || tBroken.getCount() <= 0) {
						ST.use(aPlayer, T, aStack);
					} else if (aPlayer instanceof Player) {
						if (tBroken.getCount() > 64) tBroken.setCount(64);
						if (!aPlayer.level().isClientSide()) ST.give(aPlayer, tBroken, F);
						ST.use(aPlayer, T, aStack);
					} else {
						if (tBroken.getCount() > 64) tBroken.setCount(64);
						ST.set(aStack, tBroken);
					}
				}
			}
			return tElectric == null || useEnergy(TD.Energy.EU, aStack, aAmount, aPlayer, null, null, 0, 0, 0, T);
		}
		return useEnergy(TD.Energy.EU, aStack, aAmount, aPlayer, null, null, 0, 0, 0, T);
	}
	
	// F9-tool bridge (principle 4): getDigSpeed/canHarvestBlock/getHarvestLevel/onBlockDestroyed are GT6-internal
	// domain logic wired to real engine mining via the WD.harvestLevel/WD.hardness centers. getDestroySpeed/isCorrectToolForDrops have no position (F13: BlockState carries no numeric meta) so they use meta 0, same as GT6-canHarvestBlock:518 (matches original:487-488); mineBlock has a position, so onBlockDestroyed gets the real meta via WD.meta(world,pos).
	/**
	 * ⛔ {@link #getDigSpeed} GOES HERE — AND THIS IS 1:1, VERIFIED BY A PAIRED MEASUREMENT (2026-08-06).
	 *
	 * <p><b>There was a bug here that cost a mass divergence — kept as a warning.</b> I checked
	 * that the 1.7.10 mod did not override {@code Item.func_150893_a}, and concluded the engine's speed
	 * channel stayed vanilla (1.0F), with the logic living only in the {@code BreakSpeed} event. The conclusion was wrong: the
	 * 1.7.10 engine takes its base NOT from there, but from the Forge method {@code Item.getDigSpeed(stack, block, meta)} —
	 * {@code EntityPlayer.getBreakSpeed:914} ({@code stack.getItem().getDigSpeed(...)}), and this is exactly the method
	 * {@code MultiItemTool} overrides ({@code MultiItemTool.java:472} of the original).</p>
	 *
	 * <p>End-to-end paired measurement ("destruction fraction per tick", {@code ForgeHooks.blockStrength} against
	 * {@code BlockState.getDestroyProgress}, 1360 shared "block x tool" pairs): the original has <b>1176 zeroes</b> —
	 * an unsuitable tool does not mine the block AT ALL. The vanilla-base edit gave 0 zeroes and matched the
	 * original on only <b>1.47%</b>. Reverting to {@code getDigSpeed} is the only thing that gives 1:1.</p>
	 *
	 * <p>This also explains the Jade tooltip case: it stays silent at zero speed — but that is the ORIGINAL's canon,
	 * not a port defect. Fix the tooltip with a tooltip fix ({@code Compat_Jade}), don't touch the mechanic.</p>
	 */
	@Override public float getDestroySpeed(ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState) {
		return getDigSpeed(aStack, aState.getBlock(), 0);
	}
	@Override public boolean isCorrectToolForDrops(ItemStack aStack, net.minecraft.world.level.block.state.BlockState aState) {
		return canHarvestBlock(aState.getBlock(), aStack);
	}
	@Override public boolean mineBlock(ItemStack aStack, Level aWorld, net.minecraft.world.level.block.state.BlockState aState, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.LivingEntity aPlayer) {
		// F13 contract (BUG-016): in 1.7.10 onBlockDestroyed ran BEFORE removeBlock (meta still in world); in neo
		// mineBlock runs AFTER — meta comes from the aState snapshot, not the world (already air there).
		return onBlockDestroyed(aStack, aWorld, aState.getBlock(), aPos.getX(), aPos.getY(), aPos.getZ(), aPlayer, WD.meta(aState));
	}
	public float getDigSpeed(ItemStack aStack, Block aBlock, int aMeta) {
		if (aBlock == NB || WD.bedrock(aBlock)) return 0;
		if (ST.instaharvest(aBlock, aMeta)) return 10;
		if (!isItemStackUsable(aStack)) return 0;
		// Required because a combination of Twilight Forest and Block Metadata Extenders can fuck this up and give me values like 49 for vanilla Blocks.
		if (aMeta > 15 && (gregapi.data.CS.Flattened.headOf(aBlock) == Blocks.DIRT || aBlock == Blocks.GRASS_BLOCK || aBlock == Blocks.STONE)) aMeta = 0;
		float tMultiplier = 1.0F;
		OreDictMaterial tMaterial = getPrimaryMaterial(aStack);
		if ((IL.TF_Mazestone.equal(aBlock) || IL.TF_Mazehedge.equal(aBlock) || IL.TF_Towerwood.equal(aBlock)) && tMaterial.contains(TD.Properties.MAZEBREAKER)) tMultiplier *= 40;
		IToolStats tStats = getToolStats(aStack);
		// original:482 ... < UT.Code.bind4(aBlock.getHarvestLevel(aMeta)). Capability exists via the WD.harvestLevel
		// center (GT6 block -> BlockBase.getHarvestLevel, vanilla -> neo NEEDS_*_TOOL tags). Had falsely degraded to 0 (any tool mined anything) — restored 1:1.
		if (tStats == null || tStats.getBaseQuality() + tMaterial.mToolQuality < UT.Code.bind4(WD.harvestLevel(aBlock, aMeta))) return 0;
		return tStats.getMiningSpeed(aBlock, (byte)aMeta) * Math.max(Float.MIN_NORMAL, tStats.getSpeedMultiplier() * tMultiplier * tMaterial.mToolSpeed);
	}

	public final boolean canHarvestBlock(Block aBlock, ItemStack aStack) {
		return IL.TC_Block_Air.equal(aBlock) || MD.CARP.owns(aBlock) || getDigSpeed(aStack, aBlock, (byte)0) > 0;
	}

	/**
	 * DOES THIS TOOL CONSIDER THE BLOCK ITS OWN — regardless of level and regardless of whether it has enough power.
	 *
	 * <p>Difference from {@link #canHarvestBlock}: that one answers "will it harvest it RIGHT NOW" and thus includes the
	 * quality threshold ({@code getDigSpeed} returns 0 for a weak tool). This one asks only the tool's own rule
	 * ({@code IToolStats.isMinableBlock} — the same thing GT6 uses in-game to decide): "is this even my
	 * business at all?". This is exactly what tooltip displays need, which must show the tool icon regardless of
	 * what the player is holding.
	 *
	 * <p>The channel is set up here, not at the consumer: the rule belongs to the tool, and reaching in from outside via
	 * {@code getToolStats(...).isMinableBlock(...)} is a break of encapsulation. No such call exists anywhere in the mod.
	 */
	public final boolean isMinableBlock(ItemStack aStack, Block aBlock, byte aMeta) {
		IToolStats tStats = getToolStats(aStack);
		return tStats != null && tStats.isMinableBlock(aBlock, aMeta);
	}

	public final int getHarvestLevel(ItemStack aStack, String aToolClass) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return -1;
		int rValue = tStats.getBaseQuality() + getPrimaryMaterial(aStack).mToolQuality;
		return rValue < 15 ? rValue : Integer.MAX_VALUE;
	}

	public boolean onBlockDestroyed(ItemStack aStack, Level aWorld, Block aBlock, int aX, int aY, int aZ, LivingEntity aPlayer) {
		return onBlockDestroyed(aStack, aWorld, aBlock, aX, aY, aZ, aPlayer, WD.meta(aWorld, aX, aY, aZ)); // 1.7.10 signature kept
	}
	public boolean onBlockDestroyed(ItemStack aStack, Level aWorld, Block aBlock, int aX, int aY, int aZ, LivingEntity aPlayer, byte aMeta) {
		if (ST.instaharvest(aBlock) || UT.Entities.hasInfiniteItems(aPlayer)) return T;
		if (!isItemStackUsable(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return F;
		if (TOOL_SOUNDS) UT.Sounds.forActor(tStats.getMiningSound(), 5, 1, aPlayer, aX, aY, aZ); // BUG-113: mineBlock only runs server-side
		String aRegName = ST.regName(aBlock);
		boolean rReturn = (getDigSpeed(aStack, aBlock, aMeta) > 0);
		// original: * aBlock.getBlockHardness(aWorld,aX,aY,aZ). Capability exists via the WD.hardness center
		// (BlockState.getDestroySpeed, WD.java:360). Had falsely degraded to 1.0 — restored 1:1.
		double tDamage = tStats.getToolDamagePerBlockBreak() * WD.hardness(aBlock, aWorld, aX, aY, aZ);
		OreDictMaterial aMat1 = getPrimaryMaterial(aStack);
		if (WD.dimBTL(aWorld) && !aMat1.contains(TD.Properties.BETWEENLANDS)) tDamage *= 4;
		if (MD.TFC.owns(aRegName) || MD.TFCP.owns(aRegName)) {
			tDamage /= 4;
		} else {
			if (IL.TF_Mazestone.equal(aBlock)) if (aMat1.contains(TD.Properties.MAZEBREAKER)) tDamage /= 40; else tDamage *= 16;
			if (IL.TF_Mazehedge.equal(aBlock)) {
				if (aMat1.contains(TD.Properties.MAZEBREAKER)) tDamage /= 40; else tDamage *= 16;
				// F8 (1:1): original UT.NBT.getEnchantmentLevel(Enchantment.silkTouch, aStack) <= 0 gates the special
				// Mazehedge drop to non-silk-touch only. Enchantments.SILK_TOUCH in neo is a ResourceKey (not removed); ported UT.NBT.getEnchantmentLevel (UT.java:2257) reads it. Gate restored: silk-touch suppresses the special drop.
				if (!aWorld.isClientSide() && UT.NBT.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, aStack) <= 0) {
					if (aPlayer instanceof Player && canCollectDropsDirectly(aStack, aBlock, aMeta)) {
						ST.give(aPlayer, IL.TF_Mazehedge.get(1), aWorld, aX, aY, aZ);
					} else {
						ST.drop(aWorld, aX, aY, aZ, IL.TF_Mazehedge.get(1));
					}
				}
			}
		}
		doDamage(aStack, UT.Code.roundUp(tDamage), aPlayer, F);
		return rReturn;
	}
	
	@Override
	public ItemStack getContainerItemDefault(ItemStack aStack) {
		if (!isUsableMeta(aStack)) return null;
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return null;
		// Same case as PrefixItem: the crafting path is server-side, but the engine hands us the crafting player
		// (CommonHooks, ResultSlot.java:89-91) — take it from there instead of dropping the sound.
		if (TOOL_SOUNDS) {
			net.minecraft.world.entity.player.Player tCrafter = net.minecraftforge.common.ForgeHooks.getCraftingPlayer();
			if (tCrafter != null) UT.Sounds.forActor(tStats.getCraftingSound(), 200, 1, tCrafter, UT.Code.roundDown(tCrafter.getX()), UT.Code.roundDown(tCrafter.getY()), UT.Code.roundDown(tCrafter.getZ()));
			else UT.Sounds.play(tStats.getCraftingSound(), 200, 1, LAST_TOOL_COORDS_BEFORE_DAMAGE);
		}
		aStack = ST.amount(1, aStack);
		doDamage(aStack, tStats.getToolDamagePerContainerCraft(), null, T);
		return aStack.getCount() > 0 ? aStack : null;
	}
	
	@Override
	public boolean hasContainerItem(ItemStack aStack) {
		if (!isUsableMeta(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return F;
		aStack = ST.amount(1, aStack);
		doDamage(aStack, tStats.getToolDamagePerContainerCraft(), null, T);
		return aStack.getCount() > 0;
	}
	
	@Override
	public void onCreated(ItemStack aStack, Level aWorld, Player aPlayer) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && aPlayer != null) tStats.onToolCrafted(aStack, aPlayer);
		super.onCreated(aStack, aWorld, aPlayer);
	}
	
	@Override
	public void updateItemStack(ItemStack aStack) {
		super.updateItemStack(aStack);
		destroyCheck(aStack, null);
	}
	
	@Override
	public boolean isItemStackUsable(ItemStack aStack) {
		if (aStack.getCount() <= 0) return F;

		// F8-nbt: aNBT.remove("ench")/put("ench",...) mutate a SNAPSHOT (neo CustomData copies the tag on get()) — must be
		// written back via ItemNBT.set, else the "ench" marker never persists and enchants get reapplied every call. See decisions/F8-nbt-data-components.md §7.
		CompoundTag aNBT = ItemNBT.get(aStack);
		// The Tool has no Data? Treat it like a single use Creative Tool.
		if (aNBT == null) return T;

		// Invalid Tool Index?
		if (!isUsableMeta(aStack)) {
			aNBT.remove("ench");
			ItemNBT.set(aStack, aNBT);
			return F;
		}

		IToolStats tStats = getToolStatsInternal(aStack);
		// No Tool Data?
		if (tStats == null) {
			aNBT.remove("ench");
			ItemNBT.set(aStack, aNBT);
			return F;
		}

		OreDictMaterial aMaterial = getPrimaryMaterial(aStack);
		// "Empty" Toolheads should not be able to do things.
		if (aMaterial == MT.Empty) {
			aNBT.remove("ench");
			ItemNBT.set(aStack, aNBT);
			return F;
		}

		// Some Behavior declaring this unusable?
		if (!super.isItemStackUsable(aStack)) {
			aNBT.remove("ench");
			ItemNBT.set(aStack, aNBT);
			return F;
		}

		// If no Enchantments, checks ends successfully early.
		if (aNBT.contains("ench")) return T;

		// Abuse a potentially empty List as a boolean to see if a Tool already has enchants or not.
		aNBT.put("ench", new ListTag());
		ItemNBT.set(aStack, aNBT);
		
		List<ObjectStack<Enchantment>> tEnchantments = new ArrayListNoNulls<>();
		// Get Material Specific Enchantments for applicable Tool Classes.
		if (tStats.isMiningTool  ()) for (ObjectStack<Enchantment> tEnchantment : aMaterial.mEnchantmentTools  ) tEnchantments.add(new ObjectStack<>(tEnchantment.mObject, tEnchantment.mAmount));
		if (tStats.isWeapon      ()) for (ObjectStack<Enchantment> tEnchantment : aMaterial.mEnchantmentWeapons) tEnchantments.add(new ObjectStack<>(tEnchantment.mObject, tEnchantment.mAmount));
		if (tStats.isRangedWeapon()) for (ObjectStack<Enchantment> tEnchantment : aMaterial.mEnchantmentRanged ) tEnchantments.add(new ObjectStack<>(tEnchantment.mObject, tEnchantment.mAmount));
		
		// Get Tool Specific Enchantments.
		Enchantment[] tEnchants = tStats.getEnchantments(aStack, aMaterial); // enchant currency in 1.20.1 is Enchantment itself (matches the 1.7.10 shape)
		int[] tLevels = tStats.getEnchantmentLevels(aStack, aMaterial);
		
		for (int i = 0; i < tEnchants.length; i++) if (tLevels[i] > 0) {
			boolean temp = T;
			for (ObjectStack<Enchantment> tEnchantment : tEnchantments) if (tEnchantment.mObject == tEnchants[i]) {
				tEnchantment.mAmount = 1+Math.max(tEnchantment.mAmount, tLevels[i]);
				temp = F;
				break;
			}
			if (temp) tEnchantments.add(new ObjectStack<>(tEnchants[i], tLevels[i]));
		}
		for (ObjectStack<Enchantment> tEnchantment : tEnchantments) UT.NBT.addEnchantment(aStack, tEnchantment.mObject, tEnchantment.amountShort());
		return T;
	}
	
	/** For a tool, NBT is STATE (material, durability, charge), not identity: the display gives it out bare
	 *  ({@link #getSubItems}), the recipe output comes with stats. Identity, same as in 1.7.10, = meta. Analysis and measurement —
	 *  in the javadoc {@link MultiItem#identityIncludesNBT()} and in ticket BUG-079. */
	@Override public boolean identityIncludesNBT() {return F;}

	public boolean isUsableMeta(short aMeta) {
		return aMeta % 2 == 0;
	}
	public boolean isUsableMeta(ItemStack aStack) {
		return isUsableMeta(ST.meta(aStack));
	}
	public short getUsableMeta(short aMeta) {
		return (short)(aMeta  -(aMeta % 2));
	}
	public short getUsableMeta(ItemStack aStack) {
		return getUsableMeta(ST.meta(aStack));
	}
	public short getUnusableMeta(short aMeta) {
		return (short)(aMeta+1-(aMeta % 2));
	}
	public short getUnusableMeta(ItemStack aStack) {
		return getUnusableMeta(ST.meta(aStack));
	}
	
	// F3 superseded-render (GT6BlockModel/ItemModel pipeline; old getIcon/immediate-mode is dead, 0 neo calls):
	// getRenderPasses/getColorFromItemStack/getIconIndex/getIconFromDamage*/getIcon (1.7.10 multi-pass Item render) don't exist on 26.1.2 Item; texture holder is now ResourceLocation (gregapi.render.IIconContainer). Not @Override, bodies kept 1:1 as internal domain calls between each other.
	public int getRenderPasses(int aMetaData) {
		IToolStats tStats = getToolStatsInternal(aMetaData);
		if (tStats != null) return tStats.getRenderPasses()+2;
		return 2;
	}

	public int getColorFromItemStack(ItemStack aStack, int aRenderPass) {
		IToolStats tStats = getToolStatsInternal(aStack);
		if (tStats != null) return UT.Code.getRGBaInt(tStats.getRGBa(aStack, aRenderPass));
		return 16777215;
	}

	public ResourceLocation getIconIndex(ItemStack aStack) {return getIcon(aStack, 0);}
	public ResourceLocation getIconFromDamageForRenderPass(int aMetaData, int aRenderPass) {return getIconFromDamage(aMetaData);}
	public ResourceLocation getIconFromDamage(int aMetaData) {return getIconIndex(ST.make(this, 1, aMetaData));}
	public ResourceLocation getIcon(ItemStack aStack, int aRenderPass) {return getIcon(aStack, aRenderPass, null, null, 0);}
	public ResourceLocation getIcon(ItemStack aStack, int aRenderPass, Player aPlayer, ItemStack aUsedStack, int aUseRemaining) {
		IToolStats tStats = getToolStatsInternal(aStack);
		if (tStats == null) return Textures.ItemIcons.VOID.getIcon(0);
		if (aRenderPass < tStats.getRenderPasses()) {
			ResourceLocation rIcon = tStats.getIcon(aStack, aRenderPass);
			return rIcon == null ? Textures.ItemIcons.VOID.getIcon(0) : rIcon;
		}
		if (aPlayer == null) {
			aRenderPass -= tStats.getRenderPasses();
			if (aRenderPass == 0) {
				long tDamage = MultiItemTool.getToolDamage(aStack), tMaxDamage = MultiItemTool.getToolMaxDamage(aStack);
				if (tMaxDamage <= 0) return Textures.ItemIcons.VOID.getIcon(0);
				if (tDamage <= 0) return Textures.ItemIcons.DURABILITY_BAR[8].getIcon(0);
				if (tDamage >= tMaxDamage) return Textures.ItemIcons.DURABILITY_BAR[0].getIcon(0);
				return Textures.ItemIcons.DURABILITY_BAR[(int)Math.max(0, Math.min(7, ((tMaxDamage-tDamage)*8L) / tMaxDamage))].getIcon(0);
			}
			if (aRenderPass == 1) {
				IItemEnergy tElectric = getEnergyStats(aStack);
				if (tElectric != null) {
					long tStored = tElectric.getEnergyStored(TD.Energy.EU, aStack), tCapacity = tElectric.getEnergyCapacity(TD.Energy.EU, aStack);
					if (tStored <= 0) return Textures.ItemIcons.ENERGY_BAR[0].getIcon(0);
					if (tStored >= tCapacity) return Textures.ItemIcons.ENERGY_BAR[8].getIcon(0);
					return Textures.ItemIcons.ENERGY_BAR[7-(int)Math.max(0, Math.min(6, ((tCapacity-tStored)*7L) / tCapacity))].getIcon(0);
				}
			}
		}
		return Textures.ItemIcons.VOID.getIcon(0);
	}
	
	public IToolStats getToolStats(ItemStack aStack) {isItemStackUsable(aStack); return getToolStatsInternal(aStack);}
	public IToolStats getToolStatsInternal(ItemStack aStack) {return aStack == null ? null : getToolStatsInternal(ST.meta_(aStack));}
	public IToolStats getToolStatsInternal(int aDamage) {return mToolStats.get((short)aDamage);}
	@Override public final boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	@Override public final int getItemStackLimit(ItemStack aStack) {return 1;}
	// F3 superseded-render (GT6BlockModel/ItemModel pipeline; old getIcon/immediate-mode is dead, 0 neo calls):
	// isFull3D/getSpriteNumber/requiresMultipleRenderPasses (1.7.10 multi-pass Item render) don't exist on 26.1.2 — not @Override, bodies kept 1:1.
	public boolean isFull3D() {return T;}
	public int getSpriteNumber() {return 1;}
	public boolean requiresMultipleRenderPasses() {return T;}
	// F3 superseded-render: was registerIcons(IIconRegister) (1.7.10 atlas-stitching type removed) — parameter is now
	// Object, the same neutral holder as gregapi.render.IIconContainer#registerIcons(Object).
	public void registerIcons(Object aIconRegister) {/**/}
	@Override @SuppressWarnings("deprecation") public boolean isFoil(ItemStack aStack) {return F;}
	// F3 superseded-render: was hasEffect(ItemStack,int aRenderPass) (multi-pass glint, type removed).
	public boolean hasEffect(ItemStack aStack, int aRenderPass) {return F;}
	// item-base dead-interface: getItemEnchantability/isBookEnchantable/getIsRepairable are 1.7.10 virtual hooks that
	// neo never calls (enchantability = ENCHANTABLE stack component via stack.getEnchantmentValue; repair = Properties.repairable/ENCHANTABLE). Not @Override (dead, 0 engine calls). Per-material variation is a stack component instead.
	public int getItemEnchantability() {return 0;}
	public boolean isBookEnchantable(ItemStack aStack, ItemStack aBook) {return F;}
	public boolean getIsRepairable(ItemStack aStack, ItemStack aMaterial) {return F;}
	@Override public Long[] getFluidContainerStats(ItemStack aStack) {return null;}
}
