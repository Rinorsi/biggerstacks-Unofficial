/*
 * Copyright (c) PORTB 2025
 *
 * Licensed under GNU LGPL v3
 * https://www.gnu.org/licenses/lgpl-3.0.txt
 */

package portb.biggerstacks.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.network.PacketDistributor;
import portb.biggerstacks.config.TemplateOverrideConfig;
import portb.biggerstacks.net.ClientboundOverrideManagerPacket;
import portb.biggerstacks.net.ServerboundOverrideUpdatePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class OverrideManagerScreen extends Screen
{
    private final List<TemplateOverrideConfig.StackOverride> overrides;
    private EditBox searchBox;
    private OverrideList entryList;
    private MultiLineLabel emptyLabel = MultiLineLabel.EMPTY;
    private CycleButton<TemplateOverrideConfig.StackCategory> categoryButton;
    private CycleButton<TemplateOverrideConfig.TargetType> targetTypeButton;
    private EditBox targetInput;
    private Button removeButton;
    private Button applyButton;
    private Button newButton;
    private Button pickButton;
    private TemplateOverrideConfig.StackOverride selected;
    private Component statusMessage = Component.empty();
    private int statusColor = 0xA0A0A0;

    private static final int PANEL_MARGIN = 18;
    private static final int PANEL_TOP = 60;
    private static final int PANEL_BOTTOM_MARGIN = 36;
    private static final int PANEL_GAP = 24;

    public OverrideManagerScreen(ClientboundOverrideManagerPacket packet)
    {
        super(Component.translatable("biggerstacks.override_manager.title"));
        this.overrides = new ArrayList<>(packet.overrides());
    }

    public static void open(ClientboundOverrideManagerPacket packet)
    {
        Minecraft.getInstance().setScreen(new OverrideManagerScreen(packet));
    }

    public void reloadFromPacket(ClientboundOverrideManagerPacket packet)
    {
        overrides.clear();
        overrides.addAll(packet.overrides());
        setSelected(null);
        refreshList();
    }

    @Override
    protected void init()
    {
        int availableWidth = width - PANEL_MARGIN * 2 - PANEL_GAP;
        int columnWidth = Math.min(320, availableWidth / 2);
        int leftPanelLeft = PANEL_MARGIN;
        int rightPanelLeft = width - PANEL_MARGIN - columnWidth;
        int listTop = PANEL_TOP + 14;

        searchBox = new EditBox(font, leftPanelLeft + 12, PANEL_TOP - 26, columnWidth - 24, 18, Component.translatable("gui.search"));
        addRenderableWidget(searchBox);

        entryList = new OverrideList(minecraft, columnWidth - 24, height - PANEL_BOTTOM_MARGIN - listTop - 6, listTop, height - PANEL_BOTTOM_MARGIN);
        entryList.setLeft(leftPanelLeft + 12);
        addRenderableWidget(entryList);

        emptyLabel = MultiLineLabel.create(font, Component.translatable("biggerstacks.override_manager.empty"), columnWidth - 24);

        int rightColumnX = rightPanelLeft + 12;
        int sectionWidth = columnWidth - 24;
        int rowY = PANEL_TOP;
        categoryButton = addRenderableWidget(
                CycleButton.<TemplateOverrideConfig.StackCategory>builder(value -> Component.literal(value.name()))
                        .withValues(TemplateOverrideConfig.StackCategory.values())
                        .displayOnlyValue()
                        .create(rightColumnX, rowY, sectionWidth / 2 - 6, 20, Component.translatable("biggerstacks.override_manager.category"), (button, value) -> {
                        }));
        targetTypeButton = addRenderableWidget(
                CycleButton.<TemplateOverrideConfig.TargetType>builder(value -> Component.literal(value.name()))
                        .withValues(TemplateOverrideConfig.TargetType.values())
                        .displayOnlyValue()
                        .create(rightColumnX + sectionWidth / 2 + 6, rowY, sectionWidth / 2 - 6, 20, Component.translatable("biggerstacks.override_manager.target_type"), (button, value) -> {
                        }));

        rowY += 30;
        targetInput = new EditBox(font, rightColumnX, rowY, sectionWidth, 18, Component.translatable("biggerstacks.override_manager.target"));
        addRenderableWidget(targetInput);

        rowY += 24;
        pickButton = Button.builder(Component.translatable("biggerstacks.override_manager.pick"), b -> openPicker())
                .bounds(rightColumnX, rowY, sectionWidth, 20).build();
        addRenderableWidget(pickButton);

        rowY += 28;
        applyButton = Button.builder(Component.translatable("biggerstacks.override_manager.apply"), b -> applyChanges())
                .bounds(rightColumnX, rowY, sectionWidth, 20).build();
        addRenderableWidget(applyButton);
        targetInput.setResponder(value -> applyButton.active = !value.trim().isEmpty());

        rowY += 28;
        int halfWidth = (sectionWidth - 8) / 2;
        newButton = Button.builder(Component.translatable("biggerstacks.override_manager.new"), b -> {
            setSelected(null);
            entryList.setSelected(null);
            targetInput.setValue("");
            applyButton.active = false;
            setStatus(Component.empty(), 0xA0A0A0);
        }).bounds(rightColumnX, rowY, halfWidth, 20).build();
        addRenderableWidget(newButton);

        removeButton = Button.builder(Component.translatable("biggerstacks.override_manager.remove"), b -> removeSelected())
                .bounds(rightColumnX + halfWidth + 8, rowY, halfWidth, 20).build();
        removeButton.active = false;
        addRenderableWidget(removeButton);

        refreshList();
        searchBox.setResponder(text -> refreshList());
    }

    private void refreshList()
    {
        String query = searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        List<TemplateOverrideConfig.StackOverride> filtered = overrides.stream()
                .filter(entry -> query.isEmpty() || entry.target().contains(query))
                .collect(Collectors.toList());

        entryList.setEntries(filtered);
        if (!filtered.contains(selected))
        {
            selected = null;
        }
        updateDetails();
    }

    private void updateDetails()
    {
        if (selected == null)
        {
            categoryButton.setValue(TemplateOverrideConfig.StackCategory.NORMAL);
            targetTypeButton.setValue(TemplateOverrideConfig.TargetType.ITEM);
            targetInput.setValue("");
            removeButton.active = false;
            return;
        }

        categoryButton.setValue(selected.category());
        targetTypeButton.setValue(selected.targetType());
        targetInput.setValue(selected.target());
        removeButton.active = true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)
    {
        if (entryList != null && entryList.mouseScrolled(mouseX, mouseY, deltaX, deltaY))
        {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanels(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int availableWidth = width - PANEL_MARGIN * 2 - PANEL_GAP;
        int columnWidth = Math.min(320, availableWidth / 2);
        if (entryList.children().isEmpty())
        {
            emptyLabel.renderCentered(graphics, PANEL_MARGIN + columnWidth / 2, height / 2);
        }
        graphics.drawString(font, Component.translatable("biggerstacks.override_manager.list_title"), PANEL_MARGIN + 10, PANEL_TOP - 24, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("biggerstacks.override_manager.category"), width - PANEL_MARGIN - columnWidth + 10, PANEL_TOP - 24, 0xFFFFFF);
        graphics.drawString(font, statusMessage, width - font.width(statusMessage) - 12, height - 18, statusColor);
    }

    private void drawPanels(GuiGraphics graphics)
    {
        int availableWidth = width - PANEL_MARGIN * 2 - PANEL_GAP;
        int columnWidth = Math.min(320, availableWidth / 2);
        int leftPanelLeft = PANEL_MARGIN;
        int leftPanelTop = PANEL_TOP - 32;
        int leftPanelBottom = height - PANEL_BOTTOM_MARGIN;
        int rightPanelLeft = width - PANEL_MARGIN - columnWidth;

        int bgColor = 0xC0101010;
        graphics.fill(leftPanelLeft - 8, leftPanelTop, leftPanelLeft + columnWidth + 8, leftPanelBottom, bgColor);
        graphics.fill(rightPanelLeft - 8, leftPanelTop, rightPanelLeft + columnWidth + 8, leftPanelBottom, bgColor);
        graphics.renderOutline(leftPanelLeft - 8, leftPanelTop, columnWidth + 16, leftPanelBottom - leftPanelTop, 0xFF888888);
        graphics.renderOutline(rightPanelLeft - 8, leftPanelTop, columnWidth + 16, leftPanelBottom - leftPanelTop, 0xFF888888);
    }

    private void setSelected(TemplateOverrideConfig.StackOverride overrideEntry)
    {
        this.selected = overrideEntry;
        updateDetails();
    }

    private void applyChanges()
    {
        String targetText = targetInput.getValue().trim().toLowerCase(Locale.ROOT);
        if (targetText.isEmpty())
        {
            setStatus(Component.translatable("biggerstacks.override_manager.status.invalid"), 0xFF5555);
            return;
        }

        try
        {
            ResourceLocation.parse(targetText);
        }
        catch (IllegalArgumentException e)
        {
            setStatus(Component.translatable("biggerstacks.override_manager.status.invalid"), 0xFF5555);
            return;
        }

        TemplateOverrideConfig.StackOverride entry = new TemplateOverrideConfig.StackOverride(
                targetTypeButton.getValue(),
                targetText,
                categoryButton.getValue());

        overrides.removeIf(existing -> existing.target().equals(entry.target()) && existing.targetType() == entry.targetType());
        overrides.add(entry);
        selected = entry;
        sortOverrides();
        refreshList();
        entryList.selectEntry(entry);
        sendOverridesToServer();
        setStatus(Component.translatable("biggerstacks.override_manager.status.saved"), 0x55FF55);
        applyButton.active = false;
    }

    private void openPicker()
    {
        if (minecraft == null || minecraft.player == null)
        {
            return;
        }
        Minecraft.getInstance().setScreen(new ItemPickerScreen(this, pickedId -> {
            targetTypeButton.setValue(TemplateOverrideConfig.TargetType.ITEM);
            targetInput.setValue(pickedId.toString());
            applyButton.active = true;
            setStatus(Component.translatable("biggerstacks.override_manager.status.picked", pickedId.toString()), 0xFFFFFF);
        }));
    }

    private void removeSelected()
    {
        if (selected == null)
        {
            return;
        }
        overrides.remove(selected);
        selected = null;
        sortOverrides();
        refreshList();
        sendOverridesToServer();
        setStatus(Component.translatable("biggerstacks.override_manager.status.removed"), 0x55FF55);
        applyButton.active = false;
    }

    private void sortOverrides()
    {
        overrides.sort((a, b) -> {
            int typeCmp = a.targetType().compareTo(b.targetType());
            if (typeCmp != 0)
            {
                return typeCmp;
            }
            return a.target().compareTo(b.target());
        });
    }

    private void sendOverridesToServer()
    {
        PacketDistributor.sendToServer(new ServerboundOverrideUpdatePacket(new ArrayList<>(overrides)));
    }

    private void setStatus(Component message, int color)
    {
        this.statusMessage = message;
        this.statusColor = color;
        if (minecraft != null && minecraft.player != null)
        {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    private class OverrideList extends ObjectSelectionList<OverrideList.Entry>
    {
        private int left;

        public OverrideList(Minecraft minecraft, int width, int height, int top, int bottom)
        {
            super(minecraft, width, height, top, bottom);
        }

        public void setLeft(int left)
        {
            this.left = left;
        }

        public void setEntries(List<TemplateOverrideConfig.StackOverride> entries)
        {
            replaceEntries(entries.stream().map(Entry::new).collect(Collectors.toList()));
            selectEntry(selected);
        }

        @Override
        protected int getScrollbarPosition()
        {
            return left + this.width - 6;
        }

        @Override
        public int getRowLeft()
        {
            return left + 2;
        }

        @Override
        public int getRowWidth()
        {
            return this.width - 12;
        }

        public void selectEntry(TemplateOverrideConfig.StackOverride data)
        {
            if (data == null)
            {
                setSelected(null);
                return;
            }
            for (Entry entry : children())
            {
                if (entry.data.equals(data))
                {
                    setSelected(entry);
                    break;
                }
            }
        }

        private class Entry extends ObjectSelectionList.Entry<Entry>
        {
            private final TemplateOverrideConfig.StackOverride data;
            private final ItemStack preview;
            private final Component mainLabel;
            private final Component secondaryLabel;

            private Entry(TemplateOverrideConfig.StackOverride data)
            {
                this.data = data;
                ItemStack stack = ItemStack.EMPTY;
                Component label;
                if (data.targetType() == TemplateOverrideConfig.TargetType.ITEM)
                {
                    ResourceLocation id = ResourceLocation.tryParse(data.target());
                    if (id != null)
                    {
                        stack = BuiltInRegistries.ITEM.getOptional(id).map(Item::getDefaultInstance).orElse(ItemStack.EMPTY);
                    }
                    label = stack.isEmpty() ? Component.literal(data.target()) : stack.getHoverName();
                }
                else
                {
                    label = Component.literal("#" + data.target());
                }
                this.preview = stack;
                this.mainLabel = label;
                this.secondaryLabel = Component.literal(data.category().name());
            }

            @Override
            public Component getNarration()
            {
                return mainLabel;
            }

            @Override
            public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTick)
            {
                int background = hovered ? 0x50FFFFFF : 0x20000000;
                RenderSystem.disableDepthTest();
                graphics.fill(left, top, left + entryWidth, top + entryHeight, background);
                RenderSystem.enableDepthTest();
                int iconX = left + 6;
                int textX = iconX;
                if (!preview.isEmpty())
                {
                    graphics.renderItem(preview, iconX, top + 4);
                    graphics.renderItemDecorations(font, preview, iconX, top + 4);
                    textX += 22;
                }
                else
                {
                    graphics.drawString(font, "#", iconX, top + 8, 0xFFFFFF);
                    textX += 10;
                }
                graphics.drawString(font, mainLabel, textX, top + 6, 0xFFFFFF);
                graphics.drawString(font, secondaryLabel, textX, top + 6 + font.lineHeight, 0x808080);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button)
            {
                OverrideManagerScreen.this.entryList.setSelected(this);
                OverrideManagerScreen.this.setSelected(data);
                return true;
            }
        }
    }
}
