package com.spillhuset.furious.services;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.managers.ArmorStandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles tombstone creation on player death and interactions with it.
 */
public class TombstoneService {
    private final Furious plugin;
    private final ArmorStandManager armorStandManager;

    // Configurable settings
    private final int expirationMinutes;
    private final boolean createScalp;
    private final boolean createWalletToken;
    private final double walletLossPercent;
    private final double xpLossPercent;

    public TombstoneService(Furious plugin) {
        this.plugin = plugin.getInstance();
        this.armorStandManager = this.plugin.armorStandManager;
        // Read config with sensible defaults
        org.bukkit.configuration.file.FileConfiguration cfg = this.plugin.getConfig();
        this.expirationMinutes = Math.max(1, cfg.getInt("tombstone.expiration_minutes", 30));
        this.createScalp = cfg.getBoolean("tombstone.create_scalp", true);
        this.createWalletToken = cfg.getBoolean("tombstone.create_wallet", true);
        this.walletLossPercent = Math.max(0.0, Math.min(100.0, cfg.getDouble("tombstone.wallet_loss_percent", 0.0)));
        this.xpLossPercent = Math.max(0.0, Math.min(100.0, cfg.getDouble("tombstone.xp_loss_percent", 0.0)));
        ensureTombstoneDefaultsPersisted();
    }

    private void ensureTombstoneDefaultsPersisted() {
        try {
            org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
            boolean changed = false;
            if (!cfg.isSet("tombstone.expiration_minutes")) { cfg.set("tombstone.expiration_minutes", expirationMinutes); changed = true; }
            if (!cfg.isSet("tombstone.create_scalp")) { cfg.set("tombstone.create_scalp", createScalp); changed = true; }
            if (!cfg.isSet("tombstone.create_wallet")) { cfg.set("tombstone.create_wallet", createWalletToken); changed = true; }
            if (!cfg.isSet("tombstone.wallet_loss_percent")) { cfg.set("tombstone.wallet_loss_percent", walletLossPercent); changed = true; }
            if (!cfg.isSet("tombstone.xp_loss_percent")) { cfg.set("tombstone.xp_loss_percent", xpLossPercent); changed = true; }
            if (changed) plugin.saveConfig();
        } catch (Throwable ignored) {}
    }

    // Data for each tombstone
    private static class TombData {
        UUID owner;
        UUID standId;
        double wallet;
        int xp;
        boolean walletClaimed;
        boolean xpClaimed;
        boolean scalpGiven;
        long expireAtMs;
        Inventory inventory; // stores all drops
        ItemStack head;
        ItemStack walletToken;
        ItemStack xpBottle;
        BossBar bar;
        int barTaskId;
    }

    private final Map<UUID, TombData> tombs = new ConcurrentHashMap<>(); // key: stand UUID

