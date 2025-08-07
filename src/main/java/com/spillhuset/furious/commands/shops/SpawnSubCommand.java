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
 * Subcommand for setting a shop's spawn location.
 */
public class SpawnSubCommand implements SubCommand {

    private final Furious plugin;
    private final ShopsManager shopsManager;

    /**
     * Constructor for SpawnSubCommand.
     *
     * @param plugin The main plugin instance
     */
    public SpawnSubCommand(Furious plugin) {
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
        return "spawn";
    }

    /**
     * Get the description of the subcommand.
     *
     * @return The description of the subcommand
     */
    @Override
    public String getDescription() {
        return "Set the spawn location for a shop";
    }

    /**
     * Show usage information for the subcommand.
     *
     * @param sender The command sender
     */
    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage("§e/shops spawn §7- Set the spawn location for the shop at your current location");
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

        // Get the shop at the player's location
        com.spillhuset.furious.entities.Shop shop = shopsManager.getShopByChunk(player.getLocation().getChunk());

        // Check if the player is in a shop's claimed chunk
        if (shop == null) {
            sender.sendMessage("§cYou are not in a shop's claimed chunk!");
            return false;
        }

        String shopName = shop.getName();

        // Set the spawn location
        if (shopsManager.setShopSpawn(shopName, player.getLocation())) {
            sender.sendMessage("§aSpawn location set for shop §e" + shopName + "§a!");
            return true;
        } else {
            sender.sendMessage("§cFailed to set spawn location!");
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
        return "furious.shops.spawn";
    }
}