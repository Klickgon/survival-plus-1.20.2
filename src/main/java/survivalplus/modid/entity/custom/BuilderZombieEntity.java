package survivalplus.modid.entity.custom;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import survivalplus.modid.entity.ai.BuilderZombDestroyBedGoal;
import survivalplus.modid.entity.ai.movecontrols.BuilderZombieMoveControl;
import survivalplus.modid.entity.ai.pathing.BuilderZombieNavigation;
import survivalplus.modid.util.IHostileEntityChanger;
import survivalplus.modid.util.ModGamerules;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.function.Predicate;

public class BuilderZombieEntity
        extends ZombieEntity {
    private static final TrackedData<Integer> ZOMBIE_TYPE = DataTracker.registerData(survivalplus.modid.entity.custom.BuilderZombieEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> CONVERTING_IN_WATER = DataTracker.registerData(survivalplus.modid.entity.custom.BuilderZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Predicate<Difficulty> DOOR_BREAK_DIFFICULTY_CHECKER = difficulty -> difficulty == Difficulty.HARD;
    private final BreakDoorGoal breakDoorsGoal = new BreakDoorGoal(this, DOOR_BREAK_DIFFICULTY_CHECKER);
    private boolean canBreakDoors;
    private int inWaterTime;
    private int ticksUntilWaterConversion;
    private int DirtPlaceCooldown = 0;

    public boolean hasTargetBed = false;

    public BlockPos targetBedPos;

    public BuilderZombieEntity(EntityType<? extends net.minecraft.entity.mob.ZombieEntity> entityType, World world) {
        super(entityType, world);
        this.navigation = new BuilderZombieNavigation(this, this.getWorld());
        this.moveControl = new BuilderZombieMoveControl(this);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(4, new BuilderZombDestroyBedGoal(this, 1.0, 8));
        this.goalSelector.add(5, new DestroyEggGoal(this, 1.0, 3));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.initCustomGoals();
    }

    protected void initCustomGoals() {
        this.goalSelector.add(2, new ZombieAttackGoal(this, 1.0, false));
        this.goalSelector.add(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge(ZombifiedPiglinEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
        this.targetSelector.add(5, new ActiveTargetGoal<>(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
    }

    public static DefaultAttributeContainer.Builder createZombieAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23f).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0).add(EntityAttributes.GENERIC_ARMOR, 2.0).add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(ZOMBIE_TYPE, 0);
        this.getDataTracker().startTracking(CONVERTING_IN_WATER, false);
    }

    public boolean isConvertingInWater() {
        return this.getDataTracker().get(CONVERTING_IN_WATER);
    }

    public boolean canBreakDoors() {
        return this.canBreakDoors;
    }

    public void setCanBreakDoors(boolean canBreakDoors) {
        if (this.shouldBreakDoors() && NavigationConditions.hasMobNavigation(this)) {
            if (this.canBreakDoors != canBreakDoors) {
                this.canBreakDoors = canBreakDoors;
                ((MobNavigation)this.getNavigation()).setCanPathThroughDoors(canBreakDoors);
                if (canBreakDoors) {
                    this.goalSelector.add(1, this.breakDoorsGoal);
                } else {
                    this.goalSelector.remove(this.breakDoorsGoal);
                }
            }
        } else if (this.canBreakDoors) {
            this.goalSelector.remove(this.breakDoorsGoal);
            this.canBreakDoors = false;
        }
    }

    @Override
    protected boolean canConvertInWater() {
        return false;
    }

    @Override
    public void tick() {
        this.hasTargetBed = this.targetBedPos != null;
        if (!this.getWorld().isClient && this.isAlive() && !this.isAiDisabled()) {
            if (this.isConvertingInWater()) {
                --this.ticksUntilWaterConversion;
                if (this.ticksUntilWaterConversion < 0) {
                    this.convertInWater();
                }
            } else if (this.canConvertInWater()) {
                if (this.isSubmergedIn(FluidTags.WATER)) {
                    ++this.inWaterTime;
                    if (this.inWaterTime >= 600) {
                        this.setTicksUntilWaterConversion(300);
                    }
                } else {
                    this.inWaterTime = -1;
                }
            }
        }
        super.tick();
    }

    @Override
    public void tickMovement() {
        if (this.isAlive()) {
            boolean bl;
            boolean bl2 = bl = this.burnsInDaylight() && this.isAffectedByDaylight();
            if (bl) {
                ItemStack itemStack = this.getEquippedStack(EquipmentSlot.HEAD);
                if (!itemStack.isEmpty()) {
                    if (itemStack.isDamageable()) {
                        itemStack.setDamage(itemStack.getDamage() + this.random.nextInt(2));
                        if (itemStack.getDamage() >= itemStack.getMaxDamage()) {
                            this.sendEquipmentBreakStatus(EquipmentSlot.HEAD);
                            this.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        }
                    }
                    bl = false;
                }
                if (bl) {
                    this.setOnFireFor(8);
                }
            }

            LivingEntity target = getTarget();
            IHostileEntityChanger bzomb = (IHostileEntityChanger) this;
            if (this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING) && DirtPlaceCooldown <= 0 && (target != null || this.hasTargetBed || bzomb.getBaseAssault() != null)) {
                World world = this.getWorld();
                BlockPos BlockUnder = getBlockPos().down(1);
                BlockPos BlockUnder2 = getBlockPos().down(2);

                if (calcDiffY() >= 0) {
                    if (canPlaceDirt(world, BlockUnder, BlockUnder2)) {
                        world.setBlockState(BlockUnder, Blocks.DIRT.getDefaultState());
                        world.playSound(null, BlockUnder, SoundEvents.BLOCK_GRAVEL_PLACE, SoundCategory.BLOCKS, 0.7f, 0.9f + world.random.nextFloat() * 0.2f);
                        DirtPlaceCooldown = 2;
                    }
                }
            }
            DirtPlaceCooldown--;
        }
        super.tickMovement();
    }

    public int calcDiffY(){ // Calculates the height difference between the current and the next pathnode of the mob
        Path path = this.getNavigation().getCurrentPath();
        if(path == null || path.getCurrentNodeIndex() >= path.getLength()) return -1;
        if(path.getCurrentNodeIndex() > 0){
            int currentnodeposY = path.getCurrentNodePos().getY();
            int lastnodeposY = path.getNodePos(path.getCurrentNodeIndex() - 1).getY();

            return currentnodeposY - lastnodeposY;
        }
        else return -1;
    }

    private boolean canPlaceDirt (World world, BlockPos BlockUnder, BlockPos BlockUnder2){
        if(world.getBlockState(BlockUnder).isAir()){
            return world.getBlockState(BlockUnder2).isAir();
        }
        return world.getBlockState(BlockUnder).isReplaceable() && !world.getBlockState(BlockUnder).isAir();
    }

    private void setTicksUntilWaterConversion(int ticksUntilWaterConversion) {
        this.ticksUntilWaterConversion = ticksUntilWaterConversion;
        this.getDataTracker().set(CONVERTING_IN_WATER, true);
    }

    @Override
    public void setBaby(boolean baby) {
    }

    protected void convertInWater() {
    }

    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!super.damage(source, amount)) {
            return false;
        }
        if (!(this.getWorld() instanceof ServerWorld)) {
            return false;
        }
        ServerWorld serverWorld = (ServerWorld)this.getWorld();
        LivingEntity livingEntity = this.getTarget();
        if (livingEntity == null && source.getAttacker() instanceof LivingEntity) {
            livingEntity = (LivingEntity)source.getAttacker();
        }
        if (livingEntity != null && this.getWorld().getDifficulty() == Difficulty.HARD && (double)this.random.nextFloat() < this.getAttributeValue(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS) && this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
            int i = MathHelper.floor(this.getX());
            int j = MathHelper.floor(this.getY());
            int k = MathHelper.floor(this.getZ());
            net.minecraft.entity.mob.ZombieEntity zombieEntity = new net.minecraft.entity.mob.ZombieEntity(this.getWorld());
            for (int l = 0; l < 50; ++l) {
                int m = i + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                int n = j + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                int o = k + MathHelper.nextInt(this.random, 7, 40) * MathHelper.nextInt(this.random, -1, 1);
                BlockPos blockPos = new BlockPos(m, n, o);
                EntityType<?> entityType = zombieEntity.getType();
                SpawnRestriction.Location location = SpawnRestriction.getLocation(entityType);
                if (!SpawnHelper.canSpawn(location, this.getWorld(), blockPos, entityType) || !SpawnRestriction.canSpawn(entityType, serverWorld, SpawnReason.REINFORCEMENT, blockPos, this.getWorld().random)) continue;
                zombieEntity.setPosition(m, n, o);
                if (this.getWorld().isPlayerInRange(m, n, o, 7.0) || !this.getWorld().doesNotIntersectEntities(zombieEntity) || !this.getWorld().isSpaceEmpty(zombieEntity) || this.getWorld().containsFluid(zombieEntity.getBoundingBox())) continue;
                zombieEntity.setTarget(livingEntity);
                zombieEntity.initialize(serverWorld, this.getWorld().getLocalDifficulty(zombieEntity.getBlockPos()), SpawnReason.REINFORCEMENT, null, null);
                serverWorld.spawnEntityAndPassengers(zombieEntity);
                this.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS).addPersistentModifier(new EntityAttributeModifier("Zombie reinforcement caller charge", -0.05f, EntityAttributeModifier.Operation.ADDITION));
                zombieEntity.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS).addPersistentModifier(new EntityAttributeModifier("Zombie reinforcement callee charge", -0.05f, EntityAttributeModifier.Operation.ADDITION));
                break;
            }
        }
        return true;
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean bl = super.tryAttack(target);
        if (bl) {
            float f = this.getWorld().getLocalDifficulty(this.getBlockPos()).getLocalDifficulty();
            if (this.getMainHandStack().isEmpty() && this.isOnFire() && this.random.nextFloat() < f * 0.3f) {
                target.setOnFireFor(2 * (int)f);
            }
        }
        return bl;
    }

    public static boolean canSpawn(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random){
        int FullDaysRequired = 32;
        int currentAmountOfFullDays = (int) (world.getLevelProperties().getTimeOfDay() / 24000L);
        return (!world.getLevelProperties().getGameRules().getBoolean(ModGamerules.MOB_SPAWN_PROGRESSION) || currentAmountOfFullDays >= FullDaysRequired) && canSpawnInDark(type, world, spawnReason, pos, random);
    }


    protected void initEquipment() {
                this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIRT, 1));
        }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("CanBreakDoors", this.canBreakDoors());
        nbt.putInt("InWaterTime", this.isTouchingWater() ? this.inWaterTime : -1);
        nbt.putInt("DrownedConversionTime", this.isConvertingInWater() ? this.ticksUntilWaterConversion : -1);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setCanBreakDoors(nbt.getBoolean("CanBreakDoors"));
        this.inWaterTime = nbt.getInt("InWaterTime");
        if (nbt.contains("DrownedConversionTime", NbtElement.NUMBER_TYPE) && nbt.getInt("DrownedConversionTime") > -1) {
            this.setTicksUntilWaterConversion(nbt.getInt("DrownedConversionTime"));
        }
    }

    @Override
    public boolean onKilledOther(ServerWorld world, LivingEntity other) {
        boolean bl = super.onKilledOther(world, other);
        if ((world.getDifficulty() == Difficulty.NORMAL || world.getDifficulty() == Difficulty.HARD) && other instanceof VillagerEntity) {
            VillagerEntity villagerEntity = (VillagerEntity)other;
            if (world.getDifficulty() != Difficulty.HARD && this.random.nextBoolean()) {
                return bl;
            }
            ZombieVillagerEntity zombieVillagerEntity = villagerEntity.convertTo(EntityType.ZOMBIE_VILLAGER, false);
            if (zombieVillagerEntity != null) {
                zombieVillagerEntity.initialize(world, world.getLocalDifficulty(zombieVillagerEntity.getBlockPos()), SpawnReason.CONVERSION, new net.minecraft.entity.mob.ZombieEntity.ZombieData(false, true), null);
                zombieVillagerEntity.setVillagerData(villagerEntity.getVillagerData());
                zombieVillagerEntity.setGossipData(villagerEntity.getGossip().serialize(NbtOps.INSTANCE));
                zombieVillagerEntity.setOfferData(villagerEntity.getOffers().toNbt());
                zombieVillagerEntity.setXp(villagerEntity.getExperience());
                if (!this.isSilent()) {
                    world.syncWorldEvent(null, WorldEvents.ZOMBIE_INFECTS_VILLAGER, this.getBlockPos(), 0);
                }
                bl = false;
            }
        }
        return bl;
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return this.isBaby() ? 0.93f : 1.74f;
    }

    @Override
    public boolean canPickupItem(ItemStack stack) {
        if (stack.isOf(Items.EGG) && this.isBaby() && this.hasVehicle()) {
            return false;
        }
        return super.canPickupItem(stack);
    }

    @Override
    public boolean canGather(ItemStack stack) {
        if (stack.isOf(Items.GLOW_INK_SAC)) {
            return false;
        }
        return super.canGather(stack);
    }

    @Override
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        float f = difficulty.getClampedLocalDifficulty();
        this.setCanPickUpLoot(random.nextFloat() < 0.55f * f);
        if (entityData == null) {
            entityData = new BuilderZombieEntity.ZombieData(BuilderZombieEntity.shouldBeBaby(random), false);
        }
        if (entityData instanceof survivalplus.modid.entity.custom.BuilderZombieEntity.ZombieData) {
            survivalplus.modid.entity.custom.BuilderZombieEntity.ZombieData zombieData = (survivalplus.modid.entity.custom.BuilderZombieEntity.ZombieData)entityData;
            this.setCanBreakDoors(this.shouldBreakDoors() && random.nextFloat() < f * 0.1f);
            this.initEquipment();
        }

        if (this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
            LocalDate localDate = LocalDate.now();
            int i = localDate.get(ChronoField.DAY_OF_MONTH);
            int j = localDate.get(ChronoField.MONTH_OF_YEAR);
            if (j == 10 && i == 31 && random.nextFloat() < 0.25f) {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(random.nextFloat() < 0.1f ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
                this.armorDropChances[EquipmentSlot.HEAD.getEntitySlotId()] = 0.0f;
            }
            else{
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET, 1));
                this.armorDropChances[EquipmentSlot.HEAD.getEntitySlotId()] = 0.0f;
            }
        }
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Blocks.DIRT, 1));
        this.applyAttributeModifiers(f);
        return entityData;
    }

    public static boolean shouldBeBaby(Random random) {
        return false;
    }

    protected void applyAttributeModifiers(float chanceMultiplier) {
        this.initAttributes();
        this.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).addPersistentModifier(new EntityAttributeModifier("Random spawn bonus", this.random.nextDouble() * (double)0.05f, EntityAttributeModifier.Operation.ADDITION));
        double d = this.random.nextDouble() * 1.5 * (double)chanceMultiplier;
        if (d > 1.0) {
            this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).addPersistentModifier(new EntityAttributeModifier("Random zombie-spawn bonus", d, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (this.random.nextFloat() < chanceMultiplier * 0.05f) {
            this.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS).addPersistentModifier(new EntityAttributeModifier("Leader zombie bonus", this.random.nextDouble() * 0.25 + 0.5, EntityAttributeModifier.Operation.ADDITION));
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).addPersistentModifier(new EntityAttributeModifier("Leader zombie bonus", this.random.nextDouble() * 3.0 + 1.0, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            this.setCanBreakDoors(this.shouldBreakDoors());
        }
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        ItemStack itemStack;
        CreeperEntity creeperEntity;
        super.dropEquipment(source, lootingMultiplier, allowDrops);
        Entity entity = source.getAttacker();
        if (entity instanceof CreeperEntity && (creeperEntity = (CreeperEntity)entity).shouldDropHead() && !(itemStack = this.getSkull()).isEmpty()) {
            creeperEntity.onHeadDropped();
            this.dropStack(itemStack);
        }
    }


    class DestroyEggGoal
            extends StepAndDestroyBlockGoal {
        DestroyEggGoal(PathAwareEntity mob, double speed, int maxYDifference) {
            super(Blocks.TURTLE_EGG, mob, speed, maxYDifference);
        }

        @Override
        public void tickStepping(WorldAccess world, BlockPos pos) {
            world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_DESTROY_EGG, SoundCategory.HOSTILE, 0.5f, 0.9f + survivalplus.modid.entity.custom.BuilderZombieEntity.this.random.nextFloat() * 0.2f);
        }

        @Override
        public void onDestroyBlock(World world, BlockPos pos) {
            world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7f, 0.9f + world.random.nextFloat() * 0.2f);
        }

        @Override
        public double getDesiredDistanceToTarget() {
            return 1.14;
        }
    }

    public static class ZombieData
            implements EntityData {
        public final boolean baby;
        public final boolean tryChickenJockey;

        public ZombieData(boolean baby, boolean tryChickenJockey) {
            this.baby = baby;
            this.tryChickenJockey = tryChickenJockey;
        }
    }
}

