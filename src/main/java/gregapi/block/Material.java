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

package gregapi.block;

/**
 * КАТЕГОРИЯ БЛОКА в понятиях 1.7.10 («камень», «дерево», «жидкость», …), которыми оперирует GT6.
 *
 * <p><b>Зачем существует.</b> В 1.7.10 у блока была категория-материал, из которой движок выводил
 * поведение: тонет ли в ней свет, толкает ли поршень, горит ли, нужен ли инструмент. Целевой движок
 * это понятие убрал — поведение задаётся свойствами блока и тегами. GT6 же передаёт категорию в
 * конструкторы своих блоков из 148 файлов и опрашивает её. Поэтому категория остаётся здесь, а перевод
 * её в свойства движка централизован в {@code BlockBase.mkProps} (шов F9) — одно место на весь мод.</p>
 *
 * <p><b>Устройство.</b> Один класс с набором признаков вместо иерархии из пяти: подклассы различались
 * лишь тремя-четырьмя булевыми ответами и наружу не выступали (греп по дереву = 0 обращений к ним),
 * поэтому иерархия свёрнута в признаки. Значения признаков — наблюдаемое поведение оригинала; их
 * верность стережёт паритет: паспорт ванильных блоков и матрица инструментов сверяются с живым 1.7.10
 * и обязаны сходиться при любой правке этого файла.</p>
 *
 * <p><b>Подвижность</b> (реакция на поршень) кодируется как в оригинале: 0 — обычный блок,
 * 1 — толкать нельзя, но поршень проходит, 2 — поршень блокируется полностью.</p>
 */
public class Material {
	public static final int MOBILITY_NORMAL = 0, MOBILITY_NO_PUSH = 1, MOBILITY_IMMOVABLE = 2;

	private final MapColor mMapColor;
	private boolean mLiquid          = false;  // жидкость (вода, лава)
	private boolean mSolid           = true;   // считается плотным
	private boolean mBlocksMovement  = true;   // преграждает движение
	private boolean mBlocksGrass     = true;   // губит траву под собой
	private boolean mBurns           = false;  // горюч
	private boolean mReplaceable     = false;  // застраивается поверх (снег, трава, лоза)
	private boolean mTranslucent     = false;  // просвечивает
	private boolean mToolNotRequired = true;   // добывается без правильного инструмента
	private boolean mAdventureExempt = false;  // ломается даже в режиме приключения
	private int     mMobility        = MOBILITY_NORMAL;

	public Material(MapColor aMapColor) {mMapColor = aMapColor;}

	// --- признаки, опрашиваемые модом и мостом в движок ---
	public boolean isLiquid           () {return mLiquid;}
	public boolean isSolid            () {return mSolid;}
	public boolean blocksMovement     () {return mBlocksMovement;}
	public boolean getCanBlockGrass   () {return mBlocksGrass;}
	public boolean getCanBurn         () {return mBurns;}
	public boolean isReplaceable      () {return mReplaceable;}
	public boolean isTranslucent      () {return mTranslucent;}
	public boolean isToolNotRequired  () {return mToolNotRequired;}
	public boolean isAdventureModeExempt() {return mAdventureExempt;}
	public int     getMaterialMobility() {return mMobility;}
	public MapColor getMaterialMapColor() {return mMapColor;}

	/** Непрозрачность выводится, а не хранится: просвечивающий — никогда, иначе — по преграждению движения. */
	public boolean isOpaque() {return !mTranslucent && mBlocksMovement;}

	// --- настройка при объявлении категории (возвращают себя, чтобы объявление читалось одной строкой) ---
	/** Жидкость: не плотная, не преграждает движение, застраивается поверх, поршнем не толкается. */
	public Material setLiquid             () {mLiquid = true; mSolid = false; mBlocksMovement = false; mReplaceable = true; mMobility = MOBILITY_NO_PUSH; return this;}
	/** Не плотная категория: не преграждает движение и не губит траву под собой. */
	public Material setNotSolid           () {mSolid = false; mBlocksMovement = false; mBlocksGrass = false; return this;}
	public Material setPassable           () {mBlocksMovement = false; return this;}
	public Material setBurning            () {mBurns = true; return this;}
	public Material setReplaceable        () {mReplaceable = true; return this;}
	public Material setTranslucent        () {mTranslucent = true; return this;}
	public Material setRequiresTool       () {mToolNotRequired = false; return this;}
	public Material setAdventureModeExempt() {mAdventureExempt = true; return this;}
	public Material setNoPushMobility     () {mMobility = MOBILITY_NO_PUSH; return this;}
	public Material setImmovableMobility  () {mMobility = MOBILITY_IMMOVABLE; return this;}

