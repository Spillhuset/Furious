package com.spillhuset.furious.utils;

import com.spillhuset.furious.Furious;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * Manages scheduling and tracking of regular security reviews.
 * This class is responsible for:
 * 1. Scheduling regular security reviews
 * 2. Tracking when the last security review was conducted
 * 3. Notifying administrators when a security review is due
 * 4. Providing a way to mark a security review as completed
 */
public class SecurityReviewManager implements Listener {
    private final Furious plugin;
    private final File securityReviewFile;
    private FileConfiguration securityReviewConfig;
    private final AuditLogger auditLogger;

    // Default review interval in days (can be configured)
    private static final int DEFAULT_REVIEW_INTERVAL_DAYS = 90;

    /**
     * Creates a new SecurityReviewManager.
     *
     * @param plugin The Furious plugin instance
     */
    public SecurityReviewManager(Furious plugin) {
        this.plugin = plugin;
        this.auditLogger = plugin.getAuditLogger();
        this.securityReviewFile = new File(plugin.getDataFolder(), "security_reviews.yml");

        // Create the security review file if it doesn't exist
        if (!securityReviewFile.exists()) {
            try {
                securityReviewFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create security_reviews.yml", e);
            }
        }

        // Load the security review configuration
        this.securityReviewConfig = YamlConfiguration.loadConfiguration(securityReviewFile);

        // Set default values if they don't exist
        if (!securityReviewConfig.contains("last-review")) {
            securityReviewConfig.set("last-review", new Date().getTime());
            securityReviewConfig.set("next-review", calculateNextReviewDate().getTime());
            securityReviewConfig.set("review-interval-days", DEFAULT_REVIEW_INTERVAL_DAYS);
            securityReviewConfig.set("reviews", null);
            saveConfig();
        }

        // Register this class as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Schedule a task to check if a security review is due
        scheduleReviewCheck();
    }

    /**
     * Schedules a task to check if a security review is due.
     * This task runs once a day.
     */
    private void scheduleReviewCheck() {
        // Run once a day (20 ticks * 60 seconds * 60 minutes * 24 hours)
        long checkInterval = 20L * 60L * 60L * 24L;

        new BukkitRunnable() {
            @Override
            public void run() {
                checkIfReviewIsDue();
            }
        }.runTaskTimer(plugin, 20L * 60L, checkInterval); // Start after 1 minute, then run daily
    }

    /**
     * Checks if a security review is due and notifies administrators if it is.
     */
    private void checkIfReviewIsDue() {
        Date nextReview = new Date(securityReviewConfig.getLong("next-review"));
        Date now = new Date();

        if (now.after(nextReview)) {
            // Log that a security review is due
            plugin.getLogger().warning("A security review is due! The last review was conducted on " +
                    getFormattedDate(securityReviewConfig.getLong("last-review")));

            // Notify online administrators
            notifyAdministrators();

            // Log to audit log
            auditLogger.logSensitiveOperation(null, "security review due",
                    "A security review is due. Last review: " +
                    getFormattedDate(securityReviewConfig.getLong("last-review")));
        }
    }

    /**
     * Notifies all online administrators that a security review is due.
     */
    private void notifyAdministrators() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("furious.security.admin")) {
                player.sendMessage("§c[Security] §fA security review is due! Use §e/security review complete§f to mark it as completed.");
            }
        }
    }

    /**
     * Event handler for player join events.
     * Notifies administrators when they join if a security review is due.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Only notify administrators
        if (player.hasPermission("furious.security.admin")) {
            Date nextReview = new Date(securityReviewConfig.getLong("next-review"));
            Date now = new Date();

            if (now.after(nextReview)) {
                // Schedule the notification to run after a short delay to ensure the player has fully joined
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.sendMessage("§c[Security] §fA security review is due! Use §e/security review complete§f to mark it as completed.");
                    }
                }.runTaskLater(plugin, 40L); // 2 seconds delay
            }
        }
    }

    /**
     * Marks a security review as completed.
     *
     * @param reviewer The name of the player who completed the review
     * @param notes Optional notes about the review
     * @return true if the review was marked as completed, false otherwise
     */
    public boolean completeSecurityReview(String reviewer, String notes) {
        try {
            // Get the current date
            Date now = new Date();

            // Update the last review date
            securityReviewConfig.set("last-review", now.getTime());

            // Calculate and set the next review date
            Date nextReview = calculateNextReviewDate();
            securityReviewConfig.set("next-review", nextReview.getTime());

            // Add the review to the list of completed reviews
            String reviewPath = "reviews." + now.getTime();
            securityReviewConfig.set(reviewPath + ".date", now.getTime());
            securityReviewConfig.set(reviewPath + ".reviewer", reviewer);
            securityReviewConfig.set(reviewPath + ".notes", notes);

            // Save the configuration
            saveConfig();

            // Log the completion
            plugin.getLogger().info("Security review completed by " + reviewer + ". Next review due: " +
                    getFormattedDate(nextReview.getTime()));

            // Log to audit log
            auditLogger.logSensitiveOperation(null, "security review completed",
                    "Security review completed by " + reviewer + ". Next review due: " +
                    getFormattedDate(nextReview.getTime()));

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error completing security review", e);
            return false;
        }
    }

    /**
     * Calculates the date of the next security review based on the configured interval.
     *
     * @return The date of the next security review
     */
    private Date calculateNextReviewDate() {
        int intervalDays = securityReviewConfig.getInt("review-interval-days", DEFAULT_REVIEW_INTERVAL_DAYS);
        Date lastReview = new Date(securityReviewConfig.getLong("last-review", new Date().getTime()));

        // Calculate the next review date (last review + interval days)
        return new Date(lastReview.getTime() + (long) intervalDays * 24 * 60 * 60 * 1000);
    }

    /**
     * Gets the date of the last security review.
     *
     * @return The date of the last security review
     */
    public Date getLastReviewDate() {
        return new Date(securityReviewConfig.getLong("last-review"));
    }

    /**
     * Gets the date of the next scheduled security review.
     *
     * @return The date of the next scheduled security review
     */
    public Date getNextReviewDate() {
        return new Date(securityReviewConfig.getLong("next-review"));
    }

    /**
     * Gets the interval between security reviews in days.
     *
     * @return The interval between security reviews in days
     */
    public int getReviewIntervalDays() {
        return securityReviewConfig.getInt("review-interval-days", DEFAULT_REVIEW_INTERVAL_DAYS);
    }

    /**
     * Sets the interval between security reviews in days.
     *
     * @param days The interval between security reviews in days
     */
    public void setReviewIntervalDays(int days) {
        if (days < 1) {
            days = 1; // Minimum 1 day
        }

        securityReviewConfig.set("review-interval-days", days);

        // Recalculate the next review date based on the new interval
        Date lastReview = new Date(securityReviewConfig.getLong("last-review"));
        Date nextReview = new Date(lastReview.getTime() + (long) days * 24 * 60 * 60 * 1000);
        securityReviewConfig.set("next-review", nextReview.getTime());

        saveConfig();
    }

    /**
     * Formats a timestamp as a human-readable date string.
     *
     * @param timestamp The timestamp to format
     * @return A human-readable date string
     */
    private String getFormattedDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    /**
     * Saves the security review configuration to disk.
     */
    private void saveConfig() {
        try {
            securityReviewConfig.save(securityReviewFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save security_reviews.yml", e);
        }
    }
}
