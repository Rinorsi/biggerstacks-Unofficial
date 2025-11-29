/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.compat.sophisticatedcore;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase.class, remap = false)
public abstract class StorageContainerMenuBaseMixin
{
    @Shadow private boolean slotsChangedSinceStartOfClick;

    @Unique
    private static final int BIGGERSTACKS_MAX_QUICK_MOVE_CALLS = 4096;

    @Unique
    private int biggerstacks$quickMoveCallCount = 0;

    @Inject(method = "doClick", at = @At("HEAD"))
    private void biggerstacks$resetQuickMoveCount(int slotId, int dragType, ClickType clickType, Player player, CallbackInfo ci)
    {
        biggerstacks$quickMoveCallCount = 0;
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void biggerstacks$limitQuickMoveStack(Player player, int index, CallbackInfoReturnable<ItemStack> cir)
    {
        if (biggerstacks$quickMoveCallCount >= BIGGERSTACKS_MAX_QUICK_MOVE_CALLS)
        {
            slotsChangedSinceStartOfClick = true;
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        biggerstacks$quickMoveCallCount++;
    }
}
