package me.hsgamer.extrastorage.listeners;

import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.data.user.UserManager;
import me.hsgamer.extrastorage.hooks.island.IslandProvider;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class PlayerListener
        extends BaseListener {

    private final UserManager manager;

    public PlayerListener(ExtraStorage instance) {
        super(instance);
        this.manager = instance.getUserManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        manager.load(uuid);

        // Ensure island shared storage is created/loaded as soon as a member joins.
        if (instance.getSetting().isIslandEnabled() && instance.getIslandStorageManager() != null) {
            IslandProvider islandProvider = instance.getSetting().getIslandProvider();
            if (islandProvider.isHooked()) {
                islandProvider.getIslandUUID(uuid).ifPresent(islandUUID -> instance.getIslandStorageManager().getIslandEntry(islandUUID));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        manager.save(uuid);
    }

}
