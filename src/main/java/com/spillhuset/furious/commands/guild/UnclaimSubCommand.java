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
 * Subcommand for unclaiming a chunk from a guild.
 */
public class UnclaimSubCommand implements SubCommand {
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
        return "Unclaims the current chunk from your guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild unclaim", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Unclaims the chunk you are standing in from your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You must be the guild owner to unclaim chunks.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            getUsage(sender);
            return true;
        }

        // Check if player is in a guild
        if (!plugin.getGuildManager().isInGuild(player.getUniqueId())) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return true;
        }

        // Get the guild
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

        // Check if player has permission to unclaim chunks (owner or admin)
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN)) {
            player.sendMessage(Component.text("You need to be a guild admin or owner to unclaim chunks!", NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        plugin.getGuildManager().unclaimChunk(guild, player.getLocation().getChunk(), player);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for unclaiming chunks
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.unclaim";
    }
}
