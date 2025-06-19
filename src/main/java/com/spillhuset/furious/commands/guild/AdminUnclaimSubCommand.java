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
 * Subcommand for admin unclaiming of chunks from unmanned guilds.
 */
public class AdminUnclaimSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new AdminUnclaimSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AdminUnclaimSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "adminunclaim";
    }

    @Override
    public String getDescription() {
        return "Unclaims a chunk from an unmanned guild (admin only).";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild adminunclaim <guild>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Unclaims the chunk you are standing in from the specified unmanned guild.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("This command is only available to server administrators.", NamedTextColor.YELLOW));
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

        if (args.length < 2) {
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

        // Check if the guild is unmanned
        if (!plugin.getGuildManager().isUnmannedGuild(guild)) {
            player.sendMessage(Component.text("This command can only be used on unmanned guilds!", NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        if (plugin.getGuildManager().adminUnclaimChunk(guild, player.getLocation().getChunk(), player)) {
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
            // Suggest unmanned guild names
            String partial = args[1].toLowerCase();

            // Get unmanned guilds
            Guild safeGuild = plugin.getGuildManager().getSafeGuild();
            Guild warGuild = plugin.getGuildManager().getWarGuild();
            Guild wildGuild = plugin.getGuildManager().getWildGuild();

            if (safeGuild != null && safeGuild.getName().toLowerCase().startsWith(partial)) {
                completions.add(safeGuild.getName());
            }
            if (warGuild != null && warGuild.getName().toLowerCase().startsWith(partial)) {
                completions.add(warGuild.getName());
            }
            if (wildGuild != null && wildGuild.getName().toLowerCase().startsWith(partial)) {
                completions.add(wildGuild.getName());
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.admin";
    }
}