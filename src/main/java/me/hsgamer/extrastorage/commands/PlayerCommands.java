package me.hsgamer.extrastorage.commands;

import me.hsgamer.extrastorage.api.item.Item;
import me.hsgamer.extrastorage.api.user.User;
import me.hsgamer.extrastorage.commands.abstraction.Command;
import me.hsgamer.extrastorage.commands.abstraction.CommandContext;
import me.hsgamer.extrastorage.commands.abstraction.CommandListener;
import me.hsgamer.extrastorage.commands.abstraction.CommandTarget;
import me.hsgamer.extrastorage.commands.subs.player.*;
import me.hsgamer.extrastorage.configs.Message;
import me.hsgamer.extrastorage.configs.Setting;
import me.hsgamer.extrastorage.data.Constants;
import me.hsgamer.extrastorage.gui.StorageGUI;
import me.hsgamer.extrastorage.hooks.island.IslandProvider;
import me.hsgamer.extrastorage.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(value = "extrastorage", permission = Constants.PLAYER_OPEN_PERMISSION, target = CommandTarget.ONLY_PLAYER)
public final class PlayerCommands
        extends CommandListener {

    public PlayerCommands() {
        this.add(new HelpCmd());
        this.add(new ToggleCmd());
        this.add(new FilterCmd());
        this.add(new PartnerCmd());
        this.add(new SellCmd());
        this.add(new WithdrawCmd());
        this.add(new IslandPermissionCmd());
    }

    @Override
    public void execute(CommandContext context) {
        Player player = context.castToPlayer();
        Setting setting = instance.getSetting();
        IslandProvider islandProvider = setting.getIslandProvider();

        if (context.getArgsLength() == 0) {
            if (setting.isIslandEnabled() && islandProvider.isHooked() && !islandProvider.getIslandUUID(player.getUniqueId()).isPresent()) {
                context.sendMessage(Message.getMessage("FAIL.island-not-member"));
                return;
            }
            new StorageGUI(player, null).open();
            return;
        }

        String args0 = context.getArgs(0);
        OfflinePlayer target = Bukkit.getServer().getOfflinePlayer(args0);
        User user = instance.getUserManager().getUser(target);
        if (user == null) {
            context.sendMessage(Message.getMessage("FAIL.player-not-found"));
            return;
        }

        if (target.getName().equals(player.getName())) {
            context.sendMessage(Message.getMessage("FAIL.not-yourself"));
            return;
        }

        if (!user.isPartner(player.getUniqueId())) {
            context.sendMessage(Message.getMessage("FAIL.player-not-partner").replaceAll(Utils.getRegex("player"), target.getName()));
            return;
        }

        new StorageGUI(player, user).open();
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;

        List<String> cmds = Arrays.asList(
                this.hasPermission(sender, Constants.PLAYER_HELP_PERMISSION) ? "help" : "",
                this.hasPermission(sender, Constants.PLAYER_TOGGLE_PERMISSION) ? "toggle" : "",
                this.hasPermission(sender, Constants.PLAYER_FILTER_PERMISSION) ? "filter" : "",
                this.hasPermission(sender, Constants.PLAYER_PARTNER_PERMISSION) ? "partner" : "",
                this.hasPermission(sender, Constants.PLAYER_SELL_PERMISSION) ? "sell" : "",
                this.hasPermission(sender, Constants.PLAYER_WITHDRAW_PERMISSION) ? "withdraw" : "",
                this.hasPermission(sender, Constants.PLAYER_ISLAND_PERMISSION_PERMISSION) ? "permission" : ""
        );

        String args0 = args[0].toLowerCase();
        if (args.length == 1) return cmds.stream().filter(cmd -> cmd.startsWith(args0)).collect(Collectors.toList());

        User user = instance.getUserManager().getUser(player);

        String args1 = args[1].toLowerCase();
        if (args.length == 2) {
            switch (args0) {
                case "partner":
                    return Stream.of("add", "remove", "clear")
                            .filter(cmd -> cmd.startsWith(args1))
                            .collect(Collectors.toList());
                case "withdraw":
                case "sell":
                    return user.getStorage()
                            .getItems()
                            .values()
                            .stream()
                            .filter(item -> (item.getKey().toLowerCase().startsWith(args1)) && (item.getQuantity() > 0))
                            .map(Item::getKey)
                            .collect(Collectors.toList());
                case "permission":
                    return Stream.of("grant", "revoke")
                            .filter(cmd -> cmd.startsWith(args1))
                            .collect(Collectors.toList());
                default:
                    return null;
            }
        }

        if (args.length == 3 && args0.equals("permission")) {
            if (!instance.getSetting().isIslandEnabled() || !instance.getSetting().getIslandProvider().isHooked()) {
                return null;
            }
            Optional<UUID> islandUUID = instance.getSetting().getIslandProvider().getIslandUUID(player.getUniqueId());
            if (!islandUUID.isPresent()) {
                return null;
            }
            String args2 = args[2].toLowerCase();
            return instance.getSetting().getIslandProvider().getIslandMembers(islandUUID.get())
                    .stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(OfflinePlayer::getName)
                    .filter(name -> name != null && name.toLowerCase().startsWith(args2))
                    .collect(Collectors.toList());
        }

        return null;
    }

}
