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

package gregapi.item;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import gregapi.code.TagData;
import gregapi.data.LH;
import gregapi.lang.LanguageHandler;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

/**
 * @author Gregorius Techneticies
 */
public class ItemBase extends Item implements IItemProjectile, IItemUpdatable, IItemGT, IItemNoGTOverride {
	protected Identifier mIcon;
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
	/** F1: 1.7.10 Item.setHasSubtypes(true)/getHasSubtypes() — реальный флаг подтипов (metadata), потребители
	 *  ЕСТЬ в этом же файле (addInformation/getUnlocalizedName ниже) и в MultiItem (extends ItemBase, вызывает
	 *  setHasSubtypes(T) в ctor, полагается на неунаследованный getUnlocalizedName(ItemStack) отсюда) — не может
	 *  быть no-op (иначе молчаливая потеря суффикса подтипа в имени). Тот же приём хранения поля, что mMaxStackSize/
	 *  mMaxDamage выше (IItemGT-дефолт no-op переопределён здесь реальным хранением). Дефолт F = ванильный. */
	protected boolean mHasSubtypes = F;
	public ItemBase setHasSubtypes(boolean aHasSubtypes) {mHasSubtypes = aHasSubtypes; return this;}
	public boolean getHasSubtypes() {return mHasSubtypes;}

	/**
	 * @param aUnlocalized The unlocalised Name of this Item. DO NOT START YOUR UNLOCALISED NAME WITH "gt."!!!
	 */
	public ItemBase(String aModID, String aUnlocalized, String aEnglish, String aEnglishTooltip) {
		super(new Item.Properties()); // было super() (neo Item требует Properties; mMaxStackSize/mMaxDamage override покрывают то, что раньше ставили setMaxStackSize/setMaxDamage)
		if (GAPI.mStartedInit) throw new IllegalStateException("Items can only be initialised within preInit!");
		mName = aUnlocalized;
		mModID = aModID;
		LH.add(mName, aEnglish);
		if (UT.Code.stringValid(aEnglishTooltip)) LH.add(mTooltip = mName + ".tooltip_main", aEnglishTooltip); else mTooltip = null;
		ST.register(this, mName);
		DispenserBlock.registerBehavior(this, new GT_Item_Dispense()); // было dispenseBehaviorRegistry.putObject (DispenserBlock.java:61)
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

	/** PORT-TODO(F13, item-base компонентный редизайн): было {@code extends BehaviorProjectileDispense} с
	 *  {@code getProjectileEntity(...)→null} (1.7.10, dead code — projectile-функциональность никогда не
	 *  использовалась). Neo {@code ProjectileDispenseBehavior} — конкретный класс, требующий реального
	 *  {@code ProjectileItem} в конструкторе (ProjectileDispenseBehavior.java:16), этот Item им не является —
	 *  сведено к {@code DefaultDispenseItemBehavior}, приём уже принят {@code ItemArmorBase.java:203-208}. */
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
	public final boolean getShareTag() {return T;} // just to be sure.
	// PORT-TODO(F3, baked-рендер клиента): было aIconRegister.registerIcon(mModID+":"+mName) (IIconRegister удалён) — Identifier строим напрямую из того же пути.
	@OnlyIn(Dist.CLIENT) public void registerIcons(Object aIconRegister) {mIcon = Identifier.parse(mModID + ":" + mName);}
	public Identifier getIconFromDamage(int aMeta) {return mIcon;}
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
