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

package gregapi.item;

import net.minecraftforge.api.distmarker.Dist;
import gregapi.GT_API;
import gregapi.api.Abstract_Mod;
import gregapi.cover.CoverRegistry;
import gregapi.cover.covers.CoverSelectorTag;
import gregapi.data.ANY;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.data.OP;
import gregapi.util.CR;
import gregapi.util.OM;
import gregapi.util.ST;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class ItemIntegratedCircuit extends ItemBase {
	public ItemIntegratedCircuit() {
		super(MD.GAPI.mID, "gt.integrated_circuit", "Selector Tag", "");
		setHasSubtypes(T);
		setMaxDamage(0);
		
		LH.add(mName + ".configuration", "Configuration: ");
		// Stack-init (OreDict data, recipes, cover registry) is moved into initDeferred, run post-freeze, because
		// construction happens on RegisterEvent when components are not yet bound and ST.make would fail.
		gregapi.GT_API.deferItemInit(this::initDeferred);
	}

	private void initDeferred() {
		OM.data(ST.make(this, 1, W), ANY.Iron, U2*11);
		
		CR.shaped(ST.make(this, 1, 0), CR.DEF_NCC, "GhG", "SSS", "GwG", 'G', OP.gearGtSmall.dat(ANY.Iron), 'S', OP.stick.dat(ANY.Iron));
		CR.shapeless(ST.make(this, 1, 0), CR.DEF, new Object[] {this});
		
		CR.shaped(ST.make(this, 1, 1), CR.DEF, "d ", " P", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1, 2), CR.DEF, "d ", "P ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1, 3), CR.DEF, " d", "P ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1, 4), CR.DEF, "Pd", "  ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1, 5), CR.DEF, "P ", " d", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1, 6), CR.DEF, "P ", "d ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1, 7), CR.DEF, " P", "d ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1, 8), CR.DEF, "dP", "  ", 'P', ST.make(this, 1, W));
		
		CR.shaped(ST.make(this, 1, 9), CR.DEF, "P d", "   ", "   ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,10), CR.DEF, "P  ", "  d", "   ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,11), CR.DEF, "P  ", "   ", "  d", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,12), CR.DEF, "P  ", "   ", " d ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,13), CR.DEF, "  P", "   ", "  d", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,14), CR.DEF, "  P", "   ", " d ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,15), CR.DEF, "  P", "   ", "d  ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,16), CR.DEF, "  P", "d  ", "   ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,17), CR.DEF, "   ", "   ", "d P", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,18), CR.DEF, "   ", "d  ", "  P", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,19), CR.DEF, "d  ", "   ", "  P", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,20), CR.DEF, " d ", "   ", "  P", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,21), CR.DEF, "d  ", "   ", "P  ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,22), CR.DEF, " d ", "   ", "P  ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,23), CR.DEF, "  d", "   ", "P  ", 'P', ST.make(this, 1, W));
		CR.shaped(ST.make(this, 1,24), CR.DEF, "   ", "  d", "P  ", 'P', ST.make(this, 1, W));
		
		for (byte i = 0; i < 16; i++) CoverRegistry.put(ST.make(this, 1, i), new CoverSelectorTag(i));
	}
	
	protected ResourceLocation[] mIcons = new ResourceLocation[256];

	private boolean mIconsRegistered = F;

	@Override
	// registerIcons is never called in neo, so the icon map stayed null and the item failed to render;
	// it is now filled lazily on first icon request using the same registerIcons scan.
	public ResourceLocation getIconFromDamage(int aMeta) {
		if (!mIconsRegistered) registerIcons(null);
		return mIcons[aMeta & 255];
	}
	
	@Override
	public void addAdditionalToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		super.addAdditionalToolTips(aList, aStack, aF3_H);
		aList.add(LH.get(mName + ".configuration", "Configuration: ") + getConfigurationString(getDamage(aStack)));
	}
	
	@Override
	public String getUnlocalizedName(ItemStack aStack) {
		return mName;
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public final void getSubItems(Item var1, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {
		aList.add(ST.make(this, 1, 0));
	}
	
	@Override
	// The old icon-registration hook (IIconRegister) is gone; the same path is now used to build a ResourceLocation directly.
	public void registerIcons(Object aIconRegister) {
		mIconsRegistered = T;
		for (int i = 0; i < 25/*TODO mIcons.length*/; i++) mIcons[i] = new ResourceLocation(mModID + ":" + mName + "/" + (byte)(i&255));
		// The 1.7.10 icon-load-phase dispatcher is dead in neo (GT_API.sItemIconload is cleared on init); icon loading
		// moved to the lazy TextureSet/ItemIcons path, so keeping the old lazy call here would iterate null and crash.
	}
	
	private static String getModeString(int aMetaData) {
		switch ((byte)(aMetaData >>> 8)) {
		case  0: return "==";
		case  1: return "<=";
		case  2: return ">=";
		case  3: return "<";
		case  4: return ">";
		default: return "";
		}
	}
	
	private static String getConfigurationString(int aMetaData) {
		return getModeString(aMetaData) + " " + (byte)(aMetaData & 255);
	}
}
