package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for admin transfer of guild ownership to any player.
 */
public class AdminTransferSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new AdminTransferSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AdminTransferSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "admintransfer";
    }

    @Override
    public String getDescription() {
        return "Transfers ownership of any guild to any player (admin only).";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild admintransfer <guild> <player> - Transfers ownership of a guild to a player", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("furious.guild.admin")) {
            player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        // Get the guild
        String guildName = args[1];
        Guild guild = plugin.getGuildManager().getGuildByName(guildName);
        if (guild == null) {
            player.sendMessage(Component.text("Guild not found: " + guildName, NamedTextColor.RED));
            return true;
        }

        // Get the target player
        String targetName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            player.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return true;
        }

        UUID targetId = targetPlayer.getUniqueId();

        // Transfer ownership
        if (plugin.getGuildManager().adminTransferGuild(guild, targetId, player)) {
            // Success message is sent by the manager
            return true;
        } else {
            // Failure message is sent by the manager
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player) || !player.hasPermission("furious.guild.admin")) {
            return completions;
        }

        if (args.length == 2) {
            // Suggest guild names
            String partial = args[1].toLowerCase();
            for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                if (guild.getName().toLowerCase().startsWith(partial)) {
                    completions.add(guild.getName());
                }
            }
        } else if (args.length == 3) {
            // Suggest online player names
            String partial = args[2].toLowerCase();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                    completions.add(onlinePlayer.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.admin";
    }
}