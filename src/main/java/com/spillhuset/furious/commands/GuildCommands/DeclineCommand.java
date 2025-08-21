package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Utility;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeclineCommand implements SubCommandInterface {
    private final Furious plugin;

    public DeclineCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player player)) return completions;
        if (!can(sender, false)) return completions;
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            UUID uuid = player.getUniqueId();
            // If admin, suggest players with pending join requests to your guild
            UUID gid = plugin.guildService.getGuildIdForMember(uuid);
            if (gid != null) {
                var guild = plugin.guildService.getGuildById(gid);
                if (guild != null && guild.getMembers().get(uuid) == com.spillhuset.furious.utils.GuildRole.ADMIN) {
                    for (UUID req : plugin.guildService.getJoinRequestsForGuild(gid)) {
                        OfflinePlayer p = plugin.getServer().getOfflinePlayer(req);
                        String name = p.getName();
                        if (name != null && name.toLowerCase().startsWith(prefix)) completions.add(name);
                    }
                }
            }
            // Also suggest inviting guild names for player-side invite decline
            for (String name : plugin.guildService.getInvitingGuildNamesFor(uuid)) {
                if (name != null && name.toLowerCase().startsWith(prefix)) {
                    if (!completions.contains(name)) completions.add(name);
                }
            }
        }
        return completions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can decline requests.");
            return true;
        }
        if (!can(sender, true)) return true;

        String arg = (args.length >= 2) ? args[1] : null;
        UUID self = player.getUniqueId();

        // Admin decline join-request form: /guild decline <player>
        UUID gid = plugin.guildService.getGuildIdForMember(self);
        if (gid != null) {
            var guild = plugin.guildService.getGuildById(gid);
            if (guild != null && guild.getMembers().get(self) == com.spillhuset.furious.utils.GuildRole.ADMIN && arg != null) {
                OfflinePlayer target = Utility.findPlayer(arg, self);
                if (target != null) {
                    boolean okAdmin = plugin.guildService.declineJoinRequestByAdmin(self, target.getUniqueId());
                    if (okAdmin) {
                        Components.sendSuccessMessage(sender, "Declined join request from " + (target.getName() != null ? target.getName() : target.getUniqueId()) + ".");
                        return true;
                    }
                }
            }
        }

        // Player decline invite form: /guild decline [guildName]
        List<String> pending = plugin.guildService.getInvitingGuildNamesFor(self);
        if ((arg == null || arg.isBlank()) && pending.size() > 1) {
            Components.sendInfoMessage(sender, "You have invites from multiple guilds: " + String.join(", ", pending) + ". Use /guild decline <name>.");
            return true;
        }

        boolean ok = plugin.guildService.declineInvite(self, arg);
        if (ok) {
            String gname = (arg != null && !arg.isBlank()) ? arg : (pending.size() == 1 ? pending.get(0) : "");
            Components.sendSuccessMessage(sender, "You have declined the invite from guild: " + gname + ".");
        } else {
            if (pending.isEmpty()) {
                Components.sendErrorMessage(sender, "You have no pending guild invites.");
            } else if (arg != null && !pending.contains(arg)) {
                Components.sendErrorMessage(sender, "No pending invite from guild: " + arg + ".");
            } else {
                Components.sendErrorMessage(sender, "Failed to decline invite. Please specify a valid guild name.");
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "decline";
    }

    @Override
    public String getPermission() {
        return "furious.guild.decline";
    }
}
