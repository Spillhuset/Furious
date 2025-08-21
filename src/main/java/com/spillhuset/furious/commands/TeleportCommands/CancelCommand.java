package com.spillhuset.furious.commands.TeleportCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.SubCommandInterface;
import com.spillhuset.furious.utils.Components;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CancelCommand implements SubCommandInterface {
    private final Furious plugin;
    public CancelCommand(Furious plugin) { this.plugin = plugin.getInstance(); }

    @Override public String getName() { return "cancel"; }
    @Override public String getPermission() { return "furious.teleport.cancel"; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { Components.sendErrorMessage(sender, "Only players can use this."); return true; }
        plugin.teleportsService.cancel(player);
        return true;
    }
}
