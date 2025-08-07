package com.spillhuset.furious.commands.warps;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for toggling warp visibility for admins.
 */
public class VisibilitySubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new VisibilitySubCommand.
     *
     * @param plugin The plugin instance
     */
    public VisibilitySubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "visibility";
    }

    @Override
    public String getDescription() {
        return "Toggles visibility of warp armor stands for admins.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("/warps visibility - Toggles visibility of warp armor stands", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return false;
        }

        // Check if player has permission
        if (!player.hasPermission(getPermission())) {
            sender.sendMessage(Component.text("You don't have permission to toggle warp visibility!", NamedTextColor.RED));
            return false;
        }

        // Toggle visibility
        plugin.getWarpsManager().toggleWarpVisibility(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No arguments for this command
    }

    @Override
    public String getPermission() {
        return "furious.warps.visibility";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can use this command
    }
}