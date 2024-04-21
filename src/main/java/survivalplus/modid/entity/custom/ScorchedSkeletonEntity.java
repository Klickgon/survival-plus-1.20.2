/*
 * Decompiled with CFR 0.2.1 (FabricMC 53fa44c9).
 */
package survivalplus.modid.entity.custom;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import survivalplus.modid.enchantments.ModEnchantments;
import survivalplus.modid.entity.ai.DestroyBedGoal;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

public class ScorchedSkeletonEntity
extends SkeletonEntity {
    private static final TrackedData<Boolean> CONVERTING = DataTracker.registerData(ScorchedSkeletonEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    public static final String STRAY_CONVERSION_TIME_KEY = "StrayConversionTime";
    private int conversionTime;

    public ScorchedSkeletonEntity(EntityType<? extends ScorchedSkeletonEntity> entityType, World world) {
        super((EntityType<? extends SkeletonEntity>)entityType, world);

    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(2, new AvoidSunlightGoal(this));
        this.goalSelector.add(3, new EscapeSunlightGoal(this, 1.0));
        this.goalSelector.add(3, new FleeEntityGoal<WolfEntity>(this, WolfEntity.class, 6.0f, 1.0, 1.2));
        this.goalSelector.add(4, new DestroyBedGoal(this, 1.0, 8));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(2, new ActiveTargetGoal<PlayerEntity>((MobEntity)this, PlayerEntity.class, false));
        this.targetSelector.add(3, new ActiveTargetGoal<IronGolemEntity>((MobEntity)this, IronGolemEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<TurtleEntity>(this, TurtleEntity.class, 10, true, false, TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(CONVERTING, false);
    }

    @Override
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        this.updateEnchantments(random, difficulty);
        this.updateAttackType();
        this.isFireImmune();
        this.setCanPickUpLoot(random.nextFloat() < 0.55f * difficulty.getClampedLocalDifficulty());
        if (this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
            LocalDate localDate = LocalDate.now();
            int i = localDate.get(ChronoField.DAY_OF_MONTH);
            int j = localDate.get(ChronoField.MONTH_OF_YEAR);
            if (j == 10 && i == 31 && random.nextFloat() < 0.25f) {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(random.nextFloat() < 0.1f ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
                this.armorDropChances[EquipmentSlot.HEAD.getEntitySlotId()] = 0.0f;
            }
            else this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));

            this.armorDropChances[EquipmentSlot.HEAD.getEntitySlotId()] = 0.0f;
        }
        this.handDropChances[EquipmentSlot.MAINHAND.getEntitySlotId()] = 0.0f;
        return entityData;
    }

    public static boolean canSpawn(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random){
        int FullDaysRequired = 51;
        long currentAmountofFullDays = (world.getLevelProperties().getTimeOfDay() / 24000L);
        return currentAmountofFullDays >= FullDaysRequired && canSpawnInDark(type, world, spawnReason, pos, random);
    }

    @Override
    protected void updateEnchantments(Random random, LocalDifficulty localDifficulty) {
        float f = localDifficulty.getClampedLocalDifficulty();
        this.enchantMainHandItem(random, f);
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot.getType() != EquipmentSlot.Type.ARMOR) continue;
            this.enchantEquipment(random, f, equipmentSlot);
        }
    }

    @Override
    protected void enchantMainHandItem(Random random, float power) {
        if (this.getMainHandStack().isOf(Items.BOW)) {
            ItemStack bow = this.getMainHandStack();
            bow.addEnchantment(ModEnchantments.FLAME_TWO, 2);
            this.equipStack(EquipmentSlot.MAINHAND, bow);
        }
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    @Override
    public boolean isConverting() {
        return this.getDataTracker().get(CONVERTING);
    }

    @Override
    public void setConverting(boolean converting) {
    }

    @Override
    public boolean isShaking() {
        return this.isConverting();
    }


    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt(STRAY_CONVERSION_TIME_KEY, this.isConverting() ? this.conversionTime : -1);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains(STRAY_CONVERSION_TIME_KEY, NbtElement.NUMBER_TYPE) && nbt.getInt(STRAY_CONVERSION_TIME_KEY) > -1) {
            this.setConversionTime(nbt.getInt(STRAY_CONVERSION_TIME_KEY));
        }
    }

    private void setConversionTime(int time) {
        this.conversionTime = time;
        this.setConverting(true);
    }


    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropEquipment(source, lootingMultiplier, allowDrops);
    }
}
