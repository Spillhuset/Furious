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

public class ListCommand implements SubCommandInterface {
    private final Furious plugin;
    public ListCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // /homes list [player]
        if (args.length == 2 && can(sender, false, true)) {
            String prefix = args[1].toLowerCase();
            List<String> out = new ArrayList<>();
            // Suggest online players first
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                p.getName();
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    out.add(p.getName());
                }
            });
            // Add offline players that match and aren't already included
            for (OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
                String name = p.getName();
                if (name != null) {
                    String lower = name.toLowerCase();
                    if (lower.startsWith(prefix) && !out.contains(name)) {
                        out.add(name);
                    }
                }
            }
            return out;
        }
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 2 && can(sender, true, true)) {
            OfflinePlayer target = Utility.findPlayer(args[1], (sender instanceof Player player) ? player.getUniqueId() : null);
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }
            plugin.homesService.listHomes(sender, target);
            return true;
        } else if (args.length == 1 && can(sender, true)) {
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use /homes list.");
                return true;
            }
            plugin.homesService.listHomes(sender, player);
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /homes list " + (can(sender, false, true) ? "[player]" : ""));
        return true;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getPermission() {
        return "furious.homes.list";
    }
}
