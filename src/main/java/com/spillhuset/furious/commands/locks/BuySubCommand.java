package com.spillhuset.furious.commands.locks;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.LocksManager;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for purchasing additional lock slots.
 */
public class BuySubCommand implements SubCommand {
    private final Furious plugin;

    /**
     * Creates a new BuySubCommand.
     *
     * @param plugin The plugin instance
     */
    public BuySubCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getDescription() {
        return "Purchase additional lock slots.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/locks buy <door|container|block> <amount>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Purchase additional lock slots for the specified type.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Costs:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("- Doors: " + plugin.getLocksManager().getDoorLockCost() + " " + plugin.getWalletManager().getCurrencyName() + " per slot", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("- Containers: " + plugin.getLocksManager().getContainerLockCost() + " " + plugin.getWalletManager().getCurrencyName() + " per slot", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("- Blocks: " + plugin.getLocksManager().getBlockLockCost() + " " + plugin.getWalletManager().getCurrencyName() + " per slot", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            getUsage(sender);
            return true;
        }

        String typeStr = args[1].toLowerCase();
        LocksManager.LockType lockType;

        // Parse lock type
        switch (typeStr) {
            case "door", "doors" -> lockType = LocksManager.LockType.DOOR;
            case "container", "containers", "chest", "chests" -> lockType = LocksManager.LockType.CONTAINER;
            case "block", "blocks", "utility", "utilities" -> lockType = LocksManager.LockType.BLOCK;
            default -> {
                sender.sendMessage(Component.text("Invalid lock type! Use door, container, or block.", NamedTextColor.RED));
                return true;
            }
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                sender.sendMessage(Component.text("Amount must be a positive number!", NamedTextColor.RED));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount! Please enter a number.", NamedTextColor.RED));
            return true;
        }

        // Calculate cost
        int costPerSlot;
        switch (lockType) {
            case DOOR -> costPerSlot = plugin.getLocksManager().getDoorLockCost();
            case CONTAINER -> costPerSlot = plugin.getLocksManager().getContainerLockCost();
            case BLOCK -> costPerSlot = plugin.getLocksManager().getBlockLockCost();
            default -> costPerSlot = 0;
        }

        int totalCost = costPerSlot * amount;

        // Check if player has enough money
        if (!plugin.getWalletManager().has(player, totalCost)) {
            sender.sendMessage(Component.text("You don't have enough money! You need " +
                    plugin.getWalletManager().formatAmount(totalCost) + ".", NamedTextColor.RED));
            return true;
        }

        // Purchase the slots
        if (plugin.getLocksManager().purchaseLockSlots(player, lockType, amount)) {
            sender.sendMessage(Component.text("You have purchased " + amount + " additional " +
                    typeStr + " lock slots for " + plugin.getWalletManager().formatAmount(totalCost) + "!", NamedTextColor.GREEN));

            // Show current limits
            int currentCount = plugin.getLocksManager().getPlayerLockCount(player.getUniqueId(), lockType);
            int maxLocks = plugin.getLocksManager().getPlayerMaxLocks(player.getUniqueId(), lockType);
            sender.sendMessage(Component.text("You now have " + currentCount + "/" + maxLocks + " " +
                    typeStr + " locks.", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Failed to purchase lock slots!", NamedTextColor.RED));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            List<String> types = List.of("door", "container", "block");
            for (String type : types) {
                if (type.startsWith(partial)) {
                    completions.add(type);
                }
            }
        } else if (args.length == 3) {
            String partial = args[2].toLowerCase();
            List<String> amounts = List.of("1", "5", "10");
            for (String amount : amounts) {
                if (amount.startsWith(partial)) {
                    completions.add(amount);
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.locks.buy";
    }
}