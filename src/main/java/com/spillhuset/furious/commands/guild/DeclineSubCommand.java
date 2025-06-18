package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for declining guild join requests or invitations.
 */
public class DeclineSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new DeclineSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeclineSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "decline";
    }

    @Override
    public String getDescription() {
        return "Declines a guild join request or invitation.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild decline [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Declines a join request from a player (if you're a guild admin/owner).", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild decline [guild]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Declines an invitation to join a guild (if you've been invited).", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkPermission(sender)) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player is in a guild
        Guild playerGuild = isInGuild(player);

        if (playerGuild != null) {
            // Player is in a guild, so they're declining a join request
            return handleDeclineJoinRequest(player, playerGuild, args);
        } else {
            // Player is not in a guild, so they're declining an invitation
            return handleDeclineInvitation(player, args);
        }
    }

    /**
     * Handles declining a join request from a player.
     *
     * @param player The player declining the request
     * @param guild The player's guild
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleDeclineJoinRequest(Player player, Guild guild, String[] args) {
        // Check if player has permission to decline join requests
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN) && !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a guild admin or owner to decline join requests!", NamedTextColor.RED));
            return true;
        }

        // If no player specified and there's only one request, decline it
        if (args.length == 1) {
            if (guild.getJoinRequests().isEmpty()) {
                player.sendMessage(Component.text("There are no pending join requests for your guild.", NamedTextColor.RED));
                return true;
            } else if (guild.getJoinRequests().size() == 1) {
                UUID requesterId = guild.getJoinRequests().iterator().next();
                return declineJoinRequest(player, guild, requesterId);
            } else {
                player.sendMessage(Component.text("There are multiple pending join requests. Please specify a player name:", NamedTextColor.YELLOW));
                for (UUID requesterId : guild.getJoinRequests()) {
                    String requesterName = Bukkit.getOfflinePlayer(requesterId).getName();
                    if (requesterName != null) {
                        player.sendMessage(Component.text("- " + requesterName, NamedTextColor.YELLOW));
                    }
                }
                return true;
            }
        }

        // Find the player by name
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer != null) {
            // Player is online
            if (guild.hasJoinRequest(targetPlayer.getUniqueId())) {
                return declineJoinRequest(player, guild, targetPlayer.getUniqueId());
            }
        } else {
            // Try to find offline player
            for (UUID requesterId : guild.getJoinRequests()) {
                String requesterName = Bukkit.getOfflinePlayer(requesterId).getName();
                if (requesterName != null && requesterName.equalsIgnoreCase(targetName)) {
                    return declineJoinRequest(player, guild, requesterId);
                }
            }
        }

        player.sendMessage(Component.text("No join request found from player: " + targetName, NamedTextColor.RED));
        return true;
    }

    /**
     * Declines a join request from a player.
     *
     * @param player The player declining the request
     * @param guild The guild
     * @param requesterId The UUID of the player who requested to join
     * @return true if the request was declined, false otherwise
     */
    private boolean declineJoinRequest(Player player, Guild guild, UUID requesterId) {
        return plugin.getGuildManager().declineJoinRequest(guild, requesterId, player);
    }

    /**
     * Handles declining an invitation to join a guild.
     *
     * @param player The player declining the invitation
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleDeclineInvitation(Player player, String[] args) {
        // If no guild specified and there's only one invitation, decline it
        List<Guild> invitingGuilds = new ArrayList<>();
        for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
            if (guild.isInvited(player.getUniqueId())) {
                invitingGuilds.add(guild);
            }
        }

        if (invitingGuilds.isEmpty()) {
            player.sendMessage(Component.text("You have not been invited to any guilds.", NamedTextColor.RED));
            return true;
        } else if (invitingGuilds.size() == 1 && args.length == 1) {
            Guild guild = invitingGuilds.get(0);
            return declineInvitation(player, guild);
        } else if (args.length == 1) {
            player.sendMessage(Component.text("You have been invited to multiple guilds. Please specify a guild name:", NamedTextColor.YELLOW));
            for (Guild guild : invitingGuilds) {
                player.sendMessage(Component.text("- " + guild.getName(), NamedTextColor.YELLOW));
            }
            return true;
        }

        // Find the guild by name
        String guildName = args[1];
        Guild targetGuild = plugin.getGuildManager().getGuildByName(guildName);

        if (targetGuild != null && targetGuild.isInvited(player.getUniqueId())) {
            return declineInvitation(player, targetGuild);
        }

        player.sendMessage(Component.text("You have not been invited to a guild named: " + guildName, NamedTextColor.RED));
        return true;
    }

    /**
     * Declines an invitation to join a guild.
     *
     * @param player The player declining the invitation
     * @param guild The guild
     * @return true if the invitation was declined, false otherwise
     */
    private boolean declineInvitation(Player player, Guild guild) {
        return plugin.getGuildManager().declineInvitation(guild, player.getUniqueId(), player);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }
        if (args.length == 2) {
            Guild playerGuild = isInGuild(player);
            List<String> completions = new ArrayList<>();

            if (playerGuild != null) {
                // Player is in a guild, suggest players who have requested to join
                for (UUID requesterId : playerGuild.getJoinRequests()) {
                    String requesterName = Bukkit.getOfflinePlayer(requesterId).getName();
                    if (requesterName != null) {
                        completions.add(requesterName);
                    }
                }
            } else {
                // Player is not in a guild, suggest guilds that have invited them
                for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                    if (guild.isInvited(player.getUniqueId())) {
                        completions.add(guild.getName());
                    }
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.decline";
    }

    @Override
    public GuildRole getRequiredRole() {
        // No specific role required, as this command can be used by non-guild members too
        return null;
    }
}
