package superbas11.menumobs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import superbas11.menumobs.client.util.EntityUtils;
import superbas11.menumobs.client.util.EntityUtils.MenuClientPlayer;
import superbas11.menumobs.client.util.PlayerProfiles;
import superbas11.menumobs.util.FakeClientLevel;
import superbas11.menumobs.util.FakeClientPacketListener;
import superbas11.menumobs.util.LogHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Renders the current player and a random mob on the main menu.
 *
 * <p>Entities are drawn through the vanilla {@code InventoryScreen} preview helper on the
 * {@code ScreenEvent.Render.Post} hook (after the TitleScreen has finished drawing).
 * The mobs and the player live in a tiny fake {@code ClientLevel} that is only used so the
 * entities can be constructed; they are not really simulated.
 */
@OnlyIn(Dist.CLIENT)
public class MainMenuRenderTicker {

    private static final Random RANDOM = new Random();

    private final Minecraft mc;
    private FakeClientPacketListener netHandler;
    private FakeClientLevel level;
    private LivingEntity menuEntity;     // the random (or fixed) entity on the left
    private MenuClientPlayer player;     // the logged in player on the right
    private net.minecraft.client.player.LocalPlayer contextPlayer; // stands in for mc.player while rendering
    private boolean erroredOut = false;  // a serious error happened - give up for this session
    private boolean wasOnMenu = false;
    private int ambientTicks;

    public MainMenuRenderTicker() {
        this.mc = Minecraft.getInstance();
    }

    public void register() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LogHelper.allowDebugOutput = MenuMobsConfig.allowDebugOutput();

        boolean showMenu = MenuMobsConfig.showMainMenuMobs()
                && !erroredOut
                && isTitleScreen(this.mc.screen);

        if (!showMenu) {
            // Keep the fake world/entities alive between menu visits so the player's skin
            // (and the PlayerInfo holding it) never has to be re-fetched and re-registered.
            this.wasOnMenu = false;
            return;
        }

        if (!this.wasOnMenu) {
            this.wasOnMenu = true;
            onMenuOpened();
        }

