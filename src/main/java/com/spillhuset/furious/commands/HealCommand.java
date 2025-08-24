package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HealCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public HealCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;

        // Self-heal when no args
        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                Components.sendErrorMessage(sender, "Only players can heal themselves from console. Use /heal <player> ...");
                return true;
            }
            heal(p);
            Components.sendSuccess(sender, Components.t("You have been healed."));
            return true;
        }

        // Healing others requires .others
        if (!sender.hasPermission(getPermission() + ".others")) {
            Components.sendErrorMessage(sender, "You don't have permission to heal others.");
            return true;
        }

        Set<Player> targets = new HashSet<>();
        for (String name : args) {
            Player t = Bukkit.getPlayer(name);
            if (t != null && t.isOnline()) {
                targets.add(t);
            } else {
                Components.sendErrorMessage(sender, "Player not found: " + name);
            }
        }

        if (targets.isEmpty()) {
            Components.sendInfoMessage(sender, "No valid online players to heal.");
            return true;
        }

        for (Player t : targets) {
            heal(t);
            if (!t.equals(sender)) {
                Components.sendInfo(t, Components.t("You have been healed by "), Components.playerComp(sender.getName()), Components.t("."));
            }
        }
        if (targets.size() == 1) {
            Player only = targets.iterator().next();
            Components.sendSuccess(sender, Components.t("Healed "), Components.playerComp(only.getName()), Components.t("."));
        } else {
            Components.sendSuccess(sender, Components.t("Healed "), Components.valueComp(String.valueOf(targets.size())), Components.t(" players."));
        }
        return true;
    }

    private void heal(Player p) {
        try {
            // Setting to a very high value; Bukkit will clamp to the entity's max health
            p.setHealth(p.getHealth() + 1000.0);
        } catch (Throwable ignored) {
            try { p.setHealth(20.0); } catch (Throwable ignored2) {}
        }
        p.setFireTicks(0);
        p.setFreezeTicks(0);
        p.setRemainingAir(p.getMaximumAir());
    }

    private void sendUsage(CommandSender sender) {
        Components.sendInfo(sender, Components.t("Usage: /" + getName() + " [player ...]", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length >= 1) {
            String prefix = args[args.length - 1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && name.toLowerCase().startsWith(prefix)) {
                    list.add(name);
                }
            }
        }
        return list;
    }

    @Override
    public String getName() { return "heal"; }

    @Override
    public String getPermission() { return "furious.heal"; }
}
