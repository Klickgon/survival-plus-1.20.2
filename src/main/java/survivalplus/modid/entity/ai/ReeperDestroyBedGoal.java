package survivalplus.modid.entity.ai;


import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.ai.goal.StepAndDestroyBlockGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import survivalplus.modid.entity.ai.pathing.ReeperNavigation;
import survivalplus.modid.entity.custom.ReeperEntity;

import java.util.EnumSet;

public class ReeperDestroyBedGoal extends MoveToTargetPosGoal {
    private final TagKey<Block> BedGroup = BlockTags.BEDS;
    private final ReeperEntity reeper;



    public ReeperDestroyBedGoal(ReeperEntity reeper, double speed) {
        super(reeper, speed, 256);
        this.reeper = reeper;
        this.cooldown = 600;
    }

    public boolean canStart() {
        if (this.cooldown > 0){
            --this.cooldown;
            return false;
        }
        this.cooldown = 600;

        if (!this.reeper.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            return false;
        }

        if (this.findTargetPos()) {
            return true;
        }

        return false;
    }

    @Override
    public void stop() {
        super.stop();
        this.reeper.fallDistance = 1.0f;
    }

    @Override
    public void start() {
        super.start();
    }


    @Override
    public double getDesiredDistanceToTarget() {
        return 2.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!targetPos.isWithinDistance(this.reeper.getBlockPos(), 3.0)) {
                this.reeper.setFuseSpeed(-1);
        }
        else this.reeper.setFuseSpeed(1);
    }

    @Override
    protected void startMovingToTarget() {
        EntityNavigation nav = new ReeperNavigation(this.reeper, this.reeper.getWorld());
        nav.startMovingTo((double)this.targetPos.getX() + 0.5, this.targetPos.getY() + 1, (double)this.targetPos.getZ() + 0.5, this.speed);
    }


    @Override
    protected boolean isTargetPos(WorldView world, BlockPos pos) {
        Chunk chunk = world.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
        if (chunk != null) {
            return chunk.getBlockState(pos).isIn(this.BedGroup);
        }
        return false;
    }

}

