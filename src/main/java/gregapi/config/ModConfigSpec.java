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

/** Config subsystem: reproduces 1.7.10 Forge's Configuration (file-backed, per-call
 *  get(category,key,default), read/write at any time). */
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

	/** Was Configuration.getCategory(String): access to a category's entries; readers iterate keys
	 *  (e.g. the creative-tab cache). */
	public Map<String, ConfigValue> getCategory(String aCategory) {
		return category(aCategory);
	}

	/** Was Configuration.get(String,String,boolean). */
	public ConfigValue get(String aCategory, String aKey, boolean aDefault) {
		return get(aCategory, aKey, Boolean.toString(aDefault));
	}

	/** Was Configuration.get(String,String,int). */
	public ConfigValue get(String aCategory, String aKey, int aDefault) {
		return get(aCategory, aKey, Integer.toString(aDefault));
	}

	/** Was Configuration.get(String,String,double). */
	public ConfigValue get(String aCategory, String aKey, double aDefault) {
		return get(aCategory, aKey, Double.toString(aDefault));
	}

	/** Was Configuration.get(String,String,String,String,Property.Type), the shared core of all four
	 *  scalar overloads. A key already in the file returns the EXISTING entry; otherwise a new one is created with aDefault. */
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

	/** Was Configuration.load(): reads the file, fills categories with entries; a missing file isn't an
	 *  error (fresh install), just no entries. */
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
			// Matches Configuration.load(): an IO error doesn't abort mod loading.
			e.printStackTrace(gregapi.data.CS.ERR);
		}
	}

	/** Was Configuration.save(): writes every known category/entry back to the file, each as its current
	 *  string value, not the default. */
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
			// Matches Configuration.save(): an IO error doesn't abort the mod.
			e.printStackTrace(gregapi.data.CS.ERR);
		}
	}
}
