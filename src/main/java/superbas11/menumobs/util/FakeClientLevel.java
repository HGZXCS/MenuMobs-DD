package superbas11.menumobs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;

import javax.annotation.Nullable;

/**
 * A minimal fake client world used to spawn and display entities on the main menu.
 * The world is deliberately empty: entities never collide, tick or receive block
 * updates here; it only exists so that {@code EntityType.create(level)} and entity
 * constructors have a valid level to live in.
 */
public class FakeClientLevel extends ClientLevel {

    public FakeClientLevel(ClientPacketListener listener) {
        super(listener,
                new ClientLevelData(Difficulty.NORMAL, false, false),
                Level.OVERWORLD,
                listener.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE)
                        .getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
                2,
                2,
                Minecraft.getInstance()::getProfiler,
                Minecraft.getInstance().levelRenderer,
                false,
                0L);
    }

    @Override
    public void playSeededSound(@Nullable Player pPlayer, double x, double y, double z,
                                Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
        MenuMobSounds.playMenuSound(sound.value(), volume, pitch);
    }

    @Override
    public void playSeededSound(@Nullable Player pPlayer, Entity entity,
                                Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
        MenuMobSounds.playMenuSound(sound.value(), volume, pitch);
    }

    @Override
    public void tickEntities() {
        // Entities on the menu are advanced manually by the ticker; never auto-tick.
    }
}
