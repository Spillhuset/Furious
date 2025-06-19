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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for accepting guild join requests or invitations.
 */
public class AcceptSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new AcceptSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AcceptSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "accept";
    }

    @Override
    public String getDescription() {
        return "Accepts a guild join request or invitation.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild accept [player]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Accepts a join request from a player (if you're a guild admin/owner).", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild accept [guild]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Accepts an invitation to join a guild (if you've been invited).", NamedTextColor.YELLOW));
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
            // Player is in a guild, so they're accepting a join request
            return handleAcceptJoinRequest(player, playerGuild, args);
        } else {
            // Player is not in a guild, so they're accepting an invitation
            return handleAcceptInvitation(player, args);
        }
    }

    /**
     * Handles accepting a join request from a player.
     *
     * @param player The player accepting the request
     * @param guild The player's guild
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleAcceptJoinRequest(Player player, Guild guild, String[] args) {
        // Check if player has permission to accept join requests
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN) && !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a guild admin or owner to accept join requests!", NamedTextColor.RED));
            return true;
        }

        // If no player specified and there's only one request, accept it
        if (args.length == 1) {
            if (guild.getJoinRequests().isEmpty()) {
                player.sendMessage(Component.text("There are no pending join requests for your guild.", NamedTextColor.RED));
                return true;
            } else if (guild.getJoinRequests().size() == 1) {
                UUID requesterId = guild.getJoinRequests().iterator().next();
                return acceptJoinRequest(player, guild, requesterId);
            } else {
                player.sendMessage(Component.text("There are multiple pending join requests. Please specify a player name:", NamedTextColor.YELLOW));
                for (UUID requesterId : guild.getJoinRequests()) {
                    Player requester = Bukkit.getPlayer(requesterId);
                    String requesterName;
                    if (requester != null) {
                        requesterName = requester.getName();
                    } else {
                        // Use UUID string representation for offline players to avoid blocking network calls
                        requesterName = requesterId.toString();
                    }
                    player.sendMessage(Component.text("- " + requesterName, NamedTextColor.YELLOW));
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
                return acceptJoinRequest(player, guild, targetPlayer.getUniqueId());
            }
        } else {
            // Try to find offline player by UUID string representation
            for (UUID requesterId : guild.getJoinRequests()) {
                // Use UUID string representation for offline players to avoid blocking network calls
                String requesterUUID = requesterId.toString();
                if (requesterUUID.equalsIgnoreCase(targetName)) {
                    return acceptJoinRequest(player, guild, requesterId);
                }
            }
        }

        player.sendMessage(Component.text("No join request found from player: " + targetName, NamedTextColor.RED));
        return true;
    }

    /**
     * Accepts a join request from a player.
     *
     * @param player The player accepting the request
     * @param guild The guild
     * @param requesterId The UUID of the player who requested to join
     * @return true if the request was accepted, false otherwise
     */
    private boolean acceptJoinRequest(Player player, Guild guild, UUID requesterId) {
        return plugin.getGuildManager().acceptJoinRequest(guild, requesterId, player);
    }

    /**
     * Handles accepting an invitation to join a guild.
     *
     * @param player The player accepting the invitation
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleAcceptInvitation(Player player, String[] args) {
        // Check if player is already in a guild
        if (plugin.getGuildManager().isInGuild(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a guild!", NamedTextColor.RED));
            return true;
        }

        // If no guild specified and there's only one invitation, accept it
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
            return acceptInvitation(player, guild);
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
            return acceptInvitation(player, targetGuild);
        }

        player.sendMessage(Component.text("You have not been invited to a guild named: " + guildName, NamedTextColor.RED));
        return true;
    }

    /**
     * Accepts an invitation to join a guild.
     *
     * @param player The player accepting the invitation
     * @param guild The guild
     * @return true if the invitation was accepted, false otherwise
     */
    private boolean acceptInvitation(Player player, Guild guild) {
        // Remove the invitation
        guild.removeInvite(player.getUniqueId());

        // Add player to guild
        if (plugin.getGuildManager().addPlayerToGuild(guild, player.getUniqueId())) {
            player.sendMessage(Component.text("You have joined " + guild.getName() + "!", NamedTextColor.GREEN));

            // Notify online guild members
            for (Player member : guild.getOnlineMembers()) {
                if (!member.equals(player)) {
                    member.sendMessage(Component.text(player.getName() + " has joined the guild!", NamedTextColor.GREEN));
                }
            }

            return true;
        }

        player.sendMessage(Component.text("Failed to join the guild. You may already be in another guild.", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Check permissions first
        if (!checkGuildPermission(sender, false)) {
            return completions;
        }

        if (!(sender instanceof Player player)) {
            return completions;
        }

        if (args.length == 2) {
            Guild playerGuild = isInGuild(player);

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
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.accept";
    }

    @Override
    public boolean denyOp() {
        return true;
    }

    @Override
    public GuildRole getRequiredRole() {
        // No specific role required, as this command can be used by non-guild members too
        return null;
    }
}
