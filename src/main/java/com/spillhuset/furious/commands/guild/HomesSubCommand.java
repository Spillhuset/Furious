package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.entities.Home;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.GuildSubCommand;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subcommand for managing guild homes.
 */
public class HomesSubCommand implements GuildSubCommand {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Creates a new HomesSubCommand.
     *
     * @param plugin The plugin instance
     */
    public HomesSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    /**
     * Registers all homes subcommands.
     */
    private void registerSubCommands() {
        subCommands.put("set", new HomesSetSubCommand(plugin));
        subCommands.put("teleport", new HomesTeleportSubCommand(plugin));
        // Alias "tp" for "teleport"
        subCommands.put("tp", new HomesTeleportSubCommand(plugin));
    }

    @Override
    public String getName() {
        return "homes";
    }

    @Override
    public String getDescription() {
        return "Manages guild homes.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Guild Homes Commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild homes set <name> - Sets a guild home at your current location", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild homes teleport <name> - Teleports you to a guild home", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guild homes tp <name> - Alias for teleport command", NamedTextColor.YELLOW));

        // Show admin commands if the sender has admin permissions
        if (sender.hasPermission("furious.guild.admin")) {
            sender.sendMessage(Component.text("Admin Commands:", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/guild homes <guild> [home] - Teleports you to another guild's home", NamedTextColor.YELLOW));
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if this is an admin using the format: /guild homes <guild> [home]
        if (player.hasPermission("furious.guild.admin") && args.length >= 2) {
            // Try to find a guild with the name specified in args[1]
            Guild targetGuild = plugin.getGuildManager().getGuildByName(args[1]);
            if (targetGuild != null) {
                // This is an admin teleporting to another guild's home
                String homeName = args.length >= 3 ? args[2] : "default";

                // Teleport to the guild home
                if (plugin.getHomesManager().teleportToGuildHome(player, targetGuild, homeName)) {
                    // Success message is sent by the manager
                    return true;
                } else {
                    // Failure message is sent by the manager
                    return true;
                }
            }
        }

        if (args.length < 2) {
            getUsage(sender);
            return true;
        }

        String subCommandName = args[1].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            getUsage(sender);
            return true;
        }

        // Check permissions
        if (!subCommand.checkPermission(sender)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Check if this is an admin
        if (sender instanceof Player player && player.hasPermission("furious.guild.admin")) {
            if (args.length == 2) {
                // Suggest both subcommands and guild names for admins
                String partial = args[1].toLowerCase();

                // Add subcommands
                for (SubCommand subCmd : subCommands.values()) {
                    if (subCmd.getName().startsWith(partial) && subCmd.checkPermission(sender, false)) {
                        completions.add(subCmd.getName());
                    }
                }

                // Add guild names
                for (Guild guild : plugin.getGuildManager().getAllGuilds()) {
                    if (guild.getName().toLowerCase().startsWith(partial)) {
                        completions.add(guild.getName());
                    }
                }

                return completions;
            } else if (args.length == 3) {
                // Check if args[1] is a guild name
                Guild targetGuild = plugin.getGuildManager().getGuildByName(args[1]);
                if (targetGuild != null) {
                    // Suggest home names for the specified guild
                    String partial = args[2].toLowerCase();
                    plugin.getHomesManager().getGuildHomes(targetGuild.getId()).forEach(home -> {
                        if (home.getName().toLowerCase().startsWith(partial)) {
                            completions.add(home.getName());
                        }
                    });

                    return completions;
                }
            }
        }

        // Regular tab completion for non-admins or if not using the admin format
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (SubCommand subCmd : subCommands.values()) {
                if (subCmd.getName().startsWith(partial) && subCmd.checkPermission(sender, false)) {
                    completions.add(subCmd.getName());
                }
            }
        } else if (args.length > 2) {
            SubCommand subCommand = subCommands.get(args[1].toLowerCase());
            if (subCommand != null && subCommand.checkPermission(sender, false)) {
                return subCommand.tabComplete(sender, args);
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.guild.homes";
    }

    @Override
    public GuildRole getRequiredRole() {
        return GuildRole.USER;
    }

    @Override
    public boolean checkGuildPermission(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return false;
        }

        // Allow admins to use the command even if they're not in a guild
        if (player.hasPermission("furious.guild.admin")) {
            return true;
        }

        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return false;
        }

        return true;
    }
}
