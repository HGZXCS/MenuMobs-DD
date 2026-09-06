package superbas11.menumobs.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import superbas11.menumobs.MenuMobsConfig;

/**
 * The config screen opened from the mods list. Because Forge 1.20.1 does not generate a
 * screen for a {@code ForgeConfigSpec} automatically, this small screen provides the
 * same six options that the original 1.12.2 mod had.
 */
public class ModConfigScreen extends Screen {

    private final Screen parent;

    private CycleButton<Boolean> showMenuButton;
    private CycleButton<Boolean> showPlayersOnlyButton;
    private ForgeSlider volumeSlider;
    private CycleButton<Boolean> debugButton;
    private CycleButton<Boolean> menuButtonsButton;
    private Button fixedButton;
    private Button blacklistButton;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("menumobs.configgui.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int colWidth = 260;
        int left = center - colWidth / 2;
        int row = 40;
        int step = 24;

        this.showMenuButton = CycleButton.onOffBuilder(MenuMobsConfig.showMainMenuMobs())
                .create(left, row, colWidth, 20, Component.translatable("menumobs.config.showMainMenuMobs"));
        this.addRenderableWidget(this.showMenuButton);
        row += step;

        this.showPlayersOnlyButton = CycleButton.onOffBuilder(MenuMobsConfig.showOnlyPlayerModels())
                .create(left, row, colWidth, 20, Component.translatable("menumobs.config.showOnlyPlayerModels"));
        this.addRenderableWidget(this.showPlayersOnlyButton);
        row += step;

        this.volumeSlider = new ForgeSlider(left, row, colWidth, 20,
                Component.translatable("menumobs.config.mobSoundVolume"), Component.empty(),
                0.0D, 1.0D, MenuMobsConfig.mobSoundVolume(), 0.01D, 2, true);
        this.addRenderableWidget(this.volumeSlider);
        row += step;

        this.debugButton = CycleButton.onOffBuilder(MenuMobsConfig.allowDebugOutput())
                .create(left, row, colWidth, 20, Component.translatable("menumobs.config.allowDebugOutput"));
        this.addRenderableWidget(this.debugButton);
        row += step;

        this.menuButtonsButton = CycleButton.onOffBuilder(MenuMobsConfig.showMenuButtons())
                .create(left, row, colWidth, 20, Component.translatable("menumobs.config.showMenuButtons"));
        this.addRenderableWidget(this.menuButtonsButton);
        row += step + 8;

        this.fixedButton = new Button.Builder(
                Component.translatable("menumobs.gui.fixedEntities", count(MenuMobsConfig.fixedMob())),
                b -> this.minecraft.setScreen(new StringListScreen(this,
                        Component.translatable("menumobs.config.fixedMob"), MenuMobsConfig.FIXED_MOB)))
                .bounds(left, row, colWidth, 20)
                .build();
        this.addRenderableWidget(this.fixedButton);
        row += step;

        this.blacklistButton = new Button.Builder(
                Component.translatable("menumobs.gui.blacklist", count(MenuMobsConfig.blacklist())),
                b -> this.minecraft.setScreen(new StringListScreen(this,
                        Component.translatable("menumobs.config.blacklist"), MenuMobsConfig.BLACKLIST)))
                .bounds(left, row, colWidth, 20)
                .build();
        row += step + 8;

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), b -> saveAndClose())
                .bounds(this.width / 2 - 155, this.height - 28, 150, 20)
                .build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), b -> this.onClose())
                .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
                .build());
    }

    private static int count(String[] values) {
        return values == null ? 0 : values.length;
    }

    private void saveAndClose() {
        MenuMobsConfig.SHOW_MAIN_MENU_MOBS.set(this.showMenuButton.getValue());
        MenuMobsConfig.SHOW_ONLY_PLAYER_MODELS.set(this.showPlayersOnlyButton.getValue());
        MenuMobsConfig.MOB_SOUND_VOLUME.set(this.volumeSlider.getValue());
        MenuMobsConfig.ALLOW_DEBUG_OUTPUT.set(this.debugButton.getValue());
        MenuMobsConfig.SHOW_MENU_BUTTONS.set(this.menuButtonsButton.getValue());
        MenuMobsConfig.SPEC.save();
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
