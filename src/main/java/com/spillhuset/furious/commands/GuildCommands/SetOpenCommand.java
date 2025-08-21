package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SetOpenCommand implements SubCommandInterface {
    private final Furious plugin;

    public SetOpenCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        // Player admin: /guild setopen <true|false>
        // Admin others: /guild setopen <guildName> <true|false>
        boolean others = can(sender, false, true);
        if (others) {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                for (String name : plugin.guildService.getAllGuildNames()) {
                    if (name.toLowerCase().startsWith(prefix)) out.add(name);
                }
                return out;
            } else if (args.length == 3) {
                String prefix = args[2].toLowerCase();
                if ("true".startsWith(prefix)) out.add("true");
                if ("false".startsWith(prefix)) out.add("false");
                return out;
            }
        } else {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                if ("true".startsWith(prefix)) out.add("true");
                if ("false".startsWith(prefix)) out.add("false");
                return out;
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Admin others
        if ((args.length == 3) && can(sender, true, true)) {
            String guildName = args[1];
            String val = args[2];
            boolean open;
            if (val.equalsIgnoreCase("true")) open = true;
            else if (val.equalsIgnoreCase("false")) open = false;
            else {
                Components.sendInfoMessage(sender, "Usage: /guild setopen <guildName> <true|false>");
                return true;
            }
            boolean ok = plugin.guildService.setOpenByName(guildName, open);
            if (ok) {
                Components.sendSuccessMessage(sender, "Set guild " + guildName + " open=" + open + ".");
            } else {
                Components.sendErrorMessage(sender, "Failed to set open for guild " + guildName + ".");
            }
            return true;
        }

        // Player own guild (admin)
        if ((args.length == 2) && can(sender, true, false)) {
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use this form.");
                return true;
            }
            String val = args[1];
            boolean open;
            if (val.equalsIgnoreCase("true")) open = true;
            else if (val.equalsIgnoreCase("false")) open = false;
            else {
                Components.sendInfoMessage(sender, "Usage: /guild setopen <true|false>");
                return true;
            }
            boolean ok = plugin.guildService.setOpenByMember(player.getUniqueId(), open);
            if (ok) {
                Components.sendSuccessMessage(sender, "Your guild is now " + (open ? "open" : "invitedOnly") + ".");
            } else {
                Components.sendErrorMessage(sender, "Failed to set open. You must be an admin of a guild.");
            }
            return true;
        }

        Components.sendInfoMessage(sender, "Usage: /guild setopen " + (can(sender, false, true) ? "[guild] " : "") + "<true|false>");
        return true;
    }

    @Override
    public String getName() {
        return "setopen";
    }

    @Override
    public String getPermission() {
        return "furious.guild.setopen";
    }
}
