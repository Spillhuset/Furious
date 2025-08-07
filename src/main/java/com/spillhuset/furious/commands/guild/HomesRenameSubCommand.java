package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for renaming a guild home.
 */
public class HomesRenameSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new HomesRenameSubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesRenameSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "rename";
    }

    @Override
    public String getDescription() {
        return "Renames a guild home.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guilds homes rename <old-name> <new-name> - Renames a guild home", NamedTextColor.YELLOW));
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

        // Check if player has permission to rename homes (admin or owner)
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN) && !guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You must be a guild admin or owner to rename guild homes!", NamedTextColor.RED));
            return true;
        }

        // Check if enough arguments
        if (args.length < 4) {
            getUsage(sender);
            return true;
        }

        String oldName = args[2];
        String newName = args[3];

        // Rename the guild home
        if (plugin.getHomesManager().renameGuildHome(guild.getId(), oldName, newName)) {
            player.sendMessage(Component.text("Guild home renamed from '" + oldName + "' to '" + newName + "'!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to rename guild home! Make sure the old name exists and the new name is not already taken.", NamedTextColor.RED));
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
                    // Suggest existing home names for the old name
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
        return "furious.guild.homes.rename";
    }
}