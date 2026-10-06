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
 *
 * Modified in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w): ported from Minecraft 1.7.10 / Forge
 * to Minecraft 26.1.2 / NeoForge.
 */

package gregapi.compat.buildcraft;

import java.util.Collection;

import buildcraft.api.core.render.ISprite;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.ITriggerInternalSided;
import buildcraft.api.statements.ITriggerProvider;
import buildcraft.api.statements.StatementManager;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.util.UT;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

public abstract class TriggerBC implements ITriggerExternal, ITriggerProvider {
	public final String mModID, mName;
	/** The 1.7.10 icon "modid:triggers/name"; made on clients only, since it reads the client's texture atlas. */
	private final ISprite mSprite;
	
	public TriggerBC(String aModID, String aName, String aDesciption) {
		mModID = aModID;
		mName = aName;
		LH.add("bc.trigger."+mModID+"."+mName, aDesciption);
		mSprite = gregapi.data.CS.CODE_CLIENT ? new TriggerIconBC(new net.minecraft.resources.ResourceLocation(mModID, "triggers/" + mName)) : null;

		StatementManager.registerStatement(this);
		StatementManager.registerTriggerProvider(this);
	}
	
	
	@Override public String getUniqueTag() {return mModID + ":" + mName;}
	@Override public ISprite getSprite() {return mSprite;}
	@Override public int maxParameters() {return 0;}
	@Override public int minParameters() {return 0;}
	// BuildCraft 7.99 shows the description as a Component and keys its guide pages by getDescriptionKey.
	@Override public net.minecraft.network.chat.Component getDescription() {return net.minecraft.network.chat.Component.literal(LanguageHandler.translate(getDescriptionKey()));}
	@Override public String getDescriptionKey() {return "bc.trigger."+mModID+"."+mName;}
	@Override public IStatementParameter createParameter(int aIndex) {return null;}
	@Override public IStatement rotateLeft() {return null;}
	@Override public IStatement[] getPossible() {return new IStatement[] {this};}
	@Override public boolean isTriggerActive(BlockEntity aTarget, Direction aSide, IStatementContainer aSource, IStatementParameter[] aParameters) {return isApplicable(aTarget, UT.Code.side(aSide)) ? isActive(aTarget, UT.Code.side(aSide), aSource, aParameters) : false;}
	@Override public void addInternalTriggers(Collection<ITriggerInternal> aTriggers, IStatementContainer aContainer) {/**/}
	@Override public void addInternalSidedTriggers(Collection<ITriggerInternalSided> aTriggers, IStatementContainer aContainer, Direction aSide) {/**/}
	@Override public void addExternalTriggers(Collection<ITriggerExternal> aTriggers, Direction aSide, BlockEntity aTarget) {if (isApplicable(aTarget, UT.Code.side(aSide))) aTriggers.add(this);}
	
	public abstract boolean isActive(BlockEntity aTarget, byte aSideOfTileEntity, IStatementContainer aSource, IStatementParameter[] aParameters);
	public abstract boolean isApplicable(BlockEntity aTarget, byte aSideOfTileEntity);
}
