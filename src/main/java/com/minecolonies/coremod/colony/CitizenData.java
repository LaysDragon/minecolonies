package com.minecolonies.coremod.colony;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuildingWorker;
import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.jobs.registry.IJobDataManager;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.configuration.Configurations;
import com.minecolonies.api.configuration.NameConfiguration;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.LanguageHandler;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.Suppression;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.interactionhandling.ServerCitizenInteractionResponseHandler;
import com.minecolonies.coremod.entity.ai.basic.AbstractAISkeleton;
import com.minecolonies.coremod.entity.citizen.EntityCitizen;
import com.minecolonies.coremod.entity.citizen.citizenhandlers.CitizenHappinessHandler;
import com.minecolonies.coremod.network.messages.VanillaParticleMessage;
import com.minecolonies.coremod.util.ExperienceUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

import static com.minecolonies.api.entity.citizen.AbstractEntityCitizen.*;
import static com.minecolonies.api.util.constant.CitizenConstants.BASE_MAX_HEALTH;
import static com.minecolonies.api.util.constant.CitizenConstants.MAX_CITIZEN_LEVEL;
import static com.minecolonies.api.util.constant.NbtTagConstants.*;

/**
 * Extra data for Citizens.
 */
@SuppressWarnings({Suppression.BIG_CLASS, "PMD.ExcessiveClassLength"})
public class CitizenData implements ICitizenData
{

    /**
     * The max health.
     */
    private static final float MAX_HEALTH = 20.0F;

    /**
     * Max levels of an attribute a citizen may initially have.
     */
    private static final int LETTERS_IN_THE_ALPHABET = 26;

    /**
     * Minimum saturation of a citizen.
     */
    private static final int MIN_SATURATION = 0;

    /**
     * The chance the citizen has to levels. is 1 in this number.
     */
    private static final int CHANCE_TO_LEVEL = 50;

    /**
     * The number of skills the citizen has.
     */
    private static final int AMOUNT_OF_SKILLS = 5;

    /**
     * The unique citizen id.
     */
    private final int id;

    /**
     * The colony the citizen belongs to.
     */
    private final IColony colony;

    /**
     * Inventory of the citizen.
     */
    private final InventoryCitizen inventory;

    /**
     * The name of the citizen.
     */
    private String name;

    /**
     * Boolean gender, true = female, false = male.
     */
    private boolean female;

    /**
     * Whether the citizen is still a child
     */
    private boolean isChild = false;

    /**
     * Boolean paused, true = paused, false = working.
     */
    private boolean paused;

    /**
     * If restart is scheduled.
     */
    private boolean restartScheduled;

    /**
     * Report end message to:
     */
    private EntityPlayerMP originPlayerRestart;

    /**
     * The id of the citizens texture.
     */
    private int textureId;

    /**
     * If the citizen is asleep right now.
     */
    private boolean isAsleep;

    /**
     * The citizens current bedBos.
     */
    private BlockPos bedPos = BlockPos.ORIGIN;

    /**
     * The home building of the citizen.
     */
    @Nullable
    private IBuilding homeBuilding;

    /**
     * The work building of the citizen.
     */
    @Nullable
    private IBuildingWorker workBuilding;

    /**
     * The job of the citizen.
     */
    private IJob job;

    /**
     * If the citizen is dirty (Has to be updated on client side).
     */
    private boolean dirty;

    /**
     * Minimum for citizen stats
     */
    private final static int MIN_STAT = 1;

    /**
     * Its entitity.
     */
    @NotNull
    private WeakReference<AbstractEntityCitizen> entity = new WeakReference<>(null);

    /**
     * Attributes, which influence the workers behaviour.
     * May be added more later.
     */
    private int    strength;
    private int    endurance;
    private int    charisma;
    private int    intelligence;
    private int    dexterity;

    /**
     * The citizens saturation at the current moment.
     */
    private double saturation;

    /**
     * Variable indicating if a citizen just ate.
     */
    private boolean justAte;

    /**
     * The current experiences levels the citizen is on depending on his job.
     * The total amount of experiences the citizen has depending on his job.
     * This also includes the amount of experiences within their Experience Bar.
     */
    private Map<String, Tuple<Integer, Double>> levelExperienceMap = new HashMap<>();

    /**
     * The last position of the citizen.
     */
    private BlockPos lastPosition = new BlockPos(0, 0, 0);

    /**
     * The citizen happiness handler.
     * +
     */
    private final CitizenHappinessHandler citizenHappinessHandler;

    /**
     * The citizen chat options on the server side.
     */
    private final Map<ITextComponent, IInteractionResponseHandler> citizenChatOptions = new HashMap<>();

    /**
     * Create a CitizenData given an ID.
     * Used as a super-constructor or during loading.
     *
     * @param id     ID of the Citizen.
     * @param colony Colony the Citizen belongs to.
     */
    public CitizenData(final int id, final IColony colony)
    {
        this.id = id;
        this.colony = colony;
        inventory = new InventoryCitizen("Minecolonies Inventory", true, this);
        this.citizenHappinessHandler = new CitizenHappinessHandler(this);
    }

