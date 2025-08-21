package com.spillhuset.furious.commands.GuildCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.services.GuildService;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JoinCommand implements SubCommandInterface {
    private final Furious plugin;

    public JoinCommand(Furious plugin) {
        this.plugin = plugin.getInstance();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (!can(sender, false)) return out;
        if (!(sender instanceof Player)) return out;
        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String name : plugin.guildService.getAllGuildNames()) {
                if (name != null && name.toLowerCase().startsWith(prefix)) out.add(name);
            }
        }
        return out;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Components.sendErrorMessage(sender, "Only players can join guilds.");
            return true;
        }
        if (!can(sender, true)) return true;
        if (args.length < 2) {
            Components.sendInfoMessage(sender, "Usage: /guild join <guildName>");
            return true;
        }
        String guildName = args[1];
        GuildService.JoinResult res = plugin.guildService.joinOrRequest(player.getUniqueId(), guildName);
        switch (res) {
            case SUCCESS -> Components.sendSuccessMessage(sender, "You have joined guild: " + guildName + ".");
            case GUILD_NOT_FOUND -> Components.sendErrorMessage(sender, "Guild not found: " + guildName);
            case ALREADY_IN_GUILD -> Components.sendErrorMessage(sender, "You are already in a guild.");
            case ALREADY_REQUESTED -> Components.sendErrorMessage(sender, "You have already requested to join " + guildName + ".");
            case REQUESTED -> Components.sendSuccessMessage(sender, "Join request sent to guild: " + guildName + ".");
            default -> Components.sendErrorMessage(sender, "Failed to process join.");
        }
        return true;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getPermission() {
        return "furious.guild.join";
    }
}
