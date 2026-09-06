package superbas11.menumobs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import superbas11.menumobs.MenuMobsConfig;

/**
 * Plays ambient sounds for the menu entities directly through the sound manager.
 * The volume is taken from the mod config and the sounds use the {@code MASTER}
 * category (like the original 1.12.2 mod).
 */
public final class MenuMobSounds {

    private MenuMobSounds() {
    }

    public static void playMenuSound(SoundEvent sound, float volume, float pitch) {
        float configVolume = MenuMobsConfig.mobSoundVolume();
        if (configVolume <= 0.0F || sound == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SimpleSoundInstance instance = new SimpleSoundInstance(
                sound.getLocation(),
                SoundSource.MASTER,
                configVolume,
                pitch,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D, 0.0D, 0.0D,
                true);
        mc.getSoundManager().play(instance);
    }
}
