package gregapi.block;

/**
 * Переходник (PIVOT-6, подсистема BLOCK-MATERIAL). Дословная копия ванильного
 * net.minecraft.block.material.Material (Minecraft 1.7.10, decompiled recompSrc), который целевой движок
 * удалил (роль «материала блока» ушла в BlockBehaviour.Properties + теги). GT6 ссылается на этот тип из
 * 148 файлов (27 — ядро): передаёт Material.rock/wood/iron/… в конструкторы блоков и читает
 * isLiquid()/isSolid()/getCanBurn()/blocksMovement().
 *
 * 1:1 с оригиналом (34 именованных материала, все поля и методы). Убраны только decompiler-артефакты
 * __OBFID. Мост в движок (Material → BlockBehaviour.Properties) — отдельным шагом, см.
 * decisions/F9-block-material.md.
 */
public class Material {
	public static final Material air = new MaterialTransparent(MapColor.airColor);
	public static final Material grass = new Material(MapColor.grassColor);
	public static final Material ground = new Material(MapColor.dirtColor);
	public static final Material wood = (new Material(MapColor.woodColor)).setBurning();
	public static final Material rock = (new Material(MapColor.stoneColor)).setRequiresTool();
	public static final Material iron = (new Material(MapColor.ironColor)).setRequiresTool();
	public static final Material anvil = (new Material(MapColor.ironColor)).setRequiresTool().setImmovableMobility();
	public static final Material water = (new MaterialLiquid(MapColor.waterColor)).setNoPushMobility();
	public static final Material lava = (new MaterialLiquid(MapColor.tntColor)).setNoPushMobility();
	public static final Material leaves = (new Material(MapColor.foliageColor)).setBurning().setTranslucent().setNoPushMobility();
	public static final Material plants = (new MaterialLogic(MapColor.foliageColor)).setNoPushMobility();
	public static final Material vine = (new MaterialLogic(MapColor.foliageColor)).setBurning().setNoPushMobility().setReplaceable();
	public static final Material sponge = new Material(MapColor.clothColor);
	public static final Material cloth = (new Material(MapColor.clothColor)).setBurning();
	public static final Material fire = (new MaterialTransparent(MapColor.airColor)).setNoPushMobility();
	public static final Material sand = new Material(MapColor.sandColor);
	public static final Material circuits = (new MaterialLogic(MapColor.airColor)).setNoPushMobility();
	public static final Material carpet = (new MaterialLogic(MapColor.clothColor)).setBurning();
	public static final Material glass = (new Material(MapColor.airColor)).setTranslucent().setAdventureModeExempt();
	public static final Material redstoneLight = (new Material(MapColor.airColor)).setAdventureModeExempt();
	public static final Material tnt = (new Material(MapColor.tntColor)).setBurning().setTranslucent();
	public static final Material coral = (new Material(MapColor.foliageColor)).setNoPushMobility();
	public static final Material ice = (new Material(MapColor.iceColor)).setTranslucent().setAdventureModeExempt();
	public static final Material packedIce = (new Material(MapColor.iceColor)).setAdventureModeExempt();
	public static final Material snow = (new MaterialLogic(MapColor.snowColor)).setReplaceable().setTranslucent().setRequiresTool().setNoPushMobility();
	/** The material for crafted snow. */
	public static final Material craftedSnow = (new Material(MapColor.snowColor)).setRequiresTool();
	public static final Material cactus = (new Material(MapColor.foliageColor)).setTranslucent().setNoPushMobility();
	public static final Material clay = new Material(MapColor.clayColor);
	public static final Material gourd = (new Material(MapColor.foliageColor)).setNoPushMobility();
	public static final Material dragonEgg = (new Material(MapColor.foliageColor)).setNoPushMobility();
	public static final Material portal = (new MaterialPortal(MapColor.airColor)).setImmovableMobility();
	public static final Material cake = (new Material(MapColor.airColor)).setNoPushMobility();
	public static final Material web = (new Material(MapColor.clothColor) {
		public boolean blocksMovement() {
			return false;
		}
	}).setRequiresTool().setNoPushMobility();
	/** Pistons' material. */
	public static final Material piston = (new Material(MapColor.stoneColor)).setImmovableMobility();
	/** Bool defining if the block can burn or not. */
	private boolean canBurn;
	/**
	 * Determines whether blocks with this material can be "overwritten" by other blocks when placed - eg snow, vines
	 * and tall grass.
	 */
	private boolean replaceable;
	/** Indicates if the material is translucent */
	private boolean isTranslucent;
	/** The color index used to draw the blocks of this material on maps. */
	private final MapColor materialMapColor;
	/** Determines if the material can be harvested without a tool (or with the wrong tool) */
	private boolean requiresNoTool = true;
	/**
	 * Mobility information flag. 0 indicates that this block is normal, 1 indicates that it can't push other blocks, 2
	 * indicates that it can't be pushed.
	 */
	private int mobilityFlag;
	private boolean isAdventureModeExempt;

