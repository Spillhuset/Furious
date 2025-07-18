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
 * Subcommand for leaving a guild.
 */
public class LeaveSubCommand implements GuildSubCommand {
    private final Furious plugin;

    /**
     * Creates a new LeaveSubCommand.
     *
     * @param plugin The plugin instance
     */
    public LeaveSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Leaves your current guild.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild leave", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Leaves your current guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Note: If you are the guild owner, you cannot leave. Use /guild disband instead.", NamedTextColor.YELLOW));
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
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());

        // Leave the guild
        if (plugin.getGuildManager().removePlayerFromGuild(player.getUniqueId())) {
            player.sendMessage(Component.text("You have left " + guild.getName() + "!", NamedTextColor.GREEN));

            // Notify online guild members
            for (Player member : guild.getOnlineMembers()) {
                member.sendMessage(Component.text(player.getName() + " has left the guild!", NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(Component.text("Failed to leave the guild!", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completion for leaving a guild
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.guild.leave";
    }

    @Override
    public GuildRole getRequiredRole() {
        // This command requires the player to be in a guild but doesn't require a specific role
        return GuildRole.USER;
    }

    @Override
    public boolean checkGuildPermission(@NotNull CommandSender sender, boolean feedback) {
        // First check regular permissions
        if (!checkPermission(sender, feedback)) {
            return false;
        }

        // If not a player, they can't leave a guild
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
        if (isGuildOwner(player)) {
            if (feedback) {
                sender.sendMessage(Component.text("You are the guild owner! Use /guild disband to disband the guild.", NamedTextColor.RED));
            }
            return false;
        }

        return true;
    }
}
