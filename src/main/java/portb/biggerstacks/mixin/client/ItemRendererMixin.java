/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import portb.biggerstacks.config.ClientConfig;

import java.text.DecimalFormat;

import static portb.biggerstacks.Constants.ONE_BILLION;
import static portb.biggerstacks.Constants.ONE_MILLION;
import static portb.biggerstacks.Constants.ONE_THOUSAND;

@Mixin(GuiGraphics.class)
public abstract class ItemRendererMixin
{
    @Shadow private net.minecraft.client.Minecraft minecraft;
    @Shadow private com.mojang.blaze3d.vertex.PoseStack pose;

    private static final DecimalFormat BILLION_FORMAT  = new DecimalFormat("#.##B");
    private static final DecimalFormat MILLION_FORMAT  = new DecimalFormat("#.##M");
    private static final DecimalFormat THOUSAND_FORMAT = new DecimalFormat("#.##K");
    
    private static String getStringForBigStackCount(int count)
    {
        if (ClientConfig.enableNumberShortening.get())
        {
            double value = count;
            
            if (value >= ONE_BILLION)
                return BILLION_FORMAT.format(value / ONE_BILLION);
            else if (value >= ONE_MILLION)
                return MILLION_FORMAT.format(value / ONE_MILLION);
            else if (value > ONE_THOUSAND)
                return THOUSAND_FORMAT.format(value / ONE_THOUSAND);
        }
        
        return String.valueOf(count);
    }
    
    private static double calculateStringScale(Font font, String countString)
    {
        var width = font.width(countString);
        
        if (width < 16)
            return 1.0;
        else
            return 16.0 / width;
    }
    
    // Inject at the beginning of the method to replace all vanilla rendering
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void renderText(Font font, ItemStack itemStack, int x, int y, String alternateCount, CallbackInfo ci)
    {
        if (itemStack.isEmpty())
            return;

        this.pose.pushPose();

        if (itemStack.getCount() != 1 || alternateCount != null)
        {
            String text = alternateCount == null ? getStringForBigStackCount(itemStack.getCount()) : alternateCount;
            double scale = calculateStringScale(font, text);
            float scaledWidth = (float)(font.width(text) * scale);
            float rightEdge = x + 17;
            float baseX = rightEdge - scaledWidth;
            float baseY = y + 9;

            this.pose.pushPose();
            this.pose.translate(baseX, baseY, 200.0F);
            this.pose.scale((float) scale, (float) scale, 1.0F);
            ((GuiGraphics)(Object)this).drawString(font, text, 0, 0, 0xFFFFFF, true);
            this.pose.popPose();
        }

        if (itemStack.isBarVisible())
        {
            int width = itemStack.getBarWidth();
            int color = itemStack.getBarColor();
            int barX = x + 2;
            int barY = y + 13;
            ((GuiGraphics)(Object)this).fill(RenderType.guiOverlay(), barX, barY, barX + 13, barY + 2, 0xFF000000);
            ((GuiGraphics)(Object)this).fill(RenderType.guiOverlay(), barX, barY, barX + width, barY + 1, color | 0xFF000000);
        }

        LocalPlayer localplayer = this.minecraft.player;
        float cooldown = localplayer == null ? 0.0F : localplayer.getCooldowns().getCooldownPercent(
                itemStack.getItem(), this.minecraft.getTimer().getGameTimeDeltaPartialTick(true));

        if (cooldown > 0.0F)
        {
            int startY = y + net.minecraft.util.Mth.floor(16.0F * (1.0F - cooldown));
            int endY = startY + net.minecraft.util.Mth.ceil(16.0F * cooldown);
            ((GuiGraphics)(Object)this).fill(RenderType.guiOverlay(), x, startY, x + 16, endY, Integer.MAX_VALUE);
        }

        this.pose.popPose();
        net.neoforged.neoforge.client.ItemDecoratorHandler.of(itemStack).render((GuiGraphics)(Object)this, font, itemStack, x, y);
        ci.cancel();
    }
}
