package com.spillhuset.furious.commands.shops;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.ShopsManager;
import com.spillhuset.furious.misc.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Subcommand for deleting shops with a confirmation step.
 */
public class DeleteSubCommand implements SubCommand {

    private final Furious plugin;
    private final ShopsManager shopsManager;
    private final Map<UUID, String> pendingDeletions;

    /**
     * Constructor for DeleteSubCommand.
     *
     * @param plugin The main plugin instance
     */
    public DeleteSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.shopsManager = plugin.getShopsManager();
        this.pendingDeletions = new HashMap<>();
    }

    /**
     * Get the name of the subcommand.
     *
     * @return The name of the subcommand
     */
    @Override
    public String getName() {
        return "delete";
    }

    /**
     * Get the description of the subcommand.
     *
     * @return The description of the subcommand
     */
    @Override
    public String getDescription() {
        return "Delete a shop";
    }

    /**
     * Show usage information for the subcommand.
     *
     * @param sender The command sender
     */
    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage("§e/shops delete <name> §7- Delete a shop");
        sender.sendMessage("§e/shops delete confirm §7- Confirm deletion of a shop");
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
     * Execute the delete command.
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

        UUID playerId = player.getUniqueId();

        // Check if this is a confirmation
        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            return handleConfirmation(player, playerId);
        }

        // Check if a shop name was provided
        if (args.length == 0) {
            getUsage(sender);
            return false;
        }

        String shopName = args[0];

        // Check if the shop exists
        if (!shopsManager.shopExists(shopName)) {
            sender.sendMessage("§cShop '" + shopName + "' does not exist.");
            return false;
        }

        // Store the pending deletion
        pendingDeletions.put(playerId, shopName);

        // Send warning message
        sender.sendMessage("§eWARNING: You are about to delete the shop '" + shopName + "' and all its associations.");
        sender.sendMessage("§eThis action cannot be undone. Type §6/shops delete confirm§e to proceed.");

        return true;
    }

    /**
     * Handle the confirmation of a shop deletion.
     *
     * @param player   The player confirming the deletion
     * @param playerId The UUID of the player
     * @return true if the deletion was successful, false otherwise
     */
    private boolean handleConfirmation(Player player, UUID playerId) {
        // Check if there's a pending deletion for this player
        if (!pendingDeletions.containsKey(playerId)) {
            player.sendMessage("§cYou don't have any pending shop deletions.");
            return false;
        }

        String shopName = pendingDeletions.get(playerId);

        // Implement the actual deletion logic
        boolean deleted = shopsManager.deleteShop(shopName);

        if (deleted) {
            player.sendMessage("§aShop '" + shopName + "' has been deleted.");
            pendingDeletions.remove(playerId);
            return true;
        } else {
            player.sendMessage("§cFailed to delete shop '" + shopName + "'.");
            return false;
        }
    }

    /**
     * Tab complete the command.
     *
     * @param sender The command sender
     * @param args   The command arguments
     * @return A list of tab completions
     */
    @Override
    public List<String> tabComplete(CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!checkPermission(sender, false)) {
            return completions;
        }

        if (args.length == 1) {
            if ("confirm".startsWith(args[0].toLowerCase())) {
                completions.add("confirm");
            }

            // Add shop names that start with the current argument
            for (String shopName : shopsManager.getAllShopNames()) {
                if (shopName.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(shopName);
                }
            }
        }

        return completions;
    }

    /**
     * Get the permission required to use this subcommand.
     *
     * @return The permission required to use this subcommand
     */
    @Override
    public String getPermission() {
        return "furious.shops.delete";
    }
}