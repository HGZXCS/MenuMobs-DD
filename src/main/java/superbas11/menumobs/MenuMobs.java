package superbas11.menumobs;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import superbas11.menumobs.gui.ModConfigScreen;

/**
 * Menu Mobs - Minecraft 1.20.1 port.
 * Gimmick mod that renders the current player and a random mob on the in-game menus.
 */
@Mod(Reference.MODID)
public class MenuMobs {

    public MenuMobs() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::clientSetup);

        // Config file: config/menumobs-client.toml
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MenuMobsConfig.SPEC);
    }

    /**
     * Client-only initialisation (this mod never loads on a dedicated server thanks to
     * {@code clientSideOnly=true} in mods.toml).
     */
    private void clientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        MinecraftForge.registerConfigScreen(parent -> new ModConfigScreen(parent));
        MinecraftForge.EVENT_BUS.register(new MainMenuRenderTicker());
        // Start fetching the logged-in player's skin textures on a background thread now,
        // so the first main-menu visit does not flash the default skin.
        superbas11.menumobs.client.util.PlayerProfiles.warmStart();
    }
}
