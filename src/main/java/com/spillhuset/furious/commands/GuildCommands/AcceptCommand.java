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

public class AcceptCommand implements SubCommandInterface {
    private final Furious plugin;

    public AcceptCommand(Furious plugin) {
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
            // If player is an admin of a guild, suggest players who requested to join
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
            // Also suggest inviting guild names (player-side invite accept) if no admin match
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
            Components.sendErrorMessage(sender, "Only players can accept requests.");
            return true;
        }
        if (!can(sender, true)) return true;

        String arg = (args.length >= 2) ? args[1] : null;
        UUID self = player.getUniqueId();

        // Admin accept join-request form: /guild accept <player>
        UUID gid = plugin.guildService.getGuildIdForMember(self);
        if (gid != null) {
            var guild = plugin.guildService.getGuildById(gid);
            if (guild != null && guild.getMembers().get(self) == com.spillhuset.furious.utils.GuildRole.ADMIN && arg != null) {
                OfflinePlayer target = Utility.findPlayer(arg, self);
                if (target != null) {
                    boolean okAdmin = plugin.guildService.acceptJoinRequestByAdmin(self, target.getUniqueId());
                    if (okAdmin) {
                        Components.sendSuccess(sender, Components.t("Accepted join request from "), Components.playerComp((target.getName() != null ? target.getName() : target.getUniqueId()) + "."));
                        return true;
                    }
                }
            }
        }

        // Player accept invite form: /guild accept [guildName]
        List<String> pending = plugin.guildService.getInvitingGuildNamesFor(self);
        if ((arg == null || arg.isBlank()) && pending.size() > 1) {
            Components.sendInfoMessage(sender, "You have invites from multiple guilds: " + String.join(", ", pending) + ". Use /guild accept <name>.");
            return true;
        }

        boolean ok = plugin.guildService.acceptInvite(self, arg);
        if (ok) {
            UUID joinedGid = plugin.guildService.getGuildIdForMember(self);
            String gname = joinedGid != null && plugin.guildService.getGuildById(joinedGid) != null ? plugin.guildService.getGuildById(joinedGid).getName() : (arg != null ? arg : "");
            Components.sendSuccess(sender, Components.t("You have joined guild: "), Components.playerComp(gname), Components.t("."));
        } else {
            if (pending.isEmpty()) {
                Components.sendErrorMessage(sender, "You have no pending guild invites.");
            } else if (arg != null && !pending.contains(arg)) {
                Components.sendError(sender, Components.t("No pending invite from guild: "), Components.valueComp(arg), Components.t("."));
            } else if (plugin.guildService.getGuildIdForMember(self) != null) {
                Components.sendErrorMessage(sender, "You are already in a guild.");
            } else {
                Components.sendErrorMessage(sender, "Failed to accept invite. Please specify a valid guild name.");
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "accept";
    }

    @Override
    public String getPermission() {
        return "furious.guild.accept";
    }
}
