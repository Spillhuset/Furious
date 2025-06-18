package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for rate limiting commands to prevent abuse.
 * This class tracks command usage by players and enforces rate limits
 * based on configuration settings.
 */
public class RateLimiter {
    private final Furious plugin;
    private final AuditLogger auditLogger;

    // Map of player UUID to their command usage history
    private final Map<UUID, Map<String, List<Long>>> usageHistory = new ConcurrentHashMap<>();

    // Default rate limits if not specified in config
    private final int DEFAULT_MAX_USES = 5;
    private final int DEFAULT_TIME_WINDOW_SECONDS = 60;

    // Map of command names to their rate limit settings
    private final Map<String, RateLimit> rateLimits = new HashMap<>();

    // Commands that are exempt from rate limiting
    private final Set<String> exemptCommands = new HashSet<>();

    // Player permissions that exempt them from rate limiting
    private final String RATE_LIMIT_EXEMPT_PERMISSION = "furious.ratelimit.exempt";

    /**
     * Creates a new RateLimiter.
     *
     * @param plugin The plugin instance
     */
    public RateLimiter(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();

        // Load rate limit settings from config
        loadRateLimits();

        // Schedule cleanup task to remove old usage data
        scheduleCleanupTask();
    }

    /**
     * Loads rate limit settings from the plugin's configuration.
     */
    private void loadRateLimits() {
        ConfigurationSection rateLimitSection = plugin.getConfig().getConfigurationSection("rate-limits");

        if (rateLimitSection == null) {
            // Use default settings if not configured
            setupDefaultRateLimits();
            return;
        }

        // Load exempt commands
        List<String> exemptCommandsList = plugin.getConfig().getStringList("rate-limits.exempt-commands");
        if (exemptCommandsList != null) {
            exemptCommands.addAll(exemptCommandsList);
        }

        // Load command-specific rate limits
        ConfigurationSection commandsSection = rateLimitSection.getConfigurationSection("commands");
        if (commandsSection != null) {
            for (String commandName : commandsSection.getKeys(false)) {
                ConfigurationSection commandSection = commandsSection.getConfigurationSection(commandName);
                if (commandSection != null) {
                    int maxUses = commandSection.getInt("max-uses", DEFAULT_MAX_USES);
                    int timeWindow = commandSection.getInt("time-window-seconds", DEFAULT_TIME_WINDOW_SECONDS);
                    rateLimits.put(commandName, new RateLimit(maxUses, timeWindow));
                }
            }
        }

        // If no specific commands are configured, use default settings
        if (rateLimits.isEmpty()) {
            setupDefaultRateLimits();
        }
    }

    /**
     * Sets up default rate limits for sensitive commands.
     */
    private void setupDefaultRateLimits() {
        // Default rate limits for sensitive commands
        rateLimits.put("invsee", new RateLimit(5, 60));      // 5 uses per minute
        rateLimits.put("endersee", new RateLimit(5, 60));    // 5 uses per minute
        rateLimits.put("teleport", new RateLimit(10, 60));   // 10 uses per minute
        rateLimits.put("guild", new RateLimit(15, 60));      // 15 uses per minute
        rateLimits.put("locks", new RateLimit(15, 60));      // 15 uses per minute
        rateLimits.put("minigame", new RateLimit(10, 60));   // 10 uses per minute
        rateLimits.put("homes", new RateLimit(10, 60));      // 10 uses per minute
        rateLimits.put("warps", new RateLimit(10, 60));      // 10 uses per minute
    }

