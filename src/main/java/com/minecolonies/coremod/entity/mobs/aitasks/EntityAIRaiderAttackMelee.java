package com.minecolonies.coremod.entity.mobs.aitasks;

import com.minecolonies.api.configuration.Configurations;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMob;
import com.minecolonies.api.util.SoundUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;

import static com.minecolonies.api.entity.mobs.RaiderMobUtils.MOB_ATTACK_DAMAGE;

/**
 * Barbarian Attack AI class
 */
public class EntityAIRaiderAttackMelee extends EntityAIBase
{

    private static final int                           MAX_ATTACK_DELAY        = 60;
    private static final double                        HALF_ROTATION           = 180;
    private static final double                        MIN_DISTANCE_FOR_ATTACK = 2.5;
    private static final double                        ATTACK_SPEED            = 1.3;
    public static final  int                           MUTEX_BITS              = 3;
    private static final double                        MELEE_DAMAGE_BONUS      = 1.0;
    private final        AbstractEntityMinecoloniesMob entity;
    private              EntityLivingBase              target;
    private              int                           lastAttack              = 0;

    /**
     * Timer for the update rate of attack logic
     */
    private int tickTimer = 0;

    /**
     * Constructor method for AI
     *
     * @param creatureIn The creature which is using the AI
     */
    public EntityAIRaiderAttackMelee(final AbstractEntityMinecoloniesMob creatureIn)
    {
        super();
        this.entity = creatureIn;
        this.setMutexBits(MUTEX_BITS);
    }

    @Override
    public boolean shouldExecute()
    {
        target = entity.getAttackTarget() != null ? entity.getAttackTarget() : entity.getAttackingEntity();
        return target != null;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     *
     * @return Boolean value on whether or not to continue executing
     */
    @Override
    public boolean shouldContinueExecuting()
    {
        if (target.isEntityAlive() && entity.isEntityAlive() && entity.canEntityBeSeen(target))
        {
            attack(target);
            return true;
        }
        return false;
    }

    /**
     * Is executed when the ai Starts Executing
     */
    @Override
    public void startExecuting()
    {
        attack(target);
    }

    /**
     * AI for an Entity to attack the target, Called every Tick(20tps)
     *
     * @param target The target to attack
     */
    private void attack(final EntityLivingBase target)
    {
        // Limit Actions to every 10 Ticks
        if (tickTimer > 0)
        {
            tickTimer--;
            lastAttack--;
            return;
        }
        tickTimer = 10;

        if (target != null)
        {
            double damageToBeDealt = entity.getEntityAttribute(MOB_ATTACK_DAMAGE).getAttributeValue() + MELEE_DAMAGE_BONUS;

            if (entity.getDistance(target) <= MIN_DISTANCE_FOR_ATTACK && lastAttack <= 0 && entity.canEntityBeSeen(target))
            {
                target.attackEntityFrom(new EntityDamageSource(entity.getName(), entity), (float) damageToBeDealt);
                entity.swingArm(EnumHand.MAIN_HAND);
                entity.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, (float) 1.0D, (float) SoundUtils.getRandomPitch(entity.getRNG()));
                target.setRevengeTarget(entity);
                lastAttack = getAttackDelay();
            }
            if (lastAttack > 0)
            {
                lastAttack -= 1;
            }

            entity.faceEntity(target, (float) HALF_ROTATION, (float) HALF_ROTATION);
            entity.getLookHelper().setLookPositionWithEntity(target, (float) HALF_ROTATION, (float) HALF_ROTATION);

            entity.getNavigator().tryMoveToEntityLiving(target, ATTACK_SPEED);
        }
    }

    /**
     * Gets the delay time for a next attack by the barbarian.
     *
     * @return the reload time
     */
    protected int getAttackDelay()
    {
        return MAX_ATTACK_DELAY - Configurations.gameplay.barbarianHordeDifficulty * 4;
    }
}
