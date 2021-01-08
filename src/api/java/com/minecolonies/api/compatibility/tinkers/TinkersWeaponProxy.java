package com.minecolonies.api.compatibility.tinkers;

import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.IToolType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.ToolCore;

/**
 * Class to check if certain tinkers items serve as weapons for the guards.
 */
public class TinkersWeaponProxy
{
    /**
     * Check if a certain itemstack is a tinkers weapon.
     *
     * @param stack the stack to check for.
     * @return true if so.
     */
    public boolean isTinkersWeapon(@NotNull final ItemStack stack)
    {
        return false;
    }

    /**
     * Check if a certain itemstack is a tinkers long range weapon.
     *
     * @param stack the stack to check for.
     * @return true if so.
     */
    public boolean isTinkersLongRangeWeapon(@NotNull final ItemStack stack)
    {
        return false;
    }

    /**
     * Check if a certain itemstack is a tinkers tool
     *
     * @param stack the stack to check for.
     * @param toolType the tool type to check for.
     * @return true if so.
     */
    public boolean isTinkersTool(@NotNull final ItemStack stack,@Nullable final IToolType toolType)
    {
        return false;
    }

    /**
     * Calculate the actual attack damage of the tinkers weapon.
     *
     * @param stack the stack.
     * @return the attack damage.
     */
    public double getAttackDamage(@NotNull final ItemStack stack)
    {
        return 0;
    }

    /**
     * Calculate the tool level of the stack.
     *
     * @param stack the stack.
     * @return the tool level
     */
    public int getToolLevel(@NotNull final ItemStack stack)
    {
        return -1;
    }
}
