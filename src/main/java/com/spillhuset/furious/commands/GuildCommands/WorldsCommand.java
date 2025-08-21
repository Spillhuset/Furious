package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public class WorldsCommand implements SubCommandInterface {
    private final Furious plugin;

    public WorldsCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!can(sender, true)) {
            return true;
        }
        if (args.length == 1) {
            plugin.guildService.listWorlds(sender);
            Components.sendInfoMessage(sender, "Usage: /guild worlds <world> <enable|disable>");
            return true;
        }
        if (args.length == 3) {
            World world = plugin.getServer().getWorld(args[1]);
            if (world == null) {
                Components.sendError(sender, Components.t("World not found: "), Components.valueComp(args[1]));
                return true;
            }

            String action = args[2].toLowerCase(Locale.ROOT);
            if (!action.equals("enable") && !action.equals("disable")) {
                Components.sendInfoMessage(sender, "Usage: /guild worlds <world> <enable|disable>");
                return true;
            }

            boolean enable = action.equals("enable");
            plugin.guildService.setWorldEnabled(sender, world.getUID(), enable);
            return true;
        }

        Components.sendInfoMessage(sender, "Usage: /guild worlds <world> <enable|disable>");
        return true;
    }

    @Override
    public String getName() {
        return "worlds";
    }

    @Override
    public String getPermission() {
        return "furious.guild.worlds";
    }
}
