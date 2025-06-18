package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
import com.spillhuset.furious.managers.WalletManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for showing information about the bank at the player's location.
 */
public class InfoSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;
    private final WalletManager walletManager;

    /**
     * Creates a new InfoSubCommand.
     *
     * @param plugin The plugin instance
     */
    public InfoSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
        this.walletManager = plugin.getWalletManager();
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Show information about the bank at your location";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank info", NamedTextColor.YELLOW)
                .append(Component.text(" - Show information about the bank at your current location", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank i", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        Chunk chunk = player.getLocation().getChunk();

        // Check if the chunk is claimed by any bank
        Bank bank = bankManager.getBankByChunk(chunk);
        if (bank == null) {
            player.sendMessage(Component.text("You are not in a bank's territory.", NamedTextColor.RED));
            return true;
        }

        // Show bank information
        player.sendMessage(Component.text("=== Bank Information ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Name: ", NamedTextColor.YELLOW)
                .append(Component.text(bank.getName(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Claimed Chunks: ", NamedTextColor.YELLOW)
                .append(Component.text(bank.getClaimedChunkCount(), NamedTextColor.WHITE)));

        // Show player's account in this bank
        double balance = bank.getBalance(player.getUniqueId());
        player.sendMessage(Component.text("Your Balance: ", NamedTextColor.YELLOW)
                .append(Component.text(walletManager.formatAmount(balance), NamedTextColor.WHITE)));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for info command
    }

    @Override
    public String getPermission() {
        return "furious.bank.info";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can view bank information at their location
    }
}