package dev.itsmeow.betteranimalsplus.common.entity.projectile;

import dev.itsmeow.betteranimalsplus.common.entity.EntityPheasant;
import dev.itsmeow.betteranimalsplus.init.ModEntities;
import dev.itsmeow.betteranimalsplus.init.ModItems;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class EntityPheasantEgg extends EntityModEgg {
    public static EntityType<EntityPheasantEgg> PHEASANT_EGG_TYPE = ModEntities.H.createEntityType(EntityPheasantEgg::new, "pheasant_egg", EntityClassification.MISC, 64, 1, true, 0.25F, 0.25F);
    
    public EntityPheasantEgg(World world) {
        super(PHEASANT_EGG_TYPE, world);
    }
    
    public EntityPheasantEgg(EntityType<? extends EntityPheasantEgg> type, World world) {
        super(type, world);
    }

    public EntityPheasantEgg(World world, LivingEntity thrower) {
        super(PHEASANT_EGG_TYPE, world, thrower);
    }

    public EntityPheasantEgg(World world, double x, double y, double z) {
        super(PHEASANT_EGG_TYPE, world, x, y, z);
    }
    
    public EntityPheasantEgg(World worldIn, IPosition pos) {
        super(PHEASANT_EGG_TYPE, worldIn, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Item getDefaultItem() {
        return ModItems.PHEASANT_EGG.get();
    }

    @Override
    protected Entity createEntity() {
        EntityPheasant pheasant = ModEntities.PHEASANT.getEntityType().create(this.world);
        pheasant.setGrowingAge(-24000);
        pheasant.setLocationAndAngles(this.getPosX(), this.getPosY(), this.getPosZ(), this.rotationYaw, 0.0F);
        pheasant.setType(pheasant.getRandomType());
        return pheasant;
    }

}
