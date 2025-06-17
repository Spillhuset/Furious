package com.spillhuset.furious.commands.locks;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Subcommand for creating a lock item.
 */
public class LockSubCommand implements SubCommand {
    private final Furious plugin;
    private final NamespacedKey lockKey;

    /**
     * Creates a new LockSubCommand.
     *
     * @param plugin The plugin instance
     */
    public LockSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.lockKey = new NamespacedKey(plugin, "lock_item");
    }

    @Override
    public String getName() {
        return "lock";
    }

    @Override
    public String getDescription() {
        return "Creates a lock item to lock blocks.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/locks lock", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Creates a lock item that can be used to lock blocks.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Create the lock item
        ItemStack lockItem = new ItemStack(Material.IRON_INGOT, 1);
        ItemMeta meta = lockItem.getItemMeta();

        // Set display name and lore
        meta.displayName(Component.text("Lock", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right-click a block to lock it", NamedTextColor.YELLOW));
        lore.add(Component.text("Owner: " + player.getName(), NamedTextColor.GRAY));
        meta.lore(lore);

        // Add persistent data to identify this as a lock item
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(lockKey, PersistentDataType.STRING, "lock");

        lockItem.setItemMeta(meta);

        // Give the item to the player
        player.getInventory().addItem(lockItem);
        player.sendMessage(Component.text("You have received a lock item!", NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.locks.lock";
    }
}