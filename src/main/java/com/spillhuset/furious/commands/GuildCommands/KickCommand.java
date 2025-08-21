package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class KickCommand implements SubCommandInterface {
    private final Furious plugin;

    public KickCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean canOthers = can(sender, false, true);
        // Admin form: /guild kick <guildName> <player> [reason]
        if (canOthers) {
            if (args.length == 2) {
                String prefix = args[1].toLowerCase();
                for (String name : plugin.guildService.getAllGuildNames()) {
                    if (name != null && name.toLowerCase().startsWith(prefix)) {
                        completions.add(name);
                    }
                }
                return completions;
            }
            if (args.length == 3) {
                String guildName = args[1];
                Guild guild = plugin.guildService.getGuildByName(guildName);
                if (guild == null) return completions;
                String prefix = args[2].toLowerCase();
                for (UUID member : guild.getMembers().keySet()) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(member);
                    String name = p.getName();
                    if (name != null && name.toLowerCase().startsWith(prefix)) {
                        if (!completions.contains(name)) completions.add(name);
                    }
                }
                return completions;
            }
            return completions;
        }

        // Player form: /guild kick <player> [reason]
        if (sender instanceof Player player) {
            if (args.length == 2 && can(sender, false)) {
                UUID gid = plugin.guildService.getGuildIdForMember(player.getUniqueId());
                if (gid == null) return completions;
                Guild guild = plugin.guildService.getGuildById(gid);
                if (guild == null) return completions;
                String prefix = args[1].toLowerCase();
                for (UUID member : guild.getMembers().keySet()) {
                    if (member.equals(player.getUniqueId())) continue;
                    OfflinePlayer p = Bukkit.getOfflinePlayer(member);
                    String name = p.getName();
                    if (name != null && name.toLowerCase().startsWith(prefix)) {
                        if (!completions.contains(name)) completions.add(name);
                    }
                }
                return completions;
            }
        }

        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Admin form: /guild kick <guildName> <player> [reason]
        if ((args.length >= 3) && can(sender, true, true)) {
            String guildName = args[1];
            String playerName = args[2];
            OfflinePlayer target = Utility.findPlayer(playerName);
            if (target == null) {
                Components.sendErrorMessage(sender, "Player not found: " + playerName);
                return true;
            }
            String reason = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No reason specified";
            GuildService.KickResult res = plugin.guildService.kickByName(guildName, target.getUniqueId(), reason);
            switch (res) {
                case SUCCESS -> {
                    Components.sendSuccessMessage(sender, "Kicked " + target.getName() + " from guild " + guildName + (reason.isEmpty() ? "." : ": " + reason));
                    Player online = plugin.getServer().getPlayer(target.getUniqueId());
                    if (online != null) {
                        Components.sendErrorMessage(online, "You were kicked from guild " + guildName + ". Reason: " + reason);
                    }
                }
                case GUILD_NOT_FOUND -> Components.sendErrorMessage(sender, "Guild not found: " + guildName);
                case TARGET_NOT_IN_GUILD -> Components.sendErrorMessage(sender, "That player is not in any guild.");
                case TARGET_NOT_IN_SAME_GUILD -> Components.sendErrorMessage(sender, "That player is not a member of " + guildName + ".");
                case TARGET_IS_ADMIN -> Components.sendErrorMessage(sender, "You cannot kick an admin/owner.");
                default -> Components.sendErrorMessage(sender, "Failed to kick player.");
            }
            return true;
        }

        // Player form: /guild kick <player> [reason]
        if ((args.length >= 2) && can(sender, true, false)) {
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use this form of /guild kick.");
                return true;
            }
            OfflinePlayer target = Utility.findPlayer(args[1], player.getUniqueId());
            if (target == null) {
                Components.sendErrorMessage(sender, "Player not found: " + args[1]);
                return true;
            }
            String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "No reason specified";
            GuildService.KickResult res = plugin.guildService.kick(player.getUniqueId(), target.getUniqueId(), reason);
            switch (res) {
                case SUCCESS -> {
                    UUID gid = plugin.guildService.getGuildIdForMember(player.getUniqueId());
                    String gname = gid != null && plugin.guildService.getGuildById(gid) != null ? plugin.guildService.getGuildById(gid).getName() : "your guild";
                    Components.sendSuccessMessage(sender, "Kicked " + target.getName() + " from " + gname + (reason.isEmpty() ? "." : ": " + reason));
                    Player online = plugin.getServer().getPlayer(target.getUniqueId());
                    if (online != null) {
                        Components.sendErrorMessage(online, "You were kicked from guild " + gname + ". Reason: " + reason);
                    }
                }
                case ACTOR_NOT_IN_GUILD -> Components.sendErrorMessage(sender, "You are not in a guild.");
                case ACTOR_NOT_ADMIN -> Components.sendErrorMessage(sender, "You must be an admin/owner to kick members.");
                case TARGET_INVALID -> Components.sendErrorMessage(sender, "Invalid target.");
                case TARGET_NOT_IN_GUILD -> Components.sendErrorMessage(sender, "That player is not in any guild.");
                case TARGET_NOT_IN_SAME_GUILD -> Components.sendErrorMessage(sender, "That player is not in your guild.");
                case TARGET_IS_ADMIN -> Components.sendErrorMessage(sender, "You cannot kick another admin/owner.");
                case CANNOT_KICK_SELF -> Components.sendErrorMessage(sender, "You cannot kick yourself.");
                default -> Components.sendErrorMessage(sender, "Failed to kick player.");
            }
            return true;
        }

        Components.sendInfoMessage(sender, "Usage: /guild kick " + (can(sender, false, true) ? "[guild] " : "") + "<player> [reason]");
        return true;
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getPermission() {
        return "furious.guild.kick";
    }
}
