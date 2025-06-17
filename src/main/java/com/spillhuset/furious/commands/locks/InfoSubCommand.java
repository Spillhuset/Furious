package com.spillhuset.furious.commands.locks;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Subcommand for creating an info tool.
 */
public class InfoSubCommand implements SubCommand {
    private final Furious plugin;
    private final NamespacedKey infoKey;
    private final NamespacedKey ownerKey;

    /**
     * Creates a new InfoSubCommand.
     *
     * @param plugin The plugin instance
     */
    public InfoSubCommand(Furious plugin) {
        this.plugin = plugin;
        this.infoKey = new NamespacedKey(plugin, "info_tool");
        this.ownerKey = new NamespacedKey(plugin, "owner_uuid");
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Creates an info tool to check lock ownership.";
    }

    @Override
    public void getUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/locks info", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Toggles the info tool to check who owns a locked block.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Check if player already has the info tool
        ItemStack[] inventory = player.getInventory().getContents();
        for (int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if (item != null && isInfoTool(item)) {
                // Remove the tool (toggle off)
                player.getInventory().setItem(i, null);
                player.sendMessage(Component.text("Lock info tool deactivated.", NamedTextColor.YELLOW));
                return true;
            }
        }

        // Create the info tool
        ItemStack infoTool = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = infoTool.getItemMeta();

        // Set display name and lore
        meta.displayName(Component.text("Lock Info Tool", NamedTextColor.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right-click a block to check ownership", NamedTextColor.YELLOW));
        lore.add(Component.text("Use /locks info to toggle off", NamedTextColor.GRAY));
        meta.lore(lore);

        // Make it look special and prevent it from being placed
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Add persistent data to identify this as an info tool
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(infoKey, PersistentDataType.STRING, "info");
        container.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());

        infoTool.setItemMeta(meta);

        // Give the item to the player
        player.getInventory().addItem(infoTool);
        player.sendMessage(Component.text("Lock info tool activated. Right-click a block to check ownership.", NamedTextColor.GREEN));

        return true;
    }

    /**
     * Checks if an item is an info tool.
     *
     * @param item The item to check
     * @return True if the item is an info tool
     */
    private boolean isInfoTool(ItemStack item) {
        if (item == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(infoKey, PersistentDataType.STRING);
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }

    @Override
    public String getPermission() {
        return "furious.locks.info";
    }
}