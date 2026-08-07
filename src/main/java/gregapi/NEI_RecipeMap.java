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

package gregapi;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemList;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import codechicken.nei.recipe.*;
import net.neoforged.fml.InterModComms;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.code.ItemStackContainer;
import gregapi.data.*;
import gregapi.gui.ContainerClient;
import gregapi.lang.LanguageHandler;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictManager;
import gregapi.oredict.OreDictPrefix;
import gregapi.recipes.Recipe;
import gregapi.recipes.Recipe.RecipeMap;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static gregapi.data.CS.*;

public class NEI_RecipeMap extends TemplateRecipeHandler {
	protected final RecipeMap mRecipeMap;
	
	public static final int sOffsetX = 5, sOffsetY = 11;
	
	public NEI_RecipeMap(RecipeMap aRecipeMap) {
		mRecipeMap = aRecipeMap;
		transferRects.add(new RecipeTransferRect(new Rectangle(70-sOffsetX, 24-sOffsetY, 36, 18), getOverlayIdentifier()));
	}
	
	public NEI_RecipeMap init() {
		if (NEI_NH) {
			API.registerRecipeHandler(this);
			API.registerUsageHandler(this);
			
			CompoundTag tNBT = UT.NBT.make();
			tNBT.putString("modId"            , MD.GT.mID);
			tNBT.putString("modName"          , MD.GT.mName);
			tNBT.putString("handler"          , mRecipeMap.mNameNEI);
			tNBT.putString("itemName"         , ST.regMeta(mRecipeMap.mRecipeMachineList.isEmpty() ? ST.make(Blocks.FURNACE, 1, 0) : mRecipeMap.mRecipeMachineList.get(0)));
			tNBT.putInt("handlerHeight"    , 135);
			tNBT.putInt("handlerWidth"     , 166);
			tNBT.putInt("maxRecipesPerPage",   2);
			tNBT.putInt("yShift"           ,   6);
			tNBT.putBoolean("modRequired"      ,   T);
			InterModComms.sendTo("NotEnoughItems", "registerHandlerInfo", () -> tNBT);
		} else {
			GuiCraftingRecipe.craftinghandlers.add(this);
			GuiUsageRecipe.usagehandlers.add(this);
		}
		
		/* F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code FMLInterModComms.sendRuntimeMessage(sender,modId,method,String)}
		 * (Forge 1.7.10, тип+метод удалены) — neo {@code InterModComms.sendTo(senderModId,modId,method,Supplier<?>)}
		 * (`fml-decompiled/net/neoforged/fml/InterModComms.java:27`), тот же паттерн уже применён строкой выше (:88). */
		InterModComms.sendTo(GAPI.getModID(), "NEIPlugins", "register-crafting-handler", () -> MD.GAPI.mID+"@"+getRecipeName()+"@"+getOverlayIdentifier());
		return this;
	}
	
	// @Override
	public TemplateRecipeHandler newInstance() {
		return new NEI_RecipeMap(mRecipeMap);
	}
	
	public class FixedPositionedStack extends PositionedStack {
		public boolean permutated = F;
		public final int mChance, mMaxChance;

		public FixedPositionedStack(Object object, int x, int y) {
			this(object, x, y, 0, 0);
		}

		public FixedPositionedStack(Object object, int x, int y, int aChance, int aMaxChance) {
			super(object, x-sOffsetX, y-sOffsetY, T);
			mMaxChance = aMaxChance;
			mChance = aChance;
		}

