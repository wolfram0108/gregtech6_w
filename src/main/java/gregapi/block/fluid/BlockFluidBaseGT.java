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

package gregapi.block.fluid;

import gregapi.block.IBlock;
import gregapi.block.Material;
import gregapi.util.WD;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

import static gregapi.data.CS.*;

/**
 * F5 форс движка (decisions/F5-fluids.md §5): в 1.7.10 {@link BlockWaterlike} и {@link BlockBaseFluid} делил
 * ОДИН общий предок — Forge {@code net.minecraftforge.fluids.BlockFluidBase} (quanta-текучесть: quantaPerBlock/
 * density/densityDir/tickRate/displacements-поля + canDisplace/displaceIfPossible/getDensity/
 * getQuantaValueBelow-методы). Класс удалён в neo (ни в одном из 3 корней референса) — GT6 сама этот класс
 * никогда не писала (сторонняя Forge-библиотека), поэтому предок воспроизведён здесь ОДИН раз (централизация
 * §3, F5-доклад §5 "кастомный Block-базовый класс"), тела 1:1 из Forge 1.7.10
 * {@code BlockFluidBase}/{@code BlockFluidClassic}/{@code BlockFluidFinite} (только API-свод под
 * BlockGetter/Level/BlockPos вместо IBlockAccess/World/int-тройки; {@code Material.func_149688_o()} ->
 * {@link WD#getMaterial(Block)}, {@code material.func_76230_c()} -> {@code Material.blocksMovement()},
 * {@code Material.field_151567_E} -> {@code Material.portal} — сверено `methods.csv`/`fields.csv` MCP 1.7.10).
 * Реально используемая GT6-логика quanta-потока (Ocean/River/Swamp updateTick, BlockBaseFluid.updateTick) —
 * СОБСТВЕННАЯ, не отсюда; сюда попало только то, что реально вызывается через unqualified/{@code super.}-имя
 * из {@link BlockWaterlike}/{@link BlockBaseFluid} (canDisplace/displaceIfPossible/getQuantaValueBelow/
 * getDensity) — мёртвый в GT6 {@code BlockFluidClassic}-tick-хвост (getOptimalFlowDirections/
 * calculateFlowCost/flowIntoBlock/canFlowInto/isFlowingVertically — никогда не вызывается, GT6 переопределяет
 * тик целиком в Ocean/River/Swamp и никогда не зовёт {@code super.updateTick}) не портирован — не выдумываем
 * мёртвый код.
 */
public abstract class BlockFluidBaseGT extends Block implements IBlock, gregapi.block.IBlockExtendedMetaData, gregapi.render.IRenderedBlock {
	/** было Forge {@code BlockFluidBase.displacements} + статический {@code defaultDisplacements}
	 *  (wooden_door/iron_door/standing_sign/wall_sign/reeds -> false). F5 данные-дефолт (door/sign/reeds не вытесняются жидкостью — набор блоков, не заглушка):
	 *  1.7.10 знал ОДИН блок на дверь/вывеску; neo расщепил на блок-на-древесину (нет 1:1 отображения без
	 *  угадывания полного списка — REMAP-RULES «не выдумывать»), карта оставлена пустой (безопасный дефолт:
	 *  двери/вывески в material.blocksMovement()-ветке и так возвращают false). */
	protected Map<Block, Boolean> displacements = new HashMap<>();

	protected int quantaPerBlock = 8;
	protected float quantaPerBlockFloat = 8F;
	protected int density = 1;
	protected int densityDir = -1;
	protected int tickRate = 20;

	/** F9: см. {@link gregapi.block.BlockBase#getMaterial()} — тот же приём (собственное поле вместо
	 *  удалённого neo {@code Material}-конструктора Block'а). */
	protected final Material mMaterial;
	public Material getMaterial() {return mMaterial;}

