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

package gregtech.items.behaviors;

import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.data.CS.SFX;
import gregapi.data.LH;
import gregapi.item.multiitem.MultiItem;
import gregapi.item.multiitem.behaviors.IBehavior;
import gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault;
import gregapi.tileentity.ITileEntityRemoteActivateable;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

public class Behavior_Remote extends AbstractBehaviorDefault {
	public static final IBehavior<MultiItem> INSTANCE = new Behavior_Remote();
	
	@Override
	public boolean onItemUseFirst(MultiItem aItem, ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (aWorld.isClientSide() || aPlayer == null || !aPlayer.isShiftKeyDown() || !(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
		CompoundTag aNBT = UT.NBT.getNBT(aStack);
		ArrayListNoNulls<BlockPos> tList = getCoords(aNBT, aWorld.provider.dimensionId);
		BlockPos tCoords = new BlockPos(aX, aY, aZ);
		if (tList.contains(tCoords)) {
			UT.Entities.sendchat(aPlayer, "Coordinates removed!");
			UT.Sounds.send(SFX.GT_BEEP, 0.5F, 1.0F, aWorld, tCoords);
			tList.remove(tCoords);
		} else if (tList.size() >= 64) {
			UT.Entities.sendchat(aPlayer, "Cant hold more than 64 Coordinates per Dimension!");
			UT.Sounds.send(SFX.GT_BEEP, 0.5F, 0.5F, aWorld, tCoords);
		} else {
			BlockEntity tTileEntity = WD.te(aWorld, tCoords, F);
			if (tTileEntity instanceof ITileEntityRemoteActivateable) {
				UT.Entities.sendchat(aPlayer, "Coordinates added!");
				UT.Sounds.send(SFX.GT_BEEP, 0.5F, 1.0F, aWorld, tCoords);
				tList.add(tCoords);
			} else {
				UT.Entities.sendchat(aPlayer, "This cannot be added!");
				UT.Sounds.send(SFX.GT_BEEP, 0.5F, 0.5F, aWorld, tCoords);
			}
		}
		setCoords(aNBT, aWorld.provider.dimensionId, tList);
		UT.NBT.set(aStack, aNBT);
		return T;
	}
	
	// F8: тег захвачен ОДИН раз в aNBT, getCoords его читает, setCoords его мутирует, коммит единый
	// ItemNBT.set в конце — иначе правка списка координат из setCoords тихо терялась бы (см. ItemNBT.java).
	@Override
	public ItemStack onItemRightClick(MultiItem aItem, ItemStack aStack, Level aWorld, Player aPlayer) {
		if (aWorld.isClientSide() || aPlayer.isShiftKeyDown() || !ItemNBT.has(aStack)) return aStack;
		CompoundTag aNBT = ItemNBT.get(aStack);
		ArrayListNoNulls<BlockPos> tToBeKept = new ArrayListNoNulls<>();
		for (BlockPos tCoords : getCoords(aNBT, aWorld.provider.dimensionId)) {
			if (Math.abs(tCoords.getX() - aPlayer.getX()) <= 128 && Math.abs(tCoords.getY() - aPlayer.getY()) <= 128 && Math.abs(tCoords.getZ() - aPlayer.getZ()) <= 128) {
				BlockEntity tTileEntity = WD.te(aWorld, tCoords, F);
				if (tTileEntity instanceof ITileEntityRemoteActivateable && ((ITileEntityRemoteActivateable)tTileEntity).remoteActivate()) tToBeKept.add(tCoords);
			} else {
				tToBeKept.add(tCoords);
			}
		}
		setCoords(aNBT, aWorld.provider.dimensionId, tToBeKept);
		ItemNBT.set(aStack, aNBT);
		UT.Sounds.send(SFX.MC_CLICK, aPlayer);
		return aStack;
	}
	
	public static boolean addCoords(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ) {
		CompoundTag aNBT = UT.NBT.getNBT(aStack);
		ArrayListNoNulls<BlockPos> tList = getCoords(aNBT, aWorld.provider.dimensionId);
		if (tList.size() >= 64) return F;
		BlockPos tCoords = new BlockPos(aX, aY, aZ);
		if (tList.contains(tCoords)) return T;
		BlockEntity tTileEntity = WD.te(aWorld, tCoords, F);
		if (tTileEntity instanceof ITileEntityRemoteActivateable) {
			UT.Sounds.send(SFX.GT_BEEP, 0.5F, 1.0F, aWorld, tCoords);
			tList.add(tCoords);
			setCoords(aNBT, aWorld.provider.dimensionId, tList);
			UT.NBT.set(aStack, aNBT);
			return T;
		}
		UT.Sounds.send(SFX.GT_BEEP, 0.5F, 0.5F, aWorld, tCoords);
		return F;
	}
	
	public static ArrayListNoNulls<BlockPos> getCoords(CompoundTag aNBT, int aDimension) {
		ArrayListNoNulls<BlockPos> rList = new ArrayListNoNulls<>();
		if (aNBT == null) return rList;
		CompoundTag tNBT = aNBT.getCompoundOrEmpty("gt.remote.dim."+aDimension);
		if (tNBT.isEmpty()) return rList;
		int i = -1; while (tNBT.contains("c"+(++i))) {
			rList.add(new BlockPos(tNBT.getIntOr("x"+i, 0), tNBT.getIntOr("y"+i, 0), tNBT.getIntOr("z"+i, 0)));
		}
		return rList;
	}
	
	public static void setCoords(CompoundTag aNBT, int aDimension, ArrayListNoNulls<BlockPos> aList) {
		if (aList.isEmpty()) {
			aNBT.removeTag("gt.remote.dim."+aDimension);
		} else {
			CompoundTag tNBT = UT.NBT.make();
			for (int i = 0, j = aList.size(); i < j; i++) {
				BlockPos tCoords = aList.get(i);
				UT.NBT.setBoolean(tNBT, "c"+i, T);
				UT.NBT.setNumber (tNBT, "x"+i, tCoords.getX());
				UT.NBT.setNumber (tNBT, "y"+i, tCoords.getY());
				UT.NBT.setNumber (tNBT, "z"+i, tCoords.getZ());
			}
			aNBT.put("gt.remote.dim."+aDimension, tNBT);
		}
	}
	
	static {
		LH.add("gt.behaviour.remote", "Activates up to 64 Blocks within a Range of 128m");
	}
	
	@Override
	public List<String> getAdditionalToolTips(MultiItem aItem, List<String> aList, ItemStack aStack) {
		aList.add(LH.get("gt.behaviour.remote"));
		return aList;
	}
}
