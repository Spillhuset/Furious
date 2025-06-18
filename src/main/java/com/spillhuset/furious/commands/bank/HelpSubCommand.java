package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Subcommand for showing help information for bank commands.
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
        return "Show help information for bank commands";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank help", NamedTextColor.YELLOW)
                .append(Component.text(" - Show help information for bank commands", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank h", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        sender.sendMessage(Component.text("=== Bank Commands ===", NamedTextColor.GOLD));

        // Display commands based on permissions
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.checkPermission(sender, false)) {
                sender.sendMessage(Component.text("/bank " + subCommand.getName(), NamedTextColor.YELLOW)
                        .append(Component.text(" - " + subCommand.getDescription(), NamedTextColor.WHITE)));
            }
        }

        // Show chunk-related commands if player has permission
        if (sender instanceof Player player) {
            if (player.hasPermission("furious.bank.claim") || player.hasPermission("furious.bank.unclaim") || player.hasPermission("furious.bank.info")) {
                sender.sendMessage(Component.text("=== Bank Territory Commands ===", NamedTextColor.GOLD));

                if (player.hasPermission("furious.bank.claim")) {
                    sender.sendMessage(Component.text("/bank claim [bank]", NamedTextColor.YELLOW)
                            .append(Component.text(" - Claim the current chunk for a bank", NamedTextColor.WHITE)));
                }

                if (player.hasPermission("furious.bank.unclaim")) {
                    sender.sendMessage(Component.text("/bank unclaim", NamedTextColor.YELLOW)
                            .append(Component.text(" - Unclaim the current chunk from a bank", NamedTextColor.WHITE)));
                }

                if (player.hasPermission("furious.bank.info")) {
                    sender.sendMessage(Component.text("/bank info", NamedTextColor.YELLOW)
                            .append(Component.text(" - Show information about the bank at your location", NamedTextColor.WHITE)));
                }
            }
        }

        sender.sendMessage(Component.text("Shorthand: /bank b (balance), /bank d (deposit), /bank w (withdraw), /bank t (transfer), /bank c (claim), /bank u (unclaim), /bank i (info), /bank h (help)", NamedTextColor.GRAY));

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