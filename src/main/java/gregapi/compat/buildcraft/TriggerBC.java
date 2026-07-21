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

package gregapi.compat.buildcraft;

import static gregapi.data.CS.*;

import java.util.Collection;

import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerProvider;
import buildcraft.api.statements.StatementManager;
import gregapi.code.ArrayListNoNulls;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.util.UT;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

public abstract class TriggerBC implements ITriggerExternal, ITriggerProvider {
	public final String mModID, mName;
	// F3 superseded-render: было IIcon mIcon (1.7.10 атлас-стежка, тип удалён в neo) — F10 BuildCraft-триггер, ветка мертва (BC не портирован).
	public Object mIcon;
	
	public TriggerBC(String aModID, String aName, String aDesciption) {
		mModID = aModID;
		mName = aName;
		LH.add("bc.trigger."+mModID+"."+mName, aDesciption);

		StatementManager.registerStatement(this);
		StatementManager.registerTriggerProvider(this);
	}
	
	
	public String getUniqueTag() {return mModID + ":" + mName;}
	public Object getIcon() {return mIcon;}
	// F3 superseded-render: было registerIcons(IIconRegister) {mIcon = aIconRegister.registerIcon(...)} — 1.7.10 атлас-стежка мертва (F10 BuildCraft, ветка недостижима). Тип-параметр → Object (mirror-класс IIconRegister удалён из neo/production-jar, в сигнатуре ломал рефлексию).
	public void registerIcons(Object aIconRegister) {/**/}
	public int maxParameters() {return 0;}
	public int minParameters() {return 0;}
	public String getDescription() {return LanguageHandler.translate("bc.trigger."+mModID+"."+mName);}
	public IStatementParameter createParameter(int aIndex) {return null;}
	public IStatement rotateLeft() {return null;}
	public boolean isTriggerActive(BlockEntity aTarget, Direction aSide, IStatementContainer aSource, IStatementParameter[] aParameters) {return isApplicable(aTarget, UT.Code.side(aSide)) ? isActive(aTarget, UT.Code.side(aSide), aSource, aParameters) : false;}
	public Collection<ITriggerInternal> getInternalTriggers(IStatementContainer container) {return null;}
	public Collection<ITriggerExternal> getExternalTriggers(Direction aSide, BlockEntity aTarget) {return isApplicable(aTarget, UT.Code.side(aSide)) ? new ArrayListNoNulls<ITriggerExternal>(F, this) : null;}
	
	public abstract boolean isActive(BlockEntity aTarget, byte aSideOfTileEntity, IStatementContainer aSource, IStatementParameter[] aParameters);
	public abstract boolean isApplicable(BlockEntity aTarget, byte aSideOfTileEntity);
}
