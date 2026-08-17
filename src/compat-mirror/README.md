# Зеркало чужих API — F10 (ОТЛОЖЕННЫЙ ЦЕНТРАЛЬНЫЙ УЗЕЛ)

> Решение: `doc/missions/gt6-port/decisions/F10-external-mod-compat.md` §3.2.
> Указание пользователя (2026-07-08): интеграцию с внешними модами **отложить и доделать
> централизованно, из одного места**, когда дойдём до интеграции модов.

## Что это

GregTech6 местами **реализует интерфейсы чужих модов** (IC2/AE2/Galacticraft/Forestry/Botania/…) —
условно, через `@Optional.Interface`/`@Optional.Method`. В Forge 1.7.10 движок вырезал эту дружбу, если
мода нет; NeoForge так не умеет, а самих модов под 26.1.2 нет.

Чтобы **ядро компилировалось**, здесь лежат **минимальные декларации** только тех методов чужих
интерфейсов, которые GregTech6 переопределяет — в их РОДНЫХ пакетах (`ic2/api/tile/IWrenchable`
и т.п.). Это ровно то, что делает оракул через `compileOnly` dev-jar'ы (build.gradle:199-210) — только
вместо несуществующих jar'ов кладём сами интерфейсы.

⛔ **ЗЕРКАЛО СНИМАЕТСЯ, КАК ТОЛЬКО МОД ПОДКЛЮЧЁН ПО-НАСТОЯЩЕМУ.** Пакет, которым владеет реальный jar,
не должен существовать здесь ни одним классом: один пакет обязан принадлежать ровно одному источнику
классов, иначе второй молча затирается, а порядок обхода недетерминирован. Первый случай — `appeng.*`:
зеркала удалены при подключении AE2 15.4.10 (этап Э0 слоя совместимости GT6 ⇄ AE2,
`build.gradle` → `maven.modrinth:ae2`).

## Инварианты

- **Код ядра НЕ меняется, ПОКА мод не подключён** — `implements <чужой интерфейс>` остаётся дословным (1:1).
  Когда мод подключён настоящим jar, зеркало снимается, а ядро разводится с ним по факту нового API
  (пример: `IMovableTile` у AE2 под 1.20.1 не существует — снят с `TileEntityBase01Root`, методы Грега целы).
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
перемешан с невыстроенными F4/F5/F6). FQN-inline чужие типы (`cofh.api.energy.*`, `ic2.api.reactor.*`)
— добираются тем же приёмом, когда до них дойдёт компилятор.

**Обновление (Э0 слоя AE2):** пять зеркал `appeng.*` (`api/AEApi`, `api/movable/IMovableTile`,
`api/recipes/IGrinderRecipeHandler`, `api/registries/IRegistries`, `tile/powersink/IC2`) **удалены** — AE2
15.4.10 подключена настоящим jar. Их потребители разведены: `IMovableTile` снят с `TileEntityBase01Root`
(методы `prepareToMove`/`doneMoving` Грега остались обычными), `AEApi` + grinder-реестр сняты вместе с
носителем (кварцевой мельницы у AE2 под 1.20.1 нет), `powersink.IC2` — вместе с флагом
`EnergyCompat.AE_ENERGY` (энергию AE2 принимает движковой FE-капой, её кормит общий мост `feHandler`).

## Перечень (ядро) — что зеркалить

Вскрыто грепом (2026-07-08), `@Optional.Interface` в ядре:

| Чужой интерфейс | Кто реализует (ядро) | Статус |
|---|---|---|
| ~~`appeng.api.movable.IMovableTile`~~ | ~~`TileEntityBase01Root`~~ | ⛔ СНЯТО (Э0 слоя AE2): подключена настоящая AE2 15.4.10 |
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
