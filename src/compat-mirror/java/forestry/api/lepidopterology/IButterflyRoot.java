package forestry.api.lepidopterology;
/** Forestry-mirror (compile-only; настоящий Forestry в рантайме, гейт MD.FR.mLoaded). */
public interface IButterflyRoot {
	IBreedingTracker getBreedingTracker(net.minecraft.world.level.Level aLevel, com.mojang.authlib.GameProfile aProfile);
	net.minecraft.world.item.ItemStack getMemberStack(IButterfly aButterfly, int aType);
}
