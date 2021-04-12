package its_meow.betteranimalsplus.common.entity.util.abstracts;

import dev.itsmeow.imdlib.entity.interfaces.IVariantTypes;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public abstract class EntityCreatureWithTypes extends CreatureEntity implements IVariantTypes<EntityCreatureWithTypes> {

    public EntityCreatureWithTypes(EntityType<? extends CreatureEntity> entityType, World worldIn) {
        super(entityType, worldIn);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.registerTypeKey();
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        this.writeType(compound);
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        this.readType(compound);
    }

    @Override
    @Nullable
    public ILivingEntityData onInitialSpawn(IServerWorld world, DifficultyInstance difficulty, SpawnReason reason, @Nullable ILivingEntityData livingdata, CompoundNBT compound) {
        return this.initData(world, reason, super.onInitialSpawn(world, difficulty, reason, livingdata, compound));
    }

    @Override
    public boolean canDespawn(double range) {
        return despawn(range);
    }

    @Override
    public EntityCreatureWithTypes getImplementation() {
        return this;
    }

}
