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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.tileentity.notick;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.util.FakePlayer;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public abstract class TileEntityBase03MultiTileEntities extends TileEntityBase02Sync implements IRenderedBlockObjectSideCheck, IRenderedBlockObject, IMTE_OnPainting, IMTE_GetPickBlock, IMTE_GetStackFromBlock, IMTE_OnRegistrationFirst, IMTE_RecolourBlock, IMTE_GetDrops, IMTE_OnBlockActivated, IMTE_ShouldSideBeRendered {
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
	
	private short mMTEID = W, mMTERegistry = W;
	private String mCustomName = null;
	
	/** return the internal Name of this TileEntity to be registered. DO NOT START YOUR NAME WITH "gt."!!! */
	public abstract String getTileEntityName();
	
	@Override
	public void onRegistrationFirst(MultiTileEntityRegistry aRegistry, short aID) {
		// F12 SUPERSEDED (не заглушка): было GameRegistry.registerTileEntity(getClass(), getTileEntityName()) — регистрация
		// КЛАССА тайла под именем. MultiTileEntity — динамическая система (32000 вариантов на класс), не сводимая к
		// neo-модели «тип-на-класс»; ВСЯ иерархия обслуживается ОДНИМ generic BlockEntityType MTE_TYPE (GT_API.java:195,
		// TileEntityBase01Root::createType, RegisterEvent<BlockEntityType>). Per-MTE регистрация здесь не нужна —
		// parity mte.csv=100% доказывает: все MTE регистрируются/работают через generic-тип (адаптер построен, не отложен).
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
		if (aNBT != null) readFromNBT(aNBT);
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
		if (WD.tileYInvalid(getLevel(), getBlockPos().getY())) WD.invalidateTileEntityWithNegativeYCoord(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), this); // было Y<0 — порог = дно мира getMinY() (бедрок MC26 Y=−64 легитимен; notick-MTE: камни/палки)
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
	public final boolean onBlockActivated(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		level.blockEntityChanged(getBlockPos());
		return allowRightclick(aPlayer) && (checkObstruction(aPlayer, aSide, aHitX, aHitY, aHitZ) || onBlockActivated2(aPlayer, aSide, aHitX, aHitY, aHitZ));
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
		MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(getMultiTileEntityRegistryID());
		if (tRegistry != null) rList.add(tRegistry.getItem(getMultiTileEntityID(), writeItemNBT(UT.NBT.make())));
		return rList;
	}
	
	@Override
	public boolean recolourBlock(byte aSide, byte aColor) {
		if (UT.Code.exists(aColor, DYES_INVERTED)) {
			int aRGB = (isPainted() ? UT.Code.mixRGBInt(DYES_INT_INVERTED[aColor], getPaint()) : DYES_INT_INVERTED[aColor]) & ALL_NON_ALPHA_COLOR;
			if (paint(aRGB)) {updateClientData(); causeBlockUpdate(); level.blockEntityChanged(getBlockPos()); return T;}
			return F;
		}
		if (unpaint()) {updateClientData(); causeBlockUpdate(); level.blockEntityChanged(getBlockPos()); return T;}
		return F;
	}
	
	@Override
	public boolean onPainting(byte aSide, int aRGB) {
		if (paint(aRGB)) {updateClientData(); causeBlockUpdate(); level.blockEntityChanged(getBlockPos()); return T;}
		return F;
	}
	
	public boolean unpaint() {return F;}
	public boolean isPainted() {return F;}
	public boolean paint(int aRGB) {return F;}
	public int getPaint() {return UNCOLORED;}
	
	@Override public String getCustomName() {return UT.Code.stringValid(mCustomName) ? mCustomName : null;}
	@Override public void setCustomName(String aName) {mCustomName = aName;}
	@Override public ItemStack getPickBlock(HitResult aTarget) {MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(mMTERegistry); return tRegistry == null ? null : tRegistry.getItem(mMTEID, writeItemNBT(UT.NBT.make()));}
	@Override public ItemStack getStackFromBlock(byte aSide) {MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(mMTERegistry); return tRegistry == null ? null : tRegistry.getItem(mMTEID, writeItemNBT(UT.NBT.make()));}
	@Override public short getMultiTileEntityID() {return mMTEID;}
	@Override public short getMultiTileEntityRegistryID() {return mMTERegistry;}
	@Override public void setShouldRefresh(boolean aShouldRefresh) {mShouldRefresh = aShouldRefresh;}
}
