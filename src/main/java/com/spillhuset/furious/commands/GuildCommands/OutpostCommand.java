package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OutpostCommand implements SubCommandInterface {
    private final Furious plugin;

    public OutpostCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (!can(sender, false)) return out;
        if (args.length == 2) {
            if ("buy".startsWith(args[1].toLowerCase())) out.add("buy");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("buy")) {
            out.add("1"); out.add("2"); out.add("5");
        }
        return out;
        }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!can(sender, true)) return true;
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can manage outposts.");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("buy")) {
            Components.sendInfo(sender, Components.t("Usage: /guild outpost buy [amount]"));
            return true;
        }
        int amount = 1;
        if (args.length >= 3) {
            try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
        }
        UUID actor = player.getUniqueId();
        UUID gid = plugin.guildService.getGuildIdForMember(actor);
        if (gid == null) { Components.sendErrorMessage(sender, "You are not in a guild."); return true; }
        // Require admin role to buy for the guild
        var guild = plugin.guildService.getGuildById(gid);
        var role = guild.getMembers().get(actor);
        if (role == null || role.ordinal() < 2) { // ADMIN check without importing enum here
            Components.sendErrorMessage(sender, "You must be an admin of your guild to buy outposts.");
            return true;
        }
        plugin.guildService.addOutpostAllowance(gid, amount);
        int allowed = plugin.guildService.getOutpostAllowance(gid);
        int have = plugin.guildService.getOutpostCount(gid);
        Components.sendSuccess(sender,
                Components.t("Purchased outpost(s). Allowed: "), Components.valueComp(String.valueOf(allowed)),
                Components.t(", Centers placed: "), Components.valueComp(String.valueOf(have))
        );
        return true;
    }

    @Override
    public String getName() { return "outpost"; }

    @Override
    public String getPermission() { return "furious.guild.outpost"; }
}