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
 * Subcommand for renaming a warp.
 */
public class RenameSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new RenameSubCommand.
     *
     * @param plugin The plugin instance
     */
    public RenameSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "Renames a warp.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps rename <oldname> <newname> - Renames a warp", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player is op
        if (!player.isOp()) {
            sender.sendMessage(Component.text("Only operators can rename warps!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String oldName = args[1];
        String newName = args[2];

        // Rename the warp
        if (plugin.getWarpsManager().renameWarp(player, oldName, newName)) {
            sender.sendMessage(Component.text("Warp renamed from '" + oldName + "' to '" + newName + "'!", NamedTextColor.GREEN));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2 && sender.isOp()) {
            // Suggest warp names for the old name
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
        return "furious.warps.rename";
    }
}