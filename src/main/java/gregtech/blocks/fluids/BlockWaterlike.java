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

package gregtech.blocks.fluids;

import gregapi.block.IBlock;
import gregapi.block.IBlockOnHeadInside;
import gregapi.block.fluid.BlockFluidBaseGT;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.item.IItemGT;
import gregapi.lang.LanguageHandler;
import gregapi.render.RendererBlockFluid;
import gregapi.tileentity.data.ITileEntitySurface;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import gregapi.block.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 *
 * F5 форс движка (decisions/F5-fluids.md §5): было {@code extends BlockFluidClassic} (Forge, удалён в neo) —
 * общий предок с {@link gregapi.block.fluid.BlockBaseFluid} воспроизведён ОДИН раз в
 * {@link BlockFluidBaseGT} (см. его javadoc). Тела GT6-собственных методов (updateFlow/getFlowVector/
 * getQuantaValue/shouldSideBeRendered/onHeadInside/...) — 1:1, только API-свод.
 */
public abstract class BlockWaterlike extends BlockFluidBaseGT implements IBlock, IItemGT, IBlockOnHeadInside {
	public static int WATER_UPDATE_FLAGS = 0;

	public final Fluid mFluid;

	public BlockWaterlike(String aName, Fluid aFluid, boolean aFlowsOut, boolean aHide) {
		// было super(aFluid, Material.water) + setResistance(30) — neo Block immutable (Properties ДО super,
		// F16/F9 форс движка, см. BlockFluidBaseGT). setBlockName удалён (имя — ST.register ниже, как
		// BlockBase.java); setLightOpacity(...) удалено (own getLightOpacity() ниже уже хардкодит значение);
		// setFluidStack(...) удалено (Forge-only stack-поле, GT6 drain() его не читает — мёртвый код).
		// F12-followup (block-split): setId в Properties (иначе «Block id not set»); namespace=GAPI (совпадает с реестром/call-site).
		// Fluid-перегрузка супер-ктора: перенос характеристик 1:1 c Forge BlockFluidBase(Fluid,Material) —
		// воды получают density=1000 (иначе нефти 600-900 «плотнее» воды density=1 и вытесняли бы её) и
		// tickRate=5, который подклассы переставляют после super (Ocean/River 20, Swamp 10 — 1:1).
		super(BlockBehaviour.Properties.of().explosionResistance(30F).setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(gregapi.data.CS.ModIDs.GT, gregapi.GT_API.sanitizeRegName(aName)))), Material.water, aFluid);
		mFluid = aFluid;
		quantaPerBlock = (aFlowsOut ? 8 : 3);
		quantaPerBlockFloat = quantaPerBlock;
		// F12-followup (block-split): блок регистрирует registerBlockLazy на call-site (Loader_Blocks); ЗДЕСЬ — только BlockItem.
		gregapi.GT_API.registerItemLazy(gregapi.data.CS.ModIDs.GT, aName, () -> gregapi.GT_API.blockItemFor(this, BlockItem.class));
		LH.add(getUnlocalizedName(), getLocalizedName());
		LanguageHandler.set(getLocalizedName(), getLocalizedName()); // WAILA is retarded...
		if (aHide) gregapi.GT_API.deferItemInit(() -> ST.hide(this));
	}

	// @Override
	public FluidStack drain(Level aWorld, int aX, int aY, int aZ, boolean aDoDrain) {
		if (aDoDrain) WD.set(aWorld, aX, aY, aZ, NB, 0, 2);
		return FL.make(mFluid, 1000); // было getFluid() (IFluidBlock/Forge) — mFluid собственное поле (F5)
	}
	
	// @Override
	public boolean canDrain(Level aWorld, int aX, int aY, int aZ) {
		return WD.meta(aWorld, aX, aY, aZ) == 0;
	}
	
	/** было Forge {@code BlockFluidClassic.getLargerQuanta(IBlockAccess,x,y,z,compare)} — тело 1:1 (нужно
	 *  ТОЛЬКО {@link #updateFlow}, у {@link gregapi.block.fluid.BlockBaseFluid} свой quanta-поток). */
	protected int getLargerQuanta(BlockGetter aWorld, int aX, int aY, int aZ, int aCompare) {
		int tQuantaRemaining = getQuantaValue(aWorld, aX, aY, aZ);
		if (tQuantaRemaining <= 0) return aCompare;
		return tQuantaRemaining >= aCompare ? tQuantaRemaining : aCompare;
	}

	/** было 1.7.10 {@code Block.isBlockSolid(IBlockAccess,x,y,z,side)} (Forge-хелпер на ВСЕХ Block,
	 *  {@code side}-параметр в оригинале не используется телом) — {@code aSide} сохранён в сигнатуре 1:1
	 *  (вызывающий {@link #getFlowVector} передаёт его), тело — {@code WD.getMaterial(...).isSolid()}. */
	protected boolean isBlockSolid(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		return WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isSolid();
	}

	public void updateFlow(Level aWorld, int aX, int aY, int aZ, Random aRandom) {
		int quantaRemaining = quantaPerBlock - WD.meta(aWorld, aX, aY, aZ);
		int expQuanta = -101;
		// check adjacent block levels if non-source
		if (quantaRemaining < quantaPerBlock) {
			if (WD.block(aWorld, aX  , aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX-1, aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX+1, aY-densityDir, aZ  ) instanceof BlockWaterlike ||
				WD.block(aWorld, aX  , aY-densityDir, aZ-1) instanceof BlockWaterlike ||
				WD.block(aWorld, aX  , aY-densityDir, aZ+1) instanceof BlockWaterlike) {
				expQuanta = quantaPerBlock - 1;
			} else {
				int maxQuanta = -100;
				maxQuanta = getLargerQuanta(aWorld, aX-1, aY, aZ  , maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX+1, aY, aZ  , maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX  , aY, aZ-1, maxQuanta);
				maxQuanta = getLargerQuanta(aWorld, aX  , aY, aZ+1, maxQuanta);
				expQuanta = maxQuanta - 1;
			}
			if (expQuanta != quantaRemaining) {
				quantaRemaining = expQuanta;
				if (expQuanta <= 0) {
					WD.set(aWorld, aX, aY, aZ, NB, 0, 3);
				} else {
					WD.set(aWorld, aX, aY, aZ, WD.block(aWorld, aX, aY, aZ), quantaPerBlock - expQuanta, 3, F);
					aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate); // было aWorld.scheduleBlockUpdate(x,y,z,block,delay) — ScheduledTickAccess.scheduleTick(BlockPos,Block,int)
					aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), this); // было aWorld.notifyBlocksOfNeighborChange(x,y,z,block) — LevelAccessor.updateNeighborsAt(BlockPos,Block)
				}
			}
		}
		// Here was an else Block that only caused huge amounts of Network Lag with no purpose. Forge, just what the fuck, setting Metadata from 0 to 0 and updating that "change" to Clients? There was no change that needed to be updated!
		
		
		if (canDisplace(aWorld, aX, aY+densityDir, aZ)) {
			if (displaceIfPossible(aWorld, aX, aY+densityDir, aZ)) WD.set(aWorld, aX, aY+densityDir, aZ, this, 1, WATER_UPDATE_FLAGS | 1);
			return;
		}
		
		int tFlowMeta  = (WD.block(aWorld, aX, aY-densityDir, aZ) instanceof BlockWaterlike ? 1 : quantaPerBlock - quantaRemaining + 1);
		if (tFlowMeta >= quantaPerBlock) return;
		
		if (WD.exists(aWorld, aX, aY, aZ-1)) flowTo(aWorld, aX  , aY, aZ-1, tFlowMeta);
		if (WD.exists(aWorld, aX, aY, aZ+1)) flowTo(aWorld, aX  , aY, aZ+1, tFlowMeta);
		if (WD.exists(aWorld, aX-1, aY, aZ)) flowTo(aWorld, aX-1, aY, aZ  , tFlowMeta);
		if (WD.exists(aWorld, aX+1, aY, aZ)) flowTo(aWorld, aX+1, aY, aZ  , tFlowMeta);
	}

	// B4 (слабы/неполные блоки «как mc26»): растекание на waterloggable-блок (slab/stairs/fence — SimpleWaterloggedBlock)
	// → WATERLOG его (вода внутри, блок остаётся), как vanilla-вода mc26; иначе — прежний displace+set. Waterlogging нет в
	// 1.7.10 (там вода не переживала слабы) — изобретено канонично neo (SimpleWaterloggedBlock/WATERLOGGED). Централизовано:
	// один хелпер на все 4 направления растекания (был повтор displaceIfPossible+WD.set).
	public boolean flowTo(Level aWorld, int aX, int aY, int aZ, int aMeta) {
		net.minecraft.core.BlockPos tP = new net.minecraft.core.BlockPos(aX, aY, aZ);
		net.minecraft.world.level.block.state.BlockState tSt = aWorld.getBlockState(tP);
		if (tSt.getBlock() instanceof net.minecraft.world.level.block.SimpleWaterloggedBlock
		 && tSt.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
		 && !tSt.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
			return aWorld.setBlock(tP, tSt.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, Boolean.TRUE), 3);
		}
		if (displaceIfPossible(aWorld, aX, aY, aZ)) { WD.set(aWorld, aX, aY, aZ, this, aMeta, WATER_UPDATE_FLAGS | 1); return true; }
		return false;
	}
	
	// @Override
	public Vec3 getFlowVector(BlockGetter aWorld, int aX, int aY, int aZ) {
		Vec3 rVector = new Vec3(0, 0, 0); // было Vec3.createVectorHelper(0,0,0) — Forge/1.7.10-only фабрика, neo конструктор Vec3(double,double,double)
		int tDecay = quantaPerBlock - getQuantaValue(aWorld, aX, aY, aZ);
		for (byte tSide : ALL_SIDES_HORIZONTAL) {
			int tX = aX+OFFX[tSide], tZ = aZ+OFFZ[tSide];
			int tOtherDecay = quantaPerBlock - getQuantaValue(aWorld, tX, aY, tZ);
			if (tOtherDecay >= quantaPerBlock) {
				if (!WD.getMaterial(WD.block(aWorld, tX, aY, tZ)).blocksMovement()) {
					tOtherDecay = quantaPerBlock - getQuantaValue(aWorld, tX, aY-1, tZ);
					if (tOtherDecay >= 0) {
						int tPower = tOtherDecay - (tDecay - quantaPerBlock);
						rVector = rVector.add((tX - aX) * tPower, 0, (tZ - aZ) * tPower); // было .addVector(...) — Vec3.add(double,double,double)
					}
				}
			} else if (tOtherDecay >= 0) {
				int power = tOtherDecay - tDecay;
				rVector = rVector.add((tX - aX) * power, 0, (tZ - aZ) * power);
			}
		}
		if (WD.block(aWorld, aX, aY+1, aZ) instanceof BlockWaterlike && (
			isBlockSolid(aWorld, aX  , aY  , aZ-1, SIDE_Z_NEG) ||
			isBlockSolid(aWorld, aX  , aY  , aZ+1, SIDE_Z_POS) ||
			isBlockSolid(aWorld, aX-1, aY  , aZ  , SIDE_X_NEG) ||
			isBlockSolid(aWorld, aX+1, aY  , aZ  , SIDE_X_POS) ||
			isBlockSolid(aWorld, aX  , aY+1, aZ-1, SIDE_Z_NEG) ||
			isBlockSolid(aWorld, aX  , aY+1, aZ+1, SIDE_Z_POS) ||
			isBlockSolid(aWorld, aX-1, aY+1, aZ  , SIDE_X_NEG) ||
			isBlockSolid(aWorld, aX+1, aY+1, aZ  , SIDE_X_POS))) {
			rVector = rVector.normalize().add(0, -6, 0);
		}
		return rVector.normalize();
	}

	// @Override
	public int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return 0;
		if (aBlock == this) return quantaPerBlock - WD.meta(aWorld, aX, aY, aZ);
		if (aBlock instanceof BlockWaterlike) return 8-WD.meta(aWorld, aX, aY, aZ);
		if (aBlock == Blocks.WATER || aBlock == Blocks.WATER) return 8-WD.meta(aWorld, aX, aY, aZ);
		return -1;
	}

	// F5-B (реверс воды mc26): движок применяет погружение/утопление/плавание/push/current/туман ТОЛЬКО через
	// getFluidState(pos), и entity отслеживает лишь fluid в теге FluidTags.WATER (EntityFluidInteraction:251 +
	// getTrackerFor:124 — иначе tracker=null и никаких эффектов; isInWater = isInFluid(FluidTags.WATER)). Мировая
	// вода GT6 (Ocean/River/Swamp, Material.water) отдаёт vanilla WATER FluidState по своим квантам (meta 0 =
	// source полный; meta>0 = flowing, amount = quantaPerBlock-meta) → игрок ведёт себя как в воде mc26, включая
	// весь vanilla-рендер воды. Кванты и разлив остаются на GT6 (updateFlow); vanilla fluid-tick НЕ планируется
	// (блок не LiquidBlock, scheduleTick — только свой block-tick), двойного разлива нет.
	@Override protected net.minecraft.world.level.material.FluidState getFluidState(net.minecraft.world.level.block.state.BlockState aState) {
		int tMeta = aState.getValue(FLUID_META);
		if (tMeta <= 0) return net.minecraft.world.level.material.Fluids.WATER.defaultFluidState();
		int tAmount = net.minecraft.util.Mth.clamp(quantaPerBlock - tMeta, 1, 8);
		return net.minecraft.world.level.material.Fluids.FLOWING_WATER.getFlowing(tAmount, false);
	}

	// F5-B block-контракт (ПЕРЕНОС эталона vanilla LiquidBlock, decisions/F-water-vanilla-to-gt6.md): вода — НЕ baked-model
	// блок, а рисуется ЧЕРЕЗ getFluidState→WATER (neo FluidRenderer); проходима и прозрачна. Без этих 4 override neo рисовал
	// missing-model (getRenderShape дефолт MODEL + нет json) и полный коллайдер (в воду нельзя войти). 1:1 к LiquidBlock:
	// getRenderShape:136 INVISIBLE, getShape:147 empty, getCollisionShape:82 empty (проходима), propagatesSkylightDown:115 false.
	@Override protected net.minecraft.world.level.block.RenderShape getRenderShape(net.minecraft.world.level.block.state.BlockState aState) {return net.minecraft.world.level.block.RenderShape.INVISIBLE;}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aLevel, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {return net.minecraft.world.phys.shapes.Shapes.empty();}
	@Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState aState, BlockGetter aLevel, BlockPos aPos, net.minecraft.world.phys.shapes.CollisionContext aContext) {return net.minecraft.world.phys.shapes.Shapes.empty();}
	@Override protected boolean propagatesSkylightDown(net.minecraft.world.level.block.state.BlockState aState) {return false;}

	// R1-заморозка (зимние биомы): vanilla замораживает воду в холодном биоме, но Biome.shouldFreeze:161 требует
	// blockState instanceof LiquidBlock — GT6-вода им НЕ является → не мёрзла. Воспроизводим УСЛОВИЯ vanilla 1:1
	// (Biome.shouldFreeze:154-172: !warmEnoughToRain + brightness(BLOCK)<10 + source + НЕ окружён водой=край), механизм —
	// randomTick GT6-блока (vanilla делает это через ServerLevel.tickChunk по heightmap; здесь — surface-check above).
	// Оригинал GT6 1.7.10 мёрз через vanilla (Material.water); в neo instanceof-хардкод → свой перенос. Ставит Blocks.ICE.
	@Override protected boolean isRandomlyTicking(net.minecraft.world.level.block.state.BlockState aState) {return true;}
	@Override protected void randomTick(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.server.level.ServerLevel aLevel, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		if (WD.meta(aLevel, aPos.getX(), aPos.getY(), aPos.getZ()) != 0) return;                                  // только source (полный блок)
		if (!aLevel.getBlockState(aPos.above()).isAir()) return;                                                  // только верхний открытый слой (surface)
		net.minecraft.world.level.biome.Biome tBiome = aLevel.getBiome(aPos).value();
		if (tBiome.warmEnoughToRain(aPos, aLevel.getSeaLevel())) return;                                          // тёплый биом — не мёрзнет
		if (aLevel.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, aPos) >= 10) return;                 // у источника света — не мёрзнет
		if (aLevel.isWaterAt(aPos.west()) && aLevel.isWaterAt(aPos.east()) && aLevel.isWaterAt(aPos.north()) && aLevel.isWaterAt(aPos.south())) return; // окружён водой — не мёрзнет (только края)
		aLevel.setBlockAndUpdate(aPos, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState());
	}

	// @Override
	public boolean shouldSideBeRendered(BlockGetter aWorld, int aX, int aY, int aZ, int aSide) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == NB) return T;
		if (WD.getMaterial(aBlock) == Material.water || WD.visOpq(aBlock)) return F;
		if (aWorld.getBlockState(new BlockPos(aX, aY, aZ)).isAir()) return T; // было aBlock.isAir(world,x,y,z) — BlockState.isAir()
		BlockEntity tTileEntity = WD.te(aWorld, aX, aY, aZ, T);
		if (tTileEntity instanceof ITileEntitySurface) return !((ITileEntitySurface)tTileEntity).isSurfaceOpaque(OPOS[aSide]);
		return T;
	}
	
	public boolean isSourceBlock(BlockGetter aWorld, int aX, int aY, int aZ) {return WD.block(aWorld, aX, aY, aZ) instanceof BlockWaterlike && WD.meta(aWorld, aX, aY, aZ) == 0;}
	@Override public Block getBlock() {return this;}
	public final String getUnlocalizedName() {return FL.name(mFluid, F);}
	public String getLocalizedName() {return FL.name(mFluid, T);}
	public void registerBlockIcons(IIconRegister aIconRegister) {/**/}
	public int getRenderType() {return RendererBlockFluid.RENDER_ID;}
	public int getRenderBlockPass() {return 1;}
	public int getLightOpacity() {return LIGHT_OPACITY_WATER;}
	public IIcon getIcon(int aSide, int aMeta) {throw new UnsupportedOperationException("PORT-TODO(F3, fluid icon): было Blocks.water.getIcon — 1.7.10 IIcon-атлас мёртв, реальный рендер RegisterFluidModelsEvent; crash-only per /goal");}
	public int getRenderColor(int aMeta) {throw new UnsupportedOperationException("PORT-TODO(F3, fluid tint): neo-тинт FluidTintSources; crash-only per /goal");}
	public int colorMultiplier(BlockGetter aWorld, int aX, int aY, int aZ) {throw new UnsupportedOperationException("PORT-TODO(F3, fluid tint): neo-тинт FluidTintSources; crash-only per /goal");}
	
	public int getFireSpreadSpeed(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return 0;}
	public int getFlammability(BlockGetter aWorld, int aX, int aY, int aZ, Direction aDirection) {return 0;}
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.canDisplace(aWorld, aX, aY, aZ);}
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {return !WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isLiquid() && super.displaceIfPossible(aWorld, aX, aY, aZ);}
	public boolean canCollideCheck(int aMeta, boolean aFullHit) {return aFullHit && aMeta == 0;}
	public boolean getBlocksMovement(BlockGetter aWorld, int aX, int aY, int aZ) {return !mEffects.isEmpty();}
	public boolean isNormalCube() {return F;}
	public boolean isOpaqueCube() {return F;}
	public boolean func_149730_j() {return F;}
	public boolean getTickRandomly() {return F;}
	public boolean renderAsNormalBlock() {return F;}
	public boolean isAir(BlockGetter aWorld, int aX, int aY, int aZ) {return F;}
	public boolean isSideSolid(BlockGetter aWorld, int aX, int aY, int aZ, Direction aSide) {return F;}
	
	public BlockWaterlike addEffect(int aEffectID, int aEffectDuration, int aEffectLevel) {
		mEffects.add(new int[] {aEffectID, aEffectDuration, aEffectLevel});
		return this;
	}
	
	public List<int[]> mEffects = new ArrayListNoNulls<>();
	
	@Override
	public void onHeadInside(LivingEntity aEntity, Level aWorld, int aX, int aY, int aZ) {
		if (!aWorld.isClientSide() && !mEffects.isEmpty() && (FL.gas(mFluid) ? !UT.Entities.isImmuneToBreathingGases(aEntity) : !UT.Entities.isWearingFullChemHazmat(aEntity))) {
			for (int[] tEffects : mEffects) UT.Entities.applyPotion(aEntity, tEffects[0], tEffects[1], tEffects[2], F);
			if (getMaterial() != Material.water && SERVER_TIME % 20 == 0) aEntity.hurt(aWorld.damageSources().drown(), 2.0F); // было attackEntityFrom(DamageSource.drown,...) — 1.7.10 static DamageSource-поля удалены; DamageSources.drown() (GT_API_Proxy.java:744 precedent)
		}
	}
}
