package com.spillhuset.furious.commands.teleport;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subcommand for listing worlds and their teleportation status
 */
public class WorldsSubCommand implements SubCommand {
    private final Furious plugin;

    public WorldsSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "worlds";
    }

    @Override
    public String getDescription() {
        return "List all worlds and their teleportation status";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/teleport worlds", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        listWorlds(sender);
        return true;
    }

    private void listWorlds(CommandSender sender) {
        sender.sendMessage(Component.text("Available Worlds:", NamedTextColor.YELLOW));
        for (Map.Entry<String, UUID> entry : plugin.getTeleportManager().getWorldUUIDs().entrySet()) {
            String worldName = entry.getKey();

            // Skip game worlds
            if (worldName.equals(plugin.getWorldManager().getGameWorldName()) ||
                worldName.equals(plugin.getWorldManager().getGameBackupName()) ||
                worldName.startsWith("minigame_")) {
                continue;
            }

            boolean disabled = plugin.getTeleportManager().isWorldDisabled(entry.getValue());
            NamedTextColor color = disabled ? NamedTextColor.RED : NamedTextColor.GREEN;
            sender.sendMessage(Component.text("- " + worldName + (disabled ? " [DISABLED]" : ""), color));
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.teleport.worldconfig";
    }
}