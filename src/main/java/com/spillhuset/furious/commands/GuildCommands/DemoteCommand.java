package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.Guild;
import com.spillhuset.furious.utils.GuildRole;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class DemoteCommand implements SubCommandInterface {
    private final Furious plugin;

    public DemoteCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        boolean canOthers = can(sender, false, true);
        // Admin: /guild demote <guildName> <player>
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
                for (Map.Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
                    UUID member = entry.getKey();
                    GuildRole role = entry.getValue();
                    if (role == GuildRole.MEMBER) continue; // already min
                    if (role == GuildRole.ADMIN) continue; // we don't demote admins via this command
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
        // Player: /guild demote <player>
        if (sender instanceof Player player) {
            if (args.length == 2 && can(sender, false)) {
                UUID gid = plugin.guildService.getGuildIdForMember(player.getUniqueId());
                if (gid == null) return completions;
                Guild guild = plugin.guildService.getGuildById(gid);
                if (guild == null) return completions;
                String prefix = args[1].toLowerCase();
                for (Map.Entry<UUID, GuildRole> entry : guild.getMembers().entrySet()) {
                    UUID member = entry.getKey();
                    if (member.equals(player.getUniqueId())) continue;
                    GuildRole role = entry.getValue();
                    if (role == GuildRole.MEMBER) continue; // cannot demote member further
                    if (role == GuildRole.ADMIN) continue; // cannot demote admin via player form
                    OfflinePlayer p = Bukkit.getOfflinePlayer(member);
                    String name = p.getName();
                    if (name != null && name.toLowerCase().startsWith(prefix)) {
                        if (!completions.contains(name)) completions.add(name);
                    }
                }
            }
        }
        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Admin form: /guild demote <guildName> <player>
        if (args.length >= 3 && can(sender, true, true)) {
            String guildName = args[1];
            String playerName = args[2];
            OfflinePlayer target = Utility.findPlayer(playerName);
            if (target == null) {
                Components.sendErrorMessage(sender, "Player not found: " + playerName);
                return true;
            }
            GuildService.RoleChangeResult res = plugin.guildService.demoteByName(guildName, target.getUniqueId());
            switch (res) {
                case SUCCESS -> Components.sendSuccessMessage(sender, "Demoted " + target.getName() + " in guild " + guildName + ".");
                case GUILD_NOT_FOUND -> Components.sendErrorMessage(sender, "Guild not found: " + guildName);
                case TARGET_NOT_IN_GUILD -> Components.sendErrorMessage(sender, "That player is not in any guild.");
                case TARGET_NOT_IN_SAME_GUILD -> Components.sendErrorMessage(sender, "That player is not a member of " + guildName + ".");
                case TARGET_ALREADY_AT_MIN -> Components.sendErrorMessage(sender, "That player is already at the lowest rank.");
                case CANNOT_CHANGE_ADMIN -> Components.sendErrorMessage(sender, "You cannot demote an admin/owner.");
                default -> Components.sendErrorMessage(sender, "Failed to demote player.");
            }
            return true;
        }

        // Player form: /guild demote <player>
        if (args.length >= 2 && can(sender, true, false)) {
            if (!(sender instanceof Player player)) {
                Components.sendErrorMessage(sender, "Only players can use this form of /guild demote.");
                return true;
            }
            OfflinePlayer target = Utility.findPlayer(args[1], player.getUniqueId());
            if (target == null) {
                Components.sendErrorMessage(sender, "Player not found: " + args[1]);
                return true;
            }
            GuildService.RoleChangeResult res = plugin.guildService.demote(player.getUniqueId(), target.getUniqueId());
            switch (res) {
                case SUCCESS -> Components.sendSuccessMessage(sender, "Demoted " + target.getName() + " in your guild.");
                case ACTOR_NOT_IN_GUILD -> Components.sendErrorMessage(sender, "You are not in a guild.");
                case ACTOR_NOT_ADMIN -> Components.sendErrorMessage(sender, "You must be an admin/owner to demote members.");
                case TARGET_INVALID -> Components.sendErrorMessage(sender, "Invalid target.");
                case TARGET_NOT_IN_GUILD -> Components.sendErrorMessage(sender, "That player is not in any guild.");
                case TARGET_NOT_IN_SAME_GUILD -> Components.sendErrorMessage(sender, "That player is not in your guild.");
                case TARGET_ALREADY_AT_MIN -> Components.sendErrorMessage(sender, "That player is already at the lowest rank.");
                case CANNOT_CHANGE_ADMIN -> Components.sendErrorMessage(sender, "You cannot demote an admin/owner.");
                case CANNOT_CHANGE_SELF -> Components.sendErrorMessage(sender, "You cannot change your own rank.");
                default -> Components.sendErrorMessage(sender, "Failed to demote player.");
            }
            return true;
        }

        Components.sendInfoMessage(sender, "Usage: /guild demote " + (can(sender, false, true) ? "[guild] " : "") + "<player>");
        return true;
    }

    @Override
    public String getName() {
        return "demote";
    }

    @Override
    public String getPermission() {
        return "furious.guild.demote";
    }
}
