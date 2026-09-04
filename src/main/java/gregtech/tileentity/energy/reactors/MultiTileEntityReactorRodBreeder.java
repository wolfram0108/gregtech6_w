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

package gregtech.tileentity.energy.reactors;

import static gregapi.data.CS.*;

import java.util.List;

import gregapi.code.ItemNBT;
import gregapi.data.LH;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityReactorRodBreeder extends MultiTileEntityReactorRodBase {
	public long mDurability = 0;
	public short mProduct = -1;
	public int mNeutronLoss = 0;
	public String mProductName = "";

	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		mDurability = aNBT.getLong(aNBT.contains(NBT_DURABILITY) ? NBT_DURABILITY : NBT_MAXDURABILITY);
		if (aNBT.contains(NBT_NUCLEAR_LOSS)) mNeutronLoss = aNBT.getInt(NBT_NUCLEAR_LOSS);
		if (aNBT.contains(NBT_VALUE)) mProduct = aNBT.getShort(NBT_VALUE);
	}

	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		UT.NBT.setNumber(aNBT, NBT_DURABILITY, mDurability);
	}

	@Override
	public CompoundTag writeItemNBT2(CompoundTag aNBT) {
		UT.NBT.setNumber(aNBT, NBT_DURABILITY, mDurability);
		return super.writeItemNBT2(aNBT);
	}

	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(LH.Chat.DGRAY + LH.tt("Used in Nuclear Reactor Core"));
		aList.add(LH.Chat.CYAN + LH.tt("Absorbs Neutrons to breed into an ") + LH.Chat.WHITE + LH.tt("Enriched Rod"));
		aList.add(LH.Chat.CYAN + LH.tt("Emits half the Heat per Neutron on this Rod"));
		aList.add(LH.Chat.CYAN + LH.tt("Can't breed with Neutrons from ") + LH.Chat.RED + LH.tt("Moderated") + LH.Chat.CYAN + LH.tt(" Fuel Rods"));
		aList.add(LH.Chat.CYAN + LH.tt("The ") + LH.Chat.YELLOW + LH.tt("Loss") + LH.Chat.CYAN + LH.tt(" value gets subtracted from Neutrons entering this Rod"));
		aList.add(LH.Chat.CYAN + LH.tt("This applies to each side where Neutrons enter, not to the total of all sides"));
		aList.add(LH.Chat.CYAN + LH.tt("Remaining Neutrons on this Rod get added to the breeding process"));
		if (mProductName.equals("")) mProductName = ST.meta(aStack.copy(), mProduct).getDisplayName().getString();
		aList.add(LH.Chat.GREEN + LH.tt("Turns into: ") + LH.Chat.WHITE + mProductName);
		aList.add(LH.Chat.CYAN + LH.tt("Needed: ") + LH.Chat.WHITE + mDurability + LH.Chat.PURPLE + LH.tt(" Neutrons"));
		aList.add(LH.Chat.YELLOW + LH.tt("Loss: ") + LH.Chat.WHITE + mNeutronLoss + LH.Chat.PURPLE + LH.tt(" Neutrons"));
	}
	
	@Override
	public int getReactorRodNeutronEmission(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack) {
		return 0;
	}
	
	@Override
	public boolean getReactorRodNeutronReaction(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack) {
		aReactor.mEnergy += aReactor.oNeutronCounts[aSlot] / 2;
		mDurability -= aReactor.oNeutronCounts[aSlot];
		if (mDurability <= 0) {
			ST.meta(aStack, mProduct);
			ST.nbt(aStack, null);
			mDurability = 0;
			aReactor.updateClientData();
		}
		UT.NBT.set(aStack, writeItemNBT(ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make()));
		return T;
	}
	
	@Override
	public int getReactorRodNeutronReflection(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack, int aNeutrons, boolean aModerated) {
		if (!aModerated && aNeutrons > mNeutronLoss) aReactor.mNeutronCounts[aSlot] += aNeutrons - mNeutronLoss;
		return 0;
	}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.generator.reactor.rods.breeder";}
}
