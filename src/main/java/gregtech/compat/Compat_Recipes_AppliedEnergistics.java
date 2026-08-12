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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregtech.compat;

import gregapi.api.FMLPostInitializationEvent;
import gregapi.api.Abstract_Mod;
import gregapi.code.ModData;
import gregapi.compat.AE2Names;
import gregapi.compat.CompatMods;
import gregapi.data.*;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.event.IOreDictListenerEvent;
import gregapi.oredict.event.OreDictListenerEvent_Names;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.level.block.Blocks;

import static gregapi.data.CS.*;

public class Compat_Recipes_AppliedEnergistics extends CompatMods {
	public Compat_Recipes_AppliedEnergistics(ModData aMod, Abstract_Mod aGTMod) {super(aMod, aGTMod);}
	
	@Override public void onPostLoad(FMLPostInitializationEvent aInitEvent) {OUT.println("GT_Mod: Doing AE Recipes.");
		// Э0 (AE2 26.1): три строки RM.ae_grinder(5, …) сняты — кварцевой мельницы у AE2 26.1 нет.

		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.AE_Cutter_Certus.wild(1), OP.ingot.mat(MT.Fe, 1)), ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 0, 21)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.AE_Cutter_Quartz.wild(1), OP.ingot.mat(MT.Fe, 1)), ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 0, 21)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		
		CR.shaped(ST.make(MD.AE, "tile.BlockQuartzGlass", 4, 0), CR.DEF_REM_REV_NCC, "QGQ", "GQG", "QGQ", 'G', OD.blockGlassColorless, 'Q', OP.dust.dat(ANY.Quartz));
		CR.shaped(ST.make(MD.AE, "tile.BlockQuartzLamp" , 1, 0), CR.DEF_REM_REV_NCC, "GQG", 'G', OP.dust.dat(ANY.Glowstone), 'Q', ST.make(MD.AE, "tile.BlockQuartzGlass", 1, 0));
		
		// Э2 (центр адресации gregapi.compat.AE2Names): у меты 10 — «очищенного» кристалла сертуса — носителя в
		// AE2 26.1 НЕТ (объявлен AEItemIds.java:203, но не зарегистрирован). Запись пропускается ТИХО: подача
		// null входом дала бы «ERROR: Recipe has no Inputs!». Тот же выход (печать 16) остаётся достижим
		// строкой ниже — через грегскую пластину сертуса, которую Грег и поставил вторым путём.
		if (AE2Names.has("item.ItemMultiMaterial", 10))
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 13), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 10), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 16));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 13), OP.plateGem.mat(MT.CertusQuartz            , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 16));
		for (OreDictMaterial tMat : ANY.Diamond.mToThis)
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 14), OP.plateGem.mat(tMat                       , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 17));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 15), OP.plate   .mat(MT.Au                      , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 18));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 19), OP.plate   .mat(MT.Si                      , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 19), OP.plateGem.mat(MT.Si                      , 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20));
		
		for (OreDictMaterial tMat : ANY.Iron.mToThis) if (tMat != MT.Enori) {
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 13), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 13));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 14), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 14));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 15), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 15));
		RM.Press        .addRecipe2(T, 16,   64, ST.make(MD.AE, "item.ItemMultiMaterial", 0, 19), OP.blockSolid.mat(tMat, 1), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 19));
		
		RM.sawing(16, 16, F, 10, OP.ingot.mat(tMat     , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		}
		for (OreDictMaterial tMat : ANY.Cu.mToThis)
		RM.sawing(16, 16, F, 10, OP.ingot.mat(tMat     , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Sn    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Pb    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Ag    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Ni    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Al    , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Brass , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Bronze, 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		RM.sawing(16, 16, F, 10, OP.ingot.mat(MT.Invar , 1), ST.make(MD.AE, "item.ItemMultiPart", 3, 120));
		
		RM.Press        .addRecipeX(T, 16,   64, ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 1, 16), OM.dust(MT.Redstone), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20)), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 23));
		RM.Press        .addRecipeX(T, 16,   64, ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 1, 17), OM.dust(MT.Redstone), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20)), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 24));
		RM.Press        .addRecipeX(T, 16,   64, ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 1, 18), OM.dust(MT.Redstone), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 20)), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 22));
		
		// Э2: семян кристаллов (item.ItemCrystalSeed) в AE2 26.1 нет вовсе — рост заменён budding-блоками.
		// Три рецепта автоклава «семя + пар + вода → очищенный кристалл» опираются на носитель И на входе,
		// И на выходе (меты 10/11/12 — те же «очищенные»), поэтому вся тройка тихо не регистрируется.
		if (AE2Names.has("item.ItemCrystalSeed", 0)) {
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1,    0), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 10));
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1,  600), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 11));
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1, 1200), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 12));
		}
		
		RM.Compressor   .addRecipe1(T, 16,   16, OP.gem.mat(MT.CertusQuartz                 , 4), ST.make(MD.AE, "tile.BlockQuartz", 1, 0));
		// Э2: три сжатия «8 очищенных кристаллов → блок» пропускаются тихо — носителя у мет 10/12/11 нет
		// (см. AE2Names). Обе плитки (кварц AE2 и ванильный кварцевый блок) остаются достижимы строками
		// с грегскими гемами, которые Грег и поставил рядом.
		if (AE2Names.has("item.ItemMultiMaterial", 10))
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 10), ST.make(MD.AE, "tile.BlockQuartz", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, OP.gem.mat(MT.Fluix                        , 4), ST.make(MD.AE, "tile.BlockFluix", 1, 0));
		if (AE2Names.has("item.ItemMultiMaterial", 12))
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 12), ST.make(MD.AE, "tile.BlockFluix", 1, 0));
		if (AE2Names.has("item.ItemMultiMaterial", 11))
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 11), ST.make(Blocks.QUARTZ_BLOCK, 1, 0));
		
		// Э2: выход у всех шести смешиваний — семя кристалла, носителя нет (см. выше). Тихо пропускаем.
		if (AE2Names.has("item.ItemCrystalSeed", 0)) {
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.CertusQuartz), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2,    0));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.NetherQuartz), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2,  600));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.Fluix       ), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2, 1200));
		if (IL.AETHER_Sand.exists()) {
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.CertusQuartz), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2,    0));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.NetherQuartz), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2,  600));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.Fluix       ), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2, 1200));
		}
		}
		
		RM.smash(ST.make(MD.AE, "tile.BlockQuartz"              , 1, W), OP.gem.mat(MT.CertusQuartz, 4));
		RM.smash(ST.make(MD.AE, "tile.BlockQuartzPillar"        , 1, W), OP.gem.mat(MT.CertusQuartz, 4));
		RM.smash(ST.make(MD.AE, "tile.BlockQuartzChiseled"      , 1, W), OP.gem.mat(MT.CertusQuartz, 4));
		RM.smash(ST.make(MD.AE, "tile.BlockFluix"               , 1, W), OP.gem.mat(MT.Fluix, 4));
		RM.smash(ST.make(MD.AE, "tile.QuartzStairBlock"         , 1, W), OP.gem.mat(MT.CertusQuartz, 6));
		RM.smash(ST.make(MD.AE, "tile.QuartzPillarStairBlock"   , 1, W), OP.gem.mat(MT.CertusQuartz, 6));
		RM.smash(ST.make(MD.AE, "tile.ChiseledQuartzStairBlock" , 1, W), OP.gem.mat(MT.CertusQuartz, 6));
		RM.smash(ST.make(MD.AE, "tile.FluixStairBlock"          , 1, W), OP.gem.mat(MT.Fluix, 6));
		RM.smash(ST.make(MD.AE, "tile.QuartzSlabBlock"          , 1, W), OP.gem.mat(MT.CertusQuartz, 2));
		RM.smash(ST.make(MD.AE, "tile.QuartzPillarSlabBlock"    , 1, W), OP.gem.mat(MT.CertusQuartz, 2));
		RM.smash(ST.make(MD.AE, "tile.ChiseledQuartzSlabBlock"  , 1, W), OP.gem.mat(MT.CertusQuartz, 2));
		RM.smash(ST.make(MD.AE, "tile.FluixSlabBlock"           , 1, W), OP.gem.mat(MT.Fluix, 2));
		
		
		RM.mortarize( 18, ST.make(MD.AE, "tile.BlockSkyStone", 1, W), OP.blockDust.mat(MT.STONES.SkyStone, 1));
		RM.mortarize(144, ST.make(MD.AE, "tile.BlockSkyChest", 1, W), OP.blockDust.mat(MT.STONES.SkyStone, 8));
		
		RM.stonetypes(MT.STONES.SkyStone, T, OP.rockGt.mat(MT.STONES.SkyStone, 4), OP.blockDust.mat(MT.STONES.SkyStone, 1)
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 0), ST.make(MD.AE, "tile.SkyStoneStairBlock"          , 1, 0), ST.make(MD.AE, "tile.SkyStoneSlabBlock"          , 1, 0), NI, NI)
		, NI
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 2), ST.make(MD.AE, "tile.SkyStoneBrickStairBlock"     , 1, 0), ST.make(MD.AE, "tile.SkyStoneBrickSlabBlock"     , 1, 0), NI, NI)
		, NI
		, NI
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 1), ST.make(MD.AE, "tile.SkyStoneBlockStairBlock"     , 1, 0), ST.make(MD.AE, "tile.SkyStoneBlockSlabBlock"     , 1, 0), NI, NI)
		, NI
		, RM.stoneshapes(MT.STONES.SkyStone, F, ST.make(MD.AE, "tile.BlockSkyStone", 1, 3), ST.make(MD.AE, "tile.SkyStoneSmallBrickStairBlock", 1, 0), ST.make(MD.AE, "tile.SkyStoneSmallBrickSlabBlock", 1, 0), NI, NI)
		);
		
		
		new OreDictListenerEvent_Names() {@Override public void addAllListeners() {
		// Э0 (AE2 26.1): 20 слушателей (gemCertusQuartz…ingotIron), тело которых состояло ТОЛЬКО из вызова
		// мельницы AE2, сняты вместе с ней — без вызова слушатель пуст. Четыре слушателя по линзам ниже
		// живы: они кормят RM.LaserEngraver, машину самого GT6.
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_White], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 13));
		}});
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_Cyan], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 14));
		}});
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_Yellow], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 15));
		}});
		addListener(DYE_OREDICTS_LENS[DYE_INDEX_Purple], new IOreDictListenerEvent() {@Override public void onOreRegistration(OreDictRegistrationContainer aEvent) {
			for (OreDictMaterial tMat : ANY.Fe.mToThis) if (tMat != MT.Enori)
			RM.LaserEngraver.addRecipe2(T,512,512, OP.blockSolid.mat(tMat, 1), ST.amount(0, aEvent.mStack), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 19));
		}});
		}};
	}
}