        try {
            ensureReady();
            updateAmbientSounds();
        } catch (Throwable t) {
            disableForSession("Menu mob rendering encountered a serious error and has been disabled for the remainder of this session.", t);
        }
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        if (!isReady() || !isTitleScreen(event.getScreen())) {
            return;
        }
        try {
            drawEntities(event);
        } catch (Throwable t) {
            disableForSession("Menu mob rendering encountered a serious error and has been disabled for the remainder of this session.", t);
        }
    }

    @SubscribeEvent
    public void onMouseClick(ScreenEvent.MouseButtonPressed.Post event) {
        if (!isReady() || !isTitleScreen(event.getScreen())) {
            return;
        }
        try {
            handleMouseClick(event);
        } catch (Throwable t) {
            disableForSession("Menu mob rendering encountered a serious error and has been disabled for the remainder of this session.", t);
        }
    }

    // ---------------------------------------------------------------- helpers

    private void onMenuOpened() {
        // Always show a fresh random entity when (re-)entering the main menu. The player
        // model on the right (and its loaded skin) is kept alive and never re-created.
        this.menuEntity = null;
    }

    private boolean isReady() {
        return this.level != null && this.player != null && this.contextPlayer != null && this.menuEntity != null;
    }

    private void cleanup() {
        if (this.netHandler != null) {
            this.netHandler.close();
        }
        this.netHandler = null;
        this.level = null;
        this.player = null;
        this.contextPlayer = null;
        this.menuEntity = null;
        this.ambientTicks = 0;
    }

    private void disableForSession(String message, Throwable t) {
        LogHelper.severe(message);
        LogHelper.severe(String.valueOf(t.getMessage()), t);
        this.erroredOut = true;
        cleanup();
    }

    private void ensureReady() {
        if (this.level == null) {
            this.netHandler = new FakeClientPacketListener(PlayerProfiles.localProfile());
            this.level = new FakeClientLevel(this.netHandler);
        }
        if (this.player == null) {
            this.player = EntityUtils.createLocalPlayer(this.level);
        }
        if (this.contextPlayer == null) {
            // Some vanilla rendering paths (name tag visibility, ...) read Minecraft.player
            // and assume it is never null. Provide a LocalPlayer that lives on the fake
            // level while the menu entities are drawn.
            this.contextPlayer = new net.minecraft.client.player.LocalPlayer(this.mc, this.level, this.netHandler,
                    new net.minecraft.stats.StatsCounter(), new net.minecraft.client.ClientRecipeBook(), false, false);
        }
        if (this.menuEntity == null) {
            chooseMenuEntity();
        }
    }

    private void chooseMenuEntity() {
        String[] fixed = MenuMobsConfig.fixedMob();
        if (fixed.length > 0) {
            this.menuEntity = EntityUtils.createFixedEntity(this.level, fixed[RANDOM.nextInt(fixed.length)], RANDOM);
        } else if (MenuMobsConfig.showOnlyPlayerModels()) {
            this.menuEntity = EntityUtils.createRandomKnownPlayer(this.level, RANDOM);
        } else {
            this.menuEntity = EntityUtils.spawnRandomLivingEntity(this.level, combinedBlacklist());
        }
        // Last resort fallback: a random player model.
        if (this.menuEntity == null) {
            this.menuEntity = EntityUtils.createRandomKnownPlayer(this.level, RANDOM);
        }
    }

    private Set<String> combinedBlacklist() {
        Set<String> blacklist = new HashSet<>(EntityUtils.BUILT_IN_BLACKLIST);
        List<? extends String> userBlacklist = MenuMobsConfig.BLACKLIST.get();
        if (userBlacklist != null) {
            blacklist.addAll(userBlacklist);
        }
        return blacklist;
    }

    private void updateAmbientSounds() {
        if (this.menuEntity instanceof Mob mob && MenuMobsConfig.mobSoundVolume() > 0.0F) {
            if (++this.ambientTicks >= Math.max(20, mob.getAmbientSoundInterval())) {
                this.ambientTicks = 0;
                try {
                    mob.playAmbientSound();
                } catch (Throwable ignored) {
                    // Some exotic mobs may not have a usable ambient sound.
                }
            }
        }
    }

    // ---------------------------------------------------------------- drawing

    private void drawEntities(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int guiWidth = screen.width;
        int guiHeight = screen.height;
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();

        // On the title screen no world is ever rendered, so the entity render dispatcher
        // has no camera yet - hand it one, otherwise rendering the entities crashes.
        this.mc.getEntityRenderDispatcher().prepare(this.level, this.mc.gameRenderer.getMainCamera(), this.player);

        // Vanilla entity rendering reads Minecraft.player (e.g. name tag visibility) and
        // assumes it is never null - temporarily install the fake LocalPlayer while drawing.
        net.minecraft.client.player.LocalPlayer previousPlayer = this.mc.player;
        this.mc.player = this.contextPlayer;

        try {
            // Horizontal slot positions mirroring the original mod.
            int distanceToSide = Math.max(24, (guiWidth / 2 - 98) / 2);
            int feetY = (int) (guiHeight * 0.62F);
            // Reference "pixels per block": the original aimed the entities at 1/5 of the
            // screen height, scaled for the 1.8 block tall player model.
            int pixelsPerBlock = Math.max(8, (int) ((guiHeight / 5.0F) / 1.8F));

            LivingEntity mob = this.menuEntity;
            LivingEntity you = this.player;

            int mobSize = EntityUtils.sizeForEntity(mob, pixelsPerBlock);
            int youSize = EntityUtils.sizeForEntity(you, pixelsPerBlock);

            EntityUtils.drawEntityOnScreen(guiGraphics, distanceToSide, feetY, mobSize,
                    (float) (distanceToSide - mouseX), (float) (feetY - mouseY), mob);
            EntityUtils.drawEntityOnScreen(guiGraphics, guiWidth - distanceToSide, feetY, youSize,
                    (float) (guiWidth - distanceToSide - mouseX), (float) (feetY - mouseY), you);

            drawMenuButtons(event);

            // Make sure everything we just queued is submitted this frame (the vanilla
            // screen flush already happened before the Render.Post event).
            guiGraphics.flush();
        } finally {
            this.mc.player = previousPlayer;
        }
    }

    private void drawMenuButtons(ScreenEvent.Render.Post event) {
        if (!MenuMobsConfig.showMenuButtons()) {
            return;
        }
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        boolean showingPlayer = this.menuEntity instanceof MenuClientPlayer;

        // Zone 1 (0-7, 0-7): blacklist the current mob (not shown for players).
        // Zone 2 (8-15, 0-7): next entity.
        if (mouseX < 8 && mouseY < 8) {
            Component tooltip = showingPlayer
                    ? Component.translatable("menumobs.gui.nextMob")
                    : Component.translatable("menumobs.gui.addToBlacklist");
            int tooltipWidth = this.mc.font.width(tooltip);
            guiGraphics.fill(12, 12, 12 + tooltipWidth + 6, 12 + 9 + 4, 0xCC000000);
            guiGraphics.drawString(this.mc.font, tooltip, 15, 14, 0xFFFFFFFF);
        }

        int x;
        if (!showingPlayer) {
            // blacklist button
            guiGraphics.fill(0, 0, 8, 8, 0xAA808080);
            guiGraphics.fill(1, 3, 7, 5, 0xFFE0E0E0);
            guiGraphics.fill(3, 1, 5, 7, 0xFFE0E0E0);
            // next button
            x = 9;
            guiGraphics.fill(x, 0, x + 8, 8, 0xAA808080);
            guiGraphics.drawString(this.mc.font, ">", x + 2, 0, 0xFFFFFFFF);
        } else {
            x = 0;
            guiGraphics.fill(x, 0, x + 8, 8, 0xAA808080);
            guiGraphics.drawString(this.mc.font, ">", x + 2, 0, 0xFFFFFFFF);
        }
    }

    private void handleMouseClick(ScreenEvent.MouseButtonPressed.Post event) {
        if (!MenuMobsConfig.showMenuButtons()) {
            return;
        }
        if (event.getButton() != 0) {
            return;
        }
        int mouseX = (int) Math.round(event.getMouseX());
        int mouseY = (int) Math.round(event.getMouseY());
        boolean showingPlayer = this.menuEntity instanceof MenuClientPlayer;

        if (mouseY >= 8) {
            return;
        }

        if (showingPlayer) {
            if (mouseX < 8) {
                // Next
                this.menuEntity = null;
            }
            return;
        }

        if (mouseX < 8) {
            // Add the current entity to the config blacklist and show another one.
            net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(this.menuEntity.getType());
            if (key != null) {
                List<String> list = new java.util.ArrayList<>(MenuMobsConfig.BLACKLIST.get());
                String entry = key.toString();
                if (!list.contains(entry)) {
                    list.add(entry);
                    MenuMobsConfig.BLACKLIST.set(list);
                    MenuMobsConfig.SPEC.save();
                }
            }
            this.menuEntity = null;
        } else if (mouseX < 16) {
            // Next
            this.menuEntity = null;
        }
    }

    private boolean isTitleScreen(Screen gui) {
        return gui instanceof TitleScreen;
    }
}