    @Override
    public void onResponseTriggered(@NotNull final ITextComponent key, @NotNull final ITextComponent response, final World world)
    {
        if (citizenChatOptions.containsKey(key))
        {
            citizenChatOptions.get(key).onServerResponseTriggered(response, world, this);
            markDirty();
        }
    }

    /**
     * Return the entity instance of the citizen data. Respawn the citizen if
     * needed.
     *
     * @return {@link EntityCitizen} of the citizen data.
     */
    @Override
    @NotNull
    public Optional<AbstractEntityCitizen> getCitizenEntity()
    {
        if (entity == null)
        {
            return Optional.empty();
        }

        final AbstractEntityCitizen citizen = entity.get();
        return Optional.ofNullable(citizen);
    }

    /**
     * Sets the entity of the citizen data.
     *
     * @param citizen {@link EntityCitizen} instance of the citizen data.
     */
    @Override
    public void setCitizenEntity(@Nullable final AbstractEntityCitizen citizen)
    {
        if (entity.get() != null)
        {
            entity.clear();
        }

        if (citizen != null)
        {
            entity = new WeakReference<>(citizen);
        }
    }

    /**
     * Marks the instance dirty.
     */
    @Override
    public void markDirty()
    {
        dirty = true;
        colony.getCitizenManager().markCitizensDirty();
    }

    /**
     * Returns a random element in a list.
     *
     * @param rand  Random object.
     * @param array Array to select from.
     * @return Random element from array.
     */
    private static String getRandomElement(@NotNull final Random rand, @NotNull final String[] array)
    {
        return array[rand.nextInt(array.length)];
    }

    /**
     * Returns a random capital letter from the alphabet.
     *
     * @param rand Random object.
     * @return Random capital letter.
     */
    private static char getRandomLetter(@NotNull final Random rand)
    {
        return (char) (rand.nextInt(LETTERS_IN_THE_ALPHABET) + 'A');
    }

    @Override
    public int hashCode()
    {
        return id;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final CitizenData data = (CitizenData) o;

        if (id != data.id)
        {
            return false;
        }

        return colony != null ? (data.colony != null && colony.getID() == data.colony.getID()) : (data.colony == null);
    }

    /**
     * Returns the colony of the citizen.
     *
     * @return colony of the citizen.
     */
    @Override
    public IColony getColony()
    {
        return colony;
    }

    /**
     * Returns the id of the citizen.
     *
     * @return id of the citizen.
     */
    @Override
    public int getId()
    {
        return id;
    }

    /**
     * Initializes a new citizen, when not read from nbt
     */
    @Override
    public void initForNewCitizen()
    {
        final Random rand = new Random();
        //Assign the gender before name
        female = rand.nextBoolean();
        paused = false;
        name = generateName(rand);

        saturation = MAX_SATURATION;
        final int levelCap = (int) colony.getOverallHappiness();

        if (levelCap <= 1)
        {
            intelligence = 1;
            charisma = 1;
            strength = 1;
            endurance = 1;
            dexterity = 1;
        }
        else
        {
            intelligence = rand.nextInt(levelCap - 1) + 1;
            charisma = rand.nextInt(levelCap - 1) + 1;
            strength = rand.nextInt(levelCap - 1) + 1;
            endurance = rand.nextInt(levelCap - 1) + 1;
            dexterity = rand.nextInt(levelCap - 1) + 1;
        }
        //Initialize the citizen skills and make sure they are never 0

        markDirty();
    }

    /**
     * Initializes the entities values from citizen data.
     */
    @Override
    public void initEntityValues()
    {
        if (!getCitizenEntity().isPresent())
        {
            return;
        }

        final AbstractEntityCitizen citizen = getCitizenEntity().get();

        citizen.setCitizenId(getId());
        citizen.getCitizenColonyHandler().setColonyId(getColony().getID());

        citizen.setIsChild(isChild());
        citizen.setCustomNameTag(citizen.getCitizenData().getName());

        citizen.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(BASE_MAX_HEALTH);

        citizen.setFemale(isFemale());
        citizen.setTextureId(getTextureId());

        citizen.getDataManager().set(DATA_COLONY_ID, colony.getID());
        citizen.getDataManager().set(DATA_CITIZEN_ID, citizen.getCitizenId());
        citizen.getDataManager().set(DATA_IS_FEMALE, citizen.isFemale() ? 1 : 0);
        citizen.getDataManager().set(DATA_TEXTURE, citizen.getTextureId());
        citizen.getDataManager().set(DATA_IS_ASLEEP, isAsleep());
        citizen.getDataManager().set(DATA_IS_CHILD, isChild());
        citizen.getDataManager().set(DATA_BED_POS, getBedPos());

        citizen.getCitizenExperienceHandler().updateLevel();

        setLastPosition(citizen.getPosition());

        citizen.getCitizenJobHandler().onJobChanged(citizen.getCitizenJobHandler().getColonyJob());

        markDirty();
    }

