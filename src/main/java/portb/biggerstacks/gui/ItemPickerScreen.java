/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ItemPickerScreen extends Screen
{
    private final Screen parent;
    private final Consumer<ResourceLocation> onPicked;
    private ItemList list;
    private EditBox searchBox;
    private MultiLineLabel emptyLabel = MultiLineLabel.EMPTY;

    public ItemPickerScreen(Screen parent, Consumer<ResourceLocation> onPicked)
    {
        super(Component.translatable("biggerstacks.picker.title"));
        this.parent = parent;
        this.onPicked = onPicked;
    }

    @Override
    protected void init()
    {
        int listWidth = Math.min(360, width - 60);
        int listLeft = (width - listWidth) / 2;

        searchBox = new EditBox(font, listLeft, 30, listWidth, 18, Component.translatable("gui.search"));
        searchBox.setResponder(text -> refreshList());
        addRenderableWidget(searchBox);

        int listTop = 60;
        list = new ItemList(minecraft, listWidth, height - 140, listTop, height - 80);
        list.setLeft(listLeft);
        addWidget(list);

        emptyLabel = MultiLineLabel.create(font, Component.translatable("biggerstacks.override_manager.empty"), width - 60);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> closeWithParent()).bounds(width / 2 - 90, height - 40, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("biggerstacks.picker.cancel"), b -> closeWithParent()).bounds(width / 2 + 10, height - 40, 80, 20).build());

        refreshList();
    }

    private void refreshList()
    {
        if (minecraft == null || minecraft.player == null)
        {
            return;
        }
        Inventory inv = minecraft.player.getInventory();
        Map<ResourceLocation, ItemStack> uniqueStacks = new LinkedHashMap<>();
        for (ItemStack stack : inv.items)
        {
            if (stack.isEmpty())
            {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(stack.getItem().builtInRegistryHolder().key().location().toString());
            if (id != null && !uniqueStacks.containsKey(id))
            {
                uniqueStacks.put(id, stack.copy());
            }
        }
        String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        list.replaceEntries(uniqueStacks.entrySet().stream()
                .filter(entry -> query.isEmpty() || entry.getKey().toString().contains(query))
                .map(entry -> new ItemEntry(entry.getKey(), entry.getValue()))
                .toList());
    }

    private void closeWithParent()
    {
        Minecraft.getInstance().setScreen(parent);
    }

    private void onPicked(ResourceLocation id)
    {
        onPicked.accept(id);
        closeWithParent();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        list.render(graphics, mouseX, mouseY, partialTick);
        if (list.children().isEmpty())
        {
            emptyLabel.renderCentered(graphics, width / 2, height / 2);
        }
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
    }

    private class ItemList extends ObjectSelectionList<ItemEntry>
    {
        private int left;

        public ItemList(Minecraft minecraft, int width, int height, int top, int bottom)
        {
            super(minecraft, width, height, top, bottom);
        }

        public void setLeft(int left)
        {
            this.left = left;
        }

        public void replaceEntries(java.util.List<ItemEntry> entries)
        {
            clearEntries();
            entries.forEach(this::addEntry);
        }

        @Override
        protected int getScrollbarPosition()
        {
            return left + width - 12;
        }

        @Override
        public int getRowLeft()
        {
            return left + (width - getRowWidth()) / 2;
        }

        @Override
        public int getRowWidth()
        {
            return Math.min(280, width - 12);
        }
    }

    private class ItemEntry extends ObjectSelectionList.Entry<ItemEntry>
    {
        private final ResourceLocation id;
        private final ItemStack stack;

        private ItemEntry(ResourceLocation id, ItemStack stack)
        {
            this.id = id;
            this.stack = stack;
        }

        @Override
        public Component getNarration()
        {
            return Component.literal(id.toString());
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            int background = hovered ? 0x50FFFFFF : 0x20000000;
            graphics.fill(left, top, left + entryWidth, top + entryHeight, background);
            graphics.renderItem(stack, left + 4, top + 2);
            graphics.renderItemDecorations(font, stack, left + 4, top + 2);
            graphics.drawString(font, stack.getHoverName().getString(), left + 28, top + 6, 0xFFFFFF);
            graphics.drawString(font, id.toString(), left + 28, top + 6 + font.lineHeight, 0x808080);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            onPicked(id);
            return true;
        }
    }
}
