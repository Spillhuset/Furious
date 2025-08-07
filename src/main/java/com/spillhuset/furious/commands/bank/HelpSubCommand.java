package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.HelpMenuFormatter;
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
        HelpMenuFormatter.showPlayerCommandsHeader(sender, "Bank Help");
        HelpMenuFormatter.formatPlayerSubCommand(sender, "/bank", "help", "Show help information for bank commands");
        sender.sendMessage(Component.text("Shorthand: /bank h", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        HelpMenuFormatter.showPlayerCommandsHeader(sender, "Bank");

        // Display commands based on permissions
        for (SubCommand subCommand : subCommands.values()) {
            if (subCommand.checkPermission(sender, false)) {
                HelpMenuFormatter.formatPlayerSubCommand(sender, "/bank", subCommand.getName(), subCommand.getDescription());
            }
        }

        // Show chunk-related commands if player has permission
        if (sender instanceof Player player) {
            if (player.hasPermission("furious.bank.claim") || player.hasPermission("furious.bank.unclaim") || player.hasPermission("furious.bank.info")) {
                HelpMenuFormatter.showPlayerCommandsHeader(sender, "Bank Territory");

                if (player.hasPermission("furious.bank.claim")) {
                    HelpMenuFormatter.formatPlayerSubCommandWithParams(sender, "/bank", "claim", "[bank]", "", "Claim the current chunk for a bank");
                }

                if (player.hasPermission("furious.bank.unclaim")) {
                    HelpMenuFormatter.formatPlayerSubCommand(sender, "/bank", "unclaim", "Unclaim the current chunk from a bank");
                }

                if (player.hasPermission("furious.bank.info")) {
                    HelpMenuFormatter.formatPlayerSubCommand(sender, "/bank", "info", "Show information about the bank at your location");
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