    /**
     * Generates a random name from a set of names.
     *
     * @param rand Random object.
     * @return Name of the citizen.
     */
    private String generateName(@NotNull final Random rand)
    {
        String citizenName;
        final String firstName;
        final String middleInitial;
        final String lastName;

        if (female)
        {
            firstName = getRandomElement(rand, NameConfiguration.names.femaleFirstNames);
        }
        else
        {
            firstName = getRandomElement(rand, NameConfiguration.names.maleFirstNames);
        }

        middleInitial = String.valueOf(getRandomLetter(rand));
        lastName = getRandomElement(rand, NameConfiguration.names.lastNames);

        if (NameConfiguration.names.useMiddleInitial)
        {
            citizenName = String.format("%s %s. %s", firstName, middleInitial, lastName);
        }
        else
        {
            citizenName = String.format("%s %s", firstName, lastName);
        }

        // Check whether there's already a citizen with this name
        for (final ICitizenData citizen : this.getColony().getCitizenManager().getCitizens())
        {
            if (citizen != null && citizen.getName().equals(citizenName))
            {
                // Oops - recurse this function and try again
                citizenName = generateName(rand);
                break;
            }
        }

        return citizenName;
    }

    /**
     * Returns the name of the citizen.
     *
     * @return name of the citizen.
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Returns true if citizen is female, false for male.
     *
     * @return true for female, false for male.
     */
    @Override
    public boolean isFemale()
    {
        return female;
    }

    /**
     * Sets wether this citizen is female.
     *
     * @param isFemale true if female
     */
    @Override
    public void setIsFemale(@NotNull final boolean isFemale)
    {
        this.female = isFemale;
        this.name = generateName(new Random());
        markDirty();
    }

    /**
     * Check if the citizen is paused.
     */
    @Override
    public void setPaused(final boolean p)
    {
        this.paused = p;
        markDirty();
    }

    /**
     * Check if the citizen is paused.
     *
     * @return true for paused, false for working.
     */
    @Override
    public boolean isPaused()
    {
        return paused;
    }

    /**
     * Returns the texture id for the citizen.
     *
     * @return texture ID.
     */
    @Override
    public int getTextureId()
    {
        return textureId;
    }

    /**
     * Returns whether or not the instance is dirty.
     *
     * @return true when dirty, otherwise false.
     */
    @Override
    public boolean isDirty()
    {
        return dirty;
    }

    /**
     * Markt the instance not dirty.
     */
    @Override
    public void clearDirty()
    {
        dirty = false;
    }

    /**
     * When a building is destroyed, inform the citizen so it can do any cleanup
     * of associations that the building's. own AbstractBuilding.onDestroyed did
     * not do.
     *
     * @param building building that is destroyed.
     */
    @Override
    public void onRemoveBuilding(final IBuilding building)
    {
        if (getHomeBuilding() == building)
        {
            setHomeBuilding(null);
        }

        if (getWorkBuilding() == building)
        {
            setWorkBuilding(null);
        }
    }

    /**
     * Returns the home building of the citizen.
     *
     * @return home building.
     */
    @Override
    @Nullable
    public IBuilding getHomeBuilding()
    {
        return homeBuilding;
    }

    /**
     * Sets the home of the citizen.
     *
     * @param building home building.
     */
    @Override
    public void setHomeBuilding(@Nullable final IBuilding building)
    {
        if (homeBuilding != null && building != null && !homeBuilding.equals(building))
        {
            homeBuilding.removeCitizen(this);
            markDirty();
        }

        homeBuilding = building;
        markDirty();

        if (getCitizenEntity().isPresent() && getCitizenEntity().get().getCitizenJobHandler().getColonyJob() == null)
        {
            getCitizenEntity().get().getCitizenJobHandler().setModelDependingOnJob(null);
        }
    }

    /**
     * Returns the work building of a citizen.
     *
     * @return home building of a citizen.
     */
    @Override
    @Nullable
    public IBuildingWorker getWorkBuilding()
    {
        return workBuilding;
    }

    /**
     * Sets the work building of a citizen.
     *
     * @param building work building.
     */
    @Override
    public void setWorkBuilding(@Nullable final IBuildingWorker building)
    {
        if (workBuilding != null && building != null && workBuilding != building)
        {
            Log.getLogger().warn("CitizenData.setWorkBuilding() - already assigned a work building when setting a new work building");
        }
        else if (workBuilding != building)
        {
            workBuilding = building;

            if (workBuilding != null)
            {
                //  We have a place to work, do we have the assigned Job?
                if (job == null)
                {
                    //  No job, create one!
                    setJob(workBuilding.createJob(this));
                    colony.getWorkManager().clearWorkForCitizen(this);
                }
            }
            else if (job != null)
            {
                getCitizenEntity().ifPresent(entityCitizen -> {
                    entityCitizen.getTasks().removeTask(entityCitizen.getTasks().taskEntries.stream()
                                                          .filter(task -> task.action instanceof AbstractAISkeleton)
                                                          .findFirst()
                                                          .orElse(null).action);
                });

                //  No place of employment, get rid of our job
                setJob(null);
                colony.getWorkManager().clearWorkForCitizen(this);
            }

            markDirty();
        }
    }

    /**
     * Updates {@link EntityCitizen} for the instance.
     */
    @Override
    public void updateCitizenEntityIfNecessary()
    {
        if (getCitizenEntity().isPresent())
        {
            return;
        }

        colony.getCitizenManager().spawnOrCreateCitizen(this, colony.getWorld(), lastPosition, true);
    }

