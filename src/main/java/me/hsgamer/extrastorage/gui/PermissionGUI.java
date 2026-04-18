package me.hsgamer.extrastorage.gui;

import io.github.projectunified.craftux.common.Button;
import io.github.projectunified.craftux.common.Mask;
import io.github.projectunified.craftux.mask.HybridMask;
import me.hsgamer.extrastorage.ExtraStorage;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.gui.base.BaseGUI;
import me.hsgamer.extrastorage.gui.item.GUIItem;
import me.hsgamer.extrastorage.hooks.island.IslandProvider;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI for managing island storage withdraw permissions.
 * Only island owner can open this GUI.
 */
public class PermissionGUI extends BaseGUI<PermissionGUI.SortType> {
    private final UUID islandOwnerUUID;
    private final UUID islandUUID;
    private final List<UUID> memberUUIDs;

    public PermissionGUI(Player player, UUID islandUUID, UUID islandOwnerUUID, List<UUID> memberUUIDs) {
        super(player, ExtraStorage.getInstance().getStorageGuiConfig(), SortType.class);
        this.islandUUID = islandUUID;
        this.islandOwnerUUID = islandOwnerUUID;
        this.memberUUIDs = new ArrayList<>(memberUUIDs);
        setup();
    }

    @Override
    protected List<Button> getRepresentItems(ConfigurationSection section) {
        IslandProvider islandProvider = ExtraStorage.getInstance().getSetting().getIslandProvider();
        GUIItem representItem = GUIItem.get(section, null);

        return memberUUIDs.stream().map(memberUUID -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
            User memberUser = ExtraStorage.getInstance().getUserManager().getUser(memberUUID);
            boolean hasWithdrawPerm = islandProvider.hasWithdrawPermission(islandUUID, memberUUID);
            boolean isOwner = memberUUID.equals(islandOwnerUUID);

            ItemStack item = representItem.getItem(memberUser, s -> {
                String status = hasWithdrawPerm ? "&aCho phép rút" : "&cKhông được rút";
                String role = isOwner ? "&6Chủ đảo" : "&eThành viên";
                String memberName = offlinePlayer.getName() != null ? offlinePlayer.getName() : memberUUID.toString();
                return s.replaceAll(Utils.getRegex("member"), memberName)
                        .replaceAll(Utils.getRegex("status"), status)
                        .replaceAll(Utils.getRegex("role"), role);
            });

            return (Button) (uuid, actionItem) -> {
                actionItem.setItem(item);
                actionItem.setAction(InventoryClickEvent.class, event -> {
                    if (isOwner) {
                        // Cannot modify owner's permission
                        return;
                    }

                    boolean currentPerm = islandProvider.hasWithdrawPermission(islandUUID, memberUUID);
                    if (currentPerm) {
                        islandProvider.revokeWithdrawPermission(islandUUID, memberUUID);
                        player.sendMessage(Message.getMessage("FAIL.island-withdraw-revoked")
                                .replaceAll(Utils.getRegex("player"), offlinePlayer.getName() != null ? offlinePlayer.getName() : memberUUID.toString()));
                    } else {
                        islandProvider.grantWithdrawPermission(islandUUID, memberUUID);
                        player.sendMessage(Message.getMessage("SUCCESS.island-withdraw-granted")
                                .replaceAll(Utils.getRegex("player"), offlinePlayer.getName() != null ? offlinePlayer.getName() : memberUUID.toString()));
                    }

                    updateRepresentItems();
                    update();
                });
                return true;
            };
        }).collect(Collectors.toList());
    }

    @Override
    protected Mask getControlItems(ConfigurationSection section) {
        HybridMask mask = new HybridMask();

        addAboutButton(mask, Objects.requireNonNull(section.getConfigurationSection("About")), s -> {
            return s.replaceAll(Utils.getRegex("total(\\_|\\-)members"), String.valueOf(memberUUIDs.size()));
        }, null);

        addSwitchButton(mask, Objects.requireNonNull(section.getConfigurationSection("SwitchGui")), event -> {
            browseGUI(event.isLeftClick());
        });

        return mask;
    }

    public enum SortType {
        NAME
    }
}
