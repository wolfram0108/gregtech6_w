package gregtech6;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.slf4j.Logger;

/**
 * Каркасный входной класс порта GregTech 6 на NeoForge 26.1.2.
 *
 * <p>ВРЕМЕННЫЙ: служит только для поднятия инструментария (сборка + headless-тесты).
 * При портировании lifecycle (аспект {@code architecture/lifecycle.md}) заменится на
 * связку из трёх модов оригинала — GAPI / GAPI_POST / GT.</p>
 *
 * <p>Держит один пример-блок {@code gregtech6:test_block} — он служит мишенью для
 * будущих GameTest'ов (проверка конвейера регистрация → мир → рендер).</p>
 */
@Mod(GregTech6.MODID)
public class GregTech6 {
    public static final String MODID = "gregtech6";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> TEST_BLOCK =
            BLOCKS.registerSimpleBlock("test_block", p -> p.mapColor(MapColor.METAL));
    public static final DeferredItem<BlockItem> TEST_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("test_block", TEST_BLOCK);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("gregtech6", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gregtech6"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> TEST_BLOCK_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(TEST_BLOCK_ITEM.get()))
                    .build());

    public GregTech6(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        LOGGER.info("[GregTech6] skeleton loaded — NeoForge 26.1.2 toolchain bring-up");
    }
}
