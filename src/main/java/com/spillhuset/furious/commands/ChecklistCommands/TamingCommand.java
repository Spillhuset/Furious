package com.spillhuset.furious.commands.ChecklistCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.*;
import java.util.stream.Collectors;

public class TamingCommand implements SubCommandInterface {
    private final Furious plugin;

    public TamingCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    private Collection<EntityType> allTameableTypes() {
        try {
            if (plugin.registryCache != null) return plugin.registryCache.getTameableTypes();
        } catch (Throwable ignored) {}
        return java.util.Collections.emptyList();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            return suggestions;
        }
        if (args.length == 2) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            if ("clear".startsWith(partial)) suggestions.add("clear");
            for (OfflinePlayer op : plugin.tamingService.resolveAllKnownPlayers()) {
                if (op.getName() != null && op.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                    suggestions.add(op.getName());
                }
            }
            for (EntityType t : allTameableTypes()) {
                try {
                    NamespacedKey key = t.getKey();
                    String keyNs = key.asString().toLowerCase(Locale.ROOT);
                    String keySimple = key.value().toLowerCase(Locale.ROOT);
                    if (keyNs.startsWith(partial)) suggestions.add(keyNs);
                    else if (keySimple.startsWith(partial)) suggestions.add(keySimple);
                } catch (Throwable ignored) {}
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("clear")) {
            String partial = args[2].toLowerCase(Locale.ROOT);
            if ("month".startsWith(partial)) suggestions.add("month");
            if ("year".startsWith(partial)) suggestions.add("year");
            if ("all".startsWith(partial)) suggestions.add("all");
        } else if (args.length == 4 && args[1].equalsIgnoreCase("clear") && args[2].equalsIgnoreCase("all")) {
            if ("confirm".startsWith(args[3].toLowerCase(Locale.ROOT))) suggestions.add("confirm");
        }
        return suggestions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
            if (!can(sender, true)) return true;
            String scope = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
            switch (scope) {
                case "month" -> { plugin.tamingService.clearMonthFirst(); Components.sendSuccessMessage(sender, "Cleared this-month-first winners."); return true; }
                case "year" -> { plugin.tamingService.clearYearFirst(); Components.sendSuccessMessage(sender, "Cleared this-year-first winners."); return true; }
                case "all" -> {
                    if (args.length >= 4 && args[3].equalsIgnoreCase("confirm")) {
                        plugin.tamingService.clearAllFirsts();
                        Components.sendSuccessMessage(sender, "Cleared server-first, this-year-first, and this-month-first (and global taming).");
                    } else {
                        Components.sendErrorMessage(sender, "This will clear ALL firsts and global taming. Use /checklist taming clear all confirm to proceed.");
                    }
                    return true;
                }
                default -> {
                    Components.sendInfoMessage(sender, "Usage: /checklist taming clear <month|year|all> [confirm]");
                    return true;
                }
            }
        }

        if (!(sender instanceof Player) && args.length == 1) {
            Components.sendErrorMessage(sender, "Console must specify a player or tameable type.");
            return true;
        }

        if (args.length == 1) {
            Player player = (Player) sender;
            showChecklist(sender, player.getUniqueId(), Objects.requireNonNull(player.getName()));
            return true;
        }

        String target = args[1];
        OfflinePlayer targetPlayer = resolvePlayer(target);
        if (targetPlayer != null) {
            if (!can(sender, true, true)) {
                return true;
            }
            showChecklist(sender, targetPlayer.getUniqueId(), Objects.requireNonNullElse(targetPlayer.getName(), targetPlayer.getUniqueId().toString()));
            return true;
        }

        EntityType type = resolveTameable(target);
        if (type == null) {
            Components.sendErrorMessage(sender, "Unknown player or tameable type: " + target);
            return true;
        }
        showPlayersForType(sender, type);
        return true;
    }

    private OfflinePlayer resolvePlayer(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p;
        for (OfflinePlayer op : plugin.getServer().getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(name)) return op;
        }
        return null;
    }

    private EntityType resolveTameable(String name) {
        String in = name.toLowerCase(Locale.ROOT);
        for (EntityType t : allTameableTypes()) {
            try {
                NamespacedKey key = t.getKey();
                String keyNs = key.asString().toLowerCase(Locale.ROOT);
                String keySimple = key.value().toLowerCase(Locale.ROOT);
                if (in.equals(keyNs) || in.equals(keySimple)) return t;
            } catch (Throwable ignored) {}
        }
        for (EntityType t : allTameableTypes()) {
            if (t.toString().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    private void showChecklist(CommandSender sender, UUID playerId, String displayName) {
        Set<String> tamed = plugin.tamingService.getTamed(playerId);
        List<EntityType> all = new ArrayList<>(allTameableTypes());
        all = all.stream().sorted(Comparator.comparing(t -> {
            try { return t.getKey().asString(); } catch (Throwable e) { return t.toString(); }
        })).collect(Collectors.toList());

        Components.sendInfo(sender, Components.t("Tameable mobs tamed by "), Components.playerComp(displayName));
        for (EntityType t : all) {
            String key;
            String simple;
            try { key = t.getKey().asString().toLowerCase(Locale.ROOT); simple = t.getKey().value().toLowerCase(Locale.ROOT);} catch (Throwable e) { key = t.toString().toLowerCase(Locale.ROOT); simple = key; }
            boolean has = tamed.contains(key);
            String mark = has ? "✔" : "✘";
            NamedTextColor color = has ? NamedTextColor.GREEN : NamedTextColor.RED;
            UUID month = plugin.tamingService.getMonthFirst(key);
            UUID year = plugin.tamingService.getYearFirst(key);
            UUID server = plugin.tamingService.getServerFirst(key);
            String suffix = null;
            if (month != null) {
                String n = Optional.ofNullable(plugin.getServer().getOfflinePlayer(month).getName()).orElse(month.toString());
                suffix = ": this-month-first " + n;
            } else if (year != null) {
                String n = Optional.ofNullable(plugin.getServer().getOfflinePlayer(year).getName()).orElse(year.toString());
                suffix = ": this-year-first " + n;
            } else if (server != null) {
                String n = Optional.ofNullable(plugin.getServer().getOfflinePlayer(server).getName()).orElse(server.toString());
                suffix = ": server-first " + n;
            }
            if (suffix == null) {
                Components.sendColored(sender, NamedTextColor.YELLOW,
                        Components.t("["), Components.t(mark, color), Components.t("] "),
                        Components.t(simple, NamedTextColor.GOLD)
                );
            } else {
                Components.sendColored(sender, NamedTextColor.YELLOW,
                        Components.t("["), Components.t(mark, color), Components.t("] "),
                        Components.t(simple, NamedTextColor.GOLD),
                        Components.t("" + suffix, NamedTextColor.YELLOW)
                );
            }
        }
        Components.sendInfo(sender,
                Components.t("Total tameable: "), Components.valueComp(String.valueOf(all.size())),
                Components.t(" | Tamed: "), Components.valueComp(String.valueOf(tamed.size()))
        );
    }

    private void showPlayersForType(CommandSender sender, EntityType t) {
        String key;
        String simple;
        try { key = t.getKey().asString().toLowerCase(Locale.ROOT); simple = t.getKey().value().toLowerCase(Locale.ROOT);} catch (Throwable e) { key = t.toString().toLowerCase(Locale.ROOT); simple = key; }
        Components.sendInfo(sender, Components.t("Firsts for "), Components.t(simple, NamedTextColor.GOLD), Components.t(":"));
        UUID server = plugin.tamingService.getServerFirst(key);
        UUID year = plugin.tamingService.getYearFirst(key);
        UUID month = plugin.tamingService.getMonthFirst(key);
        boolean any = false;
        if (server != null) {
            String name = Optional.ofNullable(plugin.getServer().getOfflinePlayer(server).getName()).orElse(server.toString());
            Components.sendColored(sender, NamedTextColor.YELLOW, Components.t("1. "), Components.playerComp(name));
            any = true;
        }
        if (year != null) {
            String name = Optional.ofNullable(plugin.getServer().getOfflinePlayer(year).getName()).orElse(year.toString());
            int y = plugin.tamingService.getCurrentYear();
            Components.sendColored(sender, NamedTextColor.YELLOW, Components.t(y + " - "), Components.playerComp(name));
            any = true;
        }
        if (month != null) {
            String name = Optional.ofNullable(plugin.getServer().getOfflinePlayer(month).getName()).orElse(month.toString());
            int y = plugin.tamingService.getCurrentYear();
            int m = plugin.tamingService.getCurrentMonth();
            Components.sendColored(sender, NamedTextColor.YELLOW, Components.t(y + "/" + m + " - "), Components.playerComp(name));
            any = true;
        }
        if (!any) {
            Components.sendGreyMessage(sender, "No firsts recorded yet.");
        }
    }

    @Override
    public String getName() { return "taming"; }

    @Override
    public String getPermission() { return "furious.checklist.taming"; }
}
