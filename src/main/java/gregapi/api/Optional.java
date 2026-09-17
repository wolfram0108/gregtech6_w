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

package gregapi.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Forge 1.7.10 stripped an optional interface or method at load time when the mod was missing;
 *  NeoForge does not, so this annotation is now just an empty marker, keeping every call site unchanged but for the import. */
public final class Optional {
	private Optional() {}

	/** Marks that the class conditionally implements iface from mod modid; empty marker in neo. */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface Interface {
		String iface();
		String modid();
		boolean striprefs() default false;
	}

	/** Holds multiple {@link Interface} annotations on one class. */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.TYPE)
	public @interface InterfaceList {
		Interface[] value();
	}

	/** Marks that the method only makes sense when mod modid is present; empty marker in neo. */
	@Retention(RetentionPolicy.SOURCE)
	@Target(ElementType.METHOD)
	public @interface Method {
		String modid();
	}
}
