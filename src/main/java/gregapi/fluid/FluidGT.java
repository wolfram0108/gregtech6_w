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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
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
 * {@code FluidRegistry} — глобальным реестром по строковому имени. В neo 26.1.2 это расщеплено на
 * {@link FluidType} (свойства) + {@link Fluid} (поведение), оба регистрируются через
 * {@link DeferredRegister} (`decisions/F5-fluids.md` §1,3).
 *
 * <p><b>F12/F5 отложенная конструкция source-fluid'а (2026-07-15).</b> neo {@link Fluid}-конструктор в
 * поле зовёт {@code BuiltInRegistries.FLUID.createIntrusiveHolder(this)} — это ТРЕБУЕТ окна регистрации
 * (реестр разморожен только внутри {@code RegisterEvent}). Значит source-{@link Fluid} НЕЛЬЗЯ строить
 * эагерно в фазе «preInit» (реестр заморожен → «Registry is already frozen»). Поэтому {@code FluidGT} —
 * это лёгкий <b>config-holder + координатор регистрации</b> (строится эагерно, БЕЗ наследования
 * {@link Fluid}), а реальный source-{@link Fluid} — вложенный {@link Source}, который {@link DeferredRegister}
 * строит supplier'ом уже НА {@code RegisterEvent} (разморожено). Chainable-сеттеры остаются на самом
 * {@code FluidGT} (config), а {@link Source}/{@link GTFluidType} читают эти поля живьём — оригинальная
 * возможность GT6 донастраивать жидкость после создания сохранена 1:1.
 *
 * <p>Публичный контракт {@code FL.create*} возвращает {@code FluidGT} (config-holder); движковый
 * {@link Fluid} доступен лениво через {@link #getFluid()} (резолв {@code mSourceHolder}, привязан после
 * RegisterEvent). Никто в GT6 не присваивал результат {@code FL.create} напрямую в {@link Fluid}-переменную
 * (сверено грепом) — только chain-сеттеры и передача в {@link OreDictMaterial}/{@code make}/{@code FoodStatDrink}
 * (последний берёт лишь ИМЯ), что теперь либо отложено на server-start, либо идёт через {@code mName}.
 *
 * <p>Референс сигнатур (НЕ выдумано):
 * <ul>
 * <li>{@code Fluid} abstract surface + {@code createIntrusiveHolder}-поле — {@code neo-decompiled/net/minecraft/world/level/material/Fluid.java:27-153}.</li>
 * <li>{@code FlowingFluid} abstract surface/source-flowing contract — {@code .../FlowingFluid.java:35,248,254,269,283,345,429,474}.</li>
 * <li>{@code RegisterEvent.register}=={@code Registry.register(reg,name,supplier.get())} — {@code neoforge-decompiled/.../registries/RegisterEvent.java:46-48}; {@code DeferredRegister} зовёт supplier на RegisterEvent — {@code DeferredRegister.java:42-43,214-234}.</li>
 * <li>{@link BaseFlowingFluid.Properties}/{@code .Flowing} — {@code neoforge-decompiled/net/neoforged/neoforge/fluids/BaseFlowingFluid.java:175-221}.</li>
 * </ul>
 *
 * <p>Клиентский рендер и мировые water-блоки (Ocean/River/Swamp) — вне области этого переходника
 * (F3/render и surface-B F5). Здесь регистрируются только {@link FluidType}+source/flowing {@link Fluid},
 * достаточные для танков/рецептов.
 */
public class FluidGT {

