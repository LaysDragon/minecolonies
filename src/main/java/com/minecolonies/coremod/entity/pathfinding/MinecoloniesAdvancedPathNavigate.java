package com.minecolonies.coremod.entity.pathfinding;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.mobs.pirates.AbstractEntityPirate;
import com.minecolonies.api.entity.pathfinding.*;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.BlockUtils;
import com.minecolonies.api.util.CompatibilityUtils;
import com.minecolonies.api.util.Log;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Minecolonies async PathNavigate.
 */
public class MinecoloniesAdvancedPathNavigate extends AbstractAdvancedPathNavigate
{
    private static final double ON_PATH_SPEED_MULTIPLIER = 1.3D;
    private static final double PIRATE_SWIM_BONUS        = 30;
    public static final  double MIN_Y_DISTANCE           = 0.001;
    public static final  int    MAX_SPEED_ALLOWED        = 100;

    /**
     * Amount of ticks before vanilla stuck handling is allowed to discard an existing path
     */
    private static final long MIN_KEEP_TIME = 100;

    /**
     * The world time when a path was added.
     */
    private long pathStartTime = 0;

    @Nullable
    private PathResult pathResult;

    /**
     * Instantiates the navigation of an ourEntity.
     *
     * @param entity the ourEntity.
     * @param world  the world it is in.
     */
    public MinecoloniesAdvancedPathNavigate(@NotNull final EntityLiving entity, final World world)
    {
        super(entity, world, entity);

        this.nodeProcessor = new WalkNodeProcessor();
        this.nodeProcessor.setCanEnterDoors(true);
        this.nodeProcessor.setCanOpenDoors(true);
        this.nodeProcessor.setCanSwim(true);
    }

    /**
     * Get the destination from the path.
     *
     * @return the destination position.
     */
    public BlockPos getDestination()
    {
        return destination;
    }

    /**
     * Used to path away from a position.
     *
     * @param avoid the position to avoid.
     * @param range the range he should move out of.
     * @param speed the speed to run at.
     * @return the result of the pathing.
     */
    @Nullable
    public PathResult moveAwayFromXYZ(final BlockPos avoid, final double range, final double speed)
    {
        @NotNull final BlockPos start = AbstractPathJob.prepareStart(ourEntity);

        return setPathJob(
          new PathJobMoveAwayFromLocation(CompatibilityUtils.getWorldFromEntity(ourEntity), start, avoid, (int) range, (int) getPathSearchRange(), ourEntity),
          null, speed);
    }

    @Nullable
    public PathResult setPathJob(
      @NotNull final AbstractPathJob job,
      final BlockPos dest,
      final double speed)
    {
        clearPath();

        this.destination = dest;
        this.originalDestination = dest;
        this.walkSpeed = speed;

        if (speed > MAX_SPEED_ALLOWED)
        {
            Log.getLogger().error("Tried to set a too high speed for entity:" + ourEntity, new Exception());
            return null;
        }


        calculationFuture = Pathfinding.enqueue(job);
        pathResult = job.getResult();
        return pathResult;
    }

    @Override
    public void onUpdateNavigation()
    {
        if (calculationFuture != null)
        {
            if (!calculationFuture.isDone())
            {
                return;
            }

            try
            {
                if (processCompletedCalculationResult())
                {
                    return;
                }
            }
            catch (@NotNull InterruptedException | ExecutionException e)
            {
                Log.getLogger().catching(e);
            }

            calculationFuture = null;
        }

        int oldIndex = this.noPath() ? 0 : this.getPath().getCurrentPathIndex();
        super.onUpdateNavigation();

        if (handleLadders(oldIndex))
        {
            return;
        }

        if (pathResult != null && noPath())
        {
            pathResult.setStatus(PathFindingStatus.COMPLETE);
            pathResult = null;
        }
    }

    /**
     * Try to move to a certain position.
     *
     * @param x     the x target.
     * @param y     the y target.
     * @param z     the z target.
     * @param speed the speed to walk.
     * @return the PathResult.
     */
    @Nullable
    public PathResult moveToXYZ(final double x, final double y, final double z, final double speed)
    {
        final int newX = MathHelper.floor(x);
        final int newY = (int) y;
        final int newZ = MathHelper.floor(z);

        if (pathResult != null &&
              (
                pathResult.isComputing()
                  || (destination != null && BlockPosUtil.isEqual(destination, newX, newY, newZ))
                  || (originalDestination != null && BlockPosUtil.isEqual(originalDestination, newX, newY, newZ))
              )
        )
        {
            return pathResult;
        }

        @NotNull final BlockPos start = AbstractPathJob.prepareStart(ourEntity);
        @NotNull final BlockPos dest = new BlockPos(newX, newY, newZ);

        return setPathJob(
          new PathJobMoveToLocation(CompatibilityUtils.getWorldFromEntity(ourEntity), start, dest, (int) getPathSearchRange(), ourEntity),
          dest, speed);
    }

