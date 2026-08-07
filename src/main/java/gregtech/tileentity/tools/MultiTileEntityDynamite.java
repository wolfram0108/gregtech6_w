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

package gregtech.tileentity.tools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import gregapi.block.metatype.BlockStones;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetCollisionBoundingBoxFromPool;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_GetSelectedBoundingBoxFromPool;
import gregapi.block.multitileentity.IMultiTileEntity.IMTE_SetBlockBoundsBasedOnState;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.CS.*;
import gregapi.data.LH;
import gregapi.old.Textures;
import gregapi.render.BlockTextureDefault;
import gregapi.render.BlockTextureMulti;
import gregapi.render.IIconContainer;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntityRemoteActivateable;
import gregapi.tileentity.base.TileEntityBase09FacingSingle;
import gregapi.util.UT;
import gregapi.util.WD;
import gregapi.worldgen.StoneLayer;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class MultiTileEntityDynamite extends TileEntityBase09FacingSingle implements ITileEntityRemoteActivateable, IMTE_SetBlockBoundsBasedOnState, IMTE_GetCollisionBoundingBoxFromPool, IMTE_GetSelectedBoundingBoxFromPool {
	public byte mCountDown = 0, mFortune = 0;
	public boolean mSunk = F;
	public long mMaxExplosionResistance = 10;
	
	@Override
	public void readFromNBT2(CompoundTag aNBT) {
		super.readFromNBT2(aNBT);
		if (aNBT.contains(NBT_STATE)) mCountDown = aNBT.getByteOr(NBT_STATE, (byte)0);
		if (aNBT.contains(NBT_MODE)) mSunk = aNBT.getBooleanOr(NBT_MODE, false);
		if (aNBT.contains(NBT_QUALITY)) mMaxExplosionResistance = aNBT.getLongOr(NBT_QUALITY, 0L);
		if (aNBT.contains(NBT_FORTUNE)) mFortune = aNBT.getByteOr(NBT_FORTUNE, (byte)0);
	}
	
	@Override
	public void writeToNBT2(CompoundTag aNBT) {
		super.writeToNBT2(aNBT);
		aNBT.putByte(NBT_STATE, mCountDown);
		UT.NBT.setBoolean(aNBT, NBT_MODE, mSunk);
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		aList.add(LH.Chat.CYAN     + LH.get(LH.TOOLTIP_BLASTPOWER) + LH.Chat.WHITE + mMaxExplosionResistance);
		aList.add(LH.Chat.CYAN     + LH.get(LH.TOOLTIP_BLASTRANGE) + LH.Chat.WHITE + "3x3x3");
		if (mFortune > 0)
		aList.add(LH.Chat.PINK     + LH.get(LH.TOOLTIP_BLASTFORTUNE) + LH.Chat.WHITE + mFortune);
		aList.add(LH.Chat.ORANGE   + LH.get(LH.REQUIREMENT_IGNITE_FIRE));
		aList.add(LH.Chat.RED      + LH.get(LH.TOOLTIP_EXPLOSIVE));
		super.addToolTips(aList, aStack, aF3_H);
	}
	
	@Override
	public long onToolClick2(String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, byte aSide, float aHitX, float aHitY, float aHitZ) {
		long rReturn = super.onToolClick2(aTool, aRemainingDurability, aQuality, aPlayer, aChatReturn, aPlayerInventory, aSneaking, aStack, aSide, aHitX, aHitY, aHitZ);
		if (rReturn > 0) return rReturn;
		
		if (isClientSide()) return 0;
		
		if (aTool.equals(TOOL_igniter       ) && mCountDown == 0 && WD.oxygen(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()) ) {mCountDown = 100; updateClientData(); causeBlockUpdate(); UT.Sounds.send(SFX.MC_TNT_IGNITE , 1.0F, 0.5F, this, F); return 10000;}
		if (aTool.equals(TOOL_extinguisher  ) && mCountDown != 0                                                ) {mCountDown =   0; updateClientData(); causeBlockUpdate(); UT.Sounds.send(SFX.MC_FIZZ       , 1.0F, 0.5F, this, F); return 10000;}
		return 0;
	}
	
	@Override
	public void onTick2(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide) {
			if (mBlockUpdated || aTimer == 2) {
				if ((mCountDown == 0 && hasRedstoneIncoming()) || WD.burning(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())) remoteActivate();
				
				while(mSunk) {
					Block tBlock = getBlockAtSide(OPOS[mFacing]);
					if (BlocksGT.drillableDynamite.contains(tBlock)) break;
					if (WD.hardness(tBlock, level, getBlockPos().getX()+OFFX[OPOS[mFacing]], getBlockPos().getY()+OFFY[OPOS[mFacing]], getBlockPos().getZ()+OFFZ[OPOS[mFacing]]) >= 0) {
						if (tBlock instanceof BlockStones) {
							if (getMetaDataAtSide(OPOS[mFacing]) < 3) break;
						} else {
							if (StoneLayer.REPLACEABLE_BLOCKS.contains(tBlock)) break;
							if (WD.ore_stone(tBlock, getMetaDataAtSide(OPOS[mFacing]))) break;
						}
					}
					mSunk = F;
					updateClientData();
					causeBlockUpdate();
					break;
				}
			}
			if (mCountDown > 0 && --mCountDown <= 0) explode(F);
		}
	}
	
	@Override
	public void setVisualData(byte aData) {
		mCountDown = (byte)(aData >>> 1);
		mSunk = ((aData & 1) != 0);
	}
	
	@Override public byte getVisualData() {return (byte)((mCountDown<<1)|(mSunk?1:0));}
	
	private boolean mDontDrop = F;
	
	@Override
	public void explode(boolean aInstant) {
		mDontDrop = T;
		WD.set(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), NB, 0, 3);
		DynamiteExplosion tExplosion = mSunk ? new DynamiteExplosion(level, getOffsetXN(mFacing)+0.5, getOffsetYN(mFacing)+0.5, getOffsetZN(mFacing)+0.5, mMaxExplosionResistance, mFortune) : new DynamiteExplosion(level, getBlockPos().getX()+0.5, getBlockPos().getY()+0.5, getBlockPos().getZ()+0.5, mMaxExplosionResistance, mFortune);
		tExplosion.doExplosionA();
		tExplosion.doExplosionB(T);
	}
	
	@Override
	public ArrayListNoNulls<ItemStack> getDrops(int aFortune, boolean aSilkTouch) {
		return mDontDrop ? new ArrayListNoNulls<ItemStack>() : super.getDrops(aFortune, aSilkTouch);
	}
	
	@Override
	public boolean setBlockBounds2(Block aBlock, int aRenderPass, boolean[] aShouldSideBeRendered) {
		box(aBlock, PX_P[SIDE_X_NEG==mFacing?mSunk?14:0:SIDE_X_POS==mFacing?0:5], PX_P[SIDE_Y_NEG==mFacing?mSunk?14:0:SIDE_Y_POS==mFacing?0:5], PX_P[SIDE_Z_NEG==mFacing?mSunk?14:0:SIDE_Z_POS==mFacing?0:5], PX_N[SIDE_X_POS==mFacing?mSunk?14:0:SIDE_X_NEG==mFacing?0:5], PX_N[SIDE_Y_POS==mFacing?mSunk?14:0:SIDE_Y_NEG==mFacing?0:5], PX_N[SIDE_Z_POS==mFacing?mSunk?14:0:SIDE_Z_NEG==mFacing?0:5]);
		return T;
	}
	
	public static final IIconContainer
	sTextureFront       = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/colored/front"),
	sTextureBack        = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/colored/back"),
	sTextureSide        = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/colored/side"),
	sOverlayFront       = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/overlay/front"),
	sOverlayBack        = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/overlay/back"),
	sOverlaySide        = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/overlay/side"),
	sTextureFrontActive = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/colored_active/front"),
	sTextureBackActive  = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/colored_active/back"),
	sTextureSideActive  = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/colored_active/side"),
	sOverlayFrontActive = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/overlay_active/front"),
	sOverlayBackActive  = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/overlay_active/back"),
	sOverlaySideActive  = new Textures.BlockIcons.CustomIcon("machines/tools/dynamite/overlay_active/side");
	
	@Override
	public ITexture getTexture2(Block aBlock, int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered) {
		if (aSide ==      mFacing ) return BlockTextureMulti.get(BlockTextureDefault.get(mCountDown != 0 ? sTextureFrontActive : sTextureFront, mRGBa, F, F, F, F), BlockTextureDefault.get(mCountDown != 0 ? sOverlayFrontActive : sOverlayFront));
		if (aSide == OPOS[mFacing]) return BlockTextureMulti.get(BlockTextureDefault.get(mCountDown != 0 ? sTextureBackActive  : sTextureBack , mRGBa, F, F, F, F), BlockTextureDefault.get(mCountDown != 0 ? sOverlayBackActive : sOverlayBack));
		return BlockTextureMulti.get(BlockTextureDefault.get(mCountDown != 0 ? sTextureSideActive : sTextureSide, mRGBa, F, F, F, F), BlockTextureDefault.get(mCountDown != 0 ? sOverlaySideActive : sOverlaySide));
	}
	
	@Override public int getLightOpacity() {return LIGHT_OPACITY_NONE;}
	
	@Override public AABB getCollisionBoundingBoxFromPool() {return box(PX_P[SIDE_X_NEG==mFacing?mSunk?14:0:SIDE_X_POS==mFacing?0:5], PX_P[SIDE_Y_NEG==mFacing?mSunk?14:0:SIDE_Y_POS==mFacing?0:5], PX_P[SIDE_Z_NEG==mFacing?mSunk?14:0:SIDE_Z_POS==mFacing?0:5], PX_N[SIDE_X_POS==mFacing?mSunk?14:0:SIDE_X_NEG==mFacing?0:5], PX_N[SIDE_Y_POS==mFacing?mSunk?14:0:SIDE_Y_NEG==mFacing?0:5], PX_N[SIDE_Z_POS==mFacing?mSunk?14:0:SIDE_Z_NEG==mFacing?0:5]);}
	@Override public AABB getSelectedBoundingBoxFromPool () {return box(PX_P[SIDE_X_NEG==mFacing?mSunk?14:0:SIDE_X_POS==mFacing?0:5], PX_P[SIDE_Y_NEG==mFacing?mSunk?14:0:SIDE_Y_POS==mFacing?0:5], PX_P[SIDE_Z_NEG==mFacing?mSunk?14:0:SIDE_Z_POS==mFacing?0:5], PX_N[SIDE_X_POS==mFacing?mSunk?14:0:SIDE_X_NEG==mFacing?0:5], PX_N[SIDE_Y_POS==mFacing?mSunk?14:0:SIDE_Y_NEG==mFacing?0:5], PX_N[SIDE_Z_POS==mFacing?mSunk?14:0:SIDE_Z_NEG==mFacing?0:5]);}
	@Override public void setBlockBoundsBasedOnState(Block aBlock)  {box(aBlock, PX_P[SIDE_X_NEG==mFacing?mSunk?14:0:SIDE_X_POS==mFacing?0:5], PX_P[SIDE_Y_NEG==mFacing?mSunk?14:0:SIDE_Y_POS==mFacing?0:5], PX_P[SIDE_Z_NEG==mFacing?mSunk?14:0:SIDE_Z_POS==mFacing?0:5], PX_N[SIDE_X_POS==mFacing?mSunk?14:0:SIDE_X_NEG==mFacing?0:5], PX_N[SIDE_Y_POS==mFacing?mSunk?14:0:SIDE_Y_NEG==mFacing?0:5], PX_N[SIDE_Z_POS==mFacing?mSunk?14:0:SIDE_Z_NEG==mFacing?0:5]);}
	
	@Override public byte isProvidingWeakPower2(byte aSide) {return (byte)(mCountDown != 0 ? 15 : 0);}
	@Override public byte isProvidingStrongPower2(byte aSide) {return (byte)(mCountDown != 0 ? 15 : 0);}
	
	@Override public void onExploded(Explosion aExplosion) {mDontDrop = T; super.onExploded(aExplosion); explode(T);}
	@Override public boolean remoteActivate() {if (mCountDown > 20 || mCountDown == 0) {mCountDown = 20; updateClientData(); causeBlockUpdate();} return F;}
	
	@Override public float getSurfaceSize           (byte aSide) {return 0.0F;}
	@Override public float getSurfaceSizeAttachable (byte aSide) {return 0.0F;}
	@Override public float getSurfaceDistance       (byte aSide) {return 0.0F;}
	@Override public boolean isSurfaceSolid         (byte aSide) {return F;}
	@Override public boolean isSurfaceOpaque2       (byte aSide) {return F;}
	@Override public boolean isSideSolid2           (byte aSide) {return F;}
	@Override public boolean allowCovers            (byte aSide) {return F;}
	@Override public boolean useSidePlacementRotation       () {return T;}
	@Override public boolean useInversePlacementRotation    () {return F;}
	
	@Override public boolean canDrop(int aInventorySlot) {return F;}
	
	@Override public String getTileEntityName() {return "gt.multitileentity.dynamite";}
	
	// F-explosion: extends GT6-ЦЕНТР ExplosionGT (переиспользует воспроизведённые 1.7.10-поля explosionX/Y/Z/exploder/isSmoking/affectedBlockPositions + neo-паттерны), НЕ neo Explosion-интерфейс (принцип 5).
	public static class DynamiteExplosion extends gregapi.random.ExplosionGT {
		public float mMaxExplosionResistance;
		public byte mFortune;

		public DynamiteExplosion(Level aWorld, double aX, double aY, double aZ, float aMaxExplosionResistance, byte aFortune) {
			super(aWorld, null, aX, aY, aZ, 1);
			mMaxExplosionResistance = aMaxExplosionResistance;
			mFortune = aFortune;
		}
		
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public void doExplosionA() {
			for (int tX = UT.Code.roundDown(explosionX) - 1; tX <= UT.Code.roundDown(explosionX) + 1; tX++) for (int tY = UT.Code.roundDown(explosionY) - 1; tY <= UT.Code.roundDown(explosionY) + 1; tY++) for (int tZ = UT.Code.roundDown(explosionZ) - 1; tZ <= UT.Code.roundDown(explosionZ) + 1; tZ++) {
				Block tBlock = WD.block(mWorld, tX, tY, tZ);
				if (tBlock == Blocks.SPAWNER || WD.bedrock(tBlock)) continue;
				BlockPos tPos = new BlockPos(tX, tY, tZ);
				if (tBlock.getExplosionResistance(mWorld.getBlockState(tPos), mWorld, tPos, this) <= mMaxExplosionResistance) affectedBlockPositions.add(tPos);
			}
			List tList = mWorld.getEntities(exploder, new AABB(explosionX - 2, explosionY - 2, explosionZ - 2, explosionX + 2, explosionY + 2, explosionZ + 2));
			net.neoforged.neoforge.event.EventHooks.onExplosionDetonate(mWorld, this, tList, affectedBlockPositions);
			DamageSource tSource = mWorld.damageSources().explosion(this);
			for (Object tEntity : tList) if (!(tEntity instanceof ItemEntity)) ((Entity)tEntity).hurt(tSource, 2*mMaxExplosionResistance*TFC_DAMAGE_MULTIPLIER);
			// explosionSize уже 1F (ExplosionGT-конструктор power=1, поле final).
		}
		
		@Override
		@SuppressWarnings("rawtypes")
		public void doExplosionB(boolean aEffects) {
			// F-explosion (функционально): визуал/звук взрыва идёт клиенту через explosion-packet (neo рисует/звучит сам); 1.7.10 строковый playSoundEffect/spawnParticle редундантен. Не заглушка.
			if (isSmoking) {
				Iterator iterator = affectedBlockPositions.iterator();
				while (iterator.hasNext()) {
					BlockPos tCoords = (BlockPos)iterator.next();
					int i = tCoords.getX(), j = tCoords.getY(), k = tCoords.getZ();
					Block tBlock = WD.block(mWorld, i, j, k);
					if (WD.getMaterial(tBlock) != Material.air) {
						BlockPos tPos = new BlockPos(i, j, k);
						BlockState tState = mWorld.getBlockState(tPos);
						// F-explosion (АДАПТИРОВАНО): дроп блоков реализован через neo Block.dropResources (loot-table, строка ниже). Caveat: loot-модель распределения ≠ 1.7.10 (движок-форс). Функционально, не заглушка.
						if (tBlock.canDropFromExplosion(tState, mWorld, tPos, this)) Block.dropResources(tState, mWorld, tPos);
						if (mWorld instanceof ServerLevel tServerLevel) tBlock.onBlockExploded(tState, tServerLevel, tPos, this);
					}
				}
			}
		}
	}
}
