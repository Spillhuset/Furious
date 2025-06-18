package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for detailed audit logging of sensitive operations.
 * This class provides methods to log various types of sensitive operations,
 * including inventory viewing, enderchest viewing, teleport operations, warp operations,
 * and other administrative actions.
 */
public class AuditLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Logger logger;

    /**
     * Creates a new AuditLogger.
     *
     * @param plugin The plugin instance
     */
    public AuditLogger(Furious plugin) {
        this.logger = plugin.getLogger();
    }

    /**
     * Logs an inventory viewing operation.
     *
     * @param sender The command sender who initiated the operation
     * @param targetName The name of the player whose inventory was viewed
     * @param isOnline Whether the target player is online
     */
    public void logInventoryView(CommandSender sender, String targetName, boolean isOnline) {
        String senderInfo = getSenderInfo(sender);
        String timestamp = getCurrentTimestamp();
        String status = isOnline ? "ONLINE" : "OFFLINE";

        logger.info(String.format("[AUDIT] %s | %s viewed %s's inventory | Target Status: %s",
                timestamp, senderInfo, targetName, status));
    }

    /**
     * Logs an enderchest viewing operation.
     *
     * @param sender The command sender who initiated the operation
     * @param targetName The name of the player whose enderchest was viewed
     * @param isOnline Whether the target player is online
     */
    public void logEnderchestView(CommandSender sender, String targetName, boolean isOnline) {
        String senderInfo = getSenderInfo(sender);
        String timestamp = getCurrentTimestamp();
        String status = isOnline ? "ONLINE" : "OFFLINE";

        logger.info(String.format("[AUDIT] %s | %s viewed %s's enderchest | Target Status: %s",
                timestamp, senderInfo, targetName, status));
    }

    /**
     * Logs a failed attempt to access player data.
     *
     * @param sender The command sender who initiated the operation
     * @param targetName The name of the player whose data was attempted to be accessed
     * @param operationType The type of operation that was attempted (e.g., "inventory view", "enderchest view")
     * @param reason The reason for the failure
     */
    public void logFailedAccess(CommandSender sender, String targetName, String operationType, String reason) {
        String senderInfo = getSenderInfo(sender);
        String timestamp = getCurrentTimestamp();

        logger.warning(String.format("[AUDIT] %s | %s attempted to %s for %s | FAILED: %s",
                timestamp, senderInfo, operationType, targetName, reason));
    }

    /**
     * Logs a general sensitive operation.
     *
     * @param sender The command sender who initiated the operation
     * @param operation The operation that was performed
     * @param details Additional details about the operation
     */
    public void logSensitiveOperation(CommandSender sender, String operation, String details) {
        String senderInfo = getSenderInfo(sender);
        String timestamp = getCurrentTimestamp();

        logger.info(String.format("[AUDIT] %s | %s performed %s | Details: %s",
                timestamp, senderInfo, operation, details));
    }

    /**
     * Gets information about the command sender.
     *
     * @param sender The command sender
     * @return A string containing information about the sender
     */
    private String getSenderInfo(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return String.format("Player '%s' (UUID: %s, IP: %s)",
                    player.getName(), player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
        } else {
            return String.format("Console '%s'", sender.getName());
        }
    }

    /**
     * Logs a teleport operation.
     *
     * @param sender The command sender who initiated the operation
     * @param targetName The name of the player who was teleported
     * @param destination The destination of the teleport (player name, coordinates, or "worldspawn")
     * @param details Additional details about the teleport operation
     */
    public void logTeleportOperation(CommandSender sender, String targetName, String destination, String details) {
        String senderInfo = getSenderInfo(sender);
        String timestamp = getCurrentTimestamp();

        logger.info(String.format("[AUDIT] %s | %s teleported %s to %s | Details: %s",
                timestamp, senderInfo, targetName, destination, details));
    }

    /**
     * Logs a warp operation.
     *
     * @param sender The command sender who initiated the operation
     * @param warpName The name of the warp
     * @param details Additional details about the warp operation
     */
    public void logWarpOperation(CommandSender sender, String warpName, String details) {
        String senderInfo = getSenderInfo(sender);
        String timestamp = getCurrentTimestamp();

        logger.info(String.format("[AUDIT] %s | %s used warp '%s' | Details: %s",
                timestamp, senderInfo, warpName, details));
    }

    /**
     * Gets the current timestamp.
     *
     * @return The current timestamp as a formatted string
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