    public boolean tryMoveToBlockPos(final BlockPos pos, final double speed)
    {
        moveToXYZ(pos.getX(), pos.getY(), pos.getZ(), speed);
        return true;
    }

    @Nullable
    @Override
    protected PathFinder getPathFinder()
    {
        return null;
    }

    @Override
    protected boolean canNavigate()
    {
        // Auto dismount when trying to path.
        if (ourEntity.isRiding())
        {
            ourEntity.dismountRidingEntity();
        }
        return true;
    }

    @NotNull
    @Override
    protected Vec3d getEntityPosition()
    {
        return this.ourEntity.getPositionVector();
    }

    @Nullable
    @Override
    public Path getPathToPos(@NotNull final BlockPos pos)
    {
        //Because this directly returns Path we can't do it async.
        return null;
    }

    @Override
    protected boolean isDirectPathBetweenPoints(final Vec3d start, final Vec3d end, final int sizeX, final int sizeY, final int sizeZ)
    {
        // TODO improve road walking. This is better in some situations, but still not great.
        return !BlockUtils.isPathBlock(world.getBlockState(new BlockPos(start.x, start.y - 1, start.z)).getBlock())
                 && super.isDirectPathBetweenPoints(start, end, sizeX, sizeY, sizeZ);
    }

    public double getSpeed()
    {
        if (ourEntity instanceof AbstractEntityPirate && ourEntity.isInWater())
        {
            speed = walkSpeed * PIRATE_SWIM_BONUS;
            return walkSpeed * PIRATE_SWIM_BONUS;
        }
        speed = walkSpeed;
        return walkSpeed;
    }

    @Override
    public void setSpeed(final double d)
    {
        if (d > MAX_SPEED_ALLOWED)
        {
            Log.getLogger().error("Tried to set a too high speed for entity:" + ourEntity, new Exception());
            return;
        }
        walkSpeed = d;
    }

    /**
     * Deprecated - try to use BlockPos instead
     */
    @Override
    public boolean tryMoveToXYZ(final double x, final double y, final double z, final double speed)
    {
        if (x == 0 && y == 0 && z == 0)
        {
            return false;
        }

        moveToXYZ(x, y, z, speed);
        return true;
    }

    @Override
    public boolean tryMoveToEntityLiving(@NotNull final Entity e, final double speed)
    {
        return tryMoveToBlockPos(e.getPosition(), speed);
    }

    // Removes stupid vanilla stuff, causing our pathpoints to occasionally be replaced by vanilla ones.
    @Override
    protected void removeSunnyPath() {}

    @Override
    public boolean setPath(@Nullable final Path path, final double speed)
    {
        if (path == null)
        {
            this.currentPath = null;
            return false;
        }
        pathStartTime = world.getWorldTime();
        return super.setPath(convertPath(path), speed);
    }

    /**
     * Converts the given path to a minecolonies path if needed.
     *
     * @param path given path
     * @return resulting path
     */
    private Path convertPath(final Path path)
    {
        final int pathLength = path.getCurrentPathLength();
        Path tempPath = null;
        if (pathLength > 0)
        {
            //  Fix vanilla PathPoints to be PathPointExtended
            @NotNull final PathPointExtended[] newPoints = new PathPointExtended[pathLength];

            for (int i = 0; i < pathLength; ++i)
            {
                final PathPoint point = path.getPathPointFromIndex(i);
                if (!(point instanceof PathPointExtended))
                {
                    newPoints[i] = new PathPointExtended(new BlockPos(point.x, point.y, point.z));
                }
                else
                {
                    newPoints[i] = (PathPointExtended) point;
                }
            }

            tempPath = new Path(newPoints);

            final PathPointExtended finalPoint = newPoints[pathLength - 1];
            destination = new BlockPos(finalPoint.x, finalPoint.y, finalPoint.z);
        }

        return tempPath == null ? path : tempPath;
    }

    private boolean processCompletedCalculationResult() throws InterruptedException, ExecutionException
    {
        if (calculationFuture.get() == null)
        {
            calculationFuture = null;
            return true;
        }

        setPath(calculationFuture.get(), getSpeed());

        pathResult.setPathLength(getPath().getCurrentPathLength());
        pathResult.setStatus(PathFindingStatus.IN_PROGRESS_FOLLOWING);

        final PathPoint p = getPath().getFinalPathPoint();
        if (p != null && destination == null)
        {
            destination = new BlockPos(p.x, p.y, p.z);

            //  AbstractPathJob with no destination, did reach it's destination
            pathResult.setPathReachesDestination(true);
        }
        return false;
    }

