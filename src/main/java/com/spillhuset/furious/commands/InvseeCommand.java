package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InvseeCommand implements CommandExecutor, TabCompleter {
    public InvseeCommand(Furious furious) {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if a target player was specified
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /invsee <player>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        // Check if the target player exists and is online
        if (target == null) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        // Open target's inventory to the viewer
        viewer.openInventory(target.getInventory());
        return true;
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        // Only provide suggestions for the first argument
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            // Get all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerName = player.getName();
                // Don't suggest the command sender's name
                if (sender instanceof Player && playerName.equals(sender.getName())) {
                    continue;
                }
                // Add names that match what the player has typed so far
                if (playerName.toLowerCase().startsWith(partialName)) {
                    completions.add(playerName);
                }
            }
        }

        return completions;
    }

}
