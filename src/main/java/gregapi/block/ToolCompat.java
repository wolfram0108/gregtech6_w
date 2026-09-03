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

package gregapi.block;
// F4-flattening: since 1.13 facing lives on CarvedPumpkinBlock, which JACK_O_LANTERN also is;
// the same-named PumpkinBlock is the uncarved one and carries no facing property at all.
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ChestBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DispenserBlock;

import net.minecraftforge.fml.Logging;
import forestry.apiculture.tiles.TileCandle;
import gregapi.data.*;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.lang.LanguageHandler;
import gregapi.oredict.OreDictItemData;
import gregapi.recipes.Recipe;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import ic2.api.crops.ICropTile;
import ic2.api.tile.IWrenchable;
import micdoodle8.mods.galacticraft.core.blocks.BlockAdvanced;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * For Internal Use.
 */
public class ToolCompat {
	public static boolean GC_BLOCKADVANCED = F, IC_WRENCHABLE = F, IC_CROPTILE = F, AE_BASEBLOCKENTITY = F;
	
	public static void checkAvailabilities() {
		try {
			BlockAdvanced.class.getCanonicalName();
			GC_BLOCKADVANCED = T;
		} catch(Throwable e) {/**/}
		try {
			IWrenchable.class.getCanonicalName();
			IC_WRENCHABLE = T;
		} catch(Throwable e) {/**/}
		try {
			ICropTile.class.getCanonicalName();
			IC_CROPTILE = T;
		} catch(Throwable e) {/**/}
		try {
			// AE2 blocks are served by the GregTech wrench (see the arm in onToolClick). AE2 is a compileOnly
			// dependency, so its presence is detected by the class, as with the three flags above.
			appeng.blockentity.AEBaseBlockEntity.class.getCanonicalName();
			AE_BASEBLOCKENTITY = T;
		} catch(Throwable e) {/**/}
	}
	
	/** F4-flattening (BUG-042): ванильное бревно в neo = отдельный блок породы + AXIS-blockstate (в 1.7.10 — один
	 *  Blocks.log, мета = порода(биты 0-1)|ориентация(биты 2-3)). WD.meta ванильного бревна = 0 (не IBlockExtendedMetaData)
	 *  → ориентацию (PILLAR-биты балки) читаем прямо из RotatedPillarBlock.AXIS: AXIS.Y→PILLAR_Y(0), X→PILLAR_X(4),
	 *  Z→PILLAR_Z(8) (сверено CS.PILLAR_TO_AXIS:804-809). Восстанавливает 1.7.10-контракт «Beam, порода|ориентация»
	 *  для лежачих брёвен (без этого лежачее бревно ошкуривалось в вертикальную балку). */
	private static int pillarFromAxis(Level aWorld, int aX, int aY, int aZ) {
		BlockState tState = aWorld.getBlockState(new BlockPos(aX, aY, aZ));
		if (!tState.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) return PILLAR_Y;
		net.minecraft.core.Direction.Axis tAxis = tState.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
		return tAxis == net.minecraft.core.Direction.Axis.X ? PILLAR_X : tAxis == net.minecraft.core.Direction.Axis.Z ? PILLAR_Z : PILLAR_Y;
	}

