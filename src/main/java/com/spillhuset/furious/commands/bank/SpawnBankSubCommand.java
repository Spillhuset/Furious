package com.spillhuset.furious.commands.bank;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Bank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for setting a bank's spawn point.
 */
public class SpawnBankSubCommand extends BaseBankCommand {

    /**
     * Creates a new SpawnBankSubCommand.
     *
     * @param plugin The plugin instance
     */
    public SpawnBankSubCommand(Furious plugin) {
        super(plugin, true); // Requires bank chunk
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Set the spawn point for a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank spawn", NamedTextColor.YELLOW)
                .append(Component.text(" - Set the spawn point for the bank at your current location", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/bank spawn <bankName>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set the spawn point for the specified bank at your current location", NamedTextColor.WHITE)));
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

        // Set the spawn point at the player's location
        Location location = player.getLocation();
        boolean success = bankManager.setBankSpawn(bank, location, player);

        return true;
    }

    @Override
    protected boolean executePlayerCommandWithBank(Player player, String bankName, @NotNull String[] args) {
        // Get the bank by name
        Bank bank = bankManager.getBank(bankName);
        if (bank == null) {
            player.sendMessage(Component.text("Bank not found: " + bankName, NamedTextColor.RED));
            return true;
        }

        // Set the spawn point at the player's location
        Location location = player.getLocation();
        boolean success = bankManager.setBankSpawn(bank, location, player);

        return true;
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
        return "furious.bank.spawn";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can set spawn points
    }
}