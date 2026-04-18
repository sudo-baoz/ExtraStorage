package me.hsgamer.extrastorage.data.island;

import me.hsgamer.extrastorage.api.storage.Storage;
import me.hsgamer.extrastorage.api.user.Partner;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.topper.data.core.DataEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User wrapper for island-level storage.
 * All island members share the same storage via the island UUID.
 */
public class IslandUser implements User {
    private final UUID islandUUID;
    private final DataEntry<UUID, me.hsgamer.extrastorage.data.user.UserImpl> islandEntry;

    public IslandUser(UUID islandUUID, DataEntry<UUID, me.hsgamer.extrastorage.data.user.UserImpl> islandEntry) {
        this.islandUUID = islandUUID;
        this.islandEntry = islandEntry;
    }

    @Override
    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(islandUUID);
    }

    @Override
    public Player getPlayer() {
        return Bukkit.getPlayer(islandUUID);
    }

    @Override
    public UUID getUUID() {
        return islandUUID;
    }

    @Override
    public String getName() {
        return islandUUID.toString();
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getPlayer(islandUUID) != null;
    }

    @Override
    public void save() {
        ((IslandStorageManager) islandEntry.getHolder()).save();
    }

    @Override
    public boolean hasPermission(String permission) {
        return false;
    }

    @Override
    public String getTexture() {
        return islandEntry.getValue().texture;
    }

    @Override
    public Storage getStorage() {
        return new IslandStorage(islandEntry);
    }

    @Override
    public Collection<Partner> getPartners() {
        return Collections.emptyList();
    }

    @Override
    public boolean isPartner(UUID player) {
        return false;
    }

    @Override
    public void addPartner(UUID player) {
        // No-op for island storage
    }

    @Override
    public void removePartner(UUID player) {
        // No-op for island storage
    }

    @Override
    public void clearPartners() {
        // No-op for island storage
    }
}
