package com.spillhuset.furious.commands.homes;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Home;
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
 * Subcommand for teleporting to a home.
 */
public class TeleportSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new TeleportSubCommand.
     *
     * @param plugin The plugin instance
     */
    public TeleportSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "tp";
    }

    @Override
    public String getDescription() {
        return "Teleports you to a home.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/homes tp [name] - Teleports you to a home", NamedTextColor.YELLOW));

        if (sender.hasPermission("furious.homes.admin")) {
            sender.sendMessage(Component.text("/homes tp <player> <name> - Teleports you to another player's home", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Admin command: /homes tp <player> <name>
        if (args.length >= 3 && sender.hasPermission("furious.homes.admin")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                return true;
            }

            String homeName = args[2];
            Home home = plugin.getHomesManager().getPlayerHome(target.getUniqueId(), homeName);

            if (home == null) {
                sender.sendMessage(Component.text("Home '" + homeName + "' not found for " + target.getName() + "!", NamedTextColor.RED));
                return true;
            }

            if (home.getLocation() == null) {
                sender.sendMessage(Component.text("Home world doesn't exist!", NamedTextColor.RED));
                return true;
            }

            player.teleport(home.getLocation());
            player.sendMessage(Component.text("Teleported to " + target.getName() + "'s home '" + homeName + "'!", NamedTextColor.GREEN));
            return true;
        }

        // Regular command: /homes tp [name]
        String homeName = args.length >= 2 ? args[1] : "default";

        if (plugin.getHomesManager().teleportToPlayerHome(player, homeName)) {
            // Success message is sent by the manager
            return true;
        } else {
            // Failure message is sent by the manager
            return true;
        }
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

                // Also suggest own home names
                if (sender instanceof Player player) {
                    plugin.getHomesManager().getPlayerHomes(player.getUniqueId()).forEach(home -> {
                        if (home.getName().startsWith(partial)) {
                            completions.add(home.getName());
                        }
                    });
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
        return "furious.homes.tp";
    }
}