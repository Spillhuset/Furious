package com.spillhuset.furious.listeners;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.ProfessionService;
import com.spillhuset.furious.services.ProfessionService.Profession;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Locale;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class ProfessionListener implements Listener {
    private final Furious plugin;

    private static final String PERM_VEIN_MINER = "furious.profession.veinminer";
    private static final String PERM_TREE_CHOPPER = "furious.profession.treechopper";
    private static final int VEIN_MAX_BLOCKS = 128;
    private static final int TREE_MAX_BLOCKS = 256;

    private static final EnumSet<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
            Material.ANCIENT_DEBRIS
    );

    private static final EnumSet<Material> LOGS = EnumSet.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.CRIMSON_STEM, Material.WARPED_STEM, Material.BAMBOO_BLOCK
    );

    private static final EnumSet<Material> SAPLINGS = EnumSet.of(
            Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING,
            Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.MANGROVE_PROPAGULE, Material.CHERRY_SAPLING,
            Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS, Material.BAMBOO_SAPLING
    );

    private static final EnumSet<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.MELON, Material.PUMPKIN, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP
    );

    public ProfessionListener(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    private ProfessionService svc() { return plugin.professionService; }

    private boolean hasProfession(Player p, Profession prof) {
        if (p == null || prof == null) return false;
        if (svc() == null) return false;
        Profession pri = svc().getPrimary(p.getUniqueId());
        Profession sec = svc().getSecondary(p.getUniqueId());
        return prof.equals(pri) || prof.equals(sec);
    }

    // Miner: earns by mining, extra from ores
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        if (svc() == null) return;
        int baseMiner = svc().ptsBase(Profession.MINER);
        if (baseMiner > 0) svc().addPoints(p.getUniqueId(), Profession.MINER, baseMiner);
        if (ORES.contains(b.getType())) {
            int bonus = svc().ptsBonus(Profession.MINER, "ore");
            if (bonus > 0) svc().addPoints(p.getUniqueId(), Profession.MINER, bonus);
            // Vein miner ability: break connected ore vein if permission is present and using a pickaxe
            tryVeinMine(p, b);
        }
        // Lumberjack: cutting trees
        if (LOGS.contains(b.getType())) {
            int base = svc().ptsBase(Profession.LUMBERJACK);
            if (base > 0) svc().addPoints(p.getUniqueId(), Profession.LUMBERJACK, base);
            // Tree chopper ability: break connected logs (entire tree) if permission is present and using an axe
            tryTreeChop(p, b);
        }
        // Farmer: harvesting mature crops
        if (CROPS.contains(b.getType())) {
            int base = svc().ptsBase(Profession.FARMER);
            if (base > 0) svc().addPoints(p.getUniqueId(), Profession.FARMER, base);
        }
    }

    private String key(Block b) {
        org.bukkit.Location l = b.getLocation();
        java.util.UUID w = l.getWorld() != null ? l.getWorld().getUID() : new java.util.UUID(0L,0L);
        return w + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    private void tryVeinMine(Player p, Block origin) {
        try {
            if (p == null || origin == null) return;
            if (!p.hasPermission(PERM_VEIN_MINER)) return;
            ItemStack tool = p.getInventory().getItemInMainHand();
            if (!isPickaxe(tool)) return;
            Material target = origin.getType();
            if (!ORES.contains(target)) return;

            // BFS over 6-directionally adjacent blocks of the same material
            Deque<Block> queue = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            java.util.List<Block> toBreak = new java.util.ArrayList<>();

            queue.add(origin);
            visited.add(key(origin));

            while (!queue.isEmpty() && toBreak.size() < VEIN_MAX_BLOCKS) {
                Block cur = queue.poll();
                // Explore neighbors
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                            if (manhattan != 1) continue; // 6-neighborhood only
                            Block nb = cur.getRelative(dx, dy, dz);
                            if (nb.getType() != target) continue;
                            String k = key(nb);
                            if (!visited.add(k)) continue;
                            if (!nb.equals(origin)) toBreak.add(nb);
                            if (toBreak.size() >= VEIN_MAX_BLOCKS) break;
                            queue.add(nb);
                        }
                        if (toBreak.size() >= VEIN_MAX_BLOCKS) break;
                    }
                    if (toBreak.size() >= VEIN_MAX_BLOCKS) break;
                }
            }

            // Break the collected blocks naturally with the player's tool
            for (Block blk : toBreak) {
                try {
                    if (blk.getType() == target) {
                        blk.breakNaturally(tool);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean isPickaxe(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith("_pickaxe");
    }

    private boolean isAxe(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith("_axe");
    }

    private void tryTreeChop(Player p, Block origin) {
        try {
            if (p == null || origin == null) return;
            if (!p.hasPermission(PERM_TREE_CHOPPER)) return;
            ItemStack tool = p.getInventory().getItemInMainHand();
            if (!isAxe(tool)) return;
            Material target = origin.getType();
            if (!LOGS.contains(target)) return;

            Deque<Block> queue = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            java.util.List<Block> toBreak = new java.util.ArrayList<>();

            queue.add(origin);
            visited.add(key(origin));

            while (!queue.isEmpty() && toBreak.size() < TREE_MAX_BLOCKS) {
                Block cur = queue.poll();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                            if (manhattan != 1) continue; // 6-neighborhood only
                            Block nb = cur.getRelative(dx, dy, dz);
                            Material mt = nb.getType();
                            if (!LOGS.contains(mt)) continue;
                            String k = key(nb);
                            if (!visited.add(k)) continue;
                            if (!nb.equals(origin)) toBreak.add(nb);
                            if (toBreak.size() >= TREE_MAX_BLOCKS) break;
                            queue.add(nb);
                        }
                        if (toBreak.size() >= TREE_MAX_BLOCKS) break;
                    }
                    if (toBreak.size() >= TREE_MAX_BLOCKS) break;
                }
            }

            for (Block blk : toBreak) {
                try {
                    if (LOGS.contains(blk.getType())) {
                        blk.breakNaturally(tool);
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {
        }
    }

    // Lumberjack: planting trees -> bonus
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (svc() == null) return;
        if (SAPLINGS.contains(e.getBlockPlaced().getType())) {
            int bonus = svc().ptsBonus(Profession.LUMBERJACK, "plant");
            if (bonus > 0) svc().addPoints(e.getPlayer().getUniqueId(), Profession.LUMBERJACK, bonus);
        }
        // Farmer: seeding
        if (isSeedItem(e.getItemInHand())) {
            int base = svc().ptsBase(Profession.FARMER);
            if (base > 0) svc().addPoints(e.getPlayer().getUniqueId(), Profession.FARMER, base);
        }
    }

    private boolean isSeedItem(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.WHEAT_SEEDS || m == Material.BEETROOT_SEEDS || m == Material.MELON_SEEDS ||
                m == Material.PUMPKIN_SEEDS || m == Material.TORCHFLOWER_SEEDS || m == Material.PITCHER_POD ||
                m == Material.NETHER_WART || m == Material.BAMBOO;
    }

    // Farmer: tilling -> bonus
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (svc() == null) return;
        if (e.getClickedBlock() == null) return;
        // Simple heuristic: if using a hoe on dirt/grass, count tilling
        ItemStack item = e.getItem();
        if (item == null) return;
        String name = item.getType().name().toLowerCase(Locale.ROOT);
        if (name.endsWith("_hoe")) {
            Material t = e.getClickedBlock().getType();
            if (t == Material.DIRT || t == Material.GRASS_BLOCK || t == Material.DIRT_PATH) {
                int bonus = svc().ptsBonus(Profession.FARMER, "till");
                if (bonus > 0) svc().addPoints(e.getPlayer().getUniqueId(), Profession.FARMER, bonus);
            }
        }
    }

    // Fisher: fishing and treasure
    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (svc() == null) return;
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            // Check if treasure-like item
            org.bukkit.entity.Entity caught = e.getCaught();
            boolean treasure = false;
            if (caught instanceof org.bukkit.entity.Item item) {
                Material t = item.getItemStack().getType();
                treasure = isTreasure(t);
            }
            if (treasure) {
                int bonus = svc().ptsBonus(Profession.FISHER, "treasure");
                if (bonus > 0) svc().addPoints(e.getPlayer().getUniqueId(), Profession.FISHER, bonus);
            } else {
                int base = svc().ptsBase(Profession.FISHER);
                if (base > 0) svc().addPoints(e.getPlayer().getUniqueId(), Profession.FISHER, base);
            }
        } else if (e.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            // ignore entities
        }
    }

    // Butcher: cooking in smoker -> bonus
    @EventHandler(ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent e) {
        if (svc() == null) return;
        // Only count smokers
        if (e.getBlock().getType() == Material.SMOKER) {
            // If result is edible meat, grant bonus to nearest player with BUTCHER profession
            Material res = e.getResult() != null ? e.getResult().getType() : Material.AIR;
            if (isCookedMeat(res)) {
                int bonus = svc().ptsBonus(Profession.BUTCHER, "smoker");
                if (bonus > 0) {
                    org.bukkit.Location loc = e.getBlock().getLocation();
                    org.bukkit.World world = loc.getWorld();
                    if (world != null) {
                        Player nearest = null;
                        double best = Double.MAX_VALUE;
                        for (Player p : world.getPlayers()) {
                            // Consider only players with BUTCHER profession (primary or secondary)
                            if (!hasProfession(p, Profession.BUTCHER)) continue;
                            double d2 = p.getLocation().distanceSquared(loc);
                            if (d2 < best) { best = d2; nearest = p; }
                        }
                        if (nearest != null) {
                            svc().addPoints(nearest.getUniqueId(), Profession.BUTCHER, bonus);
                        }
                    }
                }
            }
        }
    }

    private boolean isCookedMeat(Material m) {
        return m == Material.COOKED_BEEF || m == Material.COOKED_PORKCHOP || m == Material.COOKED_MUTTON ||
                m == Material.COOKED_CHICKEN || m == Material.COOKED_RABBIT || m == Material.COOKED_COD ||
                m == Material.COOKED_SALMON;
    }

    private boolean isTreasure(Material m) {
        return m == Material.BOW || m == Material.ENCHANTED_BOOK || m == Material.FISHING_ROD ||
                m == Material.NAME_TAG || m == Material.NAUTILUS_SHELL || m == Material.SADDLE;
    }

    // Butcher: slaughter and breeding
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (svc() == null) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        Player killer = e.getEntity().getKiller();
        if (killer != null) {
            // credit all animal kills as butcher points
            int base = svc().ptsBase(Profession.BUTCHER);
            if (base > 0) svc().addPoints(killer.getUniqueId(), Profession.BUTCHER, base);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (svc() == null) return;
        if (e.getBreeder() instanceof Player p) {
            int base = svc().ptsBase(Profession.BUTCHER);
            if (base > 0) svc().addPoints(p.getUniqueId(), Profession.BUTCHER, base);
        }
    }
}
