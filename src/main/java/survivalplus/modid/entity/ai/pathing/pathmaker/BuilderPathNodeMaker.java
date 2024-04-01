package survivalplus.modid.entity.ai.pathing.pathmaker;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.Nullable;

public class BuilderPathNodeMaker extends LandPathNodeMaker {

    private final Object2BooleanMap<Box> collidedBoxes = new Object2BooleanOpenHashMap<Box>();

    private double getStepHeight() {
        return Math.max(1.125, (double) this.entity.getStepHeight());
    }

    private PathNode getNodeWith(int x, int y, int z, PathNodeType type, float penalty) {
        PathNode pathNode = this.getNode(x, y, z);
        pathNode.type = type;
        pathNode.penalty = Math.max(pathNode.penalty, penalty);
        return pathNode;
    }

    private static boolean isBlocked(PathNodeType nodeType) {
        return nodeType == PathNodeType.FENCE || nodeType == PathNodeType.DOOR_WOOD_CLOSED || nodeType == PathNodeType.DOOR_IRON_CLOSED;
    }

    private boolean isBlocked(PathNode node) {
        Box box = this.entity.getBoundingBox();
        Vec3d vec3d = new Vec3d((double) node.x - this.entity.getX() + box.getLengthX() / 2.0, (double) node.y - this.entity.getY() + box.getLengthY() / 2.0, (double) node.z - this.entity.getZ() + box.getLengthZ() / 2.0);
        int i = MathHelper.ceil(vec3d.length() / box.getAverageSideLength());
        vec3d = vec3d.multiply(1.0f / (float) i);
        for (int j = 1; j <= i; ++j) {
            if (!this.checkBoxCollision(box = box.offset(vec3d))) continue;
            return false;
        }
        return true;
    }

    private PathNode getBlockedNode(int x, int y, int z) {
        PathNode pathNode = this.getNode(x, y, z);
        pathNode.type = PathNodeType.BLOCKED;
        pathNode.penalty = -1.0f;
        return pathNode;
    }

    private boolean checkBoxCollision(Box box) {
        return this.collidedBoxes.computeIfAbsent(box, box2 -> !this.cachedWorld.isSpaceEmpty(this.entity, box));
    }

    @Override
    @Nullable
    protected PathNode getPathNode(int x, int y, int z, int maxYStep, double prevFeetY, Direction direction, PathNodeType nodeType) {
        if (this.entity.getTarget() != null) {
            double h;
            double g;
            Box box;
            PathNode pathNode = null;
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            double d = this.getFeetY(mutable.set(x, y, z));
            if (d - prevFeetY > this.getStepHeight()) {
                return null;
            }
            PathNodeType pathNodeType = this.getNodeType(this.entity, x, y, z);
            float f = this.entity.getPathfindingPenalty(pathNodeType);
            double e = (double) this.entity.getWidth() / 2.0;
            if (f >= 0.0f) {
                pathNode = this.getNodeWith(x, y, z, pathNodeType, f);
            }
            if (isBlocked(nodeType) && pathNode != null && pathNode.penalty >= 0.0f && !this.isBlocked(pathNode)) {
                pathNode = null;
            }
            if (pathNodeType == PathNodeType.WALKABLE || this.isAmphibious() && pathNodeType == PathNodeType.WATER) {
                return pathNode;
            }
            if ((pathNode == null || pathNode.penalty < 0.0f) && maxYStep > 0 && (pathNodeType != PathNodeType.FENCE || this.canWalkOverFences()) && pathNodeType != PathNodeType.UNPASSABLE_RAIL && pathNodeType != PathNodeType.TRAPDOOR && pathNodeType != PathNodeType.POWDER_SNOW && (pathNode = this.getPathNode(x, y + 1, z, maxYStep - 1, prevFeetY, direction, nodeType)) != null && (pathNode.type == PathNodeType.OPEN || pathNode.type == PathNodeType.WALKABLE) && this.entity.getWidth() < 1.0f && this.checkBoxCollision(box = new Box((g = (double) (x - direction.getOffsetX()) + 0.5) - e, this.getFeetY(mutable.set(g, (double) (y + 1), h = (double) (z - direction.getOffsetZ()) + 0.5)) + 0.001, h - e, g + e, (double) this.entity.getHeight() + this.getFeetY(mutable.set((double) pathNode.x, (double) pathNode.y, (double) pathNode.z)) - 0.002, h + e))) {
                pathNode = null;
            }
            if (!this.isAmphibious() && pathNodeType == PathNodeType.WATER && !this.canSwim()) {
                if (this.getNodeType(this.entity, x, y - 1, z) != PathNodeType.WATER) {
                    return pathNode;
                }
                while (y > this.entity.getWorld().getBottomY()) {
                    if ((pathNodeType = this.getNodeType(this.entity, x, --y, z)) == PathNodeType.WATER) {
                        pathNode = this.getNodeWith(x, y, z, pathNodeType, this.entity.getPathfindingPenalty(pathNodeType));
                        continue;
                    }
                    return pathNode;
                }
            }
            if (pathNodeType == PathNodeType.OPEN) {
                BlockPos pathNodeBelowBlockPos = this.entity.getBlockPos().down();
                if (this.getNodeType(this.entity, pathNodeBelowBlockPos) == PathNodeType.OPEN) {
                    return this.getNodeWith(x, y, z, PathNodeType.WALKABLE, 0.0f);
                }
            }
            if (isBlocked(pathNodeType) && pathNode == null) {
                pathNode = this.getNode(x, y, z);
                pathNode.visited = true;
                pathNode.type = pathNodeType;
                pathNode.penalty = pathNodeType.getDefaultPenalty();
            }
            return pathNode;
        } else return super.getPathNode(x, y, z, maxYStep, prevFeetY, direction, nodeType);
    }

}
