package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RepairCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public RepairCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        if (args.length == 0) {
            // /repair (defaults to hand for players)
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /" + label + " [hand|all] or /" + label + " <player> [hand|all]");
                return true;
            }
            if (!sender.hasPermission("furious.repair.self")) {
                sender.sendMessage("You don't have permission to repair your items.");
                return true;
            }
            repairHand(player, sender);
            return true;
        }

        if (args.length == 1) {
            String a0 = args[0].toLowerCase(Locale.ROOT);
            if (a0.equals("hand") || a0.equals("all")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use this form. Try /" + label + " <player> " + a0);
                    return true;
                }
                if (!sender.hasPermission("furious.repair.self")) {
                    sender.sendMessage("You don't have permission to repair your items.");
                    return true;
                }
                if (a0.equals("hand")) {
                    repairHand(player, sender);
                } else {
                    if (!sender.hasPermission("furious.repair.all")) {
                        sender.sendMessage("You don't have permission to repair all items.");
                        return true;
                    }
                    int count = repairAll(player);
                    sender.sendMessage("Repaired " + count + " item(s) in your inventory.");
                }
                return true;
            } else {
                // /repair <player>
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage("Player not found: " + args[0]);
                    return true;
                }
                if (!sender.hasPermission("furious.repair.others")) {
                    sender.sendMessage("You don't have permission to repair other players' items.");
                    return true;
                }
                // default to hand
                repairHand(target, sender);
                if (!Objects.equals(target, sender)) target.sendMessage("Your held item has been repaired by " + sender.getName() + ".");
                return true;
            }
        }

        // args.length >= 2 -> /repair <player> [hand|all]
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[0]);
            return true;
        }
        if (!sender.hasPermission("furious.repair.others")) {
            sender.sendMessage("You don't have permission to repair other players' items.");
            return true;
        }
        String mode = args[1].toLowerCase(Locale.ROOT);
        if (mode.equals("hand")) {
            if (repairHand(target, sender)) {
                if (!Objects.equals(target, sender)) target.sendMessage("Your held item has been repaired by " + sender.getName() + ".");
            }
        } else if (mode.equals("all")) {
            int count = repairAll(target);
            sender.sendMessage("Repaired " + count + " item(s) in " + target.getName() + "'s inventory.");
            if (!Objects.equals(target, sender)) target.sendMessage("Your items have been repaired by " + sender.getName() + ".");
        } else {
            sender.sendMessage("Usage: /" + label + " [hand|all] or /" + label + " <player> [hand|all]");
        }
        return true;
    }

    private boolean repairHand(@NotNull Player player, CommandSender feedback) {
        ItemStack item = player.getInventory().getItem(EquipmentSlot.HAND);
        if (item == null || item.getType() == Material.AIR) {
            feedback.sendMessage("You are not holding any item.");
            return false;
        }
        if (!repairItem(item)) {
            feedback.sendMessage("That item cannot be repaired.");
            return false;
        }
        feedback.sendMessage("Your held item has been repaired.");
        return true;
    }

    private int repairAll(@NotNull Player player) {
        int repaired = 0;
        PlayerInventory inv = player.getInventory();
        // Main inventory + hotbar
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && repairItem(it)) repaired++;
        }
        // Offhand
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() != Material.AIR && repairItem(off)) repaired++;
        // Armor contents
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack it = armor[i];
            if (it != null && repairItem(it)) repaired++;
        }
        player.updateInventory();
        return repaired;
    }

    private boolean repairItem(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable)) return false;
        Damageable dmg = (Damageable) meta;
        if (dmg.getDamage() <= 0) return false; // already full
        dmg.setDamage(0);
        item.setItemMeta((ItemMeta) dmg);
        return true;
    }

    @Override
    public String getName() { return "repair"; }

    @Override
    public String getPermission() { return "furious.repair"; }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            // suggest players and keywords
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(p.getName());
            }
            for (String s : Arrays.asList("hand","all")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2) {
            for (String s : Arrays.asList("hand","all")) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            }
        }
        return out;
    }
}
