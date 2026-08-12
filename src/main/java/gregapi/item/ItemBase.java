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

package gregapi.item;

import net.neoforged.api.distmarker.Dist;
import gregapi.code.TagData;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class ItemBase extends Item implements IItemProjectile, IItemUpdatable, IItemGT, IItemNoGTOverride {
	protected ResourceLocation mIcon;
	protected final String mModID;
	protected final String mName, mTooltip;
	/** F16: 1.7.10 Item.setMaxStackSize мутировал поле итема. neo Item неизменяем (стек-размер — DataComponent),
	 *  но GT6-итем — свой класс, потому храним поле и переопределяем getMaxStackSize (штатная neo-точка override,
	 *  IItemExtension.getMaxStackSize(ItemStack)). Дефолт 64 = ванильный. См. центр ST.setMaxStackSize. */
	protected int mMaxStackSize = 64;
	public ItemBase setMaxStackSize(int aMaxStackSize) {mMaxStackSize = aMaxStackSize; return this;}
	@Override public int getMaxStackSize(ItemStack aStack) {return mMaxStackSize;}
	/** F12: 1.7.10 Item.setMaxDamage мутировал поле итема в рантайме. neo Item неизменяем (maxDamage — DataComponent,
	 *  задаётся Properties.durability при регистрации), но GT6-итем — свой класс, потому храним поле и переопределяем
	 *  getMaxDamage (штатная точка override, IItemExtension.getMaxDamage(ItemStack):311). Дефолт 0 = ванильный (нет прочности). */
	protected int mMaxDamage = 0;
	public ItemBase setMaxDamage(int aMaxDamage) {mMaxDamage = aMaxDamage; return this;}
	@Override public int getMaxDamage(ItemStack aStack) {return mMaxDamage;}
	/** F-item-property: 1.7.10 Item.setNoRepair() (canRepair=false) — запрет наковальня-ремонта. neo убрал простой
	 *  флаг-override (ремонт через isValidRepairItem+durability); GT6-инструменты имеют свою логику ремонта — храним флаг для неё. */
	protected boolean mNoRepair = false;
	public ItemBase setNoRepair() {mNoRepair = true; return this;}
	/** F3-render: 1.7.10 {@code Item.setFull3D()} взводил поле {@code bFull3D}, а {@code isFull3D()} его отдавал
	 *  (Item.java:577-580 recompSrc). Клиент читал этот флаг и держал предмет в руке ИНАЧЕ: «как рукоять» вместо
	 *  «плашмя» (RenderPlayer:353-374 / RenderBiped:287 / RenderWitch:92). BUG-112: в порте сеттер был no-op, флаг
	 *  негде было прочесть, и все наследники {@code GT_Tool_Item} (спреи, паяльник) лежали в руке как слиток.
	 *  Носитель различия в neo — ItemTransforms модели, их подставляет {@code GT6ItemModel.flatItemTransforms}. */
	protected boolean mFull3D = false;
	public ItemBase setFull3D() {mFull3D = true; return this;}
	public boolean isFull3D() {return mFull3D;}
	/** F1: 1.7.10 Item.setHasSubtypes(true)/getHasSubtypes() — реальный флаг подтипов (metadata), потребители
	 *  ЕСТЬ в этом же файле (addInformation/getUnlocalizedName ниже) и в MultiItem (extends ItemBase, вызывает
	 *  setHasSubtypes(T) в ctor, полагается на неунаследованный getUnlocalizedName(ItemStack) отсюда) — не может
	 *  быть no-op (иначе молчаливая потеря суффикса подтипа в имени). Тот же приём хранения поля, что mMaxStackSize/
	 *  mMaxDamage выше (IItemGT-дефолт no-op переопределён здесь реальным хранением). Дефолт F = ванильный. */
	protected boolean mHasSubtypes = F;
	public ItemBase setHasSubtypes(boolean aHasSubtypes) {mHasSubtypes = aHasSubtypes; return this;}
	public boolean getHasSubtypes() {return mHasSubtypes;}

	// F-useOn (канал сместился, как N1-установка): в 1.7.10 движок звал Item.onItemUseFirst → Block.onBlockActivated →
	// Item.onItemUse, ПКМ-в-воздух — Item.onItemRightClick. В neo та же цепь — ServerPlayerGameMode.useItemOn:
	// itemStack.onItemUseFirst(:388) → state.useItemOn(:395) → itemStack.useOn(:415); воздух — Item.use(:207). GT6-контракты
	// (onItemUse/onItemUseFirst из IItemGT; onItemRightClick ниже) движком не вызывались → канал MultiItem-поведений
	// (Behavior_Tool: ключ/отвёртка/лом/кусачки + износ doDamage) был сиротой. Мост центральный: ItemBase — корень всех
	// GT6-предметов (MultiItem/MultiItemTool/ItemFluidDisplay…); распаковка контекста единственная — IItemGT.bridgeUseOn*.
	@Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext aCtx) {return IItemGT.bridgeUseOn(this, aCtx);}
	@Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack aStack, net.minecraft.world.item.context.UseOnContext aCtx) {return IItemGT.bridgeUseOnFirst(this, aCtx);}
	/** 1.7.10-контракт Item.onItemRightClick (ПКМ-в-воздух; движок клал возврат обратно в руку). Дефолт — ванильный no-op
	 *  (возврат того же стека); MultiItem переопределяет диспатчем по behavior-списку. */
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return aStack;}
	@Override public net.minecraft.world.InteractionResult use(Level aWorld, Player aPlayer, net.minecraft.world.InteractionHand aHand) {
		ItemStack tStack = aPlayer.getItemInHand(aHand);
		ItemStack tResult = onItemRightClick(tStack, aWorld, aPlayer);
		if (tResult != tStack) {aPlayer.setItemInHand(aHand, tResult); return net.minecraft.world.InteractionResult.SUCCESS;}
		return super.use(aWorld, aPlayer, aHand);
	}

	/**
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public ItemBase(String aModID, String aUnlocalized, String aEnglish, String aEnglishTooltip) {
		// F1/F16: neo Item.<init> вычисляет descriptionId и требует ID в Properties (иначе "Item id not set"). Задаём ID из
		// (mModID, mName) — совпадает с DeferredRegister-именем регистрации (ITEMS.register(name, ...) на call-site). setId:660.
		super(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(aModID, aUnlocalized))));
		if (GAPI.mStartedInit) throw new IllegalStateException("Items can only be initialised within preInit!");
		mName = aUnlocalized;
		mModID = aModID;
		LH.add(mName, aEnglish);
		if (UT.Code.stringValid(aEnglishTooltip)) LH.add(mTooltip = mName + ".tooltip_main", aEnglishTooltip); else mTooltip = null;
		// F12: САМО-регистрация убрана из конструктора. Конструкция GT6-предмета теперь идёт на RegisterEvent (intrusive-holder
		// требует открытый реестр) через DeferredRegister-supplier `GT_API.ITEMS.register(name, ItemX::new)` на call-site —
		// а self-register ПОСЛЕ конструкции (на RegisterEvent) для DeferredRegister слишком поздно. Регистрация = обязанность
		// call-site (supplier declared в preInit, construct@RegisterEvent). ⚠️ КОНТЕНТ-предметы (MultiItem/GT_Tool_Item и др.
		// ItemBase-наследники) при порте контент-слоя ТОЖЕ перевести на supplier-паттерн (F12-followup). Было: ST.register(this, mName).
		DispenserBlock.registerBehavior(this, new GT_Item_Dispense()); // было dispenseBehaviorRegistry.putObject (DispenserBlock.java:61)
	}
	
	// F13 МОСТ ТУЛТИПА, поднят в КОРЕНЬ иерархии (2026-07-30, реестр мёртвых каналов). neo зовёт
	// appendHoverText, а не 1.7.10 addInformation. Мост существовал только у потомка MultiItem:262, поэтому
	// у самого ItemBase и остальных его наследников (ItemEmptySlot, ItemIntegratedCircuit, GT_Tool_Item)
	// подсказки до движка НЕ доходили вовсе: прочность, строка mTooltip и addAdditionalToolTips терялись.
	// Тело универсально — addInformation виртуален, поэтому каждый потомок отдаёт своё; копия в MultiItem снята.
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
		try {
			if (getMaxDamage(aStack) > 0 && !getHasSubtypes()) aList.add((aStack.getMaxDamage() - getDamage(aStack)) + " / " + aStack.getMaxDamage());
			if (mTooltip != null) aList.add(LanguageHandler.translate(mTooltip, mTooltip));
			addAdditionalToolTips(aList, aStack, aF3_H);
		} catch(Throwable e) {
			e.printStackTrace(ERR);
		}
		while (aList.remove(null));
	}
	
	protected void addAdditionalToolTips(List<String> aList, ItemStack aStack, boolean aF3_H) {
		//
	}
	
	public ItemStack onDispense(BlockSource aSource, ItemStack aStack) {
		Direction enumfacing = aSource.state().getValue(DispenserBlock.FACING); // было func_149937_b(getBlockMetadata()) -> BlockSource.state()/DispenserBlock.FACING, приём ItemArmorBase.java:187
		Position iposition = DispenserBlock.getDispensePosition(aSource);
		ItemStack itemstack1 = aStack.split(1);
		DefaultDispenseItemBehavior.spawnItem(aSource.level(), itemstack1, 6, enumfacing, iposition); // было doDispense -> spawnItem (DefaultDispenseItemBehavior.java:30)
		return aStack;
	}

	/** F13 (РЕАЛИЗОВАНО): 1.7.10 {@code extends BehaviorProjectileDispense} с {@code getProjectileEntity→null} было DEAD-кодом
	 *  (projectile-функциональность никогда не использовалась). neo {@code ProjectileDispenseBehavior} требует реального
	 *  {@code ProjectileItem} (этот Item им не является) → корректно сведено к {@code DefaultDispenseItemBehavior}. Не заглушка. */
	public static class GT_Item_Dispense extends DefaultDispenseItemBehavior {
		@Override
		protected ItemStack execute(BlockSource aSource, ItemStack aStack) {
			return ((ItemBase)aStack.getItem()).onDispense(aSource, aStack);
		}
	}
	
	@Override public boolean hasProjectile(TagData aProjectileType, ItemStack aStack) {return F;}
	@Override public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, double aX, double aY, double aZ) {return null;}
	@Override public EntityProjectile getProjectile(TagData aProjectileType, ItemStack aStack, Level aWorld, LivingEntity aEntity, float aSpeed) {return null;}
	public final Item setUnlocalizedName(String aName) {return this;}
	@Override public String toString() {return mName;}
	public final String getUnlocalizedName() {return mName;}
	public String getUnlocalizedName(ItemStack aStack) {return getHasSubtypes()?mName+"."+ST.meta_(aStack):mName;}
	public String getItemStackDisplayName(ItemStack aStack) {return gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack));}
	// LOCALIZATION-display: neo берёт имя через getName(ItemStack) (не 1.7.10 getItemStackDisplayName) — мост в GT6-имя
	// (LH.get). Без этого neo брал бы из vanilla-lang (куда BACKUPMAP не попадает) → показывались raw-ключи.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	// F1-контракт (1.7.10 itemDamage==meta): дословный GT6-код зовёт getDamage за подтипом; neo-дефолт (IItemExtension)
	// читает DAMAGE-компонент (0 у meta-предметов, у GT-инструментов прочность в NBT). Восстанавливаем на корне:
	// не-повреждаемый стек → subtype-мета (центр F1 ST.meta_). Реально-повреждаемые (getMaxDamage>0) — vanilla-ветка.
	@Override public int getDamage(ItemStack aStack) {return getMaxDamage(aStack) > 0 ? super.getDamage(aStack) : ST.meta_(aStack);}
	public final boolean getShareTag() {return T;} // just to be sure.
	// F3 superseded-render (GT6BlockModel/ItemModel пайплайн; старый getIcon/immediate-mode мёртв, 0 вызовов neo): было aIconRegister.registerIcon(mModID+":"+mName) (IIconRegister удалён) — ResourceLocation строим напрямую из того же пути.
	public void registerIcons(Object aIconRegister) {mIcon = ResourceLocation.parse((mModID + ":" + mName).toLowerCase(java.util.Locale.ROOT));}
	// F3-render: registerIcons (1.7.10 IIconRegister-хук) в neo НЕ вызывается → mIcon оставался null → GT6ItemModel.resolveIcon
	// возвращал null → предмет не рисовался (пусто/пурпур). Строим mIcon ЛЕНИВО при первом запросе (тот же путь "modid:name").
	public ResourceLocation getIconFromDamage(int aMeta) {if (mIcon == null) registerIcons(null); return mIcon;}
	public void onCreated(ItemStack aStack, Level aWorld, Player aPlayer) {isItemStackUsable(aStack);}
	public ItemStack getContainerItem(ItemStack aStack) {return null;}
	public boolean hasContainerItem(ItemStack aStack) {return getContainerItem(aStack) != null;}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	@Override public void updateItemStack(ItemStack aStack) {isItemStackUsable(aStack);}
	@Override public void updateItemStack(ItemStack aStack, Level aWorld, int aX, int aY, int aZ) {updateItemStack(aStack);}
	public boolean doesSneakBypassUse(Level aWorld, int aX, int aY, int aZ, Player aPlayer) {return T;}
	public boolean isItemStackUsable(ItemStack aStack) {return T;}
	public ItemStack make(long aMetaData) {return ST.make(this, 1, aMetaData);}
	public ItemStack make(long aAmount, long aMetaData) {return ST.make(this, aAmount, aMetaData);}
}
