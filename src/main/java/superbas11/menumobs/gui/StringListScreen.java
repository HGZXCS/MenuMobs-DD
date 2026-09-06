package superbas11.menumobs.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * A small string-list editor screen used for the {@code fixedMob} and {@code blacklist}
 * options. Entries are edited in text rows; empty rows are dropped on save.
 */
public class StringListScreen extends Screen {

    private final Screen parent;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> configValue;
    private final List<String> draft = new ArrayList<>();
    private final List<EditBox> boxes = new ArrayList<>();
    private static final int MAX_VISIBLE_ROWS = 12;

    public StringListScreen(Screen parent, Component title,
                            ForgeConfigSpec.ConfigValue<List<? extends String>> configValue) {
        super(title);
        this.parent = parent;
        this.configValue = configValue;
    }

    @Override
    protected void init() {
        this.draft.clear();
        List<? extends String> current = this.configValue.get();
        if (current != null) {
            this.draft.addAll(current);
        }
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        this.boxes.clear();

        int center = this.width / 2;
        int rowY = 40;

        List<String> entries = new ArrayList<>(this.draft);
        if (entries.isEmpty()) {
            entries.add("");
        }
        int rowCount = Math.min(entries.size(), MAX_VISIBLE_ROWS);
        for (int i = 0; i < rowCount && rowY <= this.height - 70; i++) {
            final int index = i;
            EditBox box = new EditBox(this.font, center - 130, rowY, 260, 18,
                    Component.translatable("menumobs.gui.entryLabel"));
            box.setMaxLength(128);
            box.setValue(entries.get(i));
            box.setResponder(value -> {
                while (this.draft.size() <= index) {
                    this.draft.add("");
                }
                this.draft.set(index, value);
            });
            this.addRenderableWidget(box);
            this.boxes.add(box);
            rowY += 20;
        }

        int buttonY = this.height - 28;

        this.addRenderableWidget(new Button.Builder(
                Component.translatable("menumobs.gui.addRow"), b -> addRow())
                .bounds(center - 155, buttonY, 70, 20)
                .build());
        this.addRenderableWidget(new Button.Builder(
                Component.translatable("menumobs.gui.removeLast"), b -> removeLastRow())
                .bounds(center - 83, buttonY, 100, 20)
                .build());

        this.addRenderableWidget(new Button.Builder(
                Component.translatable("gui.done"), b -> saveAndClose())
                .bounds(center + 20, buttonY, 60, 20)
                .build());
        this.addRenderableWidget(new Button.Builder(
                Component.translatable("gui.cancel"), b -> this.onClose())
                .bounds(center + 84, buttonY, 70, 20)
                .build());
    }

    private void addRow() {
        this.draft.add("");
        rebuild();
        if (!this.boxes.isEmpty()) {
            EditBox last = this.boxes.get(this.boxes.size() - 1);
            last.setFocused(true);
            this.setFocused(last);
        }
    }

    private void removeLastRow() {
        if (this.draft.size() <= 1) {
            return;
        }
        this.draft.remove(this.draft.size() - 1);
        rebuild();
    }

    private void saveAndClose() {
        List<String> entries = new ArrayList<>();
        for (String value : this.draft) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty() && !entries.contains(trimmed)) {
                entries.add(trimmed);
            }
        }
        this.configValue.set(entries);
        this.configValue.save();
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
