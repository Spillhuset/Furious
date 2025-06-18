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
 * Subcommand for moving a home to a new location.
 */
public class MoveSubCommand implements SubCommand {
    private final Furious plugin;
    private final AuditLogger auditLogger;

    /**
     * Creates a new MoveSubCommand.
     *
     * @param plugin The plugin instance
     */
    public MoveSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "move";
    }

    @Override
    public String getDescription() {
        return "Moves an existing home to your current location.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/homes move [name] - Moves a home to your current location", NamedTextColor.YELLOW));

        if (sender.hasPermission("furious.homes.admin")) {
            sender.sendMessage(Component.text("/homes move <player> <name> - Moves a home for another player", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Admin command: /homes move <player> <name>
        if (args.length >= 3 && sender.hasPermission("furious.homes.admin")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }

            String homeName = args[2];
            if (plugin.getHomesManager().setPlayerHome(target, homeName, player.getLocation())) {
                sender.sendMessage(Component.text("Home '" + homeName + "' moved for " + target.getName() + "!", NamedTextColor.GREEN));
                target.sendMessage(Component.text(player.getName() + " moved your home '" + homeName + "'!", NamedTextColor.GREEN));
                auditLogger.logSensitiveOperation(sender, "move home", "Moved home '" + homeName + "' for player " + target.getName());
            } else {
                sender.sendMessage(Component.text("Failed to move home for " + target.getName() + "! The home may not exist or this world is disabled for homes.", NamedTextColor.RED));
                auditLogger.logFailedAccess(sender, target.getName(), "move home", "Failed to move home '" + homeName + "', home may not exist or world is disabled");
            }
            return true;
        }

        // Regular command: /homes move [name]
        String homeName = args.length >= 2 ? args[1] : "default";

        if (plugin.getHomesManager().setPlayerHome(player, homeName, player.getLocation())) {
            player.sendMessage(Component.text("Home '" + homeName + "' moved!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to move home! The home may not exist or this world is disabled for homes.", NamedTextColor.RED));
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
        } else if (args.length == 3 && sender.hasPermission("furious.homes.admin")) {
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
        return "furious.homes.move";
    }
}
