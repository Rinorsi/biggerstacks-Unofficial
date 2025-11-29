/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.vanilla.stacksize;

import net.minecraft.world.item.BundleItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import portb.biggerstacks.util.StackSizeHelper;

@Mixin(BundleItem.class)
public class BundleItemMixin
{
    @ModifyConstant(method = "appendHoverText", constant = @Constant(intValue = 64))
    private static int biggerStacks$scaleTooltipLimit(int value)
    {
        return StackSizeHelper.getNewStackSize();
    }
}
