package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.PlayerDataManager;
import com.spillhuset.furious.misc.StandaloneCommand;
import com.spillhuset.furious.utils.AuditLogger;
import com.spillhuset.furious.utils.HelpMenuFormatter;
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

public class InvseeCommand extends StandaloneCommand {
    private final PlayerDataManager playerDataManager;
    private final AuditLogger auditLogger;
    private final RateLimiter rateLimiter;

    public InvseeCommand(Furious furious) {
        super(furious);
        this.playerDataManager = furious.getPlayerDataManager();
        this.auditLogger = furious.getAuditLogger();
        this.rateLimiter = furious.getRateLimiter();
    }

    @Override
    public String getName() {
        return "invsee";
    }

    @Override
    public String getDescription() {
        return "View another player's inventory";
    }

    @Override
    public void getUsage(CommandSender sender) {
        HelpMenuFormatter.showPlayerCommandsHeader(sender, "Invsee");
        HelpMenuFormatter.formatPlayerSubCommandWithParams(sender, "/invsee", "", "<player>", "", "View another player's inventory");

        if (sender.hasPermission("furious.invsee.offline") || sender.isOp()) {
            HelpMenuFormatter.showAdminCommandsHeader(sender, "Invsee");
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/invsee", "", "<player>", "", "View offline player's inventory");
        }
    }

    @Override
    public String getPermission() {
        return "furious.invsee";
    }

    @Override
    public boolean denyNonPlayer() {
        return true;
    }

    /**
     * This command uses PlayerDataManager for inventory viewing functionality.
     * It handles both online and offline players with appropriate permissions.
     */

    /**
     * Checks if the sender has permission to view offline players' inventories.
     *
     * @param sender The command sender
     * @return true if the sender has permission, false otherwise
     */
    private boolean checkOfflinePermission(CommandSender sender) {
        if (sender.hasPermission("furious.invsee.offline")) {
            return true;
        } else {
            sender.sendMessage(Component.text("You don't have permission to view offline players' inventories!", NamedTextColor.RED));
            return false;
        }
    }

    @Override
    protected boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // Since denyNonPlayer() returns true, we know sender is a Player at this point
        Player viewer = (Player) sender;

        // Check rate limit
        if (!rateLimiter.checkRateLimit(sender, "invsee")) {
            int timeUntilReset = rateLimiter.getTimeUntilReset(viewer, "invsee");
            sender.sendMessage(Component.text("You are using this command too frequently! Please wait " + timeUntilReset + " seconds before trying again.", NamedTextColor.RED));
            return true;
        }

        // Check if a target player was specified
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /invsee <player>", NamedTextColor.YELLOW));
            return true;
        }

        // Sanitize the player name
        String targetName = InputSanitizer.sanitizePlayerName(args[0]);

        // Check if the player name is valid
        if (targetName == null) {
            sender.sendMessage(Component.text("Invalid player name! Please use a valid Minecraft username.", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, args[0], "view inventory", "Invalid player name");
            return true;
        }

        // Check if the input is safe
        if (!InputSanitizer.isSafeInput(args[0])) {
            sender.sendMessage(Component.text("Invalid input detected! Please use only alphanumeric characters and underscores.", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, args[0], "view inventory", "Unsafe input");
            return true;
        }

        // Check if the player is trying to view their own inventory
        if (targetName.equalsIgnoreCase(viewer.getName())) {
            sender.sendMessage(Component.text("You cannot view your own inventory with this command!", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, targetName, "view inventory", "Attempted to view own inventory");
            return true;
        }

        // Try to get an online player first
        Player onlineTarget = Bukkit.getPlayer(targetName);

        if (onlineTarget != null) {
            // Log the successful inventory view for online player
            auditLogger.logInventoryView(sender, targetName, true);
            viewer.openInventory(onlineTarget.getInventory());
            return true;
        }

        // Check if player has permission to view offline players' inventories
        if (!checkOfflinePermission(sender)) {
            auditLogger.logFailedAccess(sender, targetName, "view offline inventory", "No permission for offline access");
            return true;
        }

        // Try to get the player's inventory using PlayerDataManager
        Inventory inventory = playerDataManager.getPlayerInventory(targetName);

        if (inventory == null) {
            sender.sendMessage(Component.text("Player has never played on this server!", NamedTextColor.RED));
            auditLogger.logFailedAccess(sender, targetName, "view inventory", "Player has never played on this server");
            return true;
        }

        // Log the inventory view attempt for offline player
        auditLogger.logInventoryView(sender, targetName, false);

        // Open the inventory to the viewer
        viewer.openInventory(inventory);

        return true;
    }
    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
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

            // Add offline players if the sender has permission
            if (sender.hasPermission("furious.invsee.offline")) {
                // Get offline players (limit to a reasonable number to avoid performance issues)
                OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
                int count = 0;
                for (OfflinePlayer offlinePlayer : offlinePlayers) {
                    // Skip if already online or is the sender
                    if (offlinePlayer.isOnline() ||
                        (sender instanceof Player && offlinePlayer.getUniqueId().equals(((Player) sender).getUniqueId()))) {
                        continue;
                    }

                    String offlinePlayerName = offlinePlayer.getName();
                    if (offlinePlayerName != null && offlinePlayerName.toLowerCase().startsWith(partialName)) {
                        completions.add(offlinePlayerName);
                        count++;

                        // Limit to 20 offline players to avoid performance issues
                        if (count >= 20) {
                            break;
                        }
                    }
                }
            }
        }

        return completions;
    }

}
