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

package gregapi.lang;

import gregapi.code.ItemNBT;
import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class LanguageHandler {
	public static ModConfigSpec sLangFile;
	public static boolean sUseFile = F;
	
	private static final HashMap<String, String> BUFFERMAP = new HashMap<>(), BACKUPMAP = new HashMap<>();
	private static boolean mWritingEnabled = F;
	
	public static void save() {
		if (sLangFile != null) {
			mWritingEnabled = T;
			sLangFile.save();
		}
	}
	
	public static synchronized void set(String aKey, String aEnglish) {
		BACKUPMAP.put(aKey, aEnglish);
		// PORT-TODO(LOCALIZATION, runtime-inject-to-vanilla-lang): 1.7.10 LanguageRegistry.instance()
		// .injectLanguage(locale, Map) регистрировал строку в ЖИВОЙ ванильной таблице переводов (видна
		// стандартному tooltip/getName()) — в neo эквивалента нет ни в одном из 3 корней референса:
		// LanguageProvider (net.neoforged.neoforge.common.data) — abstract datagen-only класс без
		// .instance()/.injectLanguage (собирает lang/en_us.json ДО запуска игры, не рантайм-API);
		// I18nManager.injectTranslations (net.neoforged.fml.i18n) — внутренний loader-механизм FML
		// для сообщений ModLoadingException, не general-purpose реестр перевода мода;
		// Language.inject(Language) заменяет ЦЕЛИКОМ статический синглтон, не аддитивно по одному
		// ключу. BACKUPMAP выше остаётся источником истины для translate() на обеих сторонах; чтобы
		// ЭТА строка была видна и в штатных ванильных путях (Item.getName()/tooltip), нужен отдельный
		// датаген-провайдер (GatherDataEvent → LanguageProvider.addTranslations(), пишет
		// lang/en_us.json) — вне зоны этого чекпоинта (см. DEFERRED-LEDGER).
	}

	public static synchronized void add(String aKey, String aEnglish) {
		if (aKey == null) return;
		aKey = aKey.trim();
		if (aKey.length() <= 0) return;
		BACKUPMAP.put(aKey, aEnglish);
		if (sLangFile == null) {
			BUFFERMAP.put(aKey, aEnglish);
		} else {
			// PORT-TODO(F12, config-subsystem): тот же класс проблемы, что у GT_API.java (см.
			// PORT-TODO "F12, config-subsystem" там) — ModConfigSpec строится через Builder на
			// регистрации мода (статическая схема), нет File-конструктора и нет per-call
			// get(category,name,default), как было у 1.7.10 Configuration/Property. Владельцы
			// (gregapi.config.Config, эта sLangFile-ветка) сами не портированы на реальный
			// ModConfigSpec — отдельный шов вне локализации. В текущем состоянии порта GT_API.java
			// держит LanguageHandler.sLangFile===null (см. GT_API.java, "sUseFile — тот же дефолт F"),
			// поэтому эта ветка фактически не выполняется; BACKUPMAP выше остаётся источником истины.
		}
	}
	
	public static String get(String aKey, String aDefault) {
		add(aKey, aDefault);
		return translate(aKey, aDefault);
	}

	/** было {@code I18n.translateToLocal(aKey)} (1.7.10 StatCollector — ТОЛЬКО чтение lang-таблицы, перевод-или-ключ, без регистрации). */
	public static String get(String aKey) {
		return translate(aKey, aKey);
	}
	
	public static String langfile(String aKey, String aEnglish) {
		if (sLangFile == null) return aEnglish;
		// PORT-TODO(F12, config-subsystem): см. add() выше — тот же класс проблемы (ModConfigSpec
		// без per-call get(category,name,default)); sLangFile-оверрайд не портирован, дефолт-строка
		// возвращается как есть (тот же исход, что и sLangFile==null).
		return aEnglish;
	}

	public static String translate(String aKey) {
		return translate(aKey, aKey);
	}

	public static String translate(String aKey, String aDefault) {
		if (aKey == null || aKey.length() < 2) return "";
		aKey = aKey.trim();
		if (aKey.length() < 2) return "";
		String
		// 1.7.10 здесь стояли ДВА раздельных запроса к движку: FML LanguageRegistry.getStringLocalization
		// (свой оверлей) и vanilla StatCollector.translateToLocal (файловый lang-реестр) — в 1.7.10 это
		// были два разных подсистемы сканирования .lang-файлов. В neo/vanilla они слиты в ОДНУ систему
		// (Language/Component), поэтому оба тира схлопнуты в один реальный вызов ниже (не дублируем
		// одинаковый запрос дважды подряд — R1). Component.translatable(...).getString() безопасен на
		// обеих сторонах (TranslatableContents.visit читает Language.getInstance() напрямую, класс не
		// @OnlyIn) и деградирует до возврата самого ключа, если перевод не найден (см. Language.java:132-136).
		rTranslation = Component.translatable(aKey).getString();
		if (UT.Code.stringValid(rTranslation) && !aKey.equals(rTranslation)) return rTranslation;
		rTranslation = BACKUPMAP.get(aKey);
		if (UT.Code.stringValid(rTranslation)) return rTranslation;

		aKey = (aKey.endsWith(".name") ? aKey.substring(0, aKey.length() - 5) : aKey + ".name");

		rTranslation = Component.translatable(aKey).getString();
		if (UT.Code.stringValid(rTranslation) && !aKey.equals(rTranslation)) return rTranslation;
		rTranslation = BACKUPMAP.get(aKey);
		if (UT.Code.stringValid(rTranslation)) return rTranslation;

		return aDefault;
	}
	
	public static String separate(String aKey, String aSeparator) {
		if (aKey == null) return "";
		String rTranslation = "";
		for (String tString : aKey.split(aSeparator)) {
			rTranslation += get(tString, tString);
		}
		return rTranslation;
	}
	
	public static String getTranslateableItemStackName(ItemStack aStack) {
		if (ST.invalid(aStack)) return "null";
		CompoundTag tNBT = ItemNBT.get(aStack);
		if (tNBT != null && tNBT.contains("display")) {
			String tName = tNBT.getCompoundOrEmpty("display").getStringOr("Name", "");
			if (UT.Code.stringValid(tName)) {
				return tName;
			}
		}
		// PORT-TODO(F1, item-unlocalized-name-per-stack): 1.7.10 ItemStack.getUnlocalizedName()
		// делегировал в переопределяемый Item.getUnlocalizedName(ItemStack) — per-metadata вариация
		// подтипов (та же развилка модели предмета, что и decisions/F1-item-metadata-model.md).
		// В neo Item.getDescriptionId() финальный и без параметра ItemStack; per-stack вариация не
		// воспроизведена здесь до решения F1 по компоненту MATERIAL/VARIANT.
		return aStack.getItem().getDescriptionId() + ".name";
	}
	
	public static String getLocalName(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		// Certain Materials have slightly different default Localisations.
		if (aPrefix == OP.crateGtRaw      || aPrefix == OP.crateGt64Raw      || aPrefix == OP.blockRaw     ) return aPrefix.mMaterialPre + getLocalName(OP.ore     , aMaterial);
		if (aPrefix == OP.crateGtGem      || aPrefix == OP.crateGt64Gem      || aPrefix == OP.blockGem     ) return aPrefix.mMaterialPre + getLocalName(OP.gem     , aMaterial);
		if (aPrefix == OP.crateGtDust     || aPrefix == OP.crateGt64Dust     || aPrefix == OP.blockDust    ) return aPrefix.mMaterialPre + getLocalName(OP.dust    , aMaterial);
		if (aPrefix == OP.crateGtIngot    || aPrefix == OP.crateGt64Ingot    || aPrefix == OP.blockIngot   ) return aPrefix.mMaterialPre + getLocalName(OP.ingot   , aMaterial);
		if (aPrefix == OP.crateGtPlate    || aPrefix == OP.crateGt64Plate    || aPrefix == OP.blockPlate   ) return aPrefix.mMaterialPre + getLocalName(OP.plate   , aMaterial);
		if (aPrefix == OP.crateGtPlateGem || aPrefix == OP.crateGt64PlateGem || aPrefix == OP.blockPlateGem) return aPrefix.mMaterialPre + getLocalName(OP.plateGem, aMaterial);
		
		if (aMaterial.mID >= 0 && aMaterial.mID < 10 && aPrefix.contains(TD.Prefix.ORE)) return APRIL_FOOLS ? "Schrödingers Ore" : "Unidentified Ore";
		
		if (APRIL_FOOLS) {
			if (aMaterial == MT.Empty) {
			if (aPrefix == OP.bulletGtSmall)                                        return aPrefix.mMaterialPre + "Bolt Shaft";
			if (aPrefix == OP.bulletGtMedium)                                       return aPrefix.mMaterialPre + "Bolt Shaft";
			if (aPrefix == OP.bulletGtLarge)                                        return aPrefix.mMaterialPre + "Bolt Shaft";
			} else {
			if (aPrefix == OP.bulletGtSmall)                                        return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Bolt";
			if (aPrefix == OP.bulletGtMedium)                                       return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Bolt";
			if (aPrefix == OP.bulletGtLarge)                                        return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Bolt";
			}
		}
		
		if (aMaterial == MT.Empty) {
			if (aPrefix == OP.chemtube)                                             return "Empty Glass Tube";
			if (aPrefix == OP.arrowGtWood)                                          return "Headless Wood Arrow";
			if (aPrefix == OP.arrowGtPlastic)                                       return "Headless Plastic Arrow";
			if (aPrefix == OP.bulletGtSmall)                                        return "Small Bullet Casing";
			if (aPrefix == OP.bulletGtMedium)                                       return "Medium Bullet Casing";
			if (aPrefix == OP.bulletGtLarge)                                        return "Large Bullet Casing";
			if (aPrefix.contains(TD.Prefix.ORE))                                    return "Unidentified Ore";
			if (aPrefix.contains(TD.Prefix.ORE_PROCESSING_BASED))                   return "Processed Unidentified Ore";
		} else
		if (aMaterial == MT.Stone) {
			if (aPrefix == OP.rockGt)                                               return "Rock";
		} else
		if (aMaterial == MT.AncientDebris) {
			if (aPrefix == OP.rockGt)                                               return aMaterial.mNameLocal;
			if (aPrefix == OP.oreRaw)                                               return "Big Chunks of " + aMaterial.mNameLocal;
			if (aPrefix == OP.crushed)                                              return "Recycled " + aMaterial.mNameLocal;
			if (aPrefix == OP.crushedTiny)                                          return "Tiny Recycled " + aMaterial.mNameLocal;
			if (aPrefix == OP.nugget)                                               return "Tiny Piece of Netherite Scrap";
			if (aPrefix == OP.chunkGt)                                              return "Small Piece of Netherite Scrap";
			if (aPrefix == OP.billet)                                               return "Billet of Netherite Scrap";
			if (aPrefix == OP.scrap)                                                return "Scrap Piece of Netherite Scrap";
			if (aPrefix.mNameInternal.startsWith("ore"))                            return aMaterial.mNameLocal;
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + aMaterial.mNameLocal;
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Netherite Scrap Powder";
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + "Netherite Scrap";
		} else
		if (aMaterial == MT.Sand || aMaterial == MT.RedSand || aMaterial == MT.SoulSand || aMaterial == MT.EndSandWhite || aMaterial == MT.EndSandBlack) {
			if (aPrefix == OP.crushed)                                              return "Ground " + aMaterial.mNameLocal;
			if (aPrefix == OP.crushedTiny)                                          return "Tiny Ground " + aMaterial.mNameLocal;
			if (aPrefix.mNameInternal.startsWith("ore"))                            return aMaterial.mNameLocal;
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + aMaterial.mNameLocal;
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal;
		} else
		if (aMaterial == MT.Netherrack) {
			if (aPrefix == OP.rockGt)                                               return "Nether Rock";
		} else
		if (aMaterial == MT.Endstone) {
			if (aPrefix == OP.rockGt)                                               return "End Rock";
		} else
		if (aMaterial == MT.MeteoricIron || aMaterial == MT.Meteorite) {
			if (aPrefix == OP.rockGt)                                               return "Meteorite";
			if (aPrefix == OP.oreRaw)                                               return "Big Meteorite";
		} else
		if (aMaterial == MT.STONES.SpaceRock) {
			if (aPrefix == OP.rockGt)                                               return "Space Rock";
		} else
		if (aMaterial == MT.STONES.MoonRock) {
			if (aPrefix == OP.rockGt)                                               return "Moon Rock";
		} else
		if (aMaterial == MT.STONES.MarsRock) {
			if (aPrefix == OP.rockGt)                                               return "Mars Rock";
		} else
		if (aMaterial == MT.STONES.MoonTurf) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Moon Turf";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Moon Turf";
			if (aPrefix == OP.rockGt)                                               return "Moon Surface Rock";
		} else
		if (aMaterial == MT.STONES.MarsSand) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Mars Turf";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Mars Turf";
			if (aPrefix == OP.rockGt)                                               return "Mars Surface Rock";
		} else
		if (aMaterial == MT.STONES.Holystone) {
			if (aPrefix == OP.rockGt)                                               return "Holy Rock";
		} else
		if (aMaterial == MT.STONES.Umber) {
			if (aPrefix == OP.rockGt)                                               return "Umber Rock";
		} else
		if (aMaterial == MT.STONES.Betweenstone) {
			if (aPrefix == OP.rockGt)                                               return "Betweenrock";
		} else
		if (aMaterial == MT.STONES.Pitstone) {
			if (aPrefix == OP.rockGt)                                               return "Pit Rock";
		} else
		if (aMaterial == MT.STONES.Gneiss) {
			if (aPrefix == OP.rockGt)                                               return "Gneiss";
		} else
		if (aMaterial == MT.Glass) {
			if (aPrefix == OP.scrapGt)                                              return aMaterial.mNameLocal + " Shards";
			if (aPrefix == OP.round)                                                return aMaterial.mNameLocal + " Marble";
			if (aPrefix == OP.plateGem)                                             return "Cast " + aMaterial.mNameLocal + " Pane";
			if (aPrefix == OP.plateGemTiny)                                         return "Tiny Cast " + aMaterial.mNameLocal + " Pane";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Crystal";
			if (aPrefix.mNameInternal.startsWith("plate"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Pane";
		} else
		if (aMaterial == MT.PrismarineLight || aMaterial == MT.PrismarineDark) {
			if (aPrefix == OP.rockGt)                                               return aMaterial.mNameLocal + " Shard";
			if (aPrefix == OP.scrapGt)                                              return aMaterial.mNameLocal + " Shards";
			if (aPrefix == OP.round)                                                return aMaterial.mNameLocal + " Marble";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Crystal";
		} else
		if (aMaterial == MT.Frezarite || aMaterial == MT.Fluix || aMaterial == MT.Redstonia || aMaterial == MT.Palis || aMaterial == MT.Diamantine || aMaterial == MT.VoidCrystal || aMaterial == MT.Emeradic || aMaterial == MT.Enori) {
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Crystal";
		} else
		if (aMaterial == MT.InfusedDull) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Dull Crystals";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Dull Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Dull Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Dull Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Dull Crystal Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Dull Crystal Powder";
			return aPrefix.mMaterialPre + "Dull" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedBalance) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Balance infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Balance Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Balance Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Balance Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Balance infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Balance infused Powder";
			return aPrefix.mMaterialPre + "Balance infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedVis) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Magic infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Magic Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Magic Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Magic Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Magic infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Magic infused Powder";
			return aPrefix.mMaterialPre + "Magic infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedAir) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Air infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Air Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Air Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Air Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Air infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Air infused Powder";
			return aPrefix.mMaterialPre + "Air infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedWater) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Water infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Water Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Water Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Water Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Water infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Water infused Powder";
			return aPrefix.mMaterialPre + "Water infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedEarth) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Earth infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Earth Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Earth Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Earth Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Earth infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Earth infused Powder";
			return aPrefix.mMaterialPre + "Earth infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedFire) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Fire infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Fire Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Fire Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Fire Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Fire infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Fire infused Powder";
			return aPrefix.mMaterialPre + "Fire infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedOrder) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Order infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Order Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Order Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Order Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Order infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Order infused Powder";
			return aPrefix.mMaterialPre + "Order infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.InfusedEntropy) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return "Entropy infused Stone";
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + "Entropy Shard";
			if (aPrefix.mNameInternal.startsWith("crystal"))                        return aPrefix.mMaterialPre + "Entropy Shard";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Entropy Shards";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Entropy infused Powder";
			if (aPrefix == OP.chemtube)                                             return aPrefix.mMaterialPre + "Entropy infused Powder";
			return aPrefix.mMaterialPre + "Entropy infused" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.Peridot) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Olivine";
		} else
		if (aMaterial == MT.Craponite) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Flavourite";
		} else
		if (aMaterial == MT.Wheat) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Flour";
		} else
		if (aMaterial == MT.Oat) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Oatmeal";
		} else
		if (aMaterial == MT.OatAbyssal) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Abyssal Oatmeal";
		} else
		if (aMaterial == MT.Rye) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Rye Flour";
		} else
		if (aMaterial == MT.Barley) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Barley Flour";
		} else
		if (aMaterial == MT.Corn) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Cornmeal";
		} else
		if (aMaterial == MT.Rice) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Rice";
		} else
		if (aMaterial == MT.Ice) {
			if (aPrefix == OP.gemChipped)                                           return "Ice Cubes";
			if (aPrefix == OP.gemFlawed)                                            return "Medium Ice Cube";
			if (aPrefix == OP.gem)                                                  return "Large Ice Cube";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Crushed Ice";
		} else
		if (aMaterial == MT.WoodTreated) {
			if (aPrefix == OP.rockGt)                                               return aMaterial.mNameLocal;
			if (aPrefix == OP.scrapGt)                                              return aMaterial.mNameLocal + " Splinters";
			if (aPrefix.mNameInternal.startsWith("bolt"))                           return "Short Treated Stick";
			if (aPrefix.mNameInternal.startsWith("stick"))                          return aPrefix.mMaterialPre + "Treated Stick";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Pulp";
			if (aPrefix == OP.nugget)                                               return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Chip";
			if (aPrefix.mNameInternal.startsWith("plate"))                          return aPrefix.mMaterialPre + "Treated Plank";
		} else
		if (aMaterial == MT.FierySteel) {
			if (aPrefix.contains(TD.Prefix.IS_CONTAINER))                           return aPrefix.mMaterialPre + "Fiery Blood" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.Steeleaf || aMaterial == MT.Fireleaf) {
			if (aPrefix == OP.plantGtBlossom)                                       return aMaterial.mNameLocal + " Leaf";
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal;
		} else
		if (aMaterial == MT.Bark) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Tree Bark";
		} else
		if (aMaterial == MT.Tea || aMaterial == MT.Mint) {
			if (aPrefix == OP.plantGtBlossom)                                       return aMaterial.mNameLocal + " Leaf";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Powder";
		} else
		if (aMaterial == MT.Bone) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return aPrefix.mMaterialPre + "Fossil";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Bonemeal";
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + "Bones";
		} else
		if (aMaterial == MT.Flint) {
			if (aPrefix == OP.rockGt)                                               return "Mario";
		} else
		if (aMaterial == MT.Pyrite) {
			if (aPrefix.contains(TD.Prefix.ORE))                                    return aPrefix.mMaterialPre + MT.Au.mNameLocal + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.MgCO3) {
			if (aPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED)) return aPrefix.mMaterialPre + "Magnesite" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.Asbestos) {
			if (aPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED)) return aPrefix.mMaterialPre + "Chrysotile" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.Talc) {
			if (aPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED)) return aPrefix.mMaterialPre + "Soapstone" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.AlO3H3) {
			if (aPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED)) return aPrefix.mMaterialPre + "Gibbsite" + aPrefix.mMaterialPost;
		} else
		if (aMaterial == MT.Au) {
			if (aPrefix == OP.plantGtBlossom)                                       return "Aurelia Leaf";
		} else
		if (aMaterial == MT.Fe) {
			if (aPrefix == OP.plantGtBlossom)                                       return "Ferru Leaf";
		} else
		if (aMaterial == MT.Pb) {
			if (aPrefix == OP.plantGtBlossom)                                       return "Plumbilia Leaf";
		} else
		if (aMaterial == MT.Ag) {
			if (aPrefix == OP.plantGtBlossom)                                       return "Argentia Leaf";
		} else
		if (aMaterial == MT.Sn) {
			if (aPrefix == OP.plantGtTwig)                                          return "Tine Twig";
		} else
		if (aMaterial == MT.Cu) {
			if (aPrefix == OP.plantGtFiber)                                         return "Coppon Fiber";
		} else
		if (aMaterial == MT.Emerald) {
			if (aPrefix == OP.plantGtBerry)                                         return "Bobs-Yer-Uncle-Berry";
		} else
		if (aMaterial == MT.Milk) {
			if (aPrefix == OP.plantGtWart)                                          return "Milkwart";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Powder";
		} else
		if (aMaterial == MT.Chocolate || aMaterial == MT.Cheese) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Powder";
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Bar";
		} else
		if (aMaterial == MT.Butter || aMaterial == MT.ButterSalted) {
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal;
		} else
		if (aMaterial == MT.Indigo || aMaterial == MT.ConstructionFoam || aMaterial == MT.Cocoa || aMaterial == MT.Curry || aMaterial == MT.Chocolate || aMaterial == MT.Coffee || aMaterial == MT.Chili || aMaterial == MT.Cheese || aMaterial == MT.Snow) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Powder";
		} else
		if (aMaterial == MT.Potato || aMaterial == MT.Hazelnut || aMaterial == MT.Pistachio || aMaterial == MT.Almond || aMaterial == MT.Peanut || aMaterial == MT.Nutmeg || aMaterial == MT.Cinnamon || aMaterial == MT.Vanilla || aMaterial == MT.PepperBlack) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Ground " + aMaterial.mNameLocal;
		} else
		if (aMaterial == MT.Paper) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Chad";
			if (aPrefix == OP.scrapGt)                                              return "Shredded " + aMaterial.mNameLocal;
			if (aPrefix == OP.plateTiny)                                            return "Tiny piece of Paper";
			if (aPrefix == OP.plate)                                                return "Sheet of Paper";
			if (aPrefix == OP.plateDouble)                                          return "Paperboard";
			if (aPrefix == OP.plateTriple)                                          return "Carton";
			if (aPrefix == OP.plateQuadruple)                                       return "Cardboard";
			if (aPrefix == OP.plateQuintuple)                                       return "Thick Cardboard";
			if (aPrefix == OP.plateDense)                                           return "Strong Cardboard";
		} else
		if (aMaterial == MT.FishRaw) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Fishmeal";
		} else
		if (aMaterial == MT.FishCooked) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Cooked Fishmeal";
		} else
		if (aMaterial == MT.FishRotten) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Rotten Fishmeal";
		} else
		if (aMaterial == MT.MeatRaw) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Mince Meat";
		} else
		if (aMaterial == MT.MeatCooked) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Cooked Mince Meat";
		} else
		if (aMaterial == MT.MeatRotten) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Rotten Mince Meat";
		} else
		if (aMaterial == MT.SoylentGreen || aMaterial == MT.Tofu) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Silken " + aMaterial.mNameLocal;
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Bar";
		} else
		if (aMaterial == MT.Peat || aMaterial == MT.PeatBituminous) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal;
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Brick";
		} else
		if (aMaterial == MT.Lignite || aMaterial == MT.LigniteCoke || aMaterial == MT.Charcoal || aMaterial == MT.Coal || aMaterial == MT.CoalCoke || aMaterial == MT.Anthracite || aMaterial == MT.Prismane || aMaterial == MT.Lonsdaleite || aMaterial == MT.PetCoke || aMaterial == MT.HydratedCoal) {
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Brick";
			if (aPrefix == OP.chunkGt)                                              return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Chunk";
			if (aPrefix == OP.nugget)                                               return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Pellet";
		} else
		if (aMaterial == MT.Sugar) {
			if (aPrefix == OP.gemChipped)                                           return "Sugar Cubes";
		} else
		if (aMaterial == MT.KCl) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Rock Salt";
		} else
		if (aMaterial == MT.Ceramic) {
			if (aPrefix == OP.scrapGt)                                              return "Brittle Ceramic Scraps";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + "Dry Clay Powder";
		} else
		if (ANY.Blaze.mToThis.contains(aMaterial)) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Powder";
			if (aPrefix.mNameInternal.startsWith("stick"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Rod";
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Bar";
		} else
		if (ANY.Clay.mToThis.contains(aMaterial)) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Powder";
		} else
		if (ANY.Plastic.mToThis.contains(aMaterial) || ANY.Rubber.mToThis.contains(aMaterial)) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Pulp";
			if (aPrefix.mNameInternal.startsWith("plate"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Sheet";
			if (aPrefix.mNameInternal.startsWith("ingot"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Bar";
			if (aPrefix == OP.nugget)                                               return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Chip";
			if (aPrefix.mNameInternal.startsWith("foil"))                           return "Thin " + aMaterial.mNameLocal + " Sheet";
		} else
		if (aMaterial == MT.Dilithium) {
			if (aPrefix.mNameInternal.startsWith("gem"))                            return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Crystal";
		} else
		if (aMaterial == MT.Ectoplasm || aMaterial == MT.Tallow || aMaterial == MT.Gunpowder || aMaterial == MT.NaCl || aMaterial == MT.KCl || aMaterial == MT.KIO3 || aMaterial == MT.Asphalt) {
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal;
		} else
		if (aMaterial == MT.Black || aMaterial == MT.Red || aMaterial == MT.Green || aMaterial == MT.Brown || aMaterial == MT.Blue || aMaterial == MT.Purple || aMaterial == MT.Cyan || aMaterial == MT.LightGray || aMaterial == MT.Gray || aMaterial == MT.Pink || aMaterial == MT.Lime || aMaterial == MT.Yellow || aMaterial == MT.LightBlue || aMaterial == MT.Magenta || aMaterial == MT.Orange || aMaterial == MT.White) {
			if (aPrefix == OP.plantGtFiber)                                         return aMaterial.mNameLocal + " String";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Dye";
		} else
		if (aMaterial == MT.Wax || aMaterial == MT.WaxMagic || aMaterial == MT.WaxAmnesic || aMaterial == MT.WaxSoulful || aMaterial == MT.WaxBee || aMaterial == MT.WaxRefractory || aMaterial == MT.WaxPlant || aMaterial == MT.WaxParaffin || aMaterial == MT.Ash || aMaterial == MT.DarkAsh || aMaterial == MT.VolcanicAsh || aMaterial == MT.ArcaneAsh || aMaterial == MT.ArcaneCompound || aMaterial == MT.OREMATS.Vermiculite || aMaterial == MT.Talc || aMaterial == MT.OREMATS.Magnetite || aMaterial == MT.OREMATS.BasalticMineralSand || aMaterial == MT.OREMATS.GraniticMineralSand || aMaterial == MT.OREMATS.GarnetSand || aMaterial == MT.SluiceSand || aMaterial == MT.OREMATS.QuartzSand || aMaterial == MT.OREMATS.Pitchblende || aMaterial == MT.Bentonite || aMaterial == MT.Palygorskite || aMaterial == MT.RareEarth || aMaterial == MT.Oilsands) {
			if (aPrefix.mNameInternal.startsWith("ore"))                            return aPrefix.mMaterialPre + aMaterial.mNameLocal;
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal;
			if (aPrefix == OP.crushed)                                              return "Ground " + aMaterial.mNameLocal;
			if (aPrefix == OP.crushedTiny)                                          return "Tiny Ground " + aMaterial.mNameLocal;
			if (aPrefix.mNameInternal.startsWith("crushed"))                        return aPrefix.mMaterialPre + aMaterial.mNameLocal;
		}
		
		if (aMaterial.contains(TD.Properties.WOOD)) {
			if (aPrefix == OP.rockGt)                                               return aMaterial.mNameLocal;
			if (aPrefix == OP.scrapGt)                                              return aMaterial.mNameLocal + " Splinters";
			if (aPrefix.mNameInternal.startsWith("bolt"))                           return "Short " + aMaterial.mNameLocal + " Stick";
			if (aPrefix.mNameInternal.startsWith("stick"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Stick";
			if (aPrefix.contains(TD.Prefix.DUST_BASED))                             return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Pulp";
			if (aPrefix == OP.nugget)                                               return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Chip";
			if (aPrefix.mNameInternal.startsWith("plate"))                          return aPrefix.mMaterialPre + aMaterial.mNameLocal + " Plank";
		}
		if (aMaterial.contains(TD.Properties.STONE)) {
			if (aPrefix == OP.rockGt)                                               return aMaterial.mNameLocal.endsWith("rock") ? (aMaterial.mNameLocal+" §§§").replaceFirst("rock §§§", " Rock") : aMaterial.mNameLocal + " Rock";
			if (aPrefix == OP.scrapGt)                                              return aMaterial.mNameLocal + " Pebbles";
		}
		if (aMaterial.mID > 0 && aMaterial.mID <= 830 && aMaterial.mID % 10 == 0 && aMaterial.mMeltingPoint > C && aMaterial.mTargetCrushing.mMaterial == aMaterial && aMaterial.contains(TD.Processing.SMITHABLE)) {
			if (aPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED)) return aPrefix.mMaterialPre + "Native " + aMaterial.mNameLocal + aPrefix.mMaterialPost;
		}
		
		
		
		// Use Standard Localisation
		return aPrefix.mMaterialPre + aMaterial.mNameLocal + aPrefix.mMaterialPost;
	}
}
