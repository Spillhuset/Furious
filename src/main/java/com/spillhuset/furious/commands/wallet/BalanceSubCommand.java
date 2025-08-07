package com.spillhuset.furious.commands.wallet;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
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
 * Subcommand for checking wallet balance.
 */
public class BalanceSubCommand implements SubCommand {
    private final Furious plugin;
    private final WalletManager walletManager;

    /**
     * Creates a new BalanceSubCommand.
     *
     * @param plugin The plugin instance
     */
    public BalanceSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.walletManager = plugin.getWalletManager();
    }

    @Override
    public String getName() {
        return "balance";
    }

    @Override
    public String getDescription() {
        return "Check your wallet balance";
    }

    @Override
    public void getUsage(CommandSender sender) {
        if (sender instanceof Player && !sender.hasPermission("furious.wallet.balance.others") && !sender.isOp()) {
            sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/wallet balance", NamedTextColor.YELLOW)
                    .append(Component.text(" - Check your wallet balance", NamedTextColor.WHITE)));
        }
        if (!(sender instanceof Player) || sender.hasPermission("furious.wallet.balance.others") || sender.isOp()) {
            sender.sendMessage(Component.text("/wallet balance <player>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Check another player's wallet balance", NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text("Shorthand: /wallet b", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        // if the sender is a player or non op nor have admin permission, they can only check their own balance
        // if the sender is a player with op or admin permission, they can check a specified players balance
        // if the sender is not a player, they can only check the given players balance

        // Case 1 & 2 combined: Console or player with appropriate permissions checking another player's balance
        boolean isConsole = !(sender instanceof Player);
        boolean hasPermission = (sender.hasPermission("furious.wallet.balance.others") ||
                               sender.isOp());

        if (isConsole || hasPermission) {
            if (args.length >= 2) {
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                    sender.sendMessage(Component.text("Player not found: ", NamedTextColor.RED).append(Component.text(args[1], NamedTextColor.GOLD)));
                    return true;
                }

                // Check if target player is op
                if (targetPlayer.isOp()) {
                    sender.sendMessage(Component.text(targetPlayer.getName() + " has no wallet (op)", NamedTextColor.RED));
                    return true;
                }

                double balance = walletManager.getWallet(targetPlayer.getUniqueId());
                sender.sendMessage(Component.text(targetPlayer.getName() + "'s wallet balance: ", NamedTextColor.YELLOW)
                        .append(Component.text(walletManager.formatAmount(balance), NamedTextColor.GOLD)));
                return true;
            } else {
                getUsage(sender);
                return true;
            }
        }

        // Case 3: Regular player checking their own balance
        Player player = (Player) sender;
        double balance = walletManager.getBalance(player);
        player.sendMessage(Component.text("Your wallet balance: ", NamedTextColor.YELLOW)
                .append(Component.text(walletManager.formatAmount(balance), NamedTextColor.GOLD)));
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player) ||
            sender.hasPermission("furious.wallet.balance.others") ||
            sender.isOp()) {

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
        return new ArrayList<>(); // No tab completions for regular players
    }

    @Override
    public String getPermission() {
        return "furious.wallet";
    }
}