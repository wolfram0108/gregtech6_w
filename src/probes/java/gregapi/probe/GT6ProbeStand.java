/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregapi.probe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// ============================================================================================================
// ИНФРАСТРУКТУРА ЖИВЫХ СТЕНДОВ Ф3.1 (LIVE-PROBE-MANUAL.md) — убрать вместе с последним стендом.
// Сам по себе побочных эффектов не имеет: чистые static-хелперы (постройка/данные/судьи/каналы) + один
// объект-стенд (Seq — тик-машина с маркером, счётчиками pass/fail, DONE/EXC/heartbeat). Вызывается ТОЛЬКО
// из проб под существующими гейтами CS.probeFlag(...). Выделено из двух принятых стендов (WIREPROBE,
// FLUIDPIPEPROBE в GT_API_Proxy.java): всё, что повторялось в обоих (клик-установка MTE реальным useOn,
// анкер-STONE, свежий стек на установку, тик-машина с DONE/EXC/heartbeat, единый формат судьи), ушло сюда;
// уникальная схема и ожидания каждого стенда остаются в самом стенде. Методология — LIVE-PROBE-MANUAL.md
// §1-§9 (не переписывать), §10 — краткий гид по этому каркасу.
// ============================================================================================================
public final class GT6ProbeStand {
	private GT6ProbeStand() {}

	private static final java.io.PrintStream O = gregapi.data.CS.OUT;

	// ================================================================================================
	// ПОСТРОЙКА
	// ================================================================================================

	/** Урок №1 (WIREPROBE): MultiTileEntityItemInternal.onItemUse ВСЕГДА делает count-- на успешной
	 *  установке — стек с count==0 молча проваливает установку (F, без исключения). Решение каркаса:
	 *  СВЕЖИЙ стек (count по умолчанию 1) из живого реестра "gt.multitileentity" на КАЖДЫЙ вызов —
	 *  проблема "стек истощился по дороге" структурно не может возникнуть. */
	public static net.minecraft.world.item.ItemStack mteStack(int aMteId) {return mteStack(aMteId, 1);}
	public static net.minecraft.world.item.ItemStack mteStack(int aMteId, int aCount) {
		gregapi.block.multitileentity.MultiTileEntityRegistry tReg = gregapi.block.multitileentity.MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		return tReg == null ? net.minecraft.world.item.ItemStack.EMPTY : tReg.getItem(aMteId, aCount);
	}

	/** Установка ОДНОГО MTE-блока реальным каналом игрока (stack.useOn(UseOnContext), сверено с принятыми
	 *  WIREPROBE/FLUIDPIPEPROBE). Ловушки, закрытые здесь: (1) твёрдый анкер — если по aAnchorPos нет
	 *  BlockEntity (то есть это НЕ уже поставленный этим же стендом MTE-блок), клетка принудительно
	 *  становится STONE — клик всегда идёт по реальному твёрдому блоку; (2) цель (aAnchorPos.relative(aFace))
	 *  расчищается в AIR непосредственно перед кликом; (3) свежий стек — {@link #mteStack(int)}; (4) верификация
	 *  класса BE ПОСЛЕ установки — при несовпадении печатает DIAG (маркер, метка, координаты, фактический класс
	 *  BE, блок в мире) и возвращает null, вместо ClassCastException выше по стеку. */
	public static <T extends net.minecraft.world.level.block.entity.BlockEntity> T place(
			net.minecraft.server.level.ServerLevel aLevel, net.minecraft.server.level.ServerPlayer aPlayer,
			net.minecraft.core.BlockPos aAnchorPos, net.minecraft.core.Direction aFace, net.minecraft.world.item.ItemStack aItem,
			Class<T> aExpectedClass, String aMarker, String aLabel) {
		net.minecraft.core.BlockPos tTarget = clickWith(aLevel, aPlayer, aAnchorPos, aFace, aItem);
		net.minecraft.world.level.block.entity.BlockEntity tBE = aLevel.getBlockEntity(tTarget);
		if (!aExpectedClass.isInstance(tBE)) {
			O.println("[" + aMarker + "] DIAG " + aLabel + " не встал @" + tTarget + " BE=" + (tBE == null ? "null" : tBE.getClass().getSimpleName()) + " блок=" + aLevel.getBlockState(tTarget).getBlock());
			return null;
		}
		return aExpectedClass.cast(tBE);
	}

