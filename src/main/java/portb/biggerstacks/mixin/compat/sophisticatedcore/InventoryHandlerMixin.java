/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.compat.sophisticatedcore;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import portb.biggerstacks.config.StackSizeRules;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class InventoryHandlerMixin
{
    @ModifyVariable(method = "setBaseSlotLimit", at = @At("HEAD"), argsOnly = true)
    private int clampBaseSlotLimit(int baseSlotLimit)
    {
        return StackSizeRules.clampStackSize(baseSlotLimit);
    }

    @Inject(method = "getBaseStackLimit", at = @At("RETURN"), cancellable = true)
    private void clampBaseStackLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir)
    {
        int clamped = StackSizeRules.clampStackSize(cir.getReturnValue());
        if (clamped != cir.getReturnValue())
            cir.setReturnValue(clamped);
    }

    @Inject(method = "getSlotLimit", at = @At("RETURN"), cancellable = true)
    private void clampSlotLimit(int slot, CallbackInfoReturnable<Integer> cir)
    {
        int clamped = StackSizeRules.clampStackSize(cir.getReturnValue());
        if (clamped != cir.getReturnValue())
            cir.setReturnValue(clamped);
    }
}
