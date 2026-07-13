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

package gregapi.cover.covers;

import gregapi.block.fluid.BlockBaseFluid;
import gregapi.block.metatype.BlockMetaType;
import gregapi.cover.CoverData;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.render.BlockTextureDefault;
import gregapi.render.ITexture;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import openblocks.common.LiquidXpUtils;
import openmods.utils.EnchantmentUtils;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class CoverDrain extends AbstractCoverAttachment {
	@Override public boolean interceptCoverPlacement(byte aCoverSide, CoverData aData, Entity aPlayer) {return !(aData.mTileEntity.canTick() && aData.mTileEntity instanceof IFluidHandler);}
	
	@Override
	@SuppressWarnings("unchecked")
	public void onTickPre(byte aCoverSide, CoverData aData, long aTimer, boolean aIsServerSide, boolean aReceivedBlockUpdate, boolean aReceivedInventoryUpdate) {
		if (aIsServerSide && !aData.mStopped && aData.mTileEntity instanceof IFluidHandler) {
			if (SERVER_TIME % 100 == 10 && SIDES_TOP_HORIZONTAL[aCoverSide] && aData.mTileEntity.getWorld().isRaining()) {
				Biome tBiome = aData.mTileEntity.getBiome();
				if (tBiome.rainfall > 0 && tBiome.temperature >= 0.2) {
					Block tInFront = aData.mTileEntity.getBlockAtSide(aCoverSide);
					if (!(tInFront instanceof LiquidBlock) && !(tInFront instanceof IFluidBlock) && !WD.sideSolid(tInFront, aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide), FORGE_DIR_OPPOSITES[aCoverSide]) && !WD.sideSolid(tInFront, aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide), FORGE_DIR[SIDE_TOP])) {
						boolean temp = F;
						if (tInFront instanceof BlockMetaType || tInFront instanceof SlabBlock || tInFront instanceof StairBlock) {
							temp = aData.mTileEntity.getRainOffset(OFFX[aCoverSide], OFFY[aCoverSide]+1, OFFZ[aCoverSide]);
						} else {
							temp = aData.mTileEntity.getRainOffset(OFFX[aCoverSide], OFFY[aCoverSide]  , OFFZ[aCoverSide]) && (SIDES_TOP[aCoverSide] || WD.sideSolid(aData.mTileEntity.getBlockOffset(OFFX[aCoverSide], -1, OFFZ[aCoverSide]), aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getY()-1, aData.mTileEntity.getOffsetZ(aCoverSide), FORGE_DIR[SIDE_TOP]));
						}
						if (temp) FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Water.make((long)Math.max(1, tBiome.rainfall*10000) * (aData.mTileEntity.getWorld().isThundering()?2:1)), T);
					}
				}
			}
			if (SERVER_TIME % 100 == 50 && (FL.XP.exists() || FL.Mob.exists())) {
				// Yes, I know that the AABB Check is a bit weird looking, but I think I will do more than just XP Orbs with this later on.
				for (Entity tEntity : (Iterable<Entity>)aData.mTileEntity.getWorld().getEntitiesOfClass(ExperienceOrb.class, new AABB(aData.mTileEntity.getOffsetX(aCoverSide, 2)-1, aData.mTileEntity.getOffsetY(aCoverSide, 2)-1, aData.mTileEntity.getOffsetZ(aCoverSide, 2)-1, aData.mTileEntity.getOffsetX(aCoverSide, 2)+2, aData.mTileEntity.getOffsetY(aCoverSide, 2)+2, aData.mTileEntity.getOffsetZ(aCoverSide, 2)+2))) if (!tEntity.isRemoved()) {
					if (tEntity instanceof ExperienceOrb) {
						if (MD.OB.mLoaded) {
							try {
								if (FL.fillAll((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.XP.make(LiquidXpUtils.xpToLiquidRatio(((ExperienceOrb)tEntity).getXpValue())), T)) {
									UT.Sounds.send(SFX.MC_XP, 0.1F, (RNGSUS.nextFloat()-RNGSUS.nextFloat()) * 0.35F + 0.9F, (BlockEntity)aData.mTileEntity);
									aData.mTileEntity.getWorld().removeEntity(tEntity);
									tEntity.discard();
									continue;
								}
							} catch(Throwable e) {e.printStackTrace(ERR);}
						}
						if (FL.fillAll((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.XP.make(((ExperienceOrb)tEntity).getXpValue() * 20, FL.Mob, UT.Code.units(((ExperienceOrb)tEntity).getXpValue(), 3, 200, F)), T)) {
							UT.Sounds.send(SFX.MC_XP, 0.1F, (RNGSUS.nextFloat()-RNGSUS.nextFloat()) * 0.35F + 0.9F, (BlockEntity)aData.mTileEntity);
							aData.mTileEntity.getWorld().removeEntity(tEntity);
							tEntity.discard();
							continue;
						}
						continue;
					}
				}
			}
			if (aReceivedBlockUpdate || SERVER_TIME % 20 == 5) {
				Block tBlock = aData.mTileEntity.getBlockAtSide(aCoverSide);
				FluidStack tFluid = NF;
				
				if (tBlock instanceof BlockBaseFluid) {
					byte tMeta = aData.mTileEntity.getMetaDataAtSide(aCoverSide);
					BlockBaseFluid tFluidBlock = ((BlockBaseFluid)tBlock);
					if (SIDES_HORIZONTAL[aCoverSide] || (tFluidBlock.mDensityDir>0?SIDES_BOTTOM:SIDES_TOP)[aCoverSide]) {
						byte i = 0; while (i <= tMeta) if (FL.fillAll((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], tFluidBlock.mQuanta.copy(), T)) i++; else break;
						if (i > 0) {
							WD.set(aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide), i > tMeta ? NB : tBlock, i > tMeta ? 0 : tMeta-i, 3);
							tFluidBlock.updateFluidBlocks(aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide), T);
						}
					}
				} else {
					if (tBlock == Blocks.WATER || tBlock == Blocks.WATER) {
						if (aData.mTileEntity.getMetaDataAtSide(aCoverSide) == 0) {
							if (WD.infiniteWater(aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide))) {
								FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Water.make(16000), T);
							} else {
								tFluid = FL.Water.make(1000);
							}
						}
					} else
					if (tBlock == Blocks.LAVA || tBlock == Blocks.LAVA) {
						if (aData.mTileEntity.getMetaDataAtSide(aCoverSide) == 0) tFluid = FL.Lava.make(1000);
					} else
					if (tBlock == BlocksGT.River || WD.waterstream(tBlock)) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Water.make(16000), T);
					} else
					if (tBlock == BlocksGT.Ocean) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Ocean.make(16000), T);
					} else
					if (tBlock == BlocksGT.Swamp) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Dirty_Water.make(16000), T);
					} else
					if (tBlock instanceof IFluidBlock) tFluid = ((IFluidBlock)tBlock).drain(aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide), F);
					
					if (tFluid != null && (SIDES_HORIZONTAL[aCoverSide] || FL.gas(tFluid) || (FL.lighter(tFluid)?SIDES_BOTTOM:SIDES_TOP)[aCoverSide])) {
						if (FL.fillAll((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], tFluid, T)) {
							if (tBlock instanceof IFluidBlock) {
								((IFluidBlock)tBlock).drain(aData.mTileEntity.getWorld(), aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide), T);
							} else {
								aData.mTileEntity.getWorld().setBlockToAir(aData.mTileEntity.getOffsetX(aCoverSide), aData.mTileEntity.getOffsetY(aCoverSide), aData.mTileEntity.getOffsetZ(aCoverSide));
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean onWalkOver(byte aCoverSide, CoverData aData, Entity aEntity) {
		if (SIDES_TOP[aCoverSide] && !aData.mStopped && aData.mTileEntity instanceof IFluidHandler && aData.mTileEntity.isServerSide()) {
			if (aEntity instanceof Player) {
				if (MD.OB.mLoaded && SERVER_TIME % 5 == 0 && ((Player)aEntity).isShiftKeyDown() && FL.XP.exists()) try {
					FluidStack tFluid = FL.XP.make(Math.min(1000, LiquidXpUtils.xpToLiquidRatio(EnchantmentUtils.getPlayerXP(((Player)aEntity)))));
					if (tFluid.getAmount() > 0) {
						int tDrainedXP = LiquidXpUtils.liquidToXpRatio((int)FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], tFluid, F));
						tFluid.setAmount(LiquidXpUtils.xpToLiquidRatio(tDrainedXP));
						if (tFluid.getAmount() > 0 && FL.fillAll((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], tFluid, T)) {
							EnchantmentUtils.addPlayerXP(((Player)aEntity), -tDrainedXP);
							UT.Sounds.send(SFX.MC_XP, 0.1F, (RNGSUS.nextFloat()-RNGSUS.nextFloat()) * 0.35F + 0.9F, aEntity);
						}
					}
				} catch(Throwable e) {e.printStackTrace(ERR);}
				return T;
			}
			if (SERVER_TIME % 20 == 5) {
				if (aEntity instanceof AbstractGolem) {
					return F;
				}
				if (aEntity.getClass() == Squid.class) {
					FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.InkSquid.make(1), T);
					return T;
				}
				if (aEntity instanceof Slime) {
					if (aEntity.getClass() == Slime.class) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Slime_Green.make(Math.max(1, ((Slime)aEntity).getSlimeSize())), T);
						return T;
					}
					if (aEntity.getClass() == MagmaCube.class) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Blaze.make(Math.max(1, ((Slime)aEntity).getSlimeSize())), T);
						return T;
					}
					String tClass = UT.Reflection.getLowercaseClass(aEntity);
					if (tClass.equalsIgnoreCase("EntityTFMazeSlime")) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Slime_Green.make(Math.max(1, ((Slime)aEntity).getSlimeSize())), T);
						return T;
					}
					if (tClass.equalsIgnoreCase("KingBlueSlime")) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Slime_Blue.make(4), T);
						return T;
					}
					if (tClass.equalsIgnoreCase("BlueSlime")) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Slime_Blue.make(1), T);
						return T;
					}
					if (tClass.equalsIgnoreCase("EntityPinkSlime")) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Slime_Pink.make(1), T);
						return T;
					}
					return F;
				}
				if (aEntity instanceof Animal && FL.Sewage.exists()) {
					if (!(aEntity instanceof EntityAgeable) || !((EntityAgeable)aEntity).isChild()) {
						FL.fill_((IFluidHandler)aData.mTileEntity, ALL_SIDES_THIS_AND_ANY[aCoverSide], FL.Sewage.make(Math.max(1, (long)(20 * aEntity.getBbWidth() * aEntity.getBbWidth() * aEntity.getBbHeight()))), T);
						return T;
					}
				}
			}
			return T;
		}
		return F;
	}
	
	@Override
	public void addToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		super.addToolTips(aList, aStack, aF3_H);
		aList.add(LH.Chat.CYAN + "Collects Fluid Blocks (if not against Gravity)");
		aList.add(LH.Chat.CYAN + "Collects Rainwater (not in Dry or Cold Areas)");
		aList.add(LH.Chat.CYAN + "Will work infinitely in River and Lake Biomes");
		if (FL.Sewage.exists())
		aList.add(LH.Chat.ORANGE + "Will collect Sewage from adult Animals walking on it (Bigger Animals make more)");
		if (FL.XP.exists())
		aList.add(LH.Chat.GREEN + "Will collect XP Orbs to make Liquid XP");
		else if (FL.Mob.exists())
		aList.add(LH.Chat.DGREEN + "Will collect XP Orbs to make Mob Essence");
		if (MD.OB.mLoaded)
		aList.add(LH.Chat.GREEN + "Stand on this and Sneak to drain your XP");
		aList.add(LH.Chat.DGRAY + LH.get(LH.TOOL_TO_TOGGLE_CONTROLLER_COVER));
	}
	
	@Override public boolean isOpaque(byte aSide, CoverData aData) {return T;}
	@Override public boolean showsConnectorFront(byte aCoverSide, CoverData aData) {return F;}
	
	@Override public ITexture getCoverTextureSurface(byte aSide, CoverData aData) {return sTextureFront;}
	@Override public ITexture getCoverTextureAttachment(byte aSide, CoverData aData, byte aTextureSide) {return aSide == aTextureSide ? sTextureFront : aSide == OPOS[aTextureSide] ? sTextureBack : sTextureSides;}
	@Override public ITexture getCoverTextureHolder(byte aSide, CoverData aData, byte aTextureSide) {return sTextureSides;}
	
	public static final ITexture
	sTextureSides = BlockTextureDefault.get("machines/covers/drain/sides"),
	sTextureFront = BlockTextureDefault.get("machines/covers/drain/front"),
	sTextureBack = BlockTextureDefault.get("machines/covers/drain/back");
}
