package com.spillhuset.furious.commands.guild;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.GuildSubCommand;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.HelpMenuFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main command handler for guild-related commands.
 */
public class GuildCommand implements CommandExecutor, TabCompleter {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Creates a new GuildCommand.
     *
     * @param plugin The plugin instance
     */
    public GuildCommand(Furious plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        registerSubCommands();
    }

    /**
     * Registers all guild subcommands.
     */
    private void registerSubCommands() {
        subCommands.put("create", new CreateSubCommand(plugin));
        subCommands.put("invite", new InviteSubCommand(plugin));
        subCommands.put("join", new JoinSubCommand(plugin));
        subCommands.put("leave", new LeaveSubCommand(plugin));
        subCommands.put("info", new InfoSubCommand(plugin));
        subCommands.put("list", new ListSubCommand(plugin));
        // Core guild management commands
        subCommands.put("kick", new KickSubCommand(plugin));
        subCommands.put("disband", new DisbandSubCommand(plugin));
        subCommands.put("transfer", new TransferSubCommand(plugin));
        // subCommands.put("description", new DescriptionSubCommand(plugin));

        // Plot management commands
        subCommands.put("claim", new ClaimSubCommand(plugin));
        subCommands.put("unclaim", new UnclaimSubCommand(plugin));
        subCommands.put("claims", new ClaimsSubCommand(plugin));
        subCommands.put("mobs", new MobsSubCommand(plugin));

        // Guild homes commands
        subCommands.put("homes", new HomesSubCommand(plugin));

        // Guild world commands
        subCommands.put("world", new WorldSubCommand(plugin));

        // Guild settings commands
        subCommands.put("set", new SetSubCommand(plugin));
        subCommands.put("role", new RoleSubCommand(plugin));

        // Guild join request/invitation commands
        subCommands.put("accept", new AcceptSubCommand(plugin));
        subCommands.put("decline", new DeclineSubCommand(plugin));
        subCommands.put("cancelinvite", new CancelInviteSubCommand(plugin));

        // Admin commands
        subCommands.put("home", new HomesAdminSubCommand(plugin));
        subCommands.put("admintransfer", new AdminTransferSubCommand(plugin));
        subCommands.put("adminunclaim", new AdminUnclaimSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            showHelp(sender);
            return true;
        }

        // Check permissions based on whether it's a GuildSubCommand or regular SubCommand
        if (subCommand instanceof GuildSubCommand guildSubCommand) {
            if (!guildSubCommand.checkGuildPermission(sender)) {
                // Message already sent by checkGuildPermission
                return true;
            }
        } else {
            if (!subCommand.checkPermission(sender)) {
                sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                return true;
            }
        }

        return subCommand.execute(sender, args);
    }

    /**
     * Shows help information for guild commands.
     *
     * @param sender The command sender
     */
    private void showHelp(CommandSender sender) {
        HelpMenuFormatter.showPlayerCommandsHeader(sender, "Guild");

        if (sender instanceof Player player) {
            boolean isInGuild = plugin.getGuildManager().isInGuild(player.getUniqueId());
            boolean isGuildOwner = isInGuild && plugin.getGuildManager().getPlayerGuild(player.getUniqueId()).getOwner().equals(player.getUniqueId());

            // Display commands based on permissions
            for (SubCommand subCommand : subCommands.values()) {
                boolean canUse;

                if (subCommand instanceof GuildSubCommand guildSubCommand) {
                    canUse = guildSubCommand.checkGuildPermission(sender, false);
                } else {
                    canUse = subCommand.checkPermission(sender, false);
                }

                if (canUse) {
                    HelpMenuFormatter.formatPlayerSubCommand(sender, "/guild", subCommand.getName(), subCommand.getDescription());
                }
            }

            // Show owner-only commands if the player is in a guild and is the owner
            if (isInGuild && isGuildOwner) {
                // Owner-only commands
                HelpMenuFormatter.formatPlayerSubCommandWithParams(sender, "/guild", "kick", "<player>", "", "Kick a player from your guild");
                HelpMenuFormatter.formatPlayerSubCommand(sender, "/guild", "disband", "Disband your guild");
                HelpMenuFormatter.formatPlayerSubCommandWithParams(sender, "/guild", "transfer", "<player>", "", "Transfer ownership of your guild");
                // HelpMenuFormatter.formatPlayerSubCommandWithParams(sender, "/guild", "description", "<text>", "", "Set your guild's description");
            }
        } else {
            // Console commands
            HelpMenuFormatter.showAdminCommandsHeader(sender, "Guild");
            HelpMenuFormatter.formatAdminSubCommand(sender, "/guild", "list", "List all guilds");
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/guild", "info", "<guild>", "", "Show information about a guild");
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/guild", "disband", "<guild>", "", "Disband a guild");
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/guild", "admintransfer", "<guild> <player>", "", "Transfer ownership of a guild to a player");
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/guild", "adminunclaim", "<guild>", "", "Unclaim a chunk from an unmanned guild");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (SubCommand subCmd : subCommands.values()) {
                boolean canUse;

                if (subCmd instanceof GuildSubCommand guildSubCmd) {
                    canUse = guildSubCmd.checkGuildPermission(sender, false);
                } else {
                    canUse = subCmd.checkPermission(sender, false);
                }

                if (subCmd.getName().startsWith(partial) && canUse) {
                    completions.add(subCmd.getName());
                }
            }
        } else if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                boolean canUse;

                if (subCommand instanceof GuildSubCommand guildSubCommand) {
                    canUse = guildSubCommand.checkGuildPermission(sender, false);
                } else {
                    canUse = subCommand.checkPermission(sender, false);
                }

                if (canUse) {
                    return subCommand.tabComplete(sender, args);
                }
            }
        }

        return completions;
    }
}