    private boolean handleLadders(int oldIndex)
    {
        //  Ladder Workaround
        if (!this.noPath())
        {
            @NotNull final PathPointExtended pEx = (PathPointExtended) this.getPath().getPathPointFromIndex(this.getPath().getCurrentPathIndex());
            final PathPointExtended pExNext = getPath().getCurrentPathLength() > this.getPath().getCurrentPathIndex() + 1 ?
                                                (PathPointExtended) this.getPath().getPathPointFromIndex(this.getPath().getCurrentPathIndex() + 1)
                                                : null;

            if (pEx.isOnLadder() && (pExNext != null && pEx.y != pExNext.y))
            {
                handlePathPointOnLadder(pEx);
            }
            else if (ourEntity.isInWater())
            {
                return handleEntityInWater(oldIndex, pEx);
            }
            else
            {
                if (BlockUtils.isPathBlock(world.getBlockState(ourEntity.getPosition().down()).getBlock()))
                {
                    speed = ON_PATH_SPEED_MULTIPLIER * getSpeed();
                }
                else
                {
                    speed = getSpeed();
                }
            }
        }
        return false;
    }

    private void handlePathPointOnLadder(final PathPointExtended pEx)
    {
        Vec3d vec3 = this.getPath().getPosition(this.ourEntity);

        if (vec3.squareDistanceTo(ourEntity.posX, vec3.y, ourEntity.posZ) < Math.random() * 0.1)
        {
            //This way he is less nervous and gets up the ladder
            double newSpeed = 0.05;
            switch (pEx.getLadderFacing())
            {
                //  Any of these values is climbing, so adjust our direction of travel towards the ladder
                case NORTH:
                    vec3 = vec3.add(0, 0, 1);
                    break;
                case SOUTH:
                    vec3 = vec3.add(0, 0, -1);
                    break;
                case WEST:
                    vec3 = vec3.add(1, 0, 0);
                    break;
                case EAST:
                    vec3 = vec3.add(-1, 0, 0);
                    break;
                //  Any other value is going down, so lets not move at all
                default:
                    newSpeed = 0;
                    break;
            }

            this.ourEntity.getMoveHelper().setMoveTo(vec3.x, vec3.y, vec3.z, newSpeed);
        }
    }

    private boolean handleEntityInWater(int oldIndex, final PathPointExtended pEx)
    {
        //  Prevent shortcuts when swimming
        final int curIndex = this.getPath().getCurrentPathIndex();
        if (curIndex > 0
              && (curIndex + 1) < this.getPath().getCurrentPathLength()
              && this.getPath().getPathPointFromIndex(curIndex - 1).y != pEx.y)
        {
            //  Work around the initial 'spin back' when dropping into water
            oldIndex = curIndex + 1;
        }

        this.getPath().setCurrentPathIndex(oldIndex);

        Vec3d vec3d = this.getPath().getPosition(this.ourEntity);

        if (vec3d.squareDistanceTo(new Vec3d(ourEntity.posX, vec3d.y, ourEntity.posZ)) < 0.1
              && Math.abs(ourEntity.posY - vec3d.y) < 0.5)
        {
            this.getPath().setCurrentPathIndex(this.getPath().getCurrentPathIndex() + 1);
            if (this.noPath())
            {
                return true;
            }

            vec3d = this.getPath().getPosition(this.ourEntity);
        }

        ourEntity.setAIMoveSpeed((float) getSpeed());
        this.ourEntity.getMoveHelper().setMoveTo(vec3d.x, vec3d.y, vec3d.z, getSpeed());
        return false;
    }

    @Override
    protected void pathFollow()
    {
        final int curNode = currentPath.getCurrentPathIndex();
        final int curNodeNext = curNode + 1;
        if (curNodeNext < currentPath.getCurrentPathLength())
        {
            final PathPoint pNormal = currentPath.getPathPointFromIndex(curNode);
            final PathPoint pNormalNext = currentPath.getPathPointFromIndex(curNodeNext);

            if (!(pNormal instanceof PathPointExtended))
            {
                currentPath.setPoint(curNode, new PathPointExtended(new BlockPos(pNormal.x, pNormal.y, pNormal.z)));
            }
            if (!(pNormalNext instanceof PathPointExtended))
            {
                currentPath.setPoint(curNode, new PathPointExtended(new BlockPos(pNormalNext.x, pNormalNext.y, pNormalNext.z)));
            }

            final PathPointExtended pEx = (PathPointExtended) currentPath.getPathPointFromIndex(curNode);
            final PathPointExtended pExNext = (PathPointExtended) currentPath.getPathPointFromIndex(curNodeNext);

            //  If current node is bottom of a ladder, then stay on this node until
            //  the ourEntity reaches the bottom, otherwise they will try to head out early
            if (pEx.isOnLadder() && pEx.getLadderFacing() == EnumFacing.DOWN
                  && !pExNext.isOnLadder())
            {
                final Vec3d vec3 = getEntityPosition();
                if ((vec3.y - (double) pEx.y) < MIN_Y_DISTANCE)
                {
                    this.currentPath.setCurrentPathIndex(curNodeNext);
                }

                this.checkForStuck(vec3);
                return;
            }
        }

        super.pathFollow();
    }

