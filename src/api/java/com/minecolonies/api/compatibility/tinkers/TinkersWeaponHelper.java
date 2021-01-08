package com.minecolonies.api.compatibility.tinkers;

import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.constant.IToolType;
import com.minecolonies.api.util.constant.ToolType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.tools.SwordCore;
import slimeknights.tconstruct.library.tools.TinkerToolCore;
import slimeknights.tconstruct.library.tools.ToolCore;
import slimeknights.tconstruct.library.tools.ranged.BowCore;
import slimeknights.tconstruct.library.utils.ToolHelper;
import slimeknights.tconstruct.tools.tools.Mattock;

/**
 * Class to check if certain tinkers items serve as weapons for the guards.
 */
public final class TinkersWeaponHelper extends TinkersWeaponProxy
{
    /**
     * Check if a certain itemstack is a tinkers weapon.
     *
     * @param stack the stack to check for.
     * @return true if so.
     */
    public static boolean isTinkersSword(@NotNull final ItemStack stack)
    {
        return new TinkersWeaponHelper().isTinkersWeapon(stack);
    }

    /**
     * Check if a certain itemstack is a tinkers long range weapon.
     *
     * @param stack the stack to check for.
     * @return true if so.
     */
    public static boolean isTinkersBow(@NotNull final ItemStack stack)
    {
        return new TinkersWeaponHelper().isTinkersLongRangeWeapon(stack);
    }

    /**
     * Check if a certain itemstack is a tinkers any tool
     *
     * @param stack the stack to check for.
     * @param toolType the tool type to check for.
     * @return true if so.
     */
    public static boolean isTinkersAnyTool(@NotNull final ItemStack stack,@Nullable final IToolType toolType)
    {
        return new TinkersWeaponHelper().isTinkersTool(stack,toolType);
    }

    /**
     * Check if a certain itemstack is a tinkers weapon.
     *
     * @param stack the stack to check for.
     * @return true if so.
     */
    @Override
    @Optional.Method(modid = "tconstruct")
    public boolean isTinkersWeapon(@NotNull final ItemStack stack)
    {
        return !ItemStackUtils.isEmpty(stack) && (stack.getItem() instanceof SwordCore);
    }

    /**
     * Check if a certain itemstack is a tinkers long range weapon.
     *
     * @param stack the stack to check for.
     * @return true if so.
     */
    @Override
    @Optional.Method(modid = "tconstruct")
    public boolean isTinkersLongRangeWeapon(@NotNull final ItemStack stack)
    {
        return !ItemStackUtils.isEmpty(stack) && (stack.getItem() instanceof BowCore);
    }

    /**
     * Check if a certain itemstack is a tinkers tool
     *
     * @param stack the stack to check for.
     * @param toolType the tool type to check for.
     * @return true if so.
     */
    @Override
    @Optional.Method(modid = "tconstruct")
    public boolean isTinkersTool(@NotNull final ItemStack stack,@Nullable final IToolType toolType)
    {
        return !ItemStackUtils.isEmpty(stack) && (stack.getItem() instanceof ToolCore) && (toolType == null ||stack.getItem().getHarvestLevel(stack,toolType.getName(),null,null)>=0 || (toolType == ToolType.HOE && stack.getItem() instanceof Mattock));
    }

    /**
     * Calculate the actual attack damage of the tinkers weapon.
     *
     * @param stack the stack.
     * @return the attack damage.
     */
    @Override
    @Optional.Method(modid = "tconstruct")
    public double getAttackDamage(@NotNull final ItemStack stack)
    {
        return ToolHelper.getActualAttack(stack);
    }

    /**
     * Calculate the tool level of the stack.
     *
     * @param stack the stack.
     * @return the tool level
     */
    @Override
    @Optional.Method(modid = "tconstruct")
    public int getToolLevel(@NotNull final ItemStack stack)
    {
        return ToolHelper.getHarvestLevelStat(stack);
    }

    /**
     * Calculate the actual attack damage of the tinkers weapon.
     *
     * @param stack the stack.
     * @return the attack damage.
     */
    public static double getDamage(@NotNull final ItemStack stack)
    {
        return new TinkersWeaponHelper().getAttackDamage(stack);
    }

    /**
     * Calculate the tool level of the stack.
     *
     * @param stack the stack.
     * @return the tool level
     */
    public static int getToolLvl(@NotNull final ItemStack stack)
    {
        return new TinkersWeaponHelper().getToolLevel(stack);
    }
}