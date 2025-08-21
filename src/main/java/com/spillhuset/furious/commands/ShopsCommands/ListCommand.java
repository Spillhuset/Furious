package com.spillhuset.furious.commands.ShopsCommands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.Components;
import com.spillhuset.furious.utils.SubCommandInterface;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ListCommand implements SubCommandInterface {
    private final Furious plugin;
    public ListCommand(Furious plugin) { this.plugin = plugin; }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Header similar to banks list
        int total = plugin.shopsService.suggestShopNames("").size();
        Components.sendInfo(sender, Components.t("Shops total: "), Components.valueComp(String.valueOf(total)));
        plugin.shopsService.listShops(sender);
        return true;
    }

    @Override
    public String getName() { return "list"; }

    @Override
    public String getPermission() { return "furious.shops.list"; }
}
