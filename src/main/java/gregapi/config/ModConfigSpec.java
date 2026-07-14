/**
 * Copyright (c) 2023 GregTech-6 Team
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

package gregapi.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * F12-config-subsystem: GT6-центр, воспроизводящий 1.7.10 Forge {@code net.minecraftforge.common.
 * config.Configuration} — файловый, динамический, per-call конфиг ({@code new Configuration(File)}
 * + {@code get(category,key,default)}, читаемый/записываемый в любой момент рантайма).
 *
 * Причина существования (см. также decisions/, DEFERRED-LEDGER.md "F12, config-subsystem"): neo
 * {@code net.neoforged.neoforge.common.ModConfigSpec} — ДЕКЛАРАТИВНАЯ модель, строится через
 * {@code Builder} на регистрации мода (ключи фиксированы заранее), не имеет ни {@code new
 * ModConfigSpec(File)}, ни {@code .load()}/{@code .save()}, ни per-call {@code get(category,key,
 * default)} → это архитектурно другая модель, несовместимая с GT6's dynamic-config-паттерном
 * ({@link gregapi.config.Config}, {@link gregapi.lang.LanguageHandler#sLangFile}). Движок сменил
 * модель → адаптируем централизованно, тем же приёмом, что F4-OreDictionary/F5-fluids/F9-block-
 * material: воспроизводим Forge-поведение в СВОЁМ, gregapi-центральном классе.
 *
 * Формат файла — человекочитаемый, той же структуры, что 1.7.10 {@code .cfg} (категория {@code {}}-
 * блок, {@code key=value} внутри), без второстепенных возможностей исходного Forge-формата
 * (типовые префиксы S:/I:/B:/D:, списки {@code <...>}, комментарии, min/max, вложенные категории,
 * child-файлы START/END) — они не используются ни одним вызывателем в этом дереве (см. grep
 * {@code .get(} в Config.java/GT_API.java/LanguageHandler.java: только 4 скалярных перегрузки
 * boolean/int/double/String, без списков и комментариев).
 *
 * Референс поведения: {@code gregtech6/build/tmp/recompSrc/net/minecraftforge/common/config/
 * Configuration.java} (методы {@code get(String,String,<type>)}, {@code load()}, {@code save()},
 * {@code getConfigFile()}; {@code wasRead()}-семантика — Configuration.java:697-710: ключ уже был в
 * {@code cat.containsKey(key)} на момент {@code get()} → true, иначе создан этим вызовом → false).
 *
 * @author Gregorius Techneticies
 */
public class ModConfigSpec {
	private final File mFile;
	private final Map<String, Map<String, ConfigValue>> mCategories = new TreeMap<>();

	public ModConfigSpec(File aFile) {
		mFile = aFile;
		load();
	}

	public File getConfigFile() {return mFile;}

	private Map<String, ConfigValue> category(String aCategory) {
		return mCategories.computeIfAbsent(aCategory, aKey -> new LinkedHashMap<>());
	}

	/** было {@code Configuration.get(String,String,boolean)} — Configuration.java:166-169. */
	public ConfigValue get(String aCategory, String aKey, boolean aDefault) {
		return get(aCategory, aKey, Boolean.toString(aDefault));
	}

	/** было {@code Configuration.get(String,String,int)} — Configuration.java:268-271. */
	public ConfigValue get(String aCategory, String aKey, int aDefault) {
		return get(aCategory, aKey, Integer.toString(aDefault));
	}

	/** было {@code Configuration.get(String,String,double)} — Configuration.java:410-413. */
	public ConfigValue get(String aCategory, String aKey, double aDefault) {
		return get(aCategory, aKey, Double.toString(aDefault));
	}

	/**
	 * было {@code Configuration.get(String,String,String,String,Property.Type)} — Configuration.java:
	 * 688-724 (общее ядро для всех 4 скалярных перегрузок). Ключ уже был в загруженном файле →
	 * возвращает СУЩЕСТВУЮЩУЮ запись (значение не трогается, только обновляется дефолт — 1:1
	 * Configuration.java:707), иначе создаёт новую с {@code aDefault} как текущим значением.
	 */
	public ConfigValue get(String aCategory, String aKey, String aDefault) {
		Map<String, ConfigValue> tCategory = category(aCategory);
		ConfigValue tExisting = tCategory.get(aKey);
		if (tExisting != null) {
			tExisting.mDefaultValue = aDefault;
			return tExisting;
		}
		ConfigValue tNew = new ConfigValue(aKey, aDefault, false);
		tCategory.put(aKey, tNew);
		return tNew;
	}

	/**
	 * было {@code Configuration.load()} — Configuration.java:791-1051: читает файл, заполняет
	 * категории записями с {@code wasRead=true} (Configuration.java:944: {@code new Property(name,
	 * value, type, true)}). Отсутствующий файл — не ошибка (новая установка), просто нет записей.
	 */
	public void load() {
		if (mFile == null || !mFile.exists()) return;
		try (BufferedReader tReader = new BufferedReader(new InputStreamReader(new FileInputStream(mFile), StandardCharsets.UTF_8))) {
			String tLine;
			Map<String, ConfigValue> tCategory = null;
			while ((tLine = tReader.readLine()) != null) {
				String tTrimmed = tLine.trim();
				if (tTrimmed.isEmpty() || tTrimmed.startsWith("#")) continue;
				if (tTrimmed.endsWith("{")) {
					tCategory = category(tTrimmed.substring(0, tTrimmed.length() - 1).trim());
					continue;
				}
				if (tTrimmed.equals("}")) {
					tCategory = null;
					continue;
				}
				if (tCategory == null) continue;
				int tSplit = tTrimmed.indexOf('=');
				if (tSplit < 0) continue;
				String tKey = tTrimmed.substring(0, tSplit).trim();
				String tValue = tTrimmed.substring(tSplit + 1);
				tCategory.put(tKey, new ConfigValue(tKey, tValue, true));
			}
		} catch (IOException e) {
			// совпадает с Configuration.load() (Configuration.java:1028-1031) — IO-ошибка не прерывает загрузку мода.
			e.printStackTrace();
		}
	}

	/**
	 * было {@code Configuration.save()} — Configuration.java:1053-1117: пишет все известные на данный
	 * момент категории/записи обратно в файл, каждая запись как {@code getString()}-значение (не
	 * дефолт), формат {@code category {\n\tkey=value\n}\n}.
	 */
	public void save() {
		if (mFile == null) return;
		try {
			if (mFile.getParentFile() != null) mFile.getParentFile().mkdirs();
			try (BufferedWriter tWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mFile), StandardCharsets.UTF_8))) {
				tWriter.write("# Configuration file");
				tWriter.newLine();
				tWriter.newLine();
				for (Map.Entry<String, Map<String, ConfigValue>> tCategoryEntry : mCategories.entrySet()) {
					if (tCategoryEntry.getValue().isEmpty()) continue;
					tWriter.write(tCategoryEntry.getKey() + " {");
					tWriter.newLine();
					for (Map.Entry<String, ConfigValue> tPropEntry : tCategoryEntry.getValue().entrySet()) {
						tWriter.write("\t" + tPropEntry.getKey() + "=" + tPropEntry.getValue().getString());
						tWriter.newLine();
					}
					tWriter.write("}");
					tWriter.newLine();
					tWriter.newLine();
				}
			}
		} catch (IOException e) {
			// совпадает с Configuration.save() (Configuration.java:1101-1104) — IO-ошибка не прерывает работу мода.
			e.printStackTrace();
		}
	}
}
