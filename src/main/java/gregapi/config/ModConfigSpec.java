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

/**
 * F12-config-subsystem: the GT6 centre reproducing 1.7.10 Forge {@code net.minecraftforge.common.
 * config.Configuration} — a file-backed, dynamic, per-call config ({@code new Configuration(File)}
 * plus {@code get(category,key,default)}, readable and writable at any point at runtime).
 *
 * Why it exists (see decisions/, DEFERRED-LEDGER.md "F12, config-subsystem"): the neo
 * {@code net.neoforged.neoforge.common.ModConfigSpec} is a DECLARATIVE model built through a
 * {@code Builder} at mod registration (keys fixed up front), with no {@code new ModConfigSpec(File)},
 * no {@code .load()}/{@code .save()} and no per-call {@code get(category,key,default)} — a different
 * architecture, incompatible with the GT6 dynamic-config pattern ({@link gregapi.config.Config},
 * {@link gregapi.lang.LanguageHandler#sLangFile}). The engine changed the model, so it is adapted
 * centrally, the same way as F4-OreDictionary/F5-fluids/F9-block-material: Forge behaviour is
 * reproduced inside one gregapi-owned class.
 *
 * The file format is human-readable and structured like the 1.7.10 {@code .cfg} (a {@code {}} category
 * block with {@code key=value} inside), without the secondary features of the Forge format (S:/I:/B:/D:
 * type prefixes, {@code <...>} lists, comments, min/max, nested categories, START/END child files) —
 * no caller in this tree uses them: Config.java/GT_API.java/LanguageHandler.java only ever call the
 * four scalar overloads boolean/int/double/String.
 *
 * Behaviour reference: {@code net/minecraftforge/common/config/Configuration.java} (methods
 * {@code get(String,String,<type>)}, {@code load()}, {@code save()}, {@code getConfigFile()};
 * {@code wasRead()} semantics at Configuration.java:697-710 — true when the key was already in
 * {@code cat.containsKey(key)} at {@code get()} time, false when that call created it).
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

	/** was {@code Configuration.getCategory(String)} (Configuration.java:132-140): access to a category's entries.
	 *  Callers iterate the keys, e.g. the F16 creative-tab cache. */
	public Map<String, ConfigValue> getCategory(String aCategory) {
		return category(aCategory);
	}

	/** was {@code Configuration.get(String,String,boolean)} (Configuration.java:166-169). */
	public ConfigValue get(String aCategory, String aKey, boolean aDefault) {
		return get(aCategory, aKey, Boolean.toString(aDefault));
	}

	/** was {@code Configuration.get(String,String,int)} (Configuration.java:268-271). */
	public ConfigValue get(String aCategory, String aKey, int aDefault) {
		return get(aCategory, aKey, Integer.toString(aDefault));
	}

	/** was {@code Configuration.get(String,String,double)} (Configuration.java:410-413). */
	public ConfigValue get(String aCategory, String aKey, double aDefault) {
		return get(aCategory, aKey, Double.toString(aDefault));
	}

	/**
	 * was {@code Configuration.get(String,String,String,String,Property.Type)} (Configuration.java:688-724),
	 * the shared core of all four scalar overloads. A key already present in the loaded file returns the
	 * EXISTING entry with its value untouched and only the default refreshed (1:1 Configuration.java:707);
	 * otherwise a new entry is created with {@code aDefault} as its current value.
	 */
	public ConfigValue get(String aCategory, String aKey, String aDefault) {
		Map<String, ConfigValue> tCategory = category(aCategory);
		ConfigValue tExisting = tCategory.get(aKey);
		if (tExisting != null) {
			// Heal a value left by the old trimming bug: equal to the default minus its trailing spaces means it
			// was never edited by hand, it only lost them on an earlier load, so the file repairs itself on save.
			if (!aDefault.equals(tExisting.mValue) && aDefault.stripTrailing().equals(tExisting.mValue)) tExisting.mValue = aDefault;
			tExisting.mDefaultValue = aDefault;
			return tExisting;
		}
		ConfigValue tNew = new ConfigValue(aKey, aDefault, false);
		tCategory.put(aKey, tNew);
		return tNew;
	}

	/**
	 * was {@code Configuration.load()} (Configuration.java:791-1051): reads the file and fills categories with
	 * {@code wasRead=true} entries (Configuration.java:944). A missing file is not an error — a fresh install
	 * simply has no entries yet.
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
				// Value comes from the raw line, not the trimmed one: trailing spaces are significant in GT6 UI
				// strings ("Loss: ", "Use ") and trim() would glue them to the next word (Configuration.java:944).
				int tSplit = tLine.indexOf('=');
				if (tSplit < 0) continue;
				String tKey = tLine.substring(0, tSplit).trim();
				String tValue = tLine.substring(tSplit + 1);
				tCategory.put(tKey, new ConfigValue(tKey, tValue, true));
			}
		} catch (IOException e) {
			// Matches Configuration.load() (Configuration.java:1028-1031): an IO error must not abort mod loading.
			e.printStackTrace(gregapi.data.CS.ERR);
		}
	}

	/**
	 * was {@code Configuration.save()} (Configuration.java:1053-1117): writes every category and entry known
	 * so far back to the file, each as its {@code getString()} value rather than its default, in the format
	 * {@code category {\n\tkey=value\n}\n}.
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
			// Matches Configuration.save() (Configuration.java:1101-1104): an IO error must not abort the mod.
			e.printStackTrace(gregapi.data.CS.ERR);
		}
	}
}
