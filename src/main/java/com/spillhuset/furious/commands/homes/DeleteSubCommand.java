package com.spillhuset.furious.commands.homes;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for deleting a home.
 */
public class DeleteSubCommand implements SubCommand {
    private final Furious plugin;
    private final AuditLogger auditLogger;

    /**
     * Creates a new DeleteSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeleteSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Deletes a home.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/homes delete <n> - Deletes a home", NamedTextColor.YELLOW));

        if (sender.hasPermission("furious.homes.delete.others") || sender.hasPermission("furious.homes.admin")) {
            sender.sendMessage(Component.text("/homes delete <player> <n> - Deletes a home for another player", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // Admin command: /homes delete <player> <n>
        if (args.length >= 3 && (sender.hasPermission("furious.homes.delete.others") || sender.hasPermission("furious.homes.admin"))) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }

            String homeName = args[2];
            if (plugin.getHomesManager().deletePlayerHome(target.getUniqueId(), homeName)) {
                sender.sendMessage(Component.text("Home '" + homeName + "' deleted for " + target.getName() + "!", NamedTextColor.GREEN));
                target.sendMessage(Component.text(sender.getName() + " deleted your home '" + homeName + "'!", NamedTextColor.YELLOW));
                auditLogger.logSensitiveOperation(sender, "delete home", "Deleted home '" + homeName + "' for player " + target.getName());
            } else {
                sender.sendMessage(Component.text("Failed to delete home for " + target.getName() + "! The home may not exist.", NamedTextColor.RED));
                auditLogger.logFailedAccess(sender, target.getName(), "delete home", "Failed to delete home '" + homeName + "', home may not exist");
            }
            return true;
        }

        // Regular command: /homes delete <n>
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        String homeName = args[1];

        if (plugin.getHomesManager().deletePlayerHome(player.getUniqueId(), homeName)) {
            player.sendMessage(Component.text("Home '" + homeName + "' deleted!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to delete home! The home may not exist.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            if (sender.hasPermission("furious.homes.delete.others") || sender.hasPermission("furious.homes.admin")) {
                // Suggest player names for admin command
                String partial = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            } else {
                // Suggest home names for regular command
                if (sender instanceof Player player) {
                    String partial = args[1].toLowerCase();
                    plugin.getHomesManager().getPlayerHomes(player.getUniqueId()).forEach(home -> {
                        if (home.getName().startsWith(partial)) {
                            completions.add(home.getName());
                        }
                    });
                }
            }
        } else if (args.length == 3 && (sender.hasPermission("furious.homes.delete.others") || sender.hasPermission("furious.homes.admin"))) {
            // For admin command, suggest home names for the target player
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                String partial = args[2].toLowerCase();
                plugin.getHomesManager().getPlayerHomes(target.getUniqueId()).forEach(home -> {
                    if (home.getName().startsWith(partial)) {
                        completions.add(home.getName());
                    }
                });
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.homes.delete";
    }
}