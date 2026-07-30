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
 */

package gregapi.block.prefixblock;
import gregapi.util.WD;

import gregapi.api.Optional;
import gregapi.code.ItemNBT;
import gregapi.data.CS.*;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.item.*;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;
import vazkii.botania.api.item.IFlowerPlaceable;
import vazkii.botania.api.subtile.SubTileEntity;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
@Optional.InterfaceList(value = {
  @Optional.Interface(iface = "vazkii.botania.api.item.IFlowerPlaceable", modid = ModIDs.BOTA)
})
public class PrefixBlockItem extends BlockItem implements IItemUpdatable, IPrefixItem, IItemGT, IItemNoGTOverride, IFlowerPlaceable {
	public final PrefixBlock mBlock;
	
	public PrefixBlockItem(Block aBlock) {
		// F12-followup (item-split): id в Properties из ключа блока (BlockItem делит id с блоком; конструкция на RegisterEvent<Item>).
		super(aBlock, new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aBlock))));
		setMaxDamage(0);
		setHasSubtypes(T);
		mBlock = (PrefixBlock)aBlock;
		mBlock.mPrefix.mRegisteredPrefixItems.add(this);
		
		if ((SHOW_HIDDEN_PREFIXES || !mBlock.mPrefix.contains(TD.Creative.HIDDEN)) && (SHOW_ORE_BLOCK_PREFIXES || "gt.meta.ore.normal.default".equalsIgnoreCase(mBlock.mNameInternal) || !mBlock.mPrefix.contains(TD.Prefix.ORE) || mBlock.mPrefix.contains(TD.Prefix.STORAGE_BASED))) {
			// F16 (1:1 golden): видимый prefix-блок → СВОЯ prefix-вкладка (setCreativeTab(mPrefix.mCreativeTab)), НЕ ванильный
			// tabBlock. this = BlockItem блока (в neo вкладки наполняются предметами, а не блоками) — присоединяем его.
			if (mBlock.mPrefix.mCreativeTab == null) mBlock.mPrefix.mCreativeTab = new CreativeTab(mBlock.mPrefix.mNameInternal, mBlock.mPrefix.mNameCategory, this, W);
			gregapi.item.CreativeTabsGT.joinOwnTab(this, mBlock.mPrefix.mCreativeTab);
		} else {
			gregapi.item.CreativeTabsGT.assign(mBlock, gregapi.item.CreativeTabsGT.BLOCK); // hidden/ore-скрытый → tabBlock (golden else)
		}
	}
	
	// @Override
	@SuppressWarnings("unchecked")
	public void getSubItems(Item var1, CreativeModeTab aCreativeTab, @SuppressWarnings("rawtypes") List aList) {
		if (!mBlock.mHidden && (SHOW_HIDDEN_PREFIXES || !mBlock.mPrefix.contains(TD.Creative.HIDDEN)) && (SHOW_ORE_BLOCK_PREFIXES || mBlock == BlocksGT.ore || !mBlock.mPrefix.contains(TD.Prefix.ORE) || mBlock.mPrefix.contains(TD.Prefix.STORAGE_BASED))) for (int i = 0; i < mBlock.mMaterialList.length; i++) if (mBlock.mPrefix.isGeneratingItem(mBlock.mMaterialList[i])) if (SHOW_HIDDEN_MATERIALS || !mBlock.mMaterialList[i].mHidden) {
			ItemStack tStack = ST.make(this, 1, i);
			updateItemStack(tStack);
			if (ST.meta_(tStack) == i) aList.add(tStack);
		}
		if (aList.isEmpty()) ST.hide(this);
	}
	
	// F-useOn мост: neo зовёт useOn(UseOnContext), а не 1.7.10 onItemUse. PrefixBlockItem (руды/материал-блоки) в 1.7.10
	// НЕ имел своего onItemUse — использовал vanilla ItemBlock.onItemUse-скелет → placeBlockAt (перенос NBT-материала).
	// Скелет воспроизведён 1:1 (образец BlockBase.onItemUse:222), завершение — свой placeBlockAt (mBlock.placeBlock+ItemNBT);
	// мета материала берётся placeBlockAt из стека (ST.meta_) сам, переданный aMeta-аргумент им игнорируется.
	@Override public InteractionResult useOn(UseOnContext aCtx) {return IItemGT.bridgeUseOn(this, aCtx);}
	@Override public boolean onItemUse(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {
		if (aStack.getCount() == 0) return F;
		Block tBlock = WD.block(aWorld, aX, aY, aZ);
		if (tBlock == Blocks.SNOW && (WD.meta(aWorld, aX, aY, aZ) & 7) < 1) {
			aSide = SIDE_UP;
		} else if (tBlock != Blocks.VINE && tBlock != Blocks.DEAD_BUSH && !WD.replaceable(tBlock, aWorld, aX, aY, aZ)) {
			aX += OFFX[aSide]; aY += OFFY[aSide]; aZ += OFFZ[aSide];
		}
		if (!WD.replaceable(WD.block(aWorld, aX, aY, aZ), aWorld, aX, aY, aZ)) return F;
		if (aPlayer != null && !aPlayer.mayUseItemAt(new BlockPos(aX, aY, aZ), FORGE_DIR[aSide], aStack)) return F;
		if (placeBlockAt(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ, ST.meta_(aStack))) {
			aStack.setCount(aStack.getCount()-1);
		}
		return T;
	}

	// @Override
	public boolean placeBlockAt(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float hitX, float hitY, float hitZ, int aMeta) {
		if (mBlock.placeBlock(aWorld, aX, aY, aZ, (byte)aSide, ST.meta_(aStack), ItemNBT.get(aStack), T, F)) {
			if (WD.block(aWorld, aX, aY, aZ) == getBlock()) {
				BlockPos tPos = new BlockPos(aX, aY, aZ);
				getBlock().setPlacedBy(aWorld, tPos, aWorld.getBlockState(tPos), aPlayer, aStack); // было onBlockPlacedBy(World,x,y,z,EntityPlayer,ItemStack) -> Block.setPlacedBy(Level,BlockPos,BlockState,LivingEntity,ItemStack) (Block.java:473)
				// onPostBlockPlaced(World,x,y,z,meta): в GT6 НИКЕМ не переопределён (греп по gregapi/: единственная ссылка — этот
				// вызыватель) -> вызывал vanilla-no-op, ничего не терял. Общий пост-place-хук в neo вызывается движком АВТОМАТИЧЕСКИ:
				// BlockState.onPlace(Level,BlockPos,oldState,movedByPiston) из LevelChunk.setBlockState:328 при setBlock. Явный вызов не нужен — не деградация.
			}
			return T;
		}
		UT.Entities.sendchat(aPlayer, "Cannot place Block in this Environment!");
		return F;
	}
	
	// @Override
	// ⚠️ КАНАЛ РАЗОБРАН — цвет ПРЕДМЕТА в neo задаётся МОДЕЛЬЮ, не методом (реестр, 2026-07-30).
	// У блоков этот канал уже закрыт центром GT6BlockTint + одна регистрация (см. GT_API_Proxy_Client), но
	// у предметов API иной: ItemTintSources.register(Identifier, MapCodec<? extends ItemTintSource>) —
	// регистрируется ТИП источника тинта, а ссылка на него стоит в json-модели предмета. Модели предметов
	// GT6 генерируются процедурно, поэтому решается вместе с F3-рендером предметов, а не делегатом.
	// Следствие сейчас: предметы GT6 рисуются без материального оттенка.
	public int getColorFromItemStack(ItemStack aStack, int aRenderPass) {
		if (aRenderPass == 0) {
			short aMetaData = ST.meta_(aStack);
			if (UT.Code.exists(aMetaData, mBlock.mMaterialList)) return UT.Code.getRGBInt(mBlock.mMaterialList[aMetaData].mRGBa[mBlock.mPrefix.mState]);
		}
		return UNCOLORED;
	}
	
	// @Override
	public final String getUnlocalizedName(ItemStack aStack) {
		short aMetaData = ST.meta_(aStack);
		if (aMetaData == W) return mBlock.mNameInternal+"."+W;
		if (UT.Code.exists(aMetaData, mBlock.mMaterialList)) return "oredict." + mBlock.mPrefix.dat(mBlock.mMaterialList[aMetaData]).toString();
		return mBlock.getUnlocalizedName();
	}
	
	// F13: neo зовёт appendHoverText (не 1.7.10 addInformation) — мост: GT6-тултип через addInformation → neo builder.
	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	public void appendHoverText(ItemStack aStack, net.minecraft.world.item.Item.TooltipContext aCtx, net.minecraft.world.item.component.TooltipDisplay aDisplay, java.util.function.Consumer<net.minecraft.network.chat.Component> aBuilder, net.minecraft.world.item.TooltipFlag aFlag) {
		Player tPlayer = gregapi.GT_API.api_proxy.getThePlayer();
		if (tPlayer == null) return;
		java.util.List tList = new java.util.ArrayList();
		try {addInformation(aStack, tPlayer, tList, aFlag.isAdvanced());} catch (Throwable e) {/**/}
		for (Object o : tList) if (o != null) aBuilder.accept(o instanceof net.minecraft.network.chat.Component tC ? tC : net.minecraft.network.chat.Component.literal(o.toString()));
	}

	// @Override
	@SuppressWarnings("unchecked")
	public void addInformation(ItemStack aStack, Player aPlayer, @SuppressWarnings("rawtypes") List aList, boolean aF3_H) {
		// F13 (1:1): GT6-тултип ПОДКЛЮЧЁН — appendHoverText (выше) зовёт этот addInformation. Снятый super.addInformation
		// в 1.7.10 vanilla был пустым (Item/ItemBlock его не наполняли) → мёртвый вызов, потери нет. Не заглушка.
		if (mBlock.mSpawnProof) aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_SPAWNPROOF));
		
		if (MD.GC.mLoaded) {
			if (mBlock.mPrefix == OP.blockSolid) {
				aList.add(LH.Chat.GREEN  + LH.get(LH.TOOLTIP_SEALABLE_ANY));
			} else if (WD.opaque(mBlock)) {
				aList.add(LH.Chat.ORANGE + LH.get(LH.TOOLTIP_SEALABLE_BUGGED));
			}
		}
		
		if (mBlock.mGravity) aList.add(LH.Chat.ORANGE + LH.get(LH.TOOLTIP_GRAVITY));
		OreDictMaterial aMaterial = mBlock.getMetaMaterial(getDamage(aStack));
		aList.add(LH.getToolTipBlastResistance(getBlock(), mBlock.mBaseResistance * (1+mBlock.getHarvestLevel(aMaterial==null?0:aMaterial.mToolQuality))));
		while (aList.remove(null));
	}
	
	@Override
	public OreDictItemData getOreDictItemData(ItemStack aStack) {
		if (mBlock.mPrefix != null && UT.Code.exists(ST.meta_(aStack), mBlock.mMaterialList)) return new OreDictItemData(mBlock.mPrefix, mBlock.mMaterialList[ST.meta_(aStack)]);
		return null;
	}
	
	@Override public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {updateItemStack(aStack);}
	@Override public void updateItemStack(ItemStack aStack) {
		if (mBlock.mMaterialList != OreDictMaterial.MATERIAL_ARRAY) return;
		int aMeta = ST.meta_(aStack);
		if (UT.Code.exists(aMeta, mBlock.mMaterialList)) {
			OreDictMaterial aMaterial = mBlock.mMaterialList[aMeta];
			if (aMeta != aMaterial.mTargetRegistration.mID) ST.meta_(aStack, aMaterial.mTargetRegistration.mID);
		}
	}
	
	@Optional.Method(modid = ModIDs.BOTA) public Block getBlockToPlaceByFlower(ItemStack aStack, SubTileEntity aFlower, int aX, int aY, int aZ) {return null;}
	@Optional.Method(modid = ModIDs.BOTA) public void onBlockPlacedByFlower(ItemStack aStack, SubTileEntity aFlower, int aX, int aY, int aZ) {/**/}
	
	public final String getUnlocalizedName() {return mBlock.getUnlocalizedName();}
	public String getItemStackDisplayName(ItemStack aStack) {return gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack));}
	// Ф1.2 (мис-порт): PrefixBlockItem extends ванильный BlockItem (не ItemBlockBase) → не наследовал GT6-мост имени.
	// getUnlocalizedName(ItemStack) выше уже даёт ключ "oredict."+prefix.dat(material) (имя зарегистрировано в PrefixBlock),
	// но ванильный BlockItem.getName его не звал → сырой ключ у крейтов/руд-в-камне/storage. Маршрут в GT6-имя, как ItemBlockBase:137.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	public final boolean hasContainerItem(ItemStack aStack) {return getContainerItem(aStack) != null;}
	public ItemStack getContainerItem(ItemStack aStack) {return null;}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	public void onCreated(ItemStack aStack, Level aWorld, Player aPlayer) {updateItemStack(aStack);}
	public boolean isBookEnchantable(ItemStack aStack, ItemStack aBook) {return F;}
	public boolean getIsRepairable(ItemStack aStack, ItemStack aMaterial) {return F;}
	public int getItemEnchantability() {return 0;}
	// BUG-021 v2: мост neo per-stack канала на 1.7.10-хук ниже (стак префикс-блоков = mDefaultStackSize, не 64).
	@Override public int getMaxStackSize(ItemStack aStack) {return UT.Code.bindStack(getItemStackLimit(aStack));}
	public int getItemStackLimit(ItemStack aStack) {return mBlock.mPrefix.mDefaultStackSize;}
	@Override public OreDictMaterial getMaterial(int aMetaData) {return UT.Code.exists(aMetaData, mBlock.mMaterialList) ? mBlock.mMaterialList[aMetaData] : null;}
	@Override public OreDictPrefix getPrefix(int aMetaData) {return mBlock.mPrefix;}
	/*
	@Override @Optional.Method(modid = ModIDs.TC) public void setAspects(ItemStack aStack, AspectList aAspectList) {}
	
	@Override @Optional.Method(modid = ModIDs.TC)
	public AspectList getAspects(ItemStack aStack) {
		List<TC_AspectStack> rAspects = new ArrayListNoNulls<TC_AspectStack>();
		for (TC_AspectStack tAspect : mBlock.mPrefix.mAspects) tAspect.addToAspectList(rAspects);
		OreDictMaterial aMaterial = (UT.Code.exists(aStack), mBlock.mMaterialList) ? mBlock.mMaterialList[ST.meta(aStack)] : null);
		if (aMaterial != null && (mBlock.mPrefix.mAmount >= U || mBlock.mPrefix.mAmount < 0)) for (TC_AspectStack tAspect : aMaterial.mAspects) tAspect.addToAspectList(rAspects);
		
		for (TC_AspectStack tAspect : rAspects) tAspect.mAmount = Math.min(10, tAspect.mAmount);
		return (AspectList)GT_API.sCompatTC.getAspectList(rAspects);
	}
	*/
}
