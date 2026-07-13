package ic2.api.crops;

/** F10 ЗЕРКАЛО (compile-only) чужого API. Сверено javap ic2:IC2Classic:1.2.1.8-dev
 *  (ic2.api.crops.ICropTile). Реально используются (греп instanceof/cast по gregtech6_w/src/main):
 *  getSize() — GT_BaseCrop.java:104,109,114; harvest(boolean) — ToolCompat.java:192, GT_BaseCrop.java:150;
 *  getWeedExStorage/setWeedExStorage — GT_Spray_Bug_Item.java:66,68;
 *  getHydrationStorage/setHydrationStorage — TileEntityBase08FluidContainer.java:256,260,
 *  Behavior_Watering_Crops.java:52,56;
 *  getScanLevel/setScanLevel, getCrop, getGrowth, getGain, getResistance, getNutrientStorage,
 *  getNutrients, getHumidity, getAirQuality — Behavior_Cropnalyzer.java:78-96.
 *  Остальные методы реального ICropTile (getID/setID/setCrop/setSize/setGrowth/setGain/setResistance/
 *  getCustomData/setNutrientStorage/getWorld/getLocation/getLightLevel/pick/harvest_automated/reset/
 *  updateState/isBlockBelow/generateSeeds) в GT6-исходнике не используются (греп 0) — не добавлены. */
public interface ICropTile {
	byte getSize();
	boolean harvest(boolean aForced);
	int getWeedExStorage();
	void setWeedExStorage(int aWeedEx);
	int getHydrationStorage();
	void setHydrationStorage(int aHydration);
	byte getScanLevel();
	void setScanLevel(byte aScanLevel);
	CropCard getCrop();
	byte getGrowth();
	byte getGain();
	byte getResistance();
	int getNutrientStorage();
	byte getNutrients();
	byte getHumidity();
	byte getAirQuality();
}
