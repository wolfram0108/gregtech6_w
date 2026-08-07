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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.code;
import gregapi.util.WD;

import net.neoforged.fml.ModList;
import gregapi.util.ST;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregorius Techneticies
 */
public final class ModData implements ICondition<ITagDataContainer<?>> {
	public static final Map<String, ModData> MODS = new HashMap<>();
	
	public boolean mLoaded;
	
	public final String mID, mName, mPrefix;
	
	public ModData(String aID, String aName) {
		mID = aID;
		mName = aName;
		mPrefix = mID + ":";
		mLoaded = ModList.get().isLoaded(mID);
		MODS.put(aID, this);
		MODS.put(aID.toLowerCase(), this);
		MODS.put(aID.toUpperCase(), this);
	}
	
	public ModData setLoaded(boolean aLoaded) {
		mLoaded = aLoaded;
		return this;
	}
	
	public boolean owns (BlockGetter aWorld, int aX, int aY, int aZ) {return mLoaded && owns_(ST.regName(WD.block(aWorld, aX, aY, aZ)));}
	public boolean owns (Block        aBlock                        ) {return mLoaded && owns_(ST.regName(aBlock));}
	public boolean owns (Item         aItem                         ) {return mLoaded && owns_(ST.regName(aItem));}
	public boolean owns (ItemStack    aStack                        ) {return mLoaded && owns_(ST.regName(aStack));}
	public boolean owns (String       aRegName                      ) {return mLoaded && owns_(aRegName);}
	public boolean owns_(String       aRegName                      ) {return aRegName != null && aRegName.startsWith(mPrefix);}
	public boolean owns (BlockGetter aWorld, int aX, int aY, int aZ, String aContains) {return mLoaded && owns_(ST.regName(WD.block(aWorld, aX, aY, aZ)), aContains);}
	public boolean owns (Block        aBlock                        , String aContains) {return mLoaded && owns_(ST.regName(aBlock), aContains);}
	public boolean owns (Item         aItem                         , String aContains) {return mLoaded && owns_(ST.regName(aItem), aContains);}
	public boolean owns (ItemStack    aStack                        , String aContains) {return mLoaded && owns_(ST.regName(aStack), aContains);}
	public boolean owns (String       aRegName                      , String aContains) {return mLoaded && owns_(aRegName, aContains);}
	public boolean owns_(String       aRegName                      , String aContains) {return aRegName != null && aRegName.startsWith(mPrefix) && aRegName.contains(aContains);}
	
	@Override
	public String toString() {
		return mID;
	}
	
	@Override
	public boolean isTrue(ITagDataContainer<?> aObject) {
		return mLoaded;
	}
	
	@Override
	public boolean equals(Object aObject) {
		return aObject instanceof ModData && ((ModData)aObject).mID.equals(mID);
	}
	
	@Override
	public int hashCode() {
		return mID.hashCode();
	}
}
