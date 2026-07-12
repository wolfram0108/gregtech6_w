package invtweaks.api.container;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * F10 compat-mirror (см. {@code src/compat-mirror/README.md}, `decisions/F10-external-mod-compat.md` §3.2) —
 * минимальное зеркало аннотации мода InvTweaks. GregTech6 помечает свои {@code Container}-классы этой
 * аннотацией ({@code gregapi/gui/ContainerCommonDefault.java}, {@code ContainerCommonChest.java}, дословно
 * {@code @invtweaks.api.container.ChestContainer}/{@code @invtweaks.api.container.ChestContainer(isLargeChest = true)})
 * для интеграции с сортировкой инвентаря InvTweaks; без мода путь мёртв (аннотация — чистая метадата,
 * читается ТОЛЬКО самим InvTweaks через рефлексию, GregTech6 её не читает). Не выдумано — форма (единственный
 * атрибут {@code isLargeChest}) выведена из фактического использования в этих же 2 местах ядра, тем же
 * приёмом, что 38 других зеркал этого узла.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChestContainer {
	boolean isLargeChest() default false;
}
