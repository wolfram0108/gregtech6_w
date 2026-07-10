package gregapi.api;

/**
 * Переходник F1 (жизненный цикл). Событие переназначения ID модов GregTech6 поверх NeoForge.
 *
 * <p>В Forge 1.7.10 движок слал {@code FMLModIdMappingEvent} при смене числовых ID блоков/предметов
 * (например, при загрузке мира, сохранённого с другим набором модов). GregTech6 ловил его в
 * {@code GT_API.onIDChangingEvent} и рассылал по compat-классам ({@code ICompat.onIDChanging}), чтобы
 * они пересобрали закэшированные ссылки на предметы.</p>
 *
 * <p>В NeoForge числовых ID нет — идентификаторы стабильны ({@code ResourceLocation}), поэтому прямого
 * аналога нет. Тип сохранён как маркер, чтобы контракт {@code ICompat} остался 1:1; привязка к
 * neo-эквиваленту (если потребуется) — вопрос рантайм-парити, решается при доводке жизненного цикла.</p>
 */
public class FMLModIdMappingEvent {
	public FMLModIdMappingEvent() {}
}
