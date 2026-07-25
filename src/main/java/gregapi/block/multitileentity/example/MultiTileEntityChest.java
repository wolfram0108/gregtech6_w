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

package gregapi.block.multitileentity.example;

import net.neoforged.api.distmarker.Dist;
import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.block.multitileentity.MultiTileEntityBlockInternal;
import gregapi.block.multitileentity.MultiTileEntityContainer;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ItemNBT;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.gui.ContainerClientChest;
import gregapi.gui.ContainerCommonChest;
import gregapi.item.IItemColorableRGB;
import gregapi.network.INetworkHandler;
import gregapi.network.IPacket;
import gregapi.oredict.OreDictMaterial;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityAdjacentInventoryUpdatable;
import gregapi.tileentity.ITileEntityDecolorable;
import gregapi.tileentity.base.TileEntityBase05Inventories;
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * An example implementation of a Chest with my MultiTileEntity System.
 */
public class MultiTileEntityChest extends TileEntityBase05Inventories implements IMTE_IsProvidingWeakPower, IMTE_IsProvidingStrongPower, IItemColorableRGB, ITileEntityDecolorable, ITileEntitySurface, IMTE_OnRegistrationClient, IMTE_OnRegistrationFirstClient, IMTE_SyncDataByte, IMTE_AddToolTips, IMTE_SetBlockBoundsBasedOnState, IMTE_GetSubItems, IMTE_SyncDataByteArray, IMTE_GetExplosionResistance, IMTE_GetBlockHardness, IMTE_GetComparatorInputOverride, IMTE_GetSelectedBoundingBoxFromPool, IMTE_GetCollisionBoundingBoxFromPool, IMTE_OnPlaced, IMTE_OnToolClick {
	protected boolean mIsPainted = F, mIsTrapped = F;
	protected int mRGBa = UNCOLORED;
	protected byte mFacing = 3, mUsingPlayers = 0, oUsingPlayers = 0;
	protected float mLidAngle = 0, oLidAngle = 0, mHardness = 6, mResistance = 3;
	protected OreDictMaterial mMaterial = MT.NULL;
	
	/** Gets supplied via Default NBT. */
	public String mTextureName = "", mDungeonLootName = "";
	
