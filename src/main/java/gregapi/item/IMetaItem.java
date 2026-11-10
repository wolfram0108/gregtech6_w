package gregapi.item;

import net.minecraft.world.item.Item;

/** Тип задаётся отдельным Item; корень многовариантного семейства означает wildcard без subtype-компонента. */
public interface IMetaItem {
	short fixedMeta();
	Item metaFamily();
	Item itemForMeta(short aMeta);
	/** Зарегистрированные варианты семейства; коллекция предназначена только для чтения. */
	java.util.Collection<? extends Item> metaVariants();
}
