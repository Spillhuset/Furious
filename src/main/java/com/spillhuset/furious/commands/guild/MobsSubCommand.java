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

/**
 * Subcommand for controlling mob spawning in guild claimed chunks.
 */
public class MobsSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new MobsSubCommand.
     *
     * @param plugin The plugin instance
     */
    public MobsSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "mobs";
    }

    @Override
    public String getDescription() {
        return "Controls mob spawning in guild claimed chunks.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild mobs", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Toggles mob spawning in your guild's claimed chunks.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("By default, mob spawning is disabled in guild claimed chunks.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("You must be the guild owner to control mob spawning.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!checkGuildPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            getUsage(sender);
            return true;
        }

        Player player = (Player) sender;

        // Get the guild
        Guild guild = isInGuild(player);

        // Toggle mob spawning
        plugin.getGuildManager().toggleMobSpawning(guild, player);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for mob spawning control
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.mobs";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be the guild owner
        return GuildRole.OWNER;
    }

    @Override
    public boolean checkGuildPermission(@NotNull CommandSender sender, boolean feedback) {
        // First check regular permissions
        if (!checkPermission(sender, feedback)) {
            return false;
        }

        // If not a player, they can't control mob spawning
        if (!(sender instanceof Player player)) {
            if (feedback) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if player is in a guild
        Guild guild = isInGuild(player);
        if (guild == null) {
            if (feedback) {
                sender.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if player is the owner
        if (!isGuildOwner(player)) {
            if (feedback) {
                sender.sendMessage(Component.text("Only the guild owner can control mob spawning!", NamedTextColor.RED));
            }
            return false;
        }

        // Check if guild has any claimed chunks
        if (guild.getClaimedChunkCount() == 0) {
            if (feedback) {
                sender.sendMessage(Component.text("Your guild has not claimed any chunks yet!", NamedTextColor.RED));
            }
            return false;
        }

        return true;
    }
}
