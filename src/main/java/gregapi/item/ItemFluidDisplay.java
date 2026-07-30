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
	// F3-render: было IIcon (удалённый 1.7.10-класс) — поле мёртвое (нигде не читается); тип сменён на neo Identifier,
	// чтобы убрать ссылку на removed-класс (иначе перечисление методов класса в GT6ItemModel.resolveIcon → NoClassDefFoundError).
	protected net.minecraft.resources.Identifier mIcon;
	private final String mName;
	
	public ItemFluidDisplay() {
		// F1/F16: neo Item.<init> требует ID в Properties (descriptionId) — задаём из (GAPI, "gt.display.fluid"), совпадает с DeferredRegister-именем.
		super(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MD.GAPI.mID, "gt.display.fluid"))));
		mName = "gt.display.fluid";
		LH.add(mName, "Fluid Display");
		// F12-lazy: САМО-регистрация убрана из конструктора — предмет регистрируется через DeferredRegister-supplier на
		// call-site (GT_API.onModPreInit2: IL.Display_Fluid.set(GT_API.ITEMS.register(name, ItemFluidDisplay::new))), т.к.
		// конструкция должна идти на RegisterEvent (intrusive-holder нужен открытый реестр), не в preInit. Иначе двойная регистрация.
		if (ConfigsGT.CLIENT.get(ConfigCategories.visibility, "HiddenGTFluidDisplay", F)) gregapi.GT_API.deferItemInit(() -> ST.hide(this));
		// BUG-030: в 1.7.10 предмет был БЕЗ вкладки, но NEI-панель перечисляла его getSubItems независимо от вкладок
		// (жидкости находились поиском). В neo канал креатив-поиска И JEI-панели ингредиентов = содержимое вкладок —
		// без членства перечисление недостижимо ниоткуда. Минимальный мост: вкладка Ingredients (маппинг tabMisc,
		// как прочие GT6-ингредиенты); перечисление — восстановленным getSubItems ниже; конфиг HiddenGTFluidDisplay
		// (ST.hide → фильтр ST.hidden в CreativeTabsGT) продолжает скрывать целиком.
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
	
	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		CompoundTag aNBT = ItemNBT.get(aStack);
		Fluid aFluid = FL.fluid(ST.meta_(aStack));
		if (aFluid == null) {
			aList.add(LH.Chat.BLINKING_RED + "CLIENTSIDE FLUID IS NULL!!!");
		} else if (FL.Error.is(aFluid)) {
			aList.add(LH.Chat.BLINKING_RED + "THIS IS AN ERROR AND SHOULD NEVER BE OBTAINABLE!!!");
		} else {
			String aName = FL.regName(aFluid);
			
			if (SHOW_INTERNAL_NAMES || aF3_H) aList.add("Registry: " + aName);
			if (FL.exists(FluidsGT.FLUID_RENAMINGS.get(aName)) || FluidsGT.NONSTANDARD.contains(aName)) aList.add(LH.Chat.BLINKING_RED + "NON-STANDARD FLUID!");
			
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
				aList.add(LH.Chat.BLUE + "Amount: " + UT.Code.makeString(tAmount) + " L");
			}
			OreDictMaterialStack tMaterial = OreDictMaterial.FLUID_MAP.get(aName);
			if (tMaterial != null) {
				if (tMaterial.mAmount > 0 && tAmount > 0) {
					long tMatAmount = UT.Code.units(tAmount, tMaterial.mAmount, U, F);
					if (tMatAmount > 0) {
						int tDigits = (int)(((tMatAmount % U) / UD) * 1000);
						aList.add(LH.Chat.BLUE + "Worth: " + (tMatAmount / U) + "." + (tDigits<1?"000":tDigits<10?"00"+tDigits:tDigits<100?"0"+tDigits:tDigits) + " Units of " + tMaterial.mMaterial.getLocal());
					}
				}
				if (UT.Code.stringValid(tMaterial.mMaterial.mTooltipChemical)) aList.add(LH.Chat.YELLOW + tMaterial.mMaterial.mTooltipChemical);
			}
			
			aList.add(LH.Chat.RED + "Temperature: " + tTemperature + " K (" + (tTemperature-C) + "°C)");
			
			// F5: 1.7.10 Fluid.isGaseous(FluidStack) удалён -> FluidType.isLighterThanAir() (без FluidStack-арга,
			// свойство типа жидкости, не стека; neoforge-decompiled/.../fluids/FluidType.java:807, тот же
			// aFluid.getFluidType() приём, что уже используется ниже в этом файле для getDensity/getLightLevel/getViscosity).
			if (FL.plasma(tFluid)) {
				aList.add(LH.Chat.GREEN + "State: " + LH.Chat.YELLOW + "Plasma" + (!aFluid.getFluidType().isLighterThanAir() ? LH.Chat.RED + " (Warning: Considered a Liquid by Mods other than GT!)" : LH.Chat.ORANGE + " (Note: Considered a Gas by Mods other than GT!)"));
			} else if (tGas) {
				aList.add(LH.Chat.GREEN + "State: " + LH.Chat.CYAN + "Gas" + (!aFluid.getFluidType().isLighterThanAir() ? LH.Chat.RED + " (Warning: Considered a Liquid by Mods other than GT!)" : ""));
			} else {
				aList.add(LH.Chat.GREEN + "State: " + LH.Chat.BLUE + "Liquid" + (tMaterial != null && ST.valid(OP.ingot.mat(tMaterial.mMaterial, 1)) ? LH.Chat.CYAN + " (Might able to cast into Molds)" : ""));
				if (aFluid.getFluidType().isLighterThanAir()) aList.add(LH.Chat.BLINKING_RED + " (Warning: Considered a Gas by Mods other than GT!)");
			}
			
			int tDensity = aFluid.getFluidType().getDensity(tFluid);
			if (tDensity > 0) {
				aList.add(LH.Chat.GREEN + "Density: " + tDensity + " ; Heavier than Air (typically moves down)");
			} else if (tDensity < 0) {
				aList.add(LH.Chat.GREEN + "Density: " + tDensity + " ; Lighter than Air (typically moves up)");
			} else {
				aList.add(LH.Chat.GREEN + "Density: 0 ; As dense as Air (typically still moves down)");
			}
			
			int tLuminosity = aFluid.getFluidType().getLightLevel(tFluid);
			if (tLuminosity != 0) aList.add(LH.Chat.YELLOW + "Luminosity: " + tLuminosity);
			
			int tViscosity = aFluid.getFluidType().getViscosity(tFluid);
			if (tViscosity != 0) aList.add(LH.Chat.BLUE + "Viscosity: " + tViscosity);
			
			if (FluidsGT.COOKING_OIL.contains(aName)) {
				aList.add(LH.Chat.DGREEN + "Usable as Cooking Oil in a GT Oven to duplicate Meat and Fish");
			}
			if (FL.simple(aFluid)) {
				aList.add(LH.Chat.DGREEN + "This is a simple Fluid that is easy to handle");
			}
			if (FL.powerconducting(aFluid)) {
				aList.add(LH.Chat.DGREEN + "This is a Power Conducting Fluid");
				aList.add(LH.Chat.ORANGE + "Cannot be stored in any normal GT6 Storage Tanks!");
			}
			if (FL.acid(aFluid)) {
				aList.add(LH.Chat.ORANGE + "Acidic! Handle with Care!");
			}
			if (FL.magic(aFluid)) {
				aList.add(LH.Chat.ORANGE + "Magical! Handle with Care!");
			}
			if (FL.Lubricant.is(aFluid) || FL.LubRoCant.is(aFluid)) {
				aList.add(LH.Chat.ORANGE + "Industrial Use ONLY!");
				aList.add(LH.Chat.RED + "Not Flammable!");
			} else {
				for (Recipe.RecipeMap tMap : Recipe.RecipeMap.FUEL_MAP_LIST) {
					Collection<Recipe> tRecipes = tMap.mRecipeFluidMap.get(aName);
					if (tRecipes != null && !tRecipes.isEmpty()) {
						long tFuelValue = 0;
						for (Recipe tRecipe : tRecipes) if (tRecipe.mEnabled && tRecipe.mFluidInputs[0] != null) tFuelValue = Math.max(tFuelValue, (tRecipe.getAbsoluteTotalPower() * U) / tRecipe.mFluidInputs[0].getAmount());
						if (tFuelValue > 0) {
							if (tAmount > 1) {
								aList.add(LH.Chat.RED + LH.get(tMap.mNameInternal) + ": " + LH.Chat.WHITE + UT.Code.makeString(tFuelValue / U) + LH.Chat.YELLOW + " GU/L; " + LH.Chat.WHITE + UT.Code.makeString((tFuelValue * tAmount) / U) + LH.Chat.YELLOW + " GU total");
							} else {
								aList.add(LH.Chat.RED + LH.get(tMap.mNameInternal) + ": " + LH.Chat.WHITE + UT.Code.makeString(tFuelValue / U) + LH.Chat.YELLOW + " GU/L ");
							}
						}
					}
				}
			}
			
			if (FluidGT.of(aFluid) != null) {
				aList.add(LH.Chat.DGRAY + "Fluid owned by GT6");
			} else {
				if (FL.Water.is(aFluid) || FL.Lava.is(aFluid)) {
					aList.add(LH.Chat.DGRAY + "Fluid owned by vanilla Minecraft");
				} else {
					aList.add(LH.Chat.DGRAY + "Fluid NOT owned by GT6");
				}
			}
		}
		
		if (UT.Entities.hasInfiniteItems(aPlayer)) {
			aList.add(LH.Chat.RAINBOW_SLOW + "Rightclick Blocks to fill their Tanks with this Fluid!");
		}
		
		while (aList.remove(null));
	}
	
	// @Override
	// F3-render: было registerIcons(IIconRegister) (removed-класс в сигнатуре ломал перечисление методов в resolveIcon →
	// NoClassDefFoundError) — param сменён на Object. «Useful hack» диспетчеризации sBlockIconload (1.7.10: этот предмет был
	// ДРАЙВЕРОМ block-icon-load-фазы) МЁРТВ в neo — GT_API.sBlockIconload обнуляется на init (GT_API.java:1048); block-иконки
	// строятся ЛЕНИВО (BI.Icon / Textures.java:171). Убран: ленивый вызов итерировал бы null → NPE.
	public void registerIcons(Object aIconRegister) {
		//
	}

	// Иконка = still-текстура жидкости (1:1 Fluid.getStillIcon): GT6-жидкости — из центра F5 (FluidGT.mTexture),
	// ванильные/чужие — neo-канон IClientFluidTypeExtensions (клиент-класс → изолирован во вложенном холдере,
	// грузится лениво только под CODE_CLIENT — dedicated-сервер его не трогает).
	// ⚠️ КАНАЛ РАЗОБРАН — тот же класс, что getIconFromDamageForRenderPass у MultiItemRandom:430: в neo вид
	// предмета задаёт МОДЕЛЬ (data-driven), метода «дай иконку по подтипу» нет. Решается вместе с F3-рендером
	// предметов, делегатом не закрывается. Спрайт жидкости сам по себе жив — stillIcon ниже используется
	// клиентским каналом IClientFluidTypeExtensions (см. разбор выше).
	// @Override
	public net.minecraft.resources.Identifier getIconFromDamage(int aMeta) {
		return stillIcon(FL.fluid(aMeta));
	}

	// getIconIndex(ItemStack) проверяется resolveIcon ПЕРВЫМ — читаем мету родным каналом ST.meta_ (не damage).
	public net.minecraft.resources.Identifier getIconIndex(ItemStack aStack) {
		return stillIcon(FL.fluid(ST.meta_(aStack)));
	}

	private static net.minecraft.resources.Identifier stillIcon(net.minecraft.world.level.material.Fluid aFluid) {
		// BUG-049: локальная копия снята — единый резолвер still-иконки теперь в центре FL.stillIcon
		// (жидкости без своей текстуры получают water_still вместо null — прежние ваниль-ветки покрыты).
		return FL.stillIcon(aFluid);
	}

	// Тинт (1:1 Fluid.getColor): GT6-жидкости — mRGBa из центра F5 (FluidGT); ванильная вода — NORMAL_WATER_COLOR
	// (OverworldBiomes.java:28, серый water_still без тинта был бы бесцветным); лава несёт цвет в текстуре.
	// ⚠️ КАНАЛ РАЗОБРАН — цвет предмета в neo задаётся МОДЕЛЬЮ, не методом; разбор — PrefixBlockItem:131.
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
	
	// ⚠️ КАНАЛ ИЗБЫТОЧЕН — имя предмета строит getItemStackDisplayName:270 (сам берёт жидкость из меты),
	// а до движка его доводит мост getName:280. Этот 1.7.10-метод в цепочке не участвует; оставлен точкой
	// сверки с оригиналом.
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

	// F1-контракт (1.7.10 itemDamage==meta): meta = fluid-ID; neo-дефолт getDamage читает DAMAGE-компонент (0). Как на прочих корнях.
	@Override public int getDamage(ItemStack aStack) {return getMaxDamage(aStack) > 0 ? super.getDamage(aStack) : ST.meta_(aStack);}

	// LOCALIZATION-display: мост getName → GT6-имя (как ItemBase:145); без него дисплей жидкости — сырой ключ.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}

	// F13-мост appendHoverText → addInformation (как ItemBlockBase:65): количество/температура жидкости в тултипе.
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

	// Подключение канала «блеск зачарования» к движку (2026-07-30, реестр мёртвых каналов). В 1.7.10 его
	// спрашивали по проходам рендера (RenderHelper.java:89 — aStack.hasEffect(i)), в neo проходов нет и
	// вопрос задаётся один раз: Item.isFoil(ItemStack). Приём взят у брата MultiItemTool:795. Тело 1:1 с
	// оригиналом (ItemFluidDisplay.java:270-273) — проход 0 у 1.7.10-версии значения не менял.
	// Без моста дисплеи жидкостей из FluidsGT.ENCHANTED_EFFECT не блестели вовсе.
	@Override public boolean isFoil(ItemStack aStack) {return hasEffect(aStack, 0);}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void getSubItems(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {
		// BUG-030: восстановлен 1.7.10-цикл перечисления жидкостей (оригинал :278-287). Тогда: плотный проход
		// FluidRegistry.getMaxID() + FL.fluid(i); в neo реестр не плотный → живой проход по BuiltInRegistries.FLUID
		// (тот же registry-канал, что уже работает в FL.id/FL.fluid; FL.display давно реален, стаба нет).
		// Modern-особенность: у текучей жидкости ДВА реестровых объекта (source+flowing), 1.7.10 знал ОДИН Fluid
		// на жидкость → перечисляем только source-вариант (иначе каждый дисплей задвоится). Гейт скрытых — тот же
		// FluidsGT.HIDDEN по 1.7.10-имени (FL.regName = FluidGT.nameOf, аналог tFluid.getName()).
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
	
	// F12-hook (потерянный приёмник): neo-канал — IItemExtension.doesSneakBypassUse(ItemStack,LevelReader,
	// BlockPos,Player) (IItemExtension.java:251). 1.7.10-сигнатура ниже ничего не переопределяла и движком
	// не звалась → присед с дисплей-предметом в руке НЕ пропускал клик к блоку (сундук/машина не открывались,
	// пока в руке дисплей). Тело 1:1 — всегда T.
	@Override
	public boolean doesSneakBypassUse(ItemStack aStack, net.minecraft.world.level.LevelReader aWorld, net.minecraft.core.BlockPos aPos, Player aPlayer) {
		return T;
	}

	// ⚠️ КАНАЛ ИЗБЫТОЧЕН — 1.7.10-подпись, роль закрыта neo-версией выше (строка 322, IItemExtension:251).
	// Оставлена как точка сверки с оригиналом; вызывателей не имеет и иметь не должна.
	// @Override
	public boolean doesSneakBypassUse(Level aWorld, int aX, int aY, int aZ, Player aPlayer) {
		return T;
	}
	
	// @Override
	public ItemStack getContainerItem(ItemStack aStack) {
		return null;
	}
	
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
			// F5: 1.7.10 Fluid.getID() удалён -> FL.id(Fluid) (FL.java:673, тот же центр-хелпер уже
			// используется по всему дереву; про нестабильность registry-id между запусками — FL.java:736, 1:1 наследуется).
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
