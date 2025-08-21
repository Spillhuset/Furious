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

public class MoveCommand implements SubCommandInterface {
    private final Furious plugin;
    public MoveCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        // /homes move <player> <name>
        if (args.length == 3 && can(sender, false, true)) {
            OfflinePlayer offlinePlayer = Utility.findPlayer(args[1], (sender instanceof Player p) ? p.getUniqueId() : null);
            if (offlinePlayer == null) {
                return completions;
            }
            UUID target = offlinePlayer.getUniqueId();
            List<String> homes = plugin.homesService.getHomesNames(target);
            String prefix = args[2].toLowerCase();
            for (String home : homes) {
                if (home.toLowerCase().startsWith(prefix)) {
                    completions.add(home);
                }
            }
            return completions;
        }

        // /homes move <player>
        if (args.length == 2 && can(sender, false, true)) {
            String prefix = args[1].toLowerCase();
            // Suggest online players first
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                String name = p.getName();
                if (name != null && name.toLowerCase().startsWith(prefix)) {
                    completions.add(name);
                }
            });
            // Add offline players that match and aren't already included
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

        // /homes move <name>
        if (args.length == 2 && can(sender, false) && sender instanceof Player player) {
            UUID target = player.getUniqueId();
            List<String> homes = plugin.homesService.getHomesNames(target);
            String prefix = args[1].toLowerCase();
            for (String home : homes) {
                if (home.toLowerCase().startsWith(prefix)) {
                    completions.add(home);
                }
            }
        }
        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        UUID uuid;
        String name;

        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use /homes move.");
            return true;
        }

        if (args.length == 3 && can(sender, true, true)) {
            // /homes move <player> <name>
            OfflinePlayer target = Utility.findPlayer(args[1], player.getUniqueId());
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }
            uuid = target.getUniqueId();
            name = args[2];

            plugin.homesService.moveHome(player,uuid,name);
            return true;
        } else if (args.length == 2 && can(sender,true, false)){
            uuid = player.getUniqueId();
            name = args[1];

            plugin.homesService.moveHome(player,uuid,name);
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /homes move " + (can(sender, false, true) ? "[player]" : "") + " <name>");
        return true;
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public String getPermission() {
        return "furious.homes.move";
    }
}
