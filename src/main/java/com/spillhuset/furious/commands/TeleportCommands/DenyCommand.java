package com.spillhuset.furious.commands.TeleportCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DenyCommand implements SubCommandInterface {
    private final Furious plugin;
    public DenyCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "deny"; }
    @Override public String getPermission() { return "furious.teleport.deny"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { Components.sendErrorMessage(sender, "Only players can use this."); return true; }
        boolean on = plugin.teleportsService.toggleDeny(player.getUniqueId());
        if (on) Components.sendInfoMessage(player, "You will now automatically decline incoming teleport requests.");
        else Components.sendSuccessMessage(player, "You will now receive teleport requests.");
        return true;
    }
}
