package com.minecolonies.coremod.blocks.huts;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.tileentities.TileEntityColonyBuilding;
import com.minecolonies.api.tileentities.TileEntityStash;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.network.messages.OpenInventoryMessage;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hut for the Stash. No different from {@link AbstractBlockHut}
 */
public class BlockStash extends AbstractBlockHut<BlockStash>
{

//    private static final VoxelShape SHAPE_NORTH = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 8.0D);
//    private static final VoxelShape SHAPE_EAST  = Block.makeCuboidShape(8.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
//    private static final VoxelShape SHAPE_SOUTH = Block.makeCuboidShape(0.0D, 0.0D, 8.0D, 16.0D, 16.0D, 16.0D);
//    private static final VoxelShape SHAPE_WEST  = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 8.0D, 16.0D, 16.0D);

    @NotNull
    @Override
    public String getName()
    {
        return "blockstash";
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new TileEntityStash(this.getBuildingEntry().getRegistryName());
    }

//    @Override
//    public boolean hasTileEntity(final IBlockState state)
//    {
//        return true;
//    }

    @Override
    public BuildingEntry getBuildingEntry()
    {
        return ModBuildings.stash;
    }

//    @NotNull
//    @Override
//    public VoxelShape getShape(
//            final BlockState state, final IBlockReader worldIn, final BlockPos pos, final ISelectionContext context)
//    {
//        switch (state.get(FACING))
//        {
//            case NORTH:
//                return SHAPE_NORTH;
//            case SOUTH:
//                return SHAPE_SOUTH;
//            case EAST:
//                return SHAPE_EAST;
//            default:
//                return SHAPE_WEST;
//        }
//    }

    @Override
    public boolean onBlockActivated(
            final World worldIn,
            final BlockPos pos,
            final IBlockState state,
            final EntityPlayer playerIn,
            final EnumHand hand,
            final EnumFacing facing,
            final float hitX,
            final float hitY,
            final float hitZ)
    {
        if (worldIn.isRemote)
        {
            @Nullable final IBuildingView building = IColonyManager.getInstance().getBuildingView(worldIn.provider.getDimension(), pos);

            if (building != null
                    && building.getColony() != null
                    && building.getColony().getPermissions().hasPermission(playerIn, Action.ACCESS_HUTS))
            {
//                Network.getNetwork().sendToServer(new OpenInventoryMessage(building));
                MineColonies.getNetwork().sendToServer(new OpenInventoryMessage(building.getID()));
//                building.openGui(true);
            }
        }
        return true;
    }
}