	/** Providing compatibility for vanilla Blocks and certain Mod Interfaces. */
	public static long onToolClick(Block aBlock, String aTool, long aRemainingDurability, long aQuality, Entity aPlayer, List<String> aChatReturn, Container aPlayerInventory, boolean aSneaking, ItemStack aStack, Level aWorld, byte aSide, int aX, int aY, int aZ, float aHitX, float aHitY, float aHitZ) {
		byte aMeta = WD.meta(aWorld, aX, aY, aZ);
		BlockEntity aTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		Player aEntityPlayer = aPlayer instanceof Player ? (Player)aPlayer : null;
		LivingEntity aEntityLiving = aPlayer instanceof LivingEntity ? (LivingEntity)aPlayer : null;
		
		try {
		
		if (aTool.equals(TOOL_hoe) && (aEntityPlayer == null || (aEntityPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack))) {
			// The 1.7.10 hoe event existed only so other mods could veto tilling; BlockToolModificationEvent needs
			// a full UseOnContext unavailable here (the player may be null), so only that veto hook is missing.
			{
				if (SIDES_TOP_HORIZONTAL[aSide] && !WD.hasCollide(aWorld, aX, aY+1, aZ) && (aBlock == Blocks.GRASS_BLOCK || gregapi.data.CS.Flattened.headOf(aBlock) == Blocks.DIRT || aBlock == BlocksGT.Grass || IL.EtFu_Path.equal(aBlock) || IL.BoP_Grass_Origin.equal(aBlock) || IL.BoP_Grass_Long.equal(aBlock))) {
					WD.playStepSound(aWorld, aX + 0.5F, aY + 0.5F, aZ + 0.5F, Blocks.FARMLAND);
					if (!aWorld.isClientSide()) WD.set(aWorld, aX, aY, aZ, Blocks.FARMLAND, 0, 3);
					return 10000;
				}
			}
		}
		
		if (aWorld.isClientSide()) return 0;
		
		boolean aCanCollect = (ST.item(aStack) instanceof MultiItemTool && ((MultiItemTool)ST.item_(aStack)).canCollectDropsDirectly(aStack));
		
		if (aTool.equals(TOOL_axe) || aTool.equals(TOOL_saw) || aTool.equals(TOOL_knife)) {
			boolean rReturn = F;
			ItemStack tBark = OM.dust(MT.Bark);
			
			if (!rReturn && BlocksGT.BeamA != null) {
				if (IL.HaC_Log_Maple.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.BeamA, 1, 3);
				}
			}
			if (!rReturn && BlocksGT.BeamB != null) {
				if (IL.HaC_Log_Cinnamon.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.BeamB, 1, 3);
					if (rReturn) tBark = IL.HaC_Cinnamon.get(2, IL.Food_Cinnamon.get(2, OM.dust(MT.Cinnamon, U*2)));
				}
			}
			if (!rReturn && MD.NeLi.mLoaded && (aMeta & 1) == 0) {
				if (IL.NeLi_Stem_Crimson.equal(aBlock) || IL.NeLi_Stem_FoxFire.equal(aBlock) || IL.NeLi_Hyphae_Crimson.equal(aBlock) ) {
					rReturn = WD.set(aWorld, aX, aY, aZ, aBlock, aMeta+1, 3);
					tBark = null;
				}
			}
			// F4-flattening: vanilla logs are one block per species plus AXIS. Species and beam come from the
			// WoodDictionary centre (LoaderWoodDictionary:51-56), never from a hardcoded map.
			if (!rReturn && (aBlock == Blocks.OAK_LOG || aBlock == Blocks.SPRUCE_LOG || aBlock == Blocks.BIRCH_LOG
					|| aBlock == Blocks.JUNGLE_LOG || aBlock == Blocks.ACACIA_LOG || aBlock == Blocks.DARK_OAK_LOG)) {
				gregapi.wooddict.WoodEntry tWood = gregapi.wooddict.WoodDictionary.WOODS.get(aBlock, 0);
				if (tWood != null && tWood.mBeamEntry != null && ST.valid(tWood.mBeamEntry.mBeam)) {
					ItemStack tBeam = tWood.mBeamEntry.mBeam;
					rReturn = WD.set(aWorld, aX, aY, aZ, ST.block_(tBeam), pillarFromAxis(aWorld, aX, aY, aZ) | (ST.meta_(tBeam) & PILLAR_DATA), 3);
				}
			}
			if (!rReturn && BlocksGT.Beam1 != null) {
				if (IL.EtFu_Bark_Oak.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam1, aMeta, 3);
				} else if (IL.TF_Log_Darkwood.equal(aBlock) && (aMeta & 3) != 3) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam1, aMeta, 3);
				} else if (IL.TF_Log_Time.equal(aBlock) && (aMeta & 1) == 0) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam1, (aMeta&12)|((aMeta & 2) == 0 ? 1 : 2), 3);
				} else if (IL.HaC_Log_Paperbark.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam1, 3, 3);
					if (rReturn) tBark = ST.make(Items.PAPER, 4, 0);
				}
			}
			if (!rReturn && BlocksGT.Beam3 != null) {
				if (IL.TC_Greatwood_Log.equal(aBlock)) {
					if ((aMeta & 3) < 2)
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam3, aMeta, 3);
				} else if (IL.AETHER_Skyroot_Log_Gold.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam3, (aMeta&12)|2, 3);
					if (rReturn && (MD.AETHEL.mLoaded || (aMeta & 3) == 2)) tBark = OP.gem.mat(MT.AmberGolden, 1);
				} else if (IL.AETHER_Skyroot_Log.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam3, (aMeta&12)|2, 3);
				} else if (IL.AETHER_Skyroot_Log_Small.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam3, 2, 3);
				} else if (IL.TF_Log_Darkwood.equal(aBlock) && (aMeta & 3) == 3) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam3, aMeta, 3);
				}
			}
			if (!rReturn && BlocksGT.Beam2 != null) {
				if (IL.EtFu_Bark_Acacia.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam2, aMeta, 3);
				} else if (IL.IC2_Log_Rubber.equal(aBlock) || IL.MFR_Log_Rubber.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam2, 2, 3);
				} else if (IL.BTL_Weedwood_Log.equal(aBlock)) {
					rReturn = WD.set(aWorld, aX, aY, aZ, IL.BTL_Weedwood_Beam.block(), 0, 3);
				} else if (IL.BTL_Weedwood_Beam.equal(aBlock)) {
					rReturn = F;
				} else if (IL.TF_Log_Trans.equal(aBlock) && (aMeta & 1) == 1) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam2, (aMeta&12)|((aMeta & 2) == 0 ? 0 : 1), 3);
				} else if (OD.logWood.is(ST.make(aBlock, 1, aMeta)) && !OD.beamWood.is(ST.make(aBlock, 1, aMeta))) {
					rReturn = WD.set(aWorld, aX, aY, aZ, BlocksGT.Beam2, (aMeta&12)|3, 3);
				}
			}
			if (rReturn) {
				if (FAST_LEAF_DECAY) WD.leafdecay(aWorld, aX, aY, aZ, null, F, F);
				ST.give(aEntityPlayer, tBark, aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]);
				return aTool.equals(TOOL_axe) ? 500 : 1000;
			}
			return 0;
		}
		if (aTool.equals(TOOL_sense) || aTool.equals(TOOL_scythe)) {
			if (IC_CROPTILE && aTileEntity instanceof ICropTile) {
				int tDamage = 0;
				for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) for (int k = -1; k < 2; k++) if ((aTileEntity = WD.te(aWorld, aX+i, aY+j, aZ+k, T)) instanceof ICropTile && ((ICropTile)aTileEntity).harvest(T)) {
					UT.Sounds.send(SFX.MC_COLLECT, 0.2F, ((RNGSUS.nextFloat()-RNGSUS.nextFloat())*0.7F+1.0F)*2.0F, aWorld, aX+i, aY+j, aZ+k);
					tDamage += 10000;
				}
				if (aCanCollect) for (ItemStack tDrop : WD.suckAll(aWorld, aX-1.5, aY-0.5, aZ-1.5, 4, 2, 4)) ST.give(aEntityPlayer, tDrop, aWorld, aX, aY, aZ);
				return tDamage;
			}
			if (aBlock instanceof BonemealableBlock) {
				int tDamage = 0;
				for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) for (int k = -1; k < 2; k++) if (WD.meta(aWorld, aX+i, aY+j, aZ+k) == 7) {
					byte  tMeta  = WD.meta (aWorld, aX+i, aY+j, aZ+k);
					Block tBlock = WD.block(aWorld, aX+i, aY+j, aZ+k);
					BlockPos tActPos = new BlockPos(aX+i, aY+j, aZ+k);
					// was func_149851_a(World,x,y,z,isClient) - BonemealableBlock.isValidBonemealTarget (BonemealableBlock.java:14)
					if (tBlock.getClass() == aBlock.getClass() && !((BonemealableBlock)tBlock).isValidBonemealTarget(aWorld, tActPos, aWorld.getBlockState(tActPos), aWorld.isClientSide())) {
						// was onBlockActivated - BlockState.useWithoutItem (BlockBehaviour.java:783); the vanilla dispatch order
						// (useItemOn then useWithoutItem) is not reproduced, this is the closest single activate call.
						aWorld.getBlockState(tActPos).use(aWorld, aEntityPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(aX+i+aHitX, aY+j+aHitY, aZ+k+aHitZ), FORGE_DIR[aSide], tActPos, F));
						tDamage += 10000;
					}
					if (tMeta != WD.meta(aWorld, aX+i, aY+j, aZ+k) || tBlock != WD.block(aWorld, aX+i, aY+j, aZ+k)) {
						UT.Sounds.send(SFX.MC_COLLECT, 0.2F, ((RNGSUS.nextFloat()-RNGSUS.nextFloat())*0.7F+1.0F)*2.0F, aWorld, aX+i, aY+j, aZ+k);
					}
				}
				if (aCanCollect) for (ItemStack tDrop : WD.suckAll(aWorld, aX-1.5, aY-0.5, aZ-1.5, 4, 2, 4)) ST.give(aEntityPlayer, tDrop, aWorld, aX, aY, aZ);
				return tDamage;
			}
		}
		if (aTool.equals(TOOL_igniter) && ST.item(aStack) != Items.FLINT_AND_STEEL) {
			// Ignite any TNT Blocks.
			if (aBlock instanceof net.minecraft.world.level.block.TntBlock) {
				// was func_150114_a - TntBlock.onCaughtFire (TntBlock.java:152); face=null as in TntBlock itself (:50,58,66)
				((net.minecraft.world.level.block.TntBlock)aBlock).onCaughtFire(aWorld.getBlockState(new BlockPos(aX, aY, aZ)), aWorld, new BlockPos(aX, aY, aZ), null, aEntityLiving);
				WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				return 10000;
			}
			// Ignite Forestry Candles.
			if (IL.FR_Candle.equal(aBlock) && aTileEntity instanceof TileCandle) {
				((TileCandle)aTileEntity).setLit(T);
				WD.update(aWorld, aX, aY, aZ); // was markBlockForUpdate - central WD.update (WD.java:612)
				return 1;
			}
			// ADAPT-024: a candle occupies the cell, so the fire branch below can never reach it.
			// Engine path of flint and steel: canLight already gates lit/waterlogged (FlintAndSteelItem:30,50-52).
			BlockState tCandleState = aWorld.getBlockState(new BlockPos(aX, aY, aZ));
			if (net.minecraft.world.level.block.CandleBlock.canLight(tCandleState)) {
				WD.set(aWorld, aX, aY, aZ, tCandleState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT, T), 11);
				aWorld.gameEvent(aEntityLiving, net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, new BlockPos(aX, aY, aZ));
				return 10000;
			}
			// This thing has a special Functionality, which should override spawning Fire Blocks.
			if (!IL.TF_Lamp_of_Cinders.equal(aStack, T, T)) {
				if (aEntityPlayer == null || (aEntityPlayer).mayUseItemAt(new BlockPos(aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]), FORGE_DIR[aSide], aStack)) {
					if (WD.air(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide])) { // was isAirBlock - central WD.air (WD.java:859)
						if (WD.oxygen(aWorld, aX, aY, aZ)) WD.set(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide], Blocks.FIRE, 0, 3); // was setBlock with meta 0 and flags 3, the Forge 1.7.10 default - central WD.set (WD.java:644)
						return 10000;
					}
				}
			}
		}
		if (aTool.equals(TOOL_chisel) && !aSneaking) {
			ItemStack tChiseledBlock = WD.stack(aWorld, aX, aY, aZ);
			if (tChiseledBlock != null) {
				Recipe tRecipe = RM.Chisel.findRecipe(null, null, T, Integer.MAX_VALUE, null, ZL_FS, tChiseledBlock);
				if (tRecipe != null && tRecipe.blockINblockOUT() && ST.equal(tRecipe.mInputs[0], tChiseledBlock) && WD.set(aWorld, aX, aY, aZ, tRecipe.mOutputs[0])) return 10000;
			}
		}
		if (aTool.equals(TOOL_rotator)) {
			if (aBlock instanceof net.minecraft.world.level.block.RotatedPillarBlock || aBlock instanceof gregapi.block.tree.BlockBaseLog || aBlock instanceof gregapi.block.tree.BlockBaseBeam || aBlock instanceof gregapi.block.misc.BlockBaseBale /* было aBlock.getRenderType()==PILLAR_RENDER — Forge-render-hook удалён из движка; ровно эти 3 класса возвращали PILLAR_RENDER(31) в оригинале (grep getRenderType по gregtech6/), instanceof — точный 1:1 эквивалент */) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta + 4) & 15, 3, F)) return 5000;
			}
			if (aBlock instanceof PistonBaseBlock || aBlock instanceof DispenserBlock) {
				if (aMeta < 6 && WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta+1) % 6, 3, F)) return 2000;
			}
			if (aBlock instanceof CarvedPumpkinBlock || aBlock instanceof FurnaceBlock || aBlock instanceof ChestBlock || aBlock instanceof EnderChestBlock) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), ((aMeta-1)%4)+2, 3, F)) return 2500;
			}
			if (aBlock instanceof HopperBlock) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta+1)%6==1?(aMeta+1)%6:2, 3, F)) return 2500;
			}
			// Rotation goes through the WD.rotateBlock centre: every block rotates itself and a non-directional one
			// returns itself, which equals the Forge default. Coordinates aX,aX,aX are verbatim from 1.7.10.
			if (WD.rotateBlock(aWorld, aX, aX, aX, FORGE_DIR[aSide])) return 10000;
		}
		if (aTool.equals(TOOL_screwdriver)) {
			if (aBlock instanceof net.minecraft.world.level.block.DiodeBlock) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta / 4) * 4  + (((aMeta%4) + 1) % 4), 3, F)) return 10000;
			}
		}
		if (aTool.equals(TOOL_crowbar)) {
			if (aBlock instanceof BaseRailBlock && (!MD.RC.mLoaded || !(MD.MC.owns(aBlock) || MD.RC.owns(aBlock)))) {
				; // 1.7.10 suppressed the client packet around setBlock; flag 0 (no UPDATE_CLIENTS) gives the same result here.
				// Why the fuck are the two Coordinate Parameters in isFlexibleRail switched? And then it is used like x y z instead of using the broken namings.
				// was isFlexibleRail(World,x,y,z) - BaseRailBlock.isFlexibleRail (BaseRailBlock.java:310)
				boolean tResult = WD.set(aWorld, aX, aY, aZ, aBlock, ((BaseRailBlock)aBlock).isFlexibleRail(aWorld.getBlockState(new BlockPos(aX, aY, aZ)), aWorld, new BlockPos(aX, aY, aZ)) ? (aMeta+1) % 10 : ((aMeta/8) * 8) + (((aMeta%8)+1) % 6), 0);
				;
				return tResult?2000:0;
			}
		}
		if (aTool.equals(TOOL_softhammer)) {
			if (aBlock == Blocks.REDSTONE_LAMP) {
				; // 1.7.10 suppressed the client packet around setBlock; flag 0 (no UPDATE_CLIENTS) gives the same result here.
				boolean tResult = WD.set(aWorld, aX, aY, aZ, Blocks.REDSTONE_LAMP, 0, 0);
				;
				return tResult?10000:0;
			}
			if (aBlock == Blocks.REDSTONE_LAMP) {
				; // 1.7.10 suppressed the client packet around setBlock; flag 0 (no UPDATE_CLIENTS) gives the same result here.
				boolean tResult = WD.set(aWorld, aX, aY, aZ, Blocks.REDSTONE_LAMP, 0, 0);
				;
				return tResult?10000:0;
			}
			if (aBlock == Blocks.POWERED_RAIL) {
				; // 1.7.10 suppressed the client packet around setBlock; flag 0 (no UPDATE_CLIENTS) gives the same result here.
				boolean tResult = WD.set(aWorld, aX, aY, aZ, aBlock, (aMeta + 8) % 16, 0);
				;
				return tResult?10000:0;
			}
			if (aBlock == Blocks.ACTIVATOR_RAIL) {
				; // 1.7.10 suppressed the client packet around setBlock; flag 0 (no UPDATE_CLIENTS) gives the same result here.
				boolean tResult = WD.set(aWorld, aX, aY, aZ, aBlock, (aMeta + 8) % 16, 0);
				;
				return tResult?10000:0;
			}
			if (aBlock instanceof net.minecraft.world.level.block.RotatedPillarBlock || aBlock instanceof gregapi.block.tree.BlockBaseLog || aBlock instanceof gregapi.block.tree.BlockBaseBeam || aBlock instanceof gregapi.block.misc.BlockBaseBale /* было aBlock.getRenderType()==PILLAR_RENDER — Forge-render-hook удалён из движка; ровно эти 3 класса возвращали PILLAR_RENDER(31) в оригинале (grep getRenderType по gregtech6/), instanceof — точный 1:1 эквивалент */) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta + 4) & 15, 3, F)) return 5000;
			}
			if (aBlock instanceof PistonBaseBlock || aBlock instanceof DispenserBlock) {
				if (aMeta < 6 && WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta+1) % 6, 3, F)) return 2000;
			}
			if (aBlock instanceof CarvedPumpkinBlock || aBlock instanceof FurnaceBlock || aBlock instanceof ChestBlock || aBlock instanceof EnderChestBlock) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), ((aMeta-1)%4)+2, 3, F)) return 2500;
			}
			if (aBlock instanceof HopperBlock) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta+1)%6==1?(aMeta+1)%6:2, 3, F)) return 2500;
			}
		}
		if (aTool.equals(TOOL_wrench) || aTool.equals(TOOL_monkeywrench)) {
			if (GC_BLOCKADVANCED && aBlock instanceof BlockAdvanced) {
				return (aSneaking ? ((BlockAdvanced)aBlock).onSneakUseWrench(aWorld, aX, aY, aZ, aEntityPlayer, aSide, aHitX, aHitY, aHitZ) : ((BlockAdvanced)aBlock).onUseWrench(aWorld, aX, aY, aZ, aEntityPlayer, aSide, aHitX, aHitY, aHitZ)) ? 2500 : 0;
			}
			
			byte aTargetSide = UT.Code.getSideWrenching(aSide, aHitX, aHitY, aHitZ);
			if (IC_WRENCHABLE && aTileEntity instanceof IWrenchable) {
				if (((IWrenchable)aTileEntity).wrenchCanSetFacing(aEntityPlayer, aTargetSide)) {
					((IWrenchable)aTileEntity).setFacing(aTargetSide);
					return 10000;
				}
				if (((IWrenchable)aTileEntity).wrenchCanRemove(aEntityPlayer)) {
					int tDamage = Math.max(10000, (int)(30000 / ((IWrenchable)aTileEntity).getWrenchDropRate()));
					// was getDrops(World,x,y,z,meta,fortune) - Block.getDrops (Block.java:361); the level here is always a ServerLevel
					List<ItemStack> tDrops = Block.getDrops(aWorld.getBlockState(new BlockPos(aX, aY, aZ)), (ServerLevel)aWorld, new BlockPos(aX, aY, aZ), aTileEntity);
					ItemStack tOutput = ((IWrenchable)aTileEntity).getWrenchDrop(aEntityPlayer);
					
					if (WD.set(aWorld, aX, aY, aZ, NB, 0, 3)) {
						if (RNGSUS.nextInt(tDamage) < aRemainingDurability) {
							for (ItemStack tStack : tDrops) {
								if (tOutput == null) {
									ST.give(aEntityPlayer, tStack, F, aWorld, aX+0.5, aY+0.5, aZ+0.5);
								} else {
									ST.give(aEntityPlayer, tOutput, F, aWorld, aX+0.5, aY+0.5, aZ+0.5);
									tOutput = null;
								}
							}
						}
						return tDamage;
					}
				}
			}

			// AE2 arm: its machines are turned by the GregTech wrench, and AE2 quartz wrenches are disabled in the pack.
			// The c:tools/wrench tag is unusable: all GT6 tools are one registry entry, the type is held in stack meta.
			if (AE_BASEBLOCKENTITY && aEntityPlayer != null && aTileEntity instanceof appeng.blockentity.AEBaseBlockEntity tAEEntity) {
				BlockPos tPosAE = new BlockPos(aX, aY, aZ);
				if (!appeng.util.Platform.hasPermissions(new appeng.api.util.DimensionalBlockPos(aWorld, tPosAE), aEntityPlayer)) return 0;
				if (aSneaking) {
					// Dismantle through AE2's own call instead of breaking the block: it exports settings and the inner
					// inventory (AEBaseBlockEntity:441). Break particles stay visible - AE2 hides them behind a private flag.
					if (tAEEntity.disassembleWithWrench(aEntityPlayer, aWorld, new BlockHitResult(new Vec3(aX+aHitX, aY+aHitY, aZ+aHitZ), FORGE_DIR[aSide], tPosAE, F), aStack).consumesAction()) {
						aWorld.playSound(aEntityPlayer, tPosAE, net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
						return 10000;
					}
					return 0;
				}
				// Rotation keeps GregTech semantics: the side comes from the hit point (nine zones per face) and the block
				// faces it, instead of the click-until-it-fits cycle of AE2 (WrenchHook:83-92). Spin is kept.
				BlockState tStateAE = aWorld.getBlockState(tPosAE);
				appeng.api.orientation.IOrientationStrategy tStrategyAE = appeng.api.orientation.IOrientationStrategy.get(tStateAE);
				if (tStrategyAE.allowsPlayerRotation()) {
					BlockState tNewAE = tStrategyAE.setOrientation(tStateAE, FORGE_DIR[aTargetSide], appeng.api.orientation.BlockOrientation.get(tStrategyAE, tStateAE).getSpin());
					if (tNewAE != tStateAE && tNewAE.canSurvive(aWorld, tPosAE) && WD.set(aWorld, aX, aY, aZ, tNewAE, 3)) return 10000;
					// Clicking the face a machine already faces: a wrench in hand gates the GUI in GregTech, so returning 0
					// would drop through and open the AE2 menu instead. An idle click costs small wear and cancels the GUI.
					return 2500;
				}
				return 0;
			}

			if (aBlock instanceof net.minecraft.world.level.block.RotatedPillarBlock || aBlock instanceof gregapi.block.tree.BlockBaseLog || aBlock instanceof gregapi.block.tree.BlockBaseBeam || aBlock instanceof gregapi.block.misc.BlockBaseBale /* было aBlock.getRenderType()==PILLAR_RENDER — Forge-render-hook удалён из движка; ровно эти 3 класса возвращали PILLAR_RENDER(31) в оригинале (grep getRenderType по gregtech6/), instanceof — точный 1:1 эквивалент */) {
				if (WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), (aMeta + 4) & 15, 3, F)) return 5000;
			}
			
			if (aBlock instanceof net.minecraft.world.level.block.CraftingTableBlock || aBlock == Blocks.BOOKSHELF) { // was instanceof BlockBookshelf - no such class here, the vanilla bookshelf is a plain Block (Blocks.java:1179)
				if (WD.set(aWorld, aX, aY, aZ, NB, 0, 3)) {
					ST.drop(aWorld, aX+0.5, aY+0.5, aZ+0.5, ST.make(aBlock, 1, aMeta));
					return 10000;
				}
			}
			
			if (aMeta == aTargetSide) {
				if (aBlock instanceof CarvedPumpkinBlock || aBlock instanceof PistonBaseBlock || aBlock instanceof DispenserBlock || aBlock instanceof FurnaceBlock || aBlock instanceof ChestBlock || aBlock instanceof HopperBlock || aBlock instanceof EnderChestBlock) {
					if (WD.set(aWorld, aX, aY, aZ, NB, 0, 3)) {
						ST.drop(aWorld, aX+0.5, aY+0.5, aZ+0.5, ST.make(aBlock, 1, 0));
						return 10000;
					}
				}
			} else {
				if (aBlock instanceof PistonBaseBlock || aBlock instanceof DispenserBlock) {
					if (aMeta < 6 && WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aTargetSide, 3, F)) return 10000;
				}
				if (aBlock instanceof CarvedPumpkinBlock || aBlock instanceof FurnaceBlock || aBlock instanceof ChestBlock || aBlock instanceof EnderChestBlock) {
					if (SIDES_HORIZONTAL[aTargetSide] && WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aTargetSide, 3, F)) return 10000;
				}
				if (aBlock instanceof HopperBlock) {
					if (SIDES_BOTTOM_HORIZONTAL[aTargetSide] && WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), aTargetSide, 3, F)) return 10000;
				}
			}
			if (aBlock instanceof BaseRailBlock || aBlock instanceof net.minecraft.world.level.block.DiodeBlock || aBlock instanceof net.minecraft.world.level.block.piston.PistonHeadBlock || aBlock instanceof PistonBaseBlock) {
				// wrench doesn't work on those.
			} else {
				// The getValidRotations pre-check of 1.7.10 is absorbed: WD.rotateBlock (state.rotate) already returns
				// false for anything non-rotatable.
				if (WD.rotateBlock(aWorld, aX, aY, aZ, FORGE_DIR[aTargetSide])) return 10000;
			}
		}
		if (aTool.equals(TOOL_prospector)) {
			if (prospectOre(aBlock, aMeta, aChatReturn, aWorld, aX, aY, aZ)) return 100;
			if (aBlock != Blocks.OBSIDIAN && (WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.STONE) || WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.NETHERRACK) || WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.END_STONE) || WD.stone(aBlock, aMeta))) {
				if (prospectStone(aBlock, aMeta, aQuality, aChatReturn, aWorld, aSide, aX, aY, aZ)) return 10000;
			}
			return 0;
		}
		
		} catch(Throwable e) {
			ERR.println(String.format("Exception occured when ToolCompat was used at the Coordinates: [%d;%d;%d] at '%s' with TileEntity '%s' using the Tool '%s' %s", aX, aY, aZ, aBlock.getDescriptionId(), aTileEntity.getClass(), aTool, e.toString())); // F2/logging: was Logging.severe - fml Logging has no such call, so it routes through the central ERR (GT_API_Proxy:25)
			e.printStackTrace(ERR);
		}
		return 0;
	}
	
	public static boolean prospectOre(Block aBlock, byte aMeta, List<String> aChatReturn, Level aWorld, int aX, int aY, int aZ) {
		OreDictItemData tAssotiation = OM.anyassociation(ST.make(aBlock, 1, WD.meta(aWorld, aX, aY, aZ)));
		if (tAssotiation != null && tAssotiation.mPrefix.contains(TD.Prefix.ORE)) {
			if (aChatReturn != null) aChatReturn.add(LanguageHandler.getLocalName(tAssotiation.mPrefix, tAssotiation.mMaterial.mMaterial)+"!");
			return T;
		}
		return F;
	}
	
	public static boolean prospectStone(Block aBlock, byte aMeta, long aQuality, List<String> aChatReturn, Level aWorld, byte aSide, int aX, int aY, int aZ) {
		Block tBlock;
		int tX = aX, tY = aY, tZ = aZ, tQuality = (int)UT.Code.bind(1, 20, aQuality + 4);
		
		for (int i = 0, j = tQuality; i < j; i++) {
			tX -= OFFX[aSide];
			tY -= OFFY[aSide];
			tZ -= OFFZ[aSide];
			
			// The Strings in this do not want to be localized, and not even Backup Lang wants to work.
			tBlock = WD.block(aWorld, tX, tY, tZ);
			if (tBlock == Blocks.LAVA || tBlock == Blocks.LAVA) {
				if (aChatReturn != null) aChatReturn.add("There is Lava behind this Rock");
				break;
			}
			if (tBlock instanceof LiquidBlock || tBlock instanceof IFluidBlock) {
				if (aChatReturn != null) aChatReturn.add("There is a Fluid behind this Rock");
				break;
			}
			if (tBlock instanceof InfestedBlock || !WD.hasCollide(aWorld, tX, tY, tZ, tBlock)) {
				if (aChatReturn != null) aChatReturn.add("There is an Air Pocket behind this Rock");
				break;
			}
			if (i < 4) if (tBlock != aBlock || aMeta != WD.meta(aWorld, tX, tY, tZ)) {
				if (aChatReturn != null) aChatReturn.add("Material is changing behind this Rock");
				break;
			}
		}
		
		Random tRandom = new Random(aX^aY^aZ^aSide);
		for (int i = 0, j = 1+2*tQuality, k = tQuality * tQuality; i < k; i++) {
			tX = aX-tQuality+tRandom.nextInt(j);
			tY = aY-tQuality+tRandom.nextInt(j);
			tZ = aZ-tQuality+tRandom.nextInt(j);
			tBlock = WD.block(aWorld, tX, tY, tZ);
			
			if (tBlock != NB && tBlock != Blocks.OBSIDIAN && tBlock != BlocksGT.RockOres) {
				OreDictItemData tAssotiation = OM.anyassociation((tBlock instanceof IBlockRetrievable ? ((IBlockRetrievable)tBlock).getItemStackFromBlock(aWorld, tX, tY, tZ, SIDE_INVALID) : ST.make(tBlock, 1, WD.meta(aWorld, tX, tY, tZ))));
				if (tAssotiation != null && tAssotiation.mPrefix.containsAny(TD.Prefix.STANDARD_ORE, TD.Prefix.DENSE_ORE)) {
					if (aChatReturn != null) aChatReturn.add("Found traces of " + tAssotiation.mMaterial.mMaterial.getLocal());
					return T;
				}
			}
		}
		if (aChatReturn != null && aChatReturn.isEmpty()) aChatReturn.add("No traces of Ore found");
		return T;
	}
}
