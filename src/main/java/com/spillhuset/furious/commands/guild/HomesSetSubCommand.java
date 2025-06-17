package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for setting a guild home.
 */
public class HomesSetSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new HomesSetSubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesSetSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getDescription() {
        return "Sets a guild home at your current location.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guilds homes set <name> - Sets a guild home at your current location", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Get the player's guild
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return true;
        }

        // Check if player has permission to set homes (admin or owner)
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN) && !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a guild admin or owner to set guild homes!", NamedTextColor.RED));
            return true;
        }

        // Check if the player is in a claimed chunk
        Chunk chunk = player.getLocation().getChunk();
        if (!guild.isChunkClaimed(chunk)) {
            player.sendMessage(Component.text("You can only set guild homes within your guild's claimed chunks!", NamedTextColor.RED));
            return true;
        }

        // Get the home name
        String homeName = args.length >= 3 ? args[2] : "default";

        // Set the guild home
        if (plugin.getHomesManager().setGuildHome(player, guild, homeName, player.getLocation())) {
            player.sendMessage(Component.text("Guild home '" + homeName + "' set!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to set guild home! Your guild may have reached its home limit or this world is disabled for homes.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 3 && sender instanceof Player player) {
            // Suggest existing home names
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                String partial = args[2].toLowerCase();
                plugin.getHomesManager().getGuildHomes(guild.getId()).forEach(home -> {
                    if (home.getName().startsWith(partial)) {
                        completions.add(home.getName());
                    }
                });

                // Always suggest "default" if it matches
                if ("default".startsWith(partial) && completions.stream().noneMatch(s -> s.equals("default"))) {
                    completions.add("default");
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.homes.set";
    }
}