    /**
     * If null path or reached the end.
     */
    @Override
    public boolean noPath()
    {
        return calculationFuture == null && super.noPath();
    }

    @Override
    public void clearPath()
    {
        if (calculationFuture != null)
        {
            calculationFuture.cancel(true);
            calculationFuture = null;
        }

        if (pathResult != null)
        {
            pathResult.setStatus(PathFindingStatus.CANCELLED);
            pathResult = null;
        }

        destination = null;
        super.clearPath();
    }

    /**
     * Don't let vanilla rapidly discard paths, set a timeout before its allowed to use stuck.
     *
     * @param positionVec3
     */
    @Override
    protected void checkForStuck(Vec3d positionVec3)
    {
        if (world.getWorldTime() - pathStartTime < MIN_KEEP_TIME)
        {
            return;
        }

        super.checkForStuck(positionVec3);
    }

    /**
     * Used to find a water.
     *
     * @param range in the range.
     * @param speed walking speed.
     * @param ponds a list of ponds.
     * @return the result of the search.
     */
    @Nullable
    public WaterPathResult moveToWater(final int range, final double speed, final List<BlockPos> ponds)
    {
        @NotNull final BlockPos start = AbstractPathJob.prepareStart(ourEntity);
        return (WaterPathResult) setPathJob(
          new PathJobFindWater(CompatibilityUtils.getWorldFromEntity(ourEntity),
            start,
            ((AbstractEntityCitizen) ourEntity).getCitizenColonyHandler().getWorkBuilding().getPosition(),
            range,
            ponds,
            ourEntity), null, speed);
    }

    /**
     * Used to find a tree.
     *
     * @param startRestriction the start of the restricted area.
     * @param endRestriction the end of the restricted area.
     * @param speed      walking speed.
     * @param treesToCut the trees which should be cut.
     * @return the result of the search.
     */
    public TreePathResult moveToTree(final BlockPos startRestriction, final BlockPos endRestriction, final double speed, final List<ItemStorage> treesToCut, final IColony colony)
    {
        @NotNull final BlockPos start = AbstractPathJob.prepareStart(ourEntity);
        final BlockPos buildingPos = ((AbstractEntityCitizen) entity).getCitizenColonyHandler().getWorkBuilding().getPosition();

        final PathJobFindTree job = new PathJobFindTree(CompatibilityUtils.getWorldFromEntity(entity), start, buildingPos, startRestriction, endRestriction, treesToCut, colony, ourEntity);

        return (TreePathResult) setPathJob(job, null, speed);
    }

    /**
     * Used to find a tree.
     *
     * @param range      in the range.
     * @param speed      walking speed.
     * @param treesToCut the trees which should be cut.
     * @return the result of the search.
     */
    public TreePathResult moveToTree(final int range, final double speed, final List<ItemStorage> treesToCut, final IColony colony)
    {
        @NotNull BlockPos start = AbstractPathJob.prepareStart(ourEntity);
        final BlockPos buildingPos = ((AbstractEntityCitizen) entity).getCitizenColonyHandler().getWorkBuilding().getPosition();

        if (BlockPosUtil.getDistance2D(buildingPos, entity.getPosition()) > range * 4)
        {
            start = buildingPos;
        }

        return (TreePathResult) setPathJob(
          new PathJobFindTree(CompatibilityUtils.getWorldFromEntity(entity), start, buildingPos, range, treesToCut, colony, ourEntity), null, speed);
    }

    /**
     * Used to move a living ourEntity with a speed.
     *
     * @param e     the ourEntity.
     * @param speed the speed.
     * @return the result.
     */
    @Nullable
    public PathResult moveToEntityLiving(@NotNull final Entity e, final double speed)
    {
        return moveToXYZ(e.posX, e.posY, e.posZ, speed);
    }

    /**
     * Used to path away from a ourEntity.
     *
     * @param e        the ourEntity.
     * @param distance the distance to move to.
     * @param speed    the speed to run at.
     * @return the result of the pathing.
     */
    @Nullable
    public PathResult moveAwayFromEntityLiving(@NotNull final Entity e, final double distance, final double speed)
    {
        return moveAwayFromXYZ(e.getPosition(), distance, speed);
    }
}
