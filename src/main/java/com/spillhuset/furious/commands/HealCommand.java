package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HealCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    public HealCommand(Furious furious) {
        this.plugin = furious;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        double maxHealth = 20;

        if (args.length == 0 && sender.hasPermission("furious.heal.self")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            player.setHealth(maxHealth);
            player.sendMessage(Component.text("You have been healed to full health!", NamedTextColor.GREEN));
        }
        if (args.length >= 1 && sender.hasPermission("furious.heal.others")) {
            StringBuilder found = new StringBuilder();
            StringBuilder notFound = new StringBuilder();
            for (String arg : args) {
                List<Player> targets = new ArrayList<>();

                // Handle selectors
                if (arg.equals("@a")) {
                    // Get all online players
                    targets.addAll(plugin.getServer().getOnlinePlayers());
                } else if (arg.equals("@p") && sender instanceof Player senderPlayer) {
                    // Get nearest player to sender
                    Player nearestPlayer = getNearestPlayer(senderPlayer);
                    if (nearestPlayer != null) {
                        targets.add(nearestPlayer);
                    }
                } else {
                    // Regular player name
                    Player target = plugin.getServer().getPlayer(arg);
                    if (target != null) {
                        targets.add(target);
                    } else {
                        notFound.append(arg).append(", ");
                    }
                }

                // Heal all found targets
                for (Player target : targets) {
                    found.append(target.getName()).append(", ");
                    double targetMaxHealth = Objects.requireNonNull(target.getAttribute(Attribute.MAX_HEALTH)).getValue();
                    target.setHealth(targetMaxHealth);
                    target.sendMessage(Component.text("You have been healed to full health!", NamedTextColor.GREEN));
                }
            }
            if (!notFound.isEmpty()) {
                notFound.delete(notFound.lastIndexOf(", "),notFound.length() );
                sender.sendMessage(Component.text("Could not find player(s): "+notFound, NamedTextColor.RED));
                return true;
            }
            if (!found.isEmpty()) {
                found.delete(found.lastIndexOf(", "), found.length());
                sender.sendMessage(Component.text("Healed player(s): "+found, NamedTextColor.GREEN));
            }
            if (found.isEmpty() && notFound.isEmpty()) {
                sender.sendMessage(Component.text("No players found!", NamedTextColor.RED));
            }
        }
        return true;
    }

    /**
     * Gets the nearest player to the given player
     * @param player The player to find the nearest player to
     * @return The nearest player, or null if no other players are online
     */
    private Player getNearestPlayer(Player player) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            // Skip the player themselves
            if (onlinePlayer.equals(player)) {
                continue;
            }

            // Skip players in different worlds
            if (!onlinePlayer.getWorld().equals(player.getWorld())) {
                continue;
            }

            double distance = player.getLocation().distance(onlinePlayer.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = onlinePlayer;
            }
        }

        return nearestPlayer;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length >= 1) {
            String partial = args[args.length - 1].toLowerCase();
            List<String> alreadyAdded = new ArrayList<>();

            // Add all arguments except the current one to the already added list
            if (args.length > 1) {
                for (int i = 0; i < args.length - 1; i++) {
                    alreadyAdded.add(args[i].toLowerCase());
                }
            }

            // Add selectors if they match the partial input
            if ("@a".startsWith(partial) && !alreadyAdded.contains("@a")) {
                completions.add("@a");
            }
            if ("@p".startsWith(partial) && !alreadyAdded.contains("@p")) {
                completions.add("@p");
            }

            // Add matching player names that haven't been added yet
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(partial) &&
                    !alreadyAdded.contains(playerName.toLowerCase())) {
                    completions.add(playerName);
                }
            }
        }

        return completions.isEmpty() ? Collections.emptyList() : completions;
    }
}
