package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class DeleteCommand implements SubCommandInterface {
    private final Furious plugin;
    private final Map<UUID, String> pendingDeletes = new HashMap<>();

    public DeleteCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        // Admin form: /guild delete <name> [confirm]
        if (can(sender, false, true)) {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                for (String name : plugin.guildService.getAllGuildNames()) {
                    if (name.toLowerCase().startsWith(prefix)) {
                        completions.add(name);
                    }
                }
                return completions;
            }
            if (args.length == 3) {
                String prefix = args[2].toLowerCase();
                if ("confirm".startsWith(prefix)) completions.add("confirm");
                return completions;
            }
        }

        // Player form: /guild delete [confirm]
        if (args.length == 2 && can(sender, false)) {
            String prefix = args[1].toLowerCase();
            if ("confirm".startsWith(prefix)) completions.add("confirm");
            return completions;
        }

        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        boolean confirmed = (args.length >= 2 && args[args.length - 1].equalsIgnoreCase("confirm"));

        // Admin paths
        if ((args.length == 2 || args.length == 3) && can(sender, true, true)) {
            // Either /guild delete <name> or /guild delete <name> confirm
            String name = args[1];
            String tokenKey = (sender instanceof Player p) ? p.getUniqueId().toString() : "console";
            String token = tokenKey + ":" + name.toLowerCase();

            if (confirmed) {
                String sample = pendingDeletes.get(((sender instanceof Player p) ? p.getUniqueId() : null));
                // For console, we cannot store under null UUID, so check by scanning map for matching token when not a Player
                boolean allowed;
                if (sender instanceof Player p) {
                    allowed = token.equals(sample);
                } else {
                    allowed = pendingDeletes.containsValue(token);
                }
                if (!allowed) {
                    Components.sendErrorMessage(sender, "Invalid confirmation token.");
                    return true;
                }
                boolean ok = plugin.guildService.deleteGuildByName(name);
                if (ok) {
                    Components.sendSuccessMessage(sender, "Guild deleted: " + name);
                } else {
                    Components.sendErrorMessage(sender, "Failed to delete guild. Ensure the name exists.");
                }
                // Clear stored token
                if (sender instanceof Player p) pendingDeletes.remove(p.getUniqueId());
                else pendingDeletes.values().removeIf(v -> v.equals(token));
                return true;
            } else {
                if (sender instanceof Player p) pendingDeletes.put(p.getUniqueId(), token);
                else {
                    // Store a synthetic key for console confirmations
                    // Not ideal, but allows single pending console deletion at a time
                    pendingDeletes.put(new UUID(0L, 0L), token);
                }
                Components.sendInfoMessage(sender, "Are you sure you want to delete the guild: " + name + "? Type /guild delete " + name + " confirm");
                return true;
            }
        }

        // Player paths: /guild delete [confirm]
        if ((args.length == 1 || args.length == 2) && can(sender, true, false)) {
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use /guild delete without specifying a name.");
                return true;
            }
            UUID uuid = player.getUniqueId();
            String token = uuid + ":self";
            if (confirmed) {
                String sample = pendingDeletes.get(uuid);
                if (sample == null || !sample.equals(token)) {
                    Components.sendErrorMessage(sender, "Invalid confirmation token.");
                    return true;
                }
                boolean ok = plugin.guildService.deleteGuildByMember(uuid);
                if (ok) {
                    Components.sendSuccessMessage(sender, "Your guild has been deleted.");
                } else {
                    Components.sendErrorMessage(sender, "Failed to delete your guild. You must be an admin/owner of a guild.");
                }
                pendingDeletes.remove(uuid);
                return true;
            } else {
                pendingDeletes.put(uuid, token);
                Components.sendInfoMessage(sender, "Are you sure you want to delete your guild? Type /guild delete confirm");
                return true;
            }
        }

        Components.sendInfoMessage(sender, "Usage: /guild delete " + (can(sender, false, true) ? "[name] " : "") + "[confirm]");
        return true;
    }

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getPermission() {
        return "furious.guild.delete";
    }
}
