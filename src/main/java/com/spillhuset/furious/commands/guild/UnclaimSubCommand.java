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
 * Subcommand for unclaiming a chunk from a guild.
 */
public class UnclaimSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new UnclaimSubCommand.
     *
     * @param plugin The plugin instance
     */
    public UnclaimSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "unclaim";
    }

    @Override
    public String getDescription() {
        return "Unclaims the current chunk from your guild or an unmanned guild (op only).";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild unclaim", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Unclaims the chunk you are standing in from your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You must be the guild owner to unclaim chunks.", NamedTextColor.YELLOW));

        if (sender.isOp() || sender.hasPermission("furious.guild.unclaim.unmanned")) {
            sender.sendMessage(Component.text("/guild unclaim <guild>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Unclaims the chunk you are standing in from the specified unmanned guild (SAFE, WAR, WILD).", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("This command is only available to server operators.", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if this is an unclaim for an unmanned guild
        if (args.length == 2) {
            // Only ops can unclaim for unmanned guilds
            if (!player.isOp() && !player.hasPermission("furious.guild.unclaim.unmanned")) {
                player.sendMessage(Component.text("You don't have permission to unclaim chunks from unmanned guilds!", NamedTextColor.RED));
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

            // Unclaim the chunk from the unmanned guild
            plugin.getGuildManager().unclaimChunk(unmannedGuild, player.getLocation().getChunk(), player);
            return true;
        } else if (args.length != 1) {
            getUsage(sender);
            return true;
        }

        // Regular unclaim for player's guild
        // Check guild permissions
        if (!checkGuildPermission(sender)) {
            return true;
        }

        // Get the guild
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

        // Check if player is in a guild
        if (guild == null) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        plugin.getGuildManager().unclaimChunk(guild, player.getLocation().getChunk(), player);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Only provide unmanned guild completions for ops
        if ((sender.isOp() || sender.hasPermission("furious.guild.unclaim.unmanned")) && args.length == 2) {
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
        return "furious.guild.unclaim";
    }

    @Override
    public GuildRole getRequiredRole() {
        return GuildRole.ADMIN;
    }
}
