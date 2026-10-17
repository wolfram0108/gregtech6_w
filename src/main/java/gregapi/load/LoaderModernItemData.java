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

package gregapi.load;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictItemData;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static gregapi.data.CS.*;

/**
 * Central material passport registry for modern Minecraft items and blocks (1.20.1 backport branch).
 * Covers animal armor, netherite equipment, redstone automation, rails, and smithing trims.
 */
public class LoaderModernItemData {

	public static void run() {
		registerAnimalArmorAndLeather();
		registerNetheriteGear();
		registerModernUtilities();
		registerRailsAndTransport();
		registerRedstoneAndAutomation();
		registerSmithingTemplates();
		inheritWaxedAndWeatheredPassports();
	}

	/** Animal armors and modern leather goods. */
	private static void registerAnimalArmorAndLeather() {
		OM.dat2(ST.make(Items.LEATHER_HORSE_ARMOR          , 1, W), MT.Leather  , U * 7);
		OM.data(ST.make(Items.BUNDLE                       , 1, W), MT.Leather  , U);
	}

	/** Netherite tools, weapons, armor, and blocks (base diamond item + netherite ingot). */
	private static void registerNetheriteGear() {
		// Tools and weapons with durability scaling.
		OM.dat2(ST.make(Items.NETHERITE_SWORD              , 1, 0), ANY.Diamond , OP.toolHeadSword  .mAmount, OM.stack(ANY.Wood, U2), OM.stack(MT.Netherite, U));
		OM.dat2(ST.make(Items.NETHERITE_PICKAXE            , 1, 0), ANY.Diamond , OP.toolHeadPickaxe.mAmount, OM.stack(ANY.Wood, U ), OM.stack(MT.Netherite, U));
		OM.dat2(ST.make(Items.NETHERITE_SHOVEL             , 1, 0), ANY.Diamond , OP.toolHeadShovel .mAmount, OM.stack(ANY.Wood, U ), OM.stack(MT.Netherite, U));
		OM.dat2(ST.make(Items.NETHERITE_AXE                , 1, 0), ANY.Diamond , OP.toolHeadAxe    .mAmount, OM.stack(ANY.Wood, U ), OM.stack(MT.Netherite, U));
		OM.dat2(ST.make(Items.NETHERITE_HOE                , 1, 0), ANY.Diamond , OP.toolHeadHoe    .mAmount, OM.stack(ANY.Wood, U ), OM.stack(MT.Netherite, U));

		// Armor with durability scaling.
		OM.dat2(ST.make(Items.NETHERITE_HELMET             , 1, 0), ANY.Diamond , U * 5, OM.stack(MT.Netherite, U));
		OM.dat2(ST.make(Items.NETHERITE_CHESTPLATE         , 1, 0), ANY.Diamond , U * 8, OM.stack(MT.Netherite, U));
		OM.dat2(ST.make(Items.NETHERITE_LEGGINGS           , 1, 0), ANY.Diamond , U * 7, OM.stack(MT.Netherite, U));
		OM.dat2(ST.make(Items.NETHERITE_BOOTS              , 1, 0), ANY.Diamond , U * 4, OM.stack(MT.Netherite, U));

		// Netherite utility block.
		OM.data(ST.make(Blocks.LODESTONE                   , 1, W), MT.Netherite, U, ANY.Stone, U * 8);
	}

	/** Modern copper utilities and items. */
	private static void registerModernUtilities() {
		OM.data(ST.make(Blocks.LIGHTNING_ROD               , 1, W), MT.Cu       , U * 3);
		OM.data(ST.make(Items.SPYGLASS                     , 1, 0), MT.Cu       , U * 2, MT.Amethyst, U);
		OM.data(ST.make(Items.BRUSH                        , 1, 0), MT.Cu       , U, ANY.Wood, U2);
	}

