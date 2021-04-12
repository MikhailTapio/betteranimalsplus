package its_meow.betteranimalsplus.common.entity.util.abstracts;

import dev.itsmeow.imdlib.entity.interfaces.ISelectiveVariantTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

public abstract class EntityAnimalWithSelectiveTypes extends EntityAnimalWithTypes implements ISelectiveVariantTypes<EntityAnimalWithTypes> {

    public EntityAnimalWithSelectiveTypes(EntityType<? extends EntityAnimalWithSelectiveTypes> type, World world) {
        super(type, world);
    }

}
