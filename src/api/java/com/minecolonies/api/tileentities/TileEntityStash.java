package com.minecolonies.api.tileentities;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import static com.minecolonies.api.util.constant.BuildingConstants.MAX_PRIO;
//import static com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable.getPlayerActionPriority;
import static com.minecolonies.api.util.constant.Constants.DEFAULT_SIZE;

/**
 * Class which handles the tileEntity for the Stash block.
 */
public class TileEntityStash extends TileEntityColonyBuilding
{

    /**
     * Constructor of the stash based on a tile entity type
     *
     * @param registryName the registry name of the building.
     */
    public TileEntityStash(final ResourceLocation registryName)
    {
        super(registryName);
    }

    /**
     * Default constructor used to create a new TileEntity via reflection. Do not use.
     */ //I guess?
    public TileEntityStash()
    {
        super();
    }



    @Override
    public ItemStackHandler createInventory(final int slots)
    {
        return new NotifyingRackInventory(slots);
    }

    /**
     * An {@link ItemStackHandler} that notifies the container TileEntity when it's inventory has changed.
     */
    public class NotifyingRackInventory extends RackInventory
    {
        public NotifyingRackInventory(final int defaultSize)
        {
            super(defaultSize);
        }

        @Override
        protected void onContentsChanged(final int slot)
        {
            super.onContentsChanged(slot);
            if(world != null && !world.isRemote && IColonyManager.getInstance().isCoordinateInAnyColony(world, pos)){
                final IColony colony = IColonyManager.getInstance().getClosestColony(world, pos);
                if (colony != null)
                {
                    final IBuilding building = colony.getBuildingManager().getBuilding(pos);

                    if (building instanceof IBuilding && !building.isPriorityStatic())
                    {
                        if(!isEmpty()){
                            if (!building.isBeingGathered())
                            {
                                building.alterPickUpPriority(MAX_PRIO);
                            }
                        }//else{
//                        if (building.isBeingGathered())
//                        {
//                            building.alterPickUpPriority(1);
//                        }
//                    }

                    }
                }

            }

        }
    }
}