package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import com.spillhuset.furious.managers.BankManager;
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
 * Subcommand for claiming a chunk for a bank.
 */
public class ClaimSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new ClaimSubCommand.
     *
     * @param plugin The plugin instance
     */
    public ClaimSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Claim the current chunk for a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank claim [bank]", NamedTextColor.YELLOW)
                .append(Component.text(" - Claim the current chunk for the specified bank (defaults to RubberBank)", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank c [bank]", NamedTextColor.GRAY));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        // Get the bank name from args if provided, otherwise use the default bank
        String bankName = args.length > 1 ? args[1] : "RubberBank";
        Bank bank = bankManager.getBank(bankName);

        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Get the chunk the player is standing in
        Chunk chunk = player.getLocation().getChunk();

        // Claim the chunk for the bank
        bankManager.claimChunk(bank, chunk, player);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 2) {
            // Suggest bank names for claim command
            String partialBankName = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();

            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.bank.claim";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can claim chunks for banks
    }
}