package com.spillhuset.furious.commands.wallet;

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
 * Subcommand for displaying wallet help information.
 */
public class HelpSubCommand implements SubCommand {
    private final Furious plugin;
    private final Map<String, SubCommand> subCommands;

    /**
     * Creates a new HelpSubCommand.
     *
     * @param plugin      The plugin instance
     * @param subCommands The map of available subcommands
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
        return "Display wallet command help";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/wallet help", NamedTextColor.YELLOW)
                .append(Component.text(" - Display wallet command help", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /wallet h", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // Commands header
        HelpMenuFormatter.showPlayerCommandsHeader(sender, "Wallet");
        HelpMenuFormatter.formatPlayerSubCommand(sender, "/wallet", "help", "Display wallet command help");

        // Player commands
        if (sender.hasPermission("furious.wallet.pay") && (sender instanceof Player) && !sender.isOp()) {
            HelpMenuFormatter.showPlayerCommandsHeader(sender, "Wallet");
            HelpMenuFormatter.formatPlayerSubCommand(sender, "/wallet", "balance", "Check a player's scrap balance");
            HelpMenuFormatter.formatPlayerSubCommandWithParams(sender, "/wallet", "pay", "<player> <amount>", "", "Pay scraps to another player");
        }

        // Admin commands - show header only once if any admin permission is present
        boolean showAdminHeader = sender.hasPermission("furious.wallet.balance.others") ||
                                 sender.hasPermission("furious.wallet.set") ||
                                 sender.hasPermission("furious.wallet.add") ||
                                 sender.hasPermission("furious.wallet.sub") ||
                                 sender.isOp();

        if (showAdminHeader) {
            HelpMenuFormatter.showAdminCommandsHeader(sender, "Wallet");
        }

        // Show specific commands based on permissions
        if (sender.hasPermission("furious.wallet.balance.others") ||
            sender.isOp()) {
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/wallet", "balance", "<player>", "", "Check a player's scrap balance");
        }

        if (sender.hasPermission("furious.wallet.set") ||
            sender.isOp()) {
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/wallet", "set", "<player> <amount>", "", "Set a player's scrap balance");
        }

        if (sender.hasPermission("furious.wallet.add") ||
            sender.isOp()) {
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/wallet", "add", "<player> <amount>", "", "Add scraps to a player");
        }

        if (sender.hasPermission("furious.wallet.sub") ||
            sender.isOp()) {
            HelpMenuFormatter.formatAdminSubCommandWithParams(sender, "/wallet", "sub", "<player> <amount>", "", "Subtract scraps from a player");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for help command
    }

    @Override
    public String getPermission() {
        return "furious.wallet";
    }
}