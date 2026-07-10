package twilightforest.entity.boss;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** F10 ЗЕРКАЛО (compile-only) чужого API — Twilight Forest. Минимум для instanceof-проверок GT6. */
public abstract class EntityTFMinoshroom extends PathfinderMob {
    protected EntityTFMinoshroom(EntityType<? extends PathfinderMob> aType, Level aLevel) {
        super(aType, aLevel);
    }
}