    /**
     * Returns the job of the citizen.
     *
     * @return Job of the citizen.
     */
    @Override
    public IJob getJob()
    {
        return job;
    }

    /**
     * Sets the job of this citizen.
     *
     * @param job Job of the citizen.
     */
    @Override
    public void setJob(final IJob job)
    {
        this.job = job;

        getCitizenEntity().ifPresent(entityCitizen -> entityCitizen.getCitizenJobHandler().onJobChanged(job));

        markDirty();
    }

    /**
     * Returns the job subclass needed. Returns null on type mismatch.
     *
     * @param type the type of job wanted.
     * @param <J>  The job type returned.
     * @return the job this citizen has.
     */
    @Override
    @Nullable
    public <J extends IJob> J getJob(@NotNull final Class<J> type)
    {
        if (type.isInstance(job))
        {
            return type.cast(job);
        }

        return null;
    }

    /**
     * Writes the citizen data to a byte buf for transition.
     *
     * @param buf Buffer to write to.
     */
    @Override
    public void serializeViewNetworkData(@NotNull final ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeBoolean(female);

        buf.writeInt(getCitizenEntity().map(AbstractEntityCitizen::getEntityId).orElse(-1));

        buf.writeBoolean(paused);

        buf.writeBoolean(isChild);

        buf.writeBoolean(homeBuilding != null);
        if (homeBuilding != null)
        {
            BlockPosUtil.writeToByteBuf(buf, homeBuilding.getID());
        }

        buf.writeBoolean(workBuilding != null);
        if (workBuilding != null)
        {
            BlockPosUtil.writeToByteBuf(buf, workBuilding.getID());
        }

        //  Attributes
        buf.writeInt(getLevel());
        buf.writeDouble(getExperience());

        // If the entity is not present we assumes standard values.
        buf.writeFloat(getCitizenEntity().map(AbstractEntityCitizen::getHealth).orElse(MAX_HEALTH));
        buf.writeFloat(getCitizenEntity().map(AbstractEntityCitizen::getMaxHealth).orElse(MAX_HEALTH));

        buf.writeInt(getStrength());
        buf.writeInt(getEndurance());
        buf.writeInt(getCharisma());
        buf.writeInt(getIntelligence());
        buf.writeInt(getDexterity());
        buf.writeDouble(getSaturation());
        buf.writeDouble(citizenHappinessHandler.getHappiness());

        citizenHappinessHandler.serializeViewNetworkData(buf);

        ByteBufUtils.writeUTF8String(buf, (job != null) ? job.getName() : "");

        buf.writeInt(colony.getID());

        final NBTTagCompound compound = new NBTTagCompound();
        compound.setTag("inventory", inventory.writeToNBT(new NBTTagList()));
        ByteBufUtils.writeTag(buf, compound);
        BlockPosUtil.writeToByteBuf(buf, lastPosition == null ? BlockPos.ORIGIN : lastPosition);

        if (colony.getWorld() != null)
        {
            final List<IInteractionResponseHandler> subInteractions = citizenChatOptions.values().stream().filter(e -> e.isVisible(colony.getWorld())).collect(Collectors.toList());

            buf.writeInt(subInteractions.size());
            for (final IInteractionResponseHandler interactionHandler : subInteractions)
            {
                ByteBufUtils.writeTag(buf, interactionHandler.serializeNBT());
            }
        }
        else
        {
            buf.writeInt(0);
        }
    }

    /**
     * Returns the levels of the citizen.
     *
     * @return levels of the citizen.
     */
    @Override
    public int getLevel()
    {
        if (job == null)
        {
            return 0;
        }
        return queryLevelExperienceMap().getFirst();
    }

    /**
     * Sets the levels of the citizen.
     *
     * @param lvl the new levels for the citizen.
     */
    @Override
    public void setLevel(final int lvl)
    {
        if (job == null)
        {
            return;
        }
        final Tuple<Integer, Double> entry = queryLevelExperienceMap();
        this.levelExperienceMap.put(job.getExperienceTag(), new Tuple<>(lvl, entry.getSecond()));
        job.onLevelUp(lvl);
    }

    /**
     * Adds experiences of the citizen.
     *
     * @param xp the amount of xp to add.
     */
    @Override
    public void addExperience(final double xp)
    {
        if (this.job != null)
        {
            final Tuple<Integer, Double> entry = queryLevelExperienceMap();
            this.levelExperienceMap.put(job.getExperienceTag(), new Tuple<>(entry.getFirst(), entry.getSecond() + xp));
        }
    }

    /**
     * Levelup actions for the citizen, increases levels and notifies the Citizen's Job
     */
    @Override
    public void levelUp()
    {
        increaseLevel();

        // Show levelup particles
        if (getCitizenEntity().isPresent())
        {
            final AbstractEntityCitizen citizen = getCitizenEntity().get();
            MineColonies.getNetwork()
              .sendToAllTracking(new VanillaParticleMessage(citizen.posX, citizen.posY, citizen.posZ, EnumParticleTypes.VILLAGER_HAPPY.getParticleID()), getCitizenEntity().get());
        }

        if (job != null)
        {
            final Tuple<Integer, Double> entry = queryLevelExperienceMap();
            job.onLevelUp(entry.getFirst());
        }
    }

