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
import java.util.UUID;

public class ClaimCommand implements SubCommandInterface {
    private final Furious plugin;

    public ClaimCommand(Furious plugin) {
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
            Components.sendErrorMessage(sender, "Only players can claim chunks.");
            return true;
        }
        // base permission check
        if (!can(sender, true)) return true;

        Chunk chunk = player.getLocation().getChunk();
        boolean isOp = sender.isOp();

        // If sender can act on others and provided a guild name, claim this chunk for that guild (OP override path)
        if (args.length >= 2 && can(sender, false, true)) {
            String guildName = args[1];
            // Ops override world-disabled restriction
            if (!isOp && !plugin.guildService.isWorldEnabled(chunk.getWorld().getUID())) {
                Components.sendErrorMessage(sender, "Guilds are disabled in this world.");
                return true;
            }
            GuildService.BulkClaimResult res = plugin.guildService.claimChunksForGuild(guildName, chunk.getWorld(), chunk.getX(), chunk.getZ(), chunk.getX(), chunk.getZ());
            if (res.total == 0) {
                Components.sendErrorMessage(sender, "Guild not found.");
                return true;
            }
            if (res.claimed == 0) {
                Components.sendErrorMessage(sender, "This chunk is already claimed by another guild.");
                return true;
            }
            Components.sendSuccess(sender,
                    Components.t("Claimed chunk at ("),
                    Components.valueComp(String.valueOf(chunk.getX())),
                    Components.t(", "),
                    Components.valueComp(String.valueOf(chunk.getZ())),
                    Components.t(") for guild "),
                    Components.valueComp(guildName),
                    Components.t("."));
            return true;
        }

        // Normal player-actor path: respect world enable unless op override
        if (!isOp && !plugin.guildService.isWorldEnabled(chunk.getWorld().getUID())) {
            Components.sendErrorMessage(sender, "Guilds are disabled in this world.");
            return true;
        }
        UUID actor = player.getUniqueId();
        GuildService.ClaimResult res = plugin.guildService.claimChunk(actor, chunk);
        switch (res) {
            case SUCCESS ->
                    Components.sendSuccess(sender, Components.t("Claimed chunk at ("), Components.valueComp(String.valueOf(chunk.getX())), Components.t(", "), Components.valueComp(String.valueOf(chunk.getZ())), Components.t(")"));
            case NOT_PLAYER_IN_GUILD -> Components.sendErrorMessage(sender, "You are not in a guild.");
            case NOT_ADMIN -> Components.sendErrorMessage(sender, "You must be an admin/owner of your guild to claim.");
            case ALREADY_CLAIMED_BY_OTHER ->
                    Components.sendErrorMessage(sender, "This chunk is already claimed by another guild.");
            case MAX_LIMIT_REACHED ->
                    Components.sendErrorMessage(sender, "Your guild has reached the maximum of " + plugin.guildService.getMaxClaimsPerGuild() + " claimed chunks.");
            case NOT_CONNECTED ->
                    Components.sendErrorMessage(sender, "Claim must touch your existing territory in this world.");
            case TOO_CLOSE_TO_OTHERS ->
                    Components.sendErrorMessage(sender, "Too close to another guild's territory. You must be at least 2 chunks away.");
            case OUTPOSTS_LIMIT_REACHED ->
                    Components.sendErrorMessage(sender, "No available outpost slots. Buy an outpost: /guild outpost buy");
            case OUTPOST_RANGE_EXCEEDED ->
                    Components.sendErrorMessage(sender, "Outpost expansion limited to a 5x5 area centered on the outpost.");
            case WORLD_DISABLED ->
                    Components.sendErrorMessage(sender, "Guilds are disabled in this world.");
            default -> Components.sendErrorMessage(sender, "Failed to claim chunk.");
        }
        return true;
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getPermission() {
        return "furious.guild.claim";
    }
}
