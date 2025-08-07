package com.spillhuset.furious.commands.wallet;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
import com.spillhuset.furious.utils.AuditLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for adding scraps to a player's wallet.
 */
public class AddSubCommand implements SubCommand {
    private final Furious plugin;
    private final WalletManager walletManager;
    private final AuditLogger auditLogger;

    /**
     * Creates a new AddSubCommand.
     *
     * @param plugin The plugin instance
     */
    public AddSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
        this.auditLogger = plugin.getAuditLogger();
    }

    @Override
    public String getName() {
        return "add";
    }

    @Override
    public String getDescription() {
        return "Add scraps to a player's wallet";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));

        if (!(sender instanceof Player) ||
            sender.hasPermission("furious.wallet.add") ||
            sender.isOp()) {
            sender.sendMessage(Component.text("/wallet add <player> <amount>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Add scraps to a player's wallet", NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text("Shorthand: /wallet a", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: ", NamedTextColor.RED).append(Component.text(args[1], NamedTextColor.GOLD)));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: ", NamedTextColor.RED).append(Component.text(args[2], NamedTextColor.GRAY)));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(Component.text("Amount must be greater than 0.", NamedTextColor.RED));
            return true;
        }

        if (walletManager.deposit(target, amount)) {
            sender.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.GOLD))
                    .append(Component.text("'s wallet.", NamedTextColor.GREEN)));

            target.sendMessage(Component.text("An admin added ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to your wallet.", NamedTextColor.GREEN)));

            auditLogger.logSensitiveOperation(sender, "wallet add", "Added " + amount + " to " + target.getName() + "'s wallet");
        } else {
            sender.sendMessage(Component.text("Failed to add scraps.", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                // Check if player has a name and matches the partial input
                if (player.getName() != null && player.getName().toLowerCase().startsWith(partial)) {
                    // Check if player is not an admin or op
                    boolean isAdmin = player.isOp();

                    if (!isAdmin) {
                        // Only include players who have a wallet
                        UUID playerId = player.getUniqueId();
                        double balance = walletManager.getWallet(playerId);
                        // The getWallet method returns the starting balance for players without a wallet,
                        // so we need to check if the player has played before to ensure they have a wallet
                        if (player.hasPlayedBefore()) {
                            completions.add(player.getName());
                        }
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.wallet.add";
    }

    /**
     * Checks if the sender has permission to use this command.
     *
     * @param sender The command sender
     * @return true if the sender has permission, false otherwise
     */
    @Override
    public boolean checkPermission(CommandSender sender) {
        return sender.hasPermission(getPermission()) ||
               sender.isOp();
    }
}