    /**
     * Increases the levels of the citizen.
     */
    private void increaseLevel()
    {
        if (job != null)
        {
            final Tuple<Integer, Double> entry = queryLevelExperienceMap();
            if (entry.getFirst() < MAX_CITIZEN_LEVEL)
            {
                this.levelExperienceMap.put(job.getExperienceTag(), new Tuple<>(entry.getFirst() + 1, entry.getSecond()));
            }
        }
    }

    /**
     * Returns the default chance to levelup
     */
    @Override
    public int getChanceToLevel() {return CHANCE_TO_LEVEL;}

    /**
     * Getter for the saturation.
     *
     * @param extraSaturation the extra saturation
     */
    @Override
    public void increaseSaturation(final double extraSaturation)
    {
        this.saturation = Math.min(MAX_SATURATION, this.saturation + Math.abs(extraSaturation));
    }

    /**
     * Getter for the saturation.
     *
     * @param extraSaturation the saturation to remove.
     */
    @Override
    public void decreaseSaturation(final double extraSaturation)
    {
        this.saturation = Math.max(MIN_SATURATION, this.saturation - Math.abs(extraSaturation * Configurations.gameplay.foodModifier));
        this.justAte = false;
    }

    /**
     * Set the citizen name.
     *
     * @param name the name to set.
     */
    @Override
    public void setName(final String name)
    {
        this.name = name;
    }

    /**
     * Returns the experiences of the citizen.
     *
     * @return experiences of the citizen.
     */
    @Override
    public double getExperience()
    {
        if (job == null)
        {
            return 0;
        }
        return queryLevelExperienceMap().getSecond();
    }

    /**
     * Query the map and compute absent if necessary.
     *
     * @return the tuple for the job.
     */
    private Tuple<Integer, Double> queryLevelExperienceMap()
    {
        return levelExperienceMap.computeIfAbsent(job.getExperienceTag(), jobKey -> new Tuple<>(0, 0.0D));
    }

    /**
     * Strength getter.
     *
     * @return citizen Strength value.
     */
    @Override
    public int getStrength()
    {
        return strength;
    }

    /**
     * Endurance getter.
     *
     * @return citizen Endurance value.
     */
    @Override
    public int getEndurance()
    {
        return endurance;
    }

    /**
     * Charisma getter.
     *
     * @return citizen Charisma value.
     */
    @Override
    public int getCharisma()
    {
        return charisma;
    }

    /**
     * Intelligence getter.
     *
     * @return citizen Intelligence value.
     */
    @Override
    public int getIntelligence()
    {
        return intelligence;
    }

    /**
     * Dexterity getter.
     *
     * @return citizen Dexterity value.
     */
    @Override
    public int getDexterity()
    {
        return dexterity;
    }

    /**
     * Set the last position of the citizen.
     *
     * @param lastPosition the last position.
     */
    @Override
    public void setLastPosition(final BlockPos lastPosition)
    {
        this.lastPosition = lastPosition;
    }

    /**
     * Get the last position of the citizen.
     *
     * @return the last position.
     */
    @Override
    public BlockPos getLastPosition()
    {
        return lastPosition;
    }

    /**
     * Getter for the saturation.
     *
     * @return the saturation.
     */
    @Override
    public double getSaturation()
    {
        return this.saturation;
    }

    /**
     * Getter for the inventory.
     *
     * @return the direct reference to the citizen inventory.
     */
    @Override
    public InventoryCitizen getInventory()
    {
        return inventory;
    }

    /**
     * Check if citizen is asleep.
     *
     * @return true if so.
     */
    @Override
    public boolean isAsleep()
    {
        return isAsleep;
    }

    /**
     * Getter for the bedPos.
     *
     * @return the bedPos.
     */
    @Override
    public BlockPos getBedPos()
    {
        return bedPos;
    }

    /**
     * Set asleep.
     *
     * @param asleep true if asleep.
     */
    @Override
    public void setAsleep(final boolean asleep)
    {
        isAsleep = asleep;
    }

    /**
     * Set the bed pos.
     *
     * @param bedPos the pos to set.
     */
    @Override
    public void setBedPos(final BlockPos bedPos)
    {
        this.bedPos = bedPos;
    }

    /**
     * Create a blocking request.
     *
     * @param requested the request to create.
     * @param <R>       the Type
     * @return the token of the request.
     */
    @Override
    public <R extends IRequestable> IToken createRequest(@NotNull final R requested)
    {
        return getWorkBuilding().createRequest(this, requested, false);
    }

    /**
     * Create an async request.
     *
     * @param requested the request to create.
     * @param <R>       the Type
     * @return the token of the request.
     */
    @Override
    public <R extends IRequestable> IToken createRequestAsync(@NotNull final R requested)
    {
        return getWorkBuilding().createRequest(this, requested, true);
    }

    /**
     * Called on request canceled.
     *
     * @param token the token to be canceled.
     */
    @Override
    public void onRequestCancelled(@NotNull final IToken token)
    {
        if (isRequestAsync(token))
        {
            job.getAsyncRequests().remove(token);
        }
    }

