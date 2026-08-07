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

package gregtech6.parity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * СУДЬЯ КОЛЛАЙДЕРА РЕШЁТОК (BUG-076).
 *
 * <p>Вопрос, на который отвечает: «какой коллайдер движок реально отдаёт для GT6-решётки, поставленной
 * в живой мир?» Не по коду, а спросив у самого движка — тремя путями, которыми ходит neo:</p>
 * <ul>
 *   <li>{@code getCollisionShape(level,pos)} — БЕЗ контекста: у нединамических блоков возвращает КЭШ,
 *       построенный на {@code EmptyBlockGetter} ({@code BlockBehaviour:674-675,916});</li>
 *   <li>{@code getCollisionShape(level,pos,context)} — С контекстом: идёт в блок мимо кэша ({@code :678-679});</li>
 *   <li>{@code getShape(level,pos,context)} — outline/прицеливание.</li>
 * </ul>
 *
 * <p><b>Контроли обязательны:</b> ванильные {@code iron_bars} — эталон тонкой решётки (PASS-контроль,
 * судья обязан уметь показать «тонко»); {@code stone} — эталон полного куба (FAIL-контроль, судья обязан
 * уметь показать «толсто»). Без них «полный куб у GT6-решётки» не отличить от поломки самого замера.</p>
 */
@ExtendWith(EphemeralTestServerProvider.class)
class BarsCollisionTest {

    private static String box(VoxelShape aShape) {
        if (aShape.isEmpty()) return "ПУСТО (сквозной)";
        return String.format("x[%.3f..%.3f] y[%.3f..%.3f] z[%.3f..%.3f] боксов=%d",
            aShape.min(net.minecraft.core.Direction.Axis.X), aShape.max(net.minecraft.core.Direction.Axis.X),
            aShape.min(net.minecraft.core.Direction.Axis.Y), aShape.max(net.minecraft.core.Direction.Axis.Y),
            aShape.min(net.minecraft.core.Direction.Axis.Z), aShape.max(net.minecraft.core.Direction.Axis.Z),
            aShape.toAabbs().size());
    }

    private static void probe(ServerLevel aLevel, BlockPos aPos, BlockState aState, String aLabel) {
        aLevel.setBlock(aPos, aState, 3);
        BlockState tPlaced = aLevel.getBlockState(aPos);
        System.out.println("[bars] === " + aLabel + " === поставлено: " + tPlaced);
        System.out.println("[bars]   collision БЕЗ контекста (кэш): " + box(tPlaced.getCollisionShape(aLevel, aPos)));
        System.out.println("[bars]   collision С контекстом       : " + box(tPlaced.getCollisionShape(aLevel, aPos, CollisionContext.empty())));
        System.out.println("[bars]   outline (getShape)           : " + box(tPlaced.getShape(aLevel, aPos, CollisionContext.empty())));
    }

