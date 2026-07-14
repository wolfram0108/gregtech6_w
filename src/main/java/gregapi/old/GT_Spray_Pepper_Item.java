/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.old;
import gregapi.util.WD;

import java.util.List;

import gregapi.lang.LanguageHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GT_Spray_Pepper_Item extends GT_Tool_Item {
	public GT_Spray_Pepper_Item(String aUnlocalized, String aEnglish, int aMaxDamage, int aEntityDamage) {
		super(aUnlocalized, aEnglish, "To defend yourself against Bears", aMaxDamage, aEntityDamage, true);/*
		setCraftingSound(GregTech_API.sSoundList.get(102));
		setBreakingSound(GregTech_API.sSoundList.get(102));
		setEntityHitSound(GregTech_API.sSoundList.get(102));
		setUsageAmounts(1, 8, 1);*/
	}
	
	@Override
	public void addAdditionalToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(LanguageHandler.get(getUnlocalizedName() + ".tooltip_1", "especially Pedobears, Care Bears,"));
		aList.add(LanguageHandler.get(getUnlocalizedName() + ".tooltip_2", "Confession Bears, Bear Grylls"));
		aList.add(LanguageHandler.get(getUnlocalizedName() + ".tooltip_3", "And ofcourse Man-Bear-Pig"));
	}
	/*
	@Override
	public void onHitEntity(Entity aEntity) {
		if (aEntity instanceof EntityLiving) {
			((EntityLiving)aEntity).addPotionEffect(new PotionEffect(Potion.blindness.getId(), 1200, 2, false));
			((EntityLiving)aEntity).addPotionEffect(new PotionEffect(Potion.poison.getId(), 120, 2, false));
			((EntityLiving)aEntity).addPotionEffect(new PotionEffect(Potion.weakness.getId(), 200, 2, false));
			((EntityLiving)aEntity).addPotionEffect(new PotionEffect(Potion.confusion.getId(), 600, 2, false));
		}
	}
	*/
	
	// @Override
	public boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float hitX, float hitY, float hitZ) {
		// F-item-use: 1.7.10 super.onItemUseFirst(старая сигнатура) = дефолт Item (no-op, false); neo сменил на
		// (ItemStack,UseOnContext) — vestigial-вызов убран (результат отбрасывался, метод возвращает false).
		if (aWorld.isClientSide()) {
			return false;
		}
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == null) return false;
//      byte aMeta = (byte)aWorld.getBlockMetadata(aX, aY, aZ);
//      TileEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		
		return false;
	}
}
