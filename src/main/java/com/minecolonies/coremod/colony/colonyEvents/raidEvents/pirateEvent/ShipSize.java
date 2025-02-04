package com.minecolonies.coremod.colony.colonyEvents.raidEvents.pirateEvent;

import com.minecolonies.api.util.constant.ColonyConstants;
import net.minecraft.util.ResourceLocation;

/**
 * Enum for ship sizes.
 */
public enum ShipSize
{
    SMALL(ColonyConstants.SMALL_HORDE_SIZE, ColonyConstants.SMALL_PIRATE_SHIP, ColonyConstants.SMALL_HORDE_MESSAGE_ID, 1, ColonyConstants.PIRATE, ColonyConstants.PIRATE),
    MEDIUM(ColonyConstants.MEDIUM_HORDE_SIZE,
      ColonyConstants.MEDIUM_PIRATE_SHIP,
      ColonyConstants.MEDIUM_HORDE_MESSAGE_ID,
      3,
      ColonyConstants.PIRATE,
      ColonyConstants.PIRATE_ARCHER,
      ColonyConstants.PIRATE_CHIEF),
    BIG(ColonyConstants.BIG_HORDE_SIZE,
      ColonyConstants.BIG_PIRATE_SHIP,
      ColonyConstants.BIG_HORDE_MESSAGE_ID,
      11,
      ColonyConstants.PIRATE,
      ColonyConstants.PIRATE,
      ColonyConstants.PIRATE_ARCHER,
      ColonyConstants.PIRATE_ARCHER,
      ColonyConstants.PIRATE_CHIEF);

    /**
     * The ships raidlevel
     */
    public final int raidLevel;

    /**
     * Structure schematic name
     */
    public final String schematicName;

    /**
     * Raid message id
     */
    public final int messageID;

    /**
     * Amount of spawners for the ship
     */
    public final int spawnerCount;

    /**
     * Array of pirates which are spawned for landing, one wave.
     */
    public final ResourceLocation[] pirates;

    ShipSize(final int raidLevel, final String schematicName, final int messageID, final int spawnerCount, final ResourceLocation... pirates)
    {
        this.raidLevel = raidLevel;
        this.schematicName = schematicName;
        this.messageID = messageID;
        this.spawnerCount = spawnerCount;
        this.pirates = pirates;
    }

    /**
     * Returns the right shipsize for the given raidlevel
     *
     * @param raidLevel
     * @return
     */
    public static ShipSize getShipForRaidLevel(final int raidLevel)
    {
        ShipSize shipSize;
        if (raidLevel < ColonyConstants.SMALL_HORDE_SIZE)
        {
            shipSize = SMALL;
        }
        else if (raidLevel < ColonyConstants.MEDIUM_HORDE_SIZE)
        {
            shipSize = MEDIUM;
        }
        else if (raidLevel < ColonyConstants.BIG_HORDE_SIZE)
        {
            shipSize = MEDIUM;
        }
        else
        {
            shipSize = BIG;
        }
        return shipSize;
    }
}
