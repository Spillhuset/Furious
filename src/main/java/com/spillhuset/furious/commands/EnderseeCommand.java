package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.PlayerDataManager;
import com.spillhuset.furious.misc.StandaloneCommand;
import com.spillhuset.furious.utils.AuditLogger;
import com.spillhuset.furious.utils.InputSanitizer;
import com.spillhuset.furious.utils.RateLimiter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnderseeCommand extends StandaloneCommand {
    private final PlayerDataManager playerDataManager;
    private final AuditLogger auditLogger;
    private final RateLimiter rateLimiter;

    public EnderseeCommand(Furious furious) {
        super(furious);
        this.playerDataManager = furious.getPlayerDataManager();
        this.auditLogger = furious.getAuditLogger();
        this.rateLimiter = furious.getRateLimiter();
    }

    @Override
    public String getName() {
        return "endersee";
    }

    @Override
    public String getDescription() {
        return "View another player's enderchest";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /endersee <player>", NamedTextColor.YELLOW));
    }

    @Override
    public String getPermission() {
        return "furious.endersee";
    }

    @Override
    public boolean denyNonPlayer() {
        return true;
    }

    /**
     * Checks if the sender has permission to view offline players' enderchests.
     *
     * @param sender The command sender
     * @return true if the sender has permission, false otherwise
     */
    private boolean checkOfflinePermission(CommandSender sender) {
        if (sender.hasPermission("furious.endersee.offline")) {
            return true;
        } else {
            sender.sendMessage(Component.text("You don't have permission to view offline players' enderchests!", NamedTextColor.RED));
            return false;
        }
    }

    @Override
    protected boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, "unknown", "view enderchest", "Command can only be used by players");
            return true;
        }

        // Check rate limit
        if (!rateLimiter.checkRateLimit(sender, "endersee")) {
            int timeUntilReset = rateLimiter.getTimeUntilReset(viewer, "endersee");
            sender.sendMessage(Component.text("You are using this command too frequently! Please wait " + timeUntilReset + " seconds before trying again.", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /endersee <player>", NamedTextColor.YELLOW));
            return true;
        }

        // Sanitize the player name
        String targetName = InputSanitizer.sanitizePlayerName(args[0]);

        // Check if the player name is valid
        if (targetName == null) {
            sender.sendMessage(Component.text("Invalid player name! Please use a valid Minecraft username.", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, args[0], "view enderchest", "Invalid player name");
            return true;
        }

        // Check if the input is safe
        if (!InputSanitizer.isSafeInput(args[0])) {
            sender.sendMessage(Component.text("Invalid input detected! Please use only alphanumeric characters and underscores.", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, args[0], "view enderchest", "Unsafe input");
            return true;
        }

        // Check if the player is trying to view their own enderchest
        if (targetName.equalsIgnoreCase(viewer.getName())) {
            sender.sendMessage(Component.text("Use /enderchest to view your own enderchest!", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, targetName, "view enderchest", "Attempted to view own enderchest");
            return true;
        }

        // Try to get an online player first
        Player onlineTarget = Bukkit.getPlayer(targetName);

        if (onlineTarget != null) {
            // Log the successful enderchest view for online player
            auditLogger.logEnderchestView(sender, targetName, true);
            viewer.openInventory(onlineTarget.getEnderChest());
            return true;
        }

        // Check if player has permission to view offline players' enderchests
        if (!checkOfflinePermission(sender)) {
            auditLogger.logFailedAccess(sender, targetName, "view offline enderchest", "No permission for offline access");
            return true;
        }

        // Try to get the player's enderchest using PlayerDataManager
        Inventory enderchest = playerDataManager.getPlayerEnderChest(targetName);

        if (enderchest == null) {
            sender.sendMessage(Component.text("Player has never played on this server!", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, targetName, "view enderchest", "Player has never played on this server");
            return true;
        }

        // Log the successful enderchest view for offline player
        auditLogger.logEnderchestView(sender, targetName, false);

        // Open the enderchest to the viewer
        viewer.openInventory(enderchest);
        return true;
    }


    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
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
