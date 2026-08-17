/**
 * Copyright (c) 2026 wolfram0108
 *
 * Written in 2026 for the GregTech 6 NeoForge port
 * (https://github.com/wolfram0108/gregtech6_w). Not part of the original GregTech 6
 * by Gregorius Techneticies; distributed under the same licence as the work it extends.
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

package gregapi.code;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * F8 — центральный мост ItemStack↔NBT (см. `decisions/F8-nbt-data-components.md`).
 * В 1.7.10 «предметный NBT» был сырым mutable-полем `NBTTagCompound` прямо на `ItemStack`
 * (`getTagCompound`/`setTagCompound`/`hasTagCompound`). В 26.1.2 канал был заменён на
 * `DataComponents.CUSTOM_DATA` (иммутабельная обёртка `CustomData`) — отсюда и брался этот мост.
 * <p>
 * В 1.20.1 сырой тег предмета живёт снова на самом стеке — `ItemStack.getTag()/getOrCreateTag()/
 * setTag()/hasTag()` (`forge-1201-decompiled/net/minecraft/world/item/ItemStack.java:507,512,516`),
 * то есть ровно как в оригинале. Мост сохранён (весь мод ходит через него — один центр), но тело
 * сведено к форме 1.7.10.
 * <p>
 * `getTag()` отдаёт ЖИВОЙ объект тега стека (а не копию, как `CustomData.copyTag()`), и там, где
 * подтипа в теге нет, {@link #get} отдаёт его как есть — прямая мутация сохраняется сама, как в
 * 1.7.10. Там, где подтип в теге есть (все meta-предметы GT6), {@link #get} вынужден отдать
 * отцепленную копию без него (см. ниже), то есть требование ADR §3-bis «мутировал → закоммить
 * через {@link #set}» остаётся в силе ровно в том же объёме, что на 26.1.2; все commit-вызовы по
 * дереву уже на месте, потому ни одно место вызова не правится.
 * <p>
 * Семантика 1:1 c 1.7.10 сохранена: {@link #get} отдаёт null, если тега нет или он пуст;
 * {@link #set} с null/пустым тегом снимает тег (нормализация GT6 — `UT.NBT.set` всегда стрипает
 * пустые теги); {@link #has} — есть непустой тег.
 *
 * <h2>ПОДТИП ПРЕДМЕТА В ЭТОМ ЖЕ ТЕГЕ — граница, которую держит этот центр</h2>
 * В 1.7.10 подтип (он же «мета», {@code ItemStack.itemDamage}) был ОТДЕЛЬНЫМ полем стека и в
 * сериализации лежал РЯДОМ с тегом:{@code {id,Count,Damage:5s,tag:{…}}} — «предметный NBT» его
 * никогда не содержал, и {@code setTagCompound(null)} подтип не трогал
 * ({@code gt6-original/…/ItemStack.java} поле {@code itemDamage}; снятие тега —
 * {@code gt6-original/…/oredict/OreDictManager.java:526}).
 * <p>
 * В 1.20.1 отдельного поля нет: движок держит подтип ВНУТРИ того же тега под ключом
 * {@link ItemStack#TAG_DAMAGE} — {@code IForgeItem.getDamage} читает
 * {@code stack.getTag().getInt("Damage")} ({@code IForgeItem.java:435-438}),
 * {@code IForgeItem.setDamage} пишет {@code stack.getOrCreateTag().putInt("Damage", …)}
 * ({@code IForgeItem.java:472-475}), а {@code ItemStack.setTag(null)}
 * ({@code ItemStack.java:553}) сносит тег целиком — вместе с подтипом.
 * <p>
 * <b>Отсюда класс дефекта:</b> любая ЦЕЛИКОВАЯ запись/снятие тега (а весь мод делает её только
 * здесь — единственный {@code setTag} в дереве) на 1.20.1 стирала подтип, которого в 1.7.10 в
 * теге не было. Носители по дереву — 41 вызов {@link #set}; в том числе
 * {@code OreDictManager.setTarget_} (снятие тега перед регистрацией цели унификации),
 * {@code OreDictManager.getStack_} и {@code ST.set} (перенос тега между стеками),
 * {@code ST.load} (тег из сейва), {@code ItemFluidDisplay.setFluid} (тег вместо прежнего).
 * Второе лицо того же класса — не запись, а ЧТЕНИЕ: «есть ли у стека NBT» ({@link #has},
 * {@code ST.hasNBT}) на 1.20.1 отвечало «да» любому meta-предмету, потому что подтип физически
 * лежит в теге. Отсюда, например, {@code Recipe.mNoNBTChecks} ({@code Recipe.java:964}, 1:1 с
 * оригиналом {@code Recipe.java:946}) получался {@code false} там, где оригинал даёт {@code true}.
 * <p>
 * <b>Тем же классом накрыт ВТОРОЙ эмулированный признак стека — «ноль с памятью типа»</b>
 * ({@code CS.NBT_ZEROSIZE}, центр {@code ST.size_}): в 1.7.10 это поле {@code stackSize == 0},
 * в 1.20.1 — маркер в том же теге. Поэтому список ключей ниже, а не один ключ.
 * <p>
 * <b>Адаптация — здесь, один раз, на весь мод:</b> «предметный NBT» в смысле 1.7.10 =
 * тег стека МИНУС ключи, которыми 1.20.1 эмулирует ПОЛЯ стека. {@link #get} их не показывает
 * (тег из одних лишь полей = «тега нет»), {@link #has} их не считает, {@link #set} их СОХРАНЯЕТ
 * и не даёт приехать с чужим тегом. Сами поля читаются и пишутся своими центрами и мимо этого
 * представления: подтип — {@code ST.meta_} ({@code getDamageValue}/{@code setDamageValue}),
 * ноль-с-памятью — {@code ST.zerosize} (через {@link #field} здесь же). Каналы не пересекаются.
 * <p>
 * Одноимённый ключ ВНУТРИ суб-компаунда (напр. {@code GT.ToolStats.Damage},
 * {@code MultiItemTool.java:441}) — не поле стека и не затрагивается: фильтруется только корень тега.
 *
 * @author Gregorius Techneticies
 */
