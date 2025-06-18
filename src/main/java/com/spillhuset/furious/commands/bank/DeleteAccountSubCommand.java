package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
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
 * Subcommand for deleting a player's account from a bank.
 */
public class DeleteAccountSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new DeleteAccountSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeleteAccountSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "deleteaccount";
    }

    @Override
    public String getDescription() {
        return "Delete a player's account from a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank deleteaccount <bankName> <playerName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Delete the specified player's account from the specified bank", NamedTextColor.WHITE)));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String bankName = args[1];
        String playerName = args[2];

        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            sender.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        UUID playerId = offlinePlayer.getUniqueId();

        // Check if the player has an account in the bank
        if (!bank.hasAccount(playerId)) {
            sender.sendMessage(Component.text("Player " + playerName + " does not have an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Delete the account
        if (bankManager.deleteAccount(bankName, playerId)) {
            sender.sendMessage(Component.text("Account for player ", NamedTextColor.GREEN)
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text(" deleted from bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to delete account.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggest bank names for the first argument
            String partialBankName = args[1].toLowerCase();

            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }
        } else if (args.length == 3) {
            // Suggest player names for the second argument
            String partialPlayerName = args[2].toLowerCase();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialPlayerName)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.deleteaccount";
    }
}