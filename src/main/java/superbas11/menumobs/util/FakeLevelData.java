package superbas11.menumobs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

import java.util.List;

/**
 * Loads the data-driven registries (dimension types, biomes, damage types, ...) from the
 * vanilla data pack so that a {@code ClientLevel} can be constructed on the title screen,
 * where normally no world / server registries are available.
 *
 * <p>This mirrors what the vanilla server would send during login (the {@code REMOTE}
 * registry layer). The result is cached for the remainder of the session.
 */
public final class FakeLevelData {

    private static RegistryAccess.Frozen cached;
    private static Throwable lastError;

    private FakeLevelData() {
    }

    /**
     * @return the data registry access, or throws if the data pack could not be loaded.
     */
    public static synchronized RegistryAccess.Frozen getRegistryAccess() {
        if (cached != null) {
            return cached;
        }
        if (lastError != null) {
            throw new IllegalStateException("Menu Mobs: could not load the data registries for the fake level.", lastError);
        }
        try {
            cached = build();
        } catch (Throwable t) {
            lastError = t;
            throw new IllegalStateException("Menu Mobs: could not load the data registries for the fake level.", t);
        }
        return cached;
    }

    private static RegistryAccess.Frozen build() {
        Minecraft mc = Minecraft.getInstance();
        List<PackResources> packs = mc.getResourcePackRepository().getSelectedPacks().stream()
                .map(pack -> pack.open())
                .toList();
        try (MultiPackResourceManager manager = new MultiPackResourceManager(PackType.SERVER_DATA, packs)) {
            // Only load the vanilla data: mod datapacks may contain broken entries that
            // would poison the whole registry load in larger packs.
            VanillaOnlyResourceManager vanilla = new VanillaOnlyResourceManager(manager);
            return RegistryDataLoader.load(
                    vanilla,
                    RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
                    RegistryDataLoader.WORLDGEN_REGISTRIES);
        }
    }
}
