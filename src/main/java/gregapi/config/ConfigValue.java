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

/** @author Gregorius Techneticies
 *  Reproduces Forge 1.7.10's dynamic, file-backed Property: neo's ModConfigSpec.ConfigValue is a declarative
 *  model fixed at builder registration, with no per-call get(category,key,default) or wasRead(). */
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

	/** No default-value overload exists here because the original Property.getString() never had one either. */
	public String getString() {return mValue;}

	public boolean getBoolean(boolean aDefault) {
		if ("true".equalsIgnoreCase(mValue) || "false".equalsIgnoreCase(mValue)) return Boolean.parseBoolean(mValue);
		return aDefault;
	}

	public int getInt(int aDefault) {
		try {
			return Integer.parseInt(mValue);
		} catch (NumberFormatException e) {
			return aDefault;
		}
	}

	/** With no argument, this falls back to the property's own default value instead of a caller-supplied one. */
	public int getInt() {
		try {
			return Integer.parseInt(mValue);
		} catch (NumberFormatException e) {
			return Integer.parseInt(mDefaultValue);
		}
	}

	public double getDouble(double aDefault) {
		try {
			return Double.parseDouble(mValue);
		} catch (NumberFormatException e) {
			return aDefault;
		}
	}

	/** True if the value already existed in the loaded config file at this get() call; false if this call
	 *  just created the entry with its default (a new key). */
	public boolean wasRead() {return mWasRead;}
}
