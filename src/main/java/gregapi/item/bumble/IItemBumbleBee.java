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

package gregapi.item.bumble;

import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Random;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public interface IItemBumbleBee {
	/** @return The Amount of different Products for this Bumblebee Species. */
	public int bumbleProductCount(ItemStack aBumbleBee, short aMetaData);
	/** @return The Item produced by this Bumblebee Species. */
	public ItemStack bumbleProductStack(ItemStack aBumbleBee, short aMetaData, long aStacksize, int aProductIndex);
	/** @return 0 for no Chance, 10000 for a 100% Chance. */
	public int bumbleProductChance(ItemStack aBumbleBee, short aMetaData, int aProductIndex);
	/** @return The Coordinates of where the Bumbles fly to for this product, or the Coords of the passed Hive, if the Environment allows the production of this specific Product. This is usually for checking for specific Flowers for this Product. */
	public BlockPos bumbleCanProduct(Level aWorld, int aX, int aY, int aZ, ItemStack aBumbleBee, short aMetaData, int aProductIndex);
	
	/** @return The Coordinates of where the Bumbles fly to for this product, or the Coords of the passed Hive, if the Environment allows the Bumblebee Species to be active at this point in time. This is used for the Main checks. */
	public BlockPos bumbleCanProduce(Level aWorld, int aX, int aY, int aZ, ItemStack aBumbleBee, short aMetaData, int aDistance);
	
	/** @return 0 = Drone, 1 = Princess, 2 = Queen, 4 = Dead, 5 = Scanned Drone, 6 = Scanned Princess, 7 = Scanned Queen, 9 = Dead Scanned */
	public byte bumbleType(ItemStack aBumbleBee);
	/** @return ItemStack with the scanned variant of this Bumblebee. */
	public ItemStack bumbleScan(ItemStack aBumbleBee);
	/** @return ItemStack with the dead variant of this Bumblebee. */
	public ItemStack bumbleKill(ItemStack aBumbleBee);
	/** @return ItemStack with a Queen variant of this Princess. */
	public ItemStack bumbleCrown(ItemStack aBumbleBee);
	
	/** @return 0 for no Chance, 10000 for a 100% Chance. */
	public int bumbleMutateChance(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a mutated variant of this Bumblebee. Used for random Mutations, when two Bumblebees of the same Species breed. */
	public ItemStack bumbleMutate(ItemStack aBumbleBee, short aMetaData, Random aRandom);
	/**
	 * This is only called in quarter of all cases for aBumbleBeeA, another quarter goes to aBumbleBeeB, the remaining half goes to selecting the same species as one of the Parents.
	 * It is not called when two Bumblebees of the same Species breed.
	 * @return ItemStack with a special Offspring for this combination and Stacksize 1 and no BumbleTag.
	 */
	public ItemStack bumbleCombine(ItemStack aBumbleBeeA, short aMetaDataA, ItemStack aBumbleBeeB, short aMetaDataB, byte aBumbleType, Random aRandom);
	
	
	/** @return ItemStack with a Drone of this Species (and without NBT!) */
	public ItemStack bumbleDrone(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a Princess of this Species (and without NBT!) */
	public ItemStack bumblePrincess(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a Queen of this Species (and without NBT!) */
	public ItemStack bumbleQueen(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a dead Bumblebee of this Species (and without NBT!) */
	public ItemStack bumbleDead(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a scanned Drone of this Species (and without NBT!) */
	public ItemStack bumbleDrone_(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a scanned Princess of this Species (and without NBT!) */
	public ItemStack bumblePrincess_(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a scanned Queen of this Species (and without NBT!) */
	public ItemStack bumbleQueen_(ItemStack aBumbleBee, short aMetaData);
	/** @return ItemStack with a scanned dead Bumblebee of this Species (and without NBT!) */
	public ItemStack bumbleDead_(ItemStack aBumbleBee, short aMetaData);
	
	
	/** @return if this Bumblebee was able to attack the Target, if it has an effect. You have to implement the damaging Entity Part fully yourself, since I don't know if you want to damage or heal someone for example. */
	public boolean bumbleAttack(ItemStack aBumbleBee, short aMetaData, LivingEntity aAttacked);
	
	/** @return if this Bumblebee Species is equal to the Species of the second Bumblebee. */
	public boolean bumbleEqual(ItemStack aBumbleBeeA, short aMetaDataA, ItemStack aBumbleBeeB, short aMetaDataB);
	
	public static class Util {
		public static CompoundTag getBumbleTag(ItemStack aBumbleBee) {
			CompoundTag aNBT = UT.NBT.getOrCreate(aBumbleBee), rBumbleTag = aNBT.getCompoundTag("gt.bumble");
			if (rBumbleTag == null || rBumbleTag.hasNoTags()) rBumbleTag = getBumbleGenes(RNGSUS);
			aNBT.setTag("gt.bumble", rBumbleTag);
			return rBumbleTag;
		}
		
		public static ItemStack setBumbleTag(ItemStack aBumbleBee, CompoundTag aBumbleTag) {
			UT.NBT.getOrCreate(aBumbleBee).setTag("gt.bumble", aBumbleTag);
			return aBumbleBee;
		}
		
		public static CompoundTag getBumbleGenes(ItemStack aBumblePrincess, ItemStack aBumbleDrone, Random aRandom) {
			CompoundTag rBumbleTag = UT.NBT.make(), tBumbleTagA = getBumbleTag(aBumblePrincess), tBumbleTagB = getBumbleTag(aBumbleDrone);
			if (tBumbleTagA == null || tBumbleTagA.hasNoTags()) tBumbleTagA = getBumbleGenes(aRandom);
			if (tBumbleTagB == null || tBumbleTagB.hasNoTags()) tBumbleTagB = getBumbleGenes(aRandom);
			setHumidityMin      (rBumbleTag, getHumidityMin     (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setHumidityMax      (rBumbleTag, getHumidityMax     (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setOffspring        (rBumbleTag, getOffspring       (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setWorkForce        (rBumbleTag, getWorkForce       (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setAggressiveness   (rBumbleTag, getAggressiveness  (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setLifeSpan         (rBumbleTag, getLifeSpan        (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setTemperatureMin   (rBumbleTag, getTemperatureMin  (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setTemperatureMax   (rBumbleTag, getTemperatureMax  (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setRainproof        (rBumbleTag, getRainproof       (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setStormproof       (rBumbleTag, getStormproof      (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setNightActive      (rBumbleTag, getNightActive     (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setDayActive        (rBumbleTag, getDayActive       (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB) || !getNightActive(rBumbleTag));
			setInsideActive     (rBumbleTag, getInsideActive    (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB));
			setOutsideActive    (rBumbleTag, getOutsideActive   (aRandom.nextBoolean()?tBumbleTagA:tBumbleTagB) || !getInsideActive(rBumbleTag));
			return rBumbleTag;
		}
		
		public static CompoundTag getBumbleGenes(Random aRandom) {return getBumbleGenes(WD.envTemp(Biome.plains), Biome.plains, T, aRandom);}
		public static CompoundTag getBumbleGenes(long aTemperature, Biome aBiome, boolean aHasSky, Random aRandom) {return getBumbleGenes(aTemperature, aBiome, aHasSky, !(BIOMES_DESERT.contains(aBiome.biomeName) || BIOMES_MESA.contains(aBiome.biomeName)), BIOMES_DESERT.contains(aBiome.biomeName) || BIOMES_MESA.contains(aBiome.biomeName), aRandom);}
		public static CompoundTag getBumbleGenes(long aTemperature, Biome aBiome, boolean aHasSky, boolean aDay, boolean aNight, Random aRandom) {
			CompoundTag rBumbleTag = UT.NBT.make();
			setHumidityMin(rBumbleTag, aBiome.rainfall - 0.10F - aRandom.nextInt(41)/100.0F);
			setHumidityMax(rBumbleTag, aBiome.rainfall + 0.10F + aRandom.nextInt(41)/100.0F);
			setTemperatureMin(rBumbleTag, aTemperature - 15 - aRandom.nextInt(31));
			setTemperatureMax(rBumbleTag, aTemperature + 15 + aRandom.nextInt(31));
			setOffspring     (rBumbleTag,    1+aRandom.nextInt(     4));
			setWorkForce     (rBumbleTag,    1+aRandom.nextInt( 10000));
			setAggressiveness(rBumbleTag,  100+aRandom.nextInt(  9901));
			setLifeSpan      (rBumbleTag, 1200+aRandom.nextInt(142801));
			// Making sure no errors can happen by turning the false,false case into a true,true case.
			setDayActive     (rBumbleTag, aDay   || !aNight);
			setNightActive   (rBumbleTag, aNight || !aDay  );
			if (aHasSky) {
				setOutsideActive(rBumbleTag, T);
				if (aRandom.nextInt(10000) < aBiome.rainfall * 10000) setRainproof(rBumbleTag, T);
				if (aRandom.nextInt(20000) < aBiome.rainfall * 10000) setStormproof(rBumbleTag, T);
			} else {
				setInsideActive(rBumbleTag, T);
			}
			return rBumbleTag;
		}
		
		public static void setHumidityMin       (CompoundTag aBumbleTag, float aHumidity)        {aBumbleTag.setFloat("minhum", aHumidity < 0.01F ? 0     : aHumidity);}
		public static void setHumidityMax       (CompoundTag aBumbleTag, float aHumidity)        {aBumbleTag.setFloat("maxhum", aHumidity < 0.01F ? 0.01F : aHumidity);}
		public static void setTemperatureMin    (CompoundTag aBumbleTag, long aTemperature)      {UT.NBT.setNumber( aBumbleTag, "mintemp"    , aTemperature);}
		public static void setTemperatureMax    (CompoundTag aBumbleTag, long aTemperature)      {UT.NBT.setNumber( aBumbleTag, "maxtemp"    , aTemperature);}
		public static void setOffspring         (CompoundTag aBumbleTag, long aOffspring)        {UT.NBT.setNumber( aBumbleTag, "offspring"  , UT.Code.bindStack(aOffspring));}
		public static void setAggressiveness    (CompoundTag aBumbleTag, long aAggressiveness)   {UT.NBT.setNumber( aBumbleTag, "aggro"      , UT.Code.bind(1, 10000, aAggressiveness));}
		public static void setWorkForce         (CompoundTag aBumbleTag, long aWorkForce)        {UT.NBT.setNumber( aBumbleTag, "work"       , UT.Code.bind(1, 10000, aWorkForce));}
		public static void setLifeSpan          (CompoundTag aBumbleTag, long aLifeSpan)         {UT.NBT.setNumber( aBumbleTag, "life"       , UT.Code.bind(1200, 144000, aLifeSpan));}
		public static void setRainproof         (CompoundTag aBumbleTag, boolean aRainproof)     {UT.NBT.setBoolean(aBumbleTag, "rain"       , aRainproof);}
		public static void setStormproof        (CompoundTag aBumbleTag, boolean aStormproof)    {UT.NBT.setBoolean(aBumbleTag, "storm"      , aStormproof);}
		public static void setDayActive         (CompoundTag aBumbleTag, boolean aDayActive)     {UT.NBT.setBoolean(aBumbleTag, "day"        , aDayActive);}
		public static void setNightActive       (CompoundTag aBumbleTag, boolean aNightActive)   {UT.NBT.setBoolean(aBumbleTag, "night"      , aNightActive);}
		public static void setOutsideActive     (CompoundTag aBumbleTag, boolean aOutsideActive) {UT.NBT.setBoolean(aBumbleTag, "outside"    , aOutsideActive);}
		public static void setInsideActive      (CompoundTag aBumbleTag, boolean aInsideActive)  {UT.NBT.setBoolean(aBumbleTag, "inside"     , aInsideActive);}
		
		public static float getHumidityMin      (CompoundTag aBumbleTag) {return Math.max(0    , aBumbleTag.getFloat("minhum"));}
		public static float getHumidityMax      (CompoundTag aBumbleTag) {return Math.max(0.01F, aBumbleTag.getFloat("maxhum"));}
		public static long getTemperatureMin    (CompoundTag aBumbleTag) {return aBumbleTag.getLong("mintemp");}
		public static long getTemperatureMax    (CompoundTag aBumbleTag) {return aBumbleTag.getLong("maxtemp");}
		public static long getOffspring         (CompoundTag aBumbleTag) {return UT.Code.bindStack(aBumbleTag.getLong("offspring"));}
		public static long getAggressiveness    (CompoundTag aBumbleTag) {return UT.Code.bind(  100,  10000, aBumbleTag.getLong("aggro"));}
		public static long getWorkForce         (CompoundTag aBumbleTag) {return UT.Code.bind(    1,  10000, aBumbleTag.getLong("work"));}
		public static long getLifeSpan          (CompoundTag aBumbleTag) {return UT.Code.bind( 1200, 144000, aBumbleTag.getLong("life"));}
		public static boolean getRainproof      (CompoundTag aBumbleTag) {return aBumbleTag.getBoolean("rain");}
		public static boolean getStormproof     (CompoundTag aBumbleTag) {return aBumbleTag.getBoolean("storm");}
		public static boolean getDayActive      (CompoundTag aBumbleTag) {return aBumbleTag.getBoolean("day");}
		public static boolean getNightActive    (CompoundTag aBumbleTag) {return aBumbleTag.getBoolean("night");}
		public static boolean getOutsideActive  (CompoundTag aBumbleTag) {return aBumbleTag.getBoolean("outside");}
		public static boolean getInsideActive   (CompoundTag aBumbleTag) {return aBumbleTag.getBoolean("inside");}
	}
}
