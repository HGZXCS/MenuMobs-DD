package superbas11.menumobs;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

import java.util.List;

/**
 * Port of the 1.12.2 {@code Configuration} options to the modern ForgeConfigSpec system.
 * The config file is written to {@code config/menumobs-client.toml}.
 */
public final class MenuMobsConfig {

    public static final ForgeConfigSpec SPEC;
    public static final BooleanValue SHOW_MAIN_MENU_MOBS;
    public static final BooleanValue SHOW_ONLY_PLAYER_MODELS;
    public static final DoubleValue MOB_SOUND_VOLUME;
    public static final ConfigValue<List<? extends String>> FIXED_MOB;
    public static final ConfigValue<List<? extends String>> BLACKLIST;
    public static final BooleanValue ALLOW_DEBUG_OUTPUT;
    public static final BooleanValue SHOW_MENU_BUTTONS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "Menu Mobs client configuration.",
                "Settings can be edited here or through the mods list (Mods -> Menu Mobs -> Config).")
                .push("general");

        SHOW_MAIN_MENU_MOBS = builder
                .comment("Set to true to show your logged-in player and a random mob on the main menu, false to disable.")
                .define("showMainMenuMobs", true);

        SHOW_ONLY_PLAYER_MODELS = builder
                .comment("If true only random player models are shown on the main menu (no mobs).")
                .define("showOnlyPlayerModels", false);

        MOB_SOUND_VOLUME = builder
                .comment("Volume (0.0 - 1.0) used for the ambient sounds the shown mobs make on the main menu.")
                .defineInRange("mobSoundVolume", 0.5D, 0.0D, 1.0D);

        FIXED_MOB = builder
                .comment("If non-empty, only the listed entities are shown on the main menu.",
                         "Entries may be entity registry ids (e.g. 'minecraft:creeper') or player names.",
                         "Leave empty to use random mobs.")
                .defineList("fixedMob", List.of(), o -> o instanceof String);

        BLACKLIST = builder
                .comment("Entity registry ids that will never be shown on the main menu.",
                         "This is added to the internal blacklist of known problematic mobs.")
                .defineList("blacklist", List.of(), o -> o instanceof String);

        ALLOW_DEBUG_OUTPUT = builder
                .comment("Enable additional debug output about the entity currently shown on the main menu.")
                .define("allowDebugOutput", false);

        SHOW_MENU_BUTTONS = builder
                .comment("Show the small 'add to blacklist' and 'next entity' buttons in the top left corner of the main menu.")
                .define("showMenuButtons", true);

        builder.pop();

        SPEC = builder.build();
    }

    private MenuMobsConfig() {
    }

    public static boolean showMainMenuMobs() {
        return SHOW_MAIN_MENU_MOBS.get();
    }

    public static boolean showOnlyPlayerModels() {
        return SHOW_ONLY_PLAYER_MODELS.get();
    }

    public static float mobSoundVolume() {
        return MOB_SOUND_VOLUME.get().floatValue();
    }

    public static String[] fixedMob() {
        return FIXED_MOB.get().toArray(new String[0]);
    }

    public static String[] blacklist() {
        return BLACKLIST.get().toArray(new String[0]);
    }

    public static boolean allowDebugOutput() {
        return ALLOW_DEBUG_OUTPUT.get();
    }

    public static boolean showMenuButtons() {
        return SHOW_MENU_BUTTONS.get();
    }
}