    @Test
    void barsCollision(MinecraftServer aServer) {
        ServerLevel tLevel = aServer.overworld();
        if (tLevel == null) for (ServerLevel tAny : aServer.getAllLevels()) {tLevel = tAny; break;}
        if (tLevel == null) {
            // Мира у эфемерного сервера нет — живая ветка (getCollisionShape с Level) непроверяема.
            // Молчать об этом нельзя: судья обязан сказать, ЧЕГО он не измерил. Но кэш-ветку (ту самую,
            // что строится на EmptyBlockGetter и уходит в BlockState-кэш) измерить можно и без мира —
            // именно она отвечает за «полный куб», если мост не сработал.
            System.out.println("[bars] ⛔ У сервера нет уровней (levels=" + aServer.levelKeys().size() + ") — ЖИВАЯ ветка НЕ измерена.");
            System.out.println("[bars] Ниже — только кэш-ветка (EmptyBlockGetter), выводы о поведении в игре по ней делать НЕЛЬЗЯ.");
            var tEmpty = net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
            var tPos = BlockPos.ZERO;
            System.out.println("[bars] КОНТРОЛЬ iron_bars : collision=" + box(Blocks.IRON_BARS.defaultBlockState().getCollisionShape(tEmpty, tPos))
                + " outline=" + box(Blocks.IRON_BARS.defaultBlockState().getShape(tEmpty, tPos, CollisionContext.empty())));
            System.out.println("[bars] КОНТРОЛЬ stone     : collision=" + box(Blocks.STONE.defaultBlockState().getCollisionShape(tEmpty, tPos))
                + " outline=" + box(Blocks.STONE.defaultBlockState().getShape(tEmpty, tPos, CollisionContext.empty())));
            int tSeen = 0;
            for (Block tBlock : BuiltInRegistries.BLOCK) {
                if (!(tBlock instanceof gregapi.block.misc.BlockBaseBars)) continue;
                if (++tSeen > 3) continue;
                BlockState tState = tBlock.defaultBlockState();
                System.out.println("[bars] GT6 " + BuiltInRegistries.BLOCK.getKey(tBlock)
                    + " : collision=" + box(tState.getCollisionShape(tEmpty, tPos))
                    + " outline=" + box(tState.getShape(tEmpty, tPos, CollisionContext.empty())));
            }
            System.out.println("[bars] блоков класса BlockBaseBars в реестре: " + tSeen);

            // ПОЛНОТА КЛАССА, машинно. Определение дефекта: класс ПЕРЕОПРЕДЕЛИЛ геометрию 1.7.10
            // (setBlockBoundsBasedOnState / addCollisionBoxesToList), но BlockState-кэш всё равно отдаёт
            // полный куб — значит форма теряется везде, где neo спрашивает бесконтекстно.
            System.out.println("[bars] --- обход всех не-MTE блоков BlockBase ---");
            java.util.Set<String> tSeenClasses = new java.util.LinkedHashSet<>();
            int tDefect = 0, tOk = 0;
            for (Block tBlock : BuiltInRegistries.BLOCK) {
                if (!(tBlock instanceof gregapi.block.BlockBase)) continue;
                if (tBlock instanceof gregapi.block.multitileentity.MultiTileEntityBlockInternal) continue;
                Class<?> tCls = tBlock.getClass();
                if (!tSeenClasses.add(tCls.getName())) continue;
                boolean tOverridesGeometry = false;
                for (Class<?> c = tCls; c != null && c != gregapi.block.BlockBase.class; c = c.getSuperclass()) {
                    for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                        if (m.getName().equals("setBlockBoundsBasedOnState") || m.getName().equals("addCollisionBoxesToList")) {tOverridesGeometry = true; break;}
                    }
                    if (tOverridesGeometry) break;
                }
                if (!tOverridesGeometry) continue;
                VoxelShape tShape = tBlock.defaultBlockState().getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                boolean tFullCube = !tShape.isEmpty()
                    && tShape.min(net.minecraft.core.Direction.Axis.X) <= 0 && tShape.max(net.minecraft.core.Direction.Axis.X) >= 1
                    && tShape.min(net.minecraft.core.Direction.Axis.Y) <= 0 && tShape.max(net.minecraft.core.Direction.Axis.Y) >= 1
                    && tShape.min(net.minecraft.core.Direction.Axis.Z) <= 0 && tShape.max(net.minecraft.core.Direction.Axis.Z) >= 1;
                if (tFullCube) {tDefect++; System.out.println("[bars]   ⛔ " + tCls.getSimpleName() + " — геометрию переопределяет, кэш = полный куб");}
                else {tOk++; System.out.println("[bars]   ✅ " + tCls.getSimpleName() + " — кэш даёт форму: " + box(tShape));}
            }
            System.out.println("[bars] ИТОГ класса: дефектных классов " + tDefect + ", корректных " + tOk);

            // «Куба больше нет» ещё не значит «форма верная». Перебираем ВСЕ подтипы и печатаем форму,
            // чтобы сверить с числами оригинала (решётка: 1→Z-, 2→Z+, 4→X-, 8→X+; шип: сторона крепления).
            System.out.println("[bars] --- формы по всем подтипам (сверять с оригиналом) ---");
            for (Block tBlock : BuiltInRegistries.BLOCK) {
                boolean tBars = tBlock instanceof gregapi.block.misc.BlockBaseBars;
                boolean tSpike = tBlock instanceof gregapi.block.misc.BlockBaseSpike;
                if (!tBars && !tSpike) continue;
                if (!BuiltInRegistries.BLOCK.getKey(tBlock).toString().endsWith(tBars ? "bars.steel" : "spikes.steel")) continue;
                System.out.println("[bars] " + BuiltInRegistries.BLOCK.getKey(tBlock));
                for (int tMeta = 0; tMeta < 16; tMeta++) {
                    BlockState tState = tBlock.defaultBlockState();
                    if (!(tBlock instanceof gregapi.block.IBlockExtendedMetaData tEM)) continue;
                    BlockState tWith = tEM.getStateForExtendedMetaData(tState, (short)tMeta);
                    if (tWith == null) continue;
                    System.out.println("[bars]   мета " + String.format("%2d", tMeta)
                        + " collision=" + box(tWith.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)));
                }
            }
            return;
        }
        int tY = 80;
        int tX = 0;

        // контроли: движок обязан показать и «тонко», и «толсто» — иначе судья не судья
        probe(tLevel, new BlockPos(tX++, tY, 0), Blocks.IRON_BARS.defaultBlockState(), "КОНТРОЛЬ ванильные iron_bars (ожидание: тонко)");
        probe(tLevel, new BlockPos(tX++, tY, 0), Blocks.STONE.defaultBlockState(), "КОНТРОЛЬ камень (ожидание: полный куб)");

        // все GT6-блоки, чей класс — BlockBaseBars
        int tFound = 0;
        for (Block tBlock : BuiltInRegistries.BLOCK) {
            if (!(tBlock instanceof gregapi.block.misc.BlockBaseBars)) continue;
            tFound++;
            if (tFound > 3) continue; // трёх носителей довольно: класс один
            probe(tLevel, new BlockPos(tX++, tY, 0), tBlock.defaultBlockState(),
                "GT6 " + BuiltInRegistries.BLOCK.getKey(tBlock) + " (мета по умолчанию)");
        }
        System.out.println("[bars] всего блоков класса BlockBaseBars в реестре: " + tFound);
    }
}
