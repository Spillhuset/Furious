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
 * Subcommand for creating a new guild.
 */
public class CreateSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new CreateSubCommand.
     *
     * @param plugin The plugin instance
     */
    public CreateSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Creates a new guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild create <name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Creates a new guild with the specified name.", NamedTextColor.YELLOW));
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

        // Check if player is already in a guild
        if (isInGuild(player) != null) {
            sender.sendMessage(Component.text("You are already in a guild! Leave your current guild before creating a new one.", NamedTextColor.RED));
            return true;
        }

        String guildName = args[1];

        // Create the guild
        Guild guild = plugin.getGuildManager().createGuild(guildName, player);

        if (guild != null) {
            player.sendMessage(Component.text("Guild " + guild.getName() + " created successfully!", NamedTextColor.GREEN));
            return true;
        }

        // If we get here, guild creation failed (error message already sent by GuildManager)
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for guild creation
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.create";
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
    public boolean checkGuildPermission(@NotNull CommandSender sender, boolean feedback) {
        // First check regular permissions
        if (!checkPermission(sender, feedback)) {
            return false;
        }

        // For create command, we just need to check that the player is not already in a guild
        if (sender instanceof Player player) {
            if (isInGuild(player) != null) {
                if (feedback) {
                    sender.sendMessage(Component.text("You are already in a guild! Leave your current guild before creating a new one.", NamedTextColor.RED));
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public GuildRole getRequiredRole() {
        // Create command doesn't require a guild role since it's used to create a new guild
        return null;
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
