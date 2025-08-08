package com.spillhuset.furious.gui;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.entities.Permission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI-based permission management system.
 * Allows administrators to browse, search, and edit permissions through an inventory interface.
 */
public class PermissionManagerGUI implements Listener {
    private final Furious plugin;
    private final Map<UUID, PermissionGUISession> activeSessions = new HashMap<>();
    private final Map<String, List<String>> permissionPresets = new HashMap<>();

    // Constants for GUI layout
    private static final int ROWS = 6;
    private static final int INVENTORY_SIZE = ROWS * 9;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows of items, bottom row for navigation
    // Using Component for title (Adventure API)
    private static final Component GUI_TITLE_COMPONENT = Component.text("Permission Manager", NamedTextColor.DARK_PURPLE);
    // Legacy string version for backward compatibility
    private static final String GUI_TITLE = PlainTextComponentSerializer.plainText().serialize(GUI_TITLE_COMPONENT);

    /**
     * Creates a new PermissionManagerGUI.
     *
     * @param plugin The plugin instance
     */
    public PermissionManagerGUI(Furious plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initializePermissionPresets();
    }

    /**
     * Initializes predefined permission presets.
     */
    private void initializePermissionPresets() {
        // Admin preset
        List<String> adminPermissions = Arrays.asList(
                "furious.*",
                "furious.admin.*"
        );
        permissionPresets.put("Admin", adminPermissions);

        // Moderator preset
        List<String> moderatorPermissions = Arrays.asList(
                "furious.teleport.admin",
                "furious.homes.*.others",
                "furious.warps.*.others",
                "furious.bank.balance.others",
                "furious.wallet.balance.others",
                "furious.guild.admin.*"
        );
        permissionPresets.put("Moderator", moderatorPermissions);

        // Builder preset
        List<String> builderPermissions = Arrays.asList(
                "furious.teleport.coords",
                "furious.teleport.worldspawn",
                "furious.warps.create",
                "furious.warps.delete",
                "furious.warps.relocate",
                "furious.warps.rename"
        );
        permissionPresets.put("Builder", builderPermissions);

        // VIP preset
        List<String> vipPermissions = Arrays.asList(
                "furious.teleport.bypass.cooldown",
                "furious.teleport.bypass.cost",
                "furious.homes.set.extra.5",
                "furious.warps.bypass.cost"
        );
        permissionPresets.put("VIP", vipPermissions);

        // Economy preset
        List<String> economyPermissions = Arrays.asList(
                "furious.bank.*",
                "furious.wallet.*",
                "furious.shop.*"
        );
        permissionPresets.put("Economy", economyPermissions);

        // Survival preset
        List<String> survivalPermissions = Arrays.asList(
                "furious.teleport.*",
                "furious.homes.*",
                "furious.warps.warp",
                "furious.warps.list",
                "furious.guild.*"
        );
        permissionPresets.put("Survival", survivalPermissions);
    }

    /**
     * Opens the permission manager GUI for a player.
     *
     * @param player The player to open the GUI for
     */
    public void openPermissionManager(Player player) {
        if (!player.hasPermission("furious.admin.permissions")) {
            player.sendMessage(Component.text("You don't have permission to use the permission manager.", NamedTextColor.RED));
            return;
        }

        PermissionGUISession session = new PermissionGUISession(player);
        activeSessions.put(player.getUniqueId(), session);

        openMainMenu(player);
    }

