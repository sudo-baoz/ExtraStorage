package me.hsgamer.extrastorage.hooks.island;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for island plugin integration.
 * Provides methods to check island membership, roles, and permissions.
 */
public interface IslandProvider {

    /**
     * Check if the island plugin is hooked and available.
     */
    boolean isHooked();

    /**
     * Get the island UUID for a player.
     * @param playerUuid the player's UUID
     * @return optional island UUID if the player is on an island
     */
    Optional<UUID> getIslandUUID(UUID playerUuid);

    /**
     * Get the owner UUID of an island.
     * @param islandUUID the island UUID
     * @return optional owner UUID
     */
    Optional<UUID> getIslandOwner(UUID islandUUID);

    /**
     * Check if the player is allowed to open the island storage.
     * Typically requires owner or co-owner role.
     * @param player the player
     * @return true if allowed
     */
    boolean isPlayerAllowedToOpenStorage(Player player);

    /**
     * Check if the player is allowed to withdraw items from the storage.
     * @param player the player
     * @param targetPlayer the player whose storage is being accessed (null = self)
     * @return true if allowed
     */
    boolean isPlayerAllowedToWithdraw(Player player, UUID targetPlayer);

    /**
     * Check if the player is allowed to deposit items into the storage.
     * All island members are typically allowed.
     * @param player the player
     * @return true if allowed
     */
    boolean isPlayerAllowedToDeposit(Player player);

    /**
     * Check if the player is allowed to upgrade the island storage.
     * Typically requires owner or co-owner role.
     * @param player the player
     * @return true if allowed
     */
    boolean isPlayerAllowedToUpgrade(Player player);

    /**
     * Check if the player (who must be island owner) can grant withdraw permission.
     * @param player the player attempting to grant permission
     * @return true if the player is island owner
     */
    boolean canGrantWithdrawPermission(Player player);

    /**
     * Get all member UUIDs in an island (excludes coop players).
     * @param islandUUID the island UUID
     * @return collection of member UUIDs (including owner)
     */
    Collection<UUID> getIslandMembers(UUID islandUUID);

    /**
     * Grant withdraw permission for a player in the island.
     * @param islandUUID the island UUID
     * @param targetUUID the player to grant permission to
     */
    void grantWithdrawPermission(UUID islandUUID, UUID targetUUID);

    /**
     * Revoke withdraw permission for a player in the island.
     * @param islandUUID the island UUID
     * @param targetUUID the player to revoke permission from
     */
    void revokeWithdrawPermission(UUID islandUUID, UUID targetUUID);

    /**
     * Check if a player has withdraw permission in the island.
     * @param islandUUID the island UUID
     * @param playerUUID the player UUID
     * @return true if the player has permission to withdraw
     */
    boolean hasWithdrawPermission(UUID islandUUID, UUID playerUUID);
}
