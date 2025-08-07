package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.PlayerVisibilityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to toggle player visibility in the locator bar
 */
public class HideCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final PlayerVisibilityManager playerVisibilityManager;

    /**
     * Creates a new HideCommand.
     *
     * @param plugin The plugin instance
     */
    public HideCommand(Furious plugin) {
        this.plugin = plugin;
        this.playerVisibilityManager = plugin.getPlayerVisibilityManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Only players can use this command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        // Check if the player has permission to use this command
        if (!player.hasPermission("furious.hide")) {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        // Toggle the player's visibility in the locator bar
        playerVisibilityManager.togglePlayerLocatorBarVisibility(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }
}