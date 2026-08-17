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

package gregapi.block.multitileentity;

import net.minecraft.core.BlockPos;

import gregapi.api.Optional;
import net.minecraftforge.api.distmarker.Dist;
import gregapi.block.multitileentity.IMultiTileEntity.*;
import gregapi.code.ArrayListNoNulls;
import gregapi.code.TagData;
import gregapi.cover.CoverData;
import gregapi.cover.ITileEntityCoverable;
import gregapi.data.LH;
import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.item.*;
import gregapi.oredict.IOreDictItemDataOverrideItem;
import gregapi.oredict.OreDictItemData;
import gregapi.render.BlockTextureCopied;
import gregapi.render.ITexture;
import gregapi.tileentity.ITileEntity;
import gregapi.tileentity.ITileEntityFoamable;
import gregapi.tileentity.ITileEntityMachineBlockUpdateable;
import gregapi.util.OM;
import gregapi.util.UT;
import gregapi.util.WD;
import gregtech.tileentity.energy.reactors.MultiTileEntityReactorCore;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import micdoodle8.mods.galacticraft.api.item.IItemElectric;
import micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.Container;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import gt6mirror.minecraftforge.fluids.IFluidContainerItem;
import vazkii.botania.api.item.IFlowerPlaceable;
import vazkii.botania.api.subtile.SubTileEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
  @Optional.Interface(iface = "squeek.applecore.api.food.IEdible", modid = ModIDs.APC)
, @Optional.Interface(iface = "ic2.api.item.ISpecialElectricItem", modid = ModIDs.IC2)
, @Optional.Interface(iface = "ic2.api.item.IElectricItemManager", modid = ModIDs.IC2)
, @Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.item.IItemElectric", modid = ModIDs.GC)
, @Optional.Interface(iface = "vazkii.botania.api.item.IFlowerPlaceable", modid = ModIDs.BOTA)
})
// F5/BUG-045: 1.7.10 Forge IFluidContainerItem восстановлен как живой compat-mirror
// (net/minecraftforge/fluids/IFluidContainerItem.java) — implements-список снова 1:1 с оригиналом
// (:88), делегаты getFluid/getCapacity/fill/drain ниже оживлены (per-stack состояние несёт NBT стека:
// TileEntityBase08FluidContainer.fill/drain сами пишут writeItemNBT обратно в стек).
public class MultiTileEntityItemInternal extends BlockItem implements squeek.applecore.api.food.IEdible, IItemReactorRod, IItemUpdatable, IItemColorableRGB, IOreDictItemDataOverrideItem, IItemGT, IItemNoGTOverride, IFluidContainerItem, ISpecialElectricItem, IElectricItemManager, IItemEnergy, IItemElectric, IItemRottable, IFlowerPlaceable {
	public final MultiTileEntityBlockInternal mBlock;

	public MultiTileEntityItemInternal(Block aBlock) {
		// F12-followup (item-split): id в Properties из ключа блока (BlockItem делит id с блоком; конструкция на RegisterEvent<Item>).
		super(aBlock, new Item.Properties());
		setMaxDamage(0);
		setHasSubtypes(T);
		mBlock = (MultiTileEntityBlockInternal)aBlock;
	}
	
	// F1-контракт (1.7.10 itemDamage==meta): переопределения getDamage(ItemStack) здесь НЕТ и быть не должно —
	// дефолт Forge уже отдаёт подтип (ID машины) из сырого "Damage", а само-вызов через ST.meta_ замыкает движок
	// (см. gregapi.util.ST#meta_).

