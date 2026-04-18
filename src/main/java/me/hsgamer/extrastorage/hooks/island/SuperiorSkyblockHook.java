package me.hsgamer.extrastorage.hooks.island;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.PlayerRole;
import com.bgsoftware.superiorskyblock.api.island.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.api.privilege.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.privilege.PrivilegeType;
import com.bgsoftware.superiorskyblock.SuperiorSkyblock;
import me.hsgamer.extrastorage.ExtraStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SuperiorSkyblock2 integration for ExtraStorage.
 * Provides island-based storage with role and permission checks.
 */
public class SuperiorSkyblockHook implements IslandProvider {

    private final SuperiorSkyblock plugin;
    private final boolean hooked;

    // Cached custom privileges (registered once)
    private static IslandPrivilege PRIVILEGE_WITHDRAW;
    private static IslandPrivilege PRIVILEGE_UPGRADE;

    public SuperiorSkyblockHook() {
        this.plugin = (SuperiorSkyblock) Bukkit.getServer().getPluginManager().getPlugin("SuperiorSkyblock2");
        this.hooked = (plugin != null);

        if (hooked) {
            ExtraStorage.getInstance().getLogger().info("Hooked into SuperiorSkyblock2.");
        }
    }

    /**
     * Register custom privileges. Call this in onLoad() of ExtraStorage.
     */
    public static void registerPrivileges() {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            return;
        }
        try {
            PRIVILEGE_WITHDRAW = IslandPrivilege.register("STORAGE_WITHDRAW", PrivilegeType.COMMAND);
            PRIVILEGE_UPGRADE = IslandPrivilege.register("STORAGE_UPGRADE", PrivilegeType.COMMAND);
        } catch (IllegalArgumentException ignored) {
            // Already registered
        }
    }

    @Override
    public boolean isHooked() {
        return hooked;
    }

    private Island getIsland(UUID playerUuid) {
        if (!hooked) return null;
        return SuperiorSkyblockAPI.getIslandByPlayer(playerUuid);
    }

    private Island getIslandByUUID(UUID islandUUID) {
        if (!hooked) return null;
        return SuperiorSkyblockAPI.getIsland(islandUUID);
    }

    private SuperiorPlayer getSuperiorPlayer(UUID playerUuid) {
        if (!hooked) return null;
        return SuperiorSkyblockAPI.getPlayer(playerUuid);
    }

    @Override
    public Optional<UUID> getIslandUUID(UUID playerUuid) {
        Island island = getIsland(playerUuid);
        if (island == null) return Optional.empty();
        return Optional.of(island.getUUID());
    }

    @Override
    public Optional<UUID> getIslandOwner(UUID islandUUID) {
        Island island = getIslandByUUID(islandUUID);
        if (island == null) return Optional.empty();
        SuperiorPlayer owner = island.getOwner();
        if (owner == null) return Optional.empty();
        return Optional.of(owner.getUniqueId());
    }

    @Override
    public boolean isPlayerAllowedToOpenStorage(Player player) {
        if (!hooked) return true;

        Island island = getIsland(player.getUniqueId());
        if (island == null) {
            // Not on an island - fall back to normal permission check
            return true;
        }

        PlayerRole defaultRole = SuperiorSkyblockAPI.getRoles().getDefaultRole();
        SuperiorPlayer superiorPlayer = getSuperiorPlayer(player.getUniqueId());
        if (superiorPlayer == null) return false;

        // Owner and Co-Owner (roles higher than default) can open storage
        return superiorPlayer.getPlayerRole().isHigherThan(defaultRole);
    }

    @Override
    public boolean isPlayerAllowedToWithdraw(Player player, UUID targetPlayer) {
        if (!hooked) return true;

        // Resolve the actual owner of the storage being accessed
        UUID resolvedPlayer = (targetPlayer != null) ? targetPlayer : player.getUniqueId();
        Island island = getIsland(resolvedPlayer);
        if (island == null) {
            // No island - normal storage, allow
            return true;
        }

        SuperiorPlayer superiorPlayer = getSuperiorPlayer(player.getUniqueId());
        if (superiorPlayer == null) return false;

        if (PRIVILEGE_WITHDRAW == null) {
            // Fallback: only owner and co-owner can withdraw
            PlayerRole defaultRole = SuperiorSkyblockAPI.getRoles().getDefaultRole();
            return superiorPlayer.getPlayerRole().isHigherThan(defaultRole);
        }

        return island.hasPermission(superiorPlayer, PRIVILEGE_WITHDRAW);
    }

    @Override
    public boolean isPlayerAllowedToDeposit(Player player) {
        if (!hooked) return true;

        Island island = getIsland(player.getUniqueId());
        if (island == null) {
            // No island - normal storage, allow
            return true;
        }

        // All island members (including coop) can deposit
        return island.isMember(getSuperiorPlayer(player.getUniqueId()))
                || island.isCoop(getSuperiorPlayer(player.getUniqueId()));
    }

    @Override
    public boolean isPlayerAllowedToUpgrade(Player player) {
        if (!hooked) return true;

        Island island = getIsland(player.getUniqueId());
        if (island == null) {
            // No island - normal storage, allow
            return true;
        }

        SuperiorPlayer superiorPlayer = getSuperiorPlayer(player.getUniqueId());
        if (superiorPlayer == null) return false;

        PlayerRole defaultRole = SuperiorSkyblockAPI.getRoles().getDefaultRole();
        return superiorPlayer.getPlayerRole().isHigherThan(defaultRole);
    }

    @Override
    public boolean canGrantWithdrawPermission(Player player) {
        if (!hooked) return false;

        Island island = getIsland(player.getUniqueId());
        if (island == null) return false;

        SuperiorPlayer owner = island.getOwner();
        if (owner == null) return false;

        // Only island owner can grant permissions
        return owner.getUniqueId().equals(player.getUniqueId());
    }

    @Override
    public Collection<UUID> getIslandMembers(UUID islandUUID) {
        if (!hooked) return Collections.emptyList();

        Island island = getIslandByUUID(islandUUID);
        if (island == null) return Collections.emptyList();

        return island.getIslandMembers(true).stream()
                .map(SuperiorPlayer::getUniqueId)
                .collect(Collectors.toList());
    }

    @Override
    public void grantWithdrawPermission(UUID islandUUID, UUID targetUUID) {
        if (!hooked || PRIVILEGE_WITHDRAW == null) return;

        Island island = getIslandByUUID(islandUUID);
        if (island == null) return;

        SuperiorPlayer target = getSuperiorPlayer(targetUUID);
        if (target == null) return;

        island.setPermission(target, PRIVILEGE_WITHDRAW, true);
    }

    @Override
    public void revokeWithdrawPermission(UUID islandUUID, UUID targetUUID) {
        if (!hooked || PRIVILEGE_WITHDRAW == null) return;

        Island island = getIslandByUUID(islandUUID);
        if (island == null) return;

        SuperiorPlayer target = getSuperiorPlayer(targetUUID);
        if (target == null) return;

        island.setPermission(target, PRIVILEGE_WITHDRAW, false);
    }

    @Override
    public boolean hasWithdrawPermission(UUID islandUUID, UUID playerUUID) {
        if (!hooked) return true;

        Island island = getIslandByUUID(islandUUID);
        if (island == null) return true;

        SuperiorPlayer player = getSuperiorPlayer(playerUUID);
        if (player == null) return false;

        if (PRIVILEGE_WITHDRAW == null) {
            // Fallback: owner and co-owner have permission
            PlayerRole defaultRole = SuperiorSkyblockAPI.getRoles().getDefaultRole();
            return player.getPlayerRole().isHigherThan(defaultRole);
        }

        return island.hasPermission(player, PRIVILEGE_WITHDRAW);
    }
}