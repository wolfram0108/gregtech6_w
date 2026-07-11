/**
 * Copyright (c) 2023 GregTech-6 Team
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

import buildcraft.api.tools.IToolWrench;
import gregapi.api.Optional;
import forestry.api.arboriculture.IToolGrafter;
import gregapi.data.CS.ModIDs;
import gregapi.data.TD;
import gregapi.item.multiitem.tools.IToolStats;
import gregapi.util.UT;
import ic2.api.item.IBoxable;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import micdoodle8.mods.galacticraft.api.item.IItemElectric;
import mods.railcraft.api.core.items.IToolCrowbar;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.IWarpingGear;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

/**
 * @author Gregorius Techneticies
 *
 * This is an example on how you can create a Tool ItemStack, in this case a Bismuth Wrench:
 * gregapi.data.CS.ToolsGT.sMetaTool.getToolWithStats(CS.ToolIDs.WRENCH, 1, MT.Bismuth, MT.Bismuth, null);
 *
 * PORT-TODO(item-base, F10 compat-mirror пустые интерфейсы): IWarpingGear/IToolGrafter/IToolCrowbar/
 * IToolWrench/IBoxable/ISpecialElectricItem/IElectricItemManager/IItemElectric сейчас ПУСТЫЕ маркер-
 * интерфейсы (compat-mirror/README.md, "члены добираются компилятором") — методы ниже временно НЕ
 * @Override (нечего переопределять), тела 1:1 сохранены для реальной мод-интеграции позже.
 */
@Optional.InterfaceList(value = {
  @Optional.Interface(iface = "thaumcraft.api.IWarpingGear", modid = ModIDs.TC)
, @Optional.Interface(iface = "forestry.api.arboriculture.IToolGrafter", modid = ModIDs.FR)
, @Optional.Interface(iface = "mods.railcraft.api.core.items.IToolCrowbar", modid = ModIDs.RC)
, @Optional.Interface(iface = "buildcraft.api.tools.IToolWrench", modid = ModIDs.BC)
, @Optional.Interface(iface = "ic2.api.item.IBoxable", modid = ModIDs.IC2)
, @Optional.Interface(iface = "ic2.api.item.ISpecialElectricItem", modid = ModIDs.IC2)
, @Optional.Interface(iface = "ic2.api.item.IElectricItemManager", modid = ModIDs.IC2)
, @Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.item.IItemElectric", modid = ModIDs.GC)
})
public class MultiItemToolWithCompat extends MultiItemTool implements IWarpingGear, IToolGrafter, IToolCrowbar, IToolWrench, IBoxable, ISpecialElectricItem, IElectricItemManager, IItemElectric {
	/**
	 * Creates the Item using these Parameters.
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public MultiItemToolWithCompat(String aModID, String aUnlocalized) {super(aModID, aUnlocalized);}

	public int getWarp(ItemStack aStack, Player aPlayer) {
		return getPrimaryMaterial(aStack).contains(TD.Properties.WARPING) || getSecondaryMaterial(aStack).contains(TD.Properties.WARPING) ? 1 : 0;
	}

	public float getSaplingModifier(ItemStack aStack, Level aWorld, Player aPlayer, int aX, int aY, int aZ) {
		IToolStats tStats = getToolStats(aStack);
		return tStats != null && tStats.isGrafter() ? Math.min(100.0F, (1+UT.Code.bind4(getHarvestLevel(aStack, ""))) * 20.0F) : 0.0F;
	}

	public boolean canWrench(Player aPlayer, int aX, int aY, int aZ) {
		ItemStack aStack = aPlayer.getMainHandItem();
		if (!isItemStackUsable(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		return tStats != null && tStats.isWrench();
	}

	public void wrenchUsed(Player aPlayer, int aX, int aY, int aZ) {
		ItemStack aStack = aPlayer.getMainHandItem();
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && !UT.Entities.hasInfiniteItems(aPlayer)) doDamage(aStack, 100, aPlayer, T);
	}

	public boolean canWhack(Player aPlayer, ItemStack aStack, int aX, int aY, int aZ) {
		if (!isItemStackUsable(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		return tStats != null && tStats.isCrowbar();
	}

	public void onWhack(Player aPlayer, ItemStack aStack, int aX, int aY, int aZ) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && !UT.Entities.hasInfiniteItems(aPlayer)) doDamage(aStack, 100, aPlayer, T);
	}

	public boolean canLink(Player aPlayer, ItemStack aStack, AbstractMinecart cart) {
		if (!isItemStackUsable(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		return tStats != null && tStats.isCrowbar();
	}

	public void onLink(Player aPlayer, ItemStack aStack, AbstractMinecart cart) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && !UT.Entities.hasInfiniteItems(aPlayer)) doDamage(aStack, tStats.getToolDamagePerEntityAttack(), aPlayer, T);
	}

	public boolean canBoost(Player aPlayer, ItemStack aStack, AbstractMinecart cart) {
		if (!isItemStackUsable(aStack)) return F;
		IToolStats tStats = getToolStats(aStack);
		return tStats != null && tStats.isCrowbar();
	}

	public void onBoost(Player aPlayer, ItemStack aStack, AbstractMinecart cart) {
		IToolStats tStats = getToolStats(aStack);
		if (tStats != null && !UT.Entities.hasInfiniteItems(aPlayer)) doDamage(aStack, tStats.getToolDamagePerEntityAttack(), aPlayer, T);
	}

	public boolean canBeStoredInToolbox(ItemStack aStack) {
		return T;
	}

	@Optional.Method(modid = ModIDs.IC2)
	public IElectricItemManager getManager(ItemStack aStack) {return this;}
}
