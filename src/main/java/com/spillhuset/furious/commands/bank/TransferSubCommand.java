package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subcommand for transferring money between bank accounts.
 */
public class TransferSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;
    private final WalletManager walletManager;

    /**
     * Creates a new TransferSubCommand.
     *
     * @param plugin The plugin instance
     */
    public TransferSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.walletManager = plugin.getWalletManager();
    }

    @Override
    public String getName() {
        return "transfer";
    }

    @Override
    public String getDescription() {
        return "Transfer from your bank to another player's bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank transfer <player> <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Transfer the specified amount from your bank account to another player's bank account", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank t <player> <amount>", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found or not online.", NamedTextColor.RED));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(Component.text("You cannot transfer to yourself.", NamedTextColor.RED));
            return true;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }

            // Check if player has enough in bank
            if (!bankManager.getBank("RubberBank").hasAmount(player.getUniqueId(), amount)) {
                player.sendMessage(Component.text("You don't have enough in your bank account.", NamedTextColor.RED));
                return true;
            }

            // Transfer between bank accounts
            if (!bankManager.transfer(player, target, amount)) {
                player.sendMessage(Component.text("Failed to transfer funds.", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Successfully transferred ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.AQUA))
                    .append(Component.text(".", NamedTextColor.GREEN)));

            target.sendMessage(Component.text("You received ", NamedTextColor.GREEN)
                    .append(Component.text(walletManager.formatAmount(amount), NamedTextColor.GOLD))
                    .append(Component.text(" from ", NamedTextColor.GREEN))
                    .append(Component.text(player.getName(), NamedTextColor.AQUA))
                    .append(Component.text(".", NamedTextColor.GREEN)));

        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid amount. Please enter a valid number.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            // Player names for transfer command
            String partialPlayerName = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialPlayerName))
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Suggest some common amounts for transfer
            String partialAmount = args[2].toLowerCase();
            List<String> amounts = Arrays.asList("10", "50", "100", "500", "1000");
            List<String> completions = new ArrayList<>();

            for (String amount : amounts) {
                if (amount.startsWith(partialAmount)) {
                    completions.add(amount);
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.bank.transfer";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can transfer from their bank account
    }
}