package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.GuildRole;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RequestsCommand implements SubCommandInterface {
    private final Furious plugin;

    public RequestsCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // No arguments expected
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can list guild join requests.");
            return true;
        }
        if (!can(sender, true)) return true;

        UUID self = player.getUniqueId();
        UUID gid = plugin.guildService.getGuildIdForMember(self);
        if (gid == null) {
            Components.sendErrorMessage(sender, "You are not in a guild.");
            return true;
        }
        var guild = plugin.guildService.getGuildById(gid);
        if (guild == null || guild.getMembers().get(self) != GuildRole.ADMIN) {
            Components.sendErrorMessage(sender, "You must be a guild admin to view join requests.");
            return true;
        }
        java.util.Set<java.util.UUID> reqs = plugin.guildService.getJoinRequestsForGuild(gid);
        if (reqs == null || reqs.isEmpty()) {
            Components.sendInfoMessage(sender, "There are no pending join requests for your guild.");
            return true;
        }
        List<String> names = new ArrayList<>();
        for (UUID u : reqs) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(u);
            String name = off.getName();
            names.add(name != null ? name : u.toString());
        }
        Components.sendSuccess(sender,
                Components.t("Pending join requests: "),
                Components.t(String.join(", ", names))
        );
        return true;
    }

    @Override
    public String getName() {
        return "requests";
    }

    @Override
    public String getPermission() {
        return "furious.guild.requests";
    }
}
