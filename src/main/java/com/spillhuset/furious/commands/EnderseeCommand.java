package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnderseeCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final PlayerDataManager playerDataManager;

    public EnderseeCommand(Furious furious) {
        this.plugin = furious;
        this.playerDataManager = furious.getPlayerDataManager();
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if the player has permission
        if (!sender.hasPermission("furious.endersee")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /endersee <player>", NamedTextColor.YELLOW));
            return true;
        }

        String targetName = args[0];

        // Check if the player is trying to view their own enderchest
        if (targetName.equalsIgnoreCase(viewer.getName())) {
            sender.sendMessage(Component.text("Use /enderchest to view your own enderchest!", NamedTextColor.RED));
            return true;
        }

        // Try to get an online player first
        Player onlineTarget = Bukkit.getPlayer(targetName);

        if (onlineTarget != null) {
            viewer.openInventory(onlineTarget.getEnderChest());
            return true;
        }

        // Check if player has permission to view offline players' enderchests
        if (!sender.hasPermission("furious.endersee.offline")) {
            sender.sendMessage(Component.text("You don't have permission to view offline players' enderchests!", NamedTextColor.RED));
            return true;
        }

        // Try to get the player's enderchest using PlayerDataManager
        Inventory enderchest = playerDataManager.getPlayerEnderChest(targetName);

        if (enderchest == null) {
            sender.sendMessage(Component.text("Player has never played on this server!", NamedTextColor.RED));
            return true;
        }

        // Open the enderchest to the viewer
        viewer.openInventory(enderchest);
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
