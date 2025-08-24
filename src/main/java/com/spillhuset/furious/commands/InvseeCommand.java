package com.spillhuset.furious.commands;

import com.spillhuset.furious.Furious;
import com.spillhuset.furious.utils.CommandInterface;
import com.spillhuset.furious.utils.Components;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InvseeCommand implements CommandInterface, CommandExecutor, TabCompleter {
    private final Furious plugin;

    public InvseeCommand(Furious plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!can(sender, true)) return true;
        if (!(sender instanceof Player viewer)) {
            Components.sendErrorMessage(sender, "Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            Components.sendErrorMessage(sender, "That player is not online.");
            return true;
        }
        if (target.equals(viewer)) {
            Components.sendInfo(sender, Components.t("Opening your own inventory."));
        }
        viewer.openInventory(target.getInventory());
        Components.sendSuccess(sender, Components.t("Opened inventory of "), Components.playerComp(target.getName()), Components.t("."));
        return true;
    }

    private void sendUsage(CommandSender sender) {
        Components.sendInfo(sender, Components.t("Usage: /" + getName() + " <player>", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String name = p.getName();
                if (name != null && name.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(name);
                }
            }
        }
        return list;
    }

    @Override
    public String getName() { return "invsee"; }

    @Override
    public String getPermission() { return "furious.invsee"; }
}