	public Material(MapColor aColor) {
		this.materialMapColor = aColor;
	}

	/**
	 * Returns if blocks of these materials are liquids.
	 */
	public boolean isLiquid() {
		return false;
	}

	public boolean isSolid() {
		return true;
	}

	/**
	 * Will prevent grass from growing on dirt underneath and kill any grass below it if it returns true
	 */
	public boolean getCanBlockGrass() {
		return true;
	}

	/**
	 * Returns if this material is considered solid or not
	 */
	public boolean blocksMovement() {
		return true;
	}

	/**
	 * Marks the material as translucent
	 */
	public Material setTranslucent() {
		this.isTranslucent = true;
		return this;
	}

	/**
	 * Makes blocks with this material require the correct tool to be harvested.
	 */
	public Material setRequiresTool() {
		this.requiresNoTool = false;
		return this;
	}

	/**
	 * Set the canBurn bool to True and return the current object.
	 */
	public Material setBurning() {
		this.canBurn = true;
		return this;
	}

	/**
	 * Returns if the block can burn or not.
	 */
	public boolean getCanBurn() {
		return this.canBurn;
	}

	/**
	 * Sets {@link #replaceable} to true.
	 */
	public Material setReplaceable() {
		this.replaceable = true;
		return this;
	}

	/**
	 * Returns whether the material can be replaced by other blocks when placed - eg snow, vines and tall grass.
	 */
	public boolean isReplaceable() {
		return this.replaceable;
	}

	/**
	 * Indicate if the material is opaque
	 */
	public boolean isOpaque() {
		return this.isTranslucent ? false : this.blocksMovement();
	}

	/**
	 * Returns true if the material can be harvested without a tool (or with the wrong tool)
	 */
	public boolean isToolNotRequired() {
		return this.requiresNoTool;
	}

	/**
	 * Returns the mobility information of the material, 0 = free, 1 = can't push but can move over, 2 = total
	 * immobility and stop pistons.
	 */
	public int getMaterialMobility() {
		return this.mobilityFlag;
	}

	/**
	 * This type of material can't be pushed, but pistons can move over it.
	 */
	public Material setNoPushMobility() {
		this.mobilityFlag = 1;
		return this;
	}

	/**
	 * This type of material can't be pushed, and pistons are blocked to move.
	 */
	public Material setImmovableMobility() {
		this.mobilityFlag = 2;
		return this;
	}

	/**
	 * @see #isAdventureModeExempt()
	 */
	public Material setAdventureModeExempt() {
		this.isAdventureModeExempt = true;
		return this;
	}

	/**
	 * Returns true if blocks with this material can always be mined in adventure mode.
	 */
	public boolean isAdventureModeExempt() {
		return this.isAdventureModeExempt;
	}

	public MapColor getMaterialMapColor() {
		return this.materialMapColor;
	}
}
