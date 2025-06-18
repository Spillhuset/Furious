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
 * Subcommand for unclaiming a chunk from a bank.
 */
public class UnclaimSubCommand implements SubCommand {
    private final Furious plugin;
    private final BankManager bankManager;

    /**
     * Creates a new UnclaimSubCommand.
     *
     * @param plugin The plugin instance
     */
    public UnclaimSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.bankManager = plugin.getBankManager();
    }

    @Override
    public String getName() {
        return "unclaim";
    }

    @Override
    public String getDescription() {
        return "Unclaim the current chunk from a bank";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/bank unclaim", NamedTextColor.YELLOW)
                .append(Component.text(" - Unclaim the current chunk from the bank that owns it", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Shorthand: /bank u", NamedTextColor.GRAY));
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
            player.sendMessage(Component.text("This chunk is not claimed by any bank.", NamedTextColor.RED));
            return true;
        }

        // Unclaim the chunk from the bank
        bankManager.unclaimChunk(bank, chunk, player);

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>(); // No tab completions for unclaim command
    }

    @Override
    public String getPermission() {
        return "furious.bank.unclaim";
    }

    @Override
    public boolean denyNonPlayer() {
        return true; // Only players can unclaim chunks from banks
    }
}