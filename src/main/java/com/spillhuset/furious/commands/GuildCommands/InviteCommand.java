package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InviteCommand implements SubCommandInterface {
    private final Furious plugin;

    public InviteCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player player)) {
            return completions;
        }
        if (!can(sender, false)) return completions;
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            UUID inviter = player.getUniqueId();
            UUID gid = plugin.guildService.getGuildIdForMember(inviter);
            // Suggest online players first
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                String name = p.getName();
                if (name == null) return;
                if (name.equalsIgnoreCase(player.getName())) return;
                if (!name.toLowerCase().startsWith(prefix)) return;
                // Filter: already in a guild
                if (plugin.guildService.getGuildIdForMember(p.getUniqueId()) != null) return;
                // Filter: already invited by this guild
                if (gid != null && plugin.guildService.isInvited(gid, p.getUniqueId())) return;
                if (!completions.contains(name)) completions.add(name);
            });
            // Add offline players that match and aren't already included
            for (OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
                String name = p.getName();
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (!lower.startsWith(prefix)) continue;
                if (player.getUniqueId().equals(p.getUniqueId())) continue;
                if (plugin.guildService.getGuildIdForMember(p.getUniqueId()) != null) continue;
                if (gid != null && plugin.guildService.isInvited(gid, p.getUniqueId())) continue;
                if (!completions.contains(name)) completions.add(name);
            }
            return completions;
        }
        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can invite to a guild.");
            return true;
        }
        if (!can(sender, true)) return true;
        if (args.length != 2) {
            Components.sendInfoMessage(sender, "Usage: /guild invite <player>");
            return true;
        }
        OfflinePlayer target = Utility.findPlayer(args[1], player.getUniqueId());
        if (target == null) {
            Components.sendErrorMessage(sender, "Player not found: " + args[1]);
            return true;
        }
        UUID inviter = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        GuildService.InviteResult result = plugin.guildService.invite(inviter, targetUuid);
        switch (result) {
            case SUCCESS -> {
                String gname = plugin.guildService.getGuildById(plugin.guildService.getGuildIdForMember(inviter)).getName();
                Components.sendSuccessMessage(sender, "Invitation sent to " + target.getName() + " for guild " + gname + ".");
                Player online = plugin.getServer().getPlayer(targetUuid);
                if (online != null) {
                    Components.sendInfoMessage(online, "You have been invited to join guild " + gname + ".");
                }
            }
            case NOT_IN_GUILD -> Components.sendErrorMessage(sender, "You are not in a guild.");
            case NOT_ADMIN -> Components.sendErrorMessage(sender, "You must be an admin/owner of your guild to invite.");
            case GUILD_NOT_OWNED_OR_UNMANNED -> Components.sendErrorMessage(sender, "Only OWNED guilds with an owner can invite players.");
            case TARGET_INVALID -> Components.sendErrorMessage(sender, "Invalid target.");
            case TARGET_ALREADY_IN_GUILD -> Components.sendErrorMessage(sender, "That player is already in a guild.");
            case ALREADY_INVITED -> Components.sendErrorMessage(sender, "That player has already been invited to your guild.");
        }
        return true;
    }

    @Override
    public String getName() {
        return "invite";
    }

    @Override
    public String getPermission() {
        return "furious.guild.invite";
    }
}
