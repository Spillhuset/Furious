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
 * Subcommand for relocating a guild home.
 */
public class HomesRelocateSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new HomesRelocateSubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesRelocateSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "relocate";
    }

    @Override
    public String getDescription() {
        return "Relocates a guild home to your current location.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guilds homes relocate <name> - Relocates a guild home to your current location", NamedTextColor.YELLOW));
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

        // Check if player has permission to relocate homes (admin or owner)
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN) && !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a guild admin or owner to relocate guild homes!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String homeName = args[2];

        // Check if the player is in a claimed chunk
        Chunk chunk = player.getLocation().getChunk();
        if (guild.isChunkUnClaimed(chunk)) {
            player.sendMessage(Component.text("You can only relocate guild homes within your guild's claimed chunks!", NamedTextColor.RED));
            return true;
        }

        // Check if the home exists
        if (plugin.getHomesManager().getGuildHome(guild.getId(), homeName) == null) {
            player.sendMessage(Component.text("Guild home '" + homeName + "' does not exist!", NamedTextColor.RED));
            return true;
        }

        // Delete the old home and set a new one at the current location
        plugin.getHomesManager().deleteGuildHome(guild.getId(), homeName);
        if (plugin.getHomesManager().setGuildHome(player, guild, homeName, player.getLocation())) {
            player.sendMessage(Component.text("Guild home '" + homeName + "' relocated!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to relocate guild home! This world may be disabled for homes.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (sender instanceof Player player) {
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            if (guild != null) {
                if (args.length == 3) {
                    // Suggest existing home names
                    String partial = args[2].toLowerCase();
                    plugin.getHomesManager().getGuildHomes(guild.getId()).forEach(home -> {
                        if (home.getName().toLowerCase().startsWith(partial)) {
                            completions.add(home.getName());
                        }
                    });
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.homes.relocate";
    }
}