    /**
     * Check if a request is async.
     *
     * @param token the token to check.
     * @return true if it is.
     */
    @Override
    public boolean isRequestAsync(@NotNull final IToken token)
    {
        if (job != null)
        {
            return job.getAsyncRequests().contains(token);
        }
        return false;
    }

    /**
     * The Handler for the citizens happiness.
     *
     * @return the instance of the handler
     */
    @Override
    public CitizenHappinessHandler getCitizenHappinessHandler()
    {
        return citizenHappinessHandler;
    }

    /**
     * Try a random levels up.
     */
    @Override
    public void tryRandomLevelUp(final Random random)
    {
        tryRandomLevelUp(random, 0);
    }

    /**
     * Try a random levels up.
     *
     * @param customChance set to 0 to not use, chance for levelup is 1/customChance
     */
    @Override
    public void tryRandomLevelUp(final Random random, final int customChance)
    {
        if ((customChance > 0 && random.nextInt(customChance) > 0) || (customChance < 1 && random.nextInt(CHANCE_TO_LEVEL) > 0))
        {
            return;
        }

        final int levelCap = (int) getCitizenHappinessHandler().getHappiness();
        switch (random.nextInt(AMOUNT_OF_SKILLS))
        {
            case 0:
                intelligence = Math.max(intelligence, Math.min(intelligence + 1, levelCap));
                break;
            case 1:
                charisma = Math.max(charisma, Math.min(charisma + 1, levelCap));
                break;
            case 2:
                strength = Math.max(strength, Math.min(strength + 1, levelCap));
                break;
            case 3:
                endurance = Math.max(endurance, Math.min(endurance + 1, levelCap));
                break;
            default:
                dexterity = Math.max(dexterity, Math.min(dexterity + 1, levelCap));
                break;
        }
        markDirty();
    }

    /**
     * Schedule restart and cleanup
     */
    @Override
    public void scheduleRestart(final EntityPlayerMP player)
    {
        originPlayerRestart = player;
        restartScheduled = true;
    }

    /**
     * AI will be restarted, also restart building etc
     */
    @Override
    public boolean shouldRestart()
    {
        return restartScheduled;
    }

    /**
     * Restart done successfully
     */
    @Override
    public void restartDone()
    {
        restartScheduled = false;
        LanguageHandler.sendPlayerMessage(originPlayerRestart, "com.minecolonies.coremod.gui.hiring.restartMessageDone", getName());
    }

    /**
     * Set the child flag.
     *
     * @param isChild boolean
     */
    @Override
    public void setIsChild(final boolean isChild)
    {
        this.isChild = isChild;
        markDirty();

        if (colony != null)
        {
            colony.updateHasChilds();
        }
    }

    /**
     * Is this citizen a child?
     *
     * @return boolean
     */
    @Override
    public boolean isChild()
    {
        return isChild;
    }

    /**
     * Set the strength of the citizen
     *
     * @param strength value to set
     */
    @Override
    public void setStrength(@NotNull final int strength)
    {
        if (strength < MIN_STAT)
        {
            this.strength = MIN_STAT;
        }
        else
        {
            this.strength = strength > colony.getOverallHappiness() ? (int) colony.getOverallHappiness() : strength;
        }
        markDirty();
    }

    /**
     * Set the endurance of the citizen
     *
     * @param endurance value to set
     */
    @Override
    public void setEndurance(@NotNull final int endurance)
    {
        if (endurance < MIN_STAT)
        {
            this.endurance = MIN_STAT;
        }
        else
        {
            this.endurance = endurance > colony.getOverallHappiness() ? (int) colony.getOverallHappiness() : endurance;
        }
        markDirty();
    }

    /**
     * Set the charisma of the citizen
     *
     * @param charisma value to set
     */
    @Override
    public void setCharisma(@NotNull final int charisma)
    {
        if (charisma < MIN_STAT)
        {
            this.charisma = MIN_STAT;
        }
        else
        {
            this.charisma = charisma > colony.getOverallHappiness() ? (int) colony.getOverallHappiness() : charisma;
        }
        markDirty();
    }

    /**
     * Set the intelligence of the citizen
     *
     * @param intelligence value to set
     */
    @Override
    public void setIntelligence(@NotNull final int intelligence)
    {
        if (intelligence < MIN_STAT)
        {
            this.intelligence = MIN_STAT;
        }
        else
        {
            this.intelligence = intelligence > colony.getOverallHappiness() ? (int) colony.getOverallHappiness() : intelligence;
        }
        markDirty();
    }

    /**
     * Set the dexterity of the citizen
     *
     * @param dexterity value to set
     */
    @Override
    public void setDexterity(@NotNull final int dexterity)
    {
        if (dexterity < MIN_STAT)
        {
            this.dexterity = MIN_STAT;
        }
        else
        {
            this.dexterity = dexterity > colony.getOverallHappiness() ? (int) colony.getOverallHappiness() : dexterity;
        }
        markDirty();
    }

    /**
     * Check if the citizen just ate.
     *
     * @return true if so.
     */
    @Override
    public boolean justAte()
    {
        return this.justAte;
    }

    /**
     * Set or reset if the citizen just ate.
     *
     * @param justAte true if justAte, false to reset.
     */
    @Override
    public void setJustAte(final boolean justAte)
    {
        this.justAte = justAte;
    }

