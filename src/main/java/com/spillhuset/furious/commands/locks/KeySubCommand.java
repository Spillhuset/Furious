package com.spillhuset.furious.commands.locks;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Guild;
import com.spillhuset.furious.enums.GuildRole;
import com.spillhuset.furious.misc.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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
    private final NamespacedKey guildKey;

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
        this.guildKey = new NamespacedKey(plugin, "guild_key");
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
        sender.sendMessage(Component.text("/locks key [block_type] [guild]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Creates a key that can be used to unlock blocks.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Optionally specify a block type the key can open.", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Add 'guild' to create a guild key (costs " + plugin.getLocksManager().getGuildKeyCost() + "S).", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Player keys cost " + plugin.getLocksManager().getPlayerKeyCost() + "S, guild keys cost " + plugin.getLocksManager().getGuildKeyCost() + "S.", NamedTextColor.YELLOW));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        // Parse arguments
        String blockType = null;
        boolean isGuildKey = false;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("guild")) {
                isGuildKey = true;
            } else if (blockType == null) {
                blockType = args[i].toUpperCase();
                // Validate block type if provided
                try {
                    Material.valueOf(blockType);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("Invalid block type: " + args[i], NamedTextColor.RED));
                    return true;
                }
            }
        }

        // Check if player is in a guild if they want to create a guild key
        if (isGuildKey) {
            if (!plugin.getGuildManager().isInGuild(player.getUniqueId())) {
                sender.sendMessage(Component.text("You must be in a guild to create a guild key!", NamedTextColor.RED));
                return true;
            }

            // Check if player has appropriate guild role
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            GuildRole minRole = GuildRole.valueOf(plugin.getLocksManager().getGuildKeyMinRole());
            if (guild == null || !guild.hasRole(player.getUniqueId(), minRole)) {
                sender.sendMessage(Component.text("You must be a " + minRole.toString().toLowerCase() + " or higher to create guild keys!", NamedTextColor.RED));
                return true;
            }
        }

        // Calculate cost
        double cost = isGuildKey ? plugin.getLocksManager().getGuildKeyCost() : plugin.getLocksManager().getPlayerKeyCost();

        // Check if player has enough money
        if (!plugin.getWalletManager().has(player, cost)) {
            sender.sendMessage(Component.text("You don't have enough money! Creating a " +
                    (isGuildKey ? "guild" : "player") + " key costs " +
                    plugin.getWalletManager().formatAmount(cost) + ".", NamedTextColor.RED));
            return true;
        }

        // Withdraw money
        if (!plugin.getWalletManager().withdraw(player, cost)) {
            sender.sendMessage(Component.text("Failed to withdraw money!", NamedTextColor.RED));
            return true;
        }

        // Create the key item
        ItemStack keyItem = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta meta = keyItem.getItemMeta();

        // Set display name and lore
        meta.displayName(Component.text(isGuildKey ? "Guild Key" : "Key", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right-click a block to unlock it", NamedTextColor.YELLOW));

        if (isGuildKey) {
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            lore.add(Component.text("Guild: " + guild.getName(), NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("Owner: " + player.getName(), NamedTextColor.GRAY));
        }

        if (blockType != null) {
            lore.add(Component.text("Opens: " + formatBlockType(blockType), NamedTextColor.GRAY));
        }

        meta.lore(lore);

        // Add persistent data to identify this as a key item
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(keyKey, PersistentDataType.STRING, "key");
        container.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());

        if (isGuildKey) {
            Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
            container.set(guildKey, PersistentDataType.STRING, guild.getId().toString());
        }

        if (blockType != null) {
            container.set(blockTypeKey, PersistentDataType.STRING, blockType);
        }

        keyItem.setItemMeta(meta);

        // Give the item to the player
        player.getInventory().addItem(keyItem);

        // Send confirmation message
        StringBuilder message = new StringBuilder();
        message.append("You have received a ");
        if (isGuildKey) {
            message.append("guild ");
        }
        message.append("key");
        if (blockType != null) {
            message.append(" for ").append(formatBlockType(blockType));
        }
        message.append("!");

        player.sendMessage(Component.text(message.toString(), NamedTextColor.GREEN));
        player.sendMessage(Component.text("You paid " + plugin.getWalletManager().formatAmount(cost) +
                " for this key.", NamedTextColor.YELLOW));

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
            if (!part.isEmpty()) {
                formatted.append(part.charAt(0)).append(part.substring(1).toLowerCase());
                formatted.append(" ");
            }
        }

        return formatted.toString().trim();
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        if (args.length == 2) {
            String partial = args[1].toLowerCase();

            // Add "guild" option if player is in a guild and has appropriate role
            if ("guild".startsWith(partial) && canCreateGuildKey(player)) {
                completions.add("guild");
            }

            // Add block types
            String partialUpper = partial.toUpperCase();
            for (Material material : Material.values()) {
                if (material.isBlock() && material.name().startsWith(partialUpper)) {
                    completions.add(material.name());
                }
            }
        } else if (args.length == 3) {
            String partial = args[2].toLowerCase();

            // If first argument is not "guild", suggest "guild" if player has permission
            if (!args[1].equalsIgnoreCase("guild") && "guild".startsWith(partial) && canCreateGuildKey(player)) {
                completions.add("guild");
            }

            // If first argument is "guild", suggest block types
            if (args[1].equalsIgnoreCase("guild")) {
                String partialUpper = partial.toUpperCase();
                for (Material material : Material.values()) {
                    if (material.isBlock() && material.name().startsWith(partialUpper)) {
                        completions.add(material.name());
                    }
                }
            }
        }

        return completions;
    }

    /**
     * Checks if a player can create a guild key.
     *
     * @param player The player to check
     * @return true if the player can create a guild key, false otherwise
     */
    private boolean canCreateGuildKey(Player player) {
        // Check if player is in a guild
        if (!plugin.getGuildManager().isInGuild(player.getUniqueId())) {
            return false;
        }

        // Check if player has appropriate guild role
        Guild guild = plugin.getGuildManager().getPlayerGuild(player.getUniqueId());
        GuildRole minRole = GuildRole.valueOf(plugin.getLocksManager().getGuildKeyMinRole());
        return guild != null && guild.hasRole(player.getUniqueId(), minRole);
    }

    @Override
    public String getPermission() {
        return "furious.locks.key";
    }
}
