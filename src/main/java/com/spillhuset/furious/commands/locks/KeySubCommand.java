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
 * Subcommand for creating a key item.
 */
public class KeySubCommand implements SubCommand {
    private final Furious plugin;
    private final NamespacedKey keyKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey blockTypeKey;

    /**
     * Creates a new KeySubCommand.
     *
     * @param plugin The plugin instance
     */
    public KeySubCommand(Furious plugin) {
        this.plugin = plugin;
        this.keyKey = new NamespacedKey(plugin, "key_item");
        this.ownerKey = new NamespacedKey(plugin, "owner_uuid");
        this.blockTypeKey = new NamespacedKey(plugin, "block_type");
    }

    @Override
    public String getName() {
        return "key";
    }

    @Override
    public String getDescription() {
        return "Creates a key to unlock specific blocks.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/locks key [block_type]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Creates a key that can be used to unlock blocks.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Optionally specify a block type the key can open.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        String blockType = null;
        if (args.length > 1) {
            blockType = args[1].toUpperCase();
            // Validate block type if provided
            try {
                Material.valueOf(blockType);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("Invalid block type: " + args[1], NamedTextColor.RED));
                return true;
            }
        }

        // Create the key item
        ItemStack keyItem = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta meta = keyItem.getItemMeta();

        // Set display name and lore
        meta.displayName(Component.text("Key", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right-click a block to unlock it", NamedTextColor.YELLOW));
        lore.add(Component.text("Owner: " + player.getName(), NamedTextColor.GRAY));

        if (blockType != null) {
            lore.add(Component.text("Opens: " + formatBlockType(blockType), NamedTextColor.GRAY));
        }

        meta.lore(lore);

        // Add persistent data to identify this as a key item
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(keyKey, PersistentDataType.STRING, "key");
        container.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());

        if (blockType != null) {
            container.set(blockTypeKey, PersistentDataType.STRING, blockType);
        }

        keyItem.setItemMeta(meta);

        // Give the item to the player
        player.getInventory().addItem(keyItem);

        if (blockType != null) {
            player.sendMessage(Component.text("You have received a key for " + formatBlockType(blockType) + "!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("You have received a key!", NamedTextColor.GREEN));
        }

        return true;
    }

    /**
     * Formats a block type string to be more readable.
     *
     * @param blockType The block type to format
     * @return The formatted block type
     */
    private String formatBlockType(String blockType) {
        String[] parts = blockType.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String part : parts) {
            if (part.length() > 0) {
                formatted.append(part.charAt(0)).append(part.substring(1).toLowerCase());
                formatted.append(" ");
            }
        }

        return formatted.toString().trim();
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String partial = args[1].toUpperCase();
            for (Material material : Material.values()) {
                if (material.isBlock() && material.name().startsWith(partial)) {
                    completions.add(material.name());
                }
            }
        }

        return completions;
    }

    @Override
    public String getPermission() {
        return "furious.locks.key";
    }
}