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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.fluid;

import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.render.IIconContainer;
import gregapi.util.UT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.FluidType;

import net.minecraftforge.registries.DeferredRegister;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** neo's Fluid constructor needs the registry unfrozen (RegisterEvent) to intrusive-hold itself, so it can't build eagerly.
 *  FluidGT is a lightweight config-holder instead; the real Fluid is a nested Source, built lazily at RegisterEvent. */
public class FluidGT {

	/** The only place GT6 registers fluids in neo; both registries are wired from the central @Mod constructor. */
	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.FLUID_TYPES, MD.GAPI.mID);
	public static final DeferredRegister<Fluid>      FLUIDS      = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.FLUIDS, MD.GAPI.mID);

	/** GT6 fluid name (often without a namespace, sometimes with spaces) to its config holder. */
	public static final Map<String, FluidGT> BY_NAME = new LinkedHashMap<>();

	/** Reverse index from a Fluid object (source or flowing) back to its FluidGT holder; built lazily. */
	private static Map<Fluid, FluidGT> BY_FLUID_CACHE;

	public final String mName;
	/** The in-world texture is drawn elsewhere, through a registered FluidModel; this field remains only as
	 *  the 1:1 texture-name carrier for GT6 code that still reads it. */
	public final IIconContainer mTexture;

	private short[] mRGBa;
	private int  mTemperature;
	private boolean mGaseous;
	private int  mDensity    = 1000;
	private int  mViscosity  = 1000;
	private int  mLuminosity = 0;

	private final FluidType mType;
	public final net.minecraftforge.registries.RegistryObject<FluidType> mTypeHolder;
	public final net.minecraftforge.registries.RegistryObject<Source>        mSourceHolder;
	public final net.minecraftforge.registries.RegistryObject<FlowingFluid>  mFlowingHolder;

	public FluidGT(String aName, IIconContainer aTexture, short[] aRGBa, long aTemperatureK, boolean aGaseous) {
		mName = aName.toLowerCase();
		mTexture = aTexture;
		mRGBa = aRGBa;
		mTemperature = UT.Code.bindInt(aTemperatureK);
		mGaseous = aGaseous;
		mType = new GTFluidType(FluidType.Properties.create().descriptionId(getUnlocalizedName()));

		String tRegName = safeRegName(mName);
		// Source and flowing fluids need the registry unfrozen to build their intrusive holder, so they are
		// built by supplier at RegisterEvent, while FluidType is not intrusive and can be held eagerly.
		mTypeHolder    = FLUID_TYPES.register(tRegName, () -> mType);
		mSourceHolder  = FLUIDS.register(tRegName, () -> new Source());
		mFlowingHolder = FLUIDS.register(tRegName + "_flowing", () -> new Flowing(fluidProperties()));

		BY_NAME.put(mName, this);
		BY_FLUID_CACHE = null;
	}

	// The original re-applied gas/temperature after post-init to guard against another mod mutating the
	// global mutable Forge registry; neo fluids are immutable objects, so that threat no longer exists.

	private ForgeFlowingFluid.Properties fluidProperties() {
		// Matches the original: content fluids never called Fluid.setBlock, so they get no block or bucket here
		// either; world water blocks are a separate hierarchy.
		// (decisions/F5-fluids.md §5).
		return new ForgeFlowingFluid.Properties(() -> mType, mSourceHolder::get, mFlowingHolder::get);
	}

	/** neo's ResourceLocation path disallows spaces and odd characters; this sanitizes only the registration key, not any name. */
	private static String safeRegName(String aName) {
		String rName = aName.toLowerCase().replaceAll("[^a-z0-9_.\\-]", "_");
		return rName.isEmpty() ? "unnamed" : rName;
	}

	public String getUnlocalizedName() {return "fluid." + mName;}
	public String getLocalizedName()   {return LH.get(getUnlocalizedName());}

	/** The source fluid is the nested {@link Source}; it only resolves after RegisterEvent binds the holder. */
	public Fluid getFluid()        {return mSourceHolder.get();}
	public Fluid getFlowingFluid() {return mFlowingHolder.isPresent() ? mFlowingHolder.get() : mSourceHolder.get();}
	public FluidType getFluidType() {return mType;}

	public boolean isGaseous() {return mGaseous;}
	public int     getDensity() {return mDensity;}
	public int     getViscosity() {return mViscosity;}
	public int     getLuminosity() {return mLuminosity;}
	public int     getTemperature() {return mTemperature;}
	public short[] getRGBa() {return mRGBa;}

	/** Chainable setters reproduce the old Forge 1.7.10 Fluid API's plain, unclamped field assignment. */
	public FluidGT setTemperature(long aTemperatureK) {mTemperature = UT.Code.bindInt(aTemperatureK); return this;}
	public FluidGT setGaseous(boolean aGaseous)        {mGaseous = aGaseous; return this;}
	public FluidGT setDensity(int aDensity)            {mDensity = aDensity; return this;}
	public FluidGT setViscosity(int aViscosity)        {mViscosity = aViscosity; return this;}
	public FluidGT setLuminosity(int aLuminosity)      {mLuminosity = aLuminosity; return this;}
	public FluidGT setRGBa(short[] aRGBa)              {mRGBa = aRGBa; return this;}

	/** Resolves the GT6 wrapper from either the source or the flowing neo Fluid instance. */
	public static FluidGT of(Fluid aFluid) {
		if (aFluid == null) return null;
		if (aFluid instanceof Source tSource) return tSource.owner();
		if (BY_FLUID_CACHE == null || BY_FLUID_CACHE.size() < BY_NAME.size()) {
			Map<Fluid, FluidGT> tMap = new IdentityHashMap<>();
			for (FluidGT tGT : BY_NAME.values()) {
				if (tGT.mSourceHolder.isPresent())  tMap.put(tGT.mSourceHolder.get(),  tGT);
				if (tGT.mFlowingHolder.isPresent()) tMap.put(tGT.mFlowingHolder.get(), tGT);
			}
			BY_FLUID_CACHE = tMap;
		}
		return BY_FLUID_CACHE.get(aFluid);
	}

	/** GT6 name for a Fluid object; for an own fluid this is the exact {@link #mName}. */
	public static String nameOf(Fluid aFluid) {
		if (aFluid == null) return null;
		FluidGT tGT = of(aFluid);
		if (tGT != null) return tGT.mName;
		ResourceLocation tId = BuiltInRegistries.FLUID.getKey(aFluid);
		return tId == null ? null : tId.getPath();
	}

	/** neo FluidType reading its properties live from the enclosing FluidGT. */
	private final class GTFluidType extends FluidType {
		GTFluidType(Properties aProperties) {super(aProperties);}
		@Override public int getTemperature() {return mTemperature;}
		@Override public int getDensity()     {return mDensity;}
		@Override public int getViscosity()   {return mViscosity;}
		@Override public int getLightLevel()  {return mLuminosity;}

		/** Fluid texture/tint comes from the engine via IClientFluidTypeExtensions; 1.20.1 has no fluid-model registration event.
		 *  The body invokes the client center lazily, since referencing client-only types here would crash a dedicated server. */
		@Override
		public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> aConsumer) {
			gregapi.GT_API_Proxy_Client.bindFluidClientExtensions(FluidGT.this, aConsumer);
		}

		/** Routes the name through the same engine channel the original carrier used, pairing with
		 *  {@link gregapi.lang.LanguageHandler#injectIntoEngine()} rather than duplicating its general-case fix. */
		@Override public net.minecraft.network.chat.Component getDescription() {return described();}
		@Override public net.minecraft.network.chat.Component getDescription(net.minecraftforge.fluids.FluidStack aStack) {return described();}

		private net.minecraft.network.chat.Component described() {
			String tKey = getUnlocalizedName(), tName = LH.get(tKey);
			return tName == null || tName.isEmpty() || tName.equals(tKey) ? super.getDescription() : net.minecraft.network.chat.Component.literal(tName);
		}
	}

	/** The real source Fluid, nested so it can read the enclosing FluidGT's config live; built by
	 *  DeferredRegister supplier at RegisterEvent, once the registry is unfrozen for its intrusive holder. */
	public final class Source extends FlowingFluid {
		/** Back-reference to the enclosing config-holder; {@link FluidGT#of} depends on this. */
		public FluidGT owner() {return FluidGT.this;}

		@Override public Fluid getFlowing() {return getFlowingFluid();}
		@Override public Fluid getSource() {return this;}
		@Override protected boolean canConvertToSource(net.minecraft.world.level.Level aLevel) {return false;} // On 1.20.1 this takes a Level argument, not a ServerLevel (FlowingFluid/ForgeFlowingFluid.java:79).
		@Override protected void beforeDestroyingBlock(LevelAccessor aLevel, BlockPos aPos, BlockState aState) {
			BlockEntity tBlockEntity = aState.hasBlockEntity() ? aLevel.getBlockEntity(aPos) : null;
			Block.dropResources(aState, aLevel, aPos, tBlockEntity);
		}
		@Override protected int getSlopeFindDistance(LevelReader aLevel) {return 4;}
		@Override protected int getDropOff(LevelReader aLevel) {return 1;}
		@Override public int getAmount(FluidState aState) {return 8;}
		@Override public boolean isSource(FluidState aState) {return true;}
		/** This fluid has no directional flow at all, a property of the fluid itself, not a rendering choice: 1.7.10 drew it with
		 *  one icon and no flow stripe, so the engine's flow-UV formula is overridden here instead of suppressed at render time. */
		@Override public Vec3 getFlow(BlockGetter aLevel, BlockPos aPos, FluidState aState) {return Vec3.ZERO;}
		@Override public Item getBucket() {return Items.AIR;}
		@Override protected boolean canBeReplacedWith(FluidState aState, BlockGetter aLevel, BlockPos aPos, Fluid aOther, Direction aDirection) {return aDirection == Direction.DOWN && !isSame(aOther);}
		@Override public int getTickDelay(LevelReader aLevel) {return 5;}
		@Override public float getExplosionResistance() {return 1.0F;}
		/** createLegacyBlock supplies the block form the engine substitutes in (map pixel, destroyBlock, buckets, falling-block);
		 *  world fluids get their block from FL.BLOCKS, a content-only fluid with no world form gets AIR, as 1.7.10 did. */
		@Override protected BlockState createLegacyBlock(FluidState aState) {
			Block tBlock = gregapi.data.FL.BLOCKS.get(gregapi.data.FL.regName(this));
			return tBlock == null ? Blocks.AIR.defaultBlockState() : tBlock.defaultBlockState();
		}
		@Override public boolean isSame(Fluid aFluid) {return aFluid == this || (mFlowingHolder != null && mFlowingHolder.isPresent() && aFluid == mFlowingHolder.get());}
		@Override public FluidType getFluidType() {return mType;}
	}

	/** The flowing partial-cell side needs its own getFlow, since at partial quanta FluidState.getType() reports Flowing. */
	public static final class Flowing extends ForgeFlowingFluid.Flowing {
		public Flowing(ForgeFlowingFluid.Properties aProperties) {super(aProperties);}
		/** Same property, same fluid as {@link Source#getFlow} above, not explained twice. */
		@Override public Vec3 getFlow(BlockGetter aLevel, BlockPos aPos, FluidState aState) {return Vec3.ZERO;}
	}
}