		// @Override
		public void generatePermutations() {
			if (permutated) return;
			
			ArrayList<ItemStack> tDisplayStacks = ST.arraylist();
			for (ItemStack tStack : items) if (ST.valid(tStack)) {
				if (ST.meta_(tStack) == W) {
					List<ItemStack> permutations = ItemList.itemMap.get(tStack.getItem());
					if (!permutations.isEmpty()) {
						for(ItemStack stack : permutations) tDisplayStacks.add(ST.amount(tStack.getCount(), stack));
					} else {
						ItemStack base = ST.make(tStack.getItem(), tStack.getCount(), 0);
						ItemNBT.set(base, ItemNBT.get(tStack));
						tDisplayStacks.add(base);
					}
				} else {
					tDisplayStacks.add(ST.copy(tStack));
				}
			}
			
			items = tDisplayStacks.toArray(ZL_IS);
			if (items.length == 0) items = ST.array(ST.make(Blocks.FIRE, 1, 0));
			permutated = T;
			setPermutationToRender(0);
		}
	}

	public class CachedDefaultRecipe extends CachedRecipe {
		public final Recipe mRecipe;
		
		public final List<PositionedStack> mOutputs = new ArrayListNoNulls<>();
		public final List<PositionedStack> mInputs  = new ArrayListNoNulls<>();
		
		// @Override
		public List<PositionedStack> getIngredients() {
			return getCycledIngredients(cycleticks / 10, mInputs);
		}
		
		// @Override
		public PositionedStack getResult() {
			return null;
		}
		
		// @Override
		public List<PositionedStack> getOtherStacks() {
			return mOutputs;
		}
		
