package superbas11.menumobs.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;

/**
 * A {@link ClientPacketListener} that provides the {@linkplain FakeLevelData data registries}
 * instead of waiting for a server login packet. This is only ever used to host the fake
 * {@code ClientLevel} for the main-menu rendering; no real network traffic happens.
 */
public class FakeClientPacketListener extends ClientPacketListener {

    private final LayeredRegistryAccess<ClientRegistryLayer> registryAccess;

    public FakeClientPacketListener(GameProfile profile) {
        super(Minecraft.getInstance(),
                null,
                new Connection(PacketFlow.CLIENTBOUND),
                null,
                profile,
                Minecraft.getInstance().getTelemetryManager().createWorldSessionManager(false, null, null));
        this.registryAccess = ClientRegistryLayer.createRegistryAccess()
                .replaceFrom(ClientRegistryLayer.REMOTE, FakeLevelData.getRegistryAccess());
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.registryAccess.compositeAccess();
    }
}
