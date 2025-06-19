package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for claiming a chunk for a guild.
 */
public class ClaimSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new ClaimSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ClaimSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Claims the current chunk for your guild or an unmanned guild (op only).";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild claim", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Claims the chunk you are standing in for your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You must be the guild owner to claim chunks.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Your guild can claim up to " + plugin.getConfig().getInt("guilds.max-plots-per-guild", 16) + " chunks.", NamedTextColor.YELLOW));

        if (sender.isOp() || sender.hasPermission("furious.guild.claim.unmanned")) {
            sender.sendMessage(Component.text("/guild claim <guild>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Claims the chunk you are standing in for the specified unmanned guild (SAFE, WAR, WILD).", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("This command is only available to server operators.", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if this is a claim for an unmanned guild
        if (args.length == 2) {
            // Only ops can claim for unmanned guilds
            if (!player.isOp() && !player.hasPermission("furious.guild.claim.unmanned")) {
                player.sendMessage(Component.text("You don't have permission to claim chunks for unmanned guilds!", NamedTextColor.RED));
                return true;
            }

            String guildName = args[1].toUpperCase();
            Guild unmannedGuild = null;

            // Get the appropriate unmanned guild
            switch (guildName) {
                case "SAFE":
                    unmannedGuild = plugin.getGuildManager().getSafeGuild();
                    break;
                case "WAR":
                    unmannedGuild = plugin.getGuildManager().getWarGuild();
                    break;
                case "WILD":
                    unmannedGuild = plugin.getGuildManager().getWildGuild();
                    break;
                default:
                    player.sendMessage(Component.text("Unknown unmanned guild: " + guildName, NamedTextColor.RED));
                    player.sendMessage(Component.text("Valid unmanned guilds are: SAFE, WAR, WILD", NamedTextColor.RED));
                    return true;
            }

            // Claim the chunk for the unmanned guild using admin claim method
            plugin.getGuildManager().adminClaimChunk(unmannedGuild, player.getLocation().getChunk(), player);
            return true;
        } else if (args.length != 1) {
            getUsage(sender);
            return true;
        }

        // Regular claim for player's guild
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

        // Check if player is in a guild
        if (guild == null) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        plugin.getGuildManager().claimChunk(guild, player.getLocation().getChunk(), player);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Provide unmanned guild completions for ops and admins
        if ((sender.isOp() || sender.hasPermission("furious.guild.claim.unmanned") || sender.hasPermission("furious.guild.admin")) && args.length == 2) {
            String partial = args[1].toUpperCase();
            List<String> unmannedGuilds = List.of("SAFE", "WAR", "WILD");

            // Filter based on what the player has typed so far
            for (String guild : unmannedGuilds) {
                if (guild.startsWith(partial)) {
                    completions.add(guild);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.claim";
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
