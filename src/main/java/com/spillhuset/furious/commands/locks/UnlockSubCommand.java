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
 * Subcommand for creating an unlock item.
 */
public class UnlockSubCommand implements SubCommand {
    private final Furious plugin;
    private final NamespacedKey unlockKey;

    /**
     * Creates a new UnlockSubCommand.
     *
     * @param plugin The plugin instance
     */
    public UnlockSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.unlockKey = new NamespacedKey(plugin, "unlock_item");
    }

    @Override
    public String getName() {
        return "unlock";
    }

    @Override
    public String getDescription() {
        return "Creates an unlock item to unlock blocks.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/locks unlock", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Creates an unlock item that can be used to unlock blocks.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Create the unlock item
        ItemStack unlockItem = new ItemStack(Material.GOLD_INGOT, 1);
        ItemMeta meta = unlockItem.getItemMeta();

        // Set display name and lore
        meta.displayName(Component.text("Unlock", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right-click a block to unlock it", NamedTextColor.YELLOW));
        lore.add(Component.text("Owner: " + player.getName(), NamedTextColor.GRAY));
        meta.lore(lore);

        // Add persistent data to identify this as an unlock item
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(unlockKey, PersistentDataType.STRING, "unlock");

        unlockItem.setItemMeta(meta);

        // Give the item to the player
        player.getInventory().addItem(unlockItem);
        player.sendMessage(Component.text("You have received an unlock item!", NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.locks.unlock";
    }
}