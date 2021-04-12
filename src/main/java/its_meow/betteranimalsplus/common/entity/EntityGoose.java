package its_meow.betteranimalsplus.common.entity;

import dev.itsmeow.imdlib.entity.EntityTypeContainer;
import dev.itsmeow.imdlib.entity.util.variant.IVariant;
import its_meow.betteranimalsplus.common.entity.ai.WaterfowlNavigator;
import its_meow.betteranimalsplus.common.entity.util.EntityUtil;
import its_meow.betteranimalsplus.common.entity.util.abstracts.EntityAnimalWithTypes;
import its_meow.betteranimalsplus.init.ModEntities;
import its_meow.betteranimalsplus.init.ModItems;
import its_meow.betteranimalsplus.init.ModSoundEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;

import java.util.*;
import java.util.function.Predicate;

public class EntityGoose extends EntityAnimalWithTypes {

    public int attacksLeft = 0;
    public int lastAttackTime = 0;
    protected final Set<UUID> dislikedPlayers = new HashSet<>();
    private int eatTicks;
    private static final Predicate<ItemEntity> ITEM_SELECTOR = (item) -> !item.cannotPickup() && item.isAlive();
    public int timeUntilNextEgg;
    public static final String PICKUP_BLOCK_LIST_KEY = "pickup_blacklist";

    public EntityGoose(EntityType<? extends EntityGoose> entityType, World worldIn) {
        super(entityType, worldIn);
        this.setCanPickUpLoot(true);
        this.timeUntilNextEgg = this.rand.nextInt(6000) + 6000;
        this.setPathPriority(PathNodeType.WATER, 0.0F);
    }