    /**
     * Opens the main menu of the permission manager.
     *
     * @param player The player to open the menu for
     */
    private void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
            Component.text().append(GUI_TITLE_COMPONENT)
                .append(Component.text(" - Main Menu", NamedTextColor.DARK_PURPLE))
                .build());

        // Browse permissions button
        ItemStack browseItem = createGuiItem(Material.BOOK,
                Component.text("Browse Permissions", NamedTextColor.GREEN),
                Component.text("Browse and edit all permissions", NamedTextColor.GRAY));
        inventory.setItem(11, browseItem);

        // Search permissions button
        ItemStack searchItem = createGuiItem(Material.COMPASS,
                Component.text("Search Permissions", NamedTextColor.GREEN),
                Component.text("Search for specific permissions", NamedTextColor.GRAY));
        inventory.setItem(13, searchItem);

        // Permission presets button
        ItemStack presetsItem = createGuiItem(Material.CHEST,
                Component.text("Permission Presets", NamedTextColor.GREEN),
                Component.text("Apply predefined permission sets", NamedTextColor.GRAY));
        inventory.setItem(15, presetsItem);

        // Player permissions button
        ItemStack playerItem = createGuiItem(Material.PLAYER_HEAD,
                Component.text("Player Permissions", NamedTextColor.GREEN),
                Component.text("Manage permissions for specific players", NamedTextColor.GRAY));
        inventory.setItem(29, playerItem);

        // Role editor button
        ItemStack roleItem = createGuiItem(Material.SHIELD,
                Component.text("Role Editor", NamedTextColor.GREEN),
                Component.text("Create and manage permission roles", NamedTextColor.GRAY));
        inventory.setItem(31, roleItem);

        // Inheritance visualization button
        ItemStack inheritanceItem = createGuiItem(Material.KNOWLEDGE_BOOK,
                Component.text("Inheritance Visualization", NamedTextColor.GREEN),
                Component.text("Visualize permission inheritance relationships", NamedTextColor.GRAY));
        inventory.setItem(33, inheritanceItem);

        // Close button
        ItemStack closeItem = createGuiItem(Material.BARRIER,
                Component.text("Close", NamedTextColor.RED),
                Component.text("Close the permission manager", NamedTextColor.GRAY));
        inventory.setItem(49, closeItem);

        player.openInventory(inventory);
    }

    /**
     * Opens the permission browser GUI.
     *
     * @param player The player to open the GUI for
     * @param page The page number to display
     */
    private void openPermissionBrowser(Player player, int page) {
        PermissionGUISession session = activeSessions.get(player.getUniqueId());
        session.setCurrentPage(page);

        List<String> allPermissions = getAllPermissions();
        int totalPages = (int) Math.ceil((double) allPermissions.size() / ITEMS_PER_PAGE);

        if (page > totalPages) {
            page = totalPages;
        }
        if (page < 1) {
            page = 1;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text().append(GUI_TITLE_COMPONENT)
                    .append(Component.text(" - Browse (Page " + page + "/" + totalPages + ")", NamedTextColor.DARK_PURPLE))
                    .build());

        // Add permission items
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allPermissions.size());

        for (int i = startIndex; i < endIndex; i++) {
            String permission = allPermissions.get(i);
            // Check if the permission is enabled for the player using the PermissionManager
            boolean isEnabled = plugin.getPermissionManager().hasPermission(player, permission);

            Material material = isEnabled ? Material.LIME_DYE : Material.GRAY_DYE;
            Component status = isEnabled
                ? Component.text("Enabled", NamedTextColor.GREEN)
                : Component.text("Disabled", NamedTextColor.RED);

            ItemStack item = createGuiItem(material,
                    Component.text(permission, NamedTextColor.YELLOW),
                    Component.text().append(Component.text("Status: ", NamedTextColor.GRAY)).append(status).build(),
                    Component.text("Click to toggle", NamedTextColor.GRAY));

            inventory.setItem(i - startIndex, item);
        }

        // Add navigation buttons
        if (page > 1) {
            ItemStack prevItem = createGuiItem(Material.ARROW,
                    Component.text("Previous Page", NamedTextColor.AQUA),
                    Component.text("Go to page " + (page - 1), NamedTextColor.GRAY));
            inventory.setItem(45, prevItem);
        }

        if (page < totalPages) {
            ItemStack nextItem = createGuiItem(Material.ARROW,
                    Component.text("Next Page", NamedTextColor.AQUA),
                    Component.text("Go to page " + (page + 1), NamedTextColor.GRAY));
            inventory.setItem(53, nextItem);
        }

        // Back button
        ItemStack backItem = createGuiItem(Material.BARRIER,
                Component.text("Back to Main Menu", NamedTextColor.RED),
                Component.text("Return to the main menu", NamedTextColor.GRAY));
        inventory.setItem(49, backItem);

        player.openInventory(inventory);
    }

    /**
     * Opens the permission presets GUI.
     *
     * @param player The player to open the GUI for
     */
    private void openPermissionPresets(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text().append(GUI_TITLE_COMPONENT)
                    .append(Component.text(" - Presets", NamedTextColor.DARK_PURPLE))
                    .build());

        int slot = 0;
        for (Map.Entry<String, List<String>> entry : permissionPresets.entrySet()) {
            String presetName = entry.getKey();
            List<String> permissions = entry.getValue();

            Material material = switch (presetName) {
                case "Admin" -> Material.DIAMOND;
                case "Moderator" -> Material.GOLDEN_SWORD;
                case "Builder" -> Material.BRICK;
                case "VIP" -> Material.EMERALD;
                case "Economy" -> Material.GOLD_INGOT;
                case "Survival" -> Material.IRON_SWORD;
                default -> Material.PAPER;
            };

            List<Component> loreComponents = new ArrayList<>();
            loreComponents.add(Component.text("Contains " + permissions.size() + " permissions", NamedTextColor.GRAY));
            loreComponents.add(Component.text("Click to view and apply", NamedTextColor.GRAY));

            ItemStack item = createGuiItem(material,
                    Component.text(presetName + " Preset", NamedTextColor.GREEN),
                    loreComponents.toArray(new Component[0]));

            inventory.setItem(slot, item);
            slot++;
        }

        // Back button
        ItemStack backItem = createGuiItem(Material.BARRIER,
                Component.text("Back to Main Menu", NamedTextColor.RED),
                Component.text("Return to the main menu", NamedTextColor.GRAY));
        inventory.setItem(49, backItem);

        player.openInventory(inventory);
    }

    /**
     * Opens the preset details GUI.
     *
     * @param player The player to open the GUI for
     * @param presetName The name of the preset to display
     */
    private void openPresetDetails(Player player, String presetName) {
        List<String> permissions = permissionPresets.get(presetName);
        if (permissions == null) {
            player.sendMessage(Component.text("Preset not found: " + presetName, NamedTextColor.RED));
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text().append(GUI_TITLE_COMPONENT)
                    .append(Component.text(" - " + presetName + " Preset", NamedTextColor.DARK_PURPLE))
                    .build());

        // Add permission items
        for (int i = 0; i < Math.min(permissions.size(), 45); i++) {
            String permission = permissions.get(i);

            ItemStack item = createGuiItem(Material.PAPER,
                    Component.text(permission, NamedTextColor.YELLOW),
                    Component.text("Included in " + presetName + " preset", NamedTextColor.GRAY));

            inventory.setItem(i, item);
        }

        // Apply preset button
        ItemStack applyItem = createGuiItem(Material.LIME_CONCRETE,
                Component.text("Apply Preset", NamedTextColor.GREEN),
                Component.text("Apply this preset to a player or role", NamedTextColor.GRAY));
        inventory.setItem(47, applyItem);

        // Back button
        ItemStack backItem = createGuiItem(Material.BARRIER,
                Component.text("Back to Presets", NamedTextColor.RED),
                Component.text("Return to the presets menu", NamedTextColor.GRAY));
        inventory.setItem(49, backItem);

        player.openInventory(inventory);
    }

    /**
     * Creates a GUI item with the specified material, name, and lore.
     *
     * @param material The material of the item
     * @param name The name of the item with legacy color codes
     * @param lore The lore of the item with legacy color codes
     * @return The created ItemStack
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Convert the name string with legacy color codes to a Component
            Component nameComponent = LegacyComponentSerializer.legacySection().deserialize(name);
            meta.displayName(nameComponent);

            if (lore.length > 0) {
                List<Component> loreComponents = new ArrayList<>();
                for (String loreLine : lore) {
                    // Convert each lore line with legacy color codes to a Component
                    Component loreComponent = LegacyComponentSerializer.legacySection().deserialize(loreLine);
                    loreComponents.add(loreComponent);
                }
                meta.lore(loreComponents);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates a GUI item with the specified material, name component, and lore components.
     *
     * @param material The material of the item
     * @param nameComponent The name of the item as a Component
     * @param loreComponents The lore of the item as Components
     * @return The created ItemStack
     */
    private ItemStack createGuiItem(Material material, Component nameComponent, Component... loreComponents) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(nameComponent);

            if (loreComponents.length > 0) {
                meta.lore(Arrays.asList(loreComponents));
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Gets all permissions registered in the plugin.
     *
     * @return A list of all permission nodes
     */
    private List<String> getAllPermissions() {
        // This would be implemented to get all permissions from your permission system
        // For now, we'll return a sample list
        return Arrays.asList(
                "furious.wallet",
                "furious.wallet.pay",
                "furious.wallet.balance.others",
                "furious.wallet.add",
                "furious.wallet.sub",
                "furious.wallet.set",
                "furious.bank.balance",
                "furious.bank.deposit",
                "furious.bank.withdraw",
                "furious.bank.transfer",
                "furious.bank.createaccount",
                "furious.bank.deleteaccount",
                "furious.bank.add",
                "furious.bank.subtract",
                "furious.bank.set",
                "furious.guild.create",
                "furious.guild.invite",
                "furious.guild.join",
                "furious.guild.leave",
                "furious.guild.info",
                "furious.guild.list",
                "furious.guild.kick",
                "furious.guild.disband",
                "furious.guild.transfer",
                "furious.guild.claim",
                "furious.guild.unclaim",
                "furious.guild.claims",
                "furious.guild.mobs",
                "furious.guild.homes",
                "furious.guild.world",
                "furious.guild.set",
                "furious.guild.role",
                "furious.guild.accept",
                "furious.guild.decline",
                "furious.guild.cancelinvite",
                "furious.guild.admin.transfer",
                "furious.guild.admin.unclaim",
                "furious.guild.admin.homes",
                "furious.guild.admin.disband",
                "furious.guild.admin.info",
                "furious.homes.set",
                "furious.homes.move",
                "furious.homes.rename",
                "furious.homes.delete",
                "furious.homes.list",
                "furious.homes.tp",
                "furious.homes.buy",
                "furious.homes.world",
                "furious.homes.set.others",
                "furious.homes.delete.others",
                "furious.homes.move.others",
                "furious.homes.rename.others",
                "furious.homes.tp.others",
                "furious.homes.list.others",
                "furious.warps.create",
                "furious.warps.delete",
                "furious.warps.relocate",
                "furious.warps.rename",
                "furious.warps.cost",
                "furious.warps.passwd",
                "furious.warps.link",
                "furious.warps.list",
                "furious.warps.warp",
                "furious.warps.visibility",
                "furious.warps.unlink",
                "furious.warps.create.others",
                "furious.warps.delete.others",
                "furious.warps.relocate.others",
                "furious.warps.rename.others",
                "furious.warps.cost.others",
                "furious.warps.passwd.others",
                "furious.teleport.request",
                "furious.teleport.accept",
                "furious.teleport.decline",
                "furious.teleport.list",
                "furious.teleport.abort",
                "furious.teleport.deny",
                "furious.teleport.worldconfig",
                "furious.teleport.worlds",
                "furious.teleport.coords",
                "furious.teleport.worldspawn",
                "furious.teleport.setworldspawn",
                "furious.teleport.force",
                "furious.teleport.admin",
                "furious.admin.permissions"
        );
    }

    /**
     * Handles inventory click events for the permission manager GUI.
     *
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        PermissionGUISession session = activeSessions.get(player.getUniqueId());

        if (session == null) {
            return;
        }

        // Get the inventory title as a Component
        Component titleComponent = event.getView().title();
        // Convert to plain text for comparison
        String inventoryTitle = PlainTextComponentSerializer.plainText().serialize(titleComponent);

        // Check if this is our GUI
        if (!inventoryTitle.startsWith(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Handle main menu clicks
        if (inventoryTitle.equals(GUI_TITLE + " - Main Menu")) {
            handleMainMenuClick(player, clickedItem, event.getSlot());
            return;
        }

        // Handle permission browser clicks
        if (inventoryTitle.contains(GUI_TITLE + " - Browse")) {
            handleBrowserClick(player, clickedItem, event.getSlot());
            return;
        }

        // Handle presets menu clicks
        if (inventoryTitle.equals(GUI_TITLE + " - Presets")) {
            handlePresetsClick(player, clickedItem, event.getSlot());
            return;
        }

        // Handle preset details clicks
        if (inventoryTitle.contains(GUI_TITLE + " - ") && inventoryTitle.contains(" Preset")) {
            handlePresetDetailsClick(player, clickedItem, event.getSlot());
            return;
        }
    }

    /**
     * Handles clicks in the main menu.
     *
     * @param player The player who clicked
     * @param clickedItem The item that was clicked
     * @param slot The slot that was clicked
     */
    private void handleMainMenuClick(Player player, ItemStack clickedItem, int slot) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Get the display name as a Component and convert to plain text for comparison
        Component displayNameComponent = meta.displayName();
        if (displayNameComponent == null) {
            return;
        }

        String itemName = PlainTextComponentSerializer.plainText().serialize(displayNameComponent);

        if (itemName.contains("Browse Permissions")) {
            openPermissionBrowser(player, 1);
        } else if (itemName.contains("Search Permissions")) {
            player.sendMessage(Component.text("Please type the permission to search for in chat.", NamedTextColor.YELLOW));
            player.closeInventory();
            // This would be handled by a chat event listener
        } else if (itemName.contains("Permission Presets")) {
            openPermissionPresets(player);
        } else if (itemName.contains("Player Permissions")) {
            player.sendMessage(Component.text("Please type the player name in chat.", NamedTextColor.YELLOW));
            player.closeInventory();
            // Set the session to await chat input for player permissions
            PermissionGUISession session = activeSessions.computeIfAbsent(player.getUniqueId(), k -> new PermissionGUISession(player));
            session.setAwaitingChatInput(true, "player_permissions");
        } else if (itemName.contains("Role Editor")) {
            player.sendMessage(Component.text("Role editor not yet implemented.", NamedTextColor.YELLOW));
        } else if (itemName.contains("Inheritance Visualization")) {
            player.sendMessage(Component.text("Inheritance visualization not yet implemented.", NamedTextColor.YELLOW));
        } else if (itemName.contains("Close")) {
            player.closeInventory();
        }
    }

    /**
     * Handles clicks in the permission browser.
     *
     * @param player The player who clicked
     * @param clickedItem The item that was clicked
     * @param slot The slot that was clicked
     */
    private void handleBrowserClick(Player player, ItemStack clickedItem, int slot) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Get the display name as a Component and convert to plain text for comparison
        Component displayNameComponent = meta.displayName();
        if (displayNameComponent == null) {
            return;
        }

        String itemName = PlainTextComponentSerializer.plainText().serialize(displayNameComponent);
        PermissionGUISession session = activeSessions.get(player.getUniqueId());

        if (itemName.contains("Previous Page")) {
            openPermissionBrowser(player, session.getCurrentPage() - 1);
        } else if (itemName.contains("Next Page")) {
            openPermissionBrowser(player, session.getCurrentPage() + 1);
        } else if (itemName.contains("Back to Main Menu")) {
            openMainMenu(player);
        } else if (slot < 45) {
            // Toggle permission
            // This would be implemented to toggle the permission in your permission system
            player.sendMessage(Component.text("Permission toggle not yet implemented: " +
                    itemName, NamedTextColor.YELLOW));
        }
    }

    /**
     * Handles clicks in the presets menu.
     *
     * @param player The player who clicked
     * @param clickedItem The item that was clicked
     * @param slot The slot that was clicked
     */
    private void handlePresetsClick(Player player, ItemStack clickedItem, int slot) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Get the display name as a Component and convert to plain text for comparison
        Component displayNameComponent = meta.displayName();
        if (displayNameComponent == null) {
            return;
        }

        String itemName = PlainTextComponentSerializer.plainText().serialize(displayNameComponent);

        if (itemName.contains("Back to Main Menu")) {
            openMainMenu(player);
        } else {
            // Extract preset name from item name
            String presetName = itemName.replace(" Preset", "");
            openPresetDetails(player, presetName);
        }
    }

    /**
     * Handles clicks in the preset details menu.
     *
     * @param player The player who clicked
     * @param clickedItem The item that was clicked
     * @param slot The slot that was clicked
     */
    private void handlePresetDetailsClick(Player player, ItemStack clickedItem, int slot) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Get the display name as a Component and convert to plain text for comparison
        Component displayNameComponent = meta.displayName();
        if (displayNameComponent == null) {
            return;
        }

        String itemName = PlainTextComponentSerializer.plainText().serialize(displayNameComponent);

        // Get the inventory title as a Component and extract the preset name
        Component titleComponent = player.getOpenInventory().title();
        String inventoryTitle = PlainTextComponentSerializer.plainText().serialize(titleComponent);
        String presetName = inventoryTitle
                .replace("Permission Manager - ", "")
                .replace(" Preset", "");

        if (itemName.contains("Apply Preset")) {
            player.sendMessage(Component.text("Please type the player or role name in chat to apply the " +
                    presetName + " preset.", NamedTextColor.YELLOW));
            player.closeInventory();
            // Set the session to await chat input for applying a preset
            PermissionGUISession session = activeSessions.computeIfAbsent(player.getUniqueId(), k -> new PermissionGUISession(player));
            session.setTargetPreset(presetName);
            session.setAwaitingChatInput(true, "apply_preset");
        } else if (itemName.contains("Back to Presets")) {
            openPermissionPresets(player);
        }
    }

    /**
     * Handles inventory close events for the permission manager GUI.
     *
     * @param event The inventory close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Get the inventory title as a Component and convert to plain text for comparison
        Component titleComponent = event.getView().title();
        String inventoryTitle = PlainTextComponentSerializer.plainText().serialize(titleComponent);

        if (inventoryTitle.startsWith(GUI_TITLE)) {
            // Only remove the session if the player is not expected to return to the GUI
            // (e.g., if they're entering something in chat)
            PermissionGUISession session = activeSessions.get(player.getUniqueId());
            if (session != null && !session.isAwaitingChatInput()) {
                activeSessions.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Handles chat events for the permission manager GUI.
     * This captures input when a player is expected to type something in chat.
     *
     * @param event The chat event
     */
    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PermissionGUISession session = activeSessions.get(player.getUniqueId());

        // Check if the player has an active session and is awaiting chat input
        if (session != null && session.isAwaitingChatInput()) {
            // Cancel the event to prevent the message from being broadcast
            event.setCancelled(true);

            // Convert the Component message to a String
            String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            String inputType = session.getInputType();

            // Process the input based on the input type
            if ("player_permissions".equals(inputType)) {
                // Handle player permissions input
                openPlayerPermissions(player, input);
            } else if ("apply_preset".equals(inputType)) {
                // Handle apply preset input
                applyPresetToTarget(player, session.getTargetPreset(), input);
            }

            // Reset the awaiting chat input state
            session.setAwaitingChatInput(false, null);
        }
    }

    /**
     * Opens a GUI displaying the permissions for a specific player.
     *
     * @param viewer The player viewing the permissions
     * @param targetPlayerName The name of the player whose permissions to display
     */
    private void openPlayerPermissions(Player viewer, String targetPlayerName) {
        // Try to find the target player
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            viewer.sendMessage(Component.text("Player not found: " + targetPlayerName, NamedTextColor.RED));
            return;
        }

        // Create an inventory for displaying the player's permissions
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text().append(GUI_TITLE_COMPONENT)
                    .append(Component.text(" - " + targetPlayer.getName() + "'s Permissions", NamedTextColor.AQUA))
                    .build());

        // Get all permissions
        List<String> allPermissions = getAllPermissions();

        // Display permissions with their status for the target player
        int slot = 0;
        for (int i = 0; i < Math.min(allPermissions.size(), 45); i++) {
            String permission = allPermissions.get(i);

            // Check if the player has this permission
            boolean hasPermission = targetPlayer.hasPermission(permission);

            // Create an item representing the permission
            Material material = hasPermission ? Material.LIME_DYE : Material.GRAY_DYE;
            Component nameComponent = Component.text(permission, hasPermission ? NamedTextColor.GREEN : NamedTextColor.GRAY);
            Component statusComponent = Component.text(hasPermission ? "Granted" : "Not granted",
                                                     hasPermission ? NamedTextColor.GREEN : NamedTextColor.RED);

            ItemStack item = createGuiItem(material, nameComponent, statusComponent);
            inventory.setItem(slot++, item);
        }

        // Add navigation buttons
        // Back button
        ItemStack backItem = createGuiItem(Material.BARRIER,
                Component.text("Back to Main Menu", NamedTextColor.RED),
                Component.text("Return to the main menu", NamedTextColor.GRAY));
        inventory.setItem(49, backItem);

        // Open the inventory for the viewer
        viewer.openInventory(inventory);
    }

    /**
     * Applies a permission preset to a target player or role.
     *
     * @param admin The admin applying the preset
     * @param presetName The name of the preset to apply
     * @param targetName The name of the player or role to apply the preset to
     */
    private void applyPresetToTarget(Player admin, String presetName, String targetName) {
        List<String> permissions = permissionPresets.get(presetName);
        if (permissions == null) {
            admin.sendMessage(Component.text("Preset not found: " + presetName, NamedTextColor.RED));
            return;
        }

        // Try to find the target player
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            admin.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }

        // Apply the permissions
        // Note: In a real implementation, you would use your permission system to apply these permissions
        // This is a simplified example
        admin.sendMessage(Component.text("Applied " + presetName + " preset to " + targetPlayer.getName(),
                                        NamedTextColor.GREEN));
        admin.sendMessage(Component.text("This would grant " + permissions.size() + " permissions",
                                        NamedTextColor.GRAY));

        // Reopen the main menu
        openMainMenu(admin);
    }

    /**
     * Represents a session for a player using the permission manager GUI.
     */
    private static class PermissionGUISession {
        private final Player player;
        private int currentPage = 1;
        private boolean awaitingChatInput = false;
        private String inputType = null;
        private String targetPreset = null;

        public PermissionGUISession(Player player) {
            this.player = player;
        }

        public Player getPlayer() {
            return player;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public boolean isAwaitingChatInput() {
            return awaitingChatInput;
        }

        public void setAwaitingChatInput(boolean awaitingChatInput, String inputType) {
            this.awaitingChatInput = awaitingChatInput;
            this.inputType = inputType;
        }

        public String getInputType() {
            return inputType;
        }

        public void setTargetPreset(String presetName) {
            this.targetPreset = presetName;
        }

        public String getTargetPreset() {
            return targetPreset;
        }
    }
}