package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class UnclaimCommand implements SubCommandInterface {
    private final Furious plugin;

    public UnclaimCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // Allow ops/others to specify guild name at args[1]
        List<String> out = new ArrayList<>();
        if (!can(sender, false, true)) return out;
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.guildService.getAllGuildNames()) {
                if (name.toLowerCase().startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can unclaim chunks.");
            return true;
        }
        if (!can(sender, true)) return true;

        Chunk chunk = player.getLocation().getChunk();
        boolean isOp = sender.isOp();

        // If specifying a guild and allowed to act on others: unclaim this chunk for that guild
        if (args.length >= 2 && can(sender, false, true)) {
            String guildName = args[1];
            if (!isOp && !plugin.guildService.isWorldEnabled(chunk.getWorld().getUID())) {
                Components.sendErrorMessage(sender, "Guilds are disabled in this world.");
                return true;
            }
            GuildService.BulkUnclaimResult res = plugin.guildService.unclaimChunksForGuild(guildName, chunk.getWorld(), chunk.getX(), chunk.getZ(), chunk.getX(), chunk.getZ());
            if (res.total == 0) {
                Components.sendErrorMessage(sender, "Guild not found.");
                return true;
            }
            if (res.removed == 0) {
                Components.sendErrorMessage(sender, "This chunk is not claimed by guild '" + guildName + "'.");
                return true;
            }
            Components.sendSuccessMessage(sender, "Unclaimed chunk at (" + chunk.getX() + ", " + chunk.getZ() + ") for guild '" + guildName + "'.");
            return true;
        }

        // Normal player path: respect world enable unless op override
        if (!isOp && !plugin.guildService.isWorldEnabled(chunk.getWorld().getUID())) {
            Components.sendErrorMessage(sender, "Guilds are disabled in this world.");
            return true;
        }
        GuildService.UnclaimResult res = plugin.guildService.unclaimChunk(player.getUniqueId(), chunk);
        switch (res) {
            case SUCCESS -> Components.sendSuccessMessage(sender, "Unclaimed chunk at (" + chunk.getX() + ", " + chunk.getZ() + ")");
            case NOT_PLAYER_IN_GUILD -> Components.sendErrorMessage(sender, "You are not in a guild.");
            case NOT_ADMIN -> Components.sendErrorMessage(sender, "You must be an admin/owner of your guild to unclaim.");
            case NOT_CLAIMED -> Components.sendErrorMessage(sender, "This chunk is not claimed.");
            case NOT_OWNED -> Components.sendErrorMessage(sender, "Your guild does not own this chunk.");
            case DISCONNECTS_TERRITORY -> {
                Components.sendErrorMessage(sender, "Unclaim would split your territory. Unclaim from the edge or adjust claims first.");
                java.util.UUID gid = plugin.guildService.getGuildIdForMember(player.getUniqueId());
                java.util.UUID worldId = chunk.getWorld().getUID();
                com.spillhuset.furious.services.GuildService.ConnectivityReport after = plugin.guildService.analyzeConnectivityAfterRemoval(gid, worldId, chunk.getX(), chunk.getZ());
                if (after != null && after.components > 1) {
                    Components.sendGreyMessage(sender, "If removed here, you'd have " + after.components + " components with sizes " + after.componentSizes + ".");
                    if (!after.samples.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        int show = Math.min(3, after.samples.size());
                        for (int i = 0; i < show; i++) {
                            long[] s = after.samples.get(i);
                            if (i > 0) sb.append("; ");
                            sb.append("(").append(s[0]).append(",").append(s[1]).append(")");
                        }
                        Components.sendGreyMessage(sender, "Example chunks from resulting components: " + sb);
                    }
                    Components.sendGreyMessage(sender, "Run /guild connectivity for a map around your location.");
                }
            }
            case WORLD_DISABLED -> Components.sendErrorMessage(sender, "Guilds are disabled in this world.");
            default -> Components.sendErrorMessage(sender, "Failed to unclaim chunk.");
        }
        return true;
    }

    @Override
    public String getName() {
        return "unclaim";
    }

    @Override
    public String getPermission() {
        return "furious.guild.unclaim";
    }
}
