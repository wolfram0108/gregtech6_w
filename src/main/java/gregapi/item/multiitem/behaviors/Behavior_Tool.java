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

import gregapi.block.IBlockToolable;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.util.UT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

public class Behavior_Tool extends AbstractBehaviorDefault {
	public final String mToolName, mSoundName;
	public final long mDamage;
	public final boolean mOnItemUseReturn;
	public final float mPitch;
	
	public Behavior_Tool(String aToolName) {this(aToolName, null, 0, T, 1.0F);}
	public Behavior_Tool(String aToolName, boolean aOnItemUseReturn) {this(aToolName, null, 0, aOnItemUseReturn, 1.0F);}
	public Behavior_Tool(String aToolName, long aDamage, boolean aOnItemUseReturn) {this(aToolName, null, aDamage, aOnItemUseReturn, 1.0F);}
	public Behavior_Tool(String aToolName, String aSoundName, long aDamage, boolean aOnItemUseReturn) {this(aToolName, aSoundName, aDamage, aOnItemUseReturn, 1.0F);}
	public Behavior_Tool(String aToolName, String aSoundName, long aDamage, boolean aOnItemUseReturn, boolean aRandomPitch) {this(aToolName, aSoundName, aDamage, aOnItemUseReturn, aRandomPitch ? SFX.RANDOM_PITCH : 1.0F);}
	public Behavior_Tool(String aToolName, String aSoundName, long aDamage, boolean aOnItemUseReturn, float aPitch) {
		mToolName = aToolName;
		mSoundName = aSoundName;
		mDamage = aDamage;
		mOnItemUseReturn = aOnItemUseReturn;
		mPitch = aPitch;
	}
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
//      if (aPlayer != null && SIDES_VALID[aSide] && !(aPlayer instanceof FakePlayer) && UT.Worlds.isSideObstructed(aWorld, aX, aY, aZ, aSide)) return !aWorld.isClientSide();
		List<String> tChatReturn = new ArrayListNoNulls<>();
		long tDamage = IBlockToolable.Util.onToolClick(mToolName, Long.MAX_VALUE, (aItem instanceof MultiItemTool ? ((MultiItemTool)aItem).getHarvestLevel(aStack, mToolName) : 1), aPlayer, tChatReturn, aPlayer==null?null:aPlayer.getInventory(), aPlayer!=null&&aPlayer.isShiftKeyDown(), aStack, aWorld, aSide, aX, aY, aZ, aHitX, aHitY, aHitZ);
		UT.Entities.sendchat(aPlayer, tChatReturn, F);
		// [GT6-SOUNDDIAG] BUG-113: самописец взаимодействия инструментом (гейт run/gt6sounddiag.flag) — снять при уборке фазы
		if (gregapi.data.CS.probeFlag("gt6sounddiag.flag")) gregapi.data.CS.OUT.println("[GT6-SOUNDDIAG] инструмент=" + mToolName
			+ " сторона=" + (aWorld == null ? "?" : aWorld.isClientSide() ? "КЛИЕНТ" : "сервер")
			+ " блок=" + (aWorld == null ? "?" : aWorld.getBlockState(new net.minecraft.core.BlockPos(aX, aY, aZ)).getBlock())
			+ " износ=" + tDamage + " звук=" + mSoundName);
		if (tDamage > 0) {
			if (mDamage > 0) ((MultiItemTool)aItem).doDamage(aStack, UT.Code.units(tDamage, 10000, mDamage, T), aPlayer, F);
			if (mSoundName != null) {
				boolean tSent = UT.Sounds.send(mSoundName, 1.0F, mPitch, aWorld, aX, aY, aZ);
				if (gregapi.data.CS.probeFlag("gt6sounddiag.flag")) gregapi.data.CS.OUT.println("[GT6-SOUNDDIAG]   -> звук " + mSoundName + " отправлен=" + tSent);
			}
			return !aWorld.isClientSide();
		}
		return F;
	}
	
	@Override
	public boolean onItemUse(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		return mOnItemUseReturn;
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.Chat.DGRAY + LH.get(TOOL_LOCALISER_PREFIX + mToolName, "Unknown") + "   " + LH.Chat.GRAY + LH.get(TOOL_TOOLTIP_PREFIX + mToolName, ""));
		return aList;
	}
}
