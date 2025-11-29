/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.compat.sophisticatedcore;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import portb.biggerstacks.config.StackSizeRules;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem", remap = false)
public abstract class StackUpgradeItemMixin
{
    @Inject(method = "getInventorySlotLimit(Lnet/p3pp3rf1y/sophisticatedcore/api/IStorageWrapper;)I",
            at = @At("RETURN"), cancellable = true)
    private static void clampInventorySlotLimit(IStorageWrapper storageWrapper, CallbackInfoReturnable<Integer> cir)
    {
        int clamped = StackSizeRules.clampStackSize(cir.getReturnValue());
        if (clamped != cir.getReturnValue())
            cir.setReturnValue(clamped);
    }
}
