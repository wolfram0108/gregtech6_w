/**
 * Copyright (c) 2019 Gregorius Techneticies
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

package gregapi.item;

import gregapi.data.LH;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/**
 * @author Gregorius Techneticies
 */
public class CreativeTab extends CreativeModeTab {
	public final String mName;
	public final Item mItem;
	public final short mMetaData;

	public CreativeTab(String aName, String aLocal, Item aItem, short aMetaData) {
		// F16 creative-tab (1:1): 1.7.10 CreativeTabs(String) → neo CreativeModeTab через Builder. Строим ПОЛНЫЙ builder
		// (title/icon/displayItems), а сам инстанс (валидный CreativeModeTab) регистрируется CreativeTabsGT на
		// RegisterEvent<CreativeModeTab>. Заголовок literal(aLocal): GT6-локализация не доходит до vanilla lang (BACKUPMAP),
		// поэтому даём готовую строку напрямую (тот же видимый результат). LH.add сохраняем (другой код читает ключ).
		super(CreativeModeTab.builder()
			.title(net.minecraft.network.chat.Component.literal(aLocal))
			.icon(() -> gregapi.util.ST.make(aItem, 1, aMetaData & 0xFFFF))
			.displayItems((aParams, aOutput) -> CreativeTabsGT.populate(aItem, aOutput)));
		LH.add("itemGroup." + aName, aLocal);
		mName = aName;
		mItem = aItem;
		mMetaData = aMetaData;
		CreativeTabsGT.registerOwnTab(this);
	}
	
	// @Override
	public Item getTabIconItem() {
		return mItem;
	}
	
	// @Override
	public int func_151243_f() {
		return mMetaData;
	}
}
