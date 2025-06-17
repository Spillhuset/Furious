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
 * Subcommand for deleting a warp.
 */
public class DeleteSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new DeleteSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeleteSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Deletes a warp.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps delete <name> - Deletes a warp", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player is op
        if (!player.isOp()) {
            sender.sendMessage(Component.text("Only operators can delete warps!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String warpName = args[1];

        // Delete the warp
        if (plugin.getWarpsManager().deleteWarp(player, warpName)) {
            sender.sendMessage(Component.text("Warp '" + warpName + "' deleted successfully!", NamedTextColor.GREEN));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && sender.isOp()) {
            // Suggest warp names
            String partial = args[1].toLowerCase();
            plugin.getWarpsManager().getAllWarps().forEach(warp -> {
                if (warp.getName().startsWith(partial)) {
                    completions.add(warp.getName());
                }
            });
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.warps.delete";
    }
}