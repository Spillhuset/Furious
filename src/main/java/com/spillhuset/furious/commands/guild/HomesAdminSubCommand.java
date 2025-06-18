package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Home;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for admin teleportation to any guild home.
 */
public class HomesAdminSubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new HomesAdminSubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesAdminSubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "home";
    }

    @Override
    public String getDescription() {
        return "Teleports you to any guild's home (admin only).";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild home <guild> [home] - Teleports you to a guild's home", NamedTextColor.YELLOW));
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

        // Get the home name
        String homeName = args.length >= 3 ? args[2] : "default";

        // Teleport to the guild home
        if (plugin.getHomesManager().teleportToGuildHome(player, guild, homeName)) {
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
            // Suggest home names for the specified guild
            String guildName = args[1];
            Guild guild = plugin.getGuildManager().getGuildByName(guildName);
            if (guild != null) {
                String partial = args[2].toLowerCase();
                plugin.getHomesManager().getGuildHomes(guild.getId()).forEach(home -> {
                    if (home.getName().toLowerCase().startsWith(partial)) {
                        completions.add(home.getName());
                    }
                });
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.admin";
    }
}