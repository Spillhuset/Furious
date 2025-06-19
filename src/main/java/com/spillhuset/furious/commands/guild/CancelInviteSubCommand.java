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
 * Subcommand for canceling a guild invitation.
 */
public class CancelInviteSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new CancelInviteSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CancelInviteSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "cancelinvite";
    }

    @Override
    public String getDescription() {
        return "Cancels an invitation to your guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild cancelinvite <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Cancels the invitation for the specified player to join your guild.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length != 2) {
            getUsage(sender);
            return true;
        }

        Player player = (Player) sender;
        Guild guild = isInGuild(player);

        // Get the target player
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            // Try to find by UUID
            try {
                UUID targetId = UUID.fromString(targetName);
                return cancelInvitation(player, guild, targetId);
            } catch (IllegalArgumentException e) {
                // Not a valid UUID
                sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
                return true;
            }
        }

        // Check if the player is invited
        if (!guild.isInvited(targetPlayer.getUniqueId())) {
            sender.sendMessage(Component.text(targetPlayer.getName() + " is not invited to your guild!", NamedTextColor.RED));
            return true;
        }

        // Cancel the invitation
        return cancelInvitation(player, guild, targetPlayer.getUniqueId());
    }

    /**
     * Cancels an invitation for a player.
     *
     * @param player The player canceling the invitation
     * @param guild The guild
     * @param targetId The UUID of the player whose invitation to cancel
     * @return true if the invitation was canceled, false otherwise
     */
    private boolean cancelInvitation(Player player, Guild guild, UUID targetId) {
        // Get the player's name if online
        Player targetPlayer = Bukkit.getPlayer(targetId);
        String targetName = targetPlayer != null ? targetPlayer.getName() : targetId.toString();

        // Check if the player is invited
        if (!guild.isInvited(targetId)) {
            player.sendMessage(Component.text(targetName + " is not invited to your guild!", NamedTextColor.RED));
            return true;
        }

        // Cancel the invitation
        if (plugin.getGuildManager().removeInvitation(guild, targetId)) {
            player.sendMessage(Component.text("Canceled the invitation for " + targetName + " to join your guild.", NamedTextColor.GREEN));

            // Notify the target player if they're online
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(Component.text("Your invitation to join " + guild.getName() + " has been canceled.", NamedTextColor.YELLOW));
            }

            return true;
        } else {
            player.sendMessage(Component.text("Failed to cancel the invitation for " + targetName + ".", NamedTextColor.RED));
            return false;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        Guild guild = isInGuild(player);
        if (guild == null) {
            return completions;
        }

        if (args.length == 2) {
            String partial = args[1].toLowerCase();

            // Add players who have been invited to the guild
            for (UUID invitedId : guild.getInvites()) {
                Player invited = Bukkit.getPlayer(invitedId);
                if (invited != null && invited.getName().toLowerCase().startsWith(partial)) {
                    completions.add(invited.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.cancelinvite";
    }

    @Override
    public GuildRole getRequiredRole() {
        return GuildRole.ADMIN;
    }
}
