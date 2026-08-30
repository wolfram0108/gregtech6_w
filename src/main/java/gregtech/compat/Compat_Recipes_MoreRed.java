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

import gregapi.api.Abstract_Mod;
import gregapi.api.FMLPostInitializationEvent;
import gregapi.code.ModData;
import gregapi.compat.CompatMods;
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
 * More Red fitted into the GT6 progression. This version of the mod carries no mechanisms and no tubes at
 * all — only redstone logic — so the adaptation reduces to three things: the soldering bench goes, the
 * gates it used to make come back on GT6 circuits, and the red alloy is GT6's one.
 */
public class Compat_Recipes_MoreRed extends CompatMods {
	public Compat_Recipes_MoreRed(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}

	@Override public void onPostLoad(FMLPostInitializationEvent aInitEvent) {OUT.println("GT_Mod: Doing More Red Recipes.");
		// The mod alloys its ingot out of COPPER (its own red_alloyable_ingots tag is forge:ingots/copper), so it
		// is GT6's RedAlloy, and the mod item stays the carrier — how GT6 has always held foreign materials.
		// The only way to it is the GT6 one: the mod's own path leaves with the bench below.
		OreDictManager.INSTANCE.setTarget(OP.ingot, MT.RedAlloy, MD.MR, "red_alloy_ingot", 0);
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


		// The bench is a second crafting progression beside GT6's own, so it goes — and its whole recipe type
		// with it, or the recipe viewer keeps showing a station the player can no longer build.
		CR.remout(MD.MR, "soldering_table");
		CR.remoutType(MD.MR, "soldering");

		// Material passports for the mod's own parts. GT6 derives a craft's content from the content of its
		// ingredients, and without these the plates and wires count as nothing, making every tooltip built on
		// them wrong. The shares are read off the mod's own recipes, not assigned.
		OM.data(ST.make(MD.MR, "stone_plate"              , 1, 0), new OreDictItemData(MT.Stone   , U8));  // 3 слэба -> 12 плит
		OM.data(ST.make(MD.MR, "red_alloy_wire"           , 1, 0), new OreDictItemData(MT.RedAlloy, U4));  // 3 слитка -> 12 проводов
		// 8 проводов и одна шерсть дают 8 кабелей, то есть кабель это ровно один провод.
		for (net.minecraft.world.item.DyeColor tColor : net.minecraft.world.item.DyeColor.values()) {
			OM.data(ST.make(MD.MR, tColor + "_network_cable", 1, 0), new OreDictItemData(MT.RedAlloy, U4));
		}
		OM.data(ST.make(MD.MR, "bundled_network_cable"    , 1, 0), new OreDictItemData(MT.RedAlloy, U4));  // 3 кабеля -> 3 пучка
		OM.data(ST.make(MD.MR, "redwire_post"             , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U)));
		OM.data(ST.make(MD.MR, "redwire_post_plate"       , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U), new OreDictMaterialStack(MT.Stone, 3*U4)));
		OM.data(ST.make(MD.MR, "redwire_post_relay_plate" , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U), new OreDictMaterialStack(MT.Redstone, 2*U), new OreDictMaterialStack(MT.Stone, 3*U4)));
		OM.data(ST.make(MD.MR, "bundled_cable_post"       , 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, U4)));
		OM.data(ST.make(MD.MR, "bundled_cable_relay_plate", 1, 0), new OreDictItemData(MT.Fe, U, new OreDictMaterialStack(MT.RedAlloy, 3*U4), new OreDictMaterialStack(MT.Stone, 3*U4)));
		OM.data(ST.make(MD.MR, "redwire_spool"            , 1, 0), new OreDictItemData(MT.Fe, 4*U, new OreDictMaterialStack(MT.RedAlloy, 3*U4), new OreDictMaterialStack(MT.Wood, U)));
		OM.data(ST.make(MD.MR, "bundled_cable_spool"      , 1, 0), new OreDictItemData(MT.Fe, 4*U, new OreDictMaterialStack(MT.RedAlloy, 3*U4), new OreDictMaterialStack(MT.Wood, U)));

		// The plate gates are built by the mod's own recipes, which GT6 does not read, so their content is
		// stated here too. Quarter slab = U/4 stone, torch = U redstone and U/2 wood, dust = U.
		gate("and_gate"      , 5*U, U   , 2*U  , 0);
		gate("diode"         , 3*U, 3*U4, U    , 0);
		gate("not_gate"      , 3*U, 3*U4, U2   , 0);
		gate("latch"         , 4*U, U   , U    , 0);
		gate("nand_gate"     , 5*U, U   , 3*U2 , 0);
		gate("nor_gate"      , 5*U, U   , U2   , 0);
		gate("or_gate"       , 5*U, U   , U    , 0);
		gate("xor_gate"      , 4*U, 5*U4, 3*U2 , 0);
		gate("xnor_gate"     , 4*U, 5*U4, 2*U  , 0);
		gate("and_2_gate"    , 4*U, 5*U4, 3*U2 , 0);
		gate("nand_2_gate"   , 4*U, 5*U4, U    , 0);
		gate("pulse_gate"    , 2*U, 3*U4, 0    , U);
		gate("multiplexer"   , 4*U, U   , 0    , U);

		// Bitwise gates existed only as soldering recipes. They come back on a GT6 circuit, and each gate gets
		// its OWN grid: one shared pattern would make the individual gate unobtainable. This version of the mod
		// has six of them — no bitwise multiplexer.
		Object tCable = ST.make(MD.MR, "bundled_network_cable", 1, 0), tPlate = ST.make(MD.MR, "stone_plate", 1, 0);
		// A tier-1 circuit also matches tier 2 and up: LoaderOreDictReRegistrations re-registers them down.
		String[][] tGates = {
			{"bitwise_and_gate" , "BTP", "TCT", "PTP"},
			{"bitwise_diode"    , "B  ", "TCT", "PPP"},
			{"bitwise_not_gate" , "B  ", "RCR", "PPP"},
			{"bitwise_or_gate"  , "BTP", "RCR", "PRP"},
			{"bitwise_xnor_gate", "BTP", "TCT", "PPP"},
			{"bitwise_xor_gate" , "BRP", "TCT", "PPP"},
		};
		for (String[] tGate : tGates) {
			CR.shaped(ST.make(MD.MR, tGate[0], 1, 0), CR.DEF_REV_NCC, tGate[1], tGate[2], tGate[3],
				'B', tCable, 'T', OD.craftingRedstoneTorch, 'R', OD.itemRedstone, 'C', OD_CIRCUITS[1], 'P', tPlate);
		}
		CR.shaped(ST.make(MD.MR, "hexidecrubrometer", 1, 0), CR.DEF_REV_NCC, "CGC", "PPP",
			'C', OD_CIRCUITS[2], 'G', OD.blockGlass, 'P', tPlate);
	}

	/** One shape for every plate gate of the mod: redstone carries the item, the rest rides along as by-products. */
	private static void gate(String aName, long aRedstone, long aStone, long aWood, long aIron) {
		List<OreDictMaterialStack> tBy = new ArrayList<>();
		if (aStone > 0) tBy.add(new OreDictMaterialStack(MT.Stone, aStone));
		if (aWood  > 0) tBy.add(new OreDictMaterialStack(MT.Wood , aWood ));
		if (aIron  > 0) tBy.add(new OreDictMaterialStack(MT.Fe   , aIron ));
		OM.data(ST.make(MD.MR, aName, 1, 0), new OreDictItemData(MT.Redstone, aRedstone, tBy.toArray(new OreDictMaterialStack[0])));
	}
}
