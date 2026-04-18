package me.hsgamer.extrastorage.hooks.island;

import org.bukkit.entity.Player;

import java.util.*;

/**
 * Default island provider when no island plugin is available.
 * All methods return safe default values (no restrictions).
 */
public class NoneIslandHook implements IslandProvider {

    @Override
    public boolean isHooked() {
        return false;
    }

    @Override
    public Optional<UUID> getIslandUUID(UUID playerUuid) {
        return Optional.empty();
    }

    @Override
    public Optional<UUID> getIslandOwner(UUID islandUUID) {
        return Optional.empty();
    }

    @Override
    public boolean isPlayerAllowedToOpenStorage(Player player) {
        // No island plugin: allow everyone (normal permission check)
        return true;
    }

    @Override
    public boolean isPlayerAllowedToWithdraw(Player player, UUID targetPlayer) {
        // No island plugin: allow everyone
        return true;
    }

    @Override
    public boolean isPlayerAllowedToDeposit(Player player) {
        // No island plugin: allow everyone
        return true;
    }

    @Override
    public boolean isPlayerAllowedToUpgrade(Player player) {
        // No island plugin: allow everyone
        return true;
    }

    @Override
    public boolean canGrantWithdrawPermission(Player player) {
        // No island plugin: no owner concept, deny
        return false;
    }

    @Override
    public Collection<UUID> getIslandMembers(UUID islandUUID) {
        return Collections.emptyList();
    }

    @Override
    public void grantWithdrawPermission(UUID islandUUID, UUID targetUUID) {
        // No-op
    }

    @Override
    public void revokeWithdrawPermission(UUID islandUUID, UUID targetUUID) {
        // No-op
    }

    @Override
    public boolean hasWithdrawPermission(UUID islandUUID, UUID playerUUID) {
        // No island plugin: allow everyone
        return true;
    }
}