	/** То же самое для блоков БЕЗ BlockEntity (дерево, листва, камень GT6): путь установки тот же — стек в руку и
	 *  {@code useOn} — но верификация по БЛОКУ, а не по BE (иначе {@link #place} отбрасывает верно поставленный
	 *  блок только потому, что у него нет BE). Возвращает поставленную позицию либо null. */
	public static net.minecraft.core.BlockPos placeBlock(
			net.minecraft.server.level.ServerLevel aLevel, net.minecraft.server.level.ServerPlayer aPlayer,
			net.minecraft.core.BlockPos aAnchorPos, net.minecraft.core.Direction aFace, net.minecraft.world.item.ItemStack aItem,
			String aMarker, String aLabel) {
		net.minecraft.core.BlockPos tTarget = clickWith(aLevel, aPlayer, aAnchorPos, aFace, aItem);
		if (aLevel.getBlockState(tTarget).isAir()) {
			O.println("[" + aMarker + "] DIAG " + aLabel + " не встал @" + tTarget + " (осталось пусто)");
			return null;
		}
		return tTarget;
	}

	/** Общий приём установки (один на оба варианта выше): твёрдый анкер → расчистка цели → стек в руку → useOn. */
	private static net.minecraft.core.BlockPos clickWith(
			net.minecraft.server.level.ServerLevel aLevel, net.minecraft.server.level.ServerPlayer aPlayer,
			net.minecraft.core.BlockPos aAnchorPos, net.minecraft.core.Direction aFace, net.minecraft.world.item.ItemStack aItem) {
		if (aLevel.getBlockEntity(aAnchorPos) == null) aLevel.setBlock(aAnchorPos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3); // твёрдый анкер
		net.minecraft.core.BlockPos tTarget = aAnchorPos.relative(aFace);
		aLevel.setBlock(tTarget, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); // расчистка цели под установку
		aPlayer.getInventory().setItem(0, aItem); aPlayer.getInventory().setSelectedSlot(0);
		net.minecraft.world.phys.Vec3 tHit = net.minecraft.world.phys.Vec3.atCenterOf(aAnchorPos).add(aFace.getStepX()*0.5, aFace.getStepY()*0.5, aFace.getStepZ()*0.5);
		// ПРИСЕД НА ВРЕМЯ УСТАНОВКИ: часть MTE ставится ТОЛЬКО в приседе (IMTE_OnlyPlaceableWhenSneaking —
		// батареи и т.п., в оригинале ровно так же: TileEntityBase08Battery:54). Стенд без приседа их не ставил,
		// и 443 блока молча выпадали из проверки как «не встали».
		boolean tWasSneaking = aPlayer.isShiftKeyDown();
		aPlayer.setShiftKeyDown(true);
		try {
			aPlayer.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(aPlayer, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(tHit, aFace, aAnchorPos, false)));
		} finally {aPlayer.setShiftKeyDown(tWasSneaking);}
		return tTarget;
	}

	/** Линия из aN однотипных MTE вдоль aDir: каждая клетка кликается ПО ПРЕДЫДУЩЕЙ (walk, тот же приём,
	 *  что связывал провод/трубу в исходных стендах — geometrически итог не зависит от того, горизонтальная
	 *  линия или вертикальная колонна, обе — один и тот же цикл {@link #place}). aFrom — позиция уже
	 *  поставленного "якорного" блока (источник и т.п.), с которого начинается клик-цепочка; результат[i]==null
	 *  и все последующие — сигнал обрыва постройки (см. DIAG внутри place()). */
	@SuppressWarnings("unchecked")
	public static <T extends net.minecraft.world.level.block.entity.BlockEntity> T[] line(
			net.minecraft.server.level.ServerLevel aLevel, net.minecraft.server.level.ServerPlayer aPlayer,
			net.minecraft.core.BlockPos aFrom, net.minecraft.core.Direction aDir, int aN, int aMteId,
			Class<T> aExpectedClass, String aMarker) {
		T[] rOut = (T[]) java.lang.reflect.Array.newInstance(aExpectedClass, aN);
		net.minecraft.core.BlockPos tCursor = aFrom;
		for (int i = 0; i < aN; i++) {
			T tBE = place(aLevel, aPlayer, tCursor, aDir, mteStack(aMteId), aExpectedClass, aMarker, "звено[" + i + "]");
			rOut[i] = tBE;
			if (tBE == null) return rOut;
			tCursor = tCursor.relative(aDir);
		}
		return rOut;
	}

	/** Плоская площадка-опора aSizeX×aSizeZ из STONE в плоскости Y=aOrigin.getY() (гигиена под линиями/
	 *  колоннами — НЕ судимый канал, как в исходном WIREPROBE-«пол снизу везде»). */
	public static void solidPad(net.minecraft.server.level.ServerLevel aLevel, net.minecraft.core.BlockPos aOrigin, int aSizeX, int aSizeZ) {
		for (int x = 0; x < aSizeX; x++) for (int z = 0; z < aSizeZ; z++) aLevel.setBlock(aOrigin.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
	}

	/** Многоблочная структура из ASCII-схемы: aLayers[y] — один Y-слой (снизу вверх) как многострочная
	 *  строка, строки внутри — по Z, символы в строке — по X. legend: символ -> Integer(MTE-id) | Block |
	 *  BlockState; ' ' = пропуск клетки (существующий мир не трогаем), '.' = принудительный AIR. Для MTE-клеток
	 *  анкер — клетка НИЖЕ цели (твёрдый анкер force-STONE, если там ещё нет BE), клик face=UP — тот же приём,
	 *  что и {@link #place}; подходит для плотных построек снизу вверх (котлы/казённые корпуса реактора);
	 *  для структур с "висящими" клетками без опоры снизу потребуется отдельное расширение (не реализовано —
	 *  ни один из принятых стендов такого не требует). Возвращает по каждому символу список поставленных BE
	 *  (в порядке обхода слоёв снизу вверх, построчно). */
	public static Map<Character, List<net.minecraft.world.level.block.entity.BlockEntity>> pattern(
			net.minecraft.server.level.ServerLevel aLevel, net.minecraft.server.level.ServerPlayer aPlayer,
			net.minecraft.core.BlockPos aOrigin, String[] aLayers, Map<Character, Object> aLegend, String aMarker) {
		Map<Character, List<net.minecraft.world.level.block.entity.BlockEntity>> rResult = new HashMap<>();
		for (int y = 0; y < aLayers.length; y++) {
			String[] tRows = aLayers[y].split("\n", -1);
			for (int z = 0; z < tRows.length; z++) {
				String tRow = tRows[z];
				for (int x = 0; x < tRow.length(); x++) {
					char tC = tRow.charAt(x);
					if (tC == ' ') continue;
					net.minecraft.core.BlockPos tTarget = aOrigin.offset(x, y, z);
					if (tC == '.') {aLevel.setBlock(tTarget, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); continue;}
					Object tVal = aLegend.get(tC);
					if (tVal instanceof Integer tMteId) {
						net.minecraft.core.BlockPos tAnchor = tTarget.below();
						if (aLevel.getBlockEntity(tAnchor) == null) aLevel.setBlock(tAnchor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
						net.minecraft.world.level.block.entity.BlockEntity tBE = place(aLevel, aPlayer, tAnchor, net.minecraft.core.Direction.UP, mteStack(tMteId), net.minecraft.world.level.block.entity.BlockEntity.class, aMarker, "pattern['" + tC + "']@" + tTarget);
						rResult.computeIfAbsent(tC, k -> new ArrayList<>()).add(tBE);
					} else if (tVal instanceof net.minecraft.world.level.block.state.BlockState tState) {
						aLevel.setBlock(tTarget, tState, 3);
					} else if (tVal instanceof net.minecraft.world.level.block.Block tBlock) {
						aLevel.setBlock(tTarget, tBlock.defaultBlockState(), 3);
					}
				}
			}
		}
		return rResult;
	}

	// ================================================================================================
	// ДАННЫЕ (рефлексия по имени поля — для генерик-кода стендов, не знающего конкретный класс BE)
	// ================================================================================================

	private static java.lang.reflect.Field findField(Class<?> aCls, String aName) throws NoSuchFieldException {
		for (Class<?> c = aCls; c != null; c = c.getSuperclass()) {
			try {return c.getDeclaredField(aName);} catch (NoSuchFieldException ignored) {}
		}
		throw new NoSuchFieldException(aName);
	}

	public static Object fld(Object aBE, String aName) {
		try {java.lang.reflect.Field f = findField(aBE.getClass(), aName); f.setAccessible(true); return f.get(aBE);}
		catch (Throwable e) {O.println("[GT6PROBESTAND] DIAG поле не найдено: " + aName + " в классе " + aBE.getClass().getSimpleName() + " (" + e + ")"); return null;}
	}
	public static long   fldLong  (Object aBE, String aName) {Object v = fld(aBE, aName); return v instanceof Number n ? n.longValue()   : 0L;}
	public static int    fldInt   (Object aBE, String aName) {Object v = fld(aBE, aName); return v instanceof Number n ? n.intValue()    : 0;}
	public static double fldDouble(Object aBE, String aName) {Object v = fld(aBE, aName); return v instanceof Number n ? n.doubleValue() : 0.0;}
	public static boolean fldBool (Object aBE, String aName) {Object v = fld(aBE, aName); return v instanceof Boolean b && b;}

	/** Пишет значение (long/int/short/byte/double/float/boolean — коэрсия через Number/Boolean, никаких
	 *  ловушек ручного (byte)/(int) со стороны вызывающего). */
	public static void fldSet(Object aBE, String aName, Object aValue) {
		try {
			java.lang.reflect.Field f = findField(aBE.getClass(), aName); f.setAccessible(true);
			Class<?> tType = f.getType();
			if      (tType == long.class)    f.setLong   (aBE, ((Number) aValue).longValue());
			else if (tType == int.class)     f.setInt    (aBE, ((Number) aValue).intValue());
			else if (tType == short.class)   f.setShort  (aBE, ((Number) aValue).shortValue());
			else if (tType == byte.class)    f.setByte   (aBE, ((Number) aValue).byteValue());
			else if (tType == double.class)  f.setDouble (aBE, ((Number) aValue).doubleValue());
			else if (tType == float.class)   f.setFloat  (aBE, ((Number) aValue).floatValue());
			else if (tType == boolean.class) f.setBoolean(aBE, (Boolean) aValue);
			else f.set(aBE, aValue);
		} catch (Throwable e) {O.println("[GT6PROBESTAND] DIAG не удалось записать поле: " + aName + " в классе " + aBE.getClass().getSimpleName() + " (" + e + ")");}
	}

	/** Навигация по цепочке полей/индексов, например fldChain(be, "mTanks", 0, "mAmount"): строка -> имя
	 *  поля (через {@link #fld}), Integer -> индекс в массив/List. */
	public static Object fldChain(Object aRoot, Object... aPath) {
		Object tCur = aRoot;
		for (Object tStep : aPath) {
			if (tCur == null) return null;
			if (tStep instanceof Integer tIdx) {
				if (tCur.getClass().isArray()) tCur = java.lang.reflect.Array.get(tCur, tIdx);
				else if (tCur instanceof List<?> tList) tCur = tList.get(tIdx);
				else {O.println("[GT6PROBESTAND] DIAG fldChain: индекс " + tIdx + " для не-индексируемого " + tCur.getClass().getSimpleName()); return null;}
			} else tCur = fld(tCur, String.valueOf(tStep));
		}
		return tCur;
	}

	private static gregapi.fluid.FluidTankGT findTank(Object aBE, int aIndex) {
		if (aIndex == 0 && fld(aBE, "mTank") instanceof gregapi.fluid.FluidTankGT t) return t;
		Object tArr = fld(aBE, "mTanks");
		if (tArr instanceof gregapi.fluid.FluidTankGT[] tArrT && aIndex < tArrT.length) return tArrT[aIndex];
		O.println("[GT6PROBESTAND] DIAG танк не найден: be=" + (aBE == null ? "null" : aBE.getClass().getSimpleName()) + " index=" + aIndex);
		return null;
	}
	/** Заливает жидкость по ИМЕНИ (gregapi.data.FL.make(String,long) — тот же лукап, что в FLUIDPIPEPROBE) в танк[0] BE. */
	public static void fill(Object aBE, String aFluidName, long aMB) {
		gregapi.fluid.FluidTankGT t = findTank(aBE, 0);
		if (t != null) t.setFluid(gregapi.data.FL.make(aFluidName, aMB));
	}
	/** Опустошает ВСЕ найденные танки BE (mTank либо весь массив mTanks). */
	public static void drainAll(Object aBE) {
		Object tSingle = fld(aBE, "mTank");
		if (tSingle instanceof gregapi.fluid.FluidTankGT t) {t.setEmpty(); return;}
		Object tArr = fld(aBE, "mTanks");
		if (tArr instanceof gregapi.fluid.FluidTankGT[] tArrT) for (gregapi.fluid.FluidTankGT t : tArrT) t.setEmpty();
	}
	public static long tankAmount(Object aBE, int aIndex) {gregapi.fluid.FluidTankGT t = findTank(aBE, aIndex); return t == null ? 0 : t.amount();}

	/** Содержимое танка как «имя:объём» — ЕДИНЫЙ формат идентичности жидкости для всех стендов.
	 *  Судить «в баке что-то есть» недостаточно: краска не отличима от воды по одному лишь объёму,
	 *  поэтому судьи сравнивают именно эту строку. Формат совпадает с дампом паритета
	 *  ({@code PortDump.recFluids}: {@code FluidGT.nameOf(...) + ":" + amount}), чтобы ожидание стенда
	 *  можно было брать прямо из golden-дампа, не переписывая руками. */
	public static String tankContent(gregapi.fluid.FluidTankGT aTank) {
		if (aTank == null) return "нет танка";
		net.neoforged.neoforge.fluids.FluidStack tFluid = aTank.getFluid();
		if (tFluid == null || tFluid.getFluid() == null || tFluid.getAmount() <= 0) return "пусто";
		return gregapi.fluid.FluidGT.nameOf(tFluid.getFluid()) + ":" + tFluid.getAmount();
	}
	/** То же по BE — танк ищется тем же {@link #findTank} (поля {@code mTank}/{@code mTanks}). */
	public static String tankContent(Object aBE, int aIndex) {
		if (aBE == null) return "нет BE";
		return tankContent(findTank(aBE, aIndex));
	}
	/** То же для ВЫХОДНЫХ танков машины: у {@code MultiTileEntityBasicMachine} они лежат отдельным
	 *  полем {@code mTanksOutput} (:106), которого не знает {@link #findTank}. */
	public static String outTankContent(Object aBE, int aIndex) {
		if (aBE == null) return "нет BE";
		Object tArr = fld(aBE, "mTanksOutput");
		if (!(tArr instanceof gregapi.fluid.FluidTankGT[] tTanks)) return "нет поля mTanksOutput";
		if (aIndex >= tTanks.length) return "нет выходных танков";
		return tankContent(tTanks[aIndex]);
	}

	/** Слоты — ТОЛЬКО через публичный логический канал BE (TileEntityBase01Root.slot(i[,stack])), НЕ через
	 *  голую рефлексию по mInventory.
	 *  ⚠️ ОЧИСТКА идёт через slotKill, а НЕ записью ItemStack.EMPTY (дефект каркаса, найден стендом №14):
	 *  GT рассуждает о пустоте в null (TileEntityBase05Inventories:98 slotKill, :99 slotHas = mInventory!=null),
	 *  поэтому записанный EMPTY — это НЕ-null, слот считается ЗАНЯТЫМ, и выход машины блокируется навсегда
	 *  (MultiTileEntityBasicMachine:637-639 canOutput → FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS — рецепт
	 *  не стартует НИКОГДА). Стенд №14 потерял на этом прогон: второй скан молча не запускался.
	 *  F15 не нарушается: null в neo-слот мы не пишем — обнуление делает сам GT своим методом. */
	public static void slotSet(net.minecraft.world.level.block.entity.BlockEntity aBE, int aIndex, net.minecraft.world.item.ItemStack aStack) {
		if (!(aBE instanceof gregapi.tileentity.base.TileEntityBase01Root tRoot)) {O.println("[GT6PROBESTAND] DIAG slotSet: BE не поддерживает slot() — " + aBE.getClass().getSimpleName()); return;}
		if (aStack == null || aStack.isEmpty()) tRoot.slotKill(aIndex); else tRoot.slot(aIndex, aStack);
	}
	public static int slotCount(net.minecraft.world.level.block.entity.BlockEntity aBE, int aIndex) {
		if (aBE instanceof gregapi.tileentity.base.TileEntityBase01Root tRoot) return gregapi.util.ST.count(tRoot.slot(aIndex));
		O.println("[GT6PROBESTAND] DIAG slotCount: BE не поддерживает slot() — " + aBE.getClass().getSimpleName());
		return 0;
	}

	// ================================================================================================
	// СУДЬИ (единый формат вывода, маркер стенда в каждой строке)
	// ================================================================================================

	public static boolean judge(String aMarker, String aName, boolean aCond, Object aExpected, Object aActual) {
		if (aCond) O.println("[" + aMarker + "] " + aName + ": => PASS");
		else O.println("[" + aMarker + "] " + aName + ": => FAIL (ожидалось " + aExpected + ", получено " + aActual + ")");
		return aCond;
	}
	public static boolean conserve(String aMarker, String aName, long aInitial, java.util.function.LongSupplier aCurrent) {
		long tNow = aCurrent.getAsLong();
		return judge(aMarker, aName, tNow == aInitial, aInitial, tNow);
	}

	// ================================================================================================
	// КАНАЛЫ ИГРОКА (обёртки §4 манифеста)
	// ================================================================================================

	public static net.minecraft.world.InteractionResult clickBlock(net.minecraft.server.level.ServerPlayer aPlayer, net.minecraft.core.BlockPos aPos, net.minecraft.core.Direction aSide) {
		net.minecraft.server.level.ServerLevel tLevel = aPlayer.level();
		net.minecraft.world.item.ItemStack tStack = aPlayer.getMainHandItem();
		net.minecraft.world.phys.Vec3 tHit = net.minecraft.world.phys.Vec3.atCenterOf(aPos).add(aSide.getStepX()*0.5, aSide.getStepY()*0.5, aSide.getStepZ()*0.5);
		return aPlayer.gameMode.useItemOn(aPlayer, tLevel, tStack, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(tHit, aSide, aPos, false));
	}

	public static void giveTool(net.minecraft.server.level.ServerPlayer aPlayer, int aToolId, gregapi.oredict.OreDictMaterial aPrimaryMat, gregapi.oredict.OreDictMaterial aSecondaryMat) {
		net.minecraft.world.item.ItemStack tTool = gregapi.data.CS.ToolsGT.sMetaTool.getToolWithStats(aToolId, aPrimaryMat, aSecondaryMat);
		aPlayer.getInventory().setItem(0, tTool); aPlayer.getInventory().setSelectedSlot(0);
	}

	/** Телепорт + ЯВНЫЙ setYRot/setYHeadRot в том же тике (анти-паттерн §7: живой клиент шлёт MovePlayerPacket
	 *  со своим поворотом и гонкой перетирает yaw серверного телепорта). */
	public static void teleportLook(net.minecraft.server.level.ServerPlayer aPlayer, double aX, double aY, double aZ, float aYaw, float aPitch) {
		aPlayer.teleportTo(aPlayer.level(), aX, aY, aZ, java.util.Set.of(), aYaw, aPitch, true);
		aPlayer.setYRot(aYaw); aPlayer.setYHeadRot(aYaw);
	}

	// ================================================================================================
	// ТИК-МАШИНА — объект-стенд
	// ================================================================================================

	/** Один экземпляр на пробу: маркер задаётся при создании, DONE/EXC/heartbeat каркас печатает сам.
	 *  Диспетчер в GT_API_Proxy остаётся строкой {@code if (CS.probeFlag("...")) MyProbe.tick(...)}; тик-логика
	 *  стенда объявляется списком шагов ({@link #at}/{@link #every}/{@link #window}) один раз при старте JVM. */
	public static final class Seq {
		private final String mMarker;
		private final Map<Integer, List<Runnable>> mAt = new TreeMap<>();
		private final List<Object[]> mEvery = new ArrayList<>();  // {период(Integer), Runnable}
		private final List<Object[]> mWindow = new ArrayList<>(); // {от(Integer), до(Integer), Runnable}
		private final Map<String, Boolean> mWatchSeen = new HashMap<>();
		private int mCurrentTick = -1;
		private boolean mDone = false;
		private int mDoneTick = Integer.MAX_VALUE;
		public int passCount = 0, failCount = 0;

		public Seq(String aMarker) {mMarker = aMarker;}

		public Seq at(int aTick, Runnable aStep) {mAt.computeIfAbsent(aTick, k -> new ArrayList<>()).add(aStep); return this;}
		public Seq every(int aPeriod, Runnable aStep) {mEvery.add(new Object[]{aPeriod, aStep}); return this;}
		public Seq window(int aFrom, int aTo, Runnable aStep) {mWindow.add(new Object[]{aFrom, aTo, aStep}); return this;}

		/** Непрерывное наблюдение «видели ли хоть раз за окно» (урок FLUIDPIPEPROBE HOT: setOnFire()
		 *  кратковременен — однократный замер в конце лжёт; копим ИЛИ по всем тикам окна). */
		public Seq watch(String aName, int aFrom, int aTo, java.util.function.BooleanSupplier aProbe) {
			mWatchSeen.putIfAbsent(aName, false);
			return window(aFrom, aTo, () -> {if (aProbe.getAsBoolean()) mWatchSeen.put(aName, true);});
		}
		public boolean everSeen(String aName) {return mWatchSeen.getOrDefault(aName, false);}

		public int currentTick() {return mCurrentTick;}
		public boolean isDone() {return mDone;}

		/** Помечает пробу завершённой: печатает терминальный DONE (с итогом pass/fail). Вызывать из
		 *  последнего запланированного шага. */
		public void done() {
			if (mDone) return;
			mDone = true; mDoneTick = mCurrentTick;
			O.println("========== [" + mMarker + "] DONE (pass=" + passCount + " fail=" + failCount + ") ==========");
			// САМОЗАВЕРШЕНИЕ. Отработавший стенд держал клиент открытым бесконечно, и гасить процесс приходилось
			// человеку — при десятке прогонов подряд это чистая потеря его времени. Флаг `<стенд>.keepalive` в
			// рабочем каталоге оставляет клиент жить (нужно, когда площадка отдаётся игроку на приёмку).
			if (gregapi.data.CS.probeFlag(mMarker.toLowerCase().replace("gt6-", "gt6") + ".keepalive")) {
				O.println("[" + mMarker + "] keepalive: клиент оставлен открытым (флаг), закройте вручную");
				return;
			}
			O.println("[" + mMarker + "] стенд отработал — гашу процесс сам (флаг .keepalive оставляет открытым)");
			new Thread(() -> {
				try {Thread.sleep(3000);} catch (InterruptedException e) {/**/}
				Runtime.getRuntime().halt(0);
			}, "gt6-probe-shutdown").start();
		}

		/** Диспетчер: вызывать из статического tick(...) пробы КАЖДЫЙ серверный тик, пока probeFlag активен. */
		public void tick(int aCurrentTick) {
			mCurrentTick = aCurrentTick;
			if (mDone) {
				if (aCurrentTick > mDoneTick && aCurrentTick % 200 == 0 && aCurrentTick <= mDoneTick + 2000) O.println("[" + mMarker + "] heartbeat: сервер жив, тик " + aCurrentTick);
				return;
			}
			try {
				List<Runnable> tAtJobs = mAt.get(aCurrentTick);
				if (tAtJobs != null) for (Runnable r : tAtJobs) r.run();
				for (Object[] e : mEvery) {int tPeriod = (Integer) e[0]; if (tPeriod > 0 && aCurrentTick % tPeriod == 0) ((Runnable) e[1]).run();}
				for (Object[] w : mWindow) {int tFrom = (Integer) w[0], tTo = (Integer) w[1]; if (aCurrentTick >= tFrom && aCurrentTick <= tTo) ((Runnable) w[2]).run();}
			} catch (Throwable e) {
				O.println("[" + mMarker + "] EXC " + e); e.printStackTrace(O); mDone = true; mDoneTick = aCurrentTick;
			}
		}

		/** Судья с автоматическим маркером стенда и накоплением pass/fail в ЭТОМ объекте. */
		public boolean judge(String aName, boolean aCond, Object aExpected, Object aActual) {
			boolean r = GT6ProbeStand.judge(mMarker, aName, aCond, aExpected, aActual);
			if (r) passCount++; else failCount++;
			return r;
		}
		public boolean conserve(String aName, long aInitial, java.util.function.LongSupplier aCurrent) {
			long tNow = aCurrent.getAsLong();
			return judge(aName, tNow == aInitial, aInitial, tNow);
		}
	}
}
