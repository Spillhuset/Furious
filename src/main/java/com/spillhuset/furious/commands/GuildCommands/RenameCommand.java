package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RenameCommand implements SubCommandInterface {
    private final Furious plugin;

    public RenameCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        // Admin form: /guild rename <oldName> <newName>
        if (can(sender, false, true)) {
            if (args.length == 2) {
                // Suggest existing guild names for <oldName>
                String prefix = args[1].toLowerCase();
                for (String name : plugin.guildService.getAllGuildNames()) {
                    if (name.toLowerCase().startsWith(prefix)) {
                        completions.add(name);
                    }
                }
                return completions;
            }
            // args.length == 3 -> <newName>, no strong suggestions
        }

        // Player form: /guild rename <newName> (no reasonable completion)
        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Admin form: /guild rename <oldName> <newName>
        if (args.length == 3 && can(sender, true, true)) {
            String oldName = args[1];
            String newName = args[2];
            boolean ok = plugin.guildService.renameGuildByName(oldName, newName);
            if (ok) {
                Components.sendSuccessMessage(sender, "Guild renamed: " + oldName + " -> " + newName);
            } else {
                Components.sendErrorMessage(sender, "Failed to rename guild. Check that the old name exists and the new name is available.");
            }
            return true;
        }

        // Player form: /guild rename <newName>
        if (args.length == 2 && can(sender, true, false)) {
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use this form of /guild rename.");
                return true;
            }
            String newName = args[1];
            boolean ok = plugin.guildService.renameGuildByMember(player.getUniqueId(), newName);
            if (ok) {
                Components.sendSuccessMessage(sender, "Your guild was renamed to: " + newName);
            } else {
                Components.sendErrorMessage(sender, "Failed to rename your guild. You must be an admin/owner and the name must be available.");
            }
            return true;
        }

        Components.sendInfoMessage(sender, "Usage: /guild rename " + (can(sender, false, true) ? "[oldName] " : "") + "<newName>");
        return true;
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getPermission() {
        return "furious.guild.rename";
    }
}
