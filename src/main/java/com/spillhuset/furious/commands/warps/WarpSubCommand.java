package com.spillhuset.furious.commands.warps;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for teleporting to a warp.
 */
public class WarpSubCommand implements SubCommand {
    private final Furious plugin;
    private final AuditLogger auditLogger;

    /**
     * Creates a new WarpSubCommand.
     *
     * @param plugin The plugin instance
     */
    public WarpSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "warp";
    }

    @Override
    public String getDescription() {
        return "Teleports you to a warp.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/warps warp <name> [password] - Teleports you to a warp", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, "unknown", "use warp", "Command can only be used by players");
            return true;
        }

        // Check if enough arguments
        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String warpName = args[1];
        String password = args.length >= 3 ? args[2] : null;

        // Log details about the warp attempt
        String passwordDetails = password != null ? "with password" : "without password";

        // Teleport to the warp
        boolean success = plugin.getWarpsManager().teleportToWarp(player, warpName, password);

        // Log the warp operation
        if (success) {
            auditLogger.logWarpOperation(
                sender,
                warpName,
                passwordDetails
            );
        } else {
            auditLogger.logFailedAccess(
                sender,
                warpName,
                "use warp",
                "Warp teleport failed - " + passwordDetails
            );
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
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
        return "furious.warps.warp";
    }
}
