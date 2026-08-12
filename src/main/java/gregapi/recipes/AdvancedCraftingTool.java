/**
 * Copyright (c) 2025 GregTech-6 Team
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

package gregapi.recipes;

import gregapi.code.ICondition;
import gregapi.data.*;
import gregapi.item.multiitem.MultiItemTool;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.event.IOreDictListenerEvent;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import gregapi.recipes.ShapelessOreRecipe;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AdvancedCraftingTool extends ShapelessOreRecipe implements ICraftingRecipeGT, IOreDictListenerEvent {
	public final ICondition mCondition;
	public final OreDictPrefix mToolHead;
	public final short mToolID;
	public final MultiItemTool mTool;
	
	public AdvancedCraftingTool(long aToolID, OreDictPrefix aToolHead) {this(ToolsGT.sMetaTool, aToolID, aToolHead, ICondition.TRUE, MT.Steel);}
	public AdvancedCraftingTool(long aToolID, OreDictPrefix aToolHead, OreDictMaterial aHead) {this(ToolsGT.sMetaTool, aToolID, aToolHead, ICondition.TRUE, aHead);}
	public AdvancedCraftingTool(long aToolID, OreDictPrefix aToolHead, ICondition aCondition) {this(ToolsGT.sMetaTool, aToolID, aToolHead, aCondition, MT.Steel);}
	public AdvancedCraftingTool(long aToolID, OreDictPrefix aToolHead, ICondition aCondition, OreDictMaterial aHead) {this(ToolsGT.sMetaTool, aToolID, aToolHead, aCondition, aHead);}
	public AdvancedCraftingTool(MultiItemTool aTool, long aToolID, OreDictPrefix aToolHead) {this(aTool, aToolID, aToolHead, ICondition.TRUE, MT.Steel);}
	public AdvancedCraftingTool(MultiItemTool aTool, long aToolID, OreDictPrefix aToolHead, OreDictMaterial aHead) {this(aTool, aToolID, aToolHead, ICondition.TRUE, aHead);}
	public AdvancedCraftingTool(MultiItemTool aTool, long aToolID, OreDictPrefix aToolHead, ICondition aCondition) {this(aTool, aToolID, aToolHead, aCondition, MT.Steel);}
	public AdvancedCraftingTool(MultiItemTool aTool, long aToolID, OreDictPrefix aToolHead, ICondition aCondition, OreDictMaterial aHead) {
		super(aTool.getToolWithStats(UT.Code.bind15(aToolID), aHead, aHead.mHandleMaterial), aToolHead.dat(aHead).toString(), OP.stick.dat(aHead.mHandleMaterial).toString());
		mTool = aTool;
		mCondition = aCondition;
		mToolHead = aToolHead;
		mToolID = UT.Code.bind15(aToolID);

		aToolHead.addListener(this);
	}
	
	@Override
	public void onOreRegistration(OreDictRegistrationContainer aEvent) {
		if (aEvent.mMaterial != MT.Empty && mCondition.isTrue(aEvent.mMaterial)) {
			// F-toolhead-handle-timing: рукоять берётся из IL.Stick (ванильный stick, LoaderItemList) / OP.stick.mat(handleMat)
			// (stick-префикс). В порту весь stack-init (LoaderItemList + prefix-регистрация) отложен на server-start
			// (runDeferredItemInit), и в момент срабатывания этого OreDict-листенера sticks ещё НЕ готовы (IL.Stick=null,
			// OP.stick.mat=null) → рукоять терялась (порт выдавал toolhead-рецепты без рукояти, паритет toolhead=0). Golden
			// (1.7.10, синхронно) имел sticks готовыми к этому моменту. Fix: отложить генерацию рецепта в конец drain-очереди —
			// выполнится, когда IL.Stick/OP.stick уже зарегистрированы (тот же приём отложки stack-init, что во всём порту).
			final OreDictMaterial tMat = aEvent.mMaterial;
			final ItemStack tHead = ST.copy(aEvent.mStack);
			gregapi.GT_API.deferItemInit(() -> {
				if (tMat.mHandleMaterial == ANY.Wood || tMat.mHandleMaterial == ANY.WoodPlastic) {
					RM.ToolHeads.addRecipeX(F,F,T,F,F, 0, 0, ST.array(tHead, NI, IL.Stick.get(1)), mTool.getToolWithStats(mToolID, tMat, tMat.mHandleMaterial));
				} else {
					if (!tMat.mHandleMaterial.contains(TD.Properties.INVALID_MATERIAL)) {
						ItemStack tHandle = OP.stick.mat(tMat.mHandleMaterial, 1);
						if (ST.valid(tHandle)) RM.ToolHeads.addRecipeX(F,F,T,F,F, 0, 0, ST.array(tHead, NI, tHandle), mTool.getToolWithStats(mToolID, tMat, tMat.mHandleMaterial));
					}
					for (OreDictMaterial tHandleMaterial : tMat.mHandleMaterial.mToThis)
					if (!tHandleMaterial                 .contains(TD.Properties.INVALID_MATERIAL)) {
						ItemStack tHandle = OP.stick.mat(tHandleMaterial                 , 1);
						if (ST.valid(tHandle)) RM.ToolHeads.addRecipeX(F,F,T,F,F, 0, 0, ST.array(tHead, NI, tHandle), mTool.getToolWithStats(mToolID, tMat, tHandleMaterial));
					}
				}
			});
		}
	}
	
	@Override
	public boolean matches(CraftingContainer aGrid, Level aWorld) {
		ItemStack tStack = null;
		OreDictMaterial rHead = null, rRod = null;
		// 1.20.1: InventoryCrafting → CraftingContainer, getSizeInventory()/getStackInSlot(i) →
		// getContainerSize()/getItem(i) (forge-1201-decompiled net/minecraft/world/Container.java).
		for (int i = 0; i < aGrid.getContainerSize(); i++) {
			tStack = aGrid.getItem(i);
			if (ST.valid(tStack)) {
				OreDictItemData tData = OM.anydata_(tStack);
				if (tData == null) return F;
				if (tData.mPrefix == OP.stick) {
					if (rRod != null) return F;
					rRod = tData.mMaterial.mMaterial;
				} else if (tData.mPrefix == mToolHead) {
					if (rHead != null) return F;
					rHead = tData.mMaterial.mMaterial;
				} else {
					return F;
				}
			}
		}
		return rHead != null && rRod != null && rHead != MT.Empty && mCondition.isTrue(rHead) && (rHead.mHandleMaterial == rRod || rRod.mReRegistrations.contains(rHead.mHandleMaterial));
	}
	
	@Override
	public ItemStack getCraftingResult(CraftingContainer aGrid) {
		ItemStack tStack = null;
		OreDictMaterial rHead = null, rRod = null;
		for (int i = 0; i < aGrid.getContainerSize(); i++) {
			tStack = aGrid.getItem(i);
			if (ST.valid(tStack)) {
				OreDictItemData tData = OM.anydata_(tStack);
				if (tData == null) continue;
				if (tData.mPrefix == OP.stick) {
					rRod = tData.mMaterial.mMaterial;
				} else if (tData.mPrefix == mToolHead) {
					rHead = tData.mMaterial.mMaterial;
				}
			}
		}
		return rHead == MT.Empty ? NI : mTool.getToolWithStats(mToolID, rHead, rRod);
	}
	
	@Override public boolean isRemovableByGT() {return F;}
	@Override public boolean isAutocraftableByGT() {return T;}
	@Override public int getRecipeSize() {return 2;}
}
