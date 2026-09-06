package superbas11.menumobs.client.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import superbas11.menumobs.util.FakeClientLevel;
import superbas11.menumobs.util.LogHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Helpers to create and draw the entities shown on the main menu.
 */
@OnlyIn(Dist.CLIENT)
public final class EntityUtils {

    /**
     * Entity types that are known to be problematic (broken rendering, huge models,
     * no useful idle animation, ...) when drawn on the main menu. This list is always
     * applied in addition to the user configurable blacklist.
     */
    public static final Set<String> BUILT_IN_BLACKLIST = new HashSet<>(Set.of(
            "minecraft:ender_dragon",
            "minecraft:giant",
            "minecraft:ghast",
            "minecraft:bat",
            "minecraft:squid",
            "minecraft:guardian",
            "minecraft:elder_guardian",
            "minecraft:wither",
            "minecraft:shulker",
            "minecraft:end_crystal"));

    private static final Item[] PLAYER_ITEMS = {
            Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.GOLDEN_SWORD,
            Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, Items.IRON_AXE
    };

    private static final Item[] ZOMBIE_ITEMS = {
            Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.GOLDEN_SWORD, Items.IRON_AXE
    };

    private static final Item[] SKELETON_ITEMS = {
            Items.BOW, Items.GOLDEN_SWORD, Items.BOW, Items.BOW, Items.BOW, Items.BOW
    };

    private static final Item[] HORSE_ARMORS = {
            Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR
    };

    private static final Block[] ENDERMAN_BLOCKS = {
            Blocks.GRASS, Blocks.TALL_GRASS, Blocks.DEAD_BUSH, Blocks.DANDELION,
            Blocks.POPPY, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM, Blocks.PUMPKIN,
            Blocks.MELON, Blocks.CACTUS, Blocks.SAND, Blocks.DIRT, Blocks.GRASS_BLOCK
    };

    /** AbstractHorse slot ids used through {@code AbstractHorse#getSlot(int)}. */
    private static final int HORSE_SADDLE_SLOT = 400;
    private static final int HORSE_ARMOR_SLOT = 401;

    private EntityUtils() {
    }

    /**
     * A client-side player that always reports the {@link PlayerInfo} we supply, so that a
     * real skin/cape can be shown without an active network connection to a server.
     */
    public static class MenuClientPlayer extends RemotePlayer {
        private final PlayerInfo playerInfo;

