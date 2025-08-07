package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
public class DeleteAccountSubCommand extends BaseBankCommand {

    /**
     * Creates a new DeleteAccountSubCommand.
     *
     * @param plugin The plugin instance
     */
    public DeleteAccountSubCommand(Furious plugin) {
        super(plugin, true); // Requires bank chunk
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
        sender.sendMessage(Component.text("/bank deleteaccount", NamedTextColor.YELLOW)
                .append(Component.text(" - Delete your own account from this bank", NamedTextColor.WHITE)));

        if (sender.hasPermission("furious.bank.deleteaccount.others")) {
            sender.sendMessage(Component.text("/bank deleteaccount <playerName>", NamedTextColor.YELLOW)
                    .append(Component.text(" - Delete the specified player's account from this bank", NamedTextColor.WHITE)));
        }
    }

    @Override
    protected boolean executeCommand(Player player, @NotNull String[] args) {
        // Get the bank from the chunk the player is standing in
        Chunk chunk = player.getLocation().getChunk();
        Bank bank = bankManager.getBankByChunk(chunk);

        if (bank == null) {
            player.sendMessage(Component.text("No bank found in this chunk.", NamedTextColor.RED));
            return true;
        }

        String bankName = bank.getName();

        // If no args provided, delete the player's own account
        if (args.length < 1) {
            // Check if the player has an account in the bank
            if (!bank.hasAccount(player.getUniqueId())) {
                player.sendMessage(Component.text("You don't have an account in bank " + bankName + ".", NamedTextColor.RED));
                return true;
            }

            // Delete the player's own account
            if (bankManager.deleteAccount(bankName, player.getUniqueId())) {
                player.sendMessage(Component.text("Your account has been deleted from bank ", NamedTextColor.GREEN)
                        .append(Component.text(bankName, NamedTextColor.GOLD))
                        .append(Component.text(".", NamedTextColor.GREEN)));
            } else {
                player.sendMessage(Component.text("Failed to delete your account.", NamedTextColor.RED));
            }

            return true;
        }

        // Admin functionality to delete another player's account
        if (!player.hasPermission("furious.bank.deleteaccount.others")) {
            player.sendMessage(Component.text("You don't have permission to delete other players' accounts.", NamedTextColor.RED));
            return true;
        }

        String playerName = args[0];

        // Get the player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null) {
            player.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        UUID playerId = offlinePlayer.getUniqueId();

        // Check if the player has an account in the bank
        if (!bank.hasAccount(playerId)) {
            player.sendMessage(Component.text("Player " + playerName + " does not have an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Delete the account
        if (bankManager.deleteAccount(bankName, playerId)) {
            player.sendMessage(Component.text("Account for player ", NamedTextColor.GREEN)
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text(" deleted from bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to delete account.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(sender);
            return true;
        }

        String playerName = args[0];

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
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        // Get the bank
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // If no args provided, delete the player's own account
        if (args.length < 1) {
            // Check if the player has an account in the bank
            if (!bank.hasAccount(player.getUniqueId())) {
                player.sendMessage(Component.text("You don't have an account in bank " + bankName + ".", NamedTextColor.RED));
                return true;
            }

            // Delete the player's own account
            if (bankManager.deleteAccount(bankName, player.getUniqueId())) {
                player.sendMessage(Component.text("Your account has been deleted from bank ", NamedTextColor.GREEN)
                        .append(Component.text(bankName, NamedTextColor.GOLD))
                        .append(Component.text(".", NamedTextColor.GREEN)));
            } else {
                player.sendMessage(Component.text("Failed to delete your account.", NamedTextColor.RED));
            }

            return true;
        }

        // Admin functionality to delete another player's account
        if (!player.hasPermission("furious.bank.deleteaccount.others")) {
            player.sendMessage(Component.text("You don't have permission to delete other players' accounts.", NamedTextColor.RED));
            return true;
        }

        String playerName = args[0];

        // Get the player
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offlinePlayer == null) {
            player.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        UUID playerId = offlinePlayer.getUniqueId();

        // Check if the player has an account in the bank
        if (!bank.hasAccount(playerId)) {
            player.sendMessage(Component.text("Player " + playerName + " does not have an account in bank " + bankName + ".", NamedTextColor.RED));
            return true;
        }

        // Delete the account
        if (bankManager.deleteAccount(bankName, playerId)) {
            player.sendMessage(Component.text("Account for player ", NamedTextColor.GREEN)
                    .append(Component.text(playerName, NamedTextColor.GOLD))
                    .append(Component.text(" deleted from bank ", NamedTextColor.GREEN))
                    .append(Component.text(bankName, NamedTextColor.GOLD))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        } else {
            player.sendMessage(Component.text("Failed to delete account.", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && !(sender instanceof Player)) {
            // Suggest bank names for console
            String partialBankName = args[0].toLowerCase();
            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }
        } else if ((args.length == 1 && sender instanceof Player) ||
                  (args.length == 2 && !(sender instanceof Player))) {
            // Suggest player names
            String partialPlayerName = args[args.length - 1].toLowerCase();
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