	public MultiTileEntityChest() {/**/}
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_COLOR)) mRGBa = aNBT.getIntOr(NBT_COLOR, 0);
		if (aNBT.contains(NBT_FACING)) mFacing = aNBT.getByte(NBT_FACING).orElse((byte)0);
		if (aNBT.contains(NBT_PAINTED)) mIsPainted = aNBT.getBoolean(NBT_PAINTED).orElse(false);
		if (aNBT.contains(NBT_TRAPPED)) mIsTrapped = aNBT.getBoolean(NBT_TRAPPED).orElse(false);
		if (aNBT.contains(NBT_TEXTURE)) mTextureName = aNBT.getString(NBT_TEXTURE).orElse("");
		if (aNBT.contains("gt.dungeonloot")) mDungeonLootName = aNBT.getString("gt.dungeonloot").orElse("");
		if (aNBT.contains(NBT_HARDNESS)) mHardness = aNBT.getFloat(NBT_HARDNESS).orElse(0F);
		if (aNBT.contains(NBT_RESISTANCE)) mResistance = aNBT.getFloat(NBT_RESISTANCE).orElse(0F);
		if (aNBT.contains(NBT_MATERIAL)) mMaterial = OreDictMaterial.get(aNBT.getString(NBT_MATERIAL).orElse(""));
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		aNBT.putByte(NBT_FACING, mFacing);
		UT.NBT.setBoolean(aNBT, NBT_TRAPPED, mIsTrapped);
		if (UT.Code.stringValid(mDungeonLootName)) aNBT.putString("gt.dungeonloot", mDungeonLootName);
	}
	
	@Override
	public CompoundTag writeItemNBT(CompoundTag aNBT) {
		aNBT = super.writeItemNBT(aNBT);
		if (UT.Code.stringValid(mDungeonLootName)) aNBT.putString("gt.dungeonloot", mDungeonLootName);
		return aNBT;
	}
	
	@Override
	public IPacket getClientDataPacket(boolean aSendAll) {
		return getClientDataPacketByteArray(aSendAll, mFacing, mUsingPlayers, (byte)getContainerSize(), (byte)UT.Code.getR(mRGBa), (byte)UT.Code.getG(mRGBa), (byte)UT.Code.getB(mRGBa));
	}
	
	@Override
	public boolean onPlaced(ItemStack aStack, Player aPlayer, MultiTileEntityContainer aMTEContainer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		mFacing = UT.Code.getSideForPlayerPlacing(aPlayer, mFacing, SIDES_HORIZONTAL);
		return T;
	}
	
	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (aIsServerSide) {
			if (mInventoryChanged) {
				for (byte tSide : ALL_SIDES_VALID) {
					DelegatorTileEntity<BlockEntity> tDelegator = getAdjacentTileEntity(tSide);
					if (tDelegator.mTileEntity instanceof ITileEntityAdjacentInventoryUpdatable) {
						((ITileEntityAdjacentInventoryUpdatable)tDelegator.mTileEntity).adjacentInventoryUpdated(tDelegator.mSideOfTileEntity, this);
					}
				}
			}
			if (mUsingPlayers > 0 && aTimer % 1200 == 0) {
				mUsingPlayers = UT.Code.bind7(getOpenGUIs());
			}
		} else {
			oLidAngle = mLidAngle;
			if (mUsingPlayers > 0) {
				mLidAngle = Math.min(1, mLidAngle+0.1F);
				if (mLidAngle > 0.1F && oLidAngle <= 0.1F) UT.Sounds.play("random.chestopen"  , 2, 0.5F, RNGSUS.nextFloat() * 0.1F + 0.9F, getCoords());
			} else {
				mLidAngle = Math.max(0, mLidAngle-0.1F);
				if (mLidAngle < 0.5F && oLidAngle >= 0.5F) UT.Sounds.play("random.chestclosed", 2, 0.5F, RNGSUS.nextFloat() * 0.1F + 0.9F, getCoords());
			}
		}
	}
	
	@Override
	public boolean onTickCheck(long aTimer) {
		return mUsingPlayers != oUsingPlayers || super.onTickCheck(aTimer);
	}
	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		oUsingPlayers = mUsingPlayers;
	}
	
	@Override
	public long onToolClick(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isClientSide()) return 0;
		if (aTool.equals(TOOL_wrench)) {
			byte aTargetSide = UT.Code.getSideWrenching(aSide, aHitX, aHitY, aHitZ);
			if (aTargetSide > 1) {
				mFacing = aTargetSide;
				updateClientData();
				causeBlockUpdate();
				return 10000;
			}
		}
		if (aTool.equals(TOOL_pincers) && aPlayerInventory != null) {
			long rCount = 0;
			for (int i = 0; i < invsize(); i++) if (slotHas(i)) {
				// Check for Achievements so those won't get skipped.
				ST.check(aPlayer, slot(i));
				// Merge Stacks first when applicable.
				for (int j = 0; j < 36; j++) {
					if (ST.equal(slot(i), aPlayerInventory.getItem(j))) {
						rCount += ST.move(this, aPlayerInventory, i, j);
						if (!slotHas(i)) break;
					}
				}
			}
			// Stackable NBT-less Items second.
			for (int i = 0; i < invsize(); i++) if (slotHas(i) && ST.maxsize(slot(i)) > 1 && ST.nbt(slot(i)) == null) {
				for (int j = 9; j < 36; j++) {
					rCount += ST.move(this, aPlayerInventory, i, j);
					if (!slotHas(i)) break;
				}
			}
			// Stackable NBT-containing Items third.
			if (rCount <= 0) for (int i = 0; i < invsize(); i++) if (slotHas(i) && ST.maxsize(slot(i)) > 1) {
				for (int j = 9; j < 36; j++) {
					rCount += ST.move(this, aPlayerInventory, i, j);
					if (!slotHas(i)) break;
				}
			}
			// Unstackable NBT-containing Items fourth.
			if (rCount <= 0) for (int i = 0; i < invsize(); i++) if (slotHas(i) && ST.nbt(slot(i)) != null) {
				for (int j = 9; j < 36; j++) {
					rCount += ST.move(this, aPlayerInventory, i, j);
					if (!slotHas(i)) break;
				}
			}
			// Unstackable NBT-less Items fifth.
			if (rCount <= 0) for (int i = 0; i < invsize(); i++) if (slotHas(i)) {
				for (int j = 9; j < 36; j++) {
					rCount += ST.move(this, aPlayerInventory, i, j);
					if (!slotHas(i)) break;
				}
			}
			// Nothing was done.
			if (rCount <= 0) return 1;
			// Make Sound and update Player Inventory if Items got transferred.
			UT.Sounds.send(SFX.MC_COLLECT, this, F);
			ST.update(aPlayer);
			return rCount;
		}
		return 0;
	}
	
	@Override
	public boolean onBlockActivated2(Player aPlayer, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (isServerSide() && !WD.sideSolid(WD.block(level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ()), level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ(), FORGE_DIR[SIDE_BOTTOM]) && isUseableByPlayerGUI(aPlayer)) {
			generateDungeonLoot();
			openGUI(aPlayer);
		}
		return T;
	}
	
	// No longer generate Loot when harvested, instead pick up the Chest including the Loot it contains!
	//@Override
	//public boolean breakBlock() {
	//  // Only auto-generate Loot if a second has passed since its original placement. Prevents Item spillage during Worldgen in most cases.
	//  if (mTimer > 20) generateDungeonLoot();
	//  return super.breakBlock();
	//}
	
	@Override public boolean canDrop(int aInventorySlot) {return T;}
	@Override public String getTileEntityName() {return "gt.multitileentity.chest";}
	@Override public void openInventoryGUI () {mUsingPlayers++; lidtrace("open" ); if (mIsTrapped) causeBlockUpdate();}
	@Override public void closeInventoryGUI() {mUsingPlayers--; lidtrace("close"); if (mIsTrapped) causeBlockUpdate();}

	// [GT6-LIDTRACE] BUG-059 (крышки в среде игрока; dev-канал доказан чистым): телеметрия каждого сдвига
	// счётчика крышки с вызывателем — читается из gregtech.log игрока. Снять при уборке захода #39.
	private void lidtrace(String aWhat) {
		try {
			StackTraceElement[] tST = new Throwable().getStackTrace();
			StackTraceElement tC = tST.length > 2 ? tST[2] : null;
			OUT.println("[GT6-LIDTRACE] " + (isClientSide() ? "CLIENT" : "SERVER") + " " + aWhat + " @" + getBlockPos().toShortString() + " using=" + mUsingPlayers
				+ " <- " + (tC == null ? "?" : tC.getClassName().substring(tC.getClassName().lastIndexOf('.') + 1) + "." + tC.getMethodName() + ":" + tC.getLineNumber()));
		} catch (Throwable ignored) {/**/}
	}
	@Override public float getExplosionResistance2() {return mResistance;}
	@Override public float getBlockHardness() {return mHardness;}
	@Override public int getComparatorInputOverride(byte aSide) {return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)this);}
	@Override public ITexture getTexture(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {return null;}
	@Override public int getRenderPasses(Block aBlock, boolean[] aShouldSideBeRendered) {return 0;}
	@Override public boolean renderBlock(Block aBlock, Object aRenderer, BlockGetter aWorld, int aX, int aY, int aZ) {return T;}
	
	protected void generateDungeonLoot() {
		if (isServerSide() && UT.Code.stringValid(mDungeonLootName) && ST.generateLoot(RNGSUS, mDungeonLootName, this)) {
			level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.4, getBlockPos().getY()+1.25, getBlockPos().getZ()+0.4, 5+RNGSUS.nextInt(5)+RNGSUS.nextInt(5)));
			level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.4, getBlockPos().getY()+1.25, getBlockPos().getZ()+0.6, 5+RNGSUS.nextInt(5)+RNGSUS.nextInt(5)));
			level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.5, getBlockPos().getY()+1.35, getBlockPos().getZ()+0.5, 5+RNGSUS.nextInt(5)+RNGSUS.nextInt(5)));
			level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.6, getBlockPos().getY()+1.25, getBlockPos().getZ()+0.4, 5+RNGSUS.nextInt(5)+RNGSUS.nextInt(5)));
			level.addFreshEntity(new ExperienceOrb(level, getBlockPos().getX()+0.6, getBlockPos().getY()+1.25, getBlockPos().getZ()+0.6, 5+RNGSUS.nextInt(5)+RNGSUS.nextInt(5)));
			mDungeonLootName = "";
		}
	}
	
	@Override
	public boolean getSubItems(MultiTileEntityBlockInternal aBlock, Item aItem, CreativeModeTab aTab, List<ItemStack> aList, short aID) {
		if (!SHOW_HIDDEN_MATERIALS && mMaterial.mHidden) return F;
		if (D1 || "lootchest".equalsIgnoreCase(mTextureName)) for (String tLoot : ST.LOOT_TABLES) aList.add(aBlock.mMultiTileEntityRegistry.getItem(aID, UT.NBT.makeString("gt.dungeonloot", tLoot)));
		return T;
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		if (UT.Code.stringValid(mDungeonLootName)) aList.add(LH.Chat.BLINKING_CYAN + "Contains Loot of " + LH.Chat.WHITE + LH.get("loot." + mDungeonLootName));
		aList.add(LH.Chat.DGRAY + LH.get(LH.TOOL_TO_TAKE_PINCERS));
	}
	
	@Override public boolean receiveDataByte(byte aData, INetworkHandler aNetworkHandler) {
		// [GT6-LIDTRACE] BUG-059 — снять при уборке захода #39
		if (aData != mUsingPlayers) OUT.println("[GT6-LIDTRACE] " + (isClientSide() ? "CLIENT" : "SERVER") + " rcvByte @" + getBlockPos().toShortString() + " using " + mUsingPlayers + "->" + aData);
		mUsingPlayers = aData; return T;
	}

	@Override
	public boolean receiveDataByteArray(byte[] aData, INetworkHandler aNetworkHandler) {
		// [GT6-LIDTRACE] BUG-059 — снять при уборке захода #39
		if (aData[1] != mUsingPlayers) OUT.println("[GT6-LIDTRACE] " + (isClientSide() ? "CLIENT" : "SERVER") + " rcvArr @" + getBlockPos().toShortString() + " len=" + aData.length
			+ " using " + mUsingPlayers + "->" + aData[1] + " facing->" + (aData[0] & 7) + " invsize->" + UT.Code.unsignB(aData[2]));
		mFacing = (byte)(aData[0] & 7);
		mUsingPlayers = aData[1];
		if (UT.Code.unsignB(aData[2]) != getContainerSize()) setInventory(new ItemStack[UT.Code.unsignB(aData[2])]);
		mRGBa = UT.Code.getRGBInt(new short[] {UT.Code.unsignB(aData[3]), UT.Code.unsignB(aData[4]), UT.Code.unsignB(aData[5])});
		return T;
	}
	
	@Override public boolean unpaint() {if (mIsPainted) {mIsPainted=F; mRGBa=UT.Code.getRGBInt(mMaterial.fRGBaSolid); updateClientData(); return T;} return F;}
	@Override public boolean isPainted() {return mIsPainted || (level != null && isClientSide() && UT.Code.getRGBInt(mMaterial.fRGBaSolid) != mRGBa);}
	@Override public boolean paint(int aRGB) {if (aRGB!=mRGBa) {mRGBa=aRGB; mIsPainted=T; return T;} return F;}
	@Override public int getPaint() {return mRGBa;}
	@Override public boolean canRecolorItem(ItemStack aStack) {return T;}
	@Override public boolean canDecolorItem(ItemStack aStack) {return mIsPainted;}
	@Override public boolean recolorItem(ItemStack aStack, int aRGB) {if (paint((isPainted() ? UT.Code.mixRGBInt(aRGB, getPaint()) : aRGB) & ALL_NON_ALPHA_COLOR)) {UT.NBT.set(aStack, writeItemNBT(ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make())); return T;} return F;}
	@Override public boolean decolorItem(ItemStack aStack) {if (unpaint()) {UT.NBT.set(aStack, writeItemNBT(ItemNBT.has(aStack) ? ItemNBT.get(aStack) : UT.NBT.make())); return T;} return F;}
	
	@Override public int isProvidingWeakPower  (byte aOppositeSide) {return mIsTrapped && mUsingPlayers > 0 ? 15 : 0;}
	@Override public int isProvidingStrongPower(byte aOppositeSide) {return mIsTrapped && mUsingPlayers > 0 ? 15 : 0;}
	
	private static final float minX = 0.0625F, minY = 0F, minZ = 0.0625F, maxX = 0.9375F, maxY = 0.875F, maxZ = 0.9375F;
	@Override public AABB getCollisionBoundingBoxFromPool() {return box(minX, minY, minZ, maxX, maxY, maxZ);}
	@Override public AABB getSelectedBoundingBoxFromPool () {return box(minX, minY, minZ, maxX, maxY, maxZ);}
	@Override public void setBlockBoundsBasedOnState(Block aBlock) {box(aBlock, minX, minY, minZ, maxX, maxY, maxZ);}
	@Override public boolean setBlockBounds(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {box(aBlock, minX, minY, minZ, maxX, maxY, maxZ); return true;}
	@Override public float getSurfaceSize           (byte aSide) {return 0.875F;}
	@Override public float getSurfaceSizeAttachable (byte aSide) {return 0.875F;}
	@Override public float getSurfaceDistance       (byte aSide) {return aSide > 1 ? 0.0625F : aSide == 1 ? 0.125F : 0;}
	@Override public boolean isSurfaceSolid         (byte aSide) {return F;}
	@Override public boolean isSurfaceOpaque        (byte aSide) {return F;}
	
	@Override public Object getGUIClient(int aGUIID, Player aPlayer) {return new ContainerClientChest(aPlayer.getInventory(), this, aGUIID);}
	@Override public Object getGUIServer(int aGUIID, Player aPlayer) {return new ContainerCommonChest(aPlayer.getInventory(), this, aGUIID);}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code TileEntityRendererDispatcher.instance.renderTileEntityAt(...)}
	 *  (пакет {@code net.minecraft.client.renderer.tileentity} удалён целиком, замены нет — item-рендер
	 *  теперь {@code ItemStackRenderState}/{@code ItemModelResolver}, decisions/F3-render.md §2.5/§3
	 *  "IItemRenderer"); параметр ретипирован {@code Object} (см. {@link gregapi.render.IRenderedBlockObject}). */
	@Override
	public boolean renderItem(Block aBlock, Object aRenderer) {
		return T;
	}
	
	private static MultiTileEntityRendererChest RENDERER;

	@Override
	public void onRegistrationFirstClient(MultiTileEntityRegistry aRegistry, short aID) {
		// было ClientRegistry.bindTileEntitySpecialRenderer (FML-диспетчер по классу TE; API мёртв, зеркало вырезано из
		// рантайма) → тот же диспетч по классу в едином GT6-BER (MTE_TYPE один на все MTE, см. MultiTileEntityBER).
		gregapi.render.MultiTileEntityBER.bindSpecialRenderer(getClass(), RENDERER = new MultiTileEntityRendererChest());
	}
	
	@Override
	public void onRegistrationClient(MultiTileEntityRegistry aRegistry, short aID) {
		/* F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code new Identifier(namespace,path)} — конструктор
		 * стал {@code private} в 26.1.2, фабрика {@code Identifier.fromNamespaceAndPath(namespace,path)}
		 * (`neo-decompiled/net/minecraft/resources/Identifier.java:41`). */
		RENDERER.mResources.put(mTextureName, new Identifier[] {Identifier.fromNamespaceAndPath(MD.GT.mID, TEX_DIR_MODEL + aRegistry.mNameInternal + "/" + mTextureName + ".colored.png"), Identifier.fromNamespaceAndPath(MD.GT.mID, TEX_DIR_MODEL + aRegistry.mNameInternal + "/" + mTextureName + ".plain.png")});
	}

	/**
	 * F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code TileEntitySpecialRenderer} (immediate-mode: GL11
	 * push/pop-матрицы, {@code OpenGlHelper.glBlendFunc}, ручной {@code bindTexture}+{@code ModelBase}/
	 * {@code ModelRenderer} с крутящейся крышкой сундука) — весь стек удалён в 26.1.2 (decisions/F3-render.md
	 * §1). Замена — {@code BlockEntityRenderer<T,S>} нового API (`neo-decompiled/net/minecraft/client/
	 * renderer/blockentity/BlockEntityRenderer.java:15-24`: {@code createRenderState()}+{@code submit(state,
	 * PoseStack,SubmitNodeCollector,CameraRenderState)}, БЕЗ старого {@code render(...)}), эталон —
	 * {@code InscriberRenderer.java:55-276} (F3-render.md §2.5). Реальная перерисовка крышки —
	 * {@code CubeBuilder}/{@code submitCustomGeometry} по образцу эталона; тело {@code submit} ниже — no-op заглушка.
	 */
	/** Состояние кадра спец-рендера сундука (extract на main-thread, submit только читает). */
	public static class MTEChestRenderState extends BlockEntityRenderState {
		public float mLidAngleRad; public byte mChestFacing; public int mChestRGBa; public Identifier[] mChestTextures;
	}

	public static class MultiTileEntityRendererChest implements BlockEntityRenderer<MultiTileEntityChest, MTEChestRenderState> {
		private static final MultiTileEntityModelChest sModel = new MultiTileEntityModelChest();
		public final Map<String, Identifier[]> mResources = new HashMap<>();

		@Override
		public MTEChestRenderState createRenderState() {
			return new MTEChestRenderState();
		}

		@Override
		public void extractRenderState(MultiTileEntityChest aChest, MTEChestRenderState aState, float aPartialTick, net.minecraft.world.phys.Vec3 aCameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay aBreakProgress) {
			BlockEntityRenderer.super.extractRenderState(aChest, aState, aPartialTick, aCameraPos, aBreakProgress);
			// 1.7.10 renderTileEntityAt: интерполяция крышки + кубическая кривая — дословно.
			double tLidAngle = 1 - (aChest.oLidAngle + (aChest.mLidAngle - aChest.oLidAngle) * aPartialTick); tLidAngle = -(((1 - tLidAngle*tLidAngle*tLidAngle) * Math.PI) / 2);
			aState.mLidAngleRad = (float)tLidAngle;
			aState.mChestFacing = aChest.level==null ? ITEM_CHEST_FACING : aChest.mFacing; // BUG-038: item-форма (detached-TE) — калибруемый facing, чтобы сундук смотрел замком к камере
			aState.mChestRGBa = aChest.mRGBa;
			aState.mChestTextures = mResources.get(aChest.mTextureName);
		}

		@Override
		public void submit(MTEChestRenderState aState, PoseStack aPoseStack, SubmitNodeCollector aNodes, CameraRenderState aCamera) {
			Identifier[] tLocation = aState.mChestTextures;
			if (tLocation == null || tLocation.length < 2) return;
			// матрицы 1:1 с 1.7.10 (translate(0,1,1)+scale(1,-1,-1) — модель и текстуры в перевёрнутой системе 1.7.10)
			aPoseStack.pushPose();
			aPoseStack.translate(0, 1, 1);
			aPoseStack.scale(1, -1, -1);
			aPoseStack.translate(0.5f, 0.5f, 0.5f);
			aPoseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(COMPASS_FROM_SIDE[aState.mChestFacing] * 90 - 180));
			aPoseStack.translate(-0.5f, -0.5f, -0.5f);
			short[] tRGBa = UT.Code.getRGBaArray(aState.mChestRGBa);
			// пасс 1: .colored.png с тинтом mRGBa; пасс 2: .plain.png белым (blend+alpha 1.7.10 → entityCutout)
			sModel.submit(aNodes, aPoseStack, tLocation[0], aState.mLidAngleRad, aState.lightCoords, net.minecraft.util.ARGB.color(255, tRGBa[0], tRGBa[1], tRGBa[2]));
			sModel.submit(aNodes, aPoseStack, tLocation[1], aState.mLidAngleRad, aState.lightCoords, -1);
			aPoseStack.popPose();
		}
	}

	/** Модель сундука 1:1 (боксы/rotationPoints/texOffs дословно из 1.7.10 ModelBase-версии; ModelPart — neo-носитель ModelRenderer). */
	public static class MultiTileEntityModelChest {
		private final net.minecraft.client.model.geom.ModelPart mRoot, mLid, mKnob;

		public MultiTileEntityModelChest() {
			net.minecraft.client.model.geom.builders.MeshDefinition tMesh = new net.minecraft.client.model.geom.builders.MeshDefinition();
			net.minecraft.client.model.geom.builders.PartDefinition tRoot = tMesh.getRoot();
			tRoot.addOrReplaceChild("lid",    net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox( 0, -5, -14, 14,  5, 14), net.minecraft.client.model.geom.PartPose.offset(1, 7, 15));
			tRoot.addOrReplaceChild("knob",   net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,  0).addBox(-1, -2, -15,  2,  4,  1), net.minecraft.client.model.geom.PartPose.offset(8, 7, 15));
			tRoot.addOrReplaceChild("bottom", net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0, 19).addBox( 0,  0,   0, 14, 10, 14), net.minecraft.client.model.geom.PartPose.offset(1, 6, 1));
			mRoot = net.minecraft.client.model.geom.builders.LayerDefinition.create(tMesh, 64, 64).bakeRoot();
			mLid  = mRoot.getChild("lid");
			mKnob = mRoot.getChild("knob");
		}

		public void submit(SubmitNodeCollector aNodes, PoseStack aPoseStack, Identifier aTexture, float aLidAngle, int aLight, int aColor) {
			mKnob.xRot = mLid.xRot = aLidAngle;
			aNodes.submitCustomGeometry(aPoseStack, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(aTexture), (tPose, tBuffer) -> {
				PoseStack tStack = new PoseStack();
				tStack.mulPose(tPose.pose());
				mRoot.render(tStack, tBuffer, aLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, aColor);
			});
		}
	}
}
