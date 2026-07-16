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

package gregapi.block;
import gregapi.util.WD;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import gregapi.data.LH;
import gregapi.data.MD;
import gregapi.item.IItemGT;
import gregapi.render.ITexture;
import gregapi.util.ST;
import gregapi.util.UT;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import java.util.List;

import static gregapi.data.CS.*;

public class ItemBlockBase extends BlockItem implements IBlock, IItemGT {
	public final IBlockBase mPlaceable;

	public ItemBlockBase(Block aBlock) {
		// F12-followup (item-split): neo Item требует id в Properties (иначе «Item id not set»); BlockItem делит id с блоком —
		// производим из ключа уже-зарегистрированного блока (конструкция item идёт на RegisterEvent<Item>, после блока).
		super(aBlock, new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(aBlock))));
		mPlaceable = (IBlockBase)aBlock;
		setMaxDamage(0);
		setHasSubtypes(T);
	}

	// F-bounds: ItemBlockBase - обёртка над Block (mPlaceable/getBlock()), собственной геометрии не хранит ->
	// маршрутизирует на ЦЕНТР WD.setBlockBounds (уже существует, gregapi/util/WD.java:122), тот же приём, что и
	// весь остальной F-bounds шов.
	@Override public void setBlockBounds(float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ) {
		WD.setBlockBounds(getBlock(), aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ);
	}
	
	// F13: neo BlockItem зовёт appendHoverText (не 1.7.10 addInformation) — мост: собираем GT6-тултип (List<String>) через
	// addInformation ниже, отдаём в neo builder как Component. Player — из клиент-прокси (getThePlayer, null на сервере → пропуск).
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
		// F13: GT6-тултип (1.7.10 addInformation-стиль); движок зовёт через appendHoverText-мост выше.
		byte aMeta = UT.Code.bind4(ST.meta_(aStack));
		mPlaceable.addInformation(aStack, aMeta, aPlayer, aList, aF3_H);
		if (WD.hasCollide(aPlayer.level(), 0, 0, 0, getBlock())) {
			if (mPlaceable.doesWalkSpeed(aMeta)) aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_WALKSPEED));
			if (mPlaceable.canCreatureSpawn(aMeta)) {
				if (ITexture.Util.OPTIFINE_LOADED && aMeta != 0 && !mPlaceable.canCreatureSpawn((byte)0)) {
					aList.add(LH.Chat.BLINKING_RED + LH.get(Minecraft.getInstance().isSingleplayer() ? LH.TOOLTIP_SPAWNPROOF_SP_BUG    : LH.TOOLTIP_SPAWNPROOF_MP_BUG   ));
					aList.add(LH.Chat.BLINKING_RED + LH.get(LH.TOOLTIP_SPAWNPROOF_OPTIFINE));
				}
			} else {
				if (ITexture.Util.OPTIFINE_LOADED && aMeta != 0 &&  mPlaceable.canCreatureSpawn((byte)0)) {
					aList.add(LH.Chat.BLINKING_RED + LH.get(Minecraft.getInstance().isSingleplayer() ? LH.TOOLTIP_SPAWNPROOF_SP_BROKEN : LH.TOOLTIP_SPAWNPROOF_MP_BROKEN));
					aList.add(LH.Chat.BLINKING_RED + LH.get(LH.TOOLTIP_SPAWNPROOF_OPTIFINE));
				} else {
					aList.add(LH.Chat.CYAN + LH.get(LH.TOOLTIP_SPAWNPROOF));
				}
			}
			if (MD.GC.mLoaded) {
				byte tCount = 0;
				for (byte tSide : ALL_SIDES_VALID) if (mPlaceable.isSealable(aMeta, tSide)) tCount++;
				if (tCount >= 6) {
					aList.add(LH.Chat.GREEN  + LH.get(LH.TOOLTIP_SEALABLE_ANY));
				} else if (WD.opaque(getBlock())) {
					aList.add(LH.Chat.ORANGE + LH.get(LH.TOOLTIP_SEALABLE_BUGGED));
				} else if (tCount > 0) {
					aList.add(LH.Chat.YELLOW + LH.get(LH.TOOLTIP_SEALABLE_SOME));
				}
			}
		}
		if (mPlaceable.useGravity(aMeta))
			aList.add(LH.Chat.ORANGE + LH.get(LH.TOOLTIP_GRAVITY));
		if (mPlaceable.doesPistonPush(aMeta))
			aList.add(LH.Chat.DGRAY + LH.get(LH.TOOLTIP_PISTONPUSHABLE));
		if (mPlaceable.isFlammable(aMeta) || mPlaceable.isFireSource(aMeta) || mPlaceable.getFlammability(aMeta) > 0)
			aList.add(LH.Chat.RED + LH.get(LH.TOOLTIP_FLAMMABLE));
		float tResistance = mPlaceable.getExplosionResistance(aMeta);
		if (tResistance >= 4) aList.add(LH.getToolTipBlastResistance(getBlock(), tResistance));
		
		// F-tool: Block.getHarvestTool/getHarvestLevel удалены из vanilla neo (getBlock() статически — vanilla Block).
		// GT6-данные живут на BlockBase (getHarvestTool/getHarvestLevel:87-88) — маршрут через каст (путь ЕСТЬ, не заглушка).
		String tHarvestTool = TOOL_pickaxe; int tHarvestLevel = 0;
		if (getBlock() instanceof gregapi.block.BlockBase tBB) {tHarvestTool = tBB.getHarvestTool(aMeta); tHarvestLevel = tBB.getHarvestLevel(aMeta);}
		aList.add(LH.getToolTipHarvest(WD.getMaterial(getBlock()), tHarvestTool, tHarvestLevel));
		while (aList.remove(null));
	}
	
	// F16 dead-interface: getCreativeTab движком neo НЕ вызывается (per-block getter удалён; вкладки — event-based).
	// Членство блока во вкладке подключено централизованно: BlockBase ctor → CreativeTabsGT.assign(...). Метод мёртв (0 вызовов).
	@OnlyIn(Dist.CLIENT) public CreativeModeTab getCreativeTab() {return null;}
	public boolean func_150936_a(Level aWorld, int aX, int aY, int aZ, int aSide, Player aPlayer, ItemStack aStack) {return T;}
	public boolean onItemUseFirst(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return mPlaceable.onItemUseFirst(this, aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ);}
	public boolean onItemUse(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ) {return mPlaceable.onItemUse(this, aStack, aPlayer, aWorld, aX, aY, aZ, aSide, aHitX, aHitY, aHitZ);}
	// PORT-TODO(F3, baked-рендер клиента): было getBlock().getIcon(SIDE_TOP,aMeta) (vanilla Block.getIcon удалён в 26.1.2 —
	// getBlock() статически типизирован как vanilla Block, не как наша BlockBase, поэтому центр IIconContainer недоступен здесь).
	public Identifier getIconFromDamage(int aMeta) {throw new UnsupportedOperationException("PORT-TODO(F3): neo BakedModel-рендер, 1.7.10 getIconFromDamage мёртв — crash-only per /goal (F3-фаза заменит моделью)");}
	@Override public Block getBlock() {return super.getBlock();}
	public boolean doesContainerItemLeaveCraftingGrid(ItemStack aStack) {return F;}
	public String getUnlocalizedName(ItemStack aStack) {return mPlaceable.name(UT.Code.bind4(getDamage(aStack)));}
	public String getItemStackDisplayName(ItemStack aStack) {return gregapi.lang.LanguageHandler.get(getUnlocalizedName(aStack));}
	// LOCALIZATION-display: neo getName(ItemStack) → GT6-имя (LH.get); иначе raw-ключ из vanilla-lang.
	@Override public net.minecraft.network.chat.Component getName(ItemStack aStack) {String s = getItemStackDisplayName(aStack); return s != null && !s.isEmpty() ? net.minecraft.network.chat.Component.literal(s) : super.getName(aStack);}
	public boolean placeBlockAt(ItemStack aStack, Player aPlayer, Level aWorld, int aX, int aY, int aZ, int aSide, float aHitX, float aHitY, float aHitZ, int aMetaData) {return WD.set(aWorld, aX, aY, aZ, getBlock(), aMetaData, 3);}
	public int getItemStackLimit(ItemStack aStack) {return mPlaceable.getItemStackLimit(aStack);}
	public int getMetadata(int aMeta) {return aMeta;}
	public ItemStack onItemRightClick(ItemStack aStack, Level aWorld, Player aPlayer) {return mPlaceable.onItemRightClick(aStack, aWorld, aPlayer);}
}
