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

package gregapi.item.multiitem;
import net.minecraft.resources.ResourceKey;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

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
		// PORT-TODO(item-base, stacksTo immutable-post-construction): Item.setMaxStackSize(int) (1.7.10
		// runtime-мутатор) не существует в 26.1.2 — стек-размер задаётся неизменяемо через
		// Item.Properties.stacksTo(...) в момент регистрации (тот же класс, что F12 item-maxdamage-subtypes-
		// runtime-mutator в GT_API_Post.java); владелец Properties — MultiItem/ItemBase (вне зоны этого захода).
		// setMaxStackSize(1);
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
	public void onHarvestBlockEvent(ArrayList<ItemStack> aDrops, ItemStack aStack, Player aPlayer, Block aBlock, int aX, int aY, int aZ, byte aMeta, int aFortune, boolean aSilkTouch, BlockDropsEvent aEvent) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null || ST.instaharvest(aBlock, aMeta) || !isItemStackUsable(aStack) || getDigSpeed(aStack, aBlock, aMeta) <= 0) {
			doDamage(aStack, 0, aPlayer, T);
			return;
		}
		long tDamage = tStats.convertBlockDrops(aDrops, aStack, aPlayer, aBlock, (getToolMaxDamage(aStack) - getToolDamage(aStack)) / tStats.getToolDamagePerDropConversion(), aX, aY, aZ, aMeta, aFortune, aSilkTouch, aEvent);
		if (aBlock == Blocks.ICE && !aDrops.isEmpty()) aPlayer.level().removeBlock(new BlockPos(aX, aY, aZ), F);
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
		// PORT-TODO(item-base, UT.Sounds.play overload ambiguity): play(String,int,float,Entity) vs
		// play(String,int,float,BlockPos) неоднозначны компилятору на этом call-site (не мой центр, вне зоны,
		// gregapi/util/UT.java) — обхожу через координатный overload, тот же эффект (Entity-overload сам вызывает
		// координатный внутри).
		if (TOOL_SOUNDS) UT.Sounds.play(tStats.getEntityHitSound(), 20, 1, UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ()));
		if (super.onLeftClickEntity(aStack, aPlayer, aEntity)) return T;
		// PORT-TODO(item-base, canAttackWithItem): Entity.canAttackWithItem() (1.7.10, false while e.g. riding
		// certain vehicles) удалён без замены в 3 корнях референса — деградация до T (консервативно: "может").
		if (T) {
			int tImplosion = UT.NBT.getEnchantmentLevelImplosion(aStack);
			// PORT-TODO(F8, enchant-registry): EnchantmentHelper.getFireAspectModifier(Player)/
			// getEnchantmentModifierLiving(Player,LivingEntity) (1.7.10 static lookups) удалены — зачарования
			// data-driven, Holder<Enchantment> недоступен в этом статическом контексте — деградация до 0.
			int tFireAspect = 0;
			boolean tIgnitesFire = !aEntity.isOnFire() && tFireAspect > 0 && aEntity instanceof LivingEntity;
			if (tIgnitesFire) aEntity.igniteForSeconds(1);
			if (aEntity.skipAttackInteraction(aPlayer)) {
				if (tIgnitesFire) aEntity.clearFire();
			} else {
				float tMagicDamage = tStats.getMagicDamageAgainstEntity(0, aEntity, aStack, aPlayer), tDamage = tStats.getNormalDamageAgainstEntity((float)aPlayer.getAttributeValue(Attributes.ATTACK_DAMAGE) + getToolCombatDamage(aStack), aEntity, aStack, aPlayer);
				// Also work on Ghasts and such. But no double dipping on Anti Creeper Damage!
				if (tImplosion > 0 && UT.Entities.isExplosiveCreature(aEntity) && !Creeper.class.isInstance(aEntity)) tMagicDamage += 1.5F * tImplosion;

				if (tDamage + tMagicDamage > 0) {
					// PORT-TODO(item-base, hurtResistantTime): Entity.hurtResistantTime (1.7.10 hit-invulnerability
					// поле, использовалось для client-side hit-prediction) отсутствует в 26.1.2 — деградация до
					// "реальный удар только на сервере" (безопаснее дублирующего клиентского предсказания).
					boolean tRealHit = !aEntity.level().isClientSide();
					boolean tCriticalHit = aPlayer.fallDistance > 0 && !aPlayer.onGround() && !aPlayer.onClimbable() && !aPlayer.isInWater() && !aPlayer.hasEffect(MobEffects.BLINDNESS) && aPlayer.getVehicle() == null && aEntity instanceof LivingEntity;
					if (tCriticalHit && tDamage > 0) tDamage *= 1.5;
					float tFullDamage = (tDamage+tMagicDamage) * TFC_DAMAGE_MULTIPLIER;
					DamageSource tSource = tStats.getDamageSource(aPlayer, aEntity);
					if (tStats.canPenetrate() && tSource instanceof gregapi.damage.DamageSources.GregTechDamageSource) ((gregapi.damage.DamageSources.GregTechDamageSource)tSource).setDamageBypassesArmor();
					// Avoiding the Betweenlands Damage Cap of 40 in a fair way.
					// Only Betweenlands Materials will avoid it. And maybe some super Lategame Materials.
					// (tRealHit гейтит вход в hurtServer — тот требует ServerLevel, на клиенте вызывать нельзя.)
					if (tRealHit && MD.BTL.mLoaded && aEntity.getClass().getName().startsWith("thebetweenlands") && getPrimaryMaterial(aStack).contains(TD.Properties.BETWEENLANDS)) {
						net.minecraft.server.level.ServerLevel tServerLevel = (net.minecraft.server.level.ServerLevel)aEntity.level();
						float tDamageToDeal = tFullDamage;
						while (tDamageToDeal > 0 && aEntity.hurtServer(tServerLevel, tSource, Math.min(tDamageToDeal, 12) / 0.3F)) {
							tDamageToDeal -= 12;
						}
						tRealHit &= (tDamageToDeal < tFullDamage);
					} else if (tRealHit) {
						tRealHit &= aEntity.hurtServer((net.minecraft.server.level.ServerLevel)aEntity.level(), tSource, tFullDamage);
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
		// PORT-TODO(item-base, setItemInUse): Player.setItemInUse(ItemStack,int) (1.7.10 explicit-duration
		// use-start) удалён — 26.1.2 стартует использование через LivingEntity.startUsingItem(InteractionHand),
		// длительность берётся из Item.getUseDuration(ItemStack,LivingEntity) (см. ниже) автоматически; этот
		// вызов не достижим текущей цепочкой super.onItemRightClick (MultiItem, вне зоны), см. класс-javadoc.
		// if (tStats != null && tStats.canBlock()) aPlayer.setItemInUse(aStack, 72000);
		return super.onItemRightClick(aStack, aWorld, aPlayer);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack aStack) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && tStats.canBlock()) return ItemUseAnimation.BLOCK;
		return ItemUseAnimation.NONE;
	}
	@Override
	public int getUseDuration(ItemStack aStack, LivingEntity aUser) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && tStats.canBlock()) return 72000;
		return 0;
	}
	
	// PORT-TODO(F13, creative-tab): Item.getSubItems(Item,CreativeModeTab,List) (1.7.10 creative-tab population
	// hook) удалён — 26.1.2 наполняет вкладки через BuildCreativeModeTabContentsEvent/CreativeModeTab.Builder.
	// displayItems, нет per-Item override; тот же класс проблемы, что setCreativeTab (ItemArmorBase.java,
	// MultiItemFood.java), центра ещё нет нигде в дереве. Список-строящая логика сохранена 1:1 доменным методом.
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
				aList.add(LH.Chat.WHITE + "Durability: x" + LH.Chat.GREEN + tStats.getMaxDurabilityMultiplier());
				aList.add(LH.Chat.WHITE + "Level: +" + LH.Chat.YELLOW + tStats.getBaseQuality());
				float tCombat = getToolCombatDamage(aStack);
				aList.add(LH.Chat.WHITE + "Melee Damage: +" + LH.Chat.BLUE + (tCombat * TFC_DAMAGE_MULTIPLIER) + LH.Chat.RED + " (= " + (TFC_DAMAGE_MULTIPLIER > 1 ? ((tCombat+1)*(TFC_DAMAGE_MULTIPLIER/2.0)) + ")" : ((tCombat+1)/2) + " Hearts)"));
				aList.add(LH.Chat.WHITE + "Mining Speed: x" + LH.Chat.PINK + tStats.getSpeedMultiplier());
				if (tStats.canCollect()) aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_AUTOCOLLECT));
				if (tStats.canPenetrate()) aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_ARMOR_PENETRATING));
			} else {
				aList.add(LH.Chat.WHITE + "Durability: " + LH.Chat.GREEN + UT.Code.makeString(tMaxDamage - tDamage) + " / " + UT.Code.makeString(tMaxDamage));
				aList.add(LH.Chat.WHITE + tMat1.getLocal() + LH.Chat.YELLOW + " Level: " + (tStats.getBaseQuality() + tMat1.mToolQuality));
				float tCombat = getToolCombatDamage(aStack);
				aList.add(LH.Chat.WHITE + "Melee Damage: " + LH.Chat.BLUE + "+" + (tCombat * TFC_DAMAGE_MULTIPLIER) + LH.Chat.RED + " (= " + (TFC_DAMAGE_MULTIPLIER > 1 ? ((tCombat+1)*(TFC_DAMAGE_MULTIPLIER/2.0)) + ")" : ((tCombat+1)/2) + " Hearts)"));
				aList.add(LH.Chat.WHITE + "Mining Speed: " + LH.Chat.PINK + Math.max(Float.MIN_NORMAL, tStats.getSpeedMultiplier() * tMat1.mToolSpeed));
				aList.add(LH.Chat.WHITE + "Crafting Uses: " + LH.Chat.GREEN + UT.Code.divup(getEnergyStats(aStack) == null ? tMaxDamage - tDamage : Math.min(getEnergyStored(TD.Energy.EU, aStack), getEnergyCapacity(TD.Energy.EU, aStack)), tStats.getToolDamagePerContainerCraft()));
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
			aNBT = aNBT.getCompoundOrEmpty("GT.ToolStats");
			if (!aNBT.isEmpty()) {
				if (aNBT.contains("a")) return OreDictMaterial.get(aNBT.getShortOr ("a", (short)0), aDefault);
				if (aNBT.contains("b")) return OreDictMaterial.get(aNBT.getStringOr("b", ""), aDefault);
			}
		}
		return aDefault;
	}

	public static final OreDictMaterial getSecondaryMaterial(ItemStack aStack) {return getSecondaryMaterial(aStack, MT.NULL);}
	public static final OreDictMaterial getSecondaryMaterial(ItemStack aStack, OreDictMaterial aDefault) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompoundOrEmpty("GT.ToolStats");
			if (!aNBT.isEmpty()) {
				if (aNBT.contains("c")) return OreDictMaterial.get(aNBT.getShortOr ("c", (short)0), aDefault);
				if (aNBT.contains("d")) return OreDictMaterial.get(aNBT.getStringOr("d", ""), aDefault);
			}
		}
		return aDefault;
	}

	@Override
	public IItemEnergy getEnergyStats(ItemStack aStack) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompoundOrEmpty("GT.ToolStats");
			if (!aNBT.isEmpty()) {
				if (aNBT.getBooleanOr("e", F)) return EnergyStat.makeTool(TD.Energy.EU, aNBT.getLongOr("f", 0L), aNBT.getLongOr("g", 0L), 64, ST.make(this, 1, getUnusableMeta(aStack)), ST.make(this, 1, getUsableMeta(aStack)), ST.make(this, 1, getUsableMeta(aStack)));
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
			aNBT = aNBT.getCompoundOrEmpty("GT.ToolStats");
			if (aNBT.contains("j")) return Math.max(1, aNBT.getLongOr("j", 0L));
			return Math.max(1, aNBT.getLongOr("MaxDamage", 0L));
		}
		return 1;
	}
	public static final long getToolDamage(ItemStack aStack) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			aNBT = aNBT.getCompoundOrEmpty("GT.ToolStats");
			if (aNBT.contains("k")) return aNBT.getLongOr("k", 0L);
			return aNBT.getLongOr("Damage", 0L);
		}
		return 0;
	}
	// PORT-TODO(F8, остаточный риск): оригинал мутирует aNBT.getCompoundOrEmpty("GT.ToolStats") и НЕ вызывает
	// setTagCompound повторно — в 1.7.10 это был живой объект тега стека, мутация сохранялась сама.
	// Под мостом ItemNBT (CustomData копирует тег на каждый get()) эта мутация НЕ долетает до стека:
	// метод становится no-op. См. ItemNBT.java javadoc и decisions/F8-nbt-data-components.md §7.
	public static final boolean setToolDamage(ItemStack aStack, long aDamage) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			UT.NBT.setNumber(aNBT.getCompoundOrEmpty("GT.ToolStats"), "k", aDamage);
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
						if (aPlayer != null) {
							UT.Sounds.send(getPrimaryMaterial(aStack) == MT.NULL ? tStats.getCraftingSound() : tStats.getBreakingSound(), aPlayer);
						} else {
							UT.Sounds.play(getPrimaryMaterial(aStack) == MT.NULL ? tStats.getCraftingSound() : tStats.getBreakingSound(), 100, 1, LAST_TOOL_COORDS_BEFORE_DAMAGE);
						}
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
	
	// PORT-TODO(F9, block-material Block-meta mining hooks): getDigSpeed(ItemStack,Block,int)/
	// canHarvestBlock(Block,ItemStack)/getHarvestLevel(ItemStack,String)/onBlockDestroyed(...) (1.7.10
	// meta-based Item mining hooks) не существуют в 26.1.2 Item целиком — реальные аналоги
	// (getDestroySpeed(ItemStack,BlockState)/isCorrectToolForDrops(ItemStack,BlockState), BlockTags-driven
	// harvest-tier) требуют BlockState-конверсию (F9, не готова в этой зоне) — методы НЕ @Override, тела 1:1
	// сохранены как внутренние доменные вычисления (используются другими методами этого же класса напрямую).
	// Block.getHarvestLevel(int)/getBlockHardness(World,x,y,z) (meta-based) удалены — консервативные заглушки
	// (0 / 1.0) ниже, помечены отдельно.
	public float getDigSpeed(ItemStack aStack, Block aBlock, int aMeta) {
		if (aBlock == NB || WD.bedrock(aBlock)) return 0;
		if (ST.instaharvest(aBlock, aMeta)) return 10;
		if (!isItemStackUsable(aStack)) return 0;
		// Required because a combination of Twilight Forest and Block Metadata Extenders can fuck this up and give me values like 49 for vanilla Blocks.
		if (aMeta > 15 && (aBlock == Blocks.DIRT || aBlock == Blocks.GRASS_BLOCK || aBlock == Blocks.STONE)) aMeta = 0;
		float tMultiplier = 1.0F;
		OreDictMaterial tMaterial = getPrimaryMaterial(aStack);
		if ((IL.TF_Mazestone.equal(aBlock) || IL.TF_Mazehedge.equal(aBlock) || IL.TF_Towerwood.equal(aBlock)) && tMaterial.contains(TD.Properties.MAZEBREAKER)) tMultiplier *= 40;
		IToolStats tStats = getToolStats(aStack);
		// PORT-TODO(F9, block-material): Block.getHarvestLevel(int aMeta) удалён (meta-based) — degraded до 0 (не найдено ни в одном из 3 корней референса).
		if (tStats == null || tStats.getBaseQuality() + tMaterial.mToolQuality < UT.Code.bind4(0)) return 0;
		return tStats.getMiningSpeed(aBlock, (byte)aMeta) * Math.max(Float.MIN_NORMAL, tStats.getSpeedMultiplier() * tMultiplier * tMaterial.mToolSpeed);
	}

	public final boolean canHarvestBlock(Block aBlock, ItemStack aStack) {
		return IL.TC_Block_Air.equal(aBlock) || MD.CARP.owns(aBlock) || getDigSpeed(aStack, aBlock, (byte)0) > 0;
	}

	public final int getHarvestLevel(ItemStack aStack, String aToolClass) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return -1;
		int rValue = tStats.getBaseQuality() + getPrimaryMaterial(aStack).mToolQuality;
		return rValue < 15 ? rValue : Integer.MAX_VALUE;
	}

	public boolean onBlockDestroyed(ItemStack aStack, Level aWorld, Block aBlock, int aX, int aY, int aZ, LivingEntity aPlayer) {
		if (ST.instaharvest(aBlock) || UT.Entities.hasInfiniteItems(aPlayer)) return T;
		if (!isItemStackUsable(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return F;
		if (TOOL_SOUNDS) UT.Sounds.play(tStats.getMiningSound(), 5, 1, aX, aY, aZ);
		String aRegName = ST.regName(aBlock);
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		boolean rReturn = (getDigSpeed(aStack, aBlock, aMeta) > 0);
		// оригинал: * aBlock.getBlockHardness(aWorld,aX,aY,aZ). Способность ЕСТЬ — ЦЕНТР WD.hardness (уже
		// через BlockState.getDestroySpeed, WD.java:360). Была ложная деградация до 1.0 — восстановлено 1:1.
		double tDamage = tStats.getToolDamagePerBlockBreak() * WD.hardness(aBlock, aWorld, aX, aY, aZ);
		OreDictMaterial aMat1 = getPrimaryMaterial(aStack);
		if (WD.dimBTL(aWorld) && !aMat1.contains(TD.Properties.BETWEENLANDS)) tDamage *= 4;
		if (MD.TFC.owns(aRegName) || MD.TFCP.owns(aRegName)) {
			tDamage /= 4;
		} else {
			if (IL.TF_Mazestone.equal(aBlock)) if (aMat1.contains(TD.Properties.MAZEBREAKER)) tDamage /= 40; else tDamage *= 16;
			if (IL.TF_Mazehedge.equal(aBlock)) {
				if (aMat1.contains(TD.Properties.MAZEBREAKER)) tDamage /= 40; else tDamage *= 16;
				// PORT-TODO(F8, enchant-registry): Enchantments.SILK_TOUCH (1.7.10 static instance) удалён —
				// зачарования data-driven, нет живого Holder<Enchantment> в статическом контексте; деградация
				// до "нет шёлковой нити" (ветка отдаёт особый дроп безусловно).
				if (!aWorld.isClientSide()) {
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
	public ItemStack getContainerItem(ItemStack aStack) {
		if (!isUsableMeta(aStack)) return null;
		IToolStats tStats = getToolStats(aStack);
		if (tStats == null) return null;
		if (TOOL_SOUNDS) UT.Sounds.play(tStats.getCraftingSound(), 200, 1, LAST_TOOL_COORDS_BEFORE_DAMAGE);
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

		// PORT-TODO(F8, остаточный риск): ниже aNBT.remove("ench")/setTag("ench", ...) мутируют локальный
		// снимок без повторного commit через ItemNBT.set — в 1.7.10 это был живой тег стека. Под мостом
		// ItemNBT эти правки в тег стека НЕ попадают (см. ItemNBT.java javadoc, decisions/F8-nbt-data-components.md §7).
		CompoundTag aNBT = ItemNBT.get(aStack);
		// The Tool has no Data? Treat it like a single use Creative Tool.
		if (aNBT == null) return T;
		
		// Invalid Tool Index?
		if (!isUsableMeta(aStack)) {
			aNBT.remove("ench");
			return F;
		}
		
		IToolStats tStats = getToolStatsInternal(aStack);
		// No Tool Data?
		if (tStats == null) {
			aNBT.remove("ench");
			return F;
		}
		
		OreDictMaterial aMaterial = getPrimaryMaterial(aStack);
		// "Empty" Toolheads should not be able to do things.
		if (aMaterial == MT.Empty) {
			aNBT.remove("ench");
			return F;
		}
		
		// Some Behavior declaring this unusable?
		if (!super.isItemStackUsable(aStack)) {
			aNBT.remove("ench");
			return F;
		}
		
		// If no Enchantments, checks ends successfully early.
		if (aNBT.contains("ench")) return T;
		
		// Abuse a potentially empty List as a boolean to see if a Tool already has enchants or not.
		aNBT.put("ench", new ListTag());
		
		List<ObjectStack<ResourceKey<Enchantment>>> tEnchantments = new ArrayListNoNulls<>();
		// Get Material Specific Enchantments for applicable Tool Classes.
		if (tStats.isMiningTool  ()) for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMaterial.mEnchantmentTools  ) tEnchantments.add(new ObjectStack<>(tEnchantment.mObject, tEnchantment.mAmount));
		if (tStats.isWeapon      ()) for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMaterial.mEnchantmentWeapons) tEnchantments.add(new ObjectStack<>(tEnchantment.mObject, tEnchantment.mAmount));
		if (tStats.isRangedWeapon()) for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMaterial.mEnchantmentRanged ) tEnchantments.add(new ObjectStack<>(tEnchantment.mObject, tEnchantment.mAmount));
		
		// Get Tool Specific Enchantments.
		ResourceKey<Enchantment>[] tEnchants = tStats.getEnchantments(aStack, aMaterial); // F-enchant-key: getEnchantments -> ResourceKey<Enchantment>[] (модель энчантов = ResourceKey).
		int[] tLevels = tStats.getEnchantmentLevels(aStack, aMaterial);
		
		for (int i = 0; i < tEnchants.length; i++) if (tLevels[i] > 0) {
			boolean temp = T;
			for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tEnchantments) if (tEnchantment.mObject == tEnchants[i]) {
				tEnchantment.mAmount = 1+Math.max(tEnchantment.mAmount, tLevels[i]);
				temp = F;
				break;
			}
			if (temp) tEnchantments.add(new ObjectStack<>(tEnchants[i], tLevels[i]));
		}
		for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tEnchantments) UT.NBT.addEnchantment(aStack, tEnchantment.mObject, tEnchantment.amountShort());
		return T;
	}
	
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
	
	// PORT-TODO(F3, baked-рендер клиента): getRenderPasses(int)/getColorFromItemStack(ItemStack,int)/
	// getIconIndex/getIconFromDamage/getIconFromDamageForRenderPass/getIcon(...) (1.7.10 multi-pass IIcon
	// Item-рендер) не существуют в 26.1.2 Item целиком — держатель текстуры теперь Identifier (см.
	// gregapi.render.IIconContainer, тот же центр); методы НЕ @Override, тела 1:1 сохранены (внутренние
	// доменные вычисления, дергают друг друга напрямую внутри этого же класса).
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

	public Identifier getIconIndex(ItemStack aStack) {return getIcon(aStack, 0);}
	public Identifier getIconFromDamageForRenderPass(int aMetaData, int aRenderPass) {return getIconFromDamage(aMetaData);}
	public Identifier getIconFromDamage(int aMetaData) {return getIconIndex(ST.make(this, 1, aMetaData));}
	public Identifier getIcon(ItemStack aStack, int aRenderPass) {return getIcon(aStack, aRenderPass, null, null, 0);}
	public Identifier getIcon(ItemStack aStack, int aRenderPass, Player aPlayer, ItemStack aUsedStack, int aUseRemaining) {
		IToolStats tStats = getToolStatsInternal(aStack);
		if (tStats == null) return Textures.ItemIcons.VOID.getIcon(0);
		if (aRenderPass < tStats.getRenderPasses()) {
			Identifier rIcon = tStats.getIcon(aStack, aRenderPass);
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
	// PORT-TODO(F3, baked-рендер клиента): isFull3D/getSpriteNumber/requiresMultipleRenderPasses (1.7.10
	// multi-pass Item-рендер) не существуют в 26.1.2 — методы НЕ @Override, тела 1:1 сохранены.
	public boolean isFull3D() {return T;}
	public int getSpriteNumber() {return 1;}
	public boolean requiresMultipleRenderPasses() {return T;}
	// PORT-TODO(F3, baked-рендер клиента): было registerIcons(IIconRegister) (тип атлас-стежки 1.7.10 удалён) —
	// параметр Object, тот же нейтральный держатель что gregapi.render.IIconContainer#registerIcons(Object).
	@OnlyIn(Dist.CLIENT) public void registerIcons(Object aIconRegister) {/**/}
	@Override @SuppressWarnings("deprecation") public boolean isFoil(ItemStack aStack) {return F;}
	// PORT-TODO(F3, baked-рендер клиента): было hasEffect(ItemStack,int aRenderPass) (multi-pass glint, тип удалён).
	public boolean hasEffect(ItemStack aStack, int aRenderPass) {return F;}
	// PORT-TODO(item-base, enchantable/repairable компоненты): getItemEnchantability()/isBookEnchantable(...)/
	// getIsRepairable(...) (1.7.10 virtual Item-хуки) удалены — 26.1.2 задаёт то же самое декларативно через
	// Item.Properties.enchantable(...)/repairable(...) на регистрации (владелец Properties — MultiItem/ItemBase,
	// вне зоны); методы НЕ @Override, тела 1:1 сохранены.
	public int getItemEnchantability() {return 0;}
	public boolean isBookEnchantable(ItemStack aStack, ItemStack aBook) {return F;}
	public boolean getIsRepairable(ItemStack aStack, ItemStack aMaterial) {return F;}
	@Override public Long[] getFluidContainerStats(ItemStack aStack) {return null;}
}
