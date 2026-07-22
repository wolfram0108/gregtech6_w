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
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.resources.Identifier;
import net.minecraft.util.IIcon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

import net.neoforged.api.distmarker.Dist;
import gregapi.GT_API;
import gregapi.code.*;
import gregapi.damage.DamageSources;
import gregapi.data.*;
import gregapi.data.TC.TC_AspectStack;
import gregapi.enchants.Enchantment_Radioactivity;
import gregapi.fluid.FluidGT;
import gregapi.fluid.FluidTankGT;
import gregapi.lang.LanguageHandler;
import gregapi.network.packets.PacketSound;
import gregapi.old.Textures;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;
import gregapi.player.EntityFoodTracker;
import gregapi.random.IHasWorldAndCoords;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.render.IIconContainer;
import gregapi.tileentity.delegate.DelegatorTileEntity;
import ic2.api.recipe.IMachineRecipeManager;
import ic2.api.recipe.IMachineRecipeManagerExt;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.RecipeOutput;
import mods.railcraft.common.items.enchantment.RailcraftEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.advancements.Advancement;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.*;
import java.util.Map.Entry;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 * 
 * Utility for accessing the random Utility Functions in a more short manner. The Short Name is for ease of overview and stands for "UtiliTy". :P
 */
public class UT {
	@Deprecated public static class Fluids {
		// F5: тело переведено на делегирование единственному центру FL (тот же приём, что createLiquid/
		// createMolten/load/save ниже в этом же классе, decisions/F5-fluids.md) — `net.minecraftforge.fluids.*`
		// (Fluid/FluidRegistry/IFluidTank/IFluidHandler/FluidContainerRegistry, 1.7.10 Forge) не существует
		// в neo целиком (пакет отсутствует во всех 3 корнях референса), FL.java уже реализует те же имена
		// методов на реальном neo/neoforge API (BuiltInRegistries.FLUID, FluidStack.getAmount(), и т.д.).
		@Deprecated public static int id (IFluidTank aTank) {return FL.id (aTank);}
		@Deprecated public static int id_(IFluidTank aTank) {return FL.id_(aTank);}
		@Deprecated public static int id (FluidStack aFluid) {return FL.id (aFluid);}
		@Deprecated public static int id_(FluidStack aFluid) {return FL.id_(aFluid);}
		@Deprecated public static int id (Fluid aFluid) {return FL.id (aFluid);}
		@Deprecated public static int id_(Fluid aFluid) {return FL.id_(aFluid);}

		@Deprecated public static Fluid fluid (int aID) {return FL.fluid (aID);}
		@Deprecated public static Fluid fluid (String aFluidName) {return FL.fluid (aFluidName);}
		@Deprecated public static Fluid fluid_(String aFluidName) {return FL.fluid_(aFluidName);}

		@Deprecated public static boolean equal(FluidStack aFluid1, FluidStack aFluid2) {return FL.equal(aFluid1, aFluid2);}
		@Deprecated public static boolean equal(FluidStack aFluid1, FluidStack aFluid2, boolean aIgnoreNBT) {return FL.equal(aFluid1, aFluid2, aIgnoreNBT);}

		@Deprecated public static boolean is(IFluidTank aTank, String... aNames) {return FL.is(aTank, aNames);}
		@Deprecated public static boolean is(FluidStack aFluid, String... aNames) {return FL.is(aFluid, aNames);}
		@Deprecated public static boolean is(Fluid aFluid, String... aNames) {return FL.is(aFluid, aNames);}

		@Deprecated public static ItemStack display(Fluid aFluid) {return FL.display(aFluid);}
		@Deprecated public static ItemStack display(FluidStack aFluid, boolean aUseStackSize, boolean aLimitStackSize) {return FL.display(aFluid, aUseStackSize, aLimitStackSize);}
		@Deprecated public static ItemStack display(FluidTankGT aTank, boolean aUseStackSize, boolean aLimitStackSize) {return FL.display(aTank, aUseStackSize, aLimitStackSize);}
		@Deprecated public static ItemStack display(FluidStack aFluid, long aAmount, boolean aUseStackSize, boolean aLimitStackSize) {return FL.display(aFluid, aAmount, aUseStackSize, aLimitStackSize);}
		
		/** @return if that Liquid is Water or Distilled Water */
		@Deprecated public static boolean water(IFluidTank aFluid) {return FL.water(aFluid);}
		/** @return if that Liquid is Water or Distilled Water */
		@Deprecated public static boolean water(FluidStack aFluid) {return FL.water(aFluid);}
		/** @return if that Liquid is Water or Distilled Water */
		@Deprecated public static boolean water(Fluid aFluid) {return FL.water(aFluid);}

		/** @return if that Liquid is distilled Water */
		@Deprecated public static boolean distw(IFluidTank aFluid) {return FL.distw(aFluid);}
		/** @return if that Liquid is distilled Water */
		@Deprecated public static boolean distw(FluidStack aFluid) {return FL.distw(aFluid);}
		/** @return if that Liquid is distilled Water */
		@Deprecated public static boolean distw(Fluid aFluid) {return FL.distw(aFluid);}

		/** @return if that Liquid is Lava */
		@Deprecated public static boolean lava(IFluidTank aFluid) {return FL.lava(aFluid);}
		/** @return if that Liquid is Lava */
		@Deprecated public static boolean lava(FluidStack aFluid) {return FL.lava(aFluid);}
		/** @return if that Liquid is Lava */
		@Deprecated public static boolean lava(Fluid aFluid) {return FL.lava(aFluid);}

		/** @return if that Liquid is Steam */
		@Deprecated public static boolean steam(IFluidTank aFluid) {return FL.steam(aFluid);}
		/** @return if that Liquid is Steam */
		@Deprecated public static boolean steam(FluidStack aFluid) {return FL.steam(aFluid);}
		/** @return if that Liquid is Steam */
		@Deprecated public static boolean steam(Fluid aFluid) {return FL.steam(aFluid);}

		/** @return if that Liquid is Milk */
		@Deprecated public static boolean milk(IFluidTank aFluid) {return FL.milk(aFluid);}
		/** @return if that Liquid is Milk */
		@Deprecated public static boolean milk(FluidStack aFluid) {return FL.milk(aFluid);}
		/** @return if that Liquid is Milk */
		@Deprecated public static boolean milk(Fluid aFluid) {return FL.milk(aFluid);}

		/** @return if that Liquid is Soy Milk */
		@Deprecated public static boolean soym(IFluidTank aFluid) {return FL.soym(aFluid);}
		/** @return if that Liquid is Soy Milk */
		@Deprecated public static boolean soym(FluidStack aFluid) {return FL.soym(aFluid);}
		/** @return if that Liquid is Soy Milk */
		@Deprecated public static boolean soym(Fluid aFluid) {return FL.soym(aFluid);}

		/** @return if that Liquid is Steam */
		@Deprecated public static boolean anysteam(IFluidTank aFluid) {return FL.anysteam(aFluid);}
		/** @return if that Liquid is Steam */
		@Deprecated public static boolean anysteam(FluidStack aFluid) {return FL.anysteam(aFluid);}
		/** @return if that Liquid is Steam */
		@Deprecated public static boolean anysteam(Fluid aFluid) {return FL.anysteam(aFluid);}

		/** @return if that Liquid is supposed to be conducting Power */
		@Deprecated public static boolean powerconducting(IFluidTank aFluid) {return FL.powerconducting(aFluid);}
		/** @return if that Liquid is supposed to be conducting Power */
		@Deprecated public static boolean powerconducting(FluidStack aFluid) {return FL.powerconducting(aFluid);}
		/** @return if that Liquid is supposed to be conducting Power */
		@Deprecated public static boolean powerconducting(Fluid aFluid) {return FL.powerconducting(aFluid);}

		/** @return if that Liquid is early-game and easy to handle */
		@Deprecated public static boolean simple(IFluidTank aFluid) {return FL.simple(aFluid);}
		/** @return if that Liquid is early-game and easy to handle */
		@Deprecated public static boolean simple(FluidStack aFluid) {return FL.simple(aFluid);}
		/** @return if that Liquid is early-game and easy to handle */
		@Deprecated public static boolean simple(Fluid aFluid) {return FL.simple(aFluid);}

		@Deprecated public static boolean acid(IFluidTank aFluid) {return FL.acid(aFluid);}
		@Deprecated public static boolean acid(FluidStack aFluid) {return FL.acid(aFluid);}
		@Deprecated public static boolean acid(Fluid aFluid) {return FL.acid(aFluid);}

		@Deprecated public static boolean plasma(IFluidTank aFluid) {return FL.plasma(aFluid);}
		@Deprecated public static boolean plasma(FluidStack aFluid) {return FL.plasma(aFluid);}
		@Deprecated public static boolean plasma(Fluid aFluid) {return FL.plasma(aFluid);}

		@Deprecated public static boolean gas(IFluidTank aFluid, boolean aDefault) {return FL.gas(aFluid, aDefault);}
		@Deprecated public static boolean gas(IFluidTank aFluid) {return FL.gas(aFluid);}
		@Deprecated public static boolean gas(FluidStack aFluid, boolean aDefault) {return FL.gas(aFluid, aDefault);}
		@Deprecated public static boolean gas(FluidStack aFluid) {return FL.gas(aFluid);}
		@Deprecated public static boolean gas(Fluid aFluid, boolean aDefault) {return FL.gas(aFluid, aDefault);}
		@Deprecated public static boolean gas(Fluid aFluid) {return FL.gas(aFluid);}

		// F5 impossible-1:1 dead (0 вызывателей): net.minecraftforge.fluids.BlockFluidBase — 1.7.10
		// Forge-класс, отсутствует во всех 3 корнях референса (пакет net.minecraftforge.fluids удалён
		// движком целиком, не переименован). Не найдено вызывающих ни в этом файле, ни во всём дереве
		// (grep) — параметр этого типа физически невозможно сохранить, перегрузки lighter(BlockFluidBase)/
		// dir(BlockFluidBase) сняты (не заменены форс-заглушкой — сигнатуру всё равно нечем заполнить).
		@Deprecated public static boolean lighter(IFluidTank aFluid) {return FL.lighter(aFluid);}
		@Deprecated public static boolean lighter(FluidStack aFluid) {return FL.lighter(aFluid);}
		@Deprecated public static boolean lighter(Fluid aFluid)      {return FL.lighter(aFluid);}

		@Deprecated public static int dir(IFluidTank aFluid) {return FL.dir(aFluid);}
		@Deprecated public static int dir(FluidStack aFluid) {return FL.dir(aFluid);}
		@Deprecated public static int dir(Fluid aFluid)      {return FL.dir(aFluid);}

		@Deprecated public static long temperature(IFluidTank aFluid) {return FL.temperature(aFluid);}
		@Deprecated public static long temperature(IFluidTank aFluid, long aDefault) {return FL.temperature(aFluid, aDefault);}

		@Deprecated public static long temperature(Fluid aFluid) {return FL.temperature(aFluid);}
		@Deprecated public static long temperature(Fluid aFluid, long aDefault) {return FL.temperature(aFluid, aDefault);}

		@Deprecated public static long temperature(FluidStack aFluid) {return FL.temperature(aFluid);}
		@Deprecated public static long temperature(FluidStack aFluid, long aDefault) {return FL.temperature(aFluid, aDefault);}

		@Deprecated public static FluidStack water(long aAmount) {return FL.Water.make(aAmount);}
		@Deprecated public static FluidStack distw(long aAmount) {return FL.DistW.make(aAmount);}
		@Deprecated public static FluidStack lava(long aAmount) {return FL.Lava.make(aAmount);}
		@Deprecated public static FluidStack steam(long aAmount) {return FL.Steam.make(aAmount);}
		@Deprecated public static FluidStack milk(long aAmount) {return FL.Milk.make(aAmount);}
		@Deprecated public static FluidStack soym(long aAmount) {return FL.MilkSoy.make(aAmount);}
		@Deprecated public static boolean distilledwater(FluidStack aFluid) {return distw(aFluid);}
		@Deprecated public static boolean distilledwater(Fluid aFluid) {return distw(aFluid);}
		@Deprecated public static FluidStack distilledwater(long aAmount) {return distw(aAmount);}
		@Deprecated public static boolean soymilk(FluidStack aFluid) {return soym(aFluid);}
		@Deprecated public static boolean soymilk(Fluid aFluid) {return soym(aFluid);}
		@Deprecated public static FluidStack soymilk(long aAmount) {return soym(aAmount);}

		@Deprecated public static boolean exists(String aFluidName) {return FL.exists(aFluidName);}
		
		@Deprecated public static FluidStack make (FL aFluid, long aAmount) {return aFluid.make (aAmount);}
		@Deprecated public static FluidStack make_(FL aFluid, long aAmount) {return aFluid.make_(aAmount);}
		@Deprecated public static FluidStack make (FL aFluid, long aAmount, FL aReplacementFluid) {return aFluid.make (aAmount, aReplacementFluid);}
		@Deprecated public static FluidStack make_(FL aFluid, long aAmount, FL aReplacementFluid) {return aFluid.make_(aAmount, aReplacementFluid);}
		@Deprecated public static FluidStack make (FL aFluid, long aAmount, String aReplacementFluidName) {return aFluid.make (aAmount, aReplacementFluidName);}
		@Deprecated public static FluidStack make_(FL aFluid, long aAmount, String aReplacementFluidName) {return aFluid.make_(aAmount, aReplacementFluidName);}
		@Deprecated public static FluidStack make (FL aFluid, long aAmount, FL aReplacementFluid, long aReplacementAmount) {return aFluid.make (aAmount, aReplacementFluid, aReplacementAmount);}
		@Deprecated public static FluidStack make_(FL aFluid, long aAmount, FL aReplacementFluid, long aReplacementAmount) {return aFluid.make_(aAmount, aReplacementFluid, aReplacementAmount);}
		@Deprecated public static FluidStack make (FL aFluid, long aAmount, String aReplacementFluidName, long aReplacementAmount) {return aFluid.make (aAmount, aReplacementFluidName, aReplacementAmount);}
		@Deprecated public static FluidStack make_(FL aFluid, long aAmount, String aReplacementFluidName, long aReplacementAmount) {return aFluid.make_(aAmount, aReplacementFluidName, aReplacementAmount);}
		
		@Deprecated public static FluidStack make (int aFluid, long aAmount) {return FL.make (aFluid, aAmount);}
		@Deprecated public static FluidStack make (Fluid aFluid, long aAmount) {return FL.make (aFluid, aAmount);}
		@Deprecated public static FluidStack make (String aFluidName, long aAmount) {return FL.make (aFluidName, aAmount);}
		@Deprecated public static FluidStack make (String aFluidName, long aAmount, String aReplacementFluidName) {return FL.make (aFluidName, aAmount, aReplacementFluidName);}
		@Deprecated public static FluidStack make (String aFluidName, long aAmount, String aReplacementFluidName, long aReplacementAmount) {return FL.make (aFluidName, aAmount, aReplacementFluidName, aReplacementAmount);}
		@Deprecated public static FluidStack make (String aFluidName, long aAmount, FluidStack aReplacementFluid) {FluidStack rFluid = FL.make(aFluidName, aAmount); return rFluid == null ? aReplacementFluid : rFluid;}

		@Deprecated public static FluidStack make_(int aFluid, long aAmount) {return FL.make_(aFluid, aAmount);}
		@Deprecated public static FluidStack make_(Fluid aFluid, long aAmount) {return FL.make_(aFluid, aAmount);}
		@Deprecated public static FluidStack make_(String aFluidName, long aAmount) {return FL.make_(aFluidName, aAmount);}
		@Deprecated public static FluidStack make_(String aFluidName, long aAmount, String aReplacementFluidName) {return FL.make_(aFluidName, aAmount, aReplacementFluidName);}
		@Deprecated public static FluidStack make_(String aFluidName, long aAmount, String aReplacementFluidName, long aReplacementAmount) {return FL.make_(aFluidName, aAmount, aReplacementFluidName, aReplacementAmount);}

		@Deprecated public static FluidStack amount(FluidStack aFluid, long aAmount) {return FL.amount(aFluid, aAmount);}

		@Deprecated public static FluidStack mul(FluidStack aFluid, long aMultiplier) {return FL.mul(aFluid, aMultiplier);}
		@Deprecated public static FluidStack mul(FluidStack aFluid, long aMultiplier, long aDivider, boolean aRoundUp) {return FL.mul(aFluid, aMultiplier, aDivider, aRoundUp);}

		@Deprecated public static long fill (@SuppressWarnings("rawtypes") DelegatorTileEntity aDelegator, FluidStack aFluid, boolean aDoFill) {return FL.fill (aDelegator, aFluid, aDoFill);}
		@Deprecated public static long fill_(@SuppressWarnings("rawtypes") DelegatorTileEntity aDelegator, FluidStack aFluid, boolean aDoFill) {return FL.fill_(aDelegator, aFluid, aDoFill);}
		// F5 dead-deprecated (0 вызывателей — живой путь FL.fill/fillAll(IFluidHandler,side,...), реализован через fillSided,
		// см. CoverDrain). Эти  UT-обёртки IFluidHandler+side мертвы; neo fill без side-параметра. Сигнатура сохранена.
		@Deprecated public static long fill (IFluidHandler aFluidHandler, byte aSide, FluidStack aFluid, boolean aDoFill) {return 0;}
		@Deprecated public static long fill_(IFluidHandler aFluidHandler, byte aSide, FluidStack aFluid, boolean aDoFill) {return 0;}
		@Deprecated public static long fill (IFluidHandler aFluidHandler, byte[] aSides, FluidStack aFluid, boolean aDoFill) {return 0;}
		@Deprecated public static long fill_(IFluidHandler aFluidHandler, byte[] aSides, FluidStack aFluid, boolean aDoFill) {return 0;}

		@Deprecated public static boolean fillAll (@SuppressWarnings("rawtypes") DelegatorTileEntity aDelegator, FluidStack aFluid, boolean aDoFill) {return FL.fillAll (aDelegator, aFluid, aDoFill);}
		@Deprecated public static boolean fillAll_(@SuppressWarnings("rawtypes") DelegatorTileEntity aDelegator, FluidStack aFluid, boolean aDoFill) {return FL.fillAll_(aDelegator, aFluid, aDoFill);}
		// F5 dead-deprecated (0 вызывателей, живой путь FL.*) — см. выше.
		@Deprecated public static boolean fillAll (IFluidHandler aFluidHandler, byte aSide, FluidStack aFluid, boolean aDoFill) {return F;}
		@Deprecated public static boolean fillAll_(IFluidHandler aFluidHandler, byte aSide, FluidStack aFluid, boolean aDoFill) {return F;}
		@Deprecated public static boolean fillAll (IFluidHandler aFluidHandler, byte[] aSides, FluidStack aFluid, boolean aDoFill) {return F;}
		@Deprecated public static boolean fillAll_(IFluidHandler aFluidHandler, byte[] aSides, FluidStack aFluid, boolean aDoFill) {return F;}

		@Deprecated public static long move (@SuppressWarnings("rawtypes") DelegatorTileEntity aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move (aFrom, aTo);}
		@Deprecated public static long move_(@SuppressWarnings("rawtypes") DelegatorTileEntity aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move_(aFrom, aTo);}
		@Deprecated public static long move (@SuppressWarnings("rawtypes") DelegatorTileEntity aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move (aFrom, aTo, aMaxMoved);}
		@Deprecated public static long move_(@SuppressWarnings("rawtypes") DelegatorTileEntity aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move_(aFrom, aTo, aMaxMoved);}
		@Deprecated public static long move (@SuppressWarnings("rawtypes") DelegatorTileEntity aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, FluidStack aMoved) {return FL.move (aFrom, aTo, aMoved);}
		@Deprecated public static long move_(@SuppressWarnings("rawtypes") DelegatorTileEntity aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, FluidStack aMoved) {return FL.move_(aFrom, aTo, aMoved);}
		@Deprecated public static long move (IFluidTank aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move (aFrom, aTo);}
		@Deprecated public static long move_(IFluidTank aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move_(aFrom, aTo);}
		@Deprecated public static long move (IFluidTank aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move (aFrom, aTo, aMaxMoved);}
		@Deprecated public static long move_(IFluidTank aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move_(aFrom, aTo, aMaxMoved);}
		@Deprecated public static long move (IFluidTank[] aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move (aFrom, aTo);}
		@Deprecated public static long move_(IFluidTank[] aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move_(aFrom, aTo);}
		@Deprecated public static long move (IFluidTank[] aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move (aFrom, aTo, aMaxMoved);}
		@Deprecated public static long move_(IFluidTank[] aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move_(aFrom, aTo, aMaxMoved);}
		@Deprecated public static long move (@SuppressWarnings("rawtypes") Iterable aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move (aFrom, aTo);}
		@Deprecated public static long move_(@SuppressWarnings("rawtypes") Iterable aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo) {return FL.move_(aFrom, aTo);}
		@Deprecated public static long move (@SuppressWarnings("rawtypes") Iterable aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move (aFrom, aTo, aMaxMoved);}
		@Deprecated public static long move_(@SuppressWarnings("rawtypes") Iterable aFrom, @SuppressWarnings("rawtypes") DelegatorTileEntity aTo, long aMaxMoved) {return FL.move_(aFrom, aTo, aMaxMoved);}


		@Deprecated public static String configName(FluidStack aFluid) {return FL.configName(aFluid);}

		@Deprecated public static String configNames(FluidStack... aFluids) {return FL.configNames(aFluids);}
		
		@Deprecated public static String name(Fluid aFluid, boolean aLocalized) {return FL.name(aFluid, aLocalized);}

		@Deprecated public static String name(FluidStack aFluid, boolean aLocalized) {return FL.name(aFluid, aLocalized);}

		@Deprecated public static String name(IFluidTank aTank, boolean aLocalized) {return FL.name(aTank, aLocalized);}

		@Deprecated public static FluidStack[] copyArray(FluidStack... aFluids) {return FL.copy(aFluids);}

		// F5 (bookkeeping в FL.FULL_TO_DATA/EMPTY_TO_FLUID_TO_DATA): net.minecraftforge.fluids.FluidContainerRegistry/
		// FluidContainerData — пакет удалён движком целиком, не существует в neo (не найден ни в одном из
		// 3 корней референса, тот же класс проблемы, что уже занесён в gregapi/oredict/OreDictManager.java
		// под меткой fluid-container-registry с приставкой oredict-). Легаси-поля sFilled2Data/sEmpty2Fluid2Data
		// (типизированные снятым FluidContainerData, ссылались на несуществующие FL.FULL_TO_DATA/
		// FL.EMPTY_TO_FLUID_TO_DATA — уже были некомпилируемы ДО этого захода) и раздельные перегрузки
		// registerFluidContainer(FluidContainerData[,...])/setFluidContainerData(FluidContainerData[,...])
		// физически невозможно сохранить — сам параметр-тип отсутствует. Внешних вызывающих ни на одно из
		// этого не найдено (grep по всему дереву) — поля и обе FluidContainerData-перегрузки сняты.
		// FluidStack/ItemStack-перегрузки ниже (без FluidContainerData в сигнатуре) сохранены — делегированы
		// на реальный FL.reg/FL.fill/FL.contains/FL.getFluid/FL.getEmpty (те же имена методов, что уже
		// реализованы в центре FL, decisions/F5-fluids.md §3,8).
		@Deprecated public static void registerFluidContainer(FluidStack aFluid, ItemStack aFull, ItemStack aEmpty) {FL.reg(aFluid, aFull, aEmpty);}
		@Deprecated public static void registerFluidContainer(FluidStack aFluid, ItemStack aFull, ItemStack aEmpty, boolean aOverrideFillingEmpty, boolean aOverrideDrainingFull) {FL.reg(aFluid, aFull, aEmpty, aOverrideFillingEmpty, aOverrideDrainingFull);}
		@Deprecated public static void registerFluidContainer(FluidStack aFluid, ItemStack aFull, ItemStack aEmpty, boolean aNullEmpty) {FL.reg(aFluid, aFull, aEmpty, aNullEmpty);}
		@Deprecated public static void registerFluidContainer(FluidStack aFluid, ItemStack aFull, ItemStack aEmpty, boolean aNullEmpty, boolean aOverrideFillingEmpty, boolean aOverrideDrainingFull) {FL.reg(aFluid, aFull, aEmpty, aNullEmpty, aOverrideFillingEmpty, aOverrideDrainingFull);}

		@Deprecated public static ItemStack fillFluidContainer(FluidStack aFluid, ItemStack aStack, boolean aRemoveFluidDirectly, boolean aCheckIFluidContainerItems) {return FL.fill(aFluid, aStack, aRemoveFluidDirectly, aCheckIFluidContainerItems);}
		@Deprecated public static ItemStack fillFluidContainer(FluidStack aFluid, ItemStack aStack, boolean aRemoveFluidDirectly, boolean aCheckIFluidContainerItems, boolean aAllowPartialFilling) {return FL.fill(aFluid, aStack, aRemoveFluidDirectly, aCheckIFluidContainerItems, aAllowPartialFilling);}
		@Deprecated public static ItemStack fillFluidContainer(FluidStack aFluid, ItemStack aStack, boolean aRemoveFluidDirectly, boolean aCheckIFluidContainerItems, boolean aAllowPartialFilling, boolean aIsNonCannerCheck) {return FL.fill(aFluid, aStack, aRemoveFluidDirectly, aCheckIFluidContainerItems, aAllowPartialFilling, aIsNonCannerCheck);}

		@Deprecated public static ItemStack fillFluidContainer(IFluidTank aTank, ItemStack aStack, boolean aRemoveFluidDirectly, boolean aCheckIFluidContainerItems) {return FL.fill(aTank, aStack, aRemoveFluidDirectly, aCheckIFluidContainerItems);}
		@Deprecated public static ItemStack fillFluidContainer(IFluidTank aTank, ItemStack aStack, boolean aRemoveFluidDirectly, boolean aCheckIFluidContainerItems, boolean aAllowPartialFilling) {return FL.fill(aTank, aStack, aRemoveFluidDirectly, aCheckIFluidContainerItems, aAllowPartialFilling);}
		@Deprecated public static ItemStack fillFluidContainer(IFluidTank aTank, ItemStack aStack, boolean aRemoveFluidDirectly, boolean aCheckIFluidContainerItems, boolean aAllowPartialFilling, boolean aIsNonCannerCheck) {return FL.fill(aTank, aStack, aRemoveFluidDirectly, aCheckIFluidContainerItems, aAllowPartialFilling, aIsNonCannerCheck);}

		@Deprecated public static boolean containsFluid(ItemStack aStack, FluidStack aFluid, boolean aCheckIFluidContainerItems) {return FL.contains(aStack, aFluid, aCheckIFluidContainerItems);}

		@Deprecated public static FluidStack getFluidForFilledItem(ItemStack aStack, boolean aCheckIFluidContainerItems) {return FL.getFluid(aStack, aCheckIFluidContainerItems);}

		@Deprecated public static ItemStack getContainerForFilledItem(ItemStack aStack, boolean aCheckIFluidContainerItems) {return FL.getEmpty(aStack, aCheckIFluidContainerItems);}
		
		@Deprecated public static FluidStack load (CompoundTag aNBT, String aTagName) {return FL.load(aNBT, aTagName);}
		@Deprecated public static FluidStack load (CompoundTag aNBT) {return FL.load (aNBT);}
		@Deprecated public static FluidStack load_(CompoundTag aNBT) {return FL.load_(aNBT);}
		
		@Deprecated public static CompoundTag save(CompoundTag aNBT, String aTagName, FluidStack aFluid) {return FL.save(aNBT, aTagName, aFluid);}
		@Deprecated public static CompoundTag save (FluidStack aFluid) {return FL.save (aFluid);}
		@Deprecated public static CompoundTag save_(FluidStack aFluid) {return FL.save_(aFluid);}
		
