package com.spillhuset.furious.listeners;

import com.spillhuset.furious.utils.AuditLog;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Logs item movements when a player (typically an op/mod) is viewing another player's inventory via /invsee.
 * Minimal approach: detect when the top inventory holder is a Player other than the viewer and log click/drag actions.
 */
public class InvseeAuditListener implements Listener {
    private final Logger logger;

    public InvseeAuditListener(Logger logger) {
        this.logger = logger;
    }

    /**
     * Determines if the current inventory view represents an invsee session (viewer looking at someone else's inventory).
     */
    private boolean isInvseeView(Inventory top, Player viewer) {
        if (top == null) return false;
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof Player target)) return false;
        return !target.getUniqueId().equals(viewer.getUniqueId());
    }

    private boolean shouldLog(Player viewer) {
        return viewer.isOp() || viewer.hasPermission("furious.invsee");
    }

    private String itemToString(ItemStack item) {
        if (item == null) return "AIRx0";
        String type = item.getType().name();
        int amount = item.getAmount();
        String meta = "";
        try {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                meta = "(\"" + item.getItemMeta().getDisplayName() + "\")";
            }
        } catch (Throwable ignored) {}
        return type + "x" + amount + (meta.isEmpty() ? "" : (" " + meta));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!shouldLog(viewer)) return;
        Inventory top = event.getView().getTopInventory();
        if (!isInvseeView(top, viewer)) return;

        Player target = (Player) top.getHolder();
        int rawSlot = event.getRawSlot();
        boolean inTop = rawSlot < event.getView().getTopInventory().getSize();

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        InventoryAction action = event.getAction();
        ClickType click = event.getClick();

        String msg = String.format("[INVSEE] %s %s slot=%d(top=%s) action=%s click=%s current=%s cursor=%s target=%s",
                viewer.getName(),
                (inTop ? "clicked target inv" : "clicked own inv"),
                rawSlot,
                inTop,
                action,
                click,
                itemToString(current),
                itemToString(cursor),
                target.getName());
        logger.info(msg);
        try { AuditLog.logResponse(viewer, msg); } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!shouldLog(viewer)) return;
        Inventory top = event.getView().getTopInventory();
        if (!isInvseeView(top, viewer)) return;

        Player target = (Player) top.getHolder();
        Map<Integer, ItemStack> newItems = event.getNewItems();
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());

        StringBuilder slots = new StringBuilder();
        event.getRawSlots().forEach(slot -> {
            if (slots.length() > 0) slots.append(",");
            slots.append(slot);
        });

        String msg = String.format("[INVSEE] %s dragged items affectsTop=%s slots=[%s] newItems=%d target=%s",
                viewer.getName(),
                affectsTop,
                slots,
                newItems.size(),
                target.getName());
        logger.info(msg);
        try { AuditLog.logResponse(viewer, msg); } catch (Throwable ignored) {}
    }
}
