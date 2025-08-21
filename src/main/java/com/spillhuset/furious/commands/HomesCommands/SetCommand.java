package com.spillhuset.furious.commands.HomesCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class SetCommand implements SubCommandInterface {
    private final Furious plugin;

    public SetCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        UUID uuid;
        String name;

        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use /homes set.");
            return true;
        }

        if (args.length == 3 && can(sender, true, true)) {
            // /homes set <player> <name>
            OfflinePlayer target = Utility.findPlayer(args[1], player.getUniqueId());
            if (target == null) {
                Components.sendError(sender, Components.t("Player not found: "), Components.playerComp(args[1]));
                return true;
            }
            uuid = target.getUniqueId();
            name = args[2];
            plugin.homesService.setHome(player, uuid, name);
            return true;
        } else if (args.length == 2 && can(sender, true, false)) {
            // /homes set <name>
            uuid = player.getUniqueId();
            name = args[1];
            plugin.homesService.setHome(player, uuid, name);
            return true;
        }
        Components.sendInfoMessage(sender, "Usage: /homes set " + (can(sender, false, true) ? "[player]" : "") + " <name>");
        return true;
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getPermission() {
        return "furious.homes.set";
    }
}