public final class ItemNBT {
	private ItemNBT() {/**/}

	/**
	 * Ключи корня тега, которыми 1.20.1 эмулирует ПОЛЯ стека 1.7.10 — «предметным NBT» они не являются.
	 * {@code Damage} = {@code ItemStack.itemDamage} (подтип/прочность), {@code gt.zerosize} =
	 * {@code stackSize == 0}. Оба — компайл-тайм константы, класс-инициализации не тянут.
	 */
	private static final String[] FIELDS = {ItemStack.TAG_DAMAGE, gregapi.data.CS.NBT_ZEROSIZE};

	/** Тег, в котором кроме эмулированных полей стека ничего нет, в терминах 1.7.10 = «тега нет». */
	private static boolean fieldsOnly(CompoundTag aNBT) {
		if (aNBT == null || aNBT.isEmpty()) return true;
		int tFields = 0;
		for (String tKey : FIELDS) if (aNBT.contains(tKey)) tFields++;
		return aNBT.size() == tFields;
	}

	private static boolean hasField(CompoundTag aNBT) {
		if (aNBT == null) return false;
		for (String tKey : FIELDS) if (aNBT.contains(tKey)) return true;
		return false;
	}

	public static CompoundTag get(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return null;
		CompoundTag rNBT = aStack.getTag();
		if (fieldsOnly(rNBT)) return null;
		// Полей стека в теге нет — отдаём ЖИВОЙ тег, как 1.7.10 (мутация сохраняется сама).
		if (!hasField(rNBT)) return rNBT;
		// Поля есть — в «предметном NBT» их быть не должно: отдаём отцепленную копию без них
		// (та же форма, что мост давал на 26.1.2, потому все commit-вызовы по дереву уже на месте).
		rNBT = rNBT.copy();
		for (String tKey : FIELDS) rNBT.remove(tKey);
		return rNBT;
	}

	public static void set(ItemStack aStack, CompoundTag aNBT) {
		if (aStack == null || aStack.isEmpty()) return;
		CompoundTag tOld = aStack.getTag();
		CompoundTag rNBT = aNBT;
		// Чужие поля стека с тегом не переезжают: их задают только свои центры (ST.meta_/ST.size_).
		if (rNBT != null && rNBT != tOld) for (String tKey : FIELDS) rNBT.remove(tKey);
		boolean tFields = hasField(tOld);
		// Нечего хранить и полей нет — тега нет (нормализация 1.7.10: пустой тег GT6 не держит).
		if ((rNBT == null || rNBT.isEmpty()) && !tFields) {aStack.setTag(null); return;}
		// Поля есть — тег обязан существовать, и им становится ПЕРЕДАННЫЙ объект, а не свежий:
		// оригинал ставил тег и мутировал ТОТ ЖЕ объект дальше (1.7.10 setTagCompound —
		// gt6-original/…/recipes/maps/RecipeMapBath.java:144-146), эта идиома должна работать.
		if (rNBT == null) rNBT = new CompoundTag();
		if (tFields && rNBT != tOld) for (String tKey : FIELDS) {
			net.minecraft.nbt.Tag tField = tOld.get(tKey);
			if (tField != null) rNBT.put(tKey, tField);
		}
		aStack.setTag(rNBT);
	}

	public static boolean has(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		return !fieldsOnly(aStack.getTag());
	}

	/**
	 * Чтение эмулированного ПОЛЯ стека (не «предметного NBT») — мимо фильтра {@link #get}.
	 * Единственный вызыватель — центр поля; сюда ходят только за ключами из {@link #FIELDS}.
	 */
	public static boolean field(ItemStack aStack, String aKey) {
		if (aStack == null || aStack.isEmpty()) return false;
		CompoundTag tNBT = aStack.getTag();
		return tNBT != null && tNBT.getBoolean(aKey);
	}

	/**
	 * Запись эмулированного ПОЛЯ стека; снятое поле не оставляет за собой пустого тега (1.7.10-норма).
	 *
	 * <p>{@code aFlag=F} означает «ПОЛЯ НЕТ» и годится любому ключу из {@link #FIELDS}, а не только
	 * булеву: у числового поля «нет» — это его дефолт, то есть 0 (в 1.7.10 {@code itemDamage == 0} и
	 * {@code stackSize == 0} были дефолтами ПОЛЕЙ, а не записями в предметном NBT). Этой веткой ходит
	 * {@code ST.meta_} при мете 0 — иначе {@code IForgeItem.setDamage} оставил бы тег {@code {Damage:0}},
	 * и стек перестал бы складываться с таким же предметом, добытым обычным путём.
	 */
	public static void field(ItemStack aStack, String aKey, boolean aFlag) {
		if (aStack == null || aStack.isEmpty()) return;
		if (aFlag) {aStack.getOrCreateTag().putBoolean(aKey, true); return;}
		CompoundTag tNBT = aStack.getTag();
		if (tNBT == null || !tNBT.contains(aKey)) return;
		tNBT.remove(aKey);
		if (tNBT.isEmpty()) aStack.setTag(null);
	}
}