    private NamespacedKey keyTomb() { return new NamespacedKey(plugin, "tombstone"); }
    private NamespacedKey keyNoDrop() { return new NamespacedKey(plugin, "no_drop"); }

    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.isOp() || player.hasPermission("furious.hidden")) {
            event.deathMessage(null);
        } else {
            event.deathMessage(
                    Component.text("[")
                            .append(Component.text("☠", NamedTextColor.BLACK))
                            .append(Component.text("] "))
                            .append(player.displayName()
                            )
            );
        }
        Location loc = safeLocation(player.getLocation());
        if (loc == null) return;

        // Capture drops and xp
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        int rawXp = event.getDroppedExp();
        // Apply configured XP loss; the reduced amount goes into the tombstone bottle
        int xp = rawXp;
        if (xpLossPercent > 0.0) {
            double keepFactor = Math.max(0.0, Math.min(1.0, (100.0 - xpLossPercent) / 100.0));
            xp = (int) Math.floor(rawXp * keepFactor);
        }
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Capture wallet balance; only move to tombstone if configured to create a wallet token
        double wallet = 0;
        try {
            if (createWalletToken && plugin.walletService != null) {
                wallet = plugin.walletService.getBalance(player.getUniqueId());
                if (walletLossPercent > 0.0) {
                    double keepFactor = Math.max(0.0, Math.min(1.0, (100.0 - walletLossPercent) / 100.0));
                    wallet = wallet * keepFactor;
                }
                // Zero deceased player's wallet when we create a token
                plugin.walletService.setBalance(player.getUniqueId(), 0, "Tombstone created: wallet moved to tombstone token");
            }
        } catch (Throwable ignored) {}

        // Create named armor stand tombstone
        String name = "Tombstone: " + player.getName();
        UUID standId = armorStandManager.create(loc, name);
        if (standId == null) {
            // Fallback: drop items if we failed to create tombstone
            for (ItemStack it : drops) if (it != null) loc.getWorld().dropItemNaturally(loc, it);
            if (xp > 0) {
                final int xpFinal = xp;
                loc.getWorld().spawn(loc, org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(xpFinal));
            }
            return;
        }
        Entity ent = Bukkit.getEntity(standId);
        if (!(ent instanceof ArmorStand stand)) return;

        try { stand.setInvulnerable(true); } catch (Throwable ignored) {}
        try { stand.setArms(true); } catch (Throwable ignored) {}
        try { stand.setBasePlate(false); } catch (Throwable ignored) {}
        try { stand.setGravity(false); } catch (Throwable ignored) {}
        try { stand.setSmall(false); } catch (Throwable ignored) {}
        try { stand.setMarker(false); } catch (Throwable ignored) {}
        try { stand.setInvisible(false); } catch (Throwable ignored) {}
        try { stand.setCustomNameVisible(true); } catch (Throwable ignored) {}

        // Equip armorstand with player's equipment snapshot
        try { stand.getEquipment().setArmorContents(player.getInventory().getArmorContents()); } catch (Throwable ignored) {}
        try { stand.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand()); } catch (Throwable ignored) {}
        try { stand.getEquipment().setItemInOffHand(player.getInventory().getItemInOffHand()); } catch (Throwable ignored) {}

        // Mark stand with PDC so we can recognize it
        try {
            PersistentDataContainer pdc = stand.getPersistentDataContainer();
            pdc.set(keyTomb(), PersistentDataType.INTEGER, 1);
        } catch (Throwable ignored) {}

        // Prepare tomb inventory (size to fit items; use 54 max). Include custom items conditionally.
        int customCount = 1; // XP bottle is always created
        if (createScalp) customCount++;
        if (createWalletToken && plugin.walletService != null) customCount++;
        int size = Math.min(54, Math.max(9, (((drops.size() + customCount) + 8) / 9) * 9));
        Inventory inv = Bukkit.createInventory(null, size, Component.text(name));
        for (ItemStack it : drops) {
            if (it != null && it.getType() != Material.AIR) inv.addItem(it);
        }

        // Create custom items and add to tomb inventory (no-drop)
        ItemStack scalp = null;
        if (createScalp) {
            scalp = tagNoDrop(makeScalp(player));
            try { inv.addItem(scalp); } catch (Throwable ignored) {}
        }
        ItemStack walletToken = null;
        if (createWalletToken && plugin.walletService != null) {
            walletToken = tagNoDrop(makeWalletToken(player.getName(), wallet));
            try { inv.addItem(walletToken); } catch (Throwable ignored) {}
        }
        ItemStack xpBottle = tagNoDrop(makeXpBottle(player.getName(), xp));
        try { inv.addItem(xpBottle); } catch (Throwable ignored) {}

        TombData data = new TombData();
        data.owner = player.getUniqueId();
        data.standId = standId;
        data.wallet = wallet;
        data.xp = xp;
        data.inventory = inv;
        data.head = scalp;
        data.walletToken = walletToken;
        data.xpBottle = xpBottle;
        data.expireAtMs = System.currentTimeMillis() + (long) expirationMinutes * 60L * 1000L;

        tombs.put(standId, data);

        // Register cleanup on stand death (unexpected)
        armorStandManager.register(standId, () -> cleanup(standId, false));

        // Schedule timed removal
        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanup(standId, true), (long) expirationMinutes * 60L * 20L);

        // Owner bossbar countdown (optional): show to owner if online
        try {
            Player ownerPlayer = Bukkit.getPlayer(data.owner);
            if (ownerPlayer != null && ownerPlayer.isOnline()) {
                int totalSeconds = expirationMinutes * 60;
                String initialTitle = "Your tombstone expires in " + String.format(java.util.Locale.ROOT, "%02d:%02d", expirationMinutes, 0);
                BossBar bar = Bukkit.createBossBar(initialTitle, BarColor.PURPLE, BarStyle.SEGMENTED_10);
                data.bar = bar;
                bar.setProgress(1.0);
                bar.addPlayer(ownerPlayer);
                data.barTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        long msLeft = data.expireAtMs - System.currentTimeMillis();
                        if (msLeft <= 0 || !tombs.containsKey(standId)) {
                            try { Bukkit.getScheduler().cancelTask(data.barTaskId); } catch (Throwable ignored) {}
                            try { if (data.bar != null) data.bar.removeAll(); } catch (Throwable ignored) {}
                            return;
                        }
                        int secondsLeft = (int)Math.max(0L, msLeft / 1000L);
                        int mins = secondsLeft / 60;
                        int secs = secondsLeft % 60;
                        try {
                            data.bar.setTitle("Your tombstone expires in " + String.format(java.util.Locale.ROOT, "%02d:%02d", mins, secs));
                            double progress = Math.max(0.0, Math.min(1.0, secondsLeft / (double) totalSeconds));
                            data.bar.setProgress(progress);
                        } catch (Throwable ignored) {}
                    }
                }, 0L, 20L);
            }
        } catch (Throwable ignored) {}
    }

    private void cleanup(UUID standId, boolean removeEntity) {
        TombData data = tombs.remove(standId);
        if (data == null) return;
        try { if (armorStandManager != null) armorStandManager.unregister(standId); } catch (Throwable ignored) {}
        // Cleanup bossbar/task if any
        try {
            if (data.bar != null) {
                data.bar.removeAll();
            }
            if (data.barTaskId > 0) {
                Bukkit.getScheduler().cancelTask(data.barTaskId);
            }
        } catch (Throwable ignored) {}
        if (removeEntity) {
            Entity ent = Bukkit.getEntity(standId);
            if (ent != null) ent.remove();
        }
        // No item drops on cleanup per spec (tombstone disappears)
    }

    /**
     * Clears all existing tombstones, removing their ArmorStand entities and internal tracking.
     * @return number of tombstones cleared
     */
    public int clearAll() {
        List<UUID> ids = new ArrayList<>(tombs.keySet());
        int count = 0;
        for (UUID id : ids) {
            try {
                cleanup(id, true);
                count++;
            } catch (Throwable ignored) {}
        }
        return count;
    }

    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;
        if (!isTomb(stand)) return;
        event.setCancelled(true);
        Player clicker = event.getPlayer();
        TombData data = tombs.get(stand.getUniqueId());
        if (data == null) return;
        // Only open the tomb inventory; do not directly grant items or balances.
        if (data.inventory != null) {
            clicker.openInventory(data.inventory);
        }
    }

    public void onArmorStandDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        if (!isTomb(stand)) return;
        event.setCancelled(true); // indestructible
    }

    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (isNoDrop(stack)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You cannot drop this item."));
        }
    }

    public void onChunkUnload(ChunkUnloadEvent event) {
        // Ensure tomb stand persists; Spigot handles entities, but we don't need special action.
        // This method exists in case future persistence is needed.
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        UUID standToCleanup = null;
        for (Map.Entry<UUID, TombData> e : tombs.entrySet()) {
            TombData data = e.getValue();
            if (data != null && data.inventory == inv) {
                if (isInventoryEmpty(inv)) {
                    standToCleanup = e.getKey();
                }
                break;
            }
        }
        if (standToCleanup != null) {
            UUID id = standToCleanup;
            // Run next tick to ensure inventory close flow completes
            Bukkit.getScheduler().runTask(plugin, () -> cleanup(id, true));
        }
    }

    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        TombData target = null;
        UUID standId = null;
        for (Map.Entry<UUID, TombData> e : tombs.entrySet()) {
            if (e.getValue() != null && e.getValue().inventory == inv) {
                target = e.getValue();
                standId = e.getKey();
                break;
            }
        }
        if (target == null) return;
        // Determine clicked item
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        Player clicker = event.getWhoClicked() instanceof Player p ? p : null;
        if (clicker == null) return;
        ItemMeta meta = null;
        try { meta = current.getItemMeta(); } catch (Throwable ignored) {}
        String display = null;
        try { display = meta != null && meta.displayName() != null ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName()) : null; } catch (Throwable ignored) {}

        // Wallet claim by clicking the wallet token (disappear on click, credit player)
        if (!target.walletClaimed && plugin.walletService != null && display != null && display.equals("Wallet of " + Bukkit.getOfflinePlayer(target.owner).getName())) {
            try {
                double amount = target.wallet;
                if (amount > 0) {
                    // Owner's balance was already zeroed at death; only credit the clicker
                    plugin.walletService.addBalance(clicker.getUniqueId(), amount, "Looted wallet of " + Bukkit.getOfflinePlayer(target.owner).getName());
                    clicker.sendMessage(Component.text("You claimed " + plugin.walletService.formatAmount(amount) + " from the wallet."));
                }
                target.walletClaimed = true;
                // Remove the clicked wallet item and cancel the event so it doesn't move to player inventory
                event.setCancelled(true);
                try { event.setCurrentItem(new ItemStack(Material.AIR)); } catch (Throwable ignored2) {}
                try { clicker.updateInventory(); } catch (Throwable ignored3) {}
            } catch (Throwable ignored) {}
        }
        // XP claim by clicking the XP bottle (disappear on click, grant XP)
        if (!target.xpClaimed && display != null && display.equals("Experience of " + Bukkit.getOfflinePlayer(target.owner).getName())) {
            try {
                int amount = target.xp;
                if (amount > 0) {
                    clicker.giveExp(amount);
                    clicker.sendMessage(Component.text("You absorbed " + amount + " XP from the tombstone."));
                }
                target.xpClaimed = true;
                // Remove the clicked XP item and cancel the event so it doesn't move to player inventory
                event.setCancelled(true);
                try { event.setCurrentItem(new ItemStack(Material.AIR)); } catch (Throwable ignored2) {}
                try { clicker.updateInventory(); } catch (Throwable ignored3) {}
            } catch (Throwable ignored) {}
        }
    }

    private boolean isInventoryEmpty(Inventory inv) {
        try {
            if (inv == null) return true;
            ItemStack[] contents = inv.getContents();
            if (contents == null) return true;
            for (ItemStack it : contents) {
                if (it != null && it.getType() != Material.AIR) {
                    return false;
                }
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isTomb(ArmorStand stand) {
        try {
            PersistentDataContainer pdc = stand.getPersistentDataContainer();
            Integer v = pdc.get(keyTomb(), PersistentDataType.INTEGER);
            return v != null && v == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ItemStack tagNoDrop(ItemStack item) {
        if (item == null) return null;
        try {
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(keyNoDrop(), PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
        } catch (Throwable ignored) {}
        return item;
    }

    private boolean isNoDrop(ItemStack item) {
        if (item == null) return false;
        try {
            ItemMeta meta = item.getItemMeta();
            Integer v = meta.getPersistentDataContainer().get(keyNoDrop(), PersistentDataType.INTEGER);
            return v != null && v == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null) return;
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            for (ItemStack it : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), it);
            }
        }
    }

    private ItemStack makeScalp(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        try {
            ItemMeta metaBase = head.getItemMeta();
            if (metaBase instanceof SkullMeta meta) {
                meta.setOwningPlayer(player);
                meta.displayName(Component.text("Scalp of " + player.getName()));
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                try { head.setItemMeta(meta); } catch (Throwable ignored) {}
            } else {
                ItemMeta meta = head.getItemMeta();
                meta.displayName(Component.text("Scalp of " + player.getName()));
                head.setItemMeta(meta);
            }
        } catch (Throwable ignored) {}
        // Cosmetic: no enchantment to ensure compatibility across versions
        return head;
    }

    private ItemStack makeWalletToken(String playerName, double amount) {
        ItemStack nugget = new ItemStack(Material.GOLD_NUGGET);
        try {
            ItemMeta meta = nugget.getItemMeta();
            meta.displayName(Component.text("Wallet of " + playerName));
            List<Component> lore = new ArrayList<>();
            String amt = plugin.walletService != null ? plugin.walletService.formatAmount(amount) : String.format(java.util.Locale.ROOT, "%.2f", amount);
            lore.add(Component.text("Balance: " + amt));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            nugget.setItemMeta(meta);
        } catch (Throwable ignored) {}
        return nugget;
    }

    private ItemStack makeXpBottle(String playerName, int xp) {
        ItemStack bottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        try {
            ItemMeta meta = bottle.getItemMeta();
            meta.displayName(Component.text("Experience of " + playerName));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("XP: " + xp));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            bottle.setItemMeta(meta);
        } catch (Throwable ignored) {}
        return bottle;
    }

    private Location safeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        // if in the air, lower to ground up to 5 blocks
        Location base = loc.clone();
        for (int i = 0; i < 5; i++) {
            if (base.getBlock().getType().isSolid()) {
                return base.add(0, 1.0, 0);
            }
            base.subtract(0, 1.0, 0);
        }
        return loc;
    }
}