    @Override
    protected PathNavigator createNavigator(World worldIn) {
        return new WaterfowlNavigator(this, worldIn);
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.95F;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4D) {
            @Override
            public boolean shouldExecute() {
                return this.creature.getAttackTarget() == null && super.shouldExecute();
            }
        });
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0D, Ingredient.fromItems(Items.PUMPKIN_SEEDS, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.BREAD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(6, new RandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new EntityGoose.FindItemsGoal());
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 6.0F));

        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, false) {
            @Override
            public void startExecuting() {
                attacksLeft = this.attacker.getRNG().nextInt(4) + 1;
                super.startExecuting();
            }

            @Override
            public boolean shouldExecute() {
                return super.shouldExecute() && this.attacker.ticksExisted - lastAttackTime > 150 && !EntityGoose.this.isPassive();
            }

            @Override
            public boolean shouldContinueExecuting() {
                return attacksLeft > 0 && super.shouldContinueExecuting();
            }

            @Override
            protected void checkAndPerformAttack(LivingEntity p_190102_1_, double p_190102_2_) {
                if(attacksLeft > 0) {
                    super.checkAndPerformAttack(p_190102_1_, p_190102_2_);
                } else {
                    this.resetTask();
                }
            }

            @Override
            public void resetTask() {
                super.resetTask();
                if(attacksLeft <= 0) {
                    this.attacker.setAttackTarget(null);
                }
            }
        });
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            @Override
            public boolean shouldExecute() {
                return super.shouldExecute() && !EntityGoose.this.isPassive();
            }
        }.setCallsForHelp(EntityGoose.class));
        this.targetSelector.addGoal(1, new DislikeTargetGoal(this) {
            @Override
            public boolean shouldExecute() {
                return super.shouldExecute() && !EntityGoose.this.isPassive();
            }
        });
    }

    @Override
    protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return sizeIn.height;
    }

    protected boolean isPassive() {
        return this.getVariantString().equals("1") || world.getDifficulty() == Difficulty.PEACEFUL;
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if(!this.world.isRemote && this.isAlive() && this.isServerWorld()) {
            ItemStack itemstack = this.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
            if(itemstack.getItem().isFood() && this.getAttackTarget() == null) {
                ++this.eatTicks;
                if(this.eatTicks > 200) {
                    if(itemstack.getItem() == Items.BREAD) {
                        this.addPotionEffect(new EffectInstance(Effects.POISON, 900));
                    }
                    ItemStack itemstack1 = itemstack.onItemUseFinish(this.world, this);

                    if(!itemstack1.isEmpty()) {
                        this.setItemStackToSlot(EquipmentSlotType.MAINHAND, itemstack1);
                    }
                    this.eatTicks = 0;
                } else if(this.eatTicks > 160 && this.rand.nextFloat() < 0.1F) {
                    this.playSound(this.getEatSound(itemstack), 1.0F, 1.0F);
                    this.world.setEntityState(this, (byte) 45); // calls handleStatusUpdate((byte) 45);
                }
            }
        }
        if(!this.world.isRemote && !this.isChild() && --this.timeUntilNextEgg <= 0) {
            this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
            this.entityDropItem(this.getRNG().nextInt(128) == 0 ? ModItems.GOLDEN_GOOSE_EGG.get() : ModItems.GOOSE_EGG.get(), 1);
            this.timeUntilNextEgg = this.rand.nextInt(6000) + 6000;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleStatusUpdate(byte id) {
        if(id == 45) {
            ItemStack stack = this.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
            if(!stack.isEmpty()) {
                for(int i = 0; i < 8; ++i) {
                    Vector3d vec3d = (new Vector3d(((double) this.rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D)).rotatePitch(-this.rotationPitch * ((float) Math.PI / 180F)).rotateYaw(-this.rotationYaw * ((float) Math.PI / 180F));
                    this.world.addParticle(new ItemParticleData(ParticleTypes.ITEM, stack), this.getPosX() + this.getLookVec().x / 2.0D, this.getPosY(), this.getPosZ() + this.getLookVec().z / 2.0D, vec3d.x, vec3d.y + 0.05D, vec3d.z);
                }
            }
        } else {
            super.handleStatusUpdate(id);
        }
    }

    @Override
    public void setAttackTarget(LivingEntity entity) {
        super.setAttackTarget(entity);
        if(entity instanceof PlayerEntity) {
            UUID uuid = ((PlayerEntity) entity).getGameProfile().getId();
            dislikedPlayers.add(uuid);
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        if(attacksLeft > 0) {
            attacksLeft--;
        }
        this.lastAttackTime = this.ticksExisted;
        return super.attackEntityAsMob(entityIn);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if(source.getTrueSource() != null && source.getTrueSource() != null && source.getTrueSource() == this.getAttackTarget()) {
            this.lastAttackTime = 0; // allow instant retaliation
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected void spawnDrops(DamageSource damageSourceIn) {
        ItemStack itemstack = this.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
        if (!itemstack.isEmpty()) {
            this.entityDropItem(itemstack);
            this.setItemStackToSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
        }

        super.spawnDrops(damageSourceIn);
    }

    @Override
    public boolean canPickUpItem(ItemStack stack) {
        EquipmentSlotType type = MobEntity.getSlotForItemStack(stack);
        if (!this.getItemStackFromSlot(type).isEmpty()) {
            return false;
        } else {
            return type == EquipmentSlotType.MAINHAND && super.canPickUpItem(stack);
        }
    }

    @Override
    public boolean canEquipItem(ItemStack newStack) {
        ItemStack oldStack = this.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
        return !isPickupBlacklisted(newStack.getItem()) && (oldStack.isEmpty() || this.eatTicks == 0 && newStack.getItem().isFood() && !oldStack.getItem().isFood());
    }
    
    public boolean isPickupBlacklisted(Item item) {
        String id = item.getRegistryName().toString();
        for(String itemsId : getContainer().getCustomConfiguration().getStringListHolder(PICKUP_BLOCK_LIST_KEY).get()) {
            if (itemsId.startsWith("#")) {
                if (item.getTags().contains(new ResourceLocation(itemsId.substring(1)))) {
                    return true;
                }
            } else if(id.equals(itemsId)) {
                return true;
            }
        }
        return false;
    }

    private void dropItem(ItemStack stack) {
        if (!stack.isEmpty() && !this.world.isRemote) {
            ItemEntity itementity = new ItemEntity(this.world, this.getPosX() + this.getLookVec().x, this.getPosY() + 1.0D, this.getPosZ() + this.getLookVec().z, stack);
            itementity.setPickupDelay(40);
            itementity.setThrowerId(this.getUniqueID());
            this.playSound(SoundEvents.ENTITY_FOX_SPIT, 1.0F, 1.0F);
            this.world.addEntity(itementity);
        }
    }

    private void spawnItem(ItemStack stack) {
        ItemEntity entity = new ItemEntity(this.world, this.getPosX(), this.getPosY(), this.getPosZ(), stack);
        this.world.addEntity(entity);
    }

    @Override
    public SoundEvent getAmbientSound() {
        return ModSoundEvents.GOOSE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return ModSoundEvents.GOOSE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.GOOSE_DEATH.get();
    }

    @Override
    protected void updateEquipmentIfNeeded(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (this.canEquipItem(stack)) {
            int i = stack.getCount();
            if (i > 1) {
                this.spawnItem(stack.split(i - 1));
            }
            this.dropItem(this.getItemStackFromSlot(EquipmentSlotType.MAINHAND));
            this.setItemStackToSlot(EquipmentSlotType.MAINHAND, stack.split(1));
            this.inventoryHandsDropChances[EquipmentSlotType.MAINHAND.getIndex()] = 2.0F;
            this.onItemPickup(itemEntity, stack.getCount());
            itemEntity.remove();
            this.eatTicks = 0;
        }
    }

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        ListNBT list = new ListNBT();
        for(UUID uuid : dislikedPlayers) {
            list.add(StringNBT.valueOf(uuid.toString()));
        }
        compound.put("disliked_players", list);
    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        compound.getList("disliked_players", Constants.NBT.TAG_STRING).forEach(nbt -> dislikedPlayers.add(UUID.fromString(nbt.getString())));
    }

    @Override
    public ILivingEntityData onInitialSpawn(IServerWorld world, DifficultyInstance difficulty, SpawnReason reason, ILivingEntityData livingdata, CompoundNBT compound) {
        // getRandomType() should really pass in some more information...
        String[] types;
        switch (reason) {
        case NATURAL:
            types = new String[] {"2","3"};
            break;
        case CHUNK_GENERATION:
            types = new String[] {"2","3"};
            break;
        case STRUCTURE:
            types = new String[] {"2","3"};
            break;
        case BREEDING:
            types = new String[] {"1"};
            break;
        default:
            types = new String[] {"1","2","3"};
            break;
        }
        livingdata = EntityUtil.childChance(this, reason, livingdata, 0.25F);
        IVariant variant = this.getContainer().getVariantForName(types[this.getRNG().nextInt(types.length)]);
        if (livingdata instanceof AgeableTypeData) {
            variant = ((AgeableTypeData) livingdata).typeData;
        } else if (livingdata instanceof AgeableData) {
            livingdata = new AgeableTypeData((AgeableData) livingdata, variant);
        } else {
            livingdata = new AgeableTypeData(variant);
        }
        this.setType(variant);
        return livingdata;
    }

    public static boolean canGooseSpawn(EntityType<EntityGoose> type, IWorld world, SpawnReason reason, BlockPos pos, Random rand) {
        Block downBlock = world.getBlockState(pos.down()).getBlock();
        return ((downBlock == Blocks.GRASS_BLOCK && nearWater(world, pos)) || downBlock == Blocks.WATER) && world.getLightSubtracted(pos, 0) > 8 && world.isAirBlock(pos);
    }

    protected static boolean nearWater(IWorld world, BlockPos pos) {
        int i = 12;
        int j = 2;
        BlockPos.Mutable newpos = new BlockPos.Mutable();
        for(int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
            for(int l = 0; l < i; ++l) {
                for(int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                    for(int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                        newpos.setPos(pos).move(i1, k - 1, j1);
                        if(newpos.getX() >> 4 == pos.getX() >> 4 && newpos.getZ() >> 4 == pos.getZ() >> 4 && world.getBlockState(newpos).getBlock() == Blocks.WATER) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected EntityGoose getBaseChild() {
        return getContainer().getEntityType().create(world);
    }

    @Override
    public EntityTypeContainer<EntityGoose> getContainer() {
        return ModEntities.GOOSE;
    }

    public class FindItemsGoal extends Goal {
        public FindItemsGoal() {
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean shouldExecute() {
            if (!EntityGoose.this.getItemStackFromSlot(EquipmentSlotType.MAINHAND).isEmpty()) {
                return false;
            } else if (EntityGoose.this.getAttackTarget() == null && EntityGoose.this.getRevengeTarget() == null) {
                if (EntityGoose.this.getRNG().nextInt(10) != 0) {
                    return false;
                } else {
                    List<ItemEntity> list = EntityGoose.this.world.getEntitiesWithinAABB(ItemEntity.class, EntityGoose.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityGoose.ITEM_SELECTOR);
                    return !list.isEmpty() && EntityGoose.this.getItemStackFromSlot(EquipmentSlotType.MAINHAND).isEmpty();
                }
            } else {
                return false;
            }
        }

        @Override
        public void tick() {
            List<ItemEntity> list = EntityGoose.this.world.getEntitiesWithinAABB(ItemEntity.class, EntityGoose.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityGoose.ITEM_SELECTOR);
            ItemStack itemstack = EntityGoose.this.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
            if (itemstack.isEmpty() && !list.isEmpty()) {
                EntityGoose.this.getNavigator().tryMoveToEntityLiving(list.get(0), 1.2F);
            }

        }

        @Override
        public void startExecuting() {
            List<ItemEntity> list = EntityGoose.this.world.getEntitiesWithinAABB(ItemEntity.class, EntityGoose.this.getBoundingBox().grow(8.0D, 8.0D, 8.0D), EntityGoose.ITEM_SELECTOR);
            if (!list.isEmpty()) {
                EntityGoose.this.getNavigator().tryMoveToEntityLiving(list.get(0), 1.2F);
            }

        }
    }

    public class DislikeTargetGoal extends TargetGoal {

        protected EntityGoose goose;

        public DislikeTargetGoal(EntityGoose goose) {
            super(goose, true);
            this.goose = goose;
        }

        @Override
        public boolean shouldExecute() {
            return goose.dislikedPlayers.size() > 0 && goose.getAttackTarget() == null && this.goose.ticksExisted - goose.lastAttackTime > 150;
        }

        @Override
        protected boolean isSuitableTarget(LivingEntity potentialTarget, EntityPredicate targetPredicate) {
            return potentialTarget instanceof PlayerEntity && super.isSuitableTarget(potentialTarget, targetPredicate) && dislikedPlayers.contains(((PlayerEntity)potentialTarget).getGameProfile().getId());
        }

    }

}
