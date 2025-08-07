package com.spillhuset.furious.commands.shops;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.ShopsManager;
import com.spillhuset.furious.misc.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for claiming a chunk for a shop.
 */
public class ClaimSubCommand implements SubCommand {

    private final Furious plugin;
    private final ShopsManager shopsManager;

    /**
     * Constructor for ClaimSubCommand.
     *
     * @param plugin The main plugin instance
     */
    public ClaimSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.shopsManager = plugin.getShopsManager();
    }

    /**
     * Get the name of the subcommand.
     *
     * @return The name of the subcommand
     */
    @Override
    public String getName() {
        return "claim";
    }

    /**
     * Get the description of the subcommand.
     *
     * @return The description of the subcommand
     */
    @Override
    public String getDescription() {
        return "Claim the current chunk for a shop";
    }

    /**
     * Show usage information for the subcommand.
     *
     * @param sender The command sender
     */
    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage("§e/shops claim <name> §7- Claim the current chunk for a shop");
    }

    /**
     * Check if the subcommand denies non-player senders.
     *
     * @return true if non-player senders are denied, false otherwise
     */
    @Override
    public boolean denyNonPlayer() {
        return true;
    }

    /**
     * Execute the subcommand.
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (args.length < 1) {
            getUsage(sender);
            return false;
        }

        String shopName = args[0];

        // Check if the shop exists
        if (!shopsManager.shopExists(shopName)) {
            sender.sendMessage("§cShop §e" + shopName + "§c does not exist!");
            return false;
        }

        // Claim the chunk
        if (shopsManager.claimChunk(shopName, player.getLocation().getChunk())) {
            sender.sendMessage("§aChunk claimed for shop §e" + shopName + "§a!");
            return true;
        } else {
            sender.sendMessage("§cFailed to claim chunk! It may already be claimed by another shop.");
            return false;
        }
    }

    /**
     * Tab complete the subcommand.
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return A list of tab completions
     */
    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull String[] args) {
        // TODO: Implement tab completion for shop names
        return new ArrayList<>();
    }

    /**
     * Get the permission required to use this subcommand.
     *
     * @return The permission required to use this subcommand
     */
    @Override
    public String getPermission() {
        return "furious.shops.claim";
    }
}