package me.hsgamer.extrastorage.data.island;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.hooks.island.IslandProvider;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class to resolve the appropriate User for a player.
 * If the player is on an island and island storage is enabled, returns island User.
 * Otherwise returns normal player User.
 */
public final class IslandAPI {

    private IslandAPI() {
    }

    /**
     * Get the appropriate User for a player.
     * If island storage is enabled and the player is on an island, returns island-level User.
     * Otherwise returns normal player-level User.
     */
    public static User getUser(UUID playerUuid) {
        ExtraStorage instance = ExtraStorage.getInstance();
        IslandProvider islandProvider = instance.getSetting().getIslandProvider();

        if (!instance.getSetting().isIslandEnabled() || !islandProvider.isHooked()) {
            return instance.getUserManager().getUser(playerUuid);
        }

        Optional<UUID> islandUUID = islandProvider.getIslandUUID(playerUuid);
        if (islandUUID.isEmpty()) {
            return instance.getUserManager().getUser(playerUuid);
        }

        IslandStorageManager islandManager = instance.getIslandStorageManager();
        if (islandManager == null) {
            return instance.getUserManager().getUser(playerUuid);
        }

        return new IslandUser(islandUUID.get(), islandManager.getIslandEntry(islandUUID.get()));
    }

    /**
     * Check if island storage is enabled and active for a player.
     */
    public static boolean hasIslandStorage(UUID playerUuid) {
        ExtraStorage instance = ExtraStorage.getInstance();
        IslandProvider islandProvider = instance.getSetting().getIslandProvider();

        if (!instance.getSetting().isIslandEnabled() || !islandProvider.isHooked()) {
            return false;
        }

        Optional<UUID> islandUUID = islandProvider.getIslandUUID(playerUuid);
        return islandUUID.isPresent() && instance.getIslandStorageManager() != null;
    }

    /**
     * Get island UUID for a player if available.
     */
    public static Optional<UUID> getIslandUUID(UUID playerUuid) {
        ExtraStorage instance = ExtraStorage.getInstance();
        if (!instance.getSetting().isIslandEnabled()) {
            return Optional.empty();
        }
        return instance.getSetting().getIslandProvider().getIslandUUID(playerUuid);
    }
}
