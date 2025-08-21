package com.spillhuset.furious.commands.LocksCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
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
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                if (p.getName() != null && p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(p.getName());
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this command.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(player, "Usage: /locks key <player|uuid>");
            return true;
        }
        UUID targetUuid = null;
        // Try player name first
        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (off != null) {
            targetUuid = off.getUniqueId();
        }
        if (targetUuid == null) {
            try {
                targetUuid = UUID.fromString(args[1]);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (targetUuid == null) {
            Components.sendErrorMessage(player, "Unknown player or invalid UUID.");
            return true;
        }
        // Create the key item (tripwire hook)
        ItemStack item = new ItemStack(org.bukkit.Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Key", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Opens locks owned by:", NamedTextColor.GRAY));
        lore.add(Component.text(targetUuid.toString(), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "locks_key_owner");
        pdc.set(key, PersistentDataType.STRING, targetUuid.toString());
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        Components.sendSuccessMessage(player, "You received a Key bound to " + targetUuid + ".");
        return true;
    }
}
