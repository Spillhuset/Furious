package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ConnectivityCommand implements SubCommandInterface {
    private final Furious plugin;

    public ConnectivityCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // no args for now
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this command.");
            return true;
        }
        if (!can(sender, true)) return true;

        UUID self = player.getUniqueId();
        UUID gid = plugin.guildService.getGuildIdForMember(self);
        if (gid == null) {
            Components.sendErrorMessage(sender, "You are not in a guild.");
            return true;
        }
        Chunk chunk = player.getLocation().getChunk();
        UUID worldId = chunk.getWorld().getUID();

        // Summary of current connectivity in this world
        GuildService.ConnectivityReport rep = plugin.guildService.analyzeConnectivity(gid, worldId);
        if (rep.components == 0) {
            Components.sendInfoMessage(sender, "Your guild has no claims in this world.");
            return true;
        }
        Components.sendSuccess(sender,
                Components.t("Connectivity in "),
                Components.valueComp(chunk.getWorld().getName()),
                Components.t(": components="),
                Components.valueComp(String.valueOf(rep.components)),
                Components.t(", sizes="),
                Components.valueComp(rep.componentSizes.toString())
        );

        // If standing on our own claim, analyze effect of removing it
        UUID owner = plugin.guildService.getClaimOwner(worldId, chunk.getX(), chunk.getZ());
        if (gid.equals(owner)) {
            GuildService.ConnectivityReport after = plugin.guildService.analyzeConnectivityAfterRemoval(gid, worldId, chunk.getX(), chunk.getZ());
            if (after.components > 1) {
                Components.sendError(sender,
                        Components.t("Removing this chunk would split your territory into "),
                        Components.valueComp(String.valueOf(after.components)),
                        Components.t(" components. Sizes: "),
                        Components.valueComp(after.componentSizes.toString())
                );
                if (!after.samples.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int show = Math.min(3, after.samples.size());
                    for (int i = 0; i < show; i++) {
                        long[] s = after.samples.get(i);
                        if (i > 0) sb.append("; ");
                        sb.append("(").append(s[0]).append(",").append(s[1]).append(")");
                    }
                    Components.sendGreyMessage(sender, "Example chunks from components: " + sb);
                }
            } else {
                Components.sendInfoMessage(sender, "Removing this chunk would not disconnect your territory (but other rules may still apply).");
            }
        } else {
            Components.sendGreyMessage(sender, "Tip: Stand on one of your claimed chunks to analyze removal impact.");
        }

        // Provide a small ASCII map around the player
        int radius = 6;
        java.util.List<String> lines = plugin.guildService.buildAsciiMap(gid, worldId, chunk.getX(), chunk.getZ(), radius, null, null);
        Components.sendGreyMessage(sender, "Map legend: #=your claim, +=other guild, .=unclaimed, X=target");
        for (String line : lines) Components.sendGreyMessage(sender, line);
        return true;
    }

    @Override
    public String getName() { return "connectivity"; }

    @Override
    public String getPermission() { return "furious.guild.connectivity"; }
}
