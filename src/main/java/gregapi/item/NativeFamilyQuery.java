package gregapi.item;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Только шаблон запроса: варианты ванильного семейства остаются собственными Item. */
public final class NativeFamilyQuery extends Item {
	private static final Map<Item, NativeFamilyQuery> QUERIES = new IdentityHashMap<>();
	public final List<Item> variants;
	private NativeFamilyQuery(ResourceKey<Item> aKey, Item[] aVariants) {
		super(new Item.Properties().setId(aKey));
		variants = List.of(aVariants);
	}
	public Item variant(int aMeta) {return aMeta >= 0 && aMeta < variants.size() ? variants.get(aMeta) : null;}
	public static NativeFamilyQuery of(Item aItem) {return aItem instanceof NativeFamilyQuery tQuery ? tQuery : QUERIES.get(gregapi.data.CS.Flattened.headItemOf(aItem));}
	public static void register(net.neoforged.neoforge.registries.RegisterEvent aEvent, Item[] aVariants) {
		Item tHead = aVariants[0];
		if (QUERIES.containsKey(tHead)) return;
		var tID = BuiltInRegistries.ITEM.getKey(tHead);
		var tKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("gregapi", "query/" + tID.getNamespace() + "/" + tID.getPath()));
		var tQuery = new NativeFamilyQuery(tKey, aVariants);
		aEvent.register(Registries.ITEM, tKey.identifier(), () -> tQuery);
		QUERIES.put(tHead, tQuery);
	}
}
