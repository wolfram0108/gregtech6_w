# Зеркало чужих API — F10 (ОТЛОЖЕННЫЙ ЦЕНТРАЛЬНЫЙ УЗЕЛ)

> Решение: `doc/missions/gt6-port/decisions/F10-external-mod-compat.md` §3.2.
> Указание пользователя (2026-07-08): интеграцию с внешними модами **отложить и доделать
> централизованно, из одного места**, когда дойдём до интеграции модов.

## Что это

GregTech6 местами **реализует интерфейсы чужих модов** (IC2/AE2/Galacticraft/Forestry/Botania/…) —
условно, через `@Optional.Interface`/`@Optional.Method`. В Forge 1.7.10 движок вырезал эту дружбу, если
мода нет; NeoForge так не умеет, а самих модов под 26.1.2 нет.

Чтобы **ядро компилировалось**, здесь лежат **минимальные декларации** только тех методов чужих
интерфейсов, которые GregTech6 переопределяет — в их РОДНЫХ пакетах (`appeng/api/movable/IMovableTile`
и т.п.). Это ровно то, что делает оракул через `compileOnly` dev-jar'ы (build.gradle:199-210) — только
вместо несуществующих jar'ов кладём сами интерфейсы.

## Инварианты

- **Код ядра НЕ меняется** — `implements IMovableTile` остаётся дословным (1:1).
- **Рантайм безопасен** — без мода все внешние пути GregTech6 мертвы (`MD.X.mLoaded == false`); методы
  этих интерфейсов никто не зовёт.
- **Единственная точка возврата** — вся отложенность здесь. Реальная интеграция мода = удалить его файл(ы)
  отсюда + подключить настоящую зависимость/мост. Ни одной правки по 22 файлам-потребителям.
- **`@Optional.*`** → пустышка-аннотация `gregapi.api.Optional` (маркер, движок её не обрабатывает).

## Статус (2026-07-08)

**38 зеркал-интерфейсов созданы разом** (скриптом, минимальные `public interface X {}`) + эталон
`IMovableTile`. Компилятор подтвердил: **все импорты чужих типов ядра резолвятся** (недостающих чужих
пакетов — ноль). ОСТАЛОСЬ по зеркалам: добрать *члены* (методы для `@Override`, поля/статику, что зовёт
ядро) — компилятор-driven, по мере гашения движковых групп (сейчас каскад «cannot find symbol»
перемешан с невыстроенными F4/F5/F6). FQN-inline чужие типы (`cofh.api.energy.*`, `ic2.api.reactor.*`,
`appeng.tile.powersink.*`) — добираются тем же приёмом, когда до них дойдёт компилятор.

## Перечень (ядро) — что зеркалить

Вскрыто грепом (2026-07-08), `@Optional.Interface` в ядре:

| Чужой интерфейс | Кто реализует (ядро) | Статус |
|---|---|---|
| `appeng.api.movable.IMovableTile` | `TileEntityBase01Root` | ✅ шаблон построен |
| `ic2.api.item.ISpecialElectricItem` | `MultiItem`, … | ⬜ |
| `ic2.api.item.IElectricItemManager` | `MultiItem`, … | ⬜ |
| `ic2.api.item.IMetalArmor` | `ItemArmorBase` | ⬜ |
| `ic2.api.tile.IWrenchable` | `TileEntityBase08Directional` | ⬜ |
| `micdoodle8.mods.galacticraft.api.item.IItemElectric` | `MultiItem`, … | ⬜ |
| `micdoodle8.mods.galacticraft.api.block.IOxygenReliantBlock` | `MultiTileEntityBlockWithCompat` | ⬜ |
| `micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock` | `IBlockSealable`-цепочка | ⬜ |
| `forestry.api.apiculture.IArmorApiarist` | `ItemArmorBase` | ⬜ |
| `vazkii.botania.api.item.IFlowerPlaceable` | `PrefixBlockItem` | ⬜ |
| `vazkii.botania.api.mana.IManaTrigger` | `MultiItem`, … | ⬜ |
| `squeek.applecore.api.food.IEdible` | `MultiItem`, … | ⬜ |
| `openblocks.api.IPaintableBlock` | `MultiTileEntityBlockWithCompat` | ⬜ |

Плюс прямые упоминания чужих типов в сигнатурах/полях ядра (ic2.api.recipe/crops/energy.event,
forestry.api.genetics, thaumcraft.api, railcraft, cofh) — добираются тем же приёмом при выходе на них.
