package its_meow.betteranimalsplus.common.entity;

import dev.itsmeow.imdlib.entity.util.EntityTypeContainer;
import dev.itsmeow.imdlib.entity.util.EntityTypeContainerContainable;
import its_meow.betteranimalsplus.common.entity.ai.HungerNearestAttackableTargetGoal;
import its_meow.betteranimalsplus.common.entity.ai.PeacefulNearestAttackableTargetGoal;
import its_meow.betteranimalsplus.common.entity.util.IHaveHunger;
import its_meow.betteranimalsplus.common.entity.util.abstracts.EntityWaterMobPathing;
import its_meow.betteranimalsplus.common.entity.util.abstracts.EntityWaterMobPathingBucketable;
import its_meow.betteranimalsplus.init.ModEntities;
import its_meow.betteranimalsplus.init.ModLootTables;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class EntityBarracuda extends EntityWaterMobPathingBucketable implements IHaveHunger<EntityWaterMobPathing> {

    private int hunger = 0;

    public EntityBarracuda(World world) {
        super(ModEntities.BARRACUDA.entityType, world);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new RushAttackGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.8D, false));
        this.goalSelector.addGoal(2, new LookAtGoal(this, WaterMobEntity.class, 10.0F));
        this.goalSelector.addGoal(2, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(3, new RandomSwimmingGoal(this, 0.5D, 1));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            @Override
            public boolean shouldExecute() {
                return EntityBarracuda.this.world.getDifficulty() != Difficulty.PEACEFUL && super.shouldExecute();
            }
        });
        this.targetSelector.addGoal(1, new HungerNearestAttackableTargetGoal<>(this, WaterMobEntity.class, 100, true, true, e -> !(e instanceof IMob) && !(e instanceof EntityBarracuda)));
        this.targetSelector.addGoal(1, new PeacefulNearestAttackableTargetGoal<>(this, PlayerEntity.class, 100, true, true, EntityBarracuda::isWearingShiny));
    }

    public static boolean isWearingShiny(LivingEntity e) {
        if(e instanceof PlayerEntity) {
            for(ItemStack stack : e.getArmorInventoryList()) {
                if(stack.getItem() instanceof ArmorItem) {
                    ArmorItem i = (ArmorItem) stack.getItem();
                    IArmorMaterial mat = i.getArmorMaterial();
                    if(mat == ArmorMaterial.CHAIN || mat == ArmorMaterial.DIAMOND || mat == ArmorMaterial.GOLD || mat == ArmorMaterial.IRON) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public int getHunger() {
        return hunger;
    }

    @Override
    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.ticksExisted % 20 == 0) {
            this.incrementHunger();
        }
    }

    @Nullable
    @Override
    public ILivingEntityData onInitialSpawn(IWorld world, DifficultyInstance difficulty, SpawnReason reason, @Nullable ILivingEntityData livingdata, CompoundNBT compound) {
        this.setInitialHunger();
        return super.onInitialSpawn(world, difficulty, reason, livingdata, compound);
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        this.writeHunger(compound);
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        this.readHunger(compound);
    }

    @Override
    public void livingTick() {
        if(!this.isInWater() && this.onGround && this.collidedVertically) {
            this.setMotion(this.getMotion().add((this.rand.nextFloat() * 2.0F - 1.0F) * 0.05F, 0.4F, (this.rand.nextFloat() * 2.0F - 1.0F) * 0.05F));
            this.onGround = false;
            this.isAirBorne = true;
            this.playSound(this.getFlopSound(), this.getSoundVolume(), this.getSoundPitch());
        }
        super.livingTick();
    }

    @Override
    protected ResourceLocation getLootTable() {
        return ModLootTables.DROPS_COD;
    }

    protected SoundEvent getFlopSound() {
        return SoundEvents.ENTITY_COD_FLOP;
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10D);
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(2D);
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(2D);
    }

    @Override
    protected boolean processInteract(PlayerEntity player, Hand hand) {
        if(player != this.getAttackTarget()) {
            return super.processInteract(player, hand);
        } else {
            return false;
        }
    }

    @Override
    public EntityTypeContainer<?> getContainer() {
        return ModEntities.BARRACUDA;
    }

    @Override
    public EntityTypeContainerContainable<?, ?> getContainableContainer() {
        return ModEntities.BARRACUDA;
    }

    public static class RushAttackGoal extends Goal {

        protected EntityBarracuda e;
        protected boolean done = false;

        public RushAttackGoal(EntityBarracuda e) {
            this.e = e;
            this.setMutexFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean shouldExecute() {
            return this.shouldContinueExecuting() && e.getPositionVec().distanceTo(e.getAttackTarget().getPositionVec()) >= 8D;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !done && e.getAttackTarget() != null && e.getAttackTarget().isAlive() && EntityBarracuda.isWearingShiny(e);
        }

        @Override
        public void startExecuting() {
            // rush!
            e.getNavigator().tryMoveToEntityLiving(e.getAttackTarget(), 10D);
        }

        @Override
        public void resetTask() {
            done = false;
        }

        @Override
        public void tick() {
            e.getNavigator().tryMoveToEntityLiving(e.getAttackTarget(), 10D);
            e.setMotion(e.getMotion().add(0, -0.005D, 0));
            // attack 5HP when close and then move to regular attack
            if(e.getPositionVec().distanceTo(e.getAttackTarget().getPositionVec()) < 1.5D) {
                done = true;
                e.getAttackTarget().attackEntityFrom(DamageSource.causeMobDamage(e), 5F);
            }
            if(e.world instanceof ServerWorld) {
                ServerWorld world = (ServerWorld) e.world;
                world.spawnParticle(ParticleTypes.BUBBLE, e.getPosX(), e.getPosY() + 0.5D, e.getPosZ(), 5, 0.25D, 0.5D, 0.25D, 0D);
            }
        }

    }
}