	// LOCALIZATION-display: neo берёт имя через getName(ItemStack) — мост в GT6-имя (LH через getItemStackDisplayName);
	// тот же мост, что ItemBase:145/PrefixItem:213/ItemBlockBase. Без него — сырой ключ "item.gregtech.gt.multitileentity".
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}

	// F3-render (ветка 1.20.1): item-форма MTE со СВОИМ рендером (сундук, масс-хранилище) рисуется BEWLR'ом —
	// движок берёт его отсюда (IForgeItem.initializeClient, Item.java:400-408, вызов защищён Dist.CLIENT).
	// Тело — ленивый invokestatic в клиентский центр (BUG-092: клиентские типы в common-классе валят линковку
	// на выделенном сервере), тот же приём, что MTEChestRenderer.bindFirst.
	@Override
	public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> aConsumer) {
		gregapi.render.MultiTileEntityBER.bindItemExtensions(aConsumer);
	}

	// F13: neo зовёт appendHoverText (не 1.7.10 addInformation) — мост как ItemBlockBase:65 (собираем GT6-тултип через
	// addInformation ниже). Без него у машин нет характеристик (ёмкость/прочность/EU из NBT-параметров).
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, net.minecraft.world.level.Level aWorld, java.util.List<net.minecraft.network.chat.Component> aTooltips, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		if (tPlayer == null) return;
		java.util.List tList = new java.util.ArrayList();
		// BUG-018: 1.7.10-контракт vanilla Item.getTooltip — [0]=имя предмета, addInformation дописывает ПОСЛЕ него;
		// GT6-код (сэндвич MultiTileEntitySandwich:109-112) легально вставляет add(1,…)/add(2,…) «сразу после имени».
		// В neo имя в список не входит (рисуется отдельно через getName) → пустой список ронял IOOBE на каждом тултипе
		// (глотался catch'ами ниже — спам-трейс + обрубленный тултип). Подкладываем имя в [0] и не выгружаем его.
		tList.add(getItemStackDisplayName(aStack));
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (int i = 1; i < tList.size(); i++) {Object o = tList.get(i); if (o != null) aTooltips.add(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));}
	}

	// @Override
	public String getItemStackDisplayName(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		// было super.getItemStackDisplayName(aStack) (реальный vanilla Item-hook 1.7.10, ItemBlock extends Item);
		// neo BlockItem/Item не объявляет getItemStackDisplayName вовсе — звать нечего, честный эквивалент —
		// та же формула, что и на дефолтной ветке ниже (LanguageHandler.get(getUnlocalizedName), приём ItemBase.java:132).
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_GetItemName) return ((IMTE_GetItemName)tTileEntityContainer.mTileEntity).getItemName(aStack, gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack)));
		return gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack));
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer == null) {aList.add("INVALID ITEM! THIS IS A BUG IF ACQUIRED IN A LEGIT WAY!"); return;}
		if (tTileEntityContainer.mTileEntity instanceof IMTE_AddToolTips) try {((IMTE_AddToolTips)tTileEntityContainer.mTileEntity).addToolTips(aList, aStack, aF3_H);} catch(Throwable e) {e.printStackTrace(ERR);}
		if (tTileEntityContainer.mTileEntity instanceof IMTE_GetFlammability ? ((IMTE_GetFlammability)tTileEntityContainer.mTileEntity).getFlammability(SIDE_ANY, WD.getMaterial(tTileEntityContainer.mBlock).getCanBurn()) > 0 : WD.getMaterial(tTileEntityContainer.mBlock).getCanBurn()) aList.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FLAMMABLE));
		if (tTileEntityContainer.mTileEntity instanceof IMTE_GetEnchantPowerBonus) aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_ENCHANT_BONUS));
		if (tTileEntityContainer.mTileEntity instanceof ITileEntityCoverable) {
			CoverData tCoverData = ((ITileEntityCoverable)tTileEntityContainer.mTileEntity).getCoverData();
			if (tCoverData != null) for (byte tSide : ALL_SIDES_VALID) if (tCoverData.mBehaviours[tSide] != null) {aList.add(LH.Chat.DGRAY + LH.get(LH.TOOL_TO_UNCOVER_CROWBAR)); break;}
		}
		if (tTileEntityContainer.mTileEntity instanceof IMTE_GetExplosionResistance) {
			float tResistance = ((IMTE_GetExplosionResistance)tTileEntityContainer.mTileEntity).getExplosionResistance();
			if (tResistance >= 4) aList.add(LH.getToolTipBlastResistance(mBlock, tResistance));
		}
		aList.add(LH.getToolTipHarvest(WD.getMaterial(tTileEntityContainer.mBlock), tTileEntityContainer.mBlock.getHarvestTool(tTileEntityContainer.mBlockMetaData), tTileEntityContainer.mBlock.getHarvestLevel(tTileEntityContainer.mBlockMetaData)));
		while (aList.remove(null));
		// Remove all Nulls and fix eventual Formatting mistakes. No longer needed because the Tooltip Event fixes it
		//for (int i = 0, j = aList.size(); i < j; i++) if (aList.get(i) == null) {aList.remove(i--); j--;} else aList.set(i, LH.Chat.GRAY + aList.get(i) + LH.Chat.RESET_TOOLTIP);
	}
	
	public int onDespawn(ItemEntity aEntity, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_OnDespawn) try {return ((IMTE_OnDespawn)tTileEntityContainer.mTileEntity).onDespawn(aEntity, aStack);} catch(Throwable e) {e.printStackTrace(ERR);}
		return 0;
	}
	
	@Override
	public int getEntityLifespan(ItemStack aStack, Level aWorld) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_GetLifeSpan) try {return ((IMTE_GetLifeSpan)tTileEntityContainer.mTileEntity).getLifeSpan(aWorld, aStack);} catch(Throwable e) {e.printStackTrace(ERR);}
		return super.getEntityLifespan(aStack, aWorld);
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void getSubItems(Item aItem, CreativeModeTab aTab, @SuppressWarnings("rawtypes") List aList) {
		if (aTab instanceof CreativeTab) {
			for (MultiTileEntityClassContainer tClass : mBlock.mMultiTileEntityRegistry.mRegistrations) if (!tClass.mHidden || SHOW_HIDDEN_ITEMS) if (((CreativeTab)aTab).mMetaData == tClass.mCreativeTabID) if (!(tClass.mCanonicalTileEntity instanceof IMTE_GetSubItems) || ((IMTE_GetSubItems)tClass.mCanonicalTileEntity).getSubItems(mBlock, aItem, aTab, aList, tClass.mID)) aList.add(mBlock.mMultiTileEntityRegistry.getItem(tClass.mID));
		} else {
			for (MultiTileEntityClassContainer tClass : mBlock.mMultiTileEntityRegistry.mRegistrations) if (!tClass.mHidden || SHOW_HIDDEN_ITEMS) if (!(tClass.mCanonicalTileEntity instanceof IMTE_GetSubItems) || ((IMTE_GetSubItems)tClass.mCanonicalTileEntity).getSubItems(mBlock, aItem, aTab, aList, tClass.mID)) aList.add(mBlock.mMultiTileEntityRegistry.getItem(tClass.mID));
		}
	}
	
	// @Override
	public CreativeModeTab[] getCreativeTabs() {
		return mBlock.mMultiTileEntityRegistry.mCreativeTabs.values().toArray(new CreativeModeTab[mBlock.mMultiTileEntityRegistry.mCreativeTabs.size()]);
	}
	
	// F-useOn мост: neo зовёт useOn/onItemUseFirst(UseOnContext), а не 1.7.10 onItemUse(x,y,z,side,hit) — распаковка+делегация
	// в существующие тела (IItemGT-центр). onItemUseFirst-мост нужен MTE (единственный с реальной first-логикой IMTE_OnItemUseFirst).
	@Override public InteractionResult useOn(UseOnContext aCtx) {return IItemGT.bridgeUseOn(this, aCtx);}
	@Override public InteractionResult onItemUseFirst(ItemStack aStack, UseOnContext aCtx) {return IItemGT.bridgeUseOnFirst(this, aCtx);}

	@Override
	public boolean onItemUse(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aY < WD.minY(aWorld) || aY > WD.maxY(aWorld)) return F; // было aY<0 || aY>getHeight() — MC26: Y∈[minY..maxY], getHeight()=COUNT(384)≠верх; порог через центр WD
		
		try {
			Block tClickedBlock = WD.block(aWorld, aX, aY, aZ);
			if (tClickedBlock instanceof SnowLayerBlock && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
				aSide = SIDE_TOP;
			} else if (tClickedBlock != Blocks.VINE && tClickedBlock != Blocks.DEAD_BUSH && tClickedBlock != Blocks.DEAD_BUSH && !WD.replaceable(tClickedBlock, aWorld, aX, aY, aZ)) {
				aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
			}
			Block tReplacedBlock = WD.block(aWorld, aX, aY, aZ);

			// Оригинал: !tReplacedBlock.isReplaceable(...) || !mBlock.canReplace(...). mBlock (MultiTileEntityBlockInternal)
			// canReplace НЕ переопределён -> резолвился в vanilla Forge-дефолт Block.canReplace(w,x,y,z,side,stack) =
			// { return w.getBlock(x,y,z).isReplaceable(w,x,y,z); } — т.е. проверка ЗАМЕНЯЕМОСТИ ЦЕЛИ, ДУБЛИРУЮЩАЯ первую
			// клаузу. Способность (заменяемость цели) сохранена через WD.replaceable ниже — вторая клауза избыточна,
			// поглощена. Не деградация: обе клаузы оригинала проверяли одно и то же.
			if (!WD.replaceable(tReplacedBlock, aWorld, aX, aY, aZ)) return F;
			if (aStack.getCount() == 0 || (aPlayer != null && !(aPlayer).mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack))) return F;
			
			MultiTileEntityContainer aMTEContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aWorld, aX, aY, aZ, aStack);
			
			if (aMTEContainer != null
			&& (aPlayer == null || aPlayer.isShiftKeyDown() || !(aMTEContainer.mTileEntity instanceof IMTE_OnlyPlaceableWhenSneaking) || !((IMTE_OnlyPlaceableWhenSneaking)aMTEContainer.mTileEntity).onlyPlaceableWhenSneaking())
			&& ((aMTEContainer.mTileEntity instanceof IMTE_IgnorePlayerCollisionWhenPlacing && ((IMTE_IgnorePlayerCollisionWhenPlacing)aMTEContainer.mTileEntity).ignorePlayerCollisionWhenPlacing(aStack, aPlayer, aWorld, aX, aY, aZ, (byte)aSide, aHitX, aHitY, aHitZ)) || WD.noEntityCollision(aWorld, new AABB(aX, aY, aZ, aX+1, aY+1, aZ+1)))
			&& (!(aMTEContainer.mTileEntity instanceof IMTE_CanPlace) || ((IMTE_CanPlace)aMTEContainer.mTileEntity).canPlace(aStack, aPlayer, aWorld, aX, aY, aZ, (byte)aSide, aHitX, aHitY, aHitZ))
			&& WD.set(aWorld, aX, aY, aZ, aMTEContainer.mBlock, 15-aMTEContainer.mBlockMetaData, 2)) {
				
				// That is some complicated Bullshit I have to do to make my MTEs work right.
				((IMultiTileEntity)aMTEContainer.mTileEntity).setShouldRefresh(F);
				WD.te (aWorld, aX, aY, aZ, aMTEContainer.mTileEntity, F);
				WD.set(aWorld, aX, aY, aZ, aMTEContainer.mBlock, aMTEContainer.mBlockMetaData, 0, F);
				((IMultiTileEntity)aMTEContainer.mTileEntity).setShouldRefresh(T);
				WD.te (aWorld, aX, aY, aZ, aMTEContainer.mTileEntity, T);
				
				try {
					if (!(aMTEContainer.mTileEntity instanceof IMTE_OnPlaced) || ((IMTE_OnPlaced)aMTEContainer.mTileEntity).onPlaced(aStack, aPlayer, aMTEContainer, aWorld, aX, aY, aZ, (byte)aSide, aHitX, aHitY, aHitZ)) {
						WD.playStepSound(aWorld, aX+0.5, aY+0.5, aZ+0.5, aMTEContainer.mBlock);
					}
				} catch(Throwable e) {e.printStackTrace(ERR);}
				try {
					if (aMTEContainer.mTileEntity instanceof IMTE_HasMultiBlockMachineRelevantData) {
						if (((IMTE_HasMultiBlockMachineRelevantData)aMTEContainer.mTileEntity).hasMultiBlockMachineRelevantData()) ITileEntityMachineBlockUpdateable.Util.causeMachineUpdate(aWorld, aX, aY, aZ, aMTEContainer.mBlock, aMTEContainer.mBlockMetaData, F);
					}
				} catch(Throwable e) {e.printStackTrace(ERR);}
				try {
					if (!aWorld.isClientSide()) {
						// было World.notifyBlockChange(x,y,z,Block) -> тело делегировало notifyBlocksOfNeighborChange (recompSrc
						// World.java:695-698) -> Level.updateNeighborsAt(BlockPos, Block) [Level.java:338], тот же
						// форс-эквивалент, что уже принят в MultiTileEntityBlockInternal.placeBlock.
						aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), tReplacedBlock);
						// было World.func_147453_f(x,y,z,Block) -> Level.updateNeighborsAt(BlockPos, Block) [Level.java:338]
						aWorld.updateNeighborsAt(new BlockPos(aX, aY, aZ), aMTEContainer.mBlock);
					}
				} catch(Throwable e) {e.printStackTrace(ERR);}
				try {
					if (aMTEContainer.mTileEntity instanceof ITileEntity) {
						((ITileEntity)aMTEContainer.mTileEntity).onTileEntityPlaced();
					}
				} catch(Throwable e) {e.printStackTrace(ERR);}
				try {
					// было World.func_147451_t(x,y,z) -> neo Level.getLightEngine().checkBlock(BlockPos) [LevelLightEngine.java:32]
					aWorld.getLightEngine().checkBlock(new BlockPos(aX, aY, aZ));
				} catch(Throwable e) {e.printStackTrace(ERR);}
				
				aStack.setCount(aStack.getCount()-1);
				return T;
			}
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		return F;
	}
	
	@Override
	public void updateItemStack(ItemStack aStack) {
		MultiTileEntityClassContainer tContainer = mBlock.mMultiTileEntityRegistry.getClassContainer(aStack);
		if (tContainer == null) return;
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemUpdatable) {
			((IItemUpdatable)tTileEntityContainer.mTileEntity).updateItemStack(aStack);
		}
	}
	@Override
	public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {
		MultiTileEntityClassContainer tContainer = mBlock.mMultiTileEntityRegistry.getClassContainer(aStack);
		if (tContainer == null) return;
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemUpdatable) {
			((IItemUpdatable)tTileEntityContainer.mTileEntity).updateItemStack(aStack, aWorld, aX, aY, aZ);
		}
	}
	
	// BUG-021 v2: мост neo per-stack канала на 1.7.10-хук ниже (per-MTE стак из класс-контейнера/IMTE_GetMaxStackSize).
	@Override public int getMaxStackSize(ItemStack aStack) {return UT.Code.bindStack(getItemStackLimit(aStack));}
	// @Override
	public int getItemStackLimit(ItemStack aStack) {
		MultiTileEntityClassContainer tContainer = mBlock.mMultiTileEntityRegistry.getClassContainer(aStack);
		if (tContainer == null) return 1;
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_GetMaxStackSize) {
			return UT.Code.bindStack(((IMTE_GetMaxStackSize)tTileEntityContainer.mTileEntity).getMaxStackSize(aStack, tContainer.mStackSize));
		}
		return tContainer.mStackSize;
	}
	
	// @Override
	public void onCreated(ItemStack aStack, Level aWorld, Player aPlayer) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_OnCrafted) {
			updateItemStack(aStack);
			((IMTE_OnCrafted)tTileEntityContainer.mTileEntity).onCrafted(aPlayer, aWorld, aStack);
		}
		updateItemStack(aStack);
	}

	// Подключение канала «предмет скрафчен» к движку (2026-07-30, реестр мёртвых каналов). 1.7.10
	// onCreated(ItemStack,World,EntityPlayer) → neo Item.onCraftedBy(ItemStack,Player) (Item.java:310;
	// мир берётся оттуда же, чем это делает сам движок — player.level(), Item.java:311). Приём взят у брата
	// ItemArmorBase:255. Без моста у свежескрафченной машины не звался IMTE_OnCrafted.onCrafted и не
	// обновлялся NBT предмета (updateItemStack) — то есть личность блока в стеке оставалась незаполненной.
	@Override public void onCraftedBy(ItemStack aStack, Level aWorld, Player aPlayer) {onCreated(aStack, aWorld, aPlayer);}
	
	@Override
	public OreDictItemData getOreDictItemData(ItemStack aStack) {
		List<OreDictItemData> rList = new ArrayListNoNulls<>(F, OM.data(aStack));
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		// Yes I keep Covers a special case, less chances for fuck ups.
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof ITileEntityCoverable) {
			CoverData tCoverData = ((ITileEntityCoverable)tTileEntityContainer.mTileEntity).getCoverData();
			if (tCoverData != null) for (byte tSide : ALL_SIDES_VALID) rList.add(OM.anydata(tCoverData.getCoverItem(tSide)));
		}
		// Same for foamed Blocks.
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof ITileEntityFoamable && ((ITileEntityFoamable)tTileEntityContainer.mTileEntity).hasFoam(SIDE_ANY)) {
			rList.add(new OreDictItemData(MT.ConstructionFoam, U));
			if (((ITileEntityFoamable)tTileEntityContainer.mTileEntity).ownedFoam(SIDE_ANY)) rList.add(new OreDictItemData(MT.Pd, U4));
		}
		// General case for Custom additional OreDictItemData.
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_GetOreDictItemData) {
			rList = ((IMTE_GetOreDictItemData)tTileEntityContainer.mTileEntity).getOreDictItemData(rList);
		}
		return rList.isEmpty() ? null : rList.size() > 1 ? new OreDictItemData(rList) : rList.get(0);
	}
	
	@Override
	public FluidStack getFluid(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IFluidContainerItem) {
			FluidStack rFluid = ((IFluidContainerItem)tTileEntityContainer.mTileEntity).getFluid(aStack);
			updateItemStack(aStack);
			return rFluid;
		}
		return NF;
	}

	@Override
	public int getCapacity(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IFluidContainerItem) {
			int rCapacity = ((IFluidContainerItem)tTileEntityContainer.mTileEntity).getCapacity(aStack);
			updateItemStack(aStack);
			return rCapacity;
		}
		return 0;
	}

	@Override
	public int fill(ItemStack aStack, FluidStack aFluid, boolean aDoFill) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IFluidContainerItem) {
			int tFilled = ((IFluidContainerItem)tTileEntityContainer.mTileEntity).fill(aStack, aFluid, aDoFill);
			updateItemStack(aStack);
			return tFilled;
		}
		return 0;
	}

	@Override
	public FluidStack drain(ItemStack aStack, int aMaxDrain, boolean aDoDrain) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IFluidContainerItem) {
			FluidStack rFluid = ((IFluidContainerItem)tTileEntityContainer.mTileEntity).drain(aStack, aMaxDrain, aDoDrain);
			updateItemStack(aStack);
			return rFluid;
		}
		return NF;
	}
	
	@Override
	public boolean canRecolorItem(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemColorableRGB) {
			return ((IItemColorableRGB)tTileEntityContainer.mTileEntity).canRecolorItem(aStack);
		}
		return F;
	}
	
	@Override
	public boolean recolorItem(ItemStack aStack, int aRGB) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemColorableRGB) {
			if (((IItemColorableRGB)tTileEntityContainer.mTileEntity).recolorItem(aStack, aRGB)) {
				updateItemStack(aStack);
				return T;
			}
		}
		return F;
	}
	
	@Override
	public boolean canDecolorItem(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemColorableRGB) {
			return ((IItemColorableRGB)tTileEntityContainer.mTileEntity).canDecolorItem(aStack);
		}
		return F;
	}
	
	@Override
	public boolean decolorItem(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemColorableRGB) {
			if (((IItemColorableRGB)tTileEntityContainer.mTileEntity).decolorItem(aStack)) {
				updateItemStack(aStack);
				return T;
			}
		}
		return F;
	}
	
	// @Override
	public boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float hitX, float hitY, float hitZ) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_OnItemUseFirst) {
			boolean rReturn = ((IMTE_OnItemUseFirst)tTileEntityContainer.mTileEntity).onItemUseFirst(this, aStack, aPlayer, aWorld, aX, aY, aZ, (byte)aSide, hitX, hitY, hitZ);
			updateItemStack(aStack);
			return rReturn;
		}
		return F;
	}
	
	// @Override
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_OnItemRightClick) {
			ItemStack rStack = ((IMTE_OnItemRightClick)tTileEntityContainer.mTileEntity).onItemRightClick(this, aStack, aWorld, aPlayer);
			if (aStack != rStack) updateItemStack(aStack);
			updateItemStack(rStack);
			return rStack;
		}
		return aStack;
	}
	
	// @Override
	public int getMaxItemUseDuration(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_GetMaxItemUseDuration) {
			int rDuration = ((IMTE_GetMaxItemUseDuration)tTileEntityContainer.mTileEntity).getMaxItemUseDuration(this, aStack);
			updateItemStack(aStack);
			return rDuration;
		}
		return 0;
	}
	
	// @Override
	public UseAnim getItemUseAction(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_GetItemUseAction) {
			UseAnim rAction = ((IMTE_GetItemUseAction)tTileEntityContainer.mTileEntity).getItemUseAction(this, aStack);
			updateItemStack(aStack);
			return rAction;
		}
		return UseAnim.NONE;
	}
	
	// @Override
	public ItemStack onEaten(ItemStack aStack, Level aWorld, Player aPlayer) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_OnEaten) {
			ItemStack rStack = ((IMTE_OnEaten)tTileEntityContainer.mTileEntity).onEaten(this, aStack, aWorld, aPlayer);
			if (aStack != rStack) updateItemStack(aStack);
			updateItemStack(rStack);
			return rStack;
		}
		return aStack;
	}

	// F13/BUG-048: мост движок->мод для use-цепочки предмета. 4 метода выше — 1.7.10-имена (в neo Item их нет,
	// движок их не звал — питьё из Кувшина/Кубка/Термоса и еда Сэндвича были мертвы); живые neo-хуки ниже
	// делегируют в них 1:1. Паттерн «начать пить» = TE-делегат сам зовёт startUsingItem (LivingEntity.java:3529),
	// хук возвращает CONSUME — как ванильный Item.use с BLOCKS_ATTACKS (Item.java:216-218). Замена стека
	// возвратом (контракт 1.7.10 onItemRightClick) -> setItemInHand + SUCCESS; иначе PASS (1.7.10 не различал,
	// мутации in-place уже применены). MAIN_HAND в TE-делегате 1:1 (1.7.10 offhand не имел).
	@Override
	public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level aWorld, Player aPlayer, InteractionHand aHand) {
		ItemStack aStack = aPlayer.getItemInHand(aHand);
		ItemStack rStack = onItemRightClick(aStack, aWorld, aPlayer);
		if (rStack != aStack) {aPlayer.setItemInHand(aHand, gregapi.util.ST.nn(rStack)); return net.minecraft.world.InteractionResultHolder.success(rStack);} // F15: контракт 1.7.10 может вернуть null — в руку движка только через центр
		if (aPlayer.isUsingItem()) return net.minecraft.world.InteractionResultHolder.consume(aStack);
		return net.minecraft.world.InteractionResultHolder.pass(aStack);
	}

	@Override
	public int getUseDuration(ItemStack aStack) {
		return getMaxItemUseDuration(aStack); // 1.20.1 Item.getUseDuration(ItemStack) — форма 1.7.10 getMaxItemUseDuration
	}

	@Override
	public UseAnim getUseAnimation(ItemStack aStack) {
		return getItemUseAction(aStack); // было Item.getItemUseAction(ItemStack) (1.7.10) -> neo Item.getUseAnimation(ItemStack) (Item.java:317)
	}

	@Override
	public ItemStack finishUsingItem(ItemStack aStack, Level aWorld, LivingEntity aEntity) {
		// было Item.onEaten(ItemStack,World,EntityPlayer) (1.7.10) -> neo Item.finishUsingItem(ItemStack,Level,LivingEntity)
		// (Item.java:232); 1.7.10 звал только для игрока — Player-гейт, не-игрок падает в super (Consumable-путь, у MTE пуст).
		return aEntity instanceof Player tPlayer ? onEaten(aStack, aWorld, tPlayer) : super.finishUsingItem(aStack, aWorld, aEntity);
	}
	
	@Override
	@Optional.Method(modid = ModIDs.APC)
	public squeek.applecore.api.food.FoodValues getFoodValues(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IMTE_GetFoodValues) return ((IMTE_GetFoodValues)tTileEntityContainer.mTileEntity).getFoodValues(this, aStack);
		return null;
	}
	
	@Override
	public ItemStack getRotten(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemRottable) return ((IItemRottable)tTileEntityContainer.mTileEntity).getRotten(aStack);
		// F5/BUG-045 (1:1): this снова IFluidContainerItem — восстановлена 2-арг перегрузка оригинала (:437).
		return IItemRottable.RottingUtil.rotting(aStack, this);
	}

	@Override
	public ItemStack getRotten(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemRottable) return ((IItemRottable)tTileEntityContainer.mTileEntity).getRotten(aStack, aWorld, aX, aY, aZ);
		// F5/BUG-045 (1:1): this снова IFluidContainerItem — восстановлена 2-арг перегрузка оригинала (:444).
		return IItemRottable.RottingUtil.rotting(aStack, this);
	}
	
	
	@Override
	public boolean isReactorRod(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).isReactorRod(aStack);
		return F;
	}

	@Override
	public boolean isModerated(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).isModerated(aReactor, aSlot, aStack);
		return false;
	}

	@Override
	public void updateModeration(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod)
			((IItemReactorRod)tTileEntityContainer.mTileEntity).updateModeration(aReactor, aSlot, aStack);
	}

	@Override
	public int getReactorRodNeutronEmission(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).getReactorRodNeutronEmission(aReactor, aSlot, aStack);
		return 0;
	}
	@Override
	public boolean getReactorRodNeutronReaction(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).getReactorRodNeutronReaction(aReactor, aSlot, aStack);
		return F;
	}
	@Override
	public int getReactorRodNeutronReflection(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack, int aNeutrons, boolean aModerated) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).getReactorRodNeutronReflection(aReactor, aSlot, aStack, aNeutrons, aModerated);
		return 0;
	}
	@Override
	public int getReactorRodNeutronMaximum(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).getReactorRodNeutronMaximum(aReactor, aSlot, aStack);
		return 0;
	}
	@Override
	public ITexture getReactorRodTextureSides(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack, boolean aActive) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).getReactorRodTextureSides(aReactor, aSlot, aStack, aActive);
		return BlockTextureCopied.get(Blocks.COBBLESTONE);
	}
	@Override
	public ITexture getReactorRodTextureTop(MultiTileEntityReactorCore aReactor, int aSlot, ItemStack aStack, boolean aActive) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemReactorRod) return ((IItemReactorRod)tTileEntityContainer.mTileEntity).getReactorRodTextureTop(aReactor, aSlot, aStack, aActive);
		return BlockTextureCopied.get(Blocks.COBBLESTONE);
	}
	
	
	@Override
	public boolean isEnergyType(TagData aEnergyType, ItemStack aStack, boolean aEmitting) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).isEnergyType(aEnergyType, aStack, aEmitting);
		return F;
	}
	@Override
	public Collection<TagData> getEnergyTypes(ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergyTypes(aStack);
		return Collections.emptyList();
	}
	@Override
	public long doEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, Container aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoInject) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).doEnergyInjection(aEnergyType, aStack, aSize, aAmount, aInventory, aWorld, aX, aY, aZ, aDoInject);
		return 0;
	}
	@Override
	public boolean canEnergyInjection(TagData aEnergyType, ItemStack aStack, long aSize) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).canEnergyInjection(aEnergyType, aStack, aSize);
		return F;
	}
	@Override
	public long doEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize, long aAmount, Container aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoExtract) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).doEnergyExtraction(aEnergyType, aStack, aSize, aAmount, aInventory, aWorld, aX, aY, aZ, aDoExtract);
		return 0;
	}
	@Override
	public boolean canEnergyExtraction(TagData aEnergyType, ItemStack aStack, long aSize) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).canEnergyExtraction(aEnergyType, aStack, aSize);
		return F;
	}
	@Override
	public boolean useEnergy(TagData aEnergyType, ItemStack aStack, long aEnergyAmount, LivingEntity aPlayer, Container aInventory, Level aWorld, int aX, int aY, int aZ, boolean aDoUse) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).useEnergy(aEnergyType, aStack, aEnergyAmount, aPlayer, aInventory, aWorld, aX, aY, aZ, aDoUse);
		return F;
	}
	@Override
	public ItemStack setEnergyStored(TagData aEnergyType, ItemStack aStack, long aAmount) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).setEnergyStored(aEnergyType, aStack, aAmount);
		return aStack;
	}
	@Override
	public long getEnergyStored(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergyStored(aEnergyType, aStack);
		return 0;
	}
	@Override
	public long getEnergyCapacity(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergyCapacity(aEnergyType, aStack);
		return 0;
	}
	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergySizeInputMin(aEnergyType, aStack);
		return 0;
	}
	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergySizeOutputMin(aEnergyType, aStack);
		return 0;
	}
	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergySizeInputRecommended(aEnergyType, aStack);
		return 0;
	}
	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergySizeOutputRecommended(aEnergyType, aStack);
		return 0;
	}
	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergySizeInputMax(aEnergyType, aStack);
		return 0;
	}
	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, ItemStack aStack) {
		MultiTileEntityContainer tTileEntityContainer = mBlock.mMultiTileEntityRegistry.getNewTileEntityContainer(aStack);
		if (tTileEntityContainer != null && tTileEntityContainer.mTileEntity instanceof IItemEnergy) return ((IItemEnergy)tTileEntityContainer.mTileEntity).getEnergySizeOutputMax(aEnergyType, aStack);
		return 0;
	}
	
	// ⚠️ КАНАЛ ЧУЖОГО МОДА — подключать нечего и некому. В оригинале charge/discharge реализовывали
	// интерфейсы IC2 (ic2.api.item.IElectricItemManager) и Galacticraft (micdoodle8...IItemElectric) под
	// @Optional.Interface (оригинал MultiTileEntityItemInternal.java:45-48, 83-88), то есть работали ТОЛЬКО
	// при наличии этих модов. Ни IC2, ни Galacticraft в сборке нет — мертвы законно, как остальные compat-каналы.
	// Собственная энергия GT6 идёт своим путём (IItemEnergy выше, doEnergyInjection/doEnergyExtraction) и жива.
	// @Override
	public double charge   (ItemStack aStack, double aCharge, int aTier, boolean aIgnoreTransferLimit, boolean aSimulate) {
		if (aCharge < V[aTier = UT.Code.bind4(aTier)]) return 0;
		return V[aTier] * doEnergyInjection (TD.Energy.EU, aStack, V[aTier], (long)(aCharge / V[aTier]), null, null, 0, 0, 0, !aSimulate);
	}
	
	// ⚠️ КАНАЛ ЧУЖОГО МОДА — та же ветка, что charge выше: интерфейсы IC2 (IElectricItemManager) и
	// Galacticraft (IItemElectric) под @Optional.Interface в оригинале (:45-48,83-88). Модов в сборке нет.
	// @Override
	public double discharge(ItemStack aStack, double aCharge, int aTier, boolean aIgnoreTransferLimit, boolean aBatteryAlike, boolean aSimulate) {
		if (aCharge < V[aTier = UT.Code.bind4(aTier)]) return 0;
		return V[aTier] * doEnergyExtraction(TD.Energy.EU, aStack, V[aTier], (long)(aCharge / V[aTier]), null, null, 0, 0, 0, !aSimulate);
	}
	
	// ⚠️ КАНАЛ ЧУЖОГО МОДА — та же ветка, что charge/discharge выше: это Galacticraft-интерфейс
	// (micdoodle8...IItemElectric, оригинал :47,85), видно и по EnergyConfigHandler.IC2_RATIO в теле.
	// Мода в сборке нет — мёртв законно. Своя энергия GT6 (IItemEnergy) жива.
	// @Override
	public float discharge(ItemStack aStack, float aEnergy, boolean aDoExtract) {
		if (aEnergy <= 0) return 0;
		long tMaxOut = getEnergySizeOutputMax(TD.Energy.EU, aStack);
		if (!canEnergyExtraction(TD.Energy.EU, aStack, tMaxOut)) return 0;
		long tAmount = UT.Code.bind(1, tMaxOut, (long)(aEnergy / EnergyConfigHandler.IC2_RATIO));
		return useEnergy(TD.Energy.EU, aStack, tAmount, null, null, null, 0, 0, 0, F) && useEnergy(TD.Energy.EU, aStack, tAmount, null, null, null, 0, 0, 0, T) ? tAmount * EnergyConfigHandler.IC2_RATIO : 0;
	}
	
	@Optional.Method(modid = ModIDs.IC2 ) public IElectricItemManager getManager(ItemStack aStack) {return this;} // We are our own Manager
	@Optional.Method(modid = ModIDs.BOTA) public Block getBlockToPlaceByFlower(ItemStack aStack, SubTileEntity aFlower, int aX, int aY, int aZ) {return null;}
	@Optional.Method(modid = ModIDs.BOTA) public void onBlockPlacedByFlower(ItemStack aStack, SubTileEntity aFlower, int aX, int aY, int aZ) {/**/}
	
	public boolean func_150936_a(Level aWorld, int aX, int aY, int aZ, int aSide, Player aPlayer, ItemStack aStack) {return T;}
	public String getToolTip(ItemStack aStack) {return null;} // This has its own ToolTip Handler, no need to let the IC2 Handler screw us up at this Point
	public void chargeFromArmor(ItemStack aStack, LivingEntity aPlayer) {/**/}
	public float getElectricityStored(ItemStack aStack) {return getEnergyStored(TD.Energy.EU, aStack) * EnergyConfigHandler.IC2_RATIO;}
	public float getMaxElectricityStored(ItemStack aStack) {return getEnergyCapacity(TD.Energy.EU, aStack) * EnergyConfigHandler.IC2_RATIO;}
	public void setElectricity(ItemStack aStack, float joules) {/**/}
	public float recharge(ItemStack aStack, float aEnergy, boolean aDoInject) {return 0;}
	public float getTransfer(ItemStack aStack) {return 0;}
	public int getTierGC(ItemStack aStack) {return 1;}
	public double getCharge(ItemStack aStack) {return getEnergyStored(TD.Energy.EU, aStack);}
	public boolean canUse(ItemStack aStack, double aAmount) {return useEnergy(TD.Energy.EU, aStack, (long)aAmount, null, null, null, 0, 0, 0, F);}
	public boolean use(ItemStack aStack, double aAmount, LivingEntity aPlayer) {return useEnergy(TD.Energy.EU, aStack, (long)aAmount, aPlayer, null, null, 0, 0, 0, T);}
	public Item getChargedItem(ItemStack itemStack) {return this;}
	public Item getEmptyItem(ItemStack itemStack) {return this;}
	public boolean canProvideEnergy(ItemStack aStack) {return T;}
	public double getMaxCharge(ItemStack aStack) {return getEnergyCapacity(TD.Energy.EU, aStack);}
	public double getTransferLimit(ItemStack aStack) {return getEnergySizeInputRecommended(TD.Energy.EU, aStack);}
	public int getTier(ItemStack aStack) {return UT.Code.tierMax(getEnergySizeInputMax(TD.Energy.EU, aStack));}
	public final String getUnlocalizedName() {return mBlock.mMultiTileEntityRegistry.mNameInternal;}
	public final String getUnlocalizedName(ItemStack aStack) {return mBlock.mMultiTileEntityRegistry.mNameInternal+"."+getDamage(aStack);}
	public final boolean hasContainerItem(ItemStack aStack) {return getContainerItem(aStack) != null;}
	public ItemStack getContainerItem(ItemStack aStack) {return null;}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	public int getSpriteNumber() {return 0;}
	public void registerIcons(Object aRegister) {/**/}
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было itemIcon=Items.BREAD.getIconFromDamage(0) (фикс eating-particle 1.7.10) —
	// и поле Item.itemIcon, и метод Item.getIconFromDamage(int) удалены в 26.1.2 целиком, замены нет до Фазы C.
	public ResourceLocation getIconFromDamage(int aMeta) {throw new UnsupportedOperationException("F3 dead-interface: 1.7.10 Item.getIconFromDamage(meta) удалён из neo (НЕ @Override; было itemIcon для eating-particle). MTEItemInternal — BlockItem, рендерится моделью блока; GT6ItemModel пропускает BlockItem'ы. Defensive throw.");}
	public boolean isBookEnchantable(ItemStack aStack, ItemStack aBook) {return F;}
	public boolean getIsRepairable(ItemStack aStack, ItemStack aMaterial) {return F;}
	public int getItemEnchantability() {return 0;}
	public final boolean getShareTag() {return T;} // just to be sure.
}
