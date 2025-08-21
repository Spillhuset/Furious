package com.spillhuset.furious.commands.HomesCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RemoveCommand implements SubCommandInterface {
    private final Furious plugin;
    private final Map<UUID, String> pendingRemovals = new HashMap<>();

    public RemoveCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        // /homes remove <player>
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

        // /homes remove <player> <name>
        if (args.length == 3 && can(sender, false, true)) {
            OfflinePlayer offlinePlayer = Utility.findPlayer(args[1], (sender instanceof Player p) ? p.getUniqueId() : null);
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

        // /homes remove <name>
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

        // /homes remove <name> confirm
        if (args.length == 3 && can(sender, false)) {
            String prefix = args[2].toLowerCase();
            if ("confirm".startsWith(prefix)) {
                completions.add("confirm");
            }
            return completions;
        }

        // /homes remove <player> <name> confirm
        if (args.length == 4 && can(sender, false, true)) {
            String prefix = args[3].toLowerCase();
            if ("confirm".startsWith(prefix)) {
                completions.add("confirm");
            }
            return completions;
        }

        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        UUID uuid = null;
        String name = "default";
        boolean confirmed = (args.length == 4 && args[3].equalsIgnoreCase("confirm")) || (args.length == 3 && args[2].equalsIgnoreCase("confirm"));

        switch (args.length) {
            case 2: {
                name = args[1];
                break;
            }
            case 3: {
                name = (args[2].equalsIgnoreCase("confirm")) ? args[1] : args[2];
                break;
            }
            case 4: {
                name = args[2];
                break;
            }
            default:
                Components.sendInfoMessage(sender, "Usage: /homes remove " + (can(sender, false, true) ? "[player]" : "") + " <name>");
                return true;
        }

        if (((args.length == 3 && !args[2].equalsIgnoreCase("confirm")) || args.length == 4) && can(sender, true, true)) {
            // /homes remove <player> <name>
            // /homes remove <player> <name> confirm
            OfflinePlayer target = Utility.findPlayer(args[1], (sender instanceof Player player) ? player.getUniqueId() : null);
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }
            uuid = target.getUniqueId();
        } else if ((args.length == 2 || (args.length == 3 && args[2].equalsIgnoreCase("confirm"))) && can(sender, true)) {
            // /homes remove <name>
            // /homes remove <name> confirm
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use /homes remove.");
                return true;
            }
            uuid = player.getUniqueId();
        }

        String token = (sender instanceof Player player) ? player.getUniqueId() + ":" + name : "console:" + name;

        if (uuid != null) {
            if (confirmed) {
                String sample = pendingRemovals.get(uuid);
                if (sample == null || !sample.equals(token)) {
                    Components.sendErrorMessage(sender, "Invalid confirmation token.");
                    return true;
                }
                plugin.homesService.removeHome(sender, uuid, name);
                pendingRemovals.remove(uuid);
            } else {
                pendingRemovals.put(uuid, token);
                Components.sendInfo(sender, Components.t("Are you sure you want to remove the home: "), Components.valueComp(name), Components.t("? Type /homes remove " + (can(sender, false, true) ? "[player]" : "") + " " + name + " confirm"));
            }
        } else {
            // If we couldn't determine the target, show usage to avoid silent no-op
            Components.sendInfoMessage(sender, "Usage: /homes remove " + (can(sender, false, true) ? "[player] " : "") + "<name> [confirm]");
        }
        return true;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getPermission() {
        return "furious.homes.remove";
    }
}
