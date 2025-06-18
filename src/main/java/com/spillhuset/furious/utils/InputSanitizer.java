package com.spillhuset.furious.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for sanitizing and validating user input in commands
 */
public class InputSanitizer {

    // Pattern for valid Minecraft usernames (3-16 characters, alphanumeric and underscore)
    private static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    // Pattern for valid guild names (3-32 characters, alphanumeric, underscore, and spaces)
    private static final Pattern VALID_GUILD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_ ]{3,32}$");

    // Pattern for valid world names (1-64 characters, alphanumeric, underscore, dash, and period)
    private static final Pattern VALID_WORLD_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.]{1,64}$");

    // Pattern for valid coordinates (integer or decimal number, can be negative)
    private static final Pattern VALID_COORDINATE_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    // Valid Minecraft selectors
    public static final List<String> VALID_SELECTORS = List.of("@a", "@p", "@r", "@s", "@e");

    /**
     * Sanitizes a player name by checking if it matches the valid username pattern
     *
     * @param name The player name to sanitize
     * @return The sanitized player name, or null if invalid
     */
    public static String sanitizePlayerName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Check if it's a valid selector
        if (isValidSelector(name)) {
            return name;
        }

        // Check if it matches the valid username pattern
        if (VALID_USERNAME_PATTERN.matcher(name).matches()) {
            return name;
        }

        // If the name doesn't match the pattern but a player with that name exists,
        // return the correct case of the name
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            return player.getName();
        }

        return null;
    }

    /**
     * Checks if a string is a valid Minecraft selector
     *
     * @param selector The selector to check
     * @return True if the selector is valid, false otherwise
     */
    public static boolean isValidSelector(String selector) {
        return selector != null && VALID_SELECTORS.contains(selector);
    }

    /**
     * Sanitizes a list of player names or selectors
     *
     * @param args The arguments to sanitize
     * @return A list of sanitized player names or selectors
     */
    public static List<String> sanitizePlayerNames(String[] args) {
        List<String> sanitizedNames = new ArrayList<>();

        for (String arg : args) {
            String sanitized = sanitizePlayerName(arg);
            if (sanitized != null) {
                sanitizedNames.add(sanitized);
            }
        }

        return sanitizedNames;
    }

    /**
     * Sanitizes a guild name by checking if it matches the valid guild name pattern
     *
     * @param name The guild name to sanitize
     * @return The sanitized guild name, or null if invalid
     */
    public static String sanitizeGuildName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Check if it matches the valid guild name pattern
        if (VALID_GUILD_NAME_PATTERN.matcher(name).matches() && isSafeInput(name)) {
            return name;
        }

        return null;
    }

    /**
     * Sanitizes a world name by checking if it matches the valid world name pattern
     *
     * @param name The world name to sanitize
     * @return The sanitized world name, or null if invalid
     */
    public static String sanitizeWorldName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // Check if it matches the valid world name pattern
        if (VALID_WORLD_NAME_PATTERN.matcher(name).matches() && isSafeInput(name)) {
            return name;
        }

        return null;
    }

    /**
     * Sanitizes a coordinate by checking if it matches the valid coordinate pattern
     *
     * @param coordinate The coordinate to sanitize
     * @return The sanitized coordinate as a double, or null if invalid
     */
    public static Double sanitizeCoordinate(String coordinate) {
        if (coordinate == null || coordinate.isEmpty()) {
            return null;
        }

        // Check if it matches the valid coordinate pattern
        if (VALID_COORDINATE_PATTERN.matcher(coordinate).matches()) {
            try {
                return Double.parseDouble(coordinate);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Checks if a string contains potentially dangerous characters or SQL injection attempts
     *
     * @param input The input to check
     * @return True if the input is safe, false otherwise
     */
    public static boolean isSafeInput(String input) {
        if (input == null) {
            return false;
        }

        // Check for SQL injection attempts
        String lowerInput = input.toLowerCase();
        if (lowerInput.contains("select ") ||
            lowerInput.contains("insert ") ||
            lowerInput.contains("update ") ||
            lowerInput.contains("delete ") ||
            lowerInput.contains("drop ") ||
            lowerInput.contains("union ") ||
            lowerInput.contains(";") ||
            lowerInput.contains("--") ||
            lowerInput.contains("/*") ||
            lowerInput.contains("*/")) {
            return false;
        }

        // Check for potentially dangerous characters
        if (input.contains("\0") || // Null byte
            input.contains("../") || // Directory traversal
            input.contains("..\\")) { // Directory traversal (Windows)
            return false;
        }

        return true;
    }
}
