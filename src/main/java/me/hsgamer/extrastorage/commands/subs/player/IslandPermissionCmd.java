package me.hsgamer.extrastorage.commands.subs.player;

import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.hooks.island.IslandProvider;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@Command(value = {"permission", "perm"}, usage = "/{label} permission <grant|revoke> <player>", permission = Constants.PLAYER_ISLAND_PERMISSION_PERMISSION, target = CommandTarget.ONLY_PLAYER, minArgs = 2)
public final class IslandPermissionCmd extends CommandListener {

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();
        Setting setting = instance.getSetting();
        IslandProvider islandProvider = setting.getIslandProvider();

        if (!setting.isIslandEnabled() || !islandProvider.isHooked()) {
            context.sendMessage(Message.getMessage("FAIL.island-not-member"));
            return;
        }

        Optional<UUID> islandUUIDOptional = islandProvider.getIslandUUID(player.getUniqueId());
        if (!islandUUIDOptional.isPresent()) {
            context.sendMessage(Message.getMessage("FAIL.island-not-member"));
            return;
        }

        if (!islandProvider.canGrantWithdrawPermission(player)) {
            context.sendMessage(Message.getMessage("FAIL.island-not-owner"));
            return;
        }

        String action = context.getArgs(0).toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(context.getArgs(1));
        UUID targetUUID = target.getUniqueId();

        if (!islandProvider.getIslandMembers(islandUUIDOptional.get()).contains(targetUUID)) {
            context.sendMessage(Message.getMessage("FAIL.player-not-in-island").replaceAll(Utils.getRegex("player"), context.getArgs(1)));
            return;
        }

        String targetName = target.getName() == null ? context.getArgs(1) : target.getName();
        switch (action) {
            case "grant":
            case "allow":
            case "add":
                islandProvider.grantWithdrawPermission(islandUUIDOptional.get(), targetUUID);
                context.sendMessage(Message.getMessage("SUCCESS.island-withdraw-granted").replaceAll(Utils.getRegex("player"), targetName));
                break;
            case "revoke":
            case "deny":
            case "remove":
                islandProvider.revokeWithdrawPermission(islandUUIDOptional.get(), targetUUID);
                context.sendMessage(Message.getMessage("SUCCESS.island-withdraw-revoked").replaceAll(Utils.getRegex("player"), targetName));
                break;
            default:
                context.sendMessage(Message.getMessage("FAIL.invalid-permission-action"));
                break;
        }
    }
}