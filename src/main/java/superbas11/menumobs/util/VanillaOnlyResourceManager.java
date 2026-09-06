package superbas11.menumobs.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A {@link ResourceManager} that only exposes resources from the {@code minecraft}
 * (vanilla) namespace.
 *
 * <p>The fake-level data registries are loaded to construct a {@code ClientLevel} on the
 * title screen. We deliberately ignore every mod-provided datapack entry: a single broken
 * JSON in any other mod would otherwise abort the whole registry load (and thereby
 * disable the menu mobs). The vanilla data pack is always self-consistent.
 */
public class VanillaOnlyResourceManager implements ResourceManager {

    private final ResourceManager delegate;

    public VanillaOnlyResourceManager(ResourceManager delegate) {
        this.delegate = delegate;
    }

    private static boolean isVanilla(ResourceLocation location) {
        return location != null && location.getNamespace().equals("minecraft");
    }

    @Override
    public Set<String> getNamespaces() {
        return Set.of("minecraft");
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        return isVanilla(location) ? this.delegate.getResource(location) : Optional.empty();
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation location) {
        return isVanilla(location) ? this.delegate.getResourceStack(location) : List.of();
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
        return this.delegate.listResources(path, location -> isVanilla(location) && filter.test(location));
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
        return this.delegate.listResourceStacks(path, location -> isVanilla(location) && filter.test(location));
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.delegate.listPacks();
    }
}
