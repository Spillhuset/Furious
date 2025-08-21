package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /guild homes <list|set|move|remove|rename|teleport>
 */
public class HomesCommand implements SubCommandInterface {
    private final Furious plugin;

    private static final List<String> ACTIONS = Arrays.asList(
            "list", "set", "move", "remove", "rename", "teleport"
    );

    public HomesCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (!can(sender, false)) return out;
        // args[0] == "homes"
        if (args.length == 2) {
            String pref = args[1].toLowerCase();
            for (String a : ACTIONS) if (a.startsWith(pref)) out.add(a);
        } else if (args.length == 3) {
            String action = args[1].toLowerCase();
            if (sender instanceof Player player) {
                UUID gid = plugin.guildService.getGuildIdForMember(player.getUniqueId());
                if (gid != null) {
                    switch (action) {
                        case "move", "remove", "rename", "teleport" -> {
                            String pref = args[2].toLowerCase();
                            for (String name : plugin.guildHomesService.getHomesNames(gid)) {
                                if (name.toLowerCase().startsWith(pref)) out.add(name);
                            }
                        }
                        default -> {}
                    }
                }
            }
        } else if (args.length == 4 && "rename".equalsIgnoreCase(args[1])) {
            // rename <old> <new> ; no suggestions for new
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!can(sender, true)) return true;
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use guild homes.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /guild homes <list|set|move|remove|rename|teleport> [name|old new]");
            return true;
        }
        UUID gid = plugin.guildService.getGuildIdForMember(player.getUniqueId());
        if (gid == null) {
            Components.sendErrorMessage(sender, "You are not in a guild.");
            return true;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "list" -> {
                plugin.guildHomesService.listHomes(sender, gid);
                return true;
            }
            case "set" -> {
                if (args.length < 3) {
                    Components.sendInfoMessage(sender, "Usage: /guild homes set <name>");
                    return true;
                }
                plugin.guildHomesService.setHome(player, gid, args[2]);
                return true;
            }
            case "move" -> {
                if (args.length < 3) {
                    Components.sendInfoMessage(sender, "Usage: /guild homes move <name>");
                    return true;
                }
                plugin.guildHomesService.moveHome(player, gid, args[2]);
                return true;
            }
            case "remove" -> {
                if (args.length < 3) {
                    Components.sendInfoMessage(sender, "Usage: /guild homes remove <name>");
                    return true;
                }
                plugin.guildHomesService.removeHome(sender, gid, args[2]);
                return true;
            }
            case "rename" -> {
                if (args.length < 4) {
                    Components.sendInfoMessage(sender, "Usage: /guild homes rename <old> <new>");
                    return true;
                }
                plugin.guildHomesService.renameHome(sender, gid, args[2], args[3]);
                return true;
            }
            case "teleport", "tp" -> {
                if (args.length < 3) {
                    Components.sendInfoMessage(sender, "Usage: /guild homes teleport <name>");
                    return true;
                }
                plugin.guildHomesService.teleportHome(player, gid, args[2]);
                return true;
            }
            default -> {
                Components.sendInfoMessage(sender, "Usage: /guild homes <list|set|move|remove|rename|teleport>");
                return true;
            }
        }
    }

    @Override
    public String getName() {
        return "homes";
    }

    @Override
    public String getPermission() {
        return "furious.guild.homes";
    }
}
