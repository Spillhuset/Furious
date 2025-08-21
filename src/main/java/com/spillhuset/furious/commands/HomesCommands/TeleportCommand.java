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

public class TeleportCommand implements SubCommandInterface {
    private final Furious plugin;

    public TeleportCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 3 && can(sender, false, true)) {
            // /homes teleport <player> <name>
            OfflinePlayer offlinePlayer = Utility.findPlayer(args[1], (sender instanceof Player p) ? p.getUniqueId() : null);
            if (offlinePlayer == null) {
                return completions;
            }
            UUID target = offlinePlayer.getUniqueId();

            List<String> homes = plugin.homesService.getHomesNames(target);
            for (String home : homes) {
                if (home.toLowerCase().startsWith(args[2].toLowerCase())) { // FIX: args[2] here
                    completions.add(home);
                }
            }
            return completions;
        }
        if (args.length == 2 && can(sender, false, true)) {
            // /homes teleport <player>
            for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
                if (player.getName() != null && player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
            return completions;
        }

        if (args.length == 2 && can(sender, false) && sender instanceof Player player) { // FIX: guard instanceof
            // /homes teleport <name>
            UUID target = player.getUniqueId();
            List<String> homes = plugin.homesService.getHomesNames(target);
            for (String home : homes) {
                if (home.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(home);
                }
            }
        }
        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use /homes teleport.");
            return true;
        }

        UUID uuid = null;
        String name = "default";

        if (args.length == 3 && can(sender, true, true)) {
            // /homes teleport <player> <name>
            OfflinePlayer target = Utility.findPlayer(args[1], player.getUniqueId());
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }
            uuid = target.getUniqueId();
            name = args[2];
        } else if (args.length == 2 && can(sender, true)) {
            // /homes teleport <name>
            uuid = player.getUniqueId();
            name = args[1];
        } else if (args.length == 1 && can(sender, true)) {
            // /homes teleport
            uuid = player.getUniqueId();
        }
        if (uuid != null) {
            plugin.homesService.teleportHome(player, uuid, name);
        } else {
            if (can(sender, false, true)) {
                Components.sendInfoMessage(sender, "Usage: /homes teleport <player> <name>");
                Components.sendInfoMessage(sender, "       /homes teleport [name]  (defaults to \"default\")");
            } else {
                Components.sendInfoMessage(sender, "Usage: /homes teleport [name]  (defaults to \"default\")");
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String getPermission() {
        return "furious.homes.teleport";
    }
}
