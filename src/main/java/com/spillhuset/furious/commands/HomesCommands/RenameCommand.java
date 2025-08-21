package com.spillhuset.furious.commands.HomesCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RenameCommand implements SubCommandInterface {
    private final Furious plugin;

    public RenameCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        // Admin form: /homes rename <player> <oldName> <newName>
        if (can(sender, false, true)) {
            if (args.length == 2) {
                // Suggest player names for <player>
                String prefix = args[1].toLowerCase();
                plugin.getServer().getOnlinePlayers().forEach(p -> {
                    String name = p.getName();
                    if (name != null && name.toLowerCase().startsWith(prefix)) {
                        completions.add(name);
                    }
                });
                for (OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
                    String name = p.getName();
                    if (name != null) {
                        String lower = name.toLowerCase();
                        if (lower.startsWith(prefix) && !completions.contains(name)) {
                            completions.add(name);
                        }
                    }
                }
                return completions;
            }
            if (args.length == 3) {
                // Suggest <oldName> for the specified player
                OfflinePlayer offlinePlayer = Utility.findPlayer(args[1], (sender instanceof Player sp) ? sp.getUniqueId() : null);
                if (offlinePlayer == null) {
                    return completions;
                }
                UUID target = offlinePlayer.getUniqueId();
                String prefix = args[2].toLowerCase();
                List<String> homes = plugin.homesService.getHomesNames(target);
                for (String home : homes) {
                    if (home.toLowerCase().startsWith(prefix)) {
                        completions.add(home);
                    }
                }
                return completions;
            }
            // args.length == 4 -> <newName>, no sensible completions
        }

        // Player form: /homes rename <oldName> <newName>
        if (args.length == 2 && can(sender, false) && sender instanceof Player player) {
            UUID target = player.getUniqueId();
            String prefix = args[1].toLowerCase();
            List<String> homes = plugin.homesService.getHomesNames(target);
            for (String home : homes) {
                if (home.toLowerCase().startsWith(prefix)) {
                    completions.add(home);
                }
            }
            return completions;
        }

        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        UUID uuid;
        String oldName;
        String newName;

        if (args.length == 4 && can(sender, true, true)) {
            // /homes rename <player> <oldName> <newName>
            OfflinePlayer target = Utility.findPlayer(args[1], (sender instanceof Player player) ? player.getUniqueId() : null);
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }
            uuid = target.getUniqueId();
            oldName = args[2];
            newName = args[3];

            plugin.homesService.renameHome(sender, uuid, oldName, newName);
            return true;
        } else if (args.length == 3 && can(sender, true, false)) {
            // /homes rename <oldName> <newName>
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use /homes rename.");
                return true;
            }
            uuid = player.getUniqueId();
            oldName = args[1];
            newName = args[2];

            plugin.homesService.renameHome(sender, uuid, oldName, newName);
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /homes rename " + (can(sender, false, true) ? "[player]" : "") + " <oldName> <newName>");
        return true;
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getPermission() {
        return "furious.homes.rename";
    }
}