	/** F-bounds: см. {@link gregapi.block.BlockBase#setBlockBounds} — тот же центр-приём, разделяемый ОБОИМИ
	 *  fluid-блоками (было Forge {@code Block.setBlockBounds} внутри {@code BlockFluidBase}-конструктора). */
	protected float[] mRenderBounds = {0, 0, 0, 1, 1, 1};
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		mRenderBounds = new float[] {aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ};
	}
	/** тот же контракт, что {@link gregapi.block.BlockBase#getRenderBounds()} — читает GT6BlockModel.applyBounds
	 *  (без этого кванта-высота жидкости терялась и блок рисовался полным кубом). */
	public float[] getRenderBounds() {return mRenderBounds;}

	/** F16/F9 форс движка: было {@code BlockFluidBase(Fluid,Material)}, читавший density/temperature/
	 *  maxScaledLight/tickRate/densityDir ИЗ САМОГО Forge {@code Fluid}-объекта (data-holder-поля) — neo
	 *  {@code net.minecraft.world.level.material.Fluid} этих полей не несёт (данные расщеплены в
	 *  {@code FluidType}, F5-доклад §1/§3). Перенос характеристик воспроизведён Fluid-перегрузкой ниже
	 *  (данные из {@link gregapi.fluid.FluidGT}); эта 2-арг перегрузка оставляет Forge-дефолты
	 *  (density=1, densityDir=-1, tickRate=20, quantaPerBlock=8). */
	public BlockFluidBaseGT(BlockBehaviour.Properties aProperties, Material aMaterial) {
		super(aProperties);
		mMaterial = aMaterial;
		registerDefaultState(getStateDefinition().any().setValue(FLUID_META, 0));
	}

	/** Перенос характеристик Fluid→блок 1:1 с Forge {@code BlockFluidBase(Fluid,Material)} (:68-72):
	 *  {@code density = fluid.density; tickRate = fluid.viscosity / 200; densityDir = density > 0 ? -1 : 1}.
	 *  В neo data-holder-поля Fluid'а живут в {@link gregapi.fluid.FluidGT} (F5) — центр {@code FluidGT.of(Fluid)}.
	 *  Отсюда: газ (density −500) течёт ВВЕРХ (densityDir=+1), нефти несут плотности 600-900, воды 1000;
	 *  tickRate: LIQUID 1000/200=5 (как vanilla-вода), GAS 200/200=1. Подклассы, которым нужен иной tickRate,
	 *  переставляют его ПОСЛЕ super (Ocean/River/Swamp 20/20/10 — 1:1 с исходником).
	 *  {@code maxScaledLight} (luminosity) НЕ перенесён: у всех 10 мировых жидкостей luminosity=0
	 *  (Loader_Fluids: воды/нефти/газ без setLuminosity) — мёртвое поле не выдумываем.
	 *  {@code temperature} НЕ перенесён: в порту никто не читает (Forge-static getTemperature не портирован). */
	public BlockFluidBaseGT(BlockBehaviour.Properties aProperties, Material aMaterial, net.minecraft.world.level.material.Fluid aFluid) {
		this(aProperties, aMaterial);
		gregapi.fluid.FluidGT tFluid = gregapi.fluid.FluidGT.of(aFluid);
		if (tFluid != null) {
			density    = tFluid.getDensity();
			tickRate   = tFluid.getViscosity() / 200;
			densityDir = tFluid.getDensity() > 0 ? -1 : 1;
		}
	}

	// МОДЕЛЬ МЕТЫ (кванты 1.7.10): Forge BlockFluidFinite хранил кванты В МЕТЕ блока (0..7 → 1..8 квант);
	// neo-носитель числовой меты = blockstate-property (как vanilla LiquidBlock.LEVEL 0..15). Канал WD.set/WD.meta
	// (IBlockExtendedMetaData) → вся дословная quanta-логика (updateTick/drain/updateFluidBlocks) оживает без правок.
	public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLUID_META =
		net.minecraft.world.level.block.state.properties.IntegerProperty.create("gt6_meta", 0, 15);

	@Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(FLUID_META);
	}

	public void setExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ, short aMetaData) {
		if (!(aWorld instanceof net.minecraft.world.level.LevelAccessor tLevel)) return;
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		BlockState tState = tLevel.getBlockState(tPos);
		if (tState.getBlock() == this) tLevel.setBlock(tPos, tState.setValue(FLUID_META, aMetaData & 15), FLUID_UPDATE_FLAGS_META);
	}
	public short getExtendedMetaData(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockState tState = aWorld.getBlockState(new BlockPos(aX, aY, aZ));
		return (short)(tState.getBlock() == this ? tState.getValue(FLUID_META) : 0);
	}
	/** флаг 2 (SEND_TO_CLIENT без соседей) — мета-запись не должна каскадить апдейты (каскад делает сама GT6-логика). */
	protected static final int FLUID_UPDATE_FLAGS_META = 2;

	// F-tick жидкостей: 1.7.10 World.scheduleBlockUpdate → Block.updateTick; neo — BlockBehaviour.tick.
	// onBlockAdded (Forge BlockFluidBase) планировал первый тик — neo onPlace 1:1.
	public void updateTick(Level aWorld, int aX, int aY, int aZ, java.util.Random aRandom) {/* переопределяют BlockBaseFluid/Ocean/River/Swamp */}
	@Override protected void tick(BlockState aState, net.minecraft.server.level.ServerLevel aWorld, BlockPos aPos, net.minecraft.util.RandomSource aRandom) {
		updateTick(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), gregapi.util.UT.Code.random(aRandom)); // конвертер — ЦЕНТР UT.Code.random
	}
	@Override protected void onPlace(BlockState aState, Level aWorld, BlockPos aPos, BlockState aOldState, boolean aMovedByPiston) {
		onBlockAdded(aWorld, aPos.getX(), aPos.getY(), aPos.getZ());
	}
	/** было Forge {@code BlockFluidBase.onBlockAdded(World,x,y,z)} (:227-230) — тело 1:1. Диспатч из onPlace
	 *  ОБЯЗАТЕЛЕН: Ocean/River/Swamp переопределяют (PLACEMENT_ALLOWED-гейт + стартовый тик 10+rand(90)) —
	 *  без диспатча их канал был сиротой (болото не тикало → грязь не конвертировалась). */
	public void onBlockAdded(Level aWorld, int aX, int aY, int aZ) {
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
	}
	@Override protected void neighborChanged(BlockState aState, Level aWorld, BlockPos aPos, Block aBlock, net.minecraft.world.level.redstone.Orientation aOrientation, boolean aMovedByPiston) {
		onNeighborBlockChange(aWorld, aPos.getX(), aPos.getY(), aPos.getZ(), aBlock);
	}

	public abstract int getQuantaValue(BlockGetter aWorld, int aX, int aY, int aZ);

	/** было Forge {@code BlockFluidBase.onNeighborBlockChange(World,x,y,z,Block)} (func_149695_a) — тело 1:1.
	 *  Нужен {@link gregtech.blocks.fluids.BlockOcean}/{@link gregtech.blocks.fluids.BlockRiver}, которые зовут
	 *  {@code super.onNeighborBlockChange(...)} после своей собственной логики. */
	public void onNeighborBlockChange(Level aWorld, int aX, int aY, int aZ, Block aBlock) {
		aWorld.scheduleTick(new BlockPos(aX, aY, aZ), this, tickRate);
	}

	/** было Forge {@code BlockFluidBase.canDisplace(IBlockAccess,x,y,z)} — тело 1:1. */
	public boolean canDisplace(BlockGetter aWorld, int aX, int aY, int aZ) {
		BlockPos aPos = new BlockPos(aX, aY, aZ);
		if (aWorld.getBlockState(aPos).isAir()) return T; // было block.isAir(world,x,y,z) — BlockState.isAir() (BlockBehaviour.java:575)
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == this) return F;
		if (displacements.containsKey(aBlock)) return displacements.get(aBlock);
		Material aBlockMaterial = WD.getMaterial(aBlock);
		if (aBlockMaterial.blocksMovement() || aBlockMaterial == Material.portal) return F;
		int tDensity = getDensity(aWorld, aX, aY, aZ);
		if (tDensity == Integer.MAX_VALUE) return T;
		return this.density > tDensity;
	}

	/** было Forge {@code BlockFluidBase.displaceIfPossible(World,x,y,z)} — тело 1:1. F5 (1:1): при density==MAX_VALUE
	 *  Forge-оригинал ронял вытесняемый блок ({@code block.dropBlockAsItem}) ДО вытеснения → neo Block.dropResources
	 *  (Block.java:380). Дроп восстановлен (был отложен как silent no-op). Отличие от canDisplace — только этот побочный drop. */
	public boolean displaceIfPossible(Level aWorld, int aX, int aY, int aZ) {
		BlockPos aPos = new BlockPos(aX, aY, aZ);
		if (aWorld.getBlockState(aPos).isAir()) return T;
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (aBlock == this) return F;
		if (displacements.containsKey(aBlock)) return displacements.get(aBlock);
		Material aBlockMaterial = WD.getMaterial(aBlock);
		if (aBlockMaterial.blocksMovement() || aBlockMaterial == Material.portal) return F;
		int tDensity = getDensity(aWorld, aX, aY, aZ);
		if (tDensity == Integer.MAX_VALUE) {
			if (aWorld instanceof net.minecraft.server.level.ServerLevel) net.minecraft.world.level.block.Block.dropResources(aWorld.getBlockState(aPos), aWorld, aPos); // Forge dropBlockAsItem вытесняемого блока
			return T;
		}
		return this.density > tDensity;
	}

	/** было Forge {@code BlockFluidBase.getDensity(IBlockAccess,x,y,z)} (static). */
	public static int getDensity(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block aBlock = WD.block(aWorld, aX, aY, aZ);
		if (!(aBlock instanceof BlockFluidBaseGT)) return Integer.MAX_VALUE;
		return ((BlockFluidBaseGT)aBlock).density;
	}

	/** было Forge {@code BlockFluidBase.getQuantaValueBelow(IBlockAccess,x,y,z,belowThis)} (final) — тело 1:1. */
	public final int getQuantaValueBelow(BlockGetter aWorld, int aX, int aY, int aZ, int aBelowThis) {
		int tQuantaRemaining = getQuantaValue(aWorld, aX, aY, aZ);
		if (tQuantaRemaining >= aBelowThis) return -1;
		return tQuantaRemaining;
	}

	/** аксессор densityDir для рендера (было 1.7.10 {@code FL.dir(BlockFluidBase)} / прямое поле). */
	public int dir() {return densityDir;}

	/** было Forge {@code BlockFluidBase.getQuantaPercentage(IBlockAccess,x,y,z)} (:452) — тело 1:1. */
	public final float getQuantaPercentage(BlockGetter aWorld, int aX, int aY, int aZ) {
		return getQuantaValue(aWorld, aX, aY, aZ) / quantaPerBlockFloat;
	}

	/** было 1.7.10 {@code Block.isBlockSolid(IBlockAccess,x,y,z,side)} — тело {@code material.isSolid()}
	 *  (тот же приём, что {@link gregtech.blocks.fluids.BlockWaterlike}). */
	protected boolean isBlockSolid(BlockGetter aWorld, int aX, int aY, int aZ, byte aSide) {
		return WD.getMaterial(WD.block(aWorld, aX, aY, aZ)).isSolid();
	}

	/** было Forge {@code BlockFluidBase.getFlowVector(IBlockAccess,x,y,z)} (:458-515) — тело 1:1
	 *  (Vec3.createVectorHelper→new Vec3, addVector→add; {@code (y-y)*power}=0 свёрнут). Читает рендер
	 *  ({@link gregapi.render.RendererBlockFluid} — поворот текстуры поверхности по направлению потока). */
	public net.minecraft.world.phys.Vec3 getFlowVector(BlockGetter aWorld, int aX, int aY, int aZ) {
		net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(0, 0, 0);
		int decay = quantaPerBlock - getQuantaValue(aWorld, aX, aY, aZ);
		for (int side = 0; side < 4; ++side) {
			int x2 = aX, z2 = aZ;
			switch (side) {
			case 0: --x2; break;
			case 1: --z2; break;
			case 2: ++x2; break;
			default: ++z2; break;
			}
			int otherDecay = quantaPerBlock - getQuantaValue(aWorld, x2, aY, z2);
			if (otherDecay >= quantaPerBlock) {
				if (!WD.getMaterial(WD.block(aWorld, x2, aY, z2)).blocksMovement()) {
					otherDecay = quantaPerBlock - getQuantaValue(aWorld, x2, aY - 1, z2);
					if (otherDecay >= 0) {
						int power = otherDecay - (decay - quantaPerBlock);
						vec = vec.add((x2 - aX) * power, 0, (z2 - aZ) * power);
					}
				}
			} else if (otherDecay >= 0) {
				int power = otherDecay - decay;
				vec = vec.add((x2 - aX) * power, 0, (z2 - aZ) * power);
			}
		}
		if (WD.block(aWorld, aX, aY + 1, aZ) == this) {
			boolean flag =
				isBlockSolid(aWorld, aX    , aY    , aZ - 1, (byte)2) ||
				isBlockSolid(aWorld, aX    , aY    , aZ + 1, (byte)3) ||
				isBlockSolid(aWorld, aX - 1, aY    , aZ    , (byte)4) ||
				isBlockSolid(aWorld, aX + 1, aY    , aZ    , (byte)5) ||
				isBlockSolid(aWorld, aX    , aY + 1, aZ - 1, (byte)2) ||
				isBlockSolid(aWorld, aX    , aY + 1, aZ + 1, (byte)3) ||
				isBlockSolid(aWorld, aX - 1, aY + 1, aZ    , (byte)4) ||
				isBlockSolid(aWorld, aX + 1, aY + 1, aZ    , (byte)5);
			if (flag) vec = vec.normalize().add(0.0D, -6.0D, 0.0D);
		}
		return vec.normalize();
	}

	/** было Forge {@code BlockFluidBase.getFlowDirection(IBlockAccess,x,y,z)} (static :421-430) — тело 1:1
	 *  (+instanceof-гейт перед кастом: зовётся только на позиции самой жидкости, семантика не меняется). */
	public static double getFlowDirection(BlockGetter aWorld, int aX, int aY, int aZ) {
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (!(tBlock instanceof BlockFluidBaseGT) || !WD.getMaterial(tBlock).isLiquid()) return -1000.0D;
		net.minecraft.world.phys.Vec3 vec = ((BlockFluidBaseGT)tBlock).getFlowVector(aWorld, aX, aY, aZ);
		return vec.x == 0.0D && vec.z == 0.0D ? -1000.0D : Math.atan2(vec.z, vec.x) - Math.PI / 2D;
	}

	// ================================ F3-render: РЕНДЕР ОБЕИХ ЖИДКОСТНЫХ ИЕРАРХИЙ — ЗДЕСЬ ================================
	// В 1.7.10 рендер жидкостных блоков был ЦЕНТРАЛИЗОВАН у самого Грегориуса: и BlockWaterlike (:197), и BlockBaseFluid
	// отдавали ОДИН и тот же getRenderType() = RendererBlockFluid.RENDER_ID — один ISimpleBlockRenderingHandler на обе
	// иерархии, ровно потому, что у них общий предок (Forge BlockFluidBase). Порт восстановил neo-эквивалент этого канала
	// (IRenderedBlock → GT6BlockModel/GT6ItemModel) ТОЛЬКО у BlockBaseFluid — водоподобные (река/океан/болото) остались вне
	// канала: onModifyBakingResult (GT_API_Proxy_Client:258) инжектит item-модель лишь блокам-IRenderedBlock, а JSON-моделей
	// в моде нет вовсе (мод процедурный) → у их BlockItem не было НИКАКОЙ модели → пурпурная заглушка (BUG-068).
	// Приём восстановления — тот же, что был у Грега: канал объявлен ОДИН РАЗ, в общем предке, и обслуживает обе иерархии.
	// Различие между ними ровно одно и живёт в потомке — какая текстура (renderTexture): у BlockBaseFluid своя жидкость
	// (mFluid.getStillIcon 1.7.10), у BlockWaterlike ВАНИЛЬНАЯ вода (1.7.10 :200 getIcon → Blocks.water.getIcon).
	// МИРОВОЙ рендер этим не задевается: у водоподобных getRenderShape()==INVISIBLE, а движок собирает модельные квады
	// только для RenderShape.MODEL (SectionCompiler.java:106 декомпила) — вода как рисовалась vanilla FluidRenderer'ом по
	// getFluidState (F5-B), так и рисуется. У BlockBaseFluid RenderShape дефолтный (MODEL) — его мировой рендер как был.

	// ================= F3 light-opacity ЦЕНТР: сколько света гасит жидкость ==================================
	// 1.7.10 спрашивал у блока getLightOpacity(), и ОБЕ жидкостные иерархии отвечали одинаково —
	// LIGHT_OPACITY_WATER=3 (gregtech6/.../BlockWaterlike.java:199 и .../BlockBaseFluid.java:367). В порте это
	// значение лежало КОПИЕЙ в обоих потомках, а движок его не спрашивал вовсе: neo считает затухание из
	// BlockState — LightEngine.getOpacity:85-87 берёт state.getLightDampening(), а тот заполняется ОДИН раз при
	// сборке состояния (BlockBehaviour.java:518) вызовом блочного getLightDampening(BlockState). Методы
	// 1.7.10-сигнатуры остались без вызывателей => вода GT6 не затемняла глубину: дефолт давал 1 вместо 3
	// (BlockBehaviour.java:290-295: не solid + propagatesSkylightDown=false → 1).
	// Мост объявлен ОДИН РАЗ здесь, в общем предке обеих иерархий, обе копии значения сняты.
	// ⚠️ Ограничение движка: getLightDampening видит ТОЛЬКО состояние. Контекстные версии оригинала
	// (BlockOcean:164 — «источник, над ним два воздуха, снизу пропускает свет → 16»; BlockSwamp:198 — «сверху
	// болото → 255») спрашивали СОСЕДЕЙ, чего в этом канале нет. Выразимое по состоянию переносим (BlockSwamp),
	// невыразимое идёт в реестр отложенного, а не в тихую заглушку.
	@Override protected int getLightDampening(net.minecraft.world.level.block.state.BlockState aState) {return getLightOpacity(aState);}

	/** Затухание света для конкретного состояния. Общее значение обеих иерархий 1.7.10 — {@code LIGHT_OPACITY_WATER}. */
	public int getLightOpacity(net.minecraft.world.level.block.state.BlockState aState) {return gregapi.data.CS.LIGHT_OPACITY_WATER;}

	/** Текстура жидкости для обеих веток рендера (мир + item-форма). Клиент-only: {@code BlockTextureFluid.get} под {@code CODE_CLIENT}. */
	public abstract gregapi.render.ITexture renderTexture();

	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, net.minecraft.world.item.ItemStack aStack) {return renderTexture();}
	@Override public gregapi.render.ITexture getTexture(int aRenderPass, byte aSide, boolean[] aShouldSideBeRendered, BlockGetter aWorld, int aX, int aY, int aZ) {return renderTexture();}
	@Override public boolean usesRenderPass(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return aRenderPass == 0;}
	@Override public boolean usesRenderPass(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return aRenderPass == 0;}
	@Override public boolean setBlockBounds(int aRenderPass, net.minecraft.world.item.ItemStack aStack) {return F;}
	/** дефолт — полный куб; квантовую высоту поверхности переопределяет {@link BlockBaseFluid} (мировой рендер своей жидкости). */
	@Override public boolean setBlockBounds(int aRenderPass, BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return F;}
	@Override public int getRenderPasses(net.minecraft.world.item.ItemStack aStack) {return 1;}
	@Override public int getRenderPasses(BlockGetter aWorld, int aX, int aY, int aZ, boolean[] aShouldSideBeRendered) {return 1;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(net.minecraft.world.item.ItemStack aStack) {return null;}
	@Override public gregapi.render.IRenderedBlockObject passRenderingToObject(BlockGetter aWorld, int aX, int aY, int aZ) {return null;}
}
