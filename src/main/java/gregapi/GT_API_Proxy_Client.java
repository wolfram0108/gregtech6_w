/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregapi;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import gregapi.api.Abstract_Mod;
import gregapi.block.IBlockBase;
import gregapi.block.ToolCompat;
import gregapi.block.metatype.BlockMetaType;
import gregapi.block.multitileentity.MultiTileEntityBlockInternal;
import gregapi.block.prefixblock.PrefixBlockFallingEntity;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.ItemNBT;
import gregapi.code.ObjectStack;
import gregapi.cover.CoverRegistry;
import gregapi.cover.ICover;
import gregapi.data.*;
import gregapi.item.ItemFluidDisplay;
import gregapi.old.Textures;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.listeners.IOreDictListenerItem;
import gregapi.recipes.AdvancedCrafting1ToY;
import gregapi.recipes.AdvancedCraftingXToY;
import gregapi.render.*;
import gregapi.tileentity.render.ITileEntityOnDrawBlockHighlight;
import gregapi.util.OM;
import gregapi.util.ST;
import gregapi.util.UT;
import gregapi.util.WD;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.minecraft.network.chat.Component;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class GT_API_Proxy_Client extends GT_API_Proxy {
	
	public GT_API_Proxy_Client() {
		super();
		CODE_SERVER = T;
		CODE_CLIENT = T;
		CODE_UNCHECKED = F;
		
		for (int i = 0; i < 4; i++) {
			sPosR.addAll(Arrays.asList(MT.ChargedCertusQuartz.mRGBa[i], MT.Enderium.mRGBa[i], MT.Vinteum.mRGBa[i], MT.U_235.mRGBa[i], MT.Am_241.mRGBa[i], MT.Am_242.mRGBa[i], MT.Pu_241.mRGBa[i], MT.Pu_243.mRGBa[i], MT.Nq_528.mRGBa[i], MT.Nq_522.mRGBa[i], MT.InfusedOrder.mRGBa[i], MT.Force.mRGBa[i], MT.Pyrotheum.mRGBa[i], MT.Sunnarium.mRGBa[i], MT.Mcg.mRGBa[i], MT.Thaumium.mRGBa[i], MT.InfusedVis.mRGBa[i], MT.InfusedAir.mRGBa[i], MT.InfusedFire.mRGBa[i], MT.FierySteel.mRGBa[i], MT.Fireleaf.mRGBa[i], MT.Firestone.mRGBa[i], MT.ArcaneAsh.mRGBa[i]));
			sPosG.addAll(Arrays.asList(MT.ChargedCertusQuartz.mRGBa[i], MT.Enderium.mRGBa[i], MT.Vinteum.mRGBa[i], MT.U_235.mRGBa[i], MT.Am_241.mRGBa[i], MT.Am_242.mRGBa[i], MT.Pu_241.mRGBa[i], MT.Pu_243.mRGBa[i], MT.Nq_528.mRGBa[i], MT.Nq_522.mRGBa[i], MT.InfusedOrder.mRGBa[i], MT.Force.mRGBa[i], MT.Pyrotheum.mRGBa[i], MT.Sunnarium.mRGBa[i], MT.InfusedAir.mRGBa[i], MT.InfusedEarth.mRGBa[i]));
			sPosB.addAll(Arrays.asList(MT.ChargedCertusQuartz.mRGBa[i], MT.Enderium.mRGBa[i], MT.Vinteum.mRGBa[i], MT.U_235.mRGBa[i], MT.Am_241.mRGBa[i], MT.Am_242.mRGBa[i], MT.Pu_241.mRGBa[i], MT.Pu_243.mRGBa[i], MT.Nq_528.mRGBa[i], MT.Nq_522.mRGBa[i], MT.InfusedOrder.mRGBa[i], MT.Mcg.mRGBa[i], MT.InfusedVis.mRGBa[i], MT.InfusedWater.mRGBa[i], MT.Thaumium.mRGBa[i], MT.Co_60.mRGBa[i], MT.Lumium.mRGBa[i], MT.VinteumPurified.mRGBa[i], MT.ArcaneAsh.mRGBa[i]));
			sNegR.addAll(Arrays.asList(MT.InfusedEntropy.mRGBa[i], MT.NetherStar.mRGBa[i]));
			sNegG.addAll(Arrays.asList(MT.InfusedEntropy.mRGBa[i], MT.NetherStar.mRGBa[i]));
			sNegB.addAll(Arrays.asList(MT.InfusedEntropy.mRGBa[i], MT.NetherStar.mRGBa[i]));
			sRainbow.addAll(Arrays.asList(MT.GaiaSpirit.mRGBa[i], MT.GaiaSpirit.mRGBa[i], MT.Shimmerwood.mRGBa[i], MT.Shimmerwood.mRGBa[i], MT.Chimerite.mRGBa[i]));
			sRainbowFast.addAll(Arrays.asList(MT.Infinity.mRGBa[i], MT.InfusedBalance.mRGBa[i]));
		}
	}
	
	@Override
	public int addArmor(String aPrefix) {
		try {return RenderingRegistry.addNewArmourRendererPrefix(aPrefix);} catch(Throwable e) {/**/}
		return 0;
	}

	// F3-render (client): регистрация единого динамического типа модели всех GT6-блоков на mod-bus.
	// Замена удалённого `RenderingRegistry.registerBlockHandler`/render-id диспетчера (decisions/F3-render.md §2.1):
	// один `GT6BlockModel` тип. Две точки: (1) RegisterBlockStateModels — тип для blockstate-JSON (fallback);
	// (2) ModifyBakingResult — рантайм-инъекция модели ВСЕМ GT6-блокам (IRenderedBlock) БЕЗ JSON — процедурный
	// мод (сотни блоков динамически) не может держать тысячи статичных JSON; централизация 1:1 (одна модель на весь мод).
	@Override
	public void registerClientModels(net.neoforged.bus.api.IEventBus aModBus) {
		aModBus.addListener(this::onRegisterBlockStateModels);
		aModBus.addListener(this::onModifyBakingResult);
	}

	private void onRegisterBlockStateModels(net.neoforged.neoforge.client.event.RegisterBlockStateModels aEvent) {
		aEvent.registerModel(gregapi.render.GT6BlockModel.Unbaked.ID, gregapi.render.GT6BlockModel.Unbaked.MAP_CODEC);
	}

	// Рантайм-инъекция: каждому BlockState каждого GT6-блока-рендера назначаем единственный GT6BlockModel
	// (модель динамическая — читает блок/позицию/состояние в collectParts, один инстанс на весь мод).
	private void onModifyBakingResult(net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult aEvent) {
		net.minecraft.client.resources.model.sprite.Material.Baked tParticle = new net.minecraft.client.resources.model.sprite.Material.Baked(
			aEvent.getTextureGetter().apply(net.minecraft.resources.Identifier.fromNamespaceAndPath("gregtech", "blocks/system/error")), false);
		gregapi.render.GT6BlockModel tModel = new gregapi.render.GT6BlockModel(tParticle);
		java.util.Map<net.minecraft.world.level.block.state.BlockState, net.minecraft.client.renderer.block.dispatch.BlockStateModel> tMap = aEvent.getBakingResult().blockStateModels();
		int tCount = 0;
		for (net.minecraft.world.level.block.Block tBlock : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
			if (!(tBlock instanceof gregapi.render.IRenderedBlock)) continue;
			for (net.minecraft.world.level.block.state.BlockState tState : tBlock.getStateDefinition().getPossibleStates()) {
				tMap.put(tState, tModel); tCount++;
			}
		}
		// F3-render: единая item-модель GT6-предметам (НЕ BlockItem — те рендерятся моделью блока). GT6-namespace, getIconIndex-иконка.
		gregapi.render.GT6ItemModel tItemModel = new gregapi.render.GT6ItemModel();
		java.util.Map<net.minecraft.resources.Identifier, net.minecraft.client.renderer.item.ItemModel> tItemMap = aEvent.getBakingResult().itemStackModels();
		int tItemCount = 0;
		for (net.minecraft.world.item.Item tItem : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (tItem instanceof net.minecraft.world.item.BlockItem) continue;
			net.minecraft.resources.Identifier tKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tItem);
			if (tKey == null || !isGregNamespace(tKey.getNamespace())) continue;
			tItemMap.put(tKey, tItemModel); tItemCount++;
		}
		gregapi.data.CS.OUT.println("[GT6] F3-render: GT6BlockModel injected into " + tCount + " block-states, GT6ItemModel into " + tItemCount + " items.");
	}

	private static boolean isGregNamespace(String aNs) {
		return aNs.equals(gregapi.data.CS.ModIDs.GT) || aNs.equals("gregtech") || aNs.equals("gregapi");
	}
	
	@Override
	public Player getThePlayer() {
		return Minecraft.getInstance().player;
	}

	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code PlayerControllerMP.sendUseItem(player,world,stack)}
	 *  с явным {@code ItemStack} (тип метода удалён). Neo {@code MultiPlayerGameMode.useItem(Player,InteractionHand)}
	 *  берёт предмет из руки игрока, а не явный {@code aStack} — семантика "использовать ИМЕННО этот стек"
	 *  недостижима без него (движко-шов), поэтому используется основная рука как ближайший эквивалент. */
	@Override
	public boolean sendUseItemPacket(Player aPlayer, Level aWorld, ItemStack aStack) {
		Minecraft.getInstance().gameMode.useItem(aPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
		return T;
	}

	// @Override
	@SuppressWarnings("deprecation")
	public void onProxyAfterPreInit(Abstract_Mod aMod, FMLCommonSetupEvent aEvent) {
		/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code RenderingRegistry.registerEntityRenderingHandler}
		 *  (`cpw.mods.fml.client.registry`, F10-зеркало compile-only) с {@code new RenderFallingBlock()} —
		 *  в 26.1.2 {@code FallingBlockRenderer} требует {@code EntityRendererProvider.Context} и
		 *  регистрируется через {@code EntityRenderersEvent.RegisterRenderers} (decisions/F3-render.md §2.5/§6),
		 *  НЕ через этот пре-инит хук FML common setup. Заглушка сохраняет вызов центра (F10-зеркало)
		 *  с нейтральным held-объектом. */
		RenderingRegistry.registerEntityRenderingHandler(PrefixBlockFallingEntity.class, null);
		/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): {@code RenderingRegistry.registerBlockHandler}/{@code getNextAvailableRenderId}
		 *  (F10-зеркало) — старый render-id диспетчер blockstate-рендера удалён целиком (decisions/F3-render.md §1,3);
		 *  замена — {@code DynamicBlockStateModel}/{@code RegisterBlockStateModels} (там же §2.1). {@link RendererBlockFluid}/
		 *  {@link RendererBlockTextured} держат серверную поверхность (см. их class javadoc) — id тут заведомо no-op (0). */
		RenderingRegistry.registerBlockHandler(new RendererBlockFluid(0));
		RenderingRegistry.registerBlockHandler(new RendererBlockTextured(0));
		/** PORT-TODO(F3/F5 граница, baked-рендер клиента): {@code net.minecraftforge.fluids.FluidRegistry}
		 *  (старый Forge-кастом-жидкостный API) удалён целиком — F5 ({@code gregapi.fluid}/{@code FL}) уже
		 *  закрыт другим заходом и не использует эту точку (см. {@link RendererBlockFluid} class javadoc,
		 *  "F5 закрыт, сюда не лезем"); эта строка — осиротевший межшовный мост, не переносится. */
		// Check if OptiFine is loaded in order to disable some GT Render Hooks to fix Glitches.
		ITexture.Util.OPTIFINE_LOADED = FMLClientHandler.instance().hasOptifine();
		
		if (XMAS_IN_JULY) {
			// Christmas in July! Go look it up, it is an actual thing!
			Textures.BlockIcons.LEAVES_CD[0] = Textures.BlockIcons.LEAVES_BLUESPRUCE_XMAS;
			Textures.BlockIcons.LEAVES_CD[8] = Textures.BlockIcons.LEAVES_OPAQUE_BLUESPRUCE_XMAS;
		}
		if (XMAS_IN_DECEMBER) {
			// Normal Holiday Season!
			Textures.BlockIcons.LEAVES_CD[0] = Textures.BlockIcons.LEAVES_BLUESPRUCE_XMAS;
			Textures.BlockIcons.LEAVES_CD[8] = Textures.BlockIcons.LEAVES_OPAQUE_BLUESPRUCE_XMAS;
		}
		
		Date tDate = new Date();
		
		switch (tDate.getMonth()+1) {// Not going to use Calendar, because it fucking crashes with Missing Resource Exception...
		case  1:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_BROWN;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_BROWN;
			break;
		case  9:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_YELLOW;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_YELLOW;
			break;
		case 10:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_ORANGE;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_ORANGE;
			break;
		case 11:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_RED;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_RED;
			break;
		case 12:
			Textures.BlockIcons.LEAVES_AB[1] = Textures.BlockIcons.LEAVES_MAPLE_BROWN;
			Textures.BlockIcons.LEAVES_AB[9] = Textures.BlockIcons.LEAVES_OPAQUE_MAPLE_BROWN;
			break;
		}
	}
	
	// @Override
	public void onProxyBeforeInit(Abstract_Mod aMod, FMLCommonSetupEvent aEvent) {
		for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_MAP.values()) LH.add("gt.material." + tMaterial.mNameInternal, tMaterial.mNameLocal);
	}
	
	// @Override
	public void onProxyAfterInit(Abstract_Mod aMod, FMLCommonSetupEvent aEvent) {
		for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
			LH.add("oredict.prefix." + tPrefix.mNameInternal, tPrefix.mNameLocal);
			tPrefix.mNameLocal = LH.get("oredict.prefix." + tPrefix.mNameInternal, tPrefix.mNameLocal);
		}
	}
	
	// @Override
	public void onProxyAfterPostInit(Abstract_Mod aMod, FMLLoadCompleteEvent aEvent) {
		// Initialising the List of Decorative Plank Icons
		for (int i = 0; i < PlankData.PLANKS.length; i++) {
			Block tBlock = ST.block(PlankData.PLANKS[i]);
			if (tBlock != null && tBlock != NB) PlankData.PLANK_ICONS[i] = new IconContainerCopied(tBlock, ST.meta_(PlankData.PLANKS[i]), SIDE_ANY);
		}
	}
	
	public static final List<short[]> sRainbow = new ArrayListNoNulls<>(), sRainbowFast = new ArrayListNoNulls<>(), sPosR = new ArrayListNoNulls<>(), sPosG = new ArrayListNoNulls<>(), sPosB = new ArrayListNoNulls<>(), sPosA = new ArrayListNoNulls<>(), sNegR = new ArrayListNoNulls<>(), sNegG = new ArrayListNoNulls<>(), sNegB = new ArrayListNoNulls<>(), sNegA = new ArrayListNoNulls<>();
	
	/**
	 * PORT-TODO(F3/F5 граница, baked-рендер клиента): 1.7.10 {@code TextureStitchEvent.Pre} (до стежки,
	 * позволял чинить иконки жидкостей ДО постройки атласа) заменён на {@code TextureAtlasStitchedEvent}
	 * (только ПОСЛЕ стежки, `neoforge-decompiled/.../TextureAtlasStitchedEvent.java:24-38`, нет Pre-варианта)
	 * — сама точка вмешательства форсированно иная (движко-шов). Тело зовёт удалённый Forge-кастом-жидкостный
	 * {@code net.minecraftforge.fluids.FluidRegistry}/{@code IIcon Fluid.getIcon()} — F5 ({@code gregapi.fluid}/
	 * {@code FL}) уже закрыт другим заходом и эту точку не использует (см. {@link RendererBlockFluid} class
	 * javadoc, "F5 закрыт, сюда не лезем") — фикс "жидкость без иконки" переносится в baked-фазу F3 (материал
	 * атласа резолвится через {@code ModelBaker.materials()}, decisions/F3-render.md §2.3), не сюда.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTextureStitchedPre(TextureAtlasStitchedEvent aEvent) {
		//
	}
	
	/**
	 * PORT-TODO(F3, baked-рендер клиента, частично): 1.7.10 {@code net.minecraftforge.event.entity.player.ItemTooltipEvent}
	 * держал tooltip как {@code List<String>} напрямую в поле {@code toolTip}; neo-эквивалент
	 * {@code net.neoforged.neoforge.event.entity.player.ItemTooltipEvent} (`neoforge-decompiled/.../ItemTooltipEvent.java:16-70`)
	 * — геттеры, а список типизирован {@code List<Component>} (движко-шов, т.к. рендер текста теперь
	 * дерево {@code Component}, не сырая строка). Вся GT6-логика ниже (300+ строк) оперирует СТРОКАМИ
	 * (конкатенация {@code LH.Chat.*} §-кодов, {@code replaceAll}, и т.д.) и передаётся в
	 * {@code ICover.addToolTips(List<String>,...)} (сотни реализаций по всему моду, {@code List<String>}
	 * НЕ тронут — вне зоны этого захода) — чтобы не терять ни строки бизнес-логики (R8), тело работает
	 * на ЛОКАЛЬНОЙ {@code List<String>}-копии (снятой из {@code Component.getString()} до, собранной
	 * обратно через {@code Component.literal(...)} после — §-коды внутри literal-строки по-прежнему
	 * рендерятся движком, см. {@code FormattedCharSequence}); синхронизация — в {@code finally}, чтобы
	 * сработать на ЛЮБОМ выходе (return/exception), как оригинал мутировал список напрямую. Единственная
	 * генуинно потерянная строка — harvest-tooltip ({@code Block.getHarvestTool/getHarvestLevel}, API
	 * удалено целиком, замены нет, помечено ниже отдельно).
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onItemTooltip(ItemTooltipEvent aEvent) {
		if (Abstract_Mod.sFinalized < Abstract_Mod.sModCountUsingGTAPI || ST.invalid(aEvent.getItemStack())) return;
		if (!DISPLAY_TEMP_TOOLTIP) {DISPLAY_TEMP_TOOLTIP = T; return;}

		List<Component> tTT = aEvent.getToolTip();
		List<String> aToolTip = new ArrayList<>(tTT.size());
		for (Component tC : tTT) aToolTip.add(tC == null ? null : tC.getString());
		try {
			if (UT.NBT.getNBT(aEvent.getItemStack()).getBooleanOr("gt.err.oredict.output", F)) {
				aToolTip.clear();
				aToolTip.add(0, LH.Chat.BLINKING_RED+"A Recipe used an OreDict Item as Output directly, without copying it before!");
				aToolTip.add(1, LH.Chat.BLINKING_RED+"This is a typical CallByReference/CallByValue Error of the Modder doing it.");
				aToolTip.add(2, LH.Chat.BLINKING_RED+"Please check all Recipes outputting this Item, and report the Recipes to their Owner.");
				aToolTip.add(3, LH.Chat.BLINKING_RED+"The Owner of the RECIPE, NOT the Owner of the Item!");
				return;
			}

			String aRegName = ST.regName(aEvent.getItemStack());
			if (aRegName == null) {
				aToolTip.set(0, LH.Chat.BLINKING_RED+"ERROR: THIS ITEM HAS NOT BEEN REGISTERED!!!");
				aRegName = "ERROR: THIS ITEM HAS NOT BEEN REGISTERED!!!";
			}
			short aMeta = ST.meta_(aEvent.getItemStack());
			byte aBlockMeta = UT.Code.bind4(aMeta);
			Block aBlock = ST.block(aEvent.getItemStack());
			Item aItem = ST.item(aEvent.getItemStack());
			OreDictItemData tData = OM.anydata_(aEvent.getItemStack());

			if (ItemNBT.get(aEvent.getItemStack()) == null) {
				if (aBlock == Blocks.DIRT && aBlockMeta == 1) {
					aToolTip.set(0, aToolTip.get(0).replaceAll("Dirt", "Coarse Dirt"));
				}
				if (MD.RC.mLoaded && "Railcraft:part.plate".equalsIgnoreCase(aRegName)) {
					switch(aMeta) {
					case 0: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateIron")); break;
					case 1: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateSteel")); break;
					case 2: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateTinAlloy")); break;
					case 3: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateCopper")); break;
					case 4: aToolTip.set(0, LH.Chat.WHITE+LH.get("oredict.plateLead")); break;
					}
				}
			} else {
				// Anything from TiC with an NBT on it has a potential to Crash if its Tooltip is touched, due to them establishing a frikkin Iterator before sending the Tooltip Event, so lets avoid that...
				if (MD.TiC.owns(aRegName)) return;
			}

			if (MD.Mek.owns(aRegName)) aToolTip.set(0, aToolTip.get(0).replaceAll("Osmium", MT.Ge.mNameLocal));
			if (MD.BP .owns(aRegName)) aToolTip.set(0, aToolTip.get(0).replaceAll("Infused Teslatite", MT.PurpleAlloy.mNameLocal).replaceAll("Teslatite", MT.Nikolite.mNameLocal));
			if (MD.BP.mLoaded) aToolTip.set(0, aToolTip.get(0).replaceAll("Teslatite", MT.Nikolite.mNameLocal));

			if (!(aItem instanceof ItemFluidDisplay) && SHOW_INTERNAL_NAMES) {
				if (tData != null && tData.validData()) {
					if (tData.mBlackListed) {
						if (ST.isGT(aItem))
						aToolTip.add(1, LH.Chat.ORANGE + tData.toString());
						else
						aToolTip.add(1, LH.Chat.DCYAN + aRegName + LH.Chat.WHITE + " - " + LH.Chat.CYAN + aMeta + LH.Chat.WHITE + " - " + LH.Chat.ORANGE + tData.toString());
					} else {
						if (ST.isGT(aItem))
						aToolTip.add(1, LH.Chat.GREEN + tData.toString());
						else
						aToolTip.add(1, LH.Chat.DCYAN + aRegName + LH.Chat.WHITE + " - " + LH.Chat.CYAN + aMeta + LH.Chat.WHITE + " - " + LH.Chat.GREEN + tData.toString());
					}
				} else {
					if (!ST.isGT(aItem))
					aToolTip.add(1, LH.Chat.DCYAN + aRegName + LH.Chat.WHITE + " - " + LH.Chat.CYAN + aMeta);
				}
			}

			if (ItemsGT.RECIPE_REMOVED_USE_TRASH_BIN_INSTEAD.contains(aEvent.getItemStack(), T)) {
				aToolTip.add(LH.Chat.BLINKING_RED + "Recipe has been removed in favour of the GregTech Ender Garbage Bin");
			}

			ICover tCover = CoverRegistry.get(aEvent.getItemStack());
			if (tCover != null) tCover.addToolTips(aToolTip, aEvent.getItemStack(), aEvent.getFlags().isAdvanced());

			if (aBlock != NB) {
				if (IL.TC_Warded_Glass.equal(aEvent.getItemStack(), F, T)) {
					aToolTip.add(LH.getToolTipBlastResistance(aBlock, 999));
				} else if (ItemsGT.SHOW_RESISTANCE.contains(aEvent.getItemStack(), T)) {
					if (IL.ICBM_Concrete.block() == aBlock) {
						switch(aMeta) {
						default: aToolTip.add(LH.getToolTipBlastResistance(aBlock, 30)); break;
						case  1: aToolTip.add(LH.getToolTipBlastResistance(aBlock, 38)); break;
						case  2: aToolTip.add(LH.getToolTipBlastResistance(aBlock, 48)); break;
						}
					} else {
						aToolTip.add(LH.getToolTipBlastResistance(aBlock, aBlock.getExplosionResistance()));
					}
					// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code Block.getHarvestTool(meta)/getHarvestLevel(meta)}
					// (API удалено целиком в 26.1.2 — заменено тег-системой {@code BlockTags.MINEABLE_WITH_*} без
					// прямого "имя инструмента + уровень" аксессора; равноценной замены нет) — строка не добавляется.
				}
				if (BlocksGT.openableCrowbar.contains(aBlock)) {
					aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOL_TO_OPEN_CROWBAR));
				}
			}

			if (BooksGT.BOOK_REGISTER.containsKey(aEvent.getItemStack(), T)) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_SHELFABLE));
			}

			if (Sandwiches.INGREDIENTS.containsKey(aEvent.getItemStack(), T)) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_SANDWICHABLE));
			}

			/* F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code Item.isBeaconPayment(ItemStack)} (Forge 1.7.10,
			 * метод удалён) — neo эквивалент тег {@code ItemTags.BEACON_PAYMENT_ITEMS}. */
			if (aEvent.getItemStack().is(net.minecraft.tags.ItemTags.BEACON_PAYMENT_ITEMS)) {
				aToolTip.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_BEACON_PAYMENT));
			}

			/* F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code cpw.mods.fml.common.registry.GameRegistry.getFuelValue(ItemStack)}
			 * (Forge 1.7.10 static API, удалён) — neo {@code Level.fuelValues().burnDuration(ItemStack)}
			 * (`neo-decompiled/net/minecraft/world/level/block/entity/FuelValues.java:38`), инстанс с клиентского
			 * {@code Minecraft.getInstance().level} (ближайший клиентский эквивалент world-контекста). */
			Level tClientLevel = Minecraft.getInstance().level;
			long tBurnValue = tClientLevel == null ? 0 : tClientLevel.fuelValues().burnDuration(ST.amount(1, aEvent.getItemStack()));
			if (tBurnValue > 0) aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FURNACE_FUEL) + LH.Chat.WHITE + tBurnValue + " ("+(tBurnValue*EU_PER_FURNACE_TICK)+LH.Chat._RED+"HU"+LH.Chat.WHITE+")");

			if (tData != null) {
				if (tData.validPrefix()) {
					for (IOreDictListenerItem tListener : tData.mPrefix.mListenersItem) {
						String tToolTip = tListener.getListenerToolTip(tData.mPrefix, tData.mMaterial.mMaterial, aEvent.getItemStack());
						if (tToolTip != null) aToolTip.add(tToolTip);
					}
				} else {
					if (IL.RC_Firestone_Refined.equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "GT6 Burning Boxes: "+LH.Chat.WHITE+(800*EU_PER_LAVA)+LH.Chat._RED+"HU"+LH.Chat._CYAN+"per Lava Block"); else
					if (IL.RC_Firestone_Cracked.equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "GT6 Burning Boxes: "+LH.Chat.WHITE+(600*EU_PER_LAVA)+LH.Chat._RED+"HU"+LH.Chat._CYAN+"per Lava Block"); else
					if (IL.TF_Pick_Giant       .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "Repairable with Knightmetal Ingots on the Vanilla Anvil"); else
					if (IL.TF_Sword_Giant      .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "Repairable with Ironwood Ingots on the Vanilla Anvil"); else
					if (IL.TF_Lamp_of_Cinders  .equal(aEvent.getItemStack(), T, T)) aToolTip.add(LH.Chat.CYAN + "Can be used as a Lighter for GT6 things and TNT");
				}
				if (tData.validMaterial()) {
					boolean tUnburnable = F;
					for (OreDictMaterialStack tMaterial : tData.getAllMaterialWeights()) {
						if (tMaterial.mMaterial.contains(TD.Properties.UNBURNABLE)) tUnburnable = T;
						for (IOreDictListenerItem tListener : tMaterial.mMaterial.mListenersItem) {
							String tToolTip = tListener.getListenerToolTip(tData.mPrefix, tData.mMaterial.mMaterial, aEvent.getItemStack());
							if (tToolTip != null) aToolTip.add(tToolTip);
						}
					}
					if (tData.mMaterial.mMaterial.mToolTypes > 0 && (tData.mPrefix != null || (aEvent.getItemStack().getMaxStackSize() > 1 && tData.mByProducts.length == 0 && tData.mMaterial.mAmount <= U))) {
						aToolTip.add(LH.Chat.BLUE + "Q: " + tData.mMaterial.mMaterial.mToolQuality + " - S: " + tData.mMaterial.mMaterial.mToolSpeed + " - D: " + tData.mMaterial.mMaterial.mToolDurability);
					}
					if (SHOW_CHEM_FORMULAS && UT.Code.stringValid(tData.mMaterial.mMaterial.mTooltipChemical) && (tData.mPrefix == null ? tData.mByProducts.length == 0 : tData.mPrefix.contains(TD.Prefix.TOOLTIP_MATERIAL))) {
						aToolTip.add(LH.Chat.YELLOW + tData.mMaterial.mMaterial.mTooltipChemical);
					}
					if (tData.mMaterial.mMaterial == MT.Nikolite) {
						aToolTip.set(0, aToolTip.get(0).replaceAll("(Teslatite|Electrotine)", MT.Nikolite.mNameLocal));
					}
					if (tData.mMaterial.mMaterial == MT.Ge) {
						aToolTip.set(0, aToolTip.get(0).replaceAll("Osmium", MT.Ge.mNameLocal));
					}
					if (tData.validPrefix()) {
						if (!ST.isGT(aItem) && tData.mPrefix == OP.dustTiny && ANY.Blaze.mToThis.contains(tData.mMaterial.mMaterial)) {
							aToolTip.set(0, aToolTip.get(0).replaceAll(tData.mMaterial.mMaterial.mNameLocal, OP.dustTiny.mMaterialPre + tData.mMaterial.mMaterial.mNameLocal));
						}
						if (tData.mPrefix.contains(TD.Prefix.NEEDS_SHARPENING)) aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_NEEDS_SHARPENING));
						if (tData.mPrefix.contains(TD.Prefix.NEEDS_HANDLE    )) aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_NEEDS_HANDLE) + LH.Chat.WHITE + tData.mMaterial.mMaterial.mHandleMaterial.getLocal());

						if (!tData.mMaterial.mMaterial.mSourceOf.isEmpty() && tData.mPrefix.containsAny(TD.Prefix.ORE,TD.Prefix.ORE_PROCESSING_DIRTY)) {
							StringBuilder
							tToolTip = null;
							for (OreDictMaterial tMaterial : tData.mMaterial.mMaterial.mSourceOf) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.CYAN).append("Source of: ").append(LH.Chat.WHITE); else tToolTip.append(", ");
								tToolTip.append(tMaterial.getLocal());
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
						}


						ArrayListNoNulls<Integer> tShapelessAmounts = new ArrayListNoNulls<>();
						for (AdvancedCrafting1ToY tHandler : tData.mPrefix.mShapelessManagersSingle) if (tHandler.hasOutputFor(tData.mMaterial.mMaterial)) tShapelessAmounts.add(1);
						for (AdvancedCraftingXToY tHandler : tData.mPrefix.mShapelessManagers      ) if (tHandler.hasOutputFor(tData.mMaterial.mMaterial)) tShapelessAmounts.add(tHandler.mInputCount);
						if (!tShapelessAmounts.isEmpty()) {
							Collections.sort(tShapelessAmounts);
							aToolTip.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_SHAPELESS_CRAFT) + LH.Chat.WHITE + tShapelessAmounts);
						}
						if (tData.mPrefix.contains(TD.Prefix.TOOLTIP_ENCHANTS)) {
							StringBuilder
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentTools) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_TOOL_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
								if (tEnchantment.mObject == Enchantments.FIRE_ASPECT && tEnchantment.mAmount >= 3) tToolTip.append(" (Autosmelt)");
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentWeapons) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_WEAPON_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentAmmo) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_AMMO_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());
							tToolTip = null;
							for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentFishing) {
								if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_FISHING_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
								tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
							}
							if (tToolTip != null) aToolTip.add(tToolTip.toString());

							if (!tData.mPrefix.containsAny(TD.Prefix.TOOL_HEAD, TD.Prefix.WEAPON_ALIKE, TD.Prefix.AMMO_ALIKE, TD.Prefix.TOOL_ALIKE)) {
								tToolTip = null;
								for (ObjectStack<ResourceKey<Enchantment>> tEnchantment : tData.mMaterial.mMaterial.mEnchantmentArmors) {
									if (tToolTip == null) tToolTip = new StringBuilder(LH.Chat.PURPLE).append(LH.get(LH.TOOLTIP_POSSIBLE_ARMOR_ENCHANTS)).append(LH.Chat.PINK); else tToolTip.append(", ");
									tToolTip.append(UT.NBT.enchantName(tEnchantment.mObject, (int)tEnchantment.mAmount));
								}
								if (tToolTip != null) aToolTip.add(tToolTip.toString());

								if (MD.TF.mLoaded && tData.mMaterial.mMaterial.contains(TD.Properties.MAZEBREAKER)) {
									aToolTip.add(LH.Chat.PINK + LH.get(LH.TOOLTIP_TWILIGHT_MAZE_BREAKING));
								}
							}

							if (MD.BTL.mLoaded && tData.mMaterial.mMaterial.contains(TD.Properties.BETWEENLANDS)) {
								aToolTip.add(LH.Chat.GREEN + LH.get(LH.TOOLTIP_BETWEENLANDS_RESISTANCE));
							}

							if (MD.TC.mLoaded && tData.mMaterial.mMaterial.contains(TD.Properties.WARPING)) {
								aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_THAUMCRAFT_WARP));
							}
						}
						if (aBlock == NB || !(aBlock instanceof MultiTileEntityBlockInternal || aBlock instanceof IBlockBase)) {
							if (tData.mMaterial.mMaterial.contains(TD.Properties.FLAMMABLE)) {
								if (tData.mMaterial.mMaterial.contains(TD.Properties.EXPLOSIVE)) {
									aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FLAMMABLE_AND_EXPLOSIVE));
								} else {
									aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FLAMMABLE));
								}
							} else if (tData.mMaterial.mMaterial.contains(TD.Properties.EXPLOSIVE)) {
								aToolTip.add(LH.Chat.RED + LH.get(LH.TOOLTIP_EXPLOSIVE));
							}
						}
					}
					if (tUnburnable && !MD.MC.owns(aRegName)) aToolTip.add(LH.Chat.GREEN + LH.get(LH.TOOLTIP_UNBURNABLE));
				}

				if (aEvent.getFlags().isAdvanced()) {
					boolean temp = T;
					for (OreDictMaterialStack tMaterial : tData.getAllMaterialWeights()) if (tMaterial.mAmount != 0 && !tMaterial.mMaterial.contains(TD.Properties.DONT_SHOW_THIS_COMPONENT)) {
						if (temp) {
							aToolTip.add(LH.Chat.DCYAN + LH.get(LH.TOOLTIP_CONTAINED_MATERIALS));
							temp = F;
						}
						StringBuilder tString = new StringBuilder(128);
						double aWeight = tMaterial.weight();
						long tWeight = ((long)(aWeight*1000))%1000;
						tString.append(LH.Chat.WHITE ).append(UT.Code.displayUnits(tMaterial.mAmount)).append(" ");
						tString.append(LH.Chat.YELLOW).append(tMaterial.mMaterial.getLocal());
						tString.append(LH.Chat.WHITE ).append(" (");
						tString.append(LH.Chat.CYAN  ).append("M: ");
						tString.append(LH.Chat.WHITE ).append(tMaterial.mMaterial.mMeltingPoint);
						tString.append(LH.Chat.RED   ).append("K ");
						tString.append(LH.Chat.CYAN  ).append(" B: ");
						tString.append(LH.Chat.WHITE ).append(tMaterial.mMaterial.mBoilingPoint);
						tString.append(LH.Chat.RED   ).append("K ");
						tString.append(LH.Chat.CYAN  ).append(" W: ");
						tString.append(LH.Chat.WHITE ).append((long)aWeight).append(".").append(tWeight<1?"000":tWeight<10?"00"+tWeight:tWeight<100?"0"+tWeight:tWeight);
						tString.append(LH.Chat.YELLOW).append("kg");
						tString.append(LH.Chat.WHITE ).append(")");
						aToolTip.add(tString.toString());
					}
				} else {
					aToolTip.add(LH.Chat.DGRAY + "Enable F3+H Mode for Info about contained Materials.");
				}

				if (tData.validData()) {
					if (ST.isGT(aItem)) {
						if (tData.mMaterial.mMaterial.mOriginalMod == null) {
							aToolTip.add(LH.Chat.BLUE + "Material from an Unknown Mod");
						} else if (tData.mMaterial.mMaterial.mOriginalMod == MD.MC) {
							aToolTip.add(LH.Chat.BLUE + "Vanilla Material");
						} else if (tData.mMaterial.mMaterial.mOriginalMod == MD.GAPI) {
							if (tData.mMaterial.mMaterial.mID > 0 && tData.mMaterial.mMaterial.mID < 8000) {
								aToolTip.add(LH.Chat.BLUE + "Material from the Periodic Table of Elements");
							} else {
								aToolTip.add(LH.Chat.BLUE + "Random Material handled by Greg API");
							}
						} else {
							aToolTip.add(LH.Chat.BLUE + "Material from " + tData.mMaterial.mMaterial.mOriginalMod.mName);
						}
					} else {
						if ((tData.mMaterial.mMaterial == MT.Fe || tData.mMaterial.mMaterial == MT.Fe2O3) && tData.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED) && !aToolTip.get(0).contains("Native")) {
							aToolTip.set(0, aToolTip.get(0).replaceAll("Banded Iron", MT.Fe2O3.mNameLocal).replaceAll("Iron", MT.Fe2O3.mNameLocal));
						}
						if (tData.mMaterial.mMaterial == MT.Au && tData.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED) && !aToolTip.get(0).contains("Native")) {
							aToolTip.set(0, aToolTip.get(0).replaceAll("Gold", "Native Gold"));
						}
						if (tData.mMaterial.mMaterial == MT.Cu && tData.mPrefix.containsAny(TD.Prefix.ORE, TD.Prefix.ORE_PROCESSING_BASED) && !aToolTip.get(0).contains("Native")) {
							aToolTip.set(0, aToolTip.get(0).replaceAll("Copper", "Native Copper"));
						}
					}
				}
			}

			// Remove all Nulls and fix eventual Formatting mistakes.
			for (int i = 1, j = aToolTip.size(); i < j; i++) {
				String tTooltip = aToolTip.get(i);
				if (tTooltip == null || LH.Chat.BASICALLY_EMPTY_STRINGS.contains(tTooltip)) {aToolTip.remove(i--); j--;} else aToolTip.set(i, tTooltip + LH.Chat.RESET_TOOLTIP);
			}
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		} finally {
			// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): синхронизация локальной List<String> обратно в
			// List<Component> события (см. class javadoc метода) — движко-форсированный шов.
			tTT.clear();
			for (String s : aToolTip) tTT.add(s == null ? null : Component.literal(s));
		}
	}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было {@code cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent}
	 *  с полем {@code phase}/сравнением {@code == ServerTickEvent.END} (тип+поле удалены, F10-зеркало
	 *  `compat-mirror/java/cpw/mods/fml/common/gameevent/TickEvent.java` явным PORT-TODO уступает это
	 *  движко-шов сюда) — neo раздельно шлёт {@code ClientTickEvent.Pre}/{@code .Post}
	 *  (`neoforge-decompiled/net/neoforged/neoforge/client/event/ClientTickEvent.java:24-38`);
	 *  {@code .Post} = "после тика" 1:1 равно старому {@code END}-фазе — сигнатура ретипирована,
	 *  условие-обёртка снята (уже подразумевается типом события), тело БЕЗ ИЗМЕНЕНИЙ. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onClientTickEvent(ClientTickEvent.Post aEvent) {
		{
			if (CLIENT_TIME == 10) {
				// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было "Initializing the Fake Furnace Recipe Map" через
				// {@code RecipeManager.smelting().getSmeltingList()} — 1.7.10-статика {@code RecipeManager.smelting()}
				// удалена целиком (neo {@code RecipeManager} инстанс-ориентирован, читается из {@code Level});
				// фейковая furnace-recipe-карта не заполняется до отдельного захода (не render).
				// PORT-TODO(NEI/JEI-интеграция, вне рендер-объёма): было "hiding stuff from NEI" через
				// {@code Item.getSubItems(Item,CreativeModeTab,List)} (метод удалён) + {@code CreativeModeTab.tabAllSearch}
				// (константа удалена, замена {@code CreativeModeTabs.SEARCH} — {@code ResourceKey}, другой контракт) —
				// see память миссии "JEI (аналог NEI) — рано, для приёмки визуала", отдельный заход, не F3.
			}

			// Countdown the Timeout of Sounds that play in rapid succession.
			for (int i = 0; i < UT.Sounds.sPlayedSounds.size(); i++) if (UT.Sounds.sPlayedSounds.get(i).mTimer-- < 0) UT.Sounds.sPlayedSounds.remove(i--);
			// Mute Sounds for the first second so people wont get blasted with nonsense.
			if (CLIENT_TIME > 20) for (UT.Sounds.SoundWithLocation tSound : UT.Sounds.sSoundsToPlay) tSound.play();
			// Regardless of whether all the Sounds actually played, clear the List, we don't want any randomly delayed junk showing up.
			UT.Sounds.sSoundsToPlay.clear();
			
			
			switch((int)(CLIENT_TIME % 10)) {
			case   0: LH.Chat.RAINBOW_FAST = LH.Chat.RED; LH.Chat.BLINKING_CYAN = LH.Chat.CYAN; LH.Chat.BLINKING_RED = LH.Chat.RED; LH.Chat.BLINKING_ORANGE = LH.Chat.ORANGE; break;
			case   1: LH.Chat.RAINBOW_FAST = LH.Chat.ORANGE; break;
			case   2: LH.Chat.RAINBOW_FAST = LH.Chat.YELLOW; break;
			case   3: LH.Chat.RAINBOW_FAST = LH.Chat.GREEN; break;
			case   4: LH.Chat.RAINBOW_FAST = LH.Chat.CYAN; break;
			case   5: LH.Chat.RAINBOW_FAST = LH.Chat.DCYAN; LH.Chat.BLINKING_CYAN = LH.Chat.WHITE; LH.Chat.BLINKING_RED = LH.Chat.WHITE; LH.Chat.BLINKING_ORANGE = LH.Chat.YELLOW; break;
			case   6: LH.Chat.RAINBOW_FAST = LH.Chat.DBLUE; break;
			case   7: LH.Chat.RAINBOW_FAST = LH.Chat.BLUE; break;
			case   8: LH.Chat.RAINBOW_FAST = LH.Chat.PURPLE; break;
			case   9: LH.Chat.RAINBOW_FAST = LH.Chat.PINK; break;
			}
			
			switch((int)(CLIENT_TIME % 50)) {
			case   0: LH.Chat.RAINBOW = LH.Chat.RED; LH.Chat.BLINKING_GRAY = LH.Chat.GRAY; break;
			case   5: LH.Chat.RAINBOW = LH.Chat.ORANGE; break;
			case  10: LH.Chat.RAINBOW = LH.Chat.YELLOW; break;
			case  15: LH.Chat.RAINBOW = LH.Chat.GREEN; break;
			case  20: LH.Chat.RAINBOW = LH.Chat.CYAN; break;
			case  25: LH.Chat.RAINBOW = LH.Chat.DCYAN; LH.Chat.BLINKING_GRAY = LH.Chat.DGRAY; break;
			case  30: LH.Chat.RAINBOW = LH.Chat.DBLUE; break;
			case  35: LH.Chat.RAINBOW = LH.Chat.BLUE; break;
			case  40: LH.Chat.RAINBOW = LH.Chat.PURPLE; break;
			case  45: LH.Chat.RAINBOW = LH.Chat.PINK; break;
			}
			
			switch((int)(CLIENT_TIME % 250)) {
			case   0: LH.Chat.RAINBOW_SLOW = LH.Chat.RED; break;
			case  25: LH.Chat.RAINBOW_SLOW = LH.Chat.ORANGE; break;
			case  50: LH.Chat.RAINBOW_SLOW = LH.Chat.YELLOW; break;
			case  75: LH.Chat.RAINBOW_SLOW = LH.Chat.GREEN; break;
			case 100: LH.Chat.RAINBOW_SLOW = LH.Chat.CYAN; break;
			case 125: LH.Chat.RAINBOW_SLOW = LH.Chat.DCYAN; break;
			case 150: LH.Chat.RAINBOW_SLOW = LH.Chat.DBLUE; break;
			case 175: LH.Chat.RAINBOW_SLOW = LH.Chat.BLUE; break;
			case 200: LH.Chat.RAINBOW_SLOW = LH.Chat.PURPLE; break;
			case 225: LH.Chat.RAINBOW_SLOW = LH.Chat.PINK; break;
			}
			
			int tDirection = (CLIENT_TIME % 100 < 50 ? +1 : -1);
			for (short[] tArray : sPosR) tArray[0] = UT.Code.bind8(tArray[0]+tDirection);
			for (short[] tArray : sPosG) tArray[1] = UT.Code.bind8(tArray[1]+tDirection);
			for (short[] tArray : sPosB) tArray[2] = UT.Code.bind8(tArray[2]+tDirection);
			for (short[] tArray : sPosA) tArray[3] = UT.Code.bind8(tArray[3]+tDirection);
			for (short[] tArray : sNegR) tArray[0] = UT.Code.bind8(tArray[0]-tDirection);
			for (short[] tArray : sNegG) tArray[1] = UT.Code.bind8(tArray[1]-tDirection);
			for (short[] tArray : sNegB) tArray[2] = UT.Code.bind8(tArray[2]-tDirection);
			for (short[] tArray : sNegA) tArray[3] = UT.Code.bind8(tArray[3]-tDirection);
			
			boolean
			tNR = UT.Code.inside(  0,  99, (CLIENT_TIME/2) % 300), tNG = UT.Code.inside( 50, 149, (CLIENT_TIME/2) % 300), tNB = UT.Code.inside(100, 199, (CLIENT_TIME/2) % 300),
			tPR = UT.Code.inside(100, 199, (CLIENT_TIME/2) % 300), tPG = UT.Code.inside(150, 249, (CLIENT_TIME/2) % 300), tPB = UT.Code.inside(200, 299, (CLIENT_TIME/2) % 300);
			
			for (short[] tArray : sRainbow) {
			if (tPR) tArray[0] = UT.Code.bind8(tArray[0] + 1);
			if (tPG) tArray[1] = UT.Code.bind8(tArray[1] + 1);
			if (tPB) tArray[2] = UT.Code.bind8(tArray[2] + 1);
			if (tNR) tArray[0] = UT.Code.bind8(tArray[0] - 1);
			if (tNG) tArray[1] = UT.Code.bind8(tArray[1] - 1);
			if (tNB) tArray[2] = UT.Code.bind8(tArray[2] - 1);
			}
			
			tNR = UT.Code.inside( 0,  9, (CLIENT_TIME/2) % 30); tNG = UT.Code.inside( 5, 14, (CLIENT_TIME/2) % 30); tNB = UT.Code.inside(10, 19, (CLIENT_TIME/2) % 30);
			tPR = UT.Code.inside(10, 19, (CLIENT_TIME/2) % 30); tPG = UT.Code.inside(15, 24, (CLIENT_TIME/2) % 30); tPB = UT.Code.inside(20, 29, (CLIENT_TIME/2) % 30);
			
			for (short[] tArray : sRainbowFast) {
			if (tPR) tArray[0] = UT.Code.bind8(tArray[0] + 10);
			if (tPG) tArray[1] = UT.Code.bind8(tArray[1] + 10);
			if (tPB) tArray[2] = UT.Code.bind8(tArray[2] + 10);
			if (tNR) tArray[0] = UT.Code.bind8(tArray[0] - 10);
			if (tNG) tArray[1] = UT.Code.bind8(tArray[1] - 10);
			if (tNB) tArray[2] = UT.Code.bind8(tArray[2] - 10);
			}
			
			CLIENT_TIME++;
		}
	}
	
	/** F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): см. javadoc {@link gregapi.tileentity.render.ITileEntityOnDrawBlockHighlight}
	 *  — {@link ExtractBlockOutlineRenderStateEvent} не несёт {@code player}/{@code currentItem}/{@code partialTicks}
	 *  старого события. Игрок восстановлен через {@code Minecraft.getInstance().player} (тот же центральный
	 *  паттерн, что {@link #getThePlayer()}); {@code sideHit} — из {@code getHitResult().getDirection()};
	 *  {@code partialTicks} недостижим (0 — нейтрально, единственный потребитель {@link RenderHelper#drawWrenchOverlay}
	 *  уже no-op, см. его javadoc). Ветвление/делегирование в {@link ITileEntityOnDrawBlockHighlight} — БЕЗ ИЗМЕНЕНИЙ. */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onDrawBlockHighlight(ExtractBlockOutlineRenderStateEvent aEvent) {
		Player tPlayer = Minecraft.getInstance().player;
		if (tPlayer == null) return;
		byte tSide = (byte)aEvent.getHitResult().getDirection().ordinal();
		Block
		aBlock = ST.block(tPlayer.getMainHandItem());
		if (aBlock instanceof BlockMetaType && ((BlockMetaType)aBlock).mIsSlab) {
			RenderHelper.drawWrenchOverlay(tPlayer, aEvent.getBlockPos().getX(), aEvent.getBlockPos().getY(), aEvent.getBlockPos().getZ(), (byte)0, tSide, 0F);
			return;
		}
		aBlock = WD.block(tPlayer.level(), aEvent.getBlockPos().getX(), aEvent.getBlockPos().getY(), aEvent.getBlockPos().getZ());
		BlockEntity aTileEntity = WD.te(tPlayer.level(), aEvent.getBlockPos().getX(), aEvent.getBlockPos().getY(), aEvent.getBlockPos().getZ(), T);
		if (!(aTileEntity instanceof ITileEntityOnDrawBlockHighlight) || !((ITileEntityOnDrawBlockHighlight)aTileEntity).onDrawBlockHighlight(aEvent)) {
			if ((ROTATABLE_VANILLA_BLOCKS.contains(aBlock) || (ToolCompat.IC_WRENCHABLE && aTileEntity instanceof ic2.api.tile.IWrenchable)) && ST.valid(tPlayer.getMainHandItem()) && ToolsGT.contains(TOOL_wrench, tPlayer.getMainHandItem())) {
				RenderHelper.drawWrenchOverlay(tPlayer, aEvent.getBlockPos().getX(), aEvent.getBlockPos().getY(), aEvent.getBlockPos().getZ(), (byte)0, tSide, 0F);
				return;
			}
		}
	}
	
	private static List<Block> ROTATABLE_VANILLA_BLOCKS = Arrays.asList(Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.FURNACE, Blocks.FURNACE, Blocks.DROPPER, Blocks.DISPENSER, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.HOPPER, Blocks.PUMPKIN, Blocks.JACK_O_LANTERN);
}
