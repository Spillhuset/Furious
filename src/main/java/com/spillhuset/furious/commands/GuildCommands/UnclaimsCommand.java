package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class UnclaimsCommand implements SubCommandInterface {
    private final Furious plugin;

    public UnclaimsCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new java.util.ArrayList<>();
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
        // OP/admin-only bulk unclaim in selection for a specific guild
        if (!can(sender, true, true)) return true;
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can use this command (requires a selection). Use it in-game.");
            return true;
        }
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /guild unclaims <guildName>");
            return true;
        }
        String guildName = args[1];
        var pos1 = plugin.guildService.getSelectionPos1(player.getUniqueId());
        var pos2 = plugin.guildService.getSelectionPos2(player.getUniqueId());
        if (pos1 == null || pos2 == null) {
            Components.sendErrorMessage(sender, "You must select two points with a wooden_axe (left/right click blocks).");
            return true;
        }
        if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
            Components.sendErrorMessage(sender, "Selection points must be in the same world.");
            return true;
        }
        World world = pos1.getWorld();
        // Ops override world-disabled restriction
        if (!sender.isOp() && !plugin.guildService.isWorldEnabled(world.getUID())) {
            Components.sendErrorMessage(sender, "Guilds are disabled in this world.");
            return true;
        }
        int cx1 = pos1.getChunk().getX();
        int cz1 = pos1.getChunk().getZ();
        int cx2 = pos2.getChunk().getX();
        int cz2 = pos2.getChunk().getZ();

        GuildService.BulkUnclaimResult res = plugin.guildService.unclaimChunksForGuild(guildName, world, cx1, cz1, cx2, cz2);
        if (res.total == 0) {
            Components.sendErrorMessage(sender, "Guild not found or empty selection.");
            return true;
        }
        Components.sendSuccessMessage(sender, "Unclaimed " + res.removed + " chunk(s) for guild '" + guildName + "' out of selection total " + res.total + ".");
        return true;
    }

    @Override
    public String getName() {
        return "unclaims";
    }

    @Override
    public String getPermission() {
        return "furious.guild.unclaims";
    }
}
