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

package gregtech.compat;

import static gregapi.data.CS.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import gregapi.api.Abstract_Mod;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.block.multitileentity.MultiTileEntityRegistry;
import gregapi.code.ModData;
import gregapi.compat.CompatMods;
import gregapi.data.ANY;
import gregapi.data.IL;
import gregapi.data.MD;
import gregapi.data.MT;
import gregapi.data.OD;
import gregapi.data.OP;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;

/**
 * Puts More Red behind the GregTech 6 progression: the mod adds redstone logic, which GT6 lacks, and
 * nothing else. Mechanical power, item logistics and the mod's own tooling are removed, and the parts
 * that stay are rebuilt out of GT6 components.
 */
public class Compat_Recipes_MoreRed extends CompatMods {
	public Compat_Recipes_MoreRed(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}

	/** Mod packages whose blocks leave the game entirely: GT6 already carries these systems. */
	private static final String[] DROPPED_PACKAGES = {".mechanisms.", ".transportation."};
	/** Kept from those packages: they are the seam to GT6 rotation, rebuilt below out of GT6 parts. */
	private static final String[] KEPT_ANYWAY = {"windcatcher", "alternator"};

	private static final short AXLE_WOOD_SMALL = 24800, AXLE_WOOD_MEDIUM = 24801;

