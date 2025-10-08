package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LeaveCommand implements SubCommandInterface {
    private final Furious plugin;

    public LeaveCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!can(sender, true)) return true;
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use /guild leave.");
            return true;
        }
        UUID playerId = player.getUniqueId();
        UUID gid = plugin.guildService.getGuildIdForMember(playerId);
        if (gid == null) {
            Components.sendErrorMessage(sender, "You are not in a guild.");
            return true;
        }
        String gname = plugin.guildService.getGuildById(gid) != null ? plugin.guildService.getGuildById(gid).getName() : "your guild";
        boolean ok = plugin.guildService.leave(playerId);
        if (ok) {
            Components.sendSuccessMessage(sender, "You left guild " + gname + ".");
        } else {
            // Likely because player is the owner
            Components.sendErrorMessage(sender, "Owners cannot leave their own guild. Use /guild delete to disband, or transfer ownership if supported.");
        }
        return true;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getPermission() {
        return "furious.guild.leave";
    }
}
