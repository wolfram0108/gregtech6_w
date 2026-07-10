/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.fluid;

import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.render.IIconContainer;
import gregapi.util.UT;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * F5 центральный переходник — единственное место мода, которое регистрирует GT6-жидкости в neo.
 *
 * <p>В Forge 1.7.10 {@code net.minecraftforge.fluids.Fluid} был ОДНИМ изменяемым data-holder'ом со
 * chainable-сеттерами ({@code setDensity/setViscosity/setLuminosity/setTemperature/setGaseous}), а
 * {@code FluidRegistry} — глобальным реестром по строковому имени, куда можно было регистрировать
 * (и дозаписывать свойства уже зарегистрированной жидкости) практически в любой момент постинициализации.
 * В neo 26.1.2 это расщеплено на {@link FluidType} (свойства) + {@link Fluid} (поведение), оба
 * ОБЯЗАНЫ регистрироваться через {@link DeferredRegister} на событии загрузки мода и, что важнее,
 * НЕИЗМЕНЯЕМЫ на уровне движка после конструирования (`decisions/F5-fluids.md` §1,3).
 *
 * <p>Этот класс воспроизводит GT6-шный "изменяемый Fluid" 1:1 по ПОВЕДЕНИЮ (а не по факту неизменности
 * движка): {@link #setDensity}/{@link #setViscosity}/{@link #setLuminosity}/{@link #setTemperature}/
 * {@link #setGaseous} мутируют ЖИВЫЕ поля ЭТОГО объекта, а не сам neo {@link FluidType} — вместо этого
 * внутренний {@link GTFluidType} ЧИТАЕТ эти поля на каждый вызов геттера, так что мутация "задним числом"
 * (GT6 `FluidGT.run()`: "Ensure that no Mod fucked up the Values") продолжает работать и после
 * регистрации, ровно как в оригинале.
 *
 * <p>Референс сигнатур (НЕ выдумано, всё сверено с декомпилом):
 * <ul>
 * <li>{@code DeferredRegister<FluidType>}/{@code <Fluid>}, регистрация Source/Flowing —
 *     {@code D:\Temp\MC_NEW\NeoForge\tests\src\main\java\net\neoforged\neoforge\oldtest\fluid\NewFluidTest.java:65-68,70-81}
 *     и {@code .../FluidTypeTest.java:83-101} (эталон-моды NeoForge 26.1.2).</li>
 * <li>{@link FluidType}/{@link FluidType.Properties} —
 *     {@code D:\Temp\MC_NEW\neoforge-decompiled\net\neoforged\neoforge\fluids\FluidType.java}.</li>
 * <li>{@link BaseFlowingFluid}/{@code .Source}/{@code .Flowing}/{@code .Properties} —
 *     {@code D:\Temp\MC_NEW\neoforge-decompiled\net\neoforged\neoforge\fluids\BaseFlowingFluid.java}.</li>
 * </ul>
 *
 * <p>Клиентский рендер (текстуры/tint через {@code RegisterFluidModelsEvent}) и мировые блоки
 * (Water/Ocean/River/Swamp, {@code LiquidBlock}) — вне области этого переходника (F3/render и
 * "поверхность B" F5 соответственно; `decisions/F5-fluids.md` §3,5). Здесь регистрируются ТОЛЬКО
 * {@link FluidType}+{@link Fluid} (source+flowing) — минимум, достаточный, чтобы {@code FluidStack}
 * жидкости существовал и жил в танках/рецептах (как и было в 1.7.10 — GT6 из ~196 жидкостей мировой
 * блок имеют только 4, `fluids.md` §1).
 */
public class FluidGT {

	/** Центральные DeferredRegister'ы мода — ЕДИНСТВЕННОЕ место, где GT6 регистрирует жидкости в neo.
	 *  PORT-TODO(F12, регистрация/wiring): {@code .register(modEventBus)} для обоих должен вызвать
	 *  центральный @Mod-конструктор (F12, отдельный шов "REGISTRATION" — сейчас не централизован,
	 *  см. STATE.md); до этого вызова записи в DeferredRegister валидны (это фаза "сбора", а не
	 *  регистрации — см. DeferredRegister.java:214 register(name,Supplier) не требует уже быть на bus). */
	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MD.GAPI.mID);
	public static final DeferredRegister<Fluid>      FLUIDS      = DeferredRegister.create(BuiltInRegistries.FLUID, MD.GAPI.mID);

	/** GT6-имя (часто БЕЗ namespace, иногда с пробелами — напр. "rc jet fuel"; это опорная строка
	 *  межмодового интеропа 1.7.10, НЕ обязательно валидный {@link Identifier}) -> инстанс.
	 *  Заменяет {@code FluidRegistry.getFluid(String)}. */
	public static final Map<String, FluidGT> BY_NAME = new LinkedHashMap<>();

	/** Обратный индекс {@link Fluid}-объект -> {@link FluidGT}; строится лениво при первом обращении
	 *  (до регистрации DeferredHolder'ы не {@link DeferredHolder#isBound()}, поэтому раньше строить нет смысла). */
	private static Map<Fluid, FluidGT> BY_FLUID_CACHE;

	public final String mName;
	/** Путь текстуры/иконка жидкости. Данные не тронуты, но САМ рендер (RegisterFluidModelsEvent/tint)
	 *  не подключается тут — PORT-TODO(F3, рендер жидкостей): вне области F5, см. decisions/F5-fluids.md §3. */
	public final IIconContainer mTexture;

	private short[] mRGBa;
	private int  mTemperature;
	private boolean mGaseous;
	private int  mDensity    = 1000;
	private int  mViscosity  = 1000;
	private int  mLuminosity = 0;

	public final DeferredHolder<FluidType, FluidType>     mTypeHolder;
	public final DeferredHolder<Fluid, FlowingFluid>       mSourceHolder;
	public final DeferredHolder<Fluid, FlowingFluid>       mFlowingHolder;

	public FluidGT(String aName, IIconContainer aTexture, short[] aRGBa, long aTemperatureK, boolean aGaseous) {
		mName = aName.toLowerCase();
		mTexture = aTexture;
		mRGBa = aRGBa;
		mTemperature = UT.Code.bindInt(aTemperatureK);
		mGaseous = aGaseous;

		String tRegName = safeRegName(mName);
		mTypeHolder    = FLUID_TYPES.register(tRegName, () -> new GTFluidType(FluidType.Properties.create().descriptionId(getUnlocalizedName())));
		mSourceHolder  = FLUIDS.register(tRegName,             () -> new BaseFlowingFluid.Source (fluidProperties()));
		mFlowingHolder = FLUIDS.register(tRegName + "_flowing", () -> new BaseFlowingFluid.Flowing(fluidProperties()));

		BY_NAME.put(mName, this);
		BY_FLUID_CACHE = null; // индекс устарел, перестроится лениво
	}

	private BaseFlowingFluid.Properties fluidProperties() {
		// PORT-TODO(F5, поверхность B, decisions/F5-fluids.md §3,5): .block()/.bucket() намеренно не заданы —
		// 192 из 196 GT6-жидкостей ("контент-жидкости") мирового блока не имели и в 1.7.10 (были чистым
		// data-holder'ом для танков/рецептов). Мировые (Water/Ocean/River/Swamp) — своя, форсированная
		// замена субстрата (кастомный Block с финитной текучестью), отдельная задача, не в этом переходнике.
		return new BaseFlowingFluid.Properties(mTypeHolder::value, mSourceHolder::value, mFlowingHolder::value);
	}

	/** neo {@link Identifier}-путь не допускает пробелы/произвольные символы (GT6-имена вида "rc jet fuel"
	 *  такому требованию не удовлетворяют) — санитизация ТОЛЬКО для ключа регистрации. {@link #mName}
	 *  (оригинальное GT6-имя, ключ {@link #BY_NAME}) остаётся как в оригинале, без изменений. */
	private static String safeRegName(String aName) {
		String rName = aName.toLowerCase().replaceAll("[^a-z0-9_.\\-]", "_");
		return rName.isEmpty() ? "unnamed" : rName;
	}

	public String getUnlocalizedName() {return "fluid." + mName;}
	public String getLocalizedName()   {return LH.get(getUnlocalizedName());}

	public Fluid getFluid()        {return mSourceHolder.get();}
	public Fluid getFlowingFluid() {return mFlowingHolder.get();}
	public FluidType getFluidType() {return mTypeHolder.get();}

	public boolean isGaseous() {return mGaseous;}
	public int     getDensity() {return mDensity;}
	public int     getViscosity() {return mViscosity;}
	public int     getLuminosity() {return mLuminosity;}
	public int     getTemperature() {return mTemperature;}
	public short[] getRGBa() {return mRGBa;}

	/** Chainable-сеттеры 1:1 воспроизводят старый Forge-1.7.10 {@code Fluid}-API (см. javadoc класса):
	 *  меняют ЖИВЫЕ поля, видимые немедленно через {@link GTFluidType}, в т.ч. уже после регистрации. */
	public FluidGT setTemperature(long aTemperatureK) {mTemperature = UT.Code.bindInt(aTemperatureK); return this;}
	public FluidGT setGaseous(boolean aGaseous)        {mGaseous = aGaseous; return this;}
	public FluidGT setDensity(int aDensity)            {mDensity = aDensity; return this;}
	public FluidGT setViscosity(int aViscosity)        {mViscosity = Math.max(0, aViscosity); return this;}
	public FluidGT setLuminosity(int aLuminosity)      {mLuminosity = Math.max(0, Math.min(15, aLuminosity)); return this;}
	public FluidGT setRGBa(short[] aRGBa)              {mRGBa = aRGBa; return this;}

	/** Находит GT6-переходник по уже зарегистрированному neo {@link Fluid} (source ИЛИ flowing).
	 *  Заменяет обратный поиск, которого 1.7.10 {@code FluidRegistry} не требовал явно (там жидкость
	 *  была тем же объектом, что и запись реестра). */
	public static FluidGT of(Fluid aFluid) {
		if (aFluid == null) return null;
		if (BY_FLUID_CACHE == null || BY_FLUID_CACHE.size() < BY_NAME.size()) {
			Map<Fluid, FluidGT> tMap = new IdentityHashMap<>();
			for (FluidGT tGT : BY_NAME.values()) {
				if (tGT.mSourceHolder.isBound())  tMap.put(tGT.mSourceHolder.value(), tGT);
				if (tGT.mFlowingHolder.isBound()) tMap.put(tGT.mFlowingHolder.value(), tGT);
			}
			BY_FLUID_CACHE = tMap;
		}
		return BY_FLUID_CACHE.get(aFluid);
	}

	/** GT6-имя для {@link Fluid}-объекта. Свои жидкости — точное {@link #mName} (1:1, включая
	 *  "неправильные" 1.7.10-имена вида "rc jet fuel"). Чужие (другой мод) — путь их {@link Identifier}.
	 *  PORT-TODO(F5, межмодовый интероп по имени, decisions/F5-fluids.md §9): 1.7.10
	 *  {@code FluidRegistry.getFluid(String)} искал по "голому" имени БЕЗ namespace среди жидкостей ВСЕХ
	 *  модов; neo {@code BuiltInRegistries.FLUID} требует namespace, поэтому здесь используется только
	 *  path — при совпадении путей у разных модов возможна неоднозначность, которой в 1.7.10 не было. */
	public static String nameOf(Fluid aFluid) {
		if (aFluid == null) return null;
		FluidGT tGT = of(aFluid);
		if (tGT != null) return tGT.mName;
		Identifier tId = BuiltInRegistries.FLUID.getKey(aFluid);
		return tId == null ? null : tId.getPath();
	}

	/** neo {@link FluidType}, читающий свойства ЖИВЬЁМ из объемлющего {@link FluidGT} — см. javadoc класса. */
	private final class GTFluidType extends FluidType {
		GTFluidType(Properties aProperties) {super(aProperties);}
		@Override public int getTemperature() {return mTemperature;}
		@Override public int getDensity()     {return mDensity;}
		@Override public int getViscosity()   {return mViscosity;}
		@Override public int getLightLevel()  {return mLuminosity;}
	}
}