		public CachedDefaultRecipe(Recipe aRecipe) {
			mRecipe = aRecipe;

			int tStartIndex = 0;

			switch (mRecipeMap.mInputItemsCount) {
			case  0:
				break;
			case  1:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>6? 7:25))); tStartIndex++;
				break;
			case  2:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>6? 7:25))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>6? 7:25))); tStartIndex++;
				break;
			case  3:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, (mRecipeMap.mInputFluidCount>6? 7:25))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>6? 7:25))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>6? 7:25))); tStartIndex++;
				break;
			case  4:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>3?25:34))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>3?25:34))); tStartIndex++;
				break;
			case  5:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>3?25:34))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>3?25:34))); tStartIndex++;
				break;
			case  6:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>3? 7:16))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, (mRecipeMap.mInputFluidCount>3?25:34))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, (mRecipeMap.mInputFluidCount>3?25:34))); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, (mRecipeMap.mInputFluidCount>3?25:34))); tStartIndex++;
				break;
			case  7:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 43)); tStartIndex++;
				break;
			case  8:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 43)); tStartIndex++;
				break;
			case  9:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 43)); tStartIndex++;
				break;
			case 10:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 61)); tStartIndex++;
				break;
			case 11:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 61)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 61)); tStartIndex++;
				break;
			default:
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53,  7)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 25)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 43)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 17, 61)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 35, 61)); tStartIndex++;
				if (aRecipe.getRepresentativeInput(tStartIndex) != null) mInputs.add(new FixedPositionedStack(aRecipe.getRepresentativeInput(tStartIndex), 53, 61)); tStartIndex++;
				break;
			}

			if (aRecipe.mSpecialItems != null) mInputs.add(new FixedPositionedStack(aRecipe.mSpecialItems,  80, 43));
			if (!mRecipeMap.mRecipeMachineList.isEmpty()) mInputs.add(new FixedPositionedStack(mRecipeMap.mRecipeMachineList, 152, 83));

			tStartIndex = 0;

			switch (mRecipeMap.mOutputItemsCount) {
			case  0:
				break;
			case  1:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>6? 7:25), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  2:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>6? 7:25), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>6? 7:25), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  3:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>6? 7:25), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>6? 7:25), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, (mRecipeMap.mOutputFluidCount>6? 7:25), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  4:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>3?25:34), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>3?25:34), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  5:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>3?25:34), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>3?25:34), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  6:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, (mRecipeMap.mOutputFluidCount>3? 7:16), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, (mRecipeMap.mOutputFluidCount>3?25:34), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, (mRecipeMap.mOutputFluidCount>3?25:34), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, (mRecipeMap.mOutputFluidCount>3?25:34), aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  7:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  8:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case  9:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case 10:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 61, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			case 11:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 61, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 61, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			default:
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143,  7, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 25, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 43, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 107, 61, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 125, 61, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				if (aRecipe.getOutput(tStartIndex) != null) mOutputs.add(new FixedPositionedStack(aRecipe.getOutput(tStartIndex), 143, 61, aRecipe.getOutputChance(tStartIndex), aRecipe.getMaxChance(tStartIndex))); tStartIndex++;
				break;
			}

			for (int i = 0; i < aRecipe.mFluidInputs .length && i < mRecipeMap.mInputFluidCount ; i++) if (aRecipe.mFluidInputs [i] != null && aRecipe.mFluidInputs [i].getFluid() != null) mInputs .add(new FixedPositionedStack(FL.display(aRecipe.mFluidInputs [i], T, F, mRecipeMap.mUseBucketSizeIn ),  53 - (i%3)*18, 63 - (i/3)*18));
			for (int i = 0; i < aRecipe.mFluidOutputs.length && i < mRecipeMap.mOutputFluidCount; i++) if (aRecipe.mFluidOutputs[i] != null && aRecipe.mFluidOutputs[i].getFluid() != null) mOutputs.add(new FixedPositionedStack(FL.display(aRecipe.mFluidOutputs[i], T, F, mRecipeMap.mUseBucketSizeOut), 107 + (i%3)*18, 63 - (i/3)*18));
		}
	}

	static {
		GuiContainerManager.addInputHandler(new GT_RectHandler());
		GuiContainerManager.addTooltipHandler(new GT_RectHandler());
	}

	public static class GT_RectHandler implements IContainerInputHandler, IContainerTooltipHandler {
		// @Override
		public boolean mouseClicked(ContainerClient gui, int mousex, int mousey, int button) {
			if (canHandle(gui)) {
				if (button == 0) return transferRect(gui, F);
				if (button == 1) return transferRect(gui, T);
			}
			return F;
		}

		// @Override
		public boolean lastKeyTyped(ContainerClient gui, char keyChar, int keyCode) {
			return F;
		}

		public boolean canHandle(ContainerClient gui) {
			return gui instanceof ContainerClient && UT.Code.stringValid(((ContainerClient)gui).mNEI);
		}

		// @Override
		public List<String> handleTooltip(ContainerClient gui, int mousex, int mousey, List<String> currenttip) {
			if (canHandle(gui) && currenttip.isEmpty() && new Rectangle(65, 13, 36, 18).contains(new Point(GuiDraw.getMousePosition().x - ((ContainerClient)gui).getLeft() - RecipeInfo.getGuiOffset(gui)[0], GuiDraw.getMousePosition().y - ((ContainerClient)gui).getTop() - RecipeInfo.getGuiOffset(gui)[1]))) currenttip.add("Recipes");
			return currenttip;
		}

		private boolean transferRect(ContainerClient gui, boolean usage) {
			return canHandle(gui) && new Rectangle(65, 13, 36, 18).contains(new Point(GuiDraw.getMousePosition().x - ((ContainerClient)gui).getLeft() - RecipeInfo.getGuiOffset(gui)[0], GuiDraw.getMousePosition().y - ((ContainerClient)gui).getTop() - RecipeInfo.getGuiOffset(gui)[1])) && (usage ? GuiUsageRecipe.openRecipeGui(((ContainerClient)gui).mNEI) : GuiCraftingRecipe.openRecipeGui(((ContainerClient)gui).mNEI));
		}

		// @Override
		public List<String> handleItemDisplayName(ContainerClient gui, ItemStack itemstack, List<String> currenttip) {
			return currenttip;
		}

		// @Override
		public List<String> handleItemTooltip(ContainerClient gui, ItemStack itemstack, int mousex, int mousey, List<String> currenttip) {
			return currenttip;
		}

		// @Override
		public boolean keyTyped(ContainerClient gui, char keyChar, int keyCode) {
			return F;
		}

		// @Override
		public void onKeyTyped(ContainerClient gui, char keyChar, int keyID) {
			//
		}

		// @Override
		public void onMouseClicked(ContainerClient gui, int mousex, int mousey, int button) {
			//
		}

		// @Override
		public void onMouseUp(ContainerClient gui, int mousex, int mousey, int button) {
			//
		}

		// @Override
		public boolean mouseScrolled(ContainerClient gui, int mousex, int mousey, int scrolled) {
			return F;
		}

		// @Override
		public void onMouseScrolled(ContainerClient gui, int mousex, int mousey, int scrolled) {
			//
		}

		// @Override
		public void onMouseDragged(ContainerClient gui, int mousex, int mousey, int button, long heldTime) {
			//
		}
	}
	
	/** Thanks to codewarrior0 for doing most of this, because Greg was way too lazy to do it, but not lazy enough to not fix it. XD */
	public void sortRecipes() {
		if (arecipes.size() > 1000) return;
		Collections.sort(arecipes, new Comparator<CachedRecipe>() {@Override public int compare(CachedRecipe aRecipe1, CachedRecipe aRecipe2) {
			Recipe tRecipe1 = ((CachedDefaultRecipe)aRecipe1).mRecipe, tRecipe2 = ((CachedDefaultRecipe)aRecipe2).mRecipe;
			int
			tCompare = Long.compare(tRecipe1.mEUt, tRecipe2.mEUt);
			if (tCompare != 0) return tCompare;
			tCompare = Integer.compare(tRecipe1.mFluidInputs.length, tRecipe2.mFluidInputs.length);
			if (tCompare != 0) return tCompare;
			if (tRecipe1.mFluidInputs.length > 0) {
				tCompare = FL.regName(tRecipe1.mFluidInputs[0]).compareTo(FL.regName(tRecipe2.mFluidInputs[0]));
				if (tCompare != 0) return tCompare;
			}
			tCompare = Integer.compare(tRecipe1.mInputs.length, tRecipe2.mInputs.length);
			if (tCompare != 0) return tCompare;
			if (tRecipe1.mInputs.length > 0) {
				ItemStack tInput1 = tRecipe1.mInputs[0], tInput2 = tRecipe2.mInputs[0];
				if (ST.valid(tInput1)) {
					if (ST.invalid(tInput2)) return -1;
					
					OreDictItemData tData1 = OM.anydata_(tInput1), tData2 = OM.anydata_(tInput2);
					if (tData1 != null && tData1.validMaterial()) {
						if (tData2 == null || !tData2.validMaterial()) return -1;
						tCompare = tData1.mMaterial.mMaterial.mNameInternal.compareTo(tData2.mMaterial.mMaterial.mNameInternal);
						if (tCompare != 0) return tCompare;
						tCompare = Long.compare(tData1.mMaterial.mAmount, tData2.mMaterial.mAmount);
						if (tCompare != 0) return tCompare;
					} else if (tData2 != null && tData2.validMaterial()) return 1;
					
					tCompare = Long.compare(tRecipe1.mDuration, tRecipe2.mDuration);
					if (tCompare != 0) return tCompare;
					tCompare = ST.regName(tInput1).compareTo(ST.regName(tInput2));
					if (tCompare != 0) return tCompare;
					return Short.compare(ST.meta_(tInput1), ST.meta_(tInput2));
				}
				if (ST.valid(tInput2)) return 1;
			}
			return 0;
		}});
	}
	
	// @Override
	public void loadCraftingRecipes(String aID, Object... aResults) {
		if (!CODE_CLIENT) return;
		if (aID.equals(getOverlayIdentifier())) {
			for (Recipe tRecipe : mRecipeMap.getNEIAllRecipes()) arecipes.add(new CachedDefaultRecipe(tRecipe));
			sortRecipes();
		} else {
			super.loadCraftingRecipes(aID, aResults);
		}
	}
	
	// @Override
	public void loadCraftingRecipes(ItemStack aResult) {
		if (!CODE_CLIENT) return;
		if (ST.invalid(aResult)) return;
		try {
			OreDictItemData tPrefixMaterial = OM.association_(aResult);
			
			ArrayList<ItemStack> tResults = ST.arraylist(aResult, OM.get_(aResult));
			
			ArrayList<ItemStack>
			tRedirects = ItemsGT.sNEIRedirects.get(new ItemStackContainer(aResult));
			if (tRedirects != null) tResults.addAll(tRedirects);
			tRedirects = ItemsGT.sNEIRedirects.get(new ItemStackContainer(aResult, W));
			if (tRedirects != null) tResults.addAll(tRedirects);
			
			if (tPrefixMaterial != null && !tPrefixMaterial.mBlackListed) {
				if (tPrefixMaterial.mMaterial.mMaterial.mID > 0 && BlocksGT.ore != null && BlocksGT.oreBroken != null && tPrefixMaterial.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED)) {
					tResults.add(ST.make((Block)BlocksGT.ore      , 1, tPrefixMaterial.mMaterial.mMaterial.mID));
					tResults.add(ST.make((Block)BlocksGT.oreBroken, 1, tPrefixMaterial.mMaterial.mMaterial.mID));
				}
				for (OreDictPrefix tPrefix : tPrefixMaterial.mPrefix.mFamiliarPrefixes) {
					tResults.add(tPrefix.mat(tPrefixMaterial.mMaterial.mMaterial, 1));
				}
				if (tPrefixMaterial.mPrefix.containsAny(TD.Prefix.DUST_BASED) && tPrefixMaterial.mMaterial.mMaterial.mTargetPulver.mMaterial == tPrefixMaterial.mMaterial.mMaterial) {
					tResults.add(OP.crushed               .mat(tPrefixMaterial.mMaterial.mMaterial, 1));
					tResults.add(OP.crushedTiny           .mat(tPrefixMaterial.mMaterial.mMaterial, 1));
					tResults.add(OP.crushedPurified       .mat(tPrefixMaterial.mMaterial.mMaterial, 1));
					tResults.add(OP.crushedPurifiedTiny   .mat(tPrefixMaterial.mMaterial.mMaterial, 1));
					tResults.add(OP.crushedCentrifuged    .mat(tPrefixMaterial.mMaterial.mMaterial, 1));
					tResults.add(OP.crushedCentrifugedTiny.mat(tPrefixMaterial.mMaterial.mMaterial, 1));
				}
			}
			
			if (!ItemsGT.NEI_DONT_SHOW_FLUIDS.contains(aResult, T)) {
				FluidStack tFluid = FL.getFluid(aResult, T);
				if (tFluid != null) {
					tResults.add(FL.display(tFluid, F, F));
					for (FluidContainerData tData : FluidContainerRegistry.getRegisteredFluidContainerData()) {
						if (FL.equal(tData.fluid, tFluid)) tResults.add(ST.copy(tData.filledContainer));
					}
				}
			}
			
			ArrayList<Recipe> tRecipes = new ArrayListNoNulls<>();
			for (Recipe tRecipe : mRecipeMap.getNEIRecipes(tResults.toArray(ZL_IS))) if (!tRecipes.contains(tRecipe)) tRecipes.add(tRecipe);
			for (Recipe tRecipe : tRecipes) arecipes.add(new CachedDefaultRecipe(tRecipe));
			sortRecipes();
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}
	
	// @Override
	public void loadUsageRecipes(ItemStack aInput) {
		if (!CODE_CLIENT) return;
		if (ST.invalid(aInput)) return;
		try {
			OreDictItemData tPrefixMaterial = OM.association_(aInput);
			
			ArrayList<ItemStack> tInputs = ST.arraylist(aInput, OreDictManager.INSTANCE.getStack_(F, aInput));
			
			ArrayList<ItemStack>
			tRedirects = ItemsGT.sNEIRedirects.get(new ItemStackContainer(aInput));
			if (tRedirects != null) tInputs.addAll(tRedirects);
			tRedirects = ItemsGT.sNEIRedirects.get(new ItemStackContainer(aInput, W));
			if (tRedirects != null) tInputs.addAll(tRedirects);
			
			if (tPrefixMaterial != null) {
				for (OreDictPrefix tPrefix : tPrefixMaterial.mPrefix.mFamiliarPrefixes) {
					tInputs.add(tPrefix.mat(tPrefixMaterial.mMaterial.mMaterial, 1));
				}
			}
			
			if (!ItemsGT.NEI_DONT_SHOW_FLUIDS.contains(aInput, T)) {
				FluidStack tFluid = FL.getFluid(aInput, T);
				if (tFluid != null) {
					tInputs.add(FL.display(tFluid, F, F));
					for (FluidContainerData tData : FluidContainerRegistry.getRegisteredFluidContainerData()) {
						if (FL.equal(tData.fluid, tFluid)) tInputs.add(ST.copy(tData.filledContainer));
					}
				}
			}
			
			ArrayList<Recipe> tRecipes = new ArrayListNoNulls<>();
			for (Recipe tRecipe : mRecipeMap.getNEIUsages(tInputs.toArray(ZL_IS))) if (!tRecipes.contains(tRecipe)) tRecipes.add(tRecipe);
			for (Recipe tRecipe : tRecipes) arecipes.add(new CachedDefaultRecipe(tRecipe));
			sortRecipes();
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
	}
	
	// @Override
	public String getOverlayIdentifier() {
		return mRecipeMap.mNameNEI;
	}
	
	public String getHandlerId() {
		return mRecipeMap.mNameNEI;
	}
	
	// @Override
	public void drawBackground(int recipe) {
		GL11.glColor4f(1, 1, 1, 1);
		GuiDraw.changeTexture(RES_PATH_GUI+"machines/NEI.png");
		GuiDraw.drawTexturedModalRect(-5, -16, 0, 0, 176, 166);
		GuiDraw.changeTexture(getGuiTexture());
		GuiDraw.drawTexturedModalRect(-5, -8, 0, 3, 176,  79);
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code Minecraft.getInstance().fontRenderer.drawString(...)}
	 *  (`getMinecraft()`→{@code getInstance()}, поле `fontRenderer`→{@code font}, метод {@code drawString}
	 *  удалён у {@code Font} — текст экрана рисуется через {@code GuiGraphicsExtractor}, см. javadoc
	 *  {@link gregapi.gui.ContainerClient} class); тот же паттерн, что {@code GuiDraw} F10-зеркало выше (no-op). */
	public static void drawText(int aX, int aY, String aString, int aColor) {
		//
	}
	
	// @Override
	public int recipiesPerPage() {
		return 1;
	}
	
	// @Override
	public String getRecipeName() {
		return LanguageHandler.translate(mRecipeMap.mNameInternal, mRecipeMap.mNameInternal);
	}
	
	// @Override
	public String getGuiTexture() {
		return UT.Code.stringValid(mRecipeMap.mGUIPath) ? mRecipeMap.mGUIPath : RES_PATH_GUI + mRecipeMap.mNameInternal + ".png";
	}
	
	// @Override
	public List<String> handleItemTooltip(GuiRecipe gui, ItemStack aStack, List<String> currenttip, int aRecipeIndex) {
		if (!CODE_CLIENT) return currenttip;
		CachedRecipe tObject = arecipes.get(aRecipeIndex);
		if (tObject instanceof CachedDefaultRecipe) {
			CachedDefaultRecipe tRecipe = (CachedDefaultRecipe)tObject;
			for (PositionedStack tStack : tRecipe.mOutputs) if (aStack == tStack.item) {
				if (tStack instanceof FixedPositionedStack && ((FixedPositionedStack)tStack).mChance > 0 && ((FixedPositionedStack)tStack).mChance != ((FixedPositionedStack)tStack).mMaxChance) {
					long tChance = UT.Code.units(((FixedPositionedStack)tStack).mChance, ((FixedPositionedStack)tStack).mMaxChance, 10000, F);
					currenttip.add(1, LH.Chat.BLINKING_CYAN + "Chance: " + (tChance / 100) + "." + ((tChance % 100)<10?"0"+(tChance % 100):(tChance % 100)) + "%" + (tStack.item.getCount()>1?" each":""));
				}
				break;
			}
			for (PositionedStack tStack : tRecipe.mInputs) if (aStack == tStack.item) {
				if (!gregapi.data.IL.Display_Fluid.equal(tStack.item, T, T)) {
					if (tStack.item.getCount() == 0) currenttip.add(1, LH.Chat.BLINKING_CYAN + "Does not get consumed in the process");
				}
				break;
			}
		}
		return currenttip;
	}

	// @Override
	public void drawExtras(int aRecipeIndex) {
		long tGUt       = ((CachedDefaultRecipe)arecipes.get(aRecipeIndex)).mRecipe.mEUt;
		long tDuration  = ((CachedDefaultRecipe)arecipes.get(aRecipeIndex)).mRecipe.mDuration;
		if (tGUt == 0) {
			if (mRecipeMap.mShowVoltageAmperageInNEI) {
				drawText(10, 93, "Tier: unspecified", 0xFF000000);
			}
		} else {
			if (tGUt > 0) {
				drawText    (10, 73, "Costs: "  + UT.Code.makeString(tGUt * tDuration        ) + " GU"  , 0xFF000000);
				if (mRecipeMap.mShowVoltageAmperageInNEI) {
					if (!mRecipeMap.mCombinePower)
					drawText(10, 83, "Usage: "  + UT.Code.makeString(tGUt                    ) + " GU/t", 0xFF000000);
					drawText(10, 93, "Tier: "   + UT.Code.makeString(tGUt / mRecipeMap.mPower) + " GU"  , 0xFF000000);
					drawText(10,103, "Power: "  + UT.Code.makeString(       mRecipeMap.mPower)          , 0xFF000000);
				} else {
					if (tGUt != 1 && !mRecipeMap.mCombinePower)
					drawText(10, 83, "Usage: "  + UT.Code.makeString(tGUt                    ) + " GU/t", 0xFF000000);
				}
			} else {
				tGUt *= -1;
				drawText    (10, 73, "Gain: "   + UT.Code.makeString(tGUt * tDuration        ) + " GU"  , 0xFF000000);
				if (mRecipeMap.mShowVoltageAmperageInNEI) {
					if (!mRecipeMap.mCombinePower)
					drawText(10, 83, "Output: " + UT.Code.makeString(tGUt                    ) + " GU/t", 0xFF000000);
					drawText(10, 93, "Tier: "   + UT.Code.makeString(tGUt / mRecipeMap.mPower) + " GU"  , 0xFF000000);
					drawText(10,103, "Power: "  + UT.Code.makeString(       mRecipeMap.mPower)          , 0xFF000000);
				} else {
					if (tGUt != 1 && !mRecipeMap.mCombinePower)
					drawText(10, 83, "Output: " + UT.Code.makeString(tGUt                    ) + " GU/t", 0xFF000000);
				}
			}
		}
		if (tDuration > 0)
		drawText(10,113, "Time: " + (tDuration < 1200 ? UT.Code.makeString(tDuration) + " ticks" : tDuration < 36000 ? UT.Code.makeString(tDuration/20) + " secs" : UT.Code.makeString(tDuration/1200) + " mins"), 0xFF000000);
		if (UT.Code.stringValid(mRecipeMap.mNEISpecialValuePre) || UT.Code.stringValid(mRecipeMap.mNEISpecialValuePost))
		drawText(10,123, mRecipeMap.mNEISpecialValuePre + UT.Code.makeString(((CachedDefaultRecipe)arecipes.get(aRecipeIndex)).mRecipe.mSpecialValue * mRecipeMap.mNEISpecialValueMultiplier) + mRecipeMap.mNEISpecialValuePost, 0xFF000000);
	}
}
