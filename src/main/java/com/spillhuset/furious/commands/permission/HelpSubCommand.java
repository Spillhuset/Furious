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

        // Display roles subcommand
        SubCommand rolesCommand = subCommands.get("roles");
        if (rolesCommand != null && rolesCommand.checkPermission(sender, false)) {
            sender.sendMessage(Component.text("/perm roles", NamedTextColor.YELLOW)
                    .append(Component.text(" - " + rolesCommand.getDescription(), NamedTextColor.WHITE)));
            rolesCommand.getUsage(sender);
        }

        // Display player subcommand
        SubCommand playerCommand = subCommands.get("player");
        if (playerCommand != null && playerCommand.checkPermission(sender, false)) {
            sender.sendMessage(Component.text("/perm player", NamedTextColor.YELLOW)
                    .append(Component.text(" - " + playerCommand.getDescription(), NamedTextColor.WHITE)));
            playerCommand.getUsage(sender);
        }

        // Show shorthand commands
        sender.sendMessage(Component.text("Shorthand: /perm r (roles), /perm p (player), /perm h (help)", NamedTextColor.GRAY));

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
