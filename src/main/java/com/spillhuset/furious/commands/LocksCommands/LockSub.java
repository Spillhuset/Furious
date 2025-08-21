package com.spillhuset.furious.commands.LocksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class LockSub implements SubCommandInterface {
    private final Furious plugin;

    public LockSub(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public String getName() {
        return "lock";
    }

    @Override
    public String getPermission() {
        return "furious.locks";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this command.");
            return true;
        }
        // Give a lock-tool (sign)
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.OAK_SIGN);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("Lock Tool", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("Right-click a block to lock it", NamedTextColor.GRAY));
        lore.add(net.kyori.adventure.text.Component.text("Cannot be dropped. Consumed on use", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "locks_tool");
        pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, "lock");
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        Components.sendSuccessMessage(player, "You received a Lock Tool.");
        return true;
    }
}
