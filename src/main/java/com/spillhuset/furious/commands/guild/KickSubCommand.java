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
 * Subcommand for kicking a player from a guild.
 */
public class KickSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new KickSubCommand.
     *
     * @param plugin The plugin instance
     */
    public KickSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kicks a player from your guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild kick <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Kicks the specified player from your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("The player must be a member of your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You cannot kick yourself or other admins.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        Player player = (Player) sender;
        Guild guild = isInGuild(player);

        // Get target player
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return true;
        }

        UUID targetId = targetPlayer.getUniqueId();

        // Check if target is in the guild
        if (!guild.isMember(targetId)) {
            sender.sendMessage(Component.text(targetName + " is not a member of your guild!", NamedTextColor.RED));
            return true;
        }

        // Check if target is the owner
        if (targetId.equals(guild.getOwner())) {
            sender.sendMessage(Component.text("You cannot kick the guild owner!", NamedTextColor.RED));
            return true;
        }

        // Check if target is the player themselves
        if (targetId.equals(player.getUniqueId())) {
            sender.sendMessage(Component.text("You cannot kick yourself! Use /guild leave instead.", NamedTextColor.RED));
            return true;
        }

        // Check if target is an admin (only owner can kick admins)
        if (guild.hasRole(targetId, GuildRole.ADMIN) && !player.getUniqueId().equals(guild.getOwner())) {
            sender.sendMessage(Component.text("Only the guild owner can kick admins!", NamedTextColor.RED));
            return true;
        }

        // Kick the player
        if (plugin.getGuildManager().adminKickPlayer(guild, targetId, player)) {
            // Notifications are handled in the adminKickPlayer method
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to kick " + targetName + " from your guild.", NamedTextColor.RED));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        Guild guild = isInGuild(player);
        if (guild == null) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            String partial = args[1].toLowerCase();

            // Add guild members except the player themselves
            for (UUID memberId : guild.getMembers()) {
                if (!memberId.equals(player.getUniqueId())) {
                    // If player is not the owner, don't suggest admins
                    if (!player.getUniqueId().equals(guild.getOwner()) && guild.hasRole(memberId, GuildRole.ADMIN)) {
                        continue;
                    }

                    // Don't suggest the owner
                    if (memberId.equals(guild.getOwner())) {
                        continue;
                    }

                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null && member.getName().toLowerCase().startsWith(partial)) {
                        completions.add(member.getName());
                    }
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.kick";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be at least an admin
        return GuildRole.ADMIN;
    }
}