	/** Центральные DeferredRegister'ы мода — ЕДИНСТВЕННОЕ место, где GT6 регистрирует жидкости в neo.
	 *  {@code .register(modEventBus)} для обоих вызывается из центрального @Mod-конструктора
	 *  ({@code gregapi.GT_API#GT_API(IEventBus)}, тем же мод-басом, что {@code ITEMS}/{@code BLOCKS}/
	 *  {@code GT6WorldgenFeature} — F12↔F5 стык закрыт). */
	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MD.GAPI.mID);
	public static final DeferredRegister<Fluid>      FLUIDS      = DeferredRegister.create(BuiltInRegistries.FLUID, MD.GAPI.mID);

	/** GT6-имя (часто БЕЗ namespace, иногда с пробелами — напр. "rc jet fuel") -> config-holder FluidGT. */
	public static final Map<String, FluidGT> BY_NAME = new LinkedHashMap<>();

	/** Обратный индекс {@link Fluid}-объект (source ИЛИ flowing) -> {@link FluidGT}; строится лениво. */
	private static Map<Fluid, FluidGT> BY_FLUID_CACHE;

	public final String mName;
	/** F3-render (СДЕЛАНО, не долг): текстуру жидкости в мире рисует не это поле, а
	 *  {@code FluidModel.Unbaked(still, flow, overlay, tintSource)}, регистрируемый на
	 *  {@code RegisterFluidModelsEvent} — {@code GT_API_Proxy_Client.onRegisterFluidModels:204}
	 *  (в логе: «F3-render: FluidModel зарегистрированы для N GT6-жидкостей»). Поле остаётся как
	 *  1:1-носитель имени текстуры для GT6-кода, который его читает. */
	public final IIconContainer mTexture;

	private short[] mRGBa;
	private int  mTemperature;
	private boolean mGaseous;
	private int  mDensity    = 1000;
	private int  mViscosity  = 1000;
	private int  mLuminosity = 0;

	private final FluidType mType;
	public final DeferredHolder<FluidType, FluidType> mTypeHolder;
	public final DeferredHolder<Fluid, Source>        mSourceHolder;
	public final DeferredHolder<Fluid, FlowingFluid>  mFlowingHolder;

	public FluidGT(String aName, IIconContainer aTexture, short[] aRGBa, long aTemperatureK, boolean aGaseous) {
		mName = aName.toLowerCase();
		mTexture = aTexture;
		mRGBa = aRGBa;
		mTemperature = UT.Code.bindInt(aTemperatureK);
		mGaseous = aGaseous;
		mType = new GTFluidType(FluidType.Properties.create().descriptionId(getUnlocalizedName()));

		String tRegName = safeRegName(mName);
		// F12/F5: source и flowing строятся supplier'ом НА RegisterEvent (реестр разморожен → createIntrusiveHolder ок);
		// FluidType не интрузивен → его можно держать эагерно. mType — эагер, mSource/mFlowing — отложенная конструкция.
		mTypeHolder    = FLUID_TYPES.register(tRegName, () -> mType);
		mSourceHolder  = FLUIDS.register(tRegName, () -> new Source());
		mFlowingHolder = FLUIDS.register(tRegName + "_flowing", () -> new BaseFlowingFluid.Flowing(fluidProperties()));

		BY_NAME.put(mName, this);
		BY_FLUID_CACHE = null;
	}

	// F5 SUPERSEDED (не заглушка): оригинал (FluidGT.java:49-52) регистрировал Runnable в GT.mAfterPostInit/
	// mAfterServerStarted — принудительно переустанавливал mGas/mTemperature ПОСЛЕ пост-инита ("Ensure that no Mod
	// fucked up the Values"): защита от подмены значений сторонним модом через ГЛОБАЛЬНЫЙ мутируемый Forge FluidRegistry.
	// В neo 26.1.2 такого реестра НЕТ (жидкости — неизменяемые registry-объекты; чужой мод не может подменить mGas/
	// mTemperature под ногами) → сама угроза устранена движком, защитный ре-апплай не нужен (осознанно не воспроизведён).

	private BaseFlowingFluid.Properties fluidProperties() {
		// PORT-TODO(F5, поверхность B): .block()/.bucket() (decisions/F5-fluids.md §3,5)
		// намеренно не заданы для content-fluid'ов; мировые water-блоки — отдельная форсированная замена.
		return new BaseFlowingFluid.Properties(() -> mType, mSourceHolder::value, mFlowingHolder::value);
	}

	/** neo {@link Identifier}-путь не допускает пробелы/произвольные символы — санитизация ТОЛЬКО для ключа регистрации. */
	private static String safeRegName(String aName) {
		String rName = aName.toLowerCase().replaceAll("[^a-z0-9_.\\-]", "_");
		return rName.isEmpty() ? "unnamed" : rName;
	}

	public String getUnlocalizedName() {return "fluid." + mName;}
	public String getLocalizedName()   {return LH.get(getUnlocalizedName());}

	/** Source-fluid — вложенный {@link Source}, привязан после RegisterEvent (резолв holder'а). */
	public Fluid getFluid()        {return mSourceHolder.value();}
	public Fluid getFlowingFluid() {return mFlowingHolder.isBound() ? mFlowingHolder.value() : mSourceHolder.value();}
	public FluidType getFluidType() {return mType;}

	public boolean isGaseous() {return mGaseous;}
	public int     getDensity() {return mDensity;}
	public int     getViscosity() {return mViscosity;}
	public int     getLuminosity() {return mLuminosity;}
	public int     getTemperature() {return mTemperature;}
	public short[] getRGBa() {return mRGBa;}

	/** Chainable-сеттеры 1:1 воспроизводят старый Forge-1.7.10 Fluid-API (recompSrc
	 *  {@code net.minecraftforge.fluids.Fluid:131-158} — плоское присваивание полей, БЕЗ клампинга). */
	public FluidGT setTemperature(long aTemperatureK) {mTemperature = UT.Code.bindInt(aTemperatureK); return this;}
	public FluidGT setGaseous(boolean aGaseous)        {mGaseous = aGaseous; return this;}
	public FluidGT setDensity(int aDensity)            {mDensity = aDensity; return this;}
	public FluidGT setViscosity(int aViscosity)        {mViscosity = aViscosity; return this;}
	public FluidGT setLuminosity(int aLuminosity)      {mLuminosity = aLuminosity; return this;}
	public FluidGT setRGBa(short[] aRGBa)              {mRGBa = aRGBa; return this;}

	/** Находит GT6-переходник по neo {@link Fluid} (source ИЛИ flowing). */
	public static FluidGT of(Fluid aFluid) {
		if (aFluid == null) return null;
		if (aFluid instanceof Source tSource) return tSource.owner();
		if (BY_FLUID_CACHE == null || BY_FLUID_CACHE.size() < BY_NAME.size()) {
			Map<Fluid, FluidGT> tMap = new IdentityHashMap<>();
			for (FluidGT tGT : BY_NAME.values()) {
				if (tGT.mSourceHolder.isBound())  tMap.put(tGT.mSourceHolder.value(),  tGT);
				if (tGT.mFlowingHolder.isBound()) tMap.put(tGT.mFlowingHolder.value(), tGT);
			}
			BY_FLUID_CACHE = tMap;
		}
		return BY_FLUID_CACHE.get(aFluid);
	}

	/** GT6-имя для {@link Fluid}-объекта. Свои жидкости — точное {@link #mName}. */
	public static String nameOf(Fluid aFluid) {
		if (aFluid == null) return null;
		FluidGT tGT = of(aFluid);
		if (tGT != null) return tGT.mName;
		Identifier tId = BuiltInRegistries.FLUID.getKey(aFluid);
		return tId == null ? null : tId.getPath();
	}

	/** neo {@link FluidType}, читающий свойства ЖИВЬЁМ из объемлющего {@link FluidGT}. */
	private final class GTFluidType extends FluidType {
		GTFluidType(Properties aProperties) {super(aProperties);}
		@Override public int getTemperature() {return mTemperature;}
		@Override public int getDensity()     {return mDensity;}
		@Override public int getViscosity()   {return mViscosity;}
		@Override public int getLightLevel()  {return mLuminosity;}

		/**
		 * ИМЯ ЖИДКОСТИ ЧЕРЕЗ ДВИЖКОВЫЙ КАНАЛ (BUG-082) — 1:1 приём оригинала: там сам носитель отдавал GT6-имя
		 * движку, {@code FluidGT.getLocalizedName(FluidStack) → LH.get(getUnlocalizedName())}
		 * ({@code gregtech6/…/FluidGT.java:71-72}). В neo носитель имени — {@link FluidType}, а движковый вопрос
		 * «как называется эта жидкость» приходит сюда: {@code FluidStack.getHoverName():453-454} →
		 * {@code FluidType.getDescription(stack)}. Тем же путём имя берут сторонние моды (витрина Jade).
		 *
		 * <p>Пара к центру {@link gregapi.lang.LanguageHandler#injectIntoEngine()}, а не замена ему: центр чинит
		 * общий случай (движок узнаёт ВСЕ ключи GT6), носитель гарантирует свой — у Грегориуса были ОБА.
		 * Берём {@code LH.get} напрямую, а НЕ {@code FL.name}: тот для чужих жидкостей сам зовёт
		 * {@code getDescription} ({@code FL.java:1029}) — вышла бы рекурсия.
		 *
		 * <p>Если перевода нет вовсе, {@code LH.get} возвращает сам ключ — тогда отдаём движку его штатный
		 * {@code translatable}, чтобы не подменять сырым текстом работу ресурспаков.
		 */
		@Override public net.minecraft.network.chat.Component getDescription() {return described();}
		@Override public net.minecraft.network.chat.Component getDescription(net.neoforged.neoforge.fluids.FluidStack aStack) {return described();}

		private net.minecraft.network.chat.Component described() {
			String tKey = getUnlocalizedName(), tName = LH.get(tKey);
			return tName == null || tName.isEmpty() || tName.equals(tKey) ? super.getDescription() : net.minecraft.network.chat.Component.literal(tName);
		}
	}

	/**
	 * Реальный source-{@link Fluid}. Вложенный (нестатический) — читает config объемлющего {@link FluidGT}
	 * живьём. Строится {@link DeferredRegister}-supplier'ом НА {@code RegisterEvent} (реестр разморожен →
	 * {@code Fluid.<init>}→{@code createIntrusiveHolder} валиден). Контракт-методы source/flowing — сигнатуры
	 * из neo-decompiled {@link FlowingFluid}, поведение 1:1 из оригинального FluidGT.
	 */
	public final class Source extends FlowingFluid {
		/** Обратная ссылка на config-holder (для {@link FluidGT#of}). */
		public FluidGT owner() {return FluidGT.this;}

		@Override public Fluid getFlowing() {return getFlowingFluid();}
		@Override public Fluid getSource() {return this;}
		@Override protected boolean canConvertToSource(ServerLevel aLevel) {return false;}
		@Override protected void beforeDestroyingBlock(LevelAccessor aLevel, BlockPos aPos, BlockState aState) {
			BlockEntity tBlockEntity = aState.hasBlockEntity() ? aLevel.getBlockEntity(aPos) : null;
			Block.dropResources(aState, aLevel, aPos, tBlockEntity);
		}
		@Override protected int getSlopeFindDistance(LevelReader aLevel) {return 4;}
		@Override protected int getDropOff(LevelReader aLevel) {return 1;}
		@Override public int getAmount(FluidState aState) {return 8;}
		@Override public boolean isSource(FluidState aState) {return true;}
		@Override public Item getBucket() {return Items.AIR;}
		@Override protected boolean canBeReplacedWith(FluidState aState, BlockGetter aLevel, BlockPos aPos, Fluid aOther, Direction aDirection) {return aDirection == Direction.DOWN && !isSame(aOther);}
		@Override public int getTickDelay(LevelReader aLevel) {return 5;}
		@Override protected float getExplosionResistance() {return 1.0F;}
		@Override protected BlockState createLegacyBlock(FluidState aState) {return Blocks.AIR.defaultBlockState();}
		@Override public boolean isSame(Fluid aFluid) {return aFluid == this || (mFlowingHolder != null && mFlowingHolder.isBound() && aFluid == mFlowingHolder.value());}
		@Override public FluidType getFluidType() {return mType;}
	}
}
