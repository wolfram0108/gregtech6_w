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

package gregapi.tileentity.base;

import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.block.multitileentity.MultiTileEntityClassContainer;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ArrayListNoNulls;
import gregapi.network.IPacket;
import gregapi.network.packets.data.*;
import gregapi.network.packets.ids.*;
import gregapi.render.IRenderedBlockObject;
import gregapi.render.IRenderedBlockObjectSideCheck;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class TileEntityBase04MultiTileEntities extends TileEntityBase03TicksAndSync implements IRenderedBlockObjectSideCheck, IRenderedBlockObject, IMTE_OnPainting, IMTE_OnNeighborBlockChange, IMTE_GetPickBlock, IMTE_OnRegistrationFirst, IMTE_RecolourBlock, IMTE_GetDrops, IMTE_OnBlockActivated, IMTE_ShouldSideBeRendered, IMTE_GetFlammability, IMTE_GetFireSpreadSpeed {
	private short mMTEID = W, mMTERegistry = W;
	private String mCustomName = null;
	
	// Function has to stay even though I moved it up to the root class, because compatibility.
	/** return the internal Name of this TileEntity to be registered. DO NOT START YOUR NAME WITH "gt."!!! */
	public abstract String getTileEntityName();
	
	@Override
	public void onRegistrationFirst(MultiTileEntityRegistry aRegistry, short aID) {
		// PORT-TODO(F12-entity): прежний вызов DeferredRegister.registerTileEntity(Class, String) —
		// выдуманный API (1.7.10 GameRegistry.registerTileEntity механически переименован в
		// DeferredRegister, которого там нет). Реальный путь — DeferredRegister<BlockEntityType<?>> +
		// BlockEntityType.Builder(BlockEntitySupplier, valid-blocks) через центр gregapi.GT_API. Но
		// MultiTileEntity — динамическая система (32000 вариантов на один тип), к статичной модели
		// BlockEntityType neo не сводится 1:1; TILEENTITY-TYPE адаптер отдельным ADR не разработан —
		// не выдумываю. Тот же PORT-TODO, что в GT_API.java (регистрация PrefixBlockTileEntity).
	}
	
	@Override
	public IPacket getClientDataPacket(boolean aSendAll)                                {return aSendAll ? new PacketSyncDataIDs                (getCoords(), getMultiTileEntityRegistryID(), getMultiTileEntityID()                ) : null;}
	public IPacket getClientDataPacketByte(boolean aSendAll, byte aByte)                {return aSendAll ? new PacketSyncDataByteAndIDs         (getCoords(), getMultiTileEntityRegistryID(), getMultiTileEntityID(), aByte         ) : new PacketSyncDataByte      (getCoords(), aByte         );}
	public IPacket getClientDataPacketShort(boolean aSendAll, short aShort)             {return aSendAll ? new PacketSyncDataShortAndIDs        (getCoords(), getMultiTileEntityRegistryID(), getMultiTileEntityID(), aShort        ) : new PacketSyncDataShort     (getCoords(), aShort        );}
	public IPacket getClientDataPacketInteger(boolean aSendAll, int aInteger)           {return aSendAll ? new PacketSyncDataIntegerAndIDs      (getCoords(), getMultiTileEntityRegistryID(), getMultiTileEntityID(), aInteger      ) : new PacketSyncDataInteger   (getCoords(), aInteger      );}
	public IPacket getClientDataPacketLong(boolean aSendAll, long aLong)                {return aSendAll ? new PacketSyncDataLongAndIDs         (getCoords(), getMultiTileEntityRegistryID(), getMultiTileEntityID(), aLong         ) : new PacketSyncDataLong      (getCoords(), aLong         );}
	public IPacket getClientDataPacketByteArray(boolean aSendAll, byte... aByteArray)   {return aSendAll ? new PacketSyncDataByteArrayAndIDs    (getCoords(), getMultiTileEntityRegistryID(), getMultiTileEntityID(), aByteArray    ) : new PacketSyncDataByteArray (getCoords(), aByteArray    );}
	
	@Override
	public final void initFromNBT(CompoundTag aNBT, short aMTEID, short aMTERegistry) {
		// Set ID and Registry ID.
		mMTEID = aMTEID;
		mMTERegistry = aMTERegistry;
		// Read the Default Parameters from NBT.
		readFromNBT(aNBT == null ? UT.NBT.make() : aNBT);
	}
	
	@Override
	public final void readFromNBT(CompoundTag aNBT) {
		// Check if this is a World/Chunk Loading Process calling readFromNBT.
		if (mMTEID == W || mMTERegistry == W) {
			// Yes it is, so read the ID Tags first.
			mMTEID = aNBT.getShort(NBT_MTE_ID).orElse((short)0);
			mMTERegistry = aNBT.getShort(NBT_MTE_REG).orElse((short)0);
			// And add additional Default Parameters, in case the Mod updated with new ones.
			MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(mMTERegistry);
			if (tRegistry != null) {
				MultiTileEntityClassContainer tClass = tRegistry.getClassContainer(mMTEID);
				if (tClass != null) {
					// Add the Default Parameters.
					aNBT = UT.NBT.fuse(aNBT, tClass.mParameters);
				}
			}
		}
		// read the Coords if it has them.
		// F8: BlockPos на BlockEntity в 26.1.2 неизменяем и уже верно выставлен движком до
		// вызова loadAdditional (см. комментарий в TileEntityBase01Root.readFromNBT) -
		// назначить x/y/z из NBT здесь невозможно и не нужно.
		// make sure Y is not negative because this causes crashes.
		if (getBlockPos().getY() < 0) WD.invalidateTileEntityWithNegativeYCoord(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), this);
		// read the custom Name.
		if (aNBT.contains("display")) mCustomName = aNBT.getCompoundOrEmpty("display").getString("Name").orElse("");
		// And now your custom readFromNBT.
		try {readFromNBT2(aNBT);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	public void readFromNBT2(CompoundTag aNBT) {/**/}
	
	@Override
	public final void writeToNBT(CompoundTag aNBT) {
		super.writeToNBT(aNBT);
		// write the IDs
		aNBT.putShort(NBT_MTE_ID, mMTEID);
		aNBT.putShort(NBT_MTE_REG, mMTERegistry);
		// write the Custom Name
		if (UT.Code.stringValid(mCustomName)) aNBT.put("display", UT.NBT.makeString(aNBT.getCompoundOrEmpty("display"), "Name", mCustomName));
		if (isPainted()) {aNBT.putInt(NBT_COLOR, getPaint()); aNBT.putBoolean(NBT_PAINTED, T);}
		// write the rest
		try {writeToNBT2(aNBT);} catch(Throwable e) {e.printStackTrace(ERR);}
	}
	
	public void writeToNBT2(CompoundTag aNBT) {/**/}
	
	@Override
	public CompoundTag writeItemNBT(CompoundTag aNBT) {
		if (UT.Code.stringValid(mCustomName)) aNBT.put("display", UT.NBT.makeString(aNBT.getCompoundOrEmpty("display"), "Name", mCustomName));
		if (UT.Code.stringValid(ERROR_MESSAGE) && isClientSide()) aNBT.put("display", UT.NBT.makeString(aNBT.getCompoundOrEmpty("display"), "Name", ERROR_MESSAGE));
		if (isPainted()) {aNBT.putInt(NBT_COLOR, getPaint()); aNBT.putBoolean(NBT_PAINTED, T);}
		return aNBT;
	}
	
	@Override
	public void sendClientData(boolean aSendAll, ServerPlayer aPlayer) {
		super.sendClientData(aSendAll, aPlayer);
		if (aSendAll && UT.Code.stringValid(mCustomName)) {
			if (aPlayer == null) {
				getNetworkHandler().sendToAllPlayersInRange(new PacketSyncDataName(getCoords(), mCustomName), level, getCoords());
			} else {
				getNetworkHandler().sendToPlayer(new PacketSyncDataName(getCoords(), mCustomName), aPlayer);
			}
		}
	}
	
	@Override
	public final boolean onBlockActivated(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		try {
			return allowRightclick(aPlayer) && (checkObstruction(aPlayer, aSide, aHitX, aHitY, aHitZ) || onBlockActivated2(aPlayer, aSide, aHitX, aHitY, aHitZ));
		} catch(Throwable e) {
			e.printStackTrace(ERR);
			return T;
		}
	}
	
	public boolean onBlockActivated2(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		return F;
	}
	
	public boolean checkObstruction(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		return !(aPlayer == null || aPlayer instanceof FakePlayer || SIDES_INVALID[aSide] || !WD.obstructed(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), aSide));
	}
	
	@Override
	public ArrayListNoNulls<ItemStack> getDrops(int aFortune, boolean aSilkTouch) {
		ArrayListNoNulls<ItemStack> rList = ST.arraylist();
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(mMTERegistry);
		if (tRegistry != null) rList.add(tRegistry.getItem(mMTEID, writeItemNBT(UT.NBT.make())));
		return rList;
	}
	
	public void popOff() {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), tStack);
		setToAir();
	}
	public void popOff(Entity aEntity) {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(aEntity.level(), aEntity.getX(), aEntity.getY(), aEntity.getZ(), tStack);
		setToAir();
	}
	public void popOff(Level aWorld, double aX, double aY, double aZ) {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(aWorld, aX, aY, aZ, tStack);
		setToAir();
	}
	public void popOff(Level aWorld, BlockPos aCoords) {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(aWorld, aCoords, tStack);
		setToAir();
	}
	
	public void burnOff() {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), tStack);
		setToFire();
	}
	public void burnOff(Entity aEntity) {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(aEntity.level(), aEntity.getX(), aEntity.getY(), aEntity.getZ(), tStack);
		setToFire();
	}
	public void burnOff(Level aWorld, double aX, double aY, double aZ) {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(aWorld, aX, aY, aZ, tStack);
		setToFire();
	}
	public void burnOff(Level aWorld, BlockPos aCoords) {
		if (isDead()) return;
		for (ItemStack tStack : getDrops(0, F)) ST.drop(aWorld, aCoords, tStack);
		setToFire();
	}
	
	public ItemStack toStack() {
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(mMTERegistry);
		if (tRegistry != null) return tRegistry.getItem(mMTEID, writeItemNBT(UT.NBT.make()));
		return null;
	}
	
	@Override
	public void onNeighborBlockChange(Level aWorld, Block aBlock) {
		mBlockUpdated = T;
	}
	
	@Override
	public boolean recolourBlock(byte aSide, byte aColor) {
		if (UT.Code.exists(aColor, DYES_INVERTED)) {
			int aRGB = (isPainted() ? UT.Code.mixRGBInt(DYES_INT_INVERTED[aColor], getPaint()) : DYES_INT_INVERTED[aColor]) & ALL_NON_ALPHA_COLOR;
			if (paint(aRGB)) {updateClientData(); causeBlockUpdate(); return T;}
			return F;
		}
		if (unpaint()) {updateClientData(); causeBlockUpdate(); return T;}
		return F;
	}
	
	@Override
	public boolean onPainting(byte aSide, int aRGB) {
		if (paint(aRGB)) {updateClientData(); causeBlockUpdate(); return T;}
		return F;
	}
	
	public boolean unpaint() {return F;}
	public boolean isPainted() {return F;}
	public boolean paint(int aRGB) {return F;}
	public int getPaint() {return UNCOLORED;}
	
	public boolean removePaint(byte aSide) {return unpaint();}
	
	@Override public String getCustomName() {return UT.Code.stringValid(mCustomName) ? mCustomName : null;}
	@Override public void setCustomName(String aName) {mCustomName = aName;}
	@Override public ItemStack getPickBlock(HitResult aTarget) {MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(mMTERegistry); return tRegistry == null ? null : tRegistry.getItem(mMTEID, writeItemNBT(UT.NBT.make()));}
	@Override public short getMultiTileEntityID() {return mMTEID;}
	@Override public short getMultiTileEntityRegistryID() {return mMTERegistry;}
	@Override public void setShouldRefresh(boolean aShouldRefresh) {mShouldRefresh = aShouldRefresh;}
}
