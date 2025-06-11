package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.misc.SubCommand;
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
public class MobsSubCommand implements SubCommand {
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

        // Check if player is the owner
        if (!guild.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("Only the guild owner can control mob spawning!", NamedTextColor.RED));
            return true;
        }

        // Check if guild has any claimed chunks
        if (guild.getClaimedChunkCount() == 0) {
            player.sendMessage(Component.text("Your guild has not claimed any chunks yet!", NamedTextColor.RED));
            return true;
        }

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
}