        public MenuClientPlayer(ClientLevel level, GameProfile profile, PlayerInfo playerInfo, int modelPartsMask) {
            super(level, profile);
            this.playerInfo = playerInfo;
            if (modelPartsMask > 0) {
                this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) modelPartsMask);
            }
        }

        @Override
        protected PlayerInfo getPlayerInfo() {
            return this.playerInfo;
        }
    }

    /**
     * Creates a client player for the main menu with the given profile (skin textures are
     * resolved asynchronously by {@link PlayerProfiles} when possible).
     */
    public static MenuClientPlayer createMenuPlayer(FakeClientLevel level, GameProfile profile) {
        PlayerInfo info = new PlayerInfo(profile, false);
        MenuClientPlayer player = new MenuClientPlayer(level, profile, info, modelPartsMask());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PLAYER_ITEMS[new Random().nextInt(PLAYER_ITEMS.length)]));
        return player;
    }

    private static int modelPartsMask() {
        net.minecraft.client.Options options = Minecraft.getInstance().options;
        int mask = 0;
        for (PlayerModelPart part : PlayerModelPart.values()) {
            if (options.isModelPartEnabled(part)) {
                mask |= part.getMask();
            }
        }
        return mask;
    }

    /**
     * Creates a random fallback player (from the hard-coded list of well known players).
     */
    public static MenuClientPlayer createRandomKnownPlayer(FakeClientLevel level, Random random) {
        PlayerProfiles.KnownPlayer known = PlayerProfiles.randomKnownPlayer(random);
        return createMenuPlayer(level, PlayerProfiles.profile(known.uuid(), known.name()));
    }

    /**
     * Creates the player shown for the locally logged in user.
     */
    public static MenuClientPlayer createLocalPlayer(FakeClientLevel level) {
        return createMenuPlayer(level, PlayerProfiles.localProfile());
    }

    /**
     * @return a random living entity that is not blacklisted, or {@code null} if none could
     * be spawned (e.g. because the fake world failed to initialise).
     */
    @Nullable
    public static LivingEntity spawnRandomLivingEntity(FakeClientLevel level, Set<String> blacklist) {
        IForgeRegistry<EntityType<?>> registry = ForgeRegistries.ENTITY_TYPES;
        List<EntityType<?>> candidates = new ArrayList<>();
        for (EntityType<?> type : registry) {
            ResourceLocation key = registry.getKey(type);
            if (key == null || blacklist.contains(key.toString())) {
                continue;
            }
            candidates.add(type);
        }
        if (candidates.isEmpty()) {
            return null;
        }

        Random random = new Random();
        int attempts = Math.min(candidates.size(), 40);
        for (int i = 0; i < attempts; i++) {
            EntityType<?> type = candidates.get(random.nextInt(candidates.size()));
            Entity entity;
            try {
                entity = type.create(level);
            } catch (Throwable ignored) {
                continue;
            }
            if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                randomizeEntity(living);
                randomizeHeldItem(living);
                return living;
            }
        }
        return null;
    }

    /**
     * Creates one of the fixed entities configured in {@code fixedMob}. Entries are entity
     * registry ids, or player names.
     */
    @Nullable
    public static LivingEntity createFixedEntity(FakeClientLevel level, String entry, Random random) {
        String id = entry.trim();
        if (id.isEmpty()) {
            return null;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(id));
        if (type != null) {
            Entity entity;
            try {
                entity = type.create(level);
            } catch (Throwable t) {
                entity = null;
            }
            if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                randomizeEntity(living);
                randomizeHeldItem(living);
                return living;
            }
        }

        // Not an entity registry id -> treat it as a player name.
        Optional<UUID> uuid = PlayerProfiles.uuidOf(id);
        if (uuid.isPresent()) {
            return createMenuPlayer(level, PlayerProfiles.profile(uuid.get(), id));
        }

        LogHelper.debug("Entity '" + id + "' is unknown, falling back to a random player.");
        return createRandomKnownPlayer(level, random);
    }

    /**
     * Applies the random appearance tweaks of the original mod (sheep colour, villager
     * profession, rabbit type, enderman carried block, horse colours/armour, chests...).
     */
    public static void randomizeEntity(LivingEntity entity) {
        Random random = new Random();
        try {
            if (entity instanceof Sheep sheep) {
                sheep.setColor(DyeColor.values()[random.nextInt(DyeColor.values().length)]);
            } else if (entity instanceof Villager villager) {
                pickRandomProfession(villager, random);
            } else if (entity instanceof Rabbit rabbit) {
                int id = random.nextInt(13);
                rabbit.setVariant(Rabbit.Variant.byId(id < 12 ? id / 2 : 99));
            } else if (entity instanceof EnderMan enderman) {
                Block block = ENDERMAN_BLOCKS[random.nextInt(ENDERMAN_BLOCKS.length)];
                enderman.setCarriedBlock(block.defaultBlockState());
            } else if (entity instanceof Horse horse) {
                horse.setVariant(Variant.byId(random.nextInt(7)));
                if (random.nextBoolean()) {
                    try {
                        horse.setTamed(true);
                        horse.getSlot(HORSE_SADDLE_SLOT).set(new ItemStack(Items.SADDLE));
                        horse.getSlot(HORSE_ARMOR_SLOT).set(new ItemStack(HORSE_ARMORS[random.nextInt(HORSE_ARMORS.length)]));
                    } catch (Throwable ignored) {
                        // Saddle/armor slots are optional eye candy - ignore failures.
                    }
                }
            } else if (entity instanceof AbstractChestedHorse chestedHorse) {
                chestedHorse.setChest(random.nextBoolean());
            }
        } catch (Throwable t) {
            LogHelper.warning("Could not fully randomise entity appearance: " + entity.getType(), t);
        }
    }

    private static void pickRandomProfession(Villager villager, Random random) {
        List<VillagerProfession> professions = new ArrayList<>();
        for (VillagerProfession profession : ForgeRegistries.VILLAGER_PROFESSIONS) {
            if (profession != VillagerProfession.NONE) {
                professions.add(profession);
            }
        }
        if (!professions.isEmpty()) {
            VillagerProfession profession = professions.get(random.nextInt(professions.size()));
            villager.setVillagerData(villager.getVillagerData().setProfession(profession));
        }
    }

    /**
     * Gives the entity a random held item, mirroring the original mod's behaviour.
     */
    public static void randomizeHeldItem(LivingEntity entity) {
        Random random = new Random();
        try {
            if (entity instanceof ZombifiedPiglin) {
                entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_SWORD));
            } else if (entity instanceof Zombie) {
                entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ZOMBIE_ITEMS[random.nextInt(ZOMBIE_ITEMS.length)]));
            } else if (entity instanceof WitherSkeleton) {
                entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_SWORD));
            } else if (entity instanceof Skeleton || entity instanceof Stray) {
                entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SKELETON_ITEMS[random.nextInt(SKELETON_ITEMS.length)]));
            } else if (entity instanceof AbstractSkeleton) {
                entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            }
        } catch (Throwable t) {
            LogHelper.warning("Could not set random held item on " + entity.getType(), t);
        }
    }

    /**
     * Draws a living entity at the given slot, following the mouse like the vanilla
     * inventory screen preview.
     *
     * @param size scale in pixels (see {@link #sizeForEntity})
     */
    public static void drawEntityOnScreen(GuiGraphics guiGraphics, int slotX, int slotY, int size,
                                          float mouseDX, float mouseDY, LivingEntity entity) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics, slotX, slotY, size, mouseDX, mouseDY, entity);
    }

    /**
     * @param pixelsPerBlock the desired rendered size of a 1 block tall entity
     * @return a size value that makes {@code entity} roughly match that scale
     */
    public static int sizeForEntity(LivingEntity entity, int pixelsPerBlock) {
        float height = Math.max(entity.getBbHeight(), 0.5F);
        return Math.max(6, Math.round(pixelsPerBlock / height));
    }
}