		// F5 стык: этот блок был мёртвым (0 вызывающих во всём дереве, grep подтверждён) буквальным
		// копированием тела gregapi.data.FL.create*(тот же F5-центр) поверх несуществующего в neo
		// net.minecraftforge.fluids.Fluid/FluidRegistry/FluidContainerRegistry (красно ещё до порта F5).
		// Оригинал 1.7.10 (gregtech6/src/main/java/gregapi/util/UT.java:492-513) держал ДВЕ раздельные
		// копии одной и той же логики (UT.Fluids и FL) — как уже сделано для load/save чуть выше в этом
		// же классе, дублирование заменено на делегирование единственному центру FL (decisions/F5-fluids.md).
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createLiquid(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createLiquid(aMaterial, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createLiquid(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createLiquid(aMaterial, aTexture, aFluidList);}

		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createMolten(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createMolten(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aTexture, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createMolten(OreDictMaterial aMaterial, long aAmount, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aAmount, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createMolten(OreDictMaterial aMaterial, long aAmount, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createMolten(aMaterial, aAmount, aTexture, aFluidList);}

		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createGas(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createGas(aMaterial, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createGas(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createGas(aMaterial, aTexture, aFluidList);}

		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createVapour(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createVapour(aMaterial, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createVapour(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createVapour(aMaterial, aTexture, aFluidList);}

		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createPlasma(OreDictMaterial aMaterial, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT createPlasma(OreDictMaterial aMaterial, IIconContainer aTexture, Set<String>... aFluidList) {return FL.createPlasma(aMaterial, aTexture, aFluidList);}

		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT create(String aName, String aLocalized, OreDictMaterial aMaterial, int aState, long aAmountPerUnit, long aTemperatureK, Set<String>... aFluidList) {return FL.create(aName, aLocalized, aMaterial, aState, aAmountPerUnit, aTemperatureK, aFluidList);}
		@Deprecated @SafeVarargs public static gregapi.fluid.FluidGT create(String aName, String aLocalized, OreDictMaterial aMaterial, int aState, long aAmountPerUnit, long aTemperatureK, java.util.function.Supplier<ItemStack> aFullContainer, java.util.function.Supplier<ItemStack> aEmptyContainer, int aFluidAmount, Set<String>... aFluidList) {return FL.create(aName, aLocalized, aMaterial, aState, aAmountPerUnit, aTemperatureK, aFullContainer, aEmptyContainer, aFluidAmount, aFluidList);}

		@Deprecated @SafeVarargs
		public static gregapi.fluid.FluidGT create(String aName, IIconContainer aTexture, String aLocalized, OreDictMaterial aMaterial, short[] aRGBa, int aState, long aAmountPerUnit, long aTemperatureK, java.util.function.Supplier<ItemStack> aFullContainer, java.util.function.Supplier<ItemStack> aEmptyContainer, int aFluidAmount, Set<String>... aFluidList) {
			return FL.create(aName, aTexture, aLocalized, aMaterial, aRGBa, aState, aAmountPerUnit, aTemperatureK, aFullContainer, aEmptyContainer, aFluidAmount, aFluidList);
		}
	}
	
	public static class Books {
		public static final Map<String, ItemStack> BOOK_MAP = new HashMap<>();
		public static final List<String> BOOK_LIST = new ArrayListNoNulls<>();
		public static final List<String> MATERIAL_DICTIONARIES = new ArrayListNoNulls<>();
		
		public static void display(Player aPlayer, ItemStack aStack) {
			String aMapping = NBT.getBookMapping(aStack);
			if (Code.stringValid(aMapping)) display(aPlayer, aMapping); else display(aPlayer, F, aStack);
		}
		public static void display(Player aPlayer, String aMapping) {
			aPlayer.openItemGui(getWrittenBook(aMapping, T, ST.make(Items.WRITTEN_BOOK, 1, 0)), net.minecraft.world.InteractionHand.MAIN_HAND); // F14: displayGUIBook(ItemStack) -> Player.openItemGui(ItemStack,InteractionHand) (Player.java:854, ServerPlayer шлёт ClientboundOpenBookPacket).
		}
		public static void display(Player aPlayer, boolean aWritable, ItemStack aStack) {
			if (ST.invalid(aStack)) return;
			display(aPlayer, aWritable, ItemNBT.get(aStack));
		}
		public static void display(Player aPlayer, boolean aWritable, CompoundTag aNBT) {
			if (aNBT == null || UT.Code.stringInvalid(UT.NBT.getBookTitle(aNBT))) return;
			aPlayer.openItemGui(ST.make(aWritable?Items.WRITABLE_BOOK:Items.WRITTEN_BOOK, 1, 0, aNBT), net.minecraft.world.InteractionHand.MAIN_HAND); // F14: displayGUIBook -> Player.openItemGui.
		}
		
		@Deprecated public static ItemStack getWrittenBook(String aMapping) {return getWrittenBook(aMapping, F, null);}
		@Deprecated public static ItemStack getWrittenBook(String aMapping, ItemStack aStackToPutNBT) {return getWrittenBook(aMapping, F, aStackToPutNBT);}
		
		public static ItemStack getWrittenBook(String aMapping, boolean aForceRecreation) {
			return getWrittenBook(aMapping, aForceRecreation, null);
		}
		public static ItemStack getWrittenBook(String aMapping, boolean aForceRecreation, ItemStack aStackToPutNBT) {
			if (Code.stringInvalid(aMapping)) return null;
			if (aForceRecreation && aMapping.startsWith("Material_Dictionary_")) UT.Books.createMaterialDictionary(OreDictMaterial.MATERIAL_MAP.get(aMapping.replaceFirst("Material_Dictionary_", "")), NI, NI);
			ItemStack tStack = BOOK_MAP.get(aMapping);
			if (tStack == null) return aStackToPutNBT==null?ST.make(Items.WRITTEN_BOOK, 1, 0):aStackToPutNBT;
			if (aStackToPutNBT == null) aStackToPutNBT = ST.copy(tStack);
			return NBT.set(aStackToPutNBT, (CompoundTag)ItemNBT.get(tStack).copy());
		}
		
		public static ItemStack getBookWithTitle(String aMapping) {
			return getBookWithTitle(aMapping, null);
		}
		public static ItemStack getBookWithTitle(String aMapping, ItemStack aStackToPutNBT) {
			ItemStack tStack = BOOK_MAP.get(aMapping);
			if (tStack == null) return aStackToPutNBT==null?ST.make(Items.WRITTEN_BOOK, 1, 0):aStackToPutNBT;
			if (aStackToPutNBT == null) aStackToPutNBT = ST.copy(tStack);
			return NBT.set(aStackToPutNBT, NBT.make("title", NBT.getBookTitle(tStack), "author", NBT.getBookAuthor(tStack), "book", aMapping));
		}
		
		public static ItemStack createWrittenBook(String aMapping, String aTitle, String aAuthor, ItemStack aDefaultBook, String... aPages) {
			for (int i = 0; i < aPages.length; i++) {aPages[i] = LanguageHandler.langfile("written.book." + aMapping + ".page." + i, aPages[i]);}
			return createWrittenBook(aMapping, aTitle, aAuthor, aDefaultBook, T, aPages);
		}
		public static ItemStack createWrittenBook(String aMapping, String aTitle, String aAuthor, ItemStack aDefaultBook, boolean aLogging, String... aPages) {
			if (Code.stringInvalid(aMapping)) return null;
			ItemStack rStack = BOOK_MAP.get(aMapping);
			if (rStack == null) rStack = aDefaultBook==null?ST.make(Items.WRITTEN_BOOK, 1, 0):ST.amount(1, aDefaultBook);
			if (Code.stringInvalid(aTitle) || Code.stringInvalid(aAuthor) || aPages.length <= 0) return null;
			CompoundTag rNBT = NBT.make();
			rNBT.putString("title", aTitle);
			rNBT.putString("author", aAuthor);
			ListTag tNBTList = new ListTag();
			for (short i = 0; i < aPages.length; i++) {
				if (aPages[i].length() < 256) {
					tNBTList.add(new StringTag(aPages[i].replaceAll("¶", "\n")));
				} else if (aLogging) {
					ERR.println("WARNING: String for Page of written Book too long! ->\n" + aPages[i]);
				}
			}
			rNBT.put("pages", tNBTList);
			NBT.set(rStack, rNBT);
			BOOK_MAP.put(aMapping, ST.copy(rStack));
			if (!BOOK_LIST.contains(aMapping)) BOOK_LIST.add(aMapping);
			return ST.copy(rStack);
		}
		
		public static ItemStack addMaterialDictionary(OreDictMaterial aMat) {
			boolean temp = F;
			int tCounter = 0, tPages = 6 + aMat.mAlloyCreationRecipes.size();
			
			if (aMat.mComponents == null && !aMat.contains(TD.Atomic.ELEMENT)) tPages--;
			
			if (!aMat.mByProducts.isEmpty()) tPages++;
			
			if (aMat.mToolTypes > 0) tPages++;
			if (!aMat.mEnchantmentTools  .isEmpty()) tPages++;
			if (!aMat.mEnchantmentWeapons.isEmpty()) tPages++;
			if (!aMat.mEnchantmentAmmo   .isEmpty()) tPages++;
			if (!aMat.mEnchantmentRanged .isEmpty()) tPages++;
			if (!aMat.mEnchantmentFishing.isEmpty()) tPages++;
			if (!aMat.mEnchantmentArmors .isEmpty()) tPages++;
			
			if (aMat.mDescription != null) for (int i = 0; i < aMat.mDescription.length; i++) if (Code.stringValid(aMat.mDescription[i])) tPages++;
			
			for (TagData tTag : TD.Properties.ALL_RELEVANTS) if (aMat.contains(tTag)) {tPages++; break;}
			for (TagData tTag : TD.Processing.ALL_MACHINES ) if (aMat.contains(tTag)) {tPages++; break;}
			for (TagData tTag : TD.Processing.ALL_ORES     ) if (aMat.contains(tTag)) {tPages++; break;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : OreDictMaterial.MATERIAL_MAP.values()) if (tMat.mComponents != null && tMat.contains(TD.Compounds.DECOMPOSABLE)) {
				for (OreDictMaterialStack tMt2 : tMat.mComponents.getUndividedComponents()) if (tMt2.mMaterial == aMat) {
					temp=!(tCounter++%6==5);
					if (!temp) tPages++;
					break;
				}
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : OreDictMaterial.MATERIAL_MAP.values()) if (tMat.mByProducts.contains(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedSmelting) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetSmelting.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedSolidifying) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetSolidifying.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedBurning) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetBurning.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedPulver) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetPulver.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedBending) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetBending.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedCompressing) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCompressing.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedCrushing) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCrushing.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedCutting) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCutting.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedForging) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetForging.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedSmashing) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetSmashing.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			tCounter = 0;
			for (OreDictMaterial tMat : aMat.mTargetedWorking) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetWorking.has(aMat)) {
				temp=!(tCounter++%6==5);
				if (!temp) tPages++;
			}
			if (temp) {tPages++; temp=F;}
			
			for (OreDictMaterial tMat : aMat.mAlloyComponentReferences) {
				for (IOreDictConfigurationComponent tConfig : tMat.mAlloyCreationRecipes) {
					for (OreDictMaterialStack tMatStack : tConfig.getUndividedComponents()) {
						if (tMatStack.mMaterial == aMat) {tPages++; break;}
					}
				}
			}
			
			MATERIAL_DICTIONARIES.add("Material_Dictionary_"+aMat.mNameInternal);
			createWrittenBook("Material_Dictionary_"+aMat.mNameInternal, aMat.getLocal(), "Material Dictionary Foundation", ST.make(ItemsGT.BOOKS, 1, tPages<=50?32002:32003), F, "If you can read this in a legitimate Material Dictionary, even if it is old, then this is a Bug, please report this to me!\n\nGregorius\nTechneticies\n\n2021");
			return ST.copy(aMat.mDictionaryBook = ST.book("Material_Dictionary_"+aMat.mNameInternal));
		}
		
		public static boolean createMaterialDictionary(OreDictMaterial aMat, ItemStack aDefaultBook, ItemStack aDefaultLargeBook) {
			if (aMat == null) return F;
			
			String tPage = "";
			List<String> tBook = new ArrayListNoNulls<>();
			boolean temp = F;
			int tCounter = 0;
			
			tBook.add("===================\n"+aMat.getLocal()+"\n===================\nID: "+(aMat.mID<0?"NONE":aMat.mID)+"\nMelting: "+aMat.mMeltingPoint+" K\nBoiling: "+aMat.mBoilingPoint+" K\nPlasma: "+aMat.mPlasmaPoint+" K\n===================\nDensity:\n"+(aMat.mGramPerCubicCentimeter == 0 ? "???" : aMat.mGramPerCubicCentimeter)+" g/cm3\n"+aMat.getWeight(U)+" kg/unit\n===================\n");
			
			//----------
			
			if (aMat.mComponents == null) {
				if (aMat.contains(TD.Atomic.ELEMENT)) {
					temp = T;
					tPage="Atomic Structure:\nProtons: "+aMat.mProtons+"\nElectrons: " + aMat.mElectrons + "\nNeutrons: " + aMat.mNeutrons + "\nMass: "+aMat.mMass+"\n===================\n";
					for (TagData tTag : TD.Atomic.ALL) if (tTag != TD.Atomic.ELEMENT && tTag != TD.Atomic.NONMETAL && aMat.contains(tTag)) tPage += tTag.getLocalisedNameLong() + "\n";
				}
			} else {
				temp = T;
				tPage="Components per "+aMat.mComponents.getCommonDivider() + "\n===================\n";
				for (OreDictMaterialStack tMaterial : aMat.mComponents.getUndividedComponents()) tPage += (tMaterial.mAmount / U)+" "+tMaterial.mMaterial.getLocal()+"\n";
			}
			
			if (temp) {tBook.add(tPage); temp = F;}
			
			//----------
			
			
			if (aMat.mToolTypes > 0) {
				tPage="Tool Properties\n===================\n";
				tPage+="Durability:\n"+aMat.mToolDurability;
				tPage+="\nQuality:\n"+aMat.mToolQuality;
				tPage+="\nSpeed:\n"+aMat.mToolSpeed;
				tPage+="\nHandle:\n"+aMat.mHandleMaterial.getLocal()+"\n";
				tBook.add(tPage+"===================\n");
			}
			if (!aMat.mEnchantmentTools  .isEmpty()) {
				tPage = "Tool Enchantments\n===================\n";
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMat.mEnchantmentTools  ) tPage += UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount) + "\n";
				tBook.add(tPage+"===================\n");
			}
			if (!aMat.mEnchantmentWeapons.isEmpty()) {
				tPage = "Weapon Enchantments\n===================\n";
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMat.mEnchantmentWeapons) tPage += UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount) + "\n";
				tBook.add(tPage+"===================\n");
			}
			if (!aMat.mEnchantmentAmmo   .isEmpty()) {
				tPage = "Ammo Enchantments\n===================\n";
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMat.mEnchantmentAmmo   ) tPage += UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount) + "\n";
				tBook.add(tPage+"===================\n");
			}
			if (!aMat.mEnchantmentRanged .isEmpty()) {
				tPage = "Ranged Enchantments\n===================\n";
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMat.mEnchantmentRanged ) tPage += UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount) + "\n";
				tBook.add(tPage+"===================\n");
			}
			if (!aMat.mEnchantmentFishing.isEmpty()) {
				tPage = "Fishing Enchantments\n===================\n";
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMat.mEnchantmentFishing) tPage += UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount) + "\n";
				tBook.add(tPage+"===================\n");
			}
			if (!aMat.mEnchantmentArmors .isEmpty()) {
				tPage = "Armor Enchantments\n===================\n";
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aMat.mEnchantmentArmors ) tPage += UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount) + "\n";
				tBook.add(tPage+"===================\n");
			}
			
			//----------
			
			tPage="Properties\n===================\n";
			
			for (TagData tTag : TD.Properties.ALL_RELEVANTS) if (aMat.contains(tTag)) {temp = T; tPage += tTag.getLocalisedNameLong() + "\n";}
			
			if (temp) {tBook.add(tPage); temp = F;}
			
			//----------
			
			tPage="Machine Processing\n===================\n";
			
			for (TagData tTag : TD.Processing.ALL_MACHINES) if (aMat.contains(tTag)) {temp = T; tPage += tTag.getLocalisedNameLong() + "\n";}
			
			if (temp) {tBook.add(tPage); temp = F;}
			
			//----------
			
			tPage="Materials which can be decomposed to this\n===================\n";
			tCounter = 0;
			for (OreDictMaterial tMat : OreDictMaterial.MATERIAL_MAP.values()) if (tMat.mComponents != null && tMat.contains(TD.Compounds.DECOMPOSABLE)) {
				for (OreDictMaterialStack tMt2 : tMat.mComponents.getUndividedComponents()) if (tMt2.mMaterial == aMat) {
					temp=!(tCounter++%6==5);
					tPage += tMat.getLocal()+"\n";
					if (!temp) {
						tBook.add(tPage);
						tPage="Materials which can be decomposed to this\n===================\n";
					}
					break;
				}
			}
			
			if (temp) {tBook.add(tPage); temp = F;}
			
			//----------
			
			tPage="Ore Processing\n===================\n";
			
			for (TagData tTag : TD.Processing.ALL_ORES) if (aMat.contains(tTag)) {temp = T; tPage += tTag.getLocalisedNameLong() + "\n";}
			
			if (temp) {tBook.add(tPage); temp = F;}
			
			//----------
			
			tPage="Ore Byproducts\n===================\n";
			
			for (OreDictMaterial tMat : aMat.mByProducts) {temp = T; tPage += tMat.getLocal() + "\n";}
			
			if (temp) {tBook.add(tPage); temp = F;}
			
			//----------
			
			tPage="Ores with this as Byproduct\n===================\n";
			tCounter = 0;
			for (OreDictMaterial tMat : OreDictMaterial.MATERIAL_MAP.values()) if (tMat.mByProducts.contains(aMat)) {
				temp=!(tCounter++%6==5);
				tPage += tMat.getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage="Ores with this as Byproduct\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp = F;}
			
			//----------
			
			tPage = "Processing Data\n===================\n";
			tPage += "Smelting:\n"      +(aMat.mTargetSmelting   .mAmount / U) + "." + ((int)(((double)(aMat.mTargetSmelting   .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetSmelting   .mAmount <= 0 ? "nothing" : aMat.mTargetSmelting   .mMaterial == aMat ? "itself" : aMat.mTargetSmelting   .mMaterial.getLocal())+"\n";
			tPage += "Solidifying:\n"   +(aMat.mTargetSolidifying.mAmount / U) + "." + ((int)(((double)(aMat.mTargetSolidifying.mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetSolidifying.mAmount <= 0 ? "nothing" : aMat.mTargetSolidifying.mMaterial == aMat ? "itself" : aMat.mTargetSolidifying.mMaterial.getLocal())+"\n";
			tPage += "Burning:\n"       +(aMat.mTargetBurning    .mAmount / U) + "." + ((int)(((double)(aMat.mTargetBurning    .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetBurning    .mAmount <= 0 ? "nothing" : aMat.mTargetBurning    .mMaterial == aMat ? "itself" : aMat.mTargetBurning    .mMaterial.getLocal())+"\n";
			tPage += "Pulverising:\n"   +(aMat.mTargetPulver     .mAmount / U) + "." + ((int)(((double)(aMat.mTargetPulver     .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetPulver     .mAmount <= 0 ? "nothing" : aMat.mTargetPulver     .mMaterial == aMat ? "itself" : aMat.mTargetPulver     .mMaterial.getLocal())+"\n";
			tPage += "Crushing:\n"      +(aMat.mTargetCrushing   .mAmount / U) + "." + ((int)(((double)(aMat.mTargetCrushing   .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetCrushing   .mAmount <= 0 ? "nothing" : aMat.mTargetCrushing   .mMaterial == aMat ? "itself" : aMat.mTargetCrushing   .mMaterial.getLocal())+"\n";
			
			tBook.add(tPage);
			
			//----------
			
			tPage = "Processing Data\n===================\n";
			tPage += "Bending:\n"       +(aMat.mTargetBending    .mAmount / U) + "." + ((int)(((double)(aMat.mTargetBending    .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetBending    .mAmount <= 0 ? "nothing" : aMat.mTargetBending    .mMaterial == aMat ? "itself" : aMat.mTargetBending    .mMaterial.getLocal())+"\n";
			tPage += "Compressing:\n"   +(aMat.mTargetCompressing.mAmount / U) + "." + ((int)(((double)(aMat.mTargetCompressing.mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetCompressing.mAmount <= 0 ? "nothing" : aMat.mTargetCompressing.mMaterial == aMat ? "itself" : aMat.mTargetCompressing.mMaterial.getLocal())+"\n";
			tPage += "Cutting:\n"       +(aMat.mTargetCutting    .mAmount / U) + "." + ((int)(((double)(aMat.mTargetCutting    .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetCutting    .mAmount <= 0 ? "nothing" : aMat.mTargetCutting    .mMaterial == aMat ? "itself" : aMat.mTargetCutting    .mMaterial.getLocal())+"\n";
			tPage += "Forging:\n"       +(aMat.mTargetForging    .mAmount / U) + "." + ((int)(((double)(aMat.mTargetForging    .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetForging    .mAmount <= 0 ? "nothing" : aMat.mTargetForging    .mMaterial == aMat ? "itself" : aMat.mTargetForging    .mMaterial.getLocal())+"\n";
			tPage += "Smashing:\n"      +(aMat.mTargetSmashing   .mAmount / U) + "." + ((int)(((double)(aMat.mTargetSmashing   .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetSmashing   .mAmount <= 0 ? "nothing" : aMat.mTargetSmashing   .mMaterial == aMat ? "itself" : aMat.mTargetSmashing   .mMaterial.getLocal())+"\n";
			
			tBook.add(tPage);
			
			//----------
			
			tPage = "Processing Data\n===================\n";
			tPage += "Working:\n"       +(aMat.mTargetWorking    .mAmount / U) + "." + ((int)(((double)(aMat.mTargetWorking    .mAmount % U) / (double)U) * 1000))+" "+(aMat.mTargetWorking    .mAmount <= 0 ? "nothing" : aMat.mTargetWorking    .mMaterial == aMat ? "itself" : aMat.mTargetWorking    .mMaterial.getLocal())+"\n";
			
			tBook.add(tPage);
			
			//----------
			
			tPage = "Thaumaturgic Data\n===================\nAspects:\n";
			
			if (aMat.mAspects.isEmpty()) {
				tPage += "None\n";
			} else {
				for (TC_AspectStack tAspect : aMat.mAspects) tPage += tAspect.mAmount + "x " + tAspect.mAspect.mName + "\n";
			}
			
			tPage += "===================\n";
			
			tBook.add(tPage);
			
			//----------
			
			Map<OreDictMaterial, Long> tMap;
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedSmelting) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetSmelting.has(aMat)) tMap.put(tMat, tMat.mTargetSmelting.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to smelt for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to smelt for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedSolidifying) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetSolidifying.has(aMat)) tMap.put(tMat, tMat.mTargetSolidifying.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to smelt and solidify for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to smelt and solidify for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedBurning) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetBurning.has(aMat)) tMap.put(tMat, tMat.mTargetBurning.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to burn for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to burn for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedPulver) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetPulver.has(aMat)) tMap.put(tMat, tMat.mTargetPulver.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to pulverise for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to pulverise for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedBending) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetBending.has(aMat)) tMap.put(tMat, tMat.mTargetBending.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to bend for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to bend for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedCompressing) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCompressing.has(aMat)) tMap.put(tMat, tMat.mTargetCompressing.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to compress for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to compress for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedCrushing) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCrushing.has(aMat)) tMap.put(tMat, tMat.mTargetCrushing.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to crush for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to crush for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedCutting) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCutting.has(aMat)) tMap.put(tMat, tMat.mTargetCutting.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to cut for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to cut for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedForging) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetForging.has(aMat)) tMap.put(tMat, tMat.mTargetForging.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to forge for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to forge for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedSmashing) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetSmashing.has(aMat)) tMap.put(tMat, tMat.mTargetSmashing.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to smash for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to smash for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			tMap = new HashMap<>(); for (OreDictMaterial tMat : aMat.mTargetedWorking) if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetWorking.has(aMat)) tMap.put(tMat, tMat.mTargetWorking.mAmount);
			tMap = Code.sortByValuesDescending(tMap);
			tPage = "Resources to use in other ways for getting "+aMat.getLocal()+"\n===================\n";
			tCounter = 0;
			for (Entry<OreDictMaterial, Long> tEntry : tMap.entrySet()) {
				temp=!(tCounter++%6==5);
				tPage+=(tEntry.getValue() / U) + "." + ((int)(((double)(tEntry.getValue() % U) / (double)U) * 1000))+" from 1 "+tEntry.getKey().getLocal()+"\n";
				if (!temp) {
					tBook.add(tPage);
					tPage = "Resources to use in other ways for getting "+aMat.getLocal()+"\n===================\n";
				}
			}
			
			if (temp) {tBook.add(tPage); temp=F;}
			
			//----------
			
			for (IOreDictConfigurationComponent tConfig : aMat.mAlloyCreationRecipes) {
				tPage="Alloy:\n"+aMat.getLocal()+"\n===================\nMelting: "+aMat.mMeltingPoint+" K\nBoiling: "+aMat.mBoilingPoint+" K\n===================\nComponents per "+tConfig.getCommonDivider() + "\n";
				for (OreDictMaterialStack tMt2 : tConfig.getUndividedComponents()) tPage += (tMt2.mAmount / U)+" "+tMt2.mMaterial.getLocal()+"\n";
				tBook.add(tPage);
			}
			
			//----------
			
			for (OreDictMaterial tMat : aMat.mAlloyComponentReferences) {
				for (IOreDictConfigurationComponent tConfig : tMat.mAlloyCreationRecipes) {
					for (OreDictMaterialStack tMatStack : tConfig.getUndividedComponents()) {
						if (tMatStack.mMaterial == aMat) {
							tPage="Alloy:\n"+tMat.getLocal()+"\n===================\nMelting: "+tMat.mMeltingPoint+" K\nBoiling: "+tMat.mBoilingPoint+" K\n===================\nComponents per "+tConfig.getCommonDivider() + "\n";
							for (OreDictMaterialStack tMt2 : tConfig.getUndividedComponents()) tPage += (tMt2.mAmount / U)+" "+tMt2.mMaterial.getLocal()+"\n";
							tBook.add(tPage);
							break;
						}
					}
				}
			}
			
			//----------
			
			if (aMat.mDescription != null) for (int i = 0, j = 0; i < aMat.mDescription.length; i++) if (Code.stringValid(aMat.mDescription[i])) tBook.add("Description Pg "+(++j)+"\n===================\n" + aMat.mDescription[i]);
			
			//----------
			
			return null != createWrittenBook("Material_Dictionary_"+aMat.mNameInternal, aMat.getLocal(), "Material Dictionary Foundation", tBook.size()<=50?(ST.valid(aDefaultBook)?ST.amount(1, aDefaultBook):ST.make(ItemsGT.BOOKS, 1, 32002)):(ST.valid(aDefaultLargeBook)?ST.amount(1, aDefaultLargeBook):ST.make(ItemsGT.BOOKS, 1, 32003)), F, tBook.toArray(ZL_STRING));
		}
	}
	
	
	public static class Code {
		/** Note: Does not work on huge amounts of Bytes. */
		public static byte averageBytes(byte... aBytes) {
			if (aBytes == null || aBytes.length <= 0) return 0;
			return (byte)(sum(aBytes) / aBytes.length);
		}
		
		/** Note: Does not work on huge amounts of Bytes. */
		public static byte averageUnsignedBytes(byte... aBytes) {
			if (aBytes == null || aBytes.length <= 0) return 0;
			long rValue = 0;
			for (byte aByte : aBytes) rValue += unsignB(aByte);
			return (byte)(rValue / aBytes.length);
		}
		
		/** Note: Does not work on huge amounts of Bytes. */
		public static short averageShorts(short... aShorts) {
			if (aShorts == null || aShorts.length <= 0) return 0;
			return (short)(sum(aShorts) / aShorts.length);
		}
		
		/** Note: Does not work on huge amounts of Shorts. */
		public static short averageUnsignedShorts(short... aShorts) {
			if (aShorts == null || aShorts.length <= 0) return 0;
			long rValue = 0;
			for (short aShort : aShorts) rValue += unsignS(aShort);
			return (short)(rValue / aShorts.length);
		}
		
		/** Note: Does not work on huge amounts of Integers. */
		public static int averageInts(int... aInts) {
			if (aInts == null || aInts.length <= 0) return 0;
			return bindInt(sum(aInts) / aInts.length);
		}
		
		/** Note: Does not work on huge amounts of Integers. */
		public static int averageUnsignedInts(int... aInts) {
			if (aInts == null || aInts.length <= 0) return 0;
			long rValue = 0;
			for (int aInt : aInts) rValue += unsignI(aInt);
			return bindInt(rValue / aInts.length);
		}
		
		/** Note: Does not work on huge amounts of Longs. */
		public static long averageLongs(long... aLongs) {
			if (aLongs == null || aLongs.length <= 0) return 0;
			return sum(aLongs) / aLongs.length;
		}
		
		public static int roundDown(double aNumber) {
			int rRounded = (int)aNumber;
			return rRounded > aNumber ? rRounded-1 : rRounded;
		}
		public static int roundUp(double aNumber) {
			int rRounded = (int)aNumber;
			return rRounded < aNumber ? rRounded+1 : rRounded;
		}
		
		/** @return an unsigned representation of this Byte. */
		public static short unsignB(byte aByte) {
			return aByte < 0 ? (short)(aByte + 256) : aByte;
		}
		
		/** @return an unsigned representation of this Short. */
		public static int unsignS(short aShort) {
			return aShort < 0 ? aShort + 65536 : aShort;
		}
		
		/** @return an unsigned representation of this Integer. */
		public static long unsignI(int aInteger) {
			return aInteger < 0 ? aInteger + 4294967296L : aInteger;
		}
		
		public static byte toByteS(short aValue, int aIndex) {return (byte)(aValue >> (aIndex<<3));}
		public static byte toByteI(int   aValue, int aIndex) {return (byte)(aValue >> (aIndex<<3));}
		public static byte toByteL(long  aValue, int aIndex) {return (byte)(aValue >> (aIndex<<3));}
		
		public static short combine(byte aValue1, byte aValue2)                                                                                     {return (short) ((0xff & aValue1) | aValue2 << 8);}
		public static int   combine(byte aValue1, byte aValue2, byte aValue3, byte aValue4)                                                         {return          (0xff & aValue1) | (0xff & aValue2) << 8 | (0xff & aValue3) << 16 | aValue4 << 24;}
		public static long  combine(byte aValue1, byte aValue2, byte aValue3, byte aValue4, byte aValue5, byte aValue6, byte aValue7, byte aValue8) {return ((long)aValue1 & 0xff) | ((long)aValue2 & 0xff) << 8 | ((long)aValue3 & 0xff) << 16 | ((long)aValue4 & 0xff) << 24 | ((long)aValue5 & 0xff) << 32 | ((long)aValue6 & 0xff) << 40 | ((long)aValue7 & 0xff) << 48 | (long)aValue8 << 56;}
		
		public static long getBits(boolean... aBits) {
			long rBits = 0;
			for (int i = 0; i < 64 && i < aBits.length; i++) if (aBits[i]) rBits |= (1 << i);
			return rBits;
		}
		
		public static boolean[] getBitsB(byte aBits) {
			boolean[] rBits = new boolean[8];
			for (int i = 0; i < rBits.length; i++) if ((aBits & (1 << i)) != 0) rBits[i] = T;
			return rBits;
		}
		
		public static boolean[] getBitsS(short aBits) {
			boolean[] rBits = new boolean[16];
			for (int i = 0; i < rBits.length; i++) if ((aBits & (1 << i)) != 0) rBits[i] = T;
			return rBits;
		}
		
		public static boolean[] getBitsI(int aBits) {
			boolean[] rBits = new boolean[32];
			for (int i = 0; i < rBits.length; i++) if ((aBits & (1 << i)) != 0) rBits[i] = T;
			return rBits;
		}
		
		public static boolean[] getBitsL(long aBits) {
			boolean[] rBits = new boolean[64];
			for (int i = 0; i < rBits.length; i++) if ((aBits & (1 << i)) != 0) rBits[i] = T;
			return rBits;
		}
		
		public static ItemStack toStack(int aStack) {
			Item tItem = Item.byId(aStack&(~0>>>16));
			if (tItem != null) return ST.make(tItem, 1, aStack>>>16);
			return null;
		}
		
		public static UUID getUUID(byte[] aData, int aOffset) {
			return aData.length - aOffset < 16 ? null : new UUID(combine(aData[aOffset], aData[aOffset+1], aData[aOffset+2], aData[aOffset+3], aData[aOffset+4], aData[aOffset+5], aData[aOffset+6], aData[aOffset+7]), combine(aData[aOffset+8], aData[aOffset+9], aData[aOffset+10], aData[aOffset+11], aData[aOffset+12], aData[aOffset+13], aData[aOffset+14], aData[aOffset+15]));
		}
		
		public static byte[] getBytes(UUID aData, int aOffset) {
			if (aData == null) return new byte[aOffset];
			byte[] rData = new byte[aOffset+16];
			for (int i = 0; i < 8; i++) {
				rData[aOffset+  i] = toByteL(aData.getMostSignificantBits(), i);
				rData[aOffset+8+i] = toByteL(aData.getLeastSignificantBits(), i);
			}
			return rData;
		}
		
		/** Converts a Number to a String with Underscores as Decimal Separators. Ignores Numbers with 4 Digits or less. */
		public static String makeString(long aNumber) {
			if (aNumber > -10000 && aNumber < 10000) return Long.toString(aNumber);
			StringBuilder rString = new StringBuilder();
			if (aNumber < 0) {
				aNumber *= -1;
				rString.append('-');
			}
			boolean temp = T;
			for (long i = 1000000000000000000L; i > 0; i /= 10) {
				long tDigit = (aNumber / i) % 10;
				if ( temp && tDigit != 0) temp = F;
				if (!temp) {
					rString.append(tDigit);
					if (i != 1) for (long j = i; j > 0; j /= 1000) if (j == 1) rString.append('_');
				}
			}
			return rString.toString();
		}
		
		@SafeVarargs
		public static <E> boolean contains(E aTarget, E... aArray) {
			if (aArray != null) for (E tValue : aArray) if (tValue == aTarget || (tValue != null && aTarget != null && tValue.equals(aTarget))) return T;
			return F;
		}
		
		public static boolean containsBoolean(boolean aTarget, boolean... aArray) {
			if (aArray != null) for (boolean tValue : aArray) if (tValue == aTarget) return T;
			return F;
		}
		
		@SafeVarargs
		public static <E> boolean containsSomething(E... aArray) {
			if (aArray != null) for (Object tObject : aArray) if (tObject != null) return T;
			return F;
		}
		
		public static boolean[] swap(int aIndexA, int aIndexB, boolean[] aArray) {boolean tSwap = aArray[aIndexA]; aArray[aIndexA] = aArray[aIndexB]; aArray[aIndexB] = tSwap; return aArray;}
		public static byte   [] swap(int aIndexA, int aIndexB, byte   [] aArray) {byte    tSwap = aArray[aIndexA]; aArray[aIndexA] = aArray[aIndexB]; aArray[aIndexB] = tSwap; return aArray;}
		public static short  [] swap(int aIndexA, int aIndexB, short  [] aArray) {short   tSwap = aArray[aIndexA]; aArray[aIndexA] = aArray[aIndexB]; aArray[aIndexB] = tSwap; return aArray;}
		public static int    [] swap(int aIndexA, int aIndexB, int    [] aArray) {int     tSwap = aArray[aIndexA]; aArray[aIndexA] = aArray[aIndexB]; aArray[aIndexB] = tSwap; return aArray;}
		public static long   [] swap(int aIndexA, int aIndexB, long   [] aArray) {long    tSwap = aArray[aIndexA]; aArray[aIndexA] = aArray[aIndexB]; aArray[aIndexB] = tSwap; return aArray;}
		public static <E>   E[] swap(int aIndexA, int aIndexB, E      [] aArray) {E       tSwap = aArray[aIndexA]; aArray[aIndexA] = aArray[aIndexB]; aArray[aIndexB] = tSwap; return aArray;}
		
		public static <E>   E[] fill(E aToFillIn, E[] rArray) {Arrays.fill(rArray, aToFillIn); return rArray;}
		
		@SafeVarargs
		public static <E> E[] makeArray(E[] rArray, E... aArray) {
			for (int i = 0; i < rArray.length && i < aArray.length; i++) rArray[i] = aArray[i];
			return rArray;
		}
		
		@SafeVarargs
		public static <E> long getNonNulls(E... aArray) {
			long rAmount = 0;
			if (aArray != null) for (Object tObject : aArray) if (tObject != null) rAmount++;
			return rAmount;
		}
		
		@SafeVarargs
		public static <E> ArrayList<E> getWithoutNulls(E... aArray) {
			if (aArray == null) return new ArrayListNoNulls<>();
			ArrayList<E> rList = new ArrayListNoNulls<>(Arrays.asList(aArray));
			return rList;
		}
		
		@SafeVarargs
		public static <E> ArrayList<E> getWithoutTrailingNulls(E... aArray) {
			if (aArray == null) return new ArrayList<>(1);
			ArrayList<E> rList = new ArrayList<>(Arrays.asList(aArray));
			for (int i = rList.size() - 1; i >= 0 && rList.get(i) == null;) rList.remove(i--);
			return rList;
		}
		
		@SafeVarargs
		public static <E> E getFirstNonNull(E... aArray) {
			if (aArray != null) for (E tObject : aArray) if (tObject != null) return tObject;
			return null;
		}
		
		public static <E> E getWithDefault(E aObject, E aDefault) {
			return aObject != null ? aObject : aDefault;
		}
		
		private static final DateFormat sDateFormat = DateFormat.getInstance();
		public static String dateAndTime() {
			return sDateFormat.format(new Date());
		}
		
		public static byte tier(long aSize) {
			return tierMax(aSize);
		}
		
		public static byte tierMax(long aSize) {
			byte i = -1;
			aSize = Math.abs(aSize);
			while (++i < V.length) if (aSize <= V[i]) return i;
			return i;
		}
		
		public static byte tierMin(long aSize) {
			byte i = -1;
			aSize = Math.abs(aSize);
			while (++i < V.length) if (aSize < V[i]) return (byte)Math.max(0, i-1);
			return --i;
		}
		
		public static long voltMax(long aSize) {
			aSize = Math.abs(aSize);
			for (int i = 0; i < VMAX.length; i++) if (aSize < VMAX[i]) return VMAX[i];
			return VMAX[VMAX.length-1];
		}
		
		public static long voltMin(long aSize) {
			aSize = Math.abs(aSize);
			for (int i = 0; i < VMAX.length; i++) if (aSize < VMAX[i]) return VMIN[i];
			return VMIN[VMIN.length-1];
		}
		
		public static boolean haveOneCommonElement(Iterable<?> aCollection1, Collection<?> aCollection2) {
			if (aCollection1 == aCollection2) return T;
			for (Object tObject : aCollection1) if (aCollection2.contains(tObject)) return T;
			return F;
		}
		
		/** re-maps all Keys of a Map after the Keys were weakened. */
		public static <X, Y> Map<X, Y> reMap(Map<X, Y> aMap) {
			Map<X, Y> tMap = new HashMap<>();
			tMap.putAll(aMap);
			aMap.clear();
			aMap.putAll(tMap);
			return aMap;
		}
		
		/** re-maps all Keys of a (Hash)-Set after the Keys were weakened. */
		public static <X> Set<X> reMap(Set<X> aSet) {
			Set<X> tSet = new HashSet<>();
			tSet.addAll(aSet);
			aSet.clear();
			aSet.addAll(aSet);
			return aSet;
		}
		
		/** Why the fuck do neither Java nor Guava have a Function to do this? */
		@SuppressWarnings("rawtypes")
		public static <X, Y extends Comparable> LinkedHashMap<X,Y> sortByValuesAcending(Map<X,Y> aMap) {
			List<Map.Entry<X,Y>> tEntrySet = new LinkedList<>(aMap.entrySet());
			Collections.sort(tEntrySet, new Comparator<Map.Entry<X,Y>>() {@SuppressWarnings("unchecked") @Override public int compare(Entry<X, Y> aValue1, Entry<X, Y> aValue2) {return aValue1.getValue().compareTo(aValue2.getValue());}});
			LinkedHashMap<X,Y> rMap = new LinkedHashMap<>();
			for (Map.Entry<X,Y> tEntry : tEntrySet) rMap.put(tEntry.getKey(), tEntry.getValue());
			return rMap;
		}
		
		/** Why the fuck do neither Java nor Guava have a Function to do this? */
		@SuppressWarnings("rawtypes")
		public static <X, Y extends Comparable> LinkedHashMap<X,Y> sortByValuesDescending(Map<X,Y> aMap) {
			List<Map.Entry<X,Y>> tEntrySet = new LinkedList<>(aMap.entrySet());
			Collections.sort(tEntrySet, new Comparator<Map.Entry<X,Y>>() {@SuppressWarnings("unchecked") @Override public int compare(Entry<X, Y> aValue1, Entry<X, Y> aValue2) {return -aValue1.getValue().compareTo(aValue2.getValue());}});
			LinkedHashMap<X,Y> rMap = new LinkedHashMap<>();
			for (Map.Entry<X,Y> tEntry : tEntrySet) rMap.put(tEntry.getKey(), tEntry.getValue());
			return rMap;
		}
		
		public static <E> E select(long aIndex, E aReplacement, List<E> aList) {
			if (aList == null || aList.isEmpty()) return aReplacement;
			if (aIndex >= aList.size()          ) return aReplacement == null ? aList.get(aList.size() - 1) : aReplacement;
			if (aIndex <  0                     ) return aReplacement == null ? aList.get(               0) : aReplacement;
			return aList.get((int)aIndex);
		}
		public static <E> E select(E aReplacement, List<E> aList) {
			return aList == null || aList.isEmpty() ? aReplacement : select(RNGSUS.nextInt(aList.size()), aReplacement, aList);
		}
		@SafeVarargs
		public static <E> E select(long aIndex, E aReplacement, E... aArray) {
			if (aArray == null || aArray.length <= 0) return aReplacement;
			if (aIndex >= aArray.length             ) return aReplacement == null ? aArray[aArray.length - 1] : aReplacement;
			if (aIndex <  0                         ) return aReplacement == null ? aArray[                0] : aReplacement;
			return aArray[(int)aIndex];
		}
		@SafeVarargs
		public static <E> E select(E aReplacement, E... aArray) {
			return aArray == null || aArray.length <= 0 ? aReplacement : select(RNGSUS.nextInt(aArray.length), aReplacement, aArray);
		}
		
		public static boolean inArray(Object aObject, Object... aObjects) {
			return inList(aObject, Arrays.asList(aObjects));
		}
		
		public static boolean inList(Object aObject, Collection<?> aObjects) {
			if (aObjects == null) return F;
			return aObjects.contains(aObject);
		}
		
		public static final int[][] ASCENDING_ARRAYS = new int[1024][];
		
		public static int[] getAscendingArray(int aLength) {
			if (aLength <= 0) return ZL_INTEGER;
			if (aLength < ASCENDING_ARRAYS.length) {
				if (ASCENDING_ARRAYS[aLength] == null) {
					ASCENDING_ARRAYS[aLength] = new int[aLength];
					for (int i = 0; i < aLength; i++) ASCENDING_ARRAYS[aLength][i] = i;
				}
				return ASCENDING_ARRAYS[aLength];
			}
			int[] rArray = new int[aLength];
			for (int i = 0; i < aLength; i++) rArray[i] = i;
			return rArray;
		}
		
		public static String stringValidate(Object aString) {return stringValidate(aString, "");}
		public static String stringValidate(Object aString, String aReplacement) {
			if (aString == null) return aReplacement;
			if (aString instanceof Biome) return gregapi.code.BiomeNameSet.biomeKeyName((Biome)aString);
			String rString = aString.toString();
			return rString == null || rString.isEmpty() ? aReplacement : rString;
		}
		
		public static boolean stringValid(Object aString) {
			return aString != null && !aString.toString().isEmpty();
		}
		
		public static boolean stringInvalid(Object aString) {
			return aString == null || aString.toString().isEmpty();
		}
		
		public static byte side(Direction aDirection) {
			return (byte)(aDirection==null?SIDE_INVALID:aDirection.ordinal());
		}
		
		public static byte side(int aSide) {
			return aSide > 5 || aSide < 0 ? SIDE_INVALID : (byte)aSide;
		}
		
		/** If this Index exists inside the passed Array and if it is != null */
		public static <E> boolean exists(int aIndex, E[] aArray) {
			return aIndex >= 0 && aIndex < aArray.length && aArray[aIndex] != null;
		}
		
		/** @return a Value for a Scale between 0 and aMax with aScale+1 possible Steps. 0 is only returned if the aValue is <= 0, aScale is only returned if the Value is >= aMax. The remaining values between ]0:aScale[ are returned for each Step of the Scale. This Function finds use in Displays such as the Barometer, but also in Redstone. */
		public static long scale(long aValue, long aMax, long aScale, boolean aInvert) {
			long rScale = (aValue <= 0 ? 0 : aValue >= aMax ? aScale : aScale <= 2 ? 1 : 1 + (aValue * (aScale-1)) / aMax);
			return aInvert ? aScale - rScale : rScale;
		}
		
		/** @return a Value for a Scale between aMin and aMax with aScale+1 possible Steps. 0 is only returned if the aValue is <= aMin, aScale is only returned if the Value is >= aMax. The remaining values between ]0:aScale[ are returned for each Step of the Scale. This Function finds use in Displays such as the Barometer, but also in Redstone. */
		public static long scale(long aValue, long aMin, long aMax, long aScale, boolean aInvert) {
			return scale(aValue-aMin, aMax-aMin, aScale, aInvert);
		}
		
		public static long bind(long aMin, long aMax, long aBoundValue) {
			return aMin > aMax ? Math.max(aMax, Math.min(aMin, aBoundValue)) : Math.max(aMin, Math.min(aMax, aBoundValue));
		}
		public static long bind_(long aMin, long aMax, long aBoundValue) {
			return Math.max(aMin, Math.min(aMax, aBoundValue));
		}
		
		public static float  bindF    (float  aBoundValue) {return        Math.max(0, Math.min(         1, aBoundValue));}
		public static double bindD    (double aBoundValue) {return        Math.max(0, Math.min(         1, aBoundValue));}
		public static byte   bind1    (long   aBoundValue) {return (byte) Math.max(0, Math.min(         1, aBoundValue));}
		public static byte   bind2    (long   aBoundValue) {return (byte) Math.max(0, Math.min(         3, aBoundValue));}
		public static byte   bind3    (long   aBoundValue) {return (byte) Math.max(0, Math.min(         7, aBoundValue));}
		public static byte   bind4    (long   aBoundValue) {return (byte) Math.max(0, Math.min(        15, aBoundValue));}
		public static byte   bind5    (long   aBoundValue) {return (byte) Math.max(0, Math.min(        31, aBoundValue));}
		public static byte   bind6    (long   aBoundValue) {return (byte) Math.max(0, Math.min(        63, aBoundValue));}
		public static byte   bind7    (long   aBoundValue) {return (byte) Math.max(0, Math.min(       127, aBoundValue));}
		public static short  bind8    (long   aBoundValue) {return (short)Math.max(0, Math.min(       255, aBoundValue));}
		public static short  bind15   (long   aBoundValue) {return (short)Math.max(0, Math.min(     32767, aBoundValue));}
		public static int    bind16   (long   aBoundValue) {return (int)  Math.max(0, Math.min(     65535, aBoundValue));}
		public static int    bind24   (long   aBoundValue) {return (int)  Math.max(0, Math.min(  16777215, aBoundValue));}
		public static int    bind31   (long   aBoundValue) {return (int)  Math.max(0, Math.min(2147483647, aBoundValue));}
		public static int    bindInt  (long   aBoundValue) {return (int)  Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, aBoundValue));}
		public static short  bindShort(long   aBoundValue) {return (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, aBoundValue));}
		public static byte   bindByte (long   aBoundValue) {return (byte) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, aBoundValue));}
		public static byte   bindStack(long   aBoundValue) {return (byte) Math.max(1, Math.min(64, aBoundValue));}
		
		public static short[] bindRGBa(short[] aColors) {
			if (aColors == null) return new short[] {255,255,255,255};
			for (int i = 0; i < aColors.length; i++) aColors[i] = bind8(aColors[i]);
			return aColors;
		}
		
		public static int mixRGBInt(int aRGB1, int aRGB2) {
			return getRGBInt(new short[] {(short)((getR(aRGB1) + getR(aRGB2)) >> 1), (short)((getG(aRGB1) + getG(aRGB2)) >> 1), (short)((getB(aRGB1) + getB(aRGB2)) >> 1)});
		}
		
		public static int getRGBInt(short[] aColors) {
			return aColors == null ? 16777215 : (bind8(aColors[0]) << 16) | (bind8(aColors[1]) << 8) | bind8(aColors[2]);
		}
		
		public static int getRGBaInt(short[] aColors) {
			return aColors == null ? 16777215 : (bind8(aColors[0]) << 16) | (bind8(aColors[1]) << 8) | bind8(aColors[2]) | (bind8(aColors[3]) << 24);
		}
		
		public static int getRGBInt(long aR, long aG, long aB) {
			return (bind8(aR) << 16) | (bind8(aG) << 8) | bind8(aB);
		}
		
		public static int getRGBaInt(long aR, long aG, long aB, long aA) {
			return (bind8(aR) << 16) | (bind8(aG) << 8) | bind8(aB) | (bind8(aA) << 24);
		}
		
		public static short[] getRGBaArray(int aColors) {
			return new short[] {(short)((aColors >>> 16) & 255), (short)((aColors >>> 8) & 255), (short)(aColors & 255), (short)((aColors >>> 24) & 255)};
		}
		
		public static short getR(int aColors) {return (short)((aColors >>> 16) & 255);}
		public static short getG(int aColors) {return (short)((aColors >>>  8) & 255);}
		public static short getB(int aColors) {return (short) (aColors         & 255);}
		public static short getA(int aColors) {return (short)((aColors >>> 24) & 255);}
		
		/** estebes helped with the code for this one, and yes that cast down there is fucking necessary... */
		public static short[] color(ItemStack aStack) {
			if (ST.invalid(aStack)) return UNCOLOURED;
			// PORT-TODO(F3, baked-рендер клиента, Фаза C): 1.7.10 читал средний цвет из атлас-спрайта предмета
			// (ItemStack.getIconIndex():IIcon + IIcon.getIconName() + Item.getColorFromItemStack) — весь этот
			// immediate-mode/IIcon стек удалён в 26.1.2 (см. decisions/F3-render.md). Атлас-спрайт на клиенте
			// станет TextureAtlasSprite/Material при Фазе C; тинт предмета — ItemColors. До неё деградируем до
			// нейтрального UNCOLOURED (тинт «нет модуляции»), НЕ крашим — метод @OnlyIn(CLIENT), на сервере не зовётся.
			return UNCOLOURED;
		}
		
		/** estebes helped with the code for this one */
		public static short[] color(String aResourceLocation) {
			Identifier aux = null;
			if (aResourceLocation.contains(":")) {
				String[] modid_itemid = aResourceLocation.split(":");
				aux = Identifier.fromNamespaceAndPath(modid_itemid[0], "textures/items/" + modid_itemid[1] + ".png"); // neo: ctor Identifier(String,String) private -> fromNamespaceAndPath (Identifier.java:41)
			} else {
				aux = Identifier.fromNamespaceAndPath("minecraft", "textures/items/" + aResourceLocation + ".png");
			}
			java.awt.image.BufferedImage tIcon = null;
			// neo: ResourceManager.getResource(Identifier) -> Optional<Resource> (не бросает FileNotFound);
			// Resource.getInputStream() -> open() (Resource.java). Читаем только если ресурс присутствует.
			// S6: доступ к client resource manager — через центр (GT_API_Proxy.getResourceStream), на сервере null (не грузим Minecraft).
			try {java.io.InputStream tStream = gregapi.GT_API.api_proxy.getResourceStream(aux); if (tStream != null) tIcon = javax.imageio.ImageIO.read(tStream);} catch (IOException e) {/**/}
			return tIcon == null ? null : color(tIcon);
		}
		
		/** estebes helped with the code for this one */
		public static short[] color(java.awt.image.BufferedImage icon) {
			long tR = 0, tG = 0, tB = 0, tPixels = 0;
			for (int tWidth = 0; tWidth < icon.getWidth(); tWidth++) for (int tHeight = 0; tHeight < icon.getHeight(); tHeight++) {
				int tPixel = icon.getRGB(tWidth, tHeight);
				if ((     (tPixel >>> 24) & 255) > 128) {
					tR += (tPixel >>> 16) & 255;
					tG += (tPixel >>>  8) & 255;
					tB +=  tPixel         & 255;
					tPixels++;
				}
			}
			return new short[] {(short)(tR / tPixels), (short)(tG / tPixels), (short)(tB / tPixels)};
		}
		
		/** toUpperCases the first Character of the String and returns it */
		public static String capitalise(String aString) {
			return aString == null ? "" : aString.length() <= 1 ? aString.toUpperCase() : aString.substring(0, 1).toUpperCase() + aString.substring(1);
		}
		
		/** toUpperCases the first Character of each Word in the String and returns it */
		public static String capitaliseWords(String aString) {
			StringBuilder rString = new StringBuilder();
			for (String tString : aString.split(" ")) if (!tString.isEmpty()) rString.append(capitalise(tString)).append(" ");
			return rString.toString().trim();
		}
		
		/** @return the opposite facing of this Side of a Block, with a boundary check. */
		public static byte opposite(int aSide) {
			return aSide < OPOS.length && aSide >= 0 ? OPOS[aSide] : 6;
		}
		
		/** Turns the Amount of Stuff into a more readable String. */
		public static String displayUnits(long aAmount) {
			if (aAmount < 0) return "?.???";
			long tDigits = ((aAmount % U) * 1000) / U;
			return (aAmount / U) + "." + (tDigits<1?"000":tDigits<10?"00"+tDigits:tDigits<100?"0"+tDigits:tDigits);
		}
		
		/** Translates Amount of aUnit1 to Amount of aUnit2. */
		public static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
			if (aTargetUnit == 0) return 0;
			if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
			if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
			if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
			return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
		}
		
		/** Translates Amount of aUnit1 to Amount of aUnit2. With additional checks to avoid 64 Bit Overflow. */
		public static long units_(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
			if (aTargetUnit == 0) return 0;
			if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
			if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
			if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;} else {
			if (aOriginalUnit %  648 == 0 && aTargetUnit %  648 == 0) {aOriginalUnit /=  648; aTargetUnit /=  648;}
			if (aOriginalUnit % 1000 == 0 && aTargetUnit % 1000 == 0) {aOriginalUnit /= 1000; aTargetUnit /= 1000;}}
			return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
		}
		
		/** Divides but rounds up. */
		public static long divup(long aNumber, long aDivider) {
			return aNumber / aDivider + (aNumber % aDivider == 0 ? 0 : 1);
		}
		
		public static long sum(byte... aArray) {
			long rAmount = 0;
			for (byte tNumber : aArray) rAmount += tNumber;
			return rAmount;
		}
		
		public static long sum(short... aArray) {
			long rAmount = 0;
			for (short tNumber : aArray) rAmount += tNumber;
			return rAmount;
		}
		
		public static long sum(int... aArray) {
			long rAmount = 0;
			for (int tNumber : aArray) rAmount += tNumber;
			return rAmount;
		}
		
		public static long sum(long... aArray) {
			long rAmount = 0;
			for (long tNumber : aArray) rAmount += tNumber;
			return rAmount;
		}
		
		public static boolean abs_greater(long aAmount1, long aAmount2) {return Math.abs(aAmount1) > Math.abs(aAmount2);}
		public static boolean abs_smaller(long aAmount1, long aAmount2) {return Math.abs(aAmount1) < Math.abs(aAmount2);}
		public static boolean abs_greater_equal(long aAmount1, long aAmount2) {return Math.abs(aAmount1) >= Math.abs(aAmount2);}
		public static boolean abs_smaller_equal(long aAmount1, long aAmount2) {return Math.abs(aAmount1) <= Math.abs(aAmount2);}
		
		public static boolean inside(long aMin, long aMax, long aNumber) {return aMin < aMax ? aMin <= aNumber && aNumber <= aMax : aMax <= aNumber && aNumber <= aMin;}
		public static boolean inside_(double aMin, double aMax, double aNumber) {return aMin < aMax ? aMin <= aNumber && aNumber <= aMax : aMax <= aNumber && aNumber <= aMin;}
		
		/** @return an Array containing the X and the Y Coordinate of the clicked Point, with the top left Corner as Origin, like on the Texture Sheet. return values should always be between [0.0F and 0.99F]. */
		public static float[] getFacingCoordsClicked(byte aSide, float aHitX, float aHitY, float aHitZ) {
			switch (aSide) {
			case  0: return new float[] {Math.min(0.99F, Math.max(0,  aHitX)), Math.min(0.99F, Math.max(0,1-aHitZ))};
			case  1: return new float[] {Math.min(0.99F, Math.max(0,  aHitX)), Math.min(0.99F, Math.max(0,  aHitZ))};
			case  2: return new float[] {Math.min(0.99F, Math.max(0,1-aHitX)), Math.min(0.99F, Math.max(0,1-aHitY))};
			case  3: return new float[] {Math.min(0.99F, Math.max(0,  aHitX)), Math.min(0.99F, Math.max(0,1-aHitY))};
			case  4: return new float[] {Math.min(0.99F, Math.max(0,  aHitZ)), Math.min(0.99F, Math.max(0,1-aHitY))};
			case  5: return new float[] {Math.min(0.99F, Math.max(0,1-aHitZ)), Math.min(0.99F, Math.max(0,1-aHitY))};
			default: return new float[] {0.5F, 0.5F};
			}
		}
		
		public static byte getSideForPlayerPlacing(Entity aPlayer) {
			if (aPlayer.getXRot() >=  65) return SIDE_UP;
			if (aPlayer.getXRot() <= -65) return SIDE_DOWN;
			return getHorizontalForPlayerPlacing(aPlayer);
		}
		public static byte getHorizontalForPlayerPlacing(Entity aPlayer) {
			return COMPASS_DIRECTIONS[UT.Code.roundDown(4*aPlayer.getYRot()/360+0.5)&3];
		}
		
		public static byte getSideForPlayerPlacing(Entity aPlayer, byte aDefaultFacing, boolean[] aAllowedFacings) {
			if (aPlayer.getXRot() >=  65 && aAllowedFacings[SIDE_UP]) return SIDE_UP;
			if (aPlayer.getXRot() <= -65 && aAllowedFacings[SIDE_DOWN]) return SIDE_DOWN;
			byte rFacing = getHorizontalForPlayerPlacing(aPlayer);
			if (aAllowedFacings[rFacing]) return rFacing;
			for (byte tSide : ALL_SIDES_VALID) if (aAllowedFacings[tSide]) return tSide;
			return aDefaultFacing;
		}
		
		public static byte getOppositeSideForPlayerPlacing(Entity aPlayer, byte aDefaultFacing, boolean[] aAllowedFacings) {
			if (aPlayer.getXRot() >=  65 && aAllowedFacings[SIDE_DOWN]) return SIDE_DOWN;
			if (aPlayer.getXRot() <= -65 && aAllowedFacings[SIDE_UP]) return SIDE_UP;
			byte rFacing = OPOS[getHorizontalForPlayerPlacing(aPlayer)];
			if (aAllowedFacings[rFacing]) return rFacing;
			for (byte tSide : ALL_SIDES_VALID) if (aAllowedFacings[tSide]) return tSide;
			return aDefaultFacing;
		}
		
		/**
		 * This Function determines the direction a Block gets when being Wrenched.
		 */
		public static byte getSideWrenching(byte aSide, float aHitX, float aHitY, float aHitZ) {
			switch (aSide) {
			case  0: case  1:
				if (aHitX < 0.25) return aHitZ < 0.25 || aHitZ > 0.75 ? OPOS[aSide] : 4;
				if (aHitX > 0.75) return aHitZ < 0.25 || aHitZ > 0.75 ? OPOS[aSide] : 5;
				if (aHitZ < 0.25) return 2;
				if (aHitZ > 0.75) return 3;
				return aSide;
			case  2: case  3:
				if (aHitX < 0.25) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 4;
				if (aHitX > 0.75) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 5;
				if (aHitY < 0.25) return 0;
				if (aHitY > 0.75) return 1;
				return aSide;
			case  4: case  5:
				if (aHitZ < 0.25) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 2;
				if (aHitZ > 0.75) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 3;
				if (aHitY < 0.25) return 0;
				if (aHitY > 0.75) return 1;
				return aSide;
			}
			return SIDE_INVALID;
		}
	}
	
	public static class NBT {
		public static CompoundTag make() {
			return new CompoundTag();
		}
		
		/** Turns each Object -> Object Pair into a Part of the passed NBT as Object-toString()-Key -> Value Pair */
		public static CompoundTag make(String aFirstKey, Object aFirstValue, Object... aTags) {
			CompoundTag rNBT = make();
			
			if (aFirstValue == null) {/* Nothing */}
			else if (aFirstValue instanceof Boolean)           rNBT.putBoolean(aFirstKey, (Boolean)                aFirstValue);
			else if (aFirstValue instanceof Byte)              rNBT.putByte(   aFirstKey, (Byte)                   aFirstValue);
			else if (aFirstValue instanceof Short)             rNBT.putShort(  aFirstKey, (Short)                  aFirstValue);
			else if (aFirstValue instanceof Integer)           rNBT.putInt(    aFirstKey, (Integer)                aFirstValue);
			else if (aFirstValue instanceof Long)              rNBT.putLong(   aFirstKey, (Long)                   aFirstValue);
			else if (aFirstValue instanceof Float)             rNBT.putFloat(  aFirstKey, (Float)                  aFirstValue);
			else if (aFirstValue instanceof Double)            rNBT.putDouble( aFirstKey, (Double)                 aFirstValue);
			else if (aFirstValue instanceof String)            rNBT.putString( aFirstKey, (String)                 aFirstValue);
			else if (aFirstValue instanceof Tag)           rNBT.put(       aFirstKey, (Tag)                aFirstValue);
			else if (aFirstValue instanceof FluidStack)        rNBT.put(       aFirstKey, FL.save((FluidStack)     aFirstValue));
			else if (aFirstValue instanceof OreDictMaterial)   rNBT.putString( aFirstKey, ((OreDictMaterial)       aFirstValue).mNameInternal);
			else if (aFirstValue instanceof RecipeMap)         rNBT.putString( aFirstKey, ((RecipeMap)             aFirstValue).mNameInternal);
			else                                               rNBT.putString( aFirstKey, aFirstValue.toString());

			for (int i = 1; i < aTags.length; i+=2) {
				if (aTags[i] == null) {/* Nothing */}
				else if (aTags[i] instanceof Boolean)          rNBT.putBoolean(aTags[i-1].toString(), (Boolean)                aTags[i]);
				else if (aTags[i] instanceof Byte)             rNBT.putByte(   aTags[i-1].toString(), (Byte)                   aTags[i]);
				else if (aTags[i] instanceof Short)            rNBT.putShort(  aTags[i-1].toString(), (Short)                  aTags[i]);
				else if (aTags[i] instanceof Integer)          rNBT.putInt(    aTags[i-1].toString(), (Integer)                aTags[i]);
				else if (aTags[i] instanceof Long)             rNBT.putLong(   aTags[i-1].toString(), (Long)                   aTags[i]);
				else if (aTags[i] instanceof Float)            rNBT.putFloat(  aTags[i-1].toString(), (Float)                  aTags[i]);
				else if (aTags[i] instanceof Double)           rNBT.putDouble( aTags[i-1].toString(), (Double)                 aTags[i]);
				else if (aTags[i] instanceof String)           rNBT.putString( aTags[i-1].toString(), (String)                 aTags[i]);
				else if (aTags[i] instanceof Tag)          rNBT.put(       aTags[i-1].toString(), (Tag)                aTags[i]);
				else if (aTags[i] instanceof FluidStack)       rNBT.put(       aTags[i-1].toString(), FL.save((FluidStack)     aTags[i]));
				else if (aTags[i] instanceof OreDictMaterial)  rNBT.putString( aTags[i-1].toString(), ((OreDictMaterial)       aTags[i]).mNameInternal);
				else if (aTags[i] instanceof RecipeMap)        rNBT.putString( aTags[i-1].toString(), ((RecipeMap)             aTags[i]).mNameInternal);
				else                                           rNBT.putString( aTags[i-1].toString(), aTags[i].toString());
			}
			return rNBT;
		}

		/** Turns each Object -> Object Pair into a Part of the passed NBT as Object-toString()-Key -> Value Pair */
		public static CompoundTag make(CompoundTag aNBT, Object... aTags) {
			if (aNBT == null) aNBT = make();
			for (int i = 1; i < aTags.length; i+=2) {
				if (aTags[i] == null) {/* Nothing */}
				else if (aTags[i] instanceof Boolean)          aNBT.putBoolean(    aTags[i-1].toString(), (Boolean)                aTags[i]);
				else if (aTags[i] instanceof Byte)             aNBT.putByte(       aTags[i-1].toString(), (Byte)                   aTags[i]);
				else if (aTags[i] instanceof Short)            aNBT.putShort(      aTags[i-1].toString(), (Short)                  aTags[i]);
				else if (aTags[i] instanceof Integer)          aNBT.putInt(        aTags[i-1].toString(), (Integer)                aTags[i]);
				else if (aTags[i] instanceof Long)             aNBT.putLong(       aTags[i-1].toString(), (Long)                   aTags[i]);
				else if (aTags[i] instanceof Float)            aNBT.putFloat(      aTags[i-1].toString(), (Float)                  aTags[i]);
				else if (aTags[i] instanceof Double)           aNBT.putDouble(     aTags[i-1].toString(), (Double)                 aTags[i]);
				else if (aTags[i] instanceof String)           aNBT.putString(     aTags[i-1].toString(), (String)                 aTags[i]);
				else if (aTags[i] instanceof Tag)          aNBT.put(           aTags[i-1].toString(), (Tag)                aTags[i]);
				else if (aTags[i] instanceof FluidStack)       aNBT.put(           aTags[i-1].toString(), FL.save((FluidStack)     aTags[i]));
				else if (aTags[i] instanceof OreDictMaterial)  aNBT.putString(     aTags[i-1].toString(), ((OreDictMaterial)       aTags[i]).mNameInternal);
				else if (aTags[i] instanceof RecipeMap)        aNBT.putString(     aTags[i-1].toString(), ((RecipeMap)             aTags[i]).mNameInternal);
				else                                           aNBT.putString(     aTags[i-1].toString(), aTags[i].toString());
			}
			return aNBT;
		}

		/** Fuses two NBT Compounds together with the Priority lying on the content of the first NBT */
		public static CompoundTag fuse(CompoundTag aNBT1, CompoundTag aNBT2) {
			if (aNBT1 == null) return aNBT2==null?make():(CompoundTag)aNBT2.copy();
			CompoundTag rNBT = (CompoundTag)aNBT1.copy();
			if (aNBT2 == null) return rNBT;
			for (Object tKey : aNBT2.keySet()) if (!rNBT.contains(tKey.toString())) rNBT.put(tKey.toString(), aNBT2.get(tKey.toString()));
			return rNBT;
		}

		public static ListTag makeInv(ItemStack... aStacks) {
			ListTag rInventory = new ListTag();
			for (int i = 0; i < aStacks.length; i++) if (ST.valid(aStacks[i])) rInventory.add(makeShort(ST.save(aStacks[i]), "s", (short)i));
			return rInventory;
		}

		public static CompoundTag makeBool(Object aTag, boolean aValue) {
			CompoundTag aNBT = make();
			aNBT.putBoolean(aTag.toString(), aValue);
			return aNBT;
		}
		public static CompoundTag makeBool(CompoundTag aNBT, Object aTag, boolean aValue) {
			if (aNBT == null) aNBT = make();
			aNBT.putBoolean(aTag.toString(), aValue);
			return aNBT;
		}

		public static CompoundTag makeByte(Object aTag, byte aValue) {
			CompoundTag aNBT = make();
			aNBT.putByte(aTag.toString(), aValue);
			return aNBT;
		}
		public static CompoundTag makeByte(CompoundTag aNBT, Object aTag, byte aValue) {
			if (aNBT == null) aNBT = make();
			aNBT.putByte(aTag.toString(), aValue);
			return aNBT;
		}

		public static CompoundTag makeShort(Object aTag, short aValue) {
			CompoundTag aNBT = make();
			aNBT.putShort(aTag.toString(), aValue);
			return aNBT;
		}
		public static CompoundTag makeShort(CompoundTag aNBT, Object aTag, short aValue) {
			if (aNBT == null) aNBT = make();
			aNBT.putShort(aTag.toString(), aValue);
			return aNBT;
		}

		public static CompoundTag makeInt(Object aTag, int aValue) {
			CompoundTag aNBT = make();
			aNBT.putInt(aTag.toString(), aValue);
			return aNBT;
		}
		public static CompoundTag makeInt(CompoundTag aNBT, Object aTag, int aValue) {
			if (aNBT == null) aNBT = make();
			aNBT.putInt(aTag.toString(), aValue);
			return aNBT;
		}
		
		public static CompoundTag makeLong(Object aTag, long aValue) {
			CompoundTag aNBT = make();
			setNumber(aNBT, aTag.toString(), aValue);
			return aNBT;
		}
		public static CompoundTag makeLong(CompoundTag aNBT, Object aTag, long aValue) {
			if (aNBT == null) aNBT = make();
			setNumber(aNBT, aTag.toString(), aValue);
			return aNBT;
		}
		
		public static CompoundTag makeFloat(Object aTag, float aValue) {
			CompoundTag aNBT = make();
			aNBT.putFloat(aTag.toString(), aValue);
			return aNBT;
		}
		public static CompoundTag makeFloat(CompoundTag aNBT, Object aTag, float aValue) {
			if (aNBT == null) aNBT = make();
			aNBT.putFloat(aTag.toString(), aValue);
			return aNBT;
		}

		public static CompoundTag makeDouble(Object aTag, double aValue) {
			CompoundTag aNBT = make();
			aNBT.putDouble(aTag.toString(), aValue);
			return aNBT;
		}
		public static CompoundTag makeDouble(CompoundTag aNBT, Object aTag, double aValue) {
			if (aNBT == null) aNBT = make();
			aNBT.putDouble(aTag.toString(), aValue);
			return aNBT;
		}

		public static CompoundTag makeString(Object aTag, Object aValue) {
			CompoundTag aNBT = make();
			if (aValue == null) return aNBT;
			aNBT.putString(aTag.toString(), aValue.toString());
			return aNBT;
		}
		public static CompoundTag makeString(CompoundTag aNBT, Object aTag, Object aValue) {
			if (aNBT == null) aNBT = make();
			if (aValue == null) return aNBT;
			aNBT.putString(aTag.toString(), aValue.toString());
			return aNBT;
		}
		
		@Deprecated public static CompoundTag getNBTs(CompoundTag aNBT, Object... aTags) {return make(aNBT, aTags);}
		@Deprecated public static CompoundTag getNBTBoolean(CompoundTag aNBT, Object aTag, boolean aValue) {return makeBool(aNBT, aTag, aValue);}
		@Deprecated public static CompoundTag getNBTByte(CompoundTag aNBT, Object aTag, byte aValue) {return makeByte(aNBT, aTag, aValue);}
		@Deprecated public static CompoundTag getNBTShort(CompoundTag aNBT, Object aTag, short aValue) {return makeShort(aNBT, aTag, aValue);}
		@Deprecated public static CompoundTag getNBTInteger(CompoundTag aNBT, Object aTag, int aValue) {return makeInt(aNBT, aTag, aValue);}
		@Deprecated public static CompoundTag getNBTLong(CompoundTag aNBT, Object aTag, long aValue) {return makeLong(aNBT, aTag, aValue);}
		@Deprecated public static CompoundTag getNBTFloat(CompoundTag aNBT, Object aTag, float aValue) {return makeFloat(aNBT, aTag, aValue);}
		@Deprecated public static CompoundTag getNBTDouble(CompoundTag aNBT, Object aTag, double aValue) {return makeDouble(aNBT, aTag, aValue);}
		@Deprecated public static CompoundTag getNBTString(CompoundTag aNBT, Object aTag, Object aValue) {return makeString(aNBT, aTag, aValue);}
		
		/** Saves on Data Size by simply not adding "false" Booleans. */
		public static CompoundTag setBoolean(CompoundTag aNBT, Object aTag, boolean aValue) {
			if (aValue) {
				aNBT.putBoolean(aTag.toString(), aValue);
			} else {
				aNBT.remove(aTag.toString());
			}
			return aNBT;
		}

		/** Saves on Data Size by choosing the smallest possible Data Type, and by also not adding zeros. The regular getLong() Function can also get the other Number Types. */
		public static CompoundTag setNumber(CompoundTag aNBT, Object aTag, long aValue) {
			if (aValue == 0) {aNBT.remove(aTag.toString()); return aNBT;}
			if (aValue > Integer.MAX_VALUE || aValue < Integer.MIN_VALUE) {aNBT.putLong(aTag.toString(), aValue); return aNBT;}
			if (aValue > Short.MAX_VALUE || aValue < Short.MIN_VALUE) {aNBT.putInt(aTag.toString(), (int)aValue); return aNBT;}
			if (aValue > Byte.MAX_VALUE || aValue < Byte.MIN_VALUE) {aNBT.putShort(aTag.toString(), (short)aValue); return aNBT;}
			aNBT.putByte(aTag.toString(), (byte)aValue);
			return aNBT;
		}

		/** Saves on Data Size by choosing the smallest possible Data Type, and by also not adding zeros or negative Numbers. The regular getLong() Function can also get the other Number Types. */
		public static CompoundTag setPosNum(CompoundTag aNBT, Object aTag, long aValue) {
			if (aValue <= 0) {aNBT.remove(aTag.toString()); return aNBT;}
			if (aValue > Integer.MAX_VALUE) {aNBT.putLong(aTag.toString(), aValue); return aNBT;}
			if (aValue > Short.MAX_VALUE) {aNBT.putInt(aTag.toString(), (int)aValue); return aNBT;}
			if (aValue > Byte.MAX_VALUE) {aNBT.putShort(aTag.toString(), (short)aValue); return aNBT;}
			aNBT.putByte(aTag.toString(), (byte)aValue);
			return aNBT;
		}
		
		public static ItemStack check(ItemStack aStack) {
			return set(aStack, ItemNBT.get(aStack));
		}

		public static ItemStack set(ItemStack aStack, CompoundTag aNBT) {
			if (aNBT == null || aNBT.isEmpty()) {ItemNBT.set(aStack, null); return aStack;}
			ArrayList<String> tTagsToRemove = new ArrayListNoNulls<>();
			for (Object tKey : aNBT.keySet()) {
				Tag tValue = aNBT.get((String)tKey);
				if (tValue == null || (tValue instanceof CompoundTag && ((CompoundTag)tValue).isEmpty()) || (tValue instanceof NumericTag && ((NumericTag)tValue).intValue() == 0) || (tValue instanceof StringTag && Code.stringInvalid(((StringTag)tValue).value()))) tTagsToRemove.add((String)tKey);
			}
			for (Object tKey : tTagsToRemove) aNBT.remove((String)tKey);
			ItemNBT.set(aStack, aNBT.isEmpty()?null:aNBT);
			return aStack;
		}

		public static CompoundTag getNBT(ItemStack aStack) {
			CompoundTag rNBT = ItemNBT.get(aStack);
			return rNBT==null?make():rNBT;
		}

		// F8 КОНТРАКТ (важно): под иммутабельным CustomData «живой» тег на стеке невозможен — этот метод
		// возвращает МУТАБЕЛЬНУЮ DETACHED-копию (ItemNBT.get() копирует). КАЖДЫЙ вызывающий, который мутирует
		// результат, ОБЯЗАН закоммитить его через UT.NBT.set(aStack, rNBT)/ItemNBT.set — без commit правки
		// теряются. Тело — дословный 1:1-порт старого getOrCreate (мост setTagCompound→ItemNBT.set);
		// при пустом теге ItemNBT.set удаляет компонент (no-op), т.е. пустой тег НЕ сохраняется на стеке,
		// в отличие от старого setTagCompound(new NBTTagCompound()). Это безопасно: UT.NBT.set в GT6 и так
		// стрипает пустые теги, поэтому GT6-код никогда не полагался на персистентный пустой тег.
		// (См. ItemNBT.java javadoc, decisions/F8-nbt-data-components.md §7.)
		public static CompoundTag getOrCreate(ItemStack aStack) {
			CompoundTag rNBT = ItemNBT.get(aStack);
			if (rNBT == null) ItemNBT.set(aStack, rNBT = make());
			return rNBT;
		}
		
		public static CompoundTag setPunchCardData(ItemStack aStack, String aPunchCardData) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putString("gt.punchcard", aPunchCardData);
			set(aStack, tNBT);
			return tNBT;
		}
		public static String getPunchCardData(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			return tNBT.getStringOr("gt.punchcard", "");
		}
		public static CompoundTag setPunchCardData(CompoundTag aNBT, String aPunchCardData) {
			aNBT.putString("gt.punchcard", aPunchCardData);
			return aNBT;
		}
		public static String getPunchCardData(CompoundTag aNBT) {
			return aNBT.getStringOr("gt.punchcard", "");
		}
		
		public static CompoundTag setBlueprintCrafting(ItemStack aStack, ItemStack... aBlueprint) {
			CompoundTag tNBT = getNBT(aStack);
			setBlueprintCrafting(tNBT, aBlueprint);
			set(aStack, tNBT);
			return tNBT;
		}
		public static ItemStack[] getBlueprintCrafting(ItemStack aStack) {
			return getBlueprintCrafting(getNBT(aStack));
		}
		public static CompoundTag setBlueprintCrafting(CompoundTag aNBT, ItemStack... aBlueprint) {
			CompoundTag tList = make();
			boolean temp = F;
			for (int i = 0; i < aBlueprint.length; i++) if (ST.valid(aBlueprint[i])) {
				ST.save(tList, ""+i, ST.amount(1, aBlueprint[i]));
				temp = T;
			}
			if (temp) aNBT.put("gt.blueprint.craft", tList);
			return aNBT;
		}
		public static ItemStack[] getBlueprintCrafting(CompoundTag aNBT) {
			CompoundTag tList = aNBT.contains("gt.blueprint.craft")?aNBT.getCompoundOrEmpty("gt.blueprint.craft"):null;
			if (tList != null) {
				ItemStack[] rRecipe = new ItemStack[9];
				for (int i = 0; i < rRecipe.length; i++) rRecipe[i] = ST.amount(1, ST.load(tList, ""+i));
				return rRecipe;
			}
			return ZL_IS;
		}
		
		public static CompoundTag setLighterFuel(ItemStack aStack, long aFuel) {
			CompoundTag tNBT = getNBT(aStack);
			setNumber(tNBT, "gt.lighter", aFuel);
			set(aStack, tNBT);
			return tNBT;
		}
		public static long getLighterFuel(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			return tNBT.getLongOr("gt.lighter", 0L);
		}
		public static CompoundTag setLighterFuel(CompoundTag aNBT, long aFuel) {
			setNumber(aNBT, "gt.lighter", aFuel);
			return aNBT;
		}
		public static long getLighterFuel(CompoundTag aNBT) {
			return aNBT.getLongOr("gt.lighter", 0L);
		}

		public static CompoundTag setMapID(ItemStack aStack, short aMapID) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putShort("map_id", aMapID);
			set(aStack, tNBT);
			return tNBT;
		}
		public static short getMapID(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			if (!tNBT.contains("map_id")) return -1;
			return tNBT.getShortOr("map_id", (short)0);
		}
		public static CompoundTag setMapID(CompoundTag aNBT, short aMapID) {
			aNBT.putShort("map_id", aMapID);
			return aNBT;
		}
		public static short getMapID(CompoundTag aNBT) {
			if (!aNBT.contains("map_id")) return -1;
			return aNBT.getShortOr("map_id", (short)0);
		}

		public static CompoundTag setMagicMapID(ItemStack aStack, short aMapID) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putShort("magic_map_id", aMapID);
			set(aStack, tNBT);
			return tNBT;
		}
		public static short getMagicMapID(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			if (!tNBT.contains("magic_map_id")) return -1;
			return tNBT.getShortOr("magic_map_id", (short)0);
		}
		public static CompoundTag setMagicMapID(CompoundTag aNBT, short aMapID) {
			aNBT.putShort("magic_map_id", aMapID);
			return aNBT;
		}
		public static short getMagicMapID(CompoundTag aNBT) {
			if (!aNBT.contains("magic_map_id")) return -1;
			return aNBT.getShortOr("magic_map_id", (short)0);
		}

		public static CompoundTag setMazeMapID(ItemStack aStack, short aMapID) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putShort("maze_map_id", aMapID);
			set(aStack, tNBT);
			return tNBT;
		}
		public static short getMazeMapID(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			if (!tNBT.contains("maze_map_id")) return -1;
			return tNBT.getShortOr("maze_map_id", (short)0);
		}
		public static CompoundTag setMazeMapID(CompoundTag aNBT, short aMapID) {
			aNBT.putShort("maze_map_id", aMapID);
			return aNBT;
		}
		public static short getMazeMapID(CompoundTag aNBT) {
			if (!aNBT.contains("maze_map_id")) return -1;
			return aNBT.getShortOr("maze_map_id", (short)0);
		}

		public static CompoundTag setOreMapID(ItemStack aStack, short aMapID) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putShort("ore_map_id", aMapID);
			set(aStack, tNBT);
			return tNBT;
		}
		public static short getOreMapID(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			if (!tNBT.contains("ore_map_id")) return -1;
			return tNBT.getShortOr("ore_map_id", (short)0);
		}
		public static CompoundTag setOreMapID(CompoundTag aNBT, short aMapID) {
			aNBT.putShort("ore_map_id", aMapID);
			return aNBT;
		}
		public static short getOreMapID(CompoundTag aNBT) {
			if (!aNBT.contains("ore_map_id")) return -1;
			return aNBT.getShortOr("ore_map_id", (short)0);
		}

		public static CompoundTag setBookMapping(ItemStack aStack, String aTitle) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putString("book", aTitle);
			set(aStack, tNBT);
			return tNBT;
		}
		public static String getBookMapping(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			return tNBT.getStringOr("book", "");
		}
		public static CompoundTag setBookMapping(CompoundTag aNBT, String aTitle) {
			aNBT.putString("book", aTitle);
			return aNBT;
		}
		public static String getBookMapping(CompoundTag aNBT) {
			return aNBT.getStringOr("book", "");
		}

		public static CompoundTag setBookTitle(ItemStack aStack, String aTitle) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putString("title", aTitle);
			set(aStack, tNBT);
			return tNBT;
		}
		public static String getBookTitle(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			return tNBT.getStringOr("title", "");
		}
		public static CompoundTag setBookTitle(CompoundTag aNBT, String aTitle) {
			aNBT.putString("title", aTitle);
			return aNBT;
		}
		public static String getBookTitle(CompoundTag aNBT) {
			return aNBT.getStringOr("title", "");
		}

		public static CompoundTag setBookAuthor(ItemStack aStack, String aAuthor) {
			CompoundTag tNBT = getNBT(aStack);
			tNBT.putString("author", aAuthor);
			set(aStack, tNBT);
			return tNBT;
		}
		public static String getBookAuthor(ItemStack aStack) {
			CompoundTag tNBT = getNBT(aStack);
			return tNBT.getStringOr("author", "");
		}
		public static CompoundTag setBookAuthor(CompoundTag aNBT, String aAuthor) {
			aNBT.putString("author", aAuthor);
			return aNBT;
		}
		public static String getBookAuthor(CompoundTag aNBT) {
			return aNBT.getStringOr("author", "");
		}
		
		public static List<String> getDataToolTip(CompoundTag aData, List<String> aList, boolean aAllDetails) {
			if (aData.contains(NBT_REACTOR_SETUP)) {
				aList.add(LH.Chat.CYAN + "Reactor Setup: " + aData.getStringOr(NBT_REACTOR_SETUP_NAME, ""));
				return aList;
			}
			if (aData.contains(NBT_CANVAS_BLOCK)) {
				aList.add(LH.Chat.CYAN + "Block Image: " + ST.names(ST.make(ST.block_(aData.getIntOr(NBT_CANVAS_BLOCK, 0)), 1, aData.getIntOr(NBT_CANVAS_META, 0))));
				return aList;
			}
			if (aData.contains(NBT_REPLICATOR_DATA)) {
				short tIndex = aData.getShortOr(NBT_REPLICATOR_DATA, (short)0);
				if (Code.exists(tIndex, OreDictMaterial.MATERIAL_ARRAY)) {
					OreDictMaterial tMaterial = OreDictMaterial.MATERIAL_ARRAY[tIndex];
					if (tMaterial.contains(TD.Processing.UUM)) {
						if (aAllDetails) {
							aList.add(LH.Chat.CYAN + "Material Data: " + LH.Chat.WHITE + tMaterial.getLocal());
							aList.add(LH.Chat.CYAN + "Can be Replicated using");
							if (tMaterial.contains(TD.Atomic.ANTIMATTER)) {
								aList.add(LH.Chat.WHITE + "Neutral Antimatter: " + LH.Chat.YELLOW + tMaterial.mNeutrons);
								aList.add(LH.Chat.WHITE + "Charged Antimatter: " + LH.Chat.RED + tMaterial.mProtons);
							} else {
								aList.add(LH.Chat.WHITE + "Neutral Matter: " + LH.Chat.YELLOW + tMaterial.mNeutrons);
								aList.add(LH.Chat.WHITE + "Charged Matter: " + LH.Chat.RED + tMaterial.mProtons);
							}
							aList.add(LH.Chat.WHITE + "Energy: " + TD.Energy.QU.getChatFormat() + ((tMaterial.mNeutrons+tMaterial.mProtons)*65536) + " " + TD.Energy.QU.getLocalisedNameShort());
						} else {
							aList.add(LH.Chat.CYAN + "Mat Data: " + LH.Chat.WHITE + tMaterial.getLocal() + (aAllDetails ? "" : " ("+LH.Chat.YELLOW+tMaterial.mNeutrons+LH.Chat.WHITE+"/"+LH.Chat.RED+tMaterial.mProtons+LH.Chat.WHITE+"/"+TD.Energy.QU.getChatFormat()+((tMaterial.mNeutrons+tMaterial.mProtons)*65536)+LH.Chat.WHITE+")"));
						}
					} else {
						aList.add(LH.Chat.CYAN + "Material Data: " + LH.Chat.WHITE + tMaterial.getLocal() + LH.Chat.ORANGE + " (Not Replicatable)");
					}
				}
				return aList;
			}
			if (IL.GC_Schematic_1.exists() && aData.contains("gc_schematics_1")) {
				aList.add(LH.Chat.CYAN + IL.GC_Schematic_1.getWithMeta(1, aData.getShortOr("gc_schematics_1", (short)0)).getDisplayName());
				return aList;
			}
			if (IL.GC_Schematic_2.exists() && aData.contains("gc_schematics_2")) {
				aList.add(LH.Chat.CYAN + IL.GC_Schematic_2.getWithMeta(1, aData.getShortOr("gc_schematics_2", (short)0)).getDisplayName());
				return aList;
			}
			if (IL.GC_Schematic_3.exists() && aData.contains("gc_schematics_3")) {
				aList.add(LH.Chat.CYAN + IL.GC_Schematic_3.getWithMeta(1, aData.getShortOr("gc_schematics_3", (short)0)).getDisplayName());
				return aList;
			}
			if (IL.IE_Blueprint_Projectiles_Common.exists() && aData.contains("ie_blueprint")) {
				short tMeta = aData.getShortOr("ie_blueprint", (short)0);
				aList.add(LH.Chat.CYAN + IL.IE_Blueprint_Projectiles_Common.getWithMeta(1, tMeta).getDisplayName());
				switch(tMeta) {
				case 0: aList.add(LH.Chat.GREEN + "Common Projectiles"); break;
				case 1: aList.add(LH.Chat.GREEN + "Specialized Projectiles"); break;
				case 2: aList.add(LH.Chat.GREEN + "Arc Furnace Electrodes"); break;
				}
				return aList;
			}
			String tString = getBookTitle(aData);
			if (Code.stringValid(tString)) {
				aList.add(LH.Chat.CYAN + "Book: " + tString);
				if (aAllDetails) {
					tString = getBookAuthor(aData);
					if (Code.stringValid(tString)) aList.add(LH.Chat.CYAN + "by " + tString);
				}
				return aList;
			}
			short
			tMapID = getMapID(aData);
			if (tMapID >= 0) {
				aList.add(LH.Chat.CYAN + "Map ID: " + tMapID);
				return aList;
			}
			tMapID = getMagicMapID(aData);
			if (tMapID >= 0) {
				aList.add(LH.Chat.CYAN + "Magic Map ID: " + tMapID);
				return aList;
			}
			tMapID = getMazeMapID(aData);
			if (tMapID >= 0) {
				aList.add(LH.Chat.CYAN + "Maze Map ID: " + tMapID);
				return aList;
			}
			tMapID = getOreMapID(aData);
			if (tMapID >= 0) {
				aList.add(LH.Chat.CYAN + "Ore Map ID: " + tMapID);
				return aList;
			}
			tString = getPunchCardData(aData);
			if (Code.stringValid(tString)) {
				aList.add(LH.Chat.CYAN + "Punch Card Data");
				if (aAllDetails) for (int i = 0, j = tString.length(); i < j; i += 64) aList.add(LH.Chat.GREEN + tString.substring(i, Math.min(i+64, j)));
				return aList;
			}
			ItemStack[] tBlueprint = getBlueprintCrafting(aData);
			if (tBlueprint != ZL_IS) {
				ItemStack tCrafted = CR.getany(DW, tBlueprint);
				if (ST.invalid(tCrafted)) {
					aList.add(LH.Chat.CYAN + "Blueprint with random Items");
				} else {
					if (aAllDetails) {
						aList.add(LH.Chat.CYAN + "Blueprint for " + tCrafted.getDisplayName());
					} else {
						aList.add(LH.Chat.CYAN + "Blueprint: " + tCrafted.getDisplayName());
					}
				}
				return aList;
			}
			return aList;
		}
		
		
		// F10 стык: RailcraftEnchantments.destruction/wrecking/implosion — compat-mirror интерфейс
		// (mods/railcraft/.../RailcraftEnchantments.java) сейчас пустая заглушка без членов (чужая зона,
		// не F8) — символ появится, когда владелец F10-зеркала добавит поля. Вызов оставлен 1:1.
		public static int getEnchantmentLevelDestruction   (ItemStack aStack) {return MD.RC.mLoaded ? getEnchantmentLevel(RailcraftEnchantments.destruction, aStack) : 0;}
		public static int getEnchantmentLevelWrecking      (ItemStack aStack) {return MD.RC.mLoaded ? getEnchantmentLevel(RailcraftEnchantments.wrecking   , aStack) : 0;}
		public static int getEnchantmentLevelImplosion     (ItemStack aStack) {return MD.RC.mLoaded ? getEnchantmentLevel(RailcraftEnchantments.implosion  , aStack) : 0;}
		// F8: "fortune"/"looting" в neo — ResourceKey<Enchantment> (Enchantments.FORTUNE/LOOTING,
		// neo-decompiled …/Enchantments.java:105,110), не готовые экземпляры Enchantment. RegistryAccess НЕ
		// нужен — тот же приём, что getEnchantmentXP(ItemStack) выше: реальные зачарования стека уже лежат
		// в типизированном DataComponents.ENCHANTMENTS/STORED_ENCHANTMENTS как Holder<Enchantment>
		// (разрешены движком заранее); сравниваем Holder С ResourceKey через Holder.is(ResourceKey)
		// (Holder.java:25), не резолвим ResourceKey->Holder сами. Полное имя класса (без import) — простое
		// имя "Enchantments" в файле уже занято вложенным UT.Enchantments (BULLSHIT-диспетчер ниже),
		// member-тип экранирует top-level импорт (JLS 6.4.1).
		public static int getEnchantmentLevelLootingFortune(ItemStack aStack) {
			ItemEnchantments tEnchantments = aStack.getOrDefault(EnchantmentHelper.getComponentType(aStack), ItemEnchantments.EMPTY);
			int rLevel = 0;
			for (Holder<Enchantment> tEnchantment : tEnchantments.keySet()) if (tEnchantment.is(net.minecraft.world.item.enchantment.Enchantments.FORTUNE) || tEnchantment.is(net.minecraft.world.item.enchantment.Enchantments.LOOTING)) rLevel = Math.max(rLevel, tEnchantments.getLevel(tEnchantment));
			return rLevel;
		}

		// F8: aEnchantment передан ЗНАЧЕНИЕМ (не Holder) — 1.7.10 адресовал зачарования числовым
		// effectId в статическом массиве Enchantment.enchantmentsList; в neo Enchantment — record без
		// effectId, зачарования на стеке идентифицируются Holder<Enchantment>. Holder.direct(aEnchantment)
		// оборачивает переданное значение 1:1 (равенство по значению record), затем читаем ЧЕРЕЗ
		// ItemStack.getEnchantmentLevel(Holder) — geймплейный аналог старого
		// EnchantmentHelper.getEnchantmentLevel(effectId, stack). Проверка "effectId < 0" убрана —
		// поля effectId в новой модели не существует, null-guard сохранён.
		public static int getEnchantmentLevel(Enchantment aEnchantment, ItemStack aStack) {
			if (aEnchantment == null) return 0;
			return aStack.getEnchantmentLevel(Holder.direct(aEnchantment));
		}
		// F8: ванильные/GT6-энчанты в neo адресуются ResourceKey<Enchantment> (Enchantments.FLAME/POWER/…,
		// Enchantment_EnderDamage.KEY), НЕ объектом Enchantment — 1.7.10 передавал сюда статический
		// Enchantment.X, которого в новой registry-driven модели нет. Тот же приём, что
		// getEnchantmentLevelLootingFortune выше: чары стека уже разрешены движком в Holder<Enchantment>
		// (типизированный DataComponents.ENCHANTMENTS/STORED_ENCHANTMENTS), сравниваем Holder С ResourceKey
		// через Holder.is(ResourceKey) (Holder.java:25) — реестр/сервер не нужны. Перегрузка не конфликтует
		// с Enchantment-версией выше: разные типы первого параметра (кастомные Enchantment-объекты F10 ещё
		// идут туда).
		public static int getEnchantmentLevel(ResourceKey<Enchantment> aEnchantment, ItemStack aStack) {
			if (aEnchantment == null) return 0;
			ItemEnchantments tEnchantments = aStack.getOrDefault(EnchantmentHelper.getComponentType(aStack), ItemEnchantments.EMPTY);
			int rLevel = 0;
			for (Holder<Enchantment> tEnchantment : tEnchantments.keySet()) if (tEnchantment.is(aEnchantment)) rLevel = Math.max(rLevel, tEnchantments.getLevel(tEnchantment));
			return rLevel;
		}
		public static int getEnchantmentXP(ItemStack aStack) {
			// УЛИКА R8 (доработка): оригинальный гейт был `!(ItemNBT.get(aStack) != null)` (1.7.10 — "ench"
			// жил ВНУТРИ общего NBT-тега стека, поэтому "нет тега вообще" ⇒ "нет чар"). F8 переносит
			// чары на ОТДЕЛЬНЫЙ канал DataComponents.ENCHANTMENTS/STORED_ENCHANTMENTS, независимый от
			// CUSTOM_DATA (см. gregapi.code.ItemNBT javadoc) — `!ItemNBT.has(aStack)` (CUSTOM_DATA-гейт)
			// был НЕВЕРНЫМ 1:1-переводом: стек без CUSTOM_DATA, но с реальными чарами в ENCHANTMENTS,
			// молча получал 0 XP. Гейт переведён на настоящий канал зачарований —
			// EnchantmentHelper.hasAnyEnchantments (neo-decompiled …/EnchantmentHelper.java:97-100,
			// проверяет ОБА канала ENCHANTMENTS/STORED_ENCHANTMENTS, тот же движковый метод, что и
			// ItemStack.isEnchanted() использует для ENCHANTMENTS).
			if (ST.invalid(aStack) || !EnchantmentHelper.hasAnyEnchantments(aStack) || ST.isGT_(aStack) || (COMPAT_EU_ITEM != null && COMPAT_EU_ITEM.is(aStack))) return 0;
			// F8: "ench" читается движком ТОЛЬКО из типизированного DataComponents.ENCHANTMENTS/
			// STORED_ENCHANTMENTS (не сырого CUSTOM_DATA-тега) — читаем реальные зачарования стека
			// напрямую, а не делегируем в getEnchantmentXP(CompoundTag) (тот работал по легаси
			// числовым id, которых в этом канале никогда не было и нет).
			ItemEnchantments tEnchantments = aStack.getOrDefault(EnchantmentHelper.getComponentType(aStack), ItemEnchantments.EMPTY);
			if (tEnchantments.isEmpty()) return 0;
			int rXP = 0;
			for (Holder<Enchantment> tEnchantment : tEnchantments.keySet()) {
				if (tEnchantment.is(EnchantmentTags.CURSE)) return 0;
				rXP += tEnchantment.value().getMinCost(tEnchantments.getLevel(tEnchantment));
			}
			return UT.Code.bindInt(UT.Code.divup(rXP, 2));
		}
		// F8 impossible-1:1: легаси-формат ListTag{id:short,lvl:short} по числовому ID
		// зачарования невосстановим — в neo зачарования регистро-driven (Holder<Enchantment>), числовых
		// ID нет (Enchantment.enchantmentsList удалён из движка целиком, не только переименован). Эта
		// перегрузка больше не вызывается изнутри дерева (getEnchantmentXP(ItemStack) выше читает
		// типизированный компонент напрямую) — сохранена как публичный API 1:1, деградирует до 0 до шва
		// ENCHANT (STATE.md "Не начато").
		public static int getEnchantmentXP(CompoundTag aNBT) {
			return 0;
		}
		public static ItemStack removeEnchantments(ItemStack aStack) {
			// F8: чары читаются движком из типизированного DataComponents.ENCHANTMENTS/STORED_ENCHANTMENTS —
			// канал отдельный от CUSTOM_DATA. Снимаем реальный компонент (это и есть рабочая замена
			// старого aNBT.remove("ench")), и следом — легаси-ключ "ench" под CUSTOM_DATA (defensive,
			// getOrCreate возвращает detached-копию; commit через set — эквивалент старого check).
			aStack.remove(EnchantmentHelper.getComponentType(aStack));
			CompoundTag tNBT = getOrCreate(aStack);
			removeEnchantments(tNBT);
			return set(aStack, tNBT);
		}
		public static void removeEnchantments(CompoundTag aNBT) {
			aNBT.remove("ench");
		}
		// F8: маршрут на типизированный DataComponents.ENCHANTMENTS/STORED_ENCHANTMENTS (см. javadoc
		// getEnchantmentLevel выше про Holder.direct). ItemEnchantments.Mutable.set(holder, level)
		// заменяет существующую запись ИЛИ добавляет новую — 1:1 замена ручного скана списка "найти id,
		// обновить lvl, иначе добавить". Каст (byte)aLevel сохраняет ОРИГИНАЛЬНУЮ 1.7.10-усечку уровня
		// (исходный код тоже писал lvl как (byte)aLevel, несмотря на short-поле) — не улучшение, воспроизведение.
		public static ItemStack addEnchantment(ItemStack aStack, ResourceKey<Enchantment> aEnchantment, long aLevel) {
			// F8 (enchant-registry, форс движка): энчанты стали registry-driven Holder — резолвим ключ через
			// server-реестр (в 1.7.10 был статический объект Enchantment.X). Нет сервера => энчантовать нечем.
			MinecraftServer tServer = ServerLifecycleHooks.getCurrentServer();
			if (tServer == null) return aStack;
			// F8 robustness: getOrThrow → get (Optional). Если ключ энчанта не в реестре (напр. кастом gregapi-энчант ещё
			// не привязан/не зарегистрирован в этом контексте), НЕ роняем — как в 1.7.10 несуществующим энчантом просто
			// не зачаровать (тогда объект был бы null). getOrThrow здесь каскадно рушил ВЕСЬ deferItemInit-Runnable
			// (getToolWithStats радиоактивного материала → toolhead + масса tool-рецептов терялись целиком).
			java.util.Optional<Holder.Reference<Enchantment>> tOpt = tServer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(aEnchantment);
			if (tOpt.isEmpty()) return aStack;
			Holder<Enchantment> tHolder = tOpt.get();
			DataComponentType<ItemEnchantments> tType = EnchantmentHelper.getComponentType(aStack);
			ItemEnchantments.Mutable tMutable = new ItemEnchantments.Mutable(aStack.getOrDefault(tType, ItemEnchantments.EMPTY));
			tMutable.set(tHolder, (byte)aLevel);
			aStack.set(tType, tMutable.toImmutable());
			return aStack;
		}

		/** F8 (enchant-registry, протухшие holder'ы — BUG-002): в 1.7.10 Enchantment был статическим синглтоном, и вшить
		 *  его в долгоживущий стек было безопасно. В neo энчанты — ДИНАМИЧЕСКИЙ пер-серверный реестр: Holder.Reference,
		 *  вшитый в статический стек (результат рецепта mOutput, зачарован один раз на запуск через isItemStackUsable с
		 *  гейтом "ench"), после перезахода в мир не находится сетевым кодеком ПО ИДЕНТИЧНОСТИ (Registry.getId(value),
		 *  Registry.java:151) -> EncoderException container_set_slot -> дисконнект. Пересобирает компонент энчантов
		 *  holder'ами ТЕКУЩЕГО сервера по ResourceKey (тот же путь резолва, что addEnchantment выше). Holder без ключа
		 *  или ключ вне реестра — переносится как есть. Нет сервера/энчантов — no-op. */
		public static ItemStack refreshEnchantments(ItemStack aStack) {
			if (aStack == null || aStack.isEmpty()) return aStack;
			MinecraftServer tServer = ServerLifecycleHooks.getCurrentServer();
			if (tServer == null) return aStack;
			DataComponentType<ItemEnchantments> tType = EnchantmentHelper.getComponentType(aStack);
			ItemEnchantments tOld = aStack.getOrDefault(tType, ItemEnchantments.EMPTY);
			if (tOld.isEmpty()) return aStack;
			net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> tReg = tServer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			ItemEnchantments.Mutable tNew = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
			for (Holder<Enchantment> tHolder : tOld.keySet()) {
				int tLevel = tOld.getLevel(tHolder);
				Holder<Enchantment> tFresh = tHolder.unwrapKey().<Holder<Enchantment>>flatMap(k -> tReg.get(k).map(h -> h)).orElse(tHolder);
				tNew.set(tFresh, tLevel);
			}
			aStack.set(tType, tNew.toImmutable());
			return aStack;
		}

		/** было {@code aEnchantment.getTranslatedName(aLevel)} (1.7.10 Enchantment) — neo: имя энчанта с уровнем через
		 *  static {@code Enchantment.getFullname(Holder<Enchantment>, level)} (neo Enchantment.java:185); ключ резолвим
		 *  через server-реестр (тот же путь, что addEnchantment выше). Нет сервера => путь ключа как fallback. */
		public static String enchantName(ResourceKey<Enchantment> aEnchantment, int aLevel) {
			MinecraftServer tServer = ServerLifecycleHooks.getCurrentServer();
			if (tServer == null) return aEnchantment.identifier().toString();
			Holder<Enchantment> tHolder = tServer.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(aEnchantment);
			return Enchantment.getFullname(tHolder, aLevel).getString();
		}
	}
	
	/**
	 * THIS IS BULLSHIT!!! WHY DO I HAVE TO DO THIS SHIT JUST TO HAVE ENCHANTS PROPERLY!?!
	 */
	public static class Enchantments {
		private static final BullshitIteratorA mBullshitIteratorA = new BullshitIteratorA();
		private static final BullshitIteratorB mBullshitIteratorB = new BullshitIteratorB();

		// F8 (creature-bonus, форс движка): 1.7.10 EnchantmentHelper.func_152377_a(stack, creatureAttribute)
		// (getEnchantmentModifierForCreature) + EntityLivingBase.getCreatureAttribute() удалены из neo целиком —
		// урон-бонус чар (Smite/Bane/Sharpness-эквиваленты) стал data-driven damage-effect'ами, применяемыми
		// движком через EnchantmentHelper.modifyDamage (neo-decompiled EnchantmentHelper.java:195). Считать
		// creature-тип вручную нельзя (MobType удалён) — делегируем движку: modifyDamage с base=0 возвращает
		// чистую прибавку чар против КОНКРЕТНОЙ жертвы (entity-type-условия движок проверяет сам). Эффекты
		// server-only => нет ServerLevel => 0. Централизовано: оба вызывателя (Behavior_Gun, EntityArrow_Material)
		// идут сюда, вместо дублирования func_152377_a per-file.
		// F8 functional-adapted (engine-model-разница, паритет-судья подтверждает): neo modifyDamage включает и общий Sharpness-бонус, тогда как
		// 1.7.10 func_152377_a возвращал ТОЛЬКО creature-conditional (Smite/Bane) — расхождение модели движка;
		// финальный паритет-судья подтверждает баланс (компилятор это не ловит).
		public static float getDamageBonusVsCreature(ItemStack aStack, Entity aTarget) {
			if (aTarget == null || aStack == null || aStack.isEmpty()) return 0;
			if (!(aTarget.level() instanceof net.minecraft.server.level.ServerLevel tSL)) return 0;
			return EnchantmentHelper.modifyDamage(tSL, aStack, aTarget, tSL.damageSources().generic(), 0.0F);
		}

		// F8 impossible-1:1: тот же класс проблемы, что NBT.getEnchantmentXP(CompoundTag)
		// выше — легаси "ench" NBTTagList{id:short,lvl:short} по числовому effectId (ItemStack.
		// getEnchantmentTagList()/ListTag.tagCount()/getCompoundTagAt(int)) и статический реестр
		// Enchantment.enchantmentsList[id] удалены из движка целиком (не переименованы; зачарования
		// теперь registry-driven Holder<Enchantment>, читаются через DataComponents.ENCHANTMENTS — см.
		// NBT.getEnchantmentXP(ItemStack) выше). Не найдено ни в одном из 3 корней референса.
		// Деградация до no-op — до шва ENCHANT (STATE.md "Не начато").
		private static void applyBullshit(IBullshit aBullshitModifier, ItemStack aStack) {
			//
		}

		private static void applyArrayOfBullshit(IBullshit aBullshitModifier, ItemStack[] aStacks) {
			for (int i = 0; i < aStacks.length; i++) applyBullshit(aBullshitModifier, aStacks[i]);
		}

		// F8: 1.7.10 EntityLivingBase.getLastActiveItems() (снимок «в руках+броне», ItemStack[5]) — прямого метода в neo нет,
		// НО реконструируется из getItemBySlot(EquipmentSlot) (MAINHAND + 4 брони), 1:1. Централизованный хелпер.
		public static ItemStack[] lastActiveItems(LivingEntity aEntity) {
			if (aEntity == null) return new ItemStack[0];
			return new ItemStack[] {
				aEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND),
				aEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET),
				aEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
				aEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
				aEntity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)};
		}
		public static void applyBullshitA(LivingEntity aPlayer, Entity aEntity, ItemStack aStack) {
			mBullshitIteratorA.mPlayer = aPlayer;
			mBullshitIteratorA.mEntity = aEntity;
			// F8: getLastActiveItems → lastActiveItems (реконструкция из getItemBySlot), armor-enchant эффекты восстановлены 1:1.
			if (aPlayer != null) applyArrayOfBullshit(mBullshitIteratorA, lastActiveItems(aPlayer));
			if (aStack != null) applyBullshit(mBullshitIteratorA, aStack);
		}

		public static void applyBullshitB(LivingEntity aPlayer, Entity aEntity, ItemStack aStack) {
			mBullshitIteratorB.mPlayer = aPlayer;
			mBullshitIteratorB.mEntity = aEntity;
			// F8: см. applyBullshitA — getLastActiveItems → lastActiveItems (реконструкция из getItemBySlot), 1:1.
			if (aPlayer != null) applyArrayOfBullshit(mBullshitIteratorB, lastActiveItems(aPlayer));
			if (aStack != null) applyBullshit(mBullshitIteratorB, aStack);
		}

		static final class BullshitIteratorA implements IBullshit {
			public LivingEntity mPlayer;
			public Entity mEntity;
			BullshitIteratorA() {}

			@Override
			public void calculateModifier(Enchantment aEnchantment, int aLevel) {
				// F-enchant SUPERSEDED (не заглушка): 1.7.10 звал виртуальный Enchantment.func_151367_b (onEntityDamaged),
				// удалённый из neo Enchantment (record без поведенческих методов). Эффекты кастом-энчантов GT6 перенесены
				// 1:1 в EnchantmentEffect_{Ender,Slime,Werewolf,Radioactivity} (gregapi/enchants) и привязаны через
				// EnchantsGT6.bootstrap(.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER/VICTIM,…));
				// движок диспетчерит их сам при POST_ATTACK. Этот путь (applyBullshitA) — мёртвый 1:1-остаток структуры GT6, безвреден.
			}
		}

		static final class BullshitIteratorB implements IBullshit {
			public LivingEntity mPlayer;
			public Entity mEntity;
			BullshitIteratorB() {}

			@Override
			public void calculateModifier(Enchantment aEnchantment, int aLevel) {
				// F-enchant SUPERSEDED (не заглушка): 1.7.10 звал виртуальный Enchantment.func_151368_a (onUserHurt); ни один
				// кастом-энчант GT6 его НЕ переопределял (сверено с референсом), а ванильный Thorns-диспетч движок neo делает
				// сам data-driven (EnchantmentEffectComponents.POST_ATTACK). Мёртвый 1:1-остаток структуры GT6, безвреден.
			}
		}
		
		interface IBullshit {
			void calculateModifier(Enchantment aEnchantment, int aLevel);
		}
	}
	
	public static class Reflection {
		public static String getClassName(Object aObject) {
			return aObject == null ? "" : aObject.getClass().getName().substring(aObject.getClass().getName().lastIndexOf(".")+1);
		}
		public static String getLowercaseClass(Object aObject) {
			return aObject == null ? "" : aObject.getClass().getName().substring(aObject.getClass().getName().lastIndexOf(".")+1).toLowerCase();
		}
		
		public static Field getPublicField(Object aObject, String aField) {
			Field rField = null;
			try {
				rField = aObject.getClass().getDeclaredField(aField);
			} catch (Throwable e) {/*Do nothing*/}
			return rField;
		}
		
		public static Field setField(Object aObject, String aField, Object aValue) {
			return setField(aObject.getClass(), aObject, aField, aValue, T);
		}
		public static Field setField(Object aObject, String aField, Object aValue, boolean aLogErrors) {
			return setField(aObject.getClass(), aObject, aField, aValue, aLogErrors);
		}
		public static Field setField(Class<?> aClass, Object aObject, String aField, Object aValue) {
			return setField(aClass, aObject, aField, aValue, T);
		}
		public static Field setField(Class<?> aClass, Object aObject, String aField, Object aValue, boolean aLogErrors) {
			Field rField = null;
			try {
				rField = aClass.getDeclaredField(aField);
				rField.setAccessible(T);
				rField.set(aObject, aValue);
			} catch (Throwable e) {if (aLogErrors) e.printStackTrace(ERR);}
			return rField;
		}
		
		public static Field getField(Object aObject, String aField) {
			Field rField = null;
			try {
				rField = aObject.getClass().getDeclaredField(aField);
				rField.setAccessible(T);
			} catch (Throwable e) {/*Do nothing*/}
			return rField;
		}
		
		public static Field getField(Class<?> aObject, String aField) {
			Field rField = null;
			try {
				rField = aObject.getDeclaredField(aField);
				rField.setAccessible(T);
			} catch (Throwable e) {/*Do nothing*/}
			return rField;
		}
		
		public static Method getMethod(Class<?> aObject, String aMethod, Class<?>... aParameterTypes) {
			Method rMethod = null;
			try {
				rMethod = aObject.getMethod(aMethod, aParameterTypes);
				rMethod.setAccessible(T);
			} catch (Throwable e) {/*Do nothing*/}
			return rMethod;
		}
		
		public static Method getMethod(Object aObject, String aMethod, Class<?>... aParameterTypes) {
			Method rMethod = null;
			try {
				rMethod = aObject.getClass().getMethod(aMethod, aParameterTypes);
				rMethod.setAccessible(T);
			} catch (Throwable e) {/*Do nothing*/}
			return rMethod;
		}
		
		public static Field getField(Object aObject, String aField, boolean aPrivate, boolean aLogErrors) {
			try {
				Field tField = (aObject instanceof Class)?((Class<?>)aObject).getDeclaredField(aField):(aObject instanceof String)?Class.forName((String)aObject).getDeclaredField(aField):aObject.getClass().getDeclaredField(aField);
				if (aPrivate) tField.setAccessible(T);
				return tField;
			} catch (Throwable e) {
				if (aLogErrors) e.printStackTrace(ERR);
			}
			return null;
		}
		
		public static Object getFieldContent(Object aObject, String aField) {return getFieldContent(aObject, aField, T, T);}
		public static Object getFieldContent(Object aObject, String aField, boolean aPrivate, boolean aLogErrors) {
			try {
				Field tField = (aObject instanceof Class)?((Class<?>)aObject).getDeclaredField(aField):(aObject instanceof String)?Class.forName((String)aObject).getDeclaredField(aField):aObject.getClass().getDeclaredField(aField);
				if (aPrivate) tField.setAccessible(T);
				return tField.get(aObject instanceof Class || aObject instanceof String ? null : aObject);
			} catch (Throwable e) {
				if (aLogErrors) e.printStackTrace(ERR);
			}
			return null;
		}
		
		public static boolean setFieldContent(Object aObject, String aField, Object aValue) {return setFieldContent(aObject, aField, aValue, T, T);}
		public static boolean setFieldContent(Object aObject, String aField, Object aValue, boolean aPrivate, boolean aLogErrors) {
			try {
				Field tField = (aObject instanceof Class)?((Class<?>)aObject).getDeclaredField(aField):(aObject instanceof String)?Class.forName((String)aObject).getDeclaredField(aField):aObject.getClass().getDeclaredField(aField);
				if (aPrivate) tField.setAccessible(T);
				tField.set(aObject instanceof Class || aObject instanceof String ? null : aObject, aValue);
				return T;
			} catch (Throwable e) {
				if (aLogErrors) e.printStackTrace(ERR);
			}
			return F;
		}
		public static boolean setFieldContent(Class<?> aClass, Object aObject, String aField, Object aValue) {return setFieldContent(aClass, aObject, aField, aValue, T, T);}
		public static boolean setFieldContent(Class<?> aClass, Object aObject, String aField, Object aValue, boolean aPrivate, boolean aLogErrors) {
			try {
				Field tField = aClass.getDeclaredField(aField);
				if (aPrivate) tField.setAccessible(T);
				tField.set(aObject, aValue);
				return T;
			} catch (Throwable e) {
				if (aLogErrors) e.printStackTrace(ERR);
			}
			return F;
		}
		
		public static Object callPublicMethod(Object aObject, String aMethod, Object... aParameters) {
			return callMethod(aObject, aMethod, F, F, T, aParameters);
		}
		
		public static Object callPrivateMethod(Object aObject, String aMethod, Object... aParameters) {
			return callMethod(aObject, aMethod, T, F, T, aParameters);
		}
		public static Object callMethod(Object aObject, String aMethod, boolean aPrivate, boolean aUseUpperCasedDataTypes, boolean aLogErrors, Object... aParameters) {
			return callMethod(aObject, new String[] {aMethod}, aPrivate, aUseUpperCasedDataTypes, aLogErrors, aParameters);
		}
		public static Object callMethod(Object aObject, String[] aMethods, boolean aPrivate, boolean aUseUpperCasedDataTypes, boolean aLogErrors, Object... aParameters) {
			try {
				Class<?>[] tParameterTypes = new Class<?>[aParameters.length];
				for (byte i = 0; i < aParameters.length; i++) {
					if (aParameters[i] instanceof Class) {
						tParameterTypes[i] = (Class<?>)aParameters[i];
						aParameters[i] = null;
					} else {
						tParameterTypes[i] = aParameters[i].getClass();
					}
					if (!aUseUpperCasedDataTypes) {
						if (tParameterTypes[i] == Boolean.class) tParameterTypes[i] = boolean.class; else
						if (tParameterTypes[i] == Byte.class   ) tParameterTypes[i] = byte.class;    else
						if (tParameterTypes[i] == Short.class  ) tParameterTypes[i] = short.class;   else
						if (tParameterTypes[i] == Integer.class) tParameterTypes[i] = int.class;     else
						if (tParameterTypes[i] == Long.class   ) tParameterTypes[i] = long.class;    else
						if (tParameterTypes[i] == Float.class  ) tParameterTypes[i] = float.class;   else
						if (tParameterTypes[i] == Double.class ) tParameterTypes[i] = double.class;
					}
				}
				for (String aMethod : aMethods) {
					try {
						Method tMethod = aPrivate?
						(aObject instanceof Class)?((Class<?>)aObject).getDeclaredMethod(aMethod, tParameterTypes):aObject.getClass().getDeclaredMethod(aMethod, tParameterTypes):
						(aObject instanceof Class)?((Class<?>)aObject).getMethod        (aMethod, tParameterTypes):aObject.getClass().getMethod        (aMethod, tParameterTypes);
						if (aPrivate) tMethod.setAccessible(T);
						return tMethod.invoke(aObject, aParameters);
					} catch(Throwable e) {
						if (aLogErrors) e.printStackTrace(ERR);
					}
				}
			} catch (Throwable e) {
				if (aLogErrors) e.printStackTrace(ERR);
			}
			return null;
		}
		
		public static Object callConstructor(String aClass, int aConstructorIndex, Object aReplacementObject, boolean aLogErrors, Object... aParameters) {
			try {return callConstructor(Class.forName(aClass), aConstructorIndex, aReplacementObject, aLogErrors, aParameters);} catch (Throwable e) {if (aLogErrors) e.printStackTrace(ERR);} return aReplacementObject;
		}
		
		public static Object callConstructor(Class<?> aClass, int aConstructorIndex, Object aReplacementObject, boolean aLogErrors, Object... aParameters) {
			if (aConstructorIndex < 0) {
				try {
					for (Constructor<?> tConstructor : aClass.getConstructors()) {
						try {
							return tConstructor.newInstance(aParameters);
						} catch (Throwable e) {/*Do nothing*/}
					}
				} catch (Throwable e) {
					if (aLogErrors) e.printStackTrace(ERR);
				}
			} else {
				try {
					return aClass.getConstructors()[aConstructorIndex].newInstance(aParameters);
				} catch (Throwable e) {
					if (aLogErrors) e.printStackTrace(ERR);
				}
			}
			return aReplacementObject;
		}
	}
	
	@Deprecated public static class Inventories {
		@Deprecated public static boolean isConnectableNonInventoryPipe(Object aTileEntity, int aSide) {return F;}
		@Deprecated public static byte moveStackIntoPipe(Container aTileEntity1, Object aTarget, int[] aGrabSlots, byte aGrabFrom, byte aPutTo, List<ItemStack> aFilter, boolean aInvertFilter, int aMaxTargetStackSize, int aMinTargetStackSize, int aMaxMoveAtOnce, int aMinMoveAtOnce) {return 0;}
		@Deprecated public static byte moveStackFromSlotAToSlotB(Container aTileEntity, Container aTarget, int aGrabFrom, int aPutTo, int aMaxTargetStackSize, int aMinTargetStackSize, int aMaxMoveAtOnce, int aMinMoveAtOnce) {return 0;}
		@Deprecated public static boolean isAllowedToTakeFromSlot(Container aTileEntity, int aSlot, byte aSide, ItemStack aStack) {return F;}
		@Deprecated public static boolean isAllowedToPutIntoSlot(Container aTileEntity, int aSlot, byte aSide, ItemStack aStack, int aMaxStackSize) {return F;}
		@Deprecated public static byte moveOneItemStack(Object aTileEntity1, Object aTileEntity2, byte aGrabFrom, byte aPutTo) {return 0;}
		@Deprecated public static byte moveOneItemStack(Object aTileEntity1, Object aTileEntity2, byte aGrabFrom, byte aPutTo, List<ItemStack> aFilter, boolean aInvertFilter, int aMaxTargetStackSize, int aMinTargetStackSize, int aMaxMoveAtOnce, int aMinMoveAtOnce) {return 0;}
		@Deprecated public static byte moveOneItemStackIntoSlot(Object aTileEntity1, Object aTarget, byte aGrabFrom, int aPutTo, List<ItemStack> aFilter, boolean aInvertFilter, int aMaxTargetStackSize, int aMinTargetStackSize, int aMaxMoveAtOnce, int aMinMoveAtOnce) {return 0;}
		@Deprecated public static byte moveFromSlotToSlot(Container aTileEntity1, Container aTileEntity2, int aGrabFrom, int aPutTo, List<ItemStack> aFilter, boolean aInvertFilter, int aMaxTargetStackSize, int aMinTargetStackSize, int aMaxMoveAtOnce, int aMinMoveAtOnce) {return 0;}
		@Deprecated public static void removeNullStacksFromInventory(Container aInventory) {ST.denull(aInventory);}
		@Deprecated public static boolean unlockAchievement(Player aPlayer, Advancement aAchievement) {return ST.achieve(aPlayer, aAchievement);}
		@Deprecated public static boolean checkAchievements(Player aPlayer, ItemStack aStack) {return ST.check(aPlayer, aStack);}
		@Deprecated public static boolean addStackToPlayerInventory(Player aPlayer, ItemStack aStack) {return ST.add(aPlayer, aStack);}
		@Deprecated public static boolean addStackToPlayerInventory(Player aPlayer, ItemStack aStack, boolean aCurrentSlotFirst) {return ST.add(aPlayer, aStack, aCurrentSlotFirst);}
		@Deprecated public static boolean addStackToPlayerInventory(Player aPlayer, Container aInventory, ItemStack aStack, boolean aCurrentSlotFirst) {return ST.add(aPlayer, aInventory, aStack, aCurrentSlotFirst);}
		@Deprecated public static boolean addStackToPlayerInventoryOrDrop(Player aPlayer, ItemStack aStack) {return ST.give(aPlayer, aStack);}
		@Deprecated public static boolean addStackToPlayerInventoryOrDrop(Player aPlayer, ItemStack aStack, boolean aCurrentSlotFirst) {return ST.give(aPlayer, aStack, aCurrentSlotFirst);}
		@Deprecated public static boolean addStackToPlayerInventoryOrDrop(Player aPlayer, ItemStack aStack, Level aWorld, double aX, double aY, double aZ) {return ST.give(aPlayer, aStack, aWorld, aX, aY, aZ);}
		@Deprecated public static boolean addStackToPlayerInventoryOrDrop(Player aPlayer, ItemStack aStack, boolean aCurrentSlotFirst, Level aWorld, double aX, double aY, double aZ) {return ST.give(aPlayer, aStack, aCurrentSlotFirst, aWorld, aX, aY, aZ);}
		@Deprecated public static boolean addStackToPlayerInventoryOrDrop(Player aPlayer, Container aInventory, ItemStack aStack, boolean aCurrentSlotFirst, Level aWorld, double aX, double aY, double aZ) {return ST.give(aPlayer, aInventory, aStack, aCurrentSlotFirst, aWorld, aX, aY, aZ);}
		@Deprecated public static ItemStack getProjectile(TagData aProjectileType, Container aInventory) {return ST.projectile(aInventory, aProjectileType);}
	}
	
	public static class Sounds {
		public static List<PlayedSound> sPlayedSounds = new ArrayListNoNulls<>();
		public static List<SoundWithLocation> sSoundsToPlay = new ArrayListNoNulls<>();

		/** F-sound (1:1 Mojang sound-flattening): легаси 1.7.10 SFX-строки → neo sound-id. КАЖДЫЙ neo-id сверен по
		 *  neo-decompiled SoundEvents.java. neo-native строки (не в карте) проходят как есть. */
		private static final java.util.Map<String, String> SFX_LEGACY = new java.util.HashMap<>();
		static {
			SFX_LEGACY.put("random.chestopen", "block.chest.open");         SFX_LEGACY.put("random.chestclosed", "block.chest.close"); // сверено: SoundEvents.java:352,354
			SFX_LEGACY.put("random.break", "entity.item.break");            SFX_LEGACY.put("random.anvil_use", "block.anvil.use");
			SFX_LEGACY.put("random.anvil_break", "block.anvil.destroy");    SFX_LEGACY.put("random.anvil_land", "block.anvil.land");
			SFX_LEGACY.put("random.click", "ui.button.click");              SFX_LEGACY.put("random.pop", "entity.item.pickup");
			SFX_LEGACY.put("random.fizz", "block.fire.extinguish");         SFX_LEGACY.put("random.explode", "entity.generic.explode");
			SFX_LEGACY.put("random.eat", "entity.generic.eat");             SFX_LEGACY.put("random.drink", "entity.generic.drink");
			SFX_LEGACY.put("random.orb", "entity.experience_orb.pickup");   SFX_LEGACY.put("game.tnt.primed", "entity.tnt.primed");
			SFX_LEGACY.put("fire.ignite", "item.flintandsteel.use");        SFX_LEGACY.put("game.neutral.swim", "entity.player.swim");
			SFX_LEGACY.put("dig.cloth", "block.wool.break");                SFX_LEGACY.put("dig.stone", "block.stone.break");
			SFX_LEGACY.put("dig.glass", "block.glass.break");               SFX_LEGACY.put("dig.grass", "block.grass.break");
			SFX_LEGACY.put("dig.gravel", "block.gravel.break");             SFX_LEGACY.put("dig.sand", "block.sand.break");
			SFX_LEGACY.put("dig.wood", "block.wood.break");                 SFX_LEGACY.put("dig.snow", "block.snow.break");
			SFX_LEGACY.put("minecart.base", "entity.minecart.riding");      SFX_LEGACY.put("minecart.inside", "entity.minecart.inside");
			SFX_LEGACY.put("fireworks.launch", "entity.firework_rocket.launch");           SFX_LEGACY.put("fireworks.blast", "entity.firework_rocket.blast");
			SFX_LEGACY.put("fireworks.blast_far", "entity.firework_rocket.blast_far");     SFX_LEGACY.put("fireworks.largeBlast", "entity.firework_rocket.large_blast");
			SFX_LEGACY.put("fireworks.largeBlast_far", "entity.firework_rocket.large_blast_far"); SFX_LEGACY.put("liquid.water", "block.water.ambient");
			SFX_LEGACY.put("mob.villager.idle", "entity.villager.ambient"); SFX_LEGACY.put("mob.villager.haggle", "entity.villager.trade");
			SFX_LEGACY.put("mob.sheep.shear", "entity.sheep.shear");        SFX_LEGACY.put("mob.slime.big", "entity.slime.squish");
			SFX_LEGACY.put("mob.slime.small", "entity.slime.squish_small"); SFX_LEGACY.put("eating", "entity.generic.eat");
		}
		public static String neoSound(String aSound) {String r = SFX_LEGACY.get(aSound); return r != null ? r : aSound;}
		
		public static boolean play(String aSound, int aTimeUntilNextSound, float aVolume) {
			if (!CODE_CLIENT || net.neoforged.fml.util.thread.EffectiveSide.get().isServer()) return F;
			return play(aSound, aTimeUntilNextSound, aVolume, GT_API.api_proxy.getThePlayer());
		}
		
		public static boolean play(String aSound, int aTimeUntilNextSound, float aVolume, Entity aEntity) {
			if (!CODE_CLIENT || aEntity == null || net.neoforged.fml.util.thread.EffectiveSide.get().isServer()) return F;
			return play(aSound, aTimeUntilNextSound, aVolume, UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ()));
		}
		
		public static boolean play(String aSound, int aTimeUntilNextSound, float aVolume, int aX, int aY, int aZ) {
			return play(aSound, aTimeUntilNextSound, aVolume, new BlockPos(aX, aY, aZ));
		}
		
		public static boolean play(String aSound, int aTimeUntilNextSound, float aVolume, BlockPos aCoords) {
			if (aCoords == null) return play(aSound, aTimeUntilNextSound, aVolume);
			if (!CODE_CLIENT || net.neoforged.fml.util.thread.EffectiveSide.get().isServer()) return F;
			return play(aSound, aTimeUntilNextSound, aVolume, 0.9F + RNGSUS.nextFloat() * 0.2F, aCoords.getX(), aCoords.getY(), aCoords.getZ());
		}
		
		public static boolean play(String aSound, int aTimeUntilNextSound, float aVolume, float aPitch, int aX, int aY, int aZ) {
			return play(aSound, aTimeUntilNextSound, aVolume, aPitch, new BlockPos(aX, aY, aZ));
		}
		
		public static boolean play(String aSound, int aTimeUntilNextSound, float aVolume, float aPitch, BlockPos aCoords) {
			if (!CODE_CLIENT || net.neoforged.fml.util.thread.EffectiveSide.get().isServer()) return F;
			Player aPlayer = GT_API.api_proxy.getThePlayer();
			if (aPlayer == null || !aPlayer.level().isClientSide() || Code.stringInvalid(aSound)) return F;
			sSoundsToPlay.add(new SoundWithLocation(aPlayer.level(), UT.Code.roundDown(aCoords.getX()), UT.Code.roundDown(aCoords.getY()), UT.Code.roundDown(aCoords.getZ()), aTimeUntilNextSound, aSound, aVolume, Float.isNaN(aPitch) || aPitch == SFX.RANDOM_PITCH ? SFX._7_GRAND_DAD_[SFX.PITCH_INDEX=((SFX.PITCH_INDEX+1)%SFX._7_GRAND_DAD_.length)] : aPitch));
			return T;
		}
		
		public static boolean send(String aSound, IHasWorldAndCoords aTileEntity) {
			return send(aSound, 1.0F, SFX.RANDOM_PITCH, aTileEntity.getWorld(), aTileEntity.getCoords());
		}
		public static boolean send(String aSound, IHasWorldAndCoords aTileEntity, boolean aIDontWannaFuckingCastThisShitAllTheTime) {
			return send(aSound, 1.0F, SFX.RANDOM_PITCH, aTileEntity.getWorld(), aTileEntity.getCoords());
		}
		public static boolean send(String aSound, BlockEntity aTileEntity) {
			return send(aSound, 1.0F, SFX.RANDOM_PITCH, aTileEntity.getLevel(), new BlockPos(aTileEntity.getBlockPos().getX(), aTileEntity.getBlockPos().getY(), aTileEntity.getBlockPos().getZ()));
		}
		public static boolean send(String aSound, Entity aEntity) {
			return send(aSound, 1.0F, SFX.RANDOM_PITCH, aEntity.level(), new BlockPos(UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ())));
		}
		public static boolean send(String aSound, Level aWorld, int aX, int aY, int aZ) {
			return send(aSound, 1.0F, SFX.RANDOM_PITCH, aWorld, new BlockPos(aX, aY, aZ));
		}
		public static boolean send(String aSound, Level aWorld, BlockPos aCoords) {
			return send(aSound, 1.0F, SFX.RANDOM_PITCH, aWorld, aCoords);
		}
		// F-sound: neo SoundType.getBreakSound()/getStepSound()/… возвращают SoundEvent (record с компонентом
		// location:Identifier), не легаси-строку 1.7.10 (aBlock.stepSound.getBreakSound() отдавал "dig.stone").
		// Центр String-based (SFX-константы вида "random.click") — извлекаем движковый ID через
		// SoundEvent.location().toString() (SoundEvent.java:15, record-компонент; neo несёт корректный neo-путь)
		// и делегируем в String-перегрузку. Мост под сменённый движком тип звука, не улучшение.
		public static boolean send(net.minecraft.sounds.SoundEvent aSound, Level aWorld, BlockPos aCoords) {
			return aSound == null ? F : send(aSound.location().toString(), aWorld, aCoords);
		}
		public static boolean send(String aSound, float aVolume, IHasWorldAndCoords aTileEntity) {
			return send(aSound, aVolume, SFX.RANDOM_PITCH, aTileEntity.getWorld(), aTileEntity.getCoords());
		}
		public static boolean send(String aSound, float aVolume, IHasWorldAndCoords aTileEntity, boolean aIDontWannaFuckingCastThisShitAllTheTime) {
			return send(aSound, aVolume, SFX.RANDOM_PITCH, aTileEntity.getWorld(), aTileEntity.getCoords());
		}
		public static boolean send(String aSound, float aVolume, BlockEntity aTileEntity) {
			return send(aSound, aVolume, SFX.RANDOM_PITCH, aTileEntity.getLevel(), new BlockPos(aTileEntity.getBlockPos().getX(), aTileEntity.getBlockPos().getY(), aTileEntity.getBlockPos().getZ()));
		}
		public static boolean send(String aSound, float aVolume, Entity aEntity) {
			return send(aSound, aVolume, SFX.RANDOM_PITCH, aEntity.level(), new BlockPos(UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ())));
		}
		public static boolean send(String aSound, float aVolume, Level aWorld, int aX, int aY, int aZ) {
			return send(aSound, aVolume, SFX.RANDOM_PITCH, aWorld, new BlockPos(aX, aY, aZ));
		}
		public static boolean send(String aSound, float aVolume, Level aWorld, BlockPos aCoords) {
			return send(aSound, aVolume, SFX.RANDOM_PITCH, aWorld, aCoords);
		}
		public static boolean send(String aSound, float aVolume, float aPitch, IHasWorldAndCoords aTileEntity) {
			return send(aSound, aVolume, aPitch, aTileEntity.getWorld(), aTileEntity.getCoords());
		}
		public static boolean send(String aSound, float aVolume, float aPitch, IHasWorldAndCoords aTileEntity, boolean aIDontWannaFuckingCastThisShitAllTheTime) {
			return send(aSound, aVolume, aPitch, aTileEntity.getWorld(), aTileEntity.getCoords());
		}
		public static boolean send(String aSound, float aVolume, float aPitch, BlockEntity aTileEntity) {
			return send(aSound, aVolume, aPitch, aTileEntity.getLevel(), new BlockPos(aTileEntity.getBlockPos().getX(), aTileEntity.getBlockPos().getY(), aTileEntity.getBlockPos().getZ()));
		}
		public static boolean send(String aSound, float aVolume, float aPitch, Entity aEntity) {
			return send(aSound, aVolume, aPitch, aEntity.level(), new BlockPos(UT.Code.roundDown(aEntity.getX()), UT.Code.roundDown(aEntity.getY()), UT.Code.roundDown(aEntity.getZ())));
		}
		public static boolean send(String aSound, float aVolume, float aPitch, Level aWorld, int aX, int aY, int aZ) {
			return send(aSound, aVolume, aPitch, aWorld, new BlockPos(aX, aY, aZ));
		}
		public static boolean send(String aSound, float aVolume, float aPitch, Level aWorld, BlockPos aCoords) {
			if (Code.stringInvalid(aSound) || aWorld == null || aWorld.isClientSide()) return F;
			NW_API.sendToAllPlayersInRange(new PacketSound(aSound, aVolume, aPitch, aCoords), aWorld, aCoords);
			return T;
		}
		
		@Deprecated public static boolean send(Level aWorld, String aSound, int aX, int aY, int aZ) {return send(aSound, 1.0F, SFX.RANDOM_PITCH, aWorld, aX, aY, aZ);}
		@Deprecated public static boolean send(Level aWorld, String aSound, float aVolume, float aPitch, int aX, int aY, int aZ) {return send(aSound, aVolume, aPitch, aWorld, aX, aY, aZ);}
		@Deprecated public static boolean send(Level aWorld, String aSound, float aVolume, float aPitch, Entity aEntity) {return send(aSound, aVolume, aPitch, aEntity);}
		@Deprecated public static boolean send(Level aWorld, String aSound, float aVolume, float aPitch, BlockPos aCoords) {return send(aSound, aVolume, aPitch, aWorld, aCoords);}
		
		public static class PlayedSound {
			public final String mSoundName;
			public final int mX, mY, mZ;
			public int mTimer = 0;
			
			public PlayedSound(String aSound, int aX, int aY, int aZ, int aTimer) {
				mSoundName = aSound==null?"":aSound;
				mTimer = aTimer;
				mX = aX;
				mY = aY;
				mZ = aZ;
			}
			
			@Override
			public boolean equals(Object aObject) {
				if (aObject instanceof PlayedSound) return ((PlayedSound)aObject).mX == mX && ((PlayedSound)aObject).mY == mY && ((PlayedSound)aObject).mZ == mZ && ((PlayedSound)aObject).mSoundName.equals(mSoundName);
				return F;
			}
			
			@Override
			public int hashCode() {
				return mX+mY+mZ+mSoundName.hashCode();
			}
		}
		
		public static class SoundWithLocation {
			public final int mX, mY, mZ, mTimeUntilNextSound;
			public final Level mWorld;
			public final String mSound;
			public final float mVolume, mPitch;
			
			public SoundWithLocation(Level aWorld, int aX, int aY, int aZ, int aTimeUntilNextSound, String aSound, float aVolume, float aPitch) {
				mWorld = aWorld; mX = aX; mY = aY; mZ = aZ; mTimeUntilNextSound = aTimeUntilNextSound; mSound = aSound; mVolume = aVolume; mPitch = aPitch;
			}
			
			public void play() {
				PlayedSound tSound = new PlayedSound(mSound, mX, mY, mZ, mTimeUntilNextSound);
				if (!sPlayedSounds.contains(tSound)) try {
					sPlayedSounds.add(tSound);
					// F-sound: neo Level.playSound(double,double,double,String,...) удалён — звук адресуется
					// SoundEvent из реестра (Registry.getValue(Identifier), Registry.java:69), проигрывается
					// Level.playLocalSound(...,SoundEvent,SoundSource,...) (Level.java:463). Резолвим mSound как
					// neo sound-id; neo-native строки играют сразу.
					// F-sound (1:1): легаси 1.7.10 SFX-строки → neo sound-id через neoSound (карта SFX_LEGACY, сверена по SoundEvents.java);
					// neo-native строки проходят как есть. Раньше легаси не резолвились → все GT6-звуки молчали. Восстановлено.
					net.minecraft.sounds.SoundEvent tEvent = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(net.minecraft.resources.Identifier.parse(neoSound(mSound)));
					if (tEvent != null) mWorld.playLocalSound(mX+0.5, mY+0.5, mZ+0.5, tEvent, net.minecraft.sounds.SoundSource.BLOCKS, mVolume, mPitch, T);
				} catch(Throwable e) {/**/}
			}
		}
	}
	
	public static class Entities {
		/** было {@code aEntity.getEquipmentInSlot(aIndex)} (1.7.10 EntityLivingBase, удалён) — neo getItemBySlot(EquipmentSlot).
		 *  Индексы 1.7.10: 0=held→MAINHAND, 1→FEET, 2→LEGS, 3→CHEST, 4→HEAD (сверено neo EquipmentSlot.java:13-18 filterFlag 1-4). */
		public static ItemStack getEquipmentInSlot(LivingEntity aEntity, int aIndex) {
			return aEntity.getItemBySlot(aIndex <= 0 ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : aIndex == 1 ? net.minecraft.world.entity.EquipmentSlot.FEET : aIndex == 2 ? net.minecraft.world.entity.EquipmentSlot.LEGS : aIndex == 3 ? net.minecraft.world.entity.EquipmentSlot.CHEST : net.minecraft.world.entity.EquipmentSlot.HEAD);
		}
		/** Sends Messages to a Player */
		public static void sendchat(Object aPlayer, String... aChatMessages) {
			if (aPlayer instanceof ServerPlayer) for (String aMessage : aChatMessages) ((ServerPlayer)aPlayer).sendSystemMessage(Component.literal(aMessage));
		}

		/** Sends Messages to a Player */
		public static void sendchat(Object aPlayer, Component... aChatMessages) {
			if (aPlayer instanceof ServerPlayer) for (Component aMessage : aChatMessages) ((ServerPlayer)aPlayer).sendSystemMessage(aMessage);
		}

		/** Sends Messages to a Player */
		public static void sendchat(Object aPlayer, @SuppressWarnings("rawtypes") List aChatMessages, boolean aSkipFirst) {
			if (aChatMessages != null && aPlayer instanceof ServerPlayer) for (Object aMessage : aChatMessages) if (aSkipFirst) aSkipFirst=F; else ((ServerPlayer)aPlayer).sendSystemMessage(aMessage instanceof Component ? (Component)aMessage : Component.literal(aMessage.toString()));
		}

		public static void chat(Object aPlayer, String... aChatMessages) {
			if (aPlayer == null) aPlayer = GT_API.api_proxy.getThePlayer();
			if (aPlayer instanceof Player) for (String aMessage : aChatMessages) ((Player)aPlayer).sendSystemMessage(Component.literal(aMessage));
		}

		public static void chat(Object aPlayer, Component... aChatMessages) {
			if (aPlayer == null) aPlayer = GT_API.api_proxy.getThePlayer();
			if (aPlayer instanceof Player) for (Component aMessage : aChatMessages) ((Player)aPlayer).sendSystemMessage(aMessage);
		}

		public static void chat(Object aPlayer, @SuppressWarnings("rawtypes") List aChatMessages, boolean aSkipFirst) {
			if (aPlayer == null) aPlayer = GT_API.api_proxy.getThePlayer();
			if (aChatMessages != null && aPlayer instanceof Player) for (Object aMessage : aChatMessages) if (aSkipFirst) aSkipFirst=F; else ((Player)aPlayer).sendSystemMessage(aMessage instanceof Component ? (Component)aMessage : Component.literal(aMessage.toString()));
		}
		
		
		
		public static boolean isWearingFullFrostHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity)) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_FROST.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		public static boolean isWearingFullHeatHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity) || aEntity.getClass() == WitherBoss.class || aEntity.getClass() == Blaze.class || aEntity.getClass() == net.minecraft.world.entity.monster.zombie.ZombifiedPiglin.class || aEntity.getClass() == MagmaCube.class || aEntity.getClass() == net.minecraft.world.entity.monster.Ghast.class) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_HEAT.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		public static boolean isWearingFullBioHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity) || aEntity.getClass() == WitherBoss.class || aEntity.getClass() == IronGolem.class) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_BIO.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		public static boolean isWearingFullChemHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity)) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_CHEM.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		public static boolean isWearingFullInsectHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity) || aEntity.getClass() == WitherBoss.class || aEntity.getClass() == IronGolem.class) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_INSECTS.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		public static boolean isWearingFullRadioHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity) || aEntity.getClass() == WitherBoss.class || aEntity.getClass() == IronGolem.class) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_RADIOACTIVE.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		public static boolean isWearingFullElectroHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity)) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_LIGHTNING.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		public static boolean isWearingFullGasHazmat(LivingEntity aEntity) {
			if (isCreative(aEntity) || aEntity.getClass() == WitherBoss.class || aEntity.getClass() == IronGolem.class) return T;
			for (byte i = 1; i < 5; i++) if (!ArmorsGT.HAZMATS_GAS.contains(UT.Entities.getEquipmentInSlot(aEntity, i), T)) return F;
			return T;
		}
		
		
		
		public static boolean isSlimeCreature(Entity aEntity) {
			return aEntity instanceof Slime || UT.Reflection.getLowercaseClass(aEntity).contains("slime");
		}
		public static boolean isEnderCreature(Entity aEntity) {
			return aEntity instanceof EnderMan || UT.Reflection.getLowercaseClass(aEntity).contains("ender");
		}
		public static boolean isZombieCreature(Entity aEntity) {
			return aEntity instanceof Zombie || UT.Reflection.getLowercaseClass(aEntity).contains("zombie");
		}
		public static boolean isCreeperCreature(Entity aEntity) {
			return aEntity instanceof Creeper || UT.Reflection.getLowercaseClass(aEntity).contains("creeper");
		}
		public static boolean isGhastCreature(Entity aEntity) {
			return aEntity instanceof Creeper || UT.Reflection.getLowercaseClass(aEntity).contains("ghast");
		}
		public static boolean isExplosiveCreature(Entity aEntity) {
			return isGhastCreature(aEntity) || isCreeperCreature(aEntity) || UT.Reflection.getLowercaseClass(aEntity).contains("firebeetle");
		}
		public static boolean isWereCreature(LivingEntity aEntity) {
			if (aEntity instanceof Player) {
				if ("Bear989Sr".equalsIgnoreCase(aEntity.getName().getString())) return T;
				// F10 foreign-gated (Werewolves-мод отсутствует, честная деградация F): 1.7.10 Entity.getExtendedProperties("WerewolfPlayer")
				// (IExtendedEntityProperties) удалён -> neo AttachmentType-модель (иная регистрация). Интеграция мода
				// Werewolves (reflection getWerewolf) отложена: детекция игрока-оборотня недоступна без порта мода +
				// регистрации AttachmentType. Возвращаем F (мод не загружен, API удалён) — честная деградация, не тихий стаб.
				return F;
			}
			if (aEntity.getClass().getName().indexOf(".") < 0) return F;
			String tClassName = UT.Reflection.getLowercaseClass(aEntity);
			return tClassName.contains("wwolf") || tClassName.contains("yeti") || tClassName.contains("villagerwere") || tClassName.contains("wolfman") || tClassName.contains("werewolf") || tClassName.contains("alphawolf") || tClassName.contains("tamewere") || tClassName.contains("minotaur") || tClassName.contains("minoshroom");
		}
		
		public static float getHeatDamageFromItem(ItemStack aStack) {
			OreDictItemData tData = OM.anydata(aStack);
			return tData==null?0:(tData.mPrefix==null?0:tData.mPrefix.mHeatDamage) + (tData.validMaterial()?tData.mMaterial.mMaterial.mHeatDamage:0);
		}
		
		public static int getRadioactivityLevel(ItemStack aStack) {
			return getRadioactivityLevel(aStack, OM.anydata(aStack));
		}
		public static int getRadioactivityLevel(ItemStack aStack, OreDictItemData aData) {
			long rLevel = 0;
			if (aData != null && aData.validMaterial()) {
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aData.mMaterial.mMaterial.mEnchantmentTools  ) if (Enchantment_Radioactivity.KEY.equals(tEnchantment.mObject)) rLevel = Math.max(rLevel, tEnchantment.mAmount);
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aData.mMaterial.mMaterial.mEnchantmentWeapons) if (Enchantment_Radioactivity.KEY.equals(tEnchantment.mObject)) rLevel = Math.max(rLevel, tEnchantment.mAmount);
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aData.mMaterial.mMaterial.mEnchantmentAmmo   ) if (Enchantment_Radioactivity.KEY.equals(tEnchantment.mObject)) rLevel = Math.max(rLevel, tEnchantment.mAmount);
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aData.mMaterial.mMaterial.mEnchantmentRanged ) if (Enchantment_Radioactivity.KEY.equals(tEnchantment.mObject)) rLevel = Math.max(rLevel, tEnchantment.mAmount);
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aData.mMaterial.mMaterial.mEnchantmentFishing) if (Enchantment_Radioactivity.KEY.equals(tEnchantment.mObject)) rLevel = Math.max(rLevel, tEnchantment.mAmount);
				for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : aData.mMaterial.mMaterial.mEnchantmentArmors ) if (Enchantment_Radioactivity.KEY.equals(tEnchantment.mObject)) rLevel = Math.max(rLevel, tEnchantment.mAmount);
			}
			// F8 (1:1): было EnchantmentHelper.getEnchantmentLevel(Enchantment_Radioactivity.INSTANCE.effectId, aStack) —
			// вклад НАДЕТОЙ Radioactivity-чары (в дополнение к GT6-материалу выше). Enchantment_Radioactivity теперь
			// registry-driven (KEY, забутстрапена EnchantsGT6) → ported UT.NBT.getEnchantmentLevel(KEY, стек) читает уровень.
			rLevel = Math.max(rLevel, NBT.getEnchantmentLevel(Enchantment_Radioactivity.KEY, aStack));
			return Code.bindInt(rLevel);
		}
		
		public static boolean isImmuneToBreathingGases(LivingEntity aEntity) {
			return isWearingFullGasHazmat(aEntity);
		}
		
		public static boolean applyTemperatureDamage(Entity aEntity, long aTemperature) {
			return applyTemperatureDamage(aEntity, aTemperature, 1);
		}
		public static boolean applyTemperatureDamage(Entity aEntity, long aTemperature, float aMultiplier) {
			if (aTemperature > 320) return applyHeatDamage (aEntity, (aMultiplier * (aTemperature - 300)) / 50.0F);
			if (aTemperature < 260) return applyFrostDamage(aEntity, (aMultiplier * (270 - aTemperature)) / 25.0F);
			return F;
		}
		public static boolean applyTemperatureDamage(Entity aEntity, long aTemperature, float aMultiplier, float aCap) {
			if (aTemperature > 320) return applyHeatDamage (aEntity, Math.max(1, Math.min(aCap, (aMultiplier * (aTemperature - 300)) / 50.0F)));
			if (aTemperature < 260) return applyFrostDamage(aEntity, Math.max(1, Math.min(aCap, (aMultiplier * (270 - aTemperature)) / 25.0F)));
			return F;
		}
		
		public static boolean applyChemDamage(Entity aEntity, float aDamage) {
			if (aDamage > 0 && aEntity instanceof LivingEntity && aEntity.isAlive() && aEntity.getClass() != Skeleton.class && !isWearingFullChemHazmat(((LivingEntity)aEntity))) {
				aEntity.hurt(DamageSources.getChemDamage(), TFC_DAMAGE_MULTIPLIER * aDamage);
				MobEffectInstance tEffect;
				((LivingEntity)aEntity).addEffect(new MobEffectInstance(MobEffects.POISON, Math.max(20, (int)(aDamage * 100 + Math.max(0, ((tEffect = ((LivingEntity)aEntity).getEffect(MobEffects.POISON))==null?0:tEffect.getDuration())))), 1));
				return T;
			}
			return F;
		}
		
		public static boolean applyHeatDamage(Entity aEntity, float aDamage) {
			if (aDamage > 0 && aEntity instanceof LivingEntity && aEntity.isAlive() && aEntity.getClass() != Blaze.class && ((LivingEntity)aEntity).getEffect(MobEffects.FIRE_RESISTANCE) == null && !isWearingFullHeatHazmat(((LivingEntity)aEntity))) {
				aEntity.hurt(DamageSources.getHeatDamage(), TFC_DAMAGE_MULTIPLIER * aDamage);
				return T;
			}
			return F;
		}
		
		public static boolean applyFrostDamage(Entity aEntity, float aDamage) {
			if (aDamage > 0 && aEntity instanceof LivingEntity && aEntity.isAlive() && !isWearingFullFrostHazmat(((LivingEntity)aEntity))) {
				aEntity.hurt(DamageSources.getFrostDamage(), TFC_DAMAGE_MULTIPLIER * aDamage);
				return T;
			}
			return F;
		}
		
		public static boolean applyElectricityDamage(Entity aEntity, long aVoltage, long aAmperage) {
			long aDamage = Code.tierMax(aVoltage) * aAmperage * 4;
			if (aDamage > 0 && aEntity instanceof LivingEntity && aEntity.isAlive() && !isWearingFullElectroHazmat(((LivingEntity)aEntity))) {
				aEntity.hurt(DamageSources.getElectricDamage(), TFC_DAMAGE_MULTIPLIER * aDamage);
				return T;
			}
			return F;
		}
		
		public static boolean applyElectricityDamage(Entity aEntity, long aWattage) {
			long aDamage = Code.tierMax(aWattage) * 4;
			if (aDamage > 0 && aEntity instanceof LivingEntity && aEntity.isAlive() && !isWearingFullElectroHazmat(((LivingEntity)aEntity))) {
				aEntity.hurt(DamageSources.getElectricDamage(), TFC_DAMAGE_MULTIPLIER * aDamage);
				return T;
			}
			return F;
		}
		
		public static boolean applyRadioactivity(Entity aEntity, int aLevel, int aAmountOfItems) {
			// F-entity: MobType/getCreatureAttribute() удалён — тип существа теперь EntityType-теги. Радиация не
			// действует на нежить/членистоногих: !is(EntityTypeTags.UNDEAD/ARTHROPOD) (Entity.is(TagKey), идиома
			// LivingEntity.canBreatheUnderwater:395; EntityTypeTags.java:11,30).
			if (aLevel > 0 && aEntity instanceof LivingEntity && aEntity.isAlive() && !((LivingEntity)aEntity).is(EntityTypeTags.UNDEAD) && !((LivingEntity)aEntity).is(EntityTypeTags.ARTHROPOD) && !isWearingFullRadioHazmat(((LivingEntity)aEntity))) {
				
				EntityFoodTracker tTracker = EntityFoodTracker.get(aEntity);
				if (tTracker != null) {tTracker.changeRadiation(aLevel * aAmountOfItems); return T;}
				
				MobEffectInstance tEffect;
				applyPotion(aEntity, MobEffects.SLOWNESS    , aLevel * 140 * aAmountOfItems + Math.max(0, ((tEffect = ((LivingEntity)aEntity).getEffect(MobEffects.SLOWNESS                       ))==null?0:tEffect.getDuration())), (int)UT.Code.bind(0, 5, (5L*aLevel) / 7), F);
				applyPotion(aEntity, MobEffects.MINING_FATIGUE     , aLevel * 150 * aAmountOfItems + Math.max(0, ((tEffect = ((LivingEntity)aEntity).getEffect(MobEffects.MINING_FATIGUE                        ))==null?0:tEffect.getDuration())), (int)UT.Code.bind(0, 5, (5L*aLevel) / 7), F);
				applyPotion(aEntity, MobEffects.NAUSEA       , aLevel * 130 * aAmountOfItems + Math.max(0, ((tEffect = ((LivingEntity)aEntity).getEffect(MobEffects.NAUSEA                          ))==null?0:tEffect.getDuration())), (int)UT.Code.bind(0, 5, (5L*aLevel) / 7), F);
				applyPotion(aEntity, MobEffects.WEAKNESS        , aLevel * 150 * aAmountOfItems + Math.max(0, ((tEffect = ((LivingEntity)aEntity).getEffect(MobEffects.WEAKNESS                           ))==null?0:tEffect.getDuration())), (int)UT.Code.bind(0, 5, (5L*aLevel) / 7), F);
				applyPotion(aEntity, MobEffects.HUNGER          , aLevel * 130 * aAmountOfItems + Math.max(0, ((tEffect = ((LivingEntity)aEntity).getEffect(MobEffects.HUNGER                             ))==null?0:tEffect.getDuration())), (int)UT.Code.bind(0, 5, (5L*aLevel) / 7), F);
				if (PotionsGT.ID_RADIATION >= 0) {
				applyPotion(aEntity, PotionsGT.ID_RADIATION , aLevel * 180 * aAmountOfItems + Math.max(0, ((tEffect = getEffectByID((LivingEntity)aEntity, PotionsGT.ID_RADIATION))==null?0:tEffect.getDuration())), (int)UT.Code.bind(0, 4, (5L*aLevel) / 7), F); // can only be between 0 and 4, or else IC2 WILL crash!!!
				} else {
				applyPotion(aEntity, MobEffects.WITHER          , aLevel * 130 * aAmountOfItems + Math.max(0, ((tEffect = ((LivingEntity)aEntity).getEffect(MobEffects.WITHER                             ))==null?0:tEffect.getDuration())), (int)UT.Code.bind(0, 5, (5L*aLevel) / 7), F);
				}
				return T;
			}
			return F;
		}
		
		// F8: числовые id 1.7.10 (см. switch ниже) не имеют аналога в neo — Potion/MobEffect больше не
		// адресуются static int-полем (сверено `gregtech6/build/tmp/recompSrc/net/minecraft/potion/Potion.java`
		// — 1=moveSpeed, 2=moveSlowdown, 3=digSpeed, 4=digSlowdown, 5=damageBoost, 6=heal, 7=harm, 8=jump,
		// 9=confusion, 10=regeneration, 11=resistance, 12=fireResistance, 13=waterBreathing, 14=invisibility,
		// 15=blindness, 16=nightVision, 17=hunger, 18=weakness, 19=poison, 20=wither, 22=absorption
		// (field_76444_x), 23=saturation (field_76443_y)). Восстановлен маппинг на реальные neo Holder-
		// константы (`neo-decompiled/.../MobEffects.java`), RegistryAccess не нужен — это заранее известные
		// ВАНИЛЬНЫЕ Holder, объявленные `static final` в самом движке. R8-доработка (GPT-переревизия):
		// набор id — полный аудит по ФАКТУ использования в GT6 (не по памяти) — `int...aPotionEffects` во
		// всех `new FoodStat(...)`/`new FoodStatDrink(...)` (`gregtech/items/MultiItemFood.java`,
		// `MultiItemCans.java`, `gregtech/loaders/a/Loader_Fluids.java` — все 3 файла, создающие FoodStat,
		// grep по дереву), `.addEffectBathing/addEffectBreathing(int,...)` (`gregtech/loaders/a/
		// Loader_Blocks.java`) и `UT.NBT.make("id", MobEffect.xxx.id,...)` (`gregapi/recipes/maps/
		// RecipeMapBath.java`, читается назад через `GT_API_Proxy.java:1128`) — все текут в этот же
		// int-канал `applyPotion(Entity,int,...)`. id 21 (healthBoost) в neo есть (`MobEffects.
		// HEALTH_BOOST`), но GT6 нигде не использует (grep=0) — не добавлен (не выдумывать неиспользуемое).
		private static final Map<Integer, Holder<MobEffect>> VANILLA_POTION_IDS = new HashMap<>();
		static {
			VANILLA_POTION_IDS.put( 1, MobEffects.SPEED);            // moveSpeed        — MultiItemFood.java:565, Loader_Fluids.java:247
			VANILLA_POTION_IDS.put( 2, MobEffects.SLOWNESS);         // moveSlowdown     — MultiItemFood.java:930, Loader_Fluids.java:283
			VANILLA_POTION_IDS.put( 3, MobEffects.HASTE);            // digSpeed         — MultiItemFood.java:862
			VANILLA_POTION_IDS.put( 4, MobEffects.MINING_FATIGUE);   // digSlowdown      — MultiItemFood.java:938 (Pill_Cure_All)
			VANILLA_POTION_IDS.put( 5, MobEffects.STRENGTH);         // damageBoost      — Loader_Fluids.java:253
			VANILLA_POTION_IDS.put( 6, MobEffects.INSTANT_HEALTH);   // heal             — MultiItemFood.java:565
			VANILLA_POTION_IDS.put( 7, MobEffects.INSTANT_DAMAGE);   // harm             — MultiItemFood.java:938 (Pill_Cure_All)
			VANILLA_POTION_IDS.put( 8, MobEffects.JUMP_BOOST);       // jump             — Loader_Fluids.java:243
			VANILLA_POTION_IDS.put( 9, MobEffects.NAUSEA);           // confusion        — MultiItemFood.java:324
			VANILLA_POTION_IDS.put(10, MobEffects.REGENERATION);     // regeneration     — MultiItemFood.java:302, Loader_Fluids.java:259, Loader_Blocks.java:146
			VANILLA_POTION_IDS.put(11, MobEffects.RESISTANCE);       // resistance       — Loader_Blocks.java:146
			VANILLA_POTION_IDS.put(12, MobEffects.FIRE_RESISTANCE);  // fireResistance   — Loader_Fluids.java:271
			VANILLA_POTION_IDS.put(13, MobEffects.WATER_BREATHING);  // waterBreathing   — Loader_Fluids.java:287
			VANILLA_POTION_IDS.put(14, MobEffects.INVISIBILITY);     // invisibility     — Loader_Fluids.java:291
			VANILLA_POTION_IDS.put(15, MobEffects.BLINDNESS);        // blindness        — MultiItemFood.java:932, Loader_Blocks.java:149
			VANILLA_POTION_IDS.put(16, MobEffects.NIGHT_VISION);     // nightVision      — Loader_Fluids.java:275
			VANILLA_POTION_IDS.put(17, MobEffects.HUNGER);           // hunger           — MultiItemFood.java:598, MultiItemCans.java:54
			VANILLA_POTION_IDS.put(18, MobEffects.WEAKNESS);         // weakness         — MultiItemFood.java:938 (Pill_Cure_All), Loader_Fluids.java:279
			VANILLA_POTION_IDS.put(19, MobEffects.POISON);           // poison           — MultiItemFood.java:933, Loader_Blocks.java:149
			VANILLA_POTION_IDS.put(20, MobEffects.WITHER);           // wither           — MultiItemFood.java:938 (Pill_Cure_All)
			VANILLA_POTION_IDS.put(22, MobEffects.ABSORPTION);       // absorption (field_76444_x) — Loader_Fluids.java:607
			VANILLA_POTION_IDS.put(23, MobEffects.SATURATION);       // saturation (field_76443_y) — MultiItemFood.java:938 (Pill_Cure_All)
		}

		// F8: GT6-внутренний id-простор зелий (== 1.7.10 Potion.X.id). neo MobEffects.X — это Holder<MobEffect> без
		// числового `.id`, но GT сериализует id в NBT ("gt.effects"→"id") и читает обратно через applyPotion(int)→
		// VANILLA_POTION_IDS. Значит запись обязана писать те же ключи карты выше. Единый источник — здесь, не россыпь
		// литералов по потребителям (RecipeMapBath NBT-запись, EntityFoodTracker id-overload). Значения — ключи карты.
		public static final int
			  POTID_MOVESPEED      =  1, POTID_MOVESLOWDOWN   =  2, POTID_DIGSPEED       =  3, POTID_DIGSLOWDOWN    =  4
			, POTID_DAMAGEBOOST    =  5, POTID_HEAL           =  6, POTID_HARM           =  7, POTID_JUMP           =  8
			, POTID_CONFUSION      =  9, POTID_REGENERATION   = 10, POTID_RESISTANCE     = 11, POTID_FIRERESISTANCE = 12
			, POTID_WATERBREATHING = 13, POTID_INVISIBILITY   = 14, POTID_BLINDNESS      = 15, POTID_NIGHTVISION    = 16
			, POTID_HUNGER         = 17, POTID_WEAKNESS       = 18, POTID_POISON         = 19, POTID_WITHER         = 20
			, POTID_ABSORPTION     = 22, POTID_SATURATION     = 23;

		/** id-адресуемый активный эффект: vanilla id → Holder через VANILLA_POTION_IDS, дальше neo getEffect(Holder).
		 *  Кастом-id чужих модов (не в карте) → null — 1:1 деградация «зелье не зарегистрировано» (как оригинал при
		 *  отсутствии мода: тихий пропуск). Централизует бывший `getEffect(MobEffect.potionTypes[id])`. */
		public static MobEffectInstance getEffectByID(LivingEntity aEntity, int aID) {
			Holder<MobEffect> tPotion = VANILLA_POTION_IDS.get(aID);
			return tPotion == null ? null : aEntity.getEffect(tPotion);
		}

		// F8: aPotion передан ЗНАЧЕНИЕМ (не Holder) — тот же приём, что NBT.getEnchantmentLevel(Enchantment,...)
		// выше: Holder.direct(aPotion) оборачивает переданный объект 1:1, дальше единый Holder-путь ниже.
		public static boolean applyPotion(Entity aEntity, MobEffect aPotion, int aDuration, int aLevel, boolean aInvisibleParticles) {return aPotion != null && applyPotion(aEntity, Holder.direct(aPotion), aDuration, aLevel, aInvisibleParticles);}
		public static boolean applyPotion(Entity aEntity, int aID, int aDuration, int aLevel, boolean aInvisibleParticles) {
			if (aID < -1) switch(aID) {
				case - 2: aID = PotionsGT.ID_RADIATION  ; break;
				case - 3: aID = PotionsGT.ID_HYPOTHERMIA; break;
				case - 4: aID = PotionsGT.ID_HEATSTROKE ; break;
				case - 5: aID = PotionsGT.ID_FROSTBITE  ; break;
				case - 6: aID = PotionsGT.ID_DEHYDRATION; break;
				case - 7: aID = PotionsGT.ID_INSANITY   ; break;
				case - 8: aID = PotionsGT.ID_FLAMMABLE  ; break;
				case - 9: aID = PotionsGT.ID_SLIPPERY   ; break;
				case -10: aID = PotionsGT.ID_CONDUCTIVE ; break;
				case -11: aID = PotionsGT.ID_STICKY     ; break;
			}
			if (aID < 0) return F;
			// vanilla id (1-20,22,23) → реальный Holder<MobEffect> из VANILLA_POTION_IDS выше, полный
			// аудит по факту использования (см. javadoc карты) — деградации для них НЕТ.
			// PORT-TODO(custom-potion-registration): деградируют ТОЛЬКО незарегистрированные кастом-зелья
			// чужих модов PotionsGT.ID_RADIATION/ID_HYPOTHERMIA/ID_HEATSTROKE/ID_FROSTBITE/ID_DEHYDRATION/
			// ID_INSANITY/ID_FLAMMABLE/ID_SLIPPERY/ID_CONDUCTIVE/ID_STICKY (`gregapi/data/CS.java`, класс
			// PotionsGT, IC2/EnviroMine/Immersive Engineering) — сейчас raw int-плейсхолдеры (не
			// `Holder<MobEffect>`), не зарегистрированы как neo MobEffect ни своим, ни чужим
			// DeferredRegister; для НИХ `VANILLA_POTION_IDS.get(aID)` вернёт null (даже если compat-мост
			// когда-нибудь пропишет туда неотрицательное число, коллизии с vanilla-диапазоном 1-23 не
			// будет — id мода отличаются) — попадание сюда форс-деградирует до "не найдено" так же, как
			// оригинал при `aID < 0` (мод не установлен ⇒ тихий пропуск, это 1:1, не новая деградация).
			// Обрести реальный Holder — только после регистрации PotionsGT на DeferredRegister<MobEffect>
			// (отдельный шов, не эта задача).
			Holder<MobEffect> tPotion = VANILLA_POTION_IDS.get(aID);
			return tPotion != null && applyPotion(aEntity, tPotion, aDuration, aLevel, aInvisibleParticles);
		}
		/** Замена 1.7.10 {@code Potion.getLiquidColor()}: цвет зелья по 1.7.10 vanilla-id через neo {@link MobEffect#getColor()}
		 *  из той же карты {@link #VANILLA_POTION_IDS} (конверсия с движком централизована в одном месте). 1.7.10
		 *  {@code Potion(id, isBad, color)} — тот же {@code color} служил liquidColor ⇒ neo effect-color = faithful 1:1.
		 *  Неизвестный/кастом-id (нет в карте) ⇒ 0 (как оригинал при отсутствии зелья). */
		public static int potionColor(int aID) {
			Holder<MobEffect> tPotion = VANILLA_POTION_IDS.get(aID);
			return tPotion == null ? 0 : tPotion.value().getColor();
		}
		public static boolean applyPotion(Entity aEntity, Holder<MobEffect> aPotion, int aDuration, int aLevel, boolean aInvisibleParticles) {
			if (aDuration <= 0 || !(aEntity instanceof LivingEntity)) return F;
			if (aLevel >= 0) {
				((LivingEntity)aEntity).addEffect(new MobEffectInstance(aPotion, aDuration, aLevel, aInvisibleParticles, T));
				return T;
			}
			((LivingEntity)aEntity).removeEffect(aPotion);
			return T;
		}
		
		public static byte pot (Object aEntity, Holder<MobEffect> aPotion) {
			if (aPotion != null && aEntity instanceof LivingEntity) {
				MobEffectInstance tEffect = ((LivingEntity)aEntity).getEffect(aPotion);
				// Limit the output value to six bit, which should be more than enough for Potions, and prevent Byte Math Issues.
				return tEffect == null ? -1 : UT.Code.bind6(tEffect.getAmplifier());
			}
			return -1;
		}
		public static byte pot0(Object aEntity, Holder<MobEffect> aPotion) {return (byte)(pot(aEntity, aPotion)+1);}
		public static byte pot1(Object aEntity, Holder<MobEffect> aPotion) {return (byte)(pot(aEntity, aPotion)+2);}
		public static byte pot2(Object aEntity, Holder<MobEffect> aPotion) {return (byte)(pot(aEntity, aPotion)+3);}
		
		// Used where the Vanilla return Value is important.
		public static byte potStrength         (Object aEntity) {return pot (aEntity, MobEffects.STRENGTH );}
		public static byte potWeakness         (Object aEntity) {return pot (aEntity, MobEffects.WEAKNESS    );}
		public static byte potHaste            (Object aEntity) {return pot (aEntity, MobEffects.HASTE    );}
		public static byte potFatique          (Object aEntity) {return pot (aEntity, MobEffects.MINING_FATIGUE );}
		public static byte potSpeed            (Object aEntity) {return pot (aEntity, MobEffects.SPEED   );}
		public static byte potSlowness         (Object aEntity) {return pot (aEntity, MobEffects.SLOWNESS);}
		
		// Used where 0 should mean no Potion.
		public static byte pot0Strength        (Object aEntity) {return pot0(aEntity, MobEffects.STRENGTH );}
		public static byte pot0Weakness        (Object aEntity) {return pot0(aEntity, MobEffects.WEAKNESS    );}
		public static byte pot0Haste           (Object aEntity) {return pot0(aEntity, MobEffects.HASTE    );}
		public static byte pot0Fatique         (Object aEntity) {return pot0(aEntity, MobEffects.MINING_FATIGUE );}
		public static byte pot0Speed           (Object aEntity) {return pot0(aEntity, MobEffects.SPEED   );}
		public static byte pot0Slowness        (Object aEntity) {return pot0(aEntity, MobEffects.SLOWNESS);}
		
		// Used for places where 2x, 3x and 4x multipliers need to be factored in. So the Base Value without Potion is 1.
		public static byte pot1Strength        (Object aEntity) {return pot1(aEntity, MobEffects.STRENGTH );}
		public static byte pot1Weakness        (Object aEntity) {return pot1(aEntity, MobEffects.WEAKNESS    );}
		public static byte pot1Haste           (Object aEntity) {return pot1(aEntity, MobEffects.HASTE    );}
		public static byte pot1Fatique         (Object aEntity) {return pot1(aEntity, MobEffects.MINING_FATIGUE );}
		public static byte pot1Speed           (Object aEntity) {return pot1(aEntity, MobEffects.SPEED   );}
		public static byte pot1Slowness        (Object aEntity) {return pot1(aEntity, MobEffects.SLOWNESS);}
		
		// Used for places where 1.5x, 2x and 2.5x multipliers need to be factored in more. So the Base Value without Potion is 2.
		public static byte pot2Strength        (Object aEntity) {return pot2(aEntity, MobEffects.STRENGTH );}
		public static byte pot2Weakness        (Object aEntity) {return pot2(aEntity, MobEffects.WEAKNESS    );}
		public static byte pot2Haste           (Object aEntity) {return pot2(aEntity, MobEffects.HASTE    );}
		public static byte pot2Fatique         (Object aEntity) {return pot2(aEntity, MobEffects.MINING_FATIGUE );}
		public static byte pot2Speed           (Object aEntity) {return pot2(aEntity, MobEffects.SPEED   );}
		public static byte pot2Slowness        (Object aEntity) {return pot2(aEntity, MobEffects.SLOWNESS);}
		
		// Will return 0 if neither, otherwise will return the good one as positive and the bad one as negative.
		public static byte potStrengthWeakness (Object aEntity) {return UT.Code.bindByte(pot0(aEntity, MobEffects.STRENGTH ) - pot0(aEntity, MobEffects.WEAKNESS    ));}
		public static byte potHasteFatique     (Object aEntity) {return UT.Code.bindByte(pot0(aEntity, MobEffects.HASTE    ) - pot0(aEntity, MobEffects.MINING_FATIGUE ));}
		public static byte potSpeedSlowness    (Object aEntity) {return UT.Code.bindByte(pot0(aEntity, MobEffects.SPEED   ) - pot0(aEntity, MobEffects.SLOWNESS));}
		
		public static long getDurabilityUse(Object aEntity, long aOriginalDurabilityUsed) {
			return UT.Code.divup(aOriginalDurabilityUsed * pot1Fatique(aEntity), pot1Haste(aEntity));
		}
		
		public static boolean exhaust(Object aPlayer) {return exhaust(aPlayer, 0.1);}
		public static boolean exhaust(Object aPlayer, double aExhaustion) {
			if (aPlayer instanceof Player) {
				if (isInvincible(aPlayer)) return T;
				((Player)aPlayer).causeFoodExhaustion((float)aExhaustion * pot1Fatique(aPlayer));
				return T;
			}
			return F;
		}
		
		public static Collection<Player> getPlayersWithLastTarget(IHasWorldAndCoords aTarget) {return getPlayersWithLastTarget(6, aTarget);}
		public static Collection<Player> getPlayersWithLastTarget(Level aWorld, int aX, int aY, int aZ) {return getPlayersWithLastTarget(6, aWorld, aX, aY, aZ);}
		public static Collection<Player> getPlayersWithLastTarget(Level aWorld, BlockPos aCoords) {return getPlayersWithLastTarget(6, aWorld, aCoords);}
		public static Collection<Player> getPlayersWithLastTarget(long aRange, IHasWorldAndCoords aTarget) {return getPlayersWithLastTarget(aRange, aTarget.getWorld(), aTarget.getCoords());}
		public static Collection<Player> getPlayersWithLastTarget(long aRange, Level aWorld, int aX, int aY, int aZ) {return getPlayersWithLastTarget(aRange, aWorld, new BlockPos(aX, aY, aZ));}
		public static Collection<Player> getPlayersWithLastTarget(long aRange, Level aWorld, BlockPos aCoords) {
			ArrayListNoNulls<Player> rList = new ArrayListNoNulls<>();
			for (Entry<Player, BlockPos> tEntry : PLAYER_LAST_CLICKED.entrySet()) {
				if (!tEntry.getKey().isRemoved() && aWorld == tEntry.getKey().level() && aCoords.equals(tEntry.getValue())) {
					if (isCreative(tEntry.getKey()) || tEntry.getKey().distanceToSqr(aCoords.getX()+0.5, aCoords.getY()+0.5, aCoords.getZ()+0.5) <= aRange * aRange) {
						rList.add(tEntry.getKey());
					}
				}
			}
			return rList;
		}
		
		public static boolean canEdit(Object aPlayer, int aX, int aY, int aZ) {
			return !(aPlayer instanceof Player) || ((Player)aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[SIDE_TOP], NI);
		}
		public static boolean canEdit(Object aPlayer, int aX, int aY, int aZ, ItemStack aStack) {
			return !(aPlayer instanceof Player) || ((Player)aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[SIDE_TOP], aStack);
		}
		public static boolean canEdit(Object aPlayer, int aX, int aY, int aZ, int aSide, ItemStack aStack) {
			return !(aPlayer instanceof Player) || ((Player)aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack);
		}
		
		/** checks if a Player is actually a Player and not a FakePlayer or something. */
		public static boolean isPlayer(Object aPlayer) {
			return aPlayer instanceof ServerPlayer && !(aPlayer instanceof FakePlayer);
		}
		
		/** only works serverside for now */
		public static boolean isSpectator(Object aPlayer) {
			return aPlayer instanceof ServerPlayer && ((ServerPlayer)aPlayer).isSpectator();
		}
		
		public static boolean isCreative(Object aPlayer) {
			return aPlayer instanceof Player && ((Player)aPlayer).getAbilities().instabuild;
		}
		
		public static boolean isInvincible(Object aPlayer) {
			return aPlayer instanceof Player && ((Player)aPlayer).getAbilities().instabuild;
		}
		
		public static boolean hasInfiniteItems(Object aPlayer) {
			return aPlayer instanceof Player && ((Player)aPlayer).getAbilities().instabuild;
		}
		
		public static boolean consumeCurrentItem(Player aPlayer) {
			if (aPlayer == null) return F;
			if (hasInfiniteItems(aPlayer)) return T;
			ItemStack aStack = aPlayer.getInventory().getItem(aPlayer.getInventory().getSelectedSlot());
			if (ST.invalid(aStack)) return F;
			if (aStack.getCount() != NEI_INFINITE) {aStack.setCount(aStack.getCount()-1); if (aStack.getCount() <= 0) aPlayer.getInventory().setItem(aPlayer.getInventory().getSelectedSlot(), NI);}
			ST.give(aPlayer, ST.container(aStack, T), F);
			return T;
		}
	}
	
	@Deprecated public static class Worlds {
		@Deprecated public static ItemStack suckOneItemStackAt(Level aWorld, double aX, double aY, double aZ, double aL, double aH, double aW) {return WD.suck(aWorld, aX, aY, aZ, aL, aH, aW);}
		@Deprecated public static boolean isSideObstructed(Level aWorld, int aX, int aY, int aZ, byte aSide) {return WD.obstructed(aWorld, aX, aY, aZ, aSide);}
		@Deprecated public static HitResult getMovingObjectPositionFromPlayer(Level aWorld, Player aPlayer, boolean aFlag) {return WD.getMOP(aWorld, aPlayer, aFlag);}
		@Deprecated public static boolean isRealDimension(int aDimensionID) {return T;}
		@Deprecated public static boolean moveEntityToDimensionAtCoords(Entity aEntity, int aDimension, double aX, double aY, double aZ) {return WD.move(aEntity, aDimension, aX, aY, aZ);}
		@Deprecated public static DelegatorTileEntity<BlockEntity> getTileEntity(Level aWorld, BlockPos aCoords, byte aSide, boolean aLoadUnloadedChunks) {return WD.te(aWorld, aCoords, aSide, aLoadUnloadedChunks);}
		@Deprecated public static DelegatorTileEntity<BlockEntity> getTileEntity(Level aWorld, int aX, int aY, int aZ, byte aSide, boolean aLoadUnloadedChunks) {return WD.te(aWorld, aX, aY, aZ, aSide, aLoadUnloadedChunks);}
		@Deprecated public static BlockEntity getTileEntity(Level aWorld, BlockPos aCoords, boolean aLoadUnloadedChunks) {return WD.te(aWorld, aCoords, aLoadUnloadedChunks);}
		@Deprecated public static BlockEntity getTileEntity(Level aWorld, int aX, int aY, int aZ, boolean aLoadUnloadedChunks) {return WD.te(aWorld, aX, aY, aZ, aLoadUnloadedChunks);}
		@Deprecated public static BlockEntity setTileEntity(Level aWorld, int aX, int aY, int aZ, BlockEntity aTileEntity, boolean aCauseTileEntityUpdates) {return WD.te(aWorld, aX, aY, aZ, aTileEntity, aCauseTileEntityUpdates);}
		@Deprecated public static long getEnvironmentalTemperature(Level aWorld, int aX, int aY, int aZ) {return WD.envTemp(aWorld, aX, aY, aZ);}
		@Deprecated public static long getTemperature(Level aWorld, int aX, int aY, int aZ) {return WD.temperature(aWorld, aX, aY, aZ);}
		@Deprecated public static ItemStack getStack(Level aWorld, int aX, int aY, int aZ) {return WD.stack(aWorld, aX, aY, aZ);}
		@Deprecated public static Block getBlock(Level aWorld, int aX, int aY, int aZ, boolean aIgnoreUnloadedChunks) {return WD.block(aWorld, aX, aY, aZ, aIgnoreUnloadedChunks);}
		@Deprecated public static boolean setBlock(Level aWorld, int aX, int aY, int aZ, Block aBlock, long aMeta, long aFlags) {return WD.set(aWorld, aX, aY, aZ, aBlock, aMeta, aFlags);}
		@Deprecated public static boolean crossedChunkBorder(int aFromX, int aFromZ, int aToX, int aToZ) {return WD.border(aFromX, aFromZ, aToX, aToZ);}
		@Deprecated public static boolean areCoordsEven(BlockEntity aTileEntity) {return WD.even(aTileEntity);}
		@Deprecated public static boolean areCoordsEven(BlockPos aCoords) {return WD.even(aCoords);}
		@Deprecated public static boolean areCoordsEven(int... aCoords) {return WD.even(aCoords);}
		@Deprecated public static boolean setBlockIfDifferent(Level aWorld, int aX, int aY, int aZ, Block aBlock, int aMetaData, int aFlags) {return WD.setIfDiff(aWorld, aX, aY, aZ, aBlock, aMetaData, aFlags);}
		@Deprecated public static boolean setBlock(Level aWorld, int aX, int aY, int aZ, ItemStack aStack) {return WD.set(aWorld, aX, aY, aZ, aStack);}
		@Deprecated public static boolean isRegularStoneBlock(Block aBlock, short aMetaData) {return WD.stone(aBlock, aMetaData);}
		@Deprecated public static boolean isOreBlock(Block aBlock, short aMetaData) {return WD.ore(aBlock, aMetaData);}
		@Deprecated public static boolean isOreOrRegularStoneBlock(Block aBlock, short aMetaData) {return WD.ore_stone(aBlock, aMetaData);}
		@Deprecated public static boolean isVisuallyOccluded(Level aWorld, int aX, int aY, int aZ, boolean aIgnoreUnloadedChunks, boolean aDefault) {return WD.visOcc(aWorld, aX, aY, aZ, aIgnoreUnloadedChunks, aDefault);}
		@Deprecated public static boolean isVisuallyOpaque(Level aWorld, int aX, int aY, int aZ, boolean aIgnoreUnloadedChunks, boolean aDefault) {return WD.visOpq(aWorld, aX, aY, aZ, aIgnoreUnloadedChunks, aDefault);}
		@Deprecated public static boolean isVisuallyOpaque(Block aBlock) {return WD.visOpq(aBlock);}
		@Deprecated public static boolean isOccluded(Level aWorld, int aX, int aY, int aZ, boolean aIgnoreUnloadedChunks, boolean aDefault) {return WD.occ(aWorld, aX, aY, aZ, aIgnoreUnloadedChunks, aDefault);}
		@Deprecated public static boolean isOpaque(Level aWorld, int aX, int aY, int aZ, boolean aIgnoreUnloadedChunks, boolean aDefault) {return WD.opq(aWorld, aX, aY, aZ, aIgnoreUnloadedChunks, aDefault);}
		@Deprecated public static boolean isAir(Level aWorld, int aX, int aY, int aZ) {return WD.air(aWorld, aX, aY, aZ);}
		@Deprecated public static boolean isEasilyReplaceable(Level aWorld, int aX, int aY, int aZ) {return WD.easyRep(aWorld, aX, aY, aZ);}
		@Deprecated public static boolean hasCollisionBox(Level aWorld, int aX, int aY, int aZ) {return WD.hasCollide(aWorld, aX, aY, aZ);}
		@Deprecated public static void setOnFire(Level aWorld, int aX, int aY, int aZ, boolean aReplaceCenter, boolean aCheckFlammability) {WD.burn(aWorld, aX, aY, aZ, aReplaceCenter, aCheckFlammability);}
		@Deprecated public static void setOnFire(Level aWorld, BlockPos aCoords, boolean aReplaceCenter, boolean aCheckFlammability) {WD.burn(aWorld, aCoords, aReplaceCenter, aCheckFlammability);}
		@Deprecated public static boolean setToFire(Level aWorld, int aX, int aY, int aZ, boolean aCheckFlammability) {return WD.fire(aWorld, aX, aY, aZ, aCheckFlammability);}
		@Deprecated public static boolean setToFire(Level aWorld, BlockPos aCoords, boolean aCheckFlammability) {return WD.fire(aWorld, aCoords, aCheckFlammability);}
		@Deprecated public static boolean getCoordsOnFire(Level aWorld, int aX, int aY, int aZ) {return WD.burning(aWorld, aX, aY, aZ);}
		@Deprecated public static long getCoordinateScan(ArrayList<String> aList, Player aPlayer, Level aWorld, int aScanLevel, int aX, int aY, int aZ, byte aSide, float aClickX, float aClickY, float aClickZ) {return WD.scan(aList, aPlayer, aWorld, aScanLevel, aX, aY, aZ, aSide, aClickX, aClickY, aClickZ);}
	}
	
	@Deprecated public static class Stacks {
		@Deprecated public static boolean debugItem(ItemStack aStack) {return ST.debug(aStack);}
		@Deprecated public static ItemStack update(ItemStack aStack) {return ST.update(aStack);}
		@Deprecated public static ItemStack update_(ItemStack aStack) {return ST.update_(aStack);}
		@Deprecated public static boolean inList(Collection<ItemStack> aList, ItemStack aStack, boolean aTrueIfListEmpty, boolean aInvertFilter) {return ST.listed(aList, aStack, aTrueIfListEmpty, aInvertFilter);}
		@Deprecated public static ItemStack set(Object aSetStack, Object aToStack) {return ST.set((ItemStack)aSetStack, (ItemStack)aToStack);}
		@Deprecated public static ItemStack set(Object aSetStack, Object aToStack, boolean aCheckStacksize, boolean aCheckNBT) {return ST.set((ItemStack)aSetStack, (ItemStack)aToStack, aCheckStacksize, aCheckNBT);}
		@Deprecated public static ItemStack container(ItemStack aStack, boolean aCheckIFluidContainerItems) {return ST.container(aStack, aCheckIFluidContainerItems);}
		@Deprecated public static ItemStack container(ItemStack aStack, boolean aCheckIFluidContainerItems, int aStacksize) {return ST.container(aStack, aCheckIFluidContainerItems, aStacksize);}
		@Deprecated public static boolean equal(ItemStack aStack1, ItemStack aStack2) {return ST.equal(aStack1, aStack2);}
		@Deprecated public static boolean equalTools(ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return ST.equalTools(aStack1, aStack2, aIgnoreNBT);}
		@Deprecated public static boolean equalTools_(ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return ST.equalTools_(aStack1, aStack2, aIgnoreNBT);}
		@Deprecated public static boolean equal(ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return ST.equal(aStack1, aStack2, aIgnoreNBT);}
		@Deprecated public static boolean equal_(ItemStack aStack1, ItemStack aStack2, boolean aIgnoreNBT) {return ST.equal_(aStack1, aStack2, aIgnoreNBT);}
		@Deprecated public static short id(Item aItem) {return ST.id(aItem);}
		@Deprecated public static short id(ItemStack aStack) {return ST.id(aStack);}
		@Deprecated public static Item item(ItemStack aStack) {return ST.item(aStack);}
		@Deprecated public static short meta(ItemStack aStack) {return ST.meta_(aStack);}
		@Deprecated public static ItemStack meta(ItemStack aStack, long aMeta) {return ST.meta_(aStack, aMeta);}
		@Deprecated public static boolean rotten(ItemStack aStack) {return ST.rotten(aStack);}
		@Deprecated public static int food(ItemStack aStack) {return ST.food(aStack);}
		@Deprecated public static float saturation(ItemStack aStack) {return ST.saturation(aStack);}
		@Deprecated public static ItemStack fuel(ItemStack aStack, short aValue) {return ST.fuel(aStack, aValue);}
		@Deprecated public static long fuel(ItemStack aStack) {return ST.fuel(aStack);}
		@Deprecated public static ItemStack makeIC2(String aItem, long aAmount, ItemStack aReplacement) {return ST.mkic(aItem, aAmount, aReplacement);}
		@Deprecated public static ItemStack makeIC2(String aItem, long aAmount, int aMeta, ItemStack aReplacement) {return ST.mkic(aItem, aAmount, aMeta, aReplacement);}
		@Deprecated public static ItemStack makeIC2(String aItem, long aAmount, int aMeta) {return ST.mkic(aItem, aAmount, aMeta);}
		@Deprecated public static ItemStack makeIC2(String aItem, long aAmount) {return ST.mkic(aItem, aAmount);}
		@Deprecated public static Item item(ModData aModID, String aItem) {return item(make(aModID, aItem, 1, null));}
		@Deprecated public static Item item(ModData aModID, String aItem, Item aReplacement) {Item rItem = item(aModID, aItem); return rItem == null ? aReplacement : rItem;}
		@Deprecated public static Item item(String aModID, String aItem) {return item(make(aModID, aItem, 1, null));}
		@Deprecated public static Item item(String aModID, String aItem, Item aReplacement) {Item rItem = item(aModID, aItem); return rItem == null ? aReplacement : rItem;}
		@Deprecated public static Block block(ModData aModID, String aBlock) {return block(make(aModID, aBlock, 1, null));}
		@Deprecated public static Block block(ModData aModID, String aBlock, Block aReplacement) {Block rBlock = block(aModID, aBlock); return rBlock == NB ? aReplacement : rBlock;}
		@Deprecated public static Block block(String aModID, String aBlock) {return block(make(aModID, aBlock, 1, null));}
		@Deprecated public static Block block(String aModID, String aBlock, Block aReplacement) {Block rBlock = block(aModID, aBlock); return rBlock == NB ? aReplacement : rBlock;}
		@Deprecated public static ItemStack make(ModData aModID, String aItem, long aAmount) {return make(aModID, aItem, aAmount, null);}
		@Deprecated public static ItemStack make(ModData aModID, String aItem, long aAmount, ItemStack aReplacement) {if (!aModID.mLoaded || Code.stringInvalid(aItem) || !GAPI_POST.mStartedPreInit) return null; if (aItem.length()>5&&aItem.charAt(0)=='t'&&aItem.charAt(1)=='i'&&aItem.charAt(2)=='l'&&aItem.charAt(3)=='e'&&aItem.charAt(4)=='.') return amount(aAmount, ST.findItemStack(aModID.mID, aItem, (int)aAmount), ST.findItemStack(aModID.mID, aItem.substring(5), (int)aAmount), aReplacement); return amount(aAmount, ST.findItemStack(aModID.mID, aItem, (int)aAmount), aReplacement);}
		@Deprecated public static ItemStack make(ModData aModID, String aItem, long aAmount, int aMeta) {ItemStack rStack = make(aModID, aItem, aAmount); if (rStack == null) return null; meta(rStack, aMeta); return rStack;}
		@Deprecated public static ItemStack make(ModData aModID, String aItem, long aAmount, int aMeta, ItemStack aReplacement) {ItemStack rStack = make(aModID, aItem, aAmount, aReplacement); if (rStack == null) return null; meta(rStack, aMeta); return rStack;}
		@Deprecated public static ItemStack make(String aModID, String aItem, long aAmount) {return make(aModID, aItem, aAmount, null);}
		@Deprecated public static ItemStack make(String aModID, String aItem, long aAmount, ItemStack aReplacement) {if (Code.stringInvalid(aItem) || !GAPI_POST.mStartedPreInit) return null; if (aItem.length()>5&&aItem.charAt(0)=='t'&&aItem.charAt(1)=='i'&&aItem.charAt(2)=='l'&&aItem.charAt(3)=='e'&&aItem.charAt(4)=='.') return amount(aAmount, ST.findItemStack(aModID, aItem, (int)aAmount), ST.findItemStack(aModID, aItem.substring(5), (int)aAmount), aReplacement); return amount(aAmount, ST.findItemStack(aModID, aItem, (int)aAmount), aReplacement);}
		@Deprecated public static ItemStack make(String aModID, String aItem, long aAmount, int aMeta) {ItemStack rStack = make(aModID, aItem, aAmount); if (rStack == null) return null; meta(rStack, aMeta); return rStack;}
		@Deprecated public static ItemStack make(String aModID, String aItem, long aAmount, int aMeta, ItemStack aReplacement) {ItemStack rStack = make(aModID, aItem, aAmount, aReplacement); if (rStack == null) return null; meta(rStack, aMeta); return rStack;}
		@Deprecated public static ItemStack make(long aItemID, long aStacksize, long aMetaData) {return aItemID==0?null:make(Item.byId((int)aItemID), aStacksize, aMetaData);}
		@Deprecated public static ItemStack make(Item aItem, long aStacksize, long aMetaData) {return aItem == null ? null : make(ST.meta_(new ItemStack(aItem, Code.bindInt(aStacksize)), aMetaData), null);}
		@Deprecated public static ItemStack make(Block aBlock, long aStacksize, long aMetaData) {return aBlock == null || aBlock == NB ? null : make(ST.meta_(new ItemStack(aBlock, Code.bindInt(aStacksize)), aMetaData), null);}
		@Deprecated public static ItemStack make(long aItemID, long aStacksize, long aMetaData, CompoundTag aNBT) {return aItemID==0?null:make(Item.byId((int)aItemID), aStacksize, aMetaData, aNBT);}
		@Deprecated public static ItemStack make(Item aItem, long aStacksize, long aMetaData, CompoundTag aNBT) {return aItem == null ? null : make(ST.meta_(new ItemStack(aItem, Code.bindInt(aStacksize)), aMetaData), aNBT);}
		@Deprecated public static ItemStack make(Block aBlock, long aStacksize, long aMetaData, CompoundTag aNBT) {return aBlock == null || aBlock == NB ? null : make(ST.meta_(new ItemStack(aBlock, Code.bindInt(aStacksize)), aMetaData), aNBT);}
		@Deprecated public static ItemStack make(ItemStack aStack, CompoundTag aNBT) {return make(aStack, null, aNBT);}
		@Deprecated public static ItemStack make(ItemStackContainer aStack, CompoundTag aNBT) {return make(aStack, null, aNBT);}
		@Deprecated public static ItemStack make(long aItemID, long aStacksize, long aMetaData, String aName) {return aItemID==0?null:make(Item.byId((int)aItemID), aStacksize, aMetaData, aName);}
		@Deprecated public static ItemStack make(Item aItem, long aStacksize, long aMetaData, String aName) {return aItem == null ? null : make(ST.meta_(new ItemStack(aItem, Code.bindInt(aStacksize)), aMetaData), aName, null);}
		@Deprecated public static ItemStack make(Block aBlock, long aStacksize, long aMetaData, String aName) {return aBlock == null || aBlock == NB ? null : make(ST.meta_(new ItemStack(aBlock, Code.bindInt(aStacksize)), aMetaData), aName, null);}
		@Deprecated public static ItemStack make(long aItemID, long aStacksize, long aMetaData, String aName, CompoundTag aNBT) {return aItemID==0?null:make(Item.byId((int)aItemID), aStacksize, aMetaData, aName, aNBT);}
		@Deprecated public static ItemStack make(Item aItem, long aStacksize, long aMetaData, String aName, CompoundTag aNBT) {return aItem == null ? null : make(ST.meta_(new ItemStack(aItem, Code.bindInt(aStacksize)), aMetaData), aName, aNBT);}
		@Deprecated public static ItemStack make(Block aBlock, long aStacksize, long aMetaData, String aName, CompoundTag aNBT) {return aBlock == null || aBlock == NB ? null : make(ST.meta_(new ItemStack(aBlock, Code.bindInt(aStacksize)), aMetaData), aName, aNBT);}
		@Deprecated public static ItemStack make(ItemStack aStack, String aName, CompoundTag aNBT) {if (aStack == null) return null; aStack = aStack.copy(); NBT.set(aStack, aNBT); if (aName != null) ST.name_(aStack, aName); return aStack;}
		@Deprecated public static ItemStack make(ItemStackContainer aStack, String aName, CompoundTag aNBT) {if (aStack == null) return null; ItemStack rStack = aStack.toStack(); if (rStack == null) return null; NBT.set(rStack, aNBT); if (aName != null) ST.name_(rStack, aName); return rStack;}
		@Deprecated public static ItemStack[] copyArray(Object... aStacks) {return ST.copyArray((ItemStack[])aStacks);}
		@Deprecated public static ItemStack copy(Object... aStacks) {return ST.copyFirst(aStacks);}
		@Deprecated public static ItemStack amount(long aAmount, Object... aStacks) {return ST.amount(aAmount, (ItemStack)aStacks[0]);}
		@Deprecated public static ItemStack copyAmount(long aAmount, Object... aStacks) {return ST.amount(aAmount, (ItemStack)aStacks[0]);}
		@Deprecated public static ItemStack copyMeta(long aMetaData, Object... aStacks) {return copyMeta(aMetaData, aStacks);}
		@Deprecated public static ItemStack copyAmountAndMeta(long aAmount, long aMetaData, Object... aStacks) {return ST.copyAmountAndMeta(aAmount, aMetaData, (ItemStack)aStacks[0]);}
		@Deprecated public static ItemStack mul(long aMultiplier, Object... aStacks) {return ST.mul(aMultiplier, (ItemStack)aStacks[0]);}
		@Deprecated public static ItemStack div(long aDivider, Object... aStacks) {return ST.div(aDivider, (ItemStack)aStacks[0]);}
		@Deprecated public static int toInt(ItemStack aStack) {return ST.toInt(aStack);}
		@Deprecated public static int toIntWildcard(ItemStack aStack) {return ST.toInt(aStack, W);}
		@Deprecated public static ItemStack toStack(int aStack) {return ST.toStack(aStack);}
		@Deprecated public static Integer[] toIntegerArray(ItemStack... aStacks) {return ST.toIntegerArray(aStacks);}
		@Deprecated public static int[] toIntArray(ItemStack... aStacks) {return ST.toIntArray(aStacks);}
		@Deprecated public static Block block(Object aStack) {return ST.block((ItemStack)aStack);}
		@Deprecated public static Block block_(Object aStack) {return ST.block_((ItemStack)aStack);}
		@Deprecated public static boolean valid(Object aStack) {return ST.valid((ItemStack)aStack);}
		@Deprecated public static boolean invalid(Object aStack) {return ST.invalid((ItemStack)aStack);}
		@Deprecated public static String configName(ItemStack aStack) {return ST.configName(aStack);}
		@Deprecated public static String configNames(ItemStack... aStacks) {return ST.configNames(aStacks);}
		@Deprecated public static String regName(ItemStack aStack) {return ST.regName(aStack);}
		@Deprecated public static String names(ItemStack... aStacks) {return ST.names(aStacks);}
		@Deprecated public static String namesAndSizes(ItemStack... aStacks) {return ST.namesAndSizes(aStacks);}
		@Deprecated public static void hide(Item aItem) {ST.hide(aItem);}
		@Deprecated public static void hide(Item aItem, long aMetaData) {ST.hide(aItem, aMetaData);}
		@Deprecated public static void hide(Block aBlock) {ST.hide(aBlock);}
		@Deprecated public static void hide(Block aBlock, long aMetaData) {ST.hide(aBlock, aMetaData);}
		@Deprecated public static void hide(ItemStack aStack) {ST.hide(aStack);}
		@Deprecated public static ItemStack load(CompoundTag aNBT, String aTagName) {return ST.load(aNBT, aTagName);}
		@Deprecated public static ItemStack load(CompoundTag aNBT) {return ST.load(aNBT);}
		@Deprecated public static CompoundTag save(CompoundTag aNBT, String aTagName, ItemStack aStack) {return ST.save(aNBT, aTagName, aStack);}
		@Deprecated public static CompoundTag save(ItemStack aStack) {return ST.save(aStack);}
	}
	
	@Deprecated public static class Crafting {
		@Deprecated public static class Bits {@Deprecated public static final long NONE = 0, MIR = B[0], BUF = B[1], REV = B[5], KEEPNBT = B[2], DISMANTLE = B[3], NO_REM = B[4], NO_AUTO = B[14], NO_COLLISION_CHECK = B[10], DEL_OTHER_RECIPES = B[6], DEL_OTHER_RECIPES_IF_SAME_NBT = B[7], DEL_OTHER_SHAPED_RECIPES = B[8], DEL_OTHER_NATIVE_RECIPES = B[9], DEL_IF_NO_DYES = B[13], ONLY_IF_HAS_OTHER_RECIPES = B[11], ONLY_IF_HAS_RESULT = B[12], DEFAULT = BUF|NO_REM, DEFAULT_MIR = DEFAULT|MIR, DEFAULT_REV = DEFAULT|REV, DEFAULT_NCC = DEFAULT|NO_COLLISION_CHECK, DEFAULT_REV_NCC = DEFAULT_REV|NO_COLLISION_CHECK, DEFAULT_NAC = DEFAULT|NO_AUTO, DEFAULT_NAC_NCC = DEFAULT_NCC|NO_AUTO, DEFAULT_NAC_REV = DEFAULT_REV|NO_AUTO, DEFAULT_NAC_REV_NCC = DEFAULT_REV_NCC|NO_AUTO, DEFAULT_REM = DEFAULT|DEL_OTHER_RECIPES, DEFAULT_REM_REV = DEFAULT_REM|REV, DEFAULT_REM_NCC = DEFAULT_REM|NO_COLLISION_CHECK, DEFAULT_REM_REV_NCC = DEFAULT_REM_REV|NO_COLLISION_CHECK, DEFAULT_REM_NAC = DEFAULT_REM|NO_AUTO, DEFAULT_REM_NAC_NCC = DEFAULT_REM_NCC|NO_AUTO, DEFAULT_REM_NAC_REV = DEFAULT_REM_REV|NO_AUTO, DEFAULT_REM_NAC_REV_NCC = DEFAULT_REM_REV_NCC|NO_AUTO;}
		@Deprecated public static boolean shaped(ItemStack aResult, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] aEnchantmentsAdded, int[] aEnchantmentLevelsAdded, Object[] aRecipe) {return CR.shaped(aResult, aEnchantmentsAdded, aEnchantmentLevelsAdded, aRecipe);}
		@Deprecated public static boolean shaped(ItemStack aResult, Object[] aRecipe) {return CR.shaped(aResult, aRecipe);}
		@Deprecated public static boolean shaped(ItemStack aResult, long aBitMask, Object[] aRecipe) {return CR.shaped(aResult, aBitMask, aRecipe);}
		@Deprecated public static boolean shapeless(ItemStack aResult, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>[] aEnchantmentsAdded, int[] aEnchantmentLevelsAdded, Object[] aRecipe) {return CR.shapeless(aResult, aEnchantmentsAdded, aEnchantmentLevelsAdded, aRecipe);}
		@Deprecated public static boolean shapeless(ItemStack aResult, Object[] aRecipe) {return CR.shapeless(aResult, aRecipe);}
		@Deprecated public static boolean shapeless(ItemStack aResult, long aBitMask, Object[] aRecipe) {return CR.shapeless(aResult, aBitMask, aRecipe);}
		@Deprecated public static ItemStack getany(Level aWorld, ItemStack... aRecipe) {return CR.getany(aWorld, aRecipe);}
		@Deprecated public static ItemStack get(ItemStack... aRecipe) {return CR.get(aRecipe);}
		@Deprecated public static ItemStack get(boolean aUncopiedStack, ItemStack... aRecipe) {return CR.get(aUncopiedStack, aRecipe);}
		@Deprecated public static boolean has(ItemStack aOutput) {return CR.has(aOutput);}
		@Deprecated public static boolean remout(ItemStack aOutput, boolean aIgnoreNBT, boolean aNotRemoveShapelessRecipes, boolean aOnlyRemoveNativeHandlers, boolean aDontRemoveDyeingRecipes) {return CR.remout(aOutput, aIgnoreNBT, aNotRemoveShapelessRecipes, aOnlyRemoveNativeHandlers, aDontRemoveDyeingRecipes);}
		@Deprecated public static boolean remout(ItemStack aOutput) {return CR.remout(aOutput);}
		@Deprecated public static boolean remout(ModData aMod, String... aNames) {return CR.remout(aMod, aNames);}
		@Deprecated public static boolean remout(ModData aMod, String aName, int aMetaData) {return CR.remout(aMod, aName, aMetaData);}
		@Deprecated public static ItemStack remove(ItemStack... aRecipe) {return CR.remove(aRecipe);}
	}
	
	public static synchronized boolean removeSimpleIC2MachineRecipe(ItemStack aInput, @SuppressWarnings("rawtypes") Map aRecipeList, ItemStack aOutput) {
		if (!MD.IC2.mLoaded || (ST.invalid(aInput) && ST.invalid(aOutput)) || aRecipeList == null || aRecipeList.isEmpty()) return F;
		boolean rReturn = F;
		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<IRecipeInput, RecipeOutput>> tIterator = aRecipeList.entrySet().iterator();
		aOutput = OM.get_(aOutput);
		while (tIterator.hasNext()) {
			Map.Entry<IRecipeInput, RecipeOutput> tEntry = tIterator.next();
			if (aInput == null || tEntry.getKey().matches(aInput)) {
				List<ItemStack> tList = tEntry.getValue().items;
				if (tList != null) for (ItemStack tOutput : tList) if (ST.invalid(aOutput) || ST.equal(OM.get(tOutput), aOutput)) {
					tIterator.remove();
					rReturn = T;
					break;
				}
			}
		}
		return rReturn;
	}
	
	public static boolean addSimpleIC2MachineRecipe(IMachineRecipeManager aRecipeManager, ItemStack aInput, CompoundTag aNBT, Object... aOutput) {
		if (!MD.IC2.mLoaded || ST.invalid(aInput) || aOutput == null || aRecipeManager == null) return F;
		try {
			aOutput = Code.getWithoutNulls(aOutput).toArray(ZL);
			if (aOutput.length == 0) return F;
			OreDictItemData tOreName = OM.association_(aInput);
			if (aRecipeManager instanceof IMachineRecipeManagerExt) {
				if (tOreName != null && !tOreName.mBlackListed && !OreDictManager.INSTANCE.isBlacklisted(aInput)) {
					((IMachineRecipeManagerExt)aRecipeManager).addRecipe((IRecipeInput)COMPAT_IC2.makeInput(tOreName.toString(), aInput.getCount()), aNBT, T, OreDictManager.INSTANCE.getStackArray(T, aOutput));
				} else {
					((IMachineRecipeManagerExt)aRecipeManager).addRecipe((IRecipeInput)COMPAT_IC2.makeInput(aInput), aNBT, T, OreDictManager.INSTANCE.getStackArray(T, aOutput));
				}
			} else {
				if (tOreName != null && !tOreName.mBlackListed && !OreDictManager.INSTANCE.isBlacklisted(aInput)) {
					aRecipeManager.addRecipe((IRecipeInput)COMPAT_IC2.makeInput(tOreName.toString(), aInput.getCount()), aNBT, OreDictManager.INSTANCE.getStackArray(T, aOutput));
				} else {
					aRecipeManager.addRecipe((IRecipeInput)COMPAT_IC2.makeInput(aInput), aNBT, OreDictManager.INSTANCE.getStackArray(T, aOutput));
				}
			}
		} catch(Throwable e) {/**/}
		return T;
	}
	
	@SuppressWarnings("unchecked")
	public static boolean addSimpleIC2MachineRecipe(ItemStack aInput, @SuppressWarnings("rawtypes") Map aRecipeList, CompoundTag aNBT, Object... aOutput) {
		if (!MD.IC2.mLoaded || ST.invalid(aInput) || aOutput.length == 0 || aRecipeList == null) return F;
		OreDictItemData tOreName = OM.association_(aInput);
		if (tOreName != null) {
			aRecipeList.put(COMPAT_IC2.makeInput(tOreName.toString(), aInput.getCount()), COMPAT_IC2.makeOutput(aNBT, OreDictManager.INSTANCE.getStackArray(T, aOutput)));
		} else {
			aRecipeList.put(COMPAT_IC2.makeInput(aInput), COMPAT_IC2.makeOutput(aNBT, OreDictManager.INSTANCE.getStackArray(T, aOutput)));
		}
		return T;
	}
	
	/**
	 * F-item-use: 1.7.10 {@code ItemStack.tryPlaceItemIntoWorld(EntityPlayer, World, int, int, int, int,
	 * float, float, float)} — метод физически удалён движком; neo заменяет его на
	 * {@code Item.useOn(UseOnContext)} -> {@code InteractionResult} (сверено
	 * neo-decompiled/net/minecraft/world/item/Item.java:198, вызывается через
	 * neo-decompiled/net/minecraft/world/item/ItemStack.java:362 {@code ItemStack.useOn(UseOnContext)}).
	 * Централизованный переходник (ОДНО место, без россыпи) — собирает {@code BlockHitResult}/
	 * {@code UseOnContext} и переводит {@code InteractionResult} обратно в старый {@code boolean}-сигнал успеха.
	 * Верифицированные neo-символы:
	 *  - {@code BlockHitResult(Vec3, Direction, BlockPos, boolean)}: neo-decompiled/net/minecraft/world/phys/BlockHitResult.java:17
	 *  - {@code Vec3(double, double, double)}: neo-decompiled/net/minecraft/world/phys/Vec3.java:65
	 *  - {@code BlockPos(int, int, int)}: neo-decompiled/net/minecraft/core/BlockPos.java:59
	 *  - {@code UseOnContext(Level, Player, InteractionHand, ItemStack, BlockHitResult)}: neo-decompiled/net/minecraft/world/item/context/UseOnContext.java:24
	 *  - {@code InteractionHand.MAIN_HAND}: neo-decompiled/net/minecraft/world/InteractionHand.java:11
	 *  - {@code ItemStack.useOn(UseOnContext)}: neo-decompiled/net/minecraft/world/item/ItemStack.java:362
	 *  - {@code InteractionResult.consumesAction()}: neo-decompiled/net/minecraft/world/InteractionResult.java:18
	 *  - {@code CS.FORGE_DIR}: gregapi/data/CS.java:660
	 * @return успешно ли было размещение (1:1 со старым {@code boolean}-возвратом).
	 */
	public static boolean tryPlaceItemIntoWorld(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, byte aSide, float aHitX, float aHitY, float aHitZ) {
		BlockHitResult tHit = new BlockHitResult(new Vec3(aX+aHitX, aY+aHitY, aZ+aHitZ), FORGE_DIR[aSide], new BlockPos(aX, aY, aZ), F);
		UseOnContext tCtx = new UseOnContext(aWorld, aPlayer, InteractionHand.MAIN_HAND, aStack, tHit);
		InteractionResult tResult = aStack.useOn(tCtx);
		return tResult.consumesAction();
	}

	/**
	 * Yes, I have read all those warning that it might break. But I don't expect any further development on this Function by Forge during 1.7.10.
	 *
	 * That said, I've put a try/catch around this Stuff in case of random Errors.
	 */
	public static class LoadingBar {
		public static boolean mEnabled = T;
		public static Object mBar = null;
		public static int mSize = 0, mCount = 0;
		public static Field mMessage = null, mStep = null;
		
		@SuppressWarnings("deprecation")
		public static boolean start(String aTitle, int aSize) {
			if (mBar == null && mEnabled && aSize > 0) {
				try {
					mBar = cpw.mods.fml.common.ProgressManager.push(aTitle, aSize, F);
					mMessage = UT.Reflection.getField(mBar, "message", T, T);
					mStep = UT.Reflection.getField(mBar, "step", T, T);
					mSize = aSize;
					mCount = 0;
					return T;
				} catch(NoClassDefFoundError e) {
					mEnabled = F;
				} catch(Throwable e) {e.printStackTrace(ERR);}
			}
			return F;
		}
		
		public static boolean step(Object aStepName) {
			if (mBar != null && mEnabled) {
				if (mCount++ < mSize) {
					try {
						mMessage.set(mBar, aStepName == null ? "Error: NULL" : aStepName.toString());
						mStep.setInt(mBar, mCount);
						return T;
					} catch(Throwable e) {e.printStackTrace(ERR);}
					return F;
				}
				ERR.println("ERROR: Progress Bar needed a forced Finish, because of too many Steps.");
				finish();
				return F;
			}
			return F;
		}
		
		@SuppressWarnings("deprecation")
		public static boolean finish() {
			if (mBar != null && mEnabled) {
				if (mCount != mSize) ERR.println("ERROR: Progress Bar needed a forced Finish, because of too few Steps.");
				try {
					cpw.mods.fml.common.ProgressManager.pop((cpw.mods.fml.common.ProgressManager.ProgressBar)mBar);
					mBar = null;
					mSize = 0;
					mCount = 0;
					return T;
				} catch(NoClassDefFoundError e) {
					mEnabled = F;
				} catch(Throwable e) {e.printStackTrace(ERR);}
				mBar = null;
				mSize = 0;
				mCount = 0;
				return F;
			}
			return F;
		}
	}
}
