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
 * Subcommand for claiming a chunk for a guild.
 */
public class ClaimSubCommand implements SubCommand {
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
        return "Claims the current chunk for your guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild claim", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Claims the chunk you are standing in for your guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You must be the guild owner to claim chunks.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Your guild can claim up to " + plugin.getConfig().getInt("guilds.max-plots-per-guild", 16) + " chunks.", NamedTextColor.YELLOW));
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

        // Check if player has permission to claim chunks (owner or admin)
        if (!guild.hasRole(player.getUniqueId(), GuildRole.ADMIN)) {
            player.sendMessage(Component.text("You need to be a guild admin or owner to claim chunks!", NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        plugin.getGuildManager().claimChunk(guild, player.getLocation().getChunk(), player);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for claiming chunks
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.claim";
    }
}
