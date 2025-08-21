package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * On chunk load, cleans up orphan ArmorStands that belong to this plugin
 * and ensures missing ArmorStands for homes/warps/guild homes/shops/banks are recreated.
 */
public class ChunkArmorStandSanitizer implements Listener {
    private final Furious plugin;
    private final NamespacedKey managedKey;

    public ChunkArmorStandSanitizer(Furious plugin) {
        this.plugin = plugin.getInstance();
        this.managedKey = new NamespacedKey(this.plugin, "managed");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Defer a tick to ensure entities are fully available after load
        plugin.getServer().getScheduler().runTask(plugin, () -> sanitizeChunk(event.getChunk()));
    }

    private void sanitizeChunk(Chunk chunk) {
        try {
            for (Entity entity : chunk.getEntities()) {
                if (!(entity instanceof ArmorStand stand)) continue;

                if (!isManagedStand(stand)) continue; // ignore stands not belonging to this plugin

                UUID id = stand.getUniqueId();
                boolean referenced = false;
                try { if (plugin.homesService != null) referenced = referenced || plugin.homesService.hasArmorStand(id); } catch (Throwable ignored) {}
                try { if (plugin.guildHomesService != null) referenced = referenced || plugin.guildHomesService.hasArmorStand(id); } catch (Throwable ignored) {}
                try { if (plugin.warpsService != null) referenced = referenced || plugin.warpsService.hasArmorStand(id); } catch (Throwable ignored) {}
                try { if (plugin.shopsService != null) referenced = referenced || plugin.shopsService.hasArmorStand(id); } catch (Throwable ignored) {}
                try { if (plugin.banksService != null) referenced = referenced || plugin.banksService.hasArmorStand(id); } catch (Throwable ignored) {}

                if (!referenced) {
                    // Not linked to any known object; remove the ArmorStand entity
                    try { stand.remove(); } catch (Throwable ignored) {}
                    try { plugin.getLogger().info("Removed orphan ArmorStand in chunk (" + chunk.getX() + "," + chunk.getZ() + ") id=" + id); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {
        }

        // After cleanup, ensure missing stands are created (centralized)
        try { if (plugin.armorStandManager != null) plugin.armorStandManager.ensureArmorStands(); } catch (Throwable ignored) {}
    }

    private boolean isManagedStand(ArmorStand stand) {
        // Prefer persistent data tag set by ArmorStandManager.create()
        try {
            PersistentDataContainer pdc = stand.getPersistentDataContainer();
            Byte b = pdc.get(managedKey, PersistentDataType.BYTE);
            if (b != null && b == (byte)1) return true;
        } catch (Throwable ignored) {}
        // Fallback to name prefix heuristic for pre-existing stands
        try {
            String name = null;
            try {
                var comp = stand.customName();
                name = (comp == null) ? null : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
            } catch (Throwable ignored) {}
            if (name == null) return false;
            name = name.trim();
            return name.startsWith("Home ") || name.startsWith("Home of ") || name.startsWith("Warp:") || name.startsWith("Warp ") || name.startsWith("Shop:") || name.startsWith("Bank:");
        } catch (Throwable ignored) {}
        return false;
    }

}
