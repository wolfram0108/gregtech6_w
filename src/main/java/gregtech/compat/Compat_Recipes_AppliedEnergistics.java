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
		// Э0 (слой AE2): три строки RM.ae_grinder(5, …) сняты — кварцевой мельницы у AE2 под 1.20.1 нет.

		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.AE_Cutter_Certus.wild(1), OP.ingot.mat(MT.Fe, 1)), ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 0, 21)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		RM.DidYouKnow.addFakeRecipe(F, ST.array(IL.AE_Cutter_Quartz.wild(1), OP.ingot.mat(MT.Fe, 1)), ST.array(ST.make(MD.AE, "item.ItemMultiMaterial", 0, 21)), null, ZL_LONG, ZL_FS, ZL_FS, 0, 0, 0);
		
		CR.shaped(ST.make(MD.AE, "tile.BlockQuartzGlass", 4, 0), CR.DEF_REM_REV_NCC, "QGQ", "GQG", "QGQ", 'G', OD.blockGlassColorless, 'Q', OP.dust.dat(ANY.Quartz));
		CR.shaped(ST.make(MD.AE, "tile.BlockQuartzLamp" , 1, 0), CR.DEF_REM_REV_NCC, "GQG", 'G', OP.dust.dat(ANY.Glowstone), 'Q', ST.make(MD.AE, "tile.BlockQuartzGlass", 1, 0));
		
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
		
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1,    0), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 10));
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1,  600), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 11));
		RM.Autoclave    .addRecipe2(T,  0, 1500, ST.make(MD.AE, "item.ItemCrystalSeed", 1, 1200), ST.tag(2), FL.Steam.make(48000), FL.DistW.make(225), ST.make(MD.AE, "item.ItemMultiMaterial", 1, 12));
		
		RM.Compressor   .addRecipe1(T, 16,   16, OP.gem.mat(MT.CertusQuartz                 , 4), ST.make(MD.AE, "tile.BlockQuartz", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 10), ST.make(MD.AE, "tile.BlockQuartz", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, OP.gem.mat(MT.Fluix                        , 4), ST.make(MD.AE, "tile.BlockFluix", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 12), ST.make(MD.AE, "tile.BlockFluix", 1, 0));
		RM.Compressor   .addRecipe1(T, 16,   16, ST.make(MD.AE, "item.ItemMultiMaterial", 8, 11), ST.make(Blocks.QUARTZ_BLOCK, 1, 0));
		
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.CertusQuartz), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2,    0));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.NetherQuartz), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2,  600));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.Fluix       ), ST.make(Blocks.SAND, 1, W), ST.make(MD.AE, "item.ItemCrystalSeed", 2, 1200));
		if (IL.AETHER_Sand.exists()) {
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.CertusQuartz), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2,    0));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.NetherQuartz), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2,  600));
		RM.Mixer        .addRecipe2(T, 16,   16, OM.dust(MT.Fluix       ), IL.AETHER_Sand     .get(1), ST.make(MD.AE, "item.ItemCrystalSeed", 2, 1200));
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
		// Э0 (слой AE2): 20 слушателей (gemCertusQuartz…ingotIron), тело которых состояло ТОЛЬКО из вызова
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

		// ================================================================================================
		// Э3: ГАШЕНИЕ РЕЦЕПТОВ AE2, ДУБЛИРУЮЩИХ МАШИНЫ GT6.
		//
		// Приём — Грегов: «стереть чужой реестр» (для IC2 это DISABLE_ALL_*_RECIPES, GT_API), просто на новом
		// носителе: рецепты чужих машин лежат в ДАТАПАКЕ, рантайм-удаления у RecipeManager нет, поэтому
		// стирание идёт единственным центром порта — GT_API.removeDatapackRecipes (тот же, которым ходит
		// Loader_Recipes_Replace), в том же отложенном окне скана. Своего механизма не заводится, ключ
		// подавления переживает релог и /reload (GT_API.SUPPRESSED_DATAPACK_RECIPES + onDatapackSyncReapply).
		//
		// ⛔ ОТБОР — решение пользователя: «всё, что Грег заместил, должно перестать быть возможным в машинах
		// AE2; либо отключается сама машина (удаляется её крафт)». Критерий по типу операции: ТРАНСФОРМАЦИЯ
		// МАТЕРИИ (дробление, штамповка, зарядка кристалла, рост/синтез кристаллов) — гасится, её выполняют
		// машины GT6; ПЕРЕМЕЩЕНИЕ и ХРАНЕНИЕ (сеть, ячейки, шины, planes, автокрафт, spatial, зарядка
		// СОБСТВЕННЫХ инструментов AE2) — ME-механика, живёт.
		//
		// Оставлено ОСОЗНАННО (не дубли — ME-механика или единственный путь):
		//  • smelting/blasting silicon_from_certus_quartz_dust — ЕДИНСТВЕННЫЙ путь к ae2:silicon, на нём висит
		//    вход тега кремния; GT6 сам плавит эту пыль в кремний AE2. Это ещё и КАНОН rv3: плавка пыли кварца
		//    в кремний была и в 1.7.10 (processing/vanilla.recipe:7-8).
		//  • smelting/smooth_sky_stone_block — тоже канон rv3: единственная плавка небесного камня в 1.7.10 это
		//    ровно «BlockSkyStone -> BlockSkyStone:1» (decorative/skystone.recipe:1-2), сырой блок в гладкий.
		//  • transform сингулярностей (entangled_singularity*) — компонент квантовых колец (дальний мост
		//    ME-сети); Грег такого не делает.
		//  • Matter Condenser — производитель материи, но ЕДИНСТВЕННЫЙ в игре поставщик ae2:singularity: у GT6
		//    пути к ней нет вовсе. Операция захардкожена, датапак-рецепта у неё нет — гасить нечего.
		//  • charger/guide (книга-гайд) — документация мода, не производство.
		//  • Growth Accelerator — ускоритель ВАНИЛЬНЫХ культур/саженцев, аналога у GT6 нет — не дубль.
		//  • Annihilation/Formation Plane — АВТОМАТИЧЕСКАЯ добыча и раскладка; машины-добытчика у GT6 нет
		//    (только ручные инструменты и бедрок-бур, качающий жидкость) — дубля нет.
		//  • компрессия кристаллов в блок, формы кварца/флюикса (block_cutter/shaped), деконструкция —
		//    ВЕРСТАК и ванильный камнерез, не машина AE2; формы вдобавок канон гем-хранилищ Грега
		//    (LoaderItemData:2118-2129). Кварцевое стекло и лампу снимает собственное замещение Грега
		//    (CR.DEF_REM_REV_NCC выше в этом же файле).
		//
		// Управление — секция «ae2» конфига GregTech.cfg, по узлам (образец — секция «ic2»). Ось «наполнять
		// чужую машину» (ENABLE_ADDING_*) носителя не имеет: рецепты машин AE2 — датапак, дописывать который
		// мод не может; поэтому у узлов есть только ось «стереть». Конфиг спрашивается ЗДЕСЬ, внутри окна
		// скана: в окне onPostLoad запись новых ключей в файл подавлена, и ключ не появился бы в GregTech.cfg.
		//
		// ⚠️ ТИП КЛЮЧА — ResourceLocation, а не ResourceKey<Recipe<?>>: в 1.20.1 id носит сам рецепт
		// (Recipe.getId()), обёртки «рецепт+ключ реестра» здесь нет — центр removeDatapackRecipes ветки
		// принимает Set<ResourceLocation> (GT_API.java:309).
		// ================================================================================================
		gregapi.GT_API.deferRecipeScan(() -> {
			java.util.Set<net.minecraft.resources.ResourceLocation> tSuppress = new java.util.HashSet<>();

			// УЗЕЛ «инскрайбер» — умирает ЦЕЛИКОМ, вместе с крафтом машины. Все 15 его рецептов —
			// трансформация материи, и каждый выход достижим машиной GT6: пыль — ступка/Shredder, пыль
			// скайстоуна — Смеситель и дробление; печати и процессоры — Пресс GT6 (строки 53-88 выше);
			// прессы — LaserEngraver из блока железа + линза (строки выше) и размножение Прессом.
			// Машина без единого рецепта — мусор в витрине, поэтому и её крафт снимается тем же узлом.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllInscriberRecipes", T)) {
				suppressAE(tSuppress, "inscriber/certus_quartz_dust");
				suppressAE(tSuppress, "inscriber/fluix_dust");
				suppressAE(tSuppress, "inscriber/ender_dust");
				suppressAE(tSuppress, "inscriber/sky_stone_dust");
				suppressAE(tSuppress, "inscriber/calculation_processor_press");
				suppressAE(tSuppress, "inscriber/engineering_processor_press");
				suppressAE(tSuppress, "inscriber/logic_processor_press");
				suppressAE(tSuppress, "inscriber/silicon_press");
				suppressAE(tSuppress, "inscriber/calculation_processor_print");
				suppressAE(tSuppress, "inscriber/engineering_processor_print");
				suppressAE(tSuppress, "inscriber/logic_processor_print");
				suppressAE(tSuppress, "inscriber/silicon_print");
				suppressAE(tSuppress, "inscriber/calculation_processor");
				suppressAE(tSuppress, "inscriber/engineering_processor");
				suppressAE(tSuppress, "inscriber/logic_processor");
				suppressAE(tSuppress, "network/blocks/inscribers");
			}

			// УЗЕЛ «зарядка кристалла». Заряженный сертус у GT6 дают сито (шанс из промытой руды сертуса,
			// Loader_Recipes_Ores:302-305) и молния (RM.Lightning). Сам чарджер ЖИВЁТ — он заряжает
			// powered-предметы AE2 (см. шапку отбора).
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllChargerCrystalRecipes", T)) {
				suppressAE(tSuppress, "charger/charged_certus_quartz_crystal");
			}

			// УЗЕЛ «крафт в воде» (transform). Рост и синтез кристаллов — производство: кристалл сертуса у
			// GT6 из жилы кварцита и сита, флюикс — Смеситель; budding-цепь — фабрика сертуса мимо экономики
			// GT6 (и второй, после гашеного метеорита, вход в неё). Сингулярности НЕ трогаются.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllTransformCrystalRecipes", T)) {
				suppressAE(tSuppress, "transform/certus_quartz_crystals");
				suppressAE(tSuppress, "transform/fluix_crystal");
				suppressAE(tSuppress, "transform/fluix_crystals");
				suppressAE(tSuppress, "transform/damaged_budding_quartz");
				suppressAE(tSuppress, "transform/chipped_budding_quartz");
				suppressAE(tSuppress, "transform/flawed_budding_quartz");
			}

			// УЗЕЛ «инструменты сертуса и незер-кварца» — все 14 (7 сертусовых + 7 незер-кварцевых).
			// Инструмент — сердце GT6: единый MetaTool (одна запись реестра gt.metatool.01), 40+ типов на
			// мете стека, каждый из ЛЮБОГО материала с качеством, износом и ремонтом. Кварцевый набор AE2 —
			// прямой дубль этой системы, причём вне её экономики.
			// ⚠️ ИЗВЕСТНОЕ СЛЕДСТВИЕ, принятое решением пользователя: гаснут и оба кварцевых КЛЮЧА, а на них
			// висел вход крафта Network Tool (tools/network_tool.json просит тег ae2:quartz_wrench).
			// Обслуживание блоков AE2 переезжает на грегов ключ нашим кодом (отдельный кусок слоя); тег
			// c:tools/wrench НЕ заводится — у GT6 все инструменты одна запись реестра, и тег пометил бы
			// ключом даже меч. Ключ поднят во флаг CS.AE2_KILL_QUARTZ_TOOLS: он двигает ДВЕ вещи — это
			// гашение и встроенный пак ae2gtrecipes, перепаивающий вход Network Tool на ключ Грега.
			if (AE2_KILL_QUARTZ_TOOLS) {
				suppressAE(tSuppress, "tools/certus_quartz_axe");
				suppressAE(tSuppress, "tools/certus_quartz_hoe");
				suppressAE(tSuppress, "tools/certus_quartz_pickaxe");
				suppressAE(tSuppress, "tools/certus_quartz_spade");
				suppressAE(tSuppress, "tools/certus_quartz_sword");
				suppressAE(tSuppress, "tools/certus_quartz_wrench");
				suppressAE(tSuppress, "tools/certus_quartz_cutting_knife");
				suppressAE(tSuppress, "tools/nether_quartz_axe");
				suppressAE(tSuppress, "tools/nether_quartz_hoe");
				suppressAE(tSuppress, "tools/nether_quartz_pickaxe");
				suppressAE(tSuppress, "tools/nether_quartz_spade");
				suppressAE(tSuppress, "tools/nether_quartz_sword");
				suppressAE(tSuppress, "tools/nether_quartz_wrench");
				suppressAE(tSuppress, "tools/nether_quartz_cutting_knife");
			}

			// УЗЕЛ «инструменты флюикса» — 5 инструментов и кузнечный шаблон, который существует только
			// ради них (FLUIX_UPGRADE_SMITHING_TEMPLATE, тип minecraft:smithing_transform).
			// Основание то же, что у кварцевого набора.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllFluixToolRecipes", T)) {
				suppressAE(tSuppress, "tools/fluix_axe");
				suppressAE(tSuppress, "tools/fluix_hoe");
				suppressAE(tSuppress, "tools/fluix_pickaxe");
				suppressAE(tSuppress, "tools/fluix_shovel");
				suppressAE(tSuppress, "tools/fluix_sword");
				suppressAE(tSuppress, "tools/fluix_upgrade_smithing_template");
			}

			// УЗЕЛ «ёмкости из небесного камня» — сундуки и бак. Хранение предметов и жидкостей у GT6 своё
			// и куда богаче: MassStorage (Box/Barrel/Standard/Logistics), сейфы, шкафчики, ящики, баки и
			// бочки. Это ХРАНЕНИЕ, но НЕ ME-механика: сундук из небесного камня к сети не подключается, он
			// просто сундук — то есть попадает под замещение, в отличие от ME Chest и ME Drive.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllSkyStoneContainerRecipes", T)) {
				suppressAE(tSuppress, "misc/chests_sky_stone");
				suppressAE(tSuppress, "misc/chests_smooth_sky_stone");
				suppressAE(tSuppress, "misc/tank_sky_stone");
			}

			// УЗЕЛ «Tiny TNT». Взрывчатка у GT6 своя (Dynamite, Dynamite_Strong, Boomstick) и завязана на
			// его же добычу (BlocksGT.drillableDynamite).
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllTinyTNTRecipes", T)) {
				suppressAE(tSuppress, "misc/tiny_tnt");
			}

			// УЗЕЛ «манипулятор энтропии» — крафт И все 10 его операций разом. Операции (тип ae2:entropy) это
			// превращение блоков в мире: нагрев булыжник→камень, лёд→вода, снег→вода, вода→воздух; охлаждение
			// вода→лёд, лава→обсидиан, камень→булыжник, кирпич→треснувший, трава→земля, текучая вода→снежок.
			// У GT6 нагрев и охлаждение — работа МАШИН, а лава+вода→обсидиан вдобавок ванильная механика.
			// Гасим вместе с крафтом: машина без операций — мусор в витрине, и предмет без операций тоже.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllEntropyManipulatorRecipes", T)) {
				suppressAE(tSuppress, "tools/misctools_entropy_manipulator");
				suppressAE(tSuppress, "entropy/heat/cobblestone_stone");
				suppressAE(tSuppress, "entropy/heat/ice_water");
				suppressAE(tSuppress, "entropy/heat/snow_water");
				suppressAE(tSuppress, "entropy/heat/water_air");
				suppressAE(tSuppress, "entropy/cool/flowing_water_snowball");
				suppressAE(tSuppress, "entropy/cool/grass_block_dirt");
				suppressAE(tSuppress, "entropy/cool/lava_obsidian");
				suppressAE(tSuppress, "entropy/cool/stone_bricks_cracked_stone_bricks");
				suppressAE(tSuppress, "entropy/cool/stone_cobblestone");
				suppressAE(tSuppress, "entropy/cool/water_ice");
			}

			// УЗЕЛ «кабельный якорь». ⚠️ ОБОСНОВАНИЕ ПЕРЕПИСАНО ПО ФАКТУ ЭТОЙ ВЕРСИИ, а не перенесено:
			// на 26.1 это единственный рецепт СВОЕГО типа ae2:quartz_cutting (GUI ножа), а у AE2 15.4.10
			// типа quartz_cutting нет вовсе (AERecipeTypes объявляет transform/entropy/inscriber/charger/
			// matter_cannon) — здесь якорь собирается ОБЫЧНЫМ верстаком: network/parts/cable_anchor.json,
			// тип minecraft:crafting_shapeless, вход «тег ae2:metal_ingots + тег ae2:knife», выход 4 штуки.
			// Гасим всё равно и по той же причине: ровно этот выход Грег заместил САМ, дословно и 1:1 с
			// оригиналом — RM.sawing из слитка (строки 67-78 выше; мета 120 = cable_anchor). Путь GT6 не
			// позже: Пила — ранний тир. Вдобавок нож гаснет узлом кварцевых инструментов, то есть без
			// гашения рецепт всё равно осиротел бы наполовину.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllCableAnchorRecipes", T)) {
				suppressAE(tSuppress, "network/parts/cable_anchor");
			}

			// УЗЕЛ «плавка пыли небесного камня в блок». Это НОВШЕСТВО поздних версий, а не канон: в 1.7.10
			// у AE2 плавка небесного камня была ровно одна — «BlockSkyStone -> BlockSkyStone:1», сырой блок
			// в гладкий (decorative/skystone.recipe:1-2), а обратного моста «пыль -> блок» не существовало.
			// Промышленный путь Грега свой: Смеситель варит пыль, пыль плавится в тигле и льётся в формы.
			// Печной мост обесценивал бы всю эту цепь.
			// ⛔ smelting/smooth_sky_stone_block НЕ трогаем — он и есть тот канон rv3 (см. шапку отбора).
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllSkyStoneDustSmeltingRecipes", T)) {
				suppressAE(tSuppress, "blasting/sky_stone_block");
			}

			// УЗЕЛ «генераторы энергии AE2» — Vibration Chamber и Crystal Resonance Generator. Оба выдают AE
			// в сеть и концептуально дублируют генераторы GT6. Питание сети остаётся: мост энергии GT6 → FE
			// сделан (EnergyCompat.feHandler + IItemEnergy.Utility.fe), а AE2 принимает FE своей капой
			// (appeng/blockentity/powersink/AEBasePoweredBlockEntity + ForgeEnergyAdapter).
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllEnergyGeneratorRecipes", T)) {
				suppressAE(tSuppress, "network/blocks/energy_vibration_chamber");
				suppressAE(tSuppress, "network/crystal_resonance_generator");
			}

			// УЗЕЛ «чарджер и ворот». Чарджер был ЕДИНСТВЕННЫМ способом зарядить powered-предметы AE2, и
			// пока GT6 их заряжать не умел, гасить его было нельзя. Теперь умеет: мост энергии сделан двумя
			// плечами в существующих центрах — блочное в EnergyCompat (feHandler, FE-капа движка) и
			// предметное в IItemEnergy.Utility (капа предмета). Зарядники GT6 заряжают беспроводной терминал
			// и переносные ячейки напрямую, курсом RF_PER_EU. Значит чарджер перестал быть незаменимым и
			// попадает под общий принцип. Ворот (Wooden Crank) — ручной привод к чарджеру и инскрайберу;
			// инскрайбер уже мёртв, чарджер гаснет здесь, приводить нечего.
			// ⛔ Гасится ТОЛЬКО КРАФТ. Сами блоки, их поведение и рецепты чарджера (charger/guide) не тронуты:
			// у кого они уже стоят в мире — продолжают работать.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllChargerAndCrankRecipes", T)) {
				suppressAE(tSuppress, "network/blocks/crystal_processing_charger");
				suppressAE(tSuppress, "network/blocks/crank");
			}

			// Компас метеоритов — не отдельный узел, а спутник мастер-ключа генерации: метеориты погашены →
			// искать компасу нечего; ключ false → компас возвращается вместе с ними.
			if (AE2_REPLACE_METEORITE_GENERATION) {
				suppressAE(tSuppress, "charger/meteorite_compass");
			}

			// УЗЕЛ «раскол блока обратно в самоцветы». AE2 разбирает блок в сетке (shapeless), GT6 — РУЧНЫМ
			// МОЛОТОМ по блоку (RM.smash → RM.Hammer), то есть путь GT6 НЕ ПОЗЖЕ и узел принцип проходит. Но
			// по умолчанию F: в 1.7.10 эти четыре shapeless-рецепта существовали (AE2 rv3,
			// misc/deconstruction.recipe) и Грег их НЕ снимал — он только добавил свои RM.smash (строки
			// 117-128 выше). Ставить T значило бы разойтись с оригиналом без нужды; рубильник дан сборщику.
			// Перечислены ровно те четыре блока, для которых у GT6 есть свой раскол; на quartz_bricks,
			// cut_quartz_block и smooth_quartz_block рецепта GT6 нет — их гашение стёрло бы разбор совсем.
			if (ConfigsGT.GREGTECH.get("ae2", "DisableAllCrystalDeconstructionRecipes", F)) {
				suppressAE(tSuppress, "misc/deconstruction_certus_quartz_block");
				suppressAE(tSuppress, "misc/deconstruction_certus_quartz_pillar");
				suppressAE(tSuppress, "misc/deconstruction_chiseled_certus_quartz");
				suppressAE(tSuppress, "misc/deconstruction_fluix_block");
			}

			OUT.println("GT_Mod: AE Duplicate Recipes to suppress: " + tSuppress.size() + " " + tSuppress);
			gregapi.GT_API.removeDatapackRecipes(gregapi.GT_API.sCurrentServerForRecipeScan, tSuppress);
		});
	}

	/** Ключ датапак-рецепта AE2 по пути его файла ({@code data/<неймспейс AE2>/recipes/<путь>.json}).
	 *  Неймспейс берётся у {@code MD.AE}, а не строкой: имя мода — уже централизованное знание.
	 *  В 1.20.1 ключ рецепта — {@code ResourceLocation} (id носит сам рецепт, {@code Recipe.getId()}). */
	private static void suppressAE(java.util.Set<net.minecraft.resources.ResourceLocation> aSet, String aPath) {
		aSet.add(new net.minecraft.resources.ResourceLocation(MD.AE.mID, aPath));
	}
}