	@Override public void onPostLoad(FMLPostInitializationEvent aInitEvent) {
		OUT.println("GT_Mod: Doing More Red Recipes.");

		// The red alloy is the same alloy GT6 already smelts (copper + 4 redstone): the mod's item
		// becomes the unification target, so every GT6 path that yields ingotRedAlloy yields it —
		// exactly how the original handled the other RedPower descendants.
		// The mod alloys its ingot out of COPPER (its own red_alloyable_ingots tag is c:ingots/copper), so it is
		// GT6's RedAlloy (Cu+Redstone), not EnderIO's RedstoneAlloy (Si+Redstone) that the similarly named
		// convention tag c:ingots/redstone_alloy pulled it under. The mod item stays the carrier, which is how
		// GT6 has always held foreign materials (OreDictTags.java:81-84), and the only way to it is the GT6 one.
		OreDictManager.INSTANCE.setTarget(OP.ingot, MT.RedAlloy, MD.MR, "red_alloy_ingot", 0);

		// Паспорта материалов базовых предметов мода. GT6 выводит состав крафта из состава его ингредиентов,
		// а у плит, проводов и кабелей мода материала не было — и подсказка о содержимом врала на всём, что
		// из них собрано. Доли посчитаны по рецептам самого мода, а не назначены.
		OM.data(ST.make(MD.MR, "stone_plate"        , 1, 0), new OreDictItemData(MT.Stone   , U8));  // 3 слэба -> 12 плит
		OM.data(ST.make(MD.MR, "red_alloy_wire"     , 1, 0), new OreDictItemData(MT.RedAlloy, U4));  // 3 слитка -> 12 проводов
		for (net.minecraft.world.item.DyeColor tColor : net.minecraft.world.item.DyeColor.values()) {
			OM.data(ST.make(MD.MR, tColor + "_cable", 1, 0), new OreDictItemData(MT.RedAlloy, U4)); // 8 проводов + шерсть -> 8 кабелей
		}
		OM.data(ST.make(MD.MR, "bundled_cable"      , 1, 0), new OreDictItemData(MT.RedAlloy, U4)); // 3 кабеля -> 3 пучка
		OM.data(ST.make(MD.MR, "redwire_post"       , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U)));
		OM.data(ST.make(MD.MR, "redwire_relay"      , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U), new OreDictMaterialStack(MT.Stone, 3*U4)));
		OM.data(ST.make(MD.MR, "redwire_junction"   , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U), new OreDictMaterialStack(MT.Redstone, 2*U), new OreDictMaterialStack(MT.Stone, 3*U4)));
		OM.data(ST.make(MD.MR, "cable_relay"        , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U4)));
		OM.data(ST.make(MD.MR, "cable_junction"     , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, 3*U4), new OreDictMaterialStack(MT.Stone, 3*U4)));
		OM.data(ST.make(MD.MR, "redwire_spool"      , 1, 0), new OreDictItemData(MT.Fe, 4*U, new OreDictMaterialStack(MT.RedAlloy, 3*U4), new OreDictMaterialStack(MT.Wood, U)));
		OM.data(ST.make(MD.MR, "bundled_cable_spool", 1, 0), new OreDictItemData(MT.Fe, 4*U, new OreDictMaterialStack(MT.RedAlloy, 3*U4), new OreDictMaterialStack(MT.Wood, U)));

		// Обычные вентили мода собираются его же рецептами, которых GT6 не разбирает, поэтому состав им тоже
		// задаётся здесь. Четверть-слэб гладкого камня = U/4 камня, факел = U редстоуна и U/2 дерева, пыль = U.
		gate("and_gate"            , 5*U, U     , 2*U   , 0);
		gate("diode"               , 3*U, 3*U4  , U     , 0);
		gate("not_gate"            , 3*U, 3*U4  , U2    , 0);
		gate("latch"               , 4*U, U     , U     , 0);
		gate("nand_gate"           , 5*U, U     , 3*U2  , 0);
		gate("nor_gate"            , 5*U, U     , U2    , 0);
		gate("or_gate"             , 5*U, U     , U     , 0);
		gate("xor_gate"            , 4*U, 5*U4  , 3*U2  , 0);
		gate("xnor_gate"           , 4*U, 5*U4  , 2*U   , 0);
		gate("two_input_and_gate"  , 4*U, 5*U4  , 3*U2  , 0);
		gate("two_input_nand_gate" , 4*U, 5*U4  , U     , 0);
		gate("pulse_gate"          , 2*U, 3*U4  , 0     , U);
		gate("multiplexer"         , 4*U, U     , 0     , U);
		CR.remout(MD.MR, "red_alloy_ingot");

		// Провод мода собирался из ТРЁХ СЛИТКОВ В РЯД, а ту же сетку занимает авторский разбор GT6
		// (AdvancedCraftingXToY: 3 слитка -> 27 самородков, Loader_Recipes_Handlers), причём безформенный —
		// он берёт любые три слитка. Кто из двух ответит игроку, решает порядок рецептов, а он не задан:
		// на одной машине выходил провод, на другой самородки. Спор снимается сменой сетки, а не борьбой за
		// приоритет: пластина режется кусачками — приём GT6, и этой сетки не занимает никто.
		CR.remout(MD.MR, "red_alloy_wire");
		// Сторона важна: у GT6 «пластина, затем кусачки» — его собственный жест, поэтому провод мода берёт
		// ЗЕРКАЛЬНУЮ пару «кусачки, затем пластина». Зеркалирования во флаге нет (DEF = BUF|NO_REM), значит
		// две сетки останутся разными, и спорить им не о чем.
		CR.shaped(ST.make(MD.MR, "red_alloy_wire", 4, 0), CR.DEF_REV_NCC, "xP", 'P', OP.plate.dat(MT.RedAlloy));


		// Whole systems GT6 owns: mechanical power and item logistics, plus the mod's own bench and pliers.
		int tDropped = dropByPackage();
		CR.remout(MD.MR, "soldering_table", "pliers");
		OUT.println("GT_Mod: More Red — " + tDropped + " machine and logistics outputs sent for removal.");

		MultiTileEntityRegistry tReg = MultiTileEntityRegistry.getRegistry("gt.multitileentity");
		ItemStack tAxleMedium = tReg == null ? null : tReg.getItem(AXLE_WOOD_MEDIUM, 1);
		ItemStack tAxleSmall  = tReg == null ? null : tReg.getItem(AXLE_WOOD_SMALL , 1);

		// The windcatcher stays as a rotation source for GT6 axles, so its recipe is built from GT6 parts.
		if (ST.valid(tAxleMedium)) for (Item tItem : woodenVariants("windcatcher")) {
			CR.shaped(ST.make(tItem, 1, 0), CR.DEF_REV_NCC, "RTR", "GAG", "WTW",
				'R', OP.stickLong.dat(ANY.Cu), 'T', OP.rotor.dat(ANY.Cu), 'G', OP.gearGt.dat(ANY.Cu),
				'A', tAxleMedium, 'W', net.minecraft.world.level.block.Blocks.WHITE_WOOL);
		}

		// The alternator turns rotation into a redstone signal — the one bridge worth keeping.
		if (ST.valid(tAxleSmall)) {
			CR.shaped(ST.make(MD.MR, "alternator", 1, 0), CR.DEF_REV_NCC, "PTP", "TAT", "PTP",
				'P', OP.plate.dat(ANY.Cu), 'T', OD.craftingRedstoneTorch, 'A', tAxleSmall);
		}

		// The soldering bench is gone, so its whole recipe type goes with it — otherwise the recipe viewer
		// keeps showing a station the player can no longer build.
		CR.remoutType(MD.MR, "soldering");

		// Bitwise gates existed only as soldering recipes. They come back on a GT6 circuit, and each gate gets
		// its OWN grid: one shared pattern would make the individual gate unobtainable.
		Object tCable = ST.make(MD.MR, "bundled_cable", 1, 0), tPlate = ST.make(MD.MR, "stone_plate", 1, 0);
		// A tier-1 circuit also matches tier 2 and up: LoaderOreDictReRegistrations.java:376 re-registers them down.
		String[][] tGates = {
			{"bitwise_and_gate"   , "BTP", "TCT", "PTP"},
			{"bitwise_diode"      , "B  ", "TCT", "PPP"},
			{"bitwise_multiplexer", "BRP", "RCR", "PRP"},
			{"bitwise_not_gate"   , "B  ", "RCR", "PPP"},
			{"bitwise_or_gate"    , "BTP", "RCR", "PRP"},
			{"bitwise_xnor_gate"  , "BTP", "TCT", "PPP"},
			{"bitwise_xor_gate"   , "BRP", "TCT", "PPP"},
		};
		for (String[] tGate : tGates) {
			CR.shaped(ST.make(MD.MR, tGate[0], 1, 0), CR.DEF_REV_NCC, tGate[1], tGate[2], tGate[3],
				'B', tCable, 'T', OD.craftingRedstoneTorch, 'R', OD.itemRedstone, 'C', OD_CIRCUITS[1], 'P', tPlate);
		}
		CR.shaped(ST.make(MD.MR, "hexidecrubrometer", 1, 0), CR.DEF_REV_NCC, "CGC", "PPP",
			'C', OD_CIRCUITS[2], 'G', OD.blockGlass, 'P', ST.make(MD.MR, "stone_plate", 1, 0));
	}

	/** One shape for every plate gate of the mod: redstone carries the item, the rest rides along as by-products. */
	private static void gate(String aName, long aRedstone, long aStone, long aWood, long aIron) {
		java.util.List<OreDictMaterialStack> tBy = new ArrayList<>();
		if (aStone > 0) tBy.add(new OreDictMaterialStack(MT.Stone, aStone));
		if (aWood  > 0) tBy.add(new OreDictMaterialStack(MT.Wood , aWood ));
		if (aIron  > 0) tBy.add(new OreDictMaterialStack(MT.Fe   , aIron ));
		OM.data(ST.make(MD.MR, aName, 1, 0), new OreDictItemData(MT.Redstone, aRedstone, tBy.toArray(new OreDictMaterialStack[0])));
	}

	/** Removal goes by the mod's own package layout, so a block added later drops with its family. */
	private int dropByPackage() {
		int rCount = 0;
		for (Block tBlock : BuiltInRegistries.BLOCK) {
			Identifier tKey = BuiltInRegistries.BLOCK.getKey(tBlock);
			if (tKey == null || !MD.MR.mID.equals(tKey.getNamespace())) continue;
			String tClass = tBlock.getClass().getName();
			boolean tDrop = false;
			for (String tPackage : DROPPED_PACKAGES) if (tClass.contains(tPackage)) tDrop = true;
			for (String tKept : KEPT_ANYWAY) if (tKey.getPath().contains(tKept)) tDrop = false;
			if (!tDrop) continue;
			// Возврат remout говорит лишь о собственном реестре GT6; чужие рецепты снимает его
			// датапак-плечо на старте сервера, поэтому считаются поданные заявки.
			ItemStack tStack = ST.make(tBlock, 1, 0);
			if (ST.valid(tStack)) {CR.remout(tStack); rCount++;}
		}
		return rCount;
	}

	private List<Item> woodenVariants(String aSuffix) {
		List<Item> rItems = new ArrayList<>();
		for (Item tItem : BuiltInRegistries.ITEM) {
			Identifier tKey = BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey != null && MD.MR.mID.equals(tKey.getNamespace()) && tKey.getPath().endsWith(aSuffix)) rItems.add(tItem);
		}
		return rItems;
	}
}
