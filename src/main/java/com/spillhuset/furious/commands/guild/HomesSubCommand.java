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
        sender.sendMessage(Component.text("/guilds homes set <name> - Sets a guild home at your current location", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guilds homes teleport <name> - Teleports you to a guild home", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/guilds homes tp <name> - Alias for teleport command", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
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

        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(Component.text("You are not in a guild!", NamedTextColor.RED));
            return false;
        }

        return true;
    }
}
