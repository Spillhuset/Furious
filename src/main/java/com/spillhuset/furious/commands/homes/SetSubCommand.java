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
 * Subcommand for setting a home.
 */
public class SetSubCommand implements SubCommand {
    private final Furious plugin;
    private final AuditLogger auditLogger;

    /**
     * Creates a new SetSubCommand.
     *
     * @param plugin The plugin instance
     */
    public SetSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getDescription() {
        return "Sets a home at your current location.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/homes set [name] - Sets a home at your current location", NamedTextColor.YELLOW));

        if (sender.hasPermission("furious.homes.admin")) {
            sender.sendMessage(Component.text("/homes set <player> <name> - Sets a home for another player", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Admin command: /homes set <player> <name>
        if (args.length >= 3 && sender.hasPermission("furious.homes.admin")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }

            String homeName = args[2];
            if (plugin.getHomesManager().setPlayerHome(target, homeName, player.getLocation())) {
                sender.sendMessage(Component.text("Home '" + homeName + "' set for " + target.getName() + "!", NamedTextColor.GREEN));
                target.sendMessage(Component.text(player.getName() + " set a home '" + homeName + "' for you!", NamedTextColor.GREEN));
                auditLogger.logSensitiveOperation(sender, "set home", "Set home '" + homeName + "' for player " + target.getName());
            } else {
                sender.sendMessage(Component.text("Failed to set home for " + target.getName() + "!", NamedTextColor.RED));
                auditLogger.logFailedAccess(sender, target.getName(), "set home", "Failed to set home '" + homeName + "'");
            }
            return true;
        }

        // Regular command: /homes set [name]
        String homeName = args.length >= 2 ? args[1] : "default";

        if (plugin.getHomesManager().setPlayerHome(player, homeName, player.getLocation())) {
            player.sendMessage(Component.text("Home '" + homeName + "' set!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to set home! You may have reached your home limit or this world is disabled for homes.", NamedTextColor.RED));
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

                    // Always suggest "default" if it matches
                    if ("default".startsWith(partial) && completions.stream().noneMatch(s -> s.equals("default"))) {
                        completions.add("default");
                    }
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

                // Always suggest "default" if it matches
                if ("default".startsWith(partial) && completions.stream().noneMatch(s -> s.equals("default"))) {
                    completions.add("default");
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.homes.set";
    }
}
