package com.spillhuset.furious.commands.ChecklistCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class BiomesCommand implements SubCommandInterface {
    private final Furious plugin;

    public BiomesCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @SuppressWarnings("deprecation")
    private Collection<Biome> allBiomes() {
        try {
            Biome[] arr = Biome.class.getEnumConstants();
            if (arr != null && arr.length > 0) return Arrays.asList(arr);
        } catch (Throwable ignored) {}
        // Fallback to registry iteration on servers where Biome is not a true enum
        try {
            Iterable<Biome> reg = Registry.BIOME;
            if (reg != null) {
                List<Biome> list = new ArrayList<>();
                for (Biome b : reg) list.add(b);
                if (!list.isEmpty()) return list;
            }
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            // We are completing the subcommand itself handled by parent
            return suggestions;
        }
        if (args.length == 2) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            if ("clear".startsWith(partial)) suggestions.add("clear");
            // suggest player names
            for (OfflinePlayer op : plugin.biomesService.resolveAllKnownPlayers()) {
                if (op.getName() != null && op.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                    suggestions.add(op.getName());
                }
            }
            // suggest biome names (both namespaced and simple value)
            for (Biome b : allBiomes()) {
                String keyNs = b.key().asString().toLowerCase(Locale.ROOT);
                String keySimple = b.key().value().toLowerCase(Locale.ROOT);
                if (keyNs.startsWith(partial)) suggestions.add(keyNs);
                else if (keySimple.startsWith(partial)) suggestions.add(keySimple);
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
                case "month" -> { plugin.biomesService.clearMonthFirst(); Components.sendSuccessMessage(sender, "Cleared this-month-first winners."); return true; }
                case "year" -> { plugin.biomesService.clearYearFirst(); Components.sendSuccessMessage(sender, "Cleared this-year-first winners."); return true; }
                case "all" -> {
                    if (args.length >= 4 && args[3].equalsIgnoreCase("confirm")) {
                        plugin.biomesService.clearAllFirsts();
                        Components.sendSuccessMessage(sender, "Cleared server-first, this-year-first, and this-month-first (and global discovery).");
                    } else {
                        Components.sendErrorMessage(sender, "This will clear ALL firsts and global discovery. Use /checklist biomes clear all confirm to proceed.");
                    }
                    return true;
                }
                default -> {
                    Components.sendInfoMessage(sender, "Usage: /checklist biomes clear <month|year|all> [confirm]");
                    return true;
                }
            }
        }

        if (!(sender instanceof Player) && args.length == 1) {
            Components.sendErrorMessage(sender, "Console must specify a player or biome.");
            return true;
        }

        if (args.length == 1) {
            // /checklist biomes for self
            Player player = (Player) sender;
            showChecklist(sender, player.getUniqueId(), Objects.requireNonNull(player.getName()));
            return true;
        }

        String target = args[1];
        // Try resolve as player first (online/offline)
        OfflinePlayer targetPlayer = resolvePlayer(target);
        if (targetPlayer != null) {
            if (!can(sender, true, true)) {
                return true;
            }
            showChecklist(sender, targetPlayer.getUniqueId(), Objects.requireNonNullElse(targetPlayer.getName(), targetPlayer.getUniqueId().toString()));
            return true;
        }

        // Try resolve as biome
        Biome biome = resolveBiome(target);
        if (biome == null) {
            Components.sendErrorMessage(sender, "Unknown player or biome: " + target);
            return true;
        }
        showPlayersForBiome(sender, biome);
        return true;
    }

    private OfflinePlayer resolvePlayer(String name) {
        // Exact match online
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p;
        // Match offline by name (case-insensitive)
        for (OfflinePlayer op : plugin.getServer().getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equalsIgnoreCase(name)) return op;
        }
        return null;
    }

    private Biome resolveBiome(String name) {
        String in = name.toLowerCase(Locale.ROOT);
        Collection<Biome> all = allBiomes();
        for (Biome b : all) {
            String keyNs = b.key().asString().toLowerCase(Locale.ROOT);
            String keySimple = b.key().value().toLowerCase(Locale.ROOT);
            if (in.equals(keyNs) || in.equals(keySimple)) return b;
        }
        // Fallback: try match legacy enum constant string without calling name()
        for (Biome b : all) {
            if (b.toString().equalsIgnoreCase(name)) return b;
        }
        return null;
    }

    private void showChecklist(CommandSender sender, UUID playerId, String displayName) {
        Set<String> visited = plugin.biomesService.getVisited(playerId);
        List<Biome> all = new ArrayList<>(allBiomes());
        // Sort by biome key (namespaced)
        all = all.stream().sorted(Comparator.comparing(b -> b.key().asString())).collect(Collectors.toList());

        Components.sendInfo(sender, Components.t("Biomes visited by "), Components.playerComp(displayName));
        int perLine = 1; // one biome per line for clarity
        int count = 0;
        for (Biome b : all) {
            String biomeKey = b.key().asString().toLowerCase(Locale.ROOT);
            boolean has = visited.contains(biomeKey);
            String mark = has ? "✔" : "✘";
            NamedTextColor color = has ? NamedTextColor.GREEN : NamedTextColor.RED;
            // Determine precedence: month > year > server
            UUID month = plugin.biomesService.getMonthFirst(biomeKey);
            UUID year = plugin.biomesService.getYearFirst(biomeKey);
            UUID server = plugin.biomesService.getServerFirst(biomeKey);
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
                        Components.t(b.key().value().toLowerCase(Locale.ROOT), NamedTextColor.GOLD)
                );
            } else {
                Components.sendColored(sender, NamedTextColor.YELLOW,
                        Components.t("["), Components.t(mark, color), Components.t("] "),
                        Components.t(b.key().value().toLowerCase(Locale.ROOT), NamedTextColor.GOLD),
                        Components.t("" + suffix, NamedTextColor.YELLOW)
                );
            }
            count++;
        }
        Components.sendInfo(sender, Components.t("Total biomes: "), Components.valueComp(String.valueOf(all.size())), Components.t(" | Visited: "), Components.valueComp(String.valueOf(visited.size())));
    }

    private void showPlayersForBiome(CommandSender sender, Biome biome) {
        String key = biome.key().asString().toLowerCase(Locale.ROOT);
        Components.sendInfo(sender, Components.t("Firsts for "), Components.t(biome.key().value().toLowerCase(Locale.ROOT), NamedTextColor.GOLD), Components.t(":"));
        UUID server = plugin.biomesService.getServerFirst(key);
        UUID year = plugin.biomesService.getYearFirst(key);
        UUID month = plugin.biomesService.getMonthFirst(key);
        boolean any = false;
        if (server != null) {
            String name = Optional.ofNullable(plugin.getServer().getOfflinePlayer(server).getName()).orElse(server.toString());
            Components.sendColored(sender, NamedTextColor.YELLOW, Components.t("1. "), Components.playerComp(name));
            any = true;
        }
        if (year != null) {
            String name = Optional.ofNullable(plugin.getServer().getOfflinePlayer(year).getName()).orElse(year.toString());
            int y = plugin.biomesService.getCurrentYear();
            Components.sendColored(sender, NamedTextColor.YELLOW, Components.t(y + " - "), Components.playerComp(name));
            any = true;
        }
        if (month != null) {
            String name = Optional.ofNullable(plugin.getServer().getOfflinePlayer(month).getName()).orElse(month.toString());
            int y = plugin.biomesService.getCurrentYear();
            int m = plugin.biomesService.getCurrentMonth();
            Components.sendColored(sender, NamedTextColor.YELLOW, Components.t(y + "/" + m + " - "), Components.playerComp(name));
            any = true;
        }
        if (!any) {
            Components.sendGreyMessage(sender, "No firsts recorded yet.");
        }
    }

    @Override
    public String getName() {
        return "biomes";
    }

    @Override
    public String getPermission() {
        return "furious.checklist.biomes";
    }
}
