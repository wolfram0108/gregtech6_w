package ganymedes01.etfuturum.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Et Futurum. Минимум для instanceof-проверок GT6. */
public abstract class EntityStray extends LivingEntity {
    protected EntityStray(EntityType<? extends LivingEntity> aType, Level aLevel) {
        super(aType, aLevel);
    }
}
