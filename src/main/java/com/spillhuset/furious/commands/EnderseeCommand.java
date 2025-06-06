package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnderseeCommand implements CommandExecutor, TabCompleter {
    public EnderseeCommand(Furious furious) {
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /endersee <player>", NamedTextColor.YELLOW));
            return true;
        }

        String targetName = args[0];

        // Try to get an online player first
        Player onlineTarget = Bukkit.getPlayer(targetName);

        if (onlineTarget != null) {
            viewer.openInventory(onlineTarget.getEnderChest());
            return true;
        }

        // Try to load offline player data
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

        if (!offlineTarget.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player has never played on this server!", NamedTextColor.RED));
            return true;
        }

        try {
            // Load the player data file
            Player loadedPlayer = (Player) offlineTarget.getPlayer();
            if (loadedPlayer == null) {
                sender.sendMessage(Component.text("Could not load player data!", NamedTextColor.RED));
                return true;
            }
            viewer.openInventory(loadedPlayer.getEnderChest());
        } catch (Exception e) {
            sender.sendMessage(Component.text("Error loading enderchest: " + e.getMessage(), NamedTextColor.RED));
            return true;
        }

        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            // Get all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                if (sender instanceof Player && playerName.equals(((Player) sender).getName())) {
                    continue;
                }
                if (playerName.toLowerCase().startsWith(partialName)) {
                    completions.add(playerName);
                }
            }
        }

        return completions;
    }

}
