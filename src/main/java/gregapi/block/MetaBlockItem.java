package gregapi.block;

import gregapi.block.multitileentity.MultiTileEntityItemInternal;
import gregapi.block.prefixblock.PrefixBlockItem;
import gregapi.item.IMetaItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import static gregapi.data.CS.W;

/** Отдельные предметы вариантов используют прежний блок и его правила размещения. */
public abstract class MetaBlockItem extends BlockItem implements IMetaItem {
	private static final List<MetaBlockItem> FAMILIES = new ArrayList<>();
	private final MetaBlockItem mFamily;
	private final short mFixedMeta;
	private final Map<Short, MetaBlockItem> mVariants;

	protected MetaBlockItem(Block aBlock, Item.Properties aProperties) {
		super(aBlock, aProperties);
		mFamily = this;
		mFixedMeta = (short)(this instanceof ItemBlockBase && aBlock instanceof IBlockBase tBase && tBase.maxMeta() == 1 ? 0 : -1);
		mVariants = new HashMap<>();
		if (mFixedMeta >= 0) mVariants.put(mFixedMeta, this);
		FAMILIES.add(this);
	}
	protected MetaBlockItem(MetaBlockItem aFamily, short aMeta) {
		super(aFamily.getBlock(), new Item.Properties().setId(variantKey(aFamily, aMeta)));
		mFamily = aFamily.mFamily;
		mFixedMeta = aMeta;
		mVariants = aFamily.mVariants;
		mVariants.put(aMeta, this);
	}
	private static ResourceKey<Item> variantKey(MetaBlockItem aFamily, short aMeta) {
		var tID = BuiltInRegistries.ITEM.getKey(aFamily);
		return ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, Identifier.fromNamespaceAndPath(tID.getNamespace(), tID.getPath() + "-" + aMeta));
	}
	@Override public short fixedMeta() {return mFixedMeta;}
	@Override public MetaBlockItem metaFamily() {return mFamily;}
	@Override public MetaBlockItem itemForMeta(short aMeta) {return mVariants.getOrDefault(aMeta, mFamily);}
	protected abstract MetaBlockItem createVariant(short aMeta);

	/** Вариант не подменяет корневой Item в отображении Block -> Item. */
	@Override public void registerBlocks(Map<Block, Item> aMap, Item aItem) {if (mFamily == this) super.registerBlocks(aMap, aItem);}
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void getSubItems(Item aItem, CreativeModeTab aTab, List aList) {if (mFixedMeta >= 0) aList.add(new ItemStack(this));}

	public static void registerVariants(net.neoforged.neoforge.registries.RegisterEvent aEvent) {
		if (!aEvent.getRegistryKey().equals(net.minecraft.core.registries.Registries.ITEM)) return;
		try (var tStream = MetaBlockItem.class.getResourceAsStream("/gregapi/machineitem-types.json")) {
			var tMachines = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(tStream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			for (var tFamily : FAMILIES) {
				if (tFamily.fixedMeta() >= 0) continue;
				if (tFamily instanceof PrefixBlockItem tPrefix) {
					for (int i = 0; i < tPrefix.mBlock.mMaterialList.length && i < W; i++) if (tPrefix.mBlock.mPrefix.isGeneratingItem(tPrefix.mBlock.mMaterialList[i])) register(aEvent, tFamily, (short)i);
				} else if (tFamily instanceof MultiTileEntityItemInternal) {
					var tID = BuiltInRegistries.ITEM.getKey(tFamily);
					if (!tMachines.has(tID.toString())) continue;
					for (String tRange : tMachines.get(tID.toString()).getAsString().split(",")) {
						String[] tEnds = tRange.split("-");
						for (int i = Integer.parseInt(tEnds[0]), tLast = Integer.parseInt(tEnds[tEnds.length - 1]); i <= tLast; i++) register(aEvent, tFamily, (short)i);
					}
				} else if (tFamily instanceof ItemBlockBase tSimple) {
					for (short i = 0; i < tSimple.mPlaceable.maxMeta(); i++) register(aEvent, tFamily, i);
				}
			}
		} catch (java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
	}
	private static void register(net.neoforged.neoforge.registries.RegisterEvent aEvent, MetaBlockItem aFamily, short aMeta) {
		aEvent.register(net.minecraft.core.registries.Registries.ITEM, variantKey(aFamily, aMeta).identifier(), () -> aFamily.createVariant(aMeta));
	}
}