	// ---------------------------------------------------------------------------------------------
	// Категории, которыми пользуется GT6. Порядок — по родству, а не по алфавиту: так видно семьи.
	// ---------------------------------------------------------------------------------------------

	// пустота и порталы
	public static final Material air     = new Material(MapColor.airColor  ).setNotSolid().setReplaceable();
	public static final Material portal  = new Material(MapColor.airColor  ).setNotSolid().setImmovableMobility();
	public static final Material fire    = new Material(MapColor.airColor  ).setNotSolid().setReplaceable().setNoPushMobility();

	// грунты и породы
	public static final Material grass   = new Material(MapColor.grassColor);
	public static final Material ground  = new Material(MapColor.dirtColor );
	public static final Material sand    = new Material(MapColor.sandColor );
	public static final Material clay    = new Material(MapColor.clayColor );
	public static final Material rock    = new Material(MapColor.stoneColor).setRequiresTool();
	public static final Material piston  = new Material(MapColor.stoneColor).setImmovableMobility();

	// металлы
	public static final Material iron    = new Material(MapColor.ironColor ).setRequiresTool();
	public static final Material anvil   = new Material(MapColor.ironColor ).setRequiresTool().setImmovableMobility();

	// древесина и растительность
	public static final Material wood    = new Material(MapColor.woodColor  ).setBurning();
	public static final Material leaves  = new Material(MapColor.foliageColor).setBurning().setTranslucent().setNoPushMobility();
	public static final Material plants  = new Material(MapColor.foliageColor).setNotSolid().setAdventureModeExempt().setNoPushMobility();
	public static final Material vine    = new Material(MapColor.foliageColor).setNotSolid().setAdventureModeExempt().setBurning().setNoPushMobility().setReplaceable();
	public static final Material cactus  = new Material(MapColor.foliageColor).setTranslucent().setNoPushMobility();
	public static final Material gourd   = new Material(MapColor.foliageColor).setNoPushMobility();
	public static final Material coral   = new Material(MapColor.foliageColor).setNoPushMobility();
	public static final Material dragonEgg = new Material(MapColor.foliageColor).setNoPushMobility();

	// жидкости
	public static final Material water   = new Material(MapColor.waterColor).setLiquid().setNoPushMobility();
	public static final Material lava    = new Material(MapColor.tntColor  ).setLiquid().setNoPushMobility();

	// снег и лёд
	public static final Material snow        = new Material(MapColor.snowColor).setNotSolid().setAdventureModeExempt().setReplaceable().setTranslucent().setRequiresTool().setNoPushMobility();
	public static final Material craftedSnow = new Material(MapColor.snowColor).setRequiresTool();
	public static final Material ice         = new Material(MapColor.iceColor ).setTranslucent().setAdventureModeExempt();
	public static final Material packedIce   = new Material(MapColor.iceColor ).setAdventureModeExempt();

	// ткань и мягкое
	public static final Material cloth   = new Material(MapColor.clothColor).setBurning();
	public static final Material carpet  = new Material(MapColor.clothColor).setNotSolid().setAdventureModeExempt().setBurning();
	public static final Material sponge  = new Material(MapColor.clothColor);
	public static final Material web     = new Material(MapColor.clothColor).setPassable().setRequiresTool().setNoPushMobility();

	// прозрачное и техническое
	public static final Material glass        = new Material(MapColor.airColor).setTranslucent().setAdventureModeExempt();
	public static final Material redstoneLight = new Material(MapColor.airColor).setAdventureModeExempt();
	public static final Material circuits     = new Material(MapColor.airColor).setNotSolid().setAdventureModeExempt().setNoPushMobility();
	public static final Material cake         = new Material(MapColor.airColor).setNoPushMobility();
	public static final Material tnt          = new Material(MapColor.tntColor).setBurning().setTranslucent();
}
