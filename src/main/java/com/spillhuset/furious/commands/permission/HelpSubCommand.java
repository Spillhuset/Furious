package com.spillhuset.furious.commands.permission;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Subcommand for showing help information for permission commands.
 */
public class HelpSubCommand implements SubCommand {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Creates a new HelpSubCommand.
     *
     * @param plugin The plugin instance
     * @param subCommands The map of subcommands to display help for
     */
    public HelpSubCommand(Furious plugin, Map<String, SubCommand> subCommands) {
        this.plugin = plugin;
        this.subCommands = subCommands;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Show help information for permission commands";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/perm help", NamedTextColor.YELLOW)
                .append(Component.text(" - Show help information for permission commands", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /perm h", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendMessage(Component.text("=== Permission Commands ===", NamedTextColor.GOLD));

        // Display role management commands
        sender.sendMessage(Component.text("=== Role Management ===", NamedTextColor.GOLD));
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.getName().startsWith("create") ||
                subCommand.getName().startsWith("delete") ||
                subCommand.getName().startsWith("list") ||
                subCommand.getName().startsWith("role") ||
                subCommand.getName().startsWith("set")) {

                if (subCommand.checkPermission(sender, false)) {
                    sender.sendMessage(Component.text("/perm " + subCommand.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" - " + subCommand.getDescription(), NamedTextColor.WHITE)));
                }
            }
        }

        // Display permission management commands
        sender.sendMessage(Component.text("=== Permission Management ===", NamedTextColor.GOLD));
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.getName().startsWith("add") && subCommand.getName().contains("permission") && !subCommand.getName().contains("player") ||
                subCommand.getName().startsWith("remove") && subCommand.getName().contains("permission") && !subCommand.getName().contains("player")) {

                if (subCommand.checkPermission(sender, false)) {
                    sender.sendMessage(Component.text("/perm " + subCommand.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" - " + subCommand.getDescription(), NamedTextColor.WHITE)));
                }
            }
        }

        // Display player-role management commands
        sender.sendMessage(Component.text("=== Player-Role Management ===", NamedTextColor.GOLD));
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.getName().contains("player") && subCommand.getName().contains("role")) {
                if (subCommand.checkPermission(sender, false)) {
                    sender.sendMessage(Component.text("/perm " + subCommand.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" - " + subCommand.getDescription(), NamedTextColor.WHITE)));
                }
            }
        }

        // Display player-permission management commands
        sender.sendMessage(Component.text("=== Player-Permission Management ===", NamedTextColor.GOLD));
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.getName().contains("player") && subCommand.getName().contains("permission")) {
                if (subCommand.checkPermission(sender, false)) {
                    sender.sendMessage(Component.text("/perm " + subCommand.getName(), NamedTextColor.YELLOW)
                            .append(Component.text(" - " + subCommand.getDescription(), NamedTextColor.WHITE)));
                }
            }
        }

        // Show shorthand commands
        sender.sendMessage(Component.text("Shorthand: /perm cr (createrole), /perm dr (deleterole), /perm lr (listroles), /perm ri (roleinfo), /perm srd (setroledescription), /perm ap (addpermission), /perm rp (removepermission), /perm apr (addplayerrole), /perm rpr (removeplayerrole), /perm lpr (listplayerroles), /perm app (addplayerpermission), /perm rpp (removeplayerpermission), /perm lpp (listplayerpermissions), /perm h (help)", NamedTextColor.GRAY));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for help command
    }

    @Override
    public String getPermission() {
        return null; // No permission required for help command
    }
}