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

package gregapi.util;
import gregapi.code.ItemNBT;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.inventory.AbstractContainerMenu;
// F5: net.minecraftforge.fluids.BlockFluidClassic/BlockFluidFinite удалены (см. liquid_classic/liquid_finite ниже).
import net.minecraftforge.fluids.IFluidBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import cpw.mods.fml.common.FMLCommonHandler;
import gregapi.GT_API;
import gregapi.block.IBlockDebugable;
import gregapi.block.IBlockExtendedMetaData;
import gregapi.block.IBlockPlacable;
import gregapi.block.IBlockTileEntity;
import gregapi.block.metatype.BlockMetaType;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.HashSetNoNulls;
import gregapi.code.ItemStackContainer;
import gregapi.code.TagData;
import gregapi.data.*;
import gregapi.event.BlockScanningEvent;
import gregapi.item.IItemGT;
import gregapi.oredict.OreDictMaterial;
import gregapi.random.IHasWorldAndCoords;
import gregapi.tileentity.ITileEntity;
import gregapi.tileentity.ITileEntityQuickObstructionCheck;
import gregapi.tileentity.ITileEntityUnloadable;
import gregapi.tileentity.data.ITileEntityGibbl;
import gregapi.tileentity.data.ITileEntityProgress;
import gregapi.tileentity.data.ITileEntityTemperature;
import gregapi.tileentity.data.ITileEntityWeight;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import gregapi.tileentity.delegate.ITileEntityDelegating;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregapi.tileentity.energy.ITileEntityEnergyDataCapacitor;
import gregapi.tileentity.machines.*;
import gregapi.util.UT.Code;
import gregtech.blocks.fluids.BlockWaterlike;
import micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock;
import micdoodle8.mods.galacticraft.core.util.OxygenUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import gregapi.block.Material;
import gregapi.block.BlockBase;
import gregapi.block.multitileentity.MultiTileEntityBlock;
import gregapi.block.metatype.BlockStones;
import net.minecraft.core.Direction;
// F#(WD-block): доступ к блокам мира переучен на BlockPos/BlockState (world.getBlockState(pos).getBlock() —
// BlockGetter.java:32 + BlockBehaviour.java:521 getBlock()); координатные типы/шейпы/рейтрейс — ниже.
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.util.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

import net.neoforged.neoforge.common.NeoForge;
import net.minecraftforge.fluids.*;
import thaumcraft.api.nodes.INode;

