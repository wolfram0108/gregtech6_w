/**
 * Copyright (c) 2022 GregTech-6 Team
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

package gregapi.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;

/** Feeds a no-op Logger to ASM transformers to suppress foreign log crashes; extends AbstractLogger instead
 *  of hand-implementing the whole Logger interface, since that manual list broke on every log4j version bump. */
public class LoggerFML extends AbstractLogger {
	private static final long serialVersionUID = 1L;
	public String mName = "";

	public LoggerFML(Object aName) {
		super(aName == null ? "" : aName.toString());
		if (aName != null) mName = aName.toString();
	}

	@Override public Level getLevel() {return Level.OFF;}

	@Override public void logMessage(String aFQCN, Level aLevel, Marker aMarker, Message aMsg, Throwable aThrowable) {/* no-op logger */}

	@Override public boolean isEnabled(Level aLevel, Marker aMarker, Message aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, CharSequence aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, Object aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Throwable aThrowable) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object... aParams) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {return false;}
	@Override public boolean isEnabled(Level aLevel, Marker aMarker, String aMsg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {return false;}
}