    @Override
    public double drainExperience(final int levelDrain)
    {
        if (job != null)
        {
            final Tuple<Integer, Double> entry = queryLevelExperienceMap();
            final double drain = ExperienceUtils.getXPNeededForNextLevel(levelDrain - 1);

            final double xpDrain = Math.min(drain, entry.getSecond());
            final double newXp = entry.getSecond() - (xpDrain / Configurations.gameplay.enchanterExperienceMultiplier);
            final int newLevel = ExperienceUtils.calculateLevel(newXp);

            this.levelExperienceMap.put(job.getExperienceTag(), new Tuple<>(newLevel, newXp));
            this.markDirty();
            return xpDrain;
        }
        return 0;
    }

    @Override
    public void spendLevels(final int levelDrain)
    {
        if (job != null)
        {
            final Tuple<Integer, Double> entry = queryLevelExperienceMap();
            final double drain = ExperienceUtils.getXPNeededForNextLevel(levelDrain - 1);

            final double xpDrain = Math.min(drain, entry.getSecond());
            final double newXp = entry.getSecond() - xpDrain;
            final int newLevel = ExperienceUtils.calculateLevel(newXp);

            this.levelExperienceMap.put(job.getExperienceTag(), new Tuple<>(newLevel, newXp));
            this.markDirty();
        }
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        final NBTTagCompound nbtTagCompound = new NBTTagCompound();

        nbtTagCompound.setInteger(TAG_ID, id);
        nbtTagCompound.setString(TAG_NAME, name);
        nbtTagCompound.setBoolean(TAG_FEMALE, female);
        nbtTagCompound.setBoolean(TAG_PAUSED, paused);
        nbtTagCompound.setBoolean(TAG_CHILD, isChild);
        nbtTagCompound.setInteger(TAG_TEXTURE, textureId);

        //  Attributes

        @NotNull final NBTTagList levelTagList = new NBTTagList();
        for (@NotNull final Map.Entry<String, Tuple<Integer, Double>> entry : levelExperienceMap.entrySet())
        {
            @NotNull final NBTTagCompound levelCompound = new NBTTagCompound();
            levelCompound.setString(TAG_NAME, entry.getKey());
            levelCompound.setInteger(TAG_LEVEL, entry.getValue().getFirst());
            levelCompound.setDouble(TAG_EXPERIENCE, entry.getValue().getSecond());
            levelTagList.appendTag(levelCompound);
        }
        nbtTagCompound.setTag(TAG_LEVEL_MAP, levelTagList);

        BlockPosUtil.writeToNBT(nbtTagCompound, TAG_POS, lastPosition);

        @NotNull final NBTTagCompound nbtTagSkillsCompound = new NBTTagCompound();
        nbtTagSkillsCompound.setInteger(TAG_SKILL_STRENGTH, strength);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_STAMINA, endurance);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_SPEED, charisma);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_INTELLIGENCE, intelligence);
        nbtTagSkillsCompound.setInteger(TAG_SKILL_DEXTERITY, dexterity);
        nbtTagCompound.setTag(TAG_SKILLS, nbtTagSkillsCompound);
        nbtTagCompound.setDouble(TAG_SATURATION, saturation);

        if (job != null)
        {
            @NotNull final NBTBase jobCompound = job.serializeNBT();
            nbtTagCompound.setTag("job", jobCompound);
        }

        nbtTagCompound.setTag(TAG_INVENTORY, inventory.writeToNBT(new NBTTagList()));
        nbtTagCompound.setInteger(TAG_HELD_ITEM_SLOT, inventory.getHeldItemSlot(EnumHand.MAIN_HAND));
        nbtTagCompound.setInteger(TAG_OFFHAND_HELD_ITEM_SLOT, inventory.getHeldItemSlot(EnumHand.OFF_HAND));

        BlockPosUtil.writeToNBT(nbtTagCompound, TAG_POS, bedPos);
        nbtTagCompound.setBoolean(TAG_ASLEEP, isAsleep);
        nbtTagCompound.setBoolean(TAG_JUST_ATE, justAte);

        citizenHappinessHandler.writeToNBT(nbtTagCompound);

        @NotNull final NBTTagList chatTagList = new NBTTagList();
        for (@NotNull final IInteractionResponseHandler entry : citizenChatOptions.values())
        {
            @NotNull final NBTTagCompound chatOptionCompound = new NBTTagCompound();
            chatOptionCompound.setTag(TAG_CHAT_OPTION, entry.serializeNBT());
            chatTagList.appendTag(chatOptionCompound);
        }
        nbtTagCompound.setTag(TAG_CHAT_OPTIONS, chatTagList);
        return nbtTagCompound;
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbtTagCompound)
    {
        name = nbtTagCompound.getString(TAG_NAME);
        female = nbtTagCompound.getBoolean(TAG_FEMALE);
        paused = nbtTagCompound.getBoolean(TAG_PAUSED);
        isChild = nbtTagCompound.getBoolean(TAG_CHILD);
        textureId = nbtTagCompound.getInteger(TAG_TEXTURE);

        lastPosition = BlockPosUtil.readFromNBT(nbtTagCompound, TAG_POS);

        final NBTTagCompound nbtTagSkillsCompound = nbtTagCompound.getCompoundTag("skills");
        strength = nbtTagSkillsCompound.getInteger("strength");
        endurance = nbtTagSkillsCompound.getInteger("endurance");
        charisma = nbtTagSkillsCompound.getInteger("charisma");
        intelligence = nbtTagSkillsCompound.getInteger("intelligence");
        dexterity = nbtTagSkillsCompound.getInteger("dexterity");
        saturation = nbtTagCompound.getDouble(TAG_SATURATION);

        if (nbtTagCompound.hasKey("job"))
        {
            setJob(IJobDataManager.getInstance().createFrom(this, nbtTagCompound.getCompoundTag("job")));
        }

        //  Attributes
        if (nbtTagCompound.hasKey(TAG_LEVEL_MAP))
        {
            final NBTTagList levelTagList = nbtTagCompound.getTagList(TAG_LEVEL_MAP, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < levelTagList.tagCount(); ++i)
            {
                final NBTTagCompound levelExperienceAtJob = levelTagList.getCompoundTagAt(i);
                levelExperienceMap.put(levelExperienceAtJob.getString(TAG_NAME),
                  new Tuple<>(Math.min(levelExperienceAtJob.getInteger(TAG_LEVEL), MAX_CITIZEN_LEVEL), levelExperienceAtJob.getDouble(TAG_EXPERIENCE)));
            }
        }
        else if (job != null)
        {
            levelExperienceMap.put(job.getExperienceTag(), new Tuple<>(nbtTagCompound.getInteger(TAG_LEVEL), nbtTagCompound.getDouble(TAG_EXPERIENCE)));
        }

        if (nbtTagCompound.hasKey(TAG_INVENTORY))
        {
            final NBTTagList nbttaglist = nbtTagCompound.getTagList(TAG_INVENTORY, 10);
            this.inventory.readFromNBT(nbttaglist);
            this.inventory.setHeldItem(EnumHand.MAIN_HAND, nbtTagCompound.getInteger(TAG_HELD_ITEM_SLOT));
            this.inventory.setHeldItem(EnumHand.OFF_HAND, nbtTagCompound.getInteger(TAG_OFFHAND_HELD_ITEM_SLOT));
        }

        if (name.isEmpty())
        {
            name = generateName(new Random());
        }

        if (nbtTagCompound.hasKey(TAG_ASLEEP))
        {
            bedPos = BlockPosUtil.readFromNBT(nbtTagCompound, TAG_POS);
            isAsleep = nbtTagCompound.getBoolean(TAG_ASLEEP);
        }

        if (nbtTagCompound.hasKey(TAG_JUST_ATE))
        {
            justAte = nbtTagCompound.getBoolean(TAG_JUST_ATE);
        }

        //  Citizen chat options.
        if (nbtTagCompound.hasKey(TAG_CHAT_OPTIONS))
        {
            final NBTTagList handlerTagList = nbtTagCompound.getTagList(TAG_CHAT_OPTIONS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < handlerTagList.tagCount(); ++i)
            {
                final ServerCitizenInteractionResponseHandler handler =
                  (ServerCitizenInteractionResponseHandler) MinecoloniesAPIProxy.getInstance().getInteractionResponseHandlerDataManager().createFrom(this, handlerTagList.getCompoundTagAt(i).getCompoundTag(TAG_CHAT_OPTION));
                citizenChatOptions.put(handler.getInquiry(), handler);
            }
        }
        citizenHappinessHandler.readFromNBT(nbtTagCompound);
    }

    @Override
    public void tick()
    {
        if (!getCitizenEntity().isPresent() || getCitizenEntity().get().isDead())
        {
            return;
        }

        final List<IInteractionResponseHandler> toRemove = new ArrayList<>();
        for (final IInteractionResponseHandler handler : citizenChatOptions.values())
        {
            try
            {
                if (!handler.isValid(this))
                {
                    toRemove.add(handler);
                }
            }
            catch (final Exception e)
            {
                Log.getLogger().warn("Error during validation of handler: " + handler.getInquiry(), e);
                // If anything goes wrong in checking validity, remove handler.
                toRemove.add(handler);
            }
        }

        if (!toRemove.isEmpty())
        {
            markDirty();
        }

        for (final IInteractionResponseHandler handler : toRemove)
        {
            citizenChatOptions.remove(handler.getInquiry());
            for (final ITextComponent comp : handler.getPossibleResponses())
            {
                if (citizenChatOptions.containsKey(handler.getResponseResult(comp)))
                {
                    citizenChatOptions.get(handler.getResponseResult(comp)).removeParent(handler.getInquiry());
                }
            }
        }
    }

    @Override
    public void triggerInteraction(@NotNull final IInteractionResponseHandler handler)
    {
        if (!this.citizenChatOptions.containsKey(handler.getInquiry()))
        {
            this.citizenChatOptions.put(handler.getInquiry(), handler);
            for (final IInteractionResponseHandler childHandler : handler.genChildInteractions())
            {
                this.citizenChatOptions.put(childHandler.getInquiry(), (ServerCitizenInteractionResponseHandler) childHandler);
            }
            markDirty();
        }
    }
}
