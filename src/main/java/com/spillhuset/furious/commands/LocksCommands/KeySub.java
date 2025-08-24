package com.spillhuset.furious.commands.LocksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class KeySub implements SubCommandInterface {
    private final Furious plugin;

    public KeySub(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public String getName() {
        return "key";
    }

    @Override
    public String getPermission() {
        return "furious.locks";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // No arguments needed; players can only make their own key
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this command.");
            return true;
        }

        // Expect no additional arguments after subcommand; keys are always self-bound
        if (args.length > 1) {
            Components.sendErrorMessage(player, "Usage: /locks key — keys are always bound to yourself; you cannot create keys for other players.");
            return true;
        }

        // Cost to craft a personal key (configurable)
        double cost = plugin.getConfig().getDouble("locks.key-cost", 10.0);
        double bal = plugin.walletService == null ? 0.0 : plugin.walletService.getBalance(player.getUniqueId());
        if (plugin.walletService != null && cost > 0) {
            if (bal < cost) {
                Components.sendErrorMessage(player, "Not enough money to craft a key. Cost: " + plugin.walletService.formatAmount(cost) + ". Balance: " + plugin.walletService.formatAmount(bal) + ".");
                return true;
            }
            boolean ok = plugin.walletService.subBalance(player.getUniqueId(), cost, "Craft personal lock key");
            if (!ok) {
                Components.sendErrorMessage(player, "Payment failed. You need " + plugin.walletService.formatAmount(cost) + ".");
                return true;
            }
        }

        UUID targetUuid = player.getUniqueId();

        // Create the key item (tripwire hook)
        ItemStack item = new ItemStack(org.bukkit.Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Key", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Opens locks owned by:", NamedTextColor.GRAY));
        // Include player name in lore for readability
        lore.add(Component.text(player.getName(), NamedTextColor.GOLD));
        // Keep UUID for technical identification/debugging
        lore.add(Component.text(targetUuid.toString(), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "locks_key_owner");
        pdc.set(key, PersistentDataType.STRING, targetUuid.toString());
        item.setItemMeta(meta);
        player.getInventory().addItem(item);

        if (cost > 0 && plugin.walletService != null) {
            Components.sendSuccessMessage(player, "Crafted a personal Key for " + player.getName() + " for " + plugin.walletService.formatAmount(cost) + ".");
        } else {
            Components.sendSuccessMessage(player, "You received a personal Key bound to yourself.");
        }
        return true;
    }
}
