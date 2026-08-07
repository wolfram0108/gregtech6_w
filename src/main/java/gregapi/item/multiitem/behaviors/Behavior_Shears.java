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

package gregapi.item.multiitem.behaviors;

import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.IShearable;

import java.util.List;

import static gregapi.data.CS.*;

public class Behavior_Shears extends AbstractBehaviorDefault {
	private final int mCosts;
	
	public Behavior_Shears(int aCosts) {
		mCosts = aCosts;
	}
	
	@Override
	public boolean onLeftClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		if (aEntity instanceof IShearable) {
			if (aPlayer.level().isClientSide()) return T;
			if (((IShearable)aEntity).isShearable(aStack, aPlayer.level(), (int)aEntity.getX(), (int)aEntity.getY(), (int)aEntity.getZ()) && ((MultiItemTool)aItem).doDamage(aStack, mCosts, aPlayer, F)) {
				int tFortune = UT.NBT.getEnchantmentLevelLootingFortune(aStack);
				String tClass = UT.Reflection.getLowercaseClass(aEntity);
				boolean tDropIncrease = ((tFortune > 0) && ("Sheep".equalsIgnoreCase(tClass) || "EntityTFBighorn".equalsIgnoreCase(tClass) || "EntityTaintSheep".equalsIgnoreCase(tClass) || "EntitySheepuff".equalsIgnoreCase(tClass)));
				for (ItemStack tStack : ((IShearable)aEntity).onSheared(aStack, aPlayer.level(), (int)aEntity.getX(), (int)aEntity.getY(), (int)aEntity.getZ(), tFortune)) {
					// 1.7.10 `Blocks.wool` — ОДИН блок с метой-цветом, т.е. проверка ловила шерсть ЛЮБОГО цвета.
					// В neo семья расщеплена на 16 блоков: сравнение с одним из них дало бы бонус только за белую.
					// Спрашиваем главу семьи через центр CS.Flattened (тот же приём, что в Behavior_Spray_Color_Remover:107).
					if (tDropIncrease && gregapi.data.CS.Flattened.headOf(ST.block(tStack)) == Blocks.WHITE_WOOL) {
						tStack.setCount(tStack.getCount()+(RNGSUS.nextInt(1+tFortune)));
						if (tStack.getCount() > 64) tStack.setCount(64);
					}
					ST.give(aPlayer, tStack, F);
				}
				return T;
			}
		}
		return F;
	}
	
	@Override
	public boolean onRightClickEntity(MultiItem aItem, ItemStack aStack, Player aPlayer, Entity aEntity) {
		return onLeftClickEntity(aItem, aStack, aPlayer, aEntity);
	}
	
	static {
		LH.add("gt.behaviour.shears", "Shaves Sheep, Mooshrooms and alike");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.shears"));
		return aList;
	}
}