import java.util.*;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class WD {
	/** F-bounds центр (instanceof-безопасно): 1.7.10 {@code WD.setBlockBounds(aBlock, ...)} мутировал bounds блока
	 *  (рендер per-pass / коллизия); neo bounds immutable → делегируем GT6-блоку (хранит сам, {@link gregapi.block.IBlock}),
	 *  не-GT6 блок игнорируется (рендер-использование отложено на F3-клиент-проход). */
	public static void setBlockBounds(net.minecraft.world.level.block.Block aBlock, float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		if (aBlock instanceof gregapi.block.IBlock) ((gregapi.block.IBlock)aBlock).setBlockBounds(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ);
	}
	public static ItemStack suck(IHasWorldAndCoords aCoordinates) {return suck(aCoordinates.getWorld(), aCoordinates.getX(), aCoordinates.getY(), aCoordinates.getZ());}
	public static ItemStack suck(Level aWorld, double aX, double aY, double aZ) {return suck(aWorld, aX, aY, aZ, 1, 1, 1);}
	@SuppressWarnings("unchecked")
	public static ItemStack suck(Level aWorld, double aX, double aY, double aZ, double aL, double aH, double aW) {
		for (ItemEntity tItem : (Iterable<ItemEntity>)aWorld.getEntitiesOfClass(ItemEntity.class, new AABB(aX, aY, aZ, aX+aL, aY+aH, aZ+aW))) {
			if (!tItem.isRemoved()) {
				tItem.discard();
				ItemStack rStack = tItem.getItem();
				tItem.setItem(ST.amount(0, rStack));
				tItem.discard();
				return rStack;
			}
		}
		return null;
	}
	public static List<ItemStack> suckAll(IHasWorldAndCoords aCoordinates) {return suckAll(aCoordinates.getWorld(), aCoordinates.getX(), aCoordinates.getY(), aCoordinates.getZ());}
	public static List<ItemStack> suckAll(Level aWorld, double aX, double aY, double aZ) {return suckAll(aWorld, aX, aY, aZ, 1, 1, 1);}
	@SuppressWarnings("unchecked")
	public static List<ItemStack> suckAll(Level aWorld, double aX, double aY, double aZ, double aL, double aH, double aW) {
		List<ItemEntity> tList = aWorld.getEntitiesOfClass(ItemEntity.class, new AABB(aX, aY, aZ, aX+aL, aY+aH, aZ+aW));
		if (tList.isEmpty()) return Collections.emptyList();
		List<ItemStack> rOutput = ST.arraylist();
		for (ItemEntity tItem : tList) {
			if (!tItem.isRemoved()) {
				tItem.discard();
				ItemStack rStack = tItem.getItem();
				tItem.setItem(ST.amount(0, rStack));
				tItem.discard();
				rOutput.add(rStack);
			}
		}
		return rOutput;
	}
	
	public static boolean obstructed(Level aWorld, int aX, int aY, int aZ, byte aSide) {
		if (!OBSTRUCTION_CHECKS) return F;
		aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		BlockEntity tTileEntity = te(aWorld, aX, aY, aZ, T);
		if (tTileEntity != null) {
			if (tTileEntity instanceof ITileEntityQuickObstructionCheck) return ((ITileEntityQuickObstructionCheck)tTileEntity).isObstructingBlockAt(OPOS[aSide]);
			if (MD.TC.mLoaded && tTileEntity instanceof INode) return F;
		}
		BlockPos tObstrPos = new BlockPos(aX, aY, aZ);
		BlockState tObstrState = aWorld.getBlockState(tObstrPos); // было aWorld.getBlock(x,y,z) — BlockGetter.java:32
		Block tBlock = tObstrState.getBlock();
		if (tBlock instanceof TrapDoorBlock || tBlock instanceof DoorBlock || tBlock instanceof LadderBlock) return F;
		// было tBlock.getCollisionBoundingBoxFromPool(world,x,y,z) — BlockBehaviour.getCollisionShape(level,pos)
		// (BlockBehaviour.java:674) даёт локальный VoxelShape; .move(pos).bounds() переносит в мировые координаты
		// (VoxelShape.java:39,81); пустой шейп = старое null-возврату (нет коллизии).
		VoxelShape tObstrShape = tObstrState.getCollisionShape(aWorld, tObstrPos);
		if (tObstrShape.isEmpty()) return F;
		AABB tBoundingBox = tObstrShape.move(tObstrPos).bounds();
		switch(aSide) {
		case 0: return tBoundingBox.maxY-aY > PX_N[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2];
		case 1: return tBoundingBox.minY-aY < PX_P[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2];
		case 2: return tBoundingBox.maxZ-aZ > PX_N[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		case 3: return tBoundingBox.minZ-aZ < PX_P[4] && tBoundingBox.maxX-aX > PX_P[2] && tBoundingBox.minX-aX < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		case 4: return tBoundingBox.maxX-aX > PX_N[4] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		case 5: return tBoundingBox.minX-aX < PX_P[4] && tBoundingBox.maxZ-aZ > PX_P[2] && tBoundingBox.minZ-aZ < PX_N[2] && tBoundingBox.maxY-aY > PX_P[2] && tBoundingBox.minY-aY < PX_N[2];
		}
		return F;
	}
	
	public static HitResult getMOP(Level aWorld, Player aPlayer, boolean aFlag) {
		Vec3 vec3 = new Vec3( // 1.7.10 Vec3.createVectorHelper(x,y,z) удалён -> neo ctor new Vec3(double,double,double).
		  aPlayer.xo + (aPlayer.getX() - aPlayer.xo)
		, aPlayer.yo + (aPlayer.getY() - aPlayer.yo) + (aWorld.isClientSide() ? aPlayer.getEyeHeight() - aPlayer.getEyeHeight(net.minecraft.world.entity.Pose.STANDING) : aPlayer.getEyeHeight()) // F6-eye: 1.7.10 getDefaultEyeHeight() -> neo getEyeHeight(Pose.STANDING) (стоячая высота глаз, Entity.java:3381). isRemote check to revert changes to ray trace position due to adding the eye height clientside and player yOffset differences
		, aPlayer.zo + (aPlayer.getZ() - aPlayer.zo)
		);
		float  tPitch = aPlayer.xRotO + (aPlayer.getXRot() - aPlayer.xRotO);
		float  tYaw   = aPlayer.yRotO   + (aPlayer.getYRot()   - aPlayer.yRotO  );
		float  tZ     =  Mth.cos(-tYaw   * 0.017453292F - (float)Math.PI);
		float  tX     =  Mth.sin(-tYaw   * 0.017453292F - (float)Math.PI);
		float  tW     = -Mth.cos(-tPitch * 0.017453292F);
		float  tY     =  Mth.sin(-tPitch * 0.017453292F);
		double tReach = (aPlayer instanceof ServerPlayer ? ((ServerPlayer)aPlayer).blockInteractionRange() : 5);
		// было aWorld.func_147447_a(from,to,stopOnLiquid,ignoreBlockWithoutBoundingBox,returnLastUncollidableBlock=F) —
		// neo: BlockGetter.clip(ClipContext) (BlockGetter.java:65). stopOnLiquid=aFlag -> ClipContext.Fluid.ANY/NONE
		// (ClipContext.java:96-110, ANY подбирает любую непустую FluidState, NONE — никогда); Block.OUTLINE — тот же
		// режим формы, которым реально пользуется ванильный player-look-raytrace (Item.getPlayerPOVHitResult,
		// Item.java:362-365); returnLastUncollidableBlock здесь всегда F (аналога нет, не задействован).
		return aWorld.clip(new ClipContext(vec3, vec3.add(tX * tW * tReach, tY * tReach, tZ * tW * tReach), ClipContext.Block.OUTLINE, aFlag ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, aPlayer));
	}
	
	// F6: было `WorldProvider aProvider`-перегрузки (числовой `dimensionId`, `UT.Reflection.getLowercaseClass`
	// по имени java-класса провайдера стороннего мода) ПАРАЛЛЕЛЬНО с `Level aWorld`-перегрузками, вызывавшими их
	// через `aWorld.provider` — `net.minecraft.world.WorldProvider` в neo удалён целиком (нет ни в одном из 3
	// корней референса), из-за чего компилятор не мог выбрать между двумя `dimXXX(...)`-перегрузками с одним
	// именем (ambiguous). `WorldProvider`-перегрузки убраны, остался один вход — `dimXXX(Level)`.
	// Собственная (не мод-зависимая) идентификация ванильных измерений переведена дословно на neo-эквиваленты
	// (`Level.dimension()`==`Level.OVERWORLD/NETHER/END`, см. `decisions/README.md` «Dimension-identity»).
	// Идентификация измерений СТОРОННИХ модов шла через reflection по имени java-класса `WorldProvider`-подкласса
	// этого мода (`"WorldProviderCaves".equalsIgnoreCase(...)` и т.п.) либо (dimTF) через числовой
	// `TwilightForestMod.dimensionID` — ни один из 3 корней референса не содержит neo-эквивалента для этих
	// древних 1.7.10-модов (не портированы), поэтому это честно `PORT-TODO`, а не выдумка: пока стоит `F`.
	public static boolean dimOverworldLike(Level aWorld) {return aWorld != null && (aWorld.dimension() == Level.OVERWORLD || dimENVM(aWorld) || dimA97(aWorld) || dimWTCH(aWorld) || dimMYST(aWorld) || dimCW2(aWorld));}

	public static boolean dimPlanet(Level aWorld) {return aWorld != null && aWorld.dimension() != Level.OVERWORLD && aWorld.dimension() != Level.NETHER && aWorld.dimension() != Level.END && !(dimMYST(aWorld) || dimATUM(aWorld) || dimWTCH(aWorld) || dimA97(aWorld) || dimCW2(aWorld) || dimTF(aWorld) || dimERE(aWorld) || dimBTL(aWorld) || dimENVM(aWorld) || dimDD(aWorld) || dimLM(aWorld) || dimAETHER(aWorld) || dimALF(aWorld) || dimTROPIC(aWorld) || dimCANDY(aWorld));}

	public static boolean dimMYST(Level aWorld) {return aWorld != null && MD.MYST.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimMYST — Mystcraft-провайдер определялся по имени java-класса ("com.xcompwiz.mystcraft"), WorldProvider удалён, аналога нет ни в одном из 3 корней референса */}

	public static boolean dimCANDY(Level aWorld) {return aWorld != null && MD.CANDY.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimCANDY — CandyCraft-провайдер по имени класса "WorldProviderCandy" */}

	public static boolean dimTROPIC(Level aWorld) {return aWorld != null && MD.TROPIC.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimTROPIC — Tropicraft-провайдер по имени класса "WorldProviderTropicraft" */}

	public static boolean dimATUM(Level aWorld) {return aWorld != null && MD.ATUM.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimATUM — Atum-провайдер по имени класса "AtumWorldProvider" */}

	public static boolean dimTF(Level aWorld) {return aWorld != null && MD.TF.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimTF — сравнение с числовым TwilightForestMod.dimensionID, WorldProvider.dimensionId удалён вместе с числовой identity измерений */}

	public static boolean dimBTL(Level aWorld) {return aWorld != null && MD.BTL.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimBTL — Betweenlands-провайдер по имени класса "WorldProviderBetweenlands" */}

	public static boolean dimERE(Level aWorld) {return aWorld != null && MD.ERE.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimERE — Erebus-провайдер по имени класса "WorldProviderErebus" */}

	public static boolean dimALF(Level aWorld) {return aWorld != null && MD.ALF.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimALF — Alfheim-провайдер по имени класса "WorldProviderAlfheim" */}

	public static boolean dimDD(Level aWorld) {return aWorld != null && (MD.ExU.mLoaded || MD.ExS.mLoaded) && F; /* PORT-TODO(F6, WD world-provider identity): dimDD — Underdark-провайдер по имени класса "WorldProviderUnderdark" */}

	public static boolean dimLM(Level aWorld) {return aWorld != null && (MD.ExU.mLoaded || MD.ExS.mLoaded) && F; /* PORT-TODO(F6, WD world-provider identity): dimLM — EndOfTime-провайдер по имени класса "WorldProviderEndOfTime" */}

	public static boolean dimENVM(Level aWorld) {return aWorld != null && MD.ENVM.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimENVM — Enviromine Caves-провайдер по имени класса "WorldProviderCaves" */}

	public static boolean dimGC(Level aWorld) {return aWorld != null && MD.GC.mLoaded && F; /* PORT-TODO(F6/F10, WD world-provider identity): dimGC — Galacticraft-измерение определялось `aWorld.provider instanceof IGalacticraftWorldProvider`, WorldProvider удалён из движка (та же болезнь, что у семейства dimXXX выше) */}

	public static boolean dimA97(Level aWorld) {return aWorld != null && MD.A97_MINING.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimA97 — Aroma1997 Mining-провайдер по имени класса "WorldProviderMiner" */}

	public static boolean dimCW2(Level aWorld) {return aWorld != null && (dimCW2AquaCavern(aWorld) || dimCW2Caveland(aWorld) || dimCW2Cavenia(aWorld) || dimCW2Cavern(aWorld) || dimCW2Caveworld(aWorld));}

	public static boolean dimCW2AquaCavern(Level aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimCW2AquaCavern — по имени класса "WorldProviderAquaCavern" */}

	public static boolean dimCW2Caveland(Level aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimCW2Caveland — по имени класса "WorldProviderCaveland" */}

	public static boolean dimCW2Cavenia(Level aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimCW2Cavenia — по имени класса "WorldProviderCavenia" */}

	public static boolean dimCW2Cavern(Level aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimCW2Cavern — по имени класса "WorldProviderCavern" */}

	public static boolean dimCW2Caveworld(Level aWorld) {return aWorld != null && MD.CW2.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimCW2Caveworld — по имени класса "WorldProviderCaveworld" */}

	public static boolean dimWTCH(Level aWorld) {return aWorld != null && MD.WTCH.mLoaded && F; /* PORT-TODO(F6, WD world-provider identity): dimWTCH — Witchery Dream World-провайдер по имени класса "WorldProviderDreamWorld" */}

	public static boolean dimAETHER(Level aWorld) {return aWorld != null && (MD.AETHER.mLoaded || MD.AETHEL.mLoaded) && F; /* PORT-TODO(F6, WD world-provider identity): dimAETHER — Aether-провайдер по имени класса "AetherWorldProvider"/"WorldProviderAether" */}

	/** было ручное 1.7.10 dimension-travel (DimensionManager/ridingEntity/removePlayerEntityDangerously/ClientboundRespawnPacket/
	 *  theItemInWorldManager/getConfigurationManager/FMLCommonHandler.firePlayerChangedDimensionEvent/createEntityByName — все удалены) —
	 *  neo Entity.teleportTo(ServerLevel,x,y,z,Set<Relative>,yRot,xRot,resetCamera) (Entity.java:3257) выполняет весь цикл кросс-мерного
	 *  перемещения (спешивание/respawn-пакет/inventory-sync/пере-создание сущности) внутри. Целевой мир по int-dim через WD.dimensionId
	 *  (getAllLevels:1239). resetCamera=F, координаты абсолютные (пустой Set<Relative>). PORT-TODO(F-dimension): модовые int-id зависят от WD.dimensionId-карты. */
	public static boolean move(Entity aEntity, int aDimension, double aX, double aY, double aZ) {
		MinecraftServer tServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
		if (tServer == null || !(aEntity.level() instanceof ServerLevel)) return F;
		ServerLevel tTargetWorld = null;
		for (ServerLevel tLevel : tServer.getAllLevels()) if (WD.dimensionId(tLevel) == aDimension) {tTargetWorld = tLevel; break;}
		if (tTargetWorld == null || tTargetWorld == aEntity.level()) return F;
		return aEntity.teleportTo(tTargetWorld, aX+0.5, aY+0.5, aZ+0.5, java.util.Set.<net.minecraft.world.entity.Relative>of(), aEntity.getYRot(), aEntity.getXRot(), F);
	}
	
	
	/** Marks a Chunk dirty so it is saved */
	public static boolean mark(Level aWorld, int aX, int aZ) {
		if (aWorld == null || aWorld.isClientSide()) return F;
		// было aWorld.getChunkFromBlockCoords(x,z) — neo: Level.getChunk(int,int) (Level.java:202), блок-координаты
		// >>4 переведены в чанк-координаты вручную (как делал старый метод внутри себя).
		LevelChunk aChunk = aWorld.getChunk(aX >> 4, aZ >> 4);
		if (aChunk == null) {
			aWorld.getBlockState(new BlockPos(aX, 0, aZ)); // было WD.meta(aWorld, x,0,z) — тот же "трогающий" вызов для форс-загрузки чанка, результат отбрасывался и раньше
			aChunk = aWorld.getChunk(aX >> 4, aZ >> 4);
			if (aChunk == null) {
				ERR.println("Some important Chunk does not exist for some reason at Coordinates X: " + aX + " and Z: " + aZ);
				return F;
			}
		}
		aChunk.markUnsaved(); // было aChunk.markUnsaved() — neo: LevelChunk.markUnsaved() (см. Level.java:868 aWorld.getChunkAt(pos).markUnsaved())
		return T;
	}
	/** Marks a Chunk dirty so it is saved */
	public static boolean mark(Object aTileEntity) {
		// было .getWorldObj()/.x/.z — neo: BlockEntity.getLevel() (BlockEntity.java:89) + .getBlockPos() (BlockEntity.java:232)
		return aTileEntity instanceof BlockEntity && mark(((BlockEntity)aTileEntity).getLevel(), ((BlockEntity)aTileEntity).getBlockPos().getX(), ((BlockEntity)aTileEntity).getBlockPos().getZ());
	}
	
	
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static DelegatorTileEntity<BlockEntity> te(Level aWorld, BlockPos aCoords, byte aSide, boolean aLoadUnloadedChunks) {
		return te(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aSide, aLoadUnloadedChunks);
	}
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static DelegatorTileEntity<BlockEntity> te(Level aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {
		BlockEntity aTileEntity = te(aWorld, aX, aY, aZ, aLoadUnloadedChunks);
		return aTileEntity instanceof ITileEntityDelegating ? ((ITileEntityDelegating)aTileEntity).getDelegateTileEntity(aSide) : new DelegatorTileEntity<>(aTileEntity, aWorld, aX, aY, aZ, aSide);
	}
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static BlockEntity te(Level aWorld, BlockPos aCoords, boolean aLoadUnloadedChunks) {
		return te(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aLoadUnloadedChunks);
	}
	/** to get a TileEntity properly, according to my additional Interfaces. Normally you should set aLoadUnloadedChunks to false, unless you have already checked these Coordinates, or you want to load Chunks */
	public static BlockEntity te(Level aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		if (aLoadUnloadedChunks || aWorld.isLoaded(tPos)) { // было WD.exists(aWorld, x, y, z) — Level.isLoaded(BlockPos) (Level.java:695)
			BlockEntity rTileEntity = aWorld.getBlockEntity(tPos); // было WD.te(aWorld, x, y, z, T) — BlockGetter.java:25 / Level.java:671
			if (rTileEntity instanceof ITileEntityUnloadable && ((ITileEntityUnloadable)rTileEntity).isDead()) return null;
			if (rTileEntity != null) return rTileEntity;
			rTileEntity = LAST_BROKEN_TILEENTITY.get();
			// было .x/.y/.z — neo: BlockEntity.getBlockPos() (BlockEntity.java:232)
			if (rTileEntity != null && rTileEntity.getBlockPos().getX() == aX && rTileEntity.getBlockPos().getY() == aY && rTileEntity.getBlockPos().getZ() == aZ) return rTileEntity;
			Block tBlock = aWorld.getBlockState(tPos).getBlock(); // было aWorld.getBlock(x,y,z) — BlockGetter.java:32
			return tBlock instanceof IBlockTileEntity ? ((IBlockTileEntity)tBlock).getTileEntity(aWorld, aX, aY, aZ) : null;
		}
		return null;
	}
	/** F-world: read-only доступ (BlockGetter = бывш. IBlockAccess). Оригинал GT6 звал aWorld.getTileEntity(x,y,z)
	 *  напрямую на IBlockAccess (MultiTileEntityBlock.receiveDataX, BlockBaseFluid) — централизуем через WD.te,
	 *  как и Level-версия. Если это Level — делегируем в полную версию (chunk-load, 1:1 поведение); иначе плоское
	 *  чтение (BlockGetter не грузит чанки — это уже read-view, aLoadUnloadedChunks нечему грузить). */
	public static BlockEntity te(BlockGetter aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {
		if (aWorld instanceof Level) return te((Level)aWorld, aX, aY, aZ, aLoadUnloadedChunks);
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockEntity rTileEntity = aWorld.getBlockEntity(tPos);
		if (rTileEntity instanceof ITileEntityUnloadable && ((ITileEntityUnloadable)rTileEntity).isDead()) return null;
		if (rTileEntity != null) return rTileEntity;
		rTileEntity = LAST_BROKEN_TILEENTITY.get();
		if (rTileEntity != null && rTileEntity.getBlockPos().getX() == aX && rTileEntity.getBlockPos().getY() == aY && rTileEntity.getBlockPos().getZ() == aZ) return rTileEntity;
		Block tBlock = aWorld.getBlockState(tPos).getBlock();
		return tBlock instanceof IBlockTileEntity ? ((IBlockTileEntity)tBlock).getTileEntity(aWorld, aX, aY, aZ) : null;
	}
	/** F-world: 1.7.10 World.blockExists(x,y,z) = «чанк с этим блоком загружен». Порт централизовал вызовы как
	 *  WD.exists, но метод не был определён. neo-эквивалент — Level.isLoaded(BlockPos) (Level.java:695). */
	public static boolean exists(Level aWorld, int aX, int aY, int aZ) {
		return aWorld != null && aWorld.isLoaded(new BlockPos(aX, aY, aZ));
	}
	/** F-world: 1.7.10 World-небовидимость(x,y,z) -> neo canSeeSky(BlockPos) (BlockAndLightGetter.java:17). */
	public static boolean canSeeSky(Level aWorld, int aX, int aY, int aZ) {
		return aWorld != null && aWorld.canSeeSky(new BlockPos(aX, aY, aZ));
	}
	/** F-world: 1.7.10 WD.hardness(Block, world,x,y,z) -> neo BlockState.getDestroySpeed(BlockGetter,BlockPos)
	 *  (BlockBehaviour.java:636). Твёрдость конкретного блока — через его defaultBlockState. */
	public static float hardness(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		return aBlock.defaultBlockState().getDestroySpeed(aWorld, new BlockPos(aX, aY, aZ));
	}
	/** F-motion: 1.7.10 WD.motionX(Entity)/Y/Z (public поля) -> neo Vec3 getDeltaMovement()/setDeltaMovement (Entity.java).
	 *  Покомпонентная запись обязана сохранять две другие оси -> централизуем здесь ОДИН раз (философия §2). */
	public static double motionX(Entity aEntity) {return aEntity.getDeltaMovement().x;}
	public static double motionY(Entity aEntity) {return aEntity.getDeltaMovement().y;}
	public static double motionZ(Entity aEntity) {return aEntity.getDeltaMovement().z;}
	public static void setMotionX(Entity aEntity, double aX) {net.minecraft.world.phys.Vec3 v = aEntity.getDeltaMovement(); aEntity.setDeltaMovement(aX, v.y, v.z);}
	public static void setMotionY(Entity aEntity, double aY) {net.minecraft.world.phys.Vec3 v = aEntity.getDeltaMovement(); aEntity.setDeltaMovement(v.x, aY, v.z);}
	public static void setMotionZ(Entity aEntity, double aZ) {net.minecraft.world.phys.Vec3 v = aEntity.getDeltaMovement(); aEntity.setDeltaMovement(v.x, v.y, aZ);}
	/** F-render: 1.7.10 WD.opaque(Block) = «непрозрачный полный куб» -> neo BlockState.canOcclude()
	 *  (BlockBehaviour.java:658). Запрос по конкретному блоку — через его defaultBlockState. */
	public static boolean opaque(Block aBlock) {return aBlock.defaultBlockState().canOcclude();}
	/** F-harvest-event (decisions/): 1.7.10 {@code ForgeEventFactory.fireBlockHarvesting} фаерил HarvestDropsEvent —
	 *  внешние моды правили список дропа и шанс, метод возвращал шанс. neo: модель дропов = движко-fired
	 *  {@code BlockDropsEvent} при спавне через loot-систему, ПРЯМОГО EventHooks-эквивалента НЕТ (сверено
	 *  neoforge/event/EventHooks.java). GT6 спавнит дроп ВРУЧНУЮ ({@code WD.dropBlockAsItem}), минуя loot; no-op:
	 *  возвращаем {@code aDropChance} как есть, GT6-дроп сохранён 1:1. PORT-TODO(F-harvest-event): хук
	 *  внешне-модовой модификации дропа/шанса не подключён (нужен ре-дизайн на BlockDropsEvent). */
	public static float fireBlockHarvesting(java.util.List<ItemStack> aDrops, Level aWorld, Block aBlock, int aX, int aY, int aZ, int aMeta, int aFortune, float aDropChance, boolean aSilkTouch, Player aPlayer) {return aDropChance;}
	/** F-render: 1.7.10 Block-нормальный-куб = isOpaque && renderAsNormalBlock && !canProvidePower — ТОЧНО «redstone
	 *  conductor» (полный непрозрачный блок, не источник сигнала). neo BlockState.isRedstoneConductor(BlockGetter,
	 *  BlockPos) (BlockBehaviour.java:616) — канонический преемник (§8). */
	public static boolean normalCube(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		return aBlock.defaultBlockState().isRedstoneConductor(aWorld, new BlockPos(aX, aY, aZ));
	}
	/** F-dimension: 1.7.10 World-провайдер числовой id -> neo числового id НЕТ (Level.dimension() =
	 *  ResourceKey<Level>). Ванильные 1:1: overworld=0, nether=-1, end=1 (Level.java:95-97). PORT-TODO(F-dimension,
	 *  modded-dim-id): модовым измерениям стабильного int в neo нет -> hash ключа (уникален в рамках сессии, но
	 *  switch-кейсы GT6 всё равно только на ванильных 0/-1/1, модовые -> default; NBT-персист модового id деградирует). */
	public static int dimensionId(Level aWorld) {
		if (aWorld == null) return 0;
		net.minecraft.resources.ResourceKey<Level> tKey = aWorld.dimension();
		if (tKey == Level.OVERWORLD) return 0;
		if (tKey == Level.NETHER) return -1;
		if (tKey == Level.END) return 1;
		return tKey.identifier().hashCode(); // neo ResourceKey: location()->identifier() (ResourceKey.java:55)
	}
	/** F9: 1.7.10 WD.getMaterial(Block) удалён в neo (класс Material убран). GT6-блок (BlockBase) хранит портированный
	 *  gregapi.block.Material; для ВАНИЛЬНЫХ neo-блоков классифицируем по идентичности (критичные fluid/air/fire —
	 *  ТОЧНО, сверено с 1.7.10) + neo BlockTags (семьи logs/leaves/carpet — надёжнее ручного списка). Материалы —
	 *  портированные 1:1 gregapi.block.Material (список сверен с референсом). PORT-TODO(F9, material-table): непокрытые
	 *  блоки -> Material.rock (документированная деградация §10; критичные сравнения GT6 — water/lava/air/fire/wood/
	 *  leaves/sand/grass/ground/gourd/cactus/vine/clay/carpet — покрыты, редкие блоки классиф. как rock). */
	/** F-sound: 1.7.10 WD.playStepSound(aWorld, x, y, z, block) —
	 *  строковый sound-path + отдельный вызов. neo: SoundType через state.getSoundType() -> getStepSound() (SoundEvent),
	 *  Level.playSound(null,x,y,z,SoundEvent,SoundSource.BLOCKS,vol,pitch) (Level.java:444). Формула шага 1:1 (едина
	 *  у всех вызывателей: (vol+1)/2, pitch*0.8; `*0.5`≡`/2`). Центр берёт блок — SoundType из его defaultBlockState. */
	public static void playStepSound(Level aWorld, double aX, double aY, double aZ, Block aBlock) {
		net.minecraft.world.level.block.SoundType tSound = aBlock.defaultBlockState().getSoundType();
		aWorld.playSound(null, aX, aY, aZ, tSound.getStepSound(), net.minecraft.sounds.SoundSource.BLOCKS, (tSound.getVolume() + 1.0F) / 2.0F, tSound.getPitch() * 0.8F);
	}
	public static gregapi.block.Material getMaterial(Block aBlock) {
		if (aBlock instanceof BlockBase) return ((BlockBase)aBlock).getMaterial();
		net.minecraft.world.level.block.state.BlockState tState = aBlock.defaultBlockState();
		if (tState.isAir())                                                                                      return gregapi.block.Material.air;
		if (aBlock == Blocks.WATER || aBlock == Blocks.BUBBLE_COLUMN)                                            return gregapi.block.Material.water;
		if (aBlock == Blocks.LAVA)                                                                               return gregapi.block.Material.lava;
		if (aBlock == Blocks.FIRE || aBlock == Blocks.SOUL_FIRE)                                                 return gregapi.block.Material.fire;
		if (aBlock == Blocks.CACTUS)                                                                             return gregapi.block.Material.cactus;
		if (aBlock == Blocks.VINE)                                                                               return gregapi.block.Material.vine;
		if (aBlock == Blocks.CLAY)                                                                               return gregapi.block.Material.clay;
		if (aBlock == Blocks.MELON || aBlock == Blocks.PUMPKIN || aBlock == Blocks.CARVED_PUMPKIN || aBlock == Blocks.JACK_O_LANTERN) return gregapi.block.Material.gourd;
		if (aBlock == Blocks.GRASS_BLOCK || aBlock == Blocks.MYCELIUM || aBlock == Blocks.PODZOL)                return gregapi.block.Material.grass;
		if (aBlock == Blocks.PACKED_ICE)                                                                         return gregapi.block.Material.packedIce;
		if (aBlock == Blocks.ICE || aBlock == Blocks.BLUE_ICE || aBlock == Blocks.FROSTED_ICE)                   return gregapi.block.Material.ice;
		if (aBlock == Blocks.SNOW || aBlock == Blocks.SNOW_BLOCK || aBlock == Blocks.POWDER_SNOW)                return gregapi.block.Material.snow;
		if (aBlock == Blocks.COBWEB)                                                                             return gregapi.block.Material.web;
		if (aBlock == Blocks.TNT)                                                                                return gregapi.block.Material.tnt;
		if (tState.is(net.minecraft.tags.BlockTags.SAND))                                                        return gregapi.block.Material.sand;
		if (aBlock == Blocks.DIRT || aBlock == Blocks.COARSE_DIRT || aBlock == Blocks.GRAVEL || aBlock == Blocks.FARMLAND || aBlock == Blocks.DIRT_PATH || aBlock == Blocks.ROOTED_DIRT || aBlock == Blocks.SOUL_SAND || aBlock == Blocks.SOUL_SOIL) return gregapi.block.Material.ground;
		if (tState.is(net.minecraft.tags.BlockTags.LEAVES))                                                      return gregapi.block.Material.leaves;
		if (tState.is(net.minecraft.tags.BlockTags.LOGS) || tState.is(net.minecraft.tags.BlockTags.PLANKS) || aBlock == Blocks.CRAFTING_TABLE || aBlock == Blocks.BOOKSHELF || aBlock == Blocks.CHEST || aBlock == Blocks.JUKEBOX || aBlock == Blocks.NOTE_BLOCK) return gregapi.block.Material.wood;
		if (tState.is(net.minecraft.tags.BlockTags.WOOL_CARPETS))                                                return gregapi.block.Material.carpet;
		if (tState.is(net.minecraft.tags.BlockTags.WOOL))                                                        return gregapi.block.Material.cloth;
		if (tState.is(net.minecraft.tags.BlockTags.SAPLINGS) || tState.is(net.minecraft.tags.BlockTags.SMALL_FLOWERS) || tState.is(net.minecraft.tags.BlockTags.FLOWERS) || tState.is(net.minecraft.tags.BlockTags.CROPS) || aBlock == Blocks.SUGAR_CANE || aBlock == Blocks.SUNFLOWER) return gregapi.block.Material.plants;
		return gregapi.block.Material.rock;
	}

	/** F-block-behavior: 1.7.10 {@code Block.isReplaceable/isSideSolid/isReplaceableOreGen} удалены в neo
	 *  (нет ни в {@code Block.java}, ни в {@code BlockBehaviour.java} ни в одном из 3 корней референса). GT6-блоки
	 *  (BlockBase, MultiTileEntityBlock, BlockStones) определяют свои версии сами (компилируются как собственные
	 *  методы) — централизуем здесь ВЫЗОВЫ на приёмниках статического типа ванильный {@code Block}: instanceof-
	 *  диспетчер (виртуальный dispatch докручивает до реального override подкласса), иначе — 1.7.10 Forge-дефолт. */
	/** было {@code tBlock.isReplaceable(aWorld, aX, aY, aZ)} — 1.7.10 {@code Block.isReplaceable} дефолт =
	 *  {@code blockMaterial.isReplaceable()} (BlockBase его НЕ переопределяет, см. `gregtech6/.../BlockBase.java`,
	 *  использует материал), MultiTileEntityBlock переопределяет (TileEntity-делегирование). */
	public static boolean replaceable(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isReplaceable(aWorld, aX, aY, aZ);
		return getMaterial(aBlock).isReplaceable();
	}
	/** было {@code aBlock.isSideSolid(aWorld, aX, aY, aZ, aSide)} — BlockBase.java:95 переопределяет (и все его
	 *  подклассы через virtual dispatch), MultiTileEntityBlock.java:279 переопределяет (TileEntity-делегирование).
	 *  Ванильный neo-эквивалент дефолта — {@code BlockState.isFaceSturdy(BlockGetter,BlockPos,Direction)}
	 *  (BlockBehaviour.java:876). */
	public static boolean sideSolid(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {
		if (aBlock instanceof BlockBase) return ((BlockBase)aBlock).isSideSolid(aWorld, aX, aY, aZ, aSide);
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isSideSolid(aWorld, aX, aY, aZ, aSide);
		return aBlock.defaultBlockState().isFaceSturdy(aWorld, new BlockPos(aX, aY, aZ), aSide);
	}
	/** было {@code aBlock.isReplaceableOreGen(aWorld, aX, aY, aZ, aTarget)} — BlockBase его НЕ переопределяет
	 *  (сверено с `gregtech6/.../BlockBase.java`, дефолт), переопределяют только MultiTileEntityBlock.java:245
	 *  (TileEntity-делегирование) и BlockStones.java:746 (каменные руды/генерация). Ванильный Forge 1.7.10
	 *  {@code Block.isReplaceableOreGen} дефолт = identity ({@code this==target}). */
	public static boolean oreGen(Block aBlock, Level aWorld, int aX, int aY, int aZ, Block aTarget) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isReplaceableOreGen(aWorld, aX, aY, aZ, aTarget);
		if (aBlock instanceof BlockStones) return ((BlockStones)aBlock).isReplaceableOreGen(aWorld, aX, aY, aZ, aTarget);
		return aBlock == aTarget;
	}
	/** было {@code aBlock.isWood(aWorld,x,y,z)} (Forge block-behavior, удалён) — GT6-блоки (MTE/BlockBaseLog) переопределяют;
	 *  ванильный дефолт Forge = false, кроме брёвен -> neo BlockTags.LOGS (1.7.10 vanilla BlockLog.isWood=true). */
	public static boolean wood(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isWood(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.tree.BlockBaseLog) return ((gregapi.block.tree.BlockBaseLog)aBlock).isWood(aWorld, aX, aY, aZ);
		return aBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.LOGS);
	}
	/** было {@code aBlock.isLeaves(aWorld,x,y,z)} (Forge block-behavior, удалён) — GT6 (MTE/BlockBaseLeaves) переопределяют;
	 *  ванильный дефолт false, кроме листьев -> neo BlockTags.LEAVES. */
	public static boolean leaves(Block aBlock, BlockGetter aWorld, int aX, int aY, int aZ) {
		if (aBlock instanceof MultiTileEntityBlock) return ((MultiTileEntityBlock)aBlock).isLeaves(aWorld, aX, aY, aZ);
		if (aBlock instanceof gregapi.block.tree.BlockBaseLeaves) return ((gregapi.block.tree.BlockBaseLeaves)aBlock).isLeaves(aWorld, aX, aY, aZ);
		return aBlock.defaultBlockState().is(net.minecraft.tags.BlockTags.LEAVES);
	}

	public static byte WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD = 0;
	
	public static BlockEntity invalidateTileEntityWithNegativeYCoord(int aX, int aY, int aZ, BlockEntity aTileEntity) {
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD == 0) UT.Entities.chat(null, "Please provide the gregtech.log File to Greg, there was a weird Error");
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD < 10) {
			ERR.println("===============================");
			ERR.println("X:" + aX);
			ERR.println("Y:" + aY);
			ERR.println("Z:" + aZ);
			ERR.println("Class:" + aTileEntity.getClass());
			new Throwable().printStackTrace(ERR);
			ERR.println("===============================");
		}
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD == 9) UT.Entities.chat(null, "Please provide the gregtech.log File to Greg, there was a LOT of weird Errors");
		if (WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD < 99) WARN_ABOUT_TILEENTITY_NEGATIVE_Y_COORD++;
		aTileEntity.setRemoved(); // было .invalidate() — neo: BlockEntity.setRemoved() (BlockEntity.java:252)
		// PORT-TODO(WD, blockentity-position-immutable): было aTileEntity.y = 0 — neo BlockEntity.worldPosition
		// (BlockEntity.java:48) protected final, задаётся один раз конструктором (.java:57-59), сеттера нет ни в
		// одном из 3 корней референса — постфактум обнулить Y у уже созданной TileEntity недостижимо.
		return aTileEntity;
	}
	
	/** Sets the TileEntity at the passed position, with the option of turning adjacent TileEntity updates off. */
	public static BlockEntity te(Level aWorld, int aX, int aY, int aZ, BlockEntity aTileEntity, boolean aCauseTileEntityUpdates) {
		if (aY < 0) return invalidateTileEntityWithNegativeYCoord(aX, aY, aZ, aTileEntity);
		if (aCauseTileEntityUpdates) aWorld.setBlockEntity(aTileEntity); // было aWorld.setTileEntity(x,y,z,te) — neo: Level.setBlockEntity(BlockEntity) (Level.java:681, позиция берётся из te.getBlockPos())
		else {
			LevelChunk tChunk = aWorld.getChunk(aX >> 4, aZ >> 4); // было aWorld.getChunk(cx,cz) — Level.getChunk(int,int) (Level.java:202)
			if (tChunk != null) {
				// было aWorld.addTileEntity(te) отдельно — neo: addAndRegisterBlockEntity(te) (LevelChunk.java:400) УЖЕ
				// сам зовёт level.addFreshBlockEntities(List.of(be)) внутри (LevelChunk.java:408); отдельный вызов
				// addFreshBlockEntities здесь дублировал бы регистрацию — оригинал (gregtech6 WD.java:347-348)
				// добавлял в world-list один раз.
				tChunk.addAndRegisterBlockEntity(aTileEntity); // было tChunk.func_150812_a(x&15,y,z&15,te) — LevelChunk.addAndRegisterBlockEntity(BlockEntity) (LevelChunk.java:400), позиция берётся из te.getBlockPos()
				tChunk.markUnsaved(); // было tChunk.markUnsaved()
			}
		}
		return aTileEntity;
	}
	
	
	public static boolean oxygen(Level aWorld, int aX, int aY, int aZ) {
		return  !MD.GC.mLoaded || !dimGC(aWorld) || OxygenUtil.checkTorchHasOxygen(aWorld, NB, aX, aY, aZ); // F10: aWorld.provider instanceof IGalacticraftWorldProvider -> центр dimGC (WorldProvider удалён).
	}
	public static boolean collectable_air(Level aWorld, int aX, int aY, int aZ) {
		return (!MD.GC.mLoaded || !dimGC(aWorld)) && !hasCollide(aWorld, aX, aY, aZ) && !liquid(aWorld, aX, aY, aZ); // F10: aWorld.provider instanceof IGalacticraftWorldProvider -> центр dimGC.
	}
	
	/** @return the regular Environment Temperature of the World at this Location according to my calculations. In Kelvin, ofcourse. */
	public static long envTemp(Level aWorld, int aX, int aY, int aZ) {
		// было aWorld.getBiomeGenForCoords(x,z) (2D) — neo: LevelReader.getBiome(BlockPos) (LevelReader.java:42),
		// возвращает Holder<Biome>; .value() (Holder.java:17) разворачивает до Biome (сигнатура envTemp(Biome,...) не меняется).
		return envTemp(aWorld.getBiome(new BlockPos(aX, aY, aZ)).value(), aX, aY, aZ);
	}
	/** @return the regular Environment Temperature of the World at this Location according to my calculations. In Kelvin, ofcourse. */
	public static long envTemp(Biome aBiome, int aX, int aY, int aZ) {
		// было aBiome.getFloatTemperature(x,y,z) (позиция-скорректированная, удалено) -> getBaseTemperature() (Biome.java:247).
		// PORT-TODO(F6, biome-temp-elevation-modifier): elevation-охлаждение (climateSettings.temperatureModifier, приватно) не воспроизведено — база доминирует, парити-деталь.
		return Math.max(1, aBiome == null ? DEF_ENV_TEMP : (long)(C - 3 + aBiome.getBaseTemperature() * 20));
	}
	/** @return the regular Environment Temperature of the World at this Location according to my calculations. In Kelvin, ofcourse. */
	public static long envTemp(Biome aBiome) {
		return Math.max(1, aBiome == null ? DEF_ENV_TEMP : (long)(C - 3 + aBiome.getBaseTemperature() * 20));
	}
	// F6-центр biome/climate/light/precipitation (было World.getBiomeGenForCoords/getLightBrightness/getPrecipitationHeight + Biome.rainfall/temperature поля — удалены):
	/** было World.getBiomeGenForCoords(x,z) (2D, BiomeGenBase) -> Level.getBiome(BlockPos).value() (LevelReader:42, Holder.value()); 2D-форма берёт Y=getSeaLevel() (LevelReader:66) как поверхностный столбец. */
	public static Biome biome(Level aWorld, int aX, int aZ) {return aWorld == null ? null : aWorld.getBiome(new BlockPos(aX, aWorld.getSeaLevel(), aZ)).value();}
	public static Biome biome(Level aWorld, int aX, int aY, int aZ) {return aWorld == null ? null : aWorld.getBiome(new BlockPos(aX, aY, aZ)).value();}
	/** было Biome.rainfall (поле, удалено) -> Biome.getModifiedClimateSettings().downfall() (Biome.java:367 record ClimateSettings.downfall, :458 getModifiedClimateSettings). */
	public static float rainfall(Biome aBiome) {return aBiome == null ? 0 : aBiome.getModifiedClimateSettings().downfall();}
	/** было World.getLightBrightness(x,y,z) (float 0..1) -> LevelLightEngine.getRawBrightness(pos,0)/15 (LevelLightEngine.java:146, Level.getLightEngine() :375). */
	public static float lightBrightness(Level aWorld, int aX, int aY, int aZ) {return aWorld == null ? 0 : aWorld.getLightEngine().getRawBrightness(new BlockPos(aX, aY, aZ), 0) / 15.0F;}
	/** было World.getPrecipitationHeight(x,z) -> Level.getHeight(Heightmap.MOTION_BLOCKING,x,z) (Level.java:359, Heightmap:147). */
	public static int precipitationHeight(Level aWorld, int aX, int aZ) {return aWorld == null ? 0 : aWorld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, aX, aZ);}
	/** было Block.dropBlockAsItem(world,x,y,z,meta,fortune) (лут блока, удалён) -> Block.dropResources(state,level,pos) (Block.java:380).
	 *  PORT-TODO(F6/F13, drop-fortune): fortune-параметр не воспроизведён (dropResources берёт дефолтный лут; вызыватели fortune=0). */
	public static void dropBlockAsItem(Level aWorld, int aX, int aY, int aZ, int aMeta, int aFortune) {if (aWorld == null) return; BlockPos tPos = new BlockPos(aX, aY, aZ); Block.dropResources(aWorld.getBlockState(tPos), aWorld, tPos);}
	/** было Block.dropBlockAsItem(world,x,y,z,ItemStack) (конкретный стек) -> Block.popResource(level,pos,stack) (Block.java:407). */
	public static void dropBlockAsItem(Level aWorld, int aX, int aY, int aZ, ItemStack aStack) {if (aWorld != null && ST.valid(aStack)) Block.popResource(aWorld, new BlockPos(aX, aY, aZ), aStack);}
	/** было Block.getCollisionBoundingBoxFromPool(w,x,y,z) (world-space AABB или null) -> getCollisionShape(w,pos).bounds().move(x,y,z)
	 *  (VoxelShape.bounds:39/isEmpty:73, AABB.move:220); пустая форма -> null (1:1 с 1.7.10 «нет коллизии»). */
	public static AABB collisionBox(Level aWorld, int aX, int aY, int aZ, Block aBlock) {if (aWorld == null) return null; BlockPos tPos = new BlockPos(aX, aY, aZ); net.minecraft.world.phys.shapes.VoxelShape tShape = aWorld.getBlockState(tPos).getCollisionShape(aWorld, tPos); return tShape.isEmpty() ? null : tShape.bounds().move(aX, aY, aZ);}
	/** было World.checkNoEntityCollision(AABB) (нет сущностей в боксе) -> EntityGetter.getEntities(null,bb).isEmpty() (EntityGetter:29); null-бокс -> true (нет коллизии). */
	public static boolean noEntityCollision(Level aWorld, AABB aBox) {return aWorld == null || aBox == null || aWorld.getEntities((Entity)null, aBox).isEmpty();}
	public static boolean noEntityCollision(Level aWorld, AABB aBox, Entity aExcept) {return aWorld == null || aBox == null || aWorld.getEntities(aExcept, aBox).isEmpty();}
	
	// F6: было `WorldProvider aProvider`-перегрузки ПАРАЛЛЕЛЬНО с `Level aWorld`-перегрузками (вызов через
	// `aWorld.provider`) — та же болезнь, что у семейства `dimXXX` выше: `WorldProvider` в neo удалён, компилятор
	// не мог выбрать между `waterLevel(Level)`/`waterLevel(WorldProvider)` (ambiguous). Слиты в один вход
	// `waterLevel(Level, int)`; `dimensionId == DIM_OVERWORLD` -> `Level.dimension() == Level.OVERWORLD`,
	// `hasNoSky` -> `!dimensionType().hasSkyLight()` (см. `decisions/README.md` «Dimension-identity»).
	/** @return the Height of the Water Level that should probably be in this World. */
	public static int waterLevel(Level aWorld) {
		return waterLevel(aWorld, 62);
	}
	/** @return the Height of the Water Level that should probably be in this World. */
	public static int waterLevel(Level aWorld, int aDefaultOverworld) {
		return aWorld.dimension() == Level.OVERWORLD ? waterLevel(aDefaultOverworld) : !aWorld.dimensionType().hasSkyLight() || dimTF(aWorld) ? 31 : 62;
	}
	/** @return the Height of the Water Level that should probably be in the Overworld. */
	public static int waterLevel(int aDefaultOverworld) {
		return MD.TFC.mLoaded || MD.TFCP.mLoaded? 143 : aDefaultOverworld;
	}
	/** @return the Height of the Water Level that should probably be in the Overworld. */
	public static int waterLevel() {
		return waterLevel(62);
	}
	
	/** @return the regular Temperature of the World at this Location according to Gregs calculations. In Kelvin, ofcourse. */
	public static long temperature(Level aWorld, int aX, int aY, int aZ) {
		long rTemperature = envTemp(aWorld, aX, aY, aZ);
		if (burning(aWorld, aX, aY, aZ)) rTemperature = Math.max(rTemperature, C + 200);
		for (BlockPos tCoords : new BlockPos[] {new BlockPos(aX, aY, aZ), new BlockPos(aX+1, aY, aZ), new BlockPos(aX-1, aY, aZ), new BlockPos(aX, aY+1, aZ), new BlockPos(aX, aY-1, aZ), new BlockPos(aX, aY, aZ+1), new BlockPos(aX, aY, aZ-1)}) {
			Block tBlock = block(aWorld, tCoords.getX(), tCoords.getY(), tCoords.getZ(), F);
			if (tBlock == Blocks.LAVA || tBlock == Blocks.LAVA) rTemperature = Math.max(rTemperature, C + 500);
			else if (tBlock instanceof FireBlock) rTemperature = Math.max(rTemperature, C + 200);
		}
		return rTemperature;
	}
	
	public static ItemStack stack(Level aWorld, int aX, int aY, int aZ) {
		Block tBlock = aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock(); // было aWorld.getBlock(x,y,z)
		// было aWorld.getBlockMetadata(x,y,z) в ветке else — числовой меты в neo больше нет (МОДЕЛЬ МЕТЫ п.4):
		// для ванильных блоков (не IBlockExtendedMetaData) возвращаем 0, не выдумывая числовую таблицу.
		return ST.make(tBlock, 1, tBlock instanceof IBlockExtendedMetaData ? ((IBlockExtendedMetaData)tBlock).getExtendedMetaData(aWorld, aX, aY, aZ) : 0);
	}

	public static void update(BlockGetter aWorld, int aX, int aY, int aZ) {
		// было ((Level)aWorld).markBlockForUpdate(x,y,z) — neo: Level.sendBlockUpdated(pos,old,new,flags)
		// (Level.java:333); старое/новое состояние не отслеживались раздельно, тот же приём уже применён в
		// GT_API_Proxy.java:1316 (getBlockState дважды, flags=3=UPDATE_ALL).
		BlockPos tUpdPos = new BlockPos(aX, aY, aZ);
		BlockState tUpdState = ((Level)aWorld).getBlockState(tUpdPos);
		((Level)aWorld).sendBlockUpdated(tUpdPos, tUpdState, tUpdState, 3);
		if (CLIENT_BLOCKUPDATE_SOUNDS && CODE_CLIENT && CLIENT_TIME > 100) {
			Player tPlayer = GT_API.api_proxy.getThePlayer();
			if (tPlayer != null && Math.abs(tPlayer.getX() - aX) < 16 && Math.abs(tPlayer.getY() - aY) < 16 && Math.abs(tPlayer.getZ() - aZ) < 16) {
				UT.Sounds.play(SFX.MC_FIREWORK_LAUNCH, 1, 1.0F, 1.0F, aX, aY, aZ);
			}
		}
	}
	
	// было aWorld.getBlock(x,y,z) — neo: BlockGetter.getBlockState(BlockPos).getBlock() (BlockGetter.java:32); было
	// WD.exists(aWorld, x, y, z) — Level.isLoaded(BlockPos) (Level.java:695).
	public static Block block(BlockGetter aWorld, int aX, int aY, int aZ) {return aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock();}
	public static Block block(Level        aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {BlockPos tP = new BlockPos(aX, aY, aZ); return aLoadUnloadedChunks || aWorld.isLoaded(tP) ? aWorld.getBlockState(tP).getBlock() : NB;}
	public static Block block(Level        aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {return block(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide], aLoadUnloadedChunks);}
	public static Block block(Level        aWorld, int aX, int aY, int aZ, byte aSide) {return block(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]);}
	// МОДЕЛЬ МЕТЫ п.4: числовой меты в neo больше нет — для IBlockExtendedMetaData (свои блоки, п.1) реальное
	// значение, иначе 0 (не выдумываем числовую таблицу для ванильных блоков).
	public static byte  meta (BlockGetter aWorld, int aX, int aY, int aZ) {Block tB = aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock(); return UT.Code.bind4(tB instanceof IBlockExtendedMetaData ? ((IBlockExtendedMetaData)tB).getExtendedMetaData(aWorld, aX, aY, aZ) : 0);}
	public static byte  meta (Level        aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {return aLoadUnloadedChunks || aWorld.isLoaded(new BlockPos(aX, aY, aZ)) ? meta((BlockGetter)aWorld, aX, aY, aZ) : 0;}
	public static byte  meta (Level        aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {return meta(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide], aLoadUnloadedChunks);}
	public static byte  meta (Level        aWorld, int aX, int aY, int aZ, byte aSide) {return meta(aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]);}
	public static byte  meta (long aBitAnd, BlockGetter aWorld, int aX, int aY, int aZ) {return UT.Code.bind4(meta(aWorld, aX, aY, aZ) & aBitAnd);}
	public static byte  meta (long aBitAnd, Level        aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {return aLoadUnloadedChunks || aWorld.isLoaded(new BlockPos(aX, aY, aZ)) ? UT.Code.bind4(meta((BlockGetter)aWorld, aX, aY, aZ) & aBitAnd) : 0;}
	public static byte  meta (long aBitAnd, Level        aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {return meta(aBitAnd, aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide], aLoadUnloadedChunks);}
	public static byte  meta (long aBitAnd, Level        aWorld, int aX, int aY, int aZ, byte aSide) {return meta(aBitAnd, aWorld, aX+OFFX[aSide], aY+OFFY[aSide], aZ+OFFZ[aSide]);}
	
	public static boolean set(Level aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta, long aFlags) {
		return set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags, WD.opaque(aBlock));
	}
	
	public static boolean set(Level aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta, long aFlags, boolean aRemoveGrassBelow) {
		if (aRemoveGrassBelow) {
			Block tBlock = aWorld.getBlockState(new BlockPos(aX, aY-1, aZ)).getBlock(); // было aWorld.getBlock(x,y-1,z)
			if (tBlock == Blocks.GRASS_BLOCK || tBlock == Blocks.MYCELIUM) aWorld.setBlock(new BlockPos(aX, aY-1, aZ), Blocks.DIRT.defaultBlockState(), (int)aFlags); // было aWorld.setBlock(x,y-1,z,Blocks.DIRT,0,flags)
		}
		// было aWorld.setBlock(x,y,z,block,meta,flags) — neo: LevelWriter.setBlock(BlockPos,BlockState,flags) (LevelWriter.java:10).
		// Числовой меты у BlockState нет (МОДЕЛЬ МЕТЫ п.1/4): для своих блоков (IBlockExtendedMetaData) — канал
		// setExtendedMetaData сохранён "как есть" после установки блока; для ванильных aMeta теряется (форс движка).
		BlockPos tSetPos = new BlockPos(aX, aY, aZ);
		boolean rSet = aWorld.setBlock(tSetPos, aBlock.defaultBlockState(), (int)aFlags);
		if (aBlock instanceof IBlockExtendedMetaData) {
			byte tNewMeta = Code.bind4(aMeta);
			// мета — отдельный канал; но setter даёт side-effects (WD.te/WD.update), потому — только при РЕАЛЬНОМ отличии
			// (оригинал Chunk.java:623-625 при совпадении block И meta возвращал false без мутации):
			if (((IBlockExtendedMetaData)aBlock).getExtendedMetaData(aWorld, aX, aY, aZ) != tNewMeta) {
				((IBlockExtendedMetaData)aBlock).setExtendedMetaData(aWorld, aX, aY, aZ, tNewMeta);
				rSet = true;
			}
		}
		return rSet;
	}

	public static boolean set(LevelChunk aChunk, int aX, int aY, int aZ, Block aBlock, long aMeta) {
		// было aChunk.func_150807_a(localX,y,localZ,block,meta) — neo: LevelChunk.setBlockState(BlockPos,BlockState,flags)
		// (LevelChunk.java:270) хочет МИРОВОЙ BlockPos (маскирует &15 внутри себя, используя абсолютные координаты
		// для heightmap/light engine) — ChunkPos.getBlockAt(localX,y,localZ) (ChunkPos.java:151) переводит локальные
		// координаты чанка в мировые, сохраняя тот же вызывающий контракт (локальные x/z 0-15).
		BlockPos tChunkSetPos = aChunk.getPos().getBlockAt(aX, aY, aZ);
		boolean rSet = aChunk.setBlockState(tChunkSetPos, aBlock.defaultBlockState(), Block.UPDATE_ALL) != null;
		if (aBlock instanceof IBlockExtendedMetaData) {
			byte tNewMeta = Code.bind4(aMeta);
			if (((IBlockExtendedMetaData)aBlock).getExtendedMetaData(aChunk.getLevel(), tChunkSetPos.getX(), tChunkSetPos.getY(), tChunkSetPos.getZ()) != tNewMeta) {
				((IBlockExtendedMetaData)aBlock).setExtendedMetaData(aChunk.getLevel(), tChunkSetPos.getX(), tChunkSetPos.getY(), tChunkSetPos.getZ(), tNewMeta);
				rSet = true;
			}
		}
		return rSet;
	}
	public static boolean set(LevelChunk aChunk, int aX, int aY, int aZ, Block aBlock, long aMeta, boolean aRemoveGrassBelow) {
		if (aRemoveGrassBelow) {
			Block tBlock = aChunk.getBlockState(aChunk.getPos().getBlockAt(aX, aY-1, aZ)).getBlock(); // было aChunk.getBlock(x,y-1,z)
			if (tBlock == Blocks.GRASS_BLOCK || tBlock == Blocks.MYCELIUM) aChunk.setBlockState(aChunk.getPos().getBlockAt(aX, aY-1, aZ), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL); // было aChunk.func_150807_a(x,y-1,z,Blocks.DIRT,0)
		}
		return set(aChunk, aX, aY, aZ, aBlock, aMeta);
	}

	public static boolean replace(Level aWorld, int aX, int aY, int aZ, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		if (aTargetBlock == null || aReplaceBlock == null) return F;
		if (aReplaceBlock != block(aWorld, aX, aY, aZ)) return F;
		if (aReplaceMeta != W && aReplaceMeta != meta(aWorld, aX, aY, aZ)) return F;
		return set(aWorld, aX, aY, aZ, aTargetBlock, aTargetMeta, Block.UPDATE_CLIENTS, F); // было aWorld.setBlock(x,y,z,block,meta,2) — флаг 2=UPDATE_CLIENTS (Block.java:91-104); маршрут через центр set(...) — мета своих блоков (IBlockExtendedMetaData) не теряется
	}
	public static boolean replace(Level aWorld, BlockPos aCoords, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		return replace(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta);
	}
	public static boolean replaceAll(Level aWorld, int aX, int aY, int aZ, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		return replaceAll(aWorld, new BlockPos(aX, aY, aZ), aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta);
	}
	public static boolean replaceAll(Level aWorld, BlockPos aCoords, Block aReplaceBlock, long aReplaceMeta, Block aTargetBlock, long aTargetMeta) {
		if (!replace(aWorld, aCoords, aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta)) return F;
		HashSetNoNulls<BlockPos> tSwap,
		tDone  = new HashSetNoNulls<>(F, aCoords),
		tCheck = new HashSetNoNulls<>(F, aCoords),
		tNext  = new HashSetNoNulls<>();
		
		while (!tCheck.isEmpty() && tDone.size() < 32768) {
			tNext.clear();
			for (BlockPos tChecking : tCheck) {
				if (Math.abs(tChecking.getX() - aCoords.getX()) < 128 && Math.abs(tChecking.getZ() - aCoords.getZ()) < 128) for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
					BlockPos tCoords = new BlockPos(tChecking.getX()+i, tChecking.getY()+j, tChecking.getZ()+k);
					if (tDone.add(tCoords) && replace(aWorld, tCoords, aReplaceBlock, aReplaceMeta, aTargetBlock, aTargetMeta)) tNext.add(tCoords);
				}
			}
			tSwap = tNext; tNext = tCheck; tCheck = tSwap;
		}
		return T;
	}
	
	public static boolean sign(Level aWorld, int aX, int aY, int aZ, byte aSide, long aFlags, String aLine1, String aLine2, String aLine3, String aLine4) {
		// было aWorld.setBlock(x,y,z,Blocks.OAK_WALL_SIGN,aSide,flags) — aSide был прямой мета-ориентацией wall_sign
		// (2-5); neo: WallSignBlock.FACING (EnumProperty<Direction>, WallSignBlock.java:30) через уже
		// централизованный FORGE_DIR[side]->Direction (тот же массив, что используется по всему файлу).
		aWorld.setBlock(new BlockPos(aX, aY, aZ), Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, FORGE_DIR[aSide]), (int)aFlags);
		BlockEntity tSign = te(aWorld, aX, aY, aZ, T);
		if (!(tSign instanceof SignBlockEntity)) return F;
		// было signText[0..3]=String (1.7.10 мутабельный массив строк) -> neo SignText immutable (front/back):
		// getFrontText():77 -> цепочка setMessage(index,Component):84 (возвращает новый SignText) -> setText(SignText,isFront):161.
		((SignBlockEntity)tSign).setText(((SignBlockEntity)tSign).getFrontText()
			.setMessage(0, net.minecraft.network.chat.Component.literal(aLine1))
			.setMessage(1, net.minecraft.network.chat.Component.literal(aLine2))
			.setMessage(2, net.minecraft.network.chat.Component.literal(aLine3))
			.setMessage(3, net.minecraft.network.chat.Component.literal(aLine4)), T);
		return T;
	}
	
	/** F-worldgen: 1.7.10 {@code World.getSeed()} -> neo только {@code ServerLevel.getSeed()}:1697 (у базового Level
	 *  сида нет). Детерм.-per-chunk random — worldgen (сервер), где aWorld всегда ServerLevel; клиент (нет worldgen) -> 0. */
	public static long seed(Level aWorld) {return aWorld instanceof net.minecraft.server.level.ServerLevel tSL ? tSL.getSeed() : 0L;}
	public static Random random(Level aWorld, long aChunkX, long aChunkZ) {return random(seed(aWorld) ^ WD.dimensionId(aWorld), aChunkX >> 4, aChunkZ >> 4);}
	public static Random random(long aSeed, long aChunkX, long aChunkZ) {
		// Seed is XOR-ed with the Dimension ID to prevent multiple Dimensions from being identical in Ore Generation.
		// Yes that actually happened with Aromas Mining World, and resulted in a prospecting exploit.
		Random rRandom = new Random(aSeed);
		// Javas Random sucks so bad, the first few results are to be discarded
		for (int i = 0; i < 50; i++) rRandom.nextInt(0x00ffffff);
		// And then I use the first Result as a Seed for a second Random because it is THAT bad!
		rRandom = new Random(aSeed ^ ((rRandom.nextLong() >> 2 + 1L) * aChunkX + (rRandom.nextLong() >> 2 + 1L) * aChunkZ));
		// Javas Random still sucks badly, discarding some results again.
		for (int i = 0; i < 50; i++) rRandom.nextInt(0x00ffffff);
		// There we have it, a somewhat working Random function that is actually random
		// and does not cause my Code to generate almost perfect Diagonal Lines of Ores.
		return rRandom;
	}
	
	public static int random(Level aWorld, int aX, int aY, int aZ, int aBound) {return random(seed(aWorld) ^ WD.dimensionId(aWorld), aX, aY, aZ, aBound);}
	public static int random(long aSeed, int aX, int aY, int aZ, int aBound) {
		Random rRandom = new Random(aSeed ^ aY);
		for (int i = 0; i < 10; i++) rRandom.nextInt(0x00ffffff);
		rRandom = new Random(aSeed ^ ((rRandom.nextLong() >> 2 + 1L) * aX + (rRandom.nextLong() >> 2 + 1L) * aZ));
		for (int i = 0; i < 10; i++) rRandom.nextInt(0x00ffffff);
		return rRandom.nextInt(aBound);
	}
	
	public static Random random(BlockEntity aTileEntity) {return new Random(aTileEntity.getBlockPos().getX() ^ aTileEntity.getBlockPos().getY() ^ aTileEntity.getBlockPos().getZ());} // было .x/.y/.z — BlockEntity.getBlockPos() (BlockEntity.java:232)
	public static int random(BlockEntity aTileEntity, int aBound) {return random(aTileEntity).nextInt(aBound);}
	public static boolean random(BlockEntity aTileEntity, int aBound, long aTime) {return random(aTileEntity, aBound) == aTime % aBound;}
	
	public static boolean border(int aFromX, int aFromZ, int aToX, int aToZ) {return aFromX >> 4 != aToX >> 4 || aFromZ >> 4 != aToZ >> 4;}
	
	public static boolean even(BlockEntity aTileEntity) {return even(aTileEntity.getBlockPos().getX(), aTileEntity.getBlockPos().getY(), aTileEntity.getBlockPos().getZ());} // было .x/.y/.z
	public static boolean even(BlockPos aCoords) {return even(aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	public static boolean even(int... aCoords) {int i = 0; for (int tCoord : aCoords) if (tCoord % 2 == 0) i++; return i % 2 == 0;}
	
	public static int evenness(BlockEntity aTileEntity) {return evenness(aTileEntity.getBlockPos().getX(), aTileEntity.getBlockPos().getY(), aTileEntity.getBlockPos().getZ());} // было .x/.y/.z
	public static int evenness(BlockPos aCoords) {return evenness(aCoords.getX(), aCoords.getY(), aCoords.getZ());}
	public static int evenness(int... aCoords) {int i = 0; for (int tCoord : aCoords) {i <<= 1; if (tCoord % 2 != 0) i++;} return i;}
	
	// было aWorld.getBlock(x,y,z)/getBlockMetadata(x,y,z)/setBlock(x,y,z,block,meta,flags) — meta через централизованный meta(...)
	public static boolean setIfDiff(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMeta, int aFlags) {return (block(aWorld, aX, aY, aZ) != aBlock || meta(aWorld, aX, aY, aZ) != aMeta) && set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags, F);} // было aWorld.setBlock(x,y,z,block,meta,flags) — маршрут через центр set(...)

	public static boolean set(Level aWorld, int aX, int aY, int aZ, ItemStack aStack) {
		Block tBlock = ST.block(aStack);
		if (tBlock == NB) return F;
		if (tBlock instanceof IBlockPlacable) return ((IBlockPlacable)tBlock).placeBlock(aWorld, aX, aY, aZ, (byte)6, ST.meta_(aStack), ItemNBT.get(aStack), T, F);
		if (ST.meta_(aStack) < 16) return set(aWorld, aX, aY, aZ, tBlock, ST.meta_(aStack), Block.UPDATE_ALL, F); // было aWorld.setBlock(x,y,z,block,meta,3) — флаг 3=UPDATE_ALL; маршрут через центр set(...)
		return F;
	}

	public static boolean leafdecay(Level aWorld, int aX, int aY, int aZ, Block aBlock) {return leafdecay(aWorld, aX, aY, aZ, aBlock, F, F);}
	public static boolean leafdecay(Level aWorld, int aX, int aY, int aZ, Block aBlock, boolean aOnlyTopArea) {return leafdecay(aWorld, aX, aY, aZ, aBlock, aOnlyTopArea, F);}
	public static boolean leafdecay(Level aWorld, int aX, int aY, int aZ, Block aBlock, boolean aOnlyTopArea, boolean aTreeCapitator) {
		// F-tree: Forge Block.canSustainLeaves (блок держит листву от распада — брёвна) удалён -> neo тег
		// BlockTags.LOGS (BlockTags.java:38; leaf-decay в neo смотрит именно логи), проверка на состоянии.
		if (aBlock == null || aWorld.getBlockState(new BlockPos(aX, aY, aZ)).is(net.minecraft.tags.BlockTags.LOGS)) {
			for (int j = (aOnlyTopArea ? 0 : -7); j <= 7; ++j) for (int i = -7; i <= 7; ++i) for (int k = -7; k <= 7; ++k) {
				Block tBlock = aWorld.getBlockState(new BlockPos(aX+i, aY+j, aZ+k)).getBlock(); // было aWorld.getBlock(x+i,y+j,z+k)
				if (tBlock != NB) {
					if (tBlock == Blocks.BROWN_MUSHROOM_BLOCK || tBlock == Blocks.RED_MUSHROOM_BLOCK) {
						if (aTreeCapitator && Math.abs(i) <= 4 && Math.abs(k) <= 4 && j <= 0 && j >= -2) aWorld.destroyBlock(new BlockPos(aX+i, aY+j, aZ+k), T); // было aWorld.func_147480_a(x,y,z,drop) — LevelWriter.destroyBlock(BlockPos,boolean) (LevelWriter.java:18)
					} else if (IL.NeLi_Wart_Block_Crimson.equal(tBlock) || IL.NeLi_ShroomLight.equal(tBlock)) {
						if (aTreeCapitator && Math.abs(i) <= 4 && Math.abs(k) <= 4) aWorld.destroyBlock(new BlockPos(aX+i, aY+j, aZ+k), T); // было aWorld.func_147480_a(x,y,z,drop)
					} else {
						if (WD.leaves(tBlock, aWorld, aX+i, aY+j, aZ+k)) aWorld.scheduleTick(new BlockPos(aX+i, aY+j, aZ+k), tBlock, 1+RNGSUS.nextInt(100)); // было aWorld.scheduleTick(new BlockPos(x, y, z), block, delay) — ScheduledTickAccess.scheduleTick(BlockPos,Block,int) (ScheduledTickAccess.java:21)
					}
				}
			}
			return T;
		}
		return F;
	}
	
	public static boolean liquid(Level aWorld, int aX, int aY, int aZ) {return liquid(aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean liquid(Block aBlock) {return aBlock instanceof LiquidBlock || aBlock instanceof IFluidBlock;}

	public static boolean liquid_classic(Level aWorld, int aX, int aY, int aZ) {return liquid_classic(aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	// F5: Forge net.minecraftforge.fluids.BlockFluidClassic удалён — модовые «классические» (бесконечный
	// источник) жидкости в neo наследуют LiquidBlock (как ваниль). Проверки LiquidBlock достаточно 1:1.
	public static boolean liquid_classic(Block aBlock) {return aBlock instanceof LiquidBlock;}

	public static boolean liquid_finite(Level aWorld, int aX, int aY, int aZ) {return liquid_finite(aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	// PORT-TODO(F5, finite-fluid): Forge net.minecraftforge.fluids.BlockFluidFinite (жидкости с конечным
	// объёмом на блок) удалён, у neo модели «конечной» жидкости-блока нет (все LiquidBlock-стиль/бесконечные).
	// Деградация до F (ни один блок не «finite» в модели neo) — НЕ тихо, до появления neo-аналога.
	public static boolean liquid_finite(Block aBlock) {return F;}

	public static boolean liquid_borken(Level aWorld, int aX, int aY, int aZ) {return liquid_borken(aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean liquid_borken(Block aBlock) {return !(aBlock instanceof IItemGT) && liquid_classic(aBlock);}
	
	public static boolean stone(Block aBlock, short aMeta) {
		if (aBlock == NB) return F;
		if (aBlock == Blocks.OBSIDIAN) return T;
		ItemStackContainer tStack = new ItemStackContainer(aBlock, 1, aMeta);
		return BlocksGT.stoneToNormalOres.containsKey(tStack) || BlocksGT.stoneToBrokenOres.containsKey(tStack) || BlocksGT.stoneToSmallOres.containsKey(tStack);
	}
	
	public static boolean floor(Level aWorld, int aX, int aY, int aZ) {return floor(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean floor(Level aWorld, int aX, int aY, int aZ, Block aBlock) {return WD.sideSolid(aBlock, aWorld, aX, aY, aZ, FORGE_DIR[SIDE_UP]) || floor(aBlock);}
	public static boolean floor(Block aBlock) {return WD.opaque(aBlock) || aBlock instanceof SlabBlock || aBlock instanceof StairBlock || aBlock instanceof BlockMetaType;}
	
	@SuppressWarnings("unlikely-arg-type")
	public static boolean ore(Block aBlock, short aMeta) {return (aBlock instanceof IBlockPlacable && (BlocksGT.stoneToBrokenOres.containsValue(aBlock) || BlocksGT.stoneToNormalOres.containsValue(aBlock) || BlocksGT.stoneToSmallOres.containsValue(aBlock)) || OM.prefixcontains(ST.make(aBlock, 1, aMeta), TD.Prefix.ORE));}
	public static boolean ore_stone(Block aBlock, short aMeta) {return ore(aBlock, aMeta) || stone(aBlock, aMeta);}
	
	public static boolean visOcc(Level aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {return visOpq(aWorld, aX+1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX+1, aZ), aDefault) && visOpq(aWorld, aX-1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX-1, aZ), aDefault) && visOpq(aWorld, aX, aY+1, aZ, T, aDefault) && visOpq(aWorld, aX, aY-1, aZ, T, aDefault) && visOpq(aWorld, aX, aY, aZ+1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ+1), aDefault) && visOpq(aWorld, aX, aY, aZ-1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ-1), aDefault);}
	public static boolean visOpq(Level aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {BlockPos tP = new BlockPos(aX, aY, aZ); return aLoadUnloadedChunks || aWorld.isLoaded(tP) ? visOpq(aWorld.getBlockState(tP).getBlock()) : aDefault;} // было blockExists/getBlock(x,y,z)
	public static boolean visOpq(Block aBlock) {return WD.opaque(aBlock) || VISUALLY_OPAQUE_BLOCKS.contains(aBlock);}
	
	public static boolean occ(Level aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {return opq(aWorld, aX+1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX+1, aZ), aDefault) && opq(aWorld, aX-1, aY, aZ, aLoadUnloadedChunks || !border(aX, aZ, aX-1, aZ), aDefault) && opq(aWorld, aX, aY+1, aZ, T, aDefault) && opq(aWorld, aX, aY-1, aZ, T, aDefault) && opq(aWorld, aX, aY, aZ+1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ+1), aDefault) && opq(aWorld, aX, aY, aZ-1, aLoadUnloadedChunks || !border(aX, aZ, aX, aZ-1), aDefault);}
	public static boolean opq(Level aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks, boolean aDefault) {BlockPos tP = new BlockPos(aX, aY, aZ); return aLoadUnloadedChunks || aWorld.isLoaded(tP) ? opq(aWorld.getBlockState(tP).getBlock()) : aDefault;} // было blockExists/getBlock(x,y,z)
	public static boolean opq(Block aBlock) {return WD.opaque(aBlock) && !(aBlock instanceof LeavesBlock);}
	
	public static boolean air(Level aWorld, int aX, int aY, int aZ) {return air(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean air(Level aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock == NB || (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).isAir() && !(MD.TC.mLoaded && !WD.opaque(aBlock) && te(aWorld, aX, aY, aZ, T) instanceof INode));} // было aBlock.isAir(world,x,y,z) — BlockBehaviour.java:575 state.isAir()
	public static boolean air(Block aBlock) {return aBlock == NB;}
	
	public static boolean lava(BlockGetter aWorld, int aX, int aY, int aZ) {return lava(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean lava(BlockGetter aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock == Blocks.LAVA || aBlock == Blocks.LAVA;}
	public static boolean lava(Block aBlock) {return aBlock == Blocks.LAVA || aBlock == Blocks.LAVA;}
	
	public static boolean water(BlockGetter aWorld, int aX, int aY, int aZ) {return water(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean water(BlockGetter aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock == Blocks.WATER || aBlock == Blocks.WATER;}
	public static boolean water(Block aBlock) {return aBlock == Blocks.WATER || aBlock == Blocks.WATER;}
	
	public static boolean waterstream(Block aBlock) {return MD.Streams.mLoaded && UT.Code.stringValidate(ST.regName(aBlock)).startsWith("streams:river/tile.water");}
	
	public static boolean anywater(BlockGetter aWorld, int aX, int aY, int aZ) {return anywater(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean anywater(BlockGetter aWorld, int aX, int aY, int aZ, Block aBlock) {return aBlock instanceof BlockWaterlike || water(aWorld, aX, aY, aZ, aBlock) || waterstream(aBlock);}
	public static boolean anywater(Block aBlock) {return aBlock instanceof BlockWaterlike || water(aBlock) || waterstream(aBlock);}
	
	public static boolean bedrock(Level aWorld, int aX, int aY, int aZ) {return bedrock(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean bedrock(Level aWorld, int aX, int aY, int aZ, Block aBlock) {return bedrock(aBlock);}
	public static boolean bedrock(Block aBlock) {return aBlock == Blocks.BEDROCK || IL.BTL_Bedrock.equal(aBlock);}
	
	public static boolean grass(Level aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {return grass(block(aWorld, aX, aY, aZ, aLoadUnloadedChunks), meta(aWorld, aX, aY, aZ, aLoadUnloadedChunks));}
	public static boolean grass(Level aWorld, int aX, int aY, int aZ) {return grass(block(aWorld, aX, aY, aZ), meta(aWorld, aX, aY, aZ));}
	public static boolean grass(Level aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta) {return grass(aBlock, aMeta);}
	public static boolean grass(Block aBlock, long aMeta) {
		if (aBlock == Blocks.DEAD_BUSH) return T;
		if (aBlock == Blocks.SUNFLOWER)  return aMeta ==  2 || aMeta ==  3;
		if (IL.TF_Tall_Grass.equal(aBlock)) return aMeta ==  8 || aMeta == 10;
		return IL.AETHER_Tall_Grass.equal(aBlock);
	}
	
	public static boolean irrelevant(Level aWorld, int aX, int aY, int aZ) {return irrelevant(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean irrelevant(Level aWorld, int aX, int aY, int aZ, Block aBlock) {return air(aWorld, aX, aY, aZ, aBlock) || aBlock == Blocks.VINE || aBlock == Blocks.SNOW || aBlock == Blocks.FIRE || grass(aWorld, aX, aY, aZ) || anywater(aBlock);}
	
	public static boolean easyRep(Level aWorld, int aX, int aY, int aZ) {return easyRep(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean easyRep(Level aWorld, int aX, int aY, int aZ, Block aBlock) {return air(aWorld, aX, aY, aZ, aBlock) || aBlock instanceof BushBlock || aBlock instanceof SnowLayerBlock || aBlock instanceof FireBlock || WD.leaves(aBlock, aWorld, aX, aY, aZ) || aWorld.getBlockState(new BlockPos(aX, aY, aZ)).canBeReplaced();}
	
	// было aWorld.getBiomeGenForCoords(x,z) — LevelReader.getBiome(BlockPos) (LevelReader.java:42); F6-центр
	// BiomeNameSet.contains(Holder<Biome>) резолвит идентичность сам (unwrapKey().identifier()), сырой
	// .value().biomeName (мёртвое 1.7.10-поле) больше не нужен — gregapi/code/BiomeNameSet.java.
	public static boolean infiniteWater(Level aWorld, int aX, int aY, int aZ              ) {int tLevel = waterLevel(aWorld); return                                                                                       UT.Code.inside(tLevel-15, tLevel, aY) && BIOMES_RIVER_LAKE.contains(aWorld.getBiome(new BlockPos(aX, aY, aZ)));}
	public static boolean infiniteWater(Level aWorld, int aX, int aY, int aZ, Block aBlock) {int tLevel = waterLevel(aWorld); return waterstream(aBlock) || ((aBlock == Blocks.WATER || aBlock == Blocks.WATER) && UT.Code.inside(tLevel-15, tLevel, aY) && BIOMES_RIVER_LAKE.contains(aWorld.getBiome(new BlockPos(aX, aY, aZ))));}
	
	public static boolean hasCollide(Level aWorld, int aX, int aY, int aZ) {return hasCollide(aWorld, aX, aY, aZ, aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock());} // было aWorld.getBlock(x,y,z)
	// было aBlock.getCollisionBoundingBoxFromPool(world,x,y,z)!=null — BlockState.getCollisionShape(level,pos).isEmpty()
	// перевёрнуто (BlockBehaviour.java:674; VoxelShape.isEmpty(), VoxelShape.java:73); isOpaqueCube() не тронут.
	public static boolean hasCollide(Level aWorld, int aX, int aY, int aZ, Block aBlock) {return WD.opaque(aBlock) || !aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getCollisionShape(aWorld, new BlockPos(aX, aY, aZ)).isEmpty();}

	public static boolean hasCollide(Level aWorld, BlockPos aCoords) {return hasCollide(aWorld, aCoords, aWorld.getBlockState(aCoords).getBlock());} // было aWorld.getBlock(x,y,z)
	public static boolean hasCollide(Level aWorld, BlockPos aCoords, Block aBlock) {return WD.opaque(aBlock) || !aWorld.getBlockState(aCoords).getCollisionShape(aWorld, aCoords).isEmpty();} // было aBlock.getCollisionBoundingBoxFromPool(world,x,y,z)!=null
	
	public static boolean flaming(Level aWorld, int aX, int aY, int aZ) {return block(aWorld, aX, aY, aZ, F) instanceof FireBlock;}
	public static boolean burning(Level aWorld, int aX, int aY, int aZ) {return flaming(aWorld, aX, aY, aZ) || flaming(aWorld, aX+1, aY, aZ) || flaming(aWorld, aX-1, aY, aZ) || flaming(aWorld, aX, aY+1, aZ) || flaming(aWorld, aX, aY-1, aZ) || flaming(aWorld, aX, aY, aZ+1) || flaming(aWorld, aX, aY, aZ-1);}
	
	public static void burn(Level aWorld, BlockPos aCoords, boolean aReplaceCenter, boolean aCheckFlammability) {for (byte tSide : aReplaceCenter?ALL_SIDES_MIDDLE_UP:ALL_SIDES_VALID) fire(aWorld, aCoords.getX()+OFFX[tSide], aCoords.getY()+OFFY[tSide], aCoords.getZ()+OFFZ[tSide], aCheckFlammability);}
	public static void burn(Level aWorld, int aX, int aY, int aZ  , boolean aReplaceCenter, boolean aCheckFlammability) {for (byte tSide : aReplaceCenter?ALL_SIDES_MIDDLE_UP:ALL_SIDES_VALID) fire(aWorld, aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide], aCheckFlammability);}
	
	public static boolean fire(Level aWorld, BlockPos aCoords, boolean aCheckFlammability) {return fire(aWorld, aCoords.getX(), aCoords.getY(), aCoords.getZ(), aCheckFlammability);}
	public static boolean fire(Level aWorld, int aX, int aY, int aZ, boolean aCheckFlammability) {
		BlockPos tFirePos = new BlockPos(aX, aY, aZ);
		Block tBlock = aWorld.getBlockState(tFirePos).getBlock(); // было aWorld.getBlock(x,y,z)
		if (WD.getMaterial(tBlock) == Material.lava || WD.getMaterial(tBlock) == Material.fire) return F;
		// было tBlock.getCollisionBoundingBoxFromPool(world,x,y,z)==null — BlockState.getCollisionShape(level,pos).isEmpty() (BlockBehaviour.java:674)
		if (WD.getMaterial(tBlock) == Material.carpet || aWorld.getBlockState(tFirePos).getCollisionShape(aWorld, tFirePos).isEmpty()) {
			if (MD.TC.mLoaded && te(aWorld, aX, aY, aZ, T) instanceof INode) return F;
			// F-block: IBlockExtension.getFlammability(int meta,world,x,y,z,dir) -> BlockState.getFlammability(
			// BlockGetter,BlockPos,Direction) (IBlockExtension.java:677) — на состоянии, не на Block.
			if (aWorld.getBlockState(tFirePos).getFlammability(aWorld, tFirePos, FORGE_DIR[SIDE_ANY]) > 0) return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // было aWorld.setBlock(x,y,z,Blocks.FIRE,0,3)
			if (tBlock instanceof IItemGT) return F;
			if (aCheckFlammability) {
				for (byte tSide : ALL_SIDES_VALID) {
					BlockPos tAdjPos = new BlockPos(aX+OFFX[tSide], aY+OFFY[tSide], aZ+OFFZ[tSide]);
					Block tAdjacent = block(aWorld, aX, aY, aZ, tSide);
					if (tAdjacent == Blocks.CHEST || tAdjacent == Blocks.TRAPPED_CHEST) return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // было aWorld.setBlock(x,y,z,Blocks.FIRE) (3-арг default meta=0,flags=3)
					// F-block: getFlammability на BlockState соседа (IBlockExtension.java:677), pos соседа вычислен.
					if (aWorld.getBlockState(tAdjPos).getFlammability(aWorld, tAdjPos, FORGE_DIR_OPPOSITES[tSide]) > 0) return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // было aWorld.setBlock(x,y,z,Blocks.FIRE)
				}
			} else {
				return aWorld.setBlock(tFirePos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL); // было aWorld.setBlock(x,y,z,Blocks.FIRE,0,3)
			}
		}
		return F;
	}
	
	public static boolean oreGenReplaceable(Level aWorld, int aX, int aY, int aZ, boolean aAllowAir) {
		Block aBlock = aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock(); // было aWorld.getBlock(x,y,z)
		if (aBlock == NB) return aAllowAir;
		byte aMeta = meta(aWorld, aX, aY, aZ); // было (byte)WD.meta(aWorld, x,y,z) — централизованный meta(...), МОДЕЛЬ МЕТЫ п.4
		if (BlocksGT.sDontGenerateOresIn.contains(new ItemStackContainer(aBlock, 1, aMeta))) return F;
		if (BlocksGT.stoneToNormalOres.containsKey(new ItemStackContainer(aBlock, 1, aMeta))) return T;
		if (Blocks.STONE      != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.STONE     )) return T;
		if (Blocks.GRAVEL     != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.GRAVEL    )) return T;
		if (Blocks.SAND       != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.SAND      )) return T;
		if (Blocks.NETHERRACK != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.NETHERRACK)) return T;
		if (Blocks.END_STONE  != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.END_STONE )) return T;
		return F;
	}
	
	public static boolean setOre(Level aWorld, int aX, int aY, int aZ, OreDictMaterial aMaterial) {
		return aMaterial != null && setOre(aWorld, aX, aY, aZ, aMaterial.mID);
	}
	
	public static boolean setOre(Level aWorld, int aX, int aY, int aZ, short aID) {
		if (aID <= 0 && aID == W) return F;
		Block aBlock = aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock(); // было aWorld.getBlock(x,y,z)
		if (aBlock == NB) return F;
		byte aMeta = meta(aWorld, aX, aY, aZ); // было (byte)WD.meta(aWorld, x,y,z)
		if (BlocksGT.sDontGenerateOresIn.contains(new ItemStackContainer(aBlock, 1, aMeta))) return F;
		IBlockPlacable tBlock = BlocksGT.stoneToNormalOres.get(new ItemStackContainer(aBlock, 1, aMeta));
		if (tBlock == null) {
		if (Blocks.STONE      != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.STONE     )) tBlock = BlocksGT.ore; else
		if (Blocks.GRAVEL     != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.GRAVEL    )) tBlock = BlocksGT.oreGravel; else
		if (Blocks.SAND       != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.SAND      )) tBlock = BlocksGT.oreSand; else
		if (Blocks.NETHERRACK != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.NETHERRACK)) tBlock = BlocksGT.oreNetherrack; else
		if (Blocks.END_STONE  != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.END_STONE )) tBlock = BlocksGT.oreEndstone;
		}
		return tBlock != null && tBlock.placeBlock(aWorld, aX, aY, aZ, (byte)6, aID, null, F, T);
	}
	
	public static boolean setSmallOre(Level aWorld, int aX, int aY, int aZ, OreDictMaterial aMaterial) {
		return aMaterial != null && setSmallOre(aWorld, aX, aY, aZ, aMaterial.mID);
	}
	
	public static boolean setSmallOre(Level aWorld, int aX, int aY, int aZ, short aID) {
		if (aID <= 0 && aID == W) return F;
		Block aBlock = aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock(); // было aWorld.getBlock(x,y,z)
		if (aBlock == NB || WD.bedrock(aBlock)) return F;
		byte aMeta = meta(aWorld, aX, aY, aZ); // было (byte)WD.meta(aWorld, x,y,z)
		if (BlocksGT.sDontGenerateOresIn.contains(new ItemStackContainer(aBlock, 1, aMeta))) return F;
		IBlockPlacable tBlock = BlocksGT.stoneToSmallOres.get(new ItemStackContainer(aBlock, 1, aMeta));
		if (tBlock == null) {
		if (Blocks.STONE      != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.STONE     )) tBlock = BlocksGT.oreSmall; else
		if (Blocks.GRAVEL     != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.GRAVEL    )) tBlock = BlocksGT.oreSmallGravel; else
		if (Blocks.SAND       != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.SAND      )) tBlock = BlocksGT.oreSmallSand; else
		if (Blocks.NETHERRACK != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.NETHERRACK)) tBlock = BlocksGT.oreSmallNetherrack; else
		if (Blocks.END_STONE  != aBlock && WD.oreGen(aBlock, aWorld, aX, aY, aZ, Blocks.END_STONE )) tBlock = BlocksGT.oreSmallEndstone;
		}
		return tBlock != null && tBlock.placeBlock(aWorld, aX, aY, aZ, (byte)6, aID, null, F, T);
	}
	
	/** Removes Bedrock from that Position and replaces it with regular Stone of the region. */
	public static boolean removeBedrock(Level aWorld, int aX, int aY, int aZ) {
		// было aWorld.getBlock(x,y,z) + WD.dimensionId(aWorld)==DIM_NETHER — Level.dimension()==Level.NETHER,
		// тот же приём F6, что уже применён у dimOverworldLike/dimPlanet выше в этом файле.
		Block tBlock = aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock(), tStone = (aWorld.dimension() == Level.NETHER ? Blocks.NETHERRACK : Blocks.STONE);

		if (tBlock == NB || bedrock(tBlock)) {
			for (byte tSide : ALL_SIDES_BUT_BOTTOM) for (int i = 1; i < 7; i++) {
				BlockPos tRBPos = new BlockPos(aX+OFFX[tSide]*i, aY+OFFY[tSide]*i, aZ+OFFZ[tSide]*i);
				tBlock = aWorld.getBlockState(tRBPos).getBlock(); // было aWorld.getBlock(x,y,z)
				if (tBlock != NB && tBlock != tStone && !bedrock(tBlock)) {
					int tMetaData = meta(aWorld, tRBPos.getX(), tRBPos.getY(), tRBPos.getZ()); // было WD.meta(aWorld, x,y,z)
					if (BlocksGT.stoneToNormalOres.containsKey(new ItemStackContainer(tBlock, 1, tMetaData))) {
						return set(aWorld, aX, aY, aZ, tBlock, tMetaData, 0, F); // было aWorld.setBlock(x,y,z,block,meta,0) — маршрут через центр set(...)
					}
				}
			}
			return set(aWorld, aX, aY, aZ, tStone, 0, 0, F); // было aWorld.setBlock(x,y,z,tStone,0,0) — маршрут через центр set(...)
		}
		return F;
	}
	
	public static List<BlockPos> line(final Vec3 aStart, final Vec3 aEnd) {
		List<BlockPos> rList = new ArrayListNoNulls<>();
		if (Double.isNaN(aStart.x) || Double.isNaN(aStart.y) || Double.isNaN(aStart.z) || Double.isNaN(aEnd.x) || Double.isNaN(aEnd.y) || Double.isNaN(aEnd.z)) return rList;
		// F-vec: neo Vec3 иммутабелен (поля x/y/z final) — 1.7.10 мутировал tPoint.xCoord покомпонентно;
		// воспроизводим реассайном tPoint = new Vec3(...) (см. три ветки ниже), поведение 1:1.
		Vec3 tPoint = new Vec3(aStart.x, aStart.y, aStart.z);
		
		int sx = UT.Code.roundDown(tPoint.x);
		int sy = UT.Code.roundDown(tPoint.y);
		int sz = UT.Code.roundDown(tPoint.z);
		int ex = UT.Code.roundDown(aEnd.x);
		int ey = UT.Code.roundDown(aEnd.y);
		int ez = UT.Code.roundDown(aEnd.z);
		
		rList.add(new BlockPos(sx, sy, sz));
		
		int maxAttempts = 2000; // Just to prevent accidental infinite loops
		
		while (maxAttempts-- >= 0) {
			if (Double.isNaN(tPoint.x) || Double.isNaN(tPoint.y) || Double.isNaN(tPoint.z)) return rList;
			if (sx == ex && sy == ey && sz == ez) return rList;
			
			boolean performx = true;
			boolean performy = true;
			boolean performz = true;
			
			double nx = 999.0D;
			double ny = 999.0D;
			double nz = 999.0D;
			
			double ndx = 999.0D;
			double ndy = 999.0D;
			double ndz = 999.0D;
			
			double distx = aEnd.x - tPoint.x;
			double disty = aEnd.y - tPoint.y;
			double distz = aEnd.z - tPoint.z;
			
			if (ex > sx) {
				nx = (double) sx + 1.0D;
			} else if (ex < sx) {
				nx = (double) sx + 0.0D;
			} else {
				performx = false;
			}
			
			if (ey > sy) {
				ny = (double) sy + 1.0D;
			} else if (ey < sy) {
				ny = (double) sy + 0.0D;
			} else {
				performy = false;
			}
			
			if (ez > sz) {
				nz = (double) sz + 1.0D;
			} else if (ez < sz) {
				nz = (double) sz + 0.0D;
			} else {
				performz = false;
			}
			
			if (performx) {
				ndx = (nx - tPoint.x) / distx;
			}
			
			if (performy) {
				ndy = (ny - tPoint.y) / disty;
			}
			
			if (performz) {
				ndz = (nz - tPoint.z) / distz;
			}
			
			byte whereTo;
			
			if (ndx < ndy && ndx < ndz) {
				if (ex > sx) whereTo = 4;
				else whereTo = 5;
				
				tPoint = new Vec3(nx, tPoint.y + disty * ndx, tPoint.z + distz * ndx);
			} else if (ndy < ndz) {
				if (ey > sy) whereTo = 0;
				else whereTo = 1;
				
				tPoint = new Vec3(tPoint.x + distx * ndy, ny, tPoint.z + distz * ndy);
			} else {
				if (ez > sz) whereTo = 2;
				else whereTo = 3;
				
				tPoint = new Vec3(tPoint.x + distx * ndz, tPoint.y + disty * ndz, nz);
			}
			
			sx = UT.Code.roundDown(tPoint.x);
			sy = UT.Code.roundDown(tPoint.y);
			sz = UT.Code.roundDown(tPoint.z);
			
			if (whereTo == 5) --sx;
			if (whereTo == 1) --sy;
			if (whereTo == 3) --sz;
			
			rList.add(new BlockPos(sx, sy, sz));
		}
		return rList;
	}
	
	public static long scan(ArrayList<String> aList, Player aPlayer, Level aWorld, int aScanLevel, int aX, int aY, int aZ, byte aSide, float aClickX, float aClickY, float aClickZ) {
		if (aList == null) return 0;
		
		ArrayList<String> rList = new ArrayListNoNulls<>();
		long rEUAmount = 0;
		
		Block aBlock = aWorld.getBlockState(new BlockPos(aX, aY, aZ)).getBlock(); // было aWorld.getBlock(x,y,z)
		byte aMeta = meta(aWorld, aX, aY, aZ); // было (byte)WD.meta(aWorld, x,y,z)
		BlockEntity aTileEntity = te(aWorld, aX, aY, aZ, T);
		
		rList.add("--- X: " + aX + " Y: " + aY + " Z: " + aZ + " ---");
		try {
			// F-container: 1.7.10 TileEntity мог быть IWorldNameable.getInventoryName() (String). neo BlockEntity
		// не Menu (instanceof AbstractContainerMenu невозможен) — кастомное имя даёт Nameable.getName():Component
		// (Nameable.java:7), .getString() -> String для stringValid. Отладочный скан, поведение 1:1.
		rList.add("Name: " + (aTileEntity instanceof net.minecraft.world.Nameable tNameable && Code.stringValid(tNameable.getName().getString()) ? tNameable.getName().getString() : aBlock.getDescriptionId()) + "  MetaData: " + aMeta);
			rList.add("Registry: " + ST.regName(aBlock));
			if (aScanLevel >= 10) {
				rList.add("Block Class: " + aBlock.getClass());
				if (aTileEntity != null) rList.add("TileEntity Class: " + aTileEntity.getClass());
			}
			// было getExplosionResistance(Entity,World,x,y,z,eX,eY,eZ) -> нет прямого эквивалента без реального Explosion-объекта
			// (IBlockExtension.getExplosionResistance(BlockState,BlockGetter,BlockPos,Explosion) [IBlockExtension.java:333]
			// требует Explosion, которого у debug-scan нет); маршрутизируем на Block.getExplosionResistance() [Block.java:453] -
			// тот же фолбэк, что location-sensitive default сам использует при отсутствии переопределения.
			float tResistance = aBlock.getExplosionResistance();
			rList.add("Hardness: " + WD.hardness(aBlock, aWorld, aX, aY, aZ) + " - " + LH.getToolTipBlastResistance(aBlock, tResistance));
			// F-tool: getHarvestLevel/getHarvestTool(int) — GT6-методы на BlockBase (Forge-точки на vanilla Block
			// удалены). Отладочный скан произвольного блока: guard instanceof, ваниль -> 0/"" (нет GT6-tier).
			int tHarvestLevel = aBlock instanceof BlockBase ? ((BlockBase)aBlock).getHarvestLevel(aMeta) : 0;
			String tHarvestTool = aBlock instanceof BlockBase ? ((BlockBase)aBlock).getHarvestTool(aMeta) : "";
			rList.add(tHarvestLevel == 0 && WD.getMaterial(aBlock).isAdventureModeExempt() ? "Hand-Harvestable, but " + (Code.stringValid(tHarvestTool)?Code.capitalise(tHarvestTool):"None") + " is faster" : "Tool to Harvest: " + (Code.stringValid(tHarvestTool)?Code.capitalise(tHarvestTool):"None") + " (" + tHarvestLevel + ")");
			// F-block: Forge Block.isBeaconBase(world,x,y,z,bx,by,bz) удалён -> neo тег BlockTags.BEACON_BASE_BLOCKS
			// (BlockTags.java:115), проверка на состоянии.
			if (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).is(net.minecraft.tags.BlockTags.BEACON_BASE_BLOCKS)) rList.add("Is usable for Beacon Pyramids");
			if (MD.GC.mLoaded && aBlock instanceof IPartialSealableBlock) rList.add(((IPartialSealableBlock)aBlock).isSealed(aWorld, aX, aY, aZ, FORGE_DIR[aSide ^ 1]) ? "Is Sealable on this Side" : "Is not Sealable on this Side");
		} catch(Throwable e) {e.printStackTrace(ERR);}
		if (aTileEntity != null) {
			try {if (aTileEntity instanceof ITileEntityWeight && ((ITileEntityWeight)aTileEntity).getWeightValue(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Weight: " + ((ITileEntityWeight)aTileEntity).getWeightValue(aSide) + " kg");
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityTemperature && ((ITileEntityTemperature)aTileEntity).getTemperatureMax(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Temperature: " + ((ITileEntityTemperature)aTileEntity).getTemperatureValue(aSide) + " / " + ((ITileEntityTemperature)aTileEntity).getTemperatureMax(aSide) + " K");
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityGibbl && ((ITileEntityGibbl)aTileEntity).getGibblMax(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Pressure: " + ((ITileEntityGibbl)aTileEntity).getGibblValue(aSide) + " / " + ((ITileEntityGibbl)aTileEntity).getGibblMax(aSide) + " Gibbl");
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityProgress && ((ITileEntityProgress)aTileEntity).getProgressMax(aSide) > 0) {
				rEUAmount+=V[3];
				rList.add("Progress: " + ((ITileEntityProgress)aTileEntity).getProgressValue(aSide) + " / " + ((ITileEntityProgress)aTileEntity).getProgressMax(aSide));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			
			
			String rState = "";
			try {if (aTileEntity instanceof ITileEntitySwitchableOnOff) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("State: " + (((ITileEntitySwitchableOnOff)aTileEntity).getStateOnOff()?"ON":"OFF"));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntitySwitchableMode) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Mode: " + (((ITileEntitySwitchableMode)aTileEntity).getStateMode()));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityRunningSuccessfully) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningSuccessfully()?"Successfully":((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningActively()?"Actively":((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningPassively()?"Passively":((ITileEntityRunningSuccessfully)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			} else if (aTileEntity instanceof ITileEntityRunningActively) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningActively)aTileEntity).getStateRunningActively()?"Actively":((ITileEntityRunningActively)aTileEntity).getStateRunningPassively()?"Passively":((ITileEntityRunningActively)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			} else if (aTileEntity instanceof ITileEntityRunningPassively) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningPassively)aTileEntity).getStateRunningPassively()?"Passively":((ITileEntityRunningPassively)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			} else if (aTileEntity instanceof ITileEntityRunningPossible) {
				if (Code.stringValid(rState)) rState += " --- ";
				rEUAmount+=V[3];
				rState += ("Running: " + (((ITileEntityRunningPossible)aTileEntity).getStateRunningPossible()?"Possible":"Not Possible"));
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			if (Code.stringValid(rState)) rList.add(rState);
			
			
			try {if (aTileEntity instanceof ITileEntityEnergy) {
				rEUAmount+=V[3];
				for (TagData tEnergyType : ((ITileEntityEnergy)aTileEntity).getEnergyTypes(aSide)) {
					rList.add("Input: " + ((ITileEntityEnergy)aTileEntity).getEnergySizeInputMin(tEnergyType, aSide) + " to " + ((ITileEntityEnergy)aTileEntity).getEnergySizeInputMax(tEnergyType, aSide) + tEnergyType.getLocalisedNameShort());
					rList.add("Output: " + ((ITileEntityEnergy)aTileEntity).getEnergySizeOutputMin(tEnergyType, aSide) + " to " + ((ITileEntityEnergy)aTileEntity).getEnergySizeOutputMax(tEnergyType, aSide) + tEnergyType.getLocalisedNameShort());
				}
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			try {if (aTileEntity instanceof ITileEntityEnergyDataCapacitor) {
				rEUAmount+=V[3];
				for (TagData tEnergyType : ((ITileEntityEnergyDataCapacitor)aTileEntity).getEnergyCapacitorTypes(aSide)) {
					rList.add("Stored: " + ((ITileEntityEnergyDataCapacitor)aTileEntity).getEnergyStored(tEnergyType, aSide) + " of " + ((ITileEntityEnergyDataCapacitor)aTileEntity).getEnergyCapacity(tEnergyType, aSide) + tEnergyType.getLocalisedNameShort());
				}
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			
			
			try {if (aTileEntity instanceof IFluidHandler) {
				rEUAmount+=V[3];
				// F5: 1.7.10 IFluidHandler.getTankInfo(ForgeDirection)->FluidTankInfo[] удалён -> neo sideless
				// getTanks()/getFluidInTank(i)/getTankCapacity(i) (IFluidHandler.java:60/81/92; side-параметр снят
				// движком — neo-capability side-specific при получении, не per-call). F15: getFluidInTank даёт EMPTY,
				// не null -> isEmpty(); в FL.name пустой бак -> null для "" (1:1 с оригиналом fluid==null->"").
				IFluidHandler tHandler = (IFluidHandler)aTileEntity;
				for (int i = 0; i < tHandler.getTanks(); i++) {
					net.neoforged.neoforge.fluids.FluidStack tFluid = tHandler.getFluidInTank(i);
					rList.add("Tank " + i + ": " + (tFluid.isEmpty()?0:tFluid.getAmount()) + " / " + tHandler.getTankCapacity(i) + " " + FL.name(tFluid.isEmpty()?null:tFluid, T));
				}
			}} catch(Throwable e) {e.printStackTrace(ERR);}
			
			if (!(aTileEntity instanceof ITileEntity)) {
				try {if (aTileEntity instanceof ic2.api.reactor.IReactorChamber) {
					rEUAmount+=V[4];
					aTileEntity = (BlockEntity)(((ic2.api.reactor.IReactorChamber)aTileEntity).getReactor());
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.reactor.IReactor) {
					rEUAmount+=V[4];
					rList.add( "Heat: " + ((ic2.api.reactor.IReactor)aTileEntity).getHeat() + "/" + ((ic2.api.reactor.IReactor)aTileEntity).getMaxHeat()
							+ "  HEM: " + ((ic2.api.reactor.IReactor)aTileEntity).getHeatEffectModifier() + "  Base IC2-EU Output: " + ((ic2.api.reactor.IReactor)aTileEntity).getReactorEUEnergyOutput());
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.tile.IWrenchable) {
					rEUAmount+=V[3];
					rList.add("Facing: " + ((ic2.api.tile.IWrenchable)aTileEntity).getFacing() + " / IC2 Wrench Drop Chance: " + (((ic2.api.tile.IWrenchable)aTileEntity).wrenchCanRemove(aPlayer)?(((ic2.api.tile.IWrenchable)aTileEntity).getWrenchDropRate()*100):0) + "%");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.energy.tile.IEnergySink) {
					rEUAmount+=V[3];
					rList.add("Demanded Energy: " + ((ic2.api.energy.tile.IEnergySink)aTileEntity).getDemandedEnergy() + " IC2-EU");
					rList.add("Max Safe Input: " + V[((ic2.api.energy.tile.IEnergySink)aTileEntity).getSinkTier()] + " IC2-EU/t");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.energy.tile.IEnergySource) {
					rEUAmount+=V[3];
					rList.add("Max Energy Output: " + V[((ic2.api.energy.tile.IEnergySource)aTileEntity).getSourceTier()] + " IC2-EU/t");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.energy.tile.IEnergyConductor) {
					rEUAmount+=V[3];
					rList.add("Conduction Loss: " + ((ic2.api.energy.tile.IEnergyConductor)aTileEntity).getConductionLoss() + " IC2-EU/m");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
				try {if (aTileEntity instanceof ic2.api.tile.IEnergyStorage) {
					rEUAmount+=V[3];
					rList.add("Contained Energy: " + ((ic2.api.tile.IEnergyStorage)aTileEntity).getStored() + " of " + ((ic2.api.tile.IEnergyStorage)aTileEntity).getCapacity() + " IC2-EU");
					rList.add(((ic2.api.tile.IEnergyStorage)aTileEntity).isTeleporterCompatible(FORGE_DIR[aSide])?"Teleporter Compatible":"Not Teleporter Compatible");
				}} catch(NoClassDefFoundError e) {/* ignore */} catch(Throwable e) {e.printStackTrace(ERR);}
			}
		}
		try {if (aBlock instanceof IBlockDebugable) {
			rEUAmount+=V[3];
			ArrayList<String> temp = ((IBlockDebugable)aBlock).getDebugInfo(aPlayer, aX, aY, aZ, aScanLevel);
			if (temp != null) rList.addAll(temp);
		}} catch(Throwable e) {e.printStackTrace(ERR);}
		
		BlockScanningEvent tEvent = new BlockScanningEvent(aWorld, aPlayer, aX, aY, aZ, aSide, aScanLevel, aBlock, aTileEntity, rList, aClickX, aClickY, aClickZ);
		tEvent.mEUCost = rEUAmount;
		NeoForge.EVENT_BUS.post(tEvent);
		if (!tEvent.isCanceled()) aList.addAll(rList);
		return tEvent.mEUCost;
	}
}