	/** Rails and transportation items (16 rails / 6 powered rails batches). */
	private static void registerRailsAndTransport() {
		OM.data(ST.make(Blocks.RAIL                        , 1, W), ANY.Fe      , 6 * U / 16, ANY.Wood, U2 / 16);
		OM.data(ST.make(Blocks.POWERED_RAIL                , 1, W), MT.Au       , U, MT.Redstone, U / 6, ANY.Wood, U2 / 6);
		OM.data(ST.make(Blocks.DETECTOR_RAIL               , 1, W), ANY.Fe      , U, MT.Redstone, U / 6, ANY.Stone, U2 / 6);
		OM.data(ST.make(Blocks.ACTIVATOR_RAIL              , 1, W), ANY.Fe      , U, MT.Redstone, U / 6, ANY.Wood, U / 6);
	}

	/** Redstone automation devices, comparators, repeaters, observers, and workstations. */
	private static void registerRedstoneAndAutomation() {
		OM.data(ST.make(Blocks.SMITHING_TABLE              , 1, W), ANY.Fe      , U * 2, ANY.Wood, U * 4);
		OM.data(ST.make(Blocks.COMPARATOR                  , 1, W), ANY.Stone   , U * 3, MT.Redstone, U * 3, MT.NetherQuartz, U);
		OM.data(ST.make(Blocks.REPEATER                    , 1, W), ANY.Stone   , U * 3, MT.Redstone, U * 3);
		OM.data(ST.make(Blocks.OBSERVER                    , 1, W), ANY.Stone   , U * 6, MT.Redstone, U * 2, MT.NetherQuartz, U);
		OM.data(ST.make(Blocks.TARGET                      , 1, W), MT.Redstone , U * 4, MT.Wheat, U * 9);
		OM.data(ST.make(Blocks.LECTERN                     , 1, W), ANY.Wood    , U * 2, MT.Paper, U * 9);
		OM.data(ST.make(Blocks.CALIBRATED_SCULK_SENSOR     , 1, W), MT.Amethyst , U * 3);
	}

	/** Smithing templates (netherite upgrade and armor trims duplication recipe = 7 diamonds). */
	private static void registerSmithingTemplates() {
		for (Item tItem : BuiltInRegistries.ITEM) {
			ResourceLocation tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID != null && "minecraft".equals(tID.getNamespace()) && tID.getPath().endsWith("_smithing_template")) {
				OM.data(ST.make(tItem, 1, 0), ANY.Diamond, U * 7);
			}
		}
	}

	/** Inherits copper passports for all waxed and weathered block/item variants. */
	private static void inheritWaxedAndWeatheredPassports() {
		for (Item tItem : BuiltInRegistries.ITEM) {
			ResourceLocation tID = BuiltInRegistries.ITEM.getKey(tItem);
			if (tID == null || !"minecraft".equals(tID.getNamespace()) || !tID.getPath().contains("copper")) continue;
			String tBase = tID.getPath();
			boolean tWasPrefixed = false;
			for (boolean tChanged = true; tChanged; ) {
				tChanged = false;
				for (String tPrefix : new String[] {"waxed_", "exposed_", "weathered_", "oxidized_"}) {
					if (tBase.startsWith(tPrefix)) {
						tBase = tBase.substring(tPrefix.length());
						tChanged = true;
						tWasPrefixed = true;
					}
				}
			}
			if (!tWasPrefixed) continue;
			Item tBaseItem = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", tBase));
			if (tBaseItem != null && tBaseItem != tItem && tBaseItem != Items.AIR) {
				ItemStack tBaseStack = ST.make(tBaseItem, 1, 0);
				OreDictItemData tBaseData = OM.anydata(tBaseStack);
				if (tBaseData != null && tBaseData.validMaterial()) {
					ItemStack tCurrentStack = ST.make(tItem, 1, 0);
					if (OM.anydata(tCurrentStack) == null) {
						OM.data(tCurrentStack, tBaseData.copy());
					}
				}
			}
		}
	}
}
