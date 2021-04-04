package its_meow.betteranimalsplus.common.entity.projectile;

import its_meow.betteranimalsplus.common.entity.EntityGoose;
import its_meow.betteranimalsplus.init.ModEntities;
import its_meow.betteranimalsplus.init.ModItems;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityGooseEgg extends EntityModEgg {
    public static EntityType<EntityGooseEgg> GOOSE_EGG_TYPE = ModEntities.H.createEntityType(EntityGooseEgg.class, EntityGooseEgg::new, "goose_egg", EntityClassification.MISC, 64, 1, true, 0.25F, 0.25F);

    public EntityGooseEgg(World world) {
        super(GOOSE_EGG_TYPE, world);
    }

    public EntityGooseEgg(EntityType<? extends EntityGooseEgg> type, World world) {
        super(type, world);
    }

    public EntityGooseEgg(World world, LivingEntity thrower) {
        super(GOOSE_EGG_TYPE, world, thrower);
    }

    public EntityGooseEgg(World world, double x, double y, double z) {
        super(GOOSE_EGG_TYPE, world, x, y, z);
    }

    public EntityGooseEgg(World worldIn, IPosition pos) {
        super(GOOSE_EGG_TYPE, worldIn, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Item getDefaultItem() {
        return ModItems.GOOSE_EGG.get();
    }

    @Override
    protected Entity createEntity() {
        EntityGoose goose = ModEntities.GOOSE.entityType.create(this.world);
        goose.setGrowingAge(-24000);
        goose.setLocationAndAngles(this.getPosX(), this.getPosY(), this.getPosZ(), this.rotationYaw, 0.0F);
        goose.setType("1");
        return goose;
    }

}
