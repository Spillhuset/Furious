package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.StandaloneCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for wallet-related commands.
 */
public class WalletCommand extends StandaloneCommand {
    private final WalletManager walletManager;
    private final AuditLogger auditLogger;

    /**
     * Creates a new WalletCommand.
     *
     * @param plugin The plugin instance
     */
    public WalletCommand(Furious plugin) {
        super(plugin);
        this.walletManager = plugin.getWalletManager();
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "wallet";
    }

    @Override
    public String getDescription() {
        return "Manage your wallet and trade scraps";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Wallet Commands:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/wallet - Check your balance", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/wallet pay <player> <amount> - Pay scraps to another player", NamedTextColor.YELLOW));

        // Admin commands
        if (sender.hasPermission("furious.wallet.admin")) {
            sender.sendMessage(Component.text("Admin Commands:", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/wallet give <player> <amount> - Give scraps to a player", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/wallet take <player> <amount> - Take scraps from a player", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/wallet set <player> <amount> - Set a player's scraps", NamedTextColor.GOLD));
        }
    }

    @Override
    public String getPermission() {
        return "furious.wallet";
    }

    @Override
    protected boolean executeCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        // No arguments - show balance
        if (args.length == 0) {
            return executeBalanceCommand((Player) sender);
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "pay" -> executePayCommand(sender, args);
            case "give" -> executeGiveCommand(sender, args);
            case "take" -> executeTakeCommand(sender, args);
            case "set" -> executeSetCommand(sender, args);
            default -> {
                getUsage(sender);
                yield true;
            }
        };
    }

    /**
     * Executes the balance subcommand.
     *
     * @param player The player
     * @return true if the command was handled, false otherwise
     */
    private boolean executeBalanceCommand(Player player) {
        double balance = walletManager.getBalance(player);
        player.sendMessage(Component.text("Your wallet balance: ", NamedTextColor.YELLOW)
                .append(Component.text(walletManager.formatAmount(balance), NamedTextColor.GOLD)));
        return true;
    }

    /**
     * Executes the pay subcommand.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean executePayCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /wallet pay <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player from = (Player) sender;
        Player to = Bukkit.getPlayer(args[1]);

        if (to == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        if (to.equals(from)) {
            sender.sendMessage(Component.text("You cannot pay yourself.", NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("Amount must be greater than 0.", NamedTextColor.RED));
            return true;
        }

        if (!walletManager.has(from, amount)) {
            sender.sendMessage(Component.text("You don't have enough scraps.", NamedTextColor.RED));
            return true;
        }

        if (walletManager.transfer(from, to, amount)) {
            sender.sendMessage(Component.text("You paid ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to " + to.getName() + ".", NamedTextColor.GREEN)));

            to.sendMessage(Component.text("You received ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from " + from.getName() + ".", NamedTextColor.GREEN)));

            auditLogger.logSensitiveOperation(from, "wallet pay", "Paid " + amount + " to " + to.getName());
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to transfer scraps.", NamedTextColor.RED));
            return true;
        }
    }

    /**
     * Executes the give subcommand.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean executeGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("furious.wallet.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /wallet give <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("Amount must be greater than 0.", NamedTextColor.RED));
            return true;
        }

        double currentBalance = walletManager.getBalance(target);
        if (walletManager.deposit(target, amount)) {
            sender.sendMessage(Component.text("Gave ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to " + target.getName() + ".", NamedTextColor.GREEN)));

            target.sendMessage(Component.text("You received ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from an admin.", NamedTextColor.GREEN)));

            auditLogger.logSensitiveOperation(sender, "wallet give", "Gave " + amount + " to " + target.getName());
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to give scraps.", NamedTextColor.RED));
            return true;
        }
    }

    /**
     * Executes the take subcommand.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean executeTakeCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("furious.wallet.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /wallet take <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("Amount must be greater than 0.", NamedTextColor.RED));
            return true;
        }

        if (!walletManager.has(target, amount)) {
            sender.sendMessage(Component.text(target.getName() + " doesn't have enough scraps.", NamedTextColor.RED));
            return true;
        }

        if (walletManager.withdraw(target, amount)) {
            sender.sendMessage(Component.text("Took ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from " + target.getName() + ".", NamedTextColor.GREEN)));

            target.sendMessage(Component.text("An admin took ", NamedTextColor.RED)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from your wallet.", NamedTextColor.RED)));

            auditLogger.logSensitiveOperation(sender, "wallet take", "Took " + amount + " from " + target.getName());
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to take scraps.", NamedTextColor.RED));
            return true;
        }
    }

    /**
     * Executes the set subcommand.
     *
     * @param sender The command sender
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean executeSetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("furious.wallet.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /wallet set <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
            return true;
        }

        if (amount < 0) {
            sender.sendMessage(Component.text("Amount cannot be negative.", NamedTextColor.RED));
            return true;
        }

        if (walletManager.setBalance(target, amount)) {
            sender.sendMessage(Component.text("Set ", NamedTextColor.GREEN)
                    .append(Component.text(target.getName() + "'s", NamedTextColor.YELLOW))
                    .append(Component.text(" balance to ", NamedTextColor.GREEN))
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD)));

            target.sendMessage(Component.text("An admin set your wallet balance to ", NamedTextColor.YELLOW)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD)));

            auditLogger.logSensitiveOperation(sender, "wallet set", "Set " + target.getName() + "'s balance to " + amount);
            return true;
        } else {
            sender.sendMessage(Component.text("Failed to set wallet balance.", NamedTextColor.RED));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("pay".startsWith(partial)) {
                completions.add("pay");
            }

            if (sender.hasPermission("furious.wallet.admin")) {
                if ("give".startsWith(partial)) {
                    completions.add("give");
                }
                if ("take".startsWith(partial)) {
                    completions.add("take");
                }
                if ("set".startsWith(partial)) {
                    completions.add("set");
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if ("pay".equals(subCommand) ||
                (sender.hasPermission("furious.wallet.admin") &&
                 ("give".equals(subCommand) || "take".equals(subCommand) || "set".equals(subCommand)))) {

                String partial = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(partial)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}
