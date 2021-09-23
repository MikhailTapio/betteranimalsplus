package dev.itsmeow.betteranimalsplus.common.entity.util.abstracts;

import dev.itsmeow.imdlib.entity.util.variant.IVariant;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public abstract class EntityAnimalWithTypesAndSize extends EntityAnimalWithTypes {

    protected static final DataParameter<Float> SIZE = EntityDataManager.createKey(EntityAnimalWithTypesAndSize.class, DataSerializers.FLOAT);

    public EntityAnimalWithTypesAndSize(EntityType<? extends EntityAnimalWithTypes> entityType, World worldIn) {
        super(entityType, worldIn);
        this.setSize(0.35F);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(SIZE, 1F);
    }

    @Override
    public EntitySize getSize(Pose pose) {
        float size = this.dataManager.get(SIZE);
        return EntitySize.flexible(size, size).scale(this.getRenderScale());
    }

    public void setSize(float size) {
        this.dataManager.set(SIZE, size);
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        compound.putFloat("Size", this.getSize(Pose.STANDING).width);
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        float size = compound.getFloat("Size");
        this.setSize(size);
    }

    @Override
    @Nullable
    public ILivingEntityData onInitialSpawn(IServerWorld world, DifficultyInstance difficulty, SpawnReason reason, @Nullable ILivingEntityData livingdata, CompoundNBT compound) {
        livingdata = super.onInitialSpawn(world, difficulty, reason, livingdata, compound);
        if(!this.isChild()) {
            IVariant i = this.getRandomType();
            float rand = this.getRandomizedSize();

            if(livingdata instanceof AgeableSizeTypeData) {
                i = ((AgeableSizeTypeData) livingdata).typeData;
                rand = ((AgeableSizeTypeData) livingdata).size;
            } else {
                livingdata = new AgeableSizeTypeData(i, rand);
            }

            this.setType(i);
            this.setSize(rand);
        }
        return livingdata;
    }

    protected abstract float getRandomizedSize();

    public static class AgeableSizeTypeData extends AgeableTypeData {

        public float size;

        public AgeableSizeTypeData(IVariant type, float size) {
            super(type);
            this.size = size;
        }

        public AgeableSizeTypeData(AgeableData ageable, IVariant type, float size) {
            super(ageable, type);
            this.size = size;
        }
    }

}
