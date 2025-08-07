package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for teleporting to a bank's spawn point, skipping the teleport queue.
 */
public class TeleportBankSubCommand extends BaseBankCommand {

    /**
     * Creates a new TeleportBankSubCommand.
     *
     * @param plugin The plugin instance
     */
    public TeleportBankSubCommand(Furious plugin) {
        super(plugin, false); // Does not require bank chunk
    }

    @Override
    public String getName() {
        return "teleport";
    }

    @Override
    public String getDescription() {
        return "Teleport to a bank's spawn point, skipping the teleport queue";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank teleport <bankName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Teleport to the specified bank's spawn point", NamedTextColor.WHITE)));
    }

    @Override
    protected boolean executeCommand(Player player, @NotNull String[] args) {
        if (args.length < 1) {
            getUsage(player);
            return true;
        }

        String bankName = args[0];

        // Get the bank by name
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Check if player has permission to teleport
        if (!player.hasPermission("furious.bank.teleport.bypass") && !player.isOp()) {
            player.sendMessage(Component.text("You don't have permission to skip the teleport queue.", NamedTextColor.RED));
            return true;
        }

        // Teleport the player to the bank
        boolean success = bankManager.teleportToBank(player, bankName);

        return true;
    }

    @Override
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        // This method is not used for this command as we always require the bank name
        // and handle it in executeCommand
        return executeCommand(player, new String[]{bankName});
    }

    @Override
    protected boolean executeConsoleCommand(CommandSender sender, String bankName, @NotNull String[] args) {
        sender.sendMessage(Component.text("This command can only be executed by a player.", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest bank names
            String partialBankName = args[0].toLowerCase();
            for (String bankName : bankManager.getBanks().keySet()) {
                if (bankName.toLowerCase().startsWith(partialBankName)) {
                    completions.add(bankName);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.bank.teleport";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can teleport
    }
}