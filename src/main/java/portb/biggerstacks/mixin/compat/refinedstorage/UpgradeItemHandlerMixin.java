/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.compat.refinedstorage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import portb.biggerstacks.config.ServerConfig;
import portb.biggerstacks.util.StackSizeHelper;

@Pseudo
@Mixin(targets = "com.refinedmods.refinedstorage.inventory.item.UpgradeItemHandler")
public class UpgradeItemHandlerMixin
{
    @Inject(method = "getStackInteractCount", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void increaseTransferRate(CallbackInfoReturnable<Integer> returnInfo)
    {
        if (ServerConfig.get().increaseTransferRate.get())
        {
            StackSizeHelper.scaleTransferRate(returnInfo, false);
        }
    }
}
