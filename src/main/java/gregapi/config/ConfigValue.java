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

/**
 * F12-config-subsystem: GT6-центр, воспроизводящий 1.7.10 Forge {@code net.minecraftforge.common.
 * config.Property} — единственная запись динамического, файлового конфига {@link ModConfigSpec}.
 * Причина существования: neo {@code net.neoforged.neoforge.common.ModConfigSpec.ConfigValue} —
 * ДЕКЛАРАТИВНАЯ модель (значение фиксируется на этапе Builder при регистрации мода, без per-call
 * {@code get(category,key,default)} и без {@code wasRead()} в духе 1.7.10) — архитектурно
 * несовместима с GT6's dynamic-config-паттерном. Референс поведения: {@code gregtech6/build/tmp/
 * recompSrc/net/minecraftforge/common/config/Property.java}.
 *
 * @author Gregorius Techneticies
 */
public class ConfigValue {
	final String mName;
	String mValue;
	String mDefaultValue;
	private final boolean mWasRead;

	ConfigValue(String aName, String aValue, boolean aWasRead) {
		mName = aName;
		mValue = aValue;
		mDefaultValue = aValue;
		mWasRead = aWasRead;
	}

	public String getName() {return mName;}

	/** было {@code Property.getString()} — сырое строковое значение свойства (Property не имеет перегрузки с параметром-дефолтом). */
	public String getString() {return mValue;}

	/** было {@code Property.getBoolean(boolean)} — Property.java:728-738. */
	public boolean getBoolean(boolean aDefault) {
		if ("true".equalsIgnoreCase(mValue) || "false".equalsIgnoreCase(mValue)) return Boolean.parseBoolean(mValue);
		return aDefault;
	}

	/** было {@code Property.getInt(int)} — Property.java:691-701. */
	public int getInt(int aDefault) {
		try {
			return Integer.parseInt(mValue);
		} catch (NumberFormatException e) {
			return aDefault;
		}
	}

	/** было {@code Property.getInt()} — Property.java:671-681 (без параметра — фолбэк на собственный дефолт свойства). */
	public int getInt() {
		try {
			return Integer.parseInt(mValue);
		} catch (NumberFormatException e) {
			return Integer.parseInt(mDefaultValue);
		}
	}

	/** было {@code Property.getDouble(double)} — Property.java:792-802. */
	public double getDouble(double aDefault) {
		try {
			return Double.parseDouble(mValue);
		} catch (NumberFormatException e) {
			return aDefault;
		}
	}

	/**
	 * было {@code Property.wasRead()} — Property.java:994-1003: true, если значение уже существовало в
	 * загруженном из файла конфиге на момент этого {@code get(category,key,default)}-вызова; false, если
	 * запись только что создана этим вызовом с дефолтным значением (новый ключ).
	 */
	public boolean wasRead() {return mWasRead;}
}
