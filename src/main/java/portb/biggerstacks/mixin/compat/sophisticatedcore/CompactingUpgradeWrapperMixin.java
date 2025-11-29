/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.compat.sophisticatedcore;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.compacting.CompactingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

@Pseudo
@Mixin(value = CompactingUpgradeWrapper.class, remap = false)
public abstract class CompactingUpgradeWrapperMixin
{
    private static final int MAX_ITEMS_PER_COMPACT_BATCH = 64 * 1024;

    @Shadow
    protected abstract boolean fitsResultAndRemainingItems(IItemHandler inventoryHandler, List<ItemStack> remainingItems, ItemStack result);

    @Inject(method = "tryCompacting", at = @At("HEAD"), cancellable = true)
    private void biggerstacks$batchCompacting(IItemHandlerSimpleInserter inventoryHandler, ItemStack stack, int width, int height, CallbackInfo ci)
    {
        int totalCount = width * height;
        CompactingResult compactingResult = RecipeHelper.getCompactingResult(stack, width, height);
        if (compactingResult.getResult().isEmpty())
        {
            ci.cancel();
            return;
        }

        ItemStack extractedStack = InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), inventoryHandler, true);
        if (extractedStack.getCount() != totalCount)
        {
            ci.cancel();
            return;
        }

        int maxConversionsPerCall = Math.max(1, MAX_ITEMS_PER_COMPACT_BATCH / totalCount);
        int conversions = 0;

        while (extractedStack.getCount() == totalCount && conversions < maxConversionsPerCall)
        {
            ItemStack resultCopy = compactingResult.getResult().copy();
            List<ItemStack> remainingItemsCopy = compactingResult.getRemainingItems().isEmpty()
                    ? Collections.emptyList()
                    : compactingResult.getRemainingItems().stream().map(ItemStack::copy).toList();

            if (!fitsResultAndRemainingItems(inventoryHandler, remainingItemsCopy, resultCopy))
                break;

            InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), inventoryHandler, false);
            inventoryHandler.insertItem(resultCopy, false);
            InventoryHelper.insertIntoInventory(remainingItemsCopy, inventoryHandler, false);
            extractedStack = InventoryHelper.extractFromInventory(stack.copyWithCount(totalCount), inventoryHandler, true);
            conversions++;
        }

        ci.cancel();
    }
}
