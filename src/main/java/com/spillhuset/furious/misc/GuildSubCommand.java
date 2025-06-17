package com.spillhuset.furious.misc;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Interface for guild-specific subcommands.
 * Extends SubCommand with additional methods for guild-specific checks.
 */
public interface GuildSubCommand extends SubCommand {

    /**
     * Gets the required role to execute this command.
     *
     * @return the required role, or null if no specific role is required
     */
    GuildRole getRequiredRole();

    /**
     * Checks if the player needs to be the owner to execute this command.
     *
     * @return true if the player needs to be the owner, false otherwise
     */
    default boolean requiresOwner() {
        return getRequiredRole() == GuildRole.OWNER;
    }

    /**
     * Checks if a player has the required role for this command.
     *
     * @param player The player to check
     * @return true if the player has the required role, false otherwise
     */
    default boolean hasRequiredRole(@NotNull Player player) {
        GuildRole requiredRole = getRequiredRole();
        if (requiredRole == null) {
            return true; // No role required
        }

        // Check if player is in a guild
        if (isInGuild(player) == null) {
            return false;
        }

        // Check for specific roles
        return switch (requiredRole) {
            case OWNER -> isGuildOwner(player);
            case ADMIN -> isGuildOwner(player) || hasRole(player, GuildRole.ADMIN);
            case MOD -> isGuildOwner(player) || hasRole(player, GuildRole.ADMIN) || hasRole(player, GuildRole.MOD);
            case USER -> true; // All guild members are at least users
            default -> false;
        };
    }

    /**
     * Checks if a player has a specific role in their guild.
     *
     * @param player The player to check
     * @param role   The role to check for
     * @return true if the player has the specified role, false otherwise
     */
    default boolean hasRole(@NotNull Player player, @NotNull GuildRole role) {
        Guild guild = isInGuild(player);
        return guild != null && guild.hasRole(player.getUniqueId(), role);
    }

    /**
     * Checks if a player is in a guild.
     *
     * @param player The player to check
     * @return true if the player is in a guild, false otherwise
     */
    default Guild isInGuild(@NotNull Player player) {
        return isInGuild(player.getUniqueId());
    }

    /**
     * Checks if a player is in a guild by UUID.
     *
     * @param playerUUID The UUID of the player to check
     * @return true if the player is in a guild, false otherwise
     */
    default Guild isInGuild(@NotNull UUID playerUUID) {
        return Furious.getInstance().getGuildManager().getPlayerGuild(playerUUID);
    }

    /**
     * Checks if a player is the owner of their guild.
     *
     * @param player The player to check
     * @return true if the player is the owner of their guild, false otherwise
     */
    default boolean isGuildOwner(@NotNull Player player) {
        return isGuildOwner(player.getUniqueId());
    }

    /**
     * Checks if a player is the owner of their guild by UUID.
     *
     * @param playerUUID The UUID of the player to check
     * @return true if the player is the owner of their guild, false otherwise
     */
    default boolean isGuildOwner(@NotNull UUID playerUUID) {
        Guild guild = isInGuild(playerUUID);
        return guild != null && guild.getOwner().equals(playerUUID);
    }

    /**
     * Checks if a command sender can execute this command based on guild-specific permissions.
     * This extends the regular permission check with guild-specific checks.
     *
     * @param sender The command sender
     * @return true if the sender can execute this command, false otherwise
     */
    default boolean checkGuildPermission(@NotNull CommandSender sender) {
        return checkGuildPermission(sender, true);
    }

    /**
     * Checks if a command sender can execute this command based on guild-specific permissions.
     * This extends the regular permission check with guild-specific checks.
     *
     * @param sender   The command sender
     * @param feedback Whether to send feedback messages to the sender
     * @return true if the sender can execute this command, false otherwise
     */
    default boolean checkGuildPermission(@NotNull CommandSender sender, boolean feedback) {
        // First check regular permissions
        if (!checkPermission(sender, feedback)) {
            return false;
        }

        // If not a player, they can't have guild roles
        if (!(sender instanceof Player player)) {
            if (feedback) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if player has the required role
        GuildRole requiredRole = getRequiredRole();
        if (requiredRole != null) {
            // Check if player is in a guild
            if (isInGuild(player) == null) {
                if (feedback) {
                    sender.sendMessage(Component.text("You must be in a guild to use this command!", NamedTextColor.RED));
                }
                return false;
            }

            // Check if player has the required role
            if (!hasRequiredRole(player)) {
                if (feedback) {
                    sender.sendMessage(Component.text("You must be " +
                            (requiredRole == GuildRole.OWNER ? "the guild owner" :
                                    requiredRole == GuildRole.ADMIN ? "a guild admin or owner" :
                                            requiredRole == GuildRole.MOD ? "a guild mod, admin, or owner" :
                                                    "a guild member") +
                            " to use this command!", NamedTextColor.RED));
                }
                return false;
            }
        }

        return true;
    }
}
