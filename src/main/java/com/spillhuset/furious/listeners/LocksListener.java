package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class LocksListener implements Listener {
    private final Furious plugin;
    private final NamespacedKey TOOL_KEY;
    private final NamespacedKey KEY_OWNER_KEY;

    public LocksListener(Furious plugin) {
        this.plugin = plugin.getInstance();
        this.TOOL_KEY = new NamespacedKey(this.plugin, "locks_tool");
        this.KEY_OWNER_KEY = new NamespacedKey(this.plugin, "locks_key_owner");
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (isTool(stack)) {
            event.setCancelled(true);
            Components.sendErrorMessage(event.getPlayer(), "You cannot drop lock/unlock tools.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // ignore off-hand to avoid double firing
        Block block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        // If locks disabled in this world, do not enforce and block tool usage
        if (plugin.locksService != null) {
            block.getWorld();
            if (!plugin.locksService.isWorldEnabled(block.getWorld().getUID())) {
                // If trying to use tools in a disabled world, cancel and inform
                ItemStack toolCheck = player.getInventory().getItemInMainHand();
                String toolType0 = getToolType(toolCheck);
                if (toolType0 != null) {
                    event.setCancelled(true);
                    Components.sendErrorMessage(player, "Locks are disabled in this world.");
                }
                return;
            }
        }
        ItemStack inHand = player.getInventory().getItemInMainHand();
        String toolType = getToolType(inHand);
        UUID owner = plugin.locksService == null ? null : plugin.locksService.getOwner(block);

        // Using tools: lock/unlock
        if (toolType != null) {
            event.setCancelled(true); // prevent normal item use (like placing a sign or using shears)
            if (toolType.equals("lock")) {
                // Prevent locking in a claimed chunk if player is not a member of the owning guild (ops bypass)
                try {
                    if (!player.isOp()) {
                        block.getWorld();
                        UUID worldId = block.getWorld().getUID();
                        int cx = block.getChunk().getX();
                        int cz = block.getChunk().getZ();
                        UUID owningGuild = plugin.guildService.getClaimOwner(worldId, cx, cz);
                        if (owningGuild != null) {
                            UUID playerGuild = plugin.guildService.getGuildIdForMember(player.getUniqueId());
                            if (playerGuild == null || !playerGuild.equals(owningGuild)) {
                                Components.sendErrorMessage(player, "You must be a member of the guild that owns this chunk to set a lock here.");
                                return;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                if (owner != null) {
                    Components.sendErrorMessage(player, "This block (or its pair) is already locked.");
                    return;
                }
                boolean ok = plugin.locksService.lockBlock(player.getUniqueId(), block);
                if (ok) {
                    consumeOne(player);
                    plugin.locksService.save();
                    Components.sendSuccessMessage(player, "Locked.");
                } else {
                    Components.sendErrorMessage(player, "Could not lock this block.");
                }
                return;
            } else if (toolType.equals("unlock")) {
                boolean ok = plugin.locksService.unlockBlock(player.getUniqueId(), player.isOp(), block);
                if (ok) {
                    consumeOne(player);
                    plugin.locksService.save();
                    Components.sendSuccessMessage(player, "Unlocked.");
                } else {
                    Components.sendErrorMessage(player, "You are not the owner, or it isn't locked.");
                }
                return;
            }
        }

        // No tool: enforce access, but allow with matching key
        if (!player.isOp() && owner != null && !owner.equals(player.getUniqueId())) {
            if (!hasMatchingKey(inHand, owner)) {
                event.setCancelled(true);
                Components.sendErrorMessage(player, "This block is locked.");
                return;
            }
        }

        // If it's a door and interaction is allowed (we return early on cancel), sync paired door open state
        if (block.getBlockData() instanceof Door) {
            syncDoubleDoor(block);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return; // ops bypass for breaking
        Block block = event.getBlock();
        // Skip enforcement in disabled worlds
        if (plugin.locksService != null && block.getWorld() != null && !plugin.locksService.isWorldEnabled(block.getWorld().getUID())) return;
        UUID owner = plugin.locksService == null ? null : plugin.locksService.getOwner(block);
        if (owner != null && !owner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            Components.sendErrorMessage(player, "This block is locked.");
        }
    }

    private boolean isTool(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(TOOL_KEY, PersistentDataType.STRING);
    }

    private String getToolType(ItemStack stack) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(TOOL_KEY, PersistentDataType.STRING);
    }

    private boolean hasMatchingKey(ItemStack stack, UUID owner) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String val = pdc.get(KEY_OWNER_KEY, PersistentDataType.STRING);
        if (val == null) return false;
        try {
            return owner.equals(UUID.fromString(val));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void consumeOne(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null) return;
        int amt = hand.getAmount();
        if (amt <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(amt - 1);
        }
    }

    private void syncDoubleDoor(Block doorBlock) {
        // Run next tick to let vanilla toggle the clicked door; then mirror the state to its pair
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!(doorBlock.getBlockData() instanceof Door door)) return;
            boolean open;
            try {
                open = ((Openable) doorBlock.getBlockData()).isOpen();
            } catch (Throwable t) {
                open = door.isOpen();
            }
            Block pair = findPairedDoor(doorBlock, door);
            if (pair == null) return;
            if (pair.getBlockData() instanceof Door d2) {
                if (d2.isOpen() != open) {
                    d2.setOpen(open);
                    // ensure we modify the bottom half (Bukkit applies to both halves based on half)
                    Block toSet = pair;
                    if (d2.getHalf() == Bisected.Half.TOP) {
                        toSet = pair.getRelative(BlockFace.DOWN);
                        if (toSet.getBlockData() instanceof Door d3) {
                            d3.setOpen(open);
                            toSet.setBlockData(d3, false);
                            return;
                        }
                    }
                    toSet.setBlockData(d2, false);
                }
            }
        });
    }

    private Block findPairedDoor(Block base, Door door) {
        BlockFace facing = door.getFacing();
        // Doors pair are adjacent on the perpendicular axis with opposite hinges
        BlockFace left = switch (facing) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.NORTH;
        };
        Block[] cands = new Block[]{base.getRelative(left), base.getRelative(left.getOppositeFace())};
        for (Block b : cands) {
            if (b.getType() == base.getType() && b.getBlockData() instanceof Door d2) {
                if (d2.getFacing() == facing && d2.getHinge() != door.getHinge()) {
                    return b;
                }
            }
        }
        return null;
    }
}
