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
 * Subcommand for inviting a player to a guild.
 */
public class InviteSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new InviteSubCommand.
     *
     * @param plugin The plugin instance
     */
    public InviteSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "invite";
    }

    @Override
    public String getDescription() {
        return "Invites a player to your guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild invite <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Invites the specified player to your guild.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 2) {
            getUsage(sender);
            return true;
        }

        // Get the guild
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

        // Get the target player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return true;
        }

        // Check if target is already in a guild
        if (plugin.getGuildManager().isInGuild(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is already in a guild!", NamedTextColor.RED));
            return true;
        }

        // Check if target is already invited
        if (guild.isInvited(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is already invited to your guild!", NamedTextColor.RED));
            return true;
        }

        // Invite the player
        if (plugin.getGuildManager().invitePlayerToGuild(guild, target.getUniqueId())) {
            player.sendMessage(Component.text("Invited " + target.getName() + " to your guild!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("You have been invited to join " + guild.getName() + "!", NamedTextColor.GREEN)
                    .append(Component.newline())
                    .append(Component.text("Use /guild join " + guild.getName() + " to accept.", NamedTextColor.YELLOW)));
        } else {
            player.sendMessage(Component.text("Failed to invite " + target.getName() + " to your guild!", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.invite";
    }

    @Override
    public GuildRole getRequiredRole() {
        return GuildRole.ADMIN;
    }

    @Override
    public Guild isInGuild(@NotNull Player player) {
        return plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
    }

    @Override
    public Guild isInGuild(@NotNull UUID playerUUID) {
        return plugin.getGuildManager().getPlayerGuild(playerUUID);
    }

    @Override
    public boolean isGuildOwner(@NotNull Player player) {
        Guild guild = isInGuild(player);
        if (guild == null) {
            return false;
        }
        return guild.getOwner().equals(player.getUniqueId());
    }

    @Override
    public boolean isGuildOwner(@NotNull UUID playerUUID) {
        Guild guild = isInGuild(playerUUID);
        if (guild == null) {
            return false;
        }
        return guild.getOwner().equals(playerUUID);
    }

    @Override
    public boolean hasRole(@NotNull Player player, @NotNull GuildRole role) {
        Guild guild = isInGuild(player);
        if (guild == null) {
            return false;
        }

        // Get the player's guild and check their role
        return guild.hasRole(player.getUniqueId(), role);
    }
}
