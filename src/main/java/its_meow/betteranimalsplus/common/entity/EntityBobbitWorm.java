package its_meow.betteranimalsplus.common.entity;

import dev.itsmeow.imdlib.entity.util.EntityTypeContainer;
import its_meow.betteranimalsplus.common.entity.util.abstracts.EntityAnimalWithTypes;
import its_meow.betteranimalsplus.init.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathType;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class EntityBobbitWorm extends EntityAnimalWithTypes {

    protected static final DataParameter<Integer> ATTACK_STATE = EntityDataManager.createKey(EntityBobbitWorm.class, DataSerializers.VARINT);
    private float lastAttack = 0;
    private float lastGrab = 0;
    private Vec3d targetPosition;

    public EntityBobbitWorm(World world) {
        super(ModEntities.BOBBIT_WORM.entityType, world);
        this.setPathPriority(PathNodeType.WATER, 10F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtGoal(this, LivingEntity.class, 10.0F));
        Set<Class<? extends LivingEntity>> blackList = new HashSet<>();
        blackList.add(SkeletonEntity.class);
        blackList.add(EndermanEntity.class);
        blackList.add(EntityJellyfish.class);
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, true, e -> e.getWidth() < 3 && !(e instanceof IMob) && !(e instanceof EntityBobbitWorm) && !(e.world.getDifficulty() == Difficulty.PEACEFUL && e instanceof PlayerEntity) && !blackList.contains(e.getClass())));
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(ATTACK_STATE, 0);
    }

    public int getAttackState() {
        return this.dataManager.get(ATTACK_STATE);
    }

    public void setAttackState(int state) {
        this.dataManager.set(ATTACK_STATE, state);
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(15D);
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4D);
        this.getAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(10D);
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        return entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), (float) this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getValue());
    }

    @Nullable
    protected Vec3d getNewTargetPosition() {
        Vec3d pos = RandomPositionGenerator.findRandomTarget(this, 20, 5);
        if(pos != null) {
            if(isGoodBurrowingPosition(new BlockPos(pos))) {
                return pos;
            }
        }
        if(world.getBlockState(this.getPosition().down()).getBlock() == Blocks.WATER) {
            return new Vec3d(this.getPosX(), this.getPosY() - 1D, this.getPosZ());
        }
        return null;
    }

    public boolean isGoodBurrowingPosition(BlockPos pos) {
        Block below = world.getBlockState(pos.down()).getBlock();
        BlockState here = this.world.getBlockState(pos);
        return (below == Blocks.CLAY || below == Blocks.SAND || below == Blocks.GRAVEL || below == Blocks.DIRT) && here.allowsMovement(world, pos, PathType.WATER) && here.getFluidState().isTagged(FluidTags.WATER);
    }

    @Override
    protected PathNavigator createNavigator(World worldIn) {
        return new SwimmerPathNavigator(this, worldIn);
    }

    @Override
    public void travel(Vec3d vec) {
        this.move(MoverType.SELF, this.getMotion());
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if(this.isPassenger(passenger)) {
            passenger.setPosition(this.getPosX(), this.getPosY() - (this.getHeight() / 2), this.getPosZ());
        }
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean canBeRiddenInWater(Entity rider) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if(!this.isInWater()) {
            this.setMotion(this.getMotion().getX() * 0.2F, this.getMotion().getY(), this.getMotion().getZ() * 0.2F);
            if(!this.hasNoGravity()) {
                this.setMotion(this.getMotion().getX(), this.getMotion().getY() - 0.08D, this.getMotion().getZ());
            }
            this.setMotion(this.getMotion().getX(), this.getMotion().getY() * 0.9800000190734863D, this.getMotion().getZ());
        } else if(!world.isRemote) {
            if(this.targetPosition != null) {
                this.setMotion((this.targetPosition.x - this.getPosX()) * 0.05F, (this.targetPosition.y - this.getPosY()) * 0.05F, (this.targetPosition.z - this.getPosZ()) * 0.05F);
            }
            if(targetPosition != null && Math.sqrt(this.getPosition().distanceSq(this.targetPosition.x, this.targetPosition.y, this.targetPosition.z, false)) < 1) {
                this.setMotion(this.getMotion().getX() * 0.2F, this.getMotion().getY(), this.getMotion().getZ() * 0.2F);
            }
        }
        if(world.isRemote) { // shut up client stop MOVING you stupid idiot
            this.setMotion(this.getMotion().getX() * 0.2F, this.getMotion().getY() * 0.2F, this.getMotion().getZ() * 0.2F);
        }
        boolean goodPos = this.isGoodBurrowingPosition(this.getPosition());
        if(this.targetPosition == null && !goodPos) {
            Vec3d pos = this.getNewTargetPosition();
            if(pos != null) {
                this.targetPosition = pos;
            }
        }
        if(targetPosition != null && Math.sqrt(this.getPosition().distanceSq(this.targetPosition.x, this.targetPosition.y, this.targetPosition.z, false)) < 1 && !goodPos) {
            this.targetPosition = null;
        }
        if(this.getAttackState() > 0) {
            this.setAttackState(this.getAttackState() - 1);
        }
        if(!this.world.isRemote && this.getAttackTarget() != null && this.getAttackTarget().isAlive() && this.isAlive()) {
            if(this.getPassengers().contains(this.getAttackTarget())) {
                float time = 30F;
                if(this.lastAttack + (time - 20) < this.ticksExisted) {
                    this.setAttackState(20);
                }
                if(this.lastAttack + time < this.ticksExisted) {
                    this.attackEntityAsMob(this.getAttackTarget());
                    this.lastAttack = this.ticksExisted;
                }
            } else if(lastGrab + 60F < this.ticksExisted && this.getDistanceSq(this.getAttackTarget()) < 5) {
                if(!this.getAttackTarget().isInvulnerable() && this.getAttackTarget().getWidth() < 2.5 && this.getAttackTarget().getHeight() < 2.5) {
                    this.getAttackTarget().startRiding(this, false);
                } else if(!this.getAttackTarget().isInvulnerable()) {
                    this.attackEntityAsMob(this.getAttackTarget());
                }
                lastGrab = this.ticksExisted;
            }
        }
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithNearbyEntities() {
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public CreatureAttribute getCreatureAttribute() {
        return CreatureAttribute.WATER;
    }

    @Override
    public boolean isNotColliding(IWorldReader worldIn) {
        return worldIn.checkNoEntityCollision(this);
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    @Override
    protected int getExperiencePoints(PlayerEntity player) {
        return 1 + this.world.rand.nextInt(3);
    }

    protected void updateAir(int air) {
        if(this.isAlive() && !this.isInWaterOrBubbleColumn()) {
            this.setAir(air - 1);
            if(this.getAir() == -20) {
                this.setAir(0);
                this.attackEntityFrom(DamageSource.DROWN, 2.0F);
            }
        } else {
            this.setAir(300);
        }

    }

    @Override
    public void baseTick() {
        int i = this.getAir();
        super.baseTick();
        this.updateAir(i);
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public EntityTypeContainer<EntityBobbitWorm> getContainer() {
        return ModEntities.BOBBIT_WORM;
    }

    @Override
    protected EntityBobbitWorm getBaseChild() {
        return null;
    }

}