    /**
     * Schedules a task to clean up old usage data to prevent memory leaks.
     */
    private void scheduleCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskTimer(plugin, 20 * 60 * 30, 20 * 60 * 30); // Run every 30 minutes
    }

    /**
     * Cleans up old usage data to prevent memory leaks.
     */
    private void cleanup() {
        final long now = System.currentTimeMillis();

        // Find the maximum time window across all rate limits
        final long maxTimeWindow = rateLimits.values().stream()
                .mapToLong(limit -> TimeUnit.SECONDS.toMillis(limit.timeWindowSeconds))
                .max()
                .orElse(TimeUnit.MINUTES.toMillis(5)) // Default to 5 minutes if no rate limits are defined
                + TimeUnit.MINUTES.toMillis(5); // Add a buffer to ensure we don't remove data that might still be needed

        // Remove usage data older than the maximum time window
        for (Map.Entry<UUID, Map<String, List<Long>>> playerEntry : usageHistory.entrySet()) {
            Map<String, List<Long>> commandMap = playerEntry.getValue();

            // For each command, remove timestamps older than the maximum time window
            for (Map.Entry<String, List<Long>> commandEntry : commandMap.entrySet()) {
                List<Long> timestamps = commandEntry.getValue();
                timestamps.removeIf(timestamp -> now - timestamp > maxTimeWindow);
            }

            // Remove empty command entries
            commandMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());

            // If the player has no command usage history left, remove them from the map
            if (commandMap.isEmpty()) {
                usageHistory.remove(playerEntry.getKey());
            }
        }
    }

    /**
     * Checks if a player has exceeded the rate limit for a command.
     *
     * @param sender The command sender
     * @param commandName The name of the command
     * @return true if the player can execute the command, false if they have exceeded the rate limit
     */
    public boolean checkRateLimit(CommandSender sender, String commandName) {
        // Console and non-players are not rate limited
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        // Players with the exempt permission are not rate limited
        if (player.hasPermission(RATE_LIMIT_EXEMPT_PERMISSION)) {
            return true;
        }

        // Commands in the exempt list are not rate limited
        if (exemptCommands.contains(commandName)) {
            return true;
        }

        // Get the rate limit for this command
        RateLimit rateLimit = rateLimits.get(commandName);
        if (rateLimit == null) {
            // No rate limit defined for this command
            return true;
        }

        UUID playerUUID = player.getUniqueId();

        // Get the player's usage history for this command
        Map<String, List<Long>> playerCommands = usageHistory.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>());
        List<Long> timestamps = playerCommands.computeIfAbsent(commandName, k -> new ArrayList<>());

        // Get the current time
        final long now = System.currentTimeMillis();

        // Remove timestamps outside the time window
        long timeWindowMillis = TimeUnit.SECONDS.toMillis(rateLimit.timeWindowSeconds);
        timestamps.removeIf(timestamp -> now - timestamp > timeWindowMillis);

        // Check if the player has exceeded the rate limit
        if (timestamps.size() >= rateLimit.maxUses) {
            // Calculate time until the rate limit resets
            long oldestTimestamp = timestamps.get(0);
            long timeUntilReset = (oldestTimestamp + timeWindowMillis) - now;
            int secondsUntilReset = (int) TimeUnit.MILLISECONDS.toSeconds(timeUntilReset) + 1;

            // Log the rate limit violation
            auditLogger.logFailedAccess(sender, "rate-limited", commandName,
                    "Rate limit exceeded: " + rateLimit.maxUses + " uses per " + rateLimit.timeWindowSeconds + " seconds");

            return false;
        }

        // Add the current timestamp to the usage history
        timestamps.add(now);
        return true;
    }

    /**
     * Gets the time in seconds until a player can use a command again.
     *
     * @param player The player
     * @param commandName The name of the command
     * @return The time in seconds until the player can use the command again, or 0 if they can use it now
     */
    public int getTimeUntilReset(Player player, String commandName) {
        // Players with the exempt permission are not rate limited
        if (player.hasPermission(RATE_LIMIT_EXEMPT_PERMISSION)) {
            return 0;
        }

        // Commands in the exempt list are not rate limited
        if (exemptCommands.contains(commandName)) {
            return 0;
        }

        // Get the rate limit for this command
        RateLimit rateLimit = rateLimits.get(commandName);
        if (rateLimit == null) {
            // No rate limit defined for this command
            return 0;
        }

        UUID playerUUID = player.getUniqueId();

        // Get the player's usage history for this command
        Map<String, List<Long>> playerCommands = usageHistory.get(playerUUID);
        if (playerCommands == null) {
            return 0;
        }

        List<Long> timestamps = playerCommands.get(commandName);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }

        // Get the current time
        final long now = System.currentTimeMillis();

        // Remove timestamps outside the time window
        long timeWindowMillis = TimeUnit.SECONDS.toMillis(rateLimit.timeWindowSeconds);
        timestamps.removeIf(timestamp -> now - timestamp > timeWindowMillis);

        // If the player hasn't exceeded the rate limit, they can use the command now
        if (timestamps.size() < rateLimit.maxUses) {
            return 0;
        }

        // Calculate time until the rate limit resets
        long oldestTimestamp = timestamps.get(0);
        long timeUntilReset = (oldestTimestamp + timeWindowMillis) - now;
        return (int) TimeUnit.MILLISECONDS.toSeconds(timeUntilReset) + 1;
    }

    /**
     * Inner class to represent a rate limit.
     */
    private static class RateLimit {
        final int maxUses;
        final int timeWindowSeconds;

        RateLimit(int maxUses, int timeWindowSeconds) {
            this.maxUses = maxUses;
            this.timeWindowSeconds = timeWindowSeconds;
        }
    }
}
