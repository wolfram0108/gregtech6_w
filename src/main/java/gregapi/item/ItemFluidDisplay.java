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

package gregapi.item;

import net.neoforged.api.distmarker.Dist;
import gregapi.GT_API;
import gregapi.api.Abstract_Mod;
import gregapi.code.ItemNBT;
import gregapi.config.ConfigCategories;
import gregapi.data.FL;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.data.OP;
import gregapi.fluid.FluidGT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.recipes.Recipe;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import java.util.Collection;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class ItemFluidDisplay extends Item implements IFluidContainerItem, IItemUpdatable, IItemGT {
	// F3-render: was IIcon (a removed 1.7.10 class) — the field is dead (never read anywhere); the type was changed to neo Identifier,
	// to remove the reference to the removed class (otherwise enumerating the class's methods in GT6ItemModel.resolveIcon → NoClassDefFoundError).
	protected net.minecraft.resources.Identifier mIcon;
	private final String mName;
	
	public ItemFluidDisplay() {
		// F1/F16: neo Item.<init> requires an ID in Properties (descriptionId) — set from (GAPI, "gt.display.fluid"), matching the DeferredRegister name.
		super(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MD.GAPI.mID, "gt.display.fluid"))));
		mName = "gt.display.fluid";
		LH.add(mName, "Fluid Display");
		// F12-lazy: SELF-registration was removed from the constructor — the item registers via a DeferredRegister supplier at the
		// call site (GT_API.onModPreInit2: IL.Display_Fluid.set(GT_API.ITEMS.register(name, ItemFluidDisplay::new))), because
		// the construction must happen on RegisterEvent (an intrusive holder needs an open registry), not in preInit. Otherwise double registration.
		if (ConfigsGT.CLIENT.get(ConfigCategories.visibility, "HiddenGTFluidDisplay", F)) gregapi.GT_API.deferItemInit(() -> ST.hide(this));
		// BUG-030: in 1.7.10 the item had NO tab, but the NEI panel enumerated it via getSubItems independent of tabs
		// (fluids were found by search). In neo the creative-search channel AND the JEI ingredient panel = tab contents —
		// without membership, enumeration is unreachable from anywhere. Minimal bridge: the Ingredients tab (mapping to tabMisc,
		// like other GT6 ingredients); enumeration — via the restored getSubItems below; the HiddenGTFluidDisplay config
		// (ST.hide → the ST.hidden filter in CreativeTabsGT) still hides it entirely.
		gregapi.item.CreativeTabsGT.assign(this, gregapi.item.CreativeTabsGT.MISC);
		ItemsGT.DEBUG_ITEMS.add(this);
		ItemsGT.ILLEGAL_DROPS.add(this);
		GarbageGT.BLACKLIST.add(this);
	}
	
	// @Override
	public boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float hitX, float hitY, float hitZ) {
		if (!aWorld.isClientSide() && UT.Entities.hasInfiniteItems(aPlayer)) for (byte tSide : ALL_SIDES_VALID) if (FL.fill(WD.te(aWorld, aX, aY, aZ, tSide, T), FL.make(FL.fluid(ST.meta_(aStack)), Integer.MAX_VALUE), T) > 0) return T;
		return !aWorld.isClientSide();
	}
	// F-useOn bridge: neo calls onItemUseFirst(ItemStack,UseOnContext), not the 1.7.10 onItemUseFirst(x,y,z,side,hit) —
	// unpack+delegate to the body above (the IItemGT center). Only this arm: the 1.7.10 original (ItemFluidDisplay.java:76-80)
	// overrode ONLY onItemUseFirst (filling tanks with creative), there was no regular onItemUse (block placement) —
	// this item is not a BlockItem, it does not need useOn.
	@Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack aStack, net.minecraft.world.item.context.UseOnContext aCtx) {return IItemGT.bridgeUseOnFirst(this, aCtx);}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		Fluid aFluid = FL.fluid(ST.meta_(aStack));
		if (aFluid == null) {
			aList.add(LH.Chat.BLINKING_RED + LH.tt("CLIENTSIDE FLUID IS NULL!!!"));
		} else if (FL.Error.is(aFluid)) {
			aList.add(LH.Chat.BLINKING_RED + LH.tt("THIS IS AN ERROR AND SHOULD NEVER BE OBTAINABLE!!!"));
		} else {
			String aName = FL.regName(aFluid);
			
			if (SHOW_INTERNAL_NAMES || aF3_H) aList.add(LH.tt("Registry: ") + aName);
			if (FL.exists(FluidsGT.FLUID_RENAMINGS.get(aName)) || FluidsGT.NONSTANDARD.contains(aName)) aList.add(LH.Chat.BLINKING_RED + LH.tt("NON-STANDARD FLUID!"));
			
			long tAmount = 0, tTemperature = DEF_ENV_TEMP;
			FluidStack tFluid = NF;
			boolean tGas = F;
			
			if (aNBT == null) {
				tAmount = 0;
				tFluid = FL.make(aFluid, (int)tAmount);
				tGas = FL.gas(tFluid);
				tTemperature = FL.temperature(tFluid);
			} else {
				tAmount = aNBT.getLong("a").orElse(0L);
				tFluid = FL.make(aFluid, (int)tAmount);
				tGas = aNBT.getBoolean("s").orElse(false);
				tTemperature = aNBT.getLong("h").orElse(0L);
			}
			
			if (tAmount > 0) {
				aList.add(LH.Chat.BLUE + LH.tt("Amount: ") + UT.Code.makeString(tAmount) + " L");
			}
			OreDictMaterialStack tMaterial = OreDictMaterial.FLUID_MAP.get(aName);
			if (tMaterial != null) {
				if (tMaterial.mAmount > 0 && tAmount > 0) {
					long tMatAmount = UT.Code.units(tAmount, tMaterial.mAmount, U, F);
					if (tMatAmount > 0) {
						int tDigits = (int)(((tMatAmount % U) / UD) * 1000);
						aList.add(LH.Chat.BLUE + LH.tt("Worth: ") + (tMatAmount / U) + "." + (tDigits<1?"000":tDigits<10?"00"+tDigits:tDigits<100?"0"+tDigits:tDigits) + LH.tt(" Units of ") + tMaterial.mMaterial.getLocal());
					}
				}
				if (UT.Code.stringValid(tMaterial.mMaterial.mTooltipChemical)) aList.add(LH.Chat.YELLOW + tMaterial.mMaterial.mTooltipChemical);
			}
			
			aList.add(LH.Chat.RED + LH.tt("Temperature: ") + tTemperature + " K (" + (tTemperature-C) + "°C)");
			
			// F5: 1.7.10 Fluid.isGaseous(FluidStack) was removed -> FluidType.isLighterThanAir() (no FluidStack arg,
			// a property of the fluid type, not the stack; neoforge-decompiled/.../fluids/FluidType.java:807, the same
			// aFluid.getFluidType() approach already used below in this file for getDensity/getLightLevel/getViscosity).
			if (FL.plasma(tFluid)) {
				aList.add(LH.Chat.GREEN + LH.tt("State: ") + LH.Chat.YELLOW + LH.tt("Plasma") + (!aFluid.getFluidType().isLighterThanAir() ? LH.Chat.RED + LH.tt(" (Warning: Considered a Liquid by Mods other than GT!)") : LH.Chat.ORANGE + LH.tt(" (Note: Considered a Gas by Mods other than GT!)")));
			} else if (tGas) {
				aList.add(LH.Chat.GREEN + LH.tt("State: ") + LH.Chat.CYAN + LH.tt("Gas") + (!aFluid.getFluidType().isLighterThanAir() ? LH.Chat.RED + LH.tt(" (Warning: Considered a Liquid by Mods other than GT!)") : ""));
			} else {
				aList.add(LH.Chat.GREEN + LH.tt("State: ") + LH.Chat.BLUE + LH.tt("Liquid") + (tMaterial != null && ST.valid(OP.ingot.mat(tMaterial.mMaterial, 1)) ? LH.Chat.CYAN + LH.tt(" (Might able to cast into Molds)") : ""));
				if (aFluid.getFluidType().isLighterThanAir()) aList.add(LH.Chat.BLINKING_RED + LH.tt(" (Warning: Considered a Gas by Mods other than GT!)"));
			}
			
			int tDensity = aFluid.getFluidType().getDensity(tFluid);
			if (tDensity > 0) {
				aList.add(LH.Chat.GREEN + LH.tt("Density: ") + tDensity + LH.tt(" ; Heavier than Air (typically moves down)"));
			} else if (tDensity < 0) {
				aList.add(LH.Chat.GREEN + LH.tt("Density: ") + tDensity + LH.tt(" ; Lighter than Air (typically moves up)"));
			} else {
				aList.add(LH.Chat.GREEN + LH.tt("Density: 0 ; As dense as Air (typically still moves down)"));
			}
			
			int tLuminosity = aFluid.getFluidType().getLightLevel(tFluid);
			if (tLuminosity != 0) aList.add(LH.Chat.YELLOW + LH.tt("Luminosity: ") + tLuminosity);
			
			int tViscosity = aFluid.getFluidType().getViscosity(tFluid);
			if (tViscosity != 0) aList.add(LH.Chat.BLUE + LH.tt("Viscosity: ") + tViscosity);
			
			if (FluidsGT.COOKING_OIL.contains(aName)) {
				aList.add(LH.Chat.DGREEN + LH.tt("Usable as Cooking Oil in a GT Oven to duplicate Meat and Fish"));
			}
			if (FL.simple(aFluid)) {
				aList.add(LH.Chat.DGREEN + LH.tt("This is a simple Fluid that is easy to handle"));
			}
			if (FL.powerconducting(aFluid)) {
				aList.add(LH.Chat.DGREEN + LH.tt("This is a Power Conducting Fluid"));
				aList.add(LH.Chat.ORANGE + LH.tt("Cannot be stored in any normal GT6 Storage Tanks!"));
			}
			if (FL.acid(aFluid)) {
				aList.add(LH.Chat.ORANGE + LH.tt("Acidic! Handle with Care!"));
			}
			if (FL.magic(aFluid)) {
				aList.add(LH.Chat.ORANGE + LH.tt("Magical! Handle with Care!"));
			}
			if (FL.Lubricant.is(aFluid) || FL.LubRoCant.is(aFluid)) {
				aList.add(LH.Chat.ORANGE + LH.tt("Industrial Use ONLY!"));
				aList.add(LH.Chat.RED + LH.tt("Not Flammable!"));
			} else {
				for (Recipe.RecipeMap tMap : Recipe.RecipeMap.FUEL_MAP_LIST) {
					Collection<Recipe> tRecipes = tMap.mRecipeFluidMap.get(aName);
					if (tRecipes != null && !tRecipes.isEmpty()) {
						long tFuelValue = 0;
						for (Recipe tRecipe : tRecipes) if (tRecipe.mEnabled && tRecipe.mFluidInputs[0] != null) tFuelValue = Math.max(tFuelValue, (tRecipe.getAbsoluteTotalPower() * U) / tRecipe.mFluidInputs[0].getAmount());
						if (tFuelValue > 0) {
							if (tAmount > 1) {
								aList.add(LH.Chat.RED + LH.get(tMap.mNameInternal) + ": " + LH.Chat.WHITE + UT.Code.makeString(tFuelValue / U) + LH.Chat.YELLOW + LH.tt(" GU/L; ") + LH.Chat.WHITE + UT.Code.makeString((tFuelValue * tAmount) / U) + LH.Chat.YELLOW + LH.tt(" GU total"));
							} else {
								aList.add(LH.Chat.RED + LH.get(tMap.mNameInternal) + ": " + LH.Chat.WHITE + UT.Code.makeString(tFuelValue / U) + LH.Chat.YELLOW + LH.tt(" GU/L "));
							}
						}
					}
				}
			}
			
			if (FluidGT.of(aFluid) != null) {
				aList.add(LH.Chat.DGRAY + LH.tt("Fluid owned by GT6"));
			} else {
				if (FL.Water.is(aFluid) || FL.Lava.is(aFluid)) {
					aList.add(LH.Chat.DGRAY + LH.tt("Fluid owned by vanilla Minecraft"));
				} else {
					aList.add(LH.Chat.DGRAY + LH.tt("Fluid NOT owned by GT6"));
				}
			}
		}
		
		if (UT.Entities.hasInfiniteItems(aPlayer)) {
			aList.add(LH.Chat.RAINBOW_SLOW + LH.tt("Rightclick Blocks to fill their Tanks with this Fluid!"));
		}
		
		while (aList.remove(null));
	}
	
	// @Override
	// F3-render: was registerIcons(IIconRegister) (a removed class in the signature broke enumerating methods in resolveIcon →
	// NoClassDefFoundError) — the param was changed to Object. The "useful hack" of dispatching sBlockIconload (1.7.10: this item was
	// the DRIVER of the block-icon-load phase) is DEAD in neo — GT_API.sBlockIconload is nulled at init (GT_API.java:1048); block icons
	// are built LAZILY (BI.Icon / Textures.java:171). Removed: a lazy call would have iterated null → NPE.
	public void registerIcons(Object aIconRegister) {
		//
	}

	// Icon = the fluid's still texture (1:1 Fluid.getStillIcon): GT6 fluids — from the F5 center (FluidGT.mTexture),
	// vanilla/foreign — the neo canon IClientFluidTypeExtensions (a client-only class → isolated in a nested holder,
	// loaded lazily only under CODE_CLIENT — a dedicated server never touches it).
	// CHANNEL IS ALIVE — caller: the GT6ItemModel.iconForPass:229 center (reflection): the class has no getIcon(ItemStack,int),
	// so pass0 falls back to getIconIndex → getIconFromDamage. The fluid sprite is also alive via a second path —
	// stillIcon below is used by the client channel IClientFluidTypeExtensions (see the analysis above).
	// The dead-channel registry cannot see reflection via grep — the previous "analyzed" label was false (2026-07-30).
	// @Override
	public net.minecraft.resources.Identifier getIconFromDamage(int aMeta) {
		return stillIcon(FL.fluid(aMeta));
	}

	// getIconIndex(ItemStack) is checked by resolveIcon FIRST — read the meta via the native channel ST.meta_ (not damage).
	public net.minecraft.resources.Identifier getIconIndex(ItemStack aStack) {
		return stillIcon(FL.fluid(ST.meta_(aStack)));
	}

	private static net.minecraft.resources.Identifier stillIcon(net.minecraft.world.level.material.Fluid aFluid) {
		// BUG-049: the local copy was removed — the single still-icon resolver is now the FL.stillIcon center
		// (fluids without their own texture get water_still instead of null — the former vanilla branches are covered).
		return FL.stillIcon(aFluid);
	}

	// Tint (1:1 Fluid.getColor): GT6 fluids — mRGBa from the F5 center (FluidGT); vanilla water — NORMAL_WATER_COLOR
	// (OverworldBiomes.java:28, the gray water_still without tint would be colorless); lava carries color in its texture.
	// CHANNEL IS ALIVE — caller: the GT6ItemModel.itemColor:224 center (reflection by signature (ItemStack,int)) tints
	// the pass's quads with it. The registry cannot see reflection via grep — the previous "analyzed" label was false (2026-07-30).
	// @Override
	public int getColorFromItemStack(ItemStack aStack, int aRenderPass) {
		net.minecraft.world.level.material.Fluid tFluid = FL.fluid(ST.meta_(aStack));
		if (tFluid == null) return 16777215;
		gregapi.fluid.FluidGT tGT = gregapi.fluid.FluidGT.of(tFluid);
		if (tGT != null) return UT.Code.getRGBInt(tGT.getRGBa());
		if (tFluid.isSame(net.minecraft.world.level.material.Fluids.WATER)) return 4159204;
		return 16777215;
	}
	
	// @Override
	public int getSpriteNumber() {
		return 0;
	}
	
	// ⚠️ CHANNEL IS REDUNDANT — the item's name is built by getItemStackDisplayName:270 (which itself takes the fluid from the meta),
	// and the getName:280 bridge carries it to the engine. This 1.7.10 method does not take part in the chain; kept as a comparison
	// point with the original.
	// @Override
	public String getUnlocalizedName(ItemStack aStack) {
		if (aStack != null) return FL.name(FL.fluid(ST.meta_(aStack)), F);
		return "";
	}
	
	// @Override
	public String getItemStackDisplayName(ItemStack aStack) {
		if (aStack == null) return "";
		Fluid tFluid = FL.fluid(ST.meta_(aStack));
		return tFluid == null ? "INVALID FLUID ID!!!" : FL.name(tFluid, T);
	}

	// F1 contract (1.7.10 itemDamage==meta): meta = fluid ID; the neo default getDamage reads the DAMAGE component (0). Same as on other roots.
	@Override public int getDamage(ItemStack aStack) {return getMaxDamage(aStack) > 0 ? super.getDamage(aStack) : ST.meta_(aStack);}

	// LOCALIZATION-display: bridge getName → the GT6 name (like ItemBase:145); without it the fluid display shows a raw key.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}

	// F13 bridge appendHoverText → addInformation (like ItemBlockBase:65): fluid amount/temperature in the tooltip.
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, net.minecraft.world.item.Item.TooltipContext aCtx, net.minecraft.world.item.component.TooltipDisplay aDisplay, java.util.function.Consumer<net.minecraft.network.chat.Component> aBuilder, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aBuilder.accept(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}
	
	// @Override
	public boolean hasEffect(ItemStack aStack, int aRenderPass) {
		Fluid aFluid = FL.fluid(ST.meta_(aStack));
		return aFluid != null && FluidsGT.ENCHANTED_EFFECT.contains(FL.regName(aFluid));
	}

	// Wiring the "enchantment glint" channel to the engine (2026-07-30, dead-channel registry). In 1.7.10 it was
	// asked per render pass (RenderHelper.java:89 — aStack.hasEffect(i)), in neo there are no passes and
	// the question is asked once: Item.isFoil(ItemStack). Approach taken from sibling MultiItemTool:795. Body 1:1 with
	// the original (ItemFluidDisplay.java:270-273) — pass 0 did not change the value in the 1.7.10 version.
	// Without the bridge, fluid displays from FluidsGT.ENCHANTED_EFFECT would not glint at all.
	@Override public boolean isFoil(ItemStack aStack) {return hasEffect(aStack, 0);}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void getSubItems(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {
		// BUG-030: the 1.7.10 fluid-enumeration loop is restored (original :278-287). Back then: a dense pass
		// FluidRegistry.getMaxID() + FL.fluid(i); in neo the registry is not dense → a live pass over BuiltInRegistries.FLUID
		// (the same registry channel already used by FL.id/FL.fluid; FL.display has long been real, no stub).
		// Modern quirk: a flowing fluid has TWO registry objects (source+flowing), 1.7.10 knew ONE Fluid
		// per fluid → we enumerate only the source variant (otherwise every display would be doubled). The hidden-set gate is the same
		// FluidsGT.HIDDEN keyed by the 1.7.10 name (FL.regName = FluidGT.nameOf, an analog of tFluid.getName()).
		for (Fluid tFluid : net.minecraft.core.registries.BuiltInRegistries.FLUID) {
			if (tFluid == net.minecraft.world.level.material.Fluids.EMPTY || !tFluid.defaultFluidState().isSource()) continue;
			if (FluidsGT.HIDDEN.contains(FL.regName(tFluid))) continue;
			ItemStack tStack = FL.display(tFluid);
			if (tStack != null) aList.add(tStack);
		}
		for (String tName : UT.Books.BOOK_LIST) aList.add(ST.book(tName));
	}
	
	public final Item setUnlocalizedName(String aName) {return this;}
	public final String getUnlocalizedName() {return mName;}
	
	// F12 hook (lost receiver): the neo channel is IItemExtension.doesSneakBypassUse(ItemStack,LevelReader,
	// BlockPos,Player) (IItemExtension.java:251). The 1.7.10 signature below overrode nothing and was never
	// called by the engine → crouching with a display item in hand did NOT let a click through to the block (chest/machine did not open
	// while a display was held). Body 1:1 — always T.
	@Override
	public boolean doesSneakBypassUse(ItemStack aStack, net.minecraft.world.level.LevelReader aWorld, net.minecraft.core.BlockPos aPos, Player aPlayer) {
		return T;
	}

	// ⚠️ CHANNEL IS REDUNDANT — the 1.7.10 signature, its role is covered by the neo version above (line 322, IItemExtension:251).
	// Kept as a comparison point with the original; has no callers and should not have any.
	// @Override
	public boolean doesSneakBypassUse(Level aWorld, int aX, int aY, int aZ, Player aPlayer) {
		return T;
	}
	
	// @Override
	public ItemStack getContainerItem(ItemStack aStack) {
		return null;
	}
	
	// ⚖️ REDUNDANT (dead-channel registry, verdict 2026-08-19). No caller: in 1.7.10 this was a hook of
	// the Item itself, the engine asked it before getContainerItem; in neo Item has no such method at all,
	// and the GT6 center (ST.gtContainerItem) asks getContainerItem DIRECTLY — the trait is derived from it
	// (ItemBase:199 and the original ItemBase:121 define hasContainerItem EXACTLY as getContainerItem != null).
	// Both methods here agree 1:1 with the original (ItemFluidDisplay:298-305: null and F), so the channel
	// is not broken, just unneeded. NOT removed — the author's contract is reproduced as-is.
	// @Override
	public final boolean hasContainerItem(ItemStack aStack) {
		return F;
	}
	
	@Override
	public void updateItemStack(ItemStack aStack) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null && aNBT.contains("f")) {
			String aName = aNBT.getString("f").orElse("");
			if (UT.Code.stringInvalid(aName)) return;
			String tName = FluidsGT.FLUID_RENAMINGS.get(aName);
			if (UT.Code.stringValid(tName)) aName = tName;
			Fluid tFluid = FL.fluid_(aName);
			// F5: 1.7.10 Fluid.getID() was removed -> FL.id(Fluid) (FL.java:673, the same center helper already
			// used across the whole tree; about registry-id instability across runs — FL.java:736, inherited 1:1).
			if (tFluid != null) ST.meta_(aStack, FL.id(tFluid));
			return;
		}
		Fluid tFluid = FL.fluid(ST.meta_(aStack));
		if (tFluid == null) ST.meta_(aStack, W); else {ItemNBT.set(aStack, UT.NBT.makeString("f", FL.regName(tFluid)));}
	}
	@Override
	public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {
		updateItemStack(aStack);
	}
	
	@Override
	public FluidStack getFluid(ItemStack aStack) {
		Fluid tFluid = FL.fluid(ST.meta_(aStack));
		if (tFluid == null) return null;
		FluidStack rFluid = null;
		CompoundTag aNBT = ItemNBT.get(aStack);
		if (aNBT != null) {
			long tAmount = aNBT.getLong("a").orElse(0L);
			if (tAmount > 0) rFluid = FL.make(tFluid, tAmount);
		}
		return rFluid == null ? FL.make(tFluid, 0) : rFluid;
	}

	@Override
	public int getCapacity(ItemStack aStack) {
		return Integer.MAX_VALUE;
	}

	@Override
	public int fill(ItemStack aStack, FluidStack aFluid, boolean aDoFill) {
		return 0;
	}

	@Override
	public FluidStack drain(ItemStack aStack, int aDrain, boolean aDoDrain) {
		return getFluid(aStack);
	}
}
