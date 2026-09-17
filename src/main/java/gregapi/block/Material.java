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

package gregapi.block;

/** 1.7.10 drove block behavior (light, piston push, burning, tool need) from this category, a concept neo replaced
 *  with properties and tags; GT6 still passes and queries it from 148 files, translated centrally in BlockBase.mkProps. */
public class Material {
	public static final int MOBILITY_NORMAL = 0, MOBILITY_NO_PUSH = 1, MOBILITY_IMMOVABLE = 2;

	private final MapColor mMapColor;
	private boolean mLiquid          = false;  // liquid, like water or lava
	private boolean mSolid           = true;   // counts as solid
	private boolean mBlocksMovement  = true;   // blocks movement
	private boolean mBlocksGrass     = true;   // kills grass underneath it
	private boolean mBurns           = false;  // flammable
	private boolean mReplaceable     = false;  // can be built over, like snow, grass, or vines
	private boolean mTranslucent     = false;  // translucent
	private boolean mToolNotRequired = true;   // harvestable without the correct tool
	private boolean mAdventureExempt = false;  // breakable even in adventure mode
	private int     mMobility        = MOBILITY_NORMAL;

	public Material(MapColor aMapColor) {mMapColor = aMapColor;}

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

	/** Opacity is derived, not stored: never opaque if translucent, otherwise based on whether it blocks movement. */
	public boolean isOpaque() {return !mTranslucent && mBlocksMovement;}

	// These setters return this so a category's declaration reads as one fluent line.
	public Material setLiquid             () {mLiquid = true; mSolid = false; mBlocksMovement = false; mReplaceable = true; mMobility = MOBILITY_NO_PUSH; return this;}
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
	// The categories GT6 uses, ordered by kinship rather than alphabetically so related families stay together.
	// ---------------------------------------------------------------------------------------------

	// void and portals
	public static final Material air     = new Material(MapColor.airColor  ).setNotSolid().setReplaceable();
	public static final Material portal  = new Material(MapColor.airColor  ).setNotSolid().setImmovableMobility();
	public static final Material fire    = new Material(MapColor.airColor  ).setNotSolid().setReplaceable().setNoPushMobility();

	// soils and stone
	public static final Material grass   = new Material(MapColor.grassColor);
	public static final Material ground  = new Material(MapColor.dirtColor );
	public static final Material sand    = new Material(MapColor.sandColor );
	public static final Material clay    = new Material(MapColor.clayColor );
	public static final Material rock    = new Material(MapColor.stoneColor).setRequiresTool();
	public static final Material piston  = new Material(MapColor.stoneColor).setImmovableMobility();

	// metals
	public static final Material iron    = new Material(MapColor.ironColor ).setRequiresTool();
	public static final Material anvil   = new Material(MapColor.ironColor ).setRequiresTool().setImmovableMobility();

	// wood and plants
	public static final Material wood    = new Material(MapColor.woodColor  ).setBurning();
	public static final Material leaves  = new Material(MapColor.foliageColor).setBurning().setTranslucent().setNoPushMobility();
	public static final Material plants  = new Material(MapColor.foliageColor).setNotSolid().setAdventureModeExempt().setNoPushMobility();
	public static final Material vine    = new Material(MapColor.foliageColor).setNotSolid().setAdventureModeExempt().setBurning().setNoPushMobility().setReplaceable();
	public static final Material cactus  = new Material(MapColor.foliageColor).setTranslucent().setNoPushMobility();
	public static final Material gourd   = new Material(MapColor.foliageColor).setNoPushMobility();
	public static final Material coral   = new Material(MapColor.foliageColor).setNoPushMobility();
	public static final Material dragonEgg = new Material(MapColor.foliageColor).setNoPushMobility();

	// liquids
	public static final Material water   = new Material(MapColor.waterColor).setLiquid().setNoPushMobility();
	public static final Material lava    = new Material(MapColor.tntColor  ).setLiquid().setNoPushMobility();

	// snow and ice
	public static final Material snow        = new Material(MapColor.snowColor).setNotSolid().setAdventureModeExempt().setReplaceable().setTranslucent().setRequiresTool().setNoPushMobility();
	public static final Material craftedSnow = new Material(MapColor.snowColor).setRequiresTool();
	public static final Material ice         = new Material(MapColor.iceColor ).setTranslucent().setAdventureModeExempt();
	public static final Material packedIce   = new Material(MapColor.iceColor ).setAdventureModeExempt();

	// cloth and soft materials
	public static final Material cloth   = new Material(MapColor.clothColor).setBurning();
	public static final Material carpet  = new Material(MapColor.clothColor).setNotSolid().setAdventureModeExempt().setBurning();
	public static final Material sponge  = new Material(MapColor.clothColor);
	public static final Material web     = new Material(MapColor.clothColor).setPassable().setRequiresTool().setNoPushMobility();

	// transparent and technical blocks
	public static final Material glass        = new Material(MapColor.airColor).setTranslucent().setAdventureModeExempt();
	public static final Material redstoneLight = new Material(MapColor.airColor).setAdventureModeExempt();
	public static final Material circuits     = new Material(MapColor.airColor).setNotSolid().setAdventureModeExempt().setNoPushMobility();
	public static final Material cake         = new Material(MapColor.airColor).setNoPushMobility();
	public static final Material tnt          = new Material(MapColor.tntColor).setBurning().setTranslucent();
}
