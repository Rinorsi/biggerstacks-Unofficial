/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import portb.biggerstacks.Constants;
import portb.biggerstacks.config.StackSizeRules;
import portb.biggerstacks.config.TemplateOverrideConfig;
import portb.configlib.ItemProperties;
import portb.configlib.TagAccessor;

import static portb.biggerstacks.BiggerStacks.LOGGER;

public class ItemStackSizeHelper
{
    private static boolean loggedApplyCall = false;

    public static void applyStackSizeToItem(ItemStack itemstack, CallbackInfoReturnable<Integer> returnInfo)
    {
        var overrideSize = TemplateOverrideConfig.resolveOverride(itemstack);
        if (overrideSize.isPresent())
        {
            int clampedStackSize = StackSizeRules.clampStackSize(overrideSize.getAsInt());
            returnInfo.cancel();
            returnInfo.setReturnValue(clampedStackSize);
            return;
        }

        if (StackSizeRules.getRuleSet() != null)
        {
            var item    = itemstack.getItem();
            var itemKey = BuiltInRegistries.ITEM.getKey(item);
            var namespace = itemKey != null ? itemKey.getNamespace() : Constants.MOD_ID;
            var identifier = itemKey != null ? itemKey.toString() : item.getClass().getName();
            boolean isEdible          = item.components().has(DataComponents.FOOD);
            boolean isBlock          = item instanceof BlockItem;
            boolean canBeDepleted    = itemstack.isDamageableItem();
            boolean isBucket         = item instanceof BucketItem;
            int      currentMaxStack = returnInfo.getReturnValue();
            
            StackSizeRules.getRuleSet().determineStackSizeForItem(
                                  new ItemProperties(
                                          namespace,
                                          identifier,
                                          "", //fixme this was never used anyway. remove it some time.
                                          currentMaxStack,
                                          isEdible,
                                          isBlock,
                                          canBeDepleted,
                                          isBucket,
                                          new TagAccessorImpl(itemstack),
                                          item.getClass()
                                  )
                          )
                          .ifPresent((stackSize) -> {
                              int clampedStackSize = StackSizeRules.clampStackSize(stackSize);
                              if (!loggedApplyCall)
                              {
                                  loggedApplyCall = true;
                                  LOGGER.info("ItemStackSizeHelper invoked; sample item {} -> {}", identifier, clampedStackSize);
                              }
                              returnInfo.cancel();
                              returnInfo.setReturnValue(clampedStackSize);
                          });
        }
        else
        {
            LOGGER.debug("Stack size ruleset is somehow null, using fallback logic. Called from " +
                                 CallingClassUtil.getCallerClassName());
            
            if (returnInfo.getReturnValue() > 1)
            {
                returnInfo.cancel();
                returnInfo.setReturnValue(StackSizeHelper.getNewStackSize());
            }
        }
    }
    
    private record TagAccessorImpl(ItemStack item) implements TagAccessor
    {
        @Override
        public boolean doesItemHaveTag(String tag)
        {
            ResourceLocation tagId = ResourceLocation.parse(tag);
            return item.is(TagKey.create(Registries.ITEM, tagId));
        }
    }
}
