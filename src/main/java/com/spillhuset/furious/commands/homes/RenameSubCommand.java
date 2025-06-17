package com.spillhuset.furious.commands.homes;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for renaming a home.
 */
public class RenameSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new RenameSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RenameSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "Renames a home.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/homes rename <oldname> <newname> - Renames a home", NamedTextColor.YELLOW));

        if (sender.hasPermission("furious.homes.admin")) {
            sender.sendMessage(Component.text("/homes rename <player> <oldname> <newname> - Renames a home for another player", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // Admin command: /homes rename <player> <oldname> <newname>
        if (args.length >= 4 && sender.hasPermission("furious.homes.admin")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }

            String oldName = args[2];
            String newName = args[3];

            if (plugin.getHomesManager().renamePlayerHome(target.getUniqueId(), oldName, newName)) {
                sender.sendMessage(Component.text("Home renamed from '" + oldName + "' to '" + newName + "' for " + target.getName() + "!", NamedTextColor.GREEN));
                target.sendMessage(Component.text(sender.getName() + " renamed your home from '" + oldName + "' to '" + newName + "'!", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("Failed to rename home for " + target.getName() + "! The home may not exist or the new name is already taken.", NamedTextColor.RED));
            }
            return true;
        }

        // Regular command: /homes rename <oldname> <newname>
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        String oldName = args[1];
        String newName = args[2];

        if (plugin.getHomesManager().renamePlayerHome(player.getUniqueId(), oldName, newName)) {
            player.sendMessage(Component.text("Home renamed from '" + oldName + "' to '" + newName + "'!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to rename home! The home may not exist or the new name is already taken.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            if (sender.hasPermission("furious.homes.admin")) {
                // Suggest player names for admin command
                String partial = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            } else {
                // Suggest home names for regular command (old name)
                if (sender instanceof Player player) {
                    String partial = args[1].toLowerCase();
                    plugin.getHomesManager().getPlayerHomes(player.getUniqueId()).forEach(home -> {
                        if (home.getName().startsWith(partial)) {
                            completions.add(home.getName());
                        }
                    });
                }
            }
        } else if (args.length == 3) {
            if (sender.hasPermission("furious.homes.admin") && Bukkit.getPlayer(args[1]) != null) {
                // For admin command, suggest home names for the target player (old name)
                Player target = Bukkit.getPlayer(args[1]);
                String partial = args[2].toLowerCase();
                plugin.getHomesManager().getPlayerHomes(target.getUniqueId()).forEach(home -> {
                    if (home.getName().startsWith(partial)) {
                        completions.add(home.getName());
                    }
                });
            } else {
                // For regular command, don't suggest anything for new name
                // or suggest some common names
                String partial = args[2].toLowerCase();
                List<String> commonNames = List.of("home", "base", "main", "spawn", "mine", "farm", "nether", "end");
                for (String name : commonNames) {
                    if (name.startsWith(partial)) {
                        completions.add(name);
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.homes.rename";
    }
}