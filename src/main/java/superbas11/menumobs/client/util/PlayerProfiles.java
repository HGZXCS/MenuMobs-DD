package superbas11.menumobs.client.util;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resolves player profiles (with skin textures) for the players shown on the main menu.
 * All network work happens on a background daemon thread so that the menu never freezes;
 * until a profile is resolved the plain (default skin) profile is used.
 */
public final class PlayerProfiles {

    /** Hard-coded fallback player list (port of the original mod's list). */
    public record KnownPlayer(UUID uuid, String name) {
    }

    private static final List<KnownPlayer> KNOWN_PLAYERS = List.of(
            new KnownPlayer(UUID.fromString("41834a72-8902-4495-86b8-731b1c253afe"), "superbas11"),
            new KnownPlayer(UUID.fromString("92d45906-7a50-4742-85b6-b079db9dc189"), "bspkrs"),
            new KnownPlayer(UUID.fromString("2efa46fa-2948-4d98-b822-fa182d254870"), "lorddusk"),
            new KnownPlayer(UUID.fromString("b9a89002-b392-4545-ab4d-5b1ff60c88a6"), "Arkember"),
            new KnownPlayer(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch"),
            new KnownPlayer(UUID.fromString("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6"), "Dinnerbone"),
            new KnownPlayer(UUID.fromString("853c80ef-3c37-49fd-aa76-92f73ff1f4c1"), "jeb_"),
            new KnownPlayer(UUID.fromString("bbb87dbe-690f-4205-bdc5-72ffb8ebc29d"), "direwolf20"),
            new KnownPlayer(UUID.fromString("90201c89-5712-4f99-a1c9-5d751565560a"), "Aureylian"),
            new KnownPlayer(UUID.fromString("e6b5c088-0680-44df-9e1b-9bf11792291b"), "Grumm"),
            new KnownPlayer(UUID.fromString("e358f277-4a4c-42f6-9ad8-3addf4869ea6"), "Adubbz"),
            new KnownPlayer(UUID.fromString("c7d5d58a-51a8-4d26-98f5-5550a37bd8d1"), "jadedcat"));

    private static final Map<UUID, GameProfile> RESOLVED = new ConcurrentHashMap<>();
    private static final Map<String, UUID> KNOWN_UUIDS = new LinkedHashMap<>();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MenuMobs-ProfileFetcher");
        thread.setDaemon(true);
        return thread;
    });

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    static {
        for (KnownPlayer known : KNOWN_PLAYERS) {
            KNOWN_UUIDS.put(known.name(), known.uuid());
            RESOLVED.putIfAbsent(known.uuid(), new GameProfile(known.uuid(), known.name()));
        }
    }

    private PlayerProfiles() {
    }

    /**
     * Kicks off the (asynchronous) fetch of the logged-in player's textures as early as
     * possible so the first main-menu visit usually already has the skin cached.
     */
    public static void warmStart() {
        try {
            localProfile();
        } catch (Throwable ignored) {
            // not booted far enough yet - the next menu visit will trigger the fetch
        }
    }

    public static List<KnownPlayer> knownPlayers() {
        return KNOWN_PLAYERS;
    }

    /**
     * @return a known random player entry.
     */
    public static KnownPlayer randomKnownPlayer(java.util.Random random) {
        return KNOWN_PLAYERS.get(random.nextInt(KNOWN_PLAYERS.size()));
    }

    /**
     * @return a profile (default-skin until textures are fetched in the background) for the
     * given player. Textures are fetched asynchronously and cached.
     */
    public static GameProfile profile(UUID uuid, String name) {
        GameProfile base = RESOLVED.computeIfAbsent(uuid, u -> new GameProfile(u, name));
        if (base.getProperties().isEmpty()) {
            EXECUTOR.submit(() -> fetchAndCache(base));
        }
        return base;
    }

    /**
     * @return the currently logged in player's profile (cached; textures fetched asynchronously).
     */
    public static GameProfile localProfile() {
        GameProfile base = Minecraft.getInstance().getUser().getGameProfile();
        GameProfile cached = RESOLVED.computeIfAbsent(base.getId(), u -> new GameProfile(u, base.getName()));
        if (cached.getProperties().isEmpty() && !base.getProperties().isEmpty()) {
            cached.getProperties().putAll(base.getProperties());
        }
        if (cached.getProperties().isEmpty()) {
            EXECUTOR.submit(() -> fetchAndCache(base));
        }
        return cached;
    }

    /**
     * Looks up the UUID for a player name, using the hard-coded list first and Mojang's
     * API (asynchronously) as a fallback. Blocks only on the known list.
     */
    public static Optional<UUID> uuidOf(String name) {
        UUID known = KNOWN_UUIDS.get(name);
        if (known != null) {
            return Optional.of(known);
        }
        EXECUTOR.submit(() -> fetchUuid(name));
        return Optional.empty();
    }

    private static void fetchAndCache(GameProfile profile) {
        try {
            GameProfile filled = Minecraft.getInstance().getMinecraftSessionService()
                    .fillProfileProperties(profile, true);
            if (filled != null) {
                RESOLVED.put(profile.getId(), filled);
            }
        } catch (Exception ignored) {
            // Offline / network failure: keep the default skin profile.
        }
    }

    private static void fetchUuid(String name) {
        try {
            String body = "[\"" + name + "\"]";
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/profiles/minecraft"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String json = response.body();
                int idx = json.indexOf("\"id\":\"");
                if (idx >= 0) {
                    int start = idx + "\"id\":\"".length();
                    int end = json.indexOf('"', start);
                    if (end > start) {
                        UUID uuid = UUIDTypeAdapter.fromString(json.substring(start, end));
                        KNOWN_UUIDS.putIfAbsent(name, uuid);
                        RESOLVED.putIfAbsent(uuid, new GameProfile(uuid, name));
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // network failure - ignored
